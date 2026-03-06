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
 * Music generation is controlled by the RhythmParameter values. SongPart instances are mutable but they can be only modified via their SongStructure container.
 * <p>
 */
public interface SongPart extends Transferable
{

    /**
     * Fired when a new rhythm parameter value was set (rhythm parameter values are immutable).
     * <p>
     * oldValue=rhythm parameter, newValue=value.
     */
    public static final String PROP_RP_VALUE = "SptRpValue";
    public static final String PROP_START_BAR_INDEX = "SptStartBarIndex";
    public static final String PROP_NB_BARS = "SptNbBars";
    public static final String PROP_NAME = "SptName";
    /**
     * The rhythm and/or the parent section has changed.
     * <p>
     * oldValue=old rhythm, newValue=old parent section.
     */
    public static final String PROP_RHYTHM_PARENT_SECTION = "SptRhythmParentSection";
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
    IntRange getBarRange();

    /**
     * By default set to the parentSection's name
     *
     * @return Cannot be null
     */
    String getName();

    /**
     * The CLI_Section associated to this SongPart.
     *
     * @return Cannot be null
     */
    CLI_Section getParentSection();

    /**
     * Get the RhythmParameter value.
     *
     * @param <T>
     * @param rp
     * @return Cannot be null
     * @throws IllegalArgumentException If rp is not a valid RhythmParameter for this SongPart
     */
    <T> T getRPValue(RhythmParameter<T> rp);

    /**
     *
     * @return Cannot be null
     */
    Rhythm getRhythm();

    /**
     * @return The SongStructure this object belong to. Cannot be null.
     */
    SongStructure getContainer();

    StringProperties getClientProperties();

    /**
     * Create a new SongPart based on this object.
     * <p>
     * If using a different rhythm, method tries to adapt the value of compatible RhythmParameters. ClientProperties are also copied.
     *
     * @param r             The new Rhythm to be used. If null Rhythm is unchanged.
     * @param startBarIndex The startBarIndex of the new SongPart
     * @param nbBars        The nbBars of the new SongPart
     * @param parentSection The parentSection of the new SongPart. TimeSignature must match the specified rhythm. If null parent section is unchanged.
     * @return A new SongPart whose name and container are unchanged.
     */
    SongPart getCopy(Rhythm r, int startBarIndex, int nbBars, CLI_Section parentSection);

    /**
     * Add a listener.
     * <p>
     * Listeners will be called on the EDT. Because listeners are called outside read/write lock, listeners should rely on event's embedded data rather than on
     * the model itself (which could theoretically be modified by another thread).
     *
     * @param l
     */
    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);

    /**
     * @return "[name,startBarIndex]"
     */
    String toShortString();
}
