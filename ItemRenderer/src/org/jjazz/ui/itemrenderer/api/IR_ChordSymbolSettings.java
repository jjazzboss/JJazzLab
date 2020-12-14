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
package org.jjazz.ui.itemrenderer.api;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.openide.util.Lookup;

public interface IR_ChordSymbolSettings
{

    public static String PROP_FONT = "ItemFont";
    public static String PROP_FONT_COLOR = "ItemFontColor";
    public static String PROP_FONT_ACCENT_COLOR = "ItemFontAccentColor";

    public static IR_ChordSymbolSettings getDefault()
    {
        IR_ChordSymbolSettings result = Lookup.getDefault().lookup(IR_ChordSymbolSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);   //NOI18N
        }
        return result;
    }

    /**
     *
     * @param font If null restore the default value.
     */
    void setFont(Font font);

    /**
     * The font used to represent the Chord Symbol e.g. "Cm7".
     *
     * @return
     */
    Font getFont();

    /**
     *
     * @param color If null restore the default value.
     */
    void setColor(Color color);

    /**
     * The color of the Chord Symbol.
     *
     * @return
     */
    Color getColor();

    /**
     * The color to be used when rendered object has an accented chord symbol.
     *
     * @param accentFeature
     * @param color If null restore the default value.
     */
    void setAccentColor(ChordRenderingInfo.Feature accentFeature, Color color);

    /**
     * The color to be used when rendered object has an accented chord symbol.
     *
     * @param accentFeature
     * @return
     */
    Color getAccentColor(ChordRenderingInfo.Feature accentFeature);

    /**
     * The font to display musical symbols like sharp and flat symbols.
     *
     * @return
     */
    Font getMusicFont();

    /**
     * @return The char representing the Sharp symbol in the music font.
     */
    char getSharpCharInMusicFont();

    /**
     * @return The char representing the Flat symbol in the music font.
     */
    char getFlatCharInMusicFont();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
