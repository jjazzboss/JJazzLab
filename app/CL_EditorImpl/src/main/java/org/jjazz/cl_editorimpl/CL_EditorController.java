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
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editorimpl.actions.AccentOptionsCrash;
import org.jjazz.cl_editorimpl.actions.AccentOptionsExtendHoldShot;
import org.jjazz.cl_editorimpl.actions.AccentOptionsStronger;
import org.jjazz.cl_editorimpl.actions.ToggleBarAnnotations;
import org.jjazz.cl_editorimpl.actions.ExtendSelectionRight;
import org.jjazz.cl_editorimpl.actions.JumpToHome;
import org.jjazz.cl_editorimpl.actions.JumpToEnd;
import org.jjazz.cl_editorimpl.actions.MoveSelectionUp;
import org.jjazz.cl_editorimpl.actions.ExtendSelectionLeft;
import org.jjazz.cl_editorimpl.actions.MoveSelectionRight;
import org.jjazz.cl_editorimpl.actions.MoveSelectionLeft;
import org.jjazz.cl_editorimpl.actions.MoveSelectionDown;
import org.jjazz.cl_editor.barbox.api.BarBox;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorMouseListener;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.cl_editorimpl.actions.MoveItemLeft;
import org.jjazz.cl_editorimpl.actions.MoveItemRight;
import org.jjazz.flatcomponents.api.FlatComponentsGlobalSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.uiutilities.api.Zoomable;
import org.openide.awt.Actions;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

/**
 * Controller for the CL_Editor.
 */
public class CL_EditorController implements CL_EditorMouseListener
{

    /**
     * Actions which can be also triggered with the mouse
     */
    private final Action editAction;
    private final Action transposeUpAction;
    private final Action transposeDownAction;
    private final Action chordAuditioningAction;

    /**
     * The graphical editor we control.
     */
    private final CL_EditorImpl editor;
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

        assert SwingUtilities.isEventDispatchThread();

        // Initialize actions used in methods
        transposeDownAction = Actions.forID("JJazz", "org.jjazz.cl_editor.actions.transposedown");
        transposeUpAction = Actions.forID("JJazz", "org.jjazz.cl_editor.actions.transposeup");
        editAction = Actions.forID("JJazz", "org.jjazz.cl_editor.actions.edit");
        chordAuditioningAction = Actions.forID("JJazz", "org.jjazz.cl_editor.actions.chordauditioning");


        // Actions created by annotations without a global shortcut 
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(MoveItemLeft.KEYSTROKE, "MoveItemLeft");
        editor.getActionMap().put("MoveItemLeft", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.moveitemleft"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(MoveItemRight.KEYSTROKE, "MoveItemRight");
        editor.getActionMap().put("MoveItemRight", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.moveitemright"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(AccentOptionsStronger.KEYSTROKE, "AccentStronger");
        editor.getActionMap().put("AccentStronger", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.accentstronger"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(AccentOptionsCrash.KEYSTROKE, "AccentChangeCrash");
        editor.getActionMap().put("AccentChangeCrash", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.accentcrash"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(AccentOptionsExtendHoldShot.KEYSTROKE, "ExtendHoldShot");
        editor.getActionMap().put("ExtendHoldShot", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.extendholdshot"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ToggleBarAnnotations.KEYSTROKE, "ToggleBarAnnotations");
        editor.getActionMap().put("ToggleBarAnnotations", ToggleBarAnnotations.getInstance(editor));


        // Our delegates for standard Netbeans callback actions
        // Note: since NB 17 (?), these actions need also to be in the TopComponent ActionMap!
        editor.getActionMap().put("cut-to-clipboard", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.cut"));
        editor.getActionMap().put("copy-to-clipboard", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.copy"));
        editor.getActionMap().put("paste-from-clipboard", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.paste"));


        // Delegates for our callback actions        
        editor.getActionMap().put("jjazz-delete", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.deleteitem"));
        editor.getActionMap().put("jjazz-selectall", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.selectall"));
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
        if (c instanceof ItemRenderer ir)
        {
            focusedItem = ir.getModel();
        }

        CL_Selection selection = new CL_Selection(editor.getLookup());

        LOGGER.log(Level.FINE, "itemClicked() item={0} selection={1}", new Object[]
        {
            item, selection
        });

        if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e))
        {
            if (focusedItem != null && item.getClass() == focusedItem.getClass())
            {
                // Updating an existing item selection                
                if (!e.isShiftDown() && !e.isControlDown())
                {
                    // Simple CLICK
                    editor.clearSelection(selection);
                    editor.selectItem(item, true);
                    editor.setFocusOnItem(item, irType);

                } else if (e.isShiftDown() && e.isControlDown() && chordAuditioningAction.isEnabled())
                {
                    // SHIFT+CTRL CLICK
                    editor.clearSelection(selection);
                    editor.selectItem(item, true);
                    editor.setFocusOnItem(item, irType);
                    chordAuditioningAction.actionPerformed(null);

                } else if (!e.isShiftDown() && e.isControlDown())
                {
                    // CTRL CLICK
                    // Just add selection, don't change focus
                    if (item != focusedItem)
                    {
                        editor.selectItem(item, !selection.isItemSelected(item));
                    }

                } else if (e.isShiftDown() && !e.isControlDown())
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
                    editor.clearSelection(selection);
                    var items = editor.getModel().getItems(minPosition.getBar(), maxPosition.getBar(), focusedItem.getClass());
                    for (var iitem : items)
                    {
                        Position pos = iitem.getPosition();
                        if (pos.compareTo(minPosition) >= 0 && pos.compareTo(maxPosition) <= 0)
                        {
                            editor.selectItem(iitem, true);
                        }
                    }

                }
            } else
            {
                // No selection, or selection is from a different type
                editor.clearSelection(selection);
                editor.selectItem(item, true);
                editor.setFocusOnItem(item, irType);

                if (e.isShiftDown() && e.isControlDown() && chordAuditioningAction.isEnabled())
                {
                    chordAuditioningAction.actionPerformed(null);
                }
            }
        } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown() && !e.isControlDown())
        {
            // DOUBLE CLICK with no ctrl nor shift = edit item

            // First do like simple click
            editor.clearSelection(selection);
            editor.selectItem(item, true);
            editor.setFocusOnItem(item, irType);

            // Edit with registered action
            ActionEvent ae = new ActionEvent(e.getSource(), ActionEvent.ACTION_FIRST, "item");
            if (editAction.isEnabled())
            {
                editAction.actionPerformed(ae);
            }

        } else if (e.getClickCount() == 1 && SwingUtilities.isRightMouseButton(e))
        {
            // Right click                   
            if (!selection.isItemSelected(item))
            {
                // First do like simple click
                editor.clearSelection(selection);
                editor.selectItem(item, true);
                editor.setFocusOnItem(item, irType);
            }

            String actionsPath = switch (item)
            {
                case CLI_ChordSymbol it ->
                    "Actions/ChordSymbol";
                case CLI_Section it ->
                    "Actions/Section";
                case CLI_BarAnnotation it ->
                    "Actions/BarAnnotation";
                default ->
                    throw new IllegalStateException("item=" + item);
            };
            buildAndShowPopupMenu(e, actionsPath, editor.getLookup());
        }
    }

    @Override
    public void itemWheelMoved(MouseWheelEvent e, ChordLeadSheetItem<?> item, IR_Type irType)
    {
        CL_Selection selection = new CL_Selection(editor.getLookup());
        var items = selection.getSelectedItems();
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

        boolean isWheelChangeEnabled = FlatComponentsGlobalSettings.getInstance().isChangeValueWithMouseWheelEnabled();
        LOGGER.log(Level.FINER, "itemWheelMoved() e.getWheelRotation()={0} isWheelChangeEnabled={1}", new Object[]
        {
            e.getWheelRotation(), isWheelChangeEnabled
        });


        // Fix Issue #347: need to give time for clTc to become active if it was not the case
        SwingUtilities.invokeLater(() ->
        {
            if (isWheelChangeEnabled)
            {
                if (e.getWheelRotation() > 0)
                {
                    if (transposeDownAction.isEnabled())
                    {
                        transposeDownAction.actionPerformed(null);
                    }

                } else
                {
                    if (transposeUpAction.isEnabled())
                    {
                        transposeUpAction.actionPerformed(null);
                    }
                }
            }
        });
    }

    @Override
    public void barClicked(MouseEvent e, int barIndex)
    {
        int focusedBarIndex = -1;
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof BarBox bb)
        {
            focusedBarIndex = bb.getBarIndex();
        }

        LOGGER.log(Level.FINE, "barClicked() barIndex{0}", barIndex);

        CL_Selection selection = new CL_Selection(editor.getLookup());

        if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e))
        {
            if (selection.isItemSelected() || selection.isEmpty() || (!e.isControlDown() && !e.isShiftDown()))
            {
                // SIMPLE CLICK, or no previous selection set
                editor.clearSelection(selection);
                editor.selectBars(barIndex, barIndex, true);
                editor.setFocusOnBar(barIndex);
            } else if (e.isControlDown() && !e.isShiftDown())
            {
                // CTRL CLICK
                // Just add selection, don't change focus
                if (barIndex != focusedBarIndex)
                {
                    editor.selectBars(barIndex, barIndex, !selection.isBarSelected(barIndex));
                }
            } else if (focusedBarIndex != -1 && (!e.isControlDown() && e.isShiftDown()))
            {
                // SHIFT CLICK
                // Select bars between the focused bar and this bar  
                int minBar = Math.min(focusedBarIndex, barIndex);
                int maxBar = Math.max(focusedBarIndex, barIndex);
                editor.clearSelection(selection);
                editor.selectBars(minBar, maxBar, true);
            }
        } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && (!e.isControlDown() && !e.isShiftDown()))
        {
            // DOUBLE CLICK = edit bar

            // First do like simple click
            editor.clearSelection(selection);
            editor.selectBars(barIndex, barIndex, true);
            editor.setFocusOnBar(barIndex);

            // Edit bar using registered action
            if (barIndex < editor.getModel().getSizeInBars())
            {
                ActionEvent ae = new ActionEvent(e.getSource(), ActionEvent.ACTION_FIRST, "bar");
                editAction.actionPerformed(ae);
            }
        } else if (e.getClickCount() == 1 && SwingUtilities.isRightMouseButton(e))
        {
            // Right click                   
            if (!selection.isBarSelected(barIndex))
            {
                // First do like simple click
                editor.clearSelection(selection);
                editor.selectBars(barIndex, barIndex, true);
                editor.setFocusOnBar(barIndex);
            }

            buildAndShowPopupMenu(e, "Actions/Bar", editor.getLookup());
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
        CL_Selection selection = new CL_Selection(editor.getLookup());
        if (dragStartBbIndex == -1)
        {
            // Start drag operation by selecting the current barbox
            dragStartBbIndex = bbIndex;
            editor.clearSelection(selection);
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
        if (!e.isControlDown())
        {
            // We manage only ctrl-wheel and ctrl-shift-wheel
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

        int factor = e.isShiftDown() ? zoomable.getZoomYFactor() : zoomable.getZoomXFactor();
        if (e.getWheelRotation() < 0)
        {
            factor = Math.min(100, factor + STEP);
        } else
        {
            factor = Math.max(0, factor - STEP);
        }
        LOGGER.log(Level.FINE, "editorWheelMoved() X or Y factor={0}", factor);
        var factor2 = factor;
        SwingUtilities.invokeLater(() ->
        {
            if (e.isShiftDown())
            {
                zoomable.setZoomYFactor(factor2, false);
            } else
            {
                zoomable.setZoomXFactor(factor2, false);
            }
        });      // Give time to TopComponent to become active


    }

    @Override
    public String toString()
    {
        return "CL_EditorController[" + editor.getSongModel().getName() + "]";
    }

    // ------------------------------------------------------------------------------
    // Private functions
    // ------------------------------------------------------------------------------   
    private void buildAndShowPopupMenu(MouseEvent e, String actionsPath, Lookup context)
    {
        var actions = Utilities.actionsForPath(actionsPath);
        var popupMenu = Utilities.actionsToPopup(actions.toArray(Action[]::new), context);   // This might use ContextAwareAction instances
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

}
