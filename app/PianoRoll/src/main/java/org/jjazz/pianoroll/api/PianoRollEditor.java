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
package org.jjazz.pianoroll.api;

import com.google.common.base.Preconditions;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsActionEvent;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.pianoroll.EditToolBar;
import org.jjazz.pianoroll.NotesPanel;
import org.jjazz.pianoroll.RulerPanel;
import org.jjazz.pianoroll.actions.CopyNotes;
import org.jjazz.pianoroll.actions.CutNotes;
import org.jjazz.pianoroll.actions.DecreaseSelectionVelocity;
import org.jjazz.pianoroll.actions.DeleteSelection;
import org.jjazz.pianoroll.actions.IncreaseSelectionVelocity;
import org.jjazz.pianoroll.actions.MoveSelectionLeft;
import org.jjazz.pianoroll.actions.MoveSelectionRight;
import org.jjazz.pianoroll.actions.PasteNotes;
import org.jjazz.pianoroll.actions.ResizeSelection;
import org.jjazz.pianoroll.actions.SelectAllNotes;
import org.jjazz.pianoroll.actions.TransposeSelectionDown;
import org.jjazz.pianoroll.actions.TransposeSelectionUp;
import org.jjazz.pianoroll.actions.ZoomToFit;
import org.jjazz.pianoroll.edittools.EraserTool;
import org.jjazz.pianoroll.edittools.PencilTool;
import org.jjazz.pianoroll.edittools.SelectionTool;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.song.api.Song;
import org.jjazz.instrumentcomponents.keyboard.api.KeyboardComponent;
import org.jjazz.instrumentcomponents.keyboard.api.KeyboardRange;
import org.jjazz.pianoroll.BottomControlPanel;
import org.jjazz.pianoroll.EditorPanel;
import org.jjazz.pianoroll.ScorePanel;
import org.jjazz.pianoroll.VelocityPanel;
import org.jjazz.pianoroll.actions.InvertNoteSelection;
import org.jjazz.pianoroll.actions.JumpToEnd;
import org.jjazz.pianoroll.actions.JumpToStart;
import org.jjazz.pianoroll.actions.PlayFromHere;
import static org.jjazz.pianoroll.actions.PlayFromHere.KEYSTROKE;
import org.jjazz.rhythmmusicgeneration.api.ChordSequence;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.uiutilities.api.SingleFileDragInTransferHandler;
import org.jjazz.uiutilities.api.UIUtilities;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.uiutilities.api.Zoomable;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * A piano roll editor of a musical phrase.
 * <p>
 * Optional view-only "ghost phrases" can be shown faded in the background of the editor, based on the ghostPhrasesModel state.
 * <p>
 * Editor's lookup must contain :<br>
 * - its ActionMap instance<br>
 * - a Zoomable instance
 */
public class PianoRollEditor extends JPanel implements PropertyChangeListener, ClsChangeListener
{

    /**
     * newValue=false. This property change event is fired ONLY once, when the editor is destroyed (cleanup() is called).
     */
    public static final String PROP_EDITOR_ALIVE = "EditorAlive";
    /**
     * oldValue=old phrase model, newValue=new Phrase model.
     */
    public static final String PROP_MODEL_PHRASE = "PhraseModel";
    /**
     * oldValue=old channel, newValue=new channel model.
     */
    public static final String PROP_MODEL_CHANNEL = "PhraseChannel";
    /**
     * oldValue=sorted list of NoteViews whose state has changed, newValue=selected state
     */
    public static final String PROP_SELECTED_NOTE_VIEWS = "NoteViewSelection";
    /**
     * oldValue=old tool, newValue=new tool
     */
    public static final String PROP_ACTIVE_TOOL = "ActiveTool";
    /**
     * oldValue=old loop zone, newValue=new loop zone
     */
    public static final String PROP_LOOP_ZONE = "LoopZone";
    /**
     * newValue=boolean
     */
    public static final String PROP_SNAP_ENABLED = "SnapEnabled";
    /**
     * oldValue=old quantization value, newValue=new quantization value
     */
    public static final String PROP_QUANTIZATION = "Quantization";
    /**
     * newValue=playback point position in beats
     */
    public static final String PROP_PLAYBACK_POINT_POSITION = "PlaybackPointPosition";
    /**
     * oldValue=old ChordSequence newValue=new ChordSequence
     */
    public static final String PROP_CHORD_SEQUENCE = "ChordSequence";
    private static final float MAX_WIDTH_FACTOR = 1.5f;
    private static SessionUISettings lastSessionUISettings = new SessionUISettings();
    private JSplitPane splitPane_TopBottom;
    private NotesPanel notesPanel;
    private VelocityPanel velocityPanel;
    private ScorePanel scorePanel;
    private BottomControlPanel bottomControlPanel;
    private final List<EditorPanel> editorPanels;
    private KeyboardComponent keyboard;
    private RulerPanel rulerPanel;
    private JScrollPane scrollPaneEditor;
    private JScrollPane bottomScrollPane;
    private JPanel pnl_keyboard;
    private ZoomValue zoomValue;
    private Phrase model;
    private IntRange loopZone;
    private DrumKit.KeyMap keyMap;
    private final PianoRollEditorSettings settings;
    private Quantization quantization;
    private final Lookup lookup;
    private JJazzUndoManager undoManager;
    private final InstanceContent generalLookupContent;
    private int rulerStartBar;
    private EditTool activeTool;
    private final NoteSelection noteSelection;
    private final ShowKeyMouseListener showKeyMouseListener;
    private final SelectionRectangleMouseListener selectionRectangleMouseListener;
    private final EditToolProxyMouseListener editToolProxyMouseListener;
    private final ZoomEditorMouseListener zoomEditorMouseListener;
    private final MoveEditorMouseListener moveXEditorMouseListener;
    private boolean snapEnabled;
    private float playbackPointPosition;
    private boolean playbackAutoScrollEnabled;
    private final List<EditTool> editTools;
    private Song song;
    private IntRange barRange;
    private FloatRange beatRange;
    private int channel = 0;
    private NavigableMap<Float, TimeSignature> mapPosTimeSignature;
    private final GhostPhrasesModel ghostPhrasesModel;
    private ChordSequence chordSequence;
    private static final Logger LOGGER = Logger.getLogger(PianoRollEditor.class.getSimpleName());


    /**
     * Create a piano roll editor for a dummy phrase model.
     *
     * @param settings          Can't be null
     * @param ghostPhrasesModel Can't be null
     */
    public PianoRollEditor(PianoRollEditorSettings settings, GhostPhrasesModel ghostPhrasesModel)
    {
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(ghostPhrasesModel);


        LOGGER.fine("PianoRollEditor() -- ");

        this.settings = settings;
        this.ghostPhrasesModel = ghostPhrasesModel;
        this.rulerStartBar = 0;
        this.model = new Phrase(0, false);
        this.channel = 0;
        this.beatRange = new FloatRange(0, 8f);
        this.barRange = new IntRange(0, 1);
        this.keyMap = null;
        this.quantization = Quantization.ONE_QUARTER_BEAT;
        this.mapPosTimeSignature = new TreeMap<>();
        this.snapEnabled = true;
        this.mapPosTimeSignature.put(0f, TimeSignature.FOUR_FOUR);
        this.loopZone = null;
        this.noteSelection = new NoteSelection();
        this.playbackAutoScrollEnabled = true;                  // MusicController.getInstance().isPlaying();

        // Be notified of changes, note added, moved, removed, set
        model.addPropertyChangeListener(this);


        // Be notified about ghost phrase visible state changes
        ghostPhrasesModel.addPropertyChangeListener(this);


        // Default undo manager to listen for model changes
        undoManager = new JJazzUndoManager();
        model.addUndoableEditListener(undoManager);


        // The lookup for other stuff, before createUI()
        generalLookupContent = new InstanceContent();
        var zoomable = new PianoRollZoomable();
        generalLookupContent.add(zoomable);
        generalLookupContent.add(getActionMap());
        lookup = new AbstractLookup(generalLookupContent);


        editTools = Arrays.asList(new SelectionTool(this), new PencilTool(this), new EraserTool(this));
        activeTool = editTools.get(0);


        // Build the UI
        createUI();

        editorPanels = List.of(notesPanel, velocityPanel, scorePanel);

        // Install mouse listeners
        editToolProxyMouseListener = new EditToolProxyMouseListener();
        zoomEditorMouseListener = new ZoomEditorMouseListener();
        moveXEditorMouseListener = new MoveEditorMouseListener(true);
        showKeyMouseListener = new ShowKeyMouseListener();
        selectionRectangleMouseListener = new SelectionRectangleMouseListener();
        var moveXYEditorMouseListener = new MoveEditorMouseListener(false);

        selectionRectangleMouseListener.install(notesPanel);
        showKeyMouseListener.install(notesPanel);
        editToolProxyMouseListener.install(notesPanel);
        moveXYEditorMouseListener.install(notesPanel);
        zoomEditorMouseListener.install(notesPanel);

        moveXEditorMouseListener.install(rulerPanel);
        zoomEditorMouseListener.install(rulerPanel);
        moveXEditorMouseListener.install(velocityPanel);
        zoomEditorMouseListener.install(velocityPanel);
        moveXEditorMouseListener.install(scorePanel);
        zoomEditorMouseListener.install(scorePanel);


        // By default enable the drag in transfer handler
        notesPanel.setTransferHandler(new MidiFileDragInTransferHandlerImpl());


        addKeyboardActions();


        // Normal zoom
        zoomValue = new ZoomValue(20, keyMap == null ? 30 : 60);        // Drum notes need more heigth
        notesPanel.setScaleFactorX(toScaleFactorX(zoomValue.hValue()));
        float yFactor = toScaleFactorY(zoomValue.vValue());
        keyboard.setScaleFactor(yFactor, Math.min(MAX_WIDTH_FACTOR, yFactor));


        // Add the notes
        addNotes(model.getNotes());


        updateGhostPhrases();


        SwingUtilities.invokeLater(() -> 
        {
            if (lastSessionUISettings.zoomH >= 0)
            {
                zoomable.setZoomXFactor(lastSessionUISettings.zoomH, false);
            }
            if (lastSessionUISettings.zoomV >= 0)
            {
                zoomable.setZoomYFactor(lastSessionUISettings.zoomV, false);
            }
        });

    }

    public KeyboardComponent getKeyboard()
    {
        return keyboard;
    }

    /**
     * Associate an optional song to the editor -this method can be called only once.
     * <p>
     * - Song isput in the editor's lookup<br>
     * - Song undo manager is used for undo/redo<br>
     * - Song can be used by subpanels to show e.g. chord symbols.<br>
     * - The Chord sequence associated to the edited phrase is kept uptodate.<br>
     *
     * @param song Can't be null
     * @see #getChordSequence()
     */
    public void setSong(Song song)
    {
        Preconditions.checkNotNull(song);
        if (this.song != null)
        {
            throw new IllegalStateException("this.song is already set: " + this.song);
        }

        this.song = song;
        updateChordSequence();

        generalLookupContent.add(song);
        rulerPanel.setSong(song);
        scorePanel.setSong(song);
        setUndoManager(JJazzUndoManagerFinder.getDefault().get(getSong()));


        song.getChordLeadSheet().addClsChangeListener(this);
    }

    /**
     * Get the song the edited phrase belongs to.
     *
     * @return Might be null.
     * @see #setSong(org.jjazz.song.api.Song)
     */
    public Song getSong()
    {
        return song;
    }

    public GhostPhrasesModel getGhostPhrasesModel()
    {
        return ghostPhrasesModel;
    }

    /**
     * Get the channel of the editor.
     * <p>
     * The channel is used e.g. when "hear preview" or "solo mode" is activated, or when notes are imported from a dragged Midi file.
     *
     * @return
     * @see #setModel(org.jjazz.phrase.api.Phrase, org.jjazz.utilities.api.FloatRange, int, int, java.util.NavigableMap, org.jjazz.midi.api.DrumKit.KeyMap) 
     */
    public int getChannel()
    {
        return channel;
    }

    /**
     * The available EditTools.
     *
     * @return
     */
    public List<EditTool> getEditTools()
    {
        return editTools;
    }


    /**
     * Get the Phrase edited by this editor.
     *
     * @return Can not be null but can be empty. Starts at bar/beat=0.
     */
    public Phrase getModel()
    {
        return model;
    }

    /**
     * Set the phrase model.
     * <p>
     * Can fire PROP_MODEL_PHRASE and PROP_MODEL_CHANNEL change events.
     *
     * @param p             The phrase model. Must start at bar/beat 0.
     * @param beatRange     The beat range of the phrase model to edit. It must start at beat 0. It might be only a part of the phrase model or be longer.
     * @param rulerStartBar The start bar displayed on the ruler. Might be &gt; 0, e.g. for a custom phrase of a song part in the middle of the song.
     * @param channel       The Midi channel of the phrase model (p.getChannel() is ignored).
     * @param mapPosTs      The position of each time signature. Must have at least 1 entry at beatRange.from position or before.
     * @param kMap          If null means it's a melodic phrase
     */
    public void setModel(Phrase p, FloatRange beatRange, int rulerStartBar, int channel, NavigableMap<Float, TimeSignature> mapPosTs, DrumKit.KeyMap kMap)
    {
        Preconditions.checkNotNull(p);
        Preconditions.checkArgument(beatRange != null && beatRange.from == 0, "beatRange=%s", beatRange);
        Preconditions.checkArgument(rulerStartBar >= 0);
        Preconditions.checkArgument(mapPosTs != null && !mapPosTs.isEmpty() && mapPosTs.firstKey() <= beatRange.from, "mapPosTs=%s  beatRange=%s", mapPosTs,
                beatRange);

        LOGGER.log(Level.FINE, "setModel() -- p.size()={0} beatRange={1} rulerStartBar={2} channel={3} mapPosTs={4} kMap={5}", new Object[]
        {
            p.size(), beatRange, rulerStartBar, channel, mapPosTs, kMap
        });


        if (p == model
                && this.channel == channel
                && rulerStartBar == this.rulerStartBar
                && beatRange.equals(this.beatRange)
                && this.mapPosTimeSignature.equals(mapPosTs)
                && Objects.equals(kMap, this.keyMap))
        {
            return;
        }


        removeNotes(model.getNotes());


        model.removePropertyChangeListener(this);
        model.removeUndoableEditListener(undoManager);


        var oldModel = model;
        model = p;
        this.rulerStartBar = rulerStartBar;
        var oldLoopZone = loopZone;
        loopZone = null;
        keyMap = kMap;
        int oldChannel = channel;
        this.channel = channel;
        this.beatRange = beatRange;
        mapPosTimeSignature = mapPosTs;
        labelNotes(keyboard, keyMap);
        ghostPhrasesModel.setVisibleChannels(null);
        ghostPhrasesModel.setEditedChannel(channel);

        model.addPropertyChangeListener(this);
        model.addUndoableEditListener(undoManager);

        barRange = computePhraseBarRange();


        updateChordSequence();


        // Update the subcomponents                  
        notesPanel.getXMapper().refresh();
        rulerPanel.revalidate();
        rulerPanel.repaint();


        // Add the notes
        addNotes(model.getNotes());


        notesPanel.scrollToFirstNote();
        for (var ep : editorPanels)
        {
            ep.revalidate();
            ep.repaint();
        }


        firePropertyChange(PROP_MODEL_PHRASE, oldModel, model);
        firePropertyChange(PROP_MODEL_CHANNEL, oldChannel, this.channel);
        firePropertyChange(PROP_LOOP_ZONE, oldLoopZone, this.loopZone);
    }


    /**
     * Get the bar index displayed on the ruler corresponding to getBeatRange().from.
     * <p>
     * Usually identical to getPhraseStartBar(), but it can be different to make the edited range appear at a different bar in the ruler.
     *
     * @return
     */
    public int getRulerStartBar()
    {
        return rulerStartBar;
    }

    /**
     * Get the ruler bar range.
     * <p>
     * An IntRange starting at getRulerStartbar() with size equals to getPhraseBarRange().
     *
     * @return
     */
    public IntRange getRulerBarRange()
    {
        return new IntRange(rulerStartBar, rulerStartBar + getPhraseBarRange().size() - 1);
    }

    /**
     * The chord sequence associated to the edited phrase.
     * <p>
     * Important: Chord sequence first bar is equals to getRulerStartBar().
     *
     * @return Can be null if song is not set.
     * @see #toPhraseRelativeBeatPosition(org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol)
     */
    public ChordSequence getChordSequence()
    {
        return chordSequence;
    }

    /**
     * Compute the phrase-based beat position of a CLI_ChordSymbol obtained via getChordSequence().
     *
     * @param cliCs
     * @return
     * @see #getChordSequence()
     */
    public float toPhraseRelativeBeatPosition(CLI_ChordSymbol cliCs)
    {
        var posOffsetted = cliCs.getPosition().getMoved(-getRulerStartBar(), 0);
        var res = toPositionInBeats(posOffsetted);
        return res;
    }

    /**
     * The time signature at the specified beat position.
     *
     * @param posInBeats Must be in the beat range
     * @return Can't be null
     */
    public TimeSignature getTimeSignature(float posInBeats)
    {
        Preconditions.checkArgument(beatRange.contains(posInBeats, false));
        return mapPosTimeSignature.floorEntry(posInBeats).getValue();
    }

    /**
     * Get all the time signatures with their position.
     *
     * @return Can't be empty.
     */
    public NavigableMap<Float, TimeSignature> getTimeSignatures()
    {
        return mapPosTimeSignature;
    }

    /**
     * Get the phase beat range of the edited phrase.
     *
     * @return A FloatRange with from=0.
     */
    public FloatRange getPhraseBeatRange()
    {
        return beatRange;
    }

    /**
     * Get the bar range of the edited phrase.
     *
     * @return An IntRange with from=0.
     */
    public IntRange getPhraseBarRange()
    {
        return barRange;
    }

    /**
     * Get the lookup of this editor.
     * <p>
     * Lookup contains
     *
     * @return
     */
    public Lookup getLookup()
    {
        return lookup;
    }


    /**
     * Convenience method which returns true if getDrumKeyMap() is non null.
     *
     * @return
     */
    public boolean isDrums()
    {
        return getDrumKeyMap() != null;
    }


    /**
     * The drum key map used by the edited phrase.
     *
     * @return Null if it's a melodic phrase.
     */
    public DrumKit.KeyMap getDrumKeyMap()
    {
        return keyMap;
    }


    /**
     * Get the graphical settings of this editor.
     *
     * @return
     */
    public PianoRollEditorSettings getSettings()
    {
        return settings;
    }

    /**
     * Clean up everything so component can be garbaged.
     * <p>
     * Fire a PROP_EDITOR_ALIVE with value=false.
     */
    public void cleanup()
    {
        LOGGER.fine("cleanup() --");

        // Save some UI settings for the session
        double h = splitPane_TopBottom.getHeight();
        double dividerWeight = Math.clamp(h > 0 ? splitPane_TopBottom.getDividerLocation() / h : 0.8d, 0, 1);
        lastSessionUISettings = new SessionUISettings(dividerWeight, getZoom().hValue(), getZoom().vValue());


        rulerPanel.cleanup();
        editorPanels.forEach(ep -> ep.cleanup());
        ghostPhrasesModel.removePropertyChangeListener(this);
        ghostPhrasesModel.cleanup();
        model.removeUndoableEditListener(undoManager);
        model.removePropertyChangeListener(this);
        if (song != null)
        {
            song.getChordLeadSheet().removeClsChangeListener(this);
        }
        firePropertyChange(PROP_EDITOR_ALIVE, true, false);
    }


    /**
     * Get the editor zoom value.
     *
     * @return
     */
    public ZoomValue getZoom()
    {
        return zoomValue;
    }


    /**
     * Set the display quantization.
     * <p>
     * Fire a PROP_QUANTIZATION change event.
     *
     * @param q Accepted values are BEAT, HALF_BEAT, ONE_THIRD_BEAT, ONE_QUARTER_BEAT, ONE_SIXTH_BEAT.
     */
    public void setQuantization(Quantization q)
    {
        Preconditions.checkArgument(EnumSet.of(Quantization.BEAT,
                Quantization.HALF_BEAT,
                Quantization.ONE_THIRD_BEAT,
                Quantization.ONE_QUARTER_BEAT,
                Quantization.ONE_SIXTH_BEAT).contains(q));
        if (quantization.equals(q))
        {
            return;
        }
        var old = quantization;
        quantization = q;
        firePropertyChange(PROP_QUANTIZATION, old, quantization);
    }

    /**
     * Get the display quantization.
     *
     * @return Can't be null
     */
    public Quantization getQuantization()
    {
        return quantization;
    }

    public boolean isNoteSelected(NoteEvent ne)
    {
        return noteSelection.getSelectedNoteEventsImpl().contains(ne);
    }

    public void selectNote(NoteEvent ne, boolean b)
    {
        selectNotes(List.of(ne), b);
    }

    /**
     * Select or unselect NoteViews.
     * <p>
     * Fire a PROP_SELECTED_NOTE_VIEWS change event.
     *
     * @param notes
     * @param b
     */
    public void selectNotes(Collection<NoteEvent> notes, boolean b)
    {
        noteSelection.selectNotesImpl(notes, b);
    }

    /**
     * Enable or disable the snap to quantization feature.
     * <p>
     * Fire a PROP_SNAP_ENABLED change event.
     *
     * @param b
     */
    public void setSnapEnabled(boolean b)
    {
        if (b == snapEnabled)
        {
            return;
        }
        snapEnabled = b;
        firePropertyChange(PROP_SNAP_ENABLED, !b, b);
    }

    /**
     * Check if the snap to quantization feature is enabled.
     *
     * @return
     */
    public boolean isSnapEnabled()
    {
        return snapEnabled;
    }

    /**
     * Get the NoteView from the main EditorPanel associated to the specified NoteEvent.
     *
     * @param ne
     * @return Can be null
     */
    public NoteView getNoteView(NoteEvent ne)
    {
        return notesPanel.getNoteView(ne);
    }

    /**
     * Get all the NoteViews from the main EditorPanel sorted by NoteEvent natural order.
     * <p>
     * @return
     */
    public List<NoteView> getNoteViews()
    {
        return notesPanel.getNoteViews();
    }

    /**
     * Unselect all notes.
     */
    public void unselectAll()
    {
        selectNotes(noteSelection.getSelectedNoteEventsImpl(), false);
    }

    /**
     * Get the currently selected NoteViews sorted by NoteEvent natural order.
     *
     * @return Unmodifiable list
     */
    public List<NoteView> getSelectedNoteViews()
    {
        return noteSelection.getSelectedNoteViewsImpl();
    }

    /**
     * Get the currently selected NoteEvents sorted by NoteEvent natural order.
     *
     * @return Unmodifiable list
     */
    public List<NoteEvent> getSelectedNoteEvents()
    {
        return noteSelection.getSelectedNoteEventsImpl();
    }

    /**
     * Set the active EditTool.
     * <p>
     * Fire a PROP_ACTIVE_TOOL change event.
     *
     * @param tool
     */
    public void setActiveTool(EditTool tool)
    {
        Preconditions.checkNotNull(tool);
        if (activeTool == tool)
        {
            return;
        }
        var old = activeTool;
        activeTool = tool;
        notesPanel.setCursor(activeTool.getCursor());
        firePropertyChange(PROP_ACTIVE_TOOL, old, activeTool);
    }

    /**
     * Get the actived EditTool.
     *
     * @return Can't be null
     */
    public EditTool getActiveTool()
    {
        return activeTool;
    }

    public boolean isPlaybackAutoScrollEnabled()
    {
        return playbackAutoScrollEnabled;
    }

    public void setPlaybackAutoScrollEnabled(boolean playbackAutoScrollEnabled)
    {
        this.playbackAutoScrollEnabled = playbackAutoScrollEnabled;
    }

    /**
     * Set the loop zone.
     *
     * @param barRange If null there is no loop zone. If not null must be contained in the phrase bar range.
     */
    public void setLoopZone(IntRange barRange)
    {
        Preconditions.checkArgument(barRange == null || getPhraseBarRange().contains(barRange), "barRange=%s", barRange);
        var old = loopZone;
        loopZone = barRange;
        if (!Objects.equals(old, loopZone))
        {
            notesPanel.repaint();
            rulerPanel.repaint();
            firePropertyChange(PROP_LOOP_ZONE, old, loopZone);
        }
    }

    /**
     * Get the loop zone.
     *
     * @return Can be null if no loop zone is set
     */
    public IntRange getLoopZone()
    {
        return loopZone;
    }

    /**
     * Show (or hide) a playback point in the editor at specified phrase position.
     * <p>
     * If pos is &lt; 0 or out of the editor bounds, nothing is shown. Fire a PROP_PLAYBACK_POINT_POSITION change event.
     *
     * @param pos The phrase position in beats.
     */
    public void showPlaybackPoint(float pos)
    {
        if (Float.floatToIntBits(pos) == Float.floatToIntBits(playbackPointPosition))
        {
            return;
        }
        float old = playbackPointPosition;
        playbackPointPosition = pos;


        int xPos = -1;
        if (getPhraseBeatRange().contains(playbackPointPosition, false))
        {
            xPos = notesPanel.getXMapper().getX(pos);
        }


        rulerPanel.showPlaybackPoint(xPos);
        for (var ep : editorPanels)
        {
            ep.showPlaybackPoint(xPos);
        }


        // Scroll if required so that playback point is on the left side
        var visibleBr = getVisibleBeatRange();
        if (!visibleBr.isEmpty() && xPos >= 0 && playbackAutoScrollEnabled && !visibleBr.contains(pos, true))
        {
            float shiftedPos = Math.min(getPhraseBeatRange().to - visibleBr.size() / 2, pos + visibleBr.size() / 2 - 1f);
            scrollToCenter(shiftedPos);
        }

        firePropertyChange(PROP_PLAYBACK_POINT_POSITION, old, playbackPointPosition);
    }

    /**
     * Get the playback point position.
     *
     * @return If &lt; 0 no playback point is shown.
     */
    public float getPlaybackPointPosition()
    {
        return playbackPointPosition;
    }

    /**
     * Return the phrase position in beats that corresponds to a graphical point in the NotesPanel.
     * <p>
     *
     * @param notesPanelPoint A point in NotesPanel's coordinates. -1 if point is not valid.
     * @return
     * @see #toNotesPanelPoint(java.awt.Point)
     */
    public float getPositionFromPoint(Point notesPanelPoint)
    {
        return notesPanel.getXMapper().getBeatPosition(notesPanelPoint.x);
    }

    /**
     * Convert a point in this editor coordinates into NotesPanel's coordinates.
     *
     * @param editorPoint
     * @return
     */
    public Point toNotesPanelPoint(Point editorPoint)
    {
        Objects.requireNonNull(editorPoint);
        return SwingUtilities.convertPoint(this, editorPoint, notesPanel);
    }


    /**
     * Return the X editor position that corresponds to a beat position of the Phrase model.
     *
     * @param pos
     * @return -1 If pos is outside the Phrase
     */
    public int getXFromPosition(float pos)
    {
        return getPhraseBeatRange().contains(pos, false) ? notesPanel.getXMapper().getX(pos) : -1;
    }

    /**
     * Convert a phrase Position into a phrase position in beats.
     *
     * @param pos Must be in the bar range.
     * @return A beat positino relative to the edited phrase (starts at beat 0)
     */
    public float toPositionInBeats(Position pos)
    {
        return notesPanel.getXMapper().getBeatPosition(pos);
    }

    /**
     * Convert a phrase position in beats into a Position.
     *
     * @param posInBeats Must be in the beat range.
     * @return A position relative to the edited phrase (starts at bar 0)
     */
    public Position toPosition(float posInBeats)
    {
        return notesPanel.getXMapper().getPosition(posInBeats);
    }

    /**
     * Return the pitch that correspond to a graphical point in the editor.
     *
     * @param notesPanelPoint A point in the editor's coordinates. -1 if point is not valid.
     * @return
     */
    public int getPitchFromPoint(Point notesPanelPoint)
    {
        return notesPanel.getYMapper().getPitch(notesPanelPoint.y);
    }


    /**
     * Scroll so that specified pitch is shown in the center of the editor, if possible.
     *
     * @param pitch
     */
    public void scrollToCenter(int pitch)
    {
        Preconditions.checkArgument(pitch >= 0 && pitch < 128);

        var vpRect = scrollPaneEditor.getViewport().getViewRect();
        float vpCenterY = vpRect.y + vpRect.height / 2f;
        IntRange pitchYRange = notesPanel.getYMapper().getKeyboardYRange(pitch);
        float pitchCenterY = (int) pitchYRange.getCenter();
        int dy = Math.round(vpCenterY - pitchCenterY);
        var r = new Rectangle(vpRect.x, dy > 0 ? vpRect.y - dy : vpRect.y + vpRect.height - 1 - dy, 1, 1);
        notesPanel.scrollRectToVisible(r);
        LOGGER.log(Level.FINE, "scrollToCenter() pitch={0} vpRect={1} r={2} notesPanel.bounds={3}", new Object[]
        {
            pitch, vpRect, r, notesPanel.getBounds()
        });

    }

    /**
     * Scroll so that specified position is shown in the center of the editor, if possible.
     *
     * @param posInBeats
     */
    public void scrollToCenter(float posInBeats)
    {
        Preconditions.checkArgument(getPhraseBeatRange().contains(posInBeats, true));

        var vpRect = scrollPaneEditor.getViewport().getViewRect();
        int vpCenterX = vpRect.x + vpRect.width / 2;
        int posCenterX = notesPanel.getXMapper().getX(posInBeats);
        int dx = vpCenterX - posCenterX;
        var r = new Rectangle(dx > 0 ? vpRect.x - dx : vpRect.x + vpRect.width - 1 - dx, vpRect.y, 1, 1);
        notesPanel.scrollRectToVisible(r);
        LOGGER.log(Level.FINE, "scrollToCenter() posInBeats={0} vpRect={1} r={2} notesPanel.bounds={3}", new Object[]
        {
            posInBeats, vpRect, r, notesPanel.getBounds()
        });
    }


    /**
     * Get the min/max phrase notes which are currently visible.
     *
     * @return Might be empty
     */
    public IntRange getVisiblePitchRange()
    {
        var res = IntRange.EMPTY_RANGE;
        var yMapper = notesPanel.getYMapper();
        if (yMapper.isUptodate())
        {
            IntRange vpYRange = IntRange.ofY(scrollPaneEditor.getViewport().getViewRect());
            IntRange keysYRange = IntRange.ofY(keyboard.getKeysBounds());
            IntRange ir = keysYRange.getIntersection(vpYRange);
            res = yMapper.getPitchRange(ir);
        }
        return res;
    }

    /**
     * Get the min/max beat positions which are visible.
     *
     * @return Can be EMPTY_FLOAT_RANGE
     */
    public FloatRange getVisibleBeatRange()
    {
        var res = FloatRange.EMPTY_FLOAT_RANGE;
        var xMapper = notesPanel.getXMapper();
        if (xMapper.isUptodate())
        {
            var rgViewPort = IntRange.ofX(scrollPaneEditor.getViewport().getViewRect());
            var rg = IntRange.ofX(notesPanel.getBounds()).getIntersection(rgViewPort);
            res = rg.isEmpty() ? FloatRange.EMPTY_FLOAT_RANGE : xMapper.getBeatPositionRange(rg);
        }
        return res;
    }

    /**
     * Check is editor is ready, ie painted and layouted at the correct size, so all editor methods can be called.
     *
     * @return
     */
    public boolean isReady()
    {
        return notesPanel.getXMapper().isUptodate() && notesPanel.getYMapper().isUptodate();
    }

    /**
     * Get the min/max bar indexes which are visible.
     *
     * @return Can be IntRange.EMPTY_RANGE
     */
    public IntRange getVisibleBarRange()
    {
        var res = IntRange.EMPTY_RANGE;
        var visibleBr = getVisibleBeatRange();
        if (!visibleBr.isEmpty())
        {
            Position posFrom = notesPanel.getXMapper().getPosition(visibleBr.from);
            Position posTo = notesPanel.getXMapper().getPosition(visibleBr.to);
            res = new IntRange(posFrom.getBar(), posTo.getBar());
        }
        return res;
    }


    /**
     * @return The UndoManager used by this editor.
     */
    public JJazzUndoManager getUndoManager()
    {
        return undoManager;
    }

    /**
     * Set the UndoManager used by this editor.
     *
     * @param um
     */
    public final void setUndoManager(JJazzUndoManager um)
    {
        model.removeUndoableEditListener(undoManager);
        undoManager = um;
        model.addUndoableEditListener(undoManager);
    }

    @Override
    public String toString()
    {
        return "PianoRollEditor[" + song.getName() + "]";
    }
    // ==========================================================================================================
    // ClsChangeListener interface
    // ==========================================================================================================    

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent e) throws UnsupportedEditException
    {
        if (e instanceof ClsActionEvent ae && ae.isComplete())
        {
            // Listen to all user actions which do not trigger a song structure change
            switch (ae.getApiId())
            {
                case AddItem, ChangeItem, RemoveItem, MoveItem, SetSectionName ->
                {
                    updateChordSequence();
                }
                default ->
                {
                    // Nothing
                }
            }
        }
    }

    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // LOGGER.log(Level.FINE, "propertyChange() -- evt={0}", Utilities.toDebugString(evt, 60));

        if (evt.getSource() == model)
        {
            switch (evt.getPropertyName())
            {
                case Phrase.PROP_NOTES_ADDED, Phrase.PROP_NOTES_ADDED_ADJUSTING ->
                {
                    List<NoteEvent> nes = (List<NoteEvent>) evt.getNewValue();
                    addNotes(nes);
                    editorPanels.forEach(ep -> ep.revalidate());
                }
                case Phrase.PROP_NOTES_REMOVED, Phrase.PROP_NOTES_REMOVED_ADJUSTING ->
                {
                    List<NoteEvent> nes = (List<NoteEvent>) evt.getNewValue();
                    removeNotes(nes);
                    editorPanels.forEach(ep -> 
                    {
                        ep.revalidate();
                        ep.repaint();
                    });
                }
                case Phrase.PROP_NOTES_MOVED, Phrase.PROP_NOTES_MOVED_ADJUSTING, Phrase.PROP_NOTES_REPLACED, Phrase.PROP_NOTES_REPLACED_ADJUSTING ->
                {
                    Map<NoteEvent, NoteEvent> mapOldNew = (Map<NoteEvent, NoteEvent>) evt.getNewValue();
                    for (var oldNe : mapOldNew.keySet())
                    {
                        var newNe = mapOldNew.get(oldNe);
                        editorPanels.forEach(ep -> ep.setNoteViewModel(oldNe, newNe));
                    }
                    editorPanels.forEach(ep -> 
                    {
                        ep.revalidate();
                        ep.repaint();
                    });
                }
                default ->
                {
                }
            }

        } else if (evt.getSource() == ghostPhrasesModel)
        {
            switch (evt.getPropertyName())
            {
                case GhostPhrasesModel.PROP_VISIBLE_PHRASE_SELECTION, GhostPhrasesModel.PROP_VISIBLE_PHRASE_CONTENT -> updateGhostPhrases();
                default ->
                {
                }
            }
        }
    }

    // =======================================================================================================================
    // Private methods
    // =======================================================================================================================

    /**
     * All the editor panels except the main notesPanel.
     *
     * @return
     */
    private List<EditorPanel> getSubPanels()
    {
        return editorPanels.stream()
                .filter(ep -> ep != notesPanel)
                .toList();
    }

    /**
     * Caller is responsible to call revalidate() and/or repaint() as required.
     * <p>
     * Don't add if not in the beat range.
     *
     * @param notes
     */
    private void addNotes(List<NoteEvent> notes)
    {
        var rangeNotes = notes.stream()
                .filter(ne -> beatRange.contains(ne.getBeatRange(), false))
                .toList();
        if (!rangeNotes.isEmpty())
        {
            for (var ne : rangeNotes)
            {
                for (var ep : editorPanels)
                {
                    var nv = ep.addNoteView(ne);
                    if (ep == notesPanel)
                    {
                        registerNoteView(nv);
                    }
                }
            }
        }
    }

    private void updateGhostPhrases()
    {
        notesPanel.setGhostPhrases(ghostPhrasesModel.getVisibleGhostPhrases());
    }

    /**
     * Caller is responsible to call revalidate() and/or repaint() as required.
     *
     * @param notes
     */
    private void removeNotes(List<NoteEvent> notes)
    {
        var rangeNotes = notes.stream()
                .filter(ne -> beatRange.contains(ne.getBeatRange(), false))
                .toList();
        if (!rangeNotes.isEmpty())
        {
            selectNotes(rangeNotes, false);
            for (var ne : rangeNotes)
            {
                var nv = notesPanel.getNoteView(ne);        // Might be null in corner cases !? Issue #399
                if (nv != null)
                {
                    unregisterNoteView(nv);
                }
                notesPanel.removeNoteView(ne);
                getSubPanels().forEach(ep -> ep.removeNoteView(ne));
            }
        }
    }

    private void registerNoteView(NoteView nv)
    {
        Preconditions.checkNotNull(nv);
        editToolProxyMouseListener.install(nv);
        showKeyMouseListener.install(nv);
        zoomEditorMouseListener.install(nv);
        nv.setInheritsPopupMenu(true);

    }

    private void unregisterNoteView(NoteView nv)
    {
        Preconditions.checkNotNull(nv);
        editToolProxyMouseListener.uninstall(nv);
        showKeyMouseListener.uninstall(nv);
        zoomEditorMouseListener.uninstall(nv);
    }

    private int toZoomHValue(float scaleFactorX)
    {
        int zoomHValue = (int) (100f * (scaleFactorX - 0.1f) / 4);
        zoomHValue = Math.max(0, zoomHValue);
        zoomHValue = Math.min(100, zoomHValue);
        return zoomHValue;
    }

    private float toScaleFactorX(int zoomHValue)
    {
        float xFactor = 0.1f + 4 * zoomHValue / 100f;
        return xFactor;
    }

    private float toScaleFactorY(int zoomVValue)
    {
        float yFactor = 0.6f + 4 * zoomVValue / 100f;
        return yFactor;
    }

    /**
     * Set the editor zoom value.
     *
     * @param zoom
     */
    private void setZoom(ZoomValue zoom)
    {
        Preconditions.checkNotNull(zoom);
        LOGGER.log(Level.FINE, "setZoom() -- this.zoomvalue={0} zoom={1}", new Object[]
        {
            this.zoomValue, zoom
        });


        if (zoomValue == null || zoomValue.hValue() != zoom.hValue())
        {
            // Save position center
            var vbr = getVisibleBeatRange();
            if (vbr.isEmpty())
            {
                LOGGER.log(Level.FINE, "setZoom() zoom={0} Unexpected getVisibleBeatRange() empty", zoom);
                return;
            }
            float saveCenterPosInBeats = vbr.getCenter();

            // This updates notesPanel preferred size and calls revalidate(), which will update the size on the EDT
            float f = toScaleFactorX(zoom.hValue());
            editorPanels.forEach(ep -> ep.setScaleFactorX(f));

            // Restore position at center
            // Must be done later on the EDT to get the notesPanel effectively resized after previous command, so that
            // XMapper() will be refreshed before calling scrollToCenter
            SwingUtilities.invokeLater(() -> scrollToCenter(saveCenterPosInBeats));

        }

        if (zoomValue == null || zoomValue.vValue() != zoom.vValue())
        {
            // Save pitch at center
            var vpr = getVisiblePitchRange();
            int saveCenterPitch = vpr.isEmpty() ? 60 : (int) vpr.getCenter();


            // Scale the keyboard
            float factor = toScaleFactorY(zoom.vValue());


            // Because keyboard is in RIGHT orientation factorX impacts the keyboard height.
            // We limit factorY because we don't want the keyboard to get wide
            // This updates keyboard preferred size and calls revalidate(), which will update the size         
            keyboard.setScaleFactor(factor, Math.min(MAX_WIDTH_FACTOR, factor));


            // This is to avoid a difficult bug when zooming in/out vertically fast with mouse-wheel: sometimes 2 successive zoom events
            // occur BEFORE the keyboard component resized event (triggered by setScaleFactor() just above) is fired. In this case 
            // the refresh of YMapper() is done too late (see component size listener in YMapper), and scrollToCenter() below fails because YMapper is not up to date.
            // So we force the refresh now.
            notesPanel.getYMapper().refresh(keyboard.getPreferredSize().height);


            // restore pitch at center
            // Note that surprisingly using SwingUtilities.invokeLater() on scrollToCenter() did not solve the bug explained above
            scrollToCenter(saveCenterPitch);
        }

        zoomValue = zoom;
    }

    private void createUI()
    {
        // The keyboard 
        // We need an enclosing panel for keyboard, so that keyboard size changes when its scaleFactor changes (zoom in/out). If we put the keyboard directly
        // in the JScrollpane, keyboard size might not change when JScrollpane is much bigger than the keys bounds.
        pnl_keyboard = new JPanel();
        pnl_keyboard.setLayout(new BorderLayout());
        keyboard = new KeyboardComponent(KeyboardRange._128_KEYS, KeyboardComponent.Orientation.RIGHT, false);
        labelNotes(keyboard, keyMap);
        pnl_keyboard.add(keyboard, BorderLayout.PAGE_START);


        // The components
        notesPanel = new NotesPanel(this, keyboard);
        rulerPanel = new RulerPanel(this, notesPanel);


        // Scroll pane
        scrollPaneEditor = new JScrollPane();
        scrollPaneEditor.setViewportView(notesPanel.getJLayer());
        scrollPaneEditor.setRowHeaderView(pnl_keyboard);
        scrollPaneEditor.setColumnHeaderView(rulerPanel);
        var vsb = scrollPaneEditor.getVerticalScrollBar();
        var hsb = scrollPaneEditor.getHorizontalScrollBar();
        vsb.setUnitIncrement(vsb.getUnitIncrement() * 10);   // view can be large...
        hsb.setUnitIncrement(hsb.getUnitIncrement() * 10);


        // The bottom component
        velocityPanel = new VelocityPanel(this, notesPanel);
        scorePanel = new ScorePanel(this, notesPanel);
        bottomControlPanel = new BottomControlPanel(velocityPanel, scorePanel);
        JPanel bottomEditorPanelsContainer = new JPanel();
        var cardLayout = new CardLayout();
        bottomEditorPanelsContainer.setLayout(cardLayout);
        bottomEditorPanelsContainer.add(velocityPanel, BottomControlPanel.VELOCITY_EDITOR_PANEL_STRING);
        bottomEditorPanelsContainer.add(scorePanel, BottomControlPanel.SCORE_EDITOR_PANEL_STRING);
        bottomControlPanel.addPropertyChangeListener(BottomControlPanel.PROP_EDITOR_PANEL_STRING,
                evt -> cardLayout.show(bottomEditorPanelsContainer, bottomControlPanel.getSelectedPanelString()));
        cardLayout.show(bottomEditorPanelsContainer, bottomControlPanel.getSelectedPanelString());


        bottomScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bottomScrollPane.setViewportView(bottomEditorPanelsContainer);
        bottomScrollPane.setRowHeaderView(bottomControlPanel);


        // Split pane
        splitPane_TopBottom = new JSplitPane();
        splitPane_TopBottom.setDividerSize(10);
        splitPane_TopBottom.setOneTouchExpandable(true);
        splitPane_TopBottom.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane_TopBottom.setResizeWeight(lastSessionUISettings.splitpaneDividerWeight);
        splitPane_TopBottom.setTopComponent(scrollPaneEditor);
        splitPane_TopBottom.setBottomComponent(bottomScrollPane);


        // We need to keep the bottom control panel vertically aligned with notesPanel        
        updateBottomControlPanelWidth();
        pnl_keyboard.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                updateBottomControlPanelWidth();
            }
        });


        // Link scrollPaneVelocity to scrollPaneEditor for horizontal scroll
        bottomScrollPane.getHorizontalScrollBar().setModel(scrollPaneEditor.getHorizontalScrollBar().getModel());


        // Final layout
        setLayout(new BorderLayout());
        add(splitPane_TopBottom, BorderLayout.CENTER);


        // Create the popupmenu
        var popupMenu = new JPopupMenu();
        var menuItem = new JMenuItem();
        menuItem.setBorder(BorderFactory.createEmptyBorder());
        EditToolBar editToolBar = new EditToolBar(this);
        editToolBar.setClickListener(() -> popupMenu.setVisible(false));
        menuItem.setPreferredSize(editToolBar.getPreferredSize());
        menuItem.add(editToolBar);
        popupMenu.add(menuItem);
        notesPanel.setComponentPopupMenu(popupMenu);
    }


    private void labelNotes(KeyboardComponent keyboard, DrumKit.KeyMap keymap)
    {
        for (var key : keyboard.getAllKeys())
        {
            String s;
            if (keymap == null)
            {
                s = key.getPitch() % 12 == 0 ? "C" + (key.getPitch() / 12 - 1) : null;
            } else
            {
                s = keymap.getKeyName(key.getPitch());
                if (s != null)
                {
                    s = s.toLowerCase();
                }
            }
            key.setText(s);
        }
    }

    private void addKeyboardActions()
    {
        // Our delegates for standard Netbeans callback actions
        getActionMap().put("cut-to-clipboard", new CutNotes(this));
        getActionMap().put("copy-to-clipboard", new CopyNotes(this));
        getActionMap().put("paste-from-clipboard", new PasteNotes(this));


        // Delegates for our callback actions        
        // Must be the editor's action map because it will be in the lookup of the TopComponent
        getActionMap().put("jjazz-delete", new DeleteSelection(this));
        getActionMap().put("jjazz-selectall", new SelectAllNotes(this));


        // Actions with no UI button or menu associated
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_F), ZoomToFit.ACTION_ID);
        getActionMap().put(ZoomToFit.ACTION_ID, new ZoomToFit(this));
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(UIUtilities.getGenericControlShiftKeyStroke(KeyEvent.VK_I), InvertNoteSelection.ACTION_ID);
        getActionMap().put(InvertNoteSelection.ACTION_ID, new InvertNoteSelection(this));
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KEYSTROKE, PlayFromHere.ACTION_ID);
        getActionMap().put(PlayFromHere.ACTION_ID, new PlayFromHere(this));


        // Use the notesPanel input map to avoid the arrow keys being captured by the enclosing JScrollPane
        var jumpToEndAction = new JumpToEnd(this);
        var jumpToStartAction = new JumpToStart(this);
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("HOME"), JumpToStart.ACTION_ID);
        notesPanel.getActionMap().put(JumpToStart.ACTION_ID, jumpToStartAction);
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("END"), JumpToEnd.ACTION_ID);
        notesPanel.getActionMap().put(JumpToEnd.ACTION_ID, jumpToEndAction);
        velocityPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("HOME"), JumpToStart.ACTION_ID);
        velocityPanel.getActionMap().put(JumpToStart.ACTION_ID, jumpToStartAction);
        velocityPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("END"), JumpToEnd.ACTION_ID);
        velocityPanel.getActionMap().put(JumpToEnd.ACTION_ID, jumpToEndAction);
        rulerPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("HOME"), JumpToStart.ACTION_ID);
        rulerPanel.getActionMap().put(JumpToStart.ACTION_ID, jumpToStartAction);
        rulerPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("END"), JumpToEnd.ACTION_ID);
        rulerPanel.getActionMap().put(JumpToEnd.ACTION_ID, jumpToEndAction);
        scorePanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("HOME"), JumpToStart.ACTION_ID);
        scorePanel.getActionMap().put(JumpToStart.ACTION_ID, jumpToStartAction);
        scorePanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("END"), JumpToEnd.ACTION_ID);
        scorePanel.getActionMap().put(JumpToEnd.ACTION_ID, jumpToEndAction);

        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("HOME"), JumpToStart.ACTION_ID);
        notesPanel.getActionMap().put(JumpToStart.ACTION_ID, jumpToStartAction);
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("END"), JumpToEnd.ACTION_ID);
        notesPanel.getActionMap().put(JumpToEnd.ACTION_ID, jumpToEndAction);
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("LEFT"), "MoveSelectionLeft");
        notesPanel.getActionMap().put("MoveSelectionLeft", new MoveSelectionLeft(this));
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("RIGHT"), "MoveSelectionRight");
        notesPanel.getActionMap().put("MoveSelectionRight", new MoveSelectionRight(this));
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("alt LEFT"), "ResizeSelectionShorter");
        notesPanel.getActionMap().put("ResizeSelectionShorter", new ResizeSelection(this, false));
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("alt RIGHT"), "ResizeSelectionLonger");
        notesPanel.getActionMap().put("ResizeSelectionLonger", new ResizeSelection(this, true));
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("alt UP"), "IncreaseSelectionVelocity");
        notesPanel.getActionMap().put("IncreaseSelectionVelocity", new IncreaseSelectionVelocity(this));
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("alt DOWN"), "DecreaseSelectionVelocity");
        notesPanel.getActionMap().put("DecreaseSelectionVelocity", new DecreaseSelectionVelocity(this));
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("UP"), "TransposeUp");
        notesPanel.getActionMap().put("TransposeUp", new TransposeSelectionUp(this));
        notesPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("DOWN"), "TransposeDown");
        notesPanel.getActionMap().put("TransposeDown", new TransposeSelectionDown(this));


    }

    /**
     * Make the panel have the same size than the keyboard panel, so that velocityPanel and notesPanel remain always left-aligned.
     */
    private void updateBottomControlPanelWidth()
    {
        int keyboardWidth = pnl_keyboard.getWidth();        // + scrollPaneEditor.getInsets().left;
        // LOGGER.log(Level.SEVERE, "createUI().componentResized() -- keyboardWidth={0}", keyboardWidth);
        var pd = bottomControlPanel.getPreferredSize();
        bottomControlPanel.setPreferredSize(new Dimension(keyboardWidth, pd.height));
        bottomControlPanel.revalidate();      // required when keyboard got smaller
        bottomControlPanel.repaint();         // required when keyboard got smaller
    }

    private void updateChordSequence()
    {
        if (song == null)
        {
            return;
        }
        var old = chordSequence;

        var offsettedBarRange = getPhraseBarRange().getTransformed(getRulerStartBar());
        chordSequence = new ChordSequence(offsettedBarRange);
        SongChordSequence.fillChordSequence(chordSequence, song, offsettedBarRange);

        firePropertyChange(PROP_CHORD_SEQUENCE, old, chordSequence);
    }


    private IntRange computePhraseBarRange()
    {
        int nbBars = 0;

        float tsPos = beatRange.from;
        TimeSignature ts = getTimeSignature(tsPos);

        for (var tsNextPos : mapPosTimeSignature.subMap(tsPos + 1f, Float.MAX_VALUE).keySet())
        {
            var tsNext = mapPosTimeSignature.get(tsNextPos);
            tsNextPos = Math.min(tsNextPos, beatRange.to);
            nbBars += (int) Math.round((tsNextPos - tsPos) / ts.getNbNaturalBeats());
            ts = tsNext;
            tsPos = tsNextPos;
        }

        if (tsPos < beatRange.to)
        {
            nbBars += (int) Math.round((beatRange.to - tsPos) / ts.getNbNaturalBeats());
        }

        return new IntRange(0, nbBars - 1);
    }


    // =======================================================================================================================
    // Inner classes
    // =======================================================================================================================
    /**
     * Implements the Zoomable functionalities.
     */
    private class PianoRollZoomable implements Zoomable
    {


        @Override
        public Zoomable.Capabilities getZoomCapabilities()
        {
            return Zoomable.Capabilities.X_Y;
        }


        @Override
        public int getZoomYFactor()
        {
            return getZoom().vValue();
        }


        @Override
        public void setZoomYFactor(int newFactor, boolean valueIsAdjusting)
        {
            if (valueIsAdjusting)
            {
                // Safer, avoid some flickering
                return;
            }
            int old = getZoomYFactor();
            if (old != newFactor)
            {
                setZoom(new ZoomValue(getZoomXFactor(), newFactor));
                firePropertyChange(Zoomable.PROPERTY_ZOOM_Y, old, newFactor);
            }
        }


        @Override
        public int getZoomXFactor()
        {
            return getZoom().hValue();
        }


        @Override
        public void setZoomXFactor(int newFactor, boolean valueIsAdjusting)
        {
//            if (valueIsAdjusting)
//            {
//                return;
//            }
            int old = getZoomXFactor();
            if (old != newFactor)
            {
                setZoom(new ZoomValue(newFactor, getZoomYFactor()));
                firePropertyChange(Zoomable.PROPERTY_ZOOM_X, old, newFactor);
            }

        }

        @Override
        public void setZoomYFactorToFitContent()
        {
            // Don't bother zooming, just make sure center pitch is visible
            var nvs = getNoteViews();
            if (nvs.isEmpty())
            {
                return;
            }
            var firstNe = nvs.get(0).getModel();
            var lastNe = nvs.get(nvs.size() - 1).getModel();
            scrollToCenter((int) Math.round((lastNe.getPitch() + firstNe.getPitch()) / 2f));
        }

        @Override
        public void setZoomXFactorToFitContent()
        {
            // Try to show all notes horizontally in the visible rectangle
            var nvs = getNoteViews();
            if (nvs.isEmpty())
            {
                return;
            }
            var firstNe = nvs.get(0).getModel();
            var lastNe = nvs.get(nvs.size() - 1).getModel();


            int visibleWidthPixel = Math.max(100, scrollPaneEditor.getViewport().getViewRect().width);
            var notesBeatRange = firstNe.getBeatRange().getUnion(lastNe.getBeatRange());
            float beatRange = Math.max(4f, notesBeatRange.size());


            // Compute optimal scaleX
            float factorX = notesPanel.computeScaleFactorX(visibleWidthPixel, beatRange);
            int zoomH = toZoomHValue(factorX);
            setZoomXFactor(zoomH, false);


            SwingUtilities.invokeLater(() -> scrollToCenter(notesBeatRange.getCenter()));

        }


        @Override
        public void addPropertyListener(PropertyChangeListener l)
        {
            addPropertyChangeListener(Zoomable.PROPERTY_ZOOM_X, l);
            addPropertyChangeListener(Zoomable.PROPERTY_ZOOM_Y, l);
        }


        @Override
        public void removePropertyListener(PropertyChangeListener l)
        {
            removePropertyChangeListener(Zoomable.PROPERTY_ZOOM_X, l);
            removePropertyChangeListener(Zoomable.PROPERTY_ZOOM_Y, l);
        }


    };


    /**
     * Handle the zoom in/out of the editor using ctrl[-shift]-mousewheel.
     * <p>
     * - Handle ctrl-mousewheel for zoom in and out<br>
     * <p>
     */
    private class ZoomEditorMouseListener implements MouseWheelListener
    {

        public void install(JComponent jc)
        {
            jc.addMouseWheelListener(this);
        }

        public void uninstall(JComponent jc)
        {
            jc.removeMouseWheelListener(this);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            // Manage vertical/horizontal zoom accelerators via ctrl (+shift)
            if (!e.isControlDown())
            {
                if (e.isAltDown())
                {
                    // Used by SelectionTool
                    return;
                }

                // We don't consume the event
                // Pass it to parent (eg used by enclosing JScrollPane to move the scrollbars up/down or left-right if shift pressed
                Container source = e.getSource() instanceof NoteView ? ((Component) e.getSource()).getParent() : (Container) e.getSource();
                Container parent = source.getParent();
                MouseEvent parentEvent = SwingUtilities.convertMouseEvent(source, e, parent);
                parent.dispatchEvent(parentEvent);
                return;
            }

            // Use the Zoomable to get the Zoomable scrollbars updated
            Zoomable zoomable = getLookup().lookup(Zoomable.class);
            if (zoomable == null)
            {
                return;
            }

            if (!e.isShiftDown())
            {
                // Horizontal Zoom
                final int STEP = 5;
                int hFactor = zoomable.getZoomXFactor();
                if (e.getWheelRotation() < 0)
                {
                    hFactor = Math.min(100, hFactor + STEP);
                } else
                {
                    hFactor = Math.max(0, hFactor - STEP);
                }
                zoomable.setZoomXFactor(hFactor, false);
            } else
            {
                // Vertical Zoom
                final int STEP = 5;
                int vFactor = zoomable.getZoomYFactor();
                if (e.getWheelRotation() < 0)
                {
                    vFactor = Math.min(100, vFactor + STEP);
                } else
                {
                    vFactor = Math.max(0, vFactor - STEP);
                }
                zoomable.setZoomYFactor(vFactor, false);
            }
        }
    }


    /**
     * Handle the move of the editor with ctrl-drag;
     * <p>
     */
    private class MoveEditorMouseListener extends MouseAdapter
    {

        /**
         * Null if no dragging.
         */
        private Point startDraggingPoint;
        private final boolean disableVerticalMove;

        public MoveEditorMouseListener(boolean disableVerticalMove)
        {
            this.disableVerticalMove = disableVerticalMove;
        }

        public void install(JComponent jc)
        {
            jc.addMouseListener(this);
            jc.addMouseMotionListener(this);
        }

        public void uninstall(JComponent jc)
        {
            jc.removeMouseListener(this);
            jc.removeMouseMotionListener(this);
        }


        @Override
        public void mousePressed(MouseEvent e)
        {
            if (e.isControlDown())
            {
                return;
            }
            JPanel panel = e.getComponent() instanceof JPanel p ? p : (JPanel) SwingUtilities.getAncestorOfClass(JPanel.class, e.getComponent());
            if (panel != null)
            {
                panel.requestFocusInWindow();          // Needed for InputMap/ActionMap actions
            }
        }


        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (SwingUtilities.isLeftMouseButton(e) && e.isControlDown())
            {
                // Move editor
                if (startDraggingPoint == null)
                {
                    startDraggingPoint = e.getPoint();
                    e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else
                {
                    JViewport viewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, notesPanel);
                    if (viewPort != null)
                    {
                        int deltaX = startDraggingPoint.x - e.getX();
                        int deltaY = disableVerticalMove ? 0 : startDraggingPoint.y - e.getY();
                        Rectangle view = viewPort.getViewRect();
                        view.x += deltaX;
                        view.y += deltaY;
                        notesPanel.scrollRectToVisible(view);
                    }
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (startDraggingPoint != null)
            {
                startDraggingPoint = null;
                e.getComponent().setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    /**
     * Show the corresponding key on the keyboard when mouse is moved.
     * <p>
     * For the NotesPanel and its NoteViews.
     */
    private class ShowKeyMouseListener extends MouseAdapter
    {

        int lastHighlightedPitch = -1;

        public void install(JComponent jc)
        {
            jc.addMouseListener(this);
            jc.addMouseMotionListener(this);
        }

        public void uninstall(JComponent jc)
        {
            jc.removeMouseListener(this);
            jc.removeMouseMotionListener(this);
        }


        @Override
        public void mouseMoved(MouseEvent e)
        {
            showMarkOnKeyboard(e);
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (lastHighlightedPitch != -1)
            {
                keyboard.getKey(lastHighlightedPitch).release();
            }
            lastHighlightedPitch = -1;
        }


        @Override
        public void mouseDragged(MouseEvent e)
        {
            showMarkOnKeyboard(e);
        }

        private void showMarkOnKeyboard(MouseEvent e)
        {
            if (!notesPanel.getYMapper().isUptodate())
            {
                return;
            }

            Point p = e.getSource() instanceof NoteView nv ? SwingUtilities.convertPoint(nv, e.getPoint(), notesPanel)
                    : e.getPoint();
            int pitch = notesPanel.getYMapper().getPitch(p.y);
            if (pitch == lastHighlightedPitch)
            {
                // Nothing
            } else if (pitch == -1)
            {
                keyboard.getKey(lastHighlightedPitch).release();
            } else
            {
                if (lastHighlightedPitch != -1)
                {
                    keyboard.getKey(lastHighlightedPitch).release();
                }
                keyboard.getKey(pitch).setPressed(50, Color.LIGHT_GRAY);
            }
            lastHighlightedPitch = pitch;
        }
    }


    /**
     * Handle the selection rectangle when dragging on the editor.
     * <p>
     */
    private class SelectionRectangleMouseListener extends MouseAdapter
    {

        /**
         * Null if no dragging.
         */
        private Point startDraggingPoint;

        public void install(JComponent jc)
        {
            jc.addMouseListener(this);
            jc.addMouseMotionListener(this);
        }

        public void uninstall(JComponent jc)
        {
            jc.removeMouseListener(this);
            jc.removeMouseMotionListener(this);
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (activeTool.isEditMultipleNotesSupported() && SwingUtilities.isLeftMouseButton(e) && !e.isControlDown())
            {
                // Draw rectangle selection
                if (startDraggingPoint == null)
                {
                    startDraggingPoint = e.getPoint();
                    unselectAll();
                } else
                {
                    ((JPanel) e.getSource()).scrollRectToVisible(new Rectangle(e.getX(), e.getY(), 1, 1));

                    Rectangle r = new Rectangle(startDraggingPoint);
                    r.add(e.getPoint());
                    notesPanel.showSelectionRectangle(r);
                }
            }

        }


        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (startDraggingPoint != null && activeTool.isEditMultipleNotesSupported())
            {
                Rectangle r = new Rectangle(startDraggingPoint);
                r.add(e.getPoint());
                notesPanel.showSelectionRectangle(null);
                startDraggingPoint = null;

                var nvs = notesPanel.getNoteViews(r);
                if (!nvs.isEmpty())
                {
                    activeTool.editMultipleNotes(nvs);
                }
            }
        }
    }

    /**
     * Redirect all mouse events to the active EditTool.
     */
    private class EditToolProxyMouseListener implements MouseInputListener, MouseWheelListener
    {

        public void install(JComponent jc)
        {
            jc.addMouseListener(this);
            jc.addMouseMotionListener(this);
            jc.addMouseWheelListener(this);
        }

        public void uninstall(JComponent jc)
        {
            jc.removeMouseListener(this);
            jc.removeMouseMotionListener(this);
            jc.removeMouseWheelListener(this);
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (e.getSource() == notesPanel)
            {
                activeTool.editorClicked(e);
            } else if (e.getSource() instanceof NoteView nv)
            {
                activeTool.noteClicked(e, nv);
            }
        }


        @Override
        public void mousePressed(MouseEvent e)
        {
            // Nothing
        }


        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (e.getSource() == notesPanel)
            {
                activeTool.editorReleased(e);
            } else if (e.getSource() instanceof NoteView nv)
            {
                activeTool.noteReleased(e, nv);
            }
        }


        @Override
        public void mouseEntered(MouseEvent e)
        {
            if (e.getSource() instanceof NoteView nv)
            {
                activeTool.noteEntered(e, nv);
            }
        }


        @Override
        public void mouseExited(MouseEvent e)
        {
            if (e.getSource() instanceof NoteView nv)
            {
                activeTool.noteExited(e, nv);
            }
        }


        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (!SwingUtilities.isLeftMouseButton(e))
            {
                return;
            }

            if (e.getSource() == notesPanel)
            {
                activeTool.editorDragged(e);
            } else if (e.getSource() instanceof NoteView nv)
            {
                ((JPanel) e.getSource()).scrollRectToVisible(new Rectangle(e.getX(), e.getY(), 1, 1));
                activeTool.noteDragged(e, nv);
            }
        }


        @Override
        public void mouseMoved(MouseEvent e)
        {
            if (e.getSource() instanceof NoteView nv)
            {
                activeTool.noteMoved(e, nv);
            }
        }


        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            if (e.getSource() == notesPanel)
            {
                activeTool.editorWheelMoved(e);
            } else if (e.getSource() instanceof NoteView nv)
            {
                activeTool.noteWheelMoved(e, nv);
            }
        }

    }


    /**
     * Handle a Midi file dragged into the notesPanel
     */
    private class MidiFileDragInTransferHandlerImpl extends SingleFileDragInTransferHandler
    {

        @Override
        protected boolean isImportEnabled()
        {
            return isEnabled();
        }

        @Override
        protected boolean importFile(File midiFile)
        {

            if (midiFile.length() == 0)
            {
                // HACK: when dragging from MixConsole on a big song, the music generation might take some time, and music is not ready yet
                // when we drop in our editor. In this case the midiFile can be empty when this method is called. So we exit right away and return true 
                // so that the MixConsole TransferHandler can display the appropriate error message,  like when user drags too fast
                // from MixConsole to the OS.
                // Note that midiFile is written in a different thread, so the check above is not 100% reliable.
                // If actually midiFile does not come from JJazzLab, the only downside is that no error message will be shown.                
                return true;
            }


            Phrase p = null;
            try
            {
                p = Phrases.importPhrase(midiFile, channel, isDrums(), false, true);
            } catch (IOException | InvalidMidiDataException ex)
            {
                LOGGER.log(Level.WARNING, "MidiFileDragInTransferHandlerImpl.importMidiFile() exception while importing midiFile={0} ex={1}",
                        new Object[]
                        {
                            midiFile.getAbsolutePath(),
                            ex.getMessage()
                        });
                String exMsg = ex.getMessage();
                if (exMsg == null)
                {
                    exMsg = "";
                }
                String msg = ResUtil.getString(PianoRollEditor.class, "ErrImportingMidiFile", midiFile.getAbsolutePath(), exMsg);
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return false;
            }


            if (p != null && !p.isEmpty())
            {
                unselectAll();

                String undoText = ResUtil.getString(getClass(), "importMidiFile");
                getUndoManager().startCEdit(undoText);

                model.add(p, true);           // Do not clone the notes

                getUndoManager().endCEdit(undoText);


                final var nv0 = getNoteView(p.getNotes().get(0));
                SwingUtilities.invokeLater(() -> notesPanel.scrollRectToVisible(nv0.getBounds()));     // invokeLater to make sure task is run after nv0 is layouted                
            }

            return true;
        }

        @Override
        protected Collection<String> getAcceptedFileExtensions()
        {
            return Arrays.asList("mid");
        }
    }


    /**
     * Manage selection data.
     */
    private class NoteSelection implements PropertyChangeListener
    {

        /**
         * Store selected NoteEvents and their NoteViews ordered by position.
         */
        private final TreeSet<NoteView> selectedNoteViews = new TreeSet<>((nv1, nv2) -> nv1.getModel().compareTo(nv2.getModel()));
        private List<NoteView> cacheNoteViewOrderedList = Collections.emptyList();
        private List<NoteEvent> cacheNoteEventOrderedList = Collections.emptyList();
        private boolean isNoteEventListDirty = true;

        private void selectNotesImpl(Collection<NoteEvent> notes, boolean b)
        {
            if (notes.isEmpty())
            {
                return;
            }

            Set<EditorPanel> updatedEditorPanels = new HashSet<>();
            List<NoteView> nvs = new ArrayList<>();

            for (var n : notes)
            {
                var nv = getNoteView(n);
                if (nv == null)
                {
                    LOGGER.log(Level.WARNING, "selectNotesImpl() Unexpected note select with no NoteView associated n={0}", n);
                    continue;
                }
                if (b && !selectedNoteViews.contains(nv))
                {
                    selectedNoteViews.add(nv);
                    nvs.add(nv);
                    nv.addPropertyChangeListener(this);     // Listen to model changes
                } else if (!b && selectedNoteViews.contains(nv))
                {
                    selectedNoteViews.remove(nv);
                    nvs.add(nv);
                    nv.removePropertyChangeListener(this);
                }
                nv.setSelected(b, false);       // Do not repaint now, especially when updating a large number of NoteViews

                for (var ep : getSubPanels())
                {
                    nv = ep.getNoteView(n);
                    if (nv != null)
                    {
                        updatedEditorPanels.add(ep);
                        nv.setSelected(b, false);
                    }
                }
            }

            // Now we can call repaint() on the containers
            notesPanel.repaint();
            updatedEditorPanels.forEach(ep -> ep.repaint());


            // Update our cache data            
            if (!nvs.isEmpty())
            {
                cacheNoteViewOrderedList = List.copyOf(selectedNoteViews);
                isNoteEventListDirty = true;
                firePropertyChange(PROP_SELECTED_NOTE_VIEWS, nvs, b);
            }
        }

        private List<NoteView> getSelectedNoteViewsImpl()
        {
            return cacheNoteViewOrderedList;
        }


        private List<NoteEvent> getSelectedNoteEventsImpl()
        {
            if (isNoteEventListDirty)
            {
                cacheNoteEventOrderedList = cacheNoteViewOrderedList.stream()
                        .map(nv -> nv.getModel())
                        .toList();
                isNoteEventListDirty = false;
            }
            return cacheNoteEventOrderedList;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (!isNoteEventListDirty && evt.getPropertyName().equals(NoteView.PROP_MODEL))
            {
                isNoteEventListDirty = true;
            }
        }
    }


    private record SessionUISettings(double splitpaneDividerWeight, int zoomH, int zoomV)
            {

        public SessionUISettings()
        {
            this(0.8d, -1, -1); // 0.8d => bottom height=20%
        }
    }
}
