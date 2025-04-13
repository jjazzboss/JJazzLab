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

import com.google.common.collect.ImmutableBiMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jjazz.proswing.walkingbass.BassPhraseBuilder;
import org.jjazz.proswing.walkingbass.TwoFeelPhraseBuilder;
import org.jjazz.proswing.walkingbass.WalkingDoublePhraseBuilder;
import org.jjazz.proswing.walkingbass.WalkingPhraseBuilder;

/**
 * The possible bass styles of a WbpSource.
 * <p>
 * X_CUSTOM styles are used for dynamically created WbpSources when database could not provide the relevant WbpSource.
 */
public enum BassStyle
{
    TWO_FEEL,
    TWO_FEEL_CUSTOM,
    WALKING,
    WALKING_CUSTOM,
    WALKING_DOUBLE,
    WALKING_DOUBLE_CUSTOM;

    private static final ImmutableBiMap<BassStyle, BassStyle> BIMAP_STYLE_CUSTOM = ImmutableBiMap.of(
            TWO_FEEL, TWO_FEEL_CUSTOM,
            WALKING, WALKING_CUSTOM,
            WALKING_DOUBLE, WALKING_DOUBLE_CUSTOM
    );
    private static final Logger LOGGER = Logger.getLogger(BassStyle.class.getSimpleName());

    /**
     * Get the factory associated to this style.
     *
     * @return
     */
    public BassPhraseBuilder getBassPhraseBuilder()
    {
        BassPhraseBuilder res;
        res = switch (this)
        {
            case TWO_FEEL ->
                new TwoFeelPhraseBuilder();
            case WALKING ->
                new WalkingPhraseBuilder();
            case WALKING_DOUBLE ->
                new WalkingDoublePhraseBuilder();
            default -> throw new IllegalStateException("this=" + this);
        };
        return res;
    }

    public boolean is2feel()
    {
        return this == TWO_FEEL || this == TWO_FEEL_CUSTOM;
    }

    public boolean isWalking()
    {
        return this == WALKING || this == WALKING_CUSTOM || this == WALKING_DOUBLE || this == WALKING_DOUBLE_CUSTOM;
    }

    public boolean isCustom()
    {
        return BIMAP_STYLE_CUSTOM.containsValue(this);
    }

    static public Set<BassStyle> getNonCustomStyles()
    {
        return BIMAP_STYLE_CUSTOM.keySet();
    }

    public BassStyle getCustomStyle()
    {
        BassStyle res = BIMAP_STYLE_CUSTOM.get(this);
        if (res == null)
        {
            throw new IllegalArgumentException("this=" + this);
        }
        return res;
    }

    public BassStyle getNonCustomStyle()
    {
        BassStyle res = BIMAP_STYLE_CUSTOM.inverse().get(this);
        if (res == null)
        {
            throw new IllegalArgumentException("this=" + this);
        }
        return res;
    }


    //===============================================================================================
    // Inner classes
    //===============================================================================================
}
