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

import java.util.logging.Level;
import org.jjazz.yamjjazz.rhythm.api.Style;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.parser.MidiParserListenerAdapter;
import org.jjazz.rhythm.api.Division;

/**
 * MidiParserListener to retrieve header and SFF1 sections data in a standard Yamaha style file: sequence name, tempo, time signature.
 * <p>
 * Also analyze the Notes to infer the style's division.
 */
public class MPL_MiscData extends MidiParserListenerAdapter
{

    private enum Section
    {
        INITIAL, SFF1, SFF2, COUNTING_NOTES, OTHER
    };
    private Section currentSection = Section.INITIAL;
    private final Style style;
    private int halfBeatNotes = 0;
    private int eighthTriplet2notes = 0;
    private int eighthTriplet3notes = 0;
    private int offBeatNotes = 0;
    private int sixteenth2or4notes = 0;
    private int channel = -1;
    private float sectionStartPosInBeats;

    private static final Logger LOGGER = Logger.getLogger(MPL_MiscData.class.getSimpleName());

    public MPL_MiscData(Style style)
    {
        this.style = style;

    }

    @Override
    public void onMarkerParsed(String marker, float posInBeats)
    {
        if (marker.equals("SFF1"))
        {
            currentSection = Section.SFF1;
            style.sffType = Style.SFFtype.SFF1;
        } else if (marker.equals("SFF2"))
        {
            currentSection = Section.SFF2;
            style.sffType = Style.SFFtype.SFF2;
        } else if (marker.equals("Main A") || marker.equals("Main B"))
        {
            // Limit counting only to those 2 first sections make computeDivision() a bit more accurate
            currentSection = Section.COUNTING_NOTES;
        } else
        {
            currentSection = Section.OTHER;
        }
        sectionStartPosInBeats = posInBeats;
    }

    @Override
    public void onTrackNameParsed(String name, float posInBeats)
    {
        style.name = name;
    }

    @Override
    public void onTimeSignatureParsed(byte numerator, byte powerOfTwo, float positionInBeats)
    {
        if (currentSection.equals(Section.INITIAL))
        {
            style.timeSignature = TimeSignature.get(numerator, (int) Math.pow(2, powerOfTwo));
        }
    }

    @Override
    public void onTempoChanged(int tempo, float positionInBeats)
    {
        if (currentSection.equals(Section.INITIAL))
        {
            style.tempo = tempo;
        }
    }

    @Override
    public void onChannelChanged(byte c)
    {
        channel = c;
    }

    @Override
    public void onNoteParsed(Note note, float posInBeats)
    {
        // if (currentSection != Section.COUNTING_NOTES || channel < 8 || channel > 10)
        if (currentSection != Section.COUNTING_NOTES)
        {
            // Count notes only for specific sections (eg Main A and Main B), and possibly specific channels (eg drums/perc/bass channels)
            // This is to improve computeDivision() accuracy
            return;
        }

        // Count the nb of notes depending on the position                
        float relPosInBeats = posInBeats - sectionStartPosInBeats;
        float inBeatPos = relPosInBeats - (float) Math.floor(relPosInBeats);

        // Yamaha styles are usually much quantized, so we can a use small window        
        final float WINDOW = 0.0416f;
        assert (0.25f + WINDOW) < (0.3333f - WINDOW) : "WINDOW=" + WINDOW;   // Check there is no overlap

        if (inBeatPos < WINDOW || inBeatPos >= 1 - WINDOW)
        {
            // On the beat: do nothing
        } else
        {
            offBeatNotes++;
            if (isInRange(inBeatPos, 0.5f, WINDOW))
            {
                // On a half beat
                halfBeatNotes++;
            } else if (isInRange(inBeatPos, 0.3333f, WINDOW))
            {
                // 2nd eighth triplet
                eighthTriplet2notes++;
            } else if (isInRange(inBeatPos, 0.6666f, WINDOW))
            {
                // 3rd eighth triplet 
                eighthTriplet3notes++;
            } else if (isInRange(inBeatPos, 0.25f, WINDOW) || isInRange(inBeatPos, 0.75f, WINDOW))
            {
                // On one of the 2 sixteenth
                sixteenth2or4notes++;
//                LOGGER.log(Level.SEVERE, "sixteenthnote counted channel={0} pitch={1} relPosInBeats={2}", new Object[]
//                {
//                    channel, note.getPitch(), relPosInBeats
//                });
            }
        }
    }

    @Override
    public void onMetaEndEvent(float posInBeats)
    {
        style.division = computeDivision();
    }

    // ============================================================================================
    // Private methods
    // ============================================================================================

    /**
     * Triplet and swing are well recognized, with limited errors. 
     * 
     * But the frontier between 8-beat and 16-beat is often blurry, it's no reliable enough => we just evaluate "Binary".
     *
     * @return
     */
    private Division computeDivision()
    {
        // We can compute the division
        Division res = Division.BINARY;     // By default
        if (offBeatNotes > 0)
        {
            float ratioHalfOff = (float) halfBeatNotes / offBeatNotes;
            float ratioSixteenthOff = (float) sixteenth2or4notes / offBeatNotes;
            float ratioEightTriplet2Off = (float) eighthTriplet2notes / offBeatNotes;
            float ratioEightTriplet3Off = (float) eighthTriplet3notes / offBeatNotes;
            int eighthTriplet2plus3 = eighthTriplet2notes + eighthTriplet3notes;
            float ratioEighthTriplet3OverTriplets = eighthTriplet2plus3 > 0 ? (float) eighthTriplet3notes / eighthTriplet2plus3 : 0;
            if (ratioHalfOff < 0.22)
            {
                // Ternary : half-beat notes are rare in the off-beat notes        
                // Choose swing if most of the off-beat notes are on the 3rd triplet
                res = ratioEighthTriplet3OverTriplets > 0.68 ? Division.EIGHTH_SHUFFLE : Division.EIGHTH_TRIPLET;
            } else 
            {
                // Make it simple (no more try to distinguish 8-beat from 16-beat)
                res = Division.BINARY;
            }

            LOGGER.log(Level.FINE,
                    "computeDivision() offBeatNotes={0} halfBeatNotes={1} eighthTriplet2notes={2} eighthTriplet3notes={3} sixteenth2or4notes={4}", new Object[]
                    {
                        offBeatNotes, halfBeatNotes, eighthTriplet2notes, eighthTriplet3notes, sixteenth2or4notes
                    });
            LOGGER.log(Level.FINE,
                    "computeDivision() ratioHalfOff={0} ratioSixteenthOff={1} ratioEightTriplet2Off={2} ratioEightTriplet3Off={3} ratioEightTriplet3OverTriplets={4}",
                    new Object[]
                    {
                        ratioHalfOff, ratioSixteenthOff, ratioEightTriplet2Off, ratioEightTriplet3Off, ratioEighthTriplet3OverTriplets
                    });
        }

        LOGGER.log(Level.FINE, "computeDivision() ==> res={0}", res);

        return res;
    }

    private boolean isInRange(float value, float windowCenter, float halfWindowSize)
    {
        return value >= (windowCenter - halfWindowSize) && value < (windowCenter + halfWindowSize);
    }

}
