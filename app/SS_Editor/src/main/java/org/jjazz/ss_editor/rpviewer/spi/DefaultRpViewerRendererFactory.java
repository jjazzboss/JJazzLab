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
package org.jjazz.ss_editor.rpviewer.spi;

import org.jjazz.ss_editor.rpviewer.api.RpViewerSettings;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.openide.util.Lookup;
import org.jjazz.ss_editor.rpviewer.api.RpViewerRenderer;

/**
 * The default RpViewerRenderer factory.
 * <p>
 * This factory must handle the default JJazzLab RhythmParameters (RP_State, RP_Integer, RP_StringSet, etc.), and provide a default renderer for unknown
 * RhythmParameters.
 * <p>
 */
public interface DefaultRpViewerRendererFactory extends RpViewerRendererFactory
{

    /**
     * The types of renderers supported by this factory.
     */
    public enum Type
    {
        METER, STRING
    }

    /**
     * Get the default implementation from the global lookup.
     * <p>
     *
     * @return
     */
    static public DefaultRpViewerRendererFactory getDefault()
    {
        DefaultRpViewerRendererFactory result = Lookup.getDefault().lookup(DefaultRpViewerRendererFactory.class);
        if (result == null)
        {
            throw new IllegalStateException("No instance found in global lookup");
        }
        return result;
    }

    /**
     * Get the RpViewerRenderer of the specified type.
     *
     * @param song     The context song
     * @param spt      The context song part
     * @param rp       The context rp
     * @param type
     * @param settings
     * @return
     */
    RpViewerRenderer getRpViewerRenderer(Song song, SongPart spt, RhythmParameter<?> rp, Type type, RpViewerSettings settings);
    
}
