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
package org.jjazz.ss_editorimpl.actions;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import javax.sound.midi.MidiUnavailableException;
import static javax.swing.Action.NAME;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.ss_editor.api.SS_ContextAction;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.jjazz.ss_editor.rpviewer.spi.RpCustomEditorFactory;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.editrpwithcustomeditor")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 10),
        })
public final class EditRpWithCustomEditor extends SS_ContextAction
{
    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_EditRhythmParameter"));
        // putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.RHYTHM_PARAMETER_SELECTION, ListeningTarget.SONG_PART_SELECTION));
    }

    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        List<SongPartParameter> sptps = selection.getSelectedSongPartParameters();
        RhythmParameter<?> rp0 = sptps.get(0).getRp();
        SongPart spt0 = sptps.get(0).getSpt();


        // Open custom editor if supported
        var factory = RpCustomEditorFactory.findFactory(rp0);
        if (factory != null)
        {
            SS_Editor editor = SS_EditorTopComponent.getActive().getEditor();


            // Prepare our dialog
            Song song = editor.getSongModel();
            MidiMix mm = null;
            try
            {
                mm = MidiMixManager.getDefault().findMix(song);
            } catch (MidiUnavailableException ex)
            {
                // Should never happen 
                Exceptions.printStackTrace(ex);
                return;
            }
            SongPartContext sptContext = new SongPartContext(song, mm, spt0);
            Object value = spt0.getRPValue(rp0);
            var dlgEditor = factory.getEditor((RhythmParameter) rp0);
            assert dlgEditor != null : "rp=" + rp0;
            dlgEditor.preset(value, sptContext);


            // Set location
            Rectangle r = editor.getRpViewerRectangle(spt0, rp0);
            Point p = r.getLocation();
            int x = p.x - ((dlgEditor.getWidth() - r.width) / 2);
            int y = p.y - dlgEditor.getHeight();
            x = Math.max(x, 0);
            y = Math.max(y, 0);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int delta = x + dlgEditor.getWidth() - screenSize.width;
            if (delta > 3)
            {
                x -= delta;
            }
            delta = y + dlgEditor.getHeight() - screenSize.height;
            if (delta > 3)
            {
                y -= delta;
            }
            dlgEditor.setLocation(x, y);
            dlgEditor.setVisible(true);


            // Process the result
            Object newValue = dlgEditor.getRpValue();
            if (dlgEditor.isExitOk() && !Objects.equals(value, newValue))
            {
                SongStructure sgs = editor.getModel();
                JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(getActionName());

                for (SongPartParameter sptp : sptps)
                {
                    sgs.setRhythmParameterValue(sptp.getSpt(), (RhythmParameter) sptp.getRp(), newValue);
                }

                JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(getActionName());
            }

        } else
        {
            // Just highlight the SptEditor
            TopComponent tcSptEditor = WindowManager.getDefault().findTopComponent("SptEditorTopComponent");
            if (tcSptEditor != null)
            {
                tcSptEditor.requestVisible();
                tcSptEditor.requestAttention(true);
            }
        }

    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        setEnabled(selection.isRhythmParameterSelected());
    }

}
