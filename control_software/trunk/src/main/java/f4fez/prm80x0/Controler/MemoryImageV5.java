/*
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
 * External RAM layout of firmware V5 (src/inc_xram.a51).
 * Every area is one page of 256 bytes, its checksum is the sum of its bytes
 * (config area: bytes 0x0A..0xFF).
 */
public class MemoryImageV5 extends MemoryImage {
    public static final int RAM_ADRESS_ID_CODE = 0x00;
    public static final int RAM_ADRESS_CONFIG_SUM = 0x01;
    public static final int RAM_ADRESS_FREQ_SUM = 0x02;
    public static final int RAM_ADRESS_SHIFT_SUM = 0x03;
    public static final int RAM_ADRESS_STATE_SUM = 0x04;
    public static final int RAM_ADRESS_CHAN = 0x10;
    public static final int RAM_ADRESS_MODE = 0x11;
    public static final int RAM_ADRESS_SQUELCH = 0x12;
    public static final int RAM_ADRESS_MAX_CHAN = 0x13;
    public static final int RAM_ADRESS_SHIFT_HI = 0x14;
    public static final int RAM_ADRESS_SHIFT_LO = 0x15;
    public static final int RAM_ADRESS_PLL_DIV_HI = 0x16;
    public static final int RAM_ADRESS_PLL_DIV_LO = 0x17;
    public static final int RAM_ADRESS_SCAN_DURATION = 0x18;

    /** ID_CODE: RAM content is valid */
    public static final int ID_CODE = 0x40;

    public static final int RAM_AREA_ADRESS_CONFIG = 0x00;
    /** 2 bytes per channel (hi, lo): PLL word of the frequency */
    public static final int RAM_AREA_ADRESS_FREQ = 0x01;
    /** 2 bytes per channel (hi, lo): channel specific shift, 0 = default shift */
    public static final int RAM_AREA_ADRESS_SHIFT = 0x02;
    /** 1 byte per channel: b0 shift enabled, b1 reverse, b2 positive, b3 scan lock out */
    public static final int RAM_AREA_ADRESS_STATE = 0x03;
}
