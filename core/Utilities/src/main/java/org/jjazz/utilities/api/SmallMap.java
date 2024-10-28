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
package org.jjazz.utilities.api;

import java.io.Serializable;
import java.util.*;

/**
 * A silly but fast map for small maps, based on simple ArrayLists and iterative search.
 *
 * @param <K>
 * @param <V>
 * @deprecated
 */
public class SmallMap<K, V> implements Serializable
{

    private static final long serialVersionUID = 7816259002201L;
    private final int VERSION = 1;
    protected ArrayList<K> keys = new ArrayList<>();
    protected ArrayList<V> values = new ArrayList<>();

    /**
     * Create an empty SillyMap.
     */
    public SmallMap()
    {
        // Nothing       
    }

    /**
     * Create a SillyMap initialized with a single key/value pair.
     *
     * @param key
     * @param value
     */
    public SmallMap(K key, V value)
    {
        putValue(key, value);
    }

    public void clear()
    {
        keys.clear();
        values.clear();
    }

    /**
     * @return A shallow copy of this map.
     */
    @Override
    public SmallMap<K, V> clone()
    {
        SmallMap<K, V> result = new SmallMap<>();
        result.keys = new ArrayList<>(keys);
        result.values = new ArrayList<>(values);
        return result;
    }

    public List<K> getKeys()
    {
        return keys;
    }

    public List<V> getValues()
    {
        return values;
    }

    /**
     * Store value associated to key, replacing existing value if key already present in the map.
     *
     * @param key
     * @param value
     */
    public void putValue(K key, V value)
    {
        int index = keys.indexOf(key);
        if (index == -1)
        {
            keys.add(key);
            values.add(value);
        } else
        {
            values.set(index, value);
        }
    }

    public void remove(K key)
    {
        int index = keys.indexOf(key);
        if (index != -1)
        {
            keys.remove(index);
            values.remove(index);
        }
    }

    /**
     *
     * @param key K
     * @return V Null if not found.
     */
    public V getValue(K key)
    {
        int index = keys.indexOf(key);
        return (index == -1) ? null : values.get(index);
    }

    /**
     *
     * @param value V
     * @return K Null if not found
     */
    public K getKey(V value)
    {
        int index = values.indexOf(value);
        return (index == -1) ? null : keys.get(index);
    }

    public boolean isEmpty()
    {
        return keys.isEmpty();
    }

    public int size()
    {
        return keys.size();
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (K key : keys)
        {
            sb.append("[").append(key).append("]=").append(getValue(key)).append(" ");
        }
        return sb.toString();
    }
}
