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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import static org.jjazz.rhythm.api.RhythmVoice.Type.CHORD1;
import static org.jjazz.rhythm.api.RhythmVoice.Type.CHORD2;
import static org.jjazz.rhythm.api.RhythmVoice.Type.PHRASE1;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorComponent;
import static org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorComponent.PROP_EDITED_RP_VALUE;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_OverrideTracks;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_OverrideTracksValue;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythmselectiondialog.spi.RhythmPreviewer;
import org.jjazz.rhythmselectiondialog.spi.RhythmSelectionDialogProvider;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.windows.WindowManager;

/**
 * A RP editor component for RP_SYS_OverrideTracks.
 */
public class RP_SYS_OverrideTracksComp extends RealTimeRpEditorComponent<RP_SYS_OverrideTracksValue> implements ListSelectionListener
{

    private final RP_SYS_OverrideTracks rp;
    private RP_SYS_OverrideTracksValue lastValue;
    private RP_SYS_OverrideTracksValue uiValue;
    private final RP_SYS_OverrideTracksValueTable tbl_mappings;
    private SongPartContext sptContext;
    private String variationValue;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_OverrideTracksComp.class.getSimpleName());

    public RP_SYS_OverrideTracksComp(RP_SYS_OverrideTracks rp)
    {
        checkNotNull(rp);
        this.rp = rp;

        initComponents();

        tbl_mappings = ((RP_SYS_OverrideTracksValueTable) table);


        // Add listeners
        tbl_mappings.addPropertyChangeListener(RP_SYS_OverrideTracksValueTable.PROP_RV_DEST, e -> 
        {
            // User changed a dest rhythm voice
            RhythmVoice rvSrc = (RhythmVoice) e.getOldValue();
            RhythmVoice rvDest = (RhythmVoice) e.getNewValue();
            if (rvDest == null)
            {
                // Special case
                rvDest = rvSrc;
            }
            var override = uiValue.getOverride(rvSrc);
            setEditedRpValue(uiValue.set(rvSrc, override.set(rvDest)));
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
    public RP_SYS_OverrideTracks getRhythmParameter()
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
    public void preset(RP_SYS_OverrideTracksValue rpValue, SongPartContext sptContext)
    {
        Objects.requireNonNull(rpValue);
        Objects.requireNonNull(sptContext);
        SongPart spt = sptContext.getSongPart();
        Preconditions.checkArgument(rpValue.getBaseRhythm() == spt.getRhythm(), "rpValue.getBaseRhythm()=%s spt=%s", rpValue.getBaseRhythm(), spt);
        this.sptContext = sptContext;


        LOGGER.log(Level.FINE, "preset() -- rpValue={0} sptContext={1}", new Object[]
        {
            rpValue, sptContext
        });

        var rpVariation = RP_SYS_Variation.getVariationRp(sptContext.getSongPart().getRhythm());
        variationValue = rpVariation == null ? "" : spt.getRPValue(rpVariation);

        uiValue = null;
        setEditedRpValue(rpValue);
    }

    @Override
    public void setEditedRpValue(RP_SYS_OverrideTracksValue rpValue)
    {
        lastValue = uiValue;
        uiValue = rpValue;
        refreshUI();
        fireUiValueChanged();
    }

    @Override
    public RP_SYS_OverrideTracksValue getEditedRpValue()
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
        btn_clear.setEnabled(rvSrc != null && uiValue.getOverride(rvSrc) != null);

    }

    // ===============================================================================
    // Private methods
    // ===============================================================================    
    /**
     * Refresh UI to match uiValue.
     */
    private void refreshUI()
    {
        var tblModel = tbl_mappings.getModel();
        tblModel.setSrcVariationValue(variationValue);
        tblModel.setRpValue(uiValue);
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


        // Select default rhythm to show in the rhythm selection dialog
        var oldOverride = uiValue.getOverride(rvSrc);
        var rId = oldOverride == null ? rvSrc.getContainer().getUniqueId() : oldOverride.rvDest().getContainer().getUniqueId();
        var rdb = RhythmDatabase.getDefault();
        var ri = rdb.getRhythm(rId);


        // Set up previewer
        RhythmPreviewer previewer = RhythmPreviewer.getDefault();
        if (previewer != null)
        {
            try
            {
                previewer.setContext(sptContext.getSong(), sptContext.getSongPart());
            } catch (MidiUnavailableException ex)
            {
                LOGGER.log(Level.WARNING, "setMapping() Can''t set context ex={0}. RhythmPreviewProvider disabled.", ex.getMessage());
                previewer = null;
            }
        }

        // Set up and show dialog
        var dlg = RhythmSelectionDialogProvider.getDefault().getDialog();
        dlg.preset(ri, previewer, () -> false);
        dlg.setCustomComponent(new JLabel(" "));
        dlg.setTitleText(ResUtil.getString(getClass(), "SelectOverrideRhythm", ri.timeSignature()));
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);


        // Dialog might have changed the active song if the RhythmPreviewer was used, need to restore the active song
        ActiveSongManager.getDefault().setActive(sptContext.getSong(), sptContext.getMidiMix());


        // Dialog exited
        if (!dlg.isExitOk())
        {
            dlg.cleanup();
            return;
        }

        // Process user selection
        RhythmInfo riDest = dlg.getSelectedRhythm();
        RhythmVoice rvDest;
        String variationDest = dlg.getLastSelectedVariation();


        if (oldOverride != null && riDest.rhythmUniqueId().equals(oldOverride.rvDest().getContainer().getUniqueId()))
        {
            // Same destination rhythm: don't change rvDest but variation might has been updated
            rvDest = oldOverride.rvDest();
        } else
        {
            // Destination rhythm has changed
            Rhythm rDest = null;
            try
            {
                rDest = rdb.getRhythmInstance(riDest);   // throws UnavailableRhythmException
            } catch (UnavailableRhythmException ex)
            {
                LOGGER.log(Level.WARNING, "setMapping() Unexpected error : can not retrieve rhytm instance for riDestNew={0}", riDest);
                dlg.cleanup();
                return;
            }

            rvDest = findRvDest(rvSrc, rDest);
        }


        RP_SYS_OverrideTracksValue.Override override = new RP_SYS_OverrideTracksValue.Override(rvDest, variationDest);

        // update model
        setEditedRpValue(uiValue.set(rvSrc, override));


        dlg.cleanup();
    }

    /**
     * Find the most appropriate destination RhythmVoice for rvSrc in rDest.
     *
     * @param rvSrc
     * @param rDest
     *
     * @return
     */
    private RhythmVoice findRvDest(RhythmVoice rvSrc, Rhythm rDest)
    {
        RhythmVoice res = rDest.getRhythmVoices().stream()
                .filter(rvi -> rvi.getType() == rvSrc.getType())
                .findFirst()
                .orElse(null);
        if (res == null)
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
            res = rDest.getRhythmVoices().stream()
                    .filter(rvi -> rvi.getType() == testType)
                    .findFirst()
                    .orElse(null);
            if (res == null)
            {
                res = rDest.getRhythmVoices().get(0);
            }
        }
        return res;
    }

    private void clearMapping(ActionEvent evt)
    {
        var rvSrc = tbl_mappings.getSelectedBaseRhythmVoice();
        if (uiValue == null || rvSrc == null)
        {
            return;
        }
        var oldRvDest = uiValue.getOverride(rvSrc);
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
        table = new org.jjazz.rpcustomeditorfactoryimpl.RP_SYS_OverrideTracksValueTable();
        btn_clear = new javax.swing.JButton();
        btn_replace = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.flatcomponents.api.HelpTextArea();

        jScrollPane1.setViewportView(table);

        org.openide.awt.Mnemonics.setLocalizedText(btn_clear, org.openide.util.NbBundle.getMessage(RP_SYS_OverrideTracksComp.class, "RP_SYS_OverrideTracksComp.btn_clear.text")); // NOI18N
        btn_clear.setEnabled(false);
        btn_clear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_clearActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_replace, org.openide.util.NbBundle.getMessage(RP_SYS_OverrideTracksComp.class, "RP_SYS_OverrideTracksComp.btn_replace.text")); // NOI18N
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
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(RP_SYS_OverrideTracksComp.class, "RP_SYS_OverrideTracksComp.helpTextArea1.text")); // NOI18N
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
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 583, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_clear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_replace, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
