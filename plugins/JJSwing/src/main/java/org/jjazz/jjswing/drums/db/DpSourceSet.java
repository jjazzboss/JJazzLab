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
package org.jjazz.jjswing.drums.db;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Store the STD and optional FILL DpSources of a given DrumsStyle.
 * <p>
 * All DpSources have the same size.
 */
public record DpSourceSet(List<DpSource> dpSourcesStd, List<DpSource> dpSourcesFill)
        {

    private static final Logger LOGGER = Logger.getLogger(DpSourceSet.class.getSimpleName());

    /**
     * Check consistency of altId values.
     *
     * @param dpSources     Can not be empty
     * @param dpSourcesFill Can be empty
     */
    public DpSourceSet(List<DpSource> dpSourcesStd, List<DpSource> dpSourcesFill)
    {
        Preconditions.checkArgument(!dpSourcesStd.isEmpty());
        int alt = 0;
        int size = dpSourcesStd.get(0).getSizeInBars();
        for (var dps : dpSourcesStd)
        {
            if (dps.getAlternateId() != alt || dps.getSizeInBars() != size)
            {
                throw new IllegalArgumentException("alt=" + alt + "  dpSources=" + dpSourcesStd);
            }
            alt++;
        }
        alt = 0;
        if (!dpSourcesFill.isEmpty())
        {
            size = dpSourcesFill.get(0).getSizeInBars();
            for (var dps : dpSourcesFill)
            {
                if (dps.getAlternateId() != alt || dps.getSizeInBars() != size)
                {
                    throw new IllegalArgumentException("alt=" + alt + "  dpSourcesFill=" + dpSourcesFill);
                }
                alt++;
            }
        }
        this.dpSourcesStd = List.copyOf(dpSourcesStd);
        this.dpSourcesFill = List.copyOf(dpSourcesFill);
    }

    /**
     * Size in bars of the DpSources.
     *
     * @return
     */
    public int getSize()
    {
        return dpSourcesStd.get(0).getSizeInBars();
    }

    /**
     * Get the DpSources of specified type.
     *
     * @param type
     * @return
     */
    public List<DpSource> getDpSources(DpSource.Type type)
    {
        return type == DpSource.Type.STD ? dpSourcesStd : dpSourcesFill;
    }

    public void dump()
    {
        for (var dps : dpSourcesStd)
        {
            LOGGER.log(Level.INFO, "   {0}:", dps);
        }
        for (var dps : dpSourcesFill)
        {
            LOGGER.log(Level.INFO, "   {0}:", dps);
        }
    }

}
