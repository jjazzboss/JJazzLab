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
package org.jjazz.yjzwizard;
import java.util.HashSet;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public class YjzCreatorWizardPanel2 implements WizardDescriptor.Panel<WizardDescriptor>, ListSelectionListener
{

    /**
     * The visual component that displays this panel. If you need to access the component from this class, just use
     * getComponent().
     */
    private YjzCreatorVisualPanel2 component;
    private Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    @Override
    public YjzCreatorVisualPanel2 getComponent()
    {
        if (component == null)
        {
            component = new YjzCreatorVisualPanel2();
            component.getRhythmTable().getSelectionModel().addListSelectionListener(this);
        }
        return component;
    }

    @Override
    public HelpCtx getHelp()
    {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
        // If you have context help:
        // return new HelpCtx("help.key.here");
    }

    @Override
    public boolean isValid()
    {
        RhythmInfo ri = component.getRhythmTable().getSelectedRhythm();
        return ri != null && !ri.isAdaptedRhythm() && ri.file() != null && ri.rhythmProviderId().equals("YamahaRhythmProviderID");
    }

    @Override
    public final void addChangeListener(ChangeListener l)
    {
        synchronized (listeners)
        {
            listeners.add(l);
        }
    }

    @Override
    public final void removeChangeListener(ChangeListener l)
    {
        synchronized (listeners)
        {
            listeners.remove(l);
        }
    }

    protected final void fireChangeEvent()
    {
        ChangeEvent ev = new ChangeEvent(this);
        for (ChangeListener cl : listeners)
        {
            cl.stateChanged(ev);
        }
    }

    @Override
    public void readSettings(WizardDescriptor wiz)
    {
        // use wiz.getProperty to retrieve previous panel state
        RhythmInfo ri = (RhythmInfo) wiz.getProperty(YjzCreatorWizardAction.PROP_BASE_RHYTHM);
        if (ri != null)
        {
            component.getRhythmTable().setSelectedRhythm(ri);
        }
    }

    @Override
    public void storeSettings(WizardDescriptor wiz)
    {
        RhythmInfo ri = component.getRhythmTable().getSelectedRhythm();
        wiz.putProperty(YjzCreatorWizardAction.PROP_BASE_RHYTHM, ri);
    }

    // -----------------------------------------------------------------------------------
    // ListSelectionListener implementation
    // -----------------------------------------------------------------------------------    
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            fireChangeEvent();
        }
    }

}
