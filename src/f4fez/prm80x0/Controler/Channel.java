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

/**
 *
 * @author fmazen
 */
public class Channel {

    private int id;

    public Channel() {
        this.id = -1;
    }
    
    public Channel(String frequency, String shift) {
        this.id = -1;
        this.shift = shift;
        this.comments = "";
        
        int sepPos = frequency.indexOf('.');
        if (sepPos > 0) {
            String left = frequency.substring(0, sepPos);
            StringBuffer right = new StringBuffer(frequency.substring(sepPos+1));
            while (right.length() < 6)
                right.append('0');
            this.frequency = left+"."+right.toString();
        }
    }

    public Channel(int frequency, String shift) {
        this.id = -1;
        this.setFrequency(frequency);
        this.shift = shift;
        this.comments = "";
    }
    /**
     * Get the value of id
     *
     * @return the value of id
     */
    public int getId() {
        return id;
    }

    /**
     * Set the value of id
     *
     * @param id new value of id
     */
    public void setId(int id) {
        this.id = id;
    }

    private String frequency;

    /**
     * Get the value of frequency
     *
     * @return the value of frequency
     */
    public String getFrequency() {
        return frequency;
    }
    
    /**
     * Get the frequency in Hertz
     * @return The frequency
     */
    public int getIntFrequency() {
        int freq = Integer.parseInt(this.frequency.replace(".", ""));
        return freq;        
    }

    /**
     * Set the value of frequency
     *
     * @param frequency new value of frequency
     */
    public void setFrequency(String frequency) {
        int sepPos = frequency.indexOf('.');
        if (sepPos > 0) {
            String left = frequency.substring(0, sepPos);
            StringBuffer right = new StringBuffer(frequency.substring(sepPos+1));
            while (right.length() < 6)
                right.append('0');
            this.frequency = left+"."+right.toString();
        }
    }
    
    public void setFrequency(int frequency) {
        int left = frequency / 1000000;
        int right = frequency % 1000000;
        this.setFrequency(left+"."+right);
    }
    
    protected String shift ="";

    /**
     * Get the value of shift
     *
     * @return the value of shift
     */
    public String getShift() {
        return this.shift;
    }
    
    /**
     * Is shift activated or not
     * @return I s shift activated
     */
    public boolean isShift() {
        return !shift.equals("");
    }

    /**
     * Set the value of shift
     *
     * @param shift new value of shift
     */
    public void setShift(String shift) {
        this.shift = shift;
    }
    private String comments;

    /**
     * Get the value of comments
     *
     * @return the value of comments
     */
    public String getComments() {
        return comments;
    }

    /**
     * Set the value of comments
     *
     * @param comments new value of comments
     */
    public void setComments(String comments) {
        this.comments = comments;
    }

}
