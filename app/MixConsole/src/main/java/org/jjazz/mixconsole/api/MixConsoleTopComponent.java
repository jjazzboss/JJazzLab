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
package org.jjazz.mixconsole.api;

import java.io.File;
import java.util.Collection;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.uiutilities.api.SingleFileDragInTransferHandler;
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

/**
 * Top component which displays something.
 * <p>
 * TopComponent's lookup is the MixConsole object's lookup.
 */
@ConvertAsProperties(
        dtd = "-//org.jjazz.mixconsole.api//MixConsole//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "MixConsoleTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = true, position = 0)
@ActionID(category = "Window", id = "org.jjazz.mixconsole.api.MixConsoleTopComponent")
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
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);        // Not enough ! see canClose() below
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.FALSE);


        // For Issue #377 be extra careful to make sure there is no exception at this stage
        String name = "MixConsole bug, please report";
        String tooltip = name;
        try
        {
            name = ResUtil.getString(getClass(), "CTL_MixConsoleTopComponent");
            tooltip = ResUtil.getString(getClass(), "CTL_MixConsoleTopComponentDesc");
        } catch (MissingResourceException ex)
        {
            exceptionOccuredInConstructor(ex);
        }
        setName(name);
        setToolTipText(tooltip);


        initComponents();   // only one basic line, can't fail

        setTransferHandler(new TcTransferHandler());

        try
        {
            // Catch exception & assertion errors to make sure user is notified, and that MixConsoleTopComponent is still created.
            // Fix Issue #261: If there is a bug in MixConsole(), MixConsoleTopComponent will not be displayed, user will exit JJazzLab and Netbeans
            // will save the window configuration (WindowManager.wswmgr) without MixConsoleTopComponent. When user restarts JJazzLab, MixConsoleTopComponent will not appear
            // anymore, even if the bug has gone!            
            editor = new MixConsole(MixConsoleSettings.getDefault());  // If bug here then editor is null
            add(editor);
        } catch (Throwable t)
        {
            // Log & notify user, this is serious, but let MixConsole be created
            exceptionOccuredInConstructor(t);
        }


        INSTANCE = this;

    }

    static private void exceptionOccuredInConstructor(Throwable t)
    {
        String msg = "ERROR can't create the MixConsole, please report this bug with the Log file content. err='" + t.getMessage() + "'";
        LOGGER.log(Level.SEVERE, "MixConsoleTopComponent() {0}", msg);
        Exceptions.printStackTrace(t);
        NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
        DialogDisplayer.getDefault().notifyLater(nd);
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
     * @return The actions to be shown in the TopComponent popup menu.
     */
    @Override
    public Action[] getActions()
    {
        var res = UIUtilities.getNetbeansTopComponentTabActions(super.getActions());
        return res.toArray(Action[]::new);
    }

    /**
     * Override to proxy the undoManager of the editor.
     *
     * @return
     */
    @Override
    public UndoRedo getUndoRedo()
    {
        // Fix Issue #377: editor might be null if exception was catched in constructor
        return editor != null ? editor.getUndoManager() : super.getUndoRedo();
    }

    @Override
    public Lookup getLookup()
    {
        // Fix Issue #377: editor might be null if exception was catched in constructor
        return editor != null ? editor.getLookup() : super.getLookup();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
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


    /**
     * A transfer handler to enable song file drag-in in this TopComponent when no song is opened yet.
     * <p>
     * When a song is opened, the MixConsoleTransferHandler is used instead since this TopComponent will be covered by the MixConsole component.
     */
    public static class TcTransferHandler extends SingleFileDragInTransferHandler
    {

        private Collection<String> fileExtensions;

        @Override
        protected boolean isImportEnabled()
        {
            return true;
        }

        @Override
        protected boolean importFile(File file)
        {
            try
            {
                SongEditorManager.getDefault().showSong(file, true, false);

            } catch (SongCreationException ex)
            {
                LOGGER.log(Level.WARNING, "importDataFile() Error loading dragged-in file {0}: {1}", new Object[]
                {
                    file.getAbsolutePath(), ex.getMessage()
                });
                String msg = ResUtil.getCommonString("ErrorLoadingSongFile", file.getAbsolutePath(), ex.getLocalizedMessage());
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return false;
            }

            return true;
        }

        @Override
        protected Collection<String> getAcceptedFileExtensions()
        {
            if (fileExtensions == null)
            {
                fileExtensions = SongImporter.getAllSupportedFileExtensions();
                fileExtensions.add("sng");
            }
            return fileExtensions;
        }
    }
}
