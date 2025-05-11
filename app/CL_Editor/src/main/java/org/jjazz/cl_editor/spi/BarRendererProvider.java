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

import java.util.Map;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererFactory;

/**
 * A BarRenderer provider.
 * <p>
 */
public interface BarRendererProvider
{

    /**
     * Provide the supported types by this provider, and whether they should be active by default when creating a new song.
     *
     * @return Can be empty but not null.
     */
    Map<String, Boolean> getSupportedTypes();

    /**
     * Create a BarRenderer for the specified parameters.
     *
     * @param editor
     * @param brType   The BarRenderer type
     * @param barIndex
     * @param settings
     * @param irf
     * @return Can be null if brType is not supported by this provider.
     */
    BarRenderer createBarRenderer(CL_Editor editor, String brType, int barIndex, BarRendererSettings settings, ItemRendererFactory irf);
}
