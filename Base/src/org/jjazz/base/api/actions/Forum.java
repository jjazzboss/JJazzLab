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

@ActionID(category = "Help", id = "org.jjazz.base.actions.Forum")
@ActionRegistration(displayName = "#CTL_Forum", lazy = true)
@ActionReference(path = "Menu/Help", position = 200)
public final class Forum implements ActionListener
{
    public static final String DOC_URL = "https://jjazzlab.freeforums.net";

    @Override
    public void actionPerformed(ActionEvent e)
    {
           URL url = null;
        try
        {
            url = new URL(DOC_URL);
        } catch (MalformedURLException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        Utilities.openInBrowser(url, false);
    }
}
