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
package org.jjazz.pianoroll.api;

import java.awt.Point;
import javax.swing.JPanel;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.PianoRollEditorImpl;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.quantizer.api.Quantization;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;

/**
 * A piano roll editor of a SizedPhrase.
 * <p>
 * Its Lookup must contain :<br>
 * - editor's ActionMap<br>
 * - edited SizedPhrase<br>
 * - edited Song (if there is one) <br>
 * - the selected NoteEvents<br>
 */
public abstract class PianoRollEditor extends JPanel implements Lookup.Provider
{

    public static PianoRollEditor getDefault(SizedPhrase sp, DrumKit.KeyMap keymap)
    {
        return new PianoRollEditorImpl(sp, keymap);
    }

    abstract public SizedPhrase getModel();

    /**
     * The drum key map used by the edited phrase.
     *
     * @return Null if it's a melodic phrase.
     */
    abstract public DrumKit.KeyMap getDrumKeyMap();


    /**
     * @return The UndoManager used but this editor.
     */
    abstract public UndoRedo getUndoManager();

    abstract public PianoRollEditorSettings getSettings();

    /**
     * Clean up everything so component can be garbaged.
     */
    abstract public void cleanup();

    /**
     * Set the zoom value.
     *
     * @param zoom
     */
    abstract public void setZoom(ZoomValue zoom);

    /**
     * Get the zoom value.
     *
     * @return
     */
    abstract public ZoomValue getZoom();

    /**
     * Set the display quantization.
     *
     * @param q
     */
    abstract public void setQuantization(Quantization q);

    /**
     * Get the display quantization.
     *
     * @return
     */
    abstract public Quantization getQuantization();

    /**
     * Enable or disable the snap to quantization feature.
     *
     * @param b
     */
    abstract public void setSnapEnabled(boolean b);

    /**
     * Check if the snap to quantization feature is enabled.
     *
     * @return
     */
    abstract public boolean isSnapEnabled();

    /**
     * Select a note.
     *
     * @param ne
     * @param b
     */
    abstract public void selectNote(NoteEvent ne, boolean b);

    /**
     * Set the focus on a specific note.
     *
     * @param ne
     */
    abstract public void setFocusOnNote(NoteEvent ne);

    /**
     * Make sure the specified note is visible.
     *
     * @param ne
     */
    abstract public void setNoteVisible(NoteEvent ne);

    abstract public void setActiveTool(EditTool tool);

    abstract public EditTool getActiveTool();

    /**
     * Get the focused note, if any.
     *
     * @return Can be null.
     */
    abstract public NoteEvent getFocusedNote();

    /**
     * Show a playback point in the editor at specified position.
     *
     * @param b   Show/hide the playback point.
     * @param pos The position in beats
     */
    abstract public void showPlaybackPoint(boolean b, float pos);

    /**
     * Return the position in beats that correspond to a graphical point in the editor.
     * <p>
     *
     * @param editorPoint A point in the editor's coordinates.
     * @return
     *
     */
    abstract public float getPositionFromPoint(Point editorPoint);

    /**
     * Return the pitch that correspond to a graphical point in the editor.
     *
     * @param editorPoint A point in the editor's coordinates.
     * @return
     */
    abstract public int getPitchFromPoint(Point editorPoint);

}
