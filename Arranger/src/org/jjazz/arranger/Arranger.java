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

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.DynamicSongSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.util.api.ResUtil;
import org.openide.util.Exceptions;

/**
 * Do the arranger thing for a specific song and song part.
 */
public class Arranger implements SgsChangeListener, PropertyChangeListener
{

    public static final String PROP_PLAYING = "PropPlaying";
    private final SongContext songContextRef;
    private final SongPart songPartRef;
    private boolean playing;
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    /**
     * Create an arranger for the specified song context.
     *
     * @param sgContext Must contain only 1 songpart
     */
    public Arranger(SongContext sgContext)
    {
        checkNotNull(sgContext);
        Preconditions.checkArgument(sgContext.getSongParts().size() == 1, "songParts=%s", sgContext.getSongParts());
        songContextRef = sgContext;
        songPartRef = songContextRef.getSongParts().get(0);


        songContextRef.getSong().getSongStructure().addSgsChangeListener(this);
        MusicController.getInstance().addPropertyChangeListener(this);

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

        // Check config is OK
        var jms = JJazzMidiSystem.getInstance();
        if (jms.getDefaultInDevice() == null)
        {
            throw new MusicGenerationException(ResUtil.getString(getClass(), "ErrNoMidiInputDevice"));
        }
        MusicController mc = MusicController.getInstance();
        if (mc.getState().equals(MusicController.State.PLAYING))
        {
            throw new MusicGenerationException(ResUtil.getString(getClass(), "ErrSequenceAlreadyPlaying"));
        }
        mc.stop();


        
        startChordRecognitionThread();


        // Start playback
        SongContext workContext = buildWorkContext(4);
        var dynSession = DynamicSongSession.getSession(workContext, true, true, false, false, Sequencer.LOOP_CONTINUOUSLY, null);
        var updatableSession = UpdatableSongSession.getSession(dynSession);        
        mc.setPlaybackSession(updatableSession);
        mc.play(0);


        playing = true;
        pcs.firePropertyChange(PROP_PLAYING, false, true);
    }

    public void stop()
    {
        if (playing)
        {
            playing = false;
            pcs.firePropertyChange(PROP_PLAYING, true, false);
        }
    }

    public void nextSongPart()
    {

    }

    public void previousSongPart()
    {

    }

    public void cleanup()
    {
        songContextRef.getSong().getSongStructure().removeSgsChangeListener(this);
        MusicController.getInstance().removePropertyChangeListener(this);
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
                        // Nothing
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
     * Prepare a context with only one useful SongPart (or 2 if songPart uses an AdaptedRhythm).
     *
     * @param maxNbBars The max size of the useful SongPart (section).
     * @return
     */
    private SongContext buildWorkContext(int maxNbBars)
    {
        // Get a work copy
        Song songCopy = SongFactory.getInstance().getCopy(songContextRef.getSong(), false);
        var sgs = songCopy.getSongStructure();
        var spts = sgs.getSongParts();
        var cls = songCopy.getChordLeadSheet();
        SongPart spt = sgs.getSongPart(songPartRef.getStartBarIndex());
        Rhythm r = spt.getRhythm();

        try
        {
            if (!(r instanceof AdaptedRhythm))
            {
                // Easy 
                // Keep only our SongPart
                List<SongPart> unusedSpts = new ArrayList<>(spts);
                unusedSpts.remove(spt);
                try
                {
                    sgs.removeSongParts(unusedSpts);
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }

                // Shorten section size if required
                if (spt.getNbBars() > maxNbBars)
                {
                    var parentSection = spt.getParentSection();
                    cls.setSize(parentSection.getPosition().getBar() + maxNbBars - 1);  // throws UnsupportedEditException
                }

            } else
            {
                // We use an AdaptedRhythm, need to keep the source rhythm in a 2nd song part
                AdaptedRhythm ar = (AdaptedRhythm) r;
                Rhythm sr = ar.getSourceRhythm();

                // Clean everything
                sgs.removeSongParts(spts);

                // Prepare the added song parts
                List<SongPart> addSpts = new ArrayList<>();
                addSpts.add(spt.clone(null, 0, spt.getNbBars(), spt.getParentSection()));   // New work song part starts at 0

                // Find the first song part which uses the source rhythm
                SongPart srSpt = spts.stream()
                        .filter(spti -> spti.getRhythm() == sr)
                        .findAny()
                        .orElse(null);
                assert srSpt != null : "spts=" + spts;
                addSpts.add(srSpt.clone(null, spt.getNbBars(), srSpt.getNbBars(), srSpt.getParentSection()));   // starts after spt

                // Add the SongParts
                sgs.addSongParts(addSpts);

                // Shorten section size if required
                if (spt.getNbBars() > maxNbBars)
                {
                    var sptParentSection = spt.getParentSection();
                    int sptParentSectionBar = sptParentSection.getPosition().getBar();
                    var srSptParentSection = srSpt.getParentSection();
                    int srSptParentSectionBar = srSptParentSection.getPosition().getBar();
                    if (sptParentSectionBar > srSptParentSectionBar)
                    {
                        // We can just shorten the chord leadsheet
                        cls.setSize(sptParentSectionBar + maxNbBars - 1);   // throws UnsupportedEditException
                    } else
                    {
                        // Can't shorten size, need to move the next section
                        var nextSection = cls.getSection(sptParentSectionBar + spt.getNbBars());
                        cls.moveSection(nextSection, sptParentSectionBar + maxNbBars);
                    }
                }
            }
        } catch (UnsupportedEditException ex)
        {
            // Should never happen 
            Exceptions.printStackTrace(ex);
        }


        // Return the new context 
        SongContext workContext = new SongContext(songCopy, songContextRef.getMidiMix(), spt.getBarRange());
        return workContext;

    }

    private void startChordRecognitionThread()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
