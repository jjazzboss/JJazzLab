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
package org.jjazz.songstructure.api;

import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.event.UndoableEditListener;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * A SongStructure manages SongParts.
 * <p>
 * Implementation must fire the relevant SgsChangeEvents (including RpValueChangedEvents for mutable RP value changes, see
 * {@link org.jjazz.rhythm.api.MutableRpValue}): start and complete SgsActionEvents for an API method, with the related low-level SgsChangeEvents. If API
 * method1 calls API method2, SgsActionEvents are only fired for API method1 (SgsActionEvents are not nested).
 * <p>
 */
public interface SongStructure
{

    /**
     * Return the list of unique rhythms used in this SongStructure.
     * <p>
     * Parameters can be used to exclude from the return list AdaptedRhythm and "implicit source rhythm" instances.
     * <p>
     * An "implicit source rhythm" is the source rhythm of an AdaptedRhythm in a song which does not directly use the source rhythm. For example if song
     * contains only spt1=bossa[3/4], then bossa(4/4) is the implicit source rhythm of the AdaptedRhythm bossa[3/4]. If song contains spt1=bossa and
     * spt2=bossa[3/4], then bossa(4/4) is a source rhythm but is not an "implicit source rhythm".
     * <p>
     * If both excludeAdaptedRhythms and excludeImplicitSourceRhythms parameters are false and there is an implicit source rhythm, then the return list will
     * contain the AdaptedRhythm instance juste before the implicit rhythm instance.
     *
     * @param excludeAdaptedRhythms        If true, don't return AdaptedRhythm instances
     * @param excludeImplicitSourceRhythms If true don't return "implicit source rhythms" instances
     * @return The list of rhythms, in the order they are used in the song.
     */
    default public List<Rhythm> getUniqueRhythms(boolean excludeAdaptedRhythms, boolean excludeImplicitSourceRhythms)
    {
        ArrayList<Rhythm> res = new ArrayList<>();
        var allRhythms = getSongParts().stream()
                .map(spt -> spt.getRhythm())
                .toList();


        for (SongPart spt : getSongParts())
        {
            Rhythm r = spt.getRhythm();

            if (res.contains(r))
            {
                continue;
            }

            if (r instanceof AdaptedRhythm)
            {
                if (!excludeAdaptedRhythms)
                {
                    res.add(spt.getRhythm());
                }

                var sr = ((AdaptedRhythm) r).getSourceRhythm();
                if (!excludeImplicitSourceRhythms && !allRhythms.contains(sr) && !res.contains(sr))
                {
                    res.add(sr);
                }
            } else
            {
                res.add(r);
            }

        }
        return res;
    }

    /**
     * Return the list of unique AdaptedRhythms used in this SongStructure.
     *
     * @return Can be empty. List is ordered by SongPart order.
     */
    default public List<AdaptedRhythm> getUniqueAdaptedRhythms()
    {
        ArrayList<AdaptedRhythm> res = new ArrayList<>();
        for (SongPart spt : getSongParts())
        {
            Rhythm r = spt.getRhythm();
            if (r instanceof AdaptedRhythm ar)
            {
                res.add(ar);
            }
        }
        return res;
    }

    /**
     * Get all the unique RhythmVoices used by this SongStructure.
     * <p>
     * Parameters are used to exclude RhythmVoice instances from AdaptedRhythms or "implicit source rhythms" (see getUniqueRhythms()).
     *
     * @param excludeAdaptedRhythms  If true exclude the RhythmVoiceDelegate instances of AdaptedRhythms
     * @param excludeImplicitRhythms If true exclude the RhythmVoice instances of the "implicit source rhythms"
     * @return
     * @see #getUniqueRhythms(boolean, boolean)
     */
    default public List<RhythmVoice> getUniqueRhythmVoices(boolean excludeAdaptedRhythms, boolean excludeImplicitRhythms)
    {
        ArrayList<RhythmVoice> rvs = new ArrayList<>();
        for (Rhythm r : getUniqueRhythms(excludeAdaptedRhythms, excludeImplicitRhythms))
        {
            r.getRhythmVoices().forEach(rv -> rvs.add(rv));
        }
        return rvs;
    }

    /**
     * Get the list of unique TimeSignatures used in the SongStructure.
     *
     * @return List is ordered by SongPart order.
     */
    default public List<TimeSignature> getUniqueTimeSignatures()
    {
        List<TimeSignature> timeSignatures = new ArrayList<>();
        for (var spt : getSongParts())
        {
            var ts = spt.getRhythm().getTimeSignature();
            if (!timeSignatures.contains(ts))
            {
                timeSignatures.add(ts);
            }
        }
        return timeSignatures;
    }

    /**
     * An optional parent ChordLeadSheet.
     * <p>
     * If set the SongStructure will listen to ChordLeadSheet changes to update itself accordingly.
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
     * @param name                 The name of the created SongPart.
     * @param startBarIndex
     * @param nbBars
     * @param parentSection        Can be null
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
     * The bar range corresponding to this song structure.
     *
     * @return [0;getSizeInBars()-1]
     * @see #getSizeInBars()
     */
    default public IntRange getBarRange()
    {
        return new IntRange(0, getSizeInBars() - 1);
    }

    /**
     * Converts the specified bar range into a natural beat range.
     * <p>
     * The method must take into account SongParts with possibly different time signatures.
     *
     * @param barRange If null use the whole song structure.
     * @return Can be an empty range if barRange is not contained in the song structure.
     */
    public FloatRange toBeatRange(IntRange barRange);

    /**
     * Converts the position of the specified bar in natural beats: take into account the possible different time signatures before specified bar.
     *
     * @param absoluteBarIndex A value in the range [0; getSizeInBars()].
     * @return
     */
    public float toPositionInNaturalBeats(int absoluteBarIndex);

    /**
     * Converts the specified position in natural beats: take into account the possible different time signatures before specified bar.
     *
     * @param pos
     * @return
     */
    default public float toPositionInNaturalBeats(Position pos)
    {
        return SongStructure.this.toPositionInNaturalBeats(pos.getBar()) + pos.getBeat();
    }

    /**
     * Converts a song structure position into a chord leadsheet position.
     *
     * @param pos
     * @return null if pos is beyond the end of the song
     */
    default public Position toClsPosition(Position pos)
    {
        Position res = null;
        SongPart spt = getSongPart(pos.getBar());
        if (spt != null)
        {
            var cliSection = spt.getParentSection();
            res = cliSection.getPosition();
            res.setBar(res.getBar() + pos.getBar() - spt.getStartBarIndex());
            res.setBeat(pos.getBeat());
        }
        return res;
    }

    /**
     * Converts a natural beats position into a bar/beats Position.
     * <p>
     * Take into account the possible different time signatures of the song.
     *
     * @param posInBeats
     * @return Null if posInBeats is beyond the end of the song.
     */
    public Position toPosition(float posInBeats);


    /**
     * Get the absolute position in the song structure of a chordleadsheet item referred to by the specified song part.
     *
     * @param spt
     * @param clsItem
     * @return A position within spt range
     * @throws IllegalArgumentException If clsItem does not belong to spt's parent Section.
     * @throws IllegalStateException    If getParentChordLeadSheet() returns null.
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
     *
     * @param spts
     * @throws UnsupportedEditException Exception is thrown before any change is done. See authorizeAddSongParts().
     */
    public void addSongParts(List<SongPart> spts) throws UnsupportedEditException;

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
    public void resizeSongParts(Map<SongPart, Integer> mapSptSize);

    /**
     * Test if a change is vetoed by listeners.
     *
     * @param event Can not be a SgsActionEvent
     * @throws UnsupportedEditException If change is vetoed by a listener
     */
    public void testChangeEventForVeto(SgsChangeEvent event) throws UnsupportedEditException;

    /**
     * Replace SongParts by other SongParts.
     * <p>
     * Typically used to changed rhythm. The size and startBarIndex of new SongParts must be the same than the replaced ones. The container of newSpt will be
     * set to this object.
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
     * @param spt   The SongPart rp belongs to.
     * @param rp    The RhythmParameter.
     * @param value The new value to apply for rp.
     */
    public <T> void setRhythmParameterValue(SongPart spt, RhythmParameter<T> rp, T value);

    /**
     * Returns the last rhythm used in this songStructure for this TimeSignature.
     * <p>
     *
     * @param ts
     * @return Can be null if the specified time signature has never been used by this SongStructure.
     */
    public Rhythm getLastUsedRhythm(TimeSignature ts);

    /**
     * Get the recommended rhythm to use for a new SongPart.
     * <p>
     * If possible use getLastUsedRhythm(). If not possible then :<br>
     * - return an AdaptedRhythm of the current rhythm at sptStartBarIndex, <br>
     * - otherwise return the RhythmDatabase default rhythm for the time signature.<br>
     *
     * @param ts               The TimeSignature of the rhythm
     * @param sptStartBarIndex The start bar index of the new song part, can be on an existing song part, or right after the last song part.
     * @return Can't be null
     */
    public Rhythm getRecommendedRhythm(TimeSignature ts, int sptStartBarIndex);

    /**
     * Get a deep copy of this SongStructure.
     * <p>
     * Transient fields such as listeners are not copied.
     *
     * @param parentCls Can be null. The parent chordleadsheet of the returned copy. If not null should contain sections with names matching this object's
     *                  SongPart names.
     * @return
     */
    public SongStructure getDeepCopy(ChordLeadSheet parentCls);

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
