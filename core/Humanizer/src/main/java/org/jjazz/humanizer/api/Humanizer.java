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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.utilities.api.FloatRange;

/**
 * Humanize notes from a Phrase.
 * <p>
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
            Preconditions.checkArgument(timingRandomness >= 0 && timingRandomness <= 1, "timingRandomness=%f", timingRandomness);
            Preconditions.checkArgument(timingBias >= -0.5f && timingBias <= 0.5f, "timingBias=%f", timingBias);
            Preconditions.checkArgument(velocityRandomness >= 0 && velocityRandomness <= 1, "velocityRandomness=%f", velocityRandomness);
        }

        /**
         * Get a new copy with some values possibly modified.
         *
         * @param timingRandomness   If -1 reuse current value
         * @param timingBias         If -1 reuse current value
         * @param velocityRandomness If -1 reuse current value
         * @return
         */
        public Config getCopy(float timingRandomness, float timingBias, float velocityRandomness)
        {
            return new Config(timingRandomness == -1 ? this.timingRandomness : timingRandomness,
                    timingBias == -1 ? this.timingBias : timingBias,
                    velocityRandomness == -1 ? this.velocityRandomness : velocityRandomness
            );
        }
    }

    /**
     * Random factors of a note.
     *
     * @param timingFactor   [-1;1]
     * @param velocityFactor [-1;1]
     */
    public record NoteFactors(float timingFactor, float velocityFactor)
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


    public static final Config DEFAULT_CONFIG = new Config();
    private static final float MAX_TIMING_DEVIATION = 0.2f;          // +/- 0.2 beat
    private static final float MAX_TIMING_BIAS_DEVIATION = 0.25f;          // +/- 0.25 beat
    private static final int MAX_VELOCITY_DEVIATION = 40;         // +/- 40    
    private final double maxTimingDeviation;
    private final Phrase phrase;
    private final FloatRange allowedBeatRange;
    private final InstrumentFamily insFamily;
    private final int tempo;
    private final HashSet<NoteEvent> notes;
    private Config userConfig;
    private Map<NoteEvent, NoteEvent> mapNoteOrig = new HashMap<>(); //  note => original note
    private Map<NoteEvent, NoteFactors> mapNoteRandomFactors = new HashMap<>(); //  note => random factors 
    private Set<ChangeListener> listeners = new HashSet<>();
    private static final Logger LOGGER = Logger.getLogger(Humanizer.class.getSimpleName());

    /**
     * Create an instance with an allowed beat range of [0;Float.MAX_VALUE].
     *
     * @param phrase    The phrase containing the notes
     * @param notes     The notes to be humanized
     * @param insFamily Can be null
     * @param tempo
     */
    public Humanizer(Phrase phrase, List<NoteEvent> notes, InstrumentFamily insFamily, int tempo)
    {
        this(phrase, new FloatRange(0, Float.MAX_VALUE), notes, insFamily, tempo);
    }

    /**
     * Create an instance to humanize the specified SizedPhrase.
     *
     * @param sizedPhrase The phrase containing the notes
     * @param notes       The notes to be humanized
     * @param insFamily   Can be null
     * @param tempo
     */
    public Humanizer(SizedPhrase sizedPhrase, List<NoteEvent> notes, InstrumentFamily insFamily, int tempo)
    {
        this(sizedPhrase, sizedPhrase.getBeatRange(), notes, insFamily, tempo);
    }

    /**
     * Create an instance to humanize the specified phrase.
     *
     * @param phrase           The phrase containing the notes
     * @param allowedBeatRange The allowed boundaries for the notes
     * @param notes            The notes to be humanized
     * @param insFamily        Can be null.
     * @param tempo
     */
    public Humanizer(Phrase phrase, FloatRange allowedBeatRange, Collection<NoteEvent> notes, InstrumentFamily insFamily, int tempo)
    {
        Objects.nonNull(phrase);
        Objects.nonNull(allowedBeatRange);
        Objects.nonNull(notes);
        Preconditions.checkArgument(tempo >= 10 && tempo <= 400, "tempo=%f", tempo);
        Preconditions.checkArgument(phrase.isEmpty() || allowedBeatRange.contains(phrase.getBeatRange(), false), "phrase=%s allowedBeatRange=%s", phrase,
                allowedBeatRange);


        LOGGER.log(Level.SEVERE, "Humanizer() -- phrase.size()={0}", phrase.size());
        this.phrase = phrase;
        this.allowedBeatRange = allowedBeatRange;
        this.notes = new HashSet<>(notes);
        this.insFamily = insFamily;
        this.tempo = tempo;
        userConfig = new Config();


        // Adjust max timing deviation for slow or fast tempo
        double tempoDeviationImpact = Math.max(-0.1d, -0.1d + (this.tempo - 50) * 0.01);      // -0.1 for tempo<=50, +0.1 for tempo=250
        this.maxTimingDeviation = MAX_TIMING_DEVIATION + tempoDeviationImpact;


        // Save the original notes
        for (var ne : this.notes)
        {
            mapNoteOrig.put(ne, ne.clone());
        }

        // Compute the random factors for each note
        computeRandomFactors(notes);

    }

    /**
     * Add phrase notes to be humanized.
     *
     * @param nes The notes must be part of the phrase.
     */
    public void addNotes(List<NoteEvent> nes)
    {
        notes.addAll(nes);
        for (var ne : nes)
        {
            checkPhraseNote(ne);
            mapNoteOrig.put(ne, ne.clone());
        }
        computeRandomFactors(nes);
        humanize(userConfig);
    }

    /**
     * Remove phrase notes from the ones to be humanized.
     * <p>
     *
     * @param nes
     */
    public void removeNotes(List<NoteEvent> nes)
    {
        notes.removeAll(nes);
        for (var ne : nes)
        {
            mapNoteOrig.remove(ne);
            mapNoteRandomFactors.remove(ne);
        }
    }

    /**
     * Recompute random factors for all notes, then re-humanize notes with the current user config.
     */
    public void newSeed()
    {
        mapNoteRandomFactors.clear();
        computeRandomFactors(notes);
        humanize(userConfig);
    }


    /**
     * The phrase whose some notes are being humanized.
     *
     * @return
     */
    public Phrase getPhrase()
    {
        return phrase;
    }

    /**
     * Get the last Config used by the humanize method.
     *
     * @return
     * @see #humanize(org.jjazz.humanizer.api.Humanizer.Config)
     */
    public Config getUserConfig()
    {
        return userConfig;
    }

    /**
     * Humanize some notes of the source phrase.
     * <p>
     * For each note, the method multiplies the note random factors (calculated upon object creation, or when newSeed() is called) by the corresponding value
     * from newUserConfig.
     *
     * @param newUserConfig
     * @see #cancel()
     * @see #closeAsConfirmed()
     */
    public void humanize(Config newUserConfig)
    {
        Objects.nonNull(newUserConfig);

        LOGGER.log(Level.SEVERE, "humanize() -- newUserConfig={0}", newUserConfig);

        var oldUserConfig = userConfig;
        userConfig = newUserConfig;


        phrase.processNotes(ne -> notes.contains(ne), ne -> 
        {
            var neOrig = mapNoteOrig.get(ne);
            assert neOrig != null : "ne=" + ne;
            float posInBeats = neOrig.getPositionInBeats();
            float duration = neOrig.getDurationInBeats();
            float newDuration = duration;
            int velocity = neOrig.getVelocity();
            var noteRandomFactors = mapNoteRandomFactors.get(ne);


            // New position
            float posShift = (float) (noteRandomFactors.timingFactor() * maxTimingDeviation * userConfig.timingRandomness()
                    + MAX_TIMING_BIAS_DEVIATION * userConfig.timingBias());
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
            int velShift = Math.round(noteRandomFactors.velocityFactor() * MAX_VELOCITY_DEVIATION * userConfig.velocityRandomness);
            int newVelocity = MidiUtilities.limit(velocity + velShift);


            var newNe = new NoteEvent(ne, -1, newDuration, newVelocity, newPosInBeats);


            // newNe will replace ne in the phrase, update our internal data
            notes.remove(ne);
            notes.add(newNe);
            mapNoteOrig.remove(ne);
            mapNoteOrig.put(newNe, neOrig);
            mapNoteRandomFactors.remove(ne);
            mapNoteRandomFactors.put(newNe, noteRandomFactors);


            LOGGER.log(Level.SEVERE, "humanize() processing ne={0} => newNe={1}", new Object[]
            {
                ne, newNe
            });
            return newNe;
        });

        if (!userConfig.equals(oldUserConfig))
        {
            fireChanged();
        }
    }


    /**
     * Get notified when the Config has changed.
     *
     * @param listener
     */
    public void addChangeListener(ChangeListener listener)
    {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener)
    {
        listeners.remove(listener);
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================    

    /**
     * Get the next gaussian random value between -1 and 1 (standard deviation is 0.3)
     *
     * @param random
     * @return
     */
    private float getNextGaussianRandomValue(Random random)
    {
        double res = random.nextGaussian(0, 0.3);
        res = Math.max(res, -1);
        res = Math.min(res, 1);
        return (float) res;
    }


    /**
     * Compute the base random values for each note.
     *
     * @param nes
     * @throws IllegalStateException if a note is not part of the phrase
     */
    private void computeRandomFactors(Collection<NoteEvent> nes) throws IllegalStateException
    {
        Random randTiming = new Random();
        Random randVelocity = new Random();
        for (var ne : nes)
        {
            checkPhraseNote(ne);
            NoteFactors nf = new NoteFactors(getNextGaussianRandomValue(randTiming), getNextGaussianRandomValue(randVelocity));
            mapNoteRandomFactors.put(ne, nf);
        }
    }

    private void checkPhraseNote(NoteEvent ne) throws IllegalStateException
    {
        if (!phrase.contains(ne))
        {
            throw new IllegalStateException("phrase does not contain ne=" + ne);
        }
    }

    private void fireChanged()
    {
        ChangeEvent e = new ChangeEvent(this);
        listeners.forEach(l -> l.stateChanged(e));
    }

}
