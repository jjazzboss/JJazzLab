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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.musiccontrol.api.TestPlayer;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.api.Utilities;
import org.openide.*;
import org.openide.awt.Actions;
import org.openide.windows.WindowManager;

final class MidiPanel extends javax.swing.JPanel implements PropertyChangeListener
{

    private final MidiOptionsPanelController controller;
    private boolean loadInProgress;
    private MidiDevice saveOutDevice;      // For cancel operation
    private static final Logger LOGGER = Logger.getLogger(MidiPanel.class.getSimpleName());

    MidiPanel(MidiOptionsPanelController controller)
    {
        this.controller = controller;
        initComponents();


        btn_test.setEnabled(false);

        OutputSynthManager.getInstance().addPropertyChangeListener(this);

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

        loadInProgress = true; // To avoid calling controller.changed() via OutDevices change event handlers.           

        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();


        // Select default devices (can be null)
        saveOutDevice = jms.getDefaultOutDevice();
        LOGGER.log(Level.FINE, "load() saveOutDevice=" + saveOutDevice + " .info=" + ((saveOutDevice == null) ? "null" : saveOutDevice.getDeviceInfo()));   //NOI18N
        list_OutDevices.setSelectedValue(saveOutDevice, true);

        btn_test.setEnabled(saveOutDevice != null);

        loadInProgress = false;

        updateOutputSynthLabels();
    }

    public void cancel()
    {
        openOutDevice(saveOutDevice);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        var osm = OutputSynthManager.getInstance();
        if (evt.getSource() == osm
                && evt.getPropertyName().equals(OutputSynthManager.PROP_DEFAULT_OUTPUTSYNTH))
        {
            updateOutputSynthLabels();
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
        MidiDevice outDevice = list_OutDevices.getSelectedValue();
        openOutDevice(outDevice);

        if (outDevice != saveOutDevice && outDevice != null)
        {
            Analytics.setProperties(Analytics.buildMap("Midi Out", outDevice.getDeviceInfo().getName()));
        }
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


    private void updateOutputSynthLabels()
    {
        var outputSynth = OutputSynthManager.getInstance().getOutputSynth();
        String strStdBanks = outputSynth.getCompatibleStdBanks().stream()
                .map(b -> b.getName())
                .collect(Collectors.toList()).toString().replace("[", "").replace("]", "").replace(" Bank", "");
        String strSynths = outputSynth.getCustomSynths().toString().replace("[", "").replace("]", "");
        strSynths = Utilities.truncateWithDots(strSynths, 60);
        lbl_synths.setText(strSynths.isBlank() ? " " : strSynths);
        lbl_stdBanks.setText(strStdBanks.isBlank() ? " " : strStdBanks);
        lbl_audioLatencyValue.setText(outputSynth.getAudioLatency() + " ms");
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
        pnl_soundbankFile = new javax.swing.JPanel();
        txtf_soundbankFile = new javax.swing.JTextField();
        btn_changeSoundbankFile = new javax.swing.JButton();
        btn_resetSoundbank = new javax.swing.JButton();
        pnl_outputSynth = new javax.swing.JPanel();
        btn_outputSynthEditor = new javax.swing.JButton();
        lbl_synths = new javax.swing.JLabel();
        lbl_stdBanks = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.api.HelpTextArea();
        lbl_audioLatencyValue = new javax.swing.JLabel();
        lbl_audioLatency = new javax.swing.JLabel();
        pnl_outDevice = new javax.swing.JPanel();
        btn_test = new javax.swing.JButton();
        btn_refresh = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        list_OutDevices = new org.jjazz.midi.api.ui.MidiOutDeviceList();

        jScrollPane1.setViewportView(midiInDeviceList1);

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
                .addComponent(txtf_soundbankFile)
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

        pnl_outputSynth.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.pnl_outputSynth.border.title"))); // NOI18N

        btn_outputSynthEditor.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/options/resources/OutputSynth2.png"))); // NOI18N
        btn_outputSynthEditor.setToolTipText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.btn_outputSynthEditor.toolTipText")); // NOI18N
        btn_outputSynthEditor.setIconTextGap(0);
        btn_outputSynthEditor.setMargin(new java.awt.Insets(2, 4, 2, 4));
        btn_outputSynthEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_outputSynthEditorActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_synths, "jLabel1"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_stdBanks, "jLabel2"); // NOI18N

        jScrollPane2.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(3);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.helpTextArea1.text")); // NOI18N
        jScrollPane2.setViewportView(helpTextArea1);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_audioLatencyValue, "jLabel1"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_audioLatency, org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.lbl_audioLatency.text")); // NOI18N

        javax.swing.GroupLayout pnl_outputSynthLayout = new javax.swing.GroupLayout(pnl_outputSynth);
        pnl_outputSynth.setLayout(pnl_outputSynthLayout);
        pnl_outputSynthLayout.setHorizontalGroup(
            pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_outputSynthLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_outputSynthLayout.createSequentialGroup()
                        .addGroup(pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_outputSynthEditor)
                            .addComponent(lbl_synths))
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE))
                    .addGroup(pnl_outputSynthLayout.createSequentialGroup()
                        .addGroup(pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_stdBanks)
                            .addGroup(pnl_outputSynthLayout.createSequentialGroup()
                                .addComponent(lbl_audioLatency)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_audioLatencyValue)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnl_outputSynthLayout.setVerticalGroup(
            pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_outputSynthLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_outputSynthEditor)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(13, 13, 13)
                .addComponent(lbl_synths)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_stdBanks)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_outputSynthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_audioLatencyValue)
                    .addComponent(lbl_audioLatency))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        pnl_outDevice.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiPanel.class, "MidiPanel.pnl_outDevice.border.title"))); // NOI18N

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

        javax.swing.GroupLayout pnl_outDeviceLayout = new javax.swing.GroupLayout(pnl_outDevice);
        pnl_outDevice.setLayout(pnl_outDeviceLayout);
        pnl_outDeviceLayout.setHorizontalGroup(
            pnl_outDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_outDeviceLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_outDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 357, Short.MAX_VALUE)
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
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_outDeviceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_refresh, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btn_test, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnl_soundbankFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnl_outDevice, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_outputSynth, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnl_outputSynth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(pnl_outDevice, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(pnl_soundbankFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

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

    private void btn_outputSynthEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_outputSynthEditorActionPerformed
    {//GEN-HEADEREND:event_btn_outputSynthEditorActionPerformed
        Action a = Actions.forID("OutputSynth", "org.jjazz.outputsynth.ui.editoutputsynth");
        a.actionPerformed(null);
        updateOutputSynthLabels();
    }//GEN-LAST:event_btn_outputSynthEditorActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_changeSoundbankFile;
    private javax.swing.JButton btn_outputSynthEditor;
    private javax.swing.JButton btn_refresh;
    private javax.swing.JButton btn_resetSoundbank;
    private javax.swing.JButton btn_test;
    private org.jjazz.ui.utilities.api.HelpTextArea helpTextArea1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lbl_audioLatency;
    private javax.swing.JLabel lbl_audioLatencyValue;
    private javax.swing.JLabel lbl_stdBanks;
    private javax.swing.JLabel lbl_synths;
    private org.jjazz.midi.api.ui.MidiOutDeviceList list_OutDevices;
    private org.jjazz.midi.api.ui.MidiInDeviceList midiInDeviceList1;
    private javax.swing.JPanel pnl_outDevice;
    private javax.swing.JPanel pnl_outputSynth;
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


}
