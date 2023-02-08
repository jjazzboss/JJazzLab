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
import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;
import java.util.logging.Logger;
import javax.swing.JTextField;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.songstructure.api.event.SptResizedEvent;
import org.jjazz.ui.flatcomponents.api.FlatTextEditDialog;
import org.jjazz.ui.mixconsole.api.MixConsoleSettings;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.Utilities;

/**
 * An extension of a MixChannelPanel to add specific controls for user phrase channels.
 * <p>
 */
public class UserExtensionPanel extends javax.swing.JPanel implements VetoableChangeListener, SgsChangeListener, PropertyChangeListener
{

    /**
     * oldValue=old rhythmVoice, newValue=new rhythmVoice
     */
    public static final String PROP_RHYTHM_VOICE = "PropRhythmVoice";
    private UserRhythmVoice userRhythmVoice;
    private Song song;
    private MidiMix midiMix;
    private final MixConsoleSettings settings;
    private final Font FONT = GeneralUISettings.getInstance().getStdCondensedFont();
    private UserExtensionPanelController controller;
    private static final Logger LOGGER = Logger.getLogger(UserExtensionPanel.class.getSimpleName());

    public UserExtensionPanel()
    {
        this(null, null, null, null, null);
    }

    public UserExtensionPanel(Song song, MidiMix midiMix, UserRhythmVoice urv, UserExtensionPanelController controller, MixConsoleSettings settings)
    {
        initComponents();

        this.settings = settings;
        if (this.settings != null)
        {
            this.settings.addPropertyChangeListener(this);
            this.roundedPanel1.setBackground(settings.getMixChannelBackgroundColor());
        }

        this.controller = controller;
        this.controller.setUserExtensionPanel(this);
        this.userRhythmVoice = urv;
        this.song = song;
        this.song.addVetoableChangeListener(this);
        this.song.getSongStructure().addSgsChangeListener(this);
        this.midiMix = midiMix;
        this.midiMix.addPropertyChangeListener(this);       // Listen to RhythmVoice change





        refreshBirdView();
        refreshUI();
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
        midiMix.removePropertyChangeListener(this);
        song.removeVetoableChangeListener(this);
        song.getSongStructure().removeSgsChangeListener(this);
        settings.removePropertyChangeListener(this);
    }

    // ----------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == settings)
        {
            refreshUI();

        } else if (evt.getSource() == midiMix)
        {
            if (evt.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE))
            {
                RhythmVoice rvNew = (RhythmVoice) evt.getNewValue();
                RhythmVoice rvOld = (RhythmVoice) evt.getOldValue();
                if (userRhythmVoice == rvOld)
                {
                    var old = userRhythmVoice;
                    userRhythmVoice = (UserRhythmVoice) rvNew;
                    refreshUI();
                    firePropertyChange(PROP_RHYTHM_VOICE, old, userRhythmVoice);
                }
            }
        }
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
                // The phrase has been replaced by another
                refreshBirdView();
            } else if (e.getPropertyName().equals(Song.PROP_VETOABLE_PHRASE_NAME))
            {
                // The phrase name was changed
                // Do nothing: we'll catch the Midix RhythmVoice change event
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
                || e instanceof SptReplacedEvent)
        {
            // Song size in beats is impacted, so is our birdViewComponent
            refreshBirdView();
        }
    }

    //-----------------------------------------------------------------------
    // Private methods
    //-----------------------------------------------------------------------
    private void refreshBirdView()
    {
        LOGGER.fine("phraseUpdated() --");
        var ss = song.getSongStructure();
        if (ss.getSizeInBars() == 0)
        {
            return;
        }
        FloatRange beatRange = ss.getBeatRange(null);
        TimeSignature ts = ss.getSongPart(0).getRhythm().getTimeSignature();
        var p = song.getUserPhrase(userRhythmVoice.getName());
        birdViewComp.setModel(p, ts, beatRange);
    }

    private void refreshUI()
    {
        this.roundedPanel1.setBackground(settings.getMixChannelBackgroundColor());
        if (userRhythmVoice != null)
        {
            String s = Utilities.truncateWithDots(userRhythmVoice.getName(), 8);
            fbtn_name.setText(s);
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

        phraseBirdView1 = new org.jjazz.phrase.api.ui.PhraseBirdsEyeViewComponent();
        roundedPanel1 = new org.jjazz.ui.flatcomponents.api.RoundedPanel();
        birdViewComp = new org.jjazz.phrase.api.ui.PhraseBirdsEyeViewComponent();
        jPanel2 = new javax.swing.JPanel();
        fbtn_edit = new org.jjazz.ui.flatcomponents.api.FlatButton();
        pnl_placement = new javax.swing.JPanel();
        fbtn_remove = new org.jjazz.ui.flatcomponents.api.FlatButton();
        fbtn_name = new org.jjazz.ui.flatcomponents.api.FlatButton();

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

        birdViewComp.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(61, 61, 61)));
        birdViewComp.setForeground(new java.awt.Color(192, 115, 242));
        birdViewComp.setToolTipText(org.openide.util.NbBundle.getMessage(UserExtensionPanel.class, "UserExtensionPanel.birdViewComp.toolTipText")); // NOI18N
        birdViewComp.setOpaque(false);

        javax.swing.GroupLayout birdViewCompLayout = new javax.swing.GroupLayout(birdViewComp);
        birdViewComp.setLayout(birdViewCompLayout);
        birdViewCompLayout.setHorizontalGroup(
            birdViewCompLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 61, Short.MAX_VALUE)
        );
        birdViewCompLayout.setVerticalGroup(
            birdViewCompLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 39, Short.MAX_VALUE)
        );

        jPanel2.setMinimumSize(new java.awt.Dimension(10, 36));
        jPanel2.setOpaque(false);
        jPanel2.setLayout(new java.awt.BorderLayout());

        fbtn_edit.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        fbtn_edit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/Edit-14x14.png"))); // NOI18N
        fbtn_edit.setToolTipText(org.openide.util.NbBundle.getMessage(UserExtensionPanel.class, "UserExtensionPanel.fbtn_edit.toolTipText")); // NOI18N
        fbtn_edit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_editActionPerformed(evt);
            }
        });
        jPanel2.add(fbtn_edit, java.awt.BorderLayout.WEST);

        pnl_placement.setOpaque(false);
        pnl_placement.setLayout(new javax.swing.BoxLayout(pnl_placement, javax.swing.BoxLayout.X_AXIS));

        fbtn_remove.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        fbtn_remove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/Close-10x10.png"))); // NOI18N
        fbtn_remove.setToolTipText(org.openide.util.NbBundle.getMessage(UserExtensionPanel.class, "UserExtensionPanel.fbtn_remove.toolTipText")); // NOI18N
        fbtn_remove.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_removeActionPerformed(evt);
            }
        });
        pnl_placement.add(fbtn_remove);

        jPanel2.add(pnl_placement, java.awt.BorderLayout.EAST);

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

        javax.swing.GroupLayout roundedPanel1Layout = new javax.swing.GroupLayout(roundedPanel1);
        roundedPanel1.setLayout(roundedPanel1Layout);
        roundedPanel1Layout.setHorizontalGroup(
            roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(fbtn_name, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanel1Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(birdViewComp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(2, 2, 2))
        );
        roundedPanel1Layout.setVerticalGroup(
            roundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fbtn_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(birdViewComp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
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
            controller.userChannelNameEdited(oldValue, newValue);
        }
    }//GEN-LAST:event_fbtn_nameActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.phrase.api.ui.PhraseBirdsEyeViewComponent birdViewComp;
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_edit;
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_name;
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_remove;
    private javax.swing.JPanel jPanel2;
    private org.jjazz.phrase.api.ui.PhraseBirdsEyeViewComponent phraseBirdView1;
    private javax.swing.JPanel pnl_placement;
    private org.jjazz.ui.flatcomponents.api.RoundedPanel roundedPanel1;
    // End of variables declaration//GEN-END:variables


}
