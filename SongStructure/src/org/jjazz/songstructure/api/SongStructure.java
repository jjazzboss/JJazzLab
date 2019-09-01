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
import javax.swing.event.UndoableEditListener;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.MidiConst;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.util.SmallMap;

/**
 * A SongStructure manages SongParts.
 */
public interface SongStructure
{

    /**
     * Various utilities methods.
     */
    public static class Util
    {

        /**
         * Return the list of unique rhythms used in a SongStructure.
         *
         * @param sgs
         * @return
         */
        static public List<Rhythm> getUniqueRhythms(SongStructure sgs)
        {
            ArrayList<Rhythm> res = new ArrayList<>();
            for (SongPart spt : sgs.getSongParts())
            {
                if (!res.contains(spt.getRhythm()))
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
         * @return
         */
        static public List<RhythmVoice> getUniqueRhythmVoices(SongStructure sgs)
        {
            ArrayList<RhythmVoice> rvs = new ArrayList<>();
            for (Rhythm r : getUniqueRhythms(sgs))
            {
                rvs.addAll(r.getRhythmVoices());
            }
            return rvs;
        }

        /**
         * Compute the position in the songStructure corresponding to the specified Midi tick position.
         *
         * @param sgs
         * @param tick
         * @return Null if tick position is not valid for the songStructure
         * @see MidiConst.PPQ_RESOLUTION
         */
        static public Position getPosition(SongStructure sgs, long tick)
        {
            long tickStart = 0;
            for (SongPart spt : sgs.getSongParts())
            {
                TimeSignature ts = spt.getRhythm().getTimeSignature();
                int nbNaturalBeats = ts.getNbNaturalBeats() * spt.getNbBars();
                long tickEnd = tickStart + nbNaturalBeats * MidiConst.PPQ_RESOLUTION - 1;
                if (tick >= tickStart && tick <= tickEnd)
                {
                    long tickInSpt = tick - tickStart;
                    float beatInSpt = (float) tickInSpt / MidiConst.PPQ_RESOLUTION;
                    int barInSpt = (int) (beatInSpt / ts.getNbNaturalBeats());
                    float beatInBar = beatInSpt - (barInSpt * ts.getNbNaturalBeats());
                    Position pos = new Position(spt.getStartBarIndex() + barInSpt, beatInBar);
                    return pos;
                }
                tickStart = tickEnd + 1;
            }
            return null;
        }
    }

    /**
     * An optional parent ChordLeadSheet.
     * <p>
     * The SongStructure might listen to ChordLeadSheet changes to update itself accordingly.
     *
     * @return
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
     * Get the SongPart which contains a specific bar.
     *
     * @param absoluteBarIndex
     * @return Null if absoluteBarIndex after end of SongStructure.
     */
    public SongPart getSongPart(int absoluteBarIndex);

    /**
     * @return The total size in bars.
     */
    public int getSizeInBars();

    /**
     * @return The total size in "natural" beats.
     * @see TimeSignature.getNbNaturalBeats()
     */
    public int getSizeInBeats();

    /**
     * The position of the specified bar in natural beats: take into account the possible different time signatures before
     * specified bar.
     *
     * @param absoluteBarIndex A value in the range [0 - getSizeInBars()]
     * @return
     */
    public float getPositionInNaturalBeats(int absoluteBarIndex);

    /**
     * Add a SongPart.
     * <p>
     * SongPart's startBarIndex must be a valid barIndex, either<br>
     * - equals to the startBarIndex of an existing SongPart <br>
     * - the last barIndex+1 <br>
     * The startBarIndex of the trailing SongParts is shifted accordingly. The SongPart container will be set to this object.
     *
     * @param spt the value of spt
     * @throws org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException If new rhythm could not be accepted and no
     * replacement done.
     */
    public void addSongPart(SongPart spt) throws UnsupportedEditException;

    /**
     * Remove some SongParts.
     * <p>
     * The startBarIndex of the trailing SongParts are updated.
     *
     * @param spts A List of SongParts.
     */
    public void removeSongParts(List<SongPart> spts);

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
     * @throws UnsupportedEditException If replacement was impossible, typically because not enough Midi channels for a new
     * rhythm.
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
     * Get the default rhythm to be used for the specified TimeSignature for this SongStructure.
     *
     * @param ts
     * @return
     */
    public Rhythm getDefaultRhythm(TimeSignature ts);

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
