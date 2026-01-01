package org.jjazz.jjswing.drums;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.jjswing.api.DrumsStyle;
import org.jjazz.phrase.api.Grid;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.jjswing.api.RP_DrumsStyle;
import org.jjazz.jjswing.drums.db.DpSource;
import org.jjazz.jjswing.drums.db.DpSourceDatabase;
import org.jjazz.jjswing.bass.BassGenerator;
import org.jjazz.jjswing.bass.BassGeneratorSettings;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.jjswing.tempoadapter.SwingDrumsTempoAdapter;
import org.jjazz.jjswing.tempoadapter.SwingProfile;
import org.jjazz.rhythm.api.Rhythm;
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

public class DrumsGenerator implements MusicGenerator
{

    public final static Level DrumsGeneratorLogLevel = Level.FINE;
    private final Rhythm rhythm;
    private final RP_DrumsStyle rpDrumsStyle;
    private Song lastSong;

    private static final Logger LOGGER = Logger.getLogger(DrumsGenerator.class.getSimpleName());

    public DrumsGenerator(Rhythm r)
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
            LOGGER.severe("generateMusic() unexpected null values for rvDrums and rvPerc");
            throw new MusicGenerationException("Unexpected error in DrumsGenerator, check the log");
        }

        // Try to guess some tags (eg blues, slow, medium, fast, modal, ...) to influence the drums pattern selection
        List<String> tags = guessTags(context);


        LOGGER.log(DrumsGeneratorLogLevel, "\n");
        LOGGER.log(DrumsGeneratorLogLevel, "############################################");
        LOGGER.log(DrumsGeneratorLogLevel, "generateMusic() -- rhythm={0} tags={1}", new Object[]
        {
            rhythm.getName(), tags
        });

        HashMap<RhythmVoice, Phrase> res = new HashMap<>();

        Phrase pDrums = rvDrums != null ? new Phrase(9, true) : null;       // channel is normally unused
        Phrase pPerc = rvPerc != null ? new Phrase(8, true) : null;         // channel is normally unused
        fillPhrases(pDrums, pPerc, context, tags);

        if (rvDrums != null)
        {
            var drumKit = getDrumKit(rvDrums, context.getMidiMix());
            postProcessPhrase(context, pDrums, drumKit);
            res.put(rvDrums, pDrums);
        }
        if (rvPerc != null)
        {
            var drumKit = getDrumKit(rvDrums, context.getMidiMix());
            postProcessPhrase(context, pPerc, drumKit);
            res.put(rvPerc, pPerc);
        }

        lastSong = context.getSong();

        return res;
    }


    // ===============================================================================
    // Private methods
    // ===============================================================================
    /**
     * Add notes to pDrums and/or pPerc for the relevant SongParts.
     * <p>
     *
     * @param pDrums  Can be null except if pPerc is null
     * @param pPerc   Can be null except if pDrums is null
     * @param context
     * @param tags
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private void fillPhrases(Phrase pDrums, Phrase pPerc, SongContext context, List<String> tags) throws MusicGenerationException
    {
        var dpsDb = DpSourceDatabase.getInstance(TimeSignature.FOUR_FOUR);
        var nbBeatsPerBar = rhythm.getTimeSignature().getNbNaturalBeats();

        for (var spt : getRhythmSpts(context))
        {
            var drumsStyle = getDrumsStyle(spt);
            var dpss = dpsDb.getDpSourceSet(drumsStyle);
            int dpsSizeInBars = dpss.getSize();
            var sptBarRange = context.getSptBarRange(spt);
            var sptBeatRange = context.getSptBeatRange(spt);


            LOGGER.log(DrumsGeneratorLogLevel, "fillPhrases() spt={0} drumsStyle={1}", new Object[]
            {
                spt, drumsStyle
            });

            // Tile the SongPart with random phrases from the DpSourceSet
            for (int bar = sptBarRange.from; sptBarRange.contains(bar); bar += dpsSizeInBars)
            {
                int relBar = bar - sptBarRange.from;
                float beatPos = sptBeatRange.from + relBar * nbBeatsPerBar;
                FloatRange beatRangeDps = new FloatRange(beatPos, beatPos + dpsSizeInBars * nbBeatsPerBar);
                DpSource dpSourceFill = null;   // no fill by default
                FloatRange beatRangeFill = FloatRange.EMPTY_FLOAT_RANGE;  // no fill by default


                boolean isLastIteration = (bar + dpsSizeInBars - 1) >= sptBarRange.to;
                if (isLastIteration)
                {
                    beatRangeDps = sptBeatRange.getIntersectRange(beatRangeDps);
                    dpSourceFill = getFillDpSource(spt, dpss.dpSourcesFill());      // null if no fill
                }


                if (dpSourceFill != null)
                {
                    // We have a fill, shorten beatRangeDps to make room for the 1-bar fill
                    float fillBeatSize = dpSourceFill.getSizeInBars() * nbBeatsPerBar;
                    assert fillBeatSize <= beatRangeDps.size() :
                            "spt=" + spt + " bar=" + bar + " beatRangeStd=" + beatRangeDps + " fillBeatSize=" + fillBeatSize;
                    beatRangeFill = new FloatRange(beatRangeDps.to - fillBeatSize, beatRangeDps.to);
                    beatRangeDps = beatRangeDps.from == beatRangeFill.from ? FloatRange.EMPTY_FLOAT_RANGE
                            : new FloatRange(beatRangeDps.from, beatRangeFill.from);
                }


                DpSource dpSource = randomPick(dpss.dpSourcesStd());
                LOGGER.log(DrumsGeneratorLogLevel, "fillPhrases()      bar={0} beatRangeStd={1} dpSource={2} dpSourceFill={3}", new Object[]
                {
                    bar, beatRangeDps, dpSource, dpSourceFill
                });

                if (pDrums != null)
                {
                    var p = dpSource.getDrumsPhrase(beatRangeDps);
                    pDrums.add(p);
                    if (dpSourceFill != null)
                    {
                        p = dpSourceFill.getDrumsPhrase(beatRangeFill);
                        pDrums.add(p);
                    }
                }

                if (pPerc != null)
                {
                    var p = dpSource.getPercPhrase(beatRangeDps);
                    pPerc.add(p);
                    if (dpSourceFill != null)
                    {
                        p = dpSourceFill.getPercPhrase(beatRangeFill);
                        pPerc.add(p);
                    }
                }

            }
        }
    }

    private DrumKit getDrumKit(RhythmVoice rv, MidiMix midiMix)
    {
        DrumKit drumKit;
        if (rv.isDrums())
        {
            drumKit = rv.getDrumKit();
            var insMix = midiMix.getInstrumentMix(rv);
            if (insMix != null && insMix.getInstrument().getDrumKit() != null)
            {
                drumKit = insMix.getInstrument().getDrumKit();
            }
        } else
        {
            drumKit = new DrumKit();
            LOGGER.log(Level.WARNING, "getDrumKit() Unexpected non drums rv={0}, using default GM DrumKit", rv);
        }
        return drumKit;
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
        return !dpSources.isEmpty() && isFillRequired(spt) ? randomPick(dpSources) : null;
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

    /**
     * Post process the phrase by SongPart, since SongParts using our rhythm can be any anywhere in the song.
     *
     * @param context
     * @param p
     * @param drumKit
     * @throws UserErrorGenerationException
     */
    private void postProcessPhrase(SongContext context, Phrase p, DrumKit drumKit) throws UserErrorGenerationException
    {
        var song = context.getSong();
        SongChordSequence songChordSequence = new SongChordSequence(song, context.getBarRange());  // throws UserErrorGenerationException. Handle alternate chord symbols

        for (var spt : getRhythmSpts(context))
        {
            var sptBeatRange = context.getSptBeatRange(spt);
            var sptBarRange = context.getSptBarRange(spt);

            var scsSpt = new SimpleChordSequence(songChordSequence.subSequence(sptBarRange, false), sptBeatRange.from, rhythm.getTimeSignature());

            // Accents
            processDrumsAccents(p, scsSpt, song.getTempo(), drumKit);

            // process RP_SYS_Intensity
            BassGenerator.processIntensity(p, sptBeatRange, spt.getRPValue(RP_SYS_Intensity.getIntensityRp(rhythm)));

            // Tempo-based adjustment
            processSwingFeelTempoAdapter(p, sptBeatRange, song.getTempo(), drumKit);
        }
    }

    private void processSwingFeelTempoAdapter(Phrase p, FloatRange beatRange, int tempo, DrumKit drumKit)
    {
        float intensity = BassGeneratorSettings.getInstance().getSwingProfileIntensity();
        LOGGER.log(Level.FINE, "processSwingFeelTempoAdapter() beatRange={0} intensity={1}", new Object[]
        {
            beatRange, intensity
        });
        SwingProfile profile = SwingProfile.createForDrums(intensity);
        SwingDrumsTempoAdapter drumsAdapter = new SwingDrumsTempoAdapter(profile, rhythm.getTimeSignature(), drumKit.getKeyMap());
        drumsAdapter.adaptToTempo(p, beatRange, ne -> true, tempo);
    }

    private List<SongPart> getRhythmSpts(SongContext context)
    {
        return context.getSongParts().stream()
                .filter(spt -> spt.getRhythm() == rhythm)
                .toList();
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
     * @param kit
     */
    private void processDrumsAccents(Phrase p, SimpleChordSequence scsSpt, int tempo, DrumKit kit)
    {
        Objects.requireNonNull(p);
        Objects.requireNonNull(scsSpt);
        Objects.requireNonNull(kit);
        int nbCellsPerBeat = Grid.getRecommendedNbCellsPerBeat(rhythm.getTimeSignature(), rhythm.getFeatures().division().isSwing());
        AccentProcessor ap = new AccentProcessor(scsSpt, nbCellsPerBeat, tempo, BassGenerator.NON_QUANTIZED_WINDOW);

        ap.processAccentDrums(p, kit);
        ap.processHoldShotDrums(p, kit, AccentProcessor.HoldShotMode.NORMAL);
    }


    private void clearCacheData()
    {
    }

    // =====================================================================================================================
    // Inner classes
    // =====================================================================================================================

}
