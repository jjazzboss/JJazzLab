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
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.swing.Action;
import org.jjazz.harmony.api.TimeSignature;
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
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


/**
 * The TopComponent for a PianoRollEditor.
 * <p>
 * The TopComponent closes itself when song is closed and listen to SongStructure changes to update the edited range.
 */
public final class PianoRollEditorTopComponent extends TopComponent implements PropertyChangeListener, SgsChangeListener
{

    // public static final String MODE = "midieditor";  // WindowManager mode
    public static final String MODE = "editor";  // WindowManager mode


    private final PianoRollEditor editor;
    private final ToolbarPanel toolbarPanel;
    private static final Logger LOGGER = Logger.getLogger(PianoRollEditorTopComponent.class.getSimpleName());
    private final Song song;
    private SongPart songPart;
    private String titleBase;


    /**
     * Create a TopComponent editor for the specified song.
     * <p>
     *
     * @param sg       The TopComponent listens to song structure changes to update the edited range.
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
        this.songPart = spts.get(0);
        TreeMap<Float, TimeSignature> tMap = new TreeMap<>();
        tMap.put(song.getSongStructure().getBeatRange(songPart.getBarRange()).from, songPart.getRhythm().getTimeSignature());


        editor = new PianoRollEditor(songPart.getStartBarIndex(), getBeatRange(), new Phrase(0), tMap, null, settings);
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
     * Update the model to edit a phrase on a single SongPart.
     * <p>
     *
     * @param spt    Must belong to the song
     * @param p
     * @param keyMap Can be null
     */
    public void setModel(SongPart spt, Phrase p, DrumKit.KeyMap keyMap)
    {
        Preconditions.checkNotNull(p);
        Preconditions.checkNotNull(spt);
        Preconditions.checkArgument(song.getSongStructure().getSongParts().contains(spt));


        songPart = spt;
        TreeMap<Float, TimeSignature> mapPosTs = new TreeMap<>();
        mapPosTs.put(0f, songPart.getRhythm().getTimeSignature());

        editor.setModel(songPart.getStartBarIndex(), getBeatRange(), p, mapPosTs, keyMap);

        refreshToolbarTitle();
    }

    /**
     * Update the model to edit a phrase on the whole song.
     * <p>
     *
     * @param p
     * @param keyMap Can be null
     */
    public void setModel(Phrase p, DrumKit.KeyMap keyMap)
    {
        Preconditions.checkNotNull(p);

        var ss = song.getSongStructure();
        var spts = ss.getSongParts();
        if (spts.isEmpty())
        {
            return;
        }

        songPart = null;
        TreeMap<Float, TimeSignature> mapPosTs = new TreeMap<>();
        spts.forEach(spt -> mapPosTs.put(ss.getPositionInNaturalBeats(spt.getStartBarIndex()), spt.getRhythm().getTimeSignature()));


        editor.setModel(0, getBeatRange(), p, mapPosTs, keyMap);
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

    /**
     * The edited SongPart, or null if the whole song is edited.
     *
     * @return
     */
    public SongPart getSongPart()
    {
        return songPart;
    }

    /**
     * The edited beat range.
     *
     * @return
     */
    public FloatRange getBeatRange()
    {
        var ss = song.getSongStructure();
        return songPart != null ? ss.getBeatRange(songPart.getBarRange()) : ss.getBeatRange(null);
    }

    /**
     * The edited bar range.
     *
     * @return
     */
    public IntRange getBarRange()
    {
        var ss = song.getSongStructure();
        return songPart != null ? songPart.getBarRange() : ss.getBarRange();
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
        toolbarPanel.cleanup();
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
        // Use high-level actions to not be polluted by intermediate states
        if (e instanceof SgsActionEvent evt && evt.isActionComplete())
        {
            var allSpts = getSong().getSongStructure().getSongParts();
            if (allSpts.isEmpty())
            {
                close();
                return;
            }


            // Refresh the editor
            if (songPart == null)
            {
                setModel(editor.getModel(), editor.getDrumKeyMap());
            } else
            {
                setModel(songPart, editor.getModel(), editor.getDrumKeyMap());
            }
        }
    }


    // ============================================================================================
    // Private methods
    // ============================================================================================
    private void refreshToolbarTitle()
    {
        var barRange = getBarRange();
        String strSongPart = songPart != null ? " - " + songPart.getName() : "";
        String strBarRange = " - bars " + (barRange.from + 1) + ".." + (barRange.to + 1);
        String strTs = songPart != null ? " - " + songPart.getRhythm().getTimeSignature() : "";
        String title = titleBase + strSongPart + strBarRange + strTs;
        toolbarPanel.setTitle(title);
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
