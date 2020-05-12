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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.harmony.StandardScaleInstance;

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
     * Change the way music is globally rendered for a chord symbol.
     */
    public enum Feature
    {
        /**
         * Add a light accent.
         * <p>
         * IMPORTANT: Exclusive with the other ACCENT*.
         */
        ACCENT_LIGHT,
        ACCENT_MEDIUM,
        ACCENT_STRONG,
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
         * Make sure there is not crash cymbal.
         * <p>
         * IMPORTANT: Exclusive with the NO_CRASH
         */
        NO_CRASH,
        /**
         * Make sure there is a crash cymbal.
         * <p>
         * IMPORTANT: Exclusive with the other CRASH
         */
        CRASH,
        /**
         * Bass line must only use the chord symbol root note (or bass note if specified for slash chord Am/D).
         */
        BASS_PEDAL,
        /**
         * Make sure chord is not played "anticipated".
         */
        NO_ANTICIPATION;

        /**
         * For example BASS_PEDAL will return "Bass Pedal"
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
     * @param scale Can be null.
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
        if (features.contains(Feature.ACCENT_LIGHT))
        {
            return Feature.ACCENT_LIGHT;
        } else if (features.contains(Feature.ACCENT_MEDIUM))
        {
            return Feature.ACCENT_MEDIUM;
        } else if (features.contains(Feature.ACCENT_STRONG))
        {
            return Feature.ACCENT_STRONG;
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
     * Return a new object transposed by the specified semi-tons (StandardScaleInstance startNote is impacted).
     * <p>
     * Default value is 0.
     *
     * @param t Transposition in semi-tons.
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


//
//    /**
//     * Convenience method for transition to new ChordRenderingInfo API.
//     *
//     * @param cri
//     * @return
//     */
//    static public boolean isOldNormalPlayStyle(ChordRenderingInfo cri)
//    {
//        return cri.getFeatures().isEmpty();
//    }
//
//    /**
//     * Convenience method for transition to new ChordRenderingInfo API.
//     *
//     * @param cri
//     * @return
//     */
//
//    static public boolean isOldAccentPlayStyle(ChordRenderingInfo cri)
//    {
//        return cri.getFeatures().contains(Feature.ACCENT_MEDIUM)
//                && !cri.getFeatures().contains(Feature.SHOT)
//                && !cri.getFeatures().contains(Feature.HOLD);
//    }
//
//    /**
//     * Convenience method for transition to new ChordRenderingInfo API.
//     *
//     * @param cri
//     * @return
//     */
//    static public boolean isOldHoldPlayStyle(ChordRenderingInfo cri)
//    {
//        return cri.getFeatures().containsAll(Arrays.asList(Feature.ACCENT_MEDIUM, Feature.HOLD));
//    }
//
//    /**
//     * Convenience method for transition to new ChordRenderingInfo API.
//     *
//     * @param cri
//     * @return
//     */
//    static public boolean isOldShotPlayStyle(ChordRenderingInfo cri)
//    {
//        return cri.getFeatures().containsAll(Arrays.asList(Feature.ACCENT_MEDIUM, Feature.SHOT));
//    }
    @Override
    public String toString()
    {
        return "features=" + features + ", scaleInstance=" + scaleInstance;
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
     * Proxy to read old versions.
     * <p>
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -655298712991L;
        private int spVERSION = 2;    // Must be FIRST field !        

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

        private Object readResolve() throws ObjectStreamException
        {
            if (spPlayStyleV1 != null)
            {
                switch (spPlayStyleV1)
                {
                    case NORMAL:
                        spFeatures = null;
                        break;
                    case ACCENT:
                        spFeatures = EnumSet.of(Feature.ACCENT_LIGHT);
                        break;
                    case HOLD:
                        spFeatures = EnumSet.of(Feature.ACCENT_LIGHT, Feature.HOLD);
                        break;
                    case SHOT:
                        spFeatures = EnumSet.of(Feature.ACCENT_LIGHT, Feature.SHOT);
                        break;
                    default:
                        LOGGER.warning("readResolve() Invalid value for spPlayStyle=" + spPlayStyleV1 + ". Ignored.");
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
