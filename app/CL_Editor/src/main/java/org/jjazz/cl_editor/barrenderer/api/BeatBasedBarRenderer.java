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
package org.jjazz.cl_editor.barrenderer.api;

import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.harmony.api.Position;

/**
 * A BarRenderer who displays events based on beat position within a bar must implements this interface.
 */
public interface BeatBasedBarRenderer
{

    /**
     * The TimeSignature for this bar.
     *
     * @return
     */
    public TimeSignature getTimeSignature();

    /**
     * @param x int The x coordinate within this bar.
     * @return Position The leadsheet position which corresponds to the x coordinate.
     */
    public Position getPositionFromPoint(int x);
}
