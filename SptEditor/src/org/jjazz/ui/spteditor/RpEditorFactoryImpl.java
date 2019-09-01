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
package org.jjazz.ui.spteditor;

import org.jjazz.rhythm.parameters.RP_Integer;
import org.jjazz.rhythm.parameters.RP_State;
import org.jjazz.rhythm.parameters.RP_StringSet;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.spteditor.spi.RpEditor;
import org.jjazz.ui.spteditor.spi.RpEditorFactory;
import org.jjazz.songstructure.api.SongPart;

public class RpEditorFactoryImpl extends RpEditorFactory
{

    private static RpEditorFactoryImpl INSTANCE;

    public static RpEditorFactoryImpl getInstance()
    {
        synchronized (RpEditorFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RpEditorFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private RpEditorFactoryImpl()
    {
    }

    @Override
    public RpEditor createRpEditor(SongPart spt, RhythmParameter<?> rp)
    {
        RpEditor rpe;
        if (rp instanceof RP_Integer || rp instanceof RP_State)
        {
            rpe = new RpEditorSpinner(spt, rp);
        } else if (rp instanceof RP_StringSet)
        {
            rpe = new RpEditorList(spt, rp);
        } else
        {
            throw new IllegalArgumentException("No RpEditor found for this rp type. rp=" + rp);
        }
        return rpe;
    }

}
