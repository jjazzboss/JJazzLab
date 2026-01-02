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
package org.jjazz.phrase.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.jjazz.harmony.api.Chord;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.LongRange;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * Helper methods using Phrases.
 */
public class Phrases
{

    private static final Logger LOGGER = Logger.getLogger(Phrases.class.getSimpleName());

    /**
     * Add NoteEvents from a list of NOTE_ON/OFF Midi events at MidiConst.PPQ_RESOLUTION.
     * <p>
     * NOTE_ON events without a corresponding NOTE_OFF event are ignored. Notes with 0-tick length are ignored.
     *
     * @param p
     * @param midiEvents       MidiEvents which are not ShortMessage.Note_ON/OFF are ignored. Must be ordered by tick position, resolution must be
     *                         MidiConst.PPQ_RESOLUTION.
     * @param posInBeatsOffset The position in natural beats of the first tick of the track.
     * @param ignoreChannel    If true, add also NoteEvents for MidiEvents which do not match this phrase channel.
     * @see MidiUtilities#getMidiEvents(javax.sound.midi.Track, java.util.function.Predicate, LongRange)
     * @see MidiConst#PPQ_RESOLUTION
     */
    static public void addMidiEvents(Phrase p, List<MidiEvent> midiEvents, float posInBeatsOffset, boolean ignoreChannel)
    {
        // Build the NoteEvents
        var nes = new ArrayList<NoteEvent>();
        MidiEvent[] lastNoteOn = new MidiEvent[128];
        for (MidiEvent me : midiEvents)
        {
            long tick = me.getTick();
            ShortMessage sm = MidiUtilities.getNoteShortMessage(me.getMessage());
            if (sm == null)
            {
                // It's not a note ON/OFF message
                continue;
            }

            int pitch = sm.getData1();
            int velocity = sm.getData2();
            int eventChannel = sm.getChannel();


            if (!ignoreChannel && p.getChannel() != eventChannel)
            {
                // Different channel, ignore
                continue;
            }

            if (sm.getCommand() == ShortMessage.NOTE_ON && velocity > 0)
            {
                // NOTE_ON
                lastNoteOn[pitch] = me;

            } else
            {
                MidiEvent meOn = lastNoteOn[pitch];

                // NOTE_OFF
                if (meOn != null)
                {
                    // Create the NoteEvent
                    long tickOn = meOn.getTick();
                    ShortMessage smOn = (ShortMessage) meOn.getMessage();
                    float duration = ((float) tick - tickOn) / MidiConst.PPQ_RESOLUTION;        // Might be 0 
                    if (duration > 0)
                    {
                        float posInBeats = posInBeatsOffset + ((float) tickOn / MidiConst.PPQ_RESOLUTION);
                        assert posInBeats >= 0 :
                                "smOn=" + MidiUtilities.toString(smOn, tickOn) + " tick=" + tick + "(duration=" + duration + ") posInBeats=" + posInBeats + " pitch=" + pitch;
                        NoteEvent ne = new NoteEvent(pitch, duration, smOn.getData2(), posInBeats);
                        nes.add(ne);
                    } else
                    {
                        LOGGER.log(Level.WARNING, "addMidiEvents() Ignoring zero-length note at tick position={0} pitch={1}", new Object[]
                        {
                            tickOn, pitch
                        });
                    }

                    // Clean the last NoteOn
                    lastNoteOn[pitch] = null;
                } else
                {
                    // A note Off without a previous note On, do nothing
                }
            }
        }
        p.addAll(nes);
    }

    /**
     * Calculate the median velocity from a list of note events.
     * <p>
     *
     * @param notes
     * @param defaultValue Return value if notes is empty
     * @return
     */
    static public int computeMedianVelocity(List<NoteEvent> notes, int defaultValue)
    {
        if (notes.isEmpty())
        {
            return 70;  // Default fallback for empty list
        }

        var velocities = notes.stream()
                .map(NoteEvent::getVelocity)
                .sorted()
                .toList();

        int size = velocities.size();
        if (size % 2 == 0)
        {
            // Even number: average of two middle values
            return (velocities.get(size / 2 - 1) + velocities.get(size / 2)) / 2;
        } else
        {
            // Odd number: middle value
            return velocities.get(size / 2);
        }
    }

    /**
     * Get the pitch range used by p's notes.
     *
     * @param p
     * @return IntRange.EMPTY_RANGE if p is empty
     */
    static public IntRange getPitchRange(Phrase p)
    {
        int pMin = 128;
        int pMax = -1;
        for (var ne : p)
        {
            pMin = Math.min(pMin, ne.getPitch());
            pMax = Math.max(pMax, ne.getPitch());
        }
        return pMin != 128 ? new IntRange(pMin, pMax) : IntRange.EMPTY_RANGE;
    }

    /**
     *
     * @param p
     * @return Null if phrase is empty.
     */
    static public NoteEvent getHighestPitchNote(Phrase p)
    {
        return p.stream().max(Comparator.comparing(NoteEvent::getPitch)).orElse(null);
    }

    /**
     *
     * @param p
     * @return Null if phrase is empty.
     */
    static public NoteEvent getLowestPitchNote(Phrase p)
    {
        return p.stream().min(Comparator.comparing(NoteEvent::getPitch)).orElse(null);
    }

    /**
     *
     * @param p
     * @return Null if phrase is empty.
     */
    static public NoteEvent getHighestVelocityNote(Phrase p)
    {
        return p.stream().max(Comparator.comparing(NoteEvent::getVelocity)).orElse(null);
    }

    /**
     * Update spDest notes so that they match the position/duration/velocity of spSrc notes.
     * <p>
     * This is typically used to "unquantize" spDest using a spSrc phrase which corresponds to a real-time playing.
     * <p>
     *
     * @param spSrc
     * @param spDest         Start bar might be different than spSrc, but nb of bars must be the same
     * @param nearBeatWindow Notes start position must not differ more thatn nearBeatWindow
     * @return True if spDest was changed
     * @throws IllegalArgumentException If nb of bars/notes/time signature is different, or if notes do not start approximately at the same position.
     */
    static public boolean applyGroove(SizedPhrase spSrc, SizedPhrase spDest, float nearBeatWindow)
    {
        Preconditions.checkArgument(
                spSrc.size() == spDest.size() && spSrc.getSizeInBars() == spDest.getSizeInBars() && spSrc.getTimeSignature() == spDest.getTimeSignature(),
                "spSrc=%s spDest=%s", spSrc, spDest);


        boolean b = false;
        float srcBeat0 = spSrc.getNotesBeatRange().from;
        float destBeat0 = spDest.getNotesBeatRange().from;


        var destNotes = new ArrayList<>(spDest);
        int i = 0;
        for (var neSrc : spSrc)
        {
            var neDest = destNotes.get(i);
            var newDestPos = neSrc.getPositionInBeats() - srcBeat0 + destBeat0;
            if (Math.abs(newDestPos - neDest.getPositionInBeats()) > nearBeatWindow)
            {
                throw new IllegalArgumentException("Notes position differ too much: neSrc=" + neSrc + " neDest=" + neDest + " newDestPos=" + newDestPos);
            }
            b = b || (newDestPos != neDest.getPositionInBeats() || neSrc.getDurationInBeats() != neDest.getDurationInBeats() || neSrc.getVelocity() != neDest.getVelocity());
            var newNeDest = neDest.setAll(neDest.getPitch(), neSrc.getDurationInBeats(), neSrc.getVelocity(), newDestPos, null, false);
            spDest.replace(neDest, newNeDest);

            i++;
        }

        return b;
    }

    /**
     * Check if both phrases have the same number of notes with approximatly the same note start positions, and optionnaly the same note durations.
     *
     * @param sp1
     * @param sp2               Start beat range might be different from sp1, but size in bars and time signature must be equal.
     * @param sameNoteDurations If true also check if note durations are approximatively the same.
     * @param nearBeatWindow
     * @return
     */
    static public boolean isSameNotePositions(SizedPhrase sp1, SizedPhrase sp2, boolean sameNoteDurations, float nearBeatWindow)
    {
        boolean b = false;

        if (sp1.size() == sp2.size() && sp1.getSizeInBars() == sp2.getSizeInBars() && sp1.getTimeSignature() == sp2.getTimeSignature())
        {
            var it2 = sp2.iterator();
            b = true;
            for (var ne1 : sp1)
            {
                var dur1 = ne1.getDurationInBeats();
                var relPos1 = ne1.getPositionInBeats() - sp1.getNotesBeatRange().from;

                var ne2 = it2.next();
                var dur2 = ne2.getDurationInBeats();
                var relPos2 = ne2.getPositionInBeats() - sp2.getNotesBeatRange().from;

                if (Math.abs(relPos2 - relPos1) > nearBeatWindow
                        || (sameNoteDurations && Math.abs(dur2 - dur1) > (2 * nearBeatWindow)))
                {
                    b = false;
                    break;
                }
            }
        }
        return b;
    }

    /**
     * Get the phrase notes as MidiEvents.
     * <p>
     * Tick resolution used is MidiConst.PPQ_RESOLUTION.
     *
     * @param p
     * @return Each note is converted into 1 MidiEvent for note ON, 1 for the note OFF
     */
    static public List<MidiEvent> toMidiEvents(Phrase p)
    {
        List<MidiEvent> res = new ArrayList<>();
        for (NoteEvent ne : p)
        {
            for (MidiEvent me : ne.toMidiEvents(p.getChannel()))
            {
                res.add(me);
            }
        }
        return res;
    }

    /**
     * Create MidiEvents for each phrase note then add them to the specified track.
     * <p>
     * Tick resolution used is MidiConst.PPQ_RESOLUTION.
     *
     * @param p
     * @param track
     */
    static public void fillTrack(Phrase p, Track track)
    {
        toMidiEvents(p).forEach(me -> track.add(me));
    }


    /**
     * Get a chord made of all unique pitch NoteEvents present in the phrase.
     *
     * @param p
     * @return
     */
    static public Chord getChord(Phrase p)
    {
        Chord c = new Chord(p.getNotes());
        return c;
    }

    /**
     * Make sure there is no note ringing in the phrase after the specified position.
     * <p>
     * Notes starting after posInBeats are removed. If a note starts before posInBeats but is still ON beyond posInBeats, note duration is shortened to have
     * Note OFF at posInBeats.
     *
     * @param p
     * @param posInBeats
     */
    static public void silenceAfter(Phrase p, float posInBeats)
    {
        var toBeRemoved = new ArrayList<>();
        var tobeReplaced = new HashMap<NoteEvent, NoteEvent>();

        var it2 = p.descendingIterator();
        while (it2.hasNext())
        {
            NoteEvent ne = it2.next();
            float pos = ne.getPositionInBeats();
            if (pos >= posInBeats)
            {
                // Remove notes after posInBeats
                toBeRemoved.add(ne);
            } else if (pos + ne.getDurationInBeats() > posInBeats)
            {
                // Shorten notes before posInBeats but ending after posInBeats
                float newDuration = posInBeats - pos;
                NoteEvent newNe = ne.setDuration(newDuration, true);
                tobeReplaced.put(ne, newNe);
            }
        }

        p.removeAll(toBeRemoved);
        p.replaceAll(tobeReplaced, false);
    }


    /**
     * Get a new phrase with cloned NoteEvents but keeping only the notes in the specified beat range, taking into account possible live-played/non-quantized
     * notes via the beatWindow parameter.
     * <p>
     * First, if beatWindow &gt; 0 then notes starting in the range [range.from-beatWindow; range.from[ are changed in the returned phrase so they start at
     * range.from, and notes starting in the range [range.to-beatWindow; range.to[ are removed.
     * <p>
     * Then, if a note is starting before startPos and ending after range.from: <br>
     * - if keepLeft is false, the note is removed<br>
     * - if keepLeft is true, the note is replaced by a shorter identical one starting at range.from
     * <p>
     * If a note is starting before range.to and ending after range.to: <br>
     * - if cutRight == 0 the note is not removed.<br>
     * - if cutRight == 1, the note is replaced by a shorter identical that ends at range.to.<br>
     * - if cutRight == 2, the note is removed<br>
     * <p>
     *
     * @param p
     * @param range
     * @param keepLeft
     * @param cutRight
     * @param beatWindow A tolerance window if this phrase contains live-played/non-quantized notes. Typical value is 0.1f.
     * @return
     */
    static public Phrase getSlice(Phrase p, FloatRange range, boolean keepLeft, int cutRight, float beatWindow)
    {
        checkArgument(cutRight >= 0 && cutRight <= 2, "cutRight=%s", cutRight);
        checkArgument(beatWindow >= 0);


        Phrase res = new Phrase(p.getChannel(), p.isDrums());


        // Preprocess to accomodate for live playing / non-quantized notes
        Set<NoteEvent> beatWindowProcessedNotes = new HashSet<>();
        if (beatWindow > 0)
        {
            FloatRange frLeft = range.from - beatWindow > 0 ? new FloatRange(range.from - beatWindow, range.from) : null;
            FloatRange frRight = new FloatRange(range.to - beatWindow, range.to);

            for (var ne : p)
            {
                var neBr = ne.getBeatRange();
                if (frLeft != null && frLeft.contains(neBr.from, true))
                {
                    if (frLeft.contains(neBr, false))
                    {
                        // Note is fully contained in the beatWindow! Probably a drums/perc note, moveAll it
                        NoteEvent newNe = ne.setPosition(range.from, true);
                        res.add(newNe);
                    } else
                    {
                        // Note crosses range.from, make it start at range.from
                        float newDur = Math.max(neBr.to - range.from, 0.05f);
                        NoteEvent newNe = ne.setAll(-1, newDur, -1, range.from, null, true);
                        res.add(newNe);
                    }
                    beatWindowProcessedNotes.add(ne);

                } else if (frRight.contains(neBr.from, true))
                {
                    // Remove the note
                    beatWindowProcessedNotes.add(ne);
                }
            }

        }


        for (var ne : p)
        {

            if (beatWindowProcessedNotes.contains(ne))
            {
                // It's already processed, skip
                continue;
            }

            float nePosFrom = ne.getPositionInBeats();
            float nePosTo = nePosFrom + ne.getDurationInBeats();


            if (nePosFrom < range.from)
            {
                // It starts before the slice zone, don't add, except if it overlaps the slice zone
                if (keepLeft && nePosTo > range.from)
                {
                    // It even goes beyond the slice zone!                                        
                    if (nePosTo > range.to)
                    {
                        switch (cutRight)
                        {
                            case 0:
                                // Add it but don't change its end point
                                break;
                            case 1:
                                // Make it shorter
                                nePosTo = range.to;
                                break;
                            case 2:
                                // Do not add
                                continue;
                            default:
                                throw new IllegalStateException("cutRight=" + cutRight);
                        }
                    }
                    float newDur = nePosTo - range.from;
                    NoteEvent newNe = ne.setAll(-1, newDur, -1, range.from, null, true);
                    res.add(newNe);
                }
            } else if (nePosFrom < range.to)
            {
                // It starts in the slice zone, add it
                if (nePosTo <= range.to)
                {
                    // It ends in the slice zone, easy
                    res.add(ne.clone());
                } else
                {
                    // It goes beyond the slice zone
                    switch (cutRight)
                    {
                        case 0:
                            // Add it anyway
                            res.add(ne.clone());
                            break;
                        case 1:
                            // Add it but make it shorter
                            float newDur = range.to - nePosFrom;
                            NoteEvent newNe = ne.setDuration(newDur, true);
                            res.add(newNe);
                            break;
                        case 2:
                            // Do not add
                            break;
                        default:
                            throw new IllegalStateException("cutRight=" + cutRight);
                    }
                }
            } else
            {
                // It starts after the slice zone, do nothing
            }
        }


        return res;
    }


    /**
     * Remove all phrase notes whose start position is in the specified beat range, taking into account possible live-played/non-quantized notes via the
     * beatWindow parameter.
     * <p>
     * If a note is starting before range.from and ending after range.from: <br>
     * - if cutLeft is false, the note is not removed.<br>
     * - if cutLeft is true, the note is replaced by a shorter identical one that ends at range.from, except if the note starts in the range
     * [range.from-beatWindow;range.from[, then it's removed.<p>
     * If a note is starting before range.to and ending after range.to: <br>
     * - if keepRight is false, the note is removed, except if the note starts in the range [range.to-beatWindow;range.to[, then it's replaced by a shorter
     * identical one starting at range.to<br>
     * - if keepRight is true, the note is replaced by a shorter identical one starting at range.to<br>
     *
     * @param p
     * @param range
     * @param cutLeft
     * @param keepRight
     * @param beatWindow A tolerance window if this phrase contains live-played/non-quantized notes. Typical value is 0.1f.
     */
    static public void silence(Phrase p, FloatRange range, boolean cutLeft, boolean keepRight, float beatWindow)
    {
        checkArgument(beatWindow >= 0);

        List<NoteEvent> toBeAdded = new ArrayList<>();
        Map<NoteEvent, NoteEvent> toBeReplaced = new HashMap<>();

        FloatRange frLeft = FloatRange.EMPTY_FLOAT_RANGE;
        FloatRange frRight = FloatRange.EMPTY_FLOAT_RANGE;

        if (beatWindow > 0)
        {
            frLeft = range.from - beatWindow >= 0 ? new FloatRange(range.from - beatWindow, range.from) : FloatRange.EMPTY_FLOAT_RANGE;
            frRight = range.to - beatWindow >= range.from ? new FloatRange(range.to - beatWindow, range.to) : FloatRange.EMPTY_FLOAT_RANGE;
        }


        Iterator<NoteEvent> it = p.iterator();
        int index = 0;
        while (it.hasNext())
        {
            NoteEvent ne = it.next();
            float nePosFrom = ne.getPositionInBeats();
            float nePosTo = nePosFrom + ne.getDurationInBeats();

            if (nePosFrom < range.from)
            {
                if (nePosTo <= range.from)
                {
                    // Leave note unchanged

                } else if (cutLeft)
                {
                    // Replace the note by a shorter one, except if it's in the frLeft beat window
                    if (!frLeft.contains(nePosFrom, true))
                    {

                        // Replace
                        float newDur = range.from - nePosFrom;
                        NoteEvent newNe = ne.setDuration(newDur, true);
                        toBeReplaced.put(ne, newNe);


                        // Special case if note was extending beyond range.to and keepRight is true, add a note after range
                        if (keepRight && nePosTo > range.to)
                        {
                            newDur = nePosTo - range.to;
                            newNe = ne.setAll(-1, newDur, -1, range.to, null, true);

                            toBeAdded.add(newNe);
                        }
                    } else
                    {
                        // It's in the left beat window, remove the note
                        it.remove();
                    }

                }
            } else if (nePosFrom < range.to)
            {
                // Remove the note
                it.remove();

                // Re-add a note after range if required
                if (nePosTo > range.to && (keepRight || frRight.contains(nePosFrom, true)))
                {
                    float newDur = nePosTo - range.to;
                    NoteEvent newNe = ne.setAll(-1, newDur, -1, range.to, null, true);
                    toBeAdded.add(newNe);
                }
            } else
            {
                // nePosFrom is after range.to
                // Nothing
            }

            index++;
        }

        // Add the new NoteEvents after range
        p.addAll(toBeAdded);
        p.replaceAll(toBeReplaced, false);
    }


    /**
     * Get the phrase notes still ringing at specified position.
     * <p>
     *
     * @param p
     * @param posInBeats
     * @param strict     If true, notes starting or ending at posInBeats are excluded.
     * @return The list of notes whose startPos is before (or equals) posInBeats and range.to eafter (or equals) posInBeats
     */
    static public List<NoteEvent> getCrossingNotes(Phrase p, float posInBeats, boolean strict)
    {
        ArrayList<NoteEvent> res = new ArrayList<>();
        for (var ne : p)
        {
            float pos = ne.getPositionInBeats();
            if ((strict && pos >= posInBeats) || (!strict && pos > posInBeats))
            {
                break;
            }
            if ((strict && pos + ne.getDurationInBeats() > posInBeats) || (!strict && pos + ne.getDurationInBeats() >= posInBeats))
            {
                res.add(ne);
            }
        }
        return res;
    }

    /**
     * Get phrase notes matching the specified tester and return them per pitch.
     *
     * @param p
     * @param tester
     * @return The matching notes grouped per pitch, in ascending order.
     */
    static public ListMultimap<Integer, NoteEvent> getNotesByPitch(Phrase p, Predicate<NoteEvent> tester)
    {
        ListMultimap<Integer, NoteEvent> res = MultimapBuilder.treeKeys().arrayListValues().build();

        for (var ne : p)
        {
            if (tester.test(ne))
            {
                res.put(ne.getPitch(), ne);
            }
        }

        return res;
    }

    /**
     * Make sure there is no note which ends right on sp boundary: when moving notes (eg Phrase.shiftAllEvents()), because of float rounding errors, this may
     * lead to notes becoming out of SizedPhrase range.
     *
     * @param sp
     * @return
     */
    static public boolean fixEndOfPhraseNotes(SizedPhrase sp)
    {
        float end = sp.getNotesBeatRange().to - 0.1f;
        Map<NoteEvent, NoteEvent> mapNotes = new HashMap<>();
        for (var ne : sp)
        {
            if (ne.getBeatRange().to > end)
            {
                mapNotes.put(ne, ne.setDuration(end - ne.getPositionInBeats(), true));
            }
        }
        sp.replaceAll(mapNotes, false);
        return !mapNotes.isEmpty();
    }

    /**
     * Remove overlapped phrase notes with identical pitch.
     * <p>
     * - Remove a note completely overlapped by another note.<br>
     * - Shorten a note partially overlapped by another one.
     *
     * @param p
     * @return The notes removed (value=null) or replaced (value=new note)
     */
    static public Map<NoteEvent, NoteEvent> fixOverlappedNotes(Phrase p)
    {
        Map<NoteEvent, NoteEvent> res = new HashMap<>();

        // Get all the notes grouped per pitch
        var mapPitchNotes = getNotesByPitch(p, ne -> true);


        List<NoteEvent> noteOnBuffer = new ArrayList<>();
        for (int pitch : mapPitchNotes.keySet())
        {
            List<NoteEvent> notes = mapPitchNotes.get(pitch);       // Ordered by position
            if (notes.size() == 1)
            {
                continue;
            }

            noteOnBuffer.clear();


            for (NoteEvent ne : notes)
            {
                FloatRange br = ne.getBeatRange();
                boolean removed = false;

                var itOn = noteOnBuffer.listIterator();
                while (itOn.hasNext())
                {
                    NoteEvent prevNoteOn = itOn.next();
                    FloatRange brOn = prevNoteOn.getBeatRange();
                    if (brOn.to <= br.from)
                    {
                        // Remove noteOns which are now Off
                        itOn.remove();
                    } else if (brOn.to >= br.to)
                    {
                        // Note is fully overlapped, remove
                        p.remove(ne);
                        res.put(ne, null);
                        removed = true;
                        break;
                    } else if (br.from == brOn.from)
                    {
                        // 2 notes start in the same time, remove the shortest
                        if (br.size() <= brOn.size())
                        {
                            p.remove(ne);
                            res.put(ne, null);
                            removed = true;
                            break;
                        } else
                        {
                            p.remove(prevNoteOn);
                            res.put(prevNoteOn, null);
                            itOn.remove();
                        }
                    } else
                    {
                        // Note is partially overlapped, replace by a shorter one
                        float newDur = br.from - brOn.from;
                        var newNoteOn = prevNoteOn.setDuration(newDur, true);
                        p.replace(prevNoteOn, newNoteOn);
                        res.put(prevNoteOn, newNoteOn);
                        itOn.remove();
                    }
                }
                if (!removed)
                {
                    noteOnBuffer.add(ne);
                }
            }
        }

        return res;
    }

    /**
     * Change the octave of phrase notes whose pitch is above highLimit or below lowLimit.
     * <p>
     *
     * @param p
     * @param lowLimit  There must be at least 1 octave between lowLimit and highLimit
     * @param highLimit There must be at least 1 octave between lowLimit and highLimit
     */
    static public void limitPitch(Phrase p, int lowLimit, int highLimit)
    {
        if (lowLimit < 0 || highLimit > 127 || lowLimit > highLimit || highLimit - lowLimit < 11)
        {
            throw new IllegalArgumentException("lowLimit=" + lowLimit + " highLimit=" + highLimit);
        }

        Predicate<NoteEvent> tester = ne -> ne.getPitch() < lowLimit || ne.getPitch() > highLimit;
        Function<NoteEvent, NoteEvent> mapper = ne -> 
        {
            int pitch = ne.getPitch();
            while (pitch < lowLimit)
            {
                pitch += 12;
            }
            while (pitch > highLimit)
            {
                pitch -= 12;
            }
            return ne.setPitch(pitch, true);
        };

        p.processNotes(tester, mapper);

    }

    /**
     * Parse a Midi file to extract one phrase from the specified Midi channel notes (notes can be on any track).
     * <p>
     * As a special case, if midiFile contains notes from only 1 channel and this channel is different from the channel parameter, then the method will still
     * accept these notes to build the returned phrase, unless the strictChannel parameter is true.
     *
     * @param midiFile
     * @param channel
     * @param isDrums                    The drums settings of the returned phrase
     * @param strictChannel
     * @param notifyUserIfNoChannelNotes If true notify user if no relevant notes found
     * @return Can be empty
     * @throws java.io.IOException
     * @throws javax.sound.midi.InvalidMidiDataException
     */
    public static Phrase importPhrase(File midiFile, int channel, boolean isDrums, boolean strictChannel, boolean notifyUserIfNoChannelNotes) throws IOException, InvalidMidiDataException
    {
        Preconditions.checkNotNull(midiFile);

        // Load file into a sequence
        Sequence sequence;
        sequence = MidiSystem.getSequence(midiFile);    // throw exceptions, note that exception message might be null
        if (sequence.getDivisionType() != Sequence.PPQ)
        {
            throw new InvalidMidiDataException("Midi file does not use PPQ division: midifile=" + midiFile.getAbsolutePath());
        }


        // LOGGER.severe("importPhrase() sequence=" + MidiUtilities.toString(sequence));
        // Get one phrase per channel
        var phrases = getPhrases(sequence.getResolution(), sequence.getTracks());

        Phrase res = new Phrase(channel, isDrums);
        if (phrases.size() == 1)
        {
            var p0 = phrases.get(0);
            if (p0.getChannel() == channel || !strictChannel)
            {
                res.add(p0);
            }
        } else
        {
            var p = phrases.stream()
                    .filter(ph -> ph.getChannel() == channel)
                    .findAny()
                    .orElse(null);
            if (p != null)
            {
                res.add(p);
            }
        }

        if (res.isEmpty() && notifyUserIfNoChannelNotes)
        {
            String msg = ResUtil.getString(Phrases.class, "NoChannelNotesInMidiFile", channel + 1, midiFile.getAbsolutePath());
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }

        return res;
    }


    /**
     * Parse all tracks to build one phrase per used channel.
     * <p>
     * A track can use notes from different channels. Notes from a given channel can be on several tracks.
     *
     * @param tracksPPQ The Midi PPQ resolution (pulses per quarter) used in the tracks.
     * @param tracks
     * @param channels  Get phrases only for the specified channels. If empty, get phrases for all channels.
     * @return A list of phrases ordered by channel in ascending order
     */
    public static List<Phrase> getPhrases(int tracksPPQ, Track[] tracks, Integer... channels)
    {
        Map<Integer, Phrase> mapChannelPhrase = new HashMap<>();
        List<Integer> selectedChannels = channels.length > 0 ? Arrays.asList(channels) : Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                12, 13, 14, 15);
        for (Track track : tracks)
        {
            // Get all the events at the appropriate resolution
            List<MidiEvent> trackEvents = MidiUtilities.getMidiEvents(track, ShortMessage.class, sm
                    -> sm.getCommand() == ShortMessage.NOTE_OFF || sm.getCommand() == ShortMessage.NOTE_ON, null);
            trackEvents = MidiUtilities.getMidiEventsAtPPQ(trackEvents, tracksPPQ, MidiConst.PPQ_RESOLUTION);
            for (int channel : MidiUtilities.getUsedChannels(track))
            {
                if (selectedChannels.contains(channel))
                {
                    Phrase p = mapChannelPhrase.get(channel);
                    if (p == null)
                    {
                        p = new Phrase(channel, channel == MidiConst.CHANNEL_DRUMS);
                        mapChannelPhrase.put(channel, p);
                    }
                    addMidiEvents(p, trackEvents, 0, false);
                }
            }
        }

        // Some phrases might be empty
        List<Phrase> res = new ArrayList<>();
        for (int i = 0; i < 16; i++)
        {
            Phrase p = mapChannelPhrase.get(i);
            if (p != null && !p.isEmpty())
            {
                res.add(p);
            }
        }
        return res;
    }

}
