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
package org.jjazz.instrumentchooser;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.Collections;
import javax.swing.DefaultListCellRenderer;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.instrumentchooser.api.InstrumentChooserDialog;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiSynth;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.rhythmmusicgeneration.MusicGenerationException;
import org.jjazz.ui.utilities.HelpTextArea;
import org.jjazz.util.Filter;
import org.openide.*;
import org.openide.windows.WindowManager;

public class InstrumentChooserDialogImpl extends InstrumentChooserDialog implements ChangeListener
{

    private static InstrumentChooserDialogImpl INSTANCE;
    private static Color[] COLORS =
    {
        Color.BLACK, Color.DARK_GRAY, Color.BLUE, Color.RED.darker(), Color.GREEN.darker().darker()
    };
    private DefaultListModel<InstrumentBank<?>> banks = new DefaultListModel<>();
    private DefaultListModel<Instrument> instruments = new DefaultListModel<>();
    private DefaultListModel<MidiSynth> midiSynths = new DefaultListModel<>();
    private Instrument selectedInstrument;
    private WeakHashMap<MidiSynth, Color> mapSynthColor = new WeakHashMap<>();       // Safe to use WeakHashMap
    private FavoriteMidiSynth favoriteSynth;
    private int colorIndex;
    private int channel;
    private Instrument initInstrument;
    private Filter<Instrument> instrumentFilter;
    /**
     * If not null, is used to select only instruments that contain this string.
     */
    private String instrumentSelectString;
    private static final Logger LOGGER = Logger.getLogger(InstrumentChooserDialogImpl.class.getSimpleName());

    static public InstrumentChooserDialogImpl getInstance()
    {
        synchronized (InstrumentChooserDialogImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new InstrumentChooserDialogImpl();
                INSTANCE.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
            }
        }
        return INSTANCE;
    }

    private InstrumentChooserDialogImpl()
    {
        setModal(true);
        initComponents();
        this.setTitle("Instrument selection dialog");
        this.list_MidiSynths.setCellRenderer(new SynthCellRenderer());
        this.list_Instruments.setCellRenderer(new InstrumentCellRenderer());
        this.list_Banks.setCellRenderer(new BankCellRenderer());
        favoriteSynth = FavoriteMidiSynth.getInstance();
        // Listen to added/removed favorites
        favoriteSynth.addChangeListener(this);
    }

    @Override
    public void preset(Instrument ins, int transpose, int chan, String title, Filter<Instrument> filter)
    {
        if (filter == null)
        {
            instrumentFilter = new Filter<Instrument>()
            {
                @Override
                public boolean accept(Instrument ins)
                {
                    return true;
                }
            };
        } else
        {
            instrumentFilter = filter;
        }
        fillSynthList();
        initInstrument = ins;
        channel = chan;
        if (initInstrument != null)
        {
            list_MidiSynths.setSelectedValue(ins.getBank().getMidiSynth(), true);
            list_Banks.setSelectedValue(ins.getBank(), true);
            list_Instruments.setSelectedValue(ins, true);
        } else
        {
            list_MidiSynths.setSelectedIndex(0);
        }
        lbl_Title.setText(title);
        spn_transposition.setValue(transpose);
    }

    private void fillSynthList()
    {
        this.midiSynths.clear();
        this.midiSynths.addElement(favoriteSynth);
//        for (MidiSynth synth : MidiSynthManager.getInstance().getSynths())
//        {
//            this.midiSynths.addElement(synth);
//        }
    }

    @Override
    public int getTransposition()
    {
        return (int) spn_transposition.getValue();
    }

    @Override
    public Instrument getSelectedInstrument()
    {
        return selectedInstrument;
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

    // ----------------------------------------------------------------------------
    // ChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void stateChanged(ChangeEvent evt)
    {
        if (evt.getSource() == favoriteSynth)
        {
            LOGGER.fine("stateChanged()");

            // Favorite synth must be the first : make sure it is repaint to have the right size displayed
            list_MidiSynths.repaint(list_MidiSynths.getCellBounds(0, 0));

            // A favorite instrument was added or removed, make sure lists are refreshed.    
            if (list_MidiSynths.getSelectedValuesList().contains(favoriteSynth))
            {
                // If favorite synth is selected, need to update the list of instruments (+1 or -1)
                List<InstrumentBank<?>> lBanks = Collections.list(banks.elements());
                updateListInstruments(lBanks);
            }
            if (selectedInstrument != null)
            {
                // Restore the selection to make sure cell is repainted
                // list_Instruments.setSelectedValue(selectedInstrument, false);
                list_Instruments.setSelectedValue(selectedInstrument, true);
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
        list_MidiSynths = new JList<>(midiSynths);
        jScrollPane2 = new javax.swing.JScrollPane();
        list_Banks = new JList<>(banks);
        jScrollPane3 = new javax.swing.JScrollPane();
        list_Instruments = new JList<>(instruments);
        lbl_BankList = new javax.swing.JLabel();
        lbl_FamilyList = new javax.swing.JLabel();
        lbl_InstrumentList = new javax.swing.JLabel();
        btn_Ok = new javax.swing.JButton();
        btn_Cancel = new javax.swing.JButton();
        txt_Filter = new javax.swing.JTextField();
        btn_Filter = new javax.swing.JButton();
        btn_Clear = new javax.swing.JButton();
        btn_Hear = new javax.swing.JButton();
        lbl_Transpose = new javax.swing.JLabel();
        lbl_Title = new javax.swing.JLabel();
        spn_transposition = new org.jjazz.ui.utilities.WheelSpinner();
        jTextArea1 = new HelpTextArea();

        list_MidiSynths.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.list_MidiSynths.toolTipText")); // NOI18N
        list_MidiSynths.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_MidiSynthsValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(list_MidiSynths);

        list_Banks.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.list_Banks.toolTipText")); // NOI18N
        list_Banks.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_BanksValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(list_Banks);

        list_Instruments.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_Instruments.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                list_InstrumentsMouseClicked(evt);
            }
        });
        list_Instruments.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_InstrumentsValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(list_Instruments);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_BankList, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_BankList.text")); // NOI18N
        lbl_BankList.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_BankList.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_FamilyList, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_FamilyList.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_InstrumentList, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_InstrumentList.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_Ok, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_Ok.text")); // NOI18N
        btn_Ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OkActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        txt_Filter.setText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.txt_Filter.text")); // NOI18N
        txt_Filter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                txt_FilterActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Filter, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_Filter.text")); // NOI18N
        btn_Filter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_FilterActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Clear, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_Clear.text")); // NOI18N
        btn_Clear.setEnabled(false);
        btn_Clear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_ClearActionPerformed(evt);
            }
        });

        btn_Hear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/instrumentchooser/resources/Speaker-20x20.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(btn_Hear, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_Hear.text")); // NOI18N
        btn_Hear.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_Hear.toolTipText")); // NOI18N
        btn_Hear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_HearActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_Transpose, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_Transpose.text")); // NOI18N
        lbl_Transpose.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_Transpose.toolTipText")); // NOI18N

        lbl_Title.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Title, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_Title.text")); // NOI18N

        spn_transposition.setModel(new javax.swing.SpinnerNumberModel(0, -48, 48, 1));
        spn_transposition.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.spn_transposition.toolTipText")); // NOI18N
        spn_transposition.setColumns(2);
        spn_transposition.setLoopValues(false);

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.jTextArea1.text")); // NOI18N
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setOpaque(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_Title, javax.swing.GroupLayout.PREFERRED_SIZE, 375, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbl_FamilyList)
                            .addComponent(lbl_BankList, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_InstrumentList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txt_Filter, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(btn_Clear)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btn_Filter))
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(btn_Cancel)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btn_Ok))))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(btn_Hear, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(spn_transposition, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(lbl_Transpose, javax.swing.GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE))
                                    .addComponent(jTextArea1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                                .addContainerGap())))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_Title, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_BankList)
                    .addComponent(lbl_InstrumentList))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lbl_FamilyList)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2))
                    .addComponent(jScrollPane3)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(txt_Filter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_Filter)
                            .addComponent(btn_Clear))
                        .addGap(27, 27, 27)
                        .addComponent(btn_Hear)
                        .addGap(30, 30, 30)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spn_transposition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbl_Transpose))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                        .addComponent(jTextArea1, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_Cancel)
                            .addComponent(btn_Ok))))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_FilterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_FilterActionPerformed
    {//GEN-HEADEREND:event_btn_FilterActionPerformed
        txt_FilterActionPerformed(null);
    }//GEN-LAST:event_btn_FilterActionPerformed

    private void list_MidiSynthsValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_MidiSynthsValueChanged
    {//GEN-HEADEREND:event_list_MidiSynthsValueChanged
        LOGGER.finer("list_BanksValueChanged() evt.firstIndex=" + evt.getFirstIndex() + " evt.lastIndex=" + evt.getLastIndex());
        if (!evt.getValueIsAdjusting())
        {
            List<MidiSynth> selectedSynths = list_MidiSynths.getSelectedValuesList();

            banks.clear();
            if (!selectedSynths.isEmpty())
            {
                for (MidiSynth synth : selectedSynths)
                {
                    for (InstrumentBank<?> bank : synth.getBanks())
                    {
                        banks.addElement(bank);
                    }
                }
                // Select the first bank for this synth to limit the nb of displayed instruments (thousands on some synths !)
                list_Banks.setSelectedIndex(0);
            }
        }
    }//GEN-LAST:event_list_MidiSynthsValueChanged

    private void list_BanksValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_BanksValueChanged
    {//GEN-HEADEREND:event_list_BanksValueChanged
        LOGGER.fine("list_FamiliesValueChanged() evt=" + evt);
        if (evt == null || !evt.getValueIsAdjusting())
        {
            List<InstrumentBank<?>> selectedBanks = list_Banks.getSelectedValuesList();
            updateListInstruments(selectedBanks);
        }
    }//GEN-LAST:event_list_BanksValueChanged

    private void updateListInstruments(List<InstrumentBank<?>> selectedBanks)
    {
        instruments.clear();
        for (InstrumentBank<?> bank : selectedBanks)
        {
            for (Instrument ins : bank.getInstruments())
            {
                if (!instrumentFilter.accept(ins))
                {
                    continue;
                }
                if (instrumentSelectString == null || ins.getPatchName().toLowerCase().contains(instrumentSelectString.toLowerCase()))
                {
                    instruments.addElement(ins);
                }
            }
        }
    }

    private void list_InstrumentsValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_InstrumentsValueChanged
    {//GEN-HEADEREND:event_list_InstrumentsValueChanged
        LOGGER.finer("list_InstrumentsValueChanged()");
        if (!evt.getValueIsAdjusting())
        {
            selectedInstrument = list_Instruments.getSelectedValue();
            if (selectedInstrument != null && channel >= MidiConst.CHANNEL_MIN)
            {
                JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(selectedInstrument.getMidiMessages(channel));
                btn_Hear.setEnabled(true);
            } else
            {
                btn_Hear.setEnabled(false);
            }
        }
    }//GEN-LAST:event_list_InstrumentsValueChanged

    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
    {//GEN-HEADEREND:event_btn_CancelActionPerformed
        if (initInstrument != null && channel >= MidiConst.CHANNEL_MIN)
        {
            // Send the Midi message to restore the original instrument
            JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(initInstrument.getMidiMessages(channel));
        }
        selectedInstrument = null;
        setVisible(false);
    }//GEN-LAST:event_btn_CancelActionPerformed

    private void btn_OkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_OkActionPerformed
    {//GEN-HEADEREND:event_btn_OkActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btn_OkActionPerformed

    private void btn_HearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_HearActionPerformed
    {//GEN-HEADEREND:event_btn_HearActionPerformed

        list_MidiSynths.setEnabled(false);
        list_Banks.setEnabled(false);
        list_Instruments.setEnabled(false);
        btn_Hear.setEnabled(false);
        btn_Ok.setEnabled(false);
        btn_Cancel.setEnabled(false);

        Runnable endAction = new Runnable()
        {
            @Override
            public void run()
            {
                list_MidiSynths.setEnabled(true);
                list_Banks.setEnabled(true);
                list_Instruments.setEnabled(true);
                btn_Hear.setEnabled(true);
                btn_Ok.setEnabled(true);
                btn_Cancel.setEnabled(true);
            }
        };
        // Send MIDI messages for the selected instrument             
        MusicController mc = MusicController.getInstance();
        try
        {
            mc.playTestNotes(channel, -1, getTransposition(), endAction);
        } catch (MusicGenerationException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }

    }//GEN-LAST:event_btn_HearActionPerformed

    private void txt_FilterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_txt_FilterActionPerformed
    {//GEN-HEADEREND:event_txt_FilterActionPerformed

        LOGGER.fine("txt_SearchActionPerformed()");
        String txt = txt_Filter.getText();
        if (txt.trim().isEmpty())
        {
            return;
        }
        btn_Clear.setEnabled(true);
        txt_Filter.setEnabled(false);
        btn_Filter.setEnabled(false);
        String s = lbl_InstrumentList.getText();
        lbl_InstrumentList.setText(s + "* (FILTERED)");
        setInstrumentFilter(txt);
    }//GEN-LAST:event_txt_FilterActionPerformed

    private void btn_ClearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_ClearActionPerformed
    {//GEN-HEADEREND:event_btn_ClearActionPerformed
        btn_Clear.setEnabled(false);
        txt_Filter.setEnabled(true);
        btn_Filter.setEnabled(true);
        String s = lbl_InstrumentList.getText();
        int i = s.indexOf("*");
        lbl_InstrumentList.setText(s.substring(0, i));
        setInstrumentFilter(null);
    }//GEN-LAST:event_btn_ClearActionPerformed

    private void list_InstrumentsMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_list_InstrumentsMouseClicked
    {//GEN-HEADEREND:event_list_InstrumentsMouseClicked
        boolean ctrl = (evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        boolean shift = (evt.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
        LOGGER.fine("list_InstrumentsMouseClicked() ctrl=" + ctrl + " shift=" + shift + " evt.getClickCount()=" + evt.getClickCount());
        if (SwingUtilities.isLeftMouseButton(evt))
        {
            if (evt.getClickCount() == 1 && !ctrl && shift)
            {
                toggleFavoriteInstrument();
            } else if (evt.getClickCount() == 2 && !ctrl && !shift)
            {
                btn_OkActionPerformed(null);
            }
        }
    }//GEN-LAST:event_list_InstrumentsMouseClicked

    private void toggleFavoriteInstrument()
    {
        LOGGER.fine("toggleFavoriteInstrument() selectedInstrument=" + selectedInstrument);
        if (selectedInstrument != null)
        {
//            FavoriteInstruments fi = FavoriteInstruments.getInstance();
//            if (fi.contains(selectedInstrument))
//            {
//                fi.removeInstrument(selectedInstrument);
//            } else
//            {
//                fi.addInstrument(selectedInstrument);
//            }
        }
    }

    /**
     * Calculate the total nb of instruments, excluding the filtered instruments.
     *
     * @param synth
     * @return
     */
    private int getSynthSize(MidiSynth synth)
    {

        int size = 0;
        for (InstrumentBank<?> bank : synth.getBanks())
        {
            size += getBankSize(bank);
        }
        return size;
    }

    private int getBankSize(InstrumentBank<? extends Instrument> bank)
    {
        int size = 0;
        for (Instrument ins : bank.getInstruments())
        {
            if (instrumentFilter.accept(ins))
            {
                size++;
            }
        }
        return size;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Clear;
    private javax.swing.JButton btn_Filter;
    private javax.swing.JButton btn_Hear;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JLabel lbl_BankList;
    private javax.swing.JLabel lbl_FamilyList;
    private javax.swing.JLabel lbl_InstrumentList;
    private javax.swing.JLabel lbl_Title;
    private javax.swing.JLabel lbl_Transpose;
    private javax.swing.JList<InstrumentBank<?>> list_Banks;
    private javax.swing.JList<Instrument> list_Instruments;
    private javax.swing.JList<MidiSynth> list_MidiSynths;
    private org.jjazz.ui.utilities.WheelSpinner spn_transposition;
    private javax.swing.JTextField txt_Filter;
    // End of variables declaration//GEN-END:variables

    private Color getSynthColor(MidiSynth synth, Instrument ins)
    {
        MidiSynth ms = synth;
        Color c = mapSynthColor.get(ms);
        if (c == null)
        {
            c = COLORS[colorIndex];
            mapSynthColor.put(ms, c);
            colorIndex++;
            if (colorIndex == COLORS.length)
            {
                colorIndex = 0;
            }
        }
        return c;
    }

    private void setInstrumentFilter(String s)
    {
        instrumentSelectString = s;
        list_BanksValueChanged(null);  // Force a refresh of list_Banks and then related instruments

    }

    private class SynthCellRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            MidiSynth synth = (MidiSynth) value;
            setText(synth.getName() + " (" + getSynthSize(synth) + ")");
            String tooltip = !synth.getManufacturer().trim().isEmpty() ? synth.getManufacturer() : null;
            setToolTipText(tooltip);
            setForeground(getSynthColor(synth, null));
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
            setText(bank.getName() + " (" + getBankSize(bank) + ")");
            return c;
        }
    }

    private class InstrumentCellRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Instrument ins = (Instrument) value;
            InstrumentBank bank = ins.getBank();
//            FavoriteInstruments fi = FavoriteInstruments.getInstance();
//            if (fi.contains(ins))
//            {
//                Font f = c.getFont();
//                Font newFont = f.deriveFont(Font.BOLD);
//                setFont(newFont);
//            }
            setText(ins.getPatchName());
            setToolTipText("Synth:" + bank.getMidiSynth().getName() + ", Bank:" + bank.getName() + ", Program Change:" + ins.getMidiAddress().getProgramChange());
            setForeground(getSynthColor(ins.getBank().getMidiSynth(), ins));
            return c;
        }
    }

}
