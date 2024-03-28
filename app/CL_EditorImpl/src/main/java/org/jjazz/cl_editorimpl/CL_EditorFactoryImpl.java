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

import javax.swing.JToolBar;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.spi.CL_EditorSettings;
import org.jjazz.cl_editor.spi.BarRendererFactory;
import org.jjazz.cl_editor.spi.CL_EditorFactory;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service=CL_EditorFactory.class)
public class CL_EditorFactoryImpl implements CL_EditorFactory
{

    @Override
    public CL_Editor createEditor(Song song, CL_EditorSettings settings, BarRendererFactory brf)
    {
        var editor = new CL_EditorImpl(song, settings, brf);
        var controller = new CL_EditorController(editor);     // This will update our songModel via Song.putClientProperty() by updating the zoom factors 
        editor.setEditorMouseListener(controller);

        return editor;

    }

    @Override
    public JToolBar createEditorToolbar(CL_Editor editor)
    {
        return new CL_EditorToolBar(editor);
    }
}
