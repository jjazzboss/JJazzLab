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
package org.jjazz.uiutilities.api;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A transferable for one file which supports DataFlavor.javaFileListFlavor.
 * <p>
 */
public class FileTransferable implements Transferable
{

    private final DataFlavor[] dataFlavors =
    {
        DataFlavor.javaFileListFlavor
    };
    private final File file;
    private static final Logger LOGGER = Logger.getLogger(FileTransferable.class.getSimpleName());

    /**
     * Build a transferable for the specified file.
     *
     * @param file Can be null
     */
    public FileTransferable(File file)
    {
        this.file = file;
    }


    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return file == null ? new DataFlavor[0] : dataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        LOGGER.log(Level.FINE, "isDataFlavorSupported() -- flavor={0}", flavor);
        return file == null ? false : flavor.equals(DataFlavor.javaFileListFlavor);
    }

    /**
     * Returns a list which contains only our file.
     *
     * @param df
     * @return
     * @throws UnsupportedFlavorException
     * @throws IOException
     */
    @Override
    public List<File> getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException
    {
        LOGGER.log(Level.FINE, "getTransferData()  df={0}", df);
        if (!df.equals(DataFlavor.javaFileListFlavor))
        {
            throw new UnsupportedFlavorException(df);
        }
        if (file == null)
        {
            throw new IOException("File not available");
        }
        return Arrays.asList(file);
    }

}
