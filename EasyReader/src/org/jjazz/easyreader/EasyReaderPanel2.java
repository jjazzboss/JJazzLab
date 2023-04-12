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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListener;
import org.jjazz.musiccontrol.api.SongMusicGenerationListener;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.barbox.api.BarBox;
import org.jjazz.ui.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.ui.cl_editor.barbox.api.BarBoxSettings;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererFactory;
import org.jjazz.ui.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.ui.utilities.api.Utilities;
import org.openide.util.Exceptions;

/**
 * Display the currently playing chord symbol.
 */
public class EasyReaderPanel2 extends JPanel implements PropertyChangeListener, PlaybackListener
{

    private static final int PRE_FIRE_EVENT_MS = 100;
    private Song song;
    private final Position posModel;
    private BarBox barBox, nextBarBox;
    private SongMusicGenerationListener songMusicGenerationListener;
    private static final Logger LOGGER = Logger.getLogger(EasyReaderPanel2.class.getSimpleName());

    public EasyReaderPanel2()
    {
        initComponents();
        lbl_nextChord.setFont(IR_ChordSymbolSettings.getDefault().getFont().deriveFont(14f));
        lbl_nextChord.setText("");
        lbl_nextSongPart.setText("");
        pnl_barBox.setLayout(new MyLayoutManager());
        posModel = new Position();
    }

    public void cleanup()
    {
        setModel(null);
    }

    /**
     * Set the song.
     *
     *
     *
     * @param song Can be null. The song CL_Editor must be opened.
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

            barBox.cleanup();
            nextBarBox.cleanup();
            pnl_barBox.remove(barBox);
            pnl_barBox.remove(nextBarBox);
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

            // Create the BarBoxes
            var cls = song.getChordLeadSheet();
            CL_Editor clEditor = CL_EditorTopComponent.get(cls).getEditor();
            barBox = new BarBox(clEditor, 0, 0,
                    song.getChordLeadSheet(),
                    new BarBoxConfig(BarRendererFactory.BR_CHORD_SYMBOL, BarRendererFactory.BR_CHORD_POSITION, BarRendererFactory.BR_SECTION),
                    BarBoxSettings.getDefault(),
                    BarRendererFactory.getDefault());
            pnl_barBox.add(barBox);
            nextBarBox = new BarBox(clEditor, 1, song.getSize() > 1 ? 1 : -1,
                    song.getChordLeadSheet(),
                    new BarBoxConfig(BarRendererFactory.BR_CHORD_SYMBOL, BarRendererFactory.BR_CHORD_POSITION, BarRendererFactory.BR_SECTION),
                    BarBoxSettings.getDefault(),
                    BarRendererFactory.getDefault());
            pnl_barBox.add(nextBarBox);


            updateBarBoxes(0);
        }

    }

    public Song getModel()
    {
        return song;
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
    public void beatChanged(Position oldPos, Position pos, float posInBeats)
    {
        if (!isEnabled())
        {
            return;
        }
        LOGGER.severe("beatChanged() pos=" + pos + " posInBeats=" + posInBeats);
        posModel.set(pos);
        int bar = pos.getBar();


        if (barBox.getModelBarIndex() == bar)
        {
            barBox.showPlaybackPoint(true, pos);
            nextBarBox.showPlaybackPoint(false, pos);
        } else if (nextBarBox.getModelBarIndex() == bar)
        {
            barBox.showPlaybackPoint(false, pos);
            nextBarBox.showPlaybackPoint(true, pos);
        } else
        {
            updateBarBoxes(bar);
            barBox.showPlaybackPoint(true, pos);
            nextBarBox.showPlaybackPoint(false, pos);
        }
    }


    @Override
    public void chordSymbolChanged(CLI_ChordSymbol newChord)
    {

    }

    @Override
    public void songPartChanged(SongPart spt)
    {
        lbl_songPart.setText(spt.getName());
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
    private void updateBarBoxes(int songBar)
    {
        LOGGER.severe("updateBarBoxes() -- songBar=" + songBar);


        int clsBar = song.getSongStructure().toClsPosition(new Position(songBar, 0)).getBar();
        int nextSongBar = songBar + 1;
        var nextSongBarPos = new Position(nextSongBar, 0);
        var nextClsBarPos = song.getSongStructure().toClsPosition(nextSongBarPos);
        var nextClsBar = nextClsBarPos != null ? nextClsBarPos.getBar() : -1;
        barBox.setBarIndex(songBar);
        barBox.setModelBarIndex(clsBar);
        nextBarBox.setBarIndex(nextSongBar);
        nextBarBox.setModelBarIndex(nextClsBar);


        // Next SongPart
        var nextNextSongBar = nextSongBar + 1;
        SongPart nextSongPart = song.getSongStructure().getSongPart(nextNextSongBar);
        String str = "";
        LOGGER.severe("        nextNextSongBar=" + nextNextSongBar + " nextSongPart=" + nextSongPart);
        if (nextSongPart != null
                && (nextSongPart.getStartBarIndex() == nextNextSongBar || nextSongPart.getStartBarIndex() == nextNextSongBar + 1))
        {
            str = "> " + nextSongPart.getName();
        }
        lbl_nextSongPart.setText(str);


        // Next chord        
        CLI_ChordSymbol nextChord = null;
        if (nextSongPart != null)
        {
            var nextNextSongBarPos = new Position(nextNextSongBar, 0);
            var nextNextClsBarPos = song.getSongStructure().toClsPosition(nextNextSongBarPos);
            var nextNextClsBar = nextNextClsBarPos.getBar();
            var cliCs = song.getChordLeadSheet().getItems(nextNextClsBar, nextNextClsBar, CLI_ChordSymbol.class,
                    cli -> cli.getPosition().getBeat() <= 1f);
            if (!cliCs.isEmpty())
            {
                nextChord = cliCs.get(0);
            }
        }
        str = nextChord != null ? "> " + nextChord.getData().getOriginalName() : ">";
        lbl_nextChord.setText(str);
    }

    // =================================================================================================
    // Inner classes
    // =================================================================================================

    /**
     * Layout the 2 BarBoxes so that they share the available width and keep their preferred height.
     */
    private class MyLayoutManager implements LayoutManager
    {

        @Override
        public void layoutContainer(Container parent)
        {
            if (barBox == null)
            {
                return;
            }
            var r = Utilities.getUsableArea((JComponent) parent);
            var prefBbHeight = barBox.getPreferredSize().height;
            var bbWidth = r.width / 2;
            barBox.setBounds(r.x, r.y, bbWidth, prefBbHeight);
            nextBarBox.setBounds(r.x + bbWidth, r.y, r.width - bbWidth, prefBbHeight);
        }

        @Override
        public void addLayoutComponent(String name, Component comp)
        {
            // Nothing
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
            // Nothing
        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            return minimumLayoutSize(parent);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return new Dimension(100, 20);
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

        pnl_barBox = new javax.swing.JPanel();
        pnl_nextChord = new javax.swing.JPanel();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 5), new java.awt.Dimension(0, 5), new java.awt.Dimension(32767, 5));
        lbl_nextChord = new javax.swing.JLabel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 5), new java.awt.Dimension(0, 5), new java.awt.Dimension(32767, 5));
        lbl_nextSongPart = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(1, 10), new java.awt.Dimension(1, 32767));
        lbl_songPart = new javax.swing.JLabel();
        posViewer = new org.jjazz.ui.musiccontrolactions.ui.api.PositionViewer();

        pnl_barBox.setLayout(new java.awt.GridLayout());

        pnl_nextChord.setLayout(new javax.swing.BoxLayout(pnl_nextChord, javax.swing.BoxLayout.Y_AXIS));
        pnl_nextChord.add(filler3);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_nextChord, "> A#7b5#9"); // NOI18N
        pnl_nextChord.add(lbl_nextChord);
        pnl_nextChord.add(filler2);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_nextSongPart, "> Verse"); // NOI18N
        pnl_nextChord.add(lbl_nextSongPart);
        pnl_nextChord.add(filler1);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_songPart, org.openide.util.NbBundle.getMessage(EasyReaderPanel2.class, "EasyReaderPanel2.lbl_songPart.text")); // NOI18N

        posViewer.setFont(new java.awt.Font("Courier New", 1, 18)); // NOI18N
        posViewer.setTimeShown(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnl_barBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pnl_nextChord, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_songPart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 169, Short.MAX_VALUE)
                        .addComponent(posViewer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(posViewer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_songPart))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_barBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnl_nextChord, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.JLabel lbl_nextChord;
    private javax.swing.JLabel lbl_nextSongPart;
    private javax.swing.JLabel lbl_songPart;
    private javax.swing.JPanel pnl_barBox;
    private javax.swing.JPanel pnl_nextChord;
    private org.jjazz.ui.musiccontrolactions.ui.api.PositionViewer posViewer;
    // End of variables declaration//GEN-END:variables


}
