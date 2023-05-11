package org.jjazz.base.api.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import org.jjazz.util.api.Utilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;

@ActionID(category = "Help", id = "org.jjazz.base.actions.Donate")
@ActionRegistration(displayName = "#CTL_Donate", lazy = true)
@ActionReference(path = "Menu/Help", position = 300, separatorBefore = 299)
public final class Donate implements ActionListener
{

    private static final String DONATE_URL = "https://www.jjazzlab.com/en/donate";

    @Override
    public void actionPerformed(ActionEvent e)
    {
        URL url = null;
        try
        {
            url = new URL(DONATE_URL);
        } catch (MalformedURLException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        Utilities.openInBrowser(url, false);
    }
}
