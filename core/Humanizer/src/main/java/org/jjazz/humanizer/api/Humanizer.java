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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.utilities.api.FloatRange;

/**
 * Humanize notes from a Phrase.
 * <p>
 * TODO: use all constructor parameters for a smarter humanization algorithm, enable multiple time signatures
 */
public class Humanizer implements PropertyChangeListener
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
    }

    /**
     * oldValue=old config, newValue=new config
     */
    public static final String PROP_CONFIG = "PropConfig";
    public static final String PROP_ENABLED = "PropEnabled";
    /**
     * A config which does not change any notes.
     */
    public static final Config DEFAULT_CONFIG = new Config();
    private static final float MAX_TIMING_DEVIATION = 0.25f;          // +/- 0.2 beat
    private static final float MAX_TIMING_BIAS_DEVIATION = 0.25f;          // +/- 0.25 beat
    private static final int MAX_VELOCITY_DEVIATION = 20;         // +/- 20    
    private final double maxTimingDeviation;
    private final Phrase sourcePhrase;
    private final TimeSignature timeSignature;
    private final FloatRange allowedBeatRange;
    private final InstrumentFamily insFamily;
    private boolean enabled;
    private final int tempo;
    private final HashSet<NoteEvent> notes;
    private Config userConfig;
    private Map<NoteEvent, NoteEvent> mapNoteOrig = new HashMap<>(); //  note => original note
    private Map<NoteEvent, NoteFactors> mapNoteRandomFactors = new HashMap<>(); //  note => random factors 
    private final transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(Humanizer.class.getSimpleName());

    /**
     * Create an instance with an allowed beat range of [0;Float.MAX_VALUE], with DEFAULT_CONFIG, and no registered notes.
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
     * Create an instance with DEFAULT_CONFIG, and no registered notes.
     *
     * @param sizedPhrase
     * @param insFamily   Can be null
     * @param tempo
     */
    public Humanizer(SizedPhrase sizedPhrase, InstrumentFamily insFamily, int tempo)
    {
        this(sizedPhrase, sizedPhrase.getTimeSignature(), sizedPhrase.getBeatRange(), insFamily, tempo);
    }

    /**
     * Create an instance with DEFAULT_CONFIG, and no registered notes.
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
        Preconditions.checkArgument(tempo >= 10 && tempo <= 400, "tempo=%f", tempo);
        Preconditions.checkArgument(sourcePhrase.isEmpty() || allowedBeatRange.contains(sourcePhrase.getBeatRange(), false), "phrase=%s allowedBeatRange=%s",
            sourcePhrase,
            allowedBeatRange);


        LOGGER.log(Level.SEVERE, "Humanizer() -- phrase.size()={0}", sourcePhrase.size());
        this.sourcePhrase = sourcePhrase;
        this.timeSignature = ts;
        this.allowedBeatRange = allowedBeatRange;
        this.notes = new HashSet<>();
        this.insFamily = insFamily;
        this.tempo = tempo;
        userConfig = DEFAULT_CONFIG;
        enabled = true;


        this.sourcePhrase.addPropertyChangeListener(this);


        // Adjust max timing deviation for slow or fast tempo
        double tempoDeviationImpact = Math.max(-0.1d, -0.1d + (this.tempo - 50) * 0.001);      // -0.1 for tempo<=50, +0.1 for tempo=250
        this.maxTimingDeviation = MAX_TIMING_DEVIATION + tempoDeviationImpact;

    }

    public void cleanup()
    {
        sourcePhrase.removePropertyChangeListener(this);
    }

    /**
     * Enable or disable the instance.
     * <p>
     * When disabled, humanize() does nothing. When re-enabling the instance, all notes are humanized() again.
     * <p>
     * Fire a PROP_ENABLED change event.
     *
     * @param b
     */
    public void setEnabled(boolean b)
    {
        if (b == enabled)
        {
            return;
        }
        enabled = b;
        LOGGER.severe("setEnabled() -- b=" + b);
        if (enabled)
        {
            humanize(null);
        }
        pcs.firePropertyChange(PROP_ENABLED, !enabled, enabled);

    }

    /**
     * Check if this Humanizer is enabled.
     *
     * @return
     */
    public boolean isEnabled()
    {
        return enabled;
    }


    /**
     * Register phrase notes to be humanized by the humanize() method.
     * <p>
     *
     * @param nes The notes must be part of the source phrase.
     * @see #humanize(org.jjazz.humanizer.api.Humanizer.Config)
     */
    public void registerNotes(List<NoteEvent> nes)
    {
        notes.addAll(nes);
        for (var ne : nes)
        {
            checkPhraseNote(ne);
            mapNoteOrig.put(ne, ne.clone());
        }
        computeRandomFactors(nes);
    }

    /**
     * Unregister phrase notes : they won't be humanized anymore by humanize().
     * <p>
     *
     * @param nes
     */
    public void unregisterNotes(Collection<NoteEvent> nes)
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
    public Phrase getSourcePhrase()
    {
        return sourcePhrase;
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
     * Humanize the source phrase registered notes.
     * <p>
     * For each note the method multiplies the note random factors (calculated upon object creation, or when newSeed() is called) by the corresponding
     * value from newUserConfig.
     * <p>
     * Notes whose client property NoteEvent.PROP_IS_ADJUSTIING is true are not humanized. If instance is disabled, all notes are left unchanged.
     * <p>
     * Fire a PROP_CONFIG change event if userConfig has changed.
     *
     * @param newUserConfig If null, reuse the same config
     * @see #registerNotes(java.util.List)
     * @see #isEnabled()
     * @see NoteEvent#PROP_IS_ADJUSTING
     */
    public void humanize(Config newUserConfig)
    {

        LOGGER.log(Level.SEVERE, "humanize() -- newUserConfig={0} enabled={1}", new Object[]
        {
            newUserConfig, enabled
        });

        assert userConfig != null;
        var oldUserConfig = userConfig;
        userConfig = newUserConfig == null ? oldUserConfig : newUserConfig;


        if (enabled)
        {
            sourcePhrase.processNotes(ne -> notes.contains(ne), ne ->
            {
                if (Boolean.TRUE.equals(ne.getClientProperties().get(NoteEvent.PROP_IS_ADJUSTING)))
                {
                    LOGGER.log(Level.SEVERE, "humanize() isAdjusting note, skip ne={0}", ne);
                    return ne;
                }

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


                var newNe = ne.setAll(-1, newDuration, newVelocity, newPosInBeats, true);
                LOGGER.log(Level.SEVERE, "humanize() processing ne={0} => newNe={1}", new Object[]
                {
                    ne, newNe
                });
                return newNe;
            });
        }


        pcs.firePropertyChange(PROP_CONFIG, oldUserConfig, userConfig);

    }


    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================    

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.log(Level.FINE, "propertyChange() -- evt={0}", evt);

        if (evt.getSource() == sourcePhrase)
        {
            switch (evt.getPropertyName())
            {
                case Phrase.PROP_NOTES_MOVED ->
                {
                    var mapOldNew = (Map<NoteEvent, NoteEvent>) evt.getNewValue();
                    for (var neOld : mapOldNew.keySet())
                    {
                        noteReplaced(neOld, mapOldNew.get(neOld), true);        // Forget the original note since user directly moved it
                    }
                }
                case Phrase.PROP_NOTES_REMOVED ->
                {
                    var removedNotes = (Collection<NoteEvent>) evt.getNewValue();
                    unregisterNotes(removedNotes);
                }
                case Phrase.PROP_NOTES_REPLACED ->
                {
                    var mapOldNew = (Map<NoteEvent, NoteEvent>) evt.getNewValue();
                    for (var neOld : mapOldNew.keySet())
                    {
                        noteReplaced(neOld, mapOldNew.get(neOld), false);
                    }
                }
                case Phrase.PROP_NOTES_ADDED ->
                {
                    // Nothing
                }
                default ->
                {
                }
            }
        }
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================    

    /**
     * Called when a Phrase note was replaced by a new one.
     *
     * @param neOld
     * @param neNew
     * @param resetOriginalNote If true do not preserve the original note
     */
    private void noteReplaced(NoteEvent neOld, NoteEvent neNew, boolean resetOriginalNote)
    {
        if (notes.contains(neOld))
        {
            notes.remove(neOld);
            notes.add(neNew);
            var neOrig = resetOriginalNote ? neNew : mapNoteOrig.remove(neOld);
            mapNoteOrig.put(neNew, neOrig);
            var factors = mapNoteRandomFactors.remove(neOld);
            mapNoteRandomFactors.put(neNew, factors);
        }
    }


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
        if (!sourcePhrase.contains(ne))
        {
            throw new IllegalStateException("phrase does not contain ne=" + ne);
        }
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
