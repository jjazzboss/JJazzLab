/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midi.api.synths;

/**
 * A family of similar instruments.
 * <p>
 * Based on the GM1 Standard: the values respect the order of the GM1 instruments, each family has 8 instruments in the GM1 bank.
 */
public enum InstrumentFamily
{
    Piano("piano"), Chromatic_Percussion("cperc"), Organ("orgn"), Guitar("guit"), Bass("bass"), Strings("violn"), Ensemble("strgs"), Brass("brass"), Reed("reed"), Pipe(
            "wind"), Synth_Lead("lead"), Synth_Pad("pad"), Synth_Effects("synfx"), Ethnic("ethnc"), Percussive("perc"), Sound_Effects("sndfx");

    private final String shortName;

    private InstrumentFamily(String shortName)
    {
        this.shortName = shortName;
    }

    /**
     * A 5 chars max. string.
     *
     * @return
     */
    public String getShortName()
    {
        return this.shortName;
    }

    /**
     * Get an appropriate absolute pitch corresponding to relPitch for this instrument family.
     * <p>
     * E.g. for a BASS instrument pick the lowest note from E1.
     *
     * @param relPitch
     * @return
     */
    public int toAbsolutePitch(int relPitch)
    {
         int octave = switch (this)
        {
            case Bass ->
                relPitch >= 4 ? 3 : 4; // from E1(28) to D#2(39)
            default ->
                5;
        };
        int res = (octave - 1) * 12 + relPitch;
        return res;
    }
    

    @Override
    public String toString()
    {
        return this.name().replace('_', ' ');
    }

    /**
     * The GM1 ProgramChange of the first instrument which belongs to this family.
     *
     * @return
     */
    public int getFirstProgramChange()
    {
        return ordinal() * 8;
    }

    /**
     * Try to guess the family from patchName.
     * <p>
     * E.g. if patchName contains "piano", family is Piano.
     *
     * @param patchName
     * @return
     */
    static public InstrumentFamily guessFamily(String patchName)
    {
        patchName = patchName.toLowerCase();
        if (patchName.contains("pn:")
                || patchName.contains("pia")
                || patchName.contains("harpsi")
                || patchName.contains("clavi")
                || patchName.matches("cp[ 789]"))
        {
            return Piano;
        } else if ((patchName.contains("vib") && !patchName.contains("slap"))
                || patchName.contains("celes")
                || patchName.contains("marim"))
        {
            return Chromatic_Percussion;
        }
        return null;
    }

    /**
     * Try to guess if patchName represents a drums/percussio patch.
     *
     * @param patchName
     * @return
     */
    public static boolean couldBeDrums(String patchName)
    {
        String s = patchName.toLowerCase();
        return (s.contains("drums") && !s.contains("steel")) || s.contains("kit") || s.contains("dr:") || s.contains("drm");
    }

}
