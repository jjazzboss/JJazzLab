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
import org.jjazz.ui.utilities.api.StringMetrics;
import org.jjazz.uisettings.api.GeneralUISettings;

/**
 * The ruler panel that shows the beat position marks over the NotesPanel.
 */
public class RulerPanel extends javax.swing.JPanel
{

    private static final int BAR_TICK_LENGTH = 8;
    private static final Color COLOR_BAR_TICK = new Color(160, 160, 160);
    private static final Color COLOR_SUBBEAT_TICK = new Color(120, 120, 120);
    private static final Color COLOR_BEAT_TICK = new Color(90, 90, 90);
    private static final Color COLOR_BACKGROUND = new Color(15, 29, 42);
    private final NotesPanel notesPanel;
    private final NotesPanel.XMapper xMapper;
    private final PianoRollEditorImpl editor;
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

        Font font = GeneralUISettings.getInstance().getStdFont().deriveFont(12f);
        setFont(font);

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
        var pd = new Dimension(notesPanel.getWidth(), (int) (bounds.getHeight() + 3 + BAR_TICK_LENGTH));
        return pd;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);        // Honor the opaque property
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);


        // Don't draw outside the current clip
        Rectangle rClip = g2.getClipBounds();
        if (rClip != null)
        {
            int more = 20;
            rClip.x -= more;  // Make it a little larger on the left, so that e.g. the right part of a bar number can be seen
            rClip.width += more;
        }


        // Compute data
        int h = getHeight();
        int barTickLength = BAR_TICK_LENGTH;
        int beatTickLength = BAR_TICK_LENGTH / 2;
        int subBeatTickLength = beatTickLength - 2;


        // Draw ticks + bar/beat
        var tmapPosX = xMapper.getAllBeatsXPositions(editor.getVisibleBarRange());
        for (Position pos : tmapPosX.navigableKeySet())
        {
            int x = tmapPosX.get(pos);

            if (rClip != null && !rClip.contains(x, rClip.y))
            {
                continue;
            }

            // Draw tick
            Color c = pos.isFirstBarBeat() ? COLOR_BAR_TICK : COLOR_BEAT_TICK;
            int tickLength = pos.isFirstBarBeat() ? barTickLength : beatTickLength;
            g2.setColor(c);
            g2.drawLine(x, 0, x, tickLength);


            // Draw bar number
            if (pos.isFirstBarBeat())
            {
                String str = String.valueOf(pos.getBar() + 1);
                float xStr = x;
                float yStr = h - 2;           // text baseline position
                g2.drawString(str, xStr, yStr);
            }
        }
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================    

}
