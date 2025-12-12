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
package org.jjazz.cl_editorimpl.itemrenderer;

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
import org.jjazz.cl_editor.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.jjazz.uiutilities.api.FontColorUserSettingsProvider;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.netbeans.api.annotations.common.StaticResource;

@ServiceProviders(value =
{
    @ServiceProvider(service = IR_ChordSymbolSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
})
public class IR_ChordSymbolSettingsImpl implements IR_ChordSymbolSettings, FontColorUserSettingsProvider, FontColorUserSettingsProvider.FCSetting
{

    @StaticResource(relative = true)
    private static final String MUSIC_FONT_PATH = "resources/ScaleDegrees-Times.ttf";
    private static Font MUSIC_FONT;

    /**
     * The Preferences of this object.
     */
    private static final Preferences prefs = NbPreferences.forModule(IR_ChordSymbolSettingsImpl.class);
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
        return new Color(prefs.getInt(PROP_DEFAULT_FONT_COLOR, Color.BLACK.getRGB()));
    }

    @Override
    public void setColor(Color color)
    {
        Color old = getColor();
        if (color == null)
        {
            prefs.remove(PROP_DEFAULT_FONT_COLOR);
            color = getColor();
        } else
        {
            prefs.putInt(PROP_DEFAULT_FONT_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_DEFAULT_FONT_COLOR, old, color);
    }

    @Override
    public Color getSubstituteFontColor()
    {
        return new Color(prefs.getInt(PROP_SUBSTITUTE_FONT_COLOR, new Color(0x026a2e).getRGB()));
    }

    @Override
    public void setSubstituteFontColor(Color color)
    {
        Color old = getSubstituteFontColor();
        if (color == null)
        {
            prefs.remove(PROP_SUBSTITUTE_FONT_COLOR);
            color = getSubstituteFontColor();
        } else
        {
            prefs.putInt(PROP_SUBSTITUTE_FONT_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_SUBSTITUTE_FONT_COLOR, old, color);
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
        
        var fcs = new FontColorUserSettingsProvider.FCSettingAdapter("ChordSymbolSubstituteId", "Chord symbol with substitute")
        {
            @Override
            public Color getColor()
            {
                return getSubstituteFontColor();
            }

            @Override
            public void setColor(Color c)
            {
                setSubstituteFontColor(c);
            }

        };
        res.add(fcs);

        return res;
    }
}
