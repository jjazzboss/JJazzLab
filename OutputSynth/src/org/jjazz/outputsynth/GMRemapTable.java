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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.midi.synths.GM1Bank;
import org.jjazz.midi.synths.GM1Instrument;
import org.jjazz.midi.Instrument;

/**
 * The table can associate an Instrument to each of the GM1 instruments + the special DRUMS/PERCUSSION static instances.
 * <p>
 * Association can also be done at the GM1Bank.Family level.
 */
public class GMRemapTable implements Serializable
{

    /**
     * Special instances for the Drums/Percussion, since GM1 does not define them.
     */
    public static final Instrument DRUMS_INSTRUMENT = new Instrument(0, "Drums");
    public static final Instrument PERCUSSION_INSTRUMENT = new Instrument(1, "Percussion");

    /**
     * oldValue=Family, newValue=Instrument
     */
    public static final String PROP_FAMILY = "Family";
    /**
     * oldValue=GM1Instrument or the special DRUMS/PERCUSSION static instances, newValue=Instrument
     */
    public static final String PROP_INSTRUMENT = "Instrument";

    private HashMap<Instrument, Instrument> mapInstruments = new HashMap<>();
    private HashMap<GM1Bank.Family, Instrument> mapFamilyInstruments = new HashMap<>();
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(GMRemapTable.class.getSimpleName());

    /**
     * Create an empty GM1RemapTable instance.
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
     * Check that the specified remapped Instrument is valid.
     *
     * @param remappedIns
     * @throws IllegalArgumentException If invalid value
     */
    static public void checkRemappedInstrument(Instrument remappedIns)
    {
        if (remappedIns == null
                || (remappedIns != DRUMS_INSTRUMENT && remappedIns != PERCUSSION_INSTRUMENT && !(remappedIns instanceof GM1Instrument)))
        {
            throw new IllegalArgumentException("remappedIns=" + remappedIns);
        }
    }

    /**
     * Get the mapped instrument for remappedIns.
     *
     * @param remappedIns Must be a GM1Instrument or the special DRUMS/PERCUSSION static instances.
     * @return Null if no mapping defined for insGM1orDrumsPerc.
     */
    public Instrument getInstrument(Instrument remappedIns)
    {
        checkRemappedInstrument(remappedIns);
        return mapInstruments.get(remappedIns);
    }

    /**
     * Set the mapped instrument for remappedIns.
     *
     * @param remappedIns           Must be a GM1Instrument or the special DRUMS/PERCUSSION static instances.
     * @param ins                   Can be null
     * @param useAsDefaultForFamily If true ins will be also the default instrument for the insGM1orDrumsPerc's family. Not used
     *                              for if insGM1orDrumsPerc is one of the special DRUMS/PERCUSSION instances.
     */
    public void setInstrument(Instrument remappedIns, Instrument ins, boolean useAsDefaultForFamily)
    {
        checkRemappedInstrument(remappedIns);
        Instrument oldIns = mapInstruments.put(remappedIns, ins);
        if (!Objects.equals(ins, oldIns))
        {
            pcs.firePropertyChange(PROP_INSTRUMENT, remappedIns, ins);
        }
        if (useAsDefaultForFamily && (remappedIns != DRUMS_INSTRUMENT && remappedIns != PERCUSSION_INSTRUMENT))
        {
            GM1Bank.Family family = ((GM1Instrument) remappedIns).getFamily();
            oldIns = mapFamilyInstruments.put(family, ins);
            if (!Objects.equals(ins, oldIns))
            {
                pcs.firePropertyChange(PROP_FAMILY, family, ins);
            }
        }
    }

    /**
     * Get the mapped instrument for the specified instrument family.
     *
     * @param family
     * @return Null if no mapping defined for this family.
     */
    public Instrument getInstrument(GM1Bank.Family family)
    {
        if (family == null)
        {
            throw new NullPointerException("family");
        }
        return mapFamilyInstruments.get(family);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

}
