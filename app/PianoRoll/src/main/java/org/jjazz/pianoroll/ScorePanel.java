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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.rhythmmusicgeneration.api.ChordSequence;
import org.jjazz.score.api.NotationGraphics;
import org.jjazz.score.api.MeasureContext;
import org.jjazz.score.api.NotationGraphics.ScoreNote;
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

    private final PianoRollEditor editor;
    private final NotesPanel notesPanel;
    private float scaleFactorX = 1;
    private int playbackPointX = -1;
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
        baseChordFontHeight = (int) Math.ceil(StringMetrics.create(baseChordFont).getHeight("I")); // include baseline

        // LOGGER.severe("ScorePanel() DEBUG Updating SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT");
        // System.setProperty(Note.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "%1$s p=%2$.1f");

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

//        LOGGER.log(Level.INFO, "paintComponent() -- width={0} height={1} clip={2}", new Object[]
//        {
//            getWidth(), getHeight(), g2.getClipBounds()
//        });

        // Make sure clip is restricted to a valid area
        g2.clipRect(0, 0, notesPanel.getWidth(), getHeight());
        var clip = g2.getClipBounds();


        var xMapper = notesPanel.getXMapper();
        var yMapper = notesPanel.getYMapper();
        if (!yMapper.isUptodate() || !xMapper.isUptodate() || clip.isEmpty())
        {
            //  Don't draw anything
            // LOGGER.severe("paintComponent() xMapper or yMapper is not uptodate, abort painting");
            return;
        }

        // Fill background
        Color c = editor.getSettings().getWhiteKeyLaneBackgroundColor();
        g2.setColor(c);
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);


        paintLoopZone(g2);
        

        notesPanel.paintVerticalGrid(g2, 0, getHeight() - 1);


        // Chord sequence
        var chordSequence = editor.getChordSequence();
        if (chordSequence != null)
        {
            // Important: we need all chord symbols from the start for paintNotes()
            IntRange brClip = xMapper.getBarRange(IntRange.ofX(clip));
            chordSequence = chordSequence.subSequence(brClip, false);
            paintChordSymbols(g2, chordSequence);
        }


        paintNotes(g2);


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
        Preconditions.checkArgument(xPos >= -1, "xPos=%s", xPos);

        int oldX = playbackPointX;
        playbackPointX = xPos;
        if (playbackPointX != oldX)
        {
            int x0, x1;
            if (oldX == -1)
            {
                x0 = playbackPointX - 1;
                x1 = playbackPointX + 1;
            } else if (playbackPointX == -1)
            {
                x0 = oldX - 1;
                x1 = oldX + 1;
            } else
            {
                x0 = Math.min(playbackPointX, oldX) - 1;
                x1 = Math.max(playbackPointX, oldX) + 1;
            }
            x0 = Math.max(0, x0);
            repaint(x0, 0, x1 - x0 + 1, getHeight());
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
                case PianoRollEditor.PROP_CHORD_SEQUENCE ->
                    repaint();
                case PianoRollEditor.PROP_SELECTED_NOTE_VIEWS ->
                {
                    var xMapper = notesPanel.getXMapper();
                    if (!xMapper.isUptodate())
                    {
                        repaint();
                    } else
                    {
                        // We can probably do a much smaller repaint
                        final int EXTRA = 100;   // Need to cover for note width, tie, shift, accidentals, ...
                        List<NoteView> nes = (List<NoteView>) evt.getOldValue();
                        NoteEvent neFirst = nes.get(0).getModel();
                        NoteEvent neLast = nes.get(nes.size() - 1).getModel();
                        int xFirst = Math.max(xMapper.getX(neFirst.getPositionInBeats()) - EXTRA, 0);
                        int xLast = Math.min(xMapper.getX(neLast.getPositionInBeats()) + EXTRA, getWidth() - 1);
                        repaint(xFirst, 0, xLast - xFirst + 1, getHeight());
                    }
                }
                case PianoRollEditor.PROP_MODEL_PHRASE ->
                    updateOctaveTransposition();
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

    /**
     * Paint the chord symbols.
     *
     * @param g2
     * @param chordSequence
     */
    private void paintChordSymbols(Graphics2D g2, ChordSequence chordSequence)
    {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float oneBeatPixelSize = notesPanel.getXMapper().getOneBeatPixelSize();

        // Draw chord symbols
        int lastCliCsBar = -1;

        for (var cliCs : chordSequence)
        {
            var posCliCs = cliCs.getPosition();

            if (oneBeatPixelSize > 14 || (posCliCs.getBar() != lastCliCsBar && posCliCs.isFirstBarBeat()))
            {
                // Draw the chord symbol
                var posInBeats = editor.toPhraseRelativeBeatPosition(cliCs);
                int x = editor.getXFromPosition(posInBeats);
                int y = baseChordFontHeight;
                AttributedString aStr;
                aStr = new AttributedString(cliCs.getData().getOriginalName(), baseChordFont.getAttributes());
                aStr.addAttribute(TextAttribute.FOREGROUND, COLOR_CHORD_SYMBOL_FONT);
                aStr.addAttribute(TextAttribute.BACKGROUND, editor.getSettings().getWhiteKeyLaneBackgroundColor());
                if (posCliCs.isFirstBarBeat())
                {
                    x += 1;
                }
                g2.drawString(aStr.getIterator(), x, y);
            } else
            {
                // No room to draw several chord symbols per bar
                continue;
            }

            lastCliCsBar = posCliCs.getBar();
        }
    }

    private void paintNotes(Graphics2D g2)
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


        // Paint notes         
        var allNoteEvents = computeClipImpactedNoteEvents(g2);
        var selectedNoteEvents = editor.getSelectedNoteEvents();
        var xMapper = notesPanel.getXMapper();
        var noteEventBuffer = new NoteEventBuffer(allNoteEvents.size());
        MeasureContext mContext = null;

        int i = 0;
        while (i < allNoteEvents.size())
        {
            var ne = allNoteEvents.get(i);
            noteEventBuffer.clear();
            noteEventBuffer.addOrdered(transpose(ne), selectedNoteEvents.contains(ne));  // single note by default
            i++;


            // Group all the next notes if they are at the same position
            while (i < allNoteEvents.size() && allNoteEvents.get(i).isNear(ne.getPositionInBeats(), 0.1f))
            {
                var samePosNe = allNoteEvents.get(i);
                noteEventBuffer.addOrdered(transpose(samePosNe), selectedNoteEvents.contains(samePosNe));
                i++;
            }


            // Update the MeasureContext
            float neBeatPos = ne.getPositionInBeats();
            Position nePos = xMapper.getPosition(neBeatPos);
            TimeSignature ts = editor.getTimeSignature(neBeatPos);
            if (mContext == null || mContext.getBarIndex() != nePos.getBar())
            {
                float barStartBeatPos = nePos.isFirstBarBeat() ? neBeatPos : xMapper.getBeatPosition(new Position(nePos.getBar(), 0));
                FloatRange beatRange = new FloatRange(barStartBeatPos, barStartBeatPos + ts.getNbNaturalBeats() - 0.0001f);
                mContext = new MeasureContext(ng, nePos.getBar(), beatRange, new Note(60));
            }


            // Paint a single note or a set of chord notes at (more or less) same position
            int x = xMapper.getX(neBeatPos);
            ChordSymbol cs = findChordSymbol(nePos, ts);
            paintChordNotes(noteEventBuffer, x, mContext, cs);

        }

    }

    /**
     * Paint one ScoreNote.
     *
     * @param sn
     * @param c
     */
    private void paintOneScoreNote(ScoreNote sn, Color c)
    {
        final int ADJACENT_NOTE_SHIFT = (int) ng.getNoteBaseWidth();

        int line = sn.isFstaff ? F_STAFF_FIRST_LINE : G_STAFF_FIRST_LINE;
        ng.absoluteLine(line);

        sn.dur = NotationGraphics.NOTE_DURATION_QUARTER;
        sn.linedir = NotationGraphics.LINE_DIR_NO;
        int shift = sn.lateralShift == 1 ? ADJACENT_NOTE_SHIFT : 0;
        sn.dotted = 0;
        sn.x = ng.getCurrentX() + shift;

        ng.getGraphics().setColor(c);
        ng.drawNote(sn);
    }

    /**
     * Paint one or more notes at same position.
     *
     * @param neBuffer
     * @param x x coordinate where to draw the notes
     * @param mContext
     * @param cs Optional chord symbol
     */
    private void paintChordNotes(NoteEventBuffer neBuffer, int x, MeasureContext mContext, ChordSymbol cs)
    {
        Preconditions.checkArgument(!neBuffer.isEmpty());
//        LOGGER.log(Level.INFO, "paintChordNotes() -- nes={0} mContext.barIndex={1}", new Object[]
//        {
//            new ArrayList<>(chordNotes), mContext.getBarIndex()
//        });

        ng.absoluteX(x);


        List<ScoreNote> scoreNotes;
        if (neBuffer.size() == 1)
        {
            scoreNotes = List.of(mContext.buildScoreNote(neBuffer.get(0), cs));
        } else
        {
            scoreNotes = mContext.buildChordScoreNotes(neBuffer, cs);
        }

        for (int i = 0; i < neBuffer.size(); i++)
        {
            var n = neBuffer.get(i);
            var sn = scoreNotes.get(i);
            Color c = neBuffer.isSelected(n) ? Color.RED.brighter() : Color.BLACK;
            paintOneScoreNote(sn, c);
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

    /**
     * Compute which notes must be painted depending on clip.
     * <p>
     * We need to paint ALL the notes from the bar corresponding to clip.x, so that MeasureContext works consistently across repaints (otherwise staff
     * line/accidentals may vary). Also add some extra room on the left and right of the clip to possibly render accidentals of first-beat notes and
     * of notes which start right after the clip.
     *
     * @param g2
     * @return
     */
    private List<NoteEvent> computeClipImpactedNoteEvents(Graphics2D g2)
    {
        List<NoteEvent> res;

        var clip = g2.getClipBounds();
        // Make sure we don't go beyond notesPanel. Might be eeded when notesPanel is max zoomed out (smaller than enclosing viewport)
        SwingUtilities.computeIntersection(0, 0, notesPanel.getWidth(), getHeight(), clip);

        if (!clip.isEmpty())
        {
            var xMapper = notesPanel.getXMapper();
            var clipLastX = clip.x + clip.width - 1;

            var fromPos = xMapper.getPositionFromX(clip.x);
            fromPos.setFirstBarBeat().setBar(Math.max(0, fromPos.getBar() - 1));
            var fromPosInBeats = xMapper.getBeatPosition(fromPos);

            var toPosInBeats = xMapper.getBeatPosition(clipLastX);
            toPosInBeats = editor.getPhraseBeatRange().clamp(toPosInBeats + 4f, 0.001f);

            var br = new FloatRange(fromPosInBeats, toPosInBeats);
            Predicate<NoteEvent> tester = ne ->
            {
                var brNe = ne.getBeatRange();
                return brNe.to <= br.to && br.intersects(brNe);
            };
            res = editor.getModel().getNotes(tester, FloatRange.MAX_FLOAT_RANGE, true);

        } else
        {
            res = new ArrayList<>();
        }

        return res;
    }

    private NoteEvent transpose(NoteEvent ne)
    {
        return octaveTransposition == 0 ? ne : ne.setPitch(MidiConst.clamp(ne.getPitch() + octaveTransposition * 12), false);
    }

    /**
     * Get the active ChordSymbol at specified pos.
     * <p>
     * Manage the case of a real time played note for which the relevant chord symbol is actually after the note. For example in a 4/4 bar, the note
     * at pos=[bar=0,beat=3.98] must be linked to the chord symbol at start of bar 1.
     *
     * @param pos
     * @param ts The TimeSignature at pos
     * @return
     */
    private ChordSymbol findChordSymbol(Position pos, TimeSignature ts)
    {
        ChordSymbol res = null;
        var cSeq = editor.getChordSequence();

        if (cSeq != null)
        {
            pos = pos.getMoved(editor.getRulerStartBar(), 0);
            if (ts.isEndOfBar(pos.getBeat(), 0.13f))
            {
                // This position is most probably played in real time and should be attached to the chord of next bar.
                pos = pos.getNextBarStart();
            }
            var cliCs = cSeq.getLastBefore(pos, true, cli -> true);
            res = cliCs != null ? cliCs.getData() : null;
        }
        return res;
    }

// ==========================================================================================================
// Inner classes
// ==========================================================================================================    
    /**
     * Store notes ordered per ascending pitch.
     * <p>
     * Also store the selected state of each note.
     */
    private class NoteEventBuffer extends ArrayList<NoteEvent>
    {

        private final Set<NoteEvent> selectedNotes = new HashSet<>();

        private NoteEventBuffer(int size)
        {
            super(size);
        }

        /**
         * Add a Note ordered by pitch.
         *
         * @param ne
         * @param isSelected
         */
        public void addOrdered(NoteEvent ne, boolean isSelected)
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
            if (isSelected)
            {
                selectedNotes.add(ne);
            }

        }

        public boolean isSelected(NoteEvent ne)
        {
            return selectedNotes.contains(ne);
        }

    }
}
