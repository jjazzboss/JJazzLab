/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.yamjjazz.rhythm.api;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.harmony.api.*;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.spi.ScaleManager;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midi.api.keymap.StandardKeyMapConverter;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.AccentProcessor;
import org.jjazz.rhythmmusicgeneration.api.AccentProcessor.HoldShotMode;
import org.jjazz.rhythmmusicgeneration.api.AnticipatedChordProcessor;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.phrase.api.Grid;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SourcePhrase;
import org.jjazz.phrase.api.SourcePhraseSet;
import org.jjazz.rhythmmusicgeneration.api.PhraseUtilities;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.SongFactory;
import org.openide.util.Exceptions;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.yamjjazz.rhythm.SpsRandomPicker;
import org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings.NoteTranspositionRule;
import org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings.NoteTranspositionTable;
import static org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings.RetriggerRule.NOTE_GENERATOR;
import static org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings.RetriggerRule.PITCH_SHIFT;
import static org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings.RetriggerRule.PITCH_SHIFT_TO_ROOT;
import static org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings.RetriggerRule.RETRIGGER;
import static org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings.RetriggerRule.RETRIGGER_TO_ROOT;
import static org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings.RetriggerRule.STOP;

/**
 * Use YamJJazz style tracks to render music.
 */

public class YamJJazzRhythmGenerator implements MusicGenerator
{

    public static Level LogLevel = Level.FINE;


    /**
     * The musical phrases for a subpart of a song which has one time signature.
     */
    private record ChordSeqPhrases(SimpleChordSequence simpleChordSequence, HashMap<AccType, Phrase> mapAccTypePhrase)
            {

    }
    public static boolean ENABLE_DRUM_KEY_MAPPING = true;
    private YamJJazzRhythm rhythm;
    private List<RhythmVoice> rhythmVoices;
    private SongContext contextOriginal;
    private SongContext contextWork;

    /**
     * The Chord Sequence with all the chords.
     */
    private SongChordSequence songChordSequence;

    protected static final Logger LOGGER = Logger.getLogger(YamJJazzRhythmGenerator.class.getSimpleName());

    public YamJJazzRhythmGenerator(YamJJazzRhythm r)
    {
        if (r == null)
        {
            throw new NullPointerException("r=" + r);   //NOI18N
        }
        rhythm = r;
    }

    @Override
    public HashMap<RhythmVoice, Phrase> generateMusic(SongContext contextOrig, RhythmVoice... rvs) throws MusicGenerationException
    {
        Objects.requireNonNull(contextOrig);
        var rhythmRvs = rhythm.getRhythmVoices();
        var rvsList = List.of(rvs);
        Preconditions.checkArgument(rhythmRvs.containsAll(rvsList), "rvsList=%s\nrhythmRvs=%s", rvsList, rhythmRvs);

        rhythmVoices = rvsList.isEmpty() ? rhythmRvs : rvsList;
        contextOriginal = contextOrig;


        // Prepare a working context which can be modified 
        IntRange contextBarRange = contextOriginal.getBarRange();
        FloatRange contextBeatRange = contextOriginal.getBeatRange();
        SongFactory sf = SongFactory.getInstance();
        Song songWork = sf.getCopy(contextOriginal.getSong(), true, false);     // Sgs disconnected from Cls
        this.contextWork = new SongContext(songWork, contextOriginal.getMidiMix(), contextBarRange);


        // Introduce fake section/songpart when a Fill rhythm parameter is used
        preprocessFillParameter(contextWork);     // SongStructure and ChordLeadsheet might be changed independently


        // Build the main chord sequence
        songChordSequence = new SongChordSequence(songWork, contextWork.getBarRange());   // Throw UserErrorGenerationException but no risk: will have a chord at beginning. Handle alternate chord symbols.       
        songChordSequence.removeRedundantStandardChords();

        LOGGER.log(LogLevel, "generateMusic()-- rhythm={0} songChordSequence={1}", new Object[]
        {
            rhythm.getName(), songChordSequence
        });

        // The final result to fill in
        List<ChordSeqPhrases> chordSeqPhrases = getAllPhrasesAllChordSequences();
                
        // Robustness check
        assert !chordSeqPhrases.isEmpty();
        for (var csp : chordSeqPhrases)
        {
            if (!contextBarRange.contains(csp.simpleChordSequence.getBarRange()))
            {
                throw new IllegalStateException("contextBarRange=" + contextBarRange + " csp=" + csp);
            } else if (!contextBeatRange.contains(csp.simpleChordSequence.getBeatRange(), false))
            {
                throw new IllegalStateException("contextBeatRange=" + contextBeatRange + " csp.beatRange()=" + csp.simpleChordSequence.getBeatRange());
            }
        }

        // Get a simplified version: merge all ChordSequences which use our rhythm
        List<ChordSeqPhrases> chordSeqPhrasesMerged = mergeChordSequences(chordSeqPhrases);

        // Perfom post process operations
        processAnticipationsAndAccents(chordSeqPhrasesMerged);

        // Apply the Intensity parameter 
        processIntensityParameter(chordSeqPhrasesMerged);

        // Post process bass line for in-bar chord symbols
        processBassLine(chordSeqPhrasesMerged);

        // Fill the resulting phrase for each RhythmVoice    
        HashMap<RhythmVoice, Phrase> res = new HashMap<>();
        for (RhythmVoice rv : rhythmVoices)       // Some can be RhythmVoiceDelegates
        {
            // Get or create the resulting phrase
            Phrase pRes = res.get(rv);
            if (pRes == null)
            {
                int destChannel = getChannelFromMidiMix(rv); // Manage the case of RhythmVoiceDelegate
                pRes = new Phrase(destChannel, rv.isDrums());
                res.put(rv, pRes);
            }

            // For each chord sequence update the resulting phrase
            AccType at = AccType.getAccType(rv);     // The AccType corresponding to this RhythmVoice
            for (ChordSeqPhrases csp : chordSeqPhrasesMerged)
            {
                HashMap<AccType, Phrase> map = csp.mapAccTypePhrase();
                Phrase p = map.get(at);
                if (p != null)
                {
                    pRes.add(p);
                } else
                {
                    // Some AccType may not be used on all styleParts of a style.
                    LOGGER.log(LogLevel, "generateMusic() no phrase for at={0}", at);
                }
            }
        }
        return res;
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================
    /**
     * Get all phrases for all AccTypes for all song context parts using our rhythm.
     * <p>
     *
     * @return @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private List<ChordSeqPhrases> getAllPhrasesAllChordSequences() throws MusicGenerationException
    {
        LOGGER.fine("getAllPhrasesAllChordSequences()--");

        List<ChordSeqPhrases> res = new ArrayList<>();


        // Process each used variation
        var rpVariation = RP_SYS_Variation.getVariationRp(rhythm);
        Set<String> usedVariationValues = contextWork.getSongParts().stream()
                .filter(spt -> spt.getRhythm() == rhythm)
                .map(spt -> spt.getRPValue(rpVariation))
                .collect(Collectors.toSet());
        for (var rpVariationValue : usedVariationValues)
        {
            StylePart stylePart = rhythm.getStylePart(rpVariationValue);
            int complexity = rhythm.getComplexityLevel(rpVariationValue);
            if (stylePart == null || complexity < 1)
            {
                LOGGER.log(Level.SEVERE,
                        "getAllPhrasesAllChordSequences() Invalid values stylePart={0} complexity={1} rhythm={2} rpVariationValue={3}", new Object[]
                        {
                            stylePart, complexity, rhythm.getName(), rpVariationValue
                        });
                throw new MusicGenerationException("Invalid rhythm data for rhythm " + rhythm.getName());
            }


            // Get all the (merged) bar ranges which use rpVariationValue
            var barRanges = contextWork.getMergedBarRanges(rhythm, rpVariation, rpVariationValue);


            // Generate music for each bar range
            for (var barRange : barRanges)
            {
                float beatStart = contextWork.getSong().getSongStructure().toPositionInNaturalBeats(new Position(barRange.from));
                SimpleChordSequence cSeq = new SimpleChordSequence(songChordSequence.subSequence(barRange, true), beatStart, rhythm.getTimeSignature());
                HashMap<AccType, Phrase> mapAccTypePhrase = getAllAccTypesPhrasesOneChordSequence(stylePart, complexity, cSeq);
                ChordSeqPhrases csp = new ChordSeqPhrases(cSeq, mapAccTypePhrase);
                res.add(csp);
            }

        }

        return res;
    }

    /**
     * Get all phrases (all AccTypes) for one chord sequence.
     * <p>
     * If cSeq is bigger than the StylePart length (most common case), slice cSeq in smaller subsequences to be processed.
     *
     * @param stylePart
     * @param complexity
     * @param cSeq       There must be a chord on first bar/beat 0. Can be any length.
     * @return
     * @throws MusicGenerationException
     */
    private HashMap<AccType, Phrase> getAllAccTypesPhrasesOneChordSequence(StylePart stylePart, int complexity, SimpleChordSequence cSeq) throws MusicGenerationException
    {
        if (stylePart == null || !cSeq.hasChordAtBeginning())
        {
            throw new IllegalArgumentException("stylePart=" + stylePart + " cSeq=" + cSeq);   //NOI18N
        }
        LOGGER.log(LogLevel, "getAllAccTypesPhrasesOneChordSequence() -- stylePart={0} cSeq={1}", new Object[]
        {
            stylePart, cSeq
        });

        HashMap<AccType, Phrase> res = new HashMap<>();

        int cSeqEndBar = cSeq.getBarRange().to;
        int stylePartNbBars = rhythm.getStyle().getStylePartSizeInBars(stylePart.getType());
        if (stylePartNbBars <= 0)
        {
            LOGGER.log(Level.SEVERE, "getAllAccTypesPhrasesOneChordSequence() Invalid value for stylePartNbBars={0}  stylePart={1}", new Object[]
            {
                stylePartNbBars, stylePart
            });
            throw new MusicGenerationException(
                    "Invalid rhythm data for rhythm " + rhythm.getName() + " / stylePart=" + stylePart.getType());
        }
        int nbLoops = cSeq.getBarRange().size() / stylePartNbBars;
        nbLoops += ((cSeq.getBarRange().size() % stylePartNbBars) > 0) ? 1 : 0;    // add one more loop if there is a remainder

        for (int i = 0; i < nbLoops; i++)
        {
            // Process one source phrase at a time
            int startBar = cSeq.getBarRange().from + i * stylePartNbBars;
            int endBar = Math.min(startBar + stylePartNbBars - 1, cSeqEndBar);
            SimpleChordSequence subSeq = cSeq.subSequence(new IntRange(startBar, endBar), true);

            // Get all the phrases for this short chord sequence
            HashMap<AccType, Phrase> mapAccTypePhrase = getAllAccTypesPhrasesOneShortChordSequence(stylePart, complexity, subSeq);

            // Append the phrases to the result
            for (AccType at : mapAccTypePhrase.keySet())
            {
                Phrase p = mapAccTypePhrase.get(at);
                Phrase pRes = res.get(at);
                if (pRes == null)
                {
                    pRes = new Phrase(p.getChannel(), at.isDrums());
                    res.put(at, pRes);
                }
                pRes.add(p);
                // Make sure all notes are OFF at the end
                Phrases.silenceAfter(pRes, cSeq.getBeatRange().to);
            }
        }
        //LOGGER.log(LogLevel, "getAllAccTypesPhrasesOneChordSequence() res=" + res);
        return res;
    }

    /**
     * Get all phrases for each StylePart's AccType adjusted to a "short ChordSequence".
     * <p>
     * Short chordSequence means its size is equal or less than the stylePart.<br>
     * If there are several SourcePhraseSet alternatives for the given stylePart and complexity, we select
     *
     * @param stylePart
     * @param complexity
     * @param shortcSeq  There must be a chord on first bar/beat 0. Can't be longer than a sourcePhrase length.
     * @return
     * @throws MusicGenerationException
     */
    private HashMap<AccType, Phrase> getAllAccTypesPhrasesOneShortChordSequence(StylePart stylePart, int complexity, SimpleChordSequence shortcSeq) throws MusicGenerationException
    {

        if (stylePart == null || complexity < 1 || !shortcSeq.hasChordAtBeginning() || shortcSeq.getBarRange().size() > rhythm.getStyle().getStylePartSizeInBars(
                stylePart.getType()))
        {
            throw new IllegalArgumentException("stylePart=" + stylePart + " shortcSeq=" + shortcSeq);   //NOI18N
        }
        LOGGER.log(LogLevel, "getAllAccTypesPhrasesOneShortChordSequence() -- stylePart={0} shortcSeq={1}", new Object[]
        {
            stylePart,
            shortcSeq
        });
        HashMap<AccType, Phrase> mapAccTypePhrase = new HashMap<>();


        // Pick the SourcePhraseSet to be used depending on the context
        SpsRandomPicker srp = SpsRandomPicker.getInstance(contextWork.getSong().getSongStructure(), rhythm, stylePart, complexity);
        SourcePhraseSet sps = srp.pick(shortcSeq.getBarRange().from);


        // Get the phrase for each AccType = bass, chord1, drums, etc.
        for (AccType at : stylePart.getAccTypes())
        {
            RhythmVoice rv = rhythm.getRhythmVoice(at);
            if (!rhythmVoices.contains(rv))
            {
                // Skip the RhythmVoices which are not requested
                continue;
            }
            Phrase p = getOneAccTypePhraseOneShortChordSequence(stylePart, sps, at, shortcSeq);
            mapAccTypePhrase.put(at, p);
        }

        return mapAccTypePhrase;
    }

    /**
     * Get the style's source phrase for the specified style's AccType for a "short chordSequence".
     * <p>
     * Short chordSequence means its size is equal or less than the stylePart source phrase.
     *
     * @param stylePart
     * @param sps       The SourcePhraseSet to be used.
     * @param at
     * @param shortcSeq Length must be less or equal to stylePart length
     * @return A Phrase starting at cSeq's start position, and length=stylePart.nbBars
     * @throws MusicGenerationException
     */
    private Phrase getOneAccTypePhraseOneShortChordSequence(StylePart stylePart, SourcePhraseSet sps, AccType at, SimpleChordSequence shortcSeq) throws MusicGenerationException
    {
        if (stylePart == null || sps == null || at == null || !shortcSeq.hasChordAtBeginning()
                || shortcSeq.getBarRange().size() > rhythm.getStyle().getStylePartSizeInBars(stylePart.getType()))
        {
            throw new IllegalArgumentException("stylePart=" + stylePart + " sps=" + sps + " at=" + at + " cSeq=" + shortcSeq);   //NOI18N
        }

        RhythmVoice rv = rhythm.getRhythmVoice(at);
        if (rv == null)
        {
            LOGGER.log(Level.SEVERE, "getOneAccTypePhraseOneShortChordSequence() at={0}  stylePart={1} rhythm={2}", new Object[]
            {
                at, stylePart, rhythm.getName()
            });

            throw new MusicGenerationException("Invalid data for rhythm " + rhythm.getName() + " / at=" + at);
        }
        int destChannel = getChannelFromMidiMix(rv);
        assert destChannel != -1 : "stylePart=" + stylePart + " rv=" + rv + " destChannel=" + destChannel + " at=" + at;   //NOI18N


        // Take all the possible source channels, we'll do the selection when we have the destination chord
        List<Integer> sourceChannels = stylePart.getSourceChannels(at, null, null);
        LOGGER.log(LogLevel, "getOneAccTypePhraseOneShortChordSequence()   at={0} sourceChannels={1}", new Object[]
        {
            at, sourceChannels
        });

        // The position of the chord sequence in the song structure
        FloatRange cSeqBeatRange = shortcSeq.getBeatRange();

        // The resulting phrase combining all source channels
        Phrase pRes = new Phrase(destChannel, at.isDrums());
        //
        // LOOP ON EACH SOURCE CHANNEL
        //
        for (Integer srcChannel : sourceChannels)
        {
            // The cTab settings for this source channel
            final CtabChannelSettings cTab = stylePart.getCtabChannelSettings(srcChannel);


            // Get the source phrase
            SourcePhrase pSrc = sps.getPhrase(srcChannel);
            if (pSrc == null)
            {
                // Can happen in case of inconsistency between CASM and Midi notes (channel is defined in CASM but no Midi note for this channel)
                LOGGER.log(LogLevel, "getOneAccTypePhraseOneShortChordSequence()   unexpected absence of phrase for srcChannel={0}",
                        srcChannel);
                continue;
            }


            // Adjust it to shortcSeq's start and length => need a copy
            pSrc = pSrc.clone();
            pSrc.shiftAllEvents(cSeqBeatRange.from, false);
            Phrases.silenceAfter(pSrc, cSeqBeatRange.to);


            LOGGER.log(LogLevel, "getOneAccTypePhraseOneShortChordSequence() at={0} srcChannel={1} pSrc={2}", new Object[]
            {
                at, srcChannel, pSrc
            });


            // Possibly remap drum notes if source/dest. keymaps differ
            if (at.isDrums())
            {
                Instrument ins = contextWork.getMidiMix().getInstrumentMix(destChannel).getInstrument();
                DrumKit.KeyMap destMap = KeyMapGM.getInstance();    // By default
                if (ins.isDrumKit())
                {
                    destMap = ins.getDrumKit().getKeyMap();
                } else if (ins != GMSynth.getInstance().getVoidInstrument())
                {
                    LOGGER.log(Level.WARNING,
                            "getOneAccTypePhraseOneShortChordSequence() non-drums instrument used for a drums channel! at={0} ins={1}, srcChannel={2}, stylePart={3}",
                            new Object[]
                            {
                                at,
                                ins, srcChannel, stylePart
                            });
                }
                if (rv.getDrumKit() == null)
                {
                    LOGGER.log(Level.WARNING,
                            "getOneAccTypePhraseOneShortChordSequence() non-drums rhythm voice for a drums channel! at={0} rv={1} srcChannel={2}, stylePart={3}",
                            new Object[]
                            {
                                at,
                                rv.getName(), srcChannel, stylePart
                            });
                } else
                {
                    remapDrumNotes(pSrc, rv.getDrumKit().getKeyMap(), destMap);
                }
            }


            // Special shortcut case, we can directly reuse the source phrase whatever the chord sequence
            // Typically for Drums on SFF1 files
            if (cTab.isSingleCtb2() && cTab.ctb2Main.ntt == NoteTranspositionTable.BYPASS && cTab.ctb2Main.ntr == NoteTranspositionRule.ROOT_FIXED)
            {
                pRes.add(pSrc);
                LOGGER.log(LogLevel, "getOneAccTypePhraseOneShortChordSequence()   ByPass+RootFixed: directly reuse source phrase");
                continue;
            }


            // The destination phrase for this source phrase, eg for all chord symbols
            Phrase pDest = new Phrase(0, at.isDrums());          // channel is not important since phrase will be merged later to pRes                                      


            // Get a complete destination phrase for each chord symbol
            for (var destCliCs : shortcSeq)
            {
                ExtChordSymbol destEcs = destCliCs.getData();
                ChordRenderingInfo cri = destEcs.getRenderingInfo();
                YamChord yc = YamChord.get(destEcs.getChordType().getName());
                if (yc == null)
                {
                    String msg = "Chord symbol " + destEcs.getChordType().getName() + " could not be converted into a Yamaha chord symbol.";
                    LOGGER.log(Level.SEVERE, "getOneAccTypePhraseOneShortChordSequence() {0}", msg);
                    throw new MusicGenerationException(msg);
                }


                // Check that srcChannel is valid for our destination ChordType/Root note
                List<Integer> validSrcChannels = stylePart.getSourceChannels(at, destEcs.getRootNote(), yc);
                if (!validSrcChannels.contains(srcChannel))
                {
                    // Skip to next chord in the sequence
                    LOGGER.log(LogLevel,
                            "getOneAccTypePhraseOneShortChordSequence()      destEcs={0} yc={1} => channel does not match, skip",
                            new Object[]
                            {
                                destEcs,
                                yc
                            });
                    continue;
                }


                LOGGER.log(LogLevel,
                        "getOneAccTypePhraseOneShortChordSequence()      destCliCs={0} yc={1}", new Object[]
                        {
                            destCliCs, yc
                        });


                // The destination phrase for one chord symbol
                Phrase pDestOneCs = new Phrase(0, at.isDrums()); // Channel not important here since phrase will be merged into another phrase


                // Split the source phrase in potentially 3 notes range (SFF2 support)
                // Low range
                if (cTab.ctb2Low != null)
                {
                    // There is specific ctb2 settings for the lowest range of notes
                    LOGGER.log(LogLevel, "getOneAccTypePhraseOneShortChordSequence() Processing SFF2 ctb2 low range at={0}", at);
                    Ctb2ChannelSettings ctb2 = cTab.ctb2Low;
                    SourcePhrase pSrcLow = pSrc.getProcessedPhrase(ne -> ne.getPitch() < cTab.getCtb2MiddeLowPitch(), ne -> ne.clone());

                    if (ctb2.ntt == NoteTranspositionTable.BYPASS && ctb2.ntr == NoteTranspositionRule.ROOT_FIXED)
                    {
                        // Special case, we can directly reuse the source phrase whatever the chord sequence    
                        pDestOneCs.add(pSrcLow);
                        LOGGER.log(LogLevel, "   ByPass+RootFixed: directly reusing source phrase");
                    } else
                    {
                        Phrase fittedPhrase = fitSrcPhraseToChordSymbol(pSrcLow, ctb2, destEcs, cri);
                        Phrases.limitPitch(fittedPhrase, ctb2.noteLowLimit.getPitch(), ctb2.noteHighLimit.getPitch());
                        pDestOneCs.add(fittedPhrase);
                    }
                }


                // Middle/main range (always used)
                if (cTab.isSingleCtb2())
                {
                    // There is only one main single ctb2 (always the case for SFF1 files)
                    pDestOneCs = fitSrcPhraseToChordSymbol(pSrc, cTab.ctb2Main, destEcs, cri);  // We can replaceAll pDestOneCs
                    Phrases.limitPitch(pDestOneCs, cTab.ctb2Main.noteLowLimit.getPitch(), cTab.ctb2Main.noteHighLimit.getPitch());
                } else
                {
                    // There is also a low or high range too
                    LOGGER.log(LogLevel, "getOneAccTypePhraseOneShortChordSequence() Processing SFF2 ctb2 middle range at={0}", at);
                    Ctb2ChannelSettings ctb2 = cTab.ctb2Main;
                    SourcePhrase pSrcMain = pSrc.getProcessedPhrase(ne -> ne.getPitch() >= cTab.getCtb2MiddeLowPitch()
                            && ne.getPitch() <= cTab.getCtb2MiddeHighPitch(),
                            ne -> ne.clone());

                    if (ctb2.ntt == NoteTranspositionTable.BYPASS && ctb2.ntr == NoteTranspositionRule.ROOT_FIXED)
                    {
                        // Special case, we can directly reuse the source phrase whatever the chord sequence    
                        pDestOneCs.add(pSrcMain);
                        LOGGER.log(LogLevel, "   ByPass+RootFixed: directly reusing source phrase");
                    } else
                    {
                        Phrase fittedPhrase = fitSrcPhraseToChordSymbol(pSrcMain, ctb2, destEcs, cri);
                        Phrases.limitPitch(fittedPhrase, ctb2.noteLowLimit.getPitch(), ctb2.noteHighLimit.getPitch());
                        pDestOneCs.add(fittedPhrase);
                    }
                }


                // High range
                if (cTab.ctb2High != null)
                {
                    // There is specific ctb2 settings for the highest range of notes
                    LOGGER.log(LogLevel, "getOneAccTypePhraseOneShortChordSequence() Processing SFF2 ctb2 high range at={0}", at);
                    Ctb2ChannelSettings ctb2 = cTab.ctb2High;
                    SourcePhrase pSrcHigh = pSrc.getProcessedPhrase(ne -> ne.getPitch() > cTab.getCtb2MiddeHighPitch(), ne -> ne.clone());

                    if (ctb2.ntt == NoteTranspositionTable.BYPASS && ctb2.ntr == NoteTranspositionRule.ROOT_FIXED)
                    {
                        // Special case, we can directly reuse the source phrase whatever the chord sequence    
                        pDestOneCs.add(pSrcHigh);
                        LOGGER.log(LogLevel, "   ByPass+RootFixed: directly reusing source phrase");
                    } else
                    {
                        Phrase fittedPhrase = fitSrcPhraseToChordSymbol(pSrcHigh, ctb2, destEcs, cri);
                        Phrases.limitPitch(fittedPhrase, ctb2.noteLowLimit.getPitch(), ctb2.noteHighLimit.getPitch());
                        pDestOneCs.add(fittedPhrase);
                    }

                }


                // LOGGER.log(LogLevel, "------getOneAccTypePhraseOneShortChordSequence()  pre-slice  destCliCs=" + destCliCs + " pDestOneCs=" + pDestOneCs.toString());
                // Keep only the relevant slice
                // If next chordsymbol is using the same source phrase, we can let the notes ring after the slice (cutRight=false): noteON transitions
                // will be managed by fixNoteOnTransitions(). If NOT, fixNoteOnTransitions() can't be used so we need to 
                // cut a "hard" slice (cutRight=true).
                int cutRight = 0; // By default consider that the next chord symbol uses the same source phrase
                if (destCliCs != shortcSeq.last())
                {
                    // All ChordSymbols except the last one
                    CLI_ChordSymbol nextDestCliCs = shortcSeq.higher(destCliCs);
                    ExtChordSymbol nextDestEcs = nextDestCliCs.getData();
                    YamChord nextYc = YamChord.get(nextDestEcs.getChordType().getName());
                    assert nextYc != null : "nextDestEcs=" + nextDestEcs;   //NOI18N
                    cutRight = !stylePart.getSourceChannels(at, nextDestEcs.getRootNote(), nextYc).contains(srcChannel) ? 1 : 0;
                }
                pDestOneCs = Phrases.getSlice(pDestOneCs, shortcSeq.getBeatRange(destCliCs), true, cutRight, 0.1f);

                // LOGGER.log(LogLevel, "------getOneAccTypePhraseOneShortChordSequence()  post-slice cutRight="+cutRight+" pDestOneCs=" + pDestOneCs.toString());

                // Merge in the destination phrase 
                pDest.add(pDestOneCs);


            }   // END  for (int i = 0; i < shortcSeq.size(); i++)


            //LOGGER.log(LogLevel, "getOneAccTypePhraseOneShortChordSequence()      pre-fixTransitions  pDest=" + pDest.toString());
            // Fix transitions for notes still ON during chord changes
            // Always use the main ctb2 RTR: not perfect since in some SFF2 files it seems there could be different RTR values for lowest/highest notes
            fixNoteOnTransitions(pDest, shortcSeq, cTab.ctb2Main);

            LOGGER.log(LogLevel, "getOneAccTypePhraseOneShortChordSequence()      post-fixTransitions pDest={0}", pDest);

            // Merge the fitted phrase for this source channel in the global result phrase
            pRes.add(pDest);


        } // END  for (Integer srcChannel : sourceChannels)


        Phrases.fixOverlappedNotes(pRes);

        LOGGER.log(LogLevel, "getOneAccTypePhraseOneShortChordSequence()   pRes={0}", pRes);

        return pRes;
    }

    /**
     * Build a new phrase by adapting pSrc to destEcs, taking in account the specified parameters.
     *
     * @param pSrc
     * @param ctb2
     * @param destEcs
     * @param cri
     * @return NoteEvents have their client property set to the PARENT_NOTE, see the Phrases.fitXX() methods.
     * @throws IllegalStateException
     */
    private Phrase fitSrcPhraseToChordSymbol(SourcePhrase pSrc, Ctb2ChannelSettings ctb2, ExtChordSymbol destEcs, ChordRenderingInfo cri) throws IllegalStateException
    {
        Phrase pRes;
        if (null == ctb2.ntr)
        {
            // GUITAR NTR - only 3 possible values
            switch (ctb2.ntt)
            {
                case ALL_PURPOSE, ARPEGGIO, STROKE -> // Use CHORD oriented processing
                {
                    pRes = PhraseUtilities.fitChordPhrase2ChordSymbol(pSrc, destEcs);
                }
//              case STROKE -> 
//                {
//                    // To be tested, use MELODY oriented processing !
//                    // (in SFF1, all guitars are played in CHORD mode, so should be ARPEGGIO...)
//                    pRes = Phrases.fitMelodyPhrase2ChordSymbol(pSrc, destEcs, true);
//                    // Apply the chord root upper limit (must be AFTER the fitting to dest chord symbol)
//                    if (destEcs.getRootNote().getRelativePitch() > ctb2.chordRootUpperLimit.getRelativePitch())
//                    {
//                        pRes = pRes.getTransposedPhrase(-12);
//                    }
//                 }
                default -> throw new IllegalStateException("cTab.ntt=" + ctb2.ntt);   //NOI18N
            }

//            END if (ROOT_FIXED) / (ROOT_TRANS) / GUITAR
        } else
        {
            switch (ctb2.ntr)
            {
                case ROOT_FIXED:
                    // ROOT_FIXED : for chord oriented source phrases
                    switch (ctb2.ntt)
                    {
                        case BYPASS:
                            // Should have been managed above
                            throw new IllegalStateException("cTab.ntt=" + ctb2.ntt);   //NOI18N
                        case MELODY:
                        case CHORD:
                            // fitChordPhrase2ChordSymbol() sets client property PARENT_NOTE for each destination note of the returned phrase
                            pRes = PhraseUtilities.fitChordPhrase2ChordSymbol(pSrc, destEcs);
                            break;
                        default:
                            LOGGER.log(Level.WARNING, "      Unexpected ctb2.ntt value={0}", ctb2.ntt);
                            pRes = PhraseUtilities.fitChordPhrase2ChordSymbol(pSrc, destEcs);
                            break;
                    }
                    break;
                case ROOT_TRANSPOSITION:
                    // ROOT_TRANS : for melody oriented source phrases
                    switch (ctb2.ntt)
                    {
                        case BYPASS ->
                        {
                            // Typically intros and endings
                            final int pitchDelta = destEcs.getRootNote().getRelativePitch() - pSrc.getSourceChordSymbol().getRootNote().getRelativePitch();
                            pRes = pSrc.getProcessedPhrasePitch(p -> p + pitchDelta);
                        }
                        case HARMONIC_MINOR_5, HARMONIC_MINOR ->
                        {
                            // Force the scale of the chord to be HARMONIC MINOR
                            StandardScaleInstance scale = new StandardScaleInstance(ScaleManager.MINOR_HARMONIC, destEcs.getRootNote());
                            ChordRenderingInfo newCri = new ChordRenderingInfo(cri, scale);
                            destEcs = destEcs.getCopy(null, newCri, destEcs.getAlternateChordSymbol(), destEcs.getAlternateFilter());
                            // fitMelodyPhrase2ChordSymbol() sets client property PARENT_NOTE for each destination note of the returned phrase
                            pRes = PhraseUtilities.fitMelodyPhrase2ChordSymbol(pSrc, destEcs, false);
                        }
                        case MELODIC_MINOR_5, MELODIC_MINOR ->
                        {
                            // Force the scale of the chord to be MELODIC_MINOR MINOR
                            StandardScaleInstance scale = new StandardScaleInstance(ScaleManager.MINOR_MELODIC, destEcs.getRootNote());
                            ChordRenderingInfo newCri = new ChordRenderingInfo(cri, scale);
                            destEcs = destEcs.getCopy(null, newCri, destEcs.getAlternateChordSymbol(), destEcs.getAlternateFilter());
                            // fitMelodyPhrase2ChordSymbol() sets client property PARENT_NOTE for each destination note of the returned phrase
                            pRes = PhraseUtilities.fitMelodyPhrase2ChordSymbol(pSrc, destEcs, false);
                        }
                        case NATURAL_MINOR_5, NATURAL_MINOR ->
                        {
                            // Force the scale of the chord to be NATURAL_MINOR
                            StandardScaleInstance scale = new StandardScaleInstance(ScaleManager.AEOLIAN, destEcs.getRootNote());
                            ChordRenderingInfo newCri = new ChordRenderingInfo(cri, scale);
                            destEcs = destEcs.getCopy(null, newCri, destEcs.getAlternateChordSymbol(), destEcs.getAlternateFilter());
                            // fitMelodyPhrase2ChordSymbol() sets client property PARENT_NOTE for each destination note of the returned phrase
                            pRes = PhraseUtilities.fitMelodyPhrase2ChordSymbol(pSrc, destEcs, false);
                        }
                        case DORIAN_5, DORIAN ->
                        {
                            // Force the scale of the chord to be DORIAN
                            StandardScaleInstance scale = new StandardScaleInstance(ScaleManager.DORIAN, destEcs.getRootNote());
                            ChordRenderingInfo newCri = new ChordRenderingInfo(cri, scale);
                            destEcs = destEcs.getCopy(null, newCri, destEcs.getAlternateChordSymbol(), destEcs.getAlternateFilter());
                            // fitMelodyPhrase2ChordSymbol() sets client property PARENT_NOTE for each destination note of the returned phrase
                            pRes = PhraseUtilities.fitMelodyPhrase2ChordSymbol(pSrc, destEcs, false);
                        }
                        case CHORD -> // Use the chord flag ON
                        {
                            pRes = PhraseUtilities.fitMelodyPhrase2ChordSymbol(pSrc, destEcs, true);
                        }
                        case MELODY ->
                        {
                            // fitMelodyPhrase2ChordSymbol() sets client property PARENT_NOTE for each destination note of the returned phrase
                            if (!ctb2.bassOn)
                            {
                                pRes = PhraseUtilities.fitMelodyPhrase2ChordSymbol(pSrc, destEcs, false);
                            } else
                            {
                                pRes = PhraseUtilities.fitBassPhrase2ChordSymbol(pSrc, destEcs);
                            }
                        }
                        default -> throw new IllegalStateException("cTab.ntt=" + ctb2.ntt);   //NOI18N
                    }   // Apply the chord root upper limit (must be AFTER the fitting to dest chord symbol)
                    if (destEcs.getRootNote().getRelativePitch() > ctb2.chordRootUpperLimit.getRelativePitch())
                    {
                        pRes.processPitch(p -> p - 12);
                    }
                    break;
                default:
                    // GUITAR NTR - only 3 possible values
                    switch (ctb2.ntt)
                    {
                        case ALL_PURPOSE, ARPEGGIO, STROKE -> // Use CHORD oriented processing
                        {
                            pRes = PhraseUtilities.fitChordPhrase2ChordSymbol(pSrc, destEcs);
                        }
                        default -> throw new IllegalStateException("cTab.ntt=" + ctb2.ntt);   //NOI18N
                    }
//                case STROKE:
//                    // To be tested, use MELODY oriented processing ! 
//                    // (in SFF1, all guitars are played in CHORD mode, so should be ARPEGGIO...)
//                    pRes = Phrases.fitMelodyPhrase2ChordSymbol(pSrc, destEcs, true);
//                    // Apply the chord root upper limit (must be AFTER the fitting to dest chord symbol)
//                    if (destEcs.getRootNote().getRelativePitch() > ctb2.chordRootUpperLimit.getRelativePitch())
//                    {
//                        pRes = pRes.getTransposedPhrase(-12);
//                    }
//                    break;
                // END if (ROOT_FIXED) / (ROOT_TRANS) / GUITAR            
            }
        }

        return pRes;
    }


    /**
     * Fix the transitions of notes ON in the specified phrase.
     * <p>
     * Detect when several notes share the same parent note. This happens when there is a chord change while a note is on in one source phrase. Parent notes
     * must be stored in each p's NoteEvent PARENT_NOTE client property.<p>
     * Note that the method won't work if p was built by merging two or more source phrases, since parentNotes will be different.
     * <p>
     * The method modifies the phrase p to apply the transition depending on the rtr setting:<br>
     * - shorten notes<br>
     * - create new notes<br>
     * - create PitchBendNoteEvent (not implemented)<br>
     *
     * @param pDest Notes position/duration must be consistent with cSeq's bounds.
     * @param cSeq  The chord sequence corresponding to phrase p.
     * @param ctb2  the channel settings of the phrase
     * @throws IllegalStateException
     */
    private void fixNoteOnTransitions(Phrase pDest, SimpleChordSequence cSeq, Ctb2ChannelSettings ctb2) throws IllegalStateException
    {
        LOGGER.log(LogLevel, "fixNoteOnTransitions() -- pDest={0}\ncSeq={1}\nctb2.rtr={2}", new Object[]
        {
            pDest, cSeq, ctb2.rtr
        });

        // Used to store the parent source notes ON at current time with the last related destination note
        HashMap<NoteEvent, NoteEvent> mapParentDestNotesOn = new HashMap<>();


        // Prepare data
        SongStructure ss = contextWork.getSong().getSongStructure();


        // Test each destination note, use array as we will modify the phrase
        for (var destNote : pDest.toArray(NoteEvent[]::new))
        {

            float posDestNote = destNote.getPositionInBeats();
            if (!cSeq.getBeatRange().contains(posDestNote, true))
            {
                throw new IllegalStateException("destNote=" + destNote + " cSeq=" + cSeq);   //NOI18N
            }


            // Keep track of previous destination notes still ON as we advance in time (time = destNote position)
            for (var it2 = mapParentDestNotesOn.keySet().iterator(); it2.hasNext();)
            {
                NoteEvent parentNoteOn = it2.next();
                NoteEvent parentDestNote = mapParentDestNotesOn.get(parentNoteOn);
                if (parentDestNote.getBeatRange().to <= posDestNote)
                {
                    // This dest note is OFF now, remove the association parent-destNote
                    it2.remove();
                }
            }

            // The parent note of the destination note
            NoteEvent parentNote = (NoteEvent) destNote.getClientProperties().get(Phrase.PARENT_NOTE);
            assert parentNote != null : "ne=" + destNote + " pDest=" + pDest;   //NOI18N


            // Check if our parent note has a previous destNote ON at current time = destNote position
            NoteEvent prevParentDestNote = mapParentDestNotesOn.get(parentNote);
            if (prevParentDestNote == null)
            {
                // NO, now destNote is the current destination note for this parentNote
                mapParentDestNotesOn.put(parentNote, destNote);
                continue;

            }

            // YES, we need to apply a transition between the 2 NoteEvents

            // Shorten the previous parent dest note, or even remove it if too short
            float shortenedDuration = posDestNote - prevParentDestNote.getPositionInBeats();
            NoteEvent shortenedPrevParentDestNote = prevParentDestNote.setDuration(shortenedDuration, true);

            if (prevParentDestNote.getDurationInBeats() >= Grid.PRE_CELL_BEAT_WINDOW_DEFAULT && shortenedDuration <= Grid.PRE_CELL_BEAT_WINDOW_DEFAULT)
            {
                // Note will be shortened to a very short note. Remove it, it's now probably useless musically, and 
                // this avoids problems later with chord hold processing when extending the duration of notes that 
                // are in the grid pre-cell beat window.
                LOGGER.log(LogLevel, "  Special case, too short note, removing prevParentDestNote={0}", prevParentDestNote);
                pDest.remove(prevParentDestNote);
            } else
            {
                LOGGER.log(LogLevel, "  Replace prevParentDestNote={0}> shortenedPrevParentDestNote={1}", new Object[]
                {
                    prevParentDestNote, shortenedPrevParentDestNote
                });
                pDest.replace(prevParentDestNote, shortenedPrevParentDestNote);
            }


            // Transition depends on RTR
            switch (ctb2.rtr)
            {
                case STOP ->
                {
                    // Remove destNote since it's a STOP transition
                    pDest.remove(destNote);
                    LOGGER.log(LogLevel, " STOP removed destNote={0}", destNote);

                    // From here there is no more destination note for parentNote
                    mapParentDestNotesOn.remove(parentNote);
                }


                case NOTE_GENERATOR, PITCH_SHIFT, RETRIGGER -> // Same as RETRIGGER
                    // Change the current destination note for parentNote
                    mapParentDestNotesOn.put(parentNote, destNote);
                case PITCH_SHIFT_TO_ROOT, RETRIGGER_TO_ROOT -> // Default to retrigger for now
                {
                    // Change pitch of destNote to the current chord symbol's root pitch (or bass note if specified)
                    Position pos = ss.toPosition(posDestNote);
                    CLI_ChordSymbol destCliCs = cSeq.getChordSymbol(pos);
                    assert destCliCs != null : "ne=" + destNote + " cSeq=" + cSeq;   //NOI18N
                    Note n = ctb2.bassOn ? destCliCs.getData().getBassNote() : destCliCs.getData().getRootNote();
                    int destCliCsRootPitch = destNote.getClosestPitch(n.getRelativePitch());
                    NoteEvent newDestNote = destNote.setPitch(destCliCsRootPitch, true);
                    pDest.replace(destNote, newDestNote);

                    LOGGER.log(LogLevel, "  RETRIGGER_TO_ROOT replace destNote={0} > newDestNote={1}", new Object[]
                    {
                        destNote, newDestNote
                    });


                    // Change the current destination note for parentNote
                    mapParentDestNotesOn.put(parentNote, newDestNote);
                }
                default -> throw new IllegalStateException("ctb2.rtr=" + ctb2.rtr);   //NOI18N
            }
            // Same as RETRIGGER
            // Default to retrigger for now
            // Default to retrigger for now
//                    case PITCH_SHIFT:
//                        // Remove dest note and replaceAll by a special pitch bend NoteEvent
//                        p.removeStylePart(destNote);
//                        // THIS DOES NOT WORK because pitch bend applies to all channel notes ! 
//                        PitchBendEvent pbe = new PitchBendEvent(destNote, prevParentDestNote.getPitch());
//                        p.add(pbe);
//                        // Change the current destination note for parentNote
//                        mapSrcDestNotesOn.put(parentNote, pbe);
//                        break;
//                    case PITCH_SHIFT_TO_ROOT:
//                        // Nothing
//                        break;

        }

    }


    /**
     * Modify the song's SongStructure and ChordLeadSheet to facilitate the processing of the RP_STD_FILL parameter.
     * <p>
     * Add a "fill" section on the last bar of each existing section for RP_STD_Fill parameter: introduce "fake" Section/SongParts with Fill_In_AA-like styles
     * parts
     * <p>
     * @param context SongStructure must not be linked to the ChordLeadSheet
     */
    private void preprocessFillParameter(SongContext context)
    {
        ChordLeadSheet cls = context.getSong().getChordLeadSheet();
        SongStructure ss = context.getSong().getSongStructure();
        try
        {
            // Add a "<sectionName>*FILL*" section and the corresponding songPart for all sections whose size >= 2 bars.
            // For section whose size=1, just rename section with "<sectionName>*FILL*"
            for (var section : cls.getItems(CLI_Section.class))
            {
                String fillSectionName = section.getData().getName() + "*FILL*";
                int sectionSize = cls.getBarRange(section).size();
                if (sectionSize == 1)
                {
                    cls.setSectionName(section, fillSectionName);
                    continue;
                }

                // Add a special "fill" section on last bar
                int fillSectionBar = section.getPosition().getBar() + sectionSize - 1;
                CLI_Section fillSection = CLI_Factory.getDefault().createSection(fillSectionName, section.getData().getTimeSignature(),
                        fillSectionBar, null);
                fillSection = cls.addSection(fillSection);


                // Update all impacted SongParts
                for (SongPart spt : context.getSongParts())
                {
                    if (spt.getParentSection() != section)
                    {
                        continue;
                    }
                    // Shorten the existing songpart
                    int startBar = spt.getStartBarIndex();
                    int nbBars = spt.getNbBars();
                    ss.resizeSongParts(Map.of(spt, nbBars - 1));

                    // Add the 1-bar SongPart
                    SongPart fillSpt = spt.getCopy(null, startBar + nbBars - 1, 1, fillSection);
                    ss.addSongParts(Arrays.asList(fillSpt));
                }
            }
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }


        // Change rpComplexity value for "fill" songParts depending on the rpFill value
        for (SongPart spt : context.getSongParts())
        {
            Rhythm r = spt.getRhythm();
            if (r.equals(rhythm) && spt.getParentSection().getData().getName().endsWith("*FILL*"))
            {
                YamJJazzRhythm yjr = (YamJJazzRhythm) r;
                String rpComplexityValue = spt.getRPValue(RP_SYS_Variation.getVariationRp(yjr));
                StylePart sp = yjr.getStylePart(rpComplexityValue);
                String rpFillValue = spt.getRPValue(RP_SYS_Fill.getFillRp(yjr));
                if (sp.getType().isMain() && RP_SYS_Fill.needFill(rpFillValue))
                {
                    // Change the RP Complexity value to use the appropriate Fill or Break
                    StylePartType breakType;
                    if (rpFillValue.equalsIgnoreCase("break"))
                    {
                        // Special case for the break value
                        breakType = StylePartType.Fill_In_BA;

                    } else
                    {
                        breakType = sp.getType().getFill();
                        assert breakType != null : "sp=" + sp;   //NOI18N
                    }
                    String newComplexityValue = "Fill In AA-1";    // Default, normally always present in all rhythms
                    if (rhythm.getStyle().getStylePart(breakType) != null)
                    {
                        newComplexityValue = breakType.toString() + "-1";
                    }
                    var rpVariation = RP_SYS_Variation.getVariationRp(yjr);
                    ss.setRhythmParameterValue(spt, rpVariation, newComplexityValue);
                }
            }
        }
    }


    /**
     * Change the velocity of notes depending on the Intensity parameter of each SongPart.
     *
     * @param chordSeqPhrases
     */
    private void processIntensityParameter(List<ChordSeqPhrases> chordSeqPhrases)
    {
        for (var chordSeqPhrase : chordSeqPhrases)
        {
            HashMap<AccType, Phrase> mapAccTypePhrase = chordSeqPhrase.mapAccTypePhrase();

            for (SongPart spt : contextWork.getSongParts())
            {
                FloatRange frg = contextWork.getSptBeatRange(spt);
                if (frg.isEmpty() || spt.getRhythm() != rhythm)
                {
                    continue;
                }

                int rpIntensityValue = spt.getRPValue(RP_SYS_Intensity.getIntensityRp(rhythm));
                int velShift = RP_SYS_Intensity.getRecommendedVelocityShift(rpIntensityValue);

                for (Phrase p : mapAccTypePhrase.values())
                {
                    p.processNotes(ne -> frg.contains(ne.getPositionInBeats(), true), ne -> 
                    {
                        int v = MidiConst.clamp(ne.getVelocity() + velShift);
                        NoteEvent newNe = ne.setVelocity(v, true);
                        return newNe;
                    });
                }
            }
        }
    }

    /**
     * Ensure the root/bass note is played at chord symbol change position.
     *
     * @param chordSeqPhrases
     */
    private void processBassLine(List<ChordSeqPhrases> chordSeqPhrases)
    {
        // Check each chord sequence
        for (var chordSeqPhrase : chordSeqPhrases)
        {
            var cSeq = chordSeqPhrase.simpleChordSequence();
            Phrase p = chordSeqPhrase.mapAccTypePhrase().get(AccType.BASS);
            if (p == null)
            {
                // Some rare rhythms don't use a bass
                continue;
            }
            float cSeqStartInBeats = contextWork.getSong().getSongStructure().toPositionInNaturalBeats(cSeq.getBarRange().from);


            // Check if there is a note played at each chord symbol position
            for (var cliCs : cSeq)
            {

                // Prepare data
                int bassRelPitch = cliCs.getData().getBassNote().getRelativePitch();
                float cliCsPosInBeats = cSeq.toPositionInBeats(cliCs.getPosition());


                // Get notes around chord symbol position
                float from = Math.max(cliCsPosInBeats - Grid.PRE_CELL_BEAT_WINDOW_DEFAULT, 0);
                float to = cliCsPosInBeats + 0.2f;
                var notes = p.getNotes(ne -> true, new FloatRange(from, to), true);

                // LOGGER.fine("processBassLine() cliCs=" + cliCs + " notes=" + notes);

                if (notes.isEmpty() // don't want to add a note and change the rhythm pattern
                        || notes.stream().anyMatch(ne -> ne.getRelativePitch() == bassRelPitch) // our bass note is already there
                        || cliCs.getPosition().isFirstBarBeat()) // don't mess with 1st beat notes, this is the style choice to not start on root/bass                        
                {
                    // Do nothing                       

                } else
                {
                    // Replace the pitch of the first note

                    NoteEvent ne = notes.get(0);
                    int newPitch = ne.getClosestPitch(bassRelPitch);
                    NoteEvent newNe = ne.setPitch(newPitch, true);
                    p.replace(ne, newNe);
                    LOGGER.log(LogLevel, "processBassLine()    => replacing {0} with {1}", new Object[]
                    {
                        ne, newNe
                    });
                }

            }

        }
    }

    private void processAnticipationsAndAccents(List<ChordSeqPhrases> chordSeqPhrases)
    {
        SongStructure ss = contextWork.getSong().getSongStructure();
        int nbCellsPerBeat = Grid.getRecommendedNbCellsPerBeat(rhythm.getTimeSignature(), rhythm.getFeatures().division().isSwing());

        LOGGER.fine("processAnticipationsAndAccents() --");


        for (var chordSeqPhrase : chordSeqPhrases)
        {
            var cSeq = chordSeqPhrase.simpleChordSequence();
            float cSeqStartInBeats = ss.toPositionInNaturalBeats(cSeq.getBarRange().from);


            AccentProcessor ap = new AccentProcessor(cSeq, nbCellsPerBeat, contextWork.getSong().getTempo(), Grid.PRE_CELL_BEAT_WINDOW_DEFAULT);
            AnticipatedChordProcessor acp = new AnticipatedChordProcessor(cSeq, nbCellsPerBeat, Grid.PRE_CELL_BEAT_WINDOW_DEFAULT);


            HashMap<AccType, Phrase> mapAtPhrase = chordSeqPhrase.mapAccTypePhrase();


            Phrase p = mapAtPhrase.get(AccType.RHYTHM);
            if (p != null)
            {
                LOGGER.log(LogLevel, "processAnticipationsAndAccents()  AccType.RHYTHM, channel={0}", p.getChannel());
                DrumKit kit = rhythm.getRhythmVoice(AccType.RHYTHM).getDrumKit();
                acp.anticipateChords_Drums(p, kit);
                ap.processAccentDrums(p, kit);
                ap.processHoldShotDrums(p, kit, HoldShotMode.NORMAL);
            }


            p = mapAtPhrase.get(AccType.BASS);
            if (p != null)
            {
                acp.anticipateChords_Mono(p);
                ap.processAccentBass(p);
                ap.processHoldShotMono(p, HoldShotMode.NORMAL);
            }


            p = mapAtPhrase.get(AccType.CHORD1);
            if (p != null)
            {
                acp.anticipateChords_Poly(p);
                ap.processAccentChord(p);
                ap.processHoldShotChord(p, HoldShotMode.NORMAL);
            }


            p = mapAtPhrase.get(AccType.CHORD2);
            if (p != null)
            {
                acp.anticipateChords_Poly(p);
                ap.processAccentChord(p);
                ap.processHoldShotChord(p, HoldShotMode.NORMAL);
            }


            p = mapAtPhrase.get(AccType.PAD);
            if (p != null)
            {
                acp.anticipateChords_Poly(p);
                ap.processHoldShotChord(p, HoldShotMode.EXTENDED);
            }


            p = mapAtPhrase.get(AccType.PHRASE1);
            if (p != null)
            {
                acp.anticipateChords_Mono(p);
                ap.processHoldShotMono(p, HoldShotMode.EXTENDED);
            }


            p = mapAtPhrase.get(AccType.PHRASE2);
            if (p != null)
            {
                acp.anticipateChords_Mono(p);
                ap.processHoldShotMono(p, HoldShotMode.EXTENDED);
            }
        }
    }

    /**
     * Merge all contiguous ChordSequences which are using our rhythm.
     * <p>
     *
     * @param chordSeqPhrases
     * @return
     */
    private List<ChordSeqPhrases> mergeChordSequences(List<ChordSeqPhrases> chordSeqPhrases)
    {
        Objects.requireNonNull(chordSeqPhrases);
        Preconditions.checkArgument(!chordSeqPhrases.isEmpty());


        List<ChordSeqPhrases> res = new ArrayList<>();
        int startBar = chordSeqPhrases.get(0).simpleChordSequence().getBarRange().from;
        int startIndex = 0;


        for (int i = 1; i <= chordSeqPhrases.size(); i++)
        {
            SimpleChordSequence cSeq = i < chordSeqPhrases.size() ? chordSeqPhrases.get(i).simpleChordSequence()
                    : chordSeqPhrases.get(i - 1).simpleChordSequence();  // cSeq fake value on last iteration (it must not be null)
            SimpleChordSequence prevSeq = chordSeqPhrases.get(i - 1).simpleChordSequence();
            int prevSeqLastBar = prevSeq.getBarRange().from + prevSeq.getBarRange().size() - 1;


            if (i == chordSeqPhrases.size() || cSeq.getBarRange().from != prevSeqLastBar + 1)
            {
                // We finished the loop, or cSeq is not contiguous : create a longer ChordSequence with a new AccType/Phrase map
                int nbBars = prevSeqLastBar - startBar + 1;
                float startBeatPos = contextWork.getSong().getSongStructure().toPositionInNaturalBeats(startBar);
                SimpleChordSequence newSeq = new SimpleChordSequence(new IntRange(startBar, startBar + nbBars - 1), startBeatPos, rhythm.getTimeSignature());
                HashMap<AccType, Phrase> newMap = new HashMap<>();
                for (int j = startIndex; j < i; j++)
                {
                    // Merge each ChordSequence into newSeq
                    SimpleChordSequence cSeqj = chordSeqPhrases.get(j).simpleChordSequence();
                    newSeq.addAll(cSeqj);

                    // Merge its AccType phrases into newMap
                    HashMap<AccType, Phrase> mapj = chordSeqPhrases.get(j).mapAccTypePhrase();
                    for (AccType at : mapj.keySet())
                    {
                        Phrase pj = mapj.get(at);
                        Phrase pAt = newMap.get(at);
                        if (pAt == null)
                        {
                            pAt = new Phrase(pj.getChannel(), at.isDrums());
                            newMap.put(at, pAt);
                        }
                        pAt.add(pj);
                    }
                }

                // Save the result
                ChordSeqPhrases csp = new ChordSeqPhrases(newSeq, newMap);
                res.add(csp);
                startBar = cSeq.getBarRange().from;
                startIndex = i;
            }
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
        int destChannel = contextWork.getMidiMix().getChannel(myRv);
        return destChannel;
    }

    /**
     * Remap notes of pSrc to match destKeyMap.
     *
     * @param pSrc
     * @param srcKeyMap  The keymap used by pSrc
     * @param destKeyMap The target keymap
     */
    private void remapDrumNotes(SourcePhrase pSrc, DrumKit.KeyMap srcKeyMap, DrumKit.KeyMap destKeyMap)
    {
        if (!ENABLE_DRUM_KEY_MAPPING || srcKeyMap == destKeyMap || destKeyMap.isContaining(srcKeyMap))
        {
            // Easy: nothing to do
            return;
        }
        if (StandardKeyMapConverter.accept(srcKeyMap, destKeyMap))
        {
            Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
            for (var ne : pSrc.toArray(NoteEvent[]::new))
            {
                int oldPitch = ne.getPitch();
                int newPitch = StandardKeyMapConverter.convertKey(srcKeyMap, oldPitch, destKeyMap);
                if (newPitch != -1 && newPitch != oldPitch)
                {
                    NoteEvent newNe = ne.setPitch(newPitch, true);
                    mapOldNew.put(ne, newNe);
                    LOGGER.log(LogLevel, "remapDrumNotes() pitch replaced {0} ==> {1}", new Object[]
                    {
                        oldPitch, newPitch
                    });
                }
            }
            pSrc.replaceAll(mapOldNew, false);
        }
    }


    // =====================================================================================================================
    // Inner classes
    // =====================================================================================================================
}
