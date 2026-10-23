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

import java.util.ArrayList;

/**
 *
 * @author Florian
 */
public class DummyControler implements Controler{
    private static int localOscillatorFrequency = 6000000;
    
    private boolean connected;
    
    private int volume = 5;
    protected PRMStateChangeListener changeListener;
   
    private int power;
    
    private int pllRefCounter;
    private int rxFreq;
    private int txFreq;
    private int scanSpeed = 10;

    private MemoryImage image;
    
    @Override
    public int connectPRM(String port) {        
        image = new MemoryImageV3();
        init();
        this.resetPRM();        
        this.connected = true;
        return DummyControler.PRM8060;        
    }

    @Override
    public void disconnectPRM() {
        this.connected = false;
    }

    @Override
    public int getPRMState() {
        return DummyControler.STATE_NORMAL;
    }

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
        assert volume > 16;
        this.volume = volume;
    }

    @Override
    public int readSquelch() {
        return this.image.getRamData(MemoryImageV3.RAM_ADRESS_SQUELCH);
    }

    @Override
    public void writeSquelch(int level) {
        assert level > 15;
        this.image.setRamData(MemoryImageV3.RAM_ADRESS_SQUELCH, (byte) level);
    }

    @Override
    public int getCurrentChannel() {
        return this.image.getRamData(MemoryImageV3.RAM_ADRESS_CHAN);
    }

    @Override
    public void setCurrentChannel(int channel) {
        assert channel > this.image.getEepromData(MemoryImageV3.RAM_ADRESS_MAX_CHAN);
        this.image.setRamData(MemoryImageV3.RAM_ADRESS_CHAN, (byte) channel);
        
        int eepromPos = channel*2 + MemoryImageV3.RAM_AREA_ADRESS_FREQ * 256;
        
        byte wordHi = this.image.getRamData(eepromPos+1);
        byte wordLo = this.image.getRamData(eepromPos);
        int pllWord = ((wordHi & 0xFF) << 8) + (wordLo & 0xFF);
        
        this.setRxPLLFrequecny(pllWord * (DummyControler.localOscillatorFrequency / this.pllRefCounter));
        this.setTxPLLFrequency(pllWord * (DummyControler.localOscillatorFrequency / this.pllRefCounter));
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
    public void reloadRAM() {
        this.volume = 4;
        this.connected = false;
        this.setPLLStep(12500);        
        this.image.copyEeprom2Ram();
       
    }

    @Override
    public void resetPRM() {                
        this.setCurrentChannel(this.image.getRamData(MemoryImageV3.RAM_ADRESS_CHAN));
    }

    @Override
    public int getMajorFirmwareVersion() {
        return 3;
    }

    @Override
    public int getMaxChan() {
        return this.image.getRamData(MemoryImageV3.RAM_ADRESS_MAX_CHAN);
    }

    @Override
    public void setPLLStep(int frequency) {
        this.pllRefCounter = DummyControler.localOscillatorFrequency / frequency;
    }

    @Override
    public int getPLLStep() {
        return DummyControler.localOscillatorFrequency / this.pllRefCounter;
    }

    @Override
    public void setRxPLLFrequecny(int frequency) {
        this.rxFreq = frequency;
        //this.pllCounter = frequency / this.getPLLStep();
    }

    @Override
    public int getRxPLLFrequency() {
        return this.rxFreq;
        //return this.pllCounter * this.getPLLStep();
    }
    @Override
        public void setTxPLLFrequency(int frequency){
        this.txFreq = frequency;
    }

    @Override
    public int getTxPLLFrequency() {
        return this.txFreq;
    }
    
    private void loadVirtualEEprom(ArrayList<Integer> array) {
        for(int i= 0; i < array.size(); i++) {
            int eepromPos = i*2 + MemoryImageV3.RAM_AREA_ADRESS_FREQ*256;
            int pllWord = array.get(i) / 12500;
            byte wordHi = (byte) (pllWord / 256);
            byte wordLo = (byte) (pllWord - (wordHi * 256) );
            this.image.setEepromData(eepromPos, wordLo);
            this.image.setEepromData(eepromPos+1, wordHi);
        }
        this.image.setEepromData(MemoryImageV3.RAM_ADRESS_MAX_CHAN, (byte) (array.size()-1));
        this.image.setEepromData(MemoryImageV3.RAM_ADRESS_SQUELCH, (byte) 5);
    }

    @Override
    public void RAM2EEPROM() {
        this.image.copyRam2Eeprom();
    }

    @Override
    public void EEPROM2RAM() {
        this.image.copyEeprom2Ram();
    }

    @Override
    public int getMinorFirmwareVersion() {
        return 0;
    }

    @Override
    public MemoryImage getMemoryImage() {
        return this.image;
    }

    @Override
    public ChannelList getChannels() {
        ChannelList list = new ChannelList();
        int maxChan = this.image.getRamData(MemoryImageV3.RAM_ADRESS_MAX_CHAN);
        for (int i= 0; i <= maxChan; i++) {
            int ramPos = i*2 + MemoryImageV3.RAM_AREA_ADRESS_FREQ*256;
            byte wordHi = this.image.getRamData(ramPos+1);
            byte wordLo = this.image.getRamData(ramPos);
            int pllWord = ((wordHi & 0xFF) << 8) + (wordLo & 0xFF);
            String freq = Integer.toString(pllWord * this.getPLLStep());
            freq = freq.substring(0, freq.length()-6) + "." + freq.substring(freq.length()-6);
            int statePos = i + MemoryImageV3.RAM_AREA_ADRESS_STATE*256;
            byte state = this.image.getRamData(statePos);
            String shift = "";
            if ((state & 1) == 1) {
                if ((state & 4) == 4) {
                    shift = "+";
                }
                else {
                    shift = "-";
                }
            }
            list.addChannel(new Channel(freq, "0.000000", "", shift, "", "Enabled"));               // here we have to add a real shift freq
        }
        return list;
    }

    @Override
    public void setChannels(ChannelList list) {
        for (int i= 0; i < list.countChannel(); i++) {
            int ramPos = i*2 + MemoryImageV3.RAM_AREA_ADRESS_FREQ*256;
            int pllWord = list.getChannel(i).getIntFrequency()  / this.getPLLStep();
            byte wordHi = (byte) (pllWord / 256);
            byte wordLo = (byte) (pllWord - (wordHi * 256) );
            this.image.setRamData(ramPos, wordLo);
            this.image.setRamData(ramPos+1, wordHi);     
            int statePos = i + MemoryImageV3.RAM_AREA_ADRESS_STATE*256;
            byte state = 0;
            if (list.getChannel(i).isShift()) {
                state += 1;
                if ("+".equals(list.getChannel(i).getShift()))
                    state += 4;
            }
            this.image.setRamData(statePos,state);
        }
        this.image.setRamData(MemoryImageV3.RAM_ADRESS_MAX_CHAN, (byte)(list.countChannel()-1));
    }

    @Override
    public void setPRMStateChangeListener(PRMStateChangeListener listener) {
        this.changeListener = listener;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    private void init() {
        ArrayList<Integer> freq = new ArrayList<Integer>();
        freq.add(144100000);
        freq.add(145600000);
        freq.add(145800000);
        this.image.setRamData(MemoryImageV3.RAM_ADRESS_CHAN, (byte) 0);
        this.loadVirtualEEprom(freq);
        
        this.reloadRAM();
    }

    @Override
    public int getTxFrequencyShift() {
        return 600000;
    }

    @Override
    public void setTxFrequencyShift(int frequency) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getScanSpeed() throws CommunicationException {
        return this.scanSpeed;
    }

    @Override
    public void setScanSpeed(int speed) throws CommunicationException {
        this.scanSpeed = speed;
    }

    @Override
    public int getPRMType() throws CommunicationException {
        return DummyControler.PRM8060;
    }

    @Override
    public int getPRMFrequencyCode() throws CommunicationException {
        return 144;
    }

}
