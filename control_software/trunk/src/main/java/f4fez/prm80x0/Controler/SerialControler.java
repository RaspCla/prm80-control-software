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

import java.util.logging.Level;
import java.util.logging.Logger;
import com.fazecast.jSerialComm.SerialPort;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Serial port connection to the PRM80, based on the jSerialComm library
 * (works on Windows, Linux and macOS without separate native libraries).
 *
 * @author fmazen
 */
public class SerialControler extends PRMControler{
    private SerialPort serialPort;
    /** read timeout the port is configured with */
    private int configuredTimeout = Integer.MIN_VALUE;

    /**
     * List the serial ports of this computer.
     * Windows: "COM3", Linux / macOS: "/dev/ttyUSB0".
     * @return the port names, possibly empty
     */
    public static List<String> listPortNames() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        List<String> names = new ArrayList<String>();
        for (SerialPort p : SerialPort.getCommPorts()) {
            names.add(windows ? p.getSystemPortName() : p.getSystemPortPath());
        }
        return names;
    }

    @Override
    public int connectPRM(String port) throws SerialPortException {
        this.openSerialPort(port);        
        return this.prmType;
    }

    @Override
    public void disconnectPRM() {
        this.connected = false;
        if (this.serialPort != null) {
            this.serialPort.closePort();
        }
    }

    private void openSerialPort(String port) throws SerialPortException {
        if (port == null || port.isEmpty()) {
            throw new SerialPortException("No serial port selected (see Tools / Options)");
        }
        try {
            serialPort = SerialPort.getCommPort(port);
        } catch (Exception ex) {
            throw new SerialPortException("Serial port doesn't exist: " + port);
        }
        serialPort.setComPortParameters(4800, 7, SerialPort.ONE_STOP_BIT, SerialPort.EVEN_PARITY);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, PRMControler.serialTimeout, 0);
        this.configuredTimeout = PRMControler.serialTimeout;
        if (!serialPort.openPort()) {
            throw new SerialPortException("Cannot open serial port " + port
                    + " (in use, missing or no permission"
                    + " - on Linux the user must be member of the group 'dialout')");
        }
        try {
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
            this.prmFreqCode = Integer.parseInt(ident.substring(13, 16));                        
            if (this.prmFreqCode != PRMControler.FREQ144 && this.prmFreqCode != PRMControler.FREQ430)
                throw new SerialPortException("unknown PRM80x0 frequency variant: "+this.prmFreqCode);
            this.connected = true;
            this.getPLLStep();
            this.getMaxChan();
            this.updateState();
        } catch (SerialPortException ex) {
            serialPort.closePort();
            throw ex;
        }
    }

    @Override
    protected void send(String data) {
        // A new command (one letter, e.g. "e", "n", "!"): throw away characters
        // that arrived too late for the previous command, otherwise they would
        // be taken as the answer to this one.
        if (data.length() == 1 && !"\r".equals(data))
            this.discardInput();
        byte[] bytes = data.getBytes(StandardCharsets.ISO_8859_1);
        if (this.serialPort.writeBytes(bytes, bytes.length) < 0) {
            Logger.getLogger(SerialControler.class.getName()).log(Level.SEVERE, "Serial write error");
            return;
        }
        Iterator<SerialListener> i = this.serialListeners.iterator();
        while (i.hasNext()) {
            i.next().dataSent(data);
        }
    }

    @Override
    protected void waitForQuietLine() {
        long end = System.currentTimeMillis() + 3000;
        try {
            do {
                Thread.sleep(150);
                if (this.serialPort.bytesAvailable() <= 0)
                    return;
                this.discardInput();
            } while (System.currentTimeMillis() < end);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /** Read and drop all characters received but not read yet. */
    private void discardInput() {
        int available = this.serialPort.bytesAvailable();
        if (available <= 0)
            return;
        byte[] junk = new byte[available];
        int n = this.serialPort.readBytes(junk, available);
        if (n > 0)
            Logger.getLogger(SerialControler.class.getName()).log(Level.INFO,
                    "Discarded {0} late characters: {1}", new Object[] {n,
                    new String(junk, 0, n, StandardCharsets.ISO_8859_1).replace("\r", "\\r").replace("\n", "\\n")});
    }

    /**
     * Change the read timeout only if needed: jSerialComm reconfigures the
     * whole port for that, on Windows characters can get lost meanwhile.
     */
    private void setReadTimeout(int waitTime) {
        if (waitTime == this.configuredTimeout)
            return;
        if (waitTime > 0) {
            this.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, waitTime, 0);
        } else {
            this.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        }
        this.configuredTimeout = waitTime;
    }

    /**
     * Read one character from the serial port.
     * @param waitTime timeout in ms, 0 or less waits without limit
     * @return the character, or -1 on timeout / error
     */
    private int readChar(int waitTime) {
        this.setReadTimeout(waitTime);
        byte[] b = new byte[1];
        if (this.serialPort.readBytes(b, 1) != 1) {
            return -1;
        }
        String s = new String(b, StandardCharsets.ISO_8859_1);
        Iterator<SerialListener> i = this.serialListeners.iterator();
        while (i.hasNext()) {
            i.next().dataReceived(s);
        }
        return b[0] & 0xFF;
    }

    @Override
    protected String waitChar(char c, int waitTime) {
        StringBuilder rx = new StringBuilder();
        int r;
        do {
            r = this.readChar(waitTime);
            if (r == -1) {
                return null;
            }
            rx.append((char) r);
        } while (r != c);
        return rx.toString();
    }

    @Override
    protected String waitChar(char[] chars, int waitTime) {
        StringBuilder rx = new StringBuilder();
        boolean found = false;
        do {
            int r = this.readChar(waitTime);
            if (r == -1) {
                return null;
            }
            rx.append((char) r);
            for (int j = 0; (j < chars.length) && !found ; j++)
                found = r == chars[j];
        } while (!found);
        return rx.toString();
    }

    // The current values (frequencies, channel, power, PLL lock) are read
    // with the "e" command in PRMControler.updateState() and cached there.
    // Before, every getter sent its own "e" command, so each click caused
    // more than ten serial round trips.
}
