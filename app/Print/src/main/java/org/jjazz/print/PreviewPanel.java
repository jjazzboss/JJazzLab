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
package org.jjazz.print;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import javax.swing.JPanel;
import org.jjazz.utilities.api.ResUtil;

/**
 * A panel to render the print preview.
 */
public class PreviewPanel extends JPanel
{

    private Pageable pageable;
    private int pageIndex;

    public void setPageIndex(int index)
    {
        if (index != pageIndex)
        {
            pageIndex = index;
            repaint();
        }
    }

    /**
     * Set the pageable.
     * <p>
     *
     * @param pageable
     * @param pageIndex
     */
    public void setPageable(Pageable pageable, int pageIndex)
    {
        this.pageable = pageable;
        this.pageIndex = Math.min(pageIndex, pageable.getNumberOfPages() - 1);
        revalidate();
        repaint();
    }

    /**
     * Overridden to return an arbitrary dimension BUT with the RIGHT width/height ratio.
     * <p>
     * Our container layoutmanager will size us to use all space while preserving the aspect ratio.
     *
     * @return
     */
    @Override
    public Dimension getPreferredSize()
    {
        PageFormat pageFormat = (pageable == null) ? null : pageable.getPageFormat(pageIndex);
        double widthToHeightRatio = pageFormat == null ? 210d / 297d : pageFormat.getWidth() / pageFormat.getHeight();
        int w = 200;
        int h = (int) Math.floor(w / widthToHeightRatio);
        return new Dimension(w, h);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (pageable == null)
        {
            return;
        }


        PageFormat pageFormat = pageable.getPageFormat(pageIndex);
        Printable printable = pageable.getPrintable(pageIndex);

        int w = getWidth();
        int h = getHeight();
        double scaleFactor = w / pageFormat.getWidth();


        // Draw the preview
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.scale(scaleFactor, scaleFactor);
        try
        {
            printable.print(g2d, pageFormat, pageIndex);
        } catch (IndexOutOfBoundsException e)
        {
            g2d.drawString(ResUtil.getString(getClass(), "PAGE_INDEX_OUT_OF_RANGE"), 10, 30);
        } catch (PrinterException e)
        {
            g2d.drawString(ResUtil.getString(getClass(), "PRINTER_ERROR"), 10, 30);
        }
        g2d.dispose();


        // Add a border
        g2d = (Graphics2D) g;
        g2d.drawRect(0, 0, w - 1, h - 1);

    }

}
