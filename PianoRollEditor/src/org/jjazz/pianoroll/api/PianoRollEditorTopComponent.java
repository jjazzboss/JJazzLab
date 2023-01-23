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
package org.jjazz.pianoroll.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.EditToolBar;
import org.jjazz.pianoroll.ToolbarPanel;
import org.jjazz.pianoroll.edittools.EraserTool;
import org.jjazz.pianoroll.edittools.PencilTool;
import org.jjazz.pianoroll.edittools.SelectionTool;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.song.api.Song;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;


/**
 * The TopComponent for a PianoRollEditor.
 * <p>
 */
public final class PianoRollEditorTopComponent extends TopComponent
{

    // public static final String MODE = "midieditor";  // WindowManager mode
    public static final String MODE = "editor";  // WindowManager mode


    private final PianoRollEditor editor;
    private final List<EditTool> editTools;
    private final ToolbarPanel toolbarPanel;
    private final JPopupMenu popupMenu;
    private static final Logger LOGGER = Logger.getLogger(PianoRollEditorTopComponent.class.getSimpleName());
    private final Song song;


    /**
     * Create a TopComponent editor for the specified parameters.
     *
     * @param song
     * @param tabName       The TopComponent name
     * @param title         The title used within the editor
     * @param startBarIndex
     * @param spModel
     * @param keyMap        Can be null
     * @param settings
     */
    public PianoRollEditorTopComponent(Song song, String tabName, String title, int startBarIndex, SizedPhrase spModel, DrumKit.KeyMap keyMap, PianoRollEditorSettings settings)
    {

        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.FALSE);     
        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.FALSE);


        setDisplayName(tabName);

        this.song = song;
        editor = new PianoRollEditor(startBarIndex, spModel, keyMap, settings);
        editTools = Arrays.asList(new SelectionTool(editor), new PencilTool(editor), new EraserTool(editor));
        editor.setActiveTool(editTools.get(0));
        toolbarPanel = new ToolbarPanel(editor, title, editTools);


        initComponents();


        // The popupmenu
        popupMenu = new JPopupMenu();
        var menuItem = new JMenuItem();
        menuItem.setBorder(BorderFactory.createEmptyBorder());
        EditToolBar editToolBar = new EditToolBar(editor, editTools);
        editToolBar.setClickListener(() -> popupMenu.setVisible(false));
        menuItem.setPreferredSize(editToolBar.getPreferredSize());
        menuItem.add(editToolBar);
        popupMenu.add(menuItem);
        editor.getNotesPanel().setComponentPopupMenu(popupMenu);
    }

    /**
     * The title used within the editor.
     *
     * @return
     */
    public String getTitle()
    {
        return toolbarPanel.getTitle();
    }

    /**
     * Set the title used within the editor.
     *
     * @param title
     */
    public void setTitle(String title)
    {
        toolbarPanel.setTitle(title);
    }

    /**
     * Update the edited model.
     *
     * @param startBarIndex
     * @param spModel
     * @param keyMap
     */
    public void setModel(int startBarIndex, SizedPhrase spModel, DrumKit.KeyMap keyMap)
    {
        editor.setModel(startBarIndex, spModel, keyMap);
    }


    /**
     * The song associated to this TopComponent.
     *
     * @return
     */
    public Song getSong()
    {
        return song;
    }

    @Override
    public String preferredID()
    {
        return "PianoRollEditorTopComponent" + getSong().getName();
    }

    public PianoRollEditor getEditor()
    {
        return editor;
    }


    @Override
    public UndoRedo getUndoRedo()
    {
        return editor.getUndoManager();
    }

    /**
     * Overridden to insert possible new actions from path "Actions/PianoRollEditorTopComponent".
     *
     * @return The actions to be shown in the TopComponent popup menu.
     */
    @Override
    public Action[] getActions()
    {
        List<? extends Action> newActions = Utilities.actionsForPath("Actions/PianoRollEditorTopComponent");
        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(newActions);
        if (!newActions.isEmpty())
        {
            actions.add(null);   // Separator         
        }
        Collections.addAll(actions, super.getActions()); // Get the standard builtin actions Close, Close All, Close Other      
        return actions.toArray(new Action[0]);
    }

    @Override
    public Lookup getLookup()
    {
        return editor.getLookup();
    }

    @Override
    public int getPersistenceType()
    {
        return TopComponent.PERSISTENCE_NEVER;
    }

    /**
     * Return the active (i.e. focused or ancestor of the focused component) PianoRollEditorTopComponent.
     *
     * @return Can be null
     */
    static public PianoRollEditorTopComponent getActive()
    {
        TopComponent tc = TopComponent.getRegistry().getActivated();
        return (tc instanceof PianoRollEditorTopComponent) ? (PianoRollEditorTopComponent) tc : null;
    }

    /**
     * Search for the PianoRollEditorTopComponent associated to song.
     *
     * @param song
     * @return Can be null
     */
    static public PianoRollEditorTopComponent get(Song song)
    {
        Set<TopComponent> tcs = TopComponent.getRegistry().getOpened();
        return tcs.stream()
                .filter(tc -> tc instanceof PianoRollEditorTopComponent preTc && preTc.getSong() == song)
                .map(tc -> (PianoRollEditorTopComponent) tc)
                .findAny()
                .orElse(null);
    }

    @Override
    public boolean canClose()
    {
//        SavableSong ss = getLookup().lookup(SavableSong.class);
//        if (ss != null)
//        {
//            String msg = songModel.getName() + " : " + ResUtil.getString(getClass(), "CTL_CL_ConfirmClose");
//            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.OK_CANCEL_OPTION);
//            Object result = DialogDisplayer.getDefault().notify(nd);
//            if (result != NotifyDescriptor.OK_OPTION)
//            {
//                return false;
//            }
//        }
        return true;
    }

    @Override
    public void componentOpened()
    {

    }

    @Override
    public void componentClosed()
    {
        editor.cleanup();
    }

    // ============================================================================================
    // Private methods
    // ============================================================================================

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
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_toolbar = toolbarPanel;
        pnl_editor = editor;

        setToolTipText(org.openide.util.NbBundle.getMessage(PianoRollEditorTopComponent.class, "PianoRollEditorTopComponent.toolTipText")); // NOI18N
        setLayout(new java.awt.BorderLayout());
        add(pnl_toolbar, java.awt.BorderLayout.NORTH);
        add(pnl_editor, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel pnl_editor;
    private javax.swing.JPanel pnl_toolbar;
    // End of variables declaration//GEN-END:variables
}
