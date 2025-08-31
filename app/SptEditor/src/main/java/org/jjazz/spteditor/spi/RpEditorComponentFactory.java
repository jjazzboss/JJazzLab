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
package org.jjazz.spteditor.spi;

import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.openide.util.Lookup;
import org.jjazz.songstructure.api.SongPart;

/**
 *
 * A factory for RhythmParameter component editors (whatever the rhythm) to be used with a RpEditor in the SongPart editor.
 */
public interface RpEditorComponentFactory
{

    /**
     * Try to find the relevant RpEditorComponentFactory for the specified RhythmParameter.
     * <p>
     * First, return rp if rp is an instanceof RpEditorComponentFactory. If not, scan all the RpEditorComponentFactory instances
     * available on the global lookup, and return the first one which supports rp and is not a DefaultRpEditorComponentFactory.
     *
     * @param rp
     * @return Can be null if no relevant RpEditorComponentFactory found.
     */
    static public RpEditorComponentFactory findFactory(RhythmParameter<?> rp)
    {
        if (rp instanceof RpEditorComponentFactory)
        {
            return (RpEditorComponentFactory) rp;
        }

        DefaultRpEditorComponentFactory defaultFactory = DefaultRpEditorComponentFactory.getDefault();

        for (var rvf : Lookup.getDefault().lookupAll(RpEditorComponentFactory.class))
        {
            if (rvf.isRpSupported(rp) && rvf != defaultFactory)
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
    boolean isRpSupported(RhythmParameter<?> rp);

    /**
     * Create a RpEditorComponent adapted to rp class (whatever the containing rhythm).
     *
     * @param song Can be null except for RhythmParameters which implement the RpCustomEditorFactory interface
     * @param spt
     * @param rp
     * @return Can be null if rp is not supported.
     */
    public RpEditorComponent createComponent(Song song, SongPart spt, RhythmParameter<?> rp);

}
