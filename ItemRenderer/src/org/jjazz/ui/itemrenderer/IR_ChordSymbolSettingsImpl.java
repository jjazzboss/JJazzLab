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
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.ui.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.util.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.jjazz.ui.utilities.FontColorUserSettingsProvider;

@ServiceProviders(value =
{
    @ServiceProvider(service = IR_ChordSymbolSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
}
)

public class IR_ChordSymbolSettingsImpl extends IR_ChordSymbolSettings implements FontColorUserSettingsProvider, FontColorUserSettingsProvider.FCSetting
{

    private static final String MUSIC_FONT_PATH = "resources/marl.ttf";
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
            try
            {
                InputStream is = IR_ChordSymbol.class.getResourceAsStream(MUSIC_FONT_PATH);
                MUSIC_FONT = Font.createFont(Font.TRUETYPE_FONT, is);
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
        return "Chord Symbol";
    }

    @Override
    public void setFont(Font font)
    {
        Font old = getFont();
        String strFont = font != null ? Utilities.fontAsString(font) : "Arial-BOLD-18";
        prefs.put(PROP_FONT, strFont);
        pcs.firePropertyChange(PROP_FONT, old, font);
    }

    @Override
    public Font getFont()
    {
        String strFont = prefs.get(PROP_FONT, "Arial-BOLD-18");
        return Font.decode(strFont);
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
        prefs.putInt(PROP_FONT_COLOR, color != null ? color.getRGB() : Color.BLACK.getRGB());
        pcs.firePropertyChange(PROP_FONT_COLOR, old, color);
    }

    @Override
    public void setAltColor(Color color)
    {
        Color old = getAltColor();
        prefs.putInt(PROP_FONT_ALT_COLOR, color != null ? color.getRGB() : new Color(0, 102, 102).getRGB());
        pcs.firePropertyChange(PROP_FONT_ALT_COLOR, old, color);
    }

    @Override
    public Color getAltColor()
    {
        return new Color(prefs.getInt(PROP_FONT_ALT_COLOR, new Color(0, 102, 102).getRGB()));
    }

    @Override
    public Font getMusicFont()
    {
        return MUSIC_FONT;
    }

    @Override
    public int[] getSharpGlyphCode()
    {
        int[] code =
        {
            0x90
        };
        return code;
    }

    @Override
    public int[] getFlatGlyphCode()
    {
        int[] code =
        {
            0x42
        };
        return code;
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
        FontColorUserSettingsProvider.FCSetting fcs = new FontColorUserSettingsProvider.FCSettingAdapter("AltChordSymbolId", "Chord Symbol with Alternate")
        {
            @Override
            public Color getColor()
            {
                return getAltColor();
            }

            @Override
            public void setColor(Color c)
            {
                setAltColor(c);
            }
        };
        res.add(fcs);
        return res;
    }
}
