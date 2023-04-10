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
import org.jjazz.ui.cl_editor.barbox.api.BarBox;
import org.jjazz.ui.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.ui.cl_editor.barbox.api.BarBoxSettings;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererFactory;
import org.jjazz.ui.utilities.api.Utilities;
import org.openide.util.Exceptions;

/**
 * Display the currently playing chord symbol.
 */
public class EasyReaderPanel2 extends JPanel implements PropertyChangeListener, PlaybackListener
{

    private static final int PRE_FIRE_EVENT_MS = 100;
    private Song song;
    private SongChordSequence songChordSequence;
    private final Position posModel;
    private BarBox barBox;
    private float posInBeatsModel;
    private CLI_ChordSymbol chord;
    private float chordPosInBeats;
    private CLI_ChordSymbol nextChord;
    private float nextChordPosInBeats;
    private Position position;
    private SongMusicGenerationListener songMusicGenerationListener;
    private static final Logger LOGGER = Logger.getLogger(EasyReaderPanel2.class.getSimpleName());

    public EasyReaderPanel2()
    {
        initComponents();
        posModel = new Position();

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


            if (barBox != null)
            {
                barBox.cleanup();
                pnl_barBox.remove(barBox);
            }

            barBox = new BarBox(null, 0, -1,
                    song.getChordLeadSheet(),
                    new BarBoxConfig(BarRendererFactory.BR_CHORD_SYMBOL, BarRendererFactory.BR_CHORD_POSITION),
                    BarBoxSettings.getDefault(),
                    BarRendererFactory.getDefault());
            pnl_barBox.add(barBox);
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
        });
    }

    @Override
    public void barChanged(int oldBar, int newBar, float newPosInBeats)
    {
        LOGGER.severe("barChanged() newBar=" + newBar + " newPosInBeats=" + newPosInBeats);
        if (barBox != null)
        {
            barBox.setModelBarIndex(newBar);
        }
    }

    @Override
    public void chordSymbolChanged(CLI_ChordSymbol newChord)
    {
        LOGGER.severe("chordSymbolChanged() newChord=" + newChord);

//        SwingUtilities.invokeLater(() -> 
//        {
//            chord = newChord;
//            synchronized (this)
//            {
//                nextChord = songChordSequence.higher(chord);        // Might be null
//            }
//
//            chordPosInBeats = song.getSongStructure().getPositionInNaturalBeats(chord.getPosition());
//            nextChordPosInBeats = nextChord != null ? song.getSongStructure().getPositionInNaturalBeats(nextChord.getPosition()) : -1f;
//
//
//            // Update chords UI
//            String txt = chord.getData().getOriginalName();
//            String nextTxt = nextChord != null ? nextChord.getData().getOriginalName() : "-";
//            pnl_barBox.revalidate();
//        });
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
                // generateChordSequence();
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

    // =================================================================================================
    // Inner classes
    // =================================================================================================
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_barBox = new javax.swing.JPanel();
        pnl_posSection = new javax.swing.JPanel();
        pnl_pos = new javax.swing.JPanel();
        posViewer = new org.jjazz.ui.musiccontrolactions.ui.api.PositionViewer();
        pnl_songPart = new javax.swing.JPanel();
        lbl_songPart = new javax.swing.JLabel();
        lbl_nextChord = new javax.swing.JLabel();

        pnl_barBox.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        pnl_barBox.setLayout(new java.awt.BorderLayout());

        pnl_posSection.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        posViewer.setFont(new java.awt.Font("Courier New", 1, 18)); // NOI18N
        posViewer.setTimeShown(false);
        pnl_pos.add(posViewer);

        pnl_posSection.add(pnl_pos);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_songPart, org.openide.util.NbBundle.getMessage(EasyReaderPanel2.class, "EasyReaderPanel2.lbl_songPart.text")); // NOI18N
        pnl_songPart.add(lbl_songPart);

        pnl_posSection.add(pnl_songPart);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_nextChord, org.openide.util.NbBundle.getMessage(EasyReaderPanel2.class, "EasyReaderPanel2.lbl_nextChord.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_posSection, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnl_barBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_nextChord)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(pnl_posSection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pnl_barBox, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_nextChord))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lbl_nextChord;
    private javax.swing.JLabel lbl_songPart;
    private javax.swing.JPanel pnl_barBox;
    private javax.swing.JPanel pnl_pos;
    private javax.swing.JPanel pnl_posSection;
    private javax.swing.JPanel pnl_songPart;
    private org.jjazz.ui.musiccontrolactions.ui.api.PositionViewer posViewer;
    // End of variables declaration//GEN-END:variables


}
