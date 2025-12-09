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
package org.jjazz.songmemoviewer;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import org.jjazz.song.api.Song;
import org.jjazz.songmemoviewer.api.SongMemoEditorSettings;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.UndoRedo;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.WindowManager;

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
    private final HashMap<Song, UndoSupport> mapSongUndoer = new HashMap<>();
    /**
     * Special Undoer when songModel is null.
     */
    private UndoSupport emptyUndoer = new UndoSupport("UM-Empty", "", this);
    private static final Logger LOGGER = Logger.getLogger(SongMemoEditor.class.getSimpleName());

    /**
     * Creates new form SongNotesEditor
     */
    public SongMemoEditor()
    {
        initComponents();


        org.jjazz.uiutilities.api.UIUtilities.installPrintableAsciiKeyTrap(txt_notes);


        // UI Settings
        settings = SongMemoEditorSettings.getDefault();
        settings.addPropertyChangeListener(this);
        uiSettingsChanged();


        // Our general lookup : store our action map and the edited song 
        instanceContent = new InstanceContent();
        instanceContent.add(getActionMap());
        lookup = new AbstractLookup(instanceContent);


        // Listen to Song presence in the global context    
        songLkpListener = le -> songPresenceChanged(le);
        Lookup context = Utilities.actionsGlobalContext();
        songLkpResult = context.lookupResult(Song.class);
        songLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, songLkpListener, songLkpResult));


        // Disabled by default
        setEditorEnabled(false);

        songPresenceChanged(null);
    }

    /**
     * Use a local UndoManager per song.
     *
     * @return
     */
    public UndoRedo getUndoManager()
    {
        UndoRedo res = songModel == null ? null : mapSongUndoer.get(songModel).getUndoManager();
        LOGGER.log(Level.FINE, "getUndoManager() songModel={0} UndoRedo={1}", new Object[]
        {
            songModel, res
        });
        return res;
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
                JJazzUndoManager um = ((JJazzUndoManager) getUndoManager());
                if (c == txt_notes)
                {
                    // If we have the focus, user is typing and we generated the change event via updateModel(), nothing to do

                } else if (um != null && um.isUndoRedoInProgress())
                {
                    // We're in the middle of an undo/redo operation, do nothing
                } else
                {
                    // An external component has changed the comments
                    var undoer = mapSongUndoer.get(songModel);
                    String txt = (String) evt.getNewValue();
                    LOGGER.log(Level.FINE, "propertyChange() an external component has changed the text to: {0}", txt);
                    undoer.setTextSilently(txt);    // This will not trigger a document change event
                }
            } else if (evt.getPropertyName().equals(Song.PROP_CLOSED))
            {
                setEditorEnabled(false);
                mapSongUndoer.remove(songModel);   // We don't need this UndoManager anymore                
                resetModel();

            } else if (evt.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED_OR_RESET)
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
     * Text has been changed in the JTextArea (user typing or undo)
     */
    private void updateModel()
    {
        if (songModel != null)
        {
            String txt = txt_notes.getText();
            songModel.setComments(txt);
        }
    }

    private void setEditorEnabled(boolean b)
    {
        txt_notes.setBackground(b ? settings.getBackgroundColor() : null);
        org.jjazz.uiutilities.api.UIUtilities.setRecursiveEnabled(b, this);
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
     *
     * @param le If null search the global Lookup for a song
     */
    private void songPresenceChanged(LookupEvent le)
    {
        Song song;
        if (le != null)
        {
            @SuppressWarnings("unchecked")
            Lookup.Result<Song> leRes = (Lookup.Result<Song>) le.getSource();
            var songs = leRes.allInstances();
            song = songs.isEmpty() ? null : songs.iterator().next();

        } else
        {
            song = Utilities.actionsGlobalContext().lookup(Song.class);
        }
        LOGGER.log(Level.FINE, "songPresenceChanged() -- song={0}", song);

        if (song == songModel || song == null)
        {
            // Do nothing
            return;
        }

        // There is a new (non-null) song

        resetModel();


        songModel = song;
        songModel.addPropertyChangeListener(this);
        setEditorEnabled(true);


        // Make sure we have an Undoer for this song and install it
        UndoSupport undoer = mapSongUndoer.get(songModel);
        if (undoer == null)
        {
            undoer = new UndoSupport("UM-" + songModel.getName(), songModel.getComments(), this);
            mapSongUndoer.put(songModel, undoer);
        }
        undoer.install(txt_notes);


        // Make sure the Undo/Redo buttons enabled state is updated: active CL/SS_Editor TopComponents may change but if focus remains
        // in our JTextArea, Undo/Redo buttons are not updated        
        var fm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (fm.getFocusOwner() == txt_notes)
        {
            fm.clearGlobalFocusOwner();
        }


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
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jScrollPane1 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.flatcomponents.api.HelpTextArea();
        lbl_songName = new javax.swing.JLabel();
        btn_insertLink = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        txt_notes = new javax.swing.JTextArea();
        btn_openLinks = new javax.swing.JButton();

        jScrollPane1.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(4);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(SongMemoEditor.class, "SongMemoEditor.helpTextArea1.text")); // NOI18N
        jScrollPane1.setViewportView(helpTextArea1);

        lbl_songName.setFont(lbl_songName.getFont().deriveFont(lbl_songName.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_songName, "-"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_insertLink, org.openide.util.NbBundle.getBundle(SongMemoEditor.class).getString("SongMemoEditor.btn_insertLink.text")); // NOI18N
        btn_insertLink.setToolTipText(org.openide.util.NbBundle.getBundle(SongMemoEditor.class).getString("SongMemoEditor.btn_insertLink.toolTipText")); // NOI18N
        btn_insertLink.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_insertLinkActionPerformed(evt);
            }
        });

        txt_notes.setColumns(20);
        txt_notes.setRows(5);
        jScrollPane3.setViewportView(txt_notes);

        org.openide.awt.Mnemonics.setLocalizedText(btn_openLinks, org.openide.util.NbBundle.getBundle(SongMemoEditor.class).getString("SongMemoEditor.btn_openLinks.text")); // NOI18N
        btn_openLinks.setToolTipText(org.openide.util.NbBundle.getBundle(SongMemoEditor.class).getString("SongMemoEditor.btn_openLinks.toolTipText")); // NOI18N
        btn_openLinks.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_openLinksActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_insertLink, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(btn_openLinks)
                                .addGap(0, 0, 0))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_songName)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_songName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 307, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addComponent(btn_insertLink)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_openLinks)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btn_insertLinkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_insertLinkActionPerformed
    {//GEN-HEADEREND:event_btn_insertLinkActionPerformed
        var dlg = new InsertLinkDialog(WindowManager.getDefault().getMainWindow(), true);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
        URL url = dlg.getLink();
        if (url != null)
        {
            String str = songModel.getComments() + "\n" + dlg.getLink().toString() + " ";
            songModel.setComments(str);
        }

    }//GEN-LAST:event_btn_insertLinkActionPerformed

    private void btn_openLinksActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_openLinksActionPerformed
    {//GEN-HEADEREND:event_btn_openLinksActionPerformed
        var uris = org.jjazz.utilities.api.Utilities.extractURIs(songModel.getComments(), "https?", "file");
        if (uris.isEmpty())
        {
            String msg = ResUtil.getString(getClass(), "SongMemoEditor.ERR_NoLinksToOpen");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }

        LOGGER.log(Level.INFO, "btn_openLinksActionPerformed() Opening links");
        new Thread(() -> uris
                .stream()
                .limit(4)    // security
                .forEach(uri -> org.jjazz.utilities.api.Utilities.systemOpenURI(uri)))
                .start();

    }//GEN-LAST:event_btn_openLinksActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_insertLink;
    private javax.swing.JButton btn_openLinks;
    private org.jjazz.flatcomponents.api.HelpTextArea helpTextArea1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lbl_songName;
    private javax.swing.JTextArea txt_notes;
    // End of variables declaration//GEN-END:variables

    /**
     * Need to change the Document for undo/redo to work with multiple UndoManagers.
     */
    private class UndoSupport
    {

        private JJazzUndoManager um;
        private Document doc;
        private DocumentListener listener;

        public UndoSupport(String umName, String txt, DocumentListener docListener)
        {
            um = new JJazzUndoManager(umName);
            doc = new PlainDocument();
            listener = docListener;
            try
            {
                doc.insertString(0, txt, null);
            } catch (BadLocationException ex)
            {
                // Should never happen
                Exceptions.printStackTrace(ex);
            }
            doc.addUndoableEditListener(um);
            doc.addDocumentListener(listener);
        }

        /**
         * Update the text of this document without firing any event.
         * <p>
         *
         * @param txt
         */
        public void setTextSilently(String txt)
        {
            doc.removeDocumentListener(listener);
            doc.removeUndoableEditListener(um);
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
                doc.addUndoableEditListener(um);
            }
        }

        public JJazzUndoManager getUndoManager()
        {
            return um;
        }

        public void install(JTextComponent comp)
        {
            comp.setDocument(doc);
        }

    }
}
