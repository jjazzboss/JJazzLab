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
package org.jjazz.pianoroll;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.ui.utilities.api.HSLColor;
import org.jjazz.ui.utilities.api.StringMetrics;
import org.jjazz.uisettings.api.GeneralUISettings;

/**
 * The ruler panel that shows the beat position marks over the NotesPanel.
 */
public class RulerPanel extends javax.swing.JPanel
{

    private static final int BAR_TICK_LENGTH = 10;
    private static final Color COLOR_BAR_TICK = new Color(160, 160, 160);
    private static final Color COLOR_BEAT_TICK = new Color(90, 90, 90);
    private static final Color COLOR_BAR_FONT = new Color(176, 199, 220);
    private static final Color COLOR_BEAT_FONT = new Color(80, 80, 80);
    private static final Color COLOR_BACKGROUND = new Color(15, 29, 42);
    private final NotesPanel notesPanel;
    private final NotesPanel.XMapper xMapper;
    private final PianoRollEditorImpl editor;
    private final Font fontBar;
    private final Font fontBeat;
    private static final Logger LOGGER = Logger.getLogger(RulerPanel.class.getSimpleName());

    /**
     *
     * @param editor
     * @param notesPanel
     */
    public RulerPanel(PianoRollEditorImpl editor, NotesPanel notesPanel)
    {
        this.editor = editor;
        this.notesPanel = notesPanel;
        this.xMapper = notesPanel.getXMapper();

        setBackground(COLOR_BACKGROUND);

        fontBar = GeneralUISettings.getInstance().getStdFont().deriveFont(13f);
        fontBeat = fontBar.deriveFont(fontBar.getSize() - 3f);

        this.notesPanel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                LOGGER.log(Level.FINE, "RulerPanel.componentResized() -- notesPanel.getWidth()={0}", notesPanel.getWidth());
                revalidate();
                repaint();
            }
        });

    }

    @Override
    public Dimension getPreferredSize()
    {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        var bounds = new StringMetrics(g2, getFont()).getLogicalBoundsNoLeading("999");
        var pd = new Dimension(notesPanel.getWidth(), (int) (bounds.getHeight() + BAR_TICK_LENGTH));
        return pd;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);        // Honor the opaque property

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);


        // Prepare data
        int h = getHeight();
        int beatTickLength = BAR_TICK_LENGTH / 2;
        int subTickLength = beatTickLength - 2;
        float oneBeatPixelSize = xMapper.getOneBeatPixelSize();
        float subTickWidth = oneBeatPixelSize / 4;
        Color subTickColor = HSLColor.changeLuminance(COLOR_BEAT_TICK, -2);

        
        boolean paintSubTicks = oneBeatPixelSize > 60;


        // Draw ticks + bar/beat
        var tmapPosX = xMapper.getBeatsXPositions(null);
        for (Position pos : tmapPosX.navigableKeySet())
        {
            int x = tmapPosX.get(pos);

            // Draw tick
            Color c = pos.isFirstBarBeat() ? COLOR_BAR_TICK : COLOR_BAR_TICK;
            int tickLength = pos.isFirstBarBeat() ? BAR_TICK_LENGTH : beatTickLength;
            g2.setColor(c);
            g2.drawLine(x, 0, x, tickLength);


            // Draw subticks
            if (paintSubTicks)
            {
                g2.setColor(subTickColor);
                float xf = x;
                for (int i = 0; i < 3; i++)
                {
                    xf += subTickWidth;
                    int xfi = Math.round(xf);
                    g2.drawLine(xfi, 0, xfi, subTickLength);
                }
            }


            // Draw bar or beat number
            String str = null;
            Font f = fontBar;
            c = COLOR_BAR_FONT;
            if (pos.isFirstBarBeat())
            {
                str = String.valueOf(pos.getBar() + 1);
            } else if (paintSubTicks)
            {
                str = String.valueOf(pos.getBar() + 1) + "." + String.valueOf((int) pos.getBeat() + 1);
                f = fontBeat;
                c = COLOR_BEAT_FONT;
            }
            if (str != null)
            {
                float xStr = x;
                float yStr = h - 2;           // text baseline position
                g2.setColor(c);
                g2.setFont(f);
                g2.drawString(str, xStr, yStr);
            }

        }

        g2.dispose();
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================    

}
