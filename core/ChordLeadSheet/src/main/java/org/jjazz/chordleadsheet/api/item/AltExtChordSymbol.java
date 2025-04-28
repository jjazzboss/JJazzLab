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

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.Note;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

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
        var ecs = ExtChordSymbol.get(s);            // throws ParseException. Use ExtChordSymbol.get() to be able to process NC.
        var res = new AltExtChordSymbol(ecs, rInfo);
        return res;
    }


    @Override
    public AltExtChordSymbol getTransposedChordSymbol(int t, Note.Accidental alt)
    {
        ChordSymbol cs = super.getTransposedChordSymbol(t, alt);
        ChordRenderingInfo cri = getRenderingInfo().getTransposed(t);
        AltExtChordSymbol aecs = new AltExtChordSymbol(cs, cri);
        return aecs;
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
                    xstream.alias("AltExtChordSymbol", AltExtChordSymbol.class);
                    xstream.alias("AltExtChordSymbolSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
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

    /**
     * Serialization proxy.
     * <p>
     * spVERSION 2 introduces new XStream aliases (see XStreamConfig)
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 2020001223L;
        private final int spVERSION = 2;
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
                LOGGER.log(Level.WARNING, "{0}: Invalid chord symbol. Using ''C'' ChordSymbol instead.", spName);
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
