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
package org.jjazz.mixconsole;

import org.jjazz.musiccontrol.api.MidiActivityMonitor;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.flatcomponents.api.FlatButton;
import org.jjazz.flatcomponents.api.FlatIntegerKnob;
import org.jjazz.flatcomponents.api.FlatIntegerVerticalSlider;
import org.jjazz.flatcomponents.api.FlatTextEditDialog;
import org.jjazz.mixconsole.api.MixConsoleSettings;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.utilities.api.Utilities;

/**
 * The panel to edit the channel Instrument and InstrumentMix.
 */
public class MixChannelPanel extends javax.swing.JPanel implements PropertyChangeListener, MidiActivityMonitor.Listener
{

    private MixChannelPanelModel model;
    private MixChannelPanelController controller;
    private MixConsoleSettings settings;
    private FlatButton fbtn_channelNameImpl;
    private boolean selected;
    private final Font FONT = GeneralUISettings.getInstance().getStdCondensedFont();
    private static final Logger LOGGER = Logger.getLogger(MixChannelPanel.class.getSimpleName());

    /**
     * Creates a new InstrumentChannel with a basic model.
     * <p>
     */
    public MixChannelPanel()
    {
        model = new BaseMixChannelPanelModel();
    }

    public MixChannelPanel(final MixChannelPanelModel model, final MixChannelPanelController controller, MixConsoleSettings settings)
    {
        if (model == null || controller == null || settings == null)
        {
            throw new IllegalArgumentException("model=" + model + " controller=" + controller + " settings=" + settings);
        }
        this.model = model;
        this.controller = controller;
        this.settings = settings;


        this.settings.addPropertyChangeListener(this);
        this.model.addPropertyChangeListener(this);

        MidiActivityMonitor.getInstance().addListener(this, this.model.getChannelId());

        if (model.isUserChannel())
        {
            fbtn_channelNameImpl = new FlatButton(org.jjazz.uiutilities.api.UIUtilities.getAction(ae -> userChannelNameClicked()), true, true, false);
        } else
        {
            fbtn_channelNameImpl = new FlatButton(false, false, false);
        }


        initComponents();


        knob_panoramic.addPropertyChangeListener(this);
        knob_reverb.addPropertyChangeListener(this);
        knob_chorus.addPropertyChangeListener(this);


        // Adjust some icons designed for dark theme
        fbtn_mute.setUnselectedIcon(GeneralUISettings.adaptIconToLightThemeIfRequired((ImageIcon) fbtn_mute.getUnselectedIcon(), -40));
        fbtn_solo.setUnselectedIcon(GeneralUISettings.adaptIconToLightThemeIfRequired((ImageIcon) fbtn_solo.getUnselectedIcon(), -40));
        fbtn_Settings.setIcon(GeneralUISettings.adaptIconToLightThemeIfRequired((ImageIcon) fbtn_Settings.getIcon(), -40));


        this.lbl_Icon.setText(null);


        // Listen to UI changes
        this.fslider_volume.addPropertyChangeListener(this);


        refreshUI();
    }

    public MixChannelPanelModel getModel()
    {
        return model;
    }

    public void cleanup()
    {
        MidiActivityMonitor.getInstance().removeListener(this, model.getChannelId());
        model.removePropertyChangeListener(this);
        settings.removePropertyChangeListener(this);
        model.cleanup();
        model = null;
    }

    public void setSelected(boolean b)
    {
        roundedPanel.setShowBorder(b);
        selected = b;
    }

    public boolean isSelected()
    {
        return selected;
    }

    // ----------------------------------------------------------------------------
    // MidiActivityMonitor.Listener interface
    // ----------------------------------------------------------------------------
    @Override
    public void showMidiActivity(int channel)
    {
        fled_midiActivity.showActivity();
    }


    // ----------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // UI changed, update model
        if (evt.getSource() == this.knob_chorus)
        {
            if (evt.getPropertyName().equals(FlatIntegerKnob.PROP_VALUE))
            {
                model.setChorus(knob_chorus.getValue());
            } else if ("enabled".equals(evt.getPropertyName()))
            {
                lbl_cho.setEnabled(knob_chorus.isEnabled());
            }
        } else if (evt.getSource() == this.knob_reverb)
        {
            if (evt.getPropertyName().equals(FlatIntegerKnob.PROP_VALUE))
            {
                model.setReverb(knob_reverb.getValue());
            } else if ("enabled".equals(evt.getPropertyName()))
            {
                lbl_rev.setEnabled(knob_reverb.isEnabled());
            }
        } else if (evt.getSource() == this.knob_panoramic)
        {
            if (evt.getPropertyName().equals(FlatIntegerKnob.PROP_VALUE))
            {
                model.setPanoramic(knob_panoramic.getValue());
            } else if ("enabled".equals(evt.getPropertyName()))
            {
                lbl_pan.setEnabled(knob_panoramic.isEnabled());
            }
        } else if (evt.getSource() == this.fslider_volume && evt.getPropertyName().equals(FlatIntegerVerticalSlider.PROP_VALUE))
        {
            int oldValue = (int) evt.getOldValue();
            int newValue = (int) evt.getNewValue();
            MouseEvent me = fslider_volume.getLastMouseEvent();
            model.setVolume(oldValue, newValue, me);
        } else if (evt.getSource() == model)
        {
            // MODEL changed, update UI
            refreshUI();
            if (evt.getPropertyName().equals(MixChannelPanelModel.PROP_CHANNEL_ID))
            {
                int oldChannel = (int) evt.getOldValue();
                int newChannel = (int) evt.getNewValue();
                var mam = MidiActivityMonitor.getInstance();
                mam.removeListener(this, oldChannel);
                mam.addListener(this, newChannel);
            }
        } else if (evt.getSource() == settings)
        {
            refreshUI();
        }
    }

// ----------------------------------------------------------------------------
// Private methods 
// ----------------------------------------------------------------------------
    /**
     * Model has changed, update UI.
     */
    private void refreshUI()
    {
        // Channel Id    
        String s = String.valueOf(model.getChannelId() + 1);
        String tt = "Midi Channel";
        if (model.isDrumsReroutingEnabled())
        {
            s += ">10";
            tt += " rerouted to Drums channel";
        }
        this.fbtn_channelId.setText(s);
        this.fbtn_channelId.setToolTipText(tt);


        // Instrument name
        Instrument ins = model.getInstrument();
        tt = ins.getFullName() + (ins.isDrumKit()
                ? ", DrumKit type=" + ins.getDrumKit().getType().toString() + " keymap= " + ins.getDrumKit().getKeyMap().getName() : "");
        fbtn_Instrument.setToolTipText(tt);
        String patchName = Utilities.truncateWithDots(ins.getPatchName(), 18);
        this.fbtn_Instrument.setvLabel(patchName);


        // Widget values and state
        this.fbtn_mute.setSelected(model.isMute());
        this.fbtn_solo.setSelected(model.isSolo());
        this.fslider_volume.setValue(model.getVolume());
        this.knob_chorus.setValue(model.getChorus());
        this.knob_panoramic.setValue(model.getPanoramic());
        this.knob_reverb.setValue(model.getReverb());
        knob_panoramic.setEnabled(model.isPanoramicEnabled());
        knob_reverb.setEnabled(model.isReverbEnabled());
        knob_chorus.setEnabled(model.isChorusEnabled());
        fslider_volume.setEnabled(model.isVolumeEnabled());
        fbtn_Instrument.setEnabled(model.isInstrumentEnabled());


        var icon = GeneralUISettings.adaptIconToLightThemeIfRequired(model.getIcon(), -20);
        this.lbl_Icon.setIcon(icon);
        this.lbl_Icon.setToolTipText(model.getIconTooltip());
        final int MAX_NB_CHARS = 9;
        this.fbtn_channelName.setText(Utilities.truncate(model.getChannelName(), MAX_NB_CHARS));
        this.fbtn_channelName.setToolTipText(model.getChannelNameTooltip());
        this.lbl_category.setText(Utilities.truncate(model.getCategory(), MAX_NB_CHARS));


        // Colors
        Color c = model.getChannelColor();
        knob_panoramic.setValueLineColor(c);
        knob_reverb.setValueLineColor(c);
        knob_chorus.setValueLineColor(c);
        fslider_volume.setValueLineColor(c);
        c = settings.getMixChannelBackgroundColor();
        roundedPanel.setBackground(c);

    }


    /**
     * Edit the text value of a FlatButton using an overlay dialog.
     *
     * @param fbtn
     * @return Null if no value change
     */
    private String editValueWithOverlayDialog(FlatButton fbtn, int nbColumns)
    {
        var dlg = FlatTextEditDialog.getInstance();
        dlg.setTextNbColumns(nbColumns);
        dlg.setTextHorizontalAlignment(JTextField.CENTER);
        String oldValue = fbtn.getText();
        dlg.setText(oldValue);
        dlg.setPositionCenter(fbtn);
        dlg.setVisible(true);
        String newValue = dlg.getText().trim();
        return dlg.isExitOk() && !newValue.equals(oldValue) && !newValue.isBlank() ? newValue : null;
    }

    private void userChannelNameClicked()
    {
        String strNewValue = editValueWithOverlayDialog(fbtn_channelName, 5);
        if (strNewValue != null)
        {
            controller.editChannelName(model.getChannelId(), strNewValue);
        }
    }

    private void startDragOut(MouseEvent evt)
    {
        TransferHandler th = getTransferHandler();  // set in MixConsole
        if (th != null && SwingUtilities.isLeftMouseButton(evt))
        {
            th.exportAsDrag(this, evt, TransferHandler.COPY);
            // Note that from now on our various mouse drag listeners won't be called anymore until DnD export operation is over
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        roundedPanel = new org.jjazz.flatcomponents.api.RoundedPanel();
        pnl_led_close = new javax.swing.JPanel();
        fled_midiActivity = new org.jjazz.flatcomponents.api.FlatLedIndicator();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(1, 0), new java.awt.Dimension(1, 0), new java.awt.Dimension(1, 32767));
        fbtn_Settings = new org.jjazz.flatcomponents.api.FlatButton();
        pnl_mute = new javax.swing.JPanel();
        fbtn_mute = new org.jjazz.flatcomponents.api.FlatToggleButton();
        pnl_solo = new javax.swing.JPanel();
        fbtn_solo = new org.jjazz.flatcomponents.api.FlatToggleButton();
        pnl_inst_volume = new javax.swing.JPanel();
        fslider_volume = new org.jjazz.flatcomponents.api.FlatIntegerVerticalSlider();
        fbtn_Instrument = new org.jjazz.mixconsole.VInstrumentButton();
        pnl_icon = new javax.swing.JPanel();
        lbl_Icon = new javax.swing.JLabel();
        pnl_category = new javax.swing.JPanel();
        lbl_category = new javax.swing.JLabel();
        pnl_name = new javax.swing.JPanel();
        fbtn_channelName = fbtn_channelNameImpl;
        pnl_channelId = new javax.swing.JPanel();
        fbtn_channelId = new org.jjazz.flatcomponents.api.FlatButton();
        knob_panoramic = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        knob_chorus = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        knob_reverb = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        lbl_pan = new javax.swing.JLabel();
        lbl_rev = new javax.swing.JLabel();
        lbl_cho = new javax.swing.JLabel();

        setBackground(new java.awt.Color(26, 26, 26));
        setMinimumSize(new java.awt.Dimension(10, 53));
        setOpaque(false);
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                formMouseDragged(evt);
            }
        });

        roundedPanel.setBackground(new java.awt.Color(46, 46, 46));
        roundedPanel.setArcDiameter(20);
        roundedPanel.setInheritsPopupMenu(true);
        roundedPanel.setInsetsThickness(1);
        roundedPanel.setMinimumSize(new java.awt.Dimension(20, 53));
        roundedPanel.setOpaque(true);

        pnl_led_close.setOpaque(false);
        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0);
        flowLayout1.setAlignOnBaseline(true);
        pnl_led_close.setLayout(flowLayout1);

        fled_midiActivity.setAlphaStepActivity(150);
        fled_midiActivity.setColorMax(new java.awt.Color(255, 67, 16));
        fled_midiActivity.setColorMin(new java.awt.Color(70, 70, 70));
        fled_midiActivity.setDiameter(8);
        pnl_led_close.add(fled_midiActivity);
        pnl_led_close.add(filler1);

        fbtn_Settings.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        fbtn_Settings.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        fbtn_Settings.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/mixconsole/resources/Settings11x11.png"))); // NOI18N
        fbtn_Settings.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_Settings.toolTipText")); // NOI18N
        fbtn_Settings.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_SettingsActionPerformed(evt);
            }
        });

        pnl_mute.setOpaque(false);
        pnl_mute.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                pnl_muteMouseDragged(evt);
            }
        });
        pnl_mute.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        fbtn_mute.setAlignmentX(0.5F);
        fbtn_mute.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/mixconsole/resources/MuteDisabled_Icon-21x21.png"))); // NOI18N
        fbtn_mute.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/mixconsole/resources/MuteOn_Icon-21x21.png"))); // NOI18N
        fbtn_mute.setUnselectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/mixconsole/resources/MuteOff_Icon-21x21.png"))); // NOI18N
        fbtn_mute.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_muteActionPerformed(evt);
            }
        });
        pnl_mute.add(fbtn_mute);

        pnl_solo.setOpaque(false);
        pnl_solo.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                pnl_soloMouseDragged(evt);
            }
        });
        pnl_solo.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        fbtn_solo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        fbtn_solo.setAlignmentX(0.5F);
        fbtn_solo.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/mixconsole/resources/SoloDisabled_Icon-21x21.png"))); // NOI18N
        fbtn_solo.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/mixconsole/resources/SoloOn_Icon-21x21.png"))); // NOI18N
        fbtn_solo.setUnselectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/mixconsole/resources/SoloOff_Icon-21x21.png"))); // NOI18N
        fbtn_solo.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_soloActionPerformed(evt);
            }
        });
        pnl_solo.add(fbtn_solo);

        pnl_inst_volume.setOpaque(false);
        pnl_inst_volume.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        fslider_volume.setButtonColor(new java.awt.Color(208, 208, 208));
        fslider_volume.setButtonHeight(20);
        fslider_volume.setInheritsPopupMenu(true);
        fslider_volume.setTooltipLabel(org.openide.util.NbBundle.getBundle(MixChannelPanel.class).getString("MixChannelPanel.fslider_volume.tooltipLabel")); // NOI18N
        fslider_volume.setValueLineColor(knob_panoramic.getValueLineColor());
        pnl_inst_volume.add(fslider_volume);

        fbtn_Instrument.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_Instrument.toolTipText")); // NOI18N
        fbtn_Instrument.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        fbtn_Instrument.setAlignmentX(0.5F);
        fbtn_Instrument.setFont(FONT.deriveFont(Font.ITALIC, 11f)
        );
        fbtn_Instrument.setvLabel("Rock piano"); // NOI18N
        fbtn_Instrument.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_InstrumentActionPerformed(evt);
            }
        });
        pnl_inst_volume.add(fbtn_Instrument);

        pnl_icon.setOpaque(false);
        pnl_icon.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        lbl_Icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/mixconsole/resources/Drums-48x48.png"))); // NOI18N
        lbl_Icon.setAlignmentX(0.5F);
        lbl_Icon.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 0, 2, 0));
        lbl_Icon.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                lbl_IconMouseDragged(evt);
            }
        });
        pnl_icon.add(lbl_Icon);

        pnl_category.setOpaque(false);
        pnl_category.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        lbl_category.setFont(FONT.deriveFont(Font.BOLD)
        );
        lbl_category.setForeground(fbtn_Instrument.getForeground());
        org.openide.awt.Mnemonics.setLocalizedText(lbl_category, org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.lbl_category.text")); // NOI18N
        lbl_category.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                lbl_categoryMouseDragged(evt);
            }
        });
        pnl_category.add(lbl_category);

        pnl_name.setOpaque(false);
        pnl_name.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        fbtn_channelName.setForeground(fbtn_Instrument.getForeground());
        fbtn_channelName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_channelName, org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_channelName.text")); // NOI18N
        fbtn_channelName.setAlignmentX(0.5F);
        fbtn_channelName.setFont(FONT);
        fbtn_channelName.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                fbtn_channelNameMouseDragged(evt);
            }
        });
        pnl_name.add(fbtn_channelName);

        pnl_channelId.setOpaque(false);
        pnl_channelId.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        fbtn_channelId.setForeground(fbtn_Instrument.getForeground());
        fbtn_channelId.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_channelId, "12"); // NOI18N
        fbtn_channelId.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_channelId.toolTipText")); // NOI18N
        fbtn_channelId.setAlignmentX(0.5F);
        fbtn_channelId.setFont(FONT);
        fbtn_channelId.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        fbtn_channelId.setName(""); // NOI18N
        fbtn_channelId.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                fbtn_channelIdMouseDragged(evt);
            }
        });
        fbtn_channelId.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_channelIdActionPerformed(evt);
            }
        });
        pnl_channelId.add(fbtn_channelId);

        knob_panoramic.setOpaque(false);
        knob_panoramic.setPanoramicType(true);
        knob_panoramic.setValueLineGap(3.0);
        knob_panoramic.setValueLineThickness(2.0);

        javax.swing.GroupLayout knob_panoramicLayout = new javax.swing.GroupLayout(knob_panoramic);
        knob_panoramic.setLayout(knob_panoramicLayout);
        knob_panoramicLayout.setHorizontalGroup(
            knob_panoramicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        knob_panoramicLayout.setVerticalGroup(
            knob_panoramicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 31, Short.MAX_VALUE)
        );

        knob_chorus.setOpaque(false);
        knob_chorus.setValueLineGap(3.0);
        knob_chorus.setValueLineThickness(2.0);

        javax.swing.GroupLayout knob_chorusLayout = new javax.swing.GroupLayout(knob_chorus);
        knob_chorus.setLayout(knob_chorusLayout);
        knob_chorusLayout.setHorizontalGroup(
            knob_chorusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        knob_chorusLayout.setVerticalGroup(
            knob_chorusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 31, Short.MAX_VALUE)
        );

        knob_reverb.setKnobStartAngle(220.0);
        knob_reverb.setOpaque(false);
        knob_reverb.setValueLineGap(3.0);
        knob_reverb.setValueLineThickness(2.0);
        knob_reverb.setLayout(null);

        lbl_pan.setFont(lbl_pan.getFont().deriveFont(lbl_pan.getFont().getSize()-2f));
        lbl_pan.setForeground(fbtn_Instrument.getForeground());
        lbl_pan.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_pan, org.openide.util.NbBundle.getBundle(MixChannelPanel.class).getString("MixChannelPanel.lbl_pan.text")); // NOI18N

        lbl_rev.setFont(lbl_rev.getFont().deriveFont(lbl_rev.getFont().getSize()-2f));
        lbl_rev.setForeground(fbtn_Instrument.getForeground());
        lbl_rev.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_rev, org.openide.util.NbBundle.getBundle(MixChannelPanel.class).getString("MixChannelPanel.lbl_rev.text")); // NOI18N

        lbl_cho.setFont(lbl_cho.getFont().deriveFont(lbl_cho.getFont().getSize()-2f));
        lbl_cho.setForeground(fbtn_Instrument.getForeground());
        lbl_cho.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_cho, org.openide.util.NbBundle.getBundle(MixChannelPanel.class).getString("MixChannelPanel.lbl_cho.text")); // NOI18N

        javax.swing.GroupLayout roundedPanelLayout = new javax.swing.GroupLayout(roundedPanel);
        roundedPanel.setLayout(roundedPanelLayout);
        roundedPanelLayout.setHorizontalGroup(
            roundedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanelLayout.createSequentialGroup()
                .addComponent(fbtn_Settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(pnl_led_close, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(pnl_mute, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_icon, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_channelId, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(knob_panoramic, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_solo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(knob_reverb, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(knob_chorus, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_inst_volume, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lbl_pan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lbl_rev, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lbl_cho, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_name, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_category, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        roundedPanelLayout.setVerticalGroup(
            roundedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanelLayout.createSequentialGroup()
                .addGroup(roundedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_led_close, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fbtn_Settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addComponent(pnl_mute, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_solo, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(knob_panoramic, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(lbl_pan)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(knob_reverb, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(lbl_rev)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(knob_chorus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(lbl_cho)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_inst_volume, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addComponent(pnl_icon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_category, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(pnl_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_channelId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(roundedPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(roundedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void fbtn_SettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_SettingsActionPerformed
    {//GEN-HEADEREND:event_fbtn_SettingsActionPerformed
        controller.editSettings(model.getChannelId());
    }//GEN-LAST:event_fbtn_SettingsActionPerformed

    private void fbtn_muteActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_muteActionPerformed
    {//GEN-HEADEREND:event_fbtn_muteActionPerformed
        model.setMute(fbtn_mute.isSelected());
    }//GEN-LAST:event_fbtn_muteActionPerformed

    private void fbtn_soloActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_soloActionPerformed
    {//GEN-HEADEREND:event_fbtn_soloActionPerformed
        model.setSolo(fbtn_solo.isSelected());
    }//GEN-LAST:event_fbtn_soloActionPerformed

    private void fbtn_InstrumentActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_InstrumentActionPerformed
    {//GEN-HEADEREND:event_fbtn_InstrumentActionPerformed
        boolean ctrl = (evt.getModifiers() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        boolean shift = (evt.getModifiers() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
        if (ctrl)
        {
            controller.editPreviousInstrument(model.getChannelId());
        } else if (shift)
        {
            controller.editNextInstrument(model.getChannelId());
        } else
        {
            controller.editInstrument(model.getChannelId());
        }
    }//GEN-LAST:event_fbtn_InstrumentActionPerformed

    private void fbtn_channelIdActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_channelIdActionPerformed
    {//GEN-HEADEREND:event_fbtn_channelIdActionPerformed
        if (model.isDrumsReroutingEnabled())
        {
            return;
        }
        String strNewValue = editValueWithOverlayDialog(fbtn_channelId, 2);
        if (strNewValue != null)
        {
            controller.editChannelId(model.getChannelId(), strNewValue);
        }
    }//GEN-LAST:event_fbtn_channelIdActionPerformed

    private void formMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_formMouseDragged
    {//GEN-HEADEREND:event_formMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_formMouseDragged

    private void pnl_muteMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_pnl_muteMouseDragged
    {//GEN-HEADEREND:event_pnl_muteMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_pnl_muteMouseDragged

    private void pnl_soloMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_pnl_soloMouseDragged
    {//GEN-HEADEREND:event_pnl_soloMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_pnl_soloMouseDragged

    private void lbl_IconMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_IconMouseDragged
    {//GEN-HEADEREND:event_lbl_IconMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_lbl_IconMouseDragged

    private void fbtn_channelIdMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_fbtn_channelIdMouseDragged
    {//GEN-HEADEREND:event_fbtn_channelIdMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_fbtn_channelIdMouseDragged

    private void fbtn_channelNameMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_fbtn_channelNameMouseDragged
    {//GEN-HEADEREND:event_fbtn_channelNameMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_fbtn_channelNameMouseDragged

    private void lbl_categoryMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_categoryMouseDragged
    {//GEN-HEADEREND:event_lbl_categoryMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_lbl_categoryMouseDragged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.mixconsole.VInstrumentButton fbtn_Instrument;
    private org.jjazz.flatcomponents.api.FlatButton fbtn_Settings;
    private org.jjazz.flatcomponents.api.FlatButton fbtn_channelId;
    private org.jjazz.flatcomponents.api.FlatButton fbtn_channelName;
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_mute;
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_solo;
    private javax.swing.Box.Filler filler1;
    private org.jjazz.flatcomponents.api.FlatLedIndicator fled_midiActivity;
    private org.jjazz.flatcomponents.api.FlatIntegerVerticalSlider fslider_volume;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_chorus;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_panoramic;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knob_reverb;
    private javax.swing.JLabel lbl_Icon;
    private javax.swing.JLabel lbl_category;
    private javax.swing.JLabel lbl_cho;
    private javax.swing.JLabel lbl_pan;
    private javax.swing.JLabel lbl_rev;
    private javax.swing.JPanel pnl_category;
    private javax.swing.JPanel pnl_channelId;
    private javax.swing.JPanel pnl_icon;
    private javax.swing.JPanel pnl_inst_volume;
    private javax.swing.JPanel pnl_led_close;
    private javax.swing.JPanel pnl_mute;
    private javax.swing.JPanel pnl_name;
    private javax.swing.JPanel pnl_solo;
    private org.jjazz.flatcomponents.api.RoundedPanel roundedPanel;
    // End of variables declaration//GEN-END:variables

    private class BaseMixChannelPanelModel implements MixChannelPanelModel
    {

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l)
        {
            // Nothing
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l)
        {
            // Nothing
        }

        @Override
        public void setPanoramicEnabled(boolean b)
        {
            // Nothing
        }

        @Override
        public boolean isPanoramicEnabled()
        {
            return true;
        }

        @Override
        public void setChorusEnabled(boolean b)
        {
            // Nothing
        }

        @Override
        public boolean isChorusEnabled()
        {
            return true;
        }

        @Override
        public void setReverbEnabled(boolean b)
        {
            // Nothing
        }

        @Override
        public boolean isReverbEnabled()
        {
            return true;
        }

        @Override
        public void setReverb(int value)
        {
            // Nothing
        }

        @Override
        public int getReverb()
        {
            return 100;
        }

        @Override
        public void setChorus(int value)
        {
            // Nothing
        }

        @Override
        public int getChorus()
        {
            return 100;
        }

        @Override
        public void setPanoramic(int value)
        {
            // Nothing
        }

        @Override
        public int getPanoramic()
        {
            return 100;
        }

        @Override
        public void setVolume(int oldValue, int newValue, MouseEvent me)
        {
            // Nothing
        }

        @Override
        public int getVolume()
        {
            return 100;
        }

        @Override
        public void setMute(boolean b)
        {
            // Nothing
        }

        @Override
        public boolean isMute()
        {
            return false;
        }

        @Override
        public void setSolo(boolean b)
        {
            // Nothing
        }

        @Override
        public boolean isSolo()
        {
            return false;
        }

        @Override
        public Instrument getInstrument()
        {
            return GMSynth.getInstance().getGM1Bank().getInstruments().get(0);
        }

        @Override
        public int getChannelId()
        {
            return MidiConst.CHANNEL_MIN;
        }

        @Override
        public void cleanup()
        {
            // Nothing
        }

        @Override
        public void setVolumeEnabled(boolean b)
        {
            // Nothing
        }

        @Override
        public boolean isVolumeEnabled()
        {
            return true;
        }

        @Override
        public void setInstrumentEnabled(boolean b)
        {
            // Nothing
        }

        @Override
        public boolean isInstrumentEnabled()
        {
            return true;
        }

        @Override
        public boolean isDrumsReroutingEnabled()
        {
            return false;
        }

        @Override
        public String getChannelName()
        {
            return "upper";
        }

        @Override
        public String getChannelNameTooltip()
        {
            return "";
        }

        @Override
        public String getIconTooltip()
        {
            return "";
        }

        @Override
        public ImageIcon getIcon()
        {
            return null;
        }

        @Override
        public Color getChannelColor()
        {
            return Color.CYAN;
        }

        @Override
        public String getCategory()
        {
            return "category";
        }

        @Override
        public boolean isUserChannel()
        {
            return false;
        }

        @Override
        public void setChannelColor(Color c)
        {
            // Nothing
        }
    }

}
