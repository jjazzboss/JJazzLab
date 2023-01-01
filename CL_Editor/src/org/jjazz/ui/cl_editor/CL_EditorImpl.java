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
package org.jjazz.ui.cl_editor;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.base.api.actions.Savable;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.event.*;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.ui.cl_editor.barbox.api.BarBox;
import org.jjazz.ui.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererFactory;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_EditorMouseListener;
import org.jjazz.ui.cl_editor.spi.CL_EditorSettings;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;
import org.jjazz.ui.utilities.api.Zoomable;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.savablesong.api.SavableSong;
import org.jjazz.savablesong.api.SaveAsCapableSong;
import org.jjazz.song.api.Song;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.rhythm.api.Feel;
import org.jjazz.ui.cl_editor.api.SelectedBar;
import org.openide.awt.UndoRedo;
import org.openide.util.NbBundle.Messages;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.cl_editor.barrenderer.spi.BarRendererProvider;

/**
 * A chordleadsheet editor using BarBox objects to render bars.
 */
@Messages(
        {
            "CTL_SaveCancelled=Save cancelled by user"
        })
public class CL_EditorImpl extends CL_Editor implements PropertyChangeListener, ClsChangeListener, Scrollable, MouseListener, MouseWheelListener, MouseMotionListener
{

    protected static final String PROP_ZOOM_FACTOR_X = "PropClEditorZoomFactorX";
    protected static final String PROP_ZOOM_FACTOR_Y = "PropClEditorZoomFactorY";

    private static final int NB_EXTRA_LINES = 4;

    /**
     * The default BarRenderer types.
     */
    private final String[] DEFAULT_BAR_RENDERER_TYPES =
    {
        BarRendererFactory.BR_CHORD_SYMBOL,
        BarRendererFactory.BR_CHORD_POSITION,
        BarRendererFactory.BR_SECTION
    };


    // GUI variables
    /**
     * Our LayoutManager.
     */
    private GridLayout gridLayout;
    /**
     * Keep a list of BarBox components.
     */
    private final List<BarBox> barBoxes = new ArrayList<>();
    /**
     * Our graphical settings.
     */
    private CL_EditorSettings settings;
    // APPLICATION variables
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
     * Store the selected items or bars.
     */
    private InstanceContent selectionLookupContent;
    /**
     * Last snapshot of objects in selectionLookupContent: assume it's faster to check than checking the lookup (?)
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
     * The leadsheet clsModel.
     */
    private ChordLeadSheet clsModel;
    /**
     * An optional container for this ChordLeadSheet.
     */
    private Song songModel;
    /**
     * The number of columns.
     */
    private int nbColumns;
    private int zoomVFactor;
    /**
     * The last position insertion point.
     */
    private Position insertionPointLastPos;
    /**
     * The last playback point.
     */
    private Position playbackPointLastPos;
    private BarRendererFactory barRendererFactory;
    /**
     * Receiver for mouse events.
     */
    private CL_EditorMouseListener editorMouseListener;
    /**
     * Our Drag and Drop handler.
     */
    private CL_EditorTransferHandler transferHandler;
    /**
     * Store the last Quantization used for each time signature.
     */
    private final HashMap<TimeSignature, Quantization> mapTsQuantization = new HashMap<>();
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(CL_EditorImpl.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public CL_EditorImpl(Song song, CL_EditorSettings settings, BarRendererFactory brf)
    {
        if (song == null || settings == null || brf == null)
        {
            throw new IllegalArgumentException("song=" + song + " settings=" + settings + " brf=" + brf);   //NOI18N
        }
        songModel = song;
        this.barRendererFactory = brf;

        // Listen to settings changes
        this.settings = settings;
        this.settings.addPropertyChangeListener(this);

        // Graphical stuff
        nbColumns = 4;
        zoomVFactor = 50;
        gridLayout = new GridLayout(0, nbColumns);   // Nb of lines adjusted to number of bars
        setLayout(gridLayout);
        setBackground(settings.getBackgroundColor());

        // The lookup for selection
        selectionLookupContent = new InstanceContent();
        selectionLookup = new AbstractLookup(selectionLookupContent);
        selectionLastContent = new ArrayList<>();

        // The lookup for other stuff
        generalLookupContent = new InstanceContent();
        generalLookup = new AbstractLookup(generalLookupContent);

        // Initialize with actionmap and our Zoomable object
        generalLookupContent.add(new CL_EditorZoomable());
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
        songModel.addPropertyChangeListener(this);
        clsModel = songModel.getChordLeadSheet();
        clsModel.addClsChangeListener(this);

        // Connect our undoManager to our model
        undoManager = JJazzUndoManagerFinder.getDefault().get(clsModel);

        if (undoManager != null)
        {
            clsModel.addUndoableEditListener(undoManager);
        }

        // Fill our lookup
        generalLookupContent.add(new SaveAsCapableSong(songModel)); // always enabled
        generalLookupContent.add(clsModel);
        generalLookupContent.add(songModel);


        if (songModel.needSave())
        {
            setSongModified();
        }


        // Add or remove barboxes at the end if required
        int newSizeInBars = computeNbBarBoxes(NB_EXTRA_LINES);
        setNbBarBoxes(newSizeInBars);  // This will update our songModel via getDisplayQuantizationValue() then Song.putClientProperty()


        // Used to zoom in / zoom out
        addMouseWheelListener(this);


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
        CL_SelectionUtilities selection = new CL_SelectionUtilities(selectionLookup);
        selection.unselectAll(this);

        // Unregister the objects we were listening to
        clsModel.removeClsChangeListener(this);
        if (undoManager != null)
        {
            clsModel.removeUndoableEditListener(undoManager);
        }
        generalLookupContent.remove(clsModel);
        songModel.removePropertyChangeListener(this);
        SaveAsCapableSong saveAsCapableSong = lookup.lookup(SaveAsCapableSong.class);
        if (saveAsCapableSong != null)
        {
            generalLookupContent.remove(saveAsCapableSong);
        }
        resetSongModified();
        generalLookupContent.remove(songModel);
        settings.removePropertyChangeListener(this);

        // Need to remove backwards to avoid consistency problems 
        for (int i = barBoxes.size() - 1; i >= 0; i--)
        {
            removeBarBox(barBoxes.get(i));
        }
        for (Component c : getComponents())
        {
            if (c instanceof PaddingBox)
            {
                removePaddingBox((PaddingBox) c);
            }
        }

        // We're not showing playback or insertion point anymore
        playbackPointLastPos = null;
        insertionPointLastPos = null;
    }

    @Override
    public void setDisplayQuantizationValue(CLI_Section cliSection, Quantization q)
    {
        int sectionBar = cliSection.getPosition().getBar();
        if (clsModel.getSection(sectionBar) != cliSection)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " not found in clsModel=" + clsModel);   //NOI18N
        }
        storeSectionQValue(cliSection.getData(), q);
        propagateSectionChange(cliSection);
        firePropertyChange(getSectionQuantizeValuePropertyName(cliSection.getData()), cliSection, q);
    }

    @Override
    public Quantization getDisplayQuantizationValue(CLI_Section cliSection)
    {
        int sectionBar = cliSection.getPosition().getBar();
        if (clsModel.getSection(sectionBar) != cliSection)
        {
            throw new IllegalArgumentException("section=" + cliSection + " not found in clsModel=" + clsModel);   //NOI18N
        }


        Quantization q = getSectionQValue(cliSection.getData());
        if (q == null)
        {
            // Nothing defined yet

            // Try to get the quantization from the 1st associated rhythm (if we can find it)
            SongStructure sgs = this.songModel.getSongStructure();
            for (SongPart spt : sgs.getSongParts())
            {
                if (spt.getParentSection().equals(cliSection))
                {
                    q = (spt.getRhythm().getFeatures().getFeel() == Feel.BINARY) ? Quantization.ONE_QUARTER_BEAT : Quantization.ONE_THIRD_BEAT;
                    break;
                }
            }

            if (q == null)
            {
                // Nothing found using the rhythm, choose the last Quantization used for this TimeSignature
                TimeSignature ts = cliSection.getData().getTimeSignature();
                q = mapTsQuantization.get(ts);
                if (q == null)
                {
                    // TimeSignature was never used, use default
                    q = Quantizer.getInstance().getDefaultQuantizationValue(ts);
                }
            }


            // Now that we got something, save it
            storeSectionQValue(cliSection.getData(), q);
        }
        return q;
    }

    @Override
    public SelectedBar getFocusedBar(boolean includeFocusedItem)
    {
        SelectedBar sb = null;
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof BarBox)
        {
            sb = new SelectedBar(((BarBox) c).getBarIndex(), this.clsModel);
        } else if (includeFocusedItem && (c instanceof ItemRenderer))
        {
            sb = new SelectedBar(((ItemRenderer) c).getModel().getPosition().getBar(), this.clsModel);
        }
        return sb;
    }

    @Override
    public void setSectionStartOnNewLine(CLI_Section cliSection, boolean b)
    {
        int barIndex = cliSection.getPosition().getBar();
        if (isSectionStartOnNewLine(cliSection) == b || barIndex == 0)
        {
            return;
        }
        songModel.putClientProperty(getSectionStartOnNewLinePropertyName(cliSection.getData()), b ? Boolean.toString(true) : null);
        updatePaddingBoxes();
    }

    @Override
    public boolean isSectionStartOnNewLine(CLI_Section cliSection)
    {
        String boolString = songModel.getClientProperty(getSectionStartOnNewLinePropertyName(cliSection.getData()), "false");
        boolean b = Boolean.valueOf(boolString);
        return b;
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
        if (nbCols < 1 || nbCols > 16)
        {
            throw new IllegalArgumentException("nbCols=" + nbCols);   //NOI18N
        }
        if (nbCols == nbColumns)
        {
            return;
        }
        int oldValue = nbColumns;
        nbColumns = nbCols;
        gridLayout.setColumns(nbColumns);
        updatePaddingBoxes();
        revalidate();
        int oldFactor = computeNbColsToXFactor(oldValue);
        int newFactor = computeNbColsToXFactor(nbColumns);
        LOGGER.log(Level.FINER, "oldFactor={0} newFactor={1}", new Object[]   //NOI18N
        {
            oldFactor, newFactor
        });
        pcs.firePropertyChange(Zoomable.PROPERTY_ZOOM_X, oldFactor, newFactor);

        // Save the zoom factor with the song as a client property
        songModel.putClientProperty(PROP_ZOOM_FACTOR_X, Integer.toString(newFactor));

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

    @Override
    public void setZoomVFactor(int factor)
    {
        if (factor < 0 || factor > 100)
        {
            throw new IllegalArgumentException("factor=" + factor);   //NOI18N
        }
        zoomVFactor = factor;
        for (BarBox bb : getBarBoxes())
        {
            bb.setZoomVFactor(zoomVFactor);
        }

        // Save the zoom factor with the song as a client property
        songModel.putClientProperty(PROP_ZOOM_FACTOR_Y, Integer.toString(zoomVFactor));

    }

    @Override
    public int getZoomVFactor()
    {
        return zoomVFactor;
    }

    /**
     * Return the position (bar, beat) which corresponds to a given point in the editor. If point is in the BarBox which does not
     * have a valid modelBar (eg after the end), barIndex is set but beat is set to 0.
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
        return (bbPos != null) ? bbPos : new Position(bb.getBarIndex(), 0);
    }

    @Override
    public void makeBarVisible(int barIndex)
    {
        if (barIndex < 0 || barIndex >= getNbBarBoxes())
        {
            throw new IllegalArgumentException("barIndex=" + barIndex + " getNbBars()=" + getNbBarBoxes());   //NOI18N
        }


        // Check how many rows are visible
        float nbVisibleRows = (float) getVisibleRect().height / getBarBox(0).getHeight();
        int row = getRowIndex(barIndex);


        if (nbVisibleRows < 1.8f || row == getNbRows() - 1 || row == 0)
        {
            // Can't see clearly 2 rows, or it's the last row
            // Make sure row is visible
            scrollRectToVisible(getBarBox(barIndex).getBounds());
        } else
        {
            // Make sure row+1 is visible, because it's better to always have next row also visible
            int compIndex = getComponentIndex(barIndex);
            assert compIndex + getNbColumns() < getComponentCount() : "compIndex=" + compIndex + " getNbColumns()=" + getNbColumns() + " getComponentCount()=" + getComponentCount();   //NOI18N
            Component c = getComponent(compIndex + getNbColumns());
            scrollRectToVisible(c.getBounds());
        }

    }

    @Override
    public Rectangle getBarRectangle(int barIndex)
    {
        if (barIndex < 0 || barIndex >= getNbBarBoxes())
        {
            throw new IllegalArgumentException("barIndex=" + barIndex);   //NOI18N
        }
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
        if (bbIndexFrom < 0 || bbIndexTo >= getNbBarBoxes() || bbIndexFrom > bbIndexTo)
        {
            throw new IllegalArgumentException("bbIndexFrom=" + bbIndexFrom + " bbIndexTo=" + bbIndexTo + " getNbBarBoxes()=" + getNbBarBoxes());   //NOI18N
        }
        LOGGER.log(Level.FINE, "Before selectBar() b={0} bbIndexFrom={1} selectionLookup={2}", new Object[]   //NOI18N
        {
            b, bbIndexFrom, selectionLookup
        });
        Collection<? extends Object> result = selectionLookup.lookupAll(Object.class);
        selectionLastContent.clear();
        selectionLastContent.addAll(result);
        for (int i = bbIndexFrom; i <= bbIndexTo; i++)
        {
            SelectedBar sb = new SelectedBar(i, clsModel);
            if (b != selectionLastContent.contains(sb))
            {
                BarBox bb = getBarBox(i);
                bb.setSelected(b);
                if (b)
                {
                    selectionLastContent.add(sb);
                } else
                {
                    selectionLastContent.remove(sb);
                }
            }
        }
        // Finally update our lookup in one shot
        selectionLookupContent.set(selectionLastContent, null);

        // repaint() here is useless functionnally, but VERY IMPORTANT for painting optimization, greatly speed up things
        // see why here : https://forums.oracle.com/forums/thread.jspa?threadID=2330727&tstart=0        
        // not required but seems to perform better because it does a single repaint of the entire frame
        // instead of trying to skip repainting the borders
        repaint();
        LOGGER.log(Level.FINE, "After selectBar() b={0} bbIndexFrom={1} selectionLookup={2}", new Object[]   //NOI18N
        {
            b, bbIndexFrom, selectionLookup
        });
    }

    @Override
    public void selectBarsExcept(int bbIndexFrom, int bbIndexTo, boolean b)
    {
        Collection<? extends Object> result = selectionLookup.lookupAll(Object.class);
        selectionLastContent.clear();
        selectionLastContent.addAll(result);
        int barMax = Math.min(bbIndexFrom - 1, getNbBarBoxes() - 1);
        for (int i = 0; i <= barMax; i++)
        {
            SelectedBar sb = new SelectedBar(i, clsModel);
            if (b != selectionLastContent.contains(sb))
            {
                BarBox bb = getBarBox(i);
                bb.setSelected(b);
                if (b)
                {
                    selectionLastContent.add(sb);
                } else
                {
                    selectionLastContent.remove(sb);
                }
            }
        }
        int barMin = Math.max(bbIndexTo + 1, 0);
        for (int i = barMin; i < getNbBarBoxes(); i++)
        {
            SelectedBar sb = new SelectedBar(i, clsModel);
            if (b != selectionLastContent.contains(sb))
            {
                BarBox bb = getBarBox(i);
                bb.setSelected(b);
                if (b)
                {
                    selectionLastContent.add(sb);
                } else
                {
                    selectionLastContent.remove(sb);
                }
            }
        }
        // Finally update our lookup in one shot
        selectionLookupContent.set(selectionLastContent, null);

        // repaint() here is useless functionnally, but VERY IMPORTANT for painting optimization, greatly speed up things
        // see why here : https://forums.oracle.com/forums/thread.jspa?threadID=2330727&tstart=0        
        // not required but seems to perform better because it does a single repaint of the entire frame
        // instead of trying to skip repainting the borders
        repaint();
        LOGGER.log(Level.FINE, "After selectBarsExcept() b={0} bbIndexFrom={1} selectionLookup={2}", new Object[]   //NOI18N
        {
            b, bbIndexFrom, selectionLookup
        });
    }

    @Override
    public void selectItem(ChordLeadSheetItem<?> item, boolean b)
    {
        if (isSelected(item) == b)
        {
            return;
        }
        BarBox bb = getBarBox(item.getPosition().getBar());
        bb.selectItem(item, b);
        if (b)
        {
            // Warning ! If item is mutable, make sure item uses Object's equals() and hashCode() !
            selectionLookupContent.add(item);
            selectionLastContent.add(item);
        } else
        {
            // Warning ! Might not work if item was mutated with equals()/hashCode() defined !
            selectionLookupContent.remove(item);
            selectionLastContent.remove(item);
        }
        LOGGER.log(Level.FINE, "After selectItem() b={0} item={1} lkp={2}", new Object[]   //NOI18N
        {
            b, item, lookup
        });
    }

    @Override
    public void selectItems(List<? extends ChordLeadSheetItem<?>> items, boolean b)
    {
        Collection<? extends Object> result = selectionLookup.lookupAll(Object.class);
        selectionLastContent.clear();
        selectionLastContent.addAll(result);
        for (ChordLeadSheetItem<?> item : items)
        {
            if (b != selectionLastContent.contains(item))
            {
                int barIndex = item.getPosition().getBar();
                BarBox bb = getBarBox(barIndex);
                bb.selectItem(item, b);
                if (b)
                {
                    selectionLastContent.add(item);
                } else
                {
                    selectionLastContent.remove(item);
                }
            }
        }
        // repaint() here is useless functionnally, but VERY IMPORTANT for painting optimization, greatly speed up things
        // see why here : https://forums.oracle.com/forums/thread.jspa?threadID=2330727&tstart=0                
        repaint();
        // Finally update our lookup
        selectionLookupContent.set(selectionLastContent, null);
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

        if (barIndexes.length == 0)
        {
            barIndexes = IntStream.range(0, getNbBarBoxes())
                    .boxed()
                    .collect(Collectors.toList()).toArray(new Integer[0]);
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
        if (item == null || (show && pos == null))
        {
            throw new NullPointerException("show=" + show + " item=" + item + " pos=" + pos + " copyMode=" + copyMode);   //NOI18N
        }

        LOGGER.log(Level.FINER, "showInsertionPoint() b=" + show + " item=" + item + " pos=" + pos + " copyMode=" + copyMode);   //NOI18N

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
        LOGGER.log(Level.FINE, "showPlaybackPoint() show={0} pos={1}", new Object[]   //NOI18N
        {
            show, pos
        });
        if (show && (pos == null))
        {
            throw new IllegalArgumentException("show=" + show + " pos=" + pos);   //NOI18N
        }
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
        if (!clsModel.contains(item))
        {
            throw new IllegalArgumentException("item=" + item + " clsModel=" + clsModel);   //NOI18N
        }
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
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                if (evt.getSource() == settings)
                {
                    if (evt.getPropertyName().equals(CL_EditorSettings.PROP_BACKGROUND_COLOR))
                    {
                        setBackground(settings.getBackgroundColor());
                    }
                } else if (evt.getSource() == songModel)
                {
                    if (evt.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED_OR_RESET))
                    {
                        boolean b = (boolean) evt.getNewValue();
                        if (b)
                        {
                            setSongModified();
                        } else
                        {
                            resetSongModified();
                        }
                    }
                }
            }
        };
        org.jjazz.ui.utilities.api.Utilities.invokeLaterIfNeeded(run);
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
    public void mouseExited(MouseEvent e
    )
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        Component c = (Component) e.getSource();
        if (c instanceof ItemRenderer)
        {
            ItemRenderer ir = (ItemRenderer) c;
            editorMouseListener.itemClicked(e, ir.getModel(), ir.getIR_Type());
        } else if (c instanceof BarRenderer)
        {
            BarBox bb = (BarBox) c.getParent();
            editorMouseListener.barClicked(e, bb.getBarIndex());
        } else if (c instanceof BarBox)
        {
            BarBox bb = (BarBox) c;
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
        } else if (c instanceof BarBox)
        {
            BarBox bb = (BarBox) c;
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
        if (c instanceof ItemRenderer)
        {
            ItemRenderer ir = (ItemRenderer) c;
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
    public void authorizeChange(ClsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void chordLeadSheetChanged(final ClsChangeEvent event)
    {
        // Model changes can be generated outside the EDT
        Runnable run = () ->
        {

            // Save focus state
            ChordLeadSheetItem<?> fItem = null;
            IR_Type fIrType = null;
            Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (c instanceof ItemRenderer)
            {
                ItemRenderer ir = (ItemRenderer) c;
                fItem = ir.getModel();
                fIrType = ir.getIR_Type();
            }
            if (event instanceof SizeChangedEvent)
            {
                SizeChangedEvent e = (SizeChangedEvent) event;
                int newSize = e.getNewSize();
                int oldSize = e.getOldSize();
                // Create or delete BarBoxes as appropriate
                setNbBarBoxes(computeNbBarBoxes(NB_EXTRA_LINES));
                // Refresh bars impacted by the resize
                int minLastBar = Math.min(oldSize - 1, newSize - 1);
                int maxLastBar = Math.max(oldSize - 1, newSize - 1);
                maxLastBar = Math.min(maxLastBar, getNbBarBoxes() - 1);
                Quantization q = getDisplayQuantizationValue(clsModel.getSection(newSize - 1));
                for (int i = minLastBar + 1; i <= maxLastBar; i++)
                {
                    int bar = (i < newSize) ? i : -1;
                    BarBox bb = getBarBox(i);
                    bb.setModelBarIndex(bar);
                    if (bar != -1)
                    {
                        // Update quantization if size got bigger
                        bb.setDisplayQuantizationValue(q);
                    }
                }
            } else if (event instanceof ItemAddedEvent)
            {
                ItemAddedEvent e = (ItemAddedEvent) event;
                for (ChordLeadSheetItem<?> item : e.getItems())
                {
                    int modelBarIndex = item.getPosition().getBar();
                    addItem(modelBarIndex, item);
                }
            } else if (event instanceof ItemRemovedEvent)
            {
                ItemRemovedEvent e = (ItemRemovedEvent) event;
                for (ChordLeadSheetItem<?> item : e.getItems())
                {
                    int barIndex = item.getPosition().getBar();
                    removeItem(barIndex, item, false);
                }
            } else if (event instanceof ItemChangedEvent)
            {
                ItemChangedEvent e = (ItemChangedEvent) event;
                ChordLeadSheetItem<?> item = e.getItem();
                if (item instanceof CLI_Section)
                {
                    CLI_Section cliSection = (CLI_Section) item;
                    Section oldSection = (Section) e.getOldData();
                    Quantization q = getSectionQValue(oldSection);
                    if (q != null)
                    {
                        // Quantization was set for this section
                        if (!oldSection.getName().equals(cliSection.getData().getName()))
                        {
                            // Name has changed, need to rename the property
                            storeSectionQValue(oldSection, null);
                            storeSectionQValue(cliSection.getData(), q);
                        }
                        if (!oldSection.getTimeSignature().equals(cliSection.getData().getTimeSignature()))
                        {
                            // TimeSignature has changed, the quantization setting is not valid anymore
                            // Remove it. A new one will be restored just after in propagateSectionChange()
                            storeSectionQValue(cliSection.getData(), null);
                        }
                    }
                    propagateSectionChange((CLI_Section) item);
                }
            } else if (event instanceof ItemMovedEvent)
            {
                // A moved ChordSymbol or other, but NOT a section
                ItemMovedEvent e = (ItemMovedEvent) event;
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
            } else if (event instanceof SectionMovedEvent)
            {
                SectionMovedEvent e = (SectionMovedEvent) event;
                CLI_Section section = e.getSection();
                setSectionStartOnNewLine(section, false);
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
            } else if (event instanceof ItemBarShiftedEvent)
            {
                ItemBarShiftedEvent e = (ItemBarShiftedEvent) event;
                int barDiff = e.getBarDiff();
                assert barDiff != 0;   //NOI18N
                List<ChordLeadSheetItem<?>> items = e.getItems();
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
                            propagateSectionChange(clsModel.getSection(barIndex - 1));
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
        };
        org.jjazz.ui.utilities.api.Utilities.invokeLaterIfNeeded(run);
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
            int direction
    )
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
     * We do NOT want the height of the Panel match the height of the viewport : panel height is calculated only function of the
     * nb of rows and row height.
     */
    @Override
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }

    // ----------------------------------------------------------------------------------
    // Private functions
    // ----------------------------------------------------------------------------------
    private void setSongModified()
    {
        SavableSong s = lookup.lookup(SavableSong.class);
        if (s == null)
        {
            s = new SavableSong(songModel);
            Savable.ToBeSavedList.add(s);
            generalLookupContent.add(s);
        }
    }

    private void resetSongModified()
    {
        SavableSong s = lookup.lookup(SavableSong.class);
        if (s != null)
        {
            Savable.ToBeSavedList.remove(s);
            generalLookupContent.remove(s);
        }
    }

    /**
     * Calculate the number of BarBoxes required to accomodate clsModel's size.
     * <p>
     * @param nbExtraLines
     * @return
     */
    private int computeNbBarBoxes(int nbExtraLines)
    {
        assert clsModel != null;   //NOI18N
        if (nbExtraLines < 1 || nbExtraLines > 100)
        {
            throw new IllegalArgumentException("nbExtraLines=" + nbExtraLines);   //NOI18N
        }
        int modelSize = clsModel.getSizeInBars() + nbExtraLines * nbColumns;
        int bars = ((modelSize / nbColumns) + 1) * nbColumns;
        return bars;
    }

    /**
     * Delete/create trailing BarBoxes as required.
     */
    private void setNbBarBoxes(int newNbBarBoxes)
    {
        int oldNnBarBoxes = getNbBarBoxes();
        if (newNbBarBoxes == oldNnBarBoxes)
        {
            return;
        }

        if (oldNnBarBoxes < newNbBarBoxes)
        {
            // Need to add BarBoxes at the end

            // The BarBoxConfig to use, default one or the one of the last bar
            BarBoxConfig config = (oldNnBarBoxes == 0) ? getDefaultBarBoxConfig() : getBarBox(oldNnBarBoxes - 1).getConfig();
            int modelSize = clsModel.getSizeInBars();
            for (int i = oldNnBarBoxes; i < newNbBarBoxes; i++)
            {
                insertBarBox(i, i >= modelSize ? SelectedBar.POST_END_BAR_MODEL_BAR_INDEX : i, config);
            }
        } else
        {
            // Easier, just delete BarBoxes
            for (int i = oldNnBarBoxes - 1; i >= newNbBarBoxes; i--)
            {
                removeBarBox(getBarBox(i));
            }
        }

        updatePaddingBoxes();
        revalidate();
    }

    private BarBox getBarBox(int bbIndex)
    {
        assert bbIndex >= 0 && bbIndex < barBoxes.size() : "bbIndex=" + bbIndex + " barBoxes=" + barBoxes;   //NOI18N
        return barBoxes.get(bbIndex);
    }

    private List<BarBox> getBarBoxes()
    {
        return barBoxes;
    }

    /**
     * Insert a new BarBox at specified location.
     *
     * @param bbIndex       A value between [0, getNbBarBoxes()] (the latter will append the BarBox at the end).
     * @param modelBarIndex Use a negative value if BarBox does not represent a model's bar.
     * @param config
     * @return The created BarBox
     */
    private BarBox insertBarBox(int bbIndex, int modelBarIndex, BarBoxConfig config)
    {
        if (bbIndex < 0 || bbIndex > getNbBarBoxes() || modelBarIndex > clsModel.getSizeInBars() - 1)
        {
            throw new IllegalArgumentException("bbIndex=" + bbIndex + " getNbBarBoxes()=" + getNbBarBoxes() + " modelBarIndex=" + modelBarIndex + " config=" + config + " clsModel=" + clsModel);   //NOI18N
        }
        BarBox bb = new BarBox(this, bbIndex, modelBarIndex, clsModel, config, settings.getBarBoxSettings(), barRendererFactory);
        if (modelBarIndex >= 0)
        {
            // If bar represents the model set quantization value
            CLI_Section section = clsModel.getSection(modelBarIndex);
            Quantization q = getDisplayQuantizationValue(section);
            bb.setDisplayQuantizationValue(q);
        }

        registerBarBox(bb);

        // Forward the enabled state
        bb.setEnabled(isEnabled());

        // Insert the BarBox at correct location
        int compIndex = getComponentIndex(bbIndex);
        add(bb, compIndex);

        // Update our BarBox list
        barBoxes.add(bbIndex, bb);

        return bb;
    }

    /**
     * Return the Component index corresponding to specified BarBox index.
     * <p>
     * This takes into account PaddingBoxes or other non-BarBox components present in the editor. BarBox is inserted after
     * non-BarBox components.
     *
     * @param barBoxIndex In the range [0,getBarBoxes().size()], the latter to append the BarBox at the end
     * @return
     */
    private int getComponentIndex(int barBoxIndex)
    {
        if (barBoxIndex < 0 || barBoxIndex > getBarBoxes().size())
        {
            throw new IllegalArgumentException("barBoxIndex=" + barBoxIndex + " getBarBoxes().size()=" + getBarBoxes().size());   //NOI18N
        }
        // getComponents() should be called on EDT, otherwise need treeLock
        assert SwingUtilities.isEventDispatchThread() : "barBoxIndex=" + barBoxIndex;   //NOI18N
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
        // Update our BarBox list        
        barBoxes.remove(bb);
    }

    /**
     * Register a BarBox and its BarRenderers and ItemRenderers.
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

    }

    /**
     * Unregister a BarBox and its BarRenderers and ItemRenderers.
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
        return selectionLastContent.contains(item);
    }

    /**
     * Ask the specified BarBox to add ItemRenderer(s) for specified item.
     * <p>
     * If item is a Section do what's required to maintain editor's consistency.
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
        if (item instanceof CLI_Section)
        {
            CLI_Section cliSection = (CLI_Section) item;
            propagateSectionChange(cliSection);
            if (isSectionStartOnNewLine(cliSection))
            {
                updatePaddingBoxes();
            }
        }
    }

    /**
     * Remove the ChordLeadSheetItem from the specified bar.
     * <p>
     * If item is a section do some cleaning: update the previous section, remove the associated UI settings (quantization, start
     * on newline), possibly update padding boxes
     *
     * @param barIndex
     * @param item
     * @param skipSectionRemovalCleaning If true leave the specific "section removal" cleaning operations to the caller
     */
    private void removeItem(int barIndex, ChordLeadSheetItem<?> item, boolean skipSectionRemovalCleaning)
    {
        // Remove from selection 
        // We can not call SelectItem(false), because item
        // might have been already moved, so item.getPosition().getBar() is 
        // different from barIndex.
        selectionLookupContent.remove(item);
        selectionLastContent.remove(item);

        for (ItemRenderer ir : getBarBox(barIndex).removeItem(item))
        {
            unregisterItemRenderer(ir);
        }

        if (!skipSectionRemovalCleaning && (item instanceof CLI_Section))
        {
            CLI_Section cliSection = (CLI_Section) item;
            // Update the previous section
            propagateSectionChange(clsModel.getSection(barIndex));
            // Remove the associated UI settings
            storeSectionQValue(cliSection.getData(), null);
            setSectionStartOnNewLine(cliSection, false);
        }
    }

    /**
     * Update the bars following the specified section that their parent section has changed.
     *
     * @param cliSection
     */
    private void propagateSectionChange(CLI_Section cliSection)
    {
        int sectionSize = clsModel.getBarRange(cliSection).size();
        int barIndex = cliSection.getPosition().getBar();
        Quantization q = getDisplayQuantizationValue(cliSection);
        for (int i = barIndex; i < barIndex + sectionSize; i++)
        {
            BarBox bb = getBarBox(i);
            bb.setSection(cliSection);
            bb.setDisplayQuantizationValue(q);
        }
    }

    /**
     * Build the default BarBoxConfig from DEFAULT_BARBOX_CONFIG + optional data from BarRendererProviders.
     */
    private BarBoxConfig getDefaultBarBoxConfig()
    {

        List<String> allTypes = new ArrayList<>();
        List<String> activeTypes = new ArrayList<>();
        Collections.addAll(allTypes, DEFAULT_BAR_RENDERER_TYPES);
        Collections.addAll(activeTypes, DEFAULT_BAR_RENDERER_TYPES);


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


        var res = new BarBoxConfig(allTypes.toArray(new String[0]));        // All active by default
        res = res.setActive(activeTypes.toArray(new String[0]));


        return res;
    }

    private int computeNbColsToXFactor(int nbCols)
    {
        int xFactor = (int) Math.round((16 - nbCols) * 100.0 / 15.0);
        return xFactor;
    }

    private int computeXFactorToNbColumns(int xFactor)
    {
        int nbCols = 16 - (int) Math.round(15.0 * xFactor / 100.0);
        return nbCols;
    }

    /**
     * Save the quantization value associated to a section.
     *
     * @param sectionData
     * @param q           Can be null: this will remove the entry for the specified section.
     */
    private void storeSectionQValue(Section sectionData, Quantization q)
    {
        songModel.putClientProperty(getSectionQuantizeValuePropertyName(sectionData), q == null ? null : q.name());
        if (q != null)
        {
            // Store the last quantization use per TimeSignature.
            mapTsQuantization.put(sectionData.getTimeSignature(), q);
        }
    }

    /**
     * @param sectionData
     * @return Null if no Quantization was stored for specified section.
     * @see storeSectionQValue()
     */
    private Quantization getSectionQValue(Section sectionData)
    {
        String qString = songModel.getClientProperty(getSectionQuantizeValuePropertyName(sectionData), null);
        return Quantization.isValidStringValue(qString) ? Quantization.valueOf(qString) : null;
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
        assert SwingUtilities.isEventDispatchThread();   //NOI18N
        boolean needRevalidate = false;
        // Remove all non-BarBox components
        for (Component c : getComponents())
        {
            if (c instanceof PaddingBox)
            {
                needRevalidate = true;
                removePaddingBox((PaddingBox) c);
            }
        }

        // Add PaddingBoxes starting from the end
        List<? extends CLI_Section> cliSections = clsModel.getItems(CLI_Section.class
        );
        int offset = 0;
        for (CLI_Section cliSection : cliSections)
        {
            int sectionCompIndex = cliSection.getPosition().getBar() + offset;
            int remainder = sectionCompIndex % nbColumns;
            if (isSectionStartOnNewLine(cliSection) && remainder != 0)
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
    // Private classes
    //===========================================================================       
    /**
     * Implements the Zoomable functionalities.
     */
    private class CL_EditorZoomable implements Zoomable
    {

        @Override
        public Zoomable.Capabilities getZoomCapabilities()
        {
            return Zoomable.Capabilities.X_Y;
        }

        @Override
        public int getZoomYFactor()
        {
            return getZoomVFactor();
        }

        @Override
        public void setZoomYFactor(int newFactor, boolean valueIsAdjusting)
        {
            int oldFactor = getZoomYFactor();
            setZoomVFactor(newFactor);
            pcs.firePropertyChange(Zoomable.PROPERTY_ZOOM_Y, oldFactor, newFactor);
        }

        @Override
        public int getZoomXFactor()
        {
            int factor = computeNbColsToXFactor(nbColumns);
            LOGGER.log(Level.FINER, "getZoomFactor() nbColumns=" + nbColumns + " factor=" + factor);   //NOI18N
            return factor;
        }

        @Override
        public void setZoomXFactor(int factor, boolean valueIsAdjusting)
        {
            int nbCols = computeXFactorToNbColumns(factor);
            LOGGER.log(Level.FINER, "setZoomFactor() factor=" + factor + "> nbCols=" + nbCols);   //NOI18N
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
    };
}
