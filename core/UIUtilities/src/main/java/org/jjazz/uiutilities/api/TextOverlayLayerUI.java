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
package org.jjazz.uiutilities.api;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.plaf.LayerUI;

/**
 * A LayerUI that display a centered text over the view component using a semi-transparent background.
 */
public class TextOverlayLayerUI extends LayerUI<JComponent>
{

    String text;

    /**
     * Create the overlay in invisible state (text is null).
     */
    public TextOverlayLayerUI()
    {
        this(null);
    }

    /**
     * Create the overlay with the specified text.
     */
    public TextOverlayLayerUI(String text)
    {
        setText(text);
    }

    /**
     * The text to be displayed on a semi-transparent background over the view component.
     * <p>
     * NOTE: caller should call repaint() after having called setText().
     *
     * @param text If null nothing is shown (overlay is invisible). If text contains '\n', text is shown a multiple lines.
     */
    public final void setText(String text)
    {
        this.text = text;
    }

    /**
     * The displayed text.
     *
     * @return If null it means the overlay is invisible.
     */
    public String getText()
    {
        return text;
    }

    @Override
    public void paint(Graphics g, JComponent jc)
    {
        super.paint(g, jc);
        if (text == null)
        {
            return;
        }


        Graphics2D g2 = (Graphics2D) g;
        int w = jc.getWidth();
        int h = jc.getHeight();


        // Semi-transparent background
        Color bg = jc.getBackground();
        Color newBg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 220);
        g2.setColor(newBg);
        g2.fillRect(0, 0, w, h);
        g2.setColor(Color.WHITE);


        // Write text
        g2.setFont(g2.getFont().deriveFont(Font.BOLD));
        UIUtilities.drawStringAligned(g2, jc, text, 1);
    }

}
