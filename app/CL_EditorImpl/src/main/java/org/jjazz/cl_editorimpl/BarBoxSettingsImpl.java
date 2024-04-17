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
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.cl_editor.spi.BarBoxSettings;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.jjazz.uiutilities.api.FontColorUserSettingsProvider;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.utilities.api.ResUtil;

@ServiceProviders(value =
{
    @ServiceProvider(service = BarBoxSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
}
)
public class BarBoxSettingsImpl implements BarBoxSettings, FontColorUserSettingsProvider
{

    /**
     * The Preferences of this object.
     */
    private static final Preferences prefs = NbPreferences.forModule(BarBoxSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    ArrayList<FontColorUserSettingsProvider.FCSetting> fcSettings = new ArrayList<>();

    public BarBoxSettingsImpl()
    {
        // The settings we want to expose for user modifications
        FontColorUserSettingsProvider.FCSetting fcs = new FontColorUserSettingsProvider.FCSettingAdapter("BarId", ResUtil.getString(getClass(), "CTL_Bar"))
        {
            @Override
            public void setColor(Color c)
            {
                setDefaultColor(c);
            }

            @Override
            public Color getColor()
            {
                return getDefaultColor();
            }
        };
        fcSettings.add(fcs);
        fcs = new FontColorUserSettingsProvider.FCSettingAdapter("SelectedBarId", ResUtil.getString(getClass(), "CTL_SelectedBar"))
        {
            @Override
            public void setColor(Color c)
            {
                setSelectedColor(c);
            }

            @Override
            public Color getColor()
            {
                return getSelectedColor();
            }
        };
        fcSettings.add(fcs);
        fcs = new FontColorUserSettingsProvider.FCSettingAdapter("PlaybackBarId", ResUtil.getString(getClass(), "CTL_PlayedBar"))
        {
            @Override
            public void setColor(Color c)
            {
                setPlaybackColor(c);
            }

            @Override
            public Color getColor()
            {
                return getPlaybackColor();
            }
        };
        fcSettings.add(fcs);
    }

    @Override
    public void setBorderFont(Font font)
    {
        Font old = getBorderFont();
        if (font == null)
        {
            prefs.remove(PROP_BORDER_FONT);
            font = getBorderFont();
        } else
        {
            prefs.put(PROP_BORDER_FONT, Utilities.fontAsString(font));
        }
        pcs.firePropertyChange(PROP_BORDER_FONT, old, font);
    }

    @Override
    public Font getBorderFont()
    {
        String strFont = prefs.get(PROP_BORDER_FONT, "Arial-BOLD-8");
        return Font.decode(strFont);
    }

    @Override
    public void setBorderColor(Color color)
    {
        Color old = getBorderColor();
        if (color == null)
        {
            prefs.remove(PROP_BORDER_COLOR);
            color = getBorderColor();
        } else
        {
            prefs.putInt(PROP_BORDER_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BORDER_COLOR, old, color);
    }

    @Override
    public Color getBorderColor()
    {
        return new Color(prefs.getInt(PROP_BORDER_COLOR, Color.LIGHT_GRAY.darker().getRGB()));
    }

    @Override
    public void setFocusedBorderColor(Color color)
    {
        Color old = getFocusedBorderColor();
        if (color == null)
        {
            prefs.remove(PROP_FOCUSED_BORDER_COLOR);
            color = getFocusedBorderColor();
        } else
        {
            prefs.putInt(PROP_FOCUSED_BORDER_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_FOCUSED_BORDER_COLOR, old, color);
    }

    @Override
    public Color getFocusedBorderColor()
    {
        return new Color(prefs.getInt(PROP_FOCUSED_BORDER_COLOR, GeneralUISettings.getInstance().getColor("default.focused.border.color").getRGB()));
    }

    @Override
    public TitledBorder getTitledBorder(String str)
    {
        Border lb = BorderFactory.createLineBorder(getBorderColor(), 1);
        TitledBorder bb = new TitledBorder(lb, str, TitledBorder.RIGHT, TitledBorder.TOP, getBorderFont(), getBorderColor());
        return bb;
    }

    @Override
    public TitledBorder getFocusedTitledBorder(String str)
    {
        Border lb = BorderFactory.createLineBorder(getFocusedBorderColor(), 1);
        TitledBorder bb = new TitledBorder(lb, str, TitledBorder.RIGHT, TitledBorder.TOP, getBorderFont(), getFocusedBorderColor());
        return bb;
    }

    @Override
    public void setDefaultColor(Color color)
    {
        Color old = getDefaultColor();
        if (color == null)
        {
            prefs.remove(PROP_BAR_DEFAULT_COLOR);
            color = getDefaultColor();
        } else
        {
            prefs.putInt(PROP_BAR_DEFAULT_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BAR_DEFAULT_COLOR, old, color);
    }

    @Override
    public Color getDefaultColor()
    {
        return new Color(prefs.getInt(PROP_BAR_DEFAULT_COLOR, GeneralUISettings.getInstance().getColor("background.white").getRGB()));
    }

    @Override
    public void setPastEndColor(Color color)
    {
        Color old = getPastEndColor();
        if (color == null)
        {
            prefs.remove(PROP_BAR_PAST_END_COLOR);
            color = getPastEndColor();
        } else
        {
            prefs.putInt(PROP_BAR_PAST_END_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BAR_PAST_END_COLOR, old, color);
    }

    @Override
    public Color getPastEndColor()
    {
        return new Color(prefs.getInt(PROP_BAR_PAST_END_COLOR, Color.LIGHT_GRAY.getRGB()));
    }

    @Override
    public void setDisabledColor(Color color)
    {
        Color old = getDisabledColor();
        if (color == null)
        {
            prefs.remove(PROP_BAR_DISABLED_COLOR);
            color = getDisabledColor();
        } else
        {
            prefs.putInt(PROP_BAR_DISABLED_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BAR_DISABLED_COLOR, old, color);
    }

    @Override
    public Color getDisabledColor()
    {
        return new Color(prefs.getInt(PROP_BAR_DISABLED_COLOR, Color.LIGHT_GRAY.brighter().getRGB()));
    }

    @Override
    public void setDisabledPastEndColor(Color color)
    {
        Color old = getDisabledPastEndColor();
        if (color == null)
        {
            prefs.remove(PROP_BAR_DISABLED_PAST_END_COLOR);
            color = getDisabledPastEndColor();
        } else
        {
            prefs.putInt(PROP_BAR_DISABLED_PAST_END_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BAR_DISABLED_PAST_END_COLOR, old, color);
    }

    @Override
    public Color getDisabledPastEndColor()
    {
        return new Color(prefs.getInt(PROP_BAR_DISABLED_PAST_END_COLOR, Color.LIGHT_GRAY.brighter().getRGB()));
    }

    @Override
    public Color getSelectedColor()
    {
        return new Color(prefs.getInt(PROP_BAR_SELECTED_COLOR, GeneralUISettings.getInstance().getColor("bar.selected.background").getRGB()));
    }

    @Override
    public void setSelectedColor(Color color)
    {
        Color old = getSelectedColor();
        if (color == null)
        {
            prefs.remove(PROP_BAR_SELECTED_COLOR);
            color = getSelectedColor();
        } else
        {
            prefs.putInt(PROP_BAR_SELECTED_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BAR_SELECTED_COLOR, old, color);
    }

    @Override
    public Color getPastEndSelectedColor()
    {
        return new Color(prefs.getInt(PROP_BAR_PAST_END_SELECTED_COLOR, getSelectedColor().darker().getRGB()));
    }

    @Override
    public void setPastEndSelectedColor(Color color)
    {
        Color old = getPastEndSelectedColor();
        if (color == null)
        {
            prefs.remove(PROP_BAR_PAST_END_SELECTED_COLOR);
            color = getPastEndSelectedColor();
        } else
        {
            prefs.putInt(PROP_BAR_PAST_END_SELECTED_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BAR_PAST_END_SELECTED_COLOR, old, color);
    }

    @Override
    public Color getPlaybackColor()
    {
        return new Color(prefs.getInt(PROP_BAR_PLAYBACK_COLOR, new Color(244, 196, 155).getRGB()));
    }

    @Override
    public void setPlaybackColor(Color color)
    {
        Color old = getPlaybackColor();
        if (color == null)
        {
            prefs.remove(PROP_BAR_PLAYBACK_COLOR);
            color = getPlaybackColor();
        } else
        {
            prefs.putInt(PROP_BAR_PLAYBACK_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BAR_PLAYBACK_COLOR, old, color);
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
        return fcSettings;
    }


}
