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
package org.jjazz.options.defaultinstrumentsdialog;

import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.outputsynth.api.GMRemapTable;
import org.jjazz.outputsynth.api.GMRemapTable.InvalidMappingException;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.testplayerservice.spi.TestPlayer;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * Dialog to let user add GM-instrument mappings.
 */
public class DefaultInstrumentsDialog extends javax.swing.JDialog implements PropertyChangeListener
{

    private final GMRemapTable saveRemapTable;
    private final GMRemapTable remapTable;
    private static final Logger LOGGER = Logger.getLogger(DefaultInstrumentsDialog.class.getSimpleName());

    /**
     * Creates new form DefaultInstrumentsDialog
     *
     * @param dlgTitle
     * @param remapTable
     */
    public DefaultInstrumentsDialog(String dlgTitle, GMRemapTable remapTable)
    {
        super(WindowManager.getDefault().getMainWindow(), true);

        setTitle(dlgTitle);

        initComponents();
        
        this.remapTable = remapTable;
        this.saveRemapTable = new GMRemapTable(remapTable.getMidiSynth());
        this.saveRemapTable.set(remapTable);
        this.remapTable.addPropertyChangeListener(this);

        tbl_Remap.setPrimaryModel(this.remapTable);
        tbl_Remap.getSelectionModel().addListSelectionListener(e -> enableDisableButtons());
        tbl_Remap.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                handleTableMouseClicked(e);
            }
        });

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                btn_CancelActionPerformed(null);
            }

            @Override
            public void windowClosed(WindowEvent e)
            {
                remapTable.removePropertyChangeListener(DefaultInstrumentsDialog.this);
            }
        });


        enableDisableButtons();
        setLocationByPlatform(true);        
    }

    public String getHelpText()
    {
        return hlp_area.getText();
    }

    // ==============================================================================
    // PropertyChangeListener interface
    // ==============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == remapTable)
        {
            enableDisableButtons();
        }
    }

    // ====================================================================================================
    // Private methods
    // ====================================================================================================    
    private void enableDisableButtons()
    {
        Instrument remappedIns = tbl_Remap.getSelectedRemappedInstrument();
        btn_changeRemappedIns.setEnabled(remappedIns != null);
        boolean mappingExist = remappedIns != null && remapTable.getInstrument(remappedIns) != null;
        btn_HearRemap.setEnabled(mappingExist);
        btn_ResetInstrument.setEnabled(mappingExist);
    }

    private void handleTableMouseClicked(MouseEvent evt)
    {
        boolean ctrl = (evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        boolean shift = (evt.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
        if (evt.getSource() == this.tbl_Remap)
        {
            if (SwingUtilities.isLeftMouseButton(evt))
            {
                if (evt.getClickCount() == 1 && shift)
                {
                    btn_HearRemapActionPerformed(null);
                } else if (evt.getClickCount() == 2 && !shift)
                {
                    btn_changeRemappedInsActionPerformed(null);
                }
            }
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

        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_Remap = new org.jjazz.options.defaultinstrumentsdialog.RemapTableUI();
        jScrollPane2 = new javax.swing.JScrollPane();
        hlp_area = new org.jjazz.flatcomponents.api.HelpTextArea();
        btn_HearRemap = new javax.swing.JButton();
        btn_changeRemappedIns = new javax.swing.JButton();
        btn_ResetInstrument = new javax.swing.JButton();
        btn_Cancel = new javax.swing.JButton();
        btn_Ok = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jScrollPane1.setViewportView(tbl_Remap);

        jScrollPane2.setBorder(null);

        hlp_area.setColumns(20);
        hlp_area.setRows(5);
        hlp_area.setText(org.openide.util.NbBundle.getMessage(DefaultInstrumentsDialog.class, "DefaultInstrumentsDialog.hlp_area.text")); // NOI18N
        jScrollPane2.setViewportView(hlp_area);

        btn_HearRemap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/options/defaultinstrumentsdialog/resources/SpeakerRed-20x20.png"))); // NOI18N
        btn_HearRemap.setToolTipText(org.openide.util.NbBundle.getMessage(DefaultInstrumentsDialog.class, "DefaultInstrumentsDialog.btn_HearRemap.toolTipText")); // NOI18N
        btn_HearRemap.setInheritsPopupMenu(true);
        btn_HearRemap.setMargin(new java.awt.Insets(2, 2, 2, 2));
        btn_HearRemap.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_HearRemapActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_changeRemappedIns, org.openide.util.NbBundle.getMessage(DefaultInstrumentsDialog.class, "DefaultInstrumentsDialog.btn_changeRemappedIns.text")); // NOI18N
        btn_changeRemappedIns.setToolTipText(org.openide.util.NbBundle.getMessage(DefaultInstrumentsDialog.class, "DefaultInstrumentsDialog.btn_changeRemappedIns.toolTipText")); // NOI18N
        btn_changeRemappedIns.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_changeRemappedInsActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_ResetInstrument, org.openide.util.NbBundle.getMessage(DefaultInstrumentsDialog.class, "DefaultInstrumentsDialog.btn_ResetInstrument.text")); // NOI18N
        btn_ResetInstrument.setToolTipText(org.openide.util.NbBundle.getMessage(DefaultInstrumentsDialog.class, "DefaultInstrumentsDialog.btn_ResetInstrument.toolTipText")); // NOI18N
        btn_ResetInstrument.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_ResetInstrumentActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(DefaultInstrumentsDialog.class, "DefaultInstrumentsDialog.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Ok, org.openide.util.NbBundle.getMessage(DefaultInstrumentsDialog.class, "DefaultInstrumentsDialog.btn_Ok.text")); // NOI18N
        btn_Ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OkActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE)
                    .addComponent(jScrollPane2)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_Ok)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btn_changeRemappedIns)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_ResetInstrument)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_HearRemap)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_Cancel, btn_Ok});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_HearRemap)
                    .addComponent(btn_changeRemappedIns)
                    .addComponent(btn_ResetInstrument))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 311, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_Cancel)
                    .addComponent(btn_Ok))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_changeRemappedInsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_changeRemappedInsActionPerformed
    {//GEN-HEADEREND:event_btn_changeRemappedInsActionPerformed
        Instrument mappedIns = tbl_Remap.getSelectedRemappedInstrument();
        if (mappedIns != null)
        {
            RemapTableInstrumentChooser chooser = RemapTableInstrumentChooser.getInstance();
            chooser.preset(remapTable, mappedIns);
            chooser.setVisible(true);
            Instrument ins = chooser.getSelectedInstrument();
            if (chooser.isExitOK() && ins != null)
            {
                try
                {
                    remapTable.setInstrument(mappedIns, ins, chooser.useAsFamilyDefault());
                } catch (InvalidMappingException ex)
                {
                    NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(d);
                }

                Analytics.logEvent("Change Default Instrument");
            }
        }
    }//GEN-LAST:event_btn_changeRemappedInsActionPerformed

    private void btn_HearRemapActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_HearRemapActionPerformed
    {//GEN-HEADEREND:event_btn_HearRemapActionPerformed
        Instrument remappedIns = tbl_Remap.getSelectedRemappedInstrument();
        if (remappedIns == null)
        {
            LOGGER.log(Level.FINE, "btn_HearActionPerformed() called but invalid remappedIns={0}", remappedIns);   
            return;
        }
        Instrument ins = remapTable.getInstrument(remappedIns);
        if (ins == null || !ins.getMidiAddress().isFullyDefined())
        {
            LOGGER.log(Level.FINE, "btn_HearActionPerformed() called but invalid ins={0} ins.getMidiAddress()={1}", new Object[]{ins,
                ins != null ? ins.getMidiAddress() : ""});   
            return;
        }
        tbl_Remap.setEnabled(false);
        btn_HearRemap.setEnabled(false);
        btn_changeRemappedIns.setEnabled(false);

        Runnable endAction = new Runnable()
        {
            @Override
            public void run()
            {
                tbl_Remap.setEnabled(true);
                btn_HearRemap.setEnabled(true);
                btn_changeRemappedIns.setEnabled(true);
            }
        };
        // Send MIDI messages for the selected instrument             
        TestPlayer tp = TestPlayer.getDefault();
        try
        {
            final int CHANNEL = ins.isDrumKit() ? MidiConst.CHANNEL_DRUMS : 0;
            final int TRANSPOSE = ins.isDrumKit() ? -24 : 0;
            JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(CHANNEL));
            tp.playTestNotes(CHANNEL, -1, TRANSPOSE, endAction);

        } catch (MusicGenerationException ex)
        {
            endAction.run();
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }
    }//GEN-LAST:event_btn_HearRemapActionPerformed

    private void btn_ResetInstrumentActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_ResetInstrumentActionPerformed
    {//GEN-HEADEREND:event_btn_ResetInstrumentActionPerformed
        Instrument remappedIns = tbl_Remap.getSelectedRemappedInstrument();
        if (remappedIns == null)
        {
            return;
        }
        Instrument ins = remapTable.getInstrument(remappedIns);
        if (ins == null)
        {
            return;
        }
        boolean useAsFamilyDefault = false;
        if (remappedIns instanceof GM1Instrument)
        {
            GM1Instrument gmIns = (GM1Instrument) remappedIns;
            useAsFamilyDefault = remapTable.getInstrument(gmIns.getFamily()) == ins;
        }
        try
        {
            remapTable.setInstrument(remappedIns, null, useAsFamilyDefault);
        } catch (InvalidMappingException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }
    }//GEN-LAST:event_btn_ResetInstrumentActionPerformed

    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
    {//GEN-HEADEREND:event_btn_CancelActionPerformed
        this.remapTable.set(saveRemapTable);
        setVisible(false);
        dispose();
    }//GEN-LAST:event_btn_CancelActionPerformed

    private void btn_OkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_OkActionPerformed
    {//GEN-HEADEREND:event_btn_OkActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_btn_OkActionPerformed



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_HearRemap;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JButton btn_ResetInstrument;
    private javax.swing.JButton btn_changeRemappedIns;
    private org.jjazz.flatcomponents.api.HelpTextArea hlp_area;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private org.jjazz.options.defaultinstrumentsdialog.RemapTableUI tbl_Remap;
    // End of variables declaration//GEN-END:variables

}
