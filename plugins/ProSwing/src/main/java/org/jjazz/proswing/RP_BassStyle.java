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
package org.jjazz.proswing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import java.util.Objects;
import org.jjazz.rhythm.api.RP_State;
import org.jjazz.rhythm.api.Rhythm;

/**
 * RhythmParameter to specify the bass style.
 */
public class RP_BassStyle extends RP_State
{
    
    public static final String AUTO_MODE_VALUE = "auto";
    public static final ImmutableBiMap<BassStyle, String> BIMAP_STYLE_RPVALUE = ImmutableBiMap.of(
            BassStyle.BASIC, "basic",
            BassStyle.TWO_FEEL_A, "2-feel A",
            BassStyle.TWO_FEEL_B, "2-feel B",
            BassStyle.WALKING, "walking",
            BassStyle.CUSTOM, "custom"
    );
    
    public RP_BassStyle(boolean isPrimary)
    {
        super("BassStyleId", "Bass style", "Adjust bass style", isPrimary, AUTO_MODE_VALUE,
                AUTO_MODE_VALUE, toRpValue(BassStyle.BASIC), toRpValue(BassStyle.TWO_FEEL_A), toRpValue(BassStyle.TWO_FEEL_B), toRpValue(BassStyle.WALKING));
    }

    /**
     * Get the rpValue corresponding to bassStyle.
     *
     * @param bassStyle
     * @return
     */
    static public String toRpValue(BassStyle bassStyle)
    {
        Objects.requireNonNull(bassStyle);
        return BIMAP_STYLE_RPVALUE.get(bassStyle);
    }

    /**
     * Return the BassStyle corresponding to rpValue.
     *
     * @param rpValue Can not be AUTO_MODE_VALUE
     * @return
     */
    static public BassStyle toBassStyle(String rpValue)
    {
        Objects.requireNonNull(rpValue);
        Preconditions.checkArgument(!rpValue.equals(AUTO_MODE_VALUE));
        return BIMAP_STYLE_RPVALUE.inverse().get(rpValue);
    }

    /**
     * Get the RP_BassStyle rpValue to be used for a given rpVariation value when in auto mode.
     *
     * @param rpVariationValue
     * @return
     */
    static public String getAutoModeRpValueFromVariation(String rpVariationValue)
    {
        Objects.requireNonNull(rpVariationValue);
        BassStyle style = switch (rpVariationValue)
        {
            case "Main A-1", "Main A-2" ->
                BassStyle.TWO_FEEL_A;
            case "Main B-1", "Main B-2" ->
                BassStyle.TWO_FEEL_B;
            case "Main C-1", "Main C-2" ->
                BassStyle.WALKING;
            case "Main D-1", "Main D-2" ->
                BassStyle.WALKING;
            case "Intro A", "Intro B", "Intro C", "Ending A", "Ending B", "Ending C", "Fill In AA","Fill In BB","Fill In CC","Fill In DD","Fill In BA" ->
                BassStyle.WALKING;
            default -> throw new IllegalArgumentException("rpVariationValue=" + rpVariationValue);
        };
        return toRpValue(style);
    }
    
    static public RP_BassStyle get(Rhythm r)
    {
        return r.getRhythmParameters().stream()
                .filter(rp -> rp instanceof RP_BassStyle)
                .map(rp -> (RP_BassStyle) rp)
                .findFirst()
                .orElse(null);
    }
    
}
