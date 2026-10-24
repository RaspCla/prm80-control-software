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
import f4fez.prm80x0.gui.channelmanager.ChannelManager;
import java.awt.Font;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import f4fez.prm80x0.Controler.CommunicationException;
import f4fez.prm80x0.Controler.DummyControler;
import f4fez.prm80x0.Controler.Controler;
import f4fez.prm80x0.Controler.PRMStateChangeListener;
import f4fez.prm80x0.Controler.SerialControler;
import f4fez.prm80x0.Controler.TcpControler;
import f4fez.prm80x0.gui.serialterminal.TerminalDialog;
import java.awt.event.MouseEvent;
import javax.swing.JFileChooser;


/**
 * The application's main frame.
 */
public class PRM80X0View extends javax.swing.JFrame {

    private VirtualDeviceFirmware vdf;
    private Option config;
    private TerminalDialog serialSpy;
            
    public PRM80X0View() {
            initComponents();

            this.config = new Option();
            
            // Disable all controls
            this.setEnableControls(false);
            
            // Expert mode ?
            this.setVisibleExpertControls(this.config.isExpertMode());

            // status bar initialization
            progressBar.setVisible(false);
            this.setStatus("Not connected");

            // Load lcd font. The stream must be closed, otherwise Windows keeps
            // the file locked and "Clean" fails while the program is running.
            try (InputStream lcdFontStream = PRM80X0View.class.getResourceAsStream("/f4fez/prm80x0/gui/resources/lcd.ttf")) {
                Font lcdFont = Font.createFont(Font.TRUETYPE_FONT, lcdFontStream);
                this.freqLabel.setFont(lcdFont.deriveFont(32f));
                this.chanLabel.setFont(lcdFont.deriveFont(32f));
                this.txFreqLabel.setFont(lcdFont.deriveFont(24f));
                this.infoLabel.setFont(lcdFont.deriveFont(24f));
            } catch (Exception ex) {
                Logger.getLogger(PRM80X0View.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this, ex.getMessage());
            } 
    }

    public void showAboutBox() {
        if (aboutBox == null) {
            aboutBox = new PRM80X0AboutBox(this);
            aboutBox.setLocationRelativeTo(this);
        }
        aboutBox.setVisible(true);
    }

    /** Leave the application, closing the connection to the PRM80 first. */
    public void quit() {
        if (this.vdf != null && this.disconnectMenuItem.isEnabled()) {
            this.disconnect();
        }
        this.dispose();
        System.exit(0);
    }

    private void setStatus(String text) {
        this.statusMessageLabel.setText(text);
    }
    
    private void setEnableControls(boolean enable) {
        this.upButton.setEnabled(enable);
        this.downButton.setEnabled(enable);
        this.volumeLabel.setEnabled(enable);
        this.volumeSlider.setEnabled(/*false*/enable);
        this.squelchLabel.setEnabled(enable);
        this.squelchSpinner.setEnabled(enable);
        this.vfoToggleButton.setEnabled(enable);
        this.memToggleButton.setEnabled(enable);
        this.resetMenuItem.setEnabled(enable);
        this.razMenuItem.setEnabled(enable);
        this.memoryMenu.setEnabled(enable);
        this.freqLabel.setVisible(enable);
        this.txFreqLabel.setVisible(enable);
        this.chanLabel.setVisible(enable && !this.vdf.isVfoMode());
        this.infoLabel.setVisible(enable);
        this.channelManagerMenuItem.setEnabled(enable);
        if (!enable) {
            this.hpLabel.setVisible(false);
            this.lpLabel.setVisible(false);
        }
        this.powerButton.setEnabled(enable);
        this.exportA51MenuItem.setEnabled(enable);
        this.configurationMenuItem.setEnabled(enable);
    }
    
    private void setVisibleExpertControls(boolean enable) {
        this.connectVirtualMenuItem.setVisible(enable & CmdLineOptions.getInstance().isDebug());
        this.resetMenuItem.setVisible(enable);
        this.razMenuItem.setVisible(enable);
        this.memoryMenu.setVisible(enable);
        this.ramExtMenuItem.setVisible(false);
        this.ramIntMenuItem.setVisible(false);
        this.eepromMenuItem.setVisible(false);
        this.serialSpyMenuItem.setVisible(enable);
        this.expertSeparator.setVisible(enable);
        this.connectTcpMenuItem.setVisible(enable);
        this.exportA51MenuItem.setVisible(enable);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        freqLabel = new javax.swing.JLabel();
        chanLabel = new javax.swing.JLabel();
        hpLabel = new javax.swing.JLabel();
        lpLabel = new javax.swing.JLabel();
        txFreqLabel = new javax.swing.JLabel();
        infoLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        vfoToggleButton = new javax.swing.JToggleButton();
        memToggleButton = new javax.swing.JToggleButton();
        volumeSlider = new javax.swing.JSlider();
        squelchLabel = new javax.swing.JLabel();
        squelchSpinner = new javax.swing.JSpinner();
        upButton = new javax.swing.JButton();
        downButton = new javax.swing.JButton();
        volumeLabel = new javax.swing.JLabel();
        powerButton = new javax.swing.JButton();
        ShiftFreqSpinner = new javax.swing.JSpinner();
        label1 = new javax.swing.JLabel();
        ShiftEnabledCheckbox = new javax.swing.JCheckBox();
        ShiftPositiveCheckbox = new javax.swing.JCheckBox();
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
        exportA51MenuItem = new javax.swing.JMenuItem();
        expertSeparator = new javax.swing.JSeparator();
        optionMenuItem = new javax.swing.JMenuItem();
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
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

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
        jPanel1.add(freqLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 160, 30));

        chanLabel.setForeground(new java.awt.Color(255, 51, 51)); // NOI18N
        chanLabel.setText("00"); // NOI18N
        chanLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        chanLabel.setName("chanLabel"); // NOI18N
        chanLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chanLabelMouseClicked(evt);
            }
        });
        jPanel1.add(chanLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 10, 50, 30));

        hpLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/f4fez/prm80x0/gui/resources/hp.png"))); // NOI18N
        hpLabel.setText(""); // NOI18N
        hpLabel.setName("hpLabel"); // NOI18N
        jPanel1.add(hpLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 10, -1, -1));

        lpLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/f4fez/prm80x0/gui/resources/lp.png"))); // NOI18N
        lpLabel.setText(""); // NOI18N
        lpLabel.setName("lpLabel"); // NOI18N
        jPanel1.add(lpLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 30, -1, -1));

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
        jPanel1.add(txFreqLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 160, 20));

        infoLabel.setForeground(new java.awt.Color(0, 255, 0)); // NOI18N
        infoLabel.setText(""); // NOI18N
        infoLabel.setName("infoLabel"); // NOI18N
        jPanel1.add(infoLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 70, 390, 20));

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
        jPanel2.add(volumeLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 50, -1));

        powerButton.setText("Power"); // NOI18N
        powerButton.setName("powerButton"); // NOI18N
        powerButton.setPreferredSize(new java.awt.Dimension(80, 23));
        powerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                powerButtonActionPerformed(evt);
            }
        });
        jPanel2.add(powerButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 60, 100, -1));

        ShiftFreqSpinner.setModel(new javax.swing.SpinnerListModel(new String[] {"9400000", "7600000", "1600000", "600000"}));
        ShiftFreqSpinner.setName("ShiftFreqSpinner"); // NOI18N
        jPanel2.add(ShiftFreqSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 100, 100, 22));

        label1.setName("ShiftFrequency"); // NOI18N
        label1.setText("Shift Frequency"); // NOI18N
        jPanel2.add(label1, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 80, -1, -1));

        ShiftEnabledCheckbox.setText("Shift enabled"); // NOI18N
        ShiftEnabledCheckbox.setName("ShiftEna"); // NOI18N
        jPanel2.add(ShiftEnabledCheckbox, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 20, -1, 20));

        ShiftPositiveCheckbox.setText("Shift positive"); // NOI18N
        ShiftPositiveCheckbox.setName("ShiftPositiveCheckbox"); // NOI18N
        jPanel2.add(ShiftPositiveCheckbox, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 50, -1, 20));

        mainPanel.add(jPanel2);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText("File"); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        connectMenuItem.setText("Connect"); // NOI18N
        connectMenuItem.setName("connectMenuItem"); // NOI18N
        connectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectMenuActionPerformed(evt);
            }
        });
        fileMenu.add(connectMenuItem);

        connectTcpMenuItem.setText("Connect  Server"); // NOI18N
        connectTcpMenuItem.setName("connectTcpMenuItem"); // NOI18N
        connectTcpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectTcpMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(connectTcpMenuItem);

        connectVirtualMenuItem.setText("Connect PRM virtuel"); // NOI18N
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

        channelManagerMenuItem.setText("Channels"); // NOI18N
        channelManagerMenuItem.setEnabled(false);
        channelManagerMenuItem.setName("channelManagerMenuItem"); // NOI18N
        channelManagerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                channelManagerMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(channelManagerMenuItem);

        configurationMenuItem.setText("Configuration"); // NOI18N
        configurationMenuItem.setEnabled(false);
        configurationMenuItem.setName("configurationMenuItem"); // NOI18N
        configurationMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configurationMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(configurationMenuItem);

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

        razMenuItem.setText("R.A.Z."); // NOI18N
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

        serialSpyMenuItem.setText("Spy series"); // NOI18N
        serialSpyMenuItem.setName("serialSpyMenuItem"); // NOI18N
        serialSpyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serialSpyMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(serialSpyMenuItem);

        exportA51MenuItem.setText("Export A51"); // NOI18N
        exportA51MenuItem.setName("exportA51MenuItem"); // NOI18N
        exportA51MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportA51MenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(exportA51MenuItem);

        expertSeparator.setName("expertSeparator"); // NOI18N
        ToolsMenu.add(expertSeparator);

        optionMenuItem.setText("Options"); // NOI18N
        optionMenuItem.setName("optionMenuItem"); // NOI18N
        optionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(optionMenuItem);

        menuBar.add(ToolsMenu);

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
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 334, Short.MAX_VALUE)
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
        try {
            SerialControler prmControler = new SerialControler();
            prmControler.connectPRM(this.config.getSerialPort());
            this.setStatus("Connected to " + this.config.getSerialPort());
                        
            // Si terminal ouvert alors ajouter listener
            if (this.serialSpy != null )
                prmControler.addSerialListener(this.serialSpy);
            
            this.vdf = new VirtualDeviceFirmware(prmControler);
            
            this.disconnectMenuItem.setEnabled(true);
            this.connectMenuItem.setEnabled(false);
            this.connectVirtualMenuItem.setEnabled(false);
            this.memToggleButton.setSelected(true);
            this.connectTcpMenuItem.setEnabled(false);
            
            this.updateValues();
            
            this.vdf.getPRMControler().setPRMStateChangeListener(new PRMStateChangeListener() {
                    @Override
                    public void stateUpdated() {
                        try {
                            updateValues();
                        } catch (CommunicationException ex) {
                            Logger.getLogger(PRM80X0View.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                
            });
            
            this.setEnableControls(true);
       } catch (CommunicationException ex) {
            this.disconnectMenuItem.setEnabled(false);
            this.connectMenuItem.setEnabled(true);
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
        }
    }                                                                                      

    private void upButtonActionPerformed(java.awt.event.ActionEvent evt) {                                         
        try {
            this.vdf.increment();
            this.updateValues();
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            this.disconnect();
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
    }                                                                                

    private void downButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           
        try {
            this.vdf.decrement();
            this.updateValues();
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            this.disconnect();
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
    }                                                                                    

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
        try {
            this.vdf.setVfoMode(false);
            this.chanLabel.setVisible(true);
            this.updateValues();
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            this.disconnect();
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
    }                                                                                              

    private void vfoToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                
        try {
            this.vdf.setVfoMode(true);
            this.chanLabel.setVisible(false);
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            this.disconnect();
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
    }                                                                                              

    private void squelchSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {                                            
        try {
            int value = Integer.parseInt(this.squelchSpinner.getValue().toString());
            if (value < 0) {
                value = 0;
            }
            if (value > 15) {
                value = 15;
            }
            this.squelchSpinner.setValue(value);
            this.vdf.setSquelch(value);
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            this.disconnect();
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
    }                                                                                      

    private void volumeSliderStateChanged(javax.swing.event.ChangeEvent evt) {                                          
        try {
            this.vdf.setVolume(this.volumeSlider.getValue());
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            this.disconnect();
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
    }                                                                                  

    private void channelManagerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_channelManagerMenuItemActionPerformed
            javax.swing.JFrame mainFrame = this;
            try {
                ChannelManager cm = new ChannelManager(mainFrame, true, this.vdf.getPRMControler().getChannels());
                cm.setLocationRelativeTo(mainFrame);
                cm.setVisible(true);
                if (cm.isChannelListValid()) {
                    this.vdf.getPRMControler().setChannels(cm.getChannelList());
                    this.vdf.reset();
                    this.vdf.setCurrentChannel(0);
                    updateValues();
                }
            } catch (CommunicationException ex) {
                this.disconnectMenuItem.setEnabled(false);
                this.connectMenuItem.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
                Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            }
    }//GEN-LAST:event_channelManagerMenuItemActionPerformed

    private void optionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionMenuItemActionPerformed
        javax.swing.JFrame mainFrame = this;
        OptionsDialog cd = new OptionsDialog(mainFrame, true);
        cd.setLocationRelativeTo(mainFrame);
        cd.setConfiguration(this.config);           
        cd.setVisible(true);
        this.setVisibleExpertControls(this.config.isExpertMode());
}//GEN-LAST:event_optionMenuItemActionPerformed

    private void connectVirtualMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectVirtualMenuItemActionPerformed
        try {
        Controler prmControler = new DummyControler();
        prmControler.connectPRM("");
        this.setStatus("Connected to virtual PRM");
        this.vdf = new VirtualDeviceFirmware(prmControler);
        this.disconnectMenuItem.setEnabled(true);
        this.connectMenuItem.setEnabled(false);
        this.connectVirtualMenuItem.setEnabled(false);
        this.connectTcpMenuItem.setEnabled(false);

        this.updateValues();
        
        this.setEnableControls(true);
        } catch (CommunicationException ex) {
            this.disconnectMenuItem.setEnabled(true);
            this.connectMenuItem.setEnabled(false);
            this.connectVirtualMenuItem.setEnabled(false);
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_connectVirtualMenuItemActionPerformed

    private void serialSpyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serialSpyMenuItemActionPerformed
        javax.swing.JFrame mainFrame = this;
            this.serialSpy = new TerminalDialog();
            this.serialSpy.setLocationRelativeTo(mainFrame);
            if (this.vdf != null && this.vdf.getPRMControler().getClass().getName().endsWith("SerialControler")) {
                ((SerialControler) this.vdf.getPRMControler()).addSerialListener(this.serialSpy);
            }            
            this.serialSpy.setVisible(true);
    }//GEN-LAST:event_serialSpyMenuItemActionPerformed

    private void powerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_powerButtonActionPerformed
        try {
            boolean power = !this.vdf.isHighPower();
            this.hpLabel.setVisible(power);
            this.lpLabel.setVisible(!power);
            this.vdf.setHighPower(power);
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            this.disconnect();
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_powerButtonActionPerformed

private void connectTcpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectTcpMenuItemActionPerformed
    ConnectDialog cd = new ConnectDialog(this, true);
    cd.setLocationRelativeTo(this);
    cd.setVisible(true);
    if (cd.getServerAdress() != null) {        
        try {
            TcpControler prmControler = new TcpControler();
            prmControler.connectPRM(cd.getServerAdress());
            this.setStatus("Connected to " + cd.getServerAdress());
            
            this.config.setLastServerURI(cd.getServerAdress());
            // Si terminal ouvert alors ajouter listener
            if (this.serialSpy != null )
                prmControler.addSerialListener(this.serialSpy);
            
            this.vdf = new VirtualDeviceFirmware(prmControler);
            
            this.disconnectMenuItem.setEnabled(true);
            this.connectMenuItem.setEnabled(false);
            this.connectVirtualMenuItem.setEnabled(false);
            this.memToggleButton.setSelected(true);
            this.connectTcpMenuItem.setEnabled(false);
            
            this.updateValues();
            
            this.vdf.getPRMControler().setPRMStateChangeListener(new PRMStateChangeListener() {
                    @Override
                    public void stateUpdated() {
                        try {
                            updateValues();
                        } catch (CommunicationException ex) {
                            Logger.getLogger(PRM80X0View.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                
            });
            
            this.setEnableControls(true);
        } catch (CommunicationException ex) {
            this.disconnectMenuItem.setEnabled(false);
            this.connectMenuItem.setEnabled(true);
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
        }
    }
}//GEN-LAST:event_connectTcpMenuItemActionPerformed

private void exportA51MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportA51MenuItemActionPerformed
    JFileChooser fc = new JFileChooser();
    int value = fc.showSaveDialog(this);
    if (value == JFileChooser.APPROVE_OPTION)  {
            try {
                A51Export.exportA51(this.vdf.getPRMControler(), fc.getSelectedFile());
            } catch (Exception ex) {
                Logger.getLogger(PRM80X0View.class.getName()).log(Level.INFO, null, ex);
            }
    }
}//GEN-LAST:event_exportA51MenuItemActionPerformed

private void resetMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetMenuItemActionPerformed
        try {
            this.vdf.reset();
            updateValues();
        } catch (CommunicationException ex) {
            this.disconnectMenuItem.setEnabled(false);
            this.connectMenuItem.setEnabled(true);
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
}//GEN-LAST:event_resetMenuItemActionPerformed

private void configurationMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configurationMenuItemActionPerformed
//     DriverManager.getInstance().loadDrivers();
    javax.swing.JFrame mainFrame = this;
        ConfigurationDialog cd = new ConfigurationDialog(mainFrame, this.vdf);
        cd.setLocationRelativeTo(mainFrame);
        cd.setVisible(true);
}//GEN-LAST:event_configurationMenuItemActionPerformed

private void ram2eepromMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ram2eepromMenuItemActionPerformed
        try {
            this.vdf.getPRMControler().RAM2EEPROM();
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
}//GEN-LAST:event_ram2eepromMenuItemActionPerformed

private void eeprom2ramMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_eeprom2ramMenuItemActionPerformed
        try {
            this.vdf.getPRMControler().EEPROM2RAM();
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
}//GEN-LAST:event_eeprom2ramMenuItemActionPerformed

private void razMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_razMenuItemActionPerformed
        try {
            this.vdf.getPRMControler().reloadRAM();
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
}//GEN-LAST:event_razMenuItemActionPerformed

private void chanLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_chanLabelMouseClicked
        try {
            if (evt.getButton() == MouseEvent.BUTTON1) {
                SelectChannelDialog d = new SelectChannelDialog(this, true);
                d.setChannel(Integer.parseInt(this.vdf.getChannel()));
                d.setVisible(true);
                if (d.getChannel() != -1) {
                    this.vdf.setCurrentChannel(d.getChannel());
                    this.updateValues();
                }
            }
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }

}//GEN-LAST:event_chanLabelMouseClicked

private void freqLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_freqLabelMouseClicked
     try {
            if (evt.getButton() == MouseEvent.BUTTON1) {
                SelectFrequencyDialog d = new SelectFrequencyDialog(this, true);
                d.setRxFrequency(this.vdf.getPRMControler().getRxPLLFrequency());
                d.setVisible(true);
                if (d.getRxFrequency() != -1) {
                    this.vdf.getPRMControler().setRxPLLFrequecny(d.getRxFrequency());

            if (this.ShiftEnabledCheckbox.isSelected()) {             // ShiftEnabled
                if (this.ShiftPositiveCheckbox.isSelected())           // ShiftPositiv
                    this.vdf.getPRMControler().setTxPLLFrequency(d.getRxFrequency()+Integer.parseInt(this.ShiftFreqSpinner.getValue().toString()));
                else
                    this.vdf.getPRMControler().setTxPLLFrequency(d.getRxFrequency()-Integer.parseInt(this.ShiftFreqSpinner.getValue().toString()));
            }    
            else
                    this.vdf.getPRMControler().setTxPLLFrequency(d.getRxFrequency());
                
                    this.vdf.setVfoMode(true);
                    this.chanLabel.setVisible(false);
                    this.vfoToggleButton.setSelected(true);
                    this.updateValues();
                }
            }
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
            JOptionPane.showMessageDialog(this, "Erreur de connexion : "+ex.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
}//GEN-LAST:event_freqLabelMouseClicked
    private void disconnect() {
        try {
            this.disconnectMenuItem.setEnabled(false);
            this.connectMenuItem.setEnabled(true);
            this.connectVirtualMenuItem.setEnabled(true);
            this.connectTcpMenuItem.setEnabled(true);
            this.setEnableControls(false);
            this.vdf.disconnect();
            this.setStatus("Not connected");
        } catch (CommunicationException ex) {
            Logger.getLogger(PRM80X0View.class.getName()).log(Level.WARNING, null, ex);
        }
    }
    
    private void updateValues() throws CommunicationException {
        this.chanLabel.setText(this.vdf.getChannel());
        this.freqLabel.setText(this.vdf.getRxFrequency());
        this.txFreqLabel.setText(this.vdf.getTxFrequency());

        this.squelchSpinner.setValue(this.vdf.getSquelch());
        this.volumeSlider.setValue(this.vdf.getVolume());

        this.hpLabel.setVisible(this.vdf.isHighPower());
        this.lpLabel.setVisible(!this.vdf.isHighPower());

        if (this.vdf.isPLLLocked()) {
            this.infoLabel.setText("");
        }
        else {
            this.infoLabel.setText("-- Synthesizer ERROR --");
        }

    }
    
    public Option getConfiguration() {
        return this.config;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu ToolsMenu;
    private javax.swing.JLabel chanLabel;
    private javax.swing.JMenuItem channelManagerMenuItem;
    private javax.swing.JCheckBox ShiftEnabledCheckbox;
    private javax.swing.JCheckBox ShiftPositiveCheckbox;
    private javax.swing.JMenuItem configurationMenuItem;
    private javax.swing.JMenuItem connectMenuItem;
    private javax.swing.JMenuItem connectTcpMenuItem;
    private javax.swing.JMenuItem connectVirtualMenuItem;
    private javax.swing.JMenuItem disconnectMenuItem;
    private javax.swing.JButton downButton;
    private javax.swing.JMenuItem eeprom2ramMenuItem;
    private javax.swing.JMenuItem eepromMenuItem;
    private javax.swing.JSeparator expertSeparator;
    private javax.swing.JMenuItem exportA51MenuItem;
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
    private javax.swing.JMenuItem optionMenuItem;
    private javax.swing.JButton powerButton;
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
