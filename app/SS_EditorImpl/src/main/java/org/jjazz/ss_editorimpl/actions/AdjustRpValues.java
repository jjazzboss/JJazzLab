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
package org.jjazz.ss_editorimpl.actions;

import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.Presenter;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.rhythm.api.RpEnumerable;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;


/**
 * Show a submenu with menu items to interpolate values on rhythm parameters between 2 left and right rhythm parameters.
 * <p>
 * Simplified implementation because action is only used in a popup menu, with no shortcut key.
 */
@SuppressWarnings(
        {
            "rawtypes", "unchecked"
        })
@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.adjustrpvalues")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 550),
        })
public class AdjustRpValues extends AbstractAction implements Presenter.Popup, ContextAwareAction
{

    private JMenu subMenu;
    private RpSelectionContext rpSelectionContext;


    private static final Logger LOGGER = Logger.getLogger(AdjustRpValues.class.getSimpleName());

    public AdjustRpValues()
    {
        // Not used besides for creating the ContextAwareAction
    }

    public AdjustRpValues(Lookup context)
    {
        Objects.requireNonNull(context);
        subMenu = new JMenu();
        subMenu.setText(ResUtil.getString(getClass(), "CTL_AdjustRpValues"));


        var selection = new SS_Selection(context);
        rpSelectionContext = new RpSelectionContext();
        boolean b = rpSelectionContext.init(selection);     // Compute context data
        setEnabled(b);
        subMenu.setEnabled(b);
        if (!b)
        {
            return;
        }

        // Add the menu items
        List<JMenuItem> items = (rpSelectionContext.doubleValue0 > rpSelectionContext.doubleValue1) ? getGoingDownItems() : getGoingUpItems();
        for (JMenuItem item : items)
        {
            subMenu.add(item);
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return new AdjustRpValues(lkp);
    }

    @Override
    public JMenuItem getPopupPresenter()
    {
        return subMenu;
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        // NOT USED
    }


//------------------------------------------------------------------------------
// Private functions
//------------------------------------------------------------------------------      
    /**
     * Set all values equal.
     */
    private void sameValue()
    {
        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).startCEdit(ResUtil.getString(getClass(), "CTL_Flat"));

        for (SongPartParameter sptp : rpSelectionContext.enumerableSptps)
        {
            RhythmParameter rp = sptp.getRp();
            Object value = ((RpEnumerable<?>) rp).calculateValue(rpSelectionContext.doubleValue0);
            rpSelectionContext.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
        }

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).endCEdit(ResUtil.getString(getClass(), "CTL_Flat"));
    }

    private void rampDirect(String undoText)
    {

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).startCEdit(undoText);

        double step = (rpSelectionContext.doubleValue1 - rpSelectionContext.doubleValue0) / (rpSelectionContext.enumerableSptps.size() - 1);
        double v = rpSelectionContext.doubleValue0;

        for (SongPartParameter sptp : rpSelectionContext.enumerableSptps)
        {
            RhythmParameter rp = sptp.getRp();
            Object value = ((RpEnumerable) rp).calculateValue(enforce0_1Range(v));
            rpSelectionContext.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            v += step;
        }

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).endCEdit(undoText);
    }

    private void upSlow()
    {
        // 1-((1+ln((10-x)*10+0.37))/5.7)

        double yDiff = rpSelectionContext.doubleValue1 - rpSelectionContext.doubleValue0;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).startCEdit(ResUtil.getString(getClass(), "CTL_UpSlow"));

        for (SongPartParameter sptp : rpSelectionContext.enumerableSptps)
        {
            RhythmParameter rp = sptp.getRp();
            double y = (1.0 - ((1 + Math.log((10 - x) * 10 + 0.37)) / 5.7)) * yDiff;
            LOGGER.log(Level.FINE, "upSlow() rpData.doubleValue0={0} rpData.doubleValue1={1} x={2} y={3}", new Object[]
            {
                rpSelectionContext.doubleValue0,
                rpSelectionContext.doubleValue1, x, y
            });
            double d = enforce0_1Range(rpSelectionContext.doubleValue0 + y);
            Object value = ((RpEnumerable) rp).calculateValue(d);
            rpSelectionContext.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            x += 10f / (rpSelectionContext.enumerableSptps.size() - 1f);
        }

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).endCEdit(ResUtil.getString(getClass(), "CTL_UpSlow"));
    }

    private void upFast()
    {
        // (1+ln(x*10+0.37))/5.7  = function x[0-10] y[0-1]

        double yDiff = rpSelectionContext.doubleValue1 - rpSelectionContext.doubleValue0;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).startCEdit(ResUtil.getString(getClass(), "CTL_UpFast"));

        for (SongPartParameter sptp : rpSelectionContext.enumerableSptps)
        {
            double y = ((1 + Math.log(x * 10 + 0.37)) / 5.7) * yDiff;
            LOGGER.log(Level.FINER, "rpData.doubleValue0={0} rpData.doubleValue1={1} x={2} y={3}", new Object[]
            {
                rpSelectionContext.doubleValue0,
                rpSelectionContext.doubleValue1, x, y
            });
            RhythmParameter rp = sptp.getRp();
            double d = enforce0_1Range(rpSelectionContext.doubleValue0 + y);
            Object value = ((RpEnumerable) rp).calculateValue(d);
            rpSelectionContext.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            x += 10f / (rpSelectionContext.enumerableSptps.size() - 1f);
        }

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).endCEdit(ResUtil.getString(getClass(), "CTL_UpFast"));
    }

    private void downSlow()
    {
        // ((1+ln((10-x)*10+0.37))/5.7)        
        double yDiff = rpSelectionContext.doubleValue0 - rpSelectionContext.doubleValue1;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).startCEdit(ResUtil.getString(getClass(), "CTL_DownSlow"));

        for (SongPartParameter sptp : rpSelectionContext.enumerableSptps)
        {
            double y = ((1 + Math.log((10 - x) * 10 + 0.37)) / 5.7) * yDiff;
            LOGGER.log(Level.FINER, "rpData.doubleValue0={0} rpData.doubleValue1={1} x={2} y={3}", new Object[]
            {
                rpSelectionContext.doubleValue0,
                rpSelectionContext.doubleValue1, x, y
            });
            RhythmParameter rp = sptp.getRp();

            double d = enforce0_1Range(rpSelectionContext.doubleValue1 + y);
            Object value = ((RpEnumerable) rp).calculateValue(d);
            rpSelectionContext.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);

            x += 10f / (rpSelectionContext.enumerableSptps.size() - 1f);
        }

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).endCEdit(ResUtil.getString(getClass(), "CTL_DownSlow"));
    }

    private void downFast()
    {
        // (1+ln(x*10+0.37))/5.7  = function x[0-10] y[0-1]

        double yDiff = rpSelectionContext.doubleValue0 - rpSelectionContext.doubleValue1;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).startCEdit(ResUtil.getString(getClass(), "CTL_DownFast"));

        for (SongPartParameter sptp : rpSelectionContext.enumerableSptps)
        {
            double y = ((1.0 + Math.log(x * 10 + 0.37)) / 5.7) * yDiff;
            LOGGER.log(Level.FINER, "rpData.doubleValue0={0} rpData.doubleValue1={1} x={2} y={3}", new Object[]
            {
                rpSelectionContext.doubleValue0,
                rpSelectionContext.doubleValue1, x, y
            });
            RhythmParameter rp = sptp.getRp();
            double d = enforce0_1Range(rpSelectionContext.doubleValue0 - y);
            Object value = ((RpEnumerable) rp).calculateValue(d);
            rpSelectionContext.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            x += 10f / (rpSelectionContext.enumerableSptps.size() - 1f);
        }

        JJazzUndoManagerFinder.getDefault().get(rpSelectionContext.sgs).endCEdit(ResUtil.getString(getClass(), "CTL_DownFast"));
    }

    private double enforce0_1Range(double d)
    {
        return Math.clamp(d, 0, 1);
    }

    private List<JMenuItem> getGoingUpItems()
    {
        List<JMenuItem> menuItems = new ArrayList<>();
        JMenuItem mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_Flat"));
        mi.addActionListener(e -> sameValue());
        setMenuIcon(getClass().getResource("resources/RampFlat.png"), mi);
        menuItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_UpDirect"));
        mi.addActionListener(e -> rampDirect(ResUtil.getString(getClass(), "CTL_UpDirect")));
        setMenuIcon(getClass().getResource("resources/RampDirectUp.png"), mi);
        menuItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_UpSlow"));
        mi.addActionListener(e -> upSlow());
        setMenuIcon(getClass().getResource("resources/RampSlowUp.png"), mi);
        menuItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_UpFast"));
        mi.addActionListener(e -> upFast());
        setMenuIcon(getClass().getResource("resources/RampFastUp.png"), mi);
        menuItems.add(mi);

        return menuItems;
    }

    private List<JMenuItem> getGoingDownItems()
    {
        List<JMenuItem> menuItems = new ArrayList<>();
        var mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_Flat"));
        mi.addActionListener(e -> sameValue());
        setMenuIcon(getClass().getResource("resources/RampFlat.png"), mi);
        menuItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_DownDirect"));
        mi.addActionListener(e -> rampDirect(ResUtil.getString(getClass(), "CTL_DownDirect")));
        setMenuIcon(getClass().getResource("resources/RampDirectDown.png"), mi);
        menuItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_DownSlow"));
        mi.addActionListener(e -> downSlow());
        setMenuIcon(getClass().getResource("resources/RampSlowDown.png"), mi);
        menuItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_DownFast"));
        mi.addActionListener(e -> downFast());
        setMenuIcon(getClass().getResource("resources/RampFastDown.png"), mi);
        menuItems.add(mi);

        return menuItems;
    }

    private void setMenuIcon(URL imgURL, JMenuItem mi)
    {
        if (imgURL != null)
        {
            var icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
    }

    /**
     * Compute context data used.
     * <p>
     * Used to choose which menu items (up or down), and to help perform the menu items actions.
     */
    private class RpSelectionContext
    {

        SongPart spt0, spt1;    // First and last select SongParts
        RhythmParameter rp0, rp1;
        List<SongPartParameter> enumerableSptps;    // Only the enumerable ones
        SongStructure sgs;
        double doubleValue0, doubleValue1;  // First and last value as a percentage [0;1]        

        /**
         * Initialize the fields.
         * <p>
         * @param selection
         * @return True if selection is valid for our actions.
         */
        public boolean init(SS_Selection selection)
        {
            boolean b = false;

            var sptps = selection.getSelectedSongPartParameters();
            if (sptps.size() > 2)
            {
                sgs = selection.getModel();
                spt0 = sptps.get(0).getSpt();
                rp0 = sptps.get(0).getRp();
                spt1 = sptps.get(sptps.size() - 1).getSpt();
                rp1 = sptps.get(sptps.size() - 1).getRp();
                if (rp0 instanceof RpEnumerable && rp1 instanceof RpEnumerable)
                {
                    doubleValue0 = ((RpEnumerable) rp0).calculatePercentage(spt0.getRPValue(rp0));
                    doubleValue1 = ((RpEnumerable) rp1).calculatePercentage(spt1.getRPValue(rp1));
                    enumerableSptps = sptps.stream().filter(sptp -> sptp.isEnumerableRp()).toList();
                    b = enumerableSptps.size() > 2;
                }
            }

            return b;

        }
    }
}
