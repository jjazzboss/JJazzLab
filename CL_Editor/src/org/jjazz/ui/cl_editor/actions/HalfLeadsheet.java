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
package org.jjazz.ui.cl_editor.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongUtils;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * Half the distance by 2 between chords, so chordleadsheet size is also divided by 2.
 *
 * @see DoubleLeadsheet
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.halfleadsheet")
@ActionRegistration(displayName = "#CTL_HalfLeadsheet", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/Tools", position = 110)
        })
public class HalfLeadsheet implements ActionListener
{

    private final Song song;
    private final String undoText = ResUtil.getString(getClass(), "CTL_HalfLeadsheet");
    private static final Logger LOGGER = Logger.getLogger(HalfLeadsheet.class.getSimpleName());

    public HalfLeadsheet(Song context)
    {
        this.song = context;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert song != null;   //NOI18N

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song.getChordLeadSheet());
        um.startCEdit(undoText);

        try
        {
            SongUtils.halfChordLeadsheet(song);
        } catch (UnsupportedEditException ex)
        {
            String msg = "Impossible to half leadsheet.\n" + ex.getLocalizedMessage();
            um.handleUnsupportedEditException(undoText, msg);
            return;
        }

        um.endCEdit(undoText);
    }
}
