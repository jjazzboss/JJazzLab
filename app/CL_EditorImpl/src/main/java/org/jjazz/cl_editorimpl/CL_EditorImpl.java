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
package org.jjazz.cl_editorimpl;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.*;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.barbox.api.BarBox;
import org.jjazz.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorMouseListener;
import org.jjazz.cl_editor.spi.CL_EditorSettings;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.uiutilities.api.Zoomable;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.api.SelectedBar;
import org.jjazz.cl_editor.api.SelectedCLI;
import org.jjazz.cl_editor.spi.BarBoxFactory;
import org.jjazz.cl_editor.spi.BarRendererFactory;
import org.jjazz.cl_editor.spi.BarRendererProvider;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.openide.awt.UndoRedo;
import org.jjazz.uisettings.api.ColorSetManager;
import org.jjazz.utilities.api.StringProperties;

/**
 * A chordleadsheet editor using BarBox objects to render bars.
 */
public class CL_EditorImpl extends CL_Editor implements PropertyChangeListener, ClsChangeListener, Scrollable, MouseListener, MouseWheelListener, MouseMotionListener
{

    private static final int NB_EXTRA_LINES = 4;

    /**
     * The default BarRenderer types.
     */
    private final String[] DEFAULT_BAR_RENDERER_TYPES =
    {
        BarRendererFactory.BR_CHORD_SYMBOL,
        BarRendererFactory.BR_CHORD_POSITION,
        BarRendererFactory.BR_SECTION,
        BarRendererFactory.BR_ANNOTATION
    };

    // GUI variables
    /**
     * Our LayoutManager.
     */
    private final GridLayout gridLayout;
    /**
     * Keep a list of BarBox components.
     */
    private final List<BarBox> barBoxes = new ArrayList<>();
    /**
     * Our graphical settings.
     */
    private final CL_EditorSettings settings;
    // APPLICATION variables
    /**
     * Our UndoManager.
     */
    private final JJazzUndoManager undoManager;
    /**
     * Our global lookup.
     */
    private final Lookup lookup;
    /**
     * The lookup for the selection.
     */
    private final Lookup selectionLookup;
    /**
     * Store the selected items or bars.
     */
    private final InstanceContent selectionLookupContent;

    /**
     * The lookup for non-selection stuff.
     */
    private final Lookup generalLookup;
    /**
     * Store non-selection stuff.
     */
    private final InstanceContent generalLookupContent;
    /**
     * The leadsheet clsModel.
     */
    private final ChordLeadSheet clsModel;
    /**
     * An optional container for this ChordLeadSheet.
     */
    private final Song songModel;
    /**
     * The number of columns.
     */
    private int nbColumns;
    /**
     * The last position insertion point.
     */
    private Position insertionPointLastPos;
    /**
     * The last playback point.
     */
    private Position playbackPointLastPos;
    private final BarRendererFactory barRendererFactory;
    private final BarBoxFactory barBoxFactory;
    /**
     * barRendererFactory Receiver for mouse events.
     */
    private CL_EditorMouseListener editorMouseListener;
    /**
     * Our Drag and Drop handler.
     */
    private final CL_EditorTransferHandler transferHandler;
    private final CL_EditorZoomable editorZoomable;
    private final Object lock = new Object();
    private static final Logger LOGGER = Logger.getLogger(CL_EditorImpl.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public CL_EditorImpl(Song song, CL_EditorSettings settings, BarBoxFactory bbf, BarRendererFactory brf)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(brf);
        Preconditions.checkNotNull(bbf);


        // This is the main part to fix Issue #582 (see also CL_EditorTopComponent.componentActivated())
        // It allows the editor to get the focus when a child component lost focus (no more selection), thus making the editor's InputMap/ActionMap still 
        // active: this way always-enabled actions like ToggleBarAnnotations can still be activated via their keyboard shortcut.
        setFocusable(true);


        songModel = song;
        songModel.getClientProperties().addPropertyChangeListener(this);     // Listen to CL_Editor client properties changes


        this.barRendererFactory = brf;
        this.barBoxFactory = bbf;

        // Listen to settings changes
        this.settings = settings;
        this.settings.addPropertyChangeListener(this);

        // For section colors changes
        ColorSetManager.getDefault().addPropertyChangeListener(this);

        // Graphical stuff
        int zxf = CL_EditorClientProperties.getZoomXFactor(songModel);
        nbColumns = zxf > -1 ? computeNbColsFromXZoomFactor(zxf) : 4;

        gridLayout = new GridLayout(0, nbColumns);   // Nb of lines adjusted to number of bars
        setLayout(gridLayout);
        setBackground(settings.getBackgroundColor());

        // The lookup for selection
        selectionLookupContent = new InstanceContent();
        selectionLookup = new AbstractLookup(selectionLookupContent);

        // The lookup for other stuff
        generalLookupContent = new InstanceContent();
        generalLookup = new AbstractLookup(generalLookupContent);

        // Initialize with actionmap and our Zoomable object
        editorZoomable = new CL_EditorZoomable();
        generalLookupContent.add(editorZoomable);
        generalLookupContent.add(getActionMap());

        // Global lookup = sum of both
        lookup = new ProxyLookup(selectionLookup, generalLookup);

        // Our Drag & Drop manager for item drag & drop
        transferHandler = new CL_EditorTransferHandler(this);

        // No bar drag operation
        insertionPointLastPos = null;

        // No playback
        playbackPointLastPos = null;

        // Listen to our model changes
        clsModel = songModel.getChordLeadSheet();
        clsModel.addClsChangeListener(this);

        // Connect our undoManager to our model
        undoManager = JJazzUndoManagerFinder.getDefault().get(clsModel);

        if (undoManager != null)
        {
            clsModel.addUndoableEditListener(undoManager);
        }

        // Fill our lookup
        generalLookupContent.add(clsModel);
        generalLookupContent.add(songModel);

        // Add or remove barboxes at the end if required
        int newSizeInBars = computeNbBarBoxes(NB_EXTRA_LINES);
        setNbBarBoxes(newSizeInBars);  // This will update our songModel via getDisplayQuantizationValue() then Song.putClientProperty()

        // Used to zoom in / zoom out
        addMouseWheelListener(this);

        PlaybackSettings.getInstance().addPropertyChangeListener(PlaybackSettings.PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION, this);
        setDisplayTransposition(PlaybackSettings.getInstance().getChordSymbolsDisplayTransposition());

    }

    @Override
    public UndoRedo getUndoManager()
    {
        return undoManager;
    }

    @Override
    public ChordLeadSheet getModel()
    {
        return clsModel;
    }

    @Override
    public Song getSongModel()
    {
        return songModel;
    }

    @Override
    public CL_EditorSettings getSettings()
    {
        return settings;
    }

    @Override
    public BarRendererFactory getBarRendererFactory()
    {
        return barRendererFactory;
    }

    @Override
    public void setEditorMouseListener(CL_EditorMouseListener brm)
    {
        editorMouseListener = brm;
    }

    @Override
    public void cleanup()
    {
        // Unselect everything
        clearSelection();

        // Unregister the objects we were listening to
        songModel.getClientProperties().removePropertyChangeListener(this);
        clsModel.removeClsChangeListener(this);
        if (undoManager != null)
        {
            clsModel.removeUndoableEditListener(undoManager);
        }
        generalLookupContent.remove(clsModel);
        generalLookupContent.remove(songModel);
        settings.removePropertyChangeListener(this);

        ColorSetManager.getDefault().removePropertyChangeListener(this);

        // Need to remove backwards to avoid consistency problems 
        for (int i = barBoxes.size() - 1; i >= 0; i--)
        {
            removeBarBox(barBoxes.get(i));
        }
        for (Component c : getComponents())
        {
            if (c instanceof PaddingBox pBox)
            {
                removePaddingBox(pBox);
            }
        }

        PlaybackSettings.getInstance()
                .removePropertyChangeListener(PlaybackSettings.PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION, this);

        // We're not showing playback or insertion point anymore
        playbackPointLastPos = null;
        insertionPointLastPos = null;
    }

    @Override
    public SelectedBar getFocusedBar(boolean includeFocusedItem)
    {
        SelectedBar sb = null;
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof BarBox bb)
        {
            sb = new SelectedBar(bb.getBarIndex(), this.clsModel);
        } else if (includeFocusedItem && (c instanceof ItemRenderer))
        {
            sb = new SelectedBar(((ItemRenderer) c).getModel().getPosition().getBar(), this.clsModel);
        }
        return sb;
    }

    @Override
    public void setEnabled(boolean b)
    {
        if (b == isEnabled())
        {
            return;
        }
        super.setEnabled(b);

        // Propagate enabled status to BarBoxes
        for (BarBox bb : getBarBoxes())
        {
            bb.setEnabled(b);
        }
    }

    @Override
    public void setNbColumns(int nbCols)
    {
        Preconditions.checkArgument(nbCols >= 1 && nbCols <= 16, "nbCols=%s", nbCols);

        if (nbCols == nbColumns)
        {
            return;
        }
        int oldNbColumns = nbColumns;
        nbColumns = nbCols;
        gridLayout.setColumns(nbColumns);
        updatePaddingBoxes();
        revalidate();
        int oldFactor = computeXZoomFactorFromNbCols(oldNbColumns);
        int newFactor = computeXZoomFactorFromNbCols(nbColumns);
        LOGGER.log(Level.FINER, "oldFactor={0} newFactor={1}", new Object[]
        {
            oldFactor, newFactor
        });
        editorZoomable.pcs.firePropertyChange(Zoomable.PROPERTY_ZOOM_X, oldFactor, newFactor);

        // Save the zoom factor with the song as a client property        
        CL_EditorClientProperties.setZoomXFactor(songModel, newFactor);
        songModel.setSaveNeeded(true);

    }

    @Override
    public int getNbColumns()
    {
        return nbColumns;
    }

    @Override
    public int getNbBarBoxes()
    {
        return barBoxes.size();
    }

    /**
     * Return the position (bar, beat) which corresponds to a given point in the editor. If point is in the BarBox which does not have a valid modelBar (eg
     * after the end), barIndex is set but beat is set to 0.
     *
     * @param editorPoint A point in the editor's coordinates.
     * @return Null if point does not correspond to a valid bar.
     */
    @Override
    public Position getPositionFromPoint(Point editorPoint)
    {
        // Find the component within this editor which is under this editorPoint
        Component c = SwingUtilities.getDeepestComponentAt(this, editorPoint.x, editorPoint.y);
        if (c == null)
        {
            return null;
        }

        // Find the enclosing BarBox
        while (!(c instanceof BarBox))
        {
            c = c.getParent();
            if (c == null)
            {
                return null;
            }
        }
        BarBox bb = (BarBox) c;
        Point bbPoint = SwingUtilities.convertPoint(this, editorPoint, bb);
        Position bbPos = bb.getPositionFromPoint(bbPoint);
        return (bbPos != null) ? bbPos : new Position(bb.getBarIndex());
    }

    @Override
    public void makeBarVisible(int barIndex)
    {
        Preconditions.checkElementIndex(barIndex, getNbBarBoxes(), "barIndex");


        float nbVisibleRows = (float) getVisibleRect().height / getBarBox(0).getHeight();
        int lastEnabledBarBoxRow = getRowIndex(clsModel.getSizeInBars() - 1);

        int row = getRowIndex(barIndex);

        Rectangle r = getBarBox(barIndex).getBounds();
        if (row == 0 || row == lastEnabledBarBoxRow || nbVisibleRows < 2)
        {
            // Nothing
        } else if (row == 1)
        {
            // Special case, show first row as well
            r.y -= r.height;
            r.height *= 2;
        } else
        {
            // Show next row too
            r.height *= 2;
        }
        scrollRectToVisible(r);
    }

    @Override
    public Rectangle getBarRectangle(int barIndex)
    {
        Preconditions.checkElementIndex(barIndex, getNbBarBoxes(), "barIndex");

        BarBox bb = getBarBox(barIndex);
        Point p = bb.getLocation();
        Rectangle r = new Rectangle(p);
        r.width = bb.getWidth();
        r.height = bb.getHeight();
        return r;
    }

    @Override
    public void selectBars(int bbIndexFrom, int bbIndexTo, boolean b)
    {
        Preconditions.checkPositionIndexes(bbIndexFrom, bbIndexTo, getNbBarBoxes() - 1);

//        LOGGER.log(Level.FINE, "Before selectBars() b={0} bbIndexFrom={1} selectionLookup={2}", new Object[]
//        {
//            b, bbIndexFrom, selectionLookup
//        });

        synchronized (lock)
        {
            var oldSelection = new CL_Selection(selectionLookup);
            if (IntStream.rangeClosed(bbIndexFrom, bbIndexTo).allMatch(bar -> oldSelection.isBarSelected(bar) == b))
            {
                return;
            }
            if (b && oldSelection.isItemSelected())
            {
                clearSelection(oldSelection);
            }

            Set<SelectedBar> newSelectedBars = new HashSet<>(oldSelection.getSelectedBars());
            for (int i = bbIndexFrom; i <= bbIndexTo; i++)
            {
                if (b)
                {
                    newSelectedBars.add(new SelectedBar(i, clsModel));
                } else
                {
                    final int j = i;
                    newSelectedBars.removeIf(sb -> sb.getBarBoxIndex() == j);
                }
            }
            // Use InstanceContent.set() to minimize nb of lookup change events      
            selectionLookupContent.set(newSelectedBars, null);
        }


        // Udpate BarBoxes
        for (int i = bbIndexFrom; i <= bbIndexTo; i++)
        {
            BarBox bb = getBarBox(i);
            bb.setSelected(b);
        }


        // repaint() here is useless functionnally, but VERY IMPORTANT for painting optimization, greatly speed up things
        // see why here : https://forums.oracle.com/forums/thread.jspa?threadID=2330727&tstart=0        
        // not required but seems to perform better because it does a single repaint of the entire frame
        // instead of trying to skip repainting the borders
        repaint();
//        LOGGER.log(Level.FINE, "After selectBars() b={0} bbIndexFrom={1} selectionLookup={2}", new Object[]
//        {
//            b, bbIndexFrom, selectionLookup
//        });
    }

    @Override
    public void selectItems(List<? extends ChordLeadSheetItem> items, boolean b)
    {
        Objects.requireNonNull(items);
        if (items.isEmpty())
        {
            return;
        }

        synchronized (lock)
        {
            var oldSelection = new CL_Selection(selectionLookup);
            if (items.stream().allMatch(item -> oldSelection.isItemSelected(item) == b))
            {
                return;
            }
            if (b && !oldSelection.isItemTypeSelected(items.getFirst().getClass()))
            {
                clearSelection(oldSelection);
            }


            var oldSelectedClis = oldSelection.getSelectedCLIs();  // unordered
            Set<SelectedCLI> newSelectedItems = new HashSet<>(oldSelectedClis);
            for (var item : items)
            {
                newSelectedItems.add(new SelectedCLI(item));
            }
            // Use InstanceContent.set() to minimize nb of lookup change events        
            selectionLookupContent.set(newSelectedItems, null);
        }


        // Udpate ItemRenderers
        items.forEach(item -> 
        {
            BarBox bb = getBarBox(item.getPosition().getBar());
            bb.selectItem(item, b);
        });
    }

    @Override
    public void selectItem(ChordLeadSheetItem<?> item, boolean b)
    {
        Objects.requireNonNull(item);

        synchronized (lock)
        {
            var oldSelection = new CL_Selection(selectionLookup);
            if (oldSelection.isItemSelected(item) == b)
            {
                return;
            }
            if (b && (oldSelection.isBarSelected() || !oldSelection.isItemTypeSelected(item.getClass())))
            {
                clearSelection(oldSelection);
            }


            var selItem = new SelectedCLI(item);
            if (b)
            {
                selectionLookupContent.add(selItem);  // Warning, hash used inside, don't use objects which can mutate while being selected!
            } else
            {
                selectionLookupContent.remove(selItem);  // Warning, hash used inside, don't use objects which can mutate while being selected!
            }
        }

        BarBox bb = getBarBox(item.getPosition().getBar());
        bb.selectItem(item, b);

//        LOGGER.log(Level.FINE, "After selectItem() b={0} item={1} lkp={2}", new Object[]
//        {
//            b, item, lookup
//        });
    }

    @Override
    public boolean clearSelection()
    {
        synchronized (lock)
        {
            var selection = new CL_Selection(selectionLookup);
            return clearSelection(selection);
        }
    }

    @Override
    public void setFocusOnBar(int barIndex)
    {
        BarBox bb = getBarBox(barIndex);
        bb.requestFocusInWindow();
    }

    @Override
    public void setFocusOnItem(ChordLeadSheetItem<?> item, IR_Type irType)
    {
        BarBox bb = getBarBox(item.getPosition().getBar());
        bb.setFocusOnItem(item, irType);
    }

    @Override
    public void setBarBoxConfig(BarBoxConfig bbConfig, Integer... barIndexes)
    {
        checkNotNull(bbConfig);
        LOGGER.log(Level.FINE, "setBarBoxConfig() -- bbConfig={0} barIndexes={1}", new Object[]
        {
            bbConfig, List.of(barIndexes)
        });
        if (barIndexes.length == 0)
        {
            barIndexes = IntStream.range(0, getNbBarBoxes())
                    .boxed()
                    .collect(Collectors.toList()).toArray(Integer[]::new);
        }
        for (int barIndex : barIndexes)
        {
            BarBox bb = getBarBox(barIndex);
            unregisterBarBox(bb);
            bb.setConfig(bbConfig);
            registerBarBox(bb);
        }
    }

    @Override
    public BarBoxConfig getBarBoxConfig(int barIndex)
    {
        BarBox bb = getBarBox(barIndex);
        return bb.getConfig();
    }

    @Override
    public void showInsertionPoint(boolean show, ChordLeadSheetItem<?> item, Position pos, boolean copyMode)
    {
        Preconditions.checkNotNull(item);
        Preconditions.checkArgument(!(show && pos == null));

        LOGGER.log(Level.FINER, "showInsertionPoint() b={0} item={1} pos={2} copyMode={3}", new Object[]
        {
            show, item, pos, copyMode
        });

        if (!show && insertionPointLastPos != null)
        {
            // Remove the insertion point
            getBarBox(insertionPointLastPos.getBar()).showInsertionPoint(false, item, insertionPointLastPos, copyMode);
            insertionPointLastPos = null;
        } else if (show && insertionPointLastPos == null)
        {
            // First show
            getBarBox(pos.getBar()).showInsertionPoint(true, item, pos, copyMode);
            insertionPointLastPos = new Position(pos);
        } else if (show && !pos.equals(insertionPointLastPos))
        {
            // InsertionPoint is already there
            if (insertionPointLastPos.getBar() != pos.getBar())
            {
                // Need to remove the previous insertion point on a different bar
                getBarBox(insertionPointLastPos.getBar()).showInsertionPoint(false, item, insertionPointLastPos, copyMode);
            }
            getBarBox(pos.getBar()).showInsertionPoint(true, item, pos, copyMode);
            insertionPointLastPos.set(pos);
        }
    }

    @Override
    public void showPlaybackPoint(boolean show, Position pos)
    {
        LOGGER.log(Level.FINE, "showPlaybackPoint() show={0} pos={1}", new Object[]
        {
            show, pos
        });
        Preconditions.checkArgument(!(show && pos == null));

        if (playbackPointLastPos != null)
        {
            // Playback point is already shown, switch it off at old location
            int lastBarIndex = playbackPointLastPos.getBar();
            if (lastBarIndex >= getNbBarBoxes())
            {
                // Can happen if song was shortened during playback: barbox was removed so nothing to do
                playbackPointLastPos = null;
            } else if (!show)
            {
                // No more playback point
                getBarBox(playbackPointLastPos.getBar()).showPlaybackPoint(false, playbackPointLastPos);
                playbackPointLastPos = null;
            } else if (pos.getBar() != lastBarIndex)
            {
                // Playback point must be shown on another bar
                getBarBox(playbackPointLastPos.getBar()).showPlaybackPoint(false, playbackPointLastPos);
            }
        }
        if (show && pos.getBar() < getNbBarBoxes())
        {
            if (playbackPointLastPos == null)
            {
                // First show
                getBarBox(pos.getBar()).showPlaybackPoint(true, pos);
                playbackPointLastPos = new Position(pos);
            } else if (!pos.equals(playbackPointLastPos))
            {
                getBarBox(pos.getBar()).showPlaybackPoint(true, pos);
                playbackPointLastPos.set(pos);
            }
            makeBarVisible(pos.getBar());
        }
    }

    @Override
    public void requestAttention(ChordLeadSheetItem<?> item)
    {
        Preconditions.checkArgument(clsModel.contains(item), "item=%s clsModel=%s", item, clsModel);

        BarBox bb = getBarBox(item.getPosition().getBar());
        for (BarRenderer br : bb.getBarRenderers())
        {
            if (br.isRegisteredItemClass(item))
            {
                ItemRenderer ir = br.getItemRenderer(item);
                if (ir != null)
                {
                    ir.requestAttention(Color.YELLOW);
                }
            }
        }
    }

    // ----------------------------------------------------------------------------------
    // Lookup.Provider interface
    // ----------------------------------------------------------------------------------
    @Override
    public Lookup getLookup()
    {
        return lookup;
    }

    // ----------------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------------
    @Override
    public void propertyChange(final PropertyChangeEvent evt)
    {
        // Changes can be generated outside the EDT
        org.jjazz.uiutilities.api.UIUtilities.invokeLaterIfNeeded(() -> 
        {
            if (evt.getSource() == settings)
            {
                if (evt.getPropertyName().equals(CL_EditorSettings.PROP_BACKGROUND_COLOR))
                {
                    setBackground(settings.getBackgroundColor());
                }
            } else if (evt.getSource() == songModel.getClientProperties())
            {
                switch (evt.getPropertyName())
                {
                    case CL_EditorClientProperties.PROP_BAR_ANNOTATION_VISIBLE ->
                    {
                        setBarAnnotationVisible(CL_EditorClientProperties.isBarAnnotationVisible(songModel));
                    }
                    case CL_EditorClientProperties.PROP_BAR_ANNOTATION_NB_LINES ->
                    {
                        setBarAnnotationNbLines(CL_EditorClientProperties.getBarAnnotationNbLines(songModel));
                    }
                    case CL_EditorClientProperties.PROP_ZOOM_FACTOR_Y ->
                    {
                        var zoomY = CL_EditorClientProperties.getZoomYFactor(songModel);
                        for (BarBox bb : getBarBoxes())
                        {
                            bb.setZoomVFactor(zoomY);
                        }
                    }
                }
            } else if (evt.getSource() instanceof StringProperties sp && sp.getOwner() instanceof CLI_Section cliSection)
            {
                switch (evt.getPropertyName())
                {
                    case CL_EditorClientProperties.PROP_SECTION_START_ON_NEW_LINE ->
                    {
                        if (cliSection != clsModel.getSection(0))
                        {
                            updatePaddingBoxes();
                        }
                    }
                }
            } else if (evt.getSource() == ColorSetManager.getDefault())
            {
                if (evt.getPropertyName().equals(ColorSetManager.PROP_REF_COLOR_CHANGED))
                {
                    Color oldColor = (Color) evt.getOldValue();
                    Color newColor = (Color) evt.getOldValue();
                    // Check if some section colors are impacted
                    boolean changed = false;
                    for (var cliSection : songModel.getChordLeadSheet().getItems(CLI_Section.class))
                    {
                        if (CL_EditorClientProperties.getSectionColor(cliSection).equals(oldColor))
                        {
                            CL_EditorClientProperties.setSectionColor(cliSection, newColor);
                            changed = true;
                        }
                    }
                    if (changed)
                    {
                        songModel.setSaveNeeded(true);
                    }
                }

            } else if (evt.getSource() == PlaybackSettings.getInstance())
            {
                if (evt.getPropertyName().equals(PlaybackSettings.PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION))
                {
                    setDisplayTransposition((int) evt.getNewValue());
                }
            }
        });
    }

    private void setDisplayTransposition(int dt)
    {
        getBarBoxes().forEach(bb -> bb.setDisplayTransposition(dt));
    }

    @Override
    public String toString()
    {
        return "CL_Editor size=" + getNbBarBoxes();
    }

    //-----------------------------------------------------------------------
    // Implementation of the MouseListener interface
    //-----------------------------------------------------------------------
    @Override
    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        Component c = (Component) e.getSource();
        if (c instanceof ItemRenderer ir)
        {
            editorMouseListener.itemClicked(e, ir.getModel(), ir.getIR_Type());
        } else if (c instanceof BarRenderer)
        {
            BarBox bb = (BarBox) c.getParent();
            editorMouseListener.barClicked(e, bb.getBarIndex());
        } else if (c instanceof BarBox bb)
        {
            editorMouseListener.barClicked(e, bb.getBarIndex());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        Component c = (Component) e.getSource();
        if (c instanceof BarRenderer)
        {
            BarBox bb = (BarBox) c.getParent();
            editorMouseListener.barReleased(e, bb.getBarIndex());
        } else if (c instanceof BarBox bb)
        {
            editorMouseListener.barReleased(e, bb.getBarIndex());
        }
    }

    //------------------------------------------------------------------
    // Implement the MouseMotionListener interface
    //------------------------------------------------------------------
    @Override
    public void mouseDragged(MouseEvent e)
    {
        if (!SwingUtilities.isLeftMouseButton(e))
        {
            return;
        }
        Component c = (Component) e.getSource();
        if (c instanceof BarRenderer)
        {
            BarBox bb = (BarBox) c.getParent();
            editorMouseListener.barDragged(e, bb.getBarIndex());
        }
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
    }

// ---------------------------------------------------------------
// Implements MouseWheelListener interface
// ---------------------------------------------------------------
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        Component c = (Component) e.getSource();
        if (c instanceof ItemRenderer ir)
        {
            editorMouseListener.itemWheelMoved(e, ir.getModel(), ir.getIR_Type());
        } else if (c == this)
        {
            editorMouseListener.editorWheelMoved(e);
        }
    }

    // ----------------------------------------------------------------------------------
    // ClsChangeListener interface
    // ----------------------------------------------------------------------------------
    @Override
    public void chordLeadSheetChanged(final ClsChangeEvent event) throws UnsupportedEditException
    {
        LOGGER.log(Level.FINE, "chordLeadSheetChanged() -- event={0}", event);

        // Model changes can be generated outside the EDT
        Runnable run = () -> 
        {

            // Save focus state
            ChordLeadSheetItem<?> fItem = null;
            IR_Type fIrType = null;
            Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (c instanceof ItemRenderer ir)
            {
                fItem = ir.getModel();
                fIrType = ir.getIR_Type();
            }
            switch (event)
            {
                case SizeChangedEvent e ->
                {
                    int newSize = e.getNewSize();
                    int oldSize = e.getOldSize();
                    // Create or delete BarBoxes as appropriate
                    setNbBarBoxes(computeNbBarBoxes(NB_EXTRA_LINES));
                    // Refresh bars impacted by the resize
                    int minLastBar = Math.min(oldSize - 1, newSize - 1);
                    int maxLastBar = Math.max(oldSize - 1, newSize - 1);
                    maxLastBar = Math.min(maxLastBar, getNbBarBoxes() - 1);
                    for (int i = minLastBar + 1; i <= maxLastBar; i++)
                    {
                        int bar = (i < newSize) ? i : -1;
                        BarBox bb = getBarBox(i);
                        bb.setModelBarIndex(bar);
                    }
                }
                case ItemAddedEvent e ->
                {
                    for (ChordLeadSheetItem<?> item : e.getItems())
                    {
                        int modelBarIndex = item.getPosition().getBar();
                        addItem(modelBarIndex, item);
                    }
                }
                case ItemRemovedEvent e ->
                {
                    for (ChordLeadSheetItem<?> item : e.getItems())
                    {
                        int barIndex = item.getPosition().getBar();
                        removeItem(barIndex, item, false);
                    }
                }
                case ItemChangedEvent e ->
                {
                    var item = e.getItem();
                    if (item instanceof CLI_Section cliSection)
                    {
                        Section oldSection = (Section) e.getOldData();
                        Section newSection = cliSection.getData();

                        if (!oldSection.getTimeSignature().equals(newSection.getTimeSignature()))
                        {
                            // TimeSignature has changed,
                            propagateSectionChange(cliSection);
                        }
                    }
                }
                case ItemMovedEvent e ->
                {
                    // A moved ChordSymbol or other, but NOT a section
                    ChordLeadSheetItem<?> item = e.getItem();
                    int barIndex = item.getPosition().getBar();
                    int oldBarIndex = e.getOldPosition().getBar();
                    boolean selected = isSelected(item);
                    if (barIndex == oldBarIndex)
                    {
                        // Simple, just update one bar
                        selectItem(item, false); // Important to not corrupt the lookup
                        getBarBox(barIndex).moveItem(item);
                    } else
                    {
                        // Remove on one bar and add on another bar
                        removeItem(oldBarIndex, item, false);
                        addItem(barIndex, item);
                        if (item == fItem)
                        {
                            getBarBox(barIndex).setFocusOnItem(item, fIrType);
                        }
                    }
                    selectItem(item, selected);
                }
                case SectionMovedEvent e ->
                {
                    CLI_Section section = e.getSection();
                    CL_EditorClientProperties.setSectionIsOnNewLine(section, false);
                    int barIndex = section.getPosition().getBar();
                    int prevBarIndex = e.getOldBar();
                    boolean selected = isSelected(section);
                    removeItem(prevBarIndex, section, true);
                    addItem(barIndex, section);       // This will updatePaddingBoxes if needed
                    propagateSectionChange(clsModel.getSection(e.getOldBar()));
                    selectItem(section, selected);
                    if (section == fItem)
                    {
                        getBarBox(barIndex).setFocusOnItem(section, fIrType);
                    }
                }
                case ItemBarShiftedEvent e ->
                {
                    int barDiff = e.getBarDiff();
                    assert barDiff != 0;
                    List<ChordLeadSheetItem> items = e.getItems();
                    int last = items.size() - 1;
                    if (barDiff > 0)
                    {
                        // Shift on the right
                        for (int i = last; i >= 0; i--)
                        {
                            // Start from the end, so moved item remain in same section automatically
                            ChordLeadSheetItem<?> item = items.get(i);
                            int barIndex = item.getPosition().getBar();
                            boolean selected = isSelected(item);
                            removeItem(barIndex - barDiff, item, true);
                            addItem(barIndex, item);
                            if (item instanceof CLI_Section)
                            {
                                // Need to also update the bars from previous position to before new position
                                propagateSectionChange(clsModel.getSection(barIndex - 1));  // CLI_Section parameter might be null in special cases
                            }
                            selectItem(item, selected);
                            if (item == fItem)
                            {
                                getBarBox(barIndex).setFocusOnItem(item, fIrType);
                            }
                        }
                    } else
                    {
                        // Shift to the left
                        for (int i = 0; i <= last; i++)
                        {
                            // Start from the end, so moved item remain in same section automatically
                            ChordLeadSheetItem<?> item = items.get(i);
                            int barIndex = item.getPosition().getBar();
                            boolean selected = isSelected(item);
                            removeItem(barIndex - barDiff, item, true);
                            addItem(barIndex, item);
                            selectItem(item, selected);
                            if (item == fItem)
                            {
                                getBarBox(barIndex).setFocusOnItem(item, fIrType);
                            }
                        }
                    }

                }
                default ->
                {
                }
            }
        };
        org.jjazz.uiutilities.api.UIUtilities.invokeLaterIfNeeded(run);
    }

    // ---------------------------------------------------------------
    // Implements Scrollable interface
    // ---------------------------------------------------------------
    @Override
    public Dimension getPreferredScrollableViewportSize()
    {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect,
            int orientation,
            int direction
    )
    {
        int unit;

        if (orientation == SwingConstants.VERTICAL)
        {
            unit = 10;
        } else
        {
            unit = 10;
        }

        return unit;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect,
            int orientation,
            int direction)
    {
        return getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    /**
     * We want width of the Panel match the width of the viewport (so no scrollbar).
     */
    @Override
    public boolean getScrollableTracksViewportWidth()
    {
        return true;
    }

    /**
     * We do NOT want the height of the Panel match the height of the viewport : panel height is calculated only function of the nb of rows and row height.
     */
    @Override
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }
    // ----------------------------------------------------------------------------------
    // Private functions
    // ----------------------------------------------------------------------------------

    /**
     * Must be used with care: selection MUST be uptodate with the current lookup state.
     *
     * @param selection
     * @return
     */
    protected boolean clearSelection(CL_Selection selection)
    {
        boolean b = false;
        if (!selection.isEmpty())
        {
            b = true;

            // Use InstanceContent.set() to minimize nb of lookup change events        
            selectionLookupContent.set(Collections.EMPTY_LIST, null);

            // Update UI
            if (selection.isBarSelected())
            {
                var bars = selection.getSelectedBars();
                for (var bar : bars)
                {
                    var bb = getBarBox(bar.getBarBoxIndex());
                    bb.setSelected(false);
                }
            } else
            {
                var items = selection.getSelectedItems();
                for (var item : items)
                {
                    var bb = getBarBox(item.getPosition().getBar());
                    bb.selectItem(item, false);
                }
            }
        }
        return b;
    }

    /**
     * Calculate the number of BarBoxes required to accomodate clsModel's size.
     * <p>
     * @param nbExtraLines
     * @return
     */
    private int computeNbBarBoxes(int nbExtraLines)
    {
        Preconditions.checkNotNull(clsModel);
        Preconditions.checkArgument(nbExtraLines >= 1 && nbExtraLines <= 100, "nbExtraLines=%s", nbExtraLines);

        int modelSize = clsModel.getSizeInBars() + nbExtraLines * nbColumns;
        int bars = ((modelSize / nbColumns) + 1) * nbColumns;
        return bars;
    }

    /**
     * Delete/create trailing BarBoxes as required.
     *
     * @param newNbBarBoxes
     */
    private void setNbBarBoxes(int newNbBarBoxes)
    {
        int oldNbBarBoxes = getNbBarBoxes();
        if (newNbBarBoxes == oldNbBarBoxes)
        {
            return;
        }

        if (oldNbBarBoxes < newNbBarBoxes)
        {
            // Need to add BarBoxes at the end

            // The BarBoxConfig to use, default one or the one of the last bar
            BarBoxConfig config = (oldNbBarBoxes == 0) ? getDefaultBarBoxConfig() : getBarBox(oldNbBarBoxes - 1).getConfig();

            for (int bbIndex = oldNbBarBoxes; bbIndex < newNbBarBoxes; bbIndex++)
            {
                int modelBarIndex = bbIndex;
                if (bbIndex >= clsModel.getSizeInBars())
                {
                    modelBarIndex = SelectedBar.POST_END_BAR_MODEL_BAR_INDEX;
                }

                BarBox bb = barBoxFactory.create(this, bbIndex, modelBarIndex, clsModel, config, settings.getBarBoxSettings(), barRendererFactory);
                bb.setEnabled(isEnabled());

                registerBarBox(bb);


                // Insert the BarBox at correct location (possible presence of padding boxes)
                int compIndex = getComponentIndex(bbIndex);
                add(bb, compIndex);


                barBoxes.add(bbIndex, bb);

            }

        } else
        {
            // Easier, just delete BarBoxes
            for (int i = oldNbBarBoxes - 1; i >= newNbBarBoxes; i--)
            {
                removeBarBox(getBarBox(i));
            }
        }

        updatePaddingBoxes();
        revalidate();
    }

    private BarBox getBarBox(int bbIndex)
    {
        Preconditions.checkElementIndex(bbIndex, barBoxes.size(), "bbIndex");

        return barBoxes.get(bbIndex);
    }

    private List<BarBox> getBarBoxes()
    {
        return barBoxes;
    }

    /**
     * Return the Component index corresponding to specified BarBox index.
     * <p>
     * This takes into account PaddingBoxes or other non-BarBox components present in the editor. BarBox is inserted after non-BarBox components.
     *
     * @param barBoxIndex In the range [0,getBarBoxes().size()], the latter to append the BarBox at the end
     * @return
     */
    private int getComponentIndex(int barBoxIndex)
    {
        Preconditions.checkPositionIndex(barBoxIndex, getBarBoxes().size(),
                "barBoxIndex=" + barBoxIndex + " getBarBoxes().size()=" + getBarBoxes().size());

        // getComponents() should be called on EDT, otherwise need treeLock
        assert SwingUtilities.isEventDispatchThread() : "Not running in the EDT! barBoxIndex=" + barBoxIndex;
        int bbIndex = 0;
        int index = 0;
        for (Component c : getComponents())
        {
            index++;
            if (c instanceof BarBox)
            {
                if (bbIndex == barBoxIndex)
                {
                    return index - 1;
                }
                bbIndex++;
            }
        }
        return index;
    }

    private void removeBarBox(BarBox bb)
    {
        selectBars(bb.getBarIndex(), bb.getBarIndex(), false);
        unregisterBarBox(bb);
        bb.cleanup();
        remove(bb);
        barBoxes.remove(bb);
    }

    /**
     * Register a BarBox, its BarRenderers, ItemRenderers, and related client properties.
     *
     * @param bb
     */
    private void registerBarBox(final BarBox bb)
    {
        bb.addMouseListener(this);
        bb.setTransferHandler(transferHandler);
        // bb.setDropTarget(new DropTarget(bb, ddManager));
        for (BarRenderer br : bb.getBarRenderers())
        {
            registerBarRenderer(br);
        }

        int modelBarIndex = bb.getModelBarIndex();
        if (modelBarIndex >= 0)
        {
            var cliSection = clsModel.getSection(modelBarIndex);
            if (cliSection.getPosition().getBar() == modelBarIndex)
            {
                cliSection.getClientProperties().addPropertyChangeListener(this);
            }
        }
    }

    /**
     * Unregister a BarBox, its BarRenderers, ItemRenderers, and related client properties.
     * <p>
     *
     * @param bb
     */
    private void unregisterBarBox(BarBox bb)
    {
        bb.removeMouseListener(this);
        // bb.setTransferHandler(null); 
        // bb.setDropTarget(null);
        for (BarRenderer br : bb.getBarRenderers())
        {
            unregisterBarRenderer(br);
        }

        int modelBarIndex = bb.getModelBarIndex();
        if (modelBarIndex >= 0)
        {
            var cliSection = clsModel.getSection(modelBarIndex);
            if (cliSection.getPosition().getBar() == modelBarIndex)
            {
                cliSection.getClientProperties().removePropertyChangeListener(this);
            }
        }
    }

    private void registerBarRenderer(BarRenderer br)
    {
        br.addMouseListener(this);
        br.addMouseMotionListener(this);
        br.setInheritsPopupMenu(true);
        for (ItemRenderer ir : br.getItemRenderers())
        {
            registerItemRenderer(ir);
        }
    }

    private void unregisterBarRenderer(BarRenderer br)
    {
        br.removeMouseListener(this);
        br.removeMouseMotionListener(this);
        for (ItemRenderer ir : br.getItemRenderers())
        {
            unregisterItemRenderer(ir);
        }
    }

    private void registerItemRenderer(ItemRenderer ir)
    {
        ir.addMouseListener(this);
        ir.addMouseWheelListener(this);
        ir.setTransferHandler(transferHandler);
    }

    private void unregisterItemRenderer(ItemRenderer ir)
    {
        ir.removeMouseListener(this);
        ir.removeMouseWheelListener(this);
        // ir.setTransferHandler(null);   // Do not set to null ! Need to have a valid TransferHandler until complete ending of the DragnDrop mechanism
    }

    private boolean isSelected(ChordLeadSheetItem<?> item)
    {
        return new CL_Selection(selectionLookup, true, false).isItemSelected(item);
    }

    /**
     * Ask the specified BarBox to add ItemRenderer(s) for specified item.
     * <p>
     * If item is a CLI_Section do what's required to maintain editor's consistency.
     *
     * @param barIndex
     * @param item
     */
    private void addItem(int barIndex, ChordLeadSheetItem<?> item)
    {
        for (ItemRenderer ir : getBarBox(barIndex).addItem(item))
        {
            registerItemRenderer(ir);
        }
        if (item instanceof CLI_Section cliSection)
        {
            propagateSectionChange(cliSection);
            if (CL_EditorClientProperties.isSectionIsOnNewLine(cliSection))
            {
                updatePaddingBoxes();
            }
            // Listen to sectionOnNewLine changes
            cliSection.getClientProperties().addPropertyChangeListener(this);
        }
    }

    /**
     * Remove the ChordLeadSheetItem from the specified bar.
     * <p>
     * If item is a section do some cleaning: update the previous section, remove the associated UI settings (quantization, start on newline), possibly update
     * padding boxes
     *
     * @param barIndex
     * @param item
     * @param skipSectionRemovalCleaning If true leave the specific "section removal" cleaning operations to the caller
     */
    private void removeItem(int barIndex, ChordLeadSheetItem<?> item, boolean skipSectionRemovalCleaning)
    {
        // Remove from selection 
        // We can not call SelectItem(false), because item
        // might have been already moved (undoing a move item), so item.toPosition().getBar() is 
        // different from barIndex => exception in selectItem(false)
        synchronized (lock)
        {
            selectionLookupContent.remove(new SelectedCLI(item));
        }

        for (ItemRenderer ir : getBarBox(barIndex).removeItem(item))
        {
            unregisterItemRenderer(ir);
        }

        if (item instanceof CLI_Section cliSection)
        {
            cliSection.getClientProperties().removePropertyChangeListener(this);
            if (CL_EditorClientProperties.isSectionIsOnNewLine(cliSection))
            {
                updatePaddingBoxes();
            }
            if (!skipSectionRemovalCleaning)
            {
                // Update the previous section
                propagateSectionChange(clsModel.getSection(barIndex));      // CLI_Section parameter might be null in special cases (ChordLeadSheet temporary intermediate state)
            }
        }
    }

    /**
     * Update the bars of the specified section that section has changed.
     *
     * @param cliSection If null does nothing
     */
    private void propagateSectionChange(CLI_Section cliSection)
    {
        if (cliSection == null)
        {
            // This can happen in ChordLeadSheet intermediate states where initial section is temporarily removed
            return;
        }
        clsModel.getBarRange(cliSection).forEach(bar -> getBarBox(bar).setSection(cliSection));
    }

    /**
     * Build the default BarBoxConfig from DEFAULT_BARBOX_CONFIG + optional data from BarRendererProviders
     *
     * @return
     */
    private BarBoxConfig getDefaultBarBoxConfig()
    {

        List<String> allTypes = new ArrayList<>();
        List<String> activeTypes = new ArrayList<>();
        Collections.addAll(allTypes, DEFAULT_BAR_RENDERER_TYPES);
        Collections.addAll(activeTypes, DEFAULT_BAR_RENDERER_TYPES);

        // Activation of BR_Annotation depends on Song property
        if (!CL_EditorClientProperties.isBarAnnotationVisible(songModel))
        {
            activeTypes.remove(BarRendererFactory.BR_ANNOTATION);
        }

        var brProviders = Lookup.getDefault().lookupAll(BarRendererProvider.class);
        for (var brProvider : brProviders)
        {
            var map = brProvider.getSupportedTypes();
            for (String brType : map.keySet())
            {
                if (!allTypes.contains(brType))
                {
                    allTypes.add(brType);
                    if (map.get(brType))
                    {
                        activeTypes.add(brType);
                    }
                }
            }
        }

        var res = new BarBoxConfig(allTypes.toArray(String[]::new));
        res = res.getUpdatedConfig(activeTypes.toArray(String[]::new));

        return res;
    }

    private void setBarAnnotationNbLines(int n)
    {
        for (var bb : getBarBoxes())
        {
            for (var br : bb.getBarRenderers())
            {
                if (br instanceof BR_Annotation bra)
                {
                    bra.setNbLines(n);
                }
            }
        }
    }

    private void setBarAnnotationVisible(boolean b)
    {
        var bbConfig = getBarBoxConfig(0);            // All BarBoxes share the same config
        var activeBrs = bbConfig.getActiveBarRenderers();
        assert b == !activeBrs.contains(BarRendererFactory.BR_ANNOTATION) : "b=" + b + " activeBrs)" + activeBrs;
        if (b)
        {
            activeBrs.add(BarRendererFactory.BR_ANNOTATION);
        } else
        {
            activeBrs.remove(BarRendererFactory.BR_ANNOTATION);
        }

        // Save selection
        CL_Selection selection = new CL_Selection(getLookup());
        int bar = 0;
        if (selection.isBarSelected())
        {
            bar = selection.getMinBarIndex();
        } else if (selection.isItemSelected())
        {
            bar = selection.getSelectedItems().getFirst().getPosition().getBar();
        }
        clearSelection(selection);


        // Update BarBoxes
        setBarBoxConfig(bbConfig.getUpdatedConfig(activeBrs.toArray(String[]::new)));


        // Restore selection (at least 1 bar) so that the the "toggle BR_Annotation visibility" keyboard shortcut can be directly reused
        selectBars(bar, bar, true);
        setFocusOnBar(bar);
    }

    private int computeXZoomFactorFromNbCols(int nbCols)
    {
        int xFactor = (int) Math.round((16 - nbCols) * 100.0 / 15.0);
        return xFactor;
    }

    private int computeNbColsFromXZoomFactor(int xFactor)
    {
        int nbCols = 16 - (int) Math.round(15.0 * xFactor / 100.0);
        return nbCols;
    }

    /**
     * The total number of rows taking into account BarBoxes (within chord leadhseet and after) and the PaddingBoxes.
     *
     * @return
     */
    private int getNbRows()
    {
        return gridLayout.getRows();
    }

    /**
     * Get the row of the specified BarBox.
     *
     * @param bbIndex
     * @return
     */
    private int getRowIndex(int bbIndex)
    {
        int compIndex = getComponentIndex(bbIndex);
        return compIndex / getNbColumns();

    }

    private void removePaddingBox(PaddingBox pd)
    {
        pd.cleanup();
        remove(pd);
    }

    /**
     * Remove/Add PaddingBoxes so that "start on new line" sections appear on a new line.
     */
    private void updatePaddingBoxes()
    {
        assert SwingUtilities.isEventDispatchThread();
        boolean needRevalidate = false;
        // Remove all non-BarBox components
        for (Component c : getComponents())
        {
            if (c instanceof PaddingBox pBox)
            {
                needRevalidate = true;
                removePaddingBox(pBox);
            }
        }

        // Add PaddingBoxes starting from the end
        var cliSections = clsModel.getItems(CLI_Section.class);
        int offset = 0;
        for (CLI_Section cliSection : cliSections)
        {
            int sectionCompIndex = cliSection.getPosition().getBar() + offset;
            int remainder = sectionCompIndex % nbColumns;
            if (CL_EditorClientProperties.isSectionIsOnNewLine(cliSection) && remainder != 0)
            {
                int padding = nbColumns - remainder;
                for (int j = 0; j < padding; j++)
                {
                    needRevalidate = true;
                    add(new PaddingBox(settings.getBarBoxSettings()), sectionCompIndex);
                }
                offset += padding;
            }
        }

        if (needRevalidate)
        {
            revalidate();
        }
    }

    //===========================================================================
    // Inner classes
    //===========================================================================       
    /**
     * Implements the Zoomable functionalities.
     */
    private class CL_EditorZoomable implements Zoomable
    {

        private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

        @Override
        public Zoomable.Capabilities getZoomCapabilities()
        {
            return Zoomable.Capabilities.X_Y;
        }

        @Override
        public int getZoomYFactor()
        {
            return CL_EditorClientProperties.getZoomYFactor(songModel);
        }

        @Override
        public void setZoomYFactor(int newFactor, boolean valueIsAdjusting)
        {
            int oldFactor = getZoomYFactor();
            CL_EditorClientProperties.setZoomYFactor(songModel, newFactor);
            pcs.firePropertyChange(Zoomable.PROPERTY_ZOOM_Y, oldFactor, newFactor);
        }

        @Override
        public int getZoomXFactor()
        {
            int factor = computeXZoomFactorFromNbCols(nbColumns);
            LOGGER.log(Level.FINER, "getZoomFactor() nbColumns={0} factor={1}", new Object[]
            {
                nbColumns, factor
            });
            return factor;
        }

        @Override
        public void setZoomXFactor(int factor, boolean valueIsAdjusting)
        {
            int nbCols = computeNbColsFromXZoomFactor(factor);
            LOGGER.log(Level.FINER, "setZoomFactor() factor={0}> nbCols={1}", new Object[]
            {
                factor, nbCols
            });
            setNbColumns(nbCols);      // This will fire the event
        }

        @Override
        public void addPropertyListener(PropertyChangeListener l)
        {
            pcs.addPropertyChangeListener(l);
        }

        @Override
        public void removePropertyListener(PropertyChangeListener l)
        {
            pcs.removePropertyChangeListener(l);
        }

        @Override
        public void setZoomYFactorToFitContent()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void setZoomXFactorToFitContent()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }

}
