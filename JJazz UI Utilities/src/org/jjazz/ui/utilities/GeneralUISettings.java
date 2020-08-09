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
package org.jjazz.ui.utilities;

import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.event.SwingPropertyChangeSupport;
import org.openide.util.NbPreferences;

/**
 * Store general UI settings
 */
public class GeneralUISettings
{

    public static final String PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL = "ChangeWithMouseWheel";
    private static GeneralUISettings INSTANCE;
    private static Preferences prefs = NbPreferences.forModule(GeneralUISettings.class);
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    static public GeneralUISettings getInstance()
    {
        synchronized (GeneralUISettings.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new GeneralUISettings();
            }
        }
        return INSTANCE;
    }

    private GeneralUISettings()
    {

    }

    /**
     * Users with trackpad or "touch motion" mouse like the Apple Magic mouse should set this to false.
     * <p>
     * Because these devices don't have a "unitary" scroll unit, therefore usually values change much too fast with these devices.
     *
     * @param b
     */
    public void setChangeValueWithMouseWheelEnabled(boolean b)
    {
        boolean old = isChangeValueWithMouseWheelEnabled();
        prefs.putBoolean(PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL, b);
        pcs.firePropertyChange(PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL, old, b);
    }

    public boolean isChangeValueWithMouseWheelEnabled()
    {
        return prefs.getBoolean(PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL, true);
    }

    /**
     * Helper method to register a component and its MouseWheelListener depending on the PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL value
     * changes.
     * <p>
     *
     * @param comp
     * @param compListener
     */
    public void installChangeValueWithMouseWheelSupport(JComponent comp, MouseWheelListener compListener)
    {
        if (isChangeValueWithMouseWheelEnabled())
        {
            comp.addMouseWheelListener(compListener);
        } else
        {
            comp.removeMouseWheelListener(compListener);
        }

        addPropertyChangeListener(e ->
        {
            if (e.getPropertyName().equals(PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL))
            {
                if (isChangeValueWithMouseWheelEnabled())
                {
                    comp.addMouseWheelListener(compListener);
                } else
                {
                    comp.removeMouseWheelListener(compListener);
                }
            }
        });
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

}
