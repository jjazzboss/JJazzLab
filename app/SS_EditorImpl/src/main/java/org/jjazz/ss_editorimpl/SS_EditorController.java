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
package org.jjazz.ss_editorimpl;

import org.jjazz.ss_editor.api.SS_Selection;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editorimpl.actions.ExtendSelectionLeft;
import org.jjazz.ss_editorimpl.actions.ExtendSelectionRight;
import org.jjazz.ss_editorimpl.actions.JumpToEnd;
import org.jjazz.ss_editorimpl.actions.JumpToHome;
import org.jjazz.ss_editorimpl.actions.MoveSelectionDown;
import org.jjazz.ss_editorimpl.actions.MoveSelectionLeft;
import org.jjazz.ss_editorimpl.actions.MoveSelectionRight;
import org.jjazz.ss_editorimpl.actions.MoveSelectionUp;
import org.jjazz.ss_editor.sptviewer.api.SptViewer;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.rpviewer.api.RpViewer;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.Actions;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_EditorMouseListener;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.rhythm.api.RpEnumerable;
import org.jjazz.ss_editor.api.SS_EditorClientProperties;
import org.jjazz.ss_editorimpl.actions.AppendSpt;
import org.jjazz.ss_editorimpl.actions.DuplicateSpt;
import org.jjazz.ss_editorimpl.api.EditRhythm;
import org.jjazz.ss_editorimpl.actions.InsertSpt;
import org.jjazz.ss_editorimpl.actions.NextRpValue;
import org.jjazz.ss_editorimpl.actions.PreviousRpValue;
import org.jjazz.ss_editorimpl.actions.PasteAppend;
import org.jjazz.ss_editorimpl.actions.ResetRpValue;
import org.jjazz.ss_editorimpl.actions.ToggleCompactView;
import org.openide.util.Lookup;

/**
 * Controller implementation of a SS_Editor.
 */
public class SS_EditorController implements SS_EditorMouseListener
{

    /**
     * The graphical editor we control.
     */
    private final SS_Editor editor;
    private final Action nextRpValueAction;
    private final Action previousRpValueAction;

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
    // private final SS_ContextActionSupport cap;
    private static final Logger LOGGER = Logger.getLogger(SS_EditorController.class.getSimpleName());

    public SS_EditorController(SS_Editor ed)
    {
        editor = ed;
        dragStartSpt = null;
        dragStartRp = null;


        // Actions also used via the mouse
        previousRpValueAction = Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.previousrpvalue");
        nextRpValueAction = Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.nextrpvalue");
        assert previousRpValueAction != null && nextRpValueAction != null;


        // Actions created by annotations (equivalent to org.openide.awt.Actions.context())
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(PreviousRpValue.KEYSTROKE, "PreviousRpValue");
        editor.getActionMap().put("PreviousRpValue", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.previousrpvalue"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(NextRpValue.KEYSTROKE, "NextRpValue");
        editor.getActionMap().put("NextRpValue", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.nextrpvalue"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(InsertSpt.KEYSTROKE, "InsertSpt");
        editor.getActionMap().put("InsertSpt", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.insertspt"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(AppendSpt.KEYSTROKE, "AppendSpt");
        editor.getActionMap().put("AppendSpt", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.appendspt"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(PasteAppend.KEYSTROKE, "PasteAppend");
        editor.getActionMap().put("PasteAppend", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.pasteappend"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(EditRhythm.KEYSTROKE, "EditRhythm");
        editor.getActionMap().put("EditRhythm", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.editrhythm"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(DuplicateSpt.KEYSTROKE, "Duplicate");
        editor.getActionMap().put("Duplicate", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.duplicatespt"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ResetRpValue.KEYSTROKE, "ResetRpValue");
        editor.getActionMap().put("ResetRpValue", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.resetrpvalue"));
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ToggleCompactView.KEYSTROKE, "ToggleCompactView");
        editor.getActionMap().put("ToggleCompactView", ToggleCompactView.getInstance(editor));


        // Set the delegate actions for standard Netbeans copy/cut/paste actions
        editor.getActionMap().put("cut-to-clipboard", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.cut"));
        editor.getActionMap().put("copy-to-clipboard", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.copy"));
        editor.getActionMap().put("paste-from-clipboard", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.paste"));


        // Delegates for our callback actions        
        editor.getActionMap().put("jjazz-delete", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.removespt"));
        editor.getActionMap().put("jjazz-selectall", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.selectall"));
        editor.getActionMap().put("jjazz-edit", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.editsptname"));
        editor.getActionMap().put("jjazz-zoomfitwidth", Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.zoomfitwidth"));


//        // Add keybindings which would be otherwise consumed by enclosing JScrollPane or other enclosing components
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("LEFT"), "MoveSelectionLeft");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift TAB"), "MoveSelectionLeft");
        editor.getActionMap().put("MoveSelectionLeft", new MoveSelectionLeft());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("RIGHT"), "MoveSelectionRight");
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("TAB"), "MoveSelectionRight");
        editor.getActionMap().put("MoveSelectionRight", new MoveSelectionRight());
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift LEFT"), "ExtendSelectionLeft");
        editor.getActionMap().put("ExtendSelectionLeft", new ExtendSelectionLeft());
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

    @Override
    public void editSongPartRhythm(SongPart spt)
    {
        SS_Selection selection = new SS_Selection(editor.getLookup());
        selection.unselectAll(editor);
        editor.selectSongPart(spt, true);
        editor.setFocusOnSongPart(spt);
        Action a = Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.editrhythm");
        if (a == null)
        {
            LOGGER.log(Level.SEVERE, "Can''t find action: org.jjazz.ss_editorimpl.actions.editrhythm");
        } else
        {
            a.actionPerformed(null);
        }
    }

    @Override
    public void editSongPartName(SongPart spt)
    {
        SS_Selection selection = new SS_Selection(editor.getLookup());
        selection.unselectAll(editor);
        editor.selectSongPart(spt, true);
        editor.setFocusOnSongPart(spt);

        Action a = Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.editsptname");
        if (a == null)
        {
            LOGGER.log(Level.SEVERE, "Can''t find the EditSptName action: org.jjazz.ss_editorimpl.actions.editsptname");
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
        if (c instanceof SptViewer sptv)
        {
            focusedSpt = sptv.getModel();
        }

        SS_Selection selection = new SS_Selection(editor.getLookup());

        LOGGER.log(Level.FINE, "songPartClicked() spt={0} multiSelect={1}", new Object[]
        {
            spt, multiSelect
        });

        if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e))
        {
            if (selection.isRhythmParameterSelected() || selection.isEmpty()
                || (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == 0)
            {
                // SIMPLE CLICK, or no previous selection set on a similar item
                LOGGER.log(Level.FINE, "    simple click");
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
                LOGGER.log(Level.FINE, "    SHIFT click");
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
            LOGGER.log(Level.FINE, "    DOUBLE click");
            Action a = Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.editsptname");
            if (a == null)
            {
                LOGGER.log(Level.SEVERE, "Can''t find the EditSptName action: org.jjazz.ss_editorimpl.actions.editsptname");
            } else if (selection.isSongPartSelected())
            {
                a.actionPerformed(null);
            }
        } else if (e.getClickCount() == 1 && SwingUtilities.isRightMouseButton(e))
        {
            // Right click      
            LOGGER.log(Level.FINE, "    RIGHT click");
            if (!selection.isSongPartSelected(spt))
            {
                // If not selected first do like simple click
                selection.unselectAll(editor);
                editor.selectSongPart(spt, true);
                editor.setFocusOnSongPart(spt);
            }

            buildAndShowPopupMenu(e, "Actions/SongPart", editor.getLookup());
        }
    }

    @Override
    public void songPartReleased(MouseEvent e, SongPart spt)
    {
        LOGGER.log(Level.FINER, "songPartReleased() spt={0}", spt);
        // Managed by SS_EditorTransferHandler of the SongPart
    }

    @Override
    public void songPartDragged(MouseEvent e, SongPart spt)
    {
        LOGGER.log(Level.FINE, "songPartDragged() spt={0}", spt);
        // Managed by SS_EditorTransferHandler of the SongPart
    }

    @Override
    public void editorClicked(MouseEvent e)
    {
        if (e.getClickCount() == 1 && SwingUtilities.isRightMouseButton(e))
        {
            buildAndShowPopupMenu(e, "Actions/SS_Editor", editor.getLookup());
        }

        // Part of the fix for Issue #582
        editor.requestFocusInWindow();
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

        // Because wheel action can be enabled even if the TopComponent is inactive, make sure to make our TopComponent active 
        // to avoid possible problems with the global selection 
        SongStructure sgs = editor.getModel();
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.get(sgs);
        ssTc.requestActive();

        int factor = e.isShiftDown()
            ? SS_EditorClientProperties.getZoomYFactor(editor.getSongModel())
            : SS_EditorClientProperties.getZoomXFactor(editor.getSongModel());
        if (e.getWheelRotation() < 0)
        {
            factor = Math.min(100, factor + STEP);
        } else
        {
            factor = Math.max(0, factor - STEP);
        }
        LOGGER.log(Level.FINE, "editorWheelMoved() factor={0}", factor);
        var factor2 = factor;
        SwingUtilities.invokeLater(() ->
        {
            // Give time for TopComponent to become active
            if (e.isShiftDown())
            {
                SS_EditorClientProperties.setZoomYFactor(editor.getSongModel(), factor2);
            } else
            {
                SS_EditorClientProperties.setZoomXFactor(editor.getSongModel(), factor2);
            }
        }
        );
    }

    @Override
    public void rhythmParameterClicked(MouseEvent e, SongPart spt, RhythmParameter<?> rp)
    {
        RhythmParameter<?> focusedRp = null;
        SongPart focusedSpt = null;
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof RpViewer rpv)
        {
            focusedRp = rpv.getRpModel();
            focusedSpt = rpv.getSptModel();
        }

        SS_Selection selection = new SS_Selection(editor.getLookup());

        LOGGER.log(Level.FINE, "rhythmParameterClicked() -- spt={0} rp={1}", new Object[]
        {
            spt, rp
        });

        if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e))
        {
            if (selection.isSongPartSelected() || selection.isEmpty() || focusedRp == null
                || !rp.isCompatibleWith(focusedRp)
                || (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == 0)
            {
                // SIMPLE CLICK, or no previous selection set on a similar item
                LOGGER.log(Level.FINE, "   simple click()");
                selection.unselectAll(editor);
                editor.selectRhythmParameter(spt, rp, true);
                editor.setFocusOnRhythmParameter(spt, rp);

            } else if ((e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) == InputEvent.CTRL_DOWN_MASK)
            {
                // CTRL CLICK
                // Just add selection, don't change focus
                LOGGER.log(Level.FINE, "   ctrl click()");
                if (spt != focusedSpt)
                {
                    editor.selectRhythmParameter(spt, rp, !selection.isRhythmParameterSelected(spt, rp));
                }

            } else if ((e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == InputEvent.SHIFT_DOWN_MASK)
            {
                // SHIFT CLICK
                LOGGER.log(Level.FINE, "   shift click()");
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
            LOGGER.log(Level.FINE, "   right click()");
            if (!selection.isRhythmParameterSelected(spt, rp))
            {
                // First do like simple click
                selection.unselectAll(editor);
                editor.selectRhythmParameter(spt, rp, true);
                editor.setFocusOnRhythmParameter(spt, rp);
            }

            buildAndShowPopupMenu(e, "Actions/RhythmParameter", editor.getLookup());
        }
    }

    @SuppressWarnings(
        {
            "unchecked", "rawtypes"
        })
    @Override
    public void rhythmParameterWheelMoved(MouseWheelEvent e, SongPart spt, RhythmParameter rp)
    {
        LOGGER.log(Level.FINE, "rhythmParameterWheelMoved() -- spt={0} rp={1}", new Object[]
        {
            spt, rp
        });

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

        SS_Selection selection = new SS_Selection(editor.getLookup());
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


        // Fix Issue #347: need to give time for TopComponent to become active if it was not the case
        SwingUtilities.invokeLater(() ->
        {
            // Next or previous actions            
            if (e.getWheelRotation() < 0)
            {
                nextRpValueAction.actionPerformed(null);
            } else
            {
                previousRpValueAction.actionPerformed(null);
            }
        });
    }

    @Override
    public void rhythmParameterDragged(MouseEvent e, SongPart spt, RhythmParameter<?> rp)
    {
        if ((e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) != 0)
        {
            // Ctrl or Shift not allowed
            return;
        }

        // LOGGER.log(Level.FINE, "rhythmParameterDragged() -- spt=" + spt + " rp=" + rp);   

        SS_Selection selection = new SS_Selection(editor.getLookup());
        List<SongPart> spts = editor.getModel().getSongParts();
        if (dragStartSpt == null)
        {
            // Start drag operation by selecting the current RhythmParameter
            dragStartSpt = spt;
            dragStartRp = rp;
            selection.unselectAll(editor);
            editor.selectRhythmParameter(spt, rp, true);
            editor.setFocusOnRhythmParameter(spt, rp);
            LOGGER.log(Level.FINE, "                      start drag dragStartSptIndex={0}", dragStartSpt);
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
        LOGGER.log(Level.FINE, "rhythmParameterReleased() -- spt={0} rp={1}", new Object[]
        {
            spt, rp
        });
        dragStartSpt = null;
    }

    @Override
    public void rhythmParameterEditWithCustomDialog(SongPart spt, RhythmParameter<?> rp)
    {
        LOGGER.log(Level.FINE, "rhythmParameterEditWithCustomDialog() -- spt={0} rp={1}", new Object[]
        {
            spt, rp
        });
        // First set selection on this RP
        SS_Selection selection = new SS_Selection(editor.getLookup());
        selection.unselectAll(editor);
        editor.selectRhythmParameter(spt, rp, true);
        editor.setFocusOnRhythmParameter(spt, rp);

        // Edit
        editRpWithCustomEditor();
    }

    @Override
    public <E> void rhythmParameterEdit(SongPart spt, RhythmParameter<E> rp, E rpValue)
    {
        LOGGER.log(Level.FINE, "rhythmParameterEdit() -- rpValue={0}", rpValue);

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
        assert sgs != null : "spt=" + spt;
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
        Action action = Actions.forID("JJazz", "org.jjazz.ss_editorimpl.actions.editrpwithcustomeditor");
        if (action.isEnabled())     // Sanity check
        {
            action.actionPerformed(null);
        }
    }

    private void buildAndShowPopupMenu(MouseEvent e, String actionsPath, Lookup context)
    {
        var actions = Utilities.actionsForPath(actionsPath);
        actions.removeIf(a -> a instanceof HideIfDisabledAction && !a.isEnabled());
        var popupMenu = Utilities.actionsToPopup(actions.toArray(Action[]::new), context);   // This might use ContextAwareAction instances
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

}
