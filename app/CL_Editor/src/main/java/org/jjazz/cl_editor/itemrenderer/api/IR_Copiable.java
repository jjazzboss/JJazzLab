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
package org.jjazz.cl_editor.itemrenderer.api;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import org.jjazz.flatcomponents.api.CrossShape;

/**
 * ItemRenderers which can handle the copy mode implement this interface.
 */
public interface IR_Copiable
{

    /**
     * If true, ItemRenderer should represent the copy mode, possibly using the provided CopyIndicator.
     *
     * @param b
     */
    public void showCopyMode(boolean b);

    /**
     * Represent the CopyIndicator using a cross shape.
     */
    static public class CopyIndicator
    {

        private static final CrossShape crossShape = new CrossShape(3, 2);

        static public void drawCopyIndicator(Graphics2D g2)
        {
            g2.setStroke(new BasicStroke(1f));
            g2.setPaint(Color.WHITE);
            g2.fill(crossShape);
            g2.setPaint(Color.GRAY.darker());
            g2.draw(crossShape);
        }

        static public int getSideLength()
        {
            return crossShape.getBounds().width + 1;
        }
    }
}
