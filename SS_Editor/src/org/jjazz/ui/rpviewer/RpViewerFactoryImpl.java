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

import org.jjazz.rhythm.parameters.RP_SYS_TempoFactor;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.rpviewer.api.RpViewerFactory;
import org.jjazz.ui.rpviewer.api.RpViewer;
import org.jjazz.songstructure.api.SongPart;

public class RpViewerFactoryImpl implements RpViewerFactory
{

    private static RpViewerFactoryImpl INSTANCE;

    public static RpViewerFactoryImpl getInstance()
    {
        synchronized (RpViewerFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RpViewerFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private RpViewerFactoryImpl()
    {
    }

    @Override
    public RpViewer createRpViewer(Type type, SongPart spt, RhythmParameter<?> rp)
    {
        if (!spt.getRhythm().getRhythmParameters().contains(rp))
        {
            throw new IllegalStateException("type=" + type + " spt=" + spt + " rp=" + rp);
        }
        RpViewer e = null;
        switch (type)
        {
            case Meter:
                e = new MeterRpViewer(spt, rp);
                break;
            case String:
                e = new StringRpViewer(spt, rp, rpValue ->
                {
                    String res = rpValue.toString();
                    if (res.equals("[]"))
                    {
                        // This can happen if rpValue is an empty collection
                        res = "";
                    }
                    return res;
                });
                break;
            case Percentage:
                e = new StringRpViewer(spt, rp, rpValue ->
                {
                    String res = rpValue.toString();
                    if (res.isBlank())
                    {
                        return "";
                    }
                    return res + "%";
                });
                break;
            default:
                throw new IllegalStateException("type=" + type);
        }
        return e;
    }

    @Override
    public RpViewer createRpViewer(SongPart spt, RhythmParameter<?> rp)
    {
        RpViewer rpv;
        Object value = spt.getRPValue(rp);
        if (rp instanceof RP_SYS_TempoFactor)
        {
            rpv = createRpViewer(Type.Percentage, spt, rp);
        } else if (value instanceof Integer)
        {
            rpv = createRpViewer(Type.Meter, spt, rp);
        } else
        {
            rpv = createRpViewer(Type.String, spt, rp);
        }
        return rpv;
    }

}
