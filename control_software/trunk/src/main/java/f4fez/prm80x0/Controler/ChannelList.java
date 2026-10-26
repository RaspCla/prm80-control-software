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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 *
 * @author fmazen
 */
public class ChannelList{
    /** The PRM80 has channels 0 to 99. */
    public static final int MAX_CHANNELS = 100;
    private static final String CSV_HEADER = "Chan Id;Frequency;ShiftFrequency;ScanLock;ShiftPosNeg;ShiftReverse;ShiftEnabled;Comments";
    private ArrayList<Channel> list;

    public ChannelList() {
        this.list = new ArrayList<Channel>();
    }

    public Channel getChannel(int channel) {
        return this.list.get(channel);
    }

    /** @return a copy of the list with copies of the channels (comments included) */
    public ChannelList copy() {
        ChannelList c = new ChannelList();
        for (Channel chan : this.list)
            c.list.add(chan.copy());
        return c;
    }

    /** Insert a channel at index, the following channels move back. */
    public void insertChannel(int index, Channel channel) {
        this.list.add(index, channel);
        this.renumber();
    }

    /** Remove the channel at index, the following channels move forward. */
    public void removeChannel(int index) {
        this.list.remove(index);
        this.renumber();
    }

    /** Move the channel at index "from" to index "to". */
    public void moveChannel(int from, int to) {
        Channel chan = this.list.remove(from);
        this.list.add(to, chan);
        this.renumber();
    }

    /** Channel numbers = position in the list. */
    private void renumber() {
        for (int i = 0; i < this.list.size(); i++)
            this.list.get(i).setId(i);
    }

    /** Remove the last channel. */
    public void removeLast() {
        if (!this.list.isEmpty())
            this.list.remove(this.list.size() - 1);
    }

    /**
     * @return true if both lists contain the same channels (comments are
     * not compared, the PRM80 has none)
     */
    public boolean sameChannelsAs(ChannelList other) {
        if (other == null || other.countChannel() != this.countChannel())
            return false;
        for (int i = 0; i < this.list.size(); i++) {
            if (!this.list.get(i).sameSettingsAs(other.getChannel(i)))
                return false;
        }
        return true;
    }

    /** Replace a channel. */
    public void setChannel(int index, Channel channel) {
        this.list.set(index, channel);
        channel.setId(index);
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
    
    /**
     * Write the channel list to a CSV file (UTF-8, separator ";").
     * @param fileName File to write
     * @throws Exception on error, with a message for the user
     */
    public void exportCSV(String fileName) throws Exception {
        try (PrintWriter out = new PrintWriter(fileName, StandardCharsets.UTF_8.name())) {
            out.println(CSV_HEADER);
            for (int i = 0; i < this.list.size(); i++) {
                Channel chan = this.list.get(i);
                out.println(    Integer.toString(i)
                                + ";"  + nullToEmpty(chan.getFrequency())
                                + ";"  + nullToEmpty(chan.getShiftFreq())
                                + ";"  + nullToEmpty(chan.getScanLock())
                                + ";"  + nullToEmpty(chan.getShift())
                                + ";"  + nullToEmpty(chan.getShiftReverse())
                                + ";"  + nullToEmpty(chan.getShiftEnabled())
                                // a ";" in the comment would start a new column
                                + ";"  + nullToEmpty(chan.getComments()).replace(';', ',').replace('\n', ' ')
                                );
            }
            if (out.checkError())
                throw new Exception("Error writing file " + fileName);
        } catch (FileNotFoundException ex) {
            throw new Exception("Cannot write file " + fileName);
        }
    }

    /**
     * Read the channel list from a CSV file written by exportCSV().
     * The list is only changed if the whole file could be read.
     * @param fileName File to read
     * @throws Exception on error, with a message for the user
     */
    public void importCSV(String fileName) throws Exception {
        ArrayList<Channel> newList = new ArrayList<Channel>();
        try (BufferedReader in = Files.newBufferedReader(Paths.get(fileName), csvCharset(fileName))) {
            String line;
            int lineNumber = 0;
            while((line = in.readLine()) != null) {
                lineNumber++;
                if (line.startsWith("\uFEFF"))           // byte order mark (Excel)
                    line = line.substring(1);
                if (line.trim().isEmpty() || line.startsWith("Chan"))   // header / empty line
                    continue;
                String[] elements = line.split(";", -1);
                if (elements.length < 7)
                    throw new Exception("Line " + lineNumber + ": " + elements.length + " columns instead of 7 or 8");
                int id;
                try {
                    id = Integer.parseInt(elements[0].trim());
                } catch (NumberFormatException e) {
                    throw new Exception("Line " + lineNumber + ": invalid channel number \"" + elements[0] + "\"");
                }
                if (id != newList.size())
                    throw new Exception("Line " + lineNumber + ": channel " + id + " instead of " + newList.size());
                String frequency =      elements[1].trim();
                String shiftFreq =      elements[2].trim();
                String scanLock =       elements[3].trim();
                String shift =          elements[4].trim();
                String shiftReverse =   elements[5].trim();
                String shiftEnabled =   elements[6].trim();
                if ("Locked".equals(scanLock))
                    scanLock = "Lock";
                if (!frequency.matches("^[0-9]{2,4}\\.[0-9]{1,6}$"))
                    throw new Exception("Channel " + id + ": invalid frequency \"" + frequency + "\"");
                if (shiftFreq.isEmpty())
                    shiftFreq = "0.000000";
                if (!shiftFreq.matches("^[0-9]{1,3}\\.[0-9]{1,6}$"))
                    throw new Exception("Channel " + id + ": invalid shift frequency \"" + shiftFreq + "\"");
                if (!(("Lock".equals(scanLock)) || ("".equals(scanLock))))
                    throw new Exception("Channel " + id + ": invalid ScanLock value \"" + scanLock + "\"");
                if (!shift.matches("^[\\+-]?$"))
                    throw new Exception("Channel " + id + ": invalid ShiftPosNeg value \"" + shift + "\"");
                if (!(("Reverse".equals(shiftReverse)) || ("".equals(shiftReverse))))
                    throw new Exception("Channel " + id + ": invalid ShiftReverse value \"" + shiftReverse + "\"");
                if (!(("Enabled".equals(shiftEnabled)) || ("".equals(shiftEnabled))))
                    throw new Exception("Channel " + id + ": invalid ShiftEnabled value \"" + shiftEnabled + "\"");
                String comments = "";
                if (elements.length > 7)
                    comments = elements[7].trim();
                Channel chan = new Channel(frequency, shiftFreq, scanLock, shift, shiftReverse, shiftEnabled);
                chan.setId(id);
                chan.setComments(comments);
                newList.add(chan);
            }
        } catch (NoSuchFileException ex) {
            throw new Exception("File not found: " + fileName);
        }
        this.list = newList;
    }

    /** Files of older versions were written in the system charset (Windows: Cp1252). */
    private static java.nio.charset.Charset csvCharset(String fileName) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(fileName));
        try {
            StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(data));
            return StandardCharsets.UTF_8;
        } catch (java.nio.charset.CharacterCodingException e) {
            return java.nio.charset.Charset.defaultCharset();
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Copy the comments of another list to the channels of this list that
     * have the same number and frequency. The PRM80 has no memory for
     * comments, so they are taken from the list shown before.
     * @param other list with comments
     */
    public void takeCommentsFrom(ChannelList other) {
        if (other == null)
            return;
        for (int i = 0; i < this.list.size() && i < other.countChannel(); i++) {
            Channel c = this.list.get(i);
            Channel o = other.getChannel(i);
            if (c.getFrequency() != null && c.getFrequency().equals(o.getFrequency()))
                c.setComments(o.getComments());
        }
    }
}
