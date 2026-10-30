/*
 * PrmConfiguration.java
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

package f4fez.prm80x0.gui;

import java.io.File;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;

/**
 * The settings of the configuration dialog (the configuration in the RAM of
 * the PRM80 without the channel list), and their file (*.cfg).
 * A value of -1 means: not known / not supported by the firmware.
 *
 * File format (text, one value per line, "#" = comment):
 * <pre>
 * Band=144
 * PllStep_kHz=12.5
 * DefaultShift_kHz=600
 * ScanSpeed_ms=400
 * Squelch=5
 * </pre>
 */
public final class PrmConfiguration {

    public static final String EXTENSION = "cfg";

    private static final String KEY_BAND = "Band";
    private static final String KEY_PLL_STEP = "PllStep_kHz";
    private static final String KEY_SHIFT = "DefaultShift_kHz";
    private static final String KEY_SCAN_SPEED = "ScanSpeed_ms";
    private static final String KEY_SQUELCH = "Squelch";

    /** band in MHz (144, 430 ...), -1 = unknown */
    public final int band;
    /** PLL step in Hz */
    public final int pllStep;
    /** default shift in Hz */
    public final int txShift;
    /** time per channel while scanning, ms */
    public final int scanSpeed;
    /** squelch level 0 to 15 */
    public final int squelch;

    public PrmConfiguration(int band, int pllStep, int txShift, int scanSpeed, int squelch) {
        this.band = band;
        this.pllStep = pllStep;
        this.txShift = txShift;
        this.scanSpeed = scanSpeed;
        this.squelch = squelch;
    }

    /** Same settings (the band is not a setting). */
    public boolean sameValues(PrmConfiguration o) {
        return o != null && this.pllStep == o.pllStep && this.txShift == o.txShift
                && this.scanSpeed == o.scanSpeed && this.squelch == o.squelch;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PrmConfiguration && this.sameValues((PrmConfiguration) obj)
                && this.band == ((PrmConfiguration) obj).band;
    }

    @Override
    public int hashCode() {
        return Objects.hash(band, pllStep, txShift, scanSpeed, squelch);
    }

    /**
     * Write the file.
     * @param device description of the PRM80 for the comment, e.g. "PRM8070 V5.1 144 MHz"
     */
    public void save(File file, String device) throws Exception {
        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            out.println("# PRM80 configuration (Tools > Configuration of the PRM80 control software " + f4fez.prm80x0.AppVersion.get() + ")");
            if (device != null && !device.isEmpty())
                out.println("# Read from: " + device);
            out.println("# The channel list is saved in the channel manager (*.csv).");
            if (this.band > 0)
                out.println(KEY_BAND + "=" + this.band);
            if (this.pllStep > 0)
                out.println(KEY_PLL_STEP + "=" + kHz(this.pllStep));
            if (this.txShift >= 0)
                out.println(KEY_SHIFT + "=" + kHz(this.txShift));
            if (this.scanSpeed >= 0)
                out.println(KEY_SCAN_SPEED + "=" + this.scanSpeed);
            if (this.squelch >= 0)
                out.println(KEY_SQUELCH + "=" + this.squelch);
            if (out.checkError())
                throw new Exception("Error writing file " + file);
        }
    }

    /** Read a file written by save(). */
    public static PrmConfiguration load(File file) throws Exception {
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            p.load(in);
        }
        if (!p.containsKey(KEY_PLL_STEP) && !p.containsKey(KEY_SHIFT)
                && !p.containsKey(KEY_SCAN_SPEED) && !p.containsKey(KEY_SQUELCH))
            throw new Exception(file.getName() + " is not a PRM80 configuration file.");
        int band = integer(p, KEY_BAND, 1, 9999);
        int step = p.containsKey(KEY_PLL_STEP) ? hz(p, KEY_PLL_STEP) : -1;
        if (step == 0)
            throw new Exception(KEY_PLL_STEP + " must not be 0.");
        int shift = p.containsKey(KEY_SHIFT) ? hz(p, KEY_SHIFT) : -1;
        int scan = integer(p, KEY_SCAN_SPEED, 50, 12750);
        int squelch = integer(p, KEY_SQUELCH, 0, 15);
        return new PrmConfiguration(band, step, shift, scan, squelch);
    }

    private static int integer(Properties p, String key, int min, int max) throws Exception {
        String v = p.getProperty(key);
        if (v == null)
            return -1;
        try {
            int i = Integer.parseInt(v.trim());
            if (i < min || i > max)
                throw new NumberFormatException();
            return i;
        } catch (NumberFormatException ex) {
            throw new Exception(key + "=" + v + ": a number from " + min + " to " + max + " is expected.");
        }
    }

    private static int hz(Properties p, String key) throws Exception {
        String v = p.getProperty(key);
        try {
            int i = hz(v);
            if (i < 0)
                throw new NumberFormatException();
            return i;
        } catch (RuntimeException ex) {
            throw new Exception(key + "=" + v + ": a value in kHz is expected, e.g. 12.5");
        }
    }

    /** 12500 Hz -> "12.5" */
    public static String kHz(int hz) {
        return new BigDecimal(hz).divide(new BigDecimal(1000)).stripTrailingZeros().toPlainString();
    }

    /** "12.5" -> 12500 Hz (NumberFormatException / ArithmeticException if not possible) */
    public static int hz(Object kHz) {
        return new BigDecimal(kHz.toString().trim().replace(',', '.')).multiply(new BigDecimal(1000)).intValueExact();
    }
}
