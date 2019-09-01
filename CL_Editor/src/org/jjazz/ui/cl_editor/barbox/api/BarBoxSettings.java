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
package org.jjazz.ui.cl_editor.barbox.api;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import javax.swing.border.TitledBorder;
import org.openide.util.Lookup;

public abstract class BarBoxSettings
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

    public static BarBoxSettings getDefault()
    {
        BarBoxSettings result = Lookup.getDefault().lookup(BarBoxSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);
        }
        return result;
    }

    abstract public void setBorderFont(Font font);

    /**
     * The font used to write the bar index.
     *
     * @return
     */
    abstract public Font getBorderFont();

    abstract public Color getBorderColor();

    abstract public void setBorderColor(Color color);

    abstract public Color getFocusedBorderColor();

    abstract public void setFocusedBorderColor(Color color);

    /**
     *
     * @param str
     * @return Can be null.
     */
    abstract public TitledBorder getTitledBorder(String str);

    abstract public TitledBorder getFocusedTitledBorder(String str);

    /**
     *
     * @param color If null restore the default value.
     */
    abstract public void setDefaultColor(Color color);

    abstract public Color getDefaultColor();

    /**
     *
     * @param color If null restore the default value.
     */
    abstract public void setSelectedColor(Color color);

    abstract public Color getSelectedColor();

    abstract public void setPastEndSelectedColor(Color color);

    abstract public Color getPastEndSelectedColor();

    abstract public void setPastEndColor(Color color);

    abstract public Color getPastEndColor();

    abstract public void setDisabledColor(Color color);

    abstract public Color getDisabledColor();

    abstract public void setDisabledPastEndColor(Color color);

    abstract public Color getDisabledPastEndColor();

    /**
     *
     * @param color If null restore the default value.
     */
    abstract public void setPlaybackColor(Color color);

    abstract public Color getPlaybackColor();

    abstract public void addPropertyChangeListener(PropertyChangeListener listener);

    abstract public void removePropertyChangeListener(PropertyChangeListener listener);
}
