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

import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.spteditor.RpEditorFactoryImpl;
import org.openide.util.Lookup;
import org.jjazz.songstructure.api.SongPart;

/**
 *
 * A factory for RhythmParameter editors suited for the SptEditor window.
 */
public abstract class RpEditorFactory
{

    public static RpEditorFactory getDefault()
    {
        RpEditorFactory result = Lookup.getDefault().lookup(RpEditorFactory.class);
        if (result == null)
        {
            result = RpEditorFactoryImpl.getInstance();
        }
        return result;
    }

    /**
     * Automatically create a RpEditor adapted to the specified parameters.
     *
     * @param spt
     * @param rp
     * @return
     */
    abstract public RpEditor createRpEditor(SongPart spt, RhythmParameter<?> rp);

}
