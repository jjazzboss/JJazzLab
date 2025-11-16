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
package org.jjazz.spteditor.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.jjazz.spteditor.spi.SptEditorFactory;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.UndoRedo;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


@ConvertAsProperties(
        dtd = "-//org.jjazz.spteditor.api//SptEditorTopComponent//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "SptEditorTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = true, position = 200)
@ActionID(category = "Window", id = "org.jjazz.spteditor.api.SptEditorTopComponent")
// @ActionReference(path = "Menu/Window", position = 130)     useless if not closable
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_SptEditorAction",
        preferredID = "SptEditorTopComponent"
)
/**
 * A TopComponent to edit one or more SongParts.
 * <p>
 * TopComponent's lookup is the SptEditor's lookup.
 */
public final class SptEditorTopComponent extends TopComponent
{

    private SptEditor sptEditor;
    private static final Logger LOGGER = Logger.getLogger(SptEditorTopComponent.class.getSimpleName());

    public SptEditorTopComponent()
    {
        setName(ResUtil.getString(getClass(), "CTL_SptEditorTopComponent"));
        setToolTipText(ResUtil.getString(getClass(), "CTL_SptEditorTopComponent"));
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);        // Not enough! see canClose() below
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.FALSE);


        try
        {
            // Catch exception & assertion errors to make sure user is notified, and that SptEditorTopComponent is opened.
            // Fix Issue #261: If there is a bug in MixConsole(), SptEditorTopComponent will not be displayed, user will exit JJazzLab and Netbeans
            // will save the window configuration (WindowManager.wswmgr) without SptEditorTopComponent. When user restarts JJazzLab, SptEditorTopComponent will not appear
            // anymore, even if the bug has gone!
            SptEditorFactory factory = SptEditorFactory.getDefault();
            sptEditor = factory.createEditor(factory.getDefaultSptEditorSettings(), factory.getDefaultRpEditorFactory());

        } catch (Throwable t)
        {
            // Log & notify user, this is serious
            String msg = "ERROR can't create the SptEditor, please report this bug with the Log file content. err='" + t.getMessage() + "'";
            LOGGER.log(Level.SEVERE, "SptEditorTopComponent() {0}", msg);
            Exceptions.printStackTrace(t);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notifyLater(nd);
        }

        initComponents();

    }


    /**
     * When UI is ready there is normally one unique instance of this TopComponent, which is not closable.
     *
     * @return
     */
    static public SptEditorTopComponent getInstance()
    {
        return (SptEditorTopComponent) WindowManager.getDefault().findTopComponent("SptEditorTopComponent");
    }

    /**
     * @return The actions to be shown in the TopComponent popup menu.
     */
    @Override
    public Action[] getActions()
    {
        var res = UIUtilities.getNetbeansTopComponentTabActions(super.getActions());
        return res.toArray(Action[]::new);
    }

    @Override
    public void componentDeactivated()
    {
        super.componentDeactivated();
    }

    @Override
    public boolean canClose()
    {
        // fix Issue #549  MixConsoleTopComponent and SptEditorTopComponent can be closed by middle-click      
        return false;
        // We have putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE) but it actually just removes the close button from the tab and the close command from the popup menu.
        // Tried adding putClientProperty(TabbedPaneFactory.NO_CLOSE_BUTTON, Boolean.TRUE) but still not enough to prevend middle-click closing.
        // Note1: in branding we have View.TopComponent.Closing.Enabled=true (applies to non-editor windows) because we want tools like ChordInspector TopComponents to be closable.
        // Note2: another approach would have been to try manging user events on TopComponent tabs using https://bits.netbeans.org/dev/javadoc/org-netbeans-swing-tabcontrol/org/netbeans/swing/tabcontrol/TabbedContainer.html
    }

    /**
     * Override to proxy the undoManager of the sptEditor.
     *
     * @return
     */
    @Override
    public UndoRedo getUndoRedo()
    {
        return sptEditor.getUndoManager();
    }

    @Override
    public Lookup getLookup()
    {
        return sptEditor.getLookup();
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jScrollPane1 = new javax.swing.JScrollPane(sptEditor);

        setLayout(new java.awt.BorderLayout());
        add(jScrollPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
      @Override
    public void componentOpened()
    {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed()
    {
        sptEditor.cleanup();
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
