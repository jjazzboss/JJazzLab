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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import org.jjazz.song.api.Song;
import org.jjazz.songeditormanager.SongEditorManager;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_EditorFactory;

/**
 * A printer for Song editors which fits available width and breaks pages at a BarBox edge.
 */
public class SongPrinter implements Printable, Pageable
{

    private static final int HEADER_HEIGHT_PTS = 40;
    private static final int FOOTER_HEIGHT_PTS = 20;
    private CL_Editor clEditor;
    double xMin;   // Upper corner of imageable
    double yMin;   // Upper corner of imageable
    double centralZoneHeight;  // Between header and footer
    int centralZoneHeightInt;
    double width;
    int widthInt;
    double dlgScaleFactor = 1;
    double scaleFactor;
    double scaledEditorHeight;  // Until bottom of last model row
    double scaledEditorPageHeight; // One page stopping at the bottom of bar row
    private double scaledEditorBarHeight;
    private int nbPages;
    private MessageFormat headerMsg;
    private MessageFormat footerMsg;
    private final PageFormat pageFormat;
    private final Font font;
    private static JDialog renderingDlg;
    private static final Logger LOGGER = Logger.getLogger(SongPrinter.class.getSimpleName());

    public SongPrinter(Song song, PageFormat pageFormat)
    {
        this.pageFormat = pageFormat;


        // Build our editor
        CL_EditorFactory clef = CL_EditorFactory.getDefault();
        clEditor = clef.createEditor(song);

        // Put it in a hidden dialog for control
        setupRenderingDialog(song, clEditor);


        computeDimensions();


        font = new Font("Helvetica", Font.PLAIN, 11);


        setHeaderMessage(new MessageFormat(song.getName()));
        setFooterMessage(new MessageFormat("{0} / " + getNumberOfPages()));

    }

    public void increaseDlgFactor()
    {
        changeRendererDialogWidth(0.9d);

    }

    public void decreaseDlgFactor()
    {
        changeRendererDialogWidth(1.1d);
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

        LOGGER.fine("print() -- pageIndex=" + pageIndex);


        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);


        // Y offset corresponding to pageIndex
        double yOffset = pageIndex * scaledEditorPageHeight;
        LOGGER.fine("print() yOffset=" + yOffset);


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
                g2d.drawString(s, (float) (width - r.getWidth() - 1), (float) (HEADER_HEIGHT_PTS + centralZoneHeight + FOOTER_HEIGHT_PTS / 2));
            }
        }


        // Main component
        g2d.setClip(0, HEADER_HEIGHT_PTS, widthInt, (int) Math.ceil(scaledEditorPageHeight));   // Show up to bottom of last possible bar row
        g2d.translate(0, HEADER_HEIGHT_PTS);                         // Top left of component zone
        g2d.translate(0, -yOffset);                                 // Offset to show only relevant
        g2d.scale(scaleFactor, scaleFactor);
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
    private void computeDimensions()
    {

        // Page format dimensions
        xMin = pageFormat.getImageableX();
        yMin = pageFormat.getImageableY();
        centralZoneHeight = pageFormat.getImageableHeight() - HEADER_HEIGHT_PTS - FOOTER_HEIGHT_PTS;
        centralZoneHeightInt = (int) Math.ceil(centralZoneHeight);
        width = pageFormat.getImageableWidth();
        widthInt = (int) Math.ceil(width);


        // Scaled editor dimensions
        scaleFactor = width / clEditor.getWidth();
        Rectangle rLast = clEditor.getBarRectangle(clEditor.getModel().getSize() - 1);
        scaledEditorBarHeight = rLast.height * scaleFactor;
        Point pLastBar = rLast.getLocation();
        scaledEditorHeight = pLastBar.y * scaleFactor + scaledEditorBarHeight;


        // Nb of pages: need to take into account clipping at a bar row boundary
        int maxNbRowsPerPage = (int) Math.floor(centralZoneHeight / scaledEditorBarHeight);
        scaledEditorPageHeight = maxNbRowsPerPage * scaledEditorBarHeight;
        nbPages = (int) Math.ceil(scaledEditorHeight / scaledEditorPageHeight);


        LOGGER.fine("computeEditorDimensions() scaledEditorBarHeight=" + scaledEditorBarHeight
                + " scaledEditorHeight=" + scaledEditorHeight
                + " scaledEditorPageHeight=" + scaledEditorPageHeight
                + " centralZoneHeight=" + centralZoneHeight
                + " nbPages=" + nbPages);
    }


    private void setupRenderingDialog(Song song, CL_Editor editor)
    {
        var res = SongEditorManager.getInstance().getEditors(song);
        CL_Editor actualEditor = res.getTcCle().getCL_Editor();

        if (renderingDlg == null)
        {
            renderingDlg = new JDialog();
        }

        // Remove existing editor if any
        for (Component c : renderingDlg.getComponents())
        {
            if (c instanceof JScrollPane)
            {
                renderingDlg.remove(c);
            }
        }

        // Add ours
        renderingDlg.add(new JScrollPane(editor));     // Scrollpane needed!


        // Start with same size than actual editor
        editor.setPreferredSize(actualEditor.getSize());


        // Layout everything
        renderingDlg.pack();
    }

    private void changeRendererDialogWidth(double scaleFactor)
    {
        Dimension pd = clEditor.getPreferredSize();
        int newWidth = Math.max(200, (int) Math.floor(pd.width * scaleFactor));
        LOGGER.severe("changeRendererDialogWidth() newWidth=" + newWidth);
        Dimension newPd = new Dimension(newWidth, pd.height);
        clEditor.setPreferredSize(newPd);
        renderingDlg.pack();
        computeDimensions();
    }
}
