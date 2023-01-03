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
import java.util.List;
import javax.swing.JPanel;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.PianoRollEditorImpl;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;

/**
 * A piano roll editor of a SizedPhrase.
 * <p>
 * Its Lookup must contain :<br>
 * - editor's ActionMap<br>
 * - edited SizedPhrase<br>
 * - edited Song (if there is one) <br>
 * - the currently selected NoteViews<br>
 */
public abstract class PianoRollEditor extends JPanel implements Lookup.Provider
{

    /**
     * oldValue=old tool, newValue=new tool
     */
    public static final String PROP_ACTIVE_TOOL = "ActiveTool";
    /**
     * newValue=boolean
     */
    public static final String PROP_SNAP_ENABLED = "SnapEnabled";
    /**
     * oldValue=old quantization value, newValue=new quantization value
     */
    public static final String PROP_QUANTIZATION = "Quantization";
    /**
     * newValue=boolean
     */
    public static final String PROP_PLAYBACK_POINT_POSITION = "PlaybackPointVisible";

    public static PianoRollEditor getDefault(int startBarIndex, SizedPhrase sp, DrumKit.KeyMap keymap, PianoRollEditorSettings settings)
    {
        return new PianoRollEditorImpl(startBarIndex, sp, keymap, settings);
    }

    abstract public int getStartBarIndex();

    abstract public SizedPhrase getModel();


    /**
     * The drum key map used by the edited phrase.
     *
     * @return Null if it's a melodic phrase.
     */
    abstract public DrumKit.KeyMap getDrumKeyMap();

    /**
     * Convenience method which returns true if getDrumKeyMap() is non null.
     *
     * @return
     */
    public boolean isDrumEdit()
    {
        return getDrumKeyMap() != null;
    }


    /**
     * @return The UndoManager used but this editor.
     */
    abstract public JJazzUndoManager getUndoManager();

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
     * <p>
     * Fire a PROP_QUANTIZATION change event.
     *
     * @param q Accepted values are BEAT, HALF_BEAT, ONE_THIRD_BEAT, ONE_QUARTER_BEAT.
     */
    abstract public void setQuantization(Quantization q);

    /**
     * Get the display quantization.
     *
     * @return Can't be null
     */
    abstract public Quantization getQuantization();

    /**
     * Enable or disable the snap to quantization feature.
     * <p>
     * Fire a PROP_SNAP_ENABLED change event.
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
     * Get the NoteView associated to the specified NoteEvent.
     *
     * @param ne
     * @return Can be null
     */
    abstract public NoteView getNoteView(NoteEvent ne);

    /**
     * Set the active EditTool.
     * <p>
     * Fire a PROP_ACTIVE_TOOL change event.
     *
     * @param tool
     */
    abstract public void setActiveTool(EditTool tool);

    /**
     * Get the actived EditTool.
     *
     * @return Can't be null
     */
    abstract public EditTool getActiveTool();


    /**
     * Show (or hide) a playback point in the editor at specified position.
     * <p>
     * If pos is &lt; 0 or out of the editor bounds, nothing is shown. Fire a PROP_PLAYBACK_POINT_POSITION change event.
     *
     * @param pos The position in beats.
     */
    abstract public void showPlaybackPoint(float pos);

    /**
     * Get the playback point position.
     *
     * @return If &lt; 0 no playback point is shown.
     */
    abstract public float getPlaybackPointPosition();

    /**
     * Return the position in beats that correspond to a graphical point in the editor.
     * <p>
     *
     * @param editorPoint A point in the editor's coordinates. -1 if point is not valid.
     * @return
     *
     */
    abstract public float getPositionFromPoint(Point editorPoint);

    /**
     * Return the pitch that correspond to a graphical point in the editor.
     *
     * @param editorPoint A point in the editor's coordinates. -1 if point is not valid.
     * @return
     */
    abstract public int getPitchFromPoint(Point editorPoint);

    /**
     * Get the currently selected NoteViews.
     *
     * @return An immutable list.
     */
    public List<NoteView> getSelectedNoteViews()
    {
        return new NotesSelection(getLookup()).getNoteViews();
    }

    /**
     * Get the currently selected notes.
     *
     * @return An immutable list.
     * @see #getSelectedNoteViews()
     */
    public List<NoteEvent> getSelectedNotes()
    {
        return new NotesSelection(getLookup()).getNotes();
    }

    /**
     * Unselect all notes.
     */
    public void unselectAll()
    {
        new NotesSelection(getLookup()).unselectAll();
    }
}
