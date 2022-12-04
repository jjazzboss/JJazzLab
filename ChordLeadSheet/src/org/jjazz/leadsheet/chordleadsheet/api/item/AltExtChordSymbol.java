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

import com.google.common.base.Preconditions;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.Note;
import org.openide.util.Exceptions;

/**
 * Used as the alternate content of an ExtChordSymbol.
 * <p>
 * Same as ExtChordSymbol except it can't have itself an alternate content.<p>
 * This is an immutable class.
 */
public class AltExtChordSymbol extends ExtChordSymbol
{

    private static final Logger LOGGER = Logger.getLogger(AltExtChordSymbol.class.getSimpleName());


    private AltExtChordSymbol()
    {

    }

    public AltExtChordSymbol(ChordSymbol cs, ChordRenderingInfo cri)
    {
        super(cs, cri, null, null);
    }


    static public AltExtChordSymbol get(String s, ChordRenderingInfo rInfo) throws ParseException
    {
        Preconditions.checkNotNull(s);
        Preconditions.checkNotNull(rInfo);
        var cs = new ChordSymbol(s);            // throws ParseException
        var res = new AltExtChordSymbol(cs, rInfo);
        return res;
    }


    @Override
    public AltExtChordSymbol getTransposedChordSymbol(int t, Note.Alteration alt)
    {
        ChordSymbol cs = super.getTransposedChordSymbol(t, alt);
        ChordRenderingInfo cri = getRenderingInfo().getTransposed(t);
        AltExtChordSymbol aecs = new AltExtChordSymbol(cs, cri);
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
            AltExtChordSymbol res = null;
            try
            {
                res = get(spName, spRenderingInfo);
            } catch (ParseException e)
            {
                LOGGER.log(Level.WARNING, spName + ": Invalid chord symbol. Using 'C' ChordSymbol instead.");   //NOI18N
                try
                {
                    res = get("C", spRenderingInfo);
                } catch (ParseException ex)
                {
                    // Should never be here
                    Exceptions.printStackTrace(ex);
                }
            }
            return res;
        }
    }
}
