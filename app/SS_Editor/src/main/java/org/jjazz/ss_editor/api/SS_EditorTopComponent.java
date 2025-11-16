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
package org.jjazz.ss_editor.api;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import org.jjazz.ss_editor.spi.SS_EditorSettings;
import org.jjazz.ss_editor.spi.SS_EditorFactory;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ss_editor.rpviewer.api.RpViewer;
import org.jjazz.ss_editor.sptviewer.api.SptViewer;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerFactory;
import org.jjazz.uiutilities.api.UIUtilities;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Actions;
import org.openide.windows.Mode;
import static org.openide.windows.TopComponent.PERSISTENCE_NEVER;
import org.openide.windows.WindowManager;

/**
 * Top component for the SongStructure editor.
 * <p>
 * TopComponent header's popupmenu actions can be added at path "Actions/SS_EditorTopComponent".<br>
 * Accept a paired TopComponent which must be always be shown/closed in the same time.<br>
 * The TopComponent's lookup is the SS_Editor's lookup.
 */
@TopComponent.Description(preferredID = "SS_EditorTopComponentId", persistenceType = PERSISTENCE_NEVER)
public final class SS_EditorTopComponent extends TopComponent implements PropertyChangeListener
{

    public static final String MODE = "output";  // see Netbeans WindowManager modes
    /**
     * Our model.
     */
    private Song songModel;
    /**
     * The SongStructure editor.
     */
    private SS_Editor ssEditor;
    private JToolBar ssToolBar;
    private boolean silentClose;

    private static final Logger LOGGER = Logger.getLogger(SS_EditorTopComponent.class.getName());


    /**
     * Create an editor.
     *
     * @param song
     */
    public SS_EditorTopComponent(Song song)
    {
        if (song == null || song.getSongStructure() == null)
        {
            throw new IllegalArgumentException("song=" + song);
        }
        songModel = song;
        songModel.addPropertyChangeListener(this);

        // Listen to active song changes
        ActiveSongManager.getDefault().addPropertyListener(this);

        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.TRUE);

        // Create our editor
        var ssef = SS_EditorFactory.getDefault();
        ssEditor = ssef.createEditor(songModel, SS_EditorSettings.getDefault(), SptViewerFactory.getDefault());


        // Create the toolbar
        ssToolBar = ssef.createEditorToolbar(ssEditor);

        initComponents();

        updateTabName();

    }

    /**
     * Overridden to insert possible new actions from path "Actions/SS_EditorTopComponent".
     *
     * @return The actions to be shown in the TopComponent popup menu.
     */
    @Override
    public Action[] getActions()
    {
        List<? extends Action> tcEditorActions = new ArrayList<>(Utilities.actionsForPath("Actions/SS_EditorTopComponent"));
        List<Action> res = new ArrayList<>();
        res.addAll(tcEditorActions);
        if (!res.isEmpty())
        {
            res.add(null);   // Separator         
        }

       res.addAll(UIUtilities.getNetbeansTopComponentTabActions(super.getActions()));

        return res.toArray(Action[]::new);
    }


    @Override
    public Lookup getLookup()
    {
        return ssEditor.getLookup();
    }

    public Song getSongModel()
    {
        return songModel;
    }


    public SS_Editor getEditor()
    {
        return ssEditor;
    }

    @Override
    public UndoRedo getUndoRedo()
    {
        return ssEditor.getUndoManager();
    }

    /**
     * Overridden to set focus on editor when user clicked on the TopComponent tab to activate it.
     * <p>
     * Part of the fix for issue #582.
     */
    @Override
    public void componentActivated()
    {
        // Note that even if user directly clicks on a SongPart (or rhythm parameter) viewer while another window had the focus, this method is called before 
        // dispatching the mouse event to the bar viewer        
        ssEditor.requestFocusInWindow();
    }

    /**
     * Return the active (i.e. focused or ancestor of the focused component) SS_EditorTopComponent.
     *
     * @return Can be null
     */
    static public SS_EditorTopComponent getActive()
    {
        TopComponent tc = TopComponent.getRegistry().getActivated();
        return (tc instanceof SS_EditorTopComponent) ? (SS_EditorTopComponent) tc : null;
    }

    /**
     * Return the visible SS_EditorTopComponent within its window mode.
     * <p>
     * The visible SS_EditorTopComponent might not be the active one (for example if it's the corresponding CL_EditorTopComponent which is active).
     *
     * @return Can be null if no SS_EditorTopComponent within its window mode.
     */
    static public SS_EditorTopComponent getVisible()
    {
        Mode mode = WindowManager.getDefault().findMode(MODE);
        TopComponent tc = mode.getSelectedTopComponent();
        return (tc instanceof SS_EditorTopComponent) ? (SS_EditorTopComponent) tc : null;
    }


    /**
     * Return the editor for a specific SongStructure.
     *
     * @param sgs
     * @return Null if not found in the open SS_EditorTopComponent windows.
     */
    static public SS_EditorTopComponent get(SongStructure sgs)
    {
        Set<TopComponent> tcs = TopComponent.getRegistry().getOpened();
        for (TopComponent tc : tcs)
        {
            if (tc instanceof SS_EditorTopComponent ssTc && ssTc.getEditor().getModel() == sgs)
            {
                return ssTc;
            }
        }
        return null;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        scrollPane_SS_Editor = new javax.swing.JScrollPane(ssEditor);
        toolbar = ssToolBar;

        setLayout(new java.awt.BorderLayout());
        add(scrollPane_SS_Editor, java.awt.BorderLayout.CENTER);

        toolbar.setOrientation(javax.swing.SwingConstants.VERTICAL);
        toolbar.setRollover(true);
        add(toolbar, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPane_SS_Editor;
    private javax.swing.JToolBar toolbar;
    // End of variables declaration//GEN-END:variables


    /**
     * Close the TopComponent without asking for user confirmation.
     */
    public void closeSilent()
    {
        silentClose = true;
        close();
    }

    @Override
    public boolean canClose()
    {

        if (!silentClose && songModel.isSaveNeeded())
        {
            String msg = songModel.getName() + " : " + ResUtil.getCommonString("CTL_CL_ConfirmClose");
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.OK_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(nd);
            if (result != NotifyDescriptor.OK_OPTION)
            {
                return false;
            }
            songModel.setSaveNeeded(false); // To not make other song editors (eg CL_EditorTopComponent) also ask for user confirmation
        }

        return true;
    }


    @Override
    public void componentClosed()
    {
        songModel.removePropertyChangeListener(this);
        ssEditor.cleanup();
    }


    //    void writeProperties(java.util.Properties p)
//    {
//        // better to version settings since initial version as advocated at
//        // http://wiki.apidesign.org/wiki/PropertyFiles
//        p.setProperty("version", "1.0");
//        // to do store your settings
//    }
//
//    void readProperties(java.util.Properties p)
//    {
//        String version = p.getProperty("version");
//        // TO DO read your settings according to their version
//    }
    @Override
    public void propertyChange(final PropertyChangeEvent evt)
    {
        // Model changes can be generated outside the EDT      
        Runnable run = () -> 
        {
            ActiveSongManager asm = ActiveSongManager.getDefault();
            if (evt.getSource() == songModel)
            {
                if (evt.getPropertyName().equals(Song.PROP_NAME))
                {
                    updateTabName();
                } else if (evt.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED_OR_RESET))
                {
                    updateTabName();
                }
            } else if (evt.getSource() == asm)
            {
                if (evt.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
                {
                    updateTabName();
                }
            }
        };
        org.jjazz.uiutilities.api.UIUtilities.invokeLaterIfNeeded(run);
    }

    // -------------------------------------------------------------------------------------
    // Private functions
    // -------------------------------------------------------------------------------------  
    private void updateTabName()
    {
        boolean isActive = ActiveSongManager.getDefault().getActiveSong() == songModel;
        String name = isActive ? songModel.getName() + " [ON]" : songModel.getName();
        if (songModel.isSaveNeeded())
        {
            setHtmlDisplayName("<html><b>" + name + "</b></html>");
        } else
        {
            setHtmlDisplayName("<html>" + name + "</html>");
        }
        String tt = songModel.getFile() == null ? ResUtil.getString(getClass(), "CTL_NotSavedYet") : songModel.getFile().getAbsolutePath();
        setToolTipText(ResUtil.getString(getClass(), "CTL_SongStructureEditor") + ": " + tt);
    }
}
