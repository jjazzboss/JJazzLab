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
package org.jjazz.rpcustomeditorfactoryimpl;

import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_DrumsMix;
import org.jjazz.ui.rpviewer.spi.RpCustomEditor;
import org.jjazz.ui.rpviewer.spi.RpCustomEditorFactory;
import org.openide.util.lookup.ServiceProvider;

/**
 * A factory for RpCustomEditors.
 * <p>
 */
@ServiceProvider(service = RpCustomEditorFactory.class)
public class RpCustomEditorFactoryImpl implements RpCustomEditorFactory
{

    @Override
    public boolean isSupported(RhythmParameter<?> rp)
    {
        boolean b = false;
        if (rp instanceof RP_SYS_DrumsMix)
        {
            b = true;
        }
        return b;
    }

    @Override
    public <E> RpCustomEditor<E> getEditor(RhythmParameter<E> rp)
    {
        RpCustomEditorImpl res = null;
        
     
        if (rp instanceof RP_SYS_DrumsMix)
        {
            var rpDrums = (RP_SYS_DrumsMix) rp;
            var editor = new RP_SYS_DrumsMixPanel(rpDrums);
            res = new RpCustomEditorImpl(editor);
        }
        
        return res;
    }


}
