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
package org.jjazz.midi;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A special kind of instrument with a DrumMap attached.
 */
public class DrumsInstrument extends Instrument implements Serializable
{

    private DrumKitType type;
    private DrumMap drumMap;

    public DrumsInstrument(DrumKitType type, DrumMap drMap, int programChange, String patchName)
    {
        super(programChange, patchName, null);
        this.drumMap = drMap;
        this.type = type;
    }

    public DrumsInstrument(DrumKitType type, DrumMap drMap, int programChange, String patchName, InstrumentBank<?> bank, int bankLSB, int bankMSB, InstrumentBank.BankSelectMethod bsm)
    {
        super(programChange, patchName, bank, bankLSB, bankMSB, bsm);
        if (drMap == null)
        {
            throw new NullPointerException("drMap=" + drMap + " programChange=" + programChange + " patchName=" + patchName);
        }
        this.drumMap = drMap;
        this.type = type;
    }

    public DrumMap getDrumMap()
    {
        return drumMap;
    }

    public DrumKitType getDrumKitType()
    {
        return type;
    }

    // --------------------------------------------------------------------- 
    // Serialization
    // --------------------------------------------------------------------- 
    private Object writeReplace()
    {
        return new SerializationProxy(this);        // Fiels do not need to be saved, we can directly reuse the parent class proxy
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

}
