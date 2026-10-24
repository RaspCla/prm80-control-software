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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import f4fez.prm80x0.gui.PRM80X0View;
import javax.swing.SwingUtilities;

/**
 * The main class of the application.
 */
public class PRM80X0App {

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        analyseCommandLine(args);
        CmdLineOptions options = CmdLineOptions.getInstance();
        if (options.isServerMode()) {
            String serial = options.getSerialPort();
            if (serial == null) {
                serial = new Option().getSerialPort();
            }
            f4fez.prm80x0.server.Server.runServerConsoleMode(options.getServerPort(), serial);
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (CmdLineOptions.getInstance().isDarkMode()) {
                        FlatDarkLaf.setup();
                    } else {
                        FlatLightLaf.setup();
                    }
                    new PRM80X0View().setVisible(true);
                }
            });
        }
    }

    private static void analyseCommandLine(String[] args) {
        CmdLineOptions options = CmdLineOptions.getInstance();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String name = arg;
            String value = null;
            int eq = arg.indexOf('=');
            if (arg.startsWith("-") && eq > 0) {        // --server=8060
                name = arg.substring(0, eq);
                value = arg.substring(eq + 1);
            }
            switch (name) {
                case "-h":
                case "--help":
                    printUsage();
                    System.exit(0);
                    break;
                case "-d":
                case "--debug":
                    options.setDebug(true);
                    break;
                case "--dark":
                    options.setDarkMode(true);
                    break;
                case "-s":
                case "--server":
                    if (value == null && i + 1 < args.length)   // -s 8060
                        value = args[++i];
                    try {
                        options.setServerPort(Integer.parseInt(value));
                        options.setServerMode(true);
                    } catch (NumberFormatException e) {
                        usageError("Invalid server port: " + value);
                    }
                    break;
                case "-p":
                case "--port":
                    if (value == null && i + 1 < args.length)   // -p COM3
                        value = args[++i];
                    if (value == null)
                        usageError("Missing serial port");
                    options.setSerialPort(value);
                    break;
                default:
                    usageError("Unknown option: " + arg);
            }
        }
    }

    private static void usageError(String message) {
        System.err.println(message);
        printUsage();
        System.exit(2);
    }

    private static void printUsage() {
        System.err.println(
            "Usage: prm80 [Options]\n\nOptions:\n" +
            "  -h,   --help           Print this help message\n" +
            "  -s,   --server=PORT    Server mode. Listen on TCP port PORT\n" +
            "  -p,   --port=SERIAL    Serial port for server mode (e.g. COM3 or /dev/ttyUSB0),\n" +
            "                         default: the port selected in the GUI options\n" +
            "  -d,   --debug          Set debug mode\n" +
            "        --dark           Use the dark look and feel\n");
    }
}
