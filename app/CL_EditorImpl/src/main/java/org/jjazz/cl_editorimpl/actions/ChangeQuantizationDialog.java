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
package org.jjazz.cl_editorimpl.actions;

import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.jjazz.harmony.api.SymbolicDuration;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.uiutilities.api.NoteIcons;
import org.openide.windows.WindowManager;

/**
 *
 */
public class ChangeQuantizationDialog extends javax.swing.JDialog
{

    public enum ExitStatus
    {
        CANCEL, OK_CURRENT_SECTION, OK_ALL_SECTIONS
    }
    private static ChangeQuantizationDialog INSTANCE;
    private ExitStatus exitStatus;
    private CLI_Section section;
    private static final Logger LOGGER = Logger.getLogger(ChangeQuantizationDialog.class.getSimpleName());

    public static ChangeQuantizationDialog getInstance()
    {
        synchronized (ChangeQuantizationDialog.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ChangeQuantizationDialog(WindowManager.getDefault().getMainWindow(), true);
            }
        }
        return INSTANCE;
    }

    private ChangeQuantizationDialog(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();
    }

    public ExitStatus getExitStatus()
    {
        return exitStatus;
    }

    /**
     * Last section used by initialiaze()
     *
     * @return
     */
    public CLI_Section getSection()
    {
        return section;
    }

    /**
     * Preset the dialog.
     *
     * @param section
     * @param q       Can be null (auto-mode)
     */
    public void preset(CLI_Section section, Quantization q)
    {
        Objects.requireNonNull(section);

        exitStatus = ExitStatus.CANCEL;
        this.section = section;
        String text = section.getData().getName() + " (" + section.getData().getTimeSignature() + ")";
        this.lbl_section.setText(text);

        switch (q)
        {
            case null -> this.rbtn_auto.setSelected(true);
            case HALF_BAR -> this.rbtn_halfBar.setSelected(true);
            case BEAT -> this.rbtn_beat.setSelected(true);
            case HALF_BEAT -> this.rbtn_eighth.setSelected(true);
            case ONE_THIRD_BEAT -> this.rbtn_eighth_triplet.setSelected(true);
            case ONE_QUARTER_BEAT -> this.rbtn_sixteenth.setSelected(true);
            default -> this.rbtn_auto.setSelected(true);
        }
        cbx_allSections.setSelected(false);
    }

    /**
     *
     * @return Can be null (auto mode).
     */
    public Quantization getQuantization()
    {
        Quantization q;
        if (this.rbtn_halfBar.isSelected())
        {
            q = Quantization.HALF_BAR;
        } else if (this.rbtn_beat.isSelected())
        {
            q = Quantization.BEAT;
        } else if (this.rbtn_eighth.isSelected())
        {
            q = Quantization.HALF_BEAT;
        } else if (this.rbtn_eighth_triplet.isSelected())
        {
            q = Quantization.ONE_THIRD_BEAT;
        } else if (this.rbtn_sixteenth.isSelected())
        {
            q = Quantization.ONE_QUARTER_BEAT;
        } else
        {
            q = null;
        }
        return q;
    }

    public void cleanup()
    {
        section = null;
    }

    /**
     * Overridden to add global key bindings
     *
     * @return
     */
    @Override
    protected JRootPane createRootPane()
    {
        JRootPane contentPane = new JRootPane();
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "actionOk");
        contentPane.getActionMap().put("actionOk", new AbstractAction("OK")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                btn_OkActionPerformed(null);
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "actionCancel");
        contentPane.getActionMap().put("actionCancel", new AbstractAction("Cancel")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                btn_CancelActionPerformed(null);
            }
        });
        return contentPane;
    }

    /**
     * This method is called from within the constructor to preset the form. WARNING: Do NOT modify this code. The content of this method is always regenerated
     * by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        noteBtnGroup = new javax.swing.ButtonGroup();
        btn_Cancel = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.flatcomponents.api.HelpTextArea();
        btn_Ok = new javax.swing.JButton();
        pnl_radioButtons = new javax.swing.JPanel();
        rbtn_auto = new javax.swing.JRadioButton();
        pnl_halfBar = new javax.swing.JPanel();
        rbtn_halfBar = new javax.swing.JRadioButton();
        lbl_halfBar1 = new javax.swing.JLabel();
        lbl_halfBar2 = new javax.swing.JLabel();
        pnl_beat = new javax.swing.JPanel();
        rbtn_beat = new javax.swing.JRadioButton();
        lbl_beat1 = new javax.swing.JLabel();
        lbl_beat2 = new javax.swing.JLabel();
        pnl_half = new javax.swing.JPanel();
        rbtn_eighth = new javax.swing.JRadioButton();
        lbl_half = new javax.swing.JLabel();
        pnl_halfTriplet = new javax.swing.JPanel();
        rbtn_eighth_triplet = new javax.swing.JRadioButton();
        lbl_halfTriplet = new javax.swing.JLabel();
        pnl_sixteenth = new javax.swing.JPanel();
        rbtn_sixteenth = new javax.swing.JRadioButton();
        lbl_sixteenth = new javax.swing.JLabel();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        cbx_allSections = new javax.swing.JCheckBox();
        lbl_section = new javax.swing.JLabel();

        setTitle(org.openide.util.NbBundle.getMessage(ChangeQuantizationDialog.class, "ChangeQuantizationDialog.title_1")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(ChangeQuantizationDialog.class, "ChangeQuantizationDialog.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        jScrollPane1.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(5);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(ChangeQuantizationDialog.class, "ChangeQuantizationDialog.helpTextArea1.text")); // NOI18N
        jScrollPane1.setViewportView(helpTextArea1);

        org.openide.awt.Mnemonics.setLocalizedText(btn_Ok, "OK"); // NOI18N
        btn_Ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OkActionPerformed(evt);
            }
        });

        pnl_radioButtons.setLayout(new javax.swing.BoxLayout(pnl_radioButtons, javax.swing.BoxLayout.Y_AXIS));

        noteBtnGroup.add(rbtn_auto);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_auto, org.openide.util.NbBundle.getMessage(ChangeQuantizationDialog.class, "ChangeQuantizationDialog.rbtn_auto.text")); // NOI18N
        rbtn_auto.setToolTipText(org.openide.util.NbBundle.getMessage(ChangeQuantizationDialog.class, "ChangeQuantizationDialog.rbtn_auto.toolTipText")); // NOI18N
        pnl_radioButtons.add(rbtn_auto);

        pnl_halfBar.setToolTipText(org.openide.util.NbBundle.getMessage(ChangeQuantizationDialog.class, "ChangeQuantizationDialog.pnl_halfBar.toolTipText")); // NOI18N
        pnl_halfBar.setAlignmentX(0.0F);
        pnl_halfBar.setLayout(new javax.swing.BoxLayout(pnl_halfBar, javax.swing.BoxLayout.LINE_AXIS));

        noteBtnGroup.add(rbtn_halfBar);
        pnl_halfBar.add(rbtn_halfBar);

        lbl_halfBar1.setIcon(NoteIcons.get20x30(SymbolicDuration.HALF));
        lbl_halfBar1.setAlignmentY(0.7F);
        pnl_halfBar.add(lbl_halfBar1);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_halfBar2, org.openide.util.NbBundle.getMessage(ChangeQuantizationDialog.class, "ChangeQuantizationDialog.lbl_halfBar2.text")); // NOI18N
        pnl_halfBar.add(lbl_halfBar2);

        pnl_radioButtons.add(pnl_halfBar);

        pnl_beat.setToolTipText(org.openide.util.NbBundle.getMessage(ChangeQuantizationDialog.class, "ChangeQuantizationDialog.pnl_beat.toolTipText")); // NOI18N
        pnl_beat.setAlignmentX(0.0F);
        pnl_beat.setLayout(new javax.swing.BoxLayout(pnl_beat, javax.swing.BoxLayout.LINE_AXIS));

        noteBtnGroup.add(rbtn_beat);
        pnl_beat.add(rbtn_beat);

        lbl_beat1.setIcon(NoteIcons.get20x30(SymbolicDuration.QUARTER));
        lbl_beat1.setAlignmentY(0.7F);
        pnl_beat.add(lbl_beat1);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_beat2, org.openide.util.NbBundle.getMessage(ChangeQuantizationDialog.class, "ChangeQuantizationDialog.lbl_beat2.text")); // NOI18N
        pnl_beat.add(lbl_beat2);

        pnl_radioButtons.add(pnl_beat);

        pnl_half.setAlignmentX(0.0F);
        pnl_half.setLayout(new javax.swing.BoxLayout(pnl_half, javax.swing.BoxLayout.LINE_AXIS));

        noteBtnGroup.add(rbtn_eighth);
        pnl_half.add(rbtn_eighth);

        lbl_half.setIcon(NoteIcons.get20x30(SymbolicDuration.EIGHTH));
        lbl_half.setAlignmentY(0.7F);
        pnl_half.add(lbl_half);

        pnl_radioButtons.add(pnl_half);

        pnl_halfTriplet.setAlignmentX(0.0F);
        pnl_halfTriplet.setLayout(new javax.swing.BoxLayout(pnl_halfTriplet, javax.swing.BoxLayout.LINE_AXIS));

        noteBtnGroup.add(rbtn_eighth_triplet);
        pnl_halfTriplet.add(rbtn_eighth_triplet);

        lbl_halfTriplet.setIcon(NoteIcons.get20x30(SymbolicDuration.EIGHTH_TRIPLET));
        lbl_halfTriplet.setAlignmentY(0.7F);
        pnl_halfTriplet.add(lbl_halfTriplet);

        pnl_radioButtons.add(pnl_halfTriplet);

        pnl_sixteenth.setAlignmentX(0.0F);
        pnl_sixteenth.setLayout(new javax.swing.BoxLayout(pnl_sixteenth, javax.swing.BoxLayout.LINE_AXIS));

        noteBtnGroup.add(rbtn_sixteenth);
        pnl_sixteenth.add(rbtn_sixteenth);

        lbl_sixteenth.setIcon(NoteIcons.get20x30(SymbolicDuration.SIXTEENTH));
        lbl_sixteenth.setAlignmentY(0.7F);
        pnl_sixteenth.add(lbl_sixteenth);

        pnl_radioButtons.add(pnl_sixteenth);
        pnl_radioButtons.add(filler6);

        org.openide.awt.Mnemonics.setLocalizedText(cbx_allSections, org.openide.util.NbBundle.getMessage(ChangeQuantizationDialog.class, "ChangeQuantizationDialog.cbx_allSections.text")); // NOI18N

        lbl_section.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_section, "SectionName"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(pnl_radioButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lbl_section)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(cbx_allSections))))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_Ok)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_Cancel, btn_Ok});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_section)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 42, Short.MAX_VALUE)
                        .addComponent(cbx_allSections))
                    .addComponent(pnl_radioButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(7, 7, 7)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_Cancel)
                    .addComponent(btn_Ok))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_OkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_OkActionPerformed
    {//GEN-HEADEREND:event_btn_OkActionPerformed
        exitStatus = this.cbx_allSections.isSelected() ? ExitStatus.OK_ALL_SECTIONS : ExitStatus.OK_CURRENT_SECTION;
        setVisible(false);
    }//GEN-LAST:event_btn_OkActionPerformed

    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
    {//GEN-HEADEREND:event_btn_CancelActionPerformed
        exitStatus = ExitStatus.CANCEL;
        setVisible(false);
    }//GEN-LAST:event_btn_CancelActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JCheckBox cbx_allSections;
    private javax.swing.Box.Filler filler6;
    private org.jjazz.flatcomponents.api.HelpTextArea helpTextArea1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_beat1;
    private javax.swing.JLabel lbl_beat2;
    private javax.swing.JLabel lbl_half;
    private javax.swing.JLabel lbl_halfBar1;
    private javax.swing.JLabel lbl_halfBar2;
    private javax.swing.JLabel lbl_halfTriplet;
    private javax.swing.JLabel lbl_section;
    private javax.swing.JLabel lbl_sixteenth;
    private javax.swing.ButtonGroup noteBtnGroup;
    private javax.swing.JPanel pnl_beat;
    private javax.swing.JPanel pnl_half;
    private javax.swing.JPanel pnl_halfBar;
    private javax.swing.JPanel pnl_halfTriplet;
    private javax.swing.JPanel pnl_radioButtons;
    private javax.swing.JPanel pnl_sixteenth;
    private javax.swing.JRadioButton rbtn_auto;
    private javax.swing.JRadioButton rbtn_beat;
    private javax.swing.JRadioButton rbtn_eighth;
    private javax.swing.JRadioButton rbtn_eighth_triplet;
    private javax.swing.JRadioButton rbtn_halfBar;
    private javax.swing.JRadioButton rbtn_sixteenth;
    // End of variables declaration//GEN-END:variables

}
