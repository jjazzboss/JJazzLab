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
package org.jjazz.flatcomponents.api;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 *
 * A JLabel which automatically resize its font to fit the available width.
 * <p>
 * From Stackoverflow.
 */
public class FontResizingLabel extends JLabel
{

    public static final int MIN_FONT_SIZE = 3;
    public static final int MAX_FONT_SIZE = 240;
    private Graphics g;
    private int currFontSize = 0;

    public FontResizingLabel(String text)
    {
        super(text);
        currFontSize = this.getFont().getSize();

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                adaptLabelFont(FontResizingLabel.this);
            }
        });
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        this.g = g;
    }

    // ======================================================================================
    // Private methods
    // ======================================================================================
    private void adaptLabelFont(JLabel l)
    {
        if (g == null)
        {
            return;
        }
        currFontSize = this.getFont().getSize();

        Rectangle r = l.getBounds();
        r.x = 0;
        r.y = 0;
        int fontSize = Math.max(MIN_FONT_SIZE, currFontSize);
        Font f = l.getFont();

        Rectangle r1 = new Rectangle(getTextSize(l, l.getFont()));
        while (!r.contains(r1))
        {
            fontSize--;
            if (fontSize <= MIN_FONT_SIZE)
            {
                break;
            }
            r1 = new Rectangle(getTextSize(l, f.deriveFont(f.getStyle(), fontSize)));
        }

        Rectangle r2 = new Rectangle();
        while (fontSize < MAX_FONT_SIZE)
        {
            r2.setSize(getTextSize(l, f.deriveFont(f.getStyle(), fontSize + 1)));
            if (!r.contains(r2))
            {
                break;
            }
            fontSize++;
        }

        setFont(f.deriveFont(f.getStyle(), fontSize));
        repaint();
    }

    private Dimension getTextSize(JLabel l, Font f)
    {
        Dimension size = new Dimension();
        FontMetrics fm = g.getFontMetrics(f);
        size.width = fm.stringWidth(l.getText());
        size.height = fm.getHeight();
        return size;
    }

}
