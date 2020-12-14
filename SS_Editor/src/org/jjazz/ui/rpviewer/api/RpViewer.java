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
package org.jjazz.ui.rpviewer.api;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import static javax.swing.SwingConstants.CENTER;
import javax.swing.border.Border;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.utilities.RedispatchingMouseAdapter;
import org.jjazz.songstructure.api.SongPart;

/**
 * A base class for RpViewers.
 * <p>
 * Display RP name and manage selected/focused status using background and border.
 */
public abstract class RpViewer extends JPanel implements PropertyChangeListener, FocusListener
{

    // UI variables
    private JLabel lbl_RpName;

    // Application variables
    private RhythmParameter<?> rpModel;
    private SongPart sptModel;
    private RpViewerSettings settings;
    private boolean isSelected = false;
    private boolean showName = true;
    /**
     * The vertical zoom factor, zoom min=0, zoom max=100.
     */
    private int zoomVFactor;

    private static final Logger LOGGER = Logger.getLogger(RpViewer.class.getSimpleName());

    public RpViewer(SongPart spt, RhythmParameter<?> rp, RpViewerSettings settings)
    {
        if (rp == null || spt == null || settings == null)
        {
            throw new NullPointerException("spt=" + spt + " rp=" + rp + " settings=" + settings);   //NOI18N
        }
        this.rpModel = rp;
        this.sptModel = spt;
        this.settings = settings;

        // Register graphical settings changes
        this.settings.addPropertyChangeListener(this);


        // Listen to rp changes
        sptModel.addPropertyChangeListener(this);


        // Listen to rhythm resource load event, as it might impact our tooltip
        sptModel.getRhythm().addPropertyChangeListener(this);


        // Update graphics depending on focus state
        addFocusListener(this);


        // Standard zoom factor by default
        zoomVFactor = 50;

        initUIComponents();
        updateUIComponents();
    }

    /**
     * Vertical zoom factor.
     * <p>
     * Default implementation only updates the internal variable and calls revalidate() and repaint().
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    public void setZoomVFactor(int factor)
    {
        if (factor < 0 || factor > 100)
        {
            throw new IllegalArgumentException("factor=" + factor);   //NOI18N
        }
        if (factor != zoomVFactor)
        {
            zoomVFactor = factor;
            revalidate();
            repaint();
        }
    }

    /**
     * A value between 0 (bird's view) and 100 (max zoom).
     * <p>
     * Default is 50.
     *
     * @return
     */
    public int getZoomVFactor()
    {
        return zoomVFactor;
    }

    /**
     * Clean up everything so this object can be garbaged.
     */
    public void cleanup()
    {
        settings.removePropertyChangeListener(this);
        sptModel.removePropertyChangeListener(this);
        sptModel.getRhythm().removePropertyChangeListener(this);
        removeFocusListener(this);
    }

    public RhythmParameter<?> getRpModel()
    {
        return rpModel;
    }

    public SongPart getSptModel()
    {
        return sptModel;
    }

    public void setSelected(boolean b)
    {
        isSelected = b;
        LOGGER.log(Level.FINE, "setSelected this=" + this + " b=" + b);   //NOI18N
        if (isSelected)
        {
            setBackground(settings.getSelectedBackgroundColor());
        } else
        {
            setBackground(settings.getDefaultBackgroundColor());
        }
    }

    public boolean isSelected()
    {
        return isSelected;
    }

    public void showRpName(boolean b)
    {
        if (b == showName)
        {
            return;
        }
        showName = b;
        if (showName)
        {
            add(lbl_RpName, BorderLayout.NORTH);
        } else
        {
            remove(this.lbl_RpName);
        }
    }

    /**
     * The position of the RP name label.
     *
     * @return
     */
    protected Rectangle getRpNameBounds()
    {
        return lbl_RpName.getBounds();
    }

    /**
     * Called when RhythmParameter value has changed.
     */
    abstract protected void valueChanged();

    // ---------------------------------------------------------------
    // Implements the PropertyChangeListener interface
    // ---------------------------------------------------------------    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == settings)
        {
            updateUIComponents();
        } else if (evt.getSource() == sptModel)
        {
            if (evt.getPropertyName().equals(SongPart.PROPERTY_RP_VALUE))
            {
                if (evt.getOldValue() == rpModel)
                {
                    updateToolTip();
                    valueChanged();
                }
            }
        } else if (evt.getSource() == sptModel.getRhythm())
        {
            if (evt.getPropertyName().equals(Rhythm.PROP_RESOURCES_LOADED))
            {
                updateToolTip();
            }
        }
    }

    // ---------------------------------------------------------------
    // Implements the FocusListener interface
    // ---------------------------------------------------------------
    @Override
    public void focusGained(FocusEvent e)
    {
        Border border = settings.getFocusedBorder();
        if (border != null)
        {
            setBorder(border);
        }
    }

    @Override
    public void focusLost(FocusEvent e)
    {
        Border border = settings.getNonFocusedBorder();
        if (border != null)
        {
            setBorder(border);
        }
    }

    // ---------------------------------------------------------------
    // Private functions
    // ---------------------------------------------------------------    
    private void updateToolTip()
    {
        Object value = sptModel.getRPValue(rpModel);
        String tt = rpModel.getDisplayName() + "=" + value.toString();
        @SuppressWarnings("rawtypes")
        RhythmParameter rp = getRpModel();      // Needed to get rid of the unbounded wildcard <?>
        @SuppressWarnings("unchecked")
        String valueDesc = rp.getValueDescription(value);
        if (valueDesc != null)
        {
            tt += ", " + valueDesc;
        }
        setToolTipText(tt);
    }

    private void initUIComponents()
    {
        setOpaque(true);
        lbl_RpName = new JLabel();
        lbl_RpName.addMouseListener(new RedispatchingMouseAdapter(this));
        lbl_RpName.setText(rpModel.getDisplayName().toLowerCase());
        lbl_RpName.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        lbl_RpName.setToolTipText(rpModel.getDescription());
        lbl_RpName.setHorizontalAlignment(CENTER);
        lbl_RpName.setOpaque(true); // but we make it slightly transparent below

        // Put the label on the right
        JPanel pnl_Top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 1));
        pnl_Top.setOpaque(false);
        pnl_Top.add(lbl_RpName);
        setLayout(new BorderLayout());
        add(pnl_Top, BorderLayout.NORTH);

        // setPreferredSize(DEFAULT_PREFERRED_SIZE);
    }

    private void updateUIComponents()
    {
        lbl_RpName.setFont(settings.getNameFont());
        lbl_RpName.setForeground(settings.getNameFontColor());
        Color c = settings.getDefaultBackgroundColor();
        lbl_RpName.setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue(), 110)); // a bit Transparent


        if (hasFocus())
        {
            setBorder(settings.getFocusedBorder());
        } else
        {
            setBorder(settings.getNonFocusedBorder());
        }
        if (isSelected())
        {
            setBackground(settings.getSelectedBackgroundColor());
        } else
        {
            setBackground(settings.getDefaultBackgroundColor());
        }
        updateToolTip();
    }

    @Override
    public String toString()
    {
        return "RpViewer[spt=" + getSptModel().getName() + " rp=" + getRpModel().getDisplayName() + "]";
    }

}
