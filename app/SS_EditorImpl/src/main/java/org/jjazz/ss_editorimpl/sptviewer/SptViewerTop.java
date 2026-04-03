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
package org.jjazz.ss_editorimpl.sptviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerSettings;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.sptviewer.api.SptViewer;
import org.jjazz.ss_editor.sptviewer.api.SptViewerConfig;
import org.jjazz.ss_editor.sptviewer.api.SptViewerConfig.MultiSelect;
import org.jjazz.uiutilities.api.UIUtilities;

/**
 * A SptViewer that shows everything except the RpViewers.
 */
public class SptViewerTop extends SptViewer
{

    private SptViewerConfig uiConfig;
    private static final Logger LOGGER = Logger.getLogger(SptViewerTop.class.getSimpleName());


    public SptViewerTop(SS_Editor ssEditor, SongPart spt, SptViewerSettings settings)
    {
        super(ssEditor, spt, settings);

        uiConfig = new SptViewerConfig();


        initComponents();
        setLayout(new MyLayoutManager());

        updateUIComponents(uiConfig);

    }

    @Override
    public void modelChanged()
    {
        updateUIComponents(uiConfig);
    }

    @Override
    public void settingsChanged()
    {
        updateUIComponents(uiConfig);
    }

    @Override
    public SptViewerConfig getConfig()
    {
        return uiConfig;
    }

    @Override
    public void setConfig(SptViewerConfig newConfig)
    {
        Objects.requireNonNull(newConfig);
        if (!uiConfig.equals(newConfig))
        {
            updateUIComponents(newConfig);
        }
        uiConfig = newConfig;
    }

    /**
     * Overridden to capture mouse click on the multiSelectBar.
     *
     * @param e
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
        if (!SwingUtilities.isLeftMouseButton(e) || controller == null)
        {
            return;
        }

        Component c = (Component) e.getSource();
        LOGGER.log(Level.FINE, "mousePressed() c={0}", c);
        if (c == multiSelectBar)
        {
            controller.songPartClicked(e, getModel(), true);
        } else
        {
            super.mousePressed(e);
        }
    }

    // ---------------------------------------------------------------
    // Private methods
    // ---------------------------------------------------------------
    private void updateUIComponents(SptViewerConfig config)
    {
        // Name 
        fbtn_sptName.setFont(settings.getNameFont());
        fbtn_sptName.setToolTipText(getToolTipText());       // required because there is a mouse listener attached to it, so it won't display parent's tooltip
        Color c = settings.getNameFontColor();
        if (!config.showName())
        {
            c = makeSemiTransparent(c, 65);
        }
        fbtn_sptName.setForeground(c);
        fbtn_sptName.setText(getModel().getName());


        // Bars
        lbl_bars.setFont(settings.getParentSectionFont());
        c = makeSemiTransparent(settings.getNameFontColor(), 65);
        lbl_bars.setForeground(c);
        var br = getModel().getBarRange().getTransformed(1);
        lbl_bars.setText(br.from + "-" + br.to);


        // Parent section & time signature
        Section section = getModel().getParentSection().getData();
        String strParent = "     ";
        if (config.showParentSection())
        {
            String strTs = config.showTimeSignature() ? section.getTimeSignature().toString() + " " : "";
            strParent = strTs + "(" + section.getName() + ")";
        } else if (config.showTimeSignature())
        {
            strParent = section.getTimeSignature().toString();
        }
        lbl_parentSection.setText(strParent);
        lbl_parentSection.setFont(settings.getParentSectionFont());
        lbl_parentSection.setForeground(settings.getParentSectionFontColor());


        // Multi select bar
        multiSelectBar.setOffTooltipText(getToolTipText());
        multiSelectBar.setOn(config.multiSelect() != MultiSelect.OFF);
        multiSelectBar.setMultiSelectFirst(config.multiSelect() == MultiSelect.ON_FIRST);


        // Rhythm button
        fbtn_rhythm.setFont(settings.getRhythmFont());
        c = settings.getRhythmFontColor();
        if (!config.showRhythm())
        {
            c = makeSemiTransparent(c, 65);
        }
        fbtn_rhythm.setForeground(c);
        Rhythm r = getModel().getRhythm();
        fbtn_rhythm.setText(r.getName());
        String fileName = r.getFile() == null ? "" : r.getFile().getName();
        fbtn_rhythm.setToolTipText(fileName + " " + r.getDescription());

    }

    private Color makeSemiTransparent(Color c, int transparency)
    {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), transparency);       // semi-transparent
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        fbtn_sptName = new org.jjazz.flatcomponents.api.FlatButton();
        multiSelectBar = new org.jjazz.ss_editorimpl.sptviewer.MultiSelectBar();
        fbtn_rhythm = new org.jjazz.flatcomponents.api.FlatButton();
        lbl_bars = new javax.swing.JLabel();
        lbl_parentSection = new javax.swing.JLabel();

        setBorder(settings.getDefaultBorder());

        fbtn_sptName.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_sptName, "Spt name"); // NOI18N
        fbtn_sptName.setMinimumSize(new java.awt.Dimension(25, 10));
        fbtn_sptName.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_sptNameActionPerformed(evt);
            }
        });

        multiSelectBar.setLineColor(new java.awt.Color(71, 91, 125));
        multiSelectBar.setOn(true);

        fbtn_rhythm.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_rhythm, "Bossa-Nova.sty"); // NOI18N
        fbtn_rhythm.setMinimumSize(new java.awt.Dimension(25, 10));
        fbtn_rhythm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_rhythmActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_bars, "22-28"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_parentSection, "(A)"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(multiSelectBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(fbtn_sptName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 287, Short.MAX_VALUE)
                                .addComponent(lbl_bars)))
                        .addGap(2, 2, 2))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_parentSection)
                            .addComponent(fbtn_rhythm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fbtn_sptName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_bars))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(multiSelectBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(lbl_parentSection)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(fbtn_rhythm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void fbtn_sptNameActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_sptNameActionPerformed
    {//GEN-HEADEREND:event_fbtn_sptNameActionPerformed
        if (controller != null)
        {
            controller.editSongPartName(getModel());
        }
    }//GEN-LAST:event_fbtn_sptNameActionPerformed

    private void fbtn_rhythmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_rhythmActionPerformed
    {//GEN-HEADEREND:event_fbtn_rhythmActionPerformed
        if (controller != null)
        {
            controller.editSongPartRhythm(getModel());
        }
    }//GEN-LAST:event_fbtn_rhythmActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.flatcomponents.api.FlatButton fbtn_rhythm;
    private org.jjazz.flatcomponents.api.FlatButton fbtn_sptName;
    private javax.swing.JLabel lbl_bars;
    private javax.swing.JLabel lbl_parentSection;
    private org.jjazz.ss_editorimpl.sptviewer.MultiSelectBar multiSelectBar;
    // End of variables declaration//GEN-END:variables


    // =================================================================================================================
    // Inner classes
    // =================================================================================================================
    private class MyLayoutManager implements LayoutManager
    {

        final static int PADDING = 2;

        @Override
        public void layoutContainer(Container parent)
        {
            var r = UIUtilities.getUsableArea((JComponent) parent);
            int xRightLimit = r.x + r.width;
            int yBottomLimit = r.y + r.height;


            fbtn_sptName.setSize(fbtn_sptName.getPreferredSize());
            int x = r.x + PADDING;
            int y = r.y + PADDING;
            fbtn_sptName.setLocation(x, y);
            y += fbtn_sptName.getHeight();
            int extraWidth = r.width - fbtn_sptName.getWidth() - 2 * PADDING;


            lbl_bars.setSize(lbl_bars.getPreferredSize());
            int w = lbl_bars.getWidth();
            x = extraWidth < w ? -1000 : xRightLimit - PADDING - w;           // Hide if not enough room
            lbl_bars.setLocation(x, PADDING);


            y += PADDING;
            multiSelectBar.setSize(r.width - 2 * PADDING, multiSelectBar.getPreferredSize().height);
            multiSelectBar.setLocation(r.x + PADDING, y);
            y += multiSelectBar.getHeight();


            y += PADDING;
            lbl_parentSection.setSize(lbl_parentSection.getPreferredSize());
            x = r.x + PADDING;
            lbl_parentSection.setLocation(x, y);
            y += lbl_parentSection.getHeight();


            y += PADDING;
            fbtn_rhythm.setSize(fbtn_rhythm.getPreferredSize());
            fbtn_rhythm.setLocation(r.x, y);

        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            var in = parent.getInsets();
            int h = in.top
                    + PADDING
                    + fbtn_sptName.getPreferredSize().height
                    + PADDING
                    + lbl_parentSection.getPreferredSize().height
                    + PADDING
                    + multiSelectBar.getPreferredSize().height
                    + PADDING
                    + fbtn_rhythm.getPreferredSize().height
                    + PADDING
                    + in.bottom;
            return new Dimension(1000, h);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return preferredLayoutSize(parent);
        }

        @Override
        public void addLayoutComponent(String name, Component comp)
        {
            // Nothing
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
            // Nothing
        }


    }

}
