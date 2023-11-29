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
import org.jjazz.midi.api.DrumKit.KeyMap;
import org.jjazz.midi.api.DrumKit.Subset;

/**
 * The GS/GM2 Midi Drum Map.
 */
public class KeyMapGSGM2 implements KeyMap
{

    public static final String NAME = "GS_GM2";
    private static KeyMapGSGM2 INSTANCE;
    private final HashMap<String, Integer> mapNamePitch = new HashMap<>();
    private final HashMap<Integer, String> mapPitchName = new HashMap<>();
    private final KeyRange range = new KeyRange(22, 87);
    private final HashMap<DrumKit.Subset, List<Integer>> mapSubsetPitches = new HashMap<>();

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
        addNote("Beep 1", 22);       // SC-88 pro only
        addNote("Beep 2", 23);       // SC-88 pro only
        addNote("Concert Snare", 24, Subset.ACCENT, Subset.SNARE, Subset.SNARE_DRUM);    // SC-88 pro only
        addNote("SNARE ROLL", 25, Subset.SNARE);
        addNote("FINGER SNAP", 26);
        addNote("HIGH Q", 27);
        addNote("SLAP", 28);
        addNote("SCRATCH PUSH", 29);
        addNote("Scratch Pull", 30);
        addNote("Sticks", 31);
        addNote("Square Click", 32);
        addNote("Metronome Click", 33);
        addNote("Metronome Bell", 34);
        addNote("ACOUSTIC BASS DRUM", 35, Subset.ACCENT, Subset.BASS);
        addNote("BASS DRUM 1", 36, Subset.ACCENT, Subset.BASS, Subset.BASS_DEFAULT);
        addNote("SIDE STICK", 37, Subset.ACCENT, Subset.SNARE, Subset.SNARE_RIMSHOT);
        addNote("ACOUSTIC SNARE", 38, Subset.ACCENT, Subset.SNARE, Subset.SNARE_DRUM);
        addNote("HAND CLAP", 39, Subset.ACCENT, Subset.SNARE, Subset.SNARE_HANDCLAP);
        addNote("ELECTRIC SNARE", 40, Subset.ACCENT, Subset.SNARE, Subset.SNARE_DRUM, Subset.SNARE_DEFAULT);
        addNote("LOW FLOOR TOM", 41, Subset.TOM);
        addNote("CLOSED HI HAT", 42, Subset.HI_HAT, Subset.HI_HAT_CLOSED);
        addNote("HIGH FLOOR TOM", 43, Subset.TOM);
        addNote("PEDAL HI HAT", 44, Subset.HI_HAT_PEDAL, Subset.HI_HAT);
        addNote("LOW TOM", 45, Subset.TOM);
        addNote("OPEN HI HAT", 46, Subset.HI_HAT, Subset.HI_HAT_OPEN);
        addNote("LOW MID TOM", 47, Subset.TOM);
        addNote("HI MID TOM", 48, Subset.TOM);
        addNote("CRASH CYMBAL 1", 49, Subset.CRASH);
        addNote("HIGH TOM", 50, Subset.TOM);
        addNote("RIDE CYMBAL 1", 51, Subset.CYMBAL);
        addNote("CHINESE CYMBAL", 52, Subset.CRASH);
        addNote("RIDE BELL", 53, Subset.CYMBAL);
        addNote("TAMBOURINE", 54);
        addNote("SPLASH CYMBAL", 55, Subset.CRASH);
        addNote("COWBELL", 56);
        addNote("CRASH CYMBAL 2", 57, Subset.CRASH);
        addNote("VIBRASLAP", 58);
        addNote("RIDE CYMBAL 2", 59, Subset.CYMBAL);
        addNote("HI BONGO", 60);
        addNote("LOW BONGO", 61);
        addNote("MUTE HI CONGA", 62);
        addNote("OPEN HI CONGA", 63);
        addNote("LOW CONGA", 64);
        addNote("HIGH TIMBALE", 65);
        addNote("LOW TIMBALE", 66);
        addNote("HIGH AGOGO", 67);
        addNote("LOW AGOGO", 68);
        addNote("CABASA", 69);
        addNote("MARACAS", 70);
        addNote("SHORT WHISTLE", 71);
        addNote("LONG WHISTLE", 72);
        addNote("SHORT GUIRO", 73);
        addNote("LONG GUIRO", 74);
        addNote("CLAVES", 75);
        addNote("HI WOOD SECTION", 76);
        addNote("LOW WOOD SECTION", 77);
        addNote("MUTE CUICA", 78);
        addNote("OPEN CUICA", 79);
        addNote("MUTE TRIANGLE", 80);
        addNote("OPEN TRIANGLE", 81);
        addNote("Shaker", 82);
        addNote("Jingle Bell", 83);
        addNote("Belltree", 84);
        addNote("Castanets", 85);
        addNote("Mute Surdo", 86);
        addNote("Open Surdo", 87);
    }

    @Override
    public List<Integer> getKeys(Subset... subsets)
    {
        var res = new ArrayList<Integer>();

        for (Subset subset : subsets)
        {
            var pitches = mapSubsetPitches.get(subset);
            if (pitches != null)
            {
                res.addAll(pitches);
            }
        }

        return res;
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
    public int getKey(String noteName)
    {
        Integer res = mapNamePitch.get(noteName.toUpperCase());
        return res != null ? res : -1;
    }

    /**
     *
     * @param name
     * @param pitch
     * @param subsets If empty assign to Subset.PERCUSSION by default
     */
    private void addNote(String name, int pitch, Subset... subsets)
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
            workSubsets.add(Subset.PERCUSSION);
        }
        for (Subset subset : workSubsets)
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
