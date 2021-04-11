/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package f4fez.prm80x0.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author f4fez
 */
public class ErrorSlot implements Runnable {
    private Socket socket;
    private OutputStream outStream;
    private InputStream inStream;
    private PrintWriter out;
    
    public void connect(Socket socket) {
        try {            
            if (this.socket == null) {
                this.socket = socket;
                socket.setSoTimeout(5000);
                this.outStream = this.socket.getOutputStream();
                this.out = new PrintWriter(outStream);
                this.inStream = this.socket.getInputStream();
                new Thread(this).start();
            }
        } catch (IOException ex) {
            Logger.getLogger(ErrorSlot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void run() {
            try {
                out.print("PRM80 server FU V1.0>\n\r");
                out.flush();
                int i = this.inStream.read();
                out.close();
            } catch (SocketTimeoutException ex) {
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(ErrorSlot.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
}
