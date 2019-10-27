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
package org.jjazz.ui.musiccontrolactions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.musiccontrol.PlaybackListener;
import org.jjazz.song.api.Song;

import org.openide.awt.Actions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.actions.BooleanStateAction;

/**
 * The panel used as a control toolbar.
 */
public class ControlToolbarPanel extends javax.swing.JPanel implements PropertyChangeListener, LookupListener, PlaybackListener
{

    private Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private Position posModel;
    private static final Logger LOGGER = Logger.getLogger(ControlToolbarPanel.class.getSimpleName());

    public ControlToolbarPanel()
    {
        initComponents();
        fbtn_Active.setAction((BooleanStateAction) Actions.forID("MixConsole", "org.jjazz.ui.mixconsole.setactive"));
        fbtn_Play.setAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.ui.musiccontrolactions.play"));
        fbtn_Pause.setAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.ui.musiccontrolactions.pause"));
        fbtn_Stop.setAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.ui.musiccontrolactions.stop"));
        fbtn_Click.setAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.ui.musiccontrolactions.click"));
        fbtn_Precount.setAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.ui.musiccontrolactions.precount"));
        fbtn_Loop.setAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.ui.musiccontrolactions.loop"));
        fbtn_PlaybackPoint.setAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.ui.musiccontrolactions.showplaybackpoint"));

        // The model for the PositionViewer
        posModel = new Position();

        // Listen to playbackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);
        MusicController.getInstance().addPlaybackListener(this);

        // Listen to the active MidiMix changes
        ActiveSongManager.getInstance().addPropertyListener(this);

        // Listen to the current Song changes
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Song.class);
        lookupResult.addLookupListener(this);
        currentSongChanged();
    }

    @Override
    public void resultChanged(LookupEvent ev)
    {
        int i = 0;
        Song newSong = null;
        for (Song s : lookupResult.allInstances())
        {
            newSong = s;
            i++;
        }
        assert i < 2 : "i=" + i + " lookupResult.allInstances()=" + lookupResult.allInstances();
        if (newSong != null)
        {
            // Current song has changed
            if (currentSong != null)
            {
                currentSong.removePropertyChangeListener(this);
            }
            currentSong = newSong;
            currentSong.addPropertyChangeListener(this);
            currentSongChanged();
        } else
        {
            // Do nothing : player is still using the last valid song
        }
    }
    // ======================================================================
    // Playbackistener interface
    // ======================================================================  

    @Override
    public void beatChanged(final Position oldPos, final Position newPos)
    {
        // Changes can be generated outside the EDT
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                posModel.set(newPos);   // PositionViewer listens to posModel changes
            }
        };
        org.jjazz.ui.utilities.Utilities.invokeLaterIfNeeded(run);
    }

    @Override
    public void barChanged(int oldBar, int newBar)
    {
        // Nothing
    }

    @Override
    public void midiActivity(int channel, long tick)
    {
        // Nothing
    }

    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        MusicController mc = MusicController.getInstance();
        if (evt.getSource() == mc)
        {
            if (evt.getPropertyName() == MusicController.PROP_PLAYBACK_STATE)
            {
                playbackStateChanged();
            }
        } else if (evt.getSource() == ActiveSongManager.getInstance())
        {
            if (evt.getPropertyName() == ActiveSongManager.PROP_ACTIVE_SONG)
            {
                activeSongChanged();
            }
        } else if (evt.getSource() == currentSong)
        {
            if (evt.getPropertyName() == Song.PROP_TEMPO)
            {
                spn_Tempo.setValue(currentSong.getTempo());
            } else if (evt.getPropertyName() == Song.PROP_CLOSED)
            {
                currentSongClosed();
            }
        }
    }

    // ======================================================================
    // Private methods
    // ======================================================================   
    private void activeSongChanged()
    {
        Song activeSong = ActiveSongManager.getInstance().getActiveSong();
        currentSongChanged();    // Enable/Disable components       
        boolean b = (currentSong != null) && (currentSong == activeSong);
        if (b)
        {
            // Current song is active, initialize Tempo and PositionViewer
            if (posViewer.getSongModel() != currentSong)
            {
                posModel.setBar(0);
                posModel.setBeat(0);
                posViewer.setModel(currentSong, posModel);
            }
            spn_Tempo.setValue(currentSong.getTempo());
        }
    }

    private void currentSongChanged()
    {
        Song activeSong = ActiveSongManager.getInstance().getActiveSong();
        boolean b = (currentSong != null) && (currentSong == activeSong);
        posViewer.setEnabled(b);
        spn_Tempo.setEnabled(b);
    }

    private void currentSongClosed()
    {
        currentSong.removePropertyChangeListener(this);
        currentSong = null;
        currentSongChanged();
    }

    private void playbackStateChanged()
    {
        // Nothing
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_Active = new org.jjazz.ui.flatcomponents.FlatToggleButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(5, 32767));
        panel_PlayButtons = new javax.swing.JPanel();
        fbtn_Stop = new org.jjazz.ui.flatcomponents.FlatToggleButton();
        fbtn_Play = new org.jjazz.ui.flatcomponents.FlatToggleButton();
        fbtn_Pause = new org.jjazz.ui.flatcomponents.FlatToggleButton();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(15, 0), new java.awt.Dimension(15, 0), new java.awt.Dimension(15, 32767));
        posViewer = new org.jjazz.ui.musiccontrolactions.PositionViewer();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 32767));
        fbtn_Loop = new org.jjazz.ui.flatcomponents.FlatToggleButton();
        fbtn_PlaybackPoint = new org.jjazz.ui.flatcomponents.FlatToggleButton();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(15, 0), new java.awt.Dimension(15, 0), new java.awt.Dimension(15, 32767));
        panel_Tempo = new javax.swing.JPanel();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        spn_Tempo = new org.jjazz.ui.utilities.WheelSpinner();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 32767));
        fbtn_Click = new org.jjazz.ui.flatcomponents.FlatToggleButton();
        fbtn_Precount = new org.jjazz.ui.flatcomponents.FlatToggleButton();

        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
        add(filler2);

        fbtn_Active.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/Off-24x18.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_Active, org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Active.text")); // NOI18N
        fbtn_Active.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Active.toolTipText")); // NOI18N
        fbtn_Active.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/On-24x18.png"))); // NOI18N
        add(fbtn_Active);
        add(filler1);

        panel_PlayButtons.setBackground(new java.awt.Color(127, 126, 126));
        panel_PlayButtons.setLayout(new javax.swing.BoxLayout(panel_PlayButtons, javax.swing.BoxLayout.LINE_AXIS));

        fbtn_Stop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/StopButtonBorderOff-24x24.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_Stop, org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Stop.text")); // NOI18N
        panel_PlayButtons.add(fbtn_Stop);

        fbtn_Play.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlayButtonBorder-24x24.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_Play, org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Play.text")); // NOI18N
        fbtn_Play.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Play.toolTipText")); // NOI18N
        fbtn_Play.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlayButtonBorderOn-24x24.png"))); // NOI18N
        panel_PlayButtons.add(fbtn_Play);

        fbtn_Pause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PauseButtonBorder-24x24.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_Pause, org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Pause.text")); // NOI18N
        fbtn_Pause.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Pause.toolTipText")); // NOI18N
        fbtn_Pause.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PauseButtonBorderOn-24x24.png"))); // NOI18N
        panel_PlayButtons.add(fbtn_Pause);

        add(panel_PlayButtons);
        add(filler3);

        posViewer.setBackground(new java.awt.Color(204, 204, 204));
        posViewer.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 102, 153)), javax.swing.BorderFactory.createEmptyBorder(1, 8, 1, 8)));
        org.openide.awt.Mnemonics.setLocalizedText(posViewer, org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.posViewer.text")); // NOI18N
        posViewer.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.posViewer.toolTipText")); // NOI18N
        posViewer.setFont(new java.awt.Font("Courier New", 1, 16)); // NOI18N
        posViewer.setOpaque(true);
        add(posViewer);
        add(filler4);

        fbtn_Loop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/Loop-OFF-24x24.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_Loop, org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Loop.text")); // NOI18N
        fbtn_Loop.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Loop.toolTipText")); // NOI18N
        fbtn_Loop.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/Loop-ON-24x24.png"))); // NOI18N
        add(fbtn_Loop);

        fbtn_PlaybackPoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointOFF-24x24.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_PlaybackPoint, org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_PlaybackPoint.text")); // NOI18N
        fbtn_PlaybackPoint.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointON-24x24.png"))); // NOI18N
        add(fbtn_PlaybackPoint);
        add(filler5);

        panel_Tempo.setOpaque(false);
        panel_Tempo.setLayout(new javax.swing.BoxLayout(panel_Tempo, javax.swing.BoxLayout.Y_AXIS));
        panel_Tempo.add(filler6);

        spn_Tempo.setModel(new javax.swing.SpinnerNumberModel(120, 30, 350, 1));
        spn_Tempo.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.spn_Tempo.toolTipText")); // NOI18N
        spn_Tempo.setMaximumSize(new java.awt.Dimension(69, 26));
        spn_Tempo.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_TempoStateChanged(evt);
            }
        });
        panel_Tempo.add(spn_Tempo);
        panel_Tempo.add(filler7);

        add(panel_Tempo);
        add(filler8);

        fbtn_Click.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/Click-OFF-24x24.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_Click, org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Click.text")); // NOI18N
        fbtn_Click.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Click.toolTipText")); // NOI18N
        fbtn_Click.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/Click-ON-24x24.png"))); // NOI18N
        add(fbtn_Click);

        fbtn_Precount.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/precount-OFF-24x24.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_Precount, org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Precount.text")); // NOI18N
        fbtn_Precount.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Precount.toolTipText")); // NOI18N
        fbtn_Precount.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/precount-ON-24x24.png"))); // NOI18N
        add(fbtn_Precount);
    }// </editor-fold>//GEN-END:initComponents

    private void spn_TempoStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_TempoStateChanged
    {//GEN-HEADEREND:event_spn_TempoStateChanged
        assert currentSong != null; // Otherwise button should be disabled
        currentSong.setTempo((int) spn_Tempo.getValue());
    }//GEN-LAST:event_spn_TempoStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.ui.flatcomponents.FlatToggleButton fbtn_Active;
    private org.jjazz.ui.flatcomponents.FlatToggleButton fbtn_Click;
    private org.jjazz.ui.flatcomponents.FlatToggleButton fbtn_Loop;
    private org.jjazz.ui.flatcomponents.FlatToggleButton fbtn_Pause;
    private org.jjazz.ui.flatcomponents.FlatToggleButton fbtn_Play;
    private org.jjazz.ui.flatcomponents.FlatToggleButton fbtn_PlaybackPoint;
    private org.jjazz.ui.flatcomponents.FlatToggleButton fbtn_Precount;
    private org.jjazz.ui.flatcomponents.FlatToggleButton fbtn_Stop;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.JPanel panel_PlayButtons;
    private javax.swing.JPanel panel_Tempo;
    private org.jjazz.ui.musiccontrolactions.PositionViewer posViewer;
    private org.jjazz.ui.utilities.WheelSpinner spn_Tempo;
    // End of variables declaration//GEN-END:variables

}
