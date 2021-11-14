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
package org.jjazz.songstructure.api;

import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.songstructure.api.event.SgsChangeEvent;

/**
 * Listen to a SongStructure changes.
 */
public interface SgsChangeListener
{

    /**
     * Some change events might need to be authorized by all listeners before being processed by songStructureChanged().
     *
     * @param e The change to authorize.
     * @throws UnsupportedEditException Listener shall throw this exception if change is not acceptable. Exception message might
     * be shown to user to explain the problem.
     */
    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException;


    /**
     * Process the change.
     * <p>
     * Note that this method might be called outside of the EDT.
     *
     * @param e
     * @throws IllegalStateException If change is not authorized by this listener.
     */
    public void songStructureChanged(SgsChangeEvent e);

}
