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
package org.jjazz.cl_editor;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.cl_editor.spi.CL_EditorSettings;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
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
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

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
        return new Color(prefs.getInt(PROP_BACKGROUND_COLOR, GeneralUISettings.getInstance().getColor("background.white").getRGB()));
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

    // =====================================================================================
    // Upgrade Task
    // =====================================================================================
    @ServiceProvider(service = UpgradeTask.class)
    static public class RestoreSettingsTask implements UpgradeTask
    {

        @Override
        public void upgrade(String oldVersion)
        {
            UpgradeManager um = UpgradeManager.getInstance();
            
            // package codebase has changed from JJazzLab 3 to JJazzLab 4: org/jjazz/ui/cl_editor => org/jjazz/cl_editor
            if (oldVersion != null && oldVersion.length() > 0 && oldVersion.charAt(0) <= '3')
            {
                um.duplicateOldPreferences(prefs, "org/jjazz/ui/cl_editor.properties");
            } else
            {
                um.duplicateOldPreferences(prefs);
            }
            
        }

    }

}
