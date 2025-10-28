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
package org.jjazz.pianoroll;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.uiutilities.api.HSLColor;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = PianoRollEditorSettings.class)
public class PianoRollEditorSettingsImpl implements PianoRollEditorSettings
{

    /**
     * The Preferences of this object.
     */
    private static final Preferences prefs = NbPreferences.forModule(PianoRollEditorSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(PianoRollEditorSettingsImpl.class.getSimpleName());

    public PianoRollEditorSettingsImpl()
    {
        // Listen to locale changes
        // GeneralUISettings.getInstance().addPropertyChangeListener(this);
    }

    @Override
    public Color getLoopZoneWhiteKeyLaneBackgroundColor()
    {
        var def = HSLColor.changeLuminance(getWhiteKeyLaneBackgroundColor(), -7);
        return new Color(prefs.getInt(PROP_LOOP_ZONE_WHITE_KEY_LANE_BACKGROUND_COLOR, def.getRGB()));
    }

    @Override
    public void setLoopZoneWhiteKeyLaneBackgroundColor(Color color)
    {
        Color old = getLoopZoneWhiteKeyLaneBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_LOOP_ZONE_WHITE_KEY_LANE_BACKGROUND_COLOR);
            color = getLoopZoneWhiteKeyLaneBackgroundColor();
        } else
        {
            prefs.putInt(PROP_LOOP_ZONE_WHITE_KEY_LANE_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_LOOP_ZONE_WHITE_KEY_LANE_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Color getLoopZoneBlackKeyLaneBackgroundColor()
    {
        var def = HSLColor.changeLuminance(getBlackKeyLaneBackgroundColor(), -7);
        return new Color(prefs.getInt(PROP_LOOP_ZONE_BLACK_KEY_LANE_BACKGROUND_COLOR, def.getRGB()));
    }

    @Override
    public void setLoopZoneBlackKeyLaneBackgroundColor(Color color)
    {
        Color old = getLoopZoneBlackKeyLaneBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_LOOP_ZONE_BLACK_KEY_LANE_BACKGROUND_COLOR);
            color = getLoopZoneWhiteKeyLaneBackgroundColor();
        } else
        {
            prefs.putInt(PROP_LOOP_ZONE_BLACK_KEY_LANE_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_LOOP_ZONE_BLACK_KEY_LANE_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Color getBlackKeyLaneBackgroundColor()
    {
        return new Color(prefs.getInt(PROP_BLACK_KEY_LANE_BACKGROUND_COLOR, new Color(182, 195, 210).getRGB()));
    }

    @Override
    public void setBlackKeyLaneBackgroundColor(Color color)
    {
        Color old = getBlackKeyLaneBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_BLACK_KEY_LANE_BACKGROUND_COLOR);
            color = getBlackKeyLaneBackgroundColor();
        } else
        {
            prefs.putInt(PROP_BLACK_KEY_LANE_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BLACK_KEY_LANE_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Color getWhiteKeyLaneBackgroundColor()
    {
        return new Color(prefs.getInt(PROP_WHITE_KEY_LANE_BACKGROUND_COLOR, new Color(193, 206, 220).getRGB()));
    }

    @Override
    public void setWhiteKeyLaneBackgroundColor(Color color)
    {
        Color old = getWhiteKeyLaneBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_WHITE_KEY_LANE_BACKGROUND_COLOR);
            color = getWhiteKeyLaneBackgroundColor();
        } else
        {
            prefs.putInt(PROP_WHITE_KEY_LANE_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_WHITE_KEY_LANE_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Font getRulerBaseFont()
    {
        Font defFont = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(13f);
        String strFont = prefs.get(PROP_RULER_BASE_FONT, null);
        return strFont != null ? Font.decode(strFont) : defFont;
    }

    @Override
    public void setRulerBaseFont(Font font)
    {
        Font old = getRulerBaseFont();
        if (font == null)
        {
            prefs.remove(PROP_RULER_BASE_FONT);
            font = getRulerBaseFont();
        } else
        {
            prefs.put(PROP_RULER_BASE_FONT, Utilities.fontAsString(font));
        }
        pcs.firePropertyChange(PROP_RULER_BASE_FONT, old, font);
    }

    @Override
    public Color getRulerBackgroundColor()
    {
        return new Color(prefs.getInt(PROP_RULER_BACKGROUND_COLOR, new Color(15, 29, 42).getRGB()));
    }

    @Override
    public void setRulerBackgroundColor(Color color)
    {
        Color old = getRulerBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_RULER_BACKGROUND_COLOR);
            color = getBlackKeyLaneBackgroundColor();
        } else
        {
            prefs.putInt(PROP_RULER_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_RULER_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Color getRulerTsLaneBackgroundColor()
    {
        return new Color(prefs.getInt(PROP_RULER_TS_LANE_BACKGROUND_COLOR, new Color(4, 6, 8).getRGB()));
    }

    @Override
    public void setRulerTsLaneBackgroundColor(Color color)
    {
        Color old = getRulerTsLaneBackgroundColor();
        if (color == null)
        {
            prefs.remove(PROP_RULER_TS_LANE_BACKGROUND_COLOR);
            color = getRulerTsLaneBackgroundColor();
        } else
        {
            prefs.putInt(PROP_RULER_TS_LANE_BACKGROUND_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_RULER_TS_LANE_BACKGROUND_COLOR, old, color);
    }

    @Override
    public Color getRulerBarTickColor()
    {
        return new Color(prefs.getInt(PROP_RULER_BAR_TICK_COLOR, new Color(160, 160, 160).getRGB()));
    }

    @Override
    public void setRulerBarTickColor(Color color)
    {
        Color old = getRulerBarTickColor();
        if (color == null)
        {
            prefs.remove(PROP_RULER_BAR_TICK_COLOR);
            color = getRulerBarTickColor();
        } else
        {
            prefs.putInt(PROP_RULER_BAR_TICK_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_RULER_BAR_TICK_COLOR, old, color);
    }

    @Override
    public Color getNoteColor()
    {
        return new Color(prefs.getInt(PROP_NOTE_COLOR, Color.RED.getRGB()));
    }

    @Override
    public void setNoteColor(Color color)
    {
        Color old = getNoteColor();
        if (color == null)
        {
            prefs.remove(PROP_NOTE_COLOR);
            color = getNoteColor();
        } else
        {
            prefs.putInt(PROP_NOTE_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_NOTE_COLOR, old, color);
    }

    @Override
    public Color getNoteContourColor()
    {
        return new Color(prefs.getInt(PROP_NOTE_CONTOUR_COLOR, Color.DARK_GRAY.brighter().getRGB()));
    }

    @Override
    public void setNoteContourColor(Color color)
    {
        Color old = getNoteContourColor();
        if (color == null)
        {
            prefs.remove(PROP_NOTE_CONTOUR_COLOR);
            color = getNoteContourColor();
        } else
        {
            prefs.putInt(PROP_NOTE_CONTOUR_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_NOTE_CONTOUR_COLOR, old, color);
    }

    @Override
    public Color getSelectedNoteColor()
    {
        return new Color(prefs.getInt(PROP_SELECTED_NOTE_COLOR, new Color(220, 220, 0).getRGB()));
    }

    @Override
    public void setSelectedNoteColor(Color color)
    {
        Color old = getSelectedNoteColor();
        if (color == null)
        {
            prefs.remove(PROP_SELECTED_NOTE_COLOR);
            color = getSelectedNoteColor();
        } else
        {
            prefs.putInt(PROP_SELECTED_NOTE_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_SELECTED_NOTE_COLOR, old, color);
    }

    @Override
    public Color getBarLineColor()
    {
        return new Color(prefs.getInt(PROP_BAR_LINE_COLOR, new Color(142, 158, 174).getRGB()));
    }

    @Override
    public void setBarLineColor(Color color)
    {
        Color old = getBarLineColor();
        if (color == null)
        {
            prefs.remove(PROP_BAR_LINE_COLOR);
            color = getBarLineColor();
        } else
        {
            prefs.putInt(PROP_BAR_LINE_COLOR, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BAR_LINE_COLOR, old, color);
    }

    @Override
    public Color getFocusedNoteContourColor()
    {
        return new Color(prefs.getInt(PROP_FOCUSED_NOTE_CONTOUR_COLOR, Color.WHITE.getRGB()));
    }

    @Override
    public void setFocusedNoteContourColor(Color color)
    {
        Color old = getFocusedNoteContourColor();
        if (color == null)
        {
            prefs.remove(PROP_FOCUSED_NOTE_CONTOUR_COLOR);
            color = getFocusedNoteContourColor();
        } else
        {
            prefs.putInt(PROP_FOCUSED_NOTE_CONTOUR_COLOR, color.getRGB());
        };
        pcs.firePropertyChange(PROP_FOCUSED_NOTE_CONTOUR_COLOR, old, color);
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

}
