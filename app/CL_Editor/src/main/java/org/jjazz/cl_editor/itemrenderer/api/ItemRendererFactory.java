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
package org.jjazz.cl_editor.itemrenderer.api;

import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.openide.util.Lookup;

/**
 * Provide a consistent set of ItemRenderer instances.
 */
public interface ItemRendererFactory
{
    public static ItemRendererFactory getDefault()
    {
        ItemRendererFactory result = Lookup.getDefault().lookup(ItemRendererFactory.class);
        if (result == null)
        {
            throw new IllegalStateException("No ItemRendererFactory instance found in lookup");
        }
        return result;
    }

    /**
     * Create an itemrenderer of specified type and set its model to item.
     *
     * @param type
     * @param item
     * @param irSettings
     * @return
     */
    ItemRenderer createItemRenderer(IR_Type type, ChordLeadSheetItem<?> item, ItemRendererSettings irSettings);

    /**
     * Create an ItemRenderer used to represent a dragged item.
     *
     * @param type
     * @param item
     * @param irSettings
     * @return Can be null if specified ItemRenderer's type can not be dragged.
     */
    ItemRenderer createDraggedItemRenderer(IR_Type type, ChordLeadSheetItem<?> item, ItemRendererSettings irSettings);

    /**
     * Get a shared instance of an ItemRenderer sample of given type.
     *
     * @param type
     * @param irSettings
     * @return
     */
    ItemRenderer getItemRendererSample(IR_Type type, ItemRendererSettings irSettings);
}
