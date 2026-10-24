/*
 * ChannelModel.java
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
 * Created on 19 February 2008, 12:59
 */

package f4fez.prm80x0.gui.channelmanager;

import f4fez.prm80x0.Controler.Channel;
import f4fez.prm80x0.Controler.ChannelList;
import javax.swing.table.AbstractTableModel;

/**
 * Table model of the channel manager. The last row ("*") adds a new channel.
 *
 * @author fmazen
 */
public class ChannelModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    public static final int COL_CHANNEL = 0;
    public static final int COL_FREQUENCY = 1;
    public static final int COL_SHIFTFREQ = 2;
    public static final int COL_SCANLOCK = 3;
    public static final int COL_SHIFTPOSNEG = 4;
    public static final int COL_SHIFTREVERSE = 5;
    public static final int COL_SHIFTENABLED = 6;
    public static final int COL_COMMENTS = 7;

    private static final String[] COLUMN_NAMES = {
        "Channel", "Frequency", "Shift Freq.", "Scan Lock", "Shift +/-", "Shift Reverse", "Shift Enabled", "Comment"
    };

    private ChannelList channels;
    private Channel newChannel;

    public ChannelModel(ChannelList channels) {
        super();
        this.channels = channels == null ? new ChannelList() : channels;
        this.newChannel = new Channel();
    }

    /** Show another channel list. */
    public void setChannels(ChannelList channels) {
        this.channels = channels == null ? new ChannelList() : channels;
        this.newChannel = new Channel();
        this.fireTableDataChanged();
    }

    public ChannelList getChannels() {
        return this.channels;
    }

    @Override
    public int getRowCount() {
        // last row "*" adds a channel, the PRM80 has 100 channels at most
        return channels.countChannel() + (channels.countChannel() < ChannelList.MAX_CHANNELS ? 1 : 0);
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    private Channel channelAt(int row) {
        if (row == this.channels.countChannel())
            return this.newChannel;
        return this.channels.getChannel(row);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Channel editedChannel = this.channelAt(rowIndex);
        switch (columnIndex) {
            case COL_CHANNEL:
                if (editedChannel.getId() == -1)
                    return "*";
                else
                    return Integer.toString(editedChannel.getId());
            case COL_FREQUENCY:
                return editedChannel.getFrequency() == null ? "" : editedChannel.getFrequency();
            case COL_SHIFTFREQ:
                return editedChannel.getShiftFreq();
            case COL_SCANLOCK:
                return editedChannel.getScanLock();
            case COL_SHIFTPOSNEG:
                return editedChannel.getShift();
            case COL_SHIFTREVERSE:
                return editedChannel.getShiftReverse();
            case COL_SHIFTENABLED:
                return editedChannel.getShiftEnabled();
            case COL_COMMENTS:
                return editedChannel.getComments();
            default:
                return "Internal Error";
        }
    }

    @Override
    public String getColumnName(int col){
        return COLUMN_NAMES[col];
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col > 0;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        String text = value == null ? "" : value.toString().trim();
        Channel editedChannel = this.channelAt(row);
        switch (col) {
            case COL_FREQUENCY:
                editedChannel.setFrequency(text);
                break;
            case COL_SHIFTFREQ:
                editedChannel.setShiftFreq(text);
                break;
            case COL_SCANLOCK:
                editedChannel.setScanLock(text);
                break;
            case COL_SHIFTPOSNEG:
                editedChannel.setShift(text);
                break;
            case COL_SHIFTREVERSE:
                editedChannel.setShiftReverse(text);
                break;
            case COL_SHIFTENABLED:
                editedChannel.setShiftEnabled(text);
                break;
            case COL_COMMENTS:
                editedChannel.setComments(value == null ? "" : value.toString());
                break;
            default:
                return;
        }
        if ( (row == this.channels.countChannel()) && (this.newChannel.getFrequency() != null) )  {
            // new channel complete (it has a frequency): add it to the list
            if (this.newChannel.getShiftFreq().isEmpty())
                this.newChannel.setShiftFreq("0.0");
            this.channels.addChannel(newChannel);
            this.newChannel = new Channel();
            this.fireTableRowsInserted(row + 1, row + 1);
        }
        // event with the column: the channel manager needs to know which value changed
        this.fireTableCellUpdated(row, col);
    }
}
