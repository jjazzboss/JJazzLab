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
package org.jjazz.cl_editorimpl.itemrenderer;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererSettings;
import org.jjazz.uiutilities.api.FontColorUserSettingsProvider;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders(value =
{
    @ServiceProvider(service = ItemRendererSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
}
)
public class ItemRendererSettingsImpl implements ItemRendererSettings, FontColorUserSettingsProvider
{

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(ItemRendererSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    @Override
    public Color getSelectedBackgroundColor()
    {
        Color c;
        c = new Color(prefs.getInt(PROP_ITEM_SELECTED_COLOR, GeneralUISettings.getInstance().getColor("item.selected.background").getRGB()));
        return c;
    }

    @Override
    public void setSelectedBackgroundColor(Color color)
    {
        Color old = getSelectedBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_ITEM_SELECTED_COLOR);
            color = getSelectedBackgroundColor();
        } else
        {
            prefs.putInt(PROP_ITEM_SELECTED_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_ITEM_SELECTED_COLOR, old, color);
    }


    @Override
    public Border getFocusedBorder()
    {
        Color color = new Color(prefs.getInt(PROP_ITEM_FOCUSED_BORDER_COLOR, GeneralUISettings.getInstance().getColor("default.focused.border.color").getRGB()));
        return BorderFactory.createLineBorder(color);
    }

    @Override
    public Border getNonFocusedBorder()
    {
        return BorderFactory.createEmptyBorder(1, 1, 1, 1);
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
    // FontColorUserSettingsProvider implementation
    // =====================================================================================
    @Override
    public List<FontColorUserSettingsProvider.FCSetting> getFCSettings()
    {
        List<FontColorUserSettingsProvider.FCSetting> res = new ArrayList<>();
        FontColorUserSettingsProvider.FCSetting fcs = new FontColorUserSettingsProvider.FCSettingAdapter("SelectedId", "Selected item background")
        {
            @Override
            public Color getColor()
            {
                return getSelectedBackgroundColor();
            }

            @Override
            public void setColor(Color c)
            {
                setSelectedBackgroundColor(c);
            }
        };
        res.add(fcs);
        return res;
    }



}
