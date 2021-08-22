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

import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ui.ss_editor.actions.ExtendSelectionLeft;
import org.jjazz.ui.ss_editor.actions.ExtendSelectionRight;
import org.jjazz.ui.ss_editor.actions.JumpToEnd;
import org.jjazz.ui.ss_editor.actions.JumpToHome;
import org.jjazz.ui.ss_editor.actions.MoveSelectionDown;
import org.jjazz.ui.ss_editor.actions.MoveSelectionLeft;
import org.jjazz.ui.ss_editor.actions.MoveSelectionRight;
import org.jjazz.ui.ss_editor.actions.MoveSelectionUp;
import org.jjazz.ui.sptviewer.api.SptViewer;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ui.rpviewer.api.RpViewer;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.Actions;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.ss_editor.api.SS_EditorMouseListener;
import static org.jjazz.ui.utilities.api.Utilities.getGenericControlKeyStroke;
import org.jjazz.util.api.ResUtil;
import org.jjazz.rhythm.api.RpEnumerable;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;
import org.jjazz.ui.ss_editor.api.SS_ContextActionSupport;

/**
 * Controller implementation of a SS_Editor.
 */
public class SS_EditorController implements SS_EditorMouseListener
{

    /**
     * The graphical editor we control.
     */
    private final SS_Editor editor;
    /**
     * The various righ-click popupmenu depending on the selection.
     */
    private JPopupMenu popupSptMenu;
    private JPopupMenu popupRpMenu;
    private JPopupMenu popupEditorMenu;
    /**
     * The SongPart on which a drag was started.
     */
    private SongPart dragStartSpt;
    /**
     * The RhythmParameter on which a drag was started.
     */
    private RhythmParameter<?> dragStartRp;
    /**
     * To listen to selection changes
     */
    private SS_ContextActionSupport cap;
    private static final Logger LOGGER = Logger.getLogger(SS_EditorController.class.getSimpleName());

    public SS_EditorController(SS_Editor ed)
    {
        editor = ed;
        dragStartSpt = null;
        dragStartRp = null;


        // Actions created by annotations (equivalent to org.openide.awt.Actions.context())
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_DOWN), "PreviousRpValue");  //NOI18N
        editor.getActionMap().put("PreviousRpValue", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.previousrpvalue"));   //NOI18N
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_UP), "NextRpValue");   //NOI18N
        editor.getActionMap().put("NextRpValue", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.nextrpvalue"));   //NOI18N
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("I"), "InsertSpt");  //NOI18N
        editor.getActionMap().put("InsertSpt", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.insertspt"));   //NOI18N
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_I), "AppendSpt");   //NOI18N
        editor.getActionMap().put("AppendSpt", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.appendspt"));   //NOI18N
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_I,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK), "PasteAppend");  //NOI18N
        editor.getActionMap().put("PasteAppend", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.pasteappend"));   //NOI18N
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("R"), "EditRhythm");  //NOI18N
        editor.getActionMap().put("EditRhythm", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.editrhythm"));   //NOI18N
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("D"), "Duplicate");   //NOI18N
        editor.getActionMap().put("Duplicate", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.duplicatespt"));   //NOI18N
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("Z"), "ResetRpValue");  //NOI18N
        editor.getActionMap().put("ResetRpValue", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.resetrpvalue"));   //NOI18N


        // Set the delegate actions for standard Netbeans copy/cut/paste actions
        editor.getActionMap().put("cut-to-clipboard", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.cut"));   //NOI18N        
        editor.getActionMap().put("copy-to-clipboard", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.copy"));   //NOI18N
        editor.getActionMap().put("paste-from-clipboard", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.paste"));   //NOI18N

        // Change these delegate actions depending on the current selection
        cap = SS_ContextActionSupport.getInstance(editor.getLookup());
        SS_ContextActionListener cas = new SS_ContextActionListener()
        {
            boolean songMode = true;

            @Override
            public void selectionChange(SS_SelectionUtilities selection)
            {
                if (selection.isSongPartSelected() && !songMode)
                {
                    editor.getActionMap().put("copy-to-clipboard", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.copy"));   //NOI18N
                    editor.getActionMap().put("paste-from-clipboard", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.paste"));   //NOI18N
                    songMode = true;
                } else if (selection.isRhythmParameterSelected() && songMode)
                {
                    editor.getActionMap().put("copy-to-clipboard", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.copyrpvalue"));   //NOI18N
                    editor.getActionMap().put("paste-from-clipboard", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.pasterpvalue"));   //NOI18N
                    songMode = false;
                }
            }
        };
        cap.addListener(cas);


        // Delegates for our callback actions        
        editor.getActionMap()
                .put("jjazz-delete", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.removespt"));   //NOI18N
        editor.getActionMap()
                .put("jjazz-selectall", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.selectall"));   //NOI18N
        editor.getActionMap()
                .put("jjazz-edit", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.editsptname"));   //NOI18N
        editor.getActionMap()
                .put("jjazz-zoomfitwidth", Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.zoomfitwidth"));   //NOI18N

//        // Add keybindings which would be otherwise consumed by enclosing JScrollPane or other enclosing components
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("LEFT"), "MoveSelectionLeft");   //NOI18N
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("shift TAB"), "MoveSelectionLeft");  //NOI18N
        editor.getActionMap()
                .put("MoveSelectionLeft", new MoveSelectionLeft());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("RIGHT"), "MoveSelectionRight");  //NOI18N
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("TAB"), "MoveSelectionRight");  //NOI18N
        editor.getActionMap()
                .put("MoveSelectionRight", new MoveSelectionRight());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("shift LEFT"), "ExtendSelectionLeft");  //NOI18N
        editor.getActionMap()
                .put("ExtendSelectionLeft", new ExtendSelectionLeft());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("shift RIGHT"), "ExtendSelectionRight");  //NOI18N
        editor.getActionMap()
                .put("ExtendSelectionRight", new ExtendSelectionRight());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("UP"), "MoveSelectionUp");   //NOI18N
        editor.getActionMap()
                .put("MoveSelectionUp", new MoveSelectionUp());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("DOWN"), "MoveSelectionDown");  //NOI18N
        editor.getActionMap()
                .put("MoveSelectionDown", new MoveSelectionDown());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("HOME"), "JumpToHome");   //NOI18N
        editor.getActionMap()
                .put("JumpToHome", new JumpToHome());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("END"), "JumpToEnd");  //NOI18N
        editor.getActionMap()
                .put("JumpToEnd", new JumpToEnd());

    }

    @Override
    public void editSongPartRhythm(SongPart spt)
    {
        SS_SelectionUtilities selection = new SS_SelectionUtilities(editor.getLookup());
        selection.unselectAll(editor);
        editor.selectSongPart(spt, true);
        editor.setFocusOnSongPart(spt);
        Action a = Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.editrhythm");   //NOI18N
        if (a == null)
        {
            LOGGER.log(Level.SEVERE, "Can't find action: org.jjazz.ui.ss_editor.actions.editrhythm");   //NOI18N
        } else
        {
            a.actionPerformed(null);
        }
    }

    @Override
    public void editSongPartName(SongPart spt)
    {
        SS_SelectionUtilities selection = new SS_SelectionUtilities(editor.getLookup());
        selection.unselectAll(editor);
        editor.selectSongPart(spt, true);
        editor.setFocusOnSongPart(spt);

        Action a = Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.editsptname");   //NOI18N
        if (a == null)
        {
            LOGGER.log(Level.SEVERE, "Can't find the EditSptName action: org.jjazz.ui.ss_editor.actions.editsptname");   //NOI18N
        } else
        {
            a.actionPerformed(null);
        }
    }

    @Override
    public void songPartClicked(MouseEvent e, SongPart spt, boolean multiSelect)
    {
        SongPart focusedSpt = null;
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof SptViewer)
        {
            focusedSpt = ((SptViewer) c).getModel();
        }

        SS_SelectionUtilities selection = new SS_SelectionUtilities(editor.getLookup());

        LOGGER.log(Level.FINE, "songPartClicked() spt=" + spt + " multiSelect=" + multiSelect);   //NOI18N

        if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e))
        {
            if (selection.isRhythmParameterSelected() || selection.isEmpty()
                    || (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == 0)
            {
                // SIMPLE CLICK, or no previous selection set on a similar item
                LOGGER.log(Level.FINE, "    simple click");   //NOI18N
                selection.unselectAll(editor);
                editor.selectSongPart(spt, true);
                editor.setFocusOnSongPart(spt);
                if (multiSelect)
                {
                    // Also select contiguous spts with same name
                    List<SongPart> msSpts = getMultiSelectSongParts(spt);
                    for (SongPart msSpt : msSpts)
                    {
                        editor.selectSongPart(msSpt, true);
                    }
                }
            } else if ((e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) == InputEvent.CTRL_DOWN_MASK)
            {
                // CTRL CLICK
                // Just add selection, don't change focus
                boolean sptSelected = selection.isSongPartSelected(spt);
                if (spt != focusedSpt)
                {
                    // Change spt selection state
                    editor.selectSongPart(spt, !sptSelected);
                }
                if (multiSelect)
                {
                    // Multiselect mode, change contiguous spt selection state
                    boolean b = true;     // By default : if spt is focused select contiguous spts
                    if (spt != focusedSpt)
                    {
                        // Just invert the multiselect
                        b = !sptSelected;
                    }
                    List<SongPart> msSpts = getMultiSelectSongParts(spt);
                    for (SongPart msSpt : msSpts)
                    {
                        editor.selectSongPart(msSpt, b);
                    }
                }
            } else if (focusedSpt != null && (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == InputEvent.SHIFT_DOWN_MASK)
            {
                // SHIFT CLICK
                LOGGER.log(Level.FINE, "    SHIFT click");   //NOI18N
                // Set selection from focusedSpt to shift clicked Spt
                selection.unselectAll(editor);
                List<SongPart> spts = editor.getModel().getSongParts();
                int minIndex = Math.min(spts.indexOf(focusedSpt), spts.indexOf(spt));
                int maxIndex = Math.max(spts.indexOf(focusedSpt), spts.indexOf(spt));
                for (int i = minIndex; i <= maxIndex; i++)
                {
                    editor.selectSongPart(spts.get(i), true);
                }
                if (multiSelect)
                {
                    List<SongPart> msSpts = getMultiSelectSongParts(spt);
                    for (SongPart msSpt : msSpts)
                    {
                        editor.selectSongPart(msSpt, true);
                    }
                }
            }
        } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == 0)
        {
            // DOUBLE CLICK
            // Don't assume the first click was on a SongPart, it can be on something else !
            // (for example it happens when double clicking while moving near the RhythmParameter editor boundaries)
            LOGGER.log(Level.FINE, "    DOUBLE click");   //NOI18N
            Action a = Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.editsptname");   //NOI18N
            if (a == null)
            {
                LOGGER.log(Level.SEVERE, "Can't find the EditSptName action: org.jjazz.ui.ss_editor.actions.editsptname");   //NOI18N
            } else if (selection.isSongPartSelected())
            {
                a.actionPerformed(null);
            }
        } else if (e.getClickCount() == 1 && SwingUtilities.isRightMouseButton(e))
        {
            // Right click      
            LOGGER.log(Level.FINE, "    RIGHT click");   //NOI18N
            if (!selection.isSongPartSelected(spt))
            {
                // If not selected first do like simple click
                selection.unselectAll(editor);
                editor.selectSongPart(spt, true);
                editor.setFocusOnSongPart(spt);
            }


            // Reconstruct popupmenu when required
            List<? extends Action> actions = Utilities.actionsForPath("Actions/SongPart");
            actions = actions.stream().filter(a -> !(a instanceof HideIfDisabledAction) || a.isEnabled()).collect(Collectors.toList());
            int nbNonNullActions = (int) actions.stream().filter(a -> a != null).count();
            if (popupSptMenu == null || popupSptMenu.getSubElements().length != nbNonNullActions)
            {
                popupSptMenu = Utilities.actionsToPopup(actions.toArray(new Action[actions.size()]), editor);
            }

            // Display popupmenu
            popupSptMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void songPartReleased(MouseEvent e, SongPart spt)
    {
        LOGGER.log(Level.FINER, "songPartReleased() spt=" + spt);   //NOI18N
        // Managed by SS_EditorTransferHandler of the SongPart
    }

    @Override
    public void songPartDragged(MouseEvent e, SongPart spt)
    {
        LOGGER.log(Level.FINE, "songPartDragged() spt=" + spt);   //NOI18N
        // Managed by SS_EditorTransferHandler of the SongPart
    }

    @Override
    public void editorClicked(MouseEvent e)
    {
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        SongPart focusedSpt = null;
        RhythmParameter<?> focusedRp = null;
        if (c instanceof RpViewer)
        {
            focusedRp = ((RpViewer) c).getRpModel();
            focusedSpt = ((RpViewer) c).getSptModel();
        } else if (c instanceof SptViewer)
        {
            focusedSpt = ((SptViewer) c).getModel();
        }

        if (e.getClickCount() == 1 && SwingUtilities.isRightMouseButton(e))
        {
            if (popupEditorMenu == null)
            {
                List<? extends Action> actions = Utilities.actionsForPath("Actions/SS_Editor");
                popupEditorMenu = Utilities.actionsToPopup(actions.toArray(new Action[actions.size()]), editor);
            }
            popupEditorMenu.show(e.getComponent(), e.getX(), e.getY());
            // Try to restore focus           
            if (focusedRp != null)
            {
                if (editor.getModel().getSongParts().indexOf(focusedSpt) >= 0)
                {
                    editor.setFocusOnRhythmParameter(focusedSpt, focusedRp);
                }
            } else if (focusedSpt != null)
            {
                if (editor.getModel().getSongParts().indexOf(focusedSpt) >= 0)
                {
                    editor.setFocusOnSongPart(focusedSpt);
                }
            }
        }
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

        // Because wheel action can be enabled even if the TopComponent is inactive, make sure to make our TopComponent active 
        // to avoid possible problems with the global selection 
        SongStructure sgs = editor.getModel();
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.get(sgs);
        ssTc.requestActive();

        int factor = editor.getZoomHFactor();
        if (e.getWheelRotation() < 0)
        {
            factor = Math.min(100, factor + STEP);
        } else
        {
            factor = Math.max(0, factor - STEP);
        }
        LOGGER.log(Level.FINE, "editorWheelMoved() factor={0}", factor);   //NOI18N
        editor.setZoomHFactor(factor);
    }

    @Override
    public void rhythmParameterClicked(MouseEvent e, SongPart spt, RhythmParameter<?> rp)
    {
        RhythmParameter<?> focusedRp = null;
        SongPart focusedSpt = null;
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof RpViewer)
        {
            focusedRp = ((RpViewer) c).getRpModel();
            focusedSpt = ((RpViewer) c).getSptModel();
        }

        SS_SelectionUtilities selection = new SS_SelectionUtilities(editor.getLookup());

        LOGGER.log(Level.FINE, "rhythmParameterClicked() -- spt=" + spt + " rp=" + rp);   //NOI18N

        if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e))
        {
            if (selection.isSongPartSelected() || selection.isEmpty() || focusedRp == null || !RhythmParameter.
                    checkCompatibility(rp, focusedRp)
                    || (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == 0)
            {
                // SIMPLE CLICK, or no previous selection set on a similar item
                LOGGER.log(Level.FINE, "   simple click()");   //NOI18N
                selection.unselectAll(editor);
                editor.selectRhythmParameter(spt, rp, true);
                editor.setFocusOnRhythmParameter(spt, rp);
            } else if ((e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))
                    == InputEvent.CTRL_DOWN_MASK)
            {
                // CTRL CLICK
                // Just add selection, don't change focus
                LOGGER.log(Level.FINE, "   ctrl click()");   //NOI18N
                if (spt != focusedSpt)
                {
                    editor.selectRhythmParameter(spt, rp, !selection.isRhythmParameterSelected(spt, rp));
                }
            } else if (focusedRp != null && (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK
                    | InputEvent.CTRL_DOWN_MASK))
                    == InputEvent.SHIFT_DOWN_MASK)
            {
                // SHIFT CLICK
                LOGGER.log(Level.FINE, "   shift click()");   //NOI18N
                // Set selection from focusedRp to shift clicked Rp
                selection.unselectAll(editor);
                List<SongPart> spts = editor.getModel().getSongParts();
                int minIndex = Math.min(spts.indexOf(focusedSpt), spts.indexOf(spt));
                int maxIndex = Math.max(spts.indexOf(focusedSpt), spts.indexOf(spt));
                for (int i = minIndex; i <= maxIndex; i++)
                {
                    SongPart spti = spts.get(i);
                    RhythmParameter<?> rpi = RhythmParameter.findFirstCompatibleRp(spti.getRhythm().getRhythmParameters(), rp);
                    if (rpi != null)
                    {
                        editor.selectRhythmParameter(spti, rpi, true);
                    }
                }
            }
        } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
        {
            // DOUBLE CLICK 
            editRpWithCustomEditor();

        } else if (e.getClickCount() == 1 && SwingUtilities.isRightMouseButton(e))
        {
            // Right click        
            LOGGER.log(Level.FINE, "   right click()");   //NOI18N
            if (!selection.isRhythmParameterSelected(spt, rp))
            {
                // First do like simple click
                selection.unselectAll(editor);
                editor.selectRhythmParameter(spt, rp, true);
                editor.setFocusOnRhythmParameter(spt, rp);
            }
            if (popupRpMenu == null)
            {
                List<? extends Action> actions = Utilities.actionsForPath("Actions/RhythmParameter");
                popupRpMenu = Utilities.actionsToPopup(actions.toArray(new Action[actions.size()]), editor);
            }
            popupRpMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void rhythmParameterWheelMoved(MouseWheelEvent e, SongPart spt, RhythmParameter rp)
    {
        LOGGER.log(Level.FINE, "rhythmParameterWheelMoved() -- spt=" + spt + " rp=" + rp);   //NOI18N        

        boolean shift = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
        if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK)
        {
            // We dont't manage ctrl-wheel 
            // but we don't want to lose the event, it may need to be processed by the above hierarchy, i.e. enclosing JScrollPane
            Container source = (Container) e.getSource();
            Container parent = source.getParent();
            MouseEvent parentEvent = SwingUtilities.convertMouseEvent(source, e, parent);
            MouseWheelEvent parentMouseWheelEvent = new MouseWheelEvent(parent,
                    e.getID(),
                    e.getWhen(),
                    e.getModifiersEx(),
                    parentEvent.getX(),
                    parentEvent.getY(),
                    e.getXOnScreen(),
                    e.getYOnScreen(),
                    e.getClickCount(),
                    e.isPopupTrigger(),
                    e.getScrollType(),
                    e.getScrollAmount(),
                    e.getWheelRotation(),
                    e.getPreciseWheelRotation());
            parent.dispatchEvent(parentMouseWheelEvent);
            return;
        }

        SS_SelectionUtilities selection = new SS_SelectionUtilities(editor.getLookup());
        if (!selection.isRhythmParameterSelected(spt, rp) || !(rp instanceof RpEnumerable))
        {
            return;
        }

        // From here rp is an instance of Enumerable

        // Make sure our TopComponent is active so that global lookup represents our editor's selection. 
        // Because wheel action can be enabled even if the TopComponent is inactive, if editor's selection was indirectly 
        // changed while editor was not active (e.g. rhythm was changed from another TopComponent, or a chordleadsheet section was removed)
        // the SS_ContextActionSupport which listens to selection via the global lookup will have missed the selection change, causing 
        // problems in actions.
        SongStructure sgs = editor.getModel();
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.get(sgs);
        ssTc.requestActive();


        // If shift is pressed we first align the values on the first selected RP
        if (shift)
        {
            double dValue = ((RpEnumerable) rp).calculatePercentage(spt.getRPValue(rp));
            String editName = ResUtil.getString(getClass(), "CTL_SetRpValue");


            JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(editName);
            for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
            {
                SongPart spti = sptp.getSpt();
                RhythmParameter rpi = sptp.getRp();
                if (spti != spt)
                {
                    Object compatibleValue = ((RpEnumerable) rpi).calculateValue(dValue); // selected RPs might be different types (but compatible)
                    editor.getModel().setRhythmParameterValue(spti, rpi, compatibleValue);
                }
            }
            JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(editName);
        }


        // Next or previous actions
        if (e.getWheelRotation() < 0)
        {
            Action action = Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.nextrpvalue");   //NOI18N
            action.actionPerformed(null);
        } else
        {
            Action action = Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.previousrpvalue");   //NOI18N
            action.actionPerformed(null);
        }
    }

    @Override
    public void rhythmParameterDragged(MouseEvent e, SongPart spt, RhythmParameter<?> rp)
    {
        if ((e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) != 0)
        {
            // Ctrl or Shift not allowed
            return;
        }

        // LOGGER.log(Level.FINE, "rhythmParameterDragged() -- spt=" + spt + " rp=" + rp);   //NOI18N

        SS_SelectionUtilities selection = new SS_SelectionUtilities(editor.getLookup());
        List<SongPart> spts = editor.getModel().getSongParts();
        if (dragStartSpt == null)
        {
            // Start drag operation by selecting the current RhythmParameter
            dragStartSpt = spt;
            dragStartRp = rp;
            selection.unselectAll(editor);
            editor.selectRhythmParameter(spt, rp, true);
            editor.setFocusOnRhythmParameter(spt, rp);
            LOGGER.log(Level.FINE, "                      start drag dragStartSptIndex={0}", dragStartSpt);   //NOI18N
        } else
        {
            // We continue a drag operation previously started
            Point editorPoint = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), editor);
            AtomicBoolean leftSpt = new AtomicBoolean();
            SongPartParameter sptp = editor.getSongPartParameterFromPoint(editorPoint, leftSpt);
            if (sptp.getRp() == null)
            {
                return;
            }
            selection.unselectAll(editor);
            int minIndex = Math.min(spts.indexOf(dragStartSpt), spts.indexOf(sptp.getSpt()));
            int maxIndex = Math.max(spts.indexOf(dragStartSpt), spts.indexOf(sptp.getSpt()));
            for (int i = minIndex; i <= maxIndex; i++)
            {
                SongPart spti = spts.get(i);
                RhythmParameter<?> rpi = RhythmParameter.findFirstCompatibleRp(spti.getRhythm().getRhythmParameters(), dragStartRp);
                if (rpi != null)
                {
                    editor.selectRhythmParameter(spti, rpi, true);
                }
            }
        }
    }

    @Override
    public void rhythmParameterReleased(MouseEvent e, SongPart spt, RhythmParameter<?> rp)
    {
        LOGGER.log(Level.FINE, "rhythmParameterReleased() -- spt=" + spt + " rp=" + rp);   //NOI18N
        dragStartSpt = null;
    }

    @Override
    public void rhythmParameterEditWithCustomDialog(SongPart spt, RhythmParameter<?> rp)
    {
        LOGGER.fine("rhythmParameterEditWithCustomDialog() -- spt=" + spt + " rp=" + rp);
        // First set selection on this RP
        SS_SelectionUtilities selection = new SS_SelectionUtilities(editor.getLookup());
        selection.unselectAll(editor);
        editor.selectRhythmParameter(spt, rp, true);
        editor.setFocusOnRhythmParameter(spt, rp);

        // Edit
        editRpWithCustomEditor();
    }

    @Override
    public <E> void rhythmParameterEdit(SongPart spt, RhythmParameter<E> rp, E rpValue)
    {
        LOGGER.fine("rhythmParameterEdit() -- rpValue=" + rpValue);

        var sgs = editor.getModel();
        String editName = ResUtil.getString(getClass(), "CTL_SetRpValue");
        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(editName);

        sgs.setRhythmParameterValue(spt, rp, rpValue);

        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(editName);
    }

    /**
     * Get the list of SongParts contiguous to spt (before and after) sharing the same name.
     *
     * @param spt
     * @return Can be empty. spt is NOT included in the returned list.
     */
    static public List<SongPart> getMultiSelectSongParts(SongPart spt)
    {
        SongStructure sgs = spt.getContainer();
        assert sgs != null : "spt=" + spt;   //NOI18N
        List<SongPart> res = new ArrayList<>();
        List<SongPart> spts = sgs.getSongParts();
        int index = spts.indexOf(spt);
        for (int i = (index + 1); i < spts.size(); i++)
        {
            SongPart spti = spts.get(i);
            if (spti.getName().equals(spt.getName()))
            {
                res.add(spti);
            } else
            {
                break;
            }
        }
        for (int i = (index - 1); i >= 0; i--)
        {
            SongPart spti = spts.get(i);
            if (spti.getName().equals(spt.getName()))
            {
                res.add(spti);
            } else
            {
                break;
            }
        }
        return res;
    }

    //----------------------------------------------------------------------------------
    // Private methods
    //----------------------------------------------------------------------------------    
    private void editRpWithCustomEditor()
    {
        // The action will rely on the current selection
        Action action = Actions.forID("JJazz", "org.jjazz.ui.ss_editor.actions.editrpwithcustomeditor");   //NOI18N
        if (action.isEnabled())     // Sanity check
        {
            action.actionPerformed(null);
        }
    }

}
