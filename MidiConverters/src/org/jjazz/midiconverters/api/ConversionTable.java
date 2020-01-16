/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midiconverters.api;

import java.util.HashMap;

/**
 * A simple class to store bank index conversion maps.
 */
public class ConversionTable
{

    private String id;
    private HashMap<Integer, Integer> map;
    private HashMap<Integer, Integer> reverseMap;

    /**
     *
     * @param id
     * @param mapFromTo
     */
    public ConversionTable(String id, HashMap<Integer, Integer> mapFromTo)
    {
        if (id == null || id.isEmpty() || map == null)
        {
            throw new IllegalArgumentException("id=" + id + " map=" + map);
        }
        this.map = map;
    }

    public HashMap<Integer, Integer> getMapFromTo()
    {
        return map;
    }

    public HashMap<Integer, Integer> getMapToFrom()
    {
        if (reverseMap == null)
        {
            reverseMap = new HashMap<>((int) (map.size() / 0.7f));     // With default LoadFactor of 0.75, this should avoid rehash
            for (int to : map.keySet())
            {
                reverseMap.put(map.get(to), to);
            }
        }
        return reverseMap;
    }

    @Override
    public String toString()
    {
        return id;
    }
}
