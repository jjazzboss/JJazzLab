package org.jjazz.test.walkingbass.generator;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.test.walkingbass.WbpDatabase;

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
        WbpDatabase.getInstance().checkConsistency();

        LOGGER.log(Level.SEVERE, "generateMusic() -- rhythm={0} contextChordSequence={1}", new Object[]
        {
            rhythm.getName(), songChordSequence
        });


        // Get the bass phrase for each used variation value
        var bassPhrases = getVariationBassPhrases();


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
     * Get the bass phrase for each variation value.
     * <p>
     *
     * @return @throws org.jjazz.rhythm.api.MusicGenerationException
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private List<Phrase> getVariationBassPhrases() throws MusicGenerationException
    {
        LOGGER.fine("getVariationBassPhrases() --");

        List<Phrase> res = new ArrayList<>();

        // Split the song structure in chord sequences of consecutive sections having the same rhythm and same RhythmParameter value
        var splitResults = songChordSequence.split(rhythm, RP_STD_Variation.getVariationRp(rhythm));


        // Make one big SimpleChordSequence per rpValue: this will let us control "which pattern is used where" at the song level
        var usedRpValues = splitResults.stream()
            .map(sr -> sr.rpValue())
            .collect(Collectors.toSet());
        for (var rpValue : usedRpValues)
        {
            SimpleChordSequenceExt mergedScs = null;

            for (var splitResult : splitResults.stream().filter(sr -> sr.rpValue().equals(rpValue)).toList())
            {
                // Merge to current SimpleChordSequence
                var scs = new SimpleChordSequenceExt(splitResult.simpleChordSequence(), true);
                mergedScs = mergedScs == null ? scs : mergedScs.getMerged(scs, true);
            }

            assert mergedScs != null : "splitResults=" + splitResults + " usedRpValues=" + usedRpValues;
            // We have our big SimpleChordSequenceExt, generate the walking bass for it
            mergedScs.removeRedundantChords();


            var phrase = getBassPhrase(mergedScs, rpValue);
            res.add(phrase);
        }


        return res;
    }

    /**
     * Get the bass phrase from a SimpleChordSequenceExt.
     *
     * @param scs
     * @param rpVariationValue
     * @return
     * @throws MusicGenerationException
     */
    private Phrase getBassPhrase(SimpleChordSequenceExt scs, String rpVariationValue) throws MusicGenerationException
    {
        LOGGER.log(Level.SEVERE, "getBassPhrase() -- scs={0} rpVariationValue={1}", new Object[]
        {
            scs, rpVariationValue
        });

        Phrase res = new Phrase(0);             // channel useless here

        var settings = WalkingBassGeneratorSettings.getInstance();
        WbpTiling tiling = new WbpTiling(scs);
        var tiler = new TilerOneOutOfX(settings.getSingleWbpSourceMaxSongCoveragePercentage(), settings.getOneOutofX());
        tiler.tile(tiling);
        if (!tiling.isCompletlyTiled())
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() =============== tiling is NOT complete !!\n");
            // return res;
        }


        LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling=\n{0}", tiling.toMultiLineString());

        LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ============   Tiling stats=\n{0}", tiling.toStatsString());

        // Transpose WbpSources to target phrases
        for (var wbpsa : tiling.getWbpSourceAdaptations())
        {
            var wbpSource = wbpsa.getWbpSource();
            var br = wbpsa.getBarRange();
            var subSeq = scs.subSequence(br, true);
            var firstRootNote = subSeq.first().getData().getRootNote();
            var p = wbpSource.getTransposedPhrase(firstRootNote);
            LOGGER.log(Level.FINE, "getBassPhrase() transposedPhrase={0}", p);
            p.shiftAllEvents(br.from * scs.getTimeSignature().getNbNaturalBeats());
            res.add(p, false);
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
