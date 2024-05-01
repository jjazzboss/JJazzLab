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
package org.jjazz.fluidsynthembeddedsynth.test;

import com.google.common.collect.Streams;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import org.jjazz.flatcomponents.api.FlatIntegerKnob;
import org.jjazz.fluidsynthjava.api.Chorus;
import org.jjazz.fluidsynthjava.api.FluidSynthJava;
import org.jjazz.fluidsynthjava.api.Reverb;
import org.jjazz.uiutilities.api.UIUtilities;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 * A Dialog to adjust FluidSynth effects.
 * <p>
 * For test purposes.
 */
public class FluidEffectsDialog extends javax.swing.JDialog implements PropertyChangeListener
{

    private final FluidSynthJava fluidSynthJava;
    private static final String PREF_REVERB_PRESETS = "PrefReverbPresets";
    private static final String PREF_CHORUS_PRESETS = "PrefChorusPresets";
    private static final Preferences prefs = NbPreferences.forModule(FluidEffectsDialog.class);
    private static final Logger LOGGER = Logger.getLogger(FluidEffectsDialog.class.getSimpleName());

    private DefaultListModel<Reverb> revPresetListModel;
    private DefaultListModel<Chorus> choPresetListModel;

    /**
     * Creates new form FluidEffectDialog
     *
     * @param modal
     * @param fluidSynth
     */
    public FluidEffectsDialog(boolean modal, FluidSynthJava fluidSynth)
    {
        super(WindowManager.getDefault().getMainWindow(), modal);
        this.fluidSynthJava = fluidSynth;
        this.fluidSynthJava.addPropertyChangeListener(this);

        initComponents();


        // Presets
        list_cho_presets.setCellRenderer(new EffectRenderer());
        choPresetListModel = new DefaultListModel<>();
        loadChorusPresets().forEach(cho -> choPresetListModel.addElement(cho));
        list_cho_presets.setModel(choPresetListModel);


        list_rev_presets.setCellRenderer(new EffectRenderer());
        revPresetListModel = new DefaultListModel<>();
        loadReverbPresets().forEach(rev -> revPresetListModel.addElement(rev));
        list_rev_presets.setModel(revPresetListModel);


        UIUtilities.installEnterKeyAction(this, () -> btn_OKActionPerformed(null));
        UIUtilities.installEscapeKeyAction(this, () -> btn_CancelActionPerformed(null));


        updateUIComponents();


        // Double-clicks on JLists
        list_rev_presets.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
                if (evt.getClickCount() == 2)
                {

                    int index = list_rev_presets.locationToIndex(evt.getPoint());
                    fluidSynthJava.setReverb(revPresetListModel.getElementAt(index));
                }
            }
        });

        // Double-clicks on JLists
        list_cho_presets.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
                if (evt.getClickCount() == 2)
                {

                    int index = list_cho_presets.locationToIndex(evt.getPoint());
                    fluidSynthJava.setChorus(choPresetListModel.getElementAt(index));
                }
            }
        });

        knob_room.addPropertyChangeListener(this);
        knob_damp.addPropertyChangeListener(this);
        knob_width.addPropertyChangeListener(this);
        knob_level_rev.addPropertyChangeListener(this);

        knob_nr.addPropertyChangeListener(this);
        knob_speed.addPropertyChangeListener(this);
        knob_depth.addPropertyChangeListener(this);
        cb_triangle.addChangeListener(e -> propertyChange(null));
        knob_level_cho.addPropertyChangeListener(this);


        // Handle window closing 
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
                fluidSynthJava.removePropertyChangeListener(FluidEffectsDialog.this);
                dispose();
            }
        });

        pack();

        setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt == null // cb_triangle
                || ((evt.getSource() instanceof FlatIntegerKnob) && evt.getPropertyName().equals(FlatIntegerKnob.PROP_VALUE)))
        {
            // Change come from the UI
            var reverb = getReverbFromUI("yy");
            fluidSynthJava.setReverb(reverb);
            var chorus = getChorusFromUI("zz");
            fluidSynthJava.setChorus(chorus);
        } else if (evt.getSource() == fluidSynthJava)
        {
            // Change come from the model
            updateUIComponents();
        }

    }


    private Chorus getChorusFromUI(String name)
    {
        int nr = knob_nr.getValue();
        float speed = convertToFluid(knob_speed.getValue());
        float depth = knob_depth.getValue();
        int type = cb_triangle.isSelected() ? 1 : 0;
        float level_chorus = convertToFluid(knob_level_cho.getValue());
        Chorus res = new Chorus(name, nr, speed, depth, type, level_chorus);
        return res;
    }

    private Reverb getReverbFromUI(String name)
    {
        float room = convertToFluid(knob_room.getValue());
        float damp = convertToFluid(knob_damp.getValue());
        float width = convertToFluid(knob_width.getValue());
        float level = convertToFluid(knob_level_rev.getValue());
        Reverb res = new Reverb(name, room, damp, width, level);
        return res;
    }


    private void updateUIComponents()
    {
        Reverb reverb = fluidSynthJava.getReverb();
        Chorus chorus = fluidSynthJava.getChorus();

        knob_room.setValue(convertFromFluid(reverb.room()));
        updateValueLabel(lbl_room, String.format("%.2f", reverb.room()));

        knob_damp.setValue(convertFromFluid(reverb.damp()));
        updateValueLabel(lbl_damp, String.format("%.2f", reverb.damp()));

        knob_width.setValue(convertFromFluid(reverb.width()));
        updateValueLabel(lbl_width, String.format("%.2f", reverb.width()));

        knob_level_rev.setValue(convertFromFluid(reverb.level()));
        updateValueLabel(lbl_rev_level, String.format("%.2f", reverb.level()));


        knob_nr.setValue(chorus.nr());
        updateValueLabel(lbl_nr, String.valueOf(chorus.nr()));

        knob_speed.setValue(convertFromFluid(chorus.speed()));
        updateValueLabel(lbl_speed, String.format("%.2f", chorus.speed()));

        knob_depth.setValue((int) Math.round(chorus.depth()));
        updateValueLabel(lbl_depth, String.format("%.2f", chorus.depth()));

        knob_level_cho.setValue(convertFromFluid(chorus.level()));
        updateValueLabel(lbl_cho_level, String.format("%.2f", chorus.level()));

        cb_triangle.setSelected(chorus.type() == 1);


    }

    private int convertFromFluid(float value)
    {
        return (int) Math.round(value * 100);
    }

    private float convertToFluid(int value)
    {
        return value / 100f;
    }

    private void updateValueLabel(JLabel label, String value)
    {
        String s = label.getText();
        int i = s.indexOf("=");
        if (i < 0)
        {
            return;
        }
        s = s.substring(0, i) + "=" + value;
        label.setText(s);
    }

    private void savePresets()
    {
        final var revJoiner = new StringJoiner("##");
        revPresetListModel.elements().asIterator()
                .forEachRemaining(rev -> revJoiner.add(rev.saveAsString()));
        prefs.put(PREF_REVERB_PRESETS, revJoiner.toString());


        final var choJoiner = new StringJoiner("##");
        choPresetListModel.elements().asIterator()
                .forEachRemaining(cho -> choJoiner.add(cho.saveAsString()));
        prefs.put(PREF_CHORUS_PRESETS, choJoiner.toString());
    }

    private List<Reverb> loadReverbPresets()
    {
        var res = new ArrayList<Reverb>();

        var s = prefs.get(PREF_REVERB_PRESETS, null);
        if (s != null)
        {
            String strs[] = s.split("\\s*##\\s*");
            for (var str : strs)
            {
                try
                {
                    res.add(Reverb.loadFromString(str));
                } catch (NumberFormatException ex)
                {
                    LOGGER.log(Level.WARNING, "loadRevPresets() Invalid Reverb string, ignoring str={0}. ex={1}", new Object[]
                    {
                        str,
                        ex.getMessage()
                    });
                }
            }
        }
        return res;
    }

    private List<Chorus> loadChorusPresets()
    {
        var res = new ArrayList<Chorus>();

        var s = prefs.get(PREF_CHORUS_PRESETS, null);
        if (s != null)
        {
            String strs[] = s.split("\\s*##\\s*");
            for (var str : strs)
            {
                try
                {
                    res.add(Chorus.loadFromString(str));
                } catch (NumberFormatException ex)
                {
                    LOGGER.log(Level.WARNING, "loadChoPresets() Invalid Chorus string, ignoring str={0}. ex={1}", new Object[]
                    {
                        str,
                        ex.getMessage()
                    });
                }
            }
        }
        return res;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jLabel5 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        cb_triangle = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        list_cho_presets = new javax.swing.JList<>();
        btn_OK = new javax.swing.JButton();
        btn_rev_store = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_rev_presets = new javax.swing.JList<>();
        btn_cho_store = new javax.swing.JButton();
        pnl_reverb = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        knob_room = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_room = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        knob_damp = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_damp = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        knob_width = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_width = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        knob_level_rev = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_rev_level = new javax.swing.JLabel();
        pnl_chorus = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        knob_nr = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_nr = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        knob_speed = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_speed = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        knob_depth = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_depth = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        knob_level_cho = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_cho_level = new javax.swing.JLabel();
        btn_rev_remove = new javax.swing.JButton();
        btn_cho_remove = new javax.swing.JButton();
        btn_Cancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(FluidEffectsDialog.class, "FluidEffectsDialog.title")); // NOI18N
        setAlwaysOnTop(true);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, "REVERB"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel10, "CHORUS"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_triangle, "triangle"); // NOI18N
        cb_triangle.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_triangleActionPerformed(evt);
            }
        });

        list_cho_presets.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(list_cho_presets);

        org.openide.awt.Mnemonics.setLocalizedText(btn_OK, "OK"); // NOI18N
        btn_OK.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OKActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_rev_store, "Store"); // NOI18N
        btn_rev_store.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_rev_storeActionPerformed(evt);
            }
        });

        list_rev_presets.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(list_rev_presets);

        org.openide.awt.Mnemonics.setLocalizedText(btn_cho_store, btn_rev_store.getText());
        btn_cho_store.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cho_storeActionPerformed(evt);
            }
        });

        pnl_reverb.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 15, 5));

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        knob_room.setMaxValue(100);
        knob_room.setValue(50);

        javax.swing.GroupLayout knob_roomLayout = new javax.swing.GroupLayout(knob_room);
        knob_room.setLayout(knob_roomLayout);
        knob_roomLayout.setHorizontalGroup(
            knob_roomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 38, Short.MAX_VALUE)
        );
        knob_roomLayout.setVerticalGroup(
            knob_roomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 32, Short.MAX_VALUE)
        );

        jPanel1.add(knob_room);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_room, "room="); // NOI18N
        lbl_room.setAlignmentX(0.5F);
        jPanel1.add(lbl_room);

        pnl_reverb.add(jPanel1);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        knob_damp.setMaxValue(100);
        knob_damp.setValue(50);

        javax.swing.GroupLayout knob_dampLayout = new javax.swing.GroupLayout(knob_damp);
        knob_damp.setLayout(knob_dampLayout);
        knob_dampLayout.setHorizontalGroup(
            knob_dampLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 38, Short.MAX_VALUE)
        );
        knob_dampLayout.setVerticalGroup(
            knob_dampLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 32, Short.MAX_VALUE)
        );

        jPanel2.add(knob_damp);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_damp, "damp="); // NOI18N
        lbl_damp.setAlignmentX(0.5F);
        jPanel2.add(lbl_damp);

        pnl_reverb.add(jPanel2);

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));

        knob_width.setMaxValue(10000);
        knob_width.setValue(50);

        javax.swing.GroupLayout knob_widthLayout = new javax.swing.GroupLayout(knob_width);
        knob_width.setLayout(knob_widthLayout);
        knob_widthLayout.setHorizontalGroup(
            knob_widthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 38, Short.MAX_VALUE)
        );
        knob_widthLayout.setVerticalGroup(
            knob_widthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 32, Short.MAX_VALUE)
        );

        jPanel3.add(knob_width);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_width, "width="); // NOI18N
        lbl_width.setAlignmentX(0.5F);
        jPanel3.add(lbl_width);

        pnl_reverb.add(jPanel3);

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

        knob_level_rev.setMaxValue(100);
        knob_level_rev.setValue(50);

        javax.swing.GroupLayout knob_level_revLayout = new javax.swing.GroupLayout(knob_level_rev);
        knob_level_rev.setLayout(knob_level_revLayout);
        knob_level_revLayout.setHorizontalGroup(
            knob_level_revLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 38, Short.MAX_VALUE)
        );
        knob_level_revLayout.setVerticalGroup(
            knob_level_revLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 32, Short.MAX_VALUE)
        );

        jPanel4.add(knob_level_rev);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_rev_level, "level="); // NOI18N
        lbl_rev_level.setAlignmentX(0.5F);
        jPanel4.add(lbl_rev_level);

        pnl_reverb.add(jPanel4);

        pnl_chorus.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 15, 5));

        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.Y_AXIS));

        knob_nr.setMaxValue(99);
        knob_nr.setValue(50);

        javax.swing.GroupLayout knob_nrLayout = new javax.swing.GroupLayout(knob_nr);
        knob_nr.setLayout(knob_nrLayout);
        knob_nrLayout.setHorizontalGroup(
            knob_nrLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 38, Short.MAX_VALUE)
        );
        knob_nrLayout.setVerticalGroup(
            knob_nrLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 32, Short.MAX_VALUE)
        );

        jPanel5.add(knob_nr);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_nr, "nr="); // NOI18N
        lbl_nr.setAlignmentX(0.5F);
        jPanel5.add(lbl_nr);

        pnl_chorus.add(jPanel5);

        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.Y_AXIS));

        knob_speed.setMaxValue(500);
        knob_speed.setMinValue(10);
        knob_speed.setValue(50);

        javax.swing.GroupLayout knob_speedLayout = new javax.swing.GroupLayout(knob_speed);
        knob_speed.setLayout(knob_speedLayout);
        knob_speedLayout.setHorizontalGroup(
            knob_speedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 38, Short.MAX_VALUE)
        );
        knob_speedLayout.setVerticalGroup(
            knob_speedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 32, Short.MAX_VALUE)
        );

        jPanel6.add(knob_speed);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_speed, "speed="); // NOI18N
        lbl_speed.setAlignmentX(0.5F);
        jPanel6.add(lbl_speed);

        pnl_chorus.add(jPanel6);

        jPanel7.setLayout(new javax.swing.BoxLayout(jPanel7, javax.swing.BoxLayout.Y_AXIS));

        knob_depth.setMaxValue(256);
        knob_depth.setValue(50);

        javax.swing.GroupLayout knob_depthLayout = new javax.swing.GroupLayout(knob_depth);
        knob_depth.setLayout(knob_depthLayout);
        knob_depthLayout.setHorizontalGroup(
            knob_depthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 38, Short.MAX_VALUE)
        );
        knob_depthLayout.setVerticalGroup(
            knob_depthLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 32, Short.MAX_VALUE)
        );

        jPanel7.add(knob_depth);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_depth, "depth="); // NOI18N
        lbl_depth.setAlignmentX(0.5F);
        jPanel7.add(lbl_depth);

        pnl_chorus.add(jPanel7);

        jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8, javax.swing.BoxLayout.Y_AXIS));

        knob_level_cho.setMaxValue(1000);
        knob_level_cho.setValue(50);

        javax.swing.GroupLayout knob_level_choLayout = new javax.swing.GroupLayout(knob_level_cho);
        knob_level_cho.setLayout(knob_level_choLayout);
        knob_level_choLayout.setHorizontalGroup(
            knob_level_choLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 38, Short.MAX_VALUE)
        );
        knob_level_choLayout.setVerticalGroup(
            knob_level_choLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 32, Short.MAX_VALUE)
        );

        jPanel8.add(knob_level_cho);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_cho_level, "level="); // NOI18N
        lbl_cho_level.setAlignmentX(0.5F);
        jPanel8.add(lbl_cho_level);

        pnl_chorus.add(jPanel8);

        org.openide.awt.Mnemonics.setLocalizedText(btn_rev_remove, "Remove"); // NOI18N
        btn_rev_remove.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_rev_removeActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_cho_remove, btn_rev_remove.getText());
        btn_cho_remove.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cho_removeActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, "Cancel"); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_reverb, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
                    .addComponent(pnl_chorus, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10)
                            .addComponent(jLabel5)
                            .addComponent(cb_triangle))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btn_cho_store, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_rev_store, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_cho_remove)
                            .addComponent(btn_rev_remove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_OK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel)
                        .addContainerGap())))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_rev_remove, btn_rev_store});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_cho_remove, btn_cho_store});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_Cancel, btn_OK});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnl_reverb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24)
                        .addComponent(jLabel10))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btn_rev_store)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_rev_remove))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnl_chorus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btn_cho_store)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_cho_remove)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(btn_OK)
                                    .addComponent(btn_Cancel)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(cb_triangle)
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_OKActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_OKActionPerformed
    {//GEN-HEADEREND:event_btn_OKActionPerformed

        savePresets();
        setVisible(false);
    }//GEN-LAST:event_btn_OKActionPerformed

    private void btn_rev_storeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_rev_storeActionPerformed
    {//GEN-HEADEREND:event_btn_rev_storeActionPerformed
        String name = JOptionPane.showInputDialog(this, "Enter Reverb preset name");
        if (name != null && !name.isBlank())
        {
            // If same name exists replace it
            var sameName = Streams.stream(revPresetListModel.elements().asIterator())
                    .filter(r -> r.name().equals(name))
                    .findAny()
                    .orElse(null);
            if (sameName != null)
            {
                int index = revPresetListModel.indexOf(sameName);
                revPresetListModel.setElementAt(getReverbFromUI(name), index);
            } else
            {
                revPresetListModel.addElement(getReverbFromUI(name));
            }
        }
    }//GEN-LAST:event_btn_rev_storeActionPerformed

    private void btn_cho_storeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cho_storeActionPerformed
    {//GEN-HEADEREND:event_btn_cho_storeActionPerformed
        String name = JOptionPane.showInputDialog(this, "Enter Chorus preset name");
        if (name != null && !name.isBlank())
        {// If same name exists replace it
            var sameName = Streams.stream(choPresetListModel.elements().asIterator())
                    .filter(r -> r.name().equals(name))
                    .findAny()
                    .orElse(null);
            if (sameName != null)
            {
                int index = choPresetListModel.indexOf(sameName);
                choPresetListModel.setElementAt(getChorusFromUI(name), index);
            } else
            {
                choPresetListModel.addElement(getChorusFromUI(name));
            }
        }

    }//GEN-LAST:event_btn_cho_storeActionPerformed

    private void cb_triangleActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_triangleActionPerformed
    {//GEN-HEADEREND:event_cb_triangleActionPerformed
        Chorus cho = getChorusFromUI("xx");
        fluidSynthJava.setChorus(cho);
    }//GEN-LAST:event_cb_triangleActionPerformed

    private void btn_rev_removeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_rev_removeActionPerformed
    {//GEN-HEADEREND:event_btn_rev_removeActionPerformed
        var rev = list_rev_presets.getSelectedValue();
        if (rev == null)
        {
            return;
        }
        revPresetListModel.removeElement(rev);
    }//GEN-LAST:event_btn_rev_removeActionPerformed

    private void btn_cho_removeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cho_removeActionPerformed
    {//GEN-HEADEREND:event_btn_cho_removeActionPerformed
        var cho = list_cho_presets.getSelectedValue();
        if (cho == null)
        {
            return;
        }
        choPresetListModel.removeElement(cho);
    }//GEN-LAST:event_btn_cho_removeActionPerformed

    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
    {//GEN-HEADEREND:event_btn_CancelActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btn_CancelActionPerformed



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_OK;
    private javax.swing.JButton btn_cho_remove;
    private javax.swing.JButton btn_cho_store;
    private javax.swing.JButton btn_rev_remove;
    private javax.swing.JButton btn_rev_store;
    private javax.swing.JCheckBox cb_triangle;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_damp;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_depth;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_level_cho;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_level_rev;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_nr;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_room;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_speed;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_width;
    private javax.swing.JLabel lbl_cho_level;
    private javax.swing.JLabel lbl_damp;
    private javax.swing.JLabel lbl_depth;
    private javax.swing.JLabel lbl_nr;
    private javax.swing.JLabel lbl_rev_level;
    private javax.swing.JLabel lbl_room;
    private javax.swing.JLabel lbl_speed;
    private javax.swing.JLabel lbl_width;
    private javax.swing.JList<Chorus> list_cho_presets;
    private javax.swing.JList<Reverb> list_rev_presets;
    private javax.swing.JPanel pnl_chorus;
    private javax.swing.JPanel pnl_reverb;
    // End of variables declaration//GEN-END:variables


    // =============================================================================
    // Inner classes
    // =============================================================================
    public class EffectRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null)
            {
                return label;
            }

            String txt, tt;
            if (value instanceof Reverb rev)
            {
                txt = rev.name();
                tt = rev.saveAsString();
            } else
            {
                Chorus cho = (Chorus) value;
                txt = cho.name();
                tt = cho.saveAsString();
            }
            label.setText(txt);
            label.setToolTipText(tt);

            return label;
        }
    }


}
