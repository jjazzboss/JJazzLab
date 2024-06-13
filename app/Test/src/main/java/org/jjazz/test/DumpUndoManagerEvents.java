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
package org.jjazz.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.song.api.Song;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Utilities;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.dumpundomanagerevents")
//@ActionRegistration(displayName = "Dump undomanager events")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 870013),
//        })
public final class DumpUndoManagerEvents implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(DumpUndoManagerEvents.class.getSimpleName());

    public DumpUndoManagerEvents()
    {
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.info("DumpUndoManagerEvents() --");   
        Song song = Utilities.actionsGlobalContext().lookup(Song.class);

        if (song == null)
        {
            LOGGER.info("No current song, aborting");   
            return;
        }

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        LOGGER.log(Level.INFO, "um={0}", um);

    }
}
