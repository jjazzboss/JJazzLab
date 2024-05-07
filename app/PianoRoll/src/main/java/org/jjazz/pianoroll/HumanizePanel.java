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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.flatcomponents.api.CollapsiblePanel;
import org.jjazz.humanizer.api.Humanizer;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;

/**
 * Humanize panel.
 */
public class HumanizePanel extends javax.swing.JPanel implements ChangeListener, PropertyChangeListener
{

    private Humanizer humanizer;
    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(HumanizePanel.class.getSimpleName());


    public HumanizePanel(PianoRollEditor editor)
    {
        this.editor = editor;
        this.editor.addPropertyChangeListener(this);

        initComponents();


        initializeNewHumanizer();
    }


    public void cleanup()
    {
        editor.removePropertyChangeListener(this);
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
    }

    // ===============================================================================================
    // ChangeListener
    // ===============================================================================================

    @Override
    public void stateChanged(ChangeEvent e)
    {
        refreshUI();
    }
    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================    

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.log(Level.FINE, "propertyChange() -- evt={0}", evt);

        if (evt.getSource() == editor)
        {
            switch (evt.getPropertyName())
            {
                case PianoRollEditor.PROP_MODEL_PHRASE -> initializeNewHumanizer();

                case PianoRollEditor.PROP_SELECTED_NOTE_VIEWS ->
                {
                    var nes = NoteView.getNotes((Collection<NoteView>) evt.getOldValue());
                    boolean b = (boolean) evt.getNewValue();
                    if (b)
                    {
                        humanizer.addNotes(nes);
                    } else
                    {
                        humanizer.removeNotes(nes);
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

        boolean b = config.equals(Humanizer.DEFAULT_CONFIG);
        btn_confirm.setEnabled(!b);
        btn_cancel.setEnabled(!b);
    }

    private void initializeNewHumanizer()
    {
        if (humanizer != null)
        {
            humanizer.removeChangeListener(this);
        }
        Phrase p = editor.getModel();
        var selectedNotes = NoteView.getNotes(editor.getSelectedNoteViews());
        humanizer = new Humanizer(p, editor.getPhraseBeatRange(), selectedNotes, null, editor.getSong().getTempo());
        humanizer.addChangeListener(this);
        refreshUI();
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        sld_timingBias = new javax.swing.JSlider();
        sld_timing = new javax.swing.JSlider();
        lbl_timingText = new javax.swing.JLabel();
        lbl_timingBiasText = new javax.swing.JLabel();
        sld_velocity = new javax.swing.JSlider();
        lbl_velocityText = new javax.swing.JLabel();
        lbl_timingValue = new javax.swing.JLabel();
        lbl_timingBiasValue = new javax.swing.JLabel();
        lbl_velocityValue = new javax.swing.JLabel();
        btn_cancel = new javax.swing.JButton();
        btn_confirm = new javax.swing.JButton();
        btn_newSeed = new javax.swing.JButton();

        sld_timingBias.setMaximum(50);
        sld_timingBias.setMinimum(-50);
        sld_timingBias.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.sld_timingBias.toolTipText")); // NOI18N
        sld_timingBias.setValue(0);
        sld_timingBias.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                sld_timingBiasStateChanged(evt);
            }
        });
        sld_timingBias.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingBiasValueMouseClicked(evt);
            }
        });

        sld_timing.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.sld_timing.toolTipText")); // NOI18N
        sld_timing.setValue(0);
        sld_timing.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                sld_timingStateChanged(evt);
            }
        });
        sld_timing.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingTextMouseClicked(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_timingText, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.lbl_timingText.text")); // NOI18N
        lbl_timingText.setToolTipText(sld_timing.getToolTipText());
        lbl_timingText.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingTextMouseClicked(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_timingBiasText, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.lbl_timingBiasText.text")); // NOI18N
        lbl_timingBiasText.setToolTipText(sld_timingBias.getToolTipText());
        lbl_timingBiasText.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingBiasValueMouseClicked(evt);
            }
        });

        sld_velocity.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.sld_velocity.toolTipText")); // NOI18N
        sld_velocity.setValue(0);
        sld_velocity.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                sld_velocityStateChanged(evt);
            }
        });
        sld_velocity.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_velocityTextMouseClicked(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_velocityText, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.lbl_velocityText.text")); // NOI18N
        lbl_velocityText.setToolTipText(sld_velocity.getToolTipText());
        lbl_velocityText.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_velocityTextMouseClicked(evt);
            }
        });

        lbl_timingValue.setFont(lbl_timingValue.getFont().deriveFont(lbl_timingValue.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_timingValue, "30%"); // NOI18N
        lbl_timingValue.setToolTipText(sld_timing.getToolTipText());
        lbl_timingValue.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingTextMouseClicked(evt);
            }
        });

        lbl_timingBiasValue.setFont(lbl_timingBiasValue.getFont().deriveFont(lbl_timingBiasValue.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_timingBiasValue, "+20%"); // NOI18N
        lbl_timingBiasValue.setToolTipText(sld_timingBias.getToolTipText());
        lbl_timingBiasValue.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_timingBiasValueMouseClicked(evt);
            }
        });

        lbl_velocityValue.setFont(lbl_velocityValue.getFont().deriveFont(lbl_velocityValue.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_velocityValue, "75%"); // NOI18N
        lbl_velocityValue.setToolTipText(sld_velocity.getToolTipText());
        lbl_velocityValue.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lbl_velocityTextMouseClicked(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_cancel, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.btn_cancel.text")); // NOI18N
        btn_cancel.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.btn_cancel.toolTipText")); // NOI18N
        btn_cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_confirm, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.btn_confirm.text")); // NOI18N
        btn_confirm.setToolTipText(org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.btn_confirm.toolTipText")); // NOI18N
        btn_confirm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_confirmActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_newSeed, org.openide.util.NbBundle.getMessage(HumanizePanel.class, "HumanizePanel.btn_newSeed.text")); // NOI18N
        btn_newSeed.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_newSeedActionPerformed(evt);
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
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_timingText)
                            .addComponent(lbl_velocityText)
                            .addComponent(lbl_timingBiasText))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(sld_timingBias, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(6, 6, 6)
                                        .addComponent(sld_velocity, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbl_timingBiasValue, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(lbl_velocityValue, javax.swing.GroupLayout.Alignment.TRAILING)))
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sld_timing, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_timingValue))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btn_newSeed)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                        .addComponent(btn_confirm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_cancel)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lbl_timingBiasValue, lbl_timingValue, lbl_velocityValue});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sld_timing, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_timingText)
                    .addComponent(lbl_timingValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_timingBiasText)
                    .addComponent(sld_timingBias, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_timingBiasValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_velocityText)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sld_velocity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbl_velocityValue)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_cancel)
                    .addComponent(btn_confirm)
                    .addComponent(btn_newSeed))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btn_confirmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_confirmActionPerformed
    {//GEN-HEADEREND:event_btn_confirmActionPerformed
        initializeNewHumanizer();
    }//GEN-LAST:event_btn_confirmActionPerformed

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cancelActionPerformed
    {//GEN-HEADEREND:event_btn_cancelActionPerformed
        humanizer.humanize(Humanizer.DEFAULT_CONFIG);
        refreshUI();

    }//GEN-LAST:event_btn_cancelActionPerformed

    private void sld_timingStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_sld_timingStateChanged
    {//GEN-HEADEREND:event_sld_timingStateChanged
        var cfg = humanizer.getUserConfig().getCopy(sld_timing.getValue() / 100f, -1, -1);
        humanizer.humanize(cfg);
    }//GEN-LAST:event_sld_timingStateChanged

    private void sld_timingBiasStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_sld_timingBiasStateChanged
    {//GEN-HEADEREND:event_sld_timingBiasStateChanged
        var cfg = humanizer.getUserConfig().getCopy(-1, sld_timingBias.getValue() / 100f, -1);
        humanizer.humanize(cfg);
    }//GEN-LAST:event_sld_timingBiasStateChanged

    private void sld_velocityStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_sld_velocityStateChanged
    {//GEN-HEADEREND:event_sld_velocityStateChanged
        var cfg = humanizer.getUserConfig().getCopy(-1, -1, sld_velocity.getValue() / 100f);
        humanizer.humanize(cfg);
    }//GEN-LAST:event_sld_velocityStateChanged

    private void lbl_timingTextMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_timingTextMouseClicked
    {//GEN-HEADEREND:event_lbl_timingTextMouseClicked
        if (evt.getClickCount() == 2)
        {
            var cfg = humanizer.getUserConfig().getCopy(0, -1, -1);
            humanizer.humanize(cfg);
        }
    }//GEN-LAST:event_lbl_timingTextMouseClicked

    private void lbl_timingBiasValueMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_timingBiasValueMouseClicked
    {//GEN-HEADEREND:event_lbl_timingBiasValueMouseClicked
        if (evt.getClickCount() == 2)
        {
            var cfg = humanizer.getUserConfig().getCopy(-1, 0, -1);
            humanizer.humanize(cfg);
        }
    }//GEN-LAST:event_lbl_timingBiasValueMouseClicked

    private void lbl_velocityTextMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_velocityTextMouseClicked
    {//GEN-HEADEREND:event_lbl_velocityTextMouseClicked
        if (evt.getClickCount() == 2)
        {
            var cfg = humanizer.getUserConfig().getCopy(-1, -1, 0);
            humanizer.humanize(cfg);
        }
    }//GEN-LAST:event_lbl_velocityTextMouseClicked

    private void btn_newSeedActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_newSeedActionPerformed
    {//GEN-HEADEREND:event_btn_newSeedActionPerformed
        humanizer.newSeed();
    }//GEN-LAST:event_btn_newSeedActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_cancel;
    private javax.swing.JButton btn_confirm;
    private javax.swing.JButton btn_newSeed;
    private javax.swing.JLabel lbl_timingBiasText;
    private javax.swing.JLabel lbl_timingBiasValue;
    private javax.swing.JLabel lbl_timingText;
    private javax.swing.JLabel lbl_timingValue;
    private javax.swing.JLabel lbl_velocityText;
    private javax.swing.JLabel lbl_velocityValue;
    private javax.swing.JSlider sld_timing;
    private javax.swing.JSlider sld_timingBias;
    private javax.swing.JSlider sld_velocity;
    // End of variables declaration//GEN-END:variables


    // ===============================================================================================
    // Inner classes
    // ===============================================================================================
}
