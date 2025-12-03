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
import java.util.HashMap;
import java.util.Objects;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.spi.ChordTypeDatabase;

/**
 * A chord used in Yamaha styles plus its aliases.
 */
public class YamChord
{
    private final String name;
    static public final ArrayList<YamChord> ALL_CHORDS = new ArrayList<>();
    static private final HashMap<String, YamChord> mapAliasYamChord = new HashMap<>();

    /**
     * ****************************************************************************
     * ! Need to add all JJazz supported chord types as aliases, not complete yet !
     * ****************************************************************************
     */
    static
    {
        // ORDER IS IMPORTANT ! In relation to the Yamaha style file code for each chord type.
        ALL_CHORDS.add(new YamChord("1+2+5", "2")); //
        ALL_CHORDS.add(new YamChord("sus4", "sus")); //
        ALL_CHORDS.add(new YamChord("1+5", "")); //
        ALL_CHORDS.add(new YamChord("1+8", "")); //
        ALL_CHORDS.add(new YamChord("7aug", "7#5", "alt", "7alt", "9#5", "7b9#5"));
        ALL_CHORDS.add(new YamChord("Maj7aug", "M7#5"));
        ALL_CHORDS.add(new YamChord("7(#9)", "7#9", "7#9#5", "7#9b5", "13#9"));
        ALL_CHORDS.add(new YamChord("7(b13)", "7#5"));
        ALL_CHORDS.add(new YamChord("7(b9)", "7b9", "13b9"));
        ALL_CHORDS.add(new YamChord("7(13)", "13"));
        ALL_CHORDS.add(new YamChord("7#11", "7b9#11", "7#9#11", "13b5", "13#11", "9#11", "13b9#11"));
        ALL_CHORDS.add(new YamChord("7(9)", "9"));
        ALL_CHORDS.add(new YamChord("7b5", "9b5", "7b9b5", "13b9b5", "13#9b5"));
        ALL_CHORDS.add(new YamChord("7sus4", "7sus", "7susb9", "9sus", "13sus", "13susb9"));
        ALL_CHORDS.add(new YamChord("7th", "7"));
        ALL_CHORDS.add(new YamChord("dim7", "dim7M")); // "dim7M" does not exist in Yamaha chords
        ALL_CHORDS.add(new YamChord("dim"));
        ALL_CHORDS.add(new YamChord("minMaj7(9)", "m97M"));
        ALL_CHORDS.add(new YamChord("minMaj7", "m7M"));
        ALL_CHORDS.add(new YamChord("min7(11)", "m711", "min11", "m11", "m911", "m11b5"));
        ALL_CHORDS.add(new YamChord("min7(9)", "m9"));
        ALL_CHORDS.add(new YamChord("min(9)", "m9", "m2"));
        ALL_CHORDS.add(new YamChord("m7b5", "m9b5", "m+", "m7#5"));
        ALL_CHORDS.add(new YamChord("min7", "m7", "m13", "m713", "m7b9"));
        ALL_CHORDS.add(new YamChord("min6", "m6", "m69"));
        ALL_CHORDS.add(new YamChord("min", "m"));
        ALL_CHORDS.add(new YamChord("aug", "+"));
        ALL_CHORDS.add(new YamChord("Maj6(9)", "69"));
        ALL_CHORDS.add(new YamChord("Maj7(9)", "M9", "M713", "M13"));
        ALL_CHORDS.add(new YamChord("Maj(9)", "M9"));
        ALL_CHORDS.add(new YamChord("Maj7#11", "M7#11", "M7b5", "M7#5", "M9#11", "M13#11"));   // M7#5 do not really match but don't have a better place
        ALL_CHORDS.add(new YamChord("Maj7", "M7"));
        ALL_CHORDS.add(new YamChord("Maj6", "6"));
        ALL_CHORDS.add(new YamChord("Maj", ""));
    }

    /**
     *
     * @param n       Name of the Yamaha chord.
     * @param aliases other names for this chord (e.g. "C-7" for "Cm7")
     */
    private YamChord(String n, String... aliases)
    {
        name = n;
        mapAliasYamChord.put(n, this);
        for (String alias : aliases)
        {
            mapAliasYamChord.put(alias, this);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || !(o instanceof YamChord))
        {
            return false;
        }
        YamChord yc = (YamChord) o;
        return name.equals(yc.name);
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
        return hash;
    }
    /**
     * The JJazz chord type equivalent to this YamChord.
     *
     * @return Can not be null.
     */
    public ChordType getChordType()
    {
        ChordTypeDatabase ctdb = ChordTypeDatabase.getDefault();
        ChordType ct = ctdb.getChordType(name);
        if (ct == null)
        {
            throw new IllegalStateException(   //NOI18N
                "No JJazz ChordType found for YamChord=" + name + ". Can be solved by adding an alias in the ChordTypeDatabase code source.");
        }
        return ct;
    }

    /**
     * Get a Yamaha chord from its name or one of its alias.
     *
     * @param n
     * @return Null if not found.
     */
    static public YamChord get(String n)
    {
        return mapAliasYamChord.get(n);
    }

    public String getName()
    {
        return name;
    }
    

    @Override
    public String toString()
    {
        return getName();
    }
}
