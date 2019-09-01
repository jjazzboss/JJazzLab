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
package org.jjazz.ui.mixconsole;

import org.jjazz.undomanager.JJazzUndoManagerFinder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
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
import javax.swing.undo.UndoManager;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.base.actions.Savable;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.savablesong.SavableSong;
import org.jjazz.savablesong.SaveAsCapableSong;
import org.jjazz.song.api.Song;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.MidiMixManager;
import org.jjazz.midimix.UserChannelRhythmVoiceKey;
import org.jjazz.rhythm.api.AbstractRhythm;
import org.jjazz.songeditormanager.SongEditorManager;
import static org.jjazz.ui.mixconsole.Bundle.CTL_AllRhythms;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.jjazz.ui.mixconsole.api.MixConsoleSettings;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Actions;
import org.openide.util.NbBundle;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.jjazz.songstructure.api.SongStructure;
import org.netbeans.api.annotations.common.StaticResource;
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
@NbBundle.Messages(
        {
            "CTL_OnOffButtonTooltip=Set the active song. Song must be active to be played and to enable Midi messages.",
            "CTL_AllRhythms=All"
        })
public class MixConsole extends JPanel implements PropertyChangeListener, ActionListener
{

    @StaticResource(relative = true)
    private final String USER_ICON_PATH = "resources/User-48x48.png";

    /**
     * Colors used to distinguish channels from different rhythms.
     */
    private static final Color[] CHANNEL_COLORS =
    {
        new Color(0, 0, 102), // Deep blue
        new Color(102, 0, 0), // Brown
        new Color(0, 102, 102)  // Deep green
    };
    private static final Color CHANNEL_COLOR_USER = new Color(102, 102, 0);
    private static Rhythm RHYTHM_ALL;
    private WeakHashMap<Rhythm, Color> mapRhythmColor = new WeakHashMap<>();
    private InstanceContent instanceContent;
    private Lookup lookup;
    private Lookup.Result<Song> songLkpResult;
    private LookupListener songLkpListener;
    /**
     * The song currently edited by this editor.
     */
    private Song songModel;
    private MidiMix songMidiMix;
    // WeakHashMap for safety/robustness: normally not needed, we remove the song entry upon song closing
    private WeakHashMap<Song, Rhythm> mapVisibleRhythm;
    private MixConsoleSettings settings;
    private int colorIndex;
    private MenuBar menuBar;
    private SavableSong savableSong;
    private SaveAsCapableSong saveAsCapableSong;

    private static final Logger LOGGER = Logger.getLogger(MixConsole.class.getSimpleName());

    public MixConsole()
    {
        // Listen to settings change events
        settings = MixConsoleSettings.getDefault();
        settings.addPropertyChangeListener(this);

        // Listen to active song and midimix changes
        ActiveSongManager.getInstance().addPropertyListener(this);

        // Listen to closed song events to cleanup our data
        SongEditorManager.getInstance().addPropertyChangeListener(this);

        // A dummy rhythm used by the visible rhythms combobox when all song rhythms are visible
        RHYTHM_ALL = new AbstractRhythm("rhythmAllID", CTL_AllRhythms());

        mapVisibleRhythm = new WeakHashMap<>();

        // UI initialization
        initComponents();

        // Our renderer to show visible rhythms
        cb_viewRhythms.setRenderer(new MyRenderer());

        // Connect to standard actions
        // fbtn_muteAll.setAction(Actions.forID("MixConsole", "org.jjazz.ui.mixconsole.actions.mastermuteall"));
        fbtn_panic.setAction(Actions.forID("MixConsole", "org.jjazz.ui.mixconsole.actions.panic"));
        fbtn_switchAllMute.setAction(Actions.forID("MixConsole", "org.jjazz.ui.mixconsole.actions.switchallmute"));
        fbtn_allSoloOff.setAction(Actions.forID("MixConsole", "org.jjazz.ui.mixconsole.actions.allsolooff"));

        // Prepare the menu with smaller font
        menuBar = new MenuBar(DataFolder.findFolder(FileUtil.getConfigFile("Actions/MixConsole/MenuBar")));
        org.jjazz.ui.utilities.Utilities.changeMenuBarFontSize(menuBar, -2f);
        setJPanelMenuBar(this, panel_Main, menuBar);

        // By default not active
        updateActiveState(false);

        songLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                songPresenceChanged();
            }
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
     */
    public void setVisibleRhythm(Rhythm r)
    {
        if (songModel == null || (r != null && !SongStructure.Util.getUniqueRhythms(songModel.getSongStructure()).contains(r)))
        {
            throw new IllegalStateException("songModel=" + songModel + " r=" + r);
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
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
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
        jLabel1 = new javax.swing.JLabel();
        masterHorizontalSlider1 = new org.jjazz.ui.mixconsole.MasterVolumeSlider();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_switchAllMute = new org.jjazz.ui.flatcomponents.FlatButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_allSoloOff = new org.jjazz.ui.flatcomponents.FlatButton();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        fbtn_panic = new org.jjazz.ui.flatcomponents.FlatButton();
        scrollPane_mixChannelsPanel = new javax.swing.JScrollPane();
        panel_mixChannels = new javax.swing.JPanel();

        setBackground(new java.awt.Color(153, 153, 153));
        setMinimumSize(new java.awt.Dimension(50, 282));
        setLayout(new java.awt.BorderLayout());

        panel_Main.setBackground(new java.awt.Color(202, 202, 202));
        panel_Main.setLayout(new java.awt.BorderLayout());

        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 1, 2);
        flowLayout1.setAlignOnBaseline(true);
        panel_MasterControls.setLayout(flowLayout1);
        panel_MasterControls.add(filler7);

        panel_MasterControls.add(cb_viewRhythms);
        panel_MasterControls.add(filler6);

        jLabel1.setFont(new java.awt.Font("Arial Narrow", 1, 10)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.jLabel1.text")); // NOI18N
        panel_MasterControls.add(jLabel1);

        masterHorizontalSlider1.setColorLine(new java.awt.Color(153, 153, 153));
        masterHorizontalSlider1.setFaderHeight(4);
        masterHorizontalSlider1.setKnobDiameter(12);
        panel_MasterControls.add(masterHorizontalSlider1);
        panel_MasterControls.add(filler3);

        fbtn_switchAllMute.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_switchAllMute, org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.fbtn_switchAllMute.text")); // NOI18N
        fbtn_switchAllMute.setToolTipText(org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.fbtn_switchAllMute.toolTipText")); // NOI18N
        panel_MasterControls.add(fbtn_switchAllMute);
        panel_MasterControls.add(filler4);

        fbtn_allSoloOff.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_allSoloOff, org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.fbtn_allSoloOff.text")); // NOI18N
        fbtn_allSoloOff.setToolTipText(org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.fbtn_allSoloOff.toolTipText")); // NOI18N
        panel_MasterControls.add(fbtn_allSoloOff);
        panel_MasterControls.add(filler5);

        fbtn_panic.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(fbtn_panic, org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.fbtn_panic.text")); // NOI18N
        fbtn_panic.setToolTipText(org.openide.util.NbBundle.getMessage(MixConsole.class, "MixConsole.fbtn_panic.toolTipText")); // NOI18N
        fbtn_panic.setFont(new java.awt.Font("Arial Narrow", 0, 11)); // NOI18N
        panel_MasterControls.add(fbtn_panic);

        panel_Main.add(panel_MasterControls, java.awt.BorderLayout.PAGE_START);

        scrollPane_mixChannelsPanel.setBackground(new java.awt.Color(220, 220, 220));
        scrollPane_mixChannelsPanel.setOpaque(false);

        panel_mixChannels.setBackground(new java.awt.Color(204, 204, 204));
        panel_mixChannels.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 3, 5));
        scrollPane_mixChannelsPanel.setViewportView(panel_mixChannels);

        panel_Main.add(scrollPane_mixChannelsPanel, java.awt.BorderLayout.CENTER);

        add(panel_Main, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<Rhythm> cb_viewRhythms;
    private org.jjazz.ui.flatcomponents.FlatButton fbtn_allSoloOff;
    private org.jjazz.ui.flatcomponents.FlatButton fbtn_panic;
    private org.jjazz.ui.flatcomponents.FlatButton fbtn_switchAllMute;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.JLabel jLabel1;
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
        LOGGER.fine("propertyChange() -- e=" + e);
        if (e.getSource() == settings)
        {
            // TO DO
        } else if (e.getSource() == songMidiMix)
        {
            if (e.getPropertyName() == MidiMix.PROP_CHANNEL_INSTRUMENT_MIX)
            {
                int channel = (int) e.getNewValue();
                RhythmVoice rv = songMidiMix.getKey(channel);
                InstrumentMix oldInsMix = (InstrumentMix) e.getOldValue();
                InstrumentMix insMix = songMidiMix.getInstrumentMixFromChannel(channel);
                updateVisibleRhythmUI();

                if (insMix == null)
                {
                    // InstrumentMix was removed
                    LOGGER.fine("propertyChange() InstrumentMix removed");
                    MixChannelPanel mcp = getMixChannelPanel(channel); // can be null if not visible
                    if (mcp != null)
                    {
                        removeMixChannelPanel(mcp);
                    }
                } else if (oldInsMix == null)
                {
                    // New InstrumentMix was added
                    LOGGER.fine("propertyChange() InstrumentMix added insMix=" + insMix);
                    if (getVisibleRhythm() == null || getVisibleRhythm() == rv.getContainer())
                    {
                        addMixChannelPanel(songMidiMix, channel);
                    }
                } else
                {
                    // InstrumentMix is replacing an existing one
                    LOGGER.fine("propertyChange() InstrumentMix replaced");
                    MixChannelPanel mcp = getMixChannelPanel(channel);
                    if (mcp != null)
                    {
                        removeMixChannelPanel(mcp);
                    }
                    if (getVisibleRhythm() == null || getVisibleRhythm() == rv.getContainer())
                    {
                        addMixChannelPanel(songMidiMix, channel);
                    }
                }
            } else if (e.getPropertyName() == Song.PROP_MODIFIED_OR_SAVED)
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
            if (e.getPropertyName() == SongEditorManager.PROP_SONG_CLOSED)
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
            if (e.getPropertyName() == ActiveSongManager.PROP_ACTIVE_SONG)
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
        LOGGER.log(Level.FINE, "songPresenceChanged() -- song=" + song + " songModel=" + songModel);
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
            String msg = "Could not retrieve MidiMix for song " + song.getName() + ".\n" + ex.getLocalizedMessage();
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
        songMidiMix.addPropertyListener(this);

        // Add the visible channel panels
        addVisibleMixChannelPanels();

        LOGGER.fine("   songMidiMix=" + songMidiMix);
    }

    private void addVisibleMixChannelPanels()
    {
        // Add the MixChannelPanels
        for (Integer channel : songMidiMix.getUsedChannels(getVisibleRhythm()))
        {
            // Add a MixChannelPanel for each InstrumentMix
            addMixChannelPanel(songMidiMix, channel);
        }
    }

    private void addMixChannelPanel(MidiMix mm, int channel)
    {
        MixChannelPanel mcp;
        RhythmVoice rvKey = songMidiMix.getKey(channel);
        if (rvKey instanceof UserChannelRhythmVoiceKey)
        {
            // User channel
            mcp = createMixChannelPanelForUserVoice(mm, channel);
        } else
        {
            // Rhythm channel
            mcp = createMixChannelPanelForRhythmVoice(mm, channel, rvKey);
        }
        insertMixChannelPanel(channel, mcp);
    }

    private MixChannelPanel createMixChannelPanelForRhythmVoice(MidiMix mm, int channel, RhythmVoice rv)
    {
        LOGGER.fine("createMixChannelPanelForRhythmVoice() -- mm=" + mm + " channel=" + channel + " rv=" + rv);
        MixChannelPanelModelImpl mcpModel = new MixChannelPanelModelImpl(mm, channel);
        MixChannelPanelControllerImpl mcpController = new MixChannelPanelControllerImpl(mm, channel);
        MixChannelPanel mcp = new MixChannelPanel(mcpModel, mcpController);
        Rhythm r = rv.getContainer();
        Color c = this.mapRhythmColor.get(r);
        if (c == null)
        {
            // Get the color and save it
            c = CHANNEL_COLORS[colorIndex];
            colorIndex = (colorIndex == CHANNEL_COLORS.length - 1) ? 0 : colorIndex + 1;
            mapRhythmColor.put(r, c);
        }
        mcp.setChannelColor(c);
        mcp.setChannelName(r.getName(), rv.getName());
        Icon icon;
        switch (rv.getType())
        {
            case Drums:
                icon = new ImageIcon(getClass().getResource("resources/Drums-48x48.png"));
                break;
            case Guitar:
                icon = new ImageIcon(getClass().getResource("resources/Guitar-48x48.png"));
                break;
            case Keyboard:
                icon = new ImageIcon(getClass().getResource("resources/Keyboard-48x48.png"));
                break;
            case Percussion:
                icon = new ImageIcon(getClass().getResource("resources/Percu-48x48.png"));
                break;
            case Bass:
                icon = new ImageIcon(getClass().getResource("resources/Bass-48x48.png"));
                break;
            case Horn_Section:
                icon = new ImageIcon(getClass().getResource("resources/HornSection-48x48.png"));
                break;
            case Pad:
                icon = new ImageIcon(getClass().getResource("resources/Strings-48x48.png"));
                break;
            default: // Accompaniment
                icon = new ImageIcon(getClass().getResource("resources/Notes-48x48.png"));
        }
        mcp.setIcon(icon);
        mcp.setIconToolTipText(rv.getName());
        return mcp;

    }

    private MixChannelPanel createMixChannelPanelForUserVoice(MidiMix mm, int channel)
    {
        LOGGER.fine("createMixChannelPanelForUserVoice() -- mm=" + mm + " channel=" + channel);
        MixChannelPanelModelImpl mcpModel = new MixChannelPanelModelImpl(mm, channel);
        MixChannelPanelControllerImpl mcpController = new MixChannelPanelControllerImpl(mm, channel);
        MixChannelPanel mcp = new MixChannelPanel(mcpModel, mcpController);
        mcp.setChannelColor(CHANNEL_COLOR_USER);
        mcp.setChannelName("User", "Channel");
        Icon icon = new ImageIcon(getClass().getResource(USER_ICON_PATH));
        mcp.setIcon(icon);
        mcp.setIconToolTipText(null);
        return mcp;
    }

    private void removeMixChannelPanel(MixChannelPanel mcp)
    {
        mcp.cleanup();
        panel_mixChannels.remove(mcp);
        panel_mixChannels.revalidate();;
        panel_mixChannels.repaint();
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
        panel_mixChannels.revalidate();;
        panel_mixChannels.repaint();
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
        if (songModel == null || (rhythms = SongStructure.Util.getUniqueRhythms(songModel.getSongStructure())).size() < 2)
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
            assert um != null;
            songMidiMix.removeUndoableEditListener(um);
        }
        if (saveAsCapableSong != null)
        {
            instanceContent.remove(saveAsCapableSong);
        }
        resetSongModified();
        if (songMidiMix != null)
        {
            songMidiMix.removePropertyListener(this);
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
        LOGGER.fine("updateActiveState() -- b=" + b);
        org.jjazz.ui.utilities.Utilities.setRecursiveEnabled(b, menuBar);
        org.jjazz.ui.utilities.Utilities.setRecursiveEnabled(b, panel_MasterControls);
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
                String s = r.getName().length() > 10 ? r.getName().substring(0, 10) + "..." : r.getName();
                setText(s);
                setToolTipText(r.getName());
            }
            return c;
        }
    }

}
