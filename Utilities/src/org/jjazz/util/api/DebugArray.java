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
package org.jjazz.util.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Special array used to track some changes (but not all of them, eg using iterators etc.).
 */
public class DebugArray<T> extends ArrayList<T>
{

    private String id = "";
    private static final Logger LOGGER = Logger.getLogger(DebugArray.class.getSimpleName());

    public DebugArray(Collection<? extends T> c)
    {
        super(c);
    }

    public void setDebugId(String id)
    {
        this.id = id;
    }

    @Override
    public void add(int index, T e)
    {
        LOGGER.log(Level.FINE, id + ":add(index,e) index={0} e={1}", new Object[]   //NOI18N
        {
            index, e
        });
        super.add(index, e);
    }

    @Override
    public boolean add(T e)
    {
        LOGGER.log(Level.FINE, id + ":add(e) e={0}", e);   //NOI18N
        return super.add(e);
    }

    @Override
    public T set(int index, T e)
    {
        LOGGER.log(Level.FINE, id + ":set(index, e) index={0} e={1}", new Object[]   //NOI18N
        {
            index, e
        });
        return super.set(index, e);
    }

    @Override
    public T remove(int index)
    {
        LOGGER.log(Level.FINE, id + ":remove(index) index={0}", index);   //NOI18N
        return super.remove(index);
    }

    @Override
    public boolean remove(Object o)
    {
        LOGGER.log(Level.FINE, id + ":remove(o) o={0}", o);   //NOI18N
        return super.remove(o);
    }

    @Override
    public void clear()
    {
        LOGGER.log(Level.FINE, id + ":clear()");   //NOI18N
        super.clear();
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        LOGGER.log(Level.FINE, id + ":addAll(c) c={0}", c);   //NOI18N
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c)
    {
        LOGGER.log(Level.FINE, id + ":addAll(index,c) index={0} c={1}", new Object[]   //NOI18N
        {
            index, c
        });
        return super.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        LOGGER.log(Level.FINE, id + ":removeAll(c) c={0}", c);   //NOI18N
        return super.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        LOGGER.log(Level.FINE, id + ":retainAll(c) c={0}", c);   //NOI18N
        return super.retainAll(c);
    }

}
