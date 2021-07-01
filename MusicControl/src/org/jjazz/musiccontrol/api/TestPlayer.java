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
package org.jjazz.musiccontrol.api;

import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythmmusicgeneration.api.NoteEvent;
import org.jjazz.rhythmmusicgeneration.api.Phrase;
import org.jjazz.util.api.ResUtil;

/**
 * Play test notes.
 */
public class TestPlayer
{

    private static TestPlayer INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(TestPlayer.class.getSimpleName());

    public static TestPlayer getInstance()
    {
        synchronized (TestPlayer.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new TestPlayer();
            }
        }
        return INSTANCE;
    }

    private TestPlayer()
    {

    }

    /**
     * Send a short sequence of Midi notes on specified channel.
     * <p>
     * If fixPitch &lt; 0 then fixPitch is ignored: play a series of notes starting at 60+transpose. If fixPitch&gt;=0 then play a
     * series of notes with same pitch=fixPitch.
     *
     * @param channel
     * @param fixPitch -1 means not used.
     * @param transpose Transposition value in semi-tons to be added to test notes. Ignored if fixPitch&gt;=0.
     * @param endAction Called when sequence is over. Can be null.
     * @throws org.jjazz.rhythm.api.MusicGenerationException If a problem occurred. endAction.run() is called before throwing the
     * exception.
     */
    public void playTestNotes(int channel, int fixPitch, int transpose, final Runnable endAction) throws MusicGenerationException
    {
        Phrase p = new Phrase(channel);
        float beat = 0;
        for (int i = 60; i <= 72; i += 3)
        {
            int pitch = fixPitch >= 0 ? fixPitch : i + transpose;
            p.addOrdered(new NoteEvent(pitch, 0.5f, 64, beat));
            beat += 0.5f;
        }
        playTestNotes(p, endAction);
    }

    /**
     * Play the test notes from specified phrase.
     * <p>
     *
     * @param p
     * @param endAction Called when sequence is over. Can be null.
     * @throws org.jjazz.rhythm.api.MusicGenerationException If a problem occurred. endAction.run() is called before throwing the
     * exception.
     */
    public void playTestNotes(Phrase p, final Runnable endAction) throws MusicGenerationException
    {
        if (p == null)
        {
            throw new NullPointerException("p=" + p + " endAction=" + endAction);   //NOI18N
        }

        final MusicController mc = MusicController.getInstance();
        Sequencer sequencer = mc.acquireSequencer(this);
        if (sequencer == null)
        {
            throw new MusicGenerationException(ResUtil.getString(getClass(), "Err_CantAccessSequencer"));
        }


        try
        {
            checkMidi();
        } catch (MusicGenerationException ex)
        {
            if (endAction != null)
            {
                endAction.run();
            }
            throw ex;
        }


        try
        {
            // build a track to hold the notes to send
            Sequence seq = new Sequence(Sequence.PPQ, MidiConst.PPQ_RESOLUTION);
            Track track = seq.createTrack();
            p.fillTrack(track);

            // create an object to listen to the End of Track MetaEvent and stop the sequencer
            MetaEventListener stopSequencer = new MetaEventListener()
            {
                @Override
                public void meta(MetaMessage event)
                {
                    if (event.getType() == 47) // Meta Event for end of sequence
                    {
                        sequencer.removeMetaEventListener(this);
                        sequencer.stop();
                        mc.releaseSequencer(TestPlayer.this);
                        if (endAction != null)
                        {
                            endAction.run();
                        }
                    }
                }
            };
            sequencer.addMetaEventListener(stopSequencer);

            // play the sequence
            sequencer.setTickPosition(0);
            sequencer.setLoopCount(0);
            sequencer.setSequence(seq);
            sequencer.setTempoFactor(1f);
            sequencer.setTempoInBPM(120);
            sequencer.start();

        } catch (InvalidMidiDataException e)
        {
            mc.releaseSequencer(TestPlayer.this);
            throw new MusicGenerationException(e.getLocalizedMessage());
        }
    }

    // ===============================================================================================
    // Private methods
    // ===============================================================================================
    private void checkMidi() throws MusicGenerationException
    {
        if (JJazzMidiSystem.getInstance().getDefaultOutDevice() == null)
        {
            throw new MusicGenerationException(ResUtil.getString(getClass(), "ERR_NoMidiOutputDeviceSet"));
        }
    }

}
