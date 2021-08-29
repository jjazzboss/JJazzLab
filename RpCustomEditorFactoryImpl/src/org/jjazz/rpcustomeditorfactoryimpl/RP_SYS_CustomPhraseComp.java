package org.jjazz.rpcustomeditorfactoryimpl;

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import org.jjazz.rpcustomeditorfactoryimpl.spi.RealTimeRpEditorComponent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.plaf.LayerUI;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.StaticSongSession;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Variation;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhraseValue;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder.SongSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.utilities.api.StringMetrics;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;


/**
 * An editor panel for RP_SYS_CustomPhrase.
 */
public class RP_SYS_CustomPhraseComp extends RealTimeRpEditorComponent<RP_SYS_CustomPhraseValue>
{
    
    private static final Color PHRASE_COMP_FOCUSED_BORDER_COLOR = new Color(131, 42, 21);
    private static final Color PHRASE_COMP_CUSTOMIZED_FOREGROUND = new Color(255, 102, 102);
    private static final Color PHRASE_COMP_FOREGROUND = new Color(102, 153, 255);
    private final RP_SYS_CustomPhrase rp;
    private RP_SYS_CustomPhraseValue lastValue;
    private RP_SYS_CustomPhraseValue uiValue;
    private SongContext songContext;
    private SongPart songPart;
    private FloatRange songPartBeatRange;
    private TextOverlayLayerUI overlayLayerUI;
    private Map<RhythmVoice, Phrase> mapRvPhrase;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_CustomPhraseComp.class.getSimpleName());
    
    public RP_SYS_CustomPhraseComp(RP_SYS_CustomPhrase rp)
    {
        this.rp = rp;
        
        initComponents();
        
        list_rhythmVoices.setCellRenderer(new RhythmVoiceRenderer());

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
        return ResUtil.getString(getClass(), "RP_SYS_CustomPhrasePanel.DialogTitle");
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
        lbl_rhythm.setText(r.getName() + strVariation);
        lbl_phraseInfo.setText(songPart.getName() + " [" + (songPart.getBarRange().from + 1) + ";" + (songPart.getBarRange().to + 1) + "]");


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


            // Refresh the birdview
            list_rhythmVoicesValueChanged(null);
            
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
        if (mapRvPhrase == null || lastValue == null)
        {
            return null;
        }
        Phrase p = isCustomizedPhrase(rv) ? lastValue.getPhrase(rv) : mapRvPhrase.get(rv);
        return p;
    }
    
    private synchronized void setMapRvPhrase(Map<RhythmVoice, Phrase> map)
    {
        mapRvPhrase = map;
    }
    
    private void addCustomizedPhrase(RhythmVoice rv, Phrase p)
    {
        uiValue = uiValue.getCopyPlus(rv, p);
        fireUiValueChanged();
    }
    
    private void removeCustomizedPhrase(RhythmVoice rv)
    {
        uiValue = uiValue.getCopyMinus(rv);
        fireUiValueChanged();
    }
    
    private void fireUiValueChanged()
    {
        firePropertyChange(PROP_EDITED_RP_VALUE, lastValue, uiValue);
        lastValue = uiValue;
        refreshUI();
        
    }
    
    private boolean isCustomizedPhrase(RhythmVoice rv)
    {
        return uiValue.getRhythmVoices().contains(rv);
    }
    
    private void refreshUI()
    {
        RhythmVoice rv = getCurrentRhythmVoice();
        if (rv != null)
        {
            Phrase p = getPhrase(rv);
            if (p != null)
            {
                // Computed phrases are shifted to start at 0.
                FloatRange beatRange0 = songPartBeatRange.getTransformed(-songPartBeatRange.from, -songPartBeatRange.from);
                birdViewComponent.setModel(p, songPart.getRhythm().getTimeSignature(), beatRange0);
                birdViewComponent.setForeground(isCustomizedPhrase(rv) ? PHRASE_COMP_CUSTOMIZED_FOREGROUND : PHRASE_COMP_FOREGROUND);
            }
            btn_remove.setEnabled(p != null && isCustomizedPhrase(rv));
            btn_edit.setEnabled(true);
        } else
        {
            btn_remove.setEnabled(false);
            btn_edit.setEnabled(false);
        }
    }

    /**
     * Create a song sequence for the current song part ready to be edited as a Midi file by user.
     * <p>
     * Get rid of everything which is not useful for user.
     *
     * @return
     */
    private SongSequence buildSequence() throws MusicGenerationException
    {
        var songBuilder = new SongSequenceBuilder(songContext);
        var songSequence = songBuilder.buildAll(false);     // throws MusicGenerationException

        
        Sequence sequence = songSequence.sequence;
        MidiMix midiMix = songContext.getMidiMix();
        Track[] tracks = sequence.getTracks();
        Track track0 = tracks[0];


        // Clean track0 from JJazzLab internal Midi events
        int i = 0;
        while (i < track0.size())
        {
            MidiEvent me = track0.get(i);
            MidiMessage mm = me.getMessage();
            if (mm instanceof ShortMessage)
            {
                ShortMessage sm = (ShortMessage) mm;
                if (sm.getCommand() == ShortMessage.CONTROL_CHANGE && sm.getData1() == MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR)
                {
                    track0.remove(me);
                    i--;
                }
            }
            i++;
        }


        // Add markers at each chord symbol position
        SongStructure ss = songContext.getSong().getSongStructure();
        for (SongPart spt : ss.getSongParts())
        {
            CLI_Section section = spt.getParentSection();
            for (CLI_ChordSymbol cliCs : songContext.getSong().getChordLeadSheet().getItems(section, CLI_ChordSymbol.class))
            {
                Position absPos = ss.getSptItemPosition(spt, cliCs);
                float posInBeats = ss.getPositionInNaturalBeats(absPos.getBar()) + absPos.getBeat();
                long tickPos = Math.round(posInBeats * MidiConst.PPQ_RESOLUTION);
                MidiEvent me = new MidiEvent(MidiUtilities.getMarkerMetaMessage(cliCs.getData().getName()), tickPos);
                track0.add(me);
            }
        }


        // Add initial tempo
        int tempo = songContext.getSong().getTempo();
        SongPart spt0 = songContext.getSongParts().get(0);
        RP_SYS_TempoFactor rp = RP_SYS_TempoFactor.getTempoFactorRp(spt0.getRhythm());
        int tempoFactor = -1;
        if (rp != null)
        {
            tempoFactor = spt0.getRPValue(rp);
            tempo = Math.round(tempoFactor / 100f * tempo);
        }
        MidiEvent me = new MidiEvent(MidiUtilities.getTempoMessage(0, tempo), 0);
        track0.add(me);


        // Add XX mode ON initialization message
        OutputSynth os = OutputSynthManager.getInstance().getOutputSynth();
        SysexMessage sxm = null;
        switch (os.getSendModeOnUponPlay())
        {
            case GM:
                sxm = MidiUtilities.getGmModeOnSysExMessage();
                break;
            case GM2:
                sxm = MidiUtilities.getGm2ModeOnSysExMessage();
                break;
            case GS:
                sxm = MidiUtilities.getGsModeOnSysExMessage();
                break;
            case XG:
                sxm = MidiUtilities.getXgModeOnSysExMessage();
                break;
            default:
            // Nothing
        }
        if (sxm != null)
        {
            me = new MidiEvent(sxm, 0);
            track0.add(me);
        }


        // For each RhythmVoice :
        // - reset all controllers
        // - add instruments initialization messages for each track
        for (RhythmVoice rv : songSequence.mapRvTrackId.keySet())
        {
            Track track = tracks[songSequence.mapRvTrackId.get(rv)];
            int channel = songContext.getMidiMix().getChannel(rv);

            // Reset all controllers
            MidiMessage mmReset = MidiUtilities.getResetAllControllersMessage(channel);
            me = new MidiEvent(mmReset, 0);
            track.add(me);

            // Instrument + volume + pan etc.
            InstrumentMix insMix = midiMix.getInstrumentMixFromKey(rv);
            for (MidiMessage mm : insMix.getAllMidiMessages(channel))
            {
                me = new MidiEvent(mm, 0);
                track.add(me);
            }
        }
        
        return songSequence;
    }
    
    private void editCurrentPhrase()
    {
        // Create the temp file
        File midiTempFile;
        try
        {
            midiTempFile = File.createTempFile("jjazzTmpFile", ".mid");
            midiTempFile.deleteOnExit();
        } catch (IOException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Build the sequence
        SongSequence songSequence;
        int nbTracks=-1;
        try
        {
            songSequence = buildSequence();
            nbTracks = songSequence.sequence.getTracks().length;
        } catch (MusicGenerationException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Write the midi file
        try
        {
            MidiSystem.write(songSequence.sequence, 1, midiTempFile);
        } catch (IOException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Activate the overlay to display a message while external editor is active
        String msg = ResUtil.getString(getClass(), "RP_SYS_CustomPhrasePanel.WaitForEditorQuit");
        overlayLayerUI.setText(msg);


        // Start the midi editor AFTER overlay update is finished
        Runnable r = () ->
        {
            
            try
            {
                JJazzMidiSystem.getInstance().editMidiFileWithExternalEditor(midiTempFile); // Blocks until editor quits
            } catch (IOException ex)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            } finally
            {
                LOGGER.info("editCurrentPhrase() resuming from external Midi editing");
                overlayLayerUI.setText(null);
            }
        };
        SwingUtilities.invokeLater(r);


        // Reload file
        Sequence newSequence=null;
        try
        {
            newSequence = MidiSystem.getSequence(midiTempFile);
        } catch (IOException | InvalidMidiDataException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Extract the phrases
        Track tracks[] = newSequence.getTracks();
        if (tracks.length != songSequence.sequence.getTracks().length)
        {
            String msg = ResUtil
                        NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }
        
        
        
        
        LOGGER.severe("editCurrentPhrase() sequence=" + MidiUtilities.toString(sequence));
    }
    
    private int getCurrentChannel()
    {
        return songContext.getMidiMix().getChannel(getCurrentRhythmVoice());
    }

    /**
     *
     * @return Can be null.
     */
    private RhythmVoice getCurrentRhythmVoice()
    {
        return list_rhythmVoices.getSelectedValue();
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
        lbl_rhythm = new javax.swing.JLabel();
        birdViewComponent = new org.jjazz.phrase.api.ui.PhraseBirdView();
        lbl_phraseInfo = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.api.HelpTextArea();
        btn_remove = new org.jjazz.ui.utilities.api.SmallFlatDarkLafButton();
        btn_edit = new org.jjazz.ui.utilities.api.SmallFlatDarkLafButton();

        setPreferredSize(new java.awt.Dimension(500, 200));
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

        lbl_rhythm.setFont(lbl_rhythm.getFont().deriveFont(lbl_rhythm.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_rhythm, "Rhythm"); // NOI18N
        lbl_rhythm.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseComp.class, "RP_SYS_CustomPhraseComp.lbl_rhythm.toolTipText")); // NOI18N

        birdViewComponent.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        birdViewComponent.setForeground(new java.awt.Color(102, 153, 255));
        birdViewComponent.setToolTipText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseComp.class, "RP_SYS_CustomPhraseComp.birdViewComponent.toolTipText")); // NOI18N
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

        jScrollPane2.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(3);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(RP_SYS_CustomPhraseComp.class, "RP_SYS_CustomPhraseComp.helpTextArea1.text")); // NOI18N
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
                        .addComponent(lbl_rhythm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbl_phraseInfo))
                    .addComponent(birdViewComponent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnl_overlayLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_remove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE))
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
                        .addComponent(lbl_rhythm)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_overlayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 156, Short.MAX_VALUE)
                    .addGroup(pnl_overlayLayout.createSequentialGroup()
                        .addComponent(birdViewComponent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_remove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2)))
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


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.phrase.api.ui.PhraseBirdView birdViewComponent;
    private org.jjazz.ui.utilities.api.SmallFlatDarkLafButton btn_edit;
    private org.jjazz.ui.utilities.api.SmallFlatDarkLafButton btn_remove;
    private org.jjazz.ui.utilities.api.HelpTextArea helpTextArea1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lbl_phraseInfo;
    private javax.swing.JLabel lbl_rhythm;
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
            if (uiValue != null && uiValue.getRhythmVoices().contains(rv))
            {
                f = f.deriveFont(Font.BOLD);
                tooltip += " (customized)";
            }
            lbl.setFont(f);
            lbl.setToolTipText(tooltip);
            return lbl;
        }
    }

    /**
     * A LayerUI that display a centered text over the view component using a semi-transparent background.
     */
    private class TextOverlayLayerUI extends LayerUI<JComponent>
    {
        
        String text;

        /**
         * Create the overlay in invisible state (text is null).
         */
        TextOverlayLayerUI()
        {
            this(null);
        }

        /**
         * Create the overlay with the specified text.
         */
        TextOverlayLayerUI(String text)
        {
            setText(text);
        }

        /**
         * The text to be displayed on a semi-transparent background over the view component.
         *
         * @param text If null nothing is shown (overlay is invisible).
         */
        public final void setText(String text)
        {
            this.text = text;
            repaint();
        }

        /**
         * The displayed text.
         *
         * @return If null it means the overlay is invisible.
         */
        public String getText()
        {
            return text;
        }
        
        @Override
        public void paint(Graphics g, JComponent c)
        {
            super.paint(g, c);
            
            if (text == null)
            {
                return;
            }
            
            Graphics2D g2 = (Graphics2D) g.create();
            
            int w = c.getWidth();
            int h = c.getHeight();


            // Semi-transparent background
            Color bg = c.getBackground();
            Color newBg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 220);
            g2.setColor(newBg);
            g2.fillRect(0, 0, w, h);
            g2.setColor(Color.WHITE);


            // Write text
            g2.setFont(g2.getFont().deriveFont(Font.BOLD));
            StringMetrics sm = new StringMetrics(g2, g2.getFont());
            var bounds = sm.getLogicalBoundsNoLeading(text);
            int x = (w - (int) bounds.getWidth()) / 2;
            int y = (h - (int) bounds.getHeight()) / 2;
            y -= (int) bounds.getY();      // bounds.getY() is <0 because baseline coordinates!
            g2.drawString(text, x, y);
            
            g2.dispose();
        }
    }
    
}
