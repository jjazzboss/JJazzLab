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
package org.jjazz.mixconsole.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.song.api.Song;
import org.jjazz.mixconsole.api.MixConsole;
import org.jjazz.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;


@ActionID(category = "MixConsole", id = "org.jjazz.mixconsole.actions.addusertrack")
@ActionRegistration(displayName = "not_used", lazy = false)
public class AddUserTrack extends AbstractAction
{

    private static final String UNDO_TEXT = ResUtil.getString(AddUserTrack.class, "CTL_AddUserTrack");
    private static final Logger LOGGER = Logger.getLogger(AddUserTrack.class.getSimpleName());

    public AddUserTrack()
    {
        putValue("hideActionText", true);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_AddUserTrackTooltip"));
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/mixconsole/resources/AddUserTrackIcon.png")));
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


        // Is it a drums or a melodic user phrase ?
        Phrase p;
        String title = ResUtil.getString(getClass(), "UserTrackTypeDialogTitle");
        String question = ResUtil.getString(getClass(), "UserTrackTypeQuestion");
        String drums = ResUtil.getString(getClass(), "Drums");
        String melodic = ResUtil.getString(getClass(), "Melodic");
        NotifyDescriptor d = new NotifyDescriptor.Confirmation(question, title, NotifyDescriptor.YES_NO_CANCEL_OPTION);
        d.setOptions(new String[]
        {
            melodic, drums
        });
        var res = DialogDisplayer.getDefault().notify(d);
        if (res.equals(-1))
        {
            return;
        }
        p = new Phrase(0, res == drums);


        // Perform the change
        performAddUserPhrase(song, name, p);

        Analytics.logEvent("Add user track");
    }


    /**
     * Perform the undoable action: add the user phrase and opens PianoRollEditor if success.
     * <p>
     * The method notifies user if problem occured.
     *
     * @param song
     * @param name Name of user phrase
     * @param p    The user phrase
     * @return true if success
     */
    static public boolean performAddUserPhrase(Song song, String name, Phrase p)
    {
        
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        um.startCEdit(UNDO_TEXT);

        try
        {
            song.setUserPhrase(name, p);
        } catch (PropertyVetoException ex)
        {
            String msg = "Impossible to add or update user phrase " + name + ": " + ex.getLocalizedMessage();
            um.abortCEdit(UNDO_TEXT, msg);
            return false;
        } catch (Exception ex)    // Capture other programming exceptions, because method can be called from within a thread
        {
            String msg = "Unexpected exception! Impossible to add or update user phrase " + name + ".\n" + ex.getMessage();
            um.abortCEdit(UNDO_TEXT, msg);
            LOGGER.log(Level.SEVERE, "setUserPhraseAction() {0}", msg);
            Exceptions.printStackTrace(ex);
            return false;
        }

        um.endCEdit(UNDO_TEXT);


        // Open the PianoRollEditor
        MidiMix midiMix = MidiMixManager.getDefault().findExistingMix(song);
        var userRv = midiMix.getUserRhythmVoice(name);
        assert userRv != null : " midiMix=" + midiMix + " name=" + name;
        SongEditorManager.getDefault().showPianoRollEditorForUserTrack(song, midiMix, userRv);
        
        return true;
    }

    // ======================================================================================================
    // Private methods
    // ======================================================================================================

}
