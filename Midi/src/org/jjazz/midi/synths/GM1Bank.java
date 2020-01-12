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
package org.jjazz.midi.synths;

import java.util.List;
import java.util.logging.*;
import org.jjazz.midi.AbstractInstrumentBank;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiAddress.BankSelectMethod;

/**
 * General Midi 1 bank.
 * <p>
 * Instance should be obtained from the StdSynth.
 */
public class GM1Bank extends AbstractInstrumentBank<GM1Instrument>
{

    public static final String GM1_BANKNAME = "GM Bank";
    public static final int DEFAULT_BANK_SELECT_LSB = 0;
    public static final int DEFAULT_BANK_SELECT_MSB = 0;
    public static final BankSelectMethod DEFAULT_BANK_SELECT_METHOD = BankSelectMethod.MSB_LSB;
    private static GM1Bank INSTANCE;

    /**
     * A group of 8 similar instruments.
     */
    public enum Family
    {
        Piano, Chromatic_Percussion, Organ, Guitar, Bass, Strings,
        Ensemble, Brass, Reed, Pipe, Synth_Lead, Synth_Pad, Synth_Effects,
        Ethnic, Percussive, Sound_Effects;

        @Override
        public String toString()
        {
            return this.name().replace('_', ' ');
        }

        /**
         * The ProgramChange of the first instrument which belongs to this family.
         *
         * @return
         */
        public int getFirstProgramChange()
        {
            return ordinal() * 8;
        }

    }

    private static final Logger LOGGER = Logger.getLogger(GM1Bank.class.getSimpleName());

    /**
     * Use GM1Synth to get access to the instance.
     *
     * @return
     */
    protected static GM1Bank getInstance()
    {
        synchronized (GM1Bank.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new GM1Bank();
            }
        }
        return INSTANCE;
    }

    /**
     * Default bank uses LSB=0 MSB=0 and BankSelectMethod.MSB_LSB
     */
    private GM1Bank()
    {
        // Yamaha MOTIF : GM bank is MSB=LSB=0
        super(GM1_BANKNAME, DEFAULT_BANK_SELECT_MSB, DEFAULT_BANK_SELECT_LSB, DEFAULT_BANK_SELECT_METHOD);
        addInstrument(new GM1Instrument(0, "Acoustic Piano", Family.Piano));
        addInstrument(new GM1Instrument(1, "Bright Piano", Family.Piano));
        addInstrument(new GM1Instrument(2, "Elec. Grand Piano", Family.Piano));
        addInstrument(new GM1Instrument(3, "Honkey-Tonk", Family.Piano));
        addInstrument(new GM1Instrument(4, "Electric Piano 1", Family.Piano));
        addInstrument(new GM1Instrument(5, "Electric Piano 2", Family.Piano));
        addInstrument(new GM1Instrument(6, "Harpsichord", Family.Piano));
        addInstrument(new GM1Instrument(7, "Clavinet", Family.Piano));
        addInstrument(new GM1Instrument(8, "Celesta", Family.Chromatic_Percussion));
        addInstrument(new GM1Instrument(9, "Glockenspiel", Family.Chromatic_Percussion));
        addInstrument(new GM1Instrument(10, "Music Box", Family.Chromatic_Percussion));
        addInstrument(new GM1Instrument(11, "Vibraphone", Family.Chromatic_Percussion));
        addInstrument(new GM1Instrument(12, "Marimba", Family.Chromatic_Percussion));
        addInstrument(new GM1Instrument(13, "Xylophone", Family.Chromatic_Percussion));
        addInstrument(new GM1Instrument(14, "Tubular Bells", Family.Chromatic_Percussion));
        addInstrument(new GM1Instrument(15, "Santur", Family.Chromatic_Percussion));
        addInstrument(new GM1Instrument(16, "Drawbar Organ", Family.Organ));
        addInstrument(new GM1Instrument(17, "Percussive Organ", Family.Organ));
        addInstrument(new GM1Instrument(18, "Rock Organ", Family.Organ));
        addInstrument(new GM1Instrument(19, "Church Organ", Family.Organ));
        addInstrument(new GM1Instrument(20, "Reed Organ", Family.Organ));
        addInstrument(new GM1Instrument(21, "Accordian", Family.Organ));
        addInstrument(new GM1Instrument(22, "Harmonica", Family.Organ));
        addInstrument(new GM1Instrument(23, "Bandoneon", Family.Organ));
        addInstrument(new GM1Instrument(24, "Nylon Guitar", Family.Guitar));
        addInstrument(new GM1Instrument(25, "Steel Guitar", Family.Guitar));
        addInstrument(new GM1Instrument(26, "Jazz Guitar", Family.Guitar));
        addInstrument(new GM1Instrument(27, "Clean Guitar", Family.Guitar));
        addInstrument(new GM1Instrument(28, "Muted Guitar", Family.Guitar));
        addInstrument(new GM1Instrument(29, "Overdrive Guitar", Family.Guitar));
        addInstrument(new GM1Instrument(30, "Distortion Guitar", Family.Guitar));
        addInstrument(new GM1Instrument(31, "Guitar Harmonics", Family.Guitar));
        addInstrument(new GM1Instrument(32, "Acoustic Bass", Family.Bass));
        addInstrument(new GM1Instrument(33, "Fingered Bass", Family.Bass));
        addInstrument(new GM1Instrument(34, "Picked Bass", Family.Bass));
        addInstrument(new GM1Instrument(35, "Fretless Bass", Family.Bass));
        addInstrument(new GM1Instrument(36, "Slap Bass 1", Family.Bass));
        addInstrument(new GM1Instrument(37, "Slap Bass 2", Family.Bass));
        addInstrument(new GM1Instrument(38, "Synth Bass 1", Family.Bass));
        addInstrument(new GM1Instrument(39, "Synth Bass 2", Family.Bass));
        addInstrument(new GM1Instrument(40, "Violin", Family.Strings));
        addInstrument(new GM1Instrument(41, "Viola", Family.Strings));
        addInstrument(new GM1Instrument(42, "Cello", Family.Strings));
        addInstrument(new GM1Instrument(43, "ContraBass", Family.Strings));
        addInstrument(new GM1Instrument(44, "Tremelo Strings", Family.Strings));
        addInstrument(new GM1Instrument(45, "Pizzicato Strings", Family.Strings));
        addInstrument(new GM1Instrument(46, "Orchestral Harp", Family.Strings));
        addInstrument(new GM1Instrument(47, "Timpani", Family.Strings));
        addInstrument(new GM1Instrument(48, "Strings", Family.Ensemble));
        addInstrument(new GM1Instrument(49, "Slow Strings", Family.Ensemble));
        addInstrument(new GM1Instrument(50, "Synth Strings 1", Family.Ensemble));
        addInstrument(new GM1Instrument(51, "Synth Strings 2", Family.Ensemble));
        addInstrument(new GM1Instrument(52, "Choir Ahhs", Family.Ensemble));
        addInstrument(new GM1Instrument(53, "Voice Oohs", Family.Ensemble));
        addInstrument(new GM1Instrument(54, "Synth Vox", Family.Ensemble));
        addInstrument(new GM1Instrument(55, "Orchestra Hit", Family.Ensemble));
        addInstrument(new GM1Instrument(56, "Trumpet", Family.Brass));
        addInstrument(new GM1Instrument(57, "Trombone", Family.Brass));
        addInstrument(new GM1Instrument(58, "Tuba", Family.Brass));
        addInstrument(new GM1Instrument(59, "Muted Trumpet", Family.Brass));
        addInstrument(new GM1Instrument(60, "French Horn", Family.Brass));
        addInstrument(new GM1Instrument(61, "Brass Section", Family.Brass));
        addInstrument(new GM1Instrument(62, "Synth Brass 1", Family.Brass));
        addInstrument(new GM1Instrument(63, "Synth Brass 2", Family.Brass));
        addInstrument(new GM1Instrument(64, "Soprano Sax", Family.Reed));
        addInstrument(new GM1Instrument(65, "Alto Sax", Family.Reed));
        addInstrument(new GM1Instrument(66, "Tenor Sax", Family.Reed));
        addInstrument(new GM1Instrument(67, "Baritone Sax", Family.Reed));
        addInstrument(new GM1Instrument(68, "Oboe", Family.Reed));
        addInstrument(new GM1Instrument(69, "English Horn", Family.Reed));
        addInstrument(new GM1Instrument(70, "Bassoon", Family.Reed));
        addInstrument(new GM1Instrument(71, "Clarinet", Family.Reed));
        addInstrument(new GM1Instrument(72, "Piccolo", Family.Pipe));
        addInstrument(new GM1Instrument(73, "Flute", Family.Pipe));
        addInstrument(new GM1Instrument(74, "Recorder", Family.Pipe));
        addInstrument(new GM1Instrument(75, "Pan Flute", Family.Pipe));
        addInstrument(new GM1Instrument(76, "Blown Bottle", Family.Pipe));
        addInstrument(new GM1Instrument(77, "Shakuhachi", Family.Pipe));
        addInstrument(new GM1Instrument(78, "Whistle", Family.Pipe));
        addInstrument(new GM1Instrument(79, "Ocarina", Family.Pipe));
        addInstrument(new GM1Instrument(80, "Detuned Square", Family.Synth_Lead));
        addInstrument(new GM1Instrument(81, "Detuned Sawtooth", Family.Synth_Lead));
        addInstrument(new GM1Instrument(82, "Synth Calliope", Family.Synth_Lead));
        addInstrument(new GM1Instrument(83, "Chiff Lead", Family.Synth_Lead));
        addInstrument(new GM1Instrument(84, "Charang", Family.Synth_Lead));
        addInstrument(new GM1Instrument(85, "Air Voice", Family.Synth_Lead));
        addInstrument(new GM1Instrument(86, "5th Sawtooth", Family.Synth_Lead));
        addInstrument(new GM1Instrument(87, "Bass & Lead", Family.Synth_Lead));
        addInstrument(new GM1Instrument(88, "Fantasia", Family.Synth_Pad));
        addInstrument(new GM1Instrument(89, "Warm Pad", Family.Synth_Pad));
        addInstrument(new GM1Instrument(90, "Polyphonic Synth", Family.Synth_Pad));
        addInstrument(new GM1Instrument(91, "Space Voice", Family.Synth_Pad));
        addInstrument(new GM1Instrument(92, "Bowed Glass", Family.Synth_Pad));
        addInstrument(new GM1Instrument(93, "Mettalic Pad", Family.Synth_Pad));
        addInstrument(new GM1Instrument(94, "Halo Pad", Family.Synth_Pad));
        addInstrument(new GM1Instrument(95, "Sweep Pad", Family.Synth_Pad));
        addInstrument(new GM1Instrument(96, "Ice Rain", Family.Synth_Effects));
        addInstrument(new GM1Instrument(97, "Sound Track", Family.Synth_Effects));
        addInstrument(new GM1Instrument(98, "Crystal", Family.Synth_Effects));
        addInstrument(new GM1Instrument(99, "Atmosphere", Family.Synth_Effects));
        addInstrument(new GM1Instrument(100, "Brightness", Family.Synth_Effects));
        addInstrument(new GM1Instrument(101, "Goblins", Family.Synth_Effects));
        addInstrument(new GM1Instrument(102, "Echo Drops", Family.Synth_Effects));
        addInstrument(new GM1Instrument(103, "Star Theme", Family.Synth_Effects));
        addInstrument(new GM1Instrument(104, "Sitar", Family.Ethnic));
        addInstrument(new GM1Instrument(105, "Banjo", Family.Ethnic));
        addInstrument(new GM1Instrument(106, "Shamisem", Family.Ethnic));
        addInstrument(new GM1Instrument(107, "Koto", Family.Ethnic));
        addInstrument(new GM1Instrument(108, "Kalimba", Family.Ethnic));
        addInstrument(new GM1Instrument(109, "Bagpipe", Family.Ethnic));
        addInstrument(new GM1Instrument(110, "Fiddle", Family.Ethnic));
        addInstrument(new GM1Instrument(111, "Shanai", Family.Ethnic));
        addInstrument(new GM1Instrument(112, "Tinkle Bell", Family.Percussive));
        addInstrument(new GM1Instrument(113, "Agogo", Family.Percussive));
        addInstrument(new GM1Instrument(114, "Steel Drums", Family.Percussive));
        addInstrument(new GM1Instrument(115, "Woodsection", Family.Percussive));
        addInstrument(new GM1Instrument(116, "Taiko", Family.Percussive));
        addInstrument(new GM1Instrument(117, "Melodic Tom", Family.Percussive));
        addInstrument(new GM1Instrument(118, "Synth Drum", Family.Percussive));
        addInstrument(new GM1Instrument(119, "Reverse Cymbal", Family.Percussive));
        addInstrument(new GM1Instrument(120, "Guitar Fret Noise", Family.Sound_Effects));
        addInstrument(new GM1Instrument(121, "Breath Noise", Family.Sound_Effects));
        addInstrument(new GM1Instrument(122, "Seashore", Family.Sound_Effects));
        addInstrument(new GM1Instrument(123, "Bird Tweet", Family.Sound_Effects));
        addInstrument(new GM1Instrument(124, "Telephone Ring", Family.Sound_Effects));
        addInstrument(new GM1Instrument(125, "Helicopter", Family.Sound_Effects));
        addInstrument(new GM1Instrument(126, "Applause", Family.Sound_Effects));
        addInstrument(new GM1Instrument(127, "GunShot", Family.Sound_Effects));
    }

    /**
     * Try to find a similar instrument: guess instrument type (piano, wind, bass, etc.) from the patch name and return an GM1
     * instrument for that type.
     *
     * @param name
     * @return Null if nothing found.
     */
    public GM1Instrument getSimilarInstrument(String name)
    {
        GM1Instrument ins = null;
        String s = name.toLowerCase();
        if (s.contains("rhodes") || s.matches("e[.]? *piano") || s.matches("el.*piano"))
        {
            ins = instruments.get(4);
        } else if (s.contains("piano"))
        {
            ins = instruments.get(0);
        } else if (s.contains("guit") || s.contains("gtr") || s.contains("gt."))
        {
            ins = instruments.get(26);
        } else if (s.contains("bass"))
        {
            ins = instruments.get(33);
        } else if (s.contains("org"))
        {
            ins = instruments.get(16);
        } else if (s.contains("clavi"))
        {
            ins = instruments.get(7);
        } else if (s.contains("vibraph"))
        {
            ins = instruments.get(11);
        } else if (s.contains("harmonica"))
        {
            ins = instruments.get(2);
        } else if (s.contains("string"))
        {
            ins = instruments.get(50);
        } else if (s.contains("lead"))
        {
            ins = instruments.get(83);
        } else if (s.contains("pad"))
        {
            ins = instruments.get(89);
        } else if (s.contains("brass") || s.contains("trump") || s.contains("trombo"))
        {
            ins = instruments.get(61);
        }
        return ins;
    }

    /**
     * Return a hard-coded default GM1Instrument for the specified GM family.
     * <p>
     *
     * @param f
     * @return A GM1Instrument.
     */
    public GM1Instrument getDefaultInstrument(Family f)
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);
        }
        switch (f)
        {
            case Piano:
                return instruments.get(0);       // Piano
            case Bass:
                return instruments.get(33);      // Fingered Bass
            case Guitar:
                return instruments.get(26);      // Jazz Guitar
            case Organ:
                return instruments.get(17);      // Organ 2
            case Synth_Pad:
                return instruments.get(89);      // Warm Pad
            case Ensemble:
                return instruments.get(48);      // Strings
            case Brass:
                return instruments.get(61);      // Brass 1
            case Chromatic_Percussion:
                return instruments.get(11);      // Vibraphone
            case Percussive:
                return instruments.get(114);     // Steel Drums                        
            case Reed:
                return instruments.get(66);      // Tenor Sax
            case Synth_Lead:
                return instruments.get(80);      // Square wave
            case Strings:
                return instruments.get(42);      // Cello
            case Pipe:
                return instruments.get(73);      // Flute
            case Synth_Effects:
                return instruments.get(99);      // Atmosphere
            case Ethnic:
                return instruments.get(104);     // Sitar
            case Sound_Effects:
                return instruments.get(121);     // Breath noise          
            default:
                return instruments.get(0);       // Piano         
        }
    }

    public void setBankSelectMethod(BankSelectMethod m)
    {
        defaultBsm = m;
    }

    public void setBankSelectLsb(int lsb)
    {
        if (lsb < 0 || lsb > 127)
        {
            throw new IllegalArgumentException("lsb=" + lsb);
        }
        this.defaultLsb = lsb;
    }

    public void setBankSelectMsb(int msb)
    {
        if (msb < 0 || msb > 127)
        {
            throw new IllegalArgumentException("msb=" + msb);
        }
        this.defaultMsb = msb;
    }

    /**
     * @param patchName
     * @return True if this patchName could represent a drums patch.
     */
    public static boolean couldBeDrums(String patchName)
    {
        String s = patchName.toLowerCase();
        return s.contains("drums") || s.contains("kit");
    }

    /**
     * Check if the specified bank holds GM1 instruments: check size and a few instruments patch names.
     *
     * @param bank
     * @return
     */
    public static boolean isGM1Compatible(InstrumentBank<? extends Instrument> bank)
    {
        List<? extends Instrument> instruments = bank.getInstruments();
        return instruments.size() >= 128
                && instruments.get(11).getPatchName().toLowerCase().contains("vib")
                && instruments.get(79).getPatchName().toLowerCase().contains("carin")
                && instruments.get(127).getPatchName().toLowerCase().contains("gun");
    }
}
