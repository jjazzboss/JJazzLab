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
package org.jjazz.cl_editorimpl.itemrenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererSettings;

/**
 * Show the position of a chord.
 * <p>
 * Shape depends on the chord rendering options.
 */
public class IR_ChordPosition extends ItemRenderer
{

    private static final int H_MARGIN = 1;
    private static final int V_MARGIN = 0;
    private static final Dimension DEFAULT_DRAWING_AREA = new Dimension(11, 6);
    private IR_ChordSymbolSettings settings;
    private ChordRenderingInfo cri;
    private int zoomFactor = 50;

    public IR_ChordPosition(CLI_ChordSymbol item, ItemRendererSettings irSettings)
    {
        super(item, IR_Type.ChordPosition);
        cri = item.getData().getRenderingInfo();

        // Reuse same font color than chords
        settings = irSettings.getIR_ChordSymbolSettings();
        settings.addPropertyChangeListener(this);
        getModel().getClientProperties().addPropertyChangeListener(this);
        setForeground(getColor());

        updateToolTipText();
    }

    @Override
    protected void modelChanged()
    {
        cri = ((CLI_ChordSymbol) getModel()).getData().getRenderingInfo();        
        updateToolTipText();
        repaint();
    }

    @Override
    protected void modelMoved()
    {
        updateToolTipText();
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        getModel().getClientProperties().removePropertyChangeListener(this);
        settings.removePropertyChangeListener(this);
    }

    /**
     * Calculate the preferredSize() depending on the zoomFactor.
     * <p>
     */
    @Override
    public Dimension getPreferredSize()
    {
        int zFactor = getZoomFactor();
        float factor = 0.8f + (.4f * zFactor / 100f);

        Insets in = getInsets();


        int pw = Math.round(DEFAULT_DRAWING_AREA.width * factor);
        if (pw % 2 == 0)
        {
            // Make sure drawing area is an odd number
            pw++;
        }
        pw += 2 * H_MARGIN + in.left + in.right;
        int ph = Math.round(DEFAULT_DRAWING_AREA.height * factor);
        ph += 2 * V_MARGIN + in.top + in.bottom;


        Dimension d = new Dimension(pw, ph);
        return d;
    }

    /**
     * Zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    @Override
    public void setZoomFactor(int factor)
    {
        zoomFactor = factor;
        revalidate();
        repaint();
    }

    @Override
    public int getZoomFactor()
    {
        return zoomFactor;
    }

    /**
     * Render the event.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Insets in = getInsets();

        int x0 = in.left + H_MARGIN;
        int y0 = in.top + H_MARGIN;
        int x1 = getWidth() - in.right - H_MARGIN - 1;
        int y1 = getHeight() - in.bottom - V_MARGIN - 1;

        int w = getWidth() - in.left - in.right - 2 * H_MARGIN;
        int xHalf = in.left + H_MARGIN + Math.round(w / 2f);
        int h = getHeight() - in.top - in.bottom - 2 * V_MARGIN;
        int yHalf = in.top + V_MARGIN + Math.round(h / 2f);

        Shape shape = null;

        if (cri.hasOneFeature(Feature.HOLD))
        {
            // Heavy rectangle
            int holdHeight = Math.min(h, 4);
            shape = new Rectangle2D.Float(x0, yHalf - holdHeight / 2, w, holdHeight);
        } else if (cri.hasOneFeature(Feature.SHOT))
        {
            // A point
            int d = Math.min(w, h) - 1;
            shape = new Ellipse2D.Float(xHalf - (d / 2), yHalf - (d / 2), d, d);
        } else if (cri.getAccentFeature() != null)      // Accent or stronger accent
        {
            // A thin rectangle
            int accentHeight = Math.min(h, 2);
            shape = new Rectangle2D.Float(x0, yHalf - accentHeight / 2, w, accentHeight);
        } else      // NORMAL
        {
            // Inversed triangle
            Path2D p = new Path2D.Float();
            int side = 4;
            if (zoomFactor > 75)
            {
                side = 5;
            } else if (zoomFactor < 25)
            {
                side = 3;
            }
            p.moveTo(xHalf, y1);
            p.lineTo(xHalf - side, y1 - side);
            p.lineTo(xHalf + side, y1 - side);
            p.lineTo(xHalf, y1);
            p.closePath();
            shape = p;
        }


        g2.fill(shape);

    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        super.propertyChange(e);
        if (e.getSource() == settings)
        {
            if (e.getPropertyName().equals(IR_ChordSymbolSettings.PROP_DEFAULT_FONT_COLOR))
            {
                setForeground(getColor());
            }
        } else if (e.getSource() == getModel().getClientProperties())
        {
            if (e.getPropertyName().equals(CL_EditorClientProperties.PROP_CHORD_USER_FONT_COLOR))
            {
                setForeground(getColor());
            }
        }
    }

    private void updateToolTipText()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("beat=").append(getModel().getPosition().getBeatAsUserString());
        String criStr = cri.toUserString();
        if (!criStr.isBlank())
        {
            sb.append(" ").append(criStr);
        }
        setToolTipText(sb.toString());
    }

    private Color getColor()
    {
        Color c = getModel().getClientProperties().getColor(CL_EditorClientProperties.PROP_CHORD_USER_FONT_COLOR, settings.getColor());
        return c;
    }

}
