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
package org.jjazz.yamjjazz;

import org.jjazz.yamjjazz.rhythm.api.Style;
import org.jjazz.yamjjazz.rhythm.api.CtabChannelSettings;
import org.jjazz.yamjjazz.rhythm.api.StylePartType;
import org.jjazz.yamjjazz.rhythm.api.StylePart;
import java.util.HashSet;
import java.util.logging.Level;
import org.jjazz.phrase.api.NoteEvent;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.parser.MidiParserListenerAdapter;
import org.jjazz.phrase.api.SourcePhrase;
import org.jjazz.phrase.api.SourcePhraseSet;

/**
 * MidiParserListener to retrieve music phrases from a Midi extension file with
 * a specific format.
 * <p>
 * The music phrases are added to an existing Style object initialized from a
 * standard Yamaha style file.
 * <p>
 * New music phrases for a StylePart/Complexity pair will override any existing
 * SourcePhraseSet for this StylePart/Complexity.
 * <p>
 * Each track must have a trackname MetaEvent defined with syntax: "Main
 * A-1-drums-8"<br>
 * "Main A" : a valid Yamaha style part <br>
 * "1" : complexity level (&gt0).<br>
 * "drums": An id string<br>
 * "8": the size of the phrase in nb of beats. All tracks for a given stylepart
 * must have the same size.
 * <p>
 * If Main A size=8 beats, track length must be 8 or 16 or 24 beats etc. The
 * parser derives the number of SourcePhraseSets variations from the track
 * length.
 * <p>
 * Retrieve only Notes (Note-ON/OFF), ignore all the rest (Sysex, program
 * changes, control changes, pitch bends etc.). Also set the sizeInBeats of each
 * StylePart.
 */
public class MPL_ExtensionFile extends MidiParserListenerAdapter
{

    public static final String SEPARATOR = "-";
    public boolean IS_BUGGED;            // If true a problem occured, collected data is not valid
    private final Style style;
    private int currentChannel;
    private StylePart currentStylePart;
    private int currentComplexityLevel;
    private final boolean skipMusicData;
    private boolean isFirstTrack;
    private String logName = "";
    private HashSet<String> usedSpComplexity = new HashSet<>();
    private static final Logger LOGGER = Logger.getLogger(MPL_ExtensionFile.class.getSimpleName());

    /**
     *
     * @param style The style must have its StyleParts initialized with cTab
     * data, sizeInBars set, SourcePhraseSets created (empty or not).
     * @param skipMusicData If true, get only descriptive data (eg complexity
     * levels and styleParts sizeInBeats) but don't store music data in order to
     * save memory.
     */
    public MPL_ExtensionFile(Style style, boolean skipMusicData, String logName)
    {
        this.style = style;
        resetGlobalVars();
        this.skipMusicData = skipMusicData;
        isFirstTrack = true;
        if (logName != null)
        {
            this.logName = logName;
        }
    }

    @Override
    public void onTrackNameParsed(String trackName, float posInBeats)
    {
        if (isFirstTrack)
        {
            // First track should not contain music data, normally reserved for song info, title, etc.
            // Not used here so we skip it
            isFirstTrack = false;
            return;
        }
        resetGlobalVars();

        LOGGER.log(Level.FINE, "{0} - onTrackNameParsed() -- Track name={1}", new Object[]
        {
            logName, trackName
        });

        if (posInBeats != 0)
        {
            LOGGER.log(Level.SEVERE, "{0} - onTrackNameParsed() Invalid position for Track Name meta event: {1} trackName={2}", new Object[]
            {
                logName, posInBeats, trackName
            });
            IS_BUGGED = true;
        }

        StylePartType spType = getType(trackName);
        currentComplexityLevel = getComplexityLevel(trackName);
        if (spType == null || currentComplexityLevel < 1)
        {
            LOGGER.log(Level.SEVERE, "{0} - onTrackNameParsed() Invalid track name={1}, spType={2}, currentComplexity={3}, style={4}. Skipping track.", new Object[]
            {
                logName, trackName, spType, currentComplexityLevel, style
            });
            return;
        }
        currentStylePart = style.getStylePart(spType);
        if (currentStylePart == null)
        {
            LOGGER.log(Level.SEVERE, "{0} - onTrackNameParsed() spType={1} not found. style={2}", new Object[]
            {
                logName, spType, style
            });
            IS_BUGGED = true;
            return;
        }
        float oldSizeInBeats = currentStylePart.getSizeInBeats();
        float newSizeInBeats = getSizeInBeats(trackName);
        if (oldSizeInBeats == 0)
        {
            currentStylePart.setSizeInBeats(newSizeInBeats);
        } else if (oldSizeInBeats != newSizeInBeats)
        {
            // For a given stylePart all sizes must be equal
            LOGGER.log(Level.SEVERE, "{0} - onTrackNameParsed() Invalid value for sizeInBeats={1}, was previously set={2}. Should be equal. Track name={3} style={4}", new Object[]{logName,
                newSizeInBeats, oldSizeInBeats, trackName, style});
            IS_BUGGED = true;
            return;
        }
        if (!skipMusicData && isFirstUse(spType, currentComplexityLevel))
        {
            // We will process new SourcePhrases for this stylePart/complexity, which should replaceAll any existing SourcePhraseSets
            // So make sure all possibly existing SourcePhraseSets are removed.
            currentStylePart.clearMusicData(currentComplexityLevel);
        }
    }

    @Override
    public void onTextParsed(String name, float posInBeats)
    {
        // To be used for parameters ? 
    }

    @Override
    public void onChannelChanged(byte b)
    {
        currentChannel = b;
    }

    @Override
    public void onNoteParsed(Note note, float posInBeats)
    {
        if (IS_BUGGED || currentStylePart == null)
        {
            return;
        }

        // Calculate in which SPS variant we are, first, second, etc.
        int spsIndex = (int) (posInBeats / currentStylePart.getSizeInBeats());

        // Get (or create) the corresponding SourcePhraseSet
        SourcePhraseSet sps = getSourcePhraseSet(spsIndex, currentComplexityLevel);

        if (skipMusicData)
        {
            return;
        }

        // Adjust position
        float relativePosInBeats = posInBeats - (spsIndex * currentStylePart.getSizeInBeats());

        NoteEvent ne = new NoteEvent(note.getPitch(), note.getDurationInBeats(), note.getVelocity(), relativePosInBeats);

        // Get (or create) the corresponding SourcePhraseSet phrase 
        SourcePhrase sp = sps.getPhrase(currentChannel);
        if (sp == null)
        {
            // Phrase does not exist yet, create it
            CtabChannelSettings cTab = currentStylePart.getCtabChannelSettings(currentChannel);
            assert cTab != null : "currentStylePart=" + currentStylePart + " currentComplexityLevel=" + currentComplexityLevel + " cTab=" + cTab + " currentChannel=" + currentChannel + " ne=" + ne + " spsIndex=" + spsIndex;
            sp = new SourcePhrase(currentChannel, cTab.getSourceChordSymbol());
            sps.setPhrase(currentChannel, sp);
        }
        sp.add(ne);
    }

    // ==============================================================================
    // Private methods
    // ==============================================================================
    private void resetGlobalVars()
    {
        currentStylePart = null;
        currentComplexityLevel = 1;
    }

    /**
     * Get the specified SourcePhraseSet for current StylePart and Complexity
     * <p>
     * If SourcePhraseSet does not exist create and add it to the StylePart.<br>
     *
     *
     * @param spsIndex The SourcePhraseSet index.
     * @return
     */
    private SourcePhraseSet getSourcePhraseSet(int spsIndex, int complexityLevel)
    {
        SourcePhraseSet sps = currentStylePart.getSourcePhraseSet(complexityLevel, spsIndex);
        if (sps == null)
        {
            // Create it
            sps = new SourcePhraseSet(getVariantId(spsIndex));
            currentStylePart.addSourcePhraseSet(sps, complexityLevel);
        }
        return sps;
    }

    private String getVariantId(int spsIndex)
    {
        int ascii = 'a';
        ascii += spsIndex;
        return String.valueOf((char) ascii);
    }

    /**
     * Extract the StylePart.Type from the marker : 1st field before the
     * SEPARATOR.
     *
     * @param marker eg "Main A-1-drums-8" => type=Main_A
     * @return Can be null.
     */
    public StylePartType getType(String marker)
    {
        if (marker == null || marker.isBlank())
        {
            throw new IllegalArgumentException("marker=" + marker);   //NOI18N
        }
        String strs[] = marker.trim().split(SEPARATOR);
        String str = strs[0];
        StylePartType type = StylePartType.getType(str);
        return type;
    }

    /**
     * Extract the complexity from the marker : 2st field SEPARATOR.
     *
     * @param marker eg "Main A-1-drums-8" => complexity=1
     * @return -1 if complexity could not be retrieved
     */
    private int getComplexityLevel(String marker)
    {
        if (marker == null || marker.isBlank())
        {
            throw new IllegalArgumentException("marker=" + marker);   //NOI18N
        }
        int res = -1;
        String strs[] = marker.trim().split(SEPARATOR);
        if (strs.length >= 2)
        {
            String str = strs[1].trim();
            try
            {
                res = Integer.parseInt(str);
            } catch (NumberFormatException ex)
            {
                // Nothing
            }
        }
        return res;
    }

    /**
     * Extract the trackId from the marker : 3st field SEPARATOR.
     *
     * @param marker eg "Main A-1-drums-8" => drums
     * @return null if problem
     */
    private String getTrackId(String marker)
    {
        if (marker == null || marker.isBlank())
        {
            throw new IllegalArgumentException("marker=" + marker);   //NOI18N
        }
        String res = null;
        String strs[] = marker.trim().split(SEPARATOR);
        if (strs.length >= 3)
        {
            res = strs[2].trim();
        }
        return res;
    }

    /**
     * Extract the sizeInBeats from the marker : 4st field SEPARATOR.
     *
     * @param marker eg "Main A-1-drums-8" => sizeInBeats=8
     * @return -1 if problem.
     */
    private int getSizeInBeats(String marker)
    {
        if (marker == null || marker.isBlank())
        {
            throw new IllegalArgumentException("marker=" + marker);   //NOI18N
        }
        int res = -1;
        String strs[] = marker.trim().split(SEPARATOR);
        if (strs.length >= 4)
        {
            String str = strs[3].trim();
            try
            {
                res = Integer.parseInt(str);
            } catch (NumberFormatException ex)
            {
                // Nothing
            }
        }
        return res;
    }

    private boolean isFirstUse(StylePartType type, int complexity)
    {
        String s = type.toString() + "-" + complexity;
        return usedSpComplexity.add(s);
    }
}
