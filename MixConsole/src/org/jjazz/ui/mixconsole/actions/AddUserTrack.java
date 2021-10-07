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
package org.jjazz.ui.mixconsole.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.song.api.Song;
import org.jjazz.ui.mixconsole.api.MixConsole;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.addusertrack")
@ActionRegistration(displayName = "not_used", lazy = false)

public class AddUserTrack extends AbstractAction
{

    private static final String undoText = ResUtil.getString(AddUserTrack.class, "CTL_AddUserTrack");
    private static final Logger LOGGER = Logger.getLogger(AddUserTrack.class.getSimpleName());

    public AddUserTrack()
    {
        putValue("hideActionText", true);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_AddUserTrackTooltip"));
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/AddUser-16x16.png")));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MixConsole mixConsole = MixConsoleTopComponent.getInstance().getEditor();
        Song song = mixConsole.getSong();
        if (song == null)
        {
            return;
        }


        // Find a name not already used
        String basename = "User";
        int index = 1;
        var usedNames = song.getUserPhraseNames();
        while (usedNames.contains(basename + index))
        {
            index++;
        }
        String name = basename + index;


        // Perform the change
        setUserPhraseAction(song, name, new Phrase(0));
    }

    /**
     * The undoable action.
     *
     * @param song
     * @param name
     * @param p
     * @return True if operation was successful
     * @throws PropertyVetoException
     */
    static public boolean setUserPhraseAction(Song song, String name, Phrase p)
    {
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        um.startCEdit(undoText);

        try
        {
            song.setUserPhrase(name, p);
        } catch (PropertyVetoException ex)
        {
            String msg = "Impossible to add or update user phrase " + name + ".\n" + ex.getLocalizedMessage();
            um.handleUnsupportedEditException(undoText, msg);
            return false;
        } catch (Exception ex)    // Capture other programming exceptions, because method can be called from within a thread
        {
            String msg = "Impossible to add or update user phrase " + name + ".\n" + ex.getLocalizedMessage();            
            um.handleUnsupportedEditException(undoText, msg);            
            Exceptions.printStackTrace(ex);
            return false;
        }

        um.endCEdit(undoText);
        
        return true;

    }

}
