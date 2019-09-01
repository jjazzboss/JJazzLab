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
import org.jjazz.ui.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererFactory;

public class BarRendererFactoryImpl extends BarRendererFactory
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

    @Override
    public BarRenderer createBarRenderer(Type type, int barIndex, ChordLeadSheet model)
    {
        BarRenderer br = null;
        switch (type)
        {
            case ChordSymbol:
                br = new BR_Chords(barIndex);
                break;
            case ChordPosition:
                br = new BR_ChordPositions(barIndex);
                break;
            case Section:
                br = new BR_Sections(barIndex);
                break;
            default:
                throw new IllegalStateException("type=" + type);
        }
        // Set the model
        int modelBarIndex = barIndex < model.getSize() ? barIndex : -1;
        br.setModel(modelBarIndex, model);
        return br;
    }
}
