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
package org.jjazz.ui.rpviewer.spi;

import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.rpviewer.api.RpViewer;
import org.openide.util.Lookup;

/**
 * Provide RpViewer implementations for the SS_Editor window.
 */
public interface RpViewerFactory
{

    /**
     * Try to find the relevant RpViewerFactory for the specified RhythmParameter.
     * <p>
     * First, return rp if rp is an instanceof RpViewerFactory. If not, scan all the RpViewerFactory instances available on the
     * global lookup, and return the first one which supports rp and is not a DefaultRpViewerFactory.
     *
     * @param rp
     * @return Can be null if no relevant RpViewerFactory found.
     */
    static public RpViewerFactory findFactory(RhythmParameter<?> rp)
    {
        if (rp instanceof RpViewerFactory)
        {
            return (RpViewerFactory) rp;
        }

        DefaultRpViewerFactory defaultFactory = DefaultRpViewerFactory.getDefault();

        for (var rvf : Lookup.getDefault().lookupAll(RpViewerFactory.class))
        {
            if (rvf.isSupported(rp) && rvf != defaultFactory)
            {
                return rvf;
            }
        }

        return null;
    }

    /**
     *
     * @param rp
     * @return True if this factory can create a RpViewer for this RhythmParameter.
     */
    boolean isSupported(RhythmParameter<?> rp);

    /**
     * Automatically create a RpViewer adapted to the RhyhtmParameter type.
     *
     * @param spt
     * @param rp
     * @param settings
     * @return Null if rp is not supported.
     */
    RpViewer createRpViewer(SongPart spt, RhythmParameter<?> rp, RpViewerSettings settings);

}
