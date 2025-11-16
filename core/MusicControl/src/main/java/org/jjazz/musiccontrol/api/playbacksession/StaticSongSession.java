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
package org.jjazz.musiccontrol.api.playbacksession;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jjazz.musiccontrol.api.SongMusicGenerationListener;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.songcontext.api.SongContext;

/**
 * A BaseSongSession which becomes dirty as soon as the SongContext has changed musically.
 * <p>
 */
public class StaticSongSession extends BaseSongSession
{

    private static final List<StaticSongSession> sessions = new ArrayList<>();
    private SongMusicGenerationListener songMusicGenerationListener;

    /**
     * Create or reuse a session for the specified parameters.
     * <p>
     * <p>
     * Sessions are cached: if a non-dirty session in the NEW or GENERATED state already exists for the same parameters then return it, otherwise a new session
     * is created.
     * <p>
     *
     * @param sgContext
     * @param sConfig
     * @param context
     * @return A session in the NEW or GENERATED state.
     */
    static public StaticSongSession getSession(SongContext sgContext, SessionConfig sConfig, Context context)
    {
        Objects.requireNonNull(sgContext);
        Objects.requireNonNull(sConfig);
        StaticSongSession session = findSession(sgContext, sConfig, context);
        if (session == null)
        {
            final StaticSongSession newSession = new StaticSongSession(sgContext, sConfig, context);
            sessions.add(newSession);
            return newSession;
        } else
        {
            return session;
        }
    }

    /**
     * Get a session with a default SessionConfig.
     * <p>
     *
     * @param sgContext
     * @param context
     * @return A targetSession in the NEW or GENERATED state.
     */
    static public StaticSongSession getSession(SongContext sgContext, Context context)
    {
        return getSession(sgContext, new SessionConfig(), context);
    }


    private StaticSongSession(SongContext sgContext, SessionConfig sConfig, Context context)
    {
        super(sgContext, sConfig, true, context);
    }

    /**
     * Overridden to add our songMusicGenerationListener.
     *
     * @param silent
     * @throws MusicGenerationException
     */
    @Override
    public void generate(boolean silent) throws MusicGenerationException
    {
        super.generate(silent);

        var song = getSongContext().getSong();
        songMusicGenerationListener = new SongMusicGenerationListener(song, getSongContext().getMidiMix(), 0);  // 0ms because we can't miss an event which might disable updates
        songMusicGenerationListener.addPropertyChangeListener(this);
    }

    @Override
    public void close()
    {
        super.close();
        sessions.remove(this);
        songMusicGenerationListener.removePropertyChangeListener(this);
        songMusicGenerationListener.cleanup();
    }

    // ============================================================================================
    // PropertyChangeListener interface
    // ============================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        super.propertyChange(e);

        if (!getState().equals(PlaybackSession.State.GENERATED))
        {
            return;
        }

        //LOGGER.fine("propertyChange() e=" + e);
        if (e.getSource() == songMusicGenerationListener && e.getPropertyName().equals(SongMusicGenerationListener.PROP_MUSIC_GENERATION_COMBINED))
        {
            setDirty();
        }
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================
    /**
     * Find an identical existing session in state NEW or GENERATED and non-dirty.
     *
     * @param sgContext
     * @param config
     * @return Null if not found
     */
    static private StaticSongSession findSession(SongContext sgContext, SessionConfig config, Context context)
    {
        for (var session : sessions)
        {
            if ((session.getState().equals(PlaybackSession.State.GENERATED) || session.getState().equals(PlaybackSession.State.NEW))
                    && !session.isDirty()
                    && sgContext.equals(session.getSongContext())
                    && config.equals(session.getSessionConfig())
                    && context == session.getContext())
            {
                return session;
            }
        }
        return null;
    }

    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================

}
