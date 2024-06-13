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
package org.jjazz.yamjjazz;

import org.jjazz.yamjjazz.rhythm.api.AccType;
import org.jjazz.yamjjazz.rhythm.api.Style;
import org.jjazz.yamjjazz.rhythm.api.CtabChannelSettings;
import org.jjazz.yamjjazz.rhythm.api.StylePartType;
import org.jjazz.yamjjazz.rhythm.api.StylePart;
import org.jjazz.phrase.api.NoteEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.parser.MidiParserListenerAdapter;
import org.jjazz.phrase.api.SourcePhrase;
import org.jjazz.phrase.api.SourcePhraseSet;

/**
 * MidiParserListener to retrieve the music data of the style parts in a standard Yamaha style file.
 * <p>
 * Retrieve only Notes (Note-ON/OFF), ignore all the rest (Sysex, program changes, control changes, pitch bends etc.). <br>
 * Set the sizeInBeats of each StylePart.<p>
 * Try to guess the style's Rhythm.Feel from notes positions.<p>
 * Remove the MegaVoice effect notes.
 */
public class MPL_MusicData extends MidiParserListenerAdapter
{

    public boolean IS_BUGGED;            // If true a problem occured, collected data is not valid
    public boolean skipNegativePositionError = false;
    public boolean skipNoCtabError = false;
    private final Style style;
    private int currentChannel;
    private float stylePartStartPos;
    private StylePart currentStylePart;
    private SourcePhraseSet currentSourcePhraseSet;
    private String logName = "";
    private static final Logger LOGGER = Logger.getLogger(MPL_MusicData.class.getSimpleName());

    /**
     *
     * @param style The style must have its StyleParts initialized.
     */
    public MPL_MusicData(Style style, String logName)
    {
        this.style = style;
        if (logName != null)
        {
            this.logName = logName;
        }
    }

    @Override
    public void onChannelChanged(byte b)
    {
        currentChannel = b;
    }

    @Override
    public void onMarkerParsed(String marker, float posInBeats)
    {
        LOGGER.log(Level.FINE, "{0} - onMarkerParsed() marker={1} posInBeats={2} currentStylePart={3} stylePartStartPos={4}", new Object[]
        {
            logName, marker, posInBeats, currentStylePart, stylePartStartPos
        });
        if (IS_BUGGED)
        {
            return;
        }
        if (currentStylePart != null)
        {
            // Save size before switching to a new StylePart
            currentStylePart.setSizeInBeats(Math.round(posInBeats - stylePartStartPos));
            if (currentStylePart.getSizeInBeats() < 1)
            {
                LOGGER.log(Level.SEVERE, "{0} - onMarkerParsed() size too small : currentStylePart.sizeInBeats={1} marker={2} posInBeats={3} currentStylePart={4}", new Object[]
                {
                    logName, currentStylePart.getSizeInBeats(), marker, posInBeats, currentStylePart
                });
                IS_BUGGED = true;
                return;
            }
        }

        // By default reset everything
        currentStylePart = null;
        stylePartStartPos = -1;
        currentSourcePhraseSet = null;

        // Analyze marker
        StylePartType type = StylePartType.getType(marker);
        if (type != null)
        {
            currentStylePart = style.getStylePart(type);
            if (currentStylePart != null)
            {
                stylePartStartPos = posInBeats;
                currentSourcePhraseSet = currentStylePart.getSourcePhraseSet(StylePart.DEFAULT_COMPLEXITY_LEVEL, 0);
                if (currentSourcePhraseSet == null)
                {
                    // Create it
                    currentSourcePhraseSet = new SourcePhraseSet(type.toString());
                    currentStylePart.addSourcePhraseSet(currentSourcePhraseSet, StylePart.DEFAULT_COMPLEXITY_LEVEL);
                }
            } else
            {
                LOGGER.log(Level.SEVERE, "{0} - onMarkerParsed() StylePart marker={1} found in Midi but not found in the CASM part !", new Object[]
                {
                    logName, type
                });
                IS_BUGGED = true;
                return;
            }
        }
    }

    @Override
    public void onNoteParsed(Note note, float posInBeats)
    {
        if (IS_BUGGED)
        {
            return;
        }
        if (currentStylePart == null)
        {
            // Can happen if CASM is not consistent with Midi tracks
            // Eg CASM does not define Main A, but there is a Main A marker in the Midi section
            return;
        }

        assert stylePartStartPos != -1;   //NOI18N
        float relativePosInBeats = posInBeats - stylePartStartPos;
        if (relativePosInBeats < 0)
        {
            // Can happen with corrupted style files: a NoteOn is before a stylepart change (marker) and the NoteOff is after
            // Ignore the note
            if (!skipNegativePositionError)
            {
                LOGGER.log(Level.FINE,
                        "{0} - onNoteParsed() note {1} with negative position={2}, currentStylePart={3} currentChannel={4}. Ignoring similar errors...", new Object[]
                        {
                            logName, note, relativePosInBeats, currentStylePart, currentChannel
                        });
                skipNegativePositionError = true;
            }
            return;
        }
        NoteEvent me = new NoteEvent(note.getPitch(), note.getDurationInBeats(), note.getVelocity(), relativePosInBeats);
        SourcePhrase sp = currentSourcePhraseSet.getPhrase(currentChannel);
        if (sp == null)
        {
            // Create the phrase if it's the first note for this channel
            CtabChannelSettings cTab = currentStylePart.getCtabChannelSettings(currentChannel);
            if (cTab == null)
            {
                if (!skipNoCtabError)
                {
                    LOGGER.log(Level.SEVERE, "{0} - onNoteParsed() no cTab found for currentStylePart={1} channel={2}. All related notes will be ignored.", new Object[]
                    {
                        logName, currentStylePart, currentChannel
                    });
                    skipNoCtabError = true;
                }
                return;
            }
            sp = new SourcePhrase(currentChannel, cTab.getSourceChordSymbol());
            currentSourcePhraseSet.setPhrase(currentChannel, sp);
        }
        AccType at = currentStylePart.getAccType(currentChannel);
        if (!isMegaVoiceNote(at, note.getPitch()))
        {
            sp.add(me);
        }
    }

    @Override
    public void onMetaEndEvent(float posInBeats)
    {
        if (IS_BUGGED)
        {
            return;
        }
        if (currentStylePart != null)
        {
            // Save size
            currentStylePart.setSizeInBeats(Math.round(posInBeats - stylePartStartPos));
            if (currentStylePart.getSizeInBeats() < 1)
            {
                // Can happen sometimes on some corrupted style files                
                if (currentStylePart.getType().equals(StylePartType.Main_A))
                {
                    // Main A must be present
                    LOGGER.log(Level.SEVERE, "{0} - onMetaEndEvent() size too small : currentStylePart.sizeInBeats={1}  posInBeats={2} currentStylePart={3}", new Object[]
                    {
                        logName, currentStylePart.getSizeInBeats(), posInBeats, currentStylePart
                    });
                    IS_BUGGED = true;
                } else
                {
                    LOGGER.log(Level.SEVERE, "{0} - onMetaEndEvent() size too small : currentStylePart.sizeInBeats={1}  posInBeats={2} currentStylePart={3} will not be usable.", new Object[]
                    {
                        logName, currentStylePart.getSizeInBeats(), posInBeats, currentStylePart
                    });
                }
            }
        }

    }

    /**
     * From Tyros manual: above C6 pitches it's only noise/effects
     *
     * @param at
     * @param pitch
     * @return
     */
    private boolean isMegaVoiceNote(AccType at, int pitch)
    {
        return style.getSInt().isExpectingMegaVoice(at) && pitch >= 84;
    }


}
