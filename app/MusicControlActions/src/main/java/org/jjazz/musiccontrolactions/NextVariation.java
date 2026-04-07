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
package org.jjazz.musiccontrolactions;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.musiccontrolactions.api.RemoteAction;
import org.jjazz.musiccontrolactions.spi.RemoteActionProvider;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_Variation;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.lookup.ServiceProvider;

/**
 * Use the next variation on selected song part, or first song part if no selection.
 * <p>
 * This is only used as a remote action, typically in pseudo-arranger mode.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.nextvariation")
@ActionRegistration(displayName = "#CTL_NextVariation", lazy=false)
public class NextVariation extends AbstractAction
{

    private static final String UNDO_TEXT = ResUtil.getString(NextVariation.class, "CTL_NextVariation");
    private static final Logger LOGGER = Logger.getLogger(NextVariation.class.getSimpleName());

    public NextVariation()
    {
        putValue(Action.NAME, UNDO_TEXT);
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "DESC_NextVariation"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        nextOrPrevousVariation(true);
    }

    /**
     * Get the first selected song part of active Song (or first song part if no selection).
     *
     * @return Null if no active song or song is empty.
     */
    static protected SongPart getTargetSongPart()
    {
        Song song = ActiveSongManager.getDefault().getActiveSong();
        if (song == null || song.getSize() == 0)
        {
            return null;
        }

        SS_EditorTopComponent ssTc = SS_EditorTopComponent.get(song.getSongStructure());
        var ssEditor = ssTc.getEditor();
        var selection = new SS_Selection(ssEditor.getLookup());
        SongPart spt;

        if (selection.isEmpty())
        {
            // Use first song part
            spt = song.getSongStructure().getSongPart(0);
        } else
        {
            // Use first selected song part
            spt = selection.isRhythmParameterSelected()
                    ? selection.getSelectedSongPartParameters().getFirst().spt()
                    : selection.getSelectedSongParts().getFirst();
        }

        return spt;
    }

    /**
     * Perform the action.
     *
     * @param next If false change to previous variation.
     */
    static protected void nextOrPrevousVariation(boolean next)
    {
        SongPart spt = getTargetSongPart();
        if (spt == null)
        {
            return;
        }

        var r = spt.getRhythm();
        var rpVariation = RP_SYS_Variation.getVariationRp(r);
        if (rpVariation == null)
        {
            return;
        }

        var sgs = spt.getContainer();
        Song song = sgs.getSong();
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        um.startCEdit(UNDO_TEXT);

        var nextRpValue = next ? rpVariation.getNextValue(spt.getRPValue(rpVariation)) : rpVariation.getPreviousValue(spt.getRPValue(rpVariation));
        sgs.setRhythmParameterValue(spt, rpVariation, nextRpValue);

        um.endCEdit(UNDO_TEXT);

    }


    // ======================================================================
    // Private methods
    // ======================================================================   
    // ======================================================================
    // Inner classes
    // ======================================================================   
    @ServiceProvider(service = RemoteActionProvider.class)
    public static class NextVariationRemoteActionProvider implements RemoteActionProvider
    {

        @Override
        public List<RemoteAction> getRemoteActions()
        {
            RemoteAction ra = RemoteAction.loadFromPreference("MusicControls", "org.jjazz.musiccontrolactions.nextvariation");
            if (ra == null)
            {
                ra = new RemoteAction("MusicControls", "org.jjazz.musiccontrolactions.nextvariation");
                ra.setMidiMessages(RemoteAction.noteOnMidiMessages(0, 23));
            }
            ra.setDefaultMidiMessages(RemoteAction.noteOnMidiMessages(0, 23));
            return Arrays.asList(ra);
        }
    }
}
