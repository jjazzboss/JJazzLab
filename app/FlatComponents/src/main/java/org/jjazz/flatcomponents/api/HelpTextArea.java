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

import java.awt.Font;
import java.util.Locale;
import javax.swing.JTextArea;
import org.jjazz.utilities.api.Utilities;

/**
 * A TextArea for help texts : non editable, font is preset, not opaque.
 */
public class HelpTextArea extends JTextArea
{

    public HelpTextArea()
    {
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(false);
        setBackground(null);  // because setOpaque(false) seems not enough on Linux !?
        setBorder(null);

        if (Utilities.isLatin(Locale.getDefault()))
        {
            // Don't do this for chinese etc.
            setFont(new java.awt.Font("Arial", 0, 10));
        } else
        {
            Font f = getFont();
            setFont(getFont().deriveFont(f.getSize() - 1));     // Make it smaller
        }
    }
}
