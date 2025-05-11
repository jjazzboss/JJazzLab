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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.song.api.Song;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_EditorClientProperties;

/**
 * A printer for a SongStructure editor.
 */
public class SongStructurePrinter implements Printable, Pageable
{

    private final SS_Editor ssEditor;
    private double xMin;   // Upper corner of imageable
    private double yMin;   // Upper corner of imageable
    private double centralZoneHeight;  // Between header and footer
    private double width;
    private int widthInt;
    private double scaleFactor;
    private double scaledEditorHeight;  // Until bottom of last model row
    private double scaledEditorPageHeight; // One page stopping at the bottom of bar row
    private double scaledEditorLastPageHeight;   
    private int nbPages;
    private MessageFormat headerMsg;
    private MessageFormat footerMsg;
    private final PageFormat pageFormat;
    private final List<ChangeListener> listeners = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(SongStructurePrinter.class.getSimpleName());

    /**
     *
     * @param actualEditor The reference actual editor
     * @param song
     * @param pageFormat
     * @param zoomVFactor [0-100]
     * @param nbColumns
     */
    public SongStructurePrinter(SS_Editor actualEditor, Song song, PageFormat pageFormat)
    {
        this.pageFormat = pageFormat;

        ssEditor = actualEditor;

        computeDimensions();

        setHeaderMessage(new MessageFormat(song.getName()));
        setFooterMessage(new MessageFormat("{0} / {1}"));
    }

    public void cleanup()
    {
        // Nothing
    }

    /**
     * Add a change listener.
     * <p>
     * Change events are fired everytime the printable content might have changed.
     *
     * @param l
     */
    public void addChangeListener(ChangeListener l)
    {
        if (!listeners.contains(l))
        {
            listeners.add(l);
        }
    }

    public void removeChangeListener(ChangeListener l)
    {
        listeners.remove(l);
    }

    /**
     *
     * @return [0-100]
     */
    public int getEditorZoomFactor()
    {
        return SS_EditorClientProperties.getZoomYFactor(ssEditor.getSongModel());
    }

    /**
     * Set header message.
     * <p>
     * {0} is replaced by pageIndex. {1} is replaced by nbPages.
     *
     * @param msg Example new MessageFormat("Page {0}/{1}"). Can be null.
     */
    public final void setHeaderMessage(MessageFormat msg)
    {
        headerMsg = msg;
        fireChanged();
    }

    /**
     * Set footer message.
     * <p>
     * {0} is replaced by pageIndex. {1} is replaced by nbPages.
     *
     * @param msg Example new MessageFormat("Page {0}/{1}"). Can be null.
     */
    public final void setFooterMessage(MessageFormat msg)
    {
        footerMsg = msg;
        fireChanged();
    }

    // =============================================================================
    // Printable implementation
    // =============================================================================
    @Override
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException
    {

        LOGGER.log(Level.FINE, "print() -- pageIndex={0}", pageIndex);   


        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);


        // Y offset corresponding to pageIndex
        double yOffset = pageIndex * scaledEditorPageHeight;
        LOGGER.log(Level.FINE, "print() yOffset={0}", yOffset);   


        // Set position on upper left corner of Imageable area
        g2d.translate(xMin, yMin);


        // Header & footer
        if (footerMsg != null || headerMsg != null)
        {
            Object[] args =
            {
                pageIndex + 1, nbPages
            };
            FontMetrics fm = g2d.getFontMetrics(LeadSheetPrinter.FONT);
            g2d.setFont(LeadSheetPrinter.FONT);

            if (headerMsg != null)
            {
                String s = headerMsg.format(args);
                Rectangle2D r = fm.getStringBounds(s, g2d);
                g2d.drawString(s, (float) (width / 2 - r.getWidth() / 2), (float) (LeadSheetPrinter.HEADER_HEIGHT_PTS / 2));
            }


            if (footerMsg != null)
            {
                String s = footerMsg.format(args);
                Rectangle2D r = fm.getStringBounds(s, g2d);
                g2d.drawString(s, (float) (width - r.getWidth() - 1), (float) (LeadSheetPrinter.HEADER_HEIGHT_PTS + centralZoneHeight + LeadSheetPrinter.FOOTER_HEIGHT_PTS / 2));
            }
        }


        // Main component
        int clipHeight = pageIndex < nbPages - 1 ? (int) Math.ceil(scaledEditorPageHeight) : (int) Math.ceil(scaledEditorLastPageHeight);
        g2d.setClip(0, LeadSheetPrinter.HEADER_HEIGHT_PTS, widthInt, clipHeight);   // Show up to bottom of last possible bar row
        g2d.translate(0, LeadSheetPrinter.HEADER_HEIGHT_PTS);                         // Top left of component zone
        g2d.translate(0, -yOffset);                                 // Offset to show only relevant
        g2d.scale(scaleFactor, scaleFactor);
        ssEditor.printAll(g2d);


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
        centralZoneHeight = pageFormat.getImageableHeight() - LeadSheetPrinter.HEADER_HEIGHT_PTS - LeadSheetPrinter.FOOTER_HEIGHT_PTS;
        width = pageFormat.getImageableWidth();
        widthInt = (int) Math.ceil(width);


        // Scaled editor dimensions
        scaleFactor = width / ssEditor.getWidth();
        scaledEditorHeight = ssEditor.getHeight() * scaleFactor;
        scaledEditorPageHeight = centralZoneHeight;

        // Nb of pages: need to take into account clipping at a bar row boundary
        nbPages = (int) Math.ceil(scaledEditorHeight / scaledEditorPageHeight);


        // Last page clipping might be shorter
        scaledEditorLastPageHeight = scaledEditorHeight - (nbPages - 1) * scaledEditorPageHeight;


        LOGGER.log(Level.FINE, "computeEditorDimensions()  scaledEditorHeight={0} scaledEditorPageHeight={1} centralZoneHeight={2} nbPages={3}", new Object[]{scaledEditorHeight,
            scaledEditorPageHeight, centralZoneHeight, nbPages});
    }

    private void fireChanged()
    {
        listeners.forEach(l -> l.stateChanged(new ChangeEvent(this)));
    }
    // =============================================================================
    // Private classes
    // =============================================================================

}
