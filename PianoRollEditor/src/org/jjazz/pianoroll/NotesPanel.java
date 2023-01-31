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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent;
import org.jjazz.ui.keyboardcomponent.api.PianoKey;
import org.jjazz.ui.utilities.api.HSLColor;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.Utilities;

/**
 * The main editor panel, shows the grid and holds the NoteViews.
 * <p>
 * Y metrics are taken from the associated vertical keyboard. The XMapper and YMapper objects provide the methods to position
 * notes.
 */
public class NotesPanel extends javax.swing.JPanel implements PropertyChangeListener
{

    private final KeyboardComponent keyboard;
    private final YMapper yMapper;
    private final XMapper xMapper;
    private final PianoRollEditor editor;
    private float scaleFactorX = 1f;
    private boolean scrollToFirstNoteHack = true;
    private final TreeMap<NoteEvent, NoteView> mapNoteViews = new TreeMap<>();

    private static final Logger LOGGER = Logger.getLogger(NotesPanel.class.getSimpleName());


    public NotesPanel(PianoRollEditor editor, KeyboardComponent keyboard)
    {
        this.editor = editor;
        this.keyboard = keyboard;
        this.xMapper = new XMapper();
        this.yMapper = new YMapper();

        editor.getSettings().addPropertyChangeListener(this);

    }

    /**
     * Early detection of size changes in order update xMapper as soon as possible.
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
        if (!xMapper.isUptodate())
        {
            xMapper.refresh();
        }
    }

    /**
     * Layout all the NoteEvents.
     */
    @Override
    public void doLayout()
    {
        if (!xMapper.isUptodate() || !yMapper.isUptodate())
        {
            return;
        }

        NoteView nv0 = null;

        if (!editor.isDrumEdit())
        {
            for (NoteView nv : mapNoteViews.values())
            {
                if (nv0 == null)
                {
                    nv0 = nv;
                }
                NoteEvent ne = nv.getModel();
                FloatRange br = ne.getBeatRange();
                int x = xMapper.getX(br.from);
                int y = yMapper.getNoteViewChannelYRange(ne.getPitch()).from;
                int w = xMapper.getX(br.to - 0.0001f) - x + 1;
                int h = yMapper.getNoteViewHeight();
                nv.setBounds(x, y, w, h);
            }
        } else
        {
            // Drum notes
            int side = yMapper.getNoteViewHeight();
            if (side % 2 == 0)
            {
                side++;     // Need an odd value so that NoteView will be perfectly centered
            }
            for (NoteView nv : mapNoteViews.values())
            {
                if (nv0 == null)
                {
                    nv0 = nv;
                }
                NoteEvent ne = nv.getModel();
                int x = xMapper.getX(ne.getPositionInBeats()) - side / 2;
                int y = yMapper.getNoteViewChannelYRange(ne.getPitch()).from;
                int w = side;
                int h = side;
                nv.setBounds(x, y, w, h);
            }
        }

        if (scrollToFirstNoteHack)
        {
            // Hack needed because a simple SwingUtilities.invokeLater(scrollToFirstNote()) is not enough, we must make sure that all NoteViews are placed            
            scrollToFirstNoteHack = false;


            // Adjust the enclosing scrollPane so that the first note is visible. If no note, show middle pitch.
            Rectangle r;
            final int SIZE = 400;
            if (nv0 == null)
            {
                // Left position, middle pitch
                r = new Rectangle(0, getHeight() / 2 - SIZE / 2, SIZE, SIZE);
            } else
            {
                // Center around the first note
                r = nv0.getBounds();
                r.x = Math.max(0, r.x - SIZE / 2);
                r.y = Math.max(0, r.y - SIZE / 2);
                r.height = SIZE;
                r.width = SIZE;
            }
            SwingUtilities.invokeLater(() -> scrollRectToVisible(r));
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
     * Change the X scale factor.
     * <p>
     * This methods impacts the preferred size then calls revalidate() (and repaint()). Hence the notesPanel size is NOT directly
     * updated right after exiting method. Size will be updated once the EDT has finished processing the revalidate.
     * <p>
     * This will
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

    /**
     * Create and add a NoteView for the specified NoteEvent.
     * <p>
     * Caller must call revalidate() and/or repaint() after as needed.
     *
     * @param ne
     */
    public NoteView addNoteView(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        Preconditions.checkArgument(editor.getBeatRange().contains(ne.getBeatRange(), false));

        var keymap = editor.getDrumKeyMap();
        NoteView nv = keymap == null ? new NoteView(ne) : new NoteViewDrum(ne);
        mapNoteViews.put(ne, nv);
        add(nv);
        nv.addPropertyChangeListener(this);
        LOGGER.log(Level.FINE, "addNoteView() ne={0} ==> mapNoteViews={1}", new Object[]
        {
            ne, mapNoteViews
        });
        return nv;
    }

    /**
     * Remove a NoteView for the specified NoteEvent.
     * <p>
     * NoteView is unselected before being removed. Caller must call revalidate() and/or repaint() after as needed.
     *
     * @param ne
     */
    public NoteView removeNoteView(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        NoteView nv = getNoteView(ne);
        nv.setSelected(false);
        mapNoteViews.remove(ne);
        remove(nv);
        nv.removePropertyChangeListener(this);
        nv.cleanup();
        LOGGER.log(Level.FINE, "removeNoteView() ne={0} ==> mapNoteViews={1}", new Object[]
        {
            ne, mapNoteViews
        });
        return nv;
    }


    /**
     * Get the NoteView corresponding to ne.
     *
     * @param ne Must be a ne added via addNoteView(NoteEvent ne)
     * @return Can be null
     */
    public NoteView getNoteView(NoteEvent ne)
    {
        var res = mapNoteViews.get(ne);
        return res;
    }

    /**
     * The NoteViews sorted by NoteEvent natural order.
     *
     * @return
     */
    public List<NoteView> getNoteViews()
    {
        return new ArrayList<>(mapNoteViews.values());
    }

    /**
     * The NoteViews which belong (whole or partly) to the specified Rectangle, sorted by NoteEvent natural order.
     *
     * @param r
     * @return
     */
    public List<NoteView> getNoteViews(Rectangle r)
    {
        var res = mapNoteViews.values()
                .stream()
                .filter(nv -> nv.getBounds().intersects(r))
                .toList();
        return res;
    }


    /**
     * Adjust the enclosing scrollPane so that the first note is visible.
     * <p>
     * If no note, show middle pitch.
     */
    public void scrollToFirstNote()
    {
        scrollToFirstNoteHack = true;
        revalidate();
    }

    public void cleanup()
    {
        for (var nv : mapNoteViews.values().toArray(NoteView[]::new))
        {
            removeNoteView(nv.getModel());
        }
        editor.getSettings().removePropertyChangeListener(this);
    }


    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.log(Level.FINE, "propertyChange() evt.source.class={0}prop={1} old={2} new={3}", new Object[]
        {
            evt.getSource().getClass().getSimpleName(), evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()
        });

        if (evt.getSource() == editor.getSettings())
        {
            settingsChanged();

        } else if (evt.getSource() instanceof NoteView nv)
        {
            if (evt.getPropertyName().equals(NoteView.PROP_MODEL))
            {
                NoteEvent oldNe = (NoteEvent) evt.getOldValue();
                // nv's model was changed, remove and readd
                mapNoteViews.remove(oldNe);
                mapNoteViews.put(nv.getModel(), nv);
                revalidate();
                repaint();

            }
        }
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
        var barRange = editor.getVisibleBarRange();
        var mapQPosX = xMapper.getQuantizedXPositions(barRange);              // only draw what's visible        

        for (Position pos : mapQPosX.navigableKeySet())
        {
            Integer xI = mapQPosX.get(pos);
            assert xI != null : "pos=" + pos + " mapQPosX=" + Utilities.toMultilineString(mapQPosX);
            int x = xI;
            Color c = cl1;
            if (!pos.isFirstBarBeat())
            {
                c = pos.isOffBeat() ? cl3 : cl2;
            }

            g2.setColor(c);
            g2.drawLine(x, y0, x, y1);
        }
    }


    private void settingsChanged()
    {
        repaint();
    }


    // =====================================================================================
    // Inner classes
    // =====================================================================================
    /**
     * Conversion methods between NoteView X coordinate and position in beats.
     */
    public class XMapper
    {

        private int lastWidth = -1;
        /**
         * Map all quantized Positions to a X coordinate.
         */
        private final NavigableMap<Position, Integer> tmap_allQuantizedPos2X = new TreeMap<>();
        /**
         * Map all quantized positions-in-beat to a X coordinate.
         */
        private final NavigableMap<Position, Integer> tmap_allIntPos2X = new TreeMap<>();
        private BiMap<Position, Float> bimap_pos_posInBeats;
        private IntRange barRange = IntRange.EMPTY_RANGE;


        private XMapper()
        {
            editor.addPropertyChangeListener(PianoRollEditor.PROP_QUANTIZATION, e ->
            {
                refresh();
                repaint();
            });
        }

        public boolean isUptodate()
        {
            return lastWidth == getWidth();
        }

        /**
         * Get the bar range of the current context.
         *
         * @return
         */
        public IntRange getBarRange()
        {
            return barRange;
        }

        /**
         * Get the last notesPanel width used to refresh this XMapper.
         *
         * @return
         */
        public int getLastWidth()
        {
            return lastWidth;
        }


        /**
         * To be called when this panel width or model has changed.
         * <p>
         * Recompute internal data to make this xMapper up-to-date.
         * <p>
         */
        public synchronized void refresh()
        {
            LOGGER.log(Level.FINE, "XMapper.refresh() lastWidth={0} getWidth()={1}", new Object[]
            {
                lastWidth, getWidth()
            });

            lastWidth = getWidth();
            var beatRange = editor.getBeatRange();
            assert beatRange.from == (int) beatRange.from;      // it's an int value         

            tmap_allQuantizedPos2X.clear();
            tmap_allIntPos2X.clear();
            bimap_pos_posInBeats = HashBiMap.create((int) beatRange.size());


            var q = editor.getQuantization();
            float[] qBeats = q.getBeats();
            float qUnit = q.getSymbolicDuration().getDuration();    // ]0;1]
            float oneBeatWidth = getOneBeatPixelSize();
            float oneQuantizationUnitWidth = oneBeatWidth * qUnit;


            // Precompute all positions and relative X coordinate
            int bar = editor.getStartBarIndex();
            float barPosInBeats = beatRange.from;
            float x = 0;


            do
            {
                var ts = editor.getTimeSignature(barPosInBeats);
                assert ts.getNbNaturalBeats() == (int) ts.getNbNaturalBeats();

                for (int beat = 0; beat < ts.getNbNaturalBeats(); beat++)
                {
                    for (float qBeat : qBeats)
                    {
                        if (qBeat == 1)
                        {
                            continue;
                        }

                        var pos = new Position(bar, beat + qBeat);
                        tmap_allQuantizedPos2X.put(pos, Math.round(x));
                        if (qBeat == 0)
                        {
                            tmap_allIntPos2X.put(pos, Math.round(x));
                            bimap_pos_posInBeats.put(pos, barPosInBeats + beat);
                        }

                        x += oneQuantizationUnitWidth;
                    }
                }

                barPosInBeats += ts.getNbNaturalBeats();
                bar++;

            } while (barPosInBeats <= beatRange.to);


            barRange = new IntRange(editor.getStartBarIndex(), bar - 2);

            // LOGGER.log(Level.SEVERE, "refresh() output barRange=" + barRange + " tmap_allQuantizedPos2X=" + Utilities.toMultilineString(tmap_allQuantizedPos2X));

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
            var res = tmap_allQuantizedPos2X.subMap(new Position(barRange.from, 0), true, new Position(barRange.to + 1, 0), false);
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
            var res = tmap_allIntPos2X.subMap(new Position(barRange.from, 0), true, new Position(barRange.to + 1, 0), false);
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
         * @return A beat position within getBeatRange(). -1 if xPos is out of the bounds of this panel.
         */
        public float getPositionInBeats(int xPos)
        {
            if (xPos < 0 && xPos >= getWidth())
            {
                return -1;
            }
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
            Preconditions.checkArgument(br.contains(posInBeats, true), "br=%s posInBeats=%s", br, posInBeats);
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
            float posInBeatsFloor = (float) Math.floor(posInBeats);
            Position pos = new Position(bimap_pos_posInBeats.inverse().get(posInBeatsFloor));
            pos.setBeat(pos.getBeat() + posInBeats - posInBeatsFloor);
            return pos;
        }

        /**
         * Convert a position in bar/beat into a position in beats.
         *
         * @param pos Must belong to the editor bar range
         * @return
         */
        public float toPositionInBeats(Position pos)
        {
            Preconditions.checkArgument(barRange.contains(pos.getBar()), "pos=%s getBarRange()=%s", pos, getBarRange());
            float posBeatFloor = (float) Math.floor(pos.getBeat());
            Position posFloor = new Position(pos.getBar(), posBeatFloor);
            float posInBeats = bimap_pos_posInBeats.get(posFloor);
            posInBeats += pos.getBeat() - posBeatFloor;
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
         * To be called when associated keyboard height has changed (
         * <p>
         * Recompute internal data to make this yMapper up-to-date. Call revalidate() and repaint() when done.
         *
         * @param newKbdHeight
         */
        public synchronized void refresh(int newKbdHeight)
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
            return lastKeyboardHeight == keyboard.getHeight();
        }

        /**
         * Get the last Keyboard height used to refresh this YMapper.
         *
         * @return
         */
        public int getLastKeyboardHeight()
        {
            return lastKeyboardHeight;
        }

        /**
         * The NoteView height to be used so that it fits all NoteView lines.
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
                throw new IllegalStateException("lastKeyboardHeight=" + lastKeyboardHeight + " keyboard.height=" + keyboard.getHeight());
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


}
