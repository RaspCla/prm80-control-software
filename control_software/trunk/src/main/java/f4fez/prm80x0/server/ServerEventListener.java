/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package f4fez.prm80x0.server;

/**
 *
 * @author f4fez
 */
public interface ServerEventListener {
    public void connected(String clientAdress);
    public void disconnected();
    public void connectionRefused(String clientAdress);
    public void charReceived(char data);
    public void charSent(char data);
}
