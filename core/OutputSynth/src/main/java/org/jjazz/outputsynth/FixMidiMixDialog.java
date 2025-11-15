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
package org.jjazz.outputsynth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.utilities.api.ResUtil;
import org.openide.windows.WindowManager;

/**
 * The dialog to get user approval for MidiMix fix.
 */
public class FixMidiMixDialog extends javax.swing.JDialog
{

    public enum FixChoice
    {
        FIX, DONT_FIX
    };
    private FixChoice choice;
    private static final Logger LOGGER = Logger.getLogger(FixMidiMixDialog.class.getSimpleName());

    public FixMidiMixDialog()
    {
        super(WindowManager.getDefault().getMainWindow(), ResUtil.getString(FixMidiMixDialog.class, "MIDI_CONFIGURATION_PROBLEMS"), true);
        choice = FixChoice.FIX;
        initComponents();
        setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
    }

    /**
     *
     * @return The user choice on this dialog.
     */
    public FixChoice getUserChoice()
    {
        return choice;
    }

    /**
     * @return True if user checked "Remember my choice"
     */
    public boolean isRememberChoiceSelected()
    {
        return cb_rememberMyChoice.isSelected();
    }

    /**
     * Preset the dialog for the specified channels.
     * <p>
     * @param mapChannelNewIns Channels which need instruments fix.
     * @param reroutedChannels
     * @param midiMix
     */
    public void preset(HashMap<Integer, Instrument> mapChannelNewIns, List<Integer> reroutedChannels, MidiMix midiMix)
    {
        if (mapChannelNewIns == null || reroutedChannels == null || midiMix == null)
        {
            throw new IllegalArgumentException("mapChannelNewIns=" + mapChannelNewIns + " reroutedChannels=" + reroutedChannels + " midiMix=" + midiMix);   
        }

        // Update dialog UI
        // Fix instruments part
        String fixInstrumentsText = null;
        if (!mapChannelNewIns.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("<HTML>");
            List<Integer> channels = new ArrayList<>(mapChannelNewIns.keySet());
            Collections.sort(channels);
            for (int ch : channels)
            {
                RhythmVoice rv = midiMix.getRhythmVoice(ch);
                Instrument ins = midiMix.getInstrumentMix(ch).getInstrument();
                Instrument newIns = mapChannelNewIns.get(ch);
                sb.append("Channel ").append(ch + 1).append(" : ");
                sb.append(ins.getFullName()).append(" >> ").append(newIns.getFullName());
                if (ch != channels.get(channels.size() - 1))
                {
                    sb.append("<br/>");
                }
            }
            sb.append("</HTML>");
            fixInstrumentsText = sb.toString();
        }
        lbl_fixedInstruments.setText(fixInstrumentsText);

        // Reroute channels part
        String reroutedChannelsTitle = null;
        String reroutedChannelsText = null;
        if (!reroutedChannels.isEmpty())
        {
            reroutedChannelsTitle = "The following drums channels should be rerouted to Midi channel 10:";
            StringBuilder sb = new StringBuilder();
            sb.append("<HTML>");
            for (int ch : reroutedChannels)
            {
                RhythmVoice rv = midiMix.getRhythmVoice(ch);
                sb.append("Channel ").append(ch + 1).append(" : ").append(rv.getName());
                if (ch != reroutedChannels.get(reroutedChannels.size() - 1))
                {
                    sb.append("<br/>");
                }
            }
            sb.append("</HTML>");
            reroutedChannelsText = sb.toString();
        }
        lbl_reroutedChannelsTitle.setText(reroutedChannelsTitle);
        lbl_reroutedChannels.setText(reroutedChannelsText);

        cb_rememberMyChoice.setSelected(false);

        pack();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * <p>
     * WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        cb_rememberMyChoice = new javax.swing.JCheckBox();
        lbl_firstLine = new javax.swing.JLabel();
        lbl_fixInstrumentTitle = new javax.swing.JLabel();
        pnl_buttons = new javax.swing.JPanel();
        btn_fix = new javax.swing.JButton();
        btn_skip = new javax.swing.JButton();
        lbl_fixedInstruments = new javax.swing.JLabel();
        lbl_reroutedChannelsTitle = new javax.swing.JLabel();
        lbl_reroutedChannels = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        org.openide.awt.Mnemonics.setLocalizedText(cb_rememberMyChoice, org.openide.util.NbBundle.getMessage(FixMidiMixDialog.class, "FixMidiMixDialog.cb_rememberMyChoice.text")); // NOI18N

        lbl_firstLine.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_firstLine, org.openide.util.NbBundle.getMessage(FixMidiMixDialog.class, "FixMidiMixDialog.lbl_firstLine.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_fixInstrumentTitle, org.openide.util.NbBundle.getMessage(FixMidiMixDialog.class, "FixMidiMixDialog.lbl_fixInstrumentTitle.text")); // NOI18N

        pnl_buttons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 20, 5));

        btn_fix.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(btn_fix, org.openide.util.NbBundle.getMessage(FixMidiMixDialog.class, "FixMidiMixDialog.btn_fix.text")); // NOI18N
        btn_fix.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_fixActionPerformed(evt);
            }
        });
        pnl_buttons.add(btn_fix);

        org.openide.awt.Mnemonics.setLocalizedText(btn_skip, org.openide.util.NbBundle.getMessage(FixMidiMixDialog.class, "FixMidiMixDialog.btn_skip.text")); // NOI18N
        btn_skip.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_skipActionPerformed(evt);
            }
        });
        pnl_buttons.add(btn_skip);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_fixedInstruments, "jLabel1"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_reroutedChannelsTitle, org.openide.util.NbBundle.getMessage(FixMidiMixDialog.class, "FixMidiMixDialog.lbl_reroutedChannelsTitle.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_reroutedChannels, "jLabel3"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_buttons, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lbl_firstLine, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lbl_reroutedChannelsTitle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_fixInstrumentTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cb_rememberMyChoice)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(30, 30, 30)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbl_reroutedChannels)
                                            .addComponent(lbl_fixedInstruments))))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lbl_firstLine)
                .addGap(18, 18, 18)
                .addComponent(lbl_fixInstrumentTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_fixedInstruments)
                .addGap(12, 12, 12)
                .addComponent(lbl_reroutedChannelsTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_reroutedChannels)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addComponent(pnl_buttons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21)
                .addComponent(cb_rememberMyChoice)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

   private void btn_skipActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_skipActionPerformed
   {//GEN-HEADEREND:event_btn_skipActionPerformed
       choice = FixChoice.DONT_FIX;
       setVisible(false);
   }//GEN-LAST:event_btn_skipActionPerformed

   private void btn_fixActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_fixActionPerformed
   {//GEN-HEADEREND:event_btn_fixActionPerformed

       choice = FixChoice.FIX;
       setVisible(false);
   }//GEN-LAST:event_btn_fixActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_fix;
    private javax.swing.JButton btn_skip;
    private javax.swing.JCheckBox cb_rememberMyChoice;
    private javax.swing.JLabel lbl_firstLine;
    private javax.swing.JLabel lbl_fixInstrumentTitle;
    private javax.swing.JLabel lbl_fixedInstruments;
    private javax.swing.JLabel lbl_reroutedChannels;
    private javax.swing.JLabel lbl_reroutedChannelsTitle;
    private javax.swing.JPanel pnl_buttons;
    // End of variables declaration//GEN-END:variables

}
