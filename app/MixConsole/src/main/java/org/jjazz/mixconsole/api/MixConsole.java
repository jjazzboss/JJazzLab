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
package org.jjazz.mixconsole.api;

import org.jjazz.mixconsole.MixConsoleTransferHandler;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.undo.UndoManager;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Actions;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.jjazz.flatcomponents.api.FlatButton;
import org.jjazz.mixconsole.MixChannelPanel;
import org.jjazz.mixconsole.MixChannelPanelControllerImpl;
import org.jjazz.mixconsole.MixChannelPanelModelImpl;
import org.jjazz.mixconsole.PhraseViewerPanel;
import org.jjazz.mixconsole.MixConsoleLayoutManager;
import org.jjazz.rhythmstubs.api.RhythmStub;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.MenuBar;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;

/**
 * Edit song instruments parameters.
 * <p>
 * MixConsole's lookup can contain:<br>
 * - our ActionMap<br>
 * - the currently edited Song<br>
 * - the currently edited MidiMix<br>
 * <p>
 * The MixConsole Netbeans MenuBar is built from subfolders and actions available in the Actions/MixConsole/MenuBar layer folder.
 */
public class MixConsole extends JPanel implements PropertyChangeListener, ActionListener
{

    /**
     * The UI panels associated to a channel.
     */
    public record ChannelPanelSet(RhythmVoice rhythmVoice, MixChannelPanel mixChannelPanel, PhraseViewerPanel phraseViewerPanel)
            {

    }


    /**
     * Colors used to distinguish channels from different rhythms.
     */
    private static final Color[] CHANNEL_COLORS =
    {
        new Color(78, 235, 249), // cyan 
        new Color(254, 142, 39), // light orange
        new Color(157, 180, 71), // light green
        new Color(102, 102, 153), // blue purple
        new Color(255, 255, 153)  // pale yellow
    };
    private static final Color CHANNEL_COLOR_USER = new Color(192, 115, 243);       // Light purple

    private static Rhythm RHYTHM_ALL;
    private final InstanceContent instanceContent;
    private final Lookup lookup;
    private final Lookup.Result<Song> songLkpResult;
    private LookupListener songLkpListener;
    /**
     * The song currently edited by this editor.
     */
    private Song songModel;
    private MidiMix songMidiMix;
    // WeakHashMap for safety/robustness: normally not needed, we remove the song entry upon song closing
    private final WeakHashMap<Song, Rhythm> mapVisibleRhythm;
    private final MixConsoleSettings settings;
    private final MenuBar menuBar;
    private final TreeMap<Integer, ChannelPanelSet> tmapChannelPanelSets;
    private final MixConsoleLayoutManager layoutManager;
    private static final Logger LOGGER = Logger.getLogger(MixConsole.class.getSimpleName());


    public MixConsole(MixConsoleSettings settings)
    {
        if (settings == null)
        {
            throw new IllegalArgumentException("settings=" + settings);
        }

        tmapChannelPanelSets = new TreeMap<>();

        // Listen to settings change events
        this.settings = settings;
        settings.addPropertyChangeListener(this);

        // Listen to active song and midimix changes
        ActiveSongManager.getDefault().addPropertyListener(this);

        // Listen to closed song events to cleanup our data
        SongEditorManager.getDefault().addPropertyChangeListener(this);

        // A dummy rhythm used by the visible rhythms combobox when all song rhythms are visible
        RHYTHM_ALL = new RhythmStub(ResUtil.getString(getClass(), "MixConsole.CTL_DummyRhythmAll"), TimeSignature.FOUR_FOUR);

        mapVisibleRhythm = new WeakHashMap<>();

        // UI initialization
        initComponents();


        // Use our LayoutManager to arranger MixChannelPanels and their extensions
        layoutManager = new MixConsoleLayoutManager(this, true);
        panel_mixChannels.setLayout(layoutManager);


        // Our renderer to show visible rhythms
        cb_viewRhythms.setRenderer(new MyRenderer());


        // Connect to standard actions
        // fbtn_muteAll.setAction(Actions.forID("MixConsole", "org.jjazz.mixconsole.actions.mastermuteall"));   
        fbtn_panic.setAction(Actions.forID("MixConsole", "org.jjazz.mixconsole.actions.panic"));
        fbtn_switchAllMute.setAction(Actions.forID("MixConsole", "org.jjazz.mixconsole.actions.switchallmute"));
        fbtn_allSoloOff.setAction(Actions.forID("MixConsole", "org.jjazz.mixconsole.actions.allsolooff"));
        fbtn_addUserChannel.setAction(Actions.forID("MixConsole", "org.jjazz.mixconsole.actions.addusertrack"));


        // Prepare our MenuBar from specified folder in layer file 
        menuBar = buildMenuBar("Actions/MixConsole/MenuBar");


        // Filler to put Midi device on the right
        Box.Filler filler = new Box.Filler(new Dimension(0, 1), new Dimension(5000, 1), new Dimension(5000, 1));
        menuBar.add(filler);


        // Reuse menu font size
        Font menuFont = menuBar.getMenu(0).getItem(0).getFont();//.deriveFont(Font.ITALIC);
        MyOutDeviceButton outLabel = new MyOutDeviceButton();
        outLabel.setFont(menuFont);
        menuBar.add(outLabel);

        setJPanelMenuBar(this, panel_Main, menuBar);

        // By default not active
        updateActiveState(false);

        songLkpListener = (LookupEvent le) -> 
        {
            songPresenceChanged();
        };

        // Our general lookup : store our action map plus the currently edited song and a Savable object when needed
        instanceContent = new InstanceContent();
        instanceContent.add(getActionMap());
        lookup = new AbstractLookup(instanceContent);

        // Listen to RL_Editor selection in the global lookup        
        Lookup context = Utilities.actionsGlobalContext();

        songLkpResult = context.lookupResult(Song.class);
        songLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, songLkpListener, songLkpResult));
        songPresenceChanged();

        refreshUI();
    }

    public Lookup getLookup()
    {
        return this.lookup;
    }

    public void cleanup()
    {
        removeAllChannels();
        settings.removePropertyChangeListener(this);
        ActiveSongManager.getDefault().removePropertyListener(this);
        SongEditorManager.getDefault().removePropertyChangeListener(this);
        songLkpListener = null;
        resetModel();
    }

    /**
     * Get the visible rhythm.
     * <p>
     * If null all song rhythms are shown.
     *
     * @return Default value is null.
     */
    public Rhythm getVisibleRhythm()
    {
        return mapVisibleRhythm.get(songModel);
    }

    /**
     * Set the visible rhythm.
     *
     * @param r If null all song rhythms are shown.
     * @throws IllegalStateException If r is an AdaptedRhythm or if r does not belong to this song
     */
    public void setVisibleRhythm(Rhythm r)
    {
        if (songModel == null || r instanceof AdaptedRhythm || (r != null && !songModel.getSongStructure().getUniqueRhythms(true, false).contains(
                r)))
        {
            throw new IllegalStateException("songModel=" + songModel + " r=" + r);
        }
        Rhythm oldVisibleRhythm = getVisibleRhythm();
        if (r != oldVisibleRhythm)
        {
            mapVisibleRhythm.put(songModel, r);
            removeAllChannels();
            addVisibleChannels();
            cb_viewRhythms.setSelectedItem(r == null ? RHYTHM_ALL : r);
        }

    }

    /**
     * The current song edited by the MixConsole.
     *
     * @return Can be null.
     */
    public Song getSong()
    {
        return songModel;
    }

    /**
     * The MidiMix currently edited by the MixConsole.
     *
     * @return Can be null.
     */
    public MidiMix getMidiMix()
    {
        return songMidiMix;
    }

    public JJazzUndoManager getUndoManager()
    {
        return JJazzUndoManagerFinder.getDefault().get(songModel);
    }

    /**
     * A map which associates a ChannelPanelSet for each used Midi channel.
     *
     * @return
     */
    public TreeMap<Integer, ChannelPanelSet> getChannelPanelSets()
    {
        return tmapChannelPanelSets;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        panel_Main = new javax.swing.JPanel();
        panel_MasterControls = new javax.swing.JPanel();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 32767));
        cb_viewRhythms = new javax.swing.JComboBox<>();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        lbl_Master = new javax.swing.JLabel();
        masterHorizontalSlider1 = new org.jjazz.mixconsole.MasterVolumeSlider();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_switchAllMute = new org.jjazz.flatcomponents.api.FlatButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_allSoloOff = new org.jjazz.flatcomponents.api.FlatButton();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_panic = new org.jjazz.flatcomponents.api.FlatButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(15, 0), new java.awt.Dimension(15, 32767));
        fbtn_addUserChannel = new org.jjazz.flatcomponents.api.FlatButton();
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 32767));
        scrollPane_mixChannelsPanel = new javax.swing.JScrollPane();
        panel_mixChannels = new javax.swing.JPanel();

        setBackground(new java.awt.Color(153, 153, 153));
        setMinimumSize(new java.awt.Dimension(50, 70));
        setLayout(new java.awt.BorderLayout());

        panel_Main.setBackground(new java.awt.Color(202, 202, 202));
        panel_Main.setLayout(new java.awt.BorderLayout());

        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 1, 2);
        flowLayout1.setAlignOnBaseline(true);
        panel_MasterControls.setLayout(flowLayout1);
        panel_MasterControls.add(filler7);

        panel_MasterControls.add(cb_viewRhythms);
        panel_MasterControls.add(filler6);

        lbl_Master.setFont(lbl_Master.getFont().deriveFont(lbl_Master.getFont().getStyle() | java.awt.Font.BOLD, lbl_Master.getFont().getSize()-1));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Master, org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.lbl_Master.text")); // NOI18N
        panel_MasterControls.add(lbl_Master);

        masterHorizontalSlider1.setColorLine(new java.awt.Color(153, 153, 153));
        masterHorizontalSlider1.setFaderHeight(4);
        masterHorizontalSlider1.setKnobDiameter(10);
        panel_MasterControls.add(masterHorizontalSlider1);
        panel_MasterControls.add(filler3);

        fbtn_switchAllMute.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_switchAllMute, " M "); // NOI18N
        fbtn_switchAllMute.setToolTipText(org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.fbtn_switchAllMute.toolTipText")); // NOI18N
        fbtn_switchAllMute.setFont(fbtn_switchAllMute.getFont().deriveFont(fbtn_switchAllMute.getFont().getSize()-1f));
        panel_MasterControls.add(fbtn_switchAllMute);
        panel_MasterControls.add(filler4);

        fbtn_allSoloOff.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_allSoloOff, " S "); // NOI18N
        fbtn_allSoloOff.setFont(fbtn_allSoloOff.getFont().deriveFont(fbtn_allSoloOff.getFont().getSize()-1f));
        panel_MasterControls.add(fbtn_allSoloOff);
        panel_MasterControls.add(filler5);

        fbtn_panic.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_panic, " PANIC "); // NOI18N
        fbtn_panic.setToolTipText(org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.fbtn_panic.toolTipText")); // NOI18N
        fbtn_panic.setFont(fbtn_panic.getFont().deriveFont(fbtn_panic.getFont().getSize()-1f));
        panel_MasterControls.add(fbtn_panic);
        panel_MasterControls.add(filler2);

        fbtn_addUserChannel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/mixconsole/resources/AddUserTrackIcon.png"))); // NOI18N
        panel_MasterControls.add(fbtn_addUserChannel);
        panel_MasterControls.add(filler8);

        panel_Main.add(panel_MasterControls, java.awt.BorderLayout.PAGE_START);

        scrollPane_mixChannelsPanel.setBackground(new java.awt.Color(220, 220, 220));
        scrollPane_mixChannelsPanel.setOpaque(false);

        panel_mixChannels.setToolTipText(org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.panel_mixChannels.toolTipText")); // NOI18N
        panel_mixChannels.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                panel_mixChannelsMouseDragged(evt);
            }
        });
        panel_mixChannels.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 3, 5));
        scrollPane_mixChannelsPanel.setViewportView(panel_mixChannels);

        panel_Main.add(scrollPane_mixChannelsPanel, java.awt.BorderLayout.CENTER);

        add(panel_Main, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents

    private void panel_mixChannelsMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_panel_mixChannelsMouseDragged
    {//GEN-HEADEREND:event_panel_mixChannelsMouseDragged
        startDragOut(evt);
    }//GEN-LAST:event_panel_mixChannelsMouseDragged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<Rhythm> cb_viewRhythms;
    private org.jjazz.flatcomponents.api.FlatButton fbtn_addUserChannel;
    private org.jjazz.flatcomponents.api.FlatButton fbtn_allSoloOff;
    private org.jjazz.flatcomponents.api.FlatButton fbtn_panic;
    private org.jjazz.flatcomponents.api.FlatButton fbtn_switchAllMute;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.JLabel lbl_Master;
    private org.jjazz.mixconsole.MasterVolumeSlider masterHorizontalSlider1;
    private javax.swing.JPanel panel_Main;
    private javax.swing.JPanel panel_MasterControls;
    private javax.swing.JPanel panel_mixChannels;
    private javax.swing.JScrollPane scrollPane_mixChannelsPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public String toString()
    {
        return "Song Instruments Editor";
    }

    //-----------------------------------------------------------------------
    // Implementation of the ActionListener interface
    //-----------------------------------------------------------------------
    @Override
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getSource() == cb_viewRhythms)
        {
            Rhythm r = (Rhythm) cb_viewRhythms.getSelectedItem();
            setVisibleRhythm(r == RHYTHM_ALL ? null : r);
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        LOGGER.log(Level.FINE, "propertyChange() -- e={0}", e);
        if (e.getSource() == settings)
        {
            if (e.getPropertyName().equals(MixConsoleSettings.PROP_BACKGROUND_COLOR))
            {
                refreshUI();
            }
        } else if (e.getSource() == songMidiMix)
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX ->
                {
                    int channel = (int) e.getNewValue();
                    RhythmVoice rv = songMidiMix.getRhythmVoice(channel);
                    InstrumentMix oldInsMix = (InstrumentMix) e.getOldValue();
                    InstrumentMix insMix = songMidiMix.getInstrumentMix(channel);
                    updateVisibleRhythmUI();
                    if (insMix == null)
                    {
                        // InstrumentMix was removed
                        LOGGER.fine("propertyChange() InstrumentMix removed");
                        removeChannel(channel);
                        if (getChannelPanelSets().isEmpty())
                        {
                            setVisibleRhythm(null);     // Make all rhythms visible
                        }
                    } else if (oldInsMix == null)
                    {
                        // New InstrumentMix was added
                        LOGGER.log(Level.FINE, "propertyChange() InstrumentMix added insMix={0}", insMix);
                        if (getVisibleRhythm() == null || getVisibleRhythm() == rv.getContainer() || rv instanceof UserRhythmVoice)
                        {
                            addChannel(channel);
                        }
                    } else
                    {
                        // InstrumentMix is replacing an existing one
                        LOGGER.fine("propertyChange() InstrumentMix replaced");
                        if (isChannelVisible(channel))
                        {
                            removeChannel(channel);
                        }
                        if (getVisibleRhythm() == null || getVisibleRhythm() == rv.getContainer() || rv instanceof UserRhythmVoice)
                        {
                            addChannel(channel);
                        }
                    }
                }
                case MidiMix.PROP_RHYTHM_VOICE ->
                {
                    // Handled directly by the MixChannelPanelModel and PhraseViewerPanel
                }
                case MidiMix.PROP_RHYTHM_VOICE_CHANNEL ->
                {
                    // MixChannelPanelModel will handle the display, but need to relayout
                    changeChannel((int) e.getOldValue(), (int) e.getNewValue());
                    panel_mixChannels.revalidate();
                    panel_mixChannels.repaint();
                }
                case MidiMix.PROP_MODIFIED_OR_SAVED ->
                {
                    boolean b = (boolean) e.getNewValue();
                    if (b)
                    {
                        songModel.setSaveNeeded(true);
                    }
                }
                default ->
                {
                }
            }
        } else if (e.getSource() == SongEditorManager.getDefault())
        {
            if (e.getPropertyName().equals(SongEditorManager.PROP_SONG_CLOSED))
            {
                Song closedSong = (Song) e.getNewValue();
                if (songModel == closedSong)
                {
                    resetModel();
                }
                mapVisibleRhythm.remove(closedSong);
            }
        } else if (e.getSource() == ActiveSongManager.getDefault())
        {
            if (e.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
            {
                MidiMix mm = (MidiMix) e.getOldValue();
                updateActiveState(mm != null && mm == songMidiMix);
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Private functions
    // ------------------------------------------------------------------------------------
    /**
     * Called when song presence changed in the lookup.
     */
    private void songPresenceChanged()
    {
        Song song = Utilities.actionsGlobalContext().lookup(Song.class);
        LOGGER.log(Level.FINE, "songPresenceChanged() -- song={0} songModel={1}", new Object[]
        {
            song, songModel
        });

        if (songModel == song || song == null)
        {
            // Do nothing if same window or non song-editor-topcomponent got activated
            return;
        }

        // Reset the model and dependent UI
        resetModel();

        try
        {
            songMidiMix = MidiMixManager.getDefault().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            String msg = ResUtil.getString(getClass(), "CTL_NoMidiMixFound", song.getName());
            msg += ".\n" + ex.getLocalizedMessage();
            LOGGER.severe(msg);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }
        songModel = song;


        // Connect the song UndoManager to the MidiMix
        UndoManager um = JJazzUndoManagerFinder.getDefault().get(songModel);
        assert um != null;
        songMidiMix.addUndoableEditListener(um);


        // Update the combobox
        updateVisibleRhythmUI();


        // Restore enabled state depe
        MidiMix activeMix = ActiveSongManager.getDefault().getActiveMidiMix();
        updateActiveState(activeMix == songMidiMix);


        // Update our lookup
        instanceContent.add(songMidiMix);
        instanceContent.add(songModel);


        // Update the console with MidiMix changes
        songMidiMix.addPropertyChangeListener(this);


        // Update the TransferHandler
        panel_mixChannels.setTransferHandler(new MixConsoleTransferHandler(songModel, songMidiMix, null));


        // Add the visible channels
        addVisibleChannels();

        LOGGER.log(Level.FINE, "   songMidiMix={0}", songMidiMix);
    }

    private void addVisibleChannels()
    {
        // Add the rhythm channels
        for (Integer channel : songMidiMix.getUsedChannels(getVisibleRhythm()))
        {
            // Add a MixChannelPanel for each InstrumentMix
            addChannel(channel);
        }

        // Add user channels if needed
        if (getVisibleRhythm() != null)
        {
            for (int channel : songMidiMix.getUserChannels())
            {
                addChannel(channel);
            }
        }
    }

    private void changeChannel(int oldChannel, int newChannel)
    {
        var panelSet = getChannelPanelSet(oldChannel);
        assert panelSet != null;
        assert getChannelPanelSet(newChannel) == null : "tmapChannelPanelSets=" + tmapChannelPanelSets + " newChannel=" + newChannel;
        tmapChannelPanelSets.remove(oldChannel);
        tmapChannelPanelSets.put(newChannel, panelSet);
    }


    private void addChannel(int channel)
    {
        RhythmVoice rv = songMidiMix.getRhythmVoice(channel);


        // The channel controller
        MixChannelPanelControllerImpl mcpController = new MixChannelPanelControllerImpl(songModel, songMidiMix);


        // Main panel                
        MixChannelPanelModelImpl mcpModel = new MixChannelPanelModelImpl(songMidiMix, channel);
        MixChannelPanel mcp = new MixChannelPanel(mcpModel, mcpController, settings);


        // Birds-eye-view panel
        var pvp = PhraseViewerPanel.createInstance(songModel, songMidiMix, mcpController, rv);

                
        var panelSet = new ChannelPanelSet(rv, mcp, pvp);        
        tmapChannelPanelSets.put(channel, panelSet);

        

        // Add the 2 components
        panel_mixChannels.add(mcp);         // Our layout manager will place it ordered by channel        
        panel_mixChannels.add(pvp);


        // Set a transfer handler 
        TransferHandler th = new MixConsoleTransferHandler(songModel, songMidiMix, rv);
        mcp.setTransferHandler(th);
        pvp.setTransferHandler(th);


        updateChannelColors();


        panel_mixChannels.revalidate();
        panel_mixChannels.repaint();
    }

    private void removeChannel(int channel)
    {

        var panelSet = tmapChannelPanelSets.remove(channel);
        if (panelSet == null)
        {
            // Possible if rhythm is not visible
            return;
        }

        panel_mixChannels.remove(panelSet.phraseViewerPanel);
        panelSet.phraseViewerPanel.cleanup();


        panel_mixChannels.remove(panelSet.mixChannelPanel);
        panelSet.mixChannelPanel.cleanup();

        updateChannelColors();

        panel_mixChannels.revalidate();
        panel_mixChannels.repaint();

    }


    private void updateChannelColors()
    {
        Map<Rhythm, Color> mapRhythmColor = new HashMap<>();

        mapRhythmColor.put(UserRhythmVoice.CONTAINER, CHANNEL_COLOR_USER);

        int index = 0;
        for (int channel : tmapChannelPanelSets.keySet())
        {
            Rhythm r = songMidiMix.getRhythmVoice(channel).getContainer();
            Color c = mapRhythmColor.get(r);
            if (c == null)
            {
                c = CHANNEL_COLORS[index];
                mapRhythmColor.put(r, c);
                index++;
                if (index >= CHANNEL_COLORS.length)
                {
                    index = 0;
                }
            }
            var panelSet = getChannelPanelSet(channel);
            panelSet.mixChannelPanel.getModel().setChannelColor(c);
            panelSet.phraseViewerPanel.setForeground(c);
        }

    }

    private void removeAllChannels()
    {
        for (int channel : tmapChannelPanelSets.keySet().toArray(Integer[]::new))
        {
            removeChannel(channel);
        }
    }

    /**
     * Update the UI used to adjust the visible rhythms (because songModel's rhythms may have changed).
     */
    private void updateVisibleRhythmUI()
    {
        List<Rhythm> rhythms;
        if (songModel == null || (rhythms = songModel.getSongStructure().getUniqueRhythms(true, false)).size() < 2)
        {
            // Hide the combo box
            if (cb_viewRhythms.getParent() != null)
            {
                panel_MasterControls.remove(cb_viewRhythms);
                panel_MasterControls.revalidate();
                panel_MasterControls.repaint();
                cb_viewRhythms.removeActionListener(this);
                cb_viewRhythms.removeAllItems();
            }
        } else
        {
            // Update the combo box content and show the combobox if not done
            cb_viewRhythms.removeActionListener(this);
            rhythms.add(0, RHYTHM_ALL);
            ComboBoxModel<Rhythm> cbModel = new DefaultComboBoxModel<>(rhythms.toArray(Rhythm[]::new));
            cb_viewRhythms.setModel(cbModel);
            Rhythm selectedRhythm = (getVisibleRhythm() == null) ? RHYTHM_ALL : getVisibleRhythm();
            cb_viewRhythms.setSelectedItem(selectedRhythm);
            cb_viewRhythms.setEnabled(ActiveSongManager.getDefault().getActiveMidiMix() == songMidiMix);
            cb_viewRhythms.addActionListener(this);
            if (cb_viewRhythms.getParent() == null)
            {
                panel_MasterControls.add(cb_viewRhythms, 1);
                panel_MasterControls.revalidate();
            }
        }
    }


    private void resetModel()
    {
        removeAllChannels();
        if (songMidiMix != null)
        {
            instanceContent.remove(songMidiMix);
            UndoManager um = JJazzUndoManagerFinder.getDefault().get(songModel);
            assert um != null;
            songMidiMix.removeUndoableEditListener(um);
        }
        if (songMidiMix != null)
        {
            songMidiMix.removePropertyChangeListener(this);
        }
        if (songModel != null)
        {
            instanceContent.remove(songModel);
        }

        // Drag out handler
        panel_mixChannels.setTransferHandler(null);


        updateActiveState(false);
        songMidiMix = null;
        songModel = null;
    }

    /**
     * @param b Active state of the console.
     */
    private void updateActiveState(boolean b)
    {
        LOGGER.log(Level.FINE, "updateActiveState() -- b={0}", b);
        org.jjazz.uiutilities.api.UIUtilities.setRecursiveEnabled(b, menuBar);
        org.jjazz.uiutilities.api.UIUtilities.setRecursiveEnabled(b, panel_MasterControls);
    }

    private void refreshUI()
    {
        panel_mixChannels.setBackground(settings.getBackgroundColor());
    }

    /**
     * Set a menuBar and a child content panel into the parent panel.
     * <p>
     * Use a JRootPane to achieve the result.
     *
     * @param parent
     * @param child
     * @param menuBar
     */
    private void setJPanelMenuBar(JPanel parent, JComponent child, JMenuBar menuBar)
    {
        parent.removeAll();
        parent.setLayout(new BorderLayout());
        JRootPane root = new JRootPane();
        parent.add(root, BorderLayout.CENTER);
        root.setJMenuBar(menuBar);
        root.getContentPane().add(child);
        parent.putClientProperty("root", root);  // if you need later
    }


    private void startDragOut(MouseEvent evt)
    {
        if (SwingUtilities.isLeftMouseButton(evt))
        {
            var th = panel_mixChannels.getTransferHandler();    // Might be null upon start when no song has been opened yet
            if (th != null)
            {
                th.exportAsDrag(panel_mixChannels, evt, TransferHandler.COPY);
            }
        }
    }

    private ChannelPanelSet getChannelPanelSet(int channel)
    {
        return tmapChannelPanelSets.get(channel);
    }

    /**
     * Get MixConsole MenuBar.
     * <p>
     * Created from layer registrations the lawyer, and apply tweaking for the MixConsole.
     *
     * @param layerPath
     * @return
     * @todo Localize File & Edit menu names
     */
    private MenuBar buildMenuBar(String layerPath)
    {

        // Hack to manage I18N on File and Edit submenus : rename the folder in the layer file!

        // Build the bar from actions which are registered in layerPath
        MenuBar mBar = new MenuBar(DataFolder.findFolder(FileUtil.getConfigFile(layerPath)));


        // Reduce font size
        org.jjazz.uiutilities.api.UIUtilities.changeMenuBarFontSize(mBar, -2f);


        return mBar;
    }

    private boolean isChannelVisible(int channel)
    {
        return tmapChannelPanelSets.containsKey(channel);
    }


    // ========================================================================================================
    // Inner classes
    // ========================================================================================================
    // ========================================================================================================
    // Private classes
    // ========================================================================================================
    private class MyRenderer extends DefaultListCellRenderer
    {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);      // actually c=this !
            Rhythm r = (Rhythm) value;
            if (r != null)
            {
                String s = r.getName().length() > 13 ? r.getName().substring(0, 13) + "..." : r.getName();
                setText(s);
                setToolTipText(r.getName());
            }
            return c;
        }
    }

    private class MyOutDeviceButton extends FlatButton
    {

        public MyOutDeviceButton()
        {
            var jms = JJazzMidiSystem.getInstance();
            jms.addPropertyChangeListener(JJazzMidiSystem.PROP_MIDI_OUT, e -> updateText());
            updateText();
            setToolTipText(ResUtil.getString(getClass(), "CURRENT_MIDI_OUT_DEVICE"));
            addActionListener(e -> showMidiOptionPanel());
        }

        private void updateText()
        {
            var jms = JJazzMidiSystem.getInstance();
            MidiDevice md = jms.getDefaultOutDevice();
            if (md != null)
            {
                setText(" OUT: " + jms.getDeviceFriendlyName(md) + " ");
            } else
            {
                setText("-");
            }
        }

        private void showMidiOptionPanel()
        {
            OptionsDisplayer.getDefault().open("MidiPanelId");
        }
    }


}
