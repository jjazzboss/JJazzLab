/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.musiccontrolactions.api.RemoteAction;
import org.jjazz.musiccontrolactions.api.RemoteController;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

final class MidiInPanel extends javax.swing.JPanel implements ListSelectionListener
{

    private static final String NO_INCOMING_NOTE = "-";
    private static final int MIDI_LEARN_TIME_OUT_MS = 4000;
    private boolean loadInProgress;
    private MidiDevice saveInDevice;       // For cancel operation
    private boolean saveMidiThru;          // For cancel operation
    private boolean saveRemoteControlEnabled;          // For cancel operation
    private final List<List<MidiMessage>> saveMidiMessages = new ArrayList<>();

    private final MidiInOptionsPanelController controller;
    private static final Logger LOGGER = Logger.getLogger(MidiInPanel.class.getSimpleName());

    MidiInPanel(MidiInOptionsPanelController controller)
    {
        this.controller = controller;
        initComponents();
        // TODO listen to changes in form fields and call controller.changed()


        list_actions.addListSelectionListener(this);
        list_actions.setCellRenderer(new RemoteActionRenderer());

        JJazzMidiSystem.getInstance().getJJazzMidiInDevice().getTransmitter().setReceiver(new LastNoteDisplayer());
    }

    // ===========================================================================================
    // ListSelectionListener interface
    // ===========================================================================================

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (e != null && e.getValueIsAdjusting())
        {
            return;
        }

        list_actions.setEnabled(cb_enableRemoteControl.isSelected());
        lbl_midiMessages.setEnabled(cb_enableRemoteControl.isSelected());

        RemoteAction ra = list_actions.getSelectedValue();
        boolean b = ra != null && cb_enableRemoteControl.isSelected();
        btn_reset.setEnabled(b);
        btn_learn.setEnabled(b);
        tf_midiMessages.setEnabled(b);
        tf_midiMessages.setText(b ? getMidiMessageString(ra.getMidiMessages()) : "");
    }

    void load()
    {
        // TODO read settings and initialize GUI
        // Example:        
        // someCheckBox.setSelected(Preferences.userNodeForPackage(MidiInPanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(MidiInPanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());
        LOGGER.log(Level.FINE, "load() --");   
        // TODO read settings and initialize GUI
        // Example:        
        // someCheckBox.setSelected(Preferences.userNodeForPackage(MidiPanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(MidiPanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());

        loadInProgress = true; // To avoid calling controller.changed() via list_In change event handlers.           

        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();


        // In devices : easy
        list_InDevices.setListData(jms.getInDeviceList().toArray(new MidiDevice[0]));


        // Select default devices (can be null)
        saveInDevice = jms.getDefaultInDevice();
        list_InDevices.setSelectedValue(saveInDevice, true);


        // Remote control
        RemoteController rc = RemoteController.getInstance();
        saveRemoteControlEnabled = rc.isEnabled();
        cb_enableRemoteControl.setSelected(saveRemoteControlEnabled);
        var remoteActions = rc.getRemoteActions();
        list_actions.removeListSelectionListener(this);
        list_actions.setListData(remoteActions.toArray(new RemoteAction[0]));
        list_actions.addListSelectionListener(this);
        list_actions.setSelectedIndex(0);
        saveRemoteActions(remoteActions);


        lbl_inNote.setText(NO_INCOMING_NOTE);
        enableRemoteControlUI(saveInDevice != null);

        // Other stuff
        saveMidiThru = jms.isThruMode();
        cb_midiThru.setSelected(saveMidiThru);

        loadInProgress = false;
    }

    void store()
    {
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(MidiInPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(MidiInPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        jms.setThruMode(cb_midiThru.isSelected());
        MidiDevice inDevice = list_InDevices.getSelectedValue();
        openInDevice(inDevice);


        RemoteController rc = RemoteController.getInstance();
        rc.setEnabled(cb_enableRemoteControl.isSelected());
        for (RemoteAction ra : rc.getRemoteActions())
        {
            ra.saveAsPreference();
        }

        if (cb_enableRemoteControl.isSelected() != saveRemoteControlEnabled)
        {
            Analytics.logEvent("Remote Control", Analytics.buildMap("Enabled", cb_enableRemoteControl.isSelected()));
        }
    }

    boolean valid()
    {
        // TODO check whether form is consistent and complete
        return true;
    }

    public void cancel()
    {
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        jms.setThruMode(saveMidiThru);
        openInDevice(saveInDevice);

        RemoteController rc = RemoteController.getInstance();
        rc.setEnabled(saveRemoteControlEnabled);
        restoreRemoteActions(RemoteController.getInstance().getRemoteActions());
    }

    // ===========================================================================================
    // Private methods
    // ===========================================================================================

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
            String msg = ResUtil.getString(getClass(), "ERR_DeviceProblem", mdIn.getDeviceInfo().getName());
            msg += "\n\n" + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, msg);   
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return false;
        }
        return true;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_remoteControl = new javax.swing.JPanel();
        cb_enableRemoteControl = new javax.swing.JCheckBox();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_actions = new javax.swing.JList<>();
        btn_learn = new javax.swing.JButton();
        lbl_midiMessages = new javax.swing.JLabel();
        btn_reset = new javax.swing.JButton();
        tf_midiMessages = new javax.swing.JTextField();
        cb_midiThru = new javax.swing.JCheckBox();
        pnl_inDevice = new javax.swing.JPanel();
        lbl_midiInNote = new javax.swing.JLabel();
        lbl_inNote = new javax.swing.JLabel();
        btn_refreshIn = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        list_InDevices = new org.jjazz.midi.api.ui.MidiInDeviceList();

        pnl_remoteControl.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getBundle(MidiInPanel.class).getString("MidiInPanel.pnl_remoteControl.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_enableRemoteControl, org.openide.util.NbBundle.getBundle(MidiInPanel.class).getString("MidiInPanel.cb_enableRemoteControl.text")); // NOI18N
        cb_enableRemoteControl.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_enableRemoteControlActionPerformed(evt);
            }
        });

        list_actions.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_actions.setVisibleRowCount(6);
        jScrollPane2.setViewportView(list_actions);

        org.openide.awt.Mnemonics.setLocalizedText(btn_learn, org.openide.util.NbBundle.getMessage(MidiInPanel.class, "MidiInPanel.btn_learn.text")); // NOI18N
        btn_learn.setToolTipText(org.openide.util.NbBundle.getMessage(MidiInPanel.class, "MidiInPanel.btn_learn.toolTipText")); // NOI18N
        btn_learn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_learnActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_midiMessages, org.openide.util.NbBundle.getMessage(MidiInPanel.class, "MidiInPanel.lbl_midiMessages.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_reset, org.openide.util.NbBundle.getMessage(MidiInPanel.class, "MidiInPanel.btn_reset.text")); // NOI18N
        btn_reset.setToolTipText(org.openide.util.NbBundle.getMessage(MidiInPanel.class, "MidiInPanel.btn_reset.toolTipText")); // NOI18N
        btn_reset.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_resetActionPerformed(evt);
            }
        });

        tf_midiMessages.setEditable(false);
        tf_midiMessages.setText("C4  (60)"); // NOI18N
        tf_midiMessages.setToolTipText(org.openide.util.NbBundle.getMessage(MidiInPanel.class, "MidiInPanel.tf_midiMessages.toolTipText")); // NOI18N

        javax.swing.GroupLayout pnl_remoteControlLayout = new javax.swing.GroupLayout(pnl_remoteControl);
        pnl_remoteControl.setLayout(pnl_remoteControlLayout);
        pnl_remoteControlLayout.setHorizontalGroup(
            pnl_remoteControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_remoteControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_remoteControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_remoteControlLayout.createSequentialGroup()
                        .addComponent(cb_enableRemoteControl)
                        .addContainerGap())
                    .addGroup(pnl_remoteControlLayout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnl_remoteControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnl_remoteControlLayout.createSequentialGroup()
                                .addComponent(lbl_midiMessages)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(pnl_remoteControlLayout.createSequentialGroup()
                                .addGroup(pnl_remoteControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(tf_midiMessages)
                                    .addGroup(pnl_remoteControlLayout.createSequentialGroup()
                                        .addComponent(btn_learn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btn_reset)
                                        .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())))))
        );
        pnl_remoteControlLayout.setVerticalGroup(
            pnl_remoteControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_remoteControlLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cb_enableRemoteControl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_remoteControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_remoteControlLayout.createSequentialGroup()
                        .addComponent(lbl_midiMessages)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tf_midiMessages, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnl_remoteControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_learn)
                            .addComponent(btn_reset))
                        .addGap(0, 19, Short.MAX_VALUE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        org.openide.awt.Mnemonics.setLocalizedText(cb_midiThru, org.openide.util.NbBundle.getMessage(MidiInPanel.class, "MidiInPanel.cb_midiThru.text")); // NOI18N
        cb_midiThru.setToolTipText(org.openide.util.NbBundle.getMessage(MidiInPanel.class, "MidiInPanel.cb_midiThru.toolTipText")); // NOI18N
        cb_midiThru.setHideActionText(true);
        cb_midiThru.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_midiThruActionPerformed(evt);
            }
        });

        pnl_inDevice.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiInPanel.class, "MidiInPanel.pnl_inDevice.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_midiInNote, org.openide.util.NbBundle.getBundle(MidiInPanel.class).getString("MidiInPanel.lbl_midiInNote.text")); // NOI18N
        lbl_midiInNote.setToolTipText(org.openide.util.NbBundle.getBundle(MidiInPanel.class).getString("MidiInPanel.lbl_midiInNote.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_inNote, "C4"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_refreshIn, org.openide.util.NbBundle.getMessage(MidiInPanel.class, "MidiInPanel.btn_refreshIn.text")); // NOI18N
        btn_refreshIn.setToolTipText(org.openide.util.NbBundle.getBundle(MidiInPanel.class).getString("MidiInPanel.btn_refreshIn.toolTipText")); // NOI18N
        btn_refreshIn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_refreshInActionPerformed(evt);
            }
        });

        list_InDevices.setVisibleRowCount(6);
        list_InDevices.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_InDevicesValueChanged(evt);
            }
        });
        jScrollPane4.setViewportView(list_InDevices);

        javax.swing.GroupLayout pnl_inDeviceLayout = new javax.swing.GroupLayout(pnl_inDevice);
        pnl_inDevice.setLayout(pnl_inDeviceLayout);
        pnl_inDeviceLayout.setHorizontalGroup(
            pnl_inDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_inDeviceLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_inDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4)
                    .addGroup(pnl_inDeviceLayout.createSequentialGroup()
                        .addComponent(lbl_midiInNote)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_inNote)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 106, Short.MAX_VALUE)
                        .addComponent(btn_refreshIn)))
                .addContainerGap())
        );
        pnl_inDeviceLayout.setVerticalGroup(
            pnl_inDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_inDeviceLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_inDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_refreshIn)
                    .addComponent(lbl_midiInNote)
                    .addComponent(lbl_inNote))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cb_midiThru)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnl_inDevice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnl_remoteControl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_remoteControl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnl_inDevice, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cb_midiThru))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cb_enableRemoteControlActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_enableRemoteControlActionPerformed
    {//GEN-HEADEREND:event_cb_enableRemoteControlActionPerformed
        valueChanged(null);
        controller.applyChanges();
        controller.changed();
    }//GEN-LAST:event_cb_enableRemoteControlActionPerformed

    private void btn_learnActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_learnActionPerformed
    {//GEN-HEADEREND:event_btn_learnActionPerformed
        var ra = list_actions.getSelectedValue();
        if (ra == null)
        {
            return;
        }

        // Disable remote control while learning
        var rc = RemoteController.getInstance();
        boolean saveEnabled = rc.isEnabled();
        rc.setEnabled(false);
        if (!ra.startMidiLearnSession(MIDI_LEARN_TIME_OUT_MS))
        {
            String msg = "Nothing was received on Midi input";
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
        } else
        {
            LOGGER.log(Level.INFO, "btn_learnActionPerformed() Midi learn session success - ra={0}", ra.toString());
            valueChanged(null);
        }
        rc.setEnabled(saveEnabled);
    }//GEN-LAST:event_btn_learnActionPerformed

    private void btn_resetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_resetActionPerformed
    {//GEN-HEADEREND:event_btn_resetActionPerformed
        var ra = list_actions.getSelectedValue();
        if (ra != null)
        {
            ra.reset();
            valueChanged(null);
        }
    }//GEN-LAST:event_btn_resetActionPerformed

    private void btn_refreshInActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_refreshInActionPerformed
    {//GEN-HEADEREND:event_btn_refreshInActionPerformed
        MidiDevice save = list_InDevices.getSelectedValue();
        list_InDevices.rescanMidiDevices();
        if (save != null)
        {
            list_InDevices.setSelectedValue(save, true);
        }
    }//GEN-LAST:event_btn_refreshInActionPerformed

    private void list_InDevicesValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_InDevicesValueChanged
    {//GEN-HEADEREND:event_list_InDevicesValueChanged
        if (loadInProgress || evt.getValueIsAdjusting())
        {
            return;
        }
        lbl_inNote.setText(NO_INCOMING_NOTE);
        enableRemoteControlUI(list_InDevices.getSelectedValue() != null);
        controller.applyChanges();
        controller.changed();
    }//GEN-LAST:event_list_InDevicesValueChanged

    private void cb_midiThruActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_midiThruActionPerformed
    {//GEN-HEADEREND:event_cb_midiThruActionPerformed
        controller.applyChanges();
        controller.changed();
    }//GEN-LAST:event_cb_midiThruActionPerformed



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_learn;
    private javax.swing.JButton btn_refreshIn;
    private javax.swing.JButton btn_reset;
    private javax.swing.JCheckBox cb_enableRemoteControl;
    private javax.swing.JCheckBox cb_midiThru;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JLabel lbl_inNote;
    private javax.swing.JLabel lbl_midiInNote;
    private javax.swing.JLabel lbl_midiMessages;
    private org.jjazz.midi.api.ui.MidiInDeviceList list_InDevices;
    private javax.swing.JList<RemoteAction> list_actions;
    private javax.swing.JPanel pnl_inDevice;
    private javax.swing.JPanel pnl_remoteControl;
    private javax.swing.JTextField tf_midiMessages;
    // End of variables declaration//GEN-END:variables


    private void enableRemoteControlUI(boolean b)
    {
        org.jjazz.uiutilities.api.UIUtilities.setRecursiveEnabled(b, pnl_remoteControl);
        if (b)
        {
            cb_enableRemoteControlActionPerformed(null);
        }
        lbl_inNote.setEnabled(b);
        lbl_midiInNote.setEnabled(b);
    }

    private String getNoteString(int pitch)
    {
        return new Note(pitch).toPianoOctaveString() + " (" + pitch + ")";
    }

    /**
     * Get a string representing the Midi Messages.
     * <p>
     * If MidiMessages represent a single Note, return a special string.
     *
     * @param messages
     * @return
     */
    private String getMidiMessageString(List<MidiMessage> messages)
    {
        var mm0 = messages.get(0);
        if (messages.size() == 1)
        {
            var sm0 = MidiUtilities.getNoteOnShortMessage(mm0);
            if (sm0 != null)
            {
                return getNoteString(sm0.getData1()) + " on channel " + sm0.getChannel();
            }
        }


        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var mm : messages)
        {
            if (!first)
            {
                sb.append(", ");
            }
            for (byte b : mm.getMessage())
            {
                int bi = (int) (b & 0xFF);
                sb.append(String.format("%02X", bi));
                sb.append(" ");
            }
            first = false;
        }
        return sb.toString();
    }

    private void saveRemoteActions(List<RemoteAction> remoteActions)
    {
        saveMidiMessages.clear();
        for (var ra : remoteActions)
        {
            saveMidiMessages.add(ra.getMidiMessages());
        }
    }

    private void restoreRemoteActions(List<RemoteAction> remoteActions)
    {
        int i = 0;
        for (var ra : remoteActions)
        {
            ra.setMidiMessages(saveMidiMessages.get(i));
            i++;
        }
    }


    // ===========================================================================================
    // Private classes
    // ===========================================================================================
    private class RemoteActionRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            JComponent jc = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            RemoteAction ra = (RemoteAction) value;
            String txt = (String) ra.getAction().getValue(Action.NAME);
            setText(txt);
            return jc;
        }
    }

    private class LastNoteDisplayer implements Receiver, ActionListener
    {

        Timer t = new Timer(1000, this);

        @Override
        public void send(MidiMessage msg, long timeStamp)
        {
            if (msg instanceof ShortMessage)
            {
                ShortMessage sm = (ShortMessage) msg;
                if (sm.getCommand() == ShortMessage.NOTE_ON && MidiInPanel.this.isShowing())
                {
                    lbl_inNote.setText(getNoteString(sm.getData1()));
                    t.restart();
                }
            }
        }

        @Override
        public void close()
        {
            // Nothing
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            lbl_inNote.setText(NO_INCOMING_NOTE);
        }

    }

}
