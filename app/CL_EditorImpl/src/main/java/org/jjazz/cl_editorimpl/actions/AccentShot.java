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
import org.jjazz.cl_editor.api.CL_ContextAction;
import java.util.EnumSet;
import java.util.logging.Logger;
import static javax.swing.Action.NAME;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.cl_editor.api.CL_Selection;
import static org.jjazz.cl_editorimpl.actions.AccentNormal.createRadioButtonMenuItem;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.Presenter;

/**
 * Set a shot accent on selected chord symbols.
 * <p>
 * Action is actually performed via a checkbox menu item (see Presenter.Popup).
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.accentshot")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbolInterpretation", position = 30, separatorAfter = 31)
        })
public final class AccentShot extends CL_ContextAction implements Presenter.Popup
{

    private JRadioButtonMenuItem rbMenuItem;
    private static final Logger LOGGER = Logger.getLogger(AccentShot.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_AccentShot"));
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES));
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        setEnabled(selection.isChordSymbolSelected());
        updateMenuItem(selection);
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        var selection = getSelection();
        if (event instanceof ItemChangedEvent && selection.getSelectedItems().contains(event.getItem()))
        {
            updateMenuItem(selection);
        }
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        // Nothing
    }

    // ============================================================================================= 
    // Presenter.Popup
    // =============================================================================================   
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (rbMenuItem == null)
        {
            rbMenuItem = createRadioButtonMenuItem(getActionName(), () -> setAccentShot());
        }

        updateMenuItem(getSelection());

        return rbMenuItem;
    }


    private void setAccentShot()
    {
        ChordLeadSheet cls = getActiveChordLeadSheet();
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(getActionName());


        for (CLI_ChordSymbol item : getSelection().getSelectedChordSymbols())
        {
            ExtChordSymbol ecs = item.getData();
            ChordRenderingInfo cri = ecs.getRenderingInfo();
            var features = cri.getFeatures();
            features.remove(Feature.HOLD);
            features.add(Feature.SHOT);
            if (!cri.hasOneFeature(Feature.ACCENT, Feature.ACCENT_STRONGER))
            {
                features.add(Feature.ACCENT);
            }
            ChordRenderingInfo newCri = new ChordRenderingInfo(cri, features);
            ExtChordSymbol newCs = ecs.getCopy(null, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
            item.getContainer().changeItem(item, newCs);
        }

        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(getActionName());
    }

    private void updateMenuItem(CL_Selection selection)
    {
        if (rbMenuItem == null)
        {
            return;
        }
        // Update the checkbox: select it if only all chord symbols use Accent Hold
        boolean b = selection.getSelectedChordSymbols().stream()
                .map(cliCs -> cliCs.getData().getRenderingInfo())
                .allMatch(cri -> cri.hasOneFeature(Feature.ACCENT, Feature.ACCENT_STRONGER) && cri.hasOneFeature(Feature.SHOT));
        rbMenuItem.setSelected(b);
        rbMenuItem.setEnabled(isEnabled());
    }
}
