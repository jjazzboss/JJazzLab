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
package org.jjazz.cl_editor.itemrenderer.api;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import org.openide.util.Lookup;

public interface IR_ChordSymbolSettings
{

    public static String PROP_FONT = "ItemFont";
    public static String PROP_DEFAULT_FONT_COLOR = "ItemFontColor";
    public static String PROP_SUBSTITUTE_FONT_COLOR = "ItemSubstituteFontColor";

    public static IR_ChordSymbolSettings getDefault()
    {
        IR_ChordSymbolSettings result = Lookup.getDefault().lookup(IR_ChordSymbolSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);
        }
        return result;
    }

    /**
     *
     * @param font If null restore the default value.
     */
    void setFont(Font font);

    /**
     * The font used to represent a chord symbol e.g. "Cm7".
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
     * Default color of a chord symbol.
     *
     * @return
     */
    Color getColor();
    /**
     *
     * @param color If null restore the default value.
     */
    void setSubstituteFontColor(Color color);

    /**
     * Chord symbol color when it has a chord substitute defined.
     *
     * @return
     */
    Color getSubstituteFontColor();

    /**
     * The font to display musical symbols like sharp and flat symbols.
     *
     * @return
     */
    Font getMusicFont();

    /**
     * @return The char representing the sharp symbol in the music font.
     */
    char getSharpCharInMusicFont();

    /**
     * @return The char representing the flat symbol in the music font.
     */
    char getFlatCharInMusicFont();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
