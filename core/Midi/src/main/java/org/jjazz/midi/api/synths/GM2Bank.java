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

/**
 * General Midi 2 bank. Instance should be obtained from the StdSynth.
 */
public class GM2Bank extends InstrumentBank<Instrument>
{

    public static final String GM2_BANKNAME = "GM2 Bank";
    public static final int DEFAULT_BANK_SELECT_LSB = 0;
    public static final int DEFAULT_BANK_SELECT_MSB = 121;
    public static final BankSelectMethod DEFAULT_BANK_SELECT_METHOD = BankSelectMethod.MSB_LSB;
    private static Instrument DEFAULT_DRUMS_INSTRUMENT;
    private static GM2Bank INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(GM2Bank.class.getSimpleName());

    /**
     * Use GM2Synth to get access to the instance.
     *
     * @return
     */
    protected static GM2Bank getInstance()
    {
        synchronized (GM2Bank.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new GM2Bank();
            }
        }
        return INSTANCE;
    }

    private GM2Bank()
    {
        super(GM2_BANKNAME, DEFAULT_BANK_SELECT_MSB, DEFAULT_BANK_SELECT_LSB, DEFAULT_BANK_SELECT_METHOD);
        addInstrument(createInstrument(0, 121, 0, "Piano 1"));
        addInstrument(createInstrument(0, 121, 1, "Piano 1 w"));
        addInstrument(createInstrument(0, 121, 2, "European Pf"));
        addInstrument(createInstrument(1, 121, 0, "Piano 2"));
        addInstrument(createInstrument(1, 121, 1, "Piano 2 w"));
        addInstrument(createInstrument(2, 121, 0, "Piano 3"));
        addInstrument(createInstrument(2, 121, 1, "Piano 3 w"));
        addInstrument(createInstrument(3, 121, 0, "Honky-tonk"));
        addInstrument(createInstrument(3, 121, 1, "Honky-tonk w"));
        addInstrument(createInstrument(4, 121, 0, "E Piano 1"));
        addInstrument(createInstrument(4, 121, 1, "St Soft EP"));
        addInstrument(createInstrument(4, 121, 2, "EP Legend 1"));
        addInstrument(createInstrument(4, 121, 3, "Wurly"));
        addInstrument(createInstrument(5, 121, 0, "E Piano 2"));
        addInstrument(createInstrument(5, 121, 1, "Detuned EP"));
        addInstrument(createInstrument(5, 121, 2, "St FM EP"));
        addInstrument(createInstrument(5, 121, 3, "EP Legend 2"));
        addInstrument(createInstrument(5, 121, 4, "EP Phase"));
        addInstrument(createInstrument(6, 121, 0, "Harpsichord"));
        addInstrument(createInstrument(6, 121, 1, "Coupled Hps"));
        addInstrument(createInstrument(6, 121, 2, "Harpsi w"));
        addInstrument(createInstrument(6, 121, 3, "Harpsi o"));
        addInstrument(createInstrument(7, 121, 0, "Clav"));
        addInstrument(createInstrument(7, 121, 1, "Pulse Clav"));
        addInstrument(createInstrument(8, 121, 0, "Celesta"));
        addInstrument(createInstrument(9, 121, 0, "Glockenspiel"));
        addInstrument(createInstrument(10, 121, 0, "Music Box"));
        addInstrument(createInstrument(11, 121, 0, "Vibraphone"));
        addInstrument(createInstrument(11, 121, 1, "Vibraphone w"));
        addInstrument(createInstrument(12, 121, 0, "Marimba"));
        addInstrument(createInstrument(12, 121, 1, "Marimba w"));
        addInstrument(createInstrument(13, 121, 0, "Xylophone"));
        addInstrument(createInstrument(14, 121, 0, "TubularBells"));
        addInstrument(createInstrument(14, 121, 1, "Church Bell"));
        addInstrument(createInstrument(14, 121, 2, "Carillon"));
        addInstrument(createInstrument(15, 121, 0, "Santur"));
        addInstrument(createInstrument(16, 121, 0, "Organ 1"));
        addInstrument(createInstrument(16, 121, 1, "Trem Organ"));
        addInstrument(createInstrument(16, 121, 2, "60's Organ"));
        addInstrument(createInstrument(16, 121, 3, "70's E Organ"));
        addInstrument(createInstrument(17, 121, 0, "Organ 2"));
        addInstrument(createInstrument(17, 121, 1, "Chorus Organ"));
        addInstrument(createInstrument(17, 121, 2, "Perc Organ"));
        addInstrument(createInstrument(18, 121, 0, "Organ 3"));
        addInstrument(createInstrument(19, 121, 0, "Church Org 1"));
        addInstrument(createInstrument(19, 121, 1, "Church Org 2"));
        addInstrument(createInstrument(19, 121, 2, "Church Org 3"));
        addInstrument(createInstrument(20, 121, 0, "Reed Organ"));
        addInstrument(createInstrument(20, 121, 1, "Puff Organ"));
        addInstrument(createInstrument(21, 121, 0, "Accordion Fr"));
        addInstrument(createInstrument(21, 121, 1, "Accordion It"));
        addInstrument(createInstrument(22, 121, 0, "Harmonica"));
        addInstrument(createInstrument(23, 121, 0, "Bandoneon"));
        addInstrument(createInstrument(24, 121, 0, "Nylon Gtr 1"));
        addInstrument(createInstrument(24, 121, 1, "Ukulele"));
        addInstrument(createInstrument(24, 121, 2, "Nylon Gtr 1o"));
        addInstrument(createInstrument(24, 121, 3, "Nylon Gtr 2"));
        addInstrument(createInstrument(25, 121, 0, "Steel-str Gt"));
        addInstrument(createInstrument(25, 121, 1, "12-str Gtr"));
        addInstrument(createInstrument(25, 121, 2, "Mandolin"));
        addInstrument(createInstrument(25, 121, 3, "Steel + Body"));
        addInstrument(createInstrument(26, 121, 0, "Jazz Guitar"));
        addInstrument(createInstrument(26, 121, 1, "Pedal Steel"));
        addInstrument(createInstrument(27, 121, 0, "Clean Guitar"));
        addInstrument(createInstrument(27, 121, 1, "Chorus Gtr"));
        addInstrument(createInstrument(27, 121, 2, "Mid Tone Gtr"));
        addInstrument(createInstrument(28, 121, 0, "Muted Guitar"));
        addInstrument(createInstrument(28, 121, 1, "Funk Pop"));
        addInstrument(createInstrument(28, 121, 2, "Funk Guitar"));
        addInstrument(createInstrument(28, 121, 3, "Jazz Man"));
        addInstrument(createInstrument(29, 121, 0, "Overdrive Gtr"));
        addInstrument(createInstrument(29, 121, 1, "Guitar Pinch"));
        addInstrument(createInstrument(30, 121, 0, "DistortionGt"));
        addInstrument(createInstrument(30, 121, 1, "Gt Feedback1"));
        addInstrument(createInstrument(30, 121, 2, "Dist Rhythm Gtr"));
        addInstrument(createInstrument(31, 121, 0, "Gt Harmonics"));
        addInstrument(createInstrument(31, 121, 1, "Gt Feedback2"));
        addInstrument(createInstrument(32, 121, 0, "Acoustic Bs"));
        addInstrument(createInstrument(33, 121, 0, "Fingered Bs"));
        addInstrument(createInstrument(33, 121, 1, "Finger Slap"));
        addInstrument(createInstrument(34, 121, 0, "Picked Bass"));
        addInstrument(createInstrument(35, 121, 0, "Fretless Bs"));
        addInstrument(createInstrument(36, 121, 0, "Slap Bass 1"));
        addInstrument(createInstrument(37, 121, 0, "Slap Bass 2"));
        addInstrument(createInstrument(38, 121, 0, "Synth Bass 1"));
        addInstrument(createInstrument(38, 121, 1, "SynthBass101"));
        addInstrument(createInstrument(38, 121, 2, "Acid Bass"));
        addInstrument(createInstrument(38, 121, 3, "Clav Bass"));
        addInstrument(createInstrument(38, 121, 4, "Hammer Bass"));
        addInstrument(createInstrument(39, 121, 0, "Synth Bass 2"));
        addInstrument(createInstrument(39, 121, 1, "SynSlap Bass"));
        addInstrument(createInstrument(39, 121, 2, "Rubber Bass"));
        addInstrument(createInstrument(39, 121, 3, "Attack Pulse"));
        addInstrument(createInstrument(40, 121, 0, "Violin"));
        addInstrument(createInstrument(40, 121, 1, "Slow Violin"));
        addInstrument(createInstrument(41, 121, 0, "Viola"));
        addInstrument(createInstrument(42, 121, 0, "Cello"));
        addInstrument(createInstrument(43, 121, 0, "Contrabass"));
        addInstrument(createInstrument(44, 121, 0, "Tremolo Str"));
        addInstrument(createInstrument(45, 121, 0, "PizzicatoStr"));
        addInstrument(createInstrument(46, 121, 0, "Harp"));
        addInstrument(createInstrument(46, 121, 1, "Yang Qin"));
        addInstrument(createInstrument(47, 121, 0, "Timpani"));
        addInstrument(createInstrument(48, 121, 0, "Strings"));
        addInstrument(createInstrument(48, 121, 1, "Orchestra"));
        addInstrument(createInstrument(48, 121, 2, "Oct Strings"));
        addInstrument(createInstrument(49, 121, 0, "Slow Strings"));
        addInstrument(createInstrument(50, 121, 0, "Syn Strings1"));
        addInstrument(createInstrument(50, 121, 1, "Syn Strings3"));
        addInstrument(createInstrument(51, 121, 0, "Syn Strings2"));
        addInstrument(createInstrument(52, 121, 0, "Choir Aahs"));
        addInstrument(createInstrument(52, 121, 1, "Chorus Aahs"));
        addInstrument(createInstrument(53, 121, 0, "Voice Oohs"));
        addInstrument(createInstrument(53, 121, 1, "Humming"));
        addInstrument(createInstrument(54, 121, 0, "SynVox"));
        addInstrument(createInstrument(54, 121, 1, "Analog Voice"));
        addInstrument(createInstrument(55, 121, 0, "OrchestraHit"));
        addInstrument(createInstrument(55, 121, 1, "Bass Hit"));
        addInstrument(createInstrument(55, 121, 2, "6th Hit"));
        addInstrument(createInstrument(55, 121, 3, "Euro Hit"));
        addInstrument(createInstrument(56, 121, 0, "Trumpet"));
        addInstrument(createInstrument(56, 121, 1, "Dark Trumpet"));
        addInstrument(createInstrument(57, 121, 0, "Trombone 1"));
        addInstrument(createInstrument(57, 121, 1, "Trombone 2"));
        addInstrument(createInstrument(57, 121, 2, "Bright Tb"));
        addInstrument(createInstrument(58, 121, 0, "Tuba"));
        addInstrument(createInstrument(59, 121, 0, "MuteTrumpet1"));
        addInstrument(createInstrument(59, 121, 1, "MuteTrumpet2"));
        addInstrument(createInstrument(60, 121, 0, "F Horn Sect"));
        addInstrument(createInstrument(60, 121, 1, "French Horn"));
        addInstrument(createInstrument(61, 121, 0, "Brass 1"));
        addInstrument(createInstrument(61, 121, 1, "Brass 2"));
        addInstrument(createInstrument(62, 121, 0, "Synth Brass1"));
        addInstrument(createInstrument(62, 121, 1, "JP Brass"));
        addInstrument(createInstrument(62, 121, 2, "Oct SynBrass"));
        addInstrument(createInstrument(62, 121, 3, "Jump Brass"));
        addInstrument(createInstrument(63, 121, 0, "Synth Brass2"));
        addInstrument(createInstrument(63, 121, 1, "SynBrass sfz"));
        addInstrument(createInstrument(63, 121, 2, "Velo Brass"));
        addInstrument(createInstrument(64, 121, 0, "Soprano Sax"));
        addInstrument(createInstrument(65, 121, 0, "Alto Sax"));
        addInstrument(createInstrument(66, 121, 0, "Tenor Sax"));
        addInstrument(createInstrument(67, 121, 0, "Baritone Sax"));
        addInstrument(createInstrument(68, 121, 0, "Oboe"));
        addInstrument(createInstrument(69, 121, 0, "English Horn"));
        addInstrument(createInstrument(70, 121, 0, "Bassoon"));
        addInstrument(createInstrument(71, 121, 0, "Clarinet"));
        addInstrument(createInstrument(72, 121, 0, "Piccolo"));
        addInstrument(createInstrument(73, 121, 0, "Flute"));
        addInstrument(createInstrument(74, 121, 0, "Recorder"));
        addInstrument(createInstrument(75, 121, 0, "Pan Flute"));
        addInstrument(createInstrument(76, 121, 0, "Bottle Blow"));
        addInstrument(createInstrument(77, 121, 0, "Shakuhachi"));
        addInstrument(createInstrument(78, 121, 0, "Whistle"));
        addInstrument(createInstrument(79, 121, 0, "Ocarina"));
        addInstrument(createInstrument(80, 121, 0, "Square Wave"));
        addInstrument(createInstrument(80, 121, 1, "MG Square"));
        addInstrument(createInstrument(80, 121, 2, "2600 Sine"));
        addInstrument(createInstrument(81, 121, 0, "Saw Wave"));
        addInstrument(createInstrument(81, 121, 1, "OB2 Saw"));
        addInstrument(createInstrument(81, 121, 2, "Doctor Solo"));
        addInstrument(createInstrument(81, 121, 3, "Natural Lead"));
        addInstrument(createInstrument(81, 121, 4, "SequencedSaw"));
        addInstrument(createInstrument(82, 121, 0, "Syn Calliope"));
        addInstrument(createInstrument(83, 121, 0, "Chiffer Lead"));
        addInstrument(createInstrument(84, 121, 0, "Charang"));
        addInstrument(createInstrument(84, 121, 1, "Wire Lead"));
        addInstrument(createInstrument(85, 121, 0, "Solo Vox"));
        addInstrument(createInstrument(86, 121, 0, "5th Saw Wave"));
        addInstrument(createInstrument(87, 121, 0, "Bass & Lead"));
        addInstrument(createInstrument(87, 121, 1, "Delayed Lead"));
        addInstrument(createInstrument(88, 121, 0, "Fantasia"));
        addInstrument(createInstrument(89, 121, 0, "Warm Pad"));
        addInstrument(createInstrument(89, 121, 1, "Sine Pad"));
        addInstrument(createInstrument(90, 121, 0, "Poly Synth"));
        addInstrument(createInstrument(91, 121, 0, "Space Voice"));
        addInstrument(createInstrument(91, 121, 1, "Itopia"));
        addInstrument(createInstrument(92, 121, 0, "Bowed Glass"));
        addInstrument(createInstrument(93, 121, 0, "Metal Pad"));
        addInstrument(createInstrument(94, 121, 0, "Halo Pad"));
        addInstrument(createInstrument(95, 121, 0, "Sweep Pad"));
        addInstrument(createInstrument(96, 121, 0, "Ice Rain"));
        addInstrument(createInstrument(97, 121, 0, "Soundtrack"));
        addInstrument(createInstrument(98, 121, 0, "Crystal"));
        addInstrument(createInstrument(98, 121, 1, "Syn Mallet"));
        addInstrument(createInstrument(99, 121, 0, "Atmosphere"));
        addInstrument(createInstrument(100, 121, 0, "Brightness"));
        addInstrument(createInstrument(101, 121, 0, "Goblin"));
        addInstrument(createInstrument(102, 121, 0, "Echo Drops"));
        addInstrument(createInstrument(102, 121, 1, "Echo Bell"));
        addInstrument(createInstrument(102, 121, 2, "Echo Pan"));
        addInstrument(createInstrument(103, 121, 0, "Star Theme"));
        addInstrument(createInstrument(104, 121, 0, "Sitar 1"));
        addInstrument(createInstrument(104, 121, 1, "Sitar 2"));
        addInstrument(createInstrument(105, 121, 0, "Banjo"));
        addInstrument(createInstrument(106, 121, 0, "Shamisen"));
        addInstrument(createInstrument(107, 121, 0, "Koto"));
        addInstrument(createInstrument(107, 121, 1, "Taisho Koto"));
        addInstrument(createInstrument(108, 121, 0, "Kalimba"));
        addInstrument(createInstrument(109, 121, 0, "Bagpipe"));
        addInstrument(createInstrument(110, 121, 0, "Fiddle"));
        addInstrument(createInstrument(111, 121, 0, "Shanai"));
        addInstrument(createInstrument(112, 121, 0, "Tinkle Bell"));
        addInstrument(createInstrument(113, 121, 0, "Agogo"));
        addInstrument(createInstrument(114, 121, 0, "Steel Drums"));
        addInstrument(createInstrument(115, 121, 0, "Woodsection"));
        addInstrument(createInstrument(115, 121, 1, "Castanets"));
        addInstrument(createInstrument(116, 121, 0, "Taiko"));
        addInstrument(createInstrument(116, 121, 1, "Concert BD"));
        addInstrument(createInstrument(117, 121, 0, "Melo Tom 1"));
        addInstrument(createInstrument(117, 121, 1, "Melo Tom 2"));
        addInstrument(createInstrument(118, 121, 0, "Synth Drum"));
        addInstrument(createInstrument(118, 121, 1, "808 Tom"));
        addInstrument(createInstrument(118, 121, 2, "Elec Perc"));
        addInstrument(createInstrument(119, 121, 0, "Reverse Cymb"));
        addInstrument(createInstrument(120, 121, 0, "Gt FretNoise"));
        addInstrument(createInstrument(120, 121, 1, "Gt Cut Noise"));
        addInstrument(createInstrument(120, 121, 2, "String Slap"));
        addInstrument(createInstrument(121, 121, 0, "Breath Noise"));
        addInstrument(createInstrument(121, 121, 1, "Fl Key Click"));
        addInstrument(createInstrument(122, 121, 0, "Seashore"));
        addInstrument(createInstrument(122, 121, 1, "Rain"));
        addInstrument(createInstrument(122, 121, 2, "Thunder"));
        addInstrument(createInstrument(122, 121, 3, "Wind"));
        addInstrument(createInstrument(122, 121, 4, "Stream"));
        addInstrument(createInstrument(122, 121, 5, "Bubble"));
        addInstrument(createInstrument(123, 121, 0, "Bird 1"));
        addInstrument(createInstrument(123, 121, 1, "Dog"));
        addInstrument(createInstrument(123, 121, 2, "Horse Gallop"));
        addInstrument(createInstrument(123, 121, 3, "Bird 2"));
        addInstrument(createInstrument(124, 121, 0, "Telephone 1"));
        addInstrument(createInstrument(124, 121, 1, "Telephone 2"));
        addInstrument(createInstrument(124, 121, 2, "DoorCreaking"));
        addInstrument(createInstrument(124, 121, 3, "Door"));
        addInstrument(createInstrument(124, 121, 4, "Scratch"));
        addInstrument(createInstrument(124, 121, 5, "Wind Chimes"));
        addInstrument(createInstrument(125, 121, 0, "Helicopter"));
        addInstrument(createInstrument(125, 121, 1, "Car Engine"));
        addInstrument(createInstrument(125, 121, 2, "Car Stop"));
        addInstrument(createInstrument(125, 121, 3, "Car Pass"));
        addInstrument(createInstrument(125, 121, 4, "Car Crash"));
        addInstrument(createInstrument(125, 121, 5, "Siren"));
        addInstrument(createInstrument(125, 121, 6, "Train"));
        addInstrument(createInstrument(125, 121, 7, "Jetplane"));
        addInstrument(createInstrument(125, 121, 8, "Starship"));
        addInstrument(createInstrument(125, 121, 9, "Burst Noise"));
        addInstrument(createInstrument(126, 121, 0, "Applause"));
        addInstrument(createInstrument(126, 121, 1, "Laughing"));
        addInstrument(createInstrument(126, 121, 2, "Screaming"));
        addInstrument(createInstrument(126, 121, 3, "Punch"));
        addInstrument(createInstrument(126, 121, 4, "Heart Beat"));
        addInstrument(createInstrument(126, 121, 5, "Footsteps"));
        addInstrument(createInstrument(127, 121, 0, "Gun Shot"));
        addInstrument(createInstrument(127, 121, 1, "Machine Gun"));
        addInstrument(createInstrument(127, 121, 2, "Laser Gun"));
        addInstrument(createInstrument(127, 121, 3, "Explosion"));

        DEFAULT_DRUMS_INSTRUMENT = createDrumsInstrument(DrumKit.Type.STANDARD, KeyMapGSGM2.getInstance(), 0, 120, 0, "Drum Kit Standard");
        addInstrument(DEFAULT_DRUMS_INSTRUMENT);
        addInstrument(createDrumsInstrument(DrumKit.Type.ROOM, KeyMapGSGM2.getInstance(), 8, 120, 0, "Drum Kit Room"));
        addInstrument(createDrumsInstrument(DrumKit.Type.POWER, KeyMapGSGM2.getInstance(), 16, 120, 0, "Drum Kit Power"));
        addInstrument(createDrumsInstrument(DrumKit.Type.ELECTRONIC, KeyMapGSGM2.getInstance(), 24, 120, 0, "Drum Kit Electronic"));
        addInstrument(createDrumsInstrument(DrumKit.Type.ANALOG, KeyMapGSGM2.getInstance(), 25, 120, 0, "Drum Kit Analog"));
        addInstrument(createDrumsInstrument(DrumKit.Type.JAZZ, KeyMapGSGM2.getInstance(), 32, 120, 0, "Drum Kit Jazz"));
        addInstrument(createDrumsInstrument(DrumKit.Type.BRUSH, KeyMapGSGM2.getInstance(), 40, 120, 0, "Drum Kit Brush"));
        addInstrument(createDrumsInstrument(DrumKit.Type.ORCHESTRA, KeyMapGSGM2.getInstance(), 48, 120, 0, "Drum Kit Orchestra"));
        addInstrument(createDrumsInstrument(DrumKit.Type.SFX, KeyMapGSGM2.getInstance(), 56, 120, 0, "Drum Kit SFX"));
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
        if (res.isEmpty() && tryHarder && (kit.getKeyMap().isContaining(KeyMapGM.getInstance())))
        {
            // GM is fully compatible
            res.add(instruments.get(256 + kit.getType().ordinal()));
        }
        return res;
    }

    /**
     * Convenience method to reorder arguments.
     *
     * @return
     */
    private static Instrument createInstrument(int pc, int msb, int lsb, String name)
    {
        GM1Instrument gmIns = GM1Bank.getInstance().getInstrument(pc); // GM2's PC is directly compatible with GM1
        Instrument ins = new Instrument(name, null, new MidiAddress(pc, msb, lsb, DEFAULT_BANK_SELECT_METHOD), null, gmIns);
        return ins;
    }

    /**
     * Convenience method to reorder arguments.
     *
     * @return
     */
    private static Instrument createDrumsInstrument(DrumKit.Type t, DrumKit.KeyMap map, int pc, int msb, int lsb, String name)
    {
        return new Instrument(name, null, new MidiAddress(pc, msb, lsb, DEFAULT_BANK_SELECT_METHOD), new DrumKit(t, map), null);
    }
}
