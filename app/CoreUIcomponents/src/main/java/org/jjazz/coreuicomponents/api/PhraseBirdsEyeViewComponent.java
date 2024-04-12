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
package org.jjazz.coreuicomponents.api;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase; 
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.uisettings.api.NoteColorManager;
import org.jjazz.uiutilities.api.HSLColor;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * A component to show a "bird's eye view" of whole or part of a Phrase.
 * <p>
 */
public class PhraseBirdsEyeViewComponent extends JPanel implements PropertyChangeListener
{

    private static final Color COLOR_LABEL_FOREGROUND = new Color(187, 187, 187);
    private static final Color COLOR_LABEL_BACKGROUND = new Color(51, 51, 51);
    public static final int MIN_HEIGHT = 8;
    public static final int MIN_WIDTH = 30;
    public static final int PREF_HEIGHT = 30;
    public static final int PREF_BAR_WIDTH = 10;
    public static final int BAR_GRADATION_LENGTH = 7;
    public static final int BEAT_GRADATION_LENGTH = 3;
    private static final IntRange MID_PITCH_RANGE = new IntRange(36, 84);
    private static final int OUT_OF_RANGE_PITCH_RATIO = 4;


    private Phrase phrase;
    private FloatRange beatRange;
    private TimeSignature timeSignature;
    private int showVelocityMode = 1;
    private float markerPos = -1;
    private String label;
    private Rectangle2D labelSize;
    private MouseMotionAdapter mouseDragListener;
    private static final NoteColorManager noteColorManager = NoteColorManager.getDefault();


    private static final Font FONT = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(10f);
    private static final Logger LOGGER = Logger.getLogger(PhraseBirdsEyeViewComponent.class.getSimpleName());


    public String getLabel()
    {
        return label;
    }

    /**
     * Add a label in the top left corner.
     *
     * @param label if null no label is shown
     */
    public void setLabel(String label)
    {
        this.label = label;
        labelSize = UIUtilities.getStringBounds(label, FONT);
        repaint();
    }

    /**
     * Set the position of the marker.
     *
     * @param pos If -1, marker is not shown.
     */
    public void setMarkerPosition(float pos)
    {
        markerPos = pos;
        repaint();
    }

    public float getMarkerPosition()
    {
        return markerPos;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        var r = UIUtilities.getUsableArea(this);


        if (phrase != null && !phrase.isEmpty())
        {
            int xMax = r.x + r.width - 1;
            int yMax = r.y + r.height - 1;
            double xRatio = r.width / beatRange.size();
            IntRange pitchRange = getViewablePitchRange(MID_PITCH_RANGE, OUT_OF_RANGE_PITCH_RATIO);
            double yRatio = (double) r.height / pitchRange.size();


            // Paint bar gradations if timeSignature is defined               
            final int STEP = 2;
            int nbBeats = (int) beatRange.size();
            float beatWidth = (float) r.width / nbBeats;
            if (timeSignature != null)
            {
                Color cBeat = HSLColor.changeLuminance(getBackground(), STEP);
                Color cBar = HSLColor.changeLuminance(getBackground(), 2 * STEP);

                for (int i = 0; i < nbBeats; i++)
                {
                    double x = r.x + i * beatWidth; //  - 0.5d;
                    boolean isBar = (i % timeSignature.getNbNaturalBeats()) == 0;
                    Color c = isBar ? cBar : cBeat;
                    g2.setColor(c);

                    // Don't draw gradations if too small
                    if ((!isBar && beatWidth > 4) // Too small for a beat
                            || (isBar && beatWidth > 1))     // Too smal for a bar too!
                    {
                        var line = new Line2D.Double(x, r.y, x, yMax);
                        g2.draw(line);
                    }
                }
            }


            // Paint marker 
            if (beatRange.contains(markerPos, true))
            {
                final float SIZE = 5;
                double x = r.x + (markerPos - beatRange.from) * beatWidth - 0.5d;


                var triangle = new Path2D.Float();
                triangle.moveTo(x - SIZE, r.y);
                triangle.lineTo(x + SIZE, r.y);
                triangle.lineTo(x, r.y + 2 * SIZE);
                triangle.lineTo(x - SIZE, r.y);

                Color cMarker = HSLColor.changeLuminance(getBackground(), 2 * STEP + 10);
                g2.setColor(cMarker);
                g2.fill(triangle);
            }


            // Draw a line segment for each note
            for (NoteEvent ne : phrase)
            {
                double x0 = r.x + (ne.getPositionInBeats() - beatRange.from) * xRatio;
                double x1 = r.x + (ne.getPositionInBeats() + ne.getDurationInBeats() - beatRange.from) * xRatio;
                double y = yMax - (getCorrectedPitch(ne.getPitch(), MID_PITCH_RANGE, OUT_OF_RANGE_PITCH_RATIO) - pitchRange.from) * yRatio;
//                if (x1 - x0 < 0.7d)
//                {
//                    x1 = x0+0.7d;
//                }
                var line = new Line2D.Double(x0, y, x1, y);
                g2.setColor(getNoteColor(ne));
                g2.draw(line);
            }


        } else
        {
            // Write "void" centered
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color c = noteColorManager.getNoteColor(getForeground(), 5);
            g2.setColor(c);
            g2.setFont(FONT);
            // Utilities.drawStringAligned(g2, this, "void", 1);
        }


        // Draw label with 
        if (label != null)
        {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color c = COLOR_LABEL_BACKGROUND;
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 170);
            g2.setColor(c);
            g2.fillRect(r.x, r.y, (int) Math.ceil(labelSize.getWidth()) + 2, (int) Math.ceil(labelSize.getHeight()));

            float x = r.x + 1;
            float yStr = r.y - (float) labelSize.getY() + 1;
            g2.setColor(COLOR_LABEL_FOREGROUND);
            g2.setFont(FONT);
            g2.drawString(label, x, yStr);
        }

    }

    /**
     * Overridden to also automatically enable mouse drag listener to initiate the drag (if th is not null) .
     *
     * @param th
     */
    @Override
    public void setTransferHandler(TransferHandler th)
    {
        super.setTransferHandler(th);

        if (mouseDragListener == null)
        {
            mouseDragListener = new MouseMotionAdapter()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    if (SwingUtilities.isLeftMouseButton(e))
                    {
                        getTransferHandler().exportAsDrag(PhraseBirdsEyeViewComponent.this, e, TransferHandler.COPY);
                        // Note that from now on this mouse drag listener won't be called anymore until DnD export operation is over
                    }
                }
            };
        }

        if (th != null)
        {
            addMouseMotionListener(mouseDragListener);
        } else
        {
            removeMouseMotionListener(mouseDragListener);
        }
    }


    public Phrase getModel()
    {
        return phrase;
    }


    /**
     * Get the show velocity mode.
     *
     * @return
     * @see #setShowVelocityMode(int)
     */
    public int getShowVelocityMode()
    {
        return showVelocityMode;
    }

    /**
     * Set the show velocity mode.
     *
     * @param showVelocityMode 0=don't show velocity. 1=use variation of the foreground color. 2=use different color shades.
     */
    public void setShowVelocityMode(int showVelocityMode)
    {
        Preconditions.checkArgument(showVelocityMode >= 0 && showVelocityMode <= 2, "showVelocityMode=%s", showVelocityMode);
        this.showVelocityMode = showVelocityMode;
        repaint();
    }

    /**
     * Set the Phrase model.
     *
     * @param model
     * @param ts        If null bar lines will not be drawn
     * @param beatRange Can't be an empty range
     */
    public void setModel(Phrase model, TimeSignature ts, FloatRange beatRange)
    {
        checkNotNull(beatRange);
        checkArgument(!beatRange.isEmpty(), "beatRange is empty");

        if (this.phrase != null)
        {
            this.phrase.removePropertyChangeListener(this);
        }
        this.phrase = model;
        if (this.phrase != null)
        {
            this.phrase.addPropertyChangeListener(this);
        }

        this.beatRange = beatRange;
        this.timeSignature = ts;
        repaint();
    }

    /**
     * The beat range shown in this component.
     *
     * @return
     */
    public FloatRange getBeatRange()
    {
        return beatRange;
    }

    /**
     * Set the beat range shown in this component.
     *
     * @param beatRange Must be consistent with the model Phrase/TimeSignature
     */
    public void setBeatRange(FloatRange beatRange)
    {
        checkNotNull(beatRange);
        checkArgument(!beatRange.isEmpty(), "beatRange is empty");

        if (!Objects.equal(this.beatRange, beatRange))
        {
            this.beatRange = beatRange;
            repaint();
        }
    }

    /**
     * Can be null.
     *
     * @return
     */
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }


    // ----------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == phrase)
        {
            if (!Phrase.isAdjustingEvent(evt.getPropertyName()))
            {
                repaint();
            }
        }
    }

    // ================================================================================
    // Private methods
    // ================================================================================

    /**
     * Map notes out of midRange pitch closer to midRange.
     * <p>
     * Mostly useful for some drums tracks with possibly very low and high notes.
     *
     * @param pitch
     * @param midRange             For ex. [36-84]
     * @param outOfRangePitchRatio
     * @return
     */
    private int getCorrectedPitch(int pitch, IntRange midRange, int outOfRangePitchRatio)
    {
        int res = pitch;
        if (!midRange.contains(pitch))
        {
            if (pitch > midRange.to)
            {
                res = midRange.to + 1 + Math.round((pitch - midRange.to + 1f) / outOfRangePitchRatio);
            } else
            {
                res = midRange.from - 1 - Math.round((midRange.from - 1f - pitch) / outOfRangePitchRatio);
            }
        }
        return res;
    }

    /**
     * The possible pitches taking into account getCorrectedPitch() effect.
     *
     * @param midRange
     * @param outOfRangePitchRatio
     * @return
     * @see getCorrectedPitch(int, IntRange, int)
     */
    private IntRange getViewablePitchRange(IntRange midRange, int outOfRangePitchRatio)
    {
        float HARD_LOW = 12;
        float HARD_HIGH = 108;
        int nbLowNotes = midRange.from <= HARD_LOW ? 0 : Math.round((midRange.from - HARD_LOW) / (float) outOfRangePitchRatio) + 1;
        int nbHighNotes = midRange.to >= HARD_HIGH ? 0 : Math.round((HARD_HIGH - midRange.to) / (float) outOfRangePitchRatio) + 1;
        return new IntRange(midRange.from - nbLowNotes, midRange.to + nbHighNotes);
    }

    private Color getNoteColor(NoteEvent ne)
    {
        Color res;
        if (!isEnabled())
        {
            res = getBackground().brighter();
        } else if (showVelocityMode == 0)
        {
            res = getForeground();
        } else if (showVelocityMode == 1)
        {
            // Make one color vary depending on velocity
            res = noteColorManager.getNoteColor(getForeground(), ne.getVelocity());
        } else
        {
            // Use several color shades depending on velocity
            res = noteColorManager.getNoteColor(ne.getVelocity());
        }

        return res;
    }


}
