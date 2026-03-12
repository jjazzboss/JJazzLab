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
package org.jjazz.fluidsynthembeddedsynth.api;

import com.google.common.base.Preconditions;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import org.jjazz.fluidsynthjava.api.Chorus;
import org.jjazz.fluidsynthjava.api.FluidSynthJava;
import org.jjazz.fluidsynthjava.api.Reverb;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.flatcomponents.api.FlatIntegerKnob;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.uiutilities.api.UIUtilities;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * A dialog to adjust the FluidSynth embedded synth settings.
 */
public class SettingsDialog extends javax.swing.JDialog implements PropertyChangeListener
{

    private final FluidSynthEmbeddedSynth fsEmbeddedSynth;
    private final int cancelLatency;
    private final float cancelGain;
    private final Reverb cancelReverb;
    private final Chorus cancelChorus;
    private final String cancelAudioDriver;
    private final OutputSynth.UserSettings outputSynthSettings;
    private FluidSynthJava fsJava;
    private static final Logger LOGGER = Logger.getLogger(SettingsDialog.class.getSimpleName());

    public SettingsDialog(FluidSynthEmbeddedSynth eSynth)
    {
        super(WindowManager.getDefault().getMainWindow(), true);
        Preconditions.checkNotNull(eSynth);
        fsEmbeddedSynth = eSynth;
        fsJava = eSynth.getFluidSynthJava();


        initComponents();
        list_audioDrivers.setCellRenderer(new MyCellRenderer());


        // FluidSynthJava data
        cancelGain = fsJava.getGain();
        cancelReverb = fsJava.getReverb();
        cancelChorus = fsJava.getChorus();
        knob_gain.setValue(fsGain2uiGain(cancelGain));
        lbl_gainValue.setText(String.format("%.1f", cancelGain));
        combo_chorus.setSelectedItem(cancelChorus);
        combo_reverb.setSelectedItem(cancelReverb);

        List<String> audioDriverOptions = fsJava.getSettings().getSettingPossibleStringValues("audio.driver");
        list_audioDrivers.setListData(audioDriverOptions.toArray(String[]::new));
        cancelAudioDriver = getAudioDriver();
        list_audioDrivers.setSelectedValue(getAudioDriver(), true);

        // Latency
        outputSynthSettings = fsEmbeddedSynth.getOutputSynth().getUserSettings();
        cancelLatency = outputSynthSettings.getAudioLatency();
        lbl_latencyValue.setText(cancelLatency + " ms");
        knob_latency.setValue(cancelLatency);

        // Listeners
        fsJava.addPropertyChangeListener(this);
        outputSynthSettings.addPropertyChangeListener(this);
        knob_latency.addPropertyChangeListener(FlatIntegerKnob.PROP_VALUE, this);
        knob_gain.addPropertyChangeListener(FlatIntegerKnob.PROP_VALUE, this);

        // Handle window closing 
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                btn_cancelActionPerformed(null);

            }

            @Override
            public void windowClosed(WindowEvent e)
            {
                fsJava.removePropertyChangeListener(SettingsDialog.this);
                outputSynthSettings.removePropertyChangeListener(SettingsDialog.this);
            }
        });

        UIUtilities.installEnterKeyAction(this, () -> btn_okActionPerformed(null));
        UIUtilities.installEscapeKeyAction(this, () -> btn_cancelActionPerformed(null));

        setLocationByPlatform(true);
    }


    // ==============================================================================
    // PropertyChangeListener interface
    // ==============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == fsJava)
        {
            switch (evt.getPropertyName())
            {
                case FluidSynthJava.PROP_CHORUS ->
                    combo_chorus.setSelectedItem(fsJava.getChorus());
                case FluidSynthJava.PROP_REVERB ->
                    combo_reverb.setSelectedItem(fsJava.getReverb());
                case FluidSynthJava.PROP_GAIN ->
                {
                    knob_gain.setValue(fsGain2uiGain(fsJava.getGain()));
                    lbl_gainValue.setText(String.format("%.1f", fsJava.getGain()));
                }
                default ->
                    throw new IllegalStateException("evt.getPropertyName()=" + evt.getPropertyName());
            }
        } else if (evt.getSource() == outputSynthSettings)
        {
            if (evt.getPropertyName().equals(OutputSynth.UserSettings.PROP_AUDIO_LATENCY))
            {
                knob_latency.setValue(outputSynthSettings.getAudioLatency());
                lbl_latencyValue.setText(outputSynthSettings.getAudioLatency() + " ms");
            }
        } else if (evt.getSource() == knob_gain)
        {
            // Only 1 property possible
            fsJava.setGain(uiGain2fsGain(knob_gain.getValue()));
        } else if (evt.getSource() == knob_latency)
        {
            // Only 1 property possible
            outputSynthSettings.setAudioLatency(knob_latency.getValue());
        }
    }

    // ==============================================================================
    // Private methods
    // ==============================================================================
    private float uiGain2fsGain(int gain)
    {
        return gain / 10f;
    }

    private int fsGain2uiGain(float gain)
    {
        return Math.round(gain * 10);
    }

    private void register(FluidSynthJava fsj)
    {
        
    }
    
    private String getAudioDriver()
    {
        return fsEmbeddedSynth.getAudioDriver();
    }

    /**
     * Try to perform the audio driver change.
     * 
     * Creates a new FluidSynthJava instance if change is successful.
     *
     * @param newDriver
     */
    private void changeAudioDriver(String newDriver)
    {
        Objects.requireNonNull(newDriver);
        String oldDriver = getAudioDriver();
        if (newDriver.equals(oldDriver))
        {
            return;
        }

        // The FluidSynthJava will change
        fsJava.removePropertyChangeListener(this);
        
        
        MusicController.getInstance().stop();
        class ChangeDriverTask implements Runnable
        {

            String errMsg = null;

            @Override
            public void run()
            {
                fsEmbeddedSynth.close();
                fsEmbeddedSynth.saveAudioDriver(newDriver);
                try
                {
                    fsEmbeddedSynth.open();         // throws EmbeddedSynthException
                } catch (Throwable exception)
                {
                    errMsg = "Error with audio driver " + newDriver + ". " + exception;
                    LOGGER.log(Level.WARNING, "changeAudioDriver() {0}", errMsg);
                    LOGGER.log(Level.WARNING, "changeAudioDriver() Restoring audio driver {0}", oldDriver);
                    fsEmbeddedSynth.close();
                    fsEmbeddedSynth.saveAudioDriver(oldDriver);
                    try
                    {
                        fsEmbeddedSynth.open();
                    } catch (Throwable ex)
                    {
                        // Should not happen
                        LOGGER.log(Level.SEVERE, "changeAudioDriver() Impossible to switch back to {0}: {1}", new Object[]
                        {
                            oldDriver, ex
                        });
                        Exceptions.printStackTrace(ex);
                        errMsg += "\nError restoring audio driver to " + oldDriver + ". This is a serious issue. Restart JJazzLab.";
                    }
                }
            }
        }

        ChangeDriverTask task = new ChangeDriverTask();
        BaseProgressUtils.showProgressDialogAndRun(task, "Restarting FluidSynth to use " + newDriver);

        if (task.errMsg != null)
        {
            NotifyDescriptor nd = new NotifyDescriptor.Message(task.errMsg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
        } 
        
        fsJava = fsEmbeddedSynth.getFluidSynthJava();
        fsJava.addPropertyChangeListener(this);

    }

    // ==============================================================================
    // Inner classes
    // ==============================================================================
    /**
     * Cell renderer to show the current audio driver in bold font.
     */
    private class MyCellRenderer extends DefaultListCellRenderer
    {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (Objects.equals(getAudioDriver(), value))
            {
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            } else
            {
                c.setFont(c.getFont().deriveFont(Font.PLAIN));
            }
            return c;
        }
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        btn_cancel = new javax.swing.JButton();
        btn_ok = new javax.swing.JButton();
        pnl_effects = new javax.swing.JPanel();
        combo_reverb = new JComboBox(fsEmbeddedSynth.getReverbPresets());
        lbl_chorus = new javax.swing.JLabel();
        lbl_reverb = new javax.swing.JLabel();
        combo_chorus = new JComboBox(fsEmbeddedSynth.getChorusPresets());
        pnl_audioDrivers = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_audioDrivers = new javax.swing.JList<>();
        btn_setAudioDriver = new javax.swing.JButton();
        pnbl_knobs = new javax.swing.JPanel();
        pnl_gain = new javax.swing.JPanel();
        lbl_gain = new javax.swing.JLabel();
        knob_gain = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_gainValue = new javax.swing.JLabel();
        pnl_latency = new javax.swing.JPanel();
        lbl_latency = new javax.swing.JLabel();
        knob_latency = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_latencyValue = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.title")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_cancel, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.btn_cancel.text")); // NOI18N
        btn_cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_ok, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.btn_ok.text")); // NOI18N
        btn_ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_okActionPerformed(evt);
            }
        });

        pnl_effects.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.pnl_effects.border.title"))); // NOI18N

        combo_reverb.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                combo_reverbActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_chorus, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_chorus.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_reverb, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_reverb.text")); // NOI18N

        combo_chorus.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                combo_chorusActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_effectsLayout = new javax.swing.GroupLayout(pnl_effects);
        pnl_effects.setLayout(pnl_effectsLayout);
        pnl_effectsLayout.setHorizontalGroup(
            pnl_effectsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_effectsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_effectsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_chorus)
                    .addComponent(lbl_reverb))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_effectsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(combo_reverb, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(combo_chorus, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnl_effectsLayout.setVerticalGroup(
            pnl_effectsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_effectsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_effectsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(combo_reverb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_reverb))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_effectsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(combo_chorus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_chorus))
                .addContainerGap())
        );

        pnl_audioDrivers.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.pnl_audioDrivers.border.title"))); // NOI18N

        list_audioDrivers.setModel(new javax.swing.AbstractListModel<String>()
        {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        list_audioDrivers.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_audioDrivers.setVisibleRowCount(4);
        list_audioDrivers.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_audioDriversValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(list_audioDrivers);

        org.openide.awt.Mnemonics.setLocalizedText(btn_setAudioDriver, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.btn_setAudioDriver.text")); // NOI18N
        btn_setAudioDriver.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_setAudioDriverActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_audioDriversLayout = new javax.swing.GroupLayout(pnl_audioDrivers);
        pnl_audioDrivers.setLayout(pnl_audioDriversLayout);
        pnl_audioDriversLayout.setHorizontalGroup(
            pnl_audioDriversLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_audioDriversLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_setAudioDriver)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnl_audioDriversLayout.setVerticalGroup(
            pnl_audioDriversLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_audioDriversLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_audioDriversLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_audioDriversLayout.createSequentialGroup()
                        .addComponent(btn_setAudioDriver)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );

        pnbl_knobs.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 20, 5));

        pnl_gain.setLayout(new javax.swing.BoxLayout(pnl_gain, javax.swing.BoxLayout.Y_AXIS));

        lbl_gain.setFont(lbl_gain.getFont().deriveFont(lbl_gain.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_gain, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_gain.text")); // NOI18N
        lbl_gain.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_gain.toolTipText")); // NOI18N
        lbl_gain.setAlignmentX(0.5F);
        pnl_gain.add(lbl_gain);

        knob_gain.setToolTipText(lbl_gain.getToolTipText());
        knob_gain.setMaxValue(100);
        knob_gain.setUseValueTooltip(false);
        knob_gain.setValue(10);
        knob_gain.setValueLineColor(new java.awt.Color(0, 153, 204));

        javax.swing.GroupLayout knob_gainLayout = new javax.swing.GroupLayout(knob_gain);
        knob_gain.setLayout(knob_gainLayout);
        knob_gainLayout.setHorizontalGroup(
            knob_gainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        knob_gainLayout.setVerticalGroup(
            knob_gainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        pnl_gain.add(knob_gain);

        lbl_gainValue.setFont(lbl_gain.getFont());
        org.openide.awt.Mnemonics.setLocalizedText(lbl_gainValue, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_gainValue.text")); // NOI18N
        lbl_gainValue.setToolTipText(knob_gain.getToolTipText());
        lbl_gainValue.setAlignmentX(0.5F);
        pnl_gain.add(lbl_gainValue);

        pnbl_knobs.add(pnl_gain);

        pnl_latency.setLayout(new javax.swing.BoxLayout(pnl_latency, javax.swing.BoxLayout.Y_AXIS));

        lbl_latency.setFont(lbl_gain.getFont());
        org.openide.awt.Mnemonics.setLocalizedText(lbl_latency, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_latency.text")); // NOI18N
        lbl_latency.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_latency.toolTipText")); // NOI18N
        lbl_latency.setAlignmentX(0.5F);
        pnl_latency.add(lbl_latency);

        knob_latency.setToolTipText(lbl_latency.getToolTipText());
        knob_latency.setMaxValue(1000);
        knob_latency.setUseValueTooltip(false);
        knob_latency.setValue(150);
        knob_latency.setValueLineColor(knob_gain.getValueLineColor());

        javax.swing.GroupLayout knob_latencyLayout = new javax.swing.GroupLayout(knob_latency);
        knob_latency.setLayout(knob_latencyLayout);
        knob_latencyLayout.setHorizontalGroup(
            knob_latencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 67, Short.MAX_VALUE)
        );
        knob_latencyLayout.setVerticalGroup(
            knob_latencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 32, Short.MAX_VALUE)
        );

        pnl_latency.add(knob_latency);

        lbl_latencyValue.setFont(lbl_gain.getFont());
        org.openide.awt.Mnemonics.setLocalizedText(lbl_latencyValue, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_latencyValue.text")); // NOI18N
        lbl_latencyValue.setToolTipText(knob_latency.getToolTipText());
        lbl_latencyValue.setAlignmentX(0.5F);
        pnl_latency.add(lbl_latencyValue);

        pnbl_knobs.add(pnl_latency);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_effects, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_ok)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_cancel))
                    .addComponent(pnl_audioDrivers, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnbl_knobs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_cancel, btn_ok});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnbl_knobs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnl_effects, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(pnl_audioDrivers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_cancel)
                    .addComponent(btn_ok))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_okActionPerformed
    {//GEN-HEADEREND:event_btn_okActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_btn_okActionPerformed

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cancelActionPerformed
    {//GEN-HEADEREND:event_btn_cancelActionPerformed
        outputSynthSettings.setAudioLatency(cancelLatency);
        changeAudioDriver(cancelAudioDriver);       // Must be done before calls below to possibly retrieve the right FluidSynthJava instance        
        fsJava.setGain(cancelGain);
        fsJava.setChorus(cancelChorus);
        fsJava.setReverb(cancelReverb);

        setVisible(false);
        dispose();
    }//GEN-LAST:event_btn_cancelActionPerformed

    private void combo_reverbActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_combo_reverbActionPerformed
    {//GEN-HEADEREND:event_combo_reverbActionPerformed
        fsJava.setReverb((Reverb) combo_reverb.getSelectedItem());
    }//GEN-LAST:event_combo_reverbActionPerformed

    private void combo_chorusActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_combo_chorusActionPerformed
    {//GEN-HEADEREND:event_combo_chorusActionPerformed
        fsJava.setChorus((Chorus) combo_chorus.getSelectedItem());
    }//GEN-LAST:event_combo_chorusActionPerformed

    private void list_audioDriversValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_audioDriversValueChanged
    {//GEN-HEADEREND:event_list_audioDriversValueChanged
        if (evt.getValueIsAdjusting())
        {
            return;
        }
        var selection = list_audioDrivers.getSelectedValue();
        btn_setAudioDriver.setEnabled(selection != null && !selection.equals(getAudioDriver()));
    }//GEN-LAST:event_list_audioDriversValueChanged

    private void btn_setAudioDriverActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_setAudioDriverActionPerformed
    {//GEN-HEADEREND:event_btn_setAudioDriverActionPerformed
        var newDriver = list_audioDrivers.getSelectedValue();
        changeAudioDriver(newDriver);
        list_audioDrivers.setSelectedValue(getAudioDriver(), true);
    }//GEN-LAST:event_btn_setAudioDriverActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_cancel;
    private javax.swing.JButton btn_ok;
    private javax.swing.JButton btn_setAudioDriver;
    private javax.swing.JComboBox<Chorus> combo_chorus;
    private javax.swing.JComboBox<Reverb> combo_reverb;
    private javax.swing.JScrollPane jScrollPane2;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_gain;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_latency;
    private javax.swing.JLabel lbl_chorus;
    private javax.swing.JLabel lbl_gain;
    private javax.swing.JLabel lbl_gainValue;
    private javax.swing.JLabel lbl_latency;
    private javax.swing.JLabel lbl_latencyValue;
    private javax.swing.JLabel lbl_reverb;
    private javax.swing.JList<String> list_audioDrivers;
    private javax.swing.JPanel pnbl_knobs;
    private javax.swing.JPanel pnl_audioDrivers;
    private javax.swing.JPanel pnl_effects;
    private javax.swing.JPanel pnl_gain;
    private javax.swing.JPanel pnl_latency;
    // End of variables declaration//GEN-END:variables
}
