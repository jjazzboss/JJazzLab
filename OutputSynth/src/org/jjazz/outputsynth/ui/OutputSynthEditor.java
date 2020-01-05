/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.outputsynth.ui;

import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import org.jjazz.midi.GSSynth;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.StdSynth;
import org.jjazz.outputsynth.OutputSynth;

/**
 *
 * @author Administrateur
 */
public class OutputSynthEditor extends javax.swing.JPanel
{

    private OutputSynth outputSynth;
    private final InstrumentTableModel tableModel = new InstrumentTableModel();

    /** Creates new form OutputSynthEditor */
    public OutputSynthEditor()
    {
        initComponents();

        // Adjust the Instrument JTable 
        TableColumnModel cm = tbl_Instruments.getColumnModel();
        cm.getColumn(0).setPreferredWidth(130); // Using pixels size, NOT nice but simple for a start...
        cm.getColumn(1).setPreferredWidth(20);
        cm.getColumn(2).setPreferredWidth(20);
        cm.getColumn(3).setPreferredWidth(20);

        this.list_Banks.setCellRenderer(new BankCellRenderer());
        this.list_MidiSynths.setCellRenderer(new SynthCellRenderer());
    }

    public void preset(OutputSynth outSynth)
    {
        if (outSynth == null)
        {
            throw new IllegalArgumentException("outSynth=" + outSynth);
        }
        outputSynth = outSynth;

        // Update the UI
        List<MidiSynth> synths = outputSynth.getCustomSynths();
        list_MidiSynths.setListData(synths.toArray(new MidiSynth[0]));
        if (!synths.isEmpty())
        {
            list_MidiSynths.setSelectedIndex(0);
        } else
        {
            updateBankList(null);
        }
        remapTable.setPrimaryModel(outSynth.getGM1RemapTable());        
    }

    private class SynthCellRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component c = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            MidiSynth synth = (MidiSynth) value;
            String s = synth.getName() + " (" + synth.getNbPatches() + ")";
            setText(s);
            File f = synth.getFile();
            if (f == null)
            {
                s = "Builtin";
                Font ft = getFont();
                setFont(ft.deriveFont(Font.ITALIC));
            } else
            {
                s = f.getName();
            }
            setToolTipText(s);
            return c;
        }
    }

    private class BankCellRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            InstrumentBank<?> bank = (InstrumentBank<?>) value;
            setText(bank.getName() + " (" + bank.getSize() + ")");
            setToolTipText("Bank select method: " + bank.getDefaultBankSelectMethod().toString());
            return c;
        }
    }

    private class InstrumentTableModel extends AbstractTableModel
    {

        InstrumentBank<? extends Instrument> bank;
        List<? extends Instrument> instruments;

        public void setBank(InstrumentBank<?> b)
        {
            bank = b;
            if (bank != null)
            {
                instruments = bank.getInstruments();
            }
            fireTableDataChanged();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return columnIndex == 0 ? String.class : Integer.class;
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            String s;
            switch (columnIndex)
            {
                case 3:
                    s = "LSB";
                    break;
                case 2:
                    s = "MSB";
                    break;
                case 1:
                    s = "PC";
                    break;
                case 0:
                    s = "Program Name";
                    break;
                default:
                    throw new IllegalStateException("columnIndex=" + columnIndex);
            }
            return s;
        }

        @Override
        public int getRowCount()
        {
            return bank == null ? 0 : bank.getSize();
        }

        @Override
        public int getColumnCount()
        {
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            Instrument ins = instruments.get(rowIndex);
            switch (columnIndex)
            {
                case 3:
                    return (Integer) ins.getMidiAddress().getBankLSB();
                case 2:
                    return (Integer) ins.getMidiAddress().getBankMSB();
                case 1:
                    return (Integer) ins.getMidiAddress().getProgramChange();
                case 0:
                    return ins.getPatchName();
                default:
                    throw new IllegalStateException("columnIndex=" + columnIndex);
            }
        }
    }

    /** This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jPanel1 = new javax.swing.JPanel();
        tabPane = new javax.swing.JTabbedPane();
        pnl_main = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_Banks = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tbl_Instruments = new javax.swing.JTable();
        jScrollPane6 = new javax.swing.JScrollPane();
        list_MidiSynths = new javax.swing.JList<>();
        jLabel4 = new javax.swing.JLabel();
        btn_AddSynth = new javax.swing.JButton();
        btn_RemoveSynth = new javax.swing.JButton();
        pnl_defaultInstruments = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.HelpTextArea();
        jScrollPane5 = new javax.swing.JScrollPane();
        remapTable = new org.jjazz.outputsynth.ui.RemapTableUI();
        pnl_Compatibility = new javax.swing.JPanel();
        cb_GM = new javax.swing.JCheckBox();
        cb_GM2 = new javax.swing.JCheckBox();
        cb_XG = new javax.swing.JCheckBox();
        cb_GS = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        hlp_area = new org.jjazz.ui.utilities.HelpTextArea();

        setLayout(new java.awt.CardLayout());

        list_Banks.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_Banks.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_BanksValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(list_Banks);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.jLabel3.text")); // NOI18N

        tbl_Instruments.setAutoCreateRowSorter(true);
        tbl_Instruments.setModel(tableModel);
        jScrollPane4.setViewportView(tbl_Instruments);

        list_MidiSynths.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_MidiSynthsValueChanged(evt);
            }
        });
        jScrollPane6.setViewportView(list_MidiSynths);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.jLabel4.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_AddSynth, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_AddSynth.text")); // NOI18N
        btn_AddSynth.setToolTipText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_AddSynth.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_RemoveSynth, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_RemoveSynth.text")); // NOI18N
        btn_RemoveSynth.setToolTipText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_RemoveSynth.toolTipText")); // NOI18N

        javax.swing.GroupLayout pnl_mainLayout = new javax.swing.GroupLayout(pnl_main);
        pnl_main.setLayout(pnl_mainLayout);
        pnl_mainLayout.setHorizontalGroup(
            pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_mainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnl_mainLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_AddSynth)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_RemoveSynth)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_mainLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(0, 188, Short.MAX_VALUE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        pnl_mainLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jScrollPane2, jScrollPane6});

        pnl_mainLayout.setVerticalGroup(
            pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_mainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(btn_AddSynth)
                    .addComponent(btn_RemoveSynth))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 304, Short.MAX_VALUE)
                    .addComponent(jScrollPane2)
                    .addComponent(jScrollPane6))
                .addContainerGap())
        );

        tabPane.addTab(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.pnl_main.TabConstraints.tabTitle"), pnl_main); // NOI18N

        jScrollPane3.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(5);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.helpTextArea1.text")); // NOI18N
        jScrollPane3.setViewportView(helpTextArea1);

        jScrollPane5.setViewportView(remapTable);

        javax.swing.GroupLayout pnl_defaultInstrumentsLayout = new javax.swing.GroupLayout(pnl_defaultInstruments);
        pnl_defaultInstruments.setLayout(pnl_defaultInstrumentsLayout);
        pnl_defaultInstrumentsLayout.setHorizontalGroup(
            pnl_defaultInstrumentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_defaultInstrumentsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_defaultInstrumentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 690, Short.MAX_VALUE)
                    .addComponent(jScrollPane3))
                .addContainerGap())
        );
        pnl_defaultInstrumentsLayout.setVerticalGroup(
            pnl_defaultInstrumentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_defaultInstrumentsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        tabPane.addTab(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.pnl_defaultInstruments.TabConstraints.tabTitle"), pnl_defaultInstruments); // NOI18N

        pnl_Compatibility.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.pnl_Compatibility.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_GM, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.cb_GM.text")); // NOI18N
        cb_GM.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_GMActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_GM2, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.cb_GM2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_XG, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.cb_XG.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_GS, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.cb_GS.text")); // NOI18N

        javax.swing.GroupLayout pnl_CompatibilityLayout = new javax.swing.GroupLayout(pnl_Compatibility);
        pnl_Compatibility.setLayout(pnl_CompatibilityLayout);
        pnl_CompatibilityLayout.setHorizontalGroup(
            pnl_CompatibilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_CompatibilityLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnl_CompatibilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cb_GM)
                    .addComponent(cb_GM2)
                    .addComponent(cb_XG)
                    .addComponent(cb_GS)))
        );
        pnl_CompatibilityLayout.setVerticalGroup(
            pnl_CompatibilityLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_CompatibilityLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cb_GM)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cb_GM2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cb_XG)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cb_GS)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setBorder(null);

        hlp_area.setColumns(20);
        hlp_area.setRows(5);
        hlp_area.setText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.hlp_area.text")); // NOI18N
        jScrollPane1.setViewportView(hlp_area);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tabPane)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(pnl_Compatibility, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(pnl_Compatibility, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabPane)
                .addContainerGap())
        );

        add(jPanel1, "card2");
    }// </editor-fold>//GEN-END:initComponents

    private void cb_GMActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_GMActionPerformed
    {//GEN-HEADEREND:event_cb_GMActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cb_GMActionPerformed

    private void list_MidiSynthsValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_MidiSynthsValueChanged
    {//GEN-HEADEREND:event_list_MidiSynthsValueChanged
        // Update the list of banks
        if (evt.getValueIsAdjusting())
        {
            return;
        }
        MidiSynth synth = list_MidiSynths.getSelectedValue();
        updateBankList(synth);
        btn_RemoveSynth.setEnabled(synth != null);
    }//GEN-LAST:event_list_MidiSynthsValueChanged

    private void list_BanksValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_BanksValueChanged
    {//GEN-HEADEREND:event_list_BanksValueChanged
        // Update the bank details
        if (evt.getValueIsAdjusting())
        {
            return;
        }
        InstrumentBank<?> bank = list_Banks.getSelectedValue();
        tbl_Instruments.setEnabled(bank != null);
        tableModel.setBank(bank);
    }//GEN-LAST:event_list_BanksValueChanged

    /**
     * Update the Banks JList contents to show synth (or null).
     *
     * @param synth Can be null
     */
    private void updateBankList(MidiSynth synth)
    {
        List<InstrumentBank<?>> banks = outputSynth.getCompatibleStdBanks();
        if (synth != null)
        {
            banks.addAll(synth.getBanks());
        }
        list_Banks.setListData(banks.toArray(new InstrumentBank<?>[0]));
        if (!banks.isEmpty())
        {
            list_Banks.setSelectedIndex(0);
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_AddSynth;
    private javax.swing.JButton btn_RemoveSynth;
    private javax.swing.JCheckBox cb_GM;
    private javax.swing.JCheckBox cb_GM2;
    private javax.swing.JCheckBox cb_GS;
    private javax.swing.JCheckBox cb_XG;
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea1;
    private org.jjazz.ui.utilities.HelpTextArea hlp_area;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JList<InstrumentBank<?>> list_Banks;
    private javax.swing.JList<MidiSynth> list_MidiSynths;
    private javax.swing.JPanel pnl_Compatibility;
    private javax.swing.JPanel pnl_defaultInstruments;
    private javax.swing.JPanel pnl_main;
    private org.jjazz.outputsynth.ui.RemapTableUI remapTable;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JTable tbl_Instruments;
    // End of variables declaration//GEN-END:variables
}
