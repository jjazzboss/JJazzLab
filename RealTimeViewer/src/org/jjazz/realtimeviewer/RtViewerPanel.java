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
package org.jjazz.realtimeviewer;

import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.midi.ui.keyboard.KeyboardComponent;
import org.jjazz.midi.ui.keyboard.KeyboardRange;
import org.jjazz.midimix.MidiMix;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.musiccontrol.NoteListener;
import org.jjazz.musiccontrol.PlaybackListenerAdapter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.uisettings.GeneralUISettings;

/**
 * Real time viewer panel
 */
public class RtViewerPanel extends javax.swing.JPanel implements PropertyChangeListener
{

    private static final String NO_TRACKNAME = " - ";
    private final KeyboardComponent keyboard;
    private Song song;
    private MidiMix midiMix;
    private int channel;
    private ViewerNoteListener noteListener;
    private boolean enableListening;
    private final Font chordSymbolFont;

    private static final Logger LOGGER = Logger.getLogger(RtViewerPanel.class.getSimpleName());

    /**
     * Creates new form RtViewerPanel
     */
    public RtViewerPanel()
    {

        // Used by initComponents
        chordSymbolFont = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(Font.BOLD, 16f);

        initComponents();


        enableListening = true;

        // Viewer
        keyboard = new KeyboardComponent(KeyboardRange._76_KEYS);
        pnl_piano.add(keyboard);


        // Get the chord symbols changes
        MusicController mc = MusicController.getInstance();
        mc.addPlaybackListener(new PlaybackListenerAdapter()
        {
            @Override
            public void chordSymbolChanged(String cs)
            {
                lbl_chordScale.setText(cs);
            }
        });

        // Get the playback state changes
        mc.addPropertyChangeListener(this);

        // Get the incoming notes
        noteListener = new ViewerNoteListener();
        mc.addNoteListener(noteListener);


        // keyboard must be created
        spn_channelStateChanged(null);


        // Listen to the active song
        var asm = ActiveSongManager.getInstance();
        asm.addPropertyListener(this);
        activeSongChanged(asm.getActiveSong(), asm.getActiveMidiMix());


    }

    // =================================================================================
    // PropertyChangeListener implementation
    // =================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == ActiveSongManager.getInstance())
        {
            if (evt.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
            {
                activeSongChanged((Song) evt.getNewValue(), (MidiMix) evt.getOldValue());
            }
        } else if (evt.getSource() == MusicController.getInstance())
        {
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                MusicController.State state = (MusicController.State) evt.getNewValue();
                switch (state)
                {
                    case DISABLED:  // Fall down
                    case STOPPED:
                        org.jjazz.ui.utilities.Utilities.invokeLaterIfNeeded(() -> lbl_chordScale.setText(" "));
                        resetKeyboard();
                        break;
                    case PAUSED:
                        // Nothing
                        break;
                    case PLAYING:
                        // Nothing
                        break;
                    default:
                        throw new AssertionError(state.name());
                }
            }
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

        jPanel1 = new javax.swing.JPanel();
        pnl_piano = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.HelpTextArea();
        spn_channel = new org.jjazz.ui.utilities.WheelSpinner();
        lbl_trackName = new javax.swing.JLabel();
        fbtn_changeSize = new org.jjazz.ui.flatcomponents.FlatButton();
        pnl_chordSymbol = new javax.swing.JPanel();
        lbl_chordScale = new javax.swing.JLabel();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        pnl_piano.setLayout(new javax.swing.BoxLayout(pnl_piano, javax.swing.BoxLayout.LINE_AXIS));

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getBundle(RtViewerPanel.class).getString("RtViewerPanel.jLabel1.text")); // NOI18N

        jScrollPane1.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(4);
        helpTextArea1.setText(org.openide.util.NbBundle.getBundle(RtViewerPanel.class).getString("RtViewerPanel.helpTextArea1.text")); // NOI18N
        jScrollPane1.setViewportView(helpTextArea1);

        spn_channel.setModel(new javax.swing.SpinnerNumberModel(11, 1, 16, 1));
        spn_channel.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_channelStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_trackName, org.openide.util.NbBundle.getBundle(RtViewerPanel.class).getString("RtViewerPanel.lbl_trackName.text")); // NOI18N

        fbtn_changeSize.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/realtimeviewer/resources/PianoSize.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_changeSize, org.openide.util.NbBundle.getBundle(RtViewerPanel.class).getString("RtViewerPanel.fbtn_changeSize.text")); // NOI18N
        fbtn_changeSize.setToolTipText(org.openide.util.NbBundle.getBundle(RtViewerPanel.class).getString("RtViewerPanel.fbtn_changeSize.toolTipText")); // NOI18N
        fbtn_changeSize.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_changeSizeActionPerformed(evt);
            }
        });

        pnl_chordSymbol.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        lbl_chordScale.setFont(chordSymbolFont);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_chordScale, org.openide.util.NbBundle.getBundle(RtViewerPanel.class).getString("RtViewerPanel.lbl_chordScale.text")); // NOI18N
        pnl_chordSymbol.add(lbl_chordScale);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_chordSymbol, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(spn_channel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lbl_trackName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 143, Short.MAX_VALUE)
                        .addComponent(fbtn_changeSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1)
                    .addComponent(pnl_piano, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(spn_channel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lbl_trackName))
                    .addComponent(fbtn_changeSize, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_chordSymbol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(pnl_piano, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void fbtn_changeSizeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_changeSizeActionPerformed
    {//GEN-HEADEREND:event_fbtn_changeSizeActionPerformed

        enableListening = false;
        keyboard.setKeyboardRange(keyboard.getKeyboardRange().next());
        resetKeyboard();            // Will restore enableListening=true
    }//GEN-LAST:event_fbtn_changeSizeActionPerformed

    private void spn_channelStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_channelStateChanged
    {//GEN-HEADEREND:event_spn_channelStateChanged
        channel = (Integer) spn_channel.getValue() - 1;
        channelOrTrackChanged();
    }//GEN-LAST:event_spn_channelStateChanged

    // =================================================================================
    // Private methods
    // =================================================================================
    /**
     *
     * @param sg Can be null
     * @param mm Can be null
     */
    private void activeSongChanged(Song sg, MidiMix mm)
    {
        // Reset everything by default
        song = null;
        midiMix = null;
        lbl_chordScale.setText(" ");

        if (sg != null && mm != null)
        {
            song = sg;
            midiMix = mm;
        }

        channelOrTrackChanged();
    }

    private void channelOrTrackChanged()
    {
        String s = NO_TRACKNAME;
        if (midiMix != null)
        {
            RhythmVoice rv = midiMix.getRhythmVoice(channel);
            if (rv != null)
            {
                s = rv.getContainer().getName() + "/" + rv.getName();
            }
        }
        lbl_trackName.setText(s);

        resetKeyboard();
    }

    private void resetKeyboard()
    {
        enableListening = false;
        keyboard.releaseAllNotes();
        noteListener.reset();
        enableListening = true;
    }
      
    // =================================================================================
    // Private classes
    // =================================================================================
    private class ViewerNoteListener implements NoteListener
    {

        public static final long MIN_DURATION_MS = 100;

        // Store the last Note On position in milliseconds for each note and each channel. Use -1 if initialized.
        private final long noteOnPosMs[][] = new long[16][128];
        // Cache the created Timers
        private final Timer timersCache[][] = new Timer[16][128];

        public ViewerNoteListener()
        {
            reset();
        }

        public void reset()
        {
            for (int i = 0; i < 16; i++)
            {
                for (int j = 0; j < 128; j++)
                {
                    noteOnPosMs[i][j] = -1;
                    Timer t = timersCache[i][j];
                    if (t != null)
                    {
                        t.stop();
                    }
                }
            }
        }

        @Override
        public void noteOn(long tick, int channel, int pitch, int velocity)
        {
            if (!enableListening || RtViewerPanel.this.channel != channel)
            {
                return;
            }
            noteOnPosMs[channel][pitch] = System.currentTimeMillis();
            keyboard.setPressed(pitch, velocity);
        }

        @Override
        public void noteOff(long tick, int channel, int pitch)
        {
            if (!enableListening || RtViewerPanel.this.channel != channel)
            {
                return;
            }
            long durationMs;
            long noteOnPos = noteOnPosMs[channel][pitch];
            if (noteOnPos >= 0 && (durationMs = System.currentTimeMillis() - noteOnPos) < MIN_DURATION_MS)
            {
                // Onset time is too short to be visible, make it longer
                Timer t = timersCache[channel][pitch];
                if (t == null)
                {
                    // This is the first time this note goes off
                    t = new Timer((int) (MIN_DURATION_MS - durationMs), evt ->
                    {
                        keyboard.setPressed(pitch, 0);
                    });
                    timersCache[channel][pitch] = t;        // Save the timer for reuse               
                    t.setRepeats(false);
                    t.start();
                } else
                {
                    // This is not the first time this note goes OFF
                    t.stop(); // Needed if 2 very short consecutive notes
                    t.setInitialDelay((int) (MIN_DURATION_MS - durationMs));
                    t.start();
                }
            } else
            {
                // Normal case, directly release the key
                SwingUtilities.invokeLater(() -> keyboard.setPressed(pitch, 0));
            }
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.ui.flatcomponents.FlatButton fbtn_changeSize;
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_chordScale;
    private javax.swing.JLabel lbl_trackName;
    private javax.swing.JPanel pnl_chordSymbol;
    private javax.swing.JPanel pnl_piano;
    private org.jjazz.ui.utilities.WheelSpinner spn_channel;
    // End of variables declaration//GEN-END:variables

}
