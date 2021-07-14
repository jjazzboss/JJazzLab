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

import java.util.List;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

/**
 * PlaybackSession additional capability: the sequence contains CTRL_CHG_JJAZZ_BEAT_CHANGE controller events to indicate the
 * current natural beat position.
 * <p>
 */
public interface PositionProvider
{

    /**
     * The list used to convert a CTRL_CHG_JJAZZ_BEAT_CHANGE event natural beat position into a Position.
     *
     * @return Null if not meaningful value can be returned.
     */
    public List<Position> getPositions();
}
