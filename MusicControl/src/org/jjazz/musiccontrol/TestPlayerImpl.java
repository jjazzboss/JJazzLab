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
package org.jjazz.musiccontrol;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.EndOfPlaybackActionProvider;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.testplayerservice.spi.TestPlayer;
import org.jjazz.util.api.IntRange;
import org.openide.util.lookup.ServiceProvider;

/**
 * Implementation of the TestPlayer service.
 */
@ServiceProvider(service = TestPlayer.class)
public class TestPlayerImpl implements TestPlayer
{

    private static final Logger LOGGER = Logger.getLogger(TestPlayerImpl.class.getSimpleName());


    /**
     * Send a short sequence of Midi notes on specified channel.
     * <p>
     * If fixPitch &lt; 0 then fixPitch is ignored: play a series of notes starting at 60+transpose. If fixPitch&gt;=0 then play a
     * series of notes with same pitch=fixPitch.
     *
     * @param channel
     * @param fixPitch  -1 means not used.
     * @param transpose Transposition value in semi-tons to be added to test notes. Ignored if fixPitch&gt;=0.
     * @param endAction Called when playback is over. Can be null.
     * @throws org.jjazz.rhythm.api.MusicGenerationException If a problem occurred.
     */
    @Override
    public void playTestNotes(int channel, int fixPitch, int transpose, final Runnable endAction) throws MusicGenerationException
    {
        Phrase p = new Phrase(channel);
        float beat = 0;
        for (int i = 60; i <= 72; i += 3)
        {
            int pitch = fixPitch >= 0 ? fixPitch : i + transpose;
            p.add(new NoteEvent(pitch, 0.5f, 64, beat));
            beat += 0.5f;
        }
        playTestNotes(p, endAction);
    }

    /**
     * Play the test notes from specified phrase at tempo 120.
     * <p>
     *
     * @param phrase
     * @param endAction Called when sequence is over. Can be null.
     * @throws org.jjazz.rhythm.api.MusicGenerationException If a problem occurred.
     */
    @Override
    public void playTestNotes(Phrase phrase, final Runnable endAction) throws MusicGenerationException
    {
        if (phrase == null)
        {
            throw new NullPointerException("p=" + phrase + " endAction=" + endAction);   
        }

        final MusicController mc = MusicController.getInstance();        
        mc.stop();

        TestSession session = new TestSession(phrase, endAction);
        session.generate(false);
        mc.setPlaybackSession(session);
        mc.play(0);

    }

    // ===============================================================================================
    // Private methods
    // ===============================================================================================
    // ===============================================================================================
    // Private class
    // ===============================================================================================
    private class TestSession implements PlaybackSession, EndOfPlaybackActionProvider
    {

        ActionListener endAction;
        Phrase phrase;
        Sequence sequence;

        protected TestSession(Phrase p, Runnable r)
        {
            this.endAction = evt ->
            {
                if (r != null)
                {
                    r.run();
                }
            };
            this.phrase = p;
        }

        @Override
        public void generate(boolean silent) throws MusicGenerationException
        {
            try
            {
                sequence = new Sequence(Sequence.PPQ, MidiConst.PPQ_RESOLUTION);
            } catch (InvalidMidiDataException ex)
            {
                throw new MusicGenerationException(ex.getMessage());
            }
            Track track = sequence.createTrack();
            Phrases.fillTrack(phrase, track);
        }

        @Override
        public Sequence getSequence()
        {
            return sequence;
        }

        @Override
        public PlaybackSession.State getState()
        {
            return PlaybackSession.State.GENERATED;
        }

        @Override
        public int getTempo()
        {
            return 120;
        }

        @Override
        public HashMap<Integer, Boolean> getTracksMuteStatus()
        {
            return null;
        }

        @Override
        public long getLoopEndTick()
        {
            return -1;      // End of sequence
        }

        @Override
        public long getLoopStartTick()
        {
            return 0;
        }

        @Override
        public int getLoopCount()
        {
            return 0;       // No loop
        }

        @Override
        public IntRange getBarRange()
        {
            return null;
        }

        @Override
        public long getTick(int barIndex)
        {
            return 0;
        }

        @Override
        public void close()
        {
            // Nothing
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l)
        {
            // Nothing
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l)
        {
            // Nothing
        }

        @Override
        public ActionListener getEndOfPlaybackAction()
        {
            return endAction;
        }

        @Override
        public boolean isDirty()
        {
            return false;
        }

        @Override
        public PlaybackSession getFreshCopy()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

}
