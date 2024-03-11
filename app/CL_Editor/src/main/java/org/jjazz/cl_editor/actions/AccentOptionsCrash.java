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
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
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
import org.openide.awt.DynamicMenuContent;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.accentcrash")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbolInterpretation", position = 400)
        })
public final class AccentOptionsCrash extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, Presenter.Popup, ClsChangeListener
{

    private enum CrashState
    {
        AUTO, ALWAYS, NEVER
    };
    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("H");
    private CL_ContextActionSupport cap;
    private final Lookup context;
    private MyMenuItem dynMenuItem;
    private ChordLeadSheet currentCls;
    private final String undoText = ResUtil.getString(getClass(), "CTL_AccentChangeCrash");

    public AccentOptionsCrash()
    {
        this(Utilities.actionsGlobalContext());
    }

    public AccentOptionsCrash(Lookup context)
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
            var cri = selection.getSelectedChordSymbols().get(0).getData().getRenderingInfo();
            changeSelectedChordSymbols(nextState(cri));
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

        if (dynMenuItem != null)
        {
            dynMenuItem.setEnabled(b);
            dynMenuItem.update();
        }

    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new AccentOptionsCrash(context);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        // Nothing
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

    // ============================================================================================= 
    // Presenter.Popup
    // =============================================================================================   
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (dynMenuItem == null)
        {
            dynMenuItem = new MyMenuItem();
        }
        dynMenuItem.update();
        return dynMenuItem;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================      

    private CrashState nextState(ChordRenderingInfo cri)
    {
        CrashState res = CrashState.AUTO;
        if (!cri.hasOneFeature(Feature.CRASH, Feature.NO_CRASH))
        {
            res = CrashState.ALWAYS;

        } else if (cri.hasOneFeature(Feature.CRASH))
        {
            res = CrashState.NEVER;
        }
        return res;
    }

    /**
     * Change selected chord symbols to the specified crash state.
     *
     * @param crashState
     */
    private void changeSelectedChordSymbols(CrashState crashState)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();

        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);

        for (CLI_ChordSymbol item : selection.getSelectedChordSymbols())
        {
            ExtChordSymbol ecs = item.getData();
            ChordRenderingInfo cri = ecs.getRenderingInfo();
            var features = cri.getFeatures();

            switch (crashState)
            {
                case AUTO ->
                {
                    features.remove(Feature.CRASH);
                    features.remove(Feature.NO_CRASH);
                }
                case ALWAYS ->
                {
                    features.remove(Feature.NO_CRASH);
                    features.add(Feature.CRASH);
                }
                case NEVER ->
                {
                    features.remove(Feature.CRASH);
                    features.add(Feature.NO_CRASH);
                }
                default ->
                {
                    throw new IllegalStateException("state=" + crashState);
                }
            }

            ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());
            ExtChordSymbol newCs = ecs.getCopy(null, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
            item.getContainer().changeItem(item, newCs);

        }

        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }


    /**
     * DynamicMenuContent item which is used to return the 2 checkboxes
     */
    private class MyMenuItem extends JMenuItem implements DynamicMenuContent
    {

        private JComponent[] components = new JComponent[2];
        public JCheckBoxMenuItem cbm_crashAlways;
        public JCheckBoxMenuItem cbm_crashNever;

        public MyMenuItem()
        {
            cbm_crashAlways = new JCheckBoxMenuItem(ResUtil.getString(getClass(), "CTL_AccentCrashAlways"));
            cbm_crashAlways.setAccelerator(KEYSTROKE);
            cbm_crashAlways.addActionListener(evt -> setCrashAlways(cbm_crashAlways.isSelected()));
            cbm_crashAlways.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true);

            cbm_crashNever = new JCheckBoxMenuItem(ResUtil.getString(getClass(), "CTL_AccentCrashNever"));
            cbm_crashNever.setAccelerator(KEYSTROKE);
            cbm_crashNever.addActionListener(evt -> setCrashNever(cbm_crashNever.isSelected()));
            cbm_crashNever.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true);

            components[0] = cbm_crashNever;
            components[1] = cbm_crashAlways;

            setEnabled(false);
        }

        public void update()
        {
            CL_SelectionUtilities selection = cap.getSelection();

            boolean crashAlways = selection.getSelectedChordSymbols().stream()
                    .allMatch(cliCs -> cliCs.getData().getRenderingInfo().hasOneFeature(Feature.CRASH));

            cbm_crashAlways.setSelected(crashAlways);
            cbm_crashAlways.setEnabled(isEnabled());

            boolean crashNever = selection.getSelectedChordSymbols().stream()
                    .allMatch(cliCs -> cliCs.getData().getRenderingInfo().hasOneFeature(Feature.NO_CRASH));

            cbm_crashNever.setSelected(crashNever);
            cbm_crashNever.setEnabled(isEnabled());
        }

        @Override
        public JComponent[] getMenuPresenters()
        {
            return components;
        }

        @Override
        public JComponent[] synchMenuPresenters(JComponent[] items)
        {
            return getMenuPresenters();
        }

        private void setCrashAlways(boolean b)
        {
            CrashState s = CrashState.AUTO;
            if (b)
            {
                cbm_crashNever.setSelected(false);
                s  = CrashState.ALWAYS;
            }             
            changeSelectedChordSymbols(s);
        }

        private void setCrashNever(boolean b)
        {
            CrashState s = CrashState.AUTO;            
            if (b)
            {
                cbm_crashAlways.setSelected(false);
                s = CrashState.NEVER;
            }
            changeSelectedChordSymbols(s);
        }


    }

}
