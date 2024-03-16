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
package org.jjazz.midi.api.keymap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.spi.KeyMapProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provide the standard KeyMaps.
 */
@ServiceProvider(service = KeyMapProvider.class)
public class StdKeyMapProvider implements KeyMapProvider
{

    private ArrayList<DrumKit.KeyMap> keyMaps = new ArrayList<>();

    public StdKeyMapProvider()
    {
        keyMaps.add(KeyMapGM.getInstance());
        keyMaps.add(KeyMapGSGM2.getInstance());
        keyMaps.add(KeyMapXG.getInstance());
        keyMaps.add(KeyMapXG_PopLatin.getInstance());
    }

    @Override
    public List<DrumKit.KeyMap> getKeyMaps()
    {
        return Collections.unmodifiableList(keyMaps);
    }

}
