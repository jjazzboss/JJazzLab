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
package org.jjazz.pianoroll;

import com.google.common.base.Preconditions;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JLayer;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.ZoomValue;
import org.jjazz.pianoroll.edittools.EraserTool;
import org.jjazz.pianoroll.edittools.PencilTool;
import org.jjazz.pianoroll.edittools.SelectionTool;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent;
import org.jjazz.ui.keyboardcomponent.api.KeyboardRange;
import org.jjazz.ui.utilities.api.Zoomable;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;

/**
 * Implementation of a PianoRollEditor.
 */
public class PianoRollEditorImpl extends PianoRollEditor implements PropertyChangeListener
{

    private static final float MAX_WIDTH_FACTOR = 1.5f;
    private final List<EditTool> editTools;
    private ToolbarPanel toolbarPanel;
    private JSplitPane splitPane;
    private VelocityPanel velocityPanel;
    private NotesPanel notesPanel;
    private KeyboardComponent keyboard;
    private RulerPanel rulerPanel;
    private JScrollPane scrollpane;
    private MouseDragLayerUI mouseDragLayerUI;
    private JLayer mouseDragLayer;
    private JPopupMenu popupMenu;

    private ZoomValue zoomValue;
    private final SizedPhrase spModel;
    private final DrumKit.KeyMap keymap;
    private final PianoRollEditorSettings settings;
    private Quantization quantization;

    /**
     * Our global lookup.
     */
    private final Lookup lookup;

    /**
     * The lookup for non-selection stuff.
     */
    private final Lookup generalLookup;
    /**
     * Store non-selection stuff.
     */
    /**
     * Our UndoManager.
     */
    private JJazzUndoManager undoManager;
    private final Lookup selectionLookup;
    private final InstanceContent selectionLookupContent;
    private final InstanceContent generalLookupContent;
    private final int startBarIndex;
    private EditTool activeTool;
    private final EditToolProxyMouseListener editToolProxyMouseListener;
    private final GenericMouseListener genericMouseListener;
    private boolean useHack = true;
    private static final Logger LOGGER = Logger.getLogger(PianoRollEditorImpl.class.getSimpleName());
    private boolean snapEnabled;
    private float playbackPointPosition;

    /**
     * Creates new form PianoRollEditorImpl
     */
    public PianoRollEditorImpl(int startBarIndex, SizedPhrase sp, DrumKit.KeyMap kmap, PianoRollEditorSettings settings)
    {
        Preconditions.checkNotNull(sp);
        Preconditions.checkNotNull(settings);
        this.startBarIndex = startBarIndex;
        this.spModel = sp;
        this.keymap = kmap;
        this.settings = settings;
        this.quantization = Quantization.ONE_QUARTER_BEAT;


        // Be notified of changes, note added, moved, removed, set
        spModel.addPropertyChangeListener(this);


        undoManager = new JJazzUndoManager();
        spModel.addUndoableEditListener(undoManager);


        // Must be set before createUI()
        editTools = Arrays.asList(new SelectionTool(this), new PencilTool(this), new EraserTool(this));
        activeTool = editTools.get(0);


        // Selection lookup
        selectionLookupContent = new InstanceContent();
        selectionLookup = new AbstractLookup(selectionLookupContent);

        // The lookup for other stuff, before createUI()
        generalLookupContent = new InstanceContent();
        generalLookupContent.add(new PianoRollZoomable());
        generalLookupContent.add(getActionMap());
        generalLookup = new AbstractLookup(generalLookupContent);

        // Global lookup = sum of both
        lookup = new ProxyLookup(selectionLookup, generalLookup);


        createUI();


        // Our popupmenu
        popupMenu = new JPopupMenu();
        var menuItem = new JMenuItem();
        menuItem.setBorder(BorderFactory.createEmptyBorder());
        EditToolBar editToolBar = new EditToolBar(this, editTools);
        editToolBar.setClickListener(() -> popupMenu.setVisible(false));
        menuItem.setPreferredSize(editToolBar.getPreferredSize());
        menuItem.add(editToolBar);
        popupMenu.add(menuItem);
        notesPanel.setComponentPopupMenu(popupMenu);


        // NotesPanel mouse listeners
        genericMouseListener = new GenericMouseListener();
        notesPanel.addMouseListener(genericMouseListener);
        notesPanel.addMouseMotionListener(genericMouseListener);
        notesPanel.addMouseWheelListener(genericMouseListener);
        editToolProxyMouseListener = new EditToolProxyMouseListener();
        notesPanel.addMouseListener(editToolProxyMouseListener);
        notesPanel.addMouseMotionListener(editToolProxyMouseListener);
        notesPanel.addMouseWheelListener(editToolProxyMouseListener);


        // Normal zoom
        zoomValue = new ZoomValue(20, keymap == null ? 30 : 60);        // Drum notes need more heigth
        notesPanel.setScaleFactorX(toScaleFactorX(zoomValue.hValue()));
        float yFactor = toScaleFactorY(zoomValue.vValue());
        keyboard.setScaleFactor(yFactor, Math.min(MAX_WIDTH_FACTOR, yFactor));


        // Add the notes
        for (NoteEvent ne : spModel)
        {
            addNote(ne);
        }


    }

    /**
     * HACK to detect the first time the editor is fully validated, so we can call scrollToNotes() effectively.
     * <p>
     * Tried without success: <br>
     * - using AncestoListener.ancestorAdded()/ComponentShown did not work (notified too early, notesPanel has height not set
     * yet).<br>
     * - using TopComponent.opened()/showing() with SwingUtilities.invokeLater()<br>
     *
     * @param g
     */
    @Override
    public void doLayout()
    {
        super.doLayout();
        if (useHack == true && notesPanel.getWidth() > 0 && notesPanel.getHeight() > 0 && isShowing())
        {
            // We consider everything is layout now
            useHack = false;
            runOnceUIisValidated();
        }
    }

    @Override
    public Lookup getLookup()
    {
        return lookup;
    }

    @Override
    public SizedPhrase getModel()
    {
        return spModel;
    }

    @Override
    public int getStartBarIndex()
    {
        return startBarIndex;
    }

    @Override
    public DrumKit.KeyMap getDrumKeyMap()
    {
        return keymap;
    }

    @Override
    public JJazzUndoManager getUndoManager()
    {
        return undoManager;
    }

    @Override
    public PianoRollEditorSettings getSettings()
    {
        return settings;
    }

    @Override
    public void cleanup()
    {
        rulerPanel.cleanup();
        notesPanel.cleanup();
        spModel.removeUndoableEditListener(undoManager);
        spModel.removePropertyChangeListener(this);
    }

    @Override
    public void setZoom(ZoomValue zoom)
    {
        Preconditions.checkNotNull(zoom);
        LOGGER.log(Level.FINE, "setZoom() -- zoom={0}", zoom);

        if (zoomValue == null || zoomValue.hValue() != zoom.hValue())
        {
            // Save position center
            float saveCenterPosInBeats = getVisibleBeatRange().getCenter();

            // This updates notesPanel preferred size and calls revalidate(), which will update the size on the EDT
            notesPanel.setScaleFactorX(toScaleFactorX(zoom.hValue()));

            // Restore position at center
            // Must be done on the EDT to get the notesPanel resized after previous command
            SwingUtilities.invokeLater(() -> scrollToCenter(saveCenterPosInBeats));

        }

        if (zoomValue == null || zoomValue.vValue() != zoom.vValue())
        {
            // Save pitch at center
            int saveCenterPitch = (int) getVisiblePitchRange().getCenter();


            // Scale the keyboard
            float factor = toScaleFactorY(zoom.vValue());
            // Because keyboard is in RIGHT orientation factorX impacts the keyboard height.
            // We limit factorY because we don't want the keyboard to get wide
            keyboard.setScaleFactor(factor, Math.min(MAX_WIDTH_FACTOR, factor));


            // restore pitch at center
            scrollToCenter(saveCenterPitch);
        }

        zoomValue = zoom;
    }

    @Override
    public ZoomValue getZoom()
    {
        return zoomValue;
    }

    @Override
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

    @Override
    public Quantization getQuantization()
    {
        return quantization;
    }

    @Override
    public void setSnapEnabled(boolean b)
    {
        if (b == snapEnabled)
        {
            return;
        }
        snapEnabled = b;
        firePropertyChange(PROP_SNAP_ENABLED, !b, b);
    }

    @Override
    public boolean isSnapEnabled()
    {
        return snapEnabled;
    }

    @Override
    public NoteView getNoteView(NoteEvent ne)
    {
        return notesPanel.getNoteView(ne);
    }

    @Override
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

    @Override
    public EditTool getActiveTool()
    {
        return activeTool;
    }


    @Override
    public void showPlaybackPoint(float pos)
    {
        if (Float.floatToIntBits(pos) == Float.floatToIntBits(playbackPointPosition))
        {
            return;
        }
        float old = playbackPointPosition;
        playbackPointPosition = pos;
        firePropertyChange(PROP_PLAYBACK_POINT_POSITION, old, playbackPointPosition);
    }

    @Override
    public float getPlaybackPointPosition()
    {
        return playbackPointPosition;
    }

    @Override
    public float getPositionFromPoint(Point editorPoint)
    {
        return notesPanel.getXMapper().getPositionInBeats(editorPoint.x);
    }

    @Override
    public int getPitchFromPoint(Point notesPanelPoint)
    {
        return notesPanel.getYMapper().getPitch(notesPanelPoint.y);
    }


    public KeyboardComponent getKeyboardComponent()
    {
        return keyboard;
    }

    public NotesPanel getNotesPanel()
    {
        return notesPanel;
    }

    public FloatRange getBeatRange()
    {
        return getModel().getBeatRange();
    }


    public IntRange getBarRange()
    {
        int nbBars = (int) (getBeatRange().size() / getModel().getTimeSignature().getNbNaturalBeats());
        return new IntRange(getStartBarIndex(), getStartBarIndex() + nbBars - 1);
    }

    /**
     * Scroll so that specified pitch is shown in the center of the editor, if possible.
     *
     * @param pitch
     */
    public void scrollToCenter(int pitch)
    {
        Preconditions.checkArgument(pitch >= 0 && pitch < 128);

        var vpRect = scrollpane.getViewport().getViewRect();
        float vpCenterY = vpRect.y + vpRect.height / 2f;
        IntRange pitchYRange = notesPanel.getYMapper().getKeyboardYRange(pitch);
        float pitchCenterY = (int) pitchYRange.getCenter();
        int dy = Math.round(vpCenterY - pitchCenterY);
        var r = new Rectangle(vpRect.x, dy > 0 ? vpRect.y - dy : vpRect.y + vpRect.height - 1 - dy, 1, 1);
        notesPanel.scrollRectToVisible(r);
//        LOGGER.log(Level.SEVERE, "scrollToCenter() pitch={0} vpRect={1} r={2} notesPanel.bounds={3}", new Object[]
//        {
//            pitch, vpRect, r, notesPanel.getBounds()
//        });

    }

    /**
     * Scroll so that specified position is shown in the center of the editor, if possible.
     *
     * @param posInBeats
     */
    public void scrollToCenter(float posInBeats)
    {
        Preconditions.checkArgument(getBeatRange().contains(posInBeats, true));

        var vpRect = scrollpane.getViewport().getViewRect();
        int vpCenterX = vpRect.x + vpRect.width / 2;
        int posCenterX = notesPanel.getXMapper().getX(posInBeats);
        int dx = vpCenterX - posCenterX;
        var r = new Rectangle(dx > 0 ? vpRect.x - dx : vpRect.x + vpRect.width - 1 - dx, vpRect.y, 1, 1);
        notesPanel.scrollRectToVisible(r);
//        LOGGER.log(Level.SEVERE, "scrollToCenter() posInBeats={0} vpRect={1} r={2} notesPanel.bounds={3}", new Object[]
//        {
//            posInBeats, vpRect, r, notesPanel.getBounds()
//        });
    }


    /**
     * Get the min/max notes which are currently visible.
     *
     * @return
     */
    public IntRange getVisiblePitchRange()
    {
        IntRange vpYRange = getYRange(scrollpane.getViewport().getViewRect());
        IntRange keysYRange = getYRange(keyboard.getKeysBounds());
        IntRange ir = keysYRange.getIntersection(vpYRange);
        var pitchBottom = notesPanel.getYMapper().getPitch(ir.to);
        var pitchTop = notesPanel.getYMapper().getPitch(ir.from);
        return new IntRange(pitchBottom, pitchTop);
    }

    /**
     * Get the min/max beat positions which are visible.
     *
     * @return
     */
    public FloatRange getVisibleBeatRange()
    {
        var vpRect = scrollpane.getViewport().getViewRect();
        var notesPanelBounds = notesPanel.getBounds();
        var vRect = vpRect.intersection(notesPanelBounds);
        var posLeft = notesPanel.getXMapper().getPositionInBeats(vRect.x);
        var posRight = notesPanel.getXMapper().getPositionInBeats(vRect.x + vRect.width - 1);
        return new FloatRange(posLeft, posRight);
    }

    /**
     * Get the min/max bar indexes which are visible.
     *
     * @return
     */
    public IntRange getVisibleBarRange()
    {
        float nbBeatsPerBar = getModel().getTimeSignature().getNbNaturalBeats();
        var beatRange = getVisibleBeatRange();
        int relBarFrom = (int) ((beatRange.from - spModel.getBeatRange().from) / nbBeatsPerBar);
        int relBarTo = (int) ((beatRange.to - spModel.getBeatRange().from) / nbBeatsPerBar);
        var res = new IntRange(startBarIndex + relBarFrom, startBarIndex + relBarTo);
        return res;
    }


    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.log(Level.FINE, "propertyChange() evt.source.class={0}prop={1} old={2} new={3}", new Object[]
        {
            evt.getSource().getClass().getSimpleName(), evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()
        });

        if (evt.getSource() == spModel)
        {
            switch (evt.getPropertyName())
            {
                case Phrase.PROP_NOTE_ADDED ->
                {
                    List<NoteEvent> nes = (List<NoteEvent>) evt.getNewValue();
                    nes.forEach(ne -> addNote(ne));
                    notesPanel.revalidate();
                }
                case Phrase.PROP_NOTE_REMOVED ->
                {
                    List<NoteEvent> nes = (List<NoteEvent>) evt.getNewValue();
                    nes.forEach(ne -> removeNote(ne));
                    notesPanel.revalidate();
                    notesPanel.repaint();
                }
                case Phrase.PROP_NOTE_MOVED, Phrase.PROP_NOTE_REPLACED ->
                {
                    NoteEvent newNe = (NoteEvent) evt.getNewValue();
                    NoteEvent oldNe = (NoteEvent) evt.getOldValue();
                    var nv = notesPanel.getNoteView(oldNe);
                    nv.setModel(newNe);
                }
                default ->
                {
                }
            }
        } else if (evt.getSource() instanceof NoteView nv)
        {
            if (evt.getPropertyName().equals(NoteView.PROP_SELECTED))
            {
                if (nv.isSelected())
                {
                    selectionLookupContent.add(nv);
                } else
                {
                    selectionLookupContent.remove(nv);
                }
            }
        }
    }

    // =======================================================================================================================
    // Private methods
    // =======================================================================================================================

    /**
     * Caller is responsible to call revalidate() and/or repaint() as required.
     *
     * @param ne
     */
    private void addNote(NoteEvent ne)
    {
        var nv = notesPanel.addNoteView(ne);
        registerNoteView(nv);
        revalidate();
    }

    /**
     * Caller is responsible to call revalidate() and/or repaint() as required.
     *
     * @param ne
     */
    private void removeNote(NoteEvent ne)
    {
        var nv = notesPanel.removeNoteView(ne);
        unregisterNoteView(nv);
    }

    private void registerNoteView(NoteView nv)
    {
        nv.addMouseListener(editToolProxyMouseListener);
        nv.addMouseMotionListener(editToolProxyMouseListener);
        nv.addMouseMotionListener(genericMouseListener);
        nv.addMouseWheelListener(editToolProxyMouseListener);
        nv.addPropertyChangeListener(this);
        nv.setInheritsPopupMenu(true);

    }

    private void unregisterNoteView(NoteView nv)
    {
        nv.removeMouseListener(editToolProxyMouseListener);
        nv.removeMouseMotionListener(editToolProxyMouseListener);
        nv.removeMouseMotionListener(genericMouseListener);
        nv.removeMouseWheelListener(editToolProxyMouseListener);
        nv.removePropertyChangeListener(this);
    }

    private float toScaleFactorX(int zoomHValue)
    {
        float xFactor = 0.2f + 4 * zoomHValue / 100f;
        return xFactor;
    }

    private float toScaleFactorY(int zoomVValue)
    {
        float yFactor = 0.4f + 4 * zoomVValue / 100f;
        return yFactor;
    }

    private void createUI()
    {
        // The keyboard 
        // We need an enclosing panel for keyboard, so that keyboard size changes when its scaleFactor changes (zoom in/out). If we put the keyboard directly
        // in the JScrollpane, keyboard size might not change when JScrollpane is much bigger than the keys bounds.
        JPanel pnl_keyboard = new JPanel();
        pnl_keyboard.setLayout(new BorderLayout());
        keyboard = new KeyboardComponent(KeyboardRange._128_KEYS, KeyboardComponent.Orientation.RIGHT, false);
        labelNotes(keyboard, keymap);
        pnl_keyboard.add(keyboard, BorderLayout.PAGE_START);


        // The scrollpane
        mouseDragLayerUI = new MouseDragLayerUI();
        notesPanel = new NotesPanel(this, keyboard);
        mouseDragLayer = new JLayer(notesPanel, mouseDragLayerUI);


        rulerPanel = new RulerPanel(this, notesPanel);
        scrollpane = new JScrollPane();
        scrollpane.setViewportView(mouseDragLayer);
        scrollpane.setRowHeaderView(pnl_keyboard);
        scrollpane.setColumnHeaderView(rulerPanel);
        var vsb = scrollpane.getVerticalScrollBar();
        var hsb = scrollpane.getHorizontalScrollBar();
        vsb.setUnitIncrement(vsb.getUnitIncrement() * 10);   // view can be large...
        hsb.setUnitIncrement(hsb.getUnitIncrement() * 10);


        // The splitpane
        velocityPanel = new VelocityPanel(this, notesPanel);
        splitPane = new javax.swing.JSplitPane();
        splitPane.setDividerSize(3);
        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(1.0);
        splitPane.setLeftComponent(scrollpane);
        splitPane.setRightComponent(velocityPanel);


        // Final layout
        toolbarPanel = new ToolbarPanel(this, editTools);
        setLayout(new BorderLayout());
        add(toolbarPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);


    }


    private void showSelectionRectangle(Rectangle r)
    {
        mouseDragLayerUI.showSelectionRectangle(r);
        mouseDragLayer.repaint();
    }

    private IntRange getYRange(Rectangle r)
    {
        assert r.height > 0;
        IntRange res = new IntRange(r.y, r.y + r.height - 1);
        return res;
    }

    private IntRange getXRange(Rectangle r)
    {
        assert r.width > 0;
        IntRange res = new IntRange(r.x, r.x + r.width - 1);
        return res;
    }

    /**
     * Adjust the scrollbars so that the first note is visible.
     */
    private void scrollToNotes()
    {
        // LOGGER.severe("scrollToNotes() --");
        var nvs = notesPanel.getNoteViews();
        var vpSize = scrollpane.getViewport().getViewSize();
        if (nvs.isEmpty() || vpSize.width == 0 || vpSize.height == 0)
        {
            // vpSize might be empty if component hidden by another (?)
            return;
        }
        var posFirst = nvs.get(0).getModel().getPositionInBeats();
        var visibleBr = getVisibleBeatRange();
        posFirst += (visibleBr.size() / 2) * 0.8f;
        posFirst = Math.min(posFirst, spModel.getBeatRange().to - 1);
        scrollToCenter(posFirst);
        int pitch = nvs.get(0).getModel().getPitch();
        scrollToCenter(pitch);
    }

    /**
     * Put here some initialization tasks that need to be executed when UI is validated, ie components layout is over.
     */
    private void runOnceUIisValidated()
    {
        SwingUtilities.invokeLater(() -> scrollToNotes());
    }

    private void labelNotes(KeyboardComponent keyboard, DrumKit.KeyMap keymap)
    {
        if (keymap == null)
        {
            keyboard.getWhiteKeys().stream()
                    .filter(k -> k.getPitch() % 12 == 0)
                    .forEach(k -> k.setText("C" + (k.getPitch() / 12 - 1)));

        } else
        {
            keyboard.getAllKeys().forEach(k ->
            {
                String s = keymap.getKeyName(k.getPitch());
                if (s != null)
                {
                    s = s.toLowerCase();
                }
                k.setText(s);
            });
        }
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
            setZoom(new ZoomValue(getZoomXFactor(), newFactor));
            firePropertyChange(Zoomable.PROPERTY_ZOOM_Y, old, newFactor);
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
            setZoom(new ZoomValue(newFactor, getZoomYFactor()));
            firePropertyChange(Zoomable.PROPERTY_ZOOM_X, old, newFactor);

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
     * Provide generic services.
     * <p>
     * - Handle the selection rectangle when dragging on the editor.<br>
     * - Show the corresponding key on the keyboard when mouse is moved.<br>
     * - Handle ctrl-mousewheel for zoom<br>
     * <p>
     */
    private class GenericMouseListener extends MouseAdapter
    {

        /**
         * Null if no dragging.
         */
        private Point startDraggingPoint;
        int lastHighlightedPitch = -1;

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

            if (e.getSource() == notesPanel && activeTool.isEditMultipleNotesSupported() && SwingUtilities.isLeftMouseButton(e))
            {
                if (startDraggingPoint == null)
                {
                    startDraggingPoint = e.getPoint();
                    unselectAll();
                } else
                {
                    ((JPanel) e.getSource()).scrollRectToVisible(new Rectangle(e.getX(), e.getY(), 1, 1));

                    Rectangle r = new Rectangle(startDraggingPoint);
                    r.add(e.getPoint());
                    showSelectionRectangle(r);
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
                showSelectionRectangle(null);
                startDraggingPoint = null;

                var nvs = notesPanel.getNoteViews(r);
                activeTool.editMultipleNotes(nvs);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            // Manage only ctrl-wheel
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != InputEvent.CTRL_DOWN_MASK)
            {
                // It's not a ctrl-wheel. We don't want to lose the event, need to be processed by the above hierarchy, i.e. enclosing JScrollPane
                Container source = (Container) e.getSource();
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
        }


        private void showMarkOnKeyboard(MouseEvent e)
        {
            if (!notesPanel.getYMapper().isUptodate())
            {
                return;
            }

            Point p = e.getSource() instanceof NoteView nv ? SwingUtilities.convertPoint(nv, e.getPoint(), notesPanel) : e.getPoint();
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
     * Redirect mouse events to the active EditTool.
     */
    private class EditToolProxyMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener
    {

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

                // Event needs also to be processed by the GenericMouseListener
                MouseEvent e2 = SwingUtilities.convertMouseEvent(nv, e, notesPanel);
                genericMouseListener.mouseMoved(e2);
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
}
