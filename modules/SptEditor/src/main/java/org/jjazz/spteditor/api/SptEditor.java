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
package org.jjazz.spteditor.api;

import javax.swing.JPanel;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.jjazz.spteditor.spi.DefaultRpEditorComponentFactory;

/**
 * A JPanel which lets user edit the currently selected SongParts.
 * <p>
 * Its Lookup must contain :<br>
 * - editor's ActionMap<br>
 * - edited Song<br>
 * - the selected SongParts<br>
 */
public abstract class SptEditor extends JPanel implements Lookup.Provider
{

    abstract public UndoRedo getUndoManager();

    abstract public SptEditorSettings getSettings();

    abstract public DefaultRpEditorComponentFactory getDefaultRpEditorComponentFactory();

    /**
     * Clean up everything so component can be garbaged.
     */
    abstract public void cleanup();

}
