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
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;


public final class YjzCreatorWizardIterator implements WizardDescriptor.InstantiatingIterator<WizardDescriptor>
{

    private int index;

    private WizardDescriptor wizard;
    private List<WizardDescriptor.Panel<WizardDescriptor>> panels;

    private List<WizardDescriptor.Panel<WizardDescriptor>> getPanels()
    {
        if (panels == null)
        {
            panels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
            panels.add(new YjzCreatorWizardPanel1());
            panels.add(new YjzCreatorWizardPanel2());
            panels.add(new YjzCreatorWizardPanel3());
            panels.add(new YjzCreatorWizardPanel4());
            String[] steps = getStepNames(panels);
            for (int i = 0; i < panels.size(); i++)
            {
                Component c = panels.get(i).getComponent();
                if (steps[i] == null)
                {
                    // Default step name to component name of panel. Mainly
                    // useful for getting the name of the target chooser to
                    // appear in the list of steps.
                    steps[i] = c.getName();
                }
                if (c instanceof JComponent)
                { // assume Swing components
                    JComponent jc = (JComponent) c;
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                    jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
                }
            }
        }
        return panels;
    }

    @Override
    public Set<?> instantiate() throws IOException
    {
        // TODO return set of FileObject (or DataObject) you have created
        return Collections.emptySet();
    }

    @Override
    public void initialize(WizardDescriptor wizard)
    {
        this.wizard = wizard;
    }

    @Override
    public void uninitialize(WizardDescriptor wizard)
    {
        panels = null;
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current()
    {
        return getPanels().get(index);
    }

    @Override
    public String name()
    {
        return index + 1 + ". from " + getPanels().size();
    }

    @Override
    public boolean hasNext()
    {
        return index < getPanels().size() - 1;
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
    }

    @Override
    public void previousPanel()
    {
        if (!hasPrevious())
        {
            throw new NoSuchElementException();
        }
        index--;
    }

    // If nothing unusual changes in the middle of the wizard, simply:
    @Override
    public void addChangeListener(ChangeListener l)
    {
    }

    @Override
    public void removeChangeListener(ChangeListener l)
    {
    }
    // If something changes dynamically (besides moving between panels), e.g.
    // the number of panels changes in response to user input, then use
    // ChangeSupport to implement add/removeChangeListener and call fireChange
    // when needed

    private String[] getStepNames(List<WizardDescriptor.Panel<WizardDescriptor>> panels)
    {
        List<String> stepNames = panels.stream().map(p -> p.getComponent().getName()).toList();
        return stepNames.toArray(String[]::new);
    }

//    // You could safely ignore this method. Is is here to keep steps which were
//    // there before this wizard was instantiated. It should be better handled
//    // by NetBeans Wizard API itself rather than needed to be implemented by a
//    // client code.
//    private String[] createSteps()
//    {
//        String[] beforeSteps = (String[]) wizard.getProperty("WizardPanel_contentData");
//        assert beforeSteps != null : "This wizard may only be used embedded in the template wizard";
//        String[] res = new String[(beforeSteps.length - 1) + panels.size()];
//        for (int i = 0; i < res.length; i++)
//        {
//            if (i < (beforeSteps.length - 1))
//            {
//                res[i] = beforeSteps[i];
//            } else
//            {
//                res[i] = panels.get(i - beforeSteps.length + 1).getComponent().getName();
//            }
//        }
//        return res;
//    }

}
