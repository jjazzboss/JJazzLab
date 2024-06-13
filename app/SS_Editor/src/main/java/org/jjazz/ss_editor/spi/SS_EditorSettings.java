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
package org.jjazz.ss_editor.spi;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import org.openide.util.Lookup;

public interface SS_EditorSettings
{

    public static String PROP_BACKGROUND_COLOR = "BackgroundColor";
    public static String PROP_TOP_BACKGROUND_COLOR = "TopBackgroundColor";

    public static SS_EditorSettings getDefault()
    {
        SS_EditorSettings result = Lookup.getDefault().lookup(SS_EditorSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);   
        }
        return result;
    }

    Color getBackgroundColor();

    void setBackgroundColor(Color color);

    Color getTopBackgroundColor();

    void setTopBackgroundColor(Color color);

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
