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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.jjazz.harmony.api.Position;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.uiutilities.api.HSLColor;
import org.jjazz.utilities.api.FloatRange;

/**
 * Show the velocity of notes in the bottom side of the editor.
 */
public class VelocityPanel extends JPanel implements PropertyChangeListener
{

    private final PianoRollEditor editor;
    private final NotesPanel notesPanel;
    private final TreeMap<NoteEvent, NoteView> mapNoteViews = new TreeMap<>();
    private static final Logger LOGGER = Logger.getLogger(VelocityPanel.class.getSimpleName());


    public VelocityPanel(PianoRollEditor editor, NotesPanel notesPanel)
    {
        this.editor = editor;
        this.notesPanel = notesPanel;

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
        return new Dimension(notesPanel.getPreferredSize().width, pd.width);
    }

    /**
     * Create and add a NoteView for the specified NoteEvent.
     * <p>
     * Caller is responsible to call revalidate() and/or repaint() after, if required.
     *
     * @param ne
     * @return
     */
    public NoteView addNoteView(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        Preconditions.checkArgument(editor.getPhraseBeatRange().contains(ne.getBeatRange(), false));

        NoteView nv = new NoteView(ne);
        nv.setShowNoteString(false);
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
    public void removeNoteView(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        NoteView nv = getNoteView(ne);  // Might be null in some corner cases ? See Issue #399
        if (nv != null)
        {
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
        super.paintComponent(g);        // Honor the opaque property

        Graphics2D g2 = (Graphics2D) g;

//        LOGGER.log(Level.FINE, "paintComponent() -- width={0} height={1}", new Object[]
//        {
//            getWidth(), getHeight()
//        });

        var xMapper = notesPanel.getXMapper();
        if (!xMapper.isUptodate())
        {
            return;
        }

        // Fill background corresponding to notesPanel width
        Color c = editor.getSettings().getBackgroundColor1();
        g2.setColor(c);
        g2.fillRect(0, 0, notesPanel.getWidth(), getHeight());


        // Possible loop zone
        var loopZone = editor.getLoopZone();
        if (loopZone != null)
        {
            c = HSLColor.changeLuminance(c, -6);
            g2.setColor(c);            
            int xFrom = xMapper.getX(new Position(loopZone.from, 0));
            int xTo = xMapper.getBarRange().contains(loopZone.to + 1) ? xMapper.getX(new Position(loopZone.to + 1, 0)) : xMapper.getLastWidth() - 1;
            g2.fillRect(xFrom, 0, xTo - xFrom, getHeight());
        }


        // Grid
        notesPanel.drawVerticalGrid(g2, 0, getHeight() - 1);
    }

    /**
     * Layout all the NoteEvents.
     */
    @Override
    public void doLayout()
    {
        // LOGGER.severe("doLayout() -- ");
        int TOP_PADDING = 2;
        int height = getHeight() - TOP_PADDING;

        for (NoteView nv : mapNoteViews.values())
        {
            NoteEvent ne = nv.getModel();
            FloatRange br = ne.getBeatRange();
            int x = editor.getXFromPosition(br.from);
            int h = Math.round(height * (ne.getVelocity() / 127f));
            int y = getHeight() - h;
            int w = 4;
            nv.setBounds(x, y, w, h);
        }

    }

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


}
