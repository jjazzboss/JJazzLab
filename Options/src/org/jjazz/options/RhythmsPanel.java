/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.harmony.TimeSignature;
import static org.jjazz.options.Bundle.CTL_SelectRhythmDir;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythmselectiondialog.ui.RhythmProviderList;
import org.jjazz.rhythmselectiondialog.ui.RhythmTable;
import org.jjazz.ui.utilities.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

@NbBundle.Messages(
        {
            "CTL_SelectRhythmDir=Directory for rhythm files"
        })
final class RhythmsPanel extends javax.swing.JPanel implements PropertyChangeListener, ChangeListener, ListSelectionListener, ActionListener
{

    private final RhythmsOptionsPanelController controller;
    private TimeSignature selectedTimeSignature;
    private RhythmTable rhythmTable = new RhythmTable();
    private RhythmProviderList rhythmProviderList = new RhythmProviderList();
    private static final Logger LOGGER = Logger.getLogger(RhythmsPanel.class.getSimpleName());

    RhythmsPanel(RhythmsOptionsPanelController controller)
    {
        this.controller = controller;
        initComponents();

        // Make sure 4/4 by default
        selectedTimeSignature = TimeSignature.FOUR_FOUR;
        cmb_timeSignature.setSelectedItem(selectedTimeSignature);

        // TODO listen to changes in form fields and call controller.changed()
        // Listen to directory changes
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        fdm.addPropertyChangeListener(this); // RhythmDir changes        

        // Listen to rdb changes
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        rdb.addChangeListener(this);

        // Listen to selection changes
        list_rhythmProviders.addListSelectionListener(this);
        ((RhythmProviderList) list_rhythmProviders).setTimeSignatureFilter(selectedTimeSignature);
        rhythmTable.getSelectionModel().addListSelectionListener(this);
        cmb_timeSignature.addActionListener(this);

        rhythmTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                handleMouseClicked(e);
            }
        });
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.log(Level.FINE, "PropertyChangeEvent() evt={0}", evt);
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        if (evt.getSource() == fdm)
        {
            if (evt.getPropertyName() == FileDirectoryManager.PROP_RHYTHM_USER_DIRECTORY)
            {
                updateRhythmUserDirField();
            }
        }
    }

    // -----------------------------------------------------------------------------------
    // ListSelectionListener implementation
    // -----------------------------------------------------------------------------------    
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (e.getValueIsAdjusting())
        {
            return;
        }
        if (e.getSource() == list_rhythmProviders)
        {
            RhythmProvider rp = list_rhythmProviders.getSelectedValue();
            LOGGER.log(Level.FINE, "valueChanged() selected RhythmProvider=" + rp);
            boolean b = false;
            if (rp != null)
            {
                updateRhythmTable(rp);
                b = rp.hasUserSettings();
            }
            btn_rhythmProviderSettings.setEnabled(b);
        } else if (e.getSource() == rhythmTable.getSelectionModel())
        {
            Rhythm r = rhythmTable.getSelectedRhythm();
            LOGGER.log(Level.FINE, "valueChanged() selected Rhythm=" + r);
            btn_setDefaultRhythm.setEnabled(r != null);
        }
    }

    // -----------------------------------------------------------------------------------
    // ActionListener implementation
    // -----------------------------------------------------------------------------------    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == cmb_timeSignature)
        {
            TimeSignature ts = cmb_timeSignature.getItemAt(cmb_timeSignature.getSelectedIndex());
            if (ts.equals(selectedTimeSignature))
            {
                return;
            }
            selectedTimeSignature = ts;
            updateDefaultRhythmField();
            ((RhythmProviderList) list_rhythmProviders).setTimeSignatureFilter(selectedTimeSignature);

            // Select default rhythm if there is one         
            RhythmDatabase rdb = RhythmDatabase.getDefault();
            Rhythm r = rdb.getDefaultRhythm(selectedTimeSignature);  // Can be null
            RhythmProvider rp = list_rhythmProviders.getSelectedValue();   // Can be null
            if (r != null)
            {
                rp = rdb.getRhythmProvider(r);
            }
            if (rp != null)
            {
                list_rhythmProviders.clearSelection();    // Make sure there will be a selection change
                list_rhythmProviders.setSelectedValue(rp, true);     // Selection will repopulate the rhythm via our listener
                if (r != null)
                {
                    rhythmTable.setSelectedRhythm(r);
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------
    // ChangeListener implementation
    // -----------------------------------------------------------------------------------    
    /**
     * Rhythmdatabase has changed.
     *
     * @param e
     */
    @Override
    public void stateChanged(ChangeEvent e)
    {
        // We can be out of the EDT
        LOGGER.fine("stateChanged()");
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                updateRhythmProviderList();
            }
        };
        org.jjazz.ui.utilities.Utilities.invokeLaterIfNeeded(run);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings(
            {
                "rawtypes" // For the JTable init
            })
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        lbl_rhythmDir = new javax.swing.JLabel();
        tf_rhythmUserDir = new javax.swing.JTextField();
        btn_rhythmDir = new javax.swing.JButton();
        btn_rescan = new javax.swing.JButton();
        tf_defaultRhythm = new javax.swing.JTextField();
        btn_setDefaultRhythm = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_rhythms = rhythmTable;
        jScrollPane2 = new javax.swing.JScrollPane();
        list_rhythmProviders = rhythmProviderList;
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        cmb_timeSignature = new JComboBox<>(TimeSignature.values());
        lbl_defaultRhythm = new javax.swing.JLabel();
        btn_rhythmProviderSettings = new javax.swing.JButton();
        lbl_timeSignature = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.HelpTextArea();

        org.openide.awt.Mnemonics.setLocalizedText(lbl_rhythmDir, org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.lbl_rhythmDir.text")); // NOI18N

        tf_rhythmUserDir.setEditable(false);
        tf_rhythmUserDir.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.tf_rhythmUserDir.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_rhythmDir, org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.btn_rhythmDir.text")); // NOI18N
        btn_rhythmDir.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.btn_rhythmDir.toolTipText")); // NOI18N
        btn_rhythmDir.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_rhythmDirActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_rescan, org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.btn_rescan.text")); // NOI18N
        btn_rescan.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.btn_rescan.toolTipText")); // NOI18N
        btn_rescan.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_rescanActionPerformed(evt);
            }
        });

        tf_defaultRhythm.setEditable(false);
        tf_defaultRhythm.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.tf_defaultRhythm.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_setDefaultRhythm, org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.btn_setDefaultRhythm.text")); // NOI18N
        btn_setDefaultRhythm.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.btn_setDefaultRhythm.toolTipText")); // NOI18N
        btn_setDefaultRhythm.setEnabled(false);
        btn_setDefaultRhythm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_setDefaultRhythmActionPerformed(evt);
            }
        });

        tbl_rhythms.setMaximumSize(new java.awt.Dimension(2147483647, 202020));
        tbl_rhythms.setMinimumSize(new java.awt.Dimension(60, 30));
        jScrollPane1.setViewportView(tbl_rhythms);

        list_rhythmProviders.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(list_rhythmProviders);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.jLabel2.text")); // NOI18N

        cmb_timeSignature.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.cmb_timeSignature.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_defaultRhythm, org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.lbl_defaultRhythm.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_rhythmProviderSettings, org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.btn_rhythmProviderSettings.text")); // NOI18N
        btn_rhythmProviderSettings.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.btn_rhythmProviderSettings.toolTipText")); // NOI18N
        btn_rhythmProviderSettings.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_rhythmProviderSettingsActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_timeSignature, org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.lbl_timeSignature.text")); // NOI18N

        jScrollPane4.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(2);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(RhythmsPanel.class, "RhythmsPanel.helpTextArea1.text")); // NOI18N
        jScrollPane4.setViewportView(helpTextArea1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 461, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_rhythmDir)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(123, 123, 123)
                                .addComponent(jLabel2))
                            .addComponent(btn_rhythmProviderSettings)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tf_rhythmUserDir, javax.swing.GroupLayout.PREFERRED_SIZE, 398, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_rhythmDir)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_rescan)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane4)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_timeSignature)
                            .addComponent(cmb_timeSignature, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(134, 134, 134)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tf_defaultRhythm)
                                .addGap(6, 6, 6))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lbl_defaultRhythm)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(btn_setDefaultRhythm)
                        .addGap(133, 133, 133)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_rhythmDir)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tf_rhythmUserDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_rescan)
                    .addComponent(btn_rhythmDir))
                .addGap(2, 2, 2)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_defaultRhythm)
                    .addComponent(lbl_timeSignature))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tf_defaultRhythm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_setDefaultRhythm)
                    .addComponent(cmb_timeSignature, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_rhythmProviderSettings)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

   private void btn_rhythmDirActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_rhythmDirActionPerformed
   {//GEN-HEADEREND:event_btn_rhythmDirActionPerformed
       FileDirectoryManager fdm = FileDirectoryManager.getInstance();
       File oldDir = fdm.getUserRhythmDirectory();
       File newDir = Utilities.showDirChooser(tf_rhythmUserDir.getText(), CTL_SelectRhythmDir());
       if (newDir != null && !oldDir.equals(newDir))
       {
           fdm.setUserRhythmDirectory(newDir);   // RhythmDatabase should get refreshed itself and possibly trigger change event    
       }
   }//GEN-LAST:event_btn_rhythmDirActionPerformed

    private void btn_rescanActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_rescanActionPerformed
    {//GEN-HEADEREND:event_btn_rescanActionPerformed
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        rdb.refresh(false);
    }//GEN-LAST:event_btn_rescanActionPerformed

    private void btn_setDefaultRhythmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_setDefaultRhythmActionPerformed
    {//GEN-HEADEREND:event_btn_setDefaultRhythmActionPerformed
        Rhythm r = rhythmTable.getSelectedRhythm();
        if (r != null)
        {
            RhythmDatabase rdb = RhythmDatabase.getDefault();
            rdb.setDefaultRhythm(selectedTimeSignature, r);
            updateDefaultRhythmField();        // Because previous line does not fire an event
        }
    }//GEN-LAST:event_btn_setDefaultRhythmActionPerformed

    private void btn_rhythmProviderSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_rhythmProviderSettingsActionPerformed
    {//GEN-HEADEREND:event_btn_rhythmProviderSettingsActionPerformed
        RhythmProvider rp = list_rhythmProviders.getSelectedValue();
        if (rp != null)
        {
            rp.showUserSettingsDialog();
        }
    }//GEN-LAST:event_btn_rhythmProviderSettingsActionPerformed

    void load()
    {
        // TODO read settings and initialize GUI
        // Example:        
        // someCheckBox.setSelected(Preferences.userNodeForPackage(RhythmsPanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(RhythmsPanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());

        // Prepare UI
        updateDefaultRhythmField();
        updateRhythmUserDirField();
        updateRhythmProviderList();
    }

    void store()
    {
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(RhythmsPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(RhythmsPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());

        // NOTHING: changes are stored on the fly
    }

    boolean valid()
    {
        // TODO check whether form is consistent and complete
        return true;
    }

    // ===================================================================================
    // Private mehods
    // ===================================================================================
    private void handleMouseClicked(MouseEvent e)
    {
        if (e.getClickCount() == 2)
        {
            btn_setDefaultRhythmActionPerformed(null);
        }
    }

    private void updateRhythmUserDirField()
    {
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        File f = fdm.getUserRhythmDirectory();
        String s = f.getAbsolutePath();
        tf_rhythmUserDir.setText(s);
    }

    /**
     * Update the list of RhythmProviders.
     */
    private void updateRhythmProviderList()
    {
        // Reset the rhythm table
        // rhythmTable.clear();

        // Refresh rhythm providers list
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        RhythmProvider[] rps = rdb.getRhythmProviders().toArray(new RhythmProvider[0]);
        list_rhythmProviders.setListData(rps);

        if (rps.length > 0)
        {
            list_rhythmProviders.setSelectedIndex(0);
        }
    }

    /**
     * Reset the rhythm table with rp's rhythms and try to restore selection.
     * <p>
     * @param rp
     * @param sri
     */
    private void updateRhythmTable(RhythmProvider rp)
    {
        if (rp == null)
        {
            throw new IllegalArgumentException("rp=" + rp);
        }
        // Refresh the list of rhythms        
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        List<Rhythm> rhythms = rdb.getRhythms(rp);    // All rp's rhythms
        rhythms = rdb.getRhythms(selectedTimeSignature, rhythms);   // only for the current timesignature
        // Update the table
        LOGGER.fine("updateRhythmTable() rp=" + rp.getInfo().getName() + " rhythms.size()=" + rhythms.size());
        rhythmTable.getModel().setRhythms(rhythms);
    }

    private void updateDefaultRhythmField()
    {
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        Rhythm r = rdb.getDefaultRhythm(selectedTimeSignature);
        String s = (r == null) ? "-" : rdb.getDefaultRhythm(selectedTimeSignature).getName();
        String t = (r == null) ? "-" : "Rhythm Provider: " + rdb.getRhythmProvider(r).getInfo().getName();
        tf_defaultRhythm.setText(s);
        tf_defaultRhythm.setToolTipText(t);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_rescan;
    private javax.swing.JButton btn_rhythmDir;
    private javax.swing.JButton btn_rhythmProviderSettings;
    private javax.swing.JButton btn_setDefaultRhythm;
    private javax.swing.JComboBox<TimeSignature> cmb_timeSignature;
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JLabel lbl_defaultRhythm;
    private javax.swing.JLabel lbl_rhythmDir;
    private javax.swing.JLabel lbl_timeSignature;
    private javax.swing.JList<RhythmProvider> list_rhythmProviders;
    private javax.swing.JTable tbl_rhythms;
    private javax.swing.JTextField tf_defaultRhythm;
    private javax.swing.JTextField tf_rhythmUserDir;
    // End of variables declaration//GEN-END:variables

}
