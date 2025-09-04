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
package org.jjazz.spteditor;

import org.jjazz.spteditor.api.RpEditor;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.song.api.Song;
import org.jjazz.spteditor.api.SptEditorSettings;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.spteditor.api.SptEditor;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.spteditor.spi.RpEditorComponentFactory;
import org.jjazz.spteditor.spi.DefaultRpEditorComponentFactory;
import org.jjazz.spteditor.spi.RpEditorComponent;
import org.jjazz.ss_editorimpl.api.EditRhythm;

public class SptEditorImpl extends SptEditor implements PropertyChangeListener
{

    private Lookup.Result<SongPartParameter> sptpLkpResult;
    private LookupListener sptpLkpListener;
    private Lookup.Result<SongPart> sptLkpResult;
    private LookupListener sptLkpListener;
    private final Lookup.Result<Song> songLkpResult;
    private LookupListener songLkpListener;
    private final Lookup lookup;
    private final InstanceContent instanceContent;
    /**
     * The songparts currently edited by this editor.
     */
    private final List<SongPart> songParts;
    private Rhythm previousRhythm;
    private Song songModel;
    private SS_Editor ssEditor;
    private final SptEditorSettings settings;
    private final DefaultRpEditorComponentFactory defaultRpEditorComponentFactory;
    private static final Logger LOGGER = Logger.getLogger(SptEditorImpl.class.getSimpleName());

    public SptEditorImpl(SptEditorSettings settings, DefaultRpEditorComponentFactory factory)
    {
        songParts = new ArrayList<>();

        // Listen to settings change
        this.settings = settings;
        this.settings.addPropertyChangeListener(this);

        this.defaultRpEditorComponentFactory = factory;

        // UI initialization
        initComponents();

        org.jjazz.uiutilities.api.UIUtilities.installSelectAllWhenFocused(tf_name);
        org.jjazz.uiutilities.api.UIUtilities.installPrintableAsciiKeyTrap(tf_name);

        setEditorEnabled(false);

        // Prepare the lookup listeners
        sptpLkpListener = lookupEvent -> sptpPresenceChanged();
        sptLkpListener = lookupEvent -> sptPresenceChanged();
        songLkpListener = lookupEvent -> songPresenceChanged();


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

    @Override
    public SptEditorSettings getSettings()
    {
        return settings;
    }

    @Override
    public DefaultRpEditorComponentFactory getDefaultRpEditorComponentFactory()
    {
        return defaultRpEditorComponentFactory;
    }

    @Override
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

    @Override
    public Lookup getLookup()
    {
        return this.lookup;
    }

    @Override
    public JJazzUndoManager getUndoManager()
    {
        return songParts.isEmpty() ? null : JJazzUndoManagerFinder.getDefault().get(songParts.get(0).getContainer());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
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
        lbl_SptSelection = new javax.swing.JLabel();

        panel_RhythmParameters.setLayout(new javax.swing.BoxLayout(panel_RhythmParameters, javax.swing.BoxLayout.Y_AXIS));

        tf_name.setText("Name"); // NOI18N
        tf_name.setToolTipText(org.openide.util.NbBundle.getMessage(SptEditorImpl.class, "SptEditorImpl.tf_name.toolTipText")); // NOI18N
        tf_name.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_nameActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Rhythm, "Rhythm"); // NOI18N
        btn_Rhythm.setToolTipText(org.openide.util.NbBundle.getMessage(SptEditorImpl.class, "SptEditorImpl.btn_Rhythm.toolTipText")); // NOI18N
        btn_Rhythm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_RhythmActionPerformed(evt);
            }
        });

        lbl_ParentSection.setFont(lbl_ParentSection.getFont().deriveFont(lbl_ParentSection.getFont().getSize()-2f));
        lbl_ParentSection.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_ParentSection, "A (4/4)"); // NOI18N
        lbl_ParentSection.setToolTipText(org.openide.util.NbBundle.getMessage(SptEditorImpl.class, "SptEditorImpl.lbl_ParentSection.toolTipText")); // NOI18N

        lbl_SptSelection.setFont(lbl_ParentSection.getFont());
        org.openide.awt.Mnemonics.setLocalizedText(lbl_SptSelection, "song part #1..."); // NOI18N
        lbl_SptSelection.setToolTipText(org.openide.util.NbBundle.getBundle(SptEditorImpl.class).getString("SptEditorImpl.lbl_SptSelection.toolTipText")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tf_name, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panel_RhythmParameters, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_Rhythm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(lbl_SptSelection)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbl_ParentSection)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tf_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_ParentSection, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_SptSelection))
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
            getUndoManager().startCEdit(ResUtil.getString(getClass(), "CTL_ChangeSptName"));
            songModel.getSongStructure().setSongPartsName(songParts, name);
            getUndoManager().endCEdit(ResUtil.getString(getClass(), "CTL_ChangeSptName"));
        }

    }//GEN-LAST:event_tf_nameActionPerformed

    private void btn_RhythmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_RhythmActionPerformed
    {//GEN-HEADEREND:event_btn_RhythmActionPerformed
        EditRhythm.changeSongPartsRhythm(songParts);
    }//GEN-LAST:event_btn_RhythmActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Rhythm;
    private javax.swing.JLabel lbl_ParentSection;
    private javax.swing.JLabel lbl_SptSelection;
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
        LOGGER.log(Level.FINE, "propertyChange() e={0}", e);
        if (e.getSource() == settings)
        {
            tf_name.setFont(settings.getNameFont());
        } else if (e.getSource() instanceof RpEditor)
        {
            // User has modified a value using our editor
            if (e.getPropertyName().equals(RpEditor.PROP_RP_VALUE))
            {
                RpEditor rpe = (RpEditor) e.getSource();
                RhythmParameter rp = rpe.getRpModel();
                Object newValue = e.getNewValue();
                getUndoManager().startCEdit(ResUtil.getString(getClass(), "CTL_SetRpValue"));
                for (SongPart spt : songParts.toArray(SongPart[]::new))
                {
                    Object value = spt.getRPValue(rp);
                    if (!value.equals(newValue))
                    {
                        songModel.getSongStructure().setRhythmParameterValue(spt, rp, newValue);
                    }
                }
                getUndoManager().endCEdit(ResUtil.getString(getClass(), "CTL_SetRpValue"));
            }
        } else if (e.getSource() instanceof SongPart)
        {
            // A value was modified in the model
            SongPart spt = (SongPart) e.getSource();
            if (!songParts.contains(spt))
            {
                throw new IllegalStateException("spt=" + spt + " songParts=" + songParts);
            }
            if (e.getPropertyName().equals(SongPart.PROP_NAME)
                    || e.getPropertyName().equals(SongPart.PROP_NB_BARS)
                    || e.getPropertyName().equals(SongPart.PROP_START_BAR_INDEX))
            {
                updateUIComponents();
            } else if (e.getPropertyName().equals(SongPart.PROP_RP_VALUE))
            {
                updateUIComponents();
            }
        } else if (e.getSource() == songModel)
        {
            if (e.getPropertyName().equals(Song.PROP_CLOSED))
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
     *
     * @param context The lookup of the SS_Editor.
     */
    private void refresh(Lookup context)
    {
        Collection<? extends SongPart> spts = context.lookupAll(SongPart.class
        );
        if (spts.isEmpty())
        {
            // Possible SongPartParameter selection
            Collection<? extends SongPartParameter> sptps = context.lookupAll(SongPartParameter.class
            );
            ArrayList<SongPart> spts2 = new ArrayList<>();
            // Get the list of SongParts corresponding to these RhythmParameters
            for (SongPartParameter sptp : sptps)
            {
                spts2.add(sptp.getSpt());
            }
            spts = spts2;
        } else
        {
            // SongPart selection. Nothing to do
        }

        LOGGER.log(Level.FINE, "refresh() spts={0}", spts);

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
        refresh(ssEditor.getLookup());
    }

    /**
     * Called when SongPartParameter presence changed in the lookup.
     */
    private void sptpPresenceChanged()
    {
        LOGGER.log(Level.FINE, "sptpPresenceChanged()");
        refresh(ssEditor.getLookup());
    }

    /**
     * Called when SongStructure presence changed in the lookup.
     * <p>
     * If a new song is detected, listen to the SS_Editor lookup selection changes.
     */
    private void songPresenceChanged()
    {
        LOGGER.log(Level.FINE, "songPresenceChanged()");
        Song song = Utilities.actionsGlobalContext().lookup(Song.class
        );
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
        // instanceContent.add(songModel.getChordLeadSheet());          // Commented out april 2021: seems useless

        var ssTc = SS_EditorTopComponent.get(songModel.getSongStructure()); // Might be null !? See Issue #395
        if (ssTc == null)
        {
            return;
        }
        ssEditor = ssTc.getEditor();
        assert ssEditor != null : "songModel=" + songModel;

        // Directly listen to the sgsModel editor selection changes
        Lookup context = ssEditor.getLookup();
        sptLkpResult = context.lookupResult(SongPart.class);
        sptLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, sptLkpListener, sptLkpResult));
        sptpLkpResult = context.lookupResult(SongPartParameter.class);
        sptpLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, sptpLkpListener, sptpLkpResult));
        refresh(context);
    }

    /**
     * Update the UI Components to match the 1st selected SongPart stored in songParts (must be non empty).
     * <p>
     * SongParts might use different rhythms. Alter the rendering on compatible RhythmParameters if there is a multi-songpart selection.
     */
    private void updateUIComponents()
    {
        if (songParts.isEmpty())
        {
            throw new IllegalStateException("isEnabled()=" + isEnabled() + " songParts=" + songParts);
        }

        // 
        // ==> SongParts can have different rhythms
        // 
        // Reference is SongPart(0), initialize UI with its values
        SongPart spt0 = songParts.get(0);
        Rhythm rhythm0 = spt0.getRhythm();
        btn_Rhythm.setText(rhythm0.getName().toLowerCase());
        btn_Rhythm.setToolTipText(rhythm0.getDescription());
        tf_name.setText(spt0.getName());


        // Update the labels
        lbl_ParentSection.setText(getParentSectionText(spt0));
        int spt0index = songModel.getSongStructure().getSongParts().indexOf(spt0) + 1;
        int sptLastIndex = songModel.getSongStructure().getSongParts().indexOf(songParts.get(songParts.size() - 1)) + 1;
        if (spt0index > sptLastIndex)
        {
            int tmp = spt0index;
            spt0index = sptLastIndex;
            sptLastIndex = tmp;
        }
        String sptText = ResUtil.getString(getClass(), "CTL_SongParts") + " #" + spt0index;
        if (sptLastIndex > spt0index)
        {
            sptText += "...#" + sptLastIndex;
        }
        lbl_SptSelection.setText(sptText);


        // Update the RpEditors if needed
        if (rhythm0 != previousRhythm)
        {
            for (RpEditor rpe : getRpEditors())
            {
                removeRpEditor(rpe);
            }

            // Add RpEditors
            addRpEditors(spt0, rhythm0.getRhythmParameters());

            previousRhythm = rhythm0;
        }


        // Update the RpEditors value with spt0 values
        for (RhythmParameter<?> rp : rhythm0.getRhythmParameters())
        {
            RpEditor rpe = getRpEditor(rp);
            rpe.updateEditorValue(spt0.getRPValue(rp));
        }


        //
        // Handle the multi-value cases 
        //
        // First get all reference values from 1st spt
        Rhythm rhythmValue = rhythm0;
        String nameValue = spt0.getName();
        String parentSectionNameValue = spt0.getParentSection().getData().getName();
        Object[] spt0Values = new Object[rhythm0.getRhythmParameters().size()];
        boolean[] changedRpValues = new boolean[spt0Values.length];
        int i = 0;
        for (RhythmParameter<?> rp : rhythm0.getRhythmParameters())
        {
            spt0Values[i] = spt0.getRPValue(rp);
            changedRpValues[i] = false;
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
                // Check RhythmParameters value only if we have the same shared rhythm
                int j = 0;
                for (RhythmParameter<?> rp : rhythm0.getRhythmParameters())
                {
                    if (!spt.getRPValue(rp).equals(spt0Values[j]))
                    {
                        // There is at least 1 different rp value
                        changedRpValues[j] = true;
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
                rpe.setMultiValueMode(changedRpValues[j] == true);
                j++;
            } else
            {
                rpe.setEnabled(false);
            }
        }
    }

    /**
     * Create the RpEditors and add them to the dedicated panel.
     *
     *
     * @param spt
     * @param rps
     */
    private void addRpEditors(SongPart spt, List<RhythmParameter<?>> rps)
    {
        int rpNameMaxPrefWidth = 0;
        var rpes = new ArrayList<RpEditor>();

        for (var rp : rps)
        {
            // Get the editor      
            RpEditorComponentFactory factory = RpEditorComponentFactory.findFactory(rp);
            if (factory == null)
            {
                factory = defaultRpEditorComponentFactory;
            }
            RpEditorComponent c = factory.createComponent(this.songModel, spt, rp);
            RpEditor rpe = new RpEditor(spt, rp, c);
            rpes.add(rpe);
            rpe.addPropertyChangeListener(RpEditor.PROP_RP_VALUE, this);     // To avoid getting all UI property change events
            rpe.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));


            // We use a boxlayout Y in panel_RhythmParameters. We must limit the maximum height so that
            // rp editors do not take all the vertical place.
            int pHeight = rpe.getPreferredSize().height;
            rpe.setMaximumSize(new Dimension(rpe.getMaximumSize().width, pHeight));
            panel_RhythmParameters.add(rpe);

            // Find the wider label
            rpNameMaxPrefWidth = Math.max(rpNameMaxPrefWidth, rpe.getRpNameLabel().getPreferredSize().width);
        }


        // Set the rpName column width
        for (RpEditor rpe : rpes)
        {
            rpe.setRpNameColumnWidth(rpNameMaxPrefWidth + 15);
        }


        panel_RhythmParameters.revalidate();
        panel_RhythmParameters.repaint();
    }

    private void removeRpEditor(RpEditor rpe)
    {
        rpe.removePropertyChangeListener(this);
        rpe.cleanup();
        panel_RhythmParameters.remove(rpe);
        panel_RhythmParameters.revalidate();
        panel_RhythmParameters.repaint();
    }

    private String getParentSectionText(SongPart spt)
    {
        Section section = spt.getParentSection().getData();
        TimeSignature ts = section.getTimeSignature();
        return ResUtil.getString(getClass(), "CTL_Parent") + ": " + section.getName() + " [" + ts + "]";
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
        org.jjazz.uiutilities.api.UIUtilities.setRecursiveEnabled(b, this);
    }

    private void resetModel()
    {
        instanceContent.remove(songModel);
        // instanceContent.remove(songModel.getChordLeadSheet());      
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
