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

import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Helper methods to use ResourceBundles.
 * <p>
 */
public class ResUtil
{

    private static final Logger LOGGER = Logger.getLogger(ResUtil.class.getSimpleName());

    /**
     * Retrieve the resource string from key.
     * <p>
     * Assume that the resource bundle is in aClass.getPackageName()+"/Bundle*".
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
        String res = java.text.MessageFormat.format(s, params);   // Return OK even if é è IF ONLY it's not a lazy=true declarations

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

    /**
     * Get a translated string for common messages which are used at several places in the application.
     * <p>
     * See file resources/org/jjazz/utilities/api/Bundle.properties for the available common strings.
     *
     * @param key
     * @param params Optional parameters to be used if the resource string is a compound message
     * @return Can't be null
     */
    static public String getCommonString(String key, Object... params)
    {
        String res = getString(ResUtil.class, key, params);
        return (res == null) ? "<" + key + ">" : res;
        
        /**
         * IMPORTANT:
         * Trick so that running "check-resources.pl --remove-extra" does not remove the common strings defined in Bundle.properties:
         * 
         * ResUtil.getString(bla.class, "CTL_CL_ConfirmClose");
         * ResUtil.getString(bla.class, "ErrorLoadingSongFile");
         * ResUtil.getString(bla.class, "CTL_Cut");
         * ResUtil.getString(bla.class, "CTL_Copy");
         * ResUtil.getString(bla.class, "CTL_Paste");
         */
    }
}
