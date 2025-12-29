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

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.itemrenderer.api.IR_AnnotationTextSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_Copiable;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererSettings;
import org.jjazz.uiutilities.api.RedispatchingMouseAdapter;
import org.jjazz.uiutilities.api.UIUtilities;

/**
 * Represents an annotation text.
 */
public class IR_AnnotationText extends ItemRenderer implements IR_Copiable
{

    /**
     * Our graphical settings.
     */
    private final IR_AnnotationTextSettings settings;
    /**
     * Copy mode.
     */
    private boolean copyMode;
    private int zoomFactor = 50;
    private int nbLines;
    private Font zFont;
    private final JTextArea textArea;
    private static final Logger LOGGER = Logger.getLogger(IR_AnnotationText.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_AnnotationText(CLI_BarAnnotation item, ItemRendererSettings irSettings)
    {
        super(item, IR_Type.BarAnnotationText);
        nbLines = 1;

        LOGGER.log(Level.FINE, "IR_AnnotationText() -- item={0}", item);


        // Register settings changes
        settings = irSettings.getIR_AnnotationTextSettings();
        settings.addPropertyChangeListener(this);
        zFont = settings.getFont();

        // Prepare the JScrollPane with the text area inside
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setOpaque(false);
        textArea.setText(item.getData());
        textArea.setCaretPosition(0);
        textArea.setBorder(null);
        textArea.setEnabled(false);     // Disable some mouse event capturing (avoid focus lost when doing ctrl+click on several annotations)
        setTextFontColor(settings.getColor());
        textArea.setFont(zFont);
        textArea.setTransferHandler(null);  // Required otherwise icon "drop not possible" is shown when dragging an IR_AnnotationText to another bar (because another IR_Annotation is used as insertion point)
        textArea.setRows(nbLines);
        setLayout(new BorderLayout());
        add(textArea);


        // Redispatch mouse events for selection and drag to work
        var mouseRedispatcher = new RedispatchingMouseAdapter(this);
        textArea.addMouseListener(mouseRedispatcher);
        textArea.addMouseMotionListener(mouseRedispatcher);


        modelChanged();

    }

    public void setNbLines(int n)
    {
        Preconditions.checkArgument(n >= 1 && n <= CL_EditorClientProperties.BAR_ANNOTATION_MAX_NB_LINES, "n=%s", n);
        this.nbLines = n;
        textArea.setRows(nbLines);
        revalidate();
        repaint();
    }

    /**
     * Overridden because JTextArea.getPreferredSize() uses all text lines to compute height if there are more actual lines than nbLines.
     * <p>
     * JTextArea.getPreferredScrollableViewportSize() height calculation is only based on nbLines.
     */
    @Override
    public Dimension getPreferredSize()
    {
        var d = textArea.getPreferredScrollableViewportSize();
        var in = getInsets();
        d.height += in.top + in.bottom;
        return d;
    }

    public int getNbLines()
    {
        return nbLines;
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
        fontOrZoomChanged();
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
        String text = ((CLI_BarAnnotation) getModel()).getData();
        textArea.setText(text);
        textArea.setCaretPosition(0);
        setToolTipText(text);
    }

    @Override
    protected void modelMoved()
    {
        // Nothing
    }


    /**
     * Compute the IR_AnnotationText font size depending on the zoom factor.
     *
     * @param fontBaseSize
     * @param zoom
     * @return
     * @see #getZoomFactor()
     */
    static public float computeFontSize(float fontBaseSize, int zoom)
    {
        float f2 = 0.5f + (zoom / 100f);
        float res = Math.max(7, f2 * fontBaseSize);
        res = Math.max(res, 7);
        return res;
    }

    /**
     * Overridden to paint some indicators above children components.
     * <p>
     * Indicators:<br>
     * CopyMode indicator when dragging <br>
     * Some text lines are not visible<br>
     */
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;
        Insets in = getInsets();
        int w = getWidth();

        if (isTextHidden())
        {
            // Draw dots in bottom right corner
            final int nbDots = 3;
            final int thickness = 2;
            final int padding = 3;
            for (int i = 1; i <= nbDots; i++)
            {
                int x = w - in.right - 1 - i * (padding + thickness);
                int y = getHeight() - in.bottom - padding - thickness - 1;
                g2.setColor(settings.getColor());
                g2.fillRect(x, y, thickness, thickness);
            }
        }

        if (copyMode)
        {
            // Draw the copy indicator in upper right corner
            int size = IR_Copiable.CopyIndicator.getSideLength();
            Graphics2D gg2 = (Graphics2D) g2.create(Math.max(w - in.right - size - 1, 0), 1 + in.top, size, size);
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
            if (e.getPropertyName().equals(IR_AnnotationTextSettings.PROP_FONT))
            {
                setFont(settings.getFont());
                fontOrZoomChanged();
            } else if (e.getPropertyName().equals(IR_AnnotationTextSettings.PROP_FONT_COLOR))
            {
                setTextFontColor(settings.getColor());
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

    private void setTextFontColor(Color c)
    {
        textArea.setDisabledTextColor(c);       // Since textArea is disabled (see why in constructor)
        textArea.repaint();
    }

    private void fontOrZoomChanged()
    {
        float zFontSize = computeFontSize(getFont().getSize2D(), zoomFactor);
        zFont = getFont().deriveFont(zFontSize);
        textArea.setFont(zFont);
        revalidate();
        repaint();
    }


    private boolean isTextHidden()
    {
        var countLines = UIUtilities.countLines(textArea);
        return countLines > nbLines;
    }


}
