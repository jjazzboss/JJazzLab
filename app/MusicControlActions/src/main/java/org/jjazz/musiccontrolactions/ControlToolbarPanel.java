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
package org.jjazz.musiccontrolactions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.harmony.api.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListenerAdapter;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
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
public class ControlToolbarPanel extends javax.swing.JPanel implements PropertyChangeListener, LookupListener
{

    private final Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private Position posModel;
    private static final Logger LOGGER = Logger.getLogger(ControlToolbarPanel.class.getSimpleName());

    public ControlToolbarPanel()
    {
        initComponents();

        // Initialize actions
        fbtn_Active.setBooleanStateAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.setactive"));
        fbtn_Play.setBooleanStateAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.play"));
        fbtn_Pause.setBooleanStateAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.pause"));
        fbtn_Stop.setBooleanStateAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.stop"));
        fbtn_Click.setBooleanStateAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.click"));
        fbtn_Precount.setBooleanStateAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.precount"));
        fbtn_Loop.setBooleanStateAction((BooleanStateAction) Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.loop"));
        fbtn_PlaybackPoint.setBooleanStateAction((BooleanStateAction) Actions.forID("MusicControls",
                "org.jjazz.musiccontrolactions.showplaybackpoint"));
        fbtn_next.setAction(Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.playbacktonextsongpart"));
        fbtn_previous.setAction(Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.playbacktoprevioussongpart"));


        // The model for the PositionViewer
        posModel = new Position();

        // Listen to playbackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);
        MusicController.getInstance().addPlaybackListener(new SongListener());


        // Listen to the active MidiMix changes
        ActiveSongManager.getDefault().addPropertyListener(this);


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
        LOGGER.log(Level.FINE, "resultChanged() newSong={0} => currentSong={1}", new Object[]
        {
            newSong, currentSong
        });
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
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                playbackStateChanged();
            }
        } else if (evt.getSource() == ActiveSongManager.getDefault())
        {
            if (evt.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
            {
                activeSongChanged();
            }
        } else if (evt.getSource() == currentSong)
        {
            if (evt.getPropertyName().equals(Song.PROP_TEMPO))
            {
                spn_Tempo.setValue(currentSong.getTempo());
            } else if (evt.getPropertyName().equals(Song.PROP_CLOSED))
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
        currentSongChanged();    // Enable/Disable components       


        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
        boolean b = (currentSong != null) && (currentSong == activeSong);


        LOGGER.log(Level.FINE, "activeSongChanged() b={0} currentSong={1} activeSong={2}", new Object[]
        {
            b, currentSong, activeSong
        });


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
        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
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

    // ======================================================================
    // Inner classes
    // ======================================================================  
    private class SongListener extends PlaybackListenerAdapter
    {

        public SongListener()
        {
            super(EnumSet.of(PlaybackSession.Context.SONG));
        }

        @Override
        public void beatChanged(Position oldPos, Position newPos, float newPosInBeats)
        {
            // Changes are generated outside the EDT
            SwingUtilities.invokeLater(() -> posModel.set(newPos)); // PositionViewer listens to posModel changes
        }
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_Active = new org.jjazz.flatcomponents.api.FlatToggleButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        panel_PlayButtons = new javax.swing.JPanel();
        fbtn_previous = new org.jjazz.flatcomponents.api.FlatButton();
        fbtn_Stop = new org.jjazz.flatcomponents.api.FlatToggleButton();
        fbtn_Play = new org.jjazz.flatcomponents.api.FlatToggleButton();
        fbtn_Pause = new org.jjazz.flatcomponents.api.FlatToggleButton();
        fbtn_next = new org.jjazz.flatcomponents.api.FlatButton();
        filler10 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(16, 0), new java.awt.Dimension(20, 32767));
        posViewer = new org.jjazz.musiccontrolactions.api.ui.PositionViewer();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 32767));
        fbtn_Loop = new org.jjazz.flatcomponents.api.FlatToggleButton();
        fbtn_PlaybackPoint = new org.jjazz.flatcomponents.api.FlatToggleButton();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 32767));
        panel_Tempo = new javax.swing.JPanel()
        {
            @Override
            public java.awt.Dimension getMaximumSize()
            {
                return getMinimumSize();
            }
        };
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        spn_Tempo = new org.jjazz.flatcomponents.api.WheelSpinner();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 32767));
        fbtn_Click = new org.jjazz.flatcomponents.api.FlatToggleButton();
        fbtn_Precount = new org.jjazz.flatcomponents.api.FlatToggleButton();
        filler9 = new javax.swing.Box.Filler(new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 32767));

        setBackground(new java.awt.Color(60, 63, 65));
        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
        add(filler2);

        fbtn_Active.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/Off-24x18.png"))); // NOI18N
        fbtn_Active.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Active.toolTipText")); // NOI18N
        fbtn_Active.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/On-24x18.png"))); // NOI18N
        add(fbtn_Active);
        add(filler1);

        panel_PlayButtons.setBackground(new java.awt.Color(48, 50, 52));
        panel_PlayButtons.setOpaque(false);
        panel_PlayButtons.setLayout(new javax.swing.BoxLayout(panel_PlayButtons, javax.swing.BoxLayout.LINE_AXIS));

        fbtn_previous.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PreviousSongpart-24x24.png"))); // NOI18N
        panel_PlayButtons.add(fbtn_previous);

        fbtn_Stop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/StopButton-24x24.png"))); // NOI18N
        fbtn_Stop.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/StopButtonOn-24x24.png"))); // NOI18N
        panel_PlayButtons.add(fbtn_Stop);

        fbtn_Play.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PlayButton-24x24.png"))); // NOI18N
        fbtn_Play.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PlayButtonOn-24x24.png"))); // NOI18N
        panel_PlayButtons.add(fbtn_Play);

        fbtn_Pause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PauseButton-24x24.png"))); // NOI18N
        fbtn_Pause.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Pause.toolTipText")); // NOI18N
        fbtn_Pause.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PauseButtonOn-24x24.png"))); // NOI18N
        panel_PlayButtons.add(fbtn_Pause);

        add(panel_PlayButtons);

        fbtn_next.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/NextSongpart-24x24.png"))); // NOI18N
        add(fbtn_next);
        add(filler10);

        posViewer.setBorder(javax.swing.BorderFactory.createCompoundBorder(spn_Tempo.getBorder(), javax.swing.BorderFactory.createEmptyBorder(1, 8, 1, 8)));
        org.openide.awt.Mnemonics.setLocalizedText(posViewer, "001:1"); // NOI18N
        posViewer.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.posViewer.toolTipText")); // NOI18N
        posViewer.setFont(new java.awt.Font("Courier New", 1, 16)); // NOI18N
        add(posViewer);
        add(filler4);

        fbtn_Loop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/Loop-OFF-24x24.png"))); // NOI18N
        fbtn_Loop.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/Loop-ON-24x24.png"))); // NOI18N
        add(fbtn_Loop);

        fbtn_PlaybackPoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PlaybackPointOFF-24x24.png"))); // NOI18N
        fbtn_PlaybackPoint.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PlaybackPointON-24x24.png"))); // NOI18N
        add(fbtn_PlaybackPoint);
        add(filler5);

        panel_Tempo.setOpaque(false);
        panel_Tempo.setLayout(new javax.swing.BoxLayout(panel_Tempo, javax.swing.BoxLayout.Y_AXIS));
        panel_Tempo.add(filler6);

        spn_Tempo.setModel(new javax.swing.SpinnerNumberModel(120, 30, 350, 1));
        spn_Tempo.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.spn_Tempo.toolTipText")); // NOI18N
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

        fbtn_Click.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/Click-OFF-24x24.png"))); // NOI18N
        fbtn_Click.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Click.toolTipText")); // NOI18N
        fbtn_Click.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/Click-ON-24x24.png"))); // NOI18N
        add(fbtn_Click);

        fbtn_Precount.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/precount2-OFF-24x24.png"))); // NOI18N
        fbtn_Precount.setToolTipText(org.openide.util.NbBundle.getMessage(ControlToolbarPanel.class, "ControlToolbarPanel.fbtn_Precount.toolTipText")); // NOI18N
        fbtn_Precount.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/precount2-ON-24x24.png"))); // NOI18N
        add(fbtn_Precount);
        add(filler9);
    }// </editor-fold>//GEN-END:initComponents

    private void spn_TempoStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_TempoStateChanged
    {//GEN-HEADEREND:event_spn_TempoStateChanged
        assert currentSong != null; // Otherwise button should be disabled   
        IncreaseTempo.setSongTempo(currentSong, (int) spn_Tempo.getValue());
    }//GEN-LAST:event_spn_TempoStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_Active;
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_Click;
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_Loop;
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_Pause;
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_Play;
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_PlaybackPoint;
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_Precount;
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_Stop;
    private org.jjazz.flatcomponents.api.FlatButton fbtn_next;
    private org.jjazz.flatcomponents.api.FlatButton fbtn_previous;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler10;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.Box.Filler filler9;
    private javax.swing.JPanel panel_PlayButtons;
    private javax.swing.JPanel panel_Tempo;
    private org.jjazz.musiccontrolactions.api.ui.PositionViewer posViewer;
    private org.jjazz.flatcomponents.api.WheelSpinner spn_Tempo;
    // End of variables declaration//GEN-END:variables

}
