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
package org.jjazz.songmemoviewer.api;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import org.openide.util.Lookup;

public interface SongMemoEditorSettings
{

    public static String PROP_FONT = "Font";
    public static String PROP_FONT_COLOR = "FontColor";
    public static String PROP_BACKGROUND_COLOR = "Background";

    public static SongMemoEditorSettings getDefault()
    {
        SongMemoEditorSettings result = Lookup.getDefault().lookup(SongMemoEditorSettings.class);
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


    Font getFont();

    /**
     *
     * @param color If null restore the default value.
     */
    void setFontColor(Color color);


    Color getFontColor();

    /**
     *
     * @param color If null restore the default value.
     */
    void setBackgroundColor(Color color);

    Color getBackgroundColor();   

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
