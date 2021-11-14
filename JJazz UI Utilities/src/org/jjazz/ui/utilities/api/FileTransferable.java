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
package org.jjazz.ui.utilities.api;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * A transferable for one or more files.
 */
public class FileTransferable implements Transferable
{

    private final DataFlavor[] dataFlavors =
    {
        DataFlavor.javaFileListFlavor
    };
    private final List<File> data;
    private static final Logger LOGGER = Logger.getLogger(FileTransferable.class.getSimpleName());


    /**
     * Build a transferable for the specified file(s).
     *
     * @param files Can be null.
     */
    public FileTransferable(List<File> files)
    {
        this.data = files;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return data == null ? new DataFlavor[0] : dataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        LOGGER.fine("isDataFlavorSupported() -- flavor=" + flavor); //NOI18N
        return data == null ? false : flavor.equals(DataFlavor.javaFileListFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException
    {
        LOGGER.fine("getTransferData()  df=" + df); //NOI18N
        if (!df.equals(DataFlavor.javaFileListFlavor))
        {
            return new UnsupportedFlavorException(df);
        }
        if (data == null)
        {
            throw new IOException("File not available");
        }
        return data;
    }

}
