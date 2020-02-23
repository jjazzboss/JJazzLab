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
package org.jjazz.ui.cl_editor.editors;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.harmony.Note;
import org.jjazz.harmony.ScaleManager;
import org.jjazz.harmony.StandardScaleInstance;
import org.jjazz.leadsheet.chordleadsheet.api.item.AltDataFilter;
import org.jjazz.leadsheet.chordleadsheet.api.item.AltExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo.PlayStyle;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.leadsheet.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.rhythm.parameters.RP_SYS_Marker;
import org.jjazz.ui.cl_editor.spi.ChordSymbolEditorDialog;
import org.jjazz.ui.utilities.NoSelectOnFocusGainedJTF;
import org.jjazz.ui.utilities.Utilities;

/**
 * Default chord symbol edit dialog.
 */
public class ChordSymbolEditorDialogImpl extends ChordSymbolEditorDialog implements ListSelectionListener
{

    private static ChordSymbolEditorDialogImpl INSTANCE;
    private static ChordSymbolEditorDialogImpl ALT_INSTANCE;
    private CLI_ChordSymbol model;
    private boolean exitOk;
    private AltExtChordSymbol altChordSymbol;
    private final DefaultListModel<StandardScaleInstance> stdScales = new DefaultListModel<>();
    private final DefaultListModel<String> markers = new DefaultListModel<>();
    private static final Logger LOGGER = Logger.getLogger(ChordSymbolEditorDialogImpl.class.getSimpleName());

    public static ChordSymbolEditorDialogImpl getInstance()
    {
        synchronized (ChordSymbolEditorDialogImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ChordSymbolEditorDialogImpl();
            }
        }
        return INSTANCE;
    }

    /**
     * Creates new form EditChordOptionsDialog
     */
    private ChordSymbolEditorDialogImpl()
    {
        super();

        for (String marker : RP_SYS_Marker.getInstance().getPossibleValues())
        {
            markers.addElement(marker);
        }

        initComponents();

        updateScaleInfo(null);

        // Listen to changes to update the rest of UI in real time
        tf_ChordSymbolName.getDocument().addDocumentListener(new ChordSymbolDocumentListener());
    }

    @Override
    public void preset(String title, CLI_ChordSymbol cliCs, char key, boolean enableAlternate)
    {
        if (cliCs == null)
        {
            throw new NullPointerException("cliCs");
        }
        exitOk = false;
        model = cliCs;
        ExtChordSymbol ecs = model.getData();
        ChordRenderingInfo cri = ecs.getRenderingInfo();
        setTitle(title);
        tabbedPane.setSelectedIndex(0);

        // Update PlayStyle UI
        if (cri.getPlayStyle().equals(PlayStyle.NORMAL))
        {
            this.rbtn_normal.setSelected(true);
        } else if (cri.getPlayStyle().equals(PlayStyle.HOLD))
        {
            this.rbtn_hold.setSelected(true);
        } else if (cri.getPlayStyle().equals(PlayStyle.SHOT))
        {
            this.rbtn_shot.setSelected(true);
        } else if (cri.getPlayStyle().equals(PlayStyle.ACCENT))
        {
            this.rbtn_accent.setSelected(true);
        }

        cb_anticipateAllowed.setSelected(cri.isAnticipateAllowed());

        // Update Scales UI      
        updateScales(ecs);
        list_scales.clearSelection();
        StandardScaleInstance ssi = cri.getScaleInstance();
        if (ssi != null)
        {
            list_scales.setSelectedValue(ssi, true);
        }

        updateChordSymbolInfo(ecs);
        lbl_optionalText.setText(getOptionalText(getPlayStyle(), getScaleInstance()));

        // Update chord UI
        tf_ChordSymbolName.requestFocus();
        if (key == 0)
        {
            tf_ChordSymbolName.setText(ecs.getOriginalName());
            tf_ChordSymbolName.selectAll();
        } else
        {
            // Input the key
            tf_ChordSymbolName.setText(String.valueOf(key).toUpperCase());
        }

        // Update alternate UI
        tabbedPane.setEnabledAt(2, enableAlternate);
        if (enableAlternate)
        {
            AltDataFilter altFilter;
            if (ecs.getAlternateChordSymbol() != null)
            {
                // There is an alternate chord symbol defined
                cb_enableAlternate.setSelected(true);
                if (ecs.getAlternateChordSymbol() == VoidAltExtChordSymbol.getInstance())
                {
                    // It's a Void alternate chord symbol
                    cb_useVoidAlt.setSelected(true);
                    // If user unselects the Void value, propose a value consistent with the current chord symbol
                    altChordSymbol = new AltExtChordSymbol(ecs.getRootNote(), ecs.getBassNote(), ecs.getChordType(), ecs.getRenderingInfo());
                } else
                {
                    cb_useVoidAlt.setSelected(false);
                    altChordSymbol = ecs.getAlternateChordSymbol();
                }
                altFilter = ecs.getAlternateFilter();
            } else
            {
                // No alternate define, just set consistent value if user enables the alternate chord symbol
                cb_enableAlternate.setSelected(false);
                altChordSymbol = new AltExtChordSymbol(ecs.getRootNote(), ecs.getBassNote(), ecs.getChordType(), ecs.getRenderingInfo());
                altFilter = new AltDataFilter(AltDataFilter.Random.RANDOM);
                cb_useVoidAlt.setSelected(false);
            }

            lbl_altChordSymbol.setText(altChordSymbol.getName());

            // Condition
            rbtn_random.setSelected(altFilter.isRandom());
            rbtn_marker.setSelected(!altFilter.isRandom());
            list_markerValues.removeListSelectionListener(this);        // To avoid the automatic reselection of item 0 by our listener
            list_markerValues.clearSelection();
            list_markerValues.addListSelectionListener(this);
            if (!altFilter.isRandom())
            {
                for (String marker : altFilter.getValues())
                {
                    int index = markers.indexOf(marker);
                    assert index != -1 : "marker=" + marker + " markers=" + markers;
                    list_markerValues.addSelectionInterval(index, index);
                }
            } else
            {
                list_markerValues.setSelectedIndex(0);
            }

            // This will enable/disable components as required
            cb_enableAlternateActionPerformed(null);

            lbl_optionalAltText.setText(getAltOptionalText(getAltChordSymbol(), getAltFilter()));
        } else
        {
            // The only UI component depending on the Alt ChordSymbol UI but not in the Alt ChordSymbol's JTabbedPane.
            lbl_optionalAltText.setText("");
        }
    }

    @Override
    public void cleanup()
    {
        // Nothing
    }

    @Override
    public boolean exitedOk()
    {
        return exitOk;
    }

    @Override
    public ExtChordSymbol getData()
    {
        if (!exitOk)
        {
            return null;
        }
        ChordRenderingInfo cri = new ChordRenderingInfo(getPlayStyle(), cb_anticipateAllowed.isSelected(), getScaleInstance());
        String text = tf_ChordSymbolName.getText();
        ExtChordSymbol ecs = null;
        try
        {
            ecs = new ExtChordSymbol(text, cri, getAltChordSymbol(), getAltFilter());
        } catch (ParseException ex)
        {
            throw new IllegalStateException("text=" + text + " :" + ex.getLocalizedMessage());
        }
        return ecs;
    }

    // =======================================================================================
    // ListSelectionListener implementation
    // =======================================================================================        
    @Override
    public void valueChanged(ListSelectionEvent event)
    {
        if (event.getValueIsAdjusting())
        {
            return;
        }
        if (list_markerValues.getSelectedIndex() == -1)
        {
            list_markerValues.setSelectedIndex(0);
        }
        lbl_optionalAltText.setText(getAltOptionalText(getAltChordSymbol(), getAltFilter()));
    }

    // =======================================================================================
    // Private methods
    // =======================================================================================    
    private ChordRenderingInfo.PlayStyle getPlayStyle()
    {
        PlayStyle ps = PlayStyle.NORMAL;
        if (this.rbtn_hold.isSelected())
        {
            ps = PlayStyle.HOLD;
        } else if (this.rbtn_shot.isSelected())
        {
            ps = PlayStyle.SHOT;
        } else if (this.rbtn_accent.isSelected())
        {
            ps = PlayStyle.ACCENT;
        }
        return ps;
    }

    private StandardScaleInstance getScaleInstance()
    {
        return list_scales.getSelectedValue();
    }

    private AltDataFilter getAltFilter()
    {
        AltDataFilter altFi = null;
        if (cb_enableAlternate.isSelected())
        {
            altFi = rbtn_random.isSelected() ? new AltDataFilter(AltDataFilter.Random.RANDOM) : new AltDataFilter(list_markerValues.getSelectedValuesList());
        }
        return altFi;
    }

    private AltExtChordSymbol getAltChordSymbol()
    {
        AltExtChordSymbol altCs = null;
        if (cb_enableAlternate.isSelected())
        {
            altCs = cb_useVoidAlt.isSelected() ? VoidAltExtChordSymbol.getInstance() : altChordSymbol;
        }
        return altCs;
    }

    private void updateScaleInfo(StandardScaleInstance ssi)
    {
        String scaleNotes = "";
        String scaleTip = "";
        if (ssi != null)
        {
            scaleNotes = ssi.toNoteString();
            int index = ScaleManager.getInstance().getStandardScales().indexOf(ssi.getScale());
            if (index > 0 && index < 7)
            {
                int pitch = 12 + ssi.getStartNote().getRelativePitch() - ScaleManager.MAJOR.getNotes().get(index).getRelativePitch();
                Note n = new Note(pitch);
                scaleTip = "Same as " + n.toRelativeNoteString() + " major scale starting on " + ssi.getStartNote().toRelativeNoteString();
            }
        }
        this.lbl_scaleNotes.setText(scaleNotes);
        this.lbl_scaleTip.setText(scaleTip);
    }

    /**
     * Update UI depending on ChordSymbol text validity.
     */
    private boolean checkChordSymbol()
    {
        String text = this.tf_ChordSymbolName.getText().trim();
        ExtChordSymbol ecs = null;
        boolean checkOk = false;
        try
        {
            if (!text.isEmpty())
            {
                ecs = new ExtChordSymbol(text);
                checkOk = true;
            }
        } catch (ParseException ex)
        {
            // Nothing
        }
        if (checkOk)
        {
            // Valid chord symbol
            updateChordSymbolInfo(ecs);
            updateScales(ecs);
            lbl_optionalText.setText(getOptionalText(getPlayStyle(), getScaleInstance()));
            btn_Ok.setEnabled(true);
            this.tf_ChordSymbolName.setForeground(Color.BLACK);
        } else
        {
            updateChordSymbolInfo(null);
            btn_Ok.setEnabled(false);
            this.tf_ChordSymbolName.setForeground(Color.GRAY);
        }
        return checkOk;
    }

    /**
     * Update the info associated to the ChordSymbol.
     *
     * @param ecs Can be null.
     */
    private void updateChordSymbolInfo(ExtChordSymbol ecs)
    {
        String text = "---";
        if (ecs != null)
        {
            text = ecs.toNoteString();
        }
        lbl_chordNotes.setText(text);
    }

    private String getAltOptionalText(AltExtChordSymbol altSymbol, AltDataFilter altFilter)
    {
        StringBuilder sb = new StringBuilder();
        if (altSymbol != null)
        {
            if (altSymbol == VoidAltExtChordSymbol.getInstance())
            {
                sb.append("Void");
            } else
            {
                ChordRenderingInfo cri = altSymbol.getRenderingInfo();
                sb.append(altSymbol);
                sb.append(" - ");
                sb.append(getOptionalText(cri.getPlayStyle(), cri.getScaleInstance()));
            }
            sb.append(" - Condition=");
            sb.append(altFilter.isRandom() ? "Random" : altFilter.getValues());
        } else
        {
            sb.append("-"); // Avoid empty string to avoid UI vertical relayout 
        }
        return sb.toString();
    }

    private String getOptionalText(PlayStyle ps, StandardScaleInstance ssi)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(ps.toString());
        if (ssi != null)
        {
            sb.append(" - ").append(ssi);
        }
        return sb.toString();
    }

    private void updateScales(ExtChordSymbol ecs)
    {
        List<StandardScaleInstance> saveSelectedValues = list_scales.getSelectedValuesList();
        stdScales.clear();
        List<Integer> indices = new ArrayList<>();
        int index = 0;
        for (StandardScaleInstance ssi : ScaleManager.getInstance().getMatchingScales(ecs))
        {
            stdScales.addElement(ssi);
            if (saveSelectedValues.contains(ssi))
            {
                indices.add(index);
            }
            index++;
        }
        for (int indice : indices)
        {
            list_scales.getSelectionModel().addSelectionInterval(indice, indice);
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

        btnGroup_chordType = new javax.swing.ButtonGroup();
        btnGroup_offBeatStyle = new javax.swing.ButtonGroup();
        buttonGroup1 = new javax.swing.ButtonGroup();
        pnl_ChordDescription = new javax.swing.JPanel();
        tf_ChordSymbolName = new NoSelectOnFocusGainedJTF();
        lbl_chordNotes = new javax.swing.JLabel();
        lbl_optionalText = new javax.swing.JLabel();
        lbl_optionalAltText = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        tabbedPane = new javax.swing.JTabbedPane();
        pnl_Interpretation = new javax.swing.JPanel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 10), new java.awt.Dimension(32767, 10));
        pnl_playStyleButtons = new javax.swing.JPanel();
        rbtn_normal = new javax.swing.JRadioButton();
        rbtn_accent = new javax.swing.JRadioButton();
        rbtn_hold = new javax.swing.JRadioButton();
        rbtn_shot = new javax.swing.JRadioButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        hlptxt_playStyle = new org.jjazz.ui.utilities.HelpTextArea();
        cb_anticipateAllowed = new javax.swing.JCheckBox();
        pnl_Harmony = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_scales = new JList<>(stdScales);
        jScrollPane3 = new javax.swing.JScrollPane();
        helpTextArea2 = new org.jjazz.ui.utilities.HelpTextArea();
        lbl_scaleNotes = new javax.swing.JLabel();
        lbl_scaleTip = new javax.swing.JLabel();
        pnl_Alternate = new javax.swing.JPanel();
        cb_enableAlternate = new javax.swing.JCheckBox();
        pnl_altCondition = new javax.swing.JPanel();
        rbtn_random = new javax.swing.JRadioButton();
        rbtn_marker = new javax.swing.JRadioButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        list_markerValues = new JList<>(markers);
        pnl_altChordSymbol = new javax.swing.JPanel();
        lbl_altChordSymbol = new javax.swing.JLabel();
        btn_setAltChordSymbol = new javax.swing.JButton();
        cb_useVoidAlt = new javax.swing.JCheckBox();
        pnl_OkButtons = new javax.swing.JPanel();
        lbl_Note = new javax.swing.JLabel();
        btn_Cancel = new javax.swing.JButton();
        btn_Ok = new javax.swing.JButton();

        setTitle(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.title")); // NOI18N
        setModal(true);

        tf_ChordSymbolName.setColumns(7);
        tf_ChordSymbolName.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        tf_ChordSymbolName.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tf_ChordSymbolName.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_ChordSymbolNameActionPerformed(evt);
            }
        });

        lbl_chordNotes.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_chordNotes, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_chordNotes.text")); // NOI18N
        lbl_chordNotes.setAlignmentX(0.5F);
        lbl_chordNotes.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_optionalText, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_optionalText.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_optionalAltText, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_optionalAltText.text")); // NOI18N

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout pnl_ChordDescriptionLayout = new javax.swing.GroupLayout(pnl_ChordDescription);
        pnl_ChordDescription.setLayout(pnl_ChordDescriptionLayout);
        pnl_ChordDescriptionLayout.setHorizontalGroup(
            pnl_ChordDescriptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_ChordDescriptionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_ChordDescriptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(tf_ChordSymbolName)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_ChordDescriptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_optionalAltText)
                    .addComponent(lbl_optionalText)
                    .addComponent(lbl_chordNotes))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnl_ChordDescriptionLayout.setVerticalGroup(
            pnl_ChordDescriptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_ChordDescriptionLayout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(pnl_ChordDescriptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tf_ChordSymbolName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_chordNotes))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_optionalText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnl_ChordDescriptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_optionalAltText)
                    .addComponent(jLabel1))
                .addContainerGap())
        );

        tabbedPane.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.tabbedPane.toolTipText")); // NOI18N

        pnl_playStyleButtons.setLayout(new javax.swing.BoxLayout(pnl_playStyleButtons, javax.swing.BoxLayout.Y_AXIS));

        btnGroup_chordType.add(rbtn_normal);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_normal, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_normal.text")); // NOI18N
        rbtn_normal.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_normalActionPerformed(evt);
            }
        });
        pnl_playStyleButtons.add(rbtn_normal);

        btnGroup_chordType.add(rbtn_accent);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_accent, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_accent.text")); // NOI18N
        rbtn_accent.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_accentActionPerformed(evt);
            }
        });
        pnl_playStyleButtons.add(rbtn_accent);

        btnGroup_chordType.add(rbtn_hold);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_hold, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_hold.text")); // NOI18N
        rbtn_hold.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_holdActionPerformed(evt);
            }
        });
        pnl_playStyleButtons.add(rbtn_hold);

        btnGroup_chordType.add(rbtn_shot);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_shot, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_shot.text")); // NOI18N
        rbtn_shot.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_shotActionPerformed(evt);
            }
        });
        pnl_playStyleButtons.add(rbtn_shot);

        jScrollPane1.setBorder(null);

        hlptxt_playStyle.setColumns(20);
        hlptxt_playStyle.setRows(5);
        hlptxt_playStyle.setText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.hlptxt_playStyle.text")); // NOI18N
        jScrollPane1.setViewportView(hlptxt_playStyle);

        org.openide.awt.Mnemonics.setLocalizedText(cb_anticipateAllowed, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_anticipateAllowed.text")); // NOI18N

        javax.swing.GroupLayout pnl_InterpretationLayout = new javax.swing.GroupLayout(pnl_Interpretation);
        pnl_Interpretation.setLayout(pnl_InterpretationLayout);
        pnl_InterpretationLayout.setHorizontalGroup(
            pnl_InterpretationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_InterpretationLayout.createSequentialGroup()
                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 79, Short.MAX_VALUE))
            .addGroup(pnl_InterpretationLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_InterpretationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_InterpretationLayout.createSequentialGroup()
                        .addComponent(pnl_playStyleButtons, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(cb_anticipateAllowed))
                .addContainerGap(85, Short.MAX_VALUE))
        );
        pnl_InterpretationLayout.setVerticalGroup(
            pnl_InterpretationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_InterpretationLayout.createSequentialGroup()
                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_InterpretationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnl_playStyleButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addGap(18, 18, 18)
                .addComponent(cb_anticipateAllowed)
                .addContainerGap(35, Short.MAX_VALUE))
        );

        tabbedPane.addTab(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.pnl_Interpretation.TabConstraints.tabTitle"), null, pnl_Interpretation, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.pnl_Interpretation.TabConstraints.tabToolTip")); // NOI18N

        list_scales.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_scales.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_scalesValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(list_scales);

        jScrollPane3.setBorder(null);

        helpTextArea2.setColumns(20);
        helpTextArea2.setRows(5);
        helpTextArea2.setText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.helpTextArea2.text")); // NOI18N
        jScrollPane3.setViewportView(helpTextArea2);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_scaleNotes, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_scaleNotes.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_scaleTip, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_scaleTip.text")); // NOI18N

        javax.swing.GroupLayout pnl_HarmonyLayout = new javax.swing.GroupLayout(pnl_Harmony);
        pnl_Harmony.setLayout(pnl_HarmonyLayout);
        pnl_HarmonyLayout.setHorizontalGroup(
            pnl_HarmonyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_HarmonyLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_HarmonyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                    .addComponent(lbl_scaleTip, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lbl_scaleNotes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnl_HarmonyLayout.setVerticalGroup(
            pnl_HarmonyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_HarmonyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_HarmonyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
                    .addGroup(pnl_HarmonyLayout.createSequentialGroup()
                        .addComponent(lbl_scaleNotes)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_scaleTip)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane3)))
                .addContainerGap())
        );

        tabbedPane.addTab(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.pnl_Harmony.TabConstraints.tabTitle"), null, pnl_Harmony, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.pnl_Harmony.TabConstraints.tabToolTip")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_enableAlternate, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_enableAlternate.text")); // NOI18N
        cb_enableAlternate.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_enableAlternateActionPerformed(evt);
            }
        });

        pnl_altCondition.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.pnl_altCondition.border.title"))); // NOI18N

        buttonGroup1.add(rbtn_random);
        rbtn_random.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_random, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_random.text")); // NOI18N
        rbtn_random.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_random.toolTipText")); // NOI18N
        rbtn_random.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_randomActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbtn_marker);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_marker, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_marker.text")); // NOI18N
        rbtn_marker.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_marker.toolTipText")); // NOI18N
        rbtn_marker.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_markerActionPerformed(evt);
            }
        });

        jScrollPane4.setViewportView(list_markerValues);

        javax.swing.GroupLayout pnl_altConditionLayout = new javax.swing.GroupLayout(pnl_altCondition);
        pnl_altCondition.setLayout(pnl_altConditionLayout);
        pnl_altConditionLayout.setHorizontalGroup(
            pnl_altConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_altConditionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_altConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_altConditionLayout.createSequentialGroup()
                        .addComponent(rbtn_random)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(pnl_altConditionLayout.createSequentialGroup()
                        .addComponent(rbtn_marker)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane4))))
        );
        pnl_altConditionLayout.setVerticalGroup(
            pnl_altConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_altConditionLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rbtn_random)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_altConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_altConditionLayout.createSequentialGroup()
                        .addComponent(rbtn_marker)
                        .addContainerGap(47, Short.MAX_VALUE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );

        pnl_altChordSymbol.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.pnl_altChordSymbol.border.title"))); // NOI18N
        pnl_altChordSymbol.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.pnl_altChordSymbol.toolTipText")); // NOI18N

        lbl_altChordSymbol.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_altChordSymbol, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_altChordSymbol.text")); // NOI18N
        lbl_altChordSymbol.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_altChordSymbol.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_setAltChordSymbol, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.btn_setAltChordSymbol.text")); // NOI18N
        btn_setAltChordSymbol.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.btn_setAltChordSymbol.toolTipText")); // NOI18N
        btn_setAltChordSymbol.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_setAltChordSymbolActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_useVoidAlt, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_useVoidAlt.text")); // NOI18N
        cb_useVoidAlt.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_useVoidAlt.toolTipText")); // NOI18N
        cb_useVoidAlt.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_useVoidAltActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_altChordSymbolLayout = new javax.swing.GroupLayout(pnl_altChordSymbol);
        pnl_altChordSymbol.setLayout(pnl_altChordSymbolLayout);
        pnl_altChordSymbolLayout.setHorizontalGroup(
            pnl_altChordSymbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_altChordSymbolLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_altChordSymbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_altChordSymbolLayout.createSequentialGroup()
                        .addComponent(lbl_altChordSymbol, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_setAltChordSymbol))
                    .addGroup(pnl_altChordSymbolLayout.createSequentialGroup()
                        .addComponent(cb_useVoidAlt)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnl_altChordSymbolLayout.setVerticalGroup(
            pnl_altChordSymbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_altChordSymbolLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_altChordSymbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_altChordSymbol)
                    .addComponent(btn_setAltChordSymbol))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cb_useVoidAlt)
                .addContainerGap())
        );

        javax.swing.GroupLayout pnl_AlternateLayout = new javax.swing.GroupLayout(pnl_Alternate);
        pnl_Alternate.setLayout(pnl_AlternateLayout);
        pnl_AlternateLayout.setHorizontalGroup(
            pnl_AlternateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_AlternateLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_AlternateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_AlternateLayout.createSequentialGroup()
                        .addComponent(pnl_altChordSymbol, javax.swing.GroupLayout.PREFERRED_SIZE, 177, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnl_altCondition, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(pnl_AlternateLayout.createSequentialGroup()
                        .addComponent(cb_enableAlternate)
                        .addGap(0, 127, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnl_AlternateLayout.setVerticalGroup(
            pnl_AlternateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_AlternateLayout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addComponent(cb_enableAlternate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_AlternateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_altCondition, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnl_altChordSymbol, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        tabbedPane.addTab(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.pnl_Alternate.TabConstraints.tabTitle"), null, pnl_Alternate, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.pnl_Alternate.TabConstraints.tabToolTip")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_Note, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_Note.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Ok, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.btn_Ok.text")); // NOI18N
        btn_Ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OkActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_OkButtonsLayout = new javax.swing.GroupLayout(pnl_OkButtons);
        pnl_OkButtons.setLayout(pnl_OkButtonsLayout);
        pnl_OkButtonsLayout.setHorizontalGroup(
            pnl_OkButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_OkButtonsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_Note)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_Ok, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btn_Cancel)
                .addContainerGap())
        );
        pnl_OkButtonsLayout.setVerticalGroup(
            pnl_OkButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_OkButtonsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_OkButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_Note)
                    .addComponent(btn_Cancel)
                    .addComponent(btn_Ok))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        lbl_Note.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_Note.AccessibleContext.accessibleName")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pnl_ChordDescription, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnl_OkButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tabbedPane, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnl_ChordDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabbedPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_OkButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void list_scalesValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_scalesValueChanged
    {//GEN-HEADEREND:event_list_scalesValueChanged
        if (!evt.getValueIsAdjusting())
        {
            StandardScaleInstance ssi = this.getScaleInstance();
            updateScaleInfo(ssi);
            lbl_optionalText.setText(getOptionalText(getPlayStyle(), getScaleInstance()));
        }
    }//GEN-LAST:event_list_scalesValueChanged

   private void tf_ChordSymbolNameActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_ChordSymbolNameActionPerformed
   {//GEN-HEADEREND:event_tf_ChordSymbolNameActionPerformed
       if (checkChordSymbol())
       {
           btn_OkActionPerformed(null);
       }
   }//GEN-LAST:event_tf_ChordSymbolNameActionPerformed

   private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
   {//GEN-HEADEREND:event_btn_CancelActionPerformed
       exitOk = false;
       setVisible(false);
   }//GEN-LAST:event_btn_CancelActionPerformed

   private void btn_OkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_OkActionPerformed
   {//GEN-HEADEREND:event_btn_OkActionPerformed
       exitOk = true;
       setVisible(false);
   }//GEN-LAST:event_btn_OkActionPerformed

   private void rbtn_normalActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_normalActionPerformed
   {//GEN-HEADEREND:event_rbtn_normalActionPerformed
       lbl_optionalText.setText(getOptionalText(getPlayStyle(), getScaleInstance()));
   }//GEN-LAST:event_rbtn_normalActionPerformed

   private void rbtn_holdActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_holdActionPerformed
   {//GEN-HEADEREND:event_rbtn_holdActionPerformed
       rbtn_normalActionPerformed(null);
   }//GEN-LAST:event_rbtn_holdActionPerformed

   private void rbtn_shotActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_shotActionPerformed
   {//GEN-HEADEREND:event_rbtn_shotActionPerformed
       rbtn_normalActionPerformed(null);
   }//GEN-LAST:event_rbtn_shotActionPerformed

   private void rbtn_accentActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_accentActionPerformed
   {//GEN-HEADEREND:event_rbtn_accentActionPerformed
       rbtn_normalActionPerformed(null);
   }//GEN-LAST:event_rbtn_accentActionPerformed

    private void cb_enableAlternateActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_enableAlternateActionPerformed
    {//GEN-HEADEREND:event_cb_enableAlternateActionPerformed
        boolean b = cb_enableAlternate.isSelected();
        Utilities.setRecursiveEnabled(b, pnl_altChordSymbol);
        Utilities.setRecursiveEnabled(b, pnl_altCondition);
        if (b)
        {
            // Possibly disable some specific components
            cb_useVoidAltActionPerformed(null);
            list_markerValues.setEnabled(rbtn_marker.isSelected());
        }
        lbl_optionalAltText.setText(getAltOptionalText(getAltChordSymbol(), getAltFilter()));
    }//GEN-LAST:event_cb_enableAlternateActionPerformed

    private void rbtn_markerActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_markerActionPerformed
    {//GEN-HEADEREND:event_rbtn_markerActionPerformed
        list_markerValues.setEnabled(rbtn_marker.isSelected());
        lbl_optionalAltText.setText(getAltOptionalText(getAltChordSymbol(), getAltFilter()));
    }//GEN-LAST:event_rbtn_markerActionPerformed

    private void cb_useVoidAltActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_useVoidAltActionPerformed
    {//GEN-HEADEREND:event_cb_useVoidAltActionPerformed
        boolean b = cb_useVoidAlt.isSelected();
        btn_setAltChordSymbol.setEnabled(!b);
        lbl_altChordSymbol.setEnabled(!b);
        lbl_optionalAltText.setText(getAltOptionalText(getAltChordSymbol(), getAltFilter()));
    }//GEN-LAST:event_cb_useVoidAltActionPerformed

    private void rbtn_randomActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_randomActionPerformed
    {//GEN-HEADEREND:event_rbtn_randomActionPerformed
        list_markerValues.setEnabled(!rbtn_random.isSelected());
        lbl_optionalAltText.setText(getAltOptionalText(getAltChordSymbol(), getAltFilter()));
    }//GEN-LAST:event_rbtn_randomActionPerformed

    private void btn_setAltChordSymbolActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_setAltChordSymbolActionPerformed
    {//GEN-HEADEREND:event_btn_setAltChordSymbolActionPerformed
        if (ALT_INSTANCE == null)
        {
            ALT_INSTANCE = new ChordSymbolEditorDialogImpl();
        }
        CLI_Factory clif = CLI_Factory.getDefault();
        Position position = model.getPosition();
        CLI_ChordSymbol altCli = clif.createChordSymbol(null, altChordSymbol, position);
        ALT_INSTANCE.preset("Alternate Chord Symbol for " + model.getData().getName(), altCli, (char) 0, false);
        ALT_INSTANCE.setLocation(getLocation().x + 40, getLocation().y + 40);
        ALT_INSTANCE.setVisible(true);
        if (ALT_INSTANCE.exitedOk())
        {
            ExtChordSymbol ecs = ALT_INSTANCE.getData();
            altChordSymbol = new AltExtChordSymbol(ecs.getRootNote(), ecs.getBassNote(), ecs.getChordType(), ecs.getRenderingInfo());
            lbl_altChordSymbol.setText(altChordSymbol.toString());
            lbl_optionalAltText.setText(getAltOptionalText(getAltChordSymbol(), getAltFilter()));
        }
        ALT_INSTANCE.cleanup();
    }//GEN-LAST:event_btn_setAltChordSymbolActionPerformed

    /**
     * Overridden to add global key bindings
     *
     * @return
     */
    @Override
    protected JRootPane createRootPane()
    {
        JRootPane contentPane = new JRootPane();
//      contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "actionOk");
//      contentPane.getActionMap().put("actionOk", new AbstractAction("OK")
//      {
//
//         @Override
//         public void actionPerformed(ActionEvent e)
//         {
//            btn_OkActionPerformed(null);
//         }
//      });

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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup btnGroup_chordType;
    private javax.swing.ButtonGroup btnGroup_offBeatStyle;
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JButton btn_setAltChordSymbol;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox cb_anticipateAllowed;
    private javax.swing.JCheckBox cb_enableAlternate;
    private javax.swing.JCheckBox cb_useVoidAlt;
    private javax.swing.Box.Filler filler1;
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea2;
    private org.jjazz.ui.utilities.HelpTextArea hlptxt_playStyle;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JLabel lbl_Note;
    private javax.swing.JLabel lbl_altChordSymbol;
    private javax.swing.JLabel lbl_chordNotes;
    private javax.swing.JLabel lbl_optionalAltText;
    private javax.swing.JLabel lbl_optionalText;
    private javax.swing.JLabel lbl_scaleNotes;
    private javax.swing.JLabel lbl_scaleTip;
    private javax.swing.JList<String> list_markerValues;
    private javax.swing.JList<StandardScaleInstance> list_scales;
    private javax.swing.JPanel pnl_Alternate;
    private javax.swing.JPanel pnl_ChordDescription;
    private javax.swing.JPanel pnl_Harmony;
    private javax.swing.JPanel pnl_Interpretation;
    private javax.swing.JPanel pnl_OkButtons;
    private javax.swing.JPanel pnl_altChordSymbol;
    private javax.swing.JPanel pnl_altCondition;
    private javax.swing.JPanel pnl_playStyleButtons;
    private javax.swing.JRadioButton rbtn_accent;
    private javax.swing.JRadioButton rbtn_hold;
    private javax.swing.JRadioButton rbtn_marker;
    private javax.swing.JRadioButton rbtn_normal;
    private javax.swing.JRadioButton rbtn_random;
    private javax.swing.JRadioButton rbtn_shot;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JTextField tf_ChordSymbolName;
    // End of variables declaration//GEN-END:variables

    //==========================================================================================================
    // Private classes
    //==========================================================================================================
    private class ChordSymbolDocumentListener implements DocumentListener
    {

        @Override
        public void insertUpdate(DocumentEvent e)
        {
            checkChordSymbol();
        }

        @Override
        public void removeUpdate(DocumentEvent e)
        {
            checkChordSymbol();
        }

        @Override
        public void changedUpdate(DocumentEvent e)
        {
            // Plain text components don't fire these events.
        }
    }

}
