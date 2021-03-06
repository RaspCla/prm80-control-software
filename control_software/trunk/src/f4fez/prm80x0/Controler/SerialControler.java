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

package f4fez.prm80x0.Controler;

import gnu.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fmazen
 */
public class SerialControler extends PRMControler{
   private SerialPort serialPort;
    private InputStream serialIn;
    private OutputStream serialOut;

    @Override
    public int connectPRM(String port) throws SerialPortException {
        this.openSerialPort(port);        
        return this.prmType;
    }

    @Override
    public void disconnectPRM() {
        this.connected = false;
        try {
            serialIn.close();
            serialOut.close();
            this.serialPort.close();
        }
        catch(Exception e) { }
    }

private void openSerialPort(String port) throws SerialPortException {
            try {
                CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
                if (portIdentifier.isCurrentlyOwned()) {
                    System.out.println("Error: Serial port is currently in use");
                } else {
                    CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);
                    if (commPort instanceof SerialPort) {
                        serialPort = (SerialPort) commPort;
                        serialPort.setSerialPortParams(4800, SerialPort.DATABITS_7, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
                        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);                        
                        serialIn = serialPort.getInputStream();
                        serialOut = serialPort.getOutputStream();
                        String ident = this.sendCommand("v", "^PRM80[67]0 V[3-9].[0-9] [0-9][0-9][0-9]\r\n>$");
                        if (ident == null)
                            throw new SerialPortException("PRM80 not detected");
                        if (ident.contains("PRM8060"))
                            this.prmType = Controler.PRM8060;
                        else if (ident.contains("PRM8070"))
                            this.prmType = Controler.PRM8070;
                        else
                            throw new SerialPortException("Unknown PRM80 device");
                        this.majorFirmwareVersion = Integer.parseInt(ident.substring(9, 10));
                        this.minorFirmwareVersion = Integer.parseInt(ident.substring(11, 12));                        
//                        this.frequencyVariant = Integer.parseInt(ident.substring(13, 16));                        
                        this.prmFreqCode = Integer.parseInt(ident.substring(13, 16));                        
                        if (this.prmFreqCode != PRMControler.FREQ144 && this.prmFreqCode != PRMControler.FREQ430)
                            throw new SerialPortException("unknown PRM80x0 frequency variant: "+this.prmFreqCode);
                        this.connected = true;
                    } else {
                        throw new SerialPortException("Invalid serial port name.");
                    }
                }
            } catch (IOException ex) {
                throw new SerialPortException("I/O error");
            } catch (UnsupportedCommOperationException ex) {
                throw new SerialPortException("Unsupported serial port parameters");
            } catch (PortInUseException ex) {
                throw new SerialPortException("Serial port in use");
            } catch (NoSuchPortException ex) {
                throw new SerialPortException("Serial port doesn't exist");
                
            }
            finally {
//                if (this.serialPort != null)
//                    this.serialPort.close();
            }
    }

    @Override
    protected void send(String data) {
        try {            
            this.serialOut.write(data.getBytes());
            Iterator<SerialListener> i = this.serialListeners.iterator();
            while (i.hasNext()) {
                i.next().dataSent(data);
            }
        } catch (IOException ex) {
            Logger.getLogger(SerialControler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    protected String waitChar(char c, int waitTime) {
        StringBuffer rx = new StringBuffer();
        try {
            if (waitTime > 0) {
                this.serialPort.enableReceiveTimeout(waitTime);
            } else {
                this.serialPort.disableReceiveTimeout();
            }
            try {
                byte[] b = new byte[1];

                do {
                    b[0] = (byte) this.serialIn.read();
                    if (b[0] == -1) {
                        return null;
                    }
                    String s = new String(b);
                    rx.append(s);
                    Iterator<SerialListener> i = this.serialListeners.iterator();
                    while (i.hasNext()) {
                        i.next().dataReceived(s);
                    }
                } while (b[0] != c);
            } catch (IOException ex) {
                Logger.getLogger(SerialControler.class.getName()).log(Level.SEVERE, null, ex);
            }
            return rx.toString();
        } catch (UnsupportedCommOperationException ex) {
            Logger.getLogger(SerialControler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rx.toString();
    }

    @Override
    protected String waitChar(char[] chars, int waitTime) {
        StringBuffer rx = new StringBuffer();
        try {
            if (waitTime > 0) {
                this.serialPort.enableReceiveTimeout(waitTime);
            } else {
                this.serialPort.disableReceiveTimeout();
            }
            try {
                byte[] b = new byte[1];
                boolean found = false;
                do {
                    b[0] = (byte) this.serialIn.read();
                    if (b[0] == -1) {
                        return null;
                    }
                    String s = new String(b);
                    rx.append(s);
                    Iterator<SerialListener> i = this.serialListeners.iterator();
                    while (i.hasNext()) {
                        i.next().dataReceived(s);
                    }
                    for (int j = 0; (j < chars.length) && !found ; j++)
                    found = b[0] != chars[j];
                } while (!found);
            } catch (IOException ex) {
                Logger.getLogger(SerialControler.class.getName()).log(Level.SEVERE, null, ex);
            }
            return rx.toString();
        } catch (UnsupportedCommOperationException ex) {
            Logger.getLogger(SerialControler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rx.toString();
    }
    @Override
    public int getRxPLLFrequency() {      
        Integer pllFreq;
        String ident = this.sendCommand("e", "^[0-9A-F]{22}\r\n>$");
//        this.waitChar('>', PRMControler.serialTimeout);
// this.wait does always run into timeout because ">" was already received in line above
        pllFreq = Integer.parseUnsignedInt(ident.substring(12, 16),16)* this.getPLLStep();        // PLL Frequency
        if (this.prmFreqCode == PRMControler.FREQ144) 
            this.rxFreq = pllFreq - 21400000;                                                       // If 430 Mhz Band Receive frequency is 21.4 Mhz higher than Receive PLL frequency (IF) 
        else
            this.rxFreq = pllFreq + 21400000;                                                       // If 144 Mhz Band Receive frequency is 21.4 Mhz higher than Receive PLL frequency (IF) 
         
        return this.rxFreq;
    }
    
    @Override
    public int getTxPLLFrequency() {      
        Integer pllFreq;
        String ident = this.sendCommand("e", "^[0-9A-F]{22}\r\n>$");
//        this.waitChar('>', PRMControler.serialTimeout);
     
        pllFreq = Integer.parseUnsignedInt(ident.substring(16, 20),16)* this.getPLLStep();        // PLL Frequency
        this.txFrreq = pllFreq;                                                       // If 430 Mhz Band Receive frequency is 21.4 Mhz higher than Receive PLL frequency (IF) 
         
        return this.txFrreq;
    }
    
   @Override
    public int getCurrentChannel() {        
        String ident = this.sendCommand("e", "^[0-9A-F]{22}\r\n>$");
//        this.waitChar('>', PRMControler.serialTimeout);
        this.channel = Integer.parseUnsignedInt(ident.substring(2, 4),16);

        return this.channel;
    }   
    
   @Override
    public boolean isPllLocked() {
        String ident = this.sendCommand("e", "^[0-9A-F]{22}\r\n>$");
//        this.waitChar('>', PRMControler.serialTimeout);
        this.mode = Integer.parseUnsignedInt(ident.substring(0, 2),16);
        return (this.mode & 16) == 16;
    }
 
    public int getPower() {
        String ident = this.sendCommand("e", "^[0-9A-F]{22}\r\n>$");
//        this.waitChar('>', PRMControler.serialTimeout);
        this.mode = Integer.parseUnsignedInt(ident.substring(0, 2),16);
        if ( (this.mode & 2) == 2)
            return SerialControler.POWER_LO;
        else
            return SerialControler.POWER_HI;
    }

}
