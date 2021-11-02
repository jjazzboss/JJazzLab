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
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.improvisionsupport.PlayRestScenario.Value;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.song.api.Song;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererSettings;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;
import org.jjazz.ui.itemrenderer.api.ItemRendererFactory;
import org.jjazz.ui.utilities.api.StringMetrics;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.util.api.ResUtil;

/**
 * A BarRenderer to show improvisition support info.
 */
public class BR_ImproSupport extends BarRenderer implements ChangeListener
{

    private static final Color REST_BACKGROUND_COLOR = Color.LIGHT_GRAY;
    private static final Font PLAY_FONT = GeneralUISettings.getInstance().getStdCondensedFont();
    private static final Color FONT_COLOR = Color.DARK_GRAY.darker();
    private static final int PREF_HEIGHT = 15;


    private PlayRestScenario scenario;

    private final String playString = ResUtil.getString(getClass(), "Play");
    private final String restString = ResUtil.getString(getClass(), "Rest");
    private int nbBeats;    // A negative value means the bar is beyond the chord leadsheet model
    private int zoomVFactor = 50;
    /**
     * Maintain an ordered list of BR_ImproSupport instances per editor.
     * <p>
     * We use a WeakHashMap to not keep a reference to a CL_Editor instance when editor is discarded.
     */
    private static final Map<CL_Editor, List<BR_ImproSupport>> mapEditorBrs = new WeakHashMap<>();
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


        nbBeats = -1; // By default, we assume the bar renderer is past the chord leadsheet end


        // Update the list of instances ordered by bar index
        var brs = mapEditorBrs.get(editor);
        if (brs == null)
        {
            brs = new ArrayList<>();
            mapEditorBrs.put(editor, brs);
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
     * Overridden to know if we're past the end of the chord leadsheet.
     *
     * @param barIndex
     */
    @Override
    public void setModelBarIndex(int barIndex)
    {
        super.setModelBarIndex(barIndex);

        if (barIndex >= 0)
        {
            var song = getEditor().getSongModel();
            var ss = song.getSongStructure();
            nbBeats = (int) ss.getSongPart(barIndex).getParentSection().getData().getTimeSignature().getNbNaturalBeats();
        } else
        {
            nbBeats = -1;
        }

    }

    /**
     * Overridden to remove the instance from mapEditorBrs.
     */
    @Override
    public void cleanup()
    {
        super.cleanup();
        var brs = mapEditorBrs.get(getEditor());
        brs.remove(this);
    }


    /**
     * Overridden to draw a line corresponding to the section's color.
     *
     * @param g
     */
    @Override
    public void paintComponent(Graphics g)
    {
        if (scenario == null || nbBeats < 0)
        {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(PLAY_FONT);


        Rectangle r = getDrawingArea();
        final float w = r.width;
        final float h = r.height;
        final float x0 = r.x;
        final float y0 = r.y;
        final float wBeat = (float) Math.ceil(w / nbBeats);
        final float yc = y0 + h / 2;


        List<Value> values = scenario.getPlayRestValues(getBarIndex());

        // Get last value of previous bar
        Value prevValue = Value.REST;
        if (getBarIndex() > 0)
        {
            var prevValues = scenario.getPlayRestValues(getBarIndex() - 1);
            prevValue = prevValues.get(prevValues.size() - 1);
        }

        StringMetrics sm = new StringMetrics(g2);
        var playBounds = sm.getLogicalBoundsNoLeading(playString);
        var restBounds = sm.getLogicalBoundsNoLeading(restString);


        // Fill the bar when playing, and print "Play" on each first Play beat
        final int strPadding = 3;
        for (int i = 0; i < nbBeats; i++)
        {
            Value value = values.get(i);
            float x = x0 + i * wBeat;
            if (value.equals(Value.REST))
            {
                g2.setColor(REST_BACKGROUND_COLOR);
                var shape = new Rectangle2D.Float(x, y0, wBeat, h);
                g2.fill(shape);
                if (prevValue.equals(Value.PLAY))
                {
                    x += strPadding;
                    float y = (float) (r.y + (r.height - restBounds.getHeight()) / 2 - restBounds.getY());  // bounds are in baseline-relative coordinates!
                    g2.setColor(FONT_COLOR);
                    g2.drawString(restString, x, y);

                }
            } else if (prevValue.equals(Value.REST))
            {
                x += strPadding;
                float y = (float) (r.y + (r.height - playBounds.getHeight()) / 2 - playBounds.getY());  // bounds are in baseline-relative coordinates!
                g2.setColor(FONT_COLOR);
                g2.drawString(playString, x, y);
            }
            prevValue = value;
        }


        // Draw top and bottom lines
        g2.setColor(REST_BACKGROUND_COLOR);
        var line = new Line2D.Float(x0, y0, x0 + w - 1, y0);
        g2.draw(line);
        line = new Line2D.Float(x0, y0 + h - 1, x0 + w - 1, y0 + h - 1);
        g2.draw(line);

    }

    @Override
    public String toString()
    {
        return "BR_ImproSupport[" + getBarIndex() + "]";
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
        throw new IllegalStateException("item=" + item);   //NOI18N
    }

    @Override
    public void moveItemRenderer(ChordLeadSheetItem<?> item)
    {
        throw new IllegalStateException("item=" + item);   //NOI18N
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
        // Nothing
    }

    @Override
    public void setDisplayQuantizationValue(Quantization q)
    {
        // Do nothing
    }

    @Override
    public Quantization getDisplayQuantizationValue()
    {
        // Do nothing
        return null;
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
        var brs = mapEditorBrs.get(editor);
        if (brs != null)
        {
            res.addAll(brs);
        }
        return res;
    }

    /**
     * Should be called when a song is discarded, in order to remove all the references to its CL_Editor and BR_Instances.
     *
     * @param song
     * @return The removed CL_Editor, or null if nothing was removed
     */
    static public CL_Editor removeBR_ImproSupportInstances(Song song)
    {
        CL_Editor clEditor = null;
        for (var editor : mapEditorBrs.keySet())
        {
            if (editor.getModel() == song.getChordLeadSheet())
            {
                clEditor = editor;
                break;
            }
        }
        if (clEditor != null)
        {
            mapEditorBrs.remove(clEditor);
        }
        return clEditor;
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
    private Color computeColor(Value v)
    {
        return v.equals(Value.PLAY) ? Color.BLUE : Color.MAGENTA;
    }


    // ---------------------------------------------------------------
    // Private classes
    // ---------------------------------------------------------------
}
