package org.jjazz.jjswing.bass.db;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.utilities.api.FloatRange;

/**
 * Statistics of a WbpSource.
 * <p>
 * IMPORTANT: variables counting a number of notes (e.g. nbEighthNotes) are based on *note-to-note duration* (not *note duration*).
 */
public record WbpSourceStats(Slope startSlope,
        Slope endSlope,
        boolean isOneNotePerBeat,
        int nbShortNotes,
        /**
         * Note-to-note duration is ~0.66 beat.
         */
        int nbDottedEighthNotes,
        int nbQuarterNotes,
        /**
         * Notes with note-to-note duration between quarter and half.
         */
        int nbDottedQuarterNotes,
        int nbHalfNotes,
        int nbLongNotes,
        int nbMaxSuccessiveShortNotes,
        int nbMaxSuccessiveDottedEighthNotes)
        {

    static private final float BEAT_WINDOW = 0.2f;
    static private final FloatRange SHORT_DURATION = new FloatRange(0f, 0.666f - BEAT_WINDOW);
    static private final FloatRange DOTTED_EIGHTH_DURATION = new FloatRange(SHORT_DURATION.to, 0.666f + BEAT_WINDOW);
    static private final FloatRange QUARTER_DURATION = new FloatRange(DOTTED_EIGHTH_DURATION.to, 1 + BEAT_WINDOW);
    static private final FloatRange HALF_DURATION = new FloatRange(2 - BEAT_WINDOW, 2 + BEAT_WINDOW);
    static private final FloatRange DOTTED_QUARTER_DURATION = new FloatRange(QUARTER_DURATION.to, HALF_DURATION.from);
    static private final FloatRange LONG_DURATION = new FloatRange(HALF_DURATION.to, 17f);


    public enum Slope
    {
        UP, DOWN, FLAT
    }

    static public WbpSourceStats of(WbpSource wbpSource)
    {
        SizedPhrase sp = wbpSource.getSizedPhrase();
        TimeSignature ts = sp.getTimeSignature();

        // Slopes        
        FloatRange fr = new FloatRange(0, ts.getNbNaturalBeats());
        var startNotes = wbpSource.getSizedPhrase().getNotes(ne -> true, fr, true);
        Slope startSlope = computeSlope(startNotes);
        float phraseTo = wbpSource.getBeatRange().to;
        fr = new FloatRange(phraseTo - ts.getNbNaturalBeats(), phraseTo);
        var endNotes = wbpSource.getSizedPhrase().getNotes(ne -> true, fr, true);
        Slope endSlope = computeSlope(endNotes);


        boolean isOneNotePerBeat = isOneNotePerBeat(sp, BEAT_WINDOW);


        // Note counting
        // We assume that the bass line does not contain chord of notes (simultaneous notes)
        int nbShortNotes = 0, nbDottedEighthNotes = 0, nbQuarterNotes = 0, nbDottedQuarterNotes = 0, nbHalfNotes = 0,
                nbLongNotes = 0, nbMaxSuccessiveShortNotes = 0, nbMaxSuccessiveDottedEighthNotes = 0;
        NoteEvent lastNe = null;
        LinkedList<NoteEvent> successiveShortNotes = new LinkedList<>();
        LinkedList<NoteEvent> successiveDottedEighthNotes = new LinkedList<>();

        List<NoteEvent> nes = new ArrayList<>(sp);
        for (int i = 0; i < nes.size(); i++)
        {
            var ne = nes.get(i);
            var beatPos = ne.getPositionInBeats();
            var nextBeatPos = i < nes.size() - 1 ? nes.get(i + 1).getPositionInBeats() : sp.getNotesBeatRange().to;

            var n2nDur = nextBeatPos - beatPos;
            boolean wasShort = false;
            boolean wasDottedEight = false;

            if (SHORT_DURATION.contains(n2nDur, true))
            {
                nbShortNotes++;
                successiveShortNotes.add(ne);
                wasShort = true;
            } else if (DOTTED_EIGHTH_DURATION.contains(n2nDur, true))
            {
                nbDottedEighthNotes++;
                successiveDottedEighthNotes.add(ne);
                wasDottedEight = true;
            } else if (QUARTER_DURATION.contains(n2nDur, true))
            {
                nbQuarterNotes++;
            } else if (DOTTED_QUARTER_DURATION.contains(n2nDur, true))
            {
                nbDottedQuarterNotes++;
            } else if (HALF_DURATION.contains(n2nDur, true))
            {
                nbHalfNotes++;
            } else if (LONG_DURATION.contains(n2nDur, true))
            {
                nbLongNotes++;
            } else
            {
                throw new IllegalStateException("ne=" + ne + " neLast=" + lastNe + " n2nDuration=" + n2nDur);
            }


            if (!wasShort && !successiveShortNotes.isEmpty())
            {
                nbMaxSuccessiveShortNotes = Math.max(nbMaxSuccessiveShortNotes, successiveShortNotes.size());
                successiveShortNotes.clear();
            }
            if (!wasDottedEight && !successiveDottedEighthNotes.isEmpty())
            {
                nbMaxSuccessiveDottedEighthNotes = Math.max(nbMaxSuccessiveDottedEighthNotes, successiveDottedEighthNotes.size());
                successiveDottedEighthNotes.clear();
            }
        }

        var res = new WbpSourceStats(startSlope, endSlope, isOneNotePerBeat, nbShortNotes, nbDottedEighthNotes, nbQuarterNotes, nbDottedQuarterNotes, nbHalfNotes,
                nbLongNotes, nbMaxSuccessiveShortNotes, nbMaxSuccessiveDottedEighthNotes);

        assert sp.size() == nbShortNotes + nbDottedEighthNotes + nbQuarterNotes + nbDottedQuarterNotes + nbHalfNotes + nbLongNotes : "sp=" + sp + " res=" + res;

        return res;
    }

    // ===============================================================================================
    // Private methods
    // ===============================================================================================

    /**
     * Compute the global slope for the specified notes.
     * <p>
     * Count the up and down intervals. If equals, use first and last note.
     *
     * @param notes
     * @return
     */
    static private Slope computeSlope(List<NoteEvent> notes)
    {
        if (notes.isEmpty())
        {
            return Slope.FLAT;
        }

        int count = 0;
        for (int i = 0; i < notes.size() - 1; i++)
        {
            var n = notes.get(i);
            var n2 = notes.get(i + 1);
            int diff = n2.getPitch() - n.getPitch();
            if (count != 0)
            {
                count += diff > 0 ? 1 : -1;
            }
        }

        if (count == 0)
        {
            var n = notes.get(0);
            var n2 = notes.get(notes.size() - 1);
            int diff = n2.getPitch() - n.getPitch();
            if (count != 0)
            {
                count = diff > 0 ? 1 : -1;
            }
        }

        Slope res = Slope.FLAT;
        if (count > 0)
        {
            res = Slope.UP;
        } else if (count < 0)
        {
            res = Slope.DOWN;
        }

        return res;
    }

    /**
     * Check if WbpSource has one note per beat.
     *
     * @param nearBeatWindow Tolerate slight difference in beat position
     * @return
     */
    static private boolean isOneNotePerBeat(SizedPhrase sp, float nearBeatWindow)
    {
        boolean b = true;

        if (sp.size() == sp.getTimeSignature().getNbNaturalBeats())
        {
            int beat = 0;
            for (var ne : sp)
            {
                FloatRange fr = new FloatRange(Math.max(0, beat - nearBeatWindow), beat + nearBeatWindow);
                if (!fr.contains(ne.getPositionInBeats(), false))
                {
                    b = false;
                    break;
                }
                beat++;
            }
        } else
        {
            b = false;
        }
        return b;
    }
}
