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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListener;
import org.jjazz.musiccontrol.api.playbacksession.SongContextProvider;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.utilities.api.Utilities;

/**
 * Display the currently playing chord symbol.
 */
public class EasyReaderPanel extends JPanel implements PropertyChangeListener, PlaybackListener
{

    private Song song;
    private SongChordSequence songChordSequence;
    private int scsIndex;
    private Position posModel;


    public EasyReaderPanel()
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
        if (this.song == song)
        {
            return;
        }

        if (this.song != null)
        {
            MusicController.getInstance().removePropertyChangeListener(this);
            MusicController.getInstance().removePlaybackListener(this);
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
        }

    }

    public Song getModel()
    {
        return song;
    }

    @Override
    public void setEnabled(boolean b)
    {
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
    public void beatChanged(Position oldPos, Position newPos)
    {
        if (!isEnabled())
        {
            return;
        }
        SwingUtilities.invokeLater(() -> 
        {
            posModel.set(newPos);
        });

    }

    @Override
    public void barChanged(int oldBar, int newBar)
    {
        if (songChordSequence == null)
        {
            return;
        }
        scsIndex = songChordSequence.indexOfFirstChordFromBar(newBar);
        CLI_ChordSymbol chord = null;
        CLI_ChordSymbol chordNext = null;
        if (scsIndex>=0)
        {
            chord =  songChordSequence.get(scsIndex);
            if (scsIndex<songChordSequence.size()-1)
            {
                chordNext = songChordSequence.get(scsIndex+1);
            }
        }
        lbl_chord.setText(chord==null ? "-" : chord.getData().getOriginalName());
        lbl_chordNext.setText(chordNext==null ? "-" : chordNext.getData().getOriginalName());
    }

    @Override
    public void chordSymbolChanged(CLI_ChordSymbol chordSymbol)
    {

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
        if (evt.getSource() == mc)
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
                        scsIndex = 0;
                        songChordSequence = null;                        
                    }
                    case PAUSED ->
                    {
                    }
                    case PLAYING ->
                    {
                        var session = mc.getPlaybackSession();
                        if (session instanceof SongContextProvider scp)
                        {
                            var songContext = scp.getSongContext();
                            try
                            {
                                songChordSequence = new SongChordSequence(songContext.getSong(), null);
                            } catch (UserErrorGenerationException ex)
                            {
                                // Eg starting chord missing
                                // Nothing
                            }
                        }
                    }
                    default -> throw new AssertionError(((MusicController.State) evt.getNewValue()).name());

                }
            }
        }
    }

    // =================================================================================================
    // Private methods
    // =================================================================================================

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

        pnl_chords.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        pnl_chords.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 50, 5));

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

        pnl_posSection.setLayout(new java.awt.BorderLayout());

        posViewer.setFont(new java.awt.Font("Courier New", 1, 18)); // NOI18N
        posViewer.setTimeShown(false);
        pnl_pos.add(posViewer);

        pnl_posSection.add(pnl_pos, java.awt.BorderLayout.NORTH);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_songPart, org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.lbl_songPart.text")); // NOI18N
        pnl_songPart.add(lbl_songPart);

        pnl_posSection.add(pnl_songPart, java.awt.BorderLayout.SOUTH);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_posSection, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnl_chords, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnl_posSection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnl_chords, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
    private javax.swing.JLabel lbl_songPart;
    private javax.swing.JPanel pnl_chords;
    private javax.swing.JPanel pnl_pos;
    private javax.swing.JPanel pnl_posSection;
    private javax.swing.JPanel pnl_songPart;
    private org.jjazz.ui.musiccontrolactions.ui.api.PositionViewer posViewer;
    // End of variables declaration//GEN-END:variables


}
