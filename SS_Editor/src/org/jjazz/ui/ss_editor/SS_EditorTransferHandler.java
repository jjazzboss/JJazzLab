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
package org.jjazz.ui.ss_editor;

import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY;
import static javax.swing.TransferHandler.MOVE;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.ui.sptviewer.api.SptViewer;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.api.ResUtil;

/**
 * Drag n Drop Transfer handler for Section/SongParts.
 * <p>
 * Drag n Drop between different SongStructures is not supported.
 */
public class SS_EditorTransferHandler extends TransferHandler
{

    private SS_Editor editor;
    private static final Logger LOGGER = Logger.getLogger(SS_EditorTransferHandler.class.getSimpleName());

    public SS_EditorTransferHandler(SS_Editor ed)
    {
        if (ed == null)
        {
            throw new NullPointerException("ed");   //NOI18N
        }
        editor = ed;
    }

    /**
     * We support both copy and move actions.
     */
    @Override
    public int getSourceActions(JComponent c)
    {
        LOGGER.fine("getSourceActions()  c" + c);   //NOI18N
        return TransferHandler.COPY_OR_MOVE;
    }

    @Override
    public Transferable createTransferable(JComponent c)
    {
        LOGGER.fine("createTransferable()  c" + c);   //NOI18N
        if (c instanceof SptViewer)
        {
            SptViewer sptv = (SptViewer) c;
            return sptv.getModel();
        } else
        {
            return null;
        }
    }

    /**
     *
     * @param c
     * @param data
     * @param action
     */
    @Override
    protected void exportDone(JComponent c, Transferable data, int action)
    {
        LOGGER.fine("exportDone()  c=" + c + " data=" + data + " action=" + action);   //NOI18N
        // Will be called if drag was initiated from this handler
        editor.showSptInsertionMark(false, 0, false);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info)
    {
        LOGGER.fine("canImport() info.getComponent()=" + info.getComponent());   //NOI18N


        // Check data flavor and origin of the transfert
        if (info.isDataFlavorSupported(SongPart.DATA_FLAVOR))
        {
            SongPart spt = getTransferredSpt(info.getTransferable());
            if (spt.getContainer() != editor.getModel())
            {
                return false;
            }
        } else if (info.isDataFlavorSupported(CLI_Section.DATA_FLAVOR))
        {
            CLI_Section section = getTransferredSection(info.getTransferable());
            if (section.getContainer() != editor.getModel().getParentChordLeadSheet())
            {
                return false;
            }
        } else
        {
            LOGGER.fine("-- unsupported DataFlavor");   //NOI18N
            return false;
        }


        // Check if the source actions (a bitwise-OR of supported actions)
        // contains the COPY or MOVE action
        boolean copySupported = (COPY & info.getSourceDropActions()) == COPY;
        boolean moveSupported = (MOVE & info.getSourceDropActions()) == MOVE;
        if (!copySupported && !moveSupported)
        {
            LOGGER.fine("-- copy or move not supported");   //NOI18N
            return false;
        }
        // Force modes if not supported by source
        if (!moveSupported)
        {
            info.setDropAction(COPY);
        } else if (!copySupported)
        {
            info.setDropAction(MOVE);
        }
        // Exception : if importing a section, it's a copy
        if (info.isDataFlavorSupported(CLI_Section.DATA_FLAVOR))
        {
            info.setDropAction(COPY);
        }

        // Show the insertion point
        int targetSptIndex = getInsertionSptIndex(info);
        editor.showSptInsertionMark(true, targetSptIndex, (info.getDropAction() & COPY) == COPY);
        return true;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info)
    {
        LOGGER.fine("importData() info.getComponent()=" + info.getComponent());   //NOI18N
        if (!info.isDrop())
        {
            LOGGER.fine("--- not a drop");   //NOI18N
            return false;
        }
        if (!canImport(info))
        {
            LOGGER.fine("--- can't import");   //NOI18N
            return false;
        }

        // Fetch the Transferable and its data
        boolean b = false;
        if (info.isDataFlavorSupported(SongPart.DATA_FLAVOR))
        {
            b = importSongPart(info);
        } else if (info.isDataFlavorSupported(CLI_Section.DATA_FLAVOR))
        {
            b = importSection(info);
        } else
        {
            LOGGER.warning("importData() unexpected data flavor=" + info.getDataFlavors());   //NOI18N
        }
        LOGGER.fine("--- exited with b=" + b);   //NOI18N
        return b;
    }

    // ==================================================================================
    // Private methods
    // ==================================================================================
    /**
     * Copy or move a songPart.
     *
     * @param info
     * @return
     */
    private boolean importSongPart(TransferSupport info)
    {
        SongPart spt = getTransferredSpt(info.getTransferable());
        assert spt != null;   //NOI18N

        // Calculate insertion point
        int targetSptIndex = getInsertionSptIndex(info);
        assert targetSptIndex != -1;   //NOI18N

        SongStructure sgs = editor.getModel();
        List<SongPart> spts = sgs.getSongParts();
        int sptIndex = spts.indexOf(spt);


        // Special case: move to same place, do nothing
        if (info.getDropAction() == MOVE && (targetSptIndex == sptIndex || targetSptIndex == sptIndex + 1))
        {
            return false;
        }


        // In all cases we need a spt clone
        SongPart newSpt;
        int newStartBarIndex = 0;
        if (targetSptIndex > 0)
        {
            SongPart prevSpt = spts.get(targetSptIndex - 1);
            newStartBarIndex = prevSpt.getStartBarIndex() + prevSpt.getNbBars();
        }
        newSpt = spt.clone(spt.getRhythm(), newStartBarIndex, spt.getNbBars(), spt.getParentSection());


        // Unselect all, restore at the end
        SS_SelectionUtilities selection = new SS_SelectionUtilities(editor.getLookup());
        selection.unselectAll(editor);


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        if (info.getDropAction() == MOVE)
        {
            // Move the SongPart
            LOGGER.log(Level.FINE, "importSongPart() MOVE spt=" + spt + " newSpt=" + newSpt);   //NOI18N
            um.startCEdit(ResUtil.getString(getClass(), "CTL_MoveSpt"));
            try
            {
                // Add to be done first !
                // Because startBarIndex will change, and newSpt can be a source rhythm of an AdaptedRhythm
                editor.getModel().addSongParts(Arrays.asList(newSpt));
                editor.getModel().removeSongParts(Arrays.asList(spt));
            } catch (UnsupportedEditException ex)
            {
                // Should never happen : we just use existing SongParts with the same song
                String msg = ResUtil.getString(getClass(),"ERR_CantMoveSongPart");
                msg += "\n" + ex.getLocalizedMessage();
                um.handleUnsupportedEditException(ResUtil.getString(getClass(), "CTL_MoveSpt"), msg);
                return false;
            }
            um.endCEdit(ResUtil.getString(getClass(), "CTL_MoveSpt"));
        } else
        {
            // Copy
            LOGGER.log(Level.FINE, "importSongPart() COPY newSpt=" + newSpt);   //NOI18N
            um.startCEdit(ResUtil.getString(getClass(), "CTL_CopySpt"));
            try
            {
                editor.getModel().addSongParts(Arrays.asList(newSpt));
            } catch (UnsupportedEditException ex)
            {
                // No new rhythm, so we should never be here
                String msg = ResUtil.getString(getClass(),"ERR_CantCopy");
                msg += "\n" + ex.getLocalizedMessage();
                um.handleUnsupportedEditException(ResUtil.getString(getClass(), "CTL_CopySpt"), msg);
                return false;
            }
            um.endCEdit(ResUtil.getString(getClass(), "CTL_CopySpt"));
        }

        // Select the target spt
        editor.selectSongPart(newSpt, true);
        editor.setFocusOnSongPart(newSpt);

        // ExportDone() will be called after
        return true;
    }

    private boolean importSection(TransferSupport info)
    {
        assert info.getDropAction() == COPY;    // Can't move a CL_Editor section here !   //NOI18N

        CLI_Section parentSection = getTransferredSection(info.getTransferable());
        assert parentSection != null;   //NOI18N

        SS_SelectionUtilities selection = new SS_SelectionUtilities(editor.getLookup());
        selection.unselectAll(editor);

        // Calculate insertion point
        int targetSptIndex = getInsertionSptIndex(info);

        // Add a new SongPart
        SongPart newSpt = addSongPartFromSection(parentSection, targetSptIndex);
        if (newSpt == null)
        {
            return false;
        }

        // Select the target spt
        editor.selectSongPart(newSpt, true);
        editor.setFocusOnSongPart(newSpt);

        // Don't rely on our exportDone(): it is used only for *export*, i.e. it is the one from CL_Editor TransferHandler
        // which will be called when drag from CL_Editor to here.
        editor.showSptInsertionMark(false, 0, false);
        return true;
    }

    /**
     *
     * @param t
     * @return Null if problem.
     */
    private CLI_Section getTransferredSection(Transferable t)
    {
        CLI_Section section = null;
        try
        {
            section = (CLI_Section) t.getTransferData(CLI_Section.DATA_FLAVOR);
        } catch (UnsupportedFlavorException | IOException ex)
        {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);   //NOI18N
        }
        return section;
    }

    private SongPart getTransferredSpt(Transferable t)
    {
        SongPart spt = null;
        try
        {
            spt = (SongPart) t.getTransferData(SongPart.DATA_FLAVOR);
        } catch (UnsupportedFlavorException | IOException ex)
        {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);   //NOI18N
        }
        return spt;
    }

    /**
     *
     * @param info
     * @return
     */
    private int getInsertionSptIndex(TransferSupport info)
    {
        Point editorPoint = info.getDropLocation().getDropPoint();
        editorPoint = SwingUtilities.convertPoint(info.getComponent(), editorPoint, editor);
        AtomicBoolean leftSpt = new AtomicBoolean();
        SongPartParameter sptp = editor.getSongPartParameterFromPoint(editorPoint, leftSpt);
        SongStructure sgs = editor.getModel();
        if (sptp.getSpt() != null)
        {
            return sgs.getSongParts().indexOf(sptp.getSpt()) + (leftSpt.get() ? 0 : 1);
        } else
        {
            return 0;
        }
    }

    /**
     * Create a new SongPart corresponding to specified section.
     * <p>
     * Adapt RP values from previous SongPart.
     *
     * @param parentSection
     * @param sptIndex
     * @return The added SongPart or null if problem
     */
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    private SongPart addSongPartFromSection(CLI_Section parentSection, int sptIndex)
    {
        SongStructure sgs = editor.getModel();
        List<SongPart> spts = sgs.getSongParts();

        // Prepare data
        SongPart prevSpt = (sptIndex == 0) ? null : spts.get(sptIndex - 1);
        int startBarIndex = (prevSpt == null) ? 0 : prevSpt.getBarRange().to + 1;
        int nbBars = parentSection.getContainer().getSectionRange(parentSection).size();


        // Choose rhythm
        Rhythm r = sgs.getRecommendedRhythm(parentSection.getData().getTimeSignature(), startBarIndex);


        // Create the song part
        SongPart spt = sgs.createSongPart(r, parentSection.getData().getName(), startBarIndex, nbBars, parentSection, true);


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        um.startCEdit(ResUtil.getString(getClass(), "CTL_AddSpt"));


        try
        {
            // Perform change
            sgs.addSongParts(Arrays.asList(spt));         // Possible exception here     

        } catch (UnsupportedEditException ex)
        {
            String msg = ResUtil.getString(getClass(),"ERR_CantAdd", parentSection.getData());
            msg += ".\n" + ex.getLocalizedMessage();
            um.handleUnsupportedEditException(ResUtil.getString(getClass(), "CTL_AddSpt"), msg);
            return null;
        }


        um.endCEdit(ResUtil.getString(getClass(), "CTL_AddSpt"));


        return spt;
    }
}
