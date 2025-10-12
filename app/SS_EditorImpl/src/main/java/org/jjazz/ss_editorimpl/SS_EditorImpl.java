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

import static com.google.common.base.Preconditions.checkNotNull;
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
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.RpValueChangedEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptRenamedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.songstructure.api.event.SptResizedEvent;
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
import org.jjazz.uiutilities.api.UIUtilities;
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

/**
 * An implementation of the SongStructure editor.
 */
public class SS_EditorImpl extends SS_Editor implements PropertyChangeListener, SgsChangeListener, MouseListener, MouseWheelListener
{


    // UI variables
    private javax.swing.JPanel panel_SongParts;
    private InsertionSptMark insertionMark;
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
    private JJazzUndoManager undoManager;
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
    private InstanceContent generalLookupContent;
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
     * Save the last Spt highlighted during song playback.
     */
    private SongPart lastPlaybackSpt;
    /**
     * Store the visible RPs for each rhythm.
     */
    private Map<Rhythm, List<RhythmParameter<?>>> mapRhythmVisibleRps;
    /**
     * Editor settings.
     */
    private SS_EditorSettings settings;
    private SptViewerFactory sptViewerFactory;
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


        // Listen to settings changes
        this.settings.addPropertyChangeListener(this);

        // The lookup for selection
        selectionLookupContent = new InstanceContent();
        selectionLookup = new AbstractLookup(selectionLookupContent);
        selectionLastContent = new ArrayList<>();

        // The lookup for other stuff
        generalLookupContent = new InstanceContent();
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
        updateUIComponents();

        panel_SongParts.addMouseListener(this);       // for editor popup menu
        addMouseWheelListener(this);                    // For zoom operations

        // Listen to our models
        sgsModel.addSgsChangeListener(this);

        // Connect our undomanager to our model
        undoManager = JJazzUndoManagerFinder.getDefault().get(sgsModel);
        assert undoManager != null : "model=" + sgsModel;
        sgsModel.addUndoableEditListener(undoManager);

        // Fill our lookup
        generalLookupContent.add(sgsModel);
        generalLookupContent.add(songModel);


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
        updateSptsVisibleRhythmAndTimeSignature();
        updateSptMultiSelectMode();
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

        settings.removePropertyChangeListener(this);

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

        // We're not showing playback anymore
        lastPlaybackSpt = null;
    }


    @Override
    public void selectSongPart(SongPart spt, boolean b)
    {
        assert spt != null;
        if (isSelected(spt) == b)
        {
            return;
        }
        SptViewer rpe = getSptViewer(spt);
        assert rpe != null;
        rpe.setSelected(b);
        if (b)
        {
            // Warning ! If item is mutable, make sure item uses Object's equals() and hashCode() !
            selectionLookupContent.add(spt);
            selectionLastContent.add(spt);
        } else
        {
            // Warning ! Might not work if item was mutated with equals()/hashCode() defined !
            selectionLookupContent.remove(spt);
            selectionLastContent.remove(spt);
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
        SptViewer spte = this.getSptViewer(spt);
        spte.setSelected(rp, b);
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
        SptViewer rpe = getSptViewer(spt);
        rpe.requestFocusInWindow();
    }

    @Override
    public void setFocusOnRhythmParameter(SongPart spt, RhythmParameter<?> rp)
    {
        assert spt != null;
        SptViewer rpe = getSptViewer(spt);
        rpe.setFocusOnRpViewer(rp);
    }

    @Override
    public Rectangle getSptViewerRectangle(SongPart spt)
    {
        if (!sgsModel.getSongParts().contains(spt))
        {
            throw new IllegalArgumentException("spt=" + spt + " model=" + sgsModel);
        }

        SptViewer sptv = getSptViewer(spt);
        Point p = sptv.getLocationOnScreen();
        Rectangle r = new Rectangle(p);
        r.width = sptv.getWidth();
        r.height = sptv.getHeight();
        return r;
    }

    @Override
    public Rectangle getRpViewerRectangle(SongPart spt, RhythmParameter<?> rp)
    {
        SptViewer sptv = getSptViewer(spt);
        return sptv.getRpViewerRectangle(rp);
    }

    @Override
    public SongPartParameter getSongPartParameterFromPoint(Point editorPoint, AtomicBoolean sptLeft)
    {
        SongPart spt = null;
        RhythmParameter<?> rp = null;

        // Find the component within this editor which is under this editorPoint
        Component c = SwingUtilities.getDeepestComponentAt(this, editorPoint.x, editorPoint.y);

        // Simple case: check if we're on known components. Recurse the hiearchy
        // Assume RpViewer bounds are WITHIN SptViewer bounds
        while (c != null)
        {
            if (c instanceof RpViewer rpViewer)
            {
                rp = rpViewer.getRpModel();
                spt = rpViewer.getSptModel();
                break;
            }
            if (c instanceof SptViewer sptViewer)
            {
                spt = sptViewer.getModel();
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
            for (SptViewer sptv : getSptViewers())
            {
                sptv.getBounds(r);
                SwingUtilities.convertRectangle(panel_SongParts, r, this);
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
            panel_SongParts.remove(insertionMark);
            insertionMarkSptIndex = -1;
            panel_SongParts.revalidate();
            panel_SongParts.repaint(); // Needed to erase the insertionMark if it was at last index position
            return;
        }
        if (insertionMarkSptIndex == -1)
        {
            // First display, adjust size
            insertionMark.setPreferredSize(new Dimension(insertionMark.getPreferredSize().width, 50));
        }
        insertionMark.setCopyMode(copyMode);
        if (insertionMarkSptIndex != sptIndex)
        {
            panel_SongParts.remove(insertionMark);
            insertionMarkSptIndex = sptIndex;
            panel_SongParts.add(insertionMark, insertionMarkSptIndex);
            panel_SongParts.revalidate();
            panel_SongParts.repaint(); // If added in last position
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

        List<RhythmParameter<?>> newRps = new ArrayList<>();
        newRps.addAll(rps);
        return newRps;
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
        if (!sgsModel.getSongParts().contains(lastPlaybackSpt))
        {
            // Special case: last playback Spt was removed (e.g. user edit during song playback)
            lastPlaybackSpt = null;
        }
        SongPart newSpt = (pos != null) ? sgsModel.getSongPart(pos.getBar()) : null;
        if (lastPlaybackSpt != null)
        {
            // Playback point was already shown
            SptViewer lastSptv = getSptViewer(lastPlaybackSpt);
            if (!show)
            {
                lastSptv.showPlaybackPoint(false, null);
                lastPlaybackSpt = null;
            } else if (newSpt != null && newSpt != lastPlaybackSpt)
            {
                // Playback point changed SptViewer, switch off old location and switch on new location
                lastSptv.showPlaybackPoint(false, null);
                SptViewer newSptv = getSptViewer(newSpt);
                newSptv.showPlaybackPoint(true, pos);
                lastPlaybackSpt = newSpt;
                makeSptViewerVisible(newSpt);
            }
        } else if (show && newSpt != null)
        {
            // First show of playback point
            SptViewer sptv = getSptViewer(newSpt); // Can be null if editor was cleaned up.
            if (sptv != null)
            {
                sptv.showPlaybackPoint(true, pos);
                lastPlaybackSpt = newSpt;
                makeSptViewerVisible(newSpt);
            }
        }
    }

    @Override
    public void makeSptViewerVisible(SongPart spt)
    {
        SptViewer sptv = getSptViewer(spt);
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
        // Model changes can be generated outside the EDT
        Runnable run = () -> 
        {
            if (evt.getSource() == settings)
            {
                updateUIComponents();

            } else if (evt.getSource() == songModel.getClientProperties())
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
        };
        UIUtilities.invokeLaterIfNeeded(run);
    }

    //------------------------------------------------------------------------------
    // SgsChangeListener interface
    //------------------------------------------------------------------------------   
    @Override
    public void songStructureChanged(final SgsChangeEvent e)
    {
        // Model changes can be generated outside the EDT
        Runnable run = () -> 
        {
            LOGGER.log(Level.FINE, "SS_EditorImpl.songStructureChanged() -- e={0} spts={1}", new Object[]
            {
                e, e.getSongParts()
            });

            if (e instanceof SptRemovedEvent)
            {
                for (SongPart spt : e.getSongParts())
                {
                    SptViewer rpe = getSptViewer(spt);
                    if (rpe != null)
                    {
                        removeSptViewer(rpe);
                    }
                }
                panel_SongParts.revalidate();
                panel_SongParts.repaint();     // Needed if removed Spt was the last one
                updateSptsVisibleRhythmAndTimeSignature();
                updateSptMultiSelectMode();
            } else if (e instanceof SptAddedEvent)
            {
                for (SongPart spt : e.getSongParts())
                {
                    addSptViewer(spt);
                }
                panel_SongParts.revalidate();  // Needed to get immediate UI update
                updateSptsVisibleRhythmAndTimeSignature();
                updateSptMultiSelectMode();
                storeVisibleRPsInCompactModeIfRequired(e.getSongParts().stream()
                        .map(spt -> spt.getRhythm())
                        .toList());

            } else if (e instanceof SptReplacedEvent re)
            {
                List<SongPart> oldSpts = re.getSongParts();
                List<SongPart> newSpts = re.getNewSpts();
                LOGGER.log(Level.FINE, "SS_EditorImpl.songStructureChanged() SptReplacedEvent  newSpts={0}", newSpts);


                storeVisibleRPsInCompactModeIfRequired(newSpts.stream()
                        .map(spt -> spt.getRhythm())
                        .toList());


                // Save selection so we can restore it the best we can after replacing
                SS_Selection previousSelection = new SS_Selection(selectionLookup);

                // Update the viewers
                for (int i = 0; i < oldSpts.size(); i++)
                {
                    SongPart oldSpt = oldSpts.get(i);
                    SongPart newSpt = newSpts.get(i);
                    removeSptViewer(getSptViewer(oldSpt));
                    addSptViewer(newSpt);
                }

                // Restore the selection
                if (previousSelection.isSongPartSelected())
                {
                    // Reselect a new songpart if the corresponding old songpart was selected
                    for (SongPart oldSpt : previousSelection.getSelectedSongParts())
                    {
                        int index = oldSpts.indexOf(oldSpt);
                        if (index != -1)
                        {
                            // oldSpt was prevously selected AND replaced
                            SongPart newSpt = newSpts.get(index);
                            selectSongPart(newSpt, true);
                        } else
                        {
                            // oldSpt was previously selected but NOT replaced, do nothing
                        }
                    }
                } else if (previousSelection.isRhythmParameterSelected())
                {
                    RhythmParameter<?> rp = previousSelection.getSelectedSongPartParameter(oldSpts.get(0));
                    if (rp != null)
                    {
                        // Try to reselect a rp in the replacing SongParts
                        for (SongPart newSpt : newSpts)
                        {
                            List<RhythmParameter<?>> newRps = newSpt.getRhythm().getRhythmParameters();
                            assert !newRps.isEmpty() : "no RhythmParameters ! newSpt=" + newSpt;
                            RhythmParameter<?> newRp = RhythmParameter.findFirstCompatibleRp(newRps, rp);
                            if (newRp != null)
                            {
                                selectRhythmParameter(newSpt, newRp, true);
                            } else
                            {
                                // Select the first RP by default
                                selectRhythmParameter(newSpt, newRps.get(0), true);
                            }
                        }
                    }
                }

                panel_SongParts.revalidate();     // Needed to get immediate UI update
                panel_SongParts.repaint();        // Needed to avoid glitch if replaced viewer is smaller
                updateSptsVisibleRhythmAndTimeSignature();
                updateSptMultiSelectMode();
            } else if (e instanceof SptResizedEvent)
            {
                // Nothing, directly managed by SptViewers
            } else if (e instanceof SptRenamedEvent)
            {
                // Update the MultiSelectBar on/off state on each SptViewer
                updateSptMultiSelectMode();
            } else if (e instanceof RpValueChangedEvent)
            {
                // Nothing, directly managed by SptViewers
            }
        };
        UIUtilities.invokeLaterIfNeeded(run);
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
        if (controller == null)
        {
            return;
        }
        Component c = (Component) e.getSource();
        LOGGER.log(Level.FINE, "mousePressed() c={0}", c);
        if (c == panel_SongParts)
        {
            controller.editorClicked(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

// ---------------------------------------------------------------
// Implements MouseWheelListener interface
// ---------------------------------------------------------------
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if (controller == null)
        {
            return;
        }
        Component c = (Component) e.getSource();
        if (c == this)
        {
            controller.editorWheelMoved(e);
        }
    }
    //------------------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------------------      

    private void initUIComponents()
    {
        insertionMark = new InsertionSptMark();

//        panel_Top = new JPanel();
//        panel_Top.setPreferredSize(new Dimension(0, 25));
//        panel_Top.setOpaque(true);
//        panel_Top.setBackground(settings.getTopBackgroundColor());
//        panel_Top.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel_SongParts = new JPanel()
        {
            // Leave a small space at the right of the last SongPart
            @Override
            public Dimension getPreferredSize()
            {
                final int EXTRA = 50;
                Dimension pd = getLayout().preferredLayoutSize(this);
                return new Dimension(pd.width + EXTRA, pd.height);
            }
        };
        java.awt.FlowLayout flowLayout = new java.awt.FlowLayout(FlowLayout.LEFT, 1, 5);
        flowLayout.setAlignOnBaseline(true); // Used to get the songparts aligned on the top line.
        panel_SongParts.setOpaque(false);
        panel_SongParts.setLayout(flowLayout);
        panel_SongParts.setMinimumSize(new java.awt.Dimension(800, 50));

        BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(boxLayout); // So that panel_SongParts uses all the available space        
        // add(panel_Top);
        add(panel_SongParts);
    }

    private void updateUIComponents()
    {
        // setBackground(settings.getBackgroundColor());
        setOpaque(false);       // To reuse LAF default background
    }


    private List<SptViewer> getSptViewers()
    {
        ArrayList<SptViewer> res = new ArrayList<>();
        for (Component c : this.panel_SongParts.getComponents())
        {
            if (c instanceof SptViewer sptViewer)
            {
                res.add(sptViewer);
            }
        }
        return res;
    }

    private void addSptViewer(SongPart spt)
    {
        assert spt != null;
        SptViewer sptv = sptViewerFactory.createSptViewer(songModel, spt, sptViewerFactory.getDefaultSptViewerSettings(),
                sptViewerFactory.getDefaultRpViewerFactory());
        registerSptViewer(sptv);
        sptv.setZoomHFactor(getZoomXFactor(songModel));
        sptv.setZoomVFactor(getZoomYFactor(songModel));
        List<RhythmParameter<?>> rps = this.getVisibleRps(spt.getRhythm());
        sptv.setVisibleRps(rps);
        int index = sgsModel.getSongParts().indexOf(spt);
        assert index >= 0 : "spt=" + spt + " model.getSongParts()=" + sgsModel.getSongParts();
        LOGGER.log(Level.FINE, "addSptViewer() spt={0} +index={1} panel_SongParts.size={2}", new Object[]
        {
            spt, index,
            panel_SongParts.
            getComponentCount()
        });
        panel_SongParts.add(sptv, index);
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
        panel_SongParts.remove(sptv);
        sptv.cleanup();
    }

    private SptViewer getSptViewer(SongPart spt)
    {
        for (SptViewer rpe : this.getSptViewers())
        {
            if (rpe.getModel() == spt)
            {
                return rpe;
            }
        }
        return null;
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
        // Here we listen to dropTarget events, if it exists the SptViewier's bound, make sure
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
        sptv.setController(null);
    }

    private boolean isSelected(SongPart spt)
    {
        return selectionLastContent.contains(spt);
    }

    private boolean isSelected(SongPart spt, RhythmParameter<?> rp)
    {
        return selectionLastContent.contains(new SongPartParameter(spt, rp));
    }

    /**
     * Show/Hide the rhythm and the time signature of all SongParts.
     */
    private void updateSptsVisibleRhythmAndTimeSignature()
    {
        Rhythm lastRhythm = null;
        for (SptViewer sptv : getSptViewers())
        {
            Rhythm rhythm = sptv.getModel().getRhythm();
            TimeSignature ts = rhythm.getTimeSignature();
            sptv.setRhythmVisible(!rhythm.equals(lastRhythm));
            sptv.setTimeSignatureVisible(lastRhythm == null ? true : !ts.equals(lastRhythm.getTimeSignature()));
            lastRhythm = rhythm;
        }
    }

    /**
     * Update the MultiSelectMode visibility of each spt.
     */
    private void updateSptMultiSelectMode()
    {
        ArrayList<SptViewer> buffer = new ArrayList<>();
        String prevName = null;
        for (SptViewer sptv : getSptViewers())
        {
            String name = sptv.getModel().getName();
            if (prevName != null && !name.equals(prevName))
            {
                // Names differ, flush buffer
                buffer.get(0).setNameVisible(true);
                buffer.get(0).setMultiSelectMode(buffer.size() > 1, true);
                for (int i = 1; i < buffer.size(); i++)
                {
                    buffer.get(i).setNameVisible(false);
                    buffer.get(i).setMultiSelectMode(true, false);
                }
                buffer.clear();
            }

            buffer.add(sptv);
            prevName = name;
        }
        // Flush buffer if required
        if (!buffer.isEmpty())
        {
            buffer.get(0).setNameVisible(true);
            buffer.get(0).setMultiSelectMode(buffer.size() > 1, true);
            for (int i = 1; i < buffer.size(); i++)
            {
                buffer.get(i).setNameVisible(false);
                buffer.get(i).setMultiSelectMode(true, false);
            }
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
                sptv.setVisibleRps(newRpsSorted);
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


    //===========================================================================
    // Private classes
    //===========================================================================    
    private class SS_EditorZoomable implements Zoomable
    {

        int yFactor = 50;
        private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

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
}
