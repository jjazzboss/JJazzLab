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
package org.jjazz.harmony.api;

import java.util.ArrayList;
import java.util.List;
import static org.jjazz.harmony.api.Bundle.*;
import org.openide.util.NbBundle;

@NbBundle.Messages(
        {
            "CTL_MAJOR=Major",
            "CTL_DORIAN=Dorian",
            "CTL_PHRYGIAN=Phrygian",
            "CTL_LYDIAN=Lydian",
            "CTL_MIXOLYDIAN=Mixolydian",
            "CTL_AEOLIAN=Aeolian",
            "CTL_LOCRIAN=Locrian",
            "CTL_MINOR_HARMONIC=Minor Harmonic",
            "CTL_MINOR_MELODIC=Minor Melodic",
            "CTL_ALTERED=Altered",
            "CTL_LYDIAN_b7=Lydian b7",
            "CTL_DIMINISHED_WHOLE_HALF=Diminished whole-half",
            "CTL_DIMINISHED_HALF_WHOLE=Diminished half-whole",
            "CTL_WHOLE_TONE=Whole Tone",
            "CTL_PENTATONIC_MAJOR=Pentatonic Major",
            "CTL_PENTATONIC_MINOR=Pentatonic Minor",
            "CTL_BLUES=Blues"
        })
public class ScaleManager
{

    private static ScaleManager INSTANCE;

    static public final StandardScale MAJOR = new StandardScale(CTL_MAJOR(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH);

    static public final StandardScale DORIAN = new StandardScale(CTL_DORIAN(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH_FLAT
    );
    static public final StandardScale PHRYGIAN = new StandardScale(CTL_PHRYGIAN(),
            Degree.ROOT, Degree.NINTH_FLAT, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH, Degree.THIRTEENTH_FLAT, Degree.SEVENTH_FLAT
    );
    static public final StandardScale LYDIAN = new StandardScale(CTL_LYDIAN(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.ELEVENTH_SHARP, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH
    );
    static public final StandardScale MIXOLYDIAN = new StandardScale(CTL_MIXOLYDIAN(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH_FLAT
    );
    static public final StandardScale AEOLIAN = new StandardScale(CTL_AEOLIAN(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH, Degree.THIRTEENTH_FLAT, Degree.SEVENTH_FLAT
    );
    static public final StandardScale LOCRIAN = new StandardScale(CTL_LOCRIAN(),
            Degree.ROOT, Degree.NINTH_FLAT, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH_FLAT, Degree.THIRTEENTH_FLAT, Degree.SEVENTH_FLAT
    );
    static public final StandardScale MINOR_HARMONIC = new StandardScale(CTL_MINOR_HARMONIC(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH, Degree.THIRTEENTH_FLAT, Degree.SEVENTH
    );
    static public final StandardScale MINOR_MELODIC = new StandardScale(CTL_MINOR_MELODIC(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH
    );
    static public final StandardScale ALTERED = new StandardScale(CTL_ALTERED(),
            Degree.ROOT, Degree.NINTH_FLAT, Degree.NINTH_SHARP, Degree.THIRD, Degree.FIFTH_FLAT, Degree.THIRTEENTH_FLAT, Degree.SEVENTH_FLAT
    );
    static public final StandardScale LYDIAN_b7 = new StandardScale(CTL_LYDIAN_b7(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.ELEVENTH_SHARP, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH_FLAT
    );
    static public final StandardScale DIMINISHED_WHOLE_HALF = new StandardScale(CTL_DIMINISHED_WHOLE_HALF(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH_FLAT, Degree.FIFTH_SHARP, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH
    );
    static public final StandardScale DIMINISHED_HALF_WHOLE = new StandardScale(CTL_DIMINISHED_HALF_WHOLE(),
            Degree.ROOT, Degree.NINTH_FLAT, Degree.THIRD_FLAT, Degree.THIRD, Degree.ELEVENTH_SHARP, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH_FLAT
    );
    static public final StandardScale WHOLE_TONE = new StandardScale(CTL_WHOLE_TONE(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FIFTH_FLAT, Degree.THIRTEENTH_FLAT, Degree.SEVENTH_FLAT
    );
    static public final StandardScale PENTATONIC_MAJOR = new StandardScale(CTL_PENTATONIC_MAJOR(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH
    );
    static public final StandardScale PENTATONIC_MINOR = new StandardScale(CTL_PENTATONIC_MINOR(),
            Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH, Degree.SEVENTH_FLAT
    );
    static public final StandardScale BLUES = new StandardScale(CTL_BLUES(),
            Degree.ROOT, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH_FLAT, Degree.FIFTH, Degree.SEVENTH_FLAT
    );

    private static ArrayList<StandardScale> stdScales;

    public static ScaleManager getInstance()
    {
        synchronized (ScaleManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ScaleManager();
            }
        }
        return INSTANCE;
    }

    private ScaleManager()
    {
        stdScales = new ArrayList<>();
        stdScales.add(MAJOR);
        stdScales.add(DORIAN);
        stdScales.add(PHRYGIAN);
        stdScales.add(LYDIAN);
        stdScales.add(MIXOLYDIAN);
        stdScales.add(AEOLIAN);
        stdScales.add(LOCRIAN);
        stdScales.add(MINOR_HARMONIC);
        stdScales.add(MINOR_MELODIC);
        stdScales.add(ALTERED);
        stdScales.add(LYDIAN_b7);
        stdScales.add(DIMINISHED_WHOLE_HALF);
        stdScales.add(DIMINISHED_HALF_WHOLE);
        stdScales.add(WHOLE_TONE);
        stdScales.add(PENTATONIC_MAJOR);
        stdScales.add(PENTATONIC_MINOR);
        stdScales.add(BLUES);
    }

    /**
     * A list of 17 standard scales.
     * <p>
     * MAJOR DORIAN PHRYGIAN LYDIAN MIXOLYDIAN AEOLIAN LOCRIAN MINOR_HARMONIC MINOR_MELODIC ALTERED LYDIAN_b7
     * DIMINISHED_WHOLE_HALF DIMINISHED_HALF_WHOLE WHOLE_TONE PENTATONIC_MAJOR PENTATONIC_MINOR BLUES
     *
     * @return
     */
    public List<StandardScale> getStandardScales()
    {
        return new ArrayList<>(stdScales);
    }

    /**
     * Compute the scale instances which match the specified chord symbol.
     * <p>
     * Test all standard scales based on cs root note. A scale matches if each chord note is a scale note.<br>
     * Example: C7b5=C E Gb Bb, matching scales=ALTERED, LYDIAN_b7, WHOLE_TONE, DIMINISHED_HALF_WHOLE<br>
     * Example: C7b9b5=C Db E Gb Bb, matching scales=ALTERED, DIMINISHED_HALF_WHOLE<br>
     *
     * @param cs
     * @return
     */
    public List<StandardScaleInstance> getMatchingScales(ChordSymbol cs)
    {
        ArrayList<StandardScaleInstance> ssis = new ArrayList<>();
        for (StandardScale ss : stdScales)
        {
            boolean add = true;
            for (Degree d : cs.getChordType().getDegrees())
            {
                // Don't use degree equality but pitch equality, more flexible (eg b5 and #11 can match)
                if (ss.getDegree(d.getPitch()) == null)
                {
                    add = false;
                    break;
                }
            }
            if (add)
            {
                StandardScaleInstance ssi = new StandardScaleInstance(ss, cs.getRootNote());
                ssis.add(ssi);
            }
        }
        return ssis;
    }
}
