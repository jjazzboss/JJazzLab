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
package org.jjazz.ui.cl_editor.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.Action;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.base.actions.Savable;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.savablesong.SavableSong;
import org.jjazz.song.api.Song;
import org.jjazz.ui.cl_editor.CL_EditorController;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import static org.jjazz.ui.cl_editor.api.Bundle.*;
import org.openide.util.Utilities;

/**
 * Top component for the ChordLeadSheet editor.
 * <p>
 * TopComponent header's popupmenu actions can be added at path "Actions/CL_EditorTopComponent".<br>
 * Accept a paired TopComponent which must be always be shown/closed in the same time.<br>
 * TopComponent's lookup is the CL_Editor's lookup.
 */
@Messages(
        {
            "CTL_CL_ConfirmClose=OK to close this song without saving changes ?"
        })
public final class CL_EditorTopComponent extends TopComponent implements PropertyChangeListener
{

    /**
     * Our model.
     */
    private Song songModel;
    /**
     * The ChordLeadSheet editor.
     */
    private CL_Editor clEditor;
    /**
     * The editor's controller.
     */
    private CL_EditorController clEditorController;

    /**
     * The paired TopComponent.
     */
    private TopComponent pairedTc;
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
        ActiveSongManager.getInstance().addPropertyListener(this);

        // Create our editor
        clEditor = CL_EditorFactory.getDefault().createEditor(songModel);

        // Our controller
        clEditorController = new CL_EditorController(clEditor);
        clEditor.setEditorMouseListener(clEditorController);

        initComponents();

        updateTabName();
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
        List<? extends Action> newActions = Utilities.actionsForPath("Actions/CL_EditorTopComponent");
        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(newActions);
        if (!newActions.isEmpty())
        {
            actions.add(null);   // Separator         
        }
        actions.addAll(Arrays.asList(super.getActions())); // Get the standard builtin actions Close, Close All, Close Other      
        return actions.toArray(new Action[0]);
    }

    @Override
    public int getPersistenceType()
    {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    public boolean canClose()
    {
        SavableSong ss = getLookup().lookup(SavableSong.class);
        if (ss != null)
        {
            String msg = songModel.getName() + " : " + CTL_CL_ConfirmClose();
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.OK_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(nd);
            if (result != NotifyDescriptor.OK_OPTION)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Bind this TopComponent to another TopComponent. Show/Close operations initiated on this TopComponent will be replicated on
     * the paired TopComponent.
     *
     * @param tc
     */
    public void setPairedTopComponent(TopComponent tc)
    {
        pairedTc = tc;
    }

    public CL_Editor getCL_Editor()
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
     * Return the active CL_EditorTopComponent.
     *
     * @return Null if no active CL_EditorTopComponent found.
     */
    static public CL_EditorTopComponent getActive()
    {
        TopComponent tc = TopComponent.getRegistry().getActivated();
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
        for (Iterator<TopComponent> it = tcs.iterator(); it.hasNext();)
        {
            TopComponent tc = it.next();
            if (tc instanceof CL_EditorTopComponent)
            {
                CL_EditorTopComponent clTc = (CL_EditorTopComponent) tc;
                if (clTc.getCL_Editor().getModel() == cls)
                {
                    return clTc;
                }
            }
        }
        return null;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        scrollPaneCL_Editor = new javax.swing.JScrollPane(clEditor);

        setLayout(new java.awt.BorderLayout());

        scrollPaneCL_Editor.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPaneCL_Editor, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPaneCL_Editor;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened()
    {
    }

    @Override
    public void componentClosed()
    {
        if (pairedTc != null)
        {
            pairedTc.close();
        }
        SavableSong ss = getLookup().lookup(SavableSong.class);
        if (ss != null)
        {
            Savable.ToBeSavedList.remove(ss);
        }
        songModel.removePropertyChangeListener(this);
        ActiveSongManager.getInstance().removePropertyListener(this);
        clEditor.cleanup();
    }

    /**
     * Show the paired TopComponent as well.
     */
    @Override
    public void componentActivated()
    {
        super.componentActivated();
        if (pairedTc != null)
        {
            pairedTc.requestVisible();
        }
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
                ActiveSongManager asm = ActiveSongManager.getInstance();
                if (evt.getSource() == songModel)
                {
                    if (evt.getPropertyName() == Song.PROP_NAME)
                    {
                        updateTabName();
                    } else if (evt.getPropertyName() == Song.PROP_MODIFIED_OR_SAVED)
                    {
                        updateTabName();
                    }
                } else if (evt.getSource() == asm)
                {
                    if (evt.getPropertyName() == ActiveSongManager.PROP_ACTIVE_SONG)
                    {
                        updateTabName();
                    }
                }
            }
        };
        org.jjazz.ui.utilities.Utilities.invokeLaterIfNeeded(run);
    }

    // -------------------------------------------------------------------------------------
    // Private functions
    // -------------------------------------------------------------------------------------  
    private void updateTabName()
    {
        boolean isActive = ActiveSongManager.getInstance().getActiveSong() == songModel;
        String name = isActive ? songModel.getName() + " [ON]" : songModel.getName();
        if (songModel.needSave())
        {
            setHtmlDisplayName("<html><b>" + name + "</b></html>");
        } else
        {
            setHtmlDisplayName("<html>" + name + "</html>");
        }
        String tt = songModel.getFile() == null ? "No file" : songModel.getFile().getAbsolutePath();
        setToolTipText(tt);
    }
}
