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

import java.awt.Dimension;
import javax.swing.JPanel;

/**
 * A JPanel with a fixed preferred width.
 * <p>
 * Content might be not displayed correctly if too large.
 */
public class FixedPreferredWidthPanel extends JPanel
{

    private int fixedPreferredWidth;

    /**
     * Create a panel with preferred width = 100 pixels.
     */
    public FixedPreferredWidthPanel()
    {
        this(100);
    }

    
    public FixedPreferredWidthPanel(int prefWidth)
    {
        setFixedPreferredWidth(prefWidth);
    }

    /**
     * @return The fixed preferred width
     */
    public int getFixedPreferredWidth()
    {
        return fixedPreferredWidth;
    }

    /**
     * @param prefWidth The fixed preferred width to set
     */
    public final void setFixedPreferredWidth(int prefWidth)
    {
        if (prefWidth < 1)
        {
            throw new IllegalArgumentException("prefWidth=" + prefWidth);   
        }
        this.fixedPreferredWidth = prefWidth;
        invalidate();
    }

    @Override
    public Dimension getPreferredSize()
    {
        Dimension d = super.getPreferredSize();
        d.width = fixedPreferredWidth;
        return d;
    }
}
