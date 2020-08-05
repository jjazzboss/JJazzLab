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

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import org.jjazz.song.api.Song;
import org.jjazz.songmemoviewer.api.SongMemoEditorSettings;
import org.jjazz.songmemoviewer.api.SongMemoTopComponent;
import org.jjazz.undomanager.JJazzUndoManager;
import org.openide.awt.UndoRedo;
import org.openide.util.Exceptions;
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
    private SongMemoEditorSettings settings;
    /**
     * Save the JTextArea context for each song: a document and its UndoManager wrapped into an Undoer
     */
    private final HashMap<Song, Undoer> mapSongUndoer = new HashMap<>();
    /**
     * Special Undoer when songModel is null.
     */
    private Undoer emptyUndoer = new Undoer("", this);
    private static final Logger LOGGER = Logger.getLogger(SongMemoEditor.class.getSimpleName());

    /**
     * Creates new form SongNotesEditor
     */
    public SongMemoEditor()
    {
        initComponents();


        // UI Settings
        settings = SongMemoEditorSettings.getDefault();
        settings.addPropertyChangeListener(this);
        uiSettingsChanged();


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

    /**
     * Use a local UndoManager per song.
     *
     * @return
     */
    public UndoRedo getUndoManager()
    {
        return songModel == null ? null : mapSongUndoer.get(songModel).getUndoManager();
    }
    
    public Lookup getLookup()
    {
        return this.lookup;
    }

    // ==================================================================================
    // PropertyChangeListener interface
    // ==================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == songModel)
        {
            if (evt.getPropertyName().equals(Song.PROP_COMMENTS))
            {                
                Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (c == txt_notes)
                {
                    // If we have the focus, user is typing and we generated the change event via updateModel(), nothing to do
                } else
                {
                    // An external component has changed the comments
                    var undoer = mapSongUndoer.get(songModel);
                    String txt = (String) evt.getNewValue();
                    undoer.setTextSilently(txt);    // This will not trigger a document change event
                }
            } else if (evt.getPropertyName().equals(Song.PROP_CLOSED))
            {
                setEditorEnabled(false);
                mapSongUndoer.remove(songModel);   // We don't need this UndoManager anymore                
                resetModel();
                
            } else if (evt.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED)
                    && evt.getNewValue() == Boolean.FALSE)
            {
                songNameChanged();
                
            }
        } else if (evt.getSource() == settings)
        {
            uiSettingsChanged();
        }
        
    }


    // ==================================================================================
    // DocumentListener interface
    // ==================================================================================

    // Methods called only when user has changed the text
    
    @Override
    public void insertUpdate(DocumentEvent e)
    {
        updateModel();
    }
    
    @Override
    public void removeUpdate(DocumentEvent e)
    {
        updateModel();
    }
    
    @Override
    public void changedUpdate(DocumentEvent e)
    {
        updateModel();
    }

    // ==================================================================================
    // Private methods
    // ==================================================================================

    
    private void songNameChanged()
    {
        // Update memo name
        lbl_songName.setText(songModel == null ? "-" : songModel.getName());
        if (songModel != null)
        {
            lbl_songName.setToolTipText(songModel.getFile() == null ? null : songModel.getFile().getAbsolutePath());
        }
    }
    
    /**
     * User has typed something.
     */
    private void updateModel()
    {
        if (songModel != null)
        {
            String txt = txt_notes.getText();
            songModel.setComments(txt);
            
            // Make sure our TopComponent is active 
            // Needed because when cursor is in the memo and you click another song tab
            // the cursor remains in memo and if directly typing, the SongMemoTopComponent being not active, then the Undo/Redo 
            // buttons are not updated.
            SongMemoTopComponent.getInstance().requestActive();
        }
    }
    
    
    private void setEditorEnabled(boolean b)
    {
        txt_notes.setBackground(b ? settings.getBackgroundColor() : null);
        org.jjazz.ui.utilities.Utilities.setRecursiveEnabled(b, this);
    }
    
    private void resetModel()
    {
        if (songModel != null)
        {
            instanceContent.remove(songModel);
            songModel.removePropertyChangeListener(this);
            songModel = null;
        }
        
        // Install the empty Undoer
        emptyUndoer.install(txt_notes);
        
        // Update UI
        songNameChanged();
    }

    /**
     * Called when SongStructure presence changed in the lookup.
     * <p>
     * If a new song is detected, listen to the SS_Editor lookup selection changes.
     */
    private void songPresenceChanged()
    {
        Song song = Utilities.actionsGlobalContext().lookup(Song.class);
        LOGGER.log(Level.FINE, "songPresenceChanged() -- song=" + song);
        
        if (song == songModel || song == null)
        {
            // Do nothing
            return;
        }
        
        
        resetModel();
        
        
        songModel = song;
        songModel.addPropertyChangeListener(this);
        setEditorEnabled(true);


        // Make sure we have an Undoer for this song and install it
        Undoer undoer = mapSongUndoer.get(songModel);
        if (undoer == null)
        {
            undoer = new Undoer(songModel.getComments(), this);
            mapSongUndoer.put(songModel, undoer);
        }
        undoer.install(txt_notes);
        

        // Update UI
        songNameChanged();
        

        // Last
        instanceContent.add(songModel);
        
    }
    
    private void uiSettingsChanged()
    {
        txt_notes.setForeground(settings.getFontColor());
        txt_notes.setFont(settings.getFont());
        setEditorEnabled(isEnabled());  // This will update background if enabled
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
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
        txt_notes.setDragEnabled(true);
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

    
    private class Undoer implements UndoableEditListener
    {
        
        private JJazzUndoManager um;
        private Document doc;
        private DocumentListener listener;
        
        public Undoer(String txt, DocumentListener docListener)
        {
            um = new JJazzUndoManager();
            doc = createDocument();
            listener = docListener;
            try
            {
                doc.insertString(0, txt, null);
            } catch (BadLocationException ex)
            {
                // Should never happen
                Exceptions.printStackTrace(ex);
            }
            doc.addUndoableEditListener(this);
            doc.addDocumentListener(listener);
        }

        /**
         * Update the text of this document without firing document change events
         * <p>
         * Note that UndoableEvents are generated.
         *
         * @param txt
         */
        public void setTextSilently(String txt)
        {
            doc.removeDocumentListener(listener);
            try
            {
                doc.remove(0, doc.getLength());
                doc.insertString(0, txt, null);
            } catch (BadLocationException ex)
            {
                Exceptions.printStackTrace(ex);
            } finally
            {
                doc.addDocumentListener(listener);
            }
        }
        
        public JJazzUndoManager getUndoManager()
        {
            return um;
        }
        
        public void undo()
        {
            um.undo();
        }
        
        public void undoOrRedo()
        {
            um.undoOrRedo();
        }
        
        protected Document createDocument()
        {
            return new PlainDocument();
        }
        
        public void install(JTextComponent comp)
        {
            comp.setDocument(doc);
        }
        
        @Override
        public void undoableEditHappened(UndoableEditEvent e)
        {
            um.addEdit(e.getEdit());
        }
        
    }
}
