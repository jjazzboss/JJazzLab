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
package org.jjazz.rhythm.api.rhythmparameters;

import org.jjazz.rhythm.api.RP_Integer;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.utilities.api.ResUtil;

/**
 * Standard RhythmParameter: Tempo percentage change.
 * <p>
 * Default 100%, min 50%, max 200%
 * .<p>
 * This RP is not primary and can't be customized: it's a shared instance.
 */
public final class RP_SYS_TempoFactor extends RP_Integer
{

    private static RP_SYS_TempoFactor INSTANCE;
    public static String ID = "rpTempoID";

    public static RP_SYS_TempoFactor getInstance()
    {
        synchronized (RP_SYS_TempoFactor.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RP_SYS_TempoFactor();
            }
        }
        return INSTANCE;
    }

    private RP_SYS_TempoFactor()
    {
        super(ID, ResUtil.getString(RP_SYS_TempoFactor.class, "CTL_RpTempoFactorName"), ResUtil.getString(RP_SYS_TempoFactor.class, "CTL_RpTempoFactorDesc"), false, 100, 50, 200, 1);
    }

    /**
     * Get the RP_SYS_TempoFactor instance if the specified rhythm uses it.
     *
     * @param r
     * @return Can be null if not found
     */
    static public RP_SYS_TempoFactor getTempoFactorRp(Rhythm r)
    {
        if (r == null)
        {
            throw new NullPointerException("r");   
        }
        return r.getRhythmParameters().contains(INSTANCE) ? INSTANCE : null;
    }

    @Override
    public String getDisplayValue(Integer value)
    {
        return value + "%";
    }
}
