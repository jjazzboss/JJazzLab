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

import org.jjazz.ui.cl_editor.actions.ExtendSelectionRight;
import org.jjazz.ui.cl_editor.actions.JumpToHome;
import org.jjazz.ui.cl_editor.actions.JumpToEnd;
import org.jjazz.ui.cl_editor.actions.MoveSelectionUp;
import org.jjazz.ui.cl_editor.actions.ExtendSelectionLeft;
import org.jjazz.ui.cl_editor.actions.MoveSelectionRight;
import org.jjazz.ui.cl_editor.actions.MoveSelectionLeft;
import org.jjazz.ui.cl_editor.actions.MoveSelectionDown;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.ui.cl_editor.barbox.api.BarBox;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_EditorMouseListener;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;
import org.jjazz.uisettings.GeneralUISettings;
import static org.jjazz.ui.utilities.Utilities.getGenericControlKeyStroke;
import org.jjazz.ui.utilities.Zoomable;
import org.openide.awt.Actions;
import org.openide.util.Utilities;

/**
 * Controller for the CL_Editor.
 */
public class CL_EditorController implements CL_EditorMouseListener
{

    public static final String PROP_ZOOM_FACTOR_X = "PropClEditorZoomFactorX";
    /**
     * Actions reused several times
     */
    private Action editAction;
    private Action transposeUpAction, transposeDownAction;
    /**
     * Popupmenus depending of selection.
     */
    private JPopupMenu popupChordSymbolMenu;
    private JPopupMenu popupSectionMenu;
    private JPopupMenu popupBarMenu;

    /**
     * The graphical editor we control.
     */
    private CL_EditorImpl editor;
    /**
     * The barIndex on which a bar drag has been started.
     */
    private int dragStartBbIndex;
    private static final Logger LOGGER = Logger.getLogger(CL_EditorController.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public CL_EditorController(CL_Editor ed)
    {
        editor = (CL_EditorImpl) ed;
        dragStartBbIndex = -1;

        // Initialize actions. 
        transposeDownAction = Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.transposedown");
        transposeUpAction = Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.transposeup");
        editAction = Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.edit");

        // Actions created by annotations (equivalent to org.openide.awt.Actions.context())
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_DOWN), "TransposeDown");
        editor.getActionMap().put("TransposeDown", transposeDownAction);
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_UP), "TransposeUp");
        editor.getActionMap().put("TransposeUp", transposeUpAction);
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift DELETE"), "RemoveBar");
        editor.getActionMap().put("RemoveBar", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.removebar"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_LEFT), "MoveItemLeft");
        editor.getActionMap().put("MoveItemLeft", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.moveitemleft"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_RIGHT), "MoveItemRight");
        editor.getActionMap().put("MoveItemRight", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.moveitemright"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_E), "SetEndBar");
        editor.getActionMap().put("SetEndBar", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.setendbar"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("I"), "InsertBar");
        editor.getActionMap().put("InsertBar", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.InsertBar"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("P"), "NextPlayStyle");
        editor.getActionMap().put("NextPlayStyle", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.nextplaystyle"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("S"), "AccentStronger");
        editor.getActionMap().put("AccentStronger", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.accentstronger"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("H"), "AccentCrash");
        editor.getActionMap().put("AccentCrash", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.accentcrash"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("X"), "ExtendHoldShot");
        editor.getActionMap().put("ExtendHoldShot", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.extendholdshot"));


        // Our delegates for standard Netbeans callback actions
        editor.getActionMap().put("cut-to-clipboard", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.cut"));
        editor.getActionMap().put("copy-to-clipboard", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.copy"));
        editor.getActionMap().put("paste-from-clipboard", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.paste"));

        // Delegates for our callback actions        
        editor.getActionMap().put("jjazz-delete", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.deleteitem"));
        editor.getActionMap().put("jjazz-selectall", Actions.forID("JJazz", "org.jjazz.ui.cl_editor.actions.selectall"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("A"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift A"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("B"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift B"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("C"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift C"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("D"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift D"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("E"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift E"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift F"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("G"), "jjazz-edit");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift G"), "jjazz-edit");
        editor.getActionMap().put("jjazz-edit", editAction);

        // Other actions
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("LEFT"), "MoveSelectionLeft");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift TAB"), "MoveSelectionLeft");
        editor.getActionMap().put("MoveSelectionLeft", new MoveSelectionLeft());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift LEFT"), "ExtendSelectionLeft");
        editor.getActionMap().put("ExtendSelectionLeft", new ExtendSelectionLeft());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("RIGHT"), "MoveSelectionRight");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("TAB"), "MoveSelectionRight");
        editor.getActionMap().put("MoveSelectionRight", new MoveSelectionRight());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift RIGHT"), "ExtendSelectionRight");
        editor.getActionMap().put("ExtendSelectionRight", new ExtendSelectionRight());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("UP"), "MoveSelectionUp");
        editor.getActionMap().put("MoveSelectionUp", new MoveSelectionUp());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("DOWN"), "MoveSelectionDown");
        editor.getActionMap().put("MoveSelectionDown", new MoveSelectionDown());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("HOME"), "JumpToHome");
        editor.getActionMap().put("JumpToHome", new JumpToHome());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("END"), "JumpToEnd");
        editor.getActionMap().put("JumpToEnd", new JumpToEnd());


        // Try to restore zoom factor X
        String str = editor.getSongModel().getClientProperty(PROP_ZOOM_FACTOR_X, null);
        if (str != null)
        {
            int zfx = -1;
            try
            {
                zfx = Integer.valueOf(str);
            } catch (NumberFormatException e)
            {
                // Nothing
            }
            if (zfx < 0 || zfx > 100)
            {
                LOGGER.warning("CL_EditorController() Invalid zoom factor X client property=" + str + " in song=" + editor.getSongModel().getName());
            } else
            {
                Zoomable zoomable = editor.getLookup().lookup(Zoomable.class);
                if (zoomable != null)
                {
                    zoomable.setZoomXFactor(zfx);
                }
            }
        }
    }

    public CL_Editor getEditor()
    {
        return editor;
    }

    // ----------------------------------------------------------------------------------
    // CL_EditorMouseListener interface
    // ----------------------------------------------------------------------------------
    @SuppressWarnings(
            {
                "rawtypes"
            })
    @Override
    public void itemClicked(MouseEvent e, ChordLeadSheetItem<?> item, IR_Type irType)
    {
        ChordLeadSheetItem<?> focusedItem = null;
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof ItemRenderer)
        {
            focusedItem = ((ItemRenderer) c).getModel();
        }

        CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());

        LOGGER.log(Level.FINE, "itemClicked() item={0} selection={1}", new Object[]
        {
            item, selection
        });

        if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e))
        {
            if (selection.isBarSelected() || selection.isEmpty() || focusedItem == null || item.getClass() != focusedItem.getClass()
                    || (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == 0)
            {
                // SIMPLE CLICK, or no previous selection set on a similar item
                selection.unselectAll(editor);
                editor.selectItem(item, true);
                editor.setFocusOnItem(item, irType);
            } else if ((e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))
                    == InputEvent.CTRL_DOWN_MASK)
            {
                // CTRL CLICK
                // Just add selection, don't change focus
                if (item != focusedItem)
                {
                    editor.selectItem(item, !selection.isItemSelected(item));
                }
            } else if (focusedItem != null && (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK
                    | InputEvent.CTRL_DOWN_MASK))
                    == InputEvent.SHIFT_DOWN_MASK)
            {
                // SHIFT CLICK
                // Select items between the focused one and this item
                Position maxPosition = item.getPosition();
                Position minPosition = focusedItem.getPosition();
                if (minPosition.compareTo(maxPosition) > 0)
                {
                    maxPosition = focusedItem.getPosition();
                    minPosition = item.getPosition();
                }
                selection.unselectAll(editor);
                List<? extends ChordLeadSheetItem> items = editor.getModel().getItems(minPosition.getBar(), maxPosition.getBar(),
                        focusedItem.getClass());
                for (ChordLeadSheetItem<?> it : items)
                {
                    Position pos = it.getPosition();
                    if (pos.compareTo(minPosition) >= 0 && pos.compareTo(maxPosition) <= 0)
                    {
                        editor.selectItem(it, true);
                    }
                }
            }
        } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == 0)
        {
            // DOUBLE CLICK = edit item

            // First do like simple click
            selection.unselectAll(editor);
            editor.selectItem(item, true);
            editor.setFocusOnItem(item, irType);

            // Edit with registered action
            editAction.actionPerformed(null);
        } else if (e.getClickCount() == 1 && SwingUtilities.isRightMouseButton(e))
        {
            // Right click                   
            if (!selection.isItemSelected(item))
            {
                // First do like simple click
                selection.unselectAll(editor);
                editor.selectItem(item, true);
                editor.setFocusOnItem(item, irType);
            }
            if (item instanceof CLI_ChordSymbol)
            {
                if (popupChordSymbolMenu == null)
                {
                    List<? extends Action> actions = Utilities.actionsForPath("Actions/ChordSymbol");
                    popupChordSymbolMenu = Utilities.actionsToPopup(actions.toArray(new Action[actions.size()]), editor);
                }
                popupChordSymbolMenu.show(e.getComponent(), e.getX(), e.getY());
            } else if (item instanceof CLI_Section)
            {
                if (popupSectionMenu == null)
                {
                    List<? extends Action> actions = Utilities.actionsForPath("Actions/Section");
                    popupSectionMenu = Utilities.actionsToPopup(actions.toArray(new Action[actions.size()]), editor);
                }
                popupSectionMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    @Override
    public void itemWheelMoved(MouseWheelEvent e, ChordLeadSheetItem<?> item, IR_Type irType)
    {
        CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());
        List<ChordLeadSheetItem<?>> items = selection.getSelectedItems();
        if (items.isEmpty() || !(items.get(0) instanceof CLI_ChordSymbol))
        {
            return;
        }

        if (e.isControlDown())
        {
            // Not managed
            return;
        }

        // Make sure our TopComponent is active so that global lookup represents our editor's selection. 
        // Because wheel action can be enabled even if the TopComponent is inactive, if editor's selection was indirectly 
        // changed while editor was not active (e.g. via an editor from another TopComponent), 
        // the CL_ContextActionSupport which listens to selection via the global lookup will have missed the selection change, causing 
        // problems in actions.
        ChordLeadSheet cls = editor.getModel();
        CL_EditorTopComponent clTc = CL_EditorTopComponent.get(cls);
        clTc.requestActive();

        boolean isWheelChangeEnabled = GeneralUISettings.getInstance().isChangeValueWithMouseWheelEnabled();
        LOGGER.log(Level.FINER, "itemWheelMoved() e.getWheelRotation()={0} isWheelChangeEnabled={1}", new Object[]
        {
            e.getWheelRotation(), isWheelChangeEnabled
        });


        if (isWheelChangeEnabled)
        {
            if (e.getWheelRotation() > 0)
            {
                transposeDownAction.actionPerformed(null);

            } else
            {
                transposeUpAction.actionPerformed(null);
            }
        }
    }

    @Override
    public void barClicked(MouseEvent e, int barIndex)
    {
        int focusedBarIndex = -1;
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof BarBox)
        {
            focusedBarIndex = ((BarBox) c).getBarIndex();
        }

        LOGGER.log(Level.FINE, "barClicked() barIndex{0}", barIndex);

        CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());

        if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e))
        {
            if (selection.isItemSelected() || selection.isEmpty()
                    || (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == 0)
            {
                // SIMPLE CLICK, or no previous selection set
                selection.unselectAll(editor);
                editor.selectBars(barIndex, barIndex, true);
                editor.setFocusOnBar(barIndex);
            } else if ((e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))
                    == InputEvent.CTRL_DOWN_MASK)
            {
                // CTRL CLICK
                // Just add selection, don't change focus
                if (barIndex != focusedBarIndex)
                {
                    editor.selectBars(barIndex, barIndex, !selection.isBarSelected(barIndex));
                }
            } else if (focusedBarIndex != -1 && (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK
                    | InputEvent.CTRL_DOWN_MASK))
                    == InputEvent.SHIFT_DOWN_MASK)
            {
                // SHIFT CLICK
                // Select bars between the focused bar and this bar  
                int minBar = Math.min(focusedBarIndex, barIndex);
                int maxBar = Math.max(focusedBarIndex, barIndex);
                editor.selectBarsExcept(minBar, maxBar, false);
                editor.selectBars(minBar, maxBar, true);
            }
        } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == 0)
        {
            // DOUBLE CLICK = edit bar

            // First do like simple click
            selection.unselectAll(editor);
            editor.selectBars(barIndex, barIndex, true);
            editor.setFocusOnBar(barIndex);

            // Edit bar using registered action
            if (barIndex < editor.getModel().getSize())
            {
                editAction.actionPerformed(null);
            }
        } else if (e.getClickCount() == 1 && SwingUtilities.isRightMouseButton(e))
        {
            // Right click                   
            if (!selection.isBarSelected(barIndex))
            {
                // First do like simple click
                selection.unselectAll(editor);
                editor.selectBars(barIndex, barIndex, true);
                editor.setFocusOnBar(barIndex);
            }
            if (popupBarMenu == null)
            {
                List<? extends Action> actions = Utilities.actionsForPath("Actions/Bar");
                popupBarMenu = Utilities.actionsToPopup(actions.toArray(new Action[actions.size()]), editor);
            }
            popupBarMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void barDragged(MouseEvent e, int bbIndex)
    {
        if ((e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) != 0)
        {
            // Ctrl or Shift not allowed
            return;
        }
        CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());
        if (dragStartBbIndex == -1)
        {
            // Start drag operation by selecting the current barbox
            dragStartBbIndex = bbIndex;
            selection.unselectAll(editor);
            editor.selectBars(dragStartBbIndex, dragStartBbIndex, true);
            editor.setFocusOnBar(bbIndex);
            LOGGER.log(Level.FINE, "barDragged() start drag bbIndex={0}", bbIndex);
        } else
        {
            // We continue a drag operation previously started
            Point editorPoint = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), editor);
            Position pos = editor.getPositionFromPoint(editorPoint);
            if (pos == null)
            {
                return;
            }
            int minBbIndex = Math.min(dragStartBbIndex, pos.getBar());
            int maxBbIndex = Math.max(dragStartBbIndex, pos.getBar());
            editor.selectBarsExcept(minBbIndex, maxBbIndex, false);
            editor.selectBars(minBbIndex, maxBbIndex, true);
        }
    }

    @Override
    public void barReleased(MouseEvent e, int barIndex)
    {
        LOGGER.log(Level.FINE, "barReleased()");
        dragStartBbIndex = -1;
    }

    @Override
    public void editorWheelMoved(MouseWheelEvent e)
    {
        final int STEP = 5;
        if ((e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) != InputEvent.CTRL_DOWN_MASK)
        {
            // We manage only ctrl-wheel
            // Don't want to lose the event, need to be processed by the above hierarchy, i.e. enclosing JScrollPane
            Container source = (Container) e.getSource();
            Container parent = source.getParent();
            MouseEvent parentEvent = SwingUtilities.convertMouseEvent(source, e, parent);
            parent.dispatchEvent(parentEvent);
            return;
        }

        Zoomable zoomable = editor.getLookup().lookup(Zoomable.class);
        if (zoomable == null)
        {
            return;
        }

        // Because wheel action can be enabled even if the TopComponent is inactive, make sure to make our TopComponent active 
        // to avoid possible problems with the global selection 
        ChordLeadSheet cls = editor.getModel();
        CL_EditorTopComponent clTc = CL_EditorTopComponent.get(cls);
        clTc.requestActive();

        int factor = zoomable.getZoomXFactor();
        if (e.getWheelRotation() < 0)
        {
            factor = Math.min(100, factor + STEP);
        } else
        {
            factor = Math.max(0, factor - STEP);
        }
        LOGGER.log(Level.FINE, "editorWheelMoved() factor=" + factor);
        zoomable.setZoomXFactor(factor);

        // Save the zoom factor with the song as a client property
        editor.getSongModel().putClientProperty(PROP_ZOOM_FACTOR_X, Integer.toString(factor));

    }

    @Override
    public String toString()
    {
        return "CL_EditorController[" + editor.getSongModel().getName() + "]";
    }

    // ------------------------------------------------------------------------------
    // Private functions
    // ------------------------------------------------------------------------------   
}
