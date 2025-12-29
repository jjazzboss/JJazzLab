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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.AttributedString;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.uisettings.api.ColorSetManager;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.uiutilities.api.HSLColor;
import org.jjazz.uiutilities.api.StringMetrics;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;

/**
 * The ruler panel that shows the beat position marks + time signatures over the NotesPanel.
 * <p>
 * The ruler has its own startBar which might be different from the model phrase start bar.
 * <p>
 * If a song is associated, show also the chord symbols and song parts. The RulerPanel listens to chord symbols changes to refresh itself (song
 * structure changes are handled by the PianoRollEditor).
 */
public class RulerPanel extends JPanel implements PropertyChangeListener
{

    private static final int BAR_TICK_LENGTH = 7;
    private static final int BEAT_TICK_LENGTH = 5;
    private static final int SIXTEENTH_TICK_LENGTH = 3;
    private static final Color COLOR_LOOP_ZONE = new Color(113, 142, 169);
    private static final Color COLOR_BAR_FONT = new Color(176, 199, 220);
    private static final Color COLOR_BEAT_FONT = new Color(80, 80, 80);
    private static final Color COLOR_TIME_SIGNATURE_FONT = COLOR_BAR_FONT;
    private static final Color COLOR_CHORD_SYMBOL_FONT = COLOR_BAR_FONT;
    private static final Color COLOR_SONG_PART_FONT = Color.BLACK;
    private static final Color COLOR_BEAT_TICK = COLOR_BEAT_FONT;
    private static final int PLAYBACK_POINT_HALF_SIZE = 4;
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
        this.editor.addPropertyChangeListener(PianoRollEditor.PROP_CHORD_SEQUENCE, this);

        this.notesPanel = notesPanel;
        this.xMapper = notesPanel.getXMapper();


        this.editor.getSettings().addPropertyChangeListener(this);

        setToolTipText(ResUtil.getString(getClass(), "RulerPanelTooltip"));
        setAutoscrolls(true);   // needed for autoscroll when dragging beyond ruler left/right bounds


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
        BASE_FONT_HEIGHT = (float) StringMetrics.create(g2, f).getLogicalBoundsNoLeadingNoDescent("9").getHeight();
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
        int oldX = playbackPointX;
        playbackPointX = xPos;
        if (playbackPointX != oldX)
        {
            int x0, x1;
            if (oldX == -1)
            {
                x0 = xPos - PLAYBACK_POINT_HALF_SIZE;
                x1 = xPos + PLAYBACK_POINT_HALF_SIZE;
            } else if (playbackPointX == -1)
            {
                x0 = oldX - PLAYBACK_POINT_HALF_SIZE;
                x1 = oldX + PLAYBACK_POINT_HALF_SIZE;
            } else
            {
                x0 = Math.min(playbackPointX, oldX) - PLAYBACK_POINT_HALF_SIZE;
                x1 = Math.max(playbackPointX, oldX) + PLAYBACK_POINT_HALF_SIZE;
            }
            repaint(x0, 0, x1 - x0 + 1, getHeight());
        }
    }

    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        var clip = g2.getClipBounds();
        // Make sure clip is restricted to a valid area
        SwingUtilities.computeIntersection(0, 0, notesPanel.getWidth(), getHeight(), clip);
        if (clip.isEmpty())
        {
            return;
        }

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
        var loopZone = editor.getLoopZone();

        // Default background
        g2.setColor(settings.getRulerBackgroundColor());
        g2.fillRect(0, yBottomTimeSignatureLane + 1, getWidth(), song != null ? CHORD_SYMBOL_LANE_HEIGHT + BAR_LANE_HEIGHT : BAR_LANE_HEIGHT);


        // Paint time signature lane background
        g2.setColor(settings.getRulerTsLaneBackgroundColor());
        g2.fillRect(0, yTopTimeSignatureLane, getWidth(), TIME_SIGNATURE_LANE_HEIGHT);


        // Get X coordinate of all beat positions
        var vbr = editor.getVisibleBarRange();
        if (vbr.isEmpty())
        {
            return;
        }
        var tmapPosX = xMapper.getAllBeatsX(vbr);
        var allBeatPositions = tmapPosX.navigableKeySet();
        float oneBeatPixelSize = xMapper.getOneBeatPixelSize();


        // Draw chord symbols
        if (song != null)
        {
            int lastCliCsBar = -1;
            for (var cliCs : editor.getChordSequence())
            {
                var posCliCs = cliCs.getPosition();

                if (oneBeatPixelSize > 14 || (posCliCs.getBar() != lastCliCsBar && posCliCs.isFirstBarBeat()))
                {
                    // Draw the chord symbol
                    var posInBeats = editor.toPhraseRelativeBeatPosition(cliCs);
                    int x = editor.getXFromPosition(posInBeats);
                    int y = yBottomChordSymbolLane - 1;
                    AttributedString aStr;
                    aStr = new AttributedString(cliCs.getData().getOriginalName(), baseFont.getAttributes());
                    aStr.addAttribute(TextAttribute.FOREGROUND, COLOR_CHORD_SYMBOL_FONT);
                    aStr.addAttribute(TextAttribute.BACKGROUND, settings.getRulerBackgroundColor());    // so that some chords remain visible when they overlap
                    if (posCliCs.isFirstBarBeat())
                    {
                        x += 1;
                    }
                    g2.drawString(aStr.getIterator(), x, y);
                } else
                {
                    // No room to draw several chord symbols per bar
                    continue;
                }

                lastCliCsBar = posCliCs.getBar();
            }
        }


        // Draw ticks, bar/beat number, time signatures, vertical bars, and possibly song part and loop zone
        boolean paintSixteenthTicks = oneBeatPixelSize > 40;
        boolean paintBeatTicks = oneBeatPixelSize > 5;
        TimeSignature lastTs = null;
        for (Position pos : allBeatPositions)
        {
            float posInBeats = xMapper.getBeatPosition(pos);
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

                StringMetrics sm = StringMetrics.create(g2);
                xSongPart = x + 1 + (int) Math.round(sm.getWidth(text)) + 3;

                lastTs = ts;
            }


            // Possibly draw song part
            if (song != null && pos.isFirstBarBeat())
            {
                int offsettedBar = pos.getBar() + editor.getRulerStartBar();
                var spt = song.getSongStructure().getSongPart(offsettedBar);
                if (spt.getStartBarIndex() == offsettedBar)
                {
                    String sptName = spt.getName();
                    String secName = spt.getParentSection().getData().getName();
                    String sptLabel = sptName.equals(secName)
                        ? sptName
                        : sptName + "(" + secName + ")";
                    StringMetrics sm = StringMetrics.create(g2, SMALL_FONT);
                    var bounds = sm.getLogicalBoundsNoLeadingNoDescent(sptLabel);

                    // Draw rounded box
                    float PADDING = 1;
                    double wRect = bounds.getWidth() + 2 * PADDING;
                    double hRect = bounds.getHeight() + 1 * PADDING;
                    double xRect = xSongPart;
                    double yRect = yTimeSignatureBaseLine + PADDING - hRect;
                    var r = new Rectangle2D.Double(xRect, yRect, wRect, hRect);
                    Color c = ColorSetManager.getDefault().getColor(spt.getParentSection());
                    g2.setColor(c);
                    g2.fill(r);

                    // Draw song part name
                    float xStr = xSongPart + PADDING;
                    g2.setFont(SMALL_FONT);
                    g2.setColor(COLOR_SONG_PART_FONT);
                    g2.drawString(sptLabel, xStr, yTimeSignatureBaseLine);
                }
            }


            // Draw tick
            if (pos.isFirstBarBeat())
            {
                Color c = settings.getRulerBarTickColor();
                int tickLength = BAR_TICK_LENGTH;
                g2.setColor(c);
                g2.drawLine(x, yTopBarLane, x, yTopBarLane + tickLength - 1);
            } else if (paintBeatTicks)
            {
                Color c = COLOR_BEAT_TICK;
                int tickLength = BEAT_TICK_LENGTH;
                g2.setColor(c);
                g2.drawLine(x, yTopBarLane, x, yTopBarLane + tickLength - 1);
            }


            // Draw sixteenth ticks
            if (paintSixteenthTicks)
            {
                g2.setColor(COLOR_SIXTEENTH_TICK);
                float xSixteenthTick = x;
                float sixteenthBeatPixelSize = oneBeatPixelSize / 4;
                for (int i = 0; i < 3; i++)
                {
                    xSixteenthTick += sixteenthBeatPixelSize;
                    int xSixteenthTickInt = Math.round(xSixteenthTick);
                    g2.drawLine(xSixteenthTickInt, yTopBarLane, xSixteenthTickInt, yTopBarLane + SIXTEENTH_TICK_LENGTH - 1);
                }
            }


            // Draw the loop zone
            if (loopZone != null)
            {
                final int SIDE_LENGTH = 14;
                g2.setColor(COLOR_LOOP_ZONE);
                int y = yBottomBarLane;
                int bar = pos.getBar();
                int oneBeatPixelSizeInt = Math.round(oneBeatPixelSize);

                if (bar == loopZone.from && pos.isFirstBarBeat())
                {
                    // Loop zone start
                    int x2 = x;
                    if (x2 == 0)
                    {
                        x2 = SIDE_LENGTH - 1;       // Make sure polygon remains visible even at extreme left
                    }
                    Polygon p = new Polygon();
                    p.addPoint(x2, y);
                    p.addPoint(x2, y - SIDE_LENGTH + 1);
                    p.addPoint(x2 - SIDE_LENGTH + 1, y);
                    g2.draw(p);
                    g2.fill(p);
                } else if (bar == loopZone.to && pos.isLastBarBeat(ts))
                {
                    // Loop zone end

                    int x2 = x + oneBeatPixelSizeInt;
                    if (pos == allBeatPositions.last())
                    {
                        x2 -= SIDE_LENGTH;           // Make sure polygon remains visible even at extreme right
                    }
                    Polygon p = new Polygon();
                    p.addPoint(x2, y);
                    p.addPoint(x2, y - SIDE_LENGTH + 1);
                    p.addPoint(x2 + SIDE_LENGTH - 1, y);
                    g2.draw(p);
                    g2.fill(p);
                }
                if (loopZone.contains(bar))
                {
                    // Draw a thin rectangle at the bottom
                    double RECT_HEIGHT = 2d;
                    var r = new Rectangle2D.Double(x, y - RECT_HEIGHT + 1, oneBeatPixelSize, RECT_HEIGHT);
                    g2.draw(r);
                    g2.fill(r);
                }
            }


            // Draw bar or beat number
            AttributedString aStr = null;
            int offsettedBar = pos.getBar() + 1 + editor.getRulerStartBar();
            if (pos.isFirstBarBeat())
            {
                var strBar = String.valueOf(offsettedBar);
                aStr = new AttributedString(strBar, baseFont.getAttributes());
                aStr.addAttribute(TextAttribute.FOREGROUND, COLOR_BAR_FONT);

            } else if (paintSixteenthTicks)
            {
                var strBeat = String.valueOf(offsettedBar) + "." + String.valueOf((int) pos.getBeat() + 1);
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
            g2.setColor(NotesPanelLayerUI.COLOR_PLAYBACK_LINE);
            Polygon p = new Polygon();
            int yMax = h - 1;
            p.addPoint(playbackPointX, yMax);
            p.addPoint(playbackPointX + PLAYBACK_POINT_HALF_SIZE, yMax - PLAYBACK_POINT_HALF_SIZE);
            p.addPoint(playbackPointX - PLAYBACK_POINT_HALF_SIZE, yMax - PLAYBACK_POINT_HALF_SIZE);
            g2.draw(p);
            g2.fill(p);
        }
    }

    public void cleanup()
    {
        editor.removePropertyChangeListener(this);
        editor.getSettings().removePropertyChangeListener(this);
    }

    // ==========================================================================================================
    //PropertyChangeListener interface
    // ========================================================================================================== 
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.log(Level.FINE, "propertyChange() evt.source.class={0} prop={1} old={2} new={3}", new Object[]
        {
            evt.getSource().getClass().getSimpleName(), evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()
        });

        if (evt.getSource() == editor.getSettings())
        {
            settingsChanged();
        } else if (evt.getSource() == editor)
        {
            switch (evt.getPropertyName())
            {
                case PianoRollEditor.PROP_CHORD_SEQUENCE, PianoRollEditor.PROP_LOOP_ZONE, PianoRollEditor.PROP_MODEL_PHRASE, PianoRollEditor.PROP_PLAYBACK_POINT_POSITION ->
                {
                    repaint();
                }
            }
        }
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================    
    private void settingsChanged()
    {
        repaint();
    }

    /**
     * Manage click and dragging operations on the ruler.
     * <p>
     * - dragging sets the loop zone by dragging the ruler (shift+drag to extend existing loop zone)<br>
     * - A simple click resets the loop zone and update the selection on chord leadsheet and song structure editors.<br>
     * - A shift+click extends loop zone if already set.
     */
    private class RulerMouseAdapter extends MouseAdapter
    {

        private int xOrigin = Integer.MIN_VALUE;    // If MIN_VALUE drag has not started
        private int notesPanelVisibleY = -1;          // Needed for autoscroll
        private int loopZoneBarOrigin = -1;         // If >= 0 it's a drag to set the loop zone

        @Override
        public void mousePressed(MouseEvent e)
        {
            LOGGER.fine("mousePressed() --");
            xOrigin = e.getX();
            if (!e.isControlDown())
            {
                var loopZone = editor.getLoopZone();
                int bar = xMapper.getPositionFromX(xOrigin).getBar();
                if (e.isShiftDown() && loopZone != null)
                {
                    if (bar < loopZone.from)
                    {
                        loopZoneBarOrigin = loopZone.to;
                        editor.setLoopZone(new IntRange(bar, loopZoneBarOrigin));
                    } else
                    {
                        loopZoneBarOrigin = loopZone.from;
                        editor.setLoopZone(new IntRange(loopZoneBarOrigin, bar));
                    }
                } else
                {
                    // Start setting new loop zone
                    loopZoneBarOrigin = bar;
                }

                notesPanelVisibleY = notesPanel.getVisibleRect().y;
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            LOGGER.fine("mouseClicked() --");

            if (song == null)
            {
                return;
            }


            float posInBeats = editor.getPositionFromPoint(e.getPoint()); // ignore y
            if (posInBeats == -1 || !editor.getPhraseBeatRange().contains(posInBeats, true))
            {
                return;
            }
            Position pos = editor.toPosition(posInBeats);


            var loopZone = editor.getLoopZone();
            if (e.isShiftDown() && loopZone != null)
            {
                // Do nothing
                return;
            }


            // Reset loop zone
            editor.setLoopZone(null);


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
            var cliCs = song.getChordLeadSheet().getLastItemBefore(clsPos, true, CLI_ChordSymbol.class, cli -> true);
            if (cliCs != null)
            {
                var clTc = CL_EditorTopComponent.get(song.getChordLeadSheet());
                var clEditor = clTc.getEditor();
                clEditor.clearSelection();
                clEditor.selectItem(cliCs, true);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            xOrigin = Integer.MIN_VALUE;
            loopZoneBarOrigin = -1;
            notesPanelVisibleY = -1;
            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (xOrigin < 0 || loopZoneBarOrigin < 0 || e.isControlDown())
            {
                return;
            }

            // Set loop zone
            Position pos = xMapper.getPositionFromX(e.getX());
            if (pos != null)
            {
                int bar = xMapper.getPositionFromX(e.getX()).getBar();
                int min = Math.min(bar, loopZoneBarOrigin);
                int max = Math.max(bar, loopZoneBarOrigin);
                editor.setLoopZone(new IntRange(min, max));
            }

            if (notesPanelVisibleY != -1)
            {
                Rectangle r = new Rectangle(e.getX(), notesPanelVisibleY, 1, 1);
                notesPanel.scrollRectToVisible(r);
            }
        }
    }
}
