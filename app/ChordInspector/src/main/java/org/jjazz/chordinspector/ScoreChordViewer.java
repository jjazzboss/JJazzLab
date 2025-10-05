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
package org.jjazz.chordinspector;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import org.jjazz.chordinspector.spi.ChordViewer;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.Note.Accidental;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.score.api.NotationGraphics;
import org.jjazz.score.api.MeasureContext;
import org.jjazz.utilities.api.FloatRange;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.lookup.ServiceProvider;

/**
 * A ChordViewer based on musical notation.
 */
@ServiceProvider(service = ChordViewer.class, position = 300)
public class ScoreChordViewer extends javax.swing.JPanel implements ChordViewer
{

    @StaticResource(relative = true)
    final private static String ICON_PATH = "resources/EighthNoteIcon.png";
    final private static Icon ICON = new ImageIcon(ScoreChordViewer.class.getResource(ICON_PATH));

    private ExtChordSymbol model;
    private final NotationGraphics ng;
    private static final Logger LOGGER = Logger.getLogger(ScoreChordViewer.class.getSimpleName());

    /**
     * Creates new form PianoChordViewer
     */
    public ScoreChordViewer()
    {
        ng = new NotationGraphics();
        ng.setSize(30f);
        initComponents();
        // setOpaque(false);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);   // honor the opaque property


        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        ng.setGraphics(g2);

        // Draw staff
        ng.absolute(2);
        ng.absoluteLine(14);
        ng.drawStaff(2048.0f);
        ng.drawBarLine();

        // G clef
        ng.relative(1);
        ng.drawClef(NotationGraphics.CLEF_G);


        if (model == null)
        {
            return;
        }


        // Draw chord notes
        MeasureContext mContext = new MeasureContext(ng, 0, new FloatRange(0, 4), new Note(0));
        ng.relative(8);
        ExtChordSymbol ecs = getModel();
        ng.startNoteGroup();
        var chord = ecs.getChord();     // chord notes accidentals are made consistent with the chord symbol
        int t = chord.getMinPitch() < 9 ? 60 : 48;
        chord.transpose(t);
        for (Note n : chord.getNotes())
        {
            var ne = new NoteEvent(n, 0);
            var sn = mContext.buildScoreNote(ne, null);
            sn.x = ng.getCurrentX();
            ng.drawNote(sn);
        }
        ng.endNoteGroup();


        // Scale
        var ssi = ecs.getRenderingInfo().getScaleInstance();
        if (ssi != null)
        {
            ng.relative(4);

            Accidental defaultAlt = ecs.getDefaultAccidental();

            var notes = ssi.getNotes();
            Note firstNote = chord.getNote(0);
            t = firstNote.getPitch() - notes.get(0).getPitch();
            Note previousNote = null;
            for (Note n : ssi.getNotes())
            {
                Note nn = new Note(n.getPitch() + t, 1f, 64, defaultAlt);
                int line = nn.getGStaffLineNumber();
                int accidental = 0;
                if (!Note.isWhiteKey(nn.getPitch()))
                {
                    accidental = nn.isFlat() ? NotationGraphics.ACCIDENTAL_FLAT : NotationGraphics.ACCIDENTAL_SHARP;
                } else if (previousNote != null
                        && line == previousNote.getGStaffLineNumber()
                        && !NoteEvent.isWhiteKey(previousNote.getPitch()))
                {
                    // Need to add a natural accidental 
                    accidental = NotationGraphics.ACCIDENTAL_NATURAL;
                }
                float relAdvance = accidental == 0 ? 3f : 4f;
                ng.relative(relAdvance);
                ng.drawNote(line - 2, 0, 0, accidental);
                previousNote = nn;
            }
        }
    }


    // ===================================================================================
    // ChordViewer interface
    // ===================================================================================
    @Override
    public JComponent getComponent()
    {
        return this;
    }

    @Override
    public String getDescription()
    {
        return "Score";
    }

    @Override
    public Icon getIcon()
    {
        return ICON;
    }

    @Override
    public void setContext(Song song, MidiMix midiMix, RhythmVoice rv)
    {
        // Nothing
    }

    @Override
    public void setModel(ExtChordSymbol ecs)
    {
        this.model = ecs;
        repaint();
    }

    @Override
    public ExtChordSymbol getModel()
    {
        return model;
    }

    @Override
    public void cleanup()
    {
        // Nothing
    }

    // ===================================================================================
    // Private methods
    // ===================================================================================

   
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents



    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
