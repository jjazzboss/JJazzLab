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
package org.jjazz.ui.cl_editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import static org.jjazz.ui.cl_editor.actions.Bundle.*;
import org.jjazz.ui.cl_editor.api.CL_ContextActionListener;
import org.jjazz.ui.cl_editor.api.CL_ContextActionSupport;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.accentcrash")
@ActionRegistration(displayName = "#CTL_AccentCrash", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbolAccent", position = 200)
        })
@Messages("CTL_AccentCrash=Auto > Crash > No Crash")
public final class AccentCrash extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{
    private CL_ContextActionSupport cap;
    private final Lookup context;
    private String undoText = CTL_AccentCrash();

    public AccentCrash()
    {
        this(Utilities.actionsGlobalContext());
    }

    public AccentCrash(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("H"));
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();


        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);


        for (CLI_ChordSymbol item : selection.getSelectedChordSymbols())
        {
            ExtChordSymbol ecs = item.getData();
            ChordRenderingInfo cri = ecs.getRenderingInfo();
            if (cri.hasOneFeature(Feature.ACCENT, Feature.ACCENT_STRONGER))
            {
                ChordRenderingInfo newCri = next(cri);
                ExtChordSymbol newCs = new ExtChordSymbol(ecs, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
                item.getContainer().changeItem(item, newCs);
            }
        }

        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = false;
        if (selection.isItemSelected())
        {
            b = selection.getSelectedItems().stream()
                    .filter(item -> item instanceof CLI_ChordSymbol)
                    .anyMatch(item -> ((CLI_ChordSymbol) item).getData().getRenderingInfo().hasOneFeature(Feature.ACCENT, Feature.ACCENT_STRONGER));
        }
        setEnabled(b);
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new AccentCrash(context);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        // Nothing
    }

    private ChordRenderingInfo next(ChordRenderingInfo cri)
    {
        var features = cri.getFeatures();

        if (!cri.hasOneFeature(Feature.CRASH, Feature.NO_CRASH))
        {
            features.add(Feature.CRASH);

        } else if (cri.hasOneFeature(Feature.CRASH))
        {
            features.remove(Feature.CRASH);
            features.add(Feature.NO_CRASH);
        } else if (cri.hasOneFeature(Feature.NO_CRASH))
        {
            features.remove(Feature.NO_CRASH);
        }

        ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());

        return newCri;
    }


}
