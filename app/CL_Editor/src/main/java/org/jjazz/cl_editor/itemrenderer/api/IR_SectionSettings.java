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

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import org.openide.util.Lookup;


public interface IR_SectionSettings
{

    public static String PROP_FONT = "SectionFont";
    public static String PROP_FONT_COLOR = "SectionFontColor";

    public static IR_SectionSettings getDefault()
    {
        IR_SectionSettings result = Lookup.getDefault().lookup(IR_SectionSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);   
        }
        return result;
    }

    /**
     *
     * @param font If null this will restore default value.
     */
     void setFont(Font font);

    /**
     * The font used to represent the name of the section.
     *
     * @return
     */
     Font getFont();

    /**
     *
     * @param color If null this will restore the default value.
     */
     void setColor(Color color);

    /**
     * The color used to represent the name of the section.
     *
     * @return
     */
     Color getColor();

     void addPropertyChangeListener(PropertyChangeListener listener);

     void removePropertyChangeListener(PropertyChangeListener listener);
}
