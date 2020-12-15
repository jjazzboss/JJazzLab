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

import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Helper methods to use ResourceBundles.
 */
public class ResUtil
{

    private static final Logger LOGGER = Logger.getLogger(ResUtil.class.getSimpleName());

    /**
     * Retrieve the resource string from key.
     * <p>
     * Assume that the resource bundle is in aClass.getPackageName()+"/Bundle".
     *
     * @param cl
     * @param key
     * @param params Optional parameters to be used if the resource string is a compound message
     * @return
     */
    static public String getString(Class<?> cl, String key, Object... params)
    {
        ResourceBundle bundle = getBundle(cl);
        String s = bundle.getString(key);
        String res = java.text.MessageFormat.format(s, params);
        return res;
    }

    /**
     * Look for a Bundle.properties in the same package than the specified class.
     *
     * @param cl
     * @return
     */
    static public ResourceBundle getBundle(Class<?> cl)
    {
        // LOGGER.severe("cl.getPackageName()=" + cl.getPackageName()+" module="+cl.getModule());
        return ResourceBundle.getBundle(cl.getPackageName().replace('.', '/') + "/Bundle", cl.getModule());
    }

}
