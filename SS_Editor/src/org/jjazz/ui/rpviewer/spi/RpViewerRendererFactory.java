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
import org.openide.util.Lookup;
import org.jjazz.ui.rpviewer.api.RpViewerRenderer;

/**
 * Provide RpVIewerRenderers implementations.
 */
public interface RpViewerRendererFactory
{

    /**
     * Try to find the relevant RpViewerRendererFactory for the specified RhythmParameter.
     * <p>
     * First, return rp if rp is an instanceof RpViewerRendererFactory. If not, scan all the RpViewerRendererFactory instances
     * available on the global lookup, and return the first one which supports rp and is not a DefaultRpViewerRendererFactory.
     *
     * @param rp
     * @return Can be null if no relevant RpViewerFactory found.
     */
    static public RpViewerRendererFactory findFactory(RhythmParameter<?> rp)
    {
        if (rp instanceof RpViewerRendererFactory)
        {
            return (RpViewerRendererFactory) rp;
        }

        var defaultFactory = DefaultRpViewerRendererFactory.getDefault();

        for (var rvf : Lookup.getDefault().lookupAll(RpViewerRendererFactory.class))
        {
            if (rvf.isSupported(rp) && rvf != defaultFactory)
            {
                return rvf;
            }
        }

        return null;
    }

    /**
     * Check if this factory can create a renderer for the specified RhythmParameter.
     *
     * @param rp
     * @return Default implementation returns true.
     */
    default boolean isSupported(RhythmParameter<?> rp)
    {
        return true;
    }

    /**
     * Get a RpViewerRenderer instance adapted to the specified RhyhtmParameter.
     * <p>
     *
     * @param rp
     * @param settings
     * @return Null if rp is not supported.
     */
    RpViewerRenderer getRpViewerRenderer(RhythmParameter<?> rp, RpViewerSettings settings);

}
