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
package org.jjazz.ui.cl_editor.barrenderer;

import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererFactory;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererSettings;
import org.jjazz.ui.cl_editor.barrenderer.spi.BarRendererProvider;
import org.jjazz.ui.itemrenderer.api.ItemRendererFactory;
import org.openide.util.Lookup;

public class BarRendererFactoryImpl implements BarRendererFactory
{

    private static BarRendererFactoryImpl INSTANCE;

    public static BarRendererFactoryImpl getInstance()
    {
        synchronized (BarRendererFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new BarRendererFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private BarRendererFactoryImpl()
    {
    }

    /**
     * Use the default ItemRendererFactory.
     *
     * @param editor Can be null
     * @param brType
     * @param barIndex
     * @return
     */
    @Override
    public BarRenderer createBarRenderer(CL_Editor editor, String brType, int barIndex, BarRendererSettings settings, ItemRendererFactory irf)
    {
        BarRenderer br = null;
        switch (brType)
        {
            case BarRendererFactory.BR_CHORD_SYMBOL:
                br = new BR_Chords(editor, barIndex, settings, irf);
                break;
            case BarRendererFactory.BR_CHORD_POSITION:
                br = new BR_ChordPositions(editor, barIndex, settings, irf);
                break;
            case BarRendererFactory.BR_SECTION:
                br = new BR_Sections(editor, barIndex, settings, irf);
                break;
            default:
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
                    throw new IllegalStateException("No BarRendererProvider found for brType=" + brType);   //NOI18N
                }
        }

        // Set the model
        var model = editor.getModel();
        int modelBarIndex = barIndex < model.getSize() ? barIndex : -1;
        br.setModel(modelBarIndex, model);
        return br;
    }
}
