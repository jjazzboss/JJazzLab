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
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiAddress.BankSelectMethod;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midi.api.keymap.KeyMapGSGM2;
import org.jjazz.midi.api.keymap.KeyMapXG_PopLatin;
import org.jjazz.midi.api.keymap.KeyMapXG;

/**
 * The Yamaha XG bank.
 * <p>
 * Instance should be obtained from the StdSynth.
 */
public class XGBank extends InstrumentBank<Instrument>
{

    public static final String BANKNAME = "XG Bank";
    public static final int DEFAULT_BANK_SELECT_LSB = 0;
    public static final int DEFAULT_BANK_SELECT_MSB = 0;
    public static final BankSelectMethod DEFAULT_BANK_SELECT_METHOD = BankSelectMethod.MSB_LSB;
    private static Instrument DEFAULT_DRUMS_INSTRUMENT;
    private static XGBank INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(XGBank.class.getSimpleName());

    /**
     * Use the XGSynth to get access to the instance.
     *
     * @return
     */
    protected static XGBank getInstance()
    {
        synchronized (XGBank.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new XGBank();
            }
        }
        return INSTANCE;
    }

    private XGBank()
    {
        super(BANKNAME, DEFAULT_BANK_SELECT_MSB, DEFAULT_BANK_SELECT_LSB, DEFAULT_BANK_SELECT_METHOD);
        addInstrument(createInstrument("GrandPiano", 0, 0, 0));
        addInstrument(createInstrument("GrandPiano 2", 0, 1, 0));
        addInstrument(createInstrument("MellowGrandPiano", 0, 18, 0));
        addInstrument(createInstrument("PianoStrings", 0, 40, 0));
        addInstrument(createInstrument("Dream", 0, 41, 0));
        addInstrument(createInstrument("BrightPiano", 0, 0, 1));
        addInstrument(createInstrument("BrightPiano 2", 0, 1, 1));
        addInstrument(createInstrument("El.GrandPiano", 0, 0, 2));
        addInstrument(createInstrument("El.GrandPiano 2", 0, 1, 2));
        addInstrument(createInstrument("DetunedCP80", 0, 32, 2));
        addInstrument(createInstrument("LayeredCP1", 0, 40, 2));
        addInstrument(createInstrument("LayeredCP2", 0, 41, 2));
        addInstrument(createInstrument("Honkytonk", 0, 0, 3));
        addInstrument(createInstrument("Honkytonk 2", 0, 1, 3));
        addInstrument(createInstrument("ElectricPiano1", 0, 0, 4));
        addInstrument(createInstrument("ElectricPiano1 2", 0, 1, 4));
        addInstrument(createInstrument("MellowElectricPiano", 0, 18, 4));
        addInstrument(createInstrument("ChorusEP1", 0, 32, 4));
        addInstrument(createInstrument("HardElectricPiano", 0, 40, 4));
        addInstrument(createInstrument("VXfadeEl.Piano1", 0, 45, 4));
        addInstrument(createInstrument("60sElectricPiano1", 0, 64, 4));
        addInstrument(createInstrument("ElectricPiano2", 0, 0, 5));
        addInstrument(createInstrument("ElectricPiano2 2", 0, 1, 5));
        addInstrument(createInstrument("ChorusEP2", 0, 32, 5));
        addInstrument(createInstrument("DXEPHard", 0, 33, 5));
        addInstrument(createInstrument("DXLegend", 0, 34, 5));
        addInstrument(createInstrument("DXPhaseEP", 0, 40, 5));
        addInstrument(createInstrument("DX+AnalogEP", 0, 41, 5));
        addInstrument(createInstrument("DXKotoEP", 0, 42, 5));
        addInstrument(createInstrument("VXfadeEl.Piano2", 0, 45, 5));
        addInstrument(createInstrument("Harpsichord", 0, 0, 6));
        addInstrument(createInstrument("Harpsichord 2", 0, 1, 6));
        addInstrument(createInstrument("Harpsichord2", 0, 25, 6));
        addInstrument(createInstrument("Harpsichord3", 0, 35, 6));
        addInstrument(createInstrument("Clavi", 0, 0, 7));
        addInstrument(createInstrument("Clavi 2", 0, 1, 7));
        addInstrument(createInstrument("ClaviWah", 0, 27, 7));
        addInstrument(createInstrument("PulseClavi", 0, 64, 7));
        addInstrument(createInstrument("PierceClavi", 0, 65, 7));
        addInstrument(createInstrument("Celesta", 0, 0, 8));
        addInstrument(createInstrument("Glockenspiel", 0, 0, 9));
        addInstrument(createInstrument("MusicBox", 0, 0, 10));
        addInstrument(createInstrument("Orgel", 0, 64, 10));
        addInstrument(createInstrument("Vibraphone", 0, 0, 11));
        addInstrument(createInstrument("Vibraphone 2", 0, 1, 11));
        addInstrument(createInstrument("HardVibes", 0, 45, 11));
        addInstrument(createInstrument("Marimba", 0, 0, 12));
        addInstrument(createInstrument("Marimba 2", 0, 1, 12));
        addInstrument(createInstrument("Sine Marimba", 0, 64, 12));
        addInstrument(createInstrument("Balimba", 0, 97, 12));
        addInstrument(createInstrument("Log Drums", 0, 98, 12));
        addInstrument(createInstrument("Xylophone", 0, 0, 13));
        addInstrument(createInstrument("TubularBells", 0, 0, 14));
        addInstrument(createInstrument("ChurchBells", 0, 96, 14));
        addInstrument(createInstrument("Carillon", 0, 97, 14));
        addInstrument(createInstrument("Dulcimer", 0, 0, 15));
        addInstrument(createInstrument("Dulcimer2", 0, 35, 15));
        addInstrument(createInstrument("Cimbalom", 0, 96, 15));
        addInstrument(createInstrument("Santur", 0, 97, 15));
        addInstrument(createInstrument("DrawbarOrgan", 0, 0, 16));
        addInstrument(createInstrument("DetunedDrawOrgan", 0, 32, 16));
        addInstrument(createInstrument("60sDrawbarOrgan1", 0, 33, 16));
        addInstrument(createInstrument("60sDrawbarOrgan2", 0, 34, 16));
        addInstrument(createInstrument("70sDrawbarOrgan1", 0, 35, 16));
        addInstrument(createInstrument("DrawbarOrgan2", 0, 36, 16));
        addInstrument(createInstrument("60sDrawbarOrgan3", 0, 37, 16));
        addInstrument(createInstrument("EvenBarOrgan", 0, 38, 16));
        addInstrument(createInstrument("16+2'2_3Organ", 0, 40, 16));
        addInstrument(createInstrument("OrganBass", 0, 64, 16));
        addInstrument(createInstrument("70sDrawbarOrgan2", 0, 65, 16));
        addInstrument(createInstrument("CheezyOrgan", 0, 66, 16));
        addInstrument(createInstrument("DrawbarOrgan3*", 0, 67, 16));
        addInstrument(createInstrument("PercussiveOrgan", 0, 0, 17));
        addInstrument(createInstrument("70sPercussiveOrgan1", 0, 24, 17));
        addInstrument(createInstrument("DetunedPercOrgan", 0, 32, 17));
        addInstrument(createInstrument("LightOrgan", 0, 33, 17));
        addInstrument(createInstrument("PercussiveOrgan2", 0, 37, 17));
        addInstrument(createInstrument("Rock Organ", 0, 0, 18));
        addInstrument(createInstrument("Rotary Organ", 0, 64, 18));
        addInstrument(createInstrument("Slow Rotary", 0, 65, 18));
        addInstrument(createInstrument("Fast Rotary", 0, 66, 18));
        addInstrument(createInstrument("ChurchOrgan", 0, 0, 19));
        addInstrument(createInstrument("ChurchOrgan3", 0, 32, 19));
        addInstrument(createInstrument("ChurchOrgan2", 0, 35, 19));
        addInstrument(createInstrument("NotreDame", 0, 40, 19));
        addInstrument(createInstrument("OrganFlute", 0, 64, 19));
        addInstrument(createInstrument("TremoloOrganFlute", 0, 65, 19));
        addInstrument(createInstrument("ReedOrgan", 0, 0, 20));
        addInstrument(createInstrument("PuffOrgan", 0, 40, 20));
        addInstrument(createInstrument("Accordion", 0, 0, 21));
        addInstrument(createInstrument("Accordion Italian", 0, 32, 21));
        addInstrument(createInstrument("Harmonica", 0, 0, 22));
        addInstrument(createInstrument("Harmonica2", 0, 32, 22));
        addInstrument(createInstrument("TangoAccordion", 0, 0, 23));
        addInstrument(createInstrument("TangoAccordion2", 0, 64, 23));
        addInstrument(createInstrument("NylonGuitar", 0, 0, 24));
        addInstrument(createInstrument("NylonGuitar2", 0, 16, 24));
        addInstrument(createInstrument("NylonGuitar3", 0, 25, 24));
        addInstrument(createInstrument("VelocityGtrHarmoics", 0, 43, 24));
        addInstrument(createInstrument("Ukulele", 0, 96, 24));
        addInstrument(createInstrument("SteelGuitar", 0, 0, 25));
        addInstrument(createInstrument("SteelGuitar2", 0, 16, 25));
        addInstrument(createInstrument("12StrGuitar", 0, 35, 25));
        addInstrument(createInstrument("Nylon&Steel", 0, 40, 25));
        addInstrument(createInstrument("Steel&Body", 0, 41, 25));
        addInstrument(createInstrument("Mandolin", 0, 96, 25));
        addInstrument(createInstrument("JazzGuitar", 0, 0, 26));
        addInstrument(createInstrument("MellowGuitar", 0, 18, 26));
        addInstrument(createInstrument("JazzAmp", 0, 32, 26));
        addInstrument(createInstrument("CleanGuitar", 0, 0, 27));
        addInstrument(createInstrument("ChorusGuitar", 0, 32, 27));
        addInstrument(createInstrument("MutedGuitar", 0, 0, 28));
        addInstrument(createInstrument("FunkGuitar1", 0, 40, 28));
        addInstrument(createInstrument("MuteSteelGuitar", 0, 41, 28));
        addInstrument(createInstrument("FunkGuitar2", 0, 43, 28));
        addInstrument(createInstrument("JazzMan", 0, 45, 28));
        addInstrument(createInstrument("Overdriven", 0, 0, 29));
        addInstrument(createInstrument("GuitarPinch", 0, 43, 29));
        addInstrument(createInstrument("DistortionGuitar", 0, 0, 30));
        addInstrument(createInstrument("FeedbackGuitar", 0, 40, 30));
        addInstrument(createInstrument("FeedbackGuitar2", 0, 41, 30));
        addInstrument(createInstrument("GuitarHarmonics", 0, 0, 31));
        addInstrument(createInstrument("GuitarFeedback", 0, 65, 31));
        addInstrument(createInstrument("GuitarHarmonics2", 0, 66, 31));
        addInstrument(createInstrument("AcousticBass", 0, 0, 32));
        addInstrument(createInstrument("JazzRhythm", 0, 40, 32));
        addInstrument(createInstrument("VXUprghtBass", 0, 45, 32));
        addInstrument(createInstrument("FingerBass", 0, 0, 33));
        addInstrument(createInstrument("FingerBassDark", 0, 18, 33));
        addInstrument(createInstrument("FlangeBass", 0, 27, 33));
        addInstrument(createInstrument("Bass&DistortionEG", 0, 40, 33));
        addInstrument(createInstrument("FingerSlapBass", 0, 43, 33));
        addInstrument(createInstrument("FingerBass2", 0, 45, 33));
        addInstrument(createInstrument("ModulatedBass", 0, 65, 33));
        addInstrument(createInstrument("PickBass", 0, 0, 34));
        addInstrument(createInstrument("MutePickBass", 0, 28, 34));
        addInstrument(createInstrument("FretlessBass", 0, 0, 35));
        addInstrument(createInstrument("FretlessBass2", 0, 32, 35));
        addInstrument(createInstrument("FretlessBass3", 0, 33, 35));
        addInstrument(createInstrument("FretlessBass4", 0, 34, 35));
        addInstrument(createInstrument("SynthFretless", 0, 96, 35));
        addInstrument(createInstrument("SmoothFretless", 0, 97, 35));
        addInstrument(createInstrument("SlapBass1", 0, 0, 36));
        addInstrument(createInstrument("PunchThumb", 0, 27, 36));
        addInstrument(createInstrument("ResonantSlap", 0, 32, 36));
        addInstrument(createInstrument("SlapBass2", 0, 0, 37));
        addInstrument(createInstrument("VelocitySwitchSlap", 0, 43, 37));
        addInstrument(createInstrument("SynthBass1", 0, 0, 38));
        addInstrument(createInstrument("SynthBass1 Dark", 0, 18, 38));
        addInstrument(createInstrument("FastResonantBass", 0, 20, 38));
        addInstrument(createInstrument("AcidBass", 0, 24, 38));
        addInstrument(createInstrument("ClaviBass", 0, 35, 38));
        addInstrument(createInstrument("TechnoBass", 0, 40, 38));
        addInstrument(createInstrument("Orbiter", 0, 64, 38));
        addInstrument(createInstrument("SquareBass", 0, 65, 38));
        addInstrument(createInstrument("RubberBass", 0, 66, 38));
        addInstrument(createInstrument("Hammer", 0, 96, 38));
        addInstrument(createInstrument("SynthBass2", 0, 0, 39));
        addInstrument(createInstrument("MellowSynthBass", 0, 6, 39));
        addInstrument(createInstrument("SequenceBass", 0, 12, 39));
        addInstrument(createInstrument("ClickSynthBass", 0, 18, 39));
        addInstrument(createInstrument("SynthBass2 Dark", 0, 19, 39));
        addInstrument(createInstrument("SmoothSynthBass", 0, 32, 39));
        addInstrument(createInstrument("ModulrSynthBass", 0, 40, 39));
        addInstrument(createInstrument("DXBass", 0, 41, 39));
        addInstrument(createInstrument("XWireBass", 0, 64, 39));
        addInstrument(createInstrument("Violin", 0, 0, 40));
        addInstrument(createInstrument("SlowAttackViolin", 0, 8, 40));
        addInstrument(createInstrument("Viola", 0, 0, 41));
        addInstrument(createInstrument("Cello", 0, 0, 42));
        addInstrument(createInstrument("Contrabass", 0, 0, 43));
        addInstrument(createInstrument("TremoloStrings", 0, 0, 44));
        addInstrument(createInstrument("SlwAtkTremStrings", 0, 8, 44));
        addInstrument(createInstrument("SuspenseStrings", 0, 40, 44));
        addInstrument(createInstrument("PizzicatoStrings", 0, 0, 45));
        addInstrument(createInstrument("OrchestralHarp", 0, 0, 46));
        addInstrument(createInstrument("YangQin", 0, 40, 46));
        addInstrument(createInstrument("Timpani", 0, 0, 47));
        addInstrument(createInstrument("Strings1", 0, 0, 48));
        addInstrument(createInstrument("StereoStrings", 0, 3, 48));
        addInstrument(createInstrument("SlwAttackStrings", 0, 8, 48));
        addInstrument(createInstrument("ArcoStrings", 0, 24, 48));
        addInstrument(createInstrument("60sStrings", 0, 35, 48));
        addInstrument(createInstrument("Orchestra", 0, 40, 48));
        addInstrument(createInstrument("Orchestra2", 0, 41, 48));
        addInstrument(createInstrument("TremoloOrchstra", 0, 42, 48));
        addInstrument(createInstrument("VelocityStrings", 0, 45, 48));
        addInstrument(createInstrument("Strings2", 0, 0, 49));
        addInstrument(createInstrument("StereoSlowStrings", 0, 3, 49));
        addInstrument(createInstrument("LegatoStrings", 0, 8, 49));
        addInstrument(createInstrument("WarmStrings", 0, 40, 49));
        addInstrument(createInstrument("Kingdom", 0, 41, 49));
        addInstrument(createInstrument("70sStrings", 0, 64, 49));
        addInstrument(createInstrument("Strings3", 0, 65, 49));
        addInstrument(createInstrument("SynthStrings1", 0, 0, 50));
        addInstrument(createInstrument("ResonantStrings", 0, 27, 50));
        addInstrument(createInstrument("SynthStrings4", 0, 64, 50));
        addInstrument(createInstrument("SynthStrings5", 0, 65, 50));
        addInstrument(createInstrument("SynthStrings2", 0, 0, 51));
        addInstrument(createInstrument("ChoirAahs", 0, 0, 52));
        addInstrument(createInstrument("StereoChoir", 0, 3, 52));
        addInstrument(createInstrument("ChoirAahs2", 0, 16, 52));
        addInstrument(createInstrument("MellowChoir", 0, 32, 52));
        addInstrument(createInstrument("ChoirStrings", 0, 40, 52));
        addInstrument(createInstrument("VoiceOohs", 0, 0, 53));
        addInstrument(createInstrument("SynthVoice", 0, 0, 54));
        addInstrument(createInstrument("SynthVoice2", 0, 40, 54));
        addInstrument(createInstrument("Choral", 0, 41, 54));
        addInstrument(createInstrument("AnalogVoice", 0, 64, 54));
        addInstrument(createInstrument("OrchestraHit", 0, 0, 55));
        addInstrument(createInstrument("OrchestraHit2", 0, 35, 55));
        addInstrument(createInstrument("Impact", 0, 64, 55));
        addInstrument(createInstrument("Trumpet", 0, 0, 56));
        addInstrument(createInstrument("Trumpet2", 0, 16, 56));
        addInstrument(createInstrument("BrightTrumpet", 0, 17, 56));
        addInstrument(createInstrument("WarmTrumpet", 0, 32, 56));
        addInstrument(createInstrument("Trombone", 0, 0, 57));
        addInstrument(createInstrument("Trombone2", 0, 18, 57));
        addInstrument(createInstrument("Tuba", 0, 0, 58));
        addInstrument(createInstrument("Tuba2", 0, 16, 58));
        addInstrument(createInstrument("MutedTrumpet", 0, 0, 59));
        addInstrument(createInstrument("FrenchHorn", 0, 0, 60));
        addInstrument(createInstrument("FrenchHornSolo", 0, 6, 60));
        addInstrument(createInstrument("FrenchHorn2", 0, 32, 60));
        addInstrument(createInstrument("HornOrchestra", 0, 37, 60));
        addInstrument(createInstrument("BrassSection", 0, 0, 61));
        addInstrument(createInstrument("Trp&TrbSection", 0, 35, 61));
        addInstrument(createInstrument("BrassSection2", 0, 40, 61));
        addInstrument(createInstrument("HighBrass", 0, 41, 61));
        addInstrument(createInstrument("MellowBrass", 0, 42, 61));
        addInstrument(createInstrument("SynthBrass1", 0, 0, 62));
        addInstrument(createInstrument("QuackBrass", 0, 12, 62));
        addInstrument(createInstrument("ResoSynthBrass", 0, 20, 62));
        addInstrument(createInstrument("PolyBrass", 0, 24, 62));
        addInstrument(createInstrument("SynthBrass3", 0, 27, 62));
        addInstrument(createInstrument("JumpBrass", 0, 32, 62));
        addInstrument(createInstrument("AnalogVeloBrass1", 0, 45, 62));
        addInstrument(createInstrument("AnalogBrass1", 0, 64, 62));
        addInstrument(createInstrument("SynthBrass2", 0, 0, 63));
        addInstrument(createInstrument("SoftBrass", 0, 18, 63));
        addInstrument(createInstrument("SynthBrass4", 0, 40, 63));
        addInstrument(createInstrument("ChoirBrass", 0, 41, 63));
        addInstrument(createInstrument("AnalogVeloBrass2", 0, 45, 63));
        addInstrument(createInstrument("AnalogBrass2", 0, 64, 63));
        addInstrument(createInstrument("SopranoSax", 0, 0, 64));
        addInstrument(createInstrument("AltoSax", 0, 0, 65));
        addInstrument(createInstrument("SaxSection", 0, 40, 65));
        addInstrument(createInstrument("HyperAltoSax", 0, 43, 65));
        addInstrument(createInstrument("TenorSax", 0, 0, 66));
        addInstrument(createInstrument("BreathyTenor", 0, 40, 66));
        addInstrument(createInstrument("SoftTenorSax", 0, 41, 66));
        addInstrument(createInstrument("TenorSax2", 0, 64, 66));
        addInstrument(createInstrument("BaritoneSax", 0, 0, 67));
        addInstrument(createInstrument("Oboe", 0, 0, 68));
        addInstrument(createInstrument("EnglishHorn", 0, 0, 69));
        addInstrument(createInstrument("Bassoon", 0, 0, 70));
        addInstrument(createInstrument("Clarinet", 0, 0, 71));
        addInstrument(createInstrument("Piccolo", 0, 0, 72));
        addInstrument(createInstrument("Flute", 0, 0, 73));
        addInstrument(createInstrument("Recorder", 0, 0, 74));
        addInstrument(createInstrument("Pan Flute", 0, 0, 75));
        addInstrument(createInstrument("Blown Bottle", 0, 0, 76));
        addInstrument(createInstrument("Shakuhachi", 0, 0, 77));
        addInstrument(createInstrument("Whistle", 0, 0, 78));
        addInstrument(createInstrument("Ocarina", 0, 0, 79));
        addInstrument(createInstrument("SquareLead", 0, 0, 80));
        addInstrument(createInstrument("SquareLead2", 0, 6, 80));
        addInstrument(createInstrument("LMSquare", 0, 8, 80));
        addInstrument(createInstrument("Hollow", 0, 18, 80));
        addInstrument(createInstrument("Shroud", 0, 19, 80));
        addInstrument(createInstrument("Mellow", 0, 64, 80));
        addInstrument(createInstrument("SoloSine", 0, 65, 80));
        addInstrument(createInstrument("SineLead", 0, 66, 80));
        addInstrument(createInstrument("SawtoothLead", 0, 0, 81));
        addInstrument(createInstrument("SawtoothLead2", 0, 6, 81));
        addInstrument(createInstrument("ThickSaw", 0, 8, 81));
        addInstrument(createInstrument("DynamicSaw", 0, 18, 81));
        addInstrument(createInstrument("DigitalSaw", 0, 19, 81));
        addInstrument(createInstrument("BigLead", 0, 20, 81));
        addInstrument(createInstrument("HeavySynth", 0, 24, 81));
        addInstrument(createInstrument("WaspySynth", 0, 25, 81));
        addInstrument(createInstrument("PulseSaw", 0, 40, 81));
        addInstrument(createInstrument("Dr.Lead", 0, 41, 81));
        addInstrument(createInstrument("VelocityLead", 0, 45, 81));
        addInstrument(createInstrument("SequencedAnalog", 0, 96, 81));
        addInstrument(createInstrument("CalliopeLead", 0, 0, 82));
        addInstrument(createInstrument("PureLead", 0, 65, 82));
        addInstrument(createInstrument("ChiffLead", 0, 0, 83));
        addInstrument(createInstrument("Rubby", 0, 64, 83));
        addInstrument(createInstrument("CharangLead", 0, 0, 84));
        addInstrument(createInstrument("DistortedLead", 0, 64, 84));
        addInstrument(createInstrument("WireLead", 0, 65, 84));
        addInstrument(createInstrument("VoiceLead", 0, 0, 85));
        addInstrument(createInstrument("SynthAahs", 0, 24, 85));
        addInstrument(createInstrument("VoxLead", 0, 64, 85));
        addInstrument(createInstrument("FifthsLead", 0, 0, 86));
        addInstrument(createInstrument("BigFive", 0, 35, 86));
        addInstrument(createInstrument("Bass&Lead", 0, 0, 87));
        addInstrument(createInstrument("Big&Low", 0, 16, 87));
        addInstrument(createInstrument("Fat&Perky", 0, 64, 87));
        addInstrument(createInstrument("SoftWhirl", 0, 65, 87));
        addInstrument(createInstrument("NewAgePad", 0, 0, 88));
        addInstrument(createInstrument("Fantasy", 0, 64, 88));
        addInstrument(createInstrument("WarmPad", 0, 0, 89));
        addInstrument(createInstrument("ThickPad", 0, 16, 89));
        addInstrument(createInstrument("SoftPad", 0, 17, 89));
        addInstrument(createInstrument("SinePad", 0, 18, 89));
        addInstrument(createInstrument("HornPad", 0, 64, 89));
        addInstrument(createInstrument("RotaryStrings", 0, 65, 89));
        addInstrument(createInstrument("PolySynthPad", 0, 0, 90));
        addInstrument(createInstrument("PolyPad80", 0, 64, 90));
        addInstrument(createInstrument("ClickPad", 0, 65, 90));
        addInstrument(createInstrument("AnalogPad", 0, 66, 90));
        addInstrument(createInstrument("SquarePad", 0, 67, 90));
        addInstrument(createInstrument("ChoirPad", 0, 0, 91));
        addInstrument(createInstrument("Heaven", 0, 64, 91));
        addInstrument(createInstrument("Itopia", 0, 66, 91));
        addInstrument(createInstrument("CCPad", 0, 67, 91));
        addInstrument(createInstrument("BowedPad", 0, 0, 92));
        addInstrument(createInstrument("Glacier", 0, 64, 92));
        addInstrument(createInstrument("GlassPad", 0, 65, 92));
        addInstrument(createInstrument("MetallicPad", 0, 0, 93));
        addInstrument(createInstrument("TinePad", 0, 64, 93));
        addInstrument(createInstrument("PanPad", 0, 65, 93));
        addInstrument(createInstrument("HaloPad", 0, 0, 94));
        addInstrument(createInstrument("SweepPad", 0, 0, 95));
        addInstrument(createInstrument("Shwimmer", 0, 20, 95));
        addInstrument(createInstrument("Converge", 0, 27, 95));
        addInstrument(createInstrument("PolarPad", 0, 64, 95));
        addInstrument(createInstrument("Celestial", 0, 66, 95));
        addInstrument(createInstrument("Rain", 0, 0, 96));
        addInstrument(createInstrument("ClaviPad", 0, 45, 96));
        addInstrument(createInstrument("HarmoRain", 0, 64, 96));
        addInstrument(createInstrument("AfricanWind", 0, 65, 96));
        addInstrument(createInstrument("Carib", 0, 66, 96));
        addInstrument(createInstrument("SoundTrack", 0, 0, 97));
        addInstrument(createInstrument("Prologue", 0, 27, 97));
        addInstrument(createInstrument("Ancestral", 0, 64, 97));
        addInstrument(createInstrument("Crystal", 0, 0, 98));
        addInstrument(createInstrument("SynthDrumComp", 0, 12, 98));
        addInstrument(createInstrument("Popcorn", 0, 14, 98));
        addInstrument(createInstrument("TinyBells", 0, 18, 98));
        addInstrument(createInstrument("RoundGlocken", 0, 35, 98));
        addInstrument(createInstrument("GlockenChime", 0, 40, 98));
        addInstrument(createInstrument("ClearBells", 0, 41, 98));
        addInstrument(createInstrument("ChorusBells", 0, 42, 98));
        addInstrument(createInstrument("SynthMallet", 0, 64, 98));
        addInstrument(createInstrument("SoftCrystal", 0, 65, 98));
        addInstrument(createInstrument("LoudGlocken", 0, 66, 98));
        addInstrument(createInstrument("ChristmasBells", 0, 67, 98));
        addInstrument(createInstrument("VibraphoneBells", 0, 68, 98));
        addInstrument(createInstrument("DigitalBells", 0, 69, 98));
        addInstrument(createInstrument("AirBells", 0, 70, 98));
        addInstrument(createInstrument("BellHarp", 0, 71, 98));
        addInstrument(createInstrument("Gamelimba", 0, 72, 98));
        addInstrument(createInstrument("Atmosphere", 0, 0, 99));
        addInstrument(createInstrument("WarmAtmosphere", 0, 18, 99));
        addInstrument(createInstrument("HollwRelease", 0, 19, 99));
        addInstrument(createInstrument("NylonElectricPiano", 0, 40, 99));
        addInstrument(createInstrument("NylonHarp", 0, 64, 99));
        addInstrument(createInstrument("HarpVox", 0, 65, 99));
        addInstrument(createInstrument("AtmospherePad", 0, 66, 99));
        addInstrument(createInstrument("Planet", 0, 67, 99));
        addInstrument(createInstrument("Brightness", 0, 0, 100));
        addInstrument(createInstrument("FantasyBells", 0, 64, 100));
        addInstrument(createInstrument("Smokey", 0, 96, 100));
        addInstrument(createInstrument("Goblins", 0, 0, 101));
        addInstrument(createInstrument("GoblinsSynth", 0, 64, 101));
        addInstrument(createInstrument("Creeper", 0, 65, 101));
        addInstrument(createInstrument("RingPad", 0, 66, 101));
        addInstrument(createInstrument("Ritual", 0, 67, 101));
        addInstrument(createInstrument("ToHeaven", 0, 68, 101));
        addInstrument(createInstrument("Night", 0, 70, 101));
        addInstrument(createInstrument("Glisten", 0, 71, 101));
        addInstrument(createInstrument("BellChoir", 0, 96, 101));
        addInstrument(createInstrument("Echoes", 0, 0, 102));
        addInstrument(createInstrument("Echoes2", 0, 8, 102));
        addInstrument(createInstrument("EchoPan", 0, 14, 102));
        addInstrument(createInstrument("EchoBells", 0, 64, 102));
        addInstrument(createInstrument("BigPan", 0, 65, 102));
        addInstrument(createInstrument("SynthPiano", 0, 66, 102));
        addInstrument(createInstrument("Creation", 0, 67, 102));
        addInstrument(createInstrument("Stardust", 0, 68, 102));
        addInstrument(createInstrument("Resonant&Panning", 0, 69, 102));
        addInstrument(createInstrument("ScienceFiction", 0, 0, 103));
        addInstrument(createInstrument("Starz", 0, 64, 103));
        addInstrument(createInstrument("Sitar", 0, 0, 104));
        addInstrument(createInstrument("DetunedSitar", 0, 32, 104));
        addInstrument(createInstrument("Sitar2", 0, 35, 104));
        addInstrument(createInstrument("Tambra", 0, 96, 104));
        addInstrument(createInstrument("Tamboura", 0, 97, 104));
        addInstrument(createInstrument("Banjo", 0, 0, 105));
        addInstrument(createInstrument("MutedBanjo", 0, 28, 105));
        addInstrument(createInstrument("Rabab", 0, 96, 105));
        addInstrument(createInstrument("Gopichant", 0, 97, 105));
        addInstrument(createInstrument("Oud", 0, 98, 105));
        addInstrument(createInstrument("Shamisen", 0, 0, 106));
        addInstrument(createInstrument("Koto", 0, 0, 107));
        addInstrument(createInstrument("Taisho,kin", 0, 96, 107));
        addInstrument(createInstrument("Kanoon", 0, 97, 107));
        addInstrument(createInstrument("Kalimba", 0, 0, 108));
        addInstrument(createInstrument("Bagpipe", 0, 0, 109));
        addInstrument(createInstrument("Fiddle", 0, 0, 110));
        addInstrument(createInstrument("Shanai", 0, 0, 111));
        addInstrument(createInstrument("Shanai2", 0, 64, 111));
        addInstrument(createInstrument("Pungi", 0, 96, 111));
        addInstrument(createInstrument("Hichiriki", 0, 97, 111));
        addInstrument(createInstrument("TinkleBell", 0, 0, 112));
        addInstrument(createInstrument("Bonang", 0, 96, 112));
        addInstrument(createInstrument("Altair", 0, 97, 112));
        addInstrument(createInstrument("GamelanGongs", 0, 98, 112));
        addInstrument(createInstrument("StereoGamlan", 0, 99, 112));
        addInstrument(createInstrument("RamaCymbal", 0, 100, 112));
        addInstrument(createInstrument("AsianBells", 0, 101, 112));
        addInstrument(createInstrument("Agogo", 0, 0, 113));
        addInstrument(createInstrument("SteelDrums", 0, 0, 114));
        addInstrument(createInstrument("GlassPercussion", 0, 97, 114));
        addInstrument(createInstrument("ThaiBells", 0, 98, 114));
        addInstrument(createInstrument("Woodblock", 0, 0, 115));
        addInstrument(createInstrument("Castanets", 0, 96, 115));
        addInstrument(createInstrument("Taiko Drum", 0, 0, 116));
        addInstrument(createInstrument("GranCassa", 0, 96, 116));
        addInstrument(createInstrument("MelodicTom", 0, 0, 117));
        addInstrument(createInstrument("MelodicTom2", 0, 64, 117));
        addInstrument(createInstrument("RealTom", 0, 65, 117));
        addInstrument(createInstrument("RockTom", 0, 66, 117));
        addInstrument(createInstrument("SynthDrum", 0, 0, 118));
        addInstrument(createInstrument("AnalogTom", 0, 64, 118));
        addInstrument(createInstrument("ElectroPercussion", 0, 65, 118));
        addInstrument(createInstrument("ReverseCymbal", 0, 0, 119));
        addInstrument(createInstrument("Guitar Fret Noise", 0, 0, 120));
        addInstrument(createInstrument("Breath Noise", 0, 0, 121));
        addInstrument(createInstrument("Seashore", 0, 0, 122));
        addInstrument(createInstrument("Bird Tweet", 0, 0, 123));
        addInstrument(createInstrument("Telephone Ring", 0, 0, 124));
        addInstrument(createInstrument("Helicopter", 0, 0, 125));
        addInstrument(createInstrument("Applause", 0, 0, 126));
        addInstrument(createInstrument("Gunshot", 0, 0, 127));
        addInstrument(createInstrument("CuttingNoise", 64, 0, 0));
        addInstrument(createInstrument("CuttingNoise2", 64, 0, 1));
        addInstrument(createInstrument("StringSlap", 64, 0, 3));
        addInstrument(createInstrument("FluteKeyClick", 64, 0, 16));
        addInstrument(createInstrument("Shower", 64, 0, 32));
        addInstrument(createInstrument("Thunder", 64, 0, 33));
        addInstrument(createInstrument("Wind", 64, 0, 34));
        addInstrument(createInstrument("Stream", 64, 0, 35));
        addInstrument(createInstrument("Bubble", 64, 0, 36));
        addInstrument(createInstrument("Feed", 64, 0, 37));
        addInstrument(createInstrument("Dog", 64, 0, 48));
        addInstrument(createInstrument("Horse", 64, 0, 49));
        addInstrument(createInstrument("BirdTweet2", 64, 0, 50));
        addInstrument(createInstrument("Ghost", 64, 0, 54));
        addInstrument(createInstrument("Maou", 64, 0, 55));
        addInstrument(createInstrument("PhoneCall", 64, 0, 64));
        addInstrument(createInstrument("DoorSqueak", 64, 0, 65));
        addInstrument(createInstrument("DoorSlam", 64, 0, 66));
        addInstrument(createInstrument("ScratchCut", 64, 0, 67));
        addInstrument(createInstrument("ScratchSplit", 64, 0, 68));
        addInstrument(createInstrument("WindChime", 64, 0, 69));
        addInstrument(createInstrument("TelephoneRing2", 64, 0, 70));
        addInstrument(createInstrument("CarEngineIgn", 64, 0, 80));
        addInstrument(createInstrument("CarTiresSqel", 64, 0, 81));
        addInstrument(createInstrument("CarPassing", 64, 0, 82));
        addInstrument(createInstrument("CarCrash", 64, 0, 83));
        addInstrument(createInstrument("Siren", 64, 0, 84));
        addInstrument(createInstrument("Train", 64, 0, 85));
        addInstrument(createInstrument("JetPlane", 64, 0, 86));
        addInstrument(createInstrument("Starship", 64, 0, 87));
        addInstrument(createInstrument("Burst", 64, 0, 88));
        addInstrument(createInstrument("RollerCoaster", 64, 0, 89));
        addInstrument(createInstrument("Submarine", 64, 0, 90));
        addInstrument(createInstrument("Laugh", 64, 0, 96));
        addInstrument(createInstrument("Scream", 64, 0, 97));
        addInstrument(createInstrument("Punch", 64, 0, 98));
        addInstrument(createInstrument("Heartbeat", 64, 0, 99));
        addInstrument(createInstrument("FootSteps", 64, 0, 100));
        addInstrument(createInstrument("MachineGun", 64, 0, 112));
        addInstrument(createInstrument("LaserGun", 64, 0, 113));
        addInstrument(createInstrument("Explosion", 64, 0, 114));
        addInstrument(createInstrument("Firework", 64, 0, 115));

        DEFAULT_DRUMS_INSTRUMENT = createDrumsInstrument("Std1 Kit", 127, 0, DrumKit.Type.STANDARD, KeyMapXG.getInstance());
        addInstrument(DEFAULT_DRUMS_INSTRUMENT);
        addInstrument(createDrumsInstrument("Std2 Kit", 127, 1, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Dry Kit", 127, 2, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Bright Kit", 127, 3, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Skim Kit", 127, 4, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Slim Kit", 127, 5, DrumKit.Type.ROOM, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Rogue Kit", 127, 6, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Hob Kit", 127, 7, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Room Kit", 127, 8, DrumKit.Type.ROOM, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Dark Kit", 127, 9, DrumKit.Type.ROOM, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Rock_Old Kit", 127, 16, DrumKit.Type.POWER, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Rock_Old2 Kit", 127, 17, DrumKit.Type.POWER, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Electro Kit", 127, 24, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Analog Kit", 127, 25, DrumKit.Type.ANALOG, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Analog2 Kit", 127, 26, DrumKit.Type.ANALOG, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Dance Kit", 127, 27, DrumKit.Type.ELECTRONIC, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Hiphop Kit", 127, 28, DrumKit.Type.ELECTRONIC, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Jungle Kit", 127, 29, DrumKit.Type.ELECTRONIC, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Apogee Kit", 127, 30, DrumKit.Type.ELECTRONIC, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Perigee Kit", 127, 31, DrumKit.Type.ELECTRONIC, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Jazz Kit", 127, 32, DrumKit.Type.JAZZ, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Jazz2 Kit", 127, 33, DrumKit.Type.JAZZ, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Brush Kit", 127, 40, DrumKit.Type.BRUSH, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Real_Brush Kit", 127, 41, DrumKit.Type.BRUSH, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Symphony Kit", 127, 48, DrumKit.Type.ORCHESTRA, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("HipHop2 Kit", 127, 56, DrumKit.Type.ELECTRONIC, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Break Kit", 127, 57, DrumKit.Type.ELECTRONIC, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Tramp Kit", 127, 64, DrumKit.Type.ROOM, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Amber Kit", 127, 65, DrumKit.Type.ROOM, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Coffin Kit", 127, 66, DrumKit.Type.ROOM, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Live_Std Kit", 127, 80, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Live_Funk Kit", 127, 81, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Live_Brush Kit", 127, 82, DrumKit.Type.BRUSH, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Live_Std_Perc Kit", 127, 83, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Live_Funk_Perc Kit", 127, 84, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Live_Brush_Perc Kit", 127, 85, DrumKit.Type.BRUSH, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("SFX1 Kit", 126, 0, DrumKit.Type.SFX, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("SFX2 Kit", 126, 1, DrumKit.Type.SFX, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Techno_KS Kit", 126, 16, DrumKit.Type.ELECTRONIC, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Techno_HI Kit", 126, 17, DrumKit.Type.ELECTRONIC, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Techno_LO Kit", 126, 18, DrumKit.Type.ELECTRONIC, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Sakura Kit", 126, 32, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Small_Latin Kit", 126, 33, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("China Kit", 126, 34, DrumKit.Type.STANDARD, KeyMapXG.getInstance()));
        addInstrument(createDrumsInstrument("Cuban Kit", 126, 40, DrumKit.Type.STANDARD, KeyMapXG_PopLatin.getInstance()));
        addInstrument(createDrumsInstrument("Cuban2 Kit", 126, 41, DrumKit.Type.STANDARD, KeyMapXG_PopLatin.getInstance()));
        addInstrument(createDrumsInstrument("Brazilian Kit", 126, 42, DrumKit.Type.STANDARD, KeyMapXG_PopLatin.getInstance()));
        addInstrument(createDrumsInstrument("PopLatin1 Kit", 126, 43, DrumKit.Type.STANDARD, KeyMapXG_PopLatin.getInstance()));
        addInstrument(createDrumsInstrument("PopLatin2 Kit", 126, 44, DrumKit.Type.STANDARD, KeyMapXG_PopLatin.getInstance()));
    }

    public Instrument getDefaultDrumsInstrument()
    {
        return DEFAULT_DRUMS_INSTRUMENT;
    }

    /**
     * Overridden to accept any GM-compatible keymaps when trying harder.
     *
     * @param kit
     * @param tryHarder
     * @return
     */
    @Override
    public List<Instrument> getDrumsInstruments(DrumKit kit, boolean tryHarder)
    {
        List<Instrument> res = super.getDrumsInstruments(kit, tryHarder);
        if (res.isEmpty() && tryHarder
                && (kit.getKeyMap().equals(KeyMapGM.getInstance()) || kit.getKeyMap().equals(KeyMapGSGM2.getInstance())))
        {
            // GM is fully compatible, XG is somewhat compatible...
            switch (kit.getType())
            {
                case STANDARD:
                    res.add(instruments.get(480));
                    break;
                case ROOM:
                    res.add(instruments.get(488));
                    break;
                case POWER:
                    res.add(instruments.get(490));
                    break;
                case ANALOG:
                    res.add(instruments.get(493));
                    break;
                case ELECTRONIC:
                    res.add(instruments.get(495));
                    break;
                case JAZZ:
                    res.add(instruments.get(500));
                    break;
                case BRUSH:
                    res.add(instruments.get(502));
                    break;
                case ORCHESTRA:
                    res.add(instruments.get(504));
                    break;
                case SFX:
                    res.add(instruments.get(516));
                    break;
                default:
                    throw new IllegalStateException("kit=" + kit);   
            }
        }
        return res;
    }

    /**
     * Convenience method to reorder arguments.
     *
     * @return
     */
    private static Instrument createInstrument(String name, int msb, int lsb, int pc)
    {
        GM1Instrument gmIns = GM1Bank.getInstance().getInstrument(pc); // XG's PC is directly compatible with GM1
        Instrument ins = new Instrument(name, null, new MidiAddress(pc, msb, lsb, DEFAULT_BANK_SELECT_METHOD), null, gmIns);
        return ins;
    }

    /**
     * Convenience method to reorder arguments.
     *
     * @return
     */
    private static Instrument createDrumsInstrument(String name, int msb, int pc, DrumKit.Type t, DrumKit.KeyMap map)
    {
        return new Instrument(name, null, new MidiAddress(pc, msb, 0, DEFAULT_BANK_SELECT_METHOD), new DrumKit(t, map), null);
    }
}
