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
package org.jjazz.pianoroll;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.AttributedString;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsActionEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.rhythmmusicgeneration.api.ChordSequence;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.song.api.Song;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.colorsetmanager.api.ColorSetManager;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ui.utilities.api.HSLColor;
import org.jjazz.ui.utilities.api.StringMetrics;

/**
 * The ruler panel that shows the beat position marks + time signatures over the NotesPanel.
 * <p>
 * If a song is associated, show also the chord symbols and song parts. The RulerPanel listens to chord symbols changes to refresh
 * itself (song structure changes are handled by the PianoRollEditor
 */
public class RulerPanel extends javax.swing.JPanel implements ClsChangeListener, PropertyChangeListener
{

    private static final int BAR_TICK_LENGTH = 7;
    private static final int BEAT_TICK_LENGTH = 5;
    private static final int SIXTEENTH_TICK_LENGTH = 3;
    private static final Color COLOR_BAR_FONT = new Color(176, 199, 220);
    private static final Color COLOR_BEAT_FONT = new Color(80, 80, 80);
    private static final Color COLOR_TIME_SIGNATURE_FONT = COLOR_BAR_FONT;
    private static final Color COLOR_CHORD_SYMBOL_FONT = COLOR_BAR_FONT;
    private static final Color COLOR_SONG_PART_FONT = Color.BLACK;
    private static final Color COLOR_BEAT_TICK = COLOR_BEAT_FONT;
    private static final Color COLOR_SIXTEENTH_TICK = HSLColor.changeLuminance(COLOR_BEAT_TICK, 3);
    private final float BASE_FONT_HEIGHT;
    private final Font SMALL_FONT;
    private final int TIME_SIGNATURE_LANE_HEIGHT;
    private final int CHORD_SYMBOL_LANE_HEIGHT;
    private final int BAR_LANE_HEIGHT;


    private final NotesPanel notesPanel;
    private final NotesPanel.XMapper xMapper;
    private final PianoRollEditor editor;

    private int playbackPointX = -1;
    private Song song;

    private static final Logger LOGGER = Logger.getLogger(RulerPanel.class.getSimpleName());

    /**
     *
     * @param editor
     * @param notesPanel
     */
    public RulerPanel(PianoRollEditor editor, NotesPanel notesPanel)
    {
        this.editor = editor;
        this.notesPanel = notesPanel;
        this.xMapper = notesPanel.getXMapper();


        this.editor.getSettings().addPropertyChangeListener(this);

        // Repaint ourself when notesPanel is resized
        this.notesPanel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                LOGGER.log(Level.FINE, "RulerPanel.componentResized() -- notesPanel.getWidth()={0}", notesPanel.getWidth());
                revalidate();
                repaint();
            }
        });


        // Enable scroll when dragging the ruler, and update chord leadsheet/song structure selection when clicked
        // this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        RulerMouseAdapter ma = new RulerMouseAdapter();
        addMouseListener(ma);
        addMouseMotionListener(ma);


        // Precalculate font metrics
        BufferedImage img = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);       // Size does not matter
        Graphics2D g2 = img.createGraphics();
        Font f = this.editor.getSettings().getRulerBaseFont();
        BASE_FONT_HEIGHT = (float) new StringMetrics(g2, f).getLogicalBoundsNoLeadingNoDescent("9").getHeight();
        img.flush();
        g2.dispose();
        TIME_SIGNATURE_LANE_HEIGHT = Math.round(BASE_FONT_HEIGHT + 3);
        CHORD_SYMBOL_LANE_HEIGHT = Math.round(BASE_FONT_HEIGHT + 3);
        BAR_LANE_HEIGHT = Math.round(BASE_FONT_HEIGHT + BAR_TICK_LENGTH + 1);
        SMALL_FONT = f.deriveFont(f.getSize2D() - 2f);
    }


    /**
     * Use the specified song to show additional info: chord symbols, song parts.
     * <p>
     * Can be called only once.
     *
     * @param song
     */
    public void setSong(Song song)
    {
        Preconditions.checkNotNull(song);
        if (this.song != null)
        {
            throw new IllegalStateException("this.song=" + this.song.getName());
        }

        this.song = song;

        // Listen to chord leadsheet changes (song structure changes must be managed at a higher level)
        song.getChordLeadSheet().addClsChangeListener(this);

        revalidate();
        repaint();
    }

    public Song getSong()
    {
        return song;
    }

    @Override
    public Dimension getPreferredSize()
    {
        int h = TIME_SIGNATURE_LANE_HEIGHT + (song == null ? 0 : CHORD_SYMBOL_LANE_HEIGHT) + BAR_LANE_HEIGHT;
        var pd = new Dimension(notesPanel.getWidth(), h);
        return pd;
    }

    /**
     * Show the playback point.
     *
     * @param xPos If &lt; 0 show nothing
     */
    public void showPlaybackPoint(int xPos)
    {
        playbackPointX = xPos;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        var settings = editor.getSettings();

        // Prepare data
        int h = getHeight();
        int yTopBarLane = h - BAR_LANE_HEIGHT;
        int yBottomBarLane = h - 1;
        int yTopTimeSignatureLane = 0;
        int yBottomTimeSignatureLane = yTopTimeSignatureLane + TIME_SIGNATURE_LANE_HEIGHT - 1;
        int yTopChordSymbolLane = yBottomTimeSignatureLane + 1;
        int yBottomChordSymbolLane = yTopBarLane - 1;
        Font baseFont = settings.getRulerBaseFont();


        // Default background
        g2.setColor(settings.getRulerBackgroundColor());
        g2.fillRect(0, yBottomTimeSignatureLane + 1, getWidth(), song != null ? CHORD_SYMBOL_LANE_HEIGHT + BAR_LANE_HEIGHT : BAR_LANE_HEIGHT);


        // Paint time signature lane background
        g2.setColor(settings.getRulerTsLaneBackgroundColor());
        g2.fillRect(0, yTopTimeSignatureLane, getWidth(), TIME_SIGNATURE_LANE_HEIGHT);


        // Get X coordinate of all beat positions
        var tmapPosX = xMapper.getBeatsXPositions(null);
        var allBeatPositions = tmapPosX.navigableKeySet();


        // Draw chord symbols
        if (song != null)
        {
            ChordSequence cs = new ChordSequence(editor.getBarRange());
            SongChordSequence.fillChordSequence(cs, song, editor.getBarRange());
            for (var cliCs : cs)
            {
                var pos = cliCs.getPosition();
                var posInBeats = editor.toPositionInBeats(pos);
                int x = editor.getXFromPosition(posInBeats);
                int y = yBottomChordSymbolLane - 1;
                AttributedString aStr;
                aStr = new AttributedString(cliCs.getData().getOriginalName(), baseFont.getAttributes());
                aStr.addAttribute(TextAttribute.FOREGROUND, COLOR_BAR_FONT);
                if (pos.isFirstBarBeat())
                {
                    x += 1;
                }
                g2.setFont(baseFont);
                g2.setColor(COLOR_CHORD_SYMBOL_FONT);
                g2.drawString(cliCs.getData().getOriginalName(), x, y);
            }
        }


        // Draw ticks, bar/beat number, time signatures, vertical bars, and possibly song part if available
        boolean paintSixteenthTicks = xMapper.getOneBeatPixelSize() > 40;
        TimeSignature lastTs = null;
        for (Position pos : allBeatPositions)
        {
            float posInBeats = xMapper.toPositionInBeats(pos);
            int x = tmapPosX.get(pos);


            // Draw vertical line on each bar on chord symbol lane
            if (song != null && pos.isFirstBarBeat())
            {
                g2.setColor(settings.getRulerBarTickColor());
                g2.drawLine(x, yBottomChordSymbolLane, x, yTopChordSymbolLane);
            }

            // Draw time signature if changed
            var ts = editor.getTimeSignature(posInBeats);
            int xSongPart = x + 1;
            int yTimeSignatureBaseLine = yBottomTimeSignatureLane - 1;
            if (!ts.equals(lastTs))
            {
                String text = ts.toString();
                g2.setFont(settings.getRulerBaseFont());
                g2.setColor(COLOR_TIME_SIGNATURE_FONT);
                g2.drawString(text, x + 1, yTimeSignatureBaseLine);

                StringMetrics sm = new StringMetrics(g2);
                xSongPart = x + 1 + (int) Math.round(sm.getWidth(text)) + 3;

                lastTs = ts;
            }


            // Possibly draw song part
            if (song != null && pos.isFirstBarBeat())
            {
                var spt = song.getSongStructure().getSongPart(pos.getBar());
                if (spt.getStartBarIndex() == pos.getBar())
                {
                    String text = spt.getName();
                    StringMetrics sm = new StringMetrics(g2, SMALL_FONT);
                    var bounds = sm.getLogicalBoundsNoLeadingNoDescent(text);

                    // Draw rounded box
                    float PADDING = 1;
                    double wRect = bounds.getWidth() + 2 * PADDING;
                    double hRect = bounds.getHeight() + 1 * PADDING;
                    double xRect = xSongPart;
                    double yRect = yTimeSignatureBaseLine + PADDING - hRect;
                    var r = new RoundRectangle2D.Double(xRect, yRect, wRect, hRect, 3, 3);
                    Color c = ColorSetManager.getDefault().getColor(spt.getParentSection().getData().getName());
                    g2.setColor(c);
                    g2.fill(r);

                    // Draw song part name
                    float xStr = xSongPart + PADDING;
                    g2.setFont(SMALL_FONT);
                    g2.setColor(COLOR_SONG_PART_FONT);
                    g2.drawString(text, xStr, yTimeSignatureBaseLine);
                }
            }


            // Draw tick
            Color c = pos.isFirstBarBeat() ? settings.getRulerBarTickColor() : COLOR_BEAT_TICK;
            int tickLength = pos.isFirstBarBeat() ? BAR_TICK_LENGTH : BEAT_TICK_LENGTH;
            g2.setColor(c);
            g2.drawLine(x, yTopBarLane, x, yTopBarLane + tickLength - 1);


            // Draw sixteenth ticks
            if (paintSixteenthTicks)
            {
                g2.setColor(COLOR_SIXTEENTH_TICK);
                float xSixteenthTick = x;
                float sixteenthBeatPixelSize = xMapper.getOneBeatPixelSize() / 4;
                for (int i = 0; i < 3; i++)
                {
                    xSixteenthTick += sixteenthBeatPixelSize;
                    int xSixteenthTickInt = Math.round(xSixteenthTick);
                    g2.drawLine(xSixteenthTickInt, yTopBarLane, xSixteenthTickInt, yTopBarLane + SIXTEENTH_TICK_LENGTH - 1);
                }
            }


            // Draw bar or beat number
            AttributedString aStr = null;
            if (pos.isFirstBarBeat())
            {
                var strBar = String.valueOf(pos.getBar() + 1);
                aStr = new AttributedString(strBar, baseFont.getAttributes());
                aStr.addAttribute(TextAttribute.FOREGROUND, COLOR_BAR_FONT);

            } else if (paintSixteenthTicks)
            {
                var strBeat = String.valueOf(pos.getBar() + 1) + "." + String.valueOf((int) pos.getBeat() + 1);
                aStr = new AttributedString(strBeat, SMALL_FONT.getAttributes());
                aStr.addAttribute(TextAttribute.FOREGROUND, COLOR_BEAT_FONT);
            }
            if (aStr != null)
            {
                // Draw the bar or beat number
                float xStr = x;
                float yStr = yBottomBarLane - 1;           // text baseline position
                g2.drawString(aStr.getIterator(), xStr, yStr);
            }

        }


        // Draw tick horizontal line on top
        g2.setColor(settings.getRulerBarTickColor());
        g2.drawLine(0, yTopBarLane, getWidth() - 1, yTopBarLane);


        // Draw playback point
        if (playbackPointX >= 0)
        {
            g2.setColor(MouseDragLayerUI.COLOR_PLAYBACK_LINE);
            Polygon p = new Polygon();
            int HALF_SIZE = 4;
            int yMax = h - 1;
            p.addPoint(playbackPointX, yMax);
            p.addPoint(playbackPointX + HALF_SIZE, yMax - HALF_SIZE);
            p.addPoint(playbackPointX - HALF_SIZE, yMax - HALF_SIZE);
            g2.fill(p);
        }


        g2.dispose();
    }


    public void cleanup()
    {
        editor.getSettings().removePropertyChangeListener(this);
        if (song != null)
        {
            song.getChordLeadSheet().removeClsChangeListener(this);
        }
    }

    // ==========================================================================================================
    //PropertyChangeListener interface
    // ========================================================================================================== 
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == editor.getSettings())
        {
            repaint();
        }
    }

    // ==========================================================================================================
    // ClsChangeListener interface
    // ==========================================================================================================    

    @Override
    public void authorizeChange(ClsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent e)
    {
        if (e instanceof ClsActionEvent ae && ae.isActionComplete())
        {
            // Listen to all user actions which do not trigger a song structure change
            switch (ae.getActionId())
            {
                case "addItem", "changeItem", "removeItem", "moveItem", "setSectionName" ->
                {
                    repaint();
                }
            }
        }
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================    

    /**
     * Enable scroll when dragging the ruler, and when clicked update the selection on chord leadsheet and song structure editors.
     */
    private class RulerMouseAdapter extends MouseAdapter
    {

        private int xOrigin = Integer.MIN_VALUE;

        @Override
        public void mousePressed(MouseEvent e)
        {
            xOrigin = e.getX();
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (song == null)
            {
                return;
            }
            float posInBeats = editor.getPositionFromPoint(e.getPoint()); // ignore y
            if (posInBeats == -1 || !editor.getBeatRange().contains(posInBeats, true))
            {
                return;
            }
            Position pos = editor.toPosition(posInBeats);


            // Select the corresponding song part
            var spt = song.getSongStructure().getSongPart(pos.getBar());
            var ssTc = SS_EditorTopComponent.get(song.getSongStructure());
            var ssEditor = ssTc.getEditor();
            ssEditor.unselectAll();
            ssEditor.selectSongPart(spt, true);


            // Select the corresponding chord symbol
            var barDelta = pos.getBar() - spt.getStartBarIndex();
            var section = spt.getParentSection();
            var clsPos = new Position(section.getPosition().getBar() + barDelta, pos.getBeat());
            var cliCs = song.getChordLeadSheet().getLastItem(clsPos, CLI_ChordSymbol.class);
            if (cliCs != null)
            {
                var clTc = CL_EditorTopComponent.get(song.getChordLeadSheet());
                var clEditor = clTc.getEditor();
                clEditor.unselectAll();
                clEditor.selectItem(cliCs, true);
            }

        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            xOrigin = Integer.MIN_VALUE;
            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (xOrigin != Integer.MIN_VALUE)
            {
                JViewport viewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, notesPanel);
                if (viewPort != null)
                {
                    int deltaX = xOrigin - e.getX();
                    Rectangle view = viewPort.getViewRect();
                    view.x += deltaX;
                    notesPanel.scrollRectToVisible(view);
                }
            }
        }
    }
}
