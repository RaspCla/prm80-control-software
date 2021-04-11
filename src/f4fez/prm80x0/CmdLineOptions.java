/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package f4fez.prm80x0;

/**
 *
 * @author FP13071
 */
public final class CmdLineOptions {
    private static CmdLineOptions instance;

    private boolean debug;

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isServerMode() {
        return serverMode;
    }

    public void setServerMode(boolean serverMode) {
        this.serverMode = serverMode;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
    private boolean serverMode;
    private int serverPort;

    private CmdLineOptions() {
    }

    static public CmdLineOptions getInstance() {
      if(null == instance) {
         instance = new CmdLineOptions();
      }
      return instance;
    }

    

}
