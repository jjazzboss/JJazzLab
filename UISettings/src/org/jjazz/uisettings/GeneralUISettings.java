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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.modules.OnStart;
import org.openide.modules.Places;
import org.openide.util.*;

/**
 * Store general UI settings, manage the current and available Themes, the current and available Locales.
 */
public class GeneralUISettings
{

    /**
     * The list of currently supported locales by the application.
     */
    public static final Locale[] SUPPORTED_LOCALES = new Locale[]
    {
        new Locale("en", "US"),
        new Locale("fr", "FR"),
        new Locale("ja", "JP")
    };

    /**
     * The supported Look & Feels.
     */
    public enum LookAndFeelId
    {
        LOOK_AND_FEEL_SYSTEM_DEFAULT, LOOK_AND_FEEL_FLAT_DARK_LAF
    }
    @StaticResource(relative = true)
    private static final String CONDENSED_FONT_PATH = "resources/RobotoCondensed-Regular.ttf";
    private static Font CONDENSED_FONT_10;
    @StaticResource(relative = true)
    private static final String FONT_PATH = "resources/Roboto-Regular.ttf";
    private static Font FONT_10;
    private static final String JJAZZLAB_CONFIG_FILE_NAME = "jjazzlab.conf";

    public static final String DEFAULT_THEME_NAME = LightTheme.NAME;
    public static final LookAndFeelId DEFAULT_LAF_ID = LookAndFeelId.LOOK_AND_FEEL_SYSTEM_DEFAULT;  // Must be the laf of DEFAULT_THEME_NAME
    public static final String PREF_THEME_UPON_RESTART = "ThemeUponRestart";   //NOI18N 
    public static final String PREF_LAF_ID_UPON_RESTART = "LafIdUponRestart";   //NOI18N 
    public static final String PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL = "ChangeWithMouseWheel";   //NOI18N 
    private static GeneralUISettings INSTANCE;
    private Theme currentTheme;
    private final HashMap<WeakReference<JComponent>, MouseWheelListener> mouseWheelInstalledComponents = new HashMap<>();
    private static final Preferences prefs = NbPreferences.forModule(GeneralUISettings.class);
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
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

    /**
     * Set the theme to be used on next application start.
     *
     * @param theme
     */
    public void setThemeUponRestart(Theme theme)
    {
        if (theme == null)
        {
            throw new NullPointerException("theme");   //NOI18N
        }
        prefs.put(PREF_THEME_UPON_RESTART, theme.getName());
        prefs.put(PREF_LAF_ID_UPON_RESTART, theme.getLookAndFeel().name());
    }

    /**
     * Get the theme name to be used on next application start.
     *
     * @return
     */
    public String getThemeNameUponRestart()
    {
        return prefs.get(PREF_THEME_UPON_RESTART, DEFAULT_THEME_NAME);
    }

    /**
     * Set the locale to use upon next restart.
     * <p>
     * Replace the --locale code in the user conf file. Note: can't be used while running from IDE!
     *
     * @param locale
     * @throws java.io.IOException If a problem occured
     */
    public void setLocaleUponRestart(Locale locale) throws IOException
    {
        if (!Arrays.asList(SUPPORTED_LOCALES).contains(locale))
        {
            throw new IllegalArgumentException("Invalid locale=" + locale);
        }

         // Special case: if running from IDE, jjazzlab.conf file is not used
        if (System.getProperty("jjazzlab.version") == null)
        {
            throw new IOException("Can't change language when running JJazzLab from the IDE");
        }

        
        // Make sure <nbUserDir>/etc exists
        File nbUserDir = Places.getUserDirectory();
        assert nbUserDir != null && nbUserDir.isDirectory() : "nbUserDir=" + nbUserDir;
        File userEtcDir = new File(nbUserDir, "etc");
        if (!userEtcDir.exists())
        {   
            userEtcDir.mkdir();
        }
        
        
        // Get the user-defined conf file                        
        File userConfigFile = new File(userEtcDir, JJAZZLAB_CONFIG_FILE_NAME);
        if (!userConfigFile.exists())
        {
            // Not present, copy the default one to create it                        
            String strNBPlatformDir = System.getProperty("netbeans.home", null);
            if (strNBPlatformDir == null)
            {
                throw new IOException("Unexpected error: netbeans.home property value=null");
            }

            Path nbConfigFile = Path.of(strNBPlatformDir, "..", "etc", JJAZZLAB_CONFIG_FILE_NAME);
            if (!nbConfigFile.toFile().exists())
            {
                throw new IOException("Unexpected error: " + nbConfigFile + " file not found");
            }
            
            // Make the copy
            Files.copy(nbConfigFile, userConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("setLocaleUponRestart() Successfully created user .conf file: " + userConfigFile.getAbsolutePath());
        }


        // Replace the locale code
        String code = locale.getLanguage() + ":" + locale.getCountry();
        String content = new String(Files.readAllBytes(userConfigFile.toPath()), StandardCharsets.UTF_8);
        content = content.replaceFirst("(^\\s*default_options.*--locale\\s+)([a-zA-Z:]+)(.*)", "$1" + code + "$3");
        Files.write(userConfigFile.toPath(), content.getBytes(StandardCharsets.UTF_8));


        LOGGER.info("setLocaleUponRestart() Set next locale upon restart=" + code);
    }

    /**
     * Get the LAF to be used on next application start.
     *
     * @return
     */
    public LookAndFeelId getLafIdUponRestart()
    {
        LookAndFeelId res = DEFAULT_LAF_ID;
        String strLaf = prefs.get(PREF_LAF_ID_UPON_RESTART, DEFAULT_LAF_ID.name());
        try
        {
            res = LookAndFeelId.valueOf(strLaf);
        } catch (IllegalArgumentException | NullPointerException ex)
        {
            LOGGER.warning("getLafIdUponRestart() Invalid LAF name=" + strLaf + ". Using default LAF=" + res.name());   //NOI18N
        }
        return res;
    }

    /**
     * Get the available themes found in the global Lookup.
     *
     * @return
     */
    public List<Theme> getAvailableThemes()
    {
        return new ArrayList<>(Lookup.getDefault().lookupAll(Theme.class));
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
        for (Theme t : getAvailableThemes())
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
     * Get the theme used for this session.
     *
     * @return
     */
    public Theme getCurrentTheme()
    {
        return currentTheme;
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
        mouseWheelInstalledComponents.put(new WeakReference<>(comp), compListener);
    }

    /**
     * Get the JJazzLab standard font with size=10pt and style=PLAIN.
     *
     * @return
     */
    public Font getStdFont()
    {
        if (FONT_10 == null)
        {
            try (InputStream is = getClass().getResourceAsStream(FONT_PATH))
            {

                FONT_10 = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(10f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(FONT_10); // So it is available in getAvailableFontFamilyNames() etc.
            } catch (IOException | FontFormatException e)
            {
                FONT_10 = Font.getFont("Arial-PLAIN-10");
                LOGGER.log(Level.SEVERE, "Can't get font from " + FONT_PATH + ". Using default font instead=" + FONT_10);   //NOI18N
            }
        }
        assert FONT_10 != null;   //NOI18N
        return FONT_10;
    }

    /**
     * Get the JJazzLab standard condensed font with size=10pt and style=PLAIN.
     *
     * @return
     */
    public Font getStdCondensedFont()
    {
        if (CONDENSED_FONT_10 == null)
        {
            try (InputStream is = getClass().getResourceAsStream(CONDENSED_FONT_PATH))
            {

                CONDENSED_FONT_10 = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(10f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(CONDENSED_FONT_10); // So it is available in getAvailableFontFamilyNames() etc.
            } catch (IOException | FontFormatException e)
            {
                CONDENSED_FONT_10 = Font.getFont("Arial-PLAIN-10");
                LOGGER.log(Level.SEVERE, "Can't get font from " + CONDENSED_FONT_PATH + ". Using default font instead=" + CONDENSED_FONT_10);   //NOI18N
            }
        }
        assert CONDENSED_FONT_10 != null;   //NOI18N
        return CONDENSED_FONT_10;
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
    // Helper methods to get default from the UIDefaults of the current theme.
    //=============================================================================  
    public Icon getIcon(String key)
    {
        // currentTheme might be null when using the Netbeans Matisse UI builder!!! This avoids a NullPointerException
        return currentTheme == null ? null : currentTheme.getUIDefaults().getIcon(key);
    }

    public Color getColor(String key)
    {
        return currentTheme == null ? null : currentTheme.getUIDefaults().getColor(key);
    }

    public Font getFont(String key)
    {
        return currentTheme == null ? null : currentTheme.getUIDefaults().getFont(key);
    }

    public boolean getBoolean(String key)
    {
        return currentTheme == null ? null : currentTheme.getUIDefaults().getBoolean(key);
    }

    public Border getBorder(String key)
    {
        return currentTheme == null ? null : currentTheme.getUIDefaults().getBorder(key);
    }

    public int getInt(String key)
    {
        return currentTheme == null ? null : currentTheme.getUIDefaults().getInt(key);
    }

    //=============================================================================
    // Inner classes
    //=============================================================================    
    /**
     * Set the current theme from the previous session "theme upon restart".
     * <p>
     * At this stage Look & Feel has already been set up by LookAndFeelInstaller, and global Lookup ServiceProviders are
     * available.
     */
    @OnStart
    static public class ThemeSetup implements Runnable
    {

        @Override
        public void run()
        {
            getInstance().setCurrentTheme(prefs.get(PREF_THEME_UPON_RESTART, DEFAULT_THEME_NAME));
        }

    }

    //=============================================================================
    // Private methods
    //=============================================================================
    /**
     * Set the theme to be used for the current session.
     * <p>
     * Can be called only once.
     *
     * @param theme
     */
    private void setCurrentTheme(String themeName)
    {
        if (currentTheme != null)
        {
            throw new IllegalStateException("currentTheme is already set=" + currentTheme.getName() + ". themeName=" + themeName);   //NOI18N
        }
        currentTheme = getTheme(themeName);
        if (currentTheme == null)
        {
            currentTheme = getTheme(DEFAULT_THEME_NAME);
            assert currentTheme != null : "DEFAULT_THEME_NAME=" + DEFAULT_THEME_NAME;   //NOI18N
        }
    }

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
