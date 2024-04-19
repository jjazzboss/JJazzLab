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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import org.jjazz.fluidsynthjava.api.Chorus;
import org.jjazz.fluidsynthjava.api.FluidSynthJava;
import org.jjazz.fluidsynthjava.api.Reverb;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.flatcomponents.api.FlatIntegerKnob;
import org.jjazz.uiutilities.api.UIUtilities;
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
    private final OutputSynth.UserSettings outputSynthSettings;
    private final FluidSynthJava fsJava;
    private static final Logger LOGGER = Logger.getLogger(SettingsDialog.class.getSimpleName());

    public SettingsDialog(FluidSynthEmbeddedSynth eSynth)
    {
        super(WindowManager.getDefault().getMainWindow(), true);
        Preconditions.checkNotNull(eSynth);
        fsEmbeddedSynth = eSynth;
        fsJava = eSynth.getFluidSynthJava();

        initComponents();

        // FluidSynthJava data
        cancelGain = fsJava.getGain();
        cancelReverb = fsJava.getReverb();
        cancelChorus = fsJava.getChorus();
        knob_gain.setValue(fsGain2uiGain(cancelGain));
        lbl_gainValue.setText(String.format("%.1f", cancelGain));
        combo_chorus.setSelectedItem(cancelChorus);
        combo_reverb.setSelectedItem(cancelReverb);

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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        btn_cancel = new javax.swing.JButton();
        btn_ok = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        pnl_gain = new javax.swing.JPanel();
        lbl_gain = new javax.swing.JLabel();
        knob_gain = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_gainValue = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        lbl_latency = new javax.swing.JLabel();
        knob_latency = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_latencyValue = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        combo_reverb = new JComboBox(fsEmbeddedSynth.getReverbPresets());
        lbl_chorus = new javax.swing.JLabel();
        lbl_reverb = new javax.swing.JLabel();
        combo_chorus = new JComboBox(fsEmbeddedSynth.getChorusPresets());

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

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

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

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        lbl_latency.setFont(lbl_gain.getFont());
        org.openide.awt.Mnemonics.setLocalizedText(lbl_latency, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_latency.text")); // NOI18N
        lbl_latency.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_latency.toolTipText")); // NOI18N
        lbl_latency.setAlignmentX(0.5F);
        jPanel2.add(lbl_latency);

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

        jPanel2.add(knob_latency);

        lbl_latencyValue.setFont(lbl_gain.getFont());
        org.openide.awt.Mnemonics.setLocalizedText(lbl_latencyValue, org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.lbl_latencyValue.text")); // NOI18N
        lbl_latencyValue.setToolTipText(knob_latency.getToolTipText());
        lbl_latencyValue.setAlignmentX(0.5F);
        jPanel2.add(lbl_latencyValue);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsDialog.class, "SettingsDialog.jPanel3.border.title"))); // NOI18N

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

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_chorus)
                    .addComponent(lbl_reverb))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(combo_chorus, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(combo_reverb, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(combo_reverb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_reverb))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(combo_chorus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_chorus))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(73, 73, 73))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(pnl_gain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 39, Short.MAX_VALUE)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_ok)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_cancel)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_cancel, btn_ok});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pnl_gain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_cancel;
    private javax.swing.JButton btn_ok;
    private javax.swing.JComboBox<Chorus> combo_chorus;
    private javax.swing.JComboBox<Reverb> combo_reverb;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_gain;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_latency;
    private javax.swing.JLabel lbl_chorus;
    private javax.swing.JLabel lbl_gain;
    private javax.swing.JLabel lbl_gainValue;
    private javax.swing.JLabel lbl_latency;
    private javax.swing.JLabel lbl_latencyValue;
    private javax.swing.JLabel lbl_reverb;
    private javax.swing.JPanel pnl_gain;
    // End of variables declaration//GEN-END:variables
}
