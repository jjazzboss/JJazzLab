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
package org.jjazz.yamjjazz.rhythm;

import java.util.ArrayList;
import java.util.List;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.spi.KeyMapProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provider of specific KeyMaps used by YamJJazz.
 */
@ServiceProvider(service = KeyMapProvider.class)
public class YamJazzKeyMapProvider implements KeyMapProvider
{

    private final List<DrumKit.KeyMap> keyMaps;

    public YamJazzKeyMapProvider()
    {        
        this.keyMaps = new ArrayList<>();
    }

    @Override
    public List<DrumKit.KeyMap> getKeyMaps()
    {
        return keyMaps;
    }

}
