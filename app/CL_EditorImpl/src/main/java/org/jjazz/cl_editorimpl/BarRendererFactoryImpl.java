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
package org.jjazz.cl_editorimpl;

import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.spi.BarRendererFactory;
import org.jjazz.cl_editor.spi.BarRendererProvider;
import org.jjazz.cl_editor.spi.BarRendererSettings;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererFactory;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service=BarRendererFactory.class)
public class BarRendererFactoryImpl implements BarRendererFactory
{
 
    @Override
    public BarRenderer createBarRenderer(CL_Editor editor, String brType, int barIndex, BarRendererSettings settings, ItemRendererFactory irf)
    {
        BarRenderer br = null;
        switch (brType)
        {
            case BarRendererFactory.BR_CHORD_SYMBOL -> br = new BR_Chords(editor, barIndex, settings, irf);
            case BarRendererFactory.BR_CHORD_POSITION -> br = new BR_ChordPositions(editor, barIndex, settings,irf);
            case BarRendererFactory.BR_SECTION -> br = new BR_Sections(editor, barIndex, settings, irf);
            case BarRendererFactory.BR_ANNOTATION -> br = new BR_Annotation(editor, barIndex, settings, irf);
            default ->
            {
                // Search a provider in the global lookup
                var brProviders = Lookup.getDefault().lookupAll(BarRendererProvider.class);
                for (var brProvider : brProviders)
                {
                    br = brProvider.createBarRenderer(editor, brType, barIndex, settings, irf);
                    if (br != null)
                    {
                        break;
                    }
                }
                if (br == null)
                {
                    throw new IllegalStateException("No BarRendererProvider found for brType=" + brType);
                }
            }
        }

        // Set the model
        var model = editor.getModel();
        int modelBarIndex = barIndex < model.getSizeInBars() ? barIndex : -1;
        br.resetModel(model, modelBarIndex);
        return br;
    }
}
