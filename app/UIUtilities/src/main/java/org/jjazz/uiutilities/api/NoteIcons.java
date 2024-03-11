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
package org.jjazz.uiutilities.api;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jjazz.harmony.api.SymbolicDuration;

/**
 * Access to note icons.
 */
public class NoteIcons
{

    /**
     * Get a 20x30 icon for the specified symbolic duration.
     *
     * @param sd
     * @return Can be null.
     */
    static public Icon get20x30(SymbolicDuration sd)
    {
        var path = switch (sd)
        {
            case SIXTEENTH_TRIPLET ->
                "resources/NoteSixteenthTriplet-20x30.png";
            case SIXTEENTH ->
                "resources/NoteSixteenth-20x30.png";
            case EIGHTH_TRIPLET ->
                "resources/NoteEighthTriplet-20x30.png";
            case EIGHTH ->
                "resources/NoteEighth-20x30.png";
            case QUARTER ->
                "resources/NoteQuarter-20x30.png";
            case HALF ->
                "resources/NoteHalf-20x30.png";
            default ->
                null;
        };
        Icon res = path != null ? new ImageIcon(NoteIcons.class.getResource(path)) : null;
        return res;
    }
}
