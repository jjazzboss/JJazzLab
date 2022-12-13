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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent;
import org.jjazz.ui.keyboardcomponent.api.PianoKey;
import org.jjazz.ui.utilities.api.HSLColor;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;

/**
 * Show the grid and hold the NoteViews.
 * <p>
 * Work in conjunction with a vertical KeyboardComponent on the left side which must be in the same enclosing panel. Provide the
 * XMapper and YMapper which give the reference positions.
 */
public class NotesPanel extends javax.swing.JPanel
{

    private final KeyboardComponent keyboard;
    private final YMapper yMapper;
    private final XMapper xMapper;
    private static final Logger LOGGER = Logger.getLogger(NotesPanel.class.getSimpleName());
    private final PianoRollEditorImpl editor;
    private float scaleFactorX = 1f;


    public NotesPanel(PianoRollEditorImpl editor, KeyboardComponent keyboard)
    {
        this.editor = editor;
        this.keyboard = keyboard;
        this.xMapper = new XMapper();
        this.yMapper = new YMapper();

        this.keyboard.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                // LOGGER.severe("componentResized() -- keyboard.getHeight()=" + keyboard.getHeight());
                yMapper.refresh(keyboard.getHeight());
                
                // Make sure editor y position does not move with the rescale
                
                revalidate();
                repaint();
            }
        });

    }

    @Override
    public Dimension getPreferredSize()
    {
        int h = keyboard.getPreferredSize().height;
        int w = (int) (xMapper.getBeatRange().size() * 50 * scaleFactorX);
        var res = new Dimension(w, h);
        // LOGGER.severe("getPreferredSize() res=" + res);
        return res;
    }

    /**
     *
     * @param factorX A value &gt; 0
     */
    public void setZoomX(float factorX)
    {
        Preconditions.checkArgument(factorX > 0);

        if (scaleFactorX != factorX)
        {
            scaleFactorX = factorX;
            revalidate();
            repaint();
        }
    }


    public float getZoomX()
    {
        return scaleFactorX;
    }

    public YMapper getYMapper()
    {
        return yMapper;
    }

    public XMapper getXMapper()
    {
        return xMapper;
    }


    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);        // Honor the opaque property
        Graphics2D g2 = (Graphics2D) g;

        // LOGGER.severe("paintComponent() -- width=" + getWidth() + " h=" + getHeight() + " yMapper.getLastKeyboardHeight()=" + yMapper.getLastKeyboardHeight());

        if (yMapper.getLastKeyboardHeight() != getHeight())
        {
            // yMapper is not consistent, don't draw anything
            return;
        }

        drawHorizontalGrid(g2);

        drawVerticalGrid(g2);

    }


    // ========================================================================================
    // Private methods
    // ========================================================================================
    private void drawHorizontalGrid(Graphics2D g2)
    {
        var settings = PianoRollEditorSettings.getDefault();
        if (settings == null)
        {
            return;
        }
        Color cb1 = settings.getBackgroundColor1();
        Color cb2 = settings.getBackgroundColor2();
        Color cl1 = settings.getBarLineColor();
        Color cl2 = HSLColor.changeLuminance(cl1, 8);      // lighter color
        var kbdRange = keyboard.getRange();

        int w = getWidth();

        for (int p = kbdRange.getLowestPitch(); p <= kbdRange.getHighestPitch(); p++)
        {
            int pp = p % 12;
            Color c = (pp == 0 || pp == 2 || pp == 4 || pp == 5 || pp == 7 || pp == 9 || pp == 11) ? cb2 : cb1;
            g2.setColor(c);
            var yRange = yMapper.getNoteViewChannelYRange(p);
            g2.fillRect(0, yRange.from, w, yRange.size());
            if (pp == 0 || pp == 5)
            {
                g2.setColor(pp == 0 ? cl1 : cl2);
                g2.drawLine(0, yRange.to, w - 1, yRange.to);
            }
        }


    }

    private void drawVerticalGrid(Graphics2D g2)
    {
        var settings = PianoRollEditorSettings.getDefault();
        if (settings == null)
        {
            return;
        }
        Color cl1 = settings.getBarLineColor();
        Color cl2 = HSLColor.changeLuminance(cl1, 8);      // lighter color
        Color cl3 = HSLColor.changeLuminance(cl2, 4);      // lighter color

        int y0 = yMapper.getKeyboardYRange(0).to;
        int y1 = yMapper.getKeyboardYRange(127).from;
        var mapQPosX = xMapper.getAllQuantizedXPositions();

        for (Position pos : mapQPosX.navigableKeySet())
        {
            int x = mapQPosX.get(pos);
            Color c = cl1;
            if (!pos.isFirstBarBeat())
            {
                c = pos.isOffBeat() ? cl3 : cl2;
            }

            g2.setColor(c);
            g2.drawLine(x, y0, x, y1);
        }
    }

    // =====================================================================================
    // Inner classes
    // =====================================================================================
    /**
     * Conversion methods between keyboard's Y coordinate, pitch and NoteView line's Y coordinate.
     */
    public class YMapper
    {

        /**
         * The keyboard height used for the last YMapper refresh.
         */
        private int lastKeyboardHeight = -1;
        private final NavigableMap<Integer, Integer> tmapPixelPitch = new TreeMap<>();
        private final Map<Integer, IntRange> mapPitchChannelYRange = new HashMap<>();
        private int noteViewHeight;

        /**
         * To be called when associated keyboard height has changed.
         * <p>
         * Recompute internal data.
         *
         * @param kbdHeight
         * @return False is refresh was not needed
         */
        private boolean refresh(int kbdHeight)
        {
            Preconditions.checkArgument(kbdHeight > 0);
            if (kbdHeight == lastKeyboardHeight)
            {
                // LOGGER.severe("NotesPanel.YMapper.refresh() -- no change");
                return false;
            }
            lastKeyboardHeight = kbdHeight;
            // LOGGER.severe("NotesPanel.YMapper.refresh() updating for kbdHeight=" + kbdHeight);

            // Compute the large NoteView line height (for C, E, F, B) and the small NoteView line height (for the other notes)
            var wKeyC0 = keyboard.getKey(0); // first C
            assert wKeyC0.getPitch() % 12 == 0 : wKeyC0;
            var yRangeC0 = getKeyboardYRange(wKeyC0);
            float adjustedLargeHeight = 24f * yRangeC0.size() / 32f;
            var wKeyC1 = keyboard.getKey(12); // 2nd C
            var yRangeC1 = getKeyboardYRange(wKeyC1);
            int octaveHeight = yRangeC0.to - yRangeC1.to;
            assert octaveHeight > 5 : "octaveHeight=" + octaveHeight;
            float adjustedSmallHeight = (octaveHeight - 4 * adjustedLargeHeight) / 8;       // So we can accomodate 4 small + 4 large


            noteViewHeight = (int) Math.floor(adjustedSmallHeight);


            // Fill in the maps
            float y = yRangeC0.to;
            tmapPixelPitch.clear();
            mapPitchChannelYRange.clear();
            var kbdRange = keyboard.getRange();


            for (int p = kbdRange.getLowestPitch(); p <= kbdRange.getHighestPitch(); p++)
            {
                int yi = Math.round(y);
                tmapPixelPitch.put(yi, p);
                int pp = p % 12;
                float yUp = (pp == 0 || pp == 4 || pp == 5 || pp == 11) ? adjustedLargeHeight : adjustedSmallHeight;
                IntRange channelNoteYRange = new IntRange(Math.round(y - yUp + 1), yi);
                mapPitchChannelYRange.put(p, channelNoteYRange);
                y -= yUp;
            }

            return true;
        }

        /**
         * The last keyboard height used to refresh this object.
         *
         * @return -1 if not refreshed yet.
         */
        public int getLastKeyboardHeight()
        {
            return lastKeyboardHeight;
        }

        /**
         * The NoteView height to be used to fit the NoteView lines.
         *
         * @return
         */
        public int getNoteViewHeight()
        {
            return noteViewHeight;
        }

        /**
         * Get the pitch corresponding to the specified yPos in this panel's coordinates.
         *
         * @param yPos
         * @return
         */
        public int getPitch(int yPos)
        {
            return tmapPixelPitch.ceilingEntry(yPos).getValue();
        }

        /**
         * Get the Y range of the NoteView channel for the specified pitch.
         *
         * @param pitch
         * @return
         */
        public IntRange getNoteViewChannelYRange(int pitch)
        {
            var res = mapPitchChannelYRange.get(pitch);
            if (res == null)
            {
                throw new IllegalArgumentException("pitch=" + pitch + " mapPitchChannelYRange=" + mapPitchChannelYRange);
            }
            return res;
        }

        /**
         * Get the Y range of the specified keyboard key.
         *
         * @param pitch
         * @return
         */
        public IntRange getKeyboardYRange(int pitch)
        {
            return getKeyboardYRange(keyboard.getKey(pitch));
        }

        /**
         * Get the Y range of the specified keyboard key.
         *
         * @param key
         * @return
         */
        public IntRange getKeyboardYRange(PianoKey key)
        {
            int yTop = key.getY();
            int yBottom = yTop + key.getHeight();
            return new IntRange(yTop, yBottom);
        }


    }

    /**
     * Conversion methods between NoteView X coordinate and position in beats.
     */
    public class XMapper
    {

        private Quantization quantization = Quantization.ONE_QUARTER_BEAT;


        public FloatRange getBeatRange()
        {
            return editor.getModel().getBeatRange();
        }


        public IntRange getBarRange()
        {
            int nbBars = (int) (getBeatRange().size() / editor.getModel().getTimeSignature().getNbNaturalBeats());
            return new IntRange(editor.getStartBarIndex(), editor.getStartBarIndex() + nbBars - 1);
        }

        public Quantization getQuantization()
        {
            return quantization;
        }

        public void setQuantization(Quantization quantization)
        {
            this.quantization = quantization;
            repaint();
        }

        /**
         * Get the X coordinate of each quantized position.
         *
         * @return
         */
        public NavigableMap<Position, Integer> getAllQuantizedXPositions()
        {
            NavigableMap<Position, Integer> res = new TreeMap<>();
            int nbBeatsPerTs = (int) editor.getModel().getTimeSignature().getNbNaturalBeats();

            for (int bar = getBarRange().from; bar <= getBarRange().to; bar++)
            {
                for (int beat = 0; beat < nbBeatsPerTs; beat++)
                {
                    for (float qBeat : quantization.getBeats())
                    {
                        if (qBeat == 1)
                        {
                            continue;
                        }
                        var pos = new Position(bar, beat + qBeat);
                        int xPos = getX(pos);
                        res.put(pos, xPos);
                    }
                }
            }
            return res;
        }

        /**
         * Get the X coordinate of each integer beat.
         *
         * @return
         */
        public NavigableMap<Position, Integer> getAllBeatsXPositions()
        {
            NavigableMap<Position, Integer> res = new TreeMap<>();
            int nbBeatsPerTs = (int) editor.getModel().getTimeSignature().getNbNaturalBeats();

            for (int bar = getBarRange().from; bar <= getBarRange().to; bar++)
            {
                for (int beat = 0; beat < nbBeatsPerTs; beat++)
                {
                    var pos = new Position(bar, beat);
                    var x = getX(pos);
                    res.put(pos, x);
                }
            }
            return res;
        }

        /**
         * Get the size in pixels of 1 beat.
         *
         * @return
         */
        public float getOneBeatPixelSize()
        {
            return getWidth() / getBeatRange().size();
        }

        /**
         * Get the beat position corresponding to the specified xPos in this panel's coordinates.
         *
         * @param yPos
         * @return A beat position within getBeatRange().
         */
        public float getPositionInBeats(int xPos)
        {
            int w = getWidth();
            Preconditions.checkArgument(xPos >= 0 && xPos < w, "xPos=%d w=%d", xPos, w);
            float relPos = xPos / getOneBeatPixelSize();
            float absPos = getBeatRange().from + relPos;
            return absPos;
        }

        /**
         * Get the position (bar, beat) corresponding to the specified xPos in this panel's coordinates.
         *
         * @param yPos
         * @return
         */
        public Position getPosition(int xPos)
        {
            var posInBeats = getPositionInBeats(xPos);
            return toPosition(posInBeats);
        }

        /**
         *
         * Get the X position (in this panel's coordinates) corresponding to the specified beat position.
         *
         * @param posInBeats A beat position within getBeatRange()
         * @return
         */
        public int getX(float posInBeats)
        {
            Preconditions.checkArgument(getBeatRange().contains(posInBeats, true), "posInBeats=%s", posInBeats);
            int w = getWidth();
            int xPos = Math.round((posInBeats - getBeatRange().from) * getOneBeatPixelSize());
            xPos = Math.min(w - 1, xPos);
            return xPos;
        }

        /**
         *
         * Get the X position (in this panel's coordinates) corresponding to the specified position in bar/beat.
         *
         * @param posInBeats A beat position within getBeatRange()
         * @return
         */
        public int getX(Position pos)
        {
            var xPos = getX(toPositionInBeats(pos));
            return xPos;
        }

        /**
         * Convert a position in beats into a bar/beat position.
         *
         * @param posInBeats
         * @return
         */
        public Position toPosition(float posInBeats)
        {
            Preconditions.checkArgument(getBeatRange().contains(posInBeats, true), "posInBeats=%s", posInBeats);
            float nbBeatsPerTs = editor.getModel().getTimeSignature().getNbNaturalBeats();
            int relBar = (int) ((posInBeats - getBeatRange().from) / nbBeatsPerTs);
            float beatWithinBar = posInBeats - getBeatRange().from - relBar * nbBeatsPerTs;
            return new Position(editor.getStartBarIndex() + relBar, beatWithinBar);
        }

        /**
         * Convert a position in bar/beat into a position in beats.
         *
         * @param pos
         * @return
         */
        public float toPositionInBeats(Position pos)
        {
            float posInBeats = getBeatRange().from + pos.getPositionInBeats(editor.getModel().getTimeSignature());
            Preconditions.checkArgument(getBeatRange().contains(posInBeats, true), "pos=%s getBeatRange()=%s", pos, getBeatRange());
            return posInBeats;
        }
    }


}
