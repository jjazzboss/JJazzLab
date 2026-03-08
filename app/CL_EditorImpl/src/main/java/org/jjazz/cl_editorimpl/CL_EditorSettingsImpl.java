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
package org.jjazz.cl_editorimpl;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import java.beans.PropertyChangeSupport;
import org.jjazz.cl_editor.spi.CL_EditorSettings;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = CL_EditorSettings.class)
public class CL_EditorSettingsImpl implements CL_EditorSettings
{

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(CL_EditorSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    @Override
    public void setBackgroundColor(Color color)
    {
        Color old = getBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_BACKGROUND_COLOR);
            color = getBackgroundColor();
        } else
        {
            prefs.putInt(PROP_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Color getBackgroundColor()
    {
        return new Color(prefs.getInt(PROP_BACKGROUND_COLOR, Color.LIGHT_GRAY.getRGB()));
    }

    @Override
    public int getSectionStartOnNewLineExtraHeight()
    {
        return prefs.getInt(PROP_SECTION_START_ON_NEW_LINE_EXTRA_HEIGHT, 8);
    }

    @Override
    public void setSectionStartOnNewLineExtraHeight(int height)
    {
        int old = getSectionStartOnNewLineExtraHeight();
        if (height < 0)
        {
            prefs.remove(PROP_SECTION_START_ON_NEW_LINE_EXTRA_HEIGHT);
            height = getSectionStartOnNewLineExtraHeight();
        } else
        {
            prefs.putInt(PROP_SECTION_START_ON_NEW_LINE_EXTRA_HEIGHT, height);
        }
        pcs.firePropertyChange(PROP_SECTION_START_ON_NEW_LINE_EXTRA_HEIGHT, old, height);
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

}
