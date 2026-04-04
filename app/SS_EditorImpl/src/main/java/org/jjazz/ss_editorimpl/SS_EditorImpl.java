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
package org.jjazz.ss_editorimpl;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.BorderLayout;
import java.awt.Color;
import org.jjazz.ss_editor.api.SS_Selection;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TooManyListenersException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.beans.PropertyChangeSupport;
import javax.swing.BorderFactory;
import javax.swing.JLayer;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import org.jjazz.harmony.api.Position;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptRenamedEvent;
import org.jjazz.songstructure.api.event.SptRhythmParentSectionChangedEvent;
import org.jjazz.ss_editor.sptviewer.api.SptViewer;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerFactory;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.spi.SS_EditorSettings;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.ss_editor.rpviewer.api.RpViewer;
import org.jjazz.uiutilities.api.Zoomable;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;
import org.jjazz.song.api.Song;
import org.openide.awt.UndoRedo;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_EditorClientProperties;
import org.jjazz.ss_editor.api.SS_EditorClientProperties.ViewMode;
import static org.jjazz.ss_editor.api.SS_EditorClientProperties.getCompactViewModeVisibleRPs;
import static org.jjazz.ss_editor.api.SS_EditorClientProperties.getRhythmIdFromCompactViewRhythmPropertyName;
import static org.jjazz.ss_editor.api.SS_EditorClientProperties.setZoomXFactor;
import static org.jjazz.ss_editor.api.SS_EditorClientProperties.getZoomXFactor;
import static org.jjazz.ss_editor.api.SS_EditorClientProperties.getZoomYFactor;
import static org.jjazz.ss_editor.api.SS_EditorClientProperties.getViewMode;
import static org.jjazz.ss_editor.api.SS_EditorClientProperties.setCompactViewModeVisibleRPs;
import org.jjazz.ss_editor.api.SS_EditorMouseListener;
import org.jjazz.ss_editor.api.SelectedSongPart;
import org.jjazz.ss_editor.sptviewer.api.SptViewerConfig;
import org.jjazz.ss_editor.sptviewer.api.SptViewerConfig.MultiSelect;
import org.jjazz.ss_editorimpl.sptviewer.SptViewerLow;
import org.jjazz.ss_editorimpl.sptviewer.SptViewerTop;
import org.jjazz.utilities.api.IdentityBasedInstanceContent;

/**
 * An implementation of the SongStructure editor.
 */

public class SS_EditorImpl extends SS_Editor implements PropertyChangeListener, SgsChangeListener
{


    // UI variables
    private JScrollPane scrollPane;
    private JPanel panel_Top;
    private JPanel panel_Low;
    private InsertionSptMark insertionMarkTop;
    private InsertionSptMark insertionMarkLow;
    private int playbackPointX;
    /**
     * Our sgsModel.
     */
    private SongStructure sgsModel;
    private Song songModel;

    /**
     * Our controller.
     */
    private SS_EditorMouseListener controller;
    /**
     * Our UndoManager.
     */
    private final JJazzUndoManager undoManager;
    /**
     * Our global lookup.
     */
    private Lookup lookup;
    /**
     * The lookup for the selection.
     */
    private Lookup selectionLookup;
    /**
     * Instance content behind the selection lookup.
     */
    private InstanceContent selectionLookupContent;
    /**
     * Last snapshot of objects in selectionLookupContent.
     */
    private List<Object> selectionLastContent;
    /**
     * The lookup for non-selection stuff.
     */
    private Lookup generalLookup;
    /**
     * Store non-selection stuff.
     */
    private IdentityBasedInstanceContent generalLookupContent;      // because we add mutable items (see bug https://github.com/apache/netbeans/issues/9270)
    /**
     * The last spt index of the insertion point.
     */
    private int insertionMarkSptIndex;
    /**
     * Our Drag and Drop handler for this editor and SptViewers.
     */
    private SS_EditorTransferHandler transferHandler;
    /**
     * Our listener to manager the drops disturbed with a focus change.
     */
    private DTListener dropTargetListener;
    private SS_EditorZoomable zoomable;
    /**
     * Store the visible RPs for each rhythm.
     */
    private final Map<Rhythm, List<RhythmParameter<?>>> mapRhythmVisibleRps;
    /**
     * Editor settings.
     */
    private SS_EditorSettings settings;
    private SptViewerFactory sptViewerFactory;
    private SS_EditorLayerUI layerUI;
    private JLayer layer;
    private static final Logger LOGGER = Logger.getLogger(SS_EditorImpl.class.getSimpleName());

    /**
     * Creates new form SS_EditorImpl.
     *
     *
     * @param song
     * @param settings
     * @param factory
     */
    public SS_EditorImpl(Song song, SS_EditorSettings settings, SptViewerFactory factory)
    {
        Objects.requireNonNull(song);
        Objects.requireNonNull(settings);
        Objects.requireNonNull(factory);


        // This is the main part to fix Issue #582 (see also SS_EditorController.editorClicked() and SS_EditorTopComponent.componentActivated())
        // It allows the editor to get the focus when e.g. a focused child is removed (no more selected component), thus making the editor's InputMap/ActionMap still 
        // active: this way always-enabled actions like ToggleCompactView can still be activated via their keyboard shortcut.
        setFocusable(true);


        this.settings = settings;
        songModel = song;
        sgsModel = song.getSongStructure();
        sptViewerFactory = factory;


        // Make sure all Rhythms have a default list of visible RPs in compact view mode
        storeVisibleRPsInCompactModeIfRequired(song.getSongStructure().getUniqueRhythms(false, true));


        songModel.getClientProperties().addPropertyChangeListener(this);


        // The lookup for selection
        selectionLookupContent = new InstanceContent();
        selectionLookup = new AbstractLookup(selectionLookupContent);
        selectionLastContent = new ArrayList<>();

        // The lookup for other stuff
        generalLookupContent = new IdentityBasedInstanceContent();
        generalLookup = new AbstractLookup(generalLookupContent);

        // Our implementation is made "Zoomable" by controllers
        // Initialize with actionmap, our Zoomable object   
        zoomable = new SS_EditorZoomable();
        generalLookupContent.add(zoomable);
        generalLookupContent.add(getActionMap());

        // Global lookup = sum of both
        lookup = new ProxyLookup(selectionLookup, generalLookup);

        // No spt drag operation
        insertionMarkSptIndex = -1;

        // Our drag & drop handler for this editor 
        transferHandler = new SS_EditorTransferHandler(this);
        setTransferHandler(transferHandler);
        dropTargetListener = new DTListener();

        // transferHandler does not manage the case where a CL_Editor section is dragged in the SS_Editor
        // and there is a focus change (ALT-TAB) => drop is not done but insertionPoint is not removed !
        // Here we listen to dropTarget events, if it exists the editor's bound, make sure
        // insertionPoint is turned off.
        try
        {
            getDropTarget().addDropTargetListener(dropTargetListener);
        } catch (TooManyListenersException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        mapRhythmVisibleRps = new HashMap<>();


        // Graphical init
        initUIComponents();

        // Listen to our models
        sgsModel.addSgsChangeListener(this);

        // Connect our undomanager to our model
        undoManager = JJazzUndoManagerFinder.getDefault().get(sgsModel);
        assert undoManager != null : "model=" + sgsModel;
        sgsModel.addUndoableEditListener(undoManager);

        // Fill our lookup
        generalLookupContent.add(sgsModel);    // sgsModel is mutable but this is OK, we never remove it so no bug https://github.com/apache/netbeans/issues/9270
        generalLookupContent.add(songModel);   // song is mutable but this is OK, we never remove it so no bug https://github.com/apache/netbeans/issues/9270


        // Restore view mode
        var viewMode = getViewMode(songModel);
        var allRhythms = sgsModel.getUniqueRhythms(false, true);
        for (var r : allRhythms)
        {
            List<RhythmParameter<?>> rps = viewMode.equals(ViewMode.COMPACT) ? getCompactViewModeVisibleRPs(songModel, r) : r.getRhythmParameters();
            mapRhythmVisibleRps.put(r, rps);
        }


        // Add the SongPartEditors
        for (SongPart spt : sgsModel.getSongParts())
        {
            addSptViewer(spt);
        }
        updateSptConfigs();
    }

    @Override
    public SS_EditorSettings getSettings()
    {
        return settings;
    }

    @Override
    public SptViewerFactory getSptViewerRendererFactory()
    {
        return sptViewerFactory;
    }

    @Override
    public UndoRedo getUndoManager()
    {
        return undoManager;
    }

    @Override
    public SongStructure getModel()
    {
        return sgsModel;
    }

    @Override
    public Song getSongModel()
    {
        return songModel;
    }

    @Override
    public void setController(SS_EditorMouseListener controller)
    {
        this.controller = controller;
        for (SptViewer sptv : this.getSptViewers())
        {
            sptv.setController(controller);
        }
    }


    @Override
    public void cleanup()
    {
        // Unselect everything
        SS_Selection selection = new SS_Selection(selectionLookup);
        selection.unselectAll(this);

        // Unregister the objects we were listening to
        songModel.getClientProperties().removePropertyChangeListener(this);
        sgsModel.removeSgsChangeListener(this);
        sgsModel.removeUndoableEditListener(undoManager);
        generalLookupContent.remove(sgsModel);

        generalLookupContent.remove(songModel);

        // Remove everything
        for (SptViewer sptv : this.getSptViewers())
        {
            removeSptViewer(sptv);
        }
    }


    @Override
    public void selectSongPart(SongPart spt, boolean b)
    {
        assert spt != null;
        if (isSelected(spt) == b)
        {
            return;
        }

        getSptViewerLow(spt).setSelected(b);
        getSptViewerTop(spt).setSelected(b);


        var selSpt = new SelectedSongPart(spt);
        if (b)
        {
            // Warning ! If item is mutable, make sure item uses Object's equals() and hashCode() !
            selectionLookupContent.add(selSpt);
            selectionLastContent.add(selSpt);
        } else
        {
            // Warning ! Might not work if item was mutated with equals()/hashCode() defined !
            selectionLookupContent.remove(selSpt);
            selectionLastContent.remove(selSpt);
        }
        // LOGGER.log(Level.FINE, "After selectSongPart() b=" + b + " spt=" + spt + " lkp=" + lookup);
    }

    @Override
    public void selectRhythmParameter(SongPart spt, RhythmParameter<?> rp, boolean b)
    {
        assert rp != null && spt != null;
        if (isSelected(spt, rp) == b)
        {
            return;
        }

        getSptViewerLow(spt).setSelected(rp, b);
        getSptViewerTop(spt).setSelected(rp, b);


        SongPartParameter sptp = new SongPartParameter(spt, rp);
        if (b)
        {
            // Warning ! If item is mutable, make sure item uses Object's equals() and hashCode() !
            selectionLookupContent.add(sptp);
            selectionLastContent.add(sptp);
        } else
        {
            // Warning ! Might not work if item was mutated with equals()/hashCode() defined !
            selectionLookupContent.remove(sptp);
            selectionLastContent.remove(sptp);
        }
        LOGGER.log(Level.FINE, "After selectRhythmParameter() b={0} spt={1} rp={2} lkp={3}", new Object[]
        {
            b, spt, rp, lookup
        });
    }

    @Override
    public void setFocusOnSongPart(SongPart spt)
    {
        assert spt != null;
        getSptViewerTop(spt).requestFocusInWindow();
    }

    @Override
    public void setFocusOnRhythmParameter(SongPart spt, RhythmParameter<?> rp)
    {
        assert spt != null;
        getSptViewerLow(spt).setFocusOnRpViewer(rp);
    }

    @Override
    public Rectangle getSptViewerRectangle(SongPart spt)
    {
        Preconditions.checkArgument(sgsModel.getSongParts().contains(spt), "spt=%s sgsModel=%s", spt, sgsModel);

        SptViewer sptv = getSptViewerTop(spt);
        Point p = sptv.getLocationOnScreen();
        Rectangle r = new Rectangle(p);
        r.width = sptv.getWidth();
        r.height = sptv.getHeight();
        return r;
    }

    @Override
    public Rectangle getRpViewerRectangle(SongPart spt, RhythmParameter<?> rp)
    {
        Preconditions.checkArgument(sgsModel.getSongParts().contains(spt), "spt=%s sgsModel=%s", spt, sgsModel);
        SptViewer sptv = getSptViewerLow(spt);
        return sptv.getRpViewerRectangle(rp);
    }

    @Override
    public SongPartParameter getSongPartParameterFromPoint(Point editorPoint, AtomicBoolean sptLeft)
    {
        SongPart spt = null;
        RhythmParameter<?> rp = null;

        // Find the component within this editor which is under this editorPoint
        Component c = SwingUtilities.getDeepestComponentAt(this, editorPoint.x, editorPoint.y);

        // Simple case: check if we're on known components. Recurse the hierarchy
        // Assume RpViewer bounds are WITHIN SptViewer bounds
        while (c != null)
        {
            if (c instanceof RpViewer rpViewer)
            {
                rp = rpViewer.getRpModel();
                spt = rpViewer.getSptModel();
                break;
            } else if (c instanceof SptViewer sptViewer)
            {
                spt = sptViewer.getModel();
                break;
            } else if (c == scrollPane)
            {
                c = null;
                break;
            }
            c = c.getParent();
        }


        if (c != null)
        {
            // We're on a SongPart or a RhythmParameter
            Point cPoint = SwingUtilities.convertPoint(this, editorPoint, c);
            if (cPoint.x > c.getWidth() / 2)
            {
                sptLeft.set(false);

            } else
            {
                sptLeft.set(true);
            }
            return new SongPartParameter(spt, rp);

        } else
        {
            // We're somewhere on the editor, use a different method to find at least a SongPart
            Rectangle r = new Rectangle();
            int i = 0;
            for (SptViewer sptv : getSptViewersLow())
            {
                sptv.getBounds(r);
                SwingUtilities.convertRectangle(panel_Low, r, this);
                if (editorPoint.x <= r.x + r.width / 2)
                {
                    spt = sgsModel.getSongParts().get(i);
                    sptLeft.set(true);
                    break;
                } else if (editorPoint.x <= r.x + r.width)
                {
                    spt = sgsModel.getSongParts().get(i);
                    sptLeft.set(false);
                    break;
                }
                i++;
            }
            if (spt == null && !sgsModel.getSongParts().isEmpty())
            {
                assert i > 0;
                // We're after the last SongPart, use the last SongPart
                spt = sgsModel.getSongParts().get(i - 1);
                sptLeft.set(false);
            }
            return new SongPartParameter(spt, rp);
        }
        // LOGGER.log(Level.FINE, "getSongPartParameterFromPoint() spt=" + spt + " sptLeft=" + sptLeft.toString());
    }

    @Override
    public void showSptInsertionMark(boolean b, int sptIndex, boolean copyMode)
    {
        LOGGER.log(Level.FINE, "showSptInsertionMark() b={0} sptIndex={1} copyMode={2}", new Object[]
        {
            b, sptIndex, copyMode
        });
        if (!b)
        {
            panel_Top.remove(insertionMarkTop);
            panel_Low.remove(insertionMarkLow);
            insertionMarkSptIndex = -1;
            panel_Top.revalidate();
            panel_Top.repaint(); // Needed to erase the insertionMark if it was at last index position
            panel_Low.revalidate();
            panel_Low.repaint(); // Needed to erase the insertionMark if it was at last index position
            return;
        }
        if (insertionMarkSptIndex == -1)
        {
            // First display, adjust size
            insertionMarkTop.setPreferredSize(new Dimension(insertionMarkTop.getPreferredSize().width, 50));
            insertionMarkLow.setPreferredSize(insertionMarkTop.getPreferredSize());
        }
        insertionMarkTop.setCopyMode(copyMode);
        insertionMarkLow.setCopyMode(copyMode);

        if (insertionMarkSptIndex != sptIndex)
        {
            insertionMarkSptIndex = sptIndex;

            panel_Top.remove(insertionMarkTop);
            panel_Top.add(insertionMarkTop, insertionMarkSptIndex);
            panel_Top.revalidate();
            panel_Top.repaint(); // If added in last position

            panel_Low.remove(insertionMarkLow);
            panel_Low.add(insertionMarkLow, insertionMarkSptIndex);
            panel_Low.revalidate();
            panel_Low.repaint(); // If added in last position

        }
    }

    @Override
    public List<RhythmParameter<?>> getVisibleRps(Rhythm r)
    {
        if (!sgsModel.getUniqueRhythms(false, true).contains(r))
        {
            throw new IllegalArgumentException("r=" + r + " sgsModel=" + sgsModel);
        }

        List<RhythmParameter<?>> rps = mapRhythmVisibleRps.get(r);
        if (rps == null)
        {
            // Show all the RhythmParameters by default
            rps = r.getRhythmParameters();
            mapRhythmVisibleRps.put(r, rps);
        }

        return new ArrayList<>(rps);
    }

    @Override
    public void showPlaybackPoint(boolean show, Position pos)
    {
        LOGGER.log(Level.FINE, "showPlaybackPoint() show={0} pos={1}", new Object[]
        {
            show, pos
        });
        if (show && pos == null)
        {
            throw new IllegalArgumentException("show=" + show + " pos=" + pos);
        }

        // Get the x position in panel_Low
        int xPos = show ? computeXpos(pos) : -1;


        // Make sure it is visible
        var viewRect = scrollPane.getViewport().getViewRect();
        final int DELTA = 20;
        viewRect.width -= DELTA;
        if (!viewRect.contains(xPos, viewRect.y))
        {
            int x = xPos >= viewRect.x  ? xPos + viewRect.width : xPos - DELTA;
            var r = new Rectangle(x, viewRect.y, 1, 1);
            panel_Low.scrollRectToVisible(r);
        }


        // Translate x into the layer coordinates since we'll draw on it, "above" the JScrollPane
        int oldPlaybackPointX = playbackPointX;
        playbackPointX = xPos - scrollPane.getViewport().getViewPosition().x;


        // Render the playback point
        layerUI.setPlaybackPoint(playbackPointX);


        // Repaint impacted zone
        int SIDE = SS_EditorLayerUI.SIDE + 1;
        int x0, x1;
        if (oldPlaybackPointX == -1)
        {
            x0 = xPos - SIDE;
            x1 = xPos + SIDE;
        } else if (playbackPointX == -1)
        {
            x0 = oldPlaybackPointX - SIDE;
            x1 = oldPlaybackPointX + SIDE;
        } else
        {
            x0 = Math.min(xPos, oldPlaybackPointX) - SIDE;
            x1 = Math.max(xPos, oldPlaybackPointX) + SIDE;
        }
        int w = x1 - x0 + 1;
        int h = getHeight();
        layer.repaint(x0, 0, w, h);
    }

    @Override
    public void makeSptViewerVisible(SongPart spt)
    {
        SptViewer sptv = getSptViewerLow(spt);
        if (sptv == null)
        {
            throw new IllegalStateException("spt=" + spt);
        }
        scrollRectToVisible(sptv.getBounds());
    }

    @Override
    public void setZoomHFactorToFitWidth(int width)
    {
        if (width < 0)
        {
            throw new IllegalArgumentException("width=" + width);
        }
        setZoomXFactor(songModel, 100);
        while (getPreferredSize().width > width && getZoomXFactor(songModel) > 0)
        {
            setZoomXFactor(songModel, Math.max(0, getZoomXFactor(songModel) - 2));
        }
    }

    @Override
    public SongPart getFocusedSongPart(boolean includeFocusedRhythmParameter)
    {
        SongPart spt = null;
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof SptViewer sptViewer)
        {
            spt = sptViewer.getModel();
        } else if (includeFocusedRhythmParameter && (c instanceof RpViewer))
        {
            spt = ((RpViewer) c).getSptModel();
        }
        return spt;
    }

    public String toString()
    {
        return "SS_EditorImpl[" + songModel.getName() + "]";
    }

    //------------------------------------------------------------------------------
    // LookupProvider interface
    //------------------------------------------------------------------------------
    @Override
    public Lookup getLookup()
    {
        return lookup;
    }

    //------------------------------------------------------------------------------
    // PropertyChangeListener interface
    //------------------------------------------------------------------------------
    @Override
    public void propertyChange(final PropertyChangeEvent evt)
    {
        if (evt.getSource() == songModel.getClientProperties())
        {
            switch (evt.getPropertyName())
            {
                case SS_EditorClientProperties.PROP_ZOOM_FACTOR_X, SS_EditorClientProperties.PROP_ZOOM_FACTOR_Y ->
                {
                    boolean isX = evt.getPropertyName().equals(SS_EditorClientProperties.PROP_ZOOM_FACTOR_X);
                    int zFactor = isX ? getZoomXFactor(songModel) : getZoomYFactor(songModel);
                    for (SptViewer sptv : getSptViewers())
                    {
                        if (isX)
                        {
                            sptv.setZoomHFactor(zFactor);
                        } else
                        {
                            sptv.setZoomVFactor(zFactor);
                        }
                    }
                    revalidate();
                    repaint();
                }
                case SS_EditorClientProperties.PROP_VIEW_MODE ->
                {
                    boolean compact = getViewMode(songModel).equals(ViewMode.COMPACT);
                    var allRhythms = sgsModel.getUniqueRhythms(false, true);

                    for (var r : allRhythms)
                    {
                        List<RhythmParameter<?>> rps = compact ? getCompactViewModeVisibleRPs(songModel, r) : r.getRhythmParameters();
                        setVisibleRps(r, rps);
                    }
                }
                default ->
                {
                    var rId = getRhythmIdFromCompactViewRhythmPropertyName(evt.getPropertyName());
                    if (rId != null)
                    {
                        // Compact RP list for rhythm rId has changed
                        if (getViewMode(songModel).equals(ViewMode.COMPACT))
                        {
                            var r = sgsModel.getSongParts().stream()
                                    .filter(spt -> spt.getRhythm().getUniqueId().equals(rId))
                                    .map(spt -> spt.getRhythm())
                                    .findAny()
                                    .orElseThrow(() -> new IllegalStateException("rId=" + rId + " sgsModel=" + sgsModel));
                            var rps = getCompactViewModeVisibleRPs(songModel, r);
                            setVisibleRps(r, rps);
                        }
                    }
                }
            }
        }
    }

    //------------------------------------------------------------------------------
    // SgsChangeListener interface
    //------------------------------------------------------------------------------   
    @Override
    public void songStructureChanged(final SgsChangeEvent event)
    {
        LOGGER.log(Level.FINE, "songStructureChanged() -- event={0} spts={1}", new Object[]
        {
            event, event.getSongParts()
        });

        switch (event)
        {
            case SptRemovedEvent e -> handleSptRemoved(e);
            case SptAddedEvent e -> handleSptAdded(e);
            case SptRenamedEvent e -> updateSptConfigs();
            case SptRhythmParentSectionChangedEvent e ->
            {
                // Event is directly managed by each SptViewer, here we only do stuff which impacts all SptViewers
                storeVisibleRPsInCompactModeIfRequired(e.getSongParts().stream()
                        .map(spt -> spt.getRhythm())
                        .toList());
                updateSptConfigs();
            }
            default ->
            {
                // nothing (directly managed by SptViewers)
            }
        }
    }

    //------------------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------------------      
    private void handleSptRemoved(SptRemovedEvent e)
    {
        if (!e.isUndo())
        {
            for (SongPart spt : e.getSongParts())
            {
                for (var sptv : getSptViewers(spt))
                {
                    removeSptViewer(sptv);
                }
            }
        } else
        {
            for (SongPart spt : e.getSongParts())
            {
                addSptViewer(spt);
            }
        }

        updateSptConfigs();

        panel_Low.revalidate();
        panel_Low.repaint();     // Needed if removed Spt was the last one
        panel_Top.revalidate();
        panel_Top.repaint();
    }

    private void handleSptAdded(final SptAddedEvent e)
    {
        if (!e.isUndo())
        {
            for (SongPart spt : e.getSongParts())
            {
                addSptViewer(spt);
            }
            storeVisibleRPsInCompactModeIfRequired(e.getSongParts().stream()
                    .map(spt -> spt.getRhythm())
                    .toList());
        } else
        {
            for (SongPart spt : e.getSongParts())
            {
                for (var sptv : getSptViewers(spt))
                {
                    removeSptViewer(sptv);
                }
            }
        }

        updateSptConfigs();

        panel_Low.revalidate();  // Needed to get immediate UI update
        panel_Low.repaint();     // Needed if removed Spt was the last one                
        panel_Top.revalidate();
        panel_Top.repaint();

    }


    private void initUIComponents()
    {
        // Build UI objects
        setOpaque(false);       // To reuse LAF default background

        insertionMarkTop = new InsertionSptMark();
        insertionMarkLow = new InsertionSptMark();
        insertionMarkLow.setEnabled(false);


        panel_Top = new SptViewerPanel();
        var border = BorderFactory.createEmptyBorder(0, 0, 1, 0);       // leave small space on bottom to separate panel_Top from panel_SongParts
        panel_Top.setBorder(border);
        panel_Low = new SptViewerPanel();


        scrollPane = new JScrollPane();
        scrollPane.setViewportView(panel_Low);
        scrollPane.setColumnHeaderView(panel_Top);

        layerUI = new SS_EditorLayerUI();
        layer = new JLayer(scrollPane, layerUI);

        setLayout(new BorderLayout());
        add(layer);


        // We need to add a MouseListener to scrollPane to get the popupmenu (righ-click) and zoom (ctrl+mousewheel) to work when mouse if outside of 
        // panel_Low and panel_Top. But our listener won't prevent scrollPane's own internal listener to also process the MouseEvent. So when scrollbar is visible, 
        // ctrl-mousewheel performs the zoom but ALSO scrolls the editor.
        // Prevent the scroll pane from scrolling on ctrl+wheel: wrap built-in scroll listeners to ignore ctrl+wheel events
        for (var l : scrollPane.getMouseWheelListeners())
        {
            scrollPane.removeMouseWheelListener(l);
            scrollPane.addMouseWheelListener(e -> 
            {
                if (!e.isControlDown())
                {
                    l.mouseWheelMoved(e);
                }
            });
        }

        var mListener = new MouseAdapter()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                if (controller == null)
                {
                    return;
                }
                controller.editorWheelMoved(e);
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                if (controller == null)
                {
                    return;
                }
                Component c = (Component) e.getSource();
                LOGGER.log(Level.FINE, "mousePressed() c={0}", c);
                controller.editorClicked(e);
            }
        };
        scrollPane.addMouseListener(mListener);
        scrollPane.addMouseWheelListener(mListener);


    }


    private void addSptViewer(SongPart spt)
    {
        assert spt != null;

        // Low SptViewer
        SptViewer sptv = sptViewerFactory.createLowSptViewer(this,
                spt,
                sptViewerFactory.getDefaultSptViewerSettings(),
                sptViewerFactory.getDefaultRpViewerFactory());
        registerSptViewer(sptv);
        sptv.setZoomHFactor(getZoomXFactor(songModel));
        sptv.setZoomVFactor(getZoomYFactor(songModel));
        List<RhythmParameter<?>> rps = getVisibleRps(spt.getRhythm());
        sptv.setConfig(sptv.getConfig().setVisibleRPs(rps));
        int index = sgsModel.getSongParts().indexOf(spt);
        assert index >= 0 : "spt=" + spt + " model.getSongParts()=" + sgsModel.getSongParts();
        LOGGER.log(Level.FINE, "addSptViewer() spt={0} +index={1} panel_SongParts.size={2}", new Object[]
        {
            spt, index,
            panel_Low.
            getComponentCount()
        });
        panel_Low.add(sptv, index);


        // Top SptViewer
        SptViewer sptvTop = sptViewerFactory.createTopSptViewer(this, spt, sptViewerFactory.getDefaultSptViewerSettings());
        registerSptViewer(sptvTop);
        sptvTop.setZoomHFactor(getZoomXFactor(songModel));
        sptvTop.setZoomVFactor(getZoomYFactor(songModel));
        panel_Top.add(sptvTop, index);
    }

    private void removeSptViewer(SptViewer sptv)
    {
        assert sptv != null;
        SongPart spt = sptv.getModel();
        // Unselect everything related to this SptViewer
        selectSongPart(spt, false);
        for (RhythmParameter<?> rp : spt.getRhythm().getRhythmParameters())
        {
            selectRhythmParameter(spt, rp, false);
        }
        unregisterSptViewer(sptv);
        panel_Low.remove(sptv);
        panel_Top.remove(sptv);
        sptv.cleanup();
    }

    private List<SptViewer> getSptViewersTop()
    {
        List<SptViewer> res = new ArrayList<>();
        for (Component c : this.panel_Top.getComponents())
        {
            if (c instanceof SptViewer sptViewer)
            {
                res.add(sptViewer);
            }
        }
        return res;
    }

    private List<SptViewer> getSptViewersLow()
    {
        List<SptViewer> res = new ArrayList<>();
        for (Component c : this.panel_Low.getComponents())
        {
            if (c instanceof SptViewer sptViewer)
            {
                res.add(sptViewer);
            }
        }
        return res;
    }

    private List<SptViewer> getSptViewers()
    {
        ArrayList<SptViewer> res = new ArrayList<>();
        res.addAll(getSptViewersTop());
        res.addAll(getSptViewersLow());
        return res;
    }

    private List<SptViewer> getSptViewers(SongPart spt)
    {
        return getSptViewers().stream()
                .filter(sptv -> sptv.getModel() == spt)
                .toList();
    }

    private SptViewer getSptViewerTop(SongPart spt)
    {
        for (var sptv : getSptViewers())
        {
            if (sptv.getModel() == spt && sptv.getClass() == SptViewerTop.class)
            {
                return sptv;
            }
        }
        throw new IllegalStateException("spt=" + spt);
    }

    private SptViewer getSptViewerLow(SongPart spt)
    {
        for (var sptv : getSptViewers())
        {
            if (sptv.getModel() == spt && sptv.getClass() == SptViewerLow.class)
            {
                return sptv;
            }
        }
        throw new IllegalStateException("spt=" + spt);
    }


    /**
     * Register a SongPart
     *
     * @param sptv
     */
    private void registerSptViewer(final SptViewer sptv)
    {
        sptv.setController(controller);
        sptv.setTransferHandler(transferHandler);
        // transferHandler does not manage the case where a CL_Editor section is dragged into the SS_Editor
        // and there is a focus change (ALT-TAB) => drop is not done but insertionPoint is not removed !
        // Here we listen to dropTarget events, if it exists the SptViewer's bound, make sure
        // insertionPoint is turned off.
        try
        {
            sptv.getDropTarget().addDropTargetListener(dropTargetListener);
        } catch (TooManyListenersException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Unregister a SongPart
     *
     * @param sptv
     */
    private void unregisterSptViewer(SptViewer sptv)
    {
        sptv.getDropTarget().removeDropTargetListener(dropTargetListener);
        sptv.setController(null);
    }

    private boolean isSelected(SongPart spt)
    {
        return selectionLastContent.contains(new SelectedSongPart(spt));
    }

    private boolean isSelected(SongPart spt, RhythmParameter<?> rp)
    {
        return selectionLastContent.contains(new SongPartParameter(spt, rp));
    }

    /**
     * Update the SptViewerConfig of all SptViewers.
     */
    private void updateSptConfigs()
    {
        Map<SptViewer, SptViewerConfig> mapViewerConfig = new HashMap<>();
        Map<SptViewer, Integer> mapViewerNameRepeatCount = new HashMap<>();

        SptViewer lastSptv = null;
        SptViewer firstSptvNameRepeat = null;
        var sptvs = getSptViewers();
        for (SptViewer sptv : sptvs)
        {
            SongPart spt = sptv.getModel();
            var r = spt.getRhythm();
            var uiConfig = sptv.getConfig();


            boolean isNameRepeat = lastSptv != null && spt.getName().equals(lastSptv.getModel().getName());
            if (isNameRepeat)
            {
                if (firstSptvNameRepeat == null)
                {
                    firstSptvNameRepeat = lastSptv;
                    mapViewerNameRepeatCount.put(firstSptvNameRepeat, 0);
                }
                mapViewerNameRepeatCount.compute(firstSptvNameRepeat, (key, value) -> value + 1);
            } else
            {
                firstSptvNameRepeat = null;
            }


            uiConfig = uiConfig.setShowName(!isNameRepeat);
            uiConfig = uiConfig.setShowTimeSignature(lastSptv == null || !r.getTimeSignature().equals(lastSptv.getModel().getRhythm().getTimeSignature()));
            uiConfig = uiConfig.setShowParentSection(!spt.getName().equals(spt.getParentSection().getData().getName()));
            uiConfig = uiConfig.setShowRhythm(lastSptv == null || r != lastSptv.getModel().getRhythm());
            uiConfig = uiConfig.setMultiSelect(MultiSelect.OFF);
            mapViewerConfig.put(sptv, uiConfig);

            lastSptv = sptv;
        }


        // Update MultiSelect
        for (var sptv : mapViewerNameRepeatCount.keySet())
        {
            int nbRepeats = mapViewerNameRepeatCount.get(sptv);
            int index = sptvs.indexOf(sptv);
            var config = mapViewerConfig.get(sptv);
            mapViewerConfig.put(sptv, config.setMultiSelect(MultiSelect.ON_FIRST));

            for (int i = index + 1; i <= index + nbRepeats; i++)
            {
                var sptvi = sptvs.get(i);
                config = mapViewerConfig.get(sptvi);
                mapViewerConfig.put(sptvi, config.setMultiSelect(MultiSelect.ON));
            }
        }


        // Apply the UI configs
        for (var sptv : mapViewerConfig.keySet())
        {
            sptv.setConfig(mapViewerConfig.get(sptv));
        }

    }

    /**
     * Sort rps to have the same order than r.getRhythmParameters().
     *
     * @param r
     * @param rps
     * @return
     */
    private List<RhythmParameter<?>> sortRhythmParameters(Rhythm r, List<RhythmParameter<?>> rps)
    {
        List<RhythmParameter<?>> res = r.getRhythmParameters()
                .stream()
                .filter(rp -> rps.contains(rp))
                .toList();
        return res;
    }

    /**
     * Update the visible RPs for a given rhythm.
     * <p>
     * Fire a PROP_RHYHM_VISIBLE_RPS change event.
     *
     * @param r
     * @param rps
     */
    private void setVisibleRps(Rhythm r, List<RhythmParameter<?>> rps)
    {
        checkNotNull(r);
        checkNotNull(rps);

        LOGGER.log(Level.FINE, "setVisibleRps() rps={0}", rps);

        var newRpsSorted = sortRhythmParameters(r, rps);
        if (newRpsSorted.isEmpty())
        {
            throw new IllegalArgumentException("r=" + r + " rps=" + rps + " newRpsSorted=" + newRpsSorted);
        }
        var oldRps = mapRhythmVisibleRps.get(r);
        if (newRpsSorted.equals(oldRps))
        {
            return;
        }

        // Store the rps
        mapRhythmVisibleRps.put(r, newRpsSorted);


        // Update UI
        for (SptViewer sptv : getSptViewers())
        {
            if (sptv.getModel().getRhythm() == r)
            {
                sptv.setConfig(sptv.getConfig().setVisibleRPs(newRpsSorted));
            }
        }

        // Fire event
        firePropertyChange(PROP_RHYHM_VISIBLE_RPS, r, newRpsSorted);
    }

    /**
     * Process each rhythm to make sure the default visible RPs in compact view mode are set.
     *
     * @param rhythms
     */
    private void storeVisibleRPsInCompactModeIfRequired(List<Rhythm> rhythms)
    {
        for (Rhythm r : rhythms)
        {
            if (getCompactViewModeVisibleRPs(songModel, r).isEmpty())
            {
                // Rhythm is new, need to set its visible RPs in compact mode
                var rps = getDefaultVisibleRpsInCompactMode(r);
                setCompactViewModeVisibleRPs(songModel, r, rps);
            }
        }
    }

    /**
     * Get the RhythmParameters visible by default in compact mode for the specified new rhythm.
     * <p>
     * Use the primary Rhythm Parameters, and others only if there are actually used in the song.
     *
     * @param r
     * @return A non-empty list
     */
    private List<RhythmParameter<?>> getDefaultVisibleRpsInCompactMode(Rhythm r)
    {
        var spts = sgsModel.getSongParts();
        var rps = r.getRhythmParameters();
        var tmp = new ArrayList<RhythmParameter<?>>();


        // Add primary RPs by default
        rps.stream()
                .filter(rp -> rp.isPrimary())
                .forEach(tmp::add);


        // Add non-primary only if used in the song
        rps.stream()
                .filter(rp -> !rp.isPrimary())
                .filter(rp -> 
                {
                    return spts.stream()
                            .filter(spt -> spt.getRhythm() == r)
                            .anyMatch(spt -> !rp.getDefaultValue().equals(spt.getRPValue(rp)));
                })
                .forEach(tmp::add);


        // Reorder
        var res = new ArrayList<RhythmParameter<?>>();
        for (var rp : r.getRhythmParameters())
        {
            if (tmp.contains(rp))
            {
                res.add(rp);
            }
        }

        if (res.isEmpty())
        {
            LOGGER.log(Level.WARNING, "getDefaultVisibleRpsInCompactMode() no default compact-mode visible RPs for r={0}, using 1st RP as default", r);
            res.add(r.getRhythmParameters().get(0));
        }

        return res;
    }

    /**
     * Get the first RhythmParameter from rps which is assignable from rpClass (same class or rpClass is a superclass).
     *
     * @param rps
     * @param rpClass
     * @return
     */
    private RhythmParameter<?> getRpFromClass(List<RhythmParameter<?>> rps, Class<?> rpClass)
    {
        return rps.stream().filter(rp -> rpClass.isAssignableFrom(rp.getClass())).findAny().orElse(null);


    }


    /**
     * Compute the X position in the viewport coordinates (panel_Low) from a song position.
     *
     * @param pos
     * @return
     */
    private int computeXpos(Position pos)
    {
        var sgs = songModel.getSongStructure();
        SongPart spt = sgs.getSongPart(pos.getBar());
        if (spt == null)
        {
            return 0;
        }

        SptViewer sptv = getSptViewerTop(spt);
        var in = sptv.getInsets();
        int sptvWidth = sptv.getWidth() - in.left - in.right - 2;
        int xStart = sptv.getX() + in.left + 1;
        var br = sgs.toBeatRange(spt.getBarRange());
        var beatPos = sgs.toPositionInNaturalBeats(pos);
        var relBeatPos = Math.max(0, beatPos - br.from);
        int x = Math.round(xStart + sptvWidth * relBeatPos / br.size());

        return x;
    }


    //===========================================================================
    // Private classes
    //===========================================================================    
    private class SS_EditorZoomable implements Zoomable
    {

        int yFactor = 50;
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        @Override
        public Zoomable.Capabilities getZoomCapabilities()
        {
            return Capabilities.X_Y;
        }

        @Override
        public int getZoomYFactor()
        {
            return SS_EditorClientProperties.getZoomYFactor(songModel);
        }

        @Override
        public void setZoomYFactor(int factor, boolean valueIsAdjusting)
        {
            int oldFactor = getZoomYFactor();
            SS_EditorClientProperties.setZoomYFactor(songModel, factor);
            pcs.firePropertyChange(Zoomable.PROPERTY_ZOOM_Y, oldFactor, factor);
        }

        @Override
        public int getZoomXFactor()
        {
            return SS_EditorClientProperties.getZoomXFactor(songModel);
        }

        @Override
        public void setZoomXFactor(int factor, boolean valueIsAdjusting)
        {
            int oldFactor = getZoomXFactor();
            SS_EditorClientProperties.setZoomXFactor(songModel, factor);
            pcs.firePropertyChange(Zoomable.PROPERTY_ZOOM_X, oldFactor, factor);
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

    /**
     * Make sure show the insertionPoint is turned off when dropLocation is out of the bounds of the target drop component (this editor or a SptViewer)
     */
    private class DTListener implements DropTargetListener
    {

        @Override
        public void dragEnter(DropTargetDragEvent dtde)
        {
            // Nothing
        }

        @Override
        public void dragOver(DropTargetDragEvent dtde)
        {
            // Nothing
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent dtde)
        {
            // Nothing
        }

        @Override
        public void dragExit(DropTargetEvent dte)
        {
            showSptInsertionMark(false, 0, true);
        }

        @Override
        public void drop(DropTargetDropEvent dtde)
        {
            // Nothing
        }
    }


    /**
     * Panel to hold SptViewers.
     * <p>
     */
    private static class SptViewerPanel extends JPanel implements Scrollable
    {

        public SptViewerPanel()
        {
            var flowLayout = new FlowLayout(FlowLayout.LEFT, 2, 0);
            flowLayout.setAlignOnBaseline(true);        // Used to get the songparts aligned on the top line.
            setLayout(flowLayout);
            setOpaque(false);
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
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction
        )
        {
            int unit;

            if (orientation == SwingConstants.VERTICAL)
            {
                unit = 30;
            } else
            {
                unit = 40;
            }

            return unit;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return getScrollableUnitIncrement(visibleRect, orientation, direction);
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return false;
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return false;
        }
    };


}
