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
package org.jjazz.phrasetransform.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import javax.swing.event.SwingPropertyChangeSupport;

/**
 * Special properties for PhraseTransformer.
 * <p>
 * Supported property keys are defined at construction, and they must all have a default value defined.
 */
public class PtProperties extends Properties
{

    public static final String PROP_PROPERTY = "PropProp";

    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    /**
     * Create a properties object.
     *
     * @param defaultProperties The supported keys and their default values.
     */
    public PtProperties(Properties defaultProperties)
    {
        super(defaultProperties);
    }

    public PtProperties getCopy()
    {
        PtProperties res = new PtProperties(defaults);
        res.putAll(this);
        return res;
    }


    /**
     * Overridden to allow setting a value only for a supported key.
     *
     * @param key Character '=' is forbidden
     * @param value Character '=' is forbidden
     * @return
     * @throws IllegalArgumentException If key is not supported, or if an illegal character was used
     */
    @Override
    public Object setProperty​(String key, String value) throws IllegalArgumentException
    {
        checkArgument(defaults.get(key) != null && !key.contains("=") && !value.contains("="), "key=%s, value=%s", key, value);
        Object oldValue = getProperty(key);
        Object res = super.setProperty(key, value);
        pcs.firePropertyChange(PROP_PROPERTY, oldValue, value);
        return res;
    }

    /**
     * The list of properties for which a non-default value is used.
     *
     * @return
     */
    public List<String> getNonDefaultValueProperties()
    {
        return stringPropertyNames().stream()
                .filter(key -> !defaults.get(key).equals(getProperty(key)))
                .toList();
    }

    public Integer getPropertyAsInteger(String key)
    {
        return Integer.valueOf(getProperty(key));
    }

    public Boolean getPropertyAsBoolean(String key)
    {
        return Boolean.valueOf(getProperty(key));
    }

    public Float getPropertyAsFloat(String key)
    {
        return Float.valueOf(getProperty(key));
    }

    public void setProperty(String key, Integer value)
    {
        setProperty(key, value.toString());
    }

    public void setProperty(String key, Boolean value)
    {
        setProperty(key, value.toString());
    }

    public void setProperty(String key, Float value)
    {
        setProperty(key, value.toString());
    }

    /**
     * Save key/value pairs as a string.
     * <p>
     * Example: "key1=value1,key2=value2"
     *
     * @param keys
     * @return
     * @see PtProperties#setPropertiesFromString(java.lang.String)
     */
    public String saveAsString(List<String> keys)
    {
        StringJoiner joiner = new StringJoiner(",");
        getNonDefaultValueProperties().stream()
                .forEach(key -> joiner.add(key + "=" + getProperty(key)));
        return joiner.toString();
    }

    /**
     * Set some properties from a saved string.
     *
     * @param s
     * @see PtProperties#saveAsString(java.util.List)
     * @throws ParseException
     */
    public void setPropertiesFromString(String s) throws ParseException
    {
        checkNotNull(s);
        String strs[] = s.split(",");
        for (String str : strs)
        {
            String subStrs[] = str.split("=");
            if (subStrs.length != 2)
            {
                throw new ParseException("setPropertiesFromString() Invalid string str=" + str + " from s=" + s, 0);
            }
            try
            {
                setProperty(subStrs[0], subStrs[1]);
            } catch (IllegalArgumentException e)
            {
                throw new ParseException(e.getMessage(), 0);
            }
        }
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }


}
