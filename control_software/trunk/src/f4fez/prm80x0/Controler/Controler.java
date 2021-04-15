/*
 * PRMControler.java
 * 
 * Created on 13 déc. 2007, 23:32:18
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
 *
 * @author Florian
 */
public interface Controler {
    /**
     * PRM 8060 identification code
     */
    public final static int PRM8060 = 1;
    /**
     * PRM 8070 identification code
     */
    public final static int PRM8070 = 2;

    /**
     * VHF PRM code frequency
     */
    public final static int FREQ144 = 144;
    /**
     * UHF PRM code frequency
     */
    public final static int FREQ430 = 430;
    
    public final static int LOCK_NONE = 0;
    public final static int LOCK_KEYS = 1;
    public final static int LOCK_TX = 2;
    
    public final static int POWER_LO = 0;
    public final static int POWER_HI = 1;
    
    public final static int STATE_NORMAL = 0;
    
    public final static int IF = 21400000;
    public final static int PLL_REF_OSC = 10000000;
   
    /**
     * Initialise the communication with de PRM
     * @param port The connection port
     * @return PRM identification code
     */
    public int connectPRM(String port) throws CommunicationException;
    
    /**
     * Terminate the communication with the PRM
     */
    public void disconnectPRM() throws CommunicationException;
    
    /**
     * Get the current state of the PRM
     * @return state
     */
    public int getPRMState() throws CommunicationException;
    
    /**
     * Set the PLL step
     * @param frequency The step (Hertz) of the PLL
     */
    public void setPLLStep(int frequency) throws CommunicationException;
    /**
     * Get the PLL step
     * @return The step (Hertz) of the PLL
     */
    public int getPLLStep() throws CommunicationException;
    
    /**
     * Set the PLL current frequency for reception
     * @param frequency The frequency (Hertz) of the PLL
     */
    public void setRxPLLFrequecny(int frequency) throws CommunicationException;
    /**
     * Get the PLL current frequency for reception
     * @return The frequency (Hertz) of the PLL
     */
    public int getRxPLLFrequency() throws CommunicationException;
    
    /**
     * Set the PLL current frequency for emission
     * @param frequency The frequency (Hertz) of the PLL
     */
    public void setTxPLLFrequecny(int frequency) throws CommunicationException;
    /**
     * Get the PLL current frequency for emission
     * @return The frequency (Hertz) of the PLL
     */
    public int getTxPLLFrequency() throws CommunicationException;
    
    /**
     * Determine if the PLL is locked or not
     * @return true if the PLL is locked
     */
    public boolean isPllLocked() throws CommunicationException;
    
    /** 
     * Get the current volume value
     * @return Volume
     */
    public int readVolume() throws CommunicationException;
    
    /**
     * Set the current volume value
     * @param volume Volume
     */
    public void writeVolume(int volume) throws CommunicationException;
    
    /**
     * Get the current squelch level
     * @return Squelch level
     */
    public int readSquelch() throws CommunicationException;
    
    /**
     * Set the current squelch level
     * @param level Squelch level
     */
    public void writeSquelch(int level) throws CommunicationException;
    
    /**
     * Get the highest channel value programmed in the PRM
     * @return Maximal channel value
     */
    public int getMaxChan() throws CommunicationException;
    
    /**
     * Get the current channel
     * @return Channel
     */
    public int getCurrentChannel() throws CommunicationException;
    
    /**
     * Set the current channel
     * @param channel Channel
     */
    public void setCurrentChannel(int channel) throws CommunicationException;
    
    /**
     * Get the level of HF power
     * @return HF power
     */
    public int getPower() throws CommunicationException;
    
    /**
     * Set the HF power level
     * @param power HF Power
     */
    public void setPower(int power) throws CommunicationException;
       
    /**
     * Force RAM and configuration reset
     */
    public void reloadRAM() throws CommunicationException;
    
    /**
     * Restart the PRM firmware
     */
    public void resetPRM() throws CommunicationException;
    
    /**
     * Obtain the version of the firmware
     * @return Firmware major version
     */
    public int getMajorFirmwareVersion() throws CommunicationException;
    
    /**
     * Obtain the version of the firmware
     * @return Firmware minor version
     */
    public int getMinorFirmwareVersion() throws CommunicationException;
    
    /**
     * Copy the RAM to the EEPROM
     */
    public void RAM2EEPROM() throws CommunicationException;
    
    /**
     * Copy the EEPROM to the RAM
     */
    public void EEPROM2RAM() throws CommunicationException;
    
    /** 
     * Permet d'obtenir une copie de la mémoire du PRM
     * @return
     */
    public MemoryImage getMemoryImage();

    /**
     * Obtain a copy of the list of channels
     * @return list of channels
     */
    public ChannelList getChannels() throws CommunicationException;
    
    /**
     * Set the list of channels
     * @return list of channels
     */
    public void setChannels(ChannelList list) throws CommunicationException;
    
    /**
     * Set listener called when state change
     * @param listener
     */
    public void setPRMStateChangeListener(PRMStateChangeListener listener);
    
    /**
     * Test is the controler is currently connected
     * @return
     */
    public boolean isConnected();
    
    /**
     * Get the PRM80 shift
     * @return TX shift frequency in Hertz
     */
    public int getTxFrequencyShift() throws CommunicationException;
    /**
     * Set the default frequency shift
     * @param frequency TX Frequency in Hertz
     */
    public void setTxFrequencyShift(int frequency) throws CommunicationException;

    /**
     * Get the current scanner speed. Time in ms to wait on each channel before jumping to the next one
     * if squelch is not open
     * @return Wait time in ms
     */
    public int getScanSpeed() throws CommunicationException;

    /**
     * Set the current scanner speed. Time in ms to wait on each channel before jumping to the next one
     * if squelch is not open
     * @param speed Wait time in ms
     */
    public void setScanSpeed(int speed) throws CommunicationException;

    /**
     * Get PRM type constant
     * @return PRM type
     * @throws f4fez.prm80x0.Controler.CommunicationException
     */
    public int getPRMType() throws CommunicationException;

    /**
     * Get PRM the PRM frequency code
     * @return PRM frequency code
     * @throws f4fez.prm80x0.Controler.CommunicationException
     */
    public int getPRMFrequencyCode() throws CommunicationException;
}
