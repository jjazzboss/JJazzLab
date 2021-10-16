package org.jjazz.rpcustomeditorfactoryimpl;

import org.jjazz.rpcustomeditorfactoryimpl.spi.RealTimeRpEditorComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_DrumsMix;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_DrumsMixValue;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.ui.flatcomponents.api.FlatIntegerKnob;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;


/**
 * An editor component for RP_SYS_DrumsMix.
 */
public class RP_SYS_DrumsMixComp extends RealTimeRpEditorComponent<RP_SYS_DrumsMixValue> implements PropertyChangeListener
{

    private RP_SYS_DrumsMix rp;
    private RP_SYS_DrumsMixValue lastValue;
    private SongPartContext songPartContext;

    public RP_SYS_DrumsMixComp(RP_SYS_DrumsMix rp)
    {
        this.rp = rp;
        initComponents();
        knb_bassDrum.addPropertyChangeListener(this);
        knb_snare.addPropertyChangeListener(this);
        knb_hihat.addPropertyChangeListener(this);
        knb_toms.addPropertyChangeListener(this);
        knb_crash.addPropertyChangeListener(this);
        knb_cymbals.addPropertyChangeListener(this);
        knb_perc.addPropertyChangeListener(this);
    }

    @Override
    public RP_SYS_DrumsMix getRhythmParameter()
    {
        return rp;
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        knb_bassDrum.setEnabled(b);
        knb_snare.setEnabled(b);
        knb_hihat.setEnabled(b);
        knb_toms.setEnabled(b);
        knb_crash.setEnabled(b);
        knb_cymbals.setEnabled(b);
        knb_perc.setEnabled(b);
    }

    @Override
    public void preset(RP_SYS_DrumsMixValue rpValue, SongPartContext sgContext)
    {
        setEditedRpValue(rpValue);
        songPartContext = sgContext;


        String strChannel = "Channel drums";
        if (songPartContext != null)
        {
            // Check muted drums
            var rvDrums = rp.getRhythmVoice();
            String msg = null;
            if (songPartContext.getMidiMix().getInstrumentMixFromKey(rvDrums).isMute())
            {
                msg = ResUtil.getString(getClass(), "ERR_DrumsTrackIsMuted");
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }


            // Update channel label
            int channel = sgContext.getMidiMix().getChannel(rvDrums);
            strChannel = "Channel " + (channel + 1);
            if (!rvDrums.getName().trim().equalsIgnoreCase("drums"))
            {
                strChannel += " (" + rvDrums.getName().trim() + ")";
            }
        }

        lbl_channel.setText(strChannel);
    }

    @Override
    public void setEditedRpValue(RP_SYS_DrumsMixValue rpValue)
    {
        knb_bassDrum.setValue(rpValue.getBassDrumOffset());
        knb_snare.setValue(rpValue.getSnareOffset());
        knb_hihat.setValue(rpValue.getHiHatOffset());
        knb_toms.setValue(rpValue.getTomsOffset());
        knb_crash.setValue(rpValue.getCrashOffset());
        knb_cymbals.setValue(rpValue.getCymbalsOffset());
        knb_perc.setValue(rpValue.getPercOffset());
        lastValue = rpValue;
    }

    @Override
    public RP_SYS_DrumsMixValue getEditedRpValue()
    {
        RP_SYS_DrumsMixValue res = new RP_SYS_DrumsMixValue(knb_bassDrum.getValue(),
                knb_snare.getValue(),
                knb_hihat.getValue(),
                knb_toms.getValue(),
                knb_crash.getValue(),
                knb_cymbals.getValue(),
                knb_perc.getValue()
        );
        return res;
    }

    @Override
    public void cleanup()
    {
    }

    @Override
    public String getTitle()
    {
        return null;
    }

    // ===================================================================================
    // PropertyChangeListener interface
    // ===================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == knb_bassDrum
                || evt.getSource() == knb_snare
                || evt.getSource() == knb_hihat
                || evt.getSource() == knb_toms
                || evt.getSource() == knb_crash
                || evt.getSource() == knb_cymbals
                || evt.getSource() == knb_perc)
        {
            if (evt.getPropertyName().equals(FlatIntegerKnob.PROP_VALUE))
            {
                var newValue = getEditedRpValue();
                firePropertyChange(PROP_EDITED_RP_VALUE, lastValue, newValue);
                lastValue = newValue;
            }
        }
    }

    // ===================================================================================
    // Private methods
    // ===================================================================================

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        lbl_channel = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        knb_bassDrum = new org.jjazz.ui.flatcomponents.api.FlatIntegerKnob();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        knb_hihat = new org.jjazz.ui.flatcomponents.api.FlatIntegerKnob();
        jLabel8 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        knb_snare = new org.jjazz.ui.flatcomponents.api.FlatIntegerKnob();
        jLabel7 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        knb_toms = new org.jjazz.ui.flatcomponents.api.FlatIntegerKnob();
        jLabel9 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        knb_cymbals = new org.jjazz.ui.flatcomponents.api.FlatIntegerKnob();
        jLabel10 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        knb_crash = new org.jjazz.ui.flatcomponents.api.FlatIntegerKnob();
        jLabel11 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        knb_perc = new org.jjazz.ui.flatcomponents.api.FlatIntegerKnob();
        jLabel12 = new javax.swing.JLabel();

        setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_DrumsMixComp.class, "RP_SYS_DrumsMixComp.toolTipText")); // NOI18N

        lbl_channel.setFont(lbl_channel.getFont().deriveFont(lbl_channel.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_channel, "Channel 2"); // NOI18N

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        knb_bassDrum.setKnobStartAngle(220.0);
        knb_bassDrum.setMaxValue(64);
        knb_bassDrum.setMinValue(-64);
        knb_bassDrum.setPanoramicType(true);
        knb_bassDrum.setValue(0);
        knb_bassDrum.setValueLineThickness(3.0);

        javax.swing.GroupLayout knb_bassDrumLayout = new javax.swing.GroupLayout(knb_bassDrum);
        knb_bassDrum.setLayout(knb_bassDrumLayout);
        knb_bassDrumLayout.setHorizontalGroup(
            knb_bassDrumLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );
        knb_bassDrumLayout.setVerticalGroup(
            knb_bassDrumLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 33, Short.MAX_VALUE)
        );

        jPanel1.add(knb_bassDrum);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsMixComp.class, "RP_SYS_DrumsMixComp.jLabel1.text")); // NOI18N
        jLabel1.setAlignmentX(0.5F);
        jPanel1.add(jLabel1);

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));

        knb_hihat.setKnobStartAngle(220.0);
        knb_hihat.setMaxValue(64);
        knb_hihat.setMinValue(-64);
        knb_hihat.setPanoramicType(true);
        knb_hihat.setValue(0);
        knb_hihat.setValueLineThickness(3.0);

        javax.swing.GroupLayout knb_hihatLayout = new javax.swing.GroupLayout(knb_hihat);
        knb_hihat.setLayout(knb_hihatLayout);
        knb_hihatLayout.setHorizontalGroup(
            knb_hihatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );
        knb_hihatLayout.setVerticalGroup(
            knb_hihatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 33, Short.MAX_VALUE)
        );

        jPanel3.add(knb_hihat);

        jLabel8.setFont(jLabel8.getFont().deriveFont(jLabel8.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsMixComp.class, "RP_SYS_DrumsMixComp.jLabel8.text")); // NOI18N
        jLabel8.setAlignmentX(0.5F);
        jPanel3.add(jLabel8);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        knb_snare.setKnobStartAngle(220.0);
        knb_snare.setMaxValue(64);
        knb_snare.setMinValue(-64);
        knb_snare.setPanoramicType(true);
        knb_snare.setValue(0);
        knb_snare.setValueLineThickness(3.0);

        javax.swing.GroupLayout knb_snareLayout = new javax.swing.GroupLayout(knb_snare);
        knb_snare.setLayout(knb_snareLayout);
        knb_snareLayout.setHorizontalGroup(
            knb_snareLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );
        knb_snareLayout.setVerticalGroup(
            knb_snareLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 33, Short.MAX_VALUE)
        );

        jPanel2.add(knb_snare);

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsMixComp.class, "RP_SYS_DrumsMixComp.jLabel7.text")); // NOI18N
        jLabel7.setAlignmentX(0.5F);
        jPanel2.add(jLabel7);

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

        knb_toms.setKnobStartAngle(220.0);
        knb_toms.setMaxValue(64);
        knb_toms.setMinValue(-64);
        knb_toms.setPanoramicType(true);
        knb_toms.setValue(0);
        knb_toms.setValueLineThickness(3.0);

        javax.swing.GroupLayout knb_tomsLayout = new javax.swing.GroupLayout(knb_toms);
        knb_toms.setLayout(knb_tomsLayout);
        knb_tomsLayout.setHorizontalGroup(
            knb_tomsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );
        knb_tomsLayout.setVerticalGroup(
            knb_tomsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 33, Short.MAX_VALUE)
        );

        jPanel4.add(knb_toms);

        jLabel9.setFont(jLabel9.getFont().deriveFont(jLabel9.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel9, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsMixComp.class, "RP_SYS_DrumsMixComp.jLabel9.text")); // NOI18N
        jLabel9.setAlignmentX(0.5F);
        jPanel4.add(jLabel9);

        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.Y_AXIS));

        knb_cymbals.setKnobStartAngle(220.0);
        knb_cymbals.setMaxValue(64);
        knb_cymbals.setMinValue(-64);
        knb_cymbals.setPanoramicType(true);
        knb_cymbals.setValue(0);
        knb_cymbals.setValueLineThickness(3.0);

        javax.swing.GroupLayout knb_cymbalsLayout = new javax.swing.GroupLayout(knb_cymbals);
        knb_cymbals.setLayout(knb_cymbalsLayout);
        knb_cymbalsLayout.setHorizontalGroup(
            knb_cymbalsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );
        knb_cymbalsLayout.setVerticalGroup(
            knb_cymbalsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 33, Short.MAX_VALUE)
        );

        jPanel5.add(knb_cymbals);

        jLabel10.setFont(jLabel10.getFont().deriveFont(jLabel10.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel10, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsMixComp.class, "RP_SYS_DrumsMixComp.jLabel10.text")); // NOI18N
        jLabel10.setAlignmentX(0.5F);
        jPanel5.add(jLabel10);

        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.Y_AXIS));

        knb_crash.setKnobStartAngle(220.0);
        knb_crash.setMaxValue(64);
        knb_crash.setMinValue(-64);
        knb_crash.setPanoramicType(true);
        knb_crash.setValue(0);
        knb_crash.setValueLineThickness(3.0);

        javax.swing.GroupLayout knb_crashLayout = new javax.swing.GroupLayout(knb_crash);
        knb_crash.setLayout(knb_crashLayout);
        knb_crashLayout.setHorizontalGroup(
            knb_crashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );
        knb_crashLayout.setVerticalGroup(
            knb_crashLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 33, Short.MAX_VALUE)
        );

        jPanel6.add(knb_crash);

        jLabel11.setFont(jLabel11.getFont().deriveFont(jLabel11.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel11, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsMixComp.class, "RP_SYS_DrumsMixComp.jLabel11.text")); // NOI18N
        jLabel11.setAlignmentX(0.5F);
        jPanel6.add(jLabel11);

        jPanel7.setLayout(new javax.swing.BoxLayout(jPanel7, javax.swing.BoxLayout.Y_AXIS));

        knb_perc.setKnobStartAngle(220.0);
        knb_perc.setMaxValue(64);
        knb_perc.setMinValue(-64);
        knb_perc.setPanoramicType(true);
        knb_perc.setValue(0);
        knb_perc.setValueLineThickness(3.0);

        javax.swing.GroupLayout knb_percLayout = new javax.swing.GroupLayout(knb_perc);
        knb_perc.setLayout(knb_percLayout);
        knb_percLayout.setHorizontalGroup(
            knb_percLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );
        knb_percLayout.setVerticalGroup(
            knb_percLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 33, Short.MAX_VALUE)
        );

        jPanel7.add(knb_perc);

        jLabel12.setFont(jLabel12.getFont().deriveFont(jLabel12.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel12, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsMixComp.class, "RP_SYS_DrumsMixComp.jLabel12.text")); // NOI18N
        jLabel12.setAlignmentX(0.5F);
        jPanel7.add(jLabel12);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_channel)
                    .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(lbl_channel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private org.jjazz.ui.flatcomponents.api.FlatIntegerKnob knb_bassDrum;
    private org.jjazz.ui.flatcomponents.api.FlatIntegerKnob knb_crash;
    private org.jjazz.ui.flatcomponents.api.FlatIntegerKnob knb_cymbals;
    private org.jjazz.ui.flatcomponents.api.FlatIntegerKnob knb_hihat;
    private org.jjazz.ui.flatcomponents.api.FlatIntegerKnob knb_perc;
    private org.jjazz.ui.flatcomponents.api.FlatIntegerKnob knb_snare;
    private org.jjazz.ui.flatcomponents.api.FlatIntegerKnob knb_toms;
    private javax.swing.JLabel lbl_channel;
    // End of variables declaration//GEN-END:variables


}
