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
package org.jjazz.ui.colorsetmanager.api;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.List;
import org.jjazz.ui.colorsetmanager.ColorSetManagerImpl;
import org.openide.util.Lookup;

/**
 * Manage a set of consistent colors.
 */
public interface ColorSetManager
{

    final static String PROP_REF_COLORS_CHANGED = "RefColorsChanged";

    static ColorSetManager getDefault()
    {
        ColorSetManager result = Lookup.getDefault().lookup(ColorSetManager.class);
        if (result == null)
        {
            return ColorSetManagerImpl.getInstance();
        }
        return result;
    }

    /**
     * Get a specific reference color.
     *
     * @param index The index of the reference color.
     * @return
     */
    Color getReferenceColor(int index);

    /**
     * Get all the reference colors present in this color set.
     *
     * @return A list of all the reference colors.
     */
    List<Color> getReferenceColors();

    /**
     * Set a reference color at specified index.
     *
     * @param index Must be in the reference colors bounds.
     * @param c
     */
    void setReferenceColor(int index, Color c);

    /**
     * Return a color associated to an identifier. If identifier does not already exist in the set, we automatically associate a
     * new reference color to it and return it.
     *
     * @param id Upper/lower case is ignored.
     * @return
     */
    Color getColor(String id);

    /**
     * Reset the color associated to the specified identifier. So next call to getColor(id) will return a (possibly) new color.
     *
     * @param id An identifier which has been already used with getColor(id)
     */
    void resetColor(String id);

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
