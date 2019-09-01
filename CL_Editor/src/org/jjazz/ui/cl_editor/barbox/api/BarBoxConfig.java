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
package org.jjazz.ui.cl_editor.barbox.api;

import java.util.*;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererFactory;

/**
 * A BarBoxConfig defines the BarRenderer types supported by a BarBox. It also says which BarRenderer types are active for one
 * BarBox.
 */
public class BarBoxConfig
{

    /**
     * The supported BarRenderer types.
     */
    private ArrayList<BarRendererFactory.Type> supportedBarRenderers = new ArrayList<>();
    /**
     * The currently active BarRenderer types.
     */
    private ArrayList<BarRendererFactory.Type> activeBarRenderers = new ArrayList<>();

    /**
     * Create a config with all BarRendererFactories active by default.
     *
     * @param brs BarRendererFactory.Type[]
     */
    public BarBoxConfig(BarRendererFactory.Type... brs)
    {
        if (brs == null || brs.length < 0)
        {
            throw new IllegalArgumentException("brs=" + brs);
        }
        Collections.addAll(supportedBarRenderers, brs);
        Collections.addAll(activeBarRenderers, brs);
    }

    public void setActiveBarRenderers(BarRendererFactory.Type... brTypes)
    {
        // Check that each passed bar renderer type is supported
        for (BarRendererFactory.Type type : brTypes)
        {
            if (!supportedBarRenderers.contains(type))
            {
                throw new IllegalArgumentException("type=" + type);
            }
        }
        activeBarRenderers.clear();
        Collections.addAll(activeBarRenderers, brTypes);
    }

    public List<BarRendererFactory.Type> getSupportedBarRenderers()
    {
        return supportedBarRenderers;
    }

    public List<BarRendererFactory.Type> getActiveBarRenderers()
    {
        return activeBarRenderers;
    }

    @Override
    public String toString()
    {
        return activeBarRenderers.toString();
    }

    /**
     * Return true if bbc has the same supported BarRenderers (order is not taken into account).
     *
     * @param bbc
     */
    public boolean hasSameSupportedBarRenderers(BarBoxConfig bbc)
    {
        if (supportedBarRenderers.size() != bbc.supportedBarRenderers.size())
        {
            return false;
        }
        for (BarRendererFactory.Type type : supportedBarRenderers)
        {
            if (!bbc.supportedBarRenderers.contains(type))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * True if both the supported and active BarRenderers are the same.
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof BarBoxConfig))
        {
            return false;
        }
        BarBoxConfig cfg = (BarBoxConfig) o;
        return cfg.supportedBarRenderers.equals(supportedBarRenderers)
                && cfg.activeBarRenderers.equals(activeBarRenderers);
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 67 * hash + (this.supportedBarRenderers != null ? this.supportedBarRenderers.hashCode() : 0);
        hash = 67 * hash + (this.activeBarRenderers != null ? this.activeBarRenderers.hashCode() : 0);
        return hash;
    }
}
