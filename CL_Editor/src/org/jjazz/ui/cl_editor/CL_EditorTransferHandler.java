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
package org.jjazz.ui.cl_editor;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.cl_editor.barbox.api.BarBox;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;

/**
 * Drag n Drop Transfer handler for ItemRenderers within a single ChordLeadSheet.
 * <p>
 * Drag n Drop between different songs is not supported.
 */
public class CL_EditorTransferHandler extends TransferHandler
{

    private CL_Editor editor;
    private static final Logger LOGGER = Logger.getLogger(CL_EditorTransferHandler.class.getSimpleName());

    public CL_EditorTransferHandler(CL_Editor ed)
    {
        if (ed == null)
        {
            throw new NullPointerException("ed");   
        }
        editor = ed;
    }

    /**
     * We support both copy and move actions except for the initial section which can not be moved.
     */
    @Override
    public int getSourceActions(JComponent c)
    {
        LOGGER.fine("getSourceActions()  c" + c);   
        if (c instanceof ItemRenderer)
        {
            ChordLeadSheetItem<?> cli = ((ItemRenderer) c).getModel();
            if ((cli instanceof CLI_Section) && cli.getPosition().getBar() == 0)
            {
                return TransferHandler.COPY;
            }
        }
        return TransferHandler.COPY_OR_MOVE;
    }

    /**
     * We manage only ItemRenderer drag n drop.
     *
     * @param c
     * @return
     */
    @Override
    public Transferable createTransferable(JComponent c)
    {
        LOGGER.fine("createTransferable()  c" + c);   
        if (c instanceof ItemRenderer)
        {
            ItemRenderer sourceIR = (ItemRenderer) c;
            return sourceIR.getModel();
        } else
        {
            return null;
        }
    }

    /**
     * Called from the source TransferHandler.
     *
     * @param c
     * @param data
     * @param action
     */
    @Override
    protected void exportDone(JComponent c, Transferable data, int action)
    {
        // Not used, everything is done in importData (need to be encapsulated in UndoEvents).
        LOGGER.fine("exportDone()  c=" + c + " data=" + data + " action=" + action);   
        editor.showInsertionPoint(false, getTransferredItem(data), null, true);
    }

    @Override
    public boolean canImport(TransferSupport info)
    {
        LOGGER.fine("canImport() -- info.getComponent()=" + info.getComponent());   


        // Check data flavor
        if (!info.isDataFlavorSupported(CLI_ChordSymbol.DATA_FLAVOR) && !info.isDataFlavorSupported(CLI_Section.DATA_FLAVOR))
        {
            LOGGER.fine("canImport() return false: unsupported DataFlavor");   
            return false;
        }


        // Check target location
        Position newPos = getDropPosition(info);
        if (newPos == null)
        {
            LOGGER.fine("canImport() return false: drop position not managed");   
            return false;
        }


        // Don't allow cross-chordleadsheet import
        ChordLeadSheetItem<?> sourceItem = getTransferredItem(info.getTransferable());
        assert sourceItem != null;   
        if (sourceItem.getContainer() != editor.getModel())
        {
            LOGGER.fine("canImport() return false: cross-chordleadsheet drag n drop not managed");   
            return false;
        }


        // Check if the source actions (a bitwise-OR of supported actions)
        // contains the COPY or MOVE action
        boolean copySupported = (COPY & info.getSourceDropActions()) == COPY;
        boolean moveSupported = (MOVE & info.getSourceDropActions()) == MOVE;
        if (!copySupported && !moveSupported)
        {
            LOGGER.fine("canImport() copy or move not supported");   
            return false;
        }
        if (!moveSupported)
        {
            info.setDropAction(COPY);
        } else if (!copySupported)
        {
            info.setDropAction(MOVE);
        }


        // Show the insertion point
        editor.showInsertionPoint(true, sourceItem, newPos, (info.getDropAction() & COPY) == COPY);


        return true;
    }

    @Override
    public boolean importData(TransferSupport info)
    {
        LOGGER.fine("importData() -- info.getComponent()=" + info.getComponent());   


        if (!info.isDrop())
        {
            LOGGER.fine("importData() not a drop");   
            return false;
        }


        if (!canImport(info))
        {
            LOGGER.fine("importData() can't import");   
            return false;
        }

        // Fetch the Transferable and its data
        ChordLeadSheetItem<?> sourceItem = getTransferredItem(info.getTransferable());
        assert sourceItem != null;   


        // Get the drop position
        Position newPos = getDropPosition(info);
        if (newPos == null)
        {
            LOGGER.fine("importData() drop position not managed");   
            return false;
        }


        int sourceBarIndex = sourceItem.getPosition().getBar();
        int newBarIndex = newPos.getBar();
        ChordLeadSheet cls = editor.getModel();


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);


        if (sourceItem instanceof CLI_Section)
        {
            CLI_Section section = (CLI_Section) sourceItem;


            if (sourceBarIndex == newBarIndex)
            {
                LOGGER.fine("importData() sourceBarIndex=" + sourceBarIndex + "=newBarIndex");   
                return false;
            }

            // Unselect everything: we will select the target item
            CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());
            selection.unselectAll(editor);

            CLI_Section curSection = cls.getSection(newBarIndex);
            if (info.getDropAction() == COPY)
            {
                String editName = ResUtil.getString(getClass(), "COPY SECTION");
                um.startCEdit(editName);


                CLI_Section sectionCopy = (CLI_Section) section.getCopy(null, newPos);
                String errMsg = ResUtil.getString(getClass(), "IMPOSSIBLE TO COPY SECTION", section.getData());

                if (curSection.getPosition().getBar() == newBarIndex)
                {
                    // There is already a section there, just update the content
                    try
                    {
                        cls.setSectionName(curSection, sectionCopy.getData().getName());
                        cls.setSectionTimeSignature(curSection, sectionCopy.getData().getTimeSignature());
                    } catch (UnsupportedEditException ex)
                    {
                        errMsg += "\n" + ex.getLocalizedMessage();
                        um.handleUnsupportedEditException(editName, errMsg);
                        return false;
                    }
                    editor.selectItem(curSection, true);
                    editor.setFocusOnItem(curSection, IR_Type.Section);
                } else
                {
                    try
                    {
                        // No section there, easy just copy
                        cls.addSection(sectionCopy);
                    } catch (UnsupportedEditException ex)
                    {
                        errMsg += "\n" + ex.getLocalizedMessage();
                        um.handleUnsupportedEditException(editName, errMsg);
                        return false;
                    }
                    editor.selectItem(sectionCopy, true);
                    editor.setFocusOnItem(sectionCopy, IR_Type.Section);
                }
                um.endCEdit(editName);
            } else
            {
                // Move mode
                String editName = ResUtil.getString(getClass(), "MOVE SECTION");
                String errMsg = ResUtil.getString(getClass(), "IMPOSSIBLE TO MOVE SECTION", section.getData());

                um.startCEdit(editName);

                if (curSection.getPosition().getBar() == newBarIndex)
                {
                    // There is already a section there, just update the content                          
                    try
                    {
                        cls.removeSection(section);
                        cls.setSectionName(curSection, section.getData().getName());
                        cls.setSectionTimeSignature(curSection, section.getData().getTimeSignature());
                    } catch (UnsupportedEditException ex)
                    {
                        // Section is just moved, it was OK before and it should be OK after the move.
                        errMsg += "\n" + ex.getLocalizedMessage();
                        um.handleUnsupportedEditException(editName, errMsg);
                        return false;
                    }
                    editor.selectItem(curSection, true);
                    editor.setFocusOnItem(curSection, IR_Type.Section);
                } else
                {
                    try
                    {
                        // No section there, we can move
                        cls.moveSection(section, newBarIndex);
                    } catch (UnsupportedEditException ex)
                    {
                        errMsg += "\n" + ex.getLocalizedMessage();
                        um.handleUnsupportedEditException(editName, errMsg);
                        return false;
                    }
                    editor.selectItem(section, true);
                    editor.setFocusOnItem(section, IR_Type.Section);
                }
                um.endCEdit(editName);
            }
        } else // ChordSymbols
        {
            if (info.getDropAction() == COPY)
            {
                String editName = ResUtil.getString(getClass(), "COPY ITEM");
                CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());
                selection.unselectAll(editor);
                um.startCEdit(editName);
                ChordLeadSheetItem<?> itemCopy = sourceItem.getCopy(null, newPos);
                cls.addItem(itemCopy);
                editor.setFocusOnItem(itemCopy, IR_Type.ChordSymbol);
                editor.selectItem(itemCopy, true);
                um.endCEdit(editName);
            } else
            {
                String editName = ResUtil.getString(getClass(), "MOVE ITEM");
                um.startCEdit(editName);
                cls.moveItem(sourceItem, newPos);  // The editor will take care about the selection
                um.endCEdit(editName);
            }
        }

        LOGGER.fine("importData() EXIT with success");   

        return true;
    }

    // ==================================================================================
    // Private methods
    // ==================================================================================
    private ChordLeadSheetItem<?> getTransferredItem(Transferable t)
    {
        ChordLeadSheetItem<?> sourceItem = null;
        try
        {
            sourceItem = (ChordLeadSheetItem<?>) t.getTransferData(CLI_ChordSymbol.DATA_FLAVOR);
        } catch (UnsupportedFlavorException | IOException ex)
        {
        }
        if (sourceItem == null)
        {
            try
            {
                sourceItem = (ChordLeadSheetItem<?>) t.getTransferData(CLI_Section.DATA_FLAVOR);
            } catch (UnsupportedFlavorException | IOException ex)
            {
                LOGGER.fine("getTransferredItem()  not supported data");   
            }
        }
        return sourceItem;
    }

    /**
     *
     * @param c
     * @return The enclosing barbox or null.
     */
    private BarBox getEnclosingBarBox(Component c)
    {
        while (c != null && !(c instanceof BarBox))
        {
            c = c.getParent();
        }
        return (BarBox) c;
    }

    /**
     * @param info
     * @return The position of the drag operation. Can be null if not in a barbox.
     */
    private Position getDropPosition(TransferSupport info)
    {
        // Check target location
        BarBox bb = getEnclosingBarBox(info.getComponent());
        if (bb == null)
        {
            LOGGER.fine("getTargetDragPosition() target drop component not linked to a BarBox");   
            return null;
        }
        Point p = info.getDropLocation().getDropPoint();
        if (!(info.getComponent() instanceof BarBox))
        {
            // Need to transpose coordinates
            p = SwingUtilities.convertPoint(info.getComponent(), p, bb);
        }
        Position newPos = bb.getPositionFromPoint(p);
        return newPos;
    }
}
