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
package org.jjazz.phrase.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.jjazz.midi.api.MidiConst;

/**
 * A set of phrases for different channels.
 * <p>
 * Some clientProperties can be attached.
 */
public class SourcePhraseSet
{
    public final static String PROP_ID = "PropId";
    private final HashMap<Integer, SourcePhrase> mapChannelPhrase = new HashMap<>();
    private HashMap<String, String> clientProperties;

    public SourcePhraseSet()
    {
        this(null);
    }

    /**
     * Create an empty SourcePhraseSet and set the PROP_ID client property with the specified id.
     *
     * @param id Not used if null or empty string.
     * @see PROP_ID
     */
    public SourcePhraseSet(String id)
    {
        if (id != null && !id.trim().isEmpty())
        {
            setClientProperty(PROP_ID, id);
        }
    }

    /**
     * Get a the SourcePhrase for specified channel.
     *
     * @param channel
     * @return Null if not set.
     */
    public SourcePhrase getPhrase(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);   
        }
        SourcePhrase sp = mapChannelPhrase.get(channel);
        return sp;
    }

    /**
     * An ordered (ascending) list of channels for which there is a SourcePhrase.
     *
     * @return
     */
    public List<Integer> getSourceChannels()
    {
        ArrayList<Integer> res = new ArrayList<>(mapChannelPhrase.keySet());
        Collections.sort(res);
        return res;
    }

    /**
     * Associate a SourcePhrase for a specific channel.
     *
     * @param channel
     * @param sp If null then no SourcePhrase is set for the specified channel.
     */
    public void setPhrase(int channel, SourcePhrase sp)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel + " sp=" + sp);   
        }
        if (sp == null)
        {
            mapChannelPhrase.remove(channel);
        } else
        {
            mapChannelPhrase.put(channel, sp);
        }
    }

    /**
     * Remove a SourcePhrase.
     *
     * @param channel
     * @return The removed SourcePhrase, or null.
     */
    public SourcePhrase removeSourcePhrase(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);   
        }
        return mapChannelPhrase.remove(channel);
    }

    /**
     * Remove all the phrases.
     */
    public void clear()
    {
        mapChannelPhrase.clear();
    }

    /**
     * Set a client property to this object.
     *
     * @param propName
     * @param propValue If null, this removes the property.
     */
    public final void setClientProperty(String propName, String propValue)
    {
        if (propName == null)
        {
            throw new IllegalArgumentException("propName=" + propName + " propValue=" + propValue);   
        }
        if (clientProperties == null)
        {
            clientProperties = new HashMap<>();
        }
        if (propValue == null)
        {
            clientProperties.remove(propName);
        } else
        {
            clientProperties.put(propName, propValue);
        }
    }

    /**
     * Get a client property.
     *
     * @param propName
     * @return Null if property not set.
     */
    public String getClientProperty(String propName)
    {
        if (propName == null)
        {
            throw new IllegalArgumentException("propName=" + propName);   
        }
        return clientProperties.get(propName);
    }

}
