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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;

/**
 * A component to get a "bird view" of whole or part of a Phrase.
 * <p>
 * Call repaint() to update the component if model has changed.
 */
public class PhraseBirdView extends JPanel
{

    public static final int MIN_HEIGHT = 10;
    public static final int MIN_WIDTH = 30;
    public static final int PREF_HEIGHT = 30;
    public static final int PREF_BAR_WIDTH = 10;
    public static final int BAR_GRADATION_LENGTH = 3;
    private static final IntRange MID_RANGE = new IntRange(36, 84);
    private static final int OUT_OF_RANGE_PITCH_RATIO = 4;


    private Phrase phrase;
    private FloatRange beatRange;
    private TimeSignature timeSignature;
    private static final Logger LOGGER = Logger.getLogger(PhraseBirdView.class.getSimpleName());

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (phrase == null || !isEnabled())
        {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);


        var r = Utilities.getUsableArea(this);
        int xMax = r.x + r.width - 1;
        int yMax = r.y + r.height - 1;
        int nbBars = getSizeInBars();
        double xRatio = r.width / beatRange.size();
        IntRange pitchRange = getViewablePitchRange(MID_RANGE, OUT_OF_RANGE_PITCH_RATIO);
        double yRatio = (double) r.height / pitchRange.size();


        // Draw a line segment for each note
        for (NoteEvent ne : phrase)
        {
            double x0 = r.x + (ne.getPositionInBeats() - beatRange.from) * xRatio;
            double x1 = r.x + (ne.getPositionInBeats() + ne.getDurationInBeats() - beatRange.from) * xRatio;
            double y = yMax - (getCorrectedPitch(ne.getPitch(), MID_RANGE, OUT_OF_RANGE_PITCH_RATIO) - pitchRange.from) * yRatio;
            if (x1 - x0 < 0.7d)
            {
                x1 = x0 + 0.7d;
            }
            var line = new Line2D.Double(x0, y, x1, y);
            g2.draw(line);
        }


        // Paint bar gradations
        for (int i = 1; i < nbBars; i++)
        {
            float barWidth = (float) r.width / nbBars;
            double x = r.x + i * barWidth - 0.5d;
            Color c = getForeground().darker();
            g2.setColor(c);
            var line = new Line2D.Double(x, r.y, x, r.y + BAR_GRADATION_LENGTH - 1);
            g2.draw(line);
            line = new Line2D.Double(x, yMax, x, yMax - BAR_GRADATION_LENGTH + 1);
            g2.draw(line);
        }

        g2.dispose();
    }

    public Phrase getModel()
    {
        return phrase;
    }
    
    

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(500,50);
//        int sizeInBars = phrase != null ? getSizeInBars() : 2;
//        int w = (int) (PREF_BAR_WIDTH * (0.7f + 0.4f * sizeInBars));
//        int h = PREF_HEIGHT;
//        return new Dimension(w, h);
    }

//    @Override
//    public Dimension getMinimumSize()
//    {
//        return new Dimension(20, 10);
//    }

    /**
     * Set the Phrase model.
     *
     * @param model
     * @param ts
     * @param beatRange Can't be an empty range
     */
    public void setModel(Phrase model, TimeSignature ts, FloatRange beatRange)
    {
        checkNotNull(model);
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
     * Map notes out of midRange closer to midRange.
     *
     * @param pitch
     * @param midRange For ex. [36-84]
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
}
