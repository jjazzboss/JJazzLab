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

import java.beans.PropertyChangeListener;

public interface Zoomable
{

    public static String PROPERTY_ZOOM_X = "PropZoomX";
    public static String PROPERTY_ZOOM_Y = "PropZoomY";

    public enum Capabilities
    {
        X_ONLY, Y_ONLY, X_Y
    };

    public Capabilities getZoomCapabilities();

    /**
     * @return A value between 0 and 100 included.
     */
    int getZoomYFactor();

    /**
     * Set the Y zoom factor.
     * <p>
     * If value is changed fire a PROPERTY_ZOOM_Y change event.
     *
     * @param factor A value between 0 and 100 included.
     */
    void setZoomYFactor(int factor);

    /**
     * @return A value between 0 and 100 included.
     */
    int getZoomXFactor();

    /**
     * Set the X zoom factor.
     * <p>
     * If value is changed fire a PROPERTY_ZOOM_X change event.
     *
     * @param factor A value between 0 and 100 included.
     */
    void setZoomXFactor(int factor);

    /**
     * Listen to the PROPERTY_ZOOM properties.
     *
     * @param l
     */
    void addPropertyListener(PropertyChangeListener l);

    void removePropertyListener(PropertyChangeListener l);
}
