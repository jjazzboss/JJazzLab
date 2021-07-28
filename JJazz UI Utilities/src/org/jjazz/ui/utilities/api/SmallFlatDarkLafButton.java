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
package org.jjazz.ui.utilities.api;

import java.awt.Insets;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 * A small round JButton tested with FlatDarkLaf.
 */
public class SmallFlatDarkLafButton extends JButton
{
    public SmallFlatDarkLafButton()
    {
        super();
        updateProperties();
    }

    public SmallFlatDarkLafButton(Icon icon)
    {
        super(icon);
        updateProperties();
    }

    public SmallFlatDarkLafButton(String text)
    {
        super(text);
        updateProperties();
    }

    public SmallFlatDarkLafButton(Action a)
    {
        super(a);
        updateProperties();
    }

    public SmallFlatDarkLafButton(String text, Icon icon)
    {
        super(text, icon);
        updateProperties();
    }

    private void updateProperties()
    {
        putClientProperty("JButton.buttonType", "roundRect");
        putClientProperty("Button.margin", new Insets(2, 7, 2, 7));
    }
}
