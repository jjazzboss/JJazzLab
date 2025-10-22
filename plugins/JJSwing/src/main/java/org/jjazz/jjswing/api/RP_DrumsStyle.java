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

import java.util.Objects;
import org.jjazz.rhythm.api.RP_State;
import org.jjazz.rhythm.api.Rhythm;

/**
 * RhythmParameter to specify the drums style.
 */
public class RP_DrumsStyle extends RP_State
{

    public static final String AUTO_MODE_VALUE = "auto";

    public RP_DrumsStyle(boolean isPrimary)
    {
        super("DrumsStyleId", "Drums style", "Adjust drums style", isPrimary, AUTO_MODE_VALUE,
                AUTO_MODE_VALUE,
                toRpValue(DrumsStyle.BRUSHES_1),
                toRpValue(DrumsStyle.BRUSHES_2),
                toRpValue(DrumsStyle.HI_HAT_1),
                toRpValue(DrumsStyle.HI_HAT_2),
                toRpValue(DrumsStyle.RIDE_1),
                toRpValue(DrumsStyle.RIDE_2),
                toRpValue(DrumsStyle.RIDE_3),
                toRpValue(DrumsStyle.RIDE_4),
                toRpValue(DrumsStyle.SHUFFLE_1),
                toRpValue(DrumsStyle.SHUFFLE_2),
                toRpValue(DrumsStyle.DOUBLE_1),
                toRpValue(DrumsStyle.INTRO),
                toRpValue(DrumsStyle.ENDING)
        );
    }

    /**
     * Get the rpValue corresponding to drumsStyle.
     *
     * @param drumsStyle
     * @return
     */
    static public String toRpValue(DrumsStyle drumsStyle)
    {
        Objects.requireNonNull(drumsStyle);
        return drumsStyle.name().toLowerCase().replace("_", " ");
    }

    /**
     * Return the DrumsStyle corresponding to rpValue.
     * <p>
     * Manage the AUTO_MODE_VALUE.
     *
     * @param rpValue
     * @param rpVariationValue Required if rpValue==AUTO_MODE_VALUE
     * @return
     */
    static public DrumsStyle toDrumsStyle(String rpValue, String rpVariationValue)
    {
        Objects.requireNonNull(rpValue);
        if (rpValue.equals(AUTO_MODE_VALUE))
        {
            rpValue = getAutoModeRpValueFromVariation(rpVariationValue);
        }
        String s = rpValue.toUpperCase().replace(" ", "_");

        return DrumsStyle.valueOf(s);
    }


    /**
     * Get the RP_DrumsStyle rpValue to be used for a given rpVariation value when in auto mode.
     *
     * @param rpVariationValue
     * @return
     */
    static public String getAutoModeRpValueFromVariation(String rpVariationValue)
    {
        Objects.requireNonNull(rpVariationValue);
        DrumsStyle style = switch (rpVariationValue)
        {
            case "Intro A-1" ->
                DrumsStyle.INTRO;
            case "Ending A-1" ->
                DrumsStyle.ENDING;
            case "Main A", "Main A-1" ->
                DrumsStyle.BRUSHES_1;
            case "Main A-2" ->
                DrumsStyle.BRUSHES_2;
            case "Main B", "Main B-1" ->
                DrumsStyle.HI_HAT_1;
            case "Main B-2" ->
                DrumsStyle.HI_HAT_2;
            case "Main C", "Main C-1" ->
                DrumsStyle.RIDE_1;
            case "Main C-2" ->
                DrumsStyle.RIDE_2;
            case "Main D", "Main D-1", "Main D-2" ->
                DrumsStyle.RIDE_3;
            case "Main E-1" ->
                DrumsStyle.RIDE_4;
            case "Main E-2" ->
                DrumsStyle.DOUBLE_1;
            default ->
                DrumsStyle.BRUSHES_2;
        };
        return toRpValue(style);
    }

    static public RP_DrumsStyle get(Rhythm r)
    {
        return r.getRhythmParameters().stream()
                .filter(rp -> rp instanceof RP_DrumsStyle)
                .map(rp -> (RP_DrumsStyle) rp)
                .findFirst()
                .orElse(null);
    }

    // ===========================================================================================================
    // Private methods
    // ===========================================================================================================

}
