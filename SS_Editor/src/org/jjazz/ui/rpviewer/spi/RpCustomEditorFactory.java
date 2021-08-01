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

/**
 * Provide RpCustomEditor implementations.
 */
public interface RpCustomEditorFactory
{

    /**
     * Try to find the relevant for the specified RhythmParameter.
     * <p>
     * First, return rp if rp is an instanceof RpCustomEditorFactory. If not, scan all the instances available on the global
     * lookup, and return the first one which supports rp.
     *
     * @param rp
     * @return Can be null if no relevant factory found.
     */
    static public RpCustomEditorFactory findFactory(RhythmParameter<?> rp)
    {
        if (rp instanceof RpCustomEditorFactory)
        {
            return (RpCustomEditorFactory) rp;
        }

        for (var rvf : Lookup.getDefault().lookupAll(RpCustomEditorFactory.class))
        {
            if (rvf.isSupported(rp))
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
     * Get a RpCustomEditor instance for the specific RhythmParameter.
     * <p>
     *
     * @param <E> RhythmParameter value class
     * @param rp
     * @return 
     */
    public <E> RpCustomEditor<E> getEditor(RhythmParameter<E> rp);

}
