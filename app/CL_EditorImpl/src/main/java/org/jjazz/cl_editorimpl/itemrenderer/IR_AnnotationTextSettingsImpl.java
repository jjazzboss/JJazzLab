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
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.cl_editor.itemrenderer.api.IR_AnnotationTextSettings;
import org.jjazz.uiutilities.api.FontColorUserSettingsProvider;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders(value =
{
    @ServiceProvider(service = IR_AnnotationTextSettings.class),
    @ServiceProvider(service = FontColorUserSettingsProvider.class)
})
public class IR_AnnotationTextSettingsImpl implements IR_AnnotationTextSettings, FontColorUserSettingsProvider, FontColorUserSettingsProvider.FCSetting
{

    /**
     * The Preferences of this object.
     */
    private static final Preferences prefs = NbPreferences.forModule(IR_AnnotationTextSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(IR_AnnotationTextSettingsImpl.class.getName());

    @Override
    public String getId()
    {
        return "AnnotationTextId";
    }

    @Override
    public String getDisplayName()
    {
        return ResUtil.getString(getClass(), "CTL_AnnotationText");
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
        Font defFont = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(13f);
        String strFont = prefs.get(PROP_FONT, null);
        return strFont != null ? Font.decode(strFont) : defFont;
    }

    @Override
    public Color getColor()
    {
        return new Color(prefs.getInt(PROP_FONT_COLOR, new Color(0, 0, 153).getRGB()));
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
        return List.of(this);
    }

}
