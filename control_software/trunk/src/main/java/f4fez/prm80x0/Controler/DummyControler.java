/*
 * DummyControler.java
 *
 * Created on 14 déc. 2007, 22:38:06
 *
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

package f4fez.prm80x0.Controler;

/**
 * Virtual PRM80 for tests without a radio (Tools > Connect virtual PRM, expert mode).
 * <p>
 * Simulates a PRM8060 144 MHz with firmware V5.0 (F4FEZ/F8EGQ/DC0CM),
 * as the firmware is meant to work, without its known errors:
 * <ul>
 * <li>external RAM with the V5 layout (config, frequency, shift and state
 *     area, checksums), EEPROM copy, factory defaults of src/inc_144.a51
 *     (66 channels, shift 600 kHz, 12.5 kHz, 400 ms)</li>
 * <li>channel specific shift (0 = default shift), state bits shift enabled,
 *     reverse, positive, scan lock out</li>
 * <li>reverse: TX = channel frequency, RX = frequency +/- shift. Taken from
 *     the channel in RAM with its own shift (the firmware V5 takes the
 *     frequency from the factory list in program memory and always the
 *     default shift)</li>
 * <li>configuration changes (squelch, shift, PLL, scan duration) update the
 *     checksum (the "M" command of the firmware does not)</li>
 * </ul>
 * Frequencies are the displayed frequencies (the firmware adds / subtracts
 * the IF for RX, the PRM80 control software shows the real frequencies).
 *
 * @author Florian
 */
public class DummyControler implements Controler {

    private static final int FREQ_CODE = Controler.FREQ144;
    private static final int MAX_CHANNELS = ChannelList.MAX_CHANNELS;

    private boolean connected;
    private int volume = 5;
    private int power = Controler.POWER_HI;
    private int rxFreq;
    private int txFreq;
    protected PRMStateChangeListener changeListener;

    private MemoryImageV5 image;

    // ------------------------------------------------------------ connection

    @Override
    public int connectPRM(String port) {
        this.image = new MemoryImageV5();
        // empty RAM and EEPROM: the firmware loads the factory defaults
        this.resetPRM();
        this.connected = true;
        return Controler.PRM8060;
    }

    @Override
    public void disconnectPRM() {
        this.connected = false;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public void setPRMStateChangeListener(PRMStateChangeListener listener) {
        this.changeListener = listener;
    }

    @Override
    public int getPRMState() {
        return Controler.STATE_NORMAL;
    }

    @Override
    public void refreshState() {
    }

    @Override
    public int getMajorFirmwareVersion() {
        return 5;
    }

    @Override
    public int getMinorFirmwareVersion() {
        return 0;
    }

    @Override
    public int getPRMType() {
        return Controler.PRM8060;
    }

    @Override
    public int getPRMFrequencyCode() {
        return FREQ_CODE;
    }

    @Override
    public MemoryImage getMemoryImage() {
        return this.image;
    }

    // ------------------------------------------------------------ RAM access

    private int ram(int adress) {
        return this.image.getRamData(adress) & 0xFF;
    }

    private void setRam(int adress, int value) {
        this.image.setRamData(adress, (byte) value);
    }

    private int config(int adress) {
        return ram(MemoryImageV5.RAM_AREA_ADRESS_CONFIG * 256 + adress);
    }

    /** Write a configuration byte and update the checksum. */
    private void setConfig(int adress, int value) {
        setRam(MemoryImageV5.RAM_AREA_ADRESS_CONFIG * 256 + adress, value);
        this.updateChecksums();
    }

    private int word(int area, int index) {
        int pos = area * 256 + index * 2;
        return (ram(pos) << 8) | ram(pos + 1);
    }

    private void setWord(int area, int index, int value) {
        int pos = area * 256 + index * 2;
        setRam(pos, (value >> 8) & 0xFF);
        setRam(pos + 1, value & 0xFF);
    }

    private int state(int chan) {
        return ram(MemoryImageV5.RAM_AREA_ADRESS_STATE * 256 + chan);
    }

    private static int pageSum(MemoryImage image, int page, int from) {
        int sum = 0;
        for (int i = from; i < 256; i++)
            sum += image.getRamData(page * 256 + i) & 0xFF;
        return sum & 0xFF;
    }

    /** The checksums of the four areas as the firmware calculates them. */
    private void updateChecksums() {
        setRam(MemoryImageV5.RAM_ADRESS_CONFIG_SUM, pageSum(this.image, MemoryImageV5.RAM_AREA_ADRESS_CONFIG, 0x0A));
        setRam(MemoryImageV5.RAM_ADRESS_FREQ_SUM, pageSum(this.image, MemoryImageV5.RAM_AREA_ADRESS_FREQ, 0));
        setRam(MemoryImageV5.RAM_ADRESS_SHIFT_SUM, pageSum(this.image, MemoryImageV5.RAM_AREA_ADRESS_SHIFT, 0));
        setRam(MemoryImageV5.RAM_ADRESS_STATE_SUM, pageSum(this.image, MemoryImageV5.RAM_AREA_ADRESS_STATE, 0));
    }

    private boolean checksumsValid() {
        return ram(MemoryImageV5.RAM_ADRESS_ID_CODE) == MemoryImageV5.ID_CODE
                && ram(MemoryImageV5.RAM_ADRESS_CONFIG_SUM) == pageSum(this.image, MemoryImageV5.RAM_AREA_ADRESS_CONFIG, 0x0A)
                && ram(MemoryImageV5.RAM_ADRESS_FREQ_SUM) == pageSum(this.image, MemoryImageV5.RAM_AREA_ADRESS_FREQ, 0)
                && ram(MemoryImageV5.RAM_ADRESS_SHIFT_SUM) == pageSum(this.image, MemoryImageV5.RAM_AREA_ADRESS_SHIFT, 0)
                && ram(MemoryImageV5.RAM_ADRESS_STATE_SUM) == pageSum(this.image, MemoryImageV5.RAM_AREA_ADRESS_STATE, 0);
    }

    /**
     * Factory defaults (load_ram_default with src/inc_144.a51), also
     * written to the EEPROM.
     */
    private void loadFactoryDefaults() {
        for (int i = 0; i < 4 * 256; i++)
            setRam(i, 0);
        int step = 12500;
        int r = Controler.PLL_REF_OSC / (2 * step);
        setRam(MemoryImageV5.RAM_ADRESS_ID_CODE, MemoryImageV5.ID_CODE);
        setRam(MemoryImageV5.RAM_ADRESS_CHAN, 0);
        setRam(MemoryImageV5.RAM_ADRESS_MODE, 0);
        setRam(MemoryImageV5.RAM_ADRESS_SQUELCH, 5);
        setRam(MemoryImageV5.RAM_ADRESS_PLL_DIV_HI, (r & 1792) >> 3);
        setRam(MemoryImageV5.RAM_ADRESS_PLL_DIV_LO, r & 255);
        int shift = 600000 / step;
        setRam(MemoryImageV5.RAM_ADRESS_SHIFT_HI, shift >> 8);
        setRam(MemoryImageV5.RAM_ADRESS_SHIFT_LO, shift & 255);
        setRam(MemoryImageV5.RAM_ADRESS_SCAN_DURATION, 8);      // 8 * 50 ms
        // channels of inc_144.a51: 16 repeaters with shift -, simplex channels
        int[] freqs = new int[66];
        int n = 0;
        for (int i = 0; i < 16; i++)
            freqs[n++] = 145600000 + i * 12500;
        for (int i = 0; i < 32; i++)
            freqs[n++] = 145200000 + i * 12500;
        freqs[n++] = 144500000;
        freqs[n++] = 144800000;
        for (int i = 0; i < 16; i++)
            freqs[n++] = 145000000 + i * 12500;
        for (int i = 0; i < n; i++) {
            setWord(MemoryImageV5.RAM_AREA_ADRESS_FREQ, i, freqs[i] / step);
            setWord(MemoryImageV5.RAM_AREA_ADRESS_SHIFT, i, 0);
            setRam(MemoryImageV5.RAM_AREA_ADRESS_STATE * 256 + i, i < 16 ? 0x01 : 0x00);
        }
        setRam(MemoryImageV5.RAM_ADRESS_MAX_CHAN, n - 1);
        this.updateChecksums();
        this.image.copyRam2Eeprom();
    }

    // ------------------------------------------------------------ restart, RAM, EEPROM

    /** Restart ("0"): check the RAM, else the EEPROM, else factory defaults. */
    @Override
    public void resetPRM() {
        if (!this.checksumsValid()) {
            this.image.copyEeprom2Ram();
            if (!this.checksumsValid())
                this.loadFactoryDefaults();
        }
        int chan = config(MemoryImageV5.RAM_ADRESS_CHAN);
        if (chan > this.getMaxChan())
            chan = 0;
        this.setCurrentChannel(chan);
    }

    /** Reload RAM: factory defaults. */
    @Override
    public void reloadRAM() {
        this.loadFactoryDefaults();
        this.setCurrentChannel(0);
    }

    @Override
    public void RAM2EEPROM() {
        this.updateChecksums();
        this.image.copyRam2Eeprom();
    }

    @Override
    public void EEPROM2RAM() {
        this.image.copyEeprom2Ram();
        this.resetPRM();
    }

    // ------------------------------------------------------------ state

    @Override
    public boolean isPllLocked() {
        return true;
    }

    @Override
    public int readVolume() {
        return this.volume;
    }

    @Override
    public void writeVolume(int volume) {
        this.volume = Math.max(0, Math.min(15, volume));
    }

    @Override
    public int readSquelch() {
        return config(MemoryImageV5.RAM_ADRESS_SQUELCH);
    }

    @Override
    public void writeSquelch(int level) {
        setConfig(MemoryImageV5.RAM_ADRESS_SQUELCH, Math.max(0, Math.min(15, level)));
    }

    @Override
    public int getPower() {
        return this.power;
    }

    @Override
    public void setPower(int power) {
        this.power = power;
    }

    @Override
    public int getMaxChan() {
        return config(MemoryImageV5.RAM_ADRESS_MAX_CHAN);
    }

    @Override
    public int getCurrentChannel() {
        return config(MemoryImageV5.RAM_ADRESS_CHAN);
    }

    @Override
    public int getChannelState() {
        return this.state(this.getCurrentChannel());
    }

    @Override
    public void setCurrentChannel(int channel) {
        if (channel < 0 || channel > this.getMaxChan())
            return;                         // the firmware ignores unknown channels
        setConfig(MemoryImageV5.RAM_ADRESS_CHAN, channel);
        this.tune(channel);
    }

    /** RX and TX frequency of a channel (get_freq of the firmware). */
    private void tune(int chan) {
        int step = this.getPLLStep();
        int base = word(MemoryImageV5.RAM_AREA_ADRESS_FREQ, chan) * step;
        int state = this.state(chan);
        this.rxFreq = base;
        this.txFreq = base;
        if ((state & 1) != 0) {
            int shift = word(MemoryImageV5.RAM_AREA_ADRESS_SHIFT, chan);
            if (shift == 0)
                shift = (config(MemoryImageV5.RAM_ADRESS_SHIFT_HI) << 8) | config(MemoryImageV5.RAM_ADRESS_SHIFT_LO);
            int offset = shift * step * ((state & 4) != 0 ? 1 : -1);
            if ((state & 2) != 0)
                this.rxFreq = base + offset;    // reverse: receive on the input of the repeater
            else
                this.txFreq = base + offset;
        }
    }

    // ------------------------------------------------------------ PLL, VFO

    @Override
    public void setPLLStep(int frequency) {
        int r = Controler.PLL_REF_OSC / (2 * frequency);
        setRam(MemoryImageV5.RAM_ADRESS_PLL_DIV_HI, (r & 1792) >> 3);
        setConfig(MemoryImageV5.RAM_ADRESS_PLL_DIV_LO, r & 255);
    }

    @Override
    public int getPLLStep() {
        int r = config(MemoryImageV5.RAM_ADRESS_PLL_DIV_LO)
                | ((config(MemoryImageV5.RAM_ADRESS_PLL_DIV_HI) << 3) & 1792);
        if (r == 0)
            return 12500;
        return Controler.PLL_REF_OSC / (2 * r);
    }

    @Override
    public void setRxPLLFrequecny(int frequency) {
        this.rxFreq = frequency;
    }

    @Override
    public int getRxPLLFrequency() {
        return this.rxFreq;
    }

    @Override
    public void setTxPLLFrequency(int frequency) {
        this.txFreq = frequency;
    }

    @Override
    public int getTxPLLFrequency() {
        return this.txFreq;
    }

    // ------------------------------------------------------------ configuration

    /** Default shift (used when a channel has no shift of its own). */
    @Override
    public int getTxFrequencyShift() {
        int w = (config(MemoryImageV5.RAM_ADRESS_SHIFT_HI) << 8) | config(MemoryImageV5.RAM_ADRESS_SHIFT_LO);
        return w * this.getPLLStep();
    }

    @Override
    public void setTxFrequencyShift(int frequency) {
        int w = frequency / this.getPLLStep();
        setRam(MemoryImageV5.RAM_ADRESS_SHIFT_HI, (w >> 8) & 0xFF);
        setConfig(MemoryImageV5.RAM_ADRESS_SHIFT_LO, w & 0xFF);
    }

    /** Scan duration per channel in ms (RAM: units of 50 ms). */
    @Override
    public int getScanSpeed() {
        return config(MemoryImageV5.RAM_ADRESS_SCAN_DURATION) * 50;
    }

    @Override
    public void setScanSpeed(int speed) {
        int units = Math.max(1, Math.min(255, Math.round(speed / 50f)));
        setConfig(MemoryImageV5.RAM_ADRESS_SCAN_DURATION, units);
    }

    // ------------------------------------------------------------ channels

    @Override
    public ChannelList getChannels() {
        ChannelList list = new ChannelList();
        int step = this.getPLLStep();
        for (int i = 0; i <= this.getMaxChan(); i++) {
            int state = this.state(i);
            String shiftEnabled = (state & 1) != 0 ? "Enabled" : "";
            String shift = (state & 1) != 0 ? ((state & 4) != 0 ? "+" : "-") : "";
            Channel chan = new Channel(word(MemoryImageV5.RAM_AREA_ADRESS_FREQ, i) * step,
                    word(MemoryImageV5.RAM_AREA_ADRESS_SHIFT, i) * step,
                    (state & 8) != 0 ? "Lock" : "",
                    (state & 2) != 0 ? "Reverse" : "",
                    shiftEnabled, shift);
            list.addChannel(chan);
        }
        return list;
    }

    private void storeChannel(int index, Channel chan) {
        int step = this.getPLLStep();
        setWord(MemoryImageV5.RAM_AREA_ADRESS_FREQ, index, chan.getIntFrequency() / step);
        String s = chan.getShiftFreq();
        int shift = (s == null || s.trim().isEmpty()) ? 0 : chan.getIntShiftFreq() / step;
        setWord(MemoryImageV5.RAM_AREA_ADRESS_SHIFT, index, shift);
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
        setRam(MemoryImageV5.RAM_AREA_ADRESS_STATE * 256 + index, state);
    }

    @Override
    public void setChannels(ChannelList list) throws CommunicationException {
        if (list.countChannel() == 0 || list.countChannel() > MAX_CHANNELS)
            throw new VirtualPrmException("1 to " + MAX_CHANNELS + " channels possible");
        for (int i = 0; i < list.countChannel(); i++)
            this.storeChannel(i, list.getChannel(i));
        setRam(MemoryImageV5.RAM_ADRESS_MAX_CHAN, list.countChannel() - 1);
        if (this.getCurrentChannel() > this.getMaxChan())
            setRam(MemoryImageV5.RAM_ADRESS_CHAN, 0);
        this.updateChecksums();
    }

    /** "P" command: change a channel or add one after the last channel. */
    @Override
    public void writeChannel(Channel channel) throws CommunicationException {
        int id = channel.getId();
        int max = this.getMaxChan();
        if (id < 0 || id > max + 1 || id >= MAX_CHANNELS)
            throw new VirtualPrmException("Channel " + id + " cannot be written (channels 0 to " + (max + 1) + ")");
        this.storeChannel(id, channel);
        if (id == max + 1)
            setRam(MemoryImageV5.RAM_ADRESS_MAX_CHAN, id);
        this.updateChecksums();
        // as the firmware: the frequencies change at the next channel change
        // (in VFO mode the VFO frequencies stay)
    }

    @Override
    public void deleteLastChannel() throws CommunicationException {
        int max = this.getMaxChan();
        if (max == 0)
            throw new VirtualPrmException("The last channel cannot be removed");
        setRam(MemoryImageV5.RAM_ADRESS_MAX_CHAN, max - 1);
        this.updateChecksums();
        if (this.getCurrentChannel() > max - 1)
            this.setCurrentChannel(max - 1);
    }

    /** Error answer of the virtual PRM80. */
    public static class VirtualPrmException extends CommunicationException {
        private static final long serialVersionUID = 1L;

        public VirtualPrmException(String message) {
            super(message);
        }
    }
}
