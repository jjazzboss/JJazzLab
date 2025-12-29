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

import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.barbox.api.BarBox;
import org.jjazz.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererFactory;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.util.Lookup;

/**
 * Provide BarBox implementations.
 */
public interface BarBoxFactory
{

    /**
     * Return the default implementation.
     * <p>
     *
     * @return
     */
    public static BarBoxFactory getDefault()
    {
        BarBoxFactory result = Lookup.getDefault().lookup(BarBoxFactory.class);
        if (result == null)
        {
            throw new IllegalArgumentException("No BarBoxFactory instance found");
        }
        return result;
    }

    /**
     * Construct a BarBox.
     *
     * @param editor        Can be null
     * @param bbIndex       The index of this BarBox.
     * @param clsModelBarIndex Use -1 if this BarBox does not represent model data (typically a bar beyond cls model size)
     * @param model
     * @param config
     * @param settings
     * @param brf
     * @return
     */
    BarBox create(
            CL_Editor editor, 
            int bbIndex, 
            int clsModelBarIndex,
            ChordLeadSheet model,
            BarBoxConfig config,
            BarBoxSettings settings,
            BarRendererFactory brf);

}
