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
package org.jjazz.songmemoviewer;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jjazz.song.api.Song;
import org.jjazz.songmemoviewer.api.SongMemoTopComponent;
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
public class SongMemoEditor extends javax.swing.JPanel implements PropertyChangeListener, DocumentListener
{

    private final Lookup.Result<Song> songLkpResult;
    private LookupListener songLkpListener;
    private final Lookup lookup;
    private final InstanceContent instanceContent;
    private Song songModel;
    private static final Logger LOGGER = Logger.getLogger(SongMemoEditor.class.getSimpleName());

    /**
     * Creates new form SongNotesEditor
     */
    public SongMemoEditor()
    {
        initComponents();

        // Listen to changes
        txt_notes.getDocument().addDocumentListener(this);

        // Our general lookup : store our action map and the edited song 
        instanceContent = new InstanceContent();
        instanceContent.add(getActionMap());
        lookup = new AbstractLookup(instanceContent);

        // Listen to Song presence in the global context    
        songLkpListener = le -> songPresenceChanged();
        Lookup context = Utilities.actionsGlobalContext();
        songLkpResult = context.lookupResult(Song.class);
        songLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, songLkpListener, songLkpResult));

        // Disabled by default
        setEditorEnabled(false);

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
            // No need to update if we have the focus: user is typing
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != txt_notes)
            {
                updateText();
                LOGGER.severe("updated externally");
            } else
            {
                LOGGER.severe("updated internal=>nothing");
            }
        } else if (evt.getPropertyName().equals(Song.PROP_CLOSED))
        {
            setEditorEnabled(false);
            if (songModel != null)
            {
                resetModel();
                updateText();
            }
        } else if (evt.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED))
        {
            updateTabName();
        }
    }
    // ==================================================================================
    // DocumentListener interface
    // ==================================================================================

    public void insertUpdate(DocumentEvent e)
    {
        updateModel();
    }

    public void removeUpdate(DocumentEvent e)
    {
        updateModel();
    }

    public void changedUpdate(DocumentEvent e)
    {
        updateModel();
    }

    // ==================================================================================
    // Private methods
    // ==================================================================================
    private void updateText()
    {
        // Don't trigger change event since it's not the user who is typing
        txt_notes.getDocument().removeDocumentListener(this);
        txt_notes.setText(songModel == null ? "" : songModel.getComments());
        txt_notes.getDocument().addDocumentListener(this);
    }

    private void updateModel()
    {
        if (songModel != null)
        {
            songModel.setComments(txt_notes.getText());
        }
    }

    /**
     * Update the TopComponent tab name with song name.
     * <p>
     */
    private void updateTabName()
    {
        lbl_songName.setText(songModel == null ? "-" : songModel.getName());
        lbl_songName.setToolTipText(songModel.getFile() == null ? null : songModel.getFile().getAbsolutePath());
//        SongMemoTopComponent tc = SongMemoTopComponent.getInstance();
//        if (tc != null)
//        {
//            String tabName;
//            if (songModel != null)
//            {
//                tabName = "Memo " + org.jjazz.util.Utilities.truncateWithDots(songModel.getName(), 10);
//            } else
//            {
//                tabName = "Song Memo";
//            }
//            tc.setDisplayName(tabName);
//        }
    }

    private void setEditorEnabled(boolean b)
    {
        txt_notes.setBackground(b ? UIManager.getColor("TextArea.background") : null);
        org.jjazz.ui.utilities.Utilities.setRecursiveEnabled(b, this);
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
        setEditorEnabled(true);

        updateText();

        updateTabName();
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
        jScrollPane1 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.HelpTextArea();
        lbl_songName = new javax.swing.JLabel();

        txt_notes.setColumns(20);
        txt_notes.setRows(5);
        txt_notes.setFont(UIManager.getFont("label.font")
        );
        jScrollPane2.setViewportView(txt_notes);

        jScrollPane1.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(2);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(SongMemoEditor.class, "SongMemoEditor.helpTextArea1.text")); // NOI18N
        jScrollPane1.setViewportView(helpTextArea1);

        lbl_songName.setFont(lbl_songName.getFont().deriveFont(lbl_songName.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_songName, org.openide.util.NbBundle.getMessage(SongMemoEditor.class, "SongMemoEditor.lbl_songName.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_songName)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jScrollPane1)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_songName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lbl_songName;
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
