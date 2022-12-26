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
package org.jjazz.pianoroll.api;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import org.jjazz.harmony.api.Note;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.ui.utilities.api.HSLColor;
import org.jjazz.ui.utilities.api.StringMetrics;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.uisettings.api.GeneralUISettings;

/**
 * A JComponent which represents a NoteEvent.
 */
public class NoteView extends JPanel implements PropertyChangeListener, Comparable<NoteEvent>
{

    public static final String PROP_SELECTED = "PropSelected";
    public static final String PROP_MODEL = "PropModel";
    private static Color[] VELOCITY_COLORS;
    private static final Color COLOR_TEXT = Color.WHITE;
    private static final Font FONT;
    private static final int FONT_HEIGHT;

    static
    {
        // Precalculate font height
        FONT = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(10f);
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        var bounds = new StringMetrics(g2, FONT).getLogicalBoundsNoLeading("A");
        FONT_HEIGHT = (int) bounds.getHeight();
        g2.dispose();
    }

    private NoteEvent noteEvent;
    private String noteAsString;
    private boolean selected;
    private boolean muted;


    private PianoRollEditorSettings settings;
    private static final Logger LOGGER = Logger.getLogger(NoteView.class.getSimpleName());

    public NoteView(NoteEvent ne)
    {
        noteEvent = ne;
        settings = PianoRollEditorSettings.getDefault();
        updateGraphics(noteEvent);

        settings.addPropertyChangeListener(this);
    }

    public void setModel(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        if (noteEvent == ne)
        {
            return;
        }
        var old = noteEvent;
        noteEvent = ne;
        updateGraphics(noteEvent);
        repaint();
        firePropertyChange(PROP_MODEL, old, noteEvent);
    }

    public NoteEvent getModel()
    {
        return noteEvent;
    }

    /**
     * Select this NoteView.
     * <p>
     * Fire a PROP_SELECTED change event.
     *
     * @param b
     */
    public void setSelected(boolean b)
    {
        if (selected != b)
        {
            selected = b;
            updateGraphics(noteEvent);
            repaint();
            firePropertyChange(PROP_SELECTED, !b, b);
        }
    }

    public boolean isSelected()
    {
        return selected;
    }


    public boolean isMuted()
    {
        return muted;
    }

    public void setMuted(boolean muted)
    {
        this.muted = muted;
        updateGraphics(noteEvent);
        repaint();
    }


    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);        // Paint background
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        var r = Utilities.getUsableArea(this);

        if (r.height >= FONT_HEIGHT)
        {
            // Draw note            
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float xStr = r.x + 1;
            float yStr = r.y + r.height - 2;           // text baseline position
            Color c = selected ? Color.BLACK : COLOR_TEXT;
            g2.setColor(c);
            g2.setFont(FONT);
            g2.drawString(noteAsString, xStr, yStr);
        }
        
        g2.dispose();
    }

    public void cleanup()
    {
        settings.removePropertyChangeListener(this);
    }

    @Override
    public String toString()
    {
        return "NoteView[" + noteEvent + "]";
    }

    /**
     * Get a color which changes with velocity, red shade for higher value, blue shade for lower value.
     *
     * @param velocity
     * @return
     */
    static public Color getColor(int velocity)
    {
        Preconditions.checkArgument(velocity >= 0 && velocity <= 127);
        if (VELOCITY_COLORS == null)
        {
            computeVelocityColors();
        }
        return VELOCITY_COLORS[velocity];
    }


    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() instanceof PianoRollEditorSettings)
        {
            updateGraphics(noteEvent);
            repaint();
        }
    }

    // ==============================================================================================================
    // Comparable<NoteEvent> interface
    // ==============================================================================================================
    /**
     * Rely on NoteEvent.compareTo() but returns 0 only if noteEvent==ne.
     *
     * @param ne
     * @return
     */
    @Override
    public int compareTo(NoteEvent ne)
    {
        int res = noteEvent.compareTo(ne);
        if (res == 0)
        {
            int h = System.identityHashCode(noteEvent);
            int hNe = System.identityHashCode(ne);
            res = Integer.compare(h, hNe);
        }
        // LOGGER.severe("compareTo() -- ne=" + ne + " this=" + this + " res=" + res);
        return res;
    }

    // ==============================================================================================================
    // Private methods
    // ==============================================================================================================

    private void updateGraphics(NoteEvent ne)
    {
        Color bgColor = selected ? settings.getSelectedNoteColor() : getColor(ne.getVelocity());
        setBackground(bgColor);
        Color borderColor = getBorderColor(getBackground());
        setBorder(BorderFactory.createLineBorder(borderColor, 1));
        noteAsString = new Note(ne.getPitch()).toPianoOctaveString();
        String tt = noteAsString + " (" + ne.getPitch() + ") v=" + ne.getVelocity();
    }

    /**
     * Pre-calculate all the velocity colors from 0 to 127.
     */
    private static void computeVelocityColors()
    {
        BufferedImage img = new BufferedImage(128, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        Point2D start = new Point2D.Float(0, 0);
        Point2D end = new Point2D.Float(127, 0);
        float[] dist =
        {
            0.0f, 0.5f, 1.0f
        };
        Color[] colors =
        {
            new Color(2, 0, 252), new Color(128, 0, 126), new Color(255, 0, 0)
        };
        LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
        g2.setPaint(p);
        g2.fillRect(0, 0, 127, 1);
        VELOCITY_COLORS = new Color[128];
        for (int i = 0; i < 128; i++)
        {
            int cInt = img.getRGB(i, 0);
            VELOCITY_COLORS[i] = new Color(cInt);
        }
        g2.dispose();

    }

    /**
     * Compute the border color from the background color.
     *
     * @param bgColor
     * @return
     */
    private Color getBorderColor(Color bgColor)
    {
        return  HSLColor.changeLuminance(bgColor, -12);       // Darker
    }


}
