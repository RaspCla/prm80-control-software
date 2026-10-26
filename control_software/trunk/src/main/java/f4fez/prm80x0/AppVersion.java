/*
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

import java.io.InputStream;
import java.util.Properties;

/**
 * Version of the program and the firmware it needs.
 * The program version is taken from &lt;version&gt; in pom.xml (Maven writes it
 * into version.properties when building).
 */
public final class AppVersion {

    /** Oldest firmware the program works with (serial protocol of V5) */
    public static final int MIN_FIRMWARE_MAJOR = 5;
    public static final int MIN_FIRMWARE_MINOR = 0;
    /** Firmware without the known errors of V5.0 (reverse, scan, D and M command) */
    public static final int RECOMMENDED_FIRMWARE_MAJOR = 5;
    public static final int RECOMMENDED_FIRMWARE_MINOR = 1;
    public static final String RECOMMENDED_FIRMWARE = "V5.1";

    public static final String HOMEPAGE = "https://github.com/RaspCla/prm80-control-software";
    public static final String FIRMWARE_HOMEPAGE = "https://github.com/RaspCla/prm80";

    private static String version;

    private AppVersion() {
    }

    /** @return e.g. "3.0.0", "?" if not known (not built with Maven) */
    public static synchronized String get() {
        if (version == null) {
            version = "?";
            try (InputStream in = AppVersion.class.getResourceAsStream("version.properties")) {
                if (in != null) {
                    Properties p = new Properties();
                    p.load(in);
                    String v = p.getProperty("version", "?");
                    if (!v.startsWith("$"))
                        version = v;
                }
            } catch (Exception ex) {
                // keep "?"
            }
        }
        return version;
    }

    /** @return true if the firmware has the V5 protocol */
    public static boolean isSupported(int major, int minor) {
        return major > MIN_FIRMWARE_MAJOR || (major == MIN_FIRMWARE_MAJOR && minor >= MIN_FIRMWARE_MINOR);
    }

    /** @return true if the firmware is the recommended one or newer */
    public static boolean isRecommended(int major, int minor) {
        return major > RECOMMENDED_FIRMWARE_MAJOR
                || (major == RECOMMENDED_FIRMWARE_MAJOR && minor >= RECOMMENDED_FIRMWARE_MINOR);
    }
}
