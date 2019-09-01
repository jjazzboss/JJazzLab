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
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.ui.ss_editor.api.RL_EditorFactory;

public class RL_EditorFactoryImpl extends RL_EditorFactory
{

    static private RL_EditorFactoryImpl INSTANCE;

    static public RL_EditorFactoryImpl getInstance()
    {
        synchronized (RL_EditorFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RL_EditorFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private RL_EditorFactoryImpl()
    {
    }

    @Override
    public SS_Editor createEditor(Song song)
    {
        return new RL_EditorImpl(song);
    }
}
