/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.harmony.spi;

import java.util.List;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.DefaultScaleManager;
import org.jjazz.harmony.api.Degree;
import org.jjazz.harmony.api.StandardScale;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.openide.util.Lookup;

/**
 * Manage StandardScale instances.
 */
public interface ScaleManager
{

    public static StandardScale AEOLIAN = new StandardScale("Aeolian", Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH,
            Degree.THIRTEENTH_FLAT, Degree.SEVENTH_FLAT);
    public static StandardScale ALTERED = new StandardScale("Altered", Degree.ROOT, Degree.NINTH_FLAT, Degree.NINTH_SHARP, Degree.THIRD, Degree.FIFTH_FLAT,
            Degree.THIRTEENTH_FLAT, Degree.SEVENTH_FLAT);
    public static StandardScale BLUES = new StandardScale("Blues", Degree.ROOT, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH_FLAT, Degree.FIFTH,
            Degree.SEVENTH_FLAT);
    public static StandardScale DIMINISHED_HALF_WHOLE = new StandardScale("Diminished half-whole", Degree.ROOT, Degree.NINTH_FLAT, Degree.THIRD_FLAT,
            Degree.THIRD,
            Degree.ELEVENTH_SHARP, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH_FLAT);
    public static StandardScale DIMINISHED_WHOLE_HALF = new StandardScale("Diminished whole-half", Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT,
            Degree.FOURTH_OR_ELEVENTH,
            Degree.FIFTH_FLAT, Degree.FIFTH_SHARP, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH);
    public static StandardScale DORIAN = new StandardScale("Dorian", Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH,
            Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH_FLAT);
    public static StandardScale LOCRIAN = new StandardScale("Locrian", Degree.ROOT, Degree.NINTH_FLAT, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH,
            Degree.FIFTH_FLAT,
            Degree.THIRTEENTH_FLAT, Degree.SEVENTH_FLAT);
    public static StandardScale LYDIAN = new StandardScale("Lydian", Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.ELEVENTH_SHARP, Degree.FIFTH,
            Degree.SIXTH_OR_THIRTEENTH,
            Degree.SEVENTH);
    public static StandardScale LYDIAN_b7 = new StandardScale("Lydian b7", Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.ELEVENTH_SHARP, Degree.FIFTH,
            Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH_FLAT);
    public static StandardScale MAJOR = new StandardScale("Major", Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH,
            Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH);
    public static StandardScale MINOR_HARMONIC = new StandardScale("Minor harmornic", Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH,
            Degree.FIFTH,
            Degree.THIRTEENTH_FLAT, Degree.SEVENTH);
    public static StandardScale MINOR_MELODIC = new StandardScale("Minor melodic", Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH,
            Degree.FIFTH,
            Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH);
    public static StandardScale MIXOLYDIAN = new StandardScale("Mixolydian", Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FOURTH_OR_ELEVENTH, Degree.FIFTH,
            Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH_FLAT);
    public static StandardScale PENTATONIC_MAJOR = new StandardScale("Pentatonic major", Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FIFTH,
            Degree.SIXTH_OR_THIRTEENTH);
    public static StandardScale PENTATONIC_MINOR = new StandardScale("Pentatonic minor", Degree.ROOT, Degree.NINTH, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH,
            Degree.FIFTH,
            Degree.SEVENTH_FLAT);
    public static StandardScale PHRYGIAN = new StandardScale("Phrygian", Degree.ROOT, Degree.NINTH_FLAT, Degree.THIRD_FLAT, Degree.FOURTH_OR_ELEVENTH,
            Degree.FIFTH,
            Degree.THIRTEENTH_FLAT, Degree.SEVENTH_FLAT);
    public static StandardScale WHOLE_TONE = new StandardScale("Whole tone", Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FIFTH_FLAT, Degree.THIRTEENTH_FLAT,
            Degree.SEVENTH_FLAT);


    /**
     * Get the implementation found in the global lookup, or if not found return the DefaultScaleManager instance.
     *
     * @return
     */
    static public ScaleManager getDefault()
    {
        var res = Lookup.getDefault().lookup(ScaleManager.class);
        if (res == null)
        {
            res = DefaultScaleManager.getInstance();
        }
        return res;
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
    List<StandardScaleInstance> getMatchingScales(ChordSymbol cs);

    /**
     * A list of standard scales.
     * <p>
     * The first items must be in this order: MAJOR DORIAN PHRYGIAN LYDIAN MIXOLYDIAN AEOLIAN LOCRIAN MINOR_HARMONIC MINOR_MELODIC ALTERED LYDIAN_b7
     * DIMINISHED_WHOLE_HALF DIMINISHED_HALF_WHOLE WHOLE_TONE PENTATONIC_MAJOR PENTATONIC_MINOR BLUES
     *
     * @return
     */
    List<StandardScale> getStandardScales();

    /**
     * Get a standard scale by the name.
     *
     * @param name Ignore case. Can be only the first letters of the name.
     * @return Can be null
     */
    default StandardScale getStandardScale(String name)
    {
        return getStandardScales().stream()
                .filter(sc -> sc.getName().toLowerCase().startsWith(name))
                .findAny()
                .orElse(null);
    }

}
