/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.flatcomponents.api;

import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.event.SwingPropertyChangeSupport;
import org.openide.util.NbPreferences;

/**
 * Manage global settings for the UI components of this module.
 */
public class FlatComponentsGlobalSettings
{

    public static final String PROP_VALUE_CHANGE_WITH_MOUSE_WHEEL = "ChangeWithMouseWheel";
    
    private static FlatComponentsGlobalSettings INSTANCE;
    private final HashMap<WeakReference<JComponent>, MouseWheelListener> mouseWheelInstalledComponents = new HashMap<>();
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Preferences prefs = NbPreferences.forModule(FlatComponentsGlobalSettings.class);
    private static final Logger LOGGER = Logger.getLogger(FlatComponentsGlobalSettings.class.getSimpleName());

    static public FlatComponentsGlobalSettings getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new FlatComponentsGlobalSettings();
        }
        return INSTANCE;
    }

    private FlatComponentsGlobalSettings()
    {
    }

    /**
     * Install or uninstall a MouseWheelListener depending on isChangeValueWithMouseWheelEnabled() value.
     * <p>
     * This component keeps track of the passed components (via a WeakReference) so that MouseWheelListener can be installed or uninstalled if
     * setChangeValueWithMouseWheelEnabled() changes later.
     *
     * @param comp
     * @param compListener
     * @see #setChangeValueWithMouseWheelEnabled(boolean)
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

        // Use a WeakReference because comp might be garbage collected in the future       
        mouseWheelInstalledComponents.put(new WeakReference<>(comp), compListener);
    }

    /**
     * Users with trackpad or "touch motion" mouse like the Apple Magic mouse should set this to false.
     * <p>
     * Because these devices don't have a "unitary" scroll unit, using the mouse wheel to change value e.g. a JSpinner does not work well.<p>
     *
     * @param b
     * @see
     * @see #installChangeValueWithMouseWheelSupport(javax.swing.JComponent, java.awt.event.MouseWheelListener)
     */
    public void setChangeValueWithMouseWheelEnabled(boolean b)
    {
        boolean old = isChangeValueWithMouseWheelEnabled();
        if (b == old)
        {
            return;
        }

        updateMouseWheelInstalledComponents(b);

        prefs.putBoolean(PROP_VALUE_CHANGE_WITH_MOUSE_WHEEL, b);
        pcs.firePropertyChange(PROP_VALUE_CHANGE_WITH_MOUSE_WHEEL, old, b);
    }

    public boolean isChangeValueWithMouseWheelEnabled()
    {
        return prefs.getBoolean(PROP_VALUE_CHANGE_WITH_MOUSE_WHEEL, true);
    }


    public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    //=============================================================================
    // Private methods
    //=============================================================================
    /**
     * add/remove MouseWheelListener for all installed components depending on isEnabled.
     *
     * @param isEnabled
     */
    private void updateMouseWheelInstalledComponents(boolean isEnabled)
    {
        for (var it = mouseWheelInstalledComponents.keySet().iterator(); it.hasNext();)
        {
            var compWeakRef = it.next();
            JComponent jc = compWeakRef.get();
            if (jc == null)
            {
                // Component has been garbage-collected, remove it
                it.remove();
                continue;
            }
            var listener = mouseWheelInstalledComponents.get(compWeakRef);
            if (isEnabled)
            {
                jc.addMouseWheelListener(listener);
            } else
            {
                jc.removeMouseWheelListener(listener);
            }
        }
    }
}
