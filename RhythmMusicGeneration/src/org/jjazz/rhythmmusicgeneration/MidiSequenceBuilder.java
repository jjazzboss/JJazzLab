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
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiUtilities;
import org.jjazz.midimix.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.parameters.RP_SYS_Mute;
import org.jjazz.rhythmmusicgeneration.spi.MidiMusicGenerator;
import org.netbeans.api.progress.BaseProgressUtils;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.openide.util.Exceptions;

/**
 * Ask all the rhythms of a song to produce music and integrate the results to make a Midi sequence.
 */
public class MidiSequenceBuilder
{

    /**
     * The context.
     */
    private MusicGenerationContext context;
    private final HashMap<RhythmVoice, Integer> mapRvTrackId = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(MidiSequenceBuilder.class.getSimpleName());

    /**
     * @param context The context to build the sequence. Song's SongStructure can not be empty.
     */
    public MidiSequenceBuilder(MusicGenerationContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        this.context = context;
        assert !context.getSong().getSongStructure().getSongParts().isEmpty();
    }

    /**
     * Build the music accompaniment sequence for the defined context.
     * <p>
     * 1/ Create a first empty track with song name.<br>
     * 2/ Ask each used rhythm in the song to produce tracks.<br>
     * 3/ Perform some checks and cleanup on produced tracks: check for possible errors in the context, adjust end of track, check
     * that generator produces music only for the relevant bars, set each track's name.
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
     * A map giving the track id (index in the sequence) for each rhythm voice.
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
     * @param mapRvTrack The tracks to fill, one per rhythmvoice.
     * @return false if there was a problem generating music.
     */
    private boolean generateRhythmTracks(Rhythm r, HashMap<RhythmVoice, Track> mapRvTrack) throws MusicGenerationException
    {
        boolean b = false;
        MidiMusicGenerator generator = r.getLookup().lookup(MidiMusicGenerator.class);
        if (generator != null)
        {
            LOGGER.fine("fillRhythmTracks() calling generateMusic() for rhythm r=" + r.getName());
            r.loadResources();
            generator.generateMusic(context, mapRvTrack);
            b = true;
        } else
        {
            LOGGER.severe("buildSequence() no MidiMusicGenerator object found in rhythm's lookup. rhythm=" + r.getName());
        }
        return b;
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
     * Apply the channel's specific transposition and velocity shift.<br>
     *
     * @param sequence
     */
    private void updatePitchAndVelocity(Sequence sequence)
    {
        MidiMix midiMix = context.getMidiMix();

        // We assume that one track = one Midi channel only
        for (Track track : sequence.getTracks())
        {
            int velocityShift = 9999;
            int transposition = 0;
            int channel = 0;
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent me = track.get(i);
                MidiMessage mm = me.getMessage();
                if (!(mm instanceof ShortMessage))
                {
                    continue;
                }
                ShortMessage sm = (ShortMessage) mm;
                if (sm.getCommand() == ShortMessage.NOTE_ON || sm.getCommand() == ShortMessage.NOTE_OFF)
                {
                    if (velocityShift == 9999)
                    {
                        channel = sm.getChannel();
                        InstrumentMix insMix = midiMix.getInstrumentMixFromChannel(channel);
                        velocityShift = insMix.getSettings().getVelocityShift();
                        transposition = insMix.getSettings().getTransposition();
                        if (velocityShift == 0 && transposition == 0)
                        {
                            // Nothing to change, skip this track
                            break;
                        }
                    }
                    // We must transpose or/and update velocity
                    int newPitch = sm.getData1() + transposition;
                    newPitch = Math.min(newPitch, 127);
                    newPitch = Math.max(newPitch, 0);
                    int newVel = sm.getCommand() == ShortMessage.NOTE_ON ? sm.getData2() + velocityShift : 0;
                    newVel = Math.min(newVel, 127);
                    newVel = Math.max(newVel, 0);
                    try
                    {
                        sm.setMessage(sm.getCommand(), channel, newPitch, newVel);
                    } catch (InvalidMidiDataException ex)
                    {
                        // Should never occur
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        }
    }

    /**
     * Remove some NoteOn/Off depending on the RP_SYS_Mute parameter on each SongPart.<br>
     *
     * @param sequence
     */
    private void muteNotes(Sequence sequence)
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
            List<RhythmVoice> mutedRvs = RP_SYS_Mute.getMutedRhythmVoices(r, muteValues);
            for (RhythmVoice rv : mutedRvs)
            {
                removeSptEvents(sequence, spt, rv);
            }
        }
    }

    /**
     * Remove all the MidiEvents corresponding to the specified spt and rhythm voice.
     *
     * @param sequence
     * @param spt
     * @param rv
     */
    private void removeSptEvents(Sequence sequence, SongPart spt, RhythmVoice rv)
    {
        long sptTickStart = context.getSptStartTick(spt);
        long sptTickEnd = sptTickStart + context.getSptTickLength(spt) - 1;

        int trackId = mapRvTrackId.get(rv);
        Track track = sequence.getTracks()[trackId];

        ArrayList<MidiEvent> needRemove = new ArrayList<>(20);

        // Loop on each MidiEvent of the track
        for (int i = 0; i < track.size(); i++)
        {
            MidiEvent me = track.get(i);
            long tick = me.getTick();
            if (tick < sptTickStart)
            {
                // We're not yet in the spt tick range
                // Nothing
            } else if (tick <= sptTickEnd)
            {
                // We're in the current spt tick range, is it a NoteOn or NoteOff on mutedChannels ?
                MidiMessage mm = me.getMessage();
                if (!(mm instanceof ShortMessage))
                {
                    continue;
                }
                ShortMessage sm = (ShortMessage) mm;
                if (sm.getCommand() == ShortMessage.NOTE_ON || sm.getCommand() == ShortMessage.NOTE_OFF)
                {
                    needRemove.add(me);
                }
            } else
            {
                // We past the end
                break;
            }
        }

        // Finally remove what needs to be removed
        for (MidiEvent me : needRemove)
        {
            track.remove(me);
        }
    }

    /**
     * If sequence uses more than 1 rhythm, check that each generator produced music only for the relevant bars.
     *
     * @param seq
     * @throws MusicGenerationException
     */
    private void checkRhythmSlices(Sequence seq) throws MusicGenerationException
    {
        if (context.getUniqueRhythms().size() == 1)
        {
            // Only 1 rhythm, nothing to check
            return;
        }

        // Get the tick ranges used by each rhythm        
        HashMap<Rhythm, TickRanges> mapRhythmTr = new HashMap<>();
        for (SongPart spt : context.getSongParts())
        {
            long sptTickStart = context.getSptStartTick(spt);
            long sptTickEnd = sptTickStart + context.getSptTickLength(spt) - 1;
            Rhythm r = spt.getRhythm();
            TickRanges tr = mapRhythmTr.get(r);
            if (tr == null)
            {
                tr = new TickRanges();
                mapRhythmTr.put(r, tr);
            }
            tr.addRange(sptTickStart, sptTickEnd);
        }

        // Check the notes Vs tick ranges
        for (Rhythm r : mapRhythmTr.keySet())
        {
            TickRanges tRanges = mapRhythmTr.get(r);
            for (RhythmVoice rv : r.getRhythmVoices())
            {
                int trackId = mapRvTrackId.get(rv);
                assert trackId != -1 : "rv=" + rv;
                Track track = seq.getTracks()[trackId];
                for (int eventId = 0; eventId < track.size(); eventId++)
                {
                    // Check that each NOTE_ON event is within the track's rhythm tick ranges
                    MidiEvent me = track.get(eventId);
                    long tick = me.getTick();
                    MidiMessage mm = me.getMessage();
                    if (mm instanceof ShortMessage)
                    {
                        ShortMessage sm = (ShortMessage) mm;
                        if (sm.getCommand() == ShortMessage.NOTE_ON && !tRanges.isIn(tick))
                        {
                            // Position pos = SongStructure.Util.getPosition(sgs, tick);
                            Position pos = context.getPosition(tick);
                            Rhythm expectedRhythmAtPos = context.getSong().getSongStructure().getSongPart(pos.getBar()).getRhythm();
                            String msg = "There was a problem in the generated Midi data. Consult log for more details.";
                            LOGGER.warning(msg);
                            LOGGER.log(Level.WARNING, "checkSequence() Unexpected NOTE_ON Midi event: {0}", MidiUtilities.toString(mm, tick));
                            LOGGER.log(Level.WARNING, "...at SongStructure position: {0}", pos);
                            LOGGER.log(Level.WARNING, "...on track {0} generated by Rhythm: {1}, and RhythmVoice: {2}", new Object[]
                            {
                                trackId, r.getName(), rv
                            });
                            LOGGER.log(Level.WARNING, "...==> this is not the rhythm data expected at bar {0}, it should be:{1}",
                                    new Object[]
                                    {
                                        pos.getBar(), expectedRhythmAtPos.getName()
                                    });
                            LOGGER.log(Level.INFO, "tRanges={0}", tRanges);
                            throw new MusicGenerationException(msg);
                        }
                    }
                }
            }
        }
    }

    /**
     * Adjust the EndOfTrack Midi marker for all tracks.
     *
     * @param seq
     */
    private void fixEndOfTracks(Sequence seq)
    {
        SongStructure sgs = context.getSong().getSongStructure();
        long lastTick = (sgs.getSizeInBeats(context.getRange()) * MidiConst.PPQ_RESOLUTION) + 1;
        for (RhythmVoice rv : mapRvTrackId.keySet())
        {
            int trackId = mapRvTrackId.get(rv);
            Track t = seq.getTracks()[trackId];

            // Make sure all tracks have the same EndOfTrack
            if (!MidiUtilities.setEndOfTrackPosition(t, lastTick))
            {
                LOGGER.log(Level.WARNING, "checkSequence() problem adjusting EndOfTrack event to lastTick={0}", lastTick);
            }
        }
    }

    /**
     * Check Midi events consistency.
     * <p>
     * Check that only authorized events are used : trackname, EndOfTrack, Note ON/OFF and PitchBend.<br>
     * Check that there is only 1 channel used per track.
     *
     * @param sequence
     */
    private void checkMidiEventsConsistency(Sequence seq) throws MusicGenerationException
    {
        for (Track track : seq.getTracks())
        {
            int channel = -1;
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent me = track.get(i);
                MidiMessage mm = me.getMessage();
                if (mm instanceof MetaMessage)
                {
                    MetaMessage metaMsg = (MetaMessage) mm;
                    switch (metaMsg.getType())
                    {
                        case 3: // Track name event
                        case 47: // End of track event
                            // OK, do nothing
                            break;
                        default:
                            String msg = "Unauthorized Midi event in generated Midi track #" + i + ": MidiEvent=" + MidiUtilities.toString(mm, me.getTick());
                            LOGGER.warning(msg);
                            throw new MusicGenerationException(msg);
                    }
                    // END event: OK do nothing
                } else if (mm instanceof ShortMessage)
                {
                    ShortMessage sm = (ShortMessage) mm;
                    switch (sm.getCommand())
                    {
                        case ShortMessage.NOTE_ON:
                        case ShortMessage.NOTE_OFF:
                        case ShortMessage.PITCH_BEND:
                            if (channel == -1)
                            {
                                channel = sm.getChannel();
                            } else if (sm.getChannel() != channel)
                            {
                                String msg = "Invalid channel used in Midi track #" + i + ": expected channel=" + channel + " MidiEvent=" + MidiUtilities.toString(mm, me.getTick());
                                LOGGER.warning(msg);
                                throw new MusicGenerationException(msg);
                            }
                            break;
                        default:
                            String msg = "Unauthorized Midi event in generated Midi track #" + i + ": MidiEvent=" + MidiUtilities.toString(mm, me.getTick());
                            LOGGER.warning(msg);
                            throw new MusicGenerationException(msg);
                    }
                } else
                {
                    String msg = "Unauthorized Midi event in generated Midi track #" + i + ": MidiEvent=" + MidiUtilities.toString(mm, me.getTick());
                    LOGGER.warning(msg);
                    throw new MusicGenerationException(msg);
                }
            }
        }
    }

    // ===================================================================================
    // Private classes
    // ===================================================================================
    /**
     * Store some ticks intervals.
     */
    private class TickRanges
    {

        private final ArrayList<Long> froms = new ArrayList<>();
        private final ArrayList<Long> tos = new ArrayList<>();

        public final void addRange(long from, long to)
        {
            if (from > to)
            {
                throw new IllegalArgumentException("from=" + from + " to=" + to);
            }
            froms.add(from);
            tos.add(to);
        }

        /**
         * @param tick
         * @return True if tick is within one of the tick range.
         */
        public boolean isIn(long tick)
        {
            for (int i = 0; i < froms.size(); i++)
            {
                if (tick >= froms.get(i) && tick <= tos.get(i))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < froms.size(); i++)
            {
                sb.append("[" + froms.get(i) + "," + tos.get(i) + "]");
            }
            return sb.toString();
        }

    }

    // ====================================================================================================
    // Private classes
    // ====================================================================================================
    /**
     * Does the real job of building the sequence.
     * <p>
     * If musicException field is not null there was an exception. Result sequence is in the sequence field.
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
            try
            {
                sequence = new Sequence(Sequence.PPQ, MidiConst.PPQ_RESOLUTION);
            } catch (InvalidMidiDataException ex)
            {
                musicException = new MusicGenerationException("SequenceBuilderTask() Can't create initial sequence : " + ex.getLocalizedMessage());
                return;
            }

            // First track is really useful only when exporting to Midi file type 1
            // Contain song name
            Track track0 = sequence.createTrack();
            MidiUtilities.addTrackNameEvent(track0, context.getSong().getName() + " (JJazzLab song)");

            // Other tracks
            // Main loop on each song's rhythm
            int trackId = 1;
            mapRvTrackId.clear();
            List<Rhythm> uniqueContextRhythms = context.getUniqueRhythms();    // Only the rhythms used in the context
            for (Rhythm r : uniqueContextRhythms)
            {
                // Create the empty tracks
                HashMap<RhythmVoice, Track> mapRvTrack = new HashMap<>();
                for (RhythmVoice rv : r.getRhythmVoices())
                {
                    Track track = sequence.createTrack();
                    // First event will be the name of the track
                    String name = rv.getContainer().getName() + "-" + rv.getName();
                    MidiUtilities.addTrackNameEvent(track, name);
                    // Store the track with the RhythmVoice
                    mapRvTrack.put(rv, track);
                    mapRvTrackId.put(rv, trackId);
                    trackId++;
                }
                try
                {
                    // Have the tracks filled
                    generateRhythmTracks(r, mapRvTrack);         // Possible MusicGenerationException here
                } catch (MusicGenerationException ex)
                {
                    musicException = ex;
                    return;
                }
            }

            // Post-process operations
            fixEndOfTracks(sequence);
            muteNotes(sequence);
            updatePitchAndVelocity(sequence);

            // Finally perform some consistency checks
            try
            {
                checkRhythmSlices(sequence);                                 // Possible MusicGenerationException here
                checkMidiEventsConsistency(sequence);                        // Possible MusicGenerationException here
            } catch (MusicGenerationException ex)
            {
                musicException = ex;
                return;
            }
        }
    }

}
