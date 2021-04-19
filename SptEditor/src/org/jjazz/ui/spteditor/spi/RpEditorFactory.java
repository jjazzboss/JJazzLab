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
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.openide.util.Lookup;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.spteditor.RpEditorFactoryImpl;

/**
 *
 * A factory for RhythmParameter editors (whatever the rhythm) to be used in the SongPart editor.
 */
public interface RpEditorFactory
{

    /**
     * The default implementation.
     *
     * @return
     */
    public static RpEditorFactory getDefault()
    {
        return RpEditorFactoryImpl.getInstance();
    }

    /**
     * Try to get a generic RpEditor from one of the RpEditorFactory service providers.
     * <p>
     * The method calls createRpEditor() on each service provider found in the global lookup until it gets a non-null
     * RpEditor. If no RpEditor returned then use getDefault().
     *
     * @param song
     * @param spt
     * @param rp
     * @return Can't be null
     */
    public static RpEditor getRpEditor(Song song, SongPart spt, RhythmParameter<?> rp)
    {
        for (RpEditorFactory f : Lookup.getDefault().lookupAll(RpEditorFactory.class))
        {
            RpEditor rpe = f.createRpEditor(song, spt, rp);
            if (rpe != null)
            {
                return rpe;
            }
        }
        return getDefault().createRpEditor(song, spt, rp);
    }


    /**
     * Get a custom RpEditor if possible, otherwise a generic one.
     * <p>
     * The method first tries to get a custom editor using CustomRpEditorFactory.getCustomRpEditor(), if null it returns
     * getRpEditor().
     *
     * @param song
     * @param spt
     * @param rp
     * @return Can't be null
     */
    public static RpEditor getCustomOrGenericRpEditor(Song song, SongPart spt, RhythmParameter<?> rp)
    {
        RpEditor rpe = CustomRpEditorFactory.getCustomRpEditor(song, spt, rp);
        if (rpe != null)
        {
            return rpe;
        } else
        {
            return getRpEditor(song, spt, rp);
        }
    }

    /**
     * Create a generic RpEditor adapted to rp class (whatever the containing rhythm).
     *
     * @param song
     * @param spt
     * @param rp
     * @return
     */
    public RpEditor createRpEditor(Song song, SongPart spt, RhythmParameter<?> rp);

}
