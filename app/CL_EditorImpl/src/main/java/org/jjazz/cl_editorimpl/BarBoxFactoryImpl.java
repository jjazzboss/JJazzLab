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

import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.barbox.api.BarBox;
import org.jjazz.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.cl_editor.spi.BarRendererFactory;
import org.jjazz.cl_editor.spi.BarBoxFactory;
import org.jjazz.cl_editor.spi.BarBoxSettings;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = BarBoxFactory.class)
public class BarBoxFactoryImpl implements BarBoxFactory
{
    @Override
    public BarBox create(
            CL_Editor editor,
            int bbIndex,
            int clsModelBarIndex,
            ChordLeadSheet model,
            BarBoxConfig config,
            BarBoxSettings settings,
            BarRendererFactory brf)
    {
        return new BarBoxImpl(editor, bbIndex, clsModelBarIndex, model, config, settings, brf);
    }

}
