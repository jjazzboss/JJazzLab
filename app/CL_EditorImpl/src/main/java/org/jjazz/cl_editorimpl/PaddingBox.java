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
package org.jjazz.cl_editorimpl;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import org.jjazz.cl_editor.spi.BarBoxSettings;

/**
 * Used as replacement for BarBox with no functionality.
 */
public class PaddingBox extends JPanel implements PropertyChangeListener
{

    BarBoxSettings bbSettings;

    public PaddingBox(BarBoxSettings settings)
    {
        bbSettings = settings;
        setOpaque(true);
        setPreferredSize(new Dimension(10, 10));
        bbSettings.addPropertyChangeListener(this);
        setBackground(bbSettings.getPastEndColor());
    }

    public void cleanup()
    {
        bbSettings.removePropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(BarBoxSettings.PROP_BAR_PAST_END_COLOR))
        {
            setBackground((Color) evt.getNewValue());
        }
    }
}
