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
package org.jjazz.ui.itemrenderer;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.ui.colorsetmanager.api.ColorSetManager;
import org.jjazz.ui.itemrenderer.api.ItemRendererSettings;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ItemRendererSettings.class)
public class ItemRendererSettingsImpl implements ItemRendererSettings
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
        return new Color(prefs.getInt(PROP_ITEM_SELECTED_COLOR, ColorSetManager.getDefault().getSelectedBackgroundColor().getRGB()));
    }

    @Override
    public void setSelectedBackgroundColor(Color color)
    {
        Color old = getSelectedBackgroundColor();
        prefs.putInt(PROP_ITEM_SELECTED_COLOR, color.getRGB());
        pcs.firePropertyChange(PROP_ITEM_SELECTED_COLOR, old, color);
    }

    @Override
    public void setFocusedBorderColor(Color color)
    {
        prefs.putInt(PROP_ITEM_FOCUSED_BORDER_COLOR, color.getRGB());
        pcs.firePropertyChange(PROP_ITEM_FOCUSED_BORDER_COLOR, null, color);
    }

    @Override
    public Border getFocusedBorder()
    {
        Color color = new Color(prefs.getInt(PROP_ITEM_FOCUSED_BORDER_COLOR, ColorSetManager.getDefault().getFocusedBorderColor().getRGB()));
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

}
