/*
 * PRM80X0App.java
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

package f4fez.prm80x0;

import f4fez.prm80x0.gui.PRM80X0View;
import gnu.io.DriverManager;
import jargs.gnu.CmdLineParser;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class PRM80X0App extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
       show(new PRM80X0View(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of PRM80X0App
     */
    public static PRM80X0App getApplication() {
        return Application.getInstance(PRM80X0App.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        analyseCommandLine(args);
        if (CmdLineOptions.getInstance().isServerMode()) {
                f4fez.prm80x0.server.Server.runServerConsoleMode(CmdLineOptions.getInstance().getServerPort());
        }
        else
            DriverManager.getInstance().loadDrivers();
            launch(PRM80X0App.class, args);
    }

    private static void analyseCommandLine(String[] args) {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option help = parser.addBooleanOption('h', "help");
        CmdLineParser.Option debug = parser.addBooleanOption('d', "debug");
        CmdLineParser.Option server = parser.addIntegerOption('s', "server");

        try {
            parser.parse(args);
        }
        catch ( CmdLineParser.OptionException e ) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        Boolean helpValue = (Boolean)parser.getOptionValue(help);
        if (helpValue != null && helpValue) {
            printUsage();
            System.exit(0);
        }

        Boolean debugValue = (Boolean)parser.getOptionValue(debug);
        if (debugValue != null && debugValue) {
            CmdLineOptions.getInstance().setDebug(true);
        }
        else {
            CmdLineOptions.getInstance().setDebug(false);
        }

        Integer serverValue = (Integer) parser.getOptionValue(server);
        if (serverValue == null) {
            CmdLineOptions.getInstance().setServerMode(false);
        }
        else {
            CmdLineOptions.getInstance().setServerMode(true);
            CmdLineOptions.getInstance().setServerPort(serverValue);
        }
    }

    private static void printUsage() {
        System.err.println(
            "Usage: prm80 [Options]\n\nOptions:\n" +
            "  -h,   --help           Print this help message\n" +
            "  -s,   --server=PORT    Server mode. Lister on PORT number\n" +
            "  -d,   --debug          Set debug mode\n");
    }
}
