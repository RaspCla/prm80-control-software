/*
 *   Copyright (c) 2007, 2008 Florian MAZEN
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package f4fez.prm80x0;

import f4fez.prm80x0.Controler.Channel;
import f4fez.prm80x0.Controler.ChannelList;
import f4fez.prm80x0.Controler.Controler;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Export of a channel list as configuration file for the firmware
 * (src/inc_144.a51 or src/inc_430.a51, 8051 assembler "A51").
 * These values are loaded when RAM and EEPROM are invalid (first start)
 * or with the "I" command.
 * <p>
 * Format of firmware V5 (F4FEZ/F8EGQ/DC0CM):
 * <pre>
 *   CONFIG_CHAN_COUNT     highest channel number (number of channels - 1)
 *   CONFIG_SHIFT_LO/HI    default shift as PLL word
 *   CONFIG_PLL_DIV_HI/LO  PLL divider (PLL step)
 *   CONFIG_SCAN_DURATION  time per channel while scanning, * 50 ms (V4, V5)
 *   freq_list:            per channel 2 bytes (hi, lo): frequency / PLL step
 *   shift_list:           per channel 1 word: channel specific shift / PLL step,
 *                         0 = default shift (V5 only)
 *   chan_state_table:     per channel 1 byte: b0 shift enabled, b1 reverse,
 *                         b2 positive shift, b3 scan lock out
 * </pre>
 */
public class A51Export {

    /** Configuration values written into the file. */
    public static class Config {
        /** firmware major version (3, 4 or 5): selects the file format */
        public int firmwareVersion = 5;
        /** PLL step in Hz */
        public int pllStep = 12500;
        /** default shift in Hz */
        public int txShift = 600000;
        /** scan duration in ms (firmware V4/V5) */
        public int scanSpeed = 400;
        /** 144 or 430 */
        public int band = Controler.FREQ144;
        /** where the values come from, written as comment */
        public String source = "default values";

        /** Default values for a band (as in the firmware files). */
        public static Config defaults(int band) {
            Config c = new Config();
            c.band = band;
            c.txShift = band == Controler.FREQ430 ? 7600000 : 600000;
            return c;
        }

        /** Values of a connected PRM80. */
        public static Config fromPrm(Controler controler) throws Exception {
            Config c = new Config();
            c.firmwareVersion = controler.getMajorFirmwareVersion();
            c.pllStep = controler.getPLLStep();
            c.band = controler.getPRMFrequencyCode();
            c.txShift = controler.supportsTxShift() ? controler.getTxFrequencyShift()
                    : (c.band == Controler.FREQ430 ? 7600000 : 600000);
            if (controler.supportsScanSpeed())
                c.scanSpeed = controler.getScanSpeed();
            c.source = (controler.getPRMType() == Controler.PRM8070 ? "PRM8070" : "PRM8060")
                    + " V" + controler.getMajorFirmwareVersion() + "." + controler.getMinorFirmwareVersion()
                    + " " + c.band + " MHz";
            return c;
        }
    }

    /** @return 144 or 430, guessed from the frequencies of the list */
    public static int guessBand(ChannelList channels) {
        for (int i = 0; i < channels.countChannel(); i++) {
            if (channels.getChannel(i).getIntFrequency() > 300000000)
                return Controler.FREQ430;
        }
        return Controler.FREQ144;
    }

    /** @return file name the firmware uses for the band */
    public static String firmwareFileName(int band) {
        return band == Controler.FREQ430 ? "inc_430.a51" : "inc_144.a51";
    }

    /**
     * Write the channel list as firmware configuration file.
     * @param channels channel list (e.g. of the channel manager)
     * @param config configuration values and file format
     * @param listName name of the list for the header comment (file name), may be null
     * @param file file to write
     * @throws Exception with a message for the user if a value does not fit
     */
    public static void exportA51(ChannelList channels, Config config, String listName, File file) throws Exception {
        int count = channels.countChannel();
        if (count == 0)
            throw new Exception("The channel list is empty.");
        if (count > ChannelList.MAX_CHANNELS)
            throw new Exception("The PRM80 has only " + ChannelList.MAX_CHANNELS + " channels.");
        int step = config.pllStep;
        StringBuilder out = new StringBuilder();

        out.append(";    PRM80 channel configuration for the firmware\n");
        out.append(";    Generated by the PRM80 control software, ")
           .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        out.append(";    Channel list: ").append(ascii(listName == null ? "not saved" : listName))
           .append(", ").append(count).append(" channels\n");
        out.append(";    Configuration: ").append(ascii(config.source)).append("\n");
        out.append(";    Format: firmware V").append(config.firmwareVersion)
           .append(config.firmwareVersion >= 5 ? " (with channel specific shift)" : "").append("\n");
        out.append(";    Use as src/").append(firmwareFileName(config.band)).append(" of the firmware.\n");
        out.append(";    These values are loaded when RAM and EEPROM are invalid (first start)\n");
        out.append(";    or with the \"I\" command (system initialisation).\n\n");

        // configuration area
        int shiftWord = word(config.txShift, step, "default shift");
        int r = Controler.PLL_REF_OSC / (2 * step);
        out.append(String.format("CONFIG_CHAN_COUNT       EQU     %s       ; highest channel number (channels 0..%d)\n",
                hexByte(count - 1), count - 1));
        out.append(String.format("CONFIG_SHIFT_LO         EQU     %s       ; default shift %s MHz\n",
                hexByte(shiftWord & 255), mhz(config.txShift)));
        out.append(String.format("CONFIG_SHIFT_HI         EQU     %s\n", hexByte((shiftWord >> 8) & 255)));
        out.append(String.format("CONFIG_PLL_DIV_HI       EQU     %s       ; PLL step %s kHz\n",
                hexByte((r & 1792) >> 3), new BigDecimal(step).movePointLeft(3).stripTrailingZeros().toPlainString()));
        out.append(String.format("CONFIG_PLL_DIV_LO       EQU     %s\n", hexByte(r & 255)));
        if (config.firmwareVersion > 3) {
            int scan = Math.max(1, Math.min(255, Math.round(config.scanSpeed / 50f)));
            out.append(String.format("CONFIG_SCAN_DURATION    EQU     %s       ; %d * 50 ms = %d ms per channel when scanning\n",
                    hexByte(scan), scan, scan * 50));
        }
        out.append("\n");

        // frequencies
        out.append("freq_list:\n");
        for (int i = 0; i < count; i++) {
            Channel chan = channels.getChannel(i);
            int w = word(chan.getIntFrequency(), step, "frequency of channel " + i);
            out.append("        ; Channel : ").append(i).append(" Frequency : ").append(chan.getIntFrequency());
            if (chan.getComments() != null && !chan.getComments().trim().isEmpty())
                out.append("  ").append(ascii(chan.getComments().trim()));
            out.append("\n");
            out.append("        DB      ").append(hexByte((w >> 8) & 255)).append("\n");
            out.append("        DB      ").append(hexByte(w & 255)).append("\n");
        }
        out.append("\n");

        // channel specific shifts (V5)
        if (config.firmwareVersion >= 5) {
            out.append("shift_list:\n");
            out.append("        ; 0 = the default shift (CONFIG_SHIFT) is used\n");
            for (int i = 0; i < count; i++) {
                Channel chan = channels.getChannel(i);
                int shift = shiftOf(chan);
                int w = word(shift, step, "shift of channel " + i);
                out.append("        ; Channel : ").append(i).append(" Shift : ").append(mhz(shift)).append(" Mhz\n");
                out.append("        DW      ").append(hexWord(w)).append("\n");
            }
            out.append("\n");
        }

        // channel states
        out.append("chan_state_table:\n");
        out.append("        ; b0 shift enabled, b1 reverse, b2 positive shift, b3 scan lock out\n");
        for (int i = 0; i < count; i++) {
            Channel chan = channels.getChannel(i);
            int state = 0;
            if (chan.isShiftEnabled()) {
                state |= 1;
                if ("+".equals(chan.getShift()))
                    state |= 4;
            }
            if ("Reverse".equals(chan.getShiftReverse()))
                state |= 2;
            if ("Lock".equals(chan.getScanLock()))
                state |= 8;
            out.append("        DB      ").append(hexByte(state)).append("    ; Channel : ").append(i)
               .append("\t").append(String.format("%8s", Integer.toBinaryString(state)).replace(' ', '0')).append("b")
               .append(describeState(state)).append("\n");
        }
        out.append("\n");

        // same line ends as the files of the firmware (LF); git converts them if configured
        String text = out.toString();
        try {
            Files.write(file.toPath(), text.getBytes(StandardCharsets.US_ASCII));
        } catch (IOException ex) {
            throw new Exception("Cannot write file " + file + ": " + ex.getMessage());
        }
    }

    private static int shiftOf(Channel chan) {
        String s = chan.getShiftFreq();
        if (s == null || s.trim().isEmpty())
            return 0;
        return chan.getIntShiftFreq();
    }

    /** Frequency in Hz -> PLL word; must be a multiple of the step and fit into 16 bit. */
    private static int word(int hz, int step, String what) throws Exception {
        if (hz % step != 0)
            throw new Exception("The " + what + " (" + mhz(hz) + " MHz) is not a multiple of the PLL step ("
                    + new BigDecimal(step).movePointLeft(3).stripTrailingZeros().toPlainString() + " kHz).");
        int w = hz / step;
        if (w < 0 || w > 0xFFFF)
            throw new Exception("The " + what + " (" + mhz(hz) + " MHz) is out of range.");
        return w;
    }

    private static String describeState(int state) {
        StringBuilder s = new StringBuilder();
        if ((state & 1) != 0)
            s.append(" = ").append((state & 4) != 0 ? "Positive" : "Negative").append(" Shift");
        if ((state & 2) != 0)
            s.append(s.length() == 0 ? " = " : ", ").append("Reverse");
        if ((state & 8) != 0)
            s.append(s.length() == 0 ? " = " : ", ").append("Scan lock out");
        return s.toString();
    }

    /** Assembler hex byte, e.g. 0a0h (must start with a digit) */
    private static String hexByte(int value) {
        return String.format("0%02xh", value & 255);
    }

    /** Assembler hex word, e.g. 0030h */
    private static String hexWord(int value) {
        return String.format("0%04xh", value & 0xFFFF);
    }

    private static String mhz(int hz) {
        return new BigDecimal(hz).movePointLeft(6).stripTrailingZeros().toPlainString();
    }

    /** Comments must be plain ASCII for the assembler: umlauts are replaced. */
    static String ascii(String text) {
        String s = text.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue")
                .replace("Ä", "Ae").replace("Ö", "Oe").replace("Ü", "Ue").replace("ß", "ss");
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
