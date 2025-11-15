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
package org.jjazz.arranger;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.SpinnerListModel;
import org.jjazz.harmony.api.ChordSymbolFinder;
import org.jjazz.harmony.api.Note;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.instrumentcomponents.keyboard.api.KeyboardComponent;
import org.jjazz.instrumentcomponents.keyboard.api.KeyboardComponent.Orientation;
import org.jjazz.instrumentcomponents.keyboard.api.KeyboardRange;
import org.jjazz.outputsynth.api.FixMidiMix;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 * The arranger UI.
 */
public class ArrangerPanel extends javax.swing.JPanel implements PropertyChangeListener, SgsChangeListener
{

    private static final Color MARK_COLOR = new Color(187, 187, 187);
    private final Font chordSymbolFont;
    private Transmitter transmitterKbdComponent;
    protected Arranger arranger;        // Protected for test only!!! must be private!
    private Song song;
    private MidiMix midiMix;
    private SongPart songPart;
    private final ChordSymbolFinder chordSymbolFinder;
    private Transmitter transmitterChordSymbolFinder;
    private ChordReceiver chordReceiver;
    private final Future<?> chordSymbolFinderBuildFuture;
    private static final Logger LOGGER = Logger.getLogger(ArrangerPanel.class.getSimpleName());

    /**
     * Creates new form ArrangerPanel
     */
    public ArrangerPanel()
    {
        // Used by initComponents
        chordSymbolFont = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(Font.BOLD, 16f);
        initComponents();


        // Set spinner model
        String notes[] = new String[128];
        for (int i = 0; i < 128; i++)
        {
            notes[i] = new Note(i).toPianoOctaveString();
        }
        spn_splitNote.setModel(new SpinnerListModel(notes));

        // Prepare the data
        chordSymbolFinderBuildFuture = Executors.newSingleThreadExecutor().submit(() -> ChordSymbolFinder.buildStaticData());   // Can take a few seconds on slow computers
        chordSymbolFinder = new ChordSymbolFinder(4);
    }

    public void closing()
    {
        if (arranger != null && arranger.isPlaying())
        {
            arranger.stop();
        }
        if (transmitterChordSymbolFinder != null)
        {
            transmitterChordSymbolFinder.close();
        }
        if (transmitterKbdComponent != null)
        {
            transmitterKbdComponent.close();
        }
        if (chordReceiver != null)
        {
            chordReceiver.close();
        }
        if (song != null)
        {
            song.getSongStructure().removeSgsChangeListener(this);
            song.removePropertyChangeListener(this);
        }
    }

    public void opened()
    {
        if (transmitterKbdComponent != null)
        {
            transmitterKbdComponent.close();
        }
        transmitterKbdComponent = JJazzMidiSystem.getInstance().getJJazzMidiInDevice().getTransmitter();
        transmitterKbdComponent.setReceiver(new PianoReceiver());


        if (transmitterChordSymbolFinder != null)
        {
            transmitterChordSymbolFinder.close();
        }
        chordReceiver = new ChordReceiver();
        chordReceiver.addChordListener(this::processIncomingChord);
        updateSplitNoteUI(chordReceiver.getSplitNote());

        transmitterChordSymbolFinder = JJazzMidiSystem.getInstance().getJJazzMidiInDevice().getTransmitter();
        transmitterChordSymbolFinder.setReceiver(chordReceiver);
    }


    /**
     * Set the model to operate on.
     * <p>
     * Both values can be null in the same time.
     *
     * @param sg
     * @param mm
     */
    public void setModel(Song sg, MidiMix mm)
    {
        if (song != sg)
        {
            if (arranger != null)
            {
                arranger.stop();    // will set songPart to null as well
            }

            if (song != null)
            {
                song.getSongStructure().removeSgsChangeListener(this);
                song.removePropertyChangeListener(this);
            }

            song = sg;
            midiMix = mm;

            if (song != null)
            {
                song.getSongStructure().addSgsChangeListener(this);
                song.addPropertyChangeListener(this);
            }
        }

        refreshUI();
    }


    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        lbl_chordSymbol.setEnabled(b);
        lbl_rhythm.setEnabled(b);
        lbl_songPart.setEnabled(b);
        kbdComponent.setEnabled(b);
        refreshUI();
    }

    // ================================================================================    
    // PropertyChangeListener interface
    // ================================================================================   

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == arranger)
        {
            if (evt.getPropertyName().equals(Arranger.PROP_PLAYING))
            {
                if (arranger.isPlaying())
                {
                    refreshUI();
                } else
                {
                    arrangerStopped();
                }
            }
        } else if (evt.getSource() == song)
        {
            if (evt.getPropertyName().equals(Song.PROP_TEMPO))
            {
                if (arranger != null)
                {
                    arranger.updateTempo(song.getTempo());
                }
            }
        }
    }

    // ================================================================================    
    // SgsChangeListener interface
    // ================================================================================   

    @Override
    public void songStructureChanged(SgsChangeEvent e)
    {
        refreshUI();
    }

    // ================================================================================    
    // Private methods
    // ================================================================================    
    private void togglePlayPause()
    {
        LOGGER.log(Level.FINE, "togglePlayPause() --  isSelected()={0}", tbtn_playPause.isSelected());

        try
        {
            // Wait in case ChordSymbolFinder.buildStaticData() is not yet complete
            chordSymbolFinderBuildFuture.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
            return;
        }


        var jms = JJazzMidiSystem.getInstance();
        MusicController mc = MusicController.getInstance();


        if (tbtn_playPause.isSelected())
        {

            // Check everything is OK
            String errorMessage = null;
            SS_Editor ssEditor = null;
            SS_Selection selection = null;
            var spts = song.getSongStructure().getSongParts();


            if (jms.getDefaultInDevice() == null)
            {
                errorMessage = ResUtil.getString(getClass(), "ErrNoMidiInputDevice");
            } else if (mc.isPlaying())
            {
                errorMessage = ResUtil.getString(getClass(), "ErrSequenceAlreadyPlaying");
            } else if (song == null)
            {
                errorMessage = ResUtil.getString(getClass(), "ErrNoActiveSong");
            } else if (spts.isEmpty())
            {
                errorMessage = ResUtil.getString(getClass(), "ErrEmptySong");
            }

            // If error notify error and rollback UI
            if (errorMessage != null)
            {
                tbtn_playPause.setSelected(false);
                NotifyDescriptor nd = new NotifyDescriptor.Message(errorMessage, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                return;
            }


            SS_EditorTopComponent ssTc = SS_EditorTopComponent.get(song.getSongStructure());
            ssEditor = ssTc.getEditor();
            selection = new SS_Selection(ssEditor.getLookup());
            if (selection.isEmpty())
            {
                // Use first song part
                songPart = spts.get(0);
            } else
            {
                // Use first selected song part
                songPart = selection.isRhythmParameterSelected()
                        ? selection.getSelectedSongPartParameters().get(0).getSpt()
                        : selection.getSelectedSongParts().get(0);
            }


            // Reset chord symbol label
            lbl_chordSymbol.setText("-");


            // Make selection become only our song part
            selection.unselectAll(ssEditor);
            ssEditor.selectSongPart(songPart, true);


            // Prepare the arranger 
            SongContext sgContext = new SongContext(song, midiMix, songPart.getBarRange());

            FixMidiMix.checkAndPossiblyFix(midiMix, true);

            if (arranger != null)
            {
                arranger.removePropertyListener(this);
                arranger.cleanup();
            }
            arranger = new Arranger(sgContext);
            arranger.addPropertyListener(this);


            // Start playback
            try
            {
                arranger.play();            // Will generate an Arranger playing change event
            } catch (MusicGenerationException ex)
            {
                NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                // Rollback UI
                tbtn_playPause.setSelected(false);
                arranger.removePropertyListener(this);
                arranger.cleanup();
                songPart = null;
                arranger = null;
                return;
            }

            // Update chord symbol label
            CLI_ChordSymbol cliCs = arranger.getCurrentChordSymbol();
            if (cliCs != null)
            {
                lbl_chordSymbol.setText(cliCs.getData().getName());
            }

        } else
        {
            arranger.stop();        // Will generate an Arranger playing change event
        }
    }


    /**
     * Called by the ChordReceiver when a chord was changed.
     *
     * @param notes
     */
    protected void processIncomingChord(List<Note> notes)       // protected for testing, should be private
    {
        LOGGER.log(Level.FINE, "processIncomingChord() -- notes={0} nanoTime()={1}", new Object[]
        {
            notes, System.nanoTime()
        });
        if (notes.size() < 3 || notes.size() > chordSymbolFinder.getMaxNbNotes())
        {
            return;
        }
        var chordSymbols = chordSymbolFinder.find(notes);
        if (chordSymbols != null)
        {
            var chordSymbol = chordSymbolFinder.getChordSymbol(notes, chordSymbols, cb_lowerNoteIsBass.isSelected());
            if (chordSymbol != null)
            {
                if (arranger != null)
                {
                    arranger.updateChordSymbol(chordSymbol);
                }
                lbl_chordSymbol.setText(chordSymbol.getName());
            }
        }
    }


    /**
     * Update UI depending on the first SongPart of current song and the playback mode.
     *
     * @param spt
     */
    private void refreshUI()
    {
        LOGGER.log(Level.FINE, "updateSptUI() -- songPart={0}", songPart);
        String sSpt, sRhythm;
        if (songPart == null)
        {
            // Not playing
            sSpt = " "; // Must not be "" to keep JLabel height unchanged
            sRhythm = " ";
        } else
        {
            // Playing
            sSpt = ResUtil.getString(getClass(), "SongPartRef", songPart.getName(), songPart.getStartBarIndex() + 1);
            Rhythm r = songPart.getRhythm();
            var rpVariation = RP_SYS_Variation.getVariationRp(r);
            String strVariation = rpVariation == null ? "" : "/" + songPart.getRPValue(rpVariation);
            sRhythm = r.getName() + strVariation;
        }

        lbl_songPart.setText(sSpt);
        lbl_rhythm.setText(sRhythm);

        tbtn_playPause.setEnabled(song != null);

        boolean b = arranger != null && arranger.isPlaying()
                && songPart != null && song != null
                && song.getSongStructure().getSongParts().contains(songPart);
        lbl_songPart.setEnabled(b);
        lbl_rhythm.setEnabled(b);

    }

    private void updateSplitNoteUI(int splitNote)
    {
        kbdComponent.reset();
        kbdComponent.setMarked(splitNote, MARK_COLOR);
        spn_splitNote.setValue(new Note(splitNote).toPianoOctaveString());
    }

    private void arrangerStopped()
    {
        LOGGER.fine("arrangerStopped() --");
        arranger.cleanup();
        tbtn_playPause.setSelected(false);
        songPart = null;
        refreshUI();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        kbdComponent = new KeyboardComponent(KeyboardRange._88_KEYS,Orientation.DOWN, false);
        lbl_chordSymbol = new javax.swing.JLabel();
        lbl_songPart = new javax.swing.JLabel();
        lbl_rhythm = new javax.swing.JLabel();
        tbtn_playPause = new org.jjazz.flatcomponents.api.FlatToggleButton();
        cb_lowerNoteIsBass = new javax.swing.JCheckBox();
        flatHelpButton1 = new org.jjazz.flatcomponents.api.FlatHelpButton();
        spn_splitNote = new org.jjazz.flatcomponents.api.WheelSpinner();
        jLabel1 = new javax.swing.JLabel();

        kbdComponent.setPreferredSize(new java.awt.Dimension(300, 60));

        lbl_chordSymbol.setFont(chordSymbolFont);
        lbl_chordSymbol.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_chordSymbol, "Bb7M"); // NOI18N
        lbl_chordSymbol.setToolTipText(org.openide.util.NbBundle.getMessage(ArrangerPanel.class, "ArrangerPanel.lbl_chordSymbol.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_songPart, "Song Part \"A\" bar 23"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_rhythm, "SlowBossa-s232.sty - Main A"); // NOI18N

        tbtn_playPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/arranger/resources/PlayPause.png"))); // NOI18N
        tbtn_playPause.setToolTipText(org.openide.util.NbBundle.getMessage(ArrangerPanel.class, "ArrangerPanel.tbtn_playPause.toolTipText")); // NOI18N
        tbtn_playPause.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/arranger/resources/PlayPause-ON.png"))); // NOI18N
        tbtn_playPause.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tbtn_playPauseActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_lowerNoteIsBass, org.openide.util.NbBundle.getMessage(ArrangerPanel.class, "ArrangerPanel.cb_lowerNoteIsBass.text")); // NOI18N
        cb_lowerNoteIsBass.setToolTipText(org.openide.util.NbBundle.getMessage(ArrangerPanel.class, "ArrangerPanel.cb_lowerNoteIsBass.toolTipText")); // NOI18N
        cb_lowerNoteIsBass.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        flatHelpButton1.setHelpText(org.openide.util.NbBundle.getMessage(ArrangerPanel.class, "ArrangerPanel.flatHelpButton1.helpText")); // NOI18N

        spn_splitNote.setToolTipText(org.openide.util.NbBundle.getMessage(ArrangerPanel.class, "ArrangerPanel.spn_splitNote.toolTipText")); // NOI18N
        spn_splitNote.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_splitNoteStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(ArrangerPanel.class, "ArrangerPanel.jLabel1.text")); // NOI18N
        jLabel1.setToolTipText(spn_splitNote.getToolTipText());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(kbdComponent, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_chordSymbol, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(spn_splitNote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cb_lowerNoteIsBass, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(flatHelpButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbl_songPart)
                                    .addComponent(lbl_rhythm))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(tbtn_playPause, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_songPart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_rhythm))
                    .addComponent(tbtn_playPause, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(lbl_chordSymbol)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(kbdComponent, javax.swing.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(spn_splitNote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1)
                        .addComponent(cb_lowerNoteIsBass))
                    .addComponent(flatHelpButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tbtn_playPauseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tbtn_playPauseActionPerformed
    {//GEN-HEADEREND:event_tbtn_playPauseActionPerformed
        togglePlayPause();
    }//GEN-LAST:event_tbtn_playPauseActionPerformed

    private void spn_splitNoteStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_splitNoteStateChanged
    {//GEN-HEADEREND:event_spn_splitNoteStateChanged
        if (chordReceiver != null)
        {
            int splitNote = 0;
            try
            {
                Note note = Note.parsePianoOctaveString((String) spn_splitNote.getValue());
                splitNote = note.getPitch();
            } catch (ParseException ex)
            {
                // Should never happen
                Exceptions.printStackTrace(ex);
                return;
            }
            chordReceiver.setSplitNote(splitNote);
            updateSplitNoteUI(splitNote);
        }
    }//GEN-LAST:event_spn_splitNoteStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox cb_lowerNoteIsBass;
    private org.jjazz.flatcomponents.api.FlatHelpButton flatHelpButton1;
    private javax.swing.JLabel jLabel1;
    private org.jjazz.instrumentcomponents.keyboard.api.KeyboardComponent kbdComponent;
    private javax.swing.JLabel lbl_chordSymbol;
    private javax.swing.JLabel lbl_rhythm;
    private javax.swing.JLabel lbl_songPart;
    private org.jjazz.flatcomponents.api.WheelSpinner spn_splitNote;
    private org.jjazz.flatcomponents.api.FlatToggleButton tbtn_playPause;
    // End of variables declaration//GEN-END:variables


    // ================================================================================    
    // Private classes
    // ================================================================================ 
    private class PianoReceiver implements Receiver
    {

        @Override
        public void send(MidiMessage msg, long timeStamp)
        {
            if (!ArrangerPanel.this.isShowing())        // But might be showing AND hidden behind another TopComponent
            {
                return;
            }

            ShortMessage noteMsg = MidiUtilities.getNoteOnShortMessage(msg);
            if (noteMsg != null)
            {
                // Note ON
                int pitch = noteMsg.getData1();
                int velocity = noteMsg.getData2();

                kbdComponent.setPressed(pitch, velocity, null);

            } else if ((noteMsg = MidiUtilities.getNoteOffShortMessage(msg)) != null)
            {
                // Note OFF
                int pitch = noteMsg.getData1();
                kbdComponent.setReleased(pitch);
            }
        }

        @Override
        public void close()
        {
            // Nothing
        }

    }
}
