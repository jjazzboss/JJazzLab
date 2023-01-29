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
package org.jjazz.pianoroll.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.Action;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.ToolbarPanel;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.ResUtil;
import org.openide.*;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


/**
 * The TopComponent for a PianoRollEditor.
 * <p>
 * The TopComponent closes itself when song is closed and listen to SongStructure changes to update the edited range or close the
 * component if SongStructure is not supported like introducing/removing time signatures.
 */
public final class PianoRollEditorTopComponent extends TopComponent implements PropertyChangeListener, SgsChangeListener
{

    // public static final String MODE = "midieditor";  // WindowManager mode
    public static final String MODE = "editor";  // WindowManager mode


    private final PianoRollEditor editor;
    private final ToolbarPanel toolbarPanel;
    private static final Logger LOGGER = Logger.getLogger(PianoRollEditorTopComponent.class.getSimpleName());
    private final Song song;
    private SongPart sptFirst;
    private int saveSptFirstIndex;
    private SongPart sptLast;
    private int saveSptLastIndex;
    private List<SongPart> saveEditedSpts;
    private boolean wholeSongEdit;
    private String titleBase;
    private static boolean notifiedOnce;


    /**
     * Create a TopComponent editor for the specified song.
     * <p>
     *
     * @param sg
     * @param settings
     */
    public PianoRollEditorTopComponent(Song sg, PianoRollEditorSettings settings)
    {
        Preconditions.checkNotNull(sg);
        Preconditions.checkNotNull(settings);

        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.FALSE);


        // Assign to mode
        // https://dzone.com/articles/secrets-netbeans-window-system
        // https://web.archive.org/web/20170314072532/https://blogs.oracle.com/geertjan/entry/creating_a_new_mode_in        
        Mode mode = WindowManager.getDefault().findMode(PianoRollEditorTopComponent.MODE);
        assert mode != null;
        assert mode.dockInto(this);


        this.song = sg;
        setDisplayName(getDefaultTabName(song));
        var spts = this.song.getSongStructure().getSongParts();
        this.sptFirst = spts.get(0);
        this.sptLast = spts.get(spts.size() - 1);
        editor = new PianoRollEditor(sptFirst.getStartBarIndex(), getBeatRange(), new Phrase(0), sptFirst.getRhythm().getTimeSignature(), null, settings);
        editor.setSong(song);
        editor.setUndoManager(JJazzUndoManagerFinder.getDefault().get(song));
        toolbarPanel = new ToolbarPanel(editor, song.getName());


        initComponents();


        // Automatically close when song is closed
        song.addPropertyChangeListener(this);


        // Update model when song structure changes
        song.getSongStructure().addSgsChangeListener(this);
    }

    /**
     * Update the model to edit the phrase on whole or part of the song.
     * <p>
     *
     * @param sptFirst Start of the edited phrase. Must belong to the editor's song.
     * @param sptLast  End of the edited phrase. Must belong to the editor's song. If sptLast has a different time signature than
     *                 sptFirst, sptLast will be replaced by the last song part after sptFirst which has the same time signature.
     * @param p
     * @param keyMap   Can be null
     */
    public void setModel(SongPart sptFirst, SongPart sptLast, Phrase p, DrumKit.KeyMap keyMap)
    {
        var ss = getSong().getSongStructure();
        Preconditions.checkNotNull(sptFirst);
        Preconditions.checkNotNull(sptLast);
        Preconditions.checkNotNull(p);
        Preconditions.checkArgument(sptFirst.getStartBarIndex() <= sptLast.getStartBarIndex(), "sptFirst=%s sptLast=%s", sptFirst, sptLast);
        Preconditions.checkArgument(sptFirst.getContainer() == ss && sptLast.getContainer() == ss, "sptFirst=%s sptLast=%s", sptFirst, sptLast);


        var spts = ss.getSongParts();
        if (spts.isEmpty())
        {
            return;
        }


        this.sptFirst = sptFirst;
        this.sptLast = sptLast;
        refreshSptLast();
        refreshSaveEditedSpts();
        wholeSongEdit = sptFirst.getStartBarIndex() == 0 && sptLast == spts.get(spts.size() - 1);

        editor.setModel(sptFirst.getStartBarIndex(), getBeatRange(), p, sptFirst.getRhythm().getTimeSignature(), keyMap);
        refreshToolbarTitle();
    }

    /**
     * The title used within the editor.
     *
     * @return
     */

    public String getTitle()
    {
        return titleBase;
    }

    /**
     * Set the title base used within the editor.
     *
     * @param title
     */
    public void setTitle(String title)
    {
        titleBase = title;
        refreshToolbarTitle();
    }

    /**
     * The song associated to this TopComponent.
     *
     * @return
     */
    public Song getSong()
    {
        return song;
    }

    public SongPart getFirstSpt()
    {
        return sptFirst;
    }

    public SongPart getLastSpt()
    {
        return sptLast;
    }

    /**
     * The beat range corresponding to start of first SongPart until end of last SongPart.
     *
     * @return
     */
    public FloatRange getBeatRange()
    {
        var brFirst = song.getSongStructure().getBeatRange(sptFirst.getBarRange());
        var brLast = song.getSongStructure().getBeatRange(sptLast.getBarRange());
        return brFirst.getUnion(brLast);
    }

    /**
     * The bar range corresponding to start of first SongPart until end of last SongPart.
     *
     * @return
     */
    public IntRange getBarRange()
    {
        return sptFirst.getBarRange().getUnion(sptLast.getBarRange());
    }

    @Override
    public String preferredID()
    {
        return "PianoRollEditorTopComponent";
    }

    public PianoRollEditor getEditor()
    {
        return editor;
    }

    /**
     * Open this TopComponent at the appropriate position compared to other song editor.
     */
    public void openNextToSongEditor()
    {
        int clTcPos = CL_EditorTopComponent.get(getSong().getChordLeadSheet()).getTabPosition();
        assert clTcPos != -1;
        openAtTabPosition(clTcPos + 1);
    }


    @Override
    public UndoRedo getUndoRedo()
    {
        return editor.getUndoManager();
    }

    /**
     * Overridden to insert possible new actions from path "Actions/PianoRollEditorTopComponent".
     *
     * @return The actions to be shown in the TopComponent popup menu.
     */
    @Override
    public Action[] getActions()
    {
        List<? extends Action> newActions = Utilities.actionsForPath("Actions/PianoRollEditorTopComponent");
        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(newActions);
        if (!newActions.isEmpty())
        {
            actions.add(null);   // Separator         
        }
        Collections.addAll(actions, super.getActions()); // Get the standard builtin actions Close, Close All, Close Other      
        return actions.toArray(new Action[0]);
    }

    @Override
    public Lookup getLookup()
    {
        return editor.getLookup();
    }

    @Override
    public int getPersistenceType()
    {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    public boolean canClose()
    {
        return true;
    }

    @Override
    public void componentOpened()
    {

    }

    @Override
    public void componentClosed()
    {
        song.removePropertyChangeListener(this);
        song.getSongStructure().removeSgsChangeListener(this);
        editor.cleanup();
    }

    /**
     * Return the active (i.e. focused or ancestor of the focused component) PianoRollEditorTopComponent.
     *
     * @return Can be null
     */
    static public PianoRollEditorTopComponent getActive()
    {
        TopComponent tc = TopComponent.getRegistry().getActivated();
        return (tc instanceof PianoRollEditorTopComponent) ? (PianoRollEditorTopComponent) tc : null;
    }

    /**
     * Search for the PianoRollEditorTopComponent associated to song.
     *
     * @param song
     * @return Can be null
     */
    static public PianoRollEditorTopComponent get(Song song)
    {
        Set<TopComponent> tcs = TopComponent.getRegistry().getOpened();
        return tcs.stream()
                .filter(tc -> tc instanceof PianoRollEditorTopComponent preTc && preTc.getSong() == song)
                .map(tc -> (PianoRollEditorTopComponent) tc)
                .findAny()
                .orElse(null);
    }


    /**
     * The default tab name for a song.
     *
     * @param song
     * @return
     */
    static public String getDefaultTabName(Song song)
    {
        return ResUtil.getString(PianoRollEditorTopComponent.class, "PianoRollEditorTabName", song.getName());
    }


    //=============================================================================
    // PropertyChangeListener interface
    //=============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == song)
        {
            if (evt.getPropertyName().equals(Song.PROP_CLOSED))
            {
                close();
            } else if (evt.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED_OR_RESET))
            {
                if (evt.getOldValue().equals(Boolean.TRUE))
                {
                    // File was saved
                    setDisplayName(getDefaultTabName(song));
                }
            }
        }
    }

    //=============================================================================
    // SgsChangeListener interface
    //=============================================================================
    @Override
    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void songStructureChanged(SgsChangeEvent e)
    {

        // Check if sptFirst/sptLast are impacted, if yes update the model
        // Use high-level actions to not be polluted by intermediate states
        if (e instanceof SgsActionEvent evt)
        {
            if (evt.isActionStarted())
            {
                return;
            }

            var allSpts = getSong().getSongStructure().getSongParts();
            if (allSpts.isEmpty())
            {
                close();
                return;
            }


            // Whole song edit mode: easy, just check that there is only one signature, otherwise close
            if (wholeSongEdit)
            {
                sptFirst = allSpts.get(0);
                sptLast = allSpts.get(allSpts.size() - 1);
                boolean multiTs = allSpts.stream()
                        .anyMatch(spt -> !spt.getRhythm().getTimeSignature().equals(sptFirst.getRhythm().getTimeSignature()));
                if (multiTs)
                {
                    // Unsupported
                    notifyUserAndCloseBecauseMultiSignature();
                    return;
                }

                setModel(sptFirst, sptLast, editor.getModel(), editor.getDrumKeyMap());
                return;
            }


            // We edit a phrase on part of the song (can be a part of a user phrase on a multi-signature song, or a phrase associated to a Song Part's Rhythm Parameter).
            switch (evt.getActionId())
            {
                case "addSongParts" ->
                {
                    setModel(sptFirst, sptLast, editor.getModel(), editor.getDrumKeyMap());
                }


                case "removeSongParts" ->
                {
                    // Get the remaining song parts from the original editing range
                    var remainingEditedSpts = saveEditedSpts.stream()
                            .filter(spt -> allSpts.contains(spt))
                            .toList();
                    if (remainingEditedSpts.isEmpty())
                    {
                        // Everything is removed, kill editor!
                        close();
                        return;
                    }

                    sptFirst = remainingEditedSpts.get(0);
                    sptLast = remainingEditedSpts.get(remainingEditedSpts.size() - 1);
                    setModel(sptFirst, sptLast, editor.getModel(), editor.getDrumKeyMap());
                }


                case "replaceSongParts" ->
                {
                    sptFirst = allSpts.get(saveSptFirstIndex);
                    sptLast = allSpts.get(saveSptLastIndex);
                    setModel(sptFirst, sptLast, editor.getModel(), editor.getDrumKeyMap());
                }


                case "resizeSongParts" ->
                {
                    setModel(sptFirst, sptLast, editor.getModel(), editor.getDrumKeyMap());
                }


                default ->
                {
                    // Nothing
                }
            }

        } else
        {
            // Nothing
        }
    }


    // ============================================================================================
    // Private methods
    // ============================================================================================
    /**
     * Update sptLast if it is not consistent with the SongStructure: all SongParts from sptFirst to sptLast must share the same
     * time signature.
     */
    private void refreshSptLast()
    {
        if (sptFirst == sptLast)
        {
            // Easy
            return;
        }

        var spts = song.getSongStructure().getSongParts();
        var index = spts.indexOf(sptFirst);
        assert index != -1;
        var newSptLast = sptFirst;
        for (int i = index + 1; i < spts.size(); i++)
        {
            var spt = spts.get(i);
            if (!sptFirst.getRhythm().getTimeSignature().equals(spt.getRhythm().getTimeSignature()))
            {
                newSptLast = spts.get(i - 1);
                break;
            }
            newSptLast = spt;
            if (spt == sptLast)
            {
                break;
            }
        }

        sptLast = newSptLast;
    }

    private void notifyUserAndCloseBecauseMultiSignature()
    {
        if (notifiedOnce)
        {
            String msg = ResUtil.getString(getClass(), "CloseEditorMultiSignature");
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            notifiedOnce = true;
        }
        close();
    }

    /**
     * Update sptFirst and sptLast in "whole song" edit mode: search for the largest range of same-time-signature-SongParts around
     * sptFirst.
     */
    private void refreshFirstLastWholeSongEdit()
    {
        List<SongPart> editedSpts = new ArrayList<>();
        var ts = sptFirst.getRhythm().getTimeSignature();
        var spts = song.getSongStructure().getSongParts();
        int index = spts.indexOf(sptFirst);
        assert index != -1;
        for (int i = index; i >= 0; i--)
        {
            var spt = spts.get(i);
            if (spt.getRhythm().getTimeSignature().equals(ts))
            {
                editedSpts.add(0, spt);
            } else
            {
                break;
            }
        }
        for (int i = index + 1; i < spts.size(); i++)
        {
            var spt = spts.get(i);
            if (spt.getRhythm().getTimeSignature().equals(ts))
            {
                editedSpts.add(spt);
            } else
            {
                break;
            }
        }
        sptFirst = editedSpts.get(0);
        sptLast = editedSpts.get(editedSpts.size() - 1);
    }

    private void refreshToolbarTitle()
    {
        var barRange = getBarRange();
        String strBarRange = (barRange.from + 1) + ".." + (barRange.to + 1);
        String strSongParts = sptFirst == sptLast ? sptFirst.getName() : sptFirst.getName() + ".." + sptLast.getName();
        String title = ResUtil.getString(getClass(), "PianoEditorUserPhraseTitle", titleBase, strSongParts, strBarRange, sptFirst.getRhythm().getTimeSignature());
        toolbarPanel.setTitle(title);
    }

    /**
     * Save the spts in the edited range, plus the indexes of sptFirst and sptLast.
     */
    private void refreshSaveEditedSpts()
    {
        var spts = getSong().getSongStructure().getSongParts();
        saveEditedSpts = spts.stream()
                .filter(spt -> spt.getStartBarIndex() >= sptFirst.getStartBarIndex() && spt.getStartBarIndex() <= sptLast.getStartBarIndex())
                .toList();

        saveSptFirstIndex = spts.indexOf(sptFirst);
        saveSptLastIndex = spts.indexOf(sptLast);
    }

    void writeProperties(java.util.Properties p)
    {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p)
    {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_toolbar = toolbarPanel;
        pnl_editor = editor;

        setToolTipText(org.openide.util.NbBundle.getMessage(PianoRollEditorTopComponent.class, "PianoRollEditorTopComponent.toolTipText")); // NOI18N
        setLayout(new java.awt.BorderLayout());
        add(pnl_toolbar, java.awt.BorderLayout.NORTH);
        add(pnl_editor, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel pnl_editor;
    private javax.swing.JPanel pnl_toolbar;
    // End of variables declaration//GEN-END:variables


}
