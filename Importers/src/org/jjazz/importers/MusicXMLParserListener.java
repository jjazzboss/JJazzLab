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
package org.jjazz.importers;

import java.util.logging.Logger;
import org.jjazz.importers.jfugue.Note;
import org.jjazz.importers.jfugue.ParserListener;

/**
 * A parser to retrieve information required to build a Song object.
 */
public class MusicXMLParserListener implements ParserListener
{

    private static final Logger LOGGER = Logger.getLogger(MusicXMLParserListener.class.getSimpleName());

    @Override
    public void beforeParsingStarts()
    {
        //
    }

    @Override
    public void afterParsingFinished()
    {
        //
    }

    @Override
    public void onTrackChanged(byte track)
    {
        //
        LOGGER.severe("onTrackChanged() track=" + track);
    }

    @Override
    public void onLayerChanged(byte layer)
    {
        //
        LOGGER.severe("onLayerChanged() layer=" + layer);
    }

    @Override
    public void onInstrumentParsed(byte instrument)
    {
        //
        LOGGER.severe("onInstrumentParsed() instrument=" + instrument);
    }

    @Override
    public void onTempoChanged(int tempoBPM)
    {
        //
        LOGGER.severe("onLayerChanged() tempoBPM=" + tempoBPM);
    }

    @Override
    public void onKeySignatureParsed(byte key, byte scale)
    {
        //
        LOGGER.severe("onKeySignatureParsed() key=" + key + " scale=" + scale);
    }

    @Override
    public void onTimeSignatureParsed(byte numerator, byte powerOfTwo)
    {
        LOGGER.severe("onTimeSignatureParsed() numerator=" + numerator + " powerOfTwo=" + powerOfTwo);
    }

    @Override
    public void onBarLineParsed(long id)
    {
        LOGGER.severe("onBarLineParsed() id=" + id);
    }

    @Override
    public void onTrackBeatTimeBookmarked(String timeBookmarkId)
    {
        //
    }

    @Override
    public void onTrackBeatTimeBookmarkRequested(String timeBookmarkId)
    {
        //
    }

    @Override
    public void onTrackBeatTimeRequested(double time)
    {
        //
    }

    @Override
    public void onPitchWheelParsed(byte lsb, byte msb)
    {
        //
    }

    @Override
    public void onChannelPressureParsed(byte pressure)
    {
        //
    }

    @Override
    public void onPolyphonicPressureParsed(byte key, byte pressure)
    {
        //
    }

    @Override
    public void onSystemExclusiveParsed(byte... bytes)
    {
        //
    }

    @Override
    public void onControllerEventParsed(byte controller, byte value)
    {
        //
    }

    @Override
    public void onLyricParsed(String lyric)
    {
        //
    }

    @Override
    public void onMarkerParsed(String marker)
    {
        //
    }

    @Override
    public void onFunctionParsed(String id, Object message)
    {
        //
    }

    @Override
    public void onNotePressed(Note note)
    {
        //
    }

    @Override
    public void onNoteReleased(Note note)
    {
        //
    }

    @Override
    public void onNoteParsed(Note note)
    {
        //
    }

    @Override
    public void onChordSymbolParsed(String chordSymbol)
    {
        LOGGER.severe("onChordSymbolParsed() chordSymbol=" + chordSymbol);
    }

}
