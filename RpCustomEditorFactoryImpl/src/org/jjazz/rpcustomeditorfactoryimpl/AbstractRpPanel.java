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
package org.jjazz.rpcustomeditorfactoryimpl;

import java.util.logging.Logger;
import javax.swing.JPanel;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;

/**
 * A JPanel to be used as a RhythmParameter value editor by GenericDialog.
 * <p>
 * Use addPropertyChangeListener(PROP_RP_VALUE) to get notified when the RP_Value changes.
 *
 * @param <E> The type of value of the RhythmParameter.
 */
public abstract class AbstractRpPanel<E> extends JPanel
{

    /**
     * This property change event must be fired each time user modifies the value in the editor.
     */
    public static final String PROP_RP_VALUE = "PropRpValue";


    private static final Logger LOGGER = Logger.getLogger(AbstractRpPanel.class.getSimpleName());


    // =======================================================================
    // Abstract methods
    // =======================================================================
    /**
     * The RhythmParameter model.
     *
     * @return
     */
    public abstract RhythmParameter<E> getRhythmParameter();


    /**
     * Initialize the editor for the specified context.
     *
     * @param rpValue Can not be null.
     * @param spt Optional SongPart, can be null.
     */
    public abstract void preset(E rpValue, SongPart spt);

    /**
     * Change the edited value to rpValue.
     * <p>
     * This must NOT trigger a PROP_RP_VALUE change event.
     *
     * @param rpValue Can not be null.
     */
    public abstract void setEditedRpValue(E rpValue);

    /**
     * Get a copy of the current RhythmParameter value in the editor.
     *
     * @return Can be null if a problem occured or RpValue is not yet ready.
     */
    public abstract E getEditedRpValue();

    /**
     * Called to cleanup possible resources before discarding this object.
     */
    public abstract void cleanup();
}
