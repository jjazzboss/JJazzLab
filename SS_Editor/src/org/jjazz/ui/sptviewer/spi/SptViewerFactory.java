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
package org.jjazz.ui.sptviewer.spi;

import org.jjazz.ui.sptviewer.SptViewerFactoryImpl;
import org.openide.util.Lookup;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.rpviewer.spi.DefaultRpViewerFactory;
import org.jjazz.ui.sptviewer.api.SptViewer;

/**
 * A factory for SongPartEditors.
 */
public interface SptViewerFactory
{

    public static SptViewerFactory getDefault()
    {
        SptViewerFactory rpef = Lookup.getDefault().lookup(SptViewerFactory.class);
        if (rpef == null)
        {
            rpef = SptViewerFactoryImpl.getInstance();
        }
        return rpef;
    }

    default DefaultRpViewerFactory getDefaultRpViewerFactory()
    {
        return DefaultRpViewerFactory.getDefault();
    }

    SptViewer createDefaultEditor(SongPart spt, SptViewerSettings settings, DefaultRpViewerFactory factory);
}
