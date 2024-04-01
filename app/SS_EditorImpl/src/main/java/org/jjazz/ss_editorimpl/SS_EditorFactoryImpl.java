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
package org.jjazz.ss_editorimpl;

import javax.swing.JToolBar;
import org.jjazz.song.api.Song;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerFactory;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.spi.SS_EditorFactory;
import org.jjazz.ss_editor.spi.SS_EditorSettings;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SS_EditorFactory.class)
public class SS_EditorFactoryImpl implements SS_EditorFactory
{

    @Override
    public SS_Editor createEditor(Song song, SS_EditorSettings settings, SptViewerFactory factory)
    {
        var editor = new SS_EditorImpl(song, settings, factory);
        var ssEditorController = new SS_EditorController(editor);
        editor.setController(ssEditorController);
        return editor;
    }

    @Override
    public JToolBar createEditorToolbar(SS_Editor editor)
    {
        return new SS_EditorToolBar(editor);
    }
}
