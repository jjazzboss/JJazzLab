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
package org.jjazz.songstructure.api;

import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.event.UndoableEditListener;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.util.FloatRange;
import org.jjazz.util.IntRange;
import org.jjazz.util.SmallMap;

/**
 * A SongStructure manages SongParts.
 * <p>
 */
public interface SongStructure
{

    /**
     * Return the list of unique rhythms used in a SongStructure.
     *
     * @param sgs
     * @param excludeAdaptedRhythms
     * @return
     */
    static public List<Rhythm> getUniqueRhythms(SongStructure sgs, boolean excludeAdaptedRhythms)
    {
        ArrayList<Rhythm> res = new ArrayList<>();
        for (SongPart spt : sgs.getSongParts())
        {
            Rhythm r = spt.getRhythm();
            if (!res.contains(r) && (!excludeAdaptedRhythms || !(r instanceof AdaptedRhythm)))
            {
                res.add(spt.getRhythm());
            }
        }
        return res;
    }

    /**
     * All the RhythmVoices of sgs.
     *
     * @param sgs
     * @param excludeRhythmVoiceDelegates
     * @return
     */
    static public List<RhythmVoice> getUniqueRhythmVoices(SongStructure sgs, boolean excludeRhythmVoiceDelegates)
    {
        ArrayList<RhythmVoice> rvs = new ArrayList<>();
        for (Rhythm r : getUniqueRhythms(sgs, false))
        {
            r.getRhythmVoices().stream()
                    .filter(rv -> !excludeRhythmVoiceDelegates || !(rv instanceof RhythmVoiceDelegate))
                    .forEach(rv -> rvs.add(rv));
        }
        return rvs;
    }

    /**
     * An optional parent ChordLeadSheet.
     * <p>
     * The SongStructure might listen to ChordLeadSheet changes to update itself accordingly.
     *
     * @return Can be null.
     */
    public ChordLeadSheet getParentChordLeadSheet();

    /**
     * Create a new SongPart instance with default RhythmParameter values and container=this object.
     *
     * @param r
     * @param startBarIndex
     * @param nbBars
     * @param parentSection
     * @return
     */
    public SongPart createSongPart(Rhythm r, int startBarIndex, int nbBars, CLI_Section parentSection);

    /**
     * @return A copy of the list of SongParts ordered according to their getStartBarIndex().
     */
    public List<SongPart> getSongParts();


    /**
     * Get the SongParts which match the tester.
     *
     * @param tester
     * @return
     */
    public List<SongPart> getSongParts(Predicate<SongPart> tester);

    /**
     * Get the SongPart which contains a specific bar.
     *
     * @param absoluteBarIndex
     * @return Null if absoluteBarIndex after end of SongStructure.
     */
    public SongPart getSongPart(int absoluteBarIndex);

    /**
     * Get the size in bars of the song.
     *
     * @return
     */
    public int getSizeInBars();

    /**
     * Convert the specified bar range into a natural beat range.
     * <p>
     * The method must take into account song with possibly different time signatures.
     *
     * @param barRange If null use the whole song structure.
     * @return
     */
    public FloatRange getBeatRange(IntRange barRange);

    /**
     * The position of the specified bar in natural beats: take into account the possible different time signatures before
     * specified bar.
     *
     * @param absoluteBarIndex A value in the range [0 - getSizeInBars()]
     * @return
     */
    public float getPositionInNaturalBeats(int absoluteBarIndex);

    /**
     * The position in bars/beats converted from a position specified in natural beats.
     * <p>
     * Take into account the possible different time signatures of the song.
     *
     * @param posInBeats
     * @return Null if posInBeats is beyond the end of the song.
     */
    public Position getPosition(float posInBeats);

    /**
     * Get the absolute position in the song structure of a chordleadsheet item referred to by the specified song part.
     *
     * @param spt
     * @param clsItem
     * @return A position within spt range
     * @throws IllegalArgumentException If clsItem does not belong to spt's parent Section.
     * @throws IllegalStateException If getParentChordLeadSheet() returns null.
     */
    public Position getSptItemPosition(SongPart spt, ChordLeadSheetItem<?> clsItem);

    /**
     * Add one by one a list of SongParts.
     * <p>
     * Each SongPart's startBarIndex must be a valid barIndex, either:<br>
     * - equals to the startBarIndex of an existing SongPart <br>
     * - the last barIndex+1 <br>
     * The startBarIndex of the trailing SongParts is shifted accordingly. The SongPart container will be set to this object.
     * <p>
     * If the added SongPart uses an AdaptedRhythm, its source rhythm must be also present in this object (possibly added by this
     * operation).
     *
     * @param spts
     * @throws UnsupportedEditException If a new rhythm could not be accepted, or if the operation adds an AdaptedRhythm but its
     * source rhythm is not present in this object. Exception is thrown before any change is done.
     */
    public void addSongParts(List<SongPart> spts) throws UnsupportedEditException;

    /**
     * Remove some SongParts.
     * <p>
     * The startBarIndex of the trailing SongParts are updated.
     *
     * @param spts A List of SongParts.
     * @throws UnsupportedEditException If the operation removes a source rhythm of a remaining AdaptedRhythm. Exception is thrown
     * before any change is done.
     */
    public void removeSongParts(List<SongPart> spts) throws UnsupportedEditException;

    /**
     * Change the size in bars of SongParts.
     * <p>
     * The startBarIndex of the trailing SongParts are updated.
     *
     * @param mapSptSize A map which associates a SongPart and the new desired size.
     */
    public void resizeSongParts(SmallMap<SongPart, Integer> mapSptSize);

    /**
     * Replace SongParts by other SongParts.
     * <p>
     * Typically used to changed rhythm. The size and startBarIndex of new SongParts must be the same than the replaced ones. The
     * container of newSpt will be set to this object.
     *
     * @param oldSpts
     * @param newSpts size must match oldSpts
     * @throws UnsupportedEditException If replacement is impossible, typically because not enough Midi channels for a new rhythm,
     * or if the operation removes a source rhythm of a remaining AdaptedRhythm, or an AdaptedRhythm is added without the presence
     * of its source Rhythm. Exception is thrown before any change occurs.
     */
    public void replaceSongParts(List<SongPart> oldSpts, List<SongPart> newSpts) throws UnsupportedEditException;

    /**
     * Change the name of one or more SongParts.
     *
     * @param spts
     * @param name The name of the SongParts.
     */
    public void setSongPartsName(List<SongPart> spts, String name);

    /**
     * Change the value of a specific RhythmParameter.
     *
     * @param <T>
     * @param spt The SongPart rp belongs to.
     * @param rp The RhythmParameter.
     * @param value The new value to apply for rp.
     */
    public <T> void setRhythmParameterValue(SongPart spt, RhythmParameter<T> rp, T value);

    /**
     * Returns the last rhythm used in this songStructure for this TimeSignature.
     * <p>
     *
     * @param ts
     * @return Can be null if ts has never been used in this song.
     */
    public Rhythm getLastUsedRhythm(TimeSignature ts);

    /**
     * Add a listener to changes of this object.
     *
     * @param l
     */
    public void addSgsChangeListener(SgsChangeListener l);

    /**
     * Remove a listener to this object's changes.
     *
     * @param l
     */
    public void removeSgsChangeListener(SgsChangeListener l);

    /**
     * Add a listener to undoable edits.
     *
     * @param l
     */
    public void addUndoableEditListener(UndoableEditListener l);

    /**
     * Remove a listener to undoable edits.
     *
     * @param l
     */
    public void removeUndoableEditListener(UndoableEditListener l);
}
