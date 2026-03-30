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
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerSettings;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.sptviewer.api.SptViewer;
import org.jjazz.ss_editor.sptviewer.api.SptViewerConfig;
import org.jjazz.ss_editor.sptviewer.api.SptViewerConfig.MultiSelect;

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

        updateUIComponents();

    }

    @Override
    public void modelChanged()
    {
        updateUIComponents();
    }

    @Override
    public void settingsChanged()
    {
        updateUIComponents();
    }

    @Override
    public SptViewerConfig getConfig()
    {
        return uiConfig;
    }

    @Override
    public void setConfig(SptViewerConfig uiConfig)
    {
        Objects.requireNonNull(uiConfig);
        if (!this.uiConfig.equals(uiConfig))
        {
            this.uiConfig = uiConfig;
            updateUIComponents();
        }
    }

    /**
     * Overridden to capture mouse click on the multiSelectBar.
     *
     * @param e
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
        if (controller == null)
        {
            return;
        }
        super.mousePressed(e);
        Component c = (Component) e.getSource();
        LOGGER.log(Level.FINE, "mousePressed() c={0}", c);
        if (c == multiSelectBar)
        {
            controller.songPartClicked(e, getModel(), true);
        }
    }

    // ---------------------------------------------------------------
    // Private methods
    // ---------------------------------------------------------------
    private void updateUIComponents()
    {
        // Name button
        fbtn_sptName.setFont(settings.getNameFont());
        Color c = settings.getNameFontColor();
        if (!uiConfig.showName())
        {
            c = makeSemiTransparent(c);
        }
        fbtn_sptName.setForeground(c);
        fbtn_sptName.setText(getModel().getName());
        fbtn_sptName.setToolTipText(getToolTipText());


        // Multi select bar
        multiSelectBar.setOn(uiConfig.multiSelect()!= MultiSelect.OFF);
        multiSelectBar.setMultiSelectFirst(uiConfig.multiSelect() == MultiSelect.ON_FIRST);


        // Parent section        
        Section section = getModel().getParentSection().getData();
        String strParent = section.getName().equals(getModel().getName()) ? "    " : "(" + section.getName() + ")";
        if (uiConfig.showTimeSignature())
        {
            strParent = strParent.isBlank() ? section.getTimeSignature().toString()
                    : strParent + " " + section.getTimeSignature().toString();
        }
        lbl_parentSection.setText(strParent);
        lbl_parentSection.setToolTipText(getToolTipText());
        lbl_parentSection.setFont(settings.getParentSectionFont());
        lbl_parentSection.setForeground(settings.getParentSectionFontColor());


        // Rhythm button
        fbtn_rhythm.setFont(settings.getRhythmFont());
        c = settings.getRhythmFontColor();
        if (!uiConfig.showRhythm())
        {
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 120);       // semi-transparent
        }
        fbtn_rhythm.setForeground(c);
        Rhythm r = getModel().getRhythm();
        fbtn_rhythm.setText(r.getName());
        String fileName = r.getFile() == null ? "" : r.getFile().getName();
        fbtn_rhythm.setToolTipText(fileName + " " + r.getDescription());


    }


    private Color makeSemiTransparent(Color c)
    {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 60);       // semi-transparent
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

        multiSelectBar.setLineColor(new java.awt.Color(0, 51, 51));
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

        org.openide.awt.Mnemonics.setLocalizedText(lbl_parentSection, "(A)  4/4"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(multiSelectBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(fbtn_sptName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 254, Short.MAX_VALUE)
                                .addComponent(lbl_bars)))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_parentSection)
                            .addComponent(fbtn_rhythm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fbtn_sptName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_bars))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(multiSelectBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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


}
