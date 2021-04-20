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

import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ui.spteditor.DefaultRpEditorFactoryImpl;
import org.openide.util.Lookup;

/**
 * A special RpEditorFactory only for the default RhythmParameters.
 */
public interface DefaultRpEditorFactory extends RpEditorFactory
{
    /**
     * The default RpEditorFactory.
     * <p>
     * If an instance is available in the global lookup, return it, otherwise return a default implementation.
     *
     * @return
     */
    static public DefaultRpEditorFactory getDefault()
    {
        DefaultRpEditorFactory result = Lookup.getDefault().lookup(DefaultRpEditorFactory.class);
        if (result == null)
        {
            result = DefaultRpEditorFactoryImpl.getInstance();
        }
        return result;
    }  

    /**
     * The DefaultRpViewerFactory must provide a RpEditor for all rps.
     *
     * @param rp
     * @return True
     */
    @Override
    default boolean isSupported(RhythmParameter<?> rp)
    {
        return true;
    }

}
