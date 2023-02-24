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
package org.jjazz.pianoroll.api;

import org.jjazz.pianoroll.BackgroundPhraseManager;
import com.google.common.base.Preconditions;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.QuantizePanel;
import org.jjazz.pianoroll.BackgroundPhrasesPanel;
import org.jjazz.pianoroll.ToolbarPanel;
import org.jjazz.pianoroll.actions.PasteNotes;
import org.jjazz.pianoroll.actions.PlayEditor;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.ui.utilities.api.CollapsiblePanel;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.UndoRedo;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


/**
 * A TopComponent to use a PianoRollEditor for a song phrase.
 * <p>
 * The TopComponent closes itself when song is closed and listen to SongStructure changes to update the edited range.
 */
public final class PianoRollEditorTopComponent extends TopComponent implements PropertyChangeListener, SgsChangeListener
{

    // public static final String MODE = "midieditor";  // WindowManager mode
    public static final String MODE = "editor";  // WindowManager mode


    private final PianoRollEditor editor;
    private final ToolbarPanel toolbarPanel;
    private final QuantizePanel quantizePanel;
    private final BackgroundPhrasesPanel backgroundPhrasesPanel;
    private final Song song;
    private MidiMix midiMix;
    private SongPart songPart;
    private String titleBase;
    private BackgroundPhraseManager backgroundPhraseManager;
    private static final Logger LOGGER = Logger.getLogger(PianoRollEditorTopComponent.class.getSimpleName());


    /**
     * Create a TopComponent editor for the specified song.
     * <p>
     *
     * @param sg       The TopComponent listens to song structure changes to update the edited range.
     * @param settings
     */
    public PianoRollEditorTopComponent(Song sg, PianoRollEditorSettings settings)
    {
        Preconditions.checkNotNull(sg);
        Preconditions.checkNotNull(settings);

        LOGGER.log(Level.FINE, "PianoRollEditorTopComponent() -- sg={0}", sg);

        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.FALSE);
        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.FALSE);


        // Assign to mode
        // https://dzone.com/articles/secrets-netbeans-window-system
        // https://web.archive.org/web/20170314072532/https://blogs.oracle.com/geertjan/entry/creating_a_new_mode_in        
        Mode mode = WindowManager.getDefault().findMode(PianoRollEditorTopComponent.MODE);
        assert mode != null;
        assert mode.dockInto(this);


        this.song = sg;
        setDisplayName(getDefaultTabName(song));
        try
        {
            midiMix = MidiMixManager.getInstance().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }


        editor = new PianoRollEditor(settings);
        editor.setSong(song);


        toolbarPanel = new ToolbarPanel(this, song.getName());


        // WEIRD: only for the callback Paste action, we need BOTH the action here and in the lookup (see PianoRollEditor constructor)
        // to make paste work... 
        getActionMap().put("paste-from-clipboard", new PasteNotes(editor));


        initComponents();
        splitpane_tools_editor.setRightComponent(editor);


        // Update the CollapsiblePanels
        cpan_quantize.getContentPane().setLayout(new BorderLayout());
        quantizePanel = new QuantizePanel(editor);
        cpan_quantize.getContentPane().add(quantizePanel, BorderLayout.CENTER);
        cpan_showTracks.getContentPane().setLayout(new BorderLayout());
        backgroundPhrasesPanel = new BackgroundPhrasesPanel();
        cpan_showTracks.getContentPane().add(backgroundPhrasesPanel, BorderLayout.CENTER);
        // Reset splitpane divider location when a panel is collapsed/expanded
        cpan_quantize.addPropertyChangeListener(CollapsiblePanel.PROP_COLLAPSED,
                e -> SwingUtilities.invokeLater(() -> updateDividerLocation()));
        cpan_showTracks.addPropertyChangeListener(CollapsiblePanel.PROP_COLLAPSED,
                e -> SwingUtilities.invokeLater(() -> updateDividerLocation()));


        // Manage the background phrases
        backgroundPhraseManager = new BackgroundPhraseManager(this, backgroundPhrasesPanel);


        // Automatically close when song is closed
        song.addPropertyChangeListener(this);


        // Listen to song structure changes: editor bounds can be impacted
        song.getSongStructure().addSgsChangeListener(this);


        refreshToolbarTitle();
    }

    /**
     * Configure the TopComponent to edit a custom phrase for a song part via its RP_SYS_CustomPhrase rhythm parameter.
     * <p>
     *
     * @param spt            Must belong to the song
     * @param rpCustomPhrase The RP_SYS_CustomPhrase rhythm parameter used by the song part rhythm
     * @param p              The phrase must start at bar/beat 0 (independently of spt start position)
     * @param channel        The Midi channel of the edited phrase (p.getChannel() is ignored). Must correspond to a RhythmVoice of the song
     *                       part rhythm.
     * @param keyMap         Null for melodic phrase
     */
    public void setModelForRP_SYS_CustomPhrase(SongPart spt, RP_SYS_CustomPhrase rpCustomPhrase, Phrase p, int channel, DrumKit.KeyMap keyMap)
    {
        Preconditions.checkNotNull(p);
        Preconditions.checkNotNull(rpCustomPhrase);
        Preconditions.checkNotNull(spt);
        Preconditions.checkArgument(spt.getRhythm().getRhythmParameters().contains(rpCustomPhrase), "rpCustomPhrase=%s rhythm=%s",
                rpCustomPhrase, spt.getRhythm());
        Preconditions.checkArgument(song.getSongStructure().getSongParts().contains(spt));
        Preconditions.checkArgument(midiMix.getUsedChannels(spt.getRhythm()).contains(channel), "channel=%s midiMix=%s", channel, midiMix);


        songPart = spt;
        var beatRange = getBeatRange();
        var beatRange0 = beatRange.getTransformed(-beatRange.from);          // phrase starts at beat 0
        TreeMap<Float, TimeSignature> mapPosTs = new TreeMap<>();
        mapPosTs.put(0f, songPart.getRhythm().getTimeSignature());

        editor.setModel(p, beatRange0, 0, spt.getStartBarIndex(), channel, mapPosTs, keyMap);

        refreshToolbarTitle();


        backgroundPhraseManager.updateTrackNames();
        backgroundPhraseManager.setRpCustomPhrase(rpCustomPhrase);
    }

    /**
     * Configure the TopComponent to edit a user phrase on the whole song.
     * <p>
     *
     * @param p
     * @param channel The Midi channel of the edited Phrase (p.getChannel() is ignored). Must correspond to a UserRhythmVoice in the song's
     *                MidiMix.
     * @param keyMap  Null for melodic phrase
     */
    public void setModelForUserPhrase(Phrase p, int channel, DrumKit.KeyMap keyMap)
    {
        Preconditions.checkNotNull(p);
        Preconditions.checkArgument(midiMix.getUserChannels().contains(channel), "channel=%s", channel);

        var ss = song.getSongStructure();
        var spts = ss.getSongParts();
        if (spts.isEmpty())
        {
            return;
        }

        songPart = null;
        TreeMap<Float, TimeSignature> mapPosTs = new TreeMap<>();
        spts.forEach(spt -> mapPosTs.put(ss.getPositionInNaturalBeats(spt.getStartBarIndex()), spt.getRhythm().getTimeSignature()));


        editor.setModel(p, getBeatRange(), 0, 0, channel, mapPosTs, keyMap);


        refreshToolbarTitle();
        backgroundPhraseManager.updateTrackNames();
        backgroundPhraseManager.setRpCustomPhrase(null);
    }


    /**
     * The title used within the editor.
     *
     * @return
     */
    public String getTitle()
    {
        return titleBase;
    }

    /**
     * Set the title base used within the editor.
     *
     * @param title
     */
    public void setTitle(String title)
    {
        titleBase = title;
        refreshToolbarTitle();
    }

    /**
     * The song associated to this TopComponent.
     *
     * @return
     */
    public Song getSong()
    {
        return song;
    }

    /**
     * The MidiMix associated to the song.
     *
     * @return
     */
    public MidiMix getMidiMix()
    {
        return midiMix;
    }

    /**
     * The RhythmVoice associated to the
     *
     * @return
     */
    public RhythmVoice getRhythmVoice()
    {
        return midiMix.getRhythmVoice(editor.getChannel());
    }

    /**
     * The edited SongPart, or null if the whole song is edited.
     *
     * @return
     */
    public SongPart getSongPart()
    {
        return songPart;
    }

    /**
     * The edited beat range.
     *
     * @return
     */
    public FloatRange getBeatRange()
    {
        var ss = song.getSongStructure();
        return isRP_SYS_CustomPhraseMode() ? ss.getBeatRange(songPart.getBarRange()) : ss.getBeatRange(null);
    }

    /**
     * The edited bar range.
     *
     * @return
     */
    public IntRange getBarRange()
    {
        var ss = song.getSongStructure();
        return isRP_SYS_CustomPhraseMode() ? songPart.getBarRange() : ss.getBarRange();
    }

    /**
     * Check if TopComponent was last configured via setModelForRP_SYS_CustomPhrase().
     *
     * @return
     */
    public boolean isRP_SYS_CustomPhraseMode()
    {
        return songPart != null;
    }

    /**
     * @return For example "PianoRollEditorTopComponent-MySongName"
     */
    @Override
    public String preferredID()
    {
        String name = song != null ? song.getName() : "no_song";
        return "PianoRollEditorTopComponent-" + name;
    }

    public PianoRollEditor getEditor()
    {
        return editor;
    }

    @Override
    public UndoRedo getUndoRedo()
    {
        return editor.getUndoManager();
    }

    /**
     * Overridden to insert possible new actions from path "Actions/PianoRollEditorTopComponent".
     *
     * @return The actions to be shown in the TopComponent popup menu.
     */
    @Override
    public Action[] getActions()
    {
        List<? extends Action> newActions = Utilities.actionsForPath("Actions/PianoRollEditorTopComponent");
        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(newActions);
        if (!newActions.isEmpty())
        {
            actions.add(null);   // Separator         
        }
        Collections.addAll(actions, super.getActions()); // Get the standard builtin actions Close, Close All, Close Other      
        return actions.toArray(new Action[0]);
    }

    @Override
    public Lookup getLookup()
    {
        return editor.getLookup();
    }

    @Override
    public int getPersistenceType()
    {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    public boolean canClose()
    {
        return true;
    }

    @Override
    public void componentOpened()
    {

    }

    @Override
    public void componentClosed()
    {
        LOGGER.fine("componentClosed() -- ");
        song.removePropertyChangeListener(this);
        song.getSongStructure().removeSgsChangeListener(this);
        quantizePanel.cleanup();
        backgroundPhraseManager.cleanup();
        editor.cleanup();
        toolbarPanel.cleanup();
    }

    /**
     * Return the active (i.e. focused or ancestor of the focused component) PianoRollEditorTopComponent.
     *
     * @return Can be null
     */
    static public PianoRollEditorTopComponent getActive()
    {
        TopComponent tc = TopComponent.getRegistry().getActivated();
        return (tc instanceof PianoRollEditorTopComponent) ? (PianoRollEditorTopComponent) tc : null;
    }

    /**
     * Search for the PianoRollEditorTopComponent associated to song.
     *
     * @param song
     * @return Can be null
     */
    static public PianoRollEditorTopComponent get(Song song)
    {
        Set<TopComponent> tcs = TopComponent.getRegistry().getOpened();
        return tcs.stream()
                .filter(tc -> tc instanceof PianoRollEditorTopComponent preTc && preTc.getSong() == song)
                .map(tc -> (PianoRollEditorTopComponent) tc)
                .findAny()
                .orElse(null);
    }


    /**
     * The default tab name for a song.
     *
     * @param song
     * @return
     */
    static public String getDefaultTabName(Song song)
    {
        return ResUtil.getString(PianoRollEditorTopComponent.class, "PianoRollEditorTabName", song.getName());
    }


    //=============================================================================
    // PropertyChangeListener interface
    //=============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == song)
        {
            if (evt.getPropertyName().equals(Song.PROP_CLOSED))
            {
                close();
            } else if (evt.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED_OR_RESET))
            {
                if (evt.getOldValue().equals(Boolean.TRUE))
                {
                    // File was saved
                    setDisplayName(getDefaultTabName(song));
                }
            }
        }
    }

    //=============================================================================
    // SgsChangeListener interface
    //=============================================================================
    @Override
    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void songStructureChanged(SgsChangeEvent e)
    {

        // Use high-level actions to not be polluted by intermediate states
        if (e instanceof SgsActionEvent evt && evt.isActionComplete())
        {
            LOGGER.log(Level.FINE, "songStructureChanged() actionId={0}", evt.getActionId());


            if (evt.getActionId().startsWith("setRhythmParameterValue"))
            {
                // No impact on structure change
                return;
            }

            // Check if the underlying model is gone
            var allSpts = getSong().getSongStructure().getSongParts();
            if (allSpts.isEmpty() || (isRP_SYS_CustomPhraseMode() && !allSpts.contains(songPart)))
            {
                close();
                return;
            }


            // Refresh the editor when the bar range can be impacted
            if (!isRP_SYS_CustomPhraseMode())
            {
                setModelForUserPhrase(editor.getModel(), editor.getChannel(), editor.getDrumKeyMap());
            } else
            {
                setModelForRP_SYS_CustomPhrase(songPart, backgroundPhraseManager.getRpCustomPhrase(), editor.getModel(), editor.getChannel(),
                        editor.getDrumKeyMap());
            }
        }
    }
    // ============================================================================================
    // Private methods
    // ============================================================================================

    private void refreshToolbarTitle()
    {
        var barRange = getBarRange();
        String strSongPart = isRP_SYS_CustomPhraseMode() ? " - " + songPart.getName() : "";
        String strBarRange = " - bars " + (barRange.from + 1) + ".." + (barRange.to + 1);
        String strTs = isRP_SYS_CustomPhraseMode() ? " - " + songPart.getRhythm().getTimeSignature() : "";
        String title = titleBase + strSongPart + strBarRange + strTs;
        toolbarPanel.setTitle(title);
    }

    private void updateDividerLocation()
    {
        splitpane_tools_editor.resetToPreferredSizes();
//        int leftWidth = sidePanel.getPreferredSize().width;
//        leftWidth += splitpane_tools_editor.getInsets().left;
//        splitpane_tools_editor.setDividerLocation(leftWidth);
    }


    void writeProperties(java.util.Properties p)
    {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p)
    {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_toolbar = toolbarPanel;
        splitpane_tools_editor = new javax.swing.JSplitPane();
        sidePanel = new javax.swing.JPanel();
        cpan_quantize = new org.jjazz.ui.utilities.api.CollapsiblePanel();
        cpan_showTracks = new org.jjazz.ui.utilities.api.CollapsiblePanel();
        jButton1 = new javax.swing.JButton();

        setToolTipText(org.openide.util.NbBundle.getMessage(PianoRollEditorTopComponent.class, "PianoRollEditorTopComponent.toolTipText")); // NOI18N
        setLayout(new java.awt.BorderLayout());
        add(pnl_toolbar, java.awt.BorderLayout.NORTH);

        splitpane_tools_editor.setDividerSize(20);
        splitpane_tools_editor.setOneTouchExpandable(true);

        cpan_quantize.setTitleComponentText(org.openide.util.NbBundle.getMessage(PianoRollEditorTopComponent.class, "PianoRollEditorTopComponent.cpan_quantize.titleComponentText")); // NOI18N

        cpan_showTracks.setTitleComponentText(org.openide.util.NbBundle.getMessage(PianoRollEditorTopComponent.class, "PianoRollEditorTopComponent.cpan_showTracks.titleComponentText")); // NOI18N

        javax.swing.GroupLayout sidePanelLayout = new javax.swing.GroupLayout(sidePanel);
        sidePanel.setLayout(sidePanelLayout);
        sidePanelLayout.setHorizontalGroup(
            sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cpan_quantize, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(cpan_showTracks, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
        );
        sidePanelLayout.setVerticalGroup(
            sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidePanelLayout.createSequentialGroup()
                .addComponent(cpan_quantize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cpan_showTracks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(187, Short.MAX_VALUE))
        );

        splitpane_tools_editor.setLeftComponent(sidePanel);

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, "jButton1"); // NOI18N
        splitpane_tools_editor.setRightComponent(jButton1);

        add(splitpane_tools_editor, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.ui.utilities.api.CollapsiblePanel cpan_quantize;
    private org.jjazz.ui.utilities.api.CollapsiblePanel cpan_showTracks;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel pnl_toolbar;
    private javax.swing.JPanel sidePanel;
    private javax.swing.JSplitPane splitpane_tools_editor;
    // End of variables declaration//GEN-END:variables


}
