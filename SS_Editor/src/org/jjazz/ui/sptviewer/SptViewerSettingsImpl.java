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
package org.jjazz.ui.sptviewer;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.ui.colorsetmanager.api.ColorSetManager;
import org.jjazz.ui.sptviewer.api.SptViewerSettings;
import org.jjazz.ui.utilities.FontColorUserSettingsProvider;
import org.jjazz.upgrade.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.jjazz.util.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Jerome
 */

@ServiceProviders(value =
{
    @ServiceProvider(service = SptViewerSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
}
)
public class SptViewerSettingsImpl implements SptViewerSettings, FontColorUserSettingsProvider
{

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(SptViewerSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    @Override
    public void setDefaultBackgroundColor(Color color)
    {
        Color old = getDefaultBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_DEFAULT_BACKGROUND_COLOR);
            color = getDefaultBackgroundColor();
        } else
        {
            prefs.putInt(PROP_DEFAULT_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_DEFAULT_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Color getDefaultBackgroundColor()
    {
        return new Color(prefs.getInt(PROP_DEFAULT_BACKGROUND_COLOR, ColorSetManager.getDefault().getWhite().getRGB()));
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
    public Color getSelectedBackgroundColor()
    {
        return new Color(prefs.getInt(PROP_SELECTED_BACKGROUND_COLOR, ColorSetManager.getDefault().getSelectedBackgroundColor().getRGB()));
    }

    @Override
    public Color getPlaybackColor()
    {
        return new Color(prefs.getInt(PROP_PLAYBACK_COLOR, new Color(244, 219, 215).getRGB()));
    }

    @Override
    public void setPlaybackColor(Color color)
    {
        Color old = getPlaybackColor();
        if (color == null)
        {
            prefs.remove(PROP_PLAYBACK_COLOR);
            color = getPlaybackColor();
        } else
        {
            prefs.putInt(PROP_PLAYBACK_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_PLAYBACK_COLOR, old, color);
    }

    @Override
    public Border getDefaultBorder()
    {
        return BorderFactory.createEmptyBorder(1, 1, 1, 1); // Need to be the same thickness than the focused border
    }

    @Override
    public Color getFocusedBorderColor()
    {
        return new Color(prefs.getInt(PROP_FOCUSED_BORDER_COLOR, ColorSetManager.getDefault().getFocusedBorderColor().getRGB()));
    }

    @Override
    public void setFocusedBorderColor(Color color)
    {
        Color old = getFocusedBorderColor();
        prefs.putInt(PROP_FOCUSED_BORDER_COLOR, color.getRGB());
        pcs.firePropertyChange(PROP_FOCUSED_BORDER_COLOR, old, color);
    }

    @Override
    public Border getFocusedBorder()
    {
        return BorderFactory.createLineBorder(getFocusedBorderColor(), 1);
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
        String strFont = prefs.get(PROP_NAME_FONT, "Helvetica-BOLD-10");
        return Font.decode(strFont);
    }

    @Override
    public Color getNameFontColor()
    {
        return new Color(prefs.getInt(PROP_NAME_FONT_COLOR, Color.BLACK.getRGB()));
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
        }
        pcs.firePropertyChange(PROP_NAME_FONT_COLOR, old, color);
    }

    @Override
    public void setParentSectionFont(Font font)
    {
        Font old = getNameFont();
        String strFont = Utilities.fontAsString(font);
        prefs.put(PROP_PARENTSECTION_FONT, strFont);
        pcs.firePropertyChange(PROP_PARENTSECTION_FONT, old, font);
    }

    @Override
    public Font getParentSectionFont()
    {
        String strFont = prefs.get(PROP_PARENTSECTION_FONT, "Helvetica-PLAIN-9");
        return Font.decode(strFont);
    }

    @Override
    public Color getParentSectionFontColor()
    {
        return new Color(prefs.getInt(PROP_PARENTSECTION_FONT_COLOR, Color.BLACK.getRGB()));
    }

    @Override
    public void setParentSectionFontColor(Color color)
    {
        Color old = getNameFontColor();
        prefs.putInt(PROP_PARENTSECTION_FONT_COLOR, color.getRGB());
        pcs.firePropertyChange(PROP_PARENTSECTION_FONT_COLOR, old, color);
    }

    @Override
    public void setRhythmFont(Font font)
    {
        Font old = getRhythmFont();
        if (font == null)
        {
            prefs.remove(PROP_RHYTHM_FONT);
            font = getRhythmFont();
        } else
        {
            String strFont = Utilities.fontAsString(font);
            prefs.put(PROP_RHYTHM_FONT, strFont);
        }
        pcs.firePropertyChange(PROP_RHYTHM_FONT, old, font);
    }

    @Override
    public Font getRhythmFont()
    {
        String strFont = prefs.get(PROP_RHYTHM_FONT, "Arial Narrow-BOLD-11");
        return Font.decode(strFont);
    }

    @Override
    public Color getRhythmFontColor()
    {
        return new Color(prefs.getInt(PROP_RHYTHM_FONT_COLOR, new Color(0, 0, 102).getRGB()));     // Deep blue
    }

    @Override
    public void setRhythmFontColor(Color color)
    {
        Color old = getRhythmFontColor();
        if (color == null)
        {
            prefs.remove(PROP_RHYTHM_FONT_COLOR);
            color = getRhythmFontColor();
        } else
        {
            prefs.putInt(PROP_RHYTHM_FONT_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_RHYTHM_FONT_COLOR, old, color);
    }

//    @Override
//    public int getBorderThickness()
//    {
//        return prefs.getInt(PROP_BORDER_THICKNESS, 3);
//    }
//
//    @Override
//    public void setBorderThickness(int thickness)
//    {
//        int old = getBorderThickness();
//        prefs.putInt(PROP_BORDER_THICKNESS, thickness);
//        pcs.firePropertyChange(PROP_BORDER_THICKNESS, old, thickness);
//    }
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


        FontColorUserSettingsProvider.FCSetting fcs = new FontColorUserSettingsProvider.FCSettingAdapter("SongPartNameId", "Song part name")
        {
            @Override
            public Font getFont()
            {
                return getNameFont();
            }

            @Override
            public void setFont(Font f)
            {
                setNameFont(f);
            }

            @Override
            public Color getColor()
            {
                return getNameFontColor();
            }

            @Override
            public void setColor(Color c)
            {
                setNameFontColor(c);
            }
        };
        res.add(fcs);


        fcs = new FontColorUserSettingsProvider.FCSettingAdapter("SongPartRhythmId", "Rhythm name")
        {
            @Override
            public Font getFont()
            {
                return getRhythmFont();
            }

            @Override
            public void setFont(Font f)
            {
                setRhythmFont(f);
            }

            @Override
            public Color getColor()
            {
                return getRhythmFontColor();
            }

            @Override
            public void setColor(Color c)
            {
                setRhythmFontColor(c);
            }
        };
        res.add(fcs);

        fcs = new FontColorUserSettingsProvider.FCSettingAdapter("SongPartColorId", "Song part")
        {

            @Override
            public Color getColor()
            {
                return getDefaultBackgroundColor();
            }

            @Override
            public void setColor(Color c)
            {
                setDefaultBackgroundColor(c);
            }
        };
        res.add(fcs);

        fcs = new FontColorUserSettingsProvider.FCSettingAdapter("SelectedSongPartColorId", "Selected song part")
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

        fcs = new FontColorUserSettingsProvider.FCSettingAdapter("PlayedSongPartColorId", "Played song part")
        {

            @Override
            public Color getColor()
            {
                return getPlaybackColor();
            }

            @Override
            public void setColor(Color c)
            {
                setPlaybackColor(c);
            }
        };
        res.add(fcs);

        return res;
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
            um.duplicateOldPreferences(prefs);
        }

    }

}
