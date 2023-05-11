package org.jjazz.base.api.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Help", id = "org.jjazz.base.actions.AboutAction")
@ActionRegistration(displayName = "#CTL_About", lazy = true)
@ActionReference(path = "Menu/Help", position = 2000, separatorBefore = 1999)
public final class AboutAction implements ActionListener
{

    @Override
    public void actionPerformed(ActionEvent e)
    {
        AboutDialog dialog = new AboutDialog();
        dialog.setVisible(true);
    }
}
