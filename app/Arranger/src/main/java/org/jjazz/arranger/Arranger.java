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
package org.jjazz.arranger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.Sequencer;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.harmony.api.Note;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.SessionConfig;
import org.jjazz.musiccontrol.api.playbacksession.UpdateProviderSongSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSessionOnePlay;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.event.RpValueChangedEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.openide.util.Exceptions;

/**
 * Do the arranger thing for a specific song part.
 * <p>
 * Listen to RhythmParameter changes of that song part.
 */
public class Arranger implements SgsChangeListener, PropertyChangeListener
{

    /**
     * The internal song name for the work song copy used by the arranger.
     */
    public static final String ARRANGER_WORK_SONG_NAME = "*ArrangerSONG*";
    public static final String PROP_PLAYING = "PropPlaying";
    public static final int SONG_PART_MAX_BAR_SIZE = 16;
    private final SongContext songContextRef;
    private final SongPart songPartRef;
    private SongContext songContextWork;
    private SongPart songPartWork;
    private CLI_ChordSymbol firstChordSymbol;
    private boolean playing;
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(Arranger.class.getSimpleName());

    /**
     * Create an arranger for the specified song context.
     *
     * @param sgContext Range must correspond to only 1 song part
     */
    public Arranger(SongContext sgContext)
    {
        checkNotNull(sgContext);
        checkArgument(sgContext.getSongParts().size() == 1, "songParts=%s", sgContext.getSongParts());
        songContextRef = sgContext;
        songPartRef = songContextRef.getSongParts().get(0);

        // Listen to RhythmParameter changes
        songContextRef.getSong().getSongStructure().addSgsChangeListener(this);

        // Listen to playback state changes
        MusicController.getInstance().addPropertyChangeListener(this);

    }

    /**
     * The SongContext used to create this Arranger.
     *
     * @return
     */
    public SongContext getSongContext()
    {
        return songContextRef;
    }


    public boolean isPlaying()
    {
        return playing;
    }

    public CLI_ChordSymbol getCurrentChordSymbol()
    {
        return firstChordSymbol;
    }

    /**
     * Update the tempo once arranger has started playing.
     * <p>
     * Do nothing if arranger is not started.
     *
     * @param tempo
     */
    public void updateTempo(int tempo)
    {
        if (songContextWork != null)
        {
            songContextWork.getSong().setTempo(tempo);
        }
    }


    /**
     * Start playback of songPart using Midi chord input.
     *
     * @throws MusicGenerationException If a problem occured.
     */
    public void play() throws MusicGenerationException
    {
        if (playing)
        {
            return;
        }


        // Make sure it's stopped
        MusicController mc = MusicController.getInstance();
        mc.stop();


        // Prepare session
        songContextWork = buildWorkContext(songContextRef, songPartRef, SONG_PART_MAX_BAR_SIZE);
        songPartWork = songContextWork.getSongParts().get(0);
        var cls = songContextWork.getSong().getChordLeadSheet();
        firstChordSymbol = cls.getItems(songPartWork.getParentSection(), CLI_ChordSymbol.class).get(0);


        var config = new SessionConfig(true, false, false, Sequencer.LOOP_CONTINUOUSLY, null);
        UpdateProviderSongSession dynSession = UpdateProviderSongSession.getSession(songContextWork, config, false, PlaybackSession.Context.ARRANGER);
        dynSession.setPreUpdateBufferTimeMs(5);     // Each user chord change generates only 2 song changes (remove and add 1 CLI_ChordSymbol)
        dynSession.setPostUpdateSleepTimeMs(100);    // This allow user to change chord quickly
        dynSession.setUserErrorExceptionHandler(null);  // User execption may occur depending on timing, as we remove then add a chord symbol at section start


        var updatableSession = new UpdatableSongSessionOnePlay(dynSession);
        mc.setPlaybackSession(updatableSession, false); // Can raise MusicGenerationException


        // Start playback        
        mc.play(0);


        playing = true;
        pcs.firePropertyChange(PROP_PLAYING, false, true);

        Analytics.incrementProperties("Play Arranger Mode", 1);
    }

    public void stop()
    {
        if (playing)
        {
            LOGGER.fine("stop()");
            playing = false;                // Must be before calling stop() below    
            MusicController.getInstance().stop();
            pcs.firePropertyChange(PROP_PLAYING, true, false);
        }
    }

    /**
     * Change the chord symbol being played.
     *
     * @param newCs
     */
    public void updateChordSymbol(ChordSymbol newCs)
    {
        LOGGER.log(Level.FINE, "updateChordSymbol() -- newCs={0} nanoTime()={1}", new Object[]
        {
            newCs, System.nanoTime()
        });

        if (songContextWork == null)
        {
            return;
        }

        ChordLeadSheet cls = songContextWork.getSong().getChordLeadSheet();
        assert firstChordSymbol != null;


        // Prepare the new CLI_ChordSymbol
        var firstEcs = firstChordSymbol.getData();
        var newEcs = firstEcs.getCopy(newCs, null, null, null);
        CLI_ChordSymbol newCliCs = CLI_Factory.getDefault().createChordSymbol(newEcs, firstChordSymbol.getPosition());


        // Update the chord leadsheet
        cls.removeItem(firstChordSymbol);
        cls.addItem(newCliCs);
        firstChordSymbol = newCliCs;

    }

    public void cleanup()
    {
        stop();
        songContextRef.getSong().getSongStructure().removeSgsChangeListener(this);
        MusicController.getInstance().removePropertyChangeListener(this);
        songContextWork = null;
        songPartWork = null;
        firstChordSymbol = null;
    }

    public void addPropertyListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // =========================================================================================
    // SgsChangeListener
    // =========================================================================================

    @Override
    public void songStructureChanged(SgsChangeEvent e)
    {
        if (e instanceof RpValueChangedEvent && e.getSongPart() == songPartRef)
        {
            // Forward the change to our work context
            RpValueChangedEvent rpe = (RpValueChangedEvent) e;
            SongStructure sgs = songContextWork.getSong().getSongStructure();
            sgs.setRhythmParameterValue(songPartWork, (RhythmParameter) rpe.getRhythmParameter(), rpe.getNewValue());
        }
    }

    // =================================================================================
    // PropertyChangeListener implementation
    // =================================================================================

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == MusicController.getInstance())
        {
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                MusicController.State state = (MusicController.State) evt.getNewValue();
                switch (state)
                {
                    case DISABLED:  // Fall down
                    case STOPPED:
                    case PAUSED:
                        stop();
                        break;
                    case PLAYING:
                        // Nothing
                        break;
                    default:
                        throw new AssertionError(state.name());
                }
            }
        }
    }

    // =========================================================================================
    // Private methods
    // =========================================================================================

    /**
     * Use the parameters to prepare a song context which contains only one SongPart.
     * <p>
     * Make sure there is only one chord symbol at the start of the chord leadsheet, and that the section/song part size is not greater than maxSptWorkNbBars.
     *
     * @param sgContext
     * @param spt
     * @param maxSptWorkNbBars
     * @return
     */
    private SongContext buildWorkContext(SongContext sgContext, SongPart spt, int maxSptWorkNbBars)
    {
        LOGGER.log(Level.FINE, "buildWorkContext() sgContext={0} spt={1}", new Object[]
        {
            sgContext, spt
        });

        // Get a work copy
        Song songWork = SongFactory.getInstance().getCopy(sgContext.getSong(), false, false);
        songWork.setName(ARRANGER_WORK_SONG_NAME);
        var sgsWork = songWork.getSongStructure();
        var spts = sgsWork.getSongParts();
        var clsWork = songWork.getChordLeadSheet();
        SongPart sptWork = sgsWork.getSongPart(spt.getStartBarIndex());

        try
        {
            // Keep only our SongPart
            List<SongPart> unusedSpts = new ArrayList<>(spts);
            unusedSpts.remove(sptWork);
            try
            {
                sgsWork.removeSongParts(unusedSpts);
            } catch (UnsupportedEditException ex)
            {
                // Should never happen
                Exceptions.printStackTrace(ex);
            }

            // Shorten section size if required
            if (sptWork.getNbBars() > maxSptWorkNbBars)
            {
                var parentSection = sptWork.getParentSection();
                clsWork.setSizeInBars(parentSection.getPosition().getBar() + maxSptWorkNbBars);  // throws UnsupportedEditException
            }

        } catch (UnsupportedEditException ex)
        {
            // Should never happen 
            Exceptions.printStackTrace(ex);
        }


        // Make sure there is only one chord symbol at the beginning
        CLI_Section cliSection = sptWork.getParentSection();
        var clis = clsWork.getItems(cliSection, CLI_ChordSymbol.class);
        if (clis.isEmpty())
        {
            // Add a "C" chord symbol at the start of the section
            ExtChordSymbol ecs = new ExtChordSymbol(new Note(0), new Note(0), ChordTypeDatabase.getDefault().getChordType(0));
            CLI_ChordSymbol cliCs = CLI_Factory.getDefault().createChordSymbol(ecs, cliSection.getPosition());
            clsWork.addItem(cliCs);

        } else if (!clis.get(0).getPosition().equals(cliSection.getPosition()))
        {
            // There is a chord symbol but not at the right position
            clsWork.moveItem(clis.get(0), cliSection.getPosition());
        }

        if (!clis.isEmpty())
        {
            clis.stream()
                    .skip(1)
                    .forEach(item -> clsWork.removeItem(item));
        }

        // LOGGER.info("              clsWork AFTER: " + toDumpString(clsWork));
        LOGGER.log(Level.FINE, "              sptWork AFTER: {0}", sptWork);
        LOGGER.log(Level.FINE, "              sgsWork AFTER: {0}", sgsWork);


        // Return the new context 
        return new SongContext(songWork, sgContext.getMidiMix(), sptWork.getBarRange());

    }

    static private String toDumpString(ChordLeadSheet cls)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(cls.toString());
        for (var item : cls.getItems())
        {
            if (item instanceof CLI_Section)
            {
                sb.append('\n').append(" ").append(item.getData()).append(item.getPosition()).append(" : ");
            } else
            {
                sb.append(item.getData()).append(item.getPosition()).append(" ");
            }
        }
        return sb.toString();
    }

    // =========================================================================================
    // Private class
    // =========================================================================================
}
