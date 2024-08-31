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

import java.util.List;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.harmony.api.Position;

public interface MusicXmlParserListener
{


    /**
     * Called when the parser first starts up, but before it starts parsing anything.
     * <p>
     * Provides listeners with a chance to initialize variables and get ready for the parser events.
     */
    void beforeParsingStarts();

    /**
     * Called when the parser has parsed its last item. Provides listeners with a chance to clean up.
     */
    void afterParsingFinished();

    /**
     * Called when the parser encounters a new tempo selection.
     *
     * @param tempoBPM The new tempo value
     * @param barIndex
     */
    void onTempoChanged(int tempoBPM, int barIndex);

    /**
     * A barline/repeat sign was parsed.
     *
     * @param barIndex
     * @param repeatStart True means a forward-direction repeat at the beginning of the bar, false for a backward-direction repeat at the end of the bar
     * @param times       Meaningful only if repeatStart is false, indicates the number of expected repeats.
     */
    void onRepeatParsed(int barIndex, boolean repeatStart, int times);

    /**
     * A direction/rehearsal element was parsed.
     *
     * @param barIndex
     * @param value    e.g. "A"
     */
    void onRehearsalParsed(int barIndex, String value);

    /**
     * Parsed a Sound attribute releated to song structure.
     * <p>
     *
     * @param barIndex
     * @param marker
     * @param value    The attribute value
     * @param timeOnly The time-only attribute associated to the marker. Can be empty.
     */
    void onNavigationMarkParsed(int barIndex, NavigationMark marker, String value, List<Integer> timeOnly);

    /**
     * An play/other-play element was parsed.
     * <p>
     * iRealPro export to musicXML uses to indicate the music style.
     *
     * @param barIndex
     * @param value    e.g. "Medium Swing"
     * @param type     type attribute
     */
    void onOtherPlayParsed(int barIndex, String value, String type);

    /**
     * An barline/ending was parsed.
     *
     * @param barIndex
     * @param numbers  Indicates which times the ending is played
     * @param type     0=start, 1=stop, 2=discontinue
     */
    void onEndingParsed(int barIndex, List<Integer> numbers, int type);

    void onTimeSignatureParsed(TimeSignature ts, int barIndex);

    /**
     * The separator character which indicates a bar line has been parsed.
     * <p>
     *
     * @param id       This is the id of the measure, which is an optional numeric value following the bar character.
     * @param barIndex This is incremented by 1 each time this method is called. Starts at 0.
     */
    void onBarLineParsed(String id, int barIndex);

    void onLyricParsed(String lyric, Position pos);

    /**
     * We may have actually parsed a musical note!
     * <p>
     * In previous versions of JFugue, ParserListener had separate listeners for parallel notes and sequential notes (now termed harmonic and melodic notes,
     * respectively) In this version of JFugue, whether a note is the first note, a harmonic note, or a melodic note is kept as a property on the Note object
     * itself.
     *
     * @param note The note that was parsed. Please see the Note class for more details about notes!
     * @param pos
     * @see Note
     */
    void onNoteParsed(Note note, Position pos);

    /**
     * ChordSymbol parsed.
     *
     * @param strChord
     * @param pos
     */
    void onChordSymbolParsed(String strChord, Position pos);
}
