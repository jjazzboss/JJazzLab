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
import javax.swing.JTextArea;
import javax.swing.UIManager;

/**
 * Our special text area for the wizard.
 * <p>
 * Reuse the JLabel font.
 */
public class WizardTextArea extends JTextArea
{

    public WizardTextArea()
    {
        Font labelFont = UIManager.getFont("Label.font");
        if (labelFont != null)
        {
            setFont(labelFont);
        }
        setBorder(null);
        setBackground(null);
        setOpaque(false);
        setLineWrap(true);
        setWrapStyleWord(true);
    }
}
