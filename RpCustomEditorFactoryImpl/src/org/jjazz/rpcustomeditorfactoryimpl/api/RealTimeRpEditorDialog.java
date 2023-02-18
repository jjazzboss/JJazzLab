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
package org.jjazz.rpcustomeditorfactoryimpl.api;

import static com.google.common.base.Preconditions.checkArgument;
import java.awt.BorderLayout;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.KeyEventPostProcessor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.Sequencer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.MusicController.State;
import org.jjazz.musiccontrol.api.playbacksession.UpdateProviderSongSession;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.SongContextProvider;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.rpviewer.spi.RpCustomEditor;
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.ResUtil;
import org.openide.*;
import org.openide.util.Exceptions;

/**
 * A RpCustomEditor dialog implementation which lets user preview the RP value changes in real time (while the sequence is
 * playing).
 * <p>
 * The dialog can be customized for a given RhythmParameter via the RealTimeRpEditorPanel panel which provides the RP value
 * editing capability.
 *
 * @param <E> RhythmParameter value class
 */
public class RealTimeRpEditorDialog<E> extends RpCustomEditor<E> implements PropertyChangeListener
{

    public static final int DEFAULT_PREVIEW_MAX_NB_BARS = 64;
    private static int previewMaxNbBars = DEFAULT_PREVIEW_MAX_NB_BARS;
    private final RealTimeRpEditorComponent<E> editor;
    private boolean exitOk;
    private UpdatableSongSession session;
    private SongPartContext songPartContextOriginal;
    private SongPartContext songPartContextpreview;
    private final E rpDefaultValue;
    private E saveRpValue;
    private GlobalKeyActionListener globalKeyListener;
    private PlaybackSession previousPlaybackSession;
    private static final Logger LOGGER = Logger.getLogger(RealTimeRpEditorDialog.class.getSimpleName());  //NOI18N

    public RealTimeRpEditorDialog(RealTimeRpEditorComponent<E> comp)
    {
        super(comp.isModal());
        
        editor = comp;
        editor.addPropertyChangeListener(this);
        setResizable(editor.isResizable());

        initComponents();

        pnl_editor.add(editor, BorderLayout.CENTER);
        pack();

        rpDefaultValue = getRhythmParameter().getDefaultValue();
    }

    @Override
    public RhythmParameter<E> getRhythmParameter()
    {
        return editor.getRhythmParameter();
    }

    @Override
    public void preset(E rpValue, SongPartContext sptContext)
    {
        if (rpValue == null || sptContext == null || sptContext.getSongParts().size() > 1)
        {
            throw new IllegalArgumentException("rpValue=" + rpValue + " sptContext=" + sptContext);
        }

        LOGGER.fine("preset() -- rpValue=" + rpValue + " sptContext=" + sptContext);

        songPartContextOriginal = sptContext;


        var spt = sptContext.getSongPart();
        if (!spt.getRhythm().getRhythmParameters().contains(editor.getRhythmParameter()))
        {
            throw new IllegalArgumentException("rpValue=" + rpValue + " sptContext=" + sptContext + " spt=" + spt + " getRhythmParameter()=" + getRhythmParameter());
        }


        // Reset UI
        editor.setEnabled(true);
        fbtn_reset.setEnabled(true);
        editor.preset(rpValue, sptContext);
        String title = editor.getTitle();
        setTitle(title == null ? buildDefaultTitle(spt) : title);
        fbtn_ok.requestFocusInWindow();
        tbtn_hear.setSelected(false);
        tbtn_compare.setSelected(false);
    }

    /**
     * Get the edited RpValue.
     *
     * @return Value is meaningful only if isExitOk() is true.
     */
    @Override
    public E getRpValue()
    {
        return tbtn_compare.isSelected() ? saveRpValue : editor.getEditedRpValue();
    }

    /**
     * Check if dialog was exited using OK
     *
     * @return False means user cancelled the operation.
     */
    @Override
    public boolean isExitOk()
    {
        return exitOk;
    }


    /**
     * Get the maximum number of bars used in the preview.
     *
     * @return the previewMaxNbBars
     */
    public int getPreviewMaxNbBars()
    {
        return previewMaxNbBars;
    }

    /**
     * Set the maximum number of bars used in the preview.
     * <p>
     */
    public void setPreviewMaxNbBars(int previewMaxNbBars)
    {
        if (previewMaxNbBars < 1)
        {
            throw new IllegalArgumentException("previewMaxNbBars=" + previewMaxNbBars);
        }
        this.previewMaxNbBars = previewMaxNbBars;
    }

    /**
     * Create a preview SongContext which uses the specified rhythm parameter value.
     *
     * @param rpValue
     * @return
     */
    static public <T> SongPartContext buildPreviewContext(SongPartContext sptContext, RhythmParameter<T> rp, T rpValue)
    {
        checkArgument(sptContext.getSongParts().size() == 1, "sptContext=%s, rp=%s, rpValue=%s", sptContext, rp, rpValue);

        // Get a song copy which uses the edited RP value        
        Song songCopy = SongFactory.getInstance().getCopy(sptContext.getSong(), false);
        SongStructure ss = songCopy.getSongStructure();
        ChordLeadSheet cls = songCopy.getChordLeadSheet();
        SongPart spt = ss.getSongPart(sptContext.getBarRange().from);


        // Make sure SongPart size does not exceed maxNbBars 
        CLI_Section section = spt.getParentSection();
        IntRange sectionRange = cls.getBarRange(section);
        if (sectionRange.size() > previewMaxNbBars)
        {
            // Shorten the section
            try
            {
                cls.setSizeInBars(sectionRange.from + previewMaxNbBars);
            } catch (UnsupportedEditException ex)
            {
                // We're removing a section, should never happen
                Exceptions.printStackTrace(ex);
            }
        }

        // Apply the RP value
        ss.setRhythmParameterValue(spt, rp, rpValue);


        // Create the new context
        SongPartContext res = new SongPartContext(songCopy, sptContext.getMidiMix(), spt);
        return res;
    }

    // ======================================================================================
    // PropertyChangeListener interface
    // ======================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == editor && evt.getPropertyName().equals(RealTimeRpEditorComponent.PROP_EDITED_RP_VALUE))
        {
            LOGGER.log(Level.FINE, "propertyChange() evt={0}", evt);
            if (tbtn_hear.isSelected() && !tbtn_compare.isSelected())
            {
                updateRpValueInPreviewContext((E) evt.getNewValue());
            }
        }
    }


    // ======================================================================================
    // Private methods
    // ======================================================================================
    private void exit(boolean ok)
    {
        MusicController mc = MusicController.getInstance();
        boolean wasPlaying = mc.getState().equals(State.PLAYING);
        mc.stop();


        // If song was playing and we're playing, restart with the original session
        if (previousPlaybackSession != null && wasPlaying)
        {
            try
            {
                mc.setPlaybackSession(previousPlaybackSession);
                mc.play(previousPlaybackSession.getBarRange().from);
            } catch (MusicGenerationException ex)
            {
                // Should never happen, it was working before this dialog opened
                Exceptions.printStackTrace(ex);
            }
        }

        exitOk = ok;
        setVisible(false);
        dispose();
    }


    private String buildDefaultTitle(SongPart spt)
    {
        String strSongPart = ResUtil.getString(getClass(), "RealTimeRpEditorDialog.song_part");
        strSongPart += " \"" + spt.getName() + "\" - bars " + (spt.getBarRange().from + 1) + "..." + (spt.getBarRange().to + 1);
        return getRhythmParameter().getDisplayName() + " - " + strSongPart;
    }


    /**
     * Start playback.
     */
    private void startPlayback()
    {
        // Prepare the dynamic session
        if (session != null)
        {
            session.close();
        }

        // Build song context with the original RP value or edited one
        E rpValue = tbtn_compare.isSelected() ? rpDefaultValue : editor.getEditedRpValue();
        songPartContextpreview = buildPreviewContext(songPartContextOriginal, getRhythmParameter(), rpValue);


        // UpdateProviderSongSession automatically generates updates if a RhythmParameter value changes
        var dynSession = UpdateProviderSongSession.getSession(songPartContextpreview,
                true,
                false,
                false,
                true,
                false,
                Sequencer.LOOP_CONTINUOUSLY,
                null);

        session = UpdatableSongSession.getSession(dynSession);
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
            mc.setPlaybackSession(session);
            mc.play(session.getBarRange().from);
        } catch (MusicGenerationException ex)
        {
            if (ex.getLocalizedMessage() != null)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }

    }


    /**
     * Update the RhythmParameter value in the preview context.
     *
     * @param rpValue
     */
    private void updateRpValueInPreviewContext(E rpValue)
    {
        SongStructure ss = songPartContextpreview.getSong().getSongStructure();
        SongPart spt = ss.getSongPart(songPartContextOriginal.getBarRange().from);
        ss.setRhythmParameterValue(spt, (RhythmParameter) editor.getRhythmParameter(), rpValue);
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
                fbtn_okActionPerformed(null);
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "actionCancel");   //NOI18N
        contentPane.getActionMap().put("actionCancel", new AbstractAction("Cancel")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                exit(false);
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
        tbtn_compare = new org.jjazz.ui.flatcomponents.api.FlatToggleButton();
        tbtn_hear = new org.jjazz.ui.flatcomponents.api.FlatToggleButton();
        fbtn_ok = new org.jjazz.ui.flatcomponents.api.FlatButton();
        fbtn_reset = new org.jjazz.ui.flatcomponents.api.FlatButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosed(java.awt.event.WindowEvent evt)
            {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt)
            {
                formWindowOpened(evt);
            }
        });

        pnl_editor.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        pnl_editor.setLayout(new java.awt.BorderLayout());

        tbtn_compare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditorfactoryimpl/api/resources/CompareArrows-OFF.png"))); // NOI18N
        tbtn_compare.setToolTipText(org.openide.util.NbBundle.getMessage(RealTimeRpEditorDialog.class, "RealTimeRpEditorDialog.tbtn_compare.toolTipText")); // NOI18N
        tbtn_compare.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditorfactoryimpl/api/resources/CompareArrows-Disabled.png"))); // NOI18N
        tbtn_compare.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditorfactoryimpl/api/resources/CompareArrows-ON.png"))); // NOI18N
        tbtn_compare.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tbtn_compareActionPerformed(evt);
            }
        });

        tbtn_hear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditorfactoryimpl/api/resources/SpeakerOff-24x24.png"))); // NOI18N
        tbtn_hear.setToolTipText(org.openide.util.NbBundle.getMessage(RealTimeRpEditorDialog.class, "RealTimeRpEditorDialog.tbtn_hear.toolTipText")); // NOI18N
        tbtn_hear.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditorfactoryimpl/api/resources/SpeakerOnRed-24x24.png"))); // NOI18N
        tbtn_hear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tbtn_hearActionPerformed(evt);
            }
        });

        fbtn_ok.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditorfactoryimpl/api/resources/OK-24x24.png"))); // NOI18N
        fbtn_ok.setToolTipText(org.openide.util.NbBundle.getMessage(RealTimeRpEditorDialog.class, "RealTimeRpEditorDialog.fbtn_ok.toolTipText")); // NOI18N
        fbtn_ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_okActionPerformed(evt);
            }
        });

        fbtn_reset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditorfactoryimpl/api/resources/Reset-24x24.png"))); // NOI18N
        fbtn_reset.setToolTipText(org.openide.util.NbBundle.getMessage(RealTimeRpEditorDialog.class, "RealTimeRpEditorDialog.fbtn_reset.toolTipText")); // NOI18N
        fbtn_reset.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_resetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_containerLayout = new javax.swing.GroupLayout(pnl_container);
        pnl_container.setLayout(pnl_containerLayout);
        pnl_containerLayout.setHorizontalGroup(
            pnl_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_containerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnl_editor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fbtn_reset, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tbtn_hear, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tbtn_compare, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fbtn_ok, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5))
        );
        pnl_containerLayout.setVerticalGroup(
            pnl_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_containerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_containerLayout.createSequentialGroup()
                        .addComponent(tbtn_hear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tbtn_compare, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fbtn_reset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fbtn_ok, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(pnl_editor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(10, 10, 10))
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

    private void tbtn_hearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tbtn_hearActionPerformed
    {//GEN-HEADEREND:event_tbtn_hearActionPerformed

        if (tbtn_hear.isSelected())
        {
            startPlayback();
        } else
        {
            MusicController.getInstance().stop();
        }
    }//GEN-LAST:event_tbtn_hearActionPerformed

    private void tbtn_compareActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tbtn_compareActionPerformed
    {//GEN-HEADEREND:event_tbtn_compareActionPerformed
        if (tbtn_compare.isSelected())
        {
            saveRpValue = editor.getEditedRpValue();
            editor.setEditedRpValue(rpDefaultValue);
            editor.setEnabled(false);
        } else
        {
            assert saveRpValue != null;
            editor.setEditedRpValue(saveRpValue);
            editor.setEnabled(true);
        }

        if (tbtn_hear.isSelected())
        {
            updateRpValueInPreviewContext(editor.getEditedRpValue());
        }
        fbtn_reset.setEnabled(!tbtn_compare.isSelected());

    }//GEN-LAST:event_tbtn_compareActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosed
    {//GEN-HEADEREND:event_formWindowClosed
        // Triggered by dispose() call
        if (editor != null)
        {
            editor.cleanup();
        }
        if (session != null)
        {
            session.close();
        }
        DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(globalKeyListener);
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        exit(false);
    }//GEN-LAST:event_formWindowClosing

    private void formWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowOpened
    {//GEN-HEADEREND:event_formWindowOpened
        if (songPartContextOriginal == null)
        {
            throw new IllegalStateException("songPartContextOriginal is null: preset() must be called before making dialog visible");
        }

        // If song was already playing, directly switch to the preview mode
        previousPlaybackSession = null;
        var mc = MusicController.getInstance();
        if (mc.getState().equals(State.PLAYING))
        {
            mc.stop();


            // Start preview mode, except if we were in arranger mode
            var prevSession = mc.getPlaybackSession();
            if (prevSession instanceof UpdatableSongSession)
            {
                Song song = ((SongContextProvider) prevSession).getSongContext().getSong();
                if (!song.getName().startsWith("*!ArrangerSONG!*"))
                {
                    tbtn_hear.setSelected(true);
                    tbtn_hearActionPerformed(null);     // This will start playing the preview


                    // Commented out to disable the feature (auto-restart playback when exiting)
                    // previousPlaybackSession = prevSession.getFreshCopy();
                }
            }

        }


        // Add our global key listener
        globalKeyListener = new GlobalKeyActionListener();
        DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(globalKeyListener);

    }//GEN-LAST:event_formWindowOpened

    private void fbtn_okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_okActionPerformed
    {//GEN-HEADEREND:event_fbtn_okActionPerformed
        exit(true);
    }//GEN-LAST:event_fbtn_okActionPerformed

    private void fbtn_resetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_resetActionPerformed
    {//GEN-HEADEREND:event_fbtn_resetActionPerformed
        String msg = ResUtil.getString(getClass(), "CTL_ConfirmReset");
        NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.OK_CANCEL_OPTION);
        Object result = DialogDisplayer.getDefault().notify(nd);
        if (result == NotifyDescriptor.OK_OPTION)
        {
            editor.setEditedRpValue(editor.getRhythmParameter().getDefaultValue());
        }
    }//GEN-LAST:event_fbtn_resetActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_ok;
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_reset;
    private javax.swing.JPanel pnl_container;
    private javax.swing.JPanel pnl_editor;
    private org.jjazz.ui.flatcomponents.api.FlatToggleButton tbtn_compare;
    private org.jjazz.ui.flatcomponents.api.FlatToggleButton tbtn_hear;
    // End of variables declaration//GEN-END:variables


// =================================================================================
// Private classes
// =================================================================================
    /**
     * Use a global approach to trigger some keyboard actions because we can't control what will be in the
     * RealTimeRpEditorComponent.
     * <p>
     * E.g. if there is a JList, it will capture the SPACE key...
     */
    private class GlobalKeyActionListener implements KeyEventPostProcessor
    {

        @Override
        public boolean postProcessKeyEvent(KeyEvent e)
        {
            boolean b = false;
            if (e.getID() == KeyEvent.KEY_RELEASED && e.getKeyChar() == ' ')
            {
                // LOGGER.severe("postProcessKeyEvent() e=" + e);
                tbtn_hear.doClick();
                b = true;
            }
            return b;
        }
    }

}
