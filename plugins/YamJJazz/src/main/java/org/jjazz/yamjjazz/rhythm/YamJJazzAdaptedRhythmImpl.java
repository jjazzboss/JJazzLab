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
package org.jjazz.yamjjazz.rhythm;

import com.google.common.base.Preconditions;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythmGenerator;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SourcePhrase;
import org.jjazz.phrase.api.SourcePhraseSet;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Feel;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_Mute;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_OverrideTracks;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.yamjjazz.rhythm.api.AccType;
import org.jjazz.yamjjazz.rhythm.api.CtabChannelSettings;
import org.jjazz.yamjjazz.rhythm.api.Style;
import org.jjazz.yamjjazz.rhythm.api.StylePart;
import org.jjazz.yamjjazz.rhythm.api.StylePartType;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzAdaptedRhythm;

/**
 * A time signature adapter rhythm for our YamJJazz rhythms.
 * <p>
 */
public class YamJJazzAdaptedRhythmImpl implements YamJJazzAdaptedRhythm
{

    private final String rhythmProviderId;
    private final List<RhythmVoiceDelegate> rhythmVoices;
    private final List<RhythmParameter<?>> rhythmParameters;
    private final YamJJazzRhythm sourceRhythm;          // The source rhythm
    private final TimeSignature newTs;                  // The new time signature
    private Style newStyle;                             // The style copy adapted to new time signature
    MusicGenerator generator;
    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(YamJJazzAdaptedRhythmImpl.class.getSimpleName());

    public YamJJazzAdaptedRhythmImpl(String rhythmProviderId, YamJJazzRhythm sourceRhythm, TimeSignature ts)
    {
        Objects.requireNonNull(rhythmProviderId);
        Objects.requireNonNull(sourceRhythm);
        Objects.requireNonNull(ts);
        Preconditions.checkArgument(!sourceRhythm.getTimeSignature().equals(ts), "sourceRhythm=%s ts=%s", sourceRhythm, ts);

        this.rhythmProviderId = rhythmProviderId;
        this.sourceRhythm = sourceRhythm;
        newTs = ts;
        rhythmVoices = buildRhythmVoices();
        rhythmParameters = buildRhythmParameters();
    }

    @Override
    public MusicGenerator getMusicGenerator()
    {
        if (generator == null)
        {
            generator = new YamJJazzRhythmGenerator(this);
        }
        return generator;
    }

    @Override
    public void setMusicGenerator(MusicGenerator mg)
    {
        Objects.requireNonNull(mg);
        generator = mg;
    }

    // ==================================================================================================
    // AdaptedRhythm interface
    // ==================================================================================================
    @Override
    public Rhythm getSourceRhythm()
    {
        return sourceRhythm;
    }

    @Override
    public String getUniqueId()
    {
        return AdaptedRhythm.buildUniqueId(rhythmProviderId, sourceRhythm, newTs);
    }

    // ==================================================================================================
    // Rhythm interface
    // ==================================================================================================
    @Override
    public RhythmFeatures getFeatures()
    {
        return sourceRhythm.getFeatures();
    }

    /**
     * Build the Style copy with the new TimeSignature.
     * <p>
     */
    @Override
    public void loadResources() throws MusicGenerationException
    {
        LOGGER.log(Level.FINE, "loadResources() -- this={0}", this);
        if (isResourcesLoaded())
        {
            return;
        }

        if (!sourceRhythm.isResourcesLoaded())
        {
            sourceRhythm.loadResources();
        }

        Style oldStyle = sourceRhythm.getStyle();
        newStyle = adaptStyle(oldStyle, newTs);

        pcs.firePropertyChange(PROP_RESOURCES_LOADED, false, true);
    }


    @Override
    public void releaseResources()
    {
        LOGGER.fine("releaseResources() --");
        if (!isResourcesLoaded())
        {
            return;
        }
        newStyle = null;
        pcs.firePropertyChange(PROP_RESOURCES_LOADED, true, false);
    }

    @Override
    public boolean isResourcesLoaded()
    {
        return newStyle != null;
    }

    @Override
    public List<RhythmVoice> getRhythmVoices()
    {
        return new ArrayList<>(rhythmVoices);
    }

    @Override
    public List<RhythmParameter<?>> getRhythmParameters()
    {
        return new ArrayList<>(rhythmParameters);
    }

    @Override
    public File getFile()
    {
        return new File("");
    }

    @Override
    public String getDescription()
    {
        return AdaptedRhythm.buildDescription(sourceRhythm, newTs);
    }

    @Override
    public int getPreferredTempo()
    {
        return sourceRhythm.getPreferredTempo();
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return newTs;
    }

    @Override
    public String getName()
    {
        return AdaptedRhythm.buildName(sourceRhythm, newTs);
    }

    @Override
    public String getAuthor()
    {
        return sourceRhythm.getAuthor();
    }

    @Override
    public String getVersion()
    {
        return sourceRhythm.getVersion();
    }

    @Override
    public String[] getTags()
    {
        return sourceRhythm.getTags();
    }

    // ==================================================================================================
    // YamJJazzRhythm interface
    // ==================================================================================================
    @Override
    public AccType getAccType(String rpMuteValue)
    {
        return sourceRhythm.getAccType(rpMuteValue);
    }

    @Override
    public int getComplexityLevel(String rpValue)
    {
        return sourceRhythm.getComplexityLevel(rpValue);
    }

    /**
     * @return Our adapted style !
     */
    @Override
    public Style getStyle()
    {
        return newStyle;
    }

    /**
     * Use our style copy.
     *
     * @param rpValue
     * @return
     */
    @Override
    public StylePart getStylePart(String rpValue)
    {
        if (rpValue == null)
        {
            throw new IllegalArgumentException("rpValue=" + rpValue);   //NOI18N
        }
        StylePart res = null;
        String strs[] = rpValue.trim().split("-");
        if (strs.length == 2)
        {
            StylePartType type = StylePartType.getType(strs[0].trim());
            if (type != null)
            {
                res = newStyle.getStylePart(type);
            }
        }
        return res;
    }

    @Override
    public boolean isExtendedRhythm()
    {
        return sourceRhythm.isExtendedRhythm();
    }

    @Override
    public boolean equals(Object o)
    {
        boolean res = false;
        if (o instanceof YamJJazzRhythm)
        {
            YamJJazzRhythm r = (YamJJazzRhythm) o;
            res = r.getUniqueId().equals(getUniqueId());
        }
        return res;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(getUniqueId());
        return hash;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // =========================================================================================
    // Private methods
    // =========================================================================================
    /**
     * Copy and possibly shift the notes from the old SourcePhrase to the new SourcePhrase.
     *
     * @param nbBars
     * @param oldPhrase The phrase using old TimeSignature
     * @param newPhrase The phrase using new TimeSignature
     */
    private void copyAndAdaptNotes(int nbBars, SourcePhrase oldPhrase, SourcePhrase newPhrase)
    {
        int oldNbBeatsPerBar = (int) sourceRhythm.getTimeSignature().getNbNaturalBeats();
        int newNbBeatsPerBar = (int) newTs.getNbNaturalBeats();
        newPhrase.clear();
        int bar = 0;
        if (newNbBeatsPerBar > oldNbBeatsPerBar)
        {
            // Need to duplicate events to fill the gaps between each new bar
            for (NoteEvent ne : oldPhrase)
            {
                float oldPosInBeats = ne.getPositionInBeats();
                bar = (int) Math.floor(oldPosInBeats / oldNbBeatsPerBar);
                float relPosInBeats = oldPosInBeats - (bar * oldNbBeatsPerBar);     // Position within the bar
                float newPosInBeats = bar * newNbBeatsPerBar + relPosInBeats;
                float newDur = ne.getDurationInBeats();
                boolean durExtended = false;
                if (newDur + relPosInBeats >= oldNbBeatsPerBar) // Note lasts beyond its starting bar
                {
                    newDur *= ((float) newNbBeatsPerBar) / oldNbBeatsPerBar;  // Make it longer proportionally
                    durExtended = true;
                }
                NoteEvent newNe = ne.setAll(-1, newDur, -1, newPosInBeats, null, true);
                newPhrase.add(newNe);

                if (!durExtended && relPosInBeats < (newNbBeatsPerBar - oldNbBeatsPerBar) - 0.1f)
                {
                    // Duplicate the first short notes of the bar at the end of the bar to fill the gap (unless note's duration has been extended)
                    float copyRelPosInBeats = relPosInBeats + oldNbBeatsPerBar;
                    float copyNePos = bar * newNbBeatsPerBar + copyRelPosInBeats;
                    float copyNeDur = copyRelPosInBeats + newDur >= newNbBeatsPerBar ? newNbBeatsPerBar - copyRelPosInBeats - 0.1f : newDur;
                    NoteEvent copyNe = ne.setAll(-1, copyNeDur, -1, copyNePos, null, true);
                    newPhrase.add(copyNe);
                }
            }
        } else if (newNbBeatsPerBar < oldNbBeatsPerBar)
        {
            // Truncate each bar, shorten notes longer than 1 (old) bar
            for (NoteEvent ne : oldPhrase)
            {
                float oldPosInBeats = ne.getPositionInBeats();
                bar = (int) Math.floor(oldPosInBeats / oldNbBeatsPerBar);
                float relPosInBeats = oldPosInBeats - (bar * oldNbBeatsPerBar);     // Position within the bar
                if (relPosInBeats < newNbBeatsPerBar - 0.1f)
                {
                    float newPosInBeats = bar * newNbBeatsPerBar + relPosInBeats;
                    float newDur = ne.getDurationInBeats();
                    if (newDur + relPosInBeats >= newNbBeatsPerBar)
                    {
                        // Note lasts beyond its starting bar, make it shorter proportionnally 
                        newDur *= ((float) newNbBeatsPerBar) / oldNbBeatsPerBar;
                    }
                    if (newPosInBeats + newDur >= nbBars * newNbBeatsPerBar)
                    {
                        // Can't exceed end of phrase
                        newDur = nbBars * newNbBeatsPerBar - newPosInBeats - 0.1f;
                    }
                    NoteEvent newNe = ne.setAll(-1, newDur, -1, newPosInBeats, null, true);
                    newPhrase.add(newNe);
                }
            }
        } else
        {
            // Easy 
            newPhrase.add(oldPhrase);
        }
    }

    /**
     * Adapt oldStyle to a newTimeSignature.
     *
     * @param oldStyle
     * @param newTimeSig
     * @return
     */
    private Style adaptStyle(Style oldStyle, TimeSignature newTimeSig)
    {
        var adaptedStyle = new Style();

        // Copy fields        
        adaptedStyle.name = oldStyle.name;
        adaptedStyle.timeSignature = newTimeSig;
        adaptedStyle.ticksPerQuarter = oldStyle.ticksPerQuarter;
        adaptedStyle.tempo = oldStyle.tempo;
        adaptedStyle.division = oldStyle.division;
        adaptedStyle.sffType = oldStyle.sffType;

        // Copy sInt
        adaptedStyle.getSInt().set(oldStyle.getSInt());

        // Copy the StyleParts
        for (StylePartType type : oldStyle.getStylePartTypes())
        {
            // Create the copy that we update below
            adaptedStyle.addStylePart(type);
            StylePart newSp = adaptedStyle.getStylePart(type);
            StylePart oldSp = oldStyle.getStylePart(type);

            // Copy cTab data
            for (Integer channel : oldSp.getSourceChannels(null, null, null))
            {
                CtabChannelSettings cTab = oldSp.getCtabChannelSettings(channel);
                newSp.setCtabChannelSettings(channel, cTab);
            }

            // Update size in beats of the reference phrase
            float oldSizeInBeats = oldSp.getSizeInBeats();  // It's an int
            int nbBars = Math.round(oldSizeInBeats / sourceRhythm.getTimeSignature().getNbNaturalBeats());
            float newSizeInBeats = nbBars * newTimeSig.getNbNaturalBeats();
            newSp.setSizeInBeats(newSizeInBeats);

            // Copy and update SourcePhraseSets
            for (Integer complexity : oldSp.getComplexityLevels())      // Get List<SPS> per complexity
            {
                int variationId = 0;
                for (SourcePhraseSet oldSps : oldSp.getSourcePhraseSets(complexity))   // Get SPS per variation
                {
                    SourcePhraseSet newSps = newSp.getSourcePhraseSet(complexity, variationId);
                    if (newSps == null)
                    {
                        newSps = new SourcePhraseSet(oldSps.getClientProperty(SourcePhraseSet.PROP_ID));
                    }
                    LOGGER.log(Level.FINE, "loadResources() type={0} complexity={1} variationId={2}", new Object[]
                    {
                        type, complexity,
                        variationId
                    });
                    for (Integer channel : oldSps.getSourceChannels())          // Get Phrase per channel
                    {
                        SourcePhrase oldPhrase = oldSps.getPhrase(channel);
                        SourcePhrase newPhrase = new SourcePhrase(channel, oldPhrase.getSourceChordSymbol());
                        copyAndAdaptNotes(nbBars, oldPhrase, newPhrase);        // Copy relevant notes at new position
                        newSps.setPhrase(channel, newPhrase);
                        if (type == StylePartType.Main_A && oldSp.getAccType(channel) == AccType.RHYTHM)
                        {
                            LOGGER.log(Level.FINE, "loadResources() MainA AccType={0} oldPhrase=\n{1}", new Object[]
                            {
                                oldSp.getAccType(channel),
                                oldPhrase
                            });
                            LOGGER.log(Level.FINE, "loadResources() MainA AccType={0} newPhrase=\n{1}", new Object[]
                            {
                                oldSp.getAccType(channel),
                                newPhrase
                            });
                        }
                    }
                    newSp.addSourcePhraseSet(newSps, complexity);   // Add the new SourcePhraseSet 
                    variationId++;
                }
            }
        }

        return adaptedStyle;
    }

    /**
     * Create RhythmVoiceDelegates for source RhythmVoices.
     *
     * @return
     */
    private List<RhythmVoiceDelegate> buildRhythmVoices()
    {
        List<RhythmVoiceDelegate> res = sourceRhythm.getRhythmVoices().stream()
                .map(rv -> RhythmVoiceDelegate.createInstance(this, rv))
                .toList();
        return res;
    }

    /**
     * Directly reuse the source rhythm RhythmParameters, except we need new specific instances.
     *
     * @return
     */
    private List<RhythmParameter<?>> buildRhythmParameters()
    {
        List<RhythmParameter<?>> res = new ArrayList<>();
        // Some RhythmParameters take a Rhythm as argument, can't directly reuse
        for (RhythmParameter<?> rp : sourceRhythm.getRhythmParameters())
        {
            if (rp instanceof RP_SYS_Variation
                    || rp instanceof RP_SYS_Variation
                    || rp instanceof RP_SYS_Feel
                    || rp instanceof RP_SYS_Intensity
                    || rp instanceof RP_SYS_Fill
                    || rp instanceof RP_SYS_Mute
                    || rp instanceof RP_SYS_Marker
                    || rp instanceof RP_SYS_TempoFactor)
            {
                // Those rhythmParameters can be directly reused between rhythms
                res.add(rp);
            } else if (rp instanceof RP_SYS_CustomPhrase)
            {
                res.add(new RP_SYS_CustomPhrase(this, rp.isPrimary()));
            } else if (rp instanceof RP_SYS_DrumsTransform)
            {
                res.add(new RP_SYS_DrumsTransform(getRhythmVoice(AccType.RHYTHM), rp.isPrimary()));
            } else if (rp instanceof RP_SYS_OverrideTracks)
            {
                res.add(new RP_SYS_OverrideTracks(this, rp.isPrimary()));
            }
        }
        return res;
    }


}
