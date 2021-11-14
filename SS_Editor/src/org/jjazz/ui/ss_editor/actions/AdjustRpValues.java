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
package org.jjazz.ui.ss_editor.actions;

import org.jjazz.ui.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.ui.ss_editor.api.RpCustomizeDialog;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;
import org.openide.windows.WindowManager;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;
import org.jjazz.util.api.ResUtil;
import org.jjazz.rhythm.api.RpEnumerable;

@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.adjustrpvalues")
@ActionRegistration(displayName = "#CTL_AdjustRpValues", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 550),
        })

@SuppressWarnings(
        {
            "rawtypes", "unchecked"
        })
public class AdjustRpValues extends AbstractAction implements ContextAwareAction, SS_ContextActionListener, Presenter.Popup
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    static private List<JMenuItem> goingUpItems;
    static private List<JMenuItem> goingDownItems;
    private JMenu subMenu;
    private RpSelectionContext rpData;

    private static final Logger LOGGER = Logger.getLogger(AdjustRpValues.class.getSimpleName());

    public AdjustRpValues()
    {
        this(Utilities.actionsGlobalContext());
    }

    public AdjustRpValues(Lookup context)
    {
        this.context = context;
        rpData = new RpSelectionContext();
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        buildMenus();
        selectionChange(cap.getSelection());        // Make sure menu is correctly intialized at creation
        LOGGER.log(Level.FINE, " AdjustRpValues(context)");   //NOI18N
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        LOGGER.log(Level.FINE, " createContextAwareInstance(context)");   //NOI18N
        return new AdjustRpValues(context);
    }

    @Override
    public JMenuItem getPopupPresenter()
    {
        LOGGER.log(Level.FINE, " getPopupPresenter()");   //NOI18N
        return subMenu;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // NOT USED
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        boolean b = false;

        if (rpData.init(selection))
        {
            subMenu.removeAll();
            List<JMenuItem> items = (rpData.doubleValue0 > rpData.doubleValue1) ? goingDownItems : goingUpItems;
            for (JMenuItem item : items)
            {
                subMenu.add(item);
            }
            b = true;
        }

        LOGGER.log(Level.FINE, "selectionChange() b=" + b);   //NOI18N        
        subMenu.setEnabled(b);
    }

//------------------------------------------------------------------------------
// Private functions
//------------------------------------------------------------------------------      
    /**
     * Set all values equal.
     */
    private void sameValue()
    {
        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).startCEdit(ResUtil.getString(getClass(), "CTL_Flat"));

        for (SongPartParameter sptp : rpData.enumerableSptps)
        {
            RhythmParameter rp = sptp.getRp();
            Object value = ((RpEnumerable<?>) rp).calculateValue(rpData.doubleValue0);
            rpData.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
        }

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).endCEdit(ResUtil.getString(getClass(), "CTL_Flat"));
    }

    private void rampDirect(String undoText)
    {

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).startCEdit(undoText);

        double step = (rpData.doubleValue1 - rpData.doubleValue0) / (rpData.enumerableSptps.size() - 1);
        double v = rpData.doubleValue0;

        for (SongPartParameter sptp : rpData.enumerableSptps)
        {
            RhythmParameter rp = sptp.getRp();
            Object value = ((RpEnumerable) rp).calculateValue(enforceBounds(v));
            rpData.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            v += step;
        }

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).endCEdit(undoText);
    }

    private void upSlow()
    {
        // 1-((1+ln((10-x)*10+0.37))/5.7)

        double yDiff = rpData.doubleValue1 - rpData.doubleValue0;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).startCEdit(ResUtil.getString(getClass(), "CTL_UpSlow"));

        for (SongPartParameter sptp : rpData.enumerableSptps)
        {
            RhythmParameter rp = sptp.getRp();
            double y = (1.0 - ((1 + Math.log((10 - x) * 10 + 0.37)) / 5.7)) * yDiff;
            LOGGER.log(Level.FINE, "upSlow() rpData.doubleValue0=" + rpData.doubleValue0 + " rpData.doubleValue1=" + rpData.doubleValue1 + " x=" + x + " y=" + y);   //NOI18N
            double d = enforceBounds(rpData.doubleValue0 + y);
            Object value = ((RpEnumerable) rp).calculateValue(d);
            rpData.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            x += 10f / (rpData.enumerableSptps.size() - 1f);
        }

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).endCEdit(ResUtil.getString(getClass(), "CTL_UpSlow"));
    }

    private void upFast()
    {
        // (1+ln(x*10+0.37))/5.7  = function x[0-10] y[0-1]

        double yDiff = rpData.doubleValue1 - rpData.doubleValue0;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).startCEdit(ResUtil.getString(getClass(), "CTL_UpFast"));

        for (SongPartParameter sptp : rpData.enumerableSptps)
        {
            double y = ((1 + Math.log(x * 10 + 0.37)) / 5.7) * yDiff;
            LOGGER.log(Level.FINER, "rpData.doubleValue0=" + rpData.doubleValue0 + " rpData.doubleValue1=" + rpData.doubleValue1 + " x=" + x + " y=" + y);   //NOI18N
            RhythmParameter rp = sptp.getRp();
            double d = enforceBounds(rpData.doubleValue0 + y);
            Object value = ((RpEnumerable) rp).calculateValue(d);
            rpData.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            x += 10f / (rpData.enumerableSptps.size() - 1f);
        }

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).endCEdit(ResUtil.getString(getClass(), "CTL_UpFast"));
    }

    private void downSlow()
    {
        // ((1+ln((10-x)*10+0.37))/5.7)        
        double yDiff = rpData.doubleValue0 - rpData.doubleValue1;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).startCEdit(ResUtil.getString(getClass(), "CTL_DownSlow"));

        for (SongPartParameter sptp : rpData.enumerableSptps)
        {
            double y = ((1 + Math.log((10 - x) * 10 + 0.37)) / 5.7) * yDiff;
            LOGGER.log(Level.FINER, "rpData.doubleValue0=" + rpData.doubleValue0 + " rpData.doubleValue1=" + rpData.doubleValue1 + " x=" + x + " y=" + y);   //NOI18N
            RhythmParameter rp = sptp.getRp();

            double d = enforceBounds(rpData.doubleValue1 + y);
            Object value = ((RpEnumerable) rp).calculateValue(d);
            rpData.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);

            x += 10f / (rpData.enumerableSptps.size() - 1f);
        }

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).endCEdit(ResUtil.getString(getClass(), "CTL_DownSlow"));
    }

    private void downFast()
    {
        // (1+ln(x*10+0.37))/5.7  = function x[0-10] y[0-1]

        double yDiff = rpData.doubleValue0 - rpData.doubleValue1;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).startCEdit(ResUtil.getString(getClass(), "CTL_DownFast"));

        for (SongPartParameter sptp : rpData.enumerableSptps)
        {
            double y = ((1.0 + Math.log(x * 10 + 0.37)) / 5.7) * yDiff;
            LOGGER.log(Level.FINER, "rpData.doubleValue0=" + rpData.doubleValue0 + " rpData.doubleValue1=" + rpData.doubleValue1 + " x=" + x + " y=" + y);   //NOI18N
            RhythmParameter rp = sptp.getRp();
            double d = enforceBounds(rpData.doubleValue0 - y);
            Object value = ((RpEnumerable) rp).calculateValue(d);
            rpData.sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            x += 10f / (rpData.enumerableSptps.size() - 1f);
        }

        JJazzUndoManagerFinder.getDefault().get(rpData.sgs).endCEdit(ResUtil.getString(getClass(), "CTL_DownFast"));
    }

    private void buildMenus()
    {
        subMenu = new JMenu();
        subMenu.setText(ResUtil.getString(getClass(), "CTL_AdjustRpValues"));

        goingUpItems = new ArrayList<>();
        JMenuItem mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_Flat"));
        mi.addActionListener((ActionEvent e) ->
        {
            sameValue();
        });
        java.net.URL imgURL = getClass().getResource("resources/RampFlat.png");
        ImageIcon icon = null;
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }

        goingUpItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_UpDirect"));
        mi.addActionListener((ActionEvent e) ->
        {
            rampDirect(ResUtil.getString(getClass(), "CTL_UpDirect"));
        });
        imgURL = getClass().getResource("resources/RampDirectUp.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingUpItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_UpSlow"));
        mi.addActionListener((ActionEvent e) ->
        {
            upSlow();
        });
        imgURL = getClass().getResource("resources/RampSlowUp.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingUpItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_UpFast"));
        mi.addActionListener((ActionEvent e) ->
        {
            upFast();
        });
        imgURL = getClass().getResource("resources/RampFastUp.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingUpItems.add(mi);

        goingDownItems = new ArrayList<>();
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_Flat"));
        mi.addActionListener((ActionEvent e) -> sameValue());
        imgURL = getClass().getResource("resources/RampFlat.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingDownItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_DownDirect"));
        mi.addActionListener((ActionEvent e) ->
        {
            rampDirect(ResUtil.getString(getClass(), "CTL_DownDirect"));
        });
        imgURL = getClass().getResource("resources/RampDirectDown.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingDownItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_DownSlow"));
        mi.addActionListener((ActionEvent e) ->
        {
            downSlow();
        });
        imgURL = getClass().getResource("resources/RampSlowDown.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingDownItems.add(mi);
        mi = new JMenuItem(ResUtil.getString(getClass(), "CTL_DownFast"));
        mi.addActionListener((ActionEvent e) ->
        {
            downFast();
        });
        imgURL = getClass().getResource("resources/RampFastDown.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingDownItems.add(mi);

    }

    private double enforceBounds(double d)
    {
        if (d < 0)
        {
            d = 0;
        } else if (d > 1)
        {
            d = 1;
        }
        return d;


    }

    /**
     * Store precomputed data on each selection change, to be used by menu actions.
     */
    private class RpSelectionContext
    {

        SongPart spt0, spt1;    // First and last select SongParts
        RhythmParameter rp0, rp1;
        double doubleValue0, doubleValue1;  // First and last value as a percentage [0;1]
        List<SongPartParameter> enumerableSptps;    // Only the enumerable ones
        SongStructure sgs;

        /**
         * Initialize the fields.
         * <p>
         * Fields will be null if selection is not valid for our actions.
         *
         * @param selection
         * @return True if selection was valid for our actions.
         */
        public boolean init(SS_SelectionUtilities selection)
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
                    enumerableSptps = sptps.stream().filter(sptp -> sptp.isEnumerableRp()).collect(Collectors.toList());
                    b = enumerableSptps.size() > 2;
                }
            }

            if (!b)
            {
                // Make sure code can not use the unitialized fields
                spt0 = spt1 = null;
                rp0 = rp1 = null;
                doubleValue0 = doubleValue1 = -1;
                enumerableSptps = null;
                sgs = null;
            }

            return b;

        }
    }
}
