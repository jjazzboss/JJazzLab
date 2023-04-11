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
package org.jjazz.easyreader;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListener;
import org.jjazz.musiccontrol.api.SongMusicGenerationListener;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.utilities.api.Utilities;
import org.openide.util.Exceptions;

/**
 * Display the currently playing chord symbol.
 */
public class EasyReaderPanel extends JPanel implements PropertyChangeListener, PlaybackListener
{

    private static final int PRE_FIRE_EVENT_MS = 100;
    private Song song;
    private SongChordSequence songChordSequence;
    private final Position posModel;
    private float posInBeatsModel;
    // private final RulerPanel rulerPanel;
    private CLI_ChordSymbol chord;
    private float chordPosInBeats;
    private CLI_ChordSymbol nextChord;
    private float nextChordPosInBeats;
    private Position position;
    private MyLayoutManager myLayoutManager;
    private SongMusicGenerationListener songMusicGenerationListener;
    private static final Logger LOGGER = Logger.getLogger(EasyReaderPanel.class.getSimpleName());

    public EasyReaderPanel()
    {
        initComponents();
        posModel = new Position();
        myLayoutManager = new MyLayoutManager();
        pnl_chords.setLayout(myLayoutManager);
        pnl_ruler.setLayout(myLayoutManager);
    }

    public void cleanup()
    {
        setModel(null);
    }

    /**
     * Set the song.
     *
     * @param song Can be null.
     */
    public void setModel(Song song)
    {
        LOGGER.severe("setModel() song=" + song);

        if (this.song == song)
        {
            return;
        }

        if (this.song != null)
        {
            MusicController.getInstance().removePropertyChangeListener(this);
            MusicController.getInstance().removePlaybackListener(this);
            songMusicGenerationListener.removePropertyChangeListener(this);
            songMusicGenerationListener.cleanup();
        }


        this.song = song;
        this.posModel.setBar(0);
        this.posModel.setFirstBarBeat();
        this.posViewer.setModel(this.song, posModel);

        setEnabled(this.song != null);


        if (this.song != null)
        {
            MusicController.getInstance().addPropertyChangeListener(this);
            MusicController.getInstance().addPlaybackListener(this);
            MidiMix midiMix;
            try
            {
                midiMix = MidiMixManager.getInstance().findMix(this.song);
            } catch (MidiUnavailableException ex)
            {
                // Should never happen
                Exceptions.printStackTrace(ex);
                return;
            }
            songMusicGenerationListener = new SongMusicGenerationListener(this.song, midiMix, PRE_FIRE_EVENT_MS);   // We could set a blacklist to avoid MidiMix/PlaybackSettings changes...
            songMusicGenerationListener.addPropertyChangeListener(this);

            generateChordSequence();
        }

    }

    public Song getModel()
    {
        return song;
    }

    public CLI_ChordSymbol getChord()
    {
        return chord;
    }

    public CLI_ChordSymbol getNextChord()
    {
        return nextChord;
    }

    @Override
    public void setEnabled(boolean b)
    {
        LOGGER.severe("setEnabled() b=" + b);
        super.setEnabled(b);
        Utilities.setRecursiveEnabled(b, this);
    }

    // ======================================================================
    // PlaybackListener interface
    // ======================================================================   

    @Override
    public void enabledChanged(boolean b)
    {
        if (!b)
        {
            setEnabled(false);
        }
    }

    @Override
    public void beatChanged(Position oldPos, Position newPos, float newPosInBeats)
    {
        if (!isEnabled())
        {
            return;
        }
        LOGGER.severe("beatChanged() newPos=" + newPos + " newPosInBeats=" + newPosInBeats);
        SwingUtilities.invokeLater(() -> 
        {
            posInBeatsModel = newPosInBeats;
            posModel.set(newPos);
            pnl_ruler.revalidate();
        });
    }

    @Override
    public void chordSymbolChanged(CLI_ChordSymbol newChord)
    {
        LOGGER.severe("chordSymbolChanged() newChord=" + newChord);

        SwingUtilities.invokeLater(() -> 
        {
            chord = newChord;
            synchronized (this)
            {
                nextChord = songChordSequence.higher(chord);        // Might be null
            }

            chordPosInBeats = song.getSongStructure().toPositionInNaturalBeats(chord.getPosition());
            nextChordPosInBeats = nextChord != null ? song.getSongStructure().toPositionInNaturalBeats(nextChord.getPosition()) : -1f;


            // Update chords UI
            String txt = chord.getData().getOriginalName();
            String nextTxt = nextChord != null ? nextChord.getData().getOriginalName() : "-";
            lbl_chord.setText(txt);
            lbl_chordNext.setText(nextTxt);
            pnl_chords.revalidate();
        });
    }

    @Override
    public void songPartChanged(SongPart spt)
    {
        // Nothing
    }

    @Override
    public void midiActivity(long tick, int channel)
    {
        // Nothing
    }


    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================   
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        var mc = MusicController.getInstance();
        if (evt.getSource() == songMusicGenerationListener)
        {
            if (evt.getPropertyName().equals(SongMusicGenerationListener.PROP_CHANGED))
            {
                // We can be notified out of the Swing EDT 
                generateChordSequence();
            }
        } else if (evt.getSource() == mc)
        {
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                switch ((MusicController.State) evt.getNewValue())
                {
                    case DISABLED ->
                    {
                    }
                    case STOPPED ->
                    {

                    }
                    case PAUSED ->
                    {
                    }
                    case PLAYING ->
                    {

                    }
                    default -> throw new AssertionError(((MusicController.State) evt.getNewValue()).name());

                }
            }
        }
    }

    // =================================================================================================
    // Private methods
    // =================================================================================================
    private synchronized void generateChordSequence()
    {
        try
        {
            songChordSequence = new SongChordSequence(this.song, null);
        } catch (UserErrorGenerationException ex)
        {
            // Do nothing, we'll retry on the next song change
        }
    }

    // =================================================================================================
    // Inner classes
    // =================================================================================================

    /**
     * Our layout manager
     */
    public class MyLayoutManager implements LayoutManager
    {

        private int chordX, nextChordX;
        private float oneBeatSizeInPixels;


        @Override
        public void layoutContainer(Container parent)
        {
            if (parent == pnl_chords)
            {
                layoutChordSymbols();
            } else if (parent == pnl_ruler)
            {
                layoutRuler();
            }
        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            return new Dimension(300, 100);
        }

        @Override
        public void addLayoutComponent(String name, Component comp)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return preferredLayoutSize(parent);
        }

        public int getChordX()
        {
            return chordX;
        }

        public int getNextChordX()
        {
            return nextChordX;
        }

        public float getOneBeatSizeInPixels()
        {
            return oneBeatSizeInPixels;
        }

        private void layoutChordSymbols()
        {
            Rectangle r = Utilities.getUsableArea(pnl_chords);
            final float NB_BEATS = 6;
            int x = r.x;

            // Chord
            var chordSize = lbl_chord.getPreferredSize();
            int y = (r.height - chordSize.height) / 2;
            chordX = x;
            lbl_chord.setLocation(x, y);
            lbl_chord.setSize(chordSize);


            // Next chord
            var nextChordSize = lbl_chordNext.getPreferredSize();
            y += chordSize.height - nextChordSize.height;
            oneBeatSizeInPixels = (r.width - chordSize.width - nextChordSize.width) / NB_BEATS;
            x += chordSize.width + Math.round(Math.min(nextChordPosInBeats - chordPosInBeats, NB_BEATS) * oneBeatSizeInPixels);
            nextChordX = x;
            lbl_chordNext.setBounds(x, y, nextChordSize.width, nextChordSize.height);
        }

        private void layoutRuler()
        {
            Rectangle r = Utilities.getUsableArea(pnl_ruler);
            if (chord !=null && posModel.compareTo(chord.getPosition()) >= 0 && nextChord != null && posModel.compareTo(nextChord.getPosition()) <= 0)
            {
                var markerSize = lbl_marker.getPreferredSize();
                int y = (r.height - markerSize.height) / 2;
                int x = nextChordX - (int) (oneBeatSizeInPixels * (nextChordPosInBeats - posInBeatsModel));
                lbl_marker.setBounds(x, y, markerSize.width, markerSize.height);
            }
        }
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_chords = new javax.swing.JPanel();
        lbl_chord = new javax.swing.JLabel();
        lbl_chordNext = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.api.HelpTextArea();
        pnl_posSection = new javax.swing.JPanel();
        pnl_pos = new javax.swing.JPanel();
        posViewer = new org.jjazz.ui.musiccontrolactions.ui.api.PositionViewer();
        pnl_songPart = new javax.swing.JPanel();
        lbl_songPart = new javax.swing.JLabel();
        pnl_ruler = new javax.swing.JPanel();
        lbl_marker = new javax.swing.JLabel();

        pnl_chords.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5);
        flowLayout1.setAlignOnBaseline(true);
        pnl_chords.setLayout(flowLayout1);

        lbl_chord.setFont(lbl_chord.getFont().deriveFont(lbl_chord.getFont().getStyle() | java.awt.Font.BOLD, lbl_chord.getFont().getSize()+9));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_chord, org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.lbl_chord.text")); // NOI18N
        pnl_chords.add(lbl_chord);

        lbl_chordNext.setFont(lbl_chordNext.getFont().deriveFont(lbl_chordNext.getFont().getStyle() | java.awt.Font.BOLD, lbl_chordNext.getFont().getSize()+1));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_chordNext, org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.lbl_chordNext.text")); // NOI18N
        pnl_chords.add(lbl_chordNext);

        jScrollPane1.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(1);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.helpTextArea1.text")); // NOI18N
        jScrollPane1.setViewportView(helpTextArea1);

        pnl_posSection.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        posViewer.setFont(new java.awt.Font("Courier New", 1, 18)); // NOI18N
        posViewer.setTimeShown(false);
        pnl_pos.add(posViewer);

        pnl_posSection.add(pnl_pos);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_songPart, org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.lbl_songPart.text")); // NOI18N
        pnl_songPart.add(lbl_songPart);

        pnl_posSection.add(pnl_songPart);

        pnl_ruler.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        org.openide.awt.Mnemonics.setLocalizedText(lbl_marker, org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.lbl_marker.text")); // NOI18N

        javax.swing.GroupLayout pnl_rulerLayout = new javax.swing.GroupLayout(pnl_ruler);
        pnl_ruler.setLayout(pnl_rulerLayout);
        pnl_rulerLayout.setHorizontalGroup(
            pnl_rulerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_rulerLayout.createSequentialGroup()
                .addGap(108, 108, 108)
                .addComponent(lbl_marker)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnl_rulerLayout.setVerticalGroup(
            pnl_rulerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_rulerLayout.createSequentialGroup()
                .addGap(0, 7, Short.MAX_VALUE)
                .addComponent(lbl_marker))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_ruler, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1)
                    .addComponent(pnl_chords, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnl_posSection, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(pnl_posSection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_ruler, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(pnl_chords, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.ui.utilities.api.HelpTextArea helpTextArea1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_chord;
    private javax.swing.JLabel lbl_chordNext;
    private javax.swing.JLabel lbl_marker;
    private javax.swing.JLabel lbl_songPart;
    private javax.swing.JPanel pnl_chords;
    private javax.swing.JPanel pnl_pos;
    private javax.swing.JPanel pnl_posSection;
    private javax.swing.JPanel pnl_ruler;
    private javax.swing.JPanel pnl_songPart;
    private org.jjazz.ui.musiccontrolactions.ui.api.PositionViewer posViewer;
    // End of variables declaration//GEN-END:variables


}
