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
package org.jjazz.cl_editor.barrenderer.api;

import java.awt.*;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;

/**
 * This LayoutManager places ItemRenderers at their corresponding beat position.
 * <p>
 * ItemRenderers are set to their preferredSize.
 */
public class BeatBasedLayoutManager implements LayoutManager
{

    private static final Logger LOGGER = Logger.getLogger(BeatBasedLayoutManager.class.getSimpleName());

    /**
     * Return the Position that corresponds to the X-coordinate xPos in the BarRenderer.
     *
     * @param br
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
     * Calculate the X position for a beat, in a bar who has width=barWidth.
     *
     * @param beat A float representing the beat position.
     * @param barWidth An integer for the width of the bar.
     * @param ts
     *
     * @return An integer representing the X position of pos.
     */
    public int getBeatXPosition(float beat, int barWidth, TimeSignature ts)
    {
        if (ts == null)
        {
            // Use default timesignature if leadsheet was empty.
            ts = TimeSignature.FOUR_FOUR;
        }
        float nbBeats = ts.getNbNaturalBeats();
        float beatLength = barWidth / nbBeats;
        return Math.round(beat * beatLength);
    }

    /**
     * Layout all children at their respective beat position with their preferredSize.
     *
     * @param parent Container
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
        int barTop = br.getDrawingArea().y;
        TimeSignature ts = ((BeatBasedBarRenderer) parent).getTimeSignature();

        for (ItemRenderer ir : br.getItemRenderers())
        {
            ir.setSize(ir.getPreferredSize());
            Position pos = ir.getModel().getPosition();
            int irWidth = ir.getWidth();
            int irHeight = ir.getHeight();
            int x = getBeatXPosition(pos.getBeat(), barWidth, ts);
            x += (barLeft - Math.round(irWidth / 2));
            int y = barTop + Math.round(barHeight - irHeight) / 2;
            ir.setLocation(x, y);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        throw new UnsupportedOperationException();
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
