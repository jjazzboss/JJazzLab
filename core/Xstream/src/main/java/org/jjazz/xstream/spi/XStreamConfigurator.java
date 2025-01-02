/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.xstream.spi;

import com.thoughtworks.xstream.XStream;

/**
 * Configure the Xstream instances managed by JJazzLab to load/save Song and load/save MidiMix.
 * <p>
 *
 * @see org.jjazz.xstream.api.XStreamInstancesManager
 */
public interface XStreamConfigurator
{

    public enum InstanceId
    {
        SONG_LOAD, SONG_SAVE, MIDIMIX_LOAD, MIDIMIX_SAVE
    }

    /**
     * Configure the specified XStream instance.
     *
     * @param instanceId 
     * @param xstream
     */
    void configure(InstanceId instanceId, XStream xstream);
}
