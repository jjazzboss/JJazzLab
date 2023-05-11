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
