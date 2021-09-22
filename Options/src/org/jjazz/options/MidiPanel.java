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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.UserChannelRvKey;
import org.jjazz.musiccontrol.api.TestPlayer;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.ui.musiccontrolactions.api.RemoteAction;
import org.jjazz.ui.musiccontrolactions.api.RemoteController;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.util.api.ResUtil;
import org.openide.*;
import org.openide.windows.WindowManager;

final class MidiPanel extends javax.swing.JPanel implements ListSelectionListener
{

    private static final String NO_INCOMING_NOTE = "-";
    private static final int MIDI_LEARN_TIME_OUT_MS = 4000;
    private final MidiOptionsPanelController controller;
    private boolean loadInProgress;
    private MidiDevice saveOutDevice;      // For cancel operation
    private MidiDevice saveInDevice;       // For cancel operation
    private boolean saveMidiThru;          // For cancel operation
    private boolean saveRemoteControlEnabled;          // For cancel operation
    private final List<List<MidiMessage>> saveMidiMessages = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(MidiPanel.class.getSimpleName());

    MidiPanel(MidiOptionsPanelController controller)
    {
        this.controller = controller;
        initComponents();

        list_actions.addListSelectionListener(this);
        list_actions.setCellRenderer(new RemoteActionRenderer());

        btn_test.setEnabled(false);
        spn_preferredUserChannel.addChangeListener(cl -> controller.changed());

        JJazzMidiSystem.getInstance().getJJazzMidiInDevice().getTransmitter().setReceiver(new LastNoteDisplayer());
    }

    void load()
    {
        LOGGER.log(Level.FINE, "load() --");   //NOI18N
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
        LOGGER.log(Level.FINE, "load() saveOutDevice=" + saveOutDevice + " .info=" + ((saveOutDevice == null) ? "null" : saveOutDevice.getDeviceInfo()));   //NOI18N
        list_OutDevices.setSelectedValue(saveOutDevice, true);
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

        btn_test.setEnabled(saveOutDevice != null);

        // Soundbank enabled only if Out device is a synth
        boolean b = (saveOutDevice instanceof Synthesizer);
        org.jjazz.ui.utilities.api.Utilities.setRecursiveEnabled(b, pnl_soundbankFile);
        updateSoundbankText();

        spn_preferredUserChannel.setValue(UserChannelRvKey.getInstance().getPreferredUserChannel() + 1);

        loadInProgress = false;
    }

    public void cancel()
    {
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        jms.setThruMode(saveMidiThru);
        openInDevice(saveInDevice);
        openOutDevice(saveOutDevice);

        RemoteController rc = RemoteController.getInstance();
        rc.setEnabled(saveRemoteControlEnabled);
        restoreRemoteActions(RemoteController.getInstance().getRemoteActions());
    }

    void store()
    {
        LOGGER.log(Level.FINE, "store() --");   //NOI18N
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(MidiPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(MidiPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        jms.setThruMode(cb_midiThru.isSelected());
        MidiDevice inDevice = list_InDevices.getSelectedValue();
        openInDevice(inDevice);
        MidiDevice outDevice = list_OutDevices.getSelectedValue();
        openOutDevice(outDevice);
        UserChannelRvKey.getInstance().setPreferredUserChannel(((Integer) spn_preferredUserChannel.getValue()) - 1);

        RemoteController rc = RemoteController.getInstance();
        rc.setEnabled(cb_enableRemoteControl.isSelected());
        for (RemoteAction ra: rc.getRemoteActions())
        {
            ra.saveAsPreference();
        }


        if (outDevice != saveOutDevice)
        {
            Analytics.setProperties(Analytics.buildMap("Midi Out", outDevice.getDeviceInfo().getName()));
        }
        if (cb_enableRemoteControl.isSelected() != saveRemoteControlEnabled)
        {
            Analytics.logEvent("Remote Control", Analytics.buildMap("Enabled", cb_enableRemoteControl.isSelected()));
        }
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
            String msg = ResUtil.getString(getClass(), "ERR_DeviceProblem", mdIn.getDeviceInfo().getName());
            msg += "\n\n" + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, msg);   //NOI18N
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
            String msg = ResUtil.getString(getClass(), "ERR_DeviceProblem", mdOut.getDeviceInfo().getName());
            msg += "\n\n" + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, msg);   //NOI18N
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

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jScrollPane1 = new javax.swing.JScrollPane();
        midiInDeviceList1 = new org.jjazz.midi.api.ui.MidiInDeviceList();
        lbl_OutDevices = new javax.swing.JLabel();
        cb_midiThru = new javax.swing.JCheckBox();
        lbl_InDevices = new javax.swing.JLabel();
        btn_test = new javax.swing.JButton();
        pnl_soundbankFile = new javax.swing.JPanel();
        txtf_soundbankFile = new javax.swing.JTextField();
        btn_changeSoundbankFile = new javax.swing.JButton();
        btn_resetSoundbank = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        list_OutDevices = new org.jjazz.midi.api.ui.MidiOutDeviceList();
        btn_refresh = new javax.swing.JButton();
        spn_preferredUserChannel = new org.jjazz.ui.utilities.api.WheelSpinner();
        lbl_preferredUserChannel = new javax.swing.JLabel();
        btn_refreshIn = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        list_InDevices = new org.jjazz.midi.api.ui.MidiInDeviceList();
        pnl_remoteControl = new javax.swing.JPanel();
        cb_enableRemoteControl = new javax.swing.JCheckBox();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_actions = new javax.swing.JList<>();
        btn_learn = new javax.swing.JButton();
        lbl_midiMessages = new javax.swing.JLabel();
        btn_reset = new javax.swing.JButton();
        tf_midiMessages = new javax.swing.JTextField();
        lbl_inNote = new javax.swing.JLabel();
        lbl_midiInNote = new javax.swing.JLabel();

        jScrollPane1.setViewportView(midiInDeviceList1);

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

        org.openide.awt.Mnemonics.setLocalizedText(lbl_InDevices, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.lbl_InDevices.text")); // NOI18N

        btn_test.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/options/resources/SpeakerRed-20x20.png"))); // NOI18N
        btn_test.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_test.toolTipText")); // NOI18N
        btn_test.setDisabledIcon(GeneralUISettings.getInstance().getIcon("speaker.icon.disabled"));
        btn_test.setMargin(new java.awt.Insets(2, 4, 2, 4));
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
                .addComponent(txtf_soundbankFile, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
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

        spn_preferredUserChannel.setModel(new javax.swing.SpinnerNumberModel(1, 1, 16, 1));
        spn_preferredUserChannel.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.spn_preferredUserChannel.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_preferredUserChannel, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.lbl_preferredUserChannel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_refreshIn, btn_refresh.getText());
        btn_refreshIn.setToolTipText(org.openide.util.NbBundle.getBundle(MidiPanel.class).getString("MidiPanel.btn_refreshIn.toolTipText")); // NOI18N
        btn_refreshIn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_refreshInActionPerformed(evt);
            }
        });

        list_InDevices.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_InDevicesValueChanged(evt);
            }
        });
        jScrollPane4.setViewportView(list_InDevices);

        pnl_remoteControl.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getBundle(MidiPanel.class).getString("MidiPanel.pnl_remoteControl.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_enableRemoteControl, org.openide.util.NbBundle.getBundle(MidiPanel.class).getString("MidiPanel.cb_enableRemoteControl.text")); // NOI18N
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

        org.openide.awt.Mnemonics.setLocalizedText(btn_learn, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_learn.text")); // NOI18N
        btn_learn.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_learn.toolTipText")); // NOI18N
        btn_learn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_learnActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_midiMessages, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.lbl_midiMessages.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_reset, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_reset.text")); // NOI18N
        btn_reset.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_reset.toolTipText")); // NOI18N
        btn_reset.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_resetActionPerformed(evt);
            }
        });

        tf_midiMessages.setEditable(false);
        tf_midiMessages.setText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.tf_midiMessages.text")); // NOI18N
        tf_midiMessages.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.tf_midiMessages.toolTipText")); // NOI18N

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
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                                        .addGap(0, 17, Short.MAX_VALUE)))
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
                            .addComponent(btn_reset)))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.openide.awt.Mnemonics.setLocalizedText(lbl_inNote, "C4"); // NOI18N
        lbl_inNote.setToolTipText(lbl_midiInNote.getText());

        org.openide.awt.Mnemonics.setLocalizedText(lbl_midiInNote, org.openide.util.NbBundle.getBundle(MidiPanel.class).getString("MidiPanel.lbl_midiInNote.text")); // NOI18N
        lbl_midiInNote.setToolTipText(org.openide.util.NbBundle.getBundle(MidiPanel.class).getString("MidiPanel.lbl_midiInNote.toolTipText")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(btn_test)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_refresh))
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 334, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lbl_OutDevices, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(spn_preferredUserChannel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_preferredUserChannel))
                    .addComponent(cb_midiThru)
                    .addComponent(pnl_soundbankFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_midiInNote)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_inNote)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_refreshIn))
                    .addComponent(jScrollPane4)
                    .addComponent(pnl_remoteControl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lbl_InDevices, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_OutDevices)
                    .addComponent(lbl_InDevices))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_refresh)
                        .addComponent(lbl_midiInNote)
                        .addComponent(lbl_inNote)
                        .addComponent(btn_refreshIn))
                    .addComponent(btn_test))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnl_soundbankFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(cb_midiThru)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spn_preferredUserChannel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbl_preferredUserChannel)))
                    .addComponent(pnl_remoteControl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cb_midiThruActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_midiThruActionPerformed
    {//GEN-HEADEREND:event_cb_midiThruActionPerformed
        controller.applyChanges();
        controller.changed();
    }//GEN-LAST:event_cb_midiThruActionPerformed

    private void btn_testActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_testActionPerformed
    {//GEN-HEADEREND:event_btn_testActionPerformed
        LOGGER.log(Level.FINE, "Testing {0}", list_OutDevices.getSelectedValue().getDeviceInfo().getName());   //NOI18N
        sendTestNotes();
    }//GEN-LAST:event_btn_testActionPerformed

   private void btn_changeSoundbankFileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_changeSoundbankFileActionPerformed
   {//GEN-HEADEREND:event_btn_changeSoundbankFileActionPerformed
       JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
       JFileChooser chooser = org.jjazz.ui.utilities.api.Utilities.getFileChooserInstance();
       FileNameExtensionFilter filter = new FileNameExtensionFilter(".sf2, .dls files ", "sf2", "dls", "SF2", "DLS");
       chooser.resetChoosableFileFilters();
       chooser.setMultiSelectionEnabled(false);
       chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
       chooser.setFileFilter(filter);
       chooser.setDialogTitle(ResUtil.getString(getClass(), "LoadSoundBankDialogTitle"));
       File previousFile = jms.getDefaultJavaSynthPreferredSoundFontFile();
       if (previousFile == null)
       {
           chooser.setCurrentDirectory(null);       // System user directory
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
           String msg = ResUtil.getString(getClass(), "ERR_SynthSoundFileProblem", f.getAbsolutePath());
           NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
           DialogDisplayer.getDefault().notify(d);
       }

       Analytics.logEvent("Load SoundBank File", Analytics.buildMap("File", f.getName()));

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
        org.jjazz.ui.utilities.api.Utilities.setRecursiveEnabled(b, pnl_soundbankFile);
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

    private void cb_enableRemoteControlActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_enableRemoteControlActionPerformed
    {//GEN-HEADEREND:event_cb_enableRemoteControlActionPerformed
        boolean b = cb_enableRemoteControl.isSelected();
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

        if (!ra.startMidiLearnSession(MIDI_LEARN_TIME_OUT_MS))
        {
            String msg = "Nothing was received on Midi input";
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
        } else
        {
            valueChanged(null);
        }
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


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_changeSoundbankFile;
    private javax.swing.JButton btn_learn;
    private javax.swing.JButton btn_refresh;
    private javax.swing.JButton btn_refreshIn;
    private javax.swing.JButton btn_reset;
    private javax.swing.JButton btn_resetSoundbank;
    private javax.swing.JButton btn_test;
    private javax.swing.JCheckBox cb_enableRemoteControl;
    private javax.swing.JCheckBox cb_midiThru;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JLabel lbl_InDevices;
    private javax.swing.JLabel lbl_OutDevices;
    private javax.swing.JLabel lbl_inNote;
    private javax.swing.JLabel lbl_midiInNote;
    private javax.swing.JLabel lbl_midiMessages;
    private javax.swing.JLabel lbl_preferredUserChannel;
    private org.jjazz.midi.api.ui.MidiInDeviceList list_InDevices;
    private org.jjazz.midi.api.ui.MidiOutDeviceList list_OutDevices;
    private javax.swing.JList<RemoteAction> list_actions;
    private org.jjazz.midi.api.ui.MidiInDeviceList midiInDeviceList1;
    private javax.swing.JPanel pnl_remoteControl;
    private javax.swing.JPanel pnl_soundbankFile;
    private org.jjazz.ui.utilities.api.WheelSpinner spn_preferredUserChannel;
    private javax.swing.JTextField tf_midiMessages;
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
        this.list_OutDevices.setEnabled(false);
        Runnable endAction = new Runnable()
        {
            @Override
            public void run()
            {
                // Called when sequence is stopped
                btn_test.setEnabled(true);
                btn_refresh.setEnabled(true);
                list_OutDevices.setEnabled(true);
            }
        };

        TestPlayer tp = TestPlayer.getInstance();
        try
        {
            tp.playTestNotes(MidiConst.CHANNEL_MIN, -1, 0, endAction);
        } catch (MusicGenerationException ex)
        {
            endAction.run();
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }
    }

    private void enableRemoteControlUI(boolean b)
    {
        org.jjazz.ui.utilities.api.Utilities.setRecursiveEnabled(b, pnl_remoteControl);
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
                if (sm.getCommand() == ShortMessage.NOTE_ON && MidiPanel.this.isShowing())
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
