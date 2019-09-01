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
package org.jjazz.ui.cl_editor.api;

import org.jjazz.quantizer.Quantization;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.JPanel;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.song.api.Song;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;

/**
 * A ChordLeadSheet editor.
 * <p>
 * Editor's lookup contains :<br>
 * - editor's ActionMap<br>
 * - editor's JJazzUndoManager<br>
 * - edited ChordLeadSheet<br>
 * - edited Song (container of the ChordLeadSheet if there is one)<br>
 * - selected items or bars<br>
 */
public abstract class CL_Editor extends JPanel implements Lookup.Provider
{

    /**
     * Section's display quantization.
     * <p>
     * Property change: oldValue=section, newValue=quantization
     */
    public static final String PROP_SECTION_DISPLAY_QUANTIZATION = "SectionDisplayQuantization";
    /**
     * Section's display quantization.
     * <p>
     * Property change: oldValue=section, newValue=true/false
     */
    public static final String PROP_SECTION_START_ON_NEW_LINE = "SectionStartOnNewLine";

    abstract public void setEditorMouseListener(CL_EditorMouseListener brm);

    /**
     * @return The ChordLeadSheet model for this CL_Editor.
     */
    abstract public ChordLeadSheet getModel();

    /**
     * @return The Song which contains the ChordLeadSheet. Can be null.
     */
    abstract public Song getSongModel();

    /**
     * @return The UndoManager used but this editor.
     */
    abstract public UndoRedo getUndoManager();

    /**
     * Set how chords positions are quantized for display.
     *
     * @param q
     * @param section If null, set the quantization display for all sections.
     */
    abstract public void setDisplayQuantizationValue(CLI_Section section, Quantization q);

    /**
     * @param section
     * @return The quantization used to display the specified section.
     */
    abstract public Quantization getDisplayQuantizationValue(CLI_Section section);

    /**
     * Set if the bar corresponding to specified section should start on a new line.
     *
     * @param section
     * @param b
     */
    abstract public void setSectionStartOnNewLine(CLI_Section section, boolean b);

    /**
     * @param section
     * @return True if section bar should start on new line.
     */
    abstract public boolean isSectionStartOnNewLine(CLI_Section section);

    /**
     * Set the number of columns per line.
     *
     * @param nbCols A value between 1 and 16.
     */
    abstract public void setNbColumns(int nbCols);

    abstract public int getNbColumns();

    abstract public int getNbBarBoxes();

    /**
     * Clean up everything so component can be garbaged.
     */
    abstract public void cleanup();

    /**
     * Return the Position that correspond to a graphical point in the editor.
     * <p>
     * If point is on a model bar try to retrieve the beat value. If point is on a non-model bar (eg past end), return only the
     * bar, beat is set to 0. If point is somewhere else return null.
     *
     * @param editorPoint A point in the editor's coordinates.
     * @return Null if point does not correspond to a barbox
     *
     */
    abstract public Position getPositionFromPoint(Point editorPoint);

    /**
     * Make sure a specific bar is visible.
     *
     * @param barIndex int The index of the bar to be made visible
     */
    abstract public void makeBarVisible(int barIndex);

    /**
     * Select the bars in the specified barIndex range.
     *
     * @param barIndexFrom int
     * @param barIndexTo int
     * @param b True to select, False to unselect.
     */
    abstract public void selectBars(int barIndexFrom, int barIndexTo, boolean b);

    /**
     * Select the bars out of the specified barIndex range.
     *
     * @param barIndexFrom int
     * @param barIndexTo int
     * @param b True to select, False to unselect.
     */
    abstract public void selectBarsExcept(int barIndexFrom, int barIndexTo, boolean b);

    /**
     * Select the ItemRenderer(s) whose model is item.
     *
     * @param item
     * @param b True to select, False to unselect.
     */
    abstract public void selectItem(ChordLeadSheetItem<?> item, boolean b);

    /**
     * Replace the current selection with the specified items.
     *
     * @param items
     * @param b True to select, False to unselect.
     */
    abstract public void selectItems(List<ChordLeadSheetItem<?>> items, boolean b);

    /**
     * Set the focus on one bar in the editor.
     *
     * @param barIndex int The index of the bar.
     */
    abstract public void setFocusOnBar(int barIndex);

    /**
     * Set the focus on an ItemRenderer whose model is item.
     *
     * @param item
     * @param irClass the type of ItemRenderer to focus if there is multiple ItemRenderers for one item. If null, focus on the
     * first ItemRenderer found.
     */
    abstract public void setFocusOnItem(ChordLeadSheetItem<?> item, IR_Type irClass);

    /**
     * Show an insertion point in the editor for copy/move operations.
     *
     * @param b Show/hide the insertion point. If false other arguments are not used.
     * @param item The item for which we show the insertion point for.
     * @param pos The position of the insertion point.
     * @param copyMode If true insertion point is shown for a copy operation, otherwise it's a move operation.
     */
    abstract public void showInsertionPoint(boolean b, ChordLeadSheetItem<?> item, Position pos, boolean copyMode);

    /**
     * Show a playback point in the editor at specified position.
     *
     * @param b Show/hide the playback point.
     * @param pos
     */
    abstract public void showPlaybackPoint(boolean b, Position pos);

    /**
     *
     * @param barIndex
     * @return A Rectangle in the screen coordinates space.
     */
    abstract public Rectangle getBarRectangle(int barIndex);

}
