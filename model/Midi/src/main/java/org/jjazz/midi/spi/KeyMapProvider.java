/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midi.spi;

import java.util.ArrayList;
import java.util.List;
import org.jjazz.midi.api.DrumKit;
import org.openide.util.Lookup;

/**
 * A provider for DrumKit.KeyMaps.
 */
public interface KeyMapProvider
{

    static public class Util
    {

        /**
         * Search for a matching KeyMap with specified name in all KeyMaps of all KeyMapProviders.
         *
         * @param name
         * @return Null if not found.
         */
        static public DrumKit.KeyMap getKeyMap(String name)
        {
            if (name == null)
            {
                throw new NullPointerException("name");   
            }
            DrumKit.KeyMap res = null;
            for (KeyMapProvider kmp : getKeyMapProviders())
            {
                for (DrumKit.KeyMap map : kmp.getKeyMaps())
                {
                    if (map.getName().equals(name))
                    {
                        res = map;
                        break;
                    }
                }
            }
            return res;
        }

        /**
         * Get all the KeyMaps from all the KeyMapProviders.
         *
         * @return
         */
        static public List<DrumKit.KeyMap> getKeyMaps()
        {
            ArrayList<DrumKit.KeyMap> res = new ArrayList<>();
            for (KeyMapProvider kmp : getKeyMapProviders())
            {
                res.addAll(kmp.getKeyMaps());
            }
            return res;
        }

        /**
         * Get all the KeyMapProviders present in the global lookup.
         *
         * @return Can be empty
         */
        static public List<KeyMapProvider> getKeyMapProviders()
        {
            ArrayList<KeyMapProvider> res = new ArrayList<>();
            for (KeyMapProvider kmp : Lookup.getDefault().lookupAll(KeyMapProvider.class))
            {
                res.add(kmp);
            }
            return res;
        }
    }

    /**
     * Provide a list of KeyMaps.
     *
     * @return
     */
    List<DrumKit.KeyMap> getKeyMaps();
}
