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
package org.jjazz.cl_editor.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JToolBar;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.cl_editor.spi.CL_EditorFactory;
import org.jjazz.song.api.Song;
import org.jjazz.uiutilities.api.UIUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.Actions;
import org.openide.util.Utilities;
import org.openide.windows.Mode;
import org.openide.windows.WindowManager;

/**
 * Top component for the ChordLeadSheet editor.
 * <p>
 * TopComponent header's popupmenu actions can be added at path "Actions/CL_EditorTopComponent".<br>
 * Accept a paired TopComponent which must be always be shown/closed in the same time.<br>
 * TopComponent's lookup is the CL_Editor's lookup.
 */
public final class CL_EditorTopComponent extends TopComponent implements PropertyChangeListener
{

    public static final String MODE = "editor"; // see Netbeans WindowManager modes

    /**
     * Our model.
     */
    private Song songModel;
    /**
     * The ChordLeadSheet editor.
     */
    private CL_Editor clEditor;
    private JToolBar clToolBar;
    private boolean silentClose;

    private static final Logger LOGGER = Logger.getLogger(CL_EditorTopComponent.class.getSimpleName());

    public CL_EditorTopComponent(Song song)
    {
        if (song == null || song.getChordLeadSheet() == null)
        {
            throw new IllegalArgumentException("song=" + song);
        }
        songModel = song;
        songModel.addPropertyChangeListener(this);

        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.TRUE);

        // Listen to active song changes
        ActiveSongManager.getDefault().addPropertyListener(this);

        // Create our editor
        var clef = CL_EditorFactory.getDefault();
        clEditor = clef.createEditor(songModel);
        clToolBar = clef.createEditorToolbar(clEditor);


        initComponents();


        updateTabName();


        // Note: since NB 17 (?), these actions also need to be in the TopComponent ActionMap,  in addition to the ActionMap in Lookup (see CL_EditorController.java)
        getActionMap().put("cut-to-clipboard", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.cut"));
        getActionMap().put("copy-to-clipboard", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.copy"));
        getActionMap().put("paste-from-clipboard", Actions.forID("JJazz", "org.jjazz.cl_editor.actions.paste"));

    }

    @Override
    public Lookup getLookup()
    {
        // Use the editor's lookup as our lookup : important to get ActionMap and selection.  
        return clEditor.getLookup();
    }

    /**
     * Overridden to insert possible new actions from path "Actions/CL_EditorTopComponent".
     *
     * @return The actions to be shown in the TopComponent popup menu.
     */
    @Override
    public Action[] getActions()
    {
        List<? extends Action> tcEditorActions = new ArrayList<>(Utilities.actionsForPath("Actions/CL_EditorTopComponent"));
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
    public int getPersistenceType()
    {
        return TopComponent.PERSISTENCE_NEVER;
    }

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
            songModel.setSaveNeeded(false); // To not make other song editors (eg SS_EditorTopComponent) also ask for user confirmation
        }

        return true;
    }


    public CL_Editor getEditor()
    {
        return clEditor;
    }

    public Song getSongModel()
    {
        return songModel;
    }

    @Override
    public UndoRedo getUndoRedo()
    {
        return clEditor.getUndoManager();
    }

    /**
     * Overridden to set focus on editor when user clicked on the TopComponent tab to activate it.
     * <p>
     * Part of the fix for issue #582.
     */
    @Override
    public void componentActivated()
    {
        // Note that even if user directly clicks on a bar (or chord leadsheet item) viewer while another window had the focus, this method is called before 
        // dispatching the mouse event to the bar viewer        
        clEditor.requestFocusInWindow();
    }

    /**
     * Return the active (i.e. focused or ancestor of the focused component) CL_EditorTopComponent.
     *
     * @return Can be null
     */
    static public CL_EditorTopComponent getActive()
    {
        TopComponent tc = TopComponent.getRegistry().getActivated();
        return (tc instanceof CL_EditorTopComponent) ? (CL_EditorTopComponent) tc : null;
    }

    /**
     * Return the visible CL_EditorTopComponent within its window mode.
     * <p>
     * The visible SS_EditorTopComponent might not be the active one (for example if it's the corresponding SS_EditorTopComponent which is active)
     *
     * @return Can be null if no SS_EditorTopComponent within its window mode.
     */
    static public CL_EditorTopComponent getVisible()
    {
        Mode mode = WindowManager.getDefault().findMode(MODE);
        TopComponent tc = mode.getSelectedTopComponent();
        return (tc instanceof CL_EditorTopComponent) ? (CL_EditorTopComponent) tc : null;
    }

    /**
     * Return the editor for a specific ChordLeadSheet.
     *
     * @param cls
     * @return Null if not found in the open CL_EditorTopComponent windows.
     */
    static public CL_EditorTopComponent get(ChordLeadSheet cls)
    {
        Set<TopComponent> tcs = TopComponent.getRegistry().getOpened();
        for (TopComponent tc : tcs)
        {
            if (tc instanceof CL_EditorTopComponent clTc)
            {
                if (clTc.getEditor().getModel() == cls)
                {
                    return clTc;
                }
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

        scrollPaneCL_Editor = new javax.swing.JScrollPane(clEditor);
        toolbar = clToolBar;

        setLayout(new java.awt.BorderLayout());

        scrollPaneCL_Editor.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPaneCL_Editor, java.awt.BorderLayout.CENTER);

        toolbar.setOrientation(javax.swing.SwingConstants.VERTICAL);
        toolbar.setRollover(true);
        add(toolbar, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPaneCL_Editor;
    private javax.swing.JToolBar toolbar;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened()
    {
    }

    @Override
    public void componentClosed()
    {
        songModel.removePropertyChangeListener(this);
        ActiveSongManager.getDefault().removePropertyListener(this);
        clEditor.cleanup();
    }


//    void writeProperties(java.util.Properties p)
//    {
//        // better to version settings since initial version as advocated at
//        // http://wiki.apidesign.org/wiki/PropertyFiles
//        p.setProperty("version", "1.0");
//        // TODO store your settings
//    }
//
//    void readProperties(java.util.Properties p)
//    {
//        String version = p.getProperty("version");
//        // TODO read your settings according to their version
//    }
    @Override
    public void propertyChange(final PropertyChangeEvent evt)
    {
        // Model changes can be generated outside the EDT      
        Runnable run = new Runnable()
        {
            @Override
            public void run()
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

        String tt = songModel.getFile() == null ? ResUtil.getString(getClass(), "CTL_NotSavedToFileYet") : songModel.getFile().getAbsolutePath();
        setToolTipText(ResUtil.getString(getClass(), "CTL_ChordLeadSheetEditor") + ": " + tt);
    }
}
