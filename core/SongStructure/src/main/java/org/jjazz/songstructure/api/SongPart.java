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
package org.jjazz.songstructure.api;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeListener;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.StringProperties;

/**
 * A song part defines how a rhythm is played for a number of bars starting at startBarIndex.
 * <p>
 * Rhythm generation is controlled by the RhythmParameters values. Rhythm can not be changed but a SongPart can be cloned using a new rhythm.
 * <p>
 */
public interface SongPart extends Transferable
{
    /**
     * Fired when a rhythm parameter value has changed.
     * <p>
     * Occurs when value was replaced by another value (for immutable values), or value state has changed (for RpMutableValue instances).
     * <p>
     * oldValue=rhythm parameter, newValue=value.
     */
    public static final String PROP_RP_VALUE = "SptRpValue";

    /**
     * Fired when a mutable value has changed (in addition to the PROP_RP_VALUE change event).
     * <p>
     * Occurs when the value state of a RpMutableValue instance has changed.
     * <p>
     * oldValue=rhythm parameter, newValue=value.
     */
    public static final String PROP_RP_MUTABLE_VALUE = "SptRpMutableValue";
    public static final String PROP_START_BAR_INDEX = "SptStartBarIndex";
    public static final String PROP_NB_BARS = "SptNbBars";
    public static final String PROP_NAME = "SptName";
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(SongPart.class, "Song Part");


    /**
     * The start barIndex of this song part.
     *
     * @return
     */
    int getStartBarIndex();

    /**
     * The size of the song part in bars (same as the parentSection size).
     *
     * @return
     */
    int getNbBars();

    /**
     * Convenience method.
     *
     * @return The range [getStartBarIndex(); getStartBarIndex()+getNbBars()-1]
     */
    default IntRange getBarRange()
    {
        return new IntRange(getStartBarIndex(), getStartBarIndex() + getNbBars() - 1);
    }

    /**
     * By default set to the parentSection's name
     *
     * @return
     */
    String getName();

    /**
     * An optional CLI_Section associated to this SongPart.
     *
     * @return
     */
    CLI_Section getParentSection();

    /**
     * Get the RhythmParameter value.
     *
     * @param <T>
     * @param rp
     * @return Can not be null
     * @throws IllegalArgumentException If rp is not a valid RhythmParameter for this SongPart
     */
    <T> T getRPValue(RhythmParameter<T> rp);

    Rhythm getRhythm();

    /**
     * @return The SongStructure this object belong to. Set by SongStructure when the SongPart is added.
     */
    SongStructure getContainer();

    StringProperties getClientProperties();

    /**
     * Create a new SongPart with same name based on this object.
     * <p>
     * Parameters of the new SongPart can be adjusted.<br>
     * If using a different rhythm, try to adapt the value of compatible RhythmParameters. ClientProperties are also copied.
     *
     * @param r             The new Rhythm to be used. If null Rhythm is unchanged.
     * @param startBarIndex The startBarIndex of the new SongPart
     * @param nbBars        The nbBars of the new SongPart
     * @param parentSection The parentSection of the new SongPart. TimeSignature must match the specified rhythm. Can be null.
     * @return A new SongPart with the same name and same container.
     */
    SongPart getCopy(Rhythm r, int startBarIndex, int nbBars, CLI_Section parentSection);

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);

    /**
     * Return true if name, startBarIndex, nbBars, rhythm, parent section and all rp values match.
     *
     * @param spt
     * @return
     */
    default boolean isEqual(SongPart spt)
    {
        boolean b = false;
        if (getStartBarIndex() == spt.getStartBarIndex()
                && getNbBars() == spt.getNbBars()
                && getName().equals(spt.getName())
                && getRhythm() == spt.getRhythm()
                && getParentSection().equals(spt.getParentSection()))
        {
            b = getRhythm().getRhythmParameters().stream()
                    .allMatch(rp -> getRPValue(rp).equals(spt.getRPValue(rp)));
        }
        return b;
    }

    default String toShortString()
    {
        return "[" + getName() + ", " + getStartBarIndex() + "]";
    }
}
