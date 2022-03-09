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
package org.jjazz.ui.mixconsole.api;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Track;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY;
import javax.swing.undo.UndoManager;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.base.api.actions.Savable;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.savablesong.api.SavableSong;
import org.jjazz.savablesong.api.SaveAsCapableSong;
import org.jjazz.song.api.Song;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.stubs.api.DummyRhythm;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder.SongSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songeditormanager.api.SongEditorManager;
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
import org.jjazz.ui.flatcomponents.api.FlatButton;
import org.jjazz.ui.mixconsole.MixChannelPanel;
import org.jjazz.ui.mixconsole.MixChannelPanelControllerImpl;
import org.jjazz.ui.mixconsole.MixChannelPanelModelImpl;
import org.jjazz.ui.mixconsole.MixConsoleLayoutManager;
import org.jjazz.ui.mixconsole.UserExtensionPanel;
import org.jjazz.ui.mixconsole.UserExtensionPanelController;
import org.jjazz.ui.utilities.api.FileTransferable;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;
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
 * - Savable and SaveAsCapable instances<p>
 * .
 * The MixConsole Netbeans MenuBar is built from subfolders and actions available in the Actions/MixConsole/MenuBar layer folder.
 */
public class MixConsole extends JPanel implements PropertyChangeListener, ActionListener
{

    @StaticResource(relative = true)
    private final String USER_ICON_PATH = "resources/User-48x48.png";

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
    private SavableSong savableSong;
    private SaveAsCapableSong saveAsCapableSong;

    private static final Logger LOGGER = Logger.getLogger(MixConsole.class.getSimpleName());

    public MixConsole(MixConsoleSettings settings)
    {
        if (settings == null)
        {
            throw new IllegalArgumentException("settings=" + settings);   //NOI18N
        }


        // Listen to settings change events
        this.settings = settings;
        settings.addPropertyChangeListener(this);

        // Listen to active song and midimix changes
        ActiveSongManager.getInstance().addPropertyListener(this);

        // Listen to closed song events to cleanup our data
        SongEditorManager.getInstance().addPropertyChangeListener(this);

        // A dummy rhythm used by the visible rhythms combobox when all song rhythms are visible
        RHYTHM_ALL = new DummyRhythm(ResUtil.getString(getClass(), "MixConsole.CTL_DummyRhythmAll"), TimeSignature.FOUR_FOUR);

        mapVisibleRhythm = new WeakHashMap<>();

        // UI initialization
        initComponents();


        // Drag out handler
        panel_mixChannels.setTransferHandler(new MidiFileDragOutTransferHandler(null));

        // Use our LayoutManager to arranger MixChannelPanels and their extensions
        panel_mixChannels.setLayout(new MixConsoleLayoutManager());

        // Our renderer to show visible rhythms
        cb_viewRhythms.setRenderer(new MyRenderer());

        // Connect to standard actions
        // fbtn_muteAll.setAction(Actions.forID("MixConsole", "org.jjazz.ui.mixconsole.actions.mastermuteall"));   //NOI18N
        fbtn_panic.setAction(Actions.forID("MixConsole", "org.jjazz.ui.mixconsole.actions.panic"));   //NOI18N
        fbtn_switchAllMute.setAction(Actions.forID("MixConsole", "org.jjazz.ui.mixconsole.actions.switchallmute"));   //NOI18N
        fbtn_allSoloOff.setAction(Actions.forID("MixConsole", "org.jjazz.ui.mixconsole.actions.allsolooff"));   //NOI18N
        fbtn_addUserChannel.setAction(Actions.forID("MixConsole", "org.jjazz.ui.mixconsole.actions.addusertrack"));


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
        settings.removePropertyChangeListener(this);
        ActiveSongManager.getInstance().removePropertyListener(this);
        SongEditorManager.getInstance().removePropertyChangeListener(this);
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
        if (songModel == null || r instanceof AdaptedRhythm || (r != null && !songModel.getSongStructure().getUniqueRhythms(true, false).contains(r)))
        {
            throw new IllegalStateException("songModel=" + songModel + " r=" + r);   //NOI18N
        }
        Rhythm oldVisibleRhythm = getVisibleRhythm();
        if (r != oldVisibleRhythm)
        {
            mapVisibleRhythm.put(songModel, r);
            removeAllMixChannelPanels();
            addVisibleMixChannelPanels();
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
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
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
        masterHorizontalSlider1 = new org.jjazz.ui.mixconsole.MasterVolumeSlider();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_switchAllMute = new org.jjazz.ui.flatcomponents.api.FlatButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_allSoloOff = new org.jjazz.ui.flatcomponents.api.FlatButton();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_panic = new org.jjazz.ui.flatcomponents.api.FlatButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_addUserChannel = new org.jjazz.ui.flatcomponents.api.FlatButton();
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

        fbtn_addUserChannel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/AddUser-16x16.png"))); // NOI18N
        panel_MasterControls.add(fbtn_addUserChannel);

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
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_addUserChannel;
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_allSoloOff;
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_panic;
    private org.jjazz.ui.flatcomponents.api.FlatButton fbtn_switchAllMute;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.JLabel lbl_Master;
    private org.jjazz.ui.mixconsole.MasterVolumeSlider masterHorizontalSlider1;
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
        LOGGER.fine("propertyChange() -- e=" + e);   //NOI18N
        if (e.getSource() == settings)
        {
            if (e.getPropertyName().equals(MixConsoleSettings.PROP_BACKGROUND_COLOR))
            {
                refreshUI();
            }
        } else if (e.getSource() == songMidiMix)
        {
            if (e.getPropertyName().equals(MidiMix.PROP_CHANNEL_INSTRUMENT_MIX))
            {
                int channel = (int) e.getNewValue();
                RhythmVoice rv = songMidiMix.getRhythmVoice(channel);
                InstrumentMix oldInsMix = (InstrumentMix) e.getOldValue();
                InstrumentMix insMix = songMidiMix.getInstrumentMixFromChannel(channel);
                updateVisibleRhythmUI();

                if (insMix == null)
                {
                    // InstrumentMix was removed
                    LOGGER.fine("propertyChange() InstrumentMix removed");   //NOI18N
                    MixChannelPanel mcp = getMixChannelPanel(channel); // can be null if not visible
                    if (mcp != null)
                    {
                        removeMixChannelPanel(mcp);
                        if (getMixChannelPanels().isEmpty())
                        {
                            setVisibleRhythm(null);     // Make all rhythms visible
                        }
                    }
                } else if (oldInsMix == null)
                {
                    // New InstrumentMix was added
                    LOGGER.fine("propertyChange() InstrumentMix added insMix=" + insMix);   //NOI18N
                    if (getVisibleRhythm() == null || getVisibleRhythm() == rv.getContainer() || rv instanceof UserRhythmVoice)
                    {
                        addMixChannelPanel(songMidiMix, channel);
                    }
                } else
                {
                    // InstrumentMix is replacing an existing one
                    LOGGER.fine("propertyChange() InstrumentMix replaced");   //NOI18N
                    MixChannelPanel mcp = getMixChannelPanel(channel);
                    if (mcp != null)
                    {
                        removeMixChannelPanel(mcp);
                    }
                    if (getVisibleRhythm() == null || getVisibleRhythm() == rv.getContainer() || rv instanceof UserRhythmVoice)
                    {
                        addMixChannelPanel(songMidiMix, channel);
                    }
                }
            } else if (e.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED_OR_RESET))
            {
                boolean b = (boolean) e.getNewValue();
                if (b)
                {
                    setSongModified();
                } else
                {
                    resetSongModified();
                }
            }
        } else if (e.getSource() == SongEditorManager.getInstance())
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
        } else if (e.getSource() == ActiveSongManager.getInstance())
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
        LOGGER.log(Level.FINE, "songPresenceChanged() -- song=" + song + " songModel=" + songModel);   //NOI18N
        if (songModel == song || song == null)
        {
            // Do nothing if same window or non song-editor-topcomponent got activated
            return;
        }

        // Reset the model and dependent UI
        resetModel();

        try
        {
            songMidiMix = MidiMixManager.getInstance().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            String msg = ResUtil.getString(getClass(), "CTL_NoMidiMixFound", song.getName());
            msg += ".\n" + ex.getLocalizedMessage();
            LOGGER.severe(msg);   //NOI18N
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }
        songModel = song;

        // Connect the song UndoManager to the MidiMix
        UndoManager um = JJazzUndoManagerFinder.getDefault().get(songModel);
        assert um != null;   //NOI18N
        songMidiMix.addUndoableEditListener(um);

        // Update the combobox
        updateVisibleRhythmUI();

        // Restore enabled state depe
//        menuFile.setEnabled(true);
//        menuEdit.setEnabled(true);
        MidiMix activeMix = ActiveSongManager.getInstance().getActiveMidiMix();
        updateActiveState(activeMix == songMidiMix);

        // Store our MidiMix in the lookup, the Song model, plus a SaveAsCapable object
        instanceContent.add(songMidiMix);
        instanceContent.add(songModel);
        saveAsCapableSong = new SaveAsCapableSong(songModel);
        instanceContent.add(saveAsCapableSong); // always enabled

        // Update the console with MidiMix changes
        songMidiMix.addPropertyChangeListener(this);

        // Add the visible channel panels
        addVisibleMixChannelPanels();

        LOGGER.fine("   songMidiMix=" + songMidiMix);   //NOI18N
    }

    private void addVisibleMixChannelPanels()
    {
        // Add the MixChannelPanels
        for (Integer channel : songMidiMix.getUsedChannels(getVisibleRhythm()))
        {
            // Add a MixChannelPanel for each InstrumentMix
            addMixChannelPanel(songMidiMix, channel);
        }

        // Add the user channel if needed
        if (getVisibleRhythm() != null)
        {
            for (int channel : songMidiMix.getUserChannels())
            {
                addMixChannelPanel(songMidiMix, channel);
            }
        }
    }

    private void addMixChannelPanel(MidiMix mm, int channel)
    {
        MixChannelPanel mcp;
        RhythmVoice rv = songMidiMix.getRhythmVoice(channel);
        if (rv instanceof UserRhythmVoice)
        {
            // User channel
            mcp = createMixChannelPanelForUserVoice(mm, channel);
            insertMixChannelPanel(channel, mcp);
            UserExtensionPanel ucep = new UserExtensionPanel(songModel,
                    songMidiMix,
                    (UserRhythmVoice) rv,
                    new UserExtensionPanelController(),
                    settings
            );
            panel_mixChannels.add(ucep);        // Add always at the last position
        } else
        {
            // Rhythm channel
            mcp = createMixChannelPanelForRhythmVoice(mm, channel, rv);
            insertMixChannelPanel(channel, mcp);
        }


        // Set a transfer handler for each panel
        mcp.setTransferHandler(new MidiFileDragOutTransferHandler(rv));


        panel_mixChannels.revalidate();
        panel_mixChannels.repaint();

        updateChannelColors();
    }

    private MixChannelPanel createMixChannelPanelForRhythmVoice(MidiMix mm, int channel, RhythmVoice rv)
    {
        LOGGER.log(Level.FINE, "createMixChannelPanelForRhythmVoice() -- mm={0} channel={1} rv={2}", new Object[]
        {
            mm, channel, rv
        });   //NOI18N
        MixChannelPanelModelImpl mcpModel = new MixChannelPanelModelImpl(mm, channel);
        MixChannelPanelControllerImpl mcpController = new MixChannelPanelControllerImpl(mm, channel);
        MixChannelPanel mcp = new MixChannelPanel(mcpModel, mcpController, settings);
        Rhythm r = rv.getContainer();
        mcp.setChannelName(r.getName(), rv.getName());
        Instrument prefIns = rv.getPreferredInstrument();
        Icon icon;
        switch (rv.getType())
        {
            case DRUMS:
                icon = new ImageIcon(getClass().getResource("resources/Drums-48x48.png"));
                break;
            case PERCUSSION:
                icon = new ImageIcon(getClass().getResource("resources/Percu-48x48.png"));
                break;
            default:    // VOICE
                switch (prefIns.getSubstitute().getFamily())
                {
                    case Guitar:
                        icon = new ImageIcon(getClass().getResource("resources/Guitar-48x48.png"));
                        break;
                    case Piano:
                    case Organ:
                    case Synth_Lead:
                        icon = new ImageIcon(getClass().getResource("resources/Keyboard-48x48.png"));
                        break;
                    case Bass:
                        icon = new ImageIcon(getClass().getResource("resources/Bass-48x48.png"));
                        break;
                    case Brass:
                    case Reed:
                        icon = new ImageIcon(getClass().getResource("resources/HornSection-48x48.png"));
                        break;
                    case Strings:
                    case Synth_Pad:
                    case Ensemble:
                        icon = new ImageIcon(getClass().getResource("resources/Strings-48x48.png"));
                        break;
                    case Percussive:
                        icon = new ImageIcon(getClass().getResource("resources/Percu-48x48.png"));
                        break;
                    default: // Ethnic, Sound_Effects, Synth_Effects, Pipe, Chromatic_Percussion:
                        icon = new ImageIcon(getClass().getResource("resources/Notes-48x48.png"));
                }

        }
        mcp.setNameToolTipText(r.getName() + " - " + rv.getName());


        mcp.setIcon(icon);
        String txt = ResUtil.getString(getClass(), "CTL_RECOMMENDED", prefIns.getFullName());
        if (!(prefIns instanceof GM1Instrument))
        {
            DrumKit kit = prefIns.getDrumKit();
            txt += " - ";
            txt += rv.isDrums() ? "DrumKit type=" + kit.getType().toString() + " keymap= " + kit.getKeyMap().getName()
                    : "GM substitute: " + prefIns.getSubstitute().getPatchName();
        }
        mcp.setIconToolTipText(txt);
        return mcp;

    }

    private MixChannelPanel createMixChannelPanelForUserVoice(MidiMix mm, int channel)
    {
        LOGGER.fine("createMixChannelPanelForUserVoice() -- mm=" + mm + " channel=" + channel);   //NOI18N
        MixChannelPanelModelImpl mcpModel = new MixChannelPanelModelImpl(mm, channel);
        MixChannelPanelControllerImpl mcpController = new MixChannelPanelControllerImpl(mm, channel);
        MixChannelPanel mcp = new MixChannelPanel(mcpModel, mcpController, settings);
        mcp.setChannelColor(CHANNEL_COLOR_USER);
        mcp.setChannelName(ResUtil.getString(getClass(), "USER"), ResUtil.getString(getClass(), "CHANNEL"));
        Icon icon = new ImageIcon(getClass().getResource(USER_ICON_PATH));
        mcp.setIcon(icon);
        mcp.setIconToolTipText(null);
        return mcp;
    }


    private void removeMixChannelPanel(MixChannelPanel mcp)
    {
        RhythmVoice rv = mcp.getModel().getRhythmVoice();
        if (rv instanceof UserRhythmVoice)
        {
            // Need to remove the UserChannelExtenionPanel as well
            var ucep = getUserChannelExtensionPanel((UserRhythmVoice) rv);
            ucep.cleanup();
            panel_mixChannels.remove(ucep);
        }


        mcp.cleanup();
        panel_mixChannels.remove(mcp);
        panel_mixChannels.revalidate();
        panel_mixChannels.repaint();
        updateChannelColors();
    }

    private UserExtensionPanel getUserChannelExtensionPanel(UserRhythmVoice urv)
    {
        for (Component c : panel_mixChannels.getComponents())
        {
            if (c instanceof UserExtensionPanel)
            {
                var ucep = (UserExtensionPanel) c;
                if (ucep.getUserRhythmVoice() == urv)
                {
                    return ucep;
                }
            }
        }
        return null;
    }


    private void updateChannelColors()
    {
        Map<Rhythm, Color> mapRhythmColor = new HashMap<>();

        mapRhythmColor.put(UserRhythmVoice.CONTAINER, CHANNEL_COLOR_USER);

        int index = 0;
        for (MixChannelPanel mcp : getMixChannelPanels())
        {
            int channel = mcp.getModel().getChannelId();
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
            mcp.setChannelColor(c);
        }

    }

    /**
     * Insert the MixChannelPanel at the right position.
     *
     * @param channel
     * @param mcp
     */
    private void insertMixChannelPanel(int channel, MixChannelPanel mcp)
    {
        // Add the panel at the right place (ordered by channel)
        List<MixChannelPanel> mcps = getMixChannelPanels();
        int index = 0;
        if (mcps.size() > 0)
        {
            for (MixChannelPanel mcpi : mcps)
            {
                int chan = mcpi.getModel().getChannelId();
                if (channel < chan)
                {
                    break;
                }
                index++;
            }
        }
        panel_mixChannels.add(mcp, index);

    }

    private void removeAllMixChannelPanels()
    {
        for (MixChannelPanel mcp : getMixChannelPanels())
        {
            removeMixChannelPanel(mcp);
        }
    }

    private List<MixChannelPanel> getMixChannelPanels()
    {
        ArrayList<MixChannelPanel> mcps = new ArrayList<>();
        for (Component c : panel_mixChannels.getComponents())
        {
            if (c instanceof MixChannelPanel)
            {
                mcps.add((MixChannelPanel) c);
            }
        }
        return mcps;
    }

    private MixChannelPanel getMixChannelPanel(int channel)
    {
        for (MixChannelPanel mcp : getMixChannelPanels())
        {
            if (mcp.getModel().getChannelId() == channel)
            {
                return mcp;
            }
        }
        return null;
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
            ComboBoxModel<Rhythm> cbModel = new DefaultComboBoxModel<>(rhythms.toArray(new Rhythm[0]));
            cb_viewRhythms.setModel(cbModel);
            Rhythm selectedRhythm = (getVisibleRhythm() == null) ? RHYTHM_ALL : getVisibleRhythm();
            cb_viewRhythms.setSelectedItem(selectedRhythm);
            cb_viewRhythms.setEnabled(ActiveSongManager.getInstance().getActiveMidiMix() == songMidiMix);
            cb_viewRhythms.addActionListener(this);
            if (cb_viewRhythms.getParent() == null)
            {
                panel_MasterControls.add(cb_viewRhythms, 1);
                panel_MasterControls.revalidate();
            }
        }
    }

    private void setSongModified()
    {
        if (savableSong == null)
        {
            savableSong = new SavableSong(songModel);
            Savable.ToBeSavedList.add(savableSong);
            instanceContent.add(savableSong);
        }
    }

    private void resetSongModified()
    {
        if (savableSong != null)
        {
            Savable.ToBeSavedList.remove(savableSong);
            instanceContent.remove(savableSong);
            savableSong = null;
        }
    }

    private void resetModel()
    {
        removeAllMixChannelPanels();
        if (songMidiMix != null)
        {
            instanceContent.remove(songMidiMix);
            UndoManager um = JJazzUndoManagerFinder.getDefault().get(songModel);
            assert um != null;   //NOI18N
            songMidiMix.removeUndoableEditListener(um);
        }
        if (saveAsCapableSong != null)
        {
            instanceContent.remove(saveAsCapableSong);
        }
        resetSongModified();
        if (songMidiMix != null)
        {
            songMidiMix.removePropertyChangeListener(this);
        }
        if (songModel != null)
        {
            instanceContent.remove(songModel);
        }

        updateActiveState(false);
        songMidiMix = null;
        songModel = null;
    }

    /**
     * @param b Active state of the console.
     */
    private void updateActiveState(boolean b)
    {
        LOGGER.fine("updateActiveState() -- b=" + b);   //NOI18N
        org.jjazz.ui.utilities.api.Utilities.setRecursiveEnabled(b, menuBar);
        org.jjazz.ui.utilities.api.Utilities.setRecursiveEnabled(b, panel_MasterControls);
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

    /**
     * Build an exportable sequence to a temp file.
     *
     * @param rv If not null export only the rv track, otherwise all tracks.
     * @return The generated Midi temporary file.
     * @throws IOException
     * @throws MusicGenerationException
     */
    private File exportSequenceToMidiTempFile(RhythmVoice rv) throws IOException, MusicGenerationException
    {
        LOGGER.fine("exportSequenceToMidiTempFile() -- ");
        assert songModel != null && songMidiMix != null : "songModel=" + songModel + " songMidiMix=" + songMidiMix;

        // Create the temp file
        File midiFile = File.createTempFile("JJazzMixConsoleDragOut", ".mid"); // throws IOException
        midiFile.deleteOnExit();


        // Build the sequence
        var sgContext = new SongContext(songModel, songMidiMix);
        SongSequence songSequence = new SongSequenceBuilder(sgContext).buildExportableSequence(true, false); // throws MusicGenerationException


        // Keep only rv track if defined
        if (rv != null)
        {
            int trackId = songSequence.mapRvTrackId.get(rv);

            // Remove all tracks except trackId, need to start from last track
            Track[] tracks = songSequence.sequence.getTracks();
            for (int i = tracks.length - 1; i >= 0; i--)
            {
                if (i != trackId)
                {
                    songSequence.sequence.deleteTrack(tracks[i]);
                }
            }

        } else
        {
            // Add click & precount tracks
            var ps = PlaybackSettings.getInstance();
            if (ps.isPlaybackClickEnabled())
            {
                ps.addClickTrack(songSequence.sequence, sgContext);
            }
            if (ps.isClickPrecountEnabled())
            {
                ps.addPrecountClickTrack(songSequence.sequence, sgContext);      // Must be done last, shift all events
            }
        }


        // Write the midi file     
        MidiSystem.write(songSequence.sequence, 1, midiFile);   // throws IOException

        return midiFile;
    }

    private void startDragOut(MouseEvent evt)
    {
        if (SwingUtilities.isLeftMouseButton(evt))
        {
            panel_mixChannels.getTransferHandler().exportAsDrag(panel_mixChannels, evt, TransferHandler.COPY);
        }
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
        MenuBar menuBar = new MenuBar(DataFolder.findFolder(FileUtil.getConfigFile(layerPath)));


        // Reduce font size
        org.jjazz.ui.utilities.api.Utilities.changeMenuBarFontSize(menuBar, -2f);


//        // Replace File & Edit menus (hard-coded in the declaratively-registered actions) by internationalized strings
//        for (int i = 0; i < menuBar.getMenuCount(); i++)
//        {
//            JMenu menu = menuBar.getMenu(i);
//            if (menu != null)
//            {
//                menu.setText("bobo");
//                if (menu.getText().equals("File"))
//                {
//                    menu.setText(ResUtil.getString(getClass(), "MixConsoleMenuFile"));
//                } else if (menu.getText().equals("Edit"))
//                {
//                    menu.setText(ResUtil.getString(getClass(), "MixConsoleMenuEdit"));
//                }
//            }
//        }
        return menuBar;
    }

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
            jms.addPropertyChangeListener(e ->
            {
                if (e.getPropertyName().equals(JJazzMidiSystem.PROP_MIDI_OUT))
                {
                    updateText();
                }
            });
            updateText();
            setToolTipText(ResUtil.getString(getClass(), "CURRENT MIDI OUT DEVICE"));
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

    /**
     * Our drag'n drop support to export Midi files when dragging from this component.
     */
    private class MidiFileDragOutTransferHandler extends TransferHandler
    {

        RhythmVoice rv;

        /**
         * @param rv If null, export the whole sequence, otherwise only the rv track.
         */
        public MidiFileDragOutTransferHandler(RhythmVoice rv)
        {
            this.rv = rv;
        }

        @Override
        public int getSourceActions(JComponent c)
        {
            LOGGER.fine("MidiFileDragOutTransferHandler.getSourceActions()  c=" + c);   //NOI18N

            int res = TransferHandler.NONE;

            // Make sure we'll be able to generate a song
            if (songModel != null && songMidiMix != null)
            {
                if (!songModel.getSongStructure().getSongParts().isEmpty() && (rv != null || !isAllMuted(songMidiMix)))
                {
                    res = TransferHandler.COPY_OR_MOVE;
                }
            }

            return res;
        }


        @Override
        public Transferable createTransferable(JComponent c)
        {
            LOGGER.fine("MidiFileDragOutTransferHandler.createTransferable()  c=" + c);   //NOI18N

            File midiFile = null;
            try
            {
                midiFile = exportSequenceToMidiTempFile(rv);
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

        private boolean isAllMuted(MidiMix mm)
        {
            boolean res = true;
            for (RhythmVoice rv : mm.getRhythmVoices())
            {
                if (!mm.getInstrumentMixFromKey(rv).isMute())
                {
                    res = false;
                    break;
                }
            }
            return res;
        }
    }

}
