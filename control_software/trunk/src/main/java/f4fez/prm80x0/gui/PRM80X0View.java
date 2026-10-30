/*
 * PRM80X0View.java
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

package f4fez.prm80x0.gui;

import f4fez.prm80x0.*;
import f4fez.prm80x0.Controler.Channel;
import f4fez.prm80x0.Controler.ChannelList;
import f4fez.prm80x0.Controler.CommunicationException;
import f4fez.prm80x0.Controler.Controler;
import f4fez.prm80x0.Controler.DummyControler;
import f4fez.prm80x0.Controler.PRMControler;
import f4fez.prm80x0.Controler.PRMStateChangeListener;
import f4fez.prm80x0.Controler.SerialControler;
import f4fez.prm80x0.Controler.TcpControler;
import f4fez.prm80x0.gui.channelmanager.ChannelManager;
import f4fez.prm80x0.gui.serialterminal.TerminalDialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


/**
 * The application's main frame.
 */
public class PRM80X0View extends javax.swing.JFrame {
    private static final long serialVersionUID = 1L;

    private VirtualDeviceFirmware vdf;
    private Option config;
    private TerminalDialog serialSpy;

    /** All communication with the PRM80 runs in this background worker. */
    private final PrmWorker worker = new PrmWorker(this::busyChanged);
    /** Scan done by the program (memory channels or VFO range). */
    private final Scanner scanner = new Scanner(this.worker, new Scanner.Listener() {
        @Override
        public void scanUpdated() {
            refreshDisplay();
        }

        @Override
        public void scanStateChanged() {
            showScanState();
        }

        @Override
        public void scanStopped(String reason) {
            setStatus(reason);
        }
    });
    /** Reads the state of the PRM80 regularly (front panel changes). */
    private Timer pollTimer;
    /** true while the display is updated: the change listeners must not send anything */
    private boolean updatingDisplay;
    /** true while a connection is being established */
    private boolean connecting;
    /** status bar text while connected */
    private String connectedStatus = "";
    /** last answer state shown in the status bar */
    private boolean lastStateValid = true;

    // values shown in the display, changed at once when the user clicks
    // (the PRM80 answers later)
    private int shownChannel;
    private int shownRx;
    private int shownTx;
    private boolean shownHighPower;

    // volume and squelch: only the last value is sent when the slider is moved fast
    private final AtomicInteger wantedVolume = new AtomicInteger();
    private final AtomicBoolean volumeQueued = new AtomicBoolean();
    private final AtomicInteger wantedSquelch = new AtomicInteger();
    private final AtomicBoolean squelchQueued = new AtomicBoolean();

    public PRM80X0View() {
            // zoom factor of the last session
            this.config = new Option();
            Zoom.set(this.config.getZoom());

            initComponents();
            this.setIconImages(loadIcons());
            
            // Disable all controls
            this.setEnableControls(false);
            this.updateConnectControls();
            
            // Expert mode ?
            this.expertModeMenuItem.setSelected(this.config.isExpertMode());
            this.setVisibleExpertControls(this.config.isExpertMode());
            this.writeMemButton.setVisible(false);
            this.scanLabel.setVisible(false);

            // status bar initialization
            progressBar.setVisible(false);
            busyLabel.setVisible(false);
            this.setStatus("Not connected");

            // Load lcd font. The stream must be closed, otherwise Windows keeps
            // the file locked and "Clean" fails while the program is running.
            try (InputStream lcdFontStream = PRM80X0View.class.getResourceAsStream("/f4fez/prm80x0/gui/resources/lcd.ttf")) {
                Font lcdFont = Font.createFont(Font.TRUETYPE_FONT, lcdFontStream);
                float zoom = Zoom.get();
                this.freqLabel.setFont(lcdFont.deriveFont(32f * zoom));
                this.chanLabel.setFont(lcdFont.deriveFont(32f * zoom));
                this.txFreqLabel.setFont(lcdFont.deriveFont(24f * zoom));
                this.infoLabel.setFont(lcdFont.deriveFont(24f * zoom));
            } catch (Exception ex) {
                Logger.getLogger(PRM80X0View.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this, ex.getMessage());
            } 
            Font powerFont = this.hpLabel.getFont();
            powerFont = powerFont.deriveFont(Font.BOLD, powerFont.getSize2D() * 0.85f);
            this.hpLabel.setFont(powerFont);
            this.lpLabel.setFont(powerFont);
            this.busyLabel.setFont(powerFont);
            this.scanLabel.setFont(powerFont);
            Font unitFont = this.rxUnitLabel.getFont();
            unitFont = unitFont.deriveFont(Font.BOLD, unitFont.getSize2D() * 0.85f);
            this.rxUnitLabel.setFont(unitFont);
            this.txUnitLabel.setFont(unitFont);
            this.chnlUnitLabel.setFont(unitFont);
            // comment of the channel (channel manager list) below the channel number
            Font commentFont = this.commentLabel.getFont();
            this.commentLabel.setFont(commentFont.deriveFont(Font.PLAIN, commentFont.getSize2D() * 1.1f));
            // "Rx", "Tx", "Chnl" stand on the baseline of the LCD digits
            this.rxUnitLabel.putClientProperty(Zoom.BASELINE_OF, this.freqLabel);
            this.txUnitLabel.putClientProperty(Zoom.BASELINE_OF, this.txFreqLabel);
            this.chnlUnitLabel.putClientProperty(Zoom.BASELINE_OF, this.chanLabel);
            // "Chnl" is shown together with the channel number (not in VFO mode)
            this.chnlUnitLabel.setVisible(false);
            this.commentLabel.setVisible(false);
            this.chanLabel.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    chnlUnitLabel.setVisible(true);
                    commentLabel.setVisible(true);
                }

                @Override
                public void componentHidden(ComponentEvent e) {
                    chnlUnitLabel.setVisible(false);
                    commentLabel.setVisible(false);
                }
            });

            this.initPortList();
            this.initZoom();

            this.pollTimer = new Timer(1500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    poll();
                }
            });
            this.pollTimer.start();
    }

    /** Program icon in several sizes (window, task bar, Alt+Tab). */
    private static java.util.List<java.awt.Image> loadIcons() {
        java.util.List<java.awt.Image> icons = new java.util.ArrayList<>();
        for (int size : new int[] {16, 24, 32, 48, 64, 128, 256}) {
            java.net.URL url = PRM80X0View.class.getResource("/f4fez/prm80x0/gui/resources/icons/prm80-" + size + ".png");
            if (url != null)
                icons.add(new javax.swing.ImageIcon(url).getImage());
        }
        return icons;
    }

    // ---- Zoom -------------------------------------------------------------

    /** Content size after the last pack(), its resize event must not change the zoom */
    private Dimension packedSize;
    /** Collects the resize events while the window border is dragged */
    private Timer zoomTimer;

    private void initZoom() {
        Zoom.install(this);
        this.setLocationRelativeTo(null);
        this.packedSize = this.getContentPane().getSize();
        this.showZoom();

        // zoom when the window size is changed with the mouse
        this.zoomTimer = new Timer(60, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomToWindowSize();
            }
        });
        this.zoomTimer.setRepeats(false);
        this.getContentPane().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!getContentPane().getSize().equals(packedSize)) {
                    zoomTimer.restart();
                }
            }
        });

        // keyboard: Ctrl + / Ctrl - / Ctrl 0, also on the numeric keypad
        int ctrl = InputEvent.CTRL_DOWN_MASK;
        this.zoomInMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ctrl));
        this.zoomOutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ctrl));
        this.zoomResetMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, ctrl));
        this.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, ctrl), "zoomIn", this.zoomInMenuItem);
        this.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ctrl), "zoomIn", this.zoomInMenuItem);
        this.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, ctrl), "zoomOut", this.zoomOutMenuItem);
        this.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, ctrl), "zoomReset", this.zoomResetMenuItem);
    }

    private void bindKey(KeyStroke key, String name, final JMenuItem item) {
        JRootPane root = this.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                item.doClick(0);
            }
        });
    }

    /** Set a new zoom factor and adapt the window size to it. */
    private void setZoom(float zoom) {
        Zoom.set(zoom);
        this.config.setZoom(Zoom.get());
        this.pack();
        this.packedSize = this.getContentPane().getSize();
        this.showZoom();
    }

    /** Choose the zoom factor so that the content fits into the window. */
    private void zoomToWindowSize() {
        float zoom = Zoom.get();
        Dimension window = this.getContentPane().getSize();
        Dimension content = this.getContentPane().getPreferredSize();
        float baseWidth = content.width / zoom;
        float baseHeight = content.height / zoom;
        float newZoom = Math.min(window.width / baseWidth, window.height / baseHeight);
        newZoom = Zoom.clamp((float) Math.floor(newZoom * 100f) / 100f);
        if (Math.abs(newZoom - zoom) >= 0.01f) {
            Zoom.set(newZoom);
            this.config.setZoom(Zoom.get());
            this.showZoom();
        }
    }

    private void showZoom() {
        this.statusAnimationLabel.setText(Math.round(Zoom.get() * 100) + " %");
    }

    // ---- Dialogs ----------------------------------------------------------

    /**
     * Show a modal dialog in front of the main window. On Windows a dialog
     * opened from a menu sometimes appeared behind the main window.
     */
    private void showDialog(final java.awt.Window dialog) {
        dialog.setLocationRelativeTo(this);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                dialog.toFront();
                dialog.requestFocus();
            }
        });
        dialog.setVisible(true);
    }

    public void showAboutBox() {
        if (aboutBox == null) {
            aboutBox = new PRM80X0AboutBox(this);
        }
        this.showDialog(aboutBox);
    }

    /** Leave the application, closing the connection to the PRM80 first. */
    public void quit() {
        this.pollTimer.stop();
        if (this.vdf != null) {
            try {
                this.vdf.disconnect();
            } catch (Exception ex) {
                Logger.getLogger(PRM80X0View.class.getName()).log(Level.FINE, null, ex);
            }
        }
        this.dispose();
        System.exit(0);
    }

    private void setStatus(String text) {
        this.statusMessageLabel.setText(text);
    }

    // ---- Serial port selection --------------------------------------------

    private void initPortList() {
        this.fillPortList();
        // update the list each time it is opened (USB adapter plugged in)
        this.portComboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                fillPortList();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    private void fillPortList() {
        Object current = this.portComboBox.getSelectedItem();
        String selected = current != null && !current.toString().trim().isEmpty()
                ? current.toString().trim() : this.config.getSerialPort();
        this.portComboBox.removeAllItems();
        for (String port : SerialControler.listPortNames()) {
            this.portComboBox.addItem(port);
        }
        this.portComboBox.setSelectedItem(selected);
    }

    private String selectedPort() {
        Object port = this.portComboBox.getEditor().getItem();
        if (port == null || port.toString().trim().isEmpty())
            port = this.portComboBox.getSelectedItem();
        return port == null ? "" : port.toString().trim();
    }

    // ---- Connection -------------------------------------------------------

    /**
     * Connect in the background.
     * @param controler the controller to use (serial, TCP or virtual)
     * @param address serial port or server address
     * @param description shown in the status bar, e.g. "COM3"
     */
    private void connect(final Controler controler, final String address, final String description) {
        if (this.vdf != null || this.connecting)
            return;
        this.connecting = true;
        this.updateConnectControls();
        this.setStatus("Connecting to " + description + " ...");
        if (this.serialSpy != null && controler instanceof PRMControler)
            ((PRMControler) controler).addSerialListener(this.serialSpy);
        this.worker.run(() -> {
            controler.connectPRM(address);
            // the program uses the serial protocol of firmware V5
            int major = controler.getMajorFirmwareVersion();
            int minor = controler.getMinorFirmwareVersion();
            if (!f4fez.prm80x0.AppVersion.isSupported(major, minor))
                throw new IllegalStateException("The PRM80 has firmware V" + major + "." + minor
                        + ", which is too old.\nThis program needs firmware "
                        + f4fez.prm80x0.AppVersion.RECOMMENDED_FIRMWARE + " or newer:\n"
                        + f4fez.prm80x0.AppVersion.FIRMWARE_HOMEPAGE);
            // read everything needed later now, so that the display can be
            // updated without waiting for the PRM80
            controler.getPLLStep();
            controler.getMaxChan();
            controler.refreshState();
            return new VirtualDeviceFirmware(controler);
        }, vdf -> {
            this.connecting = false;
            this.vdf = vdf;
            this.shownShiftKey = "";
            ChannelManager.prmConnectionChanged();
            controler.setPRMStateChangeListener(new PRMStateChangeListener() {
                @Override
                public void stateUpdated() {
                    // called by the communication thread
                    SwingUtilities.invokeLater(() -> refreshDisplay());
                }
            });
            this.memToggleButton.setSelected(true);
            this.setEnableControls(true);
            this.updateConnectControls();
            this.updateModeControls();
            this.connectedStatus = "Connected to " + description + this.describePrm();
            this.lastStateValid = true;
            this.setStatus(this.connectedStatus);
            this.refreshDisplay();
            this.warnOldFirmware(controler);
        }, ex -> {
            this.connecting = false;
            controler.setPRMStateChangeListener(null);
            try {
                controler.disconnectPRM();
            } catch (Exception e) {
                Logger.getLogger(PRM80X0View.class.getName()).log(Level.FINE, null, e);
            }
            this.updateConnectControls();
            this.setStatus("Not connected");
            JOptionPane.showMessageDialog(this, "Cannot connect to " + description + ":\n" + message(ex),
                    "Connection error", JOptionPane.ERROR_MESSAGE);
        });
    }

    /**
     * Firmware V5.0 works with the program, but has errors that V5.1 corrects:
     * tell the user once per connection.
     */
    private void warnOldFirmware(Controler c) {
        int major, minor;
        try {
            major = c.getMajorFirmwareVersion();
            minor = c.getMinorFirmwareVersion();
        } catch (CommunicationException ex) {
            return;
        }
        if (f4fez.prm80x0.AppVersion.isRecommended(major, minor))
            return;
        JOptionPane.showMessageDialog(this,
                "The PRM80 has firmware V" + major + "." + minor + ". This program needs firmware "
                + f4fez.prm80x0.AppVersion.RECOMMENDED_FIRMWARE + " or newer.\n\n"
                + "With V" + major + "." + minor + " these functions do not work correctly:\n"
                + "  - shift reverse (wrong frequency and shift)\n"
                + "  - scan of the PRM80 (wrong channel, endless loop if all channels are locked)\n"
                + "  - power switch in VFO mode (sets the channel frequency again)\n"
                + "  - configuration: a new default shift or scan speed only works after a restart\n\n"
                + "Firmware " + f4fez.prm80x0.AppVersion.RECOMMENDED_FIRMWARE + ": "
                + f4fez.prm80x0.AppVersion.FIRMWARE_HOMEPAGE,
                "Old firmware", JOptionPane.WARNING_MESSAGE);
    }

    private String describePrm() {
        try {
            Controler c = this.vdf.getPRMControler();
            return " - " + (c.getPRMType() == Controler.PRM8070 ? "PRM8070" : "PRM8060")
                    + " V" + c.getMajorFirmwareVersion() + "." + c.getMinorFirmwareVersion()
                    + " (" + c.getPRMFrequencyCode() + " MHz)";
        } catch (Exception ex) {
            return "";
        }
    }

    private void disconnect() {
        if (this.vdf == null)
            return;
        this.scanner.stop();
        final VirtualDeviceFirmware old = this.vdf;
        this.vdf = null;
        ChannelManager.prmConnectionChanged();
        old.getPRMControler().setPRMStateChangeListener(null);
        this.setEnableControls(false);
        this.updateConnectControls();
        this.setStatus("Not connected");
        this.worker.run(() -> {
            old.disconnect();
            return null;
        }, null);
    }

    /** Enable / disable the connect and disconnect buttons and menu items. */
    private void updateConnectControls() {
        boolean connected = this.vdf != null;
        boolean free = !connected && !this.connecting;
        this.connectMenuItem.setEnabled(free);
        this.connectTcpMenuItem.setEnabled(free);
        this.connectVirtualMenuItem.setEnabled(free);
        this.disconnectMenuItem.setEnabled(connected);
        this.portComboBox.setEnabled(free);
        this.connectButton.setEnabled(!this.connecting);
        this.connectButton.setText(connected ? "Disconnect" : "Connect");
    }

    // ---- Communication with the PRM80 -------------------------------------

    /**
     * Run a command in the background, the display is updated afterwards.
     * A running scan is stopped (the user operates the radio).
     */
    private void command(PrmWorker.Task<?> task) {
        this.scanner.stop();
        this.commandWhileScanning(task);
    }

    /** Run a command that does not stop the scan (volume, squelch, power). */
    private void commandWhileScanning(PrmWorker.Task<?> task) {
        if (this.vdf == null)
            return;
        this.worker.run(task, ex -> {
            this.setStatus("Error: " + message(ex));
            JOptionPane.showMessageDialog(this, "Communication error:\n" + message(ex),
                    "PRM80", JOptionPane.ERROR_MESSAGE);
        });
    }

    /** Read the state of the PRM80 when nothing else is to do. */
    private void poll() {
        final VirtualDeviceFirmware v = this.vdf;
        if (v == null || !this.worker.isIdle() || this.scanner.isRunning())
            return;     // the scanner reads the state itself
        this.worker.poll(() -> {
            v.getPRMControler().refreshState();
            return null;
        }, null);
    }

    /** Called by the worker (GUI thread) when commands start or end. */
    private void busyChanged() {
        boolean busy = this.worker.isBusy();
        this.busyLabel.setVisible(busy && this.vdf != null);
        if (!busy)
            this.refreshDisplay();
    }

    private static String message(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isEmpty())
            msg = "No valid answer from the PRM80 (" + ex.getClass().getSimpleName() + ")";
        return msg;
    }

    private static String formatFrequency(int hz) {
        String freq = Integer.toString(hz);
        if (freq.length() != 9)
            return freq;
        return freq.substring(0, 3)+"."+freq.substring(3, 6)+"."+freq.substring(6, 9);
    }

    private static String formatChannel(int channel) {
        return channel < 10 ? "0" + channel : Integer.toString(channel);
    }

    /**
     * Show the values of the PRM80 (as read by the last command or poll).
     * Not while commands are waiting: the display shows what the user
     * clicked, the PRM80 values are shown when all commands are done.
     */
    private void refreshDisplay() {
        if (this.vdf == null || this.worker.isBusy())
            return;
        try {
            Controler c = this.vdf.getPRMControler();
            this.shownChannel = c.getCurrentChannel();
            this.shownRx = c.getRxPLLFrequency();
            this.shownTx = c.getTxPLLFrequency();
            this.shownHighPower = this.vdf.isHighPower();

            if (this.vfoToggleButton.isSelected() && c.hasLeftVfo())
                this.radioLeftVfo();
            this.chanLabel.setText(formatChannel(this.shownChannel));
            this.showChannelComment(c);
            this.freqLabel.setText(formatFrequency(this.shownRx));
            this.txFreqLabel.setText(formatFrequency(this.shownTx));
            this.hpLabel.setVisible(this.shownHighPower);
            this.lpLabel.setVisible(!this.shownHighPower);
            this.infoLabel.setText(this.vdf.isPLLLocked() ? "" : "-- Synthesizer ERROR --");
            this.showChannelShift(c);
            // status line only when the answer state changes (keeps other messages)
            boolean valid = c.isStateValid();
            if (valid != this.lastStateValid) {
                this.lastStateValid = valid;
                this.setStatus(valid ? this.connectedStatus
                        : "No answer from the PRM80 - the display may be outdated");
            }

            this.updatingDisplay = true;
            if (!this.volumeSlider.getValueIsAdjusting() && !this.volumeQueued.get())
                this.volumeSlider.setValue(this.vdf.getVolume());
            if (!this.squelchQueued.get())
                this.squelchSpinner.setValue(this.vdf.getSquelch());
        } catch (CommunicationException | RuntimeException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
        } finally {
            this.updatingDisplay = false;
        }
    }
    
    /**
     * Show the comment of the current channel from the list of the channel
     * manager (the PRM80 has no memory for comments). Only if the channel
     * in the list has the frequency of the channel in the PRM80, so that no
     * comment of another channel list is shown.
     */
    private void showChannelComment(Controler c) {
        String comment = "";
        try {
            int chan = c.getCurrentChannel();
            ChannelList list = ChannelManager.getSessionList();
            if (chan >= 0 && chan < list.countChannel()) {
                Channel ch = list.getChannel(chan);
                int state = c.getChannelState();
                // channel frequency: RX, with reverse TX (TX = channel frequency)
                int base = state >= 0 && (state & 3) == 3 ? c.getTxPLLFrequency() : c.getRxPLLFrequency();
                if (ch.getComments() != null && ch.getIntFrequency() == base)
                    comment = ch.getComments().trim();
            }
        } catch (CommunicationException | RuntimeException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.FINE, null, ex);
        }
        if (!comment.equals(this.commentLabel.getText())) {
            this.commentLabel.setText(comment);
            this.commentLabel.setToolTipText(comment.isEmpty() ? null : comment);
        }
    }

    /**
     * A channel key was pressed at the radio (or its scan started) while the
     * program was in VFO mode: the radio is on a channel again, so the
     * program changes to memory mode too.
     */
    private void radioLeftVfo() {
        this.scanner.stop();
        this.vdf.leaveVfoMode();
        this.memToggleButton.setSelected(true);
        this.chanLabel.setVisible(true);
        this.shownShiftKey = "";
        this.updateModeControls();
        this.setStatus("Channel changed at the radio - memory mode");
    }

    /** channel, state and frequencies the shift fields were last set for */
    private String shownShiftKey = "";

    /**
     * Show the shift of the current channel in "Shift enabled",
     * "Shift positive" and "Shift Frequency" (memory mode only: in VFO mode
     * the fields are the settings for entering a frequency).
     * Only done when channel, state or frequencies change, so the fields can
     * still be changed by hand.
     */
    private void showChannelShift(Controler c) throws CommunicationException {
        int state = c.getChannelState();
        if (state < 0 || this.vdf.isVfoMode())
            return;
        int rx = c.getRxPLLFrequency();
        int tx = c.getTxPLLFrequency();
        String key = c.getCurrentChannel() + "/" + state + "/" + rx + "/" + tx;
        if (key.equals(this.shownShiftKey))
            return;
        this.shownShiftKey = key;
        boolean enabled = (state & 1) != 0;
        this.ShiftEnabledCheckbox.setSelected(enabled);
        this.ShiftPositiveCheckbox.setSelected((state & 4) != 0);
        this.ShiftReverseCheckbox.setSelected((state & 2) != 0);
        this.appliedReverse = (state & 2) != 0;
        this.updateShiftControls(false);
        int shift = Math.abs(tx - rx);
        if (enabled && shift > 0)
            this.setShiftSpinner(shift);
    }

    /** Select a shift in Hz in the spinner, add it to the list if needed. */
    private void setShiftSpinner(int hz) {
        boolean wasUpdating = this.updatingDisplay;
        this.updatingDisplay = true;
        try {
            this.setShiftSpinnerValue(hz);
        } finally {
            this.updatingDisplay = wasUpdating;
        }
    }

    private void setShiftSpinnerValue(int hz) {
        String value = Integer.toString(hz);
        javax.swing.SpinnerListModel model = (javax.swing.SpinnerListModel) this.ShiftFreqSpinner.getModel();
        java.util.List<Object> values = new java.util.ArrayList<Object>(model.getList());
        if (!values.contains(value)) {
            values.add(value);
            values.sort((a, b) -> Integer.compare(Integer.parseInt(b.toString()), Integer.parseInt(a.toString())));
            model.setList(values);
        }
        this.ShiftFreqSpinner.setValue(value);
    }

    private void setEnableControls(boolean enable) {
        this.upButton.setEnabled(enable);
        this.downButton.setEnabled(enable);
        this.volumeLabel.setEnabled(enable);
        this.volumeSlider.setEnabled(enable);
        this.squelchLabel.setEnabled(enable);
        this.squelchSpinner.setEnabled(enable);
        this.vfoToggleButton.setEnabled(enable);
        this.memToggleButton.setEnabled(enable);
        this.resetMenuItem.setEnabled(enable);
        this.razMenuItem.setEnabled(enable);
        this.memoryMenu.setEnabled(enable);
        this.freqLabel.setVisible(enable);
        this.txFreqLabel.setVisible(enable);
        this.rxUnitLabel.setVisible(enable);
        this.txUnitLabel.setVisible(enable);
        this.chanLabel.setVisible(enable && this.vdf != null && !this.vdf.isVfoMode());
        this.infoLabel.setVisible(enable);
        if (!enable) {
            this.hpLabel.setVisible(false);
            this.lpLabel.setVisible(false);
            this.busyLabel.setVisible(false);
            this.writeMemButton.setVisible(false);
        }
        this.powerButton.setEnabled(enable);
        this.scanToggleButton.setEnabled(enable);
        if (!enable) {
            this.scanner.stop();
            this.scanToggleButton.setSelected(false);
            this.scanLabel.setVisible(false);
        }
        this.configurationMenuItem.setEnabled(enable);
        this.updateModeControls();
    }
    
    private void setVisibleExpertControls(boolean enable) {
        this.connectVirtualMenuItem.setVisible(enable);
        this.resetMenuItem.setVisible(enable);
        this.razMenuItem.setVisible(enable);
        this.memoryMenu.setVisible(enable);
        this.ramExtMenuItem.setVisible(false);
        this.ramIntMenuItem.setVisible(false);
        this.eepromMenuItem.setVisible(false);
        this.serialSpyMenuItem.setVisible(enable);
        this.expertSeparator.setVisible(enable);
        this.connectTcpMenuItem.setVisible(enable);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        connectPanel = new javax.swing.JPanel();
        portLabel = new javax.swing.JLabel();
        portComboBox = new javax.swing.JComboBox<>();
        connectButton = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        freqLabel = new javax.swing.JLabel();
        chanLabel = new javax.swing.JLabel();
        hpLabel = new javax.swing.JLabel();
        lpLabel = new javax.swing.JLabel();
        txFreqLabel = new javax.swing.JLabel();
        infoLabel = new javax.swing.JLabel();
        rxUnitLabel = new javax.swing.JLabel();
        txUnitLabel = new javax.swing.JLabel();
        chnlUnitLabel = new javax.swing.JLabel();
        commentLabel = new javax.swing.JLabel();
        busyLabel = new javax.swing.JLabel();
        scanLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        vfoToggleButton = new javax.swing.JToggleButton();
        memToggleButton = new javax.swing.JToggleButton();
        writeMemButton = new javax.swing.JButton();
        volumeSlider = new javax.swing.JSlider();
        squelchLabel = new javax.swing.JLabel();
        squelchSpinner = new javax.swing.JSpinner();
        upButton = new javax.swing.JButton();
        downButton = new javax.swing.JButton();
        volumeLabel = new javax.swing.JLabel();
        powerButton = new javax.swing.JButton();
        scanToggleButton = new javax.swing.JToggleButton();
        ShiftFreqSpinner = new javax.swing.JSpinner();
        label1 = new javax.swing.JLabel();
        ShiftEnabledCheckbox = new javax.swing.JCheckBox();
        ShiftPositiveCheckbox = new javax.swing.JCheckBox();
        ShiftReverseCheckbox = new javax.swing.JCheckBox();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        connectMenuItem = new javax.swing.JMenuItem();
        connectTcpMenuItem = new javax.swing.JMenuItem();
        connectVirtualMenuItem = new javax.swing.JMenuItem();
        disconnectMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        ToolsMenu = new javax.swing.JMenu();
        channelManagerMenuItem = new javax.swing.JMenuItem();
        configurationMenuItem = new javax.swing.JMenuItem();
        scanSettingsMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        resetMenuItem = new javax.swing.JMenuItem();
        razMenuItem = new javax.swing.JMenuItem();
        memoryMenu = new javax.swing.JMenu();
        ramIntMenuItem = new javax.swing.JMenuItem();
        ramExtMenuItem = new javax.swing.JMenuItem();
        eepromMenuItem = new javax.swing.JMenuItem();
        ram2eepromMenuItem = new javax.swing.JMenuItem();
        eeprom2ramMenuItem = new javax.swing.JMenuItem();
        serialSpyMenuItem = new javax.swing.JMenuItem();
        expertSeparator = new javax.swing.JSeparator();
        expertModeMenuItem = new javax.swing.JCheckBoxMenuItem();
        viewMenu = new javax.swing.JMenu();
        zoomInMenuItem = new javax.swing.JMenuItem();
        zoomOutMenuItem = new javax.swing.JMenuItem();
        zoomResetMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        modeButtonGroup = new javax.swing.ButtonGroup();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PRM80 control software"); // NOI18N
        setName("mainFrame"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        connectPanel.setName("connectPanel"); // NOI18N
        connectPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        portLabel.setText("Serial port:"); // NOI18N
        portLabel.setName("portLabel"); // NOI18N
        connectPanel.add(portLabel);

        portComboBox.setEditable(true);
        portComboBox.setToolTipText("Serial port of the PRM80, the list is updated when it is opened"); // NOI18N
        portComboBox.setName("portComboBox"); // NOI18N
        portComboBox.setPreferredSize(new java.awt.Dimension(160, 26));
        connectPanel.add(portComboBox);

        connectButton.setText("Connect"); // NOI18N
        connectButton.setName("connectButton"); // NOI18N
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });
        connectPanel.add(connectButton);

        getContentPane().add(connectPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new javax.swing.BoxLayout(mainPanel, javax.swing.BoxLayout.Y_AXIS));
        jPanel1.setBackground(new java.awt.Color(0, 0, 0)); // NOI18N
        jPanel1.setForeground(new java.awt.Color(228, 227, 227)); // NOI18N
        jPanel1.setMaximumSize(new java.awt.Dimension(500, 100));
        jPanel1.setMinimumSize(new java.awt.Dimension(500, 100));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(500, 100));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        freqLabel.setForeground(new java.awt.Color(51, 255, 204)); // NOI18N
        freqLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        freqLabel.setText("145.100.0"); // NOI18N
        freqLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        freqLabel.setName("freqLabel"); // NOI18N
        freqLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                freqLabelMouseClicked(evt);
            }
        });
        jPanel1.add(freqLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 8, 170, 30));

        chanLabel.setForeground(new java.awt.Color(255, 51, 51)); // NOI18N
        chanLabel.setText("00"); // NOI18N
        chanLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        chanLabel.setName("chanLabel"); // NOI18N
        chanLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chanLabelMouseClicked(evt);
            }
        });
        jPanel1.add(chanLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(215, 10, 50, 30));

        hpLabel.setBackground(new java.awt.Color(255, 0, 0));
        hpLabel.setForeground(new java.awt.Color(0, 0, 0));
        hpLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        hpLabel.setText("HIGH POWER"); // NOI18N
        hpLabel.setName("hpLabel"); // NOI18N
        hpLabel.setOpaque(true);
        jPanel1.add(hpLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 10, 90, 16));

        lpLabel.setBackground(new java.awt.Color(255, 255, 0));
        lpLabel.setForeground(new java.awt.Color(0, 0, 0));
        lpLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lpLabel.setText("LOW POWER"); // NOI18N
        lpLabel.setName("lpLabel"); // NOI18N
        lpLabel.setOpaque(true);
        jPanel1.add(lpLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 30, 90, 16));

        txFreqLabel.setForeground(new java.awt.Color(255, 204, 0)); // NOI18N
        txFreqLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txFreqLabel.setText("145.700.0"); // NOI18N
        txFreqLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        txFreqLabel.setName("txFreqLabel"); // NOI18N
        txFreqLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                freqLabelMouseClicked(evt);
            }
        });
        jPanel1.add(txFreqLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 43, 170, 20));

        infoLabel.setForeground(new java.awt.Color(0, 255, 0)); // NOI18N
        infoLabel.setText(""); // NOI18N
        infoLabel.setName("infoLabel"); // NOI18N
        jPanel1.add(infoLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 70, 390, 20));

        rxUnitLabel.setForeground(new java.awt.Color(51, 255, 204));
        rxUnitLabel.setText("Rx"); // NOI18N
        rxUnitLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        rxUnitLabel.setName("rxUnitLabel"); // NOI18N
        jPanel1.add(rxUnitLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(174, 22, -1, -1));

        txUnitLabel.setForeground(new java.awt.Color(255, 204, 0));
        txUnitLabel.setText("Tx"); // NOI18N
        txUnitLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        txUnitLabel.setName("txUnitLabel"); // NOI18N
        jPanel1.add(txUnitLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(174, 45, -1, -1));

        chnlUnitLabel.setForeground(new java.awt.Color(255, 51, 51));
        chnlUnitLabel.setText("Chnl"); // NOI18N
        chnlUnitLabel.setName("chnlUnitLabel"); // NOI18N
        jPanel1.add(chnlUnitLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(245, 22, -1, -1));

        commentLabel.setForeground(new java.awt.Color(255, 51, 51));
        commentLabel.setText(""); // NOI18N
        commentLabel.setName("commentLabel"); // NOI18N
        jPanel1.add(commentLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(215, 45, 180, 20));

        busyLabel.setBackground(new java.awt.Color(255, 0, 0));
        busyLabel.setForeground(new java.awt.Color(0, 0, 0));
        busyLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        busyLabel.setText("BUSY"); // NOI18N
        busyLabel.setName("busyLabel"); // NOI18N
        busyLabel.setOpaque(true);
        jPanel1.add(busyLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 50, 90, 16));

        scanLabel.setBackground(new java.awt.Color(0, 204, 0));
        scanLabel.setForeground(new java.awt.Color(0, 0, 0));
        scanLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        scanLabel.setText("SCAN"); // NOI18N
        scanLabel.setName("scanLabel"); // NOI18N
        scanLabel.setOpaque(true);
        jPanel1.add(scanLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 70, 90, 16));

        mainPanel.add(jPanel1);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setPreferredSize(new java.awt.Dimension(500, 165));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        modeButtonGroup.add(vfoToggleButton);
        vfoToggleButton.setText("VFO"); // NOI18N
        vfoToggleButton.setMaximumSize(new java.awt.Dimension(50, 23));
        vfoToggleButton.setMinimumSize(new java.awt.Dimension(50, 23));
        vfoToggleButton.setName("vfoToggleButton"); // NOI18N
        vfoToggleButton.setPreferredSize(new java.awt.Dimension(50, 23));
        vfoToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vfoToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(vfoToggleButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(215, 20, 70, -1));

        modeButtonGroup.add(memToggleButton);
        memToggleButton.setSelected(true);
        memToggleButton.setText("MEM"); // NOI18N
        memToggleButton.setMaximumSize(new java.awt.Dimension(50, 23));
        memToggleButton.setMinimumSize(new java.awt.Dimension(50, 23));
        memToggleButton.setName("memToggleButton"); // NOI18N
        memToggleButton.setPreferredSize(new java.awt.Dimension(50, 23));
        memToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                memToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(memToggleButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(215, 50, 70, -1));

        writeMemButton.setText("Write to MEM"); // NOI18N
        writeMemButton.setToolTipText("Store the VFO frequency and shift in a memory channel"); // NOI18N
        writeMemButton.setName("writeMemButton"); // NOI18N
        writeMemButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                writeMemButtonActionPerformed(evt);
            }
        });
        jPanel2.add(writeMemButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(185, 85, 120, -1));

        volumeSlider.setMajorTickSpacing(5);
        volumeSlider.setMaximum(15);
        volumeSlider.setMinorTickSpacing(1);
        volumeSlider.setOrientation(javax.swing.JSlider.VERTICAL);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setSnapToTicks(true);
        volumeSlider.setName("volumeSlider"); // NOI18N
        volumeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                volumeSliderStateChanged(evt);
            }
        });
        jPanel2.add(volumeSlider, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, 40, 110));

        squelchLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        squelchLabel.setText("Squelch"); // NOI18N
        squelchLabel.setMaximumSize(new java.awt.Dimension(61, 17));
        squelchLabel.setMinimumSize(new java.awt.Dimension(61, 17));
        squelchLabel.setName("squelchLabel"); // NOI18N
        squelchLabel.setPreferredSize(new java.awt.Dimension(61, 17));
        jPanel2.add(squelchLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(85, 10, 60, -1));

        squelchSpinner.setName("squelchSpinner"); // NOI18N
        squelchSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                squelchSpinnerStateChanged(evt);
            }
        });
        jPanel2.add(squelchSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(85, 30, 60, -1));

        upButton.setText("+"); // NOI18N
        upButton.setName("upButton"); // NOI18N
        upButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upButtonActionPerformed(evt);
            }
        });
        jPanel2.add(upButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 20, 60, 60));

        downButton.setText("-"); // NOI18N
        downButton.setName("downButton"); // NOI18N
        downButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downButtonActionPerformed(evt);
            }
        });
        jPanel2.add(downButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 90, 60, 60));

        volumeLabel.setText("Volume"); // NOI18N
        volumeLabel.setName("volumeLabel"); // NOI18N
        jPanel2.add(volumeLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));

        powerButton.setText("Power"); // NOI18N
        powerButton.setName("powerButton"); // NOI18N
        powerButton.setPreferredSize(new java.awt.Dimension(80, 23));
        powerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                powerButtonActionPerformed(evt);
            }
        });
        jPanel2.add(powerButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 60, 100, -1));

        scanToggleButton.setText("Scan"); // NOI18N
        scanToggleButton.setToolTipText("Scan the memory channels (MEM) or a frequency range (VFO, see Tools > Scan settings)"); // NOI18N
        scanToggleButton.setName("scanToggleButton"); // NOI18N
        scanToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scanToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(scanToggleButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 100, 100, -1));

        ShiftFreqSpinner.setModel(new javax.swing.SpinnerListModel(new String[] {"9400000", "7600000", "1600000", "600000"}));
        ShiftFreqSpinner.setName("ShiftFreqSpinner"); // NOI18N
        ShiftFreqSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ShiftFreqSpinnerStateChanged(evt);
            }
        });
        jPanel2.add(ShiftFreqSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 130, 100, 22));

        label1.setName("ShiftFrequency"); // NOI18N
        label1.setText("Shift Frequency"); // NOI18N
        jPanel2.add(label1, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 110, -1, -1));

        ShiftEnabledCheckbox.setText("Shift enabled"); // NOI18N
        ShiftEnabledCheckbox.setName("ShiftEna"); // NOI18N
        ShiftEnabledCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShiftEnabledCheckboxActionPerformed(evt);
            }
        });
        jPanel2.add(ShiftEnabledCheckbox, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 20, -1, 20));

        ShiftPositiveCheckbox.setText("Shift positive"); // NOI18N
        ShiftPositiveCheckbox.setName("ShiftPositiveCheckbox"); // NOI18N
        ShiftPositiveCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShiftPositiveCheckboxActionPerformed(evt);
            }
        });
        jPanel2.add(ShiftPositiveCheckbox, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 50, -1, 20));

        ShiftReverseCheckbox.setText("Shift reverse"); // NOI18N
        ShiftReverseCheckbox.setToolTipText("Reverse: receive on the TX frequency of the repeater and transmit on its RX frequency"); // NOI18N
        ShiftReverseCheckbox.setName("ShiftReverseCheckbox"); // NOI18N
        ShiftReverseCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShiftReverseCheckboxActionPerformed(evt);
            }
        });
        jPanel2.add(ShiftReverseCheckbox, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 80, -1, 20));

        mainPanel.add(jPanel2);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText("File"); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        connectMenuItem.setText("Connect serial port"); // NOI18N
        connectMenuItem.setName("connectMenuItem"); // NOI18N
        connectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectMenuActionPerformed(evt);
            }
        });
        fileMenu.add(connectMenuItem);

        connectTcpMenuItem.setText("Connect to server ..."); // NOI18N
        connectTcpMenuItem.setName("connectTcpMenuItem"); // NOI18N
        connectTcpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectTcpMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(connectTcpMenuItem);

        connectVirtualMenuItem.setText("Connect virtual PRM (test)"); // NOI18N
        connectVirtualMenuItem.setName("connectVirtualMenuItem"); // NOI18N
        connectVirtualMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectVirtualMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(connectVirtualMenuItem);

        disconnectMenuItem.setText("Disconnect"); // NOI18N
        disconnectMenuItem.setEnabled(false);
        disconnectMenuItem.setName("disconnectMenuItem"); // NOI18N
        disconnectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disconnectMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(disconnectMenuItem);

        exitMenuItem.setText("Quit"); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        ToolsMenu.setText("Tools"); // NOI18N
        ToolsMenu.setName("ToolsMenu"); // NOI18N

        channelManagerMenuItem.setText("Channel manager ..."); // NOI18N
        channelManagerMenuItem.setName("channelManagerMenuItem"); // NOI18N
        channelManagerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                channelManagerMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(channelManagerMenuItem);

        configurationMenuItem.setText("Configuration ..."); // NOI18N
        configurationMenuItem.setEnabled(false);
        configurationMenuItem.setName("configurationMenuItem"); // NOI18N
        configurationMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configurationMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(configurationMenuItem);

        scanSettingsMenuItem.setText("Scan settings ..."); // NOI18N
        scanSettingsMenuItem.setName("scanSettingsMenuItem"); // NOI18N
        scanSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scanSettingsMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(scanSettingsMenuItem);

        jSeparator1.setName("jSeparator1"); // NOI18N
        ToolsMenu.add(jSeparator1);

        resetMenuItem.setText("Restart PRM80"); // NOI18N
        resetMenuItem.setEnabled(false);
        resetMenuItem.setName("resetMenuItem"); // NOI18N
        resetMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(resetMenuItem);

        razMenuItem.setText("Reload RAM"); // NOI18N
        razMenuItem.setEnabled(false);
        razMenuItem.setName("razMenuItem"); // NOI18N
        razMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                razMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(razMenuItem);

        memoryMenu.setText("Memory"); // NOI18N
        memoryMenu.setEnabled(false);
        memoryMenu.setName("memoryMenu"); // NOI18N

        ramIntMenuItem.setText("RAM intern 80c552"); // NOI18N
        ramIntMenuItem.setName("ramIntMenuItem"); // NOI18N
        memoryMenu.add(ramIntMenuItem);

        ramExtMenuItem.setText("RAM extern"); // NOI18N
        ramExtMenuItem.setName("ramExtMenuItem"); // NOI18N
        memoryMenu.add(ramExtMenuItem);

        eepromMenuItem.setText("EEPROM"); // NOI18N
        eepromMenuItem.setName("eepromMenuItem"); // NOI18N
        memoryMenu.add(eepromMenuItem);

        ram2eepromMenuItem.setText("Copy RAM -> EEPROM"); // NOI18N
        ram2eepromMenuItem.setName("ram2eepromMenuItem"); // NOI18N
        ram2eepromMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ram2eepromMenuItemActionPerformed(evt);
            }
        });
        memoryMenu.add(ram2eepromMenuItem);

        eeprom2ramMenuItem.setText("Copy EEPROM -> RAM"); // NOI18N
        eeprom2ramMenuItem.setName("eeprom2ramMenuItem"); // NOI18N
        eeprom2ramMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                eeprom2ramMenuItemActionPerformed(evt);
            }
        });
        memoryMenu.add(eeprom2ramMenuItem);

        ToolsMenu.add(memoryMenu);

        serialSpyMenuItem.setText("Serial monitor"); // NOI18N
        serialSpyMenuItem.setName("serialSpyMenuItem"); // NOI18N
        serialSpyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serialSpyMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(serialSpyMenuItem);

        expertSeparator.setName("expertSeparator"); // NOI18N
        ToolsMenu.add(expertSeparator);

        expertModeMenuItem.setText("Expert mode"); // NOI18N
        expertModeMenuItem.setName("expertModeMenuItem"); // NOI18N
        expertModeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expertModeMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(expertModeMenuItem);

        menuBar.add(ToolsMenu);

        viewMenu.setText("View"); // NOI18N
        viewMenu.setName("viewMenu"); // NOI18N

        zoomInMenuItem.setText("Zoom in"); // NOI18N
        zoomInMenuItem.setName("zoomInMenuItem"); // NOI18N
        zoomInMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomInMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(zoomInMenuItem);

        zoomOutMenuItem.setText("Zoom out"); // NOI18N
        zoomOutMenuItem.setName("zoomOutMenuItem"); // NOI18N
        zoomOutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomOutMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(zoomOutMenuItem);

        zoomResetMenuItem.setText("Zoom 100 %"); // NOI18N
        zoomResetMenuItem.setName("zoomResetMenuItem"); // NOI18N
        zoomResetMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomResetMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(zoomResetMenuItem);

        menuBar.add(viewMenu);

        helpMenu.setText("Help"); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setText("About"); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        getContentPane().add(statusPanel, java.awt.BorderLayout.PAGE_END);

        setJMenuBar(menuBar);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void connectMenuActionPerformed(java.awt.event.ActionEvent evt) {                                            
        this.connectSerial();
    }                                                                                      

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        if (this.vdf != null)
            this.disconnect();
        else
            this.connectSerial();
    }//GEN-LAST:event_connectButtonActionPerformed

    private void connectSerial() {
        String port = this.selectedPort();
        if (port.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select the serial port of the PRM80.",
                    "Connect", JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.config.setSerialPort(port);
        this.connect(new SerialControler(), port, port);
    }

    private void upButtonActionPerformed(java.awt.event.ActionEvent evt) {                                         
        this.step(+1);
    }                                                                                

    private void downButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           
        this.step(-1);
    }                                                                                    

    /** Channel or frequency up / down: shown at once, sent in the background. */
    private void step(final int direction) {
        if (this.vdf == null)
            return;
        try {
            Controler c = this.vdf.getPRMControler();
            if (this.vdf.isVfoMode()) {
                int pllStep = c.getPLLStep();
                this.shownRx += direction * pllStep;
                this.shownTx += direction * pllStep;
                this.freqLabel.setText(formatFrequency(this.shownRx));
                this.txFreqLabel.setText(formatFrequency(this.shownTx));
            } else {
                int max = c.getMaxChan();
                this.shownChannel += direction;
                if (this.shownChannel > max)
                    this.shownChannel = 0;
                if (this.shownChannel < 0)
                    this.shownChannel = max;
                this.chanLabel.setText(formatChannel(this.shownChannel));
            }
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
        }
        final VirtualDeviceFirmware v = this.vdf;
        this.command(() -> {
            if (direction > 0)
                v.increment();
            else
                v.decrement();
            return null;
        });
    }

    private void zoomInMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomInMenuItemActionPerformed
        this.setZoom(Zoom.stepUp(Zoom.get()));
    }//GEN-LAST:event_zoomInMenuItemActionPerformed

    private void zoomOutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomOutMenuItemActionPerformed
        this.setZoom(Zoom.stepDown(Zoom.get()));
    }//GEN-LAST:event_zoomOutMenuItemActionPerformed

    private void zoomResetMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomResetMenuItemActionPerformed
        this.setZoom(1f);
    }//GEN-LAST:event_zoomResetMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        this.quit();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        this.showAboutBox();
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        this.quit();
    }//GEN-LAST:event_formWindowClosing

    private void disconnectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disconnectMenuItemActionPerformed
        this.disconnect();
    }//GEN-LAST:event_disconnectMenuItemActionPerformed

    private void memToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                
        if (this.vdf == null)
            return;
        this.setStatus(this.connectedStatus);
        this.chanLabel.setVisible(true);
        this.shownShiftKey = "";       // show the shift of the channel again
        this.updateModeControls();
        final VirtualDeviceFirmware v = this.vdf;
        this.command(() -> {
            v.setVfoMode(false);
            return null;
        });
    }                                                                                              

    private void vfoToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                
        if (this.vdf == null)
            return;
        this.setStatus(this.connectedStatus);
        this.chanLabel.setVisible(false);
        this.appliedReverse = this.ShiftReverseCheckbox.isSelected();
        this.updateModeControls();
        final VirtualDeviceFirmware v = this.vdf;
        final boolean alreadyVfo = v.isVfoMode();
        this.command(() -> {
            v.setVfoMode(true);
            if (!alreadyVfo) {
                // set the current frequencies with "R" at once: the radio then
                // hides its channel number now (as the program does), not only
                // when the frequency is changed the first time
                Controler c = v.getPRMControler();
                c.setFrequencies(c.getRxPLLFrequency(), c.getTxPLLFrequency());
            }
            return null;
        });
    }                                                                                              

    private void squelchSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {                                            
        if (this.updatingDisplay || this.vdf == null)
            return;
        int value = Integer.parseInt(this.squelchSpinner.getValue().toString());
        if (value < 0 || value > 15) {
            this.squelchSpinner.setValue(Math.max(0, Math.min(15, value)));
            return;     // the corrected value fires a new event
        }
        this.wantedSquelch.set(value);
        if (this.squelchQueued.compareAndSet(false, true)) {
            final VirtualDeviceFirmware v = this.vdf;
            this.commandWhileScanning(() -> {
                this.squelchQueued.set(false);
                v.setSquelch(this.wantedSquelch.get());
                return null;
            });
        }
    }                                                                                      

    private void volumeSliderStateChanged(javax.swing.event.ChangeEvent evt) {                                          
        if (this.updatingDisplay || this.vdf == null)
            return;
        this.wantedVolume.set(this.volumeSlider.getValue());
        if (this.volumeQueued.compareAndSet(false, true)) {
            final VirtualDeviceFirmware v = this.vdf;
            this.commandWhileScanning(() -> {
                this.volumeQueued.set(false);
                v.setVolume(this.wantedVolume.get());
                return null;
            });
        }
    }                                                                                  

    private void channelManagerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_channelManagerMenuItemActionPerformed
        this.scanner.stop();
        ChannelManager cm = new ChannelManager(this, new ChannelManager.PrmAccess() {
            @Override
            public boolean isConnected() {
                return vdf != null;
            }

            @Override
            public void readChannels(Consumer<ChannelList> done, Consumer<String> error) {
                final VirtualDeviceFirmware v = vdf;
                if (v == null) {
                    error.accept("Not connected");
                    return;
                }
                worker.run(() -> {
                    ChannelList list = v.getPRMControler().getChannels();
                    if (list == null)
                        throw new Exception("No valid answer from the PRM80");
                    return list;
                }, done, ex -> error.accept(message(ex)));
            }

            @Override
            public void writeChannels(ChannelList channels, Runnable done, Consumer<String> error) {
                final VirtualDeviceFirmware v = vdf;
                if (v == null) {
                    error.accept("Not connected");
                    return;
                }
                worker.run(() -> {
                    v.getPRMControler().setChannels(channels);
                    v.reset();
                    v.setCurrentChannel(0);
                    return null;
                }, result -> {
                    memToggleButton.setSelected(true);
                    chanLabel.setVisible(true);
                    updateModeControls();
                    done.run();
                }, ex -> error.accept(message(ex)));
            }

            @Override
            public void readConfiguration(Consumer<A51Export.Config> done, Consumer<String> error) {
                final VirtualDeviceFirmware v = vdf;
                if (v == null) {
                    done.accept(null);
                    return;
                }
                worker.run(() -> A51Export.Config.fromPrm(v.getPRMControler()),
                        done, ex -> error.accept(message(ex)));
            }
        });
        this.showDialog(cm);
        this.refreshDisplay();      // comments may have changed
    }//GEN-LAST:event_channelManagerMenuItemActionPerformed

    private void expertModeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expertModeMenuItemActionPerformed
        boolean expert = this.expertModeMenuItem.isSelected();
        this.config.setEpertMode(expert);
        this.setVisibleExpertControls(expert);
    }//GEN-LAST:event_expertModeMenuItemActionPerformed

    private void ShiftEnabledCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShiftEnabledCheckboxActionPerformed
        this.applyVfoShift();
    }//GEN-LAST:event_ShiftEnabledCheckboxActionPerformed

    private void ShiftPositiveCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShiftPositiveCheckboxActionPerformed
        this.applyVfoShift();
    }//GEN-LAST:event_ShiftPositiveCheckboxActionPerformed

    private void ShiftReverseCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShiftReverseCheckboxActionPerformed
        if (this.vfoToggleButton.isSelected()) {
            this.applyVfoShift();
            return;
        }
        // memory mode: reverse of the channel, as the reverse key of the radio
        if (this.vdf == null || this.updatingDisplay)
            return;
        final boolean reverse = this.ShiftReverseCheckbox.isSelected();
        final Controler c = this.vdf.getPRMControler();
        this.shownShiftKey = "";        // show the state of the radio afterwards
        this.command(() -> {
            c.setChannelReverse(reverse);
            return null;
        });
    }//GEN-LAST:event_ShiftReverseCheckboxActionPerformed

    private void ShiftFreqSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ShiftFreqSpinnerStateChanged
        this.applyVfoShift();
    }//GEN-LAST:event_ShiftFreqSpinnerStateChanged

    private void writeMemButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_writeMemButtonActionPerformed
        if (this.vdf == null)
            return;
        this.scanner.stop();        // e.g. a signal was found: store the frequency
        // the list of the channel manager is used; it is only read from the
        // PRM80 if it is not known to be the same
        if (ChannelManager.isSyncedWithPrm()) {
            this.showWriteMemory();
            return;
        }
        final VirtualDeviceFirmware v = this.vdf;
        final Controler c = v.getPRMControler();
        this.setStatus("Reading channels from PRM80 ...");
        this.worker.run(() -> {
            ChannelList list = c.getChannels();
            if (list == null)
                throw new Exception("No valid answer from the PRM80");
            return list;
        }, list -> {
            this.setStatus(this.connectedStatus);
            if (this.vdf != v)
                return;     // disconnected meanwhile
            ChannelList current = ChannelManager.getSessionList();
            if (current.countChannel() > 0 && !list.sameChannelsAs(current)) {
                int answer = JOptionPane.showConfirmDialog(this,
                        "The channel manager contains another channel list\n("
                        + ChannelManager.describeSessionList() + ").\n\n"
                        + "Replace it with the " + list.countChannel() + " channels read from the PRM80?",
                        "Write to MEM", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer != JOptionPane.OK_OPTION)
                    return;
            }
            ChannelManager.setPrmList(list);
            this.showWriteMemory();
        }, ex -> {
            this.setStatus("Error: " + message(ex));
            JOptionPane.showMessageDialog(this, "The channels could not be read:\n" + message(ex),
                    "PRM80", JOptionPane.ERROR_MESSAGE);
        });
    }//GEN-LAST:event_writeMemButtonActionPerformed

    /** Show the dialog to write the VFO to memory channels. */
    private void showWriteMemory() {
        final Controler c = this.vdf.getPRMControler();
        Channel vfo = this.vfoChannel();
        int base = this.vfoBase();
        int rx = this.vfoFrequencies(base)[0];
        int tx = this.vfoFrequencies(base)[1];
        // shift as stored in the channel (with reverse the PRM80 swaps RX and TX)
        int shift = this.vfoShift();
        String description = "VFO  RX " + formatFrequency(rx) + "  TX " + formatFrequency(tx)
                + (shift == 0 ? "  (no shift)" : "  (shift " + (shift > 0 ? "+" : "-")
                + new java.math.BigDecimal(Math.abs(shift)).movePointLeft(3).stripTrailingZeros().toPlainString() + " kHz"
                + (this.ShiftReverseCheckbox.isSelected() ? ", reverse" : "") + ")");
        WriteMemoryDialog d = new WriteMemoryDialog(this, ChannelManager.getSessionList(), vfo, description,
                new WriteMemoryDialog.MemoryAccess() {
            @Override
            public void writeChannel(Channel channel, Runnable done, Consumer<String> error) {
                worker.run(() -> {
                    c.writeChannel(channel);
                    return null;
                }, r -> done.run(), ex -> error.accept(message(ex)));
            }

            @Override
            public void deleteLastChannel(Runnable done, Consumer<String> error) {
                worker.run(() -> {
                    c.deleteLastChannel();
                    return null;
                }, r -> done.run(), ex -> error.accept(message(ex)));
            }
        });
        this.showDialog(d);
        this.refreshDisplay();
    }

    /**
     * VFO: reverse state the shown RX / TX frequencies were calculated with.
     * Normal: RX = base, TX = base +/- shift. Reverse (as in the firmware):
     * TX = base, RX = base +/- shift.
     */
    private boolean appliedReverse;

    /** @return the shift in Hz set in the fields, 0 if shift is not enabled */
    private int vfoShift() {
        if (!this.ShiftEnabledCheckbox.isSelected())
            return 0;
        int shift = Integer.parseInt(this.ShiftFreqSpinner.getValue().toString());
        return this.ShiftPositiveCheckbox.isSelected() ? shift : -shift;
    }

    /** @return the base frequency of the VFO (the channel frequency) */
    private int vfoBase() {
        return this.appliedReverse ? this.shownTx : this.shownRx;
    }

    /** @return {RX, TX} for a base frequency and the shift fields */
    private int[] vfoFrequencies(int base) {
        int shifted = base + this.vfoShift();
        return this.ShiftReverseCheckbox.isSelected() ? new int[] {shifted, base} : new int[] {base, shifted};
    }

    /** VFO mode: a shift field was changed, set RX / TX at once. */
    private void applyVfoShift() {
        if (this.vdf == null || this.updatingDisplay || !this.vfoToggleButton.isSelected())
            return;
        int base = this.vfoBase();
        final int[] f = this.vfoFrequencies(base);
        this.appliedReverse = this.ShiftReverseCheckbox.isSelected();
        if (f[0] == this.shownRx && f[1] == this.shownTx)
            return;
        this.shownRx = f[0];
        this.shownTx = f[1];
        this.freqLabel.setText(formatFrequency(f[0]));
        this.txFreqLabel.setText(formatFrequency(f[1]));
        final Controler c = this.vdf.getPRMControler();
        this.command(() -> {
            c.setFrequencies(f[0], f[1]);
            return null;
        });
    }

    /** @return the VFO setting as memory channel */
    private Channel vfoChannel() {
        int shift = this.vfoShift();
        boolean enabled = this.ShiftEnabledCheckbox.isSelected();
        Channel channel = new Channel(this.vfoBase(), Math.abs(shift), "",
                this.ShiftReverseCheckbox.isSelected() ? "Reverse" : "",
                enabled ? "Enabled" : "", enabled ? (shift >= 0 ? "+" : "-") : "");
        return channel;
    }

    /** "Write to MEM" only in VFO mode. */
    private void updateModeControls() {
        boolean vfo = this.vdf != null && this.vfoToggleButton.isSelected();
        this.writeMemButton.setVisible(vfo);
        this.updateShiftControls(vfo);
    }

    private static final String REVERSE_TIP =
            "Reverse: receive on the TX frequency of the repeater and transmit on its RX frequency";
    private static final String MEM_SHIFT_TIP =
            "Memory mode: shows the shift of the channel (change it in the channel manager)";
    private static final String MEM_REVERSE_TIP =
            "<html>Reverse for this channel, as the reverse key of the radio<br>"
            + "(firmware V5.2: ends when another channel is selected)</html>";
    private static final String MEM_REVERSE_OFF_TIP =
            "<html>Reverse needs a channel with shift enabled<br>"
            + "(change the shift in the channel manager)</html>";

    /**
     * The shift fields only act in VFO mode. In memory mode (and without
     * connection) they only show the shift of the channel and are greyed.
     */
    private void updateShiftControls(boolean vfo) {
        this.ShiftEnabledCheckbox.setEnabled(vfo);
        this.ShiftPositiveCheckbox.setEnabled(vfo);
        this.ShiftFreqSpinner.setEnabled(vfo);
        this.label1.setEnabled(vfo);
        String tip = this.vdf != null && !vfo ? MEM_SHIFT_TIP : null;
        this.ShiftEnabledCheckbox.setToolTipText(tip);
        this.ShiftPositiveCheckbox.setToolTipText(tip);
        this.ShiftFreqSpinner.setToolTipText(tip);
        // reverse is an operating function: also in memory mode, as the
        // reverse key of the radio, if the channel has shift enabled
        boolean memReverse = false;
        if (this.vdf != null && !vfo) {
            int state = this.vdf.getPRMControler().getChannelState();
            memReverse = state >= 0 && (state & 1) != 0;
        }
        this.ShiftReverseCheckbox.setEnabled(vfo || memReverse);
        this.ShiftReverseCheckbox.setToolTipText(vfo || this.vdf == null ? REVERSE_TIP
                : memReverse ? MEM_REVERSE_TIP : MEM_REVERSE_OFF_TIP);
    }

    // ---- Scan -------------------------------------------------------------

    private void scanToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scanToggleButtonActionPerformed
        if (this.scanner.isRunning()) {
            this.scanner.stop();
            this.setStatus(this.connectedStatus);
            return;
        }
        this.startScan();
    }//GEN-LAST:event_scanToggleButtonActionPerformed

    /** Start the memory scan (MEM) or the VFO scan (VFO). */
    private void startScan() {
        this.scanToggleButton.setSelected(false);
        if (this.vdf == null)
            return;
        Controler c = this.vdf.getPRMControler();
        try {
            if (this.vfoToggleButton.isSelected()) {
                int band = c.getPRMFrequencyCode();
                int pllStep = c.getPLLStep();
                if (ScanSettingsDialog.isRangeUnset(this.config, band)) {
                    // first VFO scan: ask for the range
                    ScanSettingsDialog d = new ScanSettingsDialog(this, band, pllStep);
                    this.showDialog(d);
                    if (!d.isSaved())
                        return;
                }
                int[] range = ScanSettingsDialog.range(this.config, band);
                int step = ScanSettingsDialog.step(this.config, pllStep);
                this.scanner.startVfo(c, range[0], range[1], step, this.vfoBase(),
                        this::vfoFrequencies, this.config.getScanHold());
            } else {
                // scan lock of all channels: the channel manager list if it is
                // known to be the list in the PRM80, otherwise the scanner reads it
                this.scanner.startMemory(c, ChannelManager.isSyncedWithPrm() ? ChannelManager.getSessionList() : null,
                        this.config.getScanHold());
            }
        } catch (CommunicationException ex) {
            this.setStatus("Error: " + message(ex));
        }
    }

    /** Scan button, SCAN / HOLD in the display, status line. */
    private void showScanState() {
        boolean running = this.scanner.isRunning();
        this.scanToggleButton.setSelected(running);
        this.scanLabel.setVisible(running);
        if (!running) {
            if (this.vdf != null)
                this.setStatus(this.connectedStatus);
            return;
        }
        boolean hold = this.scanner.isHolding();
        this.scanLabel.setText(hold ? "HOLD" : "SCAN");
        this.scanLabel.setBackground(hold ? new java.awt.Color(255, 204, 0) : new java.awt.Color(0, 204, 0));
        if (hold) {
            this.setStatus("Signal - the scan continues " + Math.round(this.config.getScanHold() / 1000f)
                    + " s after its end");
        } else if (this.scanner.isVfoScan()) {
            this.setStatus("Scanning " + mhz(this.scanner.getFrom()) + " - " + mhz(this.scanner.getTo())
                    + " MHz, step " + new java.math.BigDecimal(this.scanner.getStep()).movePointLeft(3)
                    .stripTrailingZeros().toPlainString() + " kHz");
        } else if (this.scanner.isPreparing()) {
            this.setStatus("Scan: reading the channel list (scan lock) ...");
        } else {
            this.setStatus("Scanning the memory channels");
        }
    }

    private static String mhz(int hz) {
        java.math.BigDecimal m = new java.math.BigDecimal(hz).movePointLeft(6).stripTrailingZeros();
        return (m.scale() < 3 ? m.setScale(3) : m).toPlainString();
    }

    private void scanSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scanSettingsMenuItemActionPerformed
        int band = Controler.FREQ144;
        int pllStep = 12500;
        if (this.vdf != null) {
            try {
                band = this.vdf.getPRMControler().getPRMFrequencyCode();
                pllStep = this.vdf.getPRMControler().getPLLStep();
            } catch (CommunicationException ex) {
                Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        ScanSettingsDialog d = new ScanSettingsDialog(this, band, pllStep);
        this.showDialog(d);
        if (d.isSaved() && this.scanner.isRunning()) {
            this.scanner.stop();
            this.startScan();       // with the new settings
        }
    }//GEN-LAST:event_scanSettingsMenuItemActionPerformed

    private void connectVirtualMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectVirtualMenuItemActionPerformed
        this.connect(new DummyControler(), "", "virtual PRM");
    }//GEN-LAST:event_connectVirtualMenuItemActionPerformed

    private void serialSpyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serialSpyMenuItemActionPerformed
        if (this.serialSpy == null) {
            this.serialSpy = new TerminalDialog();
            this.serialSpy.setLocationRelativeTo(this);
            if (this.vdf != null && this.vdf.getPRMControler() instanceof PRMControler) {
                ((PRMControler) this.vdf.getPRMControler()).addSerialListener(this.serialSpy);
            }
        }
        this.serialSpy.setVisible(true);
        this.serialSpy.toFront();
    }//GEN-LAST:event_serialSpyMenuItemActionPerformed

    private void powerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_powerButtonActionPerformed
        if (this.vdf == null)
            return;
        final boolean power = !this.shownHighPower;
        this.shownHighPower = power;
        this.hpLabel.setVisible(power);
        this.lpLabel.setVisible(!power);
        final VirtualDeviceFirmware v = this.vdf;
        this.commandWhileScanning(() -> {
            v.setHighPower(power);
            return null;
        });
    }//GEN-LAST:event_powerButtonActionPerformed

private void connectTcpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectTcpMenuItemActionPerformed
    ConnectDialog cd = new ConnectDialog(this, true);
    this.showDialog(cd);
    final String server = cd.getServerAdress();
    if (server != null) {        
        this.config.setLastServerURI(server);
        this.connect(new TcpControler(), server, server);
    }
}//GEN-LAST:event_connectTcpMenuItemActionPerformed

private void resetMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetMenuItemActionPerformed
    final VirtualDeviceFirmware v = this.vdf;
    this.command(() -> {
        v.reset();
        return null;
    });
}//GEN-LAST:event_resetMenuItemActionPerformed

private void configurationMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configurationMenuItemActionPerformed
    if (this.vdf == null)
        return;
    this.scanner.stop();
    final VirtualDeviceFirmware v = this.vdf;
    final Controler pc = v.getPRMControler();
    // read the configuration values in the background, then show the dialog
    this.worker.run(() -> {
        pc.getPLLStep();
        if (pc.supportsTxShift())
            pc.getTxFrequencyShift();
        if (pc.supportsScanSpeed())
            pc.getScanSpeed();
        return null;
    }, r -> this.showConfiguration(v), ex -> {
        this.setStatus("Error: " + message(ex));
        JOptionPane.showMessageDialog(this, "The configuration could not be read:\n" + message(ex),
                "PRM80", JOptionPane.ERROR_MESSAGE);
    });
}//GEN-LAST:event_configurationMenuItemActionPerformed

private void showConfiguration(VirtualDeviceFirmware v) {
    if (this.vdf != v)
        return;     // disconnected meanwhile
    final ConfigurationDialog cd = new ConfigurationDialog(this, this.vdf);
    this.showDialog(cd);
    if (cd.isSaved()) {
        final Controler c = this.vdf.getPRMControler();
        this.command(() -> {
            if (cd.getPllStep() != c.getPLLStep())
                c.setPLLStep(cd.getPllStep());
            if (c.supportsTxShift() && cd.getTxShift() != c.getTxFrequencyShift())
                c.setTxFrequencyShift(cd.getTxShift());
            if (c.supportsScanSpeed() && cd.getScanSpeed() != c.getScanSpeed())
                c.setScanSpeed(cd.getScanSpeed());
            c.refreshState();
            return null;
        });
    }
}

private void ram2eepromMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ram2eepromMenuItemActionPerformed
    final VirtualDeviceFirmware v = this.vdf;
    this.command(() -> {
        v.getPRMControler().RAM2EEPROM();
        return null;
    });
}//GEN-LAST:event_ram2eepromMenuItemActionPerformed

private void eeprom2ramMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_eeprom2ramMenuItemActionPerformed
    final VirtualDeviceFirmware v = this.vdf;
    this.command(() -> {
        v.getPRMControler().EEPROM2RAM();
        return null;
    });
}//GEN-LAST:event_eeprom2ramMenuItemActionPerformed

private void razMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_razMenuItemActionPerformed
    final VirtualDeviceFirmware v = this.vdf;
    this.command(() -> {
        v.getPRMControler().reloadRAM();
        return null;
    });
}//GEN-LAST:event_razMenuItemActionPerformed

private void chanLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_chanLabelMouseClicked
    if (this.vdf == null || evt.getButton() != MouseEvent.BUTTON1)
        return;
    SelectChannelDialog d = new SelectChannelDialog(this, true);
    d.setChannel(this.shownChannel);
    this.showDialog(d);
    final int channel = d.getChannel();
    if (channel != -1) {
        this.shownChannel = channel;
        this.chanLabel.setText(formatChannel(channel));
        final VirtualDeviceFirmware v = this.vdf;
        this.command(() -> {
            v.setCurrentChannel(channel);
            return null;
        });
    }
}//GEN-LAST:event_chanLabelMouseClicked

private void freqLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_freqLabelMouseClicked
    if (this.vdf == null || evt.getButton() != MouseEvent.BUTTON1)
        return;
    SelectFrequencyDialog d = new SelectFrequencyDialog(this, true);
    d.setRxFrequency(this.shownRx);
    this.showDialog(d);
    final int rx = d.getRxFrequency();
    if (rx != -1) {
        // the entered frequency is the RX frequency; with reverse the base
        // (TX) frequency is RX minus the shift
        int base = this.ShiftReverseCheckbox.isSelected() ? rx - this.vfoShift() : rx;
        final int txFreq = this.vfoFrequencies(base)[1];
        this.appliedReverse = this.ShiftReverseCheckbox.isSelected();
        // show at once
        this.shownRx = rx;
        this.shownTx = txFreq;
        this.freqLabel.setText(formatFrequency(rx));
        this.txFreqLabel.setText(formatFrequency(txFreq));
        this.chanLabel.setVisible(false);
        this.vfoToggleButton.setSelected(true);
        this.updateModeControls();
        final VirtualDeviceFirmware v = this.vdf;
        this.command(() -> {
            v.getPRMControler().setFrequencies(rx, txFreq);
            v.setVfoMode(true);
            return null;
        });
    }
}//GEN-LAST:event_freqLabelMouseClicked
    
    public Option getConfiguration() {
        return this.config;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel busyLabel;
    private javax.swing.JLabel scanLabel;
    private javax.swing.JLabel rxUnitLabel;
    private javax.swing.JLabel txUnitLabel;
    private javax.swing.JLabel chnlUnitLabel;
    private javax.swing.JLabel commentLabel;
    private javax.swing.JCheckBox ShiftReverseCheckbox;
    private javax.swing.JButton connectButton;
    private javax.swing.JPanel connectPanel;
    private javax.swing.JComboBox<String> portComboBox;
    private javax.swing.JLabel portLabel;
    private javax.swing.JMenu ToolsMenu;
    private javax.swing.JLabel chanLabel;
    private javax.swing.JMenuItem channelManagerMenuItem;
    private javax.swing.JCheckBox ShiftEnabledCheckbox;
    private javax.swing.JCheckBox ShiftPositiveCheckbox;
    private javax.swing.JMenuItem configurationMenuItem;
    private javax.swing.JMenuItem scanSettingsMenuItem;
    private javax.swing.JMenuItem connectMenuItem;
    private javax.swing.JMenuItem connectTcpMenuItem;
    private javax.swing.JMenuItem connectVirtualMenuItem;
    private javax.swing.JMenuItem disconnectMenuItem;
    private javax.swing.JButton downButton;
    private javax.swing.JMenuItem eeprom2ramMenuItem;
    private javax.swing.JMenuItem eepromMenuItem;
    private javax.swing.JSeparator expertSeparator;
    private javax.swing.JLabel freqLabel;
    private javax.swing.JLabel hpLabel;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSpinner ShiftFreqSpinner;
    private javax.swing.JLabel label1;
    private javax.swing.JLabel lpLabel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JToggleButton memToggleButton;
    private javax.swing.JMenu memoryMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.ButtonGroup modeButtonGroup;
    private javax.swing.JCheckBoxMenuItem expertModeMenuItem;
    private javax.swing.JButton writeMemButton;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JMenuItem zoomInMenuItem;
    private javax.swing.JMenuItem zoomOutMenuItem;
    private javax.swing.JMenuItem zoomResetMenuItem;
    private javax.swing.JButton powerButton;
    private javax.swing.JToggleButton scanToggleButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JMenuItem ram2eepromMenuItem;
    private javax.swing.JMenuItem ramExtMenuItem;
    private javax.swing.JMenuItem ramIntMenuItem;
    private javax.swing.JMenuItem razMenuItem;
    private javax.swing.JMenuItem resetMenuItem;
    private javax.swing.JMenuItem serialSpyMenuItem;
    private javax.swing.JLabel squelchLabel;
    private javax.swing.JSpinner squelchSpinner;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JLabel txFreqLabel;
    private javax.swing.JButton upButton;
    private javax.swing.JToggleButton vfoToggleButton;
    private javax.swing.JLabel volumeLabel;
    private javax.swing.JSlider volumeSlider;
    // End of variables declaration//GEN-END:variables

    private JDialog aboutBox;
}
