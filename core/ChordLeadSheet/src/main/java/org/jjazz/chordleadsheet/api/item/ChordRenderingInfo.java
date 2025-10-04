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
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * Music rendering info associated to a chord.
 * <p>
 * This is an immutable class.
 */
public class ChordRenderingInfo implements Serializable
{
    private static final Logger LOGGER = Logger.getLogger(ChordRenderingInfo.class.getSimpleName());

    // =================================================================================================
    // NOT USED ANYMORE: but required to be able to read old version V1 files
    private enum PlayStyle
    {
        NORMAL, ACCENT, HOLD, SHOT
    };
    // =================================================================================================    // =================================================================================================    // =================================================================================================    // =================================================================================================    // =================================================================================================    // =================================================================================================    // =================================================================================================    // =================================================================================================    

    /**
     * Change the way music is rendered for a chord symbol.
     */
    public enum Feature
    {
        /**
         * Add an accent.
         * <p>
         * IMPORTANT: Exclusive with ACCENT_STRONGER.
         */
        ACCENT,
        /**
         * Add a stronger accent.
         * <p>
         * IMPORTANT: Exclusive with ACCENT.
         */
        ACCENT_STRONGER,
        /**
         * Hold some notes until next chord.
         * <p>
         * IMPORTANT: Exclusive with SHOT.
         */
        HOLD,
        /**
         * Make some notes played briefly.
         * <p>
         * IMPORTANT: Exclusive with HOLD.
         */
        SHOT,
        /**
         * Make Hold/Shot applied to more instruments and/or longer.
         * <p>
         * Ignored if no HOLD or SHOT.
         */
        EXTENDED_HOLD_SHOT,
        /**
         * Make sure there is no crash cymbal.
         * <p>
         * IMPORTANT: Exclusive with NO_CRASH
         */
        NO_CRASH,
        /**
         * Make sure there is a crash cymbal.
         * <p>
         * IMPORTANT: Exclusive with CRASH
         */
        CRASH,
        /**
         * Bass line must only use the chord symbol root note (or bass note if specified for slash chord Am/D).
         */
        PEDAL_BASS,
        /**
         * Make sure chord is not played "anticipated".
         */
        NO_ANTICIPATION;

        /**
         * For example PEDAL_BASS will return "Pedal Bass"
         *
         * @return
         */
        @Override
        public String toString()
        {
            String[] strs = name().split("_");
            StringBuilder sb = new StringBuilder();
            for (String s : strs)
            {
                if (sb.length() != 0)
                {
                    sb.append(" ");
                }
                sb.append(s.charAt(0)).append(s.substring(1).toLowerCase());
            }
            return sb.toString();
        }
    }

    private StandardScaleInstance scaleInstance;
    private EnumSet<Feature> features;

    /**
     * Create an object with default values.
     */
    public ChordRenderingInfo()
    {
        this((EnumSet<Feature>) null, null);
    }

    /**
     * Create an object with specified Features and default values.
     *
     * @param features If null use the default value.
     */
    public ChordRenderingInfo(EnumSet<Feature> features)
    {
        this(features, null);
    }

    /**
     * Create an object with specified scale and default values.
     *
     * @param scale If null use the default value.
     */
    public ChordRenderingInfo(StandardScaleInstance scale)
    {
        this((EnumSet<Feature>) null, scale);
    }


    /**
     * Create a new object with the specified parameters.
     *
     *
     * @param features If null use the default value.
     * @param scale    Can be null if no scale defined.
     * @throws IllegalArgumentException If the features object is not consistent, e.g. SHOT and HOLD can't both be used.
     */
    public ChordRenderingInfo(EnumSet<Feature> features, StandardScaleInstance scale)
    {
        this.features = (features == null) ? EnumSet.noneOf(Feature.class) : checkFeaturesConsistency(features.clone());
        this.scaleInstance = scale;
    }


    public ChordRenderingInfo(ChordRenderingInfo cri, StandardScaleInstance scale)
    {
        this(cri.getFeatures(), scale);
    }

    public ChordRenderingInfo(ChordRenderingInfo cri, EnumSet<Feature> features)
    {
        this(features, cri.getScaleInstance());
    }

    /**
     * Get a copy of the rendering features.
     * <p>
     * Default value is an empty EnumSet.
     *
     * @return
     */
    public EnumSet<Feature> getFeatures()
    {
        return features.clone();
    }


    /**
     * Get the accent feature if it's used.
     *
     * @return Can be null.
     */
    public Feature getAccentFeature()
    {
        if (features.contains(Feature.ACCENT))
        {
            return Feature.ACCENT;
        } else if (features.contains(Feature.ACCENT_STRONGER))
        {
            return Feature.ACCENT_STRONGER;
        }
        return null;
    }

    /**
     * Test if this object features contains the specified features.
     * <p>
     * Convenience method because of EnumSet...
     *
     * @param f1
     * @param fx
     * @return
     */
    public boolean hasAllFeatures(Feature f1, Feature... fx)
    {
        if (!features.contains(f1))
        {
            return false;
        }
        return features.containsAll(Arrays.asList(fx));
    }

    /**
     * Test if this object features contains one of the specified features.
     * <p>
     * Convenience method because of EnumSet...
     *
     * @param f1
     * @param fx
     * @return
     */
    public boolean hasOneFeature(Feature f1, Feature... fx)
    {
        if (features.contains(f1))
        {
            return true;
        }
        for (Feature f : fx)
        {
            if (features.contains(f))
            {
                return true;
            }
        }
        return false;
    }


    /**
     * Return a new object transposed by the specified semitones (StandardScaleInstance startNote is impacted).
     * <p>
     * Default value is 0.
     *
     * @param t Transposition in semitones.
     * @return
     */
    public ChordRenderingInfo getTransposed(int t)
    {
        StandardScaleInstance ssi = scaleInstance == null ? null : scaleInstance.getTransposed(t);
        return new ChordRenderingInfo(features, ssi);
    }

    /*
     * C I/MAJOR/Cmaj ou IV/LYDIAN/G major Cm II/DORIAN/Bbmaj ou III/PHRYGIAN/Abmaj ou VI/AEOLIAN/Ebmaj ou Im/Melodic ou
     * Im/Harmonic Cm7 II/DORIAN/Bbmaj ou III/PHRYGIAN/Abmaj ou VI/AEOLIAN/Ebmaj ou Im/Melodic Cm6 II/DORIAN/Bbmaj ou Im/Harmonic
     * Cm7b5 VII/LOCRIAN/Dbmaj ou IIm/Melodic Cm7M Im/Harmonic C7 C7alt C7M I/MAJOR/Cmaj ou IV/LYDIAN/G major C7M#11 IV/LYDIAN/G
     * major C7M#5 C7sus C° diminué
     *
     */
    /**
     * The standard scale instance that should be used for this chord.
     * <p>
     * Default value is null.
     *
     * @return Can be null
     */
    public StandardScaleInstance getScaleInstance()
    {
        return scaleInstance;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.scaleInstance);
        hash = 31 * hash + Objects.hashCode(this.features);
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
        final ChordRenderingInfo other = (ChordRenderingInfo) obj;
        if (!Objects.equals(this.scaleInstance, other.scaleInstance))
        {
            return false;
        }
        if (!Objects.equals(this.features, other.features))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "features=" + features + ", scaleInstance=" + scaleInstance;
    }

    /**
     * A string representation for UI.
     * <p>
     * Ex: "[Accent, Crash] - Phrygian(C)", or "Phrygian(C)". <br>
     * <p>
     * Display only meaningful info, eg. do not show Hold/Shot/Extended/Crash/NoCrash if no accent.
     *
     * @return Can be an empty string
     */
    public String toUserString()
    {
        var f = getFeatures();
        if (getAccentFeature() == null)
        {
            // Make sure to remove the accent-specific stuff
            f.removeAll(EnumSet.of(Feature.CRASH, Feature.NO_CRASH, Feature.EXTENDED_HOLD_SHOT, Feature.HOLD, Feature.SHOT));
        }
        StringBuilder sb = new StringBuilder();
        if (!f.isEmpty())
        {
            sb.append(f);
        }
        if (scaleInstance != null)
        {
            sb.append(sb.length() == 0 ? "" : " - ").append(scaleInstance);
        }
        return sb.toString();
    }

    // --------------------------------------------------------------------- 
    //    Private methods
    // ---------------------------------------------------------------------

    private EnumSet<Feature> checkFeaturesConsistency(EnumSet<Feature> features)
    {
        if (features.contains(Feature.HOLD) && features.contains(Feature.SHOT))
        {
            throw new IllegalArgumentException("features=" + features);
        }
        if (features.contains(Feature.CRASH) && features.contains(Feature.NO_CRASH))
        {
            throw new IllegalArgumentException("features=" + features);
        }
        int count = 0;
        for (Feature f : features)
        {
            if (f.name().startsWith("ACCENT"))
            {
                count++;
                if (count > 1)
                {
                    throw new IllegalArgumentException("features=" + features);
                }
            }
        }
        return features;
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
                    xstream.alias("ChordRenderingInfo", ChordRenderingInfo.class);
                    xstream.alias("ChordRenderingInfoSP", SerializationProxy.class);
                    xstream.alias("Feature", Feature.class);                                        

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
    //    Serialization
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
     * spVERSION 2 changes some saved fields, see below.<br>
     * spVERSION 3 (JJazzLab 4.1.0) introduces several aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction).
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -655298712991L;
        private int spVERSION = 3;    // Must be FIRST field !      Do not make final!  

        // ==================================================================================
        // V1 format: 
        private PlayStyle spPlayStyleV1;            // Not used in V2
        private boolean spAnticipate;               // Not used in V2
        private StandardScaleInstance spStdScale;   // Reused in V2
        // ==================================================================================

        // ==================================================================================
        // V2 format = V1 with
        private EnumSet<Feature> spFeatures; // replace spPlayStyleV1
        // ==================================================================================

        private SerializationProxy(ChordRenderingInfo cri)
        {
            // V2
            spFeatures = cri.getFeatures();
            spStdScale = cri.getScaleInstance();
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException
        {
            // V2
            out.writeInt(spVERSION);      // Make sure it's first
            out.writeObject(spFeatures);
            out.writeObject(spStdScale);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
        {
            spVERSION = in.readInt();
            if (spVERSION == 1)
            {
                // V1
                spPlayStyleV1 = (PlayStyle) in.readObject();
                spAnticipate = in.readBoolean();
                spStdScale = (StandardScaleInstance) in.readObject();
            } else
            {
                // V2
                spFeatures = (EnumSet<Feature>) in.readObject();
                spStdScale = (StandardScaleInstance) in.readObject();
            }
        }

        protected Object readResolve() throws ObjectStreamException
        {
            if (spPlayStyleV1 != null)
            {
                switch (spPlayStyleV1)
                {
                    case NORMAL:
                        spFeatures = null;
                        break;
                    case ACCENT:
                        spFeatures = EnumSet.of(Feature.ACCENT);
                        break;
                    case HOLD:
                        spFeatures = EnumSet.of(Feature.ACCENT, Feature.HOLD);
                        break;
                    case SHOT:
                        spFeatures = EnumSet.of(Feature.ACCENT, Feature.SHOT);
                        break;
                    default:
                        LOGGER.log(Level.WARNING, "readResolve() Invalid value for spPlayStyle={0}. Ignored.", spPlayStyleV1);
                }
                if (!spAnticipate)
                {
                    spFeatures = EnumSet.of(Feature.NO_ANTICIPATION, spFeatures.toArray(new Feature[0]));
                }
            }
            ChordRenderingInfo cri = new ChordRenderingInfo(spFeatures, spStdScale);
            return cri;
        }
    }

}
