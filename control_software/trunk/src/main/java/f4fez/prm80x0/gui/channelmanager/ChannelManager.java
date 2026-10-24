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
        this.table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        this.model.addTableModelListener(e -> {
            if (!busy) {
                sessionModified = true;
                // a changed channel is no longer the one in the PRM80 (comments are not in the PRM80)
                if (e.getColumn() != ChannelModel.COL_COMMENTS)
                    sessionSyncedWithPrm = false;
                updateTitle();
            }
        });

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
        exportA51Button = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
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
        readPrmButton.setName("readPrmButton"); // NOI18N
        readPrmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readPrmButtonActionPerformed(evt);
            }
        });
        prmButtonsPanel.add(readPrmButton);

        writePrmButton.setText("Write to PRM80"); // NOI18N
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
            sessionSource = "Read from PRM80: " + list.countChannel() + " channels";
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
                + "The channels in the PRM80 will be replaced."))
            return;
        this.setBusy(true, "Writing " + sessionList.countChannel() + " channels to PRM80 ...");
        this.prm.writeChannels(sessionList, () -> {
            sessionSyncedWithPrm = true;
            this.setBusy(false, "Written to PRM80: " + sessionList.countChannel() + " channels");
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

    private void showList(ChannelList list) {
        boolean wasBusy = this.busy;
        this.busy = true;               // no "modified" for the new list
        this.model.setChannels(list);
        this.busy = wasBusy;
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
