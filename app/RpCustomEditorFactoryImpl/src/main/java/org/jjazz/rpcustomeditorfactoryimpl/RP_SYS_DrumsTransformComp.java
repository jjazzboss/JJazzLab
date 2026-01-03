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
package org.jjazz.rpcustomeditorfactoryimpl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListener;
import org.jjazz.musiccontrol.api.playbacksession.BaseSongSession;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.DrumsMixTransform;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransformChain;
import org.jjazz.phrasetransform.api.PhraseTransformManager;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransformValue;
import org.jjazz.phrasetransform.api.ui.PhraseTransformListCellRenderer;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import static org.jjazz.rpcustomeditorfactoryimpl.RP_SYS_CustomPhraseEditor.PHRASE_COMP_FOREGROUND;
import org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorDialog;
import org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorComponent;
import static org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorComponent.PROP_EDITED_RP_VALUE;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.flatcomponents.api.FlatIntegerKnob;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession.Context;
import org.jjazz.musiccontrol.api.playbacksession.SessionConfig;
import org.jjazz.uiutilities.api.UIUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * A RP editor component for RP_SYS_DrumsTransformValue.
 */
public class RP_SYS_DrumsTransformComp extends RealTimeRpEditorComponent<RP_SYS_DrumsTransformValue> implements ListDataListener, PropertyChangeListener, PlaybackListener
{

    private static final DataFlavor PT_DATA_FLAVOR = new DataFlavor(PhraseTransform.class, "Phrase Transformer");
    private final RP_SYS_DrumsTransform rp;
    private RP_SYS_DrumsTransformValue lastValue;
    private RP_SYS_DrumsTransformValue uiValue;
    private SongPartContext songPartContext;
    private TimeSignature timeSignature;
    private boolean saveDrumsTrackSoloState;
    private final DefaultListModel<PhraseTransform> list_transformChainModel = new DefaultListModel<>();
    private SizedPhrase originalPhrase;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_DrumsTransformComp.class.getSimpleName());

    /**
     * Creates new form RP_SYS_DrumsTransformerComp
     */
    public RP_SYS_DrumsTransformComp(RP_SYS_DrumsTransform rp)
    {
        checkNotNull(rp);
        this.rp = rp;

        initComponents();

        registerKnobs();


        // Update JLists
        list_transformChain.setModel(list_transformChainModel);
        list_transformChain.setTransferHandler(new TransformChainListTransferHandler());
        list_transformChain.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("DELETE"),
                "RemoveTransform");
        list_transformChain.getActionMap().put("RemoveTransform", new DeleteAction());


        list_availableTransforms.setTransferHandler(new TransformChainListTransferHandler());


        list_transformChainModel.addListDataListener(this);


        MusicController.getInstance().addPlaybackListener(this);

    }


    @Override
    public RP_SYS_DrumsTransform getRhythmParameter()
    {
        return rp;
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        list_transformChain.setEnabled(b);
        lbl_transformList.setEnabled(b);
        lbl_allTransforms.setEnabled(b);
        lbl_arrows.setEnabled(b);
        list_transformChainSelectionChanged();
        UIUtilities.setRecursiveEnabled(b, pnl_mix);

        if (uiValue != null)
        {
            // Update UI
            refreshUI();
        }
    }


    @Override
    public void preset(RP_SYS_DrumsTransformValue rpValue, SongPartContext sptContext)
    {
        checkNotNull(sptContext);

        LOGGER.log(Level.FINE, "preset() -- rpValue={0} sptContext={1}", new Object[]
        {
            rpValue, sptContext
        });

        songPartContext = sptContext;
        timeSignature = songPartContext.getSongPart().getRhythm().getTimeSignature();
        originalPhrase = null;
        var insMix = songPartContext.getMidiMix().getInstrumentMix(rp.getRhythmVoice());
        saveDrumsTrackSoloState = insMix.isSolo();
        tbtn_solo.setSelected(insMix.isSolo());

        uiValue = null;
        setEditedRpValue(rpValue);


        updateAvailableTransformsList();


        // Start a task to get the generated drums phrase 
        Runnable task = () -> 
        {
            SongContext workContext = RealTimeRpEditorDialog.buildPreviewContext(songPartContext, rp, rp.getDefaultValue());
            SessionConfig config = new SessionConfig(false, false, false, 0, null);
            BaseSongSession tmpSession = new BaseSongSession(workContext, config, false, PlaybackSession.Context.RP_VALUE_PREVIEW);
            try
            {
                tmpSession.generate(true);          // This can block for some time, possibly a few seconds on slow computers/complex rhythms              
            } catch (MusicGenerationException ex)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return;
            }

            // Retrieve the original phrase
            RhythmVoice rv = rp.getRhythmVoice();
            if (rv instanceof RhythmVoiceDelegate)
            {
                rv = ((RhythmVoiceDelegate) rv).getSource();
            }
            Phrase p = tmpSession.getRvPhraseMap().get(rv);
            setOriginalPhrase(p);
            tmpSession.close();
        };
        new Thread(task).start();

    }

    @Override
    public void setEditedRpValue(RP_SYS_DrumsTransformValue rpValue)
    {
        lastValue = uiValue;
        uiValue = new RP_SYS_DrumsTransformValue(rpValue);
        refreshUI();
        fireUiValueChanged();
    }

    @Override
    public RP_SYS_DrumsTransformValue getEditedRpValue()
    {
        return new RP_SYS_DrumsTransformValue(uiValue);
    }

    @Override
    public boolean isResizable()
    {
        return true;
    }


    @Override
    public void cleanup()
    {
        // Restore solo mode of the drums track
        var insMix = songPartContext.getMidiMix().getInstrumentMix(rp.getRhythmVoice());
        insMix.setSolo(saveDrumsTrackSoloState);

        MusicController.getInstance().removePlaybackListener(this);
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
                uiUpdatedByUser();
            }
        }
    }

    // ===============================================================================
    // ListDataListener interface
    // ===============================================================================  
    @Override
    public void intervalAdded(ListDataEvent e)
    {
        uiUpdatedByUser();
    }

    @Override
    public void intervalRemoved(ListDataEvent e)
    {
        uiUpdatedByUser();
    }

    @Override
    public void contentsChanged(ListDataEvent e)
    {
        uiUpdatedByUser();
    }

    // ===============================================================================
    // PlaybackListener interface
    // ===============================================================================  

    @Override
    public boolean isAccepted(PlaybackSession session)
    {
        return session.getContext() == Context.RP_VALUE_PREVIEW;
    }

    @Override
    public void enabledChanged(boolean b)
    {
        // Nothing
    }

    @Override
    public void beatChanged(Position oldPos, Position newPos, float newPosInBeats)
    {
        if (birdview_outPhrase.getModel() == null)
        {
            // If unlucky we might be notified just before model is set
            return;
        }

        float pos = -1;
        long tickPos = songPartContext.toRelativeTick(newPos);
        if (tickPos >= 0)
        {
            pos = (float) tickPos / MidiConst.PPQ_RESOLUTION - birdview_outPhrase.getBeatRange().from;
        }
        // LOGGER.severe("DEBUG beatChanged() newPosInBeats=" + newPosInBeats + " pos=" + pos);
        birdview_outPhrase.setMarkerPosition(pos);
    }

    @Override
    public void chordSymbolChanged(CLI_ChordSymbol chordSymbol)
    {
        // Nothing
    }

    @Override
    public void songPartChanged(SongPart spt)
    {
        // Nothing
    }

    @Override
    public void midiActivity(long tick, int channel)
    {
        // Nothing
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================    
    private void uiUpdatedByUser()
    {
        var chain = buildChainFromUI();
        lastValue = uiValue;
        uiValue = uiValue.getCopy(chain);
        refreshUI();
        fireUiValueChanged();
    }

    /**
     * Create a transform chain from the UI state.
     *
     * @return
     */
    private PhraseTransformChain buildChainFromUI()
    {
        var res = new PhraseTransformChain();

        List<PhraseTransform> pts = UIUtilities.getJListModelAsList(list_transformChainModel);
        res.addAll(pts);

        // Add the drums mix transform AFTER the transformation chain: it's more natural for user
        var dmt = new DrumsMixTransform();
        dmt.setBassDrumOffset(knb_bassDrum.getValue());
        dmt.setSnareOffset(knb_snare.getValue());
        dmt.setTomsOffset(knb_toms.getValue());
        dmt.setCymbalsOffset(knb_cymbals.getValue());
        dmt.setCrashOffset(knb_crash.getValue());
        dmt.setPercOffset(knb_perc.getValue());
        dmt.setHiHatOffset(knb_hihat.getValue());
        res.add(dmt);


        return res;
    }


    /**
     * Called by thread started from preset() when phrases are ready.
     * <p>
     *
     * @param The original drums phrase Can be null if no drums track available for the current variation
     */
    private synchronized void setOriginalPhrase(Phrase p)
    {
        LOGGER.log(Level.FINE, "setOriginalPhrase() -- p={0}", p);
        if (p == null)
        {
            LOGGER.log(Level.INFO, "setOriginalPhrase() p={0}", p);
            return;
        }
        originalPhrase = new SizedPhrase(getChannel(),
                songPartContext.getBeatRange()
                        .getTransformed(-songPartContext.getBeatRange().from), timeSignature, p.isDrums());
        originalPhrase.add(p);

        // Go back to the EDT
        SwingUtilities.invokeLater(() -> 
        {
            updateAvailableTransformsList();
            refreshUI();        // to refresh the birdViewComponent
        }
        );
    }

    /**
     * Synchronized because originalPhrase is set by a thread run by preset().
     * <p>
     * If original phrase is not available yet, provide an empty phrase.
     *
     * @param sp
     */
    private synchronized SizedPhrase getOriginalPhrase()
    {
        SizedPhrase sp = originalPhrase;
        if (sp == null)
        {
            sp = new SizedPhrase(getChannel(), songPartContext.getBeatRange().getTransformed(-songPartContext.getBeatRange().from),
                    timeSignature, true);
        }
        return sp;
    }

    private void updateAvailableTransformsList()
    {
        List<PhraseTransform> pts = PhraseTransformManager.getDefault().getRecommendedPhraseTransforms(getOriginalPhrase(),
                songPartContext, true);
        DefaultListModel dlm = new DefaultListModel();
        dlm.addAll(pts);
        list_availableTransforms.setModel(dlm);
    }


    /**
     * Maintain UI consistency.
     */
    private synchronized void refreshUI()
    {

        // Update the birdviews
        var transformChainComplete = uiValue.getTransformChain(false);
        SizedPhrase inPhrase = getOriginalPhrase();
        SizedPhrase outPhrase = (transformChainComplete == null) ? inPhrase : transformChainComplete.transform(inPhrase,
                songPartContext);
        birdview_outPhrase.setModel(outPhrase, outPhrase.getTimeSignature(), outPhrase.getNotesBeatRange());
        // birdview_outPhrase.setForeground(transformChain != null ? PHRASE_COMP_CUSTOMIZED_FOREGROUND : PHRASE_COMP_FOREGROUND);
        birdview_outPhrase.setForeground(transformChainComplete != null ? PHRASE_COMP_FOREGROUND : PHRASE_COMP_FOREGROUND);

        // Update the mix part
        var mix = uiValue.getDrumsMixTransform();
        unregisterKnobs();          // We don't want to generate change events because it's not user-triggered        
        knb_bassDrum.setValue(mix.getBassDrumOffset());
        knb_snare.setValue(mix.getSnareOffset());
        knb_toms.setValue(mix.getTomsOffset());
        knb_cymbals.setValue(mix.getCymbalsOffset());
        knb_crash.setValue(mix.getCrashOffset());
        knb_hihat.setValue(mix.getHiHatOffset());
        knb_perc.setValue(mix.getPercOffset());
        registerKnobs();


        // Update the transform part        
        var transformChainNoMix = uiValue.getTransformChain(true);
        list_transformChainModel.removeListDataListener(this);  // We don't want to generate change events because it's not user-triggered        
        list_transformChainModel.clear();
        if (transformChainNoMix != null)
        {
            list_transformChainModel.addAll(transformChainNoMix);
        }
        list_transformChainModel.addListDataListener(this);


        // Update the clear button
        btn_clear.setEnabled(list_transformChain.isEnabled() && !list_transformChainModel.isEmpty());

    }

    private void registerKnobs()
    {
        knb_bassDrum.addPropertyChangeListener(this);
        knb_snare.addPropertyChangeListener(this);
        knb_hihat.addPropertyChangeListener(this);
        knb_toms.addPropertyChangeListener(this);
        knb_crash.addPropertyChangeListener(this);
        knb_cymbals.addPropertyChangeListener(this);
        knb_perc.addPropertyChangeListener(this);
    }

    private void unregisterKnobs()
    {
        knb_bassDrum.removePropertyChangeListener(this);
        knb_snare.removePropertyChangeListener(this);
        knb_hihat.removePropertyChangeListener(this);
        knb_toms.removePropertyChangeListener(this);
        knb_crash.removePropertyChangeListener(this);
        knb_cymbals.removePropertyChangeListener(this);
        knb_perc.removePropertyChangeListener(this);
    }

    private int getChannel()
    {
        return songPartContext.getMidiMix().getChannel(rp.getRhythmVoice());
    }

    private void showTransformUserSettings(PhraseTransform pt)
    {
        checkArgument(pt != null && pt.hasUserSettings(), "pt=%s", pt);

        // Listen to properties changes while dialog is shown.
        PropertyChangeListener listener = e -> 
        {
            uiUpdatedByUser();
        };
        pt.getProperties().addPropertyChangeListener(listener);


        // Show the dialog
        pt.showUserSettingsDialog(btn_ptSettings);      // Blocks

        // Stop listening
        pt.getProperties().removePropertyChangeListener(listener);
    }

    private void list_transformChainSelectionChanged()
    {
        var selPt = list_transformChain.getSelectedValue();
        btn_ptSettings.setEnabled(list_transformChain.isEnabled() && selPt != null && selPt.hasUserSettings());
        btn_remove.setEnabled(list_transformChain.isEnabled() && selPt != null);
    }

    private void fireUiValueChanged()
    {
        LOGGER.log(Level.FINE, "fireUiValueChanged() -- lastvalue={0} uiValue={1}", new Object[]
        {
            lastValue, uiValue
        });
        firePropertyChange(PROP_EDITED_RP_VALUE, lastValue, uiValue);
    }


    // ===============================================================================
    // Inner classes
    // ===============================================================================    
    private class DeleteAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            int index = list_transformChain.getSelectedIndex();
            if (index != -1)
            {
                list_transformChainModel.remove(index);
            }
        }

    }


    /**
     * We don't directly use the PhraseTransform as data, because there might be several identical PhraseTransforms in a chain, so using index and list provides
     * a unique instance to be transferred.
     */
    private class PtData
    {

        private final JList<PhraseTransform> srcList;
        private final int srcPtIndex;

        public PtData(JList<PhraseTransform> srcList, int srcPtIndex)
        {
            this.srcPtIndex = srcPtIndex;
            this.srcList = srcList;
        }
    }

    /**
     * Transferable for a single PhraseTransform instance.
     */
    private class PtTransferable implements Transferable
    {

        private final PtData data;

        public PtTransferable(PtData data)
        {
            this.data = data;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[]
            {
                PT_DATA_FLAVOR
            };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor fl)
        {
            return fl.equals(PT_DATA_FLAVOR);
        }

        @Override
        public Object getTransferData(DataFlavor fl) throws UnsupportedFlavorException
        {
            if (fl.equals(PT_DATA_FLAVOR))
            {
                return data;
            } else
            {
                throw new UnsupportedFlavorException(fl);
            }
        }
    }

    /**
     * Supported use cases:<br>
     * - drag and drop from list_availableTransforms to list_transformChain : add<br>
     * - drag and drop from list_transformChain to list_availableTransforms : remove<br>
     * - drag and drop from list_transformChain to list_transformChain : reorder<br>
     */
    private class TransformChainListTransferHandler extends TransferHandler
    {

        @Override
        public boolean canImport(TransferHandler.TransferSupport info)
        {
            return list_transformChain.isEnabled() && info.isDataFlavorSupported(PT_DATA_FLAVOR);
        }

        /**
         * Create the transferable from the selected PhraseTransform
         */
        @Override
        protected Transferable createTransferable(JComponent jc)
        {
            JList list = (JList) jc;
            return new PtTransferable(new PtData(list, list.getSelectedIndex()));
        }

        /**
         * Only moveAll actions.
         */
        @Override
        public int getSourceActions(JComponent c)
        {
            return TransferHandler.MOVE;
        }

        /**
         * Different operations depending of source and target list.
         */
        @Override
        public boolean importData(TransferHandler.TransferSupport info)
        {
            if (!info.isDrop())
            {
                return false;
            }


            // Retrieve the data
            Transferable t = info.getTransferable();
            PtData ptData;
            try
            {
                ptData = (PtData) t.getTransferData(PT_DATA_FLAVOR);
            } catch (UnsupportedFlavorException | IOException ex)
            {
                return false;
            }
            JList srcList = ptData.srcList;
            int srcIndex = ptData.srcPtIndex;
            PhraseTransform srcPt = (PhraseTransform) srcList.getModel().getElementAt(srcIndex);


            JList destList = (JList) info.getComponent();
            if (srcList == list_availableTransforms && destList == list_availableTransforms)
            {
                // No import, do nothing
                return false;

            } else if (srcList == list_transformChain && destList == list_availableTransforms)
            {
                // Just remove the moved element
                DefaultListModel listModel = (DefaultListModel) list_transformChain.getModel();
                listModel.remove(srcIndex);
                return true;

            } else if (srcList == list_availableTransforms && destList == list_transformChain)
            {
                // Add a new element in the chain
                DefaultListModel listModel = (DefaultListModel) list_transformChain.getModel();
                JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
                try
                {
                    if (dl.isInsert())
                    {
                        // Add
                        listModel.add(dl.getIndex(), srcPt);
                    } else
                    {
                        // Replace existing
                        listModel.set(dl.getIndex(), srcPt);
                    }
                } catch (Exception ex)
                {
                    // Catch programming exception to log them, because they are not shown otherwise (because of java transferhandler mechanism?)
                    ex.printStackTrace();
                    return false;
                }

                return true;
            } else
            {
                // Reorder the chain
                DefaultListModel listModel = (DefaultListModel) list_transformChain.getModel();

                // Remove the moved element
                listModel.remove(srcIndex);

                JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
                int destIndex = dl.getIndex();
                if (srcIndex < destIndex)
                {
                    // We removed the source element which was before destIndex
                    destIndex--;
                }

                if (dl.isInsert())
                {
                    // Add
                    listModel.add(destIndex, srcPt);
                } else
                {
                    // Replace existing
                    listModel.set(destIndex, srcPt);
                }
                return true;

            }
        }

        @Override
        protected void exportDone(JComponent jc, Transferable data, int action)
        {
            // Nothing
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

        lbl_allTransforms = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list_availableTransforms = new javax.swing.JList<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_transformChain = new javax.swing.JList<>();
        lbl_transformList = new javax.swing.JLabel();
        btn_clear = new javax.swing.JButton();
        btn_ptSettings = new javax.swing.JButton();
        pnl_arrows = new javax.swing.JPanel();
        lbl_arrows = new javax.swing.JLabel();
        birdview_outPhrase = new org.jjazz.coreuicomponents.api.PhraseBirdsEyeViewComponent();
        jLabel3 = new javax.swing.JLabel();
        btn_remove = new javax.swing.JButton();
        pnl_mix = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        knb_bassDrum = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        jLabel1 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        knb_hihat = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        jLabel8 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        knb_snare = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        jLabel7 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        knb_toms = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        jLabel9 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        knb_cymbals = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        jLabel10 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        knb_crash = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        jLabel11 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        knb_perc = new org.jjazz.flatcomponents.api.FlatIntegerKnob();
        jLabel12 = new javax.swing.JLabel();
        pnl_header = new javax.swing.JPanel();
        lbl_drumsTrack = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 32767));
        tbtn_solo = new org.jjazz.flatcomponents.api.FlatToggleButton();
        flatHelpButton1 = new org.jjazz.flatcomponents.api.FlatHelpButton();

        org.openide.awt.Mnemonics.setLocalizedText(lbl_allTransforms, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.lbl_allTransforms.text")); // NOI18N

        list_availableTransforms.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_availableTransforms.setCellRenderer(new PhraseTransformListCellRenderer(false));
        list_availableTransforms.setDragEnabled(true);
        list_availableTransforms.setDropMode(javax.swing.DropMode.ON);
        list_availableTransforms.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                list_availableTransformsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(list_availableTransforms);

        list_transformChain.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_transformChain.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.list_transformChain.toolTipText")); // NOI18N
        list_transformChain.setCellRenderer(new PhraseTransformListCellRenderer(false));
        list_transformChain.setDragEnabled(true);
        list_transformChain.setDropMode(javax.swing.DropMode.ON_OR_INSERT);
        list_transformChain.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                list_transformChainMouseClicked(evt);
            }
        });
        list_transformChain.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_transformChainValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(list_transformChain);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_transformList, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.lbl_transformList.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_clear, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.btn_clear.text")); // NOI18N
        btn_clear.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.btn_clear.toolTipText")); // NOI18N
        btn_clear.setEnabled(false);
        btn_clear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_clearActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_ptSettings, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.btn_ptSettings.text")); // NOI18N
        btn_ptSettings.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.btn_ptSettings.toolTipText")); // NOI18N
        btn_ptSettings.setEnabled(false);
        btn_ptSettings.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_ptSettingsActionPerformed(evt);
            }
        });

        pnl_arrows.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 5));

        lbl_arrows.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditorfactoryimpl/resources/CompareArrows-OFF.png"))); // NOI18N
        lbl_arrows.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.lbl_arrows.toolTipText")); // NOI18N
        pnl_arrows.add(lbl_arrows);

        birdview_outPhrase.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        birdview_outPhrase.setForeground(new java.awt.Color(102, 153, 255));

        javax.swing.GroupLayout birdview_outPhraseLayout = new javax.swing.GroupLayout(birdview_outPhrase);
        birdview_outPhrase.setLayout(birdview_outPhraseLayout);
        birdview_outPhraseLayout.setHorizontalGroup(
            birdview_outPhraseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        birdview_outPhraseLayout.setVerticalGroup(
            birdview_outPhraseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 61, Short.MAX_VALUE)
        );

        jLabel3.setFont(jLabel3.getFont());
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.jLabel3.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_remove, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.btn_remove.text")); // NOI18N
        btn_remove.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.btn_remove.toolTipText")); // NOI18N
        btn_remove.setEnabled(false);
        btn_remove.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_removeActionPerformed(evt);
            }
        });

        pnl_mix.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        pnl_mix.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.pnl_mix.toolTipText")); // NOI18N
        pnl_mix.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        knb_bassDrum.setKnobStartAngle(220.0);
        knb_bassDrum.setMaxValue(64);
        knb_bassDrum.setMinValue(-64);
        knb_bassDrum.setPanoramicType(true);
        knb_bassDrum.setValue(0);
        knb_bassDrum.setValueLineColor(new java.awt.Color(102, 255, 0));
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
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.jLabel1.text")); // NOI18N
        jLabel1.setAlignmentX(0.5F);
        jPanel1.add(jLabel1);

        jPanel8.add(jPanel1);

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

        knb_hihat.setKnobStartAngle(220.0);
        knb_hihat.setMaxValue(64);
        knb_hihat.setMinValue(-64);
        knb_hihat.setPanoramicType(true);
        knb_hihat.setValue(0);
        knb_hihat.setValueLineColor(knb_bassDrum.getValueLineColor());
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

        jPanel4.add(knb_hihat);

        jLabel8.setFont(jLabel8.getFont().deriveFont(jLabel8.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.jLabel8.text")); // NOI18N
        jLabel8.setAlignmentX(0.5F);
        jPanel4.add(jLabel8);

        jPanel8.add(jPanel4);

        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.Y_AXIS));

        knb_snare.setKnobStartAngle(220.0);
        knb_snare.setMaxValue(64);
        knb_snare.setMinValue(-64);
        knb_snare.setPanoramicType(true);
        knb_snare.setValue(0);
        knb_snare.setValueLineColor(knb_bassDrum.getValueLineColor());
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

        jPanel5.add(knb_snare);

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.jLabel7.text")); // NOI18N
        jLabel7.setAlignmentX(0.5F);
        jPanel5.add(jLabel7);

        jPanel8.add(jPanel5);

        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.Y_AXIS));

        knb_toms.setKnobStartAngle(220.0);
        knb_toms.setMaxValue(64);
        knb_toms.setMinValue(-64);
        knb_toms.setPanoramicType(true);
        knb_toms.setValue(0);
        knb_toms.setValueLineColor(knb_bassDrum.getValueLineColor());
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

        jPanel6.add(knb_toms);

        jLabel9.setFont(jLabel9.getFont().deriveFont(jLabel9.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel9, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.jLabel9.text")); // NOI18N
        jLabel9.setAlignmentX(0.5F);
        jPanel6.add(jLabel9);

        jPanel8.add(jPanel6);

        jPanel7.setLayout(new javax.swing.BoxLayout(jPanel7, javax.swing.BoxLayout.Y_AXIS));

        knb_cymbals.setKnobStartAngle(220.0);
        knb_cymbals.setMaxValue(64);
        knb_cymbals.setMinValue(-64);
        knb_cymbals.setPanoramicType(true);
        knb_cymbals.setValue(0);
        knb_cymbals.setValueLineColor(knb_bassDrum.getValueLineColor());
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

        jPanel7.add(knb_cymbals);

        jLabel10.setFont(jLabel10.getFont().deriveFont(jLabel10.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel10, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.jLabel10.text")); // NOI18N
        jLabel10.setAlignmentX(0.5F);
        jPanel7.add(jLabel10);

        jPanel8.add(jPanel7);

        jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9, javax.swing.BoxLayout.Y_AXIS));

        knb_crash.setKnobStartAngle(220.0);
        knb_crash.setMaxValue(64);
        knb_crash.setMinValue(-64);
        knb_crash.setPanoramicType(true);
        knb_crash.setValue(0);
        knb_crash.setValueLineColor(knb_bassDrum.getValueLineColor());
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

        jPanel9.add(knb_crash);

        jLabel11.setFont(jLabel11.getFont().deriveFont(jLabel11.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel11, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.jLabel11.text")); // NOI18N
        jLabel11.setAlignmentX(0.5F);
        jPanel9.add(jLabel11);

        jPanel8.add(jPanel9);

        jPanel10.setLayout(new javax.swing.BoxLayout(jPanel10, javax.swing.BoxLayout.Y_AXIS));

        knb_perc.setKnobStartAngle(220.0);
        knb_perc.setMaxValue(64);
        knb_perc.setMinValue(-64);
        knb_perc.setPanoramicType(true);
        knb_perc.setValue(0);
        knb_perc.setValueLineColor(knb_bassDrum.getValueLineColor());
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

        jPanel10.add(knb_perc);

        jLabel12.setFont(jLabel12.getFont().deriveFont(jLabel12.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel12, org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.jLabel12.text")); // NOI18N
        jLabel12.setAlignmentX(0.5F);
        jPanel10.add(jLabel12);

        jPanel8.add(jPanel10);

        pnl_mix.add(jPanel8);

        pnl_header.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 1));

        lbl_drumsTrack.setFont(lbl_drumsTrack.getFont().deriveFont(lbl_drumsTrack.getFont().getStyle() | java.awt.Font.BOLD));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_drumsTrack, "JJazz-S878.ff2.yjz - channel 10 - Std Kit"); // NOI18N
        pnl_header.add(lbl_drumsTrack);
        pnl_header.add(filler1);

        tbtn_solo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditorfactoryimpl/resources/SoloOff_Icon-21x21.png"))); // NOI18N
        tbtn_solo.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.tbtn_solo.toolTipText")); // NOI18N
        tbtn_solo.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rpcustomeditorfactoryimpl/resources/SoloOn_Icon-21x21.png"))); // NOI18N
        tbtn_solo.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tbtn_soloActionPerformed(evt);
            }
        });
        pnl_header.add(tbtn_solo);

        flatHelpButton1.setHelpText(org.openide.util.NbBundle.getMessage(RP_SYS_DrumsTransformComp.class, "RP_SYS_DrumsTransformComp.flatHelpButton1.helpText")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnl_header, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnl_mix, javax.swing.GroupLayout.DEFAULT_SIZE, 565, Short.MAX_VALUE)
                            .addComponent(birdview_outPhrase, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(pnl_arrows, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(lbl_allTransforms))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(lbl_transformList)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(btn_ptSettings, javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(btn_remove, javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(flatHelpButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btn_clear)))))))
                        .addContainerGap())))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_clear, btn_ptSettings, btn_remove});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(pnl_header, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(pnl_mix, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_allTransforms)
                    .addComponent(lbl_transformList))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnl_arrows, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_ptSettings))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 55, Short.MAX_VALUE)
                        .addComponent(btn_remove)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_clear)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(flatHelpButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(birdview_outPhrase, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btn_clearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_clearActionPerformed
    {//GEN-HEADEREND:event_btn_clearActionPerformed
        list_transformChainModel.clear();
    }//GEN-LAST:event_btn_clearActionPerformed

    private void list_transformChainValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_transformChainValueChanged
    {//GEN-HEADEREND:event_list_transformChainValueChanged
        if (evt.getValueIsAdjusting())
        {
            return;
        }

        list_transformChainSelectionChanged();

    }//GEN-LAST:event_list_transformChainValueChanged

    private void list_availableTransformsMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_list_availableTransformsMouseClicked
    {//GEN-HEADEREND:event_list_availableTransformsMouseClicked
        if (evt.getClickCount() == 2 && list_transformChain.isEnabled())
        {
            var pt = list_availableTransforms.getSelectedValue();
            if (pt != null)
            {
                list_transformChainModel.addElement(pt);
            }
        }
    }//GEN-LAST:event_list_availableTransformsMouseClicked

    private void btn_ptSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_ptSettingsActionPerformed
    {//GEN-HEADEREND:event_btn_ptSettingsActionPerformed
        var pt = list_transformChain.getSelectedValue();
        if (pt != null && pt.hasUserSettings())
        {
            showTransformUserSettings(pt);
        }
    }//GEN-LAST:event_btn_ptSettingsActionPerformed



    private void btn_removeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_removeActionPerformed
    {//GEN-HEADEREND:event_btn_removeActionPerformed
        var index = list_transformChain.getSelectedIndex();
        if (index != -1)
        {
            list_transformChainModel.remove(index);
        }
    }//GEN-LAST:event_btn_removeActionPerformed

    private void tbtn_soloActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tbtn_soloActionPerformed
    {//GEN-HEADEREND:event_tbtn_soloActionPerformed
        MidiMix mm = songPartContext.getMidiMix();
        var insMix = mm.getInstrumentMix(rp.getRhythmVoice());
        insMix.setSolo(tbtn_solo.isSelected());
    }//GEN-LAST:event_tbtn_soloActionPerformed

    private void list_transformChainMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_list_transformChainMouseClicked
    {//GEN-HEADEREND:event_list_transformChainMouseClicked
        if (evt.getClickCount() == 2)
        {
            var pt = list_transformChain.getSelectedValue();
            if (pt != null && pt.hasUserSettings())
            {
                showTransformUserSettings(pt);
            }
        }
    }//GEN-LAST:event_list_transformChainMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.coreuicomponents.api.PhraseBirdsEyeViewComponent birdview_outPhrase;
    private javax.swing.JButton btn_clear;
    private javax.swing.JButton btn_ptSettings;
    private javax.swing.JButton btn_remove;
    private javax.swing.Box.Filler filler1;
    private org.jjazz.flatcomponents.api.FlatHelpButton flatHelpButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knb_bassDrum;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knb_crash;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knb_cymbals;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knb_hihat;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knb_perc;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knb_snare;
    private org.jjazz.flatcomponents.api.FlatIntegerKnob knb_toms;
    private javax.swing.JLabel lbl_allTransforms;
    private javax.swing.JLabel lbl_arrows;
    private javax.swing.JLabel lbl_drumsTrack;
    private javax.swing.JLabel lbl_transformList;
    private javax.swing.JList<PhraseTransform> list_availableTransforms;
    private javax.swing.JList<PhraseTransform> list_transformChain;
    private javax.swing.JPanel pnl_arrows;
    private javax.swing.JPanel pnl_header;
    private javax.swing.JPanel pnl_mix;
    private org.jjazz.flatcomponents.api.FlatToggleButton tbtn_solo;
    // End of variables declaration//GEN-END:variables


}
