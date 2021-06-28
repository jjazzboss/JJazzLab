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
package org.jjazz.midi.api;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import static org.jjazz.midi.api.VelocityTransformer.NO_CHANGE;

/**
 * A class to adjust Midi notes velocity.
 */
public class VelocityTransformer implements Serializable
{

    /**
     * Transformer which does nothing.
     */
    public final static VelocityTransformer NO_CHANGE = new VelocityTransformer(0, Curve.LINEAR);
    /**
     * Mute all notes.
     */
    public final static VelocityTransformer MUTER = new VelocityTransformer(-128, Curve.LINEAR);

    public enum Curve
    {

        LINEAR, ROUND_UP, ROUND_DOWN, S_UP_FIRST, S_DOWN_FIRST
    }

    private Curve curve;
    private int velShift;

    public VelocityTransformer(int velShift)
    {
        this(velShift, Curve.LINEAR);
    }

    /**
     *
     * @param velShift
     * @param curve
     */
    public VelocityTransformer(int velShift, Curve curve)
    {
        this.velShift = velShift;
        this.curve = curve;
    }

    /**
     * Transform a velocity.
     *
     * @param velocity
     * @return A valid velocity between 0 and 127.
     */
    public int transform(int velocity)
    {
        throw new UnsupportedOperationException();
    }

    // ==================================================================
    // Private methods
    // ==================================================================    
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

        private static final long serialVersionUID = -9811228702L;
        private final int spVERSION = 1;
        private final int spVelShift;
        private final Curve spCurve;

        private SerializationProxy(VelocityTransformer vt)
        {
            spVelShift = vt.velShift;
            spCurve = vt.curve;
        }

        private Object readResolve() throws ObjectStreamException
        {
            VelocityTransformer vt = NO_CHANGE;
            if (spVelShift != NO_CHANGE.velShift || !spCurve.equals(NO_CHANGE.curve))
            {
                vt = new VelocityTransformer(spVelShift, spCurve);
            }
            return vt;
        }

    }

}
