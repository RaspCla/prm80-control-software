/*
 * ConnectDialog.java
 *
 * Created on 11 novembre 2008, 17:02
 */

package f4fez.prm80x0.gui;

import f4fez.prm80x0.Option;

/**
 *
 * @author  f4fez
 */
public class ConnectDialog extends javax.swing.JDialog {

    /** Creates new form ConnectDialog */
    public ConnectDialog(PRM80X0View parent, boolean modal) {
        super(parent.getFrame(), modal);
        this.config = parent.getConfiguration();
        this.setLocationRelativeTo(parent.getFrame());        
        initComponents();
        this.serverField.setText(config.getLastServerURI());
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
        jLabel1 = new javax.swing.JLabel();
        serverField = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        connectButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(f4fez.prm80x0.PRM80X0App.class).getContext().getResourceMap(ConnectDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jPanel1.add(jLabel1);

        serverField.setText(resourceMap.getString("serverField.text")); // NOI18N
        serverField.setName("serverField"); // NOI18N
        serverField.setPreferredSize(new java.awt.Dimension(300, 27));
        jPanel1.add(serverField);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel2.setName("jPanel2"); // NOI18N

        connectButton.setText(resourceMap.getString("connectButton.text")); // NOI18N
        connectButton.setName("connectButton"); // NOI18N
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });
        jPanel2.add(connectButton);

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        jPanel2.add(cancelButton);

        getContentPane().add(jPanel2, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
    this.serverAdress = this.serverField.getText();
    this.setVisible(false);
}//GEN-LAST:event_connectButtonActionPerformed

private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    this.serverAdress = null;
    this.setVisible(false);
}//GEN-LAST:event_cancelButtonActionPerformed

    
    public String getServerAdress() {
        return this.serverAdress;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton connectButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField serverField;
    // End of variables declaration//GEN-END:variables
    private String serverAdress;
    private Option config;
}