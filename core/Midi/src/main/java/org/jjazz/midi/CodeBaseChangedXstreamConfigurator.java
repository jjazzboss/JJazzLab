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
package org.jjazz.midi;

import com.thoughtworks.xstream.XStream;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * Configurator to add package aliasing for loading old Mix files.
 */
@ServiceProvider(service = XStreamConfigurator.class)
public class CodeBaseChangedXstreamConfigurator implements XStreamConfigurator
{

    @Override
    public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
    {
        switch (instanceId)
        {
            case SONG_LOAD, SONG_SAVE, MIDIMIX_SAVE ->
            {
                // Nothing
            }

            case MIDIMIX_LOAD ->
            {
                // From 3.0 all public packages were renamed with api or spi somewhere in the path
                // Need package aliasing to be able to load old sng/mix files
                xstream.aliasPackage("org.jjazz.midi.api", "org.jjazz.midi.api");           // Make sure new package name is not replaced by next alias
                xstream.aliasPackage("org.jjazz.midi", "org.jjazz.midi.api");
            }
            default ->
                throw new AssertionError(instanceId.name());
        }
    }
}
