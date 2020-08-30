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
package org.jjazz.ui.sptviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.colorsetmanager.api.ColorSetManager;
import org.jjazz.ui.sptviewer.api.SptViewer;
import org.jjazz.ui.sptviewer.api.SptViewerMouseListener;
import org.jjazz.ui.sptviewer.api.SptViewerSettings;
import org.jjazz.ui.rpviewer.api.RpViewer;
import org.jjazz.ui.rpviewer.api.RpViewerFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.uisettings.GeneralUISettings;

/**
 * An implementation of a SptViewer.
 */
public class SptViewerImpl extends SptViewer implements FocusListener, PropertyChangeListener, MouseListener, MouseMotionListener, MouseWheelListener
{

    private static final int ONE_BAR_EXTRA_SIZE = 60;
    private static final int MIN_WIDTH = 40;

    // APPLICATION Variables
    /**
     * Our model.
     */
    private SptViewerMouseListener controller;
    private SongPart sptModel;
    /**
     * Selected state.
     */
    private boolean isSelected;
    private boolean showRhythm = true;
    private boolean showTimeSignature = false;
    private boolean showName = true;
    private boolean multiSelectMode = false;
    private boolean multiSelectFirst = false;
    private int zoomHFactor;
    private int zoomVFactor;
    private boolean isPlaybackOn;
    /**
     * Our graphical settings.
     */
    private SptViewerSettings settings;
    private static final Logger LOGGER = Logger.getLogger(SptViewerImpl.class.getSimpleName());

    public SptViewerImpl(SongPart spt)
    {
        if (spt == null)
        {
            throw new NullPointerException("spt=" + spt);
        }
        sptModel = spt;
        sptModel.addPropertyChangeListener(this);
        sptModel.getParentSection().addPropertyChangeListener(this);

        // Register settings changes
        settings = SptViewerSettings.getDefault();
        settings.addPropertyChangeListener(this);

        // Keep track if section colors change
        ColorSetManager.getDefault().addPropertyChangeListener(this);

        addFocusListener(this);
        zoomHFactor = 50;
        zoomVFactor = 50;
        initComponents();

        // Register some UI components
        multiSelectBar.addMouseListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        // Update UI
        updateUIComponents();

        // Create the RpViewers as required        
        setVisibleRps(sptModel.getRhythm().getRhythmParameters());
    }

    @Override
    public void setController(SptViewerMouseListener controller)
    {
        this.controller = controller;
    }

    @Override
    public SongPart getModel()
    {
        return sptModel;
    }

    @Override
    public void setSelected(boolean b)
    {
        isSelected = b;
        refreshBackground();
    }

    @Override
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

    @Override
    public int getZoomHFactor()
    {
        return zoomHFactor;
    }

    @Override
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

    @Override
    public int getZoomVFactor()
    {
        return zoomHFactor;
    }

    /**
     * Overridden to enable SongPartEditors to be aligned on their baseline (for example by FlowLayout).
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
     * Overridden to set the preferredSize proportional to the SongPart length.
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

    @Override
    public void setRhythmVisible(boolean b)
    {
        if (showRhythm == b)
        {
            return;
        }
        showRhythm = b;
        updateUIComponents();
    }

    @Override
    public void setMultiSelectMode(boolean b, boolean first)
    {
        multiSelectMode = b;
        multiSelectFirst = first;
        updateUIComponents();
    }

    @Override
    public void setNameVisible(boolean b)
    {
        if (showName == b)
        {
            return;
        }
        showName = b;
        updateUIComponents();
    }

    @Override
    public void setTimeSignatureVisible(boolean b)
    {
        if (showTimeSignature == b)
        {
            return;
        }
        showTimeSignature = b;
        updateUIComponents();
    }

    @Override
    public void setSelected(RhythmParameter<?> rp, boolean b)
    {
        for (RpViewer rpv : getRpViewers())
        {
            if (rpv.getRpModel() == rp)
            {
                rpv.setSelected(b);
                return;
            }
        }
    }

    @Override
    public void setFocusOnRpViewer(RhythmParameter<?> rp)
    {
        for (RpViewer rpv : getRpViewers())
        {
            if (rpv.getRpModel() == rp)
            {
                rpv.requestFocusInWindow();
                return;
            }
        }
    }

    @Override
    public void setVisibleRps(List<RhythmParameter<?>> rps)
    {
        for (RpViewer rpv : getRpViewers())
        {
            unregisterRpViewer(rpv);
            rpv.cleanup();
        }
        pnl_RpEditors.removeAll();
        for (RhythmParameter<?> rp : rps)
        {
            if (sptModel.getRhythm().getRhythmParameters().contains(rp))
            {
                RpViewer rpv = RpViewerFactory.getDefault().createRpViewer(sptModel, rp);
                registerRpViewer(rpv);
                rpv.setZoomVFactor(zoomVFactor);
                pnl_RpEditors.add((Component) rpv);
                pnl_RpEditors.add(Box.createRigidArea(new Dimension(0, 4)));
            } else
            {
                throw new IllegalArgumentException("rp=" + rp + " sptModel.getRhythm().getRhythmParameters()=" + sptModel.getRhythm().getRhythmParameters());
            }
        }
        pnl_RpEditors.revalidate();
        pnl_RpEditors.repaint();
    }

    @Override
    public void showPlaybackPoint(boolean show, Position pos)
    {
        if (show && pos == null)
        {
            throw new IllegalArgumentException("show=" + show + " pos=" + pos);
        }
        isPlaybackOn = show;
        int startBarIndex = getModel().getStartBarIndex();
        int lastBarIndex = startBarIndex + getModel().getNbBars() - 1;
        if (show && (pos.getBar() < startBarIndex || pos.getBar() > lastBarIndex))
        {
            // Show the playbackpoint but BarIndex does not belong in this SptViewer ! 
            isPlaybackOn = false;
            LOGGER.warning(
                    "showPlaybackPoint() show=" + show + " pos.getBar()=" + pos.getBar() + " is outside SptViewer startBarIndex=" + getModel().
                    getStartBarIndex() + " size=" + getModel().getNbBars());
        }
        refreshBackground();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void cleanup()
    {
        setVisibleRps(Collections.EMPTY_LIST);            // Unchecked warning
        ColorSetManager.getDefault().removePropertyChangeListener(this);
        sptModel.removePropertyChangeListener(this);
        settings.removePropertyChangeListener(this);
        sptModel.getParentSection().removePropertyChangeListener(this);
        removeFocusListener(this);
        sptModel = null;
        controller = null;
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
        if (c == this)
        {
            controller.songPartClicked(e, sptModel, false);
        } else if (c == multiSelectBar)
        {
            controller.songPartClicked(e, sptModel, true);
        } else if (c instanceof RpViewer)
        {
            RpViewer rpv = (RpViewer) c;
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
        } else if (c instanceof RpViewer)
        {
            RpViewer rpv = (RpViewer) c;
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
        if (c == this)
        {
            // Since JPanel does not normally support drag-and-drop, start drag if a transfer handler is set
            TransferHandler th = getTransferHandler();
            if (th != null)
            {
                th.exportAsDrag(SptViewerImpl.this, e, TransferHandler.COPY);
            }
            controller.songPartDragged(e, sptModel);
        } else if (c instanceof RpViewer)
        {
            RpViewer rpv = (RpViewer) c;
            controller.rhythmParameterDragged(e, rpv.getSptModel(), rpv.getRpModel());
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
        if (c instanceof RpViewer)
        {
            RpViewer rpv = (RpViewer) c;
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
            updateUIComponents();
        } else if (e.getSource() == ColorSetManager.getDefault())
        {
            updateUIComponents();       // Our parentSection's color might have changed
        } else if (e.getSource() == sptModel)
        {
            if (e.getPropertyName() == SongPart.PROPERTY_NAME)
            {
                updateUIComponents();
            } else if (e.getPropertyName() == SongPart.PROPERTY_NB_BARS)
            {
                revalidate(); // our overridden getPreferredSize() could return a different value
            }
        } else if (e.getSource() == sptModel.getParentSection())
        {
            if (e.getPropertyName() == CLI_Section.PROP_ITEM_DATA)
            {
                updateUIComponents();
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

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_top = new javax.swing.JPanel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 32767));
        pnl_labels = new javax.swing.JPanel();
        fbtn_sptName = new org.jjazz.ui.flatcomponents.FlatButton();
        multiSelectBar = new org.jjazz.ui.sptviewer.MultiSelectBar();
        lbl_Parent = new javax.swing.JLabel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 4), new java.awt.Dimension(0, 4), new java.awt.Dimension(32767, 4));
        fbtn_rhythm = new org.jjazz.ui.flatcomponents.FlatButton();
        pnl_RpEditors = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        setBorder(settings.getDefaultBorder());
        setLayout(new java.awt.BorderLayout());

        pnl_top.setAlignmentX(0.0F);
        pnl_top.setOpaque(false);
        pnl_top.setLayout(new javax.swing.BoxLayout(pnl_top, javax.swing.BoxLayout.LINE_AXIS));
        pnl_top.add(filler1);

        pnl_labels.setOpaque(false);
        pnl_labels.setLayout(new javax.swing.BoxLayout(pnl_labels, javax.swing.BoxLayout.Y_AXIS));

        org.openide.awt.Mnemonics.setLocalizedText(fbtn_sptName, org.openide.util.NbBundle.getMessage(SptViewerImpl.class, "SptViewerImpl.fbtn_sptName.text_1")); // NOI18N
        fbtn_sptName.setMinimumSize(new java.awt.Dimension(25, 10));
        fbtn_sptName.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_sptNameActionPerformed(evt);
            }
        });
        pnl_labels.add(fbtn_sptName);

        multiSelectBar.setLineColor(new java.awt.Color(0, 51, 51));
        multiSelectBar.setOn(true);
        pnl_labels.add(multiSelectBar);

        lbl_Parent.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Parent, org.openide.util.NbBundle.getMessage(SptViewerImpl.class, "SptViewerImpl.lbl_Parent.text_1")); // NOI18N
        lbl_Parent.setMinimumSize(new java.awt.Dimension(25, 10));
        pnl_labels.add(lbl_Parent);
        pnl_labels.add(filler2);

        org.openide.awt.Mnemonics.setLocalizedText(fbtn_rhythm, org.openide.util.NbBundle.getMessage(SptViewerImpl.class, "SptViewerImpl.fbtn_rhythm.text_1")); // NOI18N
        fbtn_rhythm.setMinimumSize(new java.awt.Dimension(25, 10));
        fbtn_rhythm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_rhythmActionPerformed(evt);
            }
        });
        pnl_labels.add(fbtn_rhythm);

        pnl_top.add(pnl_labels);

        add(pnl_top, java.awt.BorderLayout.NORTH);

        pnl_RpEditors.setOpaque(false);
        pnl_RpEditors.setLayout(new javax.swing.BoxLayout(pnl_RpEditors, javax.swing.BoxLayout.Y_AXIS));

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SptViewerImpl.class, "SptViewerImpl.jLabel1.text_1")); // NOI18N
        pnl_RpEditors.add(jLabel1);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(SptViewerImpl.class, "SptViewerImpl.jLabel2.text")); // NOI18N
        pnl_RpEditors.add(jLabel2);

        add(pnl_RpEditors, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void fbtn_sptNameActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_sptNameActionPerformed
    {//GEN-HEADEREND:event_fbtn_sptNameActionPerformed
        if (controller != null)
        {
            controller.editSongPartName(sptModel);
        }
    }//GEN-LAST:event_fbtn_sptNameActionPerformed

    private void fbtn_rhythmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_rhythmActionPerformed
    {//GEN-HEADEREND:event_fbtn_rhythmActionPerformed
        if (controller != null)
        {
            controller.editSongPartRhythm(sptModel);
        }
    }//GEN-LAST:event_fbtn_rhythmActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.ui.flatcomponents.FlatButton fbtn_rhythm;
    private org.jjazz.ui.flatcomponents.FlatButton fbtn_sptName;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lbl_Parent;
    private org.jjazz.ui.sptviewer.MultiSelectBar multiSelectBar;
    private javax.swing.JPanel pnl_RpEditors;
    private javax.swing.JPanel pnl_labels;
    private javax.swing.JPanel pnl_top;
    // End of variables declaration//GEN-END:variables

    // ---------------------------------------------------------------
    // Private methods
    // ---------------------------------------------------------------
    private void updateUIComponents()
    {
        // Rhythm button
        fbtn_rhythm.setFont(settings.getRhythmFont());
        fbtn_rhythm.setForeground(settings.getRhythmFontColor());
        Rhythm r = sptModel.getRhythm();
        String strRhythm = r.getName().toLowerCase();
        fbtn_rhythm.setText(showRhythm ? strRhythm : "      "); // Empty string needs to be long enough to remaing clickable
        String fileName = r.getFile() == null ? "" : " - " + r.getFile().getName();
        fbtn_rhythm.setToolTipText(r.getDescription() + fileName);

        // Name button
        fbtn_sptName.setFont(settings.getNameFont());
        fbtn_sptName.setForeground(settings.getNameFontColor());
        String strName = showName ? sptModel.getName() : "   ";
        fbtn_sptName.setText(strName);
        Section section = sptModel.getParentSection().getData();
        String strTooltip = sptModel.getName() + " " + section.getTimeSignature() + " (parent:" + section.getName() + ")";
        fbtn_sptName.setToolTipText(strTooltip);

        // Optional parent name
        String strParent = section.getName().equals(sptModel.getName()) ? "    " : "(" + section.getName() + ")";
        if (showTimeSignature)
        {
            strParent = strParent.isBlank() ? section.getTimeSignature().toString() : strParent + " " + section.getTimeSignature().toString();
        }
        lbl_Parent.setText(strParent);
        lbl_Parent.setToolTipText(strTooltip);
        lbl_Parent.setFont(settings.getParentSectionFont());
        lbl_Parent.setForeground(settings.getParentSectionFontColor());

        multiSelectBar.setOn(multiSelectMode);
        multiSelectBar.setMultiSelectFirst(multiSelectFirst);

        if (hasFocus())
        {
            setBorder(settings.getFocusedBorder());
        } else
        {
            setBorder(settings.getDefaultBorder());
        }

        refreshBackground();
    }

    private List<RpViewer> getRpViewers()
    {
        ArrayList<RpViewer> rpvs = new ArrayList<>();
        for (Component c : pnl_RpEditors.getComponents())
        {
            if (c instanceof RpViewer)
            {
                RpViewer rpv = (RpViewer) c;
                rpvs.add(rpv);
            }
        }
        return rpvs;
    }

    private void registerRpViewer(RpViewer rpv)
    {
        rpv.addMouseListener(this);
        rpv.addMouseMotionListener(this);
        // Use mouse wheel only if enabled
        GeneralUISettings.getInstance().installChangeValueWithMouseWheelSupport(rpv, this);
    }

    private void unregisterRpViewer(RpViewer rpv)
    {
        rpv.removeMouseListener(this);
        rpv.removeMouseMotionListener(this);
        rpv.removeMouseWheelListener(this);
    }

    private void refreshBackground()
    {
        if (isPlaybackOn)
        {
            setBackground(settings.getPlaybackColor());
        } else if (isSelected)
        {
            setBackground(settings.getSelectedBackgroundColor());
        } else
        {
            // setBackground(settings.getDefaultBackgroundColor());
            Color c = getParentSectionColor();
            if (c == null)
            {
                c = settings.getDefaultBackgroundColor();
            }
            setBackground(c);
        }
    }

    private Color getParentSectionColor()
    {
        Color res = null;
        if (sptModel != null && settings.isSectionColorUsedAsBackground())
        {
            Color c = ColorSetManager.getDefault().getColor(sptModel.getParentSection().getData().getName());
            res = (c == null) ? res : new Color(c.getRed(), c.getGreen(), c.getBlue(), 150);    // A bit transparent
        }
        return res;
    }

    private void redispatchEvent(java.awt.event.MouseEvent evt)
    {
        Container source = (Container) evt.getSource();
        MouseEvent parentEvent = SwingUtilities.convertMouseEvent(source, evt, this);
        this.dispatchEvent(parentEvent);
    }

}
