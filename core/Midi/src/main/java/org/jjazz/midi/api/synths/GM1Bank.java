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
package org.jjazz.midi.api.synths;

import java.util.List;
import java.util.logging.*;
import java.util.regex.Pattern;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.MidiAddress.BankSelectMethod;
import org.jjazz.midi.api.MidiUtilities;

/**
 * General Midi 1 bank.
 * <p>
 * Instance should be obtained from the StdSynth.
 */
public class GM1Bank extends InstrumentBank<GM1Instrument>
{

    public static final String GM1_BANKNAME = "GM Bank";
    public static final int DEFAULT_BANK_SELECT_LSB = 0;
    public static final int DEFAULT_BANK_SELECT_MSB = 0;
    public static final BankSelectMethod DEFAULT_BANK_SELECT_METHOD = BankSelectMethod.PC_ONLY;
    private static GM1Bank INSTANCE;
    private static Patterns pInst = null;
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
        addInstrument(new GM1Instrument(0, "Acoustic Piano", InstrumentFamily.Piano));
        addInstrument(new GM1Instrument(1, "Bright Piano", InstrumentFamily.Piano));
        addInstrument(new GM1Instrument(2, "Elec. Grand Piano", InstrumentFamily.Piano));
        addInstrument(new GM1Instrument(3, "Honkey-Tonk", InstrumentFamily.Piano));
        addInstrument(new GM1Instrument(4, "Electric Piano 1", InstrumentFamily.Piano));
        addInstrument(new GM1Instrument(5, "Electric Piano 2", InstrumentFamily.Piano));
        addInstrument(new GM1Instrument(6, "Harpsichord", InstrumentFamily.Piano));
        addInstrument(new GM1Instrument(7, "Clavinet", InstrumentFamily.Piano));
        addInstrument(new GM1Instrument(8, "Celesta", InstrumentFamily.Chromatic_Percussion));
        addInstrument(new GM1Instrument(9, "Glockenspiel", InstrumentFamily.Chromatic_Percussion));
        addInstrument(new GM1Instrument(10, "Music Box", InstrumentFamily.Chromatic_Percussion));
        addInstrument(new GM1Instrument(11, "Vibraphone", InstrumentFamily.Chromatic_Percussion));
        addInstrument(new GM1Instrument(12, "Marimba", InstrumentFamily.Chromatic_Percussion));
        addInstrument(new GM1Instrument(13, "Xylophone", InstrumentFamily.Chromatic_Percussion));
        addInstrument(new GM1Instrument(14, "Tubular Bells", InstrumentFamily.Chromatic_Percussion));
        addInstrument(new GM1Instrument(15, "Santur", InstrumentFamily.Chromatic_Percussion));
        addInstrument(new GM1Instrument(16, "Drawbar Organ", InstrumentFamily.Organ));
        addInstrument(new GM1Instrument(17, "Percussive Organ", InstrumentFamily.Organ));
        addInstrument(new GM1Instrument(18, "Rock Organ", InstrumentFamily.Organ));
        addInstrument(new GM1Instrument(19, "Church Organ", InstrumentFamily.Organ));
        addInstrument(new GM1Instrument(20, "Reed Organ", InstrumentFamily.Organ));
        addInstrument(new GM1Instrument(21, "Accordian", InstrumentFamily.Organ));
        addInstrument(new GM1Instrument(22, "Harmonica", InstrumentFamily.Organ));
        addInstrument(new GM1Instrument(23, "Bandoneon", InstrumentFamily.Organ));
        addInstrument(new GM1Instrument(24, "Nylon Guitar", InstrumentFamily.Guitar));
        addInstrument(new GM1Instrument(25, "Steel Guitar", InstrumentFamily.Guitar));
        addInstrument(new GM1Instrument(26, "Jazz Guitar", InstrumentFamily.Guitar));
        addInstrument(new GM1Instrument(27, "Clean Guitar", InstrumentFamily.Guitar));
        addInstrument(new GM1Instrument(28, "Muted Guitar", InstrumentFamily.Guitar));
        addInstrument(new GM1Instrument(29, "Overdrive Guitar", InstrumentFamily.Guitar));
        addInstrument(new GM1Instrument(30, "Distortion Guitar", InstrumentFamily.Guitar));
        addInstrument(new GM1Instrument(31, "Guitar Harmonics", InstrumentFamily.Guitar));
        addInstrument(new GM1Instrument(32, "Acoustic Bass", InstrumentFamily.Bass));
        addInstrument(new GM1Instrument(33, "Fingered Bass", InstrumentFamily.Bass));
        addInstrument(new GM1Instrument(34, "Picked Bass", InstrumentFamily.Bass));
        addInstrument(new GM1Instrument(35, "Fretless Bass", InstrumentFamily.Bass));
        addInstrument(new GM1Instrument(36, "Slap Bass 1", InstrumentFamily.Bass));
        addInstrument(new GM1Instrument(37, "Slap Bass 2", InstrumentFamily.Bass));
        addInstrument(new GM1Instrument(38, "Synth Bass 1", InstrumentFamily.Bass));
        addInstrument(new GM1Instrument(39, "Synth Bass 2", InstrumentFamily.Bass));
        addInstrument(new GM1Instrument(40, "Violin", InstrumentFamily.Strings));
        addInstrument(new GM1Instrument(41, "Viola", InstrumentFamily.Strings));
        addInstrument(new GM1Instrument(42, "Cello", InstrumentFamily.Strings));
        addInstrument(new GM1Instrument(43, "ContraBass", InstrumentFamily.Strings));
        addInstrument(new GM1Instrument(44, "Tremelo Strings", InstrumentFamily.Strings));
        addInstrument(new GM1Instrument(45, "Pizzicato Strings", InstrumentFamily.Strings));
        addInstrument(new GM1Instrument(46, "Orchestral Harp", InstrumentFamily.Strings));
        addInstrument(new GM1Instrument(47, "Timpani", InstrumentFamily.Strings));
        addInstrument(new GM1Instrument(48, "Strings", InstrumentFamily.Ensemble));
        addInstrument(new GM1Instrument(49, "Slow Strings", InstrumentFamily.Ensemble));
        addInstrument(new GM1Instrument(50, "Synth Strings 1", InstrumentFamily.Ensemble));
        addInstrument(new GM1Instrument(51, "Synth Strings 2", InstrumentFamily.Ensemble));
        addInstrument(new GM1Instrument(52, "Choir Ahhs", InstrumentFamily.Ensemble));
        addInstrument(new GM1Instrument(53, "Voice Oohs", InstrumentFamily.Ensemble));
        addInstrument(new GM1Instrument(54, "Synth Vox", InstrumentFamily.Ensemble));
        addInstrument(new GM1Instrument(55, "Orchestra Hit", InstrumentFamily.Ensemble));
        addInstrument(new GM1Instrument(56, "Trumpet", InstrumentFamily.Brass));
        addInstrument(new GM1Instrument(57, "Trombone", InstrumentFamily.Brass));
        addInstrument(new GM1Instrument(58, "Tuba", InstrumentFamily.Brass));
        addInstrument(new GM1Instrument(59, "Muted Trumpet", InstrumentFamily.Brass));
        addInstrument(new GM1Instrument(60, "French Horn", InstrumentFamily.Brass));
        addInstrument(new GM1Instrument(61, "Brass Section", InstrumentFamily.Brass));
        addInstrument(new GM1Instrument(62, "Synth Brass 1", InstrumentFamily.Brass));
        addInstrument(new GM1Instrument(63, "Synth Brass 2", InstrumentFamily.Brass));
        addInstrument(new GM1Instrument(64, "Soprano Sax", InstrumentFamily.Reed));
        addInstrument(new GM1Instrument(65, "Alto Sax", InstrumentFamily.Reed));
        addInstrument(new GM1Instrument(66, "Tenor Sax", InstrumentFamily.Reed));
        addInstrument(new GM1Instrument(67, "Baritone Sax", InstrumentFamily.Reed));
        addInstrument(new GM1Instrument(68, "Oboe", InstrumentFamily.Reed));
        addInstrument(new GM1Instrument(69, "English Horn", InstrumentFamily.Reed));
        addInstrument(new GM1Instrument(70, "Bassoon", InstrumentFamily.Reed));
        addInstrument(new GM1Instrument(71, "Clarinet", InstrumentFamily.Reed));
        addInstrument(new GM1Instrument(72, "Piccolo", InstrumentFamily.Pipe));
        addInstrument(new GM1Instrument(73, "Flute", InstrumentFamily.Pipe));
        addInstrument(new GM1Instrument(74, "Recorder", InstrumentFamily.Pipe));
        addInstrument(new GM1Instrument(75, "Pan Flute", InstrumentFamily.Pipe));
        addInstrument(new GM1Instrument(76, "Blown Bottle", InstrumentFamily.Pipe));
        addInstrument(new GM1Instrument(77, "Shakuhachi", InstrumentFamily.Pipe));
        addInstrument(new GM1Instrument(78, "Whistle", InstrumentFamily.Pipe));
        addInstrument(new GM1Instrument(79, "Ocarina", InstrumentFamily.Pipe));
        addInstrument(new GM1Instrument(80, "Detuned Square", InstrumentFamily.Synth_Lead));
        addInstrument(new GM1Instrument(81, "Detuned Sawtooth", InstrumentFamily.Synth_Lead));
        addInstrument(new GM1Instrument(82, "Synth Calliope", InstrumentFamily.Synth_Lead));
        addInstrument(new GM1Instrument(83, "Chiff Lead", InstrumentFamily.Synth_Lead));
        addInstrument(new GM1Instrument(84, "Charang", InstrumentFamily.Synth_Lead));
        addInstrument(new GM1Instrument(85, "Air Voice", InstrumentFamily.Synth_Lead));
        addInstrument(new GM1Instrument(86, "5th Sawtooth", InstrumentFamily.Synth_Lead));
        addInstrument(new GM1Instrument(87, "Bass & Lead", InstrumentFamily.Synth_Lead));
        addInstrument(new GM1Instrument(88, "Fantasia", InstrumentFamily.Synth_Pad));
        addInstrument(new GM1Instrument(89, "Warm Pad", InstrumentFamily.Synth_Pad));
        addInstrument(new GM1Instrument(90, "Polyphonic Synth", InstrumentFamily.Synth_Pad));
        addInstrument(new GM1Instrument(91, "Space Voice", InstrumentFamily.Synth_Pad));
        addInstrument(new GM1Instrument(92, "Bowed Glass", InstrumentFamily.Synth_Pad));
        addInstrument(new GM1Instrument(93, "Mettalic Pad", InstrumentFamily.Synth_Pad));
        addInstrument(new GM1Instrument(94, "Halo Pad", InstrumentFamily.Synth_Pad));
        addInstrument(new GM1Instrument(95, "Sweep Pad", InstrumentFamily.Synth_Pad));
        addInstrument(new GM1Instrument(96, "Ice Rain", InstrumentFamily.Synth_Effects));
        addInstrument(new GM1Instrument(97, "Sound Track", InstrumentFamily.Synth_Effects));
        addInstrument(new GM1Instrument(98, "Crystal", InstrumentFamily.Synth_Effects));
        addInstrument(new GM1Instrument(99, "Atmosphere", InstrumentFamily.Synth_Effects));
        addInstrument(new GM1Instrument(100, "Brightness", InstrumentFamily.Synth_Effects));
        addInstrument(new GM1Instrument(101, "Goblins", InstrumentFamily.Synth_Effects));
        addInstrument(new GM1Instrument(102, "Echo Drops", InstrumentFamily.Synth_Effects));
        addInstrument(new GM1Instrument(103, "Star Theme", InstrumentFamily.Synth_Effects));
        addInstrument(new GM1Instrument(104, "Sitar", InstrumentFamily.Ethnic));
        addInstrument(new GM1Instrument(105, "Banjo", InstrumentFamily.Ethnic));
        addInstrument(new GM1Instrument(106, "Shamisem", InstrumentFamily.Ethnic));
        addInstrument(new GM1Instrument(107, "Koto", InstrumentFamily.Ethnic));
        addInstrument(new GM1Instrument(108, "Kalimba", InstrumentFamily.Ethnic));
        addInstrument(new GM1Instrument(109, "Bagpipe", InstrumentFamily.Ethnic));
        addInstrument(new GM1Instrument(110, "Fiddle", InstrumentFamily.Ethnic));
        addInstrument(new GM1Instrument(111, "Shanai", InstrumentFamily.Ethnic));
        addInstrument(new GM1Instrument(112, "Tinkle Bell", InstrumentFamily.Percussive));
        addInstrument(new GM1Instrument(113, "Agogo", InstrumentFamily.Percussive));
        addInstrument(new GM1Instrument(114, "Steel Drums", InstrumentFamily.Percussive));
        addInstrument(new GM1Instrument(115, "Woodsection", InstrumentFamily.Percussive));
        addInstrument(new GM1Instrument(116, "Taiko", InstrumentFamily.Percussive));
        addInstrument(new GM1Instrument(117, "Melodic Tom", InstrumentFamily.Percussive));
        addInstrument(new GM1Instrument(118, "Synth Drum", InstrumentFamily.Percussive));
        addInstrument(new GM1Instrument(119, "Reverse Cymbal", InstrumentFamily.Percussive));
        addInstrument(new GM1Instrument(120, "Guitar Fret Noise", InstrumentFamily.Sound_Effects));
        addInstrument(new GM1Instrument(121, "Breath Noise", InstrumentFamily.Sound_Effects));
        addInstrument(new GM1Instrument(122, "Seashore", InstrumentFamily.Sound_Effects));
        addInstrument(new GM1Instrument(123, "Bird Tweet", InstrumentFamily.Sound_Effects));
        addInstrument(new GM1Instrument(124, "Telephone Ring", InstrumentFamily.Sound_Effects));
        addInstrument(new GM1Instrument(125, "Helicopter", InstrumentFamily.Sound_Effects));
        addInstrument(new GM1Instrument(126, "Applause", InstrumentFamily.Sound_Effects));
        addInstrument(new GM1Instrument(127, "GunShot", InstrumentFamily.Sound_Effects));
    }

    /**
     * Store the guessInstruments compiled patterns.
     */
    class Patterns
    {

        Pattern CpPiano = Pattern.compile("cp[ 789]|elec.*gran");
    }

    /**
     * Try to guess from patchName the equivalent GM1 Instrument.
     *
     *
     * @param patchName
     * @return Null if nothing found.
     */
    public GM1Instrument guessInstrument(String patchName)
    {
        if (patchName == null)
        {
            throw new NullPointerException("patchName");
        }
        if (patchName.trim().isEmpty())
        {
            return null;
        }
        if (pInst == null)
        {
            pInst = new Patterns();
        }
        GM1Instrument ins = null;
        String s = patchName.trim().toLowerCase();

        if (MidiUtilities.guessIsPatchNameDrums(s))
        {
            return null;
        }

        // PIANOS 0-5 
        if (s.contains("ap:") || s.contains("pn:") || s.contains("pno") || s.contains("pian"))
        {
            if (s.contains("bri") || s.contains("rock") || s.contains("atta") || s.contains("tacky") || s.contains("danc") || s.contains("hous"))
            {
                ins = instruments.get(1);
            }
            else if (pInst.CpPiano.matcher(s).find())
            {
                ins = instruments.get(2);
            }
            else if (s.contains("honk") || s.contains("saloo"))
            {
                ins = instruments.get(3);
            }
            else if (s.contains("elec") || s.contains("elp") || s.contains("el.") || s.contains("e."))
            {
                ins = instruments.get(4);
            }
            else
            {
                ins = instruments.get(0);
            }
        }
        else if (patchName.contains("EP ") || patchName.contains("EP.") || patchName.contains("DX") || s.contains("wurli") || s.contains("kb:") || s.contains("kbd:"))
        {
            ins = instruments.get(4);
        }
        else if (s.contains("harpsi"))
        {
            ins = instruments.get(6);
        }
        else if (s.contains("clavi"))
        {
            ins = instruments.get(7);
        }
        else if (s.contains("celest"))
        {
            ins = instruments.get(8);
        }
        else if (s.contains("glock"))
        {
            ins = instruments.get(9);
        }
        else if (s.contains("music") && s.contains("box"))
        {
            ins = instruments.get(10);
        }
        else if (s.contains("vibrap") || s.contains("vibes"))
        {
            ins = instruments.get(11);
        }
        else if (s.contains("marimb"))
        {
            ins = instruments.get(12);
        }
        else if (s.contains("xylop"))
        {
            ins = instruments.get(13);
        }
        else if (s.contains("tubul"))
        {
            ins = instruments.get(14);
        }
        else if (s.contains("dulci") || s.contains("santur"))
        {
            ins = instruments.get(15);
        } // ORGANS 16-20
        else if (s.contains("or:") || s.contains("org:") || s.replaceFirst("korg", "").contains("org"))
        {
            if (s.contains("perc") || s.contains("hammo") || s.contains("jazz") || s.contains("chorus"))
            {
                ins = instruments.get(17);
            }
            else if (s.contains("rock") || s.contains("rotar"))
            {
                ins = instruments.get(18);
            }
            else if (s.contains("chur") || s.contains("cath") || s.contains("pipe"))
            {
                ins = instruments.get(19);
            }
            else if (s.contains("reed"))
            {
                ins = instruments.get(20);
            }
            else
            {
                ins = instruments.get(16);
            }
        }
        else if (s.contains("accord   "))
        {
            ins = s.contains("tango") ? instruments.get(23) : instruments.get(21);
        }
        else if (s.contains("harmonic"))
        {
            ins = instruments.get(22);
        } // GUITARS 24-31
        else if (s.contains("guit") || s.contains("gtr") || s.contains("gt.") || s.contains("gt:"))
        {
            if (s.contains("steel"))
            {
                ins = instruments.get(25);
            }
            else if (s.contains("nylon") || s.contains("acoust") || s.contains("a."))
            {
                ins = instruments.get(24);
            }
            else if (s.contains("clean"))
            {
                ins = instruments.get(27);
            }
            else if (s.contains("mute"))
            {
                ins = instruments.get(28);
            }
            else if (s.contains("overd"))
            {
                ins = instruments.get(29);
            }
            else if (s.contains("dist") || s.contains("feedb") || s.contains("lead"))
            {
                ins = instruments.get(30);
            }
            else if (s.contains("harm"))
            {
                ins = instruments.get(31);
            }
            else
            {
                ins = instruments.get(26);
            }
        } // BASSES 32-39
        else if (!(s.contains("lead") || (s.contains("contra") || s.contains("bassoo")))
            && (s.contains("bass") || s.contains("ba:") || s.contains("bs:") || s.contains("bas:")))
        {
            if (s.contains("wood") || s.contains("ac"))
            {
                ins = instruments.get(32);
            }
            else if (s.contains("pick"))
            {
                ins = instruments.get(34);
            }
            else if (s.contains("fretl"))
            {
                ins = instruments.get(35);
            }
            else if (s.contains("slap"))
            {
                ins = instruments.get(36);
            }
            else if (s.contains("syn"))
            {
                ins = instruments.get(38);
            }
            else
            {
                ins = instruments.get(33);
            }
        }
        else if (s.contains("viola"))
        {
            ins = instruments.get(41);
        }
        else if (s.contains("cello"))
        {
            ins = instruments.get(42);
        }
        else if (s.contains("contra"))
        {
            ins = instruments.get(43);
        }
        else if (s.contains("pizz"))
        {
            ins = instruments.get(45);
        }
        else if (s.contains("harp"))
        {
            ins = instruments.get(46);
        }// STRINGS 48-55        
        else if (s.contains("st:") || s.contains("str:") || s.contains("string") || s.contains("str.") || s.contains("strng"))
        {
            if (s.contains("syn"))
            {
                ins = instruments.get(50);
            }
            else
            {
                ins = instruments.get(48);
            }
        }
        else if (!s.contains("pad")
            && s.contains("choir"))
        {
            ins = instruments.get(52);
        }
        else if (s.contains("orch") && s.contains("hit"))
        {
            ins = instruments.get(55);
        } // BRASS 56-63
        else if (s.contains("tuba"))
        {
            ins = instruments.get(58);
        }
        else if (s.contains("br:") || s.contains("bra:") || s.contains("brass") || s.contains("trump") || s.contains("trp") || s.contains("tromb") || s.contains("horn"))
        {
            if (s.contains("ens") || s.contains("sect") || s.contains("trumpets") || s.contains("horns"))
            {
                ins = s.contains("syn") ? instruments.get(62) : instruments.get(61);
            }
            else if (s.contains("muted") || s.contains("mtd"))
            {
                ins = instruments.get(59);
            }
            else if (s.contains("trumpet"))
            {
                ins = instruments.get(56);
            }
            else if (s.contains("trombone"))
            {
                ins = instruments.get(57);
            }
            else if (s.contains("horn"))
            {
                ins = instruments.get(60);
            }
            else
            {
                ins = instruments.get(63);
            }
        } // SAXES
        else if (s.contains("sax"))
        {
            if (s.contains("ens") || s.contains("sect") || s.contains("saxes"))
            {
                instruments.get(61);
            }
            else if (s.contains("sop"))
            {
                ins = instruments.get(64);
            }
            else if (s.contains("ten"))
            {
                ins = instruments.get(66);
            }
            else if (s.contains("bari"))
            {
                ins = instruments.get(67);
            }
            else
            {
                ins = instruments.get(65);
            }
        }
        else if (s.contains("oboe"))
        {
            ins = instruments.get(68);
        }
        else if (s.contains("basson"))
        {
            ins = instruments.get(70);
        }
        else if (s.contains("clarin"))
        {
            ins = instruments.get(71);
        }
        else if (s.contains("picco"))
        {
            ins = instruments.get(72);
        }
        else if (s.contains("flute"))
        {
            ins = s.contains("pan") ? instruments.get(75) : instruments.get(73);
        }
        else if (s.contains("recorder"))
        {
            ins = instruments.get(74);
        }
        else if (s.contains("bottl"))
        {
            ins = instruments.get(76);
        }
        else if (s.contains("shaku"))
        {
            ins = instruments.get(77);
        }
        else if (s.contains("whistl"))
        {
            ins = instruments.get(78);
        }
        else if (s.contains("ocarina"))
        {
            ins = instruments.get(79);
        } // LEAD 80-87
        else if (s.contains("lead") || s.contains("ld"))
        {
            if (s.contains("voice"))
            {
                ins = instruments.get(85);
            }
            else if (s.contains("saw"))
            {
                ins = instruments.get(81);
            }
            else if (s.contains("fifth") || s.contains("5th"))
            {
                ins = instruments.get(86);
            }
            else if (s.contains("bass"))
            {
                ins = instruments.get(87);
            }
            else
            {
                ins = instruments.get(80);
            }
        } // SYNTH PAD 88-95
        else if (s.contains("pad") || s.contains("pd:"))
        {
            if (s.contains("syn"))
            {
                ins = instruments.get(90);
            }
            else if (s.contains("saw"))
            {
                ins = instruments.get(81);
            }
            else if (s.contains("choir"))
            {
                ins = instruments.get(91);
            }
            else if (s.contains("sweep"))
            {
                ins = instruments.get(95);
            }
            else
            {
                ins = instruments.get(89);
            }
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
    public GM1Instrument getDefaultInstrument(InstrumentFamily f)
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
