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
package org.jjazz.itemrenderer.api;

import com.google.common.base.Preconditions;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.song.api.Song;

/**
 * Represents an annotation text.
 */
public class IR_AnnotationText extends ItemRenderer implements IR_Copiable
{

    /**
     * Song property used to store the nb of annotation lines.
     */
    public static final String SONG_PROP_NB_ANNOTATION_LINES = "SongPropNbAnnotationLines";

    public static final int MAX_NB_LINES = 4;
    /**
     * Border size between text and edge.
     */
    private static final int MARGIN = 2;


    /**
     * Our graphical settings.
     */
    private final IR_AnnotationTextSettings settings;
    /**
     * Copy mode.
     */
    private boolean copyMode;
    private final String[] annotationStrings = new String[MAX_NB_LINES];
    /**
     * The x/y baseline position to draw the strings
     */
    private final int[] xString = new int[MAX_NB_LINES];
    private final int[] yString = new int[MAX_NB_LINES];
    private int zoomFactor = 50;
    private int nbLines;
    private Font zFont;
    private static final Logger LOGGER = Logger.getLogger(IR_AnnotationText.class.getName());

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_AnnotationText(CLI_BarAnnotation item, ItemRendererSettings irSettings)
    {
        super(item, IR_Type.BarAnnotationText);
        nbLines = 1;
        updateAnnotationStrings(item.getData());


        // Register settings changes
        settings = irSettings.getIR_AnnotationTextSettings();
        settings.addPropertyChangeListener(this);


        // Init
        zFont = settings.getFont();
        setFont(zFont);
        setForeground(settings.getColor());
    }

    public void setNbLines(int nbLines)
    {
        Preconditions.checkArgument(nbLines >= 1 && nbLines <= 4, "nbLines=%d", nbLines);
        this.nbLines = nbLines;
        modelChanged();
    }

    public int getNbLines()
    {
        return nbLines;
    }

    /**
     * Calculate the preferredSize() depending on font, zoomFactor, and nbLines.
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
        assert g2 != null : "g2=" + g2 + " annotationStrings=" + annotationStrings + " f=" + f + " zFactor=" + zFactor;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float factor = 0.5f + (zFactor / 100f);
        float zFontSize = factor * f.getSize2D();
        zFontSize = Math.max(zFontSize, 7);
        zFont = f.deriveFont(zFontSize);


        Insets in = getInsets();
        FontMetrics fm = g2.getFontMetrics(zFont);
        int pw = 2 * MARGIN + in.left + in.right;
        int ph = in.top + MARGIN;     // + 2 * MARGIN + in.top + in.bottom

        for (int i = 0; i < nbLines; i++)
        {
            String line = annotationStrings[i];
            if (line == null)
            {
                break;
            }
            Rectangle2D r = fm.getStringBounds(line, g2);
            pw = Math.max((int) Math.round(r.getWidth()) + 2 * MARGIN + in.left + in.right, pw);
            ph += (int) Math.round(r.getHeight()) + MARGIN;

            xString[i] = in.left + MARGIN;
            yString[i] = ph - fm.getDescent() + 1;
        }
        ph += in.bottom;

        g2.dispose();

        Dimension d = new Dimension(pw, ph);
        LOGGER.log(Level.FINE, "getPreferredSize() d={0}", d);
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
    }

    @Override
    protected void modelChanged()
    {
        updateAnnotationStrings(((CLI_BarAnnotation) getModel()).getData());
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

        // Then draw the string with zoomed font
        g2.setColor(getForeground());
        g2.setFont(zFont);
        for (int i = 0; i < nbLines; i++)
        {
            String line = annotationStrings[i];
            if (line == null)
            {
                break;
            }
            g2.drawString(line, xString[i], yString[i]);
        }

        if (copyMode)
        {
            // Draw the copy indicator in upper right corner
            int size = IR_Copiable.CopyIndicator.getSideLength();
            Graphics2D gg2 = (Graphics2D) g2.create(Math.max(getWidth() - size - 1, 0), 1, size, size);
            IR_Copiable.CopyIndicator.drawCopyIndicator(gg2);
            gg2.dispose();
        }
    }

    /**
     * Get the nb of annotation lines for the specified song.
     *
     * @param song
     * @return
     */
    static public int getNbAnnotationLinesPropertyValue(Song song)
    {
        return song.getClientProperties().getInt(SONG_PROP_NB_ANNOTATION_LINES, 2);
    }

    /**
     * Save the nb of annotation lines in the specified song property.
     *
     * @param song
     * @param nbLines Must be between 1 and 4.
     */
    static public void setNbAnnotationLinesPropertyValue(Song song, int nbLines)
    {
        Preconditions.checkArgument(nbLines >= 1 && nbLines <= 4, "nbLines=%d", nbLines);
        song.getClientProperties().putInt(SONG_PROP_NB_ANNOTATION_LINES, nbLines);
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
            if (e.getPropertyName().equals(IR_AnnotationTextSettings.PROP_FONT))
            {
                setFont(settings.getFont());
            } else if (e.getPropertyName().equals(IR_AnnotationTextSettings.PROP_FONT_COLOR))
            {
                setForeground(settings.getColor());
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

    /**
     * Makes sure nbLines is respected.
     *
     * @param s Can contain \n
     */
    private void updateAnnotationStrings(String s)
    {
        var strs = s.split("\n");
        Arrays.fill(annotationStrings, null);
        for (int i = 0; i < strs.length; i++)
        {
            if (i < nbLines)
            {
                annotationStrings[i] = strs[i];
            } else
            {
                annotationStrings[nbLines - 1] += " /" + strs[i];
            }
        }
    }
}
