package org.jjazz.instrumentcomponents.guitardiagram.api;



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
 *  
 */
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.uiutilities.api.StringMetrics;

/*
 * NOTE: code reused and modified from the TuxGuitar software (GNU Lesser GPL license), author: Julián Gabriel Casadesús
 */

/**
 *
 * Represent a guitar chord chart/diagram.
 */
public class GuitarDiagramComponent extends JPanel
{

    private static final int MAX_FRETS = 6;
    private static final Font FIRST_FRET_FONT = Font.decode("Arial-PLAIN-9");
    private static final float CHORD_FIRST_FRET_SPACING = 12;
    private static final float CHORD_STRING_SPACING = 8;
    private static final float CHORD_FRET_SPACING = 10;
    private static final float CHORD_NOTE_SIZE = 6;
    private static final float CHORD_LINE_WIDTH = 1f;
    private Color noteColor;
    private Color tonicNoteColor;
    private Font firstFretFont = FIRST_FRET_FONT;
    private float firstFretSpacing = CHORD_FIRST_FRET_SPACING;
    private float stringSpacing = CHORD_STRING_SPACING;
    private float fretSpacing = CHORD_FRET_SPACING;
    private float noteDiameter = CHORD_NOTE_SIZE;
    private float lineWidth = CHORD_LINE_WIDTH;
    private BufferedImage image;
    private TGChord chordModel;
    private ChordSymbol chordSymbol;
    private static final Logger LOGGER = Logger.getLogger(GuitarDiagramComponent.class.getSimpleName());

    public GuitarDiagramComponent()
    {
        this(null, null);
    }


    /**
     *
     * @param chordModel Can be null
     * @param cs Can be null
     */
    public GuitarDiagramComponent(TGChord chordModel, ChordSymbol cs)
    {
        setChordModel(chordModel);
        this.chordSymbol = cs;
        String tt = chordModel.getNotes().toString();
        setToolTipText(tt);
    }

    /**
     * @return the chordModel. Can be null.
     */
    public TGChord getChordModel()
    {
        return chordModel;
    }

    @Override
    public Dimension getPreferredSize()
    {
        image = chordModel == null ? buildNullImage() : buildImage(chordModel);
        return new Dimension(image.getWidth(), image.getHeight());
    }

    /**
     * @param chordModel the chordModel to set. Can be null.
     */
    public void setChordModel(TGChord chordModel)
    {
        this.chordModel = chordModel;
        revalidate();
        repaint();
    }

    public Color getNoteColor()
    {
        return this.noteColor;
    }

    public void setNoteColor(Color noteColor)
    {
        this.noteColor = noteColor;
        repaint();
    }

    public Color getTonicNoteColor()
    {
        return tonicNoteColor;
    }

    public void setTonicNoteColor(Color tonicNoteColor)
    {
        this.tonicNoteColor = tonicNoteColor;
        repaint();
    }

    public float getFirstFretSpacing()
    {
        return this.firstFretSpacing;
    }

    public void setFirstFretSpacing(float firstFretSpacing)
    {
        this.firstFretSpacing = firstFretSpacing;
        revalidate();
        repaint();
    }

    public float getFretSpacing()
    {
        return this.fretSpacing;
    }

    public void setFretSpacing(float fretSpacing)
    {
        this.fretSpacing = fretSpacing;
        revalidate();
        repaint();
    }

    public float getStringSpacing()
    {
        return this.stringSpacing;
    }

    public void setStringSpacing(float stringSpacing)
    {
        this.stringSpacing = stringSpacing;
        revalidate();
        repaint();
    }

    public float getNoteDiameter()
    {
        return this.noteDiameter;
    }

    public void setNoteDiameter(float noteDiameter)
    {
        this.noteDiameter = noteDiameter;
        revalidate();
        repaint();
    }

    public float getLineWidth()
    {
        return this.lineWidth;
    }

    public void setLineWidth(float lineWidth)
    {
        this.lineWidth = lineWidth;
        revalidate();
        repaint();
    }


    public Font getFirstFretFont()
    {
        return this.firstFretFont;
    }

    public void setFirstFretFont(Font firstFretFont)
    {
        this.firstFretFont = firstFretFont;
        revalidate();
        repaint();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); // Honor the opaque property

        // LOGGER.severe("paintComponent() -- ");

        Graphics2D g2 = (Graphics2D) g.create();

        Insets in = getInsets();
        int w = getWidth() - in.left - in.right;
        int h = getHeight() - in.top - in.bottom;


        if (image == null)
        {
            LOGGER.log(Level.WARNING, "paintComponent() image==null ! chordModel={0}", chordModel);
            return;
        }

        int x = in.left + (w - 1 - image.getWidth()) / 2;
        int y = in.top + (h - 1 - image.getHeight()) / 2;
        g2.drawImage(image, null, x, y);

    }


    // ===========================================================================================
    // Private methods
    // ===========================================================================================
    /**
     * A special replacement image when no chord model is set.
     *
     * @return A "?" image
     */
    private BufferedImage buildNullImage()
    {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        String text = "?";
        var bounds = StringMetrics.create(g2, g2.getFont()).getLogicalBoundsNoLeading(text);
        int x = (int) ((img.getWidth() - bounds.getWidth()) / 2);
        int y = (int) ((img.getHeight() - bounds.getHeight()) / 2);
        x += -bounds.getX();  // bounds are in baseline relative coordinates
        y += -bounds.getY();  // bounds are in baseline relative coordinates
        g2.drawString(text, x, y);
        g2.dispose();
        return img;
    }

    /**
     * Paint the diagram in a buffered image.
     *
     * @param chord
     * @return
     */
    private BufferedImage buildImage(TGChord chord)
    {
        checkNotNull(chord);

        // LOGGER.severe("buildImage() -- chord="+chord);

        BufferedImage img;
        Graphics2D g2;


        // Temporary img to compute fret text width
        final int H_PADDING = 5;
        String firstFretString = String.valueOf(chordModel.getFirstFret());
        img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        g2 = img.createGraphics();
        Rectangle2D firstFretStringBounds = StringMetrics.create(g2, getFirstFretFont()).getLogicalBoundsNoLeading(firstFretString);  // Bounds in baseline relative coordinates
        g2.dispose();


        // Create image     
        int w = (int) Math.ceil(getStringSpacing() + getStringSpacing() * chordModel.countStrings() + firstFretStringBounds.getWidth() + H_PADDING);
        int h = (int) Math.ceil(getFretSpacing() + getFretSpacing() * MAX_FRETS);
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);


        // Prepare graphics
        g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(lineWidth));
        g2.setFont(firstFretFont);
        g2.setColor(getForeground());


        float x = getStringSpacing();
        float y = getFretSpacing();


        // Draw the first fret number        
        g2.drawString(firstFretString, (float) Math.ceil(x + (-firstFretStringBounds.getX())), (float) Math.ceil(y + (-firstFretStringBounds.getY())));
        x += firstFretStringBounds.getWidth() + H_PADDING;


        // Draw the strings
        for (int i = 0; i < chordModel.getStrings().length; i++)
        {
            float x1 = x + (i * getStringSpacing());
            float x2 = x1;
            float y1 = y;
            float y2 = y + getFretSpacing() * (MAX_FRETS - 1);
            var shape = new Line2D.Float(x1, y1, x2, y2);
            g2.draw(shape);
        }


        // Draw the frets
        for (int i = 0; i < MAX_FRETS; i++)
        {
            float x1 = x;
            float x2 = x + getStringSpacing() * (chordModel.countStrings() - 1);
            float y1 = y + i * getFretSpacing();
            float y2 = y1;
            var shape = new Line2D.Float(x1, y1, x2, y2);
            g2.draw(shape);
            if (i == 0 && chordModel.getFirstFret() == 1)
            {
                shape = new Line2D.Float(x1, y1 - 1, x2, y2 - 1);
                g2.draw(shape);
            }
        }

        // Notes
        float noteRadius = noteDiameter / 2;
        for (int i = 0; i < chordModel.getStrings().length; i++)
        {
            g2.setColor(getForeground());
            int fret = chordModel.getFretValue(i);
            float noteX = x + ((getStringSpacing() * (chordModel.countStrings() - 1)) - (getStringSpacing() * i)) + 0.5f;
            if (fret < 0)
            {
                // Draw a cross above the top line
                var shape = new Line2D.Float(noteX - noteRadius, 0, noteX + noteRadius, noteDiameter);
                g2.draw(shape);
                shape = new Line2D.Float(noteX + noteRadius, 0, noteX - noteRadius, noteDiameter);
                g2.draw(shape);
            } else if (fret == 0)
            {
                // Draw a circle above the top line         
                var shape = new Ellipse2D.Float(noteX - noteRadius, 0, noteDiameter, noteDiameter);
                g2.draw(shape);
            } else
            {
                // Draw the note
                fret -= (chordModel.getFirstFret() - 1);
                float noteY = y + getFretSpacing() * fret - getFretSpacing() / 2;
                var shape = new Ellipse2D.Float(noteX - noteRadius, noteY - noteRadius, noteDiameter, noteDiameter);
                Color c = noteColor == null ? getForeground() : noteColor;
                if (isTonicFret(i, chordModel.getFretValue(i)) && tonicNoteColor != null)
                {
                    c = tonicNoteColor;
                }
                g2.setColor(c);
                g2.fill(shape);
            }
        }

        g2.dispose();

        return img;

    }


    private boolean isTonicFret(int sIndex, int fret)
    {
        boolean b = false;
        if (chordSymbol != null)
        {
            TGString string = TGString.createDefaultInstrumentStrings().get(sIndex);
            int sValue = string.getValue();
            int relPitch = (sValue + fret) % 12;
            int tonicRelPitch = chordSymbol.getRootNote().getRelativePitch();
            b = relPitch % 12 == tonicRelPitch;
        }
        return b;
    }
}
