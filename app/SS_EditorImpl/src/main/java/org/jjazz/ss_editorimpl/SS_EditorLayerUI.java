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
package org.jjazz.ss_editorimpl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.plaf.LayerUI;

/**
 * A LayerUI to show the playback point.
 */
public class SS_EditorLayerUI extends LayerUI<JComponent>
{

    public static final int SIDE = 4;
    private int playbackPointX;
    public static final Color COLOR_PLAYBACK_LINE = new Color(186, 34, 23);
    private static final Logger LOGGER = Logger.getLogger(SS_EditorLayerUI.class.getSimpleName());


    /**
     * Set the playback point.
     *
     * @param xPos If &lt; 0 show nothing
     */
    public void setPlaybackPoint(int xPos)
    {
        playbackPointX = xPos;
    }

    @Override
    public void paint(Graphics g, JComponent jc)
    {
        super.paint(g, jc);
        if (playbackPointX < 0)
        {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(COLOR_PLAYBACK_LINE);
        g2.drawLine(playbackPointX, 0, playbackPointX, jc.getHeight() - 1);
        
        var triangle = new Path2D.Float();
        triangle.moveTo(playbackPointX - SIDE, 0);
        triangle.lineTo(playbackPointX + SIDE, 0);
        triangle.lineTo(playbackPointX, 1.5f * SIDE);
        triangle.lineTo(playbackPointX - SIDE, 0);
        g2.fill(triangle);
        
        g2.dispose();
    }

}
