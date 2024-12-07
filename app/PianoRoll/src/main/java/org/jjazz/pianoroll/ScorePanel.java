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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsActionEvent;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.rhythmmusicgeneration.api.ChordSequence;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.score.api.NotationGraphics;
import org.jjazz.song.api.Song;
import org.jjazz.uiutilities.api.HSLColor;
import org.jjazz.uiutilities.api.RedispatchingMouseAdapter;
import org.jjazz.uiutilities.api.StringMetrics;

/**
 * Show the notes in a notation staff in the bottom side of the editor.
 */
public class ScorePanel extends EditorPanel implements PropertyChangeListener, ClsChangeListener
{

    private static final int TOP_PADDING = 2;
    private final PianoRollEditor editor;
    private final NotesPanel notesPanel;
    private float scaleFactorX = 1;
    private int displayTransposition;
    private final RedispatchingMouseAdapter mouseRedispatcher;
    private final NotationGraphics ng;
    private Song song;
    private static final Logger LOGGER = Logger.getLogger(ScorePanel.class.getSimpleName());


    public ScorePanel(PianoRollEditor editor, NotesPanel notesPanel)
    {
        this.setDoubleBuffered(true);

        this.editor = editor;
        this.notesPanel = notesPanel;
        ng = new NotationGraphics();
        ng.setSize(30f);
        displayTransposition = 0;

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


        var vml = new ScoreMouseListener();
        addMouseListener(vml);
        addMouseMotionListener(vml);

    }

    public int getDisplayTransposition()
    {
        return displayTransposition;
    }

    public void setDisplayTransposition(int displayTransposition)
    {
        this.displayTransposition = displayTransposition;
        repaint();
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

    @Override
    public NoteView addNoteView(NoteEvent ne)
    {
        return null;
    }

    @Override
    public void removeNoteView(NoteEvent ne)
    {
    }

    @Override
    public NoteView getNoteView(NoteEvent ne)
    {
        return null;
    }


    @Override
    public List<NoteView> getNoteViews()
    {
        return new ArrayList<>();
    }

    @Override
    public void setNoteViewModel(NoteEvent oldNe, NoteEvent newNe)
    {
        // 
    }

    @Override
    public void paintComponent(Graphics g)
    {
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
        int notesPanelWidth = notesPanel.getWidth();
        Color c = editor.getSettings().getBackgroundColor2();
        g2.setColor(c);
        g2.fillRect(0, 0, notesPanelWidth, getHeight());


        // Possible loop zone
        var loopZone = editor.getLoopZone();
        if (loopZone != null)
        {
            c = HSLColor.changeLuminance(c, -6);
            g2.setColor(c);
            int xFrom = xMapper.getX(new Position(loopZone.from));
            int xTo = xMapper.getBarRange().contains(loopZone.to + 1) ? xMapper.getX(new Position(loopZone.to + 1)) : xMapper.getLastWidth() - 1;
            g2.fillRect(xFrom, 0, xTo - xFrom, getHeight());
        }


        // Grid
        notesPanel.drawVerticalGrid(g2, 0, getHeight() - 1);

        final Color NOTE_COLOR = Color.BLACK;
        final Color CLEF_COLOR = new Color(NOTE_COLOR.getRed(), NOTE_COLOR.getGreen(), NOTE_COLOR.getBlue(), 150); // A bit transparent


        // Show chord symbols if available
        if (song != null)
        {
            final Color COLOR_CHORD_SYMBOL_FONT = CLEF_COLOR;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font baseFont = editor.getSettings().getRulerBaseFont();      
            int baseFontHeight = (int) Math.ceil(new StringMetrics(g2, baseFont).getHeight("F"));
            float oneBeatPixelSize = xMapper.getOneBeatPixelSize();
            
            // Take into account the possible different rulerStartBar
            int barOffset = editor.getRulerStartBar() - editor.getPhraseStartBar();

            // Draw chord symbols
            var offsettedBarRange = editor.getPhraseBarRange().getTransformed(barOffset);
            ChordSequence cs = new ChordSequence(offsettedBarRange);
            SongChordSequence.fillChordSequence(cs, song, offsettedBarRange);
            int lastBar = -1;

            for (var cliCs : cs)
            {
                var pos = cliCs.getPosition();

                if (oneBeatPixelSize > 14 || (pos.getBar() != lastBar && pos.isFirstBarBeat()))
                {
                    // Draw the chord symbol
                    var posOffsetted = pos.getMoved(-barOffset, 0); // Convert to phraseStartBar
                    var posInBeats = editor.toPositionInBeats(posOffsetted);
                    int x = editor.getXFromPosition(posInBeats);
                    int y = baseFontHeight;
                    AttributedString aStr;
                    aStr = new AttributedString(cliCs.getData().getOriginalName(), baseFont.getAttributes());
                    aStr.addAttribute(TextAttribute.FOREGROUND, COLOR_CHORD_SYMBOL_FONT);
                    aStr.addAttribute(TextAttribute.BACKGROUND, editor.getSettings().getBackgroundColor2());
                    if (posOffsetted.isFirstBarBeat())
                    {
                        x += 1;
                    }
                    g2.drawString(aStr.getIterator(), x, y);
                } else
                {
                    // No room to draw several chord symbols per bar
                    continue;
                }

                lastBar = pos.getBar();
            }
        }


        // Score
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ng.setGraphics(g2);

        final int G_STAFF_FIRST_LINE = 8;
        final int F_STAFF_FIRST_LINE = G_STAFF_FIRST_LINE + 9;
        final int G_STAFF_LOWEST_PITCH = 57;
        final int F_STAFF_HIGHEST_PITCH = 60;


        // G staff 
        g2.setColor(NOTE_COLOR);
        ng.absolute(0);         // advance of x gridSize
        ng.absoluteLine(G_STAFF_FIRST_LINE);     // location of first bottom staff line, counted from top of the window
        ng.drawStaff(notesPanelWidth);
        ng.drawBarLine();
        g2.setColor(CLEF_COLOR);
        ng.relative(1);
        ng.drawClef(NotationGraphics.CLEF_G);


        // F staff 
        g2.setColor(NOTE_COLOR);
        ng.absolute(0);         // advance of x gridSize
        ng.absoluteLine(F_STAFF_FIRST_LINE);     // location of first bottom staff line, counted from top of the window
        ng.drawStaff(notesPanelWidth);
        ng.drawBarLine();
        g2.setColor(CLEF_COLOR);
        ng.relative(1);
        ng.drawClef(NotationGraphics.CLEF_F);


        // Notes
        g2.setColor(NOTE_COLOR);

        for (var ne : editor.getModel())
        {
            int p = ne.getPitch();
            if (displayTransposition != 0)
            {
                p = MidiUtilities.limit(p + displayTransposition);
                ne = ne.setPitch(p);
            }
            int x = xMapper.getX(ne.getPositionInBeats());
            ng.absoluteX(x);

            boolean useFstaff = p < G_STAFF_LOWEST_PITCH;
            int line = useFstaff ? F_STAFF_FIRST_LINE : G_STAFF_FIRST_LINE;
            ng.absoluteLine(line);

            ng.drawNote(ng.new ScoreNote(ne, 2, -1, useFstaff)); // dur=2=quarter, linedir=-1=no tie
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

    /**
     * Let the ScorePanel show chord symbols.
     *
     * @param song
     */
    public void setSong(Song song)
    {
        this.song = song;

        // Listen to chord leadsheet changes (song structure changes must be managed at a higher level)
        song.getChordLeadSheet().addClsChangeListener(this);
        repaint();
    }

    @Override
    public void cleanup()
    {
        editor.getSettings().removePropertyChangeListener(this);
        if (song != null)
        {
            song.getChordLeadSheet().removeClsChangeListener(this);
        }
    }
    // ==========================================================================================================
    // ClsChangeListener interface
    // ==========================================================================================================    

    @Override
    public void authorizeChange(ClsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent e)
    {
        if (e instanceof ClsActionEvent ae && ae.isActionComplete())
        {
            // Listen to all user actions which do not trigger a song structure change
            switch (ae.getActionId())
            {
                case "addItem", "changeItem", "removeItem", "moveItem", "setSectionName" ->
                {
                    repaint();
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


    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================    
    /**
     * Handle mouse events.
     * <p>
     * - Drag = move the editor.
     */
    private class ScoreMouseListener extends MouseAdapter
    {

        /**
         * Null if no dragging.
         */
        private Point startDraggingPoint;

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (SwingUtilities.isLeftMouseButton(e))
            {
                if (startDraggingPoint == null)
                {
                    startDraggingPoint = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else
                {
                    // LOGGER.severe("mouseDragged() continue");
                    // Move the editor
                    JViewport viewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, notesPanel);
                    if (viewPort != null)
                    {
                        int deltaX = startDraggingPoint.x - e.getX();
                        Rectangle view = viewPort.getViewRect();
                        view.x += deltaX;
                        notesPanel.scrollRectToVisible(view);
                    }
                }
            }

        }


        @Override
        public void mouseReleased(MouseEvent e)
        {
            // LOGGER.severe("mouseReleased()");
            if (SwingUtilities.isLeftMouseButton(e))
            {
                startDraggingPoint = null;
                setCursor(Cursor.getDefaultCursor());
            }
        }

    }


}
