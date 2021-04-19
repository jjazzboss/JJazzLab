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
package org.jjazz.ui.ss_editor;

import org.jjazz.song.api.Song;
import org.jjazz.ui.sptviewer.spi.SptViewerFactory;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.ui.ss_editor.spi.SS_EditorFactory;
import org.jjazz.ui.ss_editor.spi.SS_EditorSettings;

public class SS_EditorFactoryImpl extends SS_EditorFactory
{

    static private SS_EditorFactoryImpl INSTANCE;

    static public SS_EditorFactoryImpl getInstance()
    {
        synchronized (SS_EditorFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new SS_EditorFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private SS_EditorFactoryImpl()
    {
    }

    @Override
    public SS_Editor createEditor(Song song, SS_EditorSettings settings, SptViewerFactory factory)
    {
        return new SS_EditorImpl(song, settings, factory);
    }
}
