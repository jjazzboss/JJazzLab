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
package org.jjazz.ui.cl_editor.barrenderer;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.quantizer.Quantization;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererSettings;
import org.jjazz.ui.cl_editor.barrenderer.api.BeatBasedBarRenderer;
import org.jjazz.ui.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.ui.itemrenderer.api.IR_Copiable;
import org.jjazz.ui.itemrenderer.api.IR_TimeSignatureSettings;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;
import org.jjazz.ui.itemrenderer.api.ItemRendererFactory;
import org.openide.util.Exceptions;

/**
 * A BarRenderer that show Chord and TimeSignature items.
 * <p>
 */
public class BR_Chords extends BarRenderer implements BeatBasedBarRenderer, ComponentListener
{

    /**
     * A special shared JPanel instance used to calculate the preferred size for all BR_Chords.
     */
    private static PrefSizePanel PREF_SIZE_CHORDS_PANEL = new PrefSizePanel();

    private static Dimension MIN_SIZE = new Dimension(10, 4);
    /**
     * The last TimeSignature we used to layout the items.
     */
    private TimeSignature lastTimeSignature;
    /**
     * The item used to represent the insertion point.
     */
    private ChordLeadSheetItem<?> cliIP;
    /**
     * ItemRenderer for the insertion point.
     */
    private ItemRenderer irIP;
    private BR_ChordsLayoutManager layoutManager;
    private int zoomVFactor = 50;
    private static final Logger LOGGER = Logger.getLogger(BR_Chords.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public BR_Chords(int barIndex, BarRendererSettings settings, ItemRendererFactory irf)
    {
        super(barIndex, settings, irf);

        // Default value
        lastTimeSignature = TimeSignature.FOUR_FOUR;

        // Our layout manager
        layoutManager = new BR_ChordsLayoutManager();
        setLayout(layoutManager);

        // Explicity set the preferred size so that layout's preferredLayoutSize() is never called
        // Use PREF_SIZE_BR_CHORDS prefSize and listen to its changes
        setPreferredSize(PREF_SIZE_CHORDS_PANEL.getPreferredSize());
        PREF_SIZE_CHORDS_PANEL.addComponentListener(this);
        setMinimumSize(MIN_SIZE);

    }

    /**
     * Overridden to unregister PREF_SIZE_BR_CHORDS.
     */
    @Override
    public void cleanup()
    {
        super.cleanup();
        PREF_SIZE_CHORDS_PANEL.removeComponentListener(this);
    }

    @Override
    public void moveItemRenderer(ChordLeadSheetItem<?> item)
    {
        revalidate();
    }

    @Override
    public void setSection(CLI_Section section)
    {
        TimeSignature newTs = section.getData().getTimeSignature();
        if (!newTs.equals(this.lastTimeSignature))
        {
            lastTimeSignature = newTs;
            revalidate();
        }
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

    @Override
    public void showInsertionPoint(boolean b, ChordLeadSheetItem<?> item, Position pos, boolean copyMode)
    {
        LOGGER.fine("showInsertionPoint() b=" + b + " item=" + item + " pos=" + pos + " copyMode=" + copyMode);
        if (!b)
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
        if (item instanceof CLI_ChordSymbol)
        {
            if (cliIP == null)
            {
                cliIP = new IP_ChordSymbol((CLI_ChordSymbol) item);
                ((IP_ChordSymbol) cliIP).setPosition(pos);
                irIP = addItemRenderer(cliIP);
                irIP.setSelected(true);
            } else
            {
                ((IP_ChordSymbol) cliIP).setPosition(pos);
                moveItemRenderer(cliIP);
            }
        } else if (item instanceof CLI_Section)
        {
            if (irIP == null)
            {
                irIP = addItemRenderer(item);
                irIP.setSelected(true);
            }
        }

        if (irIP instanceof IR_Copiable)
        {
            ((IR_Copiable) irIP).showCopyMode(copyMode);
        }
    }

    @Override
    public void showPlaybackPoint(boolean b, Position pos)
    {
        // Do nothing
    }

    @Override
    public void setDisplayQuantizationValue(Quantization q)
    {
        layoutManager.setDisplayQuantization(q);
        revalidate();
    }

    @Override
    public Quantization getDisplayQuantizationValue()
    {
        return layoutManager.getDisplayQuantization();
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
        PREF_SIZE_CHORDS_PANEL.setZoomVFactor(factor);
        // Apply to this BR_Chords object
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
    public String toString()
    {
        return "BR_Chords[" + getBarIndex() + "]";
    }

    @Override
    public boolean isRegisteredItemClass(ChordLeadSheetItem<?> item)
    {
        return (item instanceof CLI_ChordSymbol || item instanceof CLI_Section);
    }

    @Override
    protected ItemRenderer createItemRenderer(ChordLeadSheetItem<?> item)
    {
        if (!isRegisteredItemClass(item))
        {
            throw new IllegalArgumentException("item=" + item);
        }
        ItemRenderer ir;
        ItemRendererFactory irf = getItemRendererFactory();
        if (item instanceof CLI_ChordSymbol)
        {
            ir = irf.createItemRenderer(IR_Type.ChordSymbol, item);
        } else
        {
            // CLI_Section
            ir = irf.createItemRenderer(IR_Type.TimeSignature, item);
        }
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
        Dimension d = PREF_SIZE_CHORDS_PANEL.getSize();
        LOGGER.fine("componentResized() d=" + d);
        setPreferredSize(d);
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
    public TimeSignature getTimeSignature()
    {
        return lastTimeSignature;
    }

    // ---------------------------------------------------------------
    // Private classes
    // ---------------------------------------------------------------
    /**
     * A special shared JPanel instance used to calculate the preferred size for all BR_Chords.
     * <p>
     * Add ItemRenderers with the tallest size. Panel is added to the "hidden" BarRenderer's JDialog to be displayable so that
     * FontMetrics can be calculated with a Graphics object.
     * <p>
     */
    static private class PrefSizePanel extends JPanel implements PropertyChangeListener
    {

        int zoomVFactor;
        final ArrayList<ItemRenderer> irs = new ArrayList<>();
        final IR_ChordSymbolSettings csSettings = IR_ChordSymbolSettings.getDefault();
        final IR_TimeSignatureSettings tsSettings = IR_TimeSignatureSettings.getDefault();

        private static final Logger LOGGER2 = Logger.getLogger(PrefSizePanel.class.getName());

        public PrefSizePanel()
        {
            // FlowLayout sets children size to their preferredSize
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

            // Listen to settings changes impacting ItemRenderers size
            // Required here because the dialog is displayable but NOT visible (see myRevalidate()).
            csSettings.addPropertyChangeListener(this);
            tsSettings.addPropertyChangeListener(this);

            // Add the tallest possible items
            CLI_Factory clif = CLI_Factory.getDefault();
            ChordLeadSheetItem<?> item1 = null, item2;
            try
            {
                item1 = clif.createChordSymbol(null, new ExtChordSymbol("C#7#9b5"), new Position(0, 0));
            } catch (ParseException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            item2 = clif.createSection(null, "SECTIONNAME", TimeSignature.TWELVE_EIGHT, 0);

            ItemRenderer ir;
            ItemRendererFactory irf = ItemRendererFactory.getDefault();
            ir = irf.createItemRenderer(IR_Type.ChordSymbol, item1);
            irs.add(ir);
            add(ir);
            ir = irf.createItemRenderer(IR_Type.TimeSignature, item2);
            irs.add(ir);
            add(ir);

            // Add the panel to a hidden dialog so it can be made displayable (getGraphics() will return a non-null value, so font-based sizes
            // can be calculated
            JDialog dlg = BarRenderer.getFontMetricsDialog();
            dlg.add(this);
            dlg.pack();    // Force all components to be displayable
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

            int V_PADDING;
            if (zoomVFactor > 66)
            {
                V_PADDING = 3;
            } else if (zoomVFactor > 33)
            {
                V_PADDING = 2;
            } else
            {
                V_PADDING = 1;
            }

            Insets in = getInsets();
            int pWidth = irWidthSum + irs.size() * 5 + in.left + in.right;
            int pHeight = irMaxHeight + V_PADDING + in.top + in.bottom;

            Dimension d = new Dimension(pWidth, pHeight);
            // LOGGER2.severe("PrefSizePanel.getPreferredSize() d=" + d);
            return d;
        }

        public void setZoomVFactor(int vFactor)
        {
            if (zoomVFactor == vFactor)
            {
                return;
            }
            //  LOGGER2.severe("PrefSizePanel.setZoomVFactor() vFactor=" + vFactor);
            zoomVFactor = vFactor;
            for (ItemRenderer ir : irs)
            {
                ir.setZoomFactor(vFactor);
            }
            myRevalidate();
        }

        /**
         * Because dialog is displayable but not visible, invalidating a component is not enough to relayout everything.
         */
        private void myRevalidate()
        {
            JDialog dlg = BarRenderer.getFontMetricsDialog();
            dlg.pack();
        }

        //-----------------------------------------------------------------------
        // Implementation of the PropertiesListener interface
        //-----------------------------------------------------------------------
        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            if (e.getSource() == csSettings)
            {
                if (e.getPropertyName() == IR_ChordSymbolSettings.PROP_FONT)
                {
                    myRevalidate();
                }
            } else if (e.getSource() == tsSettings)
            {
                if (e.getPropertyName() == IR_TimeSignatureSettings.PROP_FONT)
                {
                    myRevalidate();
                }
            }
        }
    }
}
