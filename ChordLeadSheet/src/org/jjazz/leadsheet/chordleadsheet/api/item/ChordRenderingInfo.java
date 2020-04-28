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

    public enum PlayStyle
    {
        NORMAL,
        ACCENT,
        HOLD, // Hold some chord notes until next chord 
        SHOT // Chord notes are played briefly then stopped
    }

    private StandardScaleInstance scaleInstance;
    private PlayStyle playStyle;
    private boolean anticipateAllowed;

    /**
     * Create an object with PlayStyle.NORMAL, anticipateAllowed=true, and no associated standard scales.
     */
    public ChordRenderingInfo()
    {
        this(PlayStyle.NORMAL, true, null);
    }

    /**
     * Create an object with specified PlayStyle with anticipateAllowed=true and no associated standard scales.
     * @param pStyle
     */
    public ChordRenderingInfo(PlayStyle pStyle)
    {
        this(pStyle, true, null);
    }

    /**
     *
     * @param pStyle
     * @param anticipateEnabled If false don't allow to anticipate notes for this chord.
     * @param scale Can be null.
     */
    public ChordRenderingInfo(PlayStyle pStyle, boolean anticipateEnabled, StandardScaleInstance scale)
    {
        if (pStyle == null)
        {
            throw new NullPointerException("pStyle=" + pStyle + " anticipateEnabled=" + anticipateEnabled + " scale=" + scale);
        }
        playStyle = pStyle;
        scaleInstance = scale;
        this.anticipateAllowed = anticipateEnabled;
    }

    /**
     * The play style to be used for this chord.
     *
     * @return
     */
    public PlayStyle getPlayStyle()
    {
        return playStyle;
    }

    /**
     * Whether this chord can be used an an "anticipated chord".
     *
     * @return
     */
    public boolean isAnticipateAllowed()
    {
        return anticipateAllowed;
    }

    /**
     * Return a new object transposed by the specified semi-tons (StandardScaleInstance startNote is impacted).
     *
     * @param t Transposition in semi-tons.
     * @return
     */
    public ChordRenderingInfo getTransposed(int t)
    {
        StandardScaleInstance ssi = scaleInstance == null ? null : scaleInstance.getTransposed(t);
        return new ChordRenderingInfo(playStyle, anticipateAllowed, ssi);
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
        hash = 47 * hash + Objects.hashCode(this.scaleInstance);
        hash = 47 * hash + Objects.hashCode(this.playStyle);
        hash = 47 * hash + Objects.hashCode(this.anticipateAllowed);
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
        if (this.playStyle != other.playStyle)
        {
            return false;
        }
        if (this.anticipateAllowed != other.anticipateAllowed)
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "PlayStyle=" + playStyle + " anticipateEnabled=" + anticipateAllowed + " scaleInstance=" + scaleInstance;
    }

    // --------------------------------------------------------------------- 
    //    Serialization
    // --------------------------------------------------------------------- */
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * Proxy not really needed today, but ChordRenderingInfo might be more complex in the future.
     * <p>
     * Override writeObject/readObject in order to manage older object versions.
     */
    private static class SerializationProxy implements Serializable// , Externalizable
    {

        private static final long serialVersionUID = -655298712991L;
        private int spVERSION = 1;    // Must be FIRST field !
        private PlayStyle spPlayStyle;
        private boolean spAnticipate;
        private StandardScaleInstance spStdScale;
        // private int spNewVar;

        private SerializationProxy(ChordRenderingInfo cri)
        {
            spPlayStyle = cri.getPlayStyle();
            spStdScale = cri.getScaleInstance();
            spAnticipate = cri.isAnticipateAllowed();
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException
        {
            out.writeInt(spVERSION);      // Make sure it's first
            out.writeObject(spPlayStyle);
            out.writeBoolean(spAnticipate);
            out.writeObject(spStdScale);
            // out.writeInt(spNewVar);
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
        {
            this.spVERSION = in.readInt();
            this.spPlayStyle = (PlayStyle) in.readObject();
            this.spAnticipate = in.readBoolean();
            this.spStdScale = (StandardScaleInstance) in.readObject();
            if (spVERSION <= 1)
            {
                // spNewVar = 100;
            } else
            {
                // spNewVar = in.readInt();
            }
        }

        private Object readResolve() throws ObjectStreamException
        {
            ChordRenderingInfo cri = new ChordRenderingInfo(spPlayStyle, spAnticipate, spStdScale);
            return cri;
        }
    }

}
