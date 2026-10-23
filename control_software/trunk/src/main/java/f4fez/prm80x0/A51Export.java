/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package f4fez.prm80x0;

import f4fez.prm80x0.Controler.Channel;
import f4fez.prm80x0.Controler.ChannelList;
import f4fez.prm80x0.Controler.Controler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 *
 * @author f4fez
 */
public class A51Export {
    public static void exportA51(Controler controler, File file) throws Exception {
        PrintWriter out = null;
        try {
            if (file.exists()) {
                file.delete();
            }
            
            ChannelList channels = controler.getChannels();
            String chanCount = Integer.toString(channels.countChannel()-1);
            if (chanCount.length() == 1)
                chanCount = "0"+chanCount;
            out = new PrintWriter(file);
            out.println("CONFIG_CHAN_COUNT        EQU    0"+chanCount+"h");            
            
            String shift = Integer.toHexString(controler.getTxFrequencyShift()/ controler.getPLLStep());
            while(shift.length() < 4)
                    shift = "0" + shift;
            out.println("CONFIG_SHIFT_LO          EQU    0"+shift.substring(2, 4)+"h");
            out.println("CONFIG_SHIFT_HI          EQU    0"+shift.substring(0, 2)+"h");
            
            int r = Controler.PLL_REF_OSC / (2 * controler.getPLLStep());
            String rLo = Integer.toHexString(r & 255);
            if (rLo.length() == 1)
                    rLo = "0" + rLo;
            String rHi = Integer.toHexString((r & 1792) >> 3);
            out.println("CONFIG_PLL_DIV_LO        EQU    0"+rLo+"h");
            out.println("CONFIG_PLL_DIV_HI        EQU    0"+rHi+"h");
            
            if (controler.getMajorFirmwareVersion() > 3)
                out.println("CONFIG_SCAN_DURATION     EQU    008h");
            out.println();
            out.println("freq_list:");
            
            for (int i = 0; i < channels.countChannel(); i++) {
                Channel chan = channels.getChannel(i);
                int freq = chan.getIntFrequency() / controler.getPLLStep();
                String sFreq = Integer.toString(freq, 16);
                while(sFreq.length() < 4)
                    sFreq = "0" + sFreq;
                out.println("        ; Channel : "+chan.getId()+" Frequency : "+chan.getFrequency());
                out.println("        DB      0"+sFreq.substring(0, 2)+"h");
                out.println("        DB      0"+sFreq.substring(2, 4)+"h");
            }
            out.println();
            out.println("chan_state_table:");
            for (int i = 0; i < channels.countChannel(); i++) {
                Channel chan = channels.getChannel(i);
                int state = 0;
                if (chan.isShift()) {
                    state = state + 1;
                    if ("+".equals(chan.getShift()))
                        state = state + 4;
                }
                String sState = Integer.toString(state);
                if (sState.length() == 1)
                    sState = "0"+sState;
                out.println("        DB      "+sState+"h    ; Channel : "+chan.getId());                
            }
            out.println();
        } catch (FileNotFoundException ex) {
            throw new Exception("File not found");
        } finally {
            out.close();
        }        
        
    }
}
