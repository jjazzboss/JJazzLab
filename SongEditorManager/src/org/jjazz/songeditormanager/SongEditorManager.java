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
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * The central place where all song editors are created (from scratch, loaded from file, etc.) and managed.
 */
public class SongEditorManager implements PropertyChangeListener
{

    /**
     * This property change event is fired when a song is opened. NewValue is the song object.
     */
    public static final String PROP_SONG_OPENED = "SongOpened";
    /**
     * This property change event is fired when a song is closed. NewValue is the song object. Note that the Song object also
     * fires a Closed event when it is closed by the SongEditorManager.
     */
    public static final String PROP_SONG_CLOSED = "SongClosed";
    /**
     * This property change event is fired when a song is saved. NewValue is the song object.
     */
    public static final String PROP_SONG_SAVED = "SongSaved";
    private static SongEditorManager INSTANCE;
    private HashMap<Song, Editors> mapSongEditors;       // Don't use WeakHashMap here
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongEditorManager.class.getSimpleName());

    private SongEditorManager()
    {
        mapSongEditors = new HashMap<>();
        TopComponent.getRegistry().addPropertyChangeListener(this);
    }

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

    /**
     * Do what's required to show a song in the application.
     * <p>
     * Create undomanager, create and show editors, etc. If song is already shown in an editor, just make its editor active.
     *
     * @param song
     */
    public void showSong(final Song song)
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
                // Create the undo manager and associate it to both leadsheets
                JJazzUndoManager undoManager = new JJazzUndoManager();
                JJazzUndoManagerFinder.getDefault().put(undoManager, song);
                JJazzUndoManagerFinder.getDefault().put(undoManager, song.getChordLeadSheet());
                JJazzUndoManagerFinder.getDefault().put(undoManager, song.getSongStructure());

                // Connect our undoManager to the song. 
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

                updateActiveSong();
            }
        };
        // Make sure everything is run on the EDT
        org.jjazz.ui.utilities.Utilities.invokeLaterIfNeeded(run);
    }

    /**
     * Load a song from a file and show it.
     * <p>
     * If song is already shown in an editor, just activate the editor.
     *
     * @param f
     * @return The opened song, or null if a problem occured.
     */
    public Song showSong(File f)
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
            showSong(song);
            FileDirectoryManager.getInstance().setLastSongDirectory(f.getAbsoluteFile().getParentFile());
        }
        return song;
    }

    public List<Song> getOpenedSongs()
    {
        return new ArrayList<>(mapSongEditors.keySet());
    }

//   /**
//    * Get the edited song in the active CL_EditorTopComponent.
//    *
//    * @return Can be null.
//    */
//   public Song getEditedSong()
//   {
//      Mode mode = WindowManager.getDefault().findMode("editor");
//      TopComponent tc = mode.getSelectedTopComponent();
//      if (tc == null)
//      {
//         return null;
//      }
//      assert (tc instanceof CL_EditorTopComponent) : "tc=" + tc;
//      CL_EditorTopComponent clTc = (CL_EditorTopComponent) tc;
//      return clTc.getSongModel();
//   }
    /**
     * @param s
     * @return The editors which show song s.
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
                    CL_EditorTopComponent clTc = (CL_EditorTopComponent) evt.getNewValue();
                    songClosed(clTc.getSongModel());
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
     * Song is closed, do some cleanup
     *
     * @param s
     */
    private void songClosed(Song s)
    {
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(s);
        s.addUndoableEditListener(um);
        s.removePropertyChangeListener(this);
        mapSongEditors.remove(s);
        pcs.firePropertyChange(PROP_SONG_CLOSED, false, s);
        s.close(true);
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
        // If there is only one opened song, make it active by default
        if (mapSongEditors.size() == 1)
        {
            Song song = mapSongEditors.keySet().iterator().next();
            ActiveSongManager am = ActiveSongManager.getInstance();
            if (am.isActivable(song) == null)
            {
                MidiMix mm = null;
                try
                {
                    mm = MidiMixManager.getInstance().findMix(song);
                } catch (MidiUnavailableException ex)
                {
                    LOGGER.warning("updateActiveSong() Could not find MidiMix for song " + song.getName() + ".\n" + ex.getLocalizedMessage());
                }
                if (mm != null)
                {
                    am.setActive(song, mm);
                }
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
