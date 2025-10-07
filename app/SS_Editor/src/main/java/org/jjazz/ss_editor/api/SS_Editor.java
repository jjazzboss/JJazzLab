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
package org.jjazz.ss_editor.api;

import org.jjazz.ss_editor.spi.SS_EditorSettings;
import org.jjazz.songstructure.api.SongPartParameter;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JPanel;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerFactory;

/**
 * A SongStructure editor.
 * <p>
 * Its Lookup must contain :<br>
 * - editor's ActionMap<br>
 * - edited SongStructure<br>
 * - edited Song (container of the SongStructure if there is one) <br>
 * - the selected SongParts or RhythmParameters.<br>
 * <p>
 * The editor also uses Song client properties via {@link SS_EditorClientProperties}.
 */
public abstract class SS_Editor extends JPanel implements Lookup.Provider
{

    /**
     * Property change event fired when list of visible RPs change for a given rhythm.
     * <p>
     * oldValue=rhythm, newValue=list of currently visible RPs
     */
    static public final String PROP_RHYHM_VISIBLE_RPS = "PropRhythmVisibleRps";


    abstract public SongStructure getModel();

    /**
     * @return The Song which contains the SongStructure model. Can be null.
     */
    abstract public Song getSongModel();

    /**
     * @return The UndoManager used but this editor.
     */
    abstract public UndoRedo getUndoManager();

    abstract public SS_EditorSettings getSettings();

    abstract public SptViewerFactory getSptViewerRendererFactory();

    abstract public void setController(SS_EditorMouseListener controller);

    /**
     * Clean up everything so component can be garbaged.
     */
    abstract public void cleanup();

    /**
     * Adjust the horizontal zoom factor to try to fit all the SongParts in the specified width.
     *
     * @param width
     */
    abstract public void setZoomHFactorToFitWidth(int width);

    /**
     * Select a songpart.
     *
     * @param spt
     * @param b
     */
    abstract public void selectSongPart(SongPart spt, boolean b);

    /**
     * Select a specific RhythmParameter.
     *
     * @param spt
     * @param rp
     * @param b   True to select, False to unselect.
     */
    abstract public void selectRhythmParameter(SongPart spt, RhythmParameter<?> rp, boolean b);

    /**
     * Set the focus on a specific SongPart.
     *
     * @param spt The SongPart to be focused.
     */
    abstract public void setFocusOnSongPart(SongPart spt);

    /**
     * Set the focus on a specific RhythmParameter.
     *
     * @param spt
     * @param rp
     */
    abstract public void setFocusOnRhythmParameter(SongPart spt, RhythmParameter<?> rp);

    /**
     * Show an insertion mark in the editor for SongPart copy/move operations.
     *
     * @param b        Show/hide the insertion point.
     * @param sptIndex The SongPart index where insertion will be made.
     * @param copyMode If true insertion point is shown for a copy operation, otherwise it's a move operation.
     */
    abstract public void showSptInsertionMark(boolean b, int sptIndex, boolean copyMode);

    /**
     * Show a playback point in the editor at specified position.
     *
     * @param b   Show/hide the playback point.
     * @param pos The position in the SongStructure model.
     */
    abstract public void showPlaybackPoint(boolean b, Position pos);

    /**
     * Return the SongPart/RhythmParameter corresponding to a graphical point.
     * <p>
     * If we're not on a RhythmParameter, only the x coordinate is evaluated to find the right SongPart. If we're on the right of the last SongPart we return
     * the last SongPart (with sptLeft false). If there is no SongPart, return null values for both spt and rp.
     *
     * @param editorPoint
     * @param sptLeft     Used to return an indicator if point was on the left or right side of the songpart
     * @return
     */
    abstract public SongPartParameter getSongPartParameterFromPoint(Point editorPoint, AtomicBoolean sptLeft);

    /**
     * Get the bounds of the component representing the specified SongPart.
     *
     * @param spt
     * @return The bounds in the screen coordinates space.
     */
    abstract public Rectangle getSptViewerRectangle(SongPart spt);

    /**
     * Get the bounds of the component representing the specified RhythmParameter.
     *
     * @param spt
     * @param rp
     * @return The bounds in the screen coordinates space.
     */
    abstract public Rectangle getRpViewerRectangle(SongPart spt, RhythmParameter<?> rp);

    /**
     * Try to scroll so that the specified song part becomes visible.
     *
     * @param spt
     */
    abstract public void makeSptViewerVisible(SongPart spt);

    /**
     * The rhythm parameters effectively displayed in the editor for a specific rhythm.
     * <p>
     *
     * @param r
     * @return A non-empty list.
     * @see SS_EditorClientProperties#setCompactViewModeVisibleRPs(org.jjazz.song.api.Song, org.jjazz.rhythm.api.Rhythm, java.util.List)
     */
    abstract public List<RhythmParameter<?>> getVisibleRps(Rhythm r);

    /**
     * Get the focused SongPart, if any.
     *
     * @param includeFocusedRhythmParameter If true and focus is on a RhythmParameter editor, return the SongPart for this RhythmParameter editor.
     * @return Can be null. The SongPart corresponding to the SongPart editor.
     */
    abstract public SongPart getFocusedSongPart(boolean includeFocusedRhythmParameter);

    /**
     * Clear selection.
     */
    public void unselectAll()
    {
        new SS_Selection(getLookup()).unselectAll(this);
    }
}
