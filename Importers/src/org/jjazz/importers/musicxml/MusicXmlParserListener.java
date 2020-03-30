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
 *
 * MODIFICATIONS @March 2021 by Jerome Lelasseux for JJazzLab
 */
package org.jjazz.importers.musicxml;

import org.jjazz.harmony.Note;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

public interface MusicXmlParserListener
{

    /**
     * Called when the parser first starts up, but before it starts parsing anything.
     * <p>
     * Provides listeners with a chance to initialize variables and get ready for the parser events.
     */
    public void beforeParsingStarts();

    /**
     * Called when the parser has parsed its last item. Provides listeners with a chance to clean up.
     */
    public void afterParsingFinished();

    /**
     * Called when the parser encounters a new tempo selection.
     *
     * @param tempoBPM The new tempo value
     */
    public void onTempoChanged(int tempoBPM, int barIndex);

    public void onTimeSignatureParsed(TimeSignature ts, int barIndex);

    /**
     * The separator character which indicates a bar line has been parsed.
     * <p>
     *
     * @param id This is the id of the measure, which is an optional numeric value following the bar character.
     * @param barIndex This is incremented by 1 each time this method is called. Starts at 0.
     */
    public void onBarLineParsed(int id, int barIndex);

    public void onLyricParsed(String lyric, Position pos);

    /**
     * We may have actually parsed a musical note!
     * <p>
     * In previous versions of JFugue, ParserListener had separate listeners for parallel notes and sequential notes
     * (now termed harmonic and melodic notes, respectively) In this version of JFugue, whether a note is the first
     * note, a harmonic note, or a melodic note is kept as a property on the Note object itself.
     *
     * @param note The note that was parsed. Please see the Note class for more details about notes!
     * @see Note
     */
    public void onNoteParsed(Note note, Position pos);

    /**
     * ChordSymbol parsed.
     *
     * @param strChord
     */
    public void onChordSymbolParsed(String strChord, Position pos);
}
