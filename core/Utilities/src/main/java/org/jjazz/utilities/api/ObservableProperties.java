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

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Properties which can be listened to.
 * <p>
 * A PropertyChangeEvent with name=propertyName is fired when property value is changed or set to null (i.e removed).
 *
 * @param <T>
 */
public class ObservableProperties<T> implements Serializable
{

    private Map<String, T> properties;
    private Object owner;
    private transient final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(ObservableProperties.class.getSimpleName());

    public ObservableProperties()
    {
        this(null);
    }

    /**
     *
     * @param owner Can be null
     */
    public ObservableProperties(Object owner)
    {
        this.owner = owner;
    }

    /**
     *
     * @param owner           Can be null
     * @param otherProperties
     */
    public ObservableProperties(Object owner, ObservableProperties<T> otherProperties)
    {
        set(otherProperties);
        this.owner = owner;
    }

    /**
     * The (optional) owner of this properties.
     *
     * @return Can be null
     */
    public Object getOwner()
    {
        return owner;
    }

    /**
     * Put a client property.
     * <p>
     * Fire a PropertyChangeEvent using propertyName.
     *
     * @param propertyName
     * @param value        If null the property is removed.
     */
    public void put(String propertyName, T value)
    {
        Preconditions.checkNotNull(propertyName);
        if (value == null)
        {
            if (properties != null)
            {
                T old = properties.remove(propertyName);
                if (old != null)
                {
                    firePropertyChange(propertyName, old, null);
                }
            }
        } else
        {
            if (properties == null)
            {
                properties = new HashMap<>();
            }
            T old = properties.get(propertyName);
            properties.put(propertyName, value);
            firePropertyChange(propertyName, old, value);
        }
    }

    /**
     * Get a client property.
     *
     * @param propertyName
     * @return Can be null.
     */
    public T get(String propertyName)
    {
        Preconditions.checkNotNull(propertyName);
        return get(propertyName, null);
    }

    /**
     * Get a client property.
     *
     * @param propertyName
     * @param defaultValue Default value to be returned if propertyName is not defined.
     * @return
     */
    public T get(String propertyName, T defaultValue)
    {
        Preconditions.checkNotNull(propertyName);
        T res = defaultValue;
        if (properties != null && properties.containsKey(propertyName))
        {
            res = properties.get(propertyName);
        }
        return res;
    }

    /**
     * Replace the current properties by the properties from otherProperties.
     * <p>
     * Fire 0, 1 or more client property change events as required.
     *
     * @param other
     */
    public final void set(ObservableProperties<T> other)
    {
        if (properties == null && other.properties == null)
        {
            return;
        }

        // Could be simplier but we want to minimize the number of property change events
        Set<String> processedProps = new HashSet<>();
        if (properties != null)
        {
            for (var prop : properties.keySet())
            {
                T v = other.properties == null ? null : other.properties.get(prop);
                put(prop, v);
                processedProps.add(prop);
            }
        }
        if (other.properties != null)
        {
            for (var prop : other.properties.keySet())
            {
                if (!processedProps.contains(prop))
                {
                    put(prop, other.properties.get(prop));
                }
            }
        }
    }

    /**
     * Get all property names.
     *
     * @return
     */
    public Set<String> getPropertyNames()
    {
        Set<String> res = new HashSet<>();
        if (properties != null)
        {
            res.addAll(properties.keySet());
        }
        return res;
    }

    /**
     * Remove all client properties.
     * <p>
     * This will fire one or more PropertyChangeEvents.
     */
    public void clear()
    {
        for (String prop : properties.keySet().toArray(String[]::new))
        {
            put(prop, null);  // this will fire an event
        }
    }


    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String property, PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(property, listener);
    }

    public void removePropertyChangeListener(String property, PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(property, listener);
    }

    // =============================================================================================
    // Private methods
    // =============================================================================================

    protected void firePropertyChange(String prop, T oldValue, T newValue)
    {
        pcs.firePropertyChange(prop, oldValue, newValue);
    }
}
