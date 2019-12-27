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
package org.jjazz.midi.drumkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The General Midi Drum Kit.
 */
public class DrumKitGM implements DrumKit
{

    public static final String NAME = "GM Drum Kit";
    private static DrumKitGM INSTANCE;
    private HashMap<String, Integer> mapNamePitch = new HashMap<>();
    private HashMap<Integer, String> mapPitchName = new HashMap<>();
    private ArrayList<Integer> accentPitches = new ArrayList<>();

    public static DrumKitGM getInstance()
    {
        synchronized (DrumKitGM.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DrumKitGM();
            }
        }
        return INSTANCE;
    }

    private DrumKitGM()
    {
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
        addNote("OPEN HI HAT", 46, true);
        addNote("LOW MID TOM", 47, false);
        addNote("HI MID TOM", 48, false);
        addNote("CRASH CYMBAL 1", 49, true);
        addNote("HIGH TOM", 50, false);
        addNote("RIDE CYMBAL 1", 51, false);
        addNote("CHINESE CYMBAL", 52, true);
        addNote("RIDE BELL", 53, false);
        addNote("TAMBOURINE", 54, false);
        addNote("SPLASH CYMBAL", 55, true);
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
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getNoteName(int pitch)
    {
        return mapPitchName.get(pitch);
    }

    @Override
    public int getNotePitch(String noteName)
    {
        Integer res = mapNamePitch.get(noteName.toUpperCase());
        return res != null ? res : -1;
    }

    @Override
    public List<Integer> getAccentNotes()
    {
        return new ArrayList<>(accentPitches);
    }

    private void addNote(String name, int pitch, boolean isAccent)
    {
        if (pitch < 0 || pitch > 127 || name == null || name.isBlank())
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
