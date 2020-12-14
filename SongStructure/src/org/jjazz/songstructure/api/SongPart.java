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
package org.jjazz.songstructure.api;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.util.IntRange;

/**
 * A song part defines how a rhythm is played for a number of bars starting at startBarIndex.
 * <p>
 * Rhythm generation is controlled by the RhythmParameters values. Rhythm can not be changed but a SongPart can be cloned using a
 * new rhythm.
 */
public interface SongPart extends Transferable
{

    /**
     * oldValue=rhythm parameter, newValue=value.
     */
    public static final String PROPERTY_RP_VALUE = "SptRpValue";   //NOI18N 
    public static final String PROPERTY_START_BAR_INDEX = "SptStartBarIndex";   //NOI18N 
    public static final String PROPERTY_NB_BARS = "SptNbBars";   //NOI18N 
    public static final String PROPERTY_NAME = "SptName";   //NOI18N 
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(SongPart.class, "Song Part");

    public int getStartBarIndex();

    /**
     * The size of the song part in bars (same as the parentSection size).
     *
     * @return
     */
    public int getNbBars();

    /**
     * Convenience method.
     *
     * @return The range [getStartBarIndex(); getStartBarIndex()+getNbBars()-1]
     */
    default public IntRange getBarRange()
    {
        return new IntRange(getStartBarIndex(), getStartBarIndex() + getNbBars() - 1);
    }

    /**
     * By default set to the parentSection's name
     *
     * @return
     */
    public String getName();

    /**
     * An optional CLI_Section associated to this SongPart.
     *
     * @return
     */
    public CLI_Section getParentSection();

    /**
     * Get the value of a RhythmParameter.
     *
     * @param <T>
     * @param rp
     * @return the java.lang.Object
     */
    public <T> T getRPValue(RhythmParameter<T> rp);

    public Rhythm getRhythm();

    /**
     * @return The SongStructure this object belong to. Set by SongStructure when the SongPart is added.
     */
    public SongStructure getContainer();

    /**
     * Create a new SongPart with same name based on this object.
     * <p>
     * Parameters of the new SongPart can be adjusted.<br>
     * If using a different rhythm, try to adapt the value of compatible RhythmParameters.
     *
     * @param r The new Rhythm to be used. If null Rhythm is unchanged.
     * @param startBarIndex The startBarIndex of the new SongPart
     * @param nbBars The nbBars of the new SongPart
     * @param parentSection The parentSection of the new SongPart. TimeSignature must match the specified rhythm. Can be null.
     * @return A new SongPart.
     */
    public SongPart clone(Rhythm r, int startBarIndex, int nbBars, CLI_Section parentSection);

    public void addPropertyChangeListener(PropertyChangeListener l);

    public void removePropertyChangeListener(PropertyChangeListener l);
}
