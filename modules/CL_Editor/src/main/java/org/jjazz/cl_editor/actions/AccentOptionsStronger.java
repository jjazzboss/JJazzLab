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
package org.jjazz.cl_editor.actions;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.ItemChangedEvent;
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

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.accentstronger")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbolInterpretation", position = 150)
        })
public final class AccentOptionsStronger extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, Presenter.Popup, ClsChangeListener
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("S");

    private CL_ContextActionSupport cap;
    private final Lookup context;
    private final String undoText = ResUtil.getString(getClass(), "CTL_AccentStronger");
    private ChordLeadSheet currentCls;
    private JCheckBoxMenuItem cbMenuItem;
    private static final Logger LOGGER = Logger.getLogger(AccentOptionsStronger.class.getSimpleName());

    public AccentOptionsStronger()
    {
        this(Utilities.actionsGlobalContext());
    }

    public AccentOptionsStronger(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        // Called via controller/keyboard shortcut
        CL_SelectionUtilities selection = cap.getSelection();
        if (selection.isChordSymbolSelected())
        {
            var cliCs0 = selection.getSelectedChordSymbols().get(0);
            boolean b = cliCs0.getData().getRenderingInfo().hasOneFeature(Feature.ACCENT_STRONGER);
            setAccent(!b);
        }
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        // Need to listen to possible CLI_ChordSymbol accent features changes that may occur while selection is unchanged
        ChordLeadSheet cls = selection.getChordLeadSheet();
        if (cls != currentCls)
        {
            if (currentCls != null)
            {
                currentCls.removeClsChangeListener(this);
            }
            currentCls = cls;
            if (currentCls != null)
            {
                currentCls.addClsChangeListener(this);
            }
        }


        boolean b = selection.getSelectedChordSymbols().stream()
                .map(cliCs -> cliCs.getData().getRenderingInfo())
                .allMatch(cri -> cri.hasOneFeature(Feature.ACCENT, Feature.ACCENT_STRONGER));
        setEnabled(b);
        updateMenuItem();
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new AccentOptionsStronger(context);
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
            cbMenuItem.setAccelerator(KEYSTROKE);
            cbMenuItem.addActionListener(evt -> setAccent(cbMenuItem.isSelected()));
            cbMenuItem.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true);
        }

        updateMenuItem();

        return cbMenuItem;
    }

    // ============================================================================================= 
    // ClsChangeListener implementation
    // =============================================================================================      
    @Override
    public void authorizeChange(ClsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        var selection = cap.getSelection();
        if (event instanceof ItemChangedEvent && selection.getSelectedItems().contains(event.getItem()))
        {
            selectionChange(selection);
        }
    }

    private void setAccent(boolean stronger)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();

        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);

        for (CLI_ChordSymbol item : selection.getSelectedChordSymbols())
        {
            ExtChordSymbol ecs = item.getData();
            ChordRenderingInfo cri = ecs.getRenderingInfo();
            if (!cri.hasOneFeature(Feature.ACCENT, Feature.ACCENT_STRONGER))
            {
                continue;
            }

            var features = cri.getFeatures();            
            if (stronger)
            {
                features.add(Feature.ACCENT_STRONGER);
                features.remove(Feature.ACCENT);
            } else
            {
                features.add(Feature.ACCENT);
                features.remove(Feature.ACCENT_STRONGER);
            }

            ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());
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

        // Update the checkbox: select it if only all chord symbols have a Stronger Accent
        CL_SelectionUtilities selection = cap.getSelection();
        boolean b = selection.getSelectedChordSymbols().stream()
                .allMatch(cliCs -> cliCs.getData().getRenderingInfo().hasOneFeature(Feature.ACCENT_STRONGER));
        cbMenuItem.setSelected(b);

        cbMenuItem.setEnabled(isEnabled());
    }
}
