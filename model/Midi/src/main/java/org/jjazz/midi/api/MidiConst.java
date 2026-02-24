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
package org.jjazz.midi.api;

/**
 * Midi Constants
 */

public class MidiConst
{

    private static final String[] percussionMap = new String[82];
    private static final String[] percussions = new String[47];

    /**
     * Pulsation Per Quarter --
     */
    public static final int PPQ_RESOLUTION = 960;            // Some Midi file editors don't handle more than 960

    /**
     * Fixed sequencer reference tempo in BPM.
     * <p>
     * To enable interactive tempo changes during playback, sequencer tempo is set to a fixed value so that we can use Sequencer.setTempoFactor() instead of
     * Sequencer.setTempoInBPM().
     */
    public static final int SEQUENCER_REF_TEMPO = 120;  // Must be 120: this avoids side effects with JDK RealTimeSequencer bug :
    // tempo resets at 120 upon start! 
    // See StackOverflow https://stackoverflow.com/questions/37935814/pausing-java-sequencer-resets-tempo

    // Control Changes values
    public static final int CTRL_CHG_BANK_SELECT_MSB = 0;
    public static final int CTRL_CHG_MODULATION_MSB = 1;
    public static final int CTRL_CHG_VOLUME_MSB = 7;
    public static final int CTRL_CHG_PAN_MSB = 10;
    public static final int CTRL_CHG_EXPRESSION_MSB = 11;
    public static final int CTRL_CHG_BANK_SELECT_LSB = 32;
    public static final int CTRL_CHG_SUSTAIN = 64;
    public static final int CTRL_CHG_REVERB_DEPTH = 91;
    public static final int CTRL_CHG_CHORUS_DEPTH = 93;
    public static final int CTRL_CHG_JJAZZ_TEMPO_FACTOR = 114;  // Used for tempo changes during song
    public static final int CTRL_CHG_ALL_SOUND_OFF = 120;
    public static final int CTRL_CHG_RESET_ALL_CONTROLLERS = 121;
    public static final int CTRL_CHG_ALL_NOTES_OFF = 123;


    // Meta event type values
    public static final int META_TEXT = 1;
    public static final int META_COPYRIGHT = 2;
    public static final int META_TRACKNAME = 3;
    public static final int META_INSTRUMENT = 4;
    public static final int META_LYRICS = 5;
    public static final int META_MARKER = 6;
    public static final int META_END_OF_TRACK = 47;
    public static final int META_TEMPO = 81;
    public static final int META_TIME_SIGNATURE = 88;


    // SysEx
    public static final int VOLUME_STD = 100;
    public static final int PANORAMIC_STD = 64;
    public static final int CHORUS_STD = 8;
    public static final int REVERB_STD = 20;
    public static final int CHANNEL_MIN = 0;
    public static final int CHANNEL_MAX = 15;
    public static final int CHANNEL_DRUMS = 9;
    public static final int[] CHANNELS_ALL = new int[]
    {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    };

    // GM Percussion map
    public static final int ACOUSTIC_BASS_DRUM = 35;
    public static final int BASS_DRUM_1 = 36;
    public static final int SIDE_STICK = 37;
    public static final int ACOUSTIC_SNARE = 38;
    public static final int HAND_CLAP = 39;
    public static final int ELECTRIC_SNARE = 40;
    public static final int LOW_FLOOR_TOM = 41;
    public static final int CLOSED_HI_HAT = 42;
    public static final int HIGH_FLOOR_TOM = 43;
    public static final int PEDAL_HI_HAT = 44;
    public static final int LOW_TOM = 45;
    public static final int OPEN_HI_HAT = 46;
    public static final int LOW_MID_TOM = 47;
    public static final int HI_MID_TOM = 48;
    public static final int CRASH_CYMBAL_1 = 49;
    public static final int HIGH_TOM = 50;
    public static final int RIDE_CYMBAL_1 = 51;
    public static final int CHINESE_CYMBAL = 52;
    public static final int RIDE_BELL = 53;
    public static final int TAMBOURINE = 54;
    public static final int SPLASH_CYMBAL = 55;
    public static final int COWBELL = 56;
    public static final int CRASH_CYMBAL_2 = 57;
    public static final int VIBRASLAP = 58;
    public static final int RIDE_CYMBAL_2 = 59;
    public static final int HI_BONGO = 60;
    public static final int LOW_BONGO = 61;
    public static final int MUTE_HI_CONGA = 62;
    public static final int OPEN_HI_CONGA = 63;
    public static final int LOW_CONGA = 64;
    public static final int HIGH_TIMBALE = 65;
    public static final int LOW_TIMBALE = 66;
    public static final int HIGH_AGOGO = 67;
    public static final int LOW_AGOGO = 68;
    public static final int CABASA = 69;
    public static final int MARACAS = 70;
    public static final int SHORT_WHISTLE = 71;
    public static final int LONG_WHISTLE = 72;
    public static final int SHORT_GUIRO = 73;
    public static final int LONG_GUIRO = 74;
    public static final int CLAVES = 75;
    public static final int HI_WOOD_SECTION = 76;
    public static final int LOW_WOOD_SECTION = 77;
    public static final int MUTE_CUICA = 78;
    public static final int OPEN_CUICA = 79;
    public static final int MUTE_TRIANGLE = 80;
    public static final int OPEN_TRIANGLE = 81;

    /**
     *
     * @return An array with the 47 GM percussion names.
     */
    static public String[] getGMPercussions()
    {
        if (percussions[0] == null)
        {
            for (int i = 35; i <= 81; i++)
            {
                percussions[i - 35] = getGMPercussionMap()[i];
            }
        }
        return percussions;
    }

    /**
     * A String array (size=82) with percussion name for each note pitch.
     * <p>
     * Names start at pitch 35 until 81, so for index &lt; 35 array contains null values. E.g. getGMPercussionMap()[35] = "Acoustic Bass Drum" @return
     */
    static public String[] getGMPercussionMap()
    {
        if (percussionMap[35] == null)
        {
            percussionMap[35] = "Acoustic Bass Drum";
            percussionMap[36] = "Bass Drum 1";
            percussionMap[37] = "Side Stick";
            percussionMap[38] = "Acoustic Snare";
            percussionMap[39] = "Hand Clap";
            percussionMap[40] = "Electric Snare";
            percussionMap[41] = "Low Floor Tom";
            percussionMap[42] = "Closed Hi-Hat";
            percussionMap[43] = "High Floor Tom";
            percussionMap[44] = "Pedal Hi-Hat";
            percussionMap[45] = "Low Tom";
            percussionMap[46] = "Open Hi-Hat";
            percussionMap[47] = "Low-Mid Tom";
            percussionMap[48] = "Hi-Mid Tom";
            percussionMap[49] = "Crash Cymbal 1";
            percussionMap[50] = "High Tom";
            percussionMap[51] = "Ride Cymbal 1";
            percussionMap[52] = "Chinese Cymbal";
            percussionMap[53] = "Ride Bell";
            percussionMap[54] = "Tambourine";
            percussionMap[55] = "Splash Cymbal";
            percussionMap[56] = "Cowbell";
            percussionMap[57] = "Crash Cymbal 2";
            percussionMap[58] = "Vibraslap";
            percussionMap[59] = "Ride Cymbal 2";
            percussionMap[60] = "Hi Bongo";
            percussionMap[61] = "Low Bongo";
            percussionMap[62] = "Mute Hi Conga";
            percussionMap[63] = "Open Hi Conga";
            percussionMap[64] = "Low Conga";
            percussionMap[65] = "High Timbale";
            percussionMap[66] = "Low Timbale";
            percussionMap[67] = "High Agogo";
            percussionMap[68] = "Low Agogo";
            percussionMap[69] = "Cabasa";
            percussionMap[70] = "Maracas";
            percussionMap[71] = "Short Whistle";
            percussionMap[72] = "Long Whistle";
            percussionMap[73] = "Short Guiro";
            percussionMap[74] = "Long Guiro";
            percussionMap[75] = "Claves";
            percussionMap[76] = "Hi Wood Section";
            percussionMap[77] = "Low Wood Section";
            percussionMap[78] = "Mute Cuica";
            percussionMap[79] = "Open Cuica";
            percussionMap[80] = "Mute Triangle";
            percussionMap[81] = "Open Triangle ";
        }
        return percussionMap;
    }

    /**
     * Check that midiValue is in the Midi range [0;127].
     *
     * @param midiValue
     * @return
     */
    public static boolean check(int midiValue)
    {
        return (midiValue >= 0) && (midiValue <= 127);
    }

    /**
     * Return a value in the Midi range [0;127].
     *
     * @param midiValue
     * @return
     */
    public static int clamp(int midiValue)
    {
        return Math.clamp(midiValue, 0, 127);
    }

    public static boolean checkMidiChannel(int c)
    {
        return (c >= CHANNEL_MIN) && (c <= CHANNEL_MAX);
    }
}
