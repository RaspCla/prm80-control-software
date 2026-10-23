/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package f4fez.prm80x0.Controler;

/**
 *
 * @author f4fez
 */
public class MemoryImageV3 extends MemoryImage{
    public static int RAM_ADRESS_ID_CODE = 0x00;
    public static int RAM_ADRESS_CONFIG_SUM = 0x01;
    public static int RAM_ADRESS_FREQ_SUM = 0x02;
    public static int RAM_ADRESS_STATE_SUM = 0x03;
    public static int RAM_ADRESS_CHAN = 0x10;
    public static int RAM_ADRESS_MODE = 0x11;
    public static int RAM_ADRESS_SQUELCH = 0x12;
    public static int RAM_ADRESS_MAX_CHAN = 0x13;
    public static int RAM_ADRESS_SHIFT = 0x14;
    public static int RAM_ADRESS_PLL_DIV_HI = 0x15;
    public static int RAM_ADRESS_PLL_DIV_LO = 0x16;

    public static int RAM_AREA_ADRESS_CONFIG = 0x00;
    public static int RAM_AREA_ADRESS_FREQ = 0x01;
    public static int RAM_AREA_ADRESS_STATE = 0x02;
}
