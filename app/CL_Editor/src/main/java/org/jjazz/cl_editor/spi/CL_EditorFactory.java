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
package org.jjazz.cl_editor.spi;

import javax.swing.JToolBar;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.song.api.Song;
import org.openide.util.Lookup;

public interface CL_EditorFactory
{

    /**
     * Get the default instance.
     *
     * @return
     */
    static public CL_EditorFactory getDefault()
    {
        CL_EditorFactory clef = Lookup.getDefault().lookup(CL_EditorFactory.class);
        if (clef == null)
        {
            throw new IllegalStateException("No CL_EditorFactory instance found");
        }
        return clef;
    }

    /**
     * Create an editor with the default settings.
     *
     * @param song
     * @return
     */
    default CL_Editor createEditor(Song song)
    {
        return createEditor(song, CL_EditorSettings.getDefault(), BarBoxFactory.getDefault(), BarRendererFactory.getDefault());
    }

    /**
     * Create an editor with the specified settings.
     *
     * @param song
     * @param settings
     * @param bbf
     * @param brf
     * @return
     */
    CL_Editor createEditor(Song song, CL_EditorSettings settings, BarBoxFactory bbf, BarRendererFactory brf);


    /**
     * Create the side editor toolbar.
     *
     * @param editor
     * @return
     */
    JToolBar createEditorToolbar(CL_Editor editor);
}
