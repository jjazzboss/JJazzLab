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
package org.jjazz.harmony.api;

import com.thoughtworks.xstream.XStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.harmony.spi.ScaleManager;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * A standard scale (e.g. Mixolydian) with a start note (e.g. Eb).
 * <p>
 * This is an immutable class.
 */
public class StandardScaleInstance implements Serializable
{

    private StandardScale scale;
    private Note startNote;
    private static final Logger LOGGER = Logger.getLogger(StandardScaleInstance.class.getSimpleName());

    public StandardScaleInstance(StandardScale scale, Note startNote)
    {
        if (scale == null || startNote == null)
        {
            throw new NullPointerException("scale=" + scale + " startNote=" + startNote);
        }
        this.scale = scale;
        this.startNote = startNote;
    }

    public StandardScale getScale()
    {
        return scale;
    }

    public Note getStartNote()
    {
        return startNote;
    }

    /**
     * Return a copy transposed by t semitons.
     *
     * @param semitons
     * @return
     */
    public StandardScaleInstance getTransposed(int semitons)
    {
        int p = startNote.getPitch() + semitons;
        p = Math.min(127, p);
        p = Math.max(0, p);
        StandardScaleInstance ssi = new StandardScaleInstance(scale, new Note(p));
        return ssi;
    }

    /**
     * Get the notes that make the scale.
     *
     * @return
     */
    public List<Note> getNotes()
    {
        return getScale().getNotes(startNote);
    }

    /**
     * Get the relative pitches of the notes that make the scale.
     *
     * @return An unmodifiable list.
     */
    public List<Integer> getRelativePitches()
    {
        return getNotes().stream().map(n -> n.getRelativePitch()).toList();
    }

    /**
     * Try to fit d to this scale.<p>
     * 1/ If d.getPitch() is part of this scale return the corresponding degree<p>
     * Ex: d=NINTH_SHARP and scale=DORIAN, return THIRD_MINOR (same pitch)
     * <p>
     * 2/ If 1/ dit not work, try to see a derived degree match<p>
     * Ex: d=FIFTH and scale=ALTERED, return FIFTH_FLAT<br>
     *
     * @param d
     * @return A matching degree which belongs to this scale, or null.
     */
    public Degree fitDegree(Degree d)
    {
        // First see if the degree's pitch is part of the scale
        Scale s = getScale();
        Degree destDegree = s.getDegree(d.getPitch());
        if (destDegree == null)
        {
            // If not try to get scale's derived degrees (e.g. NINTH => NINTH_SHARP etc.)
            List<Degree> scaleDegrees = s.getDegrees(d.getNatural());
            if (!scaleDegrees.isEmpty())
            {
                // There can be one or two, take the first
                destDegree = scaleDegrees.get(0);
            }
        }
        return destDegree;
    }

    /**
     * Equals if same scale object and same relative pitch for start notes.
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o
    )
    {
        if (o instanceof StandardScaleInstance)
        {
            StandardScaleInstance ssi = (StandardScaleInstance) o;
            return ssi.getScale() == scale && ssi.startNote.equalsRelativePitch(startNote);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.scale);
        hash = 29 * hash + Objects.hashCode(this.startNote);
        return hash;
    }

    public String toNoteString()
    {
        return getNotes().stream()
                .map(n -> n.toRelativeNoteString())
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Override
    public String toString()
    {
        return scale.getName() + "(" + startNote.toRelativeNoteString() + ")";
    }

    /**
     * Call fitDegree(d) on each of the StandardScaleInstance and return the first non-null result.
     *
     * @param d
     * @param ssis
     * @return Can be null.
     */
    static public Degree fitDegree(Degree d, StandardScaleInstance... ssis)
    {
        Degree destDegree = null;
        for (StandardScaleInstance ssi : ssis)
        {
            destDegree = ssi.fitDegree(d);
            if (destDegree != null)
            {
                break;
            }
        }
        return destDegree;
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
                    if (instanceId.equals(SONG_LOAD))
                    {
                        // From 3.0 all public packages are renamed with api or spi somewhere in the path
                        xstream.alias("org.jjazz.harmony.StandardScaleInstance", StandardScaleInstance.class);
                        xstream.alias("org.jjazz.harmony.StandardScaleInstance$SerializationProxy", SerializationProxy.class);
                    }


                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files          
                    xstream.alias("StdScaleInstance", StandardScaleInstance.class);
                    xstream.alias("StdScaleInstanceSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spStdScaleIndex");
                    xstream.useAttributeFor(SerializationProxy.class, "spStartNotePitch");
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
     * Store the index of the std scale and the pitch of the note.
     * <p>
     * spVERSION2: introduces xstream aliases (see XSTreamConfig) for better XML readibility/size
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -3012901129L;
        private int spVERSION = 2;  // Do not make final!
        private int spStdScaleIndex;
        private int spStartNotePitch;

        private SerializationProxy(StandardScaleInstance ssi)
        {
            spStartNotePitch = ssi.getStartNote().getPitch();
            spStdScaleIndex = ScaleManager.getDefault().getStandardScales().indexOf(ssi.getScale());
            assert spStdScaleIndex != -1;
        }

        private Object readResolve() throws ObjectStreamException
        {
            List<StandardScale> stdScales = ScaleManager.getDefault().getStandardScales();
            StandardScale ss;
            if (spStdScaleIndex < 0 || spStdScaleIndex >= stdScales.size())
            {
                LOGGER.log(Level.WARNING, "readResolve() invalid standard scale index={0}. Use MAJOR scale instead.", spStdScaleIndex);
                ss = stdScales.get(0);
            } else
            {
                ss = stdScales.get(spStdScaleIndex);
            }
            StandardScaleInstance ssi = new StandardScaleInstance(ss, new Note(spStartNotePitch));
            return ssi;
        }
    }

}
