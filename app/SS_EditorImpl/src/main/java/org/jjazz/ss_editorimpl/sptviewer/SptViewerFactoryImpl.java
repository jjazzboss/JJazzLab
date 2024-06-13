
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
package org.jjazz.ss_editorimpl.sptviewer;

import org.jjazz.song.api.Song;
import org.jjazz.ss_editor.sptviewer.api.SptViewer;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerSettings;
import org.jjazz.ss_editor.rpviewer.spi.DefaultRpViewerRendererFactory;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service=SptViewerFactory.class)
public class SptViewerFactoryImpl implements SptViewerFactory
{
  
    @Override
    public SptViewer createSptViewer(Song song, SongPart spt, SptViewerSettings settings, DefaultRpViewerRendererFactory factory)
    {
        SptViewer sptv = new SptViewerImpl(song, spt, settings, factory);
        return sptv;
    }

}
