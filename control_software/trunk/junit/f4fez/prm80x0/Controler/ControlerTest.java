/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package f4fez.prm80x0.Controler;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author f4fez
 */
public abstract class ControlerTest {

    protected Controler instance;
    protected String port;
    
    public ControlerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public abstract void setUp();

    @After
    public void tearDown() {
        try {
            if (instance != null && instance.isConnected()) {
                instance.disconnectPRM();
            }
        } catch (CommunicationException ex) {
            Logger.getLogger(ControlerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test of connectPRM method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testConnectPRM() throws Exception {
        System.out.println("connectPRM");
        int result = instance.connectPRM(port);
        switch (result) {
            case Controler.PRM8060:
                System.out.println("   PRM8060 detected");
                break;
            case Controler.PRM8070:
                System.out.println("   PRM8070 detected");
                break;
            default:
                fail("Unknow PRM80 device type");
        }
        if (!instance.isConnected())
            fail("PRM80 is not connected");
    }

    /**
     * Test of disconnectPRM method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testDisconnectPRM() throws Exception {
        System.out.println("disconnectPRM");
        int result = instance.connectPRM(port);
        if (!instance.isConnected())
            fail("Can't connect device");
        instance.disconnectPRM();
        if (instance.isConnected())
            fail("PRM80 is still connected");
    }

    /**
     * Test of getPRMState method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetPRMState() throws Exception {
        System.out.println("getPRMState");
        instance.connectPRM(port);
        int result = instance.getPRMState();
        System.out.println("   PRM80 state : "+ Integer.toHexString(result).toUpperCase());
    }

    /**
     * Test of setPLLStep method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetPLLStep() throws Exception {
        System.out.println("setPLLStep");
        instance.connectPRM(port);
        int frequency = instance.getPLLStep();
        instance.setPLLStep(6250);
        if (instance.getPLLStep() != 6250)
            fail("Can't set PLL step frequency");
        instance.setPLLStep(25000);
        if (instance.getPLLStep() != 25000)
            fail("Can't set PLL step frequency");
        instance.setPLLStep(frequency);
        if (instance.getPLLStep() != frequency)
            fail("Can't set PLL step frequency");
    }

    /**
     * Test of getPLLStep method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetPLLStep() throws Exception {
        System.out.println("getPLLStep");
        instance.connectPRM(port);
        int result = instance.getPLLStep();
        System.out.println("   PRM80 PLL step : "+ Integer.toHexString(result).toUpperCase());
    }

    /**
     * Test of getRxPLLFrequency method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetRxPLLFrequency() throws Exception {
        System.out.println("getRxPLLFrequency");
        instance.connectPRM(port);
        int result = instance.getRxPLLFrequency();
        System.out.println("   PRM80 PLL RX frequency : "+ Integer.toString(result));
    }
    
    /**
     * Test of setRxPLLFrequecny method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetRxPLLFrequecny() throws Exception {
        System.out.println("setRxPLLFrequecny");
        instance.connectPRM(port);
        int frequency = instance.getRxPLLFrequency();
        instance.setRxPLLFrequecny(144000000);
        if (instance.getRxPLLFrequency() != 144000000)
            fail("Can't set RX frequency");
        instance.setRxPLLFrequecny(146000000);
        if (instance.getRxPLLFrequency() != 146000000)
            fail("Can't set RX frequency");
        instance.setRxPLLFrequecny(frequency);        
        if (instance.getRxPLLFrequency() != frequency)
            fail("Can't set RX frequency");
    }

    /**
     * Test of getTxPLLFrequency method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetTxPLLFrequency() throws Exception {
        System.out.println("getTxPLLFrequency");
        instance.connectPRM(port);
        int result = instance.getTxPLLFrequency();
        System.out.println("   PRM80 PLL TX frequency : "+ Integer.toString(result));
    }
    
    /**
     * Test of setTxPLLFrequency method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testsetTxPLLFrequency() throws Exception {
        System.out.println("setTxPLLFrequency");
        instance.connectPRM(port);
        int frequency = instance.getTxPLLFrequency();
        instance.setTxPLLFrequency(144000000);        
        if (instance.getTxPLLFrequency() != 144000000)
            fail("Can't set TX frequency");
        instance.setTxPLLFrequency(146000000);
        if (instance.getTxPLLFrequency() != 146000000)
            fail("Can't set TX frequency");
        instance.setTxPLLFrequency(frequency);        
        if (instance.getTxPLLFrequency() != frequency)
            fail("Can't set TX frequency");
    }


    /**
     * Test of isPllLocked method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testIsPllLocked() throws Exception {
        System.out.println("isPllLocked");
        instance.connectPRM(port);
        boolean result = instance.isPllLocked();
        if (!result)
            fail("PLL is NOT locked");
    }

    /**
     * Test of readVolume method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testReadVolume() throws Exception {
        System.out.println("readVolume");
        instance.connectPRM(port);
        int result = instance.readVolume();
        System.out.println("   PRM80 Volume : "+ Integer.toString(result));
    }

    /**
     * Test of writeVolume method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testWriteVolume() throws Exception {
        System.out.println("writeVolume");
        instance.connectPRM(port);

        for (int i = 0; i <= 16; i++) {
            instance.writeVolume(i);
            int vol = instance.readVolume();
            if (vol != i)
                fail("Volume level "+Integer.toString(i)+" not set");
        }
    }

    /**
     * Test of readSquelch method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testReadSquelch() throws Exception {
        System.out.println("readSquelch");
        instance.connectPRM(port);
        int result = instance.readSquelch();
        System.out.println("   PRM80 Squelch level : "+ Integer.toString(result));
    }

    /**
     * Test of writeSquelch method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testWriteSquelch() throws Exception {
        System.out.println("writeSquelch");
        instance.connectPRM(port);

        for (int i = 0; i <= 15; i++) {
            instance.writeSquelch(i);
            int sql = instance.readSquelch();
            if (sql != i)
                fail("Squelch level "+Integer.toString(i)+" not set");
        }
    }

    /**
     * Test of getMaxChan method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetMaxChan() throws Exception {
        System.out.println("getMaxChan");
        instance.connectPRM(port);
        int result = instance.getMaxChan();
        System.out.println("   PRM80 channel count : "+ Integer.toString(result));
    }

    /**
     * Test of getCurrentChannel method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetCurrentChannel() throws Exception {
        System.out.println("getCurrentChannel");
        instance.connectPRM(port);
        int result = instance.getCurrentChannel();
        System.out.println("   PRM80 Current channel : "+ Integer.toString(result));
    }

    /**
     * Test of setCurrentChannel method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetCurrentChannel() throws Exception {
        System.out.println("setCurrentChannel");
        instance.connectPRM(port);
        int channelMax = instance.getMaxChan();
        for (int i = 0; i < channelMax; i++) {
            instance.setCurrentChannel(i);
            int channel = instance.getCurrentChannel();
            if (channel != i)
                fail("Channel "+Integer.toString(i)+" not set");
        }
    }

    /**
     * Test of getPower method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetPower() throws Exception {
        System.out.println("getPower");
        instance.connectPRM(port);
        int result = instance.getPower();
        switch (result) {
            case Controler.POWER_LO:
                System.out.println("   PRM80 power : Low");
                break;
            case Controler.POWER_HI:
                System.out.println("   PRM80 power : High");
                break;
            default:
                fail("Wrong value for power");
        }
    }

    /**
     * Test of setPower method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetPower() throws Exception {
        System.out.println("setPower");
        instance.connectPRM(port);
        
        instance.setPower(Controler.POWER_HI);
        if (instance.getPower() != Controler.POWER_HI)
            fail("Could not set HIGH power");
        instance.setPower(Controler.POWER_LO);
        if (instance.getPower() != Controler.POWER_LO)
            fail("Could not set LOW power");
    }

    /**
     * Test of reloadRAM method, of class Controler.
     */
    /*@Test
    public void testReloadRAM() throws Exception {
        System.out.println("reloadRAM");
        instance.connectPRM(port);
        instance.reloadRAM();
        int result = instance.getCurrentChannel();
        if (result != 0)
            fail("PRM RAM not reloaded");
        System.out.println("   PRM80 Current channel : "+ Integer.toString(result));
    }*/

    /**
     * Test of resetPRM method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testResetPRM() throws Exception {
        System.out.println("resetPRM");
        instance.connectPRM(port);
        instance.resetPRM();
    }

    /**
     * Test of getMajorFirmwareVersion method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetMajorFirmwareVersion() throws Exception {
        System.out.println("getMajorFirmwareVersion");
        instance.connectPRM(port);
        int result = instance.getMajorFirmwareVersion();
        System.out.println("   PRM80 Major version : "+ Integer.toString(result));
        if (result < 3)
            fail("PRM80 major version shoult be a least 3");
    }

    /**
     * Test of getMinorFirmwareVersion method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetMinorFirmwareVersion() throws Exception {
        System.out.println("getMinorFirmwareVersion");
        instance.connectPRM(port);
        int result = instance.getMinorFirmwareVersion();
        System.out.println("   PRM80 Minor version : "+ Integer.toString(result));
    }

    /**
     * Test of RAM2EEPROM method, of class Controler.
     */
    /*@Test
    public void testRAM2EEPROM() throws Exception {
        System.out.println("RAM2EEPROM");
        instance.connectPRM(port);
        instance.RAM2EEPROM();
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of EEPROM2RAM method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testEEPROM2RAM() throws Exception {
        System.out.println("EEPROM2RAM");
        instance.connectPRM(port);
        instance.EEPROM2RAM();
        MemoryImage result = instance.getMemoryImage();
        for (int i = 0; i < MemoryImage.EEPROM_SIZE; i++)
            if (result.getEepromData(i) != result.getRamData(i))
                fail("RAM not equal to EEPROM at adress "+Integer.toHexString(i).toUpperCase());
    }

    /**
     * Test of getMemoryImage method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetMemoryImage() throws Exception {
        System.out.println("getMemoryImage");
        instance.connectPRM(port);
        MemoryImage result = instance.getMemoryImage();
        if (result == null)
            fail("Memory image can't be null");
    }

    /**
     * Test of getChannels method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetChannels() throws Exception {
        System.out.println("getChannels");
        instance.connectPRM(port);
        ChannelList result = instance.getChannels();
        int countChan = instance.getMaxChan()+1;
        if (result == null || result.countChannel() == 0)
            fail("No channels found");
        else if (result.countChannel() != countChan) {
            fail("MaxChan different of counted channels");}
    }

    /**
     * Test of setChannels method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetChannels() throws Exception {
        System.out.println("setChannels");
        instance.connectPRM(port);
        ChannelList oldList = instance.getChannels();
        ChannelList list = new ChannelList();
        list.addChannel(new Channel("144000000", "0", "", "", "", ""));
        list.addChannel(new Channel("145100000", "0", "", "", "", ""));
        list.addChannel(new Channel("145125000", "600000", "", "-", "", "Enabled"));
        list.addChannel(new Channel("145150000", "600000", "", "+", "", "Enabled"));
        list.addChannel(new Channel("145175000", "600000", "", "-", "Reverse", "Enabled"));
        list.addChannel(new Channel("145200000", "0", "Locked", "", "", ""));
        instance.setChannels(list);
        ChannelList newList = instance.getChannels();
        if (newList.countChannel() != 2)
            fail("Channel list not set");
        instance.setChannels(oldList);        
    }

    /**
     * Test of setPRMStateChangeListener method, of class Controler.
     */
    /*@Test
    public void testSetPRMStateChangeListener() {
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of isConnected method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testIsConnected() throws Exception {
        System.out.println("getTxFrequencyShift");
        instance.connectPRM(port);
        if(instance.isConnected() == false) {
            fail("PRM not connected");
        }
    }

    /**
     * Test of getTxFrequencyShift method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetTxFrequencyShift() throws Exception {
        System.out.println("getTxFrequencyShift");
        instance.connectPRM(port);
        int result = instance.getTxFrequencyShift();
        System.out.println("   Frequency shift : "+ Integer.toString(result));
    }

    /**
     * Test of setTxFrequencyShift method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetTxFrequencyShift() throws Exception {
        fail("The test case is a prototype.");
    }

    /**
     * Test of getScanSpeed method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetScanSpeed() throws Exception {
        System.out.println("getScanSpeed");
        instance.connectPRM(port);
        int result = instance.getScanSpeed();
        System.out.println("   Scan speed : "+ Integer.toString(result));
    }

    /**
     * Test of setScanSpeed method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetScanSpeed() throws Exception {
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPRMType method, of class Controler.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetPRMType() throws Exception {
        System.out.println("getPRMType");
        instance.connectPRM(port);
        int result = instance.getPRMType();
        System.out.println("   PRM80 type : "+ Integer.toString(result));
    }
}