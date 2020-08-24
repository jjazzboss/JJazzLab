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
package org.jjazz.ui.cl_editor;

import org.jjazz.song.api.Song;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_EditorFactory;
import org.jjazz.ui.cl_editor.api.CL_EditorSettings;
import org.jjazz.ui.cl_editor.barbox.api.BarBoxSettings;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererFactory;

public class CL_EditorFactoryImpl implements CL_EditorFactory
{

    static private CL_EditorFactoryImpl INSTANCE;

    static public CL_EditorFactoryImpl getInstance()
    {
        synchronized (CL_EditorFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new CL_EditorFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private CL_EditorFactoryImpl()
    {
    }

    @Override
    public CL_Editor createEditor(Song song)
    {
        return new CL_EditorImpl(song, CL_EditorSettings.getDefault(), BarBoxSettings.getDefault(), BarRendererFactory.getDefault());
    }

    @Override
    public CL_Editor createEditor(Song song, CL_EditorSettings settings, BarBoxSettings bbSettings, BarRendererFactory brf)
    {
        return new CL_EditorImpl(song, settings, bbSettings, brf);
    }
}
