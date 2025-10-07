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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
 * Selected items can be either SongPart or RhythmParameters, but not both in the same time. Returned SongParts or
 * SongPartParameters are ordered by startBarIndex.
 */
final public class SS_Selection
{

    private List<SongPart> songParts = new ArrayList<>();
    private List<SongPartParameter> songPartParameters = new ArrayList<>();
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
        if (lookup == null)
        {
            throw new IllegalArgumentException("lookup=" + lookup);   
        }
        @SuppressWarnings("unchecked")
        ArrayList<SongPart> spts = (ArrayList<SongPart>) new ArrayList<>(lookup.lookupAll(SongPart.class));
        @SuppressWarnings("unchecked")
        ArrayList<SongPartParameter> sptps = (ArrayList<SongPartParameter>) new ArrayList<>(lookup.lookupAll(SongPartParameter.class));
        if (!spts.isEmpty() && !sptps.isEmpty())
        {
            throw new IllegalStateException("lookup=" + lookup);   
        }
        if (!spts.isEmpty())
        {
            refreshSongParts(spts);
        } else
        {
            refreshRhythmParameters(sptps);
        }
    }

    private void refreshSongParts(List<SongPart> spts)
    {
        if (spts == null)
        {
            throw new IllegalArgumentException("items=" + spts);   
        }
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
        Collections.sort(songParts, new Comparator<SongPart>()
        {
            @Override
            public int compare(SongPart spt1, SongPart spt2)
            {
                return spt1.getStartBarIndex() - spt2.getStartBarIndex();
            }
        });
        LOGGER.log(Level.FINE, "refreshSongParts() minStartSptIndex={0} maxStartSptIndex={1} isOneSection={2}", new Object[]{minStartSptIndex,
            maxStartSptIndex, isContiguousSptSelection});   
    }

    private void refreshRhythmParameters(List<SongPartParameter> sptps)
    {
        if (sptps == null)
        {
            throw new IllegalArgumentException("srps=" + sptps);   
        }
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
                refRhythm = sptp.getSpt().getRhythm();
                refRp = sptp.getRp();
            } else if (!refRhythm.equals(sptp.getSpt().getRhythm()))
            {
                isSameRhythm = false;
            }
            if (!sptp.getRp().isCompatibleWith(refRp))
            {
                isRhythmParameterCompatible = false;
            }
            int index = sptp.getSpt().getContainer().getSongParts().indexOf(sptp.getSpt());
            indexSum += index;
            minStartSptIndex = Math.min(index, minStartSptIndex);
            maxStartSptIndex = Math.max(index, maxStartSptIndex);
        }
        // Sum of all indexes must match the sum of arithmetic progression
        isContiguousSptSelection = songPartParameters.size() == (maxStartSptIndex - minStartSptIndex + 1)
                && indexSum == songPartParameters.size() * (minStartSptIndex + maxStartSptIndex) / 2;
        // Sort our buffer by startBarIndex
        Collections.sort(songPartParameters, new Comparator<SongPartParameter>()
        {
            @Override
            public int compare(SongPartParameter sptp1, SongPartParameter sptp2)
            {
                return sptp1.getSpt().getStartBarIndex() - sptp2.getSpt().getStartBarIndex();
            }
        });
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
                editor.selectRhythmParameter(sptp.getSpt(), sptp.getRp(), false);
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
     * Works independently of the selection mode (SongParts or RhythmParameters). Return a meaningful value only if selection is
     * not empty.
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
     * Works independently of the selection mode (SongParts or RhythmParameters). Return a meaningful value only if selection is
     * not empty.
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
     * @return The selected rp belonging to spt (only one RhythmParameter can be selected for one spt). Null if no rp selected in
     * spt.
     */
    public RhythmParameter<?> getSelectedSongPartParameter(SongPart spt)
    {
        if (isEmpty() || isSongPartSelected())
        {
            return null;
        }
        for (SongPartParameter sptp : songPartParameters)
        {
            if (sptp.getSpt() == spt)
            {
                return sptp.getRp();
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
     * The list of selected SongParts (ordered by startBarIndex), or corresponding to the selected RhythmParameters if
     * RhythmParameters are selected.
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
            res.add(rpp.getSpt());
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
            return songPartParameters.get(0).getSpt().getContainer();
        }
    }

    @Override
    public String toString()
    {
        return "SelectionUtilities songParts=" + songParts + " songPartParameters=" + songPartParameters;
    }

}
