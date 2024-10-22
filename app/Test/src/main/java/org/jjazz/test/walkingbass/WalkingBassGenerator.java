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

/**
 * Walking bass generator based on pre-recorded patterns from WbpDatabase.
 *
 * @see WbpDatabase
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


        // System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "[%1$s p=%2$.1f d=%3$.1f]");
        System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "%1$s");
        // WbpDatabase.getInstance().dump();
        LOGGER.log(Level.SEVERE, "generateMusic() -- rhythm={0} contextChordSequence={1}", new Object[]
        {
            rhythm.getName(), songChordSequence
        });


        // Get all the bass phrases that make the song
        var bassPhrases = getAllBassPhrases();


        // Merge the bass phrases for each rv
        HashMap<RhythmVoice, Phrase> res = new HashMap<>();
        for (var rv : bassRvs)
        {
            int channel = getChannelFromMidiMix(rv);
            Phrase pRes = new Phrase(channel, false);
            for (var p : bassPhrases)
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
    private List<Phrase> getAllBassPhrases() throws MusicGenerationException
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
            var phrase = getBassPhrase(cSeq, rpValue);
            res.add(phrase);
        }

        return res;
    }

    /**
     * Get the bass phrase from the specified SimpleChordSequence.
     *
     * @param scs
     * @param rpVariationValue
     * @return
     * @throws MusicGenerationException
     */
    private Phrase getBassPhrase(SimpleChordSequence scs, String rpVariationValue) throws MusicGenerationException
    {
        LOGGER.log(Level.SEVERE, "getPhrase() -- scs={0} rpVariationValue={1}", new Object[]
        {
            scs, rpVariationValue
        });

        Phrase res = new Phrase(0);             // channel useless here


        // Tile scs with WbpSources
        WbpSourceTiling wbpSrcTiling = buildWbpSrcTiling(scs);
        if (!wbpSrcTiling.isCompletlyTiled())
        {
            LOGGER.log(Level.SEVERE, "getPhrase() tiling is NOT complete, aborted: {0}", wbpSrcTiling.getNonTiledBars());
            return res;
        }

        // Transpose WbpSources to target phrases
        for (var it = wbpSrcTiling.iterator(); it.hasNext();)
        {
            var wbpSource = it.next();
            var bar = it.getStartBar();
            var br = wbpSource.getBarRange();
            var subSeq = scs.subSequence(br.getTransformed(bar), true);
            var firstRootNote = subSeq.first().getData().getRootNote();
            var p = wbpSource.getTransposedPhrase(firstRootNote);
            LOGGER.log(Level.SEVERE, "            => transposedPhrase={0}", p);
            p.shiftAllEvents(bar * scs.getTimeSignature().getNbNaturalBeats());
            res.add(p, false);
        }
        return res;
    }

    /**
     * Tile a WbpSourceTiling object for scs.
     *
     * @param scs
     * @return
     */
    private WbpSourceTiling buildWbpSrcTiling(SimpleChordSequence scs)
    {
        final int MAX_NB_BEST_ADAPTATIONS = 4;
        WbpSourceTiling res = new WbpSourceTiling(scs);

        // For each bar, get the most compatible 4-bar WbpSource
        var bestWpsas4 = new BestWbpSourceAdaptations(scs, 4, scs.getBarRange().stream().boxed().toList(), MAX_NB_BEST_ADAPTATIONS);
        updateBestWbpSourcesForEachUsableBar(bestWpsas4, 4);
        res.tileMostCompatibleFirst(bestWpsas4);
        LOGGER.log(Level.SEVERE, "getPhrase() ----- 4-bar\n{0}", res.toString());


        // For each remaining 2-bar zone, get the most compatible 2-bar WbpSource
        var usableBars2 = res.getUntiledZonesStartBarIndexes(2);
        if (!usableBars2.isEmpty())
        {
            var bestWpsas2 = new BestWbpSourceAdaptations(scs, 2, usableBars2, MAX_NB_BEST_ADAPTATIONS);
            updateBestWbpSourcesForEachUsableBar(bestWpsas2, 2);
            res.tileMostCompatibleFirst(bestWpsas2);
            LOGGER.log(Level.SEVERE, "getPhrase() ----- 2-bar \n{0}", res.toString());
        }


        // For each remaining 1-bar zone, get the most compatible 1-bar WbpSource
        var usableBars1 = res.getUntiledZonesStartBarIndexes(1);
        if (!usableBars1.isEmpty())
        {
            var bestWpsas1 = new BestWbpSourceAdaptations(scs, 1, usableBars1, MAX_NB_BEST_ADAPTATIONS);
            updateBestWbpSourcesForEachUsableBar(bestWpsas1, 1);
            res.tileMostCompatibleFirst(bestWpsas1);
            LOGGER.log(Level.SEVERE, "getPhrase() ----- 1-bar \n{0}", res.toString());
        }

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
