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
package org.jjazz.ui.cl_editor.barrenderer.api;

import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.barrenderer.BarRendererFactoryImpl;
import org.jjazz.ui.itemrenderer.api.ItemRendererFactory;
import org.openide.util.Lookup;

/**
 * Provide a consistent set of BarRenderer implementations.
 */
public interface BarRendererFactory
{

    /**
     * The types of BarRenderers supported by this factory.
     */
    public enum Type
    {
        ChordSymbol, ChordPosition, Section
    }

    public static BarRendererFactory getDefault()
    {
        BarRendererFactory result = Lookup.getDefault().lookup(BarRendererFactory.class);
        if (result == null)
        {
            return BarRendererFactoryImpl.getInstance();
        }
        return result;
    }

    default ItemRendererFactory getItemRendererFactory()
    {
        return ItemRendererFactory.getDefault();
    }

    /**
     * Create a BarRender with default settings.
     *
     * @param editor Can be null
     * @param type
     * @param barIndex
     * @param model
     * @return
     */
    default BarRenderer createBarRenderer(CL_Editor editor, Type type, int barIndex, ChordLeadSheet model)
    {
        return createBarRenderer(editor, type, barIndex, model, BarRendererSettings.getDefault(), getItemRendererFactory());
    }

    /**
     * Create a BarRender of the specified type.
     *
     * @param editor Can be null
     * @param type
     * @param barIndex
     * @param model
     * @param settings
     * @param irf
     * @return
     */
    BarRenderer createBarRenderer(CL_Editor editor, Type type, int barIndex, ChordLeadSheet model, BarRendererSettings settings, ItemRendererFactory irf);

}
