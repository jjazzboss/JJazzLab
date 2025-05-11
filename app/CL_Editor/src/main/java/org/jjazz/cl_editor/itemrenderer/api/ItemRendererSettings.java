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
import java.beans.PropertyChangeListener;
import javax.swing.border.Border;
import org.openide.util.Lookup;

/**
 * Generic UI settings for any ItemRenderer.
 */
public interface ItemRendererSettings
{

    public static String PROP_ITEM_SELECTED_COLOR = "ItemSelectedColor";
    public static String PROP_ITEM_FOCUSED_BORDER_COLOR = "ItemFocusedBorderColor";

    public static ItemRendererSettings getDefault()
    {
        ItemRendererSettings result = Lookup.getDefault().lookup(ItemRendererSettings.class);
        if (result == null)
        {
            throw new NullPointerException("result=" + result);
        }
        return result;
    }

    default IR_ChordSymbolSettings getIR_ChordSymbolSettings()
    {
        return IR_ChordSymbolSettings.getDefault();
    }

    default IR_SectionSettings getIR_SectionSettings()
    {
        return IR_SectionSettings.getDefault();
    }

    default IR_AnnotationTextSettings getIR_AnnotationTextSettings()
    {
        return IR_AnnotationTextSettings.getDefault();
    }

    default IR_TimeSignatureSettings getIR_TimeSignatureSettings()
    {
        return IR_TimeSignatureSettings.getDefault();
    }

    void setSelectedBackgroundColor(Color color);

    Color getSelectedBackgroundColor();

    Border getFocusedBorder();

    Border getNonFocusedBorder();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
