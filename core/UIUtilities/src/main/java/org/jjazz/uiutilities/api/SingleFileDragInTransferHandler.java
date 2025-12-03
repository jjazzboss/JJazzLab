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
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.TransferHandler;
import org.jjazz.utilities.api.Utilities;

/**
 * A drag'n drop support to accept a single file via drag &amp; drop.
 * <p>
 * This class takes into account OS specific "glitches" of the Swing drag &amp; drop support : behaviour is different on MacOS, getSourceActions(),
 * createTransferable(), exportDone() can be called several times during a drag operation, but only once for Win/Linux. Also on MacOS the support parameter is
 * not always fully initialized on canImport(), it is fully initialized only when importData() is called.
 */
public abstract class SingleFileDragInTransferHandler extends TransferHandler
{

    public static final ImageIcon DRAG_MIDI_FILE_ICON = new ImageIcon(SingleFileDragInTransferHandler.class.getResource("resources/DragMidiIcon.png"));
    private static final Logger LOGGER = Logger.getLogger(SingleFileDragInTransferHandler.class.getSimpleName());

  
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

        // Need a single accepted file
        // Special handling for MacOS: on Mac getAcceptedFile() returns null in canImport(), even if DataFlavor.javaFileListFlavor is supported.
        // On MacOS The TransferHandler.TransferSupport parameter is initialized only when importData() is called.
        File f = getAcceptedFile(support, getAcceptedFileExtensions());
        if (org.jjazz.utilities.api.Utilities.isMac())
        {
            LOGGER.fine("canImport() MacOs ignoring getAcceptedFile() null value");
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
        // Need a single file
        File file = getAcceptedFile(support, getAcceptedFileExtensions());
        if (file == null)
        {
            if (!org.jjazz.utilities.api.Utilities.isMac())
            {
                LOGGER.warning("importData() Unexpected null value for file");
            }
            return false;
        }
        return importFile(file);
    }


    /**
     * @return True if import is enabled.
     */
    abstract protected boolean isImportEnabled();

    /**
     * Perform the import of the transfered file.
     *
     * @param file
     * @return True if import was successful
     */
    abstract protected boolean importFile(File file);
    
    /**
     * The accepted file extensions.
     * 
     * @return A list of strings such as "sng", "mid", etc.
     */
    abstract protected Collection<String> getAcceptedFileExtensions();

    /**
     * Check if TransferSupport contains a single file which matches one of the specified extensions.
     * <p>
     * NOTE: see comment, can return an empty File to avoid a Windows-specific problem.
     *
     * @param support
     * @param extensions List of file extensions such as "sng" or "mid"
     * @return Null if no accepted file found
     */
    static public File getAcceptedFile(TransferHandler.TransferSupport support, Collection<String> extensions)
    {        
        Transferable t = support.getTransferable();
        File file = null;
        try
        {
            List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
            if (files != null && files.size() == 1 && isAccepted(files.get(0), extensions))
            {
                file = files.get(0);
            } else
            {
                // Happens on MacOS files==null Issue #348
                // Looks like a known issue: https://stackoverflow.com/questions/49016784/dataflavor-javafilelistflavor-and-mac-os-x-clipboard
                // From another SO post: the TransferSupport object is valid on Mac only when importData() is called, not
                // when canImport() is called. 
                LOGGER.log(Level.FINE, "getAcceptedFile() Unexpected value for files={0}", files);
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
        return file;
    }

    // ============================================================================================================
    // Private methods
    // ============================================================================================================

    static private boolean isAccepted(File f, Collection<String> exts)
    {
        String fExt = Utilities.getExtension(f.getName());
        return exts.stream().anyMatch(ext -> ext.equalsIgnoreCase(fExt));
    }

}
