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
package org.jjazz.chordinspector;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Font;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.chordinspector.spi.ChordViewer;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.harmony.api.Position;
import org.jjazz.flatcomponents.api.BorderManager;
import org.jjazz.flatcomponents.api.FlatButton;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.openide.util.*;

/**
 * Show the active ChordViewer, and let user changes to another viewer.
 */
public class ChordInspectorPanel extends javax.swing.JPanel
{

    private static final Border BORDER_NOTHING_SELECTED = BorderFactory.createLineBorder(Color.RED);
    private static final Border BORDER_NOTHING_UNSELECTED = BorderManager.DEFAULT_BORDER_NOTHING;
    private static final Border BORDER_ENTERED_SELECTED = BORDER_NOTHING_SELECTED;
    private static final Border BORDER_ENTERED_UNSELECTED = BorderManager.DEFAULT_BORDER_ENTERED;
    private static final Border BORDER_PRESSED = BorderFactory.createLineBorder(Color.RED.darker());


    private final Map<ChordViewer, FlatButton> mapViewerButton = new HashMap<>();
    private ChordViewer chordViewer;


    public ChordInspectorPanel()
    {
        initComponents();

        setActiveChordViewer(initChordViewers());
        
        try
        {
            setModel(CLI_Factory.getDefault().createChordSymbol("C", new Position()));
        } catch (ParseException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }
    }

    public void cleanup()
    {
        chordViewer.cleanup();
    }

    public void setModel(CLI_ChordSymbol model)
    {
        chordViewer.setModel(model);
        updateChordSymbolUI(model);
    }

    public CLI_ChordSymbol getModel()
    {
        return chordViewer.getModel();
    }

    // =================================================================================================
    // Private methods
    // =================================================================================================
    /**
     * Get all the ChordViewers available in the global lookup and add a button for each one.
     *
     * @return The default ChordViewer
     */
    private ChordViewer initChordViewers()
    {
        Collection<? extends ChordViewer> chordViewers = Lookup.getDefault().lookupAll(ChordViewer.class);
        assert !chordViewers.isEmpty();

        for (var cv : chordViewers)
        {
            FlatButton btn = new FlatButton();
            btn.setIcon(cv.getIcon());
            btn.setToolTipText(cv.getDescription());
            btn.addActionListener(ae -> setActiveChordViewer(cv));
            btn.setBorderPressed(BORDER_PRESSED);
            pnl_viewerButtons.add(btn);
            mapViewerButton.put(cv, btn);
        }

        return chordViewers.iterator().next();
    }

    private void setActiveChordViewer(ChordViewer cViewer)
    {
        Preconditions.checkNotNull(cViewer);
        if (chordViewer == cViewer)
        {
            return;
        }

        CLI_ChordSymbol model = null;
        if (chordViewer != null)
        {
            model = chordViewer.getModel();
            chordViewer.cleanup();
            mapViewerButton.get(chordViewer).setBorderNothing(BORDER_NOTHING_UNSELECTED);
            mapViewerButton.get(chordViewer).setBorderEntered(BORDER_ENTERED_UNSELECTED);
        }


        chordViewer = cViewer;
        chordViewer.setModel(model); 
        

        pnl_viewer.removeAll();
        pnl_viewer.add(chordViewer.getComponent());
        pnl_viewer.revalidate();
        pnl_viewer.repaint();

        
        mapViewerButton.get(chordViewer).setBorderNothing(BORDER_NOTHING_SELECTED);
        mapViewerButton.get(chordViewer).setBorderEntered(BORDER_ENTERED_SELECTED);

        Analytics.logEvent("Set Active Chord Viewer", Analytics.buildMap("ChordViewer", chordViewer.getClass().getSimpleName()));
    }

    private void updateChordSymbolUI(CLI_ChordSymbol cliCs)
    {
        String strCs = " ";
        String strExtraInfo = " ";
        if (cliCs != null)
        {
            var ecs = cliCs.getData();
            strCs = ecs.getOriginalName();
            strExtraInfo = ecs.toNoteString();
            var scale = ecs.getRenderingInfo().getScaleInstance();
            if (scale != null)
            {
                strExtraInfo += " - " + scale.toString();
            }
        }
        lbl_chord.setText(strCs);
        lbl_extraInfo.setText(strExtraInfo);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_center_chord = new javax.swing.JPanel();
        pnl_chord_scale = new javax.swing.JPanel();
        lbl_chord = new javax.swing.JLabel();
        lbl_extraInfo = new javax.swing.JLabel();
        pnl_viewerButtons = new javax.swing.JPanel();
        pnl_viewer = new javax.swing.JPanel();

        pnl_center_chord.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        pnl_chord_scale.setLayout(new java.awt.BorderLayout());

        lbl_chord.setFont(GeneralUISettings.getInstance().getStdFont().deriveFont(Font.BOLD, 20f));
        lbl_chord.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_chord, "C7#11"); // NOI18N
        pnl_chord_scale.add(lbl_chord, java.awt.BorderLayout.CENTER);

        lbl_extraInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_extraInfo, "C D F# A"); // NOI18N
        pnl_chord_scale.add(lbl_extraInfo, java.awt.BorderLayout.SOUTH);

        pnl_center_chord.add(pnl_chord_scale);

        pnl_viewerButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        pnl_viewer.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_center_chord, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
                    .addComponent(pnl_viewerButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addComponent(pnl_viewer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(pnl_center_chord, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_viewer, javax.swing.GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_viewerButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lbl_chord;
    private javax.swing.JLabel lbl_extraInfo;
    private javax.swing.JPanel pnl_center_chord;
    private javax.swing.JPanel pnl_chord_scale;
    private javax.swing.JPanel pnl_viewer;
    private javax.swing.JPanel pnl_viewerButtons;
    // End of variables declaration//GEN-END:variables


}
