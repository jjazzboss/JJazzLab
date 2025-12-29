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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.spi.BarRendererSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_SectionSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_Copiable;
import org.jjazz.cl_editorimpl.itemrenderer.IR_Section;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererFactory;
import org.jjazz.uisettings.api.ColorSetManager;

/**
 * A BarRenderer to show section names.
 * <p>
 * TODO: get rid of PrefSizePanel, use StringMetrics() instead.
 */
public class BR_Sections extends BarRenderer implements ComponentListener, PropertyChangeListener
{

    /**
     * Special shared JPanel instances per editor, used to calculate the preferred size for a BarRenderer subclass..
     */
    private static final Map<Integer, PrefSizePanel> mapEditorPrefSizePanel = new HashMap<>();

    private static final Dimension MIN_SIZE = new Dimension(10, 4);
    private CLI_Section cliSection;
    /**
     * The last color we used to paint this BarRenderer.
     */
    private Color sectionColor;
    /**
     * The ItemRenderer to show the insertion point.
     */
    private ItemRenderer insertionPointRenderer;
    private int zoomVFactor = 50;

    private static final Logger LOGGER = Logger.getLogger(BR_Sections.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public BR_Sections(CL_Editor editor, int barIndex, BarRendererSettings settings, ItemRendererFactory irf)
    {
        super(editor, barIndex, settings, irf);


        // By default
        sectionColor = null;


        // Our layout manager
        setLayout(new SeqLayoutManager());


        // Explicity set the preferred size so that layout's preferredLayoutSize() is never called
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
        if (cliSection != null)
        {
            cliSection.getClientProperties().removePropertyChangeListener(this);
        }

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
        throw new IllegalStateException("item=" + item);
    }

    /**
     * Set the current CLI_Section and possibly update the current color.
     *
     * @param newCliSection Can be null
     */
    @Override
    public void setSection(CLI_Section newCliSection)
    {
        if (cliSection != null)
        {
            cliSection.getClientProperties().removePropertyChangeListener(this);
        }
        cliSection = newCliSection;
        if (cliSection != null)
        {
            cliSection.getClientProperties().addPropertyChangeListener(this);
        }
        updateSectionColor();
    }

    @Override
    public void showInsertionPoint(boolean b, ChordLeadSheetItem<?> item, Position pos, boolean copyMode)
    {
        if (b)
        {
            if (insertionPointRenderer == null)
            {
                insertionPointRenderer = addItemRenderer(item);
                insertionPointRenderer.setSelected(true);
            }
            if (insertionPointRenderer instanceof IR_Copiable irc)
            {
                irc.showCopyMode(copyMode);
            }
        } else
        {
            removeItemRenderer(insertionPointRenderer);
            insertionPointRenderer = null;
        }

        if (insertionPointRenderer instanceof IR_Copiable irc)
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
        // Apply to this BR_Sections object
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

    /**
     * Overridden to draw a line corresponding to the section's color.
     *
     * @param g
     */
    @Override
    public void paintComponent(Graphics g)
    {
        if (sectionColor == null)
        {
            return;
        }

        int barWidth = getDrawingArea().width;
        int barHeight = getDrawingArea().height;
        int barLeft = getDrawingArea().x;
        int barTop = getDrawingArea().y;

        // Draw the axis
        g.setColor(sectionColor);

        int axisY = barTop + (barHeight / 2);
        int width = 4;
        g.fillRect(barLeft, axisY + 1 - width / 2, barWidth, width);
    }

    @Override
    public String toString()
    {
        return "BR_Section[" + getBarIndex() + "]";
    }

    @Override
    public boolean isRegisteredItemClass(ChordLeadSheetItem<?> item)
    {
        return item instanceof CLI_Section;
    }

    @Override
    protected ItemRenderer createItemRenderer(ChordLeadSheetItem<?> item)
    {
        if (!isRegisteredItemClass(item))
        {
            throw new IllegalArgumentException("item=" + item);
        }
        ItemRenderer ir = getItemRendererFactory().createItemRenderer(IR_Type.Section, item, getSettings().getItemRendererSettings());
        if (ir instanceof IR_Section irs && sectionColor != null)
        {
            irs.setSectionColor(sectionColor);
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
    // ----------------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------------

    @Override
    public void propertyChange(final PropertyChangeEvent evt)
    {
        if (evt.getSource() == cliSection.getClientProperties())
        {
            if (CL_EditorClientProperties.PROP_SECTION_COLOR.equals(evt.getPropertyName()))
            {
                updateSectionColor();
            }
        }
    }

    // ---------------------------------------------------------------
    // Private functions
    // ---------------------------------------------------------------
    private void updateSectionColor()
    {
        Color newColor = null;
        if (cliSection != null)
        {
            newColor = CL_EditorClientProperties.getSectionColor(cliSection);
            if (newColor == null)
            {
                newColor = ColorSetManager.getDefault().getColor(cliSection);
            }
        }

        if (sectionColor == null)
        {
            if (newColor != null)
            {
                sectionColor = newColor;
                repaint();
            }
        } else if (!sectionColor.equals(newColor))
        {
            sectionColor = newColor;
            repaint();
        }

        IR_Section irSection = getIR_Section();
        if (sectionColor != null && irSection != null)
        {
            irSection.setSectionColor(sectionColor);
        }
    }

    /**
     * Retrieve our IR_Section renderer if there is one on this bar.
     *
     * @return Can be null
     */
    private IR_Section getIR_Section()
    {
        IR_Section res = null;
        if (cliSection != null && cliSection.getPosition().getBar() == getModelBarIndex())
        {
            res = (IR_Section) getItemRenderer(cliSection);
        }
        return res;
    }

    // ---------------------------------------------------------------
    // Private methods
    // ---------------------------------------------------------------
    /**
     * Get the PrefSizePanel shared instance between BR_Sections of same groupKey.
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
     * A special shared JPanel instance used to calculate the preferred size for all BR_Sections.
     * <p>
     * Add ItemRenderers with the tallest size. Panel is added to the "hidden" BarRenderer's JDialog to be displayable so that FontMetrics can be calculated
     * with a Graphics object.
     * <p>
     */
    private class PrefSizePanel extends JPanel implements PropertyChangeListener
    {

        int zoomVFactor;
        final ArrayList<ItemRenderer> irs = new ArrayList<>();
        final IR_SectionSettings settings = IR_SectionSettings.getDefault();

        public PrefSizePanel()
        {
            // FlowLayout sets children size to their preferredSize
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));


            // Listen to settings changes impacting ItemRenderers size
            // Required here because the dialog is displayable but NOT visible (see myRevalidate()).
            settings.addPropertyChangeListener(this);


            // Add the tallest possible items
            CLI_Factory clif = CLI_Factory.getDefault();
            ChordLeadSheetItem<?> item = clif.createSection("SECTIONNAME", TimeSignature.TWELVE_EIGHT, 0, null);
            ItemRendererFactory irf = getItemRendererFactory();
            ItemRenderer ir = irf.createItemRenderer(IR_Type.Section, item, getSettings().getItemRendererSettings());
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
            settings.removePropertyChangeListener(this);
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

            int V_MARGIN = 1;    // Do not depend on zoomFactor     

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

        //-----------------------------------------------------------------------
        // Implementation of the PropertiesListener interface
        //-----------------------------------------------------------------------
        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            if (e.getSource() == settings)
            {
                if (e.getPropertyName().equals(IR_SectionSettings.PROP_FONT))
                {
                    forceRevalidate();
                }
            }
        }

    }

}
