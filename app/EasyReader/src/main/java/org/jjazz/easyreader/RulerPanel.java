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
package org.jjazz.easyreader;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jjazz.harmony.api.Position;
import org.jjazz.song.api.BeatIterator;
import org.jjazz.song.api.Song;
import org.jjazz.uiutilities.api.UIUtilities;

/**
 * The ruler panel.
 */
public class RulerPanel extends JPanel
{

    private static final int BAR_TICK_LENGTH = 8;
    private Song song;
    private Position posFrom;
    private Position posTo;
    private int nbNaturalBeats;
    private Position currentPos;
    private int chordX;
    private int nextChordX;
    private final JLabel lbl_marker;
    private static final Logger LOGGER = Logger.getLogger(RulerPanel.class.getSimpleName());

    public RulerPanel()
    {
        lbl_marker = new JLabel("x");
    }

    public void setSong(Song song)
    {
        this.song = song;
    }

    /**
     * Set the position range displayed.
     *
     * @param posFrom        beat must be an int
     * @param posTo          beat must be an int. Can be null
     * @param nbNaturalBeats The nb of natural beats between posFrom and posTo. Ignored if posTo==null.
     */
    public void setPositionRange(Position posFrom, Position posTo, int nbNaturalBeats)
    {
        if (posFrom.getBeat() - (int) posFrom.getBeat() != 0 || posTo.getBeat() - (int) posTo.getBeat() != 0)
        {
            throw new IllegalArgumentException("posFrom=" + posFrom + " posTo=" + posTo);
        }
        // LOGGER.severe("setPositionRange() posFrom=" + posFrom + " posTo=" + posTo + " nbNaturalBeats=" + nbNaturalBeats);
        this.posFrom = posFrom;
        this.posTo = posTo;
        this.nbNaturalBeats = nbNaturalBeats;
        this.chordX = 0;
        this.nextChordX = 10;
        this.currentPos = posFrom;

        repaint();
    }

    /**
     *
     * @param pos If null marker is hidden
     */
    public void setMarkerPosition(Position pos)
    {
        // LOGGER.severe("setMarkerPosition() pos=" + pos);
        this.currentPos = pos;
        if (currentPos==null)
        {
            remove(lbl_marker);
            repaint();
        } else if (getComponentCount()==0)
        {
            // First time, add label            
            add(lbl_marker);
            revalidate();
        }
    }

    // @Override
    public void paintComponentXXX(Graphics g)
    {
        super.paintComponent(g);

        if (song == null || posFrom == null)
        {
            return;
        }

        BeatIterator beatIterator = new BeatIterator(song, posFrom);

        Rectangle r = UIUtilities.getUsableArea(this);
        chordX = r.x;
        int beatIndex = 0;
        float beatSizeInPixels = ((float) r.width) / nbNaturalBeats;


        int x = r.x;
        int y = r.y;


        // Initial graduation mark
        g.drawLine(x, y, x, y + getTickLength(posFrom) - 1);


        while (beatIterator.hasNext())
        {
            beatIndex++;
            x = Math.round(r.x + beatIndex * beatSizeInPixels);
            Position pos = beatIterator.next();
            if (posTo.compareTo(pos) == 0)
            {
                nextChordX = x;
            } else if (posTo.compareTo(pos) < 0)
            {
                break;
            }

            g.drawLine(x, y, x, y + getTickLength(pos) - 1);

            if (pos.equals(currentPos))
            {
                // Draw a mark
                g.drawLine(x - 2, y + 2, x + 2, y + 2);
            }
        }
    }

    public int getChordX()
    {
        return chordX;
    }

    public int getNextChordX()
    {
        return nextChordX;
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================

    private int getTickLength(Position pos)
    {
        return pos.isFirstBarBeat() ? BAR_TICK_LENGTH : BAR_TICK_LENGTH / 2;
    }

   

}
