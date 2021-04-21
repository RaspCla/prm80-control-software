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

package f4fez.prm80x0.ui.channelmanager.resources;

import f4fez.prm80x0.Controler.ChannelList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author fmazen
 */
public class ChannelModel extends AbstractTableModel {

    private ChannelList channels;

    public ChannelModel(ChannelList channels) {
        this.channels = channels;
    }

    public int getRowCount() {
        return channels.countChannel();
    }

    public int getColumnCount() {
        return 8;
    }

        public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return this.channels.getChannel(rowIndex).getId();
            case 1:
                return this.channels.getChannel(rowIndex).getFrequency();
            case 3:
                return this.channels.getChannel(rowIndex).isShift();
            case 7:
                return this.channels.getChannel(rowIndex).getComments();
            default:
                return "Internal Error";
        }
    }
    
}

