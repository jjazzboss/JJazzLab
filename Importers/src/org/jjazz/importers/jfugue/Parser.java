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
package org.jjazz.importers.jfugue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Parser
{

    private CopyOnWriteArrayList<ParserListener> parserListeners;

    public Parser()
    {
        parserListeners = new CopyOnWriteArrayList<ParserListener>();
    }

    public void addParserListener(ParserListener listener)
    {
        parserListeners.add(listener);
    }

    public void removeParserListener(ParserListener listener)
    {
        parserListeners.remove(listener);
    }

    public List<ParserListener> getParserListeners()
    {
        return parserListeners;
    }

    public void clearParserListeners()
    {
        this.parserListeners.clear();
    }

    //
    // Event firing methods
    //
    public void fireBeforeParsingStarts()
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.beforeParsingStarts();
        }
    }

    public void fireAfterParsingFinished()
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.afterParsingFinished();
        }
    }

    public void fireTrackChanged(byte track)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onTrackChanged(track);
        }
    }

    public void fireLayerChanged(byte layer)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onLayerChanged(layer);
        }
    }

    /**
     * 
     * @param instrument GM instrument program change
     */
    public void fireInstrumentParsed(byte instrument)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onInstrumentParsed(instrument);
        }
    }

    public void fireTempoChanged(int tempoBPM)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onTempoChanged(tempoBPM);
        }
    }

    public void fireKeySignatureParsed(byte key, byte scale)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onKeySignatureParsed(key, scale);
        }
    }

    public void fireTimeSignatureParsed(byte numerator, byte powerOfTwo)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onTimeSignatureParsed(numerator, powerOfTwo);
        }
    }

    public void fireBarLineParsed(long id)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onBarLineParsed(id);
        }
    }

    public void fireTrackBeatTimeBookmarked(String timeBookmarkId)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onTrackBeatTimeBookmarked(timeBookmarkId);
        }
    }

    public void fireTrackBeatTimeBookmarkRequested(String timeBookmarkId)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onTrackBeatTimeBookmarkRequested(timeBookmarkId);
        }
    }

    public void fireTrackBeatTimeRequested(double time)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onTrackBeatTimeRequested(time);
        }
    }

    public void firePitchWheelParsed(byte lsb, byte msb)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onPitchWheelParsed(lsb, msb);
        }
    }

    public void fireChannelPressureParsed(byte pressure)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onChannelPressureParsed(pressure);
        }
    }

    public void firePolyphonicPressureParsed(byte key, byte pressure)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onPolyphonicPressureParsed(key, pressure);
        }
    }

    public void fireSystemExclusiveParsed(byte... bytes)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onSystemExclusiveParsed(bytes);
        }
    }

    public void fireControllerEventParsed(byte controller, byte value)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onControllerEventParsed(controller, value);
        }
    }

    public void fireLyricParsed(String lyric)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onLyricParsed(lyric);
        }
    }

    public void fireMarkerParsed(String marker)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onMarkerParsed(marker);
        }
    }

    public void fireFunctionParsed(String id, Object message)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onFunctionParsed(id, message);
        }
    }

    public void fireNotePressed(Note note)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onNotePressed(note);
        }
    }

    public void fireNoteReleased(Note note)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onNoteReleased(note);
        }
    }

    public void fireNoteParsed(Note note)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onNoteParsed(note);
        }
    }

    public void fireChordSymbolParsed(String strChord)
    {
        List<ParserListener> listeners = getParserListeners();
        for (ParserListener listener : listeners)
        {
            listener.onChordSymbolParsed(strChord);
        }
    }

}
