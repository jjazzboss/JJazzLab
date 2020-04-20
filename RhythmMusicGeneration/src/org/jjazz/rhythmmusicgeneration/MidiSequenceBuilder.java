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
package org.jjazz.rhythmmusicgeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midi.InstrumentSettings;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiUtilities;
import org.jjazz.midimix.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.parameters.RP_SYS_Mute;
import org.jjazz.rhythm.parameters.RP_SYS_TempoFactor;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.netbeans.api.progress.BaseProgressUtils;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.FloatRange;

/**
 * Ask all the rhythms of a song to produce music and integrate the results to make a Midi sequence.
 * <p>
 */
public class MidiSequenceBuilder
{

    /**
     * The context.
     */
    private MusicGenerationContext context;
    private MusicGenerator.PostProcessor[] postProcessors;  // Can be null
    private final HashMap<RhythmVoice, Integer> mapRvTrackId = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(MidiSequenceBuilder.class.getSimpleName());

    /**
     * @param context The context to build the sequence. Song's SongStructure can not be empty.
     * @param postProcessors Optional postProcessors to run on the generated phrases.
     */
    public MidiSequenceBuilder(MusicGenerationContext context, MusicGenerator.PostProcessor... postProcessors)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        this.context = context;
        assert !context.getSong().getSongStructure().getSongParts().isEmpty();
        this.postProcessors = postProcessors;
    }

    /**
     * Build the music accompaniment sequence for the defined context.
     * <p>
     * 1/ Create a first empty track with song name.<br>
     * 2/ Ask each used rhythm in the song to produce music (one Phrase per RhythmVoice) via its MusicGenerator implementation.
     * <br>
     * 3/ Perform some checks and assemble the produced phrases into a sequence.<br>
     * 4/ Add CTRL_CHG_JJAZZ_TEMPO_FACTOR Midi controller messages based on the RP_SYS_TempoFactor value (if used by a rhythm)
     * <p>
     * If context range start bar is &gt; 0, the Midi events are shifted to start at sequence tick 0.
     *
     * @param silent If true do not show a progress dialog
     * @return A Sequence containing accompaniment tracks for the context.
     * @throws org.jjazz.rhythmmusicgeneration.MusicGenerationException
     */
    public Sequence buildSequence(boolean silent) throws MusicGenerationException
    {

        // Check that there is a valid starting chord at the beginning on each section
        checkStartChordPresence();      // throws MusicGenerationException

        // Check there is no 2 chords at same position
        checkChordsAtSamePosition();            // throws MusicGenerationException

        SequenceBuilderTask task = new SequenceBuilderTask();
        if (silent)
        {
            task.run();
        } else
        {
            BaseProgressUtils.showProgressDialogAndRun(task, "Preparing Music...");
        }

        if (task.musicException != null)
        {
            throw task.musicException;
        }

        return task.sequence;
    }

    /**
     * A map giving the track id (i7ndex in the sequence) for each rhythm voice.
     * <p>
     * Must be called AFTER call to buildSequence(). The returned map contains data only for the generated tracks in the given
     * context. In a song with 2 rhythms R1 and R2, if context only uses R2, then only the id and tracks for R2 are returned.
     *
     * @return
     */
    public HashMap<RhythmVoice, Integer> getRvTrackIdMap()
    {
        return new HashMap<>(mapRvTrackId);
    }

    /**
     * @return The last context used by buildSequence()
     * @see buildSequence()
     */
    public MusicGenerationContext getContext()
    {
        return context;
    }

    @Override
    public String toString()
    {
        return "MidiSequenceBuilder context=" + context.toString();
    }


    // =========================================================================
    // Private methods
    // =========================================================================
    /**
     * Get the rhythm's MidiMusicGenerator and ask him to generate music.
     *
     * @param r
     */
    private HashMap<RhythmVoice, Phrase> generateRhythmPhrases(Rhythm r) throws MusicGenerationException
    {
        MusicGenerator generator = r.getLookup().lookup(MusicGenerator.class);
        if (generator != null)
        {
            LOGGER.fine("fillRhythmTracks() calling generateMusic() for rhythm r=" + r.getName());
            r.loadResources();
            return generator.generateMusic(context);
        } else
        {
            throw new MusicGenerationException("No MidiMusicGenerator object found in rhythm's lookup. rhythm=" + r.getName());
        }
    }

    /**
     * Check that there is a starting chord symbol for each section.
     *
     * @throws MusicGenerationException
     */
    private void checkStartChordPresence() throws MusicGenerationException
    {
        ChordLeadSheet cls = context.getSong().getChordLeadSheet();
        for (CLI_Section section : cls.getItems(CLI_Section.class))
        {
            Position pos = section.getPosition();
            List<? extends CLI_ChordSymbol> clis = cls.getItems(section, CLI_ChordSymbol.class);
            if (clis.isEmpty() || !clis.get(0).getPosition().equals(pos))
            {
                throw new MusicGenerationException("Starting chord symbol missing for section " + section.getData().getName() + " at bar " + (pos.getBar() + 1) + ".");
            }
        }
    }

    /**
     * Check if the ChordLeadSheet contains 2 chord symbols at the same position.
     */
    private void checkChordsAtSamePosition() throws MusicGenerationException
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
                sb.append("Two chord symbols can not have the same position: ");
                sb.append(cliCs.getData().toString()).append(cliCs.getPosition().toUserString());
                sb.append(" - ");
                sb.append(existingCliCs.getData().toString()).append(existingCliCs.getPosition().toUserString());
                throw new MusicGenerationException(sb.toString());
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
    private void processMutedInstruments(HashMap<RhythmVoice, Phrase> rvPhrases)
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
                    LOGGER.warning("muteNotes() Unexpected null phase. rv=" + rv + " rvPhrases=" + rvPhrases);
                    continue;
                }
                p.split(sptRange.from, sptRange.to, true, false);
            }
        }
    }

    /**
     * Apply transposition/velocity offset to match the InstrumentSettings of each RhythmVoice.
     *
     * @param rvPhrases
     */
    private void processInstrumentsSettings(HashMap<RhythmVoice, Phrase> rvPhrases)
    {
        LOGGER.fine("processInstrumentsSettings() -- ");
        MidiMix midiMix = context.getMidiMix();
        for (RhythmVoice rv : rvPhrases.keySet())
        {
            Phrase p = rvPhrases.get(rv);
            InstrumentMix insMix = midiMix.getInstrumentMixFromKey(rv);
            if (insMix == null)
            {
                LOGGER.warning("applyInstrumentsSettings() Unexpected null InstrumentMix for rv=" + rv + " midMix=" + midiMix);
                continue;
            }
            InstrumentSettings insSet = insMix.getSettings();
            if (insSet.getTransposition() != 0)
            {
                p.processPitch(pitch -> pitch + insSet.getTransposition());
                LOGGER.fine("processInstrumentsSettings()    Adjusting transposition=" + insSet.getTransposition() + " for rv=" + rv);
            }
            if (insSet.getVelocityShift() != 0)
            {
                p.processVelocity(v -> v + insSet.getVelocityShift());
                LOGGER.fine("processInstrumentsSettings()    Adjusting velocity=" + insSet.getVelocityShift() + " for rv=" + rv);
            }
        }
    }

    /**
     * Check that rvPhrases contain music notes only for the relevant bars of rhythm r.
     *
     * @param r
     * @param rvPhrases
     * @throws MusicGenerationException
     */
    private void checkRhythmPhrasesScope(Rhythm r, HashMap<RhythmVoice, Phrase> rvPhrases) throws MusicGenerationException
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
                    // context.getPosition(0)
                    String msg = "Invalid note position " + ne.toString() + " for rhythm " + r.getName();
                    LOGGER.log(Level.INFO, "checkRhythmPhrasesScope() " + msg);
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
    private void addTempoFactorChanges(Track track)
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
     * Adjust the EndOfTrack Midi marker for all tracks.
     *
     * @param seq
     */
    private void fixEndOfTracks(Sequence seq)
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
     * Does the real job of building the sequence.
     * <p>
     * If musicException field is not null it means an exception occured. Created sequence is available in the sequence field.
     */
    private class SequenceBuilderTask implements Runnable
    {

        public Sequence sequence = null;
        public MusicGenerationException musicException = null;

        public SequenceBuilderTask()
        {
        }

        @Override
        public void run()
        {

            // Get the generated phrases for each used rhythm
            HashMap<RhythmVoice, Phrase> res = new HashMap<>();

            for (Rhythm r : context.getUniqueRhythms())
            {
                try
                {
                    HashMap<RhythmVoice, Phrase> rMap = generateRhythmPhrases(r);          // Possible MusicGenerationException here
                    if (context.getUniqueRhythms().size() > 1)
                    {
                        checkRhythmPhrasesScope(r, rMap);                                  // Possible MusicGenerationException here
                    }

                    // Merge into the final result
                    res.putAll(rMap);
                } catch (MusicGenerationException ex)
                {
                    musicException = ex;
                    return;
                }
            }


            // Optional Post-process
            if (postProcessors != null)
            {
                for (MusicGenerator.PostProcessor pp : postProcessors)
                {
                    try
                    {
                        pp.postProcess(context, res);
                    } catch (MusicGenerationException ex)
                    {
                        musicException = ex;
                        return;
                    }
                }
            }


            // Handle muted instruments via the RP_SYS_Mute parameter
            processMutedInstruments(res);


            // Handle instrument settings which impact the phrases: transposition, velocity shift, ...
            processInstrumentsSettings(res);


            // Shift phrases to start at position 0
            for (Phrase p : res.values())
            {
                p.shiftEvents(-context.getBeatRange().from);
            }


            // Convert to Midi sequence
            try
            {
                sequence = new Sequence(Sequence.PPQ, MidiConst.PPQ_RESOLUTION);
            } catch (InvalidMidiDataException ex)
            {
                musicException = new MusicGenerationException("SequenceBuilderTask() Can't create the initial empty sequence : " + ex.getLocalizedMessage());
                return;
            }


            // First track is really useful only when exporting to Midi file type 1            
            // Contain song name and tempo factor changes
            Track track0 = sequence.createTrack();
            MidiUtilities.addTrackNameEvent(track0, context.getSong().getName() + " (JJazzLab song)");
            addTempoFactorChanges(track0);


            // Other tracks : create one per RhythmVoice
            int trackId = 1;
            mapRvTrackId.clear();
            for (Rhythm r : context.getUniqueRhythms())
            {
                // Group tracks per rhythm
                for (RhythmVoice rv : r.getRhythmVoices())
                {
                    Track track = sequence.createTrack();
                    // First event will be the name of the track
                    String name = rv.getContainer().getName() + "-" + rv.getName();
                    MidiUtilities.addTrackNameEvent(track, name);
                    // Fill the track
                    Phrase p = res.get(rv);
                    p.fillTrack(track);
                    // Store the track with the RhythmVoice
                    mapRvTrackId.put(rv, trackId);
                    trackId++;
                }
            }
            fixEndOfTracks(sequence);
        }
    }

}
