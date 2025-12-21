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
package org.jjazz.ss_editor.rpviewer.api;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.LayerUI;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.flatcomponents.api.FlatButton;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.song.api.Song;
import org.jjazz.ss_editor.api.SS_EditorClientProperties;
import org.jjazz.ss_editor.api.SS_EditorClientProperties.ViewMode;
import org.jjazz.uiutilities.api.RedispatchingMouseAdapter;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.ss_editor.rpviewer.spi.RpCustomEditorFactory;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * A RhythmParameter viewer.
 * <p>
 * - Display RP name<br>
 * - Manage selected/focused status using background and border<br>
 * When mouse is over component:<br>
 * - Show an edit button if RhythmParameter model supports a custom edit dialog<br>
 * - Show the hide button<br>
 * <p>
 * The actual rendering and preferred size setting is delegated to a RpViewerRenderer.
 */
public class RpViewer extends JPanel implements PropertyChangeListener, FocusListener
{

    @StaticResource(relative = true)
    private static final String BTN_EDIT_RESOURCE_PATH = "resources/OpenPopup.png";
    @StaticResource(relative = true)
    private static final String BTN_HIDE_RESOURCE_PATH = "resources/HideBtn.png";
    // UI variables   
    private JLayer<RenderingPanel> layer;
    private RenderingPanel renderingPanel;
    private JLabel lbl_RpName;
    private FlatButton fbtn_edit;
    private FlatButton fbtn_hide;

    // Application variables
    private RpViewerRenderer renderer;
    private RhythmParameter<?> rpModel;
    private SongPart sptModel;
    private RpViewerSettings settings;
    private boolean isSelected = false;
    private RpViewerController controller;
    /**
     * The vertical zoom factor, zoom min=0, zoom max=100.
     */
    private int zoomVFactor;

    private static final Logger LOGGER = Logger.getLogger(RpViewer.class.getSimpleName());

    /**
     *
     * @param spt
     * @param rp
     * @param settings
     * @param renderer
     */
    public RpViewer(SongPart spt, RhythmParameter<?> rp, RpViewerSettings settings, RpViewerRenderer renderer)
    {
        if (rp == null || spt == null || settings == null || renderer == null)
        {
            throw new NullPointerException("controller=" + controller + " spt=" + spt + " rp=" + rp + " settings=" + settings + " renderer=" + renderer);
        }
        this.rpModel = rp;
        this.sptModel = spt;
        this.settings = settings;

        this.renderer = renderer;
        this.renderer.addChangeListener(e -> 
        {
            // Renderer configuration has changed
            renderingPanel.revalidate();
            renderingPanel.repaint();
        });


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

    public void setController(RpViewerController controller)
    {
        this.controller = controller;

        if (renderer instanceof RpViewerEditableRenderer)
        {
            ((RpViewerEditableRenderer) renderer).setController(controller);
        }
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
            throw new IllegalArgumentException("factor=" + factor);
        }
        if (factor != zoomVFactor)
        {
            zoomVFactor = factor;
            renderingPanel.revalidate();
            renderingPanel.repaint();
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
        controller = null;
        removeFocusListener(this);
    }

    public RhythmParameter<?> getRpModel()
    {
        return rpModel;
    }

    public RpViewerRenderer getRenderer()
    {
        return renderer;
    }

    public SongPart getSptModel()
    {
        return sptModel;
    }

    public void setSelected(boolean b)
    {
        isSelected = b;
        LOGGER.log(Level.FINE, "setSelected this={0} b={1}", new Object[]
        {
            this, b
        });
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

    public boolean isRpNameVisible()
    {
        return lbl_RpName.isShowing();
    }

    public void setRpNameVisible(boolean b)
    {
        if (b && !lbl_RpName.isShowing())
        {
            renderingPanel.add(lbl_RpName, RpViewerLayoutManager.NORTH_EAST);
            renderingPanel.revalidate();
            renderingPanel.repaint();
        } else if (!b && lbl_RpName.isShowing())
        {
            renderingPanel.remove(lbl_RpName);
            renderingPanel.revalidate();
            renderingPanel.repaint();
        }
    }

    @Override
    public String toString()
    {
        return "RpViewer[spt=" + getSptModel().getName() + " rp=" + getRpModel().getDisplayName() + " renderer=" + renderer + "]";
    }

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
            if (evt.getPropertyName().equals(SongPart.PROP_RP_VALUE))
            {
                if (evt.getOldValue() == rpModel)
                {
                    updateToolTip();
                    layer.repaint();
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
    /**
     * The position of the RP name label.
     *
     * @return
     */
    protected Rectangle getRpNameBounds()
    {
        return lbl_RpName.getBounds();
    }

    private void updateToolTip()
    {
        Object value = sptModel.getRPValue(rpModel);
        String strValue = value.toString();
        String tt = rpModel.getDisplayName() + "=" + strValue;
        @SuppressWarnings("rawtypes")
        RhythmParameter rp = getRpModel();      // Needed to get rid of the unbounded wildcard <?>
        @SuppressWarnings("unchecked")
        String valueDesc = rp.getValueDescription(value);
        if (valueDesc != null && !valueDesc.equals(strValue))
        {
            tt += ", " + valueDesc;
        }
        renderingPanel.setToolTipText(tt);
    }

    private void initUIComponents()
    {

        var mouseRedispatcherToThis = new RedispatchingMouseAdapter(this);

        // RhythmParameter name
        lbl_RpName = new JLabel();
        lbl_RpName.addMouseListener(mouseRedispatcherToThis);
        lbl_RpName.addMouseMotionListener(mouseRedispatcherToThis);
        lbl_RpName.addMouseWheelListener(mouseRedispatcherToThis);
        lbl_RpName.setText(rpModel.getDisplayName().toLowerCase());
        lbl_RpName.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        lbl_RpName.setToolTipText(rpModel.getDescription());
        lbl_RpName.setHorizontalAlignment(SwingConstants.CENTER);
        lbl_RpName.setOpaque(true); // but we make it slightly transparent see updateUIComponents()


        // Optional edit button
        fbtn_edit = new FlatButton();
        fbtn_edit.setIcon(new javax.swing.ImageIcon(getClass().getResource(BTN_EDIT_RESOURCE_PATH))); // NOI18N
        fbtn_edit.addActionListener(e -> showRpCustomEditDialog());
        fbtn_edit.setBorderEntered(BorderFactory.createLineBorder(Color.GRAY));
        fbtn_edit.setToolTipText(ResUtil.getString(getClass(), "TooltipRpEditCustomDialog"));

        // Hide button
        fbtn_hide = new FlatButton();
        fbtn_hide.setIcon(new javax.swing.ImageIcon(getClass().getResource(BTN_HIDE_RESOURCE_PATH))); // NOI18N
        fbtn_hide.addActionListener(e -> hideRpViewer());
        fbtn_hide.setBorderEntered(BorderFactory.createLineBorder(Color.GRAY));
        fbtn_hide.setToolTipText(ResUtil.getString(getClass(), "TooltipHideRp"));


        // Our custom layout 
        renderingPanel = new RenderingPanel();
        renderingPanel.setOpaque(false);
        // To let external classes listening to MouseEvents on RpViewer

        renderingPanel.addMouseListener(mouseRedispatcherToThis);
        renderingPanel.addMouseMotionListener(mouseRedispatcherToThis);
        renderingPanel.addMouseWheelListener(mouseRedispatcherToThis);
        if (renderer instanceof RpViewerEditableRenderer)
        {
            renderingPanel.addMouseListener((RpViewerEditableRenderer) renderer);
            renderingPanel.addMouseMotionListener((RpViewerEditableRenderer) renderer);
            renderingPanel.addMouseWheelListener((RpViewerEditableRenderer) renderer);
        }
        renderingPanel.setLayout(new RpViewerLayoutManager());
        renderingPanel.add(lbl_RpName, RpViewerLayoutManager.NORTH_EAST);


        // We use a JLayer because when using a simple MouseListener to listen to mouseEntered/mouseExited events (to show/hide edit button), sometimes
        // when moving the mouse fast across RpViewers, we miss an exit event and end up with the edit button not correctly removed.
        // Don't know really why, but this problem never happens when using the JLayer.        
        MyLayerUI layerUI = new MyLayerUI();
        layer = new JLayer<>(renderingPanel, layerUI);
        setLayout(new BorderLayout());
        add(layer, BorderLayout.CENTER);
    }

    private void setButtonsVisible(boolean showEditButton, boolean showHideButton)
    {
        boolean revalidate = false;

        if (showEditButton && !fbtn_edit.isShowing())
        {
            renderingPanel.add(fbtn_edit, RpViewerLayoutManager.NORTH_WEST);
            revalidate = true;
        } else if (!showEditButton && fbtn_edit.isShowing())
        {
            renderingPanel.remove(fbtn_edit);
            revalidate = true;
        }

        if (showHideButton && !fbtn_hide.isShowing())
        {
            renderingPanel.add(fbtn_hide, RpViewerLayoutManager.NORTH_WEST);
            revalidate = true;
        } else if (!showHideButton && fbtn_hide.isShowing())
        {
            renderingPanel.remove(fbtn_hide);
            revalidate = true;
        }

        if (revalidate)
        {
            renderingPanel.revalidate();
            renderingPanel.repaint();
        }

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

    private void showRpCustomEditDialog()
    {
        if (controller != null)
        {
            controller.rhythmParameterEditWithCustomDialog(sptModel, rpModel);
        }
    }

    private void hideRpViewer()
    {
        Song song = renderer.getSong();
        Rhythm r = sptModel.getRhythm();
        var rps = SS_EditorClientProperties.getCompactViewModeVisibleRPs(song, r);
        if (rps.remove(rpModel))
        {
            SS_EditorClientProperties.setCompactViewModeVisibleRPs(song, r, rps);
        }
        if (SS_EditorClientProperties.getViewMode(song) != ViewMode.COMPACT)
        {
            SS_EditorClientProperties.setViewMode(song, ViewMode.COMPACT);
        }
    }

    // ---------------------------------------------------------------
    // Private classes
    // ---------------------------------------------------------------        
    private class RenderingPanel extends JPanel
    {

        @Override
        public Dimension getPreferredSize()
        {
            return renderer.getPreferredSize();
        }

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            renderer.paintComponent(g);
        }

    }

    /**
     * Track mouse enter/exit events (avoiding problems with children components) to display buttons while mouse is over viewer.
     */
    private class MyLayerUI extends LayerUI<RenderingPanel>
    {

        private boolean needEditBtn = false;
        private boolean needHideBtn = false;

        @Override
        public void installUI(JComponent c)
        {
            super.installUI(c);
            JLayer jlayer = (JLayer) c;
            jlayer.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK);

            needEditBtn = RpCustomEditorFactory.findFactory(rpModel) != null;
            needHideBtn = !(rpModel instanceof RP_SYS_Variation);
        }

        @Override
        public void uninstallUI(JComponent c)
        {
            JLayer jlayer = (JLayer) c;
            jlayer.setLayerEventMask(0);
            super.uninstallUI(c);
        }

        @Override
        protected void processMouseEvent(MouseEvent e, JLayer layer)
        {
            if (e.getID() == MouseEvent.MOUSE_ENTERED || e.getID() == MouseEvent.MOUSE_EXITED)
            {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), layer);
                boolean inside = layer.contains(p);     // This way it works even if mouse is on another component inside the RpViewer
                setButtonsVisible(needEditBtn && inside, needHideBtn && inside);
            }
        }
    }
}
