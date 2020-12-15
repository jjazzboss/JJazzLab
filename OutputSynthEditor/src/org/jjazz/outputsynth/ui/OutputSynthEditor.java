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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.instrumentchooser.spi.InstrumentChooserDialog;
import org.jjazz.midi.synths.GSSynth;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.synths.GM1Instrument;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midi.ui.InstrumentTable;
import org.jjazz.midimix.UserChannelRvKey;
import org.jjazz.midisynthmanager.api.MidiSynthManager;
import org.jjazz.musiccontrol.TestPlayer;
import org.jjazz.outputsynth.GMRemapTable;
import org.jjazz.outputsynth.GMRemapTable.ArgumentsException;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.OutputSynth.SendModeOnUponStartup;
import org.jjazz.outputsynth.ui.spi.RemapTableInstrumentChooser;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.uisettings.GeneralUISettings;
import org.jjazz.util.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 * An editor for an OutputSynth.
 */
public class OutputSynthEditor extends javax.swing.JPanel implements PropertyChangeListener, ListSelectionListener
{

    private OutputSynth outputSynth;
    private MidiSynth editorStdSynth = new StdSynthProxy("Standard", "");
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
        this.tbl_Instruments.setHiddenColumns(Arrays.asList(InstrumentTable.Model.COL_SYNTH, InstrumentTable.Model.COL_BANK));
        this.tbl_Instruments.getSelectionModel().addListSelectionListener(this);
        this.tbl_Remap.getSelectionModel().addListSelectionListener(this);

        // To catch double clicks        
        this.tbl_Instruments.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                handleTableMouseClicked(e);
            }
        });
        this.tbl_Remap.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                handleTableMouseClicked(e);
            }
        });

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
            throw new IllegalArgumentException("outSynth=" + outSynth);   //NOI18N
        }

        clean();        // Unregister outputSynth

        outputSynth = outSynth;

        // Register for changes
        outputSynth.addPropertyChangeListener(this);
        outputSynth.getGMRemapTable().addPropertyChangeListener(this);

        /// Update UI
        refreshSynthList();
        refreshCompatibilityCheckBoxes();
        combo_sendMessageUponPlay.setSelectedItem(outputSynth.getSendModeOnUponPlay());
        btn_userInstrument.setText(outputSynth.getUserInstrument().getPatchName());
        btn_userInstrument.setToolTipText(outputSynth.getUserInstrument().getFullName());
        btn_Hear.setEnabled(false);
        tbl_Remap.setPrimaryModel(outputSynth.getGMRemapTable());
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

    // ===================================================================================
    // ListSelectionListener interfacce
    // ===================================================================================
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        LOGGER.log(Level.FINE, "valueChanged() e={0}", e);   //NOI18N
        if (e.getValueIsAdjusting())
        {
            return;
        }
        if (e.getSource() == tbl_Instruments.getSelectionModel())
        {
            btn_Hear.setEnabled(tbl_Instruments.getSelectedInstrument() != null);
        } else if (e.getSource() == tbl_Remap.getSelectionModel())
        {
            refreshRemapButtons();
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
            if (evt.getPropertyName().equals(OutputSynth.PROP_STD_BANK))
            {
                refreshCompatibilityCheckBoxes();
                // Stdsynth must be the first : make sure it is repaint to have the right size displayed
                list_MidiSynths.repaint(list_MidiSynths.getCellBounds(0, 0));
                if (list_MidiSynths.getSelectedValue() == editorStdSynth)
                {
                    refreshBankList();
                }
            } else if (evt.getPropertyName().equals(OutputSynth.PROP_CUSTOM_SYNTH))
            {
                refreshSynthList();
            } else if (evt.getPropertyName().equals(OutputSynth.PROP_SEND_MSG_UPON_STARTUP))
            {
                this.combo_sendMessageUponPlay.setSelectedItem(outputSynth.getSendModeOnUponPlay());
            } else if (evt.getPropertyName().equals(OutputSynth.PROP_USER_INSTRUMENT))
            {
                this.btn_userInstrument.setText(outputSynth.getUserInstrument().getPatchName());
                this.btn_userInstrument.setToolTipText(outputSynth.getUserInstrument().getFullName());
            }
        } else if (evt.getSource() == outputSynth.getGMRemapTable())
        {
            refreshRemapButtons();
        }
    }

    // ==============================================================================
    // Private methods
    // ==============================================================================  
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
        } else if (evt.getSource() == this.tbl_Instruments)
        {
            if (SwingUtilities.isLeftMouseButton(evt))
            {
                if (evt.getClickCount() == 2 || (evt.getClickCount() == 1 && shift))
                {
                    btn_HearActionPerformed(null);
                }
            }
        }
    }

    private void refreshCompatibilityCheckBoxes()
    {
        // The compatibility checkboxes
        List<InstrumentBank<?>> stdBanks = outputSynth.getCompatibleStdBanks();
        cb_GM.setSelected(stdBanks.contains(StdSynth.getInstance().getGM1Bank()));
        cb_GM2.setSelected(stdBanks.contains(StdSynth.getInstance().getGM2Bank()));
        cb_XG.setSelected(stdBanks.contains(StdSynth.getInstance().getXGBank()));
        cb_GS.setSelected(stdBanks.contains(GSSynth.getInstance().getGSBank()));

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

    private void refreshRemapButtons()
    {
        Instrument remappedIns = tbl_Remap.getSelectedRemappedInstrument();
        btn_changeRemappedIns.setEnabled(remappedIns != null);
        boolean mappingExist = remappedIns != null && outputSynth.getGMRemapTable().getInstrument(remappedIns) != null;
        btn_HearRemap.setEnabled(mappingExist);
        btn_ResetInstrument.setEnabled(mappingExist);
    }

    /**
     * Show a dialog to confirm removal of removedStd.
     *
     * @param removedStr
     * @return
     */
    private boolean confirmRemoval(String removedStr)
    {
        String msg = ResUtil.getString(getClass(), "CTL_ConfirmedDefInstrumentRemoval", removedStr);
        NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
        Object result = DialogDisplayer.getDefault().notify(d);
        return NotifyDescriptor.YES_OPTION == result;
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
            String s = synth.getName() + " (" + synth.getNbInstruments() + ")";
            setText(s);
            File f = synth.getFile();
            s = (f == null) ? ResUtil.getString(getClass(), "BUILTIN", new Object[]
            {
            }) : f.getAbsolutePath();
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
            setToolTipText(ResUtil.getString(getClass(), "CTL_BankSelectMethod", bank.getDefaultBankSelectMethod().toString()));
            if (StdSynth.getInstance().getBanks().contains(bank) || bank == GSSynth.getInstance().getGSBank())
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

        rbtnGroup_SendMsgUponStartup = new javax.swing.ButtonGroup();
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
        btn_Hear = new javax.swing.JButton();
        pnl_defaultInstruments = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.HelpTextArea();
        jScrollPane5 = new javax.swing.JScrollPane();
        tbl_Remap = new org.jjazz.outputsynth.ui.RemapTableUI();
        btn_changeRemappedIns = new javax.swing.JButton();
        btn_HearRemap = new javax.swing.JButton();
        btn_ResetInstrument = new javax.swing.JButton();
        pnl_advanced = new javax.swing.JPanel();
        combo_sendMessageUponPlay = new JComboBox<>(SendModeOnUponStartup.values());
        jLabel1 = new javax.swing.JLabel();
        btn_userInstrument = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        pnl_Compatibility = new javax.swing.JPanel();
        cb_GM = new javax.swing.JCheckBox();
        cb_GM2 = new javax.swing.JCheckBox();
        cb_XG = new javax.swing.JCheckBox();
        cb_GS = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        hlp_area = new org.jjazz.ui.utilities.HelpTextArea();
        lbl_synthImage = new javax.swing.JLabel();

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

        btn_Hear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/outputsynth/ui/resources/SpeakerRed-20x20.png"))); // NOI18N
        btn_Hear.setToolTipText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_Hear.toolTipText")); // NOI18N
        btn_Hear.setDisabledIcon(GeneralUISettings.getInstance().getIcon("speaker.icon.disabled"));
        btn_Hear.setEnabled(false);
        btn_Hear.setIconTextGap(0);
        btn_Hear.setMargin(new java.awt.Insets(2, 4, 2, 4));
        btn_Hear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_HearActionPerformed(evt);
            }
        });

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_mainLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_Hear))
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnl_mainLayout.setVerticalGroup(
            pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_mainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_Hear)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(btn_AddSynth)
                        .addComponent(btn_RemoveSynth)
                        .addComponent(jLabel2)
                        .addComponent(jLabel3)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        tabPane.addTab(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.pnl_main.TabConstraints.tabTitle"), pnl_main); // NOI18N

        jScrollPane3.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(5);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.helpTextArea1.text")); // NOI18N
        jScrollPane3.setViewportView(helpTextArea1);

        jScrollPane5.setViewportView(tbl_Remap);

        org.openide.awt.Mnemonics.setLocalizedText(btn_changeRemappedIns, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_changeRemappedIns.text")); // NOI18N
        btn_changeRemappedIns.setToolTipText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_changeRemappedIns.toolTipText")); // NOI18N
        btn_changeRemappedIns.setActionCommand(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_changeRemappedIns.actionCommand")); // NOI18N
        btn_changeRemappedIns.setEnabled(false);
        btn_changeRemappedIns.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_changeRemappedInsActionPerformed(evt);
            }
        });

        btn_HearRemap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/outputsynth/ui/resources/SpeakerRed-20x20.png"))); // NOI18N
        btn_HearRemap.setToolTipText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_HearRemap.toolTipText")); // NOI18N
        btn_HearRemap.setDisabledIcon(GeneralUISettings.getInstance().getIcon("speaker.icon.disabled"));
        btn_HearRemap.setEnabled(false);
        btn_HearRemap.setMargin(new java.awt.Insets(2, 4, 2, 4));
        btn_HearRemap.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_HearRemapActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_ResetInstrument, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_ResetInstrument.text")); // NOI18N
        btn_ResetInstrument.setToolTipText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_ResetInstrument.toolTipText")); // NOI18N
        btn_ResetInstrument.setEnabled(false);
        btn_ResetInstrument.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_ResetInstrumentActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_defaultInstrumentsLayout = new javax.swing.GroupLayout(pnl_defaultInstruments);
        pnl_defaultInstruments.setLayout(pnl_defaultInstrumentsLayout);
        pnl_defaultInstrumentsLayout.setHorizontalGroup(
            pnl_defaultInstrumentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_defaultInstrumentsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_defaultInstrumentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5)
                    .addGroup(pnl_defaultInstrumentsLayout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 481, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 101, Short.MAX_VALUE)
                        .addComponent(btn_ResetInstrument)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_changeRemappedIns)
                        .addGap(18, 18, 18)
                        .addComponent(btn_HearRemap)))
                .addContainerGap())
        );
        pnl_defaultInstrumentsLayout.setVerticalGroup(
            pnl_defaultInstrumentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_defaultInstrumentsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_defaultInstrumentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnl_defaultInstrumentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(pnl_defaultInstrumentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_changeRemappedIns)
                            .addComponent(btn_ResetInstrument))
                        .addComponent(btn_HearRemap)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabPane.addTab(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.pnl_defaultInstruments.TabConstraints.tabTitle"), pnl_defaultInstruments); // NOI18N

        combo_sendMessageUponPlay.setToolTipText(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.combo_sendMessageUponPlay.toolTipText")); // NOI18N
        combo_sendMessageUponPlay.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                combo_sendMessageUponPlayActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_userInstrument, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.btn_userInstrument.text")); // NOI18N
        btn_userInstrument.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_userInstrumentActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.jLabel6.text")); // NOI18N

        javax.swing.GroupLayout pnl_advancedLayout = new javax.swing.GroupLayout(pnl_advanced);
        pnl_advanced.setLayout(pnl_advancedLayout);
        pnl_advancedLayout.setHorizontalGroup(
            pnl_advancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_advancedLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(pnl_advancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btn_userInstrument, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(combo_sendMessageUponPlay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_advancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel6))
                .addContainerGap(429, Short.MAX_VALUE))
        );
        pnl_advancedLayout.setVerticalGroup(
            pnl_advancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_advancedLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_advancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(combo_sendMessageUponPlay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(18, 18, 18)
                .addGroup(pnl_advancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(btn_userInstrument))
                .addContainerGap(351, Short.MAX_VALUE))
        );

        tabPane.addTab(org.openide.util.NbBundle.getMessage(OutputSynthEditor.class, "OutputSynthEditor.pnl_advanced.TabConstraints.tabTitle"), pnl_advanced); // NOI18N

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

        lbl_synthImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/outputsynth/ui/resources/OutputSynth2.png"))); // NOI18N

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
                        .addComponent(lbl_synthImage)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane1)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_Compatibility, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_synthImage)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addComponent(tabPane)
                .addContainerGap())
        );

        add(jPanel1, "card2");
    }// </editor-fold>//GEN-END:initComponents

    private void cb_GMActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_GMActionPerformed
    {//GEN-HEADEREND:event_cb_GMActionPerformed
        if (!cb_GM.isSelected())
        {
            if (outputSynth.getGMRemapTable().isUsed(null, StdSynth.getInstance().getGM1Bank()))
            {
                if (!confirmRemoval("the GM bank"))
                {
                    cb_GM.setSelected(true);
                    return;
                }
            }
            if (!outputSynth.removeCompatibleStdBank(StdSynth.getInstance().getGM1Bank()))
            {
                String msg = ResUtil.getString(getClass(), "ERR_CantRemoveGMbank");
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                cb_GM.setSelected(true);
                return;
            }
        } else
        {
            outputSynth.addCompatibleStdBank(StdSynth.getInstance().getGM1Bank());
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
        InstrumentBank<? extends Instrument> bank = list_Banks.getSelectedValue();
        tbl_Instruments.getModel().setInstruments((bank != null) ? bank.getInstruments() : new ArrayList<>());
    }//GEN-LAST:event_list_BanksValueChanged

    private void cb_GM2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_GM2ActionPerformed
    {//GEN-HEADEREND:event_cb_GM2ActionPerformed
        if (!cb_GM2.isSelected())
        {
            if (outputSynth.getGMRemapTable().isUsed(null, StdSynth.getInstance().getGM2Bank()))
            {
                if (!confirmRemoval(ResUtil.getString(getClass(), "THE GM2 BANK")))
                {
                    cb_GM2.setSelected(true);
                    return;
                }
            }
            outputSynth.removeCompatibleStdBank(StdSynth.getInstance().getGM2Bank());
        } else
        {
            outputSynth.addCompatibleStdBank(StdSynth.getInstance().getGM2Bank());
        }
    }//GEN-LAST:event_cb_GM2ActionPerformed

    private void cb_XGActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_XGActionPerformed
    {//GEN-HEADEREND:event_cb_XGActionPerformed
        if (!cb_XG.isSelected())
        {
            if (outputSynth.getGMRemapTable().isUsed(null, StdSynth.getInstance().getXGBank()))
            {
                if (!confirmRemoval(ResUtil.getString(getClass(), "THE XG BANK")))
                {
                    cb_XG.setSelected(true);
                    return;
                }
            }
            outputSynth.removeCompatibleStdBank(StdSynth.getInstance().getXGBank());
        } else
        {
            outputSynth.addCompatibleStdBank(StdSynth.getInstance().getXGBank());
        }
    }//GEN-LAST:event_cb_XGActionPerformed

    private void cb_GSActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_GSActionPerformed
    {//GEN-HEADEREND:event_cb_GSActionPerformed
        if (!cb_GS.isSelected())
        {
            if (outputSynth.getGMRemapTable().isUsed(null, GSSynth.getInstance().getGSBank()))
            {
                if (!confirmRemoval(ResUtil.getString(getClass(), "THE GS BANK")))
                {
                    cb_GS.setSelected(true);
                    return;
                }
            }
            outputSynth.removeCompatibleStdBank(GSSynth.getInstance().getGSBank());
        } else
        {
            outputSynth.addCompatibleStdBank(GSSynth.getInstance().getGSBank());
        }
    }//GEN-LAST:event_cb_GSActionPerformed

    private void btn_RemoveSynthActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_RemoveSynthActionPerformed
    {//GEN-HEADEREND:event_btn_RemoveSynthActionPerformed
        MidiSynth synth = list_MidiSynths.getSelectedValue();
        if (synth != null && synth != editorStdSynth)
        {
            if (outputSynth.getGMRemapTable().isUsed(synth, null))
            {
                if (!confirmRemoval("synth " + synth.getName()))
                {
                    return;
                }
            }
            int newSelIndex = Math.min(list_MidiSynths.getSelectedIndex(), list_MidiSynths.getModel().getSize() - 2);
            outputSynth.removeCustomSynth(synth);   // This will update the list
            list_MidiSynths.setSelectedIndex(newSelIndex);
        }

    }//GEN-LAST:event_btn_RemoveSynthActionPerformed

    private void btn_AddSynthActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_AddSynthActionPerformed
    {//GEN-HEADEREND:event_btn_AddSynthActionPerformed
        MidiSynthManager msm = MidiSynthManager.getDefault();
        File f = msm.showSelectSynthFileDialog();
        if (f != null)
        {
            List<MidiSynth> synths = msm.loadSynths(f);
            for (MidiSynth synth : synths)
            {
                outputSynth.addCustomSynth(synth);
            }
        }
    }//GEN-LAST:event_btn_AddSynthActionPerformed

    private void list_MidiSynthsKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_list_MidiSynthsKeyPressed
    {//GEN-HEADEREND:event_list_MidiSynthsKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_DELETE)
        {
            btn_RemoveSynthActionPerformed(null);
        }
    }//GEN-LAST:event_list_MidiSynthsKeyPressed

    private void btn_HearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_HearActionPerformed
    {//GEN-HEADEREND:event_btn_HearActionPerformed
        Instrument ins = this.tbl_Instruments.getSelectedInstrument();
        if (ins == null || !ins.getMidiAddress().isFullyDefined())
        {
            LOGGER.fine("btn_HearActionPerformed() called but invalid ins=" + ins + " ins.getMidiAddress()=" + (ins != null ? ins.getMidiAddress() : null));   //NOI18N
            return;
        }
        list_MidiSynths.setEnabled(false);
        list_Banks.setEnabled(false);
        tbl_Instruments.setEnabled(false);
        btn_Hear.setEnabled(false);
        tabPane.setEnabled(false);
        cb_GM.setEnabled(false);
        cb_GM2.setEnabled(false);
        cb_XG.setEnabled(false);
        cb_GS.setEnabled(false);

        Runnable endAction = new Runnable()
        {
            @Override
            public void run()
            {
                list_MidiSynths.setEnabled(true);
                list_Banks.setEnabled(true);
                tbl_Instruments.setEnabled(true);
                btn_Hear.setEnabled(true);
                tabPane.setEnabled(true);
                cb_GM.setEnabled(true);
                cb_GM2.setEnabled(true);
                cb_XG.setEnabled(true);
                cb_GS.setEnabled(true);
            }
        };
        // Send MIDI messages for the selected instrument             
        TestPlayer tp = TestPlayer.getInstance();
        try
        {
            final int CHANNEL = ins.isDrumKit() ? MidiConst.CHANNEL_DRUMS : 0;
            final int TRANSPOSE = ins.isDrumKit() ? -24 : 0;
            JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(CHANNEL));
            tp.playTestNotes(CHANNEL, -1, TRANSPOSE, endAction);
        } catch (MusicGenerationException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }

    }//GEN-LAST:event_btn_HearActionPerformed

    private void btn_changeRemappedInsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_changeRemappedInsActionPerformed
    {//GEN-HEADEREND:event_btn_changeRemappedInsActionPerformed
        GMRemapTable rTable = outputSynth.getGMRemapTable();
        Instrument mappedIns = tbl_Remap.getSelectedRemappedInstrument();
        if (mappedIns != null)
        {
            RemapTableInstrumentChooser chooser = RemapTableInstrumentChooser.getDefault();
            chooser.preset(outputSynth, mappedIns);
            chooser.setVisible(true);
            Instrument ins = chooser.getSelectedInstrument();
            if (ins != null)
            {
                try
                {
                    rTable.setInstrument(mappedIns, ins, chooser.useAsFamilyDefault());
                } catch (ArgumentsException ex)
                {
                    NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(d);
                }
            }
        }
    }//GEN-LAST:event_btn_changeRemappedInsActionPerformed

    private void btn_HearRemapActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_HearRemapActionPerformed
    {//GEN-HEADEREND:event_btn_HearRemapActionPerformed
        Instrument remappedIns = tbl_Remap.getSelectedRemappedInstrument();
        if (remappedIns == null)
        {
            LOGGER.fine("btn_HearActionPerformed() called but invalid remappedIns=" + remappedIns);   //NOI18N
            return;
        }
        Instrument ins = outputSynth.getGMRemapTable().getInstrument(remappedIns);
        if (ins == null || !ins.getMidiAddress().isFullyDefined())
        {
            LOGGER.fine("btn_HearActionPerformed() called but invalid ins=" + ins + " ins.getMidiAddress()=" + (ins != null ? ins.getMidiAddress() : ""));   //NOI18N
            return;
        }
        tabPane.setEnabled(false);
        tbl_Remap.setEnabled(false);
        btn_HearRemap.setEnabled(false);
        btn_changeRemappedIns.setEnabled(false);

        Runnable endAction = new Runnable()
        {
            @Override
            public void run()
            {
                tabPane.setEnabled(true);
                tbl_Remap.setEnabled(true);
                btn_HearRemap.setEnabled(true);
                btn_changeRemappedIns.setEnabled(true);
            }
        };
        // Send MIDI messages for the selected instrument             
        TestPlayer tp = TestPlayer.getInstance();
        try
        {
            final int CHANNEL = ins.isDrumKit() ? MidiConst.CHANNEL_DRUMS : 0;
            final int TRANSPOSE = ins.isDrumKit() ? -24 : 0;
            JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(CHANNEL));
            tp.playTestNotes(CHANNEL, -1, TRANSPOSE, endAction);

        } catch (MusicGenerationException ex)
        {
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
        GMRemapTable table = outputSynth.getGMRemapTable();
        Instrument ins = table.getInstrument(remappedIns);
        if (ins == null)
        {
            return;
        }
        boolean useAsFamilyDefault = false;
        if (remappedIns instanceof GM1Instrument)
        {
            GM1Instrument gmIns = (GM1Instrument) remappedIns;
            useAsFamilyDefault = table.getInstrument(gmIns.getFamily()) == ins;
        }
        try
        {
            table.setInstrument(remappedIns, null, useAsFamilyDefault);
        } catch (ArgumentsException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }//GEN-LAST:event_btn_ResetInstrumentActionPerformed

    private void combo_sendMessageUponPlayActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_combo_sendMessageUponPlayActionPerformed
    {//GEN-HEADEREND:event_combo_sendMessageUponPlayActionPerformed
        @SuppressWarnings("unchecked")
        JComboBox<SendModeOnUponStartup> cb = (JComboBox<SendModeOnUponStartup>) evt.getSource();
        SendModeOnUponStartup mode = (SendModeOnUponStartup) cb.getSelectedItem();
        outputSynth.setSendModeOnUponPlay(mode);
    }//GEN-LAST:event_combo_sendMessageUponPlayActionPerformed

    private void btn_userInstrumentActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_userInstrumentActionPerformed
    {//GEN-HEADEREND:event_btn_userInstrumentActionPerformed
        InstrumentChooserDialog dlg = InstrumentChooserDialog.getDefault();
        dlg.preset(outputSynth,
                UserChannelRvKey.getInstance(),
                outputSynth.getUserInstrument(),
                0,
                UserChannelRvKey.getInstance().getPreferredUserChannel());
        dlg.setVisible(true);
        Instrument ins = dlg.getSelectedInstrument();
        if (ins != null)
        {
            outputSynth.setUserInstrument(ins);
        }
    }//GEN-LAST:event_btn_userInstrumentActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_AddSynth;
    private javax.swing.JButton btn_Hear;
    private javax.swing.JButton btn_HearRemap;
    private javax.swing.JButton btn_RemoveSynth;
    private javax.swing.JButton btn_ResetInstrument;
    private javax.swing.JButton btn_changeRemappedIns;
    private javax.swing.JButton btn_userInstrument;
    private javax.swing.JCheckBox cb_GM;
    private javax.swing.JCheckBox cb_GM2;
    private javax.swing.JCheckBox cb_GS;
    private javax.swing.JCheckBox cb_XG;
    private javax.swing.JComboBox<SendModeOnUponStartup> combo_sendMessageUponPlay;
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea1;
    private org.jjazz.ui.utilities.HelpTextArea hlp_area;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JLabel lbl_synthImage;
    private javax.swing.JList<InstrumentBank<? extends Instrument>> list_Banks;
    private javax.swing.JList<MidiSynth> list_MidiSynths;
    private javax.swing.JPanel pnl_Compatibility;
    private javax.swing.JPanel pnl_advanced;
    private javax.swing.JPanel pnl_defaultInstruments;
    private javax.swing.JPanel pnl_main;
    private javax.swing.ButtonGroup rbtnGroup_SendMsgUponStartup;
    private javax.swing.JTabbedPane tabPane;
    private org.jjazz.midi.ui.InstrumentTable tbl_Instruments;
    private org.jjazz.outputsynth.ui.RemapTableUI tbl_Remap;
    // End of variables declaration//GEN-END:variables
}
