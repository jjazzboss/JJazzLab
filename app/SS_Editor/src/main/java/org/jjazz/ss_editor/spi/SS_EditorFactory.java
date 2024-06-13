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
package org.jjazz.ss_editor.spi;

import javax.swing.JToolBar;
import org.jjazz.song.api.Song;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerFactory;
import org.jjazz.ss_editor.api.SS_Editor;
import org.openide.util.Lookup;

public interface SS_EditorFactory
{

    /**
     * Get the default implementation found in the global lookup.
     *
     * @return
     */
    public static SS_EditorFactory getDefault()
    {
        SS_EditorFactory rlef = Lookup.getDefault().lookup(SS_EditorFactory.class);
        if (rlef == null)
        {
            throw new IllegalArgumentException("No instance found in global lookup");
        }
        return rlef;
    }
    
    SS_Editor createEditor(Song song, SS_EditorSettings settings, SptViewerFactory factory);
    
      /**
     * Create the side editor toolbar.
     *
     * @param editor
     * @return
     */
    JToolBar createEditorToolbar(SS_Editor editor);
}
