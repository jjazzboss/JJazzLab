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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.uiutilities.api.RedispatchingMouseAdapter;

/**
 * Represents an annotation text.
 */
public class IR_AnnotationText extends ItemRenderer implements IR_Copiable
{

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
    private int zoomFactor = 50;
    private int nbLines;
    private Font zFont;
    private JTextArea textArea;
    private static final Logger LOGGER = Logger.getLogger(IR_AnnotationText.class.getName());

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_AnnotationText(CLI_BarAnnotation item, ItemRendererSettings irSettings)
    {
        super(item, IR_Type.BarAnnotationText);
        nbLines = 1;


        // Register settings changes
        settings = irSettings.getIR_AnnotationTextSettings();
        settings.addPropertyChangeListener(this);
        zFont = settings.getFont();

        // Prepare the JScrollPane with the text area inside
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setRows(nbLines);
        textArea.setOpaque(false);
        textArea.setText(item.getData());
        textArea.setCaretPosition(0);
        textArea.setBorder(null);
        textArea.setEnabled(false);     // Disable some mouse event capturing (avoid focus lost when doing ctrl+click on several annotations)
        setTextFontColor(settings.getColor());
        textArea.setFont(zFont);
        textArea.setTransferHandler(null);  // Required otherwise icon "drop not possible" is shown when dragging an IR_AnnotationText to another bar (because another IR_Annotation is used as insertion point)
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        setLayout(new BorderLayout());        
        add(scrollPane);

        
        // Redispatch mouse events for selection and drag to work
        var mouseRedispatcher = new RedispatchingMouseAdapter(this);
        textArea.addMouseListener(mouseRedispatcher);
        textArea.addMouseMotionListener(mouseRedispatcher);
        
    }

    public void setNbLines(int n)
    {
        Preconditions.checkArgument(n >= 1 && n <= MAX_NB_LINES, "n=%d", n);
        this.nbLines = n;
        textArea.setRows(nbLines);
        revalidate();
        repaint();
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
        float f2 = 0.5f + (zoomFactor / 100f);
        float zFontSize = f2 * getFont().getSize2D();
        zFontSize = Math.max(zFontSize, 7);
        zFont = getFont().deriveFont(zFontSize);
        textArea.setFont(zFont);
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
        String text = ((CLI_BarAnnotation) getModel()).getData();
        textArea.setText(text);
        textArea.setCaretPosition(0);
    }

    @Override
    protected void modelMoved()
    {
        // Nothing
    }

    /**
     * Possibly paint the copyMode ABOVE the children components.
     */
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);   


        if (copyMode)
        {
            Graphics2D g2 = (Graphics2D) g;            
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
            if (e.getPropertyName().equals(IR_AnnotationTextSettings.PROP_FONT))
            {
                setFont(settings.getFont());
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

    /**
     * Update text area font color.
     */
    private void setTextFontColor(Color c)
    {
        textArea.setDisabledTextColor(c);
    }
}
