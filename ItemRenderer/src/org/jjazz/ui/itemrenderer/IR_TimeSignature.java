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
package org.jjazz.ui.itemrenderer;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.ui.itemrenderer.api.*;

public class IR_TimeSignature extends ItemRenderer implements IR_Copiable
{

    /**
     * Start X point for the upper part.
     */
    private int upperX;
    /**
     * Start Y point for the upper part.
     */
    private int upperY;
    /**
     * Start X point for the lower part.
     */
    private int lowerX;
    /**
     * Start Y point for the lower part.
     */
    private int lowerY;
    /**
     * The string for the upper part of the TimeSignature.
     */
    private String upperString;
    /**
     * The string for the lower part of the TimeSignature.
     */
    private String lowerString;
    /**
     * Our graphical settings.
     */
    IR_TimeSignatureSettings settings;
    /**
     * Copy mode.
     */
    private boolean copyMode;
    /**
     * Save the time signature between updates.
     */
    TimeSignature timeSignature;
    private int zoomFactor = 50;
    private Font zFont;
    private static final Logger LOGGER = Logger.getLogger(IR_TimeSignature.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_TimeSignature(CLI_Section item)
    {
        super(item, IR_Type.TimeSignature);

        timeSignature = item.getData().getTimeSignature();
        upperString = String.valueOf(timeSignature.getUpper());
        lowerString = String.valueOf(timeSignature.getLower());

        // Register settings changes
        settings = IR_TimeSignatureSettings.getDefault();
        settings.addPropertyChangeListener(this);

        // Init
        zFont = settings.getFont();
        setFont(zFont);
        setForeground(settings.getColor());
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        settings.removePropertyChangeListener(this);
    }

    @Override
    protected void modelChanged()
    {
        timeSignature = ((CLI_Section) getModel()).getData().getTimeSignature();
        upperString = String.valueOf(timeSignature.getUpper());
        lowerString = String.valueOf(timeSignature.getLower());
        revalidate();
        repaint();
    }

    @Override
    protected void modelMoved()
    {
        // Nothing
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
     * Calculate the pref size depending on string, font and zoom.
     * <p>
     * Also precalculate some data to speed up paintComponent().
     *
     * @return
     */
    @Override
    public Dimension getPreferredSize()
    {
        final int H_MARGIN = 2;
        final int V_MARGIN = 1;
        final int V_GAP = 0; // 0 because font height seems overestimated always
        Font f = getFont();
        int zFactor = getZoomFactor();
        Graphics2D g2 = (Graphics2D) getGraphics();
        assert g2 != null : "g2=" + g2 + " timeSignature=" + timeSignature + " f=" + f + " zFactor=" + zFactor;

        Insets in = getInsets();

        float factor = 0.5f + (zFactor / 100f);
        float zFontSize = factor * getFont().getSize2D();
        zFontSize = Math.max(zFontSize, 9);
        zFont = getFont().deriveFont(zFontSize);

        // Calculate bounds and strings position
        FontMetrics fm = g2.getFontMetrics(zFont);

        // Width and X
        Rectangle2D rUpper = fm.getStringBounds(upperString, g2);
        Rectangle2D rLower = fm.getStringBounds(lowerString, g2);
        // LOGGER.fine("getPreferredSize() rUpper=" + rUpper + " rLower=" + rLower);
        int upperWidth = (int) Math.round(rUpper.getWidth());
        int lowerWidth = (int) Math.round(rLower.getWidth());
        int pw = Math.max(upperWidth, lowerWidth) + 2 * H_MARGIN + in.left + in.right;
        upperX = Math.round(pw / 2f - upperWidth / 2f);
        lowerX = Math.round(pw / 2f - lowerWidth / 2f);

        // Height and Y
        int upperHeight = (int) Math.ceil(rUpper.getHeight()) - 4;
        int lowerHeight = (int) Math.ceil(rLower.getHeight()) - 4;
        int ph = upperHeight + V_GAP + lowerHeight + 2 * V_MARGIN + in.top + in.bottom;
        upperY = in.top + V_MARGIN - (int) Math.round(rUpper.getY());          // getY() must be a negative value (relative to baseline)
        lowerY = in.top + V_MARGIN + upperHeight - (int) Math.round(rLower.getY());   // getY() must be a negative value (relative to baseline)

        Dimension d = new Dimension(pw, ph);
        LOGGER.fine("getPreferredSize() d=" + d);
        return d;
    }

    /**
     * Render the event.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(zFont);
        g2.drawString(upperString, upperX, upperY);
        g2.drawString(lowerString, lowerX, lowerY);

        if (copyMode)
        {
            // Draw the copy indicator in upper right corner
            int size = IR_Copiable.CopyIndicator.getSideLength();
            Graphics2D gg2 = (Graphics2D) g2.create(Math.max(getWidth() - size - 1, 0), 1, size, size);
            IR_Copiable.CopyIndicator.drawCopyIndicator(gg2);
            gg2.dispose();
        }
    }

    //-------------------------------------------------------------------------------
    // IR_Copiable interface
    //-------------------------------------------------------------------------------
    @Override
    public void showCopyMode(boolean b)
    {
        if (copyMode != b)
        {
            copyMode = b;
            repaint();
        }
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
            if (e.getPropertyName() == IR_TimeSignatureSettings.PROP_FONT)
            {
                setFont(settings.getFont());
            } else if (e.getPropertyName() == IR_TimeSignatureSettings.PROP_FONT_COLOR)
            {
                setForeground(settings.getColor());
            }
        }
    }

    //-------------------------------------------------------------------------------
    // Private functions
    //-------------------------------------------------------------------------------
}
