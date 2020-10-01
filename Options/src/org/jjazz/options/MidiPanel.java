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
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.midi.MidiConst;
import static org.jjazz.options.Bundle.*;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midimix.UserChannelRvKey;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.musiccontrol.TestPlayer;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.ui.utilities.Utilities;
import org.jjazz.uisettings.GeneralUISettings;
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
    private boolean saveEnableRecording;   // For cancel operation    
    private int saveLatency;   // For cancel operation
    private static final Logger LOGGER = Logger.getLogger(MidiPanel.class.getSimpleName());

    MidiPanel(MidiOptionsPanelController controller)
    {
        this.controller = controller;
        initComponents();

        btn_test.setEnabled(false);
        spn_preferredUserChannel.addChangeListener(cl -> controller.changed());

        JJazzMidiSystem.getInstance().getJJazzMidiInDevice().getTransmitter().setReceiver(new LedReceiver());

    }

    public void load()
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
        LOGGER.log(Level.FINE, "load() saveInDevice=" + saveInDevice + " .info=" + ((saveInDevice == null) ? "null" : saveInDevice.getDeviceInfo()));
        list_OutDevices.setSelectedValue(saveOutDevice, true);
        list_InDevices.setSelectedValue(saveInDevice, true);


        // Other stuff
        saveMidiThru = jms.isThruMode();
        cb_midiThru.setSelected(saveMidiThru);

        btn_test.setEnabled(saveOutDevice != null);

        // RecordingEnabled
        saveEnableRecording = jms.isRecordingEnabled();
        cb_enableMidiInput.setSelected(saveEnableRecording);
        cb_enableMidiInputActionPerformed(null);
        saveLatency = jms.getOutLatency();
        spn_latency.setValue(Integer.valueOf(saveLatency));


        // Soundbank enabled only if Out device is a synth
        boolean b = (saveOutDevice instanceof Synthesizer);
        org.jjazz.ui.utilities.Utilities.setRecursiveEnabled(b, pnl_soundbankFile);
        updateSoundbankText();

        spn_preferredUserChannel.setValue(UserChannelRvKey.getInstance().getPreferredUserChannel() + 1);

        loadInProgress = false;
    }

    public void store()
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
        MidiDevice inDevice = list_InDevices.getSelectedValue();
        openInDevice(inDevice);
        MidiDevice outDevice = list_OutDevices.getSelectedValue();
        LOGGER.log(Level.FINE, "store() outDevice=" + outDevice + " .info=" + ((outDevice == null) ? "null" : outDevice.getDeviceInfo()));
        openOutDevice(outDevice);
        UserChannelRvKey.getInstance().setPreferredUserChannel(((Integer) spn_preferredUserChannel.getValue()) - 1);
        jms.setRecordingEnabled(cb_enableMidiInput.isSelected());
        jms.setOutLatency((Integer) spn_latency.getValue());
    }

    public void cancel()
    {
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        jms.setThruMode(saveMidiThru);
        openInDevice(saveInDevice);
        openOutDevice(saveOutDevice);
        jms.setOutLatency(saveLatency);
        jms.setRecordingEnabled(saveEnableRecording);
    }

    public boolean valid()
    {
        // LOGGER.log(Level.INFO, "valid()");
        // TODO check whether form is consistent and complete
        return true;
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================
    private void updateLatencyUI()
    {
        boolean b = list_InDevices.getSelectedIndex() != -1 && list_OutDevices.getSelectedIndex() != -1;
        spn_latency.setEnabled(b);
        btn_autoAdjustLatency.setEnabled(b);
    }

    /**
     * Set the default int device to mdOut.
     *
     * @param mdIn Can be null.
     * @return False if there was an error.
     */
    private boolean openInDevice(MidiDevice mdIn)
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
    private boolean openOutDevice(MidiDevice mdOut)
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
        this.btn_refreshOut.setEnabled(false);
        this.list_OutDevices.setEnabled(false);
        Runnable endAction = new Runnable()
        {
            @Override
            public void run()
            {
                // Called when sequence is stopped
                btn_test.setEnabled(true);
                btn_refreshOut.setEnabled(true);
                list_OutDevices.setEnabled(true);
            }
        };

        TestPlayer tp = TestPlayer.getInstance();
        try
        {
            tp.playTestNotes(MidiConst.CHANNEL_MIN, -1, 0, endAction);
        } catch (MusicGenerationException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }
    }

    // ===========================================================================================
    // Private classes
    // ===========================================================================================
    private class LedReceiver implements Receiver
    {

        @Override
        public void send(MidiMessage msg, long timeStamp)
        {            
            if (msg instanceof ShortMessage && MidiPanel.this.isShowing())
            {
                ShortMessage sm = (ShortMessage) msg;
                if (sm.getCommand() == ShortMessage.NOTE_ON)
                {
                    // LOGGER.severe("LedReceiver.send() test NOTE_ON received");
                    led_MidiIn.showActivity();
                }
            }
        }

        @Override
        public void close()
        {
            // Nothing
        }

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        cb_midiThru = new javax.swing.JCheckBox();
        spn_preferredUserChannel = new org.jjazz.ui.utilities.WheelSpinner();
        lbl_preferredUserChannel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        btn_test = new javax.swing.JButton();
        btn_refreshOut = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        list_OutDevices = new org.jjazz.midi.ui.MidiOutDeviceList();
        pnl_soundbankFile = new javax.swing.JPanel();
        btn_changeSoundbankFile = new javax.swing.JButton();
        btn_resetSoundbank = new javax.swing.JButton();
        txtf_soundbankFile = new javax.swing.JTextField();
        pnl_MidiIN = new javax.swing.JPanel();
        led_MidiIn = new org.jjazz.ui.flatcomponents.FlatLedIndicator();
        jScrollPane2 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.HelpTextArea();
        btn_autoAdjustLatency = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        list_InDevices = new org.jjazz.midi.ui.MidiInDeviceList();
        jLabel1 = new javax.swing.JLabel();
        btn_refreshIn = new javax.swing.JButton();
        spn_latency = new org.jjazz.ui.utilities.WheelSpinner();
        jScrollPane4 = new javax.swing.JScrollPane();
        helpTextArea2 = new org.jjazz.ui.utilities.HelpTextArea();
        cb_enableMidiInput = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(cb_midiThru, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.cb_midiThru.text")); // NOI18N
        cb_midiThru.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.cb_midiThru.toolTipText")); // NOI18N
        cb_midiThru.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_midiThruActionPerformed(evt);
            }
        });

        spn_preferredUserChannel.setModel(new javax.swing.SpinnerNumberModel(1, 1, 16, 1));
        spn_preferredUserChannel.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.spn_preferredUserChannel.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_preferredUserChannel, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.lbl_preferredUserChannel.text")); // NOI18N
        lbl_preferredUserChannel.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.lbl_preferredUserChannel.toolTipText")); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.jPanel1.border.title"))); // NOI18N

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

        org.openide.awt.Mnemonics.setLocalizedText(btn_refreshOut, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_refreshOut.text")); // NOI18N
        btn_refreshOut.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_refreshOut.toolTipText")); // NOI18N
        btn_refreshOut.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_refreshOutActionPerformed(evt);
            }
        });

        list_OutDevices.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_OutDevicesValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(list_OutDevices);

        pnl_soundbankFile.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.pnl_soundbankFile.border.title"))); // NOI18N

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

        txtf_soundbankFile.setEditable(false);
        txtf_soundbankFile.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.txtf_soundbankFile.toolTipText")); // NOI18N

        javax.swing.GroupLayout pnl_soundbankFileLayout = new javax.swing.GroupLayout(pnl_soundbankFile);
        pnl_soundbankFile.setLayout(pnl_soundbankFileLayout);
        pnl_soundbankFileLayout.setHorizontalGroup(
            pnl_soundbankFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_soundbankFileLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txtf_soundbankFile)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_changeSoundbankFile)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_resetSoundbank)
                .addContainerGap())
        );

        pnl_soundbankFileLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_changeSoundbankFile, btn_resetSoundbank});

        pnl_soundbankFileLayout.setVerticalGroup(
            pnl_soundbankFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_soundbankFileLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_soundbankFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_changeSoundbankFile)
                    .addComponent(btn_resetSoundbank)
                    .addComponent(txtf_soundbankFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btn_test)
                        .addGap(220, 220, 220)
                        .addComponent(btn_refreshOut))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_soundbankFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGap(6, 6, 6))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(pnl_soundbankFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_test)
                    .addComponent(btn_refreshOut))
                .addContainerGap())
        );

        pnl_MidiIN.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.pnl_MidiIN.border.title"))); // NOI18N

        led_MidiIn.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.led_MidiIn.toolTipText")); // NOI18N
        led_MidiIn.setDiameter(10);
        led_MidiIn.setLuminanceStepOnePeriod(10);

        jScrollPane2.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(3);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.helpTextArea1.text")); // NOI18N
        jScrollPane2.setViewportView(helpTextArea1);

        org.openide.awt.Mnemonics.setLocalizedText(btn_autoAdjustLatency, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_autoAdjustLatency.text")); // NOI18N
        btn_autoAdjustLatency.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_autoAdjustLatencyActionPerformed(evt);
            }
        });

        list_InDevices.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_InDevicesValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(list_InDevices);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_refreshIn, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_refreshIn.text")); // NOI18N
        btn_refreshIn.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_refreshIn.toolTipText")); // NOI18N
        btn_refreshIn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_refreshInActionPerformed(evt);
            }
        });

        spn_latency.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1500, 1));
        spn_latency.setColumns(4);

        jScrollPane4.setBackground(null);
        jScrollPane4.setBorder(null);

        helpTextArea2.setBackground(null);
        helpTextArea2.setColumns(20);
        helpTextArea2.setRows(2);
        helpTextArea2.setText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.helpTextArea2.text")); // NOI18N
        jScrollPane4.setViewportView(helpTextArea2);

        javax.swing.GroupLayout pnl_MidiINLayout = new javax.swing.GroupLayout(pnl_MidiIN);
        pnl_MidiIN.setLayout(pnl_MidiINLayout);
        pnl_MidiINLayout.setHorizontalGroup(
            pnl_MidiINLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_MidiINLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_MidiINLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_MidiINLayout.createSequentialGroup()
                        .addGroup(pnl_MidiINLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(pnl_MidiINLayout.createSequentialGroup()
                                .addComponent(led_MidiIn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btn_refreshIn))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(pnl_MidiINLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnl_MidiINLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spn_latency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btn_autoAdjustLatency)
                                .addGap(0, 100, Short.MAX_VALUE))
                            .addGroup(pnl_MidiINLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jScrollPane2))))
                    .addComponent(jScrollPane4))
                .addContainerGap())
        );
        pnl_MidiINLayout.setVerticalGroup(
            pnl_MidiINLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_MidiINLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_MidiINLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pnl_MidiINLayout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnl_MidiINLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(led_MidiIn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_refreshIn, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(pnl_MidiINLayout.createSequentialGroup()
                        .addGroup(pnl_MidiINLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_autoAdjustLatency)
                            .addComponent(jLabel1)
                            .addComponent(spn_latency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.openide.awt.Mnemonics.setLocalizedText(cb_enableMidiInput, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.cb_enableMidiInput.text")); // NOI18N
        cb_enableMidiInput.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_enableMidiInputActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(pnl_MidiIN, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cb_midiThru)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(spn_preferredUserChannel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_preferredUserChannel))
                            .addComponent(cb_enableMidiInput))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cb_midiThru)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spn_preferredUserChannel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_preferredUserChannel))
                .addGap(21, 21, 21)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addComponent(cb_enableMidiInput)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_MidiIN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cb_midiThruActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_midiThruActionPerformed
    {//GEN-HEADEREND:event_cb_midiThruActionPerformed
        controller.applyChanges();
        controller.changed();
    }//GEN-LAST:event_cb_midiThruActionPerformed

    private void btn_testActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_testActionPerformed
    {//GEN-HEADEREND:event_btn_testActionPerformed
        LOGGER.log(Level.FINE, "Testing {0}", list_OutDevices.getSelectedValue().getDeviceInfo().getName());
        sendTestNotes();
    }//GEN-LAST:event_btn_testActionPerformed

   private void btn_changeSoundbankFileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_changeSoundbankFileActionPerformed
   {//GEN-HEADEREND:event_btn_changeSoundbankFileActionPerformed
       JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
       JFileChooser chooser = org.jjazz.ui.utilities.Utilities.getFileChooserInstance();
       FileNameExtensionFilter filter = new FileNameExtensionFilter(".sf2, .dls files ", "sf2", "dls", "SF2", "DLS");
       chooser.resetChoosableFileFilters();
       chooser.setMultiSelectionEnabled(false);
       chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
       chooser.setFileFilter(filter);
       chooser.setDialogTitle("Load sound bank file");
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
        updateLatencyUI();
        controller.applyChanges();
        controller.changed();
    }//GEN-LAST:event_list_OutDevicesValueChanged

    private void btn_refreshOutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_refreshOutActionPerformed
    {//GEN-HEADEREND:event_btn_refreshOutActionPerformed
        MidiDevice save = list_OutDevices.getSelectedValue();
        list_OutDevices.rescanMidiDevices();
        if (save != null)
        {
            list_OutDevices.setSelectedValue(save, true);
        }
    }//GEN-LAST:event_btn_refreshOutActionPerformed

    private void list_InDevicesValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_InDevicesValueChanged
    {//GEN-HEADEREND:event_list_InDevicesValueChanged
        if (loadInProgress || evt.getValueIsAdjusting())
        {
            return;
        }
        MidiDevice md = list_InDevices.getSelectedValue();
        led_MidiIn.setEnabled(md != null);
        updateLatencyUI();
        controller.applyChanges();
        controller.changed();
    }//GEN-LAST:event_list_InDevicesValueChanged

    private void btn_autoAdjustLatencyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_autoAdjustLatencyActionPerformed
    {//GEN-HEADEREND:event_btn_autoAdjustLatencyActionPerformed
        // Stop music if it was playing
        var mc = MusicController.getInstance();
        mc.stop();

        LatencyDialog dlg = new LatencyDialog((Integer) spn_latency.getValue());
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
        int res = dlg.getLatency();
        if (res != -1)
        {
            spn_latency.setValue(Integer.valueOf(res));
            controller.applyChanges();
            controller.changed();
        }
        dlg.cleanup();
    }//GEN-LAST:event_btn_autoAdjustLatencyActionPerformed

    private void cb_enableMidiInputActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_enableMidiInputActionPerformed
    {//GEN-HEADEREND:event_cb_enableMidiInputActionPerformed
        var jms = JJazzMidiSystem.getInstance();
        boolean b = cb_enableMidiInput.isSelected();
        if (b)
        {
            list_InDevices.setSelectedValue(jms.getDefaultInDevice(), true);
        } else
        {
            list_InDevices.clearSelection();
        }
        Utilities.setRecursiveEnabled(b, pnl_MidiIN);
        updateLatencyUI();
        controller.applyChanges();
        controller.changed();
    }//GEN-LAST:event_cb_enableMidiInputActionPerformed

    private void btn_refreshInActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_refreshInActionPerformed
    {//GEN-HEADEREND:event_btn_refreshInActionPerformed
        MidiDevice save = list_InDevices.getSelectedValue();
        list_InDevices.rescanMidiDevices();
        if (save != null)
        {
            list_InDevices.setSelectedValue(save, true);
        }
    }//GEN-LAST:event_btn_refreshInActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_autoAdjustLatency;
    private javax.swing.JButton btn_changeSoundbankFile;
    private javax.swing.JButton btn_refreshIn;
    private javax.swing.JButton btn_refreshOut;
    private javax.swing.JButton btn_resetSoundbank;
    private javax.swing.JButton btn_test;
    private javax.swing.JCheckBox cb_enableMidiInput;
    private javax.swing.JCheckBox cb_midiThru;
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea1;
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JLabel lbl_preferredUserChannel;
    private org.jjazz.ui.flatcomponents.FlatLedIndicator led_MidiIn;
    private org.jjazz.midi.ui.MidiInDeviceList list_InDevices;
    private org.jjazz.midi.ui.MidiOutDeviceList list_OutDevices;
    private javax.swing.JPanel pnl_MidiIN;
    private javax.swing.JPanel pnl_soundbankFile;
    private org.jjazz.ui.utilities.WheelSpinner spn_latency;
    private org.jjazz.ui.utilities.WheelSpinner spn_preferredUserChannel;
    private javax.swing.JTextField txtf_soundbankFile;
    // End of variables declaration//GEN-END:variables

}
