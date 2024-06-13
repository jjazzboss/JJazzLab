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
package org.jjazz.utilities.spi;

import java.util.Comparator;
import java.util.List;
import org.openide.util.Lookup;
import org.jjazz.utilities.DiffImpl;

public interface DiffProvider<Type>
{

    /**
     * Use the first implementation present in the global lookup. If nothing found, use the default one.
     *
     * @return
     */
    public static DiffProvider<?> getDefault()
    {
        DiffProvider<?> result = Lookup.getDefault().lookup(DiffProvider.class);
        if (result == null)
        {
            return DiffImpl.getInstance();
        }
        return result;
    }

    List<Difference> diff(Type[] a, Type[] b,
            Comparator<Type> comp);

    List<Difference> diff(Type[] a, Type[] b);

    List<Difference> diff(List<Type> a,
            List<Type> b,
            Comparator<Type> comp);

    List<Difference> diff(List<Type> a,
            List<Type> b);
}
