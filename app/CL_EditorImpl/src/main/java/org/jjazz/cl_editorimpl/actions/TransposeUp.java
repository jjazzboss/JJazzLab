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
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_Selection;
import static org.jjazz.cl_editorimpl.actions.TransposeDown.transpose;
import org.jjazz.harmony.api.Note;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession.Context;
import org.jjazz.musiccontrolactions.api.RemoteAction;
import org.jjazz.musiccontrolactions.spi.RemoteActionProvider;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.lookup.ServiceProvider;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.transposeup")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 400),
            @ActionReference(path = "Shortcuts", name = "D-UP")
        })
public final class TransposeUp extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_UP);
    private static final String undoName = ResUtil.getString(TransposeUp.class, "CTL_TransposeUp");
    private static final Logger LOGGER = Logger.getLogger(TransposeUp.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, undoName);
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION));
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        setEnabled(selection.isChordSymbolSelected());
    }

    @Override
    public void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        transpose(selection.getSelectedChordSymbols(), 1, Note.Accidental.SHARP, getActionName());
    }
    // ======================================================================
    // Inner classes
    // ======================================================================   

    @ServiceProvider(service = RemoteActionProvider.class)
    public static class TransposeUpRemoteActionProvider implements RemoteActionProvider
    {

        @Override
        public List<RemoteAction> getRemoteActions()
        {
            RemoteAction ra = RemoteAction.loadFromPreference("JJazz", "org.jjazz.cl_editor.actions.transposeupremote");
            if (ra == null)
            {
                ra = new RemoteAction("JJazz", "org.jjazz.cl_editor.actions.transposeupremote");
                ra.setMidiMessages(RemoteAction.noteOnMidiMessages(0, 31));
            }
            ra.setDefaultMidiMessages(RemoteAction.noteOnMidiMessages(0, 31));
            return List.of(ra);
        }
    }

    @ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.transposeupremote")
    @ActionRegistration(displayName = "not_used", lazy = false)
    static public final class TransposeUpRemoteAction extends AbstractAction
    {

        public TransposeUpRemoteAction()
        {
            putValue(NAME, undoName);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            var mc = MusicController.getInstance();
            var playbackSession = mc.getPlaybackSession();
            if (playbackSession != null && playbackSession.getContext() != Context.SONG)
            {
                return;
            }
            
            
            var song = ActiveSongManager.getDefault().getActiveSong();
            if (song != null)
            {
                var items = song.getChordLeadSheet().getItems(CLI_ChordSymbol.class);
                transpose(items, 1, Note.Accidental.SHARP, undoName);
            }
        }

    }
}
