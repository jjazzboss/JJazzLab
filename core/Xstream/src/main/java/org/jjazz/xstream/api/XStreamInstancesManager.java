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
package org.jjazz.xstream.api;

import com.thoughtworks.xstream.XStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.xstream.spi.XStreamConfigurator;
import org.openide.util.Lookup;

/**
 * Get access to the shared and configured XStream instances.
 * <p>
 * The configuration of the XStream instances is done by the XStreamConfigurator instances found in the global lookup. This allows the XStreamConfigurators to
 * add aliases even for private classes or classes from non-public packages of a Netbeans module.
 *
 * @see XStreamConfigurator
 */
public class XStreamInstancesManager
{

    private static XStreamInstancesManager INSTANCE;

    static public XStreamInstancesManager getInstance()
    {
        synchronized (XStreamInstancesManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new XStreamInstancesManager();
            }
        }
        return INSTANCE;
    }


    private XStream songLoadInstance;
    private XStream songSaveInstance;
    private XStream midiMixLoadInstance;
    private XStream midiMixSaveInstance;
    private static final Logger LOGGER = Logger.getLogger(XStreamInstancesManager.class.getSimpleName());

    private XStreamInstancesManager()
    {
    }

    /**
     * Get the XStream configured for Song loading.
     *
     * @return
     */
    public XStream getLoadSongInstance()
    {
        if (songLoadInstance == null)
        {
            songLoadInstance = getSecuredXStreamInstance();
            configureInstance(songLoadInstance, XStreamConfigurator.InstanceId.SONG_LOAD);
        }

        return songLoadInstance;
    }

    /**
     * Get the XStream configured for Song saving.
     *
     * @return
     */
    public XStream getSaveSongInstance()
    {
        if (songSaveInstance == null)
        {
            songSaveInstance = getSecuredXStreamInstance();
            configureInstance(songSaveInstance, XStreamConfigurator.InstanceId.SONG_SAVE);
        }

        return songSaveInstance;
    }

    /**
     * Get the XStream configured for MidiMix loading.
     *
     * @return
     */
    public XStream getLoadMidiMixInstance()
    {
        if (midiMixLoadInstance == null)
        {
            midiMixLoadInstance = getSecuredXStreamInstance();
            configureInstance(midiMixLoadInstance, XStreamConfigurator.InstanceId.MIDIMIX_LOAD);
        }

        return midiMixLoadInstance;
    }

    /**
     * Get the XStream configured for MidiMix saving.
     *
     * @return
     */
    public XStream getSaveMidiMixInstance()
    {
        if (midiMixSaveInstance == null)
        {
            midiMixSaveInstance = getSecuredXStreamInstance();
            configureInstance(midiMixSaveInstance, XStreamConfigurator.InstanceId.MIDIMIX_SAVE);
        }

        return midiMixSaveInstance;
    }

    // =======================================================================================================================
    // Private methods
    // =======================================================================================================================

    /**
     * Ask all XStreamConfigurator instances present in the global lookup to configure the specified XStream instance.
     *
     * @param xstream
     * @param id
     */
    private void configureInstance(XStream xstream, XStreamConfigurator.InstanceId id)
    {
        for (var configurator : Lookup.getDefault().lookupAll(XStreamConfigurator.class))
        {
            LOGGER.log(Level.FINE, "configureInstance() configurator={0} id={1}", new Object[]
            {
                configurator, id
            });
            configurator.configure(id, xstream);
        }
    }

    /**
     * Get a secured XStream instance for unmarshalling which only accepts org.jjazz.** objects.
     *
     * @return
     */
    private XStream getSecuredXStreamInstance()
    {
        XStream xstream = new XStream();
        xstream.allowTypesByWildcard(new String[]
        {
            "org.jjazz.**"
        });
        return xstream;
    }
}
