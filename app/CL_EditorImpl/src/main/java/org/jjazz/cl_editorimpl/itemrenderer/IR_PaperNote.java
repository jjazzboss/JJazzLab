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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.cl_editor.itemrenderer.api.IR_Copiable;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererSettings;

/**
 * Represents a Bar annotation as a paper note image.
 */
public class IR_PaperNote extends ItemRenderer implements IR_Copiable
{
    private static final Icon ICON = new ImageIcon(IR_PaperNote.class.getResource("resources/PaperNoteIcon.png"));
    private boolean copyMode;
    private int zoomFactor = 50;
    private static final Logger LOGGER = Logger.getLogger(IR_PaperNote.class.getName());

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_PaperNote(CLI_BarAnnotation item, ItemRendererSettings irSettings)
    {
        super(item, IR_Type.BarAnnotationPaperNote);
        setToolTipText(item.getData());
    }


    /**
     * Calculate the preferredSize() depending on zoomFactor.
     * <p>
     * Also precalculate some data for paintComponent().
     * <p>
     */
    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(ICON.getIconWidth(), ICON.getIconHeight());
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
    }

    @Override
    protected void modelChanged()
    {
        setToolTipText((String) getModel().getData());
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ICON.paintIcon(this, g2, 0, 0);


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

    //-------------------------------------------------------------------------------
    // Private functions
    //-------------------------------------------------------------------------------
}
