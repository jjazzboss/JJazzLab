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
package org.jjazz.cl_editorimpl.actions;

import org.jjazz.cl_editor.api.CL_ContextActionListener;
import org.jjazz.cl_editor.api.CL_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.harmony.api.Position;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

/**
 * Move selected items left.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.moveitemright")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            // Only via keyboard shortcut
        })
public class MoveItemRight extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_RIGHT);
    private Lookup context;
    private CL_ContextActionSupport cap;
    private final String undoText = ResUtil.getString(getClass(), "CTL_MoveItemRight");
    private static final Logger LOGGER = Logger.getLogger(MoveItemLeft.class.getSimpleName());

    public MoveItemRight()
    {
        this(Utilities.actionsGlobalContext());
    }

    private MoveItemRight(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
//        Icon icon = SystemAction.get(DeleteAction.class).getIcon();
//        putValue(SMALL_ICON, icon);
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new MoveItemRight(context);
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        performMove(cap.getSelection(), undoText, true);
    }


    // ===========================================================================================================
    // CL_ContextActionListener interface
    // ===========================================================================================================    
    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = selection.isItemSelected();
        setEnabled(b);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        selectionChange(cap.getSelection());
    }

    // ===========================================================================================================
    // Private methods
    // ===========================================================================================================    

    static protected void performMove(CL_SelectionUtilities selection, String actionText, boolean moveRight)
    {
        ChordLeadSheet cls = selection.getChordLeadSheet();

        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(actionText);

        if (selection.isSectionSelected())
        {
            moveSections(selection.getSelectedSections(), moveRight);
        } else if (selection.isChordSymbolSelected())
        {
            moveChordSymbols(selection.getSelectedChordSymbols(), moveRight);
        } else if (selection.isBarAnnotationSelected())
        {
            moveBarAnnotations(selection.getSelectedBarAnnotations(), moveRight);
        }

        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(actionText);
    }

    static protected void moveSections(List<CLI_Section> cliSections, boolean moveRight)
    {
        // Can not move over an existing section
        for (var cliSection : cliSections)
        {
            ChordLeadSheet model = cliSection.getContainer();
            int barIndex = cliSection.getPosition().getBar();
            int targetBar = moveRight ? barIndex + 1 : barIndex - 1;
            if (targetBar < 0 || targetBar >= model.getSizeInBars())
            {
                continue;
            }
            if (model.getItems(targetBar, targetBar, CLI_Section.class).isEmpty())
            {
                try
                {
                    // No section on the previous bar, move ok
                    model.moveSection(cliSection, targetBar);
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    static protected void moveBarAnnotations(List<CLI_BarAnnotation> cliBarAnnotations, boolean moveRight)
    {
        // Can not move over an existing bar annotation
        for (var cliBa : cliBarAnnotations)
        {
            ChordLeadSheet model = cliBa.getContainer();
            int barIndex = cliBa.getPosition().getBar();
            int targetBar = moveRight ? barIndex + 1 : barIndex - 1;
            if (targetBar < 0 || targetBar >= model.getSizeInBars())
            {
                continue;
            }
            if (model.getItems(targetBar, targetBar, CLI_BarAnnotation.class).isEmpty())
            {
                model.moveItem(cliBa, new Position(targetBar));
            }
        }
    }

    static protected void moveChordSymbols(List<CLI_ChordSymbol> cliChordSymbols, boolean moveRight)
    {
        for (var cliCs : cliChordSymbols)
        {
            ChordLeadSheet model = cliCs.getContainer();
            Position pos = cliCs.getPosition();
            int barIndex = pos.getBar();
            int targetBar = moveRight ? barIndex + 1 : barIndex - 1;
            if (targetBar < 0 || targetBar >= model.getSizeInBars())
            {
                continue;
            }
            model.moveItem(cliCs, new Position(targetBar, pos.getBeat()));   // will do nothing if move over an equal chord symbol
        }
    }
}
