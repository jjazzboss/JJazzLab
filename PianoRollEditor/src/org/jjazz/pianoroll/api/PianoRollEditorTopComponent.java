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
import java.util.Collections;
import java.util.List;
import javax.swing.Action;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.PianoRollEditorImpl;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
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

    public static final String MODE = "editor"; // see Netbeans WindowManager modes
    private PianoRollEditorImpl editor;

    public PianoRollEditorTopComponent(String tabName, SizedPhrase spModel, DrumKit.KeyMap keyMap, int startBarIndex)
    {
        initComponents();

        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
        // putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        // putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);
        // putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);        

        editor = new PianoRollEditorImpl(startBarIndex, spModel, keyMap, PianoRollEditorSettings.getDefault());
        add(editor);

        setDisplayName(tabName);

    }

    public PianoRollEditorImpl getEditor()
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
        // TODO add custom code on component closing
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

        setToolTipText(org.openide.util.NbBundle.getMessage(PianoRollEditorTopComponent.class, "PianoRollEditorTopComponent.toolTipText")); // NOI18N
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
