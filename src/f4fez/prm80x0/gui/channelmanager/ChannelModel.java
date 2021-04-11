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
 * Created on 19 février 2008, 12:59
 */

package f4fez.prm80x0.gui.channelmanager;

import f4fez.prm80x0.Controler.Channel;
import f4fez.prm80x0.Controler.ChannelList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author fmazen
 */
public class ChannelModel extends AbstractTableModel {
    public static int COL_CHANNEL = 0;
    public static int COL_FREQUENCY = 1;
    public static int COL_SHIFT = 2;
    public static int COL_COMMENTS = 3;
    private ChannelList channels;
    private Channel newChannel;

    public ChannelModel(ChannelList channels) {
        super();
        this.channels = channels;
        this.newChannel = new Channel();

    }

    @Override
    public int getRowCount() {
        return channels.countChannel()+1;
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Channel editedChannel;
        if (rowIndex == this.channels.countChannel())
            editedChannel = this.newChannel;
        else
            editedChannel = this.channels.getChannel(rowIndex);

        switch (columnIndex) {
            case 0:
                if (editedChannel.getId() == -1)
                        return "*";
                else
                    return editedChannel.getId();
            case 1:
                return editedChannel.getFrequency();
            case 2:
                return editedChannel.getShift();
            case 3:
                return editedChannel.getComments();
            default:
                return "Internal Error";
        }
    }

    @Override
    public String getColumnName(int col){
        switch (col) {
            case 0:
                return "Channel n°";
            case 1:
                return "Frequency";
            case 2:
                return "Shift";
            case 3:
                return "Comments";
            default:
                return "Internal Error";
        }
    }

    @Override
    public Class getColumnClass(int c) {
        /*if (c==2)
            return Boolean.class;
        else*/
            return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col > 0;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        Channel editedChannel;
        if (row == this.channels.countChannel()) {
            editedChannel = this.newChannel;
        }
        else {
            editedChannel = this.channels.getChannel(row);
        }

        switch (col) {
            case 1:
                editedChannel.setFrequency((String) value);
                break;
            case 2:
                /*String shift = "";
                if ( (Boolean) value )
                    shift = "-";
                editedChannel.setShift(shift);*/
                editedChannel.setShift((String) value);
                break;                
            case 3:
                editedChannel.setComments((String) value);
                break;
        }

        if ( (row == this.channels.countChannel()) && (this.newChannel.getFrequency() != null) )  {
            this.channels.addChannel(newChannel);
            this.newChannel = new Channel();
        }
    }


}
