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
package org.jjazz.ss_editor.sptviewer.spi;

import org.jjazz.song.api.Song;
import org.openide.util.Lookup;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.sptviewer.api.SptViewer;
import org.jjazz.ss_editor.rpviewer.spi.DefaultRpViewerRendererFactory;

/**
 * A factory for SongPartEditors.
 */
public interface SptViewerFactory
{

    /**
     * Get the default implementation found in the global lookup.
     *
     * @return
     */
    public static SptViewerFactory getDefault()
    {
        SptViewerFactory rpef = Lookup.getDefault().lookup(SptViewerFactory.class);
        if (rpef == null)
        {
            throw new IllegalArgumentException("No instance found in global lookup");
        }
        return rpef;
    }

    default DefaultRpViewerRendererFactory getDefaultRpViewerFactory()
    {
        return DefaultRpViewerRendererFactory.getDefault();
    }

    default SptViewerSettings getDefaultSptViewerSettings()
    {
        return SptViewerSettings.getDefault();
    }

    SptViewer createSptViewer(Song song, SongPart spt, SptViewerSettings settings, DefaultRpViewerRendererFactory factory);
}
