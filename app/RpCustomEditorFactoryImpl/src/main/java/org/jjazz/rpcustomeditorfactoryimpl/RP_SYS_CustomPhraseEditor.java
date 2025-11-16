package org.jjazz.rpcustomeditorfactoryimpl;

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.musiccontrol.api.playbacksession.BaseSongSession;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.SessionConfig;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhraseValue;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.rpviewer.api.RpCustomEditorDialog;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;


/**
 * A RpCustomEditor for RP_SYS_CustomPhrase.
 * <p>
 * The editor can not use the standard RpCustomEditorDialog<E> mechanism (i.e. make the modifications within the modal dialog) because we use a
 * PianoRollEditorTopComponent which remains available after dialog is closed, and RP_SYS_CustomPhraseValue is mutable (MutableRpValue instance).
 */
public class RP_SYS_CustomPhraseEditor extends RpCustomEditorDialog<RP_SYS_CustomPhraseValue>
{

    public static final Color PHRASE_COMP_CUSTOMIZED_FOREGROUND = new Color(255, 102, 102);
    public static final Color PHRASE_COMP_FOREGROUND = new Color(102, 153, 255);
    private final RP_SYS_CustomPhrase rp;
    private RP_SYS_CustomPhraseValue rpValue;
    private SongPartContext songPartContext;
    private boolean exitOk;
    private final Map<RhythmVoice, Phrase> mapRvPhrase = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_CustomPhraseEditor.class.getSimpleName());

    public RP_SYS_CustomPhraseEditor(RP_SYS_CustomPhrase rp)
    {
        super(true);
        this.rp = rp;

        initComponents();

        list_rhythmVoices.setCellRenderer(new RhythmVoiceRenderer());

        UIUtilities.installEscapeKeyAction(this, () -> btn_cancelActionPerformed(null));
        UIUtilities.installEnterKeyAction(this, () -> btn_editActionPerformed(null));

    }

    @Override
    public RP_SYS_CustomPhrase getRhythmParameter()
    {
        return rp;
    }

    @Override
    public void preset(RP_SYS_CustomPhraseValue rpValue, SongPartContext sptContext)
    {
        checkNotNull(rpValue);
        checkNotNull(sptContext);

        setTitle(buildTitle(sptContext.getSongPart()));

        mapRvPhrase.clear();


        songPartContext = sptContext;
        var spt = songPartContext.getSongPart();
        Rhythm r = spt.getRhythm();


        LOGGER.log(Level.FINE, "preset() -- rpValue={0} spt={1}", new Object[]
        {
            rpValue, spt
        });


        // We need a new RP_SYS_CustomPhraseValue instance between the caller framework will compare with equals() the old rpValue with the new rpValue
        // to decide to update the SongPart RP value.
        // If there is at least 1 non-empty customized phrase, rpValue instances will be different because NoteEvents are never equal.
        this.rpValue = new RP_SYS_CustomPhraseValue(r);
        this.rpValue.set(rpValue);       // This let's us add/remove a customized phrase but with the same Phrase instances


        // Populate the JList and select the first customized RhythmVoice, if any
        var rvs = r.getRhythmVoices();
        list_rhythmVoices.setListData(rvs.toArray(RhythmVoice[]::new));
        int index = 0;
        var crvs = rpValue.getCustomizedRhythmVoices();
        if (!crvs.isEmpty())
        {
            for (int i = 0; i < rvs.size(); i++)
            {
                if (crvs.contains(rvs.get(i)))
                {
                    index = i;
                    break;
                }
            }
        }
        list_rhythmVoices.setSelectedIndex(index);


        var rpVariation = RP_SYS_Variation.getVariationRp(r);
        String strVariation = rpVariation == null ? "" : "/" + spt.getRPValue(rpVariation);
        lbl_phraseInfo.setText(r.getName() + strVariation);


        // Start a task to generate the phrases 
        Runnable task = () -> 
        {
            SessionConfig config = new SessionConfig(false, false, false, 0, null);
            BaseSongSession tmpSession = new BaseSongSession(songPartContext, config, false, PlaybackSession.Context.RP_VALUE_PREVIEW);
            try
            {
                tmpSession.generate(true);          // This can block for some time, possibly a few seconds on slow computers/complex rhythms              
            } catch (MusicGenerationException ex)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                tmpSession.close();
                SwingUtilities.invokeLater(() -> btn_cancelActionPerformed(null));
                return;
            }

            // Retrieve the data
            setMapRvPhrase(tmpSession.getRvPhraseMap());        // This will refresh the UI
            tmpSession.close();

        };

        new Thread(task).start();

        pack();

    }

    @Override
    public RP_SYS_CustomPhraseValue getRpValue()
    {
        return rpValue;
    }


    @Override
    public boolean isExitOk()
    {
        return exitOk;
    }

    @Override
    public String toString()
    {
        return "RP_SYS_CustomPhraseEditor[rp=" + rp + "]";
    }

    // ===================================================================================
    // Private methods
    // ===================================================================================

    /**
     * Called by thread started from preset() when phrases are ready.
     * <p>
     *
     * @param map A map generated by SongSequenceBuilder, with no RhythmVoiceDelegate since they have been processed. Phrases start at beat 0.
     */
    private synchronized void setMapRvPhrase(Map<RhythmVoice, Phrase> map)
    {
        // LOGGER.log(Level.SEVERE, "setMapRvPhrase() -- map={0}", org.jjazz.util.api.Utilities.toMultilineString(map));

        mapRvPhrase.clear();
        mapRvPhrase.putAll(map);

        SwingUtilities.invokeLater(() -> refreshUI());  // Refresh the birdview
    }

    /**
     *
     * Get the phrase (customized or not) for the specified RhythmVoice.
     * <p>
     * Manage the case of a RhythmVoice or RhythmVoiceDelegate
     *
     * @param rv
     * @return Can be null!
     */
    private synchronized Phrase getPhrase(RhythmVoice rv)
    {
        if (mapRvPhrase.isEmpty() || rpValue == null)
        {
            return null;
        }
        Phrase p = null;
        if (isCustomizedPhrase(rv))
        {
            p = rpValue.getCustomizedPhrase(rv);
        } else if (rv instanceof RhythmVoiceDelegate)
        {
            p = mapRvPhrase.get(((RhythmVoiceDelegate) rv).getSource());
        } else
        {
            p = mapRvPhrase.get(rv);
        }

        return p;
    }

    private void setCustomizedPhrase(RhythmVoice rv, Phrase p)
    {
        rpValue.setCustomizedPhrase(rv, p);
        forceListRepaint();
    }

    private void removeCustomizedPhrase(RhythmVoice rv)
    {
        rpValue.removeCustomizedPhrase(rv);
        forceListRepaint();
    }

    private void forceListRepaint()
    {
        list_rhythmVoices.repaint(list_rhythmVoices.getCellBounds(0,
                songPartContext.getSongPart().getRhythm().getRhythmVoices().size() - 1));
    }

    private boolean isCustomizedPhrase(RhythmVoice rv)
    {
        return rpValue == null ? false : rpValue.getCustomizedRhythmVoices().contains(rv);
    }

    private void refreshUI()
    {
        String text = "";
        String tooltip = "";
        boolean isCustom = false;

        RhythmVoice rv = getCurrentRhythmVoice();
        if (rv != null)
        {
            isCustom = isCustomizedPhrase(rv);
            Phrase p = getPhrase(rv);
            if (p != null)
            {
                // Computed phrases are shifted to start at 0.
                FloatRange fr = songPartContext.getBeatRange().getTransformed(-songPartContext.getBeatRange().from);
                birdViewComponent.setModel(p, rp.getRhythm().getTimeSignature(), fr);
                birdViewComponent.setForeground(isCustom ? PHRASE_COMP_CUSTOMIZED_FOREGROUND : PHRASE_COMP_FOREGROUND);
            }

            text = isCustom ? rv.getName() + " " + ResUtil.getString(getClass(), "RP_SYS_CustomPhraseEditor.Customized") : "";
            tooltip = "Midi channel " + (getChannel(rv) + 1);
        }

        lbl_rhythmVoice.setText(text);
        lbl_rhythmVoice.setToolTipText(tooltip);
        btn_reset.setEnabled(isCustom);
        btn_edit.setEnabled(rv != null);
    }


    private String buildTitle(SongPart spt)
    {
        String txt = "\"" + spt.getName() + "\" - bars " + (spt.getBarRange().from + 1) + ".." + (spt.getBarRange().to + 1);
        return ResUtil.getString(getClass(), "RP_SYS_CustomPhraseEditor.DialogTitle", txt);
    }


    private void editCurrentPhrase()
    {
        RhythmVoice rv = getCurrentRhythmVoice();
        if (rv == null)
        {
            return;
        }


        var p = getPhrase(rv);
        if (!isCustomizedPhrase(rv))
        {
            setCustomizedPhrase(rv, p);
        }


        Song song = songPartContext.getSong();
        MidiMix midiMix = null;
        try
        {
            midiMix = MidiMixManager.getDefault().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
            return;
        }
        var spt = songPartContext.getSongPart();


        SongEditorManager.getDefault().showPianoRollEditorForSptCustomPhrase(song, midiMix, spt, rv, p);

    }


    /**
     *
     * @return Can be null, can be a RhythmVoiceDelegate
     */
    private RhythmVoice getCurrentRhythmVoice()
    {
        return list_rhythmVoices.getSelectedValue();
    }


    private int getChannel(RhythmVoice rv)
    {
        return songPartContext.getMidiMix().getChannel(rv);
    }

    private Instrument getInstrument(RhythmVoice rv)
    {
        return songPartContext.getMidiMix().getInstrumentMix(rv).getInstrument();
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_overlay = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list_rhythmVoices = new javax.swing.JList<>();
        lbl_rhythmVoice = new javax.swing.JLabel();
        birdViewComponent = new org.jjazz.coreuicomponents.api.PhraseBirdsEyeViewComponent();
        lbl_phraseInfo = new javax.swing.JLabel();
        btn_edit = new javax.swing.JButton();
        btn_cancel = new javax.swing.JButton();
        btn_reset = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(500, 200));

        list_rhythmVoices.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_rhythmVoices.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseEditor.class, "RP_SYS_CustomPhraseEditor.list_rhythmVoices.toolTipText")); // NOI18N
        list_rhythmVoices.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                list_rhythmVoicesMouseClicked(evt);
            }
        });
        list_rhythmVoices.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_rhythmVoicesValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(list_rhythmVoices);

        lbl_rhythmVoice.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_rhythmVoice, "Bass [customized]"); // NOI18N

        birdViewComponent.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        birdViewComponent.setForeground(new java.awt.Color(102, 153, 255));
        birdViewComponent.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseEditor.class, "RP_SYS_CustomPhraseEditor.birdViewComponent.toolTipText")); // NOI18N

        javax.swing.GroupLayout birdViewComponentLayout = new javax.swing.GroupLayout(birdViewComponent);
        birdViewComponent.setLayout(birdViewComponentLayout);
        birdViewComponentLayout.setHorizontalGroup(
            birdViewComponentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        birdViewComponentLayout.setVerticalGroup(
            birdViewComponentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        org.openide.awt.Mnemonics.setLocalizedText(lbl_phraseInfo, "Rhythm"); // NOI18N
        lbl_phraseInfo.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseEditor.class, "RP_SYS_CustomPhraseEditor.lbl_phraseInfo.toolTipText")); // NOI18N

        btn_edit.setFont(btn_edit.getFont().deriveFont(btn_edit.getFont().getStyle() | java.awt.Font.BOLD));
        org.openide.awt.Mnemonics.setLocalizedText(btn_edit, org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseEditor.class, "RP_SYS_CustomPhraseEditor.btn_edit.text")); // NOI18N
        btn_edit.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseEditor.class, "RP_SYS_CustomPhraseEditor.btn_edit.toolTipText")); // NOI18N
        btn_edit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_editActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_cancel, org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseEditor.class, "RP_SYS_CustomPhraseEditor.btn_cancel.text")); // NOI18N
        btn_cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_reset, org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseEditor.class, "RP_SYS_CustomPhraseEditor.btn_reset.text")); // NOI18N
        btn_reset.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseEditor.class, "RP_SYS_CustomPhraseEditor.btn_reset.toolTipText")); // NOI18N
        btn_reset.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_resetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_overlayLayout = new javax.swing.GroupLayout(pnl_overlay);
        pnl_overlay.setLayout(pnl_overlayLayout);
        pnl_overlayLayout.setHorizontalGroup(
            pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_overlayLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
                    .addComponent(lbl_phraseInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_overlayLayout.createSequentialGroup()
                        .addGap(0, 78, Short.MAX_VALUE)
                        .addComponent(btn_edit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_reset)
                        .addGap(18, 18, 18)
                        .addComponent(btn_cancel))
                    .addComponent(birdViewComponent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lbl_rhythmVoice, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnl_overlayLayout.setVerticalGroup(
            pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_overlayLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_phraseInfo)
                    .addComponent(lbl_rhythmVoice))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_overlayLayout.createSequentialGroup()
                        .addComponent(birdViewComponent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_cancel)
                            .addComponent(btn_edit)
                            .addComponent(btn_reset)))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE))
                .addContainerGap())
        );

        getContentPane().add(pnl_overlay, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void list_rhythmVoicesValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_rhythmVoicesValueChanged
    {//GEN-HEADEREND:event_list_rhythmVoicesValueChanged
        if (evt.getValueIsAdjusting())
        {
            return;
        }

        refreshUI();

    }//GEN-LAST:event_list_rhythmVoicesValueChanged

    private void btn_editActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_editActionPerformed
    {//GEN-HEADEREND:event_btn_editActionPerformed
        editCurrentPhrase();
        exitOk = true;
        setVisible(false);
    }//GEN-LAST:event_btn_editActionPerformed

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cancelActionPerformed
    {//GEN-HEADEREND:event_btn_cancelActionPerformed
        exitOk = false;
        setVisible(false);
    }//GEN-LAST:event_btn_cancelActionPerformed

    private void btn_resetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_resetActionPerformed
    {//GEN-HEADEREND:event_btn_resetActionPerformed
        removeCustomizedPhrase(getCurrentRhythmVoice());
        exitOk = true;
        setVisible(false);
    }//GEN-LAST:event_btn_resetActionPerformed

    private void list_rhythmVoicesMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_list_rhythmVoicesMouseClicked
    {//GEN-HEADEREND:event_list_rhythmVoicesMouseClicked
        if (evt.getClickCount() == 2 && getCurrentRhythmVoice() != null)
        {
            btn_editActionPerformed(null);
        }
    }//GEN-LAST:event_list_rhythmVoicesMouseClicked



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.coreuicomponents.api.PhraseBirdsEyeViewComponent birdViewComponent;
    private javax.swing.JButton btn_cancel;
    private javax.swing.JButton btn_edit;
    private javax.swing.JButton btn_reset;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_phraseInfo;
    private javax.swing.JLabel lbl_rhythmVoice;
    private javax.swing.JList<RhythmVoice> list_rhythmVoices;
    private javax.swing.JPanel pnl_overlay;
    // End of variables declaration//GEN-END:variables

    // ===================================================================================
    // Private classes
    // ===================================================================================

    /**
     * A list renderer for RhythmVoices.
     */
    private class RhythmVoiceRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            RhythmVoice rv = (RhythmVoice) value;
            int channel = -1;
            String s = rv.getName() + " [" + channel + "]";
            if (songPartContext != null)
            {
                channel = getChannel(rv) + 1;
                var ins = getInstrument(rv);
                s = "[" + channel + "] " + ins.getPatchName() + " / " + rv.getName();
            }
            lbl.setText(s);
            Font f = lbl.getFont();
            String tooltip = "Midi channel " + channel;
            if (rpValue != null && rpValue.getCustomizedRhythmVoices().contains(rv))
            {
                f = f.deriveFont(Font.ITALIC);
                tooltip = "Customized - " + tooltip;
            }
            lbl.setFont(f);
            lbl.setToolTipText(tooltip);
            return lbl;
        }
    }


}
