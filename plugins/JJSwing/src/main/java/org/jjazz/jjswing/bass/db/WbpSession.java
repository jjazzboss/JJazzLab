/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.jjswing.bass.db;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.jjswing.api.BassStyle;
import org.jjazz.jjswing.bass.BassGenerator;
import static org.jjazz.jjswing.bass.BassGenerator.NON_QUANTIZED_WINDOW;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * A session is one consistent recording of a possibly long WalkingBassPhrase, which is meant to be sliced in WbpSource units.
 * <p>
 * It is recommended to use the shortest chord symbol corresponding to the phrase notes, typically 3-degrees or 4-degrees chord symbols (C, Cm, C+, Cdim, Cm6,
 * C6, C7M, C7, Csus, C7sus, ...), though more complex chord symbols are allowed. If a chord symbol is too complex for the actual phrase notes (eg C7b9 but
 * phrase only uses C E G), chord symbol will be simplified in the WbpSource (C7b9 &gt; C).
 * <p>
 */
class WbpSession extends Wbp
{

    private final String id;
    private final List<String> tags;
    private final BassStyle bassStyle;
    private boolean[] hasCrossingNoteAtBar;
    private float[] firstNoteBeatShiftAtBar;
    private final boolean disableTargetNotes;
    private static final Logger LOGGER = Logger.getLogger(WbpSession.class.getSimpleName());

    /**
     *
     * @param id
     * @param tags       If tags contained "notn", resulting WbpSources will have no target note defined.
     * @param cSeq
     * @param phrase     Phrase must start at bar/beat 0
     * @param targetNote
     */
    public WbpSession(String id, List<String> tags, SimpleChordSequence cSeq, SizedPhrase phrase, Note targetNote)
    {
        super(cSeq, phrase, targetNote);
        this.id = id;
        this.tags = tags;
        this.disableTargetNotes = tags.remove("notn");
        this.bassStyle = computeBassStyle(tags);
    }

    public String getId()
    {
        return id;
    }

    /**
     * The tags associated to this session.
     *
     * @return
     */
    public List<String> getTags()
    {
        return tags;
    }

    /**
     * Extract all the possible WbpSources from this session.
     * <p>
     * We extract all the possible 1/2/3/4-bar WbpSources. So for one 4-bar session phrase, the method can generate up to 10 WbpSource objects: 1 * 4-bar + 2 *
     * 3-bar + 3 * 2-bar + 4 * 1-bar.
     * <p>
     * Returned WbpSources get the tags of the session.
     *
     * @param disallowNonRootStartNote     If true a WbpSource is not extracted if its first note is different from the chord root note.
     * @param disallowNonChordToneLastNote If true a WbpSource is not extracted if its last note is note a chord note (ie no transition note).
     * @return
     */
    public List<WbpSource> extractWbpSources(boolean disallowNonRootStartNote, boolean disallowNonChordToneLastNote)
    {
        LOGGER.log(Level.FINE, "extractWbpSources() -- disallowNonRootStartNote={0} disallowNonChordToneLastNote={1}", new Object[]
        {
            disallowNonRootStartNote,
            disallowNonChordToneLastNote
        });
        List<WbpSource> res = new ArrayList<>();
        int sessionSizeInBars = getSizedPhrase().getSizeInBars();

        precomputeCrossingNotes();

        for (int srcSize = WbpSourceDatabase.SIZE_MIN; srcSize <= WbpSourceDatabase.SIZE_MAX; srcSize++)
        {
            for (int bar = 0; bar <= sessionSizeInBars - srcSize; bar++)
            {
                IntRange barRange = new IntRange(bar, bar + srcSize - 1);
                if (hasCrossingNoteAtStartOf(bar) || hasCrossingNoteAtStartOf(barRange.to + 1))
                {
                    continue;
                }

                WbpSource wbpSource = extractWbpSource(barRange);
                var p = wbpSource.getSizedPhrase();

                boolean bStart = !p.isEmpty() && p.first().getPositionInBeats() <= BassGenerator.DURATION_BEAT_MARGIN;
                boolean bFirst = !disallowNonRootStartNote || wbpSource.isStartingOnChordBass();
                boolean bLast = !disallowNonChordToneLastNote || wbpSource.isEndingOnChordTone();
                if (bStart && bFirst && bLast)
                {
                    res.add(wbpSource);
                } else
                {
                    LOGGER.log(Level.FINE, "extractWbpSources() Discarding isStartingOnChordBass={0}  isEndingOnChordTone={1}   wbpSource={2} p={3}", new Object[]
                    {
                        wbpSource.isStartingOnChordBass(),
                        wbpSource.isEndingOnChordTone(),
                        wbpSource,
                        p
                    });
                }
            }
        }
        return res;
    }


    @Override
    public String toString()
    {
        return "WbpSession id=" + id + " tags=" + tags + " | " + super.toString();
    }

    // ==============================================================================================
    // Private methods
    // ==============================================================================================

    /**
     * Initialize hasNoCrossingNoteAtBar and hasNoCrossingNoteAtBar data for the session phrase.
     */
    private void precomputeCrossingNotes()
    {
        if (hasCrossingNoteAtBar != null)
        {
            return;
        }

        var sp = getSizedPhrase();
        var ts = sp.getTimeSignature();
        float nbBeatsPerBar = ts.getNbNaturalBeats();
        int arraySize = getBarRange().size() + 1;
        firstNoteBeatShiftAtBar = new float[arraySize];
        hasCrossingNoteAtBar = new boolean[arraySize];


        for (var ne : getSizedPhrase())
        {
            FloatRange neBr = ne.getBeatRange();
            if (neBr.size() <= NON_QUANTIZED_WINDOW)
            {
                continue;
            }

            int bar = (int) Math.floor(neBr.from / nbBeatsPerBar);
            float inBarBeatPos = neBr.from % nbBeatsPerBar;

            if (inBarBeatPos >= (nbBeatsPerBar - NON_QUANTIZED_WINDOW))
            {
                // Note starts at very end of bar, it's actually a note from next bar
                if (firstNoteBeatShiftAtBar[bar + 1] == 0)
                {
                    firstNoteBeatShiftAtBar[bar + 1] = inBarBeatPos - nbBeatsPerBar;
                }
            } else if (inBarBeatPos + neBr.size() > nbBeatsPerBar + NON_QUANTIZED_WINDOW)
            {
                hasCrossingNoteAtBar[bar + 1] = true;
            }

        }
    }

    private boolean hasCrossingNoteAtStartOf(int bar)
    {
        return hasCrossingNoteAtBar[bar];
    }

    private float getFirstNoteBeatShift(int bar)
    {
        return this.firstNoteBeatShiftAtBar[bar];
    }

    /**
     * Extract a WbpSource from a SizedPhrase.
     * <p>
     *
     * @param barRange Session phrase must have no crossing note at start or end of this bar range.
     * @return Can not be null
     */
    private WbpSource extractWbpSource(IntRange barRange)
    {
        Preconditions.checkArgument(barRange.size() <= WbpSourceDatabase.SIZE_MAX);

        SizedPhrase sessionPhrase = getSizedPhrase();
        float nbBeatsPerBar = sessionPhrase.getTimeSignature().getNbNaturalBeats();
        LOGGER.log(Level.FINE, "extractWbpSource() barRange={0} WbpSession={1}", new Object[]
        {
            barRange, this
        });


        // Extract our phrase slice from sessionPhrase and make it start at beat 0
        FloatRange beatRange = new FloatRange(barRange.from * nbBeatsPerBar, (barRange.to + 1) * nbBeatsPerBar);
        Phrase p = Phrases.getSlice(sessionPhrase, beatRange, true, 1, NON_QUANTIZED_WINDOW);
        p.shiftAllEvents(-beatRange.from, false);
        SizedPhrase sp = new SizedPhrase(sessionPhrase.getChannel(), beatRange.getTransformed(-beatRange.from), sessionPhrase.getTimeSignature(), false);
        sp.addAll(p);


        // Extract the chord sequence and make it start at bar 0
        SimpleChordSequence scs = getSimpleChordSequence().subSequence(barRange, true).getShifted(-barRange.from);


        // Compute targetNote if required
        Note targetNote = getTargetNote(); // Can be null. Target note for the session, ie if barRange.to is the last bar of the session
        int nextBar = barRange.to + 1;
        if (!disableTargetNotes && nextBar < sessionPhrase.getSizeInBars())
        {
            // If there is one bar after our phrase, find the next note
            FloatRange fr = new FloatRange(nextBar * nbBeatsPerBar, (nextBar + 1) * nbBeatsPerBar);
            var nextBarNotes = sessionPhrase.getNotes(n -> true, fr, true);
            if (!nextBarNotes.isEmpty())
            {
                targetNote = nextBarNotes.get(0);
            } else
            {
                LOGGER.log(Level.WARNING, "extractWbpSource() barRange={0} unexpected nextBarNotes is empty. sessionPhrase={1}", new Object[]
                {
                    barRange,
                    sessionPhrase
                });
            }
        }


        WbpSource wbpSource = null;
        try
        {
            wbpSource = new WbpSource(getId(), barRange.from, bassStyle, scs, sp, getFirstNoteBeatShift(barRange.from), targetNote);
        } catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Error creationg WBpSource for sessionId={0} barRange={1} bassStyle={2} scs={3} sp={4} targetNote={5}", new Object[]
            {
                getId(), barRange, bassStyle, scs, sp, targetNote
            });
            throw e;
        }

        return wbpSource;
    }

    private BassStyle computeBassStyle(List<String> tags)
    {
        BassStyle res = BassStyle.WALKING;
        boolean twoFeel = tags.stream().anyMatch(t -> t.startsWith("2feel"));
        boolean walking = tags.stream().anyMatch(t -> t.equalsIgnoreCase("walking"));
        if ((twoFeel && walking) || (!twoFeel && !walking))
        {
            LOGGER.log(Level.SEVERE, "computeBassStyle() Inconsistent tags found in WbpSession={0}", this);
        } else if (twoFeel)
        {
            res = BassStyle.TWO_FEEL;
        }
        return res;
    }

}
// ==============================================================================================
// Inner classes
// ==============================================================================================
