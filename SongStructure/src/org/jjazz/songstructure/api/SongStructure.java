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
     * Return the list of unique rhythms used in this SongStructure.
     *
     * @param excludeAdaptedRhythms
     * @return
     */
    default public List<Rhythm> getUniqueRhythms(boolean excludeAdaptedRhythms)
    {
        ArrayList<Rhythm> res = new ArrayList<>();
        for (SongPart spt : getSongParts())
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
     * Return the list of unique AdaptedRhythms used in this SongStructure.
     *
     * @return Can be empty.
     */
    default public List<AdaptedRhythm> getUniqueAdaptedRhythms()
    {
        ArrayList<AdaptedRhythm> res = new ArrayList<>();
        for (SongPart spt : getSongParts())
        {
            Rhythm r = spt.getRhythm();
            if (!(r instanceof AdaptedRhythm))
            {
                continue;
            }
            AdaptedRhythm ar = (AdaptedRhythm) r;
            {
                res.add(ar);
            }
        }
        return res;
    }

    /**
     * All the RhythmVoices used by this SongStructure.
     *
     * @param excludeRhythmVoiceDelegates
     * @return
     */
    default public List<RhythmVoice> getUniqueRhythmVoices(boolean excludeRhythmVoiceDelegates)
    {
        ArrayList<RhythmVoice> rvs = new ArrayList<>();
        for (Rhythm r : getUniqueRhythms(false))
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
     * Create a new SongPart instance whose container is this object.
     * <p>
     * Use default rhythm parameters values, unless reusePrevParamValues is true and there is a previous song part.
     *
     * @param r
     * @param name The name of the created SongPart.
     * @param startBarIndex
     * @param nbBars
     * @param parentSection Can be null
     * @param reusePrevParamValues
     * @return
     */
    public SongPart createSongPart(Rhythm r, String name, int startBarIndex, int nbBars, CLI_Section parentSection, boolean reusePrevParamValues);

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
     * @return Can be null
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
     * Check if add operation is doable.
     * <p>
     * Operation is not doable if a new rhythm could not be accepted by listeners, or if the operation adds an AdaptedRhythm but
     * its source rhythm will not be present in this SongStructure.
     *
     * @param spts
     * @throws UnsupportedEditException
     */
    public void authorizeAddSongParts(List<SongPart> spts) throws UnsupportedEditException;

    /**
     * Add one by one a list of SongParts.
     * <p>
     * Each SongPart's startBarIndex must be a valid barIndex, either:<br>
     * - equals to the startBarIndex of an existing SongPart <br>
     * - the last barIndex+1 <br>
     * The startBarIndex of the trailing SongParts is shifted accordingly. The SongPart container will be set to this object.
     * <p>
     *
     * @param spts
     * @throws UnsupportedEditException Exception is thrown before any change is done. See authorizeAddSongParts().
     */
    public void addSongParts(List<SongPart> spts) throws UnsupportedEditException;


    /**
     * Check if remove operation is doable.
     * <p>
     * If an AdaptedRhythm is used in this SongStructure, the song part for its source rhythm can't be removed.
     *
     * @param spts
     * @throws UnsupportedEditException
     */
    public void authorizeRemoveSongParts(List<SongPart> spts) throws UnsupportedEditException;

    /**
     * Remove some SongParts.
     * <p>
     * The startBarIndex of the trailing SongParts are updated.
     *
     * @param spts A List of SongParts.
     * @throws UnsupportedEditException Exception is thrown before any change is done. See authorizeRemoveSongParts()
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
     * Check if replace operation is doable.
     * <p>
     * UnsupportedEditException is thrown if replacement is impossible, because :<br>
     * - not enough Midi channels for a new rhythm<br>
     * - if the operation removes a source rhythm of a remaining AdaptedRhyth<br>
     * - an AdaptedRhythm is added without the presence of its source Rhythm.
     *
     * @param oldSpts
     * @param newSpts
     * @throws UnsupportedEditException
     */
    public void authorizeReplaceSongParts(List<SongPart> oldSpts, List<SongPart> newSpts) throws UnsupportedEditException;

    /**
     * Replace SongParts by other SongParts.
     * <p>
     * Typically used to changed rhythm. The size and startBarIndex of new SongParts must be the same than the replaced ones. The
     * container of newSpt will be set to this object.
     *
     * @param oldSpts
     * @param newSpts size must match oldSpts
     * @throws UnsupportedEditException Exception is thrown before any change is done. See authorizeReplaceSongParts()
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
     * Return null if:<br>
     * - The specified time signature has never been used by this SongStructure<br>
     * - If the last used rhythm is an AdaptedRhythm but its source rhythm is no more present
     *
     * @param ts
     * @return Can be null
     */
    public Rhythm getLastUsedRhythm(TimeSignature ts);

    /**
     * Get the recommended rhythm to use for a new SongPart.
     * <p>
     * If possible use getLastUsedRhythm(). If not possible then :<br>
     * - return an AdaptedRhythm if there is a previous SongPart<br>
     * - otherwise return the RhythmDatabase default rhythm for the time signature.<br>
     *
     * @param ts The TimeSignature of the rhythm
     * @param sptStartBarIndex The start bar index of the new song part.
     * @return Can't be null
     */
    public Rhythm getRecommendedRhythm(TimeSignature ts, int sptStartBarIndex);

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
