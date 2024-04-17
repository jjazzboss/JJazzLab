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
package org.jjazz.ss_editorimpl.rpviewer;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.ss_editor.rpviewer.api.RpViewerSettings;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerSettings;
import org.jjazz.uiutilities.api.FontColorUserSettingsProvider;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders(value =
{
    @ServiceProvider(service = RpViewerSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
}
)
public class RpViewerSettingsImpl implements RpViewerSettings, FontColorUserSettingsProvider, PropertyChangeListener
{

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(RpViewerSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(RpViewerSettingsImpl.class.getSimpleName());

    public RpViewerSettingsImpl()
    {
        // Listen to locale changes
        GeneralUISettings.getInstance().addPropertyChangeListener(this);
    }

    @Override
    public Color getSelectedBackgroundColor()
    {
        return new Color(prefs.getInt(PROP_SELECTED_BACKGROUND_COLOR, SptViewerSettings.getDefault().getSelectedBackgroundColor().getRGB()));
    }

    @Override
    public void setSelectedBackgroundColor(Color color)
    {
        Color old = getSelectedBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_SELECTED_BACKGROUND_COLOR);
            color = getSelectedBackgroundColor();
        } else
        {
            prefs.putInt(PROP_SELECTED_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_SELECTED_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Color getDefaultBackgroundColor()
    {
        return new Color(prefs.getInt(PROP_BACKGROUND_COLOR, GeneralUISettings.getInstance().getColor("background.white").getRGB()));
    }

    @Override
    public void setDefaultBackgroundColor(Color color)
    {
        Color old = getDefaultBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_BACKGROUND_COLOR);
            color = getDefaultBackgroundColor();
        } else
        {
            prefs.putInt(PROP_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Color getFocusedBorderColor()
    {
        return new Color(prefs.getInt(PROP_FOCUS_BORDER_COLOR, GeneralUISettings.getInstance().getColor("default.focused.border.color").getRGB()));
    }

    @Override
    public void setFocusedBorderColor(Color color)
    {
        Color old = getFocusedBorderColor();
        if (color == null)
        {
            prefs.remove(PROP_FOCUS_BORDER_COLOR);
            color = getFocusedBorderColor();
        } else
        {
            prefs.putInt(PROP_FOCUS_BORDER_COLOR, color.getRGB());
        };
        pcs.firePropertyChange(PROP_FOCUS_BORDER_COLOR, old, color);
    }

    @Override
    public Border getFocusedBorder()
    {
        return BorderFactory.createLineBorder(getFocusedBorderColor(), 1);
    }

    @Override
    public Color getDefaultBorderColor()
    {
        return new Color(prefs.getInt(PROP_DEFAULT_BORDER_COLOR, Color.GRAY.getRGB()));
    }

    @Override
    public void setDefaultBorderColor(Color color)
    {
        Color old = getDefaultBorderColor();
        if (color == null)
        {
            prefs.remove(PROP_DEFAULT_BORDER_COLOR);
            color = getDefaultBorderColor();
        } else
        {
            prefs.putInt(PROP_DEFAULT_BORDER_COLOR, color.getRGB());
        };
        pcs.firePropertyChange(PROP_DEFAULT_BORDER_COLOR, old, color);
    }

    @Override
    public Border getNonFocusedBorder()
    {
        return BorderFactory.createLineBorder(getDefaultBorderColor(), 1);
    }

    @Override
    public void setNameFont(Font font)
    {
        Font old = getNameFont();
        if (font == null)
        {
            prefs.remove(PROP_NAME_FONT);
            font = getNameFont();
        } else
        {
            String strFont = Utilities.fontAsString(font);
            prefs.put(PROP_NAME_FONT, strFont);
        }
        pcs.firePropertyChange(PROP_NAME_FONT, old, font);
    }

    @Override
    public Font getNameFont()
    {

        Font font = null;
        String strFont = prefs.get(PROP_NAME_FONT, null);
        if (strFont == null)
        {
            if (Utilities.isLatin(Locale.getDefault()))
            {
                font = GeneralUISettings.getInstance().getStdFont().deriveFont(9f);
            } else
            {
                // Must use default font for chinese etc.
                font = UIManager.getDefaults().getFont("Label.font");
                font = font.deriveFont(font.getSize2D() - 1);
            }
        } else
        {
            font = Font.decode(strFont);
        }

        assert font != null;
        return font;

    }

    @Override
    public Color getNameFontColor()
    {
        return new Color(prefs.getInt(PROP_NAME_FONT_COLOR, Color.DARK_GRAY.getRGB()));
    }

    @Override
    public void setNameFontColor(Color color)
    {
        Color old = getNameFontColor();
        if (color == null)
        {
            prefs.remove(PROP_NAME_FONT_COLOR);
            color = getNameFontColor();
        } else
        {
            prefs.putInt(PROP_NAME_FONT_COLOR, color.getRGB());
        };
        pcs.firePropertyChange(PROP_NAME_FONT_COLOR, old, color);
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
    // PropertyChangeListener implementation
    // =====================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == GeneralUISettings.getInstance())
        {
            if (evt.getPropertyName().equals(GeneralUISettings.PROP_LOCALE_UPON_RESTART))
            {
                // If we switch between latin and non latin, we must reset nameFont
                Locale oldLocale = (Locale) evt.getOldValue();
                Locale newLocale = (Locale) evt.getNewValue();
                if (Utilities.isLatin(oldLocale) != Utilities.isLatin(newLocale))
                {
                    setNameFont(null);      // Reset 
                }
            }
        }
    }

    // =====================================================================================
    // FontColorUserSettingsProvider implementation
    // =====================================================================================
    @Override
    public List<FontColorUserSettingsProvider.FCSetting> getFCSettings()
    {
        List<FontColorUserSettingsProvider.FCSetting> res = new ArrayList<>();


        FontColorUserSettingsProvider.FCSetting fcs = new FontColorUserSettingsProvider.FCSettingAdapter("rpNameId", "Rhythm parameter name")
        {
            @Override
            public Font getFont()
            {
                return RpViewerSettingsImpl.this.getNameFont();
            }

            @Override
            public void setFont(Font f)
            {
                RpViewerSettingsImpl.this.setNameFont(f);
            }

        };
        res.add(fcs);

        fcs = new FontColorUserSettingsProvider.FCSettingAdapter("rpBackgroundId", "Rhythm parameter background")
        {
            @Override
            public Color getColor()
            {
                return getDefaultBackgroundColor();
            }

            @Override
            public void setColor(Color c)
            {
                RpViewerSettingsImpl.this.setDefaultBackgroundColor(c);
            }

        };
        res.add(fcs);


        fcs = new FontColorUserSettingsProvider.FCSettingAdapter("rpSelectedBackgroundId", "Selected rhythm parameter background")
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
