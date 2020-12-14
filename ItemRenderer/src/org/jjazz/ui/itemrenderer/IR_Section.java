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

import java.awt.Color;
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
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.ui.colorsetmanager.api.ColorSetManager;
import org.jjazz.ui.itemrenderer.api.IR_SectionSettings;
import org.jjazz.ui.itemrenderer.api.IR_Copiable;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;
import org.jjazz.ui.itemrenderer.api.ItemRendererSettings;

/**
 * Represent a section's name (upper case).
 */
public class IR_Section extends ItemRenderer implements IR_Copiable
{

    /**
     * Border size between text and edge for rounding section.
     */
    private static final int MARGIN = 2;

    /**
     * Our graphical settings.
     */
    IR_SectionSettings settings;
    /**
     * Copy mode.
     */
    private boolean copyMode;
    /**
     * Our section color manager.
     */
    ColorSetManager colorSetManager;
    /**
     * The string represented by this section.
     */
    String sectionString;
    /**
     * The x/y baseline position to draw the string
     */
    int xString;
    int yString;
    /**
     * The section color.
     */
    Color sectionColor;
    private int zoomFactor = 50;
    private Font zFont;
    private static final Logger LOGGER = Logger.getLogger(IR_Section.class.getName());

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_Section(CLI_Section item, ItemRendererSettings irSettings)
    {
        super(item, IR_Type.Section);
        sectionString = item.getData().getName();

        // Register settings changes
        settings = irSettings.getIR_SectionSettings();
        settings.addPropertyChangeListener(this);

        // Register color manager changes
        colorSetManager = ColorSetManager.getDefault();
        colorSetManager.addPropertyChangeListener(this);

        // Init
        zFont = settings.getFont();
        setFont(zFont);
        setForeground(settings.getColor());
        sectionColor = colorSetManager.getColor(item.getData().getName());
    }

    /**
     * Calculate the preferredSize() depending on font and zoomFactor.
     * <p>
     * Also precalculate some data for paintComponent().
     * <p>
     */
    @Override
    public Dimension getPreferredSize()
    {
        Font f = getFont();
        int zFactor = getZoomFactor();
        Graphics2D g2 = (Graphics2D) getGraphics();
        assert g2 != null : "g2=" + g2 + " sectionString=" + sectionString + " f=" + f + " zFactor=" + zFactor;   //NOI18N

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float factor = 0.5f + (zFactor / 100f);
        float zFontSize = factor * f.getSize2D();
        zFontSize = Math.max(zFontSize, 8);
        zFont = f.deriveFont(zFontSize);

        Insets in = getInsets();
        FontMetrics fm = g2.getFontMetrics(zFont);
        Rectangle2D r = fm.getStringBounds(sectionString, g2);
        int pw = (int) Math.round(r.getWidth()) + 2 * MARGIN + in.left + in.right;
        int ph = (int) Math.round(r.getHeight()) + 2 * MARGIN + in.top + in.bottom;
        xString = (int) Math.round((pw - r.getWidth()) / 2);
        yString = ph - in.bottom - MARGIN - fm.getDescent() + 1;

        g2.dispose();

        Dimension d = new Dimension(pw, ph);
        LOGGER.fine("getPreferredSize() d=" + d);   //NOI18N
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

    @Override
    public void cleanup()
    {
        super.cleanup();
        settings.removePropertyChangeListener(this);
        colorSetManager.removePropertyChangeListener(this);
    }

    @Override
    protected void modelChanged()
    {
        sectionString = ((CLI_Section) getModel()).getData().getName();
        sectionColor = colorSetManager.getColor(sectionString);
        revalidate();
        repaint();
    }

    @Override
    protected void modelMoved()
    {
        // Nothing
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int ARC_SIZE = 5;

        // Draw a bounding box
        g2.setColor(sectionColor);
        g2.fillRoundRect(2, 2, getWidth() - 3, getHeight() - 3, ARC_SIZE, ARC_SIZE);

        // Then draw the string with zoomed font
        g2.setColor(getForeground());
        g2.setFont(zFont);
        g2.drawString(sectionString, xString, yString);

        if (copyMode)
        {
            // Draw the copy indicator in upper right corner
            int size = IR_Copiable.CopyIndicator.getSideLength();
            Graphics2D gg2 = (Graphics2D) g2.create(Math.max(getWidth() - size - 1, 0), 1, size, size);
            IR_Copiable.CopyIndicator.drawCopyIndicator(gg2);
            gg2.dispose();
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
            if (e.getPropertyName() == IR_SectionSettings.PROP_FONT)
            {
                setFont(settings.getFont());
            } else if (e.getPropertyName() == IR_SectionSettings.PROP_FONT_COLOR)
            {
                setForeground(settings.getColor());
            }
        } else if (e.getSource() == colorSetManager)
        {
            if (e.getPropertyName() == ColorSetManager.PROP_REF_COLORS_CHANGED)
            {
                CLI_Section section = (CLI_Section) getModel();
                sectionColor = colorSetManager.getColor(section.getData().getName());
                repaint();
            }
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

    //-------------------------------------------------------------------------------
    // Private functions
    //-------------------------------------------------------------------------------
}
