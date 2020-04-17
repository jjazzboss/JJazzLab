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
package org.jjazz.ui.spteditor.spi;

import org.jjazz.ui.spteditor.api.RpEditor;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.song.api.Song;
import org.openide.util.Lookup;
import org.jjazz.songstructure.api.SongPart;

/**
 *
 * A factory for RhythmParameter editors (specific to a given rhythm) to be used in the SongPart editor.
 */
public interface CustomRpEditorFactory
{

    /**
     * Try to get a custom RpEditor from one of the CustomRpEditorFactory service providers.
     * <p>
     * The method calls createCustomRpEditor() on each service provider found in the global lookup until it gets a non-null
     * RpEditor.
     *
     * @param spt
     * @param rp
     * @return Can be null
     */
    public static RpEditor getCustomRpEditor(Song song, SongPart spt, RhythmParameter<?> rp)
    {
        for (CustomRpEditorFactory f : Lookup.getDefault().lookupAll(CustomRpEditorFactory.class))
        {
            RpEditor rpe = f.createCustomRpEditor(song, spt, rp);
            if (rpe != null)
            {
                return rpe;
            }
        }
        return null;
    }

    /**
     * Create a custom RpEditor adapted to rp class AND spt Rhythm.
     *
     * @param song
     * @param spt
     * @param rp
     * @return
     */
    public RpEditor createCustomRpEditor(Song song, SongPart spt, RhythmParameter<?> rp);

}
