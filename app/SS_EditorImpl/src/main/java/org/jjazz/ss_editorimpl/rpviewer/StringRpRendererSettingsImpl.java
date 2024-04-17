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
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.uiutilities.api.FontColorUserSettingsProvider;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.jjazz.ss_editor.rpviewer.spi.StringRpRendererSettings;

@ServiceProviders(value =
{
    @ServiceProvider(service = StringRpRendererSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
}
)
public class StringRpRendererSettingsImpl implements StringRpRendererSettings, FontColorUserSettingsProvider
{

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(StringRpRendererSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(StringRpRendererSettingsImpl.class.getName());

    public StringRpRendererSettingsImpl()
    {
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
            String strFont = Utilities.fontAsString(font);
            prefs.put(PROP_FONT, strFont);
        }
        pcs.firePropertyChange(PROP_FONT, old, font);
    }

    @Override
    public Font getFont()
    {
        Font defFont = GeneralUISettings.getInstance().getStdFont();
        String strFont = prefs.get(PROP_FONT, null);
        return strFont != null ? Font.decode(strFont) : defFont;
    }

    @Override
    public Color getFontColor()
    {
        return new Color(prefs.getInt(PROP_FONT_COLOR, Color.BLACK.getRGB()));
    }

    @Override
    public void setFontColor(Color color)
    {
        Color old = getFontColor();
        if (color == null)
        {
            prefs.remove(PROP_FONT_COLOR);
            color = getFontColor();
        } else
        {
            prefs.putInt(PROP_FONT_COLOR, color.getRGB());
        };
        pcs.firePropertyChange(PROP_FONT_COLOR, old, color);
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


        FontColorUserSettingsProvider.FCSetting fcs = new FontColorUserSettingsProvider.FCSettingAdapter("rpStringId", "Rhythm parameter value")
        {
            @Override
            public Font getFont()
            {
                return StringRpRendererSettingsImpl.this.getFont();
            }

            @Override
            public void setFont(Font f)
            {
                StringRpRendererSettingsImpl.this.setFont(f);
            }

        };
        res.add(fcs);

        return res;
    }


}
