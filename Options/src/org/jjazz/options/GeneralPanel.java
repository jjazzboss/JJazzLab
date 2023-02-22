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
package org.jjazz.options;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JList;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.PhraseSamples;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.songeditormanager.api.StartupShutdownSongManager;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.LifecycleManager;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

final class GeneralPanel extends javax.swing.JPanel implements PropertyChangeListener
{

    private final GeneralOptionsPanelController controller;

    private static final Logger LOGGER = Logger.getLogger(GeneralPanel.class.getSimpleName());

    GeneralPanel(GeneralOptionsPanelController controller)
    {
        this.controller = controller;


        initComponents();

        cmb_languages.setRenderer(new LocaleCellRenderer());


        // Make button Apply up to date
        cb_disableMouseWheelChangeValue.addActionListener(al -> controller.changed());
        cb_loadLastRecentFile.addActionListener(al -> controller.changed());
        cb_useRhythmFileUserDir.addActionListener(al -> controller.changed());

        // Listen to directory changes
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        fdm.addPropertyChangeListener(this); // RhythmDir changes   

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.log(Level.FINE, "PropertyChangeEvent() evt={0}", evt);   
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        if (evt.getSource() == fdm)
        {
            if (evt.getPropertyName().equals(FileDirectoryManager.PROP_RHYTHM_USER_DIRECTORY)
                    || evt.getPropertyName().equals(FileDirectoryManager.PROP_RHYTHM_MIX_DIRECTORY)
                    || evt.getPropertyName().equals(FileDirectoryManager.PROP_USE_RHYTHM_USER_DIR_FOR_RHYTHM_DEFAULT_MIX))
            {
                updateRhythmMixDirPanel();
            }
        }
    }

    private void changeLanguage(Locale locale)
    {
        String msg = ResUtil.getString(getClass(), "CTL_ConfirmRestartToChangeLanguage");
        NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.OK_CANCEL_OPTION);
        Object result = DialogDisplayer.getDefault().notify(d);
        if (NotifyDescriptor.OK_OPTION == result)
        {
            var uis = GeneralUISettings.getInstance();
            try
            {
                uis.setLocaleUponRestart(locale);
            } catch (IOException ex)
            {
                d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return;
            }

            if (org.openide.util.Utilities.isWindows())
            {
                // For some reason does not work on Linux and Mac (language is not changed, needs a real exit)
                LifecycleManager.getDefault().markForRestart();
            }
            LifecycleManager.getDefault().exit();
        }


    }

    private void updateRhythmMixDirPanel()
    {
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        boolean b = fdm.isUseRhyhtmUserDirAsRhythmDefaultMixDir();
        File f = fdm.getRhythmMixDirectory();
        String s = f.getAbsolutePath();
        tf_defaultRhythmMixDir.setText(s);
        tf_defaultRhythmMixDir.setToolTipText(s);
        cb_useRhythmFileUserDir.setSelected(b);
        tf_defaultRhythmMixDir.setEnabled(!b);
        btn_changeDefaultRhythmMixDir.setEnabled(!b);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        cb_loadLastRecentFile = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        btn_changeDefaultRhythmMixDir = new javax.swing.JButton();
        cb_useRhythmFileUserDir = new javax.swing.JCheckBox();
        tf_defaultRhythmMixDir = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.api.HelpTextArea();
        cb_disableMouseWheelChangeValue = new javax.swing.JCheckBox();
        cmb_languages = new javax.swing.JComboBox<>();
        lbl_language = new javax.swing.JLabel();
        tf_midiEditor = new javax.swing.JTextField();
        btn_changeMidiEditor = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        btn_testMidiEditor = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(cb_loadLastRecentFile, org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.cb_loadLastRecentFile.text")); // NOI18N
        cb_loadLastRecentFile.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                cb_loadLastRecentFileStateChanged(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.jPanel1.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_changeDefaultRhythmMixDir, org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.btn_changeDefaultRhythmMixDir.text")); // NOI18N
        btn_changeDefaultRhythmMixDir.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_changeDefaultRhythmMixDirActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_useRhythmFileUserDir, org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.cb_useRhythmFileUserDir.text")); // NOI18N
        cb_useRhythmFileUserDir.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_useRhythmFileUserDirActionPerformed(evt);
            }
        });

        tf_defaultRhythmMixDir.setEditable(false);
        tf_defaultRhythmMixDir.setFont(tf_defaultRhythmMixDir.getFont().deriveFont(tf_defaultRhythmMixDir.getFont().getSize()-1f));

        jScrollPane1.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(3);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.helpTextArea1.text")); // NOI18N
        jScrollPane1.setViewportView(helpTextArea1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cb_useRhythmFileUserDir)
                            .addComponent(tf_defaultRhythmMixDir, javax.swing.GroupLayout.PREFERRED_SIZE, 297, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_changeDefaultRhythmMixDir)
                        .addGap(0, 141, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cb_useRhythmFileUserDir)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tf_defaultRhythmMixDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_changeDefaultRhythmMixDir))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1))
        );

        org.openide.awt.Mnemonics.setLocalizedText(cb_disableMouseWheelChangeValue, org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.cb_disableMouseWheelChangeValue.text")); // NOI18N
        cb_disableMouseWheelChangeValue.setToolTipText(org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.cb_disableMouseWheelChangeValue.toolTipText")); // NOI18N

        cmb_languages.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cmb_languagesActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_language, org.openide.util.NbBundle.getBundle(GeneralPanel.class).getString("GeneralPanel.lbl_language.text")); // NOI18N

        tf_midiEditor.setFont(tf_midiEditor.getFont().deriveFont(tf_midiEditor.getFont().getSize()-1f));
        tf_midiEditor.setToolTipText(org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.tf_midiEditor.toolTipText")); // NOI18N
        tf_midiEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_midiEditorActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_changeMidiEditor, org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.btn_changeMidiEditor.text")); // NOI18N
        btn_changeMidiEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_changeMidiEditorActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.jLabel1.text")); // NOI18N
        jLabel1.setToolTipText(org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.jLabel1.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_testMidiEditor, org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.btn_testMidiEditor.text")); // NOI18N
        btn_testMidiEditor.setToolTipText(org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.btn_testMidiEditor.toolTipText")); // NOI18N
        btn_testMidiEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_testMidiEditorActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cb_disableMouseWheelChangeValue)
                            .addComponent(cb_loadLastRecentFile)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(cmb_languages, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbl_language)))
                        .addContainerGap(242, Short.MAX_VALUE))
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tf_midiEditor, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_changeMidiEditor)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_testMidiEditor)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmb_languages, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_language))
                .addGap(20, 20, 20)
                .addComponent(cb_loadLastRecentFile)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cb_disableMouseWheelChangeValue)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tf_midiEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_changeMidiEditor)
                    .addComponent(btn_testMidiEditor))
                .addGap(70, 70, 70)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(23, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cb_loadLastRecentFileStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_cb_loadLastRecentFileStateChanged
    {//GEN-HEADEREND:event_cb_loadLastRecentFileStateChanged
        controller.changed();
    }//GEN-LAST:event_cb_loadLastRecentFileStateChanged

    private void btn_changeDefaultRhythmMixDirActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_changeDefaultRhythmMixDirActionPerformed
    {//GEN-HEADEREND:event_btn_changeDefaultRhythmMixDirActionPerformed
        File f = Utilities.showDirChooser(tf_defaultRhythmMixDir.getText(), "Select a directory for rhythm's default mix files");
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        if (f != null)
        {
            fdm.setRhythmMixDirectory(f);
        }
    }//GEN-LAST:event_btn_changeDefaultRhythmMixDirActionPerformed

    private void cb_useRhythmFileUserDirActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_useRhythmFileUserDirActionPerformed
    {//GEN-HEADEREND:event_cb_useRhythmFileUserDirActionPerformed
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        fdm.setUseRhyhtmUserDirAsRhythmDefaultMixDir(cb_useRhythmFileUserDir.isSelected());
    }//GEN-LAST:event_cb_useRhythmFileUserDirActionPerformed

    private void cmb_languagesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cmb_languagesActionPerformed
    {//GEN-HEADEREND:event_cmb_languagesActionPerformed
        controller.changed();
    }//GEN-LAST:event_cmb_languagesActionPerformed

    private void btn_changeMidiEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_changeMidiEditorActionPerformed
    {//GEN-HEADEREND:event_btn_changeMidiEditorActionPerformed
        JFileChooser chooser = org.jjazz.ui.utilities.api.Utilities.getFileChooserInstance();
        chooser.resetChoosableFileFilters();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle(ResUtil.getString(getClass(), "GeneralPanel.ChooseMidiEditor"));
        chooser.setCurrentDirectory(null);

        // Show dialog
        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION)
        {
            File file = chooser.getSelectedFile();
            if (file != null)
            {
                tf_midiEditor.setText(file.getAbsolutePath());
                controller.changed();
            }
        }


    }//GEN-LAST:event_btn_changeMidiEditorActionPerformed

    private void btn_testMidiEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_testMidiEditorActionPerformed
    {//GEN-HEADEREND:event_btn_testMidiEditorActionPerformed
        File midiTempFile;
        try
        {
            midiTempFile = File.createTempFile("jjazzMidiTempFile", ".mid");
        } catch (IOException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Create dummy sequence        
        Sequence sequence;
        try
        {
            sequence = new Sequence(Sequence.PPQ, MidiConst.PPQ_RESOLUTION);

            Track track = sequence.createTrack();
            Phrase p = PhraseSamples.getBasicDrumPhrase(0, 4, TimeSignature.FOUR_FOUR, 9);
            Phrases.fillTrack(p, track);
            MidiUtilities.addTrackNameEvent(track, "Drums track");


            track = sequence.createTrack();
            p = PhraseSamples.getCscalePhrase(10, 0, 16);
            Phrases.fillTrack(p, track);
            MidiUtilities.addTrackNameEvent(track, "Bass track");


            // Write the file
            MidiSystem.write(sequence, 1, midiTempFile);


        } catch (InvalidMidiDataException | IOException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        var jms = JJazzMidiSystem.getInstance();
        File saveExternalEditor = jms.getExternalMidiEditor();
        try
        {
            jms.setExternalMidiEditor(new File(tf_midiEditor.getText().trim()));
            jms.editMidiFileWithExternalEditor(midiTempFile);
        } catch (IOException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        } finally
        {
            jms.setExternalMidiEditor(saveExternalEditor);
        }


    }//GEN-LAST:event_btn_testMidiEditorActionPerformed

    private void tf_midiEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_midiEditorActionPerformed
    {//GEN-HEADEREND:event_tf_midiEditorActionPerformed
        controller.changed();
    }//GEN-LAST:event_tf_midiEditorActionPerformed

    void load()
    {
        // TODO read settings and initialize GUI
        // Example:        
        // someCheckBox.setSelected(Preferences.userNodeForPackage(GeneralPanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(GeneralPanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());

        LOGGER.fine("load() --");
        cb_loadLastRecentFile.setSelected(StartupShutdownSongManager.getInstance().isOpenRecentFilesUponStartup());
        cb_disableMouseWheelChangeValue.setSelected(!GeneralUISettings.getInstance().isChangeValueWithMouseWheelEnabled());


        // Rhythm Mix directory
        updateRhythmMixDirPanel();


        // External Midi editor
        File editorFile = JJazzMidiSystem.getInstance().getExternalMidiEditor();
        tf_midiEditor.setText(editorFile.getName().isBlank() ? "" : editorFile.getAbsolutePath());


        // Language combo
        var cmbModel = new DefaultComboBoxModel<Locale>(GeneralUISettings.SUPPORTED_LOCALES);
        cmb_languages.setModel(cmbModel);
        cmb_languages.setSelectedItem(Locale.getDefault());


    }

    void store()
    {
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(GeneralPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(GeneralPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());
        LOGGER.fine("store() --");
        StartupShutdownSongManager.getInstance().setOpenRecentFilesUponStartup(cb_loadLastRecentFile.isSelected());

        GeneralUISettings.getInstance().setChangeValueWithMouseWheelEnabled(!cb_disableMouseWheelChangeValue.isSelected());

        Analytics.setProperties(Analytics.buildMap("Mouse Wheel Value Change Support", !cb_disableMouseWheelChangeValue.isSelected()));

        JJazzMidiSystem.getInstance().setExternalMidiEditor(new File(tf_midiEditor.getText().trim()));

        Locale locale = (Locale) cmb_languages.getSelectedItem();
        Locale defLocale = Locale.getDefault();
        if (!locale.equals(defLocale))
        {
            changeLanguage(locale);
        }


    }

    boolean valid()
    {
        // TODO check whether form is consistent and complete
        return true;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_changeDefaultRhythmMixDir;
    private javax.swing.JButton btn_changeMidiEditor;
    private javax.swing.JButton btn_testMidiEditor;
    private javax.swing.JCheckBox cb_disableMouseWheelChangeValue;
    private javax.swing.JCheckBox cb_loadLastRecentFile;
    private javax.swing.JCheckBox cb_useRhythmFileUserDir;
    private javax.swing.JComboBox<Locale> cmb_languages;
    private org.jjazz.ui.utilities.api.HelpTextArea helpTextArea1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_language;
    private javax.swing.JTextField tf_defaultRhythmMixDir;
    private javax.swing.JTextField tf_midiEditor;
    // End of variables declaration//GEN-END:variables

    // ========================================================================================================
    // Private classes
    // ========================================================================================================
    private static class LocaleCellRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); // returned component=this
            if (value instanceof Locale)
            {
                Locale locale = (Locale) value;
                setText(locale.getDisplayLanguage(Locale.ENGLISH));
            }
            return this;
        }
    }

}
