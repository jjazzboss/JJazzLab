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
package org.jjazz.undomanager.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores JJazzUndoManager instances, e.g. one per song.
 */
public class JJazzUndoManagerFinder
{

    private static JJazzUndoManagerFinder INSTANCE;
    private final JJazzUndoManager defaultUndoManager;
    private final Map<Object, JJazzUndoManager> map;

    public static JJazzUndoManagerFinder getDefault()
    {
        synchronized (JJazzUndoManagerFinder.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new JJazzUndoManagerFinder();
            }
            return INSTANCE;
        }
    }

    private JJazzUndoManagerFinder()
    {
        map = new HashMap<>();
        defaultUndoManager = new JJazzUndoManager();
    }

    /**
     * Store a JJazzUndoManager and associate it to a specified object key.
     *
     * @param um
     * @param key
     */
    public void put(Object key, JJazzUndoManager um)
    {
        assert um != null && key != null;   
        map.put(key, um);
    }
    
    public void remove(Object key)
    {
        map.remove(key);
    }

    /**
     * Retrieve the JJazzUndoManager associated to key.
     *
     * @param key
     * @return
     */
    public JJazzUndoManager get(Object key)
    {
        return map.get(key);
    }

    public JJazzUndoManager getDefaultUndoManager()
    {
        return defaultUndoManager;
    }
}
