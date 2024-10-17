package org.jjazz.test.walkingbass;

import static com.google.common.base.Preconditions.checkArgument;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * A 1, 2 or 4-bar source bass phrase, extracted from a WbpSession.
 * <p>
 * Manage the fact that some live-played phrases can actually start a bit before the theorical start of the SizedPhrase.
 */
public class WbpSource extends Wbp
{

    public final IntRange BASS_MAIN_PITCH_RANGE = new IntRange(28, 55);  // E1 - G3
    public final IntRange BASS_PITCH_RANGE = new IntRange(23, 64);  // B0 - E4
    private final String sessionId;
    private final int sessionBarOffset;
    private final float firstNoteBeatShift;
    private final List<String> tags;
    private final String rootProfile;

    private record TransposibilityResult(int score, int transpose)
            {

    }
    ;
    private final Map<Integer, TransposibilityResult> mapDestChordRootTransposibility;

    private static final Logger LOGGER = Logger.getLogger(WbpSource.class.getSimpleName());

    /**
     * Create a source bass phrase.
     *
     * @param session            The session from which this source phrase comes.
     * @param sessionBarFrom     The bar index in the session phrase from which this source phrase comes
     * @param cSeq
     * @param phrase             Size must be 1, 2 or 4 bars.
     * @param firstNoteBeatShift A 0 or negative beat value. A phrase note starting at 0 should be shifted with this value.
     * @param targetNote
     */
    public WbpSource(WbpSession session, int sessionBarFrom, SimpleChordSequence cSeq, SizedPhrase phrase, float firstNoteBeatShift, Note targetNote)
    {
        super(cSeq, phrase, targetNote);
        checkArgument(phrase.getSizeInBars() == 1 || phrase.getSizeInBars() == 2 || phrase.getSizeInBars() == 4, "phrase=%s", phrase);
        checkArgument(firstNoteBeatShift <= 0, "firstNoteBeatShift=%s", firstNoteBeatShift);
        this.sessionId = session.getId();
        this.sessionBarOffset = sessionBarFrom;
        this.tags = session.getTags();
        this.firstNoteBeatShift = firstNoteBeatShift;
        this.rootProfile = cSeq.getRootProfile();
        mapDestChordRootTransposibility = new HashMap<>();
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


    public String getSessionId()
    {
        return sessionId;
    }

    public int getSessionBarOffset()
    {
        return sessionBarOffset;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public String getRootProfile()
    {
        return rootProfile;
    }

    /**
     * Get a score which indicates how much the original phrase will preserve an acceptable bass pitch range when transposed to destChordRoot.
     *
     * @param destChordRoot The target root of the first chord symbol
     * @return [0;100]
     */
    public int getTransposibilityScore(Note destChordRoot)
    {
        Note srcChordRoot = getSimpleChordSequence().first().getData().getRootNote();
        if (destChordRoot.equalsRelativePitch(srcChordRoot))
        {
            return 100;
        }

        // Check cache
        var tr = mapDestChordRootTransposibility.get(destChordRoot.getRelativePitch());
        if (tr != null)
        {
            return tr.score();
        }


        int nbNotes = getSizedPhrase().size();

        // Count transposing up
        int countMainUp = 0;
        int countBassRangeUp = 0;
        int countOutsideUp = 0;
        int transposeUp = srcChordRoot.getRelativeAscInterval(srcChordRoot);
        int pitchSum = 0;
        for (var ne : getSizedPhrase())
        {
            pitchSum += ne.getPitch();
            int pitch = ne.getPitch() + transposeUp;
            if (BASS_MAIN_PITCH_RANGE.contains(pitch))
            {
                countMainUp++;
            } else if (BASS_PITCH_RANGE.contains(pitch))
            {
                countBassRangeUp++;
            } else
            {
                countOutsideUp++;
            }
        }
        int pitchAvg = Math.round((float) pitchSum / nbNotes);


        // Count transposing down
        int countMainDown = 0;
        int countBassRangeDown = 0;
        int countOutsideDown = 0;
        int transposeDown = srcChordRoot.getRelativeDescInterval(srcChordRoot);
        for (var ne : getSizedPhrase())
        {
            int pitch = ne.getPitch() - transposeDown;
            if (BASS_MAIN_PITCH_RANGE.contains(pitch))
            {
                countMainDown++;
            } else if (BASS_PITCH_RANGE.contains(pitch))
            {
                countBassRangeDown++;
            } else
            {
                countOutsideDown++;
            }
        }


        // Compare the 2 options
        final int IDEAL_CENTRAL_PITCH = 40;         // E2
        boolean doDown;
        if (countOutsideUp == 0 && countOutsideDown == 0)
        {
            if (countBassRangeDown == 0 && countBassRangeUp == 0)
            {
                doDown = Math.abs(pitchAvg - transposeDown - IDEAL_CENTRAL_PITCH) < Math.abs(pitchAvg + transposeUp - IDEAL_CENTRAL_PITCH);
            } else
            {
                doDown = countBassRangeDown < countBassRangeUp;
            }
        } else
        {
            doDown = countOutsideDown < countOutsideUp;
        }


        float countOutside = doDown ? countOutsideDown : countOutsideUp;
        float countBassRange = doDown ? countBassRangeDown : countBassRangeUp;
        int transpose = doDown ? -transposeDown : transposeUp;
        float distanceToIdealCentralPitch = Math.max(11, Math.abs(pitchAvg + transpose - IDEAL_CENTRAL_PITCH));
        float outsideRatio = countOutside / nbNotes;
        float bassRangeRatio = countBassRange / nbNotes;
        float idealShiftRatio = distanceToIdealCentralPitch / 11;
        float ratio = (10 * outsideRatio + 5 * idealShiftRatio + bassRangeRatio) / 16f;


        int res = Math.round(90 * (1 - ratio));
        
        // Save cache
        mapDestChordRootTransposibility.put(destChordRoot.getRelativePitch(), new TransposibilityResult(res, transpose));

        LOGGER.log(Level.SEVERE, "getTransposibilityScore() destChordRoot={0} res={1}", new Object[]
        {
            destChordRoot, res
        });

        return res;
    }


    /**
     * Get the source phrase transposed so that first chord symbol root becomes destChordRoot.
     *
     * @param destChordRoot
     * @return
     */
    public SizedPhrase getTransposedPhrase(Note destChordRoot)
    {
        var tr = mapDestChordRootTransposibility.get(destChordRoot.getRelativePitch());
        if (tr == null)
        {
            getTransposibilityScore(destChordRoot); // This will update mapDestChordRootTransposibility to get transposition direction
            tr = mapDestChordRootTransposibility.get(destChordRoot.getRelativePitch());
            assert tr != null;
        }
        int transpose = tr.transpose();
        SizedPhrase sp = new SizedPhrase(getSizedPhrase());
        sp.clear();
        Phrase p = getSizedPhrase().getProcessedPhrasePitch(pitch -> pitch + transpose);
        sp.add(p);

        return sp;
    }

    /**
     * Check if the first note of the phrase corresponds to the root of the first chord.
     *
     * @return
     */
    public boolean isFirstNoteChordRoot()
    {
        var rootNote = getSimpleChordSequence().first().getData().getRootNote();
        boolean b = getSizedPhrase().first().equalsRelativePitch(rootNote);
        return b;
    }


    public boolean hasTag(String tag)
    {
        return tags == null ? false : tags.contains(tag.toLowerCase());
    }


    @Override
    public String toString()
    {
        return "WbpSource sessionId=" + sessionId + " " + super.toString();
    }

    @Override
    public String toLongString()
    {
        var res = String.format("WbpSource sessionId=%s/%d beat0Shift=%.2f tags=%s %s", sessionId, sessionBarOffset, firstNoteBeatShift, tags, super.toLongString());
        return res;
    }
}
