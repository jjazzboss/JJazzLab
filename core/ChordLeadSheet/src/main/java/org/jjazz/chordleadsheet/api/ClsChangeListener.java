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
package org.jjazz.chordleadsheet.api;

import java.util.EventListener;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;

/**
 * Objects that want to listen to ChordLeadSheet changes must implement this interface.
 */
public interface ClsChangeListener  extends EventListener
{
    /**
     * Process the change.
     * <p>
     * Note that this method might be called outside of the EDT.
     *
     * @param e
     * @throws UnsupportedEditException If a ClsVetoableChangeEvent is not authorized by a listener.
     */
    public void chordLeadSheetChanged(ClsChangeEvent e) throws UnsupportedEditException;
}
