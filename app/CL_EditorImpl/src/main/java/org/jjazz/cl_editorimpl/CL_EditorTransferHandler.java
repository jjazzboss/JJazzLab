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
package org.jjazz.cl_editorimpl;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.cl_editor.barbox.api.BarBox;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.uiutilities.api.SingleFileDragInTransferHandler;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 * Enable ItemRenderer dragging within a chord leadsheet, and open/import song feature by dragging a file in.
 * <p>
 */
public class CL_EditorTransferHandler extends TransferHandler
{

    private final CL_EditorImpl editorImpl;
    private final Collection<String> dragInFileExtensions;
    private static final Logger LOGGER = Logger.getLogger(CL_EditorTransferHandler.class.getSimpleName());

    public CL_EditorTransferHandler(CL_EditorImpl ed)
    {
        Objects.requireNonNull(ed);
        editorImpl = ed;

        // Initialize the supported file extensions
        dragInFileExtensions = SongImporter.getAllSupportedFileExtensions();
        dragInFileExtensions.add("sng");
    }

    /**
     * We support both copy and move actions except for the initial section which can not be moved.
     */
    @Override
    public int getSourceActions(JComponent c)
    {
        LOGGER.log(Level.FINE, "getSourceActions()  c.class={0}", c.getClass().getName());
        int res = TransferHandler.NONE;

        if (c instanceof ItemRenderer ir)
        {
            ChordLeadSheetItem<?> cli = ir.getModel();
            res = (cli instanceof CLI_Section) && cli.getPosition().getBar() == 0 ? TransferHandler.COPY : TransferHandler.COPY_OR_MOVE;
        }
        LOGGER.log(Level.FINE, "getSourceActions()  res={0}", res);
        return res;
    }

    /**
     * Overridden to limit drag start to ItemRenderers with no modifier used and mouse left button.
     *
     * @param comp
     * @param e
     * @param action
     */
    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action)
    {
        if (!(comp instanceof ItemRenderer)
            || (e instanceof MouseEvent me && (!SwingUtilities.isLeftMouseButton(me) || me.isAltDown() || me.isShiftDown()))) // ctrl is authorized for item copy
        {
            return;
        }
        super.exportAsDrag(comp, e, action);
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
        LOGGER.log(Level.FINE, "createTransferable()  c{0}", c);
        if (c instanceof ItemRenderer ir)
        {
            return ir.getModel();
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
        LOGGER.log(Level.FINE, "exportDone()  c={0} data={1} action={2}", new Object[]
        {
            c, data, action
        });
        editorImpl.showInsertionPoint(false, getTransferredItem(data), null, true);
    }

    /**
     * Accept ItemRenderers or a file with specific extension.
     *
     * @param info
     * @return
     */
    @Override
    public boolean canImport(TransferSupport info)
    {
        LOGGER.log(Level.FINE, "canImport() -- info.getComponent()={0}", info.getComponent());

        boolean b = false;


        if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        {
            b = canImportFile(info);

        } else if (info.isDataFlavorSupported(CLI_ChordSymbol.DATA_FLAVOR)
            || info.isDataFlavorSupported(CLI_Section.DATA_FLAVOR)
            || info.isDataFlavorSupported(CLI_BarAnnotation.DATA_FLAVOR))
        {
            b = canImportItemRenderer(info);
        }


        return b;
    }

    @Override
    public boolean importData(TransferSupport info)
    {
        LOGGER.log(Level.FINE, "importData() -- info.getComponent()={0}", info.getComponent());


        if (!info.isDrop())
        {
            LOGGER.fine("importData() not a drop");
            return false;
        }

        boolean b = false;
        if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        {
            b = importDataFile(info);
        } else
        {
            b = importDataItem(info);
        }
        return b;
    }

    // ==================================================================================
    // Private methods
    // ==================================================================================
    private boolean importDataFile(TransferSupport info)
    {
        File file = SingleFileDragInTransferHandler.getAcceptedFile(info, dragInFileExtensions);

        if (file == null)
        {
            // Can be null on MacOS since canImport() will always return true for javaFileListFlavor            
            if (!org.jjazz.utilities.api.Utilities.isMac())
            {
                LOGGER.warning("importDataFile() Unexpected null value for file");
            }
            return false;
        }


        try
        {
            SongEditorManager.getDefault().showSong(file, true, false);

        } catch (SongCreationException ex)
        {
            LOGGER.log(Level.WARNING, "importDataFile() Error loading dragged-in file {0}: {1}", new Object[]
            {
                file.getAbsolutePath(), ex.getMessage()
            });
            String msg = ResUtil.getCommonString("ErrorLoadingSongFile", file.getAbsolutePath(), ex.getLocalizedMessage());
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return false;
        }

        return true;
    }

    private boolean importDataItem(TransferSupport info)
    {
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
        ChordLeadSheet cls = editorImpl.getModel();


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);


        switch (sourceItem)
        {
            case CLI_Section cliSection ->
            {
                if (sourceBarIndex == newBarIndex)
                {
                    LOGGER.log(Level.FINE, "importData() sourceBarIndex={0}=newBarIndex", sourceBarIndex);
                    return false;
                }

                // Unselect everything: we will select the target item
                editorImpl.clearSelection();

                CLI_Section curSection = cls.getSection(newBarIndex);
                if (info.getDropAction() == COPY)
                {
                    String editName = ResUtil.getString(getClass(), "COPY_SECTION");
                    um.startCEdit(editName);

                    CLI_Section sectionCopy = cliSection.getCopy(newPos, cls);  // Adjust section name if required
                    CL_EditorClientProperties.setSectionIsOnNewLine(sectionCopy, false);        // Disable section on new line

                    String errMsg = ResUtil.getString(getClass(), "IMPOSSIBLE_TO_COPY_SECTION", cliSection.getData());
                    try
                    {
                        sectionCopy = cls.addSection(sectionCopy);
                    } catch (UnsupportedEditException ex)
                    {
                        errMsg += "\n" + ex.getLocalizedMessage();
                        um.abortCEdit(editName, errMsg);
                        return false;
                    }
                    editorImpl.selectItem(sectionCopy, true);
                    editorImpl.setFocusOnItem(sectionCopy, IR_Type.Section);
                    um.endCEdit(editName);
                } else
                {
                    // Move mode
                    String editName = ResUtil.getString(getClass(), "MOVE_SECTION");
                    String errMsg = ResUtil.getString(getClass(), "IMPOSSIBLE_TO_MOVE_SECTION", cliSection.getData());

                    um.startCEdit(editName);

                    if (curSection.getPosition().getBar() == newBarIndex)
                    {
                        // There is already a section there, just update the content
                        try
                        {
                            cls.removeSection(cliSection);
                            cls.setSectionName(curSection, cliSection.getData().getName());
                            cls.setSectionTimeSignature(curSection, cliSection.getData().getTimeSignature());
                        } catch (UnsupportedEditException ex)
                        {
                            // Section is just moved, it was OK before and it should be OK after the move.
                            errMsg += "\n" + ex.getLocalizedMessage();
                            um.abortCEdit(editName, errMsg);
                            return false;
                        }
                        editorImpl.selectItem(curSection, true);
                        editorImpl.setFocusOnItem(curSection, IR_Type.Section);
                    } else
                    {
                        try
                        {
                            // No section there, we can move
                            cls.moveSection(cliSection, newBarIndex);
                        } catch (UnsupportedEditException ex)
                        {
                            errMsg += "\n" + ex.getLocalizedMessage();
                            um.abortCEdit(editName, errMsg);
                            return false;
                        }
                        editorImpl.selectItem(cliSection, true);
                        editorImpl.setFocusOnItem(cliSection, IR_Type.Section);
                    }
                    um.endCEdit(editName);
                }
            }
            case CLI_BarAnnotation cliBa ->
            {
                if (sourceBarIndex == newBarIndex)
                {
                    LOGGER.log(Level.FINE, "importData() sourceBarIndex==newBarIndex=={0}", sourceBarIndex);
                    return false;
                }

                // Unselect everything: we will select the target item
                editorImpl.clearSelection();


                String editName = ResUtil.getString(getClass(), "MoveBarAnnotation");
                um.startCEdit(editName);


                CLI_BarAnnotation curAnnotation = cls.getBarFirstItem(newBarIndex, CLI_BarAnnotation.class, cli -> true);
                IR_Type irType = CL_EditorClientProperties.isBarAnnotationVisible(editorImpl.getSongModel()) ? IR_Type.BarAnnotationText
                    : IR_Type.BarAnnotationPaperNote;
                if (curAnnotation != null)
                {
                    // There is already an annotation there, just update its content
                    cls.changeItem(curAnnotation, cliBa.getData());
                    editorImpl.setFocusOnItem(curAnnotation, irType);
                    editorImpl.selectItem(curAnnotation, true);
                } else
                {
                    // Add a new annotation
                    CLI_BarAnnotation cliCopy = (CLI_BarAnnotation) cliBa.getCopy(null, newPos);
                    cls.addItem(cliCopy);
                    editorImpl.setFocusOnItem(cliCopy, irType);
                    editorImpl.selectItem(cliCopy, true);
                }

                if (info.getDropAction() == MOVE)
                {
                    // Move annotation
                    cls.removeItem(sourceItem);
                }

                um.endCEdit(editName);


            }
            default ->
            {
                // ChordSymbols
                if (info.getDropAction() == COPY)
                {
                    String editName = ResUtil.getString(getClass(), "COPY_ITEM");
                    editorImpl.clearSelection();
                    um.startCEdit(editName);
                    ChordLeadSheetItem<?> itemCopy = sourceItem.getCopy(null, newPos);
                    var itemToSelect = cls.addItem(itemCopy) ? itemCopy : sourceItem;        // addItem might return false if copying over an equal item
                    editorImpl.setFocusOnItem(itemToSelect, IR_Type.ChordSymbol);
                    editorImpl.selectItem(itemToSelect, true);
                    um.endCEdit(editName);
                } else
                {
                    String editName = ResUtil.getString(getClass(), "MOVE_ITEM");
                    um.startCEdit(editName);
                    cls.moveItem(sourceItem, newPos);  // might return false when moving over an equal item. In other cases the editor will take care about the selection
                    um.endCEdit(editName);
                }
            }


        }

        LOGGER.fine("importData() EXIT with success");


        return true;
    }

    private boolean canImportFile(TransferSupport info)
    {
        boolean b;

        // Copy mode must be supported
        if ((COPY & info.getSourceDropActions()) != COPY)
        {
            return false;
        }


        File file = SingleFileDragInTransferHandler.getAcceptedFile(info, dragInFileExtensions);
        // Special handling for MacOS: on Mac getAcceptedFile() will always return false, even if DataFlavor.javaFileListFlavor is supported.
        // On MacOS The TransferHandler.TransferSupport parameter is initialized only when importData() is called.
        if (file == null && org.jjazz.utilities.api.Utilities.isMac())
        {
            LOGGER.fine("canImportFile() MacOs - ignoring null return value of getAcceptedFile(info)");
            b = true;
        } else
        {
            b = file != null;
        }


        if (!b)
        {
            LOGGER.log(Level.FINE, "canImportFile() returns false: unsupported DataFlavor transferable={0}", info.getTransferable());
        } else
        {
            // Always use copy drop icon
            info.setDropAction(COPY);
        }

        return b;
    }

    private boolean canImportItemRenderer(TransferSupport info)
    {
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
        if (sourceItem.getContainer() != editorImpl.getModel())
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


        LOGGER.log(Level.FINE, "canImport() getDropAction()={0}", info.getDropAction());

        // Show the insertion point
        editorImpl.showInsertionPoint(true, sourceItem, newPos, (info.getDropAction() & COPY) == COPY);


        return true;
    }

    private ChordLeadSheetItem<?> getTransferredItem(Transferable t)
    {
        ChordLeadSheetItem<?> res = null;
        try
        {
            if (t.isDataFlavorSupported(CLI_ChordSymbol.DATA_FLAVOR))
            {
                res = (ChordLeadSheetItem<?>) t.getTransferData(CLI_ChordSymbol.DATA_FLAVOR);
            } else if (t.isDataFlavorSupported(CLI_Section.DATA_FLAVOR))
            {
                res = (ChordLeadSheetItem<?>) t.getTransferData(CLI_Section.DATA_FLAVOR);
            } else if (t.isDataFlavorSupported(CLI_BarAnnotation.DATA_FLAVOR))
            {
                res = (ChordLeadSheetItem<?>) t.getTransferData(CLI_BarAnnotation.DATA_FLAVOR);
            }
        } catch (UnsupportedFlavorException | IOException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }

        return res;
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
     * Use Section's quantization.
     *
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
        Position pos = bb.getPositionFromPoint(p);
        if (pos == null)
        {
            return pos;
        }


        // Quantize
        int barIndex = bb.getModelBarIndex();
        if (barIndex == -1)
        {
            return null;
        }
        var cls = editorImpl.getModel();
        var cliSection = cls.getSection(barIndex);
        var q = CL_EditorClientProperties.getSectionUserQuantization(cliSection);
        if (q == null)
        {
            // No user defined quantization, rely on division of first corresponding rhythm's division            
            var d = BR_ChordPositions.getDivision(editorImpl.getSongModel(), cliSection);
            q = d.isTernary() ? Quantization.ONE_THIRD_BEAT : Quantization.ONE_QUARTER_BEAT;
        }

        pos = Quantizer.getQuantized(q, pos, cliSection.getData().getTimeSignature(), 1f, cls.getSizeInBars() - 1);


        return pos;
    }

}
