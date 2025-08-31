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
package org.jjazz.rpcustomeditorfactoryimpl;

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorComponent;
import static org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorComponent.PROP_EDITED_RP_VALUE;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_RhythmCombinator;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_RhythmCombinatorValue;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythmselectiondialog.spi.RhythmSelectionDialogProvider;
import org.jjazz.uiutilities.api.UIUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

/**
 * A RP editor component for RP_RhythmCombinator.
 */
public class RP_SYS_RhythmCombinatorComp extends RealTimeRpEditorComponent<RP_SYS_RhythmCombinatorValue>
{

    private final RP_SYS_RhythmCombinator rp;
    private RP_SYS_RhythmCombinatorValue lastValue;
    private RP_SYS_RhythmCombinatorValue uiValue;
    private final RP_SYS_RhythmCombinatorValueTable tbl_mappings;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_RhythmCombinatorComp.class.getSimpleName());

    public RP_SYS_RhythmCombinatorComp(RP_SYS_RhythmCombinator rp)
    {
        checkNotNull(rp);
        this.rp = rp;

        initComponents();

        tbl_mappings = ((RP_SYS_RhythmCombinatorValueTable) table);

        // Add listeners
        tbl_mappings.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "AddMappings");
        tbl_mappings.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "ClearMappings");
        tbl_mappings.getActionMap().put("AddMappings", UIUtilities.getAction(evt -> setMapping(evt)));
        tbl_mappings.getActionMap().put("ClearMappings", UIUtilities.getAction(evt -> clearMapping(evt)));

        tbl_mappings.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                handleTableMouseClicked(e);
            }
        });
    }


    @Override
    public RP_SYS_RhythmCombinator getRhythmParameter()
    {
        return rp;
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        if (uiValue != null)
        {
            // Update UI
            refreshUI();
        }
    }


    @Override
    public void preset(RP_SYS_RhythmCombinatorValue rpValue, SongPartContext sptContext)
    {
        checkNotNull(sptContext);

        LOGGER.log(Level.FINE, "preset() -- rpValue={0} sptContext={1}", new Object[]
        {
            rpValue, sptContext
        });

        uiValue = null;
        setEditedRpValue(rpValue);
    }

    @Override
    public void setEditedRpValue(RP_SYS_RhythmCombinatorValue rpValue)
    {
        lastValue = uiValue;
        uiValue = rpValue;
        refreshUI();
        fireUiValueChanged();
    }

    @Override
    public RP_SYS_RhythmCombinatorValue getEditedRpValue()
    {
        return uiValue;
    }

    @Override
    public boolean isResizable()
    {
        return true;
    }


    @Override
    public void cleanup()
    {
    }


    // ===============================================================================
    // Private methods
    // ===============================================================================    
    /**
     * Refresh UI to match uiValue.
     */
    private void refreshUI()
    {
        lbl_rhythm.setText(uiValue.getBaseRhythm().getName());
        tbl_mappings.getModel().setRpValue(uiValue);
    }

    private void fireUiValueChanged()
    {
        LOGGER.log(Level.FINE, "fireUiValueChanged() -- lastvalue={0} uiValue={1}", new Object[]
        {
            lastValue, uiValue
        });
        firePropertyChange(PROP_EDITED_RP_VALUE, lastValue, uiValue);
    }

    private void handleTableMouseClicked(MouseEvent evt)
    {
        boolean ctrl = (evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        boolean shift = (evt.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;

        if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2)
        {
            setMapping(null);
        }
    }

    /**
     * Open the RhythmSelectionDialog to select a new mapping.
     *
     * @param evt
     */
    private void setMapping(ActionEvent evt)
    {
        var rvSrc = tbl_mappings.getSelectedRhythmVoice();
        if (uiValue == null || rvSrc == null)
        {
            return;
        }
        var oldRvDest = uiValue.getDestRhythmVoice(rvSrc);


        var dlg = RhythmSelectionDialogProvider.getDefault().getDialog();
        var rdb = RhythmDatabase.getDefault();
        var rv = oldRvDest == null ? rvSrc : oldRvDest;
        var ri = rdb.getRhythm(rv.getContainer().getUniqueId());
        dlg.preset(ri, null);
        dlg.setTitleText("Select a " + ri.timeSignature() + " destination rhythm");
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);


        // Dialog exited
        if (!dlg.isExitOk())
        {
            dlg.cleanup();
            return;
        }


        var rSrc = rvSrc.getContainer();
        var riSrc = rdb.getRhythm(rSrc.getUniqueId());
        var riNewDest = dlg.getSelectedRhythm();
        assert riNewDest != null;
        RhythmVoice newRvDest;


        if (oldRvDest != null && riNewDest.rhythmUniqueId().equals(oldRvDest.getContainer().getUniqueId()))
        {
            // no change
            newRvDest = oldRvDest;
        } else if (riNewDest == riSrc)
        {
            // clear existing mapping
            newRvDest = null;
        } else
        {
            // new mapping
            Rhythm rNewDest = null;
            try
            {
                rNewDest = rdb.getRhythmInstance(riNewDest);   // throws UnavailableRhythmException
            } catch (UnavailableRhythmException ex)
            {
                LOGGER.log(Level.WARNING, "setMapping() Unexpected error : can not retrieve rhytm instance for riDest={0}", riNewDest);
                dlg.cleanup();
                return;
            }

            newRvDest = rNewDest.getRhythmVoices().stream()
                    .filter(rvi -> rvi.getType() == rvSrc.getType())
                    .findFirst()
                    .orElse(null);

            if (newRvDest == null)
            {
                var msg = "Selected rhythm " + riNewDest + " does not have a compatible track";
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                LOGGER.log(Level.WARNING, "setMapping() {0}", msg);
            }
        }


        // update model
        if (newRvDest != oldRvDest)
        {
            setEditedRpValue(uiValue.set(rvSrc, newRvDest));
        }


        dlg.cleanup();
    }

    private void clearMapping(ActionEvent evt)
    {
        var rvSrc = tbl_mappings.getSelectedRhythmVoice();
        if (uiValue == null || rvSrc == null)
        {
            return;
        }
        var oldRvDest = uiValue.getDestRhythmVoice(rvSrc);
        if (oldRvDest != null)
        {
            setEditedRpValue(uiValue.set(rvSrc, null));
        }
    }

    // ===============================================================================
    // Inner classes
    // ===============================================================================    

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jLabel1 = new javax.swing.JLabel();
        lbl_rhythm = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new org.jjazz.rpcustomeditorfactoryimpl.RP_SYS_RhythmCombinatorValueTable();
        btn_clear = new javax.swing.JButton();
        btn_replace = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(RP_SYS_RhythmCombinatorComp.class, "RP_SYS_RhythmCombinatorComp.jLabel1.text")); // NOI18N

        lbl_rhythm.setFont(lbl_rhythm.getFont().deriveFont(lbl_rhythm.getFont().getStyle() | java.awt.Font.BOLD));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_rhythm, org.openide.util.NbBundle.getMessage(RP_SYS_RhythmCombinatorComp.class, "RP_SYS_RhythmCombinatorComp.lbl_rhythm.text")); // NOI18N

        jScrollPane1.setViewportView(table);

        org.openide.awt.Mnemonics.setLocalizedText(btn_clear, org.openide.util.NbBundle.getMessage(RP_SYS_RhythmCombinatorComp.class, "RP_SYS_RhythmCombinatorComp.btn_clear.text")); // NOI18N
        btn_clear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_clearActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_replace, org.openide.util.NbBundle.getMessage(RP_SYS_RhythmCombinatorComp.class, "RP_SYS_RhythmCombinatorComp.btn_replace.text")); // NOI18N
        btn_replace.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_RhythmCombinatorComp.class, "RP_SYS_RhythmCombinatorComp.btn_replace.toolTipText")); // NOI18N
        btn_replace.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_replaceActionPerformed(evt);
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
                        .addComponent(jScrollPane1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_clear)
                            .addComponent(btn_replace, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_rhythm)
                        .addGap(0, 547, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(lbl_rhythm))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btn_replace)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_clear)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btn_replaceActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_replaceActionPerformed
    {//GEN-HEADEREND:event_btn_replaceActionPerformed
        setMapping(evt);
    }//GEN-LAST:event_btn_replaceActionPerformed

    private void btn_clearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_clearActionPerformed
    {//GEN-HEADEREND:event_btn_clearActionPerformed
        clearMapping(evt);
    }//GEN-LAST:event_btn_clearActionPerformed



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_clear;
    private javax.swing.JButton btn_replace;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_rhythm;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables


}
