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

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 * A JButton which can be "small" even when using FlatDarkLaf (FlatDarkLaf applies a minimum width of 72 to all buttons!).
 * <p>
 * See hack from FlatDarkLaf author, GitHub issue #364.
 *
 * TODO Next FlatDarkLaf version should disable the 72px minimum width enforcement when setMargins() was used, to be tested when available.
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
        // putClientProperty("JButton.buttonType", "roundRect");
        putClientProperty("JComponent.minimumWidth", 0);      // Hack from FlatDarkLaf author! See GitHub issue #364
    }
}
