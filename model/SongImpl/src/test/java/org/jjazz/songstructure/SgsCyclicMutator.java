/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.songstructure;

import java.util.List;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_Variation;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.openide.util.Exceptions;

/**
 * Perform cyclic changes to a SongStructure.
 */
public class SgsCyclicMutator
{

    private int counter;
    private final SongStructure sgs;
    private final SongPart spt1;
    private final Rhythm r34_1, r34_2;
    private static final Logger LOGGER = Logger.getLogger(SgsCyclicMutator.class.getSimpleName());

    /**
     *
     * @param sgs Must contain at least 3 SongParts, 2nd SongPart must be in 3/4.
     */
    public SgsCyclicMutator(SongStructure sgs)
    {
        this.sgs = sgs;
        assert sgs.getSongParts().size() >= 2;
        spt1 = sgs.getSongParts().get(1);
        r34_1 = spt1.getRhythm();
        assert r34_1.getTimeSignature() == TimeSignature.THREE_FOUR : "r34_1=" + r34_1;

        var rdb = RhythmDatabase.getSharedInstance();
        var ri34All = rdb.getRhythms(TimeSignature.THREE_FOUR);
        var ri34bis = ri34All.stream()
                .filter(ri -> !ri.rhythmUniqueId().equals(r34_1.getUniqueId()))
                .toList().get(0);
        try
        {
            r34_2 = rdb.getRhythmInstance(ri34bis);
        } catch (UnavailableRhythmException ex)
        {
            Exceptions.printStackTrace(ex);
            throw new IllegalArgumentException("ex=" + ex);
        }
    }

    public void mutate() throws UnsupportedEditException
    {
        switch (counter % 5)
        {
            case 0 ->
            {
                // Add a song part at the end
                int sgsSize = sgs.getSizeInBars();
                if (sgsSize > 0)
                {
                    var firstSpt = sgs.getSongParts().getFirst();
                    var lastSpt = firstSpt.getCopy(null, sgsSize, firstSpt.getNbBars(), null);
                    sgs.addSongParts(List.of(lastSpt));
                }
            }

            case 1 ->
            {
                // Remove the last song part
                List<SongPart> spts = sgs.getSongParts();
                if (spts.size() >= 3)
                {
                    sgs.removeSongParts(List.of(spts.getLast()));
                }
            }

            case 2 ->
            {
                // Change spt1 rhythm
                Rhythm newRhythm = null;
                if (sgs.getSongParts().contains(spt1) && spt1.getRhythm().getTimeSignature() == TimeSignature.THREE_FOUR)
                {
                    newRhythm = spt1.getRhythm() == r34_1 ? r34_2 : r34_1;
                    sgs.setSongPartsRhythm(List.of(spt1), newRhythm, null);
                }
            }

            case 3 ->
            {
                // Change song part name
                if (!sgs.getSongParts().isEmpty())
                {
                    var spt0 = sgs.getSongParts().get(0);
                    sgs.setSongPartsName(List.of(spt0), "Modified-" + counter);
                }
            }

            case 4 ->
            {
                // Change rhythm parameter
                if (sgs.getSongParts().contains(spt1))
                {
                    Rhythm r = spt1.getRhythm();
                    RP_SYS_Variation rp = RP_SYS_Variation.getVariationRp(r);
                    if (rp != null)
                    {
                        String currentValue = spt1.getRPValue(rp);
                        String newValue = rp.getNextValue(currentValue);
                        sgs.setRhythmParameterValue(spt1, rp, newValue);
                    }
                }
            }
        }
        counter++;
    }
}
