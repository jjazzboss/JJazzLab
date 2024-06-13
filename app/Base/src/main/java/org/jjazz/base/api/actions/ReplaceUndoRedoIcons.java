/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.base.api.actions;

import java.util.logging.Logger;
import javax.swing.Action;
import org.openide.awt.Actions;
import org.openide.windows.OnShowing;

/**
 * Replace at runtime the undo and redo icons.
 * <p>
 * Note: it would be simpler to use the JJazzLab branding directory to substitute the Netbeans undo/redo actions icons, but it is
 * a .gif and we need a .png...
 */
@OnShowing
public class ReplaceUndoRedoIcons implements Runnable
{

    private static final Logger LOGGER = Logger.getLogger(ReplaceUndoRedoIcons.class.getSimpleName());

    @Override
    public void run()
    {
        Action action = Actions.forID("Edit", "org.openide.actions.UndoAction");
        action.putValue("iconBase", "org/jjazz/base/api/actions/resources/undo.png");
        action = Actions.forID("Edit", "org.openide.actions.RedoAction");
        action.putValue("iconBase", "org/jjazz/base/api/actions/resources/redo.png");

    }

}
