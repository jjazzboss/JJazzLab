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
package org.jjazz.midi.drumkit;

import java.util.ArrayList;
import java.util.logging.Logger;
import org.openide.util.Lookup;

/**
 * Manage the DrumKits and the DrumKitMappers.
 */
public class DrumKitManager
{

    /**
     * A special transparent DrumKitMapper who does nothing.
     */
    public static final DrumKitMapper TRANSPARENT_MAPPER = new TransparentMapper();
    private static DrumKitManager INSTANCE;
    private ArrayList<DrumKitMapper> mappers = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(DrumKitManager.class.getSimpleName());

    public static DrumKitManager getInstance()
    {
        synchronized (DrumKitManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DrumKitManager();
            }
        }
        return INSTANCE;
    }

    private DrumKitManager()
    {
        // Retrieve the available mappers
        for (DrumKitMapper p : Lookup.getDefault().lookupAll(DrumKitMapper.class))
        {
            mappers.add(p);
        }
    }

    /**
     * Get a mapper to map notes from srcKit to destKit.
     * <p>
     * Try to find a specific mapper. If not found, return the NO_MAPPER instance.
     *
     * @param srcKit
     * @param destKit
     * @return Can't be null.
     */
    public DrumKitMapper getMapper(DrumKit srcKit, DrumKit destKit)
    {
        DrumKitMapper res = TRANSPARENT_MAPPER;
        for (DrumKitMapper dkm : mappers)
        {
            if (dkm.acceptSrcKit(srcKit) && dkm.acceptDestKit(destKit))
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
    static private class TransparentMapper implements DrumKitMapper
    {

        @Override
        public String getMapperId()
        {
            return "NoMapper";
        }

        @Override
        public boolean acceptSrcKit(DrumKit kit)
        {
            return true;
        }

        @Override
        public boolean acceptDestKit(DrumKit kit)
        {
            return true;
        }

        @Override
        public int mapNote(DrumKit srcKit, int srcPitch, DrumKit destKit)
        {
            return srcPitch;
        }

    }
}
