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
package org.jjazz.cl_editorimpl.actions;

import com.google.common.base.Preconditions;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.song.api.Song;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.ToggleAction;


/**
 * The action to show/hide bar annotations.
 * <p>
 * There is one action per editor.
 */
public class ToggleBarAnnotations extends ToggleAction implements PropertyChangeListener
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_L);
    private static final Logger LOGGER = Logger.getLogger(ToggleBarAnnotations.class.getSimpleName());
    private final CL_Editor editor;
    private static final Map<CL_Editor, ToggleBarAnnotations> MAP_EDITOR_INSTANCES = new HashMap<>();

    /**
     * Get the ToggleBarAnnotations instance associated to a given editor.
     * <p>
     * Create the instance if it does not already exists.
     *
     * @param editor
     * @return
     */
    public synchronized static ToggleBarAnnotations getInstance(CL_Editor editor)
    {
        Preconditions.checkNotNull(editor);
        var instance = MAP_EDITOR_INSTANCES.get(editor);
        if (instance == null)
        {
            instance = new ToggleBarAnnotations(editor);
            MAP_EDITOR_INSTANCES.put(editor, instance);
        }
        return instance;
    }

    private ToggleBarAnnotations(CL_Editor editor)
    {
        this.editor = editor;

        putValue(NAME, "ToggleBarAnnotationsNotUsed");
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "ToggleBarAnnotationsTooltip"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);     // Useful only if action is used to create a menu entry
        putValue(SMALL_ICON, new ImageIcon(getClass().getResource("resources/ShowBarAnnotations-OFF.png")));
        putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("resources/ShowBarAnnotations-ON.png")));
        putValue("hideActionText", true);

        setSelected(CL_EditorClientProperties.isBarAnnotationVisible(editor.getSongModel()));

        editor.getSongModel().addPropertyChangeListener(this);
        editor.getSongModel().getClientProperties().addPropertyChangeListener(this);


    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        boolean b = CL_EditorClientProperties.isBarAnnotationVisible(editor.getSongModel());
        CL_EditorClientProperties.setBarAnnotationVisible(editor.getSongModel(), !b);
        editor.getSongModel().setSaveNeeded(true);
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
                editor.getSongModel().removePropertyChangeListener(this);
                editor.getSongModel().getClientProperties().removePropertyChangeListener(this);
                MAP_EDITOR_INSTANCES.remove(editor);
            }
        } else if (evt.getSource() == editor.getSongModel().getClientProperties())
        {
            if (evt.getPropertyName().equals(CL_EditorClientProperties.PROP_BAR_ANNOTATION_VISIBLE))
            {
                setSelected(CL_EditorClientProperties.isBarAnnotationVisible(editor.getSongModel()));
            }
        }
    }
}
