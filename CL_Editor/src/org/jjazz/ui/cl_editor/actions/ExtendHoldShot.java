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
import java.awt.event.ItemEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
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
import org.openide.util.actions.Presenter;

@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.extendholdshot")
@ActionRegistration(displayName = "#CTL_ExtendHoldShot", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbolAccent", position = 400)
        })
@Messages("CTL_ExtendHoldShot=Hold/Shot With More Instruments")
public final class ExtendHoldShot extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, Presenter.Popup
{

    private CL_ContextActionSupport cap;
    private final Lookup context;
    private JCheckBoxMenuItem checkBox;
    private String undoText = CTL_ExtendHoldShot();
    private static final Logger LOGGER = Logger.getLogger(ExtendHoldShot.class.getSimpleName());

    public ExtendHoldShot()
    {
        this(Utilities.actionsGlobalContext());
    }

    public ExtendHoldShot(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("M"));
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
            if (cri.hasOneFeature(Feature.HOLD, Feature.SHOT))
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
                    .anyMatch(item -> ((CLI_ChordSymbol) item).getData().getRenderingInfo().hasOneFeature(Feature.HOLD, Feature.SHOT));
        }
        setEnabled(b);
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new ExtendHoldShot(context);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        // Nothing
    }

    @Override
    public JMenuItem getPopupPresenter()
    {
        if (checkBox == null)
        {
            checkBox = new JCheckBoxMenuItem(CTL_ExtendHoldShot());
            checkBox.addItemListener(evt -> toggleExtend(evt.getStateChange() == ItemEvent.SELECTED));
        }

        LOGGER.severe("getPopupPresenter() called");
        // Update the checkbox: select it if only all chord symbols use Extend
        CL_SelectionUtilities selection = cap.getSelection();
        boolean accentNormal = selection.getSelectedItems().stream()
                .filter(item -> item instanceof CLI_ChordSymbol)
                .anyMatch(item -> !((CLI_ChordSymbol) item).getData().getRenderingInfo().hasOneFeature(Feature.HOLD_SHOT_MORE_INSTRUMENTS));
        checkBox.setSelected(!accentNormal);

        return checkBox;
    }

    private ChordRenderingInfo next(ChordRenderingInfo cri)
    {
        var features = cri.getFeatures();

        if (!cri.hasOneFeature(Feature.HOLD_SHOT_MORE_INSTRUMENTS))
        {
            features.add(Feature.HOLD_SHOT_MORE_INSTRUMENTS);

        } else
        {
            features.remove(Feature.HOLD_SHOT_MORE_INSTRUMENTS);
        }

        ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());

        return newCri;
    }

     private void toggleExtend(boolean extend)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();

        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);

        for (CLI_ChordSymbol item : selection.getSelectedChordSymbols())
        {
            ExtChordSymbol ecs = item.getData();
            ChordRenderingInfo cri = ecs.getRenderingInfo();
            var features = cri.getFeatures();

            if (extend && !features.contains(Feature.HOLD_SHOT_MORE_INSTRUMENTS))
            {
                features.add(Feature.HOLD_SHOT_MORE_INSTRUMENTS);
                ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());
                ExtChordSymbol newCs = new ExtChordSymbol(ecs, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
                item.getContainer().changeItem(item, newCs);

            } else if (!extend && features.contains(Feature.HOLD_SHOT_MORE_INSTRUMENTS))
            {
                features.remove(Feature.HOLD_SHOT_MORE_INSTRUMENTS);
                ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());
                ExtChordSymbol newCs = new ExtChordSymbol(ecs, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
                item.getContainer().changeItem(item, newCs);

            }
        }

        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }

}
