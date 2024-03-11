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
package org.jjazz.fluidsynthembeddedsynth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.embeddedsynth.spi.EmbeddedSynthProvider;
import org.jjazz.fluidsynthjava.api.FluidEffectsDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;


//@ActionID(category = "JJazz", id = "org.showfluidsyntheffectsdialog")
//@ActionRegistration(displayName = "Show FluidSynth effects dialog", lazy = true)
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 2110)
//        })
public class ShowEffectDialog implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(ShowEffectDialog.class.getSimpleName());

    public ShowEffectDialog()
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
