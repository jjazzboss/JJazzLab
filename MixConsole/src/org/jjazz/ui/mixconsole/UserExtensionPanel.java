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
package org.jjazz.ui.mixconsole;

import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.JTextField;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.songstructure.api.event.SptResizedEvent;
import org.jjazz.ui.flatcomponents.api.FlatTextEditDialog;
import org.jjazz.ui.utilities.api.MidiFileDragInTransferHandler;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.Utilities;

/**
 * An extension of a MixChannelPanel to add specific controls for user phrase channels.
 * <p>
 */
public class UserExtensionPanel extends javax.swing.JPanel implements VetoableChangeListener, SgsChangeListener
{

    private UserRhythmVoice userRhythmVoice;
    private Song song;
    private MidiMix midiMix;
    private final Font FONT = GeneralUISettings.getInstance().getStdCondensedFont();
    private UserExtensionPanelController controller;
    private static final Logger LOGGER = Logger.getLogger(UserExtensionPanel.class.getSimpleName());

    public UserExtensionPanel()
    {
        this(null, null, null, null);
    }

    public UserExtensionPanel(Song song, MidiMix midiMix, UserRhythmVoice urv, UserExtensionPanelController controller)
    {
        initComponents();

        this.controller = controller;
        this.controller.setUserExtentionPanel(this);
        this.song = song;
        this.song.addVetoableChangeListener(this);
        this.song.getSongStructure().addSgsChangeListener(this);
        this.midiMix = midiMix;
        userRhythmVoice = urv;
        if (urv != null)
        {
            String s = Utilities.truncateWithDots(userRhythmVoice.getName(), 8);
            fbtn_name.setText(s);
        }

        // By default enable the drag in transfer handler
        setTransferHandler(new MidiFileDragInTransferHandlerImpl());


        phraseUpdated();
    }

    public UserRhythmVoice getUserRhythmVoice()
    {
        return userRhythmVoice;
    }

    public Song getSong()
    {
        return song;
    }

    public MidiMix getMidiMix()
    {
        return midiMix;
    }

    public void cleanup()
    {
        song.removeVetoableChangeListener(this);
        song.getSongStructure().removeSgsChangeListener(this);
    }

    //-----------------------------------------------------------------------
    // Implementation of the VetoableListener interface
    //-----------------------------------------------------------------------
    @Override
    public void vetoableChange(PropertyChangeEvent e)
    {
        if (e.getSource() == song)
        {
            if (e.getPropertyName().equals(Song.PROP_VETOABLE_USER_PHRASE))
            {
                // If it's a first new phrase for name : should never happen here, this panel was created on this event already
                // If it's a removed phrase: this panel will be removed
                // => Do nothing
            } else if (e.getPropertyName().equals(Song.PROP_VETOABLE_USER_PHRASE_CONTENT))
            {
                phraseUpdated();
            }
        }

    }

    //-----------------------------------------------------------------------
    // SgsChangeListener interface
    //-----------------------------------------------------------------------
    @Override
    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
    {
        // Do nothing
    }

    @Override
    public void songStructureChanged(SgsChangeEvent e)
    {
        if (e instanceof SptResizedEvent
                || e instanceof SptAddedEvent
                || e instanceof SptRemovedEvent
                || e instanceof SptReplacedEvent
                )
        {
            // Song size in beats is impacted, so is our birdViewComponent
            phraseUpdated();
        }
    }

    //-----------------------------------------------------------------------
    // Private methods
    //-----------------------------------------------------------------------
    private void phraseUpdated()
    {
        LOGGER.fine("phraseUpdated() --");
        FloatRange beatRange = song.getSongStructure().getBeatRange(null);
        TimeSignature ts = song.getSongStructure().getSongPart(0).getRhythm().getTimeSignature();
        birdViewComp.setModel(song.getUserPhrase(userRhythmVoice.getName()), ts, beatRange);
    }


    //-----------------------------------------------------------------------
    // Private classes
    //---------------------------------------------------------------------
    private class MidiFileDragInTransferHandlerImpl extends MidiFileDragInTransferHandler
    {

        @Override
        protected boolean isImportEnabled()
        {
            return isEnabled();
        }

        @Override
        protected boolean importMidiFile(File midiFile)
        {
            return controller.midiFileDraggedIn(midiFile);
        }

    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        phraseBirdView1 = new org.jjazz.phrase.api.ui.PhraseBirdView();
        roundedPanel1 = new org.jjazz.ui.flatcomponents.api.RoundedPanel();
        fbtn_name = new org.jjazz.ui.flatcomponents.api.FlatButton();
        jPanel2 = new javax.swing.JPanel();
        pnl_help = new javax.swing.JPanel();
        fbtn_help = new org.jjazz.ui.flatcomponents.api.FlatHelpButton();
        pnl_edit = new javax.swing.JPanel();
        fbtn_edit = new org.jjazz.ui.flatcomponents.api.FlatButton();
        birdViewComp = new org.jjazz.phrase.api.ui.PhraseBirdView();
        pnl_close = new javax.swing.JPanel();
        fbtn_remove = new org.jjazz.ui.flatcomponents.api.FlatButton();

        javax.swing.GroupLayout phraseBirdView1Layout = new javax.swing.GroupLayout(phraseBirdView1);
        phraseBirdView1.setLayout(phraseBirdView1Layout);
        phraseBirdView1Layout.setHorizontalGroup(
            phraseBirdView1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 29, Short.MAX_VALUE)
        );
        phraseBirdView1Layout.setVerticalGroup(
            phraseBirdView1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 50, Short.MAX_VALUE)
        );

        setOpaque(false);
        setLayout(new java.awt.CardLayout());

        roundedPanel1.setBackground(new java.awt.Color(46, 46, 46));
        roundedPanel1.setArcDiameter(20);

        fbtn_name.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_name, "Theme"); // NOI18N
        fbtn_name.setToolTipText(org.openide.util.NbBundle.getMessage(UserExtensionPanel.class, "UserExtensionPanel.fbtn_name.toolTipText")); // NOI18N
        fbtn_name.setFont(FONT.deriveFont(10f));
        fbtn_name.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_nameActionPerformed(evt);
            }
        });

        jPanel2.setMinimumSize(new java.awt.Dimension(10, 36));
        jPanel2.setOpaque(false);
        jPanel2.setLayout(new java.awt.BorderLayout());

        pnl_help.setOpaque(false);
        pnl_help.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 3, 3));

        fbtn_help.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        fbtn_help.setHelpText(org.openide.util.NbBundle.getMessage(UserExtensionPanel.class, "UserExtensionPanel.fbtn_help.helpText")); // NOI18N
        pnl_help.add(fbtn_help);

        jPanel2.add(pnl_help, java.awt.BorderLayout.EAST);

        pnl_edit.setOpaque(false);
        pnl_edit.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 3, 3));

        fbtn_edit.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        fbtn_edit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/Edit-16x16.png"))); // NOI18N
        fbtn_edit.setToolTipText(org.openide.util.NbBundle.getMessage(UserExtensionPanel.class, "UserExtensionPanel.fbtn_edit.toolTipText")); // NOI18N
        fbtn_edit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_editActionPerformed(evt);
            }
        });
        pnl_edit.add(fbtn_edit);

        jPanel2.add(pnl_edit, java.awt.BorderLayout.WEST);

        birdViewComp.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(61, 61, 61)));
        birdViewComp.setForeground(new java.awt.Color(192, 115, 242));
        birdViewComp.setToolTipText(org.openide.util.NbBundle.getMessage(UserExtensionPanel.class, "UserExtensionPanel.birdViewComp.toolTipText")); // NOI18N
        birdViewComp.setOpaque(false);

        javax.swing.GroupLayout birdViewCompLayout = new javax.swing.GroupLayout(birdViewComp);
        birdViewComp.setLayout(birdViewCompLayout);
        birdViewCompLayout.setHorizontalGroup(
            birdViewCompLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        birdViewCompLayout.setVerticalGroup(
            birdViewCompLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );

        pnl_close.setOpaque(false);

        fbtn_remove.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        fbtn_remove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/Close-10x10.png"))); // NOI18N
        fbtn_remove.setToolTipText(org.openide.util.NbBundle.getMessage(UserExtensionPanel.class, "UserExtensionPanel.fbtn_remove.toolTipText")); // NOI18N
        fbtn_remove.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_removeActionPerformed(evt);
            }
        });
        pnl_close.add(fbtn_remove);

        javax.swing.GroupLayout roundedPanel1Layout = new javax.swing.GroupLayout(roundedPanel1);
        roundedPanel1.setLayout(roundedPanel1Layout);
        roundedPanel1Layout.setHorizontalGroup(
            roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(fbtn_name, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(pnl_close, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel1Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(birdViewComp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(2, 2, 2))
        );
        roundedPanel1Layout.setVerticalGroup(
            roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel1Layout.createSequentialGroup()
                .addComponent(pnl_close, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(fbtn_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(birdViewComp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3))
        );

        add(roundedPanel1, "card2");
    }// </editor-fold>//GEN-END:initComponents

    private void fbtn_editActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_editActionPerformed
    {//GEN-HEADEREND:event_fbtn_editActionPerformed
        controller.editUserPhrase();
    }//GEN-LAST:event_fbtn_editActionPerformed

    private void fbtn_removeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_removeActionPerformed
    {//GEN-HEADEREND:event_fbtn_removeActionPerformed
        controller.closePanel();
    }//GEN-LAST:event_fbtn_removeActionPerformed

    private void fbtn_nameActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_nameActionPerformed
    {//GEN-HEADEREND:event_fbtn_nameActionPerformed
        var dlg = FlatTextEditDialog.getInstance();
        dlg.setTextNbColumns(6);
        dlg.setTextHorizontalAlignment(JTextField.CENTER);
        String oldValue = userRhythmVoice.getName();
        dlg.setText(oldValue);
        dlg.setPositionCenter(fbtn_name);
        dlg.setVisible(true);
        String newValue = dlg.getText().trim();
        if (dlg.isExitOk() && newValue.length() > 0 && !newValue.equals(oldValue))
        {
            controller.userChannelNameEdited(newValue);
        }
    }//GEN-LAST:event_fbtn_nameActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.phrase.api.ui.PhraseBirdView birdViewComp;
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_edit;
    private org.jjazz.ui.flatcomponents.api.FlatHelpButton fbtn_help;
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_name;
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_remove;
    private javax.swing.JPanel jPanel2;
    private org.jjazz.phrase.api.ui.PhraseBirdView phraseBirdView1;
    private javax.swing.JPanel pnl_close;
    private javax.swing.JPanel pnl_edit;
    private javax.swing.JPanel pnl_help;
    private org.jjazz.ui.flatcomponents.api.RoundedPanel roundedPanel1;
    // End of variables declaration//GEN-END:variables


}
