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
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.Presenter;

/**
 * Set extended hold/shot accent.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.extendholdshot")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbolInterpretation", position = 401)
        })
public final class AccentOptionsExtendHoldShot extends CL_ContextAction implements Presenter.Popup
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("X");
    private JCheckBoxMenuItem cbMenuItem;
    private static final Logger LOGGER = Logger.getLogger(AccentOptionsExtendHoldShot.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_AccentExtendHoldShot"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES));        
    }


    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = selection.getSelectedChordSymbols().stream()
                .map(cliCs -> cliCs.getData().getRenderingInfo())
                .allMatch(cri -> cri.getAccentFeature() != null);
        setEnabled(b);
        updateMenuItem(selection);
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        var selection = getSelection();
        if (event instanceof ItemChangedEvent && selection.getSelectedItems().contains(event.getItem()))
        {
            selectionChange(selection);
        }
    }

    /**
     * Called when action triggered via the keyboard shortcut.
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev)
    {
        CL_Selection selection = getSelection();
        if (selection.isChordSymbolSelected())
        {
            var cliCs0 = selection.getSelectedChordSymbols().get(0);
            boolean b = cliCs0.getData().getRenderingInfo().hasOneFeature(Feature.EXTENDED_HOLD_SHOT);
            setExtended(!b);
        }
    }

    /**
     * Not used since we have overridden actionPerformed(ActionEvent).
     *
     * @param cls
     * @param selection
     */
    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
    }

    // ============================================================================================= 
    // Presenter.Popup
    // =============================================================================================   
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (cbMenuItem == null)
        {
            cbMenuItem = new JCheckBoxMenuItem(getActionName());
            cbMenuItem.setAccelerator(KEYSTROKE);
            cbMenuItem.addActionListener(evt -> setExtended(cbMenuItem.isSelected()));
            cbMenuItem.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true);
        }

        updateMenuItem(getSelection());

        return cbMenuItem;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================   

    private void setExtended(boolean extended)
    {
        ChordLeadSheet cls = getActiveChordLeadSheet();
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(getActionName());

        for (CLI_ChordSymbol item : getSelection().getSelectedChordSymbols())
        {
            ExtChordSymbol ecs = item.getData();
            ChordRenderingInfo cri = ecs.getRenderingInfo();
            var features = cri.getFeatures();

            if (extended)
            {
                features.add(Feature.EXTENDED_HOLD_SHOT);
            } else
            {
                features.remove(Feature.EXTENDED_HOLD_SHOT);
            }

            ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());
            ExtChordSymbol newCs = ecs.getCopy(null, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
            item.getContainer().changeItem(item, newCs);
        }

        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(getActionName());
    }

    private void updateMenuItem(CL_Selection selection)
    {
        if (cbMenuItem == null)
        {
            return;
        }
        // Update the checkbox: select it if only all chord symbols use Extend
        boolean b = selection.getSelectedChordSymbols().stream()
                .map(cliCs -> cliCs.getData().getRenderingInfo())
                .allMatch(cri -> cri.hasOneFeature(Feature.EXTENDED_HOLD_SHOT));
        cbMenuItem.setSelected(b);
        cbMenuItem.setEnabled(isEnabled());
    }
}
