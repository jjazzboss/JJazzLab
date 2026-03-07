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
import java.beans.PropertyChangeListener;
import org.openide.util.Lookup;

public interface CL_EditorSettings
{

    public static String PROP_BACKGROUND_COLOR = "BackgroundColor";
    public static String PROP_SECTION_START_ON_NEW_LINE_EXTRA_HEIGHT = "SectionStartOnNewLineExtraHeight";

    public static CL_EditorSettings getDefault()
    {
        CL_EditorSettings result = Lookup.getDefault().lookup(CL_EditorSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);   
        }
        return result;
    }
    
    default BarBoxSettings getBarBoxSettings()
    {
        return BarBoxSettings.getDefault();
    }

    Color getBackgroundColor();

    void setBackgroundColor(Color color);

    /**
     * Get the extra height in pixels added above a bar row when the row starts with a section set to start on a new line.
     *
     * @return A value &gt;= 0
     */
    int getSectionStartOnNewLineExtraHeight();

    /**
     * Set the extra height in pixels added above a bar row when the row starts with a section set to start on a new line.
     *
     * @param height A value &gt;= 0
     */
    void setSectionStartOnNewLineExtraHeight(int height);

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
