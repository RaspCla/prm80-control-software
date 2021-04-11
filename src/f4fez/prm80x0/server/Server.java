/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package f4fez.prm80x0.server;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author f4fez
 */
public class Server implements Runnable {

    private int port;
    private ServerSocket socketServer;
    private boolean running;
    private Slot serverSlot;
    private ServerEventListener eventListener;
    
    public Server(ServerEventListener eventListener,int port, String serialPort) throws ServerException {
        try {
            this.port = port;
            this.eventListener = eventListener;
            socketServer = new ServerSocket(port);
            this.serverSlot = new Slot(serialPort, this.eventListener);
            new Thread(this).start();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    public static void runServerConsoleMode(int port) {
        ServerEventListener eventListener;
        eventListener = new ServerEventListener() {

            @Override
            public void connected(String clientAdress) {
                System.out.println("Connected to "+ clientAdress);
            }

            @Override
            public void disconnected() {
                System.out.println("Disconnected");
            }

            @Override
            public void connectionRefused(String clientAdress) {
                System.out.println("Connection refused to "+ clientAdress);
            }

            @Override
            public void charReceived(char data) {                
            }

            @Override
            public void charSent(char data) {
                System.out.print(data);
            }
        
        };
        
        System.out.println("Server started");        
        try {
            new Server(eventListener, port, "/dev/ttyS0");
        } catch (ServerException ex) {
            System.out.println("Error : "+ex.getMessage());
        }
    }

    
    @Override
    public void run() {
        try {
            this.running = true;
            while (this.running) {
                try {
                    Socket socket = this.socketServer.accept();
                    if (this.serverSlot.isFree()) {
                        this.eventListener.connected(socket.getInetAddress().getHostAddress());
                        this.serverSlot.connect(socket);
                    } else {
                        this.eventListener.connectionRefused(socket.getInetAddress().getHostAddress());
                        new ErrorSlot().connect(socket);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            this.serverSlot.closeSlot();
            this.socketServer.close();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.WARNING, null, ex);
        }
    }

}
