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

/**
 *
 * A factory for RhythmParameter editors (whatever the rhythm) to be used in the SongPart editor.
 */
public interface RpEditorFactory
{

    /**
     * Try to find the relevant RpEditorFactory for the specified RhythmParameter.
     * <p>
     * First, return rp if rp is an instanceof RpEditorFactory. If not, scan all the RpEditorFactory instances available on the
     * global lookup, and return the first one which supports rp and is not a DefaultRpEditorFactory.
     *
     * @param rp
     * @return Can be null if no relevant RpEditorFactory found.
     */
    static public RpEditorFactory findFactory(RhythmParameter<?> rp)
    {
        if (rp instanceof RpEditorFactory)
        {
            return (RpEditorFactory) rp;
        }

        DefaultRpEditorFactory defaultFactory = DefaultRpEditorFactory.getDefault();

        for (var rvf : Lookup.getDefault().lookupAll(RpEditorFactory.class))
        {
            if (rvf.isSupported(rp) && rvf != defaultFactory)
            {
                return rvf;
            }
        }

        return null;
    }

    /**
     * Check if this RhythmParameter is handled by this factory.
     *
     * @param rp
     * @return
     */
    boolean isSupported(RhythmParameter<?> rp);

    /**
     * Create a generic RpEditor adapted to rp class (whatever the containing rhythm).
     *
     * @param song
     * @param spt
     * @param rp
     * @return Can be null if rp is not supported.
     */
    public RpEditor createRpEditor(Song song, SongPart spt, RhythmParameter<?> rp);

}
