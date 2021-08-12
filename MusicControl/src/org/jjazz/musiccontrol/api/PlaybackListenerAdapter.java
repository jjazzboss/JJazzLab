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
package org.jjazz.musiccontrol.api;

import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.songstructure.api.SongPart;

/**
 * Convenience class.
 * <p>
 * Note that methods are called on the Swing Event Dispatching Thread. Event handling must be time-efficient.
 */
public class PlaybackListenerAdapter implements PlaybackListener
{

    @Override
    public void beatChanged(Position oldPos, Position newPos)
    {
        // Do nothing
    }

    @Override
    public void barChanged(int oldBar, int newBar)
    {
        // Do nothing
    }

    @Override
    public void midiActivity(long tick, int channel)
    {
        // Do nothing
    }

    @Override
    public void chordSymbolChanged(CLI_ChordSymbol chordSymbol)
    {
        // Do nothing
    }

    @Override
    public void songPartChanged(SongPart newSpt)
    {
        // Do nothing
    }

    @Override
    public void enabledChanged(boolean b)
    {
        // Do nothing
    }
}
