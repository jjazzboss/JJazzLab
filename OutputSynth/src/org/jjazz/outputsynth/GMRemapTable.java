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
package org.jjazz.outputsynth;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.midi.synths.GM1Instrument;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.keymap.KeyMapGM;
import org.jjazz.midi.synths.Family;

/**
 * The table can associate an Instrument to each of the GM1 instruments + the special DRUMS/PERCUSSION static instances.
 * <p>
 * Association can also be done at the GM1Bank.Family level.
 */
public class GMRemapTable implements Serializable, PropertyChangeListener
{

    /**
     * Specific exception for this object.
     */
    public class ArgumentsException extends Exception
    {

        public ArgumentsException(String msg)
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
    private transient OutputSynth container;
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(GMRemapTable.class.getSimpleName());

    /**
     * Create an empty GM1RemapTable instance.
     * <p>
     */
    public GMRemapTable()
    {
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
    }

    /**
     * Associate an OutputSynth to this object.
     * <p>
     * Listen to the container changes (MidiSynth removed) and update our map accordingly.
     *
     * @param container
     */
    public void setContainer(OutputSynth container)
    {
        if (this.container != null)
        {
            this.container.removePropertyChangeListener(this);
        }
        this.container = container;
        if (container != null)
        {
            container.addPropertyChangeListener(this);
        }
    }

    public HashMap<Instrument, Instrument> getInstrumentMap()
    {
        return new HashMap<>(mapInstruments);
    }

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
     * @return Null if no mapping defined for remappedIns.
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
     *                           Drums/Perc instrument with a GM compatible DrumsKit.KeyMap.
     * @param useAsFamilyDefault If true ins will be also the default instrument for the remappedIns's family. Not used if
     *                           remappedIns is one of the special DRUMS/PERCUSSION instances.
     * @throws ArgumentsException If arguments are invalid. The exception error message can be used for user notification.
     */
    public void setInstrument(Instrument remappedIns, Instrument ins, boolean useAsFamilyDefault) throws ArgumentsException
    {
        checkRemappedInstrument(remappedIns);
        if (ins == remappedIns)
        {
            throw new ArgumentsException("Invalid instrument: " + ins.getFullName() + " is not different from the mapped instrument.");
        }
        if (ins != null && !ins.isDrumKit() && (remappedIns == DRUMS_INSTRUMENT || remappedIns == PERCUSSION_INSTRUMENT))
        {
            throw new ArgumentsException("Invalid instrument: " + ins.getFullName() + " is not a Drums/Percussion instrument.");
        }
        if (ins != null && ins.isDrumKit() && !ins.getDrumKit().getKeyMap().isContaining(KeyMapGM.getInstance()))
        {
            throw new ArgumentsException("Invalid instrument: " + ins.getFullName() + " drum kit keymap (" + ins.getDrumKit().getKeyMap().getName() + ") is not GM-compatible.");
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

    /**
     * True if there is at least one mapping which uses a destination instrument from bank OR synth.
     *
     * @param synth Can be null
     * @param bank  Can be null
     * @return
     */
    public boolean isUsed(MidiSynth synth, InstrumentBank<?> bank)
    {
        for (Instrument mappedIns : mapInstruments.keySet().toArray(new Instrument[0]))
        {
            Instrument ins = mapInstruments.get(mappedIns);
            if (ins != null && (ins.getBank().getMidiSynth() == synth || ins.getBank() == bank))
            {
                return true;
            }
        }
        return false;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // ==============================================================================
    // PropertyChangeListener interface
    // ==============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == container)
        {
            if (evt.getPropertyName().equals(OutputSynth.PROP_STD_BANK) && evt.getOldValue().equals(Boolean.FALSE))
            {
                // A standard InstrumentBank has been removed
                InstrumentBank<?> bank = (InstrumentBank) evt.getNewValue();
                removeObsoleteMappings(null, bank);
            } else if (evt.getPropertyName().equals(OutputSynth.PROP_CUSTOM_SYNTH) && evt.getOldValue().equals(Boolean.FALSE))
            {
                // A MidiSynth has been removed
                MidiSynth synth = (MidiSynth) evt.getNewValue();
                removeObsoleteMappings(synth, null);

            }
        }
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

    /**
     * Remove all mappings where destination instrument uses the specified synth OR bank.
     *
     * @param synth
     * @param bank
     */
    private void removeObsoleteMappings(MidiSynth synth, InstrumentBank<?> bank)
    {
        for (Instrument mappedIns : mapInstruments.keySet().toArray(new Instrument[0]))
        {
            Instrument ins = mapInstruments.get(mappedIns);
            if (ins != null && (ins.getBank().getMidiSynth() == synth || ins.getBank() == bank))
            {
                boolean useAsFamilyDefault = false;
                if (mappedIns instanceof GM1Instrument)
                {
                    GM1Instrument gmIns = (GM1Instrument) mappedIns;
                    useAsFamilyDefault = mapFamilyInstruments.get(gmIns.getFamily()) == ins;
                }
                setInstrumentNoException(mappedIns, null, useAsFamilyDefault);
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
