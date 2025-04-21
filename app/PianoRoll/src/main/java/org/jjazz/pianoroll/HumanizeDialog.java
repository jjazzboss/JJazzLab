/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.pianoroll;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.humanizer.api.Humanizer;
import org.jjazz.humanizer.api.Humanizer.Config;
import org.jjazz.humanizer.api.Humanizer.State;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.actions.PlayLoopZone;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.IntRange;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 * The dialog to control humanization.
 */
public class HumanizeDialog extends javax.swing.JDialog implements ChangeListener, PropertyChangeListener
{

    private static final String PREF_CONFIG = "PrefConfig";
    private final Humanizer humanizer;
    private final PianoRollEditorTopComponent editorTc;
    private final PianoRollEditor editor;
    private final String undoText;
    private static final Preferences prefs = NbPreferences.forModule(HumanizeDialog.class);
    private static final Logger LOGGER = Logger.getLogger(HumanizeDialog.class.getSimpleName());

    public HumanizeDialog(PianoRollEditorTopComponent tc)
    {
        super(WindowManager.getDefault().getMainWindow(), true);


        this.editorTc = tc;
        this.editor = editorTc.getEditor();
        Phrase p = editor.getModel();
        var beatRange = editor.getPhraseBeatRange();
        var ts = editor.getTimeSignature(beatRange.from);       // Not perfect, but for now Humanizer can't manage multiple time signatures
        humanizer = new Humanizer(p, ts, beatRange, null, editor.getSong().getTempo());
        Config cfg = Config.loadFromString(prefs.get(PREF_CONFIG, Humanizer.DEFAULT_CONFIG.toSaveString()));
        humanizer.setConfig(cfg);
        humanizer.addPropertyChangeListener(this);


        // Listen to playback state
        MusicController.getInstance().addPropertyChangeListener(this);


        initComponents();
        registerOrUnregisterSliders(true);
        lbl_timingText.setToolTipText(sld_timing.getToolTipText());
        lbl_timingValue.setToolTipText(sld_timing.getToolTipText());
        lbl_timingBiasText.setToolTipText(sld_timingBias.getToolTipText());
        lbl_timingBiasValue.setToolTipText(sld_timingBias.getToolTipText());
        lbl_velocityText.setToolTipText(sld_velocity.getToolTipText());
        lbl_velocityValue.setToolTipText(sld_velocity.getToolTipText());
        undoText = btn_humanize.getText();
        setLocationRelativeTo(WindowManager.getDefault().getMainWindow());


        UIUtilities.installEnterKeyAction(this, () -> doOk());
        UIUtilities.installEscapeKeyAction(this, () -> doCancel());


        boolean selectionEmpty = editor.getSelectedNoteViews().isEmpty();
        cb_selectedNotesOnly.setEnabled(!selectionEmpty);
        cb_selectedNotesOnly.setSelected(!selectionEmpty);
        setSelectedNotesOnly(!selectionEmpty);

        refreshUI();


        // Start the undoable action from here
        editor.getUndoManager().startCEdit(editor, undoText);


    }
    // ==========================================================================================================
    // ChangeListener interface
    // ==========================================================================================================   

    @Override
    public void stateChanged(ChangeEvent e)
    {
        Humanizer.Config cfg = null;
        if (e.getSource() == sld_timing)
        {
            cfg = humanizer.getConfig().setTimingRandomness(sld_timing.getValue() / 100f);
        } else if (e.getSource() == sld_timingBias)
        {
            cfg = humanizer.getConfig().setTimingBias(sld_timingBias.getValue() / 100f);
        } else if (e.getSource() == sld_velocity)
        {
            cfg = humanizer.getConfig().setVelocityRandomness(sld_velocity.getValue() / 100f);
        } else
        {
            throw new IllegalStateException("e.getSource()=" + e.getSource());
        }

        changeConfig(cfg);
    }

    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================    

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // LOGGER.log(Level.SEVERE, "propertyChange() -- evt={0}", Utilities.toDebugString(evt));

        if (evt.getSource() == humanizer)
        {
            switch (evt.getPropertyName())
            {
                case Humanizer.PROP_USER_CONFIG, Humanizer.PROP_STATE -> refreshUI();
                default ->
                {
                }
            }
        } else if (evt.getSource() == MusicController.getInstance())
        {
            switch (evt.getPropertyName())
            {
                case MusicController.PROP_STATE ->
                {
                    MusicController.State state = (MusicController.State) evt.getNewValue();
                    if (!state.equals(MusicController.State.PLAYING))
                    {
                        tbtn_play.setSelected(false);
                    }
                }
                default ->
                {
                }
            }
        }
    }
    // ===============================================================================================
    // Private methods
    // ===============================================================================================

    private void refreshUI()
    {
        registerOrUnregisterSliders(false);
        var config = humanizer.getConfig();
        var intValue = (int) (100 * config.timingRandomness());
        sld_timing.setValue(intValue);
        var strValue = String.format("%1$5d%%", intValue);
        lbl_timingValue.setText(strValue);

        intValue = (int) (100 * config.timingBias());
        sld_timingBias.setValue(intValue);
        strValue = String.format("%1$5d%%", intValue);
        lbl_timingBiasValue.setText(strValue);

        intValue = (int) (100 * config.velocityRandomness());
        sld_velocity.setValue(intValue);
        strValue = String.format("%1$5d%%", intValue);
        lbl_velocityValue.setText(strValue);
        registerOrUnregisterSliders(true);


        boolean humanizing = humanizer.getState().equals(Humanizer.State.HUMANIZING);
        boolean noNotes = humanizer.getRegisteredNotes().isEmpty();
        btn_humanize.setEnabled(!noNotes);
        btn_confirm.setEnabled(humanizing);
    }


    private void changeConfig(Config cfg)
    {
        humanizer.setConfig(cfg);
        if (humanizer.getState().equals(Humanizer.State.HUMANIZING))
        {
            humanizer.humanize();
        }
    }

    private void doHumanize()
    {
        if (humanizer.getState().equals(Humanizer.State.HUMANIZING))
        {
            humanizer.newSeed();
        }
        humanizer.humanize();       // this will update state to State.HUMANIZED
    }

    private void doCancel()
    {
        // Cancel all humanize SimpleEdits
        editor.getUndoManager().abortCEdit(undoText, null);

        exit();

    }

    private void doOk()
    {
        // Complete the undoable edit
        editor.getUndoManager().endCEdit(undoText);

        prefs.put(PREF_CONFIG, humanizer.getConfig().toSaveString());

        exit();

    }

    private void registerOrUnregisterSliders(boolean register)
    {
        if (register)
        {
            sld_timing.addChangeListener(this);
            sld_timingBias.addChangeListener(this);
            sld_velocity.addChangeListener(this);
        } else
        {
            sld_timing.removeChangeListener(this);
            sld_timingBias.removeChangeListener(this);
            sld_velocity.removeChangeListener(this);
        }
    }

    private void setSelectedNotesOnly(boolean b)
    {
        var mc = MusicController.getInstance();
        mc.stop();


        boolean wasHumanizing = humanizer.getState() == State.HUMANIZING;

        if (wasHumanizing)
        {
            // Restore all notes to their original position
            var cfg = humanizer.getConfig();
            humanizer.setConfig(Humanizer.ZERO_CONFIG);
            humanizer.humanize();
            humanizer.setConfig(cfg);
        }

        humanizer.reset();
        var nes = b ? editor.getSelectedNoteEvents() : editor.getModel();
        humanizer.registerNotes(nes);

        if (wasHumanizing)
        {
            humanizer.humanize();
        }
    }

    private void playHumanizedNotes(boolean play)
    {

        if (play)
        {
            // Set the loop zone
            IntRange barRange = null;     // By default play the whole phrase
            if (cb_selectedNotesOnly.isSelected())
            {
                var selNotes = editor.getSelectedNoteEvents();
                assert !selNotes.isEmpty();
                int barFrom = editor.toPosition(selNotes.get(0).getPositionInBeats()).getBar();
                int barTo = editor.toPosition(selNotes.get(selNotes.size() - 1).getPositionInBeats()).getBar();
                barRange = new IntRange(barFrom, barTo);
            }
            editor.setLoopZone(barRange);


            // Start playback in loop mode
            var playAction = new PlayLoopZone(editorTc);
            playAction.actionPerformed(null);
        } else
        {
            // Stop the music
            var mc = MusicController.getInstance();
            mc.stop();
        }
    }

    private void exit()
    {
        var mc = MusicController.getInstance();
        mc.stop();
        mc.removePropertyChangeListener(this);
        setVisible(false);
        dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jPanel1 = new javax.swing.JPanel();
        btn_cancel = new javax.swing.JButton();
        btn_humanize = new javax.swing.JButton();
        btn_confirm = new javax.swing.JButton();
        pnl_sliders = new javax.swing.JPanel();
        sld_timingBias = new javax.swing.JSlider();
        lbl_velocityValue = new javax.swing.JLabel();
        lbl_timingBiasText = new javax.swing.JLabel();
        lbl_timingText = new javax.swing.JLabel();
        sld_timing = new javax.swing.JSlider();
        lbl_velocityText = new javax.swing.JLabel();
        lbl_timingValue = new javax.swing.JLabel();
        lbl_timingBiasValue = new javax.swing.JLabel();
        sld_velocity = new javax.swing.JSlider();
        cb_selectedNotesOnly = new javax.swing.JCheckBox();
        tbtn_play = new javax.swing.JToggleButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.title")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                formWindowClosing(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_cancel, org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.btn_cancel.text")); // NOI18N
        btn_cancel.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.btn_cancel.toolTipText")); // NOI18N
        btn_cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_humanize, org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.btn_humanize.text")); // NOI18N
        btn_humanize.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.btn_humanize.toolTipText")); // NOI18N
        btn_humanize.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_humanizeActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_confirm, org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.btn_confirm.text")); // NOI18N
        btn_confirm.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.btn_confirm.toolTipText")); // NOI18N
        btn_confirm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_confirmActionPerformed(evt);
            }
        });

        sld_timingBias.setMaximum(50);
        sld_timingBias.setMinimum(-50);
        sld_timingBias.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.sld_timingBias.toolTipText")); // NOI18N
        sld_timingBias.setValue(0);
        sld_timingBias.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                sld_timingBiasMouseClicked(evt);
            }
        });

        lbl_velocityValue.setFont(lbl_velocityValue.getFont().deriveFont(lbl_velocityValue.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_velocityValue, "75%"); // NOI18N
        lbl_velocityValue.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        lbl_velocityValue.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_velocityValueMouseClicked(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_timingBiasText, org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.lbl_timingBiasText.text")); // NOI18N
        lbl_timingBiasText.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingBiasTextMouseClicked(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_timingText, org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.lbl_timingText.text")); // NOI18N
        lbl_timingText.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingTextMouseClicked(evt);
            }
        });

        sld_timing.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.sld_timing.toolTipText")); // NOI18N
        sld_timing.setValue(0);
        sld_timing.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                sld_timingMouseClicked(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_velocityText, org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.lbl_velocityText.text")); // NOI18N
        lbl_velocityText.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_velocityTextMouseClicked(evt);
            }
        });

        lbl_timingValue.setFont(lbl_timingValue.getFont().deriveFont(lbl_timingValue.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_timingValue, "30%"); // NOI18N
        lbl_timingValue.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        lbl_timingValue.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingValueMouseClicked(evt);
            }
        });

        lbl_timingBiasValue.setFont(lbl_timingBiasValue.getFont().deriveFont(lbl_timingBiasValue.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_timingBiasValue, "+20%"); // NOI18N
        lbl_timingBiasValue.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        lbl_timingBiasValue.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingBiasValueMouseClicked(evt);
            }
        });

        sld_velocity.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.sld_velocity.toolTipText")); // NOI18N
        sld_velocity.setValue(0);
        sld_velocity.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                sld_velocityMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout pnl_slidersLayout = new javax.swing.GroupLayout(pnl_sliders);
        pnl_sliders.setLayout(pnl_slidersLayout);
        pnl_slidersLayout.setHorizontalGroup(
            pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_slidersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_timingText)
                    .addComponent(lbl_velocityText)
                    .addComponent(lbl_timingBiasText))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(sld_timing, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                    .addComponent(sld_velocity, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE)
                    .addComponent(sld_timingBias, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_velocityValue, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lbl_timingBiasValue, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lbl_timingValue, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        pnl_slidersLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lbl_timingBiasValue, lbl_timingValue, lbl_velocityValue});

        pnl_slidersLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lbl_timingBiasText, lbl_timingText, lbl_velocityText});

        pnl_slidersLayout.setVerticalGroup(
            pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_slidersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sld_timing, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_timingText)
                    .addComponent(lbl_timingValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_timingBiasText)
                    .addComponent(sld_timingBias, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_timingBiasValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbl_velocityText)
                        .addComponent(sld_velocity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lbl_velocityValue))
                .addContainerGap(9, Short.MAX_VALUE))
        );

        org.openide.awt.Mnemonics.setLocalizedText(cb_selectedNotesOnly, org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.cb_selectedNotesOnly.text")); // NOI18N
        cb_selectedNotesOnly.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.cb_selectedNotesOnly.toolTipText")); // NOI18N
        cb_selectedNotesOnly.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_selectedNotesOnlyActionPerformed(evt);
            }
        });

        tbtn_play.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/pianoroll/actions/resources/PlayEditor-OFF.png"))); // NOI18N
        tbtn_play.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizeDialog.class, "HumanizeDialog.tbtn_play.toolTipText")); // NOI18N
        tbtn_play.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tbtn_playActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnl_sliders, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btn_humanize)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_confirm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_cancel))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(cb_selectedNotesOnly)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(tbtn_play)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cb_selectedNotesOnly)
                    .addComponent(tbtn_play))
                .addGap(18, 18, 18)
                .addComponent(pnl_sliders, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_cancel)
                    .addComponent(btn_humanize)
                    .addComponent(btn_confirm))
                .addContainerGap())
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cancelActionPerformed
    {//GEN-HEADEREND:event_btn_cancelActionPerformed
        doCancel();
    }//GEN-LAST:event_btn_cancelActionPerformed

    private void btn_humanizeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_humanizeActionPerformed
    {//GEN-HEADEREND:event_btn_humanizeActionPerformed
        doHumanize();
    }//GEN-LAST:event_btn_humanizeActionPerformed

    private void btn_confirmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_confirmActionPerformed
    {//GEN-HEADEREND:event_btn_confirmActionPerformed
        doOk();
    }//GEN-LAST:event_btn_confirmActionPerformed

    private void sld_timingBiasMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_sld_timingBiasMouseClicked
    {//GEN-HEADEREND:event_sld_timingBiasMouseClicked
        if (evt.getClickCount() == 2)
        {
            var cfg = humanizer.getConfig().setTimingBias(0);
            changeConfig(cfg);
        }
    }//GEN-LAST:event_sld_timingBiasMouseClicked

    private void lbl_velocityValueMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_velocityValueMouseClicked
    {//GEN-HEADEREND:event_lbl_velocityValueMouseClicked
        sld_velocityMouseClicked(evt);
    }//GEN-LAST:event_lbl_velocityValueMouseClicked

    private void lbl_timingBiasTextMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_timingBiasTextMouseClicked
    {//GEN-HEADEREND:event_lbl_timingBiasTextMouseClicked
        sld_timingBiasMouseClicked(evt);
    }//GEN-LAST:event_lbl_timingBiasTextMouseClicked

    private void lbl_timingTextMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_timingTextMouseClicked
    {//GEN-HEADEREND:event_lbl_timingTextMouseClicked
        sld_timingMouseClicked(evt);
    }//GEN-LAST:event_lbl_timingTextMouseClicked

    private void lbl_velocityTextMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_velocityTextMouseClicked
    {//GEN-HEADEREND:event_lbl_velocityTextMouseClicked
        sld_velocityMouseClicked(evt);
    }//GEN-LAST:event_lbl_velocityTextMouseClicked

    private void lbl_timingValueMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_timingValueMouseClicked
    {//GEN-HEADEREND:event_lbl_timingValueMouseClicked
        sld_timingMouseClicked(evt);
    }//GEN-LAST:event_lbl_timingValueMouseClicked

    private void lbl_timingBiasValueMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_timingBiasValueMouseClicked
    {//GEN-HEADEREND:event_lbl_timingBiasValueMouseClicked
        sld_timingBiasMouseClicked(evt);
    }//GEN-LAST:event_lbl_timingBiasValueMouseClicked

    private void sld_velocityMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_sld_velocityMouseClicked
    {//GEN-HEADEREND:event_sld_velocityMouseClicked
        if (evt.getClickCount() == 2)
        {
            var cfg = humanizer.getConfig().setVelocityRandomness(0);
            changeConfig(cfg);
        }
    }//GEN-LAST:event_sld_velocityMouseClicked

    private void sld_timingMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_sld_timingMouseClicked
    {//GEN-HEADEREND:event_sld_timingMouseClicked
        if (evt.getClickCount() == 2)
        {
            var cfg = humanizer.getConfig().setTimingRandomness(0);
            changeConfig(cfg);
        }
    }//GEN-LAST:event_sld_timingMouseClicked

    private void cb_selectedNotesOnlyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_selectedNotesOnlyActionPerformed
    {//GEN-HEADEREND:event_cb_selectedNotesOnlyActionPerformed
        setSelectedNotesOnly(cb_selectedNotesOnly.isSelected());
    }//GEN-LAST:event_cb_selectedNotesOnlyActionPerformed

    private void tbtn_playActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tbtn_playActionPerformed
    {//GEN-HEADEREND:event_tbtn_playActionPerformed
        playHumanizedNotes(tbtn_play.isSelected());
    }//GEN-LAST:event_tbtn_playActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        // User press the upper right window close button
        doCancel();
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_cancel;
    private javax.swing.JButton btn_confirm;
    private javax.swing.JButton btn_humanize;
    private javax.swing.JCheckBox cb_selectedNotesOnly;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lbl_timingBiasText;
    private javax.swing.JLabel lbl_timingBiasValue;
    private javax.swing.JLabel lbl_timingText;
    private javax.swing.JLabel lbl_timingValue;
    private javax.swing.JLabel lbl_velocityText;
    private javax.swing.JLabel lbl_velocityValue;
    private javax.swing.JPanel pnl_sliders;
    private javax.swing.JSlider sld_timing;
    private javax.swing.JSlider sld_timingBias;
    private javax.swing.JSlider sld_velocity;
    private javax.swing.JToggleButton tbtn_play;
    // End of variables declaration//GEN-END:variables


}
