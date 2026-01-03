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
package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
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
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.NCExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransformValue;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhraseValue;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.songcontext.api.SongPartContext;
import org.netbeans.api.progress.BaseProgressUtils;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.Exceptions;
import org.jjazz.outputsynth.spi.OutputSynthManager;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmmusicgeneration.api.CompositeMusicGenerator.DelegateUnit;
import org.jjazz.rhythmmusicgeneration.spi.ConfigurableMusicGeneratorProvider;
import org.jjazz.rhythmmusicgeneration.spi.MusicGeneratorProvider;
import org.jjazz.song.api.Song;

/**
 * Build the musical Phrases and Midi sequence from a Song.
 * <p>
 */
public class SongSequenceBuilder
{

    /**
     * @see #getTempoFactorMetaMessage(float)
     * @see #getTempoFactor(javax.sound.midi.MetaMessage)
     */
    public static final int TEMPO_FACTOR_META_EVENT_TYPE = 12;

    /**
     * The return value of some of the buildXXX() methods.
     *
     * @see #buildSongSequence(java.util.Map)
     * @see #buildAll(boolean)
     */
    static public class SongSequence
    {
        public Sequence sequence;
        public Map<RhythmVoice, Integer> mapRvTrackId;
        public Map<RhythmVoice, Phrase> mapRvPhrase;
    }

    private final SongContext songContextOriginal;
    private final SongContext songContextWork;
    /**
     * Store substitute-tracks-rhythms to be released upon close for each song.
     */
    private final static SetMultimap<Song, Rhythm> MMAP_SONG_SUBSTITUTE_TRACKS_RHYTHMS = MultimapBuilder.hashKeys().hashSetValues().build();

    private static final Logger LOGGER = Logger.getLogger(SongSequenceBuilder.class.getSimpleName());

    /**
     * Create an instance to generate music for the specified SongContext.
     * <p>
     * Note that sgContext is not modified, the instance works on a deep copy of sgContext.
     *
     * @param sgContext
     */
    public SongSequenceBuilder(SongContext sgContext)
    {
        Objects.requireNonNull(sgContext);
        this.songContextOriginal = sgContext;
        this.songContextWork = songContextOriginal.deepClone(false, false);
    }


    /**
     * Call buildMapRvPhrase() then buildSongSequence().
     *
     * @param silent If true do not show a progress dialog while generating the musical phrases
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     * @see #buildMapRvPhrase(boolean)
     * @see #buildSongSequence(java.util.Map)
     */
    public SongSequence buildAll(boolean silent) throws MusicGenerationException
    {
        RvPhrasesBuilderTask task = new RvPhrasesBuilderTask();
        if (silent)
        {
            task.run();
        } else
        {
            BaseProgressUtils.showProgressDialogAndRun(task, ResUtil.getString(getClass(), "PREPARING_MUSIC"));
        }
        if (task.musicException != null)
        {
            throw task.musicException;
        }

        return buildSongSequence(task.rvPhrases);
    }


    /**
     * Build the RhythmVoice phrases for the defined context.
     * <p>
     * - Perform some checks on the context (start chord on each section start, no overlapping chord symbols) <br>
     * - Ask each used rhythm in the song to produce music (one Phrase per RhythmVoice) via its MusicGenerator implementation.<br>
     * - Add the user phrases if any<br>
     * - Apply on each channel possible instrument transpositions, velocity shift, mute (RP_SYS_Mute).<br>
     * - Apply the RP_SYS_DrumsMix velocity changes. Note that it is expected that, if there is an AdaptedRhythm for a Rhythm which uses RP_SYS_DrumsMix, the
     * AdaptedRhythm reuses the same RP_SYS_DrumsMix instance.<br>
     * - Apply the RP_SYS_CustomPhrase changes<br>
     * - Apply the RP_SYS_DrumsTransform changes<br>
     * - Apply the RP_STD_Fill/fade_out value changes<br>
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
            BaseProgressUtils.showProgressDialogAndRun(task, ResUtil.getString(getClass(), "PREPARING_MUSIC"));
        }

        if (task.musicException != null)
        {
            throw task.musicException;
        } else if (task.rvPhrases == null)
        {
            throw new MusicGenerationException("SongSequenceBuilder.buildMapRvPhrase(): unexpected task.rvPhrases=null. Please report this bug.");
        }

        return task.rvPhrases;
    }


    /**
     * Build the SongSequence from the specified RhythmVoice phrases for the defined context.
     * <p>
     * - Create a track 0 with no notes but MidiEvents for song name, time signature changes, TEMPO_FACTOR_META_EVENT_TYPE MetaMessages for the
     * RP_SYS_TempoFactor value (if used by a rhythm). <br>
     * - Then create a track per RhythmVoice.
     * <p>
     * If songContext range start bar is &gt; 0, the Midi events are shifted to start at sequence tick 0.
     *
     * @param rvPhrases The RhythmVoice phrases such as produced by buildMapRvPhrase(boolean), must start at beat 0.
     * @return A Sequence containing accompaniment tracks for the songContext, including time signature change Midi meta events and JJazz custom Midi controller
     *         messages (MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR) for tempo factor changes.
     * @see #buildMapRvPhrase(boolean)
     */
    public SongSequence buildSongSequence(Map<RhythmVoice, Phrase> rvPhrases)
    {
        Objects.requireNonNull(rvPhrases);
        SongSequence res = new SongSequence();
        res.mapRvPhrase = new HashMap<>(rvPhrases);

        try
        {
            res.sequence = new Sequence(Sequence.PPQ, MidiConst.PPQ_RESOLUTION);
        } catch (InvalidMidiDataException ex)
        {
            throw new IllegalStateException("buildSequence() Can't create the initial empty sequence : " + ex.getLocalizedMessage());
        }


        // First track is really useful only when exporting to Midi file type 1            
        // Contain song name, tempo factor changes, time signatures
        Track track0 = res.sequence.createTrack();
        MidiUtilities.addTrackNameEvent(track0, songContextWork.getSong().getName() + " (JJazzLab song)");
        addTimeSignatureChanges(songContextWork, track0);
        addTempoFactorChanges(songContextWork, track0);


        // Other tracks : create one per RhythmVoice
        int trackId = 1;
        res.mapRvTrackId = new HashMap<>();


        // Normally process only normal rhythms, but if context does not use a source rhythm of an adapted rhythm, we need to process it too
        var contextRhythms = songContextWork.getUniqueRhythms();      // Contains AdaptedRhythms
        Set<Rhythm> targetRhythms = new HashSet<>();
        for (Rhythm r : contextRhythms)
        {
            if (r instanceof AdaptedRhythm ar)
            {
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
            int channel = songContextWork.getMidiMix().getChannel(rv);

            String name = buildTrackName(rv, channel);
            MidiUtilities.addTrackNameEvent(track, name);

            // Fill the track
            Phrase p = rvPhrases.get(rv);
            Phrases.fillTrack(p, track);

            // Store the track with the RhythmVoice
            res.mapRvTrackId.put(rv, trackId);
            trackId++;
        }

        fixEndOfTracks(songContextWork, res.sequence);

        return res;
    }

    /**
     * Update the Midi sequence so that it can be exported to external tools (sequencers, Midi editors, etc.).
     * <p>
     * Get rid of all JJazzLab-only MidiEvents and add some initialization events:<br>
     * - add copyright message<br>
     * - remove JJazzLab-specific tempo factor and beat <br>
     * - add tempo events<br>
     * - add GM/GS/XG/GM2 reset messages depending of the current OutputSynth configuration<br>
     * - add reset controllers for each RhythmVoice track<br>
     * - add bank/program and volume/effects events for each RhythmVoice track<br>
     * - add a marker for each chord symbol<br>
     * - add a marker for each song part<br>
     *
     *
     * @param songSequence      Must have been created using buildSongSequence() for the current SongContext
     * @param ignoreMidiMixMute If true, a track will sound even if it was muted in the context MidiMix
     * @see #buildSongSequence(java.util.Map)
     */
    public void makeSequenceExportable(SongSequence songSequence, boolean ignoreMidiMixMute)
    {
        Preconditions.checkNotNull(songSequence);

        Sequence sequence = songSequence.sequence;
        List<SongPart> spts = songContextWork.getSongParts();
        MidiMix midiMix = songContextWork.getMidiMix();
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
            if (mm instanceof ShortMessage sm)
            {
                if (sm.getCommand() == ShortMessage.CONTROL_CHANGE && sm.getData1() == MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR)
                {
                    track0.remove(me);
                    i--;
                }
            }
            i++;
        }


        // Add markers at each chord symbol position
        SongStructure ss = songContextWork.getSong().getSongStructure();
        for (SongPart spt : spts)
        {
            CLI_Section section = spt.getParentSection();
            for (var cliChordSymbol : songContextWork.getSong().getChordLeadSheet().getItems(section, CLI_ChordSymbol.class))
            {
                Position absPos = ss.getSptItemPosition(spt, cliChordSymbol);
                long tickPos = songContextWork.toRelativeTick(absPos);
                me = new MidiEvent(MidiUtilities.getMarkerMetaMessage(cliChordSymbol.getData().getName()), tickPos);
                track0.add(me);
            }
        }


        // Add initial tempo event
        int tempo = songContextWork.getSong().getTempo();
        SongPart spt0 = songContextWork.getSongParts().get(0);
        RP_SYS_TempoFactor rp = RP_SYS_TempoFactor.getTempoFactorRp(spt0.getRhythm());
        int tempoFactor = -1;
        if (rp != null)
        {
            tempoFactor = spt0.getRPValue(rp);
            tempo = Math.round(tempoFactor / 100f * tempo);
        }
        me = new MidiEvent(MidiUtilities.getTempoMessage(0, tempo), 0);
        track0.add(me);


        // Add markers for each song part and additional song part tempo changes if present
        int lastTempoFactor = tempoFactor;
        for (SongPart spt : spts)
        {
            String partName = spt.getName();
            float beatPos = songContextWork.getSptBeatRange(spt).from - songContextWork.getBeatRange().from;
            long spTickPos = Math.round(beatPos * MidiConst.PPQ_RESOLUTION);
            me = new MidiEvent(MidiUtilities.getMarkerMetaMessage(partName), spTickPos);
            track0.add(me);

            rp = RP_SYS_TempoFactor.getTempoFactorRp(spt.getRhythm());
            if (rp != null)
            {
                tempoFactor = spt.getRPValue(rp);
                if (tempoFactor != lastTempoFactor)
                {
                    tempo = Math.round(tempoFactor / 100f * songContextWork.getSong().getTempo());
                    me = new MidiEvent(MidiUtilities.getTempoMessage(0, tempo), spTickPos);
                    track0.add(me);
                    lastTempoFactor = tempoFactor;
                }
            }
        }


        // Add XX mode ON initialization message
        OutputSynth os = OutputSynthManager.getDefault().getDefaultOutputSynth();
        SysexMessage sxm = os.getUserSettings().getModeOnUponPlaySysexMessages();
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
                if (midiMix.getInstrumentMix(rv).isMute())
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
            int channel = midiMix.getChannel(rv);
            assert channel != -1 : "rv=" + rv + " midiMix=" + midiMix + " songSequence.mapRvTrackId=" + songSequence.mapRvTrackId;

            // Reset all controllers
            MidiMessage mmReset = MidiUtilities.getResetAllControllersMessage(channel);
            me = new MidiEvent(mmReset, 0);
            track.add(me);

            // Instrument + volume + pan etc.
            InstrumentMix insMix = midiMix.getInstrumentMix(rv);
            for (MidiMessage mm : insMix.getAllMidiMessages(channel))
            {
                me = new MidiEvent(mm, 0);
                track.add(me);
            }


            // FIX Issue #496 (export to mp3 with FluidSynth synth): at tick 0, make sure that note MidiEvents are *after* the program change MidiEvents
            List<MidiEvent> tick0Notes = new ArrayList<>();
            for (int j = 0; j < track.size(); j++)
            {
                me = track.get(j);
                if (me.getTick() > 0)
                {
                    break;
                }
                if (MidiUtilities.getNoteOnShortMessage(me.getMessage()) != null || MidiUtilities.getNoteOffShortMessage(me.getMessage()) != null)
                {
                    tick0Notes.add(me);
                }
            }
            for (var note0 : tick0Notes)
            {
                track.remove(note0);
                track.add(new MidiEvent(note0.getMessage(), 0));
            }

        }

    }

    @Override
    public String toString()
    {
        return "SongSequenceBuilder songContextWork=" + songContextWork.toString();
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
        String name = rv.getContainer().getName() + "-" + rv.getName() + "-channel" + (channel + 1) + "/16";
        return name;
    }

    /**
     * Get a Meta message which encodes a JJazz tempo factor.
     *
     * @param tempoFactor A percentage in the [.5;2.0] range.
     * @return A MetaMessage with type==TEMPO_FACTOR_META_EVENT_TYPE
     */
    static public MetaMessage getTempoFactorMetaMessage(float tempoFactor)
    {
        Preconditions.checkArgument(tempoFactor >= .5f && tempoFactor <= 2f, "tempoFactor=%s", tempoFactor);
        return new MmTempoFactor(tempoFactor);
    }

    /**
     * Get the tempo factor [.5;2.0] from the specified MetaMessage.
     *
     * @param tempoFactorMm MetaMessage type must be TEMPO_FACTOR_META_EVENT_TYPE
     * @return
     */
    static public float getTempoFactor(MetaMessage tempoFactorMm)
    {
        Preconditions.checkArgument(tempoFactorMm.getType() == TEMPO_FACTOR_META_EVENT_TYPE, "MeatMessage=%s type=%s", tempoFactorMm,
                tempoFactorMm.getType());
        return ((MmTempoFactor) tempoFactorMm).tempoFactor;
    }

    // =========================================================================
    // Private methods
    // =========================================================================

    /**
     * Ask each rhythm to generate a Phrase for each RhythmVoice then do the postprocessing.
     *
     * @return
     * @throws MusicGenerationException
     */
    private Map<RhythmVoice, Phrase> buildMapRvPhrase() throws MusicGenerationException
    {
        Map<RhythmVoice, Phrase> res = new HashMap<>();

        checkEmptyRange(songContextWork);       // throws MusicGenerationException

        // Check that there is a valid starting chord at the beginning on each section
        checkStartChordPresence(songContextWork);      // throws UserErrorGenerationException

        // Check there is no 2 chords at same position
        checkChordsAtSamePosition(songContextWork);            // throws MusicGenerationException        


        for (Rhythm r : songContextWork.getUniqueRhythms())
        {
            // Generate the phrases
            MusicGenerator mg = processRP_SYS_OverrideTracks(r, songContextWork);
            Map<RhythmVoice, Phrase> rMap = generateRhythmPhrases(r, mg, songContextWork);                       // Possible MusicGenerationException here

            checkPhrasesScope(songContextWork, r, rMap);                              // Possible MusicGenerationException here

            // Merge into the final result
            res.putAll(rMap);

        }


        // Handle the RP_SYS_CustomPhrase changes
        processCustomPhrases(songContextWork, res);

        // Handle the RP_SYS_DrumsTransform changes
        processDrumsTransforms(songContextWork, res);

        // Handle the NC chord symbols 
        processNoChords(songContextWork, res);

        // Add the user track phrases
        addUserTrackPhrases(res);

        // Handle the RP_SYS_Mute parameter (user track phrases must be in res)
        processMutedInstruments(songContextWork, res);

        // Handle the AdaptedRhythm's RhythmVoiceDelegates
        // IMPORTANT: after that res will NOT contain RhythmVoiceDelegates anymore
        processAdaptedRhythms(res);

        // Handle the RP_SYS_Fill with value fade_out
        processFadeOut(songContextWork, res);

        // Handle instrument settings which impact the phrases: transposition, velocity shift, ...
        processInstrumentsSettings(songContextWork, res);

        // Process drums rerouting
        processDrumsRerouting(songContextWork, res);


        // Shift phrases to start at position 0
        for (Phrase p : res.values())
        {
            p.shiftAllEvents(-songContextWork.getBeatRange().from, false);
        }


        return res;
    }

    /**
     * For each RhythmVoiceDelegate, merge its phrase into the source phrase, then remove RhythmVoiceDelegate.
     *
     * @param res
     */
    private void processAdaptedRhythms(Map<RhythmVoice, Phrase> res)
    {

        for (var rv : res.keySet().toArray(RhythmVoice[]::new))
        {
            if (rv instanceof RhythmVoiceDelegate rvd)
            {
                Phrase p = res.get(rvd);
                RhythmVoice rvds = rvd.getSource();
                Phrase pDest = res.get(rvds);
                if (pDest == null)
                {
                    // Might happen if the context range contains only SongParts with AdaptedRhythms (source rhythms are excluded)
                    pDest = new Phrase(p.getChannel(), rvds.isDrums());
                    res.put(rvds, pDest);
                }

                // There should be no overlap of phrases since the delegate is from a different rhythm, so for different song parts
                pDest.add(p);

                // Remove the delegate phrase
                res.remove(rvd);
            }
        }
    }

    /**
     * Add a phrase for each user track.
     *
     * @param res
     */
    private void addUserTrackPhrases(Map<RhythmVoice, Phrase> res)
    {

        var br = songContextWork.getBeatRange();
        for (String userPhraseName : songContextWork.getSong().getUserPhraseNames())
        {
            UserRhythmVoice urv = songContextWork.getMidiMix().getUserRhythmVoice(userPhraseName);
            assert urv != null : "userPhraseName=" + userPhraseName + " songContext.getMidiMix()=" + songContextWork.getMidiMix();


            // Create the phrase 
            int channel = songContextWork.getMidiMix().getChannel(urv);
            Phrase p = new Phrase(channel, urv.isDrums());
            p.add(songContextWork.getSong().getUserPhrase(userPhraseName));


            // Adapt to the current context
            p = Phrases.getSlice(p, br, false, 1, 0.1f);

            LOGGER.log(Level.FINE, "addUserTrackPhrases() Adding user phrase for name={0} p={1}", new Object[]
            {
                userPhraseName, p
            });

            res.put(urv, p);
        }
    }

    /**
     * Process the RP_SYS_OverrideTracks to get the MusicGenerator to use for r.
     *
     * @param r
     * @param sgContext
     * @return The default rMusicGenerator of r, or a r-based CompositeMusicGenerator if RP_SYS_OverrideTracks is used
     * @throws org.jjazz.rhythm.api.MusicGenerationException If problem loading Rhythm resources
     */
    private MusicGenerator processRP_SYS_OverrideTracks(Rhythm r, SongContext sgContext) throws MusicGenerationException
    {
        Objects.requireNonNull(r);
        Objects.requireNonNull(sgContext);

        if (!(r instanceof MusicGeneratorProvider mgp))
        {
            LOGGER.log(Level.WARNING, "processRP_SYS_OverrideTracks() r={0} is not a MusicGeneratorProvider instance", r);
            throw new MusicGenerationException("Rhythm " + r.getName() + " does not implement MusicGeneratorProvider, it can not generate music");
        }

        LOGGER.log(Level.FINE, "processRP_SYS_OverrideTracks() r={0} sgContext={1}", new Object[]
        {
            r, sgContext
        });

        MusicGenerator res = mgp.getMusicGenerator();       // Standard case by default


        // Check if RP_SYS_OverrideTracks is used
        RP_SYS_OverrideTracks rpOverride = RP_SYS_OverrideTracks.getOverrideTracksRp(r);
        if (rpOverride != null && sgContext.getSongParts().stream()
                .filter(spt -> spt.getRhythm() == r)
                .anyMatch(spt -> !spt.getRPValue(rpOverride).isEmpty()))
        {
            // RP_SYS_OverrideTracks is used, we need a CompositeMusicGenerator
            assert r instanceof ConfigurableMusicGeneratorProvider : "r=" + r;
            res = buildCompositeMusicGenerator(rpOverride);
        }

        return res;
    }

    /**
     * Generate music for r using the specified MusicGenerator.
     *
     * @param r
     * @param mg
     * @param sgContext
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private Map<RhythmVoice, Phrase> generateRhythmPhrases(Rhythm r, MusicGenerator mg, SongContext sgContext) throws MusicGenerationException
    {
        Objects.requireNonNull(r);
        Objects.requireNonNull(mg);
        Objects.requireNonNull(sgContext);


        // Make sure all Rhythm resources are loaded
        r.loadResources();          // Throws MusicGenerationException      
        var substituteRhythms = getOverrideTracksRhythms(r, sgContext);
        if (!substituteRhythms.isEmpty())
        {
            for (var cr : substituteRhythms)
            {
                cr.loadResources();     // Throws MusicGenerationException

                // Not ideal but it must be done somewhere
                releaseSubstitutetrackRhythmResourcesUponSongClose(cr);
            }
        }


        // Generate the phrases
        LOGGER.log(Level.FINE, "generateRhythmPhrases() calling generateMusic() for rhythm r={0}", r);
        Map<RhythmVoice, Phrase> res = mg.generateMusic(sgContext);


        // Robustness checks
        var rvs = res.keySet();
        assert r.getRhythmVoices().size() == rvs.size() && !rvs.contains(null) : "r=" + r + " rvs=" + rvs;
        var phrases = res.values();
        assert !phrases.contains(null) : "r=" + r + " phrases=" + phrases;


        return res;

    }

    /**
     * Build a CompositeMusicGenerator based on RP_SYS_OverrideTracks.
     *
     * @param rpOverride
     * @return
     */
    private CompositeMusicGenerator buildCompositeMusicGenerator(RP_SYS_OverrideTracks rpOverride)
    {
        var mgBase = rpOverride.getConfigurableMusicGeneratorProvider().getMusicGenerator();
        assert mgBase != null : "rpOverride=" + rpOverride;
        var baseRhythm = rpOverride.getBaseRhythm();
        var baseRpVariation = RP_SYS_Variation.getVariationRp(baseRhythm);


        CompositeMusicGenerator.RvToDelegateUnitMapper rvMapper = (rvBase, spt) -> 
        {
            Objects.requireNonNull(rvBase);

            var mg = mgBase;        // by default no mapping
            RhythmVoice rvDest = rvBase;           // by default no mapping
            String baseRpVariationValue = baseRpVariation == null ? null : spt.getRPValue(baseRpVariation);

            DelegateUnit res;

            RP_SYS_OverrideTracksValue.Override override;
            if (spt != null && spt.getRhythm() == baseRhythm && (override = spt.getRPValue(rpOverride).getOverride(rvBase)) != null)
            {
                // songPart has an override
                rvDest = override.rvDest();
                mg = ((MusicGeneratorProvider) rvDest.getContainer()).getMusicGenerator();                
                var destRpVariation = RP_SYS_Variation.getVariationRp(rvDest.getContainer());
                String destRpVariationValue = override.variation();     // if null, try to reuse source variation value
                if (destRpVariationValue == null && destRpVariation != null)
                {
                    destRpVariationValue = destRpVariation.getPossibleValues().contains(baseRpVariationValue) ? baseRpVariationValue
                            : destRpVariation.getDefaultValue();
                }
                if (destRpVariationValue == null)
                {
                    destRpVariationValue = "unusedDummyRpVariationValue";
                }

                res = new DelegateUnit(spt, rvBase, mg, rvDest, destRpVariationValue, null);
            } else
            {
                // No override, create a delegate to ourselves
                res = new DelegateUnit(spt, rvBase, mg, baseRpVariationValue);
            }
            return res;
        };

        CompositeMusicGenerator res = new CompositeMusicGenerator(baseRhythm, rvMapper);

        return res;

    }

    /**
     * Get the possible substitute tracks rhythms used by r via the RP_SYS_OverrideTracks parameter
     *
     * @param r
     * @param sgContext
     * @return Can be empty
     */
    private Set<Rhythm> getOverrideTracksRhythms(Rhythm r, SongContext sgContext)
    {
        Set<Rhythm> res = new HashSet<>();
        RP_SYS_OverrideTracks rpRc = RP_SYS_OverrideTracks.getOverrideTracksRp(r);
        if (rpRc != null)
        {
            sgContext.getSongParts().stream()
                    .filter(spt -> spt.getRhythm() == r)
                    .forEach(spt -> res.addAll(spt.getRPValue(rpRc).getAllDestinationRhythms()));
        }
        return res;
    }

    /**
     * Handle the resources release for substitute tracks rhythms.
     *
     * @param str
     */
    private void releaseSubstitutetrackRhythmResourcesUponSongClose(Rhythm str)
    {
        Song song = songContextOriginal.getSong();
        if (MMAP_SONG_SUBSTITUTE_TRACKS_RHYTHMS.get(song).isEmpty())
        {
            song.addPropertyChangeListener(Song.PROP_CLOSED, e -> 
            {
                MMAP_SONG_SUBSTITUTE_TRACKS_RHYTHMS.get(song).forEach(cr -> 
                {
                    LOGGER.log(Level.FINE, "releaseSubstitutetrackRhythmResourcesUponSongClose() (lambda-listener) song closed, release resources of cr={0}",
                            cr);
                    cr.releaseResources();
                });
                MMAP_SONG_SUBSTITUTE_TRACKS_RHYTHMS.removeAll(song);
            });
        }
        MMAP_SONG_SUBSTITUTE_TRACKS_RHYTHMS.put(song, str);
    }


    /**
     * Check that there is a starting chord symbol for each section used in the specified context.
     *
     * @param context
     * @throws org.jjazz.rhythm.api.UserErrorGenerationException
     */
    private void checkStartChordPresence(SongContext context) throws UserErrorGenerationException
    {
        ChordLeadSheet cls = context.getSong().getChordLeadSheet();
        for (CLI_Section section : context.getUniqueSections())
        {
            Position pos = section.getPosition();
            var clis = cls.getItems(section, CLI_ChordSymbol.class);
            if (clis.isEmpty() || !clis.get(0).getPosition().equals(pos))
            {
                throw new UserErrorGenerationException(ResUtil.getString(getClass(), "ERR_MissingChordSymbolAtSection",
                        section.getData().getName(), (pos.getBar() + 1)));
            }
        }
    }

    /**
     * Check if the ChordLeadSheet contains 2 chord symbols at the same position in the passed context.
     *
     * @param context
     * @throws org.jjazz.rhythm.api.UserErrorGenerationException
     */
    private void checkChordsAtSamePosition(SongContext context) throws UserErrorGenerationException
    {
        HashMap<Position, CLI_ChordSymbol> mapPosCs = new HashMap<>();
        ChordLeadSheet cls = context.getSong().getChordLeadSheet();

        for (CLI_Section cliSection : context.getUniqueSections())
        {
            var clis = cls.getItems(cliSection, CLI_ChordSymbol.class);
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
                // There is a MuteRp but nothing is muted 
                continue;
            }


            // At least one RhythmVoice/Track is muted
            FloatRange sptRange = context.getSptBeatRange(spt);
            List<RhythmVoice> mutedRvs = RP_SYS_Mute.getMutedRhythmVoices(r, context.getMidiMix(), muteValues);
            for (RhythmVoice rv : mutedRvs)
            {
                var p = rvPhrases.get(rv);
                if (p == null)
                {
                    LOGGER.log(Level.WARNING, "processMutedInstruments() Unexpected null phase. rv={0} rvPhrases={1}", new Object[]
                    {
                        rv, rvPhrases
                    });
                    continue;
                }
                Phrases.silence(p, sptRange, true, false, 0.1f);
            }
        }
    }

    /**
     * Remove notes for NC chord symbols, ie NCExtChordSymbol instances.
     *
     * @param context
     * @param rvPhrases Keys can include RhythmVoiceDelegates
     * @throws org.jjazz.rhythm.api.UserErrorGenerationException
     */
    private void processNoChords(SongContext context, Map<RhythmVoice, Phrase> rvPhrases) throws UserErrorGenerationException
    {
        var songChordSequence = new SongChordSequence(context.getSong(), context.getBarRange());        // throws UserErrorGenerationException
        SongStructure ss = context.getSong().getSongStructure();

        for (var cliCs : songChordSequence)
        {
            var pos = cliCs.getPosition();
            if (cliCs.getData() instanceof NCExtChordSymbol ncecs)
            {
                float posInBeats = ss.toPositionInNaturalBeats(pos.getBar()) + pos.getBeat();
                TimeSignature ts = ss.getSongPart(pos.getBar()).getRhythm().getTimeSignature();
                float chordDuration = songChordSequence.getChordDuration(cliCs, ts);
                FloatRange beatRange = new FloatRange(posInBeats, posInBeats + chordDuration - 0.1f);

                for (Phrase p : rvPhrases.values())
                {
                    Phrases.silence(p, beatRange, true, false, 0.01f);
                }
            }
        }

    }

    /**
     * Replace phrases by custom phrases depending on the RP_SYS_CustomPhrase value.
     *
     * @param context
     * @param rvPhrases Keys can include RhythmVoiceDelegates. Phrases contain notes within context.toBeatRange().
     */
    private void processCustomPhrases(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        for (SongPart spt : context.getSongParts())
        {

            // Check if rhythm has support for a RP_SYS_CustomPhrase
            Rhythm r = spt.getRhythm();
            RP_SYS_CustomPhrase rpCustomPhrase = RP_SYS_CustomPhrase.getCustomPhraseRp(r);
            if (rpCustomPhrase == null)
            {
                continue;
            }


            FloatRange sptBeatRange = context.getSong().getSongStructure().toBeatRange(spt.getBarRange());
            FloatRange sptBeatRangeInContext = context.getSptBeatRange(spt);


            // Get the RP value and process each customized phrase
            RP_SYS_CustomPhraseValue rpValue = spt.getRPValue(rpCustomPhrase);
            for (RhythmVoice rv : rpValue.getCustomizedRhythmVoices())
            {


                // Prepare the phrase
                Phrase pCustom = rpValue.getCustomizedPhrase(rv);
                var pWork = new Phrase(0);
                pWork.add(pCustom);
                pWork.shiftAllEvents(sptBeatRange.from, false);                // Custom phrase starts at beat 0, make it match songPart's start
                pWork = Phrases.getSlice(pWork, sptBeatRangeInContext, false, 1, 0.1f);    // Keep only the relevant slice


                // Add to the current phrase
                Phrase p = rvPhrases.get(rv);
                Phrases.silence(p, sptBeatRangeInContext, true, false, 0.1f);
                p.add(pWork);
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
            FloatRange sptBeatRange = context.getSptBeatRange(spt);     // Might be smaller than songPart.toBeatRange()
            IntRange sptBarRange = context.getSptBarRange(spt);         // Might be smaller than songPart.getBarRange()
            SongPartContext sptContext = new SongPartContext(context.getSong(), context.getMidiMix(), sptBarRange);


            // Get the RhythmParameter
            Rhythm r = spt.getRhythm();
            RP_SYS_DrumsTransform rpDrumsTransform = RP_SYS_DrumsTransform.getDrumsTransformRp(r);
            if (rpDrumsTransform == null)
            {
                // Happens if no drums RhythmVoice in the rhythm, or if rhythm does not propose this RhythmParameter
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
            SizedPhrase inSp = new SizedPhrase(p.getChannel(), sptBeatRange, r.getTimeSignature(), p.isDrums());
            inSp.add(Phrases.getSlice(p, sptBeatRange, false, 1, 0.1f));
            var chain = rpValue.getTransformChain(false);
            var outSp = chain.transform(inSp, sptContext);


            // Replace the old song part phrase by the transformed one            
            Phrases.silence(p, sptBeatRange, true, false, 0.1f);
            p.add(outSp);
        }
    }

    /**
     * Apply transposition/velocity offset to match the InstrumentSettings of each RhythmVoice.
     *
     * @param context
     * @param rvPhrases
     */
    private void processInstrumentsSettings(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        LOGGER.fine("processInstrumentsSettings() -- ");
        MidiMix midiMix = context.getMidiMix();
        for (RhythmVoice rv : rvPhrases.keySet())
        {
            Phrase p = rvPhrases.get(rv);
            InstrumentMix insMix = midiMix.getInstrumentMix(rv);
            if (insMix == null)
            {
                LOGGER.log(Level.WARNING, "applyInstrumentsSettings() Unexpected null InstrumentMix for rv={0} midMix={1}", new Object[]
                {
                    rv,
                    midiMix
                });
                continue;
            }
            InstrumentSettings insSet = insMix.getSettings();
            if (insSet.getTransposition() != 0)
            {
                p.processPitch(pitch -> pitch + insSet.getTransposition());
                LOGGER.log(Level.FINE, "processInstrumentsSettings()    Adjusting transposition={0} for rv={1}", new Object[]
                {
                    insSet.getTransposition(),
                    rv
                });
            }
            if (insSet.getVelocityShift() != 0)
            {
                p.processVelocity(v -> v + insSet.getVelocityShift());
                LOGGER.log(Level.FINE, "processInstrumentsSettings()    Adjusting velocity={0} for rv={1}", new Object[]
                {
                    insSet.getVelocityShift(),
                    rv
                });
            }
        }
    }

    /**
     * Continuously decrease velocity for song parts with RP_STD_Fill value "fade_out".
     *
     * @param context
     * @param rvPhrases
     */
    private void processFadeOut(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        LOGGER.fine("processFadeOut() -- ");
        for (SongPart spt : context.getSongParts())
        {
            // Check Fill RhythmParameter + fade_out value
            Rhythm r = spt.getRhythm();
            RP_SYS_Fill rpFill = RP_SYS_Fill.getFillRp(r);
            if (rpFill == null || !spt.getRPValue(rpFill).equals(RP_SYS_Fill.VALUE_FADE_OUT))
            {
                continue;
            }

            LOGGER.log(Level.FINE, "processFadeOut() processing spt={0}", spt);
            FloatRange beatRange = context.getSptBeatRange(spt);        // Might be smaller than songPart.toBeatRange()

            for (RhythmVoice rv : rvPhrases.keySet())
            {
                Phrase p = rvPhrases.get(rv);

                // From 100% to 0%
                p.processNotes(ne -> beatRange.contains(ne.getPositionInBeats(), true), ne -> 
                {
                    float f = 1 - beatRange.getPercentage(ne.getPositionInBeats());
                    int vel = Math.round(f * ne.getVelocity());
                    return ne.setVelocity(vel, false);
                });
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
        LOGGER.fine("processDrumsRerouting() -- ");

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
                LOGGER.log(Level.WARNING, "processDrumsRerouting() No available target drums channel found in MidiMix={0}", midiMix);
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
     * @param context
     * @param r
     * @param rvPhrases
     * @throws MusicGenerationException
     */
    private void checkPhrasesScope(SongContext context, Rhythm r, Map<RhythmVoice, Phrase> rvPhrases) throws MusicGenerationException
    {
        // Get the bar ranges used by r
        List<FloatRange> sptBeatRanges = new ArrayList<>();
        for (SongPart spt : context.getSongParts())
        {
            if (spt.getRhythm() == r)
            {
                FloatRange rg = context.getSptBeatRange(spt);
                if (!sptBeatRanges.isEmpty() && sptBeatRanges.get(sptBeatRanges.size() - 1).to == rg.from)
                {
                    // Extend previous range
                    rg = new FloatRange(sptBeatRanges.getLast().from, rg.to);
                    sptBeatRanges.set(sptBeatRanges.size() - 1, rg);
                } else
                {
                    sptBeatRanges.add(rg);
                }
            }
        }

        // Check if all rhythm notes are within the allowed range
        for (RhythmVoice rv : rvPhrases.keySet())
        {
            Phrase p = rvPhrases.get(rv);
            for (NoteEvent ne : p)
            {
                boolean inRange = sptBeatRanges.stream()
                        .anyMatch(rg -> rg.contains(ne.getPositionInBeats(), true));
                if (!inRange)
                {
                    LOGGER.log(Level.SEVERE, "checkRhythmPhrasesScope() Invalid note position ne={0} nePos={1} rv={2}", new Object[]
                    {
                        ne, ne.getPositionInBeats(), rv
                    });
                    LOGGER.log(Level.SEVERE, "checkRhythmPhrasesScope() {0} sptBeatRanges={1}", new Object[]
                    {
                        context, sptBeatRanges
                    });
                    LOGGER.log(Level.SEVERE, "p={0}", p.toStringOneNotePerLine());

                    String msg = "Unexpected error while generating music : invalid note position for " + rv + "\nPlease report problem with log file";
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
            MidiEvent me = new MidiEvent(getTempoFactorMetaMessage(tempoPercentChange), tickPos);
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
            MidiEvent me = new MidiEvent(getTempoFactorMetaMessage(firstTempoPercentChange), tickPos);
            track.add(me);
        }
    }

    /**
     * Add time signature controller messages in the specified track.
     * <p>
     *
     * @param context
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
                float beatPos = songContextWork.getSptBeatRange(spt).from - beatOffset;
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
     * @param context
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
                LOGGER.log(Level.WARNING, "checkSequence() problem adjusting EndOfTrack event to lastTick={0}", lastTick);
            }
        }
    }

    // ====================================================================================================
    // Private classes
    // ====================================================================================================

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
            }
        }
    }


    static private class MmTempoFactor extends MetaMessage
    {

        private final float tempoFactor;

        private MmTempoFactor(float tempoFactor)
        {
            this.tempoFactor = tempoFactor;
            try
            {
                setMessage(TEMPO_FACTOR_META_EVENT_TYPE, new byte[0], 0);
            } catch (InvalidMidiDataException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
    }

}
