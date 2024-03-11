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
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiAddress.BankSelectMethod;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midi.api.keymap.KeyMapGSGM2;
import org.jjazz.midi.api.keymap.KeyMapXG;

/**
 * The Roland GS Bank (based on SC55).
 * <p>
 * IMPORTANT: the GS bank is NOT compatible with GM2/XG voices. They use the same MidiAddress for different patches.
 * <p>
 * Instance should be obtained from the StdSynth.
 */
public class GSBank extends InstrumentBank<Instrument>
{

    public static final String BANKNAME = "GS Bank (SC55)";
    public static final int DEFAULT_BANK_SELECT_LSB = 0;
    public static final int DEFAULT_BANK_SELECT_MSB = 0;
    public static final BankSelectMethod DEFAULT_BANK_SELECT_METHOD = BankSelectMethod.MSB_ONLY;
    private static Instrument DEFAULT_DRUMS_INSTRUMENT;
    private static GSBank INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(GSBank.class.getSimpleName());

    /**
     * Use the XGSynth to get access to the instance.
     *
     * @return
     */
    protected static GSBank getInstance()
    {
        synchronized (GSBank.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new GSBank();
            }
        }
        return INSTANCE;
    }

    private GSBank()
    {
        super(BANKNAME, DEFAULT_BANK_SELECT_MSB, DEFAULT_BANK_SELECT_LSB, DEFAULT_BANK_SELECT_METHOD);
        // PC, MSB, PatchName
        addInstrument(createInstrument(0, 0, "Piano 1"));
        addInstrument(createInstrument(0, 8, "Piano 1w"));
        addInstrument(createInstrument(0, 16, "Piano 1d"));
        addInstrument(createInstrument(1, 0, "Piano 2"));
        addInstrument(createInstrument(1, 8, "Piano 2w"));
        addInstrument(createInstrument(2, 0, "Piano 3"));
        addInstrument(createInstrument(2, 8, "Piano 3w"));
        addInstrument(createInstrument(3, 0, "Honky-tonk"));
        addInstrument(createInstrument(3, 8, "Honky-tonk w"));
        addInstrument(createInstrument(4, 0, "E. Piano 1"));
        addInstrument(createInstrument(4, 8, "Detuned EP 1"));
        addInstrument(createInstrument(4, 16, "E. Piano 1w"));
        addInstrument(createInstrument(4, 24, "60's E. Piano"));
        addInstrument(createInstrument(5, 0, "E. Piano 2"));
        addInstrument(createInstrument(5, 8, "Detuned EP 2"));
        addInstrument(createInstrument(5, 16, "E. Piano 2w"));
        addInstrument(createInstrument(6, 0, "Harpsichord"));
        addInstrument(createInstrument(6, 8, "Coupled Harpsichord"));
        addInstrument(createInstrument(6, 16, "Harpsichord w"));
        addInstrument(createInstrument(6, 24, "Harpsichord"));
        addInstrument(createInstrument(7, 0, "Clavinet"));
        addInstrument(createInstrument(8, 0, "Celesta"));
        addInstrument(createInstrument(9, 0, "Glockenspiel"));
        addInstrument(createInstrument(10, 0, "Music Box"));
        addInstrument(createInstrument(11, 0, "Vibraphone"));
        addInstrument(createInstrument(11, 8, "Vibraphone w"));
        addInstrument(createInstrument(12, 0, "Marimba"));
        addInstrument(createInstrument(12, 8, "Marimba w"));
        addInstrument(createInstrument(13, 0, "Xylophone"));
        addInstrument(createInstrument(14, 0, "Tubular-bell"));
        addInstrument(createInstrument(14, 8, "Church Bell"));
        addInstrument(createInstrument(14, 9, "Carillon"));
        addInstrument(createInstrument(15, 0, "Santur"));
        addInstrument(createInstrument(16, 0, "Organ 1"));
        addInstrument(createInstrument(16, 8, "Detunedor.1"));
        addInstrument(createInstrument(16, 16, "60'sorgan 1"));
        addInstrument(createInstrument(16, 32, "Organ 4"));
        addInstrument(createInstrument(17, 0, "Organ 2"));
        addInstrument(createInstrument(17, 8, "Detunedorgan 2"));
        addInstrument(createInstrument(18, 0, "Organ 3"));
        addInstrument(createInstrument(18, 32, "Organ 5"));
        addInstrument(createInstrument(19, 0, "Churchorg.1"));
        addInstrument(createInstrument(19, 8, "Churchorg.2"));
        addInstrument(createInstrument(19, 16, "Churchorg.3"));
        addInstrument(createInstrument(20, 0, "Reedorgan"));
        addInstrument(createInstrument(21, 0, "Accordion French"));
        addInstrument(createInstrument(21, 8, "Accordion Italian"));
        addInstrument(createInstrument(22, 0, "Harmonica"));
        addInstrument(createInstrument(23, 0, "Bandoneon"));
        addInstrument(createInstrument(24, 0, "Nylon String Guitar"));
        addInstrument(createInstrument(24, 8, "Ukulele"));
        addInstrument(createInstrument(24, 16, "Nylon Guitar"));
        addInstrument(createInstrument(24, 32, "Nylon Guitar 2"));
        addInstrument(createInstrument(25, 0, "Steel String Guitar"));
        addInstrument(createInstrument(25, 8, "12 String Guitar"));
        addInstrument(createInstrument(25, 16, "Mandolin"));
        addInstrument(createInstrument(26, 0, "Jazz Guitar"));
        addInstrument(createInstrument(26, 8, "Hawaiian Guitar"));
        addInstrument(createInstrument(27, 0, "Clean Guitar"));
        addInstrument(createInstrument(27, 8, "Chorus Guitar"));
        addInstrument(createInstrument(28, 0, "Muted Gt."));
        addInstrument(createInstrument(28, 8, "Funk Guitar"));
        addInstrument(createInstrument(28, 16, "Funk Guitar 2"));
        addInstrument(createInstrument(29, 0, "Overdrive Guitar"));
        addInstrument(createInstrument(30, 0, "Distortion Guitar"));
        addInstrument(createInstrument(30, 8, "Feedback Guitar"));
        addInstrument(createInstrument(31, 0, "Guitar Harmonics"));
        addInstrument(createInstrument(31, 8, "Guitar Feedback"));
        addInstrument(createInstrument(32, 0, "Acoustic Bass"));
        addInstrument(createInstrument(33, 0, "Fingered Bass"));
        addInstrument(createInstrument(34, 0, "Picked Bass"));
        addInstrument(createInstrument(35, 0, "Fretless Bass"));
        addInstrument(createInstrument(36, 0, "Slap Bass 1"));
        addInstrument(createInstrument(37, 0, "Slap Bass 2"));
        addInstrument(createInstrument(38, 0, "Synth Bass 1"));
        addInstrument(createInstrument(38, 1, "Synth Bass 101"));
        addInstrument(createInstrument(38, 8, "Synth Bass 3"));
        addInstrument(createInstrument(39, 0, "Synth Bass 2"));
        addInstrument(createInstrument(39, 8, "Synth Bass 4"));
        addInstrument(createInstrument(39, 16, "Rubber Bass"));
        addInstrument(createInstrument(40, 0, "Violin"));
        addInstrument(createInstrument(41, 0, "Viola"));
        addInstrument(createInstrument(41, 8, "Slow Violin"));
        addInstrument(createInstrument(42, 0, "Cello"));
        addInstrument(createInstrument(43, 0, "Contrabass"));
        addInstrument(createInstrument(44, 0, "Tremolo Strings"));
        addInstrument(createInstrument(45, 0, "Pizzicato Strings"));
        addInstrument(createInstrument(46, 0, "Harp"));
        addInstrument(createInstrument(47, 0, "Timpani"));
        addInstrument(createInstrument(48, 0, "Strings"));
        addInstrument(createInstrument(48, 8, "Orchestra"));
        addInstrument(createInstrument(49, 0, "Slow Strings"));
        addInstrument(createInstrument(50, 0, "Synth Strings1"));
        addInstrument(createInstrument(50, 8, "Synth Strings 3"));
        addInstrument(createInstrument(51, 0, "Synth Strings2"));
        addInstrument(createInstrument(52, 0, "Choir Aahs"));
        addInstrument(createInstrument(52, 32, "Choir Aahs 2"));
        addInstrument(createInstrument(53, 0, "Voiceoohs"));
        addInstrument(createInstrument(54, 0, "Synth Vox"));
        addInstrument(createInstrument(55, 0, "Orchestra Hit"));
        addInstrument(createInstrument(56, 0, "Trumpet"));
        addInstrument(createInstrument(57, 0, "Trombone"));
        addInstrument(createInstrument(57, 1, "Trombone 2"));
        addInstrument(createInstrument(58, 0, "Tuba"));
        addInstrument(createInstrument(59, 0, "Muted Trumpet"));
        addInstrument(createInstrument(60, 0, "French Horn"));
        addInstrument(createInstrument(60, 1, "French Horn2"));
        addInstrument(createInstrument(61, 0, "Brass 1"));
        addInstrument(createInstrument(61, 8, "Brass 2"));
        addInstrument(createInstrument(62, 0, "Synth Brass 1"));
        addInstrument(createInstrument(62, 8, "Synth Brass 3"));
        addInstrument(createInstrument(62, 16, "Analog Brass 1"));
        addInstrument(createInstrument(63, 0, "Synth Brass 2"));
        addInstrument(createInstrument(63, 8, "Synth Brass 4"));
        addInstrument(createInstrument(63, 16, "Analog Brass 2"));
        addInstrument(createInstrument(64, 0, "Soprano Sax"));
        addInstrument(createInstrument(65, 0, "Alto Sax"));
        addInstrument(createInstrument(66, 0, "Tenor Sax"));
        addInstrument(createInstrument(67, 0, "Baritone Sax"));
        addInstrument(createInstrument(68, 0, "Oboe"));
        addInstrument(createInstrument(69, 0, "English Horn"));
        addInstrument(createInstrument(70, 0, "Bassoon"));
        addInstrument(createInstrument(71, 0, "Clarinet"));
        addInstrument(createInstrument(72, 0, "Piccolo"));
        addInstrument(createInstrument(73, 0, "Flute"));
        addInstrument(createInstrument(74, 0, "Recorder"));
        addInstrument(createInstrument(75, 0, "Pan Flute"));
        addInstrument(createInstrument(76, 0, "Bottle Blow"));
        addInstrument(createInstrument(77, 0, "Shakuhachi"));
        addInstrument(createInstrument(78, 0, "Whistle"));
        addInstrument(createInstrument(79, 0, "Ocarina"));
        addInstrument(createInstrument(80, 0, "Square Wave"));
        addInstrument(createInstrument(80, 1, "Square"));
        addInstrument(createInstrument(80, 8, "Sine Wave"));
        addInstrument(createInstrument(81, 0, "Saw Wave"));
        addInstrument(createInstrument(81, 1, "Saw"));
        addInstrument(createInstrument(81, 8, "Doctor Solo"));
        addInstrument(createInstrument(82, 0, "Synth Calliope"));
        addInstrument(createInstrument(83, 0, "Chiffer Lead"));
        addInstrument(createInstrument(84, 0, "Charang"));
        addInstrument(createInstrument(85, 0, "Solo Vox"));
        addInstrument(createInstrument(86, 0, "5th Saw Wave"));
        addInstrument(createInstrument(87, 0, "Bass & Lead"));
        addInstrument(createInstrument(88, 0, "Fantasia"));
        addInstrument(createInstrument(89, 0, "Warm Pad"));
        addInstrument(createInstrument(90, 0, "Polysynth"));
        addInstrument(createInstrument(91, 0, "Space Voice"));
        addInstrument(createInstrument(92, 0, "Bowed Glass"));
        addInstrument(createInstrument(93, 0, "Metal Pad"));
        addInstrument(createInstrument(94, 0, "Halo Pad"));
        addInstrument(createInstrument(95, 0, "Sweep Pad"));
        addInstrument(createInstrument(96, 0, "Ice Rain"));
        addInstrument(createInstrument(97, 0, "Soundtrack"));
        addInstrument(createInstrument(98, 0, "Crystal"));
        addInstrument(createInstrument(98, 1, "Synth Mallet"));
        addInstrument(createInstrument(99, 0, "Atmosphere"));
        addInstrument(createInstrument(100, 0, "Brightness"));
        addInstrument(createInstrument(101, 0, "Goblin"));
        addInstrument(createInstrument(102, 0, "Echo Drops"));
        addInstrument(createInstrument(102, 1, "Echo Bell"));
        addInstrument(createInstrument(102, 2, "Echo Pan"));
        addInstrument(createInstrument(103, 0, "Star Theme"));
        addInstrument(createInstrument(104, 0, "Sitar"));
        addInstrument(createInstrument(104, 1, "Sitar 2"));
        addInstrument(createInstrument(105, 0, "Banjo"));
        addInstrument(createInstrument(106, 0, "Shamisen"));
        addInstrument(createInstrument(107, 0, "Koto"));
        addInstrument(createInstrument(107, 8, "Taisho Koto"));
        addInstrument(createInstrument(108, 0, "Kalimba"));
        addInstrument(createInstrument(109, 0, "Bag Pipe"));
        addInstrument(createInstrument(110, 0, "Fiddle"));
        addInstrument(createInstrument(111, 0, "Shannai"));
        addInstrument(createInstrument(112, 0, "Tinkle Bell"));
        addInstrument(createInstrument(113, 0, "Agogo"));
        addInstrument(createInstrument(114, 0, "Steel Drums"));
        addInstrument(createInstrument(115, 0, "Woodblock"));
        addInstrument(createInstrument(115, 8, "Castanets"));
        addInstrument(createInstrument(116, 0, "Taiko"));
        addInstrument(createInstrument(116, 8, "Concert BD"));
        addInstrument(createInstrument(117, 0, "Melodic Tom 1"));
        addInstrument(createInstrument(117, 8, "Melodic Tom 2"));
        addInstrument(createInstrument(118, 0, "Synth Drum"));
        addInstrument(createInstrument(118, 8, "808 Tom"));
        addInstrument(createInstrument(118, 9, "Elec Perc"));
        addInstrument(createInstrument(119, 0, "Reverse Cymbal"));
        addInstrument(createInstrument(120, 0, "Guitar Fret Noise"));
        addInstrument(createInstrument(120, 1, "Guitar Cut Noise"));
        addInstrument(createInstrument(120, 2, "String Slap"));
        addInstrument(createInstrument(121, 0, "Breath Noise"));
        addInstrument(createInstrument(121, 1, "Flute Key Click"));
        addInstrument(createInstrument(122, 0, "Seashore"));
        addInstrument(createInstrument(122, 1, "Rain"));
        addInstrument(createInstrument(122, 2, "Thunder"));
        addInstrument(createInstrument(122, 3, "Wind"));
        addInstrument(createInstrument(122, 4, "Stream"));
        addInstrument(createInstrument(123, 0, "Bird"));
        addInstrument(createInstrument(123, 1, "Dog"));
        addInstrument(createInstrument(123, 2, "Horse-Gallop"));
        addInstrument(createInstrument(123, 3, "Bird 2"));
        addInstrument(createInstrument(124, 0, "Telephone 1"));
        addInstrument(createInstrument(124, 1, "Telephone 2"));
        addInstrument(createInstrument(124, 2, "Door Creaking"));
        addInstrument(createInstrument(124, 3, "Door Slam"));
        addInstrument(createInstrument(124, 4, "Scratch"));
        addInstrument(createInstrument(125, 0, "Helicopter"));
        addInstrument(createInstrument(125, 1, "Car-Engine"));
        addInstrument(createInstrument(125, 2, "Car-Stop"));
        addInstrument(createInstrument(125, 3, "Car-Pass"));
        addInstrument(createInstrument(125, 4, "Car-Crash"));
        addInstrument(createInstrument(125, 6, "Train"));
        addInstrument(createInstrument(125, 7, "Jetplane"));
        addInstrument(createInstrument(125, 8, "Starship"));
        addInstrument(createInstrument(125, 9, "Burst Noise"));
        addInstrument(createInstrument(126, 0, "Applause"));
        addInstrument(createInstrument(126, 1, "Laughing"));
        addInstrument(createInstrument(126, 2, "Screaming"));
        addInstrument(createInstrument(126, 3, "Punch"));
        addInstrument(createInstrument(126, 4, "Heart Beat"));
        addInstrument(createInstrument(127, 0, "Gun Shot"));
        addInstrument(createInstrument(127, 1, "Machine Gun"));
        addInstrument(createInstrument(127, 2, "Lasergun"));
        addInstrument(createInstrument(127, 3, "Explosion"));

        // IMPORTANT =======================================
        // Same as GM2 for the programChange. 
        // Normally GS accepts drums only on Midi channel 10, drumkit can't be selected via bankMSB/bankLSB to work on a different channel.
        DEFAULT_DRUMS_INSTRUMENT = createDrumsInstrument(DrumKit.Type.STANDARD, KeyMapGSGM2.getInstance(), 0, "Drum Kit Standard");
        addInstrument(DEFAULT_DRUMS_INSTRUMENT);
        addInstrument(createDrumsInstrument(DrumKit.Type.ROOM, KeyMapGSGM2.getInstance(), 8, "Drum Kit Room"));
        addInstrument(createDrumsInstrument(DrumKit.Type.POWER, KeyMapGSGM2.getInstance(), 16, "Drum Kit Power"));
        addInstrument(createDrumsInstrument(DrumKit.Type.ELECTRONIC, KeyMapGSGM2.getInstance(), 24, "Drum Kit Electronic"));
        addInstrument(createDrumsInstrument(DrumKit.Type.ANALOG, KeyMapGSGM2.getInstance(), 25, "Drum Kit Analog"));
        addInstrument(createDrumsInstrument(DrumKit.Type.JAZZ, KeyMapGSGM2.getInstance(), 32, "Drum Kit Jazz"));
        addInstrument(createDrumsInstrument(DrumKit.Type.BRUSH, KeyMapGSGM2.getInstance(), 40, "Drum Kit Brush"));
        addInstrument(createDrumsInstrument(DrumKit.Type.ORCHESTRA, KeyMapGSGM2.getInstance(), 48, "Drum Kit Orchestra"));
        addInstrument(createDrumsInstrument(DrumKit.Type.SFX, KeyMapGSGM2.getInstance(), 56, "Drum Kit SFX"));

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
                && (kit.getKeyMap().equals(KeyMapGM.getInstance()) || kit.getKeyMap().equals(KeyMapXG.getInstance())))
        {
            // GM is fully compatible, XG is somewhat compatible...
            res.add(instruments.get(222 + kit.getType().ordinal()));
        }
        return res;
    }

    /**
     * Convenience method to reorder arguments.
     *
     * @return
     */
    private Instrument createInstrument(int pc, int msb, String name)
    {
        GM1Instrument gmIns = GMSynth.getInstance().getGM1Bank().getInstrument(pc); // GS's PC is directly compatible with GM1
        Instrument ins = new GSInstrument(name, null, new MidiAddress(pc, msb, -1, DEFAULT_BANK_SELECT_METHOD), null, gmIns);
        return ins;
    }

    /**
     * Convenience method to reorder arguments.
     *
     * @return
     */
    private Instrument createDrumsInstrument(DrumKit.Type t, DrumKit.KeyMap map, int pc, String name)
    {
        // GS does not define bank select for drums: it must be on channel 10 only PC is used
        return new GSDrumsInstrument(name, null, new MidiAddress(pc, 120, -1, MidiAddress.BankSelectMethod.PC_ONLY), new DrumKit(t, map), null);
    }

}
