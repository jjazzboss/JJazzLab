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
package org.jjazz.chordleadsheet.api.item;

import com.thoughtworks.xstream.XStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.ParseException;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.harmony.api.Note;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

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
                try
                {
                    INSTANCE = new VoidAltExtChordSymbol();
                } catch (ParseException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return INSTANCE;
    }

    private VoidAltExtChordSymbol() throws ParseException
    {
        super(new ChordSymbol(), new ChordRenderingInfo());
    }

    @Override
    public String toString()
    {
        return "VoidAltExtChordSymbolInstance";
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
                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files                    
                    xstream.alias("VoidAltExtChordSymbol", VoidAltExtChordSymbol.class);
                    xstream.alias("VoidAltExtChordSymbolSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spName");
                }

                case MIDIMIX_LOAD ->
                {
                    // Nothing
                }
                case MIDIMIX_SAVE ->
                {
                    // Nothing
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
