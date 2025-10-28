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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.SwingUtilities;
import org.jjazz.harmony.api.Position;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.instrumentcomponents.keyboard.api.KeyboardComponent;
import org.jjazz.instrumentcomponents.keyboard.api.PianoKey;
import org.jjazz.uiutilities.api.HSLColor;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.Utilities;

/**
 * The main editor panel, shows the grid and holds the NoteViews.
 * <p>
 * Y metrics are taken from the associated vertical keyboard. The XMapper and YMapper objects provide the methods to position notes.
 * <p>
 * NotesPanel has an associated JLayer to draw over it.
 */
public class NotesPanel extends EditorPanel implements PropertyChangeListener
{

    private static final Color[] GHOST_NOTE_COLORS = new Color[]
    {
        new Color(112, 168, 151), new Color(93, 120, 20), new Color(212, 143, 106), new Color(173, 201, 100),
        new Color(14, 84, 63), new Color(58, 80, 0), new Color(128, 58, 21), new Color(67, 121, 131)
    };
    private Color nextGhostNoteColor = GHOST_NOTE_COLORS[0];
    private static final int GHOST_NOTE_ALPHA = 90;
    private static final int ONE_BEAT_SIZE_IN_PIXELS_AT_ZOOM_ONE = 50;

    private final KeyboardComponent keyboard;
    private final YMapper yMapper;
    private final XMapper xMapper;
    private final PianoRollEditor editor;
    private float scaleFactorX = 1f;
    private boolean scrollToFirstNoteHack = true;
    private final NotesPanelLayerUI layerUI;
    private final JLayer layer;
    private int playbackPointX = -1;
    private final TreeMap<NoteEvent, NoteView> mapNoteViews = new TreeMap<>();
    private Map<Integer, Phrase> mapChannelGhostPhrase;
    private final Map<Integer, Color> mapNameGhostNoteColor = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(NotesPanel.class.getSimpleName());

    public NotesPanel(PianoRollEditor editor, KeyboardComponent keyboard)
    {
        this.editor = editor;
        this.keyboard = keyboard;
        this.xMapper = new XMapper();
        this.yMapper = new YMapper();

        layerUI = new NotesPanelLayerUI();
        layer = new JLayer(this, layerUI);

        editor.getSettings().addPropertyChangeListener(this);
    }

    /**
     * Get the JLayer over this NotesPanel.
     *
     * @return
     */
    public JLayer getJLayer()
    {
        return layer;
    }

    /**
     * Early detection of size changes in order to update xMapper as soon as possible.
     * <p>
     * Overridden because this method is called (by parent's layoutManager) before component is painted and before the component resized/moved event
     * is fired. This lets us update xMapper as soon as possible.
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
        LOGGER.fine("doLayout() -- ");
        if (!xMapper.isUptodate() || !yMapper.isUptodate())
        {
            LOGGER.fine("   ==> doLayout() xMapper/yMapper is not up to date");
            return;
        }

        NoteView nv0 = null;

        if (!editor.isDrums())
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
            int side = yMapper.getNoteViewHeight() + 2;
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
                var yRange = yMapper.getNoteViewChannelYRange(ne.getPitch());
                int y = yRange.from;
                int w = side;
                int h = side;
                nv.setBounds(x, y, w, h);
                // LOGGER.severe("doLayout() side=" + side + " yRange=" + yRange + " bounds=" + nv.getBounds());
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
        int h = yMapper.getLastKeyboardHeight();
        int w = computePreferredWidth(scaleFactorX);
        var res = new Dimension(w, h);
        LOGGER.log(Level.FINE, "getPreferredSize() res={0}", res);
        return res;
    }

    /**
     * Set the X scale factor.
     * <p>
     * This methods impacts the preferred size then calls revalidate() (and repaint()). Hence the notesPanel size is NOT directly updated right after
     * exiting method. Size will be updated once the EDT has finished processing the revalidate.
     * <p>
     *
     * @param factorX A value &gt; 0
     */
    @Override
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

    /**
     * Get the current scale factor on the X axis.
     *
     * @return
     */
    @Override
    public float getScaleFactorX()
    {
        return scaleFactorX;
    }

    /**
     * Compute the scale factor so that pixelWidth represents beatRange.
     *
     * @param pixelWidth
     * @param beatRange
     * @return
     */
    public float computeScaleFactorX(int pixelWidth, float beatRange)
    {
        beatRange = Math.max(beatRange, 1f);
        float factor = pixelWidth / (beatRange * ONE_BEAT_SIZE_IN_PIXELS_AT_ZOOM_ONE);
        return factor;
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
        Graphics2D g2 = (Graphics2D) g;

//        LOGGER.log(Level.INFO, "paintComponent() -- clip={0}", new Object[]
//        {
//            g2.getClipBounds()
//        });

        // Make sure clip is restricted to a valid area
        g2.clipRect(0, 0, getWidth(), getHeight());
        var clip = g2.getClipBounds();

        if (!yMapper.isUptodate() || !xMapper.isUptodate() || clip.isEmpty())
        {
            //  Don't draw anything
            // LOGGER.severe("paintComponent() xMapper or yMapper is not uptodate, abort painting");
            return;
        }

        paintHorizontalGrid(g2);
        paintVerticalGrid(g2, yMapper.getKeyboardYRange(0).to, yMapper.getKeyboardYRange(127).from);
        paintGhostPhrases(g2);

    }

    public void showSelectionRectangle(Rectangle r)
    {
        // Use our JLayer to draw the rectangle over all other components
        var rOld = layerUI.getSelectionRectangle();
        layerUI.setSelectionRectangle(r);

        if (rOld == null && r == null)
        {
            return;
        }

        // Important optimization: use repaint(Rectangle) instead of repaint(): it repaints less but furthermore it avoids a surprising behavior, where 
        // each call to mouseDragLayer.repaint() triggers (via RepaintManager) a complete editor repaint including subcomponents such as JSplitePane and the 
        // Score panel, with a very bad impact on performance when many notes in the editor.        
        Rectangle rRepaint;
        rRepaint = getVisibleRect();

        // More optimized but... does not work! It leaves some garbage when dragging down then up (or right then left).
        // Take the union of the old and new rectangles
//                if (rOld != null)
//                {
//                    rRepaint = new Rectangle(rOld);
//                    if (r != null)
//                    {
//                        rRepaint.add(r);
//                    }
//                } else
//                {
//                    rRepaint = r;
//                }

        layer.repaint(rRepaint);       // This does work


//        LOGGER.log(Level.INFO, "showSelectionRectangle() r={0} rRepaint={1}", new Object[]
//        {
//            r, rRepaint
//        });
    }

    @Override
    public void showPlaybackPoint(int xPos)
    {
        // Use the JLayer to draw the line over other components
        layerUI.setPlaybackPoint(xPos);        // Represented by a single line

        int oldX = playbackPointX;
        playbackPointX = xPos;
        if (playbackPointX != oldX)
        {
            int x0, x1;
            if (oldX == -1)
            {
                x0 = xPos - 1;
                x1 = xPos + 1;
            } else if (playbackPointX == -1)
            {
                x0 = oldX - 1;
                x1 = oldX + 1;
            } else
            {
                x0 = Math.min(playbackPointX, oldX) - 1;
                x1 = Math.max(playbackPointX, oldX) + 1;
            }
            layer.repaint(x0, 0, x1 - x0 + 1, getHeight());
        }
    }

    /**
     * Create and add a NoteView for the specified NoteEvent.
     * <p>
     * Caller is responsible to call revalidate() and/or repaint() after, if required.
     *
     * @param ne
     * @return
     */
    @Override
    public NoteView addNoteView(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        Preconditions.checkArgument(editor.getPhraseBeatRange().contains(ne.getBeatRange(), false));

        var keymap = editor.getDrumKeyMap();
        NoteView nv = keymap == null ? new NoteView(ne) : new NoteViewDrum(ne);
        mapNoteViews.put(ne, nv);
        add(nv);
        LOGGER.log(Level.FINE, "addNoteView() ne={0} ==> mapNoteViews={1}", new Object[]
        {
            ne, mapNoteViews
        });
        return nv;
    }

    /**
     * Remove NoteView.
     * <p>
     * Caller must call revalidate() and/or repaint() after as needed.
     *
     * @param ne
     */
    @Override
    public void removeNoteView(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        NoteView nv = getNoteView(ne);  // Might be null in some corner cases ? See Issue #399
        if (nv != null)
        {
            remove(nv);
            nv.cleanup();
        }
        mapNoteViews.remove(ne);
        LOGGER.log(Level.FINE, "removeNoteView() ne={0} ==> mapNoteViews={1}", new Object[]
        {
            ne, mapNoteViews
        });
    }

    /**
     * Replace the model of an existing NoteView.
     * <p>
     * Caller is responsible of calling revalidate() and/or repaint() after as needed.
     *
     * @param oldNe
     * @param newNe
     */
    @Override
    public void setNoteViewModel(NoteEvent oldNe, NoteEvent newNe)
    {
        var nv = getNoteView(oldNe);
        assert nv != null : " oldNe=" + oldNe;
        nv.setModel(newNe);
        mapNoteViews.remove(oldNe);
        mapNoteViews.put(newNe, nv);
    }

    /**
     * Get the NoteView corresponding to ne.
     *
     * @param ne Must be a ne added via addNoteView(NoteEvent ne)
     * @return Can be null
     */
    @Override
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
    @Override
    public List<NoteView> getNoteViews()
    {
        return new ArrayList<>(mapNoteViews.values());
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

    /**
     * Set the ghost phrases.
     *
     * @param mapChannelPhrase Can be null if no ghost phrase
     */
    public void setGhostPhrases(Map<Integer, Phrase> mapChannelPhrase)
    {
        // LOGGER.log(Level.SEVERE, "setGhostPhrases() mapChannelPhrase={0}", Utilities.toMultilineString(mapChannelPhrase));
        mapChannelGhostPhrase = mapChannelPhrase;
        repaint();
    }

    /**
     * Can be null if not set.
     *
     * @return
     */
    public Map<Integer, Phrase> getGhostPhrases()
    {
        return mapChannelGhostPhrase;
    }

    @Override
    public void cleanup()
    {
        for (var nv : mapNoteViews.values().toArray(NoteView[]::new))
        {
            removeNoteView(nv.getModel());
        }
        editor.getSettings().removePropertyChangeListener(this);
    }

    /**
     * Get the NoteViews which intersect with the specified Rectangle, sorted by NoteEvent natural order.
     *
     * @param r
     * @return
     */
    public List<NoteView> getNoteViews(Rectangle r)
    {
        if (r.isEmpty())
        {
            return Collections.emptyList();
        }
        Collection<NoteView> nvs;
        if (xMapper.isUptodate())
        {
            // We can limit the search to some NoteViews, useful when many many notes
            SwingUtilities.computeIntersection(0, 0, getWidth(), getHeight(), r);
            float brFrom = xMapper.getBeatPosition(xMapper.getPositionFromX(r.x));
            float brTo = xMapper.getBeatPosition(xMapper.getPositionFromX(r.x + r.width - 1));
            NoteEvent neMin = new NoteEvent(0, 1, 0, Math.max(0, brFrom - 48f)); // we manage up to a 48 beats-long note that would cross our rectangle
            NoteEvent neMax = new NoteEvent(0, 1, 0, brTo + 0.1f);
            nvs = mapNoteViews.subMap(neMin, neMax).values();       // ordered by note position
        } else
        {
            // Check all NoteViews
            nvs = mapNoteViews.values();
        }

        var res = nvs.stream()
            .filter(nv -> nv.getBounds().intersects(r))
            .toList();

        return res;
    }

    /**
     * Draw the vertical lines for each bar/beat.
     *
     * @param g2 The clip horizontal range must be within the NotesPanel horizontal bounds.
     * @param y0 start of the vertical lines
     * @param y1 end of the vertical lines
     */
    public void paintVerticalGrid(Graphics2D g2, int y0, int y1)
    {
        var settings = PianoRollEditorSettings.getDefault();
        if (settings == null)
        {
            return;
        }

        Color cl1 = settings.getBarLineColor();
        Color cl2 = HSLColor.changeLuminance(cl1, 9);      // lighter color
        Color cl2_lz = HSLColor.changeLuminance(cl1, 4);      // not so lighter in loop zone
        Color cl3 = HSLColor.changeLuminance(cl2, 3);      // even lighter
        Color cl3_lz = HSLColor.changeLuminance(cl2_lz, 3);

        var clipBarRange = xMapper.getBarRange(IntRange.ofX(g2.getClipBounds()));       // Clip must NOT be within the NotesPanel horizontal bounds
        if (clipBarRange.isEmpty())
        {
            return;
        }
        var mapQPosX = xMapper.getQuantizedXPositions(clipBarRange); // Get only positions within the clip
        boolean paintSixteenth = xMapper.getOneBeatPixelSize() > 20;
        IntRange loopZone = editor.getLoopZone();

        for (Position pos : mapQPosX.navigableKeySet())
        {
            if (!paintSixteenth && pos.isOffBeat())
            {
                continue;
            }
            Integer xI = mapQPosX.get(pos);
            assert xI != null : "pos=" + pos + " mapQPosX=" + Utilities.toMultilineString(mapQPosX);
            int x = xI;
            Color c = cl1;
            if (!pos.isFirstBarBeat())
            {
                c = pos.isOffBeat() ? cl3 : cl2;
                if (loopZone != null && loopZone.contains(pos.getBar()))
                {
                    c = (c == cl3) ? cl3_lz : cl2_lz;
                }
            }

            g2.setColor(c);
            g2.drawLine(x, y0, x, y1);
        }
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

        }
    }

    // ========================================================================================
    // Private methods
    // ========================================================================================

    private void paintHorizontalGrid(Graphics2D g2)
    {
        var settings = PianoRollEditorSettings.getDefault();
        if (settings == null)
        {
            return;
        }
        var clip = g2.getClipBounds();
        int w = getWidth();
        

        Color blackKeyLaneColor = settings.getBlackKeyLaneBackgroundColor();
        Color whiteKeyLaneColor = settings.getWhiteKeyLaneBackgroundColor();
        Color cB_C = settings.getBarLineColor();
        Color cE_F = HSLColor.changeLuminance(cB_C, 8);



        // prepare LoopZone data
        IntRange loopZone = editor.getLoopZone();
        IntRange loopZoneXRange = loopZone == null ? null : xMapper.getXRange(loopZone);
        Color blackKeyLaneColorLoopZone = settings.getLoopZoneBlackKeyLaneBackgroundColor();
        Color whiteKeyLaneColorLoopZone = settings.getLoopZoneWhiteKeyLaneBackgroundColor();


        // only draw what's visible
        IntRange pitchRange = editor.getVisiblePitchRange();


        // Draw a rectangle per pitch
        for (int p = pitchRange.from; p <= pitchRange.to; p++)
        {
            int pp = p % 12;
            Color c = (pp == 0 || pp == 2 || pp == 4 || pp == 5 || pp == 7 || pp == 9 || pp == 11) ? whiteKeyLaneColor : blackKeyLaneColor;
            g2.setColor(c);
            var yRange = yMapper.getNoteViewChannelYRange(p);
            var rPitch = new Rectangle(0, yRange.from, w, yRange.size());
            rPitch = rPitch.intersection(clip);
            g2.fillRect(rPitch.x, rPitch.y, rPitch.width, rPitch.height);


            // Draw loop zone if any
            if (loopZoneXRange != null)
            {
                Color cLz = (c == blackKeyLaneColor) ? blackKeyLaneColorLoopZone : whiteKeyLaneColorLoopZone;
                g2.setColor(cLz);
                var rZone = new Rectangle(loopZoneXRange.from, yRange.from, loopZoneXRange.size(), yRange.size());
                rZone = rZone.intersection(clip);
                g2.fillRect(rZone.x, rZone.y, rZone.width, rZone.height);
            }


            // Draw lines between B and C and lighter ones between E and F       
            if (pp == 0 || pp == 5)
            {
                g2.setColor(pp == 0 ? cB_C : cE_F);
                g2.drawLine(clip.x, yRange.to, clip.x + clip.width - 1, yRange.to);
            }
        }

    }

    private void paintGhostPhrases(Graphics2D g2)
    {
        if (mapChannelGhostPhrase == null)
        {
            return;
        }

        var clipBr = xMapper.getBeatPositionRange(IntRange.ofX(g2.getClipBounds()));

        for (var channel : mapChannelGhostPhrase.keySet())
        {
            Phrase p = mapChannelGhostPhrase.get(channel);
            Color c = getGhostNoteColor(channel);
            Color c1 = new Color(c.getRed(), c.getGreen(), c.getBlue(), GHOST_NOTE_ALPHA);
            Color c2 = HSLColor.changeLuminance(c1, -23);

            for (var ne : p)
            {
                var neBr = ne.getBeatRange();
                if (editor.getPhraseBeatRange().contains(neBr, false) && clipBr.intersects(neBr))
                {
                    int y = yMapper.getNoteViewChannelYRange(ne.getPitch()).from;
                    int h = yMapper.getNoteViewHeight();

                    if (!p.isDrums())
                    {
                        IntRange xRange = xMapper.getXRange(ne.getBeatRange());
                        g2.setColor(c1);
                        g2.fillRect(xRange.from, y, xRange.size(), h);
                        g2.setColor(c2);
                        g2.drawRect(xRange.from, y, xRange.size(), h);
                    } else
                    {
                        int x = xMapper.getX(ne.getPositionInBeats());
                        int half = h / 2 + 1;
                        y += half;
                        int x0 = x - half;
                        int x1 = x + half;
                        int y0 = y - half;
                        int y1 = y + half;
                        Polygon polygon = new Polygon();
                        polygon.addPoint(x0, y);
                        polygon.addPoint(x, y1);
                        polygon.addPoint(x1, y);
                        polygon.addPoint(x, y0);
                        g2.setColor(c1);
                        g2.fill(polygon);
                        g2.setColor(c2);
                        g2.draw(polygon);
                    }
                }
            }
        }
    }

    private void settingsChanged()
    {
        repaint();
    }

    private Color getGhostNoteColor(int channel)
    {
        Color c = mapNameGhostNoteColor.get(channel);
        if (c == null)
        {
            c = nextGhostNoteColor;
            mapNameGhostNoteColor.put(channel, c);
            for (int i = 0; i < GHOST_NOTE_COLORS.length; i++)
            {
                if (GHOST_NOTE_COLORS[i] == c)
                {
                    nextGhostNoteColor = GHOST_NOTE_COLORS[(i + 1) % GHOST_NOTE_COLORS.length];
                    break;
                }
            }
        }
        return c;
    }

    private int computePreferredWidth(float scaleVFactor)
    {
        return (int) (editor.getPhraseBeatRange().size() * ONE_BEAT_SIZE_IN_PIXELS_AT_ZOOM_ONE * scaleVFactor);
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
            var beatRange = editor.getPhraseBeatRange();
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
            int bar = 0;
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


            // LOGGER.log(Level.SEVERE, "refresh() output tmap_allQuantizedPos2X=" + Utilities.toMultilineString(tmap_allQuantizedPos2X));
        }

        /**
         * Get the X coordinate of each quantized position in the specified phrase barRange.
         *
         * @param barRange If null, use this.getPhraseBarRange() instead.
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
                barRange = editor.getPhraseBarRange();
            }
            var res = tmap_allQuantizedPos2X.subMap(new Position(barRange.from), true, new Position(barRange.to + 1), false);
            return res;
        }

        /**
         * Get the X coordinate of each integer beat in the specified phrase barRange.
         *
         * @param barRange If null, use this.getPhraseBarRange() instead.
         * @return
         * @throws IllegalStateException If this yMapper is not up-to-date.
         */
        public synchronized NavigableMap<Position, Integer> getAllBeatsX(IntRange barRange)
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
                barRange = editor.getPhraseBarRange();
            }
            var res = tmap_allIntPos2X.subMap(new Position(barRange.from), true, new Position(barRange.to + 1), false);
            return res;
        }

        /**
         * Get the size in pixels of 1 beat.
         *
         * @return
         */
        public float getOneBeatPixelSize()
        {
            return getWidth() / editor.getPhraseBeatRange().size();
        }

        /**
         * Get the size in beats of 1 pixel.
         *
         * @return
         */
        public float getOnePixelBeatSize()
        {
            int w = getWidth();
            return w == 0 ? Float.MIN_VALUE : editor.getPhraseBeatRange().size() / getWidth();
        }

        /**
         * Get the beat position corresponding to the specified xPos in this panel's coordinates.
         *
         * @param xPos
         * @return A beat position within getPhraseBeatRange(). -1 if xPos is out of the bounds of this panel.
         */
        public float getBeatPosition(int xPos)
        {
            if (xPos < 0 || xPos >= getWidth())
            {
                return -1;
            }
            float relPos = xPos / getOneBeatPixelSize();
            float absPos = editor.getPhraseBeatRange().from + relPos;
            return absPos;
        }

        /**
         * Get the x positions of the first and last pixel of a bar range.
         *
         * @param barRange
         * @return Can be empty if barRange is empty
         */
        public IntRange getXRange(IntRange barRange)
        {
            Objects.requireNonNull(barRange);
            var res = IntRange.EMPTY_RANGE;
            if (!barRange.isEmpty())
            {
                int xFrom = getX(new Position(barRange.from));
                int xTo = editor.getPhraseBarRange().contains(barRange.to + 1) ? getX(new Position(barRange.to + 1)) - 1 : getLastWidth() - 1;
                res = new IntRange(xFrom, xTo);
            }
            return res;
        }

        /**
         * Get the bar range corresponding to the x position range.
         *
         * @param xRange
         * @return Can be empty if xRange is empty
         */
        public IntRange getBarRange(IntRange xRange)
        {
            Objects.requireNonNull(xRange);
            Preconditions.checkArgument(xRange.isEmpty() || xRange.to < getWidth(), "xRange=%s width=", xRange, getWidth());

            var res = IntRange.EMPTY_RANGE;
            if (!xRange.isEmpty())
            {
                int barFrom = getPositionFromX(xRange.from).getBar();
                int barTo = getPositionFromX(xRange.to).getBar();
                res = new IntRange(barFrom, barTo);
            }
            return res;
        }

        /**
         * Get the beat position range corresponding to the specified xPosRange in this panel's coordinates.
         *
         * @param xPosRange
         * @return A beat position range within getPhraseBeatRange(). Can be an empty range if xPosRange is empty or not contained in this panel.
         */
        public FloatRange getBeatPositionRange(IntRange xPosRange)
        {
            Objects.requireNonNull(xPosRange);
            var res = FloatRange.EMPTY_FLOAT_RANGE;
            if (!xPosRange.isEmpty())
            {
                float posInBeatsFrom = getBeatPosition(xPosRange.from);
                float posInBeatsTo = getBeatPosition(xPosRange.to);
                if (posInBeatsFrom >= 0 && posInBeatsTo >= 0)
                {
                    res = posInBeatsFrom == posInBeatsTo ? new FloatRange(posInBeatsFrom, posInBeatsFrom + getOnePixelBeatSize() - 0.01f)
                        : new FloatRange(posInBeatsFrom, posInBeatsTo);
                }
            }
            return res;
        }

        /**
         * Get the position (bar, beat) corresponding to the specified xPos in this panel's coordinates.
         *
         * @param xPos
         * @return Null if xPos is out of range
         */
        public Position getPositionFromX(int xPos)
        {
            var posInBeats = XMapper.this.getBeatPosition(xPos);
            return posInBeats < 0 ? null : getPosition(posInBeats);
        }

        /**
         * Get the X position (in this panel's coordinates) corresponding to the specified phrase beat position.
         *
         * @param posInBeats A beat position within getPhraseBeatRange()
         * @return
         */
        public int getX(float posInBeats)
        {
            var br = editor.getPhraseBeatRange();
            Preconditions.checkArgument(br.contains(posInBeats, false), "br=%s posInBeats=%s", br, posInBeats);
            int w = getWidth();
            int xPos = Math.round((posInBeats - br.from) * getOneBeatPixelSize());
            xPos = Math.min(w - 1, xPos);
            return xPos;
        }

        /**
         * Get the X start/end positions of the specified beatPosition range.
         *
         * @param fr
         * @return
         */
        public IntRange getXRange(FloatRange fr)
        {
            return new IntRange(getX(fr.from), getX(fr.to));
        }

        /**
         *
         * Get the X position (in this panel's coordinates) corresponding to the specified phrase position in bar/beat.
         *
         * @param pos
         * @return
         */
        public int getX(Position pos)
        {
            Objects.requireNonNull(pos);
            var xPos = getX(XMapper.this.getBeatPosition(pos));
            return xPos;
        }

        /**
         * Convert a phrase position in beats into a bar/beat phrase position.
         *
         * @param posInBeats
         * @return
         */
        public Position getPosition(float posInBeats)
        {
            var br = editor.getPhraseBeatRange();
            Preconditions.checkArgument(br.contains(posInBeats, true), "br=%s posInBeats=%s", br, posInBeats);
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
        public float getBeatPosition(Position pos)
        {
            Objects.requireNonNull(pos);
            Preconditions.checkArgument(editor.getPhraseBarRange().contains(pos.getBar()), "pos=%s getBarRange()=%s", pos, editor.getPhraseBarRange());
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
         * To be called when associated keyboard height has changed.
         * <p>
         * Recomputes internal data to make this yMapper up-to-date. Calls revalidate() and repaint() when done.
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


            noteViewHeight = (int) Math.round(adjustedSmallHeight);


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
                float yUp = (pp == 0 || pp == 4 || pp == 5 || pp == 11 || p == kbdRange.getHighestPitch())
                    ? adjustedLargeHeight
                    : adjustedSmallHeight;
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
            // Bug fix : drums notes are not resized when zoom in vertically
            // Use keyboard's preferred height instead of height, because PianoRollEditor setZoom() method directly calls yMapper.refresh(), which
            // in turn calls revalidate(), so our doLayout() (which needs isUptodate() to return true) can be called before the keyboard bounds are actually changed.
            return lastKeyboardHeight == keyboard.getPreferredSize().height;
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
         * Get the pitch range corresponding to the y-position range.
         *
         * @param yRange
         * @return
         */
        public IntRange getPitchRange(IntRange yRange)
        {
            int pTo = getPitch(yRange.from);
            int pFrom = getPitch(yRange.to);
            var res = new IntRange(pFrom, pTo);
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
