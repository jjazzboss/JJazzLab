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
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.barrenderer.api.BeatBasedBarRenderer;
import org.jjazz.cl_editor.spi.BarRendererSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_Copiable;
import org.jjazz.cl_editor.itemrenderer.api.IR_TimeSignatureSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererFactory;
import org.openide.util.Exceptions;
import org.jjazz.cl_editor.api.DisplayTransposableRenderer;

/**
 * A BarRenderer that show Chord and TimeSignature items.
 * <p>
 * TODO: get rid of PrefSizePanel, use StringMetrics() instead.
 */
public class BR_Chords extends BarRenderer implements BeatBasedBarRenderer, ComponentListener, DisplayTransposableRenderer
{

    private static final Logger LOGGER = Logger.getLogger(BR_Chords.class.getSimpleName());

    /**
     * Special shared JPanel instances per editor, used to calculate the preferred size for a BarRenderer subclass..
     */
    private static final Map<Integer, PrefSizePanel> mapEditorPrefSizePanel = new HashMap<>();
    private static final Dimension MIN_SIZE = new Dimension(10, 4);

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
    private final BR_ChordsLayoutManager layoutManager;
    private int zoomVFactor = 50;
    private int displayTransposition = 0;
    private boolean showAnnotation = false;


    @SuppressWarnings("LeakingThisInConstructor")
    public BR_Chords(CL_Editor editor, int barIndex, BarRendererSettings settings, ItemRendererFactory irf)
    {
        super(editor, barIndex, settings, irf);

        // Default value
        lastTimeSignature = TimeSignature.FOUR_FOUR;

        // Our layout manager
        layoutManager = new BR_ChordsLayoutManager();
        setLayout(layoutManager);


        // Explicity set the preferred size so that layout's preferredLayoutSize() is never called
        // listen to its preferred size changes
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

    @Override
    public void setSection(CLI_Section section)
    {
        if (section == null)
        {
            return;
        }
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
    public int getDisplayTransposition()
    {
        return displayTransposition;
    }

    @Override
    public void setDisplayTransposition(int dt)
    {
        displayTransposition = dt;

        getItemRenderers().stream()
                .filter(DisplayTransposableRenderer.class::isInstance)
                .map(DisplayTransposableRenderer.class::cast)
                .forEach(ir -> ir.setDisplayTransposition(displayTransposition));
    }

    @Override
    public void showInsertionPoint(boolean showIP, ChordLeadSheetItem<?> item, Position pos, boolean copyMode)
    {
        LOGGER.log(Level.FINE, "showInsertionPoint() showIP={0} item={1} pos={2} copyMode={3}", new Object[]
        {
            showIP, item, pos, copyMode
        });
        if (!showIP)
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
        if (item instanceof CLI_ChordSymbol cliCs)
        {
            if (cliIP == null)
            {
                IP_ChordSymbol newCliIP = new IP_ChordSymbol(cliCs);
                newCliIP.setPosition(pos);
                irIP = addItemRenderer(newCliIP);
                irIP.setSelected(true);
                cliIP = newCliIP;
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
        } else if (item instanceof CLI_BarAnnotation)
        {
            if (irIP == null)
            {
                irIP = addItemRenderer(item);
                irIP.setSelected(true);
            }
        }

        if (irIP instanceof IR_Copiable irc)
        {
            irc.showCopyMode(copyMode);
        }
    }

    @Override
    public void showPlaybackPoint(boolean b, Position pos)
    {
        // Do nothing
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

    /**
     * Set whether CLI_BarAnnotation renderer is shown or not.
     *
     * @param show
     */
    public void showAnnotation(boolean show)
    {
        boolean old = showAnnotation;
        showAnnotation = show;
        if (old != showAnnotation)
        {
            // Force update of ItemRenderers
            resetModel(getModel(), getModelBarIndex());
        }
    }

    public boolean isAnnotationShown()
    {
        return showAnnotation;
    }

    @Override
    public boolean isRegisteredItemClass(ChordLeadSheetItem<?> item)
    {
        boolean b = item instanceof CLI_ChordSymbol
                || item instanceof CLI_Section
                || (showAnnotation && item instanceof CLI_BarAnnotation);
        return b;
    }

    @Override
    protected ItemRenderer createItemRenderer(ChordLeadSheetItem<?> item)
    {
        Preconditions.checkArgument(isRegisteredItemClass(item), "item=%s", item);

        ItemRenderer ir;
        ItemRendererFactory irf = getItemRendererFactory();
        if (item instanceof CLI_ChordSymbol cliCs)
        {
            ir = irf.createItemRenderer(IR_Type.ChordSymbol, cliCs, getSettings().getItemRendererSettings());
        } else if (item instanceof CLI_Section cliSection)
        {
            // CLI_Section
            ir = irf.createItemRenderer(IR_Type.TimeSignature, cliSection, getSettings().getItemRendererSettings());
        } else
        {
            // CLI_BarAnnotation
            ir = irf.createItemRenderer(IR_Type.BarAnnotationPaperNote, item, getSettings().getItemRendererSettings());
        }
        if (ir instanceof DisplayTransposableRenderer transposable)
        {
            transposable.setDisplayTransposition(displayTransposition);
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
        Dimension d = getPrefSizePanelSharedInstance().getSize();
        LOGGER.log(Level.FINE, "componentResized() d={0}", d);
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
    // Private methods
    // ---------------------------------------------------------------
    /**
     * Get the PrefSizePanel shared instance between BR_Chords of same groupKey.
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

    // ---------------------------------------------------------------
    // Private classes
    // ---------------------------------------------------------------
    /**
     * A special shared JPanel instance used to calculate the preferred size for all BR_Chords.
     * <p>
     * Add ItemRenderers with the tallest size. Panel is added to the "hidden" BarRenderer's JDialog to be displayable so that FontMetrics can be calculated
     * with a Graphics object.
     * <p>
     */
    private class PrefSizePanel extends JPanel implements PropertyChangeListener
    {

        int zoomVFactor;
        final ArrayList<ItemRenderer> irs = new ArrayList<>();
        final IR_ChordSymbolSettings csSettings = IR_ChordSymbolSettings.getDefault();
        final IR_TimeSignatureSettings tsSettings = IR_TimeSignatureSettings.getDefault();

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
                item1 = clif.createChordSymbol("C#7#9b5", 0, 0);
            } catch (ParseException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            item2 = clif.createSection("SECTIONNAME", TimeSignature.TWELVE_EIGHT, 0, null);

            ItemRenderer ir;
            ItemRendererFactory irf = getItemRendererFactory();
            ir = irf.createItemRenderer(IR_Type.ChordSymbol, item1, getSettings().getItemRendererSettings());
            irs.add(ir);
            add(ir);
            ir = irf.createItemRenderer(IR_Type.TimeSignature, item2, getSettings().getItemRendererSettings());
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
            csSettings.removePropertyChangeListener(this);
            tsSettings.removePropertyChangeListener(this);
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
            return d;
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
         * Because dialog is displayable but not visible, invalidating a component is not enough to re-layout everything.
         */
        private void forceRevalidate()
        {
            getFontMetricsDialog(getEditor()).pack();
        }

        //-----------------------------------------------------------------------
        // Implementation of the PropertiesListener interface
        //-----------------------------------------------------------------------
        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            if (e.getSource() == csSettings)
            {
                if (e.getPropertyName().equals(IR_ChordSymbolSettings.PROP_FONT))
                {
                    forceRevalidate();
                }
            } else if (e.getSource() == tsSettings)
            {
                if (e.getPropertyName().equals(IR_TimeSignatureSettings.PROP_FONT))
                {
                    forceRevalidate();
                }
            }
        }
    }
}
