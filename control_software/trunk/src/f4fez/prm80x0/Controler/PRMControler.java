/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package f4fez.prm80x0.Controler;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author f4fez
 */
public abstract class PRMControler implements Controler{
    protected static int serialTimeout = 1000;
    protected int majorFirmwareVersion;
    protected int minorFirmwareVersion;
    protected int frequencyVariant;
    protected int prmType;
    protected int prmFreqCode;
    private int pllStep; 
    protected ArrayList<SerialListener> serialListeners;
    protected Thread updateThread;
    protected int updateSleepTime;
    protected boolean connected;
    protected PRMStateChangeListener changeListener;
    private String holdStateString;
    
    protected int rxFreq = 999999999; 
    protected int txFrreq = 999999999;
    protected int channel;
    protected int squelch;
    protected int volume;
    protected int mode;
    protected int maxChan = -1;
        
    /**
     * Number of retry for each command
     */
    protected final static int RETRY = 5;
    
    /**
     * Hold Memory dump
     */
    protected MemoryImage memoryImage = null;
    
    /**
     * Default PRMControler constructor
     */
    public PRMControler() {
        this.serialListeners = new ArrayList<SerialListener>();
    }
    
    @Override
    public abstract int connectPRM(String port) throws SerialPortException;

    @Override
    public abstract void disconnectPRM();

    @Override
    public int getPRMState() {
        return SerialControler.STATE_NORMAL;
    }

    @Override
    public void setPLLStep(int frequency) {
        int r = PLL_REF_OSC / (2 * frequency);
        int data[] = new int[2];
        data[1] = r & 255;
        data[0] = (r & 1792) >> 3;
        int ramPos;
        if (this.getMajorFirmwareVersion() == 3)
            ramPos = MemoryImageV3.RAM_ADRESS_PLL_DIV_HI;
        else
            ramPos = MemoryImageV4.RAM_ADRESS_PLL_DIV_HI;
        if (this.writeRamByte(ramPos, data))
            this.pllStep = frequency;        
    }

    @Override
    public int getPLLStep() {
        if (this.pllStep == 0)
            this.loadPllStep();
        return this.pllStep;
    }

    @Override
    public void setRxPLLFrequecny(int frequency) {
        this.setPLLFrequencies(frequency, this.getTxPLLFrequency());
        this.updateState();
    }

    @Override
    public int getRxPLLFrequency() {        
        return this.rxFreq;
    }
    
    @Override
    public void setTxPLLFrequecny(int frequency) {
        this.setPLLFrequencies(this.getRxPLLFrequency(), frequency);
        this.updateState();
    }


    @Override
    public int getTxPLLFrequency() {
        return this.txFrreq;
    }
    
    /**
     * Load into the prm PLL values for TX and RX frequency
     * @param rxFreq The RX frequency
     * @param txFreq The TX frequency
     */
    protected synchronized void setPLLFrequencies(int rxFreq, int txFreq) {
        int rxfreq;
        if (this.prmFreqCode == PRMControler.FREQ144) {
            rxfreq = (rxFreq + Controler.IF) / this.getPLLStep();
        }
        else {
            rxfreq = (rxFreq - Controler.IF) / this.getPLLStep();
        }
        int txfreq = (txFreq) / this.getPLLStep();
        this.send("r");
        this.waitChar(':', PRMControler.serialTimeout);
        String sFreq = Integer.toString(rxfreq, 16);
        while(sFreq.length() < 4)
            sFreq = "0" + sFreq;
        this.send(sFreq);
        this.waitChar(':', PRMControler.serialTimeout);
        sFreq = Integer.toString(txfreq, 16);
        while(sFreq.length() < 4)
            sFreq = "0" + sFreq;
        this.send(sFreq);
        this.waitChar('>', PRMControler.serialTimeout);
        this.updateState();
    }
    
    @Override
    public boolean isPllLocked() {
        return (mode & 16) == 16;
    }

    @Override
    public int readVolume() {
        return this.volume;
    }

    @Override
    public void writeVolume(int volume) {
        
    }

    @Override
    public int readSquelch() {
        return this.squelch;
    }

    @Override
    public synchronized void writeSquelch(int level) {
        this.send("f");
        this.waitChar(':', PRMControler.serialTimeout);
        String sChan = Integer.toString(level);
        if (sChan.length() == 1)
            sChan = "0"+sChan;
        this.send(sChan);
        this.waitChar('>', PRMControler.serialTimeout);
        this.updateState();
    }

    @Override
    public int getMaxChan() {
        if (this.maxChan == -1) {
            int[] data = readRamByte(MemoryImageV3.RAM_ADRESS_MAX_CHAN, 1);
            this.maxChan = data[0];
        }
        return this.maxChan;
    }
    
    /**
     * Set number of programmed channels.
     * @param chanCount Number of channel used
     * @return True if ok
     */
    public synchronized boolean setMaxChan(int chanCount) {
        boolean ok = true;
        for (int retryLoop = 0; retryLoop < PRMControler.RETRY; retryLoop++) {
            ok = true;
            if (retryLoop > 0)
                this.sendEscapeCommand();
        
            this.send("q");
            String commandResponse = this.waitChar(':', PRMControler.serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }                
            if (!commandResponse.equals("Channels number (00 to 99) :")) {
                ok = false;
                continue;
            }
            String chans = Integer.toString(chanCount);
            if (chans.length() == 1)
                chans = "0"+chans;
            this.send(chans);
            commandResponse = this.waitChar('>', PRMControler.serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }                
            if (!commandResponse.equals(" "+chans+"\r\n\r\n>")) {
                ok = false;
                continue;
            }
            
            if (!ok)
                continue;
            else
                break;
        }
        
        if (ok)
            this.maxChan = chanCount;
        return ok;
    }

    @Override
    public int getCurrentChannel() {        
        return this.channel;
    }

    @Override
    public synchronized void setCurrentChannel(int channel) {
        this.send("n");
        this.waitChar(':', PRMControler.serialTimeout);
        String sChan = Integer.toString(channel);
        if (sChan.length() == 1)
            sChan = "0"+sChan;
        this.send(sChan);
        this.waitChar('>', PRMControler.serialTimeout);
        this.updateState();
    }

    @Override
    public int getPower() {
        if ( (this.mode & 2) == 2)
            return SerialControler.POWER_LO;
        else
            return SerialControler.POWER_HI;
    }

    @Override
    public synchronized void setPower(int power) {
        int model = Integer.parseInt(this.sendCommand("e").substring(0, 2), 16);
        if (power == Controler.POWER_LO)
            model = model | 2;
        else
            model = model & 253;
        this.mode = model; 
        this.send("d");
        this.waitChar(':', PRMControler.serialTimeout);
        String sMode = Integer.toHexString(model).toUpperCase();
        if (sMode.length() == 1)
            sMode = "0"+sMode;
        this.send(sMode);
        this.waitChar('>', PRMControler.serialTimeout);
        this.updateState();
    }

    @Override
    public void reloadRAM() {
        
    }

    @Override
    public void resetPRM() {
        this.sendCommand("0");
    }

    @Override
    public int getMajorFirmwareVersion() {
        return this.majorFirmwareVersion;
    }

    @Override
    public int getMinorFirmwareVersion() {
        return this.minorFirmwareVersion;
    }
    
    @Override
    public void RAM2EEPROM() {
        this.sendCommand("x");
    }

    @Override
    public void EEPROM2RAM() {
        this.sendCommand("s");
    }    
    
    /**
     * Send a command to the prm and wait the answer and check if the answer is correct.
     * The command is sent again until the answer is correct or the retry counter overflow
     * @param command Command code
     * @param regex Filter to check if the answer is correct
     * @return Command answer
     */
    protected String sendCommand (String command, String regex) {
        return this.sendCommand(command, PRMControler.serialTimeout, regex);
    }
    
    /**
     * Send a command to the prm and wait the answer
     * @param command Command code
     * @return Command answer
     */
    protected String sendCommand (String command) {
        return this.sendCommand(command, PRMControler.serialTimeout);
    }
    
    /**
     * Send a command to the prm and wait the answer and check if the answer is correct.
     * The command is sent again until the answer is correct or the retry counter overflow
     * @param command Command code
     * @param waitDuration Time to wait for a response
     * @param regex Filter to check if the answer is correct
     * @return Command answer
     */
    protected synchronized String sendCommand (String command, int waitDuration, String regex) {
        String result = null;
        for (int i= 0; i < RETRY && result == null; i++) {
            if (i > 0)
                this.sendEscapeCommand();
            result = this.sendCommand(command, waitDuration);
            if (regex != null && (result == null || !result.matches(regex)))
                result = null;
        }
        return result;
            
    }
    
    /**
     * Send a command to the prm and wait the answer
     * @param command Command code
     * @param waitDuration Time to wait for a response
     * @return Command answer
     */
    protected synchronized String sendCommand (String command, int waitDuration) {
        this.send(command);
        return waitCommandAnswer(waitDuration);
    }
    
    /**
     * Send data to the PRM
     * @param data Data to send
     */
    protected abstract void send(String data);
    
    /**
     * Wait for a command answer, a response end with the ">" character
     * @param waitTime Time to wait for the answer
     * @return Command answer
     */
    protected synchronized String waitCommandAnswer(int waitTime) {
        return this.waitChar('>', waitTime);
    }
    
    /**
     * Wait for a specific character and return all character received before this character
     * @param c Char to wait for
     * @param waitTime Time to wait for the character
     * @return Characters received
     */
    protected abstract String waitChar(char c, int waitTime);
    
    /**
     * Wait for a specific character and return all character received before this character.
     * This method accept multiple characters to wait for. Return when any of them is received
     * @param chars List of characters to wait for
     * @param waitTime Time to wait
     * @return Characters received
     */
    protected abstract String waitChar(char[] chars, int waitTime);
    
    /**
     * Add a lister called when a communication event appear
     * @param list Listener to add
     */
    public void addSerialListener(SerialListener list) {
        this.serialListeners.add(list);
    }
    /**
     * Remov a character previously added
     * @param list Listener to remove
     */
    public void removeSerialListener(SerialListener list) {
        this.serialListeners.remove(list);
    }
    /**
     * Remove all SerialListener
     */
    public void removeSerialListeners() {
        this.serialListeners.clear();
    }

    @Override
    public MemoryImage getMemoryImage() {
        if (this.memoryImage == null) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return this.memoryImage;
    }

    @Override
    public ChannelList getChannels() {
        ChannelList list = new ChannelList();
        String result = null;
        int chanMax = this.getMaxChan();
        
        for (int i= 0; i < RETRY && result == null; i++) {
            int chanCount = 0;
            list.clear();
            if (i > 0)
                this.sendEscapeCommand();
            result = this.sendCommand("c");
            
            StringTokenizer tokenizer = new StringTokenizer(result, "\r\n");
            if (!tokenizer.hasMoreTokens() || !tokenizer.nextToken().equals("Channel Frequency Shift State")) {
                result = null;
            }
            else {
                while (tokenizer.hasMoreTokens() && result != null) {
                    String line = tokenizer.nextToken();
                    if (chanCount <= chanMax) {
                        if (line.matches("^[0-9]{2} [0-9A-F]{4} [0-9A-F]{4} [0-9A-F]{2}$")) {
                            int chanNum = Integer.parseInt(line.substring(0, 2), 10);
                            int freq = Integer.parseInt(line.substring(3, 7), 16);
                            int shiftFreq = Integer.parseInt(line.substring(8, 12), 16);
                            int state = Integer.parseInt(line.substring(13, 15), 16);
                            if (chanNum == chanCount++) {

                                String scanLock = "";
                                if ((state & 8) != 0)
                                    scanLock ="Lock";

                                String shiftReverse = "";
                                if ((state & 2) != 0)
                                    shiftReverse ="Reverse";

                                String shiftEnabled = "";
                                String shift = "";
                                if ((state & 1) != 0) {
                                    shiftEnabled ="Enabled";
                                    if ((state & 4) == 0) 
                                    shift="-";
                                    else
                                    shift="+";       
                                }    

                                Channel chan = new Channel(freq * this.getPLLStep(), shiftFreq * this.getPLLStep(), scanLock, shiftReverse, shiftEnabled, shift);
                                list.addChannel(chan);
                            } else {
                                result = null;
                            }
                        }
                        else {
                            result = null;
                        }
                    }
                    else { // last line
                        
                    }
                }
            }

        }
        if (result != null)
            return list;
        else
            return null;
    }

    @Override
    public void setChannels(ChannelList list) {
        for (int i=0; i < list.countChannel();i++)
            this.setChannel(list.getChannel(i));
        
        this.setMaxChan(list.countChannel() - 1);
    }

    /**
     * Ask for current prm80 state value and load them into variables
     */
    protected synchronized void updateState() {
        String stateLine = this.sendCommand("e", "^[0-9A-F]{22}\r\n>$");
        
        if (stateLine != null) {
            try {
                if (stateLine != null && stateLine.length() == 23 && !stateLine.equals(this.holdStateString)) {
                    int freq = Integer.parseInt(stateLine.substring(12, 16), 16);
                    if (this.prmFreqCode == PRMControler.FREQ144) {
                        this.rxFreq = freq*this.getPLLStep()-Controler.IF;
                    }
                    else {
                        this.rxFreq = freq*this.getPLLStep()+Controler.IF;
                    }
                    
                    freq = Integer.parseInt(stateLine.substring(16, 20), 16);
                    this.txFrreq = freq*this.getPLLStep();
                    this.volume =  (255-Integer.parseInt(stateLine.substring(8, 10), 16)) >> 4;
                    this.squelch = Integer.parseInt(stateLine.substring(6, 8), 16);
                    this.channel = Integer.parseInt(stateLine.substring(2, 4), 16);
                    this.mode = Integer.parseInt(stateLine.substring(0, 2), 16);
                    this.holdStateString = stateLine;
                    if (this.changeListener != null)
                        this.changeListener.stateUpdated();
                }
            } catch (NumberFormatException e) {
            }
        }
    }
    
    /**
     * Launch an new thread to update prm80 values periodicaly
     * @param sleepTime Time to wait between each update
     */
    protected void runUpdateThread(int sleepTime) {
        this.updateSleepTime = sleepTime;
        this.updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(connected) {
                        updateState();
                        Thread.sleep(updateSleepTime);
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(PRMControler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        });
        this.updateThread.start();
    }
    
    @Override
    public void setPRMStateChangeListener(PRMStateChangeListener listener) {
        this.changeListener = listener;
    }
    
    @Override
    public boolean isConnected() {
        return this.connected;
    }
    
    /**
     * Send an escape code to cancel the current command. 
     * @return true on sucess (Prompt received)
     */
    public boolean sendEscapeCommand() {
        this.send("!");
        return waitCommandAnswer(PRMControler.serialTimeout) != null;
    }
    
    /**
     * Read bytes from the PRM80 external RAM
     * @param adress Start adress to read
     * @param length Number of bytes to read
     * @return Array of data read from the RAM
     */
    private synchronized int[] readRamByte(int adress, int length) {
        int[] data = new int[length];
        boolean ok = true;
        
        for (int retryLoop = 0; retryLoop < PRMControler.RETRY; retryLoop++) {
            ok = true;
            if (retryLoop > 0)
                this.sendEscapeCommand();
            this.send("m");
            String commandResponse = this.waitChar('$', PRMControler.serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }                
            if (!commandResponse.equals("External RAM adress $")) {
                ok = false;
                continue;
            }
            commandResponse = this.waitChar('$', PRMControler.serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }
            if (!commandResponse.equals("XXXX : $")) {
                ok = false;
                continue;
            }
            String hexAdress = Integer.toHexString(adress);            
            while (hexAdress.length() < 4)
                hexAdress = "0".concat(hexAdress);
            this.send(hexAdress);
            commandResponse = this.waitChar('$', PRMControler.serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }
            if (!commandResponse.equals(hexAdress+"\r\n$")) {
                ok = false;
                continue;
            }
            
            // Values reading loop
            for (int i= 0; i < length; i++) {
                            
                // First read adress
                commandResponse = this.waitChar(' ', PRMControler.serialTimeout);
                if (commandResponse == null) {
                    ok = false;
                    continue;
                }
                if (!commandResponse.equals(hexAdress+" ")) {
                    ok = false;
                    continue;
                }
                // Now read the value
                commandResponse = this.waitChar(' ', PRMControler.serialTimeout);
                if (commandResponse == null) {
                    ok = false;
                    continue;
                }
                if (!commandResponse.matches("^[0-9A-F]{2} $")) {
                    ok = false;
                    continue;
                }

                data[i] = Integer.parseInt(commandResponse.trim(), 16);

                send("\r");
                
                commandResponse = this.waitChar('$', PRMControler.serialTimeout);
                if (commandResponse == null) {
                    ok = false;
                    continue;
                }
                if (!commandResponse.equals("\r\n$")) {
                    ok = false;
                    continue;
                }
                
                hexAdress = Integer.toHexString(adress + i + 1);            
                while (hexAdress.length() < 4)
                    hexAdress = "0".concat(hexAdress);
            }
            if (!ok)
                continue;
            else
                break;
        }
        sendEscapeCommand();
   
        if (ok)
            return data;
        else
            return null;
    }
    
     /**
     * Read bytes from the PRM80 external RAM
     * @param adress Start adress to read
     * @param length Number of bytes to read
     * @return Array of data read from the RAM
     */
    private synchronized boolean writeRamByte(int adress, int[] data) {
        boolean ok = true;
        
        for (int retryLoop = 0; retryLoop < PRMControler.RETRY; retryLoop++) {
            ok = true;
            if (retryLoop > 0)
                this.sendEscapeCommand();
            this.send("m");
            String commandResponse = this.waitChar('$', PRMControler.serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }                
            if (!commandResponse.equals("External RAM adress $")) {
                ok = false;
                continue;
            }
            commandResponse = this.waitChar('$', PRMControler.serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }
            if (!commandResponse.equals("XXXX : $")) {
                ok = false;
                continue;
            }
            String hexAdress = Integer.toHexString(adress);            
            while (hexAdress.length() < 4)
                hexAdress = "0".concat(hexAdress);
            this.send(hexAdress);
            commandResponse = this.waitChar('$', PRMControler.serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }
            if (!commandResponse.equals(hexAdress+"\r\n$")) {
                ok = false;
                continue;
            }
            
            // Values reading loop
            for (int i= 0; i < data.length; i++) {
                            
                // First read adress
                commandResponse = this.waitChar(' ', PRMControler.serialTimeout);
                if (commandResponse == null) {
                    ok = false;
                    continue;
                }
                if (!commandResponse.equals(hexAdress+" ")) {
                    ok = false;
                    continue;
                }
                // Read the previous value
                commandResponse = this.waitChar(' ', PRMControler.serialTimeout);
                if (commandResponse == null) {
                    ok = false;
                    continue;
                }
                if (!commandResponse.matches("^[0-9A-F]{2} $")) {
                    ok = false;
                    continue;
                }
                
                // Send the new value
                String value = Integer.toHexString( (data[i] )).toUpperCase();
                if (value.length() == 1)
                    value = "0" + value;
                send(value);
                
                commandResponse = this.waitChar('$', PRMControler.serialTimeout);
                if (commandResponse == null) {
                    ok = false;
                    continue;
                }
                if (!commandResponse.equals(value+"\r\n$")) {
                    ok = false;
                    continue;
                }
                
                hexAdress = Integer.toHexString(adress + i + 1);            
                while (hexAdress.length() < 4)
                    hexAdress = "0".concat(hexAdress);
            }
            if (!ok)
                continue;
            else
                break;
        }
        sendEscapeCommand();
        
        return ok;
    }
    
    /**
     * Send commands to the PRM to get the pll frequency step
     */
    protected void loadPllStep() {
        int ramPos;
        if (this.getMajorFirmwareVersion() == 3)
            ramPos = MemoryImageV3.RAM_ADRESS_PLL_DIV_HI;
        else
            ramPos = MemoryImageV4.RAM_ADRESS_PLL_DIV_HI;
        int[] pllStepBytes = this.readRamByte(ramPos, 2);
        int r = ((pllStepBytes[0] & 248) << 3) + pllStepBytes[1];
        this.pllStep = PLL_REF_OSC / (r * 2);
    }
    
    /**
     * Program one channel in the PRM
     * @param chan Channel to set up
     * @return True if Ok
     */
    protected synchronized boolean setChannel(Channel chan) {
        boolean ok = true;
        for (int retryLoop = 0; retryLoop < PRMControler.RETRY; retryLoop++) {
            ok = true;
            if (retryLoop > 0)
                this.sendEscapeCommand();
            this.send("p");
            String commandResponse = this.waitChar(':', serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }
            if (!commandResponse.equals("Channel to set :")) {
                ok = false;
                continue;
            }
            String chanNumber = Integer.toString(chan.getId());
            if (chanNumber.length() == 1)
                chanNumber = "0" + chanNumber;
            this.send(chanNumber);
            commandResponse = this.waitChar('$', serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }
            if (!commandResponse.equals(" "+chanNumber+"\r\nPLL value to load : $")) {
                ok = false;
                continue;
            }
            String freq = Integer.toHexString( (chan.getIntFrequency() / this.getPLLStep()) ).toUpperCase();
            while (freq.length() < 4)
                freq = "0" + freq;
            send(freq);

            commandResponse = this.waitChar('$', serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }
            if (!commandResponse.equals(freq+"\r\nShift value : $")) {
                ok = false;
                continue;
            }
            String shiftFreq = Integer.toHexString( (chan.getIntShiftFreq() / this.getPLLStep()) ).toUpperCase();
            while (shiftFreq.length() < 4)
                shiftFreq = "0" + shiftFreq;
            send(shiftFreq);
            

            commandResponse = this.waitChar('$', serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }
            if (!commandResponse.equals(shiftFreq+"\r\nChannel state : $")) {
                ok = false;
                continue;
            }
            
            int state = 0;
            if (chan.isShift()){
                state = state | 1;
                if ("+".equals(chan.shiftPosNeg))
                    state = state | 4; 
                }    
            if ("Reverse".equals(chan.shiftReverse))
                state = state | 2;
            if ("Lock".equals(chan.scanLock))
                state = state | 8;
            
            String stateStr = Integer.toString(state);
            if (stateStr.length() == 1)
                stateStr = "0" + stateStr;
            send(stateStr);
            char[] chars = {'>', '?'};
//            commandResponse = this.waitChar(chars, serialTimeout);
            commandResponse = this.waitChar('>', serialTimeout);
            if (commandResponse == null) {
                ok = false;
                continue;
            }
            boolean sendConfirm = false;
            if (!commandResponse.equals(stateStr+"\r\n\r\n\r\n>")) {
                if (!commandResponse.equals(stateStr+"\r\nThis channel number doesn't exist. Add new channel (Y/N) ?")) {
                    ok = false;
                    continue;
                }
                else {
                    sendConfirm = true;
                }
            }
            
            if (sendConfirm) {
                send("Y");
                commandResponse = this.waitChar('>', serialTimeout);
                if (commandResponse == null) {
                    ok = false;
                    continue;
                }
                if (!commandResponse.equals(" \r\n\r\n\r\n>")) {
                    ok = false;
                    continue;
                }
            }
            
            if (!ok)
                continue;
            else
                break;
        }
        return ok;
    }
    
    @Override
    public int getTxFrequencyShift() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setTxFrequencyShift(int frequency) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
        @Override
    public int getScanSpeed() throws CommunicationException {
        return 10;
    }

    @Override
    public void setScanSpeed(int speed) throws CommunicationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getPRMType() throws CommunicationException {
        return this.prmType;
    }

    @Override
    public int getPRMFrequencyCode() throws CommunicationException {
        return this.prmFreqCode;
    }
}
