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
package org.jjazz.rhythm.api;

/**
 * High-level musical genre.
 * <p>
 */
public enum Genre
{
    // Order matters for guess()
    BALLROOM("pasod"),
    BLUES("blues", "bluegr", "shuffl"),
    BOSSA("bossa", "song-for-my-father"),
    CALYPSO("calyp"),
    CHACHACHA("chac", "cha c", "cha-c"),
    CHORO("choro"),
    CLASSICAL(),    
    COUNTRY("country", "cntry"),
    CUBAN("cuban", "afro"),
    DANCE("disco", "club", "party"),
    ELECTRONIC("garage", "house", "techno"), 
    FOLK("folk"),
    FORRO("forro"),
    FUNK("funk", "fusion"),
    HIP_HOP("hip"),
    MAMBO("mambo"),
    MERENGUE("mereng"),
    METAL("metal", "punk"),
    REGGAE("regga"),
    RUMBA("rumba"),
    RnB("rnb", "r&b", "randb"),
    SALSA("salsa"),
    SAMBA("samba", "brazil", "brasil"),
    SOUL("soul", "gospel"),
    TANGO("tango"),
    UNKNOWN,    
    WORLD("world"),
    //
    // We put the very generic genres at the end for guess(), because they can be associated to other style, eg "pop bossa", "funk rock", and it's preferable
    // than guess() picks the most specific genre first (eg bossa or funk).
    // 
    LATIN("latin"),    
    POP("pop", "8beat", "8-b", "8bt", "16beat", "16bt", "16-b"),
    ROCK("rock"),           // After rock, to use "pop" for "pop rock"
    JAZZ("jazz", "swing", "jive", "footpr", "bop", "dixie", "waltz", "ballad", "bld");

    private final String[] tags;

    private Genre(String... tags)
    {
        this.tags = tags;
    }

    public String[] getTags()
    {
        return tags;
    }

    /**
     * Try to guess the genre from a string.
     *
     * @param text eg "slowBossaNov.S726.prs"
     * @return UNKNOWN if no matching found.
     */
    static public Genre guess(String text)
    {
        Genre res = UNKNOWN;
        if (text == null)
        {
            return res;
        }
        var lText = text.toLowerCase();

        for (Genre g : Genre.values())      // values() uses the order of declaration of the constants
        {
            for (var tag : g.tags)
            {
                if (lText.contains(tag.toLowerCase()))
                {
                    return g;
                }
            }
        }

        return res;
    }

    static public Genre valueOf(String s, Genre defaultValue)
    {
        try
        {
            return valueOf(s);
        } catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }
}
