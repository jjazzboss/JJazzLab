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
package org.jjazz.midi.drummap;

import org.jjazz.midi.DrumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.openide.util.Lookup;

/**
 * Manage the DrumMaps and the DrumMapConverters.
 * <p>
 * Upon startup retrieve :<br>
 * - All the DrumMapConverters found on the global lookup. <br>
 * - The standard DrumMaps plus all the DrumMaps provided by the DrumMapProviders found on the global lookup.<p>
 * The standard DrumMaps are the GM/GS_GM2/XG DrumMaps.
 */
public class DrumMapManager
{

    /**
     * A special transparent DrumMapConverter who does nothing.
     */
    public static final DrumMapConverter TRANSPARENT_CONVERTER = new TransparentConverter();
    private static DrumMapManager INSTANCE;
    private ArrayList<DrumMapConverter> converters = new ArrayList<>();
    private ArrayList<DrumMap> drumMaps = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(DrumMapManager.class.getSimpleName());

    public static DrumMapManager getInstance()
    {
        synchronized (DrumMapManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DrumMapManager();
            }
        }
        return INSTANCE;
    }

    private DrumMapManager()
    {
        for (DrumMapConverter dmc : Lookup.getDefault().lookupAll(DrumMapConverter.class))
        {
            converters.add(dmc);
        }
        drumMaps.add(DrumMapGM.getInstance());
        drumMaps.add(DrumMapGSGM2.getInstance());
        drumMaps.add(DrumMapXG.getInstance());
        for (DrumMapProvider dmp : Lookup.getDefault().lookupAll(DrumMapProvider.class))
        {
            drumMaps.addAll(dmp.getDrumMaps());
        }
    }

    /**
     * Get all the DrumMapConverters.
     *
     * @return
     */
    public List<DrumMapConverter> getConverters()
    {
        return new ArrayList(converters);
    }

    /**
     * Get all the DrumMaps.
     *
     * @return The list contains at least the standard DrumMaps.
     */
    public List<DrumMap> getDrumMaps()
    {
        return new ArrayList(drumMaps);
    }

    /**
     * Get a DrumMap from its name.
     *
     * @param drumMapName
     * @return Can be null if not found
     */
    public DrumMap getDrumMap(String drumMapName)
    {
        DrumMap res = null;
        for (DrumMap map : drumMaps)
        {
            if (map.getName().equals(drumMapName))
            {
                res = map;
                break;
            }
        }
        return res;
    }

    /**
     * Get a converter to map notes from srcDrMap to destMap.
     * <p>
     * Try to find a specific converter. If not found, return the NO_MAPPER instance.
     *
     * @param srcMap
     * @param destMap
     * @return Can't be null.
     */
    public DrumMapConverter getConverter(DrumMap srcMap, DrumMap destMap)
    {
        DrumMapConverter res = TRANSPARENT_CONVERTER;
        for (DrumMapConverter dkm : converters)
        {
            if (dkm.acceptSrcDrumMap(srcMap) && dkm.acceptDestDrumMap(destMap))
            {
                res = dkm;
                break;
            }
        }
        return res;
    }

    /**
     * ²
     * This mapper does no mapping, it just returns the source note.
     */
    static private class TransparentConverter implements DrumMapConverter
    {

        @Override
        public String getConverterId()
        {
            return "NoConverter";
        }

        @Override
        public boolean acceptSrcDrumMap(DrumMap map)
        {
            return true;
        }

        @Override
        public boolean acceptDestDrumMap(DrumMap map)
        {
            return true;
        }

        @Override
        public int mapNote(DrumMap srcDrMap, int srcPitch, DrumMap destMap)
        {
            return srcPitch;
        }

    }
}
