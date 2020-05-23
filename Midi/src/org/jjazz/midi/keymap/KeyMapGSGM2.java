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
package org.jjazz.midi.keymap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.jjazz.midi.DrumKit;
import org.jjazz.midi.DrumKit.KeyMap;

/**
 * The GS/GM2 Midi Drum Map.
 */
public class KeyMapGSGM2 implements KeyMap
{

    public static final String NAME = "GS_GM2";
    private static final Integer[] CRASH_KEYS = new Integer[]
    {
        49, 52, 55, 57
    };
    private static final Integer[] OPEN_HI_HAT_KEYS = new Integer[]
    {
        46
    };    
    private static KeyMapGSGM2 INSTANCE;
    private HashMap<String, Integer> mapNamePitch = new HashMap<>();
    private HashMap<Integer, String> mapPitchName = new HashMap<>();
    private final KeyRange range = new KeyRange(22, 87);
    private ArrayList<Integer> accentPitches = new ArrayList<>();

    public static KeyMapGSGM2 getInstance()
    {
        synchronized (KeyMapGSGM2.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new KeyMapGSGM2();
            }
        }
        return INSTANCE;
    }

    private KeyMapGSGM2()
    {
        addNote("Beep 1", 22, false);       // SC-88 pro only
        addNote("Beep 2", 23, false);       // SC-88 pro only
        addNote("Concert Snare", 24, true);    // SC-88 pro only
        addNote("SNARE ROLL", 25, false);
        addNote("FINGER SNAP", 26, false);
        addNote("HIGH Q", 27, false);
        addNote("SLAP", 28, false);
        addNote("SCRATCH PUSH", 29, false);
        addNote("Scratch Pull", 30, false);
        addNote("Sticks", 31, false);
        addNote("Square Click", 32, false);
        addNote("Metronome Click", 33, false);
        addNote("Metronome Bell", 34, false);
        addNote("ACOUSTIC BASS DRUM", 35, true);
        addNote("BASS DRUM 1", 36, true);
        addNote("SIDE STICK", 37, true);
        addNote("ACOUSTIC SNARE", 38, true);
        addNote("HAND CLAP", 39, true);
        addNote("ELECTRIC SNARE", 40, true);
        addNote("LOW FLOOR TOM", 41, false);
        addNote("CLOSED HI HAT", 42, false);
        addNote("HIGH FLOOR TOM", 43, false);
        addNote("PEDAL HI HAT", 44, false);
        addNote("LOW TOM", 45, false);
        addNote("OPEN HI HAT", 46, false);
        addNote("LOW MID TOM", 47, false);
        addNote("HI MID TOM", 48, false);
        addNote("CRASH CYMBAL 1", 49, false);
        addNote("HIGH TOM", 50, false);
        addNote("RIDE CYMBAL 1", 51, false);
        addNote("CHINESE CYMBAL", 52, false);
        addNote("RIDE BELL", 53, false);
        addNote("TAMBOURINE", 54, false);
        addNote("SPLASH CYMBAL", 55, false);
        addNote("COWBELL", 56, false);
        addNote("CRASH CYMBAL 2", 57, false);
        addNote("VIBRASLAP", 58, false);
        addNote("RIDE CYMBAL 2", 59, false);
        addNote("HI BONGO", 60, false);
        addNote("LOW BONGO", 61, false);
        addNote("MUTE HI CONGA", 62, false);
        addNote("OPEN HI CONGA", 63, false);
        addNote("LOW CONGA", 64, false);
        addNote("HIGH TIMBALE", 65, false);
        addNote("LOW TIMBALE", 66, false);
        addNote("HIGH AGOGO", 67, false);
        addNote("LOW AGOGO", 68, false);
        addNote("CABASA", 69, false);
        addNote("MARACAS", 70, false);
        addNote("SHORT WHISTLE", 71, false);
        addNote("LONG WHISTLE", 72, false);
        addNote("SHORT GUIRO", 73, false);
        addNote("LONG GUIRO", 74, false);
        addNote("CLAVES", 75, false);
        addNote("HI WOOD SECTION", 76, false);
        addNote("LOW WOOD SECTION", 77, false);
        addNote("MUTE CUICA", 78, false);
        addNote("OPEN CUICA", 79, false);
        addNote("MUTE TRIANGLE", 80, false);
        addNote("OPEN TRIANGLE", 81, false);
        addNote("Shaker", 82, false);
        addNote("Jingle Bell", 83, false);
        addNote("Belltree", 84, false);
        addNote("Castanets", 85, false);
        addNote("Mute Surdo", 86, false);
        addNote("Open Surdo", 87, false);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean isContaining(DrumKit.KeyMap otherKeyMap)
    {
        return otherKeyMap == this || otherKeyMap == KeyMapGM.getInstance();
    }

    @Override
    public String getKeyName(int pitch)
    {
        return mapPitchName.get(pitch);
    }
    
        @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public KeyRange getRange()
    {
        return range;
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
    public int getKey(String noteName)
    {
        Integer res = mapNamePitch.get(noteName.toUpperCase());
        return res != null ? res : -1;
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
            throw new IllegalArgumentException("pitch=" + pitch + " name=" + name);
        }
        name = name.toUpperCase();
        if (mapNamePitch.get(name) != null || mapPitchName.get(pitch) != null)
        {
            throw new IllegalArgumentException("pitch=" + pitch + ", name=" + name + ": value already used");
        }
        mapNamePitch.put(name, pitch);
        mapPitchName.put(pitch, name);
        if (isAccent)
        {
            accentPitches.add(pitch);
        }
    }
}
