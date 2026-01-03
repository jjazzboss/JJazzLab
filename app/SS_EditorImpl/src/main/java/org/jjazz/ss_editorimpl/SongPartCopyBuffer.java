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
package org.jjazz.ss_editorimpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * Singleton class to manage SongParts copy/cut/paste operations.
 * <p>
 * SongParts must be contiguous.
 */
public class SongPartCopyBuffer
{

    static private SongPartCopyBuffer INSTANCE;
    /**
     * The buffer for SongParts, kept ordered by startBarIndex.
     */
    private ArrayList<SongPart> sptBuffer = new ArrayList<>();
    private int sptMinStartBarIndex;
    private int sptMaxStartBarIndex;
    private ArrayList<ChangeListener> listeners = new ArrayList<>();

    private SongPartCopyBuffer()
    {
    }

    public static SongPartCopyBuffer getInstance()
    {
        synchronized (SongPartCopyBuffer.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new SongPartCopyBuffer();
            }
        }
        return INSTANCE;
    }

    /**
     * Put a copy of each SongPart (and a copy of its parent section) in the buffer.
     * <p>
     * SongParts must be contiguous.<br>
     *
     * @param spts
     */
    public void put(List<SongPart> spts)
    {
        if (spts == null)
        {
            throw new IllegalArgumentException("spts=" + spts);
        }
        if (spts.isEmpty())
        {
            return;
        }
        sptMaxStartBarIndex = -1;
        sptMinStartBarIndex = 9999999;
        sptBuffer.clear();
        for (SongPart spt : spts)
        {

            // Update min/max barIndexes
            sptMinStartBarIndex = Math.min(sptMinStartBarIndex, spt.getStartBarIndex());
            sptMaxStartBarIndex = Math.max(sptMaxStartBarIndex, spt.getStartBarIndex());

            // Prepare parentsection copy
            CLI_Section parentSection = spt.getParentSection();
            CLI_Section sectionClone = (parentSection != null) ? (CLI_Section) parentSection.getCopy((Section) null, null) : null;

            // Add a copy of the song part with a copy of the parent section
            sptBuffer.add(spt.getCopy(spt.getRhythm(), spt.getStartBarIndex(), spt.getNbBars(), sectionClone));
        }

        // Sort our buffer
        Collections.sort(sptBuffer, new Comparator<SongPart>()
        {
            @Override
            public int compare(SongPart spt1, SongPart spt2
            )
            {
                return spt1.getStartBarIndex() - spt2.getStartBarIndex();
            }
        }
        );
        fireStateChanged();
    }

    public void clear()
    {
        sptMinStartBarIndex = 0;
        sptMaxStartBarIndex = 0;
        sptBuffer.clear();
        fireStateChanged();
    }

    /**
     * @return int The nb of RhyhmParts in the buffer.
     */
    public int getSize()
    {
        return sptBuffer.size();
    }

    public int getSptMinStartBarIndex()
    {
        return sptMinStartBarIndex;
    }

    public int getSptMaxStartBarIndex()
    {
        return sptMaxStartBarIndex;
    }

    public boolean isEmpty()
    {
        return sptBuffer.isEmpty();
    }

    /**
     * The SongStructure from which the songparts have been put in the buffer.
     *
     * @return Can be null if SongPartCopyBuffer is empty.
     */
    public SongStructure getSourceSongStructure()
    {
        return isEmpty() ? null : sptBuffer.get(0).getContainer();
    }

    /**
     * Return a copy of the SongParts adapted to the specified targetSgs.
     * <p>
     * The SongParts startBarIndexes are adjusted to start at targetStartBarIndex. <br>
     * nbBars is also adjusted if pasting in a different targetSgs.<br>
     * SongParts for which we could not find a matching parent section are discarded.
     *
     * @param targetSgs           The container of the returned SongParts. If null songparts container is unchanged.
     * @param targetStartBarIndex The first returned SongPart will have startBarIndex=targetStartBarIndex.
     * @return
     */
    public List<SongPart> get(SongStructure targetSgs, int targetStartBarIndex)
    {
        ArrayList<SongPart> spts = new ArrayList<>();
        if (isEmpty())
        {
            return spts;
        }

        int barShift = sptBuffer.get(0).getStartBarIndex() - targetStartBarIndex;
        if (targetSgs == null)
        {
            targetSgs = getSourceSongStructure();
        }

        ChordLeadSheet targetCls = targetSgs.getParentChordLeadSheet();
        if (targetCls == null)
        {
            // Special case: songStructure has no parent chordleadsheet (should not happen in normal operation)
            for (SongPart spt : sptBuffer)
            {
                // don't change the rhythm and set parent section to null
                spts.add(spt.getCopy(null, spt.getStartBarIndex() - barShift, spt.getNbBars(), null));
            }
        } else
        {
            // Normal case
            // Retrieve only a spt if we can find the same parentSection or an equivalent with same nam & timesignature.
            for (SongPart spt : sptBuffer)
            {
                CLI_Section parentSection = spt.getParentSection();
                TimeSignature parentSectionTs = spt.getParentSection().getData().getTimeSignature();
                CLI_Section newParentSection = targetCls.getSection(parentSection.getData().getName());
                if (newParentSection != null && newParentSection.getData().getTimeSignature().equals(parentSectionTs))
                {
                    int nbBars = targetCls.getBarRange(newParentSection).size();
                    spts.add(spt.getCopy(null, spt.getStartBarIndex() - barShift, nbBars, newParentSection));
                }
            }
        }
        return spts;
    }

    public void addChangeListener(ChangeListener cl)
    {
        if (!listeners.contains(cl))
        {
            listeners.add(cl);
        }
    }

    public void removeChangeListener(ChangeListener cl)
    {
        listeners.remove(cl);
    }

    private void fireStateChanged()
    {
        for (ChangeListener cl : listeners)
        {
            cl.stateChanged(new ChangeEvent(this));
        }
    }
}
