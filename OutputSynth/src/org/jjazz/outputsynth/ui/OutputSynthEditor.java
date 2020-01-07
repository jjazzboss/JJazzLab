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
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;
import org.jjazz.midi.GSSynth;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.StdSynth;
import org.jjazz.midi.ui.InstrumentTable;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.OutputSynthManager;

/**
 * An editor for an OutputSynth.
 */
public class OutputSynthEditor extends javax.swing.JPanel implements PropertyChangeListener
{

    private OutputSynth outputSynth;
    private MidiSynth editorStdSynth = new StdSynthProxy("Standard Synth", "");
    private static final Logger LOGGER = Logger.getLogger(OutputSynthEditor.class.getSimpleName());
    private boolean exitedOk;

    /**
     * Creates new form OutputSynthEditor
     */
    public OutputSynthEditor()
    {
        initComponents();
        this.list_Banks.setCellRenderer(new BankCellRenderer());
        this.list_MidiSynths.setCellRenderer(new SynthCellRenderer());
    }

    /**
     * Must be called before making the editor visible.
     * <p>
     * outSynth will be directly modified by this editor.
     *
     * @param outSynth
     */
    public void preset(OutputSynth outSynth)
    {
        if (outSynth == null)
        {
            throw new IllegalArgumentException("outSynth=" + outSynth);
        }
        if (outputSynth != null)
        {
            clean();
        }

        outputSynth = outSynth;

        // Register for changes
        outputSynth.addPropertyChangeListener(this);
        outputSynth.getGMRemapTable().addPropertyChangeListener(this);

        /// Update UI
        refreshSynthList();
        refreshCompatibilityCheckBoxes();
        remapTable.setPrimaryModel(outputSynth.getGMRemapTable());
    }

    /**
     * Must be called when this editor is disposed.
     */
    public void clean()
    {
        if (outputSynth != null)
        {
            outputSynth.removePropertyChangeListener(this);
            outputSynth.getGMRemapTable().removePropertyChangeListener(this);
        }
    }

    // ==============================================================================
    // PropertyChangeListener interface
    // ==============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == outputSynth)
        {
            if (evt.getPropertyName() == OutputSynth.PROP_STD_BANK)
            {
                refreshCompatibilityCheckBoxes();
                // Stdsynth must be the first : make sure it is repaint to have the right size displayed
                list_MidiSynths.repaint(list_MidiSynths.getCellBounds(0, 0));
                if (list_MidiSynths.getSelectedValue() == editorStdSynth)
                {
                    refreshBankList();
                }
            } else if (evt.getPropertyName() == OutputSynth.PROP_CUSTOM_SYNTH)
            {
                refreshSynthList();

            } else if (evt.getPropertyName() == OutputSynth.PROP_USER_INSTRUMENT)
            {
            }
        }
    }

    // ==============================================================================
    // Private methods
    // ==============================================================================  
    private void refreshCompatibilityCheckBoxes()
    {
        // The compatibility checkboxes
        List<InstrumentBank<?>> stdBanks = outputSynth.getCompatibleStdBanks();
        cb_GM.setSelected(stdBanks.contains(StdSynth.getGM1Bank()));
        cb_GM2.setSelected(stdBanks.contains(StdSynth.getGM2Bank()));
        cb_XG.setSelected(stdBanks.contains(StdSynth.getXGBank()));
        cb_GS.setSelected(stdBanks.contains(GSSynth.getGSBank()));
        cb_GS.setEnabled(!(cb_GM2.isSelected() || cb_XG.isSelected()));
        cb_GM2.setEnabled(!cb_GS.isSelected());
        cb_XG.setEnabled(!cb_GS.isSelected());
    }

    private void refreshSynthList()
    {
        List<MidiSynth> synths = new ArrayList<>();
        synths.add(editorStdSynth);
        synths.addAll(outputSynth.getCustomSynths());
        list_MidiSynths.setListData(synths.toArray(new MidiSynth[0]));
        if (!synths.isEmpty())
        {
            list_MidiSynths.setSelectedIndex(0);    // This will trigger an update of listBank
        } else
        {
            refreshBankList();
        }
    }

    /**
     * Update the Banks JList contents depending on the MidiSynth selection.
     * <p>
     */
    private void refreshBankList()
    {
        MidiSynth synth = list_MidiSynths.getSelectedValue();
        InstrumentBank<?>[] banks;
        if (synth == null)
        {
            banks = new InstrumentBank<?>[0];
        } else
        {
            banks = synth.getBanks().toArray(new InstrumentBank<?>[0]);
        }
        list_Banks.setListData(banks);
        if (banks.length > 0)
        {
            list_Banks.setSelectedIndex(0);
        }
    }

    // ==============================================================================
    // Private classes
    // ==============================================================================
    /**
     * A virtual StdSynth to show the standard compatible banks of outputSynth.
     */
    private class StdSynthProxy extends MidiSynth
    {

        public StdSynthProxy(String name, String manufacturer)
        {
            super(name, manufacturer);
        }

        @Override
        public List<InstrumentBank<?>> getBanks()
        {
            List<InstrumentBank<?>> banks = outputSynth.getCompatibleStdBanks();
            Collections.sort(banks, new Comparator<InstrumentBank<?>>()
            {
                @Override
                public int compare(InstrumentBank<?> b, InstrumentBank<?> b1)
                {
                    return b.getName().compareTo(b1.getName());

                }
            });
            return banks;
        }
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
            s = (f == null) ? "Builtin" : f.getName();
            if (synth == editorStdSynth)
            {
                setFont(getFont().deriveFont(Font.ITALIC));
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
            if (StdSynth.getInstance().getBanks().contains(bank) || bank == GSSynth.getGSBank())
            {
                Font ft = getFont();
                // setFont(ft.deriveFont(Font.ITALIC));
            }
            return c;
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

        jPanel1 = new javax.swing.JPanel();
        tabPane = new javax.swing.JTabbedPane();
        pnl_main = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_Banks = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        list_MidiSynths = new javax.swing.JList<>();
        jLabel4 = new javax.swing.JLabel();
        btn_AddSynth = new javax.swing.JButton();
        btn_RemoveSynth = new javax.swing.JButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        tbl_Instruments = new org.jjazz.midi.ui.InstrumentTable();
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

        list_MidiSynths.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_MidiSynths.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyPressed(java.awt.event.KeyEvent evt)
            {
                list_MidiSynthsKeyPressed(evt);
            }
        });
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
        btn_AddSynth.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_AddSynthActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_RemoveSynth, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_RemoveSynth.text")); // NOI18N
        btn_RemoveSynth.setToolTipText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_RemoveSynth.toolTipText")); // NOI18N
        btn_RemoveSynth.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_RemoveSynthActionPerformed(evt);
            }
        });

        jScrollPane7.setViewportView(tbl_Instruments);

        javax.swing.GroupLayout pnl_mainLayout = new javax.swing.GroupLayout(pnl_main);
        pnl_main.setLayout(pnl_mainLayout);
        pnl_mainLayout.setHorizontalGroup(
            pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_mainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pnl_mainLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_AddSynth)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_RemoveSynth))
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_mainLayout.createSequentialGroup()
                        .addGap(43, 43, 43)
                        .addComponent(jLabel2))
                    .addGroup(pnl_mainLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
                    .addGroup(pnl_mainLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
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
                    .addComponent(jScrollPane6)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
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
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 765, Short.MAX_VALUE)
                    .addComponent(jScrollPane3))
                .addContainerGap())
        );
        pnl_defaultInstrumentsLayout.setVerticalGroup(
            pnl_defaultInstrumentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_defaultInstrumentsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
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
        cb_GM2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_GM2ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_XG, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.cb_XG.text")); // NOI18N
        cb_XG.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_XGActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_GS, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.cb_GS.text")); // NOI18N
        cb_GS.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_GSActionPerformed(evt);
            }
        });

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
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 430, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
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
        if (!cb_GM.isSelected())
        {
            outputSynth.removeCompatibleStdBank(StdSynth.getGM1Bank());
        } else
        {
            outputSynth.addCompatibleStdBank(StdSynth.getGM1Bank());
        }
    }//GEN-LAST:event_cb_GMActionPerformed

    private void list_MidiSynthsValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_MidiSynthsValueChanged
    {//GEN-HEADEREND:event_list_MidiSynthsValueChanged
        // Update the list of banks
        if (evt.getValueIsAdjusting())
        {
            return;
        }
        MidiSynth synth = list_MidiSynths.getSelectedValue();
        refreshBankList();
        btn_RemoveSynth.setEnabled(synth != null && synth != editorStdSynth);
    }//GEN-LAST:event_list_MidiSynthsValueChanged

    private void list_BanksValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_BanksValueChanged
    {//GEN-HEADEREND:event_list_BanksValueChanged
        // Update the bank details
        if (evt.getValueIsAdjusting())
        {
            return;
        }
        InstrumentBank bank = list_Banks.getSelectedValue();
        tbl_Instruments.setInstruments((bank != null) ? bank.getInstruments() : new ArrayList<>());
    }//GEN-LAST:event_list_BanksValueChanged

    private void cb_GM2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_GM2ActionPerformed
    {//GEN-HEADEREND:event_cb_GM2ActionPerformed
        if (!cb_GM2.isSelected())
        {
            outputSynth.removeCompatibleStdBank(StdSynth.getGM2Bank());
        } else
        {
            outputSynth.addCompatibleStdBank(StdSynth.getGM2Bank());
        }
    }//GEN-LAST:event_cb_GM2ActionPerformed

    private void cb_XGActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_XGActionPerformed
    {//GEN-HEADEREND:event_cb_XGActionPerformed
        if (!cb_XG.isSelected())
        {
            outputSynth.removeCompatibleStdBank(StdSynth.getXGBank());
        } else
        {
            outputSynth.addCompatibleStdBank(StdSynth.getXGBank());
        }
    }//GEN-LAST:event_cb_XGActionPerformed

    private void cb_GSActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_GSActionPerformed
    {//GEN-HEADEREND:event_cb_GSActionPerformed
        if (!cb_GS.isSelected())
        {
            outputSynth.removeCompatibleStdBank(GSSynth.getGSBank());
        } else
        {
            outputSynth.addCompatibleStdBank(GSSynth.getGSBank());
        }
    }//GEN-LAST:event_cb_GSActionPerformed

    private void btn_RemoveSynthActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_RemoveSynthActionPerformed
    {//GEN-HEADEREND:event_btn_RemoveSynthActionPerformed
        MidiSynth synth = list_MidiSynths.getSelectedValue();
        if (synth != null && synth != editorStdSynth)
        {
            int newSelIndex = Math.min(list_MidiSynths.getSelectedIndex(), list_MidiSynths.getModel().getSize() - 2);
            outputSynth.removeCustomSynth(synth);   // This will update the list
            list_MidiSynths.setSelectedIndex(newSelIndex);
        }
    }//GEN-LAST:event_btn_RemoveSynthActionPerformed

    private void btn_AddSynthActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_AddSynthActionPerformed
    {//GEN-HEADEREND:event_btn_AddSynthActionPerformed
        List<MidiSynth> synths = OutputSynthManager.getInstance().showAddCustomSynthDialog();
        for (MidiSynth synth : synths)
        {
            outputSynth.addCustomSynth(synth);
        }
    }//GEN-LAST:event_btn_AddSynthActionPerformed

    private void list_MidiSynthsKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_list_MidiSynthsKeyPressed
    {//GEN-HEADEREND:event_list_MidiSynthsKeyPressed
        LOGGER.severe("evt=" + evt.getKeyChar() + " evtcode=" + evt.getKeyCode());
        if (evt.getKeyCode() == KeyEvent.VK_DELETE)
        {
            btn_RemoveSynthActionPerformed(null);
        }
    }//GEN-LAST:event_list_MidiSynthsKeyPressed


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
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JList<InstrumentBank<?>> list_Banks;
    private javax.swing.JList<MidiSynth> list_MidiSynths;
    private javax.swing.JPanel pnl_Compatibility;
    private javax.swing.JPanel pnl_defaultInstruments;
    private javax.swing.JPanel pnl_main;
    private org.jjazz.outputsynth.ui.RemapTableUI remapTable;
    private javax.swing.JTabbedPane tabPane;
    private org.jjazz.midi.ui.InstrumentTable tbl_Instruments;
    // End of variables declaration//GEN-END:variables
}
