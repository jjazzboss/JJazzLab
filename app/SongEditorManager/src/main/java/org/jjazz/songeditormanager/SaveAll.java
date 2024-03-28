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
package org.jjazz.songeditormanager;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;

@ActionID(category = "File", id = "org.jjazz.songeditormanager.SaveAll")
@ActionRegistration(displayName = "#CTL_SaveAll", lazy = true, iconBase = "org/jjazz/songeditormanager/resources/saveAll.png")
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1540),
            @ActionReference(path = "Shortcuts", name = "DS-S")
        })
public final class SaveAll extends AbstractAction implements PropertyChangeListener
{

    private static final Logger LOGGER = Logger.getLogger(SaveAll.class.getSimpleName());

    public SaveAll()
    {
//        Icon icon = SystemAction.get(SaveAction.class).getIcon();
//        putValue(SMALL_ICON, icon);

        var sem = SongEditorManager.getDefault();
        sem.addPropertyChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        var sem = SongEditorManager.getDefault();
        int nbSavedFiles = 0;
        for (var song : sem.getOpenedSongs())
        {
            if (song.getFile() == null)
            {
                if (SaveUtils.SaveAs(song) == 0)
                {
                    nbSavedFiles++;
                }
            } else
            {
                if (SaveUtils.saveSongAndMix(song, song.getFile()) == 0)
                {
                    nbSavedFiles++;
                }
            }
        }
        if (nbSavedFiles > 0)
        {
            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_Saved", nbSavedFiles + " songs"));
        }

    }

    // ======================================================================================================
    // PropertyChangeListener interface
    // ======================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        var sem = SongEditorManager.getDefault();
        if (evt.getSource() == sem)
        {
            if (evt.getPropertyName().equals(SongEditorManager.PROP_SONG_CLOSED)
                    || evt.getPropertyName().equals(SongEditorManager.PROP_SONG_OPENED))
            {
                setEnabled(!sem.getOpenedSongs().isEmpty());
            }
        }
    }
}
