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
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.rhythm.parameters.RhythmParameter;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.ui.ss_editor.api.RpCustomizeDialog;
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
import org.openide.windows.WindowManager;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;

@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.adjustrpvalues")
@ActionRegistration(displayName = "#CTL_AdjustRpValues", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 550),
        })
@Messages(
        {
            "CTL_AdjustRpValues=Adjust Values",
            "CTL_Flat=Same value",
            "CTL_UpDirect=Linear",
            "CTL_UpSlow=Slow",
            "CTL_UpFast=Fast",
            "CTL_DownDirect=Linear",
            "CTL_DownSlow=Slow",
            "CTL_DownFast=Fast",
            "CTL_Custom=Custom"
        }
)
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
    private static final Logger LOGGER = Logger.getLogger(AdjustRpValues.class.getSimpleName());

    public AdjustRpValues()
    {
        this(Utilities.actionsGlobalContext());
    }

    public AdjustRpValues(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        buildMenus();
        selectionChange(cap.getSelection());        // Make sure menu is correctly intialized at creation
        LOGGER.log(Level.FINE, " AdjustRpValues(context)");
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        LOGGER.log(Level.FINE, " createContextAwareInstance(context)");
        return new AdjustRpValues(context);
    }

    @Override
    public JMenuItem getPopupPresenter()
    {
        LOGGER.log(Level.FINE, " getPopupPresenter()");
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
        List<SongPartParameter> sptps = selection.getSelectedSongPartParameters();
        if (sptps.size() <= 1)
        {
            subMenu.setEnabled(false);
            LOGGER.log(Level.FINE, "selectionChange() => disabled");
        } else
        {
            // Choose the sub menu according to context
            SongPart spt0 = sptps.get(0).getSpt();
            RhythmParameter rp0 = sptps.get(0).getRp();
            double value0 = rp0.calculatePercentage(spt0.getRPValue(rp0));
            SongPart spt1 = sptps.get(sptps.size() - 1).getSpt();
            RhythmParameter rp1 = sptps.get(sptps.size() - 1).getRp();
            double value1 = rp1.calculatePercentage(spt1.getRPValue(rp1));
            subMenu.setEnabled(true);
            subMenu.removeAll();
            List<JMenuItem> items = (value0 > value1) ? goingDownItems : goingUpItems;
            for (JMenuItem item : items)
            {
                subMenu.add(item);
            }
            LOGGER.log(Level.FINE, "selectionChange() => enabled");
        }
    }

    //------------------------------------------------------------------------------
    // Private functions
    //------------------------------------------------------------------------------      
    /**
     * Set all values equal.
     */
    private void sameValue()
    {
        SS_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        SongPart refSpt = selection.getSelectedSongPartParameters().get(0).getSpt(); // Spt with lowest startBarIndex
        RhythmParameter refRp = selection.getSelectedSongPartParameters().get(0).getRp();
        double refFloatValue = refRp.calculatePercentage(refSpt.getRPValue(refRp));
        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(CTL_Flat());
        for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
        {
            RhythmParameter rp = sptp.getRp();
            Object value = rp.calculateValue(refFloatValue);
            sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
        }
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(CTL_Flat());
    }

    private void rampDirect(String undoText)
    {
        SS_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        SongPart spt0 = selection.getSelectedSongPartParameters().get(0).getSpt(); // Spt with lowest startBarIndex
        RhythmParameter rp0 = selection.getSelectedSongPartParameters().get(0).getRp();
        double doubleValue0 = rp0.calculatePercentage(spt0.getRPValue(rp0));

        int size = selection.getSelectedSongPartParameters().size();
        SongPart spt1 = selection.getSelectedSongPartParameters().get(size - 1).getSpt(); // Spt with highest startBarIndex
        RhythmParameter rp1 = selection.getSelectedSongPartParameters().get(size - 1).getRp();
        double doubleValue1 = rp1.calculatePercentage(spt1.getRPValue(rp1));

        double step = (doubleValue1 - doubleValue0) / (size - 1);

        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(undoText);
        double v = doubleValue0;
        for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
        {
            RhythmParameter rp = sptp.getRp();
            Object value = rp.calculateValue(enforceBounds(v));
            sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            v += step;
        }
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(undoText);
    }

    private void upSlow()
    {
        // 1-((1+ln((10-x)*10+0.37))/5.7)
        SS_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        SongPart spt0 = selection.getSelectedSongPartParameters().get(0).getSpt(); // Spt with lowest startBarIndex
        RhythmParameter rp0 = selection.getSelectedSongPartParameters().get(0).getRp();
        double doubleValue0 = rp0.calculatePercentage(spt0.getRPValue(rp0));

        int size = selection.getSelectedSongPartParameters().size();
        SongPart spt1 = selection.getSelectedSongPartParameters().get(size - 1).getSpt(); // Spt with highest startBarIndex
        RhythmParameter rp1 = selection.getSelectedSongPartParameters().get(size - 1).getRp();
        double doubleValue1 = rp1.calculatePercentage(spt1.getRPValue(rp1));

        double yDiff = doubleValue1 - doubleValue0;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(CTL_UpSlow());
        for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
        {
            double y = (1.0 - ((1 + Math.log((10 - x) * 10 + 0.37)) / 5.7)) * yDiff;
            LOGGER.log(Level.FINE, "upSlow() doubleValue0=" + doubleValue0 + " doubleValue1=" + doubleValue1 + " x=" + x + " y=" + y);
            RhythmParameter rp = sptp.getRp();
            double d = enforceBounds(doubleValue0 + y);
            Object value = sptp.getRp().calculateValue(d);
            sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            x += 10f / (size - 1f);
        }
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(CTL_UpSlow());
    }

    private void upFast()
    {
        // (1+ln(x*10+0.37))/5.7  = function x[0-10] y[0-1]
        SS_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        SongPart spt0 = selection.getSelectedSongPartParameters().get(0).getSpt(); // Spt with lowest startBarIndex
        RhythmParameter rp0 = selection.getSelectedSongPartParameters().get(0).getRp();
        double doubleValue0 = rp0.calculatePercentage(spt0.getRPValue(rp0));

        int size = selection.getSelectedSongPartParameters().size();
        SongPart spt1 = selection.getSelectedSongPartParameters().get(size - 1).getSpt(); // Spt with highest startBarIndex
        RhythmParameter rp1 = selection.getSelectedSongPartParameters().get(size - 1).getRp();
        double doubleValue1 = rp1.calculatePercentage(spt1.getRPValue(rp1));

        double yDiff = doubleValue1 - doubleValue0;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(CTL_UpFast());
        for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
        {
            double y = ((1 + Math.log(x * 10 + 0.37)) / 5.7) * yDiff;
            LOGGER.log(Level.FINER, "doubleValue0=" + doubleValue0 + " doubleValue1=" + doubleValue1 + " x=" + x + " y=" + y);
            RhythmParameter rp = sptp.getRp();
            double d = enforceBounds(doubleValue0 + y);
            Object value = rp.calculateValue(d);
            sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            x += 10f / (size - 1f);
        }
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(CTL_UpFast());
    }

    private void downSlow()
    {
        // ((1+ln((10-x)*10+0.37))/5.7)
        SS_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        SongPart spt0 = selection.getSelectedSongPartParameters().get(0).getSpt(); // Spt with lowest startBarIndex
        RhythmParameter rp0 = selection.getSelectedSongPartParameters().get(0).getRp();
        double doubleValue0 = rp0.calculatePercentage(spt0.getRPValue(rp0));

        int size = selection.getSelectedSongPartParameters().size();
        SongPart spt1 = selection.getSelectedSongPartParameters().get(size - 1).getSpt(); // Spt with highest startBarIndex
        RhythmParameter rp1 = selection.getSelectedSongPartParameters().get(size - 1).getRp();
        double doubleValue1 = rp1.calculatePercentage(spt1.getRPValue(rp1));

        double yDiff = doubleValue0 - doubleValue1;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(CTL_DownSlow());
        for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
        {
            double y = ((1 + Math.log((10 - x) * 10 + 0.37)) / 5.7) * yDiff;
            LOGGER.log(Level.FINER, "doubleValue0=" + doubleValue0 + " doubleValue1=" + doubleValue1 + " x=" + x + " y=" + y);
            RhythmParameter rp = sptp.getRp();
            double d = enforceBounds(doubleValue1 + y);
            Object value = rp.calculateValue(d);
            sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            x += 10f / (size - 1f);
        }
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(CTL_DownSlow());
    }

    private void downFast()
    {
        // (1+ln(x*10+0.37))/5.7  = function x[0-10] y[0-1]
        SS_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        SongPart spt0 = selection.getSelectedSongPartParameters().get(0).getSpt(); // Spt with lowest startBarIndex
        RhythmParameter rp0 = selection.getSelectedSongPartParameters().get(0).getRp();
        double doubleValue0 = rp0.calculatePercentage(spt0.getRPValue(rp0));

        int size = selection.getSelectedSongPartParameters().size();
        SongPart spt1 = selection.getSelectedSongPartParameters().get(size - 1).getSpt(); // Spt with highest startBarIndex
        RhythmParameter rp1 = selection.getSelectedSongPartParameters().get(size - 1).getRp();
        double doubleValue1 = rp1.calculatePercentage(spt1.getRPValue(rp1));

        double yDiff = doubleValue0 - doubleValue1;
        double x = 0;

        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(CTL_DownFast());
        for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
        {
            double y = ((1.0 + Math.log(x * 10 + 0.37)) / 5.7) * yDiff;
            LOGGER.log(Level.FINER, "doubleValue0=" + doubleValue0 + " doubleValue1=" + doubleValue1 + " x=" + x + " y=" + y);
            RhythmParameter rp = sptp.getRp();
            double d = enforceBounds(doubleValue0 - y);
            Object value = rp.calculateValue(d);
            sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
            x += 10f / (size - 1f);
        }
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(CTL_DownFast());
    }

    private void customize()
    {
        SS_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        List<SongPartParameter> sptps = selection.getSelectedSongPartParameters();
        RhythmParameter<?> rp0 = sptps.get(0).getRp();
        RpCustomizeDialog dlg = RpCustomizeDialog.getDefault();
        dlg.preset(rp0, sptps.size());
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
        List<Double> rpValues = dlg.getRpValues();
        if (rpValues != null && !rpValues.isEmpty())
        {
            assert rpValues.size() == sptps.size() : "rpValues=" + rpValues + " sptps=" + sptps;
            JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(CTL_Custom());
            int i = 0;
            for (SongPartParameter sptp : sptps)
            {
                RhythmParameter rp = sptp.getRp();
                Object value = rp.calculateValue(rpValues.get(i));
                sgs.setRhythmParameterValue(sptp.getSpt(), rp, value);
                i++;
            }
            JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(CTL_Custom());
        }
        dlg.cleanup();
    }

    private void buildMenus()
    {
        subMenu = new JMenu();
        subMenu.setText(CTL_AdjustRpValues());

        goingUpItems = new ArrayList<>();
        JMenuItem mi = new JMenuItem(CTL_Flat());
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                sameValue();
            }
        });
        java.net.URL imgURL = getClass().getResource("resources/RampFlat.png");
        ImageIcon icon = null;
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }

        goingUpItems.add(mi);
        mi = new JMenuItem(CTL_UpDirect());
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                rampDirect(CTL_UpDirect());
            }
        });
        imgURL = getClass().getResource("resources/RampDirectUp.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingUpItems.add(mi);
        mi = new JMenuItem(CTL_UpSlow());
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                upSlow();
            }
        });
        imgURL = getClass().getResource("resources/RampSlowUp.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingUpItems.add(mi);
        mi = new JMenuItem(CTL_UpFast());
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                upFast();
            }
        });
        imgURL = getClass().getResource("resources/RampFastUp.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingUpItems.add(mi);

        goingDownItems = new ArrayList<>();
        mi = new JMenuItem(CTL_Flat());
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                sameValue();
            }
        });
        imgURL = getClass().getResource("resources/RampFlat.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingDownItems.add(mi);
        mi = new JMenuItem(CTL_DownDirect());
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                rampDirect(CTL_DownDirect());
            }
        });
        imgURL = getClass().getResource("resources/RampDirectDown.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingDownItems.add(mi);
        mi = new JMenuItem(CTL_DownSlow());
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                downSlow();
            }
        });
        imgURL = getClass().getResource("resources/RampSlowDown.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingDownItems.add(mi);
        mi = new JMenuItem(CTL_DownFast());
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                downFast();
            }
        });
        imgURL = getClass().getResource("resources/RampFastDown.png");
        if (imgURL != null)
        {
            icon = new ImageIcon(imgURL);
            mi.setIcon(icon);
        }
        goingDownItems.add(mi);

        mi = new JMenuItem(CTL_Custom());
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                customize();
            }
        });
        // Not used for now, not sure it's really useful
        // goingDownItems.add(mi);
        // goingUpItems.add(mi);
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

}
