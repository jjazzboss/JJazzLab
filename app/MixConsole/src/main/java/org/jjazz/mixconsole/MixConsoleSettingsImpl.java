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
package org.jjazz.mixconsole;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.mixconsole.api.MixConsoleSettings;
import org.jjazz.uiutilities.api.FontColorUserSettingsProvider;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders(value =
{
    @ServiceProvider(service = MixConsoleSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
}
)
public class MixConsoleSettingsImpl implements MixConsoleSettings, FontColorUserSettingsProvider
{

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(MixConsoleSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    @Override
    public Color getMixChannelBackgroundColor()
    {
        Color color = new Color(prefs.getInt(PROP_CHANNEL_PANEL_BACKGROUND_COLOR, GeneralUISettings.getInstance().getColor("mixchannel.background").getRGB()));
        return color;
    }

    @Override
    public void setMixChannelBackgroundColor(Color color)
    {
        Color old = getMixChannelBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_CHANNEL_PANEL_BACKGROUND_COLOR);
            color = getMixChannelBackgroundColor();
        } else
        {
            prefs.putInt(PROP_CHANNEL_PANEL_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_CHANNEL_PANEL_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Color getBackgroundColor()
    {
        Color color = new Color(prefs.getInt(PROP_BACKGROUND_COLOR, GeneralUISettings.getInstance().getColor("mixconsole.background").getRGB()));
        return color;

    }

    @Override
    public void setBackgroundColor(Color color)
    {
        Color old = getBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_BACKGROUND_COLOR);
            color = getBackgroundColor();
        } else
        {
            prefs.putInt(PROP_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BACKGROUND_COLOR, old, color);
    }

//    @Override
//    public void setNameFont(Font font)
//    {
//        Font old = getNameFont();
//        if (font == null)
//        {
//            prefs.remove(PROP_NAME_FONT);
//            font = getNameFont();
//        } else
//        {
//            String strFont = Utilities.fontAsString(font);
//            prefs.put(PROP_NAME_FONT, strFont);
//        }
//        pcs.firePropertyChange(PROP_NAME_FONT, old, font);
//    }
//
//    @Override
//    public Font getNameFont()
//    {
//        String strFont = prefs.get(PROP_NAME_FONT, "Helvetica-BOLD-15");
//        return Font.decode(strFont);
//    }
//
//    @Override
//    public void setRhythmFont(Font font)
//    {
//        Font old = getRhythmFont();
//        if (font == null)
//        {
//            prefs.remove(PROP_RHYTHM_FONT);
//            font = getRhythmFont();
//        } else
//        {
//            String strFont = Utilities.fontAsString(font);
//            prefs.put(PROP_RHYTHM_FONT, strFont);
//        }
//        pcs.firePropertyChange(PROP_RHYTHM_FONT, old, font);
//    }
//
//    @Override
//    public Font getRhythmFont()
//    {
//        String strFont = prefs.get(PROP_RHYTHM_FONT, "Helvetica-PLAIN-10");
//        return Font.decode(strFont);
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
        FontColorUserSettingsProvider.FCSetting fcs = new FontColorUserSettingsProvider.FCSettingAdapter("MixConsoleSettingsId", "Mix channel panel")
        {
            @Override
            public Color getColor()
            {
                return getMixChannelBackgroundColor();
            }

            @Override
            public void setColor(Color c)
            {
                setMixChannelBackgroundColor(c);
            }
        };
        res.add(fcs);
        fcs = new FontColorUserSettingsProvider.FCSettingAdapter("MixConsoleBackgroundId", "Mix console background")
        {
            @Override
            public Color getColor()
            {
                return getBackgroundColor();
            }

            @Override
            public void setColor(Color c)
            {
                setBackgroundColor(c);
            }
        };
        res.add(fcs);
        return res;
    }

}
