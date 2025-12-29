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

import java.awt.BorderLayout;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.spi.BarBoxFactory;
import org.jjazz.cl_editor.spi.CL_EditorFactory;
import org.jjazz.flatcomponents.api.FixedPreferredWidthPanel;

/**
 * A printer for a ChordLeadSheet editor which fits available width and breaks pages at a BarBox edge.
 */
public class LeadSheetPrinter implements Printable, Pageable
{

    protected static final int HEADER_HEIGHT_PTS = 40;
    protected static final int FOOTER_HEIGHT_PTS = 20;
    protected static final Font FONT = new Font("Helvetica", Font.PLAIN, 11);
    private final CL_Editor clEditor;
    private double xMin;   // Upper corner of imageable
    private double yMin;   // Upper corner of imageable
    private double centralZoneHeight;  // Between header and footer
    private double width;
    private int widthInt;
    private double scaleFactor;
    private double scaledEditorHeight;  // Until bottom of last model row
    private double scaledEditorPageHeight; // One page stopping at the bottom of bar row
    private double scaledEditorLastPageHeight;
    private double scaledEditorBarHeight;
    private int nbPages;
    private MessageFormat headerMsg;
    private MessageFormat footerMsg;
    private final PageFormat pageFormat;

    private final List<ChangeListener> listeners = new ArrayList<>();
    private static RenderingDialog renderingDialog;
    private static final Logger LOGGER = Logger.getLogger(LeadSheetPrinter.class.getSimpleName());

    /**
     *
     * @param actualEditor The reference actual editor
     * @param song
     * @param pageFormat
     * @param zoomVFactor  [0-100]
     * @param nbColumns
     */
    public LeadSheetPrinter(CL_Editor actualEditor, Song song, PageFormat pageFormat, int zoomVFactor, int nbColumns)
    {
        this.pageFormat = pageFormat;


        // Build our own editor with own settings to have full control,  e.g. adjust size, nb of columns, change colors or chord symbol font
        var ourEditorSettings = new PrintCL_EditorSettings(actualEditor.getSettings());
        clEditor = CL_EditorFactory.getDefault().createEditor(song, ourEditorSettings, BarBoxFactory.getDefault(), actualEditor.getBarRendererFactory());
        clEditor.setNbColumns(nbColumns);
        CL_EditorClientProperties.setZoomYFactor(song, zoomVFactor);


        // Add the editor to the rendering dialog
        if (renderingDialog == null)
        {
            renderingDialog = new RenderingDialog();
        }
        renderingDialog.setEditor(clEditor, (int) pageFormat.getImageableWidth());


        // Layout everything
        renderingDialog.pack();
        // renderingDialog.setVisible(true);    
        computeDimensions();


        setHeaderMessage(new MessageFormat(song.getName()));
        setFooterMessage(new MessageFormat("{0} / {1}"));
    }

    public void cleanup()
    {
        renderingDialog.cleanup();
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
        return CL_EditorClientProperties.getZoomYFactor(clEditor.getSongModel());
    }

    /**
     * Change the vertical zoom factor of the editor.
     * <p>
     *
     * @param factor A value between 0 and 100.
     */
    public final void setEditorZoomVFactor(int factor)
    {
        if (factor != getEditorZoomFactor())
        {
            CL_EditorClientProperties.setZoomYFactor(clEditor.getSongModel(), factor);
            renderingDialog.pack();
            computeDimensions();
            fireChanged();
        }
    }

    public void setNbColumns(int cols)
    {
        if (cols != clEditor.getNbColumns())
        {
            clEditor.setNbColumns(cols);
            renderingDialog.pack();
            computeDimensions();
            fireChanged();
        }
    }

    public int getNbColumns()
    {
        return clEditor.getNbColumns();
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
            FontMetrics fm = g2d.getFontMetrics(FONT);


            if (headerMsg != null)
            {
                String s = headerMsg.format(args);
                Rectangle2D r = fm.getStringBounds(s, g2d);
                g2d.setFont(FONT);
                g2d.drawString(s, (float) (width / 2 - r.getWidth() / 2), (float) (HEADER_HEIGHT_PTS / 2));
            }


            if (footerMsg != null)
            {
                String s = footerMsg.format(args);
                Rectangle2D r = fm.getStringBounds(s, g2d);
                g2d.setFont(FONT);
                g2d.drawString(s, (float) (width - r.getWidth() - 1),
                        (float) (HEADER_HEIGHT_PTS + centralZoneHeight + FOOTER_HEIGHT_PTS / 2));
            }
        }


        // Main component
        int clipHeight = pageIndex < nbPages - 1 ? (int) Math.ceil(scaledEditorPageHeight) : (int) Math.ceil(scaledEditorLastPageHeight);
        g2d.setClip(0, HEADER_HEIGHT_PTS, widthInt, clipHeight);   // Show up to bottom of last possible bar row
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
        width = pageFormat.getImageableWidth();
        widthInt = (int) Math.ceil(width);


        // Scaled editor dimensions
        scaleFactor = width / clEditor.getWidth();
        Rectangle rLast = clEditor.getBarRectangle(clEditor.getModel().getSizeInBars() - 1);
        scaledEditorBarHeight = rLast.height * scaleFactor;
        Point pLastBar = rLast.getLocation();
        scaledEditorHeight = pLastBar.y * scaleFactor + scaledEditorBarHeight;


        // Nb of pages: need to take into account clipping at a bar row boundary
        int maxNbRowsPerPage = (int) Math.floor(centralZoneHeight / scaledEditorBarHeight);
        scaledEditorPageHeight = maxNbRowsPerPage * scaledEditorBarHeight;
        nbPages = (int) Math.ceil(scaledEditorHeight / scaledEditorPageHeight);


        // Last page clipping might be shorter
        scaledEditorLastPageHeight = scaledEditorHeight - (nbPages - 1) * scaledEditorPageHeight;


        LOGGER.log(Level.FINE,
                "computeEditorDimensions() scaledEditorBarHeight={0} scaledEditorHeight={1} scaledEditorPageHeight={2} centralZoneHeight={3} nbPages={4}",
                new Object[]
                {
                    scaledEditorBarHeight,
                    scaledEditorHeight, scaledEditorPageHeight, centralZoneHeight, nbPages
                });
    }

    private void fireChanged()
    {
        listeners.forEach(l -> l.stateChanged(new ChangeEvent(this)));
    }
    // =============================================================================
    // Private classes
    // =============================================================================

    /**
     * Special hidden dialog (but displayable) used to render the CL_Editor.
     * <p>
     * A fixed width panel is used to make sure width is not changed when CL_Editor preferred width has changed, like in the application editor.
     */
    static private class RenderingDialog extends JDialog
    {

        FixedPreferredWidthPanel fixWidthPanel;
        JScrollPane scrollPane;
        CL_Editor editor;

        public RenderingDialog()
        {
            fixWidthPanel = new FixedPreferredWidthPanel();
            fixWidthPanel.setLayout(new BorderLayout());
            add(fixWidthPanel);
        }

        public void cleanup()
        {
            if (editor != null)
            {
                fixWidthPanel.remove(scrollPane);
                editor.cleanup();
            }
        }

        public void setEditor(CL_Editor editor, int refWidth)
        {
            cleanup();
            this.editor = editor;
            fixWidthPanel.setFixedPreferredWidth(refWidth);
            scrollPane = new JScrollPane(editor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            fixWidthPanel.add(scrollPane);
        }

    }
}
