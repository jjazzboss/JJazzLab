/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.leadsheet.chordleadsheet.api.item;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import org.jjazz.harmony.ChordTypeDatabase;
import org.jjazz.harmony.Note;

/**
 * A special instance used as the "void" alternate chord symbol.
 * <p>
 * When used it's like there was no chord.
 */
public class VoidAltExtChordSymbol extends AltExtChordSymbol implements Serializable
{

    private static VoidAltExtChordSymbol INSTANCE;

    public static VoidAltExtChordSymbol getInstance()
    {
        synchronized (VoidAltExtChordSymbol.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new VoidAltExtChordSymbol();
            }
        }
        return INSTANCE;
    }

    private VoidAltExtChordSymbol()
    {
        super(new Note(11), new Note(10), ChordTypeDatabase.getInstance().getChordType(1), new ChordRenderingInfo());
    }

    @Override
    public String toString()
    {
        return "VoidAltExtChordSymbolInstance";
    }

    // --------------------------------------------------------------------- 
    // Serialization
    // ---------------------------------------------------------------------
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 398736199211L;
        private final String spName;

        private SerializationProxy(VoidAltExtChordSymbol vaecs)
        {
            spName = vaecs.toString();          // Just to make it clear in the XML field, not used
        }

        private Object readResolve() throws ObjectStreamException
        {
            return getInstance();
        }
    }

}
