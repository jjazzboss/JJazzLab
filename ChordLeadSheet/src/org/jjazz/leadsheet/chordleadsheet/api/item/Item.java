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

import java.beans.PropertyChangeListener;

/**
 * The base interface for all items that are part of a leadsheet.
 *
 * @param <T>
 */
public interface Item<T>
{

    public static String PROP_ITEM_DATA = "ItemData";
    public static String PROP_ITEM_POSITION = "ItemPosition";

    /**
     * Get the data part of this item.
     *
     * @return
     */
    public T getData();

    /**
     * Get the position of this item.
     *
     * @return
     */
    public Position getPosition();

    /**
     * Add a listener to item's data and position changes.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    public void removePropertyChangeListener(PropertyChangeListener listener);
}
