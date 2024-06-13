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
package org.jjazz.ss_editorimpl.actions;

import com.google.common.base.Preconditions;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import org.jjazz.song.api.Song;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.ToggleAction;

/**
 * The action to switch from compact to normal view.
 * <p>
 * There is one action per editor.
 */
public class ToggleCompactView extends ToggleAction implements PropertyChangeListener
{
    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("V");
    private final SS_Editor editor;
    private static final Logger LOGGER = Logger.getLogger(ToggleCompactView.class.getSimpleName());

    private static Map<SS_Editor, ToggleCompactView> MAP_EDITOR_INSTANCES = new HashMap<>();

    /**
     * Get the ToggleCompactView instance associated to a given editor.
     * <p>
     * Create the instance if it does not already exists.
     *
     * @param editor
     * @return
     */
    public synchronized static ToggleCompactView getInstance(SS_Editor editor)
    {
        Preconditions.checkNotNull(editor);
        var instance = MAP_EDITOR_INSTANCES.get(editor);
        if (instance == null)
        {
            instance = new ToggleCompactView(editor);
            MAP_EDITOR_INSTANCES.put(editor, instance);
        }
        return instance;
    }

    private ToggleCompactView(SS_Editor editor)
    {
        this.editor = editor;
        putValue(NAME, "not_used");
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_ToggleCompactViewTooltip"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);     // Useful only if action is used to create a menu entry
        putValue(SMALL_ICON, new ImageIcon(getClass().getResource("resources/CompactViewMode-OFF.png")));
        putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("resources/CompactViewMode-ON.png")));
        putValue("hideActionText", true);


        setSelected(editor.getViewMode().equals(SS_Editor.ViewMode.COMPACT));
        
        
        editor.addPropertyChangeListener(SS_Editor.PROP_EDITOR_VIEW_MODE, this);
        editor.getSongModel().addPropertyChangeListener(Song.PROP_CLOSED, this);
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        var oldMode = editor.getViewMode();
        var newMode = oldMode.equals(SS_Editor.ViewMode.NORMAL) ? SS_Editor.ViewMode.COMPACT : SS_Editor.ViewMode.NORMAL;
        editor.setViewMode(newMode);
    }


    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == editor.getSongModel())
        {
            if (evt.getPropertyName().equals(Song.PROP_CLOSED))
            {
                editor.removePropertyChangeListener(this);
                editor.getSongModel().removePropertyChangeListener(this);
                MAP_EDITOR_INSTANCES.remove(editor);
            }
        } else if (evt.getSource() == editor)
        {
            if (evt.getPropertyName().equals(SS_Editor.PROP_EDITOR_VIEW_MODE))
            {
                setSelected(editor.getViewMode().equals(SS_Editor.ViewMode.COMPACT));
            }
        }
    }

}
