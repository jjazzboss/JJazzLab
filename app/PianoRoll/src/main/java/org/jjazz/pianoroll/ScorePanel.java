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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.score.api.NotationGraphics;
import org.jjazz.score.api.MeasureContext;
import org.jjazz.uiutilities.api.HSLColor;
import org.jjazz.uiutilities.api.StringMetrics;
import org.jjazz.uiutilities.api.UIUtilities;

/**
 * Show the notes in a notation staff in the bottom side of the editor.
 */
public class ScorePanel extends EditorPanel implements PropertyChangeListener
{

    private static final int G_STAFF_FIRST_LINE = 9;
    private static final int F_STAFF_FIRST_LINE = G_STAFF_FIRST_LINE + 9;
    private static final int G_STAFF_LOWEST_PITCH = 57;
    private final PianoRollEditor editor;
    private final NotesPanel notesPanel;
    private float scaleFactorX = 1;
    private int displayTransposition;
    private final NotationGraphics ng;
    private static final Logger LOGGER = Logger.getLogger(ScorePanel.class.getSimpleName());


    public ScorePanel(PianoRollEditor editor, NotesPanel notesPanel)
    {
        this.setDoubleBuffered(true);

        this.editor = editor;
        this.editor.addPropertyChangeListener(PianoRollEditor.PROP_CHORD_SEQUENCE, this);

        this.notesPanel = notesPanel;
        ng = new NotationGraphics();
        ng.setSize(30f);
        displayTransposition = 0;

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
            int xTo = editor.getPhraseBarRange().contains(loopZone.to + 1) ? xMapper.getX(new Position(loopZone.to + 1)) : xMapper.getLastWidth() - 1;
            g2.fillRect(xFrom, 0, xTo - xFrom, getHeight());
        }


        // Grid
        notesPanel.drawVerticalGrid(g2, 0, getHeight() - 1);

        final Color NOTE_COLOR = Color.BLACK;
        final Color CLEF_COLOR = new Color(NOTE_COLOR.getRed(), NOTE_COLOR.getGreen(), NOTE_COLOR.getBlue(), 150); // A bit transparent


        // Save chord symbols beat position for notes drawing below
        TreeMap<Float, CLI_ChordSymbol> tmapPosChordSymbol = new TreeMap<>();


        var chordSequence = editor.getChordSequence();
        if (chordSequence != null)
        {
            // Show chord symbols if available
            final Color COLOR_CHORD_SYMBOL_FONT = CLEF_COLOR;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font baseFont = UIUtilities.changeFontSize(editor.getSettings().getRulerBaseFont(), 1f);
            int baseFontHeight = (int) Math.ceil(new StringMetrics(g2, baseFont).getHeight("I"));
            float oneBeatPixelSize = xMapper.getOneBeatPixelSize();

            // Take into account the possible different rulerStartBar
            int barOffset = editor.getRulerStartBar() - editor.getPhraseStartBar();

            // Draw chord symbols
            int lastBar = -1;

            for (var cliCs : chordSequence)
            {
                var pos = cliCs.getPosition();

                if (oneBeatPixelSize > 14 || (pos.getBar() != lastBar && pos.isFirstBarBeat()))
                {
                    // Draw the chord symbol
                    var posOffsetted = pos.getMoved(-barOffset, 0); // Convert to phraseStartBar
                    var posInBeats = editor.toPositionInBeats(posOffsetted);
                    tmapPosChordSymbol.put(posInBeats, cliCs);      // Save chord beat position
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

        // Group notes by position so that chords are correctly displayed (eg with adjacent notes)
        MeasureContext mContext = null;
        List<NoteEvent> samePosNotes = new ArrayList<>();
        Phrase p = editor.getModel();

        for (var ne : p)
        {
            boolean draw = true;
            boolean last = ne == p.last();
            if (samePosNotes.isEmpty() || samePosNotes.get(0).equalsAsNoteNearPosition(ne, 0.1f))
            {
                // Store notes with same position
                samePosNotes.add(ne);
                draw = false;
            }

            if (draw || last)
            {
                // Draw the notes
                float posInBeats = samePosNotes.get(0).getPositionInBeats();
                int bar = xMapper.toPosition(posInBeats).getBar();
                int x = xMapper.getX(posInBeats);
                Float csPos = tmapPosChordSymbol.floorKey(posInBeats);
                var cs = csPos == null ? null : tmapPosChordSymbol.get(csPos).getData();
                if (mContext == null || mContext.getBarIndex() != bar)
                {
                    mContext = new MeasureContext(bar, new Note(60));
                }

                drawSamePositionNotes(samePosNotes, x, mContext, cs);

                samePosNotes.clear();
                samePosNotes.add(ne);
            }
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
        editor.removePropertyChangeListener(this);
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
        } else if (evt.getSource() == editor)
        {
            repaint();
        }
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================    

    private void settingsChanged()
    {
        repaint();
    }

    /**
     * Draw one or more notes at same position.
     *
     * @param nes      Notes must be approximatively at same position. Can't be empty.
     * @param x        x coordinate where to draw the notes
     * @param mContext
     * @param cs       Optional chord symbol
     */
    private void drawSamePositionNotes(List<NoteEvent> nes, int x, MeasureContext mContext, ChordSymbol cs)
    {
        Preconditions.checkArgument(!nes.isEmpty());

        ng.absoluteX(x);

        if (nes.size() > 1)
        {
            // Manage note shift for chord adjacent notes and a unique line
            ng.startNoteGroup();
        }

        for (var ne : nes)
        {
            int p = ne.getPitch();
            if (displayTransposition != 0)
            {
                p = MidiUtilities.limit(p + displayTransposition);
                ne = ne.setPitch(p);
            }
            boolean useFstaff = p < G_STAFF_LOWEST_PITCH;
            int line = useFstaff ? F_STAFF_FIRST_LINE : G_STAFF_FIRST_LINE;
            ng.absoluteLine(line);

            var sn = mContext.buildScoreNote(ne, useFstaff, cs);
            ng.drawNote(sn.staffLine, NotationGraphics.NOTE_DURATION_QUARTER, 0, sn.accidental, 0, NotationGraphics.LINE_DIR_NO);
        }

        if (nes.size() > 1)
        {
            ng.endNoteGroup();
        }

    }

    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================    


}
