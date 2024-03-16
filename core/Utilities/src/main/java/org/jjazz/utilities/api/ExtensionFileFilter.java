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

import java.io.File;
import java.io.FilenameFilter;

/**
 * A simple file filter based on file extensions .
 */
public class ExtensionFileFilter implements FilenameFilter
{

    private String[] fileExtensions;

    /**
     *
     * @param extensions Strings such as "exe" or ".exe".
     */
    public ExtensionFileFilter(String[] extensions)
    {
        fileExtensions = extensions;
    }

    public ExtensionFileFilter(String extension, String... extensions)
    {
        fileExtensions = new String[extensions.length + 1];
        fileExtensions[0] = extension;
        System.arraycopy(extensions, 0, fileExtensions, 1, extensions.length);
    }

    /**
     *
     * @param dir
     * @param name
     * @return True if name ends with one of this filter extensions, ignoring case.
     */
    @Override
    public boolean accept(File dir, String name)
    {
        for (String ext : fileExtensions)
        {
            if (ext.charAt(0) != '.')
            {
                ext = "." + ext;
            }
            if (name.toLowerCase().endsWith(ext.toLowerCase()))
            {
                return true;
            }
        }
        return false;
    }
}
