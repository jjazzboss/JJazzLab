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
package org.jjazz.cl_editorimpl;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import org.jjazz.cl_editor.barrenderer.api.BeatBasedLayoutManager;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Path2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.barrenderer.api.BeatBasedBarRenderer;
import org.jjazz.cl_editor.spi.BarRendererSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_Copiable;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererFactory;
import org.jjazz.rhythm.api.Division;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongMetaEvents;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import org.openide.util.Exceptions;

/**
 * A BarRenderer that show position marks with beat graduations in the background.
 * 
 * TODO: get rid of PrefSizePanel, use StringMetrics() instead.
 */
public class BR_ChordPositions extends BarRenderer implements BeatBasedBarRenderer, ComponentListener
{

    /**
     * Special shared JPanel instances per editor, used to calculate the preferred size for a BarRenderer subclass..
     */
    private static final Map<Integer, PrefSizePanel> mapEditorPrefSizePanel = new HashMap<>();
    private static final Map<Integer, RhythmDivisionListener> mapEditorRhythmDivisionListener = new HashMap<>();

    private static final Dimension MIN_SIZE = new Dimension(10, 4);
    /**
     * The item used to represent the insertion point.
     */
    private ChordLeadSheetItem<?> cliIP;
    /**
     * ItemRenderer for the insertion point.
     */
    private ItemRenderer irIP;
    /**
     * Save the last TimeSignature used to draw the graduations.
     */
    TimeSignature lastTimeSignature;
    /**
     * If not null, represent the playback position in this bar.
     */
    private Position playbackPosition;
    private Division division;
    private final BeatBasedLayoutManager layoutManager;
    private int zoomVFactor = 50;
    private static final Logger LOGGER = Logger.getLogger(BR_ChordPositions.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public BR_ChordPositions(CL_Editor editor, int barIndex, BarRendererSettings settings, ItemRendererFactory irf)
    {
        super(editor, barIndex, settings, irf);

        // Default values
        lastTimeSignature = TimeSignature.FOUR_FOUR;
        division = Division.BINARY;

        // Set Layout
        layoutManager = new BeatBasedLayoutManager();
        setLayout(layoutManager);


        // Explicity set the preferred size so that layout's preferredLayoutSize() is never called
        // Use PREF_SIZE_CHORDPOSITIONS_PANEL prefSize and listen to its changes
        setPreferredSize(getPrefSizePanelSharedInstance().getPreferredSize());
        getPrefSizePanelSharedInstance().addComponentListener(this);
        setMinimumSize(MIN_SIZE);

    }

    /**
     * Overridden to unregister the pref size panel shared instance.
     */
    @Override
    public void cleanup()
    {
        super.cleanup();
        getPrefSizePanelSharedInstance().removeComponentListener(this);
        getRhythmDivisionListenerSharedInstance().cleanup();


        // Remove only if it's the last bar of the editor
        if (getEditor().getNbBarBoxes() == 1)
        {
            JDialog dlg = getFontMetricsDialog(getEditor());
            dlg.remove(getPrefSizePanelSharedInstance());
            mapEditorPrefSizePanel.remove(System.identityHashCode(getEditor()));
            getPrefSizePanelSharedInstance().cleanup();
        }
    }


    @Override
    public void moveItemRenderer(ChordLeadSheetItem<?> item)
    {
        revalidate();
    }

    public Division getDivision()
    {
        return division;
    }

    public void setDivision(Division newDivision)
    {
        Objects.requireNonNull(newDivision);
        if (division == newDivision)
        {
            return;
        }
        this.division = newDivision;
        repaint();  // required to update graduations
    }

    @Override
    public int setModelBarIndex(int bar)
    {
        int res = super.setModelBarIndex(bar);
        if (res != bar)
        {
            if (bar < 0)
            {
                getRhythmDivisionListenerSharedInstance().unregister(this);
            } else
            {
                getRhythmDivisionListenerSharedInstance().register(this);
            }
        }
        return res;
    }

    /**
     * TimeSignature and Division might have changed.
     *
     * @param section
     */
    @Override
    public void setSection(CLI_Section section)
    {
        if (section == null)
        {
            return;
        }
        TimeSignature newTs = section.getData().getTimeSignature();
        var newDiv = getDivision(getEditor().getSongModel(), section);
        if (newTs == lastTimeSignature && newDiv == division)
        {
            return;
        }
        lastTimeSignature = newTs;
        division = newDiv;
        revalidate();
        repaint();
    }

    /**
     * Return beat=0 by default, to be overridden if beatposition support.
     *
     * @param x int
     * @return LeadSheetPosition
     */
    @Override
    public Position getPositionFromPoint(int x)
    {
        return layoutManager.getPositionFromPoint(this, x);
    }

    /**
     * Overridden because we want a reduction also on top and bottom sides.
     *
     * @return
     */
    @Override
    public Rectangle getDrawingArea()
    {
        Insets in = getInsets();
        int BARWIDTH_REDUCTION = 2;
        int BARHEIGHT_REDUCTION = 1;
        return new Rectangle(in.left + BARWIDTH_REDUCTION, in.top + BARHEIGHT_REDUCTION,
                getWidth() - in.left - in.right - (2 * BARWIDTH_REDUCTION),
                getHeight() - in.top - in.bottom - (2 * BARHEIGHT_REDUCTION));
    }

    /**
     * Draw graduation marks and the playback position
     *
     * @param g The graphics context.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        int barWidth = getDrawingArea().width;
        int barHeight = getDrawingArea().height;
        int barLeft = getDrawingArea().x;
        int barTop = getDrawingArea().y;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        // Draw the axis
        g2.setColor(Color.GRAY);

        int axisY = barTop + barHeight - 1;
        g2.drawLine(barLeft, axisY, barLeft + barWidth, axisY);

        // Draw graduations
        lastTimeSignature = getTimeSignature();
        float nbBeats = lastTimeSignature.getNbNaturalBeats();
        // Default 4/4, 3/4 etc. => 4 small graduations per beat
        float step = .25f;
        int perBeatGrad = 4;
        if (lastTimeSignature.getLower() == 8 || division.isTernary())
        {
            // 6/8 12/8 => 3 small graduations per beat
            step = .33333f;
            perBeatGrad = 3;
        }
        int bigGraduationY = barTop;
        int smallGraduationY = Math.max(bigGraduationY + 1, barTop + barHeight / 2);

        int beatGrad = 1;
        for (float i = 0; i <= nbBeats; i += step)
        {
            int h;
            int x;

            if (beatGrad == 1)
            {
                h = bigGraduationY;
                g2.setColor(Color.BLACK);
            } else
            {
                h = smallGraduationY;
                g2.setColor(Color.GRAY);
            }
            beatGrad = (beatGrad == perBeatGrad) ? 1 : beatGrad + 1;

            x = ((BeatBasedLayoutManager) getLayout()).getBeatXPosition(i, barWidth, lastTimeSignature)
                    + barLeft;
            g2.drawLine(x, h, x, axisY);
        }


        // Draw the playback position
        if (playbackPosition != null)
        {
            float beat = playbackPosition.getBeat();
            float x = layoutManager.getBeatXPosition(beat, barWidth, lastTimeSignature) + barLeft;
            float y = axisY;

            final float SIZE = 5;
            var triangle = new Path2D.Float();
            triangle.moveTo(x - SIZE, y);
            triangle.lineTo(x + SIZE, y);
            triangle.lineTo(x, y - 1.5f * SIZE);
            triangle.lineTo(x - SIZE, y);

            Color c = new Color(186, 34, 23);
            g2.setColor(c);
            g2.fill(triangle);
        }
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
        // Forward to the shared panel instance
        getPrefSizePanelSharedInstance().setZoomVFactor(factor);
        // Apply to this BR_ChordPositions object
        zoomVFactor = factor;
        for (ItemRenderer ir : getItemRenderers())
        {
            ir.setZoomFactor(factor);
        }
        revalidate();
        repaint();
    }

    @Override
    public int getZoomVFactor()
    {
        return zoomVFactor;
    }

    @Override
    public void showInsertionPoint(boolean showInsertionPoint, ChordLeadSheetItem<?> item, Position pos, boolean copyMode)
    {
        LOGGER.log(Level.FINE, "showInsertionPoint() showInsertionPoint={0} item={1} pos={2} copyMode={3}", new Object[]
        {
            showInsertionPoint, item, pos, copyMode
        });
        if (!showInsertionPoint)
        {
            // Remove the insertion point
            if (irIP != null)
            {
                removeItemRenderer(irIP);
            }
            cliIP = null;
            irIP = null;
            return;
        }

        // Add or move the insertion point
        if (item instanceof CLI_ChordSymbol chordSymbol)
        {
            if (cliIP == null)
            {
                IP_ChordSymbol newCliIP = new IP_ChordSymbol(chordSymbol);
                newCliIP.setPosition(pos);
                irIP = addItemRenderer(newCliIP);
                irIP.setSelected(true);
                cliIP = newCliIP;
            } else
            {
                ((IP_ChordSymbol) cliIP).setPosition(pos);
                moveItemRenderer(cliIP);
            }
        }

        if (irIP instanceof IR_Copiable copiable)
        {
            copiable.showCopyMode(copyMode);
        }
    }

    @Override
    public void showPlaybackPoint(boolean showPlaybackPoint, Position pos)
    {
        LOGGER.log(Level.FINE, "showPlaybackPoint() showPlaybackPoint={0} pos={1}", new Object[]
        {
            showPlaybackPoint, pos
        });
        Preconditions.checkArgument(!showPlaybackPoint || pos.getBar() == getModelBarIndex(), "showPlaybackPoint=%s pos=%s this=%s", showPlaybackPoint, pos, this);
        
        if (!showPlaybackPoint)
        {
            playbackPosition = null;
        } else if (playbackPosition == null)
        {
            playbackPosition = new Position(pos);
        } else
        {
            playbackPosition.set(pos);
        }
        repaint();
    }

    @Override
    public String toString()
    {
        return "BR_ChordPositions[" + getBarIndex() + "]";
    }

    @Override
    public boolean isRegisteredItemClass(ChordLeadSheetItem<?> item)
    {
        return item instanceof CLI_ChordSymbol;
    }

    @Override
    protected ItemRenderer createItemRenderer(ChordLeadSheetItem<?> item)
    {
        if (!isRegisteredItemClass(item))
        {
            throw new IllegalArgumentException("item=" + item);
        }
        ItemRenderer ir = getItemRendererFactory().createItemRenderer(IR_Type.ChordPosition, item, getSettings().getItemRendererSettings());
        return ir;
    }

    //-----------------------------------------------------------------------
    // Implementation of the ComponentListener interface
    //-----------------------------------------------------------------------
    /**
     * Our reference panel size (so its prefSize also) has changed, update our preferredSize.
     *
     * @param e
     */
    @Override
    public void componentResized(ComponentEvent e)
    {
        setPreferredSize(getPrefSizePanelSharedInstance().getSize());
        revalidate();
        repaint();
    }

    @Override
    public void componentMoved(ComponentEvent e)
    {
        // Nothing
    }

    @Override
    public void componentShown(ComponentEvent e)
    {
        // Nothing
    }

    @Override
    public void componentHidden(ComponentEvent e)
    {
        // Nothing
    }

    // ---------------------------------------------------------------
    // Implements BeatBasedBarRenderer interface
    // ---------------------------------------------------------------
    @Override
    public final TimeSignature getTimeSignature()
    {
        return lastTimeSignature;
    }

    // ---------------------------------------------------------------
    // Private methods
    // ---------------------------------------------------------------

    /**
     * Get the division from the rhythm of the first SongPart related to cliSection.
     *
     * @param song
     * @param cliSection
     * @return
     */
    static protected Division getDivision(Song song, CLI_Section cliSection)
    {
        Objects.requireNonNull(song);
        Objects.requireNonNull(cliSection);
        var spts = song.getSongStructure().getSongParts();
        var spt = spts.stream()
                .filter(s -> s.getParentSection() == cliSection)
                .findFirst()
                .orElse(null);
        if (spt == null && !spts.isEmpty())
        {
            // No SongPart use our section! take another one
            spt = spts.get(0);
        }
        var d = spt == null ? Division.BINARY : spt.getRhythm().getFeatures().division();
        return d;
    }

    /**
     * Get the PrefSizePanel shared instance for our CL_Editor.
     *
     * @return
     */
    private PrefSizePanel getPrefSizePanelSharedInstance()
    {
        PrefSizePanel panel = mapEditorPrefSizePanel.get(System.identityHashCode(getEditor()));
        if (panel == null)
        {
            panel = new PrefSizePanel();
            mapEditorPrefSizePanel.put(System.identityHashCode(getEditor()), panel);
        }
        return panel;
    }

    /**
     * Get the RhythmDivisionListener shared instance for our CL_Editor.
     *
     * @return
     */
    private RhythmDivisionListener getRhythmDivisionListenerSharedInstance()
    {
        RhythmDivisionListener rdl = mapEditorRhythmDivisionListener.get(System.identityHashCode(getEditor()));
        if (rdl == null)
        {
            rdl = new RhythmDivisionListener(getEditor().getSongModel());
            mapEditorRhythmDivisionListener.put(System.identityHashCode(getEditor()), rdl);
        }
        return rdl;
    }


    // ---------------------------------------------------------------
    // Private classes
    // ---------------------------------------------------------------
    /**
     * A special shared JPanel instance used to calculate the preferred size for all BR_ChordPositions.
     * <p>
     * Add ItemRenderers with the tallest size. Panel is added to the "hidden" BarRenderer's JDialog to be displayable so that FontMetrics can be calculated
     * with a Graphics object.
     * <p>
     */
    private class PrefSizePanel extends JPanel
    {

        int zoomVFactor;
        ArrayList<ItemRenderer> irs = new ArrayList<>();

        public PrefSizePanel()
        {
            // FlowLayout sets children size to their preferredSize
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

            // Add the tallest possible item
            CLI_Factory clif = CLI_Factory.getDefault();
            ChordLeadSheetItem<?> item = null;
            try
            {
                item = clif.createChordSymbol("C7#9", 0, 0);
            } catch (ParseException ex)
            {
                Exceptions.printStackTrace(ex);
            }

            ItemRenderer ir = getItemRendererFactory().createItemRenderer(IR_Type.ChordPosition, item, getSettings().getItemRendererSettings());
            irs.add(ir);
            add(ir);

            // Add the panel to a hidden dialog so it can be made displayable (getGraphics() will return a non-null value, so font-based sizes
            // can be calculated
            JDialog dlg = getFontMetricsDialog(getEditor());
            dlg.add(this);
            dlg.pack();    // Force all components to be displayable
        }

        public void cleanup()
        {

        }

        /**
         * Overridden to use our own calculation instead of using FlowLayout's preferredLayoutSize().
         *
         * @return
         */
        @Override
        public Dimension getPreferredSize()
        {
            // Get the max preferred height from ItemRenderers and the sum of their preferred width
            int irMaxHeight = 0;
            int irWidthSum = 0;
            for (ItemRenderer ir : irs)
            {
                Dimension pd = ir.getPreferredSize();
                irWidthSum += pd.width;
                if (pd.height > irMaxHeight)
                {
                    irMaxHeight = pd.height;
                }
            }

            int V_MARGIN = 1;
            if (zoomVFactor < 33)
            {
                V_MARGIN = 1;
            } else if (zoomVFactor > 66)
            {
                V_MARGIN = 2;
            }

            Insets in = getInsets();
            int pWidth = irWidthSum + irs.size() * 5 + in.left + in.right;
            int pHeight = irMaxHeight + 2 * V_MARGIN + in.top + in.bottom;

            return new Dimension(pWidth, pHeight);
        }

        public void setZoomVFactor(int vFactor)
        {
            if (zoomVFactor == vFactor)
            {
                return;
            }
            zoomVFactor = vFactor;
            for (ItemRenderer ir : irs)
            {
                ir.setZoomFactor(vFactor);
            }
            forceRevalidate();
        }

        /**
         * Because dialog is displayable but not visible, invalidating a component is not enough to relayout everything.
         */
        private void forceRevalidate()
        {
            getFontMetricsDialog(getEditor()).pack();
        }

    }


    /**
     * Listen to song model rhythms changes to update BR_ChordPositions division.
     */
    static private class RhythmDivisionListener implements PropertyChangeListener
    {

        private final SongMetaEvents songMetaEvents;
        private final Set<BR_ChordPositions> brChordPositions;

        public RhythmDivisionListener(Song song)
        {
            brChordPositions = new HashSet<>();
            this.songMetaEvents = SongMetaEvents.getInstance(song);
            this.songMetaEvents.addPropertyChangeListener(SongMetaEvents.PROP_CLS_SGS_API_CHANGE_COMPLETE, this);
        }

        /**
         * Register BR_ChordPositions to be updated (via setDivision()) if its related rhythm division changes.
         *
         * @param br
         */
        public void register(BR_ChordPositions br)
        {
            Objects.requireNonNull(br);
            brChordPositions.add(br);
        }

        public void unregister(BR_ChordPositions br)
        {
            Objects.requireNonNull(br);
            brChordPositions.remove(br);
        }

        public void cleanup()
        {
            songMetaEvents.removePropertyChangeListener(SongMetaEvents.PROP_CLS_SGS_API_CHANGE_COMPLETE, this);
        }

        // ---------------------------------------------------------------
        //  PropertyChangeListener interface
        // ---------------------------------------------------------------
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            // SongMetaEvents.PROP_CLS_SGS_API_CHANGE_COMPLETE
            if (evt.getOldValue() instanceof SgsActionEvent sae && sae.getApiId() == SgsActionEvent.API_ID.ReplaceSongParts)
            {
                updateSectionsDivision();
            }
        }

        private void updateSectionsDivision()
        {
            var song = songMetaEvents.getSong();

            Set<CLI_Section> processed = new HashSet<>();
            for (var spt : song.getSongStructure().getSongParts())
            {
                var d = spt.getRhythm().getFeatures().division();
                var cliSection = spt.getParentSection();
                if (!processed.contains(cliSection))
                {
                    processed.add(cliSection);
                    var brSection = song.getChordLeadSheet().getBarRange(cliSection);
                    brChordPositions.stream()
                            .filter(br -> brSection.contains(br.getModelBarIndex()))
                            .forEach(br -> br.setDivision(d));
                }
            }
        }

    }

}
