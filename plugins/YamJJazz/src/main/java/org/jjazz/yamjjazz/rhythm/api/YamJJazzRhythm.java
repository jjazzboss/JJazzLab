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
package org.jjazz.yamjjazz.rhythm.api;

import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.spi.ConfigurableMusicGeneratorProvider;

/**
 * Our Rhythm interface extension.
 */
public interface YamJJazzRhythm extends Rhythm, ConfigurableMusicGeneratorProvider
{

    /**
     * Get the AccType corresponding to a RP_SYS_Mute value.
     *
     * @param rpMuteValue Can't be null
     * @return Can be null if rpMuteValue is an empty string.
     */
    AccType getAccType(String rpMuteValue);

    /**
     * Analyze the RP_Variation value produced by buildRhythmParameters() to retrieve the complexity level.
     *
     * @param rpValue eg "Main A-2"
     * @return eg 2, or -1 if no match.
     */
    int getComplexityLevel(String rpValue);

    /**
     * Get the RhythmVoice corresponding to specified AccType.
     *
     * @param at
     * @return Can be null if this AccType is not used in this rhythm.
     */
    default RhythmVoice getRhythmVoice(AccType at)
    {
        var res = getRhythmVoices().stream()
                .filter(rv -> AccType.getAccType(rv) == at)
                .findAny().orElse(null);
        return res;
    }

    /**
     * The Style object associated to this rhythm.
     *
     * @return
     */
    Style getStyle();

    /**
     * Analyze a RhythmParameter String value produced by buildRhythmParameters() to retrieve the corresponding StylePart.
     *
     * @param rpValue eg "Main A-2"
     * @return Main_A or null if no match
     */
    StylePart getStylePart(String rpValue);

    /**
     * True if it's an extended rhythm, false if it's a standard Yamaha rhythm.
     *
     * @return
     */
    boolean isExtendedRhythm();


}
