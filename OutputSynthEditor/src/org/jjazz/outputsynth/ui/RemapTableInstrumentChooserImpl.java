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
package org.jjazz.outputsynth.ui;

import org.jjazz.midi.synths.FavoriteMidiSynth;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jjazz.midi.DrumKit;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.keymap.KeyMapGM;
import org.jjazz.midi.keymap.KeyMapGSGM2;
import org.jjazz.midi.keymap.KeyMapXG;
import org.jjazz.midi.synths.GM1Instrument;
import org.jjazz.midi.ui.InstrumentTable;
import org.jjazz.musiccontrol.TestPlayer;
import org.jjazz.outputsynth.GMRemapTable;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.ui.spi.RemapTableInstrumentChooser;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.uisettings.GeneralUISettings;
import org.jjazz.util.ResUtil;
import org.jjazz.util.Utilities;
import org.openide.*;
import org.openide.windows.WindowManager;

public class RemapTableInstrumentChooserImpl extends RemapTableInstrumentChooser implements ChangeListener, ListSelectionListener
{

    private static RemapTableInstrumentChooserImpl INSTANCE;
    private OutputSynth outputSynth;
    private Instrument selectedInstrument;
    private FavoriteMidiSynth favoriteSynth;
    private Instrument remappedInstrument;
    private GM1Instrument remappedInstrumentAsGM1;
    private List<Instrument> allInstruments;
    private List<Instrument> recommendedInstruments;
    private static final Logger LOGGER = Logger.getLogger(RemapTableInstrumentChooserImpl.class.getSimpleName());

    static public RemapTableInstrumentChooserImpl getInstance()
    {
        synchronized (RemapTableInstrumentChooserImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RemapTableInstrumentChooserImpl();
                INSTANCE.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
            }
        }
        return INSTANCE;
    }

    private RemapTableInstrumentChooserImpl()
    {
        setModal(true);
        initComponents();
        setTitle(ResUtil.getString(getClass(), "RemapTableInstrumentChooserImpl.CTL_RemapInstSelectDialogTitle"));
        favoriteSynth = FavoriteMidiSynth.getInstance();
        favoriteSynth.addChangeListener(this);      // Listen to added/removed favorites

        tbl_Instruments.setHiddenColumns(Arrays.asList(InstrumentTable.Model.COL_LSB,
                InstrumentTable.Model.COL_MSB,
                InstrumentTable.Model.COL_PC,
                InstrumentTable.Model.COL_DRUMKIT
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
        org.jjazz.ui.utilities.Utilities.installSelectAllWhenFocused(tf_Filter);
    }

    @Override
    public void preset(OutputSynth outSynth, Instrument remappedIns)
    {
        GMRemapTable.checkRemappedInstrument(remappedIns);
        if (outSynth == null)
        {
            throw new IllegalArgumentException("outSynth=" + outSynth + " remappedIns=" + remappedIns);   //NOI18N
        }

        remappedInstrument = remappedIns;
        outputSynth = outSynth;
        GMRemapTable rTable = outputSynth.getGMRemapTable();

        String myTitle = ResUtil.getString(getClass(), "RemapTableInstrumentChooserImpl.CTL_MappedInstrument", remappedInstrument.getPatchName());
        if (remappedInstrument instanceof GM1Instrument)
        {
            // Melodic
            remappedInstrumentAsGM1 = (GM1Instrument) remappedInstrument;
            myTitle += " (" + ResUtil.getString(getClass(), "RemapTableInstrumentChooserImpl.family") + "=" + remappedInstrumentAsGM1.getFamily().toString() + ")";
        } else
        {
            // Drums
            remappedInstrumentAsGM1 = null;
        }
        lbl_Title.setText(myTitle);

        // Reset text filter
        btn_TxtClearActionPerformed(null);

        cb_UseAsFamilyDefault.setSelected(false);
        btn_Hear.setEnabled(false);

        Instrument targetIns = outputSynth.getGMRemapTable().getInstrument(remappedInstrument);

        allInstruments = this.getAllInstruments(outputSynth, remappedInstrumentAsGM1 == null);
        recommendedInstruments = this.getRecommendedInstruments(allInstruments, remappedInstrument);
        if (!recommendedInstruments.isEmpty() && (targetIns == null || recommendedInstruments.contains(targetIns)))
        {
            rbtn_showRecommended.setSelected(true);
            rbtn_showRecommendedActionPerformed(null);
        } else
        {
            rbtn_showAll.setSelected(true);
            rbtn_showAllActionPerformed(null);
        }
        updateRbtnShowRecommendedText(remappedInstrument);

        if (targetIns != null)
        {
            tbl_Instruments.setSelectedInstrument(targetIns);
            if (remappedInstrumentAsGM1 != null && rTable.getInstrument(remappedInstrumentAsGM1.getFamily()) == targetIns)
            {
                cb_UseAsFamilyDefault.setSelected(true);
            }
        }

    }

    @Override
    public Instrument getSelectedInstrument()
    {
        return selectedInstrument;
    }

    @Override
    public boolean useAsFamilyDefault()
    {
        return cb_UseAsFamilyDefault.isSelected();
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
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "actionOk");   //NOI18N
        contentPane.getActionMap().put("actionOk", new AbstractAction("OK")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                btn_OkActionPerformed(null);
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "actionCancel");   //NOI18N
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
            LOGGER.fine("stateChanged()");   //NOI18N

//            // Favorite synth must be the first : make sure it is repaint to have the right size displayed
//            list_MidiSynths.repaint(list_MidiSynths.getCellBounds(0, 0));
//
//            // A favorite instrument was added or removed, make sure lists are refreshed.    
//            if (list_MidiSynths.getSelectedValuesList().contains(favoriteSynth))
//            {
//                // If favorite synth is selected, need to update the list of instruments (+1 or -1)
//                List<InstrumentBank<?>> lBanks = Collections.list(banks.elements());
//                updateListInstruments(lBanks);
//            }
//            if (selectedInstrument != null)
//            {
//                // Restore the selection to make sure cell is repainted
//                // list_Instruments.setSelectedValue(selectedInstrument, false);
//                list_Instruments.setSelectedValue(selectedInstrument, true);
//            }
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
            selectedInstrument = tbl_Instruments.getSelectedInstrument();
            btn_Hear.setEnabled(selectedInstrument != null);
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
        ArrayList<Instrument> res = new ArrayList<>();
        if (drumsMode)
        {
            for (InstrumentBank<?> bank : outSynth.getCompatibleStdBanks())
            {
                res.addAll(bank.getDrumsInstruments());
            }
            for (MidiSynth synth : outSynth.getCustomSynths())
            {
                res.addAll(synth.getDrumsInstruments());
            }
            for (InstrumentBank<?> bank : outSynth.getCompatibleStdBanks())
            {
                res.addAll(bank.getNonDrumsInstruments());
            }
            for (MidiSynth synth : outSynth.getCustomSynths())
            {
                res.addAll(synth.getNonDrumsInstruments());
            }
        } else
        {
            for (InstrumentBank<?> bank : outSynth.getCompatibleStdBanks())
            {
                res.addAll(bank.getInstruments());
            }
            for (MidiSynth synth : outSynth.getCustomSynths())
            {
                res.addAll(synth.getInstruments());
            }
        }

        return res;
    }

    /**
     * Get only the recommended instruments for mappedIns.
     *
     * @param allInsts
     * @param mappedIns
     * @return
     */
    private List<Instrument> getRecommendedInstruments(List<Instrument> allInsts, Instrument mappedIns)
    {
        List<Instrument> res = new ArrayList<>();
        if (mappedIns instanceof GM1Instrument)
        {
            GM1Instrument gm1MappedIns = (GM1Instrument) mappedIns;

            // First add instruments whose substitute is mappedIns
            for (Instrument ins : allInsts)
            {
                if (ins.getMidiAddress().equals(gm1MappedIns.getMidiAddress()))
                {
                    continue;
                }
                if (ins.getSubstitute() == gm1MappedIns)
                {
                    res.add(ins);
                }
            }

            // Second add instruments whose substitute family matches mappedInx family
            for (Instrument ins : allInsts)
            {
                if (ins.getMidiAddress().equals(gm1MappedIns.getMidiAddress()) || ins.getSubstitute() == null)
                {
                    continue;
                }
                if (ins.getSubstitute().getFamily().equals(gm1MappedIns.getFamily()))
                {
                    if (!res.contains(ins))
                    {
                        res.add(ins);
                    }
                }
            }
        } else
        {
            // Drums : keep only kits with a GM compatible KeyMap
            for (Instrument ins : allInsts)
            {
                if (!ins.isDrumKit())
                {
                    continue;
                }
                DrumKit.KeyMap keyMap = ins.getDrumKit().getKeyMap();
                if (!keyMap.isContaining(KeyMapGM.getInstance())
                        && !keyMap.isContaining(KeyMapGSGM2.getInstance())
                        && !keyMap.isContaining(KeyMapXG.getInstance()))
                {
                    continue;
                }
                res.add(ins);
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

    private void updateRbtnShowRecommendedText(Instrument remappedInstrument)
    {
        String text = this.rbtn_showRecommended.getText();
        int index = text.indexOf("(");
        if (index != -1)
        {
            text = text.substring(0, index).trim();
        }
        if (remappedInstrument instanceof GM1Instrument)
        {
            text += " (" + ((GM1Instrument) remappedInstrument).getFamily().toString().toUpperCase() + ")";
        } else if (remappedInstrument == GMRemapTable.DRUMS_INSTRUMENT)
        {
            text += " (DRUMS)";
        } else
        {
            text += " (PERCUSSION)";
        }
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
        tbl_Instruments = new org.jjazz.midi.ui.InstrumentTable();
        rbtn_showRecommended = new javax.swing.JRadioButton();
        rbtn_showAll = new javax.swing.JRadioButton();
        lbl_Filtered = new javax.swing.JLabel();
        cb_UseAsFamilyDefault = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(btn_Ok, org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.btn_Ok.text")); // NOI18N
        btn_Ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OkActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        tf_Filter.setText(org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.tf_Filter.text")); // NOI18N
        tf_Filter.setToolTipText(org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.tf_Filter.toolTipText")); // NOI18N
        tf_Filter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_FilterActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_TxtFilter, org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.btn_TxtFilter.text")); // NOI18N
        btn_TxtFilter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_TxtFilterActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_TxtClear, org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.btn_TxtClear.text")); // NOI18N
        btn_TxtClear.setEnabled(false);
        btn_TxtClear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_TxtClearActionPerformed(evt);
            }
        });

        btn_Hear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/outputsynth/ui/resources/SpeakerRed-20x20.png"))); // NOI18N
        btn_Hear.setToolTipText(org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.btn_Hear.toolTipText")); // NOI18N
        btn_Hear.setDisabledIcon(GeneralUISettings.getInstance().getIcon("speaker.icon.disabled"));
        btn_Hear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_HearActionPerformed(evt);
            }
        });

        lbl_Title.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Title, org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.lbl_Title.text")); // NOI18N

        jScrollPane1.setViewportView(tbl_Instruments);

        btn_showInstruments.add(rbtn_showRecommended);
        rbtn_showRecommended.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_showRecommended, org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.rbtn_showRecommended.text")); // NOI18N
        rbtn_showRecommended.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_showRecommendedActionPerformed(evt);
            }
        });

        btn_showInstruments.add(rbtn_showAll);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_showAll, org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.rbtn_showAll.text")); // NOI18N
        rbtn_showAll.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_showAllActionPerformed(evt);
            }
        });

        lbl_Filtered.setForeground(new java.awt.Color(153, 0, 0));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Filtered, "(FILTERED)"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_UseAsFamilyDefault, org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.cb_UseAsFamilyDefault.text")); // NOI18N
        cb_UseAsFamilyDefault.setToolTipText(org.openide.util.NbBundle.getMessage(RemapTableInstrumentChooserImpl.class, "RemapTableInstrumentChooserImpl.cb_UseAsFamilyDefault.toolTipText")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_Ok)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_Title, javax.swing.GroupLayout.PREFERRED_SIZE, 375, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(rbtn_showRecommended)
                                    .addComponent(rbtn_showAll))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(lbl_Filtered))
                                    .addComponent(jScrollPane1))
                                .addGap(13, 13, 13)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(btn_Hear, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(55, 55, 55)
                                .addComponent(btn_TxtClear)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_TxtFilter))
                            .addComponent(tf_Filter)
                            .addComponent(cb_UseAsFamilyDefault, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(6, 6, 6))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_Cancel, btn_Ok});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_Title, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbtn_showAll)
                    .addComponent(cb_UseAsFamilyDefault))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbtn_showRecommended)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_Filtered)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_Ok)
                            .addComponent(btn_Cancel)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tf_Filter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_TxtFilter)
                            .addComponent(btn_TxtClear))
                        .addGap(29, 29, 29)
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
            LOGGER.fine("btn_HearActionPerformed() called but invalid ins=" + ins + " ins.getMidiAddress()=" + ins.getMidiAddress());   //NOI18N
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
        TestPlayer tp = TestPlayer.getInstance();
        try
        {
            final int CHANNEL = ins.isDrumKit() ? MidiConst.CHANNEL_DRUMS : 0;
            JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(CHANNEL));
            tp.playTestNotes(CHANNEL, -1, 0, endAction);
        } catch (MusicGenerationException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }

    }//GEN-LAST:event_btn_HearActionPerformed

    private void tf_FilterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_FilterActionPerformed
    {//GEN-HEADEREND:event_tf_FilterActionPerformed
        LOGGER.fine("tf_FilterActionPerformed()");   //NOI18N
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
            LOGGER.warning("tf_FilterActionPerformed() invalid filter regex string e=" + e.getLocalizedMessage());   //NOI18N
            return;
        }
        TableRowSorter<? extends TableModel> sorter = (TableRowSorter<? extends TableModel>) tbl_Instruments.getRowSorter();
        sorter.setRowFilter(rf);
        btn_TxtFilter.setEnabled(false);
        btn_TxtClear.setEnabled(true);
        tf_Filter.setEnabled(false);
        lbl_Filtered.setText(ResUtil.getString(getClass(), "RemapTableInstrumentChooserImpl.Filtered", Utilities.truncateWithDots(s, 20)));
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
        tbl_Instruments.getModel().setInstruments(this.allInstruments);
    }//GEN-LAST:event_rbtn_showAllActionPerformed

    private void rbtn_showRecommendedActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_showRecommendedActionPerformed
    {//GEN-HEADEREND:event_rbtn_showRecommendedActionPerformed
        tbl_Instruments.getModel().setInstruments(this.recommendedInstruments);    }//GEN-LAST:event_rbtn_showRecommendedActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Hear;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JButton btn_TxtClear;
    private javax.swing.JButton btn_TxtFilter;
    private javax.swing.ButtonGroup btn_showInstruments;
    private javax.swing.JCheckBox cb_UseAsFamilyDefault;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_Filtered;
    private javax.swing.JLabel lbl_Title;
    private javax.swing.JRadioButton rbtn_showAll;
    private javax.swing.JRadioButton rbtn_showRecommended;
    private org.jjazz.midi.ui.InstrumentTable tbl_Instruments;
    private javax.swing.JTextField tf_Filter;
    // End of variables declaration//GEN-END:variables

}
