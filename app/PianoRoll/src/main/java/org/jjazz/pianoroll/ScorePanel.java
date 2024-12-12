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
import com.google.common.primitives.Ints;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.rhythmmusicgeneration.api.ChordSequence;
import org.jjazz.score.api.NotationGraphics;
import org.jjazz.score.api.MeasureContext;
import org.jjazz.song.api.Song;
import org.jjazz.uiutilities.api.StringMetrics;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * Show the notes in a notation staff in the bottom side of the editor.
 */
public class ScorePanel extends EditorPanel implements PropertyChangeListener
{

    private static final Color NOTE_COLOR = Color.BLACK;
    private static final Color CLEF_COLOR = Color.DARK_GRAY;
    private static final Color COLOR_CHORD_SYMBOL_FONT = Color.DARK_GRAY;

    public static final String PROP_OCTAVE_TRANSPOSITION = "OctaveTransposition";
    public static final String CLIENT_PROP_OCTAVE_TRANSPOSITION_BASE = "NoteEditorScoreOctaveTransposition";
    private static final int G_STAFF_FIRST_LINE = 9;
    private static final int F_STAFF_FIRST_LINE = G_STAFF_FIRST_LINE + 9;
    private static final int G_STAFF_LOWEST_PITCH = 57;
    private final PianoRollEditor editor;
    private final NotesPanel notesPanel;
    private float scaleFactorX = 1;
    private int octaveTransposition;
    private final NotationGraphics ng;
    private Song song;
    private final Font baseChordFont;
    private final int baseChordFontHeight;

    private static final Logger LOGGER = Logger.getLogger(ScorePanel.class.getSimpleName());


    public ScorePanel(PianoRollEditor editor, NotesPanel notesPanel)
    {
        this.editor = editor;
        this.editor.addPropertyChangeListener(this);

        this.notesPanel = notesPanel;
        ng = new NotationGraphics();
        ng.setSize(30f);
        octaveTransposition = 0;

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


        baseChordFont = UIUtilities.changeFontSize(editor.getSettings().getRulerBaseFont(), 1f);
        baseChordFontHeight = (int) Math.ceil(new StringMetrics(baseChordFont).getHeight("I")); // include baseline

    }

    public int getOctaveTransposition()
    {
        return octaveTransposition;
    }

    /**
     * Set the transposition applied to notes in the score.
     * <p>
     * Fire a PROP_OCTAVE_TRANSPOSITION change. If song is defined, save value as a song client property.
     *
     * @param nbOctaves [-3;3]
     */
    public void setOctaveTransposition(int nbOctaves)
    {
        Preconditions.checkArgument(nbOctaves >= -3 && nbOctaves <= 3);
        if (this.octaveTransposition == nbOctaves)
        {
            return;
        }
        int old = this.octaveTransposition;
        this.octaveTransposition = nbOctaves;
        if (song != null)
        {
            song.getClientProperties().putInt(buildClientPropNameOctave(editor.getChannel()), octaveTransposition);
        }
        firePropertyChange(PROP_OCTAVE_TRANSPOSITION, old, octaveTransposition);
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
        //
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
        var clip = g2.getClipBounds();
        IntRange barRange = notesPanel.getClipBarRange(g);

//        LOGGER.log(Level.INFO, "paintComponent() -- width={0} height={1} clip={2}", new Object[]
//        {
//            getWidth(), getHeight(), g2.getClipBounds()
//        });

        var xMapper = notesPanel.getXMapper();
        if (!xMapper.isUptodate())
        {
            return;
        }

        // Fill background corresponding to notesPanel width
        Color c = editor.getSettings().getBackgroundColor2();
        g2.setColor(c);
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);


        notesPanel.paintLoopZone(this, g);

        notesPanel.paintVerticalGrid(g2, 0, getHeight() - 1);


        // Show chord symbols if available        
        TreeMap<Float, CLI_ChordSymbol> tmapPosChordSymbol = new TreeMap<>();       // Save chord symbols beat position for notes drawing below
        var chordSequence = editor.getChordSequence();
        if (chordSequence != null)
        {
            paintChords(g2, chordSequence, barRange, tmapPosChordSymbol);
        }


        paintNotes(g2, tmapPosChordSymbol);

    }

    /**
     * Paint the chords
     *
     * @param g2
     * @param chordSequence
     * @param barRange
     * @param tmapPosChordSymbol Method will save the calculated positions of the chords here
     */
    private void paintChords(Graphics2D g2, ChordSequence chordSequence, IntRange barRange, TreeMap<Float, CLI_ChordSymbol> tmapPosChordSymbol)
    {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float oneBeatPixelSize = notesPanel.getXMapper().getOneBeatPixelSize();

        // Take into account the possible different rulerStartBar
        int barOffset = editor.getRulerStartBar() - editor.getPhraseStartBar();

        // Draw chord symbols
        int lastBar = -1;

        for (var cliCs : chordSequence)
        {
            var pos = cliCs.getPosition();
            if (!barRange.contains(pos.getBar()))
            {
                continue;
            }

            if (oneBeatPixelSize > 14 || (pos.getBar() != lastBar && pos.isFirstBarBeat()))
            {
                // Draw the chord symbol
                var posOffsetted = pos.getMoved(-barOffset, 0); // Convert to phraseStartBar
                var posInBeats = editor.toPositionInBeats(posOffsetted);
                tmapPosChordSymbol.put(posInBeats, cliCs);      // Save chord beat position
                int x = editor.getXFromPosition(posInBeats);
                int y = baseChordFontHeight;
                AttributedString aStr;
                aStr = new AttributedString(cliCs.getData().getOriginalName(), baseChordFont.getAttributes());
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

    private void paintNotes(Graphics2D g2, TreeMap<Float, CLI_ChordSymbol> tmapPosChordSymbol)
    {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ng.setGraphics(g2);

        int w = getWidth();

        // G staff 
        g2.setColor(NOTE_COLOR);
        ng.absolute(0);         // advance of x gridSize
        ng.absoluteLine(G_STAFF_FIRST_LINE);     // location of first bottom staff line, counted from top of the window
        ng.drawStaff(w);
        ng.drawBarLine();
        g2.setColor(CLEF_COLOR);
        ng.relative(1);
        ng.drawClef(NotationGraphics.CLEF_G);


        // F staff 
        g2.setColor(NOTE_COLOR);
        ng.absolute(0);         // advance of x gridSize
        ng.absoluteLine(F_STAFF_FIRST_LINE);     // location of first bottom staff line, counted from top of the window
        ng.drawStaff(w);
        ng.drawBarLine();
        g2.setColor(CLEF_COLOR);
        ng.relative(1);
        ng.drawClef(NotationGraphics.CLEF_F);


        // Paint all the notes  intersecting the clip
        g2.setColor(NOTE_COLOR);
        var xMapper = notesPanel.getXMapper();
        var clipBr = notesPanel.getClipBeatRange(g2);
        MeasureContext mContext = null;
        var nes = editor.getModel().getNotes(ne -> clipBr.intersects(ne.getBeatRange()), FloatRange.MAX_FLOAT_RANGE, true);
        PitchSortedNotes samePosNotes = new PitchSortedNotes();


        int i = 0;
        while (i < nes.size())
        {
            var ne = nes.get(i);
            samePosNotes.clear();
            samePosNotes.addOrdered(ne);
            i++;

            // Group all the next notes if they are at the same position
            while (i < nes.size() && nes.get(i).isNear(ne.getPositionInBeats(), 0.1f))
            {
                samePosNotes.addOrdered(nes.get(i));
                i++;
            }

            // Prepare to paint the group of notes
            float posInBeats = ne.getPositionInBeats();
            int bar = xMapper.toPosition(posInBeats).getBar();
            int x = xMapper.getX(posInBeats);
            Float csPos = tmapPosChordSymbol.floorKey(posInBeats);
            var cs = csPos == null ? null : tmapPosChordSymbol.get(csPos).getData();
            if (mContext == null || mContext.getBarIndex() != bar)
            {
                mContext = new MeasureContext(bar, new Note(60));
            }

            paintSamePositionNotes(g2, samePosNotes, x, mContext, cs, NoteView.getNotes(editor.getSelectedNoteViews()));

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
     * Try to restore the octave transposition, or compute the preferred one.
     *
     * @param song
     */
    public void setSong(Song song)
    {
        Objects.requireNonNull(song);
        this.song = song;
        updateOctaveTransposition();
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
//        LOGGER.log(Level.INFO, "propertyChange() evt.source.class={0} prop={1} old={2} new={3}", new Object[]
//        {
//            evt.getSource().getClass().getSimpleName(), evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()
//        });


        if (evt.getSource() == editor.getSettings())
        {
            settingsChanged();
        } else if (evt.getSource() == editor)
        {
            switch (evt.getPropertyName())
            {
                case PianoRollEditor.PROP_CHORD_SEQUENCE -> repaint();
                case PianoRollEditor.PROP_SELECTED_NOTE_VIEWS ->
                {
                    var xMapper = notesPanel.getXMapper();
                    if (!xMapper.isUptodate())
                    {
                        repaint();
                    } else
                    {
                        // We can probably do a much smaller repaint
                        final int EXTRA = 100;   // Need to cover for note width, tie, shift, alterations, ...
                        List<NoteView> nes = (List<NoteView>) evt.getOldValue();
                        NoteEvent neFirst = nes.get(0).getModel();
                        NoteEvent neLast = nes.get(nes.size() - 1).getModel();
                        int xFirst = Math.max(xMapper.getX(neFirst.getPositionInBeats()) - EXTRA, 0);
                        int xLast = Math.min(xMapper.getX(neLast.getPositionInBeats()) + EXTRA, getWidth() - 1);
                        repaint(xFirst, 0, xLast - xFirst + 1, getHeight());
                    }
                }
                case PianoRollEditor.PROP_MODEL_PHRASE -> updateOctaveTransposition();
                default ->
                {
                }
            }

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
     * @param g2
     * @param nes           Notes must be approximatively at same position, and ordered by pitch. Can't be empty.
     * @param x             x coordinate where to draw the notes
     * @param mContext
     * @param cs            Optional chord symbol
     * @param selectedNotes
     */
    private void paintSamePositionNotes(Graphics2D g2, List<NoteEvent> nes, int x, MeasureContext mContext, ChordSymbol cs, List<NoteEvent> selectedNotes)
    {
        Preconditions.checkArgument(!nes.isEmpty());
//        LOGGER.log(Level.INFO, "paintSamePositionNotes() -- nes={0} selectedNotes={1}", new Object[]
//        {
//            new ArrayList<>(nes), new ArrayList<>(selectedNotes)
//        });

        ng.absoluteX(x);
        final int ADJACENT_NOTE_SHIFT = (int) ng.getNoteBaseWidth();
        NoteEvent unshiftableNote = null;

        for (int i = 0; i < nes.size(); i++)
        {
            var ne = nes.get(i);
            var p = ne.getPitch();
            int shift = 0;

            if (i < nes.size() - 1 && nes.get(i + 1).getPitch() <= p + 2)
            {
                unshiftableNote = nes.get(i + 1);
                shift = ADJACENT_NOTE_SHIFT;
            }


            Color c = selectedNotes.contains(ne) ? Color.RED.brighter() : Color.BLACK;
            g2.setColor(c);

            if (octaveTransposition != 0)
            {
                p = MidiUtilities.limit(p + octaveTransposition * 12);
                ne = ne.setPitch(p);
            }
            boolean useFstaff = p < G_STAFF_LOWEST_PITCH;
            int line = useFstaff ? F_STAFF_FIRST_LINE : G_STAFF_FIRST_LINE;
            ng.absoluteLine(line);

            var sn = mContext.buildScoreNote(ne, useFstaff, cs);
            sn.dur = NotationGraphics.NOTE_DURATION_QUARTER;
            sn.linedir = NotationGraphics.LINE_DIR_NO;
            sn.x = ng.getCurrentX() + shift;
            ng.drawNote(sn);

//            LOGGER.log(Level.SEVERE, "drawSamePositionNotes()     drawing {0} c={1}", new Object[]
//            {
//                ne, c
//            });
        }

    }

    /**
     * Try to restore transposition from song client properties, otherwise compute it from phrase
     */
    private void updateOctaveTransposition()
    {
        final int INVALID = -1000;
        int t = INVALID;
        if (song != null)
        {
            // Try to restore from song client property
            t = song.getClientProperties().getInt(buildClientPropNameOctave(editor.getChannel()), INVALID);
        }
        if (t == INVALID && !editor.getModel().isEmpty())
        {
            // Try from the phrase
            t = computeBestOctaveTransposition(editor.getModel());
        }
        if (t != INVALID)
        {
            setOctaveTransposition(t);
        }
    }

    private String buildClientPropNameOctave(int channel)
    {
        return CLIENT_PROP_OCTAVE_TRANSPOSITION_BASE + "#" + channel;
    }

    /**
     *
     * @param p Can not be empty
     * @return
     */
    private int computeBestOctaveTransposition(Phrase p)
    {
        Preconditions.checkArgument(!p.isEmpty());
        int res = 0;

        var stats = p.stream()
                .mapToInt(ne -> ne.getPitch())
                .summaryStatistics();
        var avg = stats.getAverage();

        if (avg < 43f)
        {
            res = (int) Math.floor((55 - avg) / 12);
        } else if (avg > 79f)
        {
            res = (int) Math.floor((avg - 67) / 12);
        }
        res = Ints.constrainToRange(res, -3, 3);

        return res;
    }


    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================    
    class PitchSortedNotes extends LinkedList<NoteEvent>
    {

        /**
         * Add a NoteEvent ordered by pitch.
         *
         * @param ne
         */
        public void addOrdered(NoteEvent ne)
        {
            boolean added = false;
            var it = listIterator();
            while (it.hasNext())
            {
                var next = it.next();
                if (ne.getPitch() == next.getPitch())
                {
                    // Special case, we should not normally have this (same pos and same pitch), just ignore the note
                    added = true;
                    break;
                } else if (ne.getPitch() < next.getPitch())
                {
                    it.add(ne);
                    added = true;
                    break;
                }
            }
            if (!added)
            {
                add(ne);
            }
        }
    }
}
