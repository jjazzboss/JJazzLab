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
package org.jjazz.ui.sptviewer.api;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import javax.swing.border.Border;
import org.openide.util.Lookup;

/**
 * The graphical settings for a SongPartEditor.
 *
 * @author Jerome
 */
public abstract class SptViewerSettings
{

    public static String PROP_NAME_FONT = "NameFont";
    public static String PROP_NAME_FONT_COLOR = "NameFontColor";
    public static String PROP_RHYTHM_FONT = "RhythmFont";
    public static String PROP_RHYTHM_FONT_COLOR = "RhythmFontColor";
    public static String PROP_PARENTSECTION_FONT = "ParentSectionFont";
    public static String PROP_PARENTSECTION_FONT_COLOR = "ParentSectionFontColor";
    public static String PROP_FOCUSED_BORDER_COLOR = "FocusedBorderColor";
    public static String PROP_DEFAULT_BACKGROUND_COLOR = "DefaultBackgroundColor";
    public static String PROP_SELECTED_BACKGROUND_COLOR = "SelectedBackgroundColor";
    public static String PROP_PLAYBACK_COLOR = "PlaybackColor";

    public static SptViewerSettings getDefault()
    {
        SptViewerSettings result = Lookup.getDefault().lookup(SptViewerSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);
        }
        return result;
    }

    abstract public void setNameFont(Font font);

    abstract public Font getNameFont();

    abstract public void setNameFontColor(Color color);

    abstract public Color getNameFontColor();

    abstract public void setRhythmFont(Font font);

    abstract public Font getRhythmFont();

    abstract public void setRhythmFontColor(Color color);

    abstract public Color getRhythmFontColor();

    abstract public void setParentSectionFont(Font font);

    abstract public Font getParentSectionFont();

    abstract public void setParentSectionFontColor(Color color);

    abstract public Color getParentSectionFontColor();

    abstract public Color getFocusedBorderColor();

    abstract public void setFocusedBorderColor(Color color);

    abstract public void setDefaultBackgroundColor(Color color);

    abstract public Color getDefaultBackgroundColor();

    abstract public void setSelectedBackgroundColor(Color color);

    abstract public Color getSelectedBackgroundColor();

    abstract public void setPlaybackColor(Color color);

    abstract public Color getPlaybackColor();

    abstract public Border getFocusedBorder();

    abstract public Border getDefaultBorder();

    abstract public void addPropertyChangeListener(PropertyChangeListener listener);

    abstract public void removePropertyChangeListener(PropertyChangeListener listener);

}
