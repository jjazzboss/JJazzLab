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
package org.jjazz.ui.cl_editor.barbox.api;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.quantizer.Quantization;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererFactory;
import org.jjazz.ui.cl_editor.barrenderer.api.BeatBasedBarRenderer;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;

/**
 * This object groups several BarRenderers in a "stack view" that represent one leadSheet bar.
 */
public class BarBox extends JPanel implements FocusListener, PropertyChangeListener
{
    // GUI

    private CL_Editor editor;
    /**
     * Our graphical settings.
     */
    private BarBoxSettings bbSettings;
    // APPLICATION
    /**
     * The BarRenderers displayed in this BarBox.
     */
    private BarBoxConfig barBoxConfig;
    /* The bar index of this box. */
    private int barIndex;
    /**
     * Our leadsheet model.
     */
    private ChordLeadSheet model;
    /**
     * The bar index within the model.
     */
    private int modelBarIndex;
    /**
     * True if the BarBox is selected.
     */
    private boolean isSelected;
    /**
     * True if the playback position is on this bar.
     */
    private boolean isPlaybackOn;
    private Quantization displayQuantization;
    private int zoomVFactor = 50;
    private BarRendererFactory barRendererFactory;

    /**
     * Construct a BarBox.
     *
     * @param editor Can be null
     * @param bbIndex The index of this BarBox.
     * @param modelBarIndex Use -1 if this BarBox does not represent model data.
     * @param model
     * @param config
     * @param settings
     * @param brf
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public BarBox(CL_Editor editor, int bbIndex, int modelBarIndex, ChordLeadSheet model, BarBoxConfig config, BarBoxSettings settings, BarRendererFactory brf)
    {
        this.editor = editor;
        displayQuantization = Quantization.BEAT;


        // Pile up BarRenderers
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (bbIndex < 0 || model == null || config == null)
        {
            throw new IllegalArgumentException("barIndex=" + bbIndex + " model=" + model + " config="
                    + config);
        }

        // Register settings changes
        this.bbSettings = settings;
        this.bbSettings.addPropertyChangeListener(this);


        // To create BarRenderers
        barRendererFactory = brf;


        this.barIndex = bbIndex;
        setModel(modelBarIndex, model);

        Border border = settings.getTitledBorder((bbIndex >= 0) ? String.valueOf(bbIndex + 1) : "");
        if (border != null)
        {
            setBorder(border);
        }

        // The used BarRenderers
        setConfig(config);

        addFocusListener(this);

        // Disable focus keys on BarBox : must be managed at a higher level
        setFocusTraversalKeysEnabled(false);
    }

    /**
     * Set the model for this BarBox.
     *
     * @param modelBarIndex If &lt; 0, it means this BarBox does not represent a valid bar for model.
     * @param model
     */
    public final void setModel(int modelBarIndex, ChordLeadSheet model)
    {
        if (model == null || modelBarIndex >= model.getSize())
        {
            throw new IllegalArgumentException("model=" + model);
        }
        this.modelBarIndex = modelBarIndex;
        this.model = model;

        // Forward the change to BarRenderers
        for (BarRenderer br : getBarRenderers())
        {
            br.setModel(this.modelBarIndex, this.model);
        }

        refreshBackground();
    }

    /**
     * Add an item in the BarBox. The operation requests each BarRenderer to create ItemRenderers if appropriate.
     *
     * @return List The created ItemRenderers.
     */
    public List<ItemRenderer> addItem(ChordLeadSheetItem<?> item)
    {
        if (item == null)
        {
            throw new IllegalArgumentException("item=" + item);
        }
        ArrayList<ItemRenderer> result = new ArrayList<>();
        for (BarRenderer br : getBarRenderers())
        {
            if (br.isRegisteredItemClass(item))
            {
                result.add(br.addItemRenderer(item));
            }
        }
        return result;
    }

    /**
     * Remove an item from the BarBox. The operation requests each BarRenderer to remove the ItemRenderer if appropriate.
     *
     * @return List The removed ItemRenderers. Can be an empty list.
     */
    public List<ItemRenderer> removeItem(ChordLeadSheetItem<?> item)
    {
        if (item == null)
        {
            throw new IllegalArgumentException("item=" + item);
        }
        ArrayList<ItemRenderer> result = new ArrayList<>();
        for (BarRenderer br : getBarRenderers())
        {
            if (br.isRegisteredItemClass(item))
            {
                ItemRenderer ir = br.removeItemRenderer(item);
                if (ir != null)
                {
                    result.add(ir);
                }
            }
        }
        return result;
    }

    /**
     * Set the focus on an ItemRenderer for item.
     *
     * @param item
     * @param irType The irType to search for. If null set focus on the first ItemRenderer found.
     */
    public void setFocusOnItem(ChordLeadSheetItem<?> item, IR_Type irType)
    {
        for (BarRenderer br : getBarRenderers())
        {
            if (br.isRegisteredItemClass(item))
            {
                for (ItemRenderer ir : br.getItemRenderers())
                {
                    if (ir.getModel() == item && (irType == null || ir.getIR_Type() == irType))
                    {
                        ir.requestFocusInWindow();
                        return;
                    }
                }
            }
        }
    }

    /**
     * The operation requests each BarRenderer to select the ItemRenderers of item.
     *
     * @param item
     * @param b
     */
    public void selectItem(ChordLeadSheetItem<?> item, boolean b)
    {
        if (item == null)
        {
            throw new IllegalArgumentException("item=" + item);
        }
        for (BarRenderer br : getBarRenderers())
        {
            if (br.isRegisteredItemClass(item))
            {
                br.selectItem(item, b);
            }
        }
    }

    public boolean isSelected()
    {
        return isSelected;
    }

    /**
     * Set the Component selected or not, forward to BarRenderers as well.
     */
    public void setSelected(boolean b)
    {
        if (b != isSelected)
        {
            isSelected = b;
            refreshBackground();
            for (BarRenderer br : getBarRenderers())
            {
                br.setSelected(b);
            }
        }
    }

    @Override
    public void setEnabled(boolean b)
    {
        if (b == isEnabled())
        {
            return;
        }
        super.setEnabled(b);
        for (BarRenderer br : getBarRenderers())
        {
            br.setEnabled(b);
        }
        refreshBackground();
    }

    /**
     * Vertical zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    public void setZoomVFactor(int factor)
    {
        if (factor == zoomVFactor)
        {
            return;
        }
        zoomVFactor = factor;
        for (BarRenderer br : this.getBarRenderers())
        {
            br.setZoomVFactor(factor);
        }
    }

    public int getZoomVFactor()
    {
        return zoomVFactor;
    }

    public Quantization getDisplayQuantizationValue()
    {
        return displayQuantization;
    }

    /**
     * Set how chords positions are quantized for display.
     *
     * @param q
     */
    public void setDisplayQuantizationValue(Quantization q)
    {
        displayQuantization = q;
        for (BarRenderer br : getBarRenderers())
        {
            br.setDisplayQuantizationValue(q);
        }
    }

    public int getModelBarIndex()
    {
        return modelBarIndex;
    }

    public int getBarIndex()
    {
        return barIndex;
    }

    /**
     * Set the barIndex of this BarBox.
     * <p>
     *
     * @param bar A zero or positive value
     */
    public void setBarIndex(int bar)
    {
        if (bar == barIndex)
        {
            return;
        }
        barIndex = bar;
        // Update our border title
        TitledBorder tb = (TitledBorder) getBorder();
        if (tb != null)
        {
            tb.setTitle((bar >= 0) ? String.valueOf(bar + 1) : "");
            repaint();
        }
        for (BarRenderer br : getBarRenderers())
        {
            br.setBarIndex(barIndex);
        }
    }

    /**
     * Set the barIndex within the model. Forward to BarRenderers.
     *
     * @param bar If &lt; 0, it means information from model is not available (for example because the barIndex is past the end of
     * the model.)
     * @throws IllegalArgumentException If bar is &gt; or equals to model's size.
     */
    public void setModelBarIndex(int bar)
    {
        if (bar >= model.getSize())
        {
            throw new IllegalArgumentException("bar=" + bar);
        }

        if (bar == modelBarIndex)
        {
            return;
        }

        modelBarIndex = bar;

        for (BarRenderer br : getBarRenderers())
        {
            br.setModelBarIndex(modelBarIndex);
        }

        refreshBackground();
    }

    /**
     * Return the position (bar, beat) which corresponds to a given point in the BarBox.
     *
     * @param barboxPoint A point in the BarBox coordinates.
     * @return Null if point does not correspond to a valid bar.
     */
    public Position getPositionFromPoint(Point barboxPoint)
    {
        if (modelBarIndex == -1)
        {
            return null;
        }
        // Find a BeatBasedBarRenderer able to provide an accurate position
        for (BarRenderer br : getBarRenderers())
        {
            if (br instanceof BeatBasedBarRenderer)
            {
                Point point = SwingUtilities.convertPoint(this, barboxPoint, br);
                return ((BeatBasedBarRenderer) br).getPositionFromPoint(point.x);
            }
        }
        // Return raw position
        return new Position(modelBarIndex, 0);
    }

    /**
     * Request BarRenderers to update after an item has moved within the bar.
     *
     * @param item
     */
    public void moveItem(ChordLeadSheetItem<?> item)
    {
        for (BarRenderer br : getBarRenderers())
        {
            if (br.isRegisteredItemClass(item))
            {
                br.moveItemRenderer(item);
            }
        }
    }

    /**
     * Request BarRenderers to update if the section they belong to has changed.
     *
     * @param section
     */
    public void setSection(CLI_Section section)
    {
        for (BarRenderer br : getBarRenderers())
        {
            br.setSection(section);
        }
    }

    public BarBoxConfig getConfig()
    {
        return barBoxConfig;
    }

    /**
     * Set the BarBoxConfig of this BarBox.
     * <p>
     * There must be at least 1 active BarRenderer.
     *
     * @param cfg
     * @return boolean true if BarBoxConfig has been really changed, false otherwise (e.g. same value)
     */
    public final boolean setConfig(BarBoxConfig cfg)
    {
        if (cfg == null || cfg.getActiveBarRenderers().isEmpty())
        {
            throw new IllegalArgumentException("cfg=" + cfg);
        }

        if (cfg.equals(barBoxConfig))
        {
            return false;
        }

        if (barBoxConfig != null && cfg.getActiveBarRenderers().equals(barBoxConfig.getActiveBarRenderers()))
        {
            barBoxConfig = cfg;
            // Supported BarRenderers have changed, but not the active ones, we can leave
            return true;
        }

        barBoxConfig = cfg;

        // Remove previous BarRenderers
        for (BarRenderer br : getBarRenderers())
        {
            removeBarRenderer(br);
        }


        // Add new ones
        for (BarRendererFactory.Type brType : barBoxConfig.getActiveBarRenderers())
        {
            BarRenderer br = barRendererFactory.createBarRenderer(editor, brType, barIndex, model, bbSettings.getBarRendererSettings(), barRendererFactory.getItemRendererFactory());
            br.setZoomVFactor(zoomVFactor);
            br.setDisplayQuantizationValue(displayQuantization);
            br.setEnabled(isEnabled());
            add(br);
        }

        revalidate(); // Since components have been added/removed
        return true;
    }

    public void showInsertionPoint(boolean b, ChordLeadSheetItem<?> item, Position pos, boolean copyMode)
    {
        for (BarRenderer br : getBarRenderers())
        {
            if (br.isRegisteredItemClass(item))
            {
                br.showInsertionPoint(b, item, pos, copyMode);
            }
        }
    }

    /**
     * Change background to represent the playback point in this bar. Then delegate to BarRenderers to render the point.
     *
     * @param b
     * @param pos
     */
    public void showPlaybackPoint(boolean b, Position pos)
    {
        if (b && pos.getBar() != getBarIndex())
        {
            throw new IllegalArgumentException("b=" + b + " pos=" + pos);
        }
        isPlaybackOn = b;
        refreshBackground();
        for (BarRenderer br : getBarRenderers())
        {
            br.showPlaybackPoint(b, pos);
        }
    }

    /**
     * Clean up everything so this object can be garbaged.
     */
    public void cleanup()
    {
        removeFocusListener(this);
        bbSettings.removePropertyChangeListener(this);
        model = null;
        for (BarRenderer br : getBarRenderers())
        {
            removeBarRenderer(br);
        }
    }

    public java.util.List<BarRenderer> getBarRenderers()
    {
        ArrayList<BarRenderer> brs = new ArrayList<>();
        Component[] cs = getComponents();
        for (Component c : cs)
        {
            if (c instanceof BarRenderer)
            {
                brs.add((BarRenderer) c);
            }
        }
        return brs;
    }

    @Override
    public String toString()
    {
        return "BarBox " + barIndex;
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() == bbSettings)
        {
            refreshBackground();        // If a color has changed
            repaint();                  // If a border has changed
        }
    }

    // ---------------------------------------------------------------
    // Implements the FocusListener interface
    // ---------------------------------------------------------------
    @Override
    public void focusGained(FocusEvent e)
    {
        Border border = bbSettings.getFocusedTitledBorder((barIndex >= 0) ? String.valueOf(barIndex + 1) : "");
        if (border != null)
        {
            setBorder(border);
        }
    }

    @Override
    public void focusLost(FocusEvent e)
    {
        Border border = bbSettings.getTitledBorder((barIndex >= 0) ? String.valueOf(barIndex + 1) : "");
        if (border != null)
        {
            setBorder(border);
        }
    }

    // ---------------------------------------------------------------
    // Private functions
    // ---------------------------------------------------------------
    private void removeBarRenderer(BarRenderer br)
    {
        br.cleanup();
        remove(br);
    }

    /**
     * Set the background according to : selected & enabled status, before/after end of the leadsheet.
     */
    private void refreshBackground()
    {
        if (isPlaybackOn)
        {
            setBackground(bbSettings.getPlaybackColor());
            return;
        }
        if (!isEnabled())
        {
            if (modelBarIndex < 0 || modelBarIndex >= model.getSize())
            {
                setBackground(bbSettings.getDisabledPastEndColor());
            } else
            {
                setBackground(bbSettings.getDisabledColor());
            }
        } else if (modelBarIndex < 0 || modelBarIndex >= model.getSize())
        {
            setBackground(isSelected ? bbSettings.getPastEndSelectedColor() : bbSettings.getPastEndColor());
        } else
        {
            setBackground(isSelected ? bbSettings.getSelectedColor() : bbSettings.getDefaultColor());
        }
    }

}
