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

import java.awt.event.ActionEvent;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midi.MidiConst;
import org.jjazz.rhythmmusicgeneration.NoteEvent;
import org.jjazz.rhythmmusicgeneration.Phrase;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * The dialog to adjust output latency.
 */
public class LatencyDialog extends javax.swing.JDialog implements MetaEventListener
{

    private boolean exitOk;

    private enum State
    {
        INIT, RECORDING, RECORDED
    }

    private State state = State.INIT;

    private Sequence sequence;
    private Sequencer sequencer;
    private Track recordingTrack;

    /**
     * Creates new form LatencyDialog
     */
    public LatencyDialog(int latency)
    {
        super(WindowManager.getDefault().getMainWindow(), true);
        if (latency < 0)
        {
            throw new IllegalArgumentException("latency=" + latency);
        }

        initComponents();

        initSequence();

        var jms = JJazzMidiSystem.getInstance();
        sequencer = jms.getDefaultSequencer();
        sequencer.addMetaEventListener(this);

        changeState(State.INIT);
        spn_latency.setValue(latency);
    }

    /**
     * Return the latency.
     *
     * @return -1 if used cancelled the dialog.
     */
    public int getLatency()
    {
        return exitOk ? (Integer) spn_latency.getValue() : -1;
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
                btn_OKActionPerformed(null);
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "actionCancel");
        contentPane.getActionMap().put("actionCancel", new AbstractAction("Cancel")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                btn_CancelActionPerformed(null);
            }
        });
        return contentPane;
    }

    public void cleanup()
    {
        JJazzMidiSystem.getInstance().getDefaultSequencer().removeMetaEventListener(this);
    }

    // ======================================================================================================
    // MetaEventListener implementation
    // ======================================================================================================
    @Override
    public void meta(MetaMessage meta)
    {
        if (meta.getType() == 47) // Meta Event for end of sequence
        {
            // This method  is called from the Sequencer thread, NOT from the EDT !
            // So if this method impacts the UI, it must use SwingUtilities.InvokeLater() (or InvokeAndWait())
            Runnable doRun = new Runnable()
            {
                @Override
                public void run()
                {
                    recordingEnded();
                }
            };
            SwingUtilities.invokeLater(doRun);
        }
    }

    // ======================================================================================================
    // Private methods
    // ======================================================================================================
    private void changeState(State newState)
    {
        switch (newState)
        {
            case INIT:
                btn_test.setEnabled(false);
                btn_record.setEnabled(true);
                btn_stop.setEnabled(false);
                spn_latency.setEnabled(false);
                break;
            case RECORDING:
                btn_test.setEnabled(false);
                btn_record.setEnabled(false);
                btn_stop.setEnabled(true);
                spn_latency.setEnabled(false);
                break;
            case RECORDED:
                btn_test.setEnabled(true);
                btn_record.setEnabled(true);
                btn_stop.setEnabled(false);
                spn_latency.setEnabled(true);
                break;
            default:
                throw new AssertionError(state.name());
        }

        state = newState;
    }

    private void startRecording()
    {
        changeState(State.RECORDING);

        try
        {
            sequencer.setSequence(sequence);
        } catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        sequencer.setTempoInBPM(100);
        sequencer.setTempoFactor(1f);
        sequencer.setTickPosition(0);

        sequencer.start();

    }

    private void recordingEnded()
    {
        JJazzMidiSystem.getInstance().getDefaultSequencer().stop();
        changeState(State.RECORDED);
    }

    /**
     * Create the sequence with a playback track and an empty recording track.
     */
    private void initSequence()
    {
        try
        {
            sequence = new Sequence(Sequence.PPQ, MidiConst.PPQ_RESOLUTION);
            recordingTrack = sequence.createTrack();

            // Create playback track
            Track playbackTrack = sequence.createTrack();
            Phrase p = getDrumsPhrase();
            p.fillTrack(playbackTrack);

        } catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }

    private Phrase getDrumsPhrase()
    {
        final int NB_BARS = 8;
        final float DUR = 0.25f;

        Phrase p = new Phrase(MidiConst.CHANNEL_DRUMS);

        for (int bar = 0; bar < NB_BARS; bar++)
        {
            for (float beat = 0; beat < 4f; beat += 0.5f)
            {
                int pitch = MidiConst.CLOSED_HI_HAT;
                int velocity = 40;
                if (beat == 0 || beat == 2)
                {
                    pitch = MidiConst.ACOUSTIC_BASS_DRUM;
                    velocity = 90;
                } else if (beat == 1 || beat == 3)
                {
                    pitch = MidiConst.ACOUSTIC_SNARE;
                    velocity = 90;
                }

                NoteEvent ne = new NoteEvent(pitch, DUR, velocity, bar * 4 + beat);
                p.addOrdered(ne);
            }

        }
        return p;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jButton3 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        btn_record = new javax.swing.JButton();
        lbl_latency = new javax.swing.JLabel();
        spn_latency = new org.jjazz.ui.utilities.WheelSpinner();
        btn_test = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        btn_Cancel = new javax.swing.JButton();
        btn_OK = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.HelpTextArea();
        flatLedIndicator1 = new org.jjazz.ui.flatcomponents.FlatLedIndicator();
        lbl_countdown = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(jButton3, org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.jButton3.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButton5, org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.jButton5.text")); // NOI18N

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.title")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_record, org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.btn_record.text")); // NOI18N
        btn_record.setToolTipText(org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.btn_record.toolTipText")); // NOI18N
        btn_record.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_recordActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_latency, org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.lbl_latency.text")); // NOI18N

        spn_latency.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1500, 1));

        org.openide.awt.Mnemonics.setLocalizedText(btn_test, org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.btn_test.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.jLabel4.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_OK, org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.btn_OK.text")); // NOI18N
        btn_OK.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OKActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.jPanel1.border.title"))); // NOI18N

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/options/resources/Latency.png"))); // NOI18N

        jScrollPane1.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(6);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.helpTextArea1.text")); // NOI18N
        jScrollPane1.setViewportView(helpTextArea1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1)
                .addGap(2, 2, 2))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );

        flatLedIndicator1.setToolTipText(org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.flatLedIndicator1.toolTipText")); // NOI18N
        flatLedIndicator1.setDiameter(10);

        lbl_countdown.setFont(new java.awt.Font("Courier New", 1, 18)); // NOI18N
        lbl_countdown.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_countdown, org.openide.util.NbBundle.getMessage(LatencyDialog.class, "LatencyDialog.lbl_countdown.text")); // NOI18N
        lbl_countdown.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        lbl_countdown.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btn_test)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btn_record)
                                .addGap(18, 18, 18)
                                .addComponent(lbl_countdown)
                                .addGap(70, 70, 70)
                                .addComponent(lbl_latency)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spn_latency, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(btn_OK)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btn_Cancel))
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(696, Short.MAX_VALUE)
                .addComponent(flatLedIndicator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_Cancel, btn_OK});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flatLedIndicator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_record)
                    .addComponent(lbl_latency)
                    .addComponent(spn_latency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_countdown))
                .addGap(33, 33, 33)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addComponent(btn_test)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_Cancel)
                    .addComponent(btn_OK))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_OKActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_OKActionPerformed
    {//GEN-HEADEREND:event_btn_OKActionPerformed
        exitOk = true;
        dispose();
    }//GEN-LAST:event_btn_OKActionPerformed

    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
    {//GEN-HEADEREND:event_btn_CancelActionPerformed
        exitOk = false;
        dispose();
    }//GEN-LAST:event_btn_CancelActionPerformed

    private void btn_recordActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_recordActionPerformed
    {//GEN-HEADEREND:event_btn_recordActionPerformed
        startRecording();
    }//GEN-LAST:event_btn_recordActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_OK;
    private javax.swing.JButton btn_record;
    private javax.swing.JButton btn_test;
    private org.jjazz.ui.flatcomponents.FlatLedIndicator flatLedIndicator1;
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea1;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_countdown;
    private javax.swing.JLabel lbl_latency;
    private org.jjazz.ui.utilities.WheelSpinner spn_latency;
    // End of variables declaration//GEN-END:variables
}
