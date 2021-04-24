
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
package org.jjazz.ui.sptviewer;

import org.jjazz.ui.sptviewer.api.SptViewer;
import org.jjazz.ui.sptviewer.spi.SptViewerFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.sptviewer.spi.SptViewerSettings;
import org.jjazz.ui.rpviewer.spi.DefaultRpRendererFactory;

/**
 *
 */
public class SptViewerFactoryImpl implements SptViewerFactory
{

    static private SptViewerFactoryImpl INSTANCE;

    static public SptViewerFactoryImpl getInstance()
    {
        synchronized (SptViewerFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new SptViewerFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private SptViewerFactoryImpl()
    {
    }

    @Override
    public SptViewer createSptViewer(SongPart spt, SptViewerSettings settings, DefaultRpRendererFactory factory)
    {
        SptViewer sptv = new SptViewerImpl(spt, settings, factory);
        return sptv;
    }

}
