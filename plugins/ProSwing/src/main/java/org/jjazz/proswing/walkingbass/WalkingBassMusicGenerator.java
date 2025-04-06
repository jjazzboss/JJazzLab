package org.jjazz.proswing.walkingbass;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.phrase.api.Grid;
import org.jjazz.phrase.api.NoteEvent;
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
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.FloatRange;

/**
 * Walking bass generator based on pre-recorded patterns from WbpDatabase.
 *
 * @see WbpSourceDatabase
 */
public class WalkingBassMusicGenerator implements MusicGenerator
{

    /**
     * Beat position (+/-) tolerance to accomodate for unquantized notes.
     */
    public static final float NON_QUANTIZED_WINDOW = Grid.PRE_CELL_BEAT_WINDOW_DEFAULT;
    /**
     * Used to identify ghost notes that might be ignored in some cases.
     */
    public static final float GHOST_NOTE_MAX_DURATION = NON_QUANTIZED_WINDOW;

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
        var bassPhrases = getOneBassPhrasePerBassStyle(context, tags);


        // Merge the bass phrases
        HashMap<RhythmVoice, Phrase> res = new HashMap<>();
        int channel = getChannelFromMidiMix(context.getMidiMix(), rvBass);
        Phrase pRes = new Phrase(channel, false);
        for (var p : bassPhrases)
        {
            pRes.add(p);
        }


        // Some overlaps might happen when combining 2 WbpSources, when last note of 1st WbpSource is long and same pitch than the 1st note of 2nd WbpSource 
        // AND 2nd WbpSource has a firstNoteBeatShift < 0
        Phrases.fixOverlappedNotes(pRes);


        // Post process the phrase by SongPart, since SongParts using our rhythm can be any anywhere in the song     
        var song = context.getSong();
        SongChordSequence songChordSequence = new SongChordSequence(song, context.getBarRange());  // throws UserErrorGenerationException. Handle alternate chord symbols.        
        var rpIntensity = RP_SYS_Intensity.getIntensityRp(rhythm);
        List<SongPart> rhythmSpts = context.getSongParts().stream()
                .filter(spt -> spt.getRhythm() == rhythm)
                .toList();
        SongPart prevSpt = null;


        for (var spt : rhythmSpts)
        {
            var sptBeatRange = context.getSptBeatRange(spt);
            var sptBarRange = context.getSptBarRange(spt);

            if (sptBeatRange.from > 0 && (prevSpt == null || prevSpt.getStartBarIndex() + prevSpt.getNbBars() != spt.getStartBarIndex()))
            {
                // Previous spt is for another rhythm, make sure our first note does not start a bit before spt start because of non quantization
                enforceCleanStart(pRes, sptBeatRange.from);
            }


            // Accents and chord anticipations
            var scsSpt = new SimpleChordSequence(songChordSequence.subSequence(sptBarRange, false), rhythm.getTimeSignature());
            processAccentAndChordAnticipation(pRes, sptBeatRange.from, scsSpt, song.getTempo());


            // RP_SYS_Intensity
            processIntensity(pRes, sptBeatRange, spt.getRPValue(rpIntensity));


            prevSpt = spt;
        }

        res.put(rvBass, pRes);


        return res;
    }

    /**
     * Remove ghost notes from the phrase.
     *
     * @param p
     */
    static public void removeGhostNotes(Phrase p)
    {
        p.removeIf(ne -> ne.getDurationInBeats() <= WalkingBassMusicGenerator.GHOST_NOTE_MAX_DURATION);
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================
    /**
     * For each bass style, provide a single phrase which cover all the song parts using this bass style.
     * <p>
     * A phrase might contain parts with no notes if some song parts use a different rhythm or a different bass style.
     *
     * @param sgContextOrig
     * @param tags
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private List<Phrase> getOneBassPhrasePerBassStyle(SongContext sgContextOrig, List<String> tags) throws MusicGenerationException
    {
        LOGGER.fine("getVariationBassPhrases() --");

        List<Phrase> res = new ArrayList<>();


        // Prepare a working context because SongStructure might be modified by preprocessBassStyleAutoValue
        SongFactory sf = SongFactory.getInstance();
        Song songCopy = sf.getCopyUnlinked(sgContextOrig.getSong(), false);
        preprocessBassStyleAutoValue(songCopy);     // This will modify songCopy


        // The working context with auto bass style processed
        SongContext contextWork = new SongContext(songCopy, sgContextOrig.getMidiMix(), sgContextOrig.getBarRange());


        // Build the main chord sequence and split the song structure in chord sequences of consecutive sections having our rhythm with the same bass style
        var scs = new SongChordSequence(songCopy, contextWork.getBarRange());   // Throws UserErrorGenerationException but no risk: will have a chord at beginning. Handle alternate chord symbols.            
        var rpBassStyleSplitResults = scs.split(rhythm, getRP_BassStyle());


        // We want one big SimpleChordSequenceExt per rpValue: this will let us control "which pattern is used where" at the song level thus avoiding repetitions
        var usedBassStyleRpValues = rpBassStyleSplitResults.stream()
                .map(sr -> sr.rpValue())
                .collect(Collectors.toSet());
        for (var bassStyleRpValue : usedBassStyleRpValues)
        {
            SimpleChordSequenceExt mergedScsExt = null;

            for (var splitResult : rpBassStyleSplitResults.stream().filter(sr -> sr.rpValue().equals(bassStyleRpValue)).toList())
            {
                // Merge to current SimpleChordSequence
                var scsExt = new SimpleChordSequenceExt(splitResult.simpleChordSequence(), true);
                mergedScsExt = mergedScsExt == null ? scsExt : mergedScsExt.getMerged(scsExt, true);
            }
            assert mergedScsExt != null : "splitResults=" + rpBassStyleSplitResults + " usedRpValues=" + usedBassStyleRpValues;


            // We have our big SimpleChordSequenceExt, possibly with non usable bars corresponding to song parts which do not use our rhythm, or which use our 
            // rhythm but not with bassStyleRpValue
            mergedScsExt.removeRedundantChords();
            var phrase = getBassPhrase(contextWork, mergedScsExt, bassStyleRpValue);      // Generate the bass phrase
            res.add(phrase);
        }

        return res;
    }


    /**
     * Get the bass phrase for the usable bars of a SimpleChordSequenceExt.
     *
     * @param sgContext
     * @param scsExt
     * @param bassStyleRpValue The style of the generated phrase
     * @return
     * @throws MusicGenerationException
     */
    private Phrase getBassPhrase(SongContext sgContext, SimpleChordSequenceExt scsExt, String bassStyleRpValue) throws MusicGenerationException
    {
        LOGGER.log(Level.SEVERE, "\n");
        LOGGER.log(Level.SEVERE, "getBassPhrase() -- sgContext.barRange={0}  bassStyleRpValue={1}  scsExt={2}", new Object[]
        {
            sgContext.getBarRange(), bassStyleRpValue, scsExt
        });

        BassStyle bassStyle = RP_BassStyle.toBassStyle(bassStyleRpValue);

        // Do the work
        var tiling = bassStyle.getTilingFactory().build(scsExt, sgContext.getSong().getTempo());


        // Control
        debugCheck(tiling);
        if (!tiling.isFullyTiled())
        {
            LOGGER.severe("getBassPhrase() ERROR could not fully tile");
        }

        LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ===============   Tiling stats  scsExt.usableBars={0} \n{1}", new Object[]
        {
            scsExt.getUsableBars().size(),
            tiling.toStatsString()
        });


        // Get the resulting phrase
        var p = tiling.buildPhrase(new TransposerPhraseAdapter());


        return p;
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

    private RP_BassStyle getRP_BassStyle()
    {
        return RP_BassStyle.get(rhythm);
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
     * Update p for possible accents and chords anticipation found in scs.
     *
     * @param p
     * @param scsStartBeatPos
     * @param scs
     * @param tempo
     */
    private void processAccentAndChordAnticipation(Phrase p, float scsStartBeatPos, SimpleChordSequence scs, int tempo)
    {
        int nbCellsPerBeat = Grid.getRecommendedNbCellsPerBeat(rhythm.getTimeSignature(), rhythm.getFeatures().division().isSwing());
        AccentProcessor ap = new AccentProcessor(scs, scsStartBeatPos, nbCellsPerBeat, tempo, NON_QUANTIZED_WINDOW);
        AnticipatedChordProcessor acp = new AnticipatedChordProcessor(scs, scsStartBeatPos, nbCellsPerBeat, NON_QUANTIZED_WINDOW);

        acp.anticipateChords_Mono(p);
        ap.processAccentBass(p);
        ap.processHoldShotMono(p, AccentProcessor.HoldShotMode.NORMAL);
    }

    /**
     * Update notes from p depending on intensity.
     *
     * @param p
     * @param beatRange Update notes in this range
     * @param intensity [-10;10]
     */
    private void processIntensity(Phrase p, FloatRange beatRange, int intensity)
    {
        int velShift = RP_SYS_Intensity.getRecommendedVelocityShift(intensity);
        if (velShift != 0)
        {
            p.processNotes(ne -> beatRange.contains(ne.getPositionInBeats(), true), ne -> 
            {
                int v = MidiConst.clamp(ne.getVelocity() + velShift);
                NoteEvent newNe = ne.setVelocity(v);
                return newNe;
            });
        }
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
    /**
     * Make sure no note start a bit before sptBeatPos because of non quantization.
     *
     * @param p
     * @param beatPos
     */
    private void enforceCleanStart(Phrase p, float beatPos)
    {
        var nes = Phrases.getCrossingNotes(p, beatPos, true);
        for (var ne : nes)
        {
            var dur = ne.getDurationInBeats() - (beatPos - ne.getPositionInBeats());
            var newNe = ne.setAll(-1, dur, -1, beatPos, false);
            p.replace(ne, newNe);
        }
    }
}
