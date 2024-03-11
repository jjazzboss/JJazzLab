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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SourcePhrase;
import org.jjazz.phrase.api.SourcePhraseSet;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Feel;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Variation;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Mute;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.yamjjazz.AccType;
import org.jjazz.yamjjazz.CtabChannelSettings;
import org.jjazz.yamjjazz.Style;
import org.jjazz.yamjjazz.StylePart;
import org.jjazz.yamjjazz.StylePartType;

/**
 * A time signature adapter rhythm for our YamJJazz rhythms.
 * <p>
 */
public class YamJJazzAdaptedRhythmImpl implements YamJJazzRhythm, AdaptedRhythm, MusicGenerator
{

    private String rhythmProviderId;
    private final List<RhythmVoice> rhythmVoices = new ArrayList<>();
    private final List<RhythmParameter<?>> rhythmParameters = new ArrayList<>();
    private YamJJazzRhythm yjr; // The original rhythm
    private TimeSignature newTs; // The new time signature
    private Style newStyle; // The style copy adapted to new time signature
    YamJJazzRhythmGenerator generator;
    private final transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(YamJJazzAdaptedRhythmImpl.class.getSimpleName());

    public YamJJazzAdaptedRhythmImpl(String rhythmProviderId, YamJJazzRhythm originalRhythm, TimeSignature ts)
    {
        if (rhythmProviderId == null || originalRhythm == null || ts == null || ts.equals(originalRhythm.getTimeSignature()))
        {
            throw new IllegalArgumentException("rhythmProviderId=" + rhythmProviderId + " originalRhythm=" + originalRhythm + " ts=" + ts);   //NOI18N
        }
        this.rhythmProviderId = rhythmProviderId;
        yjr = originalRhythm;
        newTs = ts;
        buildRhythmVoices();
        buildRhythmParameters();
    }

    // ==================================================================================================
    // MusicGenerator interface
    // ==================================================================================================
    @Override
    public HashMap<RhythmVoice, Phrase> generateMusic(SongContext context) throws MusicGenerationException
    {
        if (generator == null)
        {
            generator = new YamJJazzRhythmGenerator(this);
        }
        return generator.generateMusic(context);
    }

    // ==================================================================================================
    // AdaptedRhythm interface
    // ==================================================================================================
    @Override
    public Rhythm getSourceRhythm()
    {
        return yjr;
    }

    @Override
    public String getUniqueId()
    {
        return rhythmProviderId + AdaptedRhythm.RHYTHM_ID_DELIMITER + yjr.getUniqueId() + AdaptedRhythm.RHYTHM_ID_DELIMITER + newTs;
    }

    // ==================================================================================================
    // Rhythm interface
    // ==================================================================================================
    @Override
    public RhythmFeatures getFeatures()
    {
        return yjr.getFeatures();
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

        if (!yjr.isResourcesLoaded())
        {
            yjr.loadResources();
        }

        Style oldStyle = yjr.getStyle();
        newStyle = new Style();

        // Copy fields
        newStyle.name = oldStyle.name;
        newStyle.timeSignature = newTs;
        newStyle.ticksPerQuarter = oldStyle.ticksPerQuarter;
        newStyle.tempo = oldStyle.tempo;
        newStyle.feel = oldStyle.feel;
        newStyle.sffType = oldStyle.sffType;

        // Copy sInt
        newStyle.getSInt().set(oldStyle.getSInt());

        // Copy the StyleParts
        for (StylePartType type : oldStyle.getStylePartTypes())
        {
            // Create the copy that we update below
            newStyle.addStylePart(type);
            StylePart newSp = newStyle.getStylePart(type);
            StylePart oldSp = oldStyle.getStylePart(type);

            // Copy cTab data
            for (Integer channel : oldSp.getSourceChannels(null, null, null))
            {
                CtabChannelSettings cTab = oldSp.getCtabChannelSettings(channel);
                newSp.setCtabChannelSettings(channel, cTab);
            }

            // Update size in beats of the reference phrase
            float oldSizeInBeats = oldSp.getSizeInBeats();  // It's an int
            int nbBars = Math.round(oldSizeInBeats / yjr.getTimeSignature().getNbNaturalBeats());
            float newSizeInBeats = nbBars * newTs.getNbNaturalBeats();
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
                    LOGGER.log(Level.FINE, "loadResources() type={0} complexity={1} variationId={2}", new Object[]{type, complexity,
                        variationId});
                    for (Integer channel : oldSps.getSourceChannels())          // Get Phrase per channel
                    {
                        SourcePhrase oldPhrase = oldSps.getPhrase(channel);
                        SourcePhrase newPhrase = new SourcePhrase(channel, oldPhrase.getSourceChordSymbol());
                        copyAndAdaptNotes(nbBars, oldPhrase, newPhrase);        // Copy relevant notes at new position
                        newSps.setPhrase(channel, newPhrase);
                        if (type == StylePartType.Main_A && oldSp.getAccType(channel) == AccType.RHYTHM)
                        {
                            LOGGER.log(Level.FINE, "loadResources() MainA AccType={0} oldPhrase=\n{1}", new Object[]{oldSp.getAccType(channel),
                                oldPhrase});
                            LOGGER.log(Level.FINE, "loadResources() MainA AccType={0} newPhrase=\n{1}", new Object[]{oldSp.getAccType(channel),
                                newPhrase});
                        }
                    }
                    newSp.addSourcePhraseSet(newSps, complexity);   // Add the new SourcePhraseSet 
                    variationId++;
                }
            }
        }

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
        return getName() + " - " + ResUtil.getString(getClass(), "CTL_AdaptedRhythmDesc", yjr.getTimeSignature(), newTs);
    }

    @Override
    public int getPreferredTempo()
    {
        return yjr.getPreferredTempo();
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return newTs;
    }

    @Override
    public String getName()
    {
        return "[" + newTs + "]" + yjr.getName();
    }

    @Override
    public String getAuthor()
    {
        return yjr.getAuthor();
    }

    @Override
    public String getVersion()
    {
        return yjr.getVersion();
    }

    @Override
    public String[] getTags()
    {
        return yjr.getTags();
    }

    // ==================================================================================================
    // YamJJazzRhythm interface
    // ==================================================================================================
    @Override
    public AccType getAccType(String rpMuteValue)
    {
        return yjr.getAccType(rpMuteValue);
    }

    @Override
    public int getComplexityLevel(String rpValue)
    {
        return yjr.getComplexityLevel(rpValue);
    }

    /**
     * Use our rhythmVoices copies.
     *
     * @param at
     * @return
     */
    @Override
    public RhythmVoice getRhythmVoice(AccType at)
    {
        return rhythmVoices.stream()
                .filter(rv -> AccType.getAccType(rv) == at)
                .findAny().orElse(null);
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
     * Use our adapted style !
     *
     * @return
     */
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
        return yjr.isExtendedRhythm();
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
        int oldNbBeatsPerBar = (int) yjr.getTimeSignature().getNbNaturalBeats();
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
                NoteEvent newNe = ne.getCopyDurPos(newDur, newPosInBeats);
                newPhrase.add(newNe);

                if (!durExtended && relPosInBeats < (newNbBeatsPerBar - oldNbBeatsPerBar) - 0.1f)
                {
                    // Duplicate the first short notes of the bar at the end of the bar to fill the gap (unless note's duration has been extended)
                    float copyRelPosInBeats = relPosInBeats + oldNbBeatsPerBar;
                    float copyNePos = bar * newNbBeatsPerBar + copyRelPosInBeats;
                    float copyNeDur = copyRelPosInBeats + newDur >= newNbBeatsPerBar ? newNbBeatsPerBar - copyRelPosInBeats - 0.1f : newDur;
                    NoteEvent copyNe = ne.getCopyDurPos(copyNeDur, copyNePos);
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
                    NoteEvent newNe = ne.getCopyDurPos(newDur, newPosInBeats);
                    newPhrase.add(newNe);          
                }
            }
        } else
        {
            // Easy 
            newPhrase.add(oldPhrase);
        }
    }

    private void buildRhythmVoices()
    {
        yjr.getRhythmVoices().stream().forEach(rv -> rhythmVoices.add(RhythmVoiceDelegate.createInstance(this, rv)));
    }

    private void buildRhythmParameters()
    {
        // Some RhythmParameters take a Rhythm as argument, can't directly reuse
        for (RhythmParameter<?> rp : yjr.getRhythmParameters())
        {
            if (rp instanceof RP_STD_Variation
                    || rp instanceof RP_STD_Variation
                    || rp instanceof RP_STD_Feel
                    || rp instanceof RP_STD_Intensity
                    || rp instanceof RP_STD_Fill
                    || rp instanceof RP_SYS_Mute
                    || rp instanceof RP_SYS_Marker
                    || rp instanceof RP_SYS_TempoFactor)
            {
                // Those rhythmParameters can be directly reused between rhythms
                rhythmParameters.add(rp);
            } else if (rp instanceof RP_SYS_CustomPhrase)
            {
                rhythmParameters.add(new RP_SYS_CustomPhrase(this, rp.isPrimary()));
            } else if (rp instanceof RP_SYS_DrumsTransform)
            {
                rhythmParameters.add(new RP_SYS_DrumsTransform(getRhythmVoice(AccType.RHYTHM), rp.isPrimary()));
            }
        }
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

}
