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

import java.beans.PropertyChangeEvent;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.harmony.api.Position;

/**
 * Mutating methods of a ChordLeadSheetItem.
 * <p>
 * These methods should be *ONLY* called by the enclosing ChordLeadSheet methods which manage global synchronization. It means that WritableItem methods
 * implementations do not have to use lock. The interface is made public only to enable creation of custom ChordLeadSheetItem subclasses.
 *
 * @param <T>
 * @see org.jjazz.chordleadsheet.api.ChordLeadSheet
 */
public interface WritableItem<T> extends ChordLeadSheetItem<T>
{

    /**
     *
     * @param pos
     * @return The event to be fired by caller.
     */
    PropertyChangeEvent setPosition(Position pos);

    /**
     *
     * @param data
     * @return The event to be fired by caller.
     */
    PropertyChangeEvent setData(T data);

    /**
     * Set an optional container for this item.
     * <p>
     * No consistency check is performed: caller is responsible to make sure this item is consistent with cls (e.g. position is within cls bounds, section does
     * not have a name clash, ...).
     *
     * @param cls Can be null.
     * @return The event to be fired by caller.
     */
    PropertyChangeEvent setContainer(ChordLeadSheet cls);

    /**
     * Fire an event to the ChordLeadSheetItem listeners.
     *
     * @param event
     */
    void firePropertyChangeEvent(PropertyChangeEvent event);


}
