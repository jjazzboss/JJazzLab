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
package org.jjazz.utilities.api;

import java.util.Collection;
import java.util.concurrent.Executor;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.AbstractLookup.Pair;
import org.openide.util.lookup.InstanceContent.Convertor;

/**
 * A special InstanceContent directly copied from the original source code (NB7), but which uses MutableInstanceContent internally
 * to allow handling of mutable objects. Note that the equals() and hashCode() functions of the stored objects will no longer be
 * used.
 * <p>
 * With the default InstanceContent implementation, this does not work, when stored objects are modified, objects removing may
 * fail because their hashCode() has changed.
 * <p>
 * Note that operations that use Convertor are not supported.
 */
public class MutableInstanceContent extends AbstractLookup.Content
{

    /**
     * Create a new, empty content.
     */
    public MutableInstanceContent()
    {
    }

    /**
     * Creates a content associated with an executor to handle dispatch of changes.
     *
     * @param notifyIn the executor to notify changes in
     * @since 7.16
     */
    public MutableInstanceContent(Executor notifyIn)
    {
        super(notifyIn);
    }

    /**
     * The method to add instance to the lookup with.
     *
     * @param inst instance
     */
    public final void add(Object inst)
    {
        addPair(new MutableSimpleItem<>(inst));
    }

    public final <T, R> void add(T inst, Convertor<T, R> conv)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove instance.
     *
     * @param inst instance
     */
    public final void remove(Object inst)
    {
        removePair(new MutableSimpleItem<>(inst));
    }

    public final <T, R> void remove(T inst, Convertor<T, R> conv)
    {
        throw new UnsupportedOperationException();
    }

    public final <T, R> void set(Collection<T> col, Convertor<T, R> conv)
    {
        throw new UnsupportedOperationException();
    }
}

/**
 * Instance of one item representing an object. The only difference with original source code is that we don't delegate to obj's
 * equals() and hashCode().
 */
class MutableSimpleItem<T> extends Pair<T>
{

    private T obj;

    /**
     * Create an item.
     *
     * @obj object to register
     */
    public MutableSimpleItem(T obj)
    {
        if (obj == null)
        {
            throw new NullPointerException();   
        }
        this.obj = obj;
    }

    /**
     * Tests whether this item can produce object of class c.
     */
    public boolean instanceOf(Class<?> c)
    {
        return c.isInstance(obj);
    }

    /**
     * Get instance of registered object. If convertor is specified then method InstanceLookup.Convertor.convertor is used and
     * weak reference to converted object is saved.
     *
     * @return the instance of the object.
     */
    public T getInstance()
    {
        return obj;
    }

    /**
     * Here we differ from original ! Use direct equality instead of obj's equals().
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof MutableSimpleItem)
        {
            // return obj.equals(((SimpleItem) o).obj);            
            return obj == ((MutableSimpleItem) o).obj;
        } else
        {
            return false;
        }
    }

    /**
     * Here we differ from original ! Use identity hash code.
     *
     * @param o
     * @return
     */
    @Override
    public int hashCode()
    {
        // return obj.hashCode();
        return System.identityHashCode(obj);

    }

    /**
     * An identity of the item.
     *
     * @return string representing the item, that can be used for persistance purposes to locate the same item next time
     */
    public String getId()
    {
        return "IL[" + obj.toString() + "]"; // NOI18N
    }

    /**
     * Getter for display name of the item.
     */
    public String getDisplayName()
    {
        return obj.toString();
    }

    /**
     * Method that can test whether an instance of a class has been created by this item.
     *
     * @param obj the instance
     * @return if the item has already create an instance and it is the same as obj.
     */
    protected boolean creatorOf(Object obj)
    {
        return obj == this.obj;
    }

    /**
     * The class of this item.
     *
     * @return the correct class
     */
    @SuppressWarnings("unchecked")
    public Class<? extends T> getType()
    {
        return (Class<? extends T>) obj.getClass();
    }
}
