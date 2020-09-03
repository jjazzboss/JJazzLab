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
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.ui.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.util.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.jjazz.ui.utilities.FontColorUserSettingsProvider;
import org.jjazz.uisettings.GeneralUISettings;
import org.jjazz.upgrade.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.netbeans.api.annotations.common.StaticResource;

@ServiceProviders(value =
{
    @ServiceProvider(service = IR_ChordSymbolSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
}
)
public class IR_ChordSymbolSettingsImpl implements IR_ChordSymbolSettings, FontColorUserSettingsProvider, FontColorUserSettingsProvider.FCSetting
{

    @StaticResource(relative = true)
    private static final String MUSIC_FONT_PATH = "resources/ScaleDegrees-Times.ttf";
    private static Font MUSIC_FONT;

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(IR_ChordSymbolSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(IR_ChordSymbolSettingsImpl.class.getName());

    public IR_ChordSymbolSettingsImpl()
    {
        if (MUSIC_FONT == null)
        {
            try (InputStream is = IR_ChordSymbol.class.getResourceAsStream(MUSIC_FONT_PATH))
            {

                MUSIC_FONT = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(MUSIC_FONT); // So it is available in getAvailableFontFamilyNames() etc.
            } catch (IOException | FontFormatException e)
            {
                LOGGER.log(Level.SEVERE, "Can''t access " + MUSIC_FONT_PATH);
            }
        }
    }

    @Override
    public String getId()
    {
        return "ChordSymbolId";
    }

    @Override
    public String getDisplayName()
    {
        return "Chord symbol";
    }

    @Override
    public void setFont(Font font)
    {
        Font old = getFont();
        if (font == null)
        {
            prefs.remove(PROP_FONT);
            font = getFont();
        } else
        {
            prefs.put(PROP_FONT, Utilities.fontAsString(font));
        }
        pcs.firePropertyChange(PROP_FONT, old, font);
    }

    @Override
    public Font getFont()
    {
        Font defFont = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(Font.BOLD, 22f);
        String strFont = prefs.get(PROP_FONT, null);
        return strFont != null ? Font.decode(strFont) : defFont;
    }

    @Override
    public Color getColor()
    {
        return new Color(prefs.getInt(PROP_FONT_COLOR, Color.BLACK.getRGB()));
    }

    @Override
    public void setColor(Color color)
    {
        Color old = getColor();
        if (color == null)
        {
            prefs.remove(PROP_FONT_COLOR);
            color = getColor();
        } else
        {
            prefs.putInt(PROP_FONT_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_FONT_COLOR, old, color);
    }

    @Override
    public void setAccentColor(Feature accentFeature, Color color)
    {
        Color old = getAccentColor(accentFeature);
        if (color == null)
        {
            prefs.remove(getAccentColorKey(accentFeature));
            color = getAccentColor(accentFeature);
        } else
        {
            prefs.putInt(getAccentColorKey(accentFeature), color.getRGB());
        }
        pcs.firePropertyChange(PROP_FONT_ACCENT_COLOR, old, color);
    }

    @Override
    public Color getAccentColor(Feature accentFeature)
    {
        return new Color(prefs.getInt(getAccentColorKey(accentFeature), getDefaultAccentColor(accentFeature).getRGB()));
    }

    private Color getDefaultAccentColor(Feature accentFeature)
    {
        Color defCol;
        switch (accentFeature)
        {
            case ACCENT:
                defCol = getColor();
                break;
            case ACCENT_STRONGER:
                defCol = new Color(0x9B2317);
                break;
            default:
                throw new AssertionError(accentFeature.name());
        }
        return defCol;
    }

    private String getAccentColorKey(Feature accentFeature)
    {
        if (accentFeature != Feature.ACCENT && accentFeature != Feature.ACCENT_STRONGER)
        {
            throw new IllegalArgumentException("accentFeature");
        }
        return PROP_FONT_ACCENT_COLOR + "-" + accentFeature.name();
    }

    @Override
    public Font getMusicFont()
    {
        return MUSIC_FONT;
    }

    @Override
    public char getSharpCharInMusicFont()
    {
        return '#';
    }

    @Override
    public char getFlatCharInMusicFont()
    {
        return 'b';
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
        res.add(this);
        FontColorUserSettingsProvider.FCSetting fcs = new FontColorUserSettingsProvider.FCSettingAdapter("AccentedStrongChordSymbolId", "Chord symbol strong accent")
        {
            @Override
            public Color getColor()
            {
                return getAccentColor(Feature.ACCENT_STRONGER);
            }

            @Override
            public void setColor(Color c)
            {
                setAccentColor(Feature.ACCENT_STRONGER, c);
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
