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
package org.jjazz.synthmanager;

import org.jjazz.midi.AbstractInstrumentBank;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;

/**
 * Sample InstrumentBank.
 */
public class TestBank2 extends AbstractInstrumentBank<Instrument>
{

    public TestBank2()
    {
        super("GM Backup 2", null, 12, 101, InstrumentBank.BankSelectMethod.LSB_ONLY);
        addInstrument(new Instrument(0, "Acoustic Piano"));
        addInstrument(new Instrument(1, "Bright Piano"));
        addInstrument(new Instrument(2, "El.Grand Piano"));
        addInstrument(new Instrument(3, "Honkey-Tonk"));
        addInstrument(new Instrument(4, "Electric Piano 1"));
        addInstrument(new Instrument(5, "Electric Piano 2"));
        addInstrument(new Instrument(6, "Harpsichord"));
        addInstrument(new Instrument(7, "Clavinet"));
        addInstrument(new Instrument(8, "Celesta"));
        addInstrument(new Instrument(9, "Glockenspiel"));
        addInstrument(new Instrument(10, "Music Box"));
        addInstrument(new Instrument(11, "Vibraphone"));
        addInstrument(new Instrument(12, "Marimba"));
        addInstrument(new Instrument(13, "Xylophone"));
        addInstrument(new Instrument(14, "Tubular Bells"));
        addInstrument(new Instrument(15, "Santur"));
        addInstrument(new Instrument(16, "Drawbar Organ 1"));
        addInstrument(new Instrument(17, "Percussive Or"));
        addInstrument(new Instrument(18, "Rock Or"));
        addInstrument(new Instrument(19, "Church Or"));
        addInstrument(new Instrument(20, "Reed Or"));
        addInstrument(new Instrument(21, "Accordian"));
        addInstrument(new Instrument(22, "Harmonica"));
        addInstrument(new Instrument(23, "Bandoneon"));
        addInstrument(new Instrument(24, "Nylon Guitar 1"));
        addInstrument(new Instrument(25, "Steel Guitar"));
        addInstrument(new Instrument(26, "Jazz Guitar"));
        addInstrument(new Instrument(27, "Clean Guitar"));
        addInstrument(new Instrument(28, "Muted Guitar"));
        addInstrument(new Instrument(29, "Overdrive Guitar"));
        addInstrument(new Instrument(30, "Distortion Guitar"));
        addInstrument(new Instrument(31, "Guitar Harmonics"));
        addInstrument(new Instrument(32, "Acoustic Bass"));
        addInstrument(new Instrument(33, "Fingered Bass"));
        addInstrument(new Instrument(34, "Picked Bass"));
        addInstrument(new Instrument(35, "Fretless Bass"));
        addInstrument(new Instrument(36, "Slap Bass 1"));
        addInstrument(new Instrument(37, "Slap Bass 2"));
        addInstrument(new Instrument(38, "Synth Bass 1"));
        addInstrument(new Instrument(39, "Synth Bass 2"));
        addInstrument(new Instrument(40, "Violin"));
        addInstrument(new Instrument(41, "Viola"));
        addInstrument(new Instrument(42, "Cello"));
        addInstrument(new Instrument(43, "ContraBass"));
        addInstrument(new Instrument(44, "Tremelo Strings"));
        addInstrument(new Instrument(45, "Pizzicato Strings"));
        addInstrument(new Instrument(46, "Orchestral Harp"));
        addInstrument(new Instrument(47, "Timpani"));
        addInstrument(new Instrument(48, "Strings"));
        addInstrument(new Instrument(49, "Slow Strings"));
        addInstrument(new Instrument(50, "Synth Strings 1"));
        addInstrument(new Instrument(51, "Synth Strings 2"));
        addInstrument(new Instrument(52, "Choir Ahhs 1"));
        addInstrument(new Instrument(53, "Voice Oohs"));
        addInstrument(new Instrument(54, "Synth Vox"));
        addInstrument(new Instrument(55, "Orchestra Hit"));
        addInstrument(new Instrument(56, "Trumpet"));
        addInstrument(new Instrument(57, "Trombone 1"));
        addInstrument(new Instrument(58, "Tuba"));
        addInstrument(new Instrument(59, "Muted Trumpet 1"));
        addInstrument(new Instrument(60, "French Horn"));
        addInstrument(new Instrument(61, "Brass Section 1"));
        addInstrument(new Instrument(62, "Synth Brass 1"));
        addInstrument(new Instrument(63, "Synth Brass 2"));
        addInstrument(new Instrument(64, "Soprano Sax"));
        addInstrument(new Instrument(65, "Alto Sax"));
        addInstrument(new Instrument(66, "Tenor Sax"));
        addInstrument(new Instrument(67, "Baritone Sax"));
        addInstrument(new Instrument(68, "Oboe"));
        addInstrument(new Instrument(69, "English Horn"));
        addInstrument(new Instrument(70, "Bassoon"));
        addInstrument(new Instrument(71, "Clarinet"));
        addInstrument(new Instrument(72, "Piccolo"));
        addInstrument(new Instrument(73, "Flute"));
        addInstrument(new Instrument(74, "Recorder"));
        addInstrument(new Instrument(75, "Pan Flute"));
        addInstrument(new Instrument(76, "Blown Bottle"));
        addInstrument(new Instrument(77, "Shakuhachi"));
        addInstrument(new Instrument(78, "Whistle"));
        addInstrument(new Instrument(79, "Ocarina"));
        addInstrument(new Instrument(80, "Detuned Square"));
        addInstrument(new Instrument(81, "Detuned Sawtooth"));
        addInstrument(new Instrument(82, "Synth Calliope"));
        addInstrument(new Instrument(83, "Chiff Lead"));
        addInstrument(new Instrument(84, "Charang"));
        addInstrument(new Instrument(85, "Air Voice"));
        addInstrument(new Instrument(86, "5th Sawtooth"));
        addInstrument(new Instrument(87, "Bass & Lead"));
        addInstrument(new Instrument(88, "Fantasia"));
        addInstrument(new Instrument(89, "Warm Pad"));
        addInstrument(new Instrument(90, "Polyphonic Synth"));
        addInstrument(new Instrument(91, "Space Voice"));
        addInstrument(new Instrument(92, "Bowed Glass"));
        addInstrument(new Instrument(93, "Mettalic Pad"));
        addInstrument(new Instrument(94, "Halo Pad"));
        addInstrument(new Instrument(95, "Sweep Pad"));
        addInstrument(new Instrument(96, "Ice Rain"));
        addInstrument(new Instrument(97, "Sound Track"));
        addInstrument(new Instrument(98, "Crystal"));
        addInstrument(new Instrument(99, "Atmosphere"));
        addInstrument(new Instrument(100, "Brightness"));
        addInstrument(new Instrument(101, "Goblins"));
        addInstrument(new Instrument(102, "Echo Drops"));
        addInstrument(new Instrument(103, "Star Theme"));
        addInstrument(new Instrument(104, "Sitar 1"));
        addInstrument(new Instrument(105, "Banjo"));
        addInstrument(new Instrument(106, "Shamisem"));
        addInstrument(new Instrument(107, "Koto"));
        addInstrument(new Instrument(108, "Kalimba"));
        addInstrument(new Instrument(109, "Bagpipe"));
        addInstrument(new Instrument(110, "Fiddle"));
        addInstrument(new Instrument(111, "Shanai"));
        addInstrument(new Instrument(112, "Tinkle Bell"));
        addInstrument(new Instrument(113, "Agogo"));
        addInstrument(new Instrument(114, "Steel Drums"));
        addInstrument(new Instrument(115, "Woodsection"));
        addInstrument(new Instrument(116, "Taiko"));
        addInstrument(new Instrument(117, "Melodic Tom 1"));
        addInstrument(new Instrument(118, "Synth Drum"));
        addInstrument(new Instrument(119, "Reverse Cymbal"));
        addInstrument(new Instrument(120, "Gtr.Fret Noise"));
        addInstrument(new Instrument(121, "Breath Noise"));
        addInstrument(new Instrument(122, "Seashore"));
        addInstrument(new Instrument(123, "Bird Tweet"));
        addInstrument(new Instrument(124, "Telephone Rin"));
        addInstrument(new Instrument(125, "Helicopter"));
        addInstrument(new Instrument(126, "Applause"));
        addInstrument(new Instrument(127, "GunShot"));
    }
}
