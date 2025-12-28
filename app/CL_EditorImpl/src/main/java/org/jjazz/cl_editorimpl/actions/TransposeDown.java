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
import org.jjazz.cl_editor.api.CL_ContextAction;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.harmony.api.Note;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrolactions.api.RemoteAction;
import org.jjazz.musiccontrolactions.spi.RemoteActionProvider;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.lookup.ServiceProvider;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.transposedown")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 410),
            @ActionReference(path = "Shortcuts", name = "D-DOWN")
        })
public final class TransposeDown extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_DOWN);
    private static final String undoName = ResUtil.getString(TransposeUp.class, "CTL_TransposeDown");
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
        transpose(selection.getSelectedChordSymbols(), -1, Note.Accidental.FLAT, undoName);
    }

    /**
     * Perform an undoable transposition of chord symbols.
     *
     * @param chordSymbols
     * @param t
     * @param accidental
     * @param undoActionName
     */
    static public void transpose(List<CLI_ChordSymbol> chordSymbols, int t, Note.Accidental accidental, String undoActionName)
    {
        Objects.requireNonNull(chordSymbols);
        Objects.requireNonNull(undoActionName);

        if (chordSymbols.isEmpty())
        {
            return;
        }
        var cls = chordSymbols.getFirst().getContainer();
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoActionName);
        for (CLI_ChordSymbol cliCs : chordSymbols)
        {
            ExtChordSymbol ecs = cliCs.getData();
            ExtChordSymbol newEcs = ecs.getTransposedChordSymbol(t, accidental);
            cliCs.getContainer().changeItem(cliCs, newEcs);
        }
        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoActionName);
    }

    // ======================================================================
    // Inner classes
    // ======================================================================   
    @ServiceProvider(service = RemoteActionProvider.class)
    public static class TransposeDownRemoteActionProvider implements RemoteActionProvider
    {

        @Override
        public List<RemoteAction> getRemoteActions()
        {
            RemoteAction ra = RemoteAction.loadFromPreference("JJazz", "org.jjazz.cl_editor.actions.transposedownremote");
            if (ra == null)
            {
                ra = new RemoteAction("JJazz", "org.jjazz.cl_editor.actions.transposedownremote");
                ra.setMidiMessages(RemoteAction.noteOnMidiMessages(0, 29));
            }
            ra.setDefaultMidiMessages(RemoteAction.noteOnMidiMessages(0, 29));
            return List.of(ra);
        }
    }

    @ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.transposedownremote")
    @ActionRegistration(displayName = "not_used", lazy = false)
    static public final class TransposeDownRemoteAction extends AbstractAction
    {

        public TransposeDownRemoteAction()
        {
            putValue(NAME, undoName);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            var mc = MusicController.getInstance();
            var playbackSession = mc.getPlaybackSession();
            if (playbackSession != null && playbackSession.getContext() != PlaybackSession.Context.SONG)
            {
                return;
            }

            var song = ActiveSongManager.getDefault().getActiveSong();
            if (song != null)
            {
                var items = song.getChordLeadSheet().getItems(CLI_ChordSymbol.class);
                transpose(items, -1, Note.Accidental.FLAT, undoName);
            }
        }

    }
}
