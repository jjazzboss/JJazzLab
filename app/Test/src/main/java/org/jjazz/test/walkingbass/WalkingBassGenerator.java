package org.jjazz.test.walkingbass;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Variation;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.utilities.api.IntRange;

/**
 * Walking bass generator based on pre-recorder patterns.
 */
public class WalkingBassGenerator implements MusicGenerator
{

    private final Rhythm rhythm;
    private SongContext context;

    /**
     * The Chord Sequence with all the chords.
     */
    private SongChordSequence songChordSequence;
    private List<String> tags;
    private static final Logger LOGGER = Logger.getLogger(WalkingBassGenerator.class.getSimpleName());

    public WalkingBassGenerator(Rhythm r)
    {
        Preconditions.checkArgument(RP_STD_Variation.getVariationRp(r) != null
                && RP_STD_Intensity.getIntensityRp(r) != null,
                "r=%s", r);
        rhythm = r;
    }

    /**
     * Only process bass tracks.
     *
     * @param context
     * @param rvs
     * @return
     * @throws MusicGenerationException
     */
    @Override
    public HashMap<RhythmVoice, Phrase> generateMusic(SongContext context, RhythmVoice... rvs) throws MusicGenerationException
    {
        this.context = context;


        var rhythmRvs = rhythm.getRhythmVoices();
        var rvsList = List.of(rvs);
        Preconditions.checkArgument(Stream.of(rvs).allMatch(rv -> rhythmRvs.contains(rv)), "rvs=", rvsList);
        var bassRvs = new ArrayList<>(rvs.length == 0 ? rhythmRvs : rvsList);
        bassRvs.removeIf(rv -> rv.getType() != RhythmVoice.Type.BASS);
        if (bassRvs.isEmpty())
        {
            throw new IllegalArgumentException("No bass track found. rhythm=" + rhythm + " rvs=" + rvsList);
        }


        // The main chord sequence
        songChordSequence = new SongChordSequence(context.getSong(), context.getBarRange());   // Will have a chord at beginning. Handle alternate chord symbols.       

        // Try to guess some tags (eg blues, slow, medium, fast, modal, ...) to influence the bass pattern selection
        tags = guessTags(context);


        System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "[%1$s p=%2$.1f d=%3$.1f]");
        WbpDatabase.getInstance().dump();
        LOGGER.log(Level.SEVERE, "generateMusic() -- rhythm={0} contextChordSequence={1}", new Object[]
        {
            rhythm.getName(), songChordSequence
        });


        // Get all the bass phrases that make the song
        var allPhrases = getAllPhrases();


        // Concatenate the bass phrases for each rv
        HashMap<RhythmVoice, Phrase> res = new HashMap<>();
        for (var rv : bassRvs)
        {
            int channel = getChannelFromMidiMix(rv);
            Phrase pRes = new Phrase(channel, false);
            for (var p : allPhrases)
            {
                pRes.add(p);
            }
            res.put(rv, pRes);
        }


        return res;
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================

    /**
     * Get the bass phrase for each of the SimpleChordSequences which make the song context.
     * <p>
     * Create one SimpleChordSequence for several contiguous parts sharing the same rhythm and same style part.
     *
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private List<Phrase> getAllPhrases() throws MusicGenerationException
    {
        LOGGER.fine("getAllPhrases() --");


        // Split the song structure in chord sequences of consecutive sections having the same rhythm and same RhythmParameter value
        var splitResults = songChordSequence.split(rhythm, RP_STD_Variation.getVariationRp(rhythm));


        List<Phrase> res = new ArrayList<>();
        for (SongChordSequence.SplitResult<String> splitResult : splitResults)
        {
            // Generate music for each chord sequence
            SimpleChordSequence cSeq = splitResult.simpleChordSequence();
            String rpValue = splitResult.rpValue();
            var phrase = getPhrase(cSeq, rpValue);
            res.add(phrase);
        }

        return res;
    }

    /**
     * Get the bass phase from the specified SimpleChordSequence.
     *
     * @param scs
     * @param rpVariationValue
     * @return
     * @throws MusicGenerationException
     */
    private Phrase getPhrase(SimpleChordSequence scs, String rpVariationValue) throws MusicGenerationException
    {
        LOGGER.log(Level.SEVERE, "getPhrase() -- scs={0} rpVariationValue={1}", new Object[]
        {
            scs, rpVariationValue
        });


        // Build the tiling of scs with WbpSources
        WbpSourceTiling wbpSrcTiling = new WbpSourceTiling(scs);

        // For each bar, get the most compatible 4-bar WbpSource
        var wbpSrcArray4 = new WbpSourceArray(scs, 4, scs.getBarRange().stream().boxed().toList());
        setBestWbpSourceForEachUsableBar(wbpSrcArray4, 4);
        wbpSrcTiling.tileMostCompatibleFirst(wbpSrcArray4);
        LOGGER.log(Level.SEVERE, "getPhrase() ----- 4-bar\n{0}", wbpSrcTiling.toString());

        // For each remaining 2-bar zone, get the most compatible 2-bar WbpSource
        var usableBars2 = wbpSrcTiling.getUntiledZonesStartBarIndexes(2);
        var wbpSrcArray2 = new WbpSourceArray(scs, 2, usableBars2);
        setBestWbpSourceForEachUsableBar(wbpSrcArray2, 2);
        wbpSrcTiling.tileMostCompatibleFirst(wbpSrcArray2);
        LOGGER.log(Level.SEVERE, "getPhrase() ----- 2-bar \n{0}", wbpSrcTiling.toString());

        // For each remaining 1-bar zone, get the most compatible 1-bar WbpSource
        var usableBars1 = wbpSrcTiling.getUntiledZonesStartBarIndexes(1);
        var wbpSrcArray1 = new WbpSourceArray(scs, 1, usableBars1);
        setBestWbpSourceForEachUsableBar(wbpSrcArray1, 1);
        wbpSrcTiling.tileMostCompatibleFirst(wbpSrcArray1);
        LOGGER.log(Level.SEVERE, "getPhrase() ----- 1-bar \n{0}", wbpSrcTiling.toString());


        // We got the source phrases
        var wbpSources = wbpSrcTiling.getWbpSources(-1);


        // Transpose source phrases as required
        Phrase res = new Phrase(0);             // channel useless here
        int bar = scs.getBarRange().from;
        for (var wbpSource : wbpSources)
        {
            var br = wbpSource.getBarRange();
            var subSeq = scs.subSequence(br.getTransformed(bar), false);
            var firstRootNote = subSeq.first().getData().getRootNote();
            var p = wbpSource.getTransposedPhrase(firstRootNote);
            p.shiftAllEvents(bar * scs.getTimeSignature().getNbNaturalBeats());
            res.add(p, false);
            bar += br.size();
        }
        return res;
    }


    /**
     * For each usable bar set the most compatible WbpSource of a given size.
     *
     * @param wbpSrcArray
     * @param size        Size in bars of each WbpSource
     * @return True if WbpSrcArray was updated
     */
    private boolean setBestWbpSourceForEachUsableBar(WbpSourceArray wbpSrcArray, int size)
    {
        Preconditions.checkArgument(size > 0, "size=%s", size);

        boolean b = false;
        var scs = wbpSrcArray.getSimpleChordSequence();

        for (int bar : wbpSrcArray.getUsableBarIndexes())
        {
            int lastBar = bar + size - 1;
            if (lastBar > scs.getBarRange().to)
            {
                break;
            }
            var subSeq = scs.subSequence(new IntRange(bar, lastBar), true);
            var wbpSources = getRootProfileCompatibleWbpSources(subSeq);
            var wbpSource = getBestWbpSource(wbpSources, subSeq);   // Might be null
            if (wbpSource != null)
            {
                wbpSrcArray.set(bar, wbpSource);
                b = true;
            }
        }
        return b;
    }

    /**
     * Find the WbpSource which is the most compatible with scs.
     * <p>
     *
     * @param wbpSources
     * @param scs
     * @return Can be null.
     */
    private WbpSource getBestWbpSource(List<WbpSource> wbpSources, SimpleChordSequence scs)
    {
        float maxScore = 0;
        WbpSource res = null;
        for (var wbp : wbpSources)
        {
            var barRange = wbp.getBarRange();
            float score = WbpSourceArray.computeCompatibilityScore(wbp, scs);
            if (score > 0 && barRange.size() > 1 && barRange.from >= songChordSequence.getBarRange().from && barRange.from <= songChordSequence.getBarRange().from + 1)
            {
                // Provide a little score boost for a "long" WbpSource at first bars: if several long WbpSources are top-scored including the first one, we prefer 
                // that the initial one is chosen to start song with a good-quality bass pattern.
                score += 10f;
            }
            if (score > maxScore)
            {
                res = wbp;
                maxScore = score;
            }
        }
        return res;
    }


    /**
     * Get the WbpSources which match the root profile of scSeq.
     * <p>
     *
     * @param scs
     * @return
     */
    private List<WbpSource> getRootProfileCompatibleWbpSources(SimpleChordSequence scs)
    {
        String rp = scs.getRootProfile();
        LOGGER.log(Level.FINE, "getRootProfileCompatibleWbpSources() -- scs={0} rp={1}", new Object[]
        {
            scs, rp
        });
        var res = WbpDatabase.getInstance().getWbpSources(rp);
        return res;
    }


    /**
     * Manage the case of RhythmVoiceDelegate.
     *
     * @param rv
     * @return
     */
    private int getChannelFromMidiMix(RhythmVoice rv)
    {
        RhythmVoice myRv = (rv instanceof RhythmVoiceDelegate) ? ((RhythmVoiceDelegate) rv).getSource() : rv;
        int destChannel = context.getMidiMix().getChannel(myRv);
        return destChannel;
    }

    /**
     * Guess tags from the SongContext.
     * <p>
     * Eg blues, bluesminor, slow, medium, fast, modal, ..., to influence the bass pattern selection.
     *
     * @param context
     * @return Can be an empty list.
     */
    private List<String> guessTags(SongContext context)
    {
        return Collections.emptyList();
    }


    // =====================================================================================================================
    // Inner classes
    // =====================================================================================================================
}
