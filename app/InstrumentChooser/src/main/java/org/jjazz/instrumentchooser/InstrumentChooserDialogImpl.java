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
package org.jjazz.instrumentchooser;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jjazz.instrumentchooser.spi.InstrumentChooserDialog;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.ui.InstrumentTable;
import org.jjazz.outputsynth.api.GMRemapTable;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.testplayerservice.spi.TestPlayer;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.openide.*;
import org.openide.windows.WindowManager;

public class InstrumentChooserDialogImpl extends InstrumentChooserDialog implements ListSelectionListener
{

    private static InstrumentChooserDialogImpl INSTANCE;
    private OutputSynth outputSynth;
    private int channel;
    private Instrument selectedInstrument;
    private Instrument preferredInstrument;
    private List<Instrument> allInstruments;
    private List<Instrument> recommendedInstruments;
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
        tbl_Instruments.setHiddenColumns(Arrays.asList(InstrumentTable.Model.COL_LSB,
                InstrumentTable.Model.COL_MSB,
                InstrumentTable.Model.COL_PC,
                InstrumentTable.Model.COL_DRUMKIT,
                InstrumentTable.Model.COL_SYNTH
        ));
        tbl_Instruments.getSelectionModel().addListSelectionListener(this);
        tbl_Instruments.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                handleTableMouseClicked(e);
            }
        });
        org.jjazz.uiutilities.api.UIUtilities.installSelectAllWhenFocused(tf_Filter);
    }

    @Override
    public void preset(OutputSynth outSynth, RhythmVoice rv, Instrument preselectedIns, int transpose, int channel)
    {
        if (outSynth == null || rv == null || !MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("outSynth=" + outSynth + " rv=" + rv + " preselectedIns=" + preselectedIns + " channel=" + channel);   
        }
        this.outputSynth = outSynth;
        this.channel = channel;
        this.preferredInstrument = rv.getPreferredInstrument();
        GM1Instrument gmSubstitute = this.preferredInstrument.getSubstitute();

        // Prepare title labels
        String rvType = "";
        if (!rv.isDrums() && !gmSubstitute.getFamily().toString().equalsIgnoreCase(rv.getName()))
        {
            rvType = " (" + ResUtil.getString(getClass(), "InstrumentChooserDialogImpl.Family") + "=" + gmSubstitute.getFamily().toString() + ")";
        }

        String myTitle = ResUtil.getString(getClass(), "InstrumentChooserDialogImpl.LabelTitle", (channel + 1), rv.getName() + rvType);
        lbl_Title.setText(myTitle);

        // Prepare recommended instrument label
        String prefInsTxt = preferredInstrument.getFullName();
        this.lbl_preferredInstrument.setText(prefInsTxt);
        DrumKit kit = rv.getDrumKit();
        String tt = null;
        if (!(preferredInstrument instanceof GM1Instrument))
        {
            tt = rv.isDrums() ? "DrumKit type=" + kit.getType().toString() + ", keymap= " + kit.getKeyMap().getName()
                    : "GM substitute: " + gmSubstitute.getSubstitute().getPatchName();
        }
        this.lbl_preferredInstrument.setToolTipText(tt);

        // OutputSynth label
        String outSynthTxt = ResUtil.getString(getClass(), "InstrumentChooserDialogImpl.OutputSynth", outputSynth.getMidiSynth().getName());
        this.lbl_outputSynthConfig.setText(outSynthTxt);

        // Reset text filter
        btn_TxtClearActionPerformed(null);

        btn_Hear.setEnabled(false);

        allInstruments = this.getAllInstruments(outputSynth, rv.isDrums());
        recommendedInstruments = this.getRecommendedInstruments(allInstruments, preferredInstrument);

        if (!recommendedInstruments.isEmpty()
                && (preselectedIns == null || preselectedIns == GMSynth.getInstance().getVoidInstrument() || recommendedInstruments.contains(preselectedIns)))
        {
            rbtn_showRecommended.setSelected(true);
            rbtn_showRecommendedActionPerformed(null);
        } else
        {
            rbtn_showAll.setSelected(true);
            rbtn_showAllActionPerformed(null);
        }
        if (preselectedIns != null)
        {
            tbl_Instruments.setSelectedInstrument(preselectedIns);
        }

        String txtType;
        if (rv.isDrums())
        {
            txtType = rv.getType().equals(RhythmVoice.Type.DRUMS) ? "Drums" : "Percussion";
        } else
        {
            txtType = gmSubstitute.getFamily().toString();
        }
        updateRbtnShowRecommendedText(txtType);
        spn_transposition.setValue(transpose);
    }

    @Override
    public Instrument getSelectedInstrument()
    {
        return selectedInstrument;
    }

    @Override
    public int getTransposition()
    {
        return (int) spn_transposition.getValue();
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

    // ===================================================================================
    // ListSelectionListener interfacce
    // ===================================================================================
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        LOGGER.log(Level.FINE, "valueChanged() e={0}", e);   
        if (e.getValueIsAdjusting())
        {
            return;
        }
        if (e.getSource() == tbl_Instruments.getSelectionModel())
        {
            selectedInstrument = tbl_Instruments.getSelectedInstrument();
            btn_Hear.setEnabled(selectedInstrument != null);
            if (selectedInstrument != null)
            {
                JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(selectedInstrument.getMidiMessages(channel));
            }
        }
    }

    // ----------------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------------
    /**
     * Get all the instruments for melodic voices or drums voices.
     *
     * @param outSynth
     * @param drumsMode If true return drums instruments first
     * @return
     */
    private List<Instrument> getAllInstruments(OutputSynth outSynth, boolean drumsMode)
    {
        return drumsMode ? outSynth.getMidiSynth().getDrumsInstruments() : outSynth.getMidiSynth().getNonDrumsInstruments();
    }

    /**
     * Get only the recommended instruments for prefIns.
     *
     * @param allInsts
     * @param prefIns  The preferredInstrument for the RhythmVoice
     * @return
     */
    private List<Instrument> getRecommendedInstruments(List<Instrument> allInsts, Instrument prefIns)
    {
        List<Instrument> res = new ArrayList<>();
        if (!prefIns.isDrumKit())
        {
            GM1Instrument gm1PrefIns = prefIns.getSubstitute();

            // First add instruments whose substitute is gm1PrefIns
            for (Instrument ins : allInsts)
            {
                if (ins.getSubstitute() == gm1PrefIns)
                {
                    res.add(ins);
                }
            }

            // Second add instruments whose substitute family matches mappedInx family
            for (Instrument ins : allInsts)
            {
                if (ins.getSubstitute() == null)
                {
                    continue;
                }
                if (ins.getSubstitute().getFamily().equals(gm1PrefIns.getFamily()))
                {
                    if (!res.contains(ins))
                    {
                        res.add(ins);
                    }
                }
            }

            // Is there a mapped instrument ? If yes add it first
            Instrument mappedIns = outputSynth.getUserSettings().getGMRemapTable().getInstrument(gm1PrefIns);
            if (mappedIns == null)
            {
                mappedIns = outputSynth.getUserSettings().getGMRemapTable().getInstrument(gm1PrefIns.getFamily());
            }
            if (mappedIns != null)
            {
                res.remove(mappedIns);
                res.add(0, mappedIns);
            }

        } else
        {
            // Drums
            List<Instrument> second = new ArrayList<>();
            List<Instrument> third = new ArrayList<>();
            DrumKit kit = prefIns.getDrumKit();
            for (Instrument ins : allInsts)
            {
                DrumKit insKit = ins.getDrumKit();
                if (insKit == null)
                {
                    continue;
                }
                if (insKit.getType().equals(kit.getType()) && kit.getKeyMap().isContaining(insKit.getKeyMap()))
                {
                    // First : full match
                    res.add(ins);
                } else if (kit.getKeyMap().isContaining(insKit.getKeyMap()))
                {
                    // Second : keymap match
                    second.add(ins);
                } else
                {
                    // Third other drums instruments
                    third.add(ins);
                }
            }
            res.addAll(second);
            res.addAll(third);

            // If mapped drums/perc instruments are defined, put them first
            Instrument mappedDrumsIns = outputSynth.getUserSettings().getGMRemapTable().getInstrument(GMRemapTable.DRUMS_INSTRUMENT);
            Instrument mappedPercIns = outputSynth.getUserSettings().getGMRemapTable().getInstrument(GMRemapTable.PERCUSSION_INSTRUMENT);
            if (mappedPercIns != null)
            {
                res.remove(mappedPercIns);
                res.add(0, mappedPercIns);
            }
            if (mappedDrumsIns != null)
            {
                res.remove(mappedDrumsIns);
                res.add(0, mappedDrumsIns);
            }

        }
        return res;
    }

    private void handleTableMouseClicked(MouseEvent evt)
    {
        boolean ctrl = (evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        boolean shift = (evt.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
        if (evt.getSource() == tbl_Instruments)
        {
            if (SwingUtilities.isLeftMouseButton(evt))
            {
                if (evt.getClickCount() == 1 && shift)
                {
                    btn_HearActionPerformed(null);
                } else if (evt.getClickCount() == 2 && !shift)
                {
                    btn_OkActionPerformed(null);
                }
            }
        }
    }

    private void updateRbtnShowRecommendedText(String txtType)
    {
        String text = ResUtil.getString(getClass(), "InstrumentChooserDialogImpl.ShowOnlyRecommendedInstruments", txtType);
        this.rbtn_showRecommended.setText(text);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        btn_showInstruments = new javax.swing.ButtonGroup();
        btn_Ok = new javax.swing.JButton();
        btn_Cancel = new javax.swing.JButton();
        tf_Filter = new javax.swing.JTextField();
        btn_TxtFilter = new javax.swing.JButton();
        btn_TxtClear = new javax.swing.JButton();
        btn_Hear = new javax.swing.JButton();
        lbl_Title = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_Instruments = new org.jjazz.midi.api.ui.InstrumentTable();
        rbtn_showRecommended = new javax.swing.JRadioButton();
        rbtn_showAll = new javax.swing.JRadioButton();
        lbl_Filtered = new javax.swing.JLabel();
        lbl_recIns = new javax.swing.JLabel();
        spn_transposition = new org.jjazz.flatcomponents.api.WheelSpinner();
        lbl_transpose = new javax.swing.JLabel();
        lbl_preferredInstrument = new javax.swing.JLabel();
        lbl_outputSynthConfig = new javax.swing.JLabel();

        setTitle(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.title")); // NOI18N

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

        tf_Filter.setText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.tf_Filter.text")); // NOI18N
        tf_Filter.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.tf_Filter.toolTipText")); // NOI18N
        tf_Filter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_FilterActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_TxtFilter, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_TxtFilter.text")); // NOI18N
        btn_TxtFilter.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_TxtFilter.toolTipText")); // NOI18N
        btn_TxtFilter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_TxtFilterActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_TxtClear, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_TxtClear.text")); // NOI18N
        btn_TxtClear.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_TxtClear.toolTipText")); // NOI18N
        btn_TxtClear.setEnabled(false);
        btn_TxtClear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_TxtClearActionPerformed(evt);
            }
        });

        btn_Hear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/instrumentchooser/resources/SpeakerRed-20x20.png"))); // NOI18N
        btn_Hear.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.btn_Hear.toolTipText")); // NOI18N
        btn_Hear.setDisabledIcon(GeneralUISettings.getInstance().getIcon("speaker.icon.disabled"));
        btn_Hear.setMargin(new java.awt.Insets(2, 4, 2, 4));
        btn_Hear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_HearActionPerformed(evt);
            }
        });

        lbl_Title.setFont(lbl_Title.getFont().deriveFont(lbl_Title.getFont().getStyle() | java.awt.Font.BOLD));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Title, "Select an instrument for channel 9 - CHORD1 - Strings"); // NOI18N

        jScrollPane1.setViewportView(tbl_Instruments);

        btn_showInstruments.add(rbtn_showRecommended);
        rbtn_showRecommended.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_showRecommended, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.rbtn_showRecommended.text")); // NOI18N
        rbtn_showRecommended.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.rbtn_showRecommended.toolTipText")); // NOI18N
        rbtn_showRecommended.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_showRecommendedActionPerformed(evt);
            }
        });

        btn_showInstruments.add(rbtn_showAll);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_showAll, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.rbtn_showAll.text")); // NOI18N
        rbtn_showAll.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.rbtn_showAll.toolTipText")); // NOI18N
        rbtn_showAll.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_showAllActionPerformed(evt);
            }
        });

        lbl_Filtered.setForeground(new java.awt.Color(251, 102, 122));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Filtered, "(FILTERED)"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_recIns, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_recIns.text")); // NOI18N
        lbl_recIns.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_recIns.toolTipText")); // NOI18N

        spn_transposition.setModel(new javax.swing.SpinnerNumberModel(0, -48, 48, 1));
        spn_transposition.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.spn_transposition.toolTipText")); // NOI18N
        spn_transposition.setColumns(2);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_transpose, org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_transpose.text")); // NOI18N
        lbl_transpose.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_transpose.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_preferredInstrument, "jLabel3"); // NOI18N
        lbl_preferredInstrument.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_preferredInstrument.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_outputSynthConfig, "Output synth: GM Synth"); // NOI18N
        lbl_outputSynthConfig.setToolTipText(org.openide.util.NbBundle.getMessage(InstrumentChooserDialogImpl.class, "InstrumentChooserDialogImpl.lbl_outputSynthConfig.toolTipText")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_Title, javax.swing.GroupLayout.DEFAULT_SIZE, 637, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(lbl_Filtered))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(spn_transposition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_transpose, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(btn_Hear)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(rbtn_showRecommended)
                                    .addComponent(rbtn_showAll))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(btn_TxtClear)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_TxtFilter))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(tf_Filter, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_recIns)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_preferredInstrument)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(lbl_outputSynthConfig)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_Ok)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_Cancel, btn_Ok});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_Title, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_recIns, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_preferredInstrument))
                .addGap(18, 18, 18)
                .addComponent(rbtn_showAll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbtn_showRecommended)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbl_Filtered)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_Ok)
                            .addComponent(btn_Cancel)
                            .addComponent(lbl_outputSynthConfig)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tf_Filter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_TxtFilter)
                            .addComponent(btn_TxtClear))
                        .addGap(50, 50, 50)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spn_transposition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbl_transpose))
                        .addGap(28, 28, 28)
                        .addComponent(btn_Hear)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_TxtFilterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_TxtFilterActionPerformed
    {//GEN-HEADEREND:event_btn_TxtFilterActionPerformed
        tf_FilterActionPerformed(null);
    }//GEN-LAST:event_btn_TxtFilterActionPerformed


    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
    {//GEN-HEADEREND:event_btn_CancelActionPerformed
        selectedInstrument = null;
        setVisible(false);
    }//GEN-LAST:event_btn_CancelActionPerformed

    private void btn_OkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_OkActionPerformed
    {//GEN-HEADEREND:event_btn_OkActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btn_OkActionPerformed

    private void btn_HearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_HearActionPerformed
    {//GEN-HEADEREND:event_btn_HearActionPerformed
        Instrument ins = tbl_Instruments.getSelectedInstrument();
        if (ins == null || !ins.getMidiAddress().isFullyDefined())
        {
            LOGGER.log(Level.FINE, "btn_HearActionPerformed() called but invalid ins={0} ins.getMidiAddress()={1}", new Object[]{ins,
                ins.getMidiAddress()});   
            return;
        }

        tbl_Instruments.setEnabled(false);
        btn_Hear.setEnabled(false);
        btn_Ok.setEnabled(false);
        btn_Cancel.setEnabled(false);

        Runnable endAction = new Runnable()
        {
            @Override
            public void run()
            {
                tbl_Instruments.setEnabled(true);
                btn_Hear.setEnabled(true);
                btn_Ok.setEnabled(true);
                btn_Cancel.setEnabled(true);
            }
        };
        // Send MIDI messages for the selected instrument             
        TestPlayer tp = TestPlayer.getDefault();
        try
        {
            final int TRANSPOSE = ins.isDrumKit() ? -24 : 0;
            JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(channel));
            tp.playTestNotes(channel, -1, TRANSPOSE + getTransposition(), endAction);
        } catch (MusicGenerationException ex)
        {
            endAction.run();
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }

    }//GEN-LAST:event_btn_HearActionPerformed

    private void tf_FilterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_FilterActionPerformed
    {//GEN-HEADEREND:event_tf_FilterActionPerformed
        LOGGER.fine("tf_FilterActionPerformed()");   
        String s = tf_Filter.getText().trim();
        if (s.isEmpty())
        {
            return;
        }
        RowFilter<TableModel, Object> rf = null;
        try
        {
            rf = RowFilter.regexFilter("(?i)" + s);
        } catch (java.util.regex.PatternSyntaxException e)
        {
            LOGGER.log(Level.WARNING, "tf_FilterActionPerformed() invalid filter regex string e={0}", e.getMessage());   
            return;
        }
        TableRowSorter<? extends TableModel> sorter = (TableRowSorter<? extends TableModel>) tbl_Instruments.getRowSorter();
        sorter.setRowFilter(rf);
        btn_TxtFilter.setEnabled(false);
        btn_TxtClear.setEnabled(true);
        tf_Filter.setEnabled(false);
        String msg = ResUtil.getString(getClass(), "InstrumentChooserDialogImpl.Filtered", Utilities.truncateWithDots(s, 10));
        lbl_Filtered.setText(msg);
    }//GEN-LAST:event_tf_FilterActionPerformed

    private void btn_TxtClearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_TxtClearActionPerformed
    {//GEN-HEADEREND:event_btn_TxtClearActionPerformed
        TableRowSorter<? extends TableModel> sorter = (TableRowSorter<? extends TableModel>) tbl_Instruments.getRowSorter();
        sorter.setRowFilter(null);
        btn_TxtFilter.setEnabled(true);
        btn_TxtClear.setEnabled(false);
        tf_Filter.setEnabled(true);
        lbl_Filtered.setText(" ");   // Not ""
    }//GEN-LAST:event_btn_TxtClearActionPerformed

    private void rbtn_showAllActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_showAllActionPerformed
    {//GEN-HEADEREND:event_rbtn_showAllActionPerformed
        Instrument sel = tbl_Instruments.getSelectedInstrument();
        tbl_Instruments.getModel().setInstruments(this.allInstruments);
        if (sel != null && allInstruments.contains(sel))
        {
            tbl_Instruments.setSelectedInstrument(sel);
        }
    }//GEN-LAST:event_rbtn_showAllActionPerformed

    private void rbtn_showRecommendedActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_showRecommendedActionPerformed
    {//GEN-HEADEREND:event_rbtn_showRecommendedActionPerformed
        Instrument sel = tbl_Instruments.getSelectedInstrument();
        tbl_Instruments.getModel().setInstruments(this.recommendedInstruments);
        if (sel != null && recommendedInstruments.contains(sel))
        {
            tbl_Instruments.setSelectedInstrument(sel);
        }
	}//GEN-LAST:event_rbtn_showRecommendedActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Hear;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JButton btn_TxtClear;
    private javax.swing.JButton btn_TxtFilter;
    private javax.swing.ButtonGroup btn_showInstruments;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_Filtered;
    private javax.swing.JLabel lbl_Title;
    private javax.swing.JLabel lbl_outputSynthConfig;
    private javax.swing.JLabel lbl_preferredInstrument;
    private javax.swing.JLabel lbl_recIns;
    private javax.swing.JLabel lbl_transpose;
    private javax.swing.JRadioButton rbtn_showAll;
    private javax.swing.JRadioButton rbtn_showRecommended;
    private org.jjazz.flatcomponents.api.WheelSpinner spn_transposition;
    private org.jjazz.midi.api.ui.InstrumentTable tbl_Instruments;
    private javax.swing.JTextField tf_Filter;
    // End of variables declaration//GEN-END:variables

}
