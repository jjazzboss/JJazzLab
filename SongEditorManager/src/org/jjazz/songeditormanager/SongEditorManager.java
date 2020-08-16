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
package org.jjazz.songeditormanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.SwingUtilities;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.MidiMixManager;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.util.Exceptions;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * The central place where all song editors are created (from scratch, loaded from file, etc.) and managed.
 * <p>
 * You can register to get some change events (opened, saved, closed) from any song managed by this object.
 */
public class SongEditorManager implements PropertyChangeListener
{

    /**
     * This property change event is fired when a song is opened.
     * <p>
     * NewValue is the song object.
     */
    public static final String PROP_SONG_OPENED = "SongOpened";
    /**
     * This property change event is fired when a song is closed.
     * <p>
     * NewValue is the song object. Note that a Song object also fires a Closed event when it is closed by the SongEditorManager.
     */
    public static final String PROP_SONG_CLOSED = "SongClosed";
    /**
     * This property change event is fired when a song is saved.
     * <p>
     * NewValue is the song object. This is just a forward of a Song Saved event.
     */
    public static final String PROP_SONG_SAVED = "SongSaved";

    private static SongEditorManager INSTANCE;
    private final HashMap<Song, Editors> mapSongEditors;       // Don't use WeakHashMap here
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongEditorManager.class.getSimpleName());

    static public SongEditorManager getInstance()
    {
        synchronized (SongEditorManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new SongEditorManager();
            }
        }
        return INSTANCE;
    }

    private SongEditorManager()
    {
        mapSongEditors = new HashMap<>();
        TopComponent.getRegistry().addPropertyChangeListener(this);

    }

    /**
     * Programmatically close the editors associated to a song.
     *
     * @param song
     * @param isShuttingDown If true, just do what's required to notify listeners.
     */
    public void closeSong(Song song, boolean isShuttingDown)
    {
        if (!isShuttingDown)
        {
            Editors editors = getEditors(song);
            if (editors != null)
            {
                editors.tcCle.close();  // This will make TopComponent.canClosed() be called first, with possibly user confirmation dialog
            }
        } else
        {
            songEditorClosed(song);
        }
    }

    /**
     * Do what's required to show a song in the application.
     * <p>
     * Create undomanager, create and show editors, etc. If song is already shown in an editor, just make its TopComponent active.
     *
     *
     * @param song
     * @param makeActive If true try to make the song musically active, see ActiveSongManager.
     */
    public void showSong(final Song song, boolean makeActive)
    {
        if (song == null)
        {
            throw new IllegalArgumentException("song=" + song);
        }

        for (Song s : getOpenedSongs())
        {
            if (s == song)
            {
                // Song is already edited, just make its editor active
                getEditors(s).getTcCle().requestActive();
                return;
            }
        }

        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                // Create the undo managers
                JJazzUndoManager undoManager = new JJazzUndoManager();
                JJazzUndoManagerFinder.getDefault().put(undoManager, song);
                JJazzUndoManagerFinder.getDefault().put(undoManager, song.getChordLeadSheet());
                JJazzUndoManagerFinder.getDefault().put(undoManager, song.getSongStructure());


                // Connect our undoManager to the song. Used by the MixConsole for MidiMix undo/redo changes
                // Note that for cls/sgs this will be done in each editor's constructor
                song.addUndoableEditListener(undoManager);


                // Create the editors
                CL_EditorTopComponent clTC = new CL_EditorTopComponent(song);
                Mode mode = WindowManager.getDefault().findMode("editor");
                mode.dockInto(clTC);
                clTC.open();

                SS_EditorTopComponent ssTC = new SS_EditorTopComponent(song);
                mode = WindowManager.getDefault().findMode("output");
                mode.dockInto(ssTC);
                ssTC.open();


                // Bind the editors together
                clTC.setPairedTopComponent(ssTC);
                ssTC.setPairedTopComponent(clTC);
                clTC.requestActive();
                song.addPropertyChangeListener(SongEditorManager.this);
                mapSongEditors.put(song, new Editors(clTC, ssTC));
                pcs.firePropertyChange(PROP_SONG_OPENED, false, song);


                // Try to make it active if requested
                var asm = ActiveSongManager.getInstance();
                if (makeActive && asm.isActivable(song) == null)
                {
                    try
                    {
                        MidiMix mm = MidiMixManager.getInstance().findMix(song);
                        asm.setActive(song, mm);
                    } catch (MidiUnavailableException ex)
                    {
                        // Should never be there
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        };
        // Make sure everything is run on the EDT
        SwingUtilities.invokeLater(run);
    }

    /**
     * Load a song from a file and show it.
     * <p>
     * Load the song from file and call showSong(song, makeActive).
     *
     * @param f
     * @param makeActive
     * @return The opened song, or null if a problem occured.
     */
    public Song showSong(File f, boolean makeActive)
    {
        for (Song s : getOpenedSongs())
        {
            if (s.getFile() == f)
            {
                getEditors(s).getTcCle().requestActive();
                return s;
            }
        }
        SongFactory sf = SongFactory.getInstance();
        Song song = sf.createFromFile(f);
        if (song != null)
        {
            showSong(song, makeActive);
            FileDirectoryManager.getInstance().setLastSongDirectory(f.getAbsoluteFile().getParentFile());
        }
        return song;
    }

    public List<Song> getOpenedSongs()
    {
        return new ArrayList<>(mapSongEditors.keySet());
    }

    /**
     * @param s
     * @return The editors which show song s. Can be null.
     */
    public Editors getEditors(Song s)
    {
        if (s == null)
        {
            throw new IllegalArgumentException("s=" + s);
        }
        return mapSongEditors.get(s);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    //=============================================================================
    // PropertyChange interface
    //=============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == TopComponent.getRegistry())
        {
            if (evt.getPropertyName() == TopComponent.Registry.PROP_TC_CLOSED)
            {
                if (evt.getNewValue() instanceof CL_EditorTopComponent)
                {
                    // User closed a song
                    CL_EditorTopComponent clTc = (CL_EditorTopComponent) evt.getNewValue();
                    songEditorClosed(clTc.getSongModel());
                }
            }
        } else if (evt.getSource() instanceof Song)
        {
            Song s = (Song) evt.getSource();
            if (evt.getPropertyName() == Song.PROP_MODIFIED_OR_SAVED && evt.getNewValue() == Boolean.FALSE)
            {
                songSaved(s);
            }
        }
    }

    //=============================================================================
    // Private
    //=============================================================================  
    /**
     * Song editor is closed, notify listeners and do some cleanup
     *
     * @param s
     */
    private void songEditorClosed(Song s)
    {
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(s);
        s.removeUndoableEditListener(um);
        s.removePropertyChangeListener(this);
        mapSongEditors.remove(s);
        pcs.firePropertyChange(PROP_SONG_CLOSED, false, s); // Event used for example by RecentSongProvider
        s.close(true);  // This will trigger an "activeSong=null" event from the ActiveSongManager
        updateActiveSong();
    }

    private void songSaved(Song s)
    {
        File f = s.getFile();
        assert f != null : "s=" + s;
        FileDirectoryManager.getInstance().setLastSongDirectory(f.getAbsoluteFile().getParentFile());
        pcs.firePropertyChange(PROP_SONG_SAVED, false, s);
    }

    private void updateActiveSong()
    {
        if (mapSongEditors.isEmpty())
        {
            return;
        }

        final Song song = (mapSongEditors.size() == 1) ? mapSongEditors.keySet().iterator().next() : null;

        // Need to wait for the new TopComponent to be selected, hence the runnable on the EDT
        Runnable r = () ->
        {
            Song sg;
            if (song != null)
            {
                sg = song;
            } else
            {
                // Find the currently selected ChordLeadSheet editor and get its song model
                WindowManager wm = WindowManager.getDefault();
                Mode mode = wm.findMode("editor");
                if (mode == null)
                {
                    return;
                }
                CL_EditorTopComponent clTc = (CL_EditorTopComponent) mode.getSelectedTopComponent();
                if (clTc == null)
                {
                    return;
                }
                sg = clTc.getSongModel();
            }
            activateSong(sg);
        };
        SwingUtilities.invokeLater(r);

    }

    private void activateSong(Song song)
    {
        ActiveSongManager am = ActiveSongManager.getInstance();
        if (am.isActivable(song) == null)
        {
            MidiMix mm = null;
            try
            {
                mm = MidiMixManager.getInstance().findMix(song);
            } catch (MidiUnavailableException ex)
            {
                LOGGER.warning("activateSong() Could not find MidiMix for song " + song.getName() + ".\n" + ex.getLocalizedMessage());
            }
            if (mm != null)
            {
                am.setActive(song, mm);
            }
        }
    }

    //=============================================================================
    // Inner classes
    //============================================================================= 
    public class Editors
    {

        private CL_EditorTopComponent tcCle;
        private SS_EditorTopComponent tcRle;

        protected Editors(CL_EditorTopComponent tcCle, SS_EditorTopComponent tcRle)
        {
            this.tcCle = tcCle;
            this.tcRle = tcRle;
        }

        public CL_EditorTopComponent getTcCle()
        {
            return tcCle;
        }

        public SS_EditorTopComponent getTcRle()
        {
            return tcRle;
        }
    }
}
