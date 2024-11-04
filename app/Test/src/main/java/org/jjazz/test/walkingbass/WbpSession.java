package org.jjazz.test.walkingbass;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * A session is one consistent recording of a a possibly long WalkingBassPhrase, which can be sliced in smaller WbpSource units.
 */
public class WbpSession extends Wbp
{

    private final String id;
    private final List<String> tags;
    private static final Logger LOGGER = Logger.getLogger(WbpDatabase.class.getSimpleName());

    public WbpSession(String id, List<String> tags, SimpleChordSequence cSeq, SizedPhrase phrase, Note targetNote)
    {
        super(cSeq, phrase, targetNote);
        this.id = id;
        this.tags = tags;
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
     * We extract all the possible 1/2/3/4-bar WbpSources. So for one 4-bar session phrase, the method can generate 10 WbpSource objects: 1 * 4-bar + 2 * 3-bar
     * + 3 * 2-bar + 4 * 1-bar.
     * <p>
     *
     * @param disallowNonRootStartNote     If true a WbpSource is not extracted if its first note is different from the chord root note.
     * @param disallowNonChordToneLastNote If true a WbpSource is not extracted if its last note is note a chord note (ie no transition note).
     * @return
     */
    public List<WbpSource> extractWbpSources(boolean disallowNonRootStartNote, boolean disallowNonChordToneLastNote)
    {
        List<WbpSource> res = new ArrayList<>();

        SizedPhrase sessionPhrase = getSizedPhrase();
        int sessionSizeInBars = sessionPhrase.getSizeInBars();

        for (int srcSize = 1; srcSize <= 4; srcSize++)
        {
            for (int bar = 0; bar < sessionSizeInBars - srcSize + 1; bar++)
            {
                WbpSource wbpSource = getWbpSource(bar, srcSize);
                boolean bFirst = !disallowNonRootStartNote || wbpSource.isFirstNoteChordRoot();
                boolean bLast = !disallowNonChordToneLastNote || wbpSource.isLastNoteChordTone();
                if (bFirst && bLast)
                {
                    res.add(wbpSource);
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

    private WbpSource getWbpSource(int barOffset, int nbBars)
    {
        SizedPhrase sessionPhrase = getSizedPhrase();
        TimeSignature ts = sessionPhrase.getTimeSignature();


        // Get the notes
        float beatWindow = 0.1f;
        FloatRange beatRange = new FloatRange(barOffset * ts.getNbNaturalBeats(), (barOffset + nbBars) * ts.getNbNaturalBeats());
        Phrase p = Phrases.getSlice(sessionPhrase, beatRange, true, 1, beatWindow);
        p.shiftAllEvents(-beatRange.from);
        SizedPhrase sp = new SizedPhrase(sessionPhrase.getChannel(), beatRange.getTransformed(-beatRange.from), sessionPhrase.getTimeSignature(), false);
        sp.addAll(p);


        // Get possible firstNoteBeatShift
        float firstNoteBeatShift = 0;       // By default
        if (beatRange.from - beatWindow >= 0)
        {
            p = Phrases.getSlice(sessionPhrase, new FloatRange(beatRange.from - beatWindow, beatRange.from), false, 1, 0);
            if (!p.isEmpty())
            {
                var neLast = p.last();
                if (neLast.getBeatRange().to >= beatRange.from)
                {
                    firstNoteBeatShift = -(beatRange.from - neLast.getPositionInBeats());
                }
            }
        }


        // Get the progression
        SimpleChordSequence cSeq = new SimpleChordSequence(new IntRange(0, nbBars - 1), ts);
        for (CLI_ChordSymbol cliCs : getSimpleChordSequence().subSequence(new IntRange(barOffset, barOffset + nbBars - 1), true))
        {
            Position pos = cliCs.getPosition();
            var newCliCs = (CLI_ChordSymbol) cliCs.getCopy(new Position(pos.getBar() - barOffset, pos.getBeat()));
            cSeq.add(newCliCs);
        }


        // Target note
        Note targetNote = getTargetNote();      // By default
        int nextBar = barOffset + nbBars;
        if (nextBar < sessionPhrase.getSizeInBars())
        {
            // If one bar after our phrase, find the next note
            FloatRange fr = new FloatRange(nextBar * ts.getNbNaturalBeats(), (nextBar + 1) * ts.getNbNaturalBeats());
            var nextBarNotes = sessionPhrase.getNotes(n -> true, fr, true);
            assert !nextBarNotes.isEmpty();
            targetNote = nextBarNotes.get(0);
        }


        return new WbpSource(this, barOffset, cSeq, sp, firstNoteBeatShift, targetNote);
    }

}
