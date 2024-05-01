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
package org.jjazz.midi.api;

import com.thoughtworks.xstream.XStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import org.jjazz.midi.api.keymap.KeyRange;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midi.spi.KeyMapProvider;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * The main parameters of a drum kit instrument: a drum/key map and its ambiance type.
 * <p>
 * This is an immutable class.
 */
public class DrumKit implements Serializable
{

    /**
     * Subset of instruments (pitches) of a in a KeyMap.
     */
    public enum Subset
    {
        BASS, // All bass drums
        BASS_DEFAULT, // Default bass
        SNARE, // All snares
        SNARE_DEFAULT, // the default snare
        SNARE_BRUSH,
        SNARE_RIMSHOT, // only rimshots
        SNARE_HANDCLAP, // onlu hand claps
        SNARE_DRUM, // snare drums except rimshots
        HI_HAT, // All Hi-Hats
        HI_HAT_PEDAL,
        HI_HAT_CLOSED, HI_HAT_OPEN,
        TOM,
        CRASH,
        CYMBAL,
        PERCUSSION,
        ACCENT  // Contains at least kicks and snares, crash cymbals and open hi-hats should be included
    }

    /**
     * Defines a key map for a drumkit: associate a percussion name to a note pitch.
     * <p>
     * 36=Kick, 37=Rimshot, 38=Snare, etc.
     */
    public interface KeyMap extends Serializable
    {

        /**
         * The key getRange of this KeyMap.
         *
         * @return
         */
        public KeyRange getRange();

        /**
         * The name of the DrumKitKeyMap.
         *
         * @return
         */
        public String getName();

        /**
         * True if this KeyMap contains otherKeyMap.
         * <p>
         * E.g. the GM2 KeyMap contains the GM keymap (but not the other way around): a GM-keymap-based rhythm can be played on a GM2-keymap-based drums
         * instrument. Should return true if this keymap==otherKeyMap.
         *
         * @param otherKeyMap
         * @return
         */
        public boolean isContaining(KeyMap otherKeyMap);

        /**
         * The isntrument name, eg "Kick" for the given key.
         *
         * @param pitch
         * @return Can be null if pitch is not used by this DrumKitKeyMap.
         */
        public String getKeyName(int pitch);

        /**
         * The pitch corresponding to the note name.
         *
         * @param noteName
         * @return -1 if noteName is not used by this DrumKitKeyMap.
         */
        public int getKey(String noteName);

        /**
         * Get the notes of the given subset(s).
         * <p>
         *
         * @param subsets
         * @return Can be an empty list.
         */
        public List<Integer> getKeys(Subset... subsets);

    }

    /**
     * The main ambience types based on GM2 standard.
     */
    public enum Type
    {
        STANDARD, ROOM, POWER, ELECTRONIC, ANALOG, JAZZ, BRUSH, ORCHESTRA, SFX;
    }

    private Type type;
    private KeyMap map;
    private static final Logger LOGGER = Logger.getLogger(DrumKit.class.getSimpleName());

    /**
     * Create a DrumKit with type=STANDARD and keyMap=GM
     */
    public DrumKit()
    {
        type = Type.STANDARD;
        map = KeyMapGM.getInstance();
    }

    public DrumKit(Type type, KeyMap map)
    {
        if (type == null || map == null)
        {
            throw new IllegalArgumentException("type=" + type + " map=" + map);
        }
        this.type = type;
        this.map = map;
    }

    /**
     * @return the type
     */
    public Type getType()
    {
        return type;
    }

    /**
     * @return the map
     */
    public KeyMap getKeyMap()
    {
        return map;
    }

    /**
     * Get a map which provide the subset corresponding to a pitch for this DrumKit.
     *
     * @param subsets Limit the map values to these subsets only. If no value specified, use all the possible Subsets values.
     * @return
     */
    public Map<Integer, Subset> getSubsetPitches(Subset... subsets)
    {
        Map<Integer, Subset> res = new HashMap<>();
        if (subsets.length == 0)
        {
            subsets = Subset.values();
        }
        for (var subset : subsets)
        {
            for (int pitch : getKeyMap().getKeys(subset))
            {
                assert res.get(pitch) == null : "subset=" + subset + " pitch=" + pitch + " res.get(pitch)=" + res.get(pitch);
                res.put(pitch, subset);
            }
        }
        return res;
    }

    @Override
    public String toString()
    {
        return "[Type:" + type + ", KeyMap:" + map.getName() + "]";
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.type);
        hash = 17 * hash + Objects.hashCode(this.map);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final DrumKit other = (DrumKit) obj;
        if (this.type != other.type)
        {
            return false;
        }
        if (!Objects.equals(this.map, other.map))
        {
            return false;
        }
        return true;
    }

    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    // Nothing
                }

                case MIDIMIX_LOAD, MIDIMIX_SAVE ->
                {
                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files                    
                    xstream.alias("DrumKit", DrumKit.class);
                    xstream.alias("DrumKitSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spType");
                    xstream.useAttributeFor(SerializationProxy.class, "spKeyMapName");
                }
                default -> throw new AssertionError(instanceId.name());
            }
        }
    }

    // --------------------------------------------------------------------- 
    // Serialization
    // ---------------------------------------------------------------------
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");

    }

    /**
     * RhythmVoices must be stored in a simplified way in order to avoid storing rhythm stuff which depend on InstrumentBanks which are themselves system
     * dependent.
     * <p>
     * Also need to do some cleaning: mapInstruments can contain useless entries if some songparts have been removed .
     * <p>
     * spVERSION 2 introduces XStream aliases (XStreamConfig)
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -10218260387192L;
        private int spVERSION = 2;      // Do not make final!
        private Type spType;
        private String spKeyMapName;

        private SerializationProxy(DrumKit kit)
        {
            spType = kit.type;
            spKeyMapName = kit.map.getName();
        }

        private Object readResolve() throws ObjectStreamException
        {
            DrumKit kit;

            // Retrieve the KeyMap from name
            KeyMap map = KeyMapProvider.Util.getKeyMap(spKeyMapName);
            if (map == null)
            {
                map = KeyMapGM.getInstance();
                LOGGER.log(Level.WARNING, "readResolve() Can''t find KeyMap from name={0}. Using GM keymap instead.", spKeyMapName);
            }

            // Rebuild the instance
            kit = new DrumKit(spType, map);
            return kit;
        }
    }


}
