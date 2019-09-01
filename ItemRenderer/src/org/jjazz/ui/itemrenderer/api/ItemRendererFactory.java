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
package org.jjazz.ui.itemrenderer.api;

import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.ui.itemrenderer.ItemRendererFactoryImpl;
import org.openide.util.Lookup;

/**
 * Provide a consistent set of ItemRenderer instances.
 */
public abstract class ItemRendererFactory
{

    public static ItemRendererFactory getDefault()
    {
        ItemRendererFactory result = Lookup.getDefault().lookup(ItemRendererFactory.class);
        if (result == null)
        {
            return ItemRendererFactoryImpl.getInstance();
        }
        return result;
    }

    /**
     * Create an itemrenderer of specified type and set its model to item.
     *
     * @param type
     * @param item
     * @return
     */
    abstract public ItemRenderer createItemRenderer(IR_Type type, ChordLeadSheetItem<?> item);

    /**
     * Create an ItemRenderer used to represent a dragged item.
     *
     * @param type
     * @param item
     * @return Can be null if specified ItemRenderer's type can not be dragged.
     */
    abstract public ItemRenderer createDraggedItemRenderer(IR_Type type, ChordLeadSheetItem<?> item);

    /**
     * Get a shared instance of an ItemRenderer sample of given type.
     *
     * @param type
     * @return
     */
    abstract public ItemRenderer getItemRendererSample(IR_Type type);
}
