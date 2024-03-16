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
package org.jjazz.midi.api.keymap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.DrumKit.Subset;

/**
 * The XG PopLatin key map.
 */
public class KeyMapXG_PopLatin implements DrumKit.KeyMap
{

    public static final String NAME = "XG_POPLATIN";
    private static KeyMapXG_PopLatin INSTANCE;
    private HashMap<String, Integer> mapNamePitch = new HashMap<>();
    private HashMap<Integer, String> mapPitchName = new HashMap<>();
    private HashMap<DrumKit.Subset, List<Integer>> mapSubsetPitches = new HashMap<>();
    private final KeyRange range = new KeyRange(13, 84);

    public static KeyMapXG_PopLatin getInstance()
    {
        synchronized (KeyMapXG_PopLatin.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new KeyMapXG_PopLatin();
            }
        }
        return INSTANCE;
    }

    private KeyMapXG_PopLatin()
    {
        addNote("Cajon Low", 13);
        addNote("Cajon Slap", 14);
        addNote("Cajon Tip", 15);
        addNote("Claves High", 16);
        addNote("Claves Low", 17);
        addNote("Hand Clap", 18, Subset.SNARE, Subset.SNARE_HANDCLAP);
        // No 19    
        addNote("Finger Snap", 20);
        addNote("Castanet", 21);
        addNote("Conga H Tip", 22);
        addNote("Conga H Heel", 23);
        addNote("Conga H Open", 24);
        addNote("Conga H Mute", 25);
        addNote("Conga H Slap Open", 26);
        addNote("Conga H Slap", 27);
        addNote("Conga H Slap Mute", 28);
        addNote("Conga L Tip", 29);
        addNote("Conga L Heel", 30);
        addNote("Conga L Open", 31);
        addNote("Conga L Mute", 32);
        addNote("Conga L Slap Open", 33);
        addNote("Conga L Slap", 34);
        addNote("Conga L Slide", 35);
        addNote("Bongo H Open 1 finger", 36);
        addNote("Bongo H Open 3 finger", 37);
        addNote("Bongo H Rim", 38);
        addNote("Bongo H Tip", 39);
        addNote("Bongo H Heel", 40);
        addNote("Bongo H Slap", 41);
        addNote("Bongo L Open 1 finger", 42);
        addNote("Bongo L Open 3 finger", 43);
        addNote("Bongo L Rim", 44);
        addNote("Bongo L Tip", 45);
        addNote("Bongo L Heel", 46);
        addNote("Bongo L Slap", 47);
        addNote("Timbale L Open", 48);
        // No 49
        // No 50
        // No 51
        // No 52
        addNote("Paila L", 53);
        addNote("Timbale H Open", 54);
        // No 55
        // No 56
        // No 57
        // No 58
        addNote("Paila H", 59);
        addNote("Cowbell Top", 60);
        addNote("Cowbell 1", 61);
        addNote("Cowbell 2", 62);
        addNote("Cowbell 3", 63);
        addNote("Guiro Short", 64);
        addNote("Guiro Long", 65);
        addNote("Metal Guiro Short", 66);
        addNote("Metal Guiro Long", 67);
        addNote("Tambourine", 68);
        addNote("Tambourim Open", 69);
        addNote("Tambourim Mute", 70);
        addNote("Tambourim Tip", 71);
        addNote("Maracas", 72);
        addNote("Shaker", 73);
        addNote("Cabasa", 74);
        addNote("Cuica Mute", 75);
        addNote("Cuica Open", 76);
        addNote("Cowbell High 1", 77);
        addNote("Cowbell High 2", 78);
        addNote("Shekere", 79);
        addNote("Shekere Tone", 80);
        addNote("Triangle Mute", 81);
        addNote("Triangle Open", 82);
        // No 83
        addNote("Wind Chime", 84);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public boolean isContaining(DrumKit.KeyMap otherKeyMap)
    {
        return otherKeyMap == this;
    }

    @Override
    public KeyRange getRange()
    {
        return range;
    }

    @Override
    public String getKeyName(int pitch)
    {
        return mapPitchName.get(pitch);
    }

    @Override
    public int getKey(String noteName)
    {
        Integer res = mapNamePitch.get(noteName.toUpperCase());
        return res != null ? res : -1;
    }

    @Override
    public List<Integer> getKeys(DrumKit.Subset... subsets)
    {
        var res = new ArrayList<Integer>();

        for (DrumKit.Subset subset : subsets)
        {
            var pitches = mapSubsetPitches.get(subset);
            if (pitches != null)
            {
                res.addAll(pitches);
            }
        }

        return res;
    }

    /**
     *
     * @param name
     * @param pitch
     * @param subsets If empty assign to Subset.PERCUSSION by default
     */
    private void addNote(String name, int pitch, DrumKit.Subset... subsets)
    {
        if (pitch < 0 || pitch > 127 || name == null || name.trim().isEmpty())
        {
            throw new IllegalArgumentException("pitch=" + pitch + " name=" + name);   
        }
        name = name.toUpperCase();
        if (mapNamePitch.get(name) != null || mapPitchName.get(pitch) != null)
        {
            throw new IllegalArgumentException("pitch=" + pitch + ", name=" + name + ": value already used");   
        }
        mapNamePitch.put(name, pitch);
        mapPitchName.put(pitch, name);
        var workSubsets = new ArrayList<>(Arrays.asList(subsets));
        if (workSubsets.isEmpty())
        {
            workSubsets.add(DrumKit.Subset.PERCUSSION);
        }
        for (DrumKit.Subset subset : workSubsets)
        {
            var notes = mapSubsetPitches.get(subset);
            if (notes == null)
            {
                notes = new ArrayList<>();
                mapSubsetPitches.put(subset, notes);
            }
            notes.add(pitch);
        }

    }
}
