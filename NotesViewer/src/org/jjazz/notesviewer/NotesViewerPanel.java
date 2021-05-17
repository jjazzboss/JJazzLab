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
package org.jjazz.notesviewer;

import java.awt.Font;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractSpinnerModel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.midi.Instrument;
import org.jjazz.midimix.MidiMix;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.musiccontrol.NoteListener;
import org.jjazz.musiccontrol.PlaybackListenerAdapter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.uisettings.GeneralUISettings;
import org.jjazz.notesviewer.spi.NotesViewer;
import org.jjazz.ui.flatcomponents.FlatButton;
import org.openide.util.Lookup;

/**
 * Real time viewer panel
 */
public class NotesViewerPanel extends javax.swing.JPanel implements PropertyChangeListener, MouseWheelListener
{

    private NotesViewer notesViewer;
    private Song song;
    private MidiMix midiMix;
    private final NotesViewerListener noteListener;
    private MySpinnerModel spinnerModel;
    private final Font chordSymbolFont;

    private static final Logger LOGGER = Logger.getLogger(NotesViewerPanel.class.getSimpleName());

    /**
     * Creates new form RtViewerPanel
     */
    public NotesViewerPanel()
    {

        // Used by initComponents
        chordSymbolFont = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(Font.BOLD, 16f);

        initComponents();


        // Use mouse wheel on spinner only if enabled
        GeneralUISettings.getInstance().installChangeValueWithMouseWheelSupport(spn_srcChannel, this);


        // Get the chord symbols changes
        MusicController mc = MusicController.getInstance();
        mc.addPlaybackListener(new PlaybackListenerAdapter()
        {
            @Override
            public void chordSymbolChanged(String cs)
            {
                lbl_chordSymbol.setText(cs);
            }
        });


        // Get the playback state changes
        mc.addPropertyChangeListener(this);


        // Get the incoming notes to update the keyboard
        noteListener = new NotesViewerListener();
        mc.addNoteListener(noteListener);


        // Initialize the viewers
        setNotesViewer(initViewers());


        // Listen to the active song
        var asm = ActiveSongManager.getInstance();
        asm.addPropertyListener(this);
        activeSongChanged(asm.getActiveSong(), asm.getActiveMidiMix());


    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        spn_srcChannel.setEnabled(b);
        lbl_chordSymbol.setEnabled(b);
        lbl_scale.setEnabled(b);
        notesViewer.setEnabled(b);
    }

    // -----------------------------------------------------------------------------
    // MouseWheelListener interface
    // -----------------------------------------------------------------------------   
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if (!isEnabled())
        {
            return;
        }
        if (!hasFocus())
        {
            requestFocusInWindow();
        }
        spn_srcChannel.setValue(e.getWheelRotation() < 0 ? spinnerModel.getNextValue() : spinnerModel.getPreviousValue());
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
                        org.jjazz.ui.utilities.Utilities.invokeLaterIfNeeded(() -> lbl_chordSymbol.setText(" "));
                        org.jjazz.ui.utilities.Utilities.invokeLaterIfNeeded(() -> lbl_scale.setText(" "));
                        switchMode(NotesViewer.DisplayMode.HarmonyNotes, null);
                        break;
                    case PAUSED:
                        // Nothing
                        break;
                    case PLAYING:
                        switchMode(NotesViewer.DisplayMode.RealTimeNotes, midiMix.getRhythmVoice(spinnerModel.getChannel()));
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
        pnl_viewer = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.HelpTextArea();
        pnl_chordSymbol = new javax.swing.JPanel();
        spn_srcChannel = new javax.swing.JSpinner();
        lbl_chordSymbol = new javax.swing.JLabel();
        lbl_scale = new javax.swing.JLabel();
        pnl_buttons = new javax.swing.JPanel();

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

        pnl_viewer.setOpaque(false);
        pnl_viewer.setLayout(new javax.swing.BoxLayout(pnl_viewer, javax.swing.BoxLayout.LINE_AXIS));

        jScrollPane1.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(4);
        helpTextArea1.setText(org.openide.util.NbBundle.getBundle(NotesViewerPanel.class).getString("NotesViewerPanel.helpTextArea1.text")); // NOI18N
        jScrollPane1.setViewportView(helpTextArea1);

        pnl_chordSymbol.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        spn_srcChannel.setFont(spn_srcChannel.getFont().deriveFont(spn_srcChannel.getFont().getSize()-1f));
        spn_srcChannel.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_srcChannelStateChanged(evt);
            }
        });

        lbl_chordSymbol.setFont(chordSymbolFont);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_chordSymbol, "C7"); // NOI18N

        lbl_scale.setFont(lbl_scale.getFont().deriveFont(lbl_scale.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_scale, "Lydian"); // NOI18N

        pnl_buttons.setOpaque(false);
        pnl_buttons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 3, 0));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_chordSymbol, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 285, Short.MAX_VALUE)
                    .addComponent(pnl_viewer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(spn_srcChannel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lbl_chordSymbol)
                        .addGap(18, 18, 18)
                        .addComponent(lbl_scale)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(pnl_buttons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spn_srcChannel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_chordSymbol)
                    .addComponent(lbl_scale))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_chordSymbol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_viewer, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_buttons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void spn_srcChannelStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_srcChannelStateChanged
    {//GEN-HEADEREND:event_spn_srcChannelStateChanged
        channelChanged();
    }//GEN-LAST:event_spn_srcChannelStateChanged

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
        lbl_chordSymbol.setText(" ");
        lbl_scale.setText(" ");


        if (sg != null && mm != null)
        {
            setEnabled(true);

            song = sg;
            midiMix = mm;
            notesViewer.setContext(song, midiMix);


            // Update spinner model 
            spinnerModel = new MySpinnerModel(midiMix);
            spn_srcChannel.setModel(spinnerModel);
            spn_srcChannel.setEditor(new JSpinner.DefaultEditor(spn_srcChannel));  // Disable editing
            int maxNbChars = spinnerModel.getLongestValueLength();
            ((JSpinner.DefaultEditor) spn_srcChannel.getEditor()).getTextField().setColumns(Math.round(maxNbChars * 0.6f));
            spn_srcChannel.setEnabled(false); // If active song has changed, it means playback is stopped

            // Update note receiver
            noteListener.setReceiveChannel(spinnerModel.getChannel());
        } else
        {
            setEnabled(false);
        }

    }

    private void channelChanged()
    {
        if (notesViewer.getMode().equals(NotesViewer.DisplayMode.RealTimeNotes))
        {
            // Update note listener and reset notesViewer
            int channel = spinnerModel.getChannel();
            noteListener.setReceiveChannel(channel);
            notesViewer.setMode(NotesViewer.DisplayMode.RealTimeNotes, midiMix.getRhythmVoice(channel));
            notesViewer.releaseAllNotes();
        }
    }

    /**
     * Get all the NotesViewers available in the global lookup and add a button for each one.
     *
     * @return The default NotesViewers
     */
    private NotesViewer initViewers()
    {
        Collection<? extends NotesViewer> nViewers = Lookup.getDefault().lookupAll(NotesViewer.class);
        for (var nViewer : nViewers)
        {
            FlatButton btn = new FlatButton();
            btn.setIcon(nViewer.getIcon());
            btn.addActionListener(ae -> setNotesViewer(nViewer));
            pnl_buttons.add(btn);
        }
        assert !nViewers.isEmpty();
        return nViewers.iterator().next();
    }

    private void setNotesViewer(NotesViewer nViewer)
    {
        if (nViewer == null)
        {
            throw new NullPointerException("nViewer");
        }
        if (notesViewer == nViewer)
        {
            return;
        }
        if (notesViewer != null)
        {
            notesViewer.cleanup();
        }
        pnl_viewer.removeAll();
        notesViewer = nViewer;
        pnl_viewer.add(notesViewer.getComponent());
        noteListener.setViewerComponent(notesViewer);
    }

    private void switchMode(NotesViewer.DisplayMode mode, RhythmVoice rv)
    {
        switch (mode)
        {
            case RealTimeNotes:
                spn_srcChannel.setEnabled(true);
                break;
            case HarmonyNotes:
                spn_srcChannel.setEnabled(false);
                break;
            default:
                throw new AssertionError(mode.name());
        }
        lbl_chordSymbol.setText(" ");
        lbl_scale.setText(" ");
        notesViewer.releaseAllNotes();
        notesViewer.setMode(mode, rv);
    }

    // =================================================================================
    // Private classes
    // =================================================================================
    private static class NotesViewerListener implements NoteListener
    {

        public static final long MIN_DURATION_MS = 100;
        private boolean enabled;
        private int receiveChannel;
        private NotesViewer viewerComponent;

        // Store the last Note On position in milliseconds for each note and each channel. Use -1 if initialized.
        private final long noteOnPosMs[][] = new long[16][128];
        // Cache the created Timers
        private final Timer timersCache[][] = new Timer[16][128];

        public NotesViewerListener()
        {
            reset();
            enabled = true;
        }

        @Override
        public synchronized void noteOn(long tick, int channel, int pitch, int velocity)
        {
            if (!enabled || receiveChannel != channel)
            {
                return;
            }
            noteOnPosMs[channel][pitch] = System.currentTimeMillis();
            getViewerComponent().realTimeNoteOn(pitch, velocity);
        }

        @Override
        public synchronized void noteOff(long tick, int channel, int pitch)
        {
            if (!enabled || receiveChannel != channel)
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
                        getViewerComponent().realTimeNoteOff(pitch);
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
                SwingUtilities.invokeLater(() -> getViewerComponent().realTimeNoteOff(pitch));
            }
        }

        /**
         * @return the receiveChannel
         */
        public synchronized int getReceiveChannel()
        {
            return receiveChannel;
        }

        /**
         * @param receiveChannel the receiveChannel to set
         */
        public synchronized void setReceiveChannel(int receiveChannel)
        {
            boolean b = enabled;
            setEnabled(false);
            this.receiveChannel = receiveChannel;
            reset();
            setEnabled(b);
        }

        /**
         * @return the enabled
         */
        public synchronized boolean isEnabled()
        {
            return enabled;
        }

        /**
         * @param enabled the enabled to set
         */
        public synchronized void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        /**
         * @return the viewerComponent
         */
        public synchronized NotesViewer getViewerComponent()
        {
            return viewerComponent;
        }

        /**
         * @param viewerComponent the viewerComponent to set
         */
        public synchronized void setViewerComponent(NotesViewer viewerComponent)
        {
            boolean b = enabled;
            setEnabled(false);
            this.viewerComponent = viewerComponent;
            reset();
            setEnabled(b);

        }

        private final void reset()
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

    }

    private static class MySpinnerModel extends AbstractSpinnerModel
    {

        MidiMix midiMix;
        List<Integer> usedChannels;
        int channelIndex;

        MySpinnerModel(MidiMix midiMix)
        {
            this.midiMix = midiMix;
            usedChannels = midiMix.getUsedChannels();
            channelIndex = 0;
        }

        @Override
        public Object getValue()
        {
            return getStringValue(getChannel());
        }

        public int getChannel()
        {
            return usedChannels.get(channelIndex);
        }

        public void setChannel(int channel)
        {
            channelIndex = usedChannels.indexOf(channel);
            if (channelIndex == -1)
            {
                channelIndex = 0;
            }
            fireStateChanged();
        }

        @Override
        public void setValue(Object o)
        {
            fireStateChanged();
        }

        @Override
        public Object getNextValue()
        {
            channelIndex++;
            if (channelIndex >= usedChannels.size())
            {
                channelIndex = 0;
            }
            return getStringValue(getChannel());
        }

        @Override
        public Object getPreviousValue()
        {
            channelIndex--;
            if (channelIndex < 0)
            {
                channelIndex = usedChannels.size() - 1;
            }
            return getStringValue(getChannel());
        }

        private String getStringValue(int ch)
        {
            Instrument ins = midiMix.getInstrumentMixFromChannel(getChannel()).getInstrument();
            RhythmVoice rv = midiMix.getRhythmVoice(ch);
            return "[" + (ch + 1) + "] " + ins.getPatchName() + " / " + rv.getName();
        }

        private int getLongestValueLength()
        {
            int max = 5;
            for (var channel : usedChannels)
            {
                int strLength = getStringValue(channel).length();
                max = Math.max(max, strLength);
            }
            return max;
        }

    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_chordSymbol;
    private javax.swing.JLabel lbl_scale;
    private javax.swing.JPanel pnl_buttons;
    private javax.swing.JPanel pnl_chordSymbol;
    private javax.swing.JPanel pnl_viewer;
    private javax.swing.JSpinner spn_srcChannel;
    // End of variables declaration//GEN-END:variables

}
