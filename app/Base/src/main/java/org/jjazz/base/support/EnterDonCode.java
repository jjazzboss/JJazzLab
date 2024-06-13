package org.jjazz.base.support;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Help", id = "org.config.actions.EnterDonationCode")
@ActionRegistration(displayName = "#CTL_EnterDonationCode", lazy = true)
@ActionReference(path = "Menu/Help", position = 400)
public final class EnterDonCode implements ActionListener
{

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var dlg = new EnterDonCodeDialog();
        dlg.setVisible(true);
        dlg.dispose();
    }


}
