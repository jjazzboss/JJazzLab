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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Item;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.NoteListener;
import org.jjazz.musiccontrol.api.PlaybackListenerAdapter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.notesviewer.spi.NotesViewer;
import org.jjazz.ui.cl_editor.api.CL_ContextActionListener;
import org.jjazz.ui.cl_editor.api.CL_ContextActionSupport;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.flatcomponents.api.BorderManager;
import org.jjazz.ui.flatcomponents.api.FlatButton;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

/**
 * Real time viewer panel
 */
public class NotesViewerPanel extends javax.swing.JPanel implements PropertyChangeListener, CL_ContextActionListener, ActionListener
{
    
    private static final Border BORDER_NOTHING_SELECTED = BorderFactory.createLineBorder(Color.RED);
    private static final Border BORDER_NOTHING_UNSELECTED = BorderManager.DEFAULT_BORDER_NOTHING;
    private static final Border BORDER_ENTERED_SELECTED = BORDER_NOTHING_SELECTED;
    private static final Border BORDER_ENTERED_UNSELECTED = BorderManager.DEFAULT_BORDER_ENTERED;
    private static final Border BORDER_PRESSED = BorderFactory.createLineBorder(Color.RED.darker());
    
    private NotesViewer notesViewer;
    private Song songPlaybackMode, songSelectionMode;
    private CLI_ChordSymbol selectedChordSymbol;
    private MidiMix midiMixPlaybackMode, midiMixSelectionMode;
    private final NotesViewerListener noteListener;
    private final Font chordSymbolFont;
    private HashMap<NotesViewer, FlatButton> mapViewerButton = new HashMap<>();
    private CL_ContextActionSupport cap;
    
    private static final Logger LOGGER = Logger.getLogger(NotesViewerPanel.class.getSimpleName());

    /**
     * Creates new form RtViewerPanel
     */
    public NotesViewerPanel()
    {

        // Used by initComponents
        chordSymbolFont = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(Font.BOLD, 16f);
        
        initComponents();
        
        cmb_srcChannel.addActionListener(this);

        // Get the chord symbols changes
        MusicController mc = MusicController.getInstance();
        mc.addPlaybackListener(new PlaybackListenerAdapter()
        {
            @Override
            public void chordSymbolChanged(CLI_ChordSymbol cliCs)
            {
                if (isUIinPlaybackMode())
                {
                    updateCurrentChordSymbolUI(cliCs);
                }
            }
        });

        // Get the playback state changes
        mc.addPropertyChangeListener(this);

        // Get the incoming notes to update the keyboard
        noteListener = new NotesViewerListener();
        mc.addNoteListener(noteListener);

        // Initialize the viewers
        setActiveNotesViewer(initNotesViewers());
        modeChanged();

        // Listen to active song changes
        var asm = ActiveSongManager.getInstance();
        asm.addPropertyListener(this);

        // Listen to selection changes in the current leadsheet editor
        cap = CL_ContextActionSupport.getInstance(Utilities.actionsGlobalContext());
        cap.addListener(this);
    }
    
    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        cmb_srcChannel.setEnabled(b);
        lbl_chordSymbol.setEnabled(b);
        lbl_scale.setEnabled(b);
        notesViewer.setEnabled(b);
    }


    // -----------------------------------------------------------------------------
    // CL_ContextActionListener interface
    // -----------------------------------------------------------------------------   
    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        
        CLI_ChordSymbol newSelectedChordSymbol = null;
        
        var chordSymbols = selection.getSelectedChordSymbols();
        if (!chordSymbols.isEmpty())
        {
            newSelectedChordSymbol = chordSymbols.get(0);
            
        } else if (selection.isBarSelectedWithinCls())
        {
            // Find the last chord valid for this bar
            var cls = selection.getChordLeadSheet();
            newSelectedChordSymbol = cls.getLastItem(0, selection.geMinBarIndex(), CLI_ChordSymbol.class);
            assert newSelectedChordSymbol != null;       // Chord symbol presence is mandatory at the beginning of a section
        } else
        {
            // Not a valid selection
            // Do nothing
            // Note: an empty selection is received when switching from a CL_Editor TopComponent to a different TopComponent
            return;
        }

        // Replace current chord symbol
        if (selectedChordSymbol != null)
        {
            selectedChordSymbol.removePropertyChangeListener(this);
        }
        selectedChordSymbol = newSelectedChordSymbol;
        if (selectedChordSymbol != null)
        {
            selectedChordSymbol.addPropertyChangeListener(this);
        }
        
        
        if (!isUIinPlaybackMode())
        {
            if (selectedChordSymbol != null)
            {
                showChordSymbolNotes(selectedChordSymbol);
            } else
            {
                // notesViewer.releaseAllNotes();       // Commented out: leave last chord symbol
            }
        }
    }
    
    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        // Nothing
    }

    // =================================================================================
    // ActionListener implementation 
    // =================================================================================
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == cmb_srcChannel)
        {
            channelChanged();
        }
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
            if (evt.getPropertyName().equals(MusicController.PROP_STATE) && notesViewer.getMode().equals(NotesViewer.Mode.ShowBackingTrack))
            {
                MusicController.State state = (MusicController.State) evt.getNewValue();
                switch (state)
                {
                    case DISABLED:  // Fall down
                    case STOPPED:
                        org.jjazz.ui.utilities.api.Utilities.invokeLaterIfNeeded(() -> lbl_chordSymbol.setText(" "));
                        org.jjazz.ui.utilities.api.Utilities.invokeLaterIfNeeded(() -> lbl_scale.setText(" "));
                        notesViewer.releaseAllNotes();
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
        } else if (evt.getSource() == midiMixPlaybackMode)
        {
            if (MidiMix.PROP_CHANNEL_INSTRUMENT_MIX.equals(evt.getPropertyName()))
            {
                updateComboModel();
            }
        } else if (evt.getSource() == selectedChordSymbol)
        {
            if (Item.PROP_ITEM_DATA.equals(evt.getPropertyName()))
            {
                if (!isUIinPlaybackMode())
                {
                    showChordSymbolNotes(selectedChordSymbol);
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
        btn_group = new javax.swing.ButtonGroup();
        pnl_viewer = new javax.swing.JPanel();
        pnl_chordSymbol = new javax.swing.JPanel();
        pnl_low = new javax.swing.JPanel();
        pnl_buttons = new javax.swing.JPanel();
        pnl_help = new javax.swing.JPanel();
        lbl_helpIcon = new javax.swing.JLabel();
        rbtn_playback = new javax.swing.JRadioButton();
        rbtn_selection = new javax.swing.JRadioButton();
        pnl_chordSymbolScaleName = new javax.swing.JPanel();
        lbl_chordSymbol = new javax.swing.JLabel();
        lbl_scale = new javax.swing.JLabel();
        cmb_srcChannel = new javax.swing.JComboBox<>();

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

        pnl_chordSymbol.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        pnl_low.setOpaque(false);
        pnl_low.setLayout(new java.awt.BorderLayout());

        pnl_buttons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 3, 0));
        pnl_low.add(pnl_buttons, java.awt.BorderLayout.WEST);
        pnl_buttons.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(NotesViewerPanel.class, "NotesViewerPanel.pnl_buttons.AccessibleContext.accessibleName")); // NOI18N

        pnl_help.setLayout(new javax.swing.BoxLayout(pnl_help, javax.swing.BoxLayout.LINE_AXIS));

        lbl_helpIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/notesviewer/resources/HelpIcon16x16.png"))); // NOI18N
        lbl_helpIcon.setToolTipText(org.openide.util.NbBundle.getMessage(NotesViewerPanel.class, "NotesViewerPanel.lbl_helpIcon.toolTipText")); // NOI18N
        pnl_help.add(lbl_helpIcon);

        pnl_low.add(pnl_help, java.awt.BorderLayout.EAST);

        btn_group.add(rbtn_playback);
        rbtn_playback.setFont(rbtn_playback.getFont().deriveFont(rbtn_playback.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_playback, org.openide.util.NbBundle.getBundle(NotesViewerPanel.class).getString("NotesViewerPanel.rbtn_playback.text")); // NOI18N
        rbtn_playback.setToolTipText(org.openide.util.NbBundle.getBundle(NotesViewerPanel.class).getString("NotesViewerPanel.rbtn_playback.toolTipText")); // NOI18N
        rbtn_playback.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_playbackActionPerformed(evt);
            }
        });

        btn_group.add(rbtn_selection);
        rbtn_selection.setFont(rbtn_selection.getFont().deriveFont(rbtn_selection.getFont().getSize()-1f));
        rbtn_selection.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_selection, org.openide.util.NbBundle.getBundle(NotesViewerPanel.class).getString("NotesViewerPanel.rbtn_selection.text")); // NOI18N
        rbtn_selection.setToolTipText(org.openide.util.NbBundle.getBundle(NotesViewerPanel.class).getString("NotesViewerPanel.rbtn_selection.toolTipText")); // NOI18N
        rbtn_selection.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_selectionActionPerformed(evt);
            }
        });

        pnl_chordSymbolScaleName.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 0));

        lbl_chordSymbol.setFont(chordSymbolFont);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_chordSymbol, "C7"); // NOI18N
        pnl_chordSymbolScaleName.add(lbl_chordSymbol);

        lbl_scale.setFont(lbl_scale.getFont().deriveFont(lbl_scale.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_scale, "Lydian"); // NOI18N
        pnl_chordSymbolScaleName.add(lbl_scale);

        cmb_srcChannel.setFont(cmb_srcChannel.getFont().deriveFont(cmb_srcChannel.getFont().getSize()-1f));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(rbtn_selection)
                        .addGap(10, 10, 10)
                        .addComponent(rbtn_playback)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmb_srcChannel, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(pnl_chordSymbol, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnl_low, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addComponent(pnl_chordSymbolScaleName, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_viewer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbtn_playback)
                    .addComponent(rbtn_selection)
                    .addComponent(cmb_srcChannel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnl_chordSymbolScaleName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_chordSymbol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_viewer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_low, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void rbtn_playbackActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_playbackActionPerformed
    {//GEN-HEADEREND:event_rbtn_playbackActionPerformed
        modeChanged();
    }//GEN-LAST:event_rbtn_playbackActionPerformed

    private void rbtn_selectionActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_selectionActionPerformed
    {//GEN-HEADEREND:event_rbtn_selectionActionPerformed
        modeChanged();
    }//GEN-LAST:event_rbtn_selectionActionPerformed

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
        if (sg != null && mm != null)
        {
            
            if (midiMixPlaybackMode != null)
            {
                midiMixPlaybackMode.removePropertyChangeListener(this);
            }
            
            songPlaybackMode = sg;
            midiMixPlaybackMode = mm;

            // Listen to midiMix changes
            mm.addPropertyChangeListener(this);

            // Update model and NotesViewer context
            updateComboModel();
            
            if (isUIinPlaybackMode())
            {
                cmb_srcChannel.setEnabled(true);        // If there was no active song before
                lbl_chordSymbol.setText(" ");
                lbl_scale.setText(" ");
            }
        } else
        {
            cmb_srcChannel.setEnabled(false);
        }
    }
    
    private void channelChanged()
    {
        // Update note listener and reset notesViewer
        noteListener.setReceiveChannel(getChannel());
        notesViewer.setContext(songPlaybackMode, midiMixPlaybackMode, midiMixPlaybackMode.getRhythmVoice(getChannel()));
        notesViewer.releaseAllNotes();
    }
    
    private int getChannel()
    {
        return ((ComboChannelElement) cmb_srcChannel.getSelectedItem()).channel;
    }
    
    private void updateComboModel()
    {
        // Disable listening while updating the model
        cmb_srcChannel.removeActionListener(this);


        // Save possible selected channel
        ComboChannelElement lastSelection = (ComboChannelElement) cmb_srcChannel.getSelectedItem();  // Can be null


        // Create and set model
        Vector<ComboChannelElement> data = new Vector<>();
        for (int ch : midiMixPlaybackMode.getUsedChannels())
        {
            var element = new ComboChannelElement(midiMixPlaybackMode, ch);
            data.add(element);
            if (lastSelection != null && lastSelection.channel == ch)
            {
                lastSelection = element;
            }
        }
        cmb_srcChannel.setModel(new DefaultComboBoxModel<>(data));
        cmb_srcChannel.setPrototypeDisplayValue(new ComboChannelElement(null, 0));


        // Relisten to changes
        cmb_srcChannel.addActionListener(this);

        // Restore saved channel
        if (lastSelection != null)
        {
            cmb_srcChannel.setSelectedItem(lastSelection);       // This will fire a action event so channelChanged() will be called
        } 
                
    }

    /**
     * Get all the NotesViewers available in the global lookup and add a button for each one.
     *
     * @return The default NotesViewer
     */
    private NotesViewer initNotesViewers()
    {
        Collection<? extends NotesViewer> nViewers = Lookup.getDefault().lookupAll(NotesViewer.class);
        assert !nViewers.isEmpty();
        
        for (var nViewer : nViewers)
        {
            FlatButton btn = new FlatButton();
            btn.setIcon(nViewer.getIcon());
            btn.setToolTipText(nViewer.getDescription());
            btn.addActionListener(ae -> setActiveNotesViewer(nViewer));
            btn.setBorderPressed(BORDER_PRESSED);
            pnl_buttons.add(btn);
            mapViewerButton.put(nViewer, btn);
        }
        
        return nViewers.iterator().next();
    }
    
    private void setActiveNotesViewer(NotesViewer nViewer)
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
            mapViewerButton.get(notesViewer).setBorderNothing(BORDER_NOTHING_UNSELECTED);
            mapViewerButton.get(notesViewer).setBorderEntered(BORDER_ENTERED_UNSELECTED);
        }
        pnl_viewer.removeAll();
        notesViewer = nViewer;
        pnl_viewer.add(notesViewer.getComponent());
        if (!isUIinPlaybackMode() && selectedChordSymbol != null)
        {
            showChordSymbolNotes(selectedChordSymbol);
        }
        pnl_viewer.revalidate();
        pnl_viewer.repaint();
        noteListener.setViewerComponent(notesViewer);
        mapViewerButton.get(notesViewer).setBorderNothing(BORDER_NOTHING_SELECTED);
        mapViewerButton.get(notesViewer).setBorderEntered(BORDER_ENTERED_SELECTED);
    }
    
    private void modeChanged()
    {
        NotesViewer.Mode mode = isUIinPlaybackMode() ? NotesViewer.Mode.ShowBackingTrack : NotesViewer.Mode.ShowSelection;
        switch (mode)
        {
            case ShowBackingTrack:
                noteListener.setEnabled(true);
                cmb_srcChannel.setEnabled(true);
                notesViewer.setMode(mode);
                lbl_chordSymbol.setText(" ");
                lbl_scale.setText(" ");
                break;
            case ShowSelection:
                noteListener.setEnabled(false);
                cmb_srcChannel.setEnabled(false);
                notesViewer.setMode(mode);
                if (selectedChordSymbol != null)
                {
                    showChordSymbolNotes(selectedChordSymbol);
                } else
                {
                    lbl_chordSymbol.setText(" ");
                    lbl_scale.setText(" ");
                }
                break;
            default:
                throw new AssertionError(mode.name());
        }
    }
    
    private boolean isUIinPlaybackMode()
    {
        return rbtn_playback.isSelected();
    }
    
    private void showChordSymbolNotes(CLI_ChordSymbol cliCs)
    {
        updateCurrentChordSymbolUI(cliCs);
        
        notesViewer.releaseAllNotes();
        var ecs = cliCs.getData();
        var ssi = ecs.getRenderingInfo().getScaleInstance();
        if (ssi != null)
        {
            notesViewer.showScaleNotes(ssi);
        }
        notesViewer.showChordSymbolNotes(cliCs);
        
    }
    
    private void updateCurrentChordSymbolUI(CLI_ChordSymbol cliCs)
    {
        var ecs = cliCs.getData();
        lbl_chordSymbol.setText(ecs.getOriginalName());
        var scale = ecs.getRenderingInfo().getScaleInstance();
        lbl_scale.setText(scale == null ? " " : scale.toString());
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
    
    private class ComboChannelElement
    {
        
        int channel;
        String stringValue;
        
        public ComboChannelElement(MidiMix mm, int channel)
        {
            this.channel = channel;
            this.stringValue = "[" + (channel + 1) + "] / unknown";
            if (mm != null)
            {
                var insMix = midiMixPlaybackMode.getInstrumentMixFromChannel(channel);
                if (insMix != null)
                {
                    Instrument ins = insMix.getInstrument();
                    RhythmVoice rv = midiMixPlaybackMode.getRhythmVoice(channel);
                    stringValue = "[" + (channel + 1) + "] " + ins.getPatchName() + " / " + rv.getName();
                }
            }
        }
        
        @Override
        public String toString()
        {
            return stringValue;
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup btn_group;
    private javax.swing.JComboBox<ComboChannelElement> cmb_srcChannel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lbl_chordSymbol;
    private javax.swing.JLabel lbl_helpIcon;
    private javax.swing.JLabel lbl_scale;
    private javax.swing.JPanel pnl_buttons;
    private javax.swing.JPanel pnl_chordSymbol;
    private javax.swing.JPanel pnl_chordSymbolScaleName;
    private javax.swing.JPanel pnl_help;
    private javax.swing.JPanel pnl_low;
    private javax.swing.JPanel pnl_viewer;
    private javax.swing.JRadioButton rbtn_playback;
    private javax.swing.JRadioButton rbtn_selection;
    // End of variables declaration//GEN-END:variables

}
