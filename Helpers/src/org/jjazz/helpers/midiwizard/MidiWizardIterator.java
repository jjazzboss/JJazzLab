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
package org.jjazz.helpers.midiwizard;

import java.awt.Component;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;

public final class MidiWizardIterator implements WizardDescriptor.Iterator<WizardDescriptor>
{

    // Example of invoking this wizard:
    // @ActionID(category="...", id="...")
    // @ActionRegistration(displayName="...")
    // @ActionReference(path="Menu/...")
    // public static ActionListener run() {
    //     return new ActionListener() {
    //         @Override public void actionPerformed(ActionEvent e) {
    //             WizardDescriptor wiz = new WizardDescriptor(new Midi2WizardIterator());
    //             // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
    //             // {1} will be replaced by WizardDescriptor.Iterator.name()
    //             wiz.setTitleFormat(new MessageFormat("{0} ({1})"));
    //             wiz.setTitle("...dialog title...");
    //             if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
    //                 ...do something...
    //             }
    //         }
    //     };
    // }
    private int index;
    private WizardDescriptor wizardDesc;
    private WizardDescriptor.Panel<WizardDescriptor>[] currentPanels;
    private WizardDescriptor.Panel<WizardDescriptor>[] sfSequenceWin;
    private WizardDescriptor.Panel<WizardDescriptor>[] sfSequenceOther;
    private WizardDescriptor.Panel<WizardDescriptor>[] midiSequence;
    private Set<ChangeListener> listeners = new HashSet<>(2);
    private static final Logger LOGGER = Logger.getLogger(MidiWizardIterator.class.getSimpleName());

    /**
     * Must be called right after object creation.
     *
     * @param wizardDescriptor
     */
    public void setWizardDescriptor(WizardDescriptor wizardDescriptor)
    {
        wizardDesc = wizardDescriptor;

        // Initialize all the panels and sequences
        final MidiWizardPanelStart panelStart = new MidiWizardPanelStart();        // Special first common panel to decide which panel sequence to go
        final MidiWizardPanelFinal panelFinal = new MidiWizardPanelFinal();        // Last common panel
        panelStart.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                // Update the sequence
                setPanelSequence(!panelStart.getComponent().useSoundFont());
            }
        });

        sfSequenceWin = new WizardDescriptor.Panel[]
        {
            panelStart, new MidiWizardPanel_SfWin_1(), new MidiWizardPanel_SfWin_2(), panelFinal
        };
        setPanelStdProperties(sfSequenceWin);
        midiSequence = new WizardDescriptor.Panel[]
        {
            panelStart, new MidiWizardPanelSelectMidiOut(), new MidiWizardPanel4(), new MidiWizardPanel5(), panelFinal
        };
        setPanelStdProperties(midiSequence);
        index = 0;
        setPanelSequence(false);
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current()
    {
        return currentPanels[index];
    }

    @Override
    public String name()
    {
        return (index + 1) + " of " + currentPanels.length;
    }

    @Override
    public boolean hasNext()
    {
        return index < currentPanels.length - 1;
    }

    @Override
    public boolean hasPrevious()
    {
        return index > 0;
    }

    @Override
    public void nextPanel()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }
        index++;
        // The index of the step (or "Content Data") to be highlighted needs to be set explicitly
        wizardDesc.putProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, index);

    }

    @Override
    public void previousPanel()
    {
        if (!hasPrevious())
        {
            throw new NoSuchElementException();
        }
        index--;
        // The index of the step (or "Content Data") to be highlighted needs to be set explicitly.
        wizardDesc.putProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, index);
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

    // If something changes dynamically (besides moving between panels), e.g.
    // the number of panels changes in response to user input, then use
    // ChangeSupport to implement add/removeChangeListener and call fireChange
    // when needed
    protected final void fireChangeEvent()
    {
        ChangeEvent ev = new ChangeEvent(this);
        for (ChangeListener cl : listeners)
        {
            cl.stateChanged(ev);
        }
    }

    private void setPanelSequence(boolean useMidiSequence)
    {
        if (useMidiSequence)
        {
            currentPanels = midiSequence;
        } else
        {
            currentPanels = sfSequenceWin;
        }
        wizardDesc.putProperty(WizardDescriptor.PROP_CONTENT_DATA, getStepNames(currentPanels));
        fireChangeEvent();
    }

    private String[] getStepNames(WizardDescriptor.Panel<WizardDescriptor>[] panels)
    {
        String[] stepNames = new String[panels.length];
        for (int i = 0; i < panels.length; i++)
        {
            stepNames[i] = panels[i].getComponent().getName();
        }
        return stepNames;
    }

    private void setPanelStdProperties(WizardDescriptor.Panel<WizardDescriptor>[] panels)
    {
        for (WizardDescriptor.Panel<WizardDescriptor> panel : panels)
        {
            Component c = panel.getComponent();
            if (c instanceof JComponent)
            { // assume Swing components
                JComponent jc = (JComponent) c;
                // jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                // PROP_CONTENT_DATA: useless because panel index is not fixed, it will change depending on the sequence
                // jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps); 
                jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
            }
        }
    }

}
