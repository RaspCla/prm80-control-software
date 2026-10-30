/*
 * ChannelManager.java
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

package f4fez.prm80x0.gui.channelmanager;

import f4fez.prm80x0.A51Export;
import f4fez.prm80x0.Controler.ChannelList;
import f4fez.prm80x0.Option;
import f4fez.prm80x0.gui.Zoom;
import java.awt.Cursor;
import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Edit the channel list. The list can be read from / written to the PRM80
 * and read from / saved to a CSV file.
 * <p>
 * The dialog does not talk to the PRM80 itself, it uses the {@link PrmAccess}
 * given by the main window (communication runs in the background there).
 *
 * @author  fmazen
 */
public class ChannelManager extends javax.swing.JDialog {
    private static final long serialVersionUID = 1L;

    /** Access to the PRM80, provided by the main window. */
    public interface PrmAccess {
        /** @return true if a PRM80 is connected */
        boolean isConnected();
        /** Read the channels in the background, then call done or error (in the GUI thread). */
        void readChannels(Consumer<ChannelList> done, Consumer<String> error);
        /** Write the channels in the background, then call done or error (in the GUI thread). */
        void writeChannels(ChannelList channels, Runnable done, Consumer<String> error);
        /**
         * Read the configuration for the A51 export (firmware version, PLL step,
         * shift, scan speed, band) in the background, then call done or error
         * (in the GUI thread). done gets null if no PRM80 is connected.
         */
        void readConfiguration(Consumer<A51Export.Config> done, Consumer<String> error);
    }

    // The list stays in memory while the program runs, so closing and
    // opening the dialog again shows the same (also unsaved) channels.
    private static ChannelList sessionList;
    private static String sessionFile;          // file of the list, null if not saved yet
    private static String sessionSource = "";   // shown in the status line
    private static boolean sessionModified;
    /**
     * true while the list is known to be the same as the channels in the
     * connected PRM80: read from / written to the PRM80, since then only
     * changed by "Write to MEM" (comments do not count, the PRM80 has none)
     */
    private static boolean sessionSyncedWithPrm;

    private final ChannelModel model = new ChannelModel(null);

    // ---- channel list shared with the main window ("Write to MEM") --------

    /**
     * @return the channel list of the channel manager. At the first call the
     * channel file used last time is read (as when the dialog is opened).
     */
    public static ChannelList getSessionList() {
        loadSessionList();
        return sessionList;
    }

    /** @return true if the list is the same as the channels in the PRM80 */
    public static boolean isSyncedWithPrm() {
        return sessionList != null && sessionSyncedWithPrm;
    }

    /**
     * @return short description of the list for questions to the user,
     * e.g. "kanaele.csv, 12 channels, not saved"
     */
    public static String describeSessionList() {
        loadSessionList();
        String text = (sessionFile == null ? "not saved in a file" : new File(sessionFile).getName())
                + ", " + sessionList.countChannel() + " channels";
        if (sessionModified && sessionFile != null)
            text += ", changed and not saved";
        return text;
    }

    /**
     * Use channels read from the PRM80 as list of the channel manager.
     * The comments of the old list are kept (same number and frequency).
     */
    public static void setPrmList(ChannelList list) {
        loadSessionList();
        list.takeCommentsFrom(sessionList);
        sessionList = list;
        sessionSource = "Read from PRM80: " + list.countChannel() + " channels";
        sessionModified = true;     // not in a file
        sessionSyncedWithPrm = true;
    }

    /** A channel was written to the PRM80 ("Write to MEM"): update the list. */
    public static void prmChannelWritten(f4fez.prm80x0.Controler.Channel channel) {
        if (!isSyncedWithPrm())
            return;
        if (channel.getId() < sessionList.countChannel())
            sessionList.setChannel(channel.getId(), channel);
        else
            sessionList.addChannel(channel);
        sessionSource = "Channel " + channel.getId() + " written to PRM80";
        sessionModified = true;
    }

    /** The last channel was removed in the PRM80 ("Undo" in Write to MEM): update the list. */
    public static void prmLastChannelRemoved() {
        if (!isSyncedWithPrm())
            return;
        sessionList.removeLast();
        sessionSource = "Channel " + sessionList.countChannel() + " removed in PRM80";
        sessionModified = true;
    }

    /** Connected to another (or no) PRM80: the list is no longer known to match. */
    public static void prmConnectionChanged() {
        sessionSyncedWithPrm = false;
    }

    /** The RAM of the PRM80 was replaced by the EEPROM: the list shown may be old. */
    public static void prmMemoryReplaced() {
        loadSessionList();
        sessionSyncedWithPrm = false;
        sessionSource = "The PRM80 has loaded its EEPROM: \"Read from PRM80\" for its channels";
    }

    /** @return the channel file shown (or used last), null if none */
    public static String currentFile() {
        loadSessionList();
        if (sessionFile != null)
            return sessionFile;
        String file = new Option().getChannelFile();
        return file.isEmpty() ? null : file;
    }

    /** First use: show the channel file used last time. */
    private static void loadSessionList() {
        if (sessionList != null)
            return;
        sessionList = new ChannelList();
        sessionSource = "Empty list - read the channels from the PRM80 or from a file";
        String file = new Option().getChannelFile();
        if (!file.isEmpty() && new File(file).isFile()) {
            try {
                ChannelList list = new ChannelList();
                list.importCSV(file);
                sessionList = list;
                sessionFile = file;
                sessionSource = "Read from file";
            } catch (Exception ex) {
                sessionSource = "Could not read " + file + ": " + ex.getMessage();
            }
        }
        sessionModified = false;
        sessionSyncedWithPrm = false;
    }
    static final String READ_PRM_TIP = "<html>Read the channels from the <b>RAM</b> of the PRM80 (the channels in use now).<br>"
            + "The EEPROM (permanent copy) is not read.</html>";
    static final String WRITE_PRM_TIP = "<html>Write the channels to the <b>RAM</b> of the PRM80.<br>"
            + "The RAM keeps them for some weeks without power.<br>"
            + "To keep them permanently: main window, Tools &gt; Memory &gt; Copy RAM -&gt; EEPROM<br>"
            + "(this copies the channel list <b>and all</b> settings).</html>";
    private static final String EEPROM_HINT = "To keep them permanently: Tools > Memory > Copy RAM -> EEPROM";

    private final PrmAccess prm;
    private final Option config = new Option();
    private boolean busy;

    /** Creates new form ChannelManager */
    public ChannelManager(java.awt.Frame parent, PrmAccess prm) {
        super(parent, true);
        this.prm = prm;
        initComponents();

        this.table.getColumnModel().getColumn(ChannelModel.COL_SCANLOCK).setCellEditor(new DefaultCellEditor(combo("", "Lock")));
        this.table.getColumnModel().getColumn(ChannelModel.COL_SHIFTPOSNEG).setCellEditor(new DefaultCellEditor(combo("", "-", "+")));
        this.table.getColumnModel().getColumn(ChannelModel.COL_SHIFTREVERSE).setCellEditor(new DefaultCellEditor(combo("", "Reverse")));
        this.table.getColumnModel().getColumn(ChannelModel.COL_SHIFTENABLED).setCellEditor(new DefaultCellEditor(combo("", "Enabled")));
        int[] widths = {55, 95, 80, 80, 65, 105, 105, 180};
        for (int i = 0; i < widths.length; i++)
            this.table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        this.table.setPreferredScrollableViewportSize(new java.awt.Dimension(765, 320));
        this.installShiftTooltips();
        this.table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        this.model.addTableModelListener(e -> {
            if (!busy) {
                sessionModified = true;
                // a changed channel is no longer the one in the PRM80 (comments are not in the PRM80)
                if (e.getColumn() != ChannelModel.COL_COMMENTS)
                    sessionSyncedWithPrm = false;
                updateTitle();
                // a value edited in one of several selected rows: offer it for all of them
                if (!this.applyingToSelection && e.getType() == javax.swing.event.TableModelEvent.UPDATE
                        && e.getFirstRow() == e.getLastRow() && e.getColumn() >= 0) {
                    final int row = e.getFirstRow();
                    final int col = e.getColumn();
                    javax.swing.SwingUtilities.invokeLater(() -> this.applyToSelection(row, col));
                }
            }
        });

        // "Undo": the list is remembered before every change
        this.model.setBeforeEdit(() -> {
            if (!this.busy && !this.applyingToSelection)
                this.remember();
        });
        this.bindKeys();

        // first call: show the channel file used last time
        loadSessionList();
        this.showList(sessionList);

        Zoom.install(this);
        this.updateButtons();
        this.updateTitle();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bottomPanel = new javax.swing.JPanel();
        statusPanel = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        prmButtonsPanel = new javax.swing.JPanel();
        readPrmButton = new javax.swing.JButton();
        writePrmButton = new javax.swing.JButton();
        fileButtonsPanel = new javax.swing.JPanel();
        readFileButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        saveAsButton = new javax.swing.JButton();
        verifyButton = new javax.swing.JButton();
        exportA51Button = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        editButtonsPanel = new javax.swing.JPanel();
        insertButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        moveUpButton = new javax.swing.JButton();
        moveDownButton = new javax.swing.JButton();
        undoButton = new javax.swing.JButton();
        redoButton = new javax.swing.JButton();
        tableScroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        closeMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Channel manager"); // NOI18N
        setName("Form"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        bottomPanel.setName("bottomPanel"); // NOI18N
        bottomPanel.setLayout(new java.awt.BorderLayout());

        statusPanel.setName("statusPanel"); // NOI18N
        statusPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        statusLabel.setText(" "); // NOI18N
        statusLabel.setName("statusLabel"); // NOI18N
        statusPanel.add(statusLabel);

        bottomPanel.add(statusPanel, java.awt.BorderLayout.PAGE_START);

        prmButtonsPanel.setName("prmButtonsPanel"); // NOI18N
        prmButtonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        readPrmButton.setText("Read from PRM80"); // NOI18N
        readPrmButton.setToolTipText(READ_PRM_TIP);
        readPrmButton.setName("readPrmButton"); // NOI18N
        readPrmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readPrmButtonActionPerformed(evt);
            }
        });
        prmButtonsPanel.add(readPrmButton);

        writePrmButton.setText("Write to PRM80"); // NOI18N
        writePrmButton.setToolTipText(WRITE_PRM_TIP);
        writePrmButton.setName("writePrmButton"); // NOI18N
        writePrmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                writePrmButtonActionPerformed(evt);
            }
        });
        prmButtonsPanel.add(writePrmButton);

        bottomPanel.add(prmButtonsPanel, java.awt.BorderLayout.WEST);

        fileButtonsPanel.setName("fileButtonsPanel"); // NOI18N
        fileButtonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        readFileButton.setText("Read"); // NOI18N
        readFileButton.setName("readFileButton"); // NOI18N
        readFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readFileButtonActionPerformed(evt);
            }
        });
        fileButtonsPanel.add(readFileButton);

        saveButton.setText("Save"); // NOI18N
        saveButton.setName("saveButton"); // NOI18N
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        fileButtonsPanel.add(saveButton);

        saveAsButton.setText("Save As"); // NOI18N
        saveAsButton.setName("saveAsButton"); // NOI18N
        saveAsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsButtonActionPerformed(evt);
            }
        });
        fileButtonsPanel.add(saveAsButton);

        verifyButton.setText("Verify"); // NOI18N
        verifyButton.setToolTipText("Compare the list shown with a channel file (e.g. after Read from PRM80)"); // NOI18N
        verifyButton.setName("verifyButton"); // NOI18N
        verifyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verifyButtonActionPerformed(evt);
            }
        });
        fileButtonsPanel.add(verifyButton);

        exportA51Button.setText("Export A51"); // NOI18N
        exportA51Button.setToolTipText("Save the list as channel configuration of the firmware (src/inc_144.a51 or src/inc_430.a51)"); // NOI18N
        exportA51Button.setName("exportA51Button"); // NOI18N
        exportA51Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportA51ButtonActionPerformed(evt);
            }
        });
        fileButtonsPanel.add(exportA51Button);

        closeButton.setText("Close"); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });
        fileButtonsPanel.add(closeButton);

        bottomPanel.add(fileButtonsPanel, java.awt.BorderLayout.EAST);

        getContentPane().add(bottomPanel, java.awt.BorderLayout.PAGE_END);

        editButtonsPanel.setName("editButtonsPanel"); // NOI18N
        editButtonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        insertButton.setText("Insert"); // NOI18N
        insertButton.setToolTipText("Insert a copy of the selected channel before it (Insert key)"); // NOI18N
        insertButton.setName("insertButton"); // NOI18N
        insertButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                insertButtonActionPerformed(evt);
            }
        });
        editButtonsPanel.add(insertButton);

        deleteButton.setText("Delete"); // NOI18N
        deleteButton.setToolTipText("Delete the selected channels, the following channels move forward (Del key)"); // NOI18N
        deleteButton.setName("deleteButton"); // NOI18N
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });
        editButtonsPanel.add(deleteButton);

        moveUpButton.setText("Move up"); // NOI18N
        moveUpButton.setToolTipText("Move the selected channels up (Alt+Up)"); // NOI18N
        moveUpButton.setName("moveUpButton"); // NOI18N
        moveUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpButtonActionPerformed(evt);
            }
        });
        editButtonsPanel.add(moveUpButton);

        moveDownButton.setText("Move down"); // NOI18N
        moveDownButton.setToolTipText("Move the selected channels down (Alt+Down)"); // NOI18N
        moveDownButton.setName("moveDownButton"); // NOI18N
        moveDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDownButtonActionPerformed(evt);
            }
        });
        editButtonsPanel.add(moveDownButton);

        undoButton.setText("Undo"); // NOI18N
        undoButton.setToolTipText("Undo the last change of the list (Ctrl+Z)"); // NOI18N
        undoButton.setName("undoButton"); // NOI18N
        undoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoButtonActionPerformed(evt);
            }
        });
        editButtonsPanel.add(undoButton);

        redoButton.setText("Redo"); // NOI18N
        redoButton.setToolTipText("Redo the change undone last (Ctrl+Y)"); // NOI18N
        redoButton.setName("redoButton"); // NOI18N
        redoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redoButtonActionPerformed(evt);
            }
        });
        editButtonsPanel.add(redoButton);

        getContentPane().add(editButtonsPanel, java.awt.BorderLayout.PAGE_START);

        tableScroll.setName("tableScroll"); // NOI18N

        table.setModel(model);
        table.setName("table"); // NOI18N
        tableScroll.setViewportView(table);

        getContentPane().add(tableScroll, java.awt.BorderLayout.CENTER);

        jMenuBar1.setName("jMenuBar1"); // NOI18N

        fileMenu.setText("File"); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        closeMenuItem.setText("Close"); // NOI18N
        closeMenuItem.setName("closeMenuItem"); // NOI18N
        closeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(closeMenuItem);

        jMenuBar1.add(fileMenu);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void readPrmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readPrmButtonActionPerformed
        this.stopEditing();
        if (sessionModified && sessionList.countChannel() > 0
                && !this.confirm("The channels shown are not saved.\nReplace them with the channels of the PRM80?"))
            return;
        this.setBusy(true, "Reading channels from PRM80 ...");
        final ChannelList before = sessionList;
        this.prm.readChannels(list -> {
            // the PRM80 has no memory for comments: keep those of the list shown before
            list.takeCommentsFrom(before);
            sessionList = list;
            sessionSource = "Read from the RAM of the PRM80: " + list.countChannel() + " channels (not from the EEPROM)";
            this.showList(list);
            sessionModified = true;     // not saved in a file yet
            sessionSyncedWithPrm = true;
            this.setBusy(false, null);
        }, error -> {
            this.setBusy(false, "Reading from PRM80 failed: " + error);
        });
    }//GEN-LAST:event_readPrmButtonActionPerformed

    private void writePrmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_writePrmButtonActionPerformed
        this.stopEditing();
        if (sessionList.countChannel() == 0) {
            JOptionPane.showMessageDialog(this, "The channel list is empty.", "Write to PRM80", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (sessionList.countChannel() > ChannelList.MAX_CHANNELS) {
            JOptionPane.showMessageDialog(this, "The PRM80 has only " + ChannelList.MAX_CHANNELS + " channels (0 to "
                    + (ChannelList.MAX_CHANNELS - 1) + ").", "Write to PRM80", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!this.confirm("Write " + sessionList.countChannel() + " channels to the PRM80?\n"
                + "The channels in the PRM80 will be replaced.\n\n"
                + "They are written to the RAM of the PRM80, which keeps them for some weeks without power.\n"
                + EEPROM_HINT + "\n(this copies all settings and the channel list)."))
            return;
        this.setBusy(true, "Writing " + sessionList.countChannel() + " channels to PRM80 ...");
        this.prm.writeChannels(sessionList, () -> {
            sessionSyncedWithPrm = true;
            this.setBusy(false, "Written to the RAM of the PRM80: " + sessionList.countChannel()
                    + " channels - permanently: Tools > Memory > Copy RAM -> EEPROM");
        }, error -> {
            this.setBusy(false, "Writing to PRM80 failed: " + error);
        });
    }//GEN-LAST:event_writePrmButtonActionPerformed

    private void readFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readFileButtonActionPerformed
        this.stopEditing();
        if (sessionModified && sessionList.countChannel() > 0
                && !this.confirm("The channels shown are not saved.\nReplace them with the channels of a file?"))
            return;
        JFileChooser fc = this.fileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        String file = fc.getSelectedFile().getAbsolutePath();
        try {
            ChannelList list = new ChannelList();
            list.importCSV(file);
            sessionList = list;
            sessionFile = file;
            sessionSource = "Read from file: " + list.countChannel() + " channels";
            this.config.setChannelFile(file);
            this.showList(list);
            sessionModified = false;
            sessionSyncedWithPrm = false;
            this.updateTitle();
        } catch (Exception ex) {
            Logger.getLogger(ChannelManager.class.getName()).log(Level.INFO, null, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_readFileButtonActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        this.save(false);
    }//GEN-LAST:event_saveButtonActionPerformed

    private void saveAsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsButtonActionPerformed
        this.save(true);
    }//GEN-LAST:event_saveAsButtonActionPerformed

    private void exportA51ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportA51ButtonActionPerformed
        this.stopEditing();
        if (sessionList.countChannel() == 0) {
            JOptionPane.showMessageDialog(this, "The channel list is empty.", "Export A51", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (sessionList.countChannel() > ChannelList.MAX_CHANNELS) {
            JOptionPane.showMessageDialog(this, "The PRM80 has only " + ChannelList.MAX_CHANNELS + " channels (0 to "
                    + (ChannelList.MAX_CHANNELS - 1) + ").", "Export A51", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (this.prm != null && this.prm.isConnected()) {
            // PLL step, shift and scan speed of the connected PRM80
            this.setBusy(true, "Reading configuration from PRM80 ...");
            this.prm.readConfiguration(cfg -> {
                this.setBusy(false, null);
                this.exportA51(cfg);
            }, error -> {
                this.setBusy(false, "Reading configuration from PRM80 failed: " + error);
            });
        } else {
            this.exportA51(null);
        }
    }//GEN-LAST:event_exportA51ButtonActionPerformed

    /** Ask for the file name and write the A51 file (config null: default values). */
    private void exportA51(A51Export.Config cfg) {
        int listBand = A51Export.guessBand(sessionList);
        if (cfg == null) {
            cfg = A51Export.Config.defaults(listBand);
            cfg.source = "default values (no PRM80 connected)";
        } else if (cfg.band != listBand) {
            if (!this.confirm("The connected PRM80 is a " + cfg.band + " MHz model, but the channels are for "
                    + listBand + " MHz.\nExport anyway?"))
                return;
        }
        String step = new java.math.BigDecimal(cfg.pllStep).movePointLeft(3).stripTrailingZeros().toPlainString();
        String shift = new java.math.BigDecimal(cfg.txShift).movePointLeft(6).stripTrailingZeros().toPlainString();
        String summary = "Export " + sessionList.countChannel() + " channels as firmware configuration\n"
                + "(src/" + A51Export.firmwareFileName(cfg.band) + " of the firmware).\n\n"
                + "Firmware format:  V" + cfg.firmwareVersion
                + (cfg.firmwareVersion >= 5 ? " (with shift per channel)" : "") + "\n"
                + "Band:  " + cfg.band + " MHz\n"
                + "PLL step:  " + step + " kHz\n"
                + "Default shift:  " + shift + " MHz\n"
                + (cfg.firmwareVersion > 3 ? "Scan speed:  " + cfg.scanSpeed + " ms per channel\n" : "")
                + "Values: " + cfg.source;
        if (!this.confirm(summary))
            return;

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Firmware configuration (*.a51)", "a51"));
        String last = this.config.getA51File();
        File dir = null;
        if (!last.isEmpty() && new File(last).getParentFile() != null)
            dir = new File(last).getParentFile();
        else if (sessionFile != null)
            dir = new File(sessionFile).getParentFile();
        if (dir != null)
            fc.setCurrentDirectory(dir);
        fc.setSelectedFile(new File(dir, A51Export.firmwareFileName(cfg.band)));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".a51"))
            file = new File(file.getParentFile(), file.getName() + ".a51");
        if (file.exists() && !this.confirm(file.getName() + " already exists.\nReplace it?"))
            return;
        try {
            A51Export.exportA51(sessionList, cfg,
                    sessionFile == null ? null : new File(sessionFile).getName(), file);
            this.config.setA51File(file.getAbsolutePath());
            sessionSource = "Exported as " + file.getName() + ": " + sessionList.countChannel() + " channels";
            this.updateTitle();
        } catch (Exception ex) {
            Logger.getLogger(ChannelManager.class.getName()).log(Level.INFO, null, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Export A51", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.close();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void closeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeMenuItemActionPerformed
        this.close();
    }//GEN-LAST:event_closeMenuItemActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        this.close();
    }//GEN-LAST:event_formWindowClosing

    /**
     * Save the list to its file, or ask for a file name.
     * @return true if saved
     */
    private boolean save(boolean askFileName) {
        this.stopEditing();
        String file = sessionFile;
        if (askFileName || file == null) {
            JFileChooser fc = this.fileChooser();
            if (file != null)
                fc.setSelectedFile(new File(file));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
                return false;
            file = fc.getSelectedFile().getAbsolutePath();
            if (!file.toLowerCase().endsWith(".csv"))
                file = file + ".csv";
            if (new File(file).exists() && !file.equals(sessionFile)
                    && !this.confirm(new File(file).getName() + " already exists.\nReplace it?"))
                return false;
        }
        try {
            sessionList.exportCSV(file);
            sessionFile = file;
            sessionSource = "Saved: " + sessionList.countChannel() + " channels";
            sessionModified = false;
            this.config.setChannelFile(file);
            this.updateTitle();
            return true;
        } catch (Exception ex) {
            Logger.getLogger(ChannelManager.class.getName()).log(Level.INFO, null, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Close the dialog, ask before unsaved changes get lost. */
    private void close() {
        if (this.busy)
            return;
        this.stopEditing();
        // the list stays in memory, but it is good to know that it is not in a file
        if (sessionModified && sessionList.countChannel() > 0) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "The channel list is not saved in a file.\nSave it now?",
                    this.getTitle(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION)
                return;
            if (answer == JOptionPane.YES_OPTION && !this.save(false))
                return;
        }
        this.dispose();
    }

    private JFileChooser fileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Channel lists (*.csv)", "csv"));
        String last = sessionFile != null ? sessionFile : this.config.getChannelFile();
        if (!last.isEmpty() && new File(last).getParentFile() != null)
            fc.setCurrentDirectory(new File(last).getParentFile());
        return fc;
    }

    /** Show a new list (read from the PRM80 or a file): no "Undo" before it. */
    private void showList(ChannelList list) {
        this.undoStack.clear();
        this.redoStack.clear();
        this.showListKeepUndo(list);
    }

    private void showListKeepUndo(ChannelList list) {
        boolean wasBusy = this.busy;
        this.busy = true;               // no "modified" for the new list
        this.model.setChannels(list);
        this.busy = wasBusy;
        this.updateTitle();
        this.updateButtons();
    }

    // ---- Insert, Delete, Move, Undo ------------------------------------------

    /** lists before the last changes, newest first */
    private final java.util.ArrayDeque<ChannelList> undoStack = new java.util.ArrayDeque<>();
    /** lists undone, newest first ("Redo") */
    private final java.util.ArrayDeque<ChannelList> redoStack = new java.util.ArrayDeque<>();
    private static final int UNDO_STEPS = 50;

    /** Remember the list before a change. */
    private void remember() {
        this.undoStack.push(sessionList.copy());
        while (this.undoStack.size() > UNDO_STEPS)
            this.undoStack.removeLast();
        this.redoStack.clear();             // a new change: what was undone is gone
        this.undoButton.setEnabled(!this.busy);
        this.redoButton.setEnabled(false);
    }

    /** The list was changed by Insert, Delete or Move: show it, mark it modified. */
    private void listChanged(String message, int selectFrom, int selectTo) {
        this.showListKeepUndo(sessionList);
        sessionModified = true;
        sessionSyncedWithPrm = false;       // the PRM80 still has the old order
        sessionSource = message;
        this.updateTitle();
        if (selectFrom >= 0 && selectFrom < this.table.getRowCount()) {
            this.table.setRowSelectionInterval(selectFrom, Math.min(selectTo, this.table.getRowCount() - 1));
            this.table.scrollRectToVisible(this.table.getCellRect(selectFrom, 0, true));
        }
    }

    /** @return selected rows that are channels (not the "*" row), ascending */
    private int[] selectedChannels() {
        return java.util.Arrays.stream(this.table.getSelectedRows())
                .map(this.table::convertRowIndexToModel)
                .filter(r -> r < sessionList.countChannel())
                .sorted().toArray();
    }

    private void insertButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_insertButtonActionPerformed
        this.stopEditing();
        if (sessionList.countChannel() >= ChannelList.MAX_CHANNELS) {
            JOptionPane.showMessageDialog(this, "The PRM80 has only " + ChannelList.MAX_CHANNELS + " channels.",
                    "Insert", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int[] rows = this.selectedChannels();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select the channel before which a channel shall be inserted.\n"
                    + "A new channel at the end is entered in the row \"*\".", "Insert", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int at = rows[0];
        this.remember();
        // a copy of the selected channel: it has a valid frequency, change it as needed
        f4fez.prm80x0.Controler.Channel copy = sessionList.getChannel(at).copy();
        sessionList.insertChannel(at, copy);
        this.listChanged("Channel " + at + " inserted (copy of the next one) - change it as needed", at, at);
    }//GEN-LAST:event_insertButtonActionPerformed

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        this.stopEditing();
        int[] rows = this.selectedChannels();
        if (rows.length == 0)
            return;
        if (rows.length >= sessionList.countChannel()) {
            JOptionPane.showMessageDialog(this, "At least one channel must remain.", "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.remember();
        for (int i = rows.length - 1; i >= 0; i--)
            sessionList.removeChannel(rows[i]);
        String which = rows.length == 1 ? "Channel " + rows[0] : rows.length + " channels";
        this.listChanged(which + " deleted, the following channels moved forward (Undo possible)", rows[0], rows[0]);
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void moveUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpButtonActionPerformed
        this.move(-1);
    }//GEN-LAST:event_moveUpButtonActionPerformed

    private void moveDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownButtonActionPerformed
        this.move(+1);
    }//GEN-LAST:event_moveDownButtonActionPerformed

    /** Move the selected channels (one block from the first to the last selected) by one. */
    private void move(int direction) {
        this.stopEditing();
        int[] rows = this.selectedChannels();
        if (rows.length == 0)
            return;
        int first = rows[0];
        int last = rows[rows.length - 1];
        if (direction < 0 && first == 0 || direction > 0 && last >= sessionList.countChannel() - 1)
            return;
        this.remember();
        if (direction < 0)
            sessionList.moveChannel(first - 1, last);       // channel above goes below the block
        else
            sessionList.moveChannel(last + 1, first);       // channel below goes above the block
        this.listChanged((last > first ? "Channels " + first + "-" + last : "Channel " + first)
                + " moved " + (direction < 0 ? "up" : "down"), first + direction, last + direction);
    }

    private void undoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoButtonActionPerformed
        this.stopEditing();
        if (this.undoStack.isEmpty())
            return;
        this.redoStack.push(sessionList);
        sessionList = this.undoStack.pop();
        this.showListKeepUndo(sessionList);
        sessionModified = true;
        sessionSyncedWithPrm = false;
        sessionSource = "Last change undone" + (this.undoStack.isEmpty() ? "" : " (" + this.undoStack.size() + " more)");
        this.updateTitle();
    }//GEN-LAST:event_undoButtonActionPerformed

    private void redoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redoButtonActionPerformed
        this.stopEditing();
        if (this.redoStack.isEmpty())
            return;
        this.undoStack.push(sessionList);
        sessionList = this.redoStack.pop();
        this.showListKeepUndo(sessionList);
        sessionModified = true;
        sessionSyncedWithPrm = false;
        sessionSource = "Change done again" + (this.redoStack.isEmpty() ? "" : " (" + this.redoStack.size() + " more)");
        this.updateTitle();
    }//GEN-LAST:event_redoButtonActionPerformed

    private static final String DEFAULT_SHIFT_TIP =
            "<html>0 with \"Shift Enabled\" = default shift of the PRM80<br>"
            + "(Configuration, Default shift). Another value is the own shift of the channel.</html>";

    /** Tooltips: "Shift Freq." 0 means the default shift of the configuration. */
    private void installShiftTooltips() {
        final javax.swing.table.JTableHeader header = this.table.getTableHeader();
        header.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int col = header.columnAtPoint(e.getPoint());
                int modelCol = col < 0 ? -1 : table.convertColumnIndexToModel(col);
                header.setToolTipText(modelCol == ChannelModel.COL_SHIFTFREQ ? DEFAULT_SHIFT_TIP : null);
            }
        });
        this.table.getColumnModel().getColumn(ChannelModel.COL_SHIFTFREQ).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object value,
                    boolean selected, boolean focus, int row, int col) {
                java.awt.Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                boolean isDefault = false;
                int r = t.convertRowIndexToModel(row);
                if (r < sessionList.countChannel()) {
                    f4fez.prm80x0.Controler.Channel ch = sessionList.getChannel(r);
                    isDefault = ch.isShiftEnabled() && hz(ch.getShiftFreq()) == 0;
                }
                this.setToolTipText(isDefault
                        ? "<html>Default shift of the PRM80 (Configuration, Default shift)</html>"
                        : null);
                return c;
            }
        });
    }

    // ---- Verify ----------------------------------------------------------------

    private void verifyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verifyButtonActionPerformed
        this.stopEditing();
        JFileChooser fc = this.fileChooser();
        fc.setDialogTitle("Verify: compare the list with a channel file");
        if (sessionFile != null)
            fc.setSelectedFile(new File(sessionFile));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File file = fc.getSelectedFile();
        ChannelList other = new ChannelList();
        try {
            other.importCSV(file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Verify", JOptionPane.ERROR_MESSAGE);
            return;
        }
        java.util.List<String> settings = new java.util.ArrayList<>();
        java.util.List<String> comments = new java.util.ArrayList<>();
        compare(sessionList, other, settings, comments);
        String title = "Verify with " + file.getName();
        if (settings.isEmpty() && comments.isEmpty()) {
            sessionSource = "Verify: identical with " + file.getName();
            this.updateTitle();
            JOptionPane.showMessageDialog(this, "The list is identical with " + file.getName() + "\n("
                    + sessionList.countChannel() + " channels).", title, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder text = new StringBuilder();
        text.append("List shown: ").append(sessionList.countChannel()).append(" channels, file: ")
            .append(other.countChannel()).append(" channels\n\n");
        if (!settings.isEmpty()) {
            text.append(settings.size()).append(" difference(s) in the channel settings:\n");
            for (String line : settings)
                text.append("  ").append(line).append('\n');
            text.append('\n');
        }
        if (!comments.isEmpty()) {
            text.append(comments.size()).append(" difference(s) in the comments")
                .append(" (the PRM80 does not store comments):\n");
            for (String line : comments)
                text.append("  ").append(line).append('\n');
        }
        sessionSource = "Verify: " + settings.size() + " difference(s) in settings, "
                + comments.size() + " in comments to " + file.getName();
        this.updateTitle();
        javax.swing.JTextArea area = new javax.swing.JTextArea(text.toString(), 16, 60);
        area.setEditable(false);
        area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, area.getFont().getSize()));
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(area), title,
                settings.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }//GEN-LAST:event_verifyButtonActionPerformed

    /** Compare two lists channel by channel, the differences as text lines. */
    private static void compare(ChannelList list, ChannelList file, java.util.List<String> settings,
            java.util.List<String> comments) {
        int n = Math.max(list.countChannel(), file.countChannel());
        for (int i = 0; i < n; i++) {
            if (i >= file.countChannel()) {
                settings.add(String.format("Channel %2d: only in the list (%s)", i, list.getChannel(i).getFrequency()));
                continue;
            }
            if (i >= list.countChannel()) {
                settings.add(String.format("Channel %2d: only in the file (%s)", i, file.getChannel(i).getFrequency()));
                continue;
            }
            f4fez.prm80x0.Controler.Channel a = list.getChannel(i);
            f4fez.prm80x0.Controler.Channel b = file.getChannel(i);
            if (hz(a.getFrequency()) != hz(b.getFrequency()))
                settings.add(diff(i, "Frequency", a.getFrequency(), b.getFrequency()));
            if (hz(a.getShiftFreq()) != hz(b.getShiftFreq()))
                settings.add(diff(i, "Shift Freq.", a.getShiftFreq(), b.getShiftFreq()));
            if (!same(a.getScanLock(), b.getScanLock()))
                settings.add(diff(i, "Scan Lock", a.getScanLock(), b.getScanLock()));
            // the sign only counts with shift enabled
            if (a.isShiftEnabled() && b.isShiftEnabled() && !same(a.getShift(), b.getShift()))
                settings.add(diff(i, "Shift +/-", a.getShift(), b.getShift()));
            if (!same(a.getShiftReverse(), b.getShiftReverse()))
                settings.add(diff(i, "Shift Reverse", a.getShiftReverse(), b.getShiftReverse()));
            if (a.isShiftEnabled() != b.isShiftEnabled())
                settings.add(diff(i, "Shift Enabled", a.getShiftEnabled(), b.getShiftEnabled()));
            if (!same(a.getComments(), b.getComments()))
                comments.add(diff(i, "Comment", a.getComments(), b.getComments()));
        }
    }

    private static String diff(int chan, String field, String list, String file) {
        return String.format("Channel %2d %-13s list: %-14s file: %s", chan, field,
                show(list), show(file));
    }

    private static String show(String value) {
        return value == null || value.trim().isEmpty() ? "(empty)" : value.trim();
    }

    private static boolean same(String a, String b) {
        return (a == null ? "" : a.trim()).equals(b == null ? "" : b.trim());
    }

    /** "145.600000" -> Hz, empty -> 0 */
    private static long hz(String mhz) {
        if (mhz == null || mhz.trim().isEmpty())
            return 0;
        try {
            return new java.math.BigDecimal(mhz.trim().replace(',', '.')).movePointRight(6).longValue();
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /** Del, Insert, Alt+Up, Alt+Down in the table, Ctrl+Z in the whole dialog. */
    private void bindKeys() {
        javax.swing.InputMap im = this.table.getInputMap(javax.swing.JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap am = this.table.getActionMap();
        bind(im, am, "DELETE", "pm80delete", this.deleteButton);
        bind(im, am, "INSERT", "pm80insert", this.insertButton);
        bind(im, am, "alt UP", "pm80up", this.moveUpButton);
        bind(im, am, "alt DOWN", "pm80down", this.moveDownButton);
        javax.swing.JRootPane root = this.getRootPane();
        bind(root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW), root.getActionMap(),
                "ctrl Z", "pm80undo", this.undoButton);
        bind(root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW), root.getActionMap(),
                "ctrl Y", "pm80redo", this.redoButton);
        bind(root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW), root.getActionMap(),
                "ctrl shift Z", "pm80redo", this.redoButton);
    }

    private static void bind(javax.swing.InputMap im, javax.swing.ActionMap am, String key, String name,
            final javax.swing.JButton button) {
        im.put(javax.swing.KeyStroke.getKeyStroke(key), name);
        am.put(name, new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (button.isEnabled())
                    button.doClick(0);
            }
        });
    }

    /** true while a value is copied to the selected rows */
    private boolean applyingToSelection;

    /**
     * A value was changed in a row that is selected together with other
     * rows: ask whether it shall be set in all selected rows (not for the
     * frequency, every channel has its own).
     */
    private void applyToSelection(int row, int col) {
        if (col == ChannelModel.COL_CHANNEL || col == ChannelModel.COL_FREQUENCY)
            return;
        if (row >= sessionList.countChannel() || !this.table.isRowSelected(row))
            return;
        java.util.List<Integer> others = new java.util.ArrayList<>();
        for (int viewRow : this.table.getSelectedRows()) {
            int r = this.table.convertRowIndexToModel(viewRow);
            if (r != row && r < sessionList.countChannel())
                others.add(r);
        }
        if (others.isEmpty())
            return;
        Object value = this.model.getValueAt(row, col);
        String text = value == null || value.toString().isEmpty() ? "(empty)" : "\"" + value + "\"";
        int answer = JOptionPane.showConfirmDialog(this,
                "Set \"" + this.model.getColumnName(col) + "\" to " + text + "\nfor all "
                + (others.size() + 1) + " selected channels?",
                this.getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION)
            return;
        this.remember();                    // one "Undo" step for all rows
        this.applyingToSelection = true;
        try {
            for (int r : others)
                this.model.setValueAt(value, r, col);
        } finally {
            this.applyingToSelection = false;
        }
        sessionSource = this.model.getColumnName(col) + " set for " + (others.size() + 1) + " channels";
        this.updateTitle();
    }

    private void stopEditing() {
        if (this.table.isEditing())
            this.table.getCellEditor().stopCellEditing();
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, this.getTitle(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    /** While the PRM80 is read or written: no editing, wait cursor. */
    private void setBusy(boolean busy, String message) {
        this.busy = busy;
        if (busy)
            this.stopEditing();
        this.table.setEnabled(!busy);
        this.setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
        if (message != null)
            sessionSource = message;
        this.updateButtons();
        this.updateTitle();
    }

    private void updateButtons() {
        boolean connected = this.prm != null && this.prm.isConnected();
        this.readPrmButton.setEnabled(!busy && connected);
        this.writePrmButton.setEnabled(!busy && connected);
        this.readFileButton.setEnabled(!busy);
        this.saveButton.setEnabled(!busy);
        this.saveAsButton.setEnabled(!busy);
        this.exportA51Button.setEnabled(!busy);
        this.exportA51Button.setVisible(this.config.isExpertMode());
        this.closeButton.setEnabled(!busy);
        this.closeMenuItem.setEnabled(!busy);
        this.insertButton.setEnabled(!busy);
        this.deleteButton.setEnabled(!busy);
        this.moveUpButton.setEnabled(!busy);
        this.moveDownButton.setEnabled(!busy);
        this.undoButton.setEnabled(!busy && !this.undoStack.isEmpty());
        this.redoButton.setEnabled(!busy && !this.redoStack.isEmpty());
        this.verifyButton.setEnabled(!busy);
    }

    private void updateTitle() {
        String name = sessionFile == null ? "not saved" : new File(sessionFile).getName();
        this.setTitle("Channel manager - " + name + (sessionModified && sessionFile != null ? " *" : ""));
        String text = sessionSource;
        if (sessionModified && !this.busy)
            text += sessionFile == null ? "  (not saved in a file)" : "  (changed, not saved)";
        this.statusLabel.setText(text);
    }

    private static JComboBox<String> combo(String... items) {
        JComboBox<String> combo = new JComboBox<>();
        for (String item : items)
            combo.addItem(item);
        return combo;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JPanel editButtonsPanel;
    private javax.swing.JButton insertButton;
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton moveUpButton;
    private javax.swing.JButton moveDownButton;
    private javax.swing.JButton undoButton;
    private javax.swing.JButton redoButton;
    private javax.swing.JButton verifyButton;
    private javax.swing.JButton exportA51Button;
    private javax.swing.JMenuItem closeMenuItem;
    private javax.swing.JPanel fileButtonsPanel;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel prmButtonsPanel;
    private javax.swing.JButton readFileButton;
    private javax.swing.JButton readPrmButton;
    private javax.swing.JButton saveAsButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTable table;
    private javax.swing.JScrollPane tableScroll;
    private javax.swing.JButton writePrmButton;
    // End of variables declaration//GEN-END:variables
}
