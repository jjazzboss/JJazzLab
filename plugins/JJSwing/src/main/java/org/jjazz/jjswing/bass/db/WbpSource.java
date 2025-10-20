package org.jjazz.jjswing.bass.db;

import com.google.common.base.Preconditions;
import org.jjazz.jjswing.api.BassStyle;
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
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.jjswing.bass.BassGenerator;
import org.jjazz.jjswing.bass.WbpSourceSlice;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * A 1, 2 or 4-bar bass phrase with meta-data meant to be managed by a WbpDatabase.
 * <p>
 */
public class WbpSource extends Wbp
{

    public static final IntRange BASS_GOOD_PITCH_RANGE = new IntRange(28, 52);  // E1 - E3
    public static final IntRange BASS_EXTENDED_PITCH_RANGE = new IntRange(28, 64);  // E1 - E4
    private final String sessionId;
    private final String id;
    private final int sessionBarOffset;
    private final BassStyle bassStyle;
    private final List<String> tags;
    private final SimpleChordSequence originalChordSequence;
    private final float firstNoteBeatShift;
    private NoteEvent firstNote, lastNote;
    private Boolean isStartingOnRoot, isEndingOnChordTone;
    private CLI_ChordSymbol firstChord, lastChord;
    private WbpSourceStats stats;
    private RootProfile rootProfile;
    private final Map<CLI_ChordSymbol, WbpSourceSlice> mapCsSlice;
    private final Map<Integer, TransposibilityResult> mapDestChordRootTransposibility;
    private static final Logger LOGGER = Logger.getLogger(WbpSource.class.getSimpleName());


    /**
     * Create a source bass phrase.
     * <p>
     *
     * @param sessionId          The session id from which this source phrase comes.
     * @param sessionBarFrom     The bar index in the session phrase from which this source phrase comes
     * @param bassStyle          The bass style of this WbpSource
     * @param cSeq               Must start at bar 0. The source chord sequence. A copy is made which is chord-simplified based on phrase.
     * @param phrase             Size must be between 1 and 4 bars, must start at beat 0. Note that phrase might be modified in order to fix issues.
     * @param firstNoteBeatShift A 0 or negative beat value. Any phrase note at bar=0 beat=0 should be shifted with this value.
     * @param targetNote         Can be null
     * @param tags
     * @see #getOriginalChordSequence()
     * @see #getSimpleChordSequence()
     */
    public WbpSource(String sessionId, int sessionBarFrom, BassStyle bassStyle, SimpleChordSequence cSeq, SizedPhrase phrase, float firstNoteBeatShift,
            Note targetNote,
            String... tags)
    {
        super(cSeq.deepClone(), phrase, targetNote);
        Objects.requireNonNull(bassStyle);
        checkArgument(sessionId != null && !sessionId.isBlank());
        checkArgument(sessionBarFrom >= 0, "sessionBarFrom=%s", sessionBarFrom);
        checkArgument(firstNoteBeatShift <= 0 && firstNoteBeatShift >= -BassGenerator.NON_QUANTIZED_WINDOW,
                "firstNoteBeatShift=%s", firstNoteBeatShift);
        checkArgument(phrase.getSizeInBars() >= 1 && phrase.getSizeInBars() <= 4, "phrase=%s", phrase);

        this.mapDestChordRootTransposibility = new HashMap<>();
        this.mapCsSlice = new HashMap<>();
        this.originalChordSequence = cSeq.deepClone();
        this.sessionId = sessionId;
        this.id = sessionId + "#fr=" + sessionBarFrom + "#sz=" + phrase.getSizeInBars();
        this.sessionBarOffset = sessionBarFrom;
        this.bassStyle = bassStyle;
        this.firstNoteBeatShift = firstNoteBeatShift;
        this.tags = new ArrayList<>();
        Stream.of(tags).forEach(t -> this.tags.add(t.toLowerCase()));


        fixShortNotesPostSessionSlice(phrase);
        Phrases.fixEndOfPhraseNotes(phrase);
        fixOctave(phrase);
        Velocities.normalizeBassVelocities(phrase, 0.5f);


        // Must be called last because WbpSource must be fully initialized
        simplifyChordSymbols();
    }

    /**
     * Only rely on Id.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.id);
        return hash;
    }

    /**
     * Only rely on Id.
     *
     * @return
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final WbpSource other = (WbpSource) obj;
        return Objects.equals(this.id, other.id);
    }


    /**
     * If &lt; 0 the phrase notes starting on bar/beat 0 should be shifted with this value.
     * <p>
     * This is used to store the exact timing of "live-played notes" which can start a bit ahead of this phrase theorical start.
     *
     * @return A 0 or negative value
     */
    public float getFirstNoteBeatShift()
    {
        return firstNoteBeatShift;
    }

//    /**
//     * Manually set the RootProfile.
//     * <p>
//     * When creating WbpSources from a WbpSession we can directly compute the RootProfile in a more efficient way that calculating it for each individual
//     * WbpSource.
//     *
//     * @param rp
//     */
//    protected void setRootProfile(RootProfile rp)
//    {
//        rootProfile = rp;
//    }

    public RootProfile getRootProfile()
    {
        if (rootProfile == null)
        {
            rootProfile = RootProfile.of(chordSequence);
        }
        return rootProfile;
    }

    public NoteEvent getFirstNote()
    {
        // Called often, save result because underlying structure is a TreeSet        
        if (firstNote == null)
        {
            firstNote = sizedPhrase.first();
        }
        return firstNote;
    }

    public NoteEvent getLastNote()
    {
        // Called often, save result because underlying structure is a TreeSet        
        if (lastNote == null)
        {
            lastNote = sizedPhrase.last();
        }
        return lastNote;
    }

    public CLI_ChordSymbol getFirstChordSymbol()
    {
        // Called often, save result because underlying structure is a TreeSet        
        if (firstChord == null)
        {
            firstChord = chordSequence.first();
        }
        return firstChord;
    }

    public CLI_ChordSymbol getLastChordSymbol()
    {
        // Called often, save result because underlying structure is a TreeSet        
        if (lastChord == null)
        {
            lastChord = chordSequence.last();
        }
        return lastChord;
    }

    /**
     * Check if the first note of the phrase corresponds to the bass of the first chord.
     *
     * @return
     */
    public boolean isStartingOnChordBass()
    {
        if (isStartingOnRoot == null)
        {
            Note bassNote = getFirstChordSymbol().getData().getBassNote();
            isStartingOnRoot = getFirstNote().equalsRelativePitch(bassNote);
        }
        return isStartingOnRoot;
    }

    /**
     * Check if the last note of the phrase is a chord tone or the bass note.
     *
     * @return
     */
    public boolean isEndingOnChordTone()
    {
        if (isEndingOnChordTone == null)
        {
            var lastChordSymbol = getLastChordSymbol().getData();
            int lastRelPitch = getLastNote().getRelativePitch();
            isEndingOnChordTone = lastChordSymbol.getChord().indexOfRelativePitch(lastRelPitch) != -1 || lastRelPitch == lastChordSymbol.getBassNote().getRelativePitch();
        }
        return isEndingOnChordTone;
    }

    /**
     * Get the WbpSourceSlice for srcCliCs.
     * <p>
     * Results are cached.
     *
     * @param srcCliCs Must belong to this WbpSource chord sequence.
     * @return
     */
    public WbpSourceSlice getSlice(CLI_ChordSymbol srcCliCs)
    {
        Preconditions.checkArgument(getSimpleChordSequence().contains(srcCliCs), "this=%s srcCliCs=%s", this, srcCliCs);
        var res = mapCsSlice.get(srcCliCs);
        if (res == null)
        {
            res = new WbpSourceSlice(this, srcCliCs);
            mapCsSlice.put(srcCliCs, res);
        }
        return res;
    }

    public WbpSourceStats getStats()
    {
        if (stats == null)
        {
            stats = WbpSourceStats.of(this);
        }
        return stats;
    }

    /**
     * @return Can't be null
     */
    public BassStyle getBassStyle()
    {
        return bassStyle;
    }

    /**
     * Return a chord-simplified copy of the SimpleChordSequence submitted in the constructor.
     *
     * @return
     */
    @Override
    public SimpleChordSequence getSimpleChordSequence()
    {
        return chordSequence;
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
     * Get a score which indicates how much the original phrase will preserve an acceptable bass pitch range when transposed to destChordRoot.
     * <p>
     * Return value will be &lt; 50 if there is at least one note which becomes out of BASS_EXTENDED_PITCH_RANGE.
     * <p>
     * Return values are cached to speed up next calls with the same note.
     *
     * @param destChordRoot The target root of the first chord symbol
     * @return [0;100]
     */
    public int getTransposabilityScore(Note destChordRoot)
    {
        Note srcChordRoot = getFirstChordSymbol().getData().getRootNote();
        int destChordRelPitch = destChordRoot.getRelativePitch();
        int rootPitchDistance = Math.abs(destChordRelPitch - srcChordRoot.getRelativePitch());
        if (rootPitchDistance == 0)
        {
            int score = 100;
            mapDestChordRootTransposibility.put(destChordRelPitch, new TransposibilityResult(score, 0));
            return score;
        }

        // Check cache
        var tr = mapDestChordRootTransposibility.get(destChordRelPitch);
        if (tr != null)
        {
            return tr.score();
        }

        int nbNotes = getSizedPhrase().size();

        // Count transposing up
        int countGoodUp = 0;
        int countExtendedUp = 0;
        int countOutsideUp = 0;
        int transposeUp = srcChordRoot.getRelativeAscInterval(destChordRoot);
        int pitchSum = 0;
        for (var ne : getSizedPhrase())
        {
            pitchSum += ne.getPitch();
            int pitch = ne.getPitch() + transposeUp;
            if (BASS_GOOD_PITCH_RANGE.contains(pitch))
            {
                countGoodUp++;
            } else if (BASS_EXTENDED_PITCH_RANGE.contains(pitch))
            {
                countExtendedUp++;
            } else
            {
                countOutsideUp++;
            }
        }
        int pitchAvg = Math.round((float) pitchSum / nbNotes);


        // Count transposing down
        int countGoodDown = 0;
        int countExtendedDown = 0;
        int countOutsideDown = 0;
        int transposeDown = srcChordRoot.getRelativeDescInterval(destChordRoot);
        for (var ne : getSizedPhrase())
        {
            int pitch = ne.getPitch() - transposeDown;
            if (BASS_GOOD_PITCH_RANGE.contains(pitch))
            {
                countGoodDown++;
            } else if (BASS_EXTENDED_PITCH_RANGE.contains(pitch))
            {
                countExtendedDown++;
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
            if (countExtendedDown == countExtendedUp)
            {
                tDown = Math.abs(pitchAvg - transposeDown - IDEAL_CENTRAL_PITCH) < Math.abs(pitchAvg + transposeUp - IDEAL_CENTRAL_PITCH);
            } else
            {
                tDown = countExtendedDown < countExtendedUp;
            }
        } else
        {
            tDown = countOutsideDown < countOutsideUp;
        }


        float countOutside = tDown ? countOutsideDown : countOutsideDown;
        float countExtended = tDown ? countExtendedDown : countExtendedUp;
        float countGood = tDown ? countGoodDown : countGoodUp;
        int transpose = tDown ? -transposeDown : transposeUp;
        float distanceToIdealCentralPitch = Math.min(11, Math.abs(pitchAvg + transpose - IDEAL_CENTRAL_PITCH)); // [1-11]
        float ratioDistanceToIdealCentralPitch = (11 - distanceToIdealCentralPitch) / 11;
        float ratioGoodToOutside = safeRatio(countGood, countGood + countOutside);
        float ratioGoodToExtended = safeRatio(countGood, countGood + countExtended);


        float maxFactor = countOutside > 0 ? 0.49f : 1f;
        float ratio = maxFactor * (60 * ratioGoodToOutside + 20 * ratioGoodToExtended + 20 * ratioDistanceToIdealCentralPitch);
        int res = Math.round(ratio);


        // Save cache
        mapDestChordRootTransposibility.put(destChordRelPitch, new TransposibilityResult(res, transpose));
        LOGGER.log(Level.FINE, "getTransposibilityScore() countGood={0} countOutside={1} countExtended={2}",
                new Object[]
                {
                    countGood, countOutside, countExtended
                });
        LOGGER.log(Level.FINE,
                "                          srcChordRoot={0} destChordRoot={1} transpose={2} score={3} phrase={4} ",
                new Object[]
                {
                    srcChordRoot, destChordRoot, transpose, res, getSizedPhrase()
                });

        return res;
    }

    /**
     * Get the optimal transposition to apply to the source phrase so that first chord root becomes destChordRoot.
     * <p>
     * Return values are cached to speed up next calls with the same note.
     *
     * @param destChordRoot
     * @return
     */
    public int getRequiredTransposition(Note destChordRoot)
    {
        var tr = mapDestChordRootTransposibility.get(destChordRoot.getRelativePitch());
        if (tr == null)
        {
            getTransposabilityScore(destChordRoot);     // This will update mapDestChordRootTransposibility to get transposition direction
            tr = mapDestChordRootTransposibility.get(destChordRoot.getRelativePitch());
            if (tr == null)
            {
                // Robustness test: happened sometimes, but not trivial to reproduce, race condition ? 
                tr = new TransposibilityResult(0, 0);
                LOGGER.log(Level.WARNING, "getRequiredTransposition() destChordRoot={0} Unexpected tr=null ! Using instead tr={1}", new Object[]
                {
                    destChordRoot, tr
                });
            }
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
        SizedPhrase res = new SizedPhrase(sp.getChannel(), sp.getNotesBeatRange(), sp.getTimeSignature(), sp.isDrums());
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
        return "WbpSource id=" + id + " style=" + bassStyle + " " + super.toString();
    }

    @Override
    public String toLongString()
    {
        var res = String.format("WbpSource id=%s style=%s tags=%s %s", id, bassStyle, tags, super.toLongString());
        return res;
    }


    // =================================================================================================================
    // Private methods
    // =================================================================================================================    
    /**
     * Simplify the chord symbols of the chord sequence so that this source phrase can be reused for a maximum of derivative chord symbols.
     *
     * @see WbpSourceSlice#getSimplifiedSourceChordSymbol()
     */
    private void simplifyChordSymbols()
    {
        for (var cliCs : chordSequence.toArray(CLI_ChordSymbol[]::new))
        {
            var newCliCs = getSlice(cliCs).getSimplifiedSourceChordSymbol();
            if (newCliCs != cliCs)
            {
                chordSequence.remove(cliCs);
                chordSequence.add(newCliCs);
//                LOGGER.log(Level.SEVERE, "simplifyChordSymbols()  cliCs={0} => {1}", new Object[]
//                {
//                    cliCs.toString(), newCliCs.getData().toString()
//                });                
            }
        }
    }


    /**
     * Transpose 1 octave down if too high<br>
     *
     * @param sp
     * @return
     */
    private boolean fixOctave(SizedPhrase sp)
    {
        boolean b = false;
        var pitchRange = Phrases.getPitchRange(sp);
        if (pitchRange.from >= 47 //  [B2+]
                || (pitchRange.from >= 44 && pitchRange.to >= 56))   // [G2;G3+]
        {
            sp.processPitch(pitch -> pitch - 12);
            b = true;
            LOGGER.log(Level.FINE, "fixOctave() Transposing 1 octave down wbpSource={0} p={1}", new Object[]
            {
                this, sp
            });
        }
        return b;
    }


    /**
     * Fix very short notes left on first beat due to session slicing.<br>
     * <p>
     * -
     * - Transpose 1 octave down if too high<br>
     * - Adjust some notes velocities<br>
     *
     * @param sp
     * @return
     */
    private boolean fixShortNotesPostSessionSlice(SizedPhrase sp)
    {
        // Session slicing might have produced remaining short notes at the beginning of the phrase
        boolean b = sp.removeIf(ne -> 
        {
            float pos = ne.getPositionInBeats();
            float dur = ne.getDurationInBeats();
            return (pos % sp.getTimeSignature().getNaturalBeat()) == 0 && dur <= BassGenerator.GHOST_NOTE_MAX_DURATION;
        });
        return b;
    }

    private float safeRatio(float upper, float lower)
    {
        return lower == 0 ? 1 : upper / lower;
    }


    // =================================================================================================================
    // Inner classes
    // =================================================================================================================    
    private record TransposibilityResult(int score, int transpose)
            {

    }

}
