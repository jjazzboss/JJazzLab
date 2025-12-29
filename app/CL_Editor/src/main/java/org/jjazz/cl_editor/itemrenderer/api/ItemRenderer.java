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
package org.jjazz.cl_editor.itemrenderer.api;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.*;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * The base class for ItemRenderers.
 * <p>
 * Subclasses should typically implement the paintComponent() function to render the item. The class listens to the item model's changes and call the
 * modelChanged() and/or modelMoved() functions that should be implemented by subclasses.
 */
public abstract class ItemRenderer extends JPanel implements PropertyChangeListener, FocusListener
{
    // GUI variables

    private static final Dimension MIN_SIZE = new Dimension(2, 2);
    /**
     * The graphics settings for this object.
     */
    private ItemRendererSettings settings;
    // APPLICATION variables
    /**
     * The item to render.
     */
    private ChordLeadSheetItem<?> modelItem = null;
    /**
     * True if ItemRenderer is selected.
     */
    private boolean isSelected;
    /**
     * Font color when enabled.
     */
    private Color enabledFontColor;
    /**
     * The type of the itemrenderer.
     */
    private IR_Type irType;
    private Timer timer;            // For requestAttention()
    private Color saveBackground;
    private static final Logger LOGGER = Logger.getLogger(ItemRenderer.class.getName());

    @SuppressWarnings(
            {
                "LeakingThisInConstructor"
            })
    public ItemRenderer(ChordLeadSheetItem<?> item, IR_Type irType)
    {
        super();
        Objects.requireNonNull(item);
        Objects.requireNonNull(irType);
        this.irType = irType;
        this.modelItem = item;
        modelItem.addPropertyChangeListener(this);        

        
        // Disable focus keys on ItemRenderer : must be managed at a higher level
        setFocusTraversalKeysEnabled(false);
        // Register focus events
        addFocusListener(this);
        // Since JPanel does not support drag-and-drop, we add a mouse listener ourselves 
        // to start drag if a transfer handler is set
        addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                TransferHandler th = getTransferHandler();
                if (th != null)
                {
                    th.exportAsDrag(ItemRenderer.this, e, TransferHandler.COPY);
                }
            }
        });

        // Register settings changes
        settings = ItemRendererSettings.getDefault();
        settings.addPropertyChangeListener(this);
        setBorder(settings.getNonFocusedBorder());
        setMinimumSize(MIN_SIZE);
        setBackground(settings.getSelectedBackgroundColor());
        setEnabled(true);
        setOpaque(false);

    }

    public IR_Type getIR_Type()
    {
        return irType;
    }

    public void setModel(ChordLeadSheetItem<?> item)
    {
        Preconditions.checkNotNull(item);
        if (modelItem == item)
        {
            return;
        }
        if (modelItem != null)
        {
            modelItem.removePropertyChangeListener(this);
        }
        modelItem = item;
        modelItem.addPropertyChangeListener(this);

        modelChanged();
        modelMoved();
    }

    /**
     * Zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    abstract public void setZoomFactor(int factor);

    abstract public int getZoomFactor();

    /**
     * Briefly flash this renderer background to request user attention.
     *
     * @param flashColor
     */
    public void requestAttention(Color flashColor)
    {
        if (timer != null && timer.isRunning())
        {
            timer.stop();
            setBackground(saveBackground);
        } else
        {
            saveBackground = getBackground();
        }

        if (timer == null)
        {
            // Create the timer
            ActionListener al = new ActionListener()
            {
                static final int NB_FLASH = 3;
                int count = NB_FLASH;

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (count % 2 == 1)     // 3 - 1
                    {
                        setBackground(saveBackground);
                    } else
                    {
                        setBackground(flashColor);
                    }
                    count--;
                    if (count == 0)
                    {
                        timer.stop();
                        count = NB_FLASH;
                    }
                }
            };
            timer = new Timer(100, al);
        }

        setBackground(flashColor);

        timer.restart();
    }

    /**
     * Called when the item model's data has changed.
     */
    protected abstract void modelChanged();

    /**
     * Called when the item model's position has changed.
     */
    protected abstract void modelMoved();

    /**
     * Clean up everything so this object can be garbaged.
     */
    public void cleanup()
    {
        setSelected(false);
        settings.removePropertyChangeListener(this);
        removeFocusListener(this);
        modelItem.removePropertyChangeListener(this);
    }

    public ChordLeadSheetItem<?> getModel()
    {
        return modelItem;
    }

    @Override
    public String toString()
    {
        return "IR" + "[" + getModel().toString() + "]";
    }

    public boolean isSelected()
    {
        return isSelected;
    }

    /**
     * Set the Component selected or not.
     *
     * @param b Select the Component if b is true.
     */
    public final void setSelected(boolean b)
    {
        if (b != isSelected)
        {
            isSelected = b;
            setOpaque(b);
            // setOpaque does not trigger a repaint(), so must be done manually...
            repaint();
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertyChangeListener interface
    //-----------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() == modelItem)
        {
            switch (e.getPropertyName())
            {
                case ChordLeadSheetItem.PROP_ITEM_DATA:
                    modelChanged();
                    break;
                case ChordLeadSheetItem.PROP_ITEM_POSITION:
                    modelMoved();
                    break;
            }
        } else if (e.getSource() == settings)
        {
            if (e.getPropertyName().equals(ItemRendererSettings.PROP_ITEM_SELECTED_COLOR))
            {
                setBackground(settings.getSelectedBackgroundColor());
            } else if (e.getPropertyName().equals(ItemRendererSettings.PROP_ITEM_FOCUSED_BORDER_COLOR))
            {
                // Refresh border
                if (isFocusOwner())
                {
                    focusGained(null);
                } else
                {
                    focusLost(null);
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the FocusListener interface
    //-----------------------------------------------------------------------
    @Override
    public void focusGained(FocusEvent e)
    {
        setBorder(settings.getFocusedBorder());
    }

    @Override
    public final void focusLost(FocusEvent e)
    {
        setBorder(settings.getNonFocusedBorder());
    }
    //-----------------------------------------------------------------------
    // Private functions
    //-----------------------------------------------------------------------
}
