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
package org.jjazz.ui.rpviewer;

import org.jjazz.ui.rpviewer.api.StringRpRenderer;
import org.jjazz.ui.rpviewer.api.MeterRpRenderer;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RP_Integer;
import org.jjazz.rhythm.api.RP_StringSet;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.rpviewer.spi.RpViewerSettings;
import org.jjazz.ui.rpviewer.api.RpViewerRenderer;
import org.jjazz.ui.rpviewer.spi.DefaultRpViewerRendererFactory;

public class DefaultRpRendererFactoryImpl implements DefaultRpViewerRendererFactory
{

    private static DefaultRpRendererFactoryImpl INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(DefaultRpRendererFactoryImpl.class.getSimpleName());

    public static DefaultRpRendererFactoryImpl getInstance()
    {
        synchronized (DefaultRpRendererFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DefaultRpRendererFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private DefaultRpRendererFactoryImpl()
    {
    }

    @Override
    public RpViewerRenderer getRpViewerRenderer(Song song, SongPart spt, Type type, RpViewerSettings settings)
    {
        RpViewerRenderer renderer = null;
        switch (type)
        {
            case METER:
                renderer = new MeterRpRenderer(song, spt);
                break;
            case STRING:
                renderer = new StringRpRenderer(song, spt, rpStrValue -> rpStrValue.toString(), settings.getStringRpRendererSettings());
                break;
            default:
                throw new AssertionError(type.name());
        }

        return renderer;
    }

    @Override
    public RpViewerRenderer getRpViewerRenderer(Song song, SongPart spt, RhythmParameter<?> rp, RpViewerSettings settings)
    {
        RpViewerRenderer rpr;

        if (rp instanceof RP_SYS_TempoFactor)
        {
            rpr = new StringRpRenderer(song, spt, rpStrValue -> song.getTempo() + " / " + rpStrValue, settings.getStringRpRendererSettings());
            song.addPropertyChangeListener(evt ->
            {
                if (evt.getPropertyName().equals(Song.PROP_TEMPO))
                {
                    ((StringRpRenderer) rpr).fireChanged();
                }
            }
            );
        } else if (rp instanceof RP_Integer)
        {
            rpr = getRpViewerRenderer(song, spt, Type.METER, settings);
        } else if (rp instanceof RP_StringSet)
        {
            rpr = getRpViewerRenderer(song, spt, Type.STRING, settings);
        } else
        {
            rpr = getRpViewerRenderer(song, spt, Type.STRING, settings);
        }
        return rpr;
    }

    // =================================================================================
    // Private methods
    // =================================================================================    
}
