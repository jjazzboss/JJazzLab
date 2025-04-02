package org.jjazz.proswing.walkingbass;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.phrase.api.Grid;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.proswing.RP_BassStyle;
import org.jjazz.proswing.BassStyle;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmmusicgeneration.api.AccentProcessor;
import org.jjazz.rhythmmusicgeneration.api.AnticipatedChordProcessor;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.FloatRange;

/**
 * Walking bass generator based on pre-recorded patterns from WbpDatabase.
 *
 * @see WbpSourceDatabase
 */
public class WalkingBassMusicGenerator implements MusicGenerator
{

    private final Rhythm rhythm;

    private static final Logger LOGGER = Logger.getLogger(WalkingBassMusicGenerator.class.getSimpleName());

    public WalkingBassMusicGenerator(Rhythm r)
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

        // Get one bass phrase per used BassStyle
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
     * @param sgContextOrig
     * @param tags
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private List<Phrase> getBassPhrases(SongContext sgContextOrig, List<String> tags) throws MusicGenerationException
    {
        LOGGER.fine("getVariationBassPhrases() --");

        List<Phrase> res = new ArrayList<>();


        // Prepare a working context because SongStructure might be modified by preprocessBassStyleAutoValue
        SongFactory sf = SongFactory.getInstance();
        Song songCopy = sf.getCopyUnlinked(sgContextOrig.getSong(), false);
        preprocessBassStyleAutoValue(songCopy);     // This will modify songCopy


        // The working context 
        SongContext contextWork = new SongContext(songCopy, sgContextOrig.getMidiMix(), sgContextOrig.getBarRange());


        // Build the main chord sequence and split the song structure in chord sequences of consecutive sections having our rhythm with the same bass style
        var scs = new SongChordSequence(songCopy, contextWork.getBarRange());   // Throws UserErrorGenerationException but no risk: will have a chord at beginning. Handle alternate chord symbols.            
        var splitResults = scs.split(rhythm, RP_BassStyle.get(rhythm));


        // Make one big SimpleChordSequenceExt per rpValue: this will let us control "which pattern is used where" at the song level
        var usedRpValues = splitResults.stream()
                .map(sr -> sr.rpValue())
                .collect(Collectors.toSet());
        for (var rpValue : usedRpValues)
        {
            SimpleChordSequenceExt mergedScsExt = null;

            for (var splitResult : splitResults.stream().filter(sr -> sr.rpValue().equals(rpValue)).toList())
            {
                // Merge to current SimpleChordSequence
                var scsExt = new SimpleChordSequenceExt(splitResult.simpleChordSequence(), true);
                mergedScsExt = mergedScsExt == null ? scsExt : mergedScsExt.getMerged(scsExt, true);
            }
            assert mergedScsExt != null : "splitResults=" + splitResults + " usedRpValues=" + usedRpValues;


            // We have our big SimpleChordSequenceExt (possibly with non usable bars), generate the walking bass for it
            mergedScsExt.removeRedundantChords();
            var phrase = getBassPhrase(contextWork, mergedScsExt, RP_BassStyle.toBassStyle(rpValue));
            res.add(phrase);
        }

        return res;
    }


    /**
     * Get the bass phrase for the usable bars of a SimpleChordSequenceExt.
     *
     * @param sgContext
     * @param scs
     * @param style
     * @return
     * @throws MusicGenerationException
     */
    private Phrase getBassPhrase(SongContext sgContext, SimpleChordSequenceExt scs, BassStyle style) throws MusicGenerationException
    {
        LOGGER.log(Level.SEVERE, "\n");
        LOGGER.log(Level.SEVERE, "getBassPhrase() -- sgContext.barRange={0}  style={1}  scs={2}", new Object[]
        {
            sgContext.getBarRange(), style, scs
        });


        // Do the work
        var tiling = style.getTilingFactory().build(scs, sgContext.getSong().getTempo());


        // Control
        debugCheck(tiling);
        if (!tiling.isFullyTiled())
        {
            LOGGER.severe("getBassPhrase() ERROR could not fully tile");
        }

        LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ===============   Tiling stats  scs.usableBars={0} \n{1}", new Object[]
        {
            scs.getUsableBars().size(),
            tiling.toStatsString()
        });


        // Get the resulting phrase
        var res = tiling.buildPhrase(new TransposerPhraseAdapter());


        // Handle accents and chords anticipation
        postProcessBassPhrase(res, sgContext, scs);


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

    /**
     * Process p for possible accents and chords anticipation.
     *
     * @param p
     * @param sgContext
     * @param scs
     *
     */
    private void postProcessBassPhrase(Phrase p, SongContext sgContext, SimpleChordSequenceExt scs)
    {
        int nbCellsPerBeat = Grid.getRecommendedNbCellsPerBeat(rhythm.getTimeSignature(), rhythm.getFeatures().division().isSwing());
        float cSeqStartInBeats = sgContext.getSong().getSongStructure().toPositionInNaturalBeats(scs.getBarRange().from);


        // Processors below require that first phrase note does not start before cSeqStartInBeats, but it can happen because of non-quantized WbpSources when
        // we process e.g. a song part in the middle of a song. So we need to cut such note.
        if (cSeqStartInBeats > 0)
        {
            Phrases.silence(p, new FloatRange(0, cSeqStartInBeats), true, true, 0);
        }


        AccentProcessor ap = new AccentProcessor(scs, cSeqStartInBeats, nbCellsPerBeat, sgContext.getSong().getTempo());
        AnticipatedChordProcessor acp = new AnticipatedChordProcessor(scs, cSeqStartInBeats, nbCellsPerBeat);
        acp.anticipateChords_Mono(p);
        ap.processAccentBass(p);
        ap.processHoldShotMono(p, AccentProcessor.HoldShotMode.NORMAL);

    }

    private void debugCheck(WbpTiling tiling)
    {
        LOGGER.log(Level.SEVERE, "\n\n debugCheck() =========");

        // Search for WbpSourceAdaptations with WbpSources using non-standard start/end note        
        for (var wbpsa : tiling.getWbpSourceAdaptations())
        {
            var wbpSource = wbpsa.getWbpSource();
            if (!wbpSource.isStartingOnChordRoot() && wbpsa.getCompatibilityScore().preTargetNoteMatch() > 0)
            {
                LOGGER.log(Level.SEVERE, "  non-root start-note: {0}", wbpsa);
            } else if (!wbpSource.isEndingOnChordTone() && wbpsa.getCompatibilityScore().postTargetNoteMatch() > 0)
            {
                LOGGER.log(Level.SEVERE, "  non-chord-tone end-note: {0}", wbpsa);
            }
        }
    }

    // =====================================================================================================================
    // Inner classes
    // =====================================================================================================================

}
