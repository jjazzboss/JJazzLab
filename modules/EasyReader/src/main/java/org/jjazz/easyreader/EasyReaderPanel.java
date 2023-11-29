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
package org.jjazz.easyreader;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.ItemAddedEvent;
import org.jjazz.chordleadsheet.api.event.ItemBarShiftedEvent;
import org.jjazz.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.chordleadsheet.api.event.ItemClientPropertyChangedEvent;
import org.jjazz.chordleadsheet.api.event.ItemMovedEvent;
import org.jjazz.chordleadsheet.api.event.ItemRemovedEvent;
import org.jjazz.chordleadsheet.api.event.SectionMovedEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListener;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.RpValueChangedEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptRenamedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.songstructure.api.event.SptResizedEvent;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.barbox.api.BarBox;
import org.jjazz.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.cl_editor.barbox.api.BarBoxSettings;
import org.jjazz.cl_editor.barrenderer.api.BarRendererFactory;
import org.jjazz.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.uiutilities.api.UIUtilities;
import org.openide.util.NbPreferences;

/**
 * Display the currently playing chord symbols.
 */
public class EasyReaderPanel extends JPanel implements PropertyChangeListener, PlaybackListener, ClsChangeListener, SgsChangeListener
{

    static final String PREF_ZOOM_Y = "PrefEasyReaderPanelZoomY";
    private Song song;
    private final Position songPosition, clsPosition;
    private SongPart songPart;
    private SongPart nextSongPart;
    private BarBox barBox, nextBarBox;
    private int playStartBar = -1;
    private final Font defaultAnnotationFont;
    private final Font defaultNextChordFont;
    private final JLabel lbl_annotation;
    private static final Preferences prefs = NbPreferences.forModule(EasyReaderPanel.class);
    private static final Logger LOGGER = Logger.getLogger(EasyReaderPanel.class.getSimpleName());

    public EasyReaderPanel()
    {
        initComponents();


        lbl_annotation = new JLabel();
        defaultAnnotationFont = lbl_annotation.getFont().deriveFont(Font.ITALIC, lbl_annotation.getFont().getSize2D() + 5f);
        lbl_annotation.setFont(defaultAnnotationFont);
        // lbl_annotation.setBorder(BorderFactory.createEtchedBorder());
        lbl_annotation.setHorizontalAlignment(SwingConstants.CENTER);
        lbl_annotation.setVerticalAlignment(SwingConstants.CENTER);
        pnl_barBox.add(lbl_annotation);
        defaultNextChordFont = IR_ChordSymbolSettings.getDefault().getFont().deriveFont(14f);
        lbl_nextChord.setFont(defaultNextChordFont);
        pnl_barBox.setLayout(new MyLayoutManager());
        songPosition = new Position();
        clsPosition = new Position();
        slider_zoom.setValue(prefs.getInt(PREF_ZOOM_Y, 50));


        // Some labels have text value in the designer 
        clearLabels();
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
        LOGGER.log(Level.FINE, "setModel() song={0}", song);

        if (this.song == song)
        {
            return;
        }

        if (this.song != null)
        {
            MusicController.getInstance().removePropertyChangeListener(this);
            MusicController.getInstance().removePlaybackListener(this);
            this.song.getChordLeadSheet().removeClsChangeListener(this);
            this.song.getSongStructure().removeSgsChangeListener(this);

            removeBarBoxes();
        }

        this.song = song;
        this.songPart = null;
        this.nextSongPart = null;


        // Update data
        songPosition.reset();
        clsPosition.reset();
        this.posViewer.setModel(this.song, songPosition);


        // Update UI
        setEnabled(this.song != null);
        clearLabels();


        if (this.song != null)
        {
            MusicController.getInstance().addPropertyChangeListener(this);
            MusicController.getInstance().addPlaybackListener(this);
            this.song.getChordLeadSheet().addClsChangeListener(this);
            this.song.getSongStructure().addSgsChangeListener(this);


            songPart = song.getSongStructure().getSongPart(0);  // Might be null            
            lbl_songPart.setText(songPart != null ? songPart.getName() : "");


            createBarBoxes(this.song);
            stopped();
        }

    }


    public Song getModel()
    {
        return song;
    }

    @Override
    public void setEnabled(boolean b)
    {
        LOGGER.log(Level.FINE, "setEnabled() b={0}", b);
        super.setEnabled(b);
        lbl_nextChord.setEnabled(b);
        lbl_nextSongPart.setEnabled(b);
        lbl_songPart.setEnabled(b);
        posViewer.setEnabled(b);
        slider_zoom.setEnabled(b);
    }

// ======================================================================
// PlaybackListener interface
// ======================================================================   

    @Override
    public void enabledChanged(boolean b)
    {
        setEnabled(b);
        if (!b)
        {
            stopped();
            clearLabels();
        }
    }


    @Override
    public void beatChanged(Position oldSongPos, Position songPos, float songPosInBeats)
    {
        if (!isEnabled())
        {
            return;
        }


        // Update positions
        songPosition.set(songPos);
        clsPosition.set(song.getSongStructure().toClsPosition(songPos));
        LOGGER.log(Level.FINE, "beatChanged() pos={0} clsPos={1}", new Object[]
        {
            songPos, clsPosition
        });


        // Process beat changed only if music is playing
        var mc = MusicController.getInstance();
        boolean isPlaying = !mc.isArrangerPlaying() && mc.getState().equals(MusicController.State.PLAYING);
        if (!isPlaying)
        {
            return;
        }


        // Progress bar
        int songBar = songPos.getBar();
        int progress = Math.round(100f * songBar / song.getSongStructure().getSizeInBars());
        progressBar.setValue(progress);


        // Update annotation on each new bar
        if (songPosition.isFirstBarBeat())
        {
            updateAnnotation(clsPosition);
        }

        if (playStartBar == -1)
        {
            // LOGGER.log(Level.FINE, "beatChanged() firstBeatChangeAfterPlay==true => songBar={0}", songBar);
            // Special case, playback has just started. songBar is the start bar, which can be any Song bar.
            // Update the BarBoxes: this is useful for user to make the 2 displayed bars start on an odd bar
            updateBarBoxes(songBar);
            updateNextStuff(songBar);
            showPlaybackPoint(clsPosition, false);
            playStartBar = songBar;
        } else if ((songBar - playStartBar) % 2 == 0)
        {
            // Even bars
            if (songPosition.isFirstBarBeat())
            {
                updateBarBoxes(songBar);
                updateNextStuff(songBar);
            }
            showPlaybackPoint(clsPosition, false);
        } else
        {
            // Odd bars
            showPlaybackPoint(clsPosition, true);
        }
    }


    @Override
    public void chordSymbolChanged(CLI_ChordSymbol newChord)
    {

    }

    @Override
    public void songPartChanged(SongPart spt)
    {
        if (!isEnabled())
        {
            return;
        }
        songPart = spt;
        lbl_songPart.setText(songPart.getName());
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
                    case STOPPED, DISABLED ->
                    {
                        stopped();
                    }
                    case PAUSED, PLAYING ->
                    {
                        // Nothing
                    }
                    default -> throw new AssertionError(((MusicController.State) evt.getNewValue()).name());

                }
            }
        }
    }
    // ----------------------------------------------------------------------------------
    // ClsChangeListener interface
    // ----------------------------------------------------------------------------------

    @Override
    public void authorizeChange(ClsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void chordLeadSheetChanged(final ClsChangeEvent event)
    {
        // Model changes can be generated outside the EDT
        Runnable run = () -> 
        {
            // Save focus state
            if (event instanceof SizeChangedEvent e)
            {
                // Nothing to do here

            } else if (event instanceof ItemAddedEvent e)
            {
                for (var item : e.getItems())
                {
                    int itemBar = item.getPosition().getBar();
                    if (barBox.getModelBarIndex() == itemBar)
                    {
                        barBox.addItem(item);
                    } else if (nextBarBox.getModelBarIndex() == itemBar)
                    {
                        nextBarBox.addItem(item);
                    }
                    if (item instanceof CLI_BarAnnotation && itemBar == clsPosition.getBar())
                    {
                        updateAnnotation(clsPosition);
                    }
                }

            } else if (event instanceof ItemRemovedEvent e)
            {
                for (var item : e.getItems())
                {
                    int itemBar = item.getPosition().getBar();
                    if (barBox.getModelBarIndex() == itemBar)
                    {
                        barBox.removeItem(item);
                    } else if (nextBarBox.getModelBarIndex() == itemBar)
                    {
                        nextBarBox.removeItem(item);
                    }
                    if (item instanceof CLI_BarAnnotation && itemBar == clsPosition.getBar())
                    {
                        updateAnnotation(clsPosition);
                    }
                }
            } else if (event instanceof ItemChangedEvent e)
            {
                // If chord symbol, catched directly by the ItemRenderer
                var item = e.getItem();
                if (item instanceof CLI_Section cliSection)
                {
                    // Handle section time signature changes. This will disable our listener but we still need to update the UI                    
                    if (barBox.getSection() == cliSection)
                    {
                        barBox.setSection(cliSection);
                    }
                    if (nextBarBox.getSection() == cliSection)
                    {
                        nextBarBox.setSection(cliSection);
                    }
                } else if (item instanceof CLI_BarAnnotation cliBa && cliBa.getPosition().getBar() == clsPosition.getBar())
                {
                    updateAnnotation(clsPosition);
                }
            } else if (event instanceof ItemMovedEvent e)
            {
                // A moved ChordSymbol or other, but NOT a section
                var item = e.getItem();
                int modelBarIndex = item.getPosition().getBar();
                int oldModelBarIndex = e.getOldPosition().getBar();
                if (modelBarIndex == oldModelBarIndex)
                {
                    if (barBox.getModelBarIndex() == modelBarIndex)
                    {
                        barBox.moveItem(item);
                    } else if (nextBarBox.getModelBarIndex() == modelBarIndex)
                    {
                        nextBarBox.moveItem(item);
                    }
                } else
                {
                    // Remove on one bar 
                    if (barBox.getModelBarIndex() == oldModelBarIndex)
                    {
                        barBox.removeItem(item);
                    } else if (nextBarBox.getModelBarIndex() == oldModelBarIndex)
                    {
                        nextBarBox.removeItem(item);
                    }

                    // Then add on another bar
                    if (barBox.getModelBarIndex() == modelBarIndex)
                    {
                        barBox.addItem(item);
                    } else if (nextBarBox.getModelBarIndex() == modelBarIndex)
                    {
                        nextBarBox.addItem(item);
                    }

                }
            } else if (event instanceof SectionMovedEvent e)
            {
                // Handled via song structure listener
                // PlaybackListener will get disabled

            } else if (event instanceof ItemBarShiftedEvent e)
            {
                // No need to handle
            } else if ((event instanceof ItemClientPropertyChangedEvent e))
            {
                // Nothing
            }
        };
        org.jjazz.uiutilities.api.UIUtilities.invokeLaterIfNeeded(run);
    }
    //------------------------------------------------------------------------------
    // SgsChangeListener interface
    //------------------------------------------------------------------------------   

    @Override
    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void songStructureChanged(final SgsChangeEvent e)
    {
        // Model changes can be generated outside the EDT
        Runnable run = () -> 
        {
            if (e instanceof SptRemovedEvent)
            {
                var songSize = song.getSongStructure().getSizeInBars();
                if (songSize == 0)
                {
                    barBox.setModelBarIndex(-1);
                    nextBarBox.setModelBarIndex(-1);
                } else if (songSize == 1)
                {
                    nextBarBox.setModelBarIndex(-1);
                }

            } else if (e instanceof SptAddedEvent)
            {
                if (barBox.getModelBarIndex() == -1)
                {
                    barBox.setModelBarIndex(0);     // 0 because playback must be disabled
                }
                if (nextBarBox.getModelBarIndex() == -1 && song.getSongStructure().getSizeInBars() > 1)
                {
                    nextBarBox.setModelBarIndex(1);
                }

            } else if (e instanceof SptReplacedEvent re)
            {
                // Nothing
            } else if (e instanceof SptResizedEvent sre)
            {
                // Nothing
            } else if (e instanceof SptRenamedEvent sre)
            {
                if (sre.getSongParts().contains(songPart))
                {
                    lbl_songPart.setText(songPart.getName());
                }
                if (sre.getSongParts().contains(nextSongPart))
                {
                    lbl_nextSongPart.setText(buildNextSongPartString(nextSongPart));
                }
            } else if (e instanceof RpValueChangedEvent)
            {
                // Nothing
            }
        };
        UIUtilities.invokeLaterIfNeeded(run);
    }

    // =================================================================================================
    // Private methods
    // =================================================================================================

    private void createBarBoxes(Song sg)
    {
        var cls = sg.getChordLeadSheet();
        CL_Editor clEditor = CL_EditorTopComponent.get(cls).getEditor();


        var clsPos = sg.getSongStructure().toClsPosition(new Position(0, 0));
        int modelBarIndex = clsPos != null ? clsPos.getBar() : -1;

        barBox = new BarBox(clEditor, 0, modelBarIndex, cls,
                new BarBoxConfig(BarRendererFactory.BR_CHORD_SYMBOL, BarRendererFactory.BR_CHORD_POSITION, BarRendererFactory.BR_SECTION),
                BarBoxSettings.getDefault(), BarRendererFactory.getDefault(), this);
        barBox.setDisplayQuantizationValue(Quantization.OFF);
        pnl_barBox.add(barBox);


        clsPos = sg.getSongStructure().toClsPosition(new Position(1, 0));
        modelBarIndex = clsPos != null ? clsPos.getBar() : -1;

        nextBarBox = new BarBox(clEditor, 1, modelBarIndex, cls,
                new BarBoxConfig(BarRendererFactory.BR_CHORD_SYMBOL, BarRendererFactory.BR_CHORD_POSITION, BarRendererFactory.BR_SECTION),
                BarBoxSettings.getDefault(), BarRendererFactory.getDefault(), this);
        pnl_barBox.add(nextBarBox);
        nextBarBox.setDisplayQuantizationValue(Quantization.OFF);

        setZoomY(prefs.getInt(PREF_ZOOM_Y, 50));
    }

    /**
     * Update the BarBoxes.
     *
     * @param songBar
     */
    private void updateBarBoxes(int songBar)
    {
        int clsBar = song.getSongStructure().toClsPosition(new Position(songBar, 0)).getBar();
        int next1SongBar = songBar + 1;
        var next1SongBarPos = new Position(next1SongBar, 0);
        var next1ClsBarPos = song.getSongStructure().toClsPosition(next1SongBarPos);
        var next1ClsBar = next1ClsBarPos != null ? next1ClsBarPos.getBar() : -1;
        barBox.setBarIndex(songBar);
        barBox.setModelBarIndex(clsBar);
        nextBarBox.setBarIndex(next1SongBar);
        nextBarBox.setModelBarIndex(next1ClsBar);
        LOGGER.log(Level.FINE, "updateBarBoxes() songBar={0} => barBox.modelBarIndex={1} nextBarBox.modelBarIndex={2}", new Object[]
        {
            songBar, clsBar, next1ClsBar
        });
    }

    /**
     * Update the next chord/song part.
     *
     * @param songBar
     */
    private void updateNextStuff(int songBar)
    {
        // Next SongPart
        var next1SongBar = songBar + 1;
        var next2SongBar = next1SongBar + 1;
        var next3SongBar = next1SongBar + 2;
        SongPart next2SongPart = song.getSongStructure().getSongPart(next2SongBar);
        SongPart next3SongPart = song.getSongStructure().getSongPart(next3SongBar);
        nextSongPart = null;
        if (next2SongPart != null && next2SongPart.getStartBarIndex() == next2SongBar)
        {
            nextSongPart = next2SongPart;
        } else if (next3SongPart != null && next3SongPart.getStartBarIndex() == next3SongBar)
        {
            nextSongPart = next3SongPart;
        }
        lbl_nextSongPart.setText(buildNextSongPartString(nextSongPart));


        // Next chord        
        CLI_ChordSymbol nextChord = null;
        var next2SongBarPos = new Position(next2SongBar, 0);
        var next2ClsBarPos = song.getSongStructure().toClsPosition(next2SongBarPos);
        if (next2ClsBarPos != null)
        {
            nextChord = song.getChordLeadSheet().getFirstItemAfter(next2ClsBarPos, true, CLI_ChordSymbol.class, cli -> true);
        }
        var str = "";
        if (nextChord != null)
        {
            var nextChordPos = nextChord.getPosition();
            String strDistantChord = new Position(next2SongBar, 1).compareTo(nextChordPos) <= 0 ? "..." : " ";
            str += ">" + strDistantChord + nextChord.getData().getOriginalName();
        }
        lbl_nextChord.setText(str);
    }

    private void stopped()
    {
        barBox.showPlaybackPoint(false, null);
        nextBarBox.showPlaybackPoint(false, null);
        songPosition.reset();
        clsPosition.reset();
        updateBarBoxes(0);
        updateNextStuff(0);
        updateAnnotation(clsPosition);
        playStartBar = -1;
    }

    private void showPlaybackPoint(Position clsPos, boolean useNextBarBox)
    {
        barBox.showPlaybackPoint(!useNextBarBox, clsPos);
        nextBarBox.showPlaybackPoint(useNextBarBox, clsPos);
    }

    private void updateAnnotation(Position clsPos)
    {
        var cliBa = song.getChordLeadSheet().getBarFirstItem(clsPos.getBar(), CLI_BarAnnotation.class, cli -> true);
        String text = cliBa != null ? cliBa.getData().replace("\n", "<br>") : "";
        lbl_annotation.setText("<html>" + text + "</html>");
    }

    private void clearLabels()
    {
        lbl_nextChord.setText("");
        lbl_nextSongPart.setText("");
        lbl_songPart.setText("");
        lbl_annotation.setText("");
    }

    private String buildNextSongPartString(SongPart nextSongPart)
    {
        return nextSongPart == null ? "" : "> " + nextSongPart.getName();
    }

    private void removeBarBoxes()
    {
        barBox.cleanup();
        nextBarBox.cleanup();
        pnl_barBox.remove(barBox);
        pnl_barBox.remove(nextBarBox);
    }

    /**
     *
     * @param value 0-100
     */
    private void setZoomY(int value)
    {
        if (barBox != null)
        {
            barBox.setZoomVFactor(value);
            nextBarBox.setZoomVFactor(value);
            float newFontSize = (1f + (Math.max(value, 40f) - 50f) / 100f) * defaultAnnotationFont.getSize2D();
            lbl_annotation.setFont(defaultAnnotationFont.deriveFont(newFontSize));
            newFontSize = (1f + (Math.max(value, 40f) - 50f) / 100f) * defaultNextChordFont.getSize2D();
            lbl_nextChord.setFont(defaultNextChordFont.deriveFont(newFontSize));
            prefs.putInt(PREF_ZOOM_Y, value);
        }

        slider_zoom.setValue(value);
    }

    // =================================================================================================
    // Inner classes
    // =================================================================================================

    /**
     * Layout the 2 BarBoxes so that they share the available width and keep their preferred height.
     * <p>
     * Use the remaining space below for the current bar annotation.
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
            var r = UIUtilities.getUsableArea((JComponent) parent);

            // 2 barboxes on the top
            var prefBbHeight = barBox.getPreferredSize().height;
            var bbWidth = r.width / 2;
            barBox.setBounds(r.x, r.y, bbWidth, prefBbHeight);
            nextBarBox.setBounds(r.x + bbWidth, r.y, r.width - bbWidth, prefBbHeight);

            // Use remaining space below for bar annotation
            lbl_annotation.setBounds(r.x, prefBbHeight, r.width, r.height - prefBbHeight);

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
        posViewer = new org.jjazz.musiccontrolactions.api.ui.PositionViewer();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        slider_zoom = UIUtilities.buildSlider(SwingConstants.HORIZONTAL, 0.5f);
        pnl_progressBar = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();

        pnl_barBox.setLayout(new java.awt.GridLayout(1, 0));

        pnl_nextChord.setLayout(new javax.swing.BoxLayout(pnl_nextChord, javax.swing.BoxLayout.Y_AXIS));
        pnl_nextChord.add(filler3);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_nextChord, "> A#7b5#9"); // NOI18N
        lbl_nextChord.setToolTipText(org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.lbl_nextChord.toolTipText")); // NOI18N
        pnl_nextChord.add(lbl_nextChord);
        pnl_nextChord.add(filler2);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_nextSongPart, "> Verse"); // NOI18N
        lbl_nextSongPart.setToolTipText(org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.lbl_nextSongPart.toolTipText")); // NOI18N
        pnl_nextChord.add(lbl_nextSongPart);
        pnl_nextChord.add(filler1);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_songPart, "Verse"); // NOI18N
        lbl_songPart.setToolTipText(org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.lbl_songPart.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(posViewer, org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.posViewer.text")); // NOI18N
        posViewer.setToolTipText(org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.posViewer.toolTipText")); // NOI18N
        posViewer.setFont(new java.awt.Font("Courier New", 1, 18)); // NOI18N
        posViewer.setTimeShown(false);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/easyreader/resources/zoomYarrow.png"))); // NOI18N
        jPanel1.add(jLabel1);

        slider_zoom.setToolTipText(org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.slider_zoom.toolTipText")); // NOI18N
        slider_zoom.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                slider_zoomStateChanged(evt);
            }
        });
        jPanel1.add(slider_zoom);

        pnl_progressBar.setToolTipText(org.openide.util.NbBundle.getMessage(EasyReaderPanel.class, "EasyReaderPanel.pnl_progressBar.toolTipText")); // NOI18N
        pnl_progressBar.setLayout(new java.awt.GridBagLayout());
        pnl_progressBar.add(progressBar, new java.awt.GridBagConstraints());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 211, Short.MAX_VALUE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(pnl_barBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pnl_nextChord, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lbl_songPart)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(pnl_progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(posViewer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(posViewer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbl_songPart)))
                    .addComponent(pnl_progressBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_barBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnl_nextChord, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void slider_zoomStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_slider_zoomStateChanged
    {//GEN-HEADEREND:event_slider_zoomStateChanged
        if (slider_zoom.getValueIsAdjusting())
        {
            return;
        }
        setZoomY(slider_zoom.getValue());
    }//GEN-LAST:event_slider_zoomStateChanged



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lbl_nextChord;
    private javax.swing.JLabel lbl_nextSongPart;
    private javax.swing.JLabel lbl_songPart;
    private javax.swing.JPanel pnl_barBox;
    private javax.swing.JPanel pnl_nextChord;
    private javax.swing.JPanel pnl_progressBar;
    private org.jjazz.musiccontrolactions.api.ui.PositionViewer posViewer;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JSlider slider_zoom;
    // End of variables declaration//GEN-END:variables


}
