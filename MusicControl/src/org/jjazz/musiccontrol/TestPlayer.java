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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midi.MidiConst;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythmmusicgeneration.NoteEvent;
import org.jjazz.rhythmmusicgeneration.Phrase;

/**
 * Play test notes.
 */
public class TestPlayer implements PropertyChangeListener
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

        // Listen to sequencer lock changes
        JJazzMidiSystem.getInstance().addPropertyChangeListener(this);
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
            throw new NullPointerException("p=" + p + " endAction=" + endAction);
        }


        final var jms = JJazzMidiSystem.getInstance();
        Sequencer sequencer = jms.getSequencer(TestPlayer.this);
        if (sequencer == null)
        {
            throw new MusicGenerationException("Can't access sequencer");
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
                        jms.releaseSequencer(TestPlayer.this);
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
            jms.releaseSequencer(TestPlayer.this);
            throw new MusicGenerationException(e.getLocalizedMessage());
        }
    }


    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        LOGGER.log(Level.FINE, "propertyChange() e={0}", e);

        var jms = JJazzMidiSystem.getInstance();

        if (e.getSource() == jms)
        {
            if (e.getPropertyName().equals(JJazzMidiSystem.PROP_SEQUENCER_LOCK))
            {
                if (e.getNewValue() == this)
                {
                    // We acquired the sequencer                    

                    // Initialize it, add our listeners
                    sequencerAcquired();

                    if (state.equals(State.DISABLED))
                    {
                        // Sequencer has been used meanwhile                        
                        jms.getSystemSequencer().stop();
                        if (playbackContext != null)
                        {
                            playbackContext.setDirty();
                        }
                        state = State.STOPPED;
                        pcs.firePropertyChange(PROP_PLAYBACK_STATE, State.DISABLED, state);
                    }

                } else if (e.getOldValue() == this)
                {
                    // We released the sequencer
                    sequencerReleased();

                    // Nothing: don't change state

                } else if (e.getNewValue() != null)
                {
                    // Another user acquired the sequencer
                    sequencerReleased();


                    // Change state
                    State old = getState();
                    state = State.DISABLED;
                    pcs.firePropertyChange(PROP_PLAYBACK_STATE, old, state);
                }
            }
        }

    }

    // ===============================================================================================
    // Private methods
    // ===============================================================================================

    private void checkMidi() throws MusicGenerationException
    {
        if (JJazzMidiSystem.getInstance().getDefaultOutDevice() == null)
        {
            throw new MusicGenerationException("No MIDI Out device set. Go to menu Tools/Options/Midi and select a Midi device.");
        }
    }

}
