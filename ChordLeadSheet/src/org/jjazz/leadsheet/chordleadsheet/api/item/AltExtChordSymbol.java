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
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.ChordSymbol;
import org.jjazz.harmony.ChordType;
import org.jjazz.harmony.ChordTypeDatabase;
import org.jjazz.harmony.Note;

/**
 * Used as the alternate content of a ExtChordSymbol.
 * <p>
 * Same as ExtChordSymbol except it can't have itself an alternate content.
 */
public class AltExtChordSymbol extends ExtChordSymbol
{

    private static final Logger LOGGER = Logger.getLogger(AltExtChordSymbol.class.getSimpleName());

    /**
     * A void alternate chord symbol.
     */
    private AltExtChordSymbol()
    {

    }

    public AltExtChordSymbol(Note rootDg, Note bassDg, ChordType ct, ChordRenderingInfo cri)
    {
        super(rootDg, bassDg, ct, cri, null, null);
    }

    public AltExtChordSymbol(String s, ChordRenderingInfo cri) throws ParseException
    {
        super(s, cri, null, null);
    }

    @Override
    public AltExtChordSymbol getTransposedChordSymbol(int t, Note.Alteration alt)
    {
        ChordSymbol cs = super.getTransposedChordSymbol(t, alt);
        ChordRenderingInfo cri = getRenderingInfo().getTransposed(t);
        AltExtChordSymbol aecs = new AltExtChordSymbol(cs.getRootNote(), cs.getBassNote(), cs.getChordType(), cri);
        return aecs;
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

        private static final long serialVersionUID = 2020001223L;
        private final int spVERSION = 1;
        private final String spName;
        private final ChordRenderingInfo spRenderingInfo;

        private SerializationProxy(AltExtChordSymbol aecs)
        {
            spName = aecs.getOriginalName();
            spRenderingInfo = aecs.getRenderingInfo();
        }

        private Object readResolve() throws ObjectStreamException
        {
            ChordSymbol cs = null;
            try
            {
                cs = new AltExtChordSymbol(spName, spRenderingInfo);
            } catch (ParseException e)
            {
                LOGGER.log(Level.WARNING, spName + ": Invalid chord symbol. Using 'C' ChordSymbol instead.");
                ChordTypeDatabase ctdb = ChordTypeDatabase.getInstance();
                cs = new AltExtChordSymbol(new Note(0), new Note(0), ctdb.getChordType(""), spRenderingInfo);
            }
            return cs;
        }
    }
}
