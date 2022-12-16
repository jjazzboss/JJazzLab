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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent;
import org.jjazz.ui.keyboardcomponent.api.PianoKey;
import org.jjazz.ui.utilities.api.HSLColor;
import org.jjazz.util.api.IntRange;

/**
 * The main editor panel, shows the grid and holds the NoteViews.
 * <p>
 * Y metrics are taken from the associated vertical keyboard. The XMapper and YMapper objects provide the methods to position
 * notes.
 */
public class NotesPanel extends javax.swing.JPanel
{

    private final KeyboardComponent keyboard;
    private final YMapper yMapper;
    private final XMapper xMapper;
    private final PianoRollEditorImpl editor;
    private float scaleFactorX = 1f;
    private static final Logger LOGGER = Logger.getLogger(NotesPanel.class.getSimpleName());


    public NotesPanel(PianoRollEditorImpl editor, KeyboardComponent keyboard)
    {
        this.editor = editor;
        this.keyboard = keyboard;
        this.xMapper = new XMapper();
        this.yMapper = new YMapper();


        var mouseListener = new MyMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
    }

    /**
     * Early detection of size changes for xMapper update.
     * <p>
     * Ovverridden because this method is called (by parent's layoutManager) before component is painted and before the component
     * resized/moved event is fired. This lets us update xMapper as soon as possible.
     *
     * @param x
     * @param y
     * @param w
     * @param h
     */
    @Override
    public void setBounds(int x, int y, int w, int h)
    {
        // LOGGER.severe("setBounds() x=" + x + " y=" + y + " w=" + w + " h=" + h);        
        super.setBounds(x, y, w, h);
        if (w != xMapper.lastWidth)
        {
            xMapper.refresh();
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        int h = yMapper.lastKeyboardHeight;
        int w = (int) (editor.getBeatRange().size() * 50 * scaleFactorX);
        var res = new Dimension(w, h);
        LOGGER.log(Level.FINE, "getPreferredSize() res={0}", res);
        return res;
    }

    /**
     *
     * @param factorX A value &gt; 0
     */
    public void setScaleFactorX(float factorX)
    {
        Preconditions.checkArgument(factorX > 0);

        if (scaleFactorX != factorX)
        {
            scaleFactorX = factorX;
            revalidate();
            repaint();
        }
    }


    public float getScaleFactorX()
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

//        LOGGER.log(Level.FINE, "paintComponent() -- width={0} height={1} yMapper.lastKeyboardHeight={2}", new Object[]
//        {
//            getWidth(), getHeight(), yMapper.lastKeyboardHeight
//        });

        if (!yMapper.isUptodate() || !xMapper.isUptodate())
        {
            //  Don't draw anything
            // LOGGER.severe("paintComponent() xMapper or yMapper is not uptodate, abort painting");
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
        int w = getWidth();

        // only draw what's visible
        IntRange pitchRange = editor.getVisiblePitchRange();

        for (int p = pitchRange.from; p <= pitchRange.to; p++)
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
        var mapQPosX = xMapper.getQuantizedXPositions(editor.getVisibleBarRange());              // only draw what's visible        

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
     * Conversion methods between NoteView X coordinate and position in beats.
     */
    public class XMapper
    {

        private Quantization quantization = Quantization.ONE_QUARTER_BEAT;
        private int lastWidth = -1;
        private final NavigableMap<Position, Integer> tmap_allQuantizedXPositions = new TreeMap<>();
        private final NavigableMap<Position, Integer> tmap_allBeatsXPositions = new TreeMap<>();


        public boolean isUptodate()
        {
            return lastWidth == getWidth();
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
         * To be called when this panel width has changed.
         * <p>
         * Recompute internal data to make this xMapper up-to-date.
         * <p>
         */
        private synchronized void refresh()
        {
            LOGGER.log(Level.FINE, "XMapper.refresh() lastWidth={0} getWidth()={1}", new Object[]
            {
                lastWidth, getWidth()
            });
            if (getWidth() == lastWidth)
            {
                return;
            }
            lastWidth = getWidth();
            tmap_allQuantizedXPositions.clear();
            tmap_allBeatsXPositions.clear();
            var allBarRange = editor.getBarRange();
            int nbBeatsPerTs = (int) editor.getModel().getTimeSignature().getNbNaturalBeats();


            // Precompute all quantized + beat positions
            for (int bar = allBarRange.from; bar <= allBarRange.to; bar++)
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
                        tmap_allQuantizedXPositions.put(pos, xPos);
                        if (qBeat == 0)
                        {
                            tmap_allBeatsXPositions.put(pos, xPos);
                        }
                    }
                }
            }
        }


        /**
         * Get the X coordinate of each quantized position in the specified barRange.
         *
         * @param barRange If null, use this.getBarRange() instead.
         * @return
         * @throws IllegalStateException If this yMapper is not up-to-date.
         */
        public synchronized NavigableMap<Position, Integer> getQuantizedXPositions(IntRange barRange)
        {
//            LOGGER.log(Level.SEVERE, "getAllQuantizedXPositions() -- w={0} barRange={1} tmap_allQuantizedXPositions={2}", new Object[]
//            {
//                getWidth(), barRange, tmap_allQuantizedXPositions
//            });
            if (!isUptodate())
            {
                throw new IllegalStateException("lastWidth=" + lastWidth + " getWidth()=" + getWidth());
            }
            if (barRange == null)
            {
                barRange = editor.getBarRange();
            }
            var res = tmap_allQuantizedXPositions.subMap(new Position(barRange.from, 0), true, new Position(barRange.to + 1, 0), false);
            return res;
        }

        /**
         * Get the X coordinate of each integer beat in the specified barRange.
         *
         * @param barRange If null, use this.getBarRange() instead.
         * @return
         * @throws IllegalStateException If this yMapper is not up-to-date.
         */
        public synchronized NavigableMap<Position, Integer> getBeatsXPositions(IntRange barRange)
        {
//            LOGGER.log(Level.SEVERE, "getAllBeatsXPositions() -- w={0} barRange={1} tmap_allBeatsXPositions={2}", new Object[]
//            {
//                getWidth(), barRange, tmap_allBeatsXPositions
//            });
            if (!isUptodate())
            {
                throw new IllegalStateException("lastWidth=" + lastWidth + " getWidth()=" + getWidth());
            }

            if (barRange == null)
            {
                barRange = editor.getBarRange();
            }
            var res = tmap_allBeatsXPositions.subMap(new Position(barRange.from, 0), true, new Position(barRange.to + 1, 0), false);
            return res;
        }

        /**
         * Get the size in pixels of 1 beat.
         *
         * @return
         */
        public float getOneBeatPixelSize()
        {
            return getWidth() / editor.getBeatRange().size();
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
            float absPos = editor.getBeatRange().from + relPos;
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
            var br = editor.getBeatRange();
            Preconditions.checkArgument(br.contains(posInBeats, true), "posInBeats=%s", posInBeats);
            int w = getWidth();
            int xPos = Math.round((posInBeats - br.from) * getOneBeatPixelSize());
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
            var br = editor.getBeatRange();
            Preconditions.checkArgument(br.contains(posInBeats, true), "posInBeats=%s", posInBeats);
            float nbBeatsPerTs = editor.getModel().getTimeSignature().getNbNaturalBeats();
            int relBar = (int) ((posInBeats - br.from) / nbBeatsPerTs);
            float beatWithinBar = posInBeats - br.from - relBar * nbBeatsPerTs;
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
            var br = editor.getBeatRange();
            float posInBeats = br.from + pos.getPositionInBeats(editor.getModel().getTimeSignature());
            Preconditions.checkArgument(br.contains(posInBeats, true), "pos=%s getBeatRange()=%s posInBeats=%s", pos, br, posInBeats);
            return posInBeats;
        }
    }


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

        private YMapper()
        {
            keyboard.addComponentListener(new ComponentAdapter()
            {
                /**
                 * Note that componentResized() event arrives AFTER keyboard is layouted and repainted.
                 */
                @Override
                public synchronized void componentResized(ComponentEvent e)
                {
                    LOGGER.log(Level.FINE, "YMapper.componentResized() --");
                    refresh(keyboard.getPreferredSize().height);    // This will also call repaint() if needed
                }
            });
        }

        /**
         * To be called when associated keyboard height has changed.
         * <p>
         * Recompute internal data to make this yMapper up-to-date. Call revalidate() and repaint() when done.
         *
         * @param newKbdHeight
         */
        private synchronized void refresh(int newKbdHeight)
        {
            Preconditions.checkArgument(newKbdHeight > 0);
            LOGGER.log(Level.FINE, "NotesPanel.YMapper.refresh() -- newKbdHeight={0}  lastKeyboardHeight={1}", new Object[]
            {
                newKbdHeight, lastKeyboardHeight
            });

            if (newKbdHeight == lastKeyboardHeight)
            {
                return;
            }
            lastKeyboardHeight = newKbdHeight;


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
                float yUp = (pp == 0 || pp == 4 || pp == 5 || pp == 11 || p == kbdRange.getHighestPitch()) ? adjustedLargeHeight : adjustedSmallHeight;
                IntRange channelNoteYRange = new IntRange(Math.round(y - yUp + 1), yi);
                mapPitchChannelYRange.put(p, channelNoteYRange);
                y -= yUp;
            }

            revalidate();
            repaint();
        }

        /**
         * Check if yMapper is up to date with the latest keyboard height (ie refresh() was called).
         *
         * @return
         */
        public synchronized boolean isUptodate()
        {
            return lastKeyboardHeight == keyboard.getPreferredSize().height;
        }

        /**
         * The NoteView height to be used to fit the NoteView lines.
         *
         * @return
         * @throws IllegalStateException If this yMapper is not up-to-date.
         */
        public synchronized int getNoteViewHeight()
        {
            if (!isUptodate())
            {
                throw new IllegalStateException();
            }
            return noteViewHeight;
        }

        /**
         * Get the pitch corresponding to the specified yPos in this panel's coordinates.
         *
         * @param yPos If above/below the first/last pianokey, use the pitch of the first/last pianokey.
         * @return
         * @throws IllegalStateException If this yMapper is not up-to-date.
         */
        public synchronized int getPitch(int yPos)
        {
            if (!isUptodate())
            {
                throw new IllegalStateException();
            }

            var entry = tmapPixelPitch.ceilingEntry(yPos);
            // We might be below the (bottom key), can happen if keyboard is unzoomed/small and there is extra space below the keys.        
            int res = entry != null ? entry.getValue() : tmapPixelPitch.lastEntry().getValue();
            return res;
        }

        /**
         * Get the Y range of the NoteView channel for the specified pitch.
         * <p>
         *
         * @param pitch
         * @return
         * @throws IllegalStateException If this yMapper is not up-to-date.
         */
        public synchronized IntRange getNoteViewChannelYRange(int pitch)
        {
            if (!isUptodate())
            {
                throw new IllegalStateException();
            }
            var res = mapPitchChannelYRange.get(pitch);
            assert res != null : "pitch=" + pitch + " mapPitchChannelYRange=" + mapPitchChannelYRange;
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

    private class MyMouseListener implements MouseMotionListener, MouseListener
    {

        int lastPitch = -1;

        @Override
        public void mouseMoved(MouseEvent e)
        {
            if (!yMapper.isUptodate())
            {
                return;
            }

            int pitch = yMapper.getPitch(e.getY());
            if (pitch == lastPitch)
            {
                // Nothing
            } else if (pitch == -1)
            {
                keyboard.getKey(lastPitch).release();
            } else
            {
                if (lastPitch != -1)
                {
                    keyboard.getKey(lastPitch).release();
                }
                keyboard.getKey(pitch).setPressed(50, Color.LIGHT_GRAY);
            }
            lastPitch = pitch;
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            // 
        }


        @Override
        public void mouseClicked(MouseEvent e)
        {
            // 
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            // 
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            // 
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            // 
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (lastPitch != -1)
            {
                keyboard.getKey(lastPitch).release();
            }
            lastPitch = -1;
        }

    }

}
