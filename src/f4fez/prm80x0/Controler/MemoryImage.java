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

import java.util.ArrayList;

/**
 *
 * @author fmazen
 */
public abstract class MemoryImage {
    public static int EEPROM_SIZE = 2048;
    public static int RAM_SIZE = 32768;
    public static int IRAM_SIZE = 512;

    private byte[] eeprom;
    private byte[] ram;
    private byte[] iram;
    
    // Declaration des constantes des adresses mémoires
    public static int RAM_CHECK_BYTE = 0;
    
    public ArrayList<MemoryChangeListener> listeners;
    
    public MemoryImage() {
        this.eeprom = new byte[MemoryImage.EEPROM_SIZE];
        this.ram = new byte[MemoryImage.RAM_SIZE];
        this.iram = new byte[MemoryImage.IRAM_SIZE];
    }
    
    /**
     * Load the image of the eeprom
     * @param eeprom data to load
     */
    public void loadEeprom(byte[] eeprom) {
        for (int i=0; i < eeprom.length; i++)
            this.eeprom[i] = eeprom[i];
    }
    
    /**
     * Load the image of the ram
     * @param ram data to load
     */
    public void loadRam(byte[] ram) {
        for (int i=0; i < ram.length; i++)
            this.ram[i] = ram[i];
    }
    /**
     * Load the image of the internal 80C552 ram
     * @param ram data to load
     */
    public void loadInternalRam(byte[] ram) {
        for (int i=0; i < ram.length; i++)
            this.iram[i] = ram[i];
    }    
    /**
     * Get a byte from the eeprom
     * @param adress Adress to read
     * @return
     */
    public byte getEepromData(int adress) {
        return this.eeprom[adress];
    }
    
    /**
     * Get a byte from the ram
     * @param adress Adress to read
     * @return
     */
    public byte getRamData(int adress) {
        return this.ram[adress];
    }
    
    /**
     * Get a byte from the internal 80C552 ram
     * @param adress Adress to read
     * @return
     */
    public byte getInternalRamData(int adress) {
        return this.iram[adress];
    }
    
        /**
     * Set a byte to the eeprom
     * @param adress Adress to read
         * @param value Byte to load
     */
    public void setEepromData(int adress, byte value) {
        this.eeprom[adress] = value;
    }
    
    /**
     * Set a byte to the ram
     * @param adress Adress to read
     * @param value Byte to load
     */
    public void setRamData(int adress, byte value) {
        this.ram[adress] = value;
    }
    
    /**
     * Set a byte to the internal 80C552 ram
     * @param adress Adress to read
     * @param value Byte to load
     */
    public void setInternalRamData(int adress, byte value) {
        this.iram[adress] = value;
    }
    
    /**
     * Copy the first 2kByte from the RAM to the EEPROM
     */
    public void copyRam2Eeprom() {
        for (int i=0; i < MemoryImage.EEPROM_SIZE; i++)
            this.eeprom[i] = this.ram[i];
    }
    /**
     * Copy the first 2kByte from the EEPROM to the RAM
     */
    public void copyEeprom2Ram() {
        for (int i=0; i < MemoryImage.EEPROM_SIZE; i++)
            this.ram[i] = this.eeprom[i];
    }
}
