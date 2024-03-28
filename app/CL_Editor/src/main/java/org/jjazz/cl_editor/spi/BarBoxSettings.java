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
package org.jjazz.cl_editor.spi;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import javax.swing.border.TitledBorder;
import org.openide.util.Lookup;

public interface BarBoxSettings
{

    public static String PROP_BORDER_FONT = "BorderFont";
    public static String PROP_BORDER_COLOR = "BorderColor";
    public static String PROP_FOCUSED_BORDER_COLOR = "FocusedBorderColor";
    public static String PROP_BAR_DEFAULT_COLOR = "BarDefaultColor";
    public static String PROP_BAR_SELECTED_COLOR = "BarSelectedColor";
    public static String PROP_BAR_PAST_END_SELECTED_COLOR = "BarPastEndSelectedPastColor";
    public static String PROP_BAR_PAST_END_COLOR = "BarPastEndColor";
    public static String PROP_BAR_DISABLED_COLOR = "BarDisabledColor";
    public static String PROP_BAR_PLAYBACK_COLOR = "BarPlaybackColor";
    public static String PROP_BAR_DISABLED_PAST_END_COLOR = "BarDisabledPastEndColor";

    static BarBoxSettings getDefault()
    {
        BarBoxSettings result = Lookup.getDefault().lookup(BarBoxSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);   
        }
        return result;
    }

    default BarRendererSettings getBarRendererSettings()
    {
        return BarRendererSettings.getDefault();
    }

    void setBorderFont(Font font);

    /**
     * The font used to write the bar index.
     *
     * @return
     */
    Font getBorderFont();

    Color getBorderColor();

    void setBorderColor(Color color);

    Color getFocusedBorderColor();

    void setFocusedBorderColor(Color color);

    /**
     *
     * @param str
     * @return Can be null.
     */
    TitledBorder getTitledBorder(String str);

    TitledBorder getFocusedTitledBorder(String str);

    /**
     * Set the default background color.
     *
     * @param color If null restore the default value.
     */
    void setDefaultColor(Color color);

    Color getDefaultColor();

    /**
     *
     * @param color If null restore the default value.
     */
    void setSelectedColor(Color color);

    Color getSelectedColor();

    void setPastEndSelectedColor(Color color);

    Color getPastEndSelectedColor();

    void setPastEndColor(Color color);

    Color getPastEndColor();

    void setDisabledColor(Color color);

    Color getDisabledColor();

    void setDisabledPastEndColor(Color color);

    Color getDisabledPastEndColor();

    /**
     *
     * @param color If null restore the default value.
     */
    void setPlaybackColor(Color color);

    Color getPlaybackColor();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
