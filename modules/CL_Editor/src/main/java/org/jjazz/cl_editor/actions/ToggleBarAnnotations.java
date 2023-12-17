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
package org.jjazz.cl_editor.actions;

import com.google.common.base.Preconditions;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.ToggleAction;


/**
 * The action to show/hide bar annotations.
 * <p>
 * There is one action per editor.
 */
public class ToggleBarAnnotations extends ToggleAction implements PropertyChangeListener
{

    private static final Logger LOGGER = Logger.getLogger(ToggleBarAnnotations.class.getSimpleName());
    private final CL_Editor editor;
    private static Map<CL_Editor, ToggleBarAnnotations> MAP_EDITOR_INSTANCES = new HashMap<>();

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
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("V"));     // Useful only if action is used to create a menu entry
        putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/cl_editor/actions/resources/ShowBarAnnotations-OFF.png")));
        putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/cl_editor/actions/resources/ShowBarAnnotations-ON.png")));
        putValue("hideActionText", true);


        setSelected(editor.isBarAnnotationVisible());


        editor.addPropertyChangeListener(CL_Editor.PROP_BAR_ANNOTATION_VISIBLE, this);
        editor.getSongModel().addPropertyChangeListener(Song.PROP_CLOSED, this);


    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        boolean b = editor.isBarAnnotationVisible();
        editor.setBarAnnotationVisible(!b);
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
            if (evt.getPropertyName().equals(CL_Editor.PROP_BAR_ANNOTATION_VISIBLE))
            {
                setSelected(editor.isBarAnnotationVisible());
            }
        }
    }
}
