/**
 * An adapter to facilitate creation of MidiParserListeners.
 * Inspired by JFugue 5.0.
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

public class MidiParserListenerAdapter implements MidiParserListener
{

    @Override
    public void beforeParsingStarts()
    {
    }

    @Override
    public void afterParsingFinished()
    {
    }

    @Override
    public void onChannelChanged(byte channel)
    {
    }

    @Override
    public void onInstrumentParsed(byte instrument, float positionInBeats)
    {
    }

    @Override
    public void onTempoChanged(int tempoBPM, float positionInBeats)
    {
    }

    @Override
    public void onTimeSignatureParsed(byte numerator, byte powerOfTwo, float positionInBeats)
    {
    }

    @Override
    public void onPitchWheelParsed(byte lsb, byte msb, float positionInBeats)
    {
    }

    @Override
    public void onChannelPressureParsed(byte pressure, float positionInBeats)
    {
    }

    @Override
    public void onPolyphonicPressureParsed(byte key, byte pressure, float positionInBeats)
    {
    }

    @Override
    public void onSystemExclusiveParsed(float positionInBeats, byte... bytes)
    {
    }

    @Override
    public void onControllerEventParsed(byte controller, byte value, float positionInBeats)
    {
    }

    @Override
    public void onLyricParsed(String lyric, float positionInBeats)
    {
    }

    @Override
    public void onTrackNameParsed(String name, float positionInBeats)
    {
    }

    @Override
    public void onTextParsed(String name, float positionInBeats)
    {
    }

    @Override
    public void onMarkerParsed(String marker, float positionInBeats)
    {
    }

    @Override
    public void onNoteParsed(Note note, float positionInBeats)
    {
    }

    @Override
    public void onMetaEndEvent(float positionInBeats)
    {
    }

}
