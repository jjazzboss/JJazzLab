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
package org.jjazz.uisettings.api;

import org.jjazz.uisettings.NoteColorManagerImpl;
import java.awt.Color;
import org.openide.util.Lookup;

/**
 * Compute note colors depending on note velocity.
 */
public interface NoteColorManager
{

    static NoteColorManager getDefault()
    {
        NoteColorManager result = Lookup.getDefault().lookup(NoteColorManager.class);
        if (result == null)
        {
            return NoteColorManagerImpl.getInstance();
        }
        return result;
    }

    /**
     * Compute the note color by adjusting refColor.
     *
     * @param refColor The reference color for velocity = 64.
     * @param velocity
     * @return
     */
    Color getNoteColor(Color refColor, int velocity);

    /**
     * Provide a note color depending on velocity.
     * <p>
     * The method uses shades of 3 different colors. Colors are cached.
     *
     * @param velocity
     * @return
     */
    Color getNoteColor(int velocity);

    /**
     * Same as getNoteColor() but for a selected note.
     *
     * @param velocity
     * @return
     */
    Color getSelectedNoteColor(int velocity);
}
