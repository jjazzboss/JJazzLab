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
package org.jjazz.helpers.midiwizard;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JPanel;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midi.MidiUtilities;
import org.jjazz.midi.synths.Family;
import org.jjazz.midi.synths.GSSynth;
import org.jjazz.musiccontrol.TestPlayer;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

public final class MidiWizardVisualPanel5 extends JPanel
{

    MidiDevice midiDeviceOut;

    /**
     * Creates new form StartupWizardVisualPanel4
     */
    public MidiWizardVisualPanel5()
    {
        initComponents();
    }

    public void setMidiDeviceOut(MidiDevice md)
    {
        midiDeviceOut = md;
        lbl_outDevice.setText("Midi Out: " + JJazzMidiSystem.getInstance().getDeviceFriendlyName(md));
        lbl_outDevice.setToolTipText(md.getDeviceInfo().getDescription());
    }

    @Override
    public String getName()
    {
        return "GM compatibility";
    }

    public void setGM2Support(boolean b)
    {
        if (b)
        {
            rbtn_yesGM2.setSelected(true);
        } else
        {
            this.rbtn_noGM2.setSelected(true);
        }
    }

    public boolean isGM2support()
    {
        return rbtn_yesGM2.isSelected();
    }

    public void setXGSupport(boolean b)
    {
        if (b)
        {
            rbtn_yesXG.setSelected(true);
        } else
        {
            this.rbtn_noXG.setSelected(true);
        }
    }

    public boolean isXGsupport()
    {
        return rbtn_yesXG.isSelected();
    }

    public void setGSSupport(boolean b)
    {
        if (b)
        {
            rbtn_yesGS.setSelected(true);
        } else
        {
            this.rbtn_noGS.setSelected(true);
        }
    }

    public boolean isGSsupport()
    {
        return rbtn_yesGS.isSelected();
    }

    /**
     * Send test notes with a possible leading set mode ON sysex message.
     * <p>
     * Send a GM ON Sysex mode after notes have been played.
     *
     * @param mode       Send mode ON sysex message before playing notes. 0=no sysex, 1=GM, 2=GM2, 3=XG, 4=GS
     * @param channel
     * @param instrument Can be null
     */
    private void sendTestNotes(int mode, int channel, Instrument instrument, int transpose)
    {
        if (mode < 0 || mode > 4)
        {
            throw new IllegalArgumentException("mode=" + mode + " channel=" + channel + " instrument=" + instrument.toLongString());
        }
        assert midiDeviceOut != null;

        final JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        final MidiDevice saveDeviceOut = jms.getDefaultOutDevice();
        try
        {
            jms.setDefaultOutDevice(midiDeviceOut);
        } catch (MidiUnavailableException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }

        switch (mode)
        {
            case 1:
                MidiUtilities.sendSysExMessage(MidiUtilities.getGmModeOnSysExMessage());
                break;
            case 2:
                MidiUtilities.sendSysExMessage(MidiUtilities.getGm2ModeOnSysExMessage());
                break;
            case 3:
                MidiUtilities.sendSysExMessage(MidiUtilities.getXgModeOnSysExMessage());
                break;
            case 4:
                MidiUtilities.sendSysExMessage(MidiUtilities.getGsModeOnSysExMessage());
                break;
            default:
            // Nothing
        }

        // Send the instrument patch messages
        if (instrument != null)
        {
            jms.sendMidiMessagesOnJJazzMidiOut(instrument.getMidiMessages(channel));
        }

        btn_test1.setEnabled(false);
        btn_test2.setEnabled(false);
        btn_testDrums.setEnabled(false);
        btn_testDrumsOtherChannelGM2.setEnabled(false);
        btn_testDrumsOtherChannelXG.setEnabled(false);
        btn_testDrumsOtherChannelGS.setEnabled(false);

        Runnable endAction = new Runnable()
        {
            @Override
            public void run()
            {
                // Called when sequence is stopped
                btn_test1.setEnabled(true);
                btn_test2.setEnabled(true);
                btn_testDrums.setEnabled(true);
                btn_testDrumsOtherChannelGM2.setEnabled(true);
                btn_testDrumsOtherChannelXG.setEnabled(true);
                btn_testDrumsOtherChannelGS.setEnabled(true);
                try
                {
                    jms.setDefaultOutDevice(saveDeviceOut);
                } catch (MidiUnavailableException ex)
                {
                    NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(d);
                }
                // For safety restore GM mode
                MidiUtilities.sendSysExMessage(MidiUtilities.getGmModeOnSysExMessage());
            }
        };

        TestPlayer tp = TestPlayer.getInstance();
        try
        {
            tp.playTestNotes(channel, -1, transpose, endAction);
        } catch (MusicGenerationException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        buttonGroupGM2 = new javax.swing.ButtonGroup();
        buttonGroupXG = new javax.swing.ButtonGroup();
        buttonGroupGS = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        wizardTextArea1 = new org.jjazz.ui.utilities.WizardTextArea();
        btn_test1 = new javax.swing.JButton();
        btn_test2 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.HelpTextArea();
        lbl_outDevice = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        rbtn_yesGM2 = new javax.swing.JRadioButton();
        btn_testDrumsOtherChannelGM2 = new javax.swing.JButton();
        rbtn_noGM2 = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        btn_testDrums = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        rbtn_yesXG = new javax.swing.JRadioButton();
        btn_testDrumsOtherChannelXG = new javax.swing.JButton();
        rbtn_noXG = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        rbtn_yesGS = new javax.swing.JRadioButton();
        btn_testDrumsOtherChannelGS = new javax.swing.JButton();
        rbtn_noGS = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();

        jScrollPane1.setBorder(null);

        wizardTextArea1.setEditable(false);
        wizardTextArea1.setColumns(20);
        wizardTextArea1.setRows(5);
        wizardTextArea1.setText(org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.wizardTextArea1.text")); // NOI18N
        jScrollPane1.setViewportView(wizardTextArea1);

        org.openide.awt.Mnemonics.setLocalizedText(btn_test1, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.btn_test1.text")); // NOI18N
        btn_test1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_test1ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_test2, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.btn_test2.text")); // NOI18N
        btn_test2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_test2ActionPerformed(evt);
            }
        });

        jScrollPane2.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(2);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.helpTextArea1.text")); // NOI18N
        jScrollPane2.setViewportView(helpTextArea1);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_outDevice, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.lbl_outDevice.text")); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.jPanel1.border.title"))); // NOI18N

        buttonGroupGM2.add(rbtn_yesGM2);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_yesGM2, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.rbtn_yesGM2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_testDrumsOtherChannelGM2, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.btn_testDrumsOtherChannelGM2.text")); // NOI18N
        btn_testDrumsOtherChannelGM2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_testDrumsOtherChannelGM2ActionPerformed(evt);
            }
        });

        buttonGroupGM2.add(rbtn_noGM2);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_noGM2, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.rbtn_noGM2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_testDrumsOtherChannelGM2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbtn_yesGM2)
                .addGap(18, 18, 18)
                .addComponent(rbtn_noGM2)
                .addContainerGap(154, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_testDrumsOtherChannelGM2)
                    .addComponent(rbtn_yesGM2)
                    .addComponent(rbtn_noGM2)
                    .addComponent(jLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.openide.awt.Mnemonics.setLocalizedText(btn_testDrums, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.btn_testDrums.text")); // NOI18N
        btn_testDrums.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_testDrumsActionPerformed(evt);
            }
        });

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.jPanel2.border.title"))); // NOI18N

        buttonGroupXG.add(rbtn_yesXG);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_yesXG, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.rbtn_yesXG.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_testDrumsOtherChannelXG, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.btn_testDrumsOtherChannelXG.text")); // NOI18N
        btn_testDrumsOtherChannelXG.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_testDrumsOtherChannelXGActionPerformed(evt);
            }
        });

        buttonGroupXG.add(rbtn_noXG);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_noXG, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.rbtn_noXG.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.jLabel2.text")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_testDrumsOtherChannelXG)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbtn_yesXG)
                .addGap(18, 18, 18)
                .addComponent(rbtn_noXG)
                .addContainerGap(154, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_testDrumsOtherChannelXG)
                    .addComponent(rbtn_yesXG)
                    .addComponent(rbtn_noXG)
                    .addComponent(jLabel2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.jPanel3.border.title"))); // NOI18N

        buttonGroupGS.add(rbtn_yesGS);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_yesGS, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.rbtn_yesGS.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_testDrumsOtherChannelGS, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.btn_testDrumsOtherChannelGS.text")); // NOI18N
        btn_testDrumsOtherChannelGS.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_testDrumsOtherChannelGSActionPerformed(evt);
            }
        });

        buttonGroupGS.add(rbtn_noGS);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_noGS, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.rbtn_noGS.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(MidiWizardVisualPanel5.class, "MidiWizardVisualPanel5.jLabel3.text")); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_testDrumsOtherChannelGS)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbtn_yesGS)
                .addGap(18, 18, 18)
                .addComponent(rbtn_noGS)
                .addContainerGap(154, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_testDrumsOtherChannelGS)
                    .addComponent(rbtn_yesGS)
                    .addComponent(rbtn_noGS)
                    .addComponent(jLabel3))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lbl_outDevice)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btn_test1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_test2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_testDrums))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_outDevice)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_test1)
                    .addComponent(btn_test2)
                    .addComponent(btn_testDrums))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btn_test1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_test1ActionPerformed
    {//GEN-HEADEREND:event_btn_test1ActionPerformed
        sendTestNotes(1, 2, StdSynth.getInstance().getGM1Bank().getDefaultInstrument(Family.Organ), 0);
    }//GEN-LAST:event_btn_test1ActionPerformed

    private void btn_testDrumsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_testDrumsActionPerformed
    {//GEN-HEADEREND:event_btn_testDrumsActionPerformed
        MidiUtilities.sendSysExMessage(MidiUtilities.getGmModeOnSysExMessage());
        sendTestNotes(1, 9, null, -12);
    }//GEN-LAST:event_btn_testDrumsActionPerformed

    private void btn_test2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_test2ActionPerformed
    {//GEN-HEADEREND:event_btn_test2ActionPerformed
        sendTestNotes(1, 11, StdSynth.getInstance().getGM1Bank().getDefaultInstrument(Family.Reed), -12);
    }//GEN-LAST:event_btn_test2ActionPerformed

    private void btn_testDrumsOtherChannelGM2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_testDrumsOtherChannelGM2ActionPerformed
    {//GEN-HEADEREND:event_btn_testDrumsOtherChannelGM2ActionPerformed
        sendTestNotes(2, 2, StdSynth.getInstance().getGM2Bank().getDefaultDrumsInstrument(), -12);
    }//GEN-LAST:event_btn_testDrumsOtherChannelGM2ActionPerformed

    private void btn_testDrumsOtherChannelXGActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_testDrumsOtherChannelXGActionPerformed
    {//GEN-HEADEREND:event_btn_testDrumsOtherChannelXGActionPerformed
        sendTestNotes(3, 3, StdSynth.getInstance().getXGBank().getDefaultDrumsInstrument(), -12);
    }//GEN-LAST:event_btn_testDrumsOtherChannelXGActionPerformed

    private void btn_testDrumsOtherChannelGSActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_testDrumsOtherChannelGSActionPerformed
    {//GEN-HEADEREND:event_btn_testDrumsOtherChannelGSActionPerformed
        sendTestNotes(4, 4, GSSynth.getInstance().getGSBank().getDefaultDrumsInstrument(), -12);
    }//GEN-LAST:event_btn_testDrumsOtherChannelGSActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_test1;
    private javax.swing.JButton btn_test2;
    private javax.swing.JButton btn_testDrums;
    private javax.swing.JButton btn_testDrumsOtherChannelGM2;
    private javax.swing.JButton btn_testDrumsOtherChannelGS;
    private javax.swing.JButton btn_testDrumsOtherChannelXG;
    private javax.swing.ButtonGroup buttonGroupGM2;
    private javax.swing.ButtonGroup buttonGroupGS;
    private javax.swing.ButtonGroup buttonGroupXG;
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lbl_outDevice;
    private javax.swing.JRadioButton rbtn_noGM2;
    private javax.swing.JRadioButton rbtn_noGS;
    private javax.swing.JRadioButton rbtn_noXG;
    private javax.swing.JRadioButton rbtn_yesGM2;
    private javax.swing.JRadioButton rbtn_yesGS;
    private javax.swing.JRadioButton rbtn_yesXG;
    private org.jjazz.ui.utilities.WizardTextArea wizardTextArea1;
    // End of variables declaration//GEN-END:variables
}
