/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.uiutilities.api;

import java.io.*;
import javax.swing.filechooser.*;

/**
 * A FileSystemView class that limits the file selections to a single root.
 * <p>
 * When used with the JFileChooser component the user will only be able to traverse the directories contained within the specified
 * root fill.
 * <p>
 * The "Look In" combo box will only display the specified root.
 * <p>
 * The "Up One Level" button will be disable when at the root.
 * <p>
 * Thanks to Rob Camick. This class is designed to be used once, for one ROOT directory. To limit the file chooser to search the
 * files in my Java JDK I used: <br>
 * File root = new File("c:/java/jdk6.7"); <br>
 * FileSystemView fsv = new SingleRootFileSystemView( root );<br>
 * JFileChooser chooser = new JFileChooser(fsv);
 */
public class SingleRootFileSystemView extends FileSystemView
{
    File root;
    File[] roots = new File[1];

    public SingleRootFileSystemView(File path)
    {
        super();

        try
        {
            root = path.getCanonicalFile();
            roots[0] = root;
        } catch (IOException e)
        {
            throw new IllegalArgumentException(e);   
        }

        if (!root.isDirectory())
        {
            String message = root + " is not a directory";
            throw new IllegalArgumentException(message);   
        }
    }

    @Override
    public File createNewFolder(File containingDir)
    {
        File folder = new File(containingDir, "New Folder");
        folder.mkdir();
        return folder;
    }

    @Override
    public File getDefaultDirectory()
    {
        return root;
    }

    @Override
    public File getHomeDirectory()
    {
        return root;
    }

    @Override
    public File[] getRoots()
    {
        return roots;
    }
}
