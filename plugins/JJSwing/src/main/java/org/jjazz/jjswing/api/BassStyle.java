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

import com.google.common.collect.ImmutableBiMap;
import java.util.Set;
import java.util.logging.Logger;
import org.jjazz.jjswing.bass.EndingPhraseBuilder;
import org.jjazz.jjswing.bass.TwoFeelPhraseBuilder;
import org.jjazz.jjswing.bass.WalkingDoubleTimePhraseBuilder;
import org.jjazz.jjswing.bass.WalkingPhraseBuilder;
import org.jjazz.jjswing.bass.PhraseBuilder;
import org.jjazz.jjswing.bass.WalkingDoubleNotePhraseBuilder;
import org.jjazz.phrase.api.Phrase;

/**
 * The possible bass styles of a WbpSource.
 * <p>
 * X_CUSTOM styles are used for dynamically created WbpSources when database could not provide the relevant WbpSource.
 */
public enum BassStyle
{
    INTRO,
    ENDING,
    TWO_FEEL,
    TWO_FEEL_CUSTOM,
    WALKING,
    WALKING_CUSTOM,
    WALKING_DOUBLE_NOTE,
    WALKING_DOUBLE_NOTE_CUSTOM,
    WALKING_DOUBLE_TIME,
    WALKING_DOUBLE_TIME_CUSTOM;

    private static final ImmutableBiMap<BassStyle, BassStyle> BIMAP_STYLE_CUSTOM = ImmutableBiMap.of(
            TWO_FEEL, TWO_FEEL_CUSTOM,
            WALKING, WALKING_CUSTOM,
            WALKING_DOUBLE_NOTE, WALKING_DOUBLE_NOTE_CUSTOM,
            WALKING_DOUBLE_TIME, WALKING_DOUBLE_TIME_CUSTOM
    );
    private static final Logger LOGGER = Logger.getLogger(BassStyle.class.getSimpleName());

    /**
     * Get the factory associated to this style.
     *
     * @return
     */
    public PhraseBuilder getBassPhraseBuilder()
    {
        PhraseBuilder res;
        res = switch (this)
        {
            case INTRO ->
                (scsList, tempo) -> new Phrase(0);      // no bass note
            case ENDING ->
                new EndingPhraseBuilder();
            case TWO_FEEL ->
                new TwoFeelPhraseBuilder();
            case WALKING ->
                new WalkingPhraseBuilder();
            case WALKING_DOUBLE_NOTE ->
                new WalkingDoubleNotePhraseBuilder();
            case WALKING_DOUBLE_TIME ->
                new WalkingDoubleTimePhraseBuilder();
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
        return this == WALKING || this == WALKING_CUSTOM
                || this == WALKING_DOUBLE_NOTE || this == WALKING_DOUBLE_NOTE_CUSTOM
                || this == WALKING_DOUBLE_TIME || this == WALKING_DOUBLE_TIME_CUSTOM;
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
