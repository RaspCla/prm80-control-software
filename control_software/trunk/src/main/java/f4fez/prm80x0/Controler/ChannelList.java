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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fmazen
 */
public class ChannelList{
    private ArrayList<Channel> list;

    public ChannelList() {
        this.list = new ArrayList<Channel>();
    }

    public Channel getChannel(int channel) {
        return this.list.get(channel);
    }

    public void addChannel(Channel channel) {
        this.list.add(channel);
        channel.setId(this.list.indexOf(channel));
    }

    public int countChannel() {
        return this.list.size();
    }
    
    public void clear() {
        this.list.clear();
    }
    
    public void exportCSV(String fileName) throws Exception {
        PrintWriter out = null;
        try {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
            out = new PrintWriter(file);
            out.println( "Chan Id;Frequency;ShiftFrequency;ScanLock;ShifPosNeg;ShiftReverse;ShiftEnabled;Comments");
            for (int i = 0; i < this.list.size(); i++) {
                Channel chan = this.list.get(i);
                out.println(    Integer.toString(chan.getId()) 
                                + ";"  + chan.getFrequency()
                                + ";"  + chan.getShiftFreq()
                                + ";"  + chan.getScanLock()
                                + ";"  + chan.getShift()
//                                + ";"  + chan.getShiftPosNeg()
                                + ";"  + chan.getShiftReverse()
                                + ";"  + chan.getShiftEnabled()
                                + ";"  + chan.getComments()
                                );
            }

            out.flush();
            out.close();
        } catch (FileNotFoundException ex) {
            throw new Exception("File not found");
        } finally {
            out.close();
        }
    }
    
    public void importCSV(String fileName) throws Exception {
        this.list.clear();
        File file = new File(fileName);
        FileReader fr = new FileReader(file);
        BufferedReader in = new BufferedReader(fr);
        String line;
        int lineCount = 0;
        ArrayList<Channel> newList = new ArrayList<Channel>();
        while((line = in.readLine()) != null) {
            try {
                Object[] elements = lineTokenizer(line);
                if ((elements.length < 3) || (elements.length > 8))
                    throw new Exception("Invalid parameters count");
                int id = Integer.parseInt( (String)elements[0]);
                if (id != lineCount)
                    throw new Exception("Invalid sequence number. Value read : "+Integer.toString(id)+" instead of "+Integer.toString(lineCount));
                String frequency =      ((String) elements[1]).trim();
                String shiftFreq =      ((String) elements[2]).trim();
                String scanLock =       ((String) elements[3]).trim();
                String shift =          ((String) elements[4]).trim();
                String shiftReverse =   ((String) elements[5]).trim();
                String shiftEnabled =   ((String) elements[6]).trim();

                if (!(("Locked".equals(scanLock)) || ("".equals(scanLock))))
                    throw new Exception("Invalid ScanLock value for channel Id : "+Integer.toString(id));
                if (!shift.matches("^[\\+-]?$"))
                    throw new Exception("Invalid ShiftEnabled value for channel Id : "+Integer.toString(id));
                if (!(("Reverse".equals(shiftReverse)) || ("".equals(shiftReverse))))
                    throw new Exception("Invalid ShiftReverse value for channel Id : "+Integer.toString(id));
                if (!(("Enabled".equals(shiftEnabled)) || ("".equals(shiftEnabled))))
                    throw new Exception("Invalid ShiftEnabled value for channel Id : "+Integer.toString(id));

                String comments = "";
                if (elements.length > 7)
                    comments =  ((String)elements[7]).trim();
                Channel chan = new Channel(frequency, shiftFreq, scanLock, shift, shiftReverse, shiftEnabled);
                chan.setId(id);
                chan.setComments(comments);
                newList.add(chan);
                lineCount++;
            } catch (NumberFormatException e) {
                if (lineCount > 0)
                    throw new Exception("Invalid file format. Channel Id "+Integer.toString(lineCount)+" : Invalid number");
            }            
        }
        
        in.close();
        fr.close();
        this.list = newList;
    }
    
    private static Object[] lineTokenizer(String line) {
        ArrayList<String> list = new ArrayList<String>();
        int firstPos = 0;
        int lastPos = 0;
        while ( firstPos < line.length()) {
            lastPos = line.indexOf(';', firstPos);
            if (lastPos == -1)
                lastPos = line.length();
            String token = line.substring(firstPos,lastPos);
            list.add(token);
            firstPos = lastPos+1;
        }
        return list.toArray();
    }
}
