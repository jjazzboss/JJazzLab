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
import java.util.EnumSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.harmony.api.Degree;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.SymbolicDuration;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * An extended chord symbol with additional features:
 * <p>
 * - Chord rendering information<br>
 * - An optional conditionally-enabled alternate chord symbol<br>
 * - Special NC chord
 * <p>
 * This is an immutable class.
 */
public class ExtChordSymbol extends ChordSymbol implements Serializable
{

    private ChordRenderingInfo renderingInfo;
    private AltExtChordSymbol altChordSymbol;
    private AltDataFilter altFilter;
    private static final ChordRenderingInfo DEFAULT_CRI_FOR_SLASH_CHORD = new ChordRenderingInfo(EnumSet.of(Feature.PEDAL_BASS), null);
    private static final Logger LOGGER = Logger.getLogger(ExtChordSymbol.class.getSimpleName());

    /**
     * Create a 'C' chord symbol with a standard rendering info and no alternate chord symbol.
     */
    public ExtChordSymbol()
    {
        this(new Note(0), new Note(0), ChordTypeDatabase.getDefault().getChordType(""));
    }

    /**
     * Create an ExtChordSymbol with a standard RenderingInfo and no alternate chord symbol.
     *
     * @param rootDg
     * @param bassDg
     * @param ct
     */
    public ExtChordSymbol(Note rootDg, Note bassDg, ChordType ct)
    {
        this(rootDg, bassDg, ct, new ChordRenderingInfo(), null, null);
    }

    /**
     * Create an ExtChordSymbol with a standard RenderingInfo and no alternate chord symbol.
     *
     * @param cs
     */
    public ExtChordSymbol(ChordSymbol cs)
    {
        this(cs, new ChordRenderingInfo(), null, null);
    }

    /**
     * Create an ExtChordSymbol from the specified ChordSymbol with the specified parameters.
     *
     * @param cs
     * @param rInfo
     * @param altChordSymbol Can be null if altFilter is also null
     * @param altFilter      Can be null if altChordSymbol is also null
     */
    public ExtChordSymbol(ChordSymbol cs, ChordRenderingInfo rInfo, AltExtChordSymbol altChordSymbol, AltDataFilter altFilter)
    {
        super(cs.getRootNote(), cs.getBassNote(), cs.getChordType(), cs.getOriginalName());
        Objects.requireNonNull(rInfo);
        Preconditions.checkArgument((altChordSymbol == null && altFilter == null)
                || (altChordSymbol != null && altFilter != null), "altChordSymbol=%s altFilter=%s", altChordSymbol, altFilter);
        
        renderingInfo = rInfo;
        this.altChordSymbol = altChordSymbol;
        this.altFilter = altFilter;
    }

    /**
     * Create an ExtChordSymbol from the specified parameters.
     *
     * @param rootDg
     * @param bassDg
     * @param ct
     * @param rInfo
     * @param altChordSymbol Optional alternate chord symbol. If not null altFilter must be also non-null.
     * @param altFilter      Optional filter to enable the use of the alternate chord symbol. If not null altChordSymbol must be also non-null.
     */
    public ExtChordSymbol(Note rootDg, Note bassDg, ChordType ct, ChordRenderingInfo rInfo, AltExtChordSymbol altChordSymbol, AltDataFilter altFilter)
    {
        super(rootDg, bassDg, ct);

        Objects.requireNonNull(rInfo);
        Preconditions.checkArgument((altChordSymbol == null && altFilter == null)
                || (altChordSymbol != null && altFilter != null), "altChordSymbol=%s altFilter=%s", altChordSymbol, altFilter);

        renderingInfo = rInfo;
        this.altChordSymbol = altChordSymbol;
        this.altFilter = altFilter;
    }


    /**
     * Create a ChordSymbol from a chord string specification, with a standard RenderingInfo and no alternate chord symbol.
     * <p>
     * If string contains a '/', use ChordRenderingInfo.BassLineModifier.PEDAL_BASS as bassLineModifier. If string is "NC" returns the special NCExtChordSymbol
     * instance.
     *
     * @param s Eg 'C7' or 'NC'
     * @return
     * @throws ParseException
     */
    static public ExtChordSymbol get(String s) throws ParseException
    {
        return get(s, s.contains("/") ? DEFAULT_CRI_FOR_SLASH_CHORD : new ChordRenderingInfo(), null, null);
    }

    /**
     * Create a ChordSymbol from a chord string specification, with the specified RenderingInfo and alternate chord symbol.
     *
     * @param s              Eg 'C7' or 'NC'
     * @param rInfo          Can't be null
     * @param altChordSymbol Optional alternate chord symbol. If not null altFilter must be also non-null.
     * @param altFilter      Optional filter to enable the use of the alternate chord symbol. If not null altChordSymbol must be also non-null.
     * @return
     *
     * @throws ParseException
     */
    static public ExtChordSymbol get(String s, ChordRenderingInfo rInfo, AltExtChordSymbol altChordSymbol, AltDataFilter altFilter) throws ParseException
    {
        Preconditions.checkNotNull(s);
        Preconditions.checkNotNull(rInfo);

        ExtChordSymbol res;

        if (NCExtChordSymbol.NAME.equals(s))
        {
            res = new NCExtChordSymbol(rInfo, altChordSymbol, altFilter);
        } else
        {
            var cs = new ChordSymbol(s);            // throws ParseException
            res = new ExtChordSymbol(cs, rInfo, altChordSymbol, altFilter);
        }
        return res;
    }

    /**
     * Get a copy of this ExtChordSymbol, possibly modified with the specified non-null parameters.
     *
     * @param cs             If not null return value will use this parameter.
     * @param rInfo          If not null return value will use this parameter.
     * @param altChordSymbol If not null return value will use this parameter. If not null altFilter must be also non-null.
     * @param altFilter      If not null return value will use this parameter. If not null altChordSymbol must be also non-null.
     * @return
     */
    public ExtChordSymbol getCopy(ChordSymbol cs, ChordRenderingInfo rInfo, AltExtChordSymbol altChordSymbol, AltDataFilter altFilter)
    {
        Preconditions.checkArgument((altChordSymbol == null && altFilter == null)
                || (altChordSymbol != null && altFilter != null), "altChordSymbol=%s altFilter=%s", altChordSymbol, altFilter);
        
        cs = cs != null ? cs : this;
        rInfo = rInfo != null ? rInfo : getRenderingInfo();
        altChordSymbol = altChordSymbol != null ? altChordSymbol : getAlternateChordSymbol();
        altFilter = altFilter != null ? altFilter : getAlternateFilter();
        return new ExtChordSymbol(cs, rInfo, altChordSymbol, altFilter);
    }

    @Override
    public int hashCode()
    {
        int hash = super.hashCode();
        hash = 17 * hash + Objects.hashCode(this.renderingInfo);
        hash = 17 * hash + Objects.hashCode(this.altChordSymbol);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final ExtChordSymbol other = (ExtChordSymbol) obj;
        if (!Objects.equals(this.renderingInfo, other.renderingInfo))
        {
            return false;
        }
        if (!Objects.equals(this.altChordSymbol, other.altChordSymbol))
        {
            return false;
        }
        if (!Objects.equals(this.altFilter, other.altFilter))
        {
            return false;
        }

        return super.equals(obj);
    }


    /**
     * Get this object or the alternate chord symbol, depending on the specified string.
     * <p>
     * Return this object if : <br>
     * - altDataFilterString is null, or <br>
     * - alternate ChordSymbol is null, or <br>
     * - the AltDataFilter is null, or <br>
     * - the non-null AltDataFilter does not accept the specified string.<p>
     * Otherwise return getAlternateChordSymbol().
     *
     * @param altDataFilterString String to be passed to the AltDataFilter
     * @return
     */
    public ExtChordSymbol getChordSymbol(String altDataFilterString)
    {
        if (altDataFilterString == null || altChordSymbol == null || altFilter == null || !altFilter.accept(altDataFilterString))
        {
            return this;
        } else
        {
            return getAlternateChordSymbol();
        }
    }

    /**
     * Get the optional alternate chord symbol.
     *
     * @return If null getAlternateFilter() will also return null.
     */
    public AltExtChordSymbol getAlternateChordSymbol()
    {
        return this.altChordSymbol;
    }

    /**
     * Get the optional filter used to check if we need to use the alternate chord symbol.
     * <p>
     *
     * @return If null getAlternateChordSymbol() will also return null.
     */
    public AltDataFilter getAlternateFilter()
    {
        return this.altFilter;
    }

    /**
     * Check if this instance does not use any ExtChordSymbol specific feature (alternate chord or ChordRenderingInfo).
     *
     * @return
     */
    public boolean isStandard()
    {
        return altChordSymbol == null && new ChordRenderingInfo().equals(renderingInfo);
    }

    /**
     * Adapt a source note from this chord symbol to a destination chord symbol.
     * <p>
     * If source note is a degree of the source chord symbol (ex: G=b7 for A7) :<br>
     * - Try to reapply it to the destination chord symbol. Ex: G becomes B for C7M dest chord. <br>
     * - If NOK(Ex: G=b7 becomes ? for C dest chord), try to use the destination scales if present.<br>
     * - If scales NOK, make some assumptions to find the "best" possible note.
     * <p>
     * If source note is NOT a source chord symbol degree (ex: D for A7), return -1.
     *
     * @param srcRelPitch The relative pitch of the source note (eg 2 for note D)
     * @param destEcs
     * @return The relative pitch of the destination note, or -1 if source note could not be fitted.
     */
    public int fitNote(int srcRelPitch, ExtChordSymbol destEcs)
    {
        int destRelPitch = -1;
        int srcRelPitchToRoot = Note.getNormalizedRelPitch(srcRelPitch - getRootNote().getRelativePitch());
        Degree srcDegree = getChordType().getDegree(srcRelPitchToRoot);
        if (srcDegree != null)
        {
            // srcNote is a source chord note, eg G for Eb7
            Degree destDegree = destEcs.getChordType().fitDegreeAdvanced(srcDegree, destEcs.renderingInfo.getScaleInstance());
            destRelPitch = Note.getNormalizedRelPitch(destDegree.getPitch() + destEcs.getRootNote().getPitch());
        }
        return destRelPitch;
    }

    /**
     * @return Additional info to help music generation programs render this chord. Can't be null.
     */
    public ChordRenderingInfo getRenderingInfo()
    {
        return renderingInfo;
    }

    /**
     * Get a transposed ExtChordSymbol.
     *
     * @param t   The amount of transposition in semitones.
     * @param alt If <code>null</code>, accidental is unchanged; otherwise use alt.
     * @return A new transposed ExtChordSymbol.
     */
    @Override
    public ExtChordSymbol getTransposedChordSymbol(int t, Note.Accidental alt)
    {
        ChordSymbol cs = super.getTransposedChordSymbol(t, alt);
        ChordRenderingInfo cri = getRenderingInfo().getTransposed(t);
        AltExtChordSymbol altCs = (altChordSymbol == null)
                ? null
                : altChordSymbol.getTransposedChordSymbol(t, alt);
        ExtChordSymbol ecs = new ExtChordSymbol(cs, cri, altCs, altFilter);
        return ecs;
    }

    /**
     * @return ExtChordSymbol A random chord symbol (random degree, random chord type)
     */
    public static ExtChordSymbol createRandomChordSymbol()
    {
        int p = Note.OCTAVE_STD * 12 + (int) Math.round(Math.random() * 12f);
        Note.Accidental alt = (Math.random() < .5) ? Note.Accidental.FLAT : Note.Accidental.SHARP;
        Note n = new Note(p, SymbolicDuration.QUARTER.getDuration(), 64, alt);
        ChordTypeDatabase ctb = ChordTypeDatabase.getDefault();
        int index = (int) (ctb.getSize() * Math.random());
        ChordType ct = ctb.getChordType(index);
        ExtChordSymbol ecs = new ExtChordSymbol(n, n, ct);
        return ecs;
    }

    // --------------------------------------------------------------------- 
    // Private methods
    // ---------------------------------------------------------------------    

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
                    xstream.alias("ExtChordSymbol", ExtChordSymbol.class);
                    xstream.alias("ExtChordSymbolSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spName");
                    xstream.useAttributeFor(SerializationProxy.class, "spOriginalName");
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

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * Serialization proxy.
     * <p>
     * spVERSION 2 changes some saved fields, see below.<br>
     * spVERSION 3 (JJazzLab 4.1.0) introduces several aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction).
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -6112620289882L;
        private int spVERSION = 3;      // Do not make final!
        private String spName;
        private String spOriginalName;        // from VERSION 2 only
        private ChordRenderingInfo spRenderingInfo;
        private AltExtChordSymbol spAltChordSymbol;
        private AltDataFilter spAltFilter;

        // Kept for legacy purpose: before JJazzLab 2.2, we did not use UFT-8 encoding with Xstream hence this workaround for "°" char
        private static final String DOT_REPLACEMENT = "_UpperDot_";

        private SerializationProxy(ExtChordSymbol cs)
        {
            spName = cs.getName();
            spOriginalName = cs.getOriginalName();
            spRenderingInfo = cs.getRenderingInfo();
            spAltChordSymbol = cs.getAlternateChordSymbol();
            spAltFilter = cs.getAlternateFilter();
        }

        private Object readResolve() throws ObjectStreamException
        {

            // First try with originalName (or spName if originalName not saved due to V1 .sng file)
            String s = spOriginalName == null ? spName.replace(DOT_REPLACEMENT, "°") : spOriginalName.replace(DOT_REPLACEMENT, "°");
            ExtChordSymbol ecs = null;

            try
            {
                ecs = get(s, spRenderingInfo, spAltChordSymbol, spAltFilter);
            } catch (ParseException e)
            {
                // Nothing
            }

            if (ecs == null && spOriginalName != null)
            {
                // If spOriginalName used, the error may be due to a missing user alias on current system
                // Retry with standard name
                try
                {
                    ecs = get(spName, spRenderingInfo, spAltChordSymbol, spAltFilter);
                    LOGGER.log(Level.WARNING, "{0}: Invalid chord symbol. Using ''{1}'' instead.", new Object[]
                    {
                        spOriginalName, spName
                    });
                } catch (ParseException e)
                {
                    // Nothing
                }
            }
            if (ecs == null)
            {
                LOGGER.log(Level.WARNING, "{0}: Invalid chord symbol. Using ''C'' ChordSymbol instead.", spName);
                ecs = new ExtChordSymbol();
            }

            return ecs;
        }
    }
}
