package org.jjazz.rpcustomeditorfactoryimpl;

import org.jjazz.ui.utilities.api.MidiFileDragInTransferHandler;
import org.jjazz.ui.utilities.api.FileTransferable;
import org.jjazz.ui.utilities.api.TextOverlayLayerUI;
import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import org.jjazz.rpcustomeditorfactoryimpl.spi.RealTimeRpEditorComponent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.StaticSongSession;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Variation;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhraseValue;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder.SongSequence;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;


/**
 * An editor panel for RP_SYS_CustomPhrase.
 */
public class RP_SYS_CustomPhraseComp extends RealTimeRpEditorComponent<RP_SYS_CustomPhraseValue>
{

    private static final float PHRASE_COMPARE_BEAT_WINDOW = 0.01f;
    private static final Color PHRASE_COMP_FOCUSED_BORDER_COLOR = new Color(131, 42, 21);
    private static final Color PHRASE_COMP_CUSTOMIZED_FOREGROUND = new Color(255, 102, 102);
    private static final Color PHRASE_COMP_FOREGROUND = new Color(102, 153, 255);
    private final RP_SYS_CustomPhrase rp;
    private RP_SYS_CustomPhraseValue lastValue;
    private RP_SYS_CustomPhraseValue uiValue;
    private SongContext songContext;
    private SongPart songPart;
    private FloatRange songPartBeatRange;
    private final TextOverlayLayerUI overlayLayerUI;
    private Map<RhythmVoice, Phrase> mapRvPhrase;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_CustomPhraseComp.class.getSimpleName());

    public RP_SYS_CustomPhraseComp(RP_SYS_CustomPhrase rp)
    {
        this.rp = rp;

        initComponents();

        list_rhythmVoices.setCellRenderer(new RhythmVoiceRenderer());

        // By default enable the drag in transfer handler
        setAllTransferHandlers(new MidiFileDragInTransferHandlerImpl());


        // Remove and replace by a JLayer  (this way we can use pnl_overlay in Netbeans form designer)
        remove(pnl_overlay);
        overlayLayerUI = new TextOverlayLayerUI();
        add(new JLayer(pnl_overlay, overlayLayerUI));

    }


    @Override
    public RP_SYS_CustomPhrase getRhythmParameter()
    {
        return rp;
    }

    @Override
    public String getTitle()
    {
        String txt = "\"" + songPart.getName() + "\" - bars " + (songPart.getBarRange().from + 1) + "..." + (songPart.getBarRange().to + 1);
        return ResUtil.getString(getClass(), "RP_SYS_CustomPhraseComp.DialogTitle", txt);
    }

    @Override
    public boolean isResizable()
    {
        return true;
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);

        // Update our UI
        list_rhythmVoices.setEnabled(b);
        btn_edit.setEnabled(b);
        btn_remove.setEnabled(b);
        birdViewComponent.setEnabled(b);
    }


    @Override
    public void preset(RP_SYS_CustomPhraseValue rpValue, SongContext sgContext)
    {
        checkNotNull(rpValue);
        checkNotNull(sgContext);


        songContext = sgContext;
        songPart = songContext.getSongParts().get(0);
        songPartBeatRange = songContext.getSptBeatRange(songPart);
        setMapRvPhrase(null);


        LOGGER.info("preset() -- rpValue=" + rpValue + " songPart=" + songPart);


        list_rhythmVoices.setListData(rpValue.getRhythm().getRhythmVoices().toArray(new RhythmVoice[0]));
        list_rhythmVoices.setSelectedIndex(0);
        Rhythm r = songPart.getRhythm();
        var rpVariation = RP_STD_Variation.getVariationRp(r);
        String strVariation = rpVariation == null ? "" : "/" + songPart.getRPValue(rpVariation).toString();
        lbl_phraseInfo.setText(r.getName() + strVariation);


        // Start a task to generate the phrases 
        Runnable task = () ->
        {
            StaticSongSession tmpSession = StaticSongSession.getSession(songContext, false, false, false, false, 0, null);
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
            setMapRvPhrase(tmpSession.getRvPhraseMap());
            tmpSession.close();


        };

        new Thread(task).start();


        setEditedRpValue(rpValue);
    }

    @Override
    public void setEditedRpValue(RP_SYS_CustomPhraseValue rpValue)
    {
        uiValue = rpValue;
        lastValue = uiValue;


        // Update UI
        list_rhythmVoicesValueChanged(null);
    }

    @Override
    public RP_SYS_CustomPhraseValue getEditedRpValue()
    {
        return uiValue;
    }

    @Override
    public void cleanup()
    {
    }

    @Override
    public String toString()
    {
        return "RP_SYS_CustomPhraseComp[rp=" + rp + "]";
    }

    // ===================================================================================
    // Private methods
    // ===================================================================================
    /**
     *
     * @param rv
     * @return Can be null!
     */
    private synchronized Phrase getPhrase(RhythmVoice rv)
    {
        if (mapRvPhrase == null || uiValue == null)
        {
            return null;
        }
        Phrase p = isCustomizedPhrase(rv) ? uiValue.getCustomizedPhrase(rv) : mapRvPhrase.get(rv);
        return p;
    }

    private synchronized void setMapRvPhrase(Map<RhythmVoice, Phrase> map)
    {
        mapRvPhrase = map;

        // Refresh the birdview
        list_rhythmVoicesValueChanged(null);
    }

    private void addCustomizedPhrase(RhythmVoice rv, SizedPhrase sp)
    {
        uiValue = uiValue.getCopyPlus(rv, sp);
        fireUiValueChanged();
        forceListRepaint();
    }

    private void removeCustomizedPhrase(RhythmVoice rv)
    {
        uiValue = uiValue.getCopyMinus(rv);
        fireUiValueChanged();
        forceListRepaint();
    }

    private void forceListRepaint()
    {
        list_rhythmVoices.repaint(list_rhythmVoices.getCellBounds(0, songPart.getRhythm().getRhythmVoices().size() - 1));
    }

    private void fireUiValueChanged()
    {
        firePropertyChange(PROP_EDITED_RP_VALUE, lastValue, uiValue);
        lastValue = uiValue;
        refreshUI();

    }

    private boolean isCustomizedPhrase(RhythmVoice rv)
    {
        return uiValue == null ? false : uiValue.getCustomizedRhythmVoices().contains(rv);
    }

    private void refreshUI()
    {
        RhythmVoice rv = getCurrentRhythmVoice();
        if (rv != null)
        {
            Phrase p = getPhrase(rv);
            boolean isCustom = isCustomizedPhrase(rv);
            if (p != null)
            {
                // Computed phrases are shifted to start at 0.
                FloatRange beatRange0 = songPartBeatRange.getTransformed(-songPartBeatRange.from);
                birdViewComponent.setModel(p, songPart.getRhythm().getTimeSignature(), beatRange0);
                birdViewComponent.setForeground(isCustom ? PHRASE_COMP_CUSTOMIZED_FOREGROUND : PHRASE_COMP_FOREGROUND);
            }
            btn_remove.setEnabled(isCustom);
            btn_edit.setEnabled(true);
            String txt = isCustom ? " [" + ResUtil.getString(getClass(), "RP_SYS_CustomPhraseComp.Customized") + "]" : "";
            lbl_rhythmVoice.setText(rv.getName() + txt);
            MidiMix mm = songContext.getMidiMix();
            int channel = mm.getChannel(rv) + 1;
            lbl_rhythmVoice.setToolTipText("Midi channel " + channel);
        } else
        {
            btn_remove.setEnabled(false);
            btn_edit.setEnabled(false);
            lbl_rhythmVoice.setText("");
        }
    }

    /**
     * Create a SongContext which uses the current RP_SYS_CustomPhraseValue.
     *
     * @return
     */
    private SongContext buildWorkContext()
    {
        // Get a song copy which uses the edited RP value
        Song songCopy = SongFactory.getInstance().getCopy(songContext.getSong(), false);
        SongStructure ss = songCopy.getSongStructure();
        SongPart spt = ss.getSongPart(songContext.getBarRange().from);

        // Apply the RP value
        ss.setRhythmParameterValue(spt, rp, uiValue);

        // Create the new context
        SongContext res = new SongContext(songCopy, songContext.getMidiMix(), songContext.getBarRange());
        return res;
    }

    /**
     * Import phrases from the Midi file.
     * <p>
     * <p>
     * Notify user if problems occur.
     *
     * @param midiFile
     * @return True if import was successful
     */
    private boolean importEditedMidiFile(File midiFile)
    {
        // Load file into a sequence
        Sequence sequence;
        try
        {
            sequence = MidiSystem.getSequence(midiFile);
        } catch (IOException | InvalidMidiDataException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return false;
        }

        // LOGGER.severe("importMidiFile() importSequence=" + MidiUtilities.toString(importSequence));

        // Get one phrase per channel
        Track[] tracks = sequence.getTracks();
        List<Phrase> phrases = Phrase.getPhrases(tracks);


        boolean contentFound = false;
        List<RhythmVoice> impactedRvs = new ArrayList<>();


        // Check which phrases are relevant and if they have changed
        MidiMix mm = songContext.getMidiMix();
        for (Phrase pNew : phrases)
        {
            RhythmVoice rv = mm.getRhythmVoice(pNew.getChannel());
            if (rv != null && !(rv instanceof UserRhythmVoice))
            {
                contentFound = true;
                Phrase pOld = getPhrase(rv);
                if (!pNew.equalsLoosePosition(pOld, PHRASE_COMPARE_BEAT_WINDOW))
                {
//                    LOGGER.info("importMidiFile() setting custom phrase for rv=" + rv);
//                    LOGGER.info("importMidiFile() pOld=" + pOld);
//                    LOGGER.info("importMidiFile() pNew=" + pNew);
                    SizedPhrase sp = new SizedPhrase(pNew.getChannel(),  songContext.getBeatRange(), songPart.getRhythm().getTimeSignature());
                    sp.add(pNew);
                    addCustomizedPhrase(rv, sp);
                    impactedRvs.add(rv);
                }
            }
        }


        // Notify user
        if (!contentFound)
        {
            String msg = ResUtil.getString(getClass(), "RP_SYS_CustomPhraseComp.NoContent", midiFile.getName());
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            LOGGER.info("importMidiFile() No relevant Midi notes found in file " + midiFile.getAbsolutePath());

        } else if (impactedRvs.isEmpty())
        {
            String msg = ResUtil.getString(getClass(), "RP_SYS_CustomPhraseComp.NothingImported", midiFile.getName());
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            LOGGER.info("importMidiFile() No new phrase found in file " + midiFile.getAbsolutePath());

        } else
        {
            // We customized at least 1 phrase
            List<String> strs = impactedRvs.stream()
                    .map(rv -> rv.getName())
                    .collect(Collectors.toList());
            String strRvs = Joiner.on(",").join(strs);
            String msg = ResUtil.getString(getClass(), "RP_SYS_CustomPhraseComp.CustomizedRvs", strRvs);
            StatusDisplayer.getDefault().setStatusText(msg);
            LOGGER.info("importMidiFile() Successfully set custom phrases for " + strRvs + " from Midi file " + midiFile.getAbsolutePath());
        }

        return true;
    }


    /**
     * Build an exportable sequence with current custom phrase and store it in a temp file.
     *
     * @return The generated Midi temporary file.
     * @throws IOException
     * @throws MusicGenerationException
     */
    private File exportSequenceToMidiTempFile() throws IOException, MusicGenerationException
    {
        LOGGER.fine("exportSequenceToMidiTempFile() -- ");
        // Create the temp file
        File midiFile = File.createTempFile("JJazzCustomPhrase", ".mid"); // throws IOException
        midiFile.deleteOnExit();


        // Build the sequence
        SongContext workContext = buildWorkContext();
        SongSequence songSequence = new SongSequenceBuilder(workContext).buildExportableSequence(true, true); // throws MusicGenerationException


        // Update the reference mapRvPhrases because the generation of some rhythms (e.g. from the YamJJazz engine) might use random items.
        // If we don't update them, some reference phrases might appear customized when we go back from the external edit, even if nothing
        // was modified in the editor.
        setMapRvPhrase(songSequence.mapRvPhrase);


        // Write the midi file     
        MidiSystem.write(songSequence.sequence, 1, midiFile);   // throws IOException

        return midiFile;
    }

    private void editCurrentPhrase()
    {
        File midiFile;
        try
        {
            // Build and store sequence
            midiFile = exportSequenceToMidiTempFile();
        } catch (IOException | MusicGenerationException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            LOGGER.warning("editCurrentPhrase() Can't create Midi file ex=" + ex.getMessage());
            return;
        }


        // Activate the overlay to display a message while external editor is active       
        String msg = ResUtil.getString(getClass(), "RP_SYS_CustomPhraseComp.WaitForEditorQuit");
        overlayLayerUI.setText(msg);
        repaint();


        // Use SwingUtilites.invokeLater() to make sure this is executed AFTER the repaint task from previous "overlayLayerUI.setText(msg)"
        Runnable r = () ->
        {
            try
            {
                // Start the midi editor
                LOGGER.log(Level.INFO, "editCurrentPhrase() Lanching external Midi editor with file {0}", midiFile.getAbsolutePath());
                JJazzMidiSystem.getInstance().editMidiFileWithExternalEditor(midiFile); // Blocks until editor quits
            } catch (IOException ex)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                LOGGER.warning("editCurrentPhrase() Can't launch external Midi editor. ex=" + ex.getMessage());
                return;
            } finally
            {
                LOGGER.info("editCurrentPhrase() resuming from external Midi editing");
                overlayLayerUI.setText(null);
                repaint();
            }


            // Extract the custom phrases and add them
            importEditedMidiFile(midiFile);
        };
        SwingUtilities.invokeLater(r);

    }


    /**
     *
     * @return Can be null.
     */
    private RhythmVoice getCurrentRhythmVoice()
    {
        return list_rhythmVoices.getSelectedValue();
    }


    private void setAllTransferHandlers(TransferHandler th)
    {
        setTransferHandler(th);
        helpTextArea1.setTransferHandler(th);
    }


    private void startDragOut(MouseEvent evt)
    {
        if (SwingUtilities.isLeftMouseButton(evt))
        {
            // Use the drag export transfer handler
            setAllTransferHandlers(new MidiFileDragOutTransferHandler());
            getTransferHandler().exportAsDrag(this, evt, TransferHandler.COPY);
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

        pnl_overlay = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list_rhythmVoices = new javax.swing.JList<>();
        lbl_rhythmVoice = new javax.swing.JLabel();
        birdViewComponent = new org.jjazz.phrase.api.ui.PhraseBirdView();
        lbl_phraseInfo = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.api.HelpTextArea();
        btn_remove = new org.jjazz.ui.utilities.api.SmallFlatDarkLafButton();
        btn_edit = new org.jjazz.ui.utilities.api.SmallFlatDarkLafButton();

        setPreferredSize(new java.awt.Dimension(500, 200));
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                formMouseDragged(evt);
            }
        });
        setLayout(new java.awt.BorderLayout());

        list_rhythmVoices.setFont(list_rhythmVoices.getFont().deriveFont(list_rhythmVoices.getFont().getSize()-1f));
        list_rhythmVoices.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_rhythmVoices.setVisibleRowCount(6);
        list_rhythmVoices.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_rhythmVoicesValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(list_rhythmVoices);

        lbl_rhythmVoice.setFont(lbl_rhythmVoice.getFont().deriveFont(lbl_rhythmVoice.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_rhythmVoice, "Rhythm"); // NOI18N
        lbl_rhythmVoice.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                lbl_rhythmVoiceMouseDragged(evt);
            }
        });

        birdViewComponent.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        birdViewComponent.setForeground(new java.awt.Color(102, 153, 255));
        birdViewComponent.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseComp.class, "RP_SYS_CustomPhraseComp.birdViewComponent.toolTipText")); // NOI18N
        birdViewComponent.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                birdViewComponentMouseDragged(evt);
            }
        });
        birdViewComponent.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusGained(java.awt.event.FocusEvent evt)
            {
                birdViewComponentFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt)
            {
                birdViewComponentFocusLost(evt);
            }
        });
        birdViewComponent.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mousePressed(java.awt.event.MouseEvent evt)
            {
                birdViewComponentMousePressed(evt);
            }
        });

        javax.swing.GroupLayout birdViewComponentLayout = new javax.swing.GroupLayout(birdViewComponent);
        birdViewComponent.setLayout(birdViewComponentLayout);
        birdViewComponentLayout.setHorizontalGroup(
            birdViewComponentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        birdViewComponentLayout.setVerticalGroup(
            birdViewComponentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 46, Short.MAX_VALUE)
        );

        lbl_phraseInfo.setFont(lbl_phraseInfo.getFont().deriveFont(lbl_phraseInfo.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_phraseInfo, "bars 2 - 4"); // NOI18N
        lbl_phraseInfo.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseComp.class, "RP_SYS_CustomPhraseComp.lbl_phraseInfo.toolTipText")); // NOI18N
        lbl_phraseInfo.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                lbl_phraseInfoMouseDragged(evt);
            }
        });

        jScrollPane2.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(3);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseComp.class, "RP_SYS_CustomPhraseComp.helpTextArea1.text")); // NOI18N
        helpTextArea1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                helpTextArea1MouseDragged(evt);
            }
        });
        jScrollPane2.setViewportView(helpTextArea1);

        org.openide.awt.Mnemonics.setLocalizedText(btn_remove, org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseComp.class, "RP_SYS_CustomPhraseComp.btn_remove.text")); // NOI18N
        btn_remove.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseComp.class, "RP_SYS_CustomPhraseComp.btn_remove.toolTipText")); // NOI18N
        btn_remove.setFont(btn_remove.getFont().deriveFont(btn_remove.getFont().getSize()-2f));
        btn_remove.setMargin(new java.awt.Insets(2, 7, 2, 7));
        btn_remove.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_removeActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_edit, org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseComp.class, "RP_SYS_CustomPhraseComp.btn_edit.text")); // NOI18N
        btn_edit.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseComp.class, "RP_SYS_CustomPhraseComp.btn_edit.toolTipText")); // NOI18N
        btn_edit.setFont(btn_edit.getFont().deriveFont(btn_edit.getFont().getSize()-2f));
        btn_edit.setMargin(new java.awt.Insets(2, 7, 2, 7));
        btn_edit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_editActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_overlayLayout = new javax.swing.GroupLayout(pnl_overlay);
        pnl_overlay.setLayout(pnl_overlayLayout);
        pnl_overlayLayout.setHorizontalGroup(
            pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_overlayLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_edit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_overlayLayout.createSequentialGroup()
                        .addComponent(lbl_rhythmVoice)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbl_phraseInfo))
                    .addComponent(birdViewComponent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnl_overlayLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_remove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnl_overlayLayout.setVerticalGroup(
            pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_overlayLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_edit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbl_phraseInfo)
                        .addComponent(lbl_rhythmVoice)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE)
                    .addGroup(pnl_overlayLayout.createSequentialGroup()
                        .addComponent(birdViewComponent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_remove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 102, Short.MAX_VALUE)))
                .addContainerGap())
        );

        add(pnl_overlay, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void list_rhythmVoicesValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_rhythmVoicesValueChanged
    {//GEN-HEADEREND:event_list_rhythmVoicesValueChanged
        if (evt != null && evt.getValueIsAdjusting())
        {
            return;
        }

        refreshUI();

    }//GEN-LAST:event_list_rhythmVoicesValueChanged

    private void birdViewComponentFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_birdViewComponentFocusGained
    {//GEN-HEADEREND:event_birdViewComponentFocusGained
        birdViewComponent.setBorder(BorderFactory.createEtchedBorder(null, PHRASE_COMP_FOCUSED_BORDER_COLOR));
    }//GEN-LAST:event_birdViewComponentFocusGained

    private void birdViewComponentFocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_birdViewComponentFocusLost
    {//GEN-HEADEREND:event_birdViewComponentFocusLost
        birdViewComponent.setBorder(BorderFactory.createEtchedBorder(null, null));
    }//GEN-LAST:event_birdViewComponentFocusLost

    private void birdViewComponentMousePressed(java.awt.event.MouseEvent evt)//GEN-FIRST:event_birdViewComponentMousePressed
    {//GEN-HEADEREND:event_birdViewComponentMousePressed
        birdViewComponent.requestFocusInWindow();
    }//GEN-LAST:event_birdViewComponentMousePressed

    private void btn_editActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_editActionPerformed
    {//GEN-HEADEREND:event_btn_editActionPerformed
        editCurrentPhrase();
    }//GEN-LAST:event_btn_editActionPerformed

    private void btn_removeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_removeActionPerformed
    {//GEN-HEADEREND:event_btn_removeActionPerformed
        removeCustomizedPhrase(getCurrentRhythmVoice());
    }//GEN-LAST:event_btn_removeActionPerformed

    private void formMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_formMouseDragged
    {//GEN-HEADEREND:event_formMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_formMouseDragged

    private void lbl_rhythmVoiceMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_rhythmVoiceMouseDragged
    {//GEN-HEADEREND:event_lbl_rhythmVoiceMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_lbl_rhythmVoiceMouseDragged

    private void birdViewComponentMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_birdViewComponentMouseDragged
    {//GEN-HEADEREND:event_birdViewComponentMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_birdViewComponentMouseDragged

    private void helpTextArea1MouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_helpTextArea1MouseDragged
    {//GEN-HEADEREND:event_helpTextArea1MouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_helpTextArea1MouseDragged

    private void lbl_phraseInfoMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lbl_phraseInfoMouseDragged
    {//GEN-HEADEREND:event_lbl_phraseInfoMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_lbl_phraseInfoMouseDragged



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.phrase.api.ui.PhraseBirdView birdViewComponent;
    private org.jjazz.ui.utilities.api.SmallFlatDarkLafButton btn_edit;
    private org.jjazz.ui.utilities.api.SmallFlatDarkLafButton btn_remove;
    private org.jjazz.ui.utilities.api.HelpTextArea helpTextArea1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
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
            if (songContext != null)
            {
                channel = songContext.getMidiMix().getChannel(rv) + 1;
            }
            lbl.setText(rv.getName() + " [" + channel + "]");
            Font f = lbl.getFont();
            String tooltip = "Midi channel " + channel;
            if (uiValue != null && uiValue.getCustomizedRhythmVoices().contains(rv))
            {
                f = f.deriveFont(Font.ITALIC);
                tooltip = "Customized - " + tooltip;
            }
            lbl.setFont(f);
            lbl.setToolTipText(tooltip);
            return lbl;
        }
    }


    private class MidiFileDragInTransferHandlerImpl extends MidiFileDragInTransferHandler
    {

        @Override
        protected boolean isImportEnabled()
        {
            return isEnabled();
        }

        @Override
        protected boolean importMidiFile(File midiFile)
        {
            return importEditedMidiFile(midiFile);
        }

    }


    /**
     * Our drag'n drop support to export Midi files when dragging from this component.
     */
    private class MidiFileDragOutTransferHandler extends TransferHandler
    {

        @Override
        public int getSourceActions(JComponent c)
        {
            LOGGER.fine("MidiFileDragOutTransferHandler.getSourceActions()  c=" + c);   //NOI18N
            return TransferHandler.COPY_OR_MOVE;
        }

        @Override
        public Transferable createTransferable(JComponent c)
        {
            LOGGER.fine("MidiFileDragOutTransferHandler.createTransferable()  c=" + c);   //NOI18N

            File midiFile = null;
            try
            {
                midiFile = exportSequenceToMidiTempFile();
            } catch (MusicGenerationException | IOException ex)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }

            List<File> data = midiFile == null ? null : Arrays.asList(midiFile);

            Transferable t = new FileTransferable(data);

            return t;
        }

        /**
         *
         * @param c
         * @param data
         * @param action
         */
        @Override
        protected void exportDone(JComponent c, Transferable data, int action)
        {
            // Will be called if drag was initiated from this handler
            LOGGER.fine("MidiFileDragOutTransferHandler.exportDone()  c=" + c + " data=" + data + " action=" + action);   //NOI18N
            // Restore the default handler
            setAllTransferHandlers(new MidiFileDragInTransferHandlerImpl());
        }

        /**
         * Overridden only to show a drag icon when dragging is still inside this component
         *
         * @param support
         * @return
         */
        @Override
        public boolean canImport(TransferHandler.TransferSupport support)
        {
            // Use copy drop icon
            support.setDropAction(COPY);
            return true;
        }

        /**
         * Do nothing if we drop on ourselves.
         *
         * @param support
         * @return
         */
        @Override
        public boolean importData(TransferHandler.TransferSupport support)
        {
            return false;
        }


    }


}
