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
package org.jjazz.phrase.api.ui;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.ui.utilities.api.HSLColor;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;

/**
 * A component to show a "bird's eye view" of whole or part of a Phrase.
 * <p>
 * Call repaint() to update the component if model has changed.
 */
public class PhraseBirdsEyeViewComponent extends JPanel
{

    public static final int MIN_HEIGHT = 10;
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
    private boolean showVelocity = true;
    private float markerPos = -1;
    private static final Font FONT = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(10f);
    private static final Logger LOGGER = Logger.getLogger(PhraseBirdsEyeViewComponent.class.getSimpleName());

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

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        var r = Utilities.getUsableArea(this);


        if (phrase != null && !phrase.isEmpty())
        {
            int xMax = r.x + r.width - 1;
            int yMax = r.y + r.height - 1;
            int nbBars = getSizeInBars();
            double xRatio = r.width / beatRange.size();
            IntRange pitchRange = getViewablePitchRange(MID_PITCH_RANGE, OUT_OF_RANGE_PITCH_RATIO);
            double yRatio = (double) r.height / pitchRange.size();


            // Paint bar gradations                
            final int STEP = 2;
            int nbBeats = (int) beatRange.size();
            float beatWidth = (float) r.width / nbBeats;
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
                if (x1 - x0 < 0.7d)
                {
                    x1 = x0 + 0.7d;
                }
                var line = new Line2D.Double(x0, y, x1, y);
                g2.setColor(getNoteColor(ne));
                g2.draw(line);
            }


        } else
        {
            // Write "void" centered
            g2.setFont(FONT);
            Utilities.drawStringAligned(g2, this, "void", 1);
        }

        g2.dispose();
    }


    public Phrase getModel()
    {
        return phrase;
    }


    /**
     *
     * @return If true use different color nuances depending on velocity.
     */
    public boolean isShowVelocity()
    {
        return showVelocity;
    }

    /**
     * If true use different color nuances depending on velocity.
     *
     * @param showVelocity the showVelocity to set
     */
    public void setShowVelocity(boolean showVelocity)
    {
        this.showVelocity = showVelocity;
        repaint();
    }

    /**
     * Set the Phrase model.
     *
     * @param model
     * @param ts
     * @param beatRange Can't be an empty range
     */
    public void setModel(Phrase model, TimeSignature ts, FloatRange beatRange)
    {
        checkNotNull(ts);
        checkNotNull(beatRange);
        checkArgument(!beatRange.isEmpty(), "beatRange is empty");

        this.phrase = model;
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

    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    /**
     * Get the number of bars corresponding to the beat range.
     *
     * @return Rounded to number of bars.
     */
    public int getSizeInBars()
    {
        return (int) Math.ceil(this.beatRange.size() / timeSignature.getNbNaturalBeats());
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
        } else if (!isShowVelocity())
        {
            res = getForeground();
        } else
        {
            // Make color vary depending on velocity
            // Use a luminance variation centered around velocity=64
            int v = ne.getVelocity();
            HSLColor hsl = new HSLColor(getForeground());
            float lum = hsl.getLuminance();
            int lumMaxDelta = 20;
            float lumDelta = (v - 64) * lumMaxDelta / 64;
            lum += lumDelta;
            lum = Math.min(100f, lum);
            lum = Math.max(0f, lum);
            res = hsl.adjustLuminance(lum);
        }

        return res;
    }
}
