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
package org.jjazz.print;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.util.logging.Logger;
import org.jjazz.song.api.Song;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.songeditormanager.SongEditorManager;

/**
 * A printer for Song editors which fits available width and breaks pages at a BarBox edge.
 */
public class SongPrinter implements Printable, Pageable
{

    private static final int HEADER_HEIGHT_PTS = 40;
    private static final int FOOTER_HEIGHT_PTS = 20;
    private final Song song;
    private final CL_Editor clEditor;
    double xMin;
    double yMin;
    double height;
    int heightInt;
    double width;
    int widthInt;
    double hScaleFactor;
    double vScaleFactor;
    double scaledEditorHeight;
    private int editorBarRowHeight;
    private int editorWidth;
    private int editorHeight;
    private int nbPages;
    private MessageFormat headerMsg;
    private MessageFormat footerMsg;
    private final PageFormat pageFormat;
    private final Font font;
    private static final Logger LOGGER = Logger.getLogger(SongPrinter.class.getSimpleName());

    public SongPrinter(Song song, PageFormat pageFormat)
    {
        this.song = song;
        this.pageFormat = pageFormat;

        // Get the LeadSheet Editor
        var res = SongEditorManager.getInstance().getEditors(song);
        assert res != null : "song=" + song;
        clEditor = res.getTcCle().getCL_Editor();

        preComputeData(clEditor, pageFormat);


        font = new Font("Helvetica", Font.PLAIN, 11);
        assert font.getSize2D() <= HEADER_HEIGHT_PTS * .8f : "font=" + font;
        assert font.getSize2D() <= FOOTER_HEIGHT_PTS * .8f : "font=" + font;

        setHeaderMessage(new MessageFormat(song.getName()));
        setFooterMessage(new MessageFormat("{0} / " + getNumberOfPages()));

    }

    /**
     * Set header message.
     * <p>
     * {0} is replaced by pageIndex.
     *
     * @param msg Example new MessageFormat("Page {0}/12"). Can be null.
     */
    public final void setHeaderMessage(MessageFormat msg)
    {
        headerMsg = msg;
    }

    /**
     * Set footer message.
     * <p>
     * {0} is replaced by pageIndex.
     *
     * @param msg Example new MessageFormat("Page {0}/12");
     */
    public final void setFooterMessage(MessageFormat msg)
    {
        footerMsg = msg;
    }

    // =============================================================================
    // Printable implementation
    // =============================================================================
    @Override
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException
    {
        LOGGER.fine("print() -- pageIndex=" + pageIndex + " pageFormat.getImageableWidth()=" + pageFormat.getImageableWidth()
                + " pageFormat.getImageableHeight()=" + pageFormat.getImageableHeight());

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);


        // Y offset corresponding to pageIndex
        double yOffset = pageIndex * height;
        LOGGER.fine("print() yOffset=" + yOffset + " (scaleFactor=" + hScaleFactor + " scaledEditorHeight=" + scaledEditorHeight + " height=" + height + ")");


        // Set position on upper left corner of Imageable area
        g2d.translate(xMin, yMin);


        // Header & footer
        if (footerMsg != null || headerMsg != null)
        {
            Object[] args =
            {
                pageIndex + 1
            };
            FontMetrics fm = g2d.getFontMetrics(font);


            if (headerMsg != null)
            {
                String s = headerMsg.format(args);
                Rectangle2D r = fm.getStringBounds(s, g2d);
                g2d.setFont(font);
                g2d.drawString(s, (float) (width / 2 - r.getWidth() / 2), (float) (HEADER_HEIGHT_PTS / 2));
            }


            if (footerMsg != null)
            {
                String s = footerMsg.format(args);
                Rectangle2D r = fm.getStringBounds(s, g2d);
                g2d.setFont(font);
                g2d.drawString(s, (float) (width / 2 - r.getWidth() / 2), (float) (HEADER_HEIGHT_PTS + height + HEADER_HEIGHT_PTS / 2));
            }
        }


        // Main component
        g2d.setClip(0, HEADER_HEIGHT_PTS, widthInt, heightInt);   // Show only the component zone
        g2d.translate(0, HEADER_HEIGHT_PTS);                         // Top left of component zone
        g2d.translate(0, -yOffset);                             // Offset to show only relevant
        g2d.scale(hScaleFactor, vScaleFactor);
        clEditor.printAll(g2d);


        g2d.dispose();

        return PAGE_EXISTS;

    }

    // =============================================================================
    // Pageable implementation
    // =============================================================================
    @Override
    public final int getNumberOfPages()
    {
        return nbPages;
    }

    @Override
    public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException
    {
        return pageFormat;
    }

    @Override
    public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException
    {
        return this;
    }

    // =============================================================================
    // Private methods
    // =============================================================================
    private void preComputeData(CL_Editor editor, PageFormat pg)
    {
        // Current editor dimensions
        Rectangle r = editor.getBarRectangle(0);
        editorBarRowHeight = r.height;
        editorWidth = editor.getWidth();
        editorHeight = editor.getHeight();


        // Page format dimensions
        xMin = pageFormat.getImageableX();
        yMin = pageFormat.getImageableY();
        height = pageFormat.getImageableHeight() - HEADER_HEIGHT_PTS - FOOTER_HEIGHT_PTS;
        heightInt = (int) Math.ceil(height);
        width = pageFormat.getImageableWidth();
        widthInt = (int) Math.ceil(width);


        // Scaled editor dimensions
        hScaleFactor = width / editorWidth;
        vScaleFactor = hScaleFactor;
        scaledEditorHeight = editorHeight * vScaleFactor;


        // Nb of pages
        nbPages = (int) Math.ceil(scaledEditorHeight / height);


        LOGGER.fine("computeEditorDimensions() editorBarRowHeight=" + editorBarRowHeight
                + " editorWidth=" + editorWidth
                + " editorSongHeight=" + editorHeight);
    }

}
