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
package org.jjazz.ui.cl_editor.barrenderer;

import org.jjazz.ui.cl_editor.barrenderer.api.BeatBasedBarRenderer;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRenderer;
import java.awt.*;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.quantizer.Quantizer;
import org.jjazz.quantizer.Quantization;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;

/**
 * This LayoutManager places ItemRenderers at their corresponding beat position, depending on the DisplayQuantization setting.
 * <p>
 * ItemRenderers are set to their preferredSize.
 */
public class BeatBasedLayoutManager implements LayoutManager
{

    private Quantization displayQuantization = Quantization.OFF;
    private static final Logger LOGGER = Logger.getLogger(BeatBasedLayoutManager.class.getSimpleName());

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
            throw new IllegalArgumentException("br=" + br);   //NOI18N
        }
        TimeSignature ts = ((BeatBasedBarRenderer) br).getTimeSignature();
        Rectangle r = br.getDrawingArea();
        xPos = Math.max(r.x, xPos);
        xPos = Math.min(r.x + r.width - 1, xPos);
        float beat = (xPos - r.x) * (ts.getNbNaturalBeats() / (float) r.width);
        Position pos = new Position(br.getBarIndex(), beat);
        return quantize(pos, ts);
    }

    /**
     * Calculate the X position for a beat, in a bar who has width=barWidth.
     *
     * @param beat A float representing the beat position.
     * @param barWidth An integer for the width of the bar.
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
            throw new IllegalArgumentException("parent=" + parent);   //NOI18N
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
            Position pos = quantize(ir.getModel().getPosition(), ts);
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

    public Quantization getDisplayQuantization()
    {
        return displayQuantization;
    }

    public void setDisplayQuantization(Quantization displayQuantization)
    {
        this.displayQuantization = displayQuantization;
    }

    private Position quantize(Position pos, TimeSignature ts)
    {
        Quantizer qr = Quantizer.getInstance();
        qr.setQuantizationValue(this.displayQuantization);
        Position newPos = qr.quantize(pos, ts, pos.getBar());
        return newPos;
    }

}
