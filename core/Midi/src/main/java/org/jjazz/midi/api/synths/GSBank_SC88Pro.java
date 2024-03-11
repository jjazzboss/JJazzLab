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

import java.util.logging.*;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiAddress.BankSelectMethod;
import static org.jjazz.midi.api.MidiUtilities.buildMessage;
import org.jjazz.midi.api.keymap.KeyMapGSGM2;
import org.openide.util.Exceptions;

/**
 * The Roland GS Bank.
 * <p>
 * Instance should be obtained from the StdSynth.
 */
public class GSBank_SC88Pro extends InstrumentBank<Instrument>
{

    public static final String BANKNAME = "GS Bank SC88 Pro";
    public static final int DEFAULT_BANK_SELECT_LSB = 0;
    public static final int DEFAULT_BANK_SELECT_MSB = 0;
    public static final BankSelectMethod DEFAULT_BANK_SELECT_METHOD = BankSelectMethod.MSB_LSB;
    private static Instrument DEFAULT_DRUMS_INSTRUMENT;
    private static GSBank_SC88Pro INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(GSBank_SC88Pro.class.getSimpleName());

    /**
     * Use the XGSynth to get access to the instance.
     *
     * @return
     */
    protected static GSBank_SC88Pro getInstance()
    {
        synchronized (GSBank_SC88Pro.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new GSBank_SC88Pro();
            }
        }
        return INSTANCE;
    }

    private GSBank_SC88Pro()
    {
        super(BANKNAME, DEFAULT_BANK_SELECT_MSB, DEFAULT_BANK_SELECT_LSB, DEFAULT_BANK_SELECT_METHOD);
        addInstrument(createInstrument(0, 0, "Piano 1"));
        addInstrument(createInstrument(0, 8, "Piano 1w"));
        addInstrument(createInstrument(0, 16, "Piano 1d"));
        addInstrument(createInstrument(0, 24, "Piano + String"));
        addInstrument(createInstrument(1, 0, "Piano 2"));
        addInstrument(createInstrument(1, 8, "Piano 2w"));
        addInstrument(createInstrument(1, 16, "Dance Piano"));
        addInstrument(createInstrument(2, 0, "Piano 3"));
        addInstrument(createInstrument(2, 1, "EG + Rhodes 1"));
        addInstrument(createInstrument(2, 2, "EG + Rhodes 2"));
        addInstrument(createInstrument(2, 8, "Piano 3w"));
        addInstrument(createInstrument(3, 0, "Honky-tonk"));
        addInstrument(createInstrument(3, 8, "Honky-tonk w"));
        addInstrument(createInstrument(4, 0, "E. Piano 1"));
        addInstrument(createInstrument(4, 8, "Detuned EP 1"));
        addInstrument(createInstrument(4, 9, "Chorused E. Piano"));
        addInstrument(createInstrument(4, 10, "Silent Rhodes"));
        addInstrument(createInstrument(4, 16, "E. Piano 1w"));
        addInstrument(createInstrument(4, 17, "Dist E. Piano"));
        addInstrument(createInstrument(4, 24, "60's E. Piano"));
        addInstrument(createInstrument(4, 25, "Hard Rhodes"));
        addInstrument(createInstrument(4, 26, "Mellow Rhodes"));
        addInstrument(createInstrument(5, 0, "E. Piano 2"));
        addInstrument(createInstrument(5, 8, "Detuned EP 2"));
        addInstrument(createInstrument(5, 16, "E. Piano 2w"));
        addInstrument(createInstrument(5, 24, "Hard FM EP"));
        addInstrument(createInstrument(6, 0, "Harpsichord"));
        addInstrument(createInstrument(6, 1, "Harpsichord 2"));
        addInstrument(createInstrument(6, 8, "Coupled Harpsichord"));
        addInstrument(createInstrument(6, 16, "Harpsichord w"));
        addInstrument(createInstrument(6, 24, "Harpsichord o"));
        addInstrument(createInstrument(6, 32, "Synth Harpsichord"));
        addInstrument(createInstrument(7, 0, "Clavinet"));
        addInstrument(createInstrument(7, 8, "Comp Clav"));
        addInstrument(createInstrument(7, 16, "Reso Clav"));
        addInstrument(createInstrument(7, 24, "Clav o"));
        addInstrument(createInstrument(7, 32, "Analog Clav"));
        addInstrument(createInstrument(7, 33, "JP8 Clav 1"));
        addInstrument(createInstrument(7, 35, "JP8 Clav 2"));
        addInstrument(createInstrument(8, 0, "Celesta"));
        addInstrument(createInstrument(8, 1, "Pop Celesta"));
        addInstrument(createInstrument(9, 0, "Glockenspiel"));
        addInstrument(createInstrument(10, 0, "Music Box"));
        addInstrument(createInstrument(11, 0, "Vibraphone"));
        addInstrument(createInstrument(11, 1, "Hard Vibe"));
        addInstrument(createInstrument(11, 8, "Vibraphone w"));
        addInstrument(createInstrument(11, 9, "Vibraphones"));
        addInstrument(createInstrument(12, 0, "Marimba"));
        addInstrument(createInstrument(12, 8, "Marimba w"));
        addInstrument(createInstrument(12, 16, "Barafon"));
        addInstrument(createInstrument(12, 17, "Barafon 2"));
        addInstrument(createInstrument(12, 24, "Log Drum"));
        addInstrument(createInstrument(13, 0, "Xylophone"));
        addInstrument(createInstrument(14, 0, "Tubular-bell"));
        addInstrument(createInstrument(14, 8, "Church Bell"));
        addInstrument(createInstrument(14, 9, "Carillon"));
        addInstrument(createInstrument(15, 0, "Santur"));
        addInstrument(createInstrument(15, 1, "Santur 2"));
        addInstrument(createInstrument(15, 8, "Cimbalom"));
        addInstrument(createInstrument(15, 16, "Zither 1"));
        addInstrument(createInstrument(15, 17, "Zither 2"));
        addInstrument(createInstrument(15, 24, "Dulcimer"));
        addInstrument(createInstrument(16, 0, "Organ 1"));
        addInstrument(createInstrument(16, 1, "Organ 101"));
        addInstrument(createInstrument(16, 8, "Detuned Or.1"));
        addInstrument(createInstrument(16, 9, "Organ 109"));
        addInstrument(createInstrument(16, 16, "60's Organ 1"));
        addInstrument(createInstrument(16, 17, "60's Organ 2"));
        addInstrument(createInstrument(16, 18, "60's Organ 3"));
        addInstrument(createInstrument(16, 19, "Farf Organ"));
        addInstrument(createInstrument(16, 24, "Cheese Organ"));
        addInstrument(createInstrument(16, 25, "D-50 Organ"));
        addInstrument(createInstrument(16, 26, "JUNO Organ"));
        addInstrument(createInstrument(16, 27, "Hybrid Organ"));
        addInstrument(createInstrument(16, 28, "VS Organ"));
        addInstrument(createInstrument(16, 29, "Digi Church"));
        addInstrument(createInstrument(16, 32, "Organ 4"));
        addInstrument(createInstrument(16, 33, "Even Bar"));
        addInstrument(createInstrument(16, 40, "Organ Bass"));
        addInstrument(createInstrument(16, 48, "5th Organ"));
        addInstrument(createInstrument(17, 0, "Organ 2"));
        addInstrument(createInstrument(17, 1, "Organ 201"));
        addInstrument(createInstrument(17, 2, "E. Organ 16+2"));
        addInstrument(createInstrument(17, 8, "Detuned Organ 2"));
        addInstrument(createInstrument(17, 9, "Octave Organ"));
        addInstrument(createInstrument(17, 32, "Organ 5"));
        addInstrument(createInstrument(18, 0, "Organ 3"));
        addInstrument(createInstrument(18, 8, "Rotary Organ"));
        addInstrument(createInstrument(18, 16, "Rotary Organ S"));
        addInstrument(createInstrument(18, 17, "Rock Organ 1"));
        addInstrument(createInstrument(18, 18, "Rock Organ 2"));
        addInstrument(createInstrument(18, 24, "Rotary Organ F"));
        addInstrument(createInstrument(19, 0, "Church Org.1"));
        addInstrument(createInstrument(19, 8, "Church Org.2"));
        addInstrument(createInstrument(19, 16, "Church Org.3"));
        addInstrument(createInstrument(19, 24, "Organ Flute"));
        addInstrument(createInstrument(19, 32, "Tremolo Flute"));
        addInstrument(createInstrument(19, 33, "Theater Org."));
        addInstrument(createInstrument(20, 0, "Reed Organ"));
        addInstrument(createInstrument(20, 8, "Wind Organ"));
        addInstrument(createInstrument(21, 0, "Accordion French"));
        addInstrument(createInstrument(21, 8, "Accordion Italian"));
        addInstrument(createInstrument(21, 9, "Distorted Accord"));
        addInstrument(createInstrument(21, 16, "Chorused Accord"));
        addInstrument(createInstrument(21, 24, "Hard Accord"));
        addInstrument(createInstrument(21, 25, "Soft Accord"));
        addInstrument(createInstrument(22, 0, "Harmonica"));
        addInstrument(createInstrument(22, 1, "Harmonica 2"));
        addInstrument(createInstrument(23, 0, "Bandoneon"));
        addInstrument(createInstrument(23, 8, "Bandoneon 2"));
        addInstrument(createInstrument(23, 16, "Bandoneon 3"));
        addInstrument(createInstrument(24, 0, "Nylon String Guitar"));
        addInstrument(createInstrument(24, 8, "Ukulele"));
        addInstrument(createInstrument(24, 16, "Nylon Guitar o"));
        addInstrument(createInstrument(24, 24, "Velo Harmonics"));
        addInstrument(createInstrument(24, 32, "Nylon Guitar 2"));
        addInstrument(createInstrument(24, 40, "Lequint Guitar."));
        addInstrument(createInstrument(25, 0, "Steel String Guitar"));
        addInstrument(createInstrument(25, 8, "12 String Guitar"));
        addInstrument(createInstrument(25, 9, "Nylon + Steel"));
        addInstrument(createInstrument(25, 16, "Mandolin"));
        addInstrument(createInstrument(25, 17, "Mandolin 2"));
        addInstrument(createInstrument(25, 18, "Mandolin 3"));
        addInstrument(createInstrument(25, 32, "Steel Guitar 2"));
        addInstrument(createInstrument(26, 0, "Jazz Guitar"));
        addInstrument(createInstrument(26, 1, "Mellow Guitar"));
        addInstrument(createInstrument(26, 8, "Hawaiian Guitar"));
        addInstrument(createInstrument(27, 0, "Clean Guitar"));
        addInstrument(createInstrument(27, 1, "Clean Half"));
        addInstrument(createInstrument(27, 2, "Open Hard 1"));
        addInstrument(createInstrument(27, 3, "Open Hard 2"));
        addInstrument(createInstrument(27, 4, "JC Clean Guitar."));
        addInstrument(createInstrument(27, 8, "Chorus Guitar"));
        addInstrument(createInstrument(27, 9, "JC Chorus Guitar"));
        addInstrument(createInstrument(27, 16, "TC Front Pick"));
        addInstrument(createInstrument(27, 17, "TC Rear Pick"));
        addInstrument(createInstrument(27, 18, "TC Clean ff"));
        addInstrument(createInstrument(27, 19, "TC Clean 2 :"));
        addInstrument(createInstrument(28, 0, "Muted Gt."));
        addInstrument(createInstrument(28, 1, "Muted Distorted Guitar"));
        addInstrument(createInstrument(28, 2, "TC Muted Guitar"));
        addInstrument(createInstrument(28, 8, "Funk Guitar"));
        addInstrument(createInstrument(28, 16, "Funk Guitar 2"));
        addInstrument(createInstrument(29, 0, "Overdrive Guitar"));
        addInstrument(createInstrument(29, 1, "Overdrive 2"));
        addInstrument(createInstrument(29, 2, "Overdrive 3"));
        addInstrument(createInstrument(29, 3, "More Drive"));
        addInstrument(createInstrument(29, 8, "LP Over Drive Guitar"));
        addInstrument(createInstrument(29, 9, "LP Over Drive :"));
        addInstrument(createInstrument(30, 0, "Distortion Guitar"));
        addInstrument(createInstrument(30, 1, "Distortion Guitar 2"));
        addInstrument(createInstrument(30, 2, "Dazed Guitar"));
        addInstrument(createInstrument(30, 3, "Distortion :"));
        addInstrument(createInstrument(30, 4, "Distortion Fast :"));
        addInstrument(createInstrument(30, 8, "Feedback Guitar"));
        addInstrument(createInstrument(30, 9, "Feedback Guitar 2"));
        addInstrument(createInstrument(30, 16, "Power Guitar"));
        addInstrument(createInstrument(30, 17, "Power Guitar 2"));
        addInstrument(createInstrument(30, 18, "5th Distortion Chord"));
        addInstrument(createInstrument(30, 24, "Rock Rhythm"));
        addInstrument(createInstrument(30, 25, "Rock Rhythm 2"));
        addInstrument(createInstrument(31, 0, "Guitar Harmonics"));
        addInstrument(createInstrument(31, 8, "Guitar Feedback"));
        addInstrument(createInstrument(31, 9, "Guitar Feedback 2"));
        addInstrument(createInstrument(31, 16, "Acoustic Guitar Harmonics"));
        addInstrument(createInstrument(31, 24, "Electric Bass Harmonics"));
        addInstrument(createInstrument(32, 0, "Acoustic Bass"));
        addInstrument(createInstrument(32, 1, "Rockabilly"));
        addInstrument(createInstrument(32, 8, "Wild Acoustic Bass"));
        addInstrument(createInstrument(32, 16, "Bass + OHH"));
        addInstrument(createInstrument(33, 0, "Fingered Bass"));
        addInstrument(createInstrument(33, 1, "Fingered Bass 2"));
        addInstrument(createInstrument(33, 2, "Jazz Bass"));
        addInstrument(createInstrument(33, 3, "Jazz Bass 2"));
        addInstrument(createInstrument(33, 4, "Rock Bass"));
        addInstrument(createInstrument(33, 8, "Chorus Jazz Bass"));
        addInstrument(createInstrument(33, 16, "Fingered Bass/Harm"));
        addInstrument(createInstrument(34, 0, "Picked Bass"));
        addInstrument(createInstrument(34, 1, "Picked Bass 2"));
        addInstrument(createInstrument(34, 2, "Picked Bass 3"));
        addInstrument(createInstrument(34, 3, "Picked Bass 4"));
        addInstrument(createInstrument(34, 8, "Mute Pick Bass"));
        addInstrument(createInstrument(34, 16, "Picked Bass/Harm"));
        addInstrument(createInstrument(35, 0, "Fretless Bass"));
        addInstrument(createInstrument(35, 1, "Fretless Bass 2"));
        addInstrument(createInstrument(35, 2, "Fretless Bass 3"));
        addInstrument(createInstrument(35, 3, "Fretless Bass 4"));
        addInstrument(createInstrument(35, 4, "Synth Fretless"));
        addInstrument(createInstrument(35, 5, "Mr. Smooth"));
        addInstrument(createInstrument(35, 8, "Wood + Fretless Bass"));
        addInstrument(createInstrument(36, 0, "Slap Bass 1"));
        addInstrument(createInstrument(36, 1, "Slap Pop"));
        addInstrument(createInstrument(36, 8, "Reso Slap"));
        addInstrument(createInstrument(36, 9, "Unison Slap"));
        addInstrument(createInstrument(37, 0, "Slap Bass 2"));
        addInstrument(createInstrument(37, 8, "FM Slap"));
        addInstrument(createInstrument(38, 0, "Synth Bass 1"));
        addInstrument(createInstrument(38, 1, "Synth Bass 101"));
        addInstrument(createInstrument(38, 2, "CS Bass"));
        addInstrument(createInstrument(38, 3, "JP-4 Bass"));
        addInstrument(createInstrument(38, 4, "JP-8 Bass"));
        addInstrument(createInstrument(38, 5, "P5 Bass"));
        addInstrument(createInstrument(38, 6, "JPMG Bass"));
        addInstrument(createInstrument(38, 8, "Synth Bass 3"));
        addInstrument(createInstrument(38, 9, "TB303 Bass"));
        addInstrument(createInstrument(38, 10, "Tekno Bass"));
        addInstrument(createInstrument(38, 11, "TB303 Bass 2"));
        addInstrument(createInstrument(38, 12, "Kicked TB303"));
        addInstrument(createInstrument(38, 13, "TB303 Saw Bass"));
        addInstrument(createInstrument(38, 14, "Rubber303 Bass"));
        addInstrument(createInstrument(38, 15, "Reso 303 Bass"));
        addInstrument(createInstrument(38, 16, "Reso SH Bass"));
        addInstrument(createInstrument(38, 17, "TB303 Square Bass"));
        addInstrument(createInstrument(38, 18, "TB303 Distorted Bass"));
        addInstrument(createInstrument(38, 24, "Arpeggio Bass"));
        addInstrument(createInstrument(39, 0, "Synth Bass 2"));
        addInstrument(createInstrument(39, 1, "Synth Bass 201"));
        addInstrument(createInstrument(39, 2, "Modular Bass"));
        addInstrument(createInstrument(39, 3, "Sequenced Bass"));
        addInstrument(createInstrument(39, 4, "MG Bass"));
        addInstrument(createInstrument(39, 5, "MG Octave Bass 1"));
        addInstrument(createInstrument(39, 6, "MG Octave Bass 2"));
        addInstrument(createInstrument(39, 7, "MG Blip Bs :"));
        addInstrument(createInstrument(39, 8, "Synth Bass 4"));
        addInstrument(createInstrument(39, 9, "Dry Bass"));
        addInstrument(createInstrument(39, 10, "X Wire Bass"));
        addInstrument(createInstrument(39, 11, "Wire String Bass"));
        addInstrument(createInstrument(39, 12, "Blip Bass :"));
        addInstrument(createInstrument(39, 13, "Rubber Bass 1"));
        addInstrument(createInstrument(39, 16, "Rubber Bass"));
        addInstrument(createInstrument(39, 17, "SH101 Bass 1"));
        addInstrument(createInstrument(39, 18, "SH101 Bass 2"));
        addInstrument(createInstrument(39, 19, "Smooth Bass"));
        addInstrument(createInstrument(39, 20, "SH101 Bass 3"));
        addInstrument(createInstrument(39, 21, "Spike Bass"));
        addInstrument(createInstrument(39, 22, "House Bass :"));
        addInstrument(createInstrument(39, 23, "KG Bass"));
        addInstrument(createInstrument(39, 24, "Sync Bass"));
        addInstrument(createInstrument(39, 25, "MG 5th Bass"));
        addInstrument(createInstrument(39, 26, "RND Bass"));
        addInstrument(createInstrument(39, 27, "Wow MG Bass"));
        addInstrument(createInstrument(39, 28, "Bubble Bass"));
        addInstrument(createInstrument(40, 0, "Violin"));
        addInstrument(createInstrument(40, 1, "Violin Atk :"));
        addInstrument(createInstrument(40, 8, "Slow Violin"));
        addInstrument(createInstrument(41, 0, "Viola"));
        addInstrument(createInstrument(41, 1, "Viola Atk :"));
        addInstrument(createInstrument(42, 0, "Cello"));
        addInstrument(createInstrument(42, 1, "Cello Atk :"));
        addInstrument(createInstrument(43, 0, "Contrabass"));
        addInstrument(createInstrument(44, 0, "Tremolo Strings"));
        addInstrument(createInstrument(44, 8, "Slow Tremolo"));
        addInstrument(createInstrument(44, 9, "Suspense Strings"));
        addInstrument(createInstrument(45, 0, "Pizzicato Strings"));
        addInstrument(createInstrument(45, 1, "Vcs & Cbs Pizzicato"));
        addInstrument(createInstrument(45, 2, "Chamber Pizzicato"));
        addInstrument(createInstrument(45, 3, "St. Pizzicato"));
        addInstrument(createInstrument(45, 8, "Solo Pizzicato"));
        addInstrument(createInstrument(45, 16, "Solo Spic"));
        addInstrument(createInstrument(46, 0, "Harp"));
        addInstrument(createInstrument(46, 16, "Synth Harp"));
        addInstrument(createInstrument(47, 0, "Timpani"));
        addInstrument(createInstrument(48, 0, "Strings"));
        addInstrument(createInstrument(48, 1, "Strings 2"));
        addInstrument(createInstrument(48, 2, "Chamber Strings :"));
        addInstrument(createInstrument(48, 3, "Cello sect."));
        addInstrument(createInstrument(48, 8, "Orchestra"));
        addInstrument(createInstrument(48, 9, "Orchestra 2"));
        addInstrument(createInstrument(48, 10, "Tremolo Orchestra"));
        addInstrument(createInstrument(48, 11, "Choir Strings"));
        addInstrument(createInstrument(48, 12, "Strings + Horn"));
        addInstrument(createInstrument(48, 16, "St. Strings"));
        addInstrument(createInstrument(48, 24, "Velo Strings"));
        addInstrument(createInstrument(48, 32, "Octave Strings 1"));
        addInstrument(createInstrument(48, 33, "Octave Strings 2"));
        addInstrument(createInstrument(49, 0, "Slow Strings"));
        addInstrument(createInstrument(49, 1, "Slow Strings 2"));
        addInstrument(createInstrument(49, 8, "Legato Strings"));
        addInstrument(createInstrument(49, 9, "Warm Strings"));
        addInstrument(createInstrument(49, 10, "St. Slow Strings"));
        addInstrument(createInstrument(50, 0, "Synth Strings1"));
        addInstrument(createInstrument(50, 1, "OB Strings"));
        addInstrument(createInstrument(50, 2, "Stack Strings"));
        addInstrument(createInstrument(50, 3, "JP Strings"));
        addInstrument(createInstrument(50, 8, "Synth Strings 3"));
        addInstrument(createInstrument(50, 9, "Synth Strings 4"));
        addInstrument(createInstrument(50, 16, "High Strings"));
        addInstrument(createInstrument(50, 17, "Hybrid Strings"));
        addInstrument(createInstrument(50, 24, "Tron Strings"));
        addInstrument(createInstrument(50, 25, "Noise Strings"));
        addInstrument(createInstrument(51, 0, "Synth Strings2"));
        addInstrument(createInstrument(51, 1, "Synth Strings5"));
        addInstrument(createInstrument(51, 2, "JUNO Strings"));
        addInstrument(createInstrument(51, 8, "Air Strings"));
        addInstrument(createInstrument(52, 0, "Choir Aahs"));
        addInstrument(createInstrument(52, 8, "St. Choir"));
        addInstrument(createInstrument(52, 9, "Mello Choir"));
        addInstrument(createInstrument(52, 10, "Church Choir"));
        addInstrument(createInstrument(52, 16, "Choir Hahs"));
        addInstrument(createInstrument(52, 24, "Chorus Lahs"));
        addInstrument(createInstrument(52, 32, "Choir Aahs 2"));
        addInstrument(createInstrument(52, 33, "Male Aah + String"));
        addInstrument(createInstrument(53, 0, "Voice Oohs"));
        addInstrument(createInstrument(53, 8, "Voice Dahs"));
        addInstrument(createInstrument(54, 0, "Synth Vox"));
        addInstrument(createInstrument(54, 8, "Synth Voice"));
        addInstrument(createInstrument(54, 9, "Silent Night"));
        addInstrument(createInstrument(54, 16, "VP330 Choir"));
        addInstrument(createInstrument(54, 17, "Vinyl Choir"));
        addInstrument(createInstrument(55, 0, "Orchestra Hit"));
        addInstrument(createInstrument(55, 8, "Impact Hit"));
        addInstrument(createInstrument(55, 9, "Philly Hit"));
        addInstrument(createInstrument(55, 10, "Double Hit"));
        addInstrument(createInstrument(55, 11, "Percussive Hit"));
        addInstrument(createInstrument(55, 12, "Shock Wave"));
        addInstrument(createInstrument(55, 16, "Lo Fi Rave"));
        addInstrument(createInstrument(55, 17, "Techno Hit"));
        addInstrument(createInstrument(55, 18, "Dist. Hit"));
        addInstrument(createInstrument(55, 19, "Bam Hit"));
        addInstrument(createInstrument(55, 20, "Bit Hit"));
        addInstrument(createInstrument(55, 21, "Bim Hit"));
        addInstrument(createInstrument(55, 22, "Technorg Hit"));
        addInstrument(createInstrument(55, 23, "Rave Hit"));
        addInstrument(createInstrument(55, 24, "Strings Hit"));
        addInstrument(createInstrument(55, 25, "Stack Hit"));
        addInstrument(createInstrument(56, 0, "Trumpet"));
        addInstrument(createInstrument(56, 1, "Trumpet 2"));
        addInstrument(createInstrument(56, 2, "Trumpet :"));
        addInstrument(createInstrument(56, 8, "Flugel Horn"));
        addInstrument(createInstrument(56, 16, "4th Trumpets"));
        addInstrument(createInstrument(56, 24, "Bright Trumpet"));
        addInstrument(createInstrument(56, 25, "Warm Trumpet"));
        addInstrument(createInstrument(56, 32, "Synth Trumpet"));
        addInstrument(createInstrument(57, 0, "Trombone"));
        addInstrument(createInstrument(57, 1, "Trombone 2"));
        addInstrument(createInstrument(57, 2, "Twin bones"));
        addInstrument(createInstrument(57, 8, "Bass Trombone"));
        addInstrument(createInstrument(58, 0, "Tuba"));
        addInstrument(createInstrument(58, 1, "Tuba 2"));
        addInstrument(createInstrument(59, 0, "Muted Trumpet"));
        addInstrument(createInstrument(59, 8, "Muted Horns"));
        addInstrument(createInstrument(60, 0, "French Horn"));
        addInstrument(createInstrument(60, 1, "French Horn2"));
        addInstrument(createInstrument(60, 2, "Horn + Orch"));
        addInstrument(createInstrument(60, 3, "Wide French Horns"));
        addInstrument(createInstrument(60, 8, "French Horn Solo"));
        addInstrument(createInstrument(60, 9, "Dual Horns"));
        addInstrument(createInstrument(60, 16, "Horn Orch"));
        addInstrument(createInstrument(60, 24, "French Horn Rip"));
        addInstrument(createInstrument(61, 0, "Brass 1"));
        addInstrument(createInstrument(61, 1, "Brass ff"));
        addInstrument(createInstrument(61, 2, "Bones Section"));
        addInstrument(createInstrument(61, 8, "Brass 2"));
        addInstrument(createInstrument(61, 9, "Brass 3"));
        addInstrument(createInstrument(61, 10, "Brass SFZ"));
        addInstrument(createInstrument(61, 16, "Brass Fall"));
        addInstrument(createInstrument(61, 17, "Trumpet Fall"));
        addInstrument(createInstrument(61, 24, "Octave Brass"));
        addInstrument(createInstrument(61, 25, "Brass + Reed"));
        addInstrument(createInstrument(62, 0, "Synth Brass 1"));
        addInstrument(createInstrument(62, 1, "Poly Brass"));
        addInstrument(createInstrument(62, 2, "Stack Brass"));
        addInstrument(createInstrument(62, 3, "SH-5 Brass"));
        addInstrument(createInstrument(62, 4, "MKS Brass"));
        addInstrument(createInstrument(62, 8, "Synth Brass 3"));
        addInstrument(createInstrument(62, 9, "Quack Brass"));
        addInstrument(createInstrument(62, 16, "Analog Brass 1"));
        addInstrument(createInstrument(62, 17, "Hybrid Brass"));
        addInstrument(createInstrument(63, 0, "Synth Brass 2"));
        addInstrument(createInstrument(63, 1, "Soft Brass"));
        addInstrument(createInstrument(63, 2, "Warm Brass"));
        addInstrument(createInstrument(63, 8, "Synth Brass 4"));
        addInstrument(createInstrument(63, 9, "OB Brass"));
        addInstrument(createInstrument(63, 10, "Reso Brass"));
        addInstrument(createInstrument(63, 16, "Analog Brass 2"));
        addInstrument(createInstrument(63, 17, "Velo Brass 2"));
        addInstrument(createInstrument(64, 0, "Soprano Sax"));
        addInstrument(createInstrument(64, 8, "Soprano Exp."));
        addInstrument(createInstrument(65, 0, "Alto Sax"));
        addInstrument(createInstrument(65, 8, "Hyper Alto"));
        addInstrument(createInstrument(65, 9, "Grow Sax"));
        addInstrument(createInstrument(65, 16, "Alto Sax + Trumpet"));
        addInstrument(createInstrument(66, 0, "Tenor Sax"));
        addInstrument(createInstrument(66, 1, "Tenor Sax :"));
        addInstrument(createInstrument(66, 8, "Breathy Tenor"));
        addInstrument(createInstrument(66, 9, "St. Tenor Sax"));
        addInstrument(createInstrument(67, 0, "Baritone Sax"));
        addInstrument(createInstrument(67, 1, "Baritone Sax :"));
        addInstrument(createInstrument(68, 0, "Oboe"));
        addInstrument(createInstrument(68, 8, "Oboe Exp."));
        addInstrument(createInstrument(68, 16, "Multi Reed"));
        addInstrument(createInstrument(69, 0, "English Horn"));
        addInstrument(createInstrument(70, 0, "Bassoon"));
        addInstrument(createInstrument(71, 0, "Clarinet"));
        addInstrument(createInstrument(71, 8, "Bass Clarinet"));
        addInstrument(createInstrument(71, 16, "Multi Wind"));
        addInstrument(createInstrument(72, 0, "Piccolo"));
        addInstrument(createInstrument(72, 1, "Piccolo :"));
        addInstrument(createInstrument(72, 8, "Nay"));
        addInstrument(createInstrument(72, 9, "Nay Tremolo"));
        addInstrument(createInstrument(72, 16, "Di"));
        addInstrument(createInstrument(73, 0, "Flute"));
        addInstrument(createInstrument(73, 1, "Flute 2 :"));
        addInstrument(createInstrument(73, 2, "Flute Exp."));
        addInstrument(createInstrument(73, 3, "Flute Travelso"));
        addInstrument(createInstrument(73, 8, "Flute + Violin"));
        addInstrument(createInstrument(73, 16, "Tron Flute"));
        addInstrument(createInstrument(74, 0, "Recorder"));
        addInstrument(createInstrument(75, 0, "Pan Flute"));
        addInstrument(createInstrument(75, 8, "Kawala"));
        addInstrument(createInstrument(75, 16, "Zampona"));
        addInstrument(createInstrument(75, 17, "Zampona Atk"));
        addInstrument(createInstrument(76, 0, "Bottle Blow"));
        addInstrument(createInstrument(77, 0, "Shakuhachi"));
        addInstrument(createInstrument(77, 1, "Shakuhachi2"));
        addInstrument(createInstrument(78, 0, "Whistle"));
        addInstrument(createInstrument(78, 1, "Whistle 2"));
        addInstrument(createInstrument(79, 0, "Ocarina"));
        addInstrument(createInstrument(80, 0, "Square Wave"));
        addInstrument(createInstrument(80, 1, "Square"));
        addInstrument(createInstrument(80, 2, "Hollow Mini"));
        addInstrument(createInstrument(80, 3, "Mellow FM"));
        addInstrument(createInstrument(80, 4, "CC Solo"));
        addInstrument(createInstrument(80, 5, "Shmoog"));
        addInstrument(createInstrument(80, 6, "LM Square"));
        addInstrument(createInstrument(80, 8, "Sine Wave"));
        addInstrument(createInstrument(80, 9, "Sine Lead"));
        addInstrument(createInstrument(80, 10, "KG Lead"));
        addInstrument(createInstrument(80, 16, "P5 Square"));
        addInstrument(createInstrument(80, 17, "OB Square"));
        addInstrument(createInstrument(80, 18, "JP-8 Square"));
        addInstrument(createInstrument(80, 24, "Pulse Lead"));
        addInstrument(createInstrument(80, 25, "JP8 Pulse Lead 1"));
        addInstrument(createInstrument(80, 26, "JP8 Pulse Lead 2"));
        addInstrument(createInstrument(80, 27, "MG Resonant Pulse"));
        addInstrument(createInstrument(81, 0, "Saw Wave"));
        addInstrument(createInstrument(81, 1, "Saw"));
        addInstrument(createInstrument(81, 2, "Pulse Saw"));
        addInstrument(createInstrument(81, 3, "Feline GR"));
        addInstrument(createInstrument(81, 4, "Big Lead"));
        addInstrument(createInstrument(81, 5, "Velo Lead"));
        addInstrument(createInstrument(81, 6, "GR-300"));
        addInstrument(createInstrument(81, 7, "LA Saw"));
        addInstrument(createInstrument(81, 8, "Doctor Solo"));
        addInstrument(createInstrument(81, 9, "Fat Saw Lead"));
        addInstrument(createInstrument(81, 11, "D-50 Fat Saw"));
        addInstrument(createInstrument(81, 16, "Waspy Synth"));
        addInstrument(createInstrument(81, 17, "PM Lead"));
        addInstrument(createInstrument(81, 18, "CS Saw Lead"));
        addInstrument(createInstrument(81, 24, "MG Saw 1"));
        addInstrument(createInstrument(81, 25, "MG Saw 2"));
        addInstrument(createInstrument(81, 26, "OB Saw 1"));
        addInstrument(createInstrument(81, 27, "OB Saw 2"));
        addInstrument(createInstrument(81, 28, "D-50 Saw"));
        addInstrument(createInstrument(81, 29, "SH-101 Saw"));
        addInstrument(createInstrument(81, 30, "CS Saw"));
        addInstrument(createInstrument(81, 31, "MG Saw Lead"));
        addInstrument(createInstrument(81, 32, "OB Saw Lead"));
        addInstrument(createInstrument(81, 33, "P5 Saw Lead"));
        addInstrument(createInstrument(81, 34, "MG unison"));
        addInstrument(createInstrument(81, 35, "Octave Saw Lead"));
        addInstrument(createInstrument(81, 40, "Sequence Saw 1"));
        addInstrument(createInstrument(81, 41, "Sequence Saw 2"));
        addInstrument(createInstrument(81, 42, "Reso Saw"));
        addInstrument(createInstrument(81, 43, "Cheese Saw 1"));
        addInstrument(createInstrument(81, 44, "Cheese Saw 2"));
        addInstrument(createInstrument(81, 45, "Rhythmic Saw"));
        addInstrument(createInstrument(82, 0, "Synth Calliope"));
        addInstrument(createInstrument(82, 1, "Vent Synth"));
        addInstrument(createInstrument(82, 2, "Pure Pan Lead"));
        addInstrument(createInstrument(83, 0, "Chiffer Lead"));
        addInstrument(createInstrument(83, 1, "TB Lead"));
        addInstrument(createInstrument(83, 8, "Mad Lead"));
        addInstrument(createInstrument(84, 0, "Charang"));
        addInstrument(createInstrument(84, 8, "Distorted Lead"));
        addInstrument(createInstrument(84, 9, "Acid Guitar1"));
        addInstrument(createInstrument(84, 10, "Acid Guitar2"));
        addInstrument(createInstrument(84, 16, "P5 Sync Lead"));
        addInstrument(createInstrument(84, 17, "Fat Sync Lead"));
        addInstrument(createInstrument(84, 18, "Rock Lead"));
        addInstrument(createInstrument(84, 19, "5th Deca Sync"));
        addInstrument(createInstrument(84, 20, "Dirty Sync"));
        addInstrument(createInstrument(84, 24, "JUNO Sub Osc"));
        addInstrument(createInstrument(85, 0, "Solo Vox"));
        addInstrument(createInstrument(85, 8, "Vox Lead"));
        addInstrument(createInstrument(85, 9, "LFO Vox"));
        addInstrument(createInstrument(86, 0, "5th Saw Wave"));
        addInstrument(createInstrument(86, 1, "Big Fives"));
        addInstrument(createInstrument(86, 2, "5th Lead"));
        addInstrument(createInstrument(86, 3, "5th Analog Clav"));
        addInstrument(createInstrument(86, 8, "4th Lead"));
        addInstrument(createInstrument(87, 0, "Bass & Lead"));
        addInstrument(createInstrument(87, 1, "Big & Raw"));
        addInstrument(createInstrument(87, 2, "Fat & Perky"));
        addInstrument(createInstrument(87, 3, "JUNO Rave"));
        addInstrument(createInstrument(87, 4, "JP8 Bass Lead 1"));
        addInstrument(createInstrument(87, 5, "JP8 Bass Lead 2"));
        addInstrument(createInstrument(87, 6, "SH-5 Bass Lead"));
        addInstrument(createInstrument(88, 0, "Fantasia"));
        addInstrument(createInstrument(88, 1, "Fantasia 2"));
        addInstrument(createInstrument(88, 2, "New Age Pad"));
        addInstrument(createInstrument(88, 3, "Bell Heaven"));
        addInstrument(createInstrument(89, 0, "Warm Pad"));
        addInstrument(createInstrument(89, 1, "Thick Pad"));
        addInstrument(createInstrument(89, 2, "Horn Pad"));
        addInstrument(createInstrument(89, 3, "Rotary String"));
        addInstrument(createInstrument(89, 4, "Soft Pad"));
        addInstrument(createInstrument(89, 8, "Octave Pad"));
        addInstrument(createInstrument(89, 9, "Stack Pad"));
        addInstrument(createInstrument(90, 0, "Polysynth"));
        addInstrument(createInstrument(90, 1, "80's Polysynth"));
        addInstrument(createInstrument(90, 2, "Polysynth 2"));
        addInstrument(createInstrument(90, 3, "Poly King"));
        addInstrument(createInstrument(90, 8, "Power Stack"));
        addInstrument(createInstrument(90, 9, "Octave Stack"));
        addInstrument(createInstrument(90, 10, "Resonant Stack"));
        addInstrument(createInstrument(90, 11, "Techno Stack"));
        addInstrument(createInstrument(91, 0, "Space Voice"));
        addInstrument(createInstrument(91, 1, "Heaven II"));
        addInstrument(createInstrument(91, 2, "SC Heaven"));
        addInstrument(createInstrument(91, 8, "Cosmic Voice"));
        addInstrument(createInstrument(91, 9, "Auh Vox"));
        addInstrument(createInstrument(91, 10, "Auh Auh"));
        addInstrument(createInstrument(91, 11, "Vocoderman"));
        addInstrument(createInstrument(92, 0, "Bowed Glass"));
        addInstrument(createInstrument(92, 1, "Soft Bell Pad"));
        addInstrument(createInstrument(92, 2, "JP8 Square Pad"));
        addInstrument(createInstrument(92, 3, "7th Bell Pad"));
        addInstrument(createInstrument(93, 0, "Metal Pad"));
        addInstrument(createInstrument(93, 1, "Tine Pad"));
        addInstrument(createInstrument(93, 2, "Panner Pad"));
        addInstrument(createInstrument(94, 0, "Halo Pad"));
        addInstrument(createInstrument(94, 1, "Vox Pad"));
        addInstrument(createInstrument(94, 2, "Vox Sweep"));
        addInstrument(createInstrument(94, 8, "Horror Pad"));
        addInstrument(createInstrument(95, 0, "Sweep Pad"));
        addInstrument(createInstrument(95, 1, "Polar Pad"));
        addInstrument(createInstrument(95, 8, "Converge"));
        addInstrument(createInstrument(95, 9, "Shwimmer"));
        addInstrument(createInstrument(95, 10, "Celestial Pd"));
        addInstrument(createInstrument(95, 11, "Bag Sweep"));
        addInstrument(createInstrument(96, 0, "Ice Rain"));
        addInstrument(createInstrument(96, 1, "Harmo Rain"));
        addInstrument(createInstrument(96, 2, "African Wood"));
        addInstrument(createInstrument(96, 3, "Anklung Pad"));
        addInstrument(createInstrument(96, 4, "Rattle Pad"));
        addInstrument(createInstrument(96, 8, "Clavi Pad"));
        addInstrument(createInstrument(97, 0, "Soundtrack"));
        addInstrument(createInstrument(97, 1, "Ancestral"));
        addInstrument(createInstrument(97, 2, "Prologue"));
        addInstrument(createInstrument(97, 3, "Prologue 2"));
        addInstrument(createInstrument(97, 4, "Hols Strings"));
        addInstrument(createInstrument(97, 8, "Rave"));
        addInstrument(createInstrument(98, 0, "Crystal"));
        addInstrument(createInstrument(98, 1, "Synth Mallet"));
        addInstrument(createInstrument(98, 2, "Soft Crystal"));
        addInstrument(createInstrument(98, 3, "Round Glock"));
        addInstrument(createInstrument(98, 4, "Loud Glock"));
        addInstrument(createInstrument(98, 5, "Glocken Chime"));
        addInstrument(createInstrument(98, 6, "Clear Bells"));
        addInstrument(createInstrument(98, 7, "Christmas Bell"));
        addInstrument(createInstrument(98, 8, "Vibra Bells"));
        addInstrument(createInstrument(98, 9, "Digi Bells"));
        addInstrument(createInstrument(98, 10, "Music Bell"));
        addInstrument(createInstrument(98, 11, "Analog Bell"));
        addInstrument(createInstrument(98, 16, "Choral Bells"));
        addInstrument(createInstrument(98, 17, "Air Bells"));
        addInstrument(createInstrument(98, 18, "Bell Harp"));
        addInstrument(createInstrument(98, 19, "Gamelimba"));
        addInstrument(createInstrument(98, 20, "JUNO Bell"));
        addInstrument(createInstrument(99, 0, "Atmosphere"));
        addInstrument(createInstrument(99, 1, "Warm Atmosphere"));
        addInstrument(createInstrument(99, 2, "Nylon Harp"));
        addInstrument(createInstrument(99, 3, "Harpvox"));
        addInstrument(createInstrument(99, 4, "Hollow Release"));
        addInstrument(createInstrument(99, 5, "Nylon + Rhodes"));
        addInstrument(createInstrument(99, 6, "Ambient Pad"));
        addInstrument(createInstrument(99, 7, "Invisible"));
        addInstrument(createInstrument(99, 8, "Pulsey Key"));
        addInstrument(createInstrument(99, 9, "Noise Piano"));
        addInstrument(createInstrument(100, 0, "Brightness"));
        addInstrument(createInstrument(100, 1, "Shining Star"));
        addInstrument(createInstrument(100, 2, "OB Stab"));
        addInstrument(createInstrument(100, 8, "Organ Bell"));
        addInstrument(createInstrument(101, 0, "Goblin"));
        addInstrument(createInstrument(101, 1, "Goblinson"));
        addInstrument(createInstrument(101, 2, "50's Sci-Fi"));
        addInstrument(createInstrument(101, 3, "Abduction"));
        addInstrument(createInstrument(101, 4, "Auhbient"));
        addInstrument(createInstrument(101, 5, "LFO Pad"));
        addInstrument(createInstrument(101, 6, "Random String"));
        addInstrument(createInstrument(101, 7, "Random Pad"));
        addInstrument(createInstrument(101, 8, "Low Birds Pad"));
        addInstrument(createInstrument(101, 9, "Falling Down"));
        addInstrument(createInstrument(101, 10, "LFO RAVE"));
        addInstrument(createInstrument(101, 11, "LFO Horror"));
        addInstrument(createInstrument(101, 12, "LFO Techno"));
        addInstrument(createInstrument(101, 13, "Alternative"));
        addInstrument(createInstrument(101, 14, "UFO FX"));
        addInstrument(createInstrument(101, 15, "Gargle Man"));
        addInstrument(createInstrument(101, 16, "Sweep FX"));
        addInstrument(createInstrument(102, 0, "Echo Drops"));
        addInstrument(createInstrument(102, 1, "Echo Bell"));
        addInstrument(createInstrument(102, 2, "Echo Pan"));
        addInstrument(createInstrument(102, 3, "Echo Pan 2"));
        addInstrument(createInstrument(102, 4, "Big Panner"));
        addInstrument(createInstrument(102, 5, "Reso Panner"));
        addInstrument(createInstrument(102, 6, "Water Piano"));
        addInstrument(createInstrument(102, 8, "Pan Sequence"));
        addInstrument(createInstrument(102, 9, "Aqua"));
        addInstrument(createInstrument(103, 0, "Star Theme"));
        addInstrument(createInstrument(103, 1, "Star Theme 2"));
        addInstrument(createInstrument(103, 8, "Dream Pad"));
        addInstrument(createInstrument(103, 9, "Silky Pad"));
        addInstrument(createInstrument(103, 16, "New Century"));
        addInstrument(createInstrument(103, 17, "7th Atmosphere"));
        addInstrument(createInstrument(103, 18, "Galaxy Way"));
        addInstrument(createInstrument(104, 0, "Sitar"));
        addInstrument(createInstrument(104, 1, "Sitar 2"));
        addInstrument(createInstrument(104, 2, "Detune Sitar"));
        addInstrument(createInstrument(104, 3, "Sitar 3"));
        addInstrument(createInstrument(104, 8, "Tambra"));
        addInstrument(createInstrument(104, 16, "Tamboura"));
        addInstrument(createInstrument(105, 0, "Banjo"));
        addInstrument(createInstrument(105, 1, "Muted Banjo"));
        addInstrument(createInstrument(105, 8, "Rabab"));
        addInstrument(createInstrument(105, 9, "San Xian"));
        addInstrument(createInstrument(105, 16, "Gopichant"));
        addInstrument(createInstrument(105, 24, "Oud"));
        addInstrument(createInstrument(105, 28, "Oud + Strings"));
        addInstrument(createInstrument(105, 32, "Pi Pa"));
        addInstrument(createInstrument(106, 0, "Shamisen"));
        addInstrument(createInstrument(106, 1, "Tsugaru"));
        addInstrument(createInstrument(106, 8, "Synth Shamisen"));
        addInstrument(createInstrument(107, 0, "Koto"));
        addInstrument(createInstrument(107, 1, "Gu Zheng"));
        addInstrument(createInstrument(107, 8, "Taisho Koto"));
        addInstrument(createInstrument(107, 16, "Kanoon"));
        addInstrument(createInstrument(107, 19, "Kanoon + Choir"));
        addInstrument(createInstrument(107, 24, "Octave Harp"));
        addInstrument(createInstrument(108, 0, "Kalimba"));
        addInstrument(createInstrument(108, 8, "Sanza"));
        addInstrument(createInstrument(109, 0, "Bag Pipe"));
        addInstrument(createInstrument(109, 8, "Didgeridoo"));
        addInstrument(createInstrument(110, 0, "Fiddle"));
        addInstrument(createInstrument(110, 8, "Er Hu"));
        addInstrument(createInstrument(110, 9, "Gao Hu"));
        addInstrument(createInstrument(111, 0, "Shannai"));
        addInstrument(createInstrument(111, 1, "Shannai 2"));
        addInstrument(createInstrument(111, 8, "Pungi"));
        addInstrument(createInstrument(111, 16, "Hichiriki"));
        addInstrument(createInstrument(111, 24, "Mizmar"));
        addInstrument(createInstrument(111, 32, "Suona 1"));
        addInstrument(createInstrument(111, 33, "Suona 2"));
        addInstrument(createInstrument(112, 0, "Tinkle Bell"));
        addInstrument(createInstrument(112, 8, "Bonang"));
        addInstrument(createInstrument(112, 9, "Gender"));
        addInstrument(createInstrument(112, 10, "Gamelan Gong"));
        addInstrument(createInstrument(112, 11, "St. Gamelan"));
        addInstrument(createInstrument(112, 12, "Jang Gu"));
        addInstrument(createInstrument(112, 16, "RAMA Cymbal"));
        addInstrument(createInstrument(113, 0, "Agogo"));
        addInstrument(createInstrument(113, 8, "Atarigane"));
        addInstrument(createInstrument(113, 16, "Tambourine"));
        addInstrument(createInstrument(114, 0, "Steel Drums"));
        addInstrument(createInstrument(114, 1, "Island Mallet"));
        addInstrument(createInstrument(115, 0, "Woodblock"));
        addInstrument(createInstrument(115, 8, "Castanets"));
        addInstrument(createInstrument(115, 16, "Angklung"));
        addInstrument(createInstrument(115, 17, "Angkl Rhythm"));
        addInstrument(createInstrument(115, 24, "Finger Snaps"));
        addInstrument(createInstrument(115, 32, "909 Hand Clap"));
        addInstrument(createInstrument(116, 0, "Taiko"));
        addInstrument(createInstrument(116, 1, "Small Taiko"));
        addInstrument(createInstrument(116, 8, "Concert BD"));
        addInstrument(createInstrument(116, 16, "Jungle BD"));
        addInstrument(createInstrument(116, 17, "Techno BD"));
        addInstrument(createInstrument(116, 18, "Bounce"));
        addInstrument(createInstrument(117, 0, "Melodic Tom 1"));
        addInstrument(createInstrument(117, 1, "Real Tom"));
        addInstrument(createInstrument(117, 8, "Melodic Tom 2"));
        addInstrument(createInstrument(117, 9, "Rock Tom"));
        addInstrument(createInstrument(117, 16, "Rash SD"));
        addInstrument(createInstrument(117, 17, "House SD"));
        addInstrument(createInstrument(117, 18, "Jungle SD"));
        addInstrument(createInstrument(117, 19, "909 SD"));
        addInstrument(createInstrument(118, 0, "Synth Drum"));
        addInstrument(createInstrument(118, 8, "808 Tom"));
        addInstrument(createInstrument(118, 9, "Elec Perc"));
        addInstrument(createInstrument(118, 10, "Sine Perc"));
        addInstrument(createInstrument(118, 11, "606 Tom"));
        addInstrument(createInstrument(118, 12, "909 Tom"));
        addInstrument(createInstrument(119, 0, "Reverse Cymbal"));
        addInstrument(createInstrument(119, 1, "Reverse Cym2"));
        addInstrument(createInstrument(119, 2, "Reverse Cym3"));
        addInstrument(createInstrument(119, 8, "Reverse Snare 1"));
        addInstrument(createInstrument(119, 9, "Reverse Snare 2"));
        addInstrument(createInstrument(119, 16, "Reverse Kick 1"));
        addInstrument(createInstrument(119, 17, "Reverse Con BD"));
        addInstrument(createInstrument(119, 24, "Reverse Tom 1"));
        addInstrument(createInstrument(119, 25, "Reverse Tom 2"));
        addInstrument(createInstrument(120, 0, "Guitar Fret Noise"));
        addInstrument(createInstrument(120, 1, "Guitar Cut Noise"));
        addInstrument(createInstrument(120, 2, "String Slap"));
        addInstrument(createInstrument(120, 3, "Guitar Cut Noise 2"));
        addInstrument(createInstrument(120, 4, "Distorted Cut Noise"));
        addInstrument(createInstrument(120, 5, "Bass Slide"));
        addInstrument(createInstrument(120, 6, "Pick Scrape"));
        addInstrument(createInstrument(120, 8, "Guitar FX Menu"));
        addInstrument(createInstrument(120, 9, "Bartok Pizzicato"));
        addInstrument(createInstrument(120, 10, "Guitar Slap"));
        addInstrument(createInstrument(120, 11, "Chord Stroke"));
        addInstrument(createInstrument(120, 12, "Biwa Stroke"));
        addInstrument(createInstrument(120, 13, "Biwa Tremolo"));
        addInstrument(createInstrument(121, 0, "Breath Noise"));
        addInstrument(createInstrument(121, 1, "Flute Key Click"));
        addInstrument(createInstrument(122, 0, "Seashore"));
        addInstrument(createInstrument(122, 1, "Rain"));
        addInstrument(createInstrument(122, 2, "Thunder"));
        addInstrument(createInstrument(122, 3, "Wind"));
        addInstrument(createInstrument(122, 4, "Stream"));
        addInstrument(createInstrument(122, 5, "Bubble"));
        addInstrument(createInstrument(122, 6, "Wind 2"));
        addInstrument(createInstrument(122, 16, "Pink Noise"));
        addInstrument(createInstrument(122, 17, "White Noise"));
        addInstrument(createInstrument(123, 0, "Bird"));
        addInstrument(createInstrument(123, 1, "Dog"));
        addInstrument(createInstrument(123, 2, "Horse-Gallop"));
        addInstrument(createInstrument(123, 3, "Bird 2"));
        addInstrument(createInstrument(123, 4, "Kitty"));
        addInstrument(createInstrument(123, 5, "Growl"));
        addInstrument(createInstrument(124, 0, "Telephone 1"));
        addInstrument(createInstrument(124, 1, "Telephone 2"));
        addInstrument(createInstrument(124, 2, "Door Creaking"));
        addInstrument(createInstrument(124, 3, "Door Slam"));
        addInstrument(createInstrument(124, 4, "Scratch"));
        addInstrument(createInstrument(124, 5, "Wind Chimes"));
        addInstrument(createInstrument(124, 7, "Scratch 2"));
        addInstrument(createInstrument(124, 8, "Scratch Key"));
        addInstrument(createInstrument(124, 9, "Tape Rewind"));
        addInstrument(createInstrument(124, 10, "Phono Noise"));
        addInstrument(createInstrument(124, 11, "MC-500 Beep"));
        addInstrument(createInstrument(125, 0, "Helicopter"));
        addInstrument(createInstrument(125, 1, "Car-Engine"));
        addInstrument(createInstrument(125, 2, "Car-Stop"));
        addInstrument(createInstrument(125, 3, "Car-Pass"));
        addInstrument(createInstrument(125, 4, "Car-Crash"));
        addInstrument(createInstrument(125, 5, "Siren"));
        addInstrument(createInstrument(125, 6, "Train"));
        addInstrument(createInstrument(125, 7, "Jetplane"));
        addInstrument(createInstrument(125, 8, "Starship"));
        addInstrument(createInstrument(125, 9, "Burst Noise"));
        addInstrument(createInstrument(125, 10, "Calculating"));
        addInstrument(createInstrument(125, 11, "Percussive Bang"));
        addInstrument(createInstrument(126, 0, "Applause"));
        addInstrument(createInstrument(126, 1, "Laughing"));
        addInstrument(createInstrument(126, 2, "Screaming"));
        addInstrument(createInstrument(126, 3, "Punch"));
        addInstrument(createInstrument(126, 4, "Heart Beat"));
        addInstrument(createInstrument(126, 5, "Footsteps"));
        addInstrument(createInstrument(126, 6, "Applause 2"));
        addInstrument(createInstrument(126, 7, "Small Club"));
        addInstrument(createInstrument(126, 8, "Applause Wave"));
        addInstrument(createInstrument(126, 16, "Voice One"));
        addInstrument(createInstrument(126, 17, "Voice Two"));
        addInstrument(createInstrument(126, 18, "Voice Three"));
        addInstrument(createInstrument(126, 19, "Voice Tah"));
        addInstrument(createInstrument(126, 20, "Voice Whey"));
        addInstrument(createInstrument(127, 0, "Gun Shot"));
        addInstrument(createInstrument(127, 1, "Machine Gun"));
        addInstrument(createInstrument(127, 2, "Lasergun"));
        addInstrument(createInstrument(127, 3, "Explosion"));
        addInstrument(createInstrument(127, 4, "Eruption"));
        addInstrument(createInstrument(127, 5, "Big Shot"));

        // IMPORTANT =======================================
        // Same as GM2 for the programChange. Reused 120 for MSB but in theory it is useless.
        // Normally GS accepts drums only on Midi channel 10, drumkit can't be selected via bankMSB/bankLSB to work on a different channel.
        // However GS defines some specific SysEx messages which can program a Drums channel on other than 10 => TO BE IMPLEMENTED !
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
     * Convenience method to reorder arguments.
     *
     * @return
     */
    private Instrument createInstrument(int pc, int msb, String name)
    {
        GM1Instrument gmIns = GMSynth.getInstance().getGM1Bank().getInstrument(pc); // GS's PC is directly compatible with GM1
        Instrument ins = new Instrument(name, null, new MidiAddress(pc, msb, 0, DEFAULT_BANK_SELECT_METHOD), null, gmIns);
        return ins;
    }

    /**
     * Convenience method to reorder arguments.
     *
     * @return
     */
    private Instrument createDrumsInstrument(DrumKit.Type t, DrumKit.KeyMap map, int pc, int msb, int lsb, String name)
    {
        return new GSDrumsInstrument(name, null, new MidiAddress(pc, msb, lsb, DEFAULT_BANK_SELECT_METHOD), new DrumKit(t, map), null);
    }

    /**
     * A special class to override getMidiMessages() because of GS limitation of only 1 drums channel
     */
    private class GSDrumsInstrument extends Instrument
    {
        public GSDrumsInstrument(String patchName, InstrumentBank<?> bank, MidiAddress ma, DrumKit kit, GM1Instrument substitute)
        {
            super(patchName, bank, ma, kit, substitute);
        }

        /**
         * Overridden to use GS SysEx messages to enable Drums on channel 8 (9 if channel drums is 10).
         * <p>
         *
         * @param channel
         * @return
         */
        @Override
        public MidiMessage[] getMidiMessages(int channel)
        {
            MidiMessage[] messages = null;
            if (channel == 8)
            {
                messages = new MidiMessage[2];      // Send SysEx message + PC

                // Found on the web: 
                // https://www.pgmusic.com/forums/ubbthreads.php?ubb=showflat&Number=490421
                // http://www.synthfont.com/SysEx.txt
                // "The following SysEx messages will allow you to set channel 9 or 11 to drums (in addition to channel 10). 
                // This will work for Roland GS compatible devices such as the Roland VSC and Roland SD-20.
                // Set channel 9 to drums: F0 41 10 42 12 40 19 15 02 10 F7
                // Set channel 11 to drums: F0 41 10 42 12 40 1A 15 02 0F F7
                byte[] bytes =
                {
                    (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x19, 0x15, 0x02, 0x10, (byte) 0xF7
                };
                SysexMessage sysMsg = new SysexMessage();
                try
                {
                    sysMsg.setMessage(bytes, bytes.length);
                } catch (InvalidMidiDataException ex)
                {
                    Exceptions.printStackTrace(ex);
                }
                messages[0] = sysMsg;
                LOGGER.log(Level.INFO, "getMidiMessages() (special GS Drums instrument) sending SysEx messages to enable drums on channel 8");   
            } else
            {
                messages = new MidiMessage[1];          // Send Only PC
            }

            // Send PC_ONLY : GS does not use bank select for drums channel
            messages[messages.length - 1] = buildMessage(ShortMessage.PROGRAM_CHANGE, channel, getMidiAddress().getProgramChange(), 0);

            return messages;
        }

    }
}
