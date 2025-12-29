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
package org.jjazz.improvisionsupport;

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.improvisionsupport.PlayRestScenario.DenseSparseValue;
import org.jjazz.improvisionsupport.PlayRestScenario.PlayRestValue;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.spi.BarRendererSettings;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.uiutilities.api.HSLColor;
import org.jjazz.uiutilities.api.StringMetrics;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.utilities.api.ResUtil;

/**
 * A BarRenderer to show improvisation support info.
 */
public class BR_ImproSupport extends BarRenderer implements ChangeListener
{

    private static final Color PLAY_BACKGROUND_COLOR = new Color(88, 189, 235);
    private static final Color PLAY_DENSE_BACKGROUND_COLOR = HSLColor.changeLuminance(PLAY_BACKGROUND_COLOR, -7);
    private static final Color PLAY_SPARSE_BACKGROUND_COLOR = HSLColor.changeLuminance(PLAY_BACKGROUND_COLOR, +7);
    private static final Font PLAY_FONT = GeneralUISettings.getInstance().getStdCondensedFont();
    private static final Color FONT_COLOR = Color.DARK_GRAY.darker();
    private static final int PREF_HEIGHT = 15;
    private static final String PLAY_STRING = ResUtil.getString(BR_ImproSupport.class, "Play");
    private static final String PLAY_DENSE_STRING = ResUtil.getString(BR_ImproSupport.class, "PlayDense");
    private static final String PLAY_SPARSE_STRING = ResUtil.getString(BR_ImproSupport.class, "PlaySparse");
    private static final String REST_STRING = ResUtil.getString(BR_ImproSupport.class, "Rest");
    private static Rectangle2D PLAY_STRING_BOUNDS;
    private static Rectangle2D PLAY_SPARSE_STRING_BOUNDS;
    private static Rectangle2D PLAY_DENSE_STRING_BOUNDS;
    private static Rectangle2D REST_STRING_BOUNDS;


    private PlayRestScenario scenario;
    private boolean playbackPointEnabled;
    private Position playbackPosition;
    private int songBarIndex;
    private int saveModelBarIndex;

    private int zoomVFactor = 50;

    /**
     * Maintain an ordered list of BR_ImproSupport instances per song.
     * <p>
     */
    private static final Map<Integer, List<BR_ImproSupport>> mapEditorBrs = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(BR_ImproSupport.class.getSimpleName());

    /**
     * Create an instance and keep its reference in mapEditorBrs.
     *
     * @param editor
     * @param barIndex
     * @param settings
     * @param irf
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public BR_ImproSupport(CL_Editor editor, int barIndex, BarRendererSettings settings, ItemRendererFactory irf)
    {
        super(editor, barIndex, settings, irf);
        setPreferredSize(new Dimension(10, PREF_HEIGHT));      // Width ignored because of the containers layout

        playbackPointEnabled = false;
        songBarIndex = -1;
        saveModelBarIndex = -2;


        // Update the list of instances ordered by bar index
        var brs = mapEditorBrs.get(getSongKey());
        if (brs == null)
        {
            brs = new ArrayList<>();
            mapEditorBrs.put(getSongKey(), brs);
        }
        int index = Collections.binarySearch(brs, this, (br1, br2) -> Integer.compare(br1.getBarIndex(), br2.getBarIndex()));
        if (index >= 0)
        {
            throw new IllegalStateException("this=" + this + " brs=" + brs);
        }
        index = -(index + 1);
        brs.add(index, this);

    }


    public void setScenario(PlayRestScenario scenario)
    {
        if (this.scenario == scenario)
        {
            return;
        }
        this.scenario = scenario;
        repaint();
    }

    /**
     * Overridden to know if leadsheet model has changed.
     *
     * @param barIndex
     */
    @Override
    public int setModelBarIndex(int barIndex)
    {
        int res = super.setModelBarIndex(barIndex);

        if (barIndex != saveModelBarIndex)
        {
            // LOGGER.log(Level.INFO, "setModelBarIndex() -- barIndex=" + barIndex + "  saveModelBarIndex=" + saveModelBarIndex);
            setSongBarIndex(-1);
        }
        return res;
    }

    /**
     * The barIndex in the whole song.
     *
     * @return
     */
    public int getSongBarIndex()
    {
        return songBarIndex;
    }

    /**
     * Set the barIndex in the whole song.
     *
     * @param songBarIndex -1 means no data available
     */
    public void setSongBarIndex(int songBarIndex)
    {
        // LOGGER.log(Level.INFO, "setSongBarIndex() -- songBarIndex=" + songBarIndex + " oldSongBarIndex=" + this.songBarIndex);
        if (this.songBarIndex != songBarIndex)
        {
            this.songBarIndex = songBarIndex;
            this.saveModelBarIndex = getModelBarIndex();
            repaint();
        }
    }

    public boolean isPlaybackPointEnabled()
    {
        return playbackPointEnabled;
    }

    public void setPlaybackPointEnabled(boolean playbackPointEnabled)
    {
        this.playbackPointEnabled = playbackPointEnabled;
        repaint();
    }

    /**
     * Overridden to remove the instance from mapEditorBrs.
     */
    @Override
    public void cleanup()
    {
        super.cleanup();
        var brs = mapEditorBrs.get(getSongKey());
        brs.remove(this);
        songBarIndex = -1;
    }


    /**
     * Overridden to draw a line corresponding to the section's color.
     *
     * @param g
     */
    @Override
    public void paintComponent(Graphics g)
    {
//        if (getBarIndex() == 0)
//        {
//            LOGGER.info("paintComponent() barIndex=" + getBarIndex() + "  songBarIndex=" + songBarIndex);
//        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(PLAY_FONT);

        Rectangle r = getDrawingArea();
        final float w = r.width;
        final float h = r.height;
        final float x0 = r.x;
        final float y0 = r.y;


        // Do nothing if model not ready yet or no model data available
        var sgs = getEditor().getSongModel().getSongStructure();
        if (scenario == null
                || songBarIndex == -1
                || songBarIndex >= sgs.getSizeInBars()
                || !scenario.isPlayRestDataAvailable(songBarIndex))
        {
            drawTopBottomLines(g2);
            return;
        }

        SongPart spt = sgs.getSongPart(songBarIndex);
        TimeSignature ts = spt.getRhythm().getTimeSignature();
        final float wBeat = (float) Math.ceil(w / ts.getNbNaturalBeats());


        List<PlayRestValue> playRestValues = scenario.getPlayRestValues(songBarIndex);
        List<DenseSparseValue> denseSparseValues = scenario.getDenseSparseValues(songBarIndex);


        if (getBarIndex() == 0)
        {
            // LOGGER.info("paintComponent() bar=0 songBarIndex=" + songBarIndex + " playRestValues=" + playRestValues);
        }


        // Get last values of previous bar
        PlayRestValue prevBarPlayRestValue = null;
        DenseSparseValue prevBarDenseSparseValue = null;
        if (songBarIndex > 0)
        {
            var prevPRvalues = scenario.getPlayRestValues(songBarIndex - 1);
            prevBarPlayRestValue = prevPRvalues.get(1);
            if (scenario.isDenseSparseDataAvailable(songBarIndex - 1))
            {
                var prevDSvalues = scenario.getDenseSparseValues(songBarIndex - 1);
                prevBarDenseSparseValue = prevDSvalues.get(1);
            }
        }

        // Get the first values of next bar
        PlayRestValue nextBarPlayRestValue = null;
        if (songBarIndex < sgs.getSizeInBars() - 1 && scenario.isPlayRestDataAvailable(songBarIndex + 1))
        {
            var nextValues = scenario.getPlayRestValues(songBarIndex + 1);
            nextBarPlayRestValue = nextValues.get(0);
        }


        // Draw the Play rectangle(s) and Play/Rest strings
        final int PLAY_RECT_RADIUS = 12;
        final int STR_PADDING = PLAY_RECT_RADIUS / 2;
        float halfBeat = ts.getHalfBarBeat(spt.getRhythm().getFeatures().division().isTernary());
        PlayRestValue playRestValue0 = playRestValues.get(0);
        PlayRestValue playRestValue1 = playRestValues.get(1);
        DenseSparseValue denseSparseValue0 = denseSparseValues.isEmpty() ? null : denseSparseValues.get(0);
        DenseSparseValue denseSparseValue1 = denseSparseValues.isEmpty() ? null : denseSparseValues.get(1);


        // First half  
        float x = x0;
        float wRect = halfBeat * wBeat;
        if (playRestValue0.equals(PlayRestValue.PLAY))
        {
            if (prevBarPlayRestValue != null && prevBarPlayRestValue.equals(PlayRestValue.PLAY))
            {
                // Hide the left rounded corners
                x -= PLAY_RECT_RADIUS;
                wRect += PLAY_RECT_RADIUS;
            }

            if (playRestValue1.equals(PlayRestValue.PLAY))
            {
                // Hide the right rounded corners                    
                wRect += PLAY_RECT_RADIUS;
            }

            // Do we also have dense/sparse info ?

            // Draw the round rectangle
            var shape = new RoundRectangle2D.Float(x, y0, wRect, h, PLAY_RECT_RADIUS, PLAY_RECT_RADIUS);
            Color c = PLAY_BACKGROUND_COLOR;
            if (denseSparseValue0 != null)
            {
                c = denseSparseValue0.equals(DenseSparseValue.DENSE) ? PLAY_DENSE_BACKGROUND_COLOR : PLAY_SPARSE_BACKGROUND_COLOR;
            }
            g2.setColor(c);
            g2.fill(shape);


            Rectangle2D bounds = getPlayStringBounds(g2);
            if (denseSparseValue0 != null)
            {
                bounds = denseSparseValue0.equals(DenseSparseValue.DENSE) ? getPlayDenseStringBounds(g2) : getPlaySparseStringBounds(g2);
            }

            // If first Play, add the string, except if not enough space            
            if ((prevBarPlayRestValue == null || prevBarPlayRestValue.equals(PlayRestValue.REST)) && bounds.getWidth() < wRect - STR_PADDING)
            {
                x += STR_PADDING;
                float y = (float) (r.y + (r.height - bounds.getHeight()) / 2 - bounds.getY());  // bounds are in baseline-relative coordinates!
                g2.setColor(FONT_COLOR);
                String s = PLAY_STRING;
                if (denseSparseValue0 != null)
                {
                    s = denseSparseValue0.equals(DenseSparseValue.DENSE) ? PLAY_DENSE_STRING : PLAY_SPARSE_STRING;
                }
                g2.drawString(s, x, y);

            } else if (denseSparseValue0 != null && !denseSparseValue0.equals(prevBarDenseSparseValue))
            {
                x += STR_PADDING;
                float y = (float) (r.y + (r.height - bounds.getHeight()) / 2 - bounds.getY());  // bounds are in baseline-relative coordinates!
                g2.setColor(FONT_COLOR);
                String s = denseSparseValue0.equals(DenseSparseValue.DENSE) ? PLAY_DENSE_STRING : PLAY_SPARSE_STRING;
                g2.drawString(s, x, y);
            }

        } else if (prevBarPlayRestValue == null || prevBarPlayRestValue.equals(PlayRestValue.PLAY))
        {
            // First rest 
            x += STR_PADDING;
            Rectangle2D bounds = getRestStringBounds(g2);
            float y = (float) (r.y + (r.height - bounds.getHeight()) / 2 - bounds.getY());  // bounds are in baseline-relative coordinates!
            g2.setColor(FONT_COLOR);
            g2.drawString(REST_STRING, x, y);
        }


        // Second half  
        x = x0 + halfBeat * wBeat;
        wRect = w - halfBeat * wBeat;
        if (playRestValue1.equals(PlayRestValue.PLAY))
        {
            if (playRestValue0.equals(PlayRestValue.PLAY))
            {
                // Hide the left rounded corners
                x -= PLAY_RECT_RADIUS;
                wRect += PLAY_RECT_RADIUS;
            }

            if (nextBarPlayRestValue != null && nextBarPlayRestValue.equals(PlayRestValue.PLAY))
            {
                // Hide the right rounded corners                    
                wRect += PLAY_RECT_RADIUS;
            }

            // Draw the round rectangle
            var shape = new RoundRectangle2D.Float(x, y0, wRect, h, PLAY_RECT_RADIUS, PLAY_RECT_RADIUS);
            Color c = PLAY_BACKGROUND_COLOR;
            if (denseSparseValue1 != null)
            {
                c = denseSparseValue1.equals(DenseSparseValue.DENSE) ? PLAY_DENSE_BACKGROUND_COLOR : PLAY_SPARSE_BACKGROUND_COLOR;
            }
            g2.setColor(c);
            g2.fill(shape);


            Rectangle2D bounds = getPlayStringBounds(g2);
            if (denseSparseValue1 != null)
            {
                bounds = denseSparseValue1.equals(DenseSparseValue.DENSE) ? getPlayDenseStringBounds(g2) : getPlaySparseStringBounds(g2);
            }

            // If first Play, add the string, except if not enough space
            if (playRestValue0.equals(PlayRestValue.REST) && bounds.getWidth() < wRect - STR_PADDING)
            {
                x += STR_PADDING;
                float y = (float) (r.y + (r.height - bounds.getHeight()) / 2 - bounds.getY());  // bounds are in baseline-relative coordinates!
                g2.setColor(FONT_COLOR);
                String s = PLAY_STRING;
                if (denseSparseValue1 != null)
                {
                    s = denseSparseValue1.equals(DenseSparseValue.DENSE) ? PLAY_DENSE_STRING : PLAY_SPARSE_STRING;
                }
                g2.drawString(s, x, y);
            } else if (denseSparseValue1 != null && !denseSparseValue1.equals(denseSparseValue0))
            {
                x += STR_PADDING;
                float y = (float) (r.y + (r.height - bounds.getHeight()) / 2 - bounds.getY());  // bounds are in baseline-relative coordinates!
                g2.setColor(FONT_COLOR);
                String s = denseSparseValue1.equals(DenseSparseValue.DENSE) ? PLAY_DENSE_STRING : PLAY_SPARSE_STRING;
                g2.drawString(s, x, y);
            }

        } else if (playRestValue0.equals(PlayRestValue.PLAY))
        {
            // First rest 
            x += STR_PADDING;
            Rectangle2D bounds = getRestStringBounds(g2);
            float y = (float) (r.y + (r.height - bounds.getHeight()) / 2 - bounds.getY());  // bounds are in baseline-relative coordinates!
            g2.setColor(FONT_COLOR);
            g2.drawString(REST_STRING, x, y);
        }


        drawTopBottomLines(g2);


        // Draw the playback point if required
        if (playbackPosition != null && playbackPointEnabled)
        {
            Color c = new Color(186, 34, 23);
            x = x0 + Math.round(playbackPosition.getBeat() * wBeat);
            float y = r.y + h - 1;

            final float SIZE = 5;
            var triangle = new Path2D.Float();
            triangle.moveTo(x - SIZE, y);
            triangle.lineTo(x + SIZE, y);
            triangle.lineTo(x, y - 1.5f * SIZE);
            triangle.lineTo(x - SIZE, y);

            g2.setColor(c);
            g2.fill(triangle);
        }

    }

    @Override
    public String toString()
    {
        return "BR_ImproSupport[barIndex=" + getBarIndex() + ", songBarIndex=" + songBarIndex + "]";
    }

    /**
     * Vertical zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    @Override
    public void setZoomVFactor(int factor)
    {
        if (zoomVFactor == factor)
        {
            return;
        }
        zoomVFactor = factor;
        repaint();
    }

    @Override
    public int getZoomVFactor()
    {
        return zoomVFactor;
    }

    @Override
    public boolean isRegisteredItemClass(ChordLeadSheetItem<?> item)
    {
        return false;
    }

    @Override
    protected ItemRenderer createItemRenderer(ChordLeadSheetItem<?> item)
    {
        throw new IllegalStateException("item=" + item);
    }

    @Override
    public void moveItemRenderer(ChordLeadSheetItem<?> item)
    {
        throw new IllegalStateException("item=" + item);
    }

    @Override
    public void setSection(CLI_Section section)
    {
        // Nothing
    }

    @Override
    public void showInsertionPoint(boolean b, ChordLeadSheetItem<?> item, Position pos, boolean copyMode)
    {
        // Nothing
    }

    @Override
    public void showPlaybackPoint(boolean b, Position pos)
    {
        playbackPosition = b ? new Position(pos) : null;
        repaint();
    }


    /**
     * Get all the BR_ImproSupport instances ordered by bar index for the specified editor.
     *
     * @param editor
     * @return Can be empty
     */
    static public List<BR_ImproSupport> getBR_ImproSupportInstances(CL_Editor editor)
    {
        checkNotNull(editor);
        var res = new ArrayList<BR_ImproSupport>();
        var brs = mapEditorBrs.get(System.identityHashCode(editor.getSongModel()));
        if (brs != null)
        {
            res.addAll(brs);
        }
        return res;
    }

    /**
     * Should be called when a song is discarded, in order to remove all the references to the related BR_Instances.
     *
     * @param song
     */
    static public void removeBR_ImproSupportInstances(Song song)
    {
        for (var songKey : mapEditorBrs.keySet())
        {
            if (songKey == System.identityHashCode(song))
            {
                mapEditorBrs.remove(songKey);
                break;
            }
        }
    }
    // ---------------------------------------------------------------
    // ChangeListener interface
    // ---------------------------------------------------------------

    @Override
    public void stateChanged(ChangeEvent e)
    {
        repaint();
    }

    // ---------------------------------------------------------------
    // Private methods
    // ---------------------------------------------------------------

    private void drawTopBottomLines(Graphics2D g2)
    {
        Rectangle r = getDrawingArea();
        final float w = r.width;
        final float h = r.height;
        final float x0 = r.x;
        final float y0 = r.y;
        g2.setColor(Color.LIGHT_GRAY);
        var line = new Line2D.Float(x0, y0, x0 + w - 1, y0);
        g2.draw(line);
        line = new Line2D.Float(x0, y0 + h - 1, x0 + w - 1, y0 + h - 1);
        g2.draw(line);
    }

    private Rectangle2D getPlayStringBounds(Graphics2D g2)
    {
        if (PLAY_STRING_BOUNDS == null)
        {
            StringMetrics sm = StringMetrics.create(g2);
            PLAY_STRING_BOUNDS = sm.getLogicalBoundsNoLeading(PLAY_STRING);
        }
        return PLAY_STRING_BOUNDS;
    }

    private Rectangle2D getPlaySparseStringBounds(Graphics2D g2)
    {
        if (PLAY_SPARSE_STRING_BOUNDS == null)
        {
            StringMetrics sm = StringMetrics.create(g2);
            PLAY_SPARSE_STRING_BOUNDS = sm.getLogicalBoundsNoLeading(PLAY_SPARSE_STRING);
        }
        return PLAY_SPARSE_STRING_BOUNDS;
    }

    private Rectangle2D getPlayDenseStringBounds(Graphics2D g2)
    {
        if (PLAY_DENSE_STRING_BOUNDS == null)
        {
            StringMetrics sm = StringMetrics.create(g2);
            PLAY_DENSE_STRING_BOUNDS = sm.getLogicalBoundsNoLeading(PLAY_DENSE_STRING);
        }
        return PLAY_DENSE_STRING_BOUNDS;
    }

    private Rectangle2D getRestStringBounds(Graphics2D g2)
    {
        if (REST_STRING_BOUNDS == null)
        {
            StringMetrics sm = StringMetrics.create(g2);
            REST_STRING_BOUNDS = sm.getLogicalBoundsNoLeading(REST_STRING);
        }
        return REST_STRING_BOUNDS;
    }

    private int getSongKey()
    {
        return System.identityHashCode(getEditor().getSongModel());
    }

    // ---------------------------------------------------------------
    // Private classes
    // ---------------------------------------------------------------
}
