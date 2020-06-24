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
package org.jjazz.songnotesviewer;

import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.UIManager;
import org.jjazz.song.api.Song;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * Text editor component for the song notes.
 */
public class SongNotesEditor extends javax.swing.JPanel implements PropertyChangeListener// , DocumentListener
{

    private final Lookup.Result<Song> songLkpResult;
    private LookupListener songLkpListener;
    private final Lookup lookup;
    private final InstanceContent instanceContent;
    private Song songModel;
    private static final Logger LOGGER = Logger.getLogger(SongNotesEditor.class.getSimpleName());

    /**
     * Creates new form SongNotesEditor
     */
    public SongNotesEditor()
    {
        initComponents();

        
        // Save changes upon focus lost
        txt_notes.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                updateModel();
            }
        });

        
        // Our general lookup : store our action map and the edited song 
        instanceContent = new InstanceContent();
        instanceContent.add(getActionMap());
        lookup = new AbstractLookup(instanceContent);

        
        // Listen to Song presence in the global context    
        songLkpListener = le -> songPresenceChanged();
        Lookup context = Utilities.actionsGlobalContext();
        songLkpResult = context.lookupResult(Song.class);
        songLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, songLkpListener, songLkpResult));

        
        songPresenceChanged();
    }

    public void setModel(Song song)
    {
        if (songModel != null)
        {
            songModel.removePropertyChangeListener(this);
        }

        songModel = song;
        songModel.addPropertyChangeListener(this);

    }

    public UndoRedo getUndoManager()
    {
        return songModel == null ? null : JJazzUndoManagerFinder.getDefault().get(songModel);
    }

    public Lookup getLookup()
    {
        return this.lookup;
    }

    public void cleanup()
    {
        if (songModel != null)
        {
            resetModel();
        }
        songLkpListener = null;
    }

    // ==================================================================================
    // PropertyChangeListener interface
    // ==================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(Song.PROP_COMMENTS))
        {
            update();
            
        } else if (evt.getPropertyName().equals(Song.PROP_CLOSED))
        {
            txt_notes.setEnabled(false);
            if (songModel != null)
            {
                resetModel();
                update();
            }
        }
    }
    // ==================================================================================
    // DocumentListener interface
    // ==================================================================================
//
//    public void insertUpdate(DocumentEvent e)
//    {
//        displayEditInfo(e);
//    }
//
//    public void removeUpdate(DocumentEvent e)
//    {
//        displayEditInfo(e);
//    }
//
//    public void changedUpdate(DocumentEvent e)
//    {
//        displayEditInfo(e);
//    }

    // ==================================================================================
    // Private methods
    // ==================================================================================
    private void update()
    {
        txt_notes.setText(songModel == null ? "" : songModel.getComments());
    }

    private void updateModel()
    {
        LOGGER.fine("updateModel() songModel=" + songModel);
        if (songModel != null)
        {
            songModel.setComments(txt_notes.getText());
        }
    }

    private void resetModel()
    {
        instanceContent.remove(songModel);
        songModel.removePropertyChangeListener(this);
        songModel = null;
    }

    /**
     * Called when SongStructure presence changed in the lookup.
     * <p>
     * If a new song is detected, listen to the SS_Editor lookup selection changes.
     */
    private void songPresenceChanged()
    {
        LOGGER.log(Level.FINE, "songPresenceChanged()");
        Song song = Utilities.actionsGlobalContext().lookup(Song.class);
        if (song == songModel || song == null)
        {
            // Do nothing
            return;
        }

        if (songModel != null)
        {
            resetModel();
        }

        songModel = song;
        songModel.addPropertyChangeListener(this);
        instanceContent.add(songModel);
        txt_notes.setEnabled(true);
        
        update();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jScrollPane2 = new javax.swing.JScrollPane();
        txt_notes = new org.jjazz.ui.utilities.JTextAreaNoKeyBinding();

        txt_notes.setColumns(20);
        txt_notes.setRows(5);
        txt_notes.setFont(UIManager.getFont("label.font")
        );
        jScrollPane2.setViewportView(txt_notes);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 282, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane2;
    private org.jjazz.ui.utilities.JTextAreaNoKeyBinding txt_notes;
    // End of variables declaration//GEN-END:variables

    private class NoAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            //do nothing
        }
    }

}
