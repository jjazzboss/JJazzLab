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
package org.jjazz.ui.mixconsole;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JTextField;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.StdSynth;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.MidiConst;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.musiccontrol.PlaybackListener;
import org.jjazz.ui.flatcomponents.FlatButton;
import org.jjazz.ui.flatcomponents.FlatIntegerKnob;
import org.jjazz.ui.flatcomponents.FlatIntegerVerticalSlider;
import org.jjazz.ui.flatcomponents.FlatTextEditDialog;

/**
 * Display a MixChannel.
 */
public class MixChannelPanel extends javax.swing.JPanel implements PropertyChangeListener, PlaybackListener
{

    private static FlatTextEditDialog TEXT_EDIT_DIALOG;
    private MixChannelPanelModel model;
    private MixChannelPanelController controller;
    private Color channelColor;
    private boolean selected;
    private static final Logger LOGGER = Logger.getLogger(MixChannelPanel.class.getSimpleName());

    /**
     * Creates a new form InstrumentChannel with a basic model.
     * 
     */
    public MixChannelPanel()
    {
        model = new BaseMixChannelPanelModel();
    }

    public MixChannelPanel(final MixChannelPanelModel model, final MixChannelPanelController controller)
    {
        if (model == null || controller == null)
        {
            throw new IllegalArgumentException("model=" + model + " controller=" + controller);
        }
        this.model = model;
        this.controller = controller;
        this.model.addPropertyChangeListener(this);

        MusicController.getInstance().addPlaybackListener(this);

        initComponents();
        
        this.fbtn_mute.setEnabled(!model.isUserChannel());
        this.fbtn_solo.setEnabled(!model.isUserChannel());        
        this.lbl_Icon.setText(null);

        // Listen to UI changes
        this.fslider_volume.addPropertyChangeListener(this);
        this.knob_chorus.addPropertyChangeListener(this);
        this.knob_panoramic.addPropertyChangeListener(this);
        this.knob_reverb.addPropertyChangeListener(this);

        refreshUI();
    }

    public MixChannelPanelModel getModel()
    {
        return model;
    }

    public void cleanup()
    {
        MusicController.getInstance().removePlaybackListener(this);
        model.removePropertyChangeListener(this);
        model.cleanup();
        model = null;
    }

    public void setChannelColor(Color c)
    {
        channelColor = c;
        lbl_name.setForeground(c);
        fbtn_channelId.setForeground(c);
    }

    public Color getChannelColor()
    {
        return channelColor;
    }

    public void setIcon(Icon icon)
    {
        this.lbl_Icon.setIcon(icon);
    }

    public void setIconToolTipText(String text)
    {
        this.lbl_Icon.setToolTipText(text);
    }

    /**
     *
     * @param upperName
     * @param lowerName Can be null if not used
     */
    public void setChannelName(String upperName, String lowerName)
    {
        if (upperName == null)
        {
            throw new NullPointerException("upperName");
        }
        String tt = (lowerName != null) ? upperName + " /" + lowerName : upperName;
        lbl_name.setToolTipText(tt);
        if (upperName.length() > 10)
        {
            upperName = upperName.substring(0, 6).trim() + "...";
        }
        if (lowerName != null && lowerName.length() > 10)
        {
            lowerName = lowerName.substring(0, 6).trim() + "...";
        }
        String s = (lowerName != null) ? "<html><div style='text-align: center;'>" + upperName + "<br><font size='-1'>" + lowerName + "</div></html>" : upperName;
        lbl_name.setText(s);
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

    // ======================================================================
    // Playbackistener interface
    // ======================================================================  
    @Override
    public void beatChanged(Position oldPos, Position newPos)
    {
        // Nothing
    }

    @Override
    public void barChanged(int oldBar, int newBar)
    {
        // Nothing
    }

    @Override
    public void midiActivity(int channel, long tick)
    {
        if (model.getChannelId() == channel)
        {
            fled_midiActivity.showActivity();
        }
    }

    // ----------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // UI changed, update model
        if (evt.getSource() == this.knob_chorus && evt.getPropertyName() == FlatIntegerKnob.PROP_VALUE)
        {
            model.setChorus(knob_chorus.getValue());
        } else if (evt.getSource() == this.knob_reverb && evt.getPropertyName() == FlatIntegerKnob.PROP_VALUE)
        {
            model.setReverb(knob_reverb.getValue());
        } else if (evt.getSource() == this.knob_panoramic && evt.getPropertyName() == FlatIntegerKnob.PROP_VALUE)
        {
            model.setPanoramic(knob_panoramic.getValue());
        } else if (evt.getSource() == this.fslider_volume && evt.getPropertyName() == FlatIntegerVerticalSlider.PROP_VALUE)
        {
            int oldValue = (int) evt.getOldValue();
            int newValue = (int) evt.getNewValue();
            MouseEvent me = fslider_volume.getLastMouseEvent();
            model.setVolume(oldValue, newValue, me);
        } else if (evt.getSource() == model)
        {
            // MODEL changed, update UI
            refreshUI();
        }
    }

// ----------------------------------------------------------------------------
// Private functions 
// ----------------------------------------------------------------------------
    /**
     * Model has changed, update UI.
     */
    private void refreshUI()
    {
        String s = String.valueOf(model.getChannelId() + 1);
        String tt = "Midi Channel";
        if (model.isDrumsReroutingEnabled())
        {
            s += ">10";
            tt += " rerouted to Drums channel";
        }
        this.fbtn_channelId.setText(s);
        this.fbtn_channelId.setToolTipText(tt);
        this.fbtn_mute.setSelected(model.isMute());
        this.fbtn_solo.setSelected(model.isSolo());
        Instrument ins = model.getInstrument();
        String patchName = ins.getPatchName();
        String longName = patchName + " [synth=" + ins.getBank().getMidiSynth().getName() + ", bank=" + ins.getBank().getName() + "]";
        this.fbtn_Instrument.setToolTipText(longName);
        if (patchName.length() > 15)
        {
            patchName = patchName.substring(0, 15).trim() + "...";
        }
        this.fbtn_Instrument.setvLabel(patchName);
        this.fslider_volume.setValue(model.getVolume());
        this.knob_chorus.setValue(model.getChorus());
        this.knob_panoramic.setValue(model.getPanoramic());
        this.knob_reverb.setValue(model.getReverb());
        knob_panoramic.setEnabled(model.isPanoramicEnabled());
        knob_reverb.setEnabled(model.isReverbEnabled());
        knob_chorus.setEnabled(model.isChorusEnabled());
        fslider_volume.setEnabled(model.isVolumeEnabled());
        fbtn_Instrument.setEnabled(model.isInstrumentEnabled());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        roundedPanel = new org.jjazz.ui.flatcomponents.RoundedPanel();
        pnl_led_close = new javax.swing.JPanel();
        fled_midiActivity = new org.jjazz.ui.flatcomponents.FlatLedIndicator();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(2, 0), new java.awt.Dimension(2, 0), new java.awt.Dimension(3, 32767));
        fbtn_Settings = new org.jjazz.ui.flatcomponents.FlatButton();
        pnl_mute = new javax.swing.JPanel();
        fbtn_mute = new org.jjazz.ui.flatcomponents.FlatToggleButton();
        pnl_solo = new javax.swing.JPanel();
        fbtn_solo = new org.jjazz.ui.flatcomponents.FlatToggleButton();
        pnl_pan = new javax.swing.JPanel();
        knob_panoramic = new org.jjazz.ui.mixconsole.PanoramicKnob();
        pnl_rev = new javax.swing.JPanel();
        knob_reverb = new org.jjazz.ui.flatcomponents.FlatIntegerKnob();
        pnl_cho = new javax.swing.JPanel();
        knob_chorus = new org.jjazz.ui.flatcomponents.FlatIntegerKnob();
        pnl_inst_volume = new javax.swing.JPanel();
        fslider_volume = new org.jjazz.ui.flatcomponents.FlatIntegerVerticalSlider();
        fbtn_Instrument = new org.jjazz.ui.mixconsole.VInstrumentButton();
        pnl_icon = new javax.swing.JPanel();
        lbl_Icon = new javax.swing.JLabel();
        pnl_name = new javax.swing.JPanel();
        lbl_name = new javax.swing.JLabel();
        pnl_channelId = new javax.swing.JPanel();
        fbtn_channelId = new org.jjazz.ui.flatcomponents.FlatButton();

        setBackground(null);
        setMinimumSize(new java.awt.Dimension(10, 53));
        setOpaque(false);

        roundedPanel.setBackground(new java.awt.Color(237, 237, 237));
        roundedPanel.setForeground(new java.awt.Color(255, 153, 153));
        roundedPanel.setArcDiameter(20);
        roundedPanel.setInheritsPopupMenu(true);
        roundedPanel.setMinimumSize(new java.awt.Dimension(20, 53));
        roundedPanel.setThickness(1);

        pnl_led_close.setOpaque(false);
        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0);
        flowLayout1.setAlignOnBaseline(true);
        pnl_led_close.setLayout(flowLayout1);

        fled_midiActivity.setForeground(new java.awt.Color(153, 153, 153));
        fled_midiActivity.setColorFill(new java.awt.Color(204, 51, 0));
        fled_midiActivity.setDiameter(8);
        fled_midiActivity.setLuminanceStepEventReceived(-35);
        pnl_led_close.add(fled_midiActivity);
        pnl_led_close.add(filler1);

        fbtn_Settings.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        fbtn_Settings.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/Settings11x11.png"))); // NOI18N
        fbtn_Settings.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_Settings.toolTipText")); // NOI18N
        fbtn_Settings.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                fbtn_SettingsStateChanged(evt);
            }
        });

        pnl_mute.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        fbtn_mute.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/MuteOff_Icon-21x21.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_mute, org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_mute.text")); // NOI18N
        fbtn_mute.setAlignmentX(0.5F);
        fbtn_mute.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/MuteDisabled_Icon-21x21.png"))); // NOI18N
        fbtn_mute.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/MuteOn_Icon-21x21.png"))); // NOI18N
        fbtn_mute.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                fbtn_muteStateChanged(evt);
            }
        });
        pnl_mute.add(fbtn_mute);

        pnl_solo.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        fbtn_solo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        fbtn_solo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/SoloOff_Icon-21x21.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_solo, org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_solo.text")); // NOI18N
        fbtn_solo.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_solo.toolTipText")); // NOI18N
        fbtn_solo.setAlignmentX(0.5F);
        fbtn_solo.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/SoloDisabled_Icon-21x21.png"))); // NOI18N
        fbtn_solo.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/SoloOn_Icon-21x21.png"))); // NOI18N
        fbtn_solo.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                fbtn_soloStateChanged(evt);
            }
        });
        pnl_solo.add(fbtn_solo);

        pnl_pan.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        knob_panoramic.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.knob_panoramic.toolTipText")); // NOI18N
        knob_panoramic.setInheritsPopupMenu(true);
        knob_panoramic.setKnobDiameter(20);
        knob_panoramic.setLabel(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.knob_panoramic.label")); // NOI18N
        pnl_pan.add(knob_panoramic);

        pnl_rev.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        knob_reverb.setInheritsPopupMenu(true);
        knob_reverb.setKnobDiameter(20);
        knob_reverb.setLabel(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.knob_reverb.label")); // NOI18N
        pnl_rev.add(knob_reverb);

        pnl_cho.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        knob_chorus.setInheritsPopupMenu(true);
        knob_chorus.setKnobDiameter(20);
        knob_chorus.setLabel(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.knob_chorus.label")); // NOI18N
        pnl_cho.add(knob_chorus);

        pnl_inst_volume.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 3, 0));

        fslider_volume.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fslider_volume.toolTipText")); // NOI18N
        fslider_volume.setFaderWidth(6);
        fslider_volume.setInheritsPopupMenu(true);
        fslider_volume.setKnobDiameter(14);
        pnl_inst_volume.add(fslider_volume);

        fbtn_Instrument.setForeground(new java.awt.Color(0, 102, 102));
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_Instrument, org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_Instrument.text")); // NOI18N
        fbtn_Instrument.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_Instrument.toolTipText")); // NOI18N
        fbtn_Instrument.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        fbtn_Instrument.setAlignmentX(0.5F);
        fbtn_Instrument.setFont(new java.awt.Font("Arial", 2, 10)); // NOI18N
        fbtn_Instrument.setvLabel(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_Instrument.vLabel")); // NOI18N
        fbtn_Instrument.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                fbtn_InstrumentStateChanged(evt);
            }
        });
        pnl_inst_volume.add(fbtn_Instrument);

        pnl_icon.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        lbl_Icon.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        lbl_Icon.setForeground(new java.awt.Color(0, 102, 102));
        lbl_Icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/Drums-48x48.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Icon, org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.lbl_Icon.text")); // NOI18N
        lbl_Icon.setAlignmentX(0.5F);
        lbl_Icon.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 0, 2, 0));
        pnl_icon.add(lbl_Icon);

        pnl_name.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        lbl_name.setBackground(new java.awt.Color(153, 255, 153));
        lbl_name.setFont(new java.awt.Font("Arial Narrow", 0, 12)); // NOI18N
        lbl_name.setForeground(new java.awt.Color(0, 102, 102));
        lbl_name.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_name, org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.lbl_name.text")); // NOI18N
        lbl_name.setAlignmentX(0.5F);
        lbl_name.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 0, 4, 0));
        pnl_name.add(lbl_name);

        pnl_channelId.setOpaque(false);
        pnl_channelId.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        fbtn_channelId.setForeground(new java.awt.Color(0, 70, 70));
        fbtn_channelId.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_channelId, org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.text")); // NOI18N
        fbtn_channelId.setToolTipText(org.openide.util.NbBundle.getMessage(MixChannelPanel.class, "MixChannelPanel.fbtn_channelId.toolTipText")); // NOI18N
        fbtn_channelId.setAlignmentX(0.5F);
        fbtn_channelId.setFont(new java.awt.Font("Arial", 0, 9)); // NOI18N
        fbtn_channelId.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        fbtn_channelId.setName(""); // NOI18N
        fbtn_channelId.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                fbtn_channelIdStateChanged(evt);
            }
        });
        pnl_channelId.add(fbtn_channelId);

        javax.swing.GroupLayout roundedPanelLayout = new javax.swing.GroupLayout(roundedPanel);
        roundedPanel.setLayout(roundedPanelLayout);
        roundedPanelLayout.setHorizontalGroup(
            roundedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, roundedPanelLayout.createSequentialGroup()
                .addComponent(fbtn_Settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(pnl_led_close, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(pnl_mute, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_solo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_pan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_rev, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_cho, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_inst_volume, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_name, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_icon, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnl_channelId, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        roundedPanelLayout.setVerticalGroup(
            roundedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanelLayout.createSequentialGroup()
                .addGroup(roundedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_led_close, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fbtn_Settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_mute, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pnl_solo, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addComponent(pnl_pan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pnl_rev, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pnl_cho, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(pnl_inst_volume, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addComponent(pnl_icon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pnl_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pnl_channelId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(roundedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(roundedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void fbtn_channelIdStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_fbtn_channelIdStateChanged
    {//GEN-HEADEREND:event_fbtn_channelIdStateChanged
        if (model.isDrumsReroutingEnabled())
        {
            return;
        }
        if (TEXT_EDIT_DIALOG == null)
        {
            TEXT_EDIT_DIALOG = FlatTextEditDialog.getInstance();
            TEXT_EDIT_DIALOG.setForeground(fbtn_channelId.getForeground());
            TEXT_EDIT_DIALOG.setBackground(Color.WHITE);
            TEXT_EDIT_DIALOG.setFont(fbtn_channelId.getFont());
            TEXT_EDIT_DIALOG.setHorizontalAlignment(JTextField.CENTER);
            TEXT_EDIT_DIALOG.setColumns(2);
        }
        String oldValue = this.fbtn_channelId.getText();
        TEXT_EDIT_DIALOG.setText(oldValue);
        TEXT_EDIT_DIALOG.pack();
        TEXT_EDIT_DIALOG.setPositionCenter(fbtn_channelId);
        TEXT_EDIT_DIALOG.setVisible(true);
        String newValue = TEXT_EDIT_DIALOG.getText().trim();
        if (TEXT_EDIT_DIALOG.isExitOk() && newValue.length() > 0 && !newValue.equals(oldValue))
        {
            controller.editChannelId(newValue);
        }
    }//GEN-LAST:event_fbtn_channelIdStateChanged

    private void fbtn_InstrumentStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_fbtn_InstrumentStateChanged
    {//GEN-HEADEREND:event_fbtn_InstrumentStateChanged
        FlatButton.ExtendedChangeEvent e = (FlatButton.ExtendedChangeEvent) evt;
        boolean ctrl = (e.mouseEvent.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        boolean shift = (e.mouseEvent.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
        if (ctrl)
        {
            controller.editPreviousInstrument();
        } else if (shift)
        {
            controller.editNextInstrument();
        } else
        {
            controller.editInstrument();
        }

    }//GEN-LAST:event_fbtn_InstrumentStateChanged

    private void fbtn_SettingsStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_fbtn_SettingsStateChanged
    {//GEN-HEADEREND:event_fbtn_SettingsStateChanged
        controller.editSettings();
    }//GEN-LAST:event_fbtn_SettingsStateChanged

   private void fbtn_muteStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_fbtn_muteStateChanged
   {//GEN-HEADEREND:event_fbtn_muteStateChanged
       model.setMute(fbtn_mute.isSelected());
   }//GEN-LAST:event_fbtn_muteStateChanged

   private void fbtn_soloStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_fbtn_soloStateChanged
   {//GEN-HEADEREND:event_fbtn_soloStateChanged
       model.setSolo(fbtn_solo.isSelected());
   }//GEN-LAST:event_fbtn_soloStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.ui.mixconsole.VInstrumentButton fbtn_Instrument;
    private org.jjazz.ui.flatcomponents.FlatButton fbtn_Settings;
    private org.jjazz.ui.flatcomponents.FlatButton fbtn_channelId;
    private org.jjazz.ui.flatcomponents.FlatToggleButton fbtn_mute;
    private org.jjazz.ui.flatcomponents.FlatToggleButton fbtn_solo;
    private javax.swing.Box.Filler filler1;
    private org.jjazz.ui.flatcomponents.FlatLedIndicator fled_midiActivity;
    private org.jjazz.ui.flatcomponents.FlatIntegerVerticalSlider fslider_volume;
    private org.jjazz.ui.flatcomponents.FlatIntegerKnob knob_chorus;
    private org.jjazz.ui.mixconsole.PanoramicKnob knob_panoramic;
    private org.jjazz.ui.flatcomponents.FlatIntegerKnob knob_reverb;
    private javax.swing.JLabel lbl_Icon;
    private javax.swing.JLabel lbl_name;
    private javax.swing.JPanel pnl_channelId;
    private javax.swing.JPanel pnl_cho;
    private javax.swing.JPanel pnl_icon;
    private javax.swing.JPanel pnl_inst_volume;
    private javax.swing.JPanel pnl_led_close;
    private javax.swing.JPanel pnl_mute;
    private javax.swing.JPanel pnl_name;
    private javax.swing.JPanel pnl_pan;
    private javax.swing.JPanel pnl_rev;
    private javax.swing.JPanel pnl_solo;
    private org.jjazz.ui.flatcomponents.RoundedPanel roundedPanel;
    // End of variables declaration//GEN-END:variables

}

class BaseMixChannelPanelModel implements MixChannelPanelModel
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
        return StdSynth.getInstance().getGM1Bank().getInstruments().get(0);
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
    public boolean isUserChannel()
    {
        return false;
    }
}
