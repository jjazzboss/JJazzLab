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
import javax.swing.event.ChangeListener;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public class YjzCreatorWizardPanel3 implements WizardDescriptor.Panel<WizardDescriptor>
{

    /**
     * The visual component that displays this panel. If you need to access the component from this class, just use
     * getComponent().
     */
    private YjzCreatorVisualPanel3 component;

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    @Override
    public YjzCreatorVisualPanel3 getComponent()
    {
        if (component == null)
        {
            component = new YjzCreatorVisualPanel3();
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
        return true;
    }

    @Override
    public final void addChangeListener(ChangeListener l)
    {

    }

    @Override
    public final void removeChangeListener(ChangeListener l)
    {

    }

    @Override
    public void readSettings(WizardDescriptor wiz)
    {
        // use wiz.getProperty to retrieve previous panel state
        int nbMainA = (Integer) wiz.getProperty(YjzCreatorWizardAction.PROP_NB_MAIN_A);
        int nbMainB = (Integer) wiz.getProperty(YjzCreatorWizardAction.PROP_NB_MAIN_B);
        int nbMainC = (Integer) wiz.getProperty(YjzCreatorWizardAction.PROP_NB_MAIN_C);
        int nbMainD = (Integer) wiz.getProperty(YjzCreatorWizardAction.PROP_NB_MAIN_D);
        component.setNbMain(nbMainA, nbMainB, nbMainC, nbMainD);
        int nbSrcPhrases = (Integer) wiz.getProperty(YjzCreatorWizardAction.PROP_NB_SRC_PHRASES);
        component.setNbSrcPhrases(nbSrcPhrases);
        RhythmInfo riBase = (RhythmInfo) wiz.getProperty(YjzCreatorWizardAction.PROP_BASE_RHYTHM);
        component.setExtendedFilename(YjzCreatorWizardAction.getExtendedFile(riBase).getName());
        boolean b = (Boolean) wiz.getProperty(YjzCreatorWizardAction.PROP_INCLUDE_INTRO_ENDINGS);
        component.setIncludeIntroEndings(b);
        b = (Boolean) wiz.getProperty(YjzCreatorWizardAction.PROP_INCLUDE_FILLS);
        component.setIncludeFills(b);

    }

    @Override
    public void storeSettings(WizardDescriptor wiz)
    {
        wiz.putProperty(YjzCreatorWizardAction.PROP_NB_MAIN_A, component.getNbMain('A'));
        wiz.putProperty(YjzCreatorWizardAction.PROP_NB_MAIN_B, component.getNbMain('B'));
        wiz.putProperty(YjzCreatorWizardAction.PROP_NB_MAIN_C, component.getNbMain('C'));
        wiz.putProperty(YjzCreatorWizardAction.PROP_NB_MAIN_D, component.getNbMain('D'));
        wiz.putProperty(YjzCreatorWizardAction.PROP_NB_SRC_PHRASES, component.getNbSrcPhrases());
        wiz.putProperty(YjzCreatorWizardAction.PROP_INCLUDE_INTRO_ENDINGS, component.isIncludeIntroEndings());
        wiz.putProperty(YjzCreatorWizardAction.PROP_INCLUDE_FILLS, component.isIncludeFills());
    }

}
