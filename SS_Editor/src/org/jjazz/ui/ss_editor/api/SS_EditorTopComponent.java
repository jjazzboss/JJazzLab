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
package org.jjazz.ui.ss_editor.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.base.actions.Savable;
import org.jjazz.song.api.Song;
import org.jjazz.ui.ss_editor.SS_EditorController;
import org.jjazz.savablesong.SavableSong;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.jjazz.songstructure.api.SongStructure;

/**
 * Top component for the SongStructure editor.
 * <p>
 * TopComponent header's popupmenu actions can be added at path "Actions/SS_EditorTopComponent".<br>
 * Accept a paired TopComponent which must be always be shown/closed in the same time.<br>
 * The TopComponent's lookup is the SS_Editor's lookup.
 */
@Messages(
        {
            "HINT_RL_EditorTopComponent=This is a song structure editor window",
            "CTL_RL_ConfirmClose=OK to close this song without saving changes ?"
        })
public final class SS_EditorTopComponent extends TopComponent implements PropertyChangeListener
{

    /**
     * Our model.
     */
    private Song songModel;
    /**
     * The SongStructure editor.
     */
    private SS_Editor ssEditor;
    /**
     * The editor's controller.
     */
    private SS_EditorMouseListener ssEditorController;
    /**
     * The paired TopComponent.
     */
    private TopComponent pairedTc;
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
        ActiveSongManager.getInstance().addPropertyListener(this);

        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.TRUE);

        // Create our editor
        ssEditor = SS_EditorFactory.getDefault().createEditor(songModel);
        ssEditorController = new SS_EditorController(ssEditor);
        ssEditor.setController(ssEditorController);

        initComponents();

        updateTabName();
        setToolTipText(Bundle.HINT_RL_EditorTopComponent());
    }

    /**
     * Overridden to insert possible new actions from path "Actions/SS_EditorTopComponent".
     *
     * @return The actions to be shown in the TopComponent popup menu.
     */
    @Override
    public Action[] getActions()
    {
        List<? extends Action> newActions = Utilities.actionsForPath("Actions/RL_EditorTopComponent");
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
    public Lookup getLookup()
    {
        // Use the editor's lookup as our lookup        
        return ssEditor.getLookup();
    }

    public Song getSongModel()
    {
        return songModel;
    }

    @Override
    public int getPersistenceType()
    {
        return TopComponent.PERSISTENCE_NEVER;
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

    public SS_Editor getSS_Editor()
    {
        return ssEditor;
    }

    @Override
    public UndoRedo getUndoRedo()
    {
        return ssEditor.getUndoManager();
    }

    /**
     * Return the active SS_EditorTopComponent.
     *
     * @return Null if no active CL_EditorTopComponent found.
     */
    static public SS_EditorTopComponent getActive()
    {
        TopComponent tc = TopComponent.getRegistry().getActivated();
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
        for (Iterator<TopComponent> it = tcs.iterator(); it.hasNext();)
        {
            TopComponent tc = it.next();
            if (tc instanceof SS_EditorTopComponent)
            {
                SS_EditorTopComponent ssTc = (SS_EditorTopComponent) tc;
                if (ssTc.getSS_Editor().getModel() == sgs)
                {
                    return ssTc;
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

        scrollPane_RL_Editor = new javax.swing.JScrollPane(ssEditor);

        setLayout(new java.awt.BorderLayout());
        add(scrollPane_RL_Editor, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPane_RL_Editor;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened()
    {
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                ssEditor.setZoomHFactorToFitWidth(SS_EditorTopComponent.this.getWidth());
            }
        };
        // If invokeLater is not used layout is not yet performed and components size are = 0 !
        SwingUtilities.invokeLater(r);
    }

    /**
     * Close the paired TopComponent as well.
     */
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
        ssEditor.cleanup();
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
    }
}
