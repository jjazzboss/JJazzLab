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
package org.jjazz.ss_editor.api;

import org.jjazz.songstructure.api.SongPartParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.openide.util.Lookup;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * Provide convenience methods to get information about a selection in a lookup.
 * <p>
 * Selected items can be either SelectedSongParts or SongPartParameters, but not both in the same time. Returned SelectedSongParts or SongPartParameters are
 * ordered by startBarIndex.
 */
final public class SS_Selection
{

    private final List<SongPart> songParts = new ArrayList<>();
    private final List<SongPartParameter> songPartParameters = new ArrayList<>();
    private int minStartSptIndex;
    private int maxStartSptIndex;
    private boolean isContiguousSptSelection;
    private boolean isRhythmParameterCompatible;
    private boolean isSameRhythm;
    private static final Logger LOGGER = Logger.getLogger(SS_Selection.class.getSimpleName());

    public SS_Selection(List<SongPart> spts)
    {
        refreshSongParts(spts);
    }

    /**
     * @param sptps
     * @param not_used Not used, just to avoid name clash with other constructor !
     */
    public SS_Selection(List<SongPartParameter> sptps, Object not_used)
    {
        refreshRhythmParameters(sptps);
    }

    /**
     * Refresh the selection with selected objects in the lookup.
     *
     * @param lookup
     * @throws IllegalStateException If lookup contains both SongParts and RhythmParameters
     */
    public SS_Selection(Lookup lookup)
    {
        Objects.requireNonNull(lookup);

        var selSpts = lookup.lookupAll(SelectedSongPart.class);
        var sptps = lookup.lookupAll(SongPartParameter.class);

        if (!selSpts.isEmpty() && !sptps.isEmpty())
        {
            throw new IllegalStateException("lookup=" + lookup);
        }
        
        
        if (!selSpts.isEmpty())
        {
            refreshSongParts(selSpts.stream()
                    .map(selSpt -> selSpt.songPart())
                    .toList());
        } else
        {
            refreshRhythmParameters(sptps);
        }
    }

    private void refreshSongParts(List<SongPart> spts)
    {
        Objects.requireNonNull(spts);

        songParts.clear();
        songParts.addAll(spts);
        if (!songParts.isEmpty() && !songPartParameters.isEmpty())
        {
            throw new IllegalStateException("songParts=" + spts + " rhythmParameters=" + songPartParameters);
        }
        isRhythmParameterCompatible = false;
        isSameRhythm = true;
        minStartSptIndex = Integer.MAX_VALUE;
        maxStartSptIndex = -1;
        int indexSum = 0;
        Rhythm refRhythm = null;
        for (SongPart spt : songParts)
        {
            if (refRhythm == null)
            {
                refRhythm = spt.getRhythm();
            } else if (!refRhythm.equals(spt.getRhythm()))
            {
                isSameRhythm = false;
            }
            int index = spt.getContainer().getSongParts().indexOf(spt);
            indexSum += index;
            minStartSptIndex = Math.min(index, minStartSptIndex);
            maxStartSptIndex = Math.max(index, maxStartSptIndex);
        }
        // Sum of all indexes must match the sum of arithmetic progression
        isContiguousSptSelection = songParts.size() == (maxStartSptIndex - minStartSptIndex + 1)
                && indexSum == songParts.size() * (minStartSptIndex + maxStartSptIndex) / 2;
        // Sort our buffer by startBarIndex
        Collections.sort(songParts, (spt1, spt2) -> Integer.compare(spt1.getStartBarIndex(), spt2.getStartBarIndex()));

        LOGGER.log(Level.FINE, "refreshSongParts() minStartSptIndex={0} maxStartSptIndex={1} isOneSection={2}", new Object[]
        {
            minStartSptIndex,
            maxStartSptIndex, isContiguousSptSelection
        });
    }

    private void refreshRhythmParameters(Collection<? extends SongPartParameter> sptps)
    {
        Objects.requireNonNull(sptps);
        songPartParameters.clear();
        songPartParameters.addAll(sptps);
        if (!songParts.isEmpty() && !songPartParameters.isEmpty())
        {
            throw new IllegalStateException("songParts=" + songParts + " SongPartParameters=" + songPartParameters);
        }
        isRhythmParameterCompatible = true;
        isSameRhythm = true;
        minStartSptIndex = Integer.MAX_VALUE;
        maxStartSptIndex = -1;
        int indexSum = 0;
        Rhythm refRhythm = null;
        RhythmParameter<?> refRp = null;
        for (SongPartParameter sptp : songPartParameters)
        {
            if (refRhythm == null)
            {
                refRhythm = sptp.spt().getRhythm();
                refRp = sptp.rp();
            } else if (!refRhythm.equals(sptp.spt().getRhythm()))
            {
                isSameRhythm = false;
            }
            if (!sptp.rp().isCompatibleWith(refRp))
            {
                isRhythmParameterCompatible = false;
            }
            int index = sptp.spt().getContainer().getSongParts().indexOf(sptp.spt());
            indexSum += index;
            minStartSptIndex = Math.min(index, minStartSptIndex);
            maxStartSptIndex = Math.max(index, maxStartSptIndex);
        }
        // Sum of all indexes must match the sum of arithmetic progression
        isContiguousSptSelection = songPartParameters.size() == (maxStartSptIndex - minStartSptIndex + 1)
                && indexSum == songPartParameters.size() * (minStartSptIndex + maxStartSptIndex) / 2;
        // Sort our buffer by startBarIndex
        Collections.sort(songPartParameters, (sptp1, sptp2) -> Integer.compare(sptp1.spt().getStartBarIndex(), sptp2.spt().getStartBarIndex()));
    }

    /**
     * Convenience function.
     * <p>
     * Unselect everything in specified editor.
     *
     * @param editor
     */
    public void unselectAll(SS_Editor editor)
    {
        if (isRhythmParameterSelected())
        {
            for (SongPartParameter sptp : songPartParameters)
            {
                editor.selectRhythmParameter(sptp.spt(), sptp.rp(), false);
            }
        }
        if (isSongPartSelected())
        {
            for (SongPart spt : songParts)
            {
                editor.selectSongPart(spt, false);
            }
        }
    }

    /**
     * True if all selected Song Parts or Rhythm Parameters share the same rhythm.
     *
     * @return
     */
    public boolean isSameRhythm()
    {
        return isSameRhythm;
    }

    /**
     * True if all selected Rhythm Parameters are compatible.
     *
     * @return
     */
    public boolean isRhythmParameterCompatible()
    {
        return isRhythmParameterCompatible;
    }

    /**
     * True if the selected song parts are contiguous (valid whatever the selection type, SongParts or RhythmParameters)
     *
     * @return
     */
    public boolean isContiguousSptSelection()
    {
        return isContiguousSptSelection;
    }

    public boolean isEmpty()
    {
        return !isSongPartSelected() && !isRhythmParameterSelected();
    }

    public boolean isSongPartSelected()
    {
        return !songParts.isEmpty();
    }

    public boolean isRhythmParameterSelected()
    {
        return !songPartParameters.isEmpty();
    }

    /**
     * True if the first selected RhythmParameter is an instanceof RP_Enumerabl.
     *
     * @return
     */
    public boolean isEnumerableRhythmParameterSelected()
    {
        return !songPartParameters.isEmpty() && songPartParameters.get(0).isEnumerableRp();
    }

    /**
     * Get the first SongPart of the selection.
     * <p>
     * Works independently of the selection mode (SongParts or RhythmParameters). Return a meaningful value only if selection is not empty.
     *
     * @return The index of the SongPart in the SongStructure.
     */
    public int getMinStartSptIndex()
    {
        return minStartSptIndex;
    }

    /**
     * Get the last SongPart of the selection.
     * <p>
     * Works independently of the selection mode (SongParts or RhythmParameters). Return a meaningful value only if selection is not empty.
     *
     * @return The index of the SongPart in the SongStructure.
     */
    public int getMaxStartSptIndex()
    {
        return maxStartSptIndex;
    }

    public boolean isSongPartSelected(SongPart spt)
    {
        return songParts.contains(spt);
    }

    public boolean isRhythmParameterSelected(SongPart spt, RhythmParameter<?> rp)
    {
        return songPartParameters.contains(new SongPartParameter(spt, rp));
    }

    /**
     * @param spt
     * @return The selected rp belonging to spt (only one RhythmParameter can be selected for one spt). Null if no rp selected in spt.
     */
    public RhythmParameter<?> getSelectedSongPartParameter(SongPart spt)
    {
        if (isEmpty() || isSongPartSelected())
        {
            return null;
        }
        for (SongPartParameter sptp : songPartParameters)
        {
            if (sptp.spt() == spt)
            {
                return sptp.rp();
            }
        }
        return null;
    }

    /**
     * @return The returned list is ordered by SongPart startBarIndex.
     */
    public List<SongPart> getSelectedSongParts()
    {
        return songParts;
    }

    /**
     * @return The returned list is ordered by SongPart startBarIndex.
     */
    public List<SongPartParameter> getSelectedSongPartParameters()
    {
        return songPartParameters;
    }

    /**
     * The list of selected SongParts (ordered by startBarIndex), or corresponding to the selected RhythmParameters if RhythmParameters are selected.
     *
     * @return
     */
    public List<SongPart> getIndirectlySelectedSongParts()
    {
        if (isSongPartSelected())
        {
            return getSelectedSongParts();
        }
        ArrayList<SongPart> res = new ArrayList<>();
        for (SongPartParameter rpp : songPartParameters)
        {
            res.add(rpp.spt());
        }
        return res;
    }

    /**
     *
     * @return Can be null if isEmpty() is true.
     */
    public SongStructure getModel()
    {
        if (isEmpty())
        {
            return null;
        } else if (isSongPartSelected())
        {
            return songParts.get(0).getContainer();
        } else
        {
            return songPartParameters.get(0).spt().getContainer();
        }
    }

    @Override
    public String toString()
    {
        return "SelectionUtilities songParts=" + songParts + " songPartParameters=" + songPartParameters;
    }

}
