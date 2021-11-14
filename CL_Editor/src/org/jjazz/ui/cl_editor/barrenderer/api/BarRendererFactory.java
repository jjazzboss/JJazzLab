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
     * BarRenderer type for chord symbols.
     */
    public final String BR_CHORD_SYMBOL = "BrChordSymbol";
    /**
     * BarRenderer type for chord positions.
     */
    public final String BR_CHORD_POSITION = "BrChordPosition";
    /**
     * BarRenderer type for sections.
     */
    public final String BR_SECTION = "BrSection";


    /**
     * Return the default implementation.
     * <p>
     * If brType is not handled by the default implementation, it tries to find a relevant BarRendererProvider in the global
     * lookup.
     *
     * @return
     */
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
     * @param brType
     * @param barIndex
     * @return
     */
    default BarRenderer createBarRenderer(CL_Editor editor, String brType, int barIndex)
    {
        return createBarRenderer(editor, brType, barIndex, BarRendererSettings.getDefault(), getItemRendererFactory());
    }

    /**
     * Create a BarRender of the specified type.
     *
     * @param editor Can be null
     * @param brType
     * @param barIndex
     * @param settings
     * @param irf
     * @return
     */
    BarRenderer createBarRenderer(CL_Editor editor, String brType, int barIndex, BarRendererSettings settings, ItemRendererFactory irf);

}
