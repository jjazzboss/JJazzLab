/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.jjswing.api;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;

/**
 * The possible drums styles
 * <p>
 */
public enum DrumsStyle
{
    BRUSHES_1("CoolJazzBallad.S738.prs", "Main B-1"),
    BRUSHES_2("MediumJazz.S737.sst", "Main A-1"),
    BRUSHES_3("EasyAcJazz.T157.STY", "Main A-1"),
    BRUSHES_4("JazzClub.S120.prs", "Main A-1"),
    BRUSHES_5("JazzClub.S120.prs", "Main B-1"),
    HI_HAT_1("DrawbarSwing.T153.sst", "Main A-1"),
    HI_HAT_2("Swing1.S737.bcs", "Main A-1"),
    HI_HAT_3("Swing1.S737.bcs", "Main B-1"),
    RIDE_1("MediumJazz.S737.sst", "Main B-1"),
    RIDE_2("MediumJazz.S737.sst", "Main C-1"),
    RIDE_3("MediumJazz.S737.sst", "Main D-1"),
    RIDE_4("LACoolSwing.STY", "Main B-1"),
    RIDE_5("CoolJazzBallad.S738.prs", "Main D-1"),
    RIDE_6("LACoolSwing.STY", "Main D-1"),
    SHUFFLE_1("CountryShuffle.S477.bcs", "Main B-1"),
    SHUFFLE_2("LACoolSwing.STY", "Main D-1");


    private final String rhythmId;
    private final String variationId;
    private static final Logger LOGGER = Logger.getLogger(DrumsStyle.class.getSimpleName());

    DrumsStyle(String rId, String varId)
    {
        this.rhythmId = rId;
        this.variationId = varId;
    }

    public String getRhythmId()
    {
        return rhythmId;
    }

    public Rhythm getRhythm() throws UnavailableRhythmException
    {
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        Rhythm res = rdb.getRhythmInstance(rhythmId+"-ID");
        return res;
    }

    public String getVariationId()
    {
        return variationId;
    }

    /**
     * Get all rhythm instances used by the possible DrumsStyles.
     *
     * @return
     */
    static public Set<Rhythm> getAllRhythms()
    {
        Set<Rhythm> res = new HashSet<>();
        for (var ds : DrumsStyle.values())
        {
            try
            {
                res.add(ds.getRhythm());
            } catch (UnavailableRhythmException ex)
            {
                LOGGER.log(Level.SEVERE, "getAllRhythms() Could not retrieve rhythm instance for rId={0}. ex={1}", new Object[]
                {
                    ds.rhythmId, ex.getMessage()
                });
            }
        }
        return res;
    }

    //===============================================================================================
    // Inner classes
    //===============================================================================================
}
