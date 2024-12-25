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
package org.jjazz.pianoroll;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.plaf.LayerUI;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;

/**
 * A LayerUI to show the selection rectangle over the notes and the playback point.
 */
public class NotesPanelLayerUI extends LayerUI<JComponent>
{
    
    private Rectangle rectangle;
    private int playbackPointX;
    
    private static final Color COLOR_BORDER_OUT = Color.DARK_GRAY;
    private static final Color COLOR_BORDER_IN = Color.WHITE;
    public static final Color COLOR_PLAYBACK_LINE = Color.WHITE;
    private final Color COLOR_BACKGROUND;
    private final boolean DEBUG_SHOW_COORDINATES = false;
    private static final Logger LOGGER = Logger.getLogger(NotesPanelLayerUI.class.getSimpleName());
    
    public NotesPanelLayerUI()
    {
        Color bg = PianoRollEditorSettings.getDefault().getSelectedNoteColor();
        COLOR_BACKGROUND = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 30);    // Semi transparent
    }

    /**
     * Set the selection rectangle.
     *
     * @param r If null does not show anything
     */
    public void setSelectionRectangle(Rectangle r)
    {
        rectangle = r;
    }
    
    public Rectangle getSelectionRectangle()
    {
        return rectangle;
    }

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
        
        Graphics2D g2 = null;
        
        if (rectangle != null)
        {
            g2 = (Graphics2D) g.create();
            g2.setColor(COLOR_BORDER_OUT);
            g2.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            g2.setColor(COLOR_BORDER_IN);
            g2.drawRect(rectangle.x + 1, rectangle.y + 1, rectangle.width - 2, rectangle.height - 2);
            g2.setColor(COLOR_BACKGROUND);
            g2.fillRect(rectangle.x + 2, rectangle.y + 2, rectangle.width - 4, rectangle.height - 4);
            
            if (DEBUG_SHOW_COORDINATES)
            {
                g2.setColor(Color.BLUE);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int xRight = rectangle.x + rectangle.width - 1;
                String s = rectangle.toString().substring(18);
                g2.drawString(s, xRight + 3, rectangle.y + 3);
            }
            
        }
        
        if (playbackPointX >= 0)
        {
            if (g2 == null)
            {
                g2 = (Graphics2D) g.create();
            }
            g2.setColor(COLOR_PLAYBACK_LINE);
            g2.drawLine(playbackPointX, 0, playbackPointX, jc.getHeight() - 1);
            
        }
        
        if (g2 != null)
        {
            g2.dispose();
        }
        
    }
    
}
