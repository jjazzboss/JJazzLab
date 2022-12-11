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
package org.jjazz.pianoroll;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = PianoRollEditorSettings.class)
public class PianoRollEditorSettingsImpl implements PianoRollEditorSettings
{

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(PianoRollEditorSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(PianoRollEditorSettingsImpl.class.getSimpleName());

    public PianoRollEditorSettingsImpl()
    {
        // Listen to locale changes
        // GeneralUISettings.getInstance().addPropertyChangeListener(this);
    }


    @Override
    public Color getBackgroundColor1()
    {
        return new Color(prefs.getInt(PROP_BACKGROUND_COLOR1, new Color(182,195,210).getRGB()));
    }

    @Override
    public void setBackgroundColor1(Color color)
    {
        Color old = getBackgroundColor1();
        if (color == null)
        {
            prefs.remove(PROP_BACKGROUND_COLOR1);
            color = getBackgroundColor1();
        } else
        {
            prefs.putInt(PROP_BACKGROUND_COLOR1, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BACKGROUND_COLOR1, old, color);
    }

    @Override
    public Color getBackgroundColor2()
    {
        return new Color(prefs.getInt(PROP_BACKGROUND_COLOR2,  new Color(193,206,220).getRGB()));
    }

    @Override
    public void setBackgroundColor2(Color color)
    {
        Color old = getBackgroundColor2();
        if (color == null)
        {
            prefs.remove(PROP_BACKGROUND_COLOR2);
            color = getBackgroundColor2();
        } else
        {
            prefs.putInt(PROP_BACKGROUND_COLOR2, color.getRGB());
        }
        pcs.firePropertyChange(PROP_BACKGROUND_COLOR2, old, color);
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
        return new Color(prefs.getInt(PROP_SELECTED_NOTE_COLOR, Color.MAGENTA.brighter().getRGB()));
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
        return new Color(prefs.getInt(PROP_BAR_LINE_COLOR, new Color(142,158,174).getRGB()));
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
