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
package org.jjazz.ss_editorimpl.rpviewer;

import java.util.function.Supplier;
import org.jjazz.ss_editor.rpviewer.api.StringRpRenderer;
import org.jjazz.ss_editor.rpviewer.api.MeterRpRenderer;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RP_Integer;
import org.jjazz.rhythm.api.RP_StringSet;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.rpviewer.api.RpViewerSettings;
import org.jjazz.ss_editor.rpviewer.api.RpViewerRenderer;
import org.jjazz.ss_editor.rpviewer.spi.DefaultRpViewerRendererFactory;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = DefaultRpViewerRendererFactory.class)
public class DefaultRpRendererFactoryImpl implements DefaultRpViewerRendererFactory
{

    private static final Logger LOGGER = Logger.getLogger(DefaultRpRendererFactoryImpl.class.getSimpleName());


    @Override
    public RpViewerRenderer getRpViewerRenderer(Song song, SongPart spt, RhythmParameter<?> rp, Type type, RpViewerSettings settings)
    {
        RpViewerRenderer renderer = switch (type)
        {
            case METER ->
                new MeterRpRenderer(song, spt);
            case STRING ->
                new StringRpRenderer(song, spt, () -> ((RhythmParameter) rp).getDisplayValue(spt.getRPValue(rp)), settings.getStringRpRendererSettings());
            default -> throw new AssertionError(type.name());
        };

        return renderer;
    }

    @Override
    public RpViewerRenderer getRpViewerRenderer(Song song, SongPart spt, RhythmParameter<?> rp, RpViewerSettings settings)
    {
        RpViewerRenderer rpr;

        if (rp instanceof RP_SYS_TempoFactor rpTF)
        {
            Supplier<String> stringSupplier = () -> 
            {
                int rpValue = spt.getRPValue(rpTF);
                float rpValueFloat = rpValue / 100f;
                int tempo = (int) (Math.round(song.getTempo() * rpValueFloat));
                return rpValue + "% (" + tempo + ")";
            };

            rpr = new StringRpRenderer(song, spt, stringSupplier, settings.getStringRpRendererSettings());

            // Keep it updated when tempo changes
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
            rpr = getRpViewerRenderer(song, spt, rp, Type.METER, settings);
        } else if (rp instanceof RP_StringSet)
        {
            rpr = getRpViewerRenderer(song, spt, rp, Type.STRING, settings);
        } else
        {
            rpr = getRpViewerRenderer(song, spt, rp, Type.STRING, settings);
        }
        return rpr;
    }

    // =================================================================================
    // Private methods
    // =================================================================================    
}
