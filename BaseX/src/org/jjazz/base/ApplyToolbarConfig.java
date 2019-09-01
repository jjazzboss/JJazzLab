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
package org.jjazz.base;

import java.awt.EventQueue;
import java.util.logging.Logger;
import org.openide.awt.ToolbarPool;
import org.openide.windows.OnShowing;

/**
 * Class only used to apply the toolbar configuration defined in the layer file ("Toolbars" directory).
 * <p>
 * Because could not find another way to get rid of the Profiling toolbar by default !<br>
 * There is still the Warning upon run : "Not all children in Toolbars/ marked with the position attribute: [MusicControls,
 * MyToolBarConfig.xml], but some are: [File, Clipboard, UndoRedo, Memory]" Did not find how to get rid of this, although
 * MusicControls "position" is specified in my MyToolBarConfig.xml.
 */
@OnShowing
public class ApplyToolbarConfig implements Runnable
{

    private static final Logger LOGGER = Logger.getLogger(ApplyToolbarConfig.class.getSimpleName());

    /**
     * Will be executed when Netbeans app UI is ready (@onShowing)
     */
    @Override
    public void run()
    {
        assert EventQueue.isDispatchThread();
        LOGGER.fine("ApplyToolbarConfig.run() --");
        ToolbarPool.getDefault().setConfiguration("MyToolBarConfig");
    }
}
