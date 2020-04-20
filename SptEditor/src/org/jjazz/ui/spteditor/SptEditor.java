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
package org.jjazz.ui.spteditor;

import org.jjazz.ui.spteditor.api.RpEditor;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.song.api.Song;
import org.jjazz.ui.ss_editor.actions.EditRhythm;
import org.jjazz.ui.spteditor.api.SptEditorSettings;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import static org.jjazz.ui.spteditor.Bundle.*;
import org.jjazz.ui.spteditor.spi.RpEditorFactory;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.spteditor.api.SptEditorTopComponent;

/**
 * Edit one or more selected SongParts.
 * <p>
 * Lookup contains:<br>
 * - edited SongStructure<br>
 * - edited Song (container of the SongStructure if there is one)<br>
 */
@Messages(
        {
            "CTL_LinkedTo=",
            "CTL_Start=start:",
            "CTL_Size=size:",
            "CTL_ChangeSptName=Change Song Part Name",
            "CTL_ChangeSptRhythm=Change Rhythm",
            "CTL_SetRpValue=Set Rhythm Parameter",
            "ERR_ChangeRhythm=Impossible to use rhythm"
        })
public class SptEditor extends JPanel implements PropertyChangeListener
{

    private Lookup.Result<SongPartParameter> sptpLkpResult;
    private LookupListener sptpLkpListener;
    private Lookup.Result<SongPart> sptLkpResult;
    private LookupListener sptLkpListener;
    private Lookup.Result<Song> songLkpResult;
    private LookupListener songLkpListener;
    private Lookup lookup;
    private InstanceContent instanceContent;
    /**
     * The songparts currently edited by this editor.
     */
    private List<SongPart> songParts;
    private Rhythm previousRhythm;
    private Song songModel;
    private SS_Editor rlEditor;
    private SptEditorSettings settings;

    private static final Logger LOGGER = Logger.getLogger(SptEditor.class.getSimpleName());

    public SptEditor()
    {
        songParts = new ArrayList<>();

        // Listen to settings change
        settings = SptEditorSettings.getDefault();
        settings.addPropertyChangeListener(this);

        // UI initialization
        initComponents();

        // HACK ! 
        // Key presses are not consumed by JTextField, they are also processed by the keybinding framework
        // Discard this mechanism for some keys, eg SPACE should not trigger the play/pause action while
        // editing the JTextField field.
        tf_name.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("SPACE"), "doNothing");
        tf_name.getActionMap().put("doNothing", new NoAction());

        setEditorEnabled(false);

        sptpLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                sptpPresenceChanged();
            }
        };
        sptLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                sptPresenceChanged();
            }
        };
        songLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                songPresenceChanged();
            }
        };

        // Our general lookup : store our action map, the edited song and songStructure and the edited songparts.
        instanceContent = new InstanceContent();
        instanceContent.add(getActionMap());
        lookup = new AbstractLookup(instanceContent);

        // Listen to Song presence in the global context    
        Lookup context = Utilities.actionsGlobalContext();
        songLkpResult = context.lookupResult(Song.class);
        songLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, songLkpListener, songLkpResult));
        songPresenceChanged();
    }

    public void cleanup()
    {
        settings.removePropertyChangeListener(this);
        if (songModel != null)
        {
            resetModel();
        }
        songLkpListener = null;
        sptLkpListener = null;
        sptpLkpListener = null;
    }

    public Lookup getLookup()
    {
        return this.lookup;
    }

    public JJazzUndoManager getUndoManager()
    {
        return songParts.isEmpty() ? null : JJazzUndoManagerFinder.getDefault().get(songParts.get(0).getContainer());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        panel_RhythmParameters = new javax.swing.JPanel();
        tf_name = new javax.swing.JTextField();
        tf_name.setFont(settings.getNameFont());
        btn_Rhythm = new javax.swing.JButton();
        lbl_ParentSection = new javax.swing.JLabel();

        panel_RhythmParameters.setLayout(new javax.swing.BoxLayout(panel_RhythmParameters, javax.swing.BoxLayout.Y_AXIS));

        tf_name.setText(org.openide.util.NbBundle.getMessage(SptEditor.class, "SptEditor.tf_name.text")); // NOI18N
        tf_name.setToolTipText(org.openide.util.NbBundle.getMessage(SptEditor.class, "SptEditor.tf_name.toolTipText")); // NOI18N
        tf_name.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_nameActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Rhythm, org.openide.util.NbBundle.getMessage(SptEditor.class, "SptEditor.btn_Rhythm.text")); // NOI18N
        btn_Rhythm.setToolTipText(org.openide.util.NbBundle.getMessage(SptEditor.class, "SptEditor.btn_Rhythm.toolTipText")); // NOI18N
        btn_Rhythm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_RhythmActionPerformed(evt);
            }
        });

        lbl_ParentSection.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        lbl_ParentSection.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_ParentSection, org.openide.util.NbBundle.getMessage(SptEditor.class, "SptEditor.lbl_ParentSection.text")); // NOI18N
        lbl_ParentSection.setToolTipText(org.openide.util.NbBundle.getMessage(SptEditor.class, "SptEditor.lbl_ParentSection.toolTipText")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_ParentSection, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tf_name, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panel_RhythmParameters, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_Rhythm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tf_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_ParentSection, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btn_Rhythm, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(panel_RhythmParameters, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tf_nameActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_nameActionPerformed
    {//GEN-HEADEREND:event_tf_nameActionPerformed

        String name = tf_name.getText().trim();
        if (!name.isEmpty())
        {
            getUndoManager().startCEdit(CTL_ChangeSptName());
            songModel.getSongStructure().setSongPartsName(songParts, name);
            getUndoManager().endCEdit(CTL_ChangeSptName());
        }

    }//GEN-LAST:event_tf_nameActionPerformed

    private void btn_RhythmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_RhythmActionPerformed
    {//GEN-HEADEREND:event_btn_RhythmActionPerformed

        EditRhythm.changeRhythm(songParts);
    }//GEN-LAST:event_btn_RhythmActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Rhythm;
    private javax.swing.JLabel lbl_ParentSection;
    private javax.swing.JPanel panel_RhythmParameters;
    private javax.swing.JTextField tf_name;
    // End of variables declaration//GEN-END:variables

    @Override
    public String toString()
    {
        return "SptEditor";
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        LOGGER.fine("propertyChange() e=" + e);
        if (e.getSource() == settings)
        {
            tf_name.setFont(settings.getNameFont());
        } else if (e.getSource() instanceof RpEditor)
        {
            // User has modified a value using our editor
            if (e.getPropertyName() == RpEditor.PROP_RPVALUE)
            {
                RpEditor rpe = (RpEditor) e.getSource();
                RhythmParameter rp = rpe.getRpModel();
                Object newValue = e.getNewValue();
                for (SongPart spt : songParts.toArray(new SongPart[0]))
                {
                    Object value = spt.getRPValue(rp);
                    if (!value.equals(newValue))
                    {
                        getUndoManager().startCEdit(CTL_SetRpValue());
                        songModel.getSongStructure().setRhythmParameterValue(spt, rp, newValue);
                        getUndoManager().endCEdit(CTL_SetRpValue());
                    }
                }
            }
        } else if (e.getSource() instanceof SongPart)
        {
            // A value was modified in the model
            SongPart spt = (SongPart) e.getSource();
            if (!songParts.contains(spt))
            {
                throw new IllegalStateException("spt=" + spt + " songParts=" + songParts);
            }
            if (e.getPropertyName() == SongPart.PROPERTY_NAME
                    || e.getPropertyName() == SongPart.PROPERTY_NB_BARS
                    || e.getPropertyName() == SongPart.PROPERTY_START_BAR_INDEX)
            {
                updateUIComponents();
            } else if (e.getPropertyName() == SongPart.PROPERTY_RP_VALUE)
            {
                if (spt == songParts.get(0))
                {
                    // If change if for the 1st spt, update the corresponding RpEditor
                    RhythmParameter rp = (RhythmParameter) e.getOldValue();
                    RpEditor rpe = getRpEditor(rp);
                    rpe.setRpValue(e.getNewValue(), false);
                }
                updateUIComponents();
            }
        } else if (e.getSource() == songModel)
        {
            if (e.getPropertyName() == Song.PROP_CLOSED)
            {
                setEditorEnabled(false);
                if (songModel != null)
                {
                    resetModel();
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Private functions
    // ------------------------------------------------------------------------------------
    /**
     * Refresh the editor based on the selection (SongPart and SongPartParameters) found in the provided context.
     * <p>
     * Register the selected SongParts and call updateUIComponents().
     */
    private void refresh(Lookup context)
    {
        Collection<? extends SongPart> spts = context.lookupAll(SongPart.class);
        if (spts.isEmpty())
        {
            // Possible SongPartParameter selection
            Collection<? extends SongPartParameter> sptps = context.lookupAll(SongPartParameter.class);
            ArrayList<SongPart> spts2 = new ArrayList<>();
            // Get the list of SongParts corresponding to these RhythmParameters
            for (Iterator<? extends SongPartParameter> it = sptps.iterator(); it.hasNext();)
            {
                SongPartParameter sptp = it.next();
                spts2.add(sptp.getSpt());
            }
            spts = spts2;
        } else
        {
            // SongPart selection. Nothing to do
        }

        LOGGER.log(Level.FINE, "refresh() spts=" + spts);

        // Unregister previous songParts
        for (SongPart spt : songParts)
        {
            spt.removePropertyChangeListener(this);
        }
        songParts.clear();

        // Update editor enabled status
        if (spts.isEmpty())
        {
            // No good, just disable the editor
            setEditorEnabled(false);
            updateTabName(songParts);
        } else
        {
            // Ok, register the songparts and update the editor
            for (SongPart spt : spts)
            {
                songParts.add(spt);
                spt.addPropertyChangeListener(this);
            }
            setEditorEnabled(true);
            updateUIComponents();
        }
    }

    private RpEditor getRpEditor(RhythmParameter<?> rp)
    {
        for (Component c : panel_RhythmParameters.getComponents())
        {
            RpEditor e = (RpEditor) c;
            if (e.getRpModel() == rp)
            {
                return e;
            }
        }
        return null;
    }

    /**
     * Called when SongPart presence changed in the lookup.
     */
    private void sptPresenceChanged()
    {
        LOGGER.log(Level.FINE, "sptPresenceChanged()");
        refresh(rlEditor.getLookup());
    }

    /**
     * Called when SongPartParameter presence changed in the lookup.
     */
    private void sptpPresenceChanged()
    {
        LOGGER.log(Level.FINE, "sptpPresenceChanged()");
        refresh(rlEditor.getLookup());
    }

    /**
     * Called when SongStructure presence changed in the lookup.
     * <p>
     * If a new song is detected, listen to the SS_Editor lookup selection changes.
     */
    private void songPresenceChanged()
    {
        LOGGER.log(Level.FINE, "songPresenceChanged()");
        Song song = Utilities.actionsGlobalContext().lookup(Song.class);
        if (song == songModel || song == null)
        {
            // Do nothing
            return;
        }

        if (songModel != null)
        {
            resetModel();
        }

        songModel = song;
        songModel.addPropertyChangeListener(this); // Listen to closed events
        instanceContent.add(songModel);
        instanceContent.add(songModel.getChordLeadSheet());

        rlEditor = SS_EditorTopComponent.get(songModel.getSongStructure()).getSS_Editor();
        assert rlEditor != null : "songModel=" + songModel;

        // Directly listen to the sgsModel editor selection changes
        Lookup context = rlEditor.getLookup();
        sptLkpResult = context.lookupResult(SongPart.class);
        sptLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, sptLkpListener, sptLkpResult));
        sptpLkpResult = context.lookupResult(SongPartParameter.class);
        sptpLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, sptpLkpListener, sptpLkpResult));
        refresh(context);
    }

    /**
     * Update the UI Components to match the 1st selected SongPart stored in songParts (must be non empty).
     * <p>
     * Alter the rendering if there is a multi-songpart selection.
     */
    private void updateUIComponents()
    {
        if (songParts.isEmpty())
        {
            throw new IllegalStateException("isEnabled()=" + isEnabled() + " songParts=" + songParts);
        }

        // SongParts can have different rhythms
        // Reference is SongPart(0), initialize UI with its values
        updateTabName(songParts);
        SongPart spt0 = songParts.get(0);
        Rhythm rhythm0 = spt0.getRhythm();
        btn_Rhythm.setText(rhythm0.getName().toLowerCase());
        btn_Rhythm.setToolTipText(rhythm0.getDescription());
        tf_name.setText(spt0.getName());
        lbl_ParentSection.setText(getParentSectionText(spt0));
        if (rhythm0 != previousRhythm)
        {
            // Need to update the RpEditors
            for (RpEditor rpe : getRpEditors())
            {
                removeRpEditor(rpe);
            }
            for (RhythmParameter<?> rp : rhythm0.getRhythmParameters())
            {
                addRpEditor(spt0, rp);
            }
            previousRhythm = rhythm0;
        }

        // Update the RpEditors value
        for (RhythmParameter<?> rp : rhythm0.getRhythmParameters())
        {
            RpEditor rpe = this.getRpEditor(rp);
            rpe.setRpValue(spt0.getRPValue(rp), false);
        }

        //
        // Handle the multi-value cases 
        //
        // First get all reference values from 1st spt
        Rhythm rhythmValue = rhythm0;
        String nameValue = spt0.getName();
        String parentSectionNameValue = spt0.getParentSection().getData().getName();
        Object[] spt0Values = new Object[rhythm0.getRhythmParameters().size()];
        boolean[] changedRps = new boolean[spt0Values.length];
        int i = 0;
        for (RhythmParameter<?> rp : rhythm0.getRhythmParameters())
        {
            spt0Values[i] = spt0.getRPValue(rp);
            changedRps[i] = false;
            i++;
        }

        // Identify fields that change across song parts
        // If a value remains unchanged, it means all spts share the same value
        // If a value is set to null, it means at least 1 spt has a different value      
        for (i = 1; i < songParts.size(); i++)
        {
            SongPart spt = songParts.get(i);
            if (rhythmValue != null && spt.getRhythm() != rhythmValue)
            {
                // At least one rhythm differ
                rhythmValue = null;
            }
            if (nameValue != null && !spt.getName().equalsIgnoreCase(nameValue))
            {
                // There is at least 1 different name
                nameValue = null;
            }
            if (parentSectionNameValue != null && !spt.getParentSection().getData().getName().equalsIgnoreCase(parentSectionNameValue))
            {
                // There is at least 1 different parent section name
                parentSectionNameValue = null;
            }
            if (rhythmValue != null)
            {
                // Check RhythmParameters only if we have the same shared rhythm
                int j = 0;
                for (RhythmParameter<?> rp : rhythm0.getRhythmParameters())
                {
                    if (!spt.getRPValue(rp).equals(spt0Values[j]))
                    {
                        // There is at least 1 different rp value
                        changedRps[j] = true;
                    }
                    j++;
                }
            }
        }

        // Set the multiValue modes on UI components
        RpEditor.showMultiModeUsingFont(rhythmValue == null, btn_Rhythm);
        RpEditor.showMultiModeUsingFont(nameValue == null, tf_name);
        RpEditor.showMultiModeUsingFont(parentSectionNameValue == null, lbl_ParentSection);

        // Set the multivalue modes on RpEditors 
        int j = 0;
        for (RhythmParameter<?> rp : rhythm0.getRhythmParameters())
        {
            RpEditor rpe = getRpEditor(rp);
            if (rhythmValue != null)
            {
                // RP editor is enabled only when all song parts share the same rhythm
                rpe.setEnabled(true);
                rpe.setMultiValueMode(changedRps[j] == true);
                j++;
            } else
            {
                rpe.setEnabled(false);
            }
        }
    }

    private RpEditor addRpEditor(SongPart spt, RhythmParameter<?> rp)
    {
        // Add an editor for each rp        
        RpEditor rpe = RpEditorFactory.getCustomOrGenericRpEditor(songModel, spt, rp);
        rpe.addPropertyChangeListener(RpEditor.PROP_RPVALUE ,this);     // To avoid getting all UI property change events
        rpe.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        // We use a boxlayout Y in panel_RhythmParameters. We must limit the maximum height so that
        // rp editors do not take all the vertical place.
        int pHeight = rpe.getPreferredSize().height;
        rpe.setMaximumSize(new Dimension(rpe.getMaximumSize().width, pHeight));
        panel_RhythmParameters.add(rpe);
        panel_RhythmParameters.repaint();
        return rpe;
    }

    private void removeRpEditor(RpEditor rpe)
    {
        rpe.removePropertyChangeListener(this);
        rpe.cleanup();
        panel_RhythmParameters.remove(rpe);
        panel_RhythmParameters.repaint();
    }

    private String getParentSectionText(SongPart spt)
    {
        assert spt != null;
        Section section = spt.getParentSection().getData();
        return CTL_LinkedTo() + section.getName() + " [" + section.getTimeSignature() + "] "
                + CTL_Start() + spt.getStartBarIndex() + " " + CTL_Size() + spt.getNbBars();
    }

    /**
     * Update the TopComponent tab name depending on the specified song parts.
     * <p>
     * @param spts The song parts
     */
    private void updateTabName(List<SongPart> spts)
    {
        SptEditorTopComponent tc = SptEditorTopComponent.getInstance();
        if (tc != null)
        {
            String tabName = "Song Part";
            if (!spts.isEmpty())
            {
                SongPart spt0 = spts.get(0);
                int spt0Index = songModel.getSongStructure().getSongParts().indexOf(spts.get(0));
                if (spts.size() > 1)
                {
                    String spt0Name = org.jjazz.util.Utilities.truncate(spt0.getName(), 4) + "(" + (spt0Index + 1) + ")";
                    SongPart lastSpt = spts.get(spts.size() - 1);
                    int lastSptIndex = songModel.getSongStructure().getSongParts().indexOf(lastSpt);
                    String lastSptName = org.jjazz.util.Utilities.truncate(lastSpt.getName(), 4) + "(" + (lastSptIndex + 1) + ")";
                    tabName += "s: " + spt0Name + "..." + lastSptName;
                } else
                {
                    tabName += ": " + org.jjazz.util.Utilities.truncateWithDots(spt0.getName(), 10) + "(" + (spt0Index + 1) + ")";
                }
            }
            tc.setDisplayName(tabName);
        }
    }

    private List<RpEditor> getRpEditors()
    {
        ArrayList<RpEditor> rpes = new ArrayList<>();
        for (Component c : panel_RhythmParameters.getComponents())
        {
            if (c instanceof RpEditor)
            {
                rpes.add((RpEditor) c);
            }
        }
        return rpes;
    }

    private void setEditorEnabled(boolean b)
    {
        org.jjazz.ui.utilities.Utilities.setRecursiveEnabled(b, this);
    }

    private void resetModel()
    {
        instanceContent.remove(songModel);
        instanceContent.remove(songModel.getChordLeadSheet());
        songModel.removePropertyChangeListener(this);
        songModel = null;
    }

    private class NoAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            //do nothing
        }
    }
}
