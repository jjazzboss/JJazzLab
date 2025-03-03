package org.jjazz.proswing.walkingbass;

import org.jjazz.proswing.BassStyle;
import static com.google.common.base.Preconditions.checkArgument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Note;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * A 1, 2 or 4-bar bass phrase with meta-data meant to be managed of a WbpDatabase.
 * <p>
 */
public class WbpSource extends Wbp
{

    public final IntRange BASS_MAIN_PITCH_RANGE = new IntRange(28, 55);  // E1 - G3
    public final IntRange BASS_PITCH_RANGE = new IntRange(23, 64);  // B0 - E4
    private final String sessionId;
    private final String id;
    private final int sessionBarOffset;
    private final BassStyle bassStyle;
    private final List<String> tags;
    private final SimpleChordSequence originalChordSequence;

    private record TransposibilityResult(int score, int transpose)
            {

    }
    ;
    private final Map<Integer, TransposibilityResult> mapDestChordRootTransposibility;

    private static final Logger LOGGER = Logger.getLogger(WbpSource.class.getSimpleName());

    /**
     * Create a source bass phrase.
     *
     * @param sessionId          The session id from which this source phrase comes.
     * @param sessionBarFrom     The bar index in the session phrase from which this source phrase comes
     * @param bassStyle          The bass style of this WbpSource
     * @param cSeq               Must start at bar 0. Chord simplification will be applied on the passed SimpleChordSequence?
     * @param phrase             Size must be between 1 and 4 bars, must start at beat 0
     * @param firstNoteBeatShift A 0 or negative beat value. A phrase note starting at 0 should be shifted with this value.
     * @param targetNote         Can be null
     * @param tags
     * @see #setOriginalChordSequence(org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence)
     */
    public WbpSource(String sessionId, int sessionBarFrom, BassStyle bassStyle, SimpleChordSequence cSeq, SizedPhrase phrase, float firstNoteBeatShift,
            Note targetNote,
            String... tags)
    {
        super(cSeq, phrase, firstNoteBeatShift, targetNote);
        Objects.requireNonNull(bassStyle);
        checkArgument(sessionId != null && !sessionId.isBlank());
        checkArgument(sessionBarFrom >= 0, "sessionBarFrom=%s", sessionBarFrom);
        checkArgument(phrase.getSizeInBars() >= 1 && phrase.getSizeInBars() <= 4, "phrase=%s", phrase);

        this.mapDestChordRootTransposibility = new HashMap<>();
        this.originalChordSequence = cSeq.clone();
        this.sessionId = sessionId;
        this.id = sessionId + "#fr=" + sessionBarFrom + "#sz=" + phrase.getSizeInBars();
        this.sessionBarOffset = sessionBarFrom;
        this.tags = new ArrayList<>();
        this.bassStyle = bassStyle;
        Stream.of(tags).forEach(t -> this.tags.add(t.toLowerCase()));
    }

    /**
     * @return Can't be null
     */
    public BassStyle getBassStyle()
    {
        return bassStyle;
    }

    /**
     * A chord sequence copy of the SimpleChordSequence submitted in the constructor.
     * <p>
     *
     * @return @see #getSimpleChordSequence()
     * @see #simplifyChordSymbols()
     */
    public SimpleChordSequence getOriginalChordSequence()
    {
        return originalChordSequence;
    }


    public String getSessionId()
    {
        return sessionId;
    }

    public IntRange getBarRangeInSession()
    {
        int offset = getSessionBarOffset();
        return new IntRange(offset, offset + getBarRange().size() - 1);
    }

    public String getId()
    {
        return id;
    }

    public int getSessionBarOffset()
    {
        return sessionBarOffset;
    }

    public List<String> getTags()
    {
        return Collections.unmodifiableList(tags);
    }

    public boolean addTag(String tag)
    {
        boolean b = false;
        if (!tags.contains(tag))
        {
            tags.add(tag);
            b = true;
        }
        return b;
    }

    public boolean removeTag(String tag)
    {
        return tags.remove(tag);
    }

    /**
     * Simplify the chord symbols of the chord sequence so that this source phrase can be reused for a maximum of derivative chord symbols.
     *
     * @return True if the SimpleChordSequence was modified as a result
     * @see WbpSourceChordPhrase#getSimplifiedSourceChordSymbol()
     */
    public boolean simplifyChordSymbols()
    {
        boolean b = false;

        var scs = getSimpleChordSequence();
//        LOGGER.log(Level.SEVERE, "simplifyChordSequence() {0}  {1}  p={2}:", new Object[]
//        {
//            getId(), scs.toString(), getSizedPhrase().toStringSimple(true)
//        });

        for (var cliCs : scs.toArray(CLI_ChordSymbol[]::new))
        {
            var newCliCs = new WbpSourceChordPhrase(this, cliCs).getSimplifiedSourceChordSymbol();
            if (newCliCs != cliCs)
            {
                scs.remove(cliCs);
                scs.add(newCliCs);
                b = true;
//                LOGGER.log(Level.SEVERE, "  cliCs={0} => {1}", new Object[]
//                {
//                    cliCs.toString(), newCliCs.getData().toString()
//                });                
            }
        }

        return b;
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
            int score = 100;
            mapDestChordRootTransposibility.put(destChordRoot.getRelativePitch(), new TransposibilityResult(score, 0));
            return score;
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
        int transposeUp = srcChordRoot.getRelativeAscInterval(destChordRoot);
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
        int transposeDown = srcChordRoot.getRelativeDescInterval(destChordRoot);
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
        boolean tDown;
        if (countOutsideUp == countOutsideDown)
        {
            if (countBassRangeDown == countBassRangeUp)
            {
                tDown = Math.abs(pitchAvg - transposeDown - IDEAL_CENTRAL_PITCH) < Math.abs(pitchAvg + transposeUp - IDEAL_CENTRAL_PITCH);
            } else
            {
                tDown = countBassRangeDown < countBassRangeUp;
            }
        } else
        {
            tDown = countOutsideDown < countOutsideUp;
        }


        float countOutside = tDown ? countOutsideDown : countOutsideDown;
        float countBassRange = tDown ? countBassRangeDown : countBassRangeUp;
        int transpose = tDown ? -transposeDown : transposeUp;
        float distanceToIdealCentralPitch = Math.min(11, Math.abs(pitchAvg + transpose - IDEAL_CENTRAL_PITCH));
        float outsideRatio = countOutside / nbNotes;
        float bassRangeRatio = countBassRange / nbNotes;
        float idealShiftRatio = distanceToIdealCentralPitch / 11;
        float ratio = (10 * outsideRatio + 5 * idealShiftRatio + bassRangeRatio) / 16f;


        int res = Math.round(95 * (1 - ratio));

        // Save cache
        mapDestChordRootTransposibility.put(destChordRoot.getRelativePitch(), new TransposibilityResult(res, transpose));
        LOGGER.log(Level.FINE, "getTransposibilityScore() countOutsideDown={0} countOutsideUp={1} countBassRangeDown={2} countBassRangeUp={3} pitchAvg={4}",
                new Object[]
                {
                    countOutsideDown, countOutsideUp, countBassRangeDown, countBassRangeUp, pitchAvg
                });
        LOGGER.log(Level.FINE,
                "                          srcChordRoot={0} destChordRoot={1} distToCentralPitch={2} transpose={3} score={4} phrase={5} ",
                new Object[]
                {
                    srcChordRoot, destChordRoot, distanceToIdealCentralPitch, transpose, res, getSizedPhrase()
                });

        return res;
    }

    /**
     * Get the optimal transposition to apply to the source phrase so that first chord root becomes destChordRoot.
     *
     * @param destChordRoot
     * @return
     */
    public int getRequiredTransposition(Note destChordRoot)
    {
        var tr = mapDestChordRootTransposibility.get(destChordRoot.getRelativePitch());
        if (tr == null)
        {
            getTransposibilityScore(destChordRoot);     // This will update mapDestChordRootTransposibility to get transposition direction
            tr = mapDestChordRootTransposibility.get(destChordRoot.getRelativePitch());
            assert tr != null;
        }

        return tr.transpose();
    }

    /**
     * Get the source phrase transposed so that first chord symbol root becomes destChordRoot.
     *
     * @param destChordRoot
     * @return
     */
    public SizedPhrase getTransposedPhrase(Note destChordRoot)
    {
        int transpose = getRequiredTransposition(destChordRoot);
        var sp = getSizedPhrase();
        SizedPhrase res = new SizedPhrase(sp.getChannel(), sp.getBeatRange(), sp.getTimeSignature(), sp.isDrums());
        Phrase p = sp.getProcessedPhrasePitch(pitch -> pitch + transpose);
        LOGGER.log(Level.FINE, "getTransposedPhrase() transpose={0} => p={1}", new Object[]
        {
            transpose, p
        });

        res.add(p);

        return res;
    }


    public boolean hasTag(String tag)
    {
        return tags.contains(tag.toLowerCase());
    }


    @Override
    public String toString()
    {
        return "WbpSource id=" + id + " " + super.toString();
    }

    @Override
    public String toLongString()
    {
        var res = String.format("WbpSource id=%s tags=%s %s", id, tags, super.toLongString());
        return res;
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================    

}
