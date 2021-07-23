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
package org.jjazz.musiccontrol.api.playbacksession;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythmmusicgeneration.api.ContextChordSequence;
import org.jjazz.rhythmmusicgeneration.api.SongContext;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.util.api.IntRange;

/**
 * A PlaybackSession which allows the sequence to be updated (add/remove MidiEvents without changing the Sequence size) in
 * realtime while playing.
 */
public class DynamicSongSession implements PropertyChangeListener, PlaybackSession, PositionProvider, ChordSymbolProvider, SongContextProvider
{

    private TrackSet modifiableTrackSet;        // Either trackSetA or trackSetB
    private TrackSet trackSetA;
    private TrackSet trackSetB;
    private SongContextSession songContextSession;
    private Sequence sequence;
    private static final Logger LOGGER = Logger.getLogger(DynamicSongSession.class.getSimpleName());  //NOI18N

    /**
     * Create a DynamicSongSession with the specified parameters.
     *
     * @param sgContext
     * @param enablePlaybackTransposition
     * @param enableClickTrack
     * @param enablePrecountTrack
     * @param enableControlTrack
     * @param postProcessors Can be null, passed to the MidiSequenceBuilder in charge of creating the sequence.
     */
    public DynamicSongSession(SongContext sgContext, boolean enablePlaybackTransposition, boolean enableClickTrack, boolean enablePrecountTrack, boolean enableControlTrack,
            MusicGenerator.PostProcessor... postProcessors)
    {
        songContextSession = new SongContextSession(sgContext, enablePlaybackTransposition, enableClickTrack, enablePrecountTrack, enableControlTrack, postProcessors);
    }

    @Override
    public void generate() throws MusicGenerationException
    {
        songContextSession.generate();

    }

    /**
     * Get the original sequence and create additional empty tracks to allow "double buffering modification" via muting/unmuting
     * tracks.
     *
     * @return
     */
    @Override
    public Sequence getSequence()
    {
        sequence = songContextSession.getSequence();

        trackSetA = new TrackSet(0, sequence.getTracks());
        
        
        return sequence;
    }

    public void updateSequence(Map<Integer, Track> mapIdTrack)
    {
        
    }
    
    @Override
    public State getState()
    {
        return songContextSession.getState();
    }

    @Override
    public int getTempo()
    {
        return songContextSession.getTempo();
    }

    @Override
    public HashMap<Integer, Boolean> getTracksMuteStatus()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long getLoopEndTick()
    {
        return songContextSession.getLoopEndTick();
    }

    @Override
    public long getLoopStartTick()
    {
        return songContextSession.getLoopStartTick();
    }

    @Override
    public int getLoopCount()
    {
        return songContextSession.getLoopCount();
    }

    @Override
    public IntRange getBarRange()
    {
        return songContextSession.getBarRange();
    }

    @Override
    public long getTick(int barIndex)
    {
        return songContextSession.getTick(barIndex);

    }

    @Override
    public void cleanup()
    {
        songContextSession.cleanup();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        songContextSession.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        songContextSession.removePropertyChangeListener(l);
    }

    // ==========================================================================================================
    // SongContextProvider implementation
    // ==========================================================================================================    
    @Override
    public SongContext getSongContext()
    {
        return songContextSession.getSongContext();
    }

    // ==========================================================================================================
    // PositionProvider implementation
    // ==========================================================================================================    
    @Override
    public List<Position> getPositions()
    {
        return songContextSession.getPositions();
    }

    // ==========================================================================================================
    // ChordSymbolProvider implementation
    // ==========================================================================================================    
    @Override
    public ContextChordSequence getContextChordGetSequence()
    {
        return songContextSession.getContextChordGetSequence();
    }

    // ==========================================================================================================
    // PropertyChangeListener implementation
    // ==========================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {

        LOGGER.fine("propertyChange() e=" + e);


        if (e.getSource() == songContextSession)
        {

        }


    }

    @Override
    public String toString()
    {
        return "DynamicSongSession=[" + songContextSession + "]";
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================
    /**
     * A set of tracks.
     */
    private class TrackSet
    {

        int trackIdOffset = 0;
        Track[] tracks;

        public TrackSet(int trackIdOffset, Track[] tracks)
        {
            this.trackIdOffset = trackIdOffset;
            this.tracks = tracks;
        }

    }
}
