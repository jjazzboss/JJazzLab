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

import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.IR_TimeSignatureSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_Copiable;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererSettings;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.beans.PropertyChangeEvent;
import java.text.AttributedString;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.uiutilities.api.TextLayoutUtils;

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
    private AttributedString attrStrUpper;
    private AttributedString attrStrLower;
    TimeSignature timeSignature;
    private int zoomFactor = 50;
    private static final Logger LOGGER = Logger.getLogger(IR_TimeSignature.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_TimeSignature(CLI_Section item, ItemRendererSettings irSettings)
    {
        super(item, IR_Type.TimeSignature);

        timeSignature = item.getData().getTimeSignature();
        upperString = String.valueOf(timeSignature.getUpper());
        lowerString = String.valueOf(timeSignature.getLower());

        // Register settings changes
        settings = irSettings.getIR_TimeSignatureSettings();
        settings.addPropertyChangeListener(this);

        // Init
        setFont(settings.getFont());
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
        final int PADDING = 1;
        final int MIDDLE_HGAP = 2;

        Insets in = getInsets();
        Font font = getFont();
        Graphics2D g2 = (Graphics2D) getGraphics();
        assert g2 != null;   
        FontRenderContext frc = g2.getFontRenderContext();

        // Font size depends on zoom factor
        float factor = 0.5f + (getZoomFactor() / 100f);
        float zFontSize = factor * getFont().getSize2D();
        zFontSize = Math.max(zFontSize, 9);

        // Create the AttributedStrings
        attrStrLower = new AttributedString(lowerString, font.getAttributes());
        attrStrLower.addAttribute(TextAttribute.SIZE, zFontSize);   // Override size attribute
        // attrStrLower.addAttribute(TextAttribute.FAMILY, font.getFontName());
        attrStrUpper = new AttributedString(upperString, font.getAttributes());
        attrStrUpper.addAttribute(TextAttribute.SIZE, zFontSize);   // Override size attribute
        // attrStrUpper.addAttribute(TextAttribute.FAMILY, font.getFontName());

        // Create the TextLayout to get its dimension       
        TextLayout textLayoutUpper = new TextLayout(attrStrUpper.getIterator(), frc);
        TextLayout textLayoutLower = new TextLayout(attrStrLower.getIterator(), frc);
        int wUpper = (int) TextLayoutUtils.getWidth(textLayoutUpper, upperString, true);
        int wLower = (int) TextLayoutUtils.getWidth(textLayoutLower, lowerString, true);
        int hUpper = TextLayoutUtils.getHeight(textLayoutUpper, frc);
        int hLower = TextLayoutUtils.getHeight(textLayoutLower, frc);

        // Set preferred size
        int maxWidth = Math.max(wUpper, wLower);
        int pw = in.left + PADDING + maxWidth + PADDING + in.right;
        int ph = in.top + PADDING + hUpper + MIDDLE_HGAP + hLower + PADDING + in.bottom;

        // Also set the position
        upperX = in.left + PADDING;
        lowerX = upperX;

        lowerY = ph - 1 - in.bottom - PADDING + 1; //  - (int) Math.ceil(textLayoutLower.getDescent());
        upperY = in.top + PADDING + hUpper;

        Dimension d = new Dimension(pw, ph);
        LOGGER.log(Level.FINE, "getPreferredSize() d={0}", d);   
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the upper and lower string
        g2.drawString(attrStrUpper.getIterator(), upperX, upperY);
        g2.drawString(this.attrStrLower.getIterator(), lowerX, lowerY);

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
            if (e.getPropertyName().equals(IR_TimeSignatureSettings.PROP_FONT))
            {
                setFont(settings.getFont());
            } else if (e.getPropertyName().equals(IR_TimeSignatureSettings.PROP_FONT_COLOR))
            {
                setForeground(settings.getColor());
            }
        }
    }

    //-------------------------------------------------------------------------------
    // Private functions
    //-------------------------------------------------------------------------------
}
