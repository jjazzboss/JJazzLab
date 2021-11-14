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
package org.jjazz.improvisionsupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererSettings;
import org.jjazz.ui.cl_editor.barrenderer.spi.BarRendererProvider;
import org.jjazz.ui.itemrenderer.api.ItemRendererFactory;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provider our BarRenderer
 */
@ServiceProvider(service = BarRendererProvider.class)
public class ImproSupportBrProvider implements BarRendererProvider
{

    public static final String BR_IMPRO_SUPPORT = "BrImproSupport";

    @Override
    public BarRenderer createBarRenderer(CL_Editor editor, String brType, int barIndex, BarRendererSettings settings, ItemRendererFactory irf)
    {
        BarRenderer res = null;
        if (brType.equals(BR_IMPRO_SUPPORT))
        {
            res = new BR_ImproSupport(editor, barIndex, settings, irf);
        }
        return res;
    }

    @Override
    public Map<String, Boolean> getSupportedTypes()
    {
        var res = new HashMap<String, Boolean>();
        res.put(BR_IMPRO_SUPPORT, Boolean.FALSE);
        return res;
    }


}
