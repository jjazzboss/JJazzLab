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
package org.jjazz.musiccontrol.api.playbacksession;

import org.jjazz.rhythmmusicgeneration.api.ContextChordSequence;

/**
 * PlaybackSession additional capability: the sequence contains special Meta marker Midi events "csIndex=XX" to indicate current
 * chord symbol index in a chord sequence.
 */
public interface ChordSymbolProvider
{

    /**
     * The chord sequence used to retrieve the chord symbol from the index passed in the Meta market event.
     *
     * @return Null if no meaningful value can be returned
     */
    ContextChordSequence getContextChordGetSequence();
}
