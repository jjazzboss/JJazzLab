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
package org.jjazz.util;

/**
 * A map which uses Class object as keys. A subclass is considered as an equivalent key.
 */
@SuppressWarnings("unchecked")
public class SillyClassMap<V> extends SmallMap<Class<?>, V>
{

    /**
     * Overridden so that aClass key can be a subclass of the actual key.
     *
     * @param aClass
     * @return
     */
    @Override
    public V getValue(Class<?> aClass)
    {
        for (Class<?> cl : getKeys())
        {
            if (cl.isAssignableFrom(aClass))
            {
                int index = keys.indexOf(cl);
                return values.get(index);
            }
        }
        return null;
    }
}
