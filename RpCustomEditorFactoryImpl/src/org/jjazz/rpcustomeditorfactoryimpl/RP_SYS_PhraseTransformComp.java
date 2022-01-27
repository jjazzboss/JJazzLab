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
package org.jjazz.rpcustomeditorfactoryimpl;

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.Instrument;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.StaticSongSession;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransformChain;
import org.jjazz.phrasetransform.api.PhraseTransformManager;
import org.jjazz.phrasetransform.api.rps.RP_SYS_PhraseTransform;
import org.jjazz.phrasetransform.api.rps.RP_SYS_PhraseTransformValue;
import org.jjazz.phrasetransform.api.ui.PhraseTransformListCellRenderer;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import static org.jjazz.rpcustomeditorfactoryimpl.RP_SYS_CustomPhraseComp.PHRASE_COMP_FOREGROUND;
import org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorDialog;
import org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorComponent;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.ui.utilities.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * A RP editor component for RP_SYS_PhraseTransformValue.
 */
public class RP_SYS_PhraseTransformComp extends RealTimeRpEditorComponent<RP_SYS_PhraseTransformValue> implements ListDataListener, ActionListener
{

    private static final DataFlavor PT_DATA_FLAVOR = new DataFlavor(PhraseTransform.class, "Phrase Transformer");
    private final RP_SYS_PhraseTransform rp;
    private RP_SYS_PhraseTransformValue lastValue;
    private RP_SYS_PhraseTransformValue uiValue;
    private SongPartContext songPartContext;
    private TimeSignature timeSignature;
    private List<RhythmVoice> rhythmVoices;
    private final DefaultListModel<PhraseTransform> list_transformChainModel = new DefaultListModel<>();
    private RvComboBoxModel cmb_RhythmVoicesModel;
    private final HashMap<RhythmVoice, SizedPhrase> mapRvPhrase = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_PhraseTransformComp.class.getSimpleName());  //NOI18N

    /**
     * Creates new form RP_SYS_DrumsTransformerComp
     */
    public RP_SYS_PhraseTransformComp(RP_SYS_PhraseTransform rp)
    {
        checkNotNull(rp);
        this.rp = rp;

        initComponents();

        // Update JLists
        list_transformChain.setModel(list_transformChainModel);
        list_transformChain.setTransferHandler(new TransformChainListTransferHandler());
        list_transformChain.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("DELETE"), "RemoveTransform");   //NOI18N
        list_transformChain.getActionMap().put("RemoveTransform", new DeleteAction());   //NOI18N


        list_availableTransforms.setTransferHandler(new TransformChainListTransferHandler());


        list_transformChainModel.addListDataListener(this);
        cmb_rhythmVoices.addActionListener(this);

    }

    @Override
    public RP_SYS_PhraseTransform getRhythmParameter()
    {
        return rp;
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        list_transformChain.setEnabled(b);

        if (uiValue != null)
        {
            // Update UI
            updateUI(uiValue);
            list_transformChainSelectionChanged();
            lbl_transformList.setEnabled(b);
            hlp_area.setEnabled(b);
            lbl_arrows.setEnabled(b);
        }
    }


    @Override
    public void preset(RP_SYS_PhraseTransformValue rpValue, SongPartContext sptContext)
    {
        checkNotNull(sptContext);

        LOGGER.log(Level.FINE, "preset() -- rpValue={0} sptContext={1}", new Object[]
        {
            rpValue, sptContext
        });

        songPartContext = sptContext;
        rhythmVoices = songPartContext.getSongPart().getRhythm().getRhythmVoices();
        timeSignature = songPartContext.getSongPart().getRhythm().getTimeSignature();
        assert !rhythmVoices.isEmpty();
        mapRvPhrase.clear();


        uiValue = new RP_SYS_PhraseTransformValue(rpValue);
        lastValue = new RP_SYS_PhraseTransformValue(uiValue);


        // Update the RhythmVoices combo box and select the first transformed rhythm voice
        cmb_rhythmVoices.removeActionListener(this);
        cmb_RhythmVoicesModel = new RvComboBoxModel();
        cmb_RhythmVoicesModel.addAll(rhythmVoices);
        cmb_rhythmVoices.setModel(cmb_RhythmVoicesModel);
        var transformedRvs = rpValue.getChainRhythmVoices();
        RhythmVoice startRv = rhythmVoices.stream()
                .filter(rv -> transformedRvs.contains(rv))
                .findAny()
                .orElse(null);
        if (startRv == null)
        {
            startRv = rhythmVoices.get(0);
        }
        cmb_rhythmVoices.setSelectedItem(startRv);
        cmb_rhythmVoices.addActionListener(this);


        updateAvailableTransformsList(startRv);


        // Start a task to get the generated phrases 
        Runnable task = () ->
        {
            SongContext workContext = RealTimeRpEditorDialog.buildPreviewContext(songPartContext, rp, rp.getDefaultValue());
            StaticSongSession tmpSession = StaticSongSession.getSession(workContext, false, false, false, false, 0, null);
            if (tmpSession.getState().equals(PlaybackSession.State.NEW))
            {
                try
                {
                    tmpSession.generate(true);          // This can block for some time, possibly a few seconds on slow computers/complex rhythms              
                } catch (MusicGenerationException ex)
                {
                    NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(d);
                    return;
                }
            }

            // Retrieve the data
            setMapRvPhrase(tmpSession.getRvPhraseMap());        // Will call UpdateUI(rpValue)
            tmpSession.close();
        };
        new Thread(task).start();

    }

    @Override
    public void setEditedRpValue(RP_SYS_PhraseTransformValue rpValue)
    {
        lastValue = uiValue;
        uiValue = new RP_SYS_PhraseTransformValue(rpValue);
        updateUI(uiValue);
    }

    @Override
    public RP_SYS_PhraseTransformValue getEditedRpValue()
    {
        return uiValue;
    }

    @Override
    public boolean isResizable()
    {
        return true;
    }


    @Override
    public void cleanup()
    {
        // 
    }


    // ===============================================================================
    // ActionListener interface
    // ===============================================================================
    @Override
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getSource() == cmb_rhythmVoices)
        {
            updateAvailableTransformsList(getCurrentRhythmVoice());
            updateUI(uiValue);
        }
    }


    // ===============================================================================
    // ListDataListener interface
    // ===============================================================================  
    @Override
    public void intervalAdded(ListDataEvent e)
    {
        list_transformChainChanged();
    }

    @Override
    public void intervalRemoved(ListDataEvent e)
    {
        list_transformChainChanged();
    }

    @Override
    public void contentsChanged(ListDataEvent e)
    {
        list_transformChainChanged();
    }


    // ===============================================================================
    // Private methods
    // ===============================================================================    
    private void updateUiValue(PhraseTransformChain chain)
    {
        var rv = getCurrentRhythmVoice();
        lastValue = uiValue;
        uiValue = uiValue.getUpdatedTransformChain(rv, chain);
        updateUI(uiValue);
        fireUiValueChanged();
    }

    /**
     * Called when list_transformChain was directly modified by user.
     */
    private void list_transformChainChanged()
    {
        // Update uiValue from list_transformChain model
        List<PhraseTransform> pts = Utilities.getListModelAsList(list_transformChainModel);
        var chain = new PhraseTransformChain(pts);
        updateUiValue(chain);
    }


    /**
     * Called by thread started from preset() when phrases are ready.
     * <p>
     *
     * @param map A map generated by SongSequenceBuilder, with no RhythmVoiceDelegate since they have been processed
     */
    private synchronized void setMapRvPhrase(Map<RhythmVoice, Phrase> map)
    {
        LOGGER.log(Level.FINE, "setMapRvPhrase() -- map={0}", map);

        for (var rv : map.keySet())
        {
            Phrase p = map.get(rv);     // Phrase always start at bar 0
            int channel = getChannel(rv);
            SizedPhrase sp = new SizedPhrase(channel, songPartContext.getBeatRange().getTransformed(-songPartContext.getBeatRange().from), timeSignature);
            sp.add(p);
            mapRvPhrase.put(rv, sp);
        }

        // Refresh the birdviews
        updateUI(uiValue);
    }


    /**
     * Synchronized because mapRvPhrase is filled by a thread run by preset().
     * <p>
     * Manage the case of RhythmVoice and RhythmVoiceDelegates.
     *
     * @param rv
     * @param sp
     */
    private synchronized SizedPhrase getInPhrase(RhythmVoice rv)
    {
        if (rv instanceof RhythmVoiceDelegate)
        {
            rv = ((RhythmVoiceDelegate) rv).getSource();
        }
        SizedPhrase sp = mapRvPhrase.get(rv);
        if (sp == null)
        {
            sp = new SizedPhrase(getChannel(rv), songPartContext.getBeatRange().getTransformed(-songPartContext.getBeatRange().from), timeSignature);
        }
        return sp;
    }

    private void updateAvailableTransformsList(RhythmVoice rv)
    {
        List<PhraseTransform> pts = PhraseTransformManager.getDefault().getRecommendedPhraseTransforms(getInPhrase(rv), songPartContext, true);
        DefaultListModel dlm = new DefaultListModel();
        dlm.addAll(pts);
        list_availableTransforms.setModel(dlm);
    }

    /**
     * Update the UI to reflect the specified RP value for the current rhythm voice.
     */
    private synchronized void updateUI(RP_SYS_PhraseTransformValue rpValue)
    {
        var rv = getCurrentRhythmVoice();
        var transformChain = rpValue.getTransformChain(rv);


        // Update the list transform chain if required
        var pts = Utilities.getListModelAsList(list_transformChainModel);
        if (!pts.equals(transformChain))
        {
            // NOTE: we don't want to generate list change events because it's not user-triggered
            list_transformChainModel.removeListDataListener(this);
            list_transformChainModel.clear();
            if (transformChain != null)
            {
                list_transformChainModel.addAll(transformChain);
            }
            list_transformChainModel.addListDataListener(this);
        }


        // Update the birdviews
        SizedPhrase inPhrase = getInPhrase(rv);
        SizedPhrase outPhrase = (transformChain == null) ? inPhrase : transformChain.transform(inPhrase, songPartContext);
        birdview_outPhrase.setModel(outPhrase, outPhrase.getTimeSignature(), outPhrase.getBeatRange());
        // birdview_outPhrase.setForeground(transformChain != null ? PHRASE_COMP_CUSTOMIZED_FOREGROUND : PHRASE_COMP_FOREGROUND);
        birdview_outPhrase.setForeground(transformChain != null ? PHRASE_COMP_FOREGROUND : PHRASE_COMP_FOREGROUND);


        // Update the clear button
        btn_clearChain.setEnabled(list_transformChain.isEnabled() && !list_transformChainModel.isEmpty());


        // Make sure cmb is repaint
        cmb_RhythmVoicesModel.triggerRepaint();

    }


    private RhythmVoice getCurrentRhythmVoice()
    {
        RhythmVoice rv = (RhythmVoice) cmb_rhythmVoices.getSelectedItem();
        return rv;
    }

    private int getChannel(RhythmVoice rv)
    {
        return songPartContext.getMidiMix().getChannel(rv);
    }

    private Instrument getInstrument(RhythmVoice rv)
    {
        return songPartContext.getMidiMix().getInstrumentMixFromKey(rv).getInstrument();
    }

    private void list_transformChainSelectionChanged()
    {
        var pt = list_transformChain.getSelectedValue();
        btn_ptSettings.setEnabled(list_transformChain.isEnabled() && pt != null && pt.hasUserSettings());
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

    private class RvComboBoxModel extends DefaultComboBoxModel
    {

        public void triggerRepaint()
        {
            fireContentsChanged(this, 0, this.getSize() - 1);
        }
    }

    private class RhythmVoiceRenderer extends BasicComboBoxRenderer
    {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            RhythmVoice rv = (RhythmVoice) value;

            if (rv != null)
            {
                int channel = getChannel(rv);
                var ins = getInstrument(rv);
                String text = "[" + (channel + 1) + "] " + ins.getPatchName() + " / " + rv.getName();

                if (uiValue != null && uiValue.getTransformChain(rv) != null)
                {
                    text += " *";
                }

                label.setText(text);
            }

            return label;
        }
    }

    /**
     * We don't directly use the PhraseTransform as data, because there might be several identical PhraseTransforms in a chain, so
     * using index and list provides a unique instance to be transferred.
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
         * Only move actions.
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
                        listModel.add(dl.getIndex(), srcPt.getCopy());
                    } else
                    {
                        // Replace existing
                        listModel.set(dl.getIndex(), srcPt.getCopy());
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
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
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
        btn_clearChain = new javax.swing.JButton();
        btn_ptSettings = new javax.swing.JButton();
        pnl_arrows = new javax.swing.JPanel();
        lbl_arrows = new javax.swing.JLabel();
        birdview_outPhrase = new org.jjazz.phrase.api.ui.PhraseBirdsEyeViewComponent();
        jScrollPane3 = new javax.swing.JScrollPane();
        hlp_area = new org.jjazz.ui.utilities.api.HelpTextArea();
        cmb_rhythmVoices = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(lbl_allTransforms, org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.lbl_allTransforms.text")); // NOI18N

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
        list_transformChain.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.list_transformChain.toolTipText")); // NOI18N
        list_transformChain.setCellRenderer(new PhraseTransformListCellRenderer(false));
        list_transformChain.setDragEnabled(true);
        list_transformChain.setDropMode(javax.swing.DropMode.ON_OR_INSERT);
        list_transformChain.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_transformChainValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(list_transformChain);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_transformList, org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.lbl_transformList.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_clearChain, org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.btn_clearChain.text")); // NOI18N
        btn_clearChain.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.btn_clearChain.toolTipText")); // NOI18N
        btn_clearChain.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_clearChainActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_ptSettings, org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.btn_ptSettings.text")); // NOI18N
        btn_ptSettings.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.btn_ptSettings.toolTipText")); // NOI18N
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
        lbl_arrows.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.lbl_arrows.toolTipText")); // NOI18N
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
            .addGap(0, 69, Short.MAX_VALUE)
        );

        jScrollPane3.setBorder(null);

        hlp_area.setColumns(20);
        hlp_area.setRows(5);
        hlp_area.setText(org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.hlp_area.text")); // NOI18N
        jScrollPane3.setViewportView(hlp_area);

        cmb_rhythmVoices.setRenderer(new RhythmVoiceRenderer());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.jLabel4.text")); // NOI18N

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(RP_SYS_PhraseTransformComp.class, "RP_SYS_PhraseTransformComp.jLabel3.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(birdview_outPhrase, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pnl_arrows, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lbl_allTransforms))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_transformList))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(btn_clearChain)
                                            .addComponent(btn_ptSettings))
                                        .addGap(0, 81, Short.MAX_VALUE))))))
                    .addComponent(jLabel3)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cmb_rhythmVoices, javax.swing.GroupLayout.PREFERRED_SIZE, 317, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_clearChain, btn_ptSettings});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmb_rhythmVoices, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addGap(19, 19, 19)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_allTransforms)
                    .addComponent(lbl_transformList))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnl_arrows, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btn_clearChain)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_ptSettings)))
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane3))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(birdview_outPhrase, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btn_clearChainActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_clearChainActionPerformed
    {//GEN-HEADEREND:event_btn_clearChainActionPerformed
        updateUiValue(null);
    }//GEN-LAST:event_btn_clearChainActionPerformed

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
                list_transformChainModel.addElement(pt.getCopy());
            }
        }
    }//GEN-LAST:event_list_availableTransformsMouseClicked

    private void btn_ptSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_ptSettingsActionPerformed
    {//GEN-HEADEREND:event_btn_ptSettingsActionPerformed
        var pt = list_transformChain.getSelectedValue();
        assert pt != null && pt.hasUserSettings();

        // Listen to properties changes while dialog is shown.
        PropertyChangeListener listener = e ->
        {
            updateUI(uiValue);
            fireUiValueChanged();
        };
        pt.getProperties().addPropertyChangeListener(listener);

        // Save lastValue to make sure uiValue will be different from lastValue if a property is changed
        lastValue = new RP_SYS_PhraseTransformValue(uiValue);

        // Show the dialog
        pt.showUserSettingsDialog(btn_ptSettings);

        // Stop listening
        pt.getProperties().removePropertyChangeListener(listener);
    }//GEN-LAST:event_btn_ptSettingsActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.phrase.api.ui.PhraseBirdsEyeViewComponent birdview_outPhrase;
    private javax.swing.JButton btn_clearChain;
    private javax.swing.JButton btn_ptSettings;
    private javax.swing.JComboBox<RhythmVoice> cmb_rhythmVoices;
    private org.jjazz.ui.utilities.api.HelpTextArea hlp_area;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lbl_allTransforms;
    private javax.swing.JLabel lbl_arrows;
    private javax.swing.JLabel lbl_transformList;
    private javax.swing.JList<PhraseTransform> list_availableTransforms;
    private javax.swing.JList<PhraseTransform> list_transformChain;
    private javax.swing.JPanel pnl_arrows;
    // End of variables declaration//GEN-END:variables


}
