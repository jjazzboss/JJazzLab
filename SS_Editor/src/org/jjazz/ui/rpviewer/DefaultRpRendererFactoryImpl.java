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

import java.util.logging.Logger;
import org.jjazz.rhythm.parameters.RP_SYS_TempoFactor;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.parameters.RP_Integer;
import org.jjazz.rhythm.parameters.RP_StringSet;
import org.jjazz.ui.rpviewer.spi.RpViewerSettings;
import org.jjazz.ui.rpviewer.spi.DefaultRpRendererFactory;
import org.jjazz.ui.rpviewer.api.RpRenderer;

public class DefaultRpRendererFactoryImpl implements DefaultRpRendererFactory
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
    public RpRenderer getRpRenderer(Type type, RpViewerSettings settings)
    {
        RpRenderer renderer = null;
        switch (type)
        {
            case METER:
                renderer = new MeterRpRenderer();
                break;
            case STRING:
                renderer = new StringRpRenderer(rpValue -> rpValue.toString(), settings.getStringRpRendererSettings());
                break;
            case STRING_SET:
                renderer = new StringRpRenderer(rpValue ->
                {
                    String res = rpValue.toString();
                    return res.equals("[]") ? "" : res;
                }, settings.getStringRpRendererSettings());
                break;
            case PERCENTAGE:
                renderer = new StringRpRenderer(rpValue ->
                {
                    String res = rpValue.toString();
                    if (res.isBlank())
                    {
                        return "";
                    }
                    return res + "%";
                }, settings.getStringRpRendererSettings());
                break;
            default:
                throw new AssertionError(type.name());
        }

        return renderer;
    }

    @Override
    public RpRenderer getRpRenderer(RhythmParameter<?> rp, RpViewerSettings settings)
    {
        RpRenderer rpr;

        if (rp instanceof RP_SYS_TempoFactor)
        {
            rpr = getRpRenderer(Type.PERCENTAGE, settings);
        } else if (rp instanceof RP_Integer)
        {
            rpr = getRpRenderer(Type.METER, settings);
        } else if (rp instanceof RP_StringSet)
        {
            rpr = getRpRenderer(Type.STRING_SET, settings);
        } else
        {
            rpr = getRpRenderer(Type.STRING, settings);
        }
        return rpr;
    }

    // =================================================================================
    // Private methods
    // =================================================================================    
}
