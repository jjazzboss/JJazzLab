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
package org.jjazz.cl_editor.barrenderer.api;

import com.google.common.base.Preconditions;
import org.jjazz.cl_editor.spi.BarRendererSettings;
import java.awt.*;
import java.beans.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.Border;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererFactory;

/**
 * Base class for BarRenderer.
 * <p>
 * A BarRenderer is a container for ItemRenderers. A BarRenderer has a barIndex and a modelBarIndex (the barIndex within the model). Both values are equal when
 * showing bars within the model, but if showing a bar past the end of the model, modelBarIndex = -1.
 */
abstract public class BarRenderer extends JPanel implements PropertyChangeListener
{

    /**
     * Store the font rendering hidden dialogs instances per editor.
     */
    private static final Map<Integer, JDialog> mapEditorFontMetricsDialog = new HashMap<>();


    // GUI settings
    private static final int BARWIDTH_REDUCTION = 2;
    /**
     * Graphical settings.
     */
    private final CL_Editor editor;
    private final BarRendererSettings settings;
    // APPLICATION variables
    /**
     * The bar index.
     */
    private int barIndex;
    /**
     * Our model.
     */
    private ChordLeadSheet model;
    /**
     * The bar index within the model.
     */
    private int modelBarIndex;
    /**
     * Save the selected state.
     */
    private boolean isSelected;
    private ItemRendererFactory itemFactory;
    private static final Logger LOGGER = Logger.getLogger(BarRenderer.class.getSimpleName());

    /**
     * Construct a BarRenderer.
     *
     * @param editor   Can be null
     * @param barIndex The barIndex of this BarRenderer.
     * @param settings
     * @param irf
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public BarRenderer(CL_Editor editor, int barIndex, BarRendererSettings settings, ItemRendererFactory irf)
    {
        Objects.requireNonNull(settings);
        Objects.requireNonNull(irf);
        Preconditions.checkArgument(barIndex >= 0, "barIndex=%s", barIndex);

        this.editor = editor;
        this.barIndex = barIndex;

        // Register settings changes
        this.settings = settings;
        settings.addPropertyChangeListener(this);

        itemFactory = irf;

        // Graphical setup
        Border border = settings.getDefaultBorder();
        if (border != null)
        {
            setBorder(border);
        }

        // Be transparent, background color will be managed by enclosing BarBox
        setOpaque(false);
    }

    /**
     * @return True, revalidating a BarRenderer should not be propagated to the BarBox container.
     */
//   @Override
//   public boolean isValidateRoot()
//   {
//      return true;
//   }
    /**
     * Reset the ChordLeadSheet and bar index models for this BarRenderer.
     * <p>
     *
     * @param modelBarIndex If &lt; 0, it means this BarRenderer does not represent a valid bar for model
     * @param clsModel
     */
    public void resetModel(ChordLeadSheet clsModel, int modelBarIndex)
    {
        Objects.requireNonNull(clsModel);

        model = clsModel;
        this.modelBarIndex = -9786561;  // Make sure that setModelBarIndex() will reset its ItemRenderers
        setModelBarIndex(modelBarIndex);
    }

    public ChordLeadSheet getModel()
    {
        return model;
    }

    public BarRendererSettings getSettings()
    {
        return settings;
    }

    /**
     * Get the current CLI_Section for this BarRenderer.
     *
     * @return Can be null if getModelBarIndex() == -1. The CLI_Section can be on this bar or a previous one.
     */
    public CLI_Section getCLI_Section()
    {
        var mbi = getModelBarIndex();
        return mbi != -1 ? getModel().getSection(mbi) : null;
    }

    public ItemRendererFactory getItemRendererFactory()
    {
        return itemFactory;
    }

    /**
     * Get all child components which are ItemRenderers.
     *
     * @return
     */
    public java.util.List<ItemRenderer> getItemRenderers()
    {
        ArrayList<ItemRenderer> irs = new ArrayList<>();
        for (Component c : getComponents())
        {
            if (c instanceof ItemRenderer)
            {
                irs.add((ItemRenderer) c);
            }
        }
        return irs;
    }

    /**
     * Add one ItemRenderer for item e.
     *
     * @param item
     * @return ItemRenderer The object that has been created and added.
     */
    public ItemRenderer addItemRenderer(ChordLeadSheetItem<?> item)
    {
        Preconditions.checkArgument(isRegisteredItemClass(item), "item=%s", item);

        ItemRenderer ir = createItemRenderer(item);
        ir.setZoomFactor(getZoomVFactor());

        // Transmit the enabled state
        ir.setEnabled(isEnabled());

        // Add it as a children of this BarRenderer
        add(ir);

        revalidate();

        return ir;
    }

    /**
     * Remove an ItemRenderer from this bar.
     *
     * @param item
     * @return ItemRenderer The ItemRenderer that has been removed, or null if ItemRenderer was not found.
     */
    public ItemRenderer removeItemRenderer(ChordLeadSheetItem<?> item)
    {
        ItemRenderer ir = getItemRenderer(item);
        if (ir == null)
        {
            return null;
        }
        removeItemRenderer(ir);
        return ir;
    }

    public void selectItem(ChordLeadSheetItem<?> item, boolean b)
    {
        ItemRenderer ir = getItemRenderer(item);
        if (ir == null)
        {
            throw new IllegalArgumentException("this=" + this + " item=" + item + " b=" + b);
        }
        ir.setSelected(b);
    }

    /**
     * Set the Component selected or not.
     *
     * @param b Select the Component if b is true.
     */
    public final void setSelected(boolean b)
    {
        isSelected = b;
    }

    public final boolean isSelected()
    {
        return isSelected;
    }

    @Override
    public final void setEnabled(boolean b)
    {
        super.setEnabled(b);
        for (ItemRenderer er : getItemRenderers())
        {
            er.setEnabled(b);
        }
    }

    /**
     * Vertical zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    public abstract void setZoomVFactor(int factor);

    public abstract int getZoomVFactor();

    /**
     * Set the barIndex of this BarRenderer.
     * <p>
     * Some BarRenderer might use this information when rendering the bar.
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
    }

    /**
     * Set the barIndex within the model.
     * <p>
     * All items shown in this BarRenderer will belong to this bar. Method does nothing if bar == current model bar index.
     *
     * @param bar If &lt; 0, it means information from model is not available (for example because the barIndex is past the end of the leadsheet.)
     * @return The previous modelBarIndex value
     * @throws IllegalArgumentException If bar is &gt; or equals to model's size.
     */
    public int setModelBarIndex(int bar)
    {
        if (bar >= model.getSizeInBars())
        {
            throw new IllegalArgumentException("this=" + this + " bar=" + bar);
        }

        if (bar == modelBarIndex)
        {
            return bar;
        }

        int old = modelBarIndex;
        modelBarIndex = bar;

        // Remove all item renderers
        for (ItemRenderer ir : getItemRenderers())
        {
            removeItemRenderer(ir);
        }

        LOGGER.log(Level.FINE, "this={0} modelBarIndex={1}", new Object[]
        {
            this, modelBarIndex
        });


        if (modelBarIndex >= 0)
        {
            // Possibly update section            
            setSection(model.getSection(modelBarIndex));
            // Add the required item renderers            
            for (ChordLeadSheetItem<?> item : getRegisteredModelItems())
            {
                LOGGER.log(Level.FINE, "   item={0}", item);
                addItemRenderer(item);
            }
        } else
        {
            setSection(null);
        }

        return old;
    }

    /**
     * Move one item of this bar.
     * <p>
     * The new position is also within this bar.
     *
     * @param item
     */
    abstract public void moveItemRenderer(ChordLeadSheetItem<?> item);

    /**
     * Set the section for this bar.
     *
     * @param section Can be null
     */
    abstract public void setSection(CLI_Section section);

    /**
     * Return True if this item's class is supported by this BarRenderer.
     *
     * @param item
     * @return
     */
    abstract public boolean isRegisteredItemClass(ChordLeadSheetItem<?> item);

    /**
     * Get an ItemRenderer instance for a specific item.
     *
     * @param item Item
     * @return
     */
    abstract protected ItemRenderer createItemRenderer(ChordLeadSheetItem<?> item);

    /**
     * Show or hide an insertion point for the specified item.
     *
     * @param showIP   Show if true, hide if false.
     * @param item
     * @param pos      The position of the insertion point
     * @param copyMode
     */
    abstract public void showInsertionPoint(boolean showIP, ChordLeadSheetItem<?> item, Position pos, boolean copyMode);

    /**
     * Show or hide the playback point at specified position.
     *
     * @param b   Show if true, hide if false.
     * @param pos
     */
    abstract public void showPlaybackPoint(boolean b, Position pos);

    /**
     * Clean up everything so this object can be garbaged.
     * <p>
     * Model is set to null.
     */
    public void cleanup()
    {
        setSelected(false);
        settings.removePropertyChangeListener(this);
        model = null;

        // Remove all item renderers
        for (ItemRenderer ir : getItemRenderers())
        {
            removeItemRenderer(ir);
        }

        // Remove only if it's the last bar of the editor
        if (getEditor().getNbBarBoxes() == 1)
        {
            mapEditorFontMetricsDialog.remove(System.identityHashCode(getEditor()));
        }
    }

    /**
     * Get the ItemRenderer of the specified item.
     *
     * @param item
     * @return Can be null.
     */
    public ItemRenderer getItemRenderer(ChordLeadSheetItem<?> item)
    {
        for (ItemRenderer ir : getItemRenderers())
        {
            if (ir.getModel() == item)
            {
                return ir;
            }
        }
        return null;
    }

    public int getBarIndex()
    {
        return barIndex;
    }

    /**
     * The bar index in the chordleadsheet model.
     *
     * @return -1 if we're past the end of chord leadsheet.
     */
    public int getModelBarIndex()
    {
        return modelBarIndex;
    }

    /**
     * Get the available drawing area of the Bar.
     * <p>
     * <p>
     * This implementation returns an area corresponding to getInsets() except the width is slightly reduced.
     *
     * @return A Rectangle object representing the available drawing area.
     */
    public Rectangle getDrawingArea()
    {
        Insets in = getInsets();

        return new Rectangle(in.left + BARWIDTH_REDUCTION, in.top,
                getWidth() - in.left - in.right - (2 * BARWIDTH_REDUCTION),
                getHeight() - in.top - in.bottom);
    }

    /**
     * The editor to which this BarRenderer belongs to.
     *
     * @return Can be null
     */
    public CL_Editor getEditor()
    {
        return editor;
    }


    @Override
    public String toString()
    {
        return "BR[" + getBarIndex() + "]";
    }


    /**
     * Return a editor-level shared instance of a hidden JDialog used to get dimensions of Font-based objects.
     * <p>
     *
     * @param editor
     * @return
     */
    static public JDialog getFontMetricsDialog(CL_Editor editor)
    {
        JDialog dlg = mapEditorFontMetricsDialog.get(System.identityHashCode(editor));
        if (dlg == null)
        {
            dlg = new JDialog();
            dlg.getContentPane().setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
            mapEditorFontMetricsDialog.put(System.identityHashCode(editor), dlg);
        }
        return dlg;
    }


    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() == settings)
        {
            repaint();                  // If a border has changed
        }
    }

    //------------------------------------------------------------------
    // Private functions
    //------------------------------------------------------------------
    /**
     * Remove the specified ItemRenderer.
     *
     * @param ir
     */
    protected void removeItemRenderer(ItemRenderer ir)
    {
        ir.cleanup();
        remove(ir);
        revalidate();
        // Repaint background behind removed ItemRenderer
        repaint();
    }

    /**
     * @return The List of the model items registered by this BarRenderer. List will be empty if modelBarIndex is &lt; 0.
     */
    private java.util.List<ChordLeadSheetItem> getRegisteredModelItems()
    {
        // Get registeredEvents from the LeadSheet
        java.util.List<ChordLeadSheetItem> registeredItems = new ArrayList<>();

        if (modelBarIndex >= 0)
        {
            for (var item : model.getItems(modelBarIndex, modelBarIndex, ChordLeadSheetItem.class))
            {
                if (isRegisteredItemClass(item))
                {
                    registeredItems.add(item);
                }
            }
        }
        return registeredItems;
    }

}
