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
package org.jjazz.importers.jfugue;

public interface ParserListener
{

    /**
     * Called when the parser first starts up, but before it starts parsing anything. Provides listeners with a chance to initialize
     * variables and get ready for the parser events.
     */
    public void beforeParsingStarts();

    /**
     * Called when the parser has parsed its last item. Provides listeners with a chance to clean up.
     */
    public void afterParsingFinished();

    /**
     * Called when the parser encounters a new track (also known as a channel; previously in JFugue, known as a Voice).
     * <p>
     * Tracks correspond to MIDI tracks/channels.
     *
     * @param track the new track event that has been parsed
     */
    public void onTrackChanged(byte track);

    /**
     * Called when the parser encounters a new layer.
     * <p>
     * A layer deals with polyphony within a track. While any track may have layers, layers are intended for use with the percussion track,
     * where each layer may represent notes for a specific percussive instrument. Layers can essentially be thought of as a "track within a
     * track." Each layer maintains its own time progression, so "L1 Eq Eq L2 Cq Gq" would be the same as saying "Eq+Cq Eq+Gq". Layers are a
     * JFugue feature, and are not a part of the MIDI specification.
     *
     * @param newTrack the new track event that has been parsed
     * @param oldTrack the previous track. The default value for oldTrack is 0.
     */
    public void onLayerChanged(byte layer);

    /**
     * Called when the parser encounters a new instrument selection.
     *
     * @param instrument the MIDI instrument value that has been parsed
     */
    public void onInstrumentParsed(byte instrument);

    /**
     * Called when the parser encounters a new tempo selection.
     *
     * @param tempoBPM The new tempo value
     */
    public void onTempoChanged(int tempoBPM);

    public void onKeySignatureParsed(byte key, byte scale);

    /**
     * The first parameter is the number of beats per measure; The second parameter is the power by which 2 must be raised to create the
     * note that represents one beat.
     * <p>
     * Example 1: For a 5/8 time signature, expect 5,3 (since 2^3 = 8) Example 2: For a 4/4 time signature, expect 4,2 (since 2^2 = 4)
     */
    public void onTimeSignatureParsed(byte numerator, byte powerOfTwo);

    /**
     * The separator character which indicates a bar line has been parsed.
     * <p>
     * Generally, you will want to care about this if you're counting measures, but this should have no effect on the rendering of a parsed
     * piece of music.
     *
     * @param id This is the id of the measure, which is an optional numeric value following the bar character.
     */
    public void onBarLineParsed(long id);

    public void onTrackBeatTimeBookmarked(String timeBookmarkId);

    public void onTrackBeatTimeBookmarkRequested(String timeBookmarkId);

    public void onTrackBeatTimeRequested(double time);

    public void onPitchWheelParsed(byte lsb, byte msb);

    public void onChannelPressureParsed(byte pressure);

    public void onPolyphonicPressureParsed(byte key, byte pressure);

    public void onSystemExclusiveParsed(byte... bytes);

    public void onControllerEventParsed(byte controller, byte value);

    public void onLyricParsed(String lyric);

    public void onMarkerParsed(String marker);

    public void onFunctionParsed(String id, Object message);

    /**
     * Used to indicate when a note is pressed. Used in realtime cases when notes are actually being pressed and released. Parsers that do
     * not operate in realtime are not expected to report onNotePressed.
     * <p>
     * Expect the Note event to contain only the note number and note-on velocity.
     */
    public void onNotePressed(Note note);

    /**
     * Used to indicate when a note is released. Used in realtime cases when notes are actually being pressed and released. Parsers that do
     * not operate in realtime are not expected to report onNoteReleased.
     * <p>
     * Expect the Note event to contain only the note number and note-off velocity. Duration may not be set on the Note from onNoteReleased.
     */
    public void onNoteReleased(Note note);

    /**
     * We may have actually parsed a musical note! In previous versions of JFugue, ParserListener had separate listeners for parallel notes
     * and sequential notes (now termed harmonic and melodic notes, respectively) In this version of JFugue, whether a note is the first
     * note, a harmonic note, or a melodic note is kept as a property on the Note object itself.
     *
     * @param note The note that was parsed. Please see the Note class for more details about notes!
     * @see Note
     */
    public void onNoteParsed(Note note);

    /**
     * ChordSymbol parsed.
     *
     * @param strChord
     */
    public void onChordSymbolParsed(String strChord);
}
