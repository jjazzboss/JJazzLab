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
package org.jjazz.embeddedsynth.spi;

import org.jjazz.embeddedsynth.api.Mp3Encoder;
import org.openide.util.Lookup;

/**
 * Provide a MP3 encoder.
 */
public interface Mp3EncoderProvider
{

    /**
     * Get the default Mp3Encoder (if any).
     * <p>
     * Rely on the first Mp3EncoderProvider found in the global lookup.
     *
     * @return Can be null.
     */
    static Mp3Encoder getDefault()
    {
        Mp3EncoderProvider provider = Lookup.getDefault().lookup(Mp3EncoderProvider.class);
        return provider == null ? null : provider.getMp3Encoder();
    }

    Mp3Encoder getMp3Encoder();
}
