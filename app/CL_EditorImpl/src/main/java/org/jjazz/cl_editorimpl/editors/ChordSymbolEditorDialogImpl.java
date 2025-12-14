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
package org.jjazz.cl_editorimpl.editors;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.spi.ScaleManager;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.jjazz.chordleadsheet.api.item.AltDataFilter;
import org.jjazz.chordleadsheet.api.item.AltExtChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.cl_editor.spi.ChordSymbolEditorDialog;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.lookup.ServiceProvider;

/**
 * Default chord symbol edit dialog.
 */
@ServiceProvider(service = ChordSymbolEditorDialog.class)
public class ChordSymbolEditorDialogImpl extends ChordSymbolEditorDialog implements ListSelectionListener
{

    private static ChordSymbolEditorDialogImpl ALT_INSTANCE;
    private CLI_ChordSymbol model;
    private boolean exitOk;
    private AltExtChordSymbol altChordSymbol;
    private final DefaultListModel<StandardScaleInstance> stdScales = new DefaultListModel<>();
    private final DefaultListModel<String> markers = new DefaultListModel<>();
    private int displayTransposition;
    private static final Logger LOGGER = Logger.getLogger(ChordSymbolEditorDialogImpl.class.getSimpleName());


    /**
     * Creates new form EditChordOptionsDialog
     */
    public ChordSymbolEditorDialogImpl()
    {
        super();

        displayTransposition = 0;

        for (String marker : RP_SYS_Marker.getInstance().getPossibleValues())
        {
            markers.addElement(marker);
        }

        initComponents();

        tf_ChordSymbolName.getDocument().addDocumentListener(new ChordSymbolDocumentListener());
        updateScaleInfo(null);

    }

    @Override
    public void preset(String title, CLI_ChordSymbol cliCs, char key, boolean enableAlternate)
    {
        if (cliCs == null)
        {
            throw new NullPointerException("cliCs");
        }
        exitOk = false;
        model = displayTransposition == 0 ? cliCs : (CLI_ChordSymbol) cliCs.getCopy(cliCs.getData().getTransposedChordSymbol(displayTransposition, null), null);
        ExtChordSymbol ecs = model.getData();
        ChordRenderingInfo cri = ecs.getRenderingInfo();
        setTitle(title);
        tabbedPane.setSelectedIndex(0);


        // Update PlayStyle UI
        cb_pedalBass.setSelected(cri.hasOneFeature(Feature.PEDAL_BASS));
        cb_crash.setSelected(cri.hasOneFeature(Feature.CRASH));
        cb_noCrash.setSelected(cri.hasOneFeature(Feature.NO_CRASH));
        cb_moreInstruments.setSelected(cri.hasOneFeature(Feature.EXTENDED_HOLD_SHOT));
        cb_stronger.setSelected(cri.hasOneFeature(Feature.ACCENT_STRONGER));
        if (cri.hasOneFeature(Feature.HOLD))
        {
            rbtn_hold.setSelected(true);
        } else if (cri.hasOneFeature(Feature.SHOT))
        {
            rbtn_shot.setSelected(true);
        } else if (cri.getAccentFeature() != null)
        {
            rbtn_accent.setSelected(true);
        } else
        {
            rbtn_normal.setSelected(true);
        }

        enableAccentDependentUI();
        enableHoldShotDependentUI();


        // Update Scales UI      
        updateScales(ecs);

        list_scales.clearSelection();
        StandardScaleInstance ssi = cri.getScaleInstance();
        if (ssi
                != null)
        {
            list_scales.setSelectedValue(ssi, true);
        }

        updateChordSymbolInfo(ecs);

        updateOptionalText();

        // Update chord UI        
        tf_ChordSymbolName.requestFocus();
        Font f = tf_ChordSymbolName.getFont();
        tf_ChordSymbolName.setFont(f.deriveFont(displayTransposition == 0 ? Font.PLAIN : Font.ITALIC));
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
                    altChordSymbol = new AltExtChordSymbol(ecs, ecs.getRenderingInfo());
                } else
                {
                    cb_useVoidAlt.setSelected(false);
                    altChordSymbol = ecs.getAlternateChordSymbol();
                }
                altFilter = ecs.getAlternateFilter();
            } else
            {
                // No alternate define, just set consistent values if ever user enables the alternate chord symbol
                cb_enableAlternate.setSelected(false);
                altChordSymbol = new AltExtChordSymbol(ecs, ecs.getRenderingInfo());
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
                    if (index == -1)
                    {
                        // There is a problem (can happen if e.g. .sng was manually updated to change the AltDataFilter values)
                        // Do not add the value
                        LOGGER.log(Level.WARNING, "preset() cliCs={0} invalid AltFilter marker value={1}", new Object[]
                        {
                            cliCs, marker
                        });
                    } else
                    {
                        list_markerValues.addSelectionInterval(index, index);
                    }
                }
                // We need at least one marker selected
                if (list_markerValues.getSelectedIndex() == -1)
                {
                    list_markerValues.addSelectionInterval(0, 0);
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

        pack();

    }

    @Override
    public void cleanup()
    {
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
        ChordRenderingInfo cri = new ChordRenderingInfo(getFeatures(), getScaleInstance());
        String text = tf_ChordSymbolName.getText();
        ExtChordSymbol ecs = null;
        try
        {
            ecs = ExtChordSymbol.get(text, cri, getAltChordSymbol(), getAltFilter());
            ecs = ecs.getTransposedChordSymbol(-displayTransposition, null);
        } catch (ParseException ex)
        {
            throw new IllegalStateException("text=" + text + " :" + ex.getMessage());
        }
        return ecs;
    }
    // ------------------------------------------------------------------------------
    // DisplayTransposableRenderer interface
    // ------------------------------------------------------------------------------    

    @Override
    public void setDisplayTransposition(int dt)
    {
        displayTransposition = dt;
    }

    @Override
    public int getDisplayTransposition()
    {
        return displayTransposition;
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
    private EnumSet<Feature> getFeatures()
    {
        EnumSet<Feature> res = EnumSet.noneOf(Feature.class);
        if (rbtn_normal.isSelected())
        {
            // Nothing
        } else
        {
            res.add(cb_stronger.isSelected() ? Feature.ACCENT_STRONGER : Feature.ACCENT);
            if (cb_crash.isSelected())
            {
                res.add(Feature.CRASH);
            } else if (cb_noCrash.isSelected())
            {
                res.add(Feature.NO_CRASH);
            }
        }
        if (rbtn_hold.isSelected())
        {
            res.add(Feature.HOLD);
        } else if (this.rbtn_shot.isSelected())
        {
            res.add(Feature.SHOT);
        }
        if ((res.contains(Feature.HOLD) || res.contains(Feature.SHOT)) && cb_moreInstruments.isSelected())
        {
            res.add(Feature.EXTENDED_HOLD_SHOT);
        }

        if (cb_pedalBass.isSelected())
        {
            res.add(Feature.PEDAL_BASS);
        }


        return res;
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

    private void updateOptionalText()
    {
        lbl_optionalText.setText(getOptionalText(new ChordRenderingInfo(getFeatures(), getScaleInstance())));
        // pack();     Fix Issue #317 Freeze when entering hebrew char in chord edit dialog then exit dialog
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
            int index = ScaleManager.getDefault().getStandardScales().indexOf(ssi.getScale());
            if (index > 0 && index < 7)
            {
                int pitch = 12 + ssi.getStartNote().getRelativePitch() - ScaleManager.MAJOR.getNotes().get(index).getRelativePitch();
                Note n = new Note(pitch);
                scaleTip = ResUtil.getString(getClass(), "ChordSymbolEditorDialogImpl.ScaleTip", n.toRelativeNoteString(),
                        ssi.getStartNote().toRelativeNoteString());
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
                ecs = ExtChordSymbol.get(text);
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
            updateOptionalText();
            btn_Ok.setEnabled(true);
            this.tf_ChordSymbolName.setForeground(UIManager.getDefaults().getColor("Label.foreground"));
        } else
        {
            updateChordSymbolInfo(null);
            btn_Ok.setEnabled(false);
            this.tf_ChordSymbolName.setForeground(UIManager.getDefaults().getColor("Label.disabledForeground"));
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
        if (altSymbol == null)
        {
            return "-"; // Avoid empty string to avoid UI vertical relayout  
        }
        var strs = new ArrayList<String>();

        strs.add(altSymbol == VoidAltExtChordSymbol.getInstance()
                ? ResUtil.getString(getClass(), "ChordSymbolEditorDialogImpl.void")
                : altSymbol.toString());

        ChordRenderingInfo cri = altSymbol.getRenderingInfo();
        if (altSymbol != VoidAltExtChordSymbol.getInstance() && !cri.equals(new ChordRenderingInfo()))
        {
            strs.add(getOptionalText(cri));
        }

        strs.add(ResUtil.getString(getClass(), "ChordSymbolEditorDialogImpl.condition") + "=" + (altFilter.isRandom()
                ? ResUtil.getString(getClass(), "ChordSymbolEditorDialogImpl.random")
                : altFilter.getValues()));
        return toNiceString(strs);
    }

    private String getOptionalText(ChordRenderingInfo cri)
    {
        String s = cri.getFeatures().toString().replace("[]", "[ ]") + (cri.getScaleInstance() == null ? "" : " - " + cri.getScaleInstance());
        return s;
    }

    private void updateScales(ExtChordSymbol ecs)
    {
        List<StandardScaleInstance> saveSelectedValues = list_scales.getSelectedValuesList();
        stdScales.clear();
        List<Integer> indices = new ArrayList<>();
        int index = 0;
        for (StandardScaleInstance ssi : ScaleManager.getDefault().getMatchingScales(ecs))
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

    private void enableAccentDependentUI()
    {
        boolean b = !this.rbtn_normal.isSelected();
        this.cb_crash.setEnabled(b);
        this.cb_noCrash.setEnabled(b);
        this.cb_stronger.setEnabled(b);
    }

    private void enableHoldShotDependentUI()
    {
        boolean b = this.rbtn_hold.isSelected() || this.rbtn_shot.isSelected();
        this.cb_moreInstruments.setEnabled(b);
    }

    private String toNiceString(List<String> strs)
    {
        if (strs.isEmpty())
        {
            return "-";
        }
        return strs.stream().collect(Collectors.joining(" - "));
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        btnGroup_Condition = new javax.swing.ButtonGroup();
        btnGroup_Accent = new javax.swing.ButtonGroup();
        pnl_ChordDescription = new javax.swing.JPanel();
        tf_ChordSymbolName = new javax.swing.JTextField();
        lbl_chordNotes = new javax.swing.JLabel();
        lbl_optionalText = new javax.swing.JLabel();
        lbl_optionalAltText = new javax.swing.JLabel();
        lbl_alternate = new javax.swing.JLabel();
        tabbedPane = new javax.swing.JTabbedPane();
        pnl_Interpretation = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        rbtn_normal = new javax.swing.JRadioButton();
        rbtn_accent = new javax.swing.JRadioButton();
        rbtn_hold = new javax.swing.JRadioButton();
        rbtn_shot = new javax.swing.JRadioButton();
        cb_moreInstruments = new javax.swing.JCheckBox();
        cb_stronger = new javax.swing.JCheckBox();
        cb_noCrash = new javax.swing.JCheckBox();
        cb_crash = new javax.swing.JCheckBox();
        cb_pedalBass = new javax.swing.JCheckBox();
        lbl_tooltip = new javax.swing.JLabel();
        pnl_Harmony = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_scales = new JList<>(stdScales);
        jScrollPane3 = new javax.swing.JScrollPane();
        helpTextArea2 = new org.jjazz.flatcomponents.api.HelpTextArea();
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
        btn_resetOptions = new javax.swing.JButton();
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
        org.openide.awt.Mnemonics.setLocalizedText(lbl_chordNotes, "[C E G Bb D F#]   bar3 beat2"); // NOI18N
        lbl_chordNotes.setAlignmentX(0.5F);
        lbl_chordNotes.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_optionalText, "optional info"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_optionalAltText, "alt optional info"); // NOI18N

        lbl_alternate.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_alternate, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_alternate.text")); // NOI18N

        javax.swing.GroupLayout pnl_ChordDescriptionLayout = new javax.swing.GroupLayout(pnl_ChordDescription);
        pnl_ChordDescription.setLayout(pnl_ChordDescriptionLayout);
        pnl_ChordDescriptionLayout.setHorizontalGroup(
            pnl_ChordDescriptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_ChordDescriptionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_ChordDescriptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(tf_ChordSymbolName)
                    .addComponent(lbl_alternate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_ChordDescriptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_chordNotes)
                    .addComponent(lbl_optionalAltText)
                    .addComponent(lbl_optionalText))
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
                    .addComponent(lbl_alternate))
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        btnGroup_Accent.add(rbtn_normal);
        rbtn_normal.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_normal, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_normal.text")); // NOI18N
        rbtn_normal.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                rbtn_normalStateChanged(evt);
            }
        });

        btnGroup_Accent.add(rbtn_accent);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_accent, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_accent.text")); // NOI18N
        rbtn_accent.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_accent.toolTipText")); // NOI18N
        rbtn_accent.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                rbtn_accentStateChanged(evt);
            }
        });

        btnGroup_Accent.add(rbtn_hold);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_hold, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_hold.text")); // NOI18N
        rbtn_hold.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_hold.toolTipText")); // NOI18N
        rbtn_hold.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                rbtn_holdStateChanged(evt);
            }
        });

        btnGroup_Accent.add(rbtn_shot);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_shot, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_shot.text")); // NOI18N
        rbtn_shot.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.rbtn_shot.toolTipText")); // NOI18N
        rbtn_shot.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                rbtn_shotStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_moreInstruments, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_moreInstruments.text")); // NOI18N
        cb_moreInstruments.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_moreInstruments.toolTipText")); // NOI18N
        cb_moreInstruments.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_moreInstrumentsActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_stronger, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_stronger.text")); // NOI18N
        cb_stronger.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_stronger.toolTipText")); // NOI18N
        cb_stronger.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_strongerActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_noCrash, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_noCrash.text")); // NOI18N
        cb_noCrash.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_noCrash.toolTipText")); // NOI18N
        cb_noCrash.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_noCrashActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_crash, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_crash.text")); // NOI18N
        cb_crash.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_crash.toolTipText")); // NOI18N
        cb_crash.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_crashActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbtn_shot)
                    .addComponent(rbtn_normal)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rbtn_accent)
                            .addComponent(rbtn_hold))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(cb_stronger)
                                .addGap(18, 18, 18)
                                .addComponent(cb_crash)
                                .addGap(18, 18, 18)
                                .addComponent(cb_noCrash))
                            .addComponent(cb_moreInstruments))))
                .addContainerGap(84, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rbtn_normal)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbtn_accent)
                    .addComponent(cb_stronger)
                    .addComponent(cb_noCrash)
                    .addComponent(cb_crash))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbtn_hold)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbtn_shot)
                    .addComponent(cb_moreInstruments))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.openide.awt.Mnemonics.setLocalizedText(cb_pedalBass, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_pedalBass.text")); // NOI18N
        cb_pedalBass.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.cb_pedalBass.toolTipText")); // NOI18N
        cb_pedalBass.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_pedalBassActionPerformed(evt);
            }
        });

        lbl_tooltip.setFont(helpTextArea2.getFont());
        org.openide.awt.Mnemonics.setLocalizedText(lbl_tooltip, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.lbl_tooltip.text")); // NOI18N

        javax.swing.GroupLayout pnl_InterpretationLayout = new javax.swing.GroupLayout(pnl_Interpretation);
        pnl_Interpretation.setLayout(pnl_InterpretationLayout);
        pnl_InterpretationLayout.setHorizontalGroup(
            pnl_InterpretationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_InterpretationLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(pnl_InterpretationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_InterpretationLayout.createSequentialGroup()
                        .addComponent(cb_pedalBass)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbl_tooltip))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnl_InterpretationLayout.setVerticalGroup(
            pnl_InterpretationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_InterpretationLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(pnl_InterpretationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cb_pedalBass)
                    .addComponent(lbl_tooltip))
                .addContainerGap())
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

        org.openide.awt.Mnemonics.setLocalizedText(lbl_scaleNotes, "[C, D, E, F, G, A, B]"); // NOI18N

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
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)
                    .addComponent(lbl_scaleTip, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lbl_scaleNotes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnl_HarmonyLayout.setVerticalGroup(
            pnl_HarmonyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_HarmonyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_HarmonyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 156, Short.MAX_VALUE)
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

        btnGroup_Condition.add(rbtn_random);
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

        btnGroup_Condition.add(rbtn_marker);
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
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
                        .addContainerGap(42, Short.MAX_VALUE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );

        pnl_altChordSymbol.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.pnl_altChordSymbol.border.title"))); // NOI18N

        lbl_altChordSymbol.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_altChordSymbol, "C7M"); // NOI18N
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
                        .addComponent(pnl_altChordSymbol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnl_altCondition, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(pnl_AlternateLayout.createSequentialGroup()
                        .addComponent(cb_enableAlternate)
                        .addContainerGap(113, Short.MAX_VALUE))))
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

        org.openide.awt.Mnemonics.setLocalizedText(btn_resetOptions, org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.btn_resetOptions.text")); // NOI18N
        btn_resetOptions.setToolTipText(org.openide.util.NbBundle.getMessage(ChordSymbolEditorDialogImpl.class, "ChordSymbolEditorDialogImpl.btn_resetOptions.toolTipText")); // NOI18N
        btn_resetOptions.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_resetOptionsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_OkButtonsLayout = new javax.swing.GroupLayout(pnl_OkButtons);
        pnl_OkButtons.setLayout(pnl_OkButtonsLayout);
        pnl_OkButtonsLayout.setHorizontalGroup(
            pnl_OkButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_OkButtonsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_resetOptions)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnl_OkButtonsLayout.setVerticalGroup(
            pnl_OkButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_OkButtonsLayout.createSequentialGroup()
                .addComponent(btn_resetOptions)
                .addGap(0, 11, Short.MAX_VALUE))
        );

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pnl_ChordDescription, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnl_OkButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Ok, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_Cancel)
                        .addContainerGap())
                    .addComponent(tabbedPane, javax.swing.GroupLayout.Alignment.LEADING)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnl_ChordDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabbedPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_OkButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_Cancel)
                        .addComponent(btn_Ok))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void list_scalesValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_scalesValueChanged
    {//GEN-HEADEREND:event_list_scalesValueChanged
        if (!evt.getValueIsAdjusting())
        {
            StandardScaleInstance ssi = getScaleInstance();
            updateScaleInfo(ssi);
            updateOptionalText();
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

    private void cb_enableAlternateActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_enableAlternateActionPerformed
    {//GEN-HEADEREND:event_cb_enableAlternateActionPerformed
        boolean b = cb_enableAlternate.isSelected();
        UIUtilities.setRecursiveEnabled(b, pnl_altChordSymbol);
        UIUtilities.setRecursiveEnabled(b, pnl_altCondition);
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
        CLI_ChordSymbol altCli = clif.createChordSymbol(altChordSymbol, position);
        String msg = ResUtil.getString(getClass(), "ChordSymbolEditorDialogImpl.AltChordSymbolFor", model.getData().getName());
        ALT_INSTANCE.preset(msg, altCli, (char) 0, false);
        ALT_INSTANCE.setLocation(getLocation().x + 40, getLocation().y + 40);
        ALT_INSTANCE.setVisible(true);
        if (ALT_INSTANCE.exitedOk())
        {
            ExtChordSymbol ecs = ALT_INSTANCE.getData();
            altChordSymbol = new AltExtChordSymbol(ecs, ecs.getRenderingInfo());
            lbl_altChordSymbol.setText(altChordSymbol.toString());
            lbl_optionalAltText.setText(getAltOptionalText(getAltChordSymbol(), getAltFilter()));
        }
        ALT_INSTANCE.cleanup();
    }//GEN-LAST:event_btn_setAltChordSymbolActionPerformed

    private void cb_strongerActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_strongerActionPerformed
    {//GEN-HEADEREND:event_cb_strongerActionPerformed
        updateOptionalText();
    }//GEN-LAST:event_cb_strongerActionPerformed


    private void cb_pedalBassActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_pedalBassActionPerformed
    {//GEN-HEADEREND:event_cb_pedalBassActionPerformed
        updateOptionalText();
    }//GEN-LAST:event_cb_pedalBassActionPerformed

    private void cb_moreInstrumentsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_moreInstrumentsActionPerformed
    {//GEN-HEADEREND:event_cb_moreInstrumentsActionPerformed
        updateOptionalText();
    }//GEN-LAST:event_cb_moreInstrumentsActionPerformed

    private void rbtn_accentStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_rbtn_accentStateChanged
    {//GEN-HEADEREND:event_rbtn_accentStateChanged
        enableAccentDependentUI();
        enableHoldShotDependentUI();
        updateOptionalText();
    }//GEN-LAST:event_rbtn_accentStateChanged

    private void rbtn_holdStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_rbtn_holdStateChanged
    {//GEN-HEADEREND:event_rbtn_holdStateChanged
        enableAccentDependentUI();
        enableHoldShotDependentUI();
        updateOptionalText();
    }//GEN-LAST:event_rbtn_holdStateChanged

    private void rbtn_shotStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_rbtn_shotStateChanged
    {//GEN-HEADEREND:event_rbtn_shotStateChanged
        enableAccentDependentUI();
        enableHoldShotDependentUI();
        updateOptionalText();
    }//GEN-LAST:event_rbtn_shotStateChanged

    private void rbtn_normalStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_rbtn_normalStateChanged
    {//GEN-HEADEREND:event_rbtn_normalStateChanged
        enableAccentDependentUI();
        enableHoldShotDependentUI();
        updateOptionalText();
    }//GEN-LAST:event_rbtn_normalStateChanged

    private void cb_crashActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_crashActionPerformed
    {//GEN-HEADEREND:event_cb_crashActionPerformed
        updateOptionalText();
        if (cb_crash.isSelected())
        {
            cb_noCrash.setSelected(false);
        }
    }//GEN-LAST:event_cb_crashActionPerformed

    private void cb_noCrashActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_noCrashActionPerformed
    {//GEN-HEADEREND:event_cb_noCrashActionPerformed
        updateOptionalText();
        if (cb_noCrash.isSelected())
        {
            cb_crash.setSelected(false);
        }
    }//GEN-LAST:event_cb_noCrashActionPerformed

    private void btn_resetOptionsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_resetOptionsActionPerformed
    {//GEN-HEADEREND:event_btn_resetOptionsActionPerformed
        rbtn_normal.setSelected(true);      // Will disable the other options etc.
        list_scales.clearSelection();
        if (cb_enableAlternate.isSelected())
        {
            cb_enableAlternate.setSelected(false);   // Does not trigger the action
            cb_enableAlternateActionPerformed(null);
        }
        cb_pedalBass.setSelected(false);
        cb_pedalBassActionPerformed(evt);
    }//GEN-LAST:event_btn_resetOptionsActionPerformed

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
    private javax.swing.ButtonGroup btnGroup_Accent;
    private javax.swing.ButtonGroup btnGroup_Condition;
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JButton btn_resetOptions;
    private javax.swing.JButton btn_setAltChordSymbol;
    private javax.swing.JCheckBox cb_crash;
    private javax.swing.JCheckBox cb_enableAlternate;
    private javax.swing.JCheckBox cb_moreInstruments;
    private javax.swing.JCheckBox cb_noCrash;
    private javax.swing.JCheckBox cb_pedalBass;
    private javax.swing.JCheckBox cb_stronger;
    private javax.swing.JCheckBox cb_useVoidAlt;
    private org.jjazz.flatcomponents.api.HelpTextArea helpTextArea2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JLabel lbl_altChordSymbol;
    private javax.swing.JLabel lbl_alternate;
    private javax.swing.JLabel lbl_chordNotes;
    private javax.swing.JLabel lbl_optionalAltText;
    private javax.swing.JLabel lbl_optionalText;
    private javax.swing.JLabel lbl_scaleNotes;
    private javax.swing.JLabel lbl_scaleTip;
    private javax.swing.JLabel lbl_tooltip;
    private javax.swing.JList<String> list_markerValues;
    private javax.swing.JList<StandardScaleInstance> list_scales;
    private javax.swing.JPanel pnl_Alternate;
    private javax.swing.JPanel pnl_ChordDescription;
    private javax.swing.JPanel pnl_Harmony;
    private javax.swing.JPanel pnl_Interpretation;
    private javax.swing.JPanel pnl_OkButtons;
    private javax.swing.JPanel pnl_altChordSymbol;
    private javax.swing.JPanel pnl_altCondition;
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
