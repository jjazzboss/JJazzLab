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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.utilities.api.HSLColor;
import org.jjazz.ui.utilities.api.StringMetrics;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.util.api.Utilities;

/**
 * The ruler panel that shows the beat position marks over the NotesPanel.
 */
public class RulerPanel extends javax.swing.JPanel implements PropertyChangeListener
{

    private static final int BAR_TICK_LENGTH = 5;
    private static final Color COLOR_BAR_TICK = new Color(160, 160, 160);
    private static final Color COLOR_BEAT_TICK = new Color(90, 90, 90);
    private static final Color COLOR_BAR_FONT = new Color(176, 199, 220);
    private static final Color COLOR_BEAT_FONT = new Color(80, 80, 80);
    private final NotesPanel notesPanel;
    private final NotesPanel.XMapper xMapper;
    private final PianoRollEditor editor;
    private final Font fontBar;
    private final Font fontBeat;
    private static final Logger LOGGER = Logger.getLogger(RulerPanel.class.getSimpleName());
    private int playbackPointX = -1;
    private int preferredHeight;
    private float fontHeight;
    private int upperLaneHeight;
    private boolean showUpperLane;
    private TreeSet<Object> upperLaneObjects;

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


        editor.getSettings().addPropertyChangeListener(this);
        settingsChanged();


        fontBar = GeneralUISettings.getInstance().getStdFont().deriveFont(12f);
        fontBeat = fontBar.deriveFont(fontBar.getSize() - 3f);


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

        updatePreferredHeight();
    }


    /**
     * Show the upper lane to display chords and song parts.
     * <p>
     * Do nothing if no song is associated to PianoRollEditor.
     *
     * @param b
     */
    public void setTextLaneVisible(boolean b)
    {
        if (b == showUpperLane || editor.getSong() == null)
        {
            return;
        }

        showUpperLane = b;
        updatePreferredHeight();

        revalidate();
        repaint();
    }

    public boolean isTextLaneVisible()
    {
        return showUpperLane;
    }

    @Override
    public Dimension getPreferredSize()
    {
        var pd = new Dimension(notesPanel.getWidth(), preferredHeight);
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
        super.paintComponent(g);        // Honor the opaque property

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);


        // Prepare data
        int h = getHeight();
        int yTick = showUpperLane ? upperLaneHeight : 0;
        int beatTickLength = BAR_TICK_LENGTH / 2;
        int subTickLength = beatTickLength - 2;
        float oneBeatPixelSize = xMapper.getOneBeatPixelSize();
        float subTickWidth = oneBeatPixelSize / 4;
        Color subTickColor = HSLColor.changeLuminance(COLOR_BEAT_TICK, -2);


        // Get X coordinate of all beat positions
        var tmapPosX = xMapper.getBeatsXPositions(null);
        var allBeatPositions = tmapPosX.navigableKeySet();


        // Draw upper lane
        if (showUpperLane && upperLaneObjects == null && editor.isReady())
        {
            updateUpperLaneObjects();
        }
        if (showUpperLane && upperLaneObjects != null)
        {
            // Different background
            Color c = HSLColor.changeLuminance(getBackground(), 2);
            g2.setColor(c);
            g2.fillRect(0, 0, getWidth(), upperLaneHeight);


            // Draw objects as string
            for (var obj : upperLaneObjects)
            {
                var pos = getPos(obj);
                var posInBeats = editor.toPositionInBeats(pos);
                int x = editor.getXFromPosition(posInBeats);
                int y = upperLaneHeight - 2;
                AttributedString aStr = new AttributedString(getString(obj), fontBar.getAttributes());
                aStr.addAttribute(TextAttribute.FOREGROUND, COLOR_BAR_FONT);
                if (obj instanceof SongPart)
                {
                    x += 20;
                    aStr.addAttribute(TextAttribute.FOREGROUND, Color.YELLOW.darker());
                }
                g2.drawString(aStr.getIterator(), x, y);
            }
        }


        // Draw ticks + bar/beat
        boolean paintSubTicks = oneBeatPixelSize > 60;
        TimeSignature oldTs = null;

        for (Position pos : allBeatPositions)
        {
            int x = tmapPosX.get(pos);

            // Draw tick
            Color c = pos.isFirstBarBeat() ? COLOR_BAR_TICK : COLOR_BAR_TICK;
            int tickLength = pos.isFirstBarBeat() ? BAR_TICK_LENGTH : beatTickLength;
            g2.setColor(c);
            g2.drawLine(x, yTick, x, yTick + tickLength);


            // Draw subticks
            if (paintSubTicks)
            {
                g2.setColor(subTickColor);
                float xf = x;
                for (int i = 0; i < 3; i++)
                {
                    xf += subTickWidth;
                    int xfi = Math.round(xf);
                    g2.drawLine(xfi, yTick, xfi, yTick + subTickLength);
                }
            }


            // Draw bar or beat number
            AttributedString aStr = null;
            if (pos.isFirstBarBeat())
            {
                var strBar = String.valueOf(pos.getBar() + 1) + " ";
                var strTs = "";
                var newTs = editor.getTimeSignature(editor.toPositionInBeats(pos));
                if (!newTs.equals(oldTs))
                {
                    strTs = newTs.toString();
                    oldTs = newTs;
                }
                aStr = new AttributedString(strBar + strTs, fontBar.getAttributes());
                aStr.addAttribute(TextAttribute.FOREGROUND, COLOR_BAR_FONT);
                if (!strTs.isEmpty())
                {
                    // Make time signature use smaller font
                    int from = strBar.length();
                    int to = strBar.length() + strTs.length();
                    aStr.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, from, to);
//                    aStr.addAttribute(TextAttribute.SIZE, fontBar.getSize2D() * 0.9, from, to);
//                    aStr.addAttribute(TextAttribute.BACKGROUND, Color.BLACK, from, to);
                }

            } else if (paintSubTicks)
            {
                var strBeat = String.valueOf(pos.getBar() + 1) + "." + String.valueOf((int) pos.getBeat() + 1);
                aStr = new AttributedString(strBeat, fontBeat.getAttributes());
                aStr.addAttribute(TextAttribute.FOREGROUND, COLOR_BEAT_FONT);
            }
            if (aStr != null)
            {
                // Draw the bar or beat number
                float xStr = x;
                float yStr = h - 2;           // text baseline position
                g2.drawString(aStr.getIterator(), xStr, yStr);
            }

        }

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
    }

    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == editor.getSettings())
        {
            settingsChanged();
        }
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================    

    /**
     * Prepare the upperLaneObjects data for paintComponent().
     * <p>
     */
    private boolean updateUpperLaneObjects()
    {
        assert editor.isReady();

        upperLaneObjects = new TreeSet<>((o1, o2) ->
        {
            Position pos1 = getPos(o1);
            Position pos2 = getPos(o2);
            int res = pos1.compareTo(pos2);
            if (res == 0)
            {
                // Make SongPart "before" chord symbol, so that chord symbol is drawn upon the song part
                if (o1 instanceof ChordLeadSheetItem && o2 instanceof SongPart)
                {
                    res = 1;
                } else if (o1 instanceof SongPart && o2 instanceof ChordLeadSheetItem)
                {
                    res = -1;
                }
            }
            return res;
        });

        Song song = editor.getSong();
        var barRange = editor.getBarRange();
        upperLaneObjects.addAll(song.getSongStructure().getSongParts(spt -> barRange.contains(spt.getStartBarIndex())));
        SongChordSequence cs = new SongChordSequence(song, barRange);
        upperLaneObjects.addAll(cs);
        return true;
    }

    private void settingsChanged()
    {
        setBackground(editor.getSettings().getRulerBackgroundColor());
    }

    private void updatePreferredHeight()
    {
        // Precalculate main fontHeight & preferred height
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        var bounds = new StringMetrics(g2, fontBar).getLogicalBoundsNoLeading("9");
        fontHeight = (float) bounds.getHeight();
        upperLaneHeight = showUpperLane ? (int) (fontHeight + 2) : 0;
        preferredHeight = (int) (upperLaneHeight + fontHeight + BAR_TICK_LENGTH + 1);
        g2.dispose();
    }

    private String getString(Object o)
    {
        String res;
        if (o instanceof SongPart spt)
        {
            res = spt.getName();
        } else if (o instanceof CLI_ChordSymbol cliCs)
        {
            res = cliCs.getData().getOriginalName();
        } else
        {
            throw new IllegalArgumentException("o=" + o);
        }
        return res;
    }

    private Position getPos(Object o)
    {
        Position res;
        if (o instanceof ChordLeadSheetItem item)
        {
            res = item.getPosition();
        } else if (o instanceof SongPart spt)
        {
            res = new Position(spt.getStartBarIndex(), 0);
        } else
        {
            throw new IllegalArgumentException("o=" + o);
        }

        return res;
    }
}
