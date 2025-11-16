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
package org.jjazz.musiccontrol.api;

import java.util.EnumSet;
import java.util.Objects;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.songstructure.api.SongPart;

/**
 * Convenience class.
 * <p>
 * Note that methods are called on the Swing Event Dispatching Thread. Event handling must be time-efficient.
 */
public class PlaybackListenerAdapter implements PlaybackListener
{

    public final EnumSet<PlaybackSession.Context> acceptedContexts;

    /**
     * Create a PlaybackListenerAdapter which accepts any PlaybackSession.
     * <p>
     */
    public PlaybackListenerAdapter()
    {
        this(EnumSet.allOf(PlaybackSession.Context.class));
    }

    /**
     * Create a PlaybackListenerAdapter which accepts only the specified PlaybackSession contexts.
     * <p>
     * @param acceptedContexts Can not be null
     */
    public PlaybackListenerAdapter(EnumSet<PlaybackSession.Context> acceptedContexts)
    {
        Objects.requireNonNull(acceptedContexts);
        this.acceptedContexts = acceptedContexts;
    }

    public EnumSet<PlaybackSession.Context> getAcceptedContexts()
    {
        return acceptedContexts;
    }

    /**
     * Accept if session.getContext()
     *
     * @param session
     * @return
     */
    @Override
    public boolean isAccepted(PlaybackSession session)
    {
        Objects.requireNonNull(session);
        boolean b = acceptedContexts.contains(session.getContext());
        return b;
    }

    @Override
    public void beatChanged(Position oldPos, Position newPos, float newPosInBeats)
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
