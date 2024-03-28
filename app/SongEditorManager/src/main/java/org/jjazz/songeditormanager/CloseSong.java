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
import java.awt.event.ActionListener;
import org.jjazz.song.api.Song;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.openide.awt.ActionID; 
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;


@ActionID(
        category = "File",
        id = "org.jjazz.base.actions.CloseSong"
)
@ActionRegistration(
        displayName = "#CTL_CloseSong"
)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1700, separatorAfter = 1705),
            @ActionReference(path = "Shortcuts", name = "D-W")
        })
public final class CloseSong implements ActionListener
{

    private final Song context;

    public CloseSong(Song context)
    {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        SongEditorManager.getDefault().closeSong(context, false);
    }
}
