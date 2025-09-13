package org.jjazz.jjswing.drums.db;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.jjswing.api.DrumsStyle;
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
import org.jjazz.jjswing.api.RP_DrumsStyle;
import org.jjazz.jjswing.drums.db.DpSource.Type;
import org.jjazz.jjswing.walkingbass.JJSwingBassMusicGenerator;
import org.jjazz.jjswing.walkingbass.JJSwingBassMusicGeneratorSettings;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmmusicgeneration.api.AccentProcessor;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.Utilities;

/**
 * Drums generator based on pre-recorded patterns from DpSoruceDatabase.
 *
 * @see DpSourceDatabase
 */

public class JJSwingDrumsMusicGenerator implements MusicGenerator
{


    private final Rhythm rhythm;
    private final RP_DrumsStyle rpDrumsStyle;
    private Song lastSong;

    private static final Logger LOGGER = Logger.getLogger(JJSwingDrumsMusicGenerator.class.getSimpleName());

    public JJSwingDrumsMusicGenerator(Rhythm r)
    {
        Preconditions.checkArgument(RP_SYS_Variation.getVariationRp(r) != null
                && RP_SYS_Intensity.getIntensityRp(r) != null
                && RP_DrumsStyle.get(r) != null,
                "r=%s", r);
        rhythm = r;
        lastSong = null;
        rpDrumsStyle = RP_DrumsStyle.get(r);
    }

    /**
     * Process only drums and percussion tracks.
     *
     * @param context
     * @param rvs     If specified must be a drums or percussion RhythmVoice
     * @return
     * @throws MusicGenerationException
     */
    @Override
    public HashMap<RhythmVoice, Phrase> generateMusic(SongContext context, RhythmVoice... rvs) throws MusicGenerationException
    {
        Objects.requireNonNull(context);
        Preconditions.checkArgument(rvs.length == 0
                || Stream.of(rvs).allMatch(rv -> rv.getType() == RhythmVoice.Type.DRUMS || rv.getType() == RhythmVoice.Type.PERCUSSION),
                "context=%s, rvs=%s", context, rvs);


        if (context.getSong() != lastSong)
        {
            clearCacheData();
        }


        var rvsList = rvs.length == 0 ? rhythm.getRhythmVoices() : List.of(rvs);
        var rvDrums = rvsList.stream()
                .filter(rv -> rv.getType() == RhythmVoice.Type.DRUMS)
                .findAny()
                .orElse(null);
        var rvPerc = rvsList.stream()
                .filter(rv -> rv.getType() == RhythmVoice.Type.PERCUSSION)
                .findAny()
                .orElse(null);
        if (rvDrums == null && rvPerc == null)
        {
            throw new MusicGenerationException("JJSwingDrumsMusicGenerator.generateMusic() unexpected null values for rvDrums and rvPerc");
        }

        // Try to guess some tags (eg blues, slow, medium, fast, modal, ...) to influence the drums pattern selection
        List<String> tags = guessTags(context);


        LOGGER.log(Level.SEVERE, "\n\n\n\n");
        LOGGER.log(Level.SEVERE, "############################################");
        LOGGER.log(Level.SEVERE, "generateMusic() -- rhythm={0} tags={1}", new Object[]
        {
            rhythm.getName(), tags
        });


        HashMap<RhythmVoice, Phrase> res = new HashMap<>();


        int channelDrums = rvDrums == null ? 0 : getChannelFromMidiMix(context.getMidiMix(), rvDrums);
        int channelPerc = rvPerc == null ? 0 : getChannelFromMidiMix(context.getMidiMix(), rvPerc);
        Phrase pDrums = new Phrase(channelDrums, true);
        Phrase pPerc = new Phrase(channelPerc, true);
        fillPhrases(pDrums, pPerc, context, tags);


        postProcessPhrase(pDrums, context);
        postProcessPhrase(pPerc, context);


        if (rvDrums != null)
        {
            res.put(rvDrums, pDrums);
        }
        if (rvPerc != null)
        {
            res.put(rvPerc, pPerc);
        }

        lastSong = context.getSong();

        return res;
    }


    // ===============================================================================
    // Private methods
    // ===============================================================================
    /**
     * Add notes to pDrums and pPerc for the relevant SongParts.
     * <p>
     *
     * @param pDrums
     * @param pPerc
     * @param context
     * @param tags
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private void fillPhrases(Phrase pDrums, Phrase pPerc, SongContext context, List<String> tags) throws MusicGenerationException
    {
        LOGGER.fine("fillPhrases() --");
        var dpsDb = DpSourceDatabase.getInstance(TimeSignature.FOUR_FOUR);
        var nbBeatsPerBar = rhythm.getTimeSignature().getNbNaturalBeats();

        for (var spt : getRhythmSpts(context))
        {
            var drumsStyle = getDrumsStyle(spt);

            LOGGER.log(Level.SEVERE, "fillPhrases() spt={0} drumsStyle={1}", new Object[]
            {
                spt, drumsStyle
            });

            var dpss = dpsDb.getDpSourceSet(drumsStyle);
            int dpsSize = dpss.getSize();
            var sptBarRange = spt.getBarRange();
            var sptBeatRange = context.getSptBeatRange(spt);
            float beatPos = sptBeatRange.from;


            // Tile the SongPart with random phrases from the DpSourceSet
            for (int bar = sptBarRange.from; sptBarRange.contains(bar); bar += dpsSize)
            {
                FloatRange beatRange = new FloatRange(beatPos, beatPos + dpsSize * nbBeatsPerBar);
                FloatRange beatRangeStd = beatRange;
                DpSource dpSourceFill = null;   // no fill by default
                FloatRange beatRangeFill = FloatRange.EMPTY_FLOAT_RANGE;  // no fill by default


                boolean isLastIteration = (bar + dpsSize - 1) >= sptBarRange.to;
                if (isLastIteration)
                {
                    beatRange = sptBeatRange.getIntersectRange(beatRange);
                    dpSourceFill = getFillDpSource(spt, dpss.dpSourcesFill());      // null if no fill
                }


                if (dpSourceFill != null)
                {
                    float fillBeatSize = dpSourceFill.getSizeInBars() * nbBeatsPerBar;
                    assert fillBeatSize <= beatRange.size() : "spt=" + spt + " bar=" + bar + " beatRange=" + beatRange + " fillBeatSize=" + fillBeatSize;
                    beatRangeFill = new FloatRange(beatRange.to - fillBeatSize, beatRange.to);
                    beatRangeStd = beatRange.from == beatRangeFill.from ? FloatRange.EMPTY_FLOAT_RANGE : new FloatRange(beatRange.from, beatRangeFill.from);
                }


                DpSource dpSource = randomPick(dpss.dpSourcesStd());
                Phrase spDrums = dpSource.getDrumsPhrase(beatRangeStd);
                Phrase spPerc = dpSource.getPercPhrase(beatRangeStd);


                if (dpSourceFill != null)
                {
                    var spDrumsFill = dpSourceFill.getDrumsPhrase(beatRangeFill);
                    var p = new Phrase(0);
                    p.add(spDrums);
                    p.add(spDrumsFill);
                    spDrums = p;

                    var spPercFill = dpSourceFill.getPercPhrase(beatRangeFill);
                    p = new Phrase(0);
                    p.add(spPerc);
                    p.add(spPercFill);
                    spPerc = p;
                }


                pDrums.add(spDrums);
                pPerc.add(spPerc);


                beatPos += dpsSize * rhythm.getTimeSignature().getNbNaturalBeats();
            }
        }

    }

    private SizedPhrase getShortenedPhrase(SizedPhrase sp, FloatRange newBeatRange)
    {
        SizedPhrase newSpDrums = new SizedPhrase(sp.getChannel(), newBeatRange, sp.getTimeSignature(), true);
        sp.stream()
                .filter(ne -> newSpDrums.canAddNote(ne))
                .forEach(ne -> newSpDrums.add(ne));
        return newSpDrums;
    }

    /**
     * Get the RP_DrumsStyle value (process RP_DrumsStyle.AUTO_MODE_VALUE if required).
     *
     * @param spt
     * @return
     */
    private DrumsStyle getDrumsStyle(SongPart spt)
    {
        var rpDrumsValueStr = spt.getRPValue(rpDrumsStyle);
        var rpVariationValue = spt.getRPValue(RP_SYS_Variation.getVariationRp(rhythm));
        return RP_DrumsStyle.toDrumsStyle(rpDrumsValueStr, rpVariationValue);
    }


    /**
     *
     * @param spt
     * @param dpSources
     * @return Null if no need for a fill
     */
    private DpSource getFillDpSource(SongPart spt, List<DpSource> dpSources)
    {
        return isFillRequired(spt) ? randomPick(dpSources) : null;
    }

    private boolean isFillRequired(SongPart spt)
    {
        RP_SYS_Fill rpFill = RP_SYS_Fill.getFillRp(rhythm);
        if (rpFill == null)
        {
            return false;
        }
        String strValue = spt.getRPValue(rpFill);
        return RP_SYS_Fill.needFill(strValue);
    }

    private DpSource randomPick(List<DpSource> dpss)
    {
        int size = dpss.size();
        assert size > 0;
        if (size == 1)
        {
            return dpss.get(0);
        }
        int index = Utilities.getIntRandom(0, size - 1);
        return dpss.get(index);
    }

    private void postProcessPhrase(Phrase p, SongContext context) throws UserErrorGenerationException
    {

        // Add slight velocity randomization +/- 2 
        p.processVelocity(v -> (int) Math.round(v + Math.random() * 4 - 2));


        // Post process the phrase by SongPart, since SongParts using our rhythm can be any anywhere in the song     
        var song = context.getSong();
        SongChordSequence songChordSequence = new SongChordSequence(song, context.getBarRange());  // throws UserErrorGenerationException. Handle alternate chord symbols.        
        var rpIntensity = RP_SYS_Intensity.getIntensityRp(rhythm);
        List<SongPart> rhythmSpts = getRhythmSpts(context);
        SongPart prevSpt = null;


        for (var spt : rhythmSpts)
        {
            var sptBeatRange = context.getSptBeatRange(spt);
            var sptBarRange = context.getSptBarRange(spt);
            var scsSpt = new SimpleChordSequence(songChordSequence.subSequence(sptBarRange, false), sptBeatRange.from, rhythm.getTimeSignature());


            if (sptBeatRange.from > 0 && (prevSpt == null || prevSpt.getStartBarIndex() + prevSpt.getNbBars() != spt.getStartBarIndex()))
            {
                // Previous spt is for another rhythm, make sure our first note does not start a bit before spt start because of non quantization
                enforceCleanStart(p, sptBeatRange.from);
            }


            // Accents 
            processAccents(p, scsSpt, song.getTempo());


            // process RP_SYS_Intensity
            processIntensity(p, sptBeatRange, spt.getRPValue(rpIntensity));


            // Position shift depending on setting and tempo
            float bias = computeNotePositionBias(song.getTempo());
            // LOGGER.severe("  BIAS="+bias+" (tempo="+song.getTempo()+")");
            processNotePositionBias(p, sptBeatRange, bias);


            prevSpt = spt;
        }
    }

    private List<SongPart> getRhythmSpts(SongContext context)
    {
        return context.getSongParts().stream()
                .filter(spt -> spt.getRhythm() == rhythm)
                .toList();
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
     * Eg blues, bluesminor, slow, medium, fast, modal, ..., to influence the drums pattern selection.
     *
     * @param context
     * @return Can be an empty list. TODO implement !
     */
    private List<String> guessTags(SongContext context)
    {
        return Collections.emptyList();
    }


    /**
     * Update p for possible accents in a SongPart.
     *
     * @param p
     * @param scsSpt The song chord sequence of the SongPart
     * @param tempo
     */
    private void processAccents(Phrase p, SimpleChordSequence scsSpt, int tempo)
    {
        int nbCellsPerBeat = Grid.getRecommendedNbCellsPerBeat(rhythm.getTimeSignature(), rhythm.getFeatures().division().isSwing());
        AccentProcessor ap = new AccentProcessor(scsSpt, nbCellsPerBeat, tempo, JJSwingBassMusicGenerator.NON_QUANTIZED_WINDOW);

        LOGGER.severe("processAccents() TO BE IMPLEMENTED");
        //ap.processAccentDrums(p, kit);
        //ap.processHoldShotDrums(p, kit, AccentProcessor.HoldShotMode.NORMAL);
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

    /**
     * Update note timing depending on bias and tempo.
     * <p>
     *
     * @param p
     * @param beatRange    Update notes in this range
     * @param positionBias
     */
    private void processNotePositionBias(Phrase p, FloatRange beatRange, float positionBias)
    {
        if (positionBias != 0)
        {
            p.processNotes(ne -> beatRange.contains(ne.getPositionInBeats(), true), ne -> 
            {
                float newPos = Math.max(ne.getPositionInBeats() + positionBias, beatRange.from);
                NoteEvent newNe = ne.setPosition(newPos, false);
                return newNe;
            });
        }
    }

    /**
     * The faster the tempo the more we play before the beat.
     * <p>
     * Also depends on the setting getTempoNotePositionBiasFactor().
     *
     * @param tempo
     * @return
     */
    private float computeNotePositionBias(int tempo)
    {
        final int TEMPO_HIGH = 240;
        final int TEMPO_NORMAL = TEMPO_HIGH / 2;
        float TEMPO_HIGH_BIAS = -0.04f;
        float TEMPO_NORMAL_BIAS = 0;
        float tempo2 = Math.clamp(tempo, TEMPO_NORMAL, TEMPO_HIGH);
        float biasTempo = TEMPO_NORMAL_BIAS + (tempo2 - TEMPO_NORMAL) / (TEMPO_HIGH - TEMPO_NORMAL) * (TEMPO_HIGH_BIAS - TEMPO_NORMAL_BIAS);

        float BIAS_RANGE_MAX = 0.07f;
        float biasSettingFactor = JJSwingBassMusicGeneratorSettings.getInstance().getTempoNotePositionBiasFactor();
        float biasSetting = biasSettingFactor * BIAS_RANGE_MAX;
        float res = Math.clamp(biasSetting + biasTempo, -BIAS_RANGE_MAX, BIAS_RANGE_MAX);
        return res;
    }


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


    private void clearCacheData()
    {
    }

    // =====================================================================================================================
    // Inner classes
    // =====================================================================================================================

}
