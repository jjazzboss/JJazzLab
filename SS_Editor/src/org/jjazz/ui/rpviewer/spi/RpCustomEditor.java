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

import javax.swing.JDialog;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songcontext.api.SongPartContext;
import org.openide.windows.WindowManager;

/**
 * A RhythmParameter-specific dialog to edit its value.
 * <p>
 * @param <E> RhythmParameter value class
 */
public abstract class RpCustomEditor<E> extends JDialog
{

    public RpCustomEditor(boolean modal)
    {
        super(WindowManager.getDefault().getMainWindow(), modal);
    }

    /**
     * The RhythmParameter whose value is edited in this dialog.
     *
     * @return
     */
    public abstract RhythmParameter<E> getRhythmParameter();

    /**
     * Initialize the dialog before being shown.
     *
     *
     * @param rpValue The RhythmParameter value
     * @param sptContext 
     */
    public abstract void preset(E rpValue, SongPartContext sptContext);


    /**
     * Get the edited RpValue.
     *
     * @return Value is meaningful only if isExitOk() is true.
     */
    public abstract E getRpValue();

    /**
     * Check if dialog was exited using OK
     *
     * @return False means user cancelled the operation.
     */
    public abstract boolean isExitOk();

}
