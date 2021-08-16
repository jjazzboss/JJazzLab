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

import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_DrumsMix;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_DrumsMixValue;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Mute;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.netbeans.api.progress.BaseProgressUtils;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.ResUtil;

/**
 * Methods to convert a Song into Phrases and Midi sequence.
 * <p>
 */
public class SongSequenceBuilder
{

    static public class SongSequence
    {

        public Sequence sequence;
        public Map<RhythmVoice, Integer> mapRvTrackId;
        public Map<RhythmVoice, Phrase> mapRvPhrase;
    }

    /**
     * A special kind of MusicGenerationException for errors that user can fix, such as 2 chord symbols at the same position, no
     * chord symbol at section start, etc.
     */
    static public class UserErrorException extends MusicGenerationException
    {

        public UserErrorException(String msg)
        {
            super(msg);
        }

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
     * - Ask each used rhythm in the song to produce music (one Phrase per RhythmVoice) via its MusicGenerator implementation.
     * <br>
     * - Apply on each channel possible instrument transpositions, velocity shift, mute (RP_SYS_Mute).<br>
     * - Apply the RP_SYS_DrumsMix velocity changes.<br>
     * <p>
     * Phrases for RhythmVoiceDelegates are merged into the phrases of the source RhythmVoices.
     *
     * @param silent If true do not show a progress dialog
     * @return
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
     * @param rvPhrases The RhythmVoice phrases such as produced by buildMapRvPhrase(boolean).
     * @param silent If true do not show a progress dialog
     * @return A Sequence containing accompaniment tracks for the songContext, including time signature change Midi meta events
     * and JJazz custom Midi controller messages (MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR) for tempo factor changes.
     * @throws MusicGenerationException
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
            Map<RhythmVoice, Phrase> rMap = generateRhythmPhrases(r);                   // Possible MusicGenerationException here

            if (songContext.getUniqueRhythms().size() > 1)
            {
                checkRhythmPhrasesScope(songContext, r, rMap);                              // Possible MusicGenerationException here
            }

            // Merge into the final result
            res.putAll(rMap);

        }


        // Handle muted instruments via the SongPart's RP_SYS_Mute parameter
        processMutedInstruments(songContext, res);


        // Handle the RP_SYS_DrumsMix changes
        processDrumsMixSettings(songContext, res);


        // Merge the phrases from delegate RhythmVoices to the source phrase, and remove the delegate phrases            
        for (var it = res.keySet().iterator(); it.hasNext();)
        {
            var rv = it.next();
            if (rv instanceof RhythmVoiceDelegate)
            {
                RhythmVoiceDelegate rvd = (RhythmVoiceDelegate) rv;
                Phrase p = res.get(rvd);
                Phrase pDest = res.get(rvd.getSource());
                if (pDest == null)
                {
                    throw new IllegalStateException("rv=" + rv + " res=" + res);   //NOI18N
                }

                // There should be no overlap of phrases since the delegate is from a different rhythm, so for different song parts 
                pDest.add(p);

                // Remove the delegate
                it.remove();
            }
        }
        // 
        // From here no more SongPart-based processing allowed, since the phrases for SongParts using an AdaptedRhythm have 
        // been merged into the tracks of its source rhythm.
        // 


        // Handle instrument settings which impact the phrases: transposition, velocity shift, ...
        processInstrumentsSettings(songContext, res);


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
        for (Rhythm r : songContext.getUniqueRhythms())
        {

            // Group tracks per rhythm
            for (RhythmVoice rv : r.getRhythmVoices())
            {

                // Delegate phrases have already been merged
                if (!(rv instanceof RhythmVoiceDelegate))
                {
                    Track track = res.sequence.createTrack();

                    // First event will be the name of the track
                    String name = rv.getContainer().getName() + "-" + rv.getName();
                    MidiUtilities.addTrackNameEvent(track, name);

                    // Fill the track
                    Phrase p = rvPhrases.get(rv);
                    p.fillTrack(track);

                    // Store the track with the RhythmVoice
                    res.mapRvTrackId.put(rv, trackId);
                    trackId++;
                }
            }
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
            LOGGER.fine("fillRhythmTracks() calling generateMusic() for rhythm r=" + r.getName());   //NOI18N
            r.loadResources();
            return ((MusicGenerator) r).generateMusic(songContext);
        } else
        {
            LOGGER.warning("generateRhythmPhrases() r=" + r + " is not a MusicGenerator instance");
            throw new MusicGenerationException("Rhythm " + r.getName() + " is not able to generate music");
        }
    }

    /**
     * Check that there is a starting chord symbol for each section.
     *
     * @throws MusicGenerationException
     */
    private void checkStartChordPresence(SongContext context) throws UserErrorException
    {
        ChordLeadSheet cls = context.getSong().getChordLeadSheet();
        for (CLI_Section section : cls.getItems(CLI_Section.class))
        {
            Position pos = section.getPosition();
            List<? extends CLI_ChordSymbol> clis = cls.getItems(section, CLI_ChordSymbol.class);
            if (clis.isEmpty() || !clis.get(0).getPosition().equals(pos))
            {
                throw new UserErrorException(ResUtil.getString(getClass(), "ERR_MissingChordSymbolAtSection", section.getData().getName(), (pos.getBar() + 1)));
            }
        }
    }

    /**
     * Check if the ChordLeadSheet contains 2 chord symbols at the same position.
     */
    private void checkChordsAtSamePosition(SongContext context) throws UserErrorException
    {
        HashMap<Position, CLI_ChordSymbol> mapPosCs = new HashMap<>();
        ChordLeadSheet cls = context.getSong().getChordLeadSheet();
        List<? extends CLI_ChordSymbol> clis = cls.getItems(CLI_ChordSymbol.class);
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
                throw new UserErrorException(sb.toString());
            } else
            {
                mapPosCs.put(pos, cliCs);
            }
        }
    }

    /**
     * For each SongPart remove notes for muted RhythmVoices depending on the RP_SYS_Mute value.<br>
     *
     * @param rvPhrases
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
                p.split(sptRange.from, sptRange.to, true, false);
            }
        }
    }

    /**
     * Change some note velocities depending on the RP_SYS_DrumsMix value for each SongPart.
     *
     * @param rvPhrases
     */
    private void processDrumsMixSettings(SongContext context, Map<RhythmVoice, Phrase> rvPhrases)
    {
        for (SongPart spt : context.getSongParts())
        {

            // Get the RhythmParameter
            Rhythm r = spt.getRhythm();
            RP_SYS_DrumsMix rpDrums = RP_SYS_DrumsMix.getDrumsMixRp(r);
            if (rpDrums == null)
            {
                continue;
            }


            // Get the RP value
            RP_SYS_DrumsMixValue rpValue = spt.getRPValue(rpDrums);
            var mapSubsetOffset = rpValue.getMapSubsetOffset(); // The offsets to apply for each Subset
            if (mapSubsetOffset.isEmpty())
            {
                continue;
            }


            // Retrieve drums keymap
            RhythmVoice rvDrums = rpDrums.getRhythmVoice();
            var drumkit = context.getMidiMix().getInstrumentMixFromKey(rvDrums).getInstrument().getDrumKit();   // Can be null for "Not Set" drums voice
            var keymap = drumkit != null ? drumkit.getKeyMap() : KeyMapGM.getInstance();


            // Apply the velocity offsets on the SongPart drums notes
            Phrase p = rvPhrases.get(rvDrums);
            assert p != null : "rpDrums=" + rpDrums + " rvDrums=" + rvDrums;
            FloatRange sptRange = context.getSptBeatRange(spt);
            for (NoteEvent ne : p.getNotes(sptRange, true))
            {
                int pitch = ne.getPitch();
                for (DrumKit.Subset subset : mapSubsetOffset.keySet())
                {
                    if (keymap.getKeys(subset).contains(pitch))
                    {
                        // Replace with a new NoteEvent with adjusted velocity
                        int v = MidiUtilities.limit(ne.getVelocity() + mapSubsetOffset.get(subset));
                        NoteEvent newNote = new NoteEvent(ne, ne.getPitch(), ne.getDurationInBeats(), v);
                        p.set(p.indexOf(ne), newNote);
                    }
                }
            }
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
    
    private void checkEmptyRange(SongContext context) throws UserErrorException
    {
        if (context.getBarRange().isEmpty())
        {
            throw new UserErrorException(ResUtil.getString(getClass(), "ERR_NothingToPlay"));
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
