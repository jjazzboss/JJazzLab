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
package org.jjazz.uisettings;

import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.event.SwingPropertyChangeSupport;
import org.openide.util.*;

/**
 * Store general UI settings, manage current and available Themes.
 */
public class GeneralUISettings
{

    /**
     * The supported Look & Feels.
     */
    public enum LookAndFeel
    {
        LOOK_AND_FEEL_DEFAULT, LOOK_AND_FEEL_FLAT_DARK_LAF
    }

    public static final String PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL = "ChangeWithMouseWheel";
    public static final String PREF_THEME_UPON_RESTART = "ThemeUponRestart";
    private static GeneralUISettings INSTANCE;
    private Theme theme;
    private HashMap<WeakReference<JComponent>, MouseWheelListener> mouseWheelInstalledComponents = new HashMap<>();
    private static Preferences prefs = NbPreferences.forModule(GeneralUISettings.class);
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(GeneralUISettings.class.getSimpleName());

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

    public Theme getDefaultTheme()
    {
        Theme res = getTheme(DarkTheme.NAME);
        assert res != null;
        return res;
    }

    /**
     * Set the theme to be used on next application restart.
     *
     * @param themeName
     */
    public void setThemeUponRestart(String themeName)
    {
        if (getTheme(themeName) == null)
        {
            throw new IllegalArgumentException("Can't find a Theme with name=" + themeName);
        }
        prefs.put(PREF_THEME_UPON_RESTART, themeName);
    }

    /**
     * Get the theme name to be used on next application restart.
     *
     * @return
     */
    public String getThemeUponRestart()
    {
        return prefs.get(PREF_THEME_UPON_RESTART, getDefaultTheme().getName());
    }

    /**
     * Get the available themes names found in the global Lookup.
     *
     * @return
     */
    public List<String> getAvailableThemeNames()
    {
        var res = new ArrayList<>(Lookup.getDefault().lookupAll(Theme.class));
        return res.stream().map(t -> t.getName()).collect(Collectors.toList());
    }

    /**
     * Get the Theme with specified name.
     *
     * @param themeName
     * @return Null if not found.
     */
    public Theme getTheme(String themeName)
    {
        Theme res = null;
        for (Theme t : Lookup.getDefault().lookupAll(Theme.class))
        {
            if (t.getName().equals(themeName))
            {
                res = t;
                break;
            }
        }
        return res;
    }

    /**
     * Get the currently used theme.
     *
     * @return
     */
    public Theme getCurrentTheme()
    {
        return theme;
    }

    /**
     * Users with trackpad or "touch motion" mouse like the Apple Magic mouse should set this to false.
     * <p>
     * Because these devices don't have a "unitary" scroll unit, therefore usually values change much too fast with these devices.
     * Register/unregister all installed components via installChangeValueWithMouseWheelSupport().
     *
     * @param b
     */
    public void setChangeValueWithMouseWheelEnabled(boolean b)
    {
        boolean old = isChangeValueWithMouseWheelEnabled();
        if (b == old)
        {
            return;
        }


        updateMouseWheelInstalledComponents(b);


        prefs.putBoolean(PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL, b);
        pcs.firePropertyChange(PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL, old, b);
    }

    public boolean isChangeValueWithMouseWheelEnabled()
    {
        return prefs.getBoolean(PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL, true);
    }

    /**
     * Helper method to register/unregister a component and its MouseWheelListener depending on the
     * PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL value changes.
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

        // Use a WeakReference because comp might be garbage collected in the future       
        mouseWheelInstalledComponents.put(new WeakReference(comp), compListener);

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
