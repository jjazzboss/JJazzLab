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

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import static org.jjazz.rhythm.api.RhythmVoice.Type.CHORD1;
import static org.jjazz.rhythm.api.RhythmVoice.Type.CHORD2;
import static org.jjazz.rhythm.api.RhythmVoice.Type.PHRASE1;
import org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorComponent;
import static org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorComponent.PROP_EDITED_RP_VALUE;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_SubstituteTracks;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_SubstituteTracksValue;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythmselectiondialog.spi.RhythmSelectionDialogProvider;
import org.jjazz.uiutilities.api.ColumnLeftAlignedLayoutManager;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.windows.WindowManager;

/**
 * A RP editor component for RP_SYS_SubstituteTracks.
 */
public class RP_SYS_SubstituteTracksComp extends RealTimeRpEditorComponent<RP_SYS_SubstituteTracksValue> implements ListSelectionListener
{

    private final RP_SYS_SubstituteTracks rp;
    private RP_SYS_SubstituteTracksValue lastValue;
    private RP_SYS_SubstituteTracksValue uiValue;
    private final RP_SYS_SubstituteTracksValueTable tbl_mappings;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_SubstituteTracksComp.class.getSimpleName());

    public RP_SYS_SubstituteTracksComp(RP_SYS_SubstituteTracks rp)
    {
        checkNotNull(rp);
        this.rp = rp;

        initComponents();

        tbl_mappings = ((RP_SYS_SubstituteTracksValueTable) table);


        // Add listeners
        tbl_mappings.addPropertyChangeListener(RP_SYS_SubstituteTracksValueTable.PROP_RVDEST, e -> 
        {
            // User changed the destination track of a given rhythm
            RhythmVoice rvSrc = (RhythmVoice) e.getOldValue();
            RhythmVoice newRvDest = (RhythmVoice) e.getNewValue();
            setEditedRpValue(uiValue.set(rvSrc, newRvDest));
        });
        tbl_mappings.getSelectionModel().addListSelectionListener(this);
        tbl_mappings.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "AddMappings");
        tbl_mappings.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "ClearMappings");
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
    public RP_SYS_SubstituteTracks getRhythmParameter()
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
    public void preset(RP_SYS_SubstituteTracksValue rpValue, SongPartContext sptContext)
    {
        Objects.requireNonNull(rpValue);
        Objects.requireNonNull(sptContext);
        var spt0 = sptContext.getSongParts().get(0);
        Preconditions.checkArgument(rpValue.getBaseRhythm() == spt0.getRhythm(),
                "rpValue.getBaseRhythm()=" + rpValue.getBaseRhythm() + " spt0=" + spt0.getRhythm());

        LOGGER.log(Level.FINE, "preset() -- rpValue={0} sptContext={1}", new Object[]
        {
            rpValue, sptContext
        });

        uiValue = null;
        setEditedRpValue(rpValue);
    }

    @Override
    public void setEditedRpValue(RP_SYS_SubstituteTracksValue rpValue)
    {
        lastValue = uiValue;
        uiValue = rpValue;
        refreshUI();
        fireUiValueChanged();
    }

    @Override
    public RP_SYS_SubstituteTracksValue getEditedRpValue()
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
    // ListSelectionListener methods
    // ===============================================================================  
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (e.getValueIsAdjusting())
        {
            return;
        }

        var rvSrc = tbl_mappings.getSelectedBaseRhythmVoice();
        btn_replace.setEnabled(rvSrc != null);
        btn_clear.setEnabled(rvSrc != null && uiValue.getDestRhythmVoice(rvSrc) != null);

    }

    // ===============================================================================
    // Private methods
    // ===============================================================================    
    /**
     * Refresh UI to match uiValue.
     */
    private void refreshUI()
    {
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
            btn_replaceActionPerformed(null);
        }
    }

    /**
     * Open the RhythmSelectionDialog to select a new mapping.
     *
     * @param evt
     */
    private void setMapping(ActionEvent evt)
    {
        var rvSrc = tbl_mappings.getSelectedBaseRhythmVoice();
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
        dlg.setCustomComponent(new JLabel(" "));    
        dlg.setTitleText(ResUtil.getString(getClass(), "SelectSubstituteRhythm", ri.timeSignature()));
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


            // Try to find the most appropriate RhythmVoice for rvSrc            
            newRvDest = rNewDest.getRhythmVoices().stream()
                    .filter(rvi -> rvi.getType() == rvSrc.getType())
                    .findFirst()
                    .orElse(null);
            if (newRvDest == null)
            {
                RhythmVoice.Type testType = switch (rvSrc.getType())
                {
                    case DRUMS ->
                        RhythmVoice.Type.PERCUSSION;
                    case PERCUSSION ->
                        RhythmVoice.Type.DRUMS;
                    case CHORD1 ->
                        RhythmVoice.Type.CHORD2;
                    case CHORD2, PAD, OTHER ->
                        RhythmVoice.Type.CHORD1;
                    case PHRASE1 ->
                        RhythmVoice.Type.PHRASE2;
                    case PHRASE2 ->
                        RhythmVoice.Type.PHRASE1;
                    default ->
                        null;
                };
                newRvDest = rNewDest.getRhythmVoices().stream()
                        .filter(rvi -> rvi.getType() == testType)
                        .findFirst()
                        .orElse(null);
                if (newRvDest == null)
                {
                    newRvDest = rNewDest.getRhythmVoices().get(0);
                }
            }
            assert newRvDest != null;
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
        var rvSrc = tbl_mappings.getSelectedBaseRhythmVoice();
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

        jScrollPane1 = new javax.swing.JScrollPane();
        table = new org.jjazz.rpcustomeditorfactoryimpl.RP_SYS_SubstituteTracksValueTable();
        btn_clear = new javax.swing.JButton();
        btn_replace = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.flatcomponents.api.HelpTextArea();

        jScrollPane1.setViewportView(table);

        org.openide.awt.Mnemonics.setLocalizedText(btn_clear, org.openide.util.NbBundle.getMessage(RP_SYS_SubstituteTracksComp.class, "RP_SYS_SubstituteTracksComp.btn_clear.text")); // NOI18N
        btn_clear.setEnabled(false);
        btn_clear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_clearActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_replace, org.openide.util.NbBundle.getMessage(RP_SYS_SubstituteTracksComp.class, "RP_SYS_SubstituteTracksComp.btn_replace.text")); // NOI18N
        btn_replace.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_SubstituteTracksComp.class, "RP_SYS_SubstituteTracksComp.btn_replace.toolTipText")); // NOI18N
        btn_replace.setEnabled(false);
        btn_replace.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_replaceActionPerformed(evt);
            }
        });

        jScrollPane2.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(3);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(RP_SYS_SubstituteTracksComp.class, "RP_SYS_SubstituteTracksComp.helpTextArea1.text")); // NOI18N
        jScrollPane2.setViewportView(helpTextArea1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2)
                        .addGap(29, 29, 29))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_clear)
                    .addComponent(btn_replace))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_clear, btn_replace});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE)
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
    private org.jjazz.flatcomponents.api.HelpTextArea helpTextArea1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables


}
