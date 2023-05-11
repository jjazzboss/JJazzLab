package org.jjazz.base;

import java.util.logging.Logger;
import javax.swing.JFrame;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;

/**
 * Set the main window title.
 * <p>
 * Need to be done once the UI is ready.
 */
@OnShowing
public class SetMainWindowTitle implements Runnable
{
    private static final Logger LOGGER = Logger.getLogger(SetMainWindowTitle.class.getSimpleName());

    @Override
    public void run()
    {
        JFrame mainFrame = (JFrame) WindowManager.getDefault().getMainWindow();
        String version = System.getProperty("jjazzlab.version");
        if (version == null)
        {
            LOGGER.warning("SetMainWindowTitle.run() The jjazzlab.version system property is not set.");
            version = "";
        } else
        {
            version = " " + version;
        }
        mainFrame.setTitle("JJazzLab " + version);
    }
}
