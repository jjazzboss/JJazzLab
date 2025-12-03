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
package org.jjazz.humanizer.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.Utilities;

/**
 * Humanize notes from a Phrase.
 * <p>
 * TODO: use all constructor parameters for a smarter humanization algorithm, enable multiple time signatures
 */
public class Humanizer
{

    /**
     * A humanization user configuration.
     *
     * @param timingRandomness   [0;1.0] Amount of randomness to notes start time, and possibly their duration
     * @param timingBias         [-0.5;0.5] Shift to notes start time
     * @param velocityRandomness [0-1.0] Amount of randomness to notes velocity
     */
    public record Config(float timingRandomness, float timingBias, float velocityRandomness)
            {

        public Config()
        {
            this(0, 0, 0);
        }

        public Config  
        {
            Preconditions.checkArgument(timingRandomness >= 0 && timingRandomness <= 1, "timingRandomness=%s", timingRandomness);
            Preconditions.checkArgument(timingBias >= -0.5f && timingBias <= 0.5f, "timingBias=%s", timingBias);
            Preconditions.checkArgument(velocityRandomness >= 0 && velocityRandomness <= 1, "velocityRandomness=%s", velocityRandomness);
        }

        public Config setTimingRandomness(float newValue)
        {
            return new Config(newValue, this.timingBias(), this.velocityRandomness());
        }

        public Config setTimingBias(float newValue)
        {
            return new Config(this.timingRandomness(), newValue, this.velocityRandomness());
        }

        public Config setVelocityRandomness(float newValue)
        {
            return new Config(this.timingRandomness(), this.timingBias(), newValue);
        }

        public String toSaveString()
        {
            return Float.toString(timingRandomness()) + " " + Float.toString(timingBias()) + " " + Float.toString(velocityRandomness());
        }

        static public Config loadFromString(String str)
        {
            Objects.requireNonNull(str);
            Config res;
            Scanner s = new Scanner(str).useLocale(Locale.ENGLISH);  // Because Float.toString() always uses english locale ("." in floating point)
            try
            {
                float tr = s.nextFloat();
                float tb = s.nextFloat();
                float vr = s.nextFloat();
                res = new Config(tr, tb, vr);
            } catch (Exception ex)
            {
                LOGGER.log(Level.WARNING, "loadFromString() Invalid string value ={0}. ex={1}. Using default value instead", new Object[]
                {
                    str, ex.getMessage()
                });
                res = new Config();
            }
            s.close();
            return res;
        }
    }

    /**
     * oldValue=old config, newValue=new config.
     */
    public static final String PROP_USER_CONFIG = "PropUserConfig";
    /**
     * oldValue=old state, newValue=new state
     */
    public static final String PROP_STATE = "PropState";

    /**
     * The default config used by default.
     */
    public static final Config DEFAULT_CONFIG = new Config(0.2f, 0f, 0.2f);
    /**
     * A configuration which does not change notes.
     */
    public static final Config ZERO_CONFIG = new Config(0f, 0f, 0f);

    public enum State
    {
        /**
         * Default state upon Humanizer creation or after reset() is called.
         */
        INIT,
        /**
         * humanize() was called at least once.
         */
        HUMANIZING
    };
    private static final float MAX_TIMING_DEVIATION = 0.2f;          // +/- 0.2 beat
    private static final float MAX_TIMING_BIAS_DEVIATION = 0.2f;          // +/- 0.2 beat
    private static final int MAX_VELOCITY_DEVIATION = 30;         // +/- 30    

    private State state;
    private final double maxTimingDeviation;
    private final Phrase sourcePhrase;
    private final TimeSignature timeSignature;
    private final FloatRange allowedBeatRange;
    private final InstrumentFamily insFamily;
    private final int tempo;
    private final HashSet<NoteEvent> registeredNotes;
    private Config config;
    private Map<NoteEvent, NoteEvent> mapNoteOrig = new HashMap<>(); //  note => original note
    private Map<NoteEvent, NoteFactors> mapNoteRandomFactors = new HashMap<>(); //  note => random factors 
    private final transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(Humanizer.class.getSimpleName());

    /**
     * Create an instance in INIT state with an allowed beat range of [0;Float.MAX_VALUE], with DEFAULT_CONFIG, and no registered notes.
     *
     * @param phrase
     * @param ts
     * @param insFamily Can be null
     * @param tempo
     */
    public Humanizer(Phrase phrase, TimeSignature ts, InstrumentFamily insFamily, int tempo)
    {
        this(phrase, ts, new FloatRange(0, Float.MAX_VALUE), insFamily, tempo);
    }

    /**
     * Create an instance in INIT state with DEFAULT_CONFIG, and no registered notes.
     *
     * @param sizedPhrase
     * @param insFamily   Can be null
     * @param tempo
     */
    public Humanizer(SizedPhrase sizedPhrase, InstrumentFamily insFamily, int tempo)
    {
        this(sizedPhrase, sizedPhrase.getTimeSignature(), sizedPhrase.getNotesBeatRange(), insFamily, tempo);
    }

    /**
     * Create an instance in INIT state with DEFAULT_CONFIG and no registered notes.
     *
     * @param sourcePhrase
     * @param ts
     * @param allowedBeatRange The allowed boundaries for the notes
     * @param insFamily        Can be null.
     * @param tempo
     */
    public Humanizer(Phrase sourcePhrase, TimeSignature ts, FloatRange allowedBeatRange, InstrumentFamily insFamily, int tempo)
    {
        Objects.nonNull(sourcePhrase);
        Objects.nonNull(allowedBeatRange);
        Objects.nonNull(ts);
        Preconditions.checkArgument(tempo >= 10 && tempo <= 400, "tempo=%s", tempo);
        Preconditions.checkArgument(sourcePhrase.isEmpty() || allowedBeatRange.contains(sourcePhrase.getNotesBeatRange(), false), "phrase=%s allowedBeatRange=%s",
                sourcePhrase,
                allowedBeatRange);


        LOGGER.log(Level.FINE, "Humanizer() -- phrase.size()={0}", sourcePhrase.size());
        this.sourcePhrase = sourcePhrase;
        this.timeSignature = ts;
        this.allowedBeatRange = allowedBeatRange;
        this.registeredNotes = new HashSet<>();
        this.insFamily = insFamily;
        this.tempo = tempo;
        config = DEFAULT_CONFIG;
        state = State.INIT;


        // Adjust max timing deviation for slow or fast tempo
        double tempoDeviationImpact = Math.max(-0.1d, -0.1d + (this.tempo - 50) * 0.001);      // -0.1 for tempo<=50, +0.1 for tempo=250
        this.maxTimingDeviation = MAX_TIMING_DEVIATION + tempoDeviationImpact;

    }

    public State getState()
    {
        return state;
    }

    /**
     * Reset this instance.
     * <p>
     * Clear the registered notes and set the INIT state.
     * <p>
     */
    public void reset()
    {
        registeredNotes.clear();
        mapNoteOrig.clear();
        mapNoteRandomFactors.clear();
        changeState(State.INIT);
    }


    /**
     * Add phrase notes to be humanized by the humanize() method.
     * <p>
     * @param nes The notes must be part of the source phrase. Note is ignored if already registered or if it's an adjusting note.
     * @see #humanize() 
     * @see NoteEvent#isAdjustingNote(org.jjazz.phrase.api.NoteEvent)
     */
    public void registerNotes(Collection<NoteEvent> nes)
    {
        LOGGER.log(Level.FINE, "registerNotes() -- nes={0}", nes);
        var newNotes = new ArrayList<NoteEvent>();
        for (var ne : nes)
        {
            if (!NoteEvent.isAdjustingNote(ne) && !registeredNotes.contains(ne))
            {
                registeredNotes.add(ne);
                mapNoteOrig.put(ne, ne.clone());
                newNotes.add(ne);
            }
        }
        computeRandomFactors(newNotes);
    }

    /**
     *
     * @return An unmodifiable set
     */
    public Set<NoteEvent> getRegisteredNotes()
    {
        return Collections.unmodifiableSet(registeredNotes);
    }

    /**
     * Recompute random factors for all notes, with effect on the next call to humanize().
     */
    public void newSeed()
    {
        LOGGER.log(Level.FINE, "newSeed() -- notes={0}", registeredNotes);
        mapNoteRandomFactors.clear();
        computeRandomFactors(registeredNotes);
    }


    /**
     * The phrase whose notes are being humanized.
     *
     * @return
     */
    public Phrase getSourcePhrase()
    {
        return sourcePhrase;
    }

    /**
     * Change the config.
     * <p>
     * May fire a PROP_USER_CONFIG change event.
     *
     * @param newConfig
     * @see #humanize()
     */
    public void setConfig(Config newConfig)
    {
        Objects.requireNonNull(newConfig);
        var old = config;
        config = newConfig;
        pcs.firePropertyChange(PROP_USER_CONFIG, old, config);
    }

    /**
     * Get the Config used by the humanize method.
     *
     * @return
     * @see #humanize()
     * @see #setConfig(org.jjazz.humanizer.api.Humanizer.Config)
     */
    public Config getConfig()
    {
        return config;
    }

    /**
     * Humanize the registered notes with the current config.
     * <p>
     * For each note the method multiplies the note random factors (calculated upon object creation, or when newSeed() is called) by the corresponding value
     * from the current Config. Then overlapping same-pitch notes are fixed.
     * <p>
     * If state was INIT it is changed to HUMANIZED.
     *
     * @see #registerNotes(java.util.Collection) 
     * @see #getConfig()
     */
    public void humanize()
    {
        LOGGER.log(Level.FINE, "humanize() -- config={0} ", config);

        sourcePhrase.processNotes(ne -> registeredNotes.contains(ne), ne -> 
        {
            var neOrig = mapNoteOrig.get(ne);
            assert neOrig != null : "ne=" + ne;
            float posInBeats = neOrig.getPositionInBeats();
            float duration = neOrig.getDurationInBeats();
            float newDuration = duration;
            int velocity = neOrig.getVelocity();
            var noteRandomFactors = mapNoteRandomFactors.get(ne);


            // New position
            float posShift = (float) (noteRandomFactors.timingFactor() * maxTimingDeviation * config.timingRandomness()
                    + MAX_TIMING_BIAS_DEVIATION * config.timingBias());
            float newPosInBeats = posInBeats + posShift;

            // Check that we remain in the allowed range
            newPosInBeats = Math.max(newPosInBeats, allowedBeatRange.from);
            newPosInBeats = Math.min(newPosInBeats, allowedBeatRange.to - 0.1f);
            if (newPosInBeats + duration > allowedBeatRange.to)
            {
                // New duration to remain in the range
                newDuration = allowedBeatRange.to - 0.05f - newPosInBeats;
            }


            // New velocity
            int velShift = Math.round(noteRandomFactors.velocityFactor() * MAX_VELOCITY_DEVIATION * config.velocityRandomness);
            int newVelocity = MidiConst.clamp(velocity + velShift);


            if (Float.compare(newDuration, ne.getDurationInBeats()) == 0
                    && newVelocity == ne.getVelocity()
                    && Float.compare(newPosInBeats, ne.getPositionInBeats()) == 0)
            {
                // Nothing changed
                return ne;
            }


            // Get the new replacing note and update state
            var newNe = ne.setAll(-1, newDuration, newVelocity, newPosInBeats, null, true);
            registeredNotes.remove(ne);
            registeredNotes.add(newNe);
            mapNoteOrig.remove(ne);
            mapNoteOrig.put(newNe, neOrig);
            var factors = mapNoteRandomFactors.remove(ne);
            mapNoteRandomFactors.put(newNe, factors);


            LOGGER.log(Level.FINE, "humanize() processed ne={0} => newNe={1}", new Object[]
            {
                ne, newNe
            });
            return newNe;
        });


        var map = Phrases.fixOverlappedNotes(sourcePhrase);
        // fixOverlappedNotes() possibly changed sourcePhrase, need to update state
        for (var ne : map.keySet())
        {
            var newNe = map.get(ne);
            if (newNe != null)
            {
                // Note was replaced
                var neOrig = mapNoteOrig.remove(ne);
                var factors = mapNoteRandomFactors.remove(ne);
                registeredNotes.remove(ne);

                mapNoteOrig.put(newNe, neOrig);
                mapNoteRandomFactors.put(newNe, factors);
                registeredNotes.add(newNe);
            } else
            {
                // Note was removed
                mapNoteOrig.remove(ne);
                mapNoteRandomFactors.remove(ne);
                registeredNotes.remove(ne);
            }
        }

        changeState(State.HUMANIZING);
    }


    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================    
  
    /**
     * Compute the base random values for each note.
     *
     * @param nes
     * @throws IllegalStateException If a note is not part of the phrase
     */
    private void computeRandomFactors(Collection<NoteEvent> nes) throws IllegalStateException
    {
        Random randTiming = new Random();
        Random randVelocity = new Random();
        for (var ne : nes)
        {
            if (!sourcePhrase.contains(ne))
            {
                throw new IllegalStateException("Source phrase does not contain ne=" + ne + ", sourcePhrase=" + sourcePhrase);
            }
            NoteFactors nf = new NoteFactors(Utilities.getNextGaussianRandomValue(randTiming), Utilities.getNextGaussianRandomValue(randVelocity));
            mapNoteRandomFactors.put(ne, nf);
        }
    }

    private void changeState(State newState)
    {
        var old = state;
        state = newState;
        pcs.firePropertyChange(PROP_STATE, old, state);
    }


    // ====================================================================================
    // Inner classes
    // ====================================================================================  
    /**
     * Random factors of a note.
     *
     * @param timingFactor   [-1;1]
     * @param velocityFactor [-1;1]
     */
    private record NoteFactors(float timingFactor, float velocityFactor)
            {

        public NoteFactors()
        {
            this(0, 0);
        }

        public NoteFactors 
        {
            Preconditions.checkArgument(timingFactor >= -1 && timingFactor <= 1);
            Preconditions.checkArgument(velocityFactor >= -1 && velocityFactor <= 1);
        }
    }

}
