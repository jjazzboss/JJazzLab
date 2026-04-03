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
package org.jjazz.ss_editor.sptviewer.api;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerSettings;
import org.jjazz.ss_editor.rpviewer.api.RpViewer;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.uisettings.api.ColorSetManager;

/**
 * A base implementation of a SptViewer.
 * <p>
 * Handle preferred size, zooming, selection/focused state rendering, mouse events capture to call controller.
 */
abstract public class SptViewer extends JPanel implements FocusListener, PropertyChangeListener, MouseListener, MouseMotionListener, MouseWheelListener
{

    private static final int ONE_BAR_EXTRA_SIZE = 60;
    private static final int MIN_WIDTH = 40;

    protected SptViewerMouseListener controller;
    private SongPart sptModel;
    private final SS_Editor editor;
    private boolean isSelected;
    private int zoomHFactor;
    protected int zoomVFactor;
    private Color sptColor;

    /**
     * Our graphical settings.
     */
    protected final SptViewerSettings settings;
    private static final Logger LOGGER = Logger.getLogger(SptViewer.class.getSimpleName());


    /**
     *
     * @param ssEditor
     * @param spt
     * @param settings
     */
    public SptViewer(SS_Editor ssEditor, SongPart spt, SptViewerSettings settings)
    {
        Objects.requireNonNull(ssEditor);
        Objects.requireNonNull(spt);
        Objects.requireNonNull(settings);

        this.editor = ssEditor;
        sptModel = spt;
        sptModel.addPropertyChangeListener(this);
        zoomHFactor = 50;
        zoomVFactor = 50;


        // Register settings changes
        this.settings = settings;
        settings.addPropertyChangeListener(this);


        // Keep track if section colors change
        var cliSection = sptModel.getParentSection();
        sptColor = Color.LIGHT_GRAY.brighter().brighter();
        if (cliSection != null)
        {
            cliSection.addPropertyChangeListener(this);
            cliSection.getClientProperties().addPropertyChangeListener(this);
            sptColor = CL_EditorClientProperties.getSectionColor(sptModel.getParentSection());
            if (sptColor == null)
            {
                sptColor = ColorSetManager.getDefault().getColor(cliSection);
            }
        }


        addFocusListener(this);
        addMouseListener(this);     // We'll also get the MouseEvents from children which has not mouselistener attached (note that just setting a tooltip implicitly adds a mouselistener)
        addMouseMotionListener(this);


        updateGlobalUIComponents();
        updateToolTip();
    }

    public void setController(SptViewerMouseListener controller)
    {
        this.controller = controller;
        getRpViewers().forEach(rpv -> rpv.setController(controller));
    }

    public SptViewerSettings getSettings()
    {
        return settings;
    }

    public SongPart getModel()
    {
        return sptModel;
    }

    public void setSelected(boolean b)
    {
        isSelected = b;
        refreshBackground();
    }

    public void setZoomHFactor(int factor)
    {
        if (factor < 0 || factor > 100)
        {
            throw new IllegalArgumentException("factor=" + factor);
        }
        zoomHFactor = factor;
        revalidate();
        repaint();
    }

    public int getZoomHFactor()
    {
        return zoomHFactor;
    }

    public void setZoomVFactor(int factor)
    {
        if (factor < 0 || factor > 100)
        {
            throw new IllegalArgumentException("factor=" + factor);
        }
        zoomVFactor = factor;

        // Only RpViewers height is impacted
        for (RpViewer rpv : this.getRpViewers())
        {
            rpv.setZoomVFactor(factor);
        }
    }

    public int getZoomVFactor()
    {
        return zoomHFactor;
    }

    /**
     * Overridden to enable SptViewers to be aligned on their baseline (for example by FlowLayout).
     *
     * @param width
     * @param height
     * @return 0 Means these components are aligned on the top.
     */
    @Override
    public int getBaseline(int width, int height)
    {
        return 0;
    }

    /**
     * Overridden to be consistent with getBaseline override
     *
     * @return
     */
    @Override
    public Component.BaselineResizeBehavior getBaselineResizeBehavior()
    {
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    /**
     * PreferredSize is proportional to the SongPart length.
     *
     * @return
     */
    @Override
    public Dimension getPreferredSize()
    {
        int nbBars = (sptModel == null) ? 0 : sptModel.getNbBars();
        float width = MIN_WIDTH + nbBars * ONE_BAR_EXTRA_SIZE * (zoomHFactor / 100f);
        Dimension pd = getLayout().preferredLayoutSize(this);
        return new Dimension((int) width, pd.height);
    }

    /**
     * Select/unselect the specified RhythmParameter.
     *
     * @param rp
     * @param b
     */
    public void setSelected(RhythmParameter<?> rp, boolean b)
    {
        RpViewer rpv = getRpViewer(rp);
        if (rpv != null)
        {
            rpv.setSelected(b);
        }
    }

    /**
     * Set the focus on the rp's RpViewer.
     *
     * @param rp
     */
    public void setFocusOnRpViewer(RhythmParameter<?> rp)
    {
        RpViewer rpv = getRpViewer(rp);
        if (rpv != null)
        {
            rpv.requestFocusInWindow();
        }
    }

    /**
     * Get the specified RpViewer bounds.
     *
     * @param rp
     * @return Can be null if rp is not used by this SongPart.
     */
    public Rectangle getRpViewerRectangle(RhythmParameter<?> rp)
    {
        Rectangle r = null;
        RpViewer rpv = getRpViewer(rp);
        if (rpv != null)
        {
            Point p = rpv.getLocationOnScreen();
            r = new Rectangle(p);
            r.width = rpv.getWidth();
            r.height = rpv.getHeight();
        }
        return r;
    }

    public Color getSptColor()
    {
        return this.sptColor;
    }

    /**
     * Set the default color.
     *
     * @param c
     */
    public void setSptColor(Color c)
    {
        this.sptColor = c;
        refreshBackground();
    }


    public void cleanup()
    {
        var uiConfig = getConfig();
        uiConfig = uiConfig.setVisibleRPs(Collections.emptyList());
        setConfig(uiConfig);
        var cliSection = sptModel.getParentSection();
        if (cliSection != null)
        {
            cliSection.getClientProperties().removePropertyChangeListener(this);
            cliSection.removePropertyChangeListener(this);
        }
        sptModel.removePropertyChangeListener(this);
        settings.removePropertyChangeListener(this);
        removeFocusListener(this);
        sptModel = null;
        controller = null;
    }


    /**
     * Model has changed, UI might need to be updated.
     */
    abstract public void modelChanged();

    /**
     * Settings have changed, UI might need to be updated.
     */
    abstract public void settingsChanged();

    /**
     * Get the UI config of this SptViewer.
     *
     * @return
     */
    abstract public SptViewerConfig getConfig();

    /**
     * Set the UI config of this SptViewer.
     * <p>
     *
     * @param uiConfig
     */
    abstract public void setConfig(SptViewerConfig uiConfig);

    /**
     * Show a playback point in the editor at specified position.
     * <p>
     * Default implementation does nothing.
     *
     * @param show Show/hide the playback point.
     * @param pos  The position within the SongStructure model. Not used if b==false.
     */
    public void showPlaybackPoint(boolean show, Position pos)
    {
        // Nothing
    }

    /**
     * Get the RpViewers used by this SptViewer.
     * <p>
     * Default implementation returns an empty list.
     *
     * @return
     */
    public List<RpViewer> getRpViewers()
    {
        return Collections.emptyList();
    }

    /**
     * Get the RpViewer for the specified RhythmParameter.
     * <p>
     * Default implementation returns an empty list.
     *
     * @param rp
     * @return Can be null
     */
    public RpViewer getRpViewer(RhythmParameter<?> rp)
    {
        return null;
    }

    @Override
    public String toString()
    {
        return "SptViewer(" + sptModel + ")";
    }

    //-----------------------------------------------------------------------
    // Implementation of the MouseListener interface
    //-----------------------------------------------------------------------
    @Override
    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if (controller == null)
        {
            return;
        }
        Component c = (Component) e.getSource();
        LOGGER.log(Level.FINE, "mousePressed() c={0}", c);
        if (c instanceof SptViewer)
        {
            controller.songPartClicked(e, sptModel, false);
        } else if (c instanceof RpViewer rpv)
        {
            controller.rhythmParameterClicked(e, rpv.getSptModel(), rpv.getRpModel());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (controller == null)
        {
            return;
        }
        Component c = (Component) e.getSource();
        if (c == this)
        {
            controller.songPartReleased(e, sptModel);
        } else if (c instanceof RpViewer rpv)
        {
            controller.rhythmParameterReleased(e, rpv.getSptModel(), rpv.getRpModel());
        }
    }

    //------------------------------------------------------------------
    // Implement the MouseMotionListener interface
    //------------------------------------------------------------------
    @Override
    public void mouseDragged(MouseEvent e)
    {
        if (!SwingUtilities.isLeftMouseButton(e) || controller == null)
        {
            return;
        }
        Component c = (Component) e.getSource();
        if (c instanceof RpViewer rpv)
        {
            controller.rhythmParameterDragged(e, rpv.getSptModel(), rpv.getRpModel());
        } else
        {
            // Since JPanel does not normally support drag-and-drop, start drag if a transfer handler is set
            TransferHandler th = getTransferHandler();
            if (th != null)
            {
                th.exportAsDrag(SptViewer.this, e, TransferHandler.COPY);
            }
            controller.songPartDragged(e, sptModel);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        // Nothing
    }

// ---------------------------------------------------------------
// Implements MouseWheelListener interface
// ---------------------------------------------------------------
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if (controller == null)
        {
            return;
        }
        Component c = (Component) e.getSource();
        if (c instanceof RpViewer rpv)
        {
            controller.rhythmParameterWheelMoved(e, rpv.getSptModel(), rpv.getRpModel());
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() == settings)
        {
            settingsChanged();
        } else if (e.getSource() == sptModel)
        {
            updateToolTip();

            switch (e.getPropertyName())
            {
                case SongPart.PROP_NAME ->
                {
                    modelChanged();
                }
                case SongPart.PROP_RHYTHM_PARENT_SECTION ->
                {
                    Rhythm oldRhythm = (Rhythm) e.getOldValue();
                    Rhythm newRhythm = sptModel.getRhythm();
                    if (oldRhythm != newRhythm)
                    {
                        // Rhythm has changed, need to update the RpViewers
                        var uiConfig = getConfig().setVisibleRPs(editor.getVisibleRps(newRhythm));
                        setConfig(uiConfig);
                    }
                    modelChanged();
                }
                case SongPart.PROP_NB_BARS ->
                {
                    revalidate(); // our overridden getPreferredSize() could return a different value
                }
                default ->
                {
                    // Nothing
                }
            }
        } else if (e.getSource() == sptModel.getParentSection())
        {
            if (e.getPropertyName().equals(CLI_Section.PROP_ITEM_DATA))
            {
                modelChanged();
            }
        } else if (e.getSource() == sptModel.getParentSection().getClientProperties())
        {
            // Check if our parent section color has changed in CL_Editor
            if (CL_EditorClientProperties.PROP_SECTION_COLOR.equals(e.getPropertyName()))
            {
                setSptColor(CL_EditorClientProperties.getSectionColor(getModel().getParentSection()));
            }
        }
    }

    // ---------------------------------------------------------------
    // Implements the FocusListener interface
    // ---------------------------------------------------------------
    @Override
    public void focusGained(FocusEvent e
    )
    {
        Border border = settings.getFocusedBorder();
        if (border != null)
        {
            setBorder(border);
        }
    }

    @Override
    public void focusLost(FocusEvent e
    )
    {
        Border border = settings.getDefaultBorder();
        if (border != null)
        {
            setBorder(border);
        }
    }

    // ---------------------------------------------------------------
    // Private methods
    // ---------------------------------------------------------------

    private void updateToolTip()
    {
        Section section = sptModel.getParentSection().getData();
        String strSongPart = ResUtil.getString(SptViewer.class, "SongPart");
        String strParentSection = ResUtil.getString(SptViewer.class, "ParentSection");
        var br = sptModel.getBarRange().getTransformed(1);
        String strBars = ResUtil.getString(SptViewer.class, "Bars");
        strBars += ": " + br.from + "-" + br.to;
        String tt = strSongPart + ": '" + sptModel.getName() + "'  (" + strParentSection + ": '" + section.getName() + "' " + section.getTimeSignature() + ")  " + strBars;
        setToolTipText(tt);
    }

    private void refreshBackground()
    {
        if (isSelected)
        {
            setBackground(settings.getSelectedBackgroundColor());
        } else
        {
            setBackground(sptColor);
        }
    }

    private void updateGlobalUIComponents()
    {
        if (hasFocus())
        {
            setBorder(settings.getFocusedBorder());
        } else
        {
            setBorder(settings.getDefaultBorder());
        }

        refreshBackground();
    }


}
