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
package org.jjazz.spteditor.api;

import java.awt.Font;
import java.beans.PropertyChangeListener;
import org.openide.util.Lookup;

public abstract class SptEditorSettings
{

    public static String PROP_NAME_FONT = "NameFont";
    public static String PROP_RHYTHM_FONT = "RhythmFont";

    public static SptEditorSettings getDefault()
    {
        SptEditorSettings result = Lookup.getDefault().lookup(SptEditorSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);   
        }
        return result;
    }

    abstract public void setNameFont(Font font);

    abstract public Font getNameFont();

    abstract public void setRhythmFont(Font font);

    abstract public Font getRhythmFont();

    abstract public void addPropertyChangeListener(PropertyChangeListener listener);

    abstract public void removePropertyChangeListener(PropertyChangeListener listener);
}
