/*
 * VirtualDeviceFirmware.java
 * 
 * Created on 14 déc. 2007, 22:19:30
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

package f4fez.prm80x0;

import f4fez.prm80x0.Controler.CommunicationException;
import f4fez.prm80x0.Controler.Controler;

/**
 *
 * @author Florian
 */
public class VirtualDeviceFirmware {
    
    private Controler controler;
    private boolean vfoMode;
    private int memChan;
    
    public VirtualDeviceFirmware(Controler controler) throws CommunicationException {
        this.controler = controler;
    }
            
    public void increment() throws CommunicationException {
        memChan = this.controler.getCurrentChannel();
        if (this.vfoMode) {
            this.controler.setRxPLLFrequecny(this.controler.getRxPLLFrequency()+this.controler.getPLLStep());
            this.controler.setTxPLLFrequency(this.controler.getTxPLLFrequency()+this.controler.getPLLStep());
        }
        else {
            memChan++;
            if (memChan > this.controler.getMaxChan())
                memChan = 0;
            this.controler.setCurrentChannel(memChan);
        }
    }
    
    public void decrement() throws CommunicationException {
        memChan = this.controler.getCurrentChannel();
        if (this.vfoMode) {
            this.controler.setRxPLLFrequecny(this.controler.getRxPLLFrequency()-this.controler.getPLLStep());
            this.controler.setTxPLLFrequency(this.controler.getTxPLLFrequency()-this.controler.getPLLStep());
        }
        else {
            memChan--;
            if (memChan < 0)
               memChan = this.controler.getMaxChan();
            this.controler.setCurrentChannel(memChan);
        }
    }
    
    public String getChannel() throws CommunicationException {
        String chan = Integer.toString(this.controler.getCurrentChannel());
        if(chan.length() < 2)
            chan = "0"+chan;
        return chan;
    }
    
    public String getRxFrequency() throws CommunicationException {
        String freq = Integer.toString(this.controler.getRxPLLFrequency());
        return freq.substring(0, 3)+"."+freq.substring(3, 6)+"."+freq.substring(6, 9);
      }
    
    public String getTxFrequency() throws CommunicationException {
        String freq = Integer.toString(this.controler.getTxPLLFrequency());
        return freq.substring(0, 3)+"."+freq.substring(3, 6)+"."+freq.substring(6, 9);
  }
    
    public void setVfoMode(boolean mode) throws CommunicationException {
        this.vfoMode = mode;
        if(!mode)
            this.controler.setCurrentChannel(memChan);
    }
    
    public boolean isVfoMode() {
        return this.vfoMode;
    }
    
    public int getVolume() throws CommunicationException {
        return this.controler.readVolume();
    }
    
    public void setVolume(int volume) throws CommunicationException {
        this.controler.writeVolume(volume);
    }
    
    public int getSquelch() throws CommunicationException {
        return this.controler.readSquelch();
    }
    
    public void setSquelch(int level) throws CommunicationException {
        this.controler.writeSquelch(level);
    }
    
    public Controler getPRMControler() {
        return this.controler;
    }
    
    public boolean isHighPower() throws CommunicationException {
        return this.controler.getPower() == this.controler.POWER_HI;
    }
    
    public void setHighPower(boolean power) throws CommunicationException {
        if (power)
            this.controler.setPower(this.controler.POWER_HI);
        else
            this.controler.setPower(this.controler.POWER_LO);
    }
    
    public void disconnect() throws CommunicationException {
        this.controler.disconnectPRM();
    }
    
    public void reset() throws CommunicationException {
        this.controler.resetPRM();
    }
    
    public void setCurrentChannel(int chan) throws CommunicationException {
        this.vfoMode = false;
        this.controler.setCurrentChannel(chan);
    }

    public boolean isPLLLocked() throws CommunicationException {
        return this.controler.isPllLocked();
    }
}
