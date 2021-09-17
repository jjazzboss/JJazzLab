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
package org.jjazz.arranger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.midi.Sequencer;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.ChordTypeDatabase;
import org.jjazz.harmony.api.Note;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.DynamicSongSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.event.RpChangedEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.util.api.ResUtil;
import org.openide.util.Exceptions;

/**
 * Do the arranger thing for a specific song part.
 * <p>
 * Listen to RhythmParameter changes of that song part.
 */
public class Arranger implements SgsChangeListener, PropertyChangeListener
{

    public static final String PROP_PLAYING = "PropPlaying";
    public static final int SONG_PART_BAR_SIZE = 4;
    private final SongContext songContextRef;
    private final SongPart songPartRef;
    private SongContext songContextWork;
    private SongPart songPartWork;
    private CLI_ChordSymbol firstChordSymbol;
    private boolean playing;
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(Arranger.class.getSimpleName());  //NOI18N    

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
        songContextWork = buildWorkContext(songContextRef, songPartRef, SONG_PART_BAR_SIZE);
        songPartWork = songContextWork.getSongParts().get(0);
        firstChordSymbol = songContextWork.getSong().getChordLeadSheet().getItems(songPartWork.getParentSection(), CLI_ChordSymbol.class).get(0);
        var dynSession = DynamicSongSession.getSession(songContextWork, true, true, false, false, false, Sequencer.LOOP_CONTINUOUSLY, null);
        var updatableSession = UpdatableSongSession.getSession(dynSession);

        // Start playback
        mc.setPlaybackSession(updatableSession);
        mc.play(0);

        playing = true;
        pcs.firePropertyChange(PROP_PLAYING, false, true);
    }

    public void stop()
    {
        if (playing)
        {
            LOGGER.severe("stop()");
            MusicController.getInstance().stop();
            playing = false;
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
        LOGGER.info("updateChordSymbol() -- newCs=" + newCs);
        if (songContextWork == null)
        {
            LOGGER.warning("updateChordSymbol() songContextWork is null!");
        }
        
        ChordLeadSheet cls = songContextWork.getSong().getChordLeadSheet();
        assert firstChordSymbol != null;

        var old = firstChordSymbol.getData().getChordSymbol(null);

        // Prepare the new CLI_ChordSymbol
        var firstEcs = firstChordSymbol.getData();
        var newEcs = new ExtChordSymbol(newCs, firstEcs.getRenderingInfo(), firstEcs.getAlternateChordSymbol(), firstEcs.getAlternateFilter());
        CLI_ChordSymbol newCliCs = CLI_Factory.getDefault().createChordSymbol(cls, newEcs, firstChordSymbol.getPosition());


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
    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void songStructureChanged(SgsChangeEvent e)
    {
        if (e instanceof RpChangedEvent && e.getSongPart() == songPartRef)
        {
            // Forward the change to our work context
            RpChangedEvent rpe = (RpChangedEvent) e;
            SongStructure sgs = songContextWork.getSong().getSongStructure();
            sgs.setRhythmParameterValue(songPartWork, (RhythmParameter) rpe.getRhytmParameter(), rpe.getNewValue());
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
     * Use the parameters to prepare a new work song context with only one work SongPart (or 2 if songPart uses an AdaptedRhythm).
     * <p>
     * Size of the work song part is newSptSize. Make sure there is only one chord symbol at the start of the chord leadsheet.
     *
     * @param sgContext
     * @param spt
     * @param workSptSize
     * @return
     */
    private SongContext buildWorkContext(SongContext sgContext, SongPart spt, int workSptSize)
    {
        // Get a work copy
        Song songWork = SongFactory.getInstance().getCopy(sgContext.getSong(), false);
        var sgsWork = songWork.getSongStructure();
        var spts = sgsWork.getSongParts();
        var clsWork = songWork.getChordLeadSheet();
        SongPart sptWork = sgsWork.getSongPart(spt.getStartBarIndex());
        Rhythm r = sptWork.getRhythm();

        try
        {
            if (!(r instanceof AdaptedRhythm))
            {
                // Easy 
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

                // Adjust section size if required
                if (sptWork.getNbBars() != workSptSize)
                {
                    var parentSection = sptWork.getParentSection();
                    clsWork.setSize(parentSection.getPosition().getBar() + workSptSize - 1);  // throws UnsupportedEditException
                }

            } else
            {
                // We use an AdaptedRhythm, need to keep the source rhythm in a 2nd song part
                AdaptedRhythm ar = (AdaptedRhythm) r;
                Rhythm sr = ar.getSourceRhythm();

                // Clean everything
                sgsWork.removeSongParts(spts);

                // Prepare the added song parts
                List<SongPart> addSpts = new ArrayList<>();
                addSpts.add(sptWork.clone(null, 0, sptWork.getNbBars(), sptWork.getParentSection()));   // New work song part starts at 0

                // Find the first song part which uses the source rhythm
                SongPart srSpt = spts.stream()
                        .filter(spti -> spti.getRhythm() == sr)
                        .findAny()
                        .orElse(null);
                assert srSpt != null : "spts=" + spts;
                addSpts.add(srSpt.clone(null, sptWork.getNbBars(), srSpt.getNbBars(), srSpt.getParentSection()));   // starts after spt

                // Add the SongParts
                sgsWork.addSongParts(addSpts);

                // Adjust section size if required
                if (sptWork.getNbBars() > workSptSize)
                {
                    var sptParentSection = sptWork.getParentSection();
                    int sptParentSectionBar = sptParentSection.getPosition().getBar();
                    var srSptParentSection = srSpt.getParentSection();
                    int srSptParentSectionBar = srSptParentSection.getPosition().getBar();
                    if (sptParentSectionBar > srSptParentSectionBar)
                    {
                        // We can just shorten the chord leadsheet
                        clsWork.setSize(sptParentSectionBar + workSptSize - 1);   // throws UnsupportedEditException
                    } else
                    {
                        // Can't shorten size, need to move the next section
                        var nextSection = clsWork.getSection(sptParentSectionBar + sptWork.getNbBars());
                        clsWork.moveSection(nextSection, sptParentSectionBar + workSptSize);
                    }
                }
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
            ExtChordSymbol ecs = new ExtChordSymbol(new Note(0), ChordTypeDatabase.getInstance().getChordType(0));
            CLI_ChordSymbol cliCs = CLI_Factory.getDefault().createChordSymbol(clsWork, ecs, cliSection.getPosition());
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


        // Return the new context 
        return new SongContext(songWork, sgContext.getMidiMix(), sptWork.getBarRange());

    }


    // =========================================================================================
    // Private class
    // =========================================================================================
}
