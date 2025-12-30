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
package org.jjazz.songeditormanager;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.SwingUtilities;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhraseValue;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.jjazz.outputsynth.spi.OutputSynthManager;
import org.jjazz.songeditormanager.api.StartupShutdownSongManager;
import org.jjazz.songmemoviewer.api.SongMemoTopComponent;


@ServiceProvider(service = SongEditorManager.class)
public class SongEditorManagerImpl implements SongEditorManager, PropertyChangeListener
{

    private final HashMap<Song, SongEditorSet> mapSongEditors;       // Don't use WeakHashMap here
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongEditorManager.class.getSimpleName());


    public SongEditorManagerImpl()
    {
        mapSongEditors = new HashMap<>();
        TopComponent.getRegistry().addPropertyChangeListener(this);
        LOGGER.info("SongEditorManagerImpl() Started");

    }

    @Override
    public boolean closeSong(Song song, boolean enforce)
    {
        var editorSet = getSongEditorSet(song);
        Preconditions.checkNotNull(editorSet, "song=%s mapSongEditors=%s", song, mapSongEditors);

        boolean b = true;
        var clTc = editorSet.getCL_EditorTc();
        assert clTc != null : " song=" + song;
        if (enforce)
        {
            clTc.closeSilent();         // triggers a TopComponent.Registry event captured by SongEditorManager to complete the closing
        } else
        {
            b = clTc.close();           // user might cancel closing. If ok triggers a TopComponent.Registry event captured by SongEditorManager to complete the closing
        }
        return b;
    }


    @Override
    public void showSong(final Song song, boolean makeActive, boolean savable)
    {
        Preconditions.checkNotNull(song);


        // Check if song is already opened
        for (Song s : getOpenedSongs())
        {
            if (s == song)
            {
                // Song is already edited, just make its editor active
                getSongEditorSet(s).getCL_EditorTc().requestActive();


                // Try to make it active if requested
                var asm = ActiveSongManager.getDefault();
                if (makeActive && asm.isActivable(song) == null)
                {
                    MidiMix mm = MidiMixManager.getDefault().findExistingMix(s);
                    SwingUtilities.invokeLater(() -> asm.setActive(s, mm));
                }
                return;
            }
        }


        // Retrieve MidiMix
        MidiMix midiMix;
        try
        {
            midiMix = MidiMixManager.getDefault().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            // Should never be there
            Exceptions.printStackTrace(ex);
            return;
        }


        Runnable openEditorsTask = () -> 
        {
            // Create the undo managers
            JJazzUndoManager undoManager = new JJazzUndoManager();
            JJazzUndoManagerFinder.getDefault().put(song, undoManager);
            JJazzUndoManagerFinder.getDefault().put(song.getChordLeadSheet(), undoManager);
            JJazzUndoManagerFinder.getDefault().put(song.getSongStructure(), undoManager);


            // Connect our undoManager to the song (e.g. add/removed UserPhrase)
            // Note that for cls/sgs this will be done in each editor's constructor
            song.addUndoableEditListener(undoManager);


            // Create the main editors
            CL_EditorTopComponent clTC = new CL_EditorTopComponent(song);
            Mode mode = WindowManager.getDefault().findMode(CL_EditorTopComponent.MODE);
            mode.dockInto(clTC);
            clTC.open();

            SS_EditorTopComponent ssTC = new SS_EditorTopComponent(song);
            mode = WindowManager.getDefault().findMode(SS_EditorTopComponent.MODE);
            mode.dockInto(ssTC);
            ssTC.open();


            var editorSet = new SongEditorSet(clTC, ssTC);
            mapSongEditors.put(song, editorSet);


            var userChannels = midiMix.getUserChannels();
            if (!userChannels.isEmpty())
            {
                var preTc = showPianoRollEditorForUserTrack(song, midiMix, (UserRhythmVoice) midiMix.getRhythmVoice(userChannels.get(0)));
                editorSet.setPianoRollEditor(preTc);
            }


            if (!Song.DEFAULT_COMMENTS.equals(song.getComments()))
            {
                SongMemoTopComponent.getInstance().open();
            }


            // Activate the chord leadsheet editor
            clTC.requestActive();


            song.addPropertyChangeListener(SongEditorManagerImpl.this);
            pcs.firePropertyChange(PROP_SONG_OPENED, false, song);


            // Try to make it active if requested
            var asm = ActiveSongManager.getDefault();
            if (makeActive && asm.isActivable(song) == null)
            {
                // To avoid problem (Issue #109 Tempo sometimes not right after 1st song auto-loaded), use invokeLater() to make sure setActive() comes after enough the clTc.requestActive() above.
                SwingUtilities.invokeLater(() -> asm.setActive(song, midiMix));
            }


            // Adjust zoom on the song structure editor so we can see all song parts
            SwingUtilities.invokeLater(() -> ssTC.getEditor().setZoomHFactorToFitWidth(ssTC.getWidth()));


            // Upon initialization the song editors will update the song by calling Song.putClientProperty() to store some UI settings
            // like quantization of zoom factors. This makes the song appear as "modified/savable" by default.
            if (!savable)
            {
                SwingUtilities.invokeLater(() -> song.setSaveNeeded(false));
            }
        };

        // Make sure everything is run on the EDT
        SwingUtilities.invokeLater(openEditorsTask);


        // Open the links in the song memo
        if (makeActive)
        {
            new Thread(() -> openLinks(song)).start();
        }


    }

    /**
     * Load or import a song from a file then show it.
     * <p>
     * Load or import the song from file, fix the MidiMix if required, then call showSong(song, makeActive).
     *
     * @param f
     * @param makeActive
     * @param updateLastSongDirectory If true and the file is not already shown, update the LastSongDirectory in FileDirectoryManager.
     * @return The created song from file f
     * @throws org.jjazz.song.api.SongCreationException
     */
    @Override
    public Song showSong(File f, boolean makeActive, boolean updateLastSongDirectory) throws SongCreationException
    {

        // Check if file is already opened
        for (Song s : getOpenedSongs())
        {
            if (s.getFile() == f)
            {
                showSong(s, makeActive, false);
                return s;
            }
        }


        // Open or import the song file
        Song song = null;
        boolean savable = false;
        String ext = Utilities.getExtension(f.getName());
        if (ext.equalsIgnoreCase("sng"))
        {
            song = Song.loadFromFile(f);       // throws SongCreationException

        } else
        {
            var songImporters = SongImporter.getMatchingImporters(SongImporter.getImporters(), ext);
            if (songImporters.isEmpty())
            {
                throw new SongCreationException("No matching song importer for file " + f.getName());
            }
            var songImporter = songImporters.get(0);
            try
            {
                song = songImporter.importFromFile(f);
            } catch (IOException ex)
            {
                throw new SongCreationException(ex.getLocalizedMessage());
            }
            savable = true;
        }


        // Read song from file
        // Fix the MidiMix if needed
        try
        {
            var mm = MidiMixManager.getDefault().findMix(song);
            OutputSynthManager.getDefault().getDefaultOutputSynth().fixInstruments(mm, true);
        } catch (MidiUnavailableException ex)
        {
            Exceptions.printStackTrace(ex);
        }


        // Update last song directory
        if (updateLastSongDirectory)
        {
            FileDirectoryManager.getInstance().setLastSongDirectory(f.getAbsoluteFile().getParentFile());
        }


        // Show the song
        showSong(song, makeActive, savable);


        return song;
    }


    /**
     * Open (or show) the song's PianoRollEditor and set it up to edit a user track associated to userRhythmVoice.
     *
     * @param song
     * @param midiMix
     * @param userRhythmVoice
     * @return
     */
    @Override
    public PianoRollEditorTopComponent showPianoRollEditorForUserTrack(Song song, MidiMix midiMix, UserRhythmVoice userRhythmVoice)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(midiMix);
        Preconditions.checkNotNull(userRhythmVoice);

        LOGGER.log(Level.FINE, "showPianoRollEditor() song={0} userRhythmVoice={1}", new Object[]
        {
            song, userRhythmVoice
        });


        // Create editor TopComponent and open it if required        
        var preTc = showPianoRollEditor(song);

        if (song.getSize() == 0)
        {
            return preTc;
        }

        // Prepare keyMap
        DrumKit.KeyMap keyMap = null;               // melodic phrase by default        
        var drumKit = midiMix.getInstrumentMix(userRhythmVoice).getInstrument().getDrumKit();     // can be null if instrument is the VoidInstrument        
        if (userRhythmVoice.isDrums())
        {
            // Use instrument keymap if possible
            keyMap = drumKit == null ? userRhythmVoice.getDrumKit().getKeyMap() : drumKit.getKeyMap();
        } else if (drumKit != null)
        {
            // There is an inconcistency: should be a melodic instrument !
            // Display the editor in drums mode anyway -this will also contribute to solve issue #653 (before 5.0.2 MidiMix wrongly loaded all UserRhythmVoice as melodic)
            keyMap = drumKit.getKeyMap();
        }


        // Set model of piano roll editor
        String initialPhraseName = userRhythmVoice.getName();
        int initialChannel = midiMix.getChannel(userRhythmVoice);
        assert initialChannel != -1 : "midiMix=" + midiMix + " userRhythmVoice=" + userRhythmVoice;
        var userPhrase = song.getUserPhrase(initialPhraseName);
        String title = buildPrEditorUserTrackTitle(initialPhraseName, initialChannel);
        preTc.setTitle(title);
        preTc.setModelForUserPhrase(userPhrase, initialChannel, keyMap);


        // Prepare listeners to:
        // - Stop listening when editor is destroyed or its model is changed  
        // - Update title if phrase name or channel is changed
        // - Remove PianoRollEditor if user phrase is removed
        var editor = preTc.getEditor();
        VetoableChangeListener vcl = evt -> 
        {
            if (evt.getSource() == song)
            {
                if (evt.getPropertyName().equals(Song.PROP_VETOABLE_USER_PHRASE))
                {
                    // Close the editor if our phrase is removed
                    if (evt.getOldValue() instanceof String && evt.getNewValue() instanceof Phrase p && p == userPhrase)
                    {
                        preTc.close();
                    }
                }
            }
        };
        DrumKit.KeyMap keyMap2 = keyMap;
        PropertyChangeListener pcl = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                // LOGGER.severe("editUserPhrase.propertyChange() e=" + Utilities.toDebugString(evt));
                if (evt.getSource() == editor)
                {
                    switch (evt.getPropertyName())
                    {
                        case PianoRollEditor.PROP_MODEL_PHRASE, PianoRollEditor.PROP_EDITOR_ALIVE ->
                        {
                            editor.removePropertyChangeListener(this);
                            midiMix.removePropertyChangeListener(this);
                            song.removeVetoableChangeListener(vcl);
                        }
                    }
                } else if (evt.getSource() == midiMix)
                {
                    if (evt.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE))
                    {
                        // Used for UserRhythmVoice name change
                        var newRv = (RhythmVoice) evt.getNewValue();
                        var newRvName = newRv.getName();
                        if (newRv instanceof UserRhythmVoice && song.getUserPhrase(newRvName) == userPhrase)
                        {
                            int channel = midiMix.getChannel(newRv);                // Normally unchanged
                            preTc.setTitle(buildPrEditorUserTrackTitle(newRvName, channel));
                        }

                    } else if (evt.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE_CHANNEL))
                    {
                        // Used to change channel of a RhythmVoice
                        int newChannel = (int) evt.getNewValue();
                        var rv = midiMix.getRhythmVoice(newChannel);
                        var rvName = rv.getName();
                        if (rv instanceof UserRhythmVoice && song.getUserPhrase(rvName) == userPhrase)
                        {
                            preTc.setModelForUserPhrase(userPhrase, newChannel, keyMap2);
                            preTc.setTitle(buildPrEditorUserTrackTitle(rvName, newChannel));
                        }
                    }
                }
            }
        };


        editor.addPropertyChangeListener(pcl);
        midiMix.addPropertyChangeListener(pcl);
        song.addVetoableChangeListener(vcl);


        preTc.requestActive();

        return preTc;
    }

    /**
     * Open (or show) the song's PianoRollEditor and set it up to edit a custom phrase of a SongPart.
     *
     * @param song
     * @param spt  The SongPart for which a phrase is customized
     * @param rv   The RhythmVoice for which we customize the phrase
     * @param p    The customized phrase
     *
     * @return
     */
    @Override
    public PianoRollEditorTopComponent showPianoRollEditorForSptCustomPhrase(Song song, MidiMix midiMix, SongPart spt, RhythmVoice rv, Phrase p)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(spt);
        Preconditions.checkNotNull(rv);
        Preconditions.checkNotNull(p);
        Preconditions.checkArgument(spt.getRhythm() == rv.getContainer(), "spt=%s rv=%s", spt, rv);


        // Create editor TopComponent and open it if required
        var preTc = showPianoRollEditor(song);
        var editor = preTc.getEditor();


        // Update the editor model
        DrumKit drumKit = midiMix.getInstrumentMix(rv).getInstrument().getDrumKit();
        DrumKit.KeyMap keyMap = drumKit == null ? null : drumKit.getKeyMap();
        int channel = midiMix.getChannel(rv);
        preTc.setModelForSongPartCustomPhrase(spt, p, channel, keyMap);
        preTc.setTitle(buildPrEditorSongPartPhraseTitle(rv.getName(), channel));
        preTc.requestActive();


        // Listen to RP value changes while editor edits our model, and MidiMix for channel changes        
        PropertyChangeListener listener = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent e)
            {
                if (e.getSource() == spt)
                {
                    if (e.getPropertyName().equals(SongPart.PROP_RP_VALUE)
                            && e.getOldValue() instanceof RP_SYS_CustomPhrase
                            && e.getNewValue() instanceof RP_SYS_CustomPhraseValue newRpValue)
                    {
                        // Our rpValue was replaced, check if our customized phrase is still there
                        Phrase newP = newRpValue.getCustomizedPhrase(rv);
                        if (newP != p)
                        {
                            // It's not there anymore, close the editor
                            preTc.close();
                        }
                    }
                } else if (e.getSource() == editor)
                {
                    switch (e.getPropertyName())
                    {
                        case PianoRollEditor.PROP_MODEL_PHRASE, PianoRollEditor.PROP_EDITOR_ALIVE ->
                        {
                            editor.removePropertyChangeListener(this);
                            spt.removePropertyChangeListener(this);
                            midiMix.removePropertyChangeListener(this);
                        }
                    }
                } else if (e.getSource() == midiMix)
                {
                    if (e.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE_CHANNEL)
                            || e.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE))
                    {
                        int channel = midiMix.getChannel(rv);
                        preTc.setModelForSongPartCustomPhrase(spt, p, channel, keyMap);
                        preTc.setTitle(buildPrEditorSongPartPhraseTitle(rv.getName(), channel));
                    }
                }
            }
        };

        editor.addPropertyChangeListener(listener);
        spt.addPropertyChangeListener(listener);
        midiMix.addPropertyChangeListener(listener);

        return preTc;
    }


    @Override
    public List<Song> getOpenedSongs()
    {
        return new ArrayList<>(mapSongEditors.keySet());
    }

    /**
     * Get the editors which show song s.
     *
     * @param s
     * @return Can be null.
     */
    @Override
    public SongEditorSet getSongEditorSet(Song s)
    {
        if (s == null)
        {
            throw new IllegalArgumentException("s=" + s);
        }
        return mapSongEditors.get(s);
    }

    @Override
    public void setOpenLastFilesUponStartup(boolean b)
    {
        StartupShutdownSongManager.getInstance().setOpenLastFilesUponStartup(b);
    }

    @Override
    public boolean isOpenLastFilesUponStartup()
    {
        return StartupShutdownSongManager.getInstance().isOpenLastFilesUponStartup();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    //=============================================================================
    // PropertyChangeListener interface
    //=============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == TopComponent.getRegistry())
        {
            if (evt.getPropertyName().equals(TopComponent.Registry.PROP_TC_CLOSED))
            {
                if (evt.getNewValue() instanceof CL_EditorTopComponent clTc)
                {
                    // User closed a song, close all other editors
                    completeSongClosing(clTc.getSongModel());

                } else if (evt.getNewValue() instanceof SS_EditorTopComponent ssTc)
                {
                    // User closed a song, close all other editors
                    completeSongClosing(ssTc.getSongModel());

                } else if (evt.getNewValue() instanceof PianoRollEditorTopComponent prTc)
                {
                    getSongEditorSet(prTc.getSong()).setPianoRollEditor(null);
                }
            } else if (evt.getPropertyName().equals(TopComponent.Registry.PROP_ACTIVATED))
            {
                if (evt.getNewValue() instanceof CL_EditorTopComponent clTc)
                {
                    // Make the corresponding ssTc visible
                    Song song = clTc.getSongModel();
                    var editors = getSongEditorSet(song);
                    if (editors != null)        // Might be null when application is exiting
                    {
                        editors.getSS_EditorTc().requestVisible();
                    }

                } else if (evt.getNewValue() instanceof SS_EditorTopComponent ssTc)
                {
                    // Make the corresponding clTc visible, unless the song pianoroll editor is already visible
                    Song song = ssTc.getSongModel();
                    var editors = getSongEditorSet(song);
                    if (editors != null)
                    {
                        var clTc = editors.getCL_EditorTc();
                        var prTc = editors.getPianoRollTc();
                        if (prTc == null || WindowManager.getDefault().findMode(PianoRollEditorTopComponent.MODE).getSelectedTopComponent() != prTc)
                        {
                            clTc.requestVisible();
                        }
                    }
                } else if (evt.getNewValue() instanceof PianoRollEditorTopComponent prTc)
                {
                    // Make the corresponding ssTc visible
                    Song song = prTc.getSong();
                    var editors = getSongEditorSet(song);
                    if (editors != null)
                    {
                        editors.getSS_EditorTc().requestVisible();
                    }
                }
            }
        } else if (evt.getSource() instanceof Song s)
        {
            if (evt.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED_OR_RESET) && evt.getOldValue() == Boolean.TRUE && evt.getNewValue() == Boolean.FALSE)
            {
                songSaved(s);
            }
        }
    }

    //=============================================================================
    // Private
    //=============================================================================  

    private void songSaved(Song s)
    {
        File f = s.getFile();   // Might be null for a new song if Song.resetModified() is called
        if (f != null)
        {
            FileDirectoryManager.getInstance().setLastSongDirectory(f.getAbsoluteFile().getParentFile());
            pcs.firePropertyChange(PROP_SONG_SAVED, false, s);
        }
    }

    /**
     * Either CL_EditorTopComponent or SS_EditorTopComponent was closed, close the remaining editors and clean up.
     *
     * @param song
     */
    private void completeSongClosing(Song song)
    {
        var editors = getSongEditorSet(song);
        assert editors != null : "song=" + song + " mapSongEditors=" + mapSongEditors;


        // Disable listening while closing other TopComponents
        TopComponent.getRegistry().removePropertyChangeListener(this);


        var clTc = editors.getCL_EditorTc();
        var ssTc = editors.getSS_EditorTc();
        if (clTc.isOpened())
        {
            // If we're here, it means SS_EditorTc was closed first
            assert !ssTc.isOpened();
            editors.getCL_EditorTc().closeSilent();
        } else
        {
            // If we're here, it means CL_EditorTc was closed first            
            assert ssTc.isOpened();
            ssTc.closeSilent();
        }


        // PianoRollEditor
        if (editors.getPianoRollTc() != null)
        {
            editors.getPianoRollTc().close();   // This never triggers user confirmation dialog
        }


        // Reenable listening to TopComponents closing events
        TopComponent.getRegistry().addPropertyChangeListener(this);


        // Cleanup
        song.removePropertyChangeListener(this);
        song.removeUndoableEditListener(JJazzUndoManagerFinder.getDefault().get(song));
        mapSongEditors.remove(song);
        pcs.firePropertyChange(PROP_SONG_CLOSED, false, song); // Event used for example by RecentSongProvider
        song.close(true);  // This will trigger an "activeSong=null" event from the ActiveSongManager
        findSongToBeActivated();
    }


    /**
     * Show the PianoRollEditorTopComponent for the specified song.
     * <p>
     * Song must be already edited. Create the PianoRollEditorTopComponent at the appropriate position, or just activate it.
     *
     * @param song
     * @return
     */
    private PianoRollEditorTopComponent showPianoRollEditor(Song song)
    {
        Preconditions.checkNotNull(song);


        var songEditors = getSongEditorSet(song);
        if (songEditors == null)
        {
            throw new IllegalArgumentException("song=" + song + " mapSongEditors.keySet()=" + mapSongEditors.keySet());
        }


        var tc = songEditors.getPianoRollTc();
        if (tc != null)
        {
            tc.requestActive();
        } else
        {
            // Create and open next to CL_EditorTopComponent
            tc = new PianoRollEditorTopComponent(song, PianoRollEditorSettings.getDefault());
            int posInMode = songEditors.getCL_EditorTc().getTabPosition();
            assert posInMode != -1;
            tc.openAtTabPosition(posInMode + 1);
            songEditors.setPianoRollEditor(tc);
        }

        return tc;
    }

    private void findSongToBeActivated()
    {

        // Need to wait for the new TopComponent to be selected, or for the shutdown to complete, hence the runnable on the EDT
        Runnable r = () -> 
        {
            if (mapSongEditors.isEmpty())
            {
                return;
            }
            Song song = (mapSongEditors.size() == 1) ? mapSongEditors.keySet().iterator().next() : null;

            if (song == null)
            {
                // Find the currently selected ChordLeadSheet editor and get its song model
                WindowManager wm = WindowManager.getDefault();
                Mode mode = wm.findMode("editor");
                if (mode == null)
                {
                    return;
                }

                var tc = mode.getSelectedTopComponent();
                if (tc instanceof CL_EditorTopComponent clTc)
                {
                    song = clTc.getSongModel();
                } else if (tc instanceof PianoRollEditorTopComponent preTc)
                {
                    song = preTc.getSong();
                } else
                {
                    return;
                }
            }
            activateSong(song);
        };
        SwingUtilities.invokeLater(r);

    }


    private void activateSong(Song song)
    {
        ActiveSongManager am = ActiveSongManager.getDefault();
        if (am.isActivable(song) == null)
        {
            MidiMix mm = null;
            try
            {
                mm = MidiMixManager.getDefault().findMix(song);
            } catch (MidiUnavailableException ex)
            {
                LOGGER.log(Level.WARNING, "activateSong() Could not find MidiMix for song {0}.\n{1}", new Object[]
                {
                    song.getName(),
                    ex.getMessage()
                });
            }
            if (mm != null)
            {
                am.setActive(song, mm);
            }
        }
    }

    private String buildPrEditorUserTrackTitle(String initialPhraseName, int initialChannel)
    {
        var title = ResUtil.getString(SongEditorManagerImpl.class, "UserTrackTitle", initialPhraseName, initialChannel + 1);
        return title;
    }

    private String buildPrEditorSongPartPhraseTitle(String initialPhraseName, int initialChannel)
    {
        var title = ResUtil.getString(SongEditorManagerImpl.class, "SongPartCustomPhraseTitle", initialPhraseName, initialChannel + 1);
        return title;
    }

    private void openLinks(Song song)
    {
        var uris = Utilities.extractURIs(song.getComments(), "file", "https?");
        if (!uris.isEmpty())
        {
            LOGGER.log(Level.INFO, "openLinks() Opening links in song comments {0}", song.getName());
            uris.stream()
                    .limit(4) // Security
                    .forEach(uri -> Utilities.systemOpenURI(uri));
        }
    }
}
