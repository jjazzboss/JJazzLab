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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jjazz.musiccontrol.SongContextSession;
import org.jjazz.rhythmmusicgeneration.api.SongContext;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;

/**
 * Create PlaybackSessions.
 */
public class SessionFactory implements PropertyChangeListener
{

    private static SessionFactory INSTANCE;

    private List<SongContextSession> songContextSessions = new ArrayList<>();

    public static SessionFactory getInstance()
    {
        synchronized (SessionFactory.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new SessionFactory();
            }
        }
        return INSTANCE;
    }

    private SessionFactory()
    {

    }

    /**
     * Get a SongContext-based session for the specified parameters.
     * <p>
     * If an existing session in the NEW or GENERATED state exists for the same parameters, return it. Otherwise a new session is
     * created.
     * <p>
     * The session implements all the PlaybackSession capabilities (e.g. ChordSymbolProvider, PositionProvider, etc.). The
     * sequence takes into account the MusicController playback transposition. The session handles the click/precount features
     * (generate click/precount tracks, listen to ClickManager for enabling/disabling). The session listens to MidiMix changes to
     * update the tracks muted status.
     *
     * @param sgContext
     * @param postProcessors
     * @return A session in the NEW or GENERATED state.
     */
    public PlaybackSession getSongContextSession(SongContext sgContext, MusicGenerator.PostProcessor... postProcessors)
    {
        if (sgContext == null)
        {
            throw new IllegalArgumentException("sgContext=" + sgContext);
        }
        SongContextSession session = findSongContextSessionGenerated(sgContext, postProcessors);
        if (session == null)
        {
            session = new SongContextSession(sgContext, postProcessors);
            session.addPropertyChangeListener(this);
            songContextSessions.add(session);
        }
        return session;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {

        if (evt.getPropertyName().equals(PlaybackSession.PROP_STATE))
        {
            PlaybackSession session = (PlaybackSession) evt.getSource();
            if (session.getState().equals(PlaybackSession.State.CLOSED))
            {
                songContextSessions.remove(session);
            }
        }
    }

    // ===================================================================================
    // Private methods
    // ===================================================================================
    /**
     * Find an identical existing session in state NEW or GENERATED.
     *
     * @param sgContext
     * @param postProcessors
     * @return Null if not found
     */
    private SongContextSession findSongContextSessionGenerated(SongContext sgContext, MusicGenerator.PostProcessor... postProcessors)
    {
        for (var session : songContextSessions)
        {
            if ((session.getState().equals(PlaybackSession.State.GENERATED) || session.getState().equals(PlaybackSession.State.NEW))
                    && sgContext.equals(session.getSongContext())
                    && Objects.equals(session.getPostProcessors(), Arrays.asList(postProcessors)))
            {
                return session;
            }
        }
        return null;
    }
}
