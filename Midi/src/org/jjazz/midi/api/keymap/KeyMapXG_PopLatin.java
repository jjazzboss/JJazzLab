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
package org.jjazz.midi.api.keymap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.jjazz.midi.api.DrumKit;

/**
 * The XG PopLatin key map.
 */
public class KeyMapXG_PopLatin implements DrumKit.KeyMap
{

    public static final String NAME = "XG_POPLATIN";
    private static final Integer[] CRASH_KEYS = new Integer[]
    {
        
    };
    private static final Integer[] OPEN_HI_HAT_KEYS = new Integer[]
    {
        
    };

    private static KeyMapXG_PopLatin INSTANCE;
    private HashMap<String, Integer> mapNamePitch = new HashMap<>();
    private HashMap<Integer, String> mapPitchName = new HashMap<>();
    private ArrayList<Integer> accentPitches = new ArrayList<>();
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
        addNote("Cajon Low", 13, false);
        addNote("Cajon Slap", 14, false);
        addNote("Cajon Tip", 15, false);
        addNote("Claves High", 16, false);
        addNote("Claves Low", 17, false);
        addNote("Hand Clap", 18, true);
        // No 19    
        addNote("Finger Snap", 20, false);
        addNote("Castanet", 21, false);
        addNote("Conga H Tip", 22, false);
        addNote("Conga H Heel", 23, false);
        addNote("Conga H Open", 24, false);
        addNote("Conga H Mute", 25, false);
        addNote("Conga H Slap Open", 26, false);
        addNote("Conga H Slap", 27, false);
        addNote("Conga H Slap Mute", 28, false);
        addNote("Conga L Tip", 29, false);
        addNote("Conga L Heel", 30, false);
        addNote("Conga L Open", 31, false);
        addNote("Conga L Mute", 32, false);
        addNote("Conga L Slap Open", 33, false);
        addNote("Conga L Slap", 34, false);
        addNote("Conga L Slide", 35, false);
        addNote("Bongo H Open 1 finger", 36, false);
        addNote("Bongo H Open 3 finger", 37, false);
        addNote("Bongo H Rim", 38, false);
        addNote("Bongo H Tip", 39, false);
        addNote("Bongo H Heel", 40, false);
        addNote("Bongo H Slap", 41, false);
        addNote("Bongo L Open 1 finger", 42, false);
        addNote("Bongo L Open 3 finger", 43, false);
        addNote("Bongo L Rim", 44, false);
        addNote("Bongo L Tip", 45, false);
        addNote("Bongo L Heel", 46, false);
        addNote("Bongo L Slap", 47, false);
        addNote("Timbale L Open", 48, true);
        // No 49
        // No 50
        // No 51
        // No 52
        addNote("Paila L", 53, false);
        addNote("Timbale H Open", 54, false);
        // No 55
        // No 56
        // No 57
        // No 58
        addNote("Paila H", 59, false);
        addNote("Cowbell Top", 60, false);
        addNote("Cowbell 1", 61, false);
        addNote("Cowbell 2", 62, false);
        addNote("Cowbell 3", 63, false);
        addNote("Guiro Short", 64, false);
        addNote("Guiro Long", 65, false);
        addNote("Metal Guiro Short", 66, false);
        addNote("Metal Guiro Long", 67, false);
        addNote("Tambourine", 68, false);
        addNote("Tambourim Open", 69, false);
        addNote("Tambourim Mute", 70, false);
        addNote("Tambourim Tip", 71, false);
        addNote("Maracas", 72, false);
        addNote("Shaker", 73, false);
        addNote("Cabasa", 74, false);
        addNote("Cuica Mute", 75, false);
        addNote("Cuica Open", 76, false);
        addNote("Cowbell High 1", 77, false);
        addNote("Cowbell High 2", 78, false);
        addNote("Shekere", 79, false);
        addNote("Shekere Tone", 80, false);
        addNote("Triangle Mute", 81, false);
        addNote("Triangle Open", 82, false);
        // No 83
        addNote("Wind Chime", 84, true);
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
    public List<Integer> getCrashKeys()
    {
        return Arrays.asList(CRASH_KEYS);
    }

    @Override
    public List<Integer> getOpenHiHatKeys()
    {
        return Arrays.asList(OPEN_HI_HAT_KEYS);
    }

    @Override
    public List<Integer> getAccentKeys()
    {
        return new ArrayList<>(accentPitches);
    }

    private void addNote(String name, int pitch, boolean isAccent)
    {
        if (pitch < 0 || pitch > 127 || name == null || name.trim().isEmpty())
        {
            throw new IllegalArgumentException("pitch=" + pitch + " name=" + name);   //NOI18N
        }
        name = name.toUpperCase();
        if (mapNamePitch.get(name) != null || mapPitchName.get(pitch) != null)
        {
            throw new IllegalArgumentException("pitch=" + pitch + ", name=" + name + ": value already used");   //NOI18N
        }
        mapNamePitch.put(name, pitch);
        mapPitchName.put(pitch, name);
        if (isAccent)
        {
            accentPitches.add(pitch);
        }
    }
}
