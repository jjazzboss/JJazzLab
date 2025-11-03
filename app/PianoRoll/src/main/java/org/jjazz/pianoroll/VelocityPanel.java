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
package org.jjazz.pianoroll;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.uiutilities.api.RedispatchingMouseAdapter;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * Show the velocity of notes in the bottom side of the editor.
 */
public class VelocityPanel extends EditorPanel implements PropertyChangeListener
{

    private static final int TOP_PADDING = 2;
    private final PianoRollEditor editor;
    private final NotesPanel notesPanel;
    private final TreeMap<NoteEvent, NoteView> mapNoteViews = new TreeMap<>();
    private float scaleFactorX = 1;
    private int playbackPointX = -1;
    private final RedispatchingMouseAdapter mouseRedispatcher;
    private static final Logger LOGGER = Logger.getLogger(VelocityPanel.class.getSimpleName());

    public VelocityPanel(PianoRollEditor editor, NotesPanel notesPanel)
    {
        this.editor = editor;
        this.notesPanel = notesPanel;
        this.mouseRedispatcher = new RedispatchingMouseAdapter();

        var settings = editor.getSettings();
        settings.addPropertyChangeListener(this);


        // Refresh ourself when notesPanel is resized
        this.notesPanel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                LOGGER.log(Level.FINE, "VelocityPanel.componentResized() -- notesPanel.getWidth()={0}", notesPanel.getWidth());
                revalidate();
                repaint();
            }
        });

        var vml = new VelocityPanelMouseListener();
        addMouseListener(vml);
        addMouseMotionListener(vml);

    }

    /**
     * Reuse the same preferred width than notesPanel.
     *
     * @return
     */
    @Override
    public Dimension getPreferredSize()
    {
        var pd = super.getPreferredSize();
        return new Dimension(notesPanel.getPreferredSize().width, pd.height);
    }

    /**
     * Create and add a NoteView for the specified NoteEvent.
     * <p>
     * Caller is responsible to call revalidate() and/or repaint() after, if required.
     *
     * @param ne
     * @return
     */
    @Override
    public NoteView addNoteView(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        Preconditions.checkArgument(editor.getPhraseBeatRange().contains(ne.getBeatRange(), false));

        NoteView nv = new NoteView(ne);
        nv.addMouseListener(mouseRedispatcher);
        nv.addMouseMotionListener(mouseRedispatcher);
        nv.setShowNoteString(false);
        nv.setExtraTooltip("");
        mapNoteViews.put(ne, nv);
        add(nv);
        LOGGER.log(Level.FINE, "addNoteView() ne={0} ==> mapNoteViews={1}", new Object[]
        {
            ne, mapNoteViews
        });
        return nv;
    }

    /**
     * Remove NoteView.
     * <p>
     * Caller must call revalidate() and/or repaint() after as needed.
     *
     * @param ne
     */
    @Override
    public void removeNoteView(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        NoteView nv = getNoteView(ne);  // Might be null in some corner cases ? See Issue #399
        if (nv != null)
        {
            nv.removeMouseListener(mouseRedispatcher);
            nv.removeMouseMotionListener(mouseRedispatcher);
            remove(nv);
            nv.cleanup();
        }
        mapNoteViews.remove(ne);
        LOGGER.log(Level.FINE, "removeNoteView() ne={0} ==> mapNoteViews={1}", new Object[]
        {
            ne, mapNoteViews
        });
    }

    /**
     * Get the NoteView corresponding to ne.
     *
     * @param ne Must be a ne added via addNoteView(NoteEvent ne)
     * @return Can be null
     */
    @Override
    public NoteView getNoteView(NoteEvent ne)
    {
        var res = mapNoteViews.get(ne);
        return res;
    }

    /**
     * The NoteViews sorted by NoteEvent natural order.
     *
     * @return
     */
    @Override
    public List<NoteView> getNoteViews()
    {
        return new ArrayList<>(mapNoteViews.values());
    }

    /**
     * Replace the model of an existing NoteView.
     * <p>
     * Caller is responsible of calling revalidate() and/or repaint() after as needed.
     *
     * @param oldNe
     * @param newNe
     */
    @Override
    public void setNoteViewModel(NoteEvent oldNe, NoteEvent newNe)
    {
        var nv = getNoteView(oldNe);
        assert nv != null : " oldNe=" + oldNe;
        nv.setModel(newNe);
        mapNoteViews.remove(oldNe);
        mapNoteViews.put(newNe, nv);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;

//        LOGGER.log(Level.FINE, "paintComponent() -- width={0} height={1}", new Object[]
//        {
//            getWidth(), getHeight()
//        });

        // Make sure clip is restricted to a valid area
        g2.clipRect(0, 0, notesPanel.getWidth(), getHeight());
        var clip = g2.getClipBounds();

        var xMapper = notesPanel.getXMapper();
        if (!xMapper.isUptodate())
        {
            return;
        }

        // Fill background corresponding to notesPanel width
        Color c = editor.getSettings().getWhiteKeyLaneBackgroundColor();
        g2.setColor(c);
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);


        // Paint loop zone
        paintLoopZone(g);


        // Grid
        notesPanel.paintVerticalGrid(g2, clip.y, clip.y + clip.height - 1);


        // Paint playback line
        if (playbackPointX >= 0)
        {
            g2.setColor(NotesPanelLayerUI.COLOR_PLAYBACK_LINE);
            g2.drawLine(playbackPointX, 0, playbackPointX, getHeight() - 1);
        }


    }

    @Override
    public void showPlaybackPoint(int xPos)
    {
        int oldX = playbackPointX;
        playbackPointX = xPos;
        if (playbackPointX != oldX)
        {
            int x0, x1;
            if (oldX == -1)
            {
                x0 = xPos - 1;
                x1 = xPos + 1;
            } else if (playbackPointX == -1)
            {
                x0 = oldX - 1;
                x1 = oldX + 1;
            } else
            {
                x0 = Math.min(playbackPointX, oldX) - 1;
                x1 = Math.max(playbackPointX, oldX) + 1;
            }
            repaint(x0, 0, x1 - x0 + 1, getHeight());
        }
    }

    /**
     * Layout all the NoteEvents.
     */
    @Override
    public void doLayout()
    {
        // LOGGER.severe("doLayout() -- ");
        final int USABLE_HEIGHT = getHeight() - TOP_PADDING;
        final int NOTE_WIDTH_DEFAULT = 5;
        final int NOTE_WIDTH_DELTA = 3;
        int w = Math.round(NOTE_WIDTH_DEFAULT + (2f * editor.getZoom().hValueFloat() - 1f) * NOTE_WIDTH_DELTA);


        // If several notes at same position, slightly change their x position so that they can be all visible
        int lastX = -10;
        int lastXShift = 0;
        for (var nv : mapNoteViews.values())
        {
            var ne = nv.getModel();
            int x = editor.getXFromPosition(ne.getPositionInBeats());

            if (Math.abs(x - lastX) <= 1)
            {
                lastXShift += 3;
                x += lastXShift;
            } else
            {
                lastX = x;
                lastXShift = 0;
            }

            int h = Math.round(USABLE_HEIGHT * (ne.getVelocity() / 127f));
            int y = getHeight() - h;

            nv.setBounds(x, y, w, h);
        }

    }

    /**
     * Set the X scale factor.
     * <p>
     * Impact the width of the notes.
     *
     * @param factorX A value &gt; 0
     */
    @Override
    public void setScaleFactorX(float factorX)
    {
        Preconditions.checkArgument(factorX > 0);

        if (this.scaleFactorX != factorX)
        {
            scaleFactorX = factorX;
            revalidate();
            repaint();
        }
    }

    /**
     * Get the current scale factor on the X axis.
     *
     * @return
     */
    @Override
    public float getScaleFactorX()
    {
        return scaleFactorX;
    }

    @Override
    public void cleanup()
    {
        for (var nv : mapNoteViews.values().toArray(NoteView[]::new))
        {
            removeNoteView(nv.getModel());
        }
        editor.getSettings().removePropertyChangeListener(this);
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

        if (evt.getSource() == editor.getSettings())
        {
            settingsChanged();
        }
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================    
    private void settingsChanged()
    {
        repaint();
    }

    private void paintLoopZone(Graphics g)
    {
        IntRange loopZone = editor.getLoopZone();
        if (loopZone != null)
        {
            var loopZoneXRange = notesPanel.getXMapper().getXRange(loopZone);
            var lZone = new Rectangle(loopZoneXRange.from, 0, loopZoneXRange.size(), getHeight());
            lZone = lZone.intersection(g.getClipBounds());
            Color c = editor.getSettings().getLoopZoneWhiteKeyLaneBackgroundColor();
            g.setColor(c);
            g.fillRect(lZone.x, lZone.y, lZone.width, lZone.height);
        }
    }

    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================    
    /**
     * Handle mouse events.
     * <p>
     * - Dragging or clicking update notes velocity.<br>
     */
    private class VelocityPanelMouseListener extends MouseAdapter
    {

        private final String UNDO_TEXT = "Update note(s) velocity";
        /**
         * Null if no dragging.
         */
        private Point startDraggingPoint;
        /**
         * Notes matching the x value of the mouse pointer.
         */
        private NavigableSet<NoteEvent> impactedNotes = new TreeSet<>();

        @Override
        public void mouseClicked(MouseEvent e)
        {
            // LOGGER.severe("mouseClicked()");
            if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown())
            {
                editor.getUndoManager().startCEdit(editor, UNDO_TEXT);
                updateVelocity(e);
                editor.getUndoManager().endCEdit(UNDO_TEXT);

            }
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            if (e.isControlDown() || startDraggingPoint != null)
            {
                return;
            }

            updateImpactedNotes(e);

            if (impactedNotes.isEmpty() && startDraggingPoint == null)
            {
                setCursor(Cursor.getDefaultCursor());
            } else
            {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {

            if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown())
            {
                if (startDraggingPoint == null)
                {
                    startDraggingPoint = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    editor.getUndoManager().startCEdit(editor, UNDO_TEXT);
                } else
                {
                    // LOGGER.severe("mouseDragged() continue");
                    if (contains(e.getX(), e.getY()))
                    {
                        updateImpactedNotes(e);
                        updateVelocity(e);
                    }
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            // LOGGER.severe("mouseReleased()");
            if (startDraggingPoint != null && SwingUtilities.isLeftMouseButton(e))
            {
                startDraggingPoint = null;
                setCursor(Cursor.getDefaultCursor());
                editor.getUndoManager().endCEdit(UNDO_TEXT);
            }

        }

        // ============================================================================================
        // Private methods
        // ============================================================================================
        private int getVelocity(Point p)
        {
            int v = Math.round((127 * (getHeight() - 1f - p.y)) / (getHeight() - TOP_PADDING));
            return MidiConst.clamp(v);
        }

        /**
         * Update the list of impacted notes.
         *
         * @param e
         */
        private void updateImpactedNotes(MouseEvent e)
        {
            var xMapper = notesPanel.getXMapper();
            float posInBeats = xMapper.getBeatPosition(e.getX());
            if (posInBeats < 0)
            {
                return;
            }
            final float HALF_WIDTH = 0.1f;
            var br = new FloatRange(Math.max(0, posInBeats - HALF_WIDTH), posInBeats + HALF_WIDTH);
            impactedNotes = editor.getModel().subSet(br, false);
        }

        /**
         * Update the velocity of the impactedNotes.
         *
         * @param e
         */
        private void updateVelocity(MouseEvent e)
        {
            if (!impactedNotes.isEmpty())
            {
                int v = getVelocity(e.getPoint());
                for (var ne : impactedNotes.toArray(NoteEvent[]::new))
                {
                    var newNe = ne.setVelocity(v, true);
                    editor.getModel().replace(ne, newNe);
                }
            }
        }
    }

}
