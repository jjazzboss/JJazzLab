/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.ui.mixconsole.api;

import java.util.logging.Logger;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.UndoRedo;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;

/**
 * Top component which displays something.
 * <p>
 * TopComponent's lookup is the MixConsole object's lookup.
 */
@ConvertAsProperties(
        dtd = "-//org.jjazz.ui.mixconsole.api//MixConsole//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "MixConsoleTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = true, position = 1)
@ActionID(category = "Window", id = "org.jjazz.ui.mixconsole.api.MixConsoleTopComponent")
// @ActionReference(path = "Menu/Window", position = 120)      Useless if not closable
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_MixConsoleTopComponent",
        preferredID = "MixConsoleTopComponent"
)
public final class MixConsoleTopComponent extends TopComponent
{

    private static MixConsoleTopComponent INSTANCE;
    private MixConsole editor;
    private static final Logger LOGGER = Logger.getLogger(MixConsoleTopComponent.class.getSimpleName());

    public MixConsoleTopComponent()
    {
        setName(ResUtil.getString(getClass(), "CTL_MixConsoleTopComponent"));
        setToolTipText(ResUtil.getString(getClass(), "CTL_MixConsoleTopComponentDesc"));
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.FALSE);

        initComponents();


        try
        {
            // Catch exception & assertion errors to make sure user is notified, and that MixConsoleTopComponent is opened.
            // Fix Issue #261: If there is a bug in MixConsole(), MixConsoleTopComponent will not be displayed, user will exit JJazzLab and Netbeans
            // will save the window configuration (WindowManager.wswmgr) without MixConsoleTopComponent. When user restarts JJazzLab, MixConsoleTopComponent will not appear
            // anymore, even if the bug has gone!
            editor = new MixConsole(MixConsoleSettings.getDefault());
            add(editor);
        } catch (Throwable t)
        {
            // Log & notify user, this is serious
            String msg = "ERROR can't create the MixConsole, please report this bug with the Log file content. err='" + t.getMessage() + "'";
            LOGGER.severe("MixConsoleTopComponent() " + msg);
            Exceptions.printStackTrace(t);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notifyLater(nd);
        }


        INSTANCE = this;
    }

    /**
     * Get the unique instance of this MixConsoleTopComponent.
     *
     * @return Can be null if not created yet.
     */
    static public MixConsoleTopComponent getInstance()
    {
        return INSTANCE;
    }

    public MixConsole getEditor()
    {
        return editor;
    }

    /**
     * Override to proxy the undoManager of the editor.
     *
     * @return
     */
    @Override
    public UndoRedo getUndoRedo()
    {
        return editor.getUndoManager();
    }

    @Override
    public Lookup getLookup()
    {
        return editor.getLookup();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened()
    {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed()
    {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p)
    {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p)
    {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
