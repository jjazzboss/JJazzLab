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
package org.jjazz.ss_editor.rpviewer.api;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import javax.swing.border.Border;
import org.jjazz.ss_editor.rpviewer.spi.StringRpRendererSettings;
import org.openide.util.Lookup;

public interface RpViewerSettings
{

    public static String PROP_NAME_FONT = "ViewerNameFont";
    public static String PROP_NAME_FONT_COLOR = "ViewerFontColor";
    public static String PROP_SELECTED_BACKGROUND_COLOR = "ViewerSelectedBackgroundColor";
    public static String PROP_BACKGROUND_COLOR = "ViewerDefaultBackgroundColor";
    public static String PROP_FOCUS_BORDER_COLOR = "ViewerFocusedBorderColor";
    public static String PROP_DEFAULT_BORDER_COLOR = "ViewerDefaultBorderColor";

    public static RpViewerSettings getDefault()
    {
        RpViewerSettings result = Lookup.getDefault().lookup(RpViewerSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);   
        }
        return result;
    }

    default StringRpRendererSettings getStringRpRendererSettings()
    {
        return StringRpRendererSettings.getDefault();
    }

    void setNameFont(Font font);

    Font getNameFont();

    void setNameFontColor(Color color);

    Color getNameFontColor();

    void setSelectedBackgroundColor(Color color);

    Color getSelectedBackgroundColor();

    void setDefaultBackgroundColor(Color color);

    Color getDefaultBackgroundColor();

    void setFocusedBorderColor(Color color);

    Color getFocusedBorderColor();

    Border getFocusedBorder();

    void setDefaultBorderColor(Color color);

    Color getDefaultBorderColor();

    Border getNonFocusedBorder();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
