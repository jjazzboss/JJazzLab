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
import java.awt.dnd.InvalidDnDOperationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.TransferHandler;

/**
 * Our drag'n drop support to accept external Midi files dragged into a component.
 * <p>
 * Note that behaviour is different on MacOS: getSourceActions(), createTransferable(), exportDone() can be called several times during a
 * drag operation ! (only 1 for Win/Linux). Also on MacOS the support parameter is not always fully initialized on canImport(), is is fully
 * initialized only when importData() is called (see MidiFileDragInTransferHandler.java for example).
 */
public abstract class MidiFileDragInTransferHandler extends TransferHandler
{

    public static final ImageIcon DRAG_ICON = new ImageIcon(MidiFileDragInTransferHandler.class.getResource("resources/DragMidiIcon.png"));
    private static final Logger LOGGER = Logger.getLogger(MidiFileDragInTransferHandler.class.getSimpleName());

    @Override
    public boolean canImport(TransferHandler.TransferSupport support)
    {
        LOGGER.log(Level.FINE, "canImport() -- support={0}", support);
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
        // Special handling for MacOS: on Mac getMidiFile() returns null in canImport(), even if DataFlavor.javaFileListFlavor is supported.
        // On MacOS The TransferHandler.TransferSupport parameter is initialized only when importData() is called.
        File f = getMidiFile(support);
        if (org.jjazz.utilities.api.Utilities.isMac())
        {
            LOGGER.fine("canImport() MacOs ignoring getMidiFile() null value");
        } else if (f == null)
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
        LOGGER.log(Level.FINE, "importData() -- support={0}", support);
        // Need a single midi file
        File midiFile = getMidiFile(support);
        if (midiFile == null)
        {
            if (!org.jjazz.utilities.api.Utilities.isMac())
            {
                LOGGER.warning("importData() Unexpected null value for midiFile");
            }
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
            if (files != null && files.size() == 1 && files.get(0).getName().toLowerCase().endsWith(".mid"))
            {
                midiFile = files.get(0);
            } else
            {
                // Happens on MacOS files==null Issue #348
                // Looks like a known issue: https://stackoverflow.com/questions/49016784/dataflavor-javafilelistflavor-and-mac-os-x-clipboard
                // From another SO post: it seems that the TransferSupport object is valid on Mac only when importData() is called, not
                // when canImport() is called. 
                LOGGER.log(Level.FINE, "getMidiFile() Unexpected value for files={0}", files);
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
