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
package org.jjazz.cl_editor.spi;

import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererFactory;
import org.netbeans.api.annotations.common.NonNull;
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
     * BarRenderer type for bar annotation.
     */    
    public final String BR_ANNOTATION = "BrAnnotation";


    /**
     * Return the default implementation.
     * <p>
     *
     * @return
     */
    public static BarRendererFactory getDefault()
    {
        BarRendererFactory result = Lookup.getDefault().lookup(BarRendererFactory.class);
        if (result == null)
        {
            throw new IllegalArgumentException("No BarRendererFactory instance found");
        }
        return result;
    }

    default ItemRendererFactory getItemRendererFactory()
    {
        return ItemRendererFactory.getDefault();
    }

    /**
     * Create a BarRenderer with default settings.
     *
     * @param editor   Can be null
     * @param brType
     * @param barIndex The barIndex and the modelBarIndex (unless barIndex is beyond ChordLeadSheet size, then modelBarIndex is set to -1)
     * @return
     */
    default BarRenderer createBarRenderer(CL_Editor editor, String brType, int barIndex)
    {
        return createBarRenderer(editor, brType, barIndex, BarRendererSettings.getDefault(), getItemRendererFactory());
    }

    /**
     * Create a BarRenderer of the specified type.
     *
     * @param editor   Can be null
     * @param brType
     * @param barIndex The barIndex and the modelBarIndex (unless barIndex is beyond ChordLeadSheet size, then modelBarIndex is set to -1)
     * @param settings
     * @param irf
     * @return
     */
    @NonNull
    BarRenderer createBarRenderer(CL_Editor editor, String brType, int barIndex, BarRendererSettings settings, ItemRendererFactory irf);

}
