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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongUtilities;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * Double the distance by 2 between chords, so chordleadsheet size is also multiplied by 2.
 * <p>
 * @see HalfLeadsheet
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.doubleleadsheet")
@ActionRegistration(displayName = "#CTL_DoubleLeadsheet", lazy = true)
@ActionReferences(
        { 
            @ActionReference(path = "Menu/Edit", position = 2110)
        })
public class DoubleLeadsheet implements ActionListener
{

    private final Song song;
    private final String undoText = ResUtil.getString(getClass(), "CTL_DoubleLeadsheet");
    private static final Logger LOGGER = Logger.getLogger(DoubleLeadsheet.class.getSimpleName());

    public DoubleLeadsheet(Song context)
    {
        this.song = context;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert song != null;   
        ChordLeadSheet cls = song.getChordLeadSheet();
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);
        SongUtilities.doubleChordLeadsheet(song);
        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }
}
