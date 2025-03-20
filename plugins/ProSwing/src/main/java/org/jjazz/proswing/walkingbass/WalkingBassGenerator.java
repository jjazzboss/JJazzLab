package org.jjazz.proswing.walkingbass;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.proswing.RP_BassStyle;
import org.jjazz.proswing.BassStyle;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmmusicgeneration.api.DummyGenerator;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.proswing.walkingbass.WbpSource;
import org.jjazz.proswing.walkingbass.WbpSourceDatabase;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongStructure;
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

    /**
     * The Chord Sequence with all the chords.
     */
    private SongChordSequence songChordSequence;
    private static final Logger LOGGER = Logger.getLogger(WalkingBassGenerator.class.getSimpleName());

    public WalkingBassGenerator(Rhythm r)
    {
        Preconditions.checkArgument(RP_SYS_Variation.getVariationRp(r) != null
                && RP_SYS_Intensity.getIntensityRp(r) != null,
                "r=%s", r);
        rhythm = r;
    }

    /**
     * Process only one bass track.
     *
     * @param context
     * @param rvs     0 or 1 value. If specified must be a bass RhythmVoice
     * @return
     * @throws MusicGenerationException
     */
    @Override
    public HashMap<RhythmVoice, Phrase> generateMusic(SongContext context, RhythmVoice... rvs) throws MusicGenerationException
    {
        Objects.requireNonNull(context);
        Preconditions.checkArgument(rvs.length == 0 || (rvs.length == 1 && rvs[0].getType() == RhythmVoice.Type.BASS), "context=%s, rvs=%s", context, rvs);

        RhythmVoice rvBass;
        if (rvs.length == 1)
        {
            rvBass = rvs[0];
        } else
        {
            rvBass = rhythm.getRhythmVoices().stream()
                    .filter(rv -> rv.getType() == RhythmVoice.Type.BASS)
                    .findAny()
                    .orElseThrow();
        }

        // Try to guess some tags (eg blues, slow, medium, fast, modal, ...) to influence the bass pattern selection
        List<String> tags = guessTags(context);


        LOGGER.log(Level.SEVERE, "\n\n\n\n############################################ \ngenerateMusic() -- rhythm={0} tags={1}", new Object[]
        {
            rhythm.getName(), tags
        });

        // Get the bass phrase for each used variation value
        var bassPhrases = getBassPhrases(context, tags);


        // Merge the bass phrases 
        HashMap<RhythmVoice, Phrase> res = new HashMap<>();
        int channel = getChannelFromMidiMix(context.getMidiMix(), rvBass);
        Phrase pRes = new Phrase(channel, false);
        for (var p : bassPhrases)
        {
            pRes.add(p);
        }
        res.put(rvBass, pRes);


        return res;
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================
    /**
     * Get the bass phrases for each consecutive sections using our rhythm and having the same bass style.
     * <p>
     * @param contextOrig
     * @param tags
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private List<Phrase> getBassPhrases(SongContext contextOrig, List<String> tags) throws MusicGenerationException
    {
        LOGGER.fine("getVariationBassPhrases() --");

        List<Phrase> res = new ArrayList<>();


        // Prepare a working context because SongStructure might be modified by preprocessBassStyleAutoValue
        SongFactory sf = SongFactory.getInstance();
        Song songCopy = sf.getCopyUnlinked(contextOrig.getSong(), false);
        preprocessBassStyleAutoValue(songCopy);     // This will modify songCopy

        // The working context 
        SongContext contextWork = new SongContext(songCopy, contextOrig.getMidiMix(), contextOrig.getBarRange());

        // Build the main chord sequence
        songChordSequence = new SongChordSequence(songCopy, contextWork.getBarRange());   // Throw UserErrorGenerationException but no risk: will have a chord at beginning. Handle alternate chord symbols.       

        // Split the song structure in chord sequences of consecutive sections having the same rhythm and same RhythmParameter value
        var splitResults = songChordSequence.split(rhythm, RP_BassStyle.get(rhythm));


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
            var phrase = getBassPhrase(mergedScs, RP_BassStyle.toBassStyle(rpValue), contextOrig.getSong().getTempo());
            res.add(phrase);
        }

        return res;
    }


    /**
     * Get the bass phrase from a SimpleChordSequenceExt.
     *
     * @param scs
     * @param style
     * @param tempo
     * @return
     * @throws MusicGenerationException
     */
    private Phrase getBassPhrase(SimpleChordSequenceExt scs, BassStyle style, int tempo) throws MusicGenerationException
    {
        LOGGER.log(Level.SEVERE, "getBassPhrase() -- style={0} tempo={1} scs={2}", new Object[]
        {
            style, tempo, scs
        });

        Phrase res = new Phrase(0);             // channel useless here

        var settings = WalkingBassGeneratorSettings.getInstance();
        WbpTiling tiling = new WbpTiling(scs);
        var phraseAdapter = new TransposerPhraseAdapter();


        // PREMIUM PHASE
        WbpsaScorer scorerPremium = new WbpsaScorerDefault(phraseAdapter, tempo, Score.PREMIUM_ONLY_TESTER, style);
        
        LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling PREMIUM LongestFirstNoRepeat");
        var tilerLongestPremium = new TilerLongestFirstNoRepeat(scorerPremium, settings.getWbpsaStoreWidth());
        tilerLongestPremium.tile(tiling);
        LOGGER.log(Level.SEVERE, tiling.toMultiLineString());

        
        var untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling PREMIUM MaxDistance");            
            var tilerMaxDistancePremium = new TilerMaxDistance(scorerPremium, settings.getWbpsaStoreWidth());
            tilerMaxDistancePremium.tile(tiling);
            LOGGER.log(Level.SEVERE, tiling.toMultiLineString());
        }


        // STANDARD PHASE
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling STANDARD LongestFirstNoRepeat");            
            WbpsaScorer scorerStandard = new WbpsaScorerDefault(phraseAdapter, tempo, null, style);

            var tilerLongestStandard = new TilerLongestFirstNoRepeat(scorerStandard, settings.getWbpsaStoreWidth());
            tilerLongestStandard.tile(tiling);
            LOGGER.log(Level.SEVERE, tiling.toMultiLineString());

            untiled = !tiling.isFullyTiled();
            if (untiled)
            {
                LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling STANDARD MaxDistance");                
                var tilerMaxDistanceStandard = new TilerMaxDistance(scorerStandard, settings.getWbpsaStoreWidth());
                tilerMaxDistanceStandard.tile(tiling);
                LOGGER.log(Level.SEVERE, tiling.toMultiLineString());
            }
        }


        // If still untiled, try using previously computed CUSTOM source phrases
        WbpsaScorer scorerCustom = new WbpsaScorerDefault(phraseAdapter, tempo, null, BassStyle.CUSTOM);
        var tilerMaxDistanceCustomStandard = new TilerMaxDistance(scorerCustom, settings.getWbpsaStoreWidth());
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling EXISTING CUSTOM MaxDistance");            
            tilerMaxDistanceCustomStandard.tile(tiling);
            LOGGER.log(Level.SEVERE, tiling.toMultiLineString());
        }


        // If still untiled, add new CUSTOM source phrases then retile -that should be enough
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling CREATED CUSTOM MaxDistance");            
            untiled = addCustomWbpSources(tiling);
            tilerMaxDistanceCustomStandard.tile(tiling);
            LOGGER.log(Level.SEVERE, tiling.toMultiLineString());
        }


        if (untiled)
        {
            LOGGER.severe("getBassPhrase() ERROR could not fully tile");
        }


        LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ===============   Tiling stats  scs.usableBars={0} \n{1}", new Object[]
        {
            scs.getUsableBars().size(),
            tiling.toStatsString()
        });

        // Transpose WbpSources to target phrases
        for (var wbpsa : tiling.getWbpSourceAdaptations())
        {
            var p = phraseAdapter.getPhrase(wbpsa);
            LOGGER.log(Level.FINE, "getBassPhrase() transposedPhrase={0}", p);
            res.add(p, false);
        }
        return res;
    }

    /**
     * Manage the case of RhythmVoiceDelegate.
     *
     * @param mm
     * @param rv
     * @return
     */
    private int getChannelFromMidiMix(MidiMix mm, RhythmVoice rv)
    {
        RhythmVoice myRv = (rv instanceof RhythmVoiceDelegate) ? ((RhythmVoiceDelegate) rv).getSource() : rv;
        int destChannel = mm.getChannel(myRv);
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
     * For each untiled bar create and add BassStyle.CUSTOM WbpSource(s) to WbpSourceDatabase.
     * <p>
     * Untiled zones with only 1 chord are NOT handled by this method (it means that the default WbpSourceDatabase should be updated).
     *
     * @param tiling
     * @return True if there still are some untiled bars.
     */
    private boolean addCustomWbpSources(WbpTiling tiling)
    {
        boolean b = false;

        for (int size = WbpSourceDatabase.SIZE_MAX; size >= WbpSourceDatabase.SIZE_MIN; size--)
        {
            var startBars = tiling.getUntiledZonesStartBarIndexes(size);
            for (int startBar : startBars)
            {
                b = true;
                var br = new IntRange(startBar, startBar + size - 1);
                var subSeq = tiling.getSimpleChordSequenceExt().subSequence(br, true).getShifted(-startBar);
                if (subSeq.size() == 1)
                {
                    // Not normal, some additional default WbpSources should be added in the database
                    LOGGER.log(Level.SEVERE, "handleNonTiledBars() 1-chord subSeq not previously tiled: {0}", subSeq);
                    continue;
                }

                // subSeq contains at least 2 chords
                List<WbpSource> wbpSources = createCustomWbpSources(subSeq);

                // Add 
                var wbpDb = WbpSourceDatabase.getInstance();
                for (var wbps : wbpSources)
                {
                    if (!wbpDb.addWbpSource(wbps))
                    {
                        LOGGER.log(Level.WARNING, "handleNonTiledBars() subSeq={0} ADD FAIL: {1} ", new Object[]
                        {
                            subSeq, wbps
                        });
                    }
                }
            }
        }

        return b;
    }

    /**
     * Create one or more default WbpSources with BassType=CUSTOM for subSeq.
     *
     * @param subSeq Must start at bar 0. Contains 2 or more chords.
     * @return
     */
    private List<WbpSource> createCustomWbpSources(SimpleChordSequence subSeq)
    {
        Preconditions.checkArgument(subSeq.getBarRange().from == 0 && subSeq.size() > 1, "subSeq=%s", subSeq);

        LOGGER.log(Level.SEVERE, "createCustomWbpSources() - subSeq={0}", subSeq);

        List<WbpSource> res = new ArrayList<>();
        if (subSeq.isTwoChordsPerBar(0.25f, false))
        {
            LOGGER.log(Level.SEVERE, "createCustomWbpSources() => 2-chord-per-bar phrase");
            SizedPhrase sp = create2ChordsPerBarPhrase(subSeq);
            String id = "Gen2Chords-" + (SESSION_COUNT++);
            WbpSource wbpSource = new WbpSource(id, 0, BassStyle.CUSTOM, subSeq, sp, 0, null);
            WbpSource wbpGrooveRef = findGrooveReference(sp);
            if (wbpGrooveRef != null)
            {
                Phrases.applyGroove(wbpGrooveRef.getSizedPhrase(), sp, 0.15f);
            } else
            {
                LOGGER.log(Level.WARNING, "createCustomWbpSources() no groove reference found for phrase {0}", sp);
            }

            res.add(wbpSource);

        } else
        {
            LOGGER.log(Level.SEVERE, "createCustomWbpSources() => random phrase");
            var p = DummyGenerator.getBasicBassPhrase(0, subSeq, new IntRange(50, 65), 0);
            SizedPhrase sp = new SizedPhrase(0, subSeq.getBeatRange(0), subSeq.getTimeSignature(), false);
            sp.add(p);
            String id = "GenDefault-" + (SESSION_COUNT++);
            WbpSource wbpSource = new WbpSource(id, 0, BassStyle.CUSTOM, subSeq, sp, 0, null);
            res.add(wbpSource);
        }

        return res;
    }

    /**
     * Create a bass phrase for a 2-chord per bar chord sequence.
     *
     * @param subSeq
     * @return
     */
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

        var wbpSources = WbpSourceDatabase.getInstance().getWbpSources(sp.getSizeInBars()).stream()
                .filter(w -> !isGeneratedWbpSource(w) && Phrases.isSamePositions(w.getSizedPhrase(), sp, 0.15f))
                .toList();

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

    /**
     * Update SongParts with RP_BassStyle value="auto" to the appropriate value (depends on the RP_SYS_Variation value).
     *
     * @param song
     */
    private void preprocessBassStyleAutoValue(Song song)
    {
        SongStructure ss = song.getSongStructure();

        for (var spt : ss.getSongParts())
        {
            var r = spt.getRhythm();
            var rpBassStyle = RP_BassStyle.get(r);
            if (rpBassStyle == null)
            {
                continue;
            }
            var rpValue = spt.getRPValue(rpBassStyle);
            if (rpValue.equals(RP_BassStyle.AUTO_MODE_VALUE))
            {
                var rpVariation = RP_SYS_Variation.getVariationRp(r);
                var rpVariationValue = spt.getRPValue(rpVariation);
                var rpBassValue = RP_BassStyle.getAutoModeRpValueFromVariation(rpVariationValue);
                ss.setRhythmParameterValue(spt, rpBassStyle, rpBassValue);
            }
        }
    }


    // =====================================================================================================================
    // Inner classes
    // =====================================================================================================================
}
