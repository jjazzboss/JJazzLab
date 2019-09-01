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
package org.jjazz.undomanager;

import org.jjazz.util.SmallMap;

/**
 * Stores JJazzUndoManager instances, e.g. one per song.
 */
public class JJazzUndoManagerFinder
{

    private static JJazzUndoManagerFinder INSTANCE;
    private JJazzUndoManager defaultUndoManager;
    private SmallMap<Object, JJazzUndoManager> map;

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
        map = new SmallMap<>();
        defaultUndoManager = new JJazzUndoManager();
    }

    /**
     * Store a JJazzUndoManager and associate it to a specified object key.
     *
     * @param um
     * @param key
     */
    public void put(JJazzUndoManager um, Object key)
    {
        assert um != null && key != null;
        map.putValue(key, um);
    }

    /**
     * Retrieve the JJazzUndoManager associated to key.
     *
     * @param key
     * @return
     */
    public JJazzUndoManager get(Object key)
    {
        return map.getValue(key);
    }

    public JJazzUndoManager getDefaultUndoManager()
    {
        return defaultUndoManager;
    }
}
