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
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.ui.colorsetmanager.api.ColorSetManager;
import org.jjazz.ui.sptviewer.api.SptViewerSettings;
import org.jjazz.util.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jerome
 */
@ServiceProvider(service = SptViewerSettings.class)
public class SptViewerSettingsImpl extends SptViewerSettings
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
        prefs.putInt(PROP_DEFAULT_BACKGROUND_COLOR, color.getRGB());
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
        prefs.putInt(PROP_SELECTED_BACKGROUND_COLOR, color.getRGB());
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
        prefs.putInt(PROP_PLAYBACK_COLOR, color.getRGB());
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
        String strFont = Utilities.fontAsString(font);
        prefs.put(PROP_RHYTHM_FONT, strFont);
        pcs.firePropertyChange(PROP_RHYTHM_FONT, old, font);
    }

    @Override
    public Font getNameFont()
    {
        String strFont = prefs.get(PROP_RHYTHM_FONT, "Helvetica-BOLD-10");
        return Font.decode(strFont);
    }

    @Override
    public Color getNameFontColor()
    {
        return new Color(prefs.getInt(PROP_RHYTHM_FONT_COLOR, Color.BLACK.getRGB()));
    }

    @Override
    public void setNameFontColor(Color color)
    {
        Color old = getNameFontColor();
        prefs.putInt(PROP_RHYTHM_FONT_COLOR, color.getRGB());
        pcs.firePropertyChange(PROP_RHYTHM_FONT_COLOR, old, color);
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
        String strFont = Utilities.fontAsString(font);
        prefs.put(PROP_RHYTHM_FONT, strFont);
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
        prefs.putInt(PROP_RHYTHM_FONT_COLOR, color.getRGB());
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

}
