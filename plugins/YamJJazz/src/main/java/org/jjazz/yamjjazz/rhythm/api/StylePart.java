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
package org.jjazz.yamjjazz.rhythm.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.phrase.api.SourcePhrase;
import org.jjazz.phrase.api.SourcePhraseSet;

/**
 * Data representing a Style Part, e.g. MAIN_A.
 * <p>
 * For each source Midi channel, store ctab data and SourcePhraseSets.<br>
 * SourcePhraseSets are stored per ComplexityLevel. There can be several SourcePhraseSets variations for one complexity level.
 * Standard Yamaha styles use only one variation at the default complexity level. <br>
 * Cntt data is discarded because not used in this version.
 */
public class StylePart
{

    public static final int DEFAULT_COMPLEXITY_LEVEL = 1;
    private final StylePartType type;
    /**
     * Store the CtabChannelSettings for each channel.
     */
    private final HashMap<Integer, CtabChannelSettings> mapChannelCtab = new HashMap<>();
    /**
     * The data structure to hold the SourcePhrases.
     */
    private final HashMap<Integer, List<SourcePhraseSet>> mapComplexity_PhraseList = new HashMap<>();

    private float sizeInBeats;
    private static final Logger LOGGER = Logger.getLogger(StylePart.class.getSimpleName());

    /**
     * Create a StylePart of specified type.
     * <p>
     * StylePart is initialized with 1 SourcePhraseSet at DEFAULT_COMPLEXITY_LEVEL.
     *
     * @param t
     */
    public StylePart(StylePartType t)
    {
        type = t;
        sizeInBeats = 0;
        SourcePhraseSet sps = new SourcePhraseSet(type.toString());
        addSourcePhraseSet(sps, StylePart.DEFAULT_COMPLEXITY_LEVEL);
    }

    public StylePartType getType()
    {
        return type;
    }

    /**
     * Get this StylePart's CtabChannelSettings for the specified channel.
     *
     * @param channel
     * @return
     */
    public CtabChannelSettings getCtabChannelSettings(int channel)
    {
        return mapChannelCtab.get(channel);
    }

    /**
     * Set the StylePart's CtabChannelSettings for the specified channel.
     *
     * @param channel
     * @param cTab
     */
    public void setCtabChannelSettings(int channel, CtabChannelSettings cTab)
    {
        if (!MidiConst.checkMidiChannel(channel) || cTab == null)
        {
            throw new IllegalArgumentException("channel=" + channel + " cTab=" + cTab);   //NOI18N
        }
        mapChannelCtab.put(channel, cTab);
    }

    /**
     * Get the size of this stylepart.
     * <p>
     * NOTE: might be 0 if music data has not been loaded yet (see isMusicLoaded()).
     *
     * @return
     */
    public float getSizeInBeats()
    {
        return sizeInBeats;
    }

    /**
     * Set the size of this stylepart : must be consistent with the actual source phrases size.
     * <p>
     *
     * @param sizeInBeats Must be an integer value.
     */
    public void setSizeInBeats(float sizeInBeats)
    {
        if (sizeInBeats < 0 || (sizeInBeats % 1) != 0)
        {
            throw new IllegalArgumentException("sizeInBeats=" + sizeInBeats);   //NOI18N
        }
        this.sizeInBeats = sizeInBeats;
    }

    /**
     * Add a SourcePhraseSet for the specified complexity level.
     * <p>
     * There can be several SourcePhraseSets variations for a given complexity level. Size in beats of the source phrases must be
     * consistent with getSizeInBeats().
     *
     * @param sps
     * @param complexity An integer &gt; 0
     */
    public void addSourcePhraseSet(SourcePhraseSet sps, int complexity)
    {
        if (sps == null || complexity < 1)
        {
            throw new IllegalArgumentException("sps=" + sps + " complexity=" + complexity);   //NOI18N
        }
        List<SourcePhraseSet> list = mapComplexity_PhraseList.get(complexity);
        if (list == null)
        {
            list = new ArrayList<>();
            mapComplexity_PhraseList.put(complexity, list);
        }
        list.add(sps);
    }

    /**
     * The complexity levels for which there is at least 1 SourcePhraseSet.
     *
     * @return A list with an ascending order.
     */
    public List<Integer> getComplexityLevels()
    {
        ArrayList<Integer> res = new ArrayList<>(mapComplexity_PhraseList.keySet());
        Collections.sort(res);
        return res;
    }

    /**
     * Get the SourcePhraseSet for the specified parameters.
     *
     * @param complexity An integer &gt; 0
     * @param variationIndex The index of the variation phrase &gt;=0
     * @return Can be null
     */
    public SourcePhraseSet getSourcePhraseSet(int complexity, int variationIndex)
    {
        if (complexity < 1 || variationIndex < 0)
        {
            throw new IllegalArgumentException("complexity=" + complexity + " variationIndex=" + variationIndex);   //NOI18N
        }
        List<SourcePhraseSet> list = mapComplexity_PhraseList.get(complexity);
        if (list == null || variationIndex >= list.size())
        {
            return null;
        }
        return list.get(variationIndex);
    }

    /**
     * Get the SourcePhraseSet which match the specified parameters.
     *
     * @param complexity
     * @param propName the name of the property to check
     * @param propValue The value of the of the property to check. Can be null to check if property is not set.
     * @return Null if not found
     */
    public SourcePhraseSet getSourcePhraseSet(int complexity, String propName, String propValue)
    {
        if (complexity < 1 || propName == null)
        {
            throw new IllegalArgumentException("complexity=" + complexity + " propName=" + propName + " propValue=" + propValue);   //NOI18N
        }
        SourcePhraseSet res = null;
        for (SourcePhraseSet sps : getSourcePhraseSets(complexity))
        {
            if ((propValue == null && sps.getClientProperty(propName) == null) || (propValue != null && propValue.equals(sps.getClientProperty(propName))))
            {
                res = sps;
                break;
            }
        }
        return res;
    }

    /**
     * Get the list of SourcePhraseSets for the specified complexity.
     *
     * @param complexity
     * @return
     */
    public List<SourcePhraseSet> getSourcePhraseSets(int complexity)
    {
        ArrayList<SourcePhraseSet> res = new ArrayList<>();
        List<SourcePhraseSet> list = mapComplexity_PhraseList.get(complexity);
        if (list != null)
        {
            res.addAll(list);
        }
        return res;
    }

    /**
     * Check if music data has been loaded for this StylePart.
     *
     * @return True if there is at least one non-empty SourcePhraseSet for a given complexity level with one SourcePhrase inside.
     */
    public boolean isMusicLoaded()
    {
        boolean b = false;
        for (Integer complexityLevel : mapComplexity_PhraseList.keySet())
        {
            List<SourcePhraseSet> spsList = mapComplexity_PhraseList.get(complexityLevel);
            for (SourcePhraseSet sps : spsList)
            {
                if (!sps.getSourceChannels().isEmpty())
                {
                    b = true;
                    break;
                }
            }
        }
        return b;
    }

    /**
     * Remove the music data for the specified complexity level.
     * <p>
     * This calls clear() on each SourcePhraseSet of this complexityLevel.
     *
     * @param complexityLevel If value is &lt; 1, remove music data for all the complexity levels.
     */
    public void clearMusicData(int complexityLevel)
    {
        if (complexityLevel < 1)
        {
            for (Integer cl : mapComplexity_PhraseList.keySet())
            {
                List<SourcePhraseSet> spsList = mapComplexity_PhraseList.get(cl);
                for (SourcePhraseSet sps : spsList)
                {
                    sps.clear();
                }
            }
        } else
        {
            List<SourcePhraseSet> spsList = mapComplexity_PhraseList.get(complexityLevel);
            if (spsList != null)
            {
                for (SourcePhraseSet sps : spsList)
                {
                    sps.clear();
                }
            }
        }
    }

    /**
     * Get the source channels matching the parameters.
     * <p>
     * There might have 0, 1 or more channels for the specified AccType and chord symbol. There are 2 (or more) channels when each
     * channel handles a part of the chord, e.g one channel for the 1+5 notes, another for the 3+7 notes.
     * <p>
     * Channels match if :<br>
     * - same AccType as at<br>
     * - rootNote is not muted<br>
     * - chord is not muted<br>
     *
     * @param at If null, parameter is ignored
     * @param rootNote If null, parameter is ignored.
     * @param yc If null, parameter is ignored.
     * @return The channels which match the parameters.
     */
    public List<Integer> getSourceChannels(AccType at, Note rootNote, YamChord yc)
    {
        ArrayList<Integer> res = new ArrayList<>();
        for (Integer channel : mapChannelCtab.keySet())
        {
            CtabChannelSettings cTab = mapChannelCtab.get(channel);
            if ((at == null || cTab.accType.equals(at))
                    && (rootNote == null || !cTab.isMuted(rootNote))
                    && (yc == null || !cTab.isMuted(yc)))
            {
                res.add(channel);
            }
        }
        return res;
    }

    /**
     * The AccParts used by this object, in ascending channel order.
     *
     * @return
     */
    public List<AccType> getAccTypes()
    {
        ArrayList<AccType> res = new ArrayList<>();
        ArrayList<Integer> channels = new ArrayList<>(mapChannelCtab.keySet());
        Collections.sort(channels);
        for (Integer channel : channels)
        {
            AccType at = mapChannelCtab.get(channel).accType;
            if (!res.contains(at))
            {
                res.add(at);
            }
        }
        return res;
    }

    /**
     * Get the AccType for a specified channel (which can be a "secondary" source channel or the "main" one).
     *
     * @param channel
     * @return Null if this channel is not used.
     */
    public AccType getAccType(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);   //NOI18N
        }
        CtabChannelSettings cTab = mapChannelCtab.get(channel);
        return cTab == null ? null : mapChannelCtab.get(channel).accType;
    }

    public void dump(boolean showMusicData, boolean showCtab)
    {
        List<Integer> channels = getSourceChannels(null, null, null);
        LOGGER.log(Level.INFO, "STYLEPART: {0} sizeInBeats={1} channels={2}", new Object[]{getType(), getSizeInBeats(), channels});
        LOGGER.log(Level.INFO, " CTABs : {0}", getType());
        if (showCtab)
        {
            for (Integer channel : channels)
            {
                CtabChannelSettings settings = getCtabChannelSettings(channel);
                settings.dump();
            }
        }
        LOGGER.log(Level.INFO, " MUSIC : {0}", getType());
        if (showMusicData)
        {
            for (int complexity : this.getComplexityLevels())
            {
                LOGGER.log(Level.INFO, "   COMPLEXITY : {0}", complexity);
                int i = 0;
                for (SourcePhraseSet sps : getSourcePhraseSets(complexity))
                {
                    LOGGER.log(Level.INFO, "      VARIATION INDEX : {0}", i);
                    for (Integer channel : sps.getSourceChannels())
                    {
                        SourcePhrase sp = sps.getPhrase(channel);
                        AccType at = getCtabChannelSettings(channel).accType;
                        LOGGER.log(Level.INFO, "{0}:\n{1}", new Object[]{at, sp.toString()});
                    }
                    i++;
                }
            }
        }
        LOGGER.log(Level.INFO, "  --END of StylePart {0}--", getType());
    }

    @Override
    public String toString()
    {
        return "[" + getType() + "," + getSizeInBeats() + "beats]";
    }

    // =======================================================================================
    // Private methods
    // =======================================================================================
}
