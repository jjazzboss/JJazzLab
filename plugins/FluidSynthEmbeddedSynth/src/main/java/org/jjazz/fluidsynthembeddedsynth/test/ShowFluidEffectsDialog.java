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
package org.jjazz.fluidsynthembeddedsynth.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.embeddedsynth.spi.EmbeddedSynthProvider;
import org.jjazz.fluidsynthembeddedsynth.api.FluidSynthEmbeddedSynth;

/**
 * Action used for test.
 */

//@ActionID(category = "JJazz", id = "org.showfluidsyntheffectsdialog")
//@ActionRegistration(displayName = "Show FluidSynth effects dialog", lazy = true)
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 2110)
//        })
public class ShowFluidEffectsDialog implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(ShowFluidEffectsDialog.class.getSimpleName());

    public ShowFluidEffectsDialog()
    {
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var es = EmbeddedSynthProvider.getDefaultSynth();
        if (es == null)
        {
            return;
        }
        FluidSynthEmbeddedSynth fluidSynth = (FluidSynthEmbeddedSynth) es;
        var dlg = new FluidEffectsDialog(false, fluidSynth.getFluidSynthJava());
        dlg.setVisible(true);
    }
}
