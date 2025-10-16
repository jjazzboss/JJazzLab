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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_ContextAction.ListeningTarget;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.harmony.api.Position;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;

/**
 * Move selected items left.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.moveitemright")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            // Only via keyboard shortcut
        })
public class MoveItemRight extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_RIGHT);
    private static final Logger LOGGER = Logger.getLogger(MoveItemLeft.class.getSimpleName());

  @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_MoveItemRight"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION));        
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        MoveItemRight.performMove(selection, getActionName(), true);
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = selection.isItemSelected();
        setEnabled(b);
    }

    // ===========================================================================================================
    // Private methods
    // ===========================================================================================================    

    static protected void performMove(CL_Selection selection, String actionText, boolean moveRight)
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
