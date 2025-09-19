package org.jjazz.jjswing.bass.db;

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
    protected final SimpleChordSequence chordSequence;
    protected final SizedPhrase sizedPhrase;
    private final Note targetNote;
    private static final Logger LOGGER = Logger.getLogger(Wbp.class.getSimpleName());


    /**
     * Create an instance.
     *
     * @param cSeq               Must start at bar 0, must have a chord at beginning and use only one time signature
     * @param phrase             Can not be empty, must start at bar/beat 0
     * @param targetNote         The ideal target note of this phrase. Can be null.
     */
    public Wbp(SimpleChordSequence cSeq, SizedPhrase phrase, Note targetNote)
    {
        checkNotNull(cSeq);
        checkNotNull(phrase);
        checkArgument(cSeq.getBarRange().size() == (int) Math.round(phrase.getNotesBeatRange().size() / phrase.getTimeSignature().getNbNaturalBeats())
                && phrase.getNotesBeatRange().from == 0
                && !phrase.isEmpty()
                && cSeq.getBarRange().from == 0
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
  

    public SizedPhrase getSizedPhrase()
    {
        return sizedPhrase;
    }

    /**
     * The most expected note to start the next phrase.
     *
     * @return Can be null
     */
    public Note getTargetNote()
    {
        return targetNote;
    }

    /**
     * The bar range starting at bar 0.
     *
     * @return
     */
    public IntRange getBarRange()
    {
        return chordSequence.getBarRange();
    }

    public TimeSignature getTimeSignature()
    {
        return sizedPhrase.getTimeSignature();
    }

    /**
     * The beat range starting at beat 0.
     *
     * @return
     */
    public FloatRange getBeatRange()
    {
        return sizedPhrase.getNotesBeatRange();
    }

    @Override
    public String toString()
    {
        final int NB_CHORDS_MAX = 4;
        return "rg=" + chordSequence.getBarRange() + " chords=" + chordSequence.stream().limit(NB_CHORDS_MAX).toList()
                + (chordSequence.size() > NB_CHORDS_MAX ? "..." : "");
    }

    public String toLongString()
    {
        final int NB_NOTES_MAX = 5;
        return "cSeq=" + chordSequence
                + " rg=" + sizedPhrase.getNotesBeatRange()
                + " p=" + sizedPhrase.stream().limit(NB_NOTES_MAX).toList() + (sizedPhrase.size() > NB_NOTES_MAX ? "..." : "");
    }

}
