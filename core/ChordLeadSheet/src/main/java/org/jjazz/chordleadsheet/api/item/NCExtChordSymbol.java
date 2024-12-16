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
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.harmony.api.Note;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * A special "NC" chord symbol for No Chord.
 * <p>
 * When used nothing should be played.
 */
public class NCExtChordSymbol extends ExtChordSymbol implements Serializable
{

    public static final String NAME = "NC";
    public static final String DESCRIPTION = "No Chord";

    /**
     * Create a NC chord with no standard ChordRenderingInfo and no alternate chord symbol.
     */
    public NCExtChordSymbol()
    {
        this(new ChordRenderingInfo(), null, null);
    }

    /**
     * Create a NC chord symbol with the specified parameters.
     *
     * @param rInfo
     * @param altChordSymbol
     * @param altFilter
     */
    public NCExtChordSymbol(ChordRenderingInfo rInfo, AltExtChordSymbol altChordSymbol, AltDataFilter altFilter)
    {
        super(new Note(13), new Note(13), ChordTypeDatabase.getDefault().getChordType(3), rInfo, altChordSymbol, altFilter);
    }

    @Override
    public NCExtChordSymbol getCopy(ChordSymbol cs, ChordRenderingInfo rInfo, AltExtChordSymbol altChordSymbol, AltDataFilter altFilter)
    {
        if ((altChordSymbol == null && altFilter != null) || (altChordSymbol != null && altFilter == null))
        {
            throw new IllegalArgumentException("rInfo=" + rInfo + " altChordSymbol=" + altChordSymbol + " altFilter=" + altFilter);
        }
        rInfo = rInfo != null ? rInfo : getRenderingInfo();
        altChordSymbol = altChordSymbol != null ? altChordSymbol : getAlternateChordSymbol();
        altFilter = altFilter != null ? altFilter : getAlternateFilter();
        return new NCExtChordSymbol(rInfo, altChordSymbol, altFilter);
    }


    @Override
    public ExtChordSymbol getTransposedChordSymbol(int t, Note.Accidental alt)
    {
        return this;
    }

    @Override
    public String getName()
    {
        return NAME;
    }


    @Override
    public String getOriginalName()
    {
        return getName();
    }

    @Override
    public String toNoteString()
    {
        return "[" + getName() + "]";
    }

    @Override
    public String toString()
    {
        return "NCExtChordSymbol";
    }

    @Override
    public void dump()
    {
        System.out.print("NC-ChordSymbol");
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
                    xstream.alias("NCExtChordSymbol", NCExtChordSymbol.class);
                    xstream.alias("NCExtChordSymbolSP", SerializationProxy.class);
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
     * 
     * spVERSION 2 introduces new XStream aliases (see XStreamConfig)
     */    
    private static class SerializationProxy implements Serializable
    {

        private int spVERSION = 2;      // Do not make final!
        private static final long serialVersionUID = -118977269L;
        private  String spName;
        private  ChordRenderingInfo spRenderingInfo;
        private  AltExtChordSymbol spAltChordSymbol;
        private  AltDataFilter spAltFilter;

        private SerializationProxy(NCExtChordSymbol ncecs)
        {
            spName = ncecs.toString();          // Just to make it clear in the XML field, not used
            spRenderingInfo = ncecs.getRenderingInfo();
            spAltChordSymbol = ncecs.getAlternateChordSymbol();
            spAltFilter = ncecs.getAlternateFilter();
        }

        private Object readResolve() throws ObjectStreamException
        {
            NCExtChordSymbol res = new NCExtChordSymbol(spRenderingInfo, spAltChordSymbol, spAltFilter);
            return res;
        }
    }

}
