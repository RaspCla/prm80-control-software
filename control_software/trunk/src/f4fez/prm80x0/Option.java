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

import gnu.io.CommPortIdentifier;
import java.util.Enumeration;
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
    public Option() {
        this.prefs = Preferences.userRoot().node(prefRoot);
        Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
        while (thePorts.hasMoreElements()) {
            CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
            if (com.getPortType() == CommPortIdentifier.PORT_SERIAL ) {
                this.serialPort = this.prefs.get(prefSerialPort, com.getName());
                break;
            }
        }
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
    
    public String getLastServerURI() {
        return this.lastServerURI;
    }
}
