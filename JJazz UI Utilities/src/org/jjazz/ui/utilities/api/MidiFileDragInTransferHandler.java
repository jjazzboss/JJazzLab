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
import java.awt.dnd.InvalidDnDOperationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.TransferHandler;

/**
 * Our drag'n drop support to accept external Midi files dragged into a component.
 */
public abstract class MidiFileDragInTransferHandler extends TransferHandler
{

    private static final Logger LOGGER = Logger.getLogger(MidiFileDragInTransferHandler.class.getSimpleName());

    @Override
    public boolean canImport(TransferHandler.TransferSupport support)
    {
        LOGGER.fine("MidiFileDragInTransferHandler.canImport() -- support=" + support); 
        if (!isImportEnabled() || !support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        {
            return false;
        }
        
        // Copy mode must be supported
        if ((COPY & support.getSourceDropActions()) != COPY)
        {
            return false;
        }
        
        // Need a single midi file
        if (getMidiFile(support) == null)
        {
            return false;
        }
        
        // Use copy drop icon
        support.setDropAction(COPY);
        return true;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support)
    {
        // Need a single midi file
        File midiFile = getMidiFile(support);
        if (midiFile == null)
        {
            return false;
        }
        return importMidiFile(midiFile);
    }

    /**
     *
     * @param support
     * @return Null if no valid Midi file found
     */
    private File getMidiFile(TransferHandler.TransferSupport support)
    {
        Transferable t = support.getTransferable();
        File midiFile = null;
        try
        {
            List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
            if (files.size() == 1 && files.get(0).getName().toLowerCase().endsWith(".mid"))
            {
                midiFile = files.get(0);
            }
        } catch (UnsupportedFlavorException | IOException e)
        {
            return null;
        } catch (InvalidDnDOperationException dontCare)
        {
            // We wish to test the content of the transfer data and
            // determine if they are (a) files and (b) files we are
            // actually interested in processing. So we need to
            // call getTransferData() so that we can inspect the file names.
            // Unfortunately, this will not always work.
            // Under Windows, the Transferable instance
            // will have transfer data ONLY while the mouse button is
            // depressed.  However, when the user releases the mouse
            // button, the method canImport() will be called one last time by the drop() method.  And
            // when this method attempts to getTransferData, Java will throw
            // an InvalidDnDOperationException.  Since we know that the
            // exception is coming, we simply catch it and ignore it.
            // Note that the same operation in importData() will work OK.
            // See https://coderanch.com/t/664525/java/Invalid-Drag-Drop-Exception
            return new File(""); // Trick to not make canImport() fail
        }
        return midiFile;
    }

    /**
     * @return True if import is enabled.
     */
    abstract protected boolean isImportEnabled();

    /**
     * Perform the import of the transfered Midi file.
     *
     * @param midiFile
     * @return True if import was successful
     */
    abstract protected boolean importMidiFile(File midiFile);

}
