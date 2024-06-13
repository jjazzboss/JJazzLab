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
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.cl_editor.api.CL_ContextActionListener;
import org.jjazz.cl_editor.api.CL_ContextActionSupport;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.interpretation.basspedal")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbolInterpretation", position = 1000, separatorBefore = 999)
        })
public final class InterpretationBassPedal extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, Presenter.Popup
{

    private CL_ContextActionSupport cap;
    private final Lookup context;
    private JCheckBoxMenuItem cbMenuItem;
    private final String undoText = ResUtil.getString(getClass(), "CTL_InterpretationBassPedal");
    private static final Logger LOGGER = Logger.getLogger(InterpretationBassPedal.class.getSimpleName());

    public InterpretationBassPedal()
    {
        this(Utilities.actionsGlobalContext());
    }

    public InterpretationBassPedal(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        // putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("X"));
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        // not used
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        setEnabled(selection.isChordSymbolSelected());
        updateMenuItem();
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new InterpretationBassPedal(context);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        // Nothing
    }

    // ============================================================================================= 
    // Presenter.Popup
    // =============================================================================================   
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (cbMenuItem == null)
        {
            cbMenuItem = new JCheckBoxMenuItem(undoText);
            // rbMenuItem.setAccelerator(KEYSTROKE);
            cbMenuItem.addActionListener(evt -> 
            {
                setBassPedal(cbMenuItem.isSelected());
            });
            cbMenuItem.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true);
        }

        updateMenuItem();

        return cbMenuItem;
    }


    private void setBassPedal(boolean b)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();


        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);


        for (CLI_ChordSymbol item : selection.getSelectedChordSymbols())
        {
            ExtChordSymbol ecs = item.getData();
            ChordRenderingInfo cri = ecs.getRenderingInfo();
            var features = cri.getFeatures();
            if (b)
            {
                features.add(Feature.PEDAL_BASS);
            } else
            {
                features.remove(Feature.PEDAL_BASS);
            }
            ChordRenderingInfo newCri = new ChordRenderingInfo(cri, features);
            ExtChordSymbol newCs = ecs.getCopy(null, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
            item.getContainer().changeItem(item, newCs);
        }

        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }

    private void updateMenuItem()
    {
        if (cbMenuItem == null)
        {
            return;
        }
        
        // Update the checkbox: select if only all chord symbols use bass pedal
        CL_SelectionUtilities selection = cap.getSelection();
        boolean b = selection.getSelectedChordSymbols().stream()
                .map(cliCs -> cliCs.getData().getRenderingInfo())
                .allMatch(cri -> cri.hasOneFeature(Feature.PEDAL_BASS));
        cbMenuItem.setSelected(b);
        cbMenuItem.setEnabled(isEnabled());
    }

}
