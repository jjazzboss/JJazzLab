package org.jjazz.test.walkingbass;

import org.jjazz.test.walkingbass.api.Wbp;
import java.util.List;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.utilities.api.FloatRange;

/**
 * Statistics of a WalkingBassPhrase.
 */
public class WbpStats
{

    public enum Slope
    {
        UP, DOWN, FLAT
    }
    private final Wbp wbpPhrase;
    private Slope startSlope, endSlope;

    public WbpStats(Wbp phrase)
    {
        wbpPhrase = phrase;
        computeStats();
    }
    
//    public int getInsideFactor()
//    {
//        
//    }

    public Wbp getWbpPhrase()
    {
        return wbpPhrase;
    }

    public Slope getStartSlope()
    {
        return startSlope;
    }

    public Slope getEndSlope()
    {
        return endSlope;
    }

    // ===============================================================================================
    // Private methods
    // ===============================================================================================

    private void computeStats()
    {
        TimeSignature ts = wbpPhrase.getSizedPhrase().getTimeSignature();
        
        // Slopes        
        FloatRange fr = new FloatRange(0, ts.getNbNaturalBeats());
        var startNotes = wbpPhrase.getSizedPhrase().getNotes(ne -> true, fr, true);
        startSlope = computeSlope(startNotes);

        float phraseTo = wbpPhrase.getBeatRange().to;
        fr = new FloatRange(phraseTo - ts.getNbNaturalBeats(), phraseTo);
        var endNotes = wbpPhrase.getSizedPhrase().getNotes(ne -> true, fr, true);
        endSlope = computeSlope(endNotes);

    }

    /**
     * Compute the global slope for the specified notes.
     * <p>
     * Count the up and down intervals. If equals, use first and last note.
     *
     * @param notes
     * @return 
     */
    private Slope computeSlope(List<NoteEvent> notes)
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

}
