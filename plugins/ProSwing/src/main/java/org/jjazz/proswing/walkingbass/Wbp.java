package org.jjazz.proswing.walkingbass;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Chord;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
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
    private final String rootProfile;
    private final float firstNoteBeatShift;
    private static final Logger LOGGER = Logger.getLogger(Wbp.class.getSimpleName());


    /**
     * Create an instance.
     *
     * @param cSeq               Must start at bar 0, must have a chord at beginning and use only one time signature
     * @param phrase             Can not be empty, must start at beat 0
     * @param firstNoteBeatShift A 0 or negative beat value. Any note starting at the start position should be shifted with this value.
     * @param targetNote         The ideal target note of this phrase. Can be null.
     */
    public Wbp(SimpleChordSequence cSeq, SizedPhrase phrase, float firstNoteBeatShift, Note targetNote)
    {
        checkNotNull(cSeq);
        checkNotNull(phrase);
        checkArgument(firstNoteBeatShift <= 0, "firstNoteBeatShift=%s", firstNoteBeatShift);
        checkArgument(cSeq.getBarRange().size() == (int) Math.round(phrase.getBeatRange().size() / phrase.getTimeSignature().getNbNaturalBeats())
                && phrase.getBeatRange().from == 0
                && !phrase.isEmpty()
                && cSeq.getBarRange().from == 0
                && cSeq.hasChordAtBeginning()
                && cSeq.getTimeSignature().equals(phrase.getTimeSignature()),
                "cSeq=%s phrase=%s", cSeq, phrase);


        this.chordSequence = cSeq;
        this.sizedPhrase = phrase;
        this.targetNote = targetNote;
        this.firstNoteBeatShift = firstNoteBeatShift;
        this.rootProfile = cSeq.getRootProfile();
    }

    public SimpleChordSequence getSimpleChordSequence()
    {
        return chordSequence;
    }

    public String getRootProfile()
    {
        return rootProfile;
    }

    /**
     * If &lt; 0 the phrase notes starting on bar/beat 0 should be shifted with this value.
     * <p>
     * This is used to store the exact timing of "live-played notes" which can start a bit ahead of this phrase theorical start.
     *
     * @return
     */
    public float getFirstNoteBeatShift()
    {
        return firstNoteBeatShift;
    }

    public SizedPhrase getSizedPhrase()
    {
        return sizedPhrase;
    }

    public NoteEvent getFirstNote()
    {
        return sizedPhrase.first();
    }

    public NoteEvent getLastNote()
    {
        return sizedPhrase.last();
    }

    public CLI_ChordSymbol getFirstChordSymbol()
    {
        return chordSequence.first();
    }

    public CLI_ChordSymbol getLastChordSymbol()
    {
        return chordSequence.last();
    }

    /**
     * Check if the first note of the phrase corresponds to the root of the first chord.
     *
     * @return
     */
    public boolean isStartingOnChordRoot()
    {
        var rootNote = getFirstChordSymbol().getData().getRootNote();
        boolean b = getFirstNote().equalsRelativePitch(rootNote);
        return b;
    }


    /**
     * Check if the last note of the phrase is a chord tone.
     *
     * @return
     */
    public boolean isEndingOnChordTone()
    {
        Chord lastChord = getLastChordSymbol().getData().getChord();
        int lastRelPitch = getLastNote().getRelativePitch();
        boolean b = lastChord.indexOfRelativePitch(lastRelPitch) != -1;
        return b;
    }

    /**
     * The most expected note to start the next phrase right after this phrase.
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
        return sizedPhrase.getBeatRange();
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
                + " n0shift=" + firstNoteBeatShift
                + " rg=" + sizedPhrase.getBeatRange()
                + " p=" + sizedPhrase.stream().limit(NB_NOTES_MAX).toList() + (sizedPhrase.size() > NB_NOTES_MAX ? "..." : "");
    }

}
