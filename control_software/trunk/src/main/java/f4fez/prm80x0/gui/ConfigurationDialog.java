/*
 * ConfigurationDialog.java
 *
 * Created on 17 février 2009, 21:28
 */

package f4fez.prm80x0.gui;

import java.awt.Cursor;
import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

/**
 * Configuration of the PRM80 (without the channel list), as the channel
 * manager: "Read from PRM80" / "Write to PRM80" use the RAM of the PRM80,
 * "Read" / "Save" / "Save As" a configuration file (*.cfg).
 * Nothing is written to the EEPROM here (Tools &gt; Memory).
 *
 * @author  f4fez
 */
public class ConfigurationDialog extends javax.swing.JDialog {

    /** Access to the PRM80, done by the main window in the background. */
    public interface PrmAccess {
        boolean isConnected();

        /** Read the configuration from the RAM of the PRM80 (not the values remembered before). */
        void readConfiguration(Consumer<PrmConfiguration> done, Consumer<String> error);

        /** Write the changed values to the RAM; done gets the values read back. */
        void writeConfiguration(PrmConfiguration configuration, Consumer<PrmConfiguration> done, Consumer<String> error);
    }

    static final String READ_PRM_TIP = "<html>Read the configuration from the <b>RAM</b> of the PRM80 (the values in use now).<br>"
            + "The EEPROM (permanent copy) is not read.</html>";
    static final String WRITE_PRM_TIP = "<html>Write the values shown to the <b>RAM</b> of the PRM80.<br>"
            + "The RAM keeps them for some weeks without power.<br>"
            + "To keep them permanently: main window, Tools &gt; Memory &gt; Copy RAM -&gt; EEPROM<br>"
            + "(this copies <b>all</b> settings and the channel list).</html>";
    private static final String EEPROM_HINT = "To keep it permanently: Tools > Memory > Copy RAM -> EEPROM";
    private static final String EEPROM_HINT_HTML = "To keep it permanently: Tools &gt; Memory &gt; Copy RAM -&gt; EEPROM";

    /** configuration file read or saved last (while the program runs) */
    private static String sessionFile;

    private final PrmAccess prm;
    private final String device;
    private final int band;
    private final boolean txShiftSupported;
    private final boolean scanSpeedSupported;

    /** values in the PRM80 as known (read or written last) */
    private PrmConfiguration prmValues;
    /** values when they were read, saved or written last: changed since? */
    private PrmConfiguration lastValues;
    private String source;
    private boolean busy;
    private boolean showing;    // no "changed" while values are put into the fields

    /**
     * @param prm access to the PRM80
     * @param device PRM80 type, firmware and band, e.g. "PRM8070 V5.1 144 MHz"
     * @param band band in MHz (144, 430 ...)
     * @param prmValues values read from the PRM80
     */
    public ConfigurationDialog(java.awt.Frame parent, PrmAccess prm, String device, int band,
            boolean txShiftSupported, boolean scanSpeedSupported, PrmConfiguration prmValues) {
        super(parent, true);
        initComponents();
        this.prm = prm;
        this.device = device;
        this.band = band;
        this.txShiftSupported = txShiftSupported;
        this.scanSpeedSupported = scanSpeedSupported;
        this.versionTextField.setText(device);
        if (!txShiftSupported) {
            this.shiftComboBox.setEnabled(false);
            this.shiftComboBox.setToolTipText("Not supported by the PRM80 firmware");
        }
        if (!scanSpeedSupported) {
            this.scanSpeedSpinner.setEnabled(false);
            this.scanSpeedSpinner.setToolTipText("Not supported by the PRM80 firmware");
        }
        this.installChangeListeners();
        this.prmValues = prmValues;
        this.show(prmValues, "Read from the RAM of the PRM80");
        Zoom.install(this);
    }

    // ---- values ---------------------------------------------------------------

    private void show(PrmConfiguration c, String source) {
        this.showing = true;
        if (c.pllStep > 0)
            this.pllStepComboBox.setSelectedItem(PrmConfiguration.kHz(c.pllStep));
        if (this.txShiftSupported && c.txShift >= 0)
            this.shiftComboBox.setSelectedItem(PrmConfiguration.kHz(c.txShift));
        if (this.scanSpeedSupported && c.scanSpeed >= 0)
            this.scanSpeedSpinner.setValue(c.scanSpeed);
        if (c.squelch >= 0)
            this.squelchSpinner.setValue(c.squelch);
        this.showing = false;
        this.lastValues = this.shownValues();
        this.source = source;
        this.updateStatus();
    }

    /** @return the values shown, null if one of them is not valid */
    private PrmConfiguration shownValues() {
        try {
            int step = PrmConfiguration.hz(editorText(this.pllStepComboBox));
            if (step <= 0)
                return null;
            int shift = -1;
            if (this.txShiftSupported) {
                shift = PrmConfiguration.hz(editorText(this.shiftComboBox));
                if (shift < 0)
                    return null;
            }
            int scan = this.scanSpeedSupported ? ((Number) this.scanSpeedSpinner.getValue()).intValue() : -1;
            int squelch = ((Number) this.squelchSpinner.getValue()).intValue();
            return new PrmConfiguration(this.band, step, shift, scan, squelch);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String html(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String editorText(javax.swing.JComboBox<?> combo) {
        Object item = combo.getEditor().getItem();
        return item == null ? "" : item.toString();
    }

    private void installChangeListeners() {
        DocumentListener d = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        };
        for (javax.swing.JComboBox<?> combo : new javax.swing.JComboBox<?>[] {this.pllStepComboBox, this.shiftComboBox}) {
            java.awt.Component editor = combo.getEditor().getEditorComponent();
            if (editor instanceof JTextComponent)
                ((JTextComponent) editor).getDocument().addDocumentListener(d);
            combo.addActionListener(e -> changed());
        }
        this.scanSpeedSpinner.addChangeListener(e -> changed());
        this.squelchSpinner.addChangeListener(e -> changed());
    }

    private void changed() {
        if (!this.showing)
            javax.swing.SwingUtilities.invokeLater(this::updateStatus);
    }

    private void updateStatus() {
        String name = sessionFile == null ? "no file" : new File(sessionFile).getName();
        this.setTitle("PRM80 configuration - " + name);
        PrmConfiguration shown = this.shownValues();
        String text = this.source;
        if (shown == null)
            text += "<br><b>A value is not valid</b> (PLL step and shift in kHz, e.g. 12.5)";
        else if (!shown.sameValues(this.prmValues))
            text += "<br><b>Not written to the PRM80</b> (\"Write to PRM80\")";
        else if (this.lastValues != null && !shown.sameValues(this.lastValues))
            text += "<br>Changed";
        this.statusLabel.setText("<html>" + text + "</html>");
        this.updateButtons();
    }

    private void updateButtons() {
        boolean connected = this.prm != null && this.prm.isConnected();
        this.readPrmButton.setEnabled(!this.busy && connected);
        this.writePrmButton.setEnabled(!this.busy && connected);
        this.readFileButton.setEnabled(!this.busy);
        this.saveButton.setEnabled(!this.busy);
        this.saveAsButton.setEnabled(!this.busy);
        this.closeButton.setEnabled(!this.busy);
    }

    private void setBusy(boolean busy, String message) {
        this.busy = busy;
        this.setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
        if (message != null)
            this.source = message;
        this.updateStatus();
    }

    /** @return true if the values shown can be replaced */
    private boolean mayReplaceShown(String what) {
        PrmConfiguration shown = this.shownValues();
        if (shown != null && (shown.sameValues(this.lastValues) || shown.sameValues(this.prmValues)))
            return true;
        return this.confirm("The values shown are changed and not saved.\nReplace them with " + what + "?");
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "PRM80 configuration",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private PrmConfiguration validValues() {
        PrmConfiguration c = this.shownValues();
        if (c == null)
            JOptionPane.showMessageDialog(this, "Please enter the PLL step and the shift in kHz, e.g. 12.5",
                    "PRM80 configuration", JOptionPane.ERROR_MESSAGE);
        return c;
    }

    // ---- files ----------------------------------------------------------------

    /** Default file: the last configuration file, else the name of the channel list with .cfg */
    private File defaultFile() {
        if (sessionFile != null)
            return new File(sessionFile);
        String channels = f4fez.prm80x0.gui.channelmanager.ChannelManager.currentFile();
        if (channels == null || channels.isEmpty())
            return null;
        File f = new File(channels);
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0)
            name = name.substring(0, dot);
        return new File(f.getParentFile(), name + "." + PrmConfiguration.EXTENSION);
    }

    private JFileChooser fileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("PRM80 configuration (*." + PrmConfiguration.EXTENSION + ")",
                PrmConfiguration.EXTENSION));
        File def = this.defaultFile();
        if (def != null) {
            if (def.getParentFile() != null)
                fc.setCurrentDirectory(def.getParentFile());
            fc.setSelectedFile(def);
        }
        return fc;
    }

    private boolean save(boolean askFileName) {
        PrmConfiguration c = this.validValues();
        if (c == null)
            return false;
        File file = sessionFile == null ? null : new File(sessionFile);
        if (askFileName || file == null) {
            JFileChooser fc = this.fileChooser();
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
                return false;
            file = fc.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith("." + PrmConfiguration.EXTENSION))
                file = new File(file.getParentFile(), file.getName() + "." + PrmConfiguration.EXTENSION);
            if (file.exists() && !file.getAbsolutePath().equals(sessionFile)
                    && !this.confirm(file.getName() + " already exists.\nReplace it?"))
                return false;
        }
        try {
            c.save(file, c.sameValues(this.prmValues) ? this.device : this.device + " (values changed before saving)");
            sessionFile = file.getAbsolutePath();
            this.lastValues = c;
            this.source = "Saved in " + html(file.getName())
                    + (c.sameValues(this.prmValues) ? " (same as in the PRM80)" : "");
            this.updateStatus();
            return true;
        } catch (Exception ex) {
            Logger.getLogger(ConfigurationDialog.class.getName()).log(Level.INFO, null, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        versionTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        pllStepComboBox = new javax.swing.JComboBox();
        shiftComboBox = new javax.swing.JComboBox();
        scanSpeedSpinner = new javax.swing.JSpinner();
        squelchSpinner = new javax.swing.JSpinner();
        statusLabel = new javax.swing.JLabel();
        readPrmButton = new javax.swing.JButton();
        writePrmButton = new javax.swing.JButton();
        readFileButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        saveAsButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PRM80 configuration"); // NOI18N
        setName("Configuration"); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(470, 375));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel4.setText("Version:"); // NOI18N
        jLabel4.setName("versionLabel"); // NOI18N
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 25, -1, -1));

        versionTextField.setText("PRM8060 144 V3.0"); // NOI18N
        versionTextField.setEnabled(false);
        versionTextField.setName("versionTextField"); // NOI18N
        jPanel1.add(versionTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 20, 290, -1));

        jLabel1.setText("PLL step (kHz):"); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 65, -1, -1));

        jLabel2.setText("Default shift (kHz):");
        jLabel2.setToolTipText("<html>Default shift of the PRM80.<br>It is used for every channel with \"Shift Enabled\" and<br>\"Shift Freq.\" 0 (channel without its own shift).<br>A change here applies to all these channels at once.</html>"); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 105, -1, -1));

        jLabel3.setText("Scan speed (ms):"); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 145, -1, -1));

        jLabel5.setText("Squelch:"); // NOI18N
        jLabel5.setToolTipText("Squelch level 0 to 15, the same value as in the main window"); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 185, -1, -1));

        pllStepComboBox.setEditable(true);
        pllStepComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "6.25", "12.5", "25" }));
        pllStepComboBox.setSelectedIndex(1);
        pllStepComboBox.setToolTipText("Frequency step of the PLL in kHz"); // NOI18N
        pllStepComboBox.setName("pllStepComboBox"); // NOI18N
        jPanel1.add(pllStepComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 60, 100, -1));

        shiftComboBox.setEditable(true);
        shiftComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "600", "1600", "7600" }));
        shiftComboBox.setToolTipText("<html>Default shift of the PRM80.<br>It is used for every channel with \"Shift Enabled\" and<br>\"Shift Freq.\" 0 (channel without its own shift).<br>A change here applies to all these channels at once.</html>"); // NOI18N
        shiftComboBox.setName("shiftComboBox"); // NOI18N
        jPanel1.add(shiftComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 100, 100, -1));

        scanSpeedSpinner.setModel(new javax.swing.SpinnerNumberModel(400, 50, 12750, 50));
        scanSpeedSpinner.setToolTipText("Time per channel while scanning (steps of 50 ms)"); // NOI18N
        scanSpeedSpinner.setName("scanSpeedSpinner"); // NOI18N
        jPanel1.add(scanSpeedSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 140, 100, -1));

        squelchSpinner.setModel(new javax.swing.SpinnerNumberModel(5, 0, 15, 1));
        squelchSpinner.setToolTipText("Squelch level 0 to 15, the same value as in the main window"); // NOI18N
        squelchSpinner.setName("squelchSpinner"); // NOI18N
        jPanel1.add(squelchSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 180, 100, -1));

        statusLabel.setText("Read from PRM80"); // NOI18N
        statusLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        statusLabel.setName("statusLabel"); // NOI18N
        jPanel1.add(statusLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 220, 430, 60));

        readPrmButton.setText("Read from PRM80"); // NOI18N
        readPrmButton.setToolTipText(READ_PRM_TIP);
        readPrmButton.setName("readPrmButton"); // NOI18N
        readPrmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readPrmButtonActionPerformed(evt);
            }
        });
        jPanel1.add(readPrmButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 290, 170, -1));

        writePrmButton.setText("Write to PRM80"); // NOI18N
        writePrmButton.setToolTipText(WRITE_PRM_TIP);
        writePrmButton.setName("writePrmButton"); // NOI18N
        writePrmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                writePrmButtonActionPerformed(evt);
            }
        });
        jPanel1.add(writePrmButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 290, 170, -1));

        readFileButton.setText("Read"); // NOI18N
        readFileButton.setToolTipText("<html>Read the configuration from a file (*.cfg).<br>It goes to the PRM80 only with \"Write to PRM80\".</html>"); // NOI18N
        readFileButton.setName("readFileButton"); // NOI18N
        readFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readFileButtonActionPerformed(evt);
            }
        });
        jPanel1.add(readFileButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 330, 90, -1));

        saveButton.setText("Save"); // NOI18N
        saveButton.setToolTipText("Save the values shown in the configuration file"); // NOI18N
        saveButton.setName("saveButton"); // NOI18N
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        jPanel1.add(saveButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(115, 330, 90, -1));

        saveAsButton.setText("Save As"); // NOI18N
        saveAsButton.setToolTipText("<html>Save the values shown in a new file (*.cfg).<br>Proposed: the name of the channel list of the channel manager.</html>"); // NOI18N
        saveAsButton.setName("saveAsButton"); // NOI18N
        saveAsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsButtonActionPerformed(evt);
            }
        });
        jPanel1.add(saveAsButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 330, 90, -1));

        closeButton.setText("Close"); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });
        jPanel1.add(closeButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 330, 100, -1));

        getContentPane().add(jPanel1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void readPrmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readPrmButtonActionPerformed
        if (!this.mayReplaceShown("the values of the PRM80"))
            return;
        this.setBusy(true, "Reading the configuration from the PRM80 ...");
        this.prm.readConfiguration(c -> {
            this.prmValues = c;
            this.busy = false;
            this.setCursor(null);
            this.show(c, "Read from the RAM of the PRM80 (not from the EEPROM)");
        }, error -> this.setBusy(false, "Reading from the PRM80 failed: " + html(error)));
    }//GEN-LAST:event_readPrmButtonActionPerformed

    private void writePrmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_writePrmButtonActionPerformed
        PrmConfiguration c = this.validValues();
        if (c == null)
            return;
        if (!this.confirm("Write the configuration to the PRM80?\n\n"
                + "It is written to the RAM of the PRM80, which keeps it for some weeks without power.\n"
                + EEPROM_HINT + "\n(this copies all settings and the channel list)."))
            return;
        this.setBusy(true, "Writing the configuration to the PRM80 ...");
        this.prm.writeConfiguration(c, written -> {
            this.prmValues = written;
            this.busy = false;
            this.setCursor(null);
            this.show(written, "Written to the RAM of the PRM80.<br>" + EEPROM_HINT_HTML);
        }, error -> this.setBusy(false, "Writing to the PRM80 failed: " + html(error)));
    }//GEN-LAST:event_writePrmButtonActionPerformed

    private void readFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readFileButtonActionPerformed
        if (!this.mayReplaceShown("the values of a file"))
            return;
        JFileChooser fc = this.fileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File file = fc.getSelectedFile();
        try {
            PrmConfiguration c = PrmConfiguration.load(file);
            if (c.band > 0 && this.band > 0 && c.band != this.band
                    && !this.confirm(file.getName() + " was saved for the " + c.band + " MHz band,\n"
                            + "the PRM80 is for " + this.band + " MHz. Use it anyway?"))
                return;
            // values not in the file (or not supported): keep those shown
            PrmConfiguration shown = this.shownValues();
            PrmConfiguration base = shown != null ? shown : this.prmValues;
            PrmConfiguration merged = new PrmConfiguration(this.band,
                    c.pllStep > 0 ? c.pllStep : base.pllStep,
                    c.txShift >= 0 ? c.txShift : base.txShift,
                    c.scanSpeed >= 0 ? c.scanSpeed : base.scanSpeed,
                    c.squelch >= 0 ? c.squelch : base.squelch);
            sessionFile = file.getAbsolutePath();
            this.show(merged, "Read from file " + html(file.getName())
                    + (merged.sameValues(this.prmValues) ? " (same as in the PRM80)" : ""));
            // show() takes the file values as "last": not "changed" against the file
        } catch (Exception ex) {
            Logger.getLogger(ConfigurationDialog.class.getName()).log(Level.INFO, null, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_readFileButtonActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        this.save(false);
    }//GEN-LAST:event_saveButtonActionPerformed

    private void saveAsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsButtonActionPerformed
        this.save(true);
    }//GEN-LAST:event_saveAsButtonActionPerformed

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.close();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        this.close();
    }//GEN-LAST:event_formWindowClosing

    private void close() {
        if (this.busy)
            return;
        PrmConfiguration shown = this.shownValues();
        if (shown != null && !shown.sameValues(this.prmValues)
                && !this.confirm("The values shown are not written to the PRM80.\nClose anyway?"))
            return;
        this.dispose();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JComboBox pllStepComboBox;
    private javax.swing.JButton readFileButton;
    private javax.swing.JButton readPrmButton;
    private javax.swing.JButton saveAsButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JSpinner scanSpeedSpinner;
    private javax.swing.JComboBox shiftComboBox;
    private javax.swing.JSpinner squelchSpinner;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JTextField versionTextField;
    private javax.swing.JButton writePrmButton;
    // End of variables declaration//GEN-END:variables

}
