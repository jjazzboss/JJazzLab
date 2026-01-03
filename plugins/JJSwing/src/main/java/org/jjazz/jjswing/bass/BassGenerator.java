package org.jjazz.jjswing.bass;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.phrase.api.Grid;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.jjswing.api.RP_BassStyle;
import org.jjazz.jjswing.bass.db.Velocities;
import org.jjazz.jjswing.bass.db.WbpSource;
import org.jjazz.jjswing.bass.db.WbpSourceDatabase;
import org.jjazz.jjswing.tempoadapter.SwingBassTempoAdapter;
import org.jjazz.jjswing.tempoadapter.SwingProfile;
import org.jjazz.rhythm.api.Division;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmmusicgeneration.api.AccentProcessor;
import org.jjazz.rhythmmusicgeneration.api.AnticipatedChordProcessor;
import org.jjazz.rhythmmusicgeneration.api.ChordSequence;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * Walking bass generator based on pre-recorded patterns from WbpDatabase.
 *
 * @see WbpSourceDatabase
 */

public class BassGenerator implements MusicGenerator
{

    /**
     * Beat position (+/-) tolerance to accomodate for unquantized notes.
     */
    public static final float NON_QUANTIZED_WINDOW = Grid.PRE_CELL_BEAT_WINDOW_DEFAULT;
    /**
     * Used to identify ghost notes that might be ignored in some cases.
     */
    public static final float GHOST_NOTE_MAX_DURATION = NON_QUANTIZED_WINDOW;

    /**
     * The value removed from duration when generating beat-to-beat notes.
     * <p>
     * Do not use 0.1f because 0.25f-0.1f=0.15f, a duration which might be considered as a ghost note and removed (0.25f=smallest chord symbol duration)
     */
    public static final float DURATION_BEAT_MARGIN = 0.09f;


    private final Rhythm rhythm;
    private Song lastSong;

    private static final Logger LOGGER = Logger.getLogger(BassGenerator.class.getSimpleName());

    public BassGenerator(Rhythm r)
    {
        Preconditions.checkArgument(RP_SYS_Variation.getVariationRp(r) != null
                && RP_SYS_Intensity.getIntensityRp(r) != null,
                "r=%s", r);
        rhythm = r;
        lastSong = null;
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

        if (context.getSong() != lastSong)
        {
            clearCacheData();
        }

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


        LOGGER.log(Level.FINE, "\n\n\n\n");
        LOGGER.log(Level.FINE, "############################################");
        LOGGER.log(Level.FINE, "generateMusic() -- rhythm={0} tags={1}", new Object[]
        {
            rhythm.getName(), tags
        });


        // Get one bass phrase per used BassStyle, then merge them into pRes
        var bassPhrases = getOneBassPhrasePerBassStyle(context, tags);
        HashMap<RhythmVoice, Phrase> res = new HashMap<>();
        int channel = getChannelFromMidiMix(context.getMidiMix(), rvBass);
        Phrase pRes = new Phrase(channel, false);
        for (var p : bassPhrases)
        {
            pRes.add(p);
        }

        postProcessSongParts(context, pRes);
        postProcessGlobal(context, pRes);       // Throws UserErrorGenerationException
        enforceSongPartsBounds(context, pRes);

        // Some overlaps might happen when :
        // - combining 2 WbpSources, if last note of 1st WbpSource is long and has the same pitch than 1st note of 2nd WbpSource, and 2nd WbpSource has firstNoteBeatShift < 0
        // - when converting a normal bass line to a pedal bass line, some same pitch notes overlaps may appear.
        Phrases.fixOverlappedNotes(pRes);

        res.put(rvBass, pRes);

        lastSong = context.getSong();

        return res;
    }

    /**
     * Post processing which depend on SongPart parameters.
     *
     * @param context
     * @param pRes
     */
    private void postProcessSongParts(SongContext context, Phrase pRes)
    {
        var rpIntensity = RP_SYS_Intensity.getIntensityRp(rhythm);
        var rpFill = RP_SYS_Fill.getFillRp(rhythm);

        var rhythmSpts = getRhythmSpts(context);
        for (var spt : rhythmSpts)
        {
            var sptBeatRange = context.getSptBeatRange(spt);

            processIntensity(pRes, sptBeatRange, spt.getRPValue(rpIntensity));

            processFill(pRes, sptBeatRange, spt.getRPValue(rpFill));
        }
    }

    /**
     * Post processing which does not depend on SongPart parameters.
     *
     * @param context
     * @param pRes
     * @throws org.jjazz.rhythm.api.UserErrorGenerationException
     */
    private void postProcessGlobal(SongContext context, Phrase pRes) throws UserErrorGenerationException
    {
        var song = context.getSong();
        var songChordSequence = new SongChordSequence(song, context.getBarRange());  // throws UserErrorGenerationException. Handle alternate chord symbols.                               

        // Merged SimpleChordSequences with our rhythm
        var scsList = songChordSequence.buildSimpleChordSequences(spt -> spt.getRhythm() == rhythm);
        for (var scs : scsList)
        {
            // Adapt notes for pedal bass and slash chords
            processPedalBassAndSlashChords(pRes, scs, song.getTempo());

            // Accents and chord anticipations
            processAccentAndChordAnticipation(pRes, scs, song.getTempo());

            // Add tempo-based adjustment
            processSwingFeelTempoAdapter(pRes, scs.getBeatRange(), song.getTempo());
        }

    }


    private List<SongPart> getRhythmSpts(SongContext context)
    {
        return context.getSongParts().stream()
                .filter(spt -> spt.getRhythm() == rhythm)
                .toList();
    }

    static public int getClosestAndAcceptableBassPitch(Note n, int relPitch)
    {
        int res = n.getClosestPitch(relPitch);
        if (res < WbpSource.BASS_GOOD_PITCH_RANGE.from)
        {
            res += 12;
        } else if (res > WbpSource.BASS_GOOD_PITCH_RANGE.to)
        {
            res -= 12;
        }
        return res;
    }

    /**
     * Update notes from p depending on intensity.
     *
     * @param p
     * @param beatRange Update notes in this range
     * @param intensity [-10;10]
     */
    static public void processIntensity(Phrase p, FloatRange beatRange, int intensity)
    {
        int velShift = RP_SYS_Intensity.getRecommendedVelocityShift(intensity);
        if (velShift != 0)
        {
            p.processNotes(ne -> beatRange.contains(ne.getPositionInBeats(), true), ne -> 
            {
                int v = MidiConst.clamp(ne.getVelocity() + velShift);
                NoteEvent newNe = ne.setVelocity(v, false);
                return newNe;
            });
        }
    }

//    /**
//     * The faster the tempo the more we play before the beat.
//     * <p>
//     * Also depends on the setting getTempoNotePositionBiasFactor().
//     *
//     * @param tempo
//     * @return A flopat value to shift notes beat positions
//     */
//    static public float computeNotePositionBias(int tempo)
//    {
//        final int TEMPO_HIGH = 240;
//        final float TEMPO_HIGH_BIAS = -0.04f;
//        final int TEMPO_NORMAL = TEMPO_HIGH / 2;
//        final float TEMPO_NORMAL_BIAS = 0;
//
//
//        float tempo2 = Math.clamp(tempo, TEMPO_NORMAL, TEMPO_HIGH);
//        float biasTempo = TEMPO_NORMAL_BIAS + (tempo2 - TEMPO_NORMAL) / (TEMPO_HIGH - TEMPO_NORMAL) * (TEMPO_HIGH_BIAS - TEMPO_NORMAL_BIAS);
//
//        final float BIAS_RANGE_MAX = 0.07f;
//        float biasSettingFactor = BassGeneratorSettings.getInstance().getTempoNotePositionBiasFactor();
//        float biasSetting = biasSettingFactor * BIAS_RANGE_MAX;
//        float res = Math.clamp(biasSetting + biasTempo, -BIAS_RANGE_MAX, BIAS_RANGE_MAX);
//        return res;
//    }

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


        // Prepare a working context because we'll use a modified song copy 
        SongFactory sf = SongFactory.getInstance();
        Song songWork = sf.getCopy(sgContextOrig.getSong(), true, false);
        SongContext contextWork = new SongContext(songWork, sgContextOrig.getMidiMix(), sgContextOrig.getBarRange());
        preprocessBassStyleAutoValue(contextWork);     // Update SongStructure to replace auto BassStyle values by standard BassStyle values


        // Prepare a SongChordSequence where we removed some anticipated chords to facilitate tiling of WbpSources
        var scsWork = prepareWorkSongChordSequence(contextWork);
        LOGGER.log(Level.FINE, "getOneBassPhrasePerBassStyle() scsWork={0}", scsWork);


        // Get the used bass styles
        var rpBassStyle = RP_BassStyle.get(rhythm);
        var usedBassStyles = scsWork.getSongParts().stream()
                .filter(spt -> spt.getRhythm() == rhythm)
                .map(spt -> RP_BassStyle.toBassStyle(spt.getRPValue(rpBassStyle)))
                .collect(Collectors.toSet());


        // Process each used bass style
        for (var style : usedBassStyles)
        {
            // Prepare the list of SimpleChordSequence
            var barRanges = contextWork.getMergedBarRanges(rhythm, rpBassStyle, RP_BassStyle.toRpValue(style));
            if (barRanges.isEmpty())
            {
                continue;
            }
            var scsList = barRanges.stream()
                    .map(barRange -> 
                    {
                        float startBeatPos = contextWork.getSong().getSongStructure().toPositionInNaturalBeats(barRange.from);
                        var scs = new SimpleChordSequence((ChordSequence) scsWork.subSequence(barRange, true), startBeatPos, rhythm.getTimeSignature());
                        scs.removeRedundantStandardChords();
                        return scs;
                    })
                    .toList();


            if (!scsList.isEmpty())
            {
                // Build the bass phrase for that style
                var phrase = style.getBassPhraseBuilder().build(scsList, songWork.getTempo());

                res.add(phrase);
            }

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
     * Update SongParts with RP_BassStyle value="auto" to the appropriate value (depends on the RP_SYS_Variation value).
     *
     * @param context
     */
    private void preprocessBassStyleAutoValue(SongContext context)
    {
        for (var spt : context.getSongParts())
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
                context.getSong().getSongStructure().setRhythmParameterValue(spt, rpBassStyle, rpBassValue);
            }
        }
    }

    /**
     * Create a SongChordSequence with some anticipated chords moved to facilitate standard WbpSources tiling.
     * <p>
     * Anticipated chords are moved when :<br>
     * 1/ it allows to use standard 1 or 2-chord-per-bar WbpSources<br>
     * 2/ the chord anticipation can be later processed by an AnticipatedChordProcessor.
     *
     * @param context
     * @return
     * @throws org.jjazz.rhythm.api.UserErrorGenerationException
     */
    private SongChordSequence prepareWorkSongChordSequence(SongContext context) throws UserErrorGenerationException
    {
        var ts = rhythm.getTimeSignature();
        boolean isTernary = rhythm.getFeatures().division() == Division.EIGHTH_SHUFFLE || rhythm.getFeatures().division() == Division.EIGHTH_TRIPLET;
        float halfBeat = ts.getHalfBarBeat(isTernary);
        float nbBeats = ts.getNbNaturalBeats();


        // The merged bar ranges which use our rhythm
        List<IntRange> rhythmBarRanges = context.getSongParts().stream()
                .filter(spt -> spt.getRhythm() == rhythm)
                .map(spt -> context.getSptBarRange(spt))
                .toList();
        rhythmBarRanges = IntRange.merge(rhythmBarRanges);


        SongChordSequence res = new SongChordSequence(context.getSong(), context.getBarRange());    // throws UserErrorGenerationException        
        res.removeRedundantStandardChords();


        SongStructure ss = context.getSong().getSongStructure();
        for (var rhythmBarRange : rhythmBarRanges)
        {
            // Retrieve the default anticipatable chords
            float startBeatPos = ss.toPositionInNaturalBeats(rhythmBarRange.from);
            SimpleChordSequence scs = new SimpleChordSequence(res.subSequence(rhythmBarRange, true), startBeatPos, ts);
            var acp = new AnticipatedChordProcessor(scs, isTernary ? 3 : 4, Grid.PRE_CELL_BEAT_WINDOW_DEFAULT);
            var anticipatableChords = acp.getAnticipatableChords();


            // Process bar by bar
            for (int bar : rhythmBarRange)
            {
                var scsBar = scs.subSequence(new IntRange(bar, bar), true);     // contains an initial chord at beat 0     

                switch (scsBar.size())          // first chord is on beat 0
                {
                    case 2 ->
                    {
                        // OK if 2nd chord is an anticipatable chord for half bar or next bar
                        var cliCs = scsBar.last();
                        var pos = cliCs.getPosition();
                        float beat = pos.getBeat();
                        if (anticipatableChords.contains(cliCs) && (Math.floor(beat) == halfBeat - 1 || Math.floor(beat) == nbBeats - 1))
                        {
                            moveChordSymbol(res, cliCs, pos.getNext(ts));
                        }
                    }
                    case 3 ->
                    {
                        // OK if 2nd and 3rd chords are for half bar and next bar anticipation
                        if (!scsBar.isMatchingInBarBeatPositions(false, new FloatRange(0, 0.01f),
                                new FloatRange(halfBeat - 0.8f, halfBeat + 0.01f),
                                new FloatRange(nbBeats - 0.8f, nbBeats)))
                        {
                            continue;
                        }
                        var cliCsLast = scsBar.last();
                        var posLast = cliCsLast.getPosition();
                        var cliCsMiddle = scsBar.lower(cliCsLast);
                        var posMiddle = cliCsMiddle.getPosition();
                        if (posMiddle.getBeat() == halfBeat && anticipatableChords.contains(cliCsLast))
                        {
                            moveChordSymbol(res, cliCsLast, posLast.getNext(ts));
                        } else if (anticipatableChords.contains(cliCsMiddle) && anticipatableChords.contains(cliCsLast))
                        {
                            moveChordSymbol(res, cliCsMiddle, posMiddle.getNext(ts));
                            moveChordSymbol(res, cliCsLast, posLast.getNext(ts));
                        }
                    }
                    default ->
                    {
                        // Nothing
                    }
                }
            }
        }

        return res;
    }

    /**
     * Update p depending on Fill parameter value
     *
     * @param p
     * @param sptBeatRange
     * @param rpFillValue  Fill parameter value
     */
    private void processFill(Phrase p, FloatRange sptBeatRange, String rpFillValue)
    {
        switch (rpFillValue)
        {
            case RP_SYS_Fill.VALUE_BREAK ->
            {
                processFillBreak(p, sptBeatRange);
            }
            default ->
            {
                // nothing
            }
        }
    }

    /**
     * Process a break value for the Fill parameter.
     *
     * @param p
     * @param sptBeatRange
     */
    private void processFillBreak(Phrase p, FloatRange sptBeatRange)
    {
        // Remove all notes of the last bar, except the start note, and make it short
        float lastBarStart = sptBeatRange.to - rhythm.getTimeSignature().getNbNaturalBeats();
        var brLastBar = new FloatRange(Math.max(0, lastBarStart - 0.4f), sptBeatRange.to - 0.4f);    // Take into accound possible anticipated/pushed notes
        var notes = p.getNotes(ne -> true, brLastBar, false);
        if (!notes.isEmpty())
        {
            p.removeAll(notes);
            NoteEvent ne0 = notes.getFirst();
            var ne0br = ne0.getBeatRange();
            if (ne0br.from < (lastBarStart + 0.2f))
            {
                float noteEndMax = lastBarStart + 0.8f;
                if (ne0br.to >= noteEndMax)
                {
                    float newDur = noteEndMax - ne0br.from;
                    ne0 = ne0.setDuration(newDur, true);
                }
                p.add(ne0);
            }
        }
    }

    /**
     * Update p for possible accents and chords anticipation in a SongPart.
     *
     * @param p
     * @param scsSpt The song chord sequence of the SongPart
     * @param tempo
     */
    private void processAccentAndChordAnticipation(Phrase p, SimpleChordSequence scsSpt, int tempo)
    {
        int nbCellsPerBeat = Grid.getRecommendedNbCellsPerBeat(rhythm.getTimeSignature(), rhythm.getFeatures().division().isSwing());
        AccentProcessor ap = new AccentProcessor(scsSpt, nbCellsPerBeat, tempo, NON_QUANTIZED_WINDOW);
        AnticipatedChordProcessor acp = new AnticipatedChordProcessor(scsSpt, nbCellsPerBeat, NON_QUANTIZED_WINDOW);

        acp.anticipateChords_Mono(p);
        ap.processAccentBass(p);
        ap.processHoldShotMono(p, AccentProcessor.HoldShotMode.NORMAL);
    }


    /**
     * Update notes for pedal bass and slash chords in a SongPart.
     *
     * @param p
     * @param scsSpt The simple chord sequence corresponding to a SongPart
     * @param tempo
     */
    private void processPedalBassAndSlashChords(Phrase p, SimpleChordSequence scsSpt, int tempo)
    {
        var ts = rhythm.getTimeSignature();
        var nbBeatsPerBar = ts.getNbNaturalBeats();


        var cliCs = scsSpt.first();
        while (cliCs != null)
        {
            var pos = cliCs.getPosition();
            var ecs = cliCs.getData();
            boolean isSlash = ecs.isSlashChord();
            boolean isPedalBass = ecs.getRenderingInfo().getFeatures().contains(Feature.PEDAL_BASS);


            if (!isSlash && !isPedalBass)
            {
                cliCs = scsSpt.higher(cliCs);
                continue;
            }


            var bassRelPitch = ecs.getBassNote().getRelativePitch();
            var brCliCs = scsSpt.getBeatRange(cliCs);


            if (isPedalBass)
            {
                // How long is the pedal bass  ?
                var cliCsStopPedalBass = getStopPedalBassChord(cliCs, scsSpt);
                var stopPedalBeatPos = cliCsStopPedalBass == null ? scsSpt.getBeatRange().to : scsSpt.toPositionInBeats(cliCsStopPedalBass.getPosition());
                FloatRange extendedChordBeatRange = new FloatRange(brCliCs.from, stopPedalBeatPos);


                // Accomodate for non-quantized playing
                var notesBeatRange = extendedChordBeatRange.getTransformed(extendedChordBeatRange.from >= NON_QUANTIZED_WINDOW ? -NON_QUANTIZED_WINDOW : 0,
                        -NON_QUANTIZED_WINDOW);
                var nes = new ArrayList<>(p.subSet(notesBeatRange, true));


                if (!pos.isFirstBarBeat() || extendedChordBeatRange.size() < nbBeatsPerBar)
                {
                    // Simple, just change the pitch of existing notes
                    for (var ne : nes)
                    {
                        var neNew = ne.setPitch(getClosestAndAcceptableBassPitch(ne, bassRelPitch), false);
                        p.replace(ne, neNew);
                    }
                } else
                {
                    // Starting on a beat for 1 bar or more: use a pattern like quarter half quarter half ...
                    var ne0 = nes.isEmpty() ? null : nes.get(0);
                    int pitch = ne0 == null ? InstrumentFamily.Bass.toAbsolutePitch(bassRelPitch) : getClosestAndAcceptableBassPitch(ne0, bassRelPitch);

                    p.removeAll(nes);

                    if (Math.random() >= 0.5)
                    {
                        addPedalNotes1(extendedChordBeatRange, pitch, p);
                    } else
                    {
                        addPedalNotes2(extendedChordBeatRange, pitch, p);
                    }
                }


                // Skip to non pedal chord 
                cliCs = cliCsStopPedalBass;

            } else
            {
                // Slash chord with no pedal bass, make sure bass note is played during the 2 first beats
                var notesBeatRange = brCliCs.getTransformed(brCliCs.from >= NON_QUANTIZED_WINDOW ? -NON_QUANTIZED_WINDOW : 0, -NON_QUANTIZED_WINDOW);

                var nes = new ArrayList<>(p.subSet(notesBeatRange, true));
                nes.stream()
                        .filter(ne -> ne.getPositionInBeats() < brCliCs.from + 2f - NON_QUANTIZED_WINDOW)
                        .forEach(ne -> 
                        {
                            var neNew = ne.setPitch(getClosestAndAcceptableBassPitch(ne, bassRelPitch), false);
                            p.replace(ne, neNew);
                        });

                cliCs = scsSpt.higher(cliCs);
            }

        }
    }

    private void processSwingFeelTempoAdapter(Phrase p, FloatRange beatRange, int tempo)
    {
        var brAdjusted = beatRange.getTransformed(beatRange.from >= 0.4f ? -0.4f : 0, 0);       // Take into accound possible anticipated/pushed notes        
        float intensity = BassGeneratorSettings.getInstance().getSwingProfileIntensity();
        LOGGER.log(Level.FINE, "processSwingFeelTempoAdapter() beatRange={0} intensity={1} tempo={2}", new Object[]
        {
            beatRange, intensity, tempo
        });
        SwingProfile profile = SwingProfile.create(intensity);
        SwingBassTempoAdapter bassAdapter = new SwingBassTempoAdapter(profile, rhythm.getTimeSignature());
        bassAdapter.adaptToTempo(p, brAdjusted, ne -> true, tempo);   // Does not process notes not contained in brAdjusted
    }

    /**
     * Make sure our notes do not overlap other SongParts.
     *
     * @param context
     * @param p
     * @throws org.jjazz.rhythm.api.UserErrorGenerationException
     */
    private void enforceSongPartsBounds(SongContext context, Phrase p) throws UserErrorGenerationException
    {
        Song song = context.getSong();
        var barRange = context.getBarRange();
        var songChordSequence = new SongChordSequence(song, barRange);  // throws UserErrorGenerationException. Handle alternate chord symbols.                               

        // Merged SimpleChordSequences which do NOT use our rhythm
        var scsList = songChordSequence.buildSimpleChordSequences(spt -> spt.getRhythm() != rhythm);
        for (var scs : scsList)
        {
            var beatRange = scs.getBeatRange();
            Phrases.silence(p, beatRange, true, false, NON_QUANTIZED_WINDOW);


//            var crossingNotes = Phrases.getCrossingNotes(p, beatRange.from, false);
//            LOGGER.log(Level.SEVERE, "enforceSongPartsBounds() DEBUG control br={0} crossingNotesStart={1}", new Object[]
//            {
//                beatRange, crossingNotes
//            });
        }

        // If context bar range is in the middle of the song, we need also to silence bars before and after
        var beatRange = songChordSequence.getBeatRange();
        if (beatRange.from > 1)
        {
            // Silence only 1 beat, we just want to silent slight overlaps due non-quantized play or swing-feel tempo adaptations            
            var beatRangeBefore = new FloatRange(beatRange.from - 1f, beatRange.from);
            Phrases.silence(p, beatRangeBefore, true, false, NON_QUANTIZED_WINDOW);
        }
        if (barRange.to < song.getSize() - 1)
        {
            // Silence only 1 beat, we just want to silent slight overlaps due non-quantized play or swing-feel tempo adaptations            
            var beatRangeBefore = new FloatRange(beatRange.to, beatRange.to + 1f);
            Phrases.silence(p, beatRangeBefore, true, false, NON_QUANTIZED_WINDOW);
        }

    }

    /**
     * Add pedal notes to p with pattern quarter-half-quarter-half-quarter etc.
     *
     * @param extendedChordBeatRange Add notes in this range. Starts on beat 0 of a bar
     * @param pitch                  The pedal note pitch
     * @param p
     */
    /**
     * Add pedal notes to p with pattern quarter-half-quarter-half-quarter etc.
     *
     * @param extendedChordBeatRange Add notes in this range. Starts on beat 0 of a bar
     * @param pitch                  The pedal note pitch
     * @param p
     */
    private void addPedalNotes1(FloatRange extendedChordBeatRange, int pitch, Phrase p)
    {
        float beatPos = extendedChordBeatRange.from;

        while (beatPos < extendedChordBeatRange.to)
        {
            int oneOrTwo = ((int) beatPos) % 2 == 0 ? 1 : 2;
            float dur = oneOrTwo - 2 * NON_QUANTIZED_WINDOW;
            if (beatPos + dur >= extendedChordBeatRange.to)
            {
                dur = extendedChordBeatRange.to - 2 * NON_QUANTIZED_WINDOW - beatPos;
            }

            int vel = Velocities.getRandomBassVelocity();
            var ne = new NoteEvent(pitch, dur, vel, beatPos);
            p.add(ne);

            beatPos += oneOrTwo;
        }
    }

    /**
     * Add pedal notes to p with pattern quarter_dotted-quarter_dotted-quarter-quarter_dotted- etc.
     *
     * @param extendedChordBeatRange Add notes in this range. Starts on beat 0 of a bar
     * @param pitch                  The pedal note pitch
     * @param p
     */
    private void addPedalNotes2(FloatRange extendedChordBeatRange, int pitch, Phrase p)
    {
        float beatPos = extendedChordBeatRange.from;
        assert beatPos % rhythm.getTimeSignature().getNbNaturalBeats() == 0 : "extendedChordBeatRange=" + extendedChordBeatRange;

        while (beatPos < extendedChordBeatRange.to)
        {
            float inBarBeat = beatPos % rhythm.getTimeSignature().getNbNaturalBeats();
            float advance = inBarBeat < 3 ? 1.5f : 1f;
            float dur = advance - 2 * NON_QUANTIZED_WINDOW;
            if (beatPos + dur >= extendedChordBeatRange.to)
            {
                dur = extendedChordBeatRange.to - 2 * NON_QUANTIZED_WINDOW - beatPos;
            }

            int vel = Velocities.getRandomBassVelocity();
            var ne = new NoteEvent(pitch, dur, vel, beatPos);
            p.add(ne);

            beatPos += advance;
        }
    }


    /**
     * Move a chord symbol to newPos in scs.
     *
     * @param scs
     * @param cliCs
     * @param newPos
     */
    private void moveChordSymbol(ChordSequence scs, CLI_ChordSymbol cliCs, Position newPos)
    {
        var movedCliCs = (CLI_ChordSymbol) cliCs.getCopy(null, newPos);
        assert scs.remove(cliCs) : "cliCs" + cliCs + " scs=" + scs;
        scs.add(movedCliCs);
    }

    /**
     * Get the first next chord after cliCS which is not a pedal bass or which does not use the same bass note than cliCs.
     * <p>
     * @param cliCs  A pedal bass chord
     * @param scsSpt
     * @return Can be null if we reached the end of scsSpt
     */
    private CLI_ChordSymbol getStopPedalBassChord(CLI_ChordSymbol cliCs, SimpleChordSequence scsSpt)
    {
        var bassNote = cliCs.getData().getBassNote();

        var nextCliCs = scsSpt.higher(cliCs);
        while (nextCliCs != null)
        {
            var ecsNext = nextCliCs.getData();
            if (ecsNext.getRenderingInfo().getFeatures().contains(Feature.PEDAL_BASS) && ecsNext.getBassNote().equalsRelativePitch(bassNote))
            {
                nextCliCs = scsSpt.higher(nextCliCs);
            } else
            {
                break;
            }
        }

        return nextCliCs;
    }


    private void clearCacheData()
    {
        WbpSourceAdaptation.clearCacheData();
    }

    // =====================================================================================================================
    // Inner classes
    // =====================================================================================================================

}
