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
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.addusertrack")
@ActionRegistration(displayName = "not_used", lazy = false)

public class AddUserTrack extends AbstractAction
{

    private final String undoText = ResUtil.getString(getClass(), "CTL_AddUserTrack");
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

        String basename = "User";
        int index = 1;
        var usedNames = song.getUserPhraseNames();
        while (usedNames.contains(basename + index))
        {
            index++;
        }
        String name = basename + index;

        try
        {
            song.addUserPhrase(name, new Phrase(0));
        } catch (PropertyVetoException ex)
        {
            NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
        }
    }
}
