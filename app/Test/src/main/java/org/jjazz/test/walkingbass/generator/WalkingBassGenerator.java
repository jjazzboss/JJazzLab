package org.jjazz.test.walkingbass.generator;

import org.jjazz.test.walkingbass.tiler.TilerMaxDistance;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.test.walkingbass.WbpSource;
import org.jjazz.test.walkingbass.WbpSourceDatabase;
import org.jjazz.test.walkingbass.WbpSources;
import org.jjazz.test.walkingbass.tiler.TilerBestFirstNoRepeat;
import org.jjazz.utilities.api.IntRange;

/**
 * Walking bass generator based on pre-recorded patterns from WbpDatabase.
 *
 * @see WbpSourceDatabase
 */
public class WalkingBassGenerator implements MusicGenerator
{

    private static int SESSION_COUNT = 0;
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
        Preconditions.checkArgument(RP_SYS_Variation.getVariationRp(r) != null
                && RP_SYS_Intensity.getIntensityRp(r) != null,
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
        WbpSourceDatabase.getInstance().checkConsistency();

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
        var splitResults = songChordSequence.split(rhythm, RP_SYS_Variation.getVariationRp(rhythm));


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
        int robustness = 20;

        while (!tiling.isFullyTiled() && --robustness > 0)
        {

            // var tiler = new TilerOneOutOfX(settings.getSingleWbpSourceMaxSongCoveragePercentage(), settings.getOneOutofX(), settings.getWbpsaStoreWidth());
            var tiler0 = new TilerBestFirstNoRepeat(settings.getWbpsaStoreWidth());
            tiler0.tile(tiling);
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling BestFirstNoRepeat =\n{0}", tiling.toMultiLineString());


            var tiler1 = new TilerMaxDistance(settings.getWbpsaStoreWidth());
            tiler1.tile(tiling);
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling MaxDistance =\n{0}", tiling.toMultiLineString());


            handleMultiChordNonTiledBars(tiling);
        }


        LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ===============   Tiling stats  scs.usableBars={0} \n{1}", new Object[]
        {
            scs.getUsableBars().size(),
            tiling.toStatsString()
        });


        // Transpose WbpSources to target phrases
        for (var wbpsa : tiling.getWbpSourceAdaptations())
        {
            var p = new TransposerPhraseAdapter().getPhrase(wbpsa);
            LOGGER.log(Level.FINE, "getBassPhrase() transposedPhrase={0}", p);
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
     * @return Can be an empty list. TODO implement !
     */
    private List<String> guessTags(SongContext context)
    {
        return Collections.emptyList();
    }

    /**
     * Compute WbpSources for untiled bars and add them to the database.
     *
     * @param tiling
     */
    private void handleMultiChordNonTiledBars(WbpTiling tiling)
    {

        for (int size = WbpSourceDatabase.SIZE_MAX; size >= WbpSourceDatabase.SIZE_MIN; size--)
        {
            var startBars = tiling.getUntiledZonesStartBarIndexes(size);
            for (int startBar : startBars)
            {
                var br = new IntRange(startBar, startBar + size - 1);
                var subSeq = tiling.getSimpleChordSequenceExt().subSequence(br, true).getShifted(-startBar);
                if (size == 1 && subSeq.size() == 1)
                {
                    LOGGER.log(Level.WARNING, "handleMultiChordNonTiledBars() 1-chord bar not previously tiled: {0}", subSeq);
                    continue;
                }

                List<WbpSource> wbpSources = generateMultiChordPerBarWbpSources(subSeq);
                var wbpDb = WbpSourceDatabase.getInstance();
                for (var wbps : wbpSources)
                {
                    if (!wbpDb.addWbpSource(wbps))
                    {
                        LOGGER.log(Level.WARNING, "handleMultiChordNonTiledBars() subSeq={0} ADD FAIL: {1} ", new Object[]
                        {
                            subSeq, wbps
                        });
                    }
                }
            }
        }

    }


    /**
     *
     * @param subSeq Must start at bar 0
     * @return
     */
    private List<WbpSource> generateMultiChordPerBarWbpSources(SimpleChordSequence subSeq)
    {
        Preconditions.checkArgument(subSeq.getBarRange().from == 0, "subSeq=%s", subSeq);

        List<WbpSource> res = new ArrayList<>();
        if (subSeq.isTwoChordsPerBar(0.25f, false))
        {
            SizedPhrase sp = create2ChordsPerBarPhrase(subSeq);
            String id = "Gen2Chords-" + (SESSION_COUNT++);
            WbpSource wbpSource = new WbpSource(id, 0, subSeq, sp, 0, null);
            WbpSource wbpGrooveRef = findGrooveReference(sp);
            if (wbpGrooveRef != null)
            {
                LOGGER.log(Level.SEVERE, "generateMultiChordPerBarWbpSources() applying groove from {0}", wbpGrooveRef);
                Phrases.applyGroove(wbpGrooveRef.getSizedPhrase(), sp, 0.15f);
            }

            res.add(wbpSource);

        } else
        {
            var p = DummyGenerator.getBasicBassPhrase(0, subSeq, 0);
            SizedPhrase sp = new SizedPhrase(0, subSeq.getBeatRange(0), subSeq.getTimeSignature(), false);
            sp.add(p);
            String id = "GenDefault-" + (SESSION_COUNT++);
            WbpSource wbpSource = new WbpSource(id, 0, subSeq, sp, 0, null);
            res.add(wbpSource);
        }

        return res;
    }

    private SizedPhrase create2ChordsPerBarPhrase(SimpleChordSequence subSeq)
    {
        SizedPhrase sp = new SizedPhrase(0, subSeq.getBeatRange(0), subSeq.getTimeSignature(), false);
        for (var cliCs : subSeq)
        {
            var ecs = cliCs.getData();
            int cBase = 3 * 12;
            int bassPitch0 = cBase + ecs.getRootNote().getRelativePitch();
            int bassPitch1 = cBase + ecs.getRelativePitch(ChordType.DegreeIndex.THIRD_OR_FOURTH);
            float pos0 = subSeq.toPositionInBeats(cliCs.getPosition(), 0);
            float pos1 = pos0 + 1f;
            NoteEvent ne0 = new NoteEvent(bassPitch0, 1f, 80, pos0);
            NoteEvent ne1 = new NoteEvent(bassPitch1, 1f, 80, pos1);
            sp.add(ne0);
            sp.add(ne1);
        }
        return sp;
    }

    private boolean isGeneratedWbpSource(WbpSource wbpSource)
    {
        return wbpSource.getId().startsWith("Gen");
    }

    /**
     * Find a random WbpSource which can serve as groove reference for sp.
     *
     * @param sp
     * @return
     */
    private WbpSource findGrooveReference(SizedPhrase sp)
    {
        WbpSource res = null;
        
        var wbpSources = WbpSourceDatabase.getInstance().getWbpSources(sp.getSizeInBars(), 
                w ->  !isGeneratedWbpSource(w) && Phrases.isSamePositions(w.getSizedPhrase(), sp, 0.15f));
        
        if (wbpSources.size() == 1)
        {
            res = wbpSources.get(0);
        } else if (wbpSources.size() > 1)
        {
            int index = (int) (Math.random() * wbpSources.size());
            res = wbpSources.get(index);
        }

        return res;
    }

    // =====================================================================================================================
    // Inner classes
    // =====================================================================================================================

}
