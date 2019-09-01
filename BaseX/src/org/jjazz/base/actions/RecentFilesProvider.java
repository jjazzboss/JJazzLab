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
package org.jjazz.base.actions;

import java.beans.PropertyChangeListener;
import java.io.File;
import org.openide.util.Lookup;

/**
 * Provides opened/closed file information to the OpenRecentFile action.
 */
public abstract class RecentFilesProvider
{

    /**
     * This property change event must be fired when a file is opened. NewValue is the file object. This property should also be
     * fired the first time a new file is saved.
     */
    public static final String PROP_FILE_OPENED = "FileOpened";
    /**
     * This property change event must be fired when a file is closed. NewValue is the file object.
     */
    public static final String PROP_FILE_CLOSED = "FileClosed";

    public static RecentFilesProvider getDefault()
    {
        RecentFilesProvider result = Lookup.getDefault().lookup(RecentFilesProvider.class);
        return result;
    }

    /**
     * The open action associated to f : it will be called by the corresponding Open Recent Files menu item.
     *
     * @param f
     * @return False is object represented by f could not be opened.
     */
    public abstract boolean open(File f);

    public abstract void addPropertyChangeListener(PropertyChangeListener l);

    public abstract void removePropertyChangeListener(PropertyChangeListener l);
}
