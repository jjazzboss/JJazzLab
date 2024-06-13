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
package org.jjazz.ss_editor.sptviewer.spi;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import javax.swing.border.Border;
import org.jjazz.ss_editor.rpviewer.api.RpViewerSettings;
import org.openide.util.Lookup;

/**
 * The graphical settings for a SongPartEditor.
 *
 * @author Jerome
 */
public interface SptViewerSettings
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

    public static SptViewerSettings getDefault()
    {
        SptViewerSettings result = Lookup.getDefault().lookup(SptViewerSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);   
        }
        return result;
    }

    default RpViewerSettings getRpViewerSettings()
    {
        return RpViewerSettings.getDefault();
    }

    void setNameFont(Font font);

    Font getNameFont();

    void setNameFontColor(Color color);

    Color getNameFontColor();

    void setRhythmFont(Font font);

    Font getRhythmFont();

    void setRhythmFontColor(Color color);

    Color getRhythmFontColor();

    void setParentSectionFont(Font font);

    Font getParentSectionFont();

    void setParentSectionFontColor(Color color);

    Color getParentSectionFontColor();

    Color getFocusedBorderColor();

    void setFocusedBorderColor(Color color);

    void setDefaultBackgroundColor(Color color);

    Color getDefaultBackgroundColor();
  
    void setSelectedBackgroundColor(Color color);

    Color getSelectedBackgroundColor();

    Border getFocusedBorder();

    Border getDefaultBorder();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

}
