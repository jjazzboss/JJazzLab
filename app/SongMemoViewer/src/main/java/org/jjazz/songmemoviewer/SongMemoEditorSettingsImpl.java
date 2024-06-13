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
package org.jjazz.songmemoviewer;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.UIManager;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.songmemoviewer.api.SongMemoEditorSettings;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.jjazz.uiutilities.api.FontColorUserSettingsProvider;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;

@ServiceProviders(value =
{
    @ServiceProvider(service = SongMemoEditorSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
}
)
public class SongMemoEditorSettingsImpl implements SongMemoEditorSettings, FontColorUserSettingsProvider
{

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(SongMemoEditorSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongMemoEditorSettingsImpl.class.getName());

    public SongMemoEditorSettingsImpl()
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
            prefs.put(PROP_FONT, Utilities.fontAsString(font));
        }
        pcs.firePropertyChange(PROP_FONT, old, font);
    }

    @Override
    public Font getFont()
    {
        Font def = UIManager.getFont("TextArea.font");
        String strFont = prefs.get(PROP_FONT, Utilities.fontAsString(def));
        return Font.decode(strFont);
    }

    @Override
    public Color getFontColor()
    {
        Color def = UIManager.getColor("TextArea.foreground");
        return new Color(prefs.getInt(PROP_FONT_COLOR, def.getRGB()));
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
        }
        pcs.firePropertyChange(PROP_FONT_COLOR, old, color);
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

    @Override
    public Color getBackgroundColor()
    {
        Color def = UIManager.getColor("TextArea.background");
        return new Color(prefs.getInt(PROP_BACKGROUND_COLOR, def.getRGB()));
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
        FontColorUserSettingsProvider.FCSetting fcs = new FontColorUserSettingsProvider.FCSettingAdapter("SongMemoFontId", "Song memo font")
        {
            @Override
            public Color getColor()
            {
                return getFontColor();
            }

            @Override
            public void setColor(Color c)
            {
                setFontColor(c);
            }

            @Override
            public Font getFont()
            {
                return SongMemoEditorSettingsImpl.this.getFont();
            }

            @Override
            public void setFont(Font f)
            {
                SongMemoEditorSettingsImpl.this.setFont(f);
            }
        };
        res.add(fcs);
//        fcs = new FontColorUserSettingsProvider.FCSettingAdapter("SongMemoBackgroundId", "Song memo")
//        {
//            @Override
//            public Color getColor()
//            {
//                return getBackgroundColor();
//            }
//
//            @Override
//            public void setColor(Color c)
//            {
//                setBackgroundColor(c);
//            }
//        };
//        res.add(fcs);

        return res;
    }

}
