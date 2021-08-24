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
package org.jjazz.rpcustomeditorfactoryimpl.spi;

import javax.swing.JPanel;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songcontext.api.SongContext;

/**
 * A JPanel to be used as a RhythmParameter value editor by the RealTimeRpEditorDialog.
 * <p>
 * The panel must fire PROP_EDITED_RP_VALUE property change events when RP value is changed by the user.
 * <p>
 *
 * @param <E> The type of value of the RhythmParameter.
 */
public abstract class RealTimeRpEditorPanel<E> extends JPanel
{

    /**
     * This property change event must be fired each time user modifies the value in the editor.
     */
    public static final String PROP_EDITED_RP_VALUE = "PropEditedRpValue";


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
     * @param sgContext Optional SongContext, can be null.
     */
    public abstract void preset(E rpValue, SongContext sgContext);

    /**
     * Change the edited value to rpValue.
     * <p>
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
