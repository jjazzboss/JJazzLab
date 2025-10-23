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

/**
 * A helper class which uses the default implementation for all get...() methods, in order to facilitate subclassing.
 */
public class ItemRendererSettingsAdapter implements ItemRendererSettings
{

    @Override
    public void setSelectedBackgroundColor(Color color)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Color getSelectedBackgroundColor()
    {
        return ItemRendererSettings.getDefault().getSelectedBackgroundColor();
    }

    @Override
    public Border getFocusedBorder()
    {
        return ItemRendererSettings.getDefault().getFocusedBorder();
    }

    @Override
    public Border getNonFocusedBorder()
    {
        return ItemRendererSettings.getDefault().getNonFocusedBorder();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        // Nothing
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        // Nothing
    }

}
