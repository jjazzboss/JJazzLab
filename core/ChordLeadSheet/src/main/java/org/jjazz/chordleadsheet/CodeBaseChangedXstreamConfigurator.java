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
package org.jjazz.chordleadsheet;

import com.thoughtworks.xstream.XStream;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * Configurator to add package aliasing for loading old song files.
 */
@ServiceProvider(service = XStreamConfigurator.class)
public class CodeBaseChangedXstreamConfigurator implements XStreamConfigurator
{

    @Override
    public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
    {
        switch (instanceId)
        {
            case SONG_LOAD ->
            {
                // From 4.0 ChordLeadSheet packages were renamed from org.jjazz.leadsheet.chordleadsheet.* to org.jjazz.chordleadsheet.*
                xstream.aliasPackage("org.jjazz.leadsheet.chordleadsheet", "org.jjazz.chordleadsheet");
            }

            case MIDIMIX_LOAD, MIDIMIX_SAVE, SONG_SAVE ->
            {
                // Nothing
            }
            default ->
                throw new AssertionError(instanceId.name());
        }
    }
}
