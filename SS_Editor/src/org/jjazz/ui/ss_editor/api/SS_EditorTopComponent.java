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

import org.jjazz.ui.ss_editor.spi.SS_EditorSettings;
import org.jjazz.ui.ss_editor.spi.SS_EditorFactory;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.base.api.actions.Savable;
import org.jjazz.song.api.Song;
import org.jjazz.ui.ss_editor.SS_EditorController;
import org.jjazz.savablesong.api.SavableSong;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.sptviewer.spi.SptViewerFactory;
import org.jjazz.ui.ss_editor.CompactViewModeController;
import org.jjazz.ui.ss_editor.SS_EditorImpl;
import org.jjazz.ui.ss_editor.SS_EditorToolBar;
import org.jjazz.ui.utilities.api.Zoomable;
import org.jjazz.util.api.ResUtil;
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
    /**
     * The editor's controller.
     */
    private SS_EditorMouseListener ssEditorController;
    private CompactViewModeController compactViewController;
    /**
     * The paired TopComponent.
     */
    private TopComponent pairedTc;
    private SS_EditorToolBar ssToolBar;

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
            throw new IllegalArgumentException("song=" + song);   //NOI18N
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
        ssEditor = SS_EditorFactory.getDefault().createEditor(songModel, SS_EditorSettings.getDefault(), SptViewerFactory.getDefault());
        ssEditorController = new SS_EditorController(ssEditor);
        ssEditor.setController(ssEditorController);
        compactViewController = new CompactViewModeController(ssEditor);


        // Create the toolbar
        ssToolBar = new SS_EditorToolBar(ssEditor);

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
        List<? extends Action> newActions = Utilities.actionsForPath("Actions/SS_EditorTopComponent");
        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(newActions);
        if (!newActions.isEmpty())
        {
            actions.add(null);   // Separator         
        }
        Collections.addAll(actions, super.getActions());// Get the standard builtin actions Close, Close All, Close Other      
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

    /**
     * Bind this TopComponent to another TopComponent.
     * <p>
     * Show/Close operations initiated on this TopComponent will be replicated on the paired TopComponent.
     *
     * @param tc
     */
    public void setPairedTopComponent(TopComponent tc)
    {
        pairedTc = tc;
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
     * The visible SS_EditorTopComponent might not be the active one (for example if it's the corresponding CL_EditorTopComponent
     * which is active).
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
        for (Iterator<TopComponent> it = tcs.iterator(); it.hasNext();)
        {
            TopComponent tc = it.next();
            if (tc instanceof SS_EditorTopComponent)
            {
                SS_EditorTopComponent ssTc = (SS_EditorTopComponent) tc;
                if (ssTc.getEditor().getModel() == sgs)
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

        scrollPane_SS_Editor = new javax.swing.JScrollPane(ssEditor);
        toolbar = ssToolBar;

        setLayout(new java.awt.BorderLayout());
        add(scrollPane_SS_Editor, java.awt.BorderLayout.CENTER);

        toolbar.setFloatable(false);
        toolbar.setOrientation(javax.swing.SwingConstants.VERTICAL);
        toolbar.setRollover(true);
        add(toolbar, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPane_SS_Editor;
    private javax.swing.JToolBar toolbar;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened()
    {

        // Try to restore zoom factor X from client property, or zoom to fit width
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                String str = songModel.getClientProperty(SS_EditorImpl.PROP_ZOOM_FACTOR_X, null);
                if (str != null)
                {
                    int zfx = -1;
                    try
                    {
                        zfx = Integer.valueOf(str);
                    } catch (NumberFormatException e)
                    {
                        // Nothing
                    }
                    if (zfx < 0 || zfx > 100)
                    {
                        LOGGER.warning("SS_EditorController() Invalid zoom factor X client property=" + str + " in song=" + ssEditor.getSongModel().getName());   //NOI18N
                    } else
                    {
                        Zoomable zoomable = ssEditor.getLookup().lookup(Zoomable.class);
                        if (zoomable != null)
                        {
                            zoomable.setZoomXFactor(zfx, false);    // This will mark songModel as modified via Song.putClientProperty()
                        }
                    }
                } else
                {
                    ssEditor.setZoomHFactorToFitWidth(SS_EditorTopComponent.this.getWidth());   // This will mark songModel as modified via Song.putClientProperty()
                }

            }
        };
        // If invokeLater is not used layout is not yet performed and components size are = 0 !
        SwingUtilities.invokeLater(r);
    }


    @Override
    public boolean canClose()
    {
        if (pairedTc != null)
        {
            // If CL_Editor is first closed, just let this TopComponent be closed
            // If CL_Editor is still here, user is closing this TopComponent first, rely on CL_Editor canClose() logic because CL_Editor listens to song changes to add a Savable instance in its lookup
            return pairedTc.isOpened() ? pairedTc.canClose() : true;
        }

        return true;
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
            ActiveSongManager asm = ActiveSongManager.getInstance();
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
        org.jjazz.ui.utilities.api.Utilities.invokeLaterIfNeeded(run);
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
        String tt = songModel.getFile() == null ? ResUtil.getString(getClass(), "CTL_NotSavedYet") : songModel.getFile().getAbsolutePath();
        setToolTipText(ResUtil.getString(getClass(), "CTL_SongStructureEditor") + ": " + tt);
    }
}
