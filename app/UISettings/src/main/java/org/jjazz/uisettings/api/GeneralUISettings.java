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
package org.jjazz.uisettings.api;

import com.google.common.base.Preconditions;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.LifecycleManager;
import org.openide.NotifyDescriptor;
import org.openide.modules.Places;
import org.openide.util.*;

/**
 * Store general UI settings, manage the current and available Themes, the current and available Locales.
 */
public class GeneralUISettings
{

    /**
     * newValue=new locale
     */
    public static final String PROP_LOCALE_UPON_RESTART = "LocaleUponRestart";

    /**
     * The list of currently supported locales by the application.
     */
    public static final Locale[] SUPPORTED_LOCALES = new Locale[]
    {
        new Locale("en", "US"),
        new Locale("fr", "FR"),
        new Locale("de", "DE"),
        new Locale("zh", "CN"),
        new Locale("es", "ES"),
        new Locale("pt", "BR"),
        new Locale("ja", "JP")
    };

    protected enum LookAndFeelId
    {
        LOOK_AND_FEEL_FLAT_LIGHT_LAF("com.formdev.flatlaf.FlatLightLaf"), LOOK_AND_FEEL_FLAT_DARK_LAF("com.formdev.flatlaf.FlatDarkLaf");
        private final String path;

        LookAndFeelId(String path)
        {
            this.path = path;
        }

        String getPath()
        {
            return path;
        }
    }

    private static final Theme DEFAULT_THEME = new DarkTheme();
    // We don't use @ServiceProvider to get available themes because LookAndFeelInstaller needs Theme instances very early in the startup sequence, before global Lookup ServiceProviders are available.
    private static final List<Theme> AVAILABLE_THEMES = Arrays.asList(DEFAULT_THEME, new LightTheme()); // Must contain DEFAULT_THEME


    @StaticResource(relative = true)
    private static final String CONDENSED_FONT_PATH = "resources/RobotoCondensed-Regular.ttf";
    private static Font CONDENSED_FONT_10;
    @StaticResource(relative = true)
    private static final String FONT_PATH = "resources/Roboto-Regular.ttf";
    private static Font FONT_10;
    private static final String JJAZZLAB_CONFIG_FILE_NAME = "jjazzlab.conf";


    private static final String PREF_THEME_UPON_RESTART = "ThemeUponRestart";
    private static final String PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL = "ChangeWithMouseWheel";
    private static GeneralUISettings INSTANCE;
    private final Theme sessionTheme;
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
        String name = prefs.get(PREF_THEME_UPON_RESTART, null);
        sessionTheme = getTheme(name, DEFAULT_THEME);

        Analytics.setProperties(Analytics.buildMap("Theme", sessionTheme.getName()));
    }

    /**
     * Get the available themes.
     *
     * @return
     */
    static public List<Theme> getThemes()
    {
        return AVAILABLE_THEMES;
    }

    /**
     * Get the Theme with specified name.
     *
     * @param name
     * @param def
     * @return Return def if no theme matching name.
     */
    static public Theme getTheme(String name, Theme def)
    {
        Theme res = getThemes().stream().
                filter(t -> t.getName().equals(name))
                .findAny()
                .orElse(def);
        return res;
    }

    /**
     * Get the theme used for this session.
     *
     * @return Can't be null
     */
    public Theme getSessionTheme()
    {
        return sessionTheme;
    }

    /**
     * Get the theme name to be used on next application start.
     *
     * @return
     */
    public String getThemeNameUponRestart()
    {
        return prefs.get(PREF_THEME_UPON_RESTART, DEFAULT_THEME.getName());
    }

    /**
     * Set the theme to be used on next application start.
     * <p>
     * This will restart the application. User is asked to confirm unless silent is true.
     *
     * @param theme
     * @param silent
     */
    public void setThemeUponRestart(Theme theme, boolean silent)
    {
        Preconditions.checkArgument(getThemes().contains(theme), "theme=%s", theme.getName());

        if (theme == sessionTheme)
        {
            return;
        }

        if (!silent)
        {
            if (theme != DEFAULT_THEME)
            {
                String msg = ResUtil.getString(getClass(), "CTL_UseNonDefaultTheme", theme.getName());
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
            }

            String msg = ResUtil.getString(getClass(), "CTL_ConfirmRestartToChangeTheme");
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.OK_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(d);
            if (NotifyDescriptor.OK_OPTION != result)
            {
                return;
            }
        }


        prefs.put(PREF_THEME_UPON_RESTART, theme.getName());

        if (org.openide.util.Utilities.isWindows())
        {
            // For some reason does not work on Linux and Mac
            LifecycleManager.getDefault().markForRestart();
        }
        LifecycleManager.getDefault().exit();

    }

    /**
     * Set the locale to use upon next restart.
     * <p>
     * Add or replace the --locale code in the user conf file. <br>
     * <br>
     * Fire a PROP_LOCALE_UPON_RESTART change event.
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
            Path nbConfigFile = FileDirectoryManager.getInstance().getInstallationDirectory().toPath().resolve("etc").resolve(JJAZZLAB_CONFIG_FILE_NAME);
            if (!nbConfigFile.toFile().exists())
            {
                throw new IOException("Unexpected error: " + nbConfigFile + " file not found");
            }

            // Make the copy
            Files.copy(nbConfigFile, userConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.log(Level.INFO, "setLocaleUponRestart() Successfully created user .conf file: {0}", userConfigFile.getAbsolutePath());
        }


        // Add or replace the locale code
        String code = locale.getLanguage() + ":" + locale.getCountry();
        String content = new String(Files.readAllBytes(userConfigFile.toPath()), StandardCharsets.UTF_8);
        Pattern p1 = Pattern.compile("^\\s*default_options\\s*=.*--locale\\s+", Pattern.MULTILINE);
        Pattern p2 = Pattern.compile("^\\s*default_options\\s*=\\s*\"", Pattern.MULTILINE);

        if (p1.matcher(content).find())
        {
            // Replace the --locale xx:XX
            content = content.replaceFirst("(?m)(^\\s*default_options\\s*=.*--locale\\s+)([a-zA-Z:]+)(.*)", "$1" + code + "$3");
        } else if (p2.matcher(content).find())
        {
            // Add the --locale xx:XX
            code = "--locale " + code + " ";
            content = content.replaceFirst("(?m)(^\\s*default_options\\s*=\\s*\")(.*)", "$1" + code + "$2");
        } else
        {
            throw new IOException("Unexpected error: no 'default_options' property found in " + userConfigFile.getAbsolutePath());
        }
        Files.write(userConfigFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        LOGGER.log(Level.INFO, "setLocaleUponRestart() Set next locale upon restart={0}", code);

        pcs.firePropertyChange(PROP_LOCALE_UPON_RESTART, Locale.getDefault(), locale);
    }


    /**
     * Users with trackpad or "touch motion" mouse like the Apple Magic mouse should set this to false.
     * <p>
     * Because these devices don't have a "unitary" scroll unit, therefore usually values change much too fast with these devices. Register/unregister all
     * installed components via installChangeValueWithMouseWheelSupport().
     * <p>
     * Fire a PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL change event.
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
     * Helper method to register/unregister a component and its MouseWheelListener depending on the PREF_VALUE_CHANGE_WITH_MOUSE_WHEEL value changes.
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
     * Get the JJazzLab standard font (for latin locales only) with size=10pt and style=PLAIN.
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
                LOGGER.log(Level.SEVERE, "Can't get font from " + FONT_PATH + ". Using default font instead={0}", FONT_10);
            }
        }
        assert FONT_10 != null;
        return FONT_10;
    }

    /**
     * Get the JJazzLab standard condensed font (for latin locales only) with size=10pt and style=PLAIN.
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
                LOGGER.log(Level.SEVERE, "Can't get font from " + CONDENSED_FONT_PATH + ". Using default font instead={0}",
                        CONDENSED_FONT_10);
            }
        }
        assert CONDENSED_FONT_10 != null;
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

    public static final boolean isDarkTheme()
    {
        return getInstance().getSessionTheme().getName().equals(DarkTheme.NAME);
    }

    //=============================================================================
    // Helper methods to get default from the UIDefaults of the current theme.
    //=============================================================================  
    public Icon getIcon(String key)
    {
        // currentTheme might be null when using the Netbeans Matisse UI builder!!! This avoids a NullPointerException
        return sessionTheme == null ? null : sessionTheme.getUIDefaults().getIcon(key);
    }

    public Color getColor(String key)
    {
        return sessionTheme == null ? null : sessionTheme.getUIDefaults().getColor(key);
    }

    public Font getFont(String key)
    {
        return sessionTheme == null ? null : sessionTheme.getUIDefaults().getFont(key);
    }

    public boolean getBoolean(String key)
    {
        return sessionTheme == null ? null : sessionTheme.getUIDefaults().getBoolean(key);
    }

    public Border getBorder(String key)
    {
        return sessionTheme == null ? null : sessionTheme.getUIDefaults().getBorder(key);
    }

    public int getInt(String key)
    {
        return sessionTheme == null ? null : sessionTheme.getUIDefaults().getInt(key);
    }

    //=============================================================================
    // Inner classes
    //=============================================================================    

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
