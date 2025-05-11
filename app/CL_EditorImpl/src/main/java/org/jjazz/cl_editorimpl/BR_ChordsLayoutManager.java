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
package org.jjazz.cl_editorimpl;

import org.jjazz.cl_editor.barrenderer.api.BeatBasedBarRenderer;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;

/**
 * The LayoutManager for the BR_Chords BarRenderer : chords and a possible time signature.
 * <p>
 */
class BR_ChordsLayoutManager implements LayoutManager
{

    private static final Logger LOGGER = Logger.getLogger(BR_ChordsLayoutManager.class.getSimpleName());

    /**
     * Calculate the X position for a beat, in a bar who has width=barWidth.
     *
     * @param beat     A float representing the beat position.
     * @param barWidth An integer for the width of the bar.
     * @param ts
     *
     * @return An integer representing the X position of pos.
     */
    public int getBeatXPosition(float beat, int barWidth, TimeSignature ts)
    {
        float nbBeats = (ts == null) ? TimeSignature.FOUR_FOUR.getNbNaturalBeats() : ts.getNbNaturalBeats();
        float beatLength = barWidth / nbBeats;
        return (int) (beat * beatLength);
    }

    /**
     * Return the Position that corresponds to the X-coordinate xPos in the BarRenderer.
     *
     * @param xPos int The x position in the BarRenderer coordinates.
     * @return Position
     */
    public Position getPositionFromPoint(BarRenderer br, int xPos)
    {
        if (!(br instanceof BeatBasedBarRenderer))
        {
            throw new IllegalArgumentException("br=" + br);
        }
        TimeSignature ts = ((BeatBasedBarRenderer) br).getTimeSignature();
        Rectangle r = br.getDrawingArea();
        xPos = Math.max(r.x, xPos);
        xPos = Math.min(r.x + r.width - 1, xPos);
        float beat = (xPos - r.x) * (ts.getNbNaturalBeats() / (float) r.width);
        Position pos = new Position(br.getBarIndex(), beat);
        return pos;
    }

    /**
     * Layout the ItemRenderers: 0 or 1 Section and 0 or more ChordSymbols.
     * <p>
     * Set ItemRenderers size to their preferredsize.
     *
     * @param parent
     */
    @Override
    public void layoutContainer(Container parent)
    {
        if (!(parent instanceof BarRenderer) || !(parent instanceof BeatBasedBarRenderer))
        {
            throw new IllegalArgumentException("parent=" + parent);
        }

        BarRenderer br = (BarRenderer) parent;
        int barWidth = br.getDrawingArea().width;
        int barHeight = br.getDrawingArea().height;
        int barLeft = br.getDrawingArea().x;
        int timeSignatureSpace = 0;
        int barTop = br.getDrawingArea().y;

        TimeSignature ts = ((BeatBasedBarRenderer) br).getTimeSignature();
        List<ItemRenderer> irs = br.getItemRenderers();

        // Set size of each ItemRenderer.
        // Handle presence of sections, and remove them (there might be 2 of them if in the middle of a section drag operation)
        for (ItemRenderer ir : irs.toArray(new ItemRenderer[0]))
        {
            ir.setSize(ir.getPreferredSize());
            if (ir.getModel() instanceof CLI_Section)
            {
                int x = barLeft + timeSignatureSpace;
                // int y = barTop + (barHeight - ir.getHeight()) / 2;       // vertically centered
                int y = barTop + barHeight - ir.getHeight() - 1; // y bottom aligned
                ir.setLocation(x, y);
                timeSignatureSpace = ir.getWidth() + 2;
                irs.remove(ir);
            } else if (ir.getModel() instanceof CLI_BarAnnotation)
            {
                int x = barLeft+barWidth-ir.getWidth()-1;
                int y = barTop;
                ir.setLocation(x, y);
                irs.remove(ir);
            }
        }

        // Sort the ItemRenderers by position, should be only chord symbols
        Collections.sort(irs, new Comparator<ItemRenderer>()
        {
            @Override
            public int compare(ItemRenderer er1, ItemRenderer er2)
            {
                return er1.getModel().getPosition().compareTo(er2.getModel().getPosition());
            }
        });

        // Layout the chords to minimize overlapping
        // First calculate the theorical coordinates
        ArrayList<Rectangle> chordIrsCoordinates = new ArrayList<>();
        int totalChordIrsWidth = 0;
        boolean isOverlapping = false;
        int minX = barWidth;
        for (int i = 0; i < irs.size(); i++)
        {
            ItemRenderer ir = irs.get(i);
            ChordLeadSheetItem<?> item = ir.getModel();
            assert (item instanceof CLI_ChordSymbol) : "item=" + item + " irs=" + irs;
            Position pos = item.getPosition();
            int itemWidth = ir.getWidth();
            int itemHeight = ir.getHeight();
            int x;
            int y;
            // Chords, put it according its beat position
            x = getBeatXPosition(pos.getBeat(), barWidth, ts);
            x -= (itemWidth / 2);
            // Force position so the ItemRenderer remains within the Bar bounds
            x = Math.max(x, timeSignatureSpace);
            x = Math.min(x, barWidth - itemWidth);
            y = barTop + barHeight - itemHeight - 1; // y bottom aligned because chord symbols might have different heights
            // Save the theorical coordinates
            chordIrsCoordinates.add(new Rectangle(x, y, itemWidth, itemHeight));
            // Total width of all item renderers
            totalChordIrsWidth += itemWidth;
            // Overlapping of 2 ItemRenderers
            if ((i > 0) && (isOverlapping == false))
            {
                Rectangle prevEr = chordIrsCoordinates.get(i - 1);
                isOverlapping = x <= ((prevEr.x + prevEr.width) - 1);
            }
            minX = Math.min(x, minX);
        }

        if (totalChordIrsWidth > (barWidth - timeSignatureSpace))
        {
            // All ItemRenderers will overlap

            // Shift everything so 1st er start at x=0
            for (int i = 0; i < chordIrsCoordinates.size(); i++)
            {
                Rectangle coord = chordIrsCoordinates.get(i);
                coord.x -= minX;
            }

            // Then set a fix overlap between all ItemRenderers
            int excessWidth = totalChordIrsWidth - (barWidth - timeSignatureSpace);
            int oneOverlap = (chordIrsCoordinates.size() != 1) ? excessWidth / (chordIrsCoordinates.size() - 1) : 0;
            for (int i = 1; i < chordIrsCoordinates.size(); i++)
            {
                Rectangle coord = chordIrsCoordinates.get(i);
                Rectangle prevCoord = chordIrsCoordinates.get(i - 1);
                coord.x = ((prevCoord.x + prevCoord.width) - 1) - oneOverlap + 1;
            }
        } else if (isOverlapping)
        {
            int robustness = 0;
            int[] shifts = new int[chordIrsCoordinates.size()];
            while (isOverlapping && (robustness < 200))
            {
                // Reset the computed data on each iteration
                for (int i = 0; i < shifts.length; i++)
                {
                    shifts[i] = 0;
                }
                isOverlapping = false;
                // Calculate the shifts to apply to each ItemRenderer
                for (int i = 0; i < (chordIrsCoordinates.size() - 1); i++)
                {
                    Rectangle coord = chordIrsCoordinates.get(i);
                    int c_l = coord.x;
                    int c_r = (coord.x + coord.width) - 1;
                    for (int j = i + 1; j < chordIrsCoordinates.size(); j++)
                    {
                        Rectangle coord2 = chordIrsCoordinates.get(j);
                        int c2_l = coord2.x;
                        int c2_r = (coord2.x + coord2.width) - 1;
                        int overlap = c_r - c2_l + 1;
                        if (overlap > 0)
                        {
                            // One overlap, at least one more iteration will be needed...
                            isOverlapping = true;
                            // There is overlap for these 2 item renderers : calculate the shifts
                            int shift_c = 0;
                            int shift_c2 = 0;
                            // Special case if overlap = 1 and there is room at left and right
                            if ((overlap == 1) && (c_l >= 1 + timeSignatureSpace) && (c2_r < (barWidth - 1)))
                            {
                                // Do like overlap=2 so we are sure BOTH ItemRenderers will move.
                                // This avoids infinite loops.
                                overlap = 2;
                            } // Don't use (overlap/2) to avoid integer rounding problems
                            int room_left = Math.min(c_l - timeSignatureSpace, overlap / 2);
                            int room_right = Math.min(barWidth - c2_r - 1, overlap - (overlap / 2));
                            // Calculate the shifts
                            if (room_left < room_right)
                            {
                                shift_c = room_left;
                                shift_c2 = overlap - shift_c;
                            } else
                            {
                                shift_c2 = room_right;
                                shift_c = overlap - shift_c2;
                            }
                            // Cumulate the shifts with previous ones (with other ItemRenderers)
                            shifts[i] -= shift_c;
                            shifts[j] += shift_c2;
                        } else
                        {
                            /*
                      * System.out.println(" No overlap for " + chordIrs.get(i) + " and " + chordIrs.get(j));
                             */
                        }
                    }
                }

                // Apply the shifts
                if (isOverlapping)
                {
                    for (int i = 0; i < chordIrsCoordinates.size(); i++)
                    {
                        Rectangle coord = chordIrsCoordinates.get(i);
                        coord.x += shifts[i];
                    }
                }
                robustness++;
            }

            if (isOverlapping)
            {
                throw new IllegalStateException("Error robustness=0 irsCoordinates=" + chordIrsCoordinates);
            }
        }

        // Finally set the location with the computed coordinates
        int i = 0;
        for (ItemRenderer er : irs)
        {
            Rectangle coord = chordIrsCoordinates.get(i);
            er.setLocation(coord.x + barLeft, coord.y);
            i++;
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        throw new UnsupportedOperationException("Should not be used!");
    }

    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        throw new UnsupportedOperationException("Should not be used!");
    }

    @Override
    public void addLayoutComponent(String name, Component comp)
    {
        // Nothing
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        // Nothing
    }

}
