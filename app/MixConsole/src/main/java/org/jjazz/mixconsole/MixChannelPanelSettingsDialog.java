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
package org.jjazz.mixconsole;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.uiutilities.api.UIUtilities;
import org.openide.windows.WindowManager;

/**
 * The settings dialog for one MixChannel.
 * <p>
 * Because settings and drumsrerouting are interdependent, we can't have a
 * simple dialog where data is retrieved only when dialog is closed. Dialog must
 * directly update and listen to the model changes.
 */
public class MixChannelPanelSettingsDialog extends javax.swing.JDialog implements PropertyChangeListener
{

    private static MixChannelPanelSettingsDialog INSTANCE;
    private MidiMix midiMix;
    private InstrumentMix insMix;
    private InstrumentSettings insSet;
    private int channel;
    private InstrumentMix saveInsMix;
    private boolean saveDrumsRerouting;
    private static final Logger LOGGER = Logger.getLogger(MixChannelPanelSettingsDialog.class.getSimpleName());

    public static MixChannelPanelSettingsDialog getInstance()
    {
        synchronized (MixChannelPanelSettingsDialog.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new MixChannelPanelSettingsDialog(WindowManager.getDefault().getMainWindow(), true);
                INSTANCE.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
            }
        }
        return INSTANCE;
    }

    /**
     * Creates new form MixChannelPanelSettings
     */
    private MixChannelPanelSettingsDialog(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();
    }

    /**
     * Initialize the dialog for the specified MidiMix channel.
     *
     * @param mm
     * @param channel
     * @param title
     */
    public void preset(MidiMix mm, int channel, String title)
    {
        if (mm == null || !MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("mm=" + mm + " channel=" + channel + " title=" + title);   
        }
        midiMix = mm;
        insMix = mm.getInstrumentMix(channel);
        insSet = insMix.getSettings();
        this.channel = channel;

        // For cancel operation
        saveInsMix = new InstrumentMix(insMix);
        saveDrumsRerouting = midiMix.getDrumsReroutedChannels().contains(channel);

        registerModel();

        lbl_title.setText(title);
        RhythmVoice rv = mm.getRhythmVoice(channel);
        if (channel != MidiConst.CHANNEL_DRUMS && (rv.isDrums() || rv instanceof UserRhythmVoice))
        {
            // Enable drums rerouting
            UIUtilities.setRecursiveEnabled(true, pnl_rerouting);
        } else if (saveDrumsRerouting)
        {
            // Check consistency of the model
            throw new IllegalStateException("Drums rerouting is ON though it should not. channel=" + channel + " rv=" + rv + " mm=" + mm);   
        } else
        {
            // Drums rerouting not available for this channel
            UIUtilities.setRecursiveEnabled(false, pnl_rerouting);
        }

        updateUI();
    }

    /**
     * Overridden to add global key bindings
     *
     * @return
     */
    @Override
    protected JRootPane createRootPane()
    {
        JRootPane contentPane = new JRootPane();
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "actionOk");   
        contentPane.getActionMap().put("actionOk", new AbstractAction("OK")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                btn_okActionPerformed(null);
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "actionCancel");   
        contentPane.getActionMap().put("actionCancel", new AbstractAction("Cancel")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                btn_cancelActionPerformed(null);
            }
        });
        return contentPane;
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertyChangeListener interface
    //-----------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() == insMix && null != e.getPropertyName())
        {
            switch (e.getPropertyName())
            {
                case InstrumentMix.PROP_INSTRUMENT_ENABLED:
                    updateUI();
                    break;
                default:
                // Nothing
                }
        } else if (e.getSource() == insSet && null != e.getPropertyName())
        {
            switch (e.getPropertyName())
            {
                case InstrumentSettings.PROPERTY_PANORAMIC_ENABLED:
                case InstrumentSettings.PROPERTY_REVERB_ENABLED:
                case InstrumentSettings.PROPERTY_CHORUS_ENABLED:
                case InstrumentSettings.PROPERTY_VOLUME_ENABLED:
                case InstrumentSettings.PROPERTY_VELOCITY_SHIFT:
                    updateUI();
                    break;
                default:
                // Nothing
            }
        } else if (e.getSource() == midiMix && null != e.getPropertyName())
        {
            if (e.getPropertyName() == MidiMix.PROP_CHANNEL_DRUMS_REROUTED)
            {
                int ch = (int) e.getOldValue();
                if (ch == channel)
                {
                    updateUI();
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // Private methods
    //-----------------------------------------------------------------------
    private void updateUI()
    {
        cb_chorus.setSelected(insSet.isChorusEnabled());
        cb_reverb.setSelected(insSet.isReverbEnabled());
        cb_panoramic.setSelected(insSet.isPanoramicEnabled());
        cb_volume.setSelected(insSet.isVolumeEnabled());
        cb_instrument.setSelected(insMix.isInstrumentEnabled());
        boolean b = midiMix.getDrumsReroutedChannels().contains(channel);
        cb_drumsRerouting.setSelected(b);
        cb_volume.setEnabled(!b);
        cb_reverb.setEnabled(!b);
        cb_chorus.setEnabled(!b);
        cb_panoramic.setEnabled(!b);
        cb_instrument.setEnabled(!b);
        spn_velocityShift.setValue(Integer.valueOf(insSet.getVelocityShift()));
    }

    private void registerModel()
    {
        midiMix.addPropertyChangeListener(this);
        insMix.addPropertyChangeListener(this);
        insSet.addPropertyChangeListener(this);
    }

    private void unregisterModel()
    {
        midiMix.removePropertyChangeListener(this);
        insMix.removePropertyChangeListener(this);
        insSet.removePropertyChangeListener(this);
        midiMix = null;
        insMix = null;
        insSet = null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        lbl_title = new javax.swing.JLabel();
        pnl_enabledMidiMessages = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        cb_panoramic = new javax.swing.JCheckBox();
        cb_volume = new javax.swing.JCheckBox();
        cb_chorus = new javax.swing.JCheckBox();
        cb_reverb = new javax.swing.JCheckBox();
        cb_instrument = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.flatcomponents.api.HelpTextArea();
        btn_ok = new javax.swing.JButton();
        btn_cancel = new javax.swing.JButton();
        pnl_rerouting = new javax.swing.JPanel();
        cb_drumsRerouting = new javax.swing.JCheckBox();
        jScrollPane3 = new javax.swing.JScrollPane();
        helpTextArea3 = new org.jjazz.flatcomponents.api.HelpTextArea();
        spn_velocityShift = new org.jjazz.flatcomponents.api.WheelSpinner();
        lbl_velocityShift = new javax.swing.JLabel();

        setTitle(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.title")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosed(java.awt.event.WindowEvent evt)
            {
                formWindowClosed(evt);
            }
        });

        lbl_title.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_title, "Title"); // NOI18N

        pnl_enabledMidiMessages.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.pnl_enabledMidiMessages.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_panoramic, org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_panoramic.text")); // NOI18N
        cb_panoramic.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_panoramic.toolTipText")); // NOI18N
        cb_panoramic.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_panoramicActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_volume, org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_volume.text")); // NOI18N
        cb_volume.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_volume.toolTipText")); // NOI18N
        cb_volume.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_volumeActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_chorus, org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_chorus.text")); // NOI18N
        cb_chorus.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_chorus.toolTipText")); // NOI18N
        cb_chorus.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_chorusActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_reverb, org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_reverb.text")); // NOI18N
        cb_reverb.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_reverb.toolTipText")); // NOI18N
        cb_reverb.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_reverbActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_instrument, org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_instrument.text")); // NOI18N
        cb_instrument.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_instrument.toolTipText")); // NOI18N
        cb_instrument.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_instrumentActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cb_chorus)
                    .addComponent(cb_volume)
                    .addComponent(cb_panoramic)
                    .addComponent(cb_reverb)
                    .addComponent(cb_instrument))
                .addContainerGap(16, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cb_volume)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cb_panoramic)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cb_reverb)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cb_chorus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cb_instrument)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(5);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.helpTextArea1.text")); // NOI18N
        jScrollPane1.setViewportView(helpTextArea1);

        javax.swing.GroupLayout pnl_enabledMidiMessagesLayout = new javax.swing.GroupLayout(pnl_enabledMidiMessages);
        pnl_enabledMidiMessages.setLayout(pnl_enabledMidiMessagesLayout);
        pnl_enabledMidiMessagesLayout.setHorizontalGroup(
            pnl_enabledMidiMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_enabledMidiMessagesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                .addContainerGap())
        );
        pnl_enabledMidiMessagesLayout.setVerticalGroup(
            pnl_enabledMidiMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_enabledMidiMessagesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
            .addGroup(pnl_enabledMidiMessagesLayout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        org.openide.awt.Mnemonics.setLocalizedText(btn_ok, org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.btn_ok.text")); // NOI18N
        btn_ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_okActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_cancel, org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.btn_cancel.text")); // NOI18N
        btn_cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cancelActionPerformed(evt);
            }
        });

        pnl_rerouting.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.pnl_rerouting.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_drumsRerouting, org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.cb_drumsRerouting.text")); // NOI18N
        cb_drumsRerouting.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_drumsReroutingActionPerformed(evt);
            }
        });

        jScrollPane3.setBorder(null);

        helpTextArea3.setColumns(20);
        helpTextArea3.setRows(5);
        helpTextArea3.setText(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.helpTextArea3.text")); // NOI18N
        jScrollPane3.setViewportView(helpTextArea3);

        javax.swing.GroupLayout pnl_reroutingLayout = new javax.swing.GroupLayout(pnl_rerouting);
        pnl_rerouting.setLayout(pnl_reroutingLayout);
        pnl_reroutingLayout.setHorizontalGroup(
            pnl_reroutingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_reroutingLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_reroutingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(pnl_reroutingLayout.createSequentialGroup()
                        .addComponent(cb_drumsRerouting)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnl_reroutingLayout.setVerticalGroup(
            pnl_reroutingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_reroutingLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cb_drumsRerouting)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                .addContainerGap())
        );

        spn_velocityShift.setModel(new javax.swing.SpinnerNumberModel(0, -64, 64, 1));
        spn_velocityShift.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.spn_velocityShift.toolTipText")); // NOI18N
        spn_velocityShift.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_velocityShiftStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_velocityShift, org.openide.util.NbBundle.getMessage(MixChannelPanelSettingsDialog.class, "MixChannelPanelSettingsDialog.lbl_velocityShift.text")); // NOI18N
        lbl_velocityShift.setToolTipText(spn_velocityShift.getToolTipText());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_ok)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_cancel))
                    .addComponent(pnl_enabledMidiMessages, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnl_rerouting, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_title)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(spn_velocityShift, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_velocityShift, javax.swing.GroupLayout.PREFERRED_SIZE, 218, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_cancel, btn_ok});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_title)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spn_velocityShift, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_velocityShift))
                .addGap(18, 18, 18)
                .addComponent(pnl_enabledMidiMessages, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnl_rerouting, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_ok)
                    .addComponent(btn_cancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

   private void btn_okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_okActionPerformed
   {//GEN-HEADEREND:event_btn_okActionPerformed
       unregisterModel();
       setVisible(false);
   }//GEN-LAST:event_btn_okActionPerformed

   private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cancelActionPerformed
   {//GEN-HEADEREND:event_btn_cancelActionPerformed
       // Restore previous state
       if (midiMix != null)
       {
           // method can be called with midiMix=null in special circustamces like when switching to full-screen mode
           midiMix.setDrumsReroutedChannel(saveDrumsRerouting, channel);
           insMix.setInstrumentEnabled(saveInsMix.isInstrumentEnabled());
           InstrumentSettings saveInsSet = saveInsMix.getSettings();
           insSet.setChorusEnabled(saveInsSet.isChorusEnabled());
           insSet.setReverbEnabled(saveInsSet.isReverbEnabled());
           insSet.setVolumeEnabled(saveInsSet.isVolumeEnabled());
           insSet.setPanoramicEnabled(saveInsSet.isPanoramicEnabled());
           insSet.setVelocityShift(saveInsSet.getVelocityShift());
           insSet.setTransposition(saveInsSet.getTransposition());
           unregisterModel();
       }
       setVisible(false);
   }//GEN-LAST:event_btn_cancelActionPerformed

   private void cb_drumsReroutingActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_drumsReroutingActionPerformed
   {//GEN-HEADEREND:event_cb_drumsReroutingActionPerformed
       midiMix.setDrumsReroutedChannel(cb_drumsRerouting.isSelected(), channel);
   }//GEN-LAST:event_cb_drumsReroutingActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosed
    {//GEN-HEADEREND:event_formWindowClosed

        btn_cancelActionPerformed(null);
    }//GEN-LAST:event_formWindowClosed

    private void cb_volumeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_volumeActionPerformed
    {//GEN-HEADEREND:event_cb_volumeActionPerformed
        insSet.setVolumeEnabled(this.cb_volume.isSelected());
    }//GEN-LAST:event_cb_volumeActionPerformed

    private void cb_panoramicActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_panoramicActionPerformed
    {//GEN-HEADEREND:event_cb_panoramicActionPerformed
        insSet.setPanoramicEnabled(this.cb_panoramic.isSelected());
    }//GEN-LAST:event_cb_panoramicActionPerformed

    private void cb_reverbActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_reverbActionPerformed
    {//GEN-HEADEREND:event_cb_reverbActionPerformed
        insSet.setReverbEnabled(this.cb_reverb.isSelected());
    }//GEN-LAST:event_cb_reverbActionPerformed

    private void cb_chorusActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_chorusActionPerformed
    {//GEN-HEADEREND:event_cb_chorusActionPerformed
        insSet.setChorusEnabled(this.cb_chorus.isSelected());
    }//GEN-LAST:event_cb_chorusActionPerformed

    private void cb_instrumentActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_instrumentActionPerformed
    {//GEN-HEADEREND:event_cb_instrumentActionPerformed
        insMix.setInstrumentEnabled(this.cb_instrument.isSelected());
    }//GEN-LAST:event_cb_instrumentActionPerformed

    private void spn_velocityShiftStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_velocityShiftStateChanged
    {//GEN-HEADEREND:event_spn_velocityShiftStateChanged
        insSet.setVelocityShift((Integer) spn_velocityShift.getValue());
    }//GEN-LAST:event_spn_velocityShiftStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_cancel;
    private javax.swing.JButton btn_ok;
    private javax.swing.JCheckBox cb_chorus;
    private javax.swing.JCheckBox cb_drumsRerouting;
    private javax.swing.JCheckBox cb_instrument;
    private javax.swing.JCheckBox cb_panoramic;
    private javax.swing.JCheckBox cb_reverb;
    private javax.swing.JCheckBox cb_volume;
    private org.jjazz.flatcomponents.api.HelpTextArea helpTextArea1;
    private org.jjazz.flatcomponents.api.HelpTextArea helpTextArea3;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lbl_title;
    private javax.swing.JLabel lbl_velocityShift;
    private javax.swing.JPanel pnl_enabledMidiMessages;
    private javax.swing.JPanel pnl_rerouting;
    private org.jjazz.flatcomponents.api.WheelSpinner spn_velocityShift;
    // End of variables declaration//GEN-END:variables
}
