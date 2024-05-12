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
package org.jjazz.pianoroll;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.flatcomponents.api.CollapsiblePanel;
import org.jjazz.humanizer.api.Humanizer;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.Utilities;

/**
 * The humanize notes panel.
 */
public class HumanizePanel extends javax.swing.JPanel implements PropertyChangeListener, ChangeListener
{

    private Humanizer humanizer;
    private final PianoRollEditor editor;
    private final String undoText;
    private static final Logger LOGGER = Logger.getLogger(HumanizePanel.class.getSimpleName());


    public HumanizePanel(PianoRollEditor editor)
    {
        this.editor = editor;
        this.editor.addPropertyChangeListener(this);

        initComponents();
        registerOrUnregisterSliders(true);
        lbl_timingText.setToolTipText(sld_timing.getToolTipText());
        lbl_timingValue.setToolTipText(sld_timing.getToolTipText());
        lbl_timingBiasText.setToolTipText(sld_timingBias.getToolTipText());
        lbl_timingBiasValue.setToolTipText(sld_timingBias.getToolTipText());
        lbl_velocityText.setToolTipText(sld_velocity.getToolTipText());
        lbl_velocityValue.setToolTipText(sld_velocity.getToolTipText());
        undoText = btn_humanize.getText();


        initializeNewHumanizer();

        refreshUI();
    }

    public void cleanup()
    {
        editor.removePropertyChangeListener(this);
        humanizer.cleanup();
    }

    /**
     * Get notified when this panel is shown (it assumes that container is a CollapsiblePanel).
     */
    @Override
    public void addNotify()
    {
        super.addNotify();
        assert getParent().getParent() instanceof CollapsiblePanel : "getParent().getParent()=" + getParent().getParent();
    }

    /**
     * Get notified when this panel is hidden (it assumes that container is a CollapsiblePanel).
     */
    @Override
    public void removeNotify()
    {
        super.removeNotify();
        // LOGGER.severe("removeNotify()");
        if (humanizer.getState().equals(Humanizer.State.HUMANIZED))
        {
            confirmChanges();
        }
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
            cfg = humanizer.getUserConfig().setTimingRandomness(sld_timing.getValue() / 100f);
        } else if (e.getSource() == sld_timingBias)
        {
            cfg = humanizer.getUserConfig().setTimingBias(sld_timingBias.getValue() / 100f);
        } else if (e.getSource() == sld_velocity)
        {
            cfg = humanizer.getUserConfig().setVelocityRandomness(sld_velocity.getValue() / 100f);
        } else
        {
            throw new IllegalStateException("e.getSource()=" + e.getSource());
        }

        humanizer.humanize(cfg);
    }

    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================    

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // LOGGER.log(Level.SEVERE, "propertyChange() -- evt={0}", Utilities.toDebugString(evt));

        if (evt.getSource() == editor)
        {
            switch (evt.getPropertyName())
            {
                case PianoRollEditor.PROP_MODEL_PHRASE ->
                {
                    initializeNewHumanizer();
                    refreshUI();
                }
                case PianoRollEditor.PROP_SELECTED_NOTE_VIEWS ->
                {
                    if (humanizer.getState().equals(Humanizer.State.HUMANIZED))
                    {
                        humanizer.reset(null);
                    }
                    refreshUI();
                }
                default ->
                {
                }
            }
        } else if (evt.getSource() == humanizer)
        {
            switch (evt.getPropertyName())
            {
                case Humanizer.PROP_USER_CONFIG -> refreshUI();
                case Humanizer.PROP_STATE ->
                {
                    var newState = (Humanizer.State) evt.getNewValue();
                    if (newState.equals(Humanizer.State.INIT))
                    {
                        confirmChanges();
                    }
                    refreshUI();
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
        var config = humanizer.getUserConfig();
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


        boolean humanizeState = humanizer.getState().equals(Humanizer.State.HUMANIZED);
        boolean zeroConfig = config.equals(Humanizer.ZERO_CONFIG);
        if (humanizeState)
        {
            UIUtilities.enableContainer(pnl_sliders);
        } else
        {
            UIUtilities.disableContainer(pnl_sliders);
        }
        btn_humanize.setEnabled(!editor.getSelectedNoteViews().isEmpty() && (!humanizeState || !zeroConfig));
        btn_confirm.setEnabled(humanizeState);
        btn_cancel.setEnabled(humanizeState && !zeroConfig);

    }

    private void startHumanize()
    {
        if (humanizer.getState().equals(Humanizer.State.HUMANIZED))
        {
            humanizer.newSeed();
        } else
        {
            editor.getUndoManager().startCEdit(editor, undoText);

            humanizer.registerNotes(NoteView.getNotes(editor.getSelectedNoteViews()));
            humanizer.humanize(null);       // this will update state to State.HUMANIZED
        }
    }

    private void cancelChanges()
    {
        // Cancel all humanize SimpleEdits
        editor.getUndoManager().abortCEdit(undoText, null);
        humanizer.reset(null);
    }

    private void confirmChanges()
    {
        // We did some changes as part of the undoAction, complete it
        editor.getUndoManager().endCEdit(undoText);
        humanizer.reset(null);
    }

    private void initializeNewHumanizer()
    {
        if (humanizer != null)
        {
            humanizer.removePropertyChangeListener(this);
            humanizer.cleanup();
        }
        Phrase p = editor.getModel();
        var beatRange = editor.getPhraseBeatRange();
        var ts = editor.getTimeSignature(beatRange.from);       // Not perfect, but for now Humanizer can't manage multiple time signatures
        humanizer = new Humanizer(p, ts, beatRange, null, editor.getSong().getTempo());
        humanizer.addPropertyChangeListener(this);
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

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

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

        org.openide.awt.Mnemonics.setLocalizedText(btn_cancel, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.btn_cancel.text")); // NOI18N
        btn_cancel.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.btn_cancel.toolTipText")); // NOI18N
        btn_cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_humanize, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.btn_humanize.text")); // NOI18N
        btn_humanize.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.btn_humanize.toolTipText")); // NOI18N
        btn_humanize.setEnabled(false);
        btn_humanize.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_humanizeActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_confirm, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.btn_confirm.text")); // NOI18N
        btn_confirm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_confirmActionPerformed(evt);
            }
        });

        sld_timingBias.setMaximum(50);
        sld_timingBias.setMinimum(-50);
        sld_timingBias.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.sld_timingBias.toolTipText")); // NOI18N
        sld_timingBias.setValue(0);
        sld_timingBias.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingBiasValueMouseClicked(evt);
            }
        });

        lbl_velocityValue.setFont(lbl_velocityValue.getFont().deriveFont(lbl_velocityValue.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_velocityValue, "75%"); // NOI18N
        lbl_velocityValue.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        lbl_velocityValue.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_velocityTextMouseClicked(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_timingBiasText, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.lbl_timingBiasText.text")); // NOI18N
        lbl_timingBiasText.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingBiasValueMouseClicked(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_timingText, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.lbl_timingText.text")); // NOI18N
        lbl_timingText.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingTextMouseClicked(evt);
            }
        });

        sld_timing.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.sld_timing.toolTipText")); // NOI18N
        sld_timing.setValue(0);
        sld_timing.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingTextMouseClicked(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_velocityText, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.lbl_velocityText.text")); // NOI18N
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
                lbl_timingTextMouseClicked(evt);
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

        sld_velocity.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.sld_velocity.toolTipText")); // NOI18N
        sld_velocity.setValue(0);
        sld_velocity.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_velocityTextMouseClicked(evt);
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
                .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(pnl_slidersLayout.createSequentialGroup()
                        .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnl_slidersLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sld_timingBias, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE))
                            .addGroup(pnl_slidersLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(sld_velocity, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_timingBiasValue, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lbl_velocityValue, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(pnl_slidersLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sld_timing, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_timingValue)))
                .addContainerGap())
        );

        pnl_slidersLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lbl_timingBiasValue, lbl_timingValue, lbl_velocityValue});

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
                    .addComponent(lbl_velocityText)
                    .addGroup(pnl_slidersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sld_velocity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbl_velocityValue)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_humanize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
                .addComponent(btn_confirm)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_cancel)
                .addContainerGap())
            .addComponent(pnl_sliders, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnl_sliders, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_cancel)
                    .addComponent(btn_humanize)
                    .addComponent(btn_confirm))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cancelActionPerformed
    {//GEN-HEADEREND:event_btn_cancelActionPerformed
        cancelChanges();
    }//GEN-LAST:event_btn_cancelActionPerformed


    private void lbl_timingTextMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_timingTextMouseClicked
    {//GEN-HEADEREND:event_lbl_timingTextMouseClicked
        if (evt.getClickCount() == 2)
        {
            var cfg = humanizer.getUserConfig().setTimingRandomness(0);
            humanizer.humanize(cfg);
        }
    }//GEN-LAST:event_lbl_timingTextMouseClicked

    private void lbl_timingBiasValueMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_timingBiasValueMouseClicked
    {//GEN-HEADEREND:event_lbl_timingBiasValueMouseClicked
        if (evt.getClickCount() == 2)
        {
            var cfg = humanizer.getUserConfig().setTimingBias(0);
            humanizer.humanize(cfg);
        }
    }//GEN-LAST:event_lbl_timingBiasValueMouseClicked

    private void lbl_velocityTextMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_velocityTextMouseClicked
    {//GEN-HEADEREND:event_lbl_velocityTextMouseClicked
        if (evt.getClickCount() == 2)
        {
            var cfg = humanizer.getUserConfig().setVelocityRandomness(0);
            humanizer.humanize(cfg);
        }
    }//GEN-LAST:event_lbl_velocityTextMouseClicked

    private void btn_humanizeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_humanizeActionPerformed
    {//GEN-HEADEREND:event_btn_humanizeActionPerformed
        startHumanize();
    }//GEN-LAST:event_btn_humanizeActionPerformed

    private void btn_confirmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_confirmActionPerformed
    {//GEN-HEADEREND:event_btn_confirmActionPerformed
        confirmChanges();
    }//GEN-LAST:event_btn_confirmActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_cancel;
    private javax.swing.JButton btn_confirm;
    private javax.swing.JButton btn_humanize;
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
    // End of variables declaration//GEN-END:variables


    // ===============================================================================================
    // Inner classes
    // ===============================================================================================
}
