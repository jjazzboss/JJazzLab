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
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.ui.cl_editor.api.CL_ContextActionListener;
import org.jjazz.ui.cl_editor.api.CL_ContextActionSupport;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.accentcrash")
@ActionRegistration(displayName = "#CTL_AccentCrash", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbolAccent", position = 200)
        })
public final class AccentCrash extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, Presenter.Popup, ClsChangeListener
{

    private CL_ContextActionSupport cap;
    private final Lookup context;
    private MyMenuItem menuItem;
    private ChordLeadSheet currentCls;
    private final String undoText = ResUtil.getString(getClass(), "CTL_AccentCrash");

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
        if (menuItem == null)
        {
            menuItem = new MyMenuItem();
        }
        menuItem.update();
        return menuItem;
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

    /**
     * DynamicMenuContent item which is used to return the 2 checkboxes
     */
    private class MyMenuItem extends JMenuItem implements DynamicMenuContent
    {

        private JComponent[] components = new JComponent[2];
        public JCheckBoxMenuItem cbm_crash;
        public JCheckBoxMenuItem cbm_noCrash;

        public MyMenuItem()
        {
            cbm_crash = new JCheckBoxMenuItem(ResUtil.getString(getClass(),"CrashAlways", new Object[] {}));
            cbm_crash.setAccelerator(KeyStroke.getKeyStroke('H'));
            cbm_crash.addItemListener(evt -> setCrash(evt.getStateChange() == ItemEvent.SELECTED));
            cbm_crash.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true);

            cbm_noCrash = new JCheckBoxMenuItem(ResUtil.getString(getClass(),"CrashNever", new Object[] {}));
            cbm_noCrash.setAccelerator(KeyStroke.getKeyStroke('H'));
            cbm_noCrash.addItemListener(evt -> setNoCrash(evt.getStateChange() == ItemEvent.SELECTED));
            cbm_noCrash.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true);

            components[0] = cbm_noCrash;
            components[1] = cbm_crash;
        }

        public void update()
        {
            CL_SelectionUtilities selection = cap.getSelection();

            boolean crashAlways = selection.getSelectedItems().stream()
                    .filter(item -> item instanceof CLI_ChordSymbol)
                    .allMatch(item -> ((CLI_ChordSymbol) item).getData().getRenderingInfo().hasOneFeature(Feature.CRASH));

            cbm_crash.setSelected(crashAlways);

            boolean crashNever = selection.getSelectedItems().stream()
                    .filter(item -> item instanceof CLI_ChordSymbol)
                    .allMatch(item -> ((CLI_ChordSymbol) item).getData().getRenderingInfo().hasOneFeature(Feature.NO_CRASH));

            cbm_noCrash.setSelected(crashNever);
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

        private void setCrash(boolean b)
        {
            if (b)
            {
                cbm_noCrash.setSelected(false);
            }
            changeSelectedChordSymbols((b ? 1 : 0) + (cbm_noCrash.isSelected() ? 2 : 0));
        }

        private void setNoCrash(boolean b)
        {
            if (b)
            {
                cbm_crash.setSelected(false);
            }
            changeSelectedChordSymbols((cbm_crash.isSelected() ? 1 : 0) + (b ? 2 : 0));
        }

        /**
         *
         * @param state 0:auto, 1:crash, 2:nocrash
         */
        private void changeSelectedChordSymbols(int state)
        {
            CL_SelectionUtilities selection = cap.getSelection();
            ChordLeadSheet cls = selection.getChordLeadSheet();

            JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);

            for (CLI_ChordSymbol item : selection.getSelectedChordSymbols())
            {
                ExtChordSymbol ecs = item.getData();
                ChordRenderingInfo cri = ecs.getRenderingInfo();
                var features = cri.getFeatures();

                if (state == 0 && cri.hasOneFeature(Feature.CRASH, Feature.NO_CRASH))
                {
                    features.remove(Feature.CRASH);
                    features.remove(Feature.NO_CRASH);
                    ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());
                    ExtChordSymbol newCs = new ExtChordSymbol(ecs, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
                    item.getContainer().changeItem(item, newCs);

                } else if (state == 1 && !features.contains(Feature.CRASH))
                {
                    features.remove(Feature.NO_CRASH);
                    features.add(Feature.CRASH);
                    ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());
                    ExtChordSymbol newCs = new ExtChordSymbol(ecs, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
                    item.getContainer().changeItem(item, newCs);

                } else if (state == 2 && !features.contains(Feature.NO_CRASH))
                {
                    features.remove(Feature.CRASH);
                    features.add(Feature.NO_CRASH);
                    ChordRenderingInfo newCri = new ChordRenderingInfo(features, cri.getScaleInstance());
                    ExtChordSymbol newCs = new ExtChordSymbol(ecs, newCri, ecs.getAlternateChordSymbol(), ecs.getAlternateFilter());
                    item.getContainer().changeItem(item, newCs);

                }
            }

            JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
        }

    }

}
