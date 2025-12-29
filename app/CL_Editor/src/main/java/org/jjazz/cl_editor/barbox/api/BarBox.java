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
package org.jjazz.cl_editor.barbox.api;

import java.awt.Point;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.api.DisplayTransposableRenderer;

/**
 * This object groups several BarRenderers in a "stack view" that represent one bar.
 */
public abstract class BarBox extends JPanel implements DisplayTransposableRenderer
{

    private static final Logger LOGGER = Logger.getLogger(BarBox.class.getSimpleName());

    /**
     * Set the model for this BarBox.
     *
     * @param modelBarIndex If &lt; 0, it means this BarBox does not represent a valid bar for model.
     * @param clsModel
     */
    abstract public void setModel(int modelBarIndex, ChordLeadSheet clsModel);

    abstract public ChordLeadSheet getClsModel();

    /**
     * The bar index in the chordleadsheet model.
     *
     * @return -1 if BarBox is past the end of chord leadsheet.
     */
    abstract public int getModelBarIndex();

    /**
     * Set the barIndex within the ChordLeadSheet model.
     *
     * @param bar If &lt; 0, it means information from model is not available (typically barIndex is past the end of the ChordLeadSheet model)
     * @throws IllegalArgumentException If bar is &gt; or equals to model's size.
     */
    abstract public void setModelBarIndex(int bar);


    abstract public int getBarIndex();

    /**
     * Set the barIndex of this BarBox.
     * <p>
     * Might be different from the model barIndex if e.g. we're past the end of the ChordLeadSheet model.
     *
     * @param bar A zero or positive value
     */
    abstract public void setBarIndex(int bar);

    /**
     * Add an item in the BarBox.
     * <p>
     * The operation requests each BarRenderer to create ItemRenderers if appropriate.
     *
     * @param item
     * @return List The created ItemRenderers.
     */
    abstract public List<ItemRenderer> addItem(ChordLeadSheetItem<?> item);

    /**
     * Remove an item from the BarBox.
     * <p>
     * The operation requests each BarRenderer to remove the ItemRenderer if appropriate.
     *
     * @param item
     * @return List The removed ItemRenderers. Can be an empty list.
     */
    abstract public List<ItemRenderer> removeItem(ChordLeadSheetItem<?> item);


    /**
     * Set the focus on an ItemRenderer for item.
     *
     * @param item
     * @param irType The irType to search for. If null set focus on the first ItemRenderer found.
     */
    abstract public void setFocusOnItem(ChordLeadSheetItem<?> item, IR_Type irType);

    /**
     * The operation requests each BarRenderer to select the ItemRenderers of item.
     *
     * @param item
     * @param b
     */
    abstract public void selectItem(ChordLeadSheetItem<?> item, boolean b);

    abstract public boolean isSelected();

    /**
     * Set the Component selected or not, forward to BarRenderers as well.
     *
     * @param b
     */
    abstract public void setSelected(boolean b);

    /**
     * Vertical zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    abstract public void setZoomVFactor(int factor);

    abstract public int getZoomVFactor();


    /**
     * Return the position (bar, beat) which corresponds to a given point in the BarBox.
     *
     * @param barboxPoint A point in the BarBox coordinates.
     * @return Null if point does not correspond to a valid bar.
     */
    abstract public Position getPositionFromPoint(Point barboxPoint);

    /**
     * Request BarRenderers to update after an item has moved while remaining in this bar.
     *
     * @param item
     */
    abstract public void moveItem(ChordLeadSheetItem<?> item);

    /**
     * Get the section this BarBox belongs to.
     *
     * @return Can be null if modelBarIndex==-1
     */
    abstract public CLI_Section getSection();

    /**
     * Request BarRenderers to update if the section they belong to has changed.
     *
     * @param section
     */
    abstract public void setSection(CLI_Section section);

    /**
     * Get the bar renderers configuration.
     *
     * @return
     */
    abstract public BarBoxConfig getConfig();

    /**
     * Set the BarBoxConfig of this BarBox.
     * <p>
     * There must be at least 1 active BarRenderer.
     *
     * @param bbConfig
     * @return boolean true if BarBoxConfig has been really changed, false otherwise (e.g. same value)
     */
    abstract public boolean setConfig(BarBoxConfig bbConfig);

    /**
     * Show the insertion point for a drag operation.
     *
     * @param showIP
     * @param item
     * @param pos
     * @param copyMode
     */
    abstract public void showInsertionPoint(boolean showIP, ChordLeadSheetItem<?> item, Position pos, boolean copyMode);

    /**
     * Change background to represent the playback point in this bar. Then delegate to BarRenderers to render the point.
     *
     * @param b   Show if true, hide if false
     * @param pos Used if b is true
     */
    abstract public void showPlaybackPoint(boolean b, Position pos);

    /**
     * Clean up everything so this object can be garbaged.
     */
    abstract public void cleanup();

    /**
     * The bar renderers that make this BarBox.
     *
     * @return
     */
    abstract public java.util.List<BarRenderer> getBarRenderers();

}
