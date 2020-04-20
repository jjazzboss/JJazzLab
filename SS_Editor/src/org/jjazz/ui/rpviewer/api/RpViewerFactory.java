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
package org.jjazz.ui.rpviewer.api;

import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.rpviewer.RpViewerFactoryImpl;
import org.openide.util.Lookup;
import org.jjazz.songstructure.api.SongPart;

/**
 * Provide a consistent set of RpViewer implementations for the SS_Editor window.
 */
public interface RpViewerFactory
{

    /**
     * The types of RPs supported by this factory.
     */
    public enum Type
    {
        Meter, String, Percentage
    }

    public static RpViewerFactory getDefault()
    {
        RpViewerFactory result = Lookup.getDefault().lookup(RpViewerFactory.class);
        if (result == null)
        {
            return RpViewerFactoryImpl.getInstance();
        }
        return result;
    }

    /**
     * Automatically create a RpViewer adapted to the RhyhtmParameter type.
     *
     * @param spt
     * @param rp
     * @return
     */
    RpViewer createRpViewer(SongPart spt, RhythmParameter<?> rp);

    /**
     * Create a RpViewer of a specific type.
     *
     * @param type
     * @param spt
     * @param rp
     * @return May be null if RpViewer requested type is not compatible with rp.
     */
    RpViewer createRpViewer(Type type, SongPart spt, RhythmParameter<?> rp);
}
