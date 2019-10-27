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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midi.MidiConst;
import static org.jjazz.options.Bundle.*;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midi.ui.MidiDeviceRenderer;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerationException;
import org.openide.*;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

@NbBundle.Messages(
        {
            "CTL_DeviceProblem=Problem accessing MIDI device: ",
            "ERR_SynthSoundFileProblem=Problem loading sound file"
        })
final class MidiPanel extends javax.swing.JPanel
{

    private final MidiOptionsPanelController controller;
    private boolean loadInProgress;
    private MidiDevice saveOutDevice;      // For cancel operation
    private MidiDevice saveInDevice;       // For cancel operation
    private boolean saveMidiThru;          // For cancel operation
    private boolean saveSendGmOnUponStartup;          // For cancel operation
    private static final Logger LOGGER = Logger.getLogger(MidiPanel.class.getSimpleName());

    MidiPanel(MidiOptionsPanelController controller)
    {
        this.controller = controller;
        initComponents();
        list_InDevices.setCellRenderer(new MidiDeviceRenderer());
        btn_test.setEnabled(false);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jButton1 = new javax.swing.JButton();
        lbl_OutDevices = new javax.swing.JLabel();
        cb_midiThru = new javax.swing.JCheckBox();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_InDevices = new javax.swing.JList<>();
        lbl_InDevices = new javax.swing.JLabel();
        btn_test = new javax.swing.JButton();
        pnl_soundbankFile = new javax.swing.JPanel();
        txtf_soundbankFile = new javax.swing.JTextField();
        btn_changeSoundbankFile = new javax.swing.JButton();
        btn_resetSoundbank = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        list_OutDevices = new org.jjazz.midi.ui.MidiOutDeviceList();
        btn_refresh = new javax.swing.JButton();
        cb_sendGmOnUponStartup = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.jButton1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_OutDevices, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.lbl_OutDevices.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_midiThru, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.cb_midiThru.text")); // NOI18N
        cb_midiThru.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.cb_midiThru.toolTipText")); // NOI18N
        cb_midiThru.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_midiThruActionPerformed(evt);
            }
        });

        list_InDevices.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_InDevices.setEnabled(false);
        list_InDevices.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_InDevicesValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(list_InDevices);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_InDevices, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.lbl_InDevices.text")); // NOI18N
        lbl_InDevices.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(btn_test, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_test.text")); // NOI18N
        btn_test.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_test.toolTipText")); // NOI18N
        btn_test.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_testActionPerformed(evt);
            }
        });

        pnl_soundbankFile.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.pnl_soundbankFile.border.title"))); // NOI18N

        txtf_soundbankFile.setEditable(false);
        txtf_soundbankFile.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.txtf_soundbankFile.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_changeSoundbankFile, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_changeSoundbankFile.text")); // NOI18N
        btn_changeSoundbankFile.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_changeSoundbankFileActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_resetSoundbank, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_resetSoundbank.text")); // NOI18N
        btn_resetSoundbank.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_resetSoundbankActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_soundbankFileLayout = new javax.swing.GroupLayout(pnl_soundbankFile);
        pnl_soundbankFile.setLayout(pnl_soundbankFileLayout);
        pnl_soundbankFileLayout.setHorizontalGroup(
            pnl_soundbankFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_soundbankFileLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txtf_soundbankFile, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_changeSoundbankFile)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_resetSoundbank)
                .addContainerGap())
        );
        pnl_soundbankFileLayout.setVerticalGroup(
            pnl_soundbankFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_soundbankFileLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_soundbankFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtf_soundbankFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_changeSoundbankFile)
                    .addComponent(btn_resetSoundbank))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        list_OutDevices.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_OutDevicesValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(list_OutDevices);

        org.openide.awt.Mnemonics.setLocalizedText(btn_refresh, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_refresh.text")); // NOI18N
        btn_refresh.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_refresh.toolTipText")); // NOI18N
        btn_refresh.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_refreshActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_sendGmOnUponStartup, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.cb_sendGmOnUponStartup.text")); // NOI18N
        cb_sendGmOnUponStartup.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.cb_sendGmOnUponStartup.toolTipText")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_OutDevices, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbl_InDevices, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(btn_refresh))
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 268, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cb_sendGmOnUponStartup)
                            .addComponent(btn_test)
                            .addComponent(cb_midiThru)
                            .addComponent(pnl_soundbankFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(cb_midiThru)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cb_sendGmOnUponStartup)
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_OutDevices)
                    .addComponent(lbl_InDevices))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_test)
                    .addComponent(btn_refresh))
                .addGap(21, 21, 21)
                .addComponent(pnl_soundbankFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(87, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cb_midiThruActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_midiThruActionPerformed
    {//GEN-HEADEREND:event_cb_midiThruActionPerformed
        controller.changed();
        controller.applyChanges();
    }//GEN-LAST:event_cb_midiThruActionPerformed

    private void list_InDevicesValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_InDevicesValueChanged
    {//GEN-HEADEREND:event_list_InDevicesValueChanged
        if (loadInProgress || evt.getValueIsAdjusting())
        {
            return;
        }
        controller.applyChanges();
        controller.changed();
    }//GEN-LAST:event_list_InDevicesValueChanged

    private void btn_testActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_testActionPerformed
    {//GEN-HEADEREND:event_btn_testActionPerformed
        LOGGER.log(Level.FINE, "Testing {0}", list_OutDevices.getSelectedValue().getDeviceInfo().getName());
        sendTestNotes();
    }//GEN-LAST:event_btn_testActionPerformed

   private void btn_changeSoundbankFileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_changeSoundbankFileActionPerformed
   {//GEN-HEADEREND:event_btn_changeSoundbankFileActionPerformed
       JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
       FileDirectoryManager fdm = FileDirectoryManager.getInstance();
       JFileChooser chooser = org.jjazz.ui.utilities.Utilities.getFileChooserInstance();
       FileNameExtensionFilter filter = new FileNameExtensionFilter(".sf2, .dls files ", "sf2", "dls", "SF2", "DLS");
       chooser.resetChoosableFileFilters();
       chooser.setMultiSelectionEnabled(false);
       chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
       chooser.setFileFilter(filter);
       File previousFile = jms.getDefaultJavaSynthPreferredSoundFontFile();
       if (previousFile == null)
       {
           chooser.setCurrentDirectory(fdm.getLastSongDirectory());
       } else
       {
           chooser.setSelectedFile(previousFile);
       }
       if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) != JFileChooser.APPROVE_OPTION)
       {
           // User cancel
           return;
       }

       // Process selected files
       File f = chooser.getSelectedFile();
       if (f.equals(previousFile))
       {
           return;
       }

       boolean b = jms.loadSoundbankFileOnSynth(f, false);
       if (!b)
       {
           String msg = ERR_SynthSoundFileProblem() + ":" + f.getAbsolutePath();
           NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
           DialogDisplayer.getDefault().notify(d);
       }
       updateSoundbankText();
   }//GEN-LAST:event_btn_changeSoundbankFileActionPerformed

   private void btn_resetSoundbankActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_resetSoundbankActionPerformed
   {//GEN-HEADEREND:event_btn_resetSoundbankActionPerformed
       JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
       jms.resetSynth();
       updateSoundbankText();
   }//GEN-LAST:event_btn_resetSoundbankActionPerformed

    private void list_OutDevicesValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_OutDevicesValueChanged
    {//GEN-HEADEREND:event_list_OutDevicesValueChanged
        if (loadInProgress || evt.getValueIsAdjusting())
        {
            return;
        }
        MidiDevice md = list_OutDevices.getSelectedValue();
        btn_test.setEnabled(md != null);
        boolean b = (md instanceof Synthesizer);
        org.jjazz.ui.utilities.Utilities.setRecursiveEnabled(b, pnl_soundbankFile);
        controller.applyChanges();
        controller.changed();
    }//GEN-LAST:event_list_OutDevicesValueChanged

    private void btn_refreshActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_refreshActionPerformed
    {//GEN-HEADEREND:event_btn_refreshActionPerformed
        MidiDevice save = list_OutDevices.getSelectedValue();
        list_OutDevices.rescanMidiDevices();
        if (save != null)
        {
            list_OutDevices.setSelectedValue(save, true);
        }
    }//GEN-LAST:event_btn_refreshActionPerformed

    void load()
    {
        LOGGER.log(Level.FINE, "load() --");
        // TODO read settings and initialize GUI
        // Example:        
        // someCheckBox.setSelected(Preferences.userNodeForPackage(MidiPanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(MidiPanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());

        loadInProgress = true; // To avoid calling controller.changed() via list_In/OutDevices change event handlers.           

        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();

        // In devices : easy
        list_InDevices.setListData(jms.getInDeviceList().toArray(new MidiDevice[0]));

        // Select default devices (can be null)
        saveOutDevice = jms.getDefaultOutDevice();
        saveInDevice = jms.getDefaultInDevice();
        LOGGER.log(Level.FINE, "load() saveOutDevice=" + saveOutDevice + " .info=" + ((saveOutDevice == null) ? "null" : saveOutDevice.getDeviceInfo()));
        list_OutDevices.setSelectedValue(saveOutDevice, true);
        list_InDevices.setSelectedValue(saveInDevice, true);

        // Other stuff
        saveMidiThru = jms.isThruMode();
        cb_midiThru.setSelected(saveMidiThru);
        saveSendGmOnUponStartup = jms.isSendGmOnUponStartup();
        cb_sendGmOnUponStartup.setSelected(saveSendGmOnUponStartup);

        btn_test.setEnabled(saveOutDevice != null);

        // Soundbank enabled only if Out device is a synth
        boolean b = (saveOutDevice instanceof Synthesizer);
        org.jjazz.ui.utilities.Utilities.setRecursiveEnabled(b, pnl_soundbankFile);
        updateSoundbankText();

        loadInProgress = false;
    }

    public void cancel()
    {
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        jms.setThruMode(saveMidiThru);
        jms.setSendGmOnUponStartup(saveSendGmOnUponStartup);
        openInDevice(saveInDevice);
        openOutDevice(saveOutDevice);
    }

    void store()
    {
        LOGGER.log(Level.FINE, "store() --");
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(MidiPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(MidiPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        jms.setThruMode(cb_midiThru.isSelected());
        jms.setSendGmOnUponStartup(cb_sendGmOnUponStartup.isSelected());
        MidiDevice inDevice = list_InDevices.getSelectedValue();
        openInDevice(inDevice);
        MidiDevice outDevice = list_OutDevices.getSelectedValue();
        LOGGER.log(Level.FINE, "store() outDevice=" + outDevice + " .info=" + ((outDevice == null) ? "null" : outDevice.getDeviceInfo()));
        openOutDevice(outDevice);
    }

    /**
     * Set the default int device to mdOut.
     *
     * @param mdIn Can be null.
     * @return False if there was an error.
     */
    protected boolean openInDevice(MidiDevice mdIn)
    {
        try
        {
            JJazzMidiSystem.getInstance().setDefaultInDevice(mdIn);
        } catch (MidiUnavailableException ex)
        {
            String msg = CTL_DeviceProblem() + mdIn.getDeviceInfo().getName();
            msg += "\n\n" + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return false;
        }
        return true;
    }

    /**
     * Set the default out device to mdOut.
     *
     * @param mdOut Can be null.
     * @return False if there was an error.
     */
    protected boolean openOutDevice(MidiDevice mdOut)
    {
        try
        {
            JJazzMidiSystem.getInstance().setDefaultOutDevice(mdOut);
        } catch (MidiUnavailableException ex)
        {
            String msg = CTL_DeviceProblem() + mdOut.getDeviceInfo().getName();
            msg += "\n\n" + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return false;
        }
        return true;
    }

    boolean valid()
    {
        // LOGGER.log(Level.INFO, "valid()");
        // TODO check whether form is consistent and complete
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_changeSoundbankFile;
    private javax.swing.JButton btn_refresh;
    private javax.swing.JButton btn_resetSoundbank;
    private javax.swing.JButton btn_test;
    private javax.swing.JCheckBox cb_midiThru;
    private javax.swing.JCheckBox cb_sendGmOnUponStartup;
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lbl_InDevices;
    private javax.swing.JLabel lbl_OutDevices;
    private javax.swing.JList<MidiDevice> list_InDevices;
    private org.jjazz.midi.ui.MidiOutDeviceList list_OutDevices;
    private javax.swing.JPanel pnl_soundbankFile;
    private javax.swing.JTextField txtf_soundbankFile;
    // End of variables declaration//GEN-END:variables

    private void updateSoundbankText()
    {
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        File f = jms.getDefaultJavaSynthPreferredSoundFontFile();
        txtf_soundbankFile.setText(f == null ? "Default sound bank" : f.getAbsolutePath());
    }

    /**
     * Send a few notes with the associated MIDI Out device.
     */
    private void sendTestNotes()
    {
        this.btn_test.setEnabled(false);
        this.btn_refresh.setEnabled(false);
        this.list_InDevices.setEnabled(false);
        this.list_OutDevices.setEnabled(false);
        Runnable endAction = new Runnable()
        {
            @Override
            public void run()
            {
                // Called when sequence is stopped
                btn_test.setEnabled(true);
                btn_refresh.setEnabled(true);
                // list_InDevices.setEnabled(true);
                list_OutDevices.setEnabled(true);
            }
        };

        MusicController mc = MusicController.getInstance();
        try
        {
            mc.playTestNotes(MidiConst.CHANNEL_MIN, -1, 0, endAction);
        } catch (MusicGenerationException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }
    }

}
