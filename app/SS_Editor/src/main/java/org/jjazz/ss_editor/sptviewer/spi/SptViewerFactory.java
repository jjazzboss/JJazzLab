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

import org.openide.util.Lookup;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.sptviewer.api.SptViewer;
import org.jjazz.ss_editor.rpviewer.spi.DefaultRpViewerRendererFactory;

/**
 * A factory for SongPart editors (SptViewers).
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

    /**
     * Create a SptViewer which contains the RpViewers.
     *
     * @param ssEditor
     * @param spt
     * @param settings
     * @param factory
     * @return
     */
    SptViewer createLowSptViewer(SS_Editor ssEditor, SongPart spt, SptViewerSettings settings, DefaultRpViewerRendererFactory factory);

    /**
     * Create a top SptViewer for name etc.
     *
     * @param ssEditor
     * @param spt
     * @param settings
     * @return
     */
    SptViewer createTopSptViewer(SS_Editor ssEditor, SongPart spt, SptViewerSettings settings);
}
