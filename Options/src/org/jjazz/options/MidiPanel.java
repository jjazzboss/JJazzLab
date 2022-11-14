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

import org.jjazz.outputsynth.api.ui.DefaultInstrumentsDialog;
import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.embeddedsynth.api.EmbeddedSynth;
import org.jjazz.embeddedsynth.spi.EmbeddedSynthProvider;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.ui.MidiOutDeviceList;
import org.jjazz.outputsynth.api.MidiSynthManager;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.outputsynth.api.OutputSynth.UserSettings;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.testplayerservice.spi.TestPlayer;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.*;
import org.openide.windows.WindowManager;

final class MidiPanel extends javax.swing.JPanel implements PropertyChangeListener
{

    private final ComboMidiSynthModel comboModel;
    private final MidiOptionsPanelController controller;
    private OutputSynth editedOutputSynth;
    private String saveOutDeviceNameForCancel;
    private int saveOutDeviceIndexBeforeJJSynth = 0;
    private final EmbeddedSynth embeddedSynth;


    /**
     * Used as a map key for mapDeviceSynth.
     * <p>
     * Using MidiDevice name because a OUT MidiDevice might appear/disappear/re-appear on the system.
     */
    private record MidiDeviceSynth(String midiDeviceName, MidiSynth midiSynth)
            {

    }
    /**
     * Keep one OutputSynth instance per MidiDevice/MidiSynth pair.
     */
    private final Map<MidiDeviceSynth, OutputSynth> mapDeviceSynth = new HashMap<>();


    private static final Logger LOGGER = Logger.getLogger(MidiPanel.class.getSimpleName());

    MidiPanel(MidiOptionsPanelController controller)
    {
        this.controller = controller;


        comboModel = new ComboMidiSynthModel();
        initComponents();       // Use comboModel                     

        updateMapDeviceSynth();


        // Listen to added/removed loaded MidiSynths
        var msm = MidiSynthManager.getInstance();
        msm.addPropertyChangeListener(this);


        // Prepare for the embedded synth if present
        embeddedSynth = EmbeddedSynthProvider.getDefault();

        if (embeddedSynth == null)
        {
            // Disable UI for EmbeddedSynth
            cb_usejjSynth.setSelected(false);
            Utilities.setRecursiveEnabled(false, pnl_jjSynth);
        }
        String tooltip = embeddedSynth != null ? ResUtil.getString(getClass(), "MidiPanel.jjSynthPanelToolTip")
                : ResUtil.getString(getClass(), "MidiPanel.jjSynthPanelToolTipDisabled");
        pnl_jjSynth.setToolTipText(tooltip);
        cb_usejjSynth.setToolTipText(tooltip);
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

        // Save current Midi OUT for cancel
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        var mdOut = jms.getDefaultOutDevice();
        saveOutDeviceNameForCancel = mdOut == null ? null : mdOut.getDeviceInfo().getName();
        LOGGER.log(Level.FINE, "load() saveOutDeviceNameForCancel=" + saveOutDeviceNameForCancel);

        // Restore UI        
        boolean isEmbeddedSynthUsed = embeddedSynth != null && jms.getDefaultOutDevice() == embeddedSynth.getOutMidiDevice();
        if (embeddedSynth != null)
        {
            updateUseEmbeddedSynthUI(isEmbeddedSynthUsed);
        }
        if (embeddedSynth == null || !isEmbeddedSynthUsed)
        {
            list_OutDevices.setSelectedValue(getOutDevice(saveOutDeviceNameForCancel), true); // trigger event for our listeners
            btn_test.setEnabled(saveOutDeviceNameForCancel != null);
        }


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


        // Nothing is stored: changes are performed directly
        MidiDevice outDevice = cb_usejjSynth.isSelected() ? embeddedSynth.getOutMidiDevice() : list_OutDevices.getSelectedValue();
        if (outDevice != null && !outDevice.getDeviceInfo().getName().equals(saveOutDeviceNameForCancel))
        {
            Analytics.setProperties(Analytics.buildMap("Midi Out", outDevice.getDeviceInfo().getName()));
        }


    }

    public void cancel()
    {
        var mdOut = getOutDevice(saveOutDeviceNameForCancel);
        openOutDevice(mdOut);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {

        if (evt.getSource() == MidiSynthManager.getInstance())
        {
            if (evt.getPropertyName().equals(MidiSynthManager.PROP_FILE_BASED_MIDISYNTH_LIST))
            {
                if (evt.getNewValue() != null)
                {
                    // A new MidiSynth was added
                    assert evt.getOldValue() == null;
                    comboModel.addMidiSynth((MidiSynth) evt.getNewValue());
                    updateMapDeviceSynth();

                } else if (evt.getOldValue() != null)
                {
                    // A MidiSynth was removed
                    assert evt.getNewValue() == null;
                    comboModel.removeMidiSynth((MidiSynth) evt.getOldValue());
                }
            }
        } else if (editedOutputSynth != null && evt.getSource() == editedOutputSynth.getUserSettings())
        {
            if (evt.getPropertyName().equals(UserSettings.PROP_AUDIO_LATENCY))
            {
                spn_audioLatency.setValue(evt.getNewValue());
            } else if (evt.getPropertyName().equals(UserSettings.PROP_SEND_MODE_ON_UPON_PLAY))
            {
                combo_sendMessageUponPlay.setSelectedItem(evt.getNewValue());
            }
        }
    }

    // ===================================================================================================================
    // Private methods
    // ===================================================================================================================
    /**
     * Set the currently edited OutputSynth.
     *
     * @param outSynth
     */
    private void setEditedOutputSynth(OutputSynth outSynth)
    {
        if (editedOutputSynth == outSynth)
        {
            return;
        }

        if (editedOutputSynth != null)
        {
            editedOutputSynth.getUserSettings().removePropertyChangeListener(this);
        }

        editedOutputSynth = outSynth;


        Utilities.setRecursiveEnabled(editedOutputSynth != null, pnl_outputSynth);


        if (editedOutputSynth != null)
        {
            editedOutputSynth.getUserSettings().addPropertyChangeListener(this);        // Register for changes

            // Update UI
            MidiSynth mSynth = editedOutputSynth.getMidiSynth();
            combo_midiSynths.setSelectedItem(mSynth);
            spn_audioLatency.setValue(editedOutputSynth.getUserSettings().getAudioLatency());
            combo_sendMessageUponPlay.setSelectedItem(editedOutputSynth.getUserSettings().getSendModeOnUponPlay());
            // tbl_instruments.getModel().setInstruments(mSynth.getInstruments());
        }
    }

    /**
     * Update mapDeviceSynth to reflect the available OUT MidiDevices and the available MidiSynths.
     */
    private void updateMapDeviceSynth()
    {
        // Make sure there is an OutputSynth for all current OUT MidiDevices, and that MidiSynthManager is up-to-date
        var osm = OutputSynthManager.getInstance();
        osm.refresh();


        // Get the updated list of MidiDevices
        var mdOuts = list_OutDevices.getOutDevices();


        // Make sure there is an OutputSynth for each MidiDeviceOUT-MidiSynth pair
        var msm = MidiSynthManager.getInstance();
        for (var mdOut : mdOuts)
        {

            if (embeddedSynth != null && embeddedSynth.getOutMidiDevice() == mdOut)
            {
                // Don't need an entry for the embeddedSynth' MidiDevice
                continue;
            }

            var mdDefaultOutSynth = osm.getOutputSynth(mdOut.getDeviceInfo().getName());       // The current default OutputSynth for mdOut


            List<OutputSynth> outSynthList = getNewBuiltinOutputSynths();
            for (var outSynth : outSynthList)
            {
                // Add each new OutputSynth if not already done 
                // Note that 2 MidiSynths are equal if they have the same list of MidiSynths.
                var midiSynth = outSynth.getMidiSynth();
                var mdsKey = new MidiDeviceSynth(mdOut.getDeviceInfo().getName(), midiSynth);
                var outSynth2 = (midiSynth == mdDefaultOutSynth.getMidiSynth()) ? mdDefaultOutSynth : outSynth;
                mapDeviceSynth.putIfAbsent(mdsKey, outSynth2);
            }


            for (var midiSynth : msm.getMidiSynths(false, true))
            {
                // Add each new OutputSynth if not already done 
                // Note that 2 MidiSynths are equal if they have the same list of MidiSynths.                
                var mdsKey = new MidiDeviceSynth(mdOut.getDeviceInfo().getName(), midiSynth);
                var outSynth = (midiSynth == mdDefaultOutSynth.getMidiSynth()) ? mdDefaultOutSynth : new OutputSynth(midiSynth);
                mapDeviceSynth.putIfAbsent(mdsKey, outSynth);
            }
        }
    }

    /**
     * Get a list of builtin OutputSynths to show for each OUT MidiDevice.
     *
     * @return
     */
    private List<OutputSynth> getNewBuiltinOutputSynths()
    {
        // Create OutputSynths for the builtin synths
        var osm = OutputSynthManager.getInstance();
        var outSynthList = Arrays.asList(osm.getNewGMOuputSynth(),
                osm.getNewGM2OuputSynth(),
                osm.getNewXGOuputSynth(),
                osm.getNewGSOuputSynth(),
                osm.getNewYamahaRefOuputSynth(),
                osm.getNewJazzLabSoundFontXGOuputSynth(),
                osm.getNewJazzLabSoundFontGSOuputSynth());
        return outSynthList;
    }

    /**
     * Get an OUT MidiDevice from its name.
     *
     * @param mdName
     * @return
     */
    private MidiDevice getOutDevice(String mdName)
    {
        return JJazzMidiSystem.getInstance().getOutDeviceList()
                .stream()
                .filter(md -> md.getDeviceInfo().getName().equals(mdName))
                .findAny()
                .orElse(null);
    }

    boolean valid()
    {
        // LOGGER.log(Level.INFO, "valid()");
        // TODO check whether form is consistent and complete
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
        class MyRunnable implements Runnable
        {

            private Exception exception = null;

            @Override
            public void run()
            {
                try
                {
                    JJazzMidiSystem.getInstance().setDefaultOutDevice(mdOut);
                } catch (MidiUnavailableException ex)
                {
                    exception = ex;
                }
            }
        };
        MyRunnable r = new MyRunnable();


        if (embeddedSynth != null && embeddedSynth.getOutMidiDevice() == mdOut)
        {
            // It's the embedded synth, this can take a few seconds
            BaseProgressUtils.showProgressDialogAndRun(r, ResUtil.getString(getClass(), "MidiPanel.LoadingJJSynth"));
        } else
        {
            r.run();
        }

        if (r.exception != null)
        {
            String msg = ResUtil.getString(getClass(), "ERR_DeviceProblem", mdOut.getDeviceInfo().getName());
            msg += "\n\n" + r.exception.getLocalizedMessage();
            LOGGER.log(Level.WARNING, msg);   //NOI18N
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return false;
        }

        return true;
    }

    /**
     * Create a MidiDeviceOutList which does not contain the EmbeddedSynth (if present).
     */
    private MidiOutDeviceList buildMidiOutDeviceList()
    {
        EmbeddedSynth embeddedSynth = EmbeddedSynthProvider.getDefault();
        Predicate<MidiDevice> tester = (embeddedSynth == null) ? md -> true : md -> !md.getDeviceInfo().getName().equals(embeddedSynth.getOutMidiDevice().getDeviceInfo().getName());
        return new MidiOutDeviceList(tester);
    }

    /**
     * Propose user to load a custom Instrument definition file (.ins).
     */
    private void addCustomMidiSynth()
    {
        var msm = MidiSynthManager.getInstance();
        File f = msm.showSelectSynthFileDialog();
        if (f == null)
        {
            return;
        }

        // Retrieve or create the new OutputSynth from the file
        MidiSynth midiSynth = msm.getMidiSynth(f.getName());
        if (midiSynth == null)
        {
            try
            {
                midiSynth = MidiSynth.loadFromFile(f);
            } catch (IOException ex)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return;
            }

            // Register the new loaded MidiSynth
            // This will notify our listener to update the comboBox and call updateMapDeviceSynth() to create the related OutputSynth
            msm.addFileBasedMidiSynth(midiSynth);

        }


        // Select the new multisynth
        // This must be done on the EDT : if we call setSelectedItem() directly, JComboBox action listeners are NOT
        // notified because we are already running from a ComboBox action listener (the one triggered by "Add custom synth...")
        // which is not terminated yet.
        final var ms = midiSynth;
        SwingUtilities.invokeLater(() -> combo_midiSynths.setSelectedItem(ms));

    }

    private void updateUseEmbeddedSynthUI(boolean use)
    {
        cb_usejjSynth.setSelected(use);
        btn_jjSynthSettings.setEnabled(use);
        Utilities.setRecursiveEnabled(!use, pnl_outDevice);
        Utilities.setRecursiveEnabled(!use, pnl_outputSynth);
    }


    /**
     * Send a few notes to the current Midi OUT device and OutputSynth.
     */
    private void sendTestNotes(Instrument ins)
    {
        this.btn_test.setEnabled(false);
        this.btn_refresh.setEnabled(false);
        this.list_OutDevices.setEnabled(false);
        Utilities.setRecursiveEnabled(false, pnl_outputSynth);
        // tbl_instruments.setEnabled(false);
        Runnable endAction = new Runnable()
        {
            @Override
            public void run()
            {
                // Called when sequence is stopped
                btn_test.setEnabled(true);
                btn_refresh.setEnabled(true);
                list_OutDevices.setEnabled(true);
                Utilities.setRecursiveEnabled(true, pnl_outputSynth);
                // tbl_instruments.setEnabled(true);
            }
        };

        TestPlayer tp = TestPlayer.getDefault();
        try
        {
            int channel = MidiConst.CHANNEL_MIN;
            if (ins != null)
            {
                JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(channel));
            }
            tp.playTestNotes(channel, -1, 0, endAction);
        } catch (MusicGenerationException ex)
        {
            endAction.run();
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }
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
        jScrollPane5 = new javax.swing.JScrollPane();
        instrumentTable1 = new org.jjazz.midi.api.ui.InstrumentTable();
        jScrollPane6 = new javax.swing.JScrollPane();
        instrumentTable2 = new org.jjazz.midi.api.ui.InstrumentTable();
        pnl_outputSynth = new javax.swing.JPanel();
        combo_midiSynths = new JComboBox(comboModel);
        combo_sendMessageUponPlay = new JComboBox<>(UserSettings.SendModeOnUponPlay.values());
        spn_audioLatency = new org.jjazz.ui.utilities.api.WheelSpinner();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        btn_defaultInstruments = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.api.HelpTextArea();
        pnl_outDevice = new javax.swing.JPanel();
        btn_refresh = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        list_OutDevices = buildMidiOutDeviceList();
        btn_test = new javax.swing.JButton();
        pnl_jjSynth = new javax.swing.JPanel();
        cb_usejjSynth = new javax.swing.JCheckBox();
        btn_jjSynthSettings = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        jScrollPane1.setViewportView(midiInDeviceList1);

        jScrollPane5.setViewportView(instrumentTable1);

        jScrollPane6.setViewportView(instrumentTable2);

        pnl_outputSynth.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.pnl_outputSynth.border.title"))); // NOI18N

        combo_midiSynths.setRenderer(new MidiSynthComboBoxRenderer());
        combo_midiSynths.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                combo_midiSynthsActionPerformed(evt);
            }
        });

        combo_sendMessageUponPlay.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                combo_sendMessageUponPlayActionPerformed(evt);
            }
        });

        spn_audioLatency.setModel(new javax.swing.SpinnerNumberModel(5, 0, 1000, 5));
        spn_audioLatency.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.spn_audioLatency.toolTipText")); // NOI18N
        spn_audioLatency.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_audioLatencyStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.jLabel3.text")); // NOI18N
        jLabel3.setToolTipText(spn_audioLatency.getToolTipText());

        org.openide.awt.Mnemonics.setLocalizedText(btn_defaultInstruments, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_defaultInstruments.text")); // NOI18N
        btn_defaultInstruments.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_defaultInstruments.toolTipText")); // NOI18N
        btn_defaultInstruments.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_defaultInstrumentsActionPerformed(evt);
            }
        });

        jScrollPane2.setBackground(null);
        jScrollPane2.setBorder(null);

        helpTextArea1.setBackground(null);
        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(3);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.helpTextArea1.text")); // NOI18N
        jScrollPane2.setViewportView(helpTextArea1);

        javax.swing.GroupLayout pnl_outputSynthLayout = new javax.swing.GroupLayout(pnl_outputSynth);
        pnl_outputSynth.setLayout(pnl_outputSynthLayout);
        pnl_outputSynthLayout.setHorizontalGroup(
            pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_outputSynthLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(combo_midiSynths, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnl_outputSynthLayout.createSequentialGroup()
                        .addGroup(pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnl_outputSynthLayout.createSequentialGroup()
                                .addComponent(combo_sendMessageUponPlay, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2))
                            .addGroup(pnl_outputSynthLayout.createSequentialGroup()
                                .addComponent(spn_audioLatency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3))
                            .addComponent(btn_defaultInstruments))
                        .addGap(0, 99, Short.MAX_VALUE))
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );
        pnl_outputSynthLayout.setVerticalGroup(
            pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_outputSynthLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(combo_midiSynths, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(33, 33, 33)
                .addGroup(pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(combo_sendMessageUponPlay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(18, 18, 18)
                .addGroup(pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spn_audioLatency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(18, 18, 18)
                .addComponent(btn_defaultInstruments)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 73, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pnl_outDevice.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.pnl_outDevice.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_refresh, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_refresh.text")); // NOI18N
        btn_refresh.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_refresh.toolTipText")); // NOI18N
        btn_refresh.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_refreshActionPerformed(evt);
            }
        });

        list_OutDevices.setVisibleRowCount(6);
        list_OutDevices.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_OutDevicesValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(list_OutDevices);

        btn_test.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/options/resources/SpeakerRed-20x20.png"))); // NOI18N
        btn_test.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_test.toolTipText")); // NOI18N
        btn_test.setDisabledIcon(GeneralUISettings.getInstance().getIcon("speaker.icon.disabled"));
        btn_test.setEnabled(false);
        btn_test.setMargin(new java.awt.Insets(2, 4, 2, 4));
        btn_test.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_testActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_outDeviceLayout = new javax.swing.GroupLayout(pnl_outDevice);
        pnl_outDevice.setLayout(pnl_outDeviceLayout);
        pnl_outDeviceLayout.setHorizontalGroup(
            pnl_outDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_outDeviceLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_outDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_outDeviceLayout.createSequentialGroup()
                        .addComponent(btn_test)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_refresh)))
                .addContainerGap())
        );
        pnl_outDeviceLayout.setVerticalGroup(
            pnl_outDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_outDeviceLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_outDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_refresh, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btn_test, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        pnl_jjSynth.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.pnl_jjSynth.border.title"))); // NOI18N

        cb_usejjSynth.setFont(cb_usejjSynth.getFont().deriveFont(cb_usejjSynth.getFont().getStyle() | java.awt.Font.BOLD));
        org.openide.awt.Mnemonics.setLocalizedText(cb_usejjSynth, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.cb_usejjSynth.text")); // NOI18N
        cb_usejjSynth.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_usejjSynthActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_jjSynthSettings, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_jjSynthSettings.text")); // NOI18N
        btn_jjSynthSettings.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_jjSynthSettings.toolTipText")); // NOI18N
        btn_jjSynthSettings.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_jjSynthSettingsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_jjSynthLayout = new javax.swing.GroupLayout(pnl_jjSynth);
        pnl_jjSynth.setLayout(pnl_jjSynthLayout);
        pnl_jjSynthLayout.setHorizontalGroup(
            pnl_jjSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_jjSynthLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cb_usejjSynth)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_jjSynthSettings)
                .addContainerGap())
        );
        pnl_jjSynthLayout.setVerticalGroup(
            pnl_jjSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_jjSynthLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(pnl_jjSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cb_usejjSynth, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_jjSynthSettings))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/options/resources/ArrowRightBig.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_jjSynth, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnl_outDevice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnl_outputSynth, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnl_jjSynth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnl_outDevice, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(pnl_outputSynth, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(40, 40, 40)
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cb_usejjSynthActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_usejjSynthActionPerformed
    {//GEN-HEADEREND:event_cb_usejjSynthActionPerformed
        boolean b = cb_usejjSynth.isSelected();
        if (b)
        {
            saveOutDeviceIndexBeforeJJSynth = Math.max(0, list_OutDevices.getSelectedIndex());  // Use 0=Java Internal Synth if no selection
            openOutDevice(embeddedSynth.getOutMidiDevice());
        } else
        {
            assert list_OutDevices.getModel().getSize() > 0;
            if (saveOutDeviceIndexBeforeJJSynth >= list_OutDevices.getModel().getSize())
            {
                // Some MidiDevices have disappeared after a refresh
                saveOutDeviceIndexBeforeJJSynth = 0;
            }
            list_OutDevices.setSelectedValue(null, true);   // Make sure next selection will notify our listener
            list_OutDevices.setSelectedIndex(saveOutDeviceIndexBeforeJJSynth);
        }

        updateUseEmbeddedSynthUI(b);
    }//GEN-LAST:event_cb_usejjSynthActionPerformed

    private void btn_testActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_testActionPerformed
    {//GEN-HEADEREND:event_btn_testActionPerformed
        LOGGER.log(Level.FINE, "Testing {0}", list_OutDevices.getSelectedValue().getDeviceInfo().getName());   //NOI18N
        // sendTestNotes(tbl_instruments.getSelectedInstrument());
        sendTestNotes(null);
    }//GEN-LAST:event_btn_testActionPerformed

    private void btn_refreshActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_refreshActionPerformed
    {//GEN-HEADEREND:event_btn_refreshActionPerformed
        MidiDevice save = list_OutDevices.getSelectedValue();
        list_OutDevices.rescanMidiDevices();
        if (save != null)
        {
            list_OutDevices.setSelectedValue(save, true);
        } else
        {
            list_OutDevices.setSelectedIndex(0);        // Java Internal Synth
        }
        updateMapDeviceSynth();
    }//GEN-LAST:event_btn_refreshActionPerformed

    private void list_OutDevicesValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_OutDevicesValueChanged
    {//GEN-HEADEREND:event_list_OutDevicesValueChanged
        if (evt.getValueIsAdjusting())
        {
            return;
        }
        MidiDevice md = list_OutDevices.getSelectedValue();

        // Update UI
        btn_test.setEnabled(md != null);
        String mdName = (md == null) ? "" : md.getDeviceInfo().getName();
        String friendlyMdName = (md == null) ? "" : JJazzMidiSystem.getInstance().getDeviceFriendlyName(md);
        ((TitledBorder) pnl_outputSynth.getBorder()).setTitle(ResUtil.getString(getClass(), "MidiPanel.OutputSynthFor", friendlyMdName));
        pnl_outputSynth.repaint();  // Needed for the title renaming to be visible immediatly


        // Update state
        setEditedOutputSynth(md != null ? OutputSynthManager.getInstance().getOutputSynth(mdName) : null);
        openOutDevice(md);

    }//GEN-LAST:event_list_OutDevicesValueChanged

    private void combo_sendMessageUponPlayActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_combo_sendMessageUponPlayActionPerformed
    {//GEN-HEADEREND:event_combo_sendMessageUponPlayActionPerformed
        if (editedOutputSynth == null)
        {
            return;
        }
        editedOutputSynth.getUserSettings().setSendModeOnUponPlay((UserSettings.SendModeOnUponPlay) combo_sendMessageUponPlay.getSelectedItem());

    }//GEN-LAST:event_combo_sendMessageUponPlayActionPerformed

    private void spn_audioLatencyStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_audioLatencyStateChanged
    {//GEN-HEADEREND:event_spn_audioLatencyStateChanged
        if (editedOutputSynth == null)
        {
            return;
        }
        editedOutputSynth.getUserSettings().setAudioLatency((int) spn_audioLatency.getValue());
    }//GEN-LAST:event_spn_audioLatencyStateChanged

    private void combo_midiSynthsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_combo_midiSynthsActionPerformed
    {//GEN-HEADEREND:event_combo_midiSynthsActionPerformed
        var mdOut = list_OutDevices.getSelectedValue();
        if (mdOut == null)
        {
            // Should not be there as the OutputSynth should be disabled...
            return;
        }
        MidiSynth mSynth = (MidiSynth) combo_midiSynths.getSelectedItem();
        if (mSynth == ComboMidiSynthModel.FAKE_MULTISYNTH)
        {
            // Special case
            addCustomMidiSynth();
            return;
        }

        OutputSynth outSynth = mapDeviceSynth.get(new MidiDeviceSynth(mdOut.getDeviceInfo().getName(), mSynth));
        OutputSynthManager.getInstance().setOutputSynth(mdOut.getDeviceInfo().getName(), outSynth);
        setEditedOutputSynth(outSynth);
    }//GEN-LAST:event_combo_midiSynthsActionPerformed

    private void btn_defaultInstrumentsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_defaultInstrumentsActionPerformed
    {//GEN-HEADEREND:event_btn_defaultInstrumentsActionPerformed

        var dialog = new DefaultInstrumentsDialog(editedOutputSynth.getUserSettings().getGMRemapTable());
        dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dialog.setVisible(true);
    }//GEN-LAST:event_btn_defaultInstrumentsActionPerformed

    private void btn_jjSynthSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_jjSynthSettingsActionPerformed
    {//GEN-HEADEREND:event_btn_jjSynthSettingsActionPerformed
        if (embeddedSynth != null)
        {
            embeddedSynth.showSettings();
        }
    }//GEN-LAST:event_btn_jjSynthSettingsActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_defaultInstruments;
    private javax.swing.JButton btn_jjSynthSettings;
    private javax.swing.JButton btn_refresh;
    private javax.swing.JButton btn_test;
    private javax.swing.JCheckBox cb_usejjSynth;
    private javax.swing.JComboBox<MidiSynth> combo_midiSynths;
    private javax.swing.JComboBox<UserSettings.SendModeOnUponPlay> combo_sendMessageUponPlay;
    private org.jjazz.ui.utilities.api.HelpTextArea helpTextArea1;
    private org.jjazz.midi.api.ui.InstrumentTable instrumentTable1;
    private org.jjazz.midi.api.ui.InstrumentTable instrumentTable2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private org.jjazz.midi.api.ui.MidiOutDeviceList list_OutDevices;
    private org.jjazz.midi.api.ui.MidiInDeviceList midiInDeviceList1;
    private javax.swing.JPanel pnl_jjSynth;
    private javax.swing.JPanel pnl_outDevice;
    private javax.swing.JPanel pnl_outputSynth;
    private org.jjazz.ui.utilities.api.WheelSpinner spn_audioLatency;
    // End of variables declaration//GEN-END:variables

    // ===================================================================================================================
    // Inner classes
    // ===================================================================================================================

    /**
     * The model for the JComboBox: use all the available MidiSynths + an extra "Add synth from file..."
     */
    private class ComboMidiSynthModel extends DefaultComboBoxModel<MidiSynth>
    {

        // A dummy MidiSynth used only to create the extra "Add synth from file..." at the end
        protected static final MidiSynth FAKE_MULTISYNTH = new MidiSynth(ResUtil.getString(MidiPanel.class, "MidiPanel.AddSynthFromFile"), "");

        public ComboMidiSynthModel()
        {
            OutputSynthManager.getInstance().refresh();
            addAll(getNewBuiltinOutputSynths().stream()
                    .map(outSynth -> outSynth.getMidiSynth())
                    .toList());
            addAll(MidiSynthManager.getInstance().getMidiSynths(false, true));
            addElement(FAKE_MULTISYNTH);
        }

        public void addMidiSynth(MidiSynth mSynth)
        {
            insertElementAt(mSynth, getSize() - 1); // Add before the FAKE_MULTISYNTH
        }

        public void removeMidiSynth(MidiSynth mSynth)
        {
            int index = getIndexOf(mSynth);
            assert index > -1;
            removeElementAt(index);
        }
    }

    class MidiSynthComboBoxRenderer extends DefaultListCellRenderer
    {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MidiSynth midiSynth)
            {
                label.setText(midiSynth.getName());
                int style = (value == ComboMidiSynthModel.FAKE_MULTISYNTH) ? Font.BOLD : Font.PLAIN;
                label.setFont(label.getFont().deriveFont(style));
            }
            return this;
        }
    }

}
