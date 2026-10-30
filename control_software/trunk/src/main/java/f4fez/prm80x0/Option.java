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

import f4fez.prm80x0.Controler.SerialControler;
import java.util.List;
import java.util.prefs.Preferences;

/**
 *
 * @author fmazen
 */
public class Option {
    private String serialPort;
    private boolean expertMode;
    private Preferences prefs;
    private String lastServerURI;
    
    private final static String prefRoot = "f4fez/prm80";
    private final static String prefSerialPort = "serialPort";
    private final static String prefExpertMode = "expertMode";
    private final static String prefLastURI = "lastServerURI";   
    private final static String prefZoom = "zoom";
    private final static String prefChannelFile = "channelFile";
    private final static String prefA51File = "a51File";
    public Option() {
        this.prefs = Preferences.userRoot().node(prefRoot);
        // Last used serial port, otherwise the first one found
        List<String> ports = SerialControler.listPortNames();
        this.serialPort = this.prefs.get(prefSerialPort, ports.isEmpty() ? "" : ports.get(0));
        this.expertMode = this.prefs.getBoolean(Option.prefExpertMode, false);
        this.lastServerURI = this.prefs.get(Option.prefLastURI, "prm80://127.0.0.1");
    }            
    
    public void setSerialPort(String port) {
        this.serialPort = port;
        this.prefs.put(prefSerialPort, port);
    }
    
    public String getSerialPort() {
        return this.serialPort;
    }
    
    public boolean isExpertMode() {
        return this.expertMode;
    }
    
    public void setEpertMode(boolean expert) {
        this.prefs.putBoolean(Option.prefExpertMode, expert);
        this.expertMode = expert;
    }
    
    public void setLastServerURI(String uri) {
        if (!this.lastServerURI.equals(uri)) {
            this.prefs.put(Option.prefLastURI, uri);        
            this.lastServerURI = uri;
        }
    }
    
    /** Zoom factor of the user interface, 1 = 100 %. */
    public float getZoom() {
        return this.prefs.getFloat(Option.prefZoom, 1f);
    }

    public void setZoom(float zoom) {
        this.prefs.putFloat(Option.prefZoom, zoom);
    }

    /** Channel file (CSV) last read or saved in the channel manager, "" if none. */
    public String getChannelFile() {
        return this.prefs.get(Option.prefChannelFile, "");
    }

    public void setChannelFile(String file) {
        this.prefs.put(Option.prefChannelFile, file == null ? "" : file);
    }

    /** @return the file of the last A51 export, "" if none */
    public String getA51File() {
        return this.prefs.get(Option.prefA51File, "");
    }

    public void setA51File(String file) {
        this.prefs.put(Option.prefA51File, file == null ? "" : file);
    }

    // ---- scan settings ------------------------------------------------------

    /**
     * @param band 144 or 430
     * @return start frequency of the VFO scan in Hz, 0 if not set yet
     */
    public int getScanFrom(int band) {
        return this.prefs.getInt("scanFrom" + band, 0);
    }

    /** @return end frequency of the VFO scan in Hz, 0 if not set yet */
    public int getScanTo(int band) {
        return this.prefs.getInt("scanTo" + band, 0);
    }

    public void setScanRange(int band, int from, int to) {
        this.prefs.putInt("scanFrom" + band, from);
        this.prefs.putInt("scanTo" + band, to);
    }

    /** @return step of the VFO scan in Hz, 0 = PLL step */
    public int getScanStep() {
        return this.prefs.getInt("scanStep", 0);
    }

    public void setScanStep(int step) {
        this.prefs.putInt("scanStep", step);
    }

    /** @return time per channel / frequency of the software scan in ms */
    public int getScanDwell() {
        return this.prefs.getInt("scanDwell", 400);
    }

    public void setScanDwell(int ms) {
        this.prefs.putInt("scanDwell", ms);
    }

    /** @return time in ms the scan waits after a signal has ended */
    public int getScanHold() {
        return this.prefs.getInt("scanHold", 3000);
    }

    public void setScanHold(int ms) {
        this.prefs.putInt("scanHold", ms);
    }

    public String getLastServerURI() {
        return this.lastServerURI;
    }
}
