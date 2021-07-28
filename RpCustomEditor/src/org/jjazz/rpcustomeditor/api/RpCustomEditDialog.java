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
package org.jjazz.rpcustomeditor.api;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequencer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.DynamicSongSession;
import org.jjazz.musiccontrol.api.playbacksession.SongContextSession;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythmmusicgeneration.api.Phrase;
import org.jjazz.rhythmmusicgeneration.api.SongContext;
import org.jjazz.rpcustomeditor.spi.RpCustomEditor;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.util.api.IntRange;
import org.openide.*;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * A dialog to show a RpCustomEditor panel.
 */
public class RpCustomEditDialog extends javax.swing.JDialog implements PropertyChangeListener
{

    private static RpCustomEditDialog INSTANCE;
    private RpCustomEditor<?> rpEditor;
    private boolean exitOk;
    private DynamicSongSession session;
    private SongContext songContextOriginal;
    private SongContext workContext;
    private static final Logger LOGGER = Logger.getLogger(RpCustomEditDialog.class.getSimpleName());  //NOI18N

    public static RpCustomEditDialog getInstance()
    {
        synchronized (RpCustomEditDialog.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RpCustomEditDialog(WindowManager.getDefault().getMainWindow(), true);
            }
        }
        return INSTANCE;
    }

    /**
     * Creates new form RpCustomEditDialog
     */
    private RpCustomEditDialog(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();
    }

    /**
     * Initialize the dialog.
     *
     * @param <E>
     * @param editor
     * @param sgContext The bar range must correspond to a single SongPart (a single RhythmParameter value).
     */
    public <E> void preset(RpCustomEditor<E> editor, SongContext sgContext)
    {
        if (editor == null || sgContext == null
                || sgContext.getSongParts().size() > 1
                || !sgContext.getSongParts().get(0).getRhythm().getRhythmParameters().contains(editor.getRhythmParameter()))
        {
            throw new IllegalArgumentException("editor=" + editor + " rp=" + editor.getRhythmParameter() + " sgContext=" + sgContext);
        }


        if (rpEditor != null)
        {
            rpEditor.removePropertyChangeListener(RpCustomEditor.PROP_RP_VALUE, this);
            rpEditor.cleanup();
        }

        // Update UI
        rpEditor = editor;
        pnl_editor.removeAll();
        pnl_editor.add(rpEditor, BorderLayout.CENTER);
        var spt = sgContext.getSongParts().get(0);
        setTitle(rpEditor.getRhythmParameter().getDisplayName() + " (" + spt.getName() + " bar:" + spt.getStartBarIndex() + ")");
        pack();
        btn_ok.requestFocusInWindow();      // Must be after pack()
        tbtn_hear.setSelected(false);
        tbtn_bypass.setSelected(false);
        tbtn_bypass.setEnabled(false);
    }

    /**
     * Get the edited RpValue.
     *
     * @return Value is meaningful only if isExitOk() is true.
     */
    public Object getRpValue()
    {
        return rpEditor.getEditedRpValue();
    }

    /**
     * Check if dialog was exited using OK
     *
     * @return False means user cancelled the operation.
     */
    public boolean isExitOk()
    {
        return exitOk;
    }

    // ======================================================================================
    // PropertyChangeListener interface
    // ======================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == rpEditor && evt.getPropertyName().equals(RpCustomEditor.PROP_RP_VALUE))
        {
            if (tbtn_hear.isSelected())
            {
                try
                {
                    updateSession();
                } catch (MusicGenerationException ex)
                {
                    LOGGER.severe("propertyChange() ex=" + ex.getMessage());
                }
            }
        }
    }


    // ======================================================================================
    // Private methods
    // ======================================================================================
    private void exit(boolean ok)
    {
        MusicController mc = MusicController.getInstance();
        mc.stop();

        exitOk = ok;
        setVisible(false);
    }

    /**
     * Return the original or current context depending on the bypass button.
     *
     * @return
     */
    private SongContext pickContext()
    {
        SongContext res = songContextOriginal;
        if (!tbtn_bypass.isSelected())
        {
            res = buildContextWithCurrentRpValue();
        }
        return res;
    }

    private void updateSession() throws MusicGenerationException
    {
        assert session != null && tbtn_hear.isSelected();

        SongContext sgContext = pickContext();
        SongContextSession tmpSession = SongContextSession.getSession(sgContext, true, false, false, true, 0, null);
        tmpSession.generate(false);

        // 
        Map<Integer, List<MidiEvent>> mapTrackIdEvents = new HashMap<>();


        // Process only changed phrases
        var originalRvPhraseMap = session.getOriginalRvPhraseMap();
        var originalRvTrackIdMap = session.getOriginalRvTrackIdMap();
        var newRvPhraseMap = tmpSession.getRvPhraseMap();

        for (var rv : originalRvPhraseMap.keySet())
        {
            Phrase p = originalRvPhraseMap.get(rv);
            Phrase newP = newRvPhraseMap.get(rv);
            if (p.equals(newP))
            {
                LOGGER.info("updateSession() skipped identitical phrases for rv=" + rv + " p.size()=" + p.size());
            } else
            {
                LOGGER.info("updateSession() different phrases for rv=" + rv + ", newP.size()=" + newP.size() + " => saving MidiEvents");
                mapTrackIdEvents.put(originalRvTrackIdMap.get(rv), newP.toMidiEvents());
            }
        }

        // Update the dynamic session
        session.updateSequence(mapTrackIdEvents);
    }

    /**
     * Set the workContext and start playback with it.
     *
     * @param sgContext
     */
    private void startPlayback(SongContext sgContext)
    {
        workContext = null;


        // Prepare the dynamic session
        if (session != null)
        {
            session.cleanup();
        }
        session = new DynamicSongSession(sgContext,
                true,
                false,
                false,
                true,
                Sequencer.LOOP_CONTINUOUSLY,
                null);
        try
        {
            session.generate(false);
        } catch (MusicGenerationException ex)
        {
            if (ex.getLocalizedMessage() != null)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
            return;
        }


        // Play
        MusicController mc = MusicController.getInstance();
        try
        {
            mc.play(session, sgContext.getBarRange().from);
        } catch (MusicGenerationException ex)
        {
            if (ex.getLocalizedMessage() != null)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }

        workContext = sgContext;
    }


    /**
     * Create a new context where SongPart uses the edited rhythm parameter value.
     *
     * @return
     */
    private SongContext buildContextWithCurrentRpValue()
    {
        // Get a song copy which uses the edited RP value
        Song songCopy = SongFactory.getInstance().getCopy(songContextOriginal.getSong());
        SongStructure ssCopy = songCopy.getSongStructure();
        SongPart sptCopy = ssCopy.getSongPart(songContextOriginal.getBarRange().from);
        ssCopy.setRhythmParameterValue(sptCopy, (RhythmParameter) rpEditor.getRhythmParameter(), rpEditor.getEditedRpValue());

        // Create the new context
        SongContext res = new SongContext(songCopy, songContextOriginal.getMidiMix(), songContextOriginal.getBarRange());
        return res;
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
                btn_okActionPerformed(null);
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "actionCancel");   //NOI18N
        contentPane.getActionMap().put("actionCancel", new AbstractAction("Cancel")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                btn_cancelActionPerformed(null);
            }
        });
        return contentPane;
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_container = new javax.swing.JPanel();
        pnl_editor = new javax.swing.JPanel();
        tbtn_bypass = new org.jjazz.ui.flatcomponents.api.FlatToggleButton();
        tbtn_hear = new org.jjazz.ui.flatcomponents.api.FlatToggleButton();
        btn_ok = new org.jjazz.ui.utilities.api.SmallFlatDarkLafButton();
        btn_cancel = new org.jjazz.ui.utilities.api.SmallFlatDarkLafButton();

        setModalityType(java.awt.Dialog.ModalityType.DOCUMENT_MODAL);
        setResizable(false);

        pnl_editor.setLayout(new javax.swing.BoxLayout(pnl_editor, javax.swing.BoxLayout.LINE_AXIS));

        tbtn_bypass.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditor/api/resources/CompareArrows-OFF.png"))); // NOI18N
        tbtn_bypass.setToolTipText(org.openide.util.NbBundle.getMessage(RpCustomEditDialog.class, "RpCustomEditDialog.tbtn_bypass.toolTipText")); // NOI18N
        tbtn_bypass.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditor/api/resources/CompareArrows-ON.png"))); // NOI18N
        tbtn_bypass.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tbtn_bypassActionPerformed(evt);
            }
        });

        tbtn_hear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditor/api/resources/SpeakerOff-24x24.png"))); // NOI18N
        tbtn_hear.setToolTipText(org.openide.util.NbBundle.getMessage(RpCustomEditDialog.class, "RpCustomEditDialog.tbtn_hear.toolTipText")); // NOI18N
        tbtn_hear.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditor/api/resources/SpeakerOnRed-24x24.png"))); // NOI18N
        tbtn_hear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tbtn_hearActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_ok, org.openide.util.NbBundle.getMessage(RpCustomEditDialog.class, "RpCustomEditDialog.btn_ok.text")); // NOI18N
        btn_ok.setFont(btn_ok.getFont().deriveFont(btn_ok.getFont().getSize()-2f));
        btn_ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_okActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_cancel, org.openide.util.NbBundle.getMessage(RpCustomEditDialog.class, "RpCustomEditDialog.btn_cancel.text")); // NOI18N
        btn_cancel.setFont(btn_cancel.getFont().deriveFont(btn_cancel.getFont().getSize()-2f));
        btn_cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_containerLayout = new javax.swing.GroupLayout(pnl_container);
        pnl_container.setLayout(pnl_containerLayout);
        pnl_containerLayout.setHorizontalGroup(
            pnl_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_containerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_containerLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_ok, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_cancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnl_containerLayout.createSequentialGroup()
                        .addComponent(pnl_editor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnl_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tbtn_hear, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tbtn_bypass, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        pnl_containerLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_cancel, btn_ok});

        pnl_containerLayout.setVerticalGroup(
            pnl_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_containerLayout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(pnl_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_containerLayout.createSequentialGroup()
                        .addComponent(tbtn_hear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tbtn_bypass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(pnl_editor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_cancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_ok, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnl_container, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnl_container, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_okActionPerformed
    {//GEN-HEADEREND:event_btn_okActionPerformed
        exit(true);
    }//GEN-LAST:event_btn_okActionPerformed

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cancelActionPerformed
    {//GEN-HEADEREND:event_btn_cancelActionPerformed
        exit(false);
    }//GEN-LAST:event_btn_cancelActionPerformed

    private void tbtn_hearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tbtn_hearActionPerformed
    {//GEN-HEADEREND:event_tbtn_hearActionPerformed
        if (tbtn_hear.isSelected())
        {
            startPlayback(pickContext());
            tbtn_bypass.setEnabled(true);
        } else
        {
            MusicController.getInstance().stop();
            tbtn_bypass.setEnabled(false);
        }
    }//GEN-LAST:event_tbtn_hearActionPerformed

    private void tbtn_bypassActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tbtn_bypassActionPerformed
    {//GEN-HEADEREND:event_tbtn_bypassActionPerformed
        assert tbtn_hear.isSelected();
    }//GEN-LAST:event_tbtn_bypassActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.ui.utilities.api.SmallFlatDarkLafButton btn_cancel;
    private org.jjazz.ui.utilities.api.SmallFlatDarkLafButton btn_ok;
    private javax.swing.JPanel pnl_container;
    private javax.swing.JPanel pnl_editor;
    private org.jjazz.ui.flatcomponents.api.FlatToggleButton tbtn_bypass;
    private org.jjazz.ui.flatcomponents.api.FlatToggleButton tbtn_hear;
    // End of variables declaration//GEN-END:variables

}
