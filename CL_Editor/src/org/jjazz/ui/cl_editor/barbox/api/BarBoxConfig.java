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

import static com.google.common.base.Preconditions.checkArgument;
import java.util.*;
import java.util.logging.Logger;

/**
 * A BarBoxConfig defines the BarRenderer types supported by a BarBox and the active ones.
 * <p>
 * This is an immutable class.
 */
public class BarBoxConfig
{

    /**
     * The supported BarRenderer types.
     */
    private final List<String> supportedBarRenderers = new ArrayList<>();
    /**
     * The active BarRenderer types.
     */
    private final List<String> activeBarRenderers = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(BarBoxConfig.class.getSimpleName());

    /**
     * Restricted.
     */
    private BarBoxConfig()
    {

    }

    /**
     * Create a config with all BarRenderer types active by default.
     *
     * @param brTypes Duplicate strings are ignored. No blank string allowed.
     */
    public BarBoxConfig(String... brTypes)
    {
        if (brTypes.length == 0)
        {
            throw new IllegalArgumentException("brs=" + brTypes);   //NOI18N
        }
        for (String brType : brTypes)
        {
            if (brType.isBlank())
            {
                throw new IllegalArgumentException("brTypse=" + Arrays.asList(brTypes));
            }
            if (!supportedBarRenderers.contains(brType))
            {
                supportedBarRenderers.add(brType);
                activeBarRenderers.add(brType);
            }
        }
    }


    /**
     * Return a new instance of this BarBoxConfig with only the specified BarRenderer types active.
     *
     * @param brTypes Duplicate strings are ignored
     * @return
     */
    public BarBoxConfig setActive(String... brTypes)
    {
        checkArgument(brTypes.length > 0, "brTypes.length=%s", brTypes.length);

        var res = new BarBoxConfig();
        res.supportedBarRenderers.addAll(supportedBarRenderers);


        for (String brType : brTypes)
        {
            if (!supportedBarRenderers.contains(brType))
            {
                throw new IllegalArgumentException("brType=" + brType);   //NOI18N
            }
            if (!res.activeBarRenderers.contains(brType))
            {
                res.activeBarRenderers.add(brType);
            }
        }

        return res;
    }

    public List<String> getSupportedBarRenderers()
    {
        return new ArrayList<>(supportedBarRenderers);
    }

    public List<String> getActiveBarRenderers()
    {
        return new ArrayList<>(activeBarRenderers);
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
     * @return
     */
    public boolean hasSameSupportedBarRenderers(BarBoxConfig bbc)
    {
        if (supportedBarRenderers.size() != bbc.supportedBarRenderers.size())
        {
            return false;
        }
        for (var type : supportedBarRenderers)
        {
            if (!bbc.supportedBarRenderers.contains(type))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true if bbc has the same active BarRenderers (order is not taken into account).
     *
     * @param bbc
     * @return
     */
    public boolean hasSameActiveBarRenderers(BarBoxConfig bbc)
    {
        if (activeBarRenderers.size() != bbc.activeBarRenderers.size())
        {
            return false;
        }
        for (var type : activeBarRenderers)
        {
            if (!bbc.activeBarRenderers.contains(type))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.supportedBarRenderers);
        hash = 67 * hash + Objects.hashCode(this.activeBarRenderers);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final BarBoxConfig other = (BarBoxConfig) obj;
        if (!Objects.equals(this.supportedBarRenderers, other.supportedBarRenderers))
        {
            return false;
        }
        if (!Objects.equals(this.activeBarRenderers, other.activeBarRenderers))
        {
            return false;
        }
        return true;
    }

    
    
}
