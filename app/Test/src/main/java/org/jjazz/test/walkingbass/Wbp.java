package org.jjazz.test.walkingbass;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;


/**
 * A walking bass phrase associated to a SimpleChordSequence.
 */
public class Wbp
{

    private final SimpleChordSequence chordSequence;
    private final SizedPhrase sizedPhrase;
    private final Note targetNote;
    private WbpStats stats;
    private static final Logger LOGGER = Logger.getLogger(Wbp.class.getSimpleName());
    
    /**
     *
     * @param cSeq       Must have a chord at beginning and use only one time signature
     * @param phrase     Can not be empty
     * @param targetNote
     */
    public Wbp(SimpleChordSequence cSeq, SizedPhrase phrase, Note targetNote)
    {
        checkNotNull(targetNote);
        checkNotNull(cSeq);
        checkNotNull(phrase);
        checkArgument(cSeq.getBarRange().size() == (int) Math.round(phrase.getBeatRange().size() / phrase.getTimeSignature().getNbNaturalBeats())
                && phrase.getBeatRange().from == 0
                && !phrase.isEmpty()
                && cSeq.hasChordAtBeginning()
                && cSeq.getTimeSignature().equals(phrase.getTimeSignature()),
                "cSeq=%s phrase=%s", cSeq, phrase);

        this.chordSequence = cSeq;
        this.sizedPhrase = phrase;
        this.targetNote = targetNote;
    }

    public SimpleChordSequence getSimpleChordSequence()
    {
        return chordSequence;
    }

    public WbpStats getStats()
    {
        if (stats == null)
        {
            stats = new WbpStats(this);
        }
        return stats;
    }

    public SizedPhrase getSizedPhrase()
    {
        return sizedPhrase;
    }

    public Note getFirstNote()
    {
        return sizedPhrase.first();
    }

//    public boolean startsOnRoot()
//    {
//
//    }
//
//    public boolean startOnFifth()
//    {
//
//    }

    /**
     * The most expected note to start the next phrase right after this phrase.
     *
     * @return
     */
    public Note getTargetNote()
    {
        return targetNote;
    }

    public IntRange getBarRange()
    {
        return chordSequence.getBarRange();
    }

    public TimeSignature getTimeSignature()
    {
        return sizedPhrase.getTimeSignature();
    }

    public FloatRange getBeatRange()
    {
        return sizedPhrase.getBeatRange();
    }


    @Override
    public String toString()
    {
        final int NB_CHORDS_MAX = 4;
        return "nbBars=" + chordSequence.getBarRange().size() + " chords=" + chordSequence.stream().limit(NB_CHORDS_MAX).toList()
                + (chordSequence.size() > NB_CHORDS_MAX ? "..." : "");
    }

    public String toLongString()
    {
        final int NB_NOTES_MAX = 3;
        return "cSeq=" + chordSequence
                + " rootProfile=" + chordSequence.getRootProfile()
                + " range=" + sizedPhrase.getBeatRange()
                + " phrase=" + sizedPhrase.stream().limit(NB_NOTES_MAX).toList() + (sizedPhrase.size() > NB_NOTES_MAX ? "..." : "");
    }

}
