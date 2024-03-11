/**
 * A MidiParserListener inspired by JFugue 5.0.
 *
 * Create your MidiParserListener to analyze a Midi file.
 *
 * Then add it to a MidiParser which will call the MidiParserListener methods.
 */

/*
 * JFugue, an Application Programming Interface (API) for Music Programming
 * http://www.jfugue.org
 *
 * Copyright (C) 2003-2014 David Koelle
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
package org.jjazz.midi.api.parser;

import org.jjazz.harmony.api.Note;

public interface MidiParserListener
{

    /**
     * Called when the parser first starts up, but before it starts parsing anything. Provides listeners with a chance to
     * initialize variables and get ready for the parser events.
     */
    public void beforeParsingStarts();

    /**
     * Called when the parser has parsed its last item. Provides listeners with a chance to clean up.
     */
    public void afterParsingFinished();

    /**
     * Called when the parser encounters the END META Event.
     *
     * @param positionInBeats
     */
    public void onMetaEndEvent(float positionInBeats);

    /**
     * Called when the parser encounters a new channel. known as a Voice). Tracks correspond to MIDI tracks/channels.
     *
     * @param channel the new track event that has been parsed
     */
    public void onChannelChanged(byte channel);

    /**
     * Called when the parser encounters a new instrument selection.
     *
     * @param progChange the MIDI instrument (program change) value that has been parsed
     */
    public void onInstrumentParsed(byte progChange, float positionInBeats);

    /**
     * Called when the parser encounters a new tempo selection.
     *
     * @param tempoBPM The new tempo value
     */
    public void onTempoChanged(int tempoBPM, float positionInBeats);

    /**
     * The first parameter is the number of beats per measure; The second parameter is the power by which 2 must be raised to
     * create the note that represents one beat. Example 1: For a 5/8 time signature, expect 5,3 (since 2^3 = 8) Example 2: For a
     * 4/4 time signature, expect 4,2 (since 2^2 = 4)
     */
    public void onTimeSignatureParsed(byte numerator, byte powerOfTwo, float positionInBeats);

    public void onPitchWheelParsed(byte lsb, byte msb, float positionInBeats);

    public void onChannelPressureParsed(byte pressure, float positionInBeats);

    public void onPolyphonicPressureParsed(byte key, byte pressure, float positionInBeats);

    public void onSystemExclusiveParsed(float positionInBeats, byte... bytes);

    public void onControllerEventParsed(byte controller, byte value, float positionInBeats);

    public void onLyricParsed(String lyric, float positionInBeats);

    public void onTrackNameParsed(String name, float positionInBeats);

    public void onTextParsed(String text, float positionInBeats);

    public void onMarkerParsed(String marker, float positionInBeats);

    public void onNoteParsed(Note note, float positionInBeats);

}
