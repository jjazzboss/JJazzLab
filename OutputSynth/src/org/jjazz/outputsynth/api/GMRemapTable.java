/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.outputsynth.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midi.api.synths.Family;

/**
 * The table can associate an Instrument from a MidiSynthList to each of the GM1 instruments + the special DRUMS/PERCUSSION static
 * instances.
 * <p>
 * Association can also be done at the GM1Bank.Family level.
 */
public class GMRemapTable implements Serializable
{

    /**
     * Specific exception for this object.
     */
    public class InvalidMappingException extends Exception
    {

        public InvalidMappingException(String msg)
        {
            super(msg);
        }
    }

    /**
     * Special instances for the Drums/Percussion, since GM1 does not define them.
     */
    public static final Instrument DRUMS_INSTRUMENT = new Instrument(0, "Drums");
    public static final Instrument PERCUSSION_INSTRUMENT = new Instrument(1, "Percussion");

    /**
     * oldValue=Family, newValue=Instrument
     */
    public static final String PROP_FAMILY = "Family";   //NOI18N 
    /**
     * oldValue=GM1Instrument or the special DRUMS/PERCUSSION static instances, newValue=Instrument
     */
    public static final String PROP_INSTRUMENT = "Instrument";   //NOI18N 
    private HashMap<Instrument, Instrument> mapInstruments = new HashMap<>();
    private HashMap<Family, Instrument> mapFamilyInstruments = new HashMap<>();
    private final MidiSynthList midiSynthList;
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(GMRemapTable.class.getSimpleName());

    /**
     * Create an instance to remap GM instruments to instruments from the specified MidiSynthList.
     * <p>
     * @param synthList
     */
    public GMRemapTable(MidiSynthList synthList)
    {
        Preconditions.checkNotNull(synthList);
        midiSynthList = synthList;
    }

    /**
     * Create a table which copies the value from rt.
     *
     * @param rt
     */
    public GMRemapTable(GMRemapTable rt)
    {
        mapInstruments = new HashMap<>(rt.mapInstruments);
        mapFamilyInstruments = new HashMap<>(rt.mapFamilyInstruments);
        midiSynthList = rt.midiSynthList;
    }

    /**
     * The associated MidiSynthList.
     *
     * @return
     */
    public MidiSynthList getMidiSynthList()
    {
        return midiSynthList;
    }


    /**
     * The map which associates a GM instrument (+ the special drums instances) to an instrument from the associated
     * MidiSynthList.
     *
     * @return
     */
    public HashMap<Instrument, Instrument> getInstrumentMap()
    {
        return new HashMap<>(mapInstruments);
    }

    /**
     * The map which associates a GM family to an instrument from the associated MidiSynthList.
     *
     * @return
     */
    public HashMap<Family, Instrument> getFamilyInstrumentMap()
    {
        return new HashMap<>(mapFamilyInstruments);
    }

    /**
     * Remove all mappings.
     */
    public void clear()
    {
        for (Instrument ins : getInstrumentMap().keySet())
        {
            setInstrumentNoException(ins, null, true);
        }
    }

    /**
     * Check that the specified remapped Instrument is valid.
     *
     * @param remappedIns
     * @throws IllegalArgumentException If invalid value
     */
    static public void checkRemappedInstrument(Instrument remappedIns)
    {
        if (remappedIns == null || remappedIns != DRUMS_INSTRUMENT && remappedIns != PERCUSSION_INSTRUMENT && !(remappedIns instanceof GM1Instrument))
        {
            throw new IllegalArgumentException("remappedIns");   //NOI18N
        }
    }

    /**
     * Get the mapped instrument for remappedIns.
     *
     * @param remappedIns Must be a GM1Instrument or the special DRUMS/PERCUSSION static instances.
     * @return Null if no mapping defined for remappedIns. Returned instrument belongs to the associated MidiSynthList.
     */
    public Instrument getInstrument(Instrument remappedIns)
    {
        checkRemappedInstrument(remappedIns);
        return mapInstruments.get(remappedIns);
    }

    /**
     * Set the mapped instrument for remappedIns.
     *
     * @param remappedIns        Must be a GM1Instrument or the special DRUMS/PERCUSSION static instances.
     * @param ins                Can be null. If remappedIns is one of the special DRUMS/PERCUSSION instances, ins must be a
     *                           Drums/Perc instrument with a GM compatible DrumsKit.KeyMap. ins must belong to the associated
     *                           MidiSynthList.
     * @param useAsFamilyDefault If true ins will be also the default instrument for the remappedIns's family. Not used if
     *                           remappedIns is one of the special DRUMS/PERCUSSION instances.
     * @throws InvalidMappingException If arguments are invalid. The exception error message can be used for user notification.
     */
    public void setInstrument(Instrument remappedIns, Instrument ins, boolean useAsFamilyDefault) throws InvalidMappingException
    {
        checkRemappedInstrument(remappedIns);
        if (ins == remappedIns)
        {
            throw new InvalidMappingException("Invalid instrument: " + ins.getFullName() + " is not different from the mapped instrument.");
        }
        if (ins != null && !ins.isDrumKit() && (remappedIns == DRUMS_INSTRUMENT || remappedIns == PERCUSSION_INSTRUMENT))
        {
            throw new InvalidMappingException("Invalid instrument: " + ins.getFullName() + " is not a Drums/Percussion instrument.");
        }
        if (ins != null && ins.isDrumKit() && !ins.getDrumKit().getKeyMap().isContaining(KeyMapGM.getInstance()))
        {
            throw new InvalidMappingException("Invalid instrument: " + ins.getFullName() + " drum kit keymap (" + ins.getDrumKit().getKeyMap().getName() + ") is not GM-compatible.");
        }
        if (ins != null && !midiSynthList.contains(ins))
        {
            throw new IllegalArgumentException("Invalid instrument: " + ins.getFullName() + " does not belong to associated midiSynthList=" + midiSynthList);
        }
        setInstrumentNoException(remappedIns, ins, useAsFamilyDefault);
    }

    /**
     * Get the mapped instrument for the specified instrument family.
     *
     * @param family
     * @return Null if no mapping defined for this family.
     */
    public Instrument getInstrument(Family family)
    {
        if (family == null)
        {
            throw new NullPointerException("family");   //NOI18N
        }
        return mapFamilyInstruments.get(family);
    }
    
    public String saveAsString()
    {
        var sj = new StringJoiner(";", "[", "]");
        for (var insKey : mapInstruments.keySet())
        {
            sj.add(insKey.)
        }
    }
    
    public void setFromString(String s)
    {
        
    }


    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }


    // --------------------------------------------------------------------- 
    // Private methods
    // --------------------------------------------------------------------- 
    private void setInstrumentNoException(Instrument remappedIns, Instrument ins, boolean useAsFamilyDefault)
    {
        Instrument oldIns = mapInstruments.put(remappedIns, ins);
        if (!Objects.equals(ins, oldIns))
        {
            pcs.firePropertyChange(PROP_INSTRUMENT, remappedIns, ins);
        }
        if (useAsFamilyDefault && (remappedIns != DRUMS_INSTRUMENT && remappedIns != PERCUSSION_INSTRUMENT))
        {
            Family family = ((GM1Instrument) remappedIns).getFamily();
            oldIns = mapFamilyInstruments.put(family, ins);
            if (!Objects.equals(ins, oldIns))
            {
                pcs.firePropertyChange(PROP_FAMILY, family, ins);
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
     * Our serialization proxy.
     * <p>
     * Take into account the special DRUMS/PERCUSSION instances. Take into account the fact that some Instruments's MidiSynths
     * might not be present on the system when reading file.
     */
    protected static class SerializationProxy implements Serializable
    {

        private static final String STR_FAMILY_DEFAULT = "__FAMILY_DEFAULT__";      //NOI18N
        private static final long serialVersionUID = 1122298372L;
        private final int spVERSION = 1;
        private HashMap<GM1Instrument, String> spMapInstruments = new HashMap<>();
        private String spDrumsInstrumentStr = "NOT_SET";
        private String spPercInstrumentStr = "NOT_SET";

        protected SerializationProxy(GMRemapTable table)
        {
            if (table == null)
            {
                throw new IllegalStateException("table=" + table);   //NOI18N
            }
            // Can't copy the map because DRUMS/PERCUSSION_INSTRUMENT are not serializable
            HashMap<Instrument, Instrument> mapOrig = table.getInstrumentMap();
            for (Instrument ins : mapOrig.keySet())
            {
                if (!(ins == DRUMS_INSTRUMENT || ins == PERCUSSION_INSTRUMENT))
                {
                    GM1Instrument gmIns = (GM1Instrument) ins;
                    Instrument destIns = mapOrig.get(gmIns);
                    if (destIns != null)
                    {
                        String str = mapOrig.get(gmIns).saveAsString();
                        if (destIns == table.getInstrument(gmIns.getFamily()))
                        {
                            str += STR_FAMILY_DEFAULT;
                        }
                        spMapInstruments.put(gmIns, str);
                    }
                }
            }
            if (table.getInstrument(DRUMS_INSTRUMENT) != null)
            {
                spDrumsInstrumentStr = table.getInstrument(DRUMS_INSTRUMENT).saveAsString();
            }
            if (table.getInstrument(PERCUSSION_INSTRUMENT) != null)
            {
                spPercInstrumentStr = table.getInstrument(PERCUSSION_INSTRUMENT).saveAsString();
            }
        }

        private Object readResolve() throws ObjectStreamException
        {
            GMRemapTable table = new GMRemapTable();
            for (GM1Instrument gmIns : spMapInstruments.keySet())
            {
                String strIns = spMapInstruments.get(gmIns);
                boolean useAsFamilyDefault = false;
                if (strIns.contains(STR_FAMILY_DEFAULT))
                {
                    useAsFamilyDefault = true;
                    strIns = strIns.replace(STR_FAMILY_DEFAULT, "");
                }
                Instrument destIns = Instrument.loadFromString(strIns);
                if (destIns == null)
                {
                    LOGGER.warning("readResolve() Can't find instrument for saved string: " + strIns + ". Instrument mapping could not be set for GM instrument " + gmIns.getPatchName());   //NOI18N
                    continue;
                }
                table.setInstrumentNoException(gmIns, destIns, useAsFamilyDefault);
            }
            if (!spPercInstrumentStr.equals("NOT_SET"))
            {
                Instrument ins = Instrument.loadFromString(spPercInstrumentStr);
                if (ins == null)
                {
                    LOGGER.warning("readResolve() Can't find instrument for saved string: " + spPercInstrumentStr + ". Instrument mapping could not be set for the PERCUSSION instrument.");   //NOI18N
                } else
                {
                    table.setInstrumentNoException(PERCUSSION_INSTRUMENT, ins, false);
                }
            }
            if (!spDrumsInstrumentStr.equals("NOT_SET"))
            {
                Instrument ins = Instrument.loadFromString(spDrumsInstrumentStr);
                if (ins == null)
                {
                    LOGGER.warning("readResolve() Can't find instrument for saved string: " + spDrumsInstrumentStr + ". Instrument mapping could not be set for the DRUMS instrument.");   //NOI18N
                } else
                {
                    table.setInstrumentNoException(DRUMS_INSTRUMENT, ins, false);
                }
            }
            return table;
        }
    }
}
