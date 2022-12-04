/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3)
 *  as published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 *
 *  Contributor(s):
 */
package org.jjazz.rhythmmusicgeneration.api;

import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.NCExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransformValue;
import org.jjazz.phrasetransform.api.rps.RP_SYS_PhraseTransform;
import org.jjazz.phrasetransform.api.rps.RP_SYS_PhraseTransformValue;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhraseValue;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Mute;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.songcontext.api.SongPartContext;
import org.netbeans.api.progress.BaseProgressUtils;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.ResUtil;

/**
 * Methods to convert a Song into Phrases and Midi sequence.
 * <p>
 */
public class SongSequenceBuilder
{

    /**
     * The return value of the buildSongSequence() methods.
     */
    static public class SongSequence
    {

        public Sequence sequence;
        public Map<RhythmVoice, Integer> mapRvTrackId;
        public Map<RhythmVoice, Phrase> mapRvPhrase;
    }


    private SongContext songContext;

    private static final Logger LOGGER = Logger.getLogger(SongSequenceBuilder.class.getSimpleName());

    /**
     * @param context The songContext to build the sequence. Song's SongStructure can not be empty.
     */
    public SongSequenceBuilder(SongContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");   //NOI18N
        }
        this.songContext = context;
        assert !context.getSong().getSongStructure().getSongParts().isEmpty();   //NOI18N
    }

    /**
     * Built the Midi track name from the specified parameters.
     *
     * @param rv
     * @param channel
     * @return
     */
    static public String buildTrackName(RhythmVoice rv, int channel)
    {
        // First event will be the name of the track: rhythm - rhythmVoice - channel
        String name = rv.getContainer().getName() + "-" + rv.getName() + "-Channel" + (channel + 1) + " [1-16]";
        return name;
    }

    /**
     * Call buildMapRvPhrase() then buildSongSequence().
     *
     * @param silent If true do not show a progress dialog
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     * @see #buildSongSequence(java.util.Map, boolean)
     * @see #buildMapRvPhrase(boolean)
     */
    public SongSequence buildAll(boolean silent) throws MusicGenerationException
    {
        AllBuilderTask task = new AllBuilderTask();
        if (silent)
        {
            task.run();
        } else
        {
            BaseProgressUtils.showProgressDialogAndRun(task, ResUtil.getString(getClass(), "PREPARING MUSIC"));
        }

        if (task.musicException != null)
        {
            throw task.musicException;
        }

        return task.songSequence;
    }


    /**
     * Build the RhythmVoice phrases for the defined context.
     * <p>
     * - Perform some checks on the context (start chord on each section start, no overlapping chord symbols) <br>
     * - Ask each used rhythm in the song to produce music (one Phrase per RhythmVoice) via its MusicGenerator implementation.<br>
     * - Add the user phrases if any<br>
     * - Apply on each channel possible instrument transpositions, velocity shift, mute (RP_SYS_Mute).<br>
     * - Apply the RP_SYS_DrumsMix velocity changes. Note that it is expected that, if there is an AdaptedRhythm for a Rhythm
     * which uses RP_SYS_DrumsMix, the AdaptedRhythm reuses the same RP_SYS_DrumsMix instance.<br>
     * - Apply the RP_SYS_CustomPhrase changes<br>
     * - Apply the RP_SYS_PhraseTransform changes<br>
     * - Apply drums rerouting if needed <br>
     * - Handle the NC chord symbols<br>
     * <p>
     * Phrases for RhythmVoiceDelegates are merged into the phrases of the source RhythmVoices.
     *
     * @param silent If true do not show a progress dialog
     * @return The returned phrases always start at beat/bar 0 (i.e phrases are shifted if context start bar is not bar 0).
     * @throws MusicGenerationException
     */
    public Map<RhythmVoice, Phrase> buildMapRvPhrase(boolean silent) throws MusicGenerationException
    {
        RvPhrasesBuilderTask task = new RvPhrasesBuilderTask();
        if (silent)
        {
            task.run();
        } else
        {
            BaseProgressUtils.showProgressDialogAndRun(task, ResUtil.getString(getClass(), "PREPARING MUSIC"));
        }

        if (task.musicException != null)
        {
            throw task.musicException;
        }

        return task.rvPhrases;
    }


    /**
     * Build the SongSequence from the specified RhythmVoice phrases for the defined context.
     * <p>
     * - Create a track 0 with no notes but MidiEvents for song name, time signature changes, CTRL_CHG_JJAZZ_TEMPO_FACTOR
     * controller messages based on the RP_SYS_TempoFactor value (if used by a rhythm). <br>
     * - Then create a track per RhythmVoice.
     * <p>
     * If songContext range start bar is &gt; 0, the Midi events are shifted to start at sequence tick 0.
     *
     * @param rvPhrases The RhythmVoice phrases such as produced by buildMapRvPhrase(boolean), must start at beat 0.
     * @param silent    If true do not show a progress dialog
     * @return A Sequence containing accompaniment tracks for the songContext, including time signature change Midi meta events
     *         and JJazz custom Midi controller messages (MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR) for tempo factor changes.
     * @throws MusicGenerationException
     * @see #buildMapRvPhrase(boolean)
     */
    public SongSequence buildSongSequence(Map<RhythmVoice, Phrase> rvPhrases, boolean silent) throws MusicGenerationException
    {
        SongSequenceBuilderTask task = new SongSequenceBuilderTask(rvPhrases);
        if (silent)
        {
            task.run();
        } else
        {
            BaseProgressUtils.showProgressDialogAndRun(task, ResUtil.getString(getClass(), "PREPARING MUSIC"));
        }

        if (task.musicException != null)
        {
            throw task.musicException;
        }

        return task.songSequence;
    }

    /**
     * Create a sequence (for the current SongContext) ready to be exported and read by an external sequencer.
     * <p>
     * Get rid of all JJazzLab-only MidiEvents and add some initialization events:<br>
     * - add copyright message<br>
     * - remove JJazzLab-specific tempo factor and beat <br>
     * - add tempo events<br>
     * - add GM/GS/XG/GM2 reset messages depending of the current OutputSynth configuration<br>
     * - add reset controllers for each RhythmVoice track<br>
     * - add bank/program and volume/effects events for each RhythmVoice track<br>
     * - add a marker for each chord symbol<br>
     *
     *
     * @param silent
     * @param ignoreMidiMixMute If true, a track will sound even if it was muted in the context MidiMix
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    public SongSequence buildExportableSequence(boolean silent, boolean ignoreMidiMixMute) throws MusicGenerationException
    {

        var songSequence = buildAll(silent);     // throws MusicGenerationException


        Sequence sequence = songSequence.sequence;
        MidiMix midiMix = songContext.getMidiMix();
        Track[] tracks = sequence.getTracks();
        Track track0 = tracks[0];


        if (!MidiUtilities.checkMidiFileTypeSupport(sequence, 1, true))
        {
            throw new IllegalStateException("This Java Sequence implementation does not support Midi File Type 1");
        }


        // ========== Track 0 settings =============
        // Copyright
        MidiMessage mmCopyright = MidiUtilities.getCopyrightMetaMessage("JJazzLab Midi Export file");
        MidiEvent me = new MidiEvent(mmCopyright, 0);
        track0.add(me);


        // Clean track0 from JJazzLab internal Midi events
        int i = 0;
        while (i < track0.size())
        {
            me = track0.get(i);
            MidiMessage mm = me.getMessage();
            if (mm instanceof ShortMessage)
            {
                ShortMessage sm = (ShortMessage) mm;
                if (sm.getCommand() == ShortMessage.CONTROL_CHANGE && sm.getData1() == MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR)
                {
                    track0.remove(me);
                    i--;
                }
            }
            i++;
        }


        // Add markers at each chord symbol position
        SongStructure ss = songContext.getSong().getSongStructure();
        for (SongPart spt : songContext.getSongParts())
        {
            CLI_Section section = spt.getParentSection();
            for (CLI_ChordSymbol cliCs : songContext.getSong().getChordLeadSheet().getItems(section, CLI_ChordSymbol.class))
            {

                Position absPos = ss.getSptItemPosition(spt, cliCs);
                long tickPos = songContext.getRelativeTick(absPos);
                me = new MidiEvent(MidiUtilities.getMarkerMetaMessage(cliCs.getData().getName()), tickPos);
                track0.add(me);
            }
        }


        // Add initial tempo event
        int tempo = songContext.getSong().getTempo();
        SongPart spt0 = songContext.getSongParts().get(0);
        RP_SYS_TempoFactor rp = RP_SYS_TempoFactor.getTempoFactorRp(spt0.getRhythm());
        int tempoFactor = -1;
        if (rp != null)
        {
            tempoFactor = spt0.getRPValue(rp);
            tempo = Math.round(tempoFactor / 100f * tempo);
        }
        me = new MidiEvent(MidiUtilities.getTempoMessage(0, tempo), 0);
        track0.add(me);


        // Add additional song part tempo changes
        int lastTempoFactor = tempoFactor;
        var spts = songContext.getSongParts();
        for (i = 1; i < spts.size(); i++)
        {
            SongPart spt = spts.get(i);
            rp = RP_SYS_TempoFactor.getTempoFactorRp(spt.getRhythm());
            if (rp != null)
            {
                tempoFactor = spt.getRPValue(rp);
                if (tempoFactor != lastTempoFactor)
                {
                    tempo = Math.round(tempoFactor / 100f * songContext.getSong().getTempo());
                    float beatPos = songContext.getSptBeatRange(spt).from - songContext.getBeatRange().from;
                    long tickPos = Math.round(beatPos * MidiConst.PPQ_RESOLUTION);
                    me = new MidiEvent(MidiUtilities.getTempoMessage(0, tempo), tickPos);
                    track0.add(me);
                    lastTempoFactor = tempoFactor;
                }
            }
        }


        // Add XX mode ON initialization message
        OutputSynth os = OutputSynthManager.getInstance().getDefaultOutputSynth();
        SysexMessage sxm = null;
        switch (os.getUserSettings().getSendModeOnUponPlay())
        {
            case GM:
                sxm = MidiUtilities.getGmModeOnSysExMessage();
                break;
            case GM2:
                sxm = MidiUtilities.getGm2ModeOnSysExMessage();
                break;
            case GS:
                sxm = MidiUtilities.getGsModeOnSysExMessage();
                break;
            case XG:
                sxm = MidiUtilities.getXgModeOnSysExMessage();
                break;
            default:
            // Nothing
        }
        if (sxm != null)
        {
            me = new MidiEvent(sxm, 0);
            track0.add(me);
        }

        // ========== RhythmVoice tracks settings =============

        // Remove elements from muted tracks (don't remove the muted tracks because it would impact mapRvTrack)
        if (!ignoreMidiMixMute)
        {
            for (RhythmVoice rv : midiMix.getRhythmVoices())
            {
                if (midiMix.getInstrumentMixFromKey(rv).isMute())
                {
                    Track track = sequence.getTracks()[songSequence.mapRvTrackId.get(rv)];
                    MidiUtilities.clearTrack(track);
                }
            }
        }


        // For each RhythmVoice :
        // - reset all controllers
        // - add instruments initialization messages for each track
        for (RhythmVoice rv : songSequence.mapRvTrackId.keySet())
        {
            Track track = tracks[songSequence.mapRvTrackId.get(rv)];
            int channel = songContext.getMidiMix().getChannel(rv);

            // Reset all controllers
            MidiMessage mmReset = MidiUtilities.getResetAllControllersMessage(channel);
            me = new MidiEvent(mmReset, 0);
            track.add(me);

            // Instrument + volume + pan etc.
            InstrumentMix insMix = midiMix.getInstrumentMixFromKey(rv);
            for (MidiMessage mm : insMix.getAllMidiMessages(channel))
            {
                me = new MidiEvent(mm, 0);
                track.add(me);
            }
        }

        return songSequence;
    }


    public SongContext getSongContext()
    {
        return songContext;
    }

    @Override
    public String toString()
    {
        return "MidiSequenceBuilder context=" + songContext.toString();
    }

    // =========================================================================
    // Private methods
    // =========================================================================
    private Map<RhythmVoice, Phrase> buildMapRvPhrase() throws MusicGenerationException
    {
        Map<RhythmVoice, Phrase> res = new HashMap<>();

        checkEmptyRange(songContext);       // throws MusicGenerationException

        // Check that there is a valid starting chord at the beginning on each section
        checkStartChordPresence(songContext);      // throws MusicGenerationException

        // Check there is no 2 chords at same position
        checkChordsAtSamePosition(songContext);            // throws MusicGenerationException        


        for (Rhythm r : songContext.getUniqueRhythms())
        {

            // Generate the phrase
            Map<RhythmVoice, Phrase> rMap = generateRhythmPhrases(r);                       // Possible MusicGenerationException here

            if (songContext.getUniqueRhythms().size() > 1)
            {
                checkRhythmPhrasesScope(songContext, r, rMap);                              // Possible MusicGenerationException here
            }

            // Merge into the final result
            res.putAll(rMap);

        }


        // Handle the RP_SYS_CustomPhrase changes
        processCustomPhrases(songContext, res);

        // Handle the RP_SYS_DrumsTransform changes
        processDrumsTransforms(songContext, res);

        // Handle the RP_SYS_PhraseTransform changes
        processPhraseTransforms(songContext, res);

        // Handle muted instruments via the SongPart's RP_SYS_Mute parameter
        processMutedInstruments(songContext, res);

        // Handle the NC chord symbols 
        processNoChords(songContext, res);


        // Merge the phrases from delegate RhythmVoices to the source phrase, then remove the delegate phrases        
        for (var rv : res.keySet().toArray(new RhythmVoice[0]))
        {
            if (rv instanceof RhythmVoiceDelegate)
            {
                RhythmVoiceDelegate rvd = (RhythmVoiceDelegate) rv;
                Phrase p = res.get(rvd);
                RhythmVoice rvds = rvd.getSource();
                Phrase pDest = res.get(rvds);
                if (pDest == null)
                {
                    // Might happen if the context range contains only SongParts with AdaptedRhythms (source rhythms are excluded)
                    pDest = new Phrase(p.getChannel());
                    res.put(rvds, pDest);
                }

                // There should be no overlap of phrases since the delegate is from a different rhythm, so for different song parts 
                pDest.add(p);

                // Remove the delegate phrase
                res.remove(rvd);
            }
        }

        // 
        // From here no more SongPart-based processing allowed, since the phrases for SongParts using an AdaptedRhythm have 
        // been merged into the tracks of its source rhythm.
        // 

        // Add the user phrases
        var br = songContext.getBeatRange();
        for (String userPhraseName : songContext.getSong().getUserPhraseNames())
        {
            UserRhythmVoice urv = songContext.getMidiMix().getUserRhythmVoice(userPhraseName);
            assert urv != null : "userPhraseName=" + userPhraseName + " songContext.getMidiMix()=" + songContext.getMidiMix();


            // Create the phrase on the right Midi channel
            int channel = songContext.getMidiMix().getChannel(urv);
            Phrase p = new Phrase(channel);
            p.add(songContext.getSong().getUserPhrase(userPhraseName));


            // Adapt the phrase to the current context
            p = p.getSlice(br, false, 1, 0.1f);

            LOGGER.log(Level.FINE, "buildMapRvPhrase() Adding user phrase for name={0} p={1}", new Object[]
            {
                userPhraseName, p
            });

            res.put(urv, p);
        }


        // Handle instrument settings which impact the phrases: transposition, velocity shift, ...
        processInstrumentsSettings(songContext, res);


        // Process the drums rerouting
        processDrumsRerouting(songContext, res);


        // Shift phrases to start at position 0
        for (Phrase p : res.values())
        {
            p.shiftEvents(-songContext.getBeatRange().from);
        }

        return res;
    }

    private SongSequence buildSongSequence(Map<RhythmVoice, Phrase> rvPhrases) throws MusicGenerationException
    {
        SongSequence res = new SongSequence();
        res.mapRvPhrase = new HashMap<>(rvPhrases);

        try
        {
            res.sequence = new Sequence(Sequence.PPQ, MidiConst.PPQ_RESOLUTION);
        } catch (InvalidMidiDataException ex)
        {
            throw new MusicGenerationException("buildSequence() Can't create the initial empty sequence : " + ex.getLocalizedMessage());
        }


        // First track is really useful only when exporting to Midi file type 1            
        // Contain song name, tempo factor changes, time signatures
        Track track0 = res.sequence.createTrack();
        MidiUtilities.addTrackNameEvent(track0, songContext.getSong().getName() + " (JJazzLab song)");
        addTimeSignatureChanges(songContext, track0);
        addTempoFactorChanges(songContext, track0);


        // Other tracks : create one per RhythmVoice
        int trackId = 1;
        res.mapRvTrackId = new HashMap<>();


        // Normally process only normal rhythms, but if context does not use a source rhythm of an adapted rhythm, we need to process it too
        var contextRhythms = songContext.getUniqueRhythms();      // Contains AdaptedRhythms
        Set<Rhythm> targetRhythms = new HashSet<>();
        for (Rhythm r : contextRhythms)
        {
            if (r instanceof AdaptedRhythm)
            {
                AdaptedRhythm ar = (AdaptedRhythm) r;
                Rhythm sr = ar.getSourceRhythm();
                if (!contextRhythms.contains(sr))
                {
                    // Add the source rhythm if not present in context range
                    targetRhythms.add(sr);
                }
            } else
            {
                targetRhythms.add(r);
            }
        }


        // The final RhythmVoices to process: all targetRhythms + UserRhythmVoices
        final List<RhythmVoice> targetRhythmVoices = new ArrayList<>();
        targetRhythms.forEach(r -> targetRhythmVoices.addAll(r.getRhythmVoices()));
        rvPhrases.keySet().stream()
                .filter(rv -> rv instanceof UserRhythmVoice)
                .forEach(rv -> targetRhythmVoices.add(rv));


        // Create the tracks
        for (RhythmVoice rv : targetRhythmVoices)
        {

            Track track = res.sequence.createTrack();
            int channel = songContext.getMidiMix().getChannel(rv);

            String name = buildTrackName(rv, channel);
            MidiUtilities.addTrackNameEvent(track, name);

            // Fill the track
            Phrase p = rvPhrases.get(rv);
            p.fillTrack(track);

            // Store the track with the RhythmVoice
            res.mapRvTrackId.put(rv, trackId);
            trackId++;
        }

        fixEndOfTracks(songContext, res.sequence);

        return res;
    }


    /**
     * Ask specified rhythm to generate music.
     *
     * @param r
     */
    private Map<RhythmVoice, Phrase> generateRhythmPhrases(Rhythm r) throws MusicGenerationException
    {
        if (r instanceof MusicGenerator)
        {
            LOGGER.log(Level.FINE, "fillRhythmTracks() calling generateMusic() for rhythm r={0} hashCode(r)={1}", new Object[]
            {
                r.getName(), Objects.hashCode(r)
            });
            r.loadResources();
            return ((MusicGenerator) r).generateMusic(songContext);
        } else
        {
            LOGGER.warning("generateRhythmPhrases() r=" + r + " is not a MusicGenerator instance");
            throw new MusicGenerationException("Rhythm " + r.getName() + " is not able to generate music");
        }
    }

    /**
     * Check that there is a starting chord symbol for each section used in the specified context.
     *
     * @param context
     * @throws UserErrorException
     */
    private void checkStartChordPresence(SongContext context) throws UserErrorGenerationException
    {
        ChordLeadSheet cls = context.getSong().getChordLeadSheet();
        for (CLI_Section section : getContextSections(context))
        {
            Position pos = section.getPosition();
            List<? extends CLI_ChordSymbol> clis = cls.getItems(section, CLI_ChordSymbol.class);
            if (clis.isEmpty() || !clis.get(0).getPosition().equals(pos))
            {
                throw new UserErrorGenerationException(ResUtil.getString(getClass(), "ERR_MissingChordSymbolAtSection", section.getData().getName(), (pos.getBar() + 1)));
            }
        }
    }

    /**
     * Check if the ChordLeadSheet contains 2 chord symbols at the same position in the passed context.
     *
     * @param context
     * @throws org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder.UserErrorException
     */
    private void checkChordsAtSamePosition(SongContext context) throws UserErrorGenerationException
    {
        HashMap<Position, CLI_ChordSymbol> mapPosCs = new HashMap<>();
        ChordLeadSheet cls = context.getSong().getChordLeadSheet();

        for (CLI_Section cliSection : getContextSections(context))
        {
            List<? extends CLI_ChordSymbol> clis = cls.getItems(cliSection, CLI_ChordSymbol.class);
            for (CLI_ChordSymbol cliCs : clis)
            {
                Position pos = cliCs.getPosition();
                CLI_ChordSymbol existingCliCs = mapPosCs.get(pos);
                if (existingCliCs != null)
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append(ResUtil.getString(getClass(), "ERR_ChordSymbolPositionConflict"));
                    sb.append(cliCs.getData().toString()).append(cliCs.getPosition().toUserString());
                    sb.append(" - ");
                    sb.append(existingCliCs.getData().toString()).append(existingCliCs.getPosition().toUserString());
                    throw new UserErrorGenerationException(sb.toString());
                } else
                {
                    mapPosCs.put(pos, cliCs);
                }
            }
        }
    }

    /**
     * Get the set of sections for the given context.
     *
     * @param context
     * @return
     */
    private Set<CLI_Section> getContextSections(SongContext context)
    {
        Set<CLI_Section> res = context.getSong().getSongStructure().getSongParts().stream()
                .map(spt -> spt.getParentSection())
                .collect(Collectors.toSet());
        return res;
    }

    /**
     * For each SongPart remove notes for muted RhythmVoices depending on the RP_SYS_Mute value.<br>
     *
     * @param context
     * @param rvPhrases Keys can include RhythmVoiceDelegates
     */
    private void processMutedInstruments(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        for (SongPart spt : context.getSongParts())
        {
            Rhythm r = spt.getRhythm();
            RP_SYS_Mute rpMute = RP_SYS_Mute.getMuteRp(r);
            if (rpMute == null)
            {
                continue;
            }


            Set<String> muteValues = spt.getRPValue(rpMute);
            if (muteValues.isEmpty())
            {
                // There is a MuteRp but nothing is muted, 
                continue;
            }


            // At least one RhythmVoice/Track is muted
            FloatRange sptRange = context.getSptBeatRange(spt);
            List<RhythmVoice> mutedRvs = RP_SYS_Mute.getMutedRhythmVoices(r, muteValues);
            for (RhythmVoice rv : mutedRvs)
            {
                Phrase p = rvPhrases.get(rv);
                if (p == null)
                {
                    LOGGER.warning("muteNotes() Unexpected null phase. rv=" + rv + " rvPhrases=" + rvPhrases);   //NOI18N
                    continue;
                }
                p.silence(sptRange, true, false, 0.1f);
            }
        }
    }

    /**
     * Remove notes for NC chord symbols, ie NCExtChordSymbol instances.
     *
     * @param context
     * @param rvPhrases Keys can include RhythmVoiceDelegates
     */
    private void processNoChords(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        var songChordSequence = new SongChordSequence(context.getSong(), context.getBarRange());
        SongStructure ss = context.getSong().getSongStructure();

        for (int i = 0; i < songChordSequence.size(); i++)
        {
            var cliCs = songChordSequence.get(i);
            var pos = cliCs.getPosition();
            if (cliCs.getData() instanceof NCExtChordSymbol ncecs)
            {
                float posInBeats = ss.getPositionInNaturalBeats(pos.getBar()) + pos.getBeat();
                TimeSignature ts = ss.getSongPart(pos.getBar()).getRhythm().getTimeSignature();
                float chordDuration = songChordSequence.getChordDuration(i, ts);
                FloatRange beatRange = new FloatRange(posInBeats, posInBeats + chordDuration - 0.1f);

                for (Phrase p : rvPhrases.values())
                {
                    p.silence(beatRange, true, false, 0.01f);
                }
            }
        }

    }

    /**
     * Replace phrases by custom phrases depending on the RP_SYS_CustomPhrase value.
     *
     * @param context
     * @param rvPhrases Keys can include RhythmVoiceDelegates
     */
    private void processCustomPhrases(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        for (SongPart spt : context.getSongParts())
        {
            FloatRange sptBeatRange = context.getSptBeatRange(spt);


            // Get the RhythmParameter
            Rhythm r = spt.getRhythm();
            RP_SYS_CustomPhrase rpCustomPhrase = RP_SYS_CustomPhrase.getCustomPhraseRp(r);
            if (rpCustomPhrase == null)
            {
                continue;
            }


            // Get the RP value and process each customized phrase
            RP_SYS_CustomPhraseValue rpValue = spt.getRPValue(rpCustomPhrase);
            for (RhythmVoice rv : rpValue.getCustomizedRhythmVoices())
            {

                // Remove a slice for the current songpart            
                Phrase p = rvPhrases.get(rv);
                p.silence(sptBeatRange, true, false, 0.1f);


                // Get the custom phrase, starts at beat 0
                SizedPhrase spCustom = rpValue.getCustomizedPhrase(rv);
                float sizeInBeats = spCustom.getBeatRange().size();
                TimeSignature ts = spCustom.getTimeSignature();


                // If custom phrase is at least one bar shorter than current song part, duplicate the custom phrase to fill the remaining space
                if (sizeInBeats <= sptBeatRange.size() - ts.getNbNaturalBeats())
                {
                    float offset = sizeInBeats;
                    List<NoteEvent> toAdd = new ArrayList<>();
                    while (offset < sptBeatRange.size())
                    {
                        for (NoteEvent ne : spCustom)
                        {
                            float newPosInBeats = ne.getPositionInBeats() + offset;
                            if (newPosInBeats >= sptBeatRange.size())
                            {
                                break;
                            }
                            toAdd.add(new NoteEvent(ne, ne.getDurationInBeats(), newPosInBeats));
                        }
                        offset += sizeInBeats;
                    }

                    toAdd.forEach(ne -> spCustom.add(ne));  // No need for addOrdered() here
                }


                // Make sure it's not too long 
                spCustom.silenceAfter(sptBeatRange.size());


                // Shift to fit the current song part position
                spCustom.shiftEvents(sptBeatRange.from);

                // Update the current phrase
                p.add(spCustom);
            }
        }
    }

    /**
     * Transform rhythm phrases depending on the RP_SYS_PhraseTransform value.
     *
     * @param context
     * @param rvPhrases Keys can include RhythmVoiceDelegates
     */
    private void processPhraseTransforms(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        LOGGER.log(Level.FINE, "processPhraseTransforms() -- context={0}", context);
        for (SongPart spt : context.getSongParts())
        {
            FloatRange sptBeatRange = context.getSptBeatRange(spt);     // Might be smaller than spt.getBeatRange()
            IntRange sptBarRange = context.getSptBarRange(spt);         // Might be smaller than spt.getBarRange()
            SongPartContext sptContext = new SongPartContext(context.getSong(), context.getMidiMix(), sptBarRange);

            // Get the RhythmParameter
            Rhythm r = spt.getRhythm();
            RP_SYS_PhraseTransform rpCustomPhrase = RP_SYS_PhraseTransform.getPhraseTransformRp(r);
            if (rpCustomPhrase == null)
            {
                continue;
            }


            // Get the RP value and transform phrases as needed
            RP_SYS_PhraseTransformValue rpValue = spt.getRPValue(rpCustomPhrase);
            LOGGER.log(Level.FINE, "processPhraseTransforms() rpValue={0}", rpValue);
            for (RhythmVoice rv : rpValue.getChainRhythmVoices())
            {

                // The original phrase
                Phrase p = rvPhrases.get(rv);

                // Make it a SizedPhrase on the relevant beat range and transform it
                SizedPhrase inSp = new SizedPhrase(p.getChannel(), sptBeatRange, r.getTimeSignature());
                inSp.add(p.getSlice(sptBeatRange, false, 1, 0.1f));
                var chain = rpValue.getTransformChain(rv);
                var outSp = chain.transform(inSp, sptContext);


                // Replace the old song part phrase by the transformed one
                p.silence(sptBeatRange, true, false, 0.1f);
                p.add(outSp);
            }
        }
    }

    /**
     * Transform the drums phrase depending on the RP_SYS_DrumsTransform value.
     *
     * @param context
     * @param rvPhrases Keys can include RhythmVoiceDelegates
     */
    private void processDrumsTransforms(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        LOGGER.log(Level.FINE, "processDrumsTransforms() -- context={0}", context);


        for (SongPart spt : context.getSongParts())
        {
            FloatRange sptBeatRange = context.getSptBeatRange(spt);     // Might be smaller than spt.getBeatRange()
            IntRange sptBarRange = context.getSptBarRange(spt);         // Might be smaller than spt.getBarRange()
            SongPartContext sptContext = new SongPartContext(context.getSong(), context.getMidiMix(), sptBarRange);


            // Get the RhythmParameter
            Rhythm r = spt.getRhythm();
            RP_SYS_DrumsTransform rpDrumsTransform = RP_SYS_DrumsTransform.getDrumsTransformRp(r);
            if (rpDrumsTransform == null)
            {
                continue;
            }


            // Get the RP value and transform phrases as needed
            RP_SYS_DrumsTransformValue rpValue = spt.getRPValue(rpDrumsTransform);
            RhythmVoice rvDrums = rpValue.getRhythmVoice();
            LOGGER.log(Level.FINE, "processDrumsTransforms() rpValue={0} rvDrums={1}", new Object[]
            {
                rpValue, rvDrums
            });


            // The phrase to modify
            Phrase p = rvPhrases.get(rvDrums);

            // Make it a SizedPhrase only on the relevant slice and transform it
            SizedPhrase inSp = new SizedPhrase(p.getChannel(), sptBeatRange, r.getTimeSignature());
            inSp.add(p.getSlice(sptBeatRange, false, 1, 0.1f));
            var chain = rpValue.getTransformChain(false);
            var outSp = chain.transform(inSp, sptContext);


            // Replace the old song part phrase by the transformed one            
            p.silence(sptBeatRange, true, false, 0.1f);
            p.add(outSp);
        }
    }

    /**
     * Apply transposition/velocity offset to match the InstrumentSettings of each RhythmVoice.
     *
     * @param rvPhrases
     */
    private void processInstrumentsSettings(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        LOGGER.fine("processInstrumentsSettings() -- ");   //NOI18N
        MidiMix midiMix = context.getMidiMix();
        for (RhythmVoice rv : rvPhrases.keySet())
        {
            Phrase p = rvPhrases.get(rv);
            InstrumentMix insMix = midiMix.getInstrumentMixFromKey(rv);
            if (insMix == null)
            {
                LOGGER.warning("applyInstrumentsSettings() Unexpected null InstrumentMix for rv=" + rv + " midMix=" + midiMix);   //NOI18N
                continue;
            }
            InstrumentSettings insSet = insMix.getSettings();
            if (insSet.getTransposition() != 0)
            {
                p.processPitch(pitch -> pitch + insSet.getTransposition());
                LOGGER.fine("processInstrumentsSettings()    Adjusting transposition=" + insSet.getTransposition() + " for rv=" + rv);   //NOI18N
            }
            if (insSet.getVelocityShift() != 0)
            {
                p.processVelocity(v -> v + insSet.getVelocityShift());
                LOGGER.fine("processInstrumentsSettings()    Adjusting velocity=" + insSet.getVelocityShift() + " for rv=" + rv);   //NOI18N
            }
        }
    }

    /**
     * Move rerouted channels phrase content into the GM Drums chanel phrase content.
     *
     * @param context
     * @param rvPhrases
     */
    private void processDrumsRerouting(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        LOGGER.fine("processDrumsRerouting() -- ");   //NOI18N

        MidiMix midiMix = context.getMidiMix();
        var reroutedChannels = midiMix.getDrumsReroutedChannels();


        // Get the target Drums RhythmVoice
        var rvDrums = midiMix.getRhythmVoice(MidiConst.CHANNEL_DRUMS);
        if (rvDrums == null || !rvDrums.isDrums())
        {
            // Search for another RhythmVoice, maybe it's not on standard channel
            rvDrums = midiMix.getRhythmVoices().stream()
                    .filter(rv -> rv.isDrums() && !reroutedChannels.contains(midiMix.getChannel(rv)))
                    .findAny()
                    .orElse(null);
            if (rvDrums == null)
            {
                LOGGER.warning("processDrumsRerouting() No available target drums channel found in MidiMix=" + midiMix);
                return;
            }
        }


        Phrase pDrums = rvPhrases.get(rvDrums);
        for (int channel : midiMix.getDrumsReroutedChannels())
        {
            RhythmVoice rv = midiMix.getRhythmVoice(channel);
            Phrase reroutedPhrase = rvPhrases.get(rv);
            pDrums.add(reroutedPhrase);
            reroutedPhrase.clear();
        }

    }

    private void checkEmptyRange(SongContext context) throws UserErrorGenerationException
    {
        if (context.getBarRange().isEmpty())
        {
            throw new UserErrorGenerationException(ResUtil.getString(getClass(), "ERR_NothingToPlay"));
        }
    }

    /**
     * Check that rvPhrases contain music notes only for the relevant bars of rhythm r.
     *
     * @param r
     * @param rvPhrases
     * @throws MusicGenerationException
     */
    private void checkRhythmPhrasesScope(SongContext context, Rhythm r, Map<RhythmVoice, Phrase> rvPhrases) throws MusicGenerationException
    {
        // Get the bar ranges used by r
        List<FloatRange> sptRanges = new ArrayList<>();
        for (SongPart spt : context.getSongParts())
        {
            if (spt.getRhythm() == r)
            {
                FloatRange rg = context.getSptBeatRange(spt);
                if (!sptRanges.isEmpty() && sptRanges.get(sptRanges.size() - 1).to == rg.from)
                {
                    // Extend previous range
                    rg = new FloatRange(sptRanges.get(sptRanges.size() - 1).from, rg.to);
                    sptRanges.set(sptRanges.size() - 1, rg);
                } else
                {
                    sptRanges.add(rg);
                }
            }
        }

        // Check if all rhythm notes are within the allowed range
        for (RhythmVoice rv : rvPhrases.keySet())
        {
            Phrase p = rvPhrases.get(rv);
            for (NoteEvent ne : p)
            {
                boolean inRange = false;
                for (FloatRange rg : sptRanges)
                {
                    if (rg.contains(ne.getPositionInBeats(), true))
                    {
                        inRange = true;
                        break;
                    }
                }
                if (!inRange)
                {
                    // songContext.getPosition(0)
                    String msg = ResUtil.getString(getClass(), "ERR_InvalidNotePosition", ne.toString(), r.getName());
                    LOGGER.log(Level.INFO, "checkRhythmPhrasesScope() " + msg);   //NOI18N
                    LOGGER.fine("DEBUG!  rv=" + rv.getName() + " ne=" + ne + " p=" + p);   //NOI18N
                    throw new MusicGenerationException(msg);
                }
            }
        }
    }

    /**
     * Add tempo factor change JJazz Midi controller messages in the specified track.
     * <p>
     *
     * @param context
     * @param track
     */
    private void addTempoFactorChanges(SongContext context, Track track)
    {
        List<SongPart> spts = context.getSongParts();
        float beatOffset = context.getBeatRange().from;
        float firstTempoPercentChange = -1;
        float lastTempoPercentChange = -1;
        for (SongPart spt : spts)
        {
            float tempoPercentChange = 1;   // By default
            RP_SYS_TempoFactor rp = RP_SYS_TempoFactor.getTempoFactorRp(spt.getRhythm());
            if (rp != null)
            {
                tempoPercentChange = spt.getRPValue(rp) / 100f;
            }
            float beatPos = context.getSptBeatRange(spt).from - beatOffset;
            long tickPos = Math.round(beatPos * MidiConst.PPQ_RESOLUTION);
            MidiEvent me = new MidiEvent(MidiUtilities.getJJazzTempoFactorControllerMessage(0, tempoPercentChange), tickPos);
            track.add(me);
            if (firstTempoPercentChange == -1)
            {
                firstTempoPercentChange = tempoPercentChange;
            }
            lastTempoPercentChange = tempoPercentChange;
        }


        // Add an extra tempo factor change event at the end if first and last songparts don't have the same TempoFactor
        // Needed in order to avoid playback look delays
        if (firstTempoPercentChange != lastTempoPercentChange)
        {
            float beatPos = context.getSptBeatRange(spts.get(spts.size() - 1)).to - beatOffset;
            long tickPos = Math.round(beatPos * MidiConst.PPQ_RESOLUTION) - 2;  // Make sure it's before the End of Track
            MidiEvent me = new MidiEvent(MidiUtilities.getJJazzTempoFactorControllerMessage(0, firstTempoPercentChange), tickPos);
            track.add(me);
        }
    }

    /**
     * Add time signature controller messages in the specified track.
     * <p>
     *
     * @param track
     */
    private void addTimeSignatureChanges(SongContext context, Track track)
    {
        List<SongPart> spts = context.getSongParts();
        float beatOffset = context.getBeatRange().from;
        TimeSignature prevTs = null;
        for (SongPart spt : spts)
        {
            TimeSignature ts = spt.getRhythm().getTimeSignature();
            if (!ts.equals(prevTs))
            {
                float beatPos = songContext.getSptBeatRange(spt).from - beatOffset;
                long tickPos = Math.round(beatPos * MidiConst.PPQ_RESOLUTION);
                MidiEvent me = new MidiEvent(MidiUtilities.getTimeSignatureMessage(ts), tickPos);
                track.add(me);
                prevTs = ts;
            }
        }
    }


    /**
     * Adjust the EndOfTrack Midi marker for all tracks.
     *
     * @param seq
     */
    private void fixEndOfTracks(SongContext context, Sequence seq)
    {
        long lastTick = (long) (context.getBeatRange().size() * MidiConst.PPQ_RESOLUTION) + 1;
        for (Track t : seq.getTracks())
        {
            // Make sure all tracks have the same EndOfTrack
            if (!MidiUtilities.setEndOfTrackPosition(t, lastTick))
            {
                LOGGER.log(Level.WARNING, "checkSequence() problem adjusting EndOfTrack event to lastTick={0}", lastTick);   //NOI18N
            }
        }
    }

    // ====================================================================================================
    // Private classes
    // ====================================================================================================

    /**
     * A runnable to generate all data.
     * <p>
     * Produced data must be retrieved from the fields. If musicException field is not null it means an exception occured.
     */
    private class AllBuilderTask implements Runnable
    {

        // The generated sequence from the phrases
        private SongSequence songSequence;
        private MusicGenerationException musicException = null;

        @Override
        public void run()
        {
            try
            {
                var rvPhrases = buildMapRvPhrase();
                songSequence = buildSongSequence(rvPhrases);
            } catch (MusicGenerationException ex)
            {
                musicException = ex;
                return;
            }
        }
    }

    /**
     * A runnable to generate the RhythmVoice phrases.
     * <p>
     * Produced data must be retrieved from the fields. If musicException field is not null it means an exception occured.
     */
    private class RvPhrasesBuilderTask implements Runnable
    {

        // The generated sequence from the phrases
        private Map<RhythmVoice, Phrase> rvPhrases;
        private MusicGenerationException musicException = null;

        @Override
        public void run()
        {
            try
            {
                rvPhrases = buildMapRvPhrase();
            } catch (MusicGenerationException ex)
            {
                musicException = ex;
                return;
            }
        }
    }

    /**
     * A runnable to generate the SongSequence.
     * <p>
     * Produced data must be retrieved from the fields. If musicException field is not null it means an exception occured.
     */
    private class SongSequenceBuilderTask implements Runnable
    {

        // The generated sequence from the phrases
        private Map<RhythmVoice, Phrase> rvPhrases;
        private SongSequence songSequence;
        private MusicGenerationException musicException = null;

        public SongSequenceBuilderTask(Map<RhythmVoice, Phrase> rvPhrases)
        {
            this.rvPhrases = rvPhrases;
        }

        @Override
        public void run()
        {
            try
            {
                songSequence = buildSongSequence(rvPhrases);
            } catch (MusicGenerationException ex)
            {
                musicException = ex;
                return;
            }
        }
    }

}
