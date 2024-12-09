package org.jjazz.score;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jjazz.score.api.NotationGraphics;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_DEMIFLAT;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_DEMISHARP;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_DOUBLE_FLAT;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_FLAT;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_FLAT_AND_A_HALF;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_NATURAL;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_NO;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_SHARP;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_SHARP_AND_A_HALF;
import static org.jjazz.score.api.NotationGraphics.LINE_DIR_DOWN;
import static org.jjazz.score.api.NotationGraphics.LINE_DIR_NO;
import static org.jjazz.score.api.NotationGraphics.LINE_DIR_UP;
import static org.jjazz.score.api.NotationGraphics.NOTE_DURATION_EIGHTH;
import static org.jjazz.score.api.NotationGraphics.NOTE_DURATION_HALF;
import static org.jjazz.score.api.NotationGraphics.NOTE_DURATION_QUARTER;
import static org.jjazz.score.api.NotationGraphics.NOTE_DURATION_SIXTEENTH;
import static org.jjazz.score.api.NotationGraphics.NOTE_DURATION_SIXTEENTH2;
import static org.jjazz.score.api.NotationGraphics.NOTE_DURATION_WHOLE;
import static org.jjazz.score.api.NotationGraphics.NOTE_DURATION_WHOLE2;
import static org.jjazz.score.api.NotationGraphics.NOTE_DURATION_WHOLE4;
import org.jjazz.score.api.NotationGraphics.ScoreNote;


public class TestScore extends JPanel
{

    private static final long serialVersionUID = 1L;

    public static void main(String[] args)
    {

        JFrame frame = new JFrame();
        frame.setSize(1024, 800);
        frame.setLocation(100, 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        TestScore s = new TestScore();
        s.setLayout(new BorderLayout());

        frame.add(s);
        frame.setVisible(true);
    }

    public TestScore()
    {
        setOpaque(false);
        ng.setSize(30);

    }

    NotationGraphics ng = new NotationGraphics();

    @Override
    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 40, 2048, 600);
        g2.setColor(Color.BLACK);
        ng.setGraphics(g2);

        ng.absolute(2);
        ng.absoluteLine(14);
        ng.drawStaff(2048.0f);
        ng.drawBarLine();

        ng.absolute(3);
        g2.setColor(Color.RED);
        ng.drawBarLine();
        g2.setColor(Color.BLACK);

        ng.drawClef(NotationGraphics.CLEF_F);

        ng.relative(3);
        g2.setColor(Color.RED);
        ng.drawBarLine();
        g2.setColor(Color.BLACK);

        ng.relative(1);
        ng.drawTimeSignature(3, 4);

        ng.absolute(13);
        g2.setColor(Color.RED);
        ng.drawBarLine();
        g2.setColor(Color.BLACK);

        ng.drawClef(NotationGraphics.CLEF_G);

        ng.relative(3);
        g2.setColor(Color.RED);
        ng.drawBarLine();
        g2.setColor(Color.BLACK);


        ng.absolute(23);
        g2.setColor(Color.RED);
        ng.drawBarLine();
        g2.setColor(Color.BLACK);
        ng.drawClef(NotationGraphics.CLEF_C);

        ng.relative(3);
        g2.setColor(Color.RED);
        ng.drawBarLine();
        g2.setColor(Color.BLACK);


        ng.absolute(33);
        g2.setColor(Color.RED);
        ng.drawBarLine();
        g2.setColor(Color.BLACK);
        ng.drawClef(NotationGraphics.CLEF_NEUTRAL);

        ng.relative(3);
        g2.setColor(Color.RED);
        ng.drawBarLine();
        g2.setColor(Color.BLACK);

        ng.absolute(43);
        g2.setColor(Color.RED);
        ng.drawBarLine();
        g2.setColor(Color.BLACK);
        ng.drawClef(NotationGraphics.CLEF_TAB);

        ng.relative(3);
        g2.setColor(Color.RED);
        ng.drawBarLine();
        g2.setColor(Color.BLACK);


        ng.drawNote(0, NOTE_DURATION_EIGHTH);
        ng.relative(3);
        ng.drawNote(2, NOTE_DURATION_EIGHTH);

        ng.relative(3);
        ng.drawNote(0, NOTE_DURATION_EIGHTH, 1);
        ng.relative(3);
        ng.drawNote(2, NOTE_DURATION_SIXTEENTH, 0);

        ng.startNoteGroup();
        ng.relative(3);
        ng.drawNote(0, NOTE_DURATION_EIGHTH);
        ng.relative(3);
        ng.drawNote(2, NOTE_DURATION_EIGHTH);

        ng.relative(3);
        ng.drawNote(0, NOTE_DURATION_EIGHTH, 1);
        ng.relative(3);
        ng.drawNote(2, NOTE_DURATION_SIXTEENTH, 0);
        ng.endNoteGroup();


        ng.absolute(2);
        ng.absoluteLine(24);
        ng.drawStaff(2048.0f);
        ng.drawBarLine();
        ng.relative(1);
        ng.drawClef(NotationGraphics.CLEF_G); // Draw G-Clef
        ng.relative(4);
        ng.drawTimeSignature(0);
        //ng.drawTimeSignature(4, 4);
        ng.relative(6);

        ng.startNoteGroup();

        ng.drawNote(1, NOTE_DURATION_QUARTER);
        ng.relative(3);
        ng.drawNote(6, NOTE_DURATION_QUARTER);

        ng.relative(3);
        ng.drawNote(6, NOTE_DURATION_WHOLE4);
        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_WHOLE2);
        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_WHOLE);
        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_HALF);
        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_QUARTER);
        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_EIGHTH);
        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_SIXTEENTH);

        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_QUARTER);
        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_EIGHTH);
        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_SIXTEENTH);

        ng.relative(3);
        ng.drawNote(0, NOTE_DURATION_QUARTER);
        ng.drawNote(2, NOTE_DURATION_QUARTER);
        ng.drawNote(4, NOTE_DURATION_EIGHTH);

        ng.relative(3);
        ng.drawNote(-1, NOTE_DURATION_EIGHTH);
        ng.relative(3);
        ng.drawNote(-2, NOTE_DURATION_EIGHTH);
        ng.relative(3);
        ng.drawNote(-3, NOTE_DURATION_EIGHTH);
        ng.relative(3);
        ng.drawNote(-4, NOTE_DURATION_EIGHTH);
        ng.relative(3);


        ng.drawNote(12, NOTE_DURATION_EIGHTH, 0);
        ng.relative(3);
        ng.drawNote(10, NOTE_DURATION_EIGHTH, 1);
        ng.relative(3);
        ng.drawNote(9, NOTE_DURATION_EIGHTH, 2);


        ng.relative(3);

        ng.relative(3);
        ng.drawRest(-2);
        ng.relative(3);
        ng.drawRest(-1);
        ng.relative(3);
        ng.drawRest(0);
        ng.relative(3);
        ng.drawRest(1);
        ng.relative(3);
        ng.drawRest(2);
        ng.relative(3);
        ng.drawRest(3);
        ng.relative(3);
        ng.drawRest(4);
        ng.relative(3);
        ng.drawRest(5);
        ng.relative(3);
        ng.drawRest(6);
        ng.relative(3);
        ng.drawRest(7);

        //g2.setColor(Color.RED);
        //ng.drawBarLine();

        //ng.drawBarLine(2);
        //ng.relative(3);
        //ng.drawBarLine(0);
        ng.endNoteGroup();

        ng.absolute(2);
        ng.absoluteLine(34);
        ng.drawStaff(2048.0f);
        ng.drawBarLine();
        ng.relative(1);
        ng.drawClef(NotationGraphics.CLEF_G); // Draw G-Clef

        Font romanfont = new Font("Times new roman", Font.BOLD, 16).deriveFont(ng.getGridSize() * 2f);
        g2.setFont(romanfont);
        g2.drawString("Piano track 1", ng.getCurrentX(), ng.getCurrentY() - ng.getGridSize() * 6.5f);

        ng.relative(4);

        //ng.relative(ng.drawSharpKeySignature(8,5,9,6,3,7,4)+1);		
        ng.relative(ng.drawSharpKeySignature(8, 5, 9) + 1);

        ng.drawTimeSignature(4, 4);
        ng.relative(6);

        ng.drawNote(1, NOTE_DURATION_QUARTER, 0, ACCIDENTAL_DOUBLE_FLAT);
        ng.relative(6);
        ng.drawNote(1, NOTE_DURATION_QUARTER, 0, ACCIDENTAL_FLAT_AND_A_HALF);
        ng.relative(6);
        ng.drawNote(1, NOTE_DURATION_QUARTER, 0, ACCIDENTAL_FLAT);
        ng.relative(6);
        ng.drawNote(1, NOTE_DURATION_QUARTER, 0, ACCIDENTAL_DEMIFLAT);
        ng.relative(6);
        ng.drawNote(1, NOTE_DURATION_QUARTER, 0, ACCIDENTAL_NATURAL);
        ng.relative(6);
        ng.drawNote(1, NOTE_DURATION_QUARTER, 0, ACCIDENTAL_NO);
        ng.relative(6);
        ng.drawNote(1, NOTE_DURATION_QUARTER, 0, ACCIDENTAL_DEMISHARP);
        ng.relative(6);
        ScoreNote n1 = ng.drawNote(1, NOTE_DURATION_QUARTER, 0, ACCIDENTAL_SHARP);
        ng.relative(6);
        ScoreNote n2 = ng.drawNote(1, NOTE_DURATION_QUARTER, 0, ACCIDENTAL_SHARP_AND_A_HALF);
        ng.drawNoteTie(n1, n2);

        ng.relative(6);
        ng.drawNote(2, NOTE_DURATION_QUARTER, 0, 200);
        ng.relative(6);

        ng.startNoteGroup();
        ng.drawNote(2, NOTE_DURATION_EIGHTH, 0, ACCIDENTAL_NO, 0, LINE_DIR_NO);
        ng.drawNote(3, NOTE_DURATION_EIGHTH, 0, ACCIDENTAL_NO, 0, LINE_DIR_NO);
        ng.drawNote(6, NOTE_DURATION_EIGHTH, 0, ACCIDENTAL_NO, 0, LINE_DIR_NO);;
        ng.relative(4);
        ng.endNoteGroup();

        // Same but without NoteGroup
        ng.drawNote(2, NOTE_DURATION_EIGHTH, 0, ACCIDENTAL_NO, 0, LINE_DIR_NO);
        ng.drawNote(3, NOTE_DURATION_EIGHTH, 0, ACCIDENTAL_NO, 0, LINE_DIR_NO);
        ng.drawNote(6, NOTE_DURATION_EIGHTH, 0, ACCIDENTAL_NO, 0, LINE_DIR_NO);
        ng.relative(4);

        ng.startNoteGroup();
        ng.drawNote(2, NOTE_DURATION_QUARTER);
        ng.drawNote(4, NOTE_DURATION_QUARTER);
        ng.drawNote(6, NOTE_DURATION_QUARTER);
        ng.relative(4);
        ng.endNoteGroup();

        ng.startNoteGroup();
        ng.drawNote(2, NOTE_DURATION_HALF);
        ng.drawNote(5, NOTE_DURATION_HALF);
        ng.drawNote(6, NOTE_DURATION_HALF);
        ng.relative(4);
        ng.endNoteGroup();

        ng.startNoteGroup();
        ng.drawNote(2, NOTE_DURATION_WHOLE);
        ng.drawNote(4, NOTE_DURATION_WHOLE);
        ng.drawNote(6, NOTE_DURATION_WHOLE);
        ng.relative(3);
        ng.endNoteGroup();


        ng.startNoteGroup();
        ng.drawNote(2, NOTE_DURATION_EIGHTH, 0, ACCIDENTAL_NO, 0, LINE_DIR_UP);
        ng.drawNote(4, NOTE_DURATION_EIGHTH, 0, ACCIDENTAL_NO, 0, LINE_DIR_UP);
        ng.drawNote(6, NOTE_DURATION_EIGHTH, 0, ACCIDENTAL_NO, 0, LINE_DIR_UP);
        ng.relative(4);
        ng.endNoteGroup();

        ng.startNoteGroup();
        ng.drawNote(8, NOTE_DURATION_EIGHTH);
        ng.relative(3);
        ng.drawNote(5, NOTE_DURATION_SIXTEENTH);
        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_SIXTEENTH);
        ng.relative(3);
        ng.endNoteGroup();


        ng.startNoteGroup();
        // ng.drawNote(0,  NOTE_DURATION_QUARTER).mark = NotationGraphics.ARTICULATION_MARK_OPEN_NOTE;       // seems not implemented
        ng.drawNote(0, NOTE_DURATION_QUARTER, 0, ACCIDENTAL_NO, NotationGraphics.ARTICULATION_MARK_ACCENT);     // seems not implemented
        ng.relative(3);
        ng.drawNote(0, NOTE_DURATION_EIGHTH);
        ng.relative(3);
        ng.drawNote(2, NOTE_DURATION_SIXTEENTH).mark = NotationGraphics.ORNAMENT_MARK_TRILL;          // seems not implemented
        ng.relative(3);
        ng.drawNote(6, NOTE_DURATION_SIXTEENTH2).mark = NotationGraphics.ORNAMENT_MARK_TURN;           // seems not implemented
        ng.relative(3);
        ng.drawNote(4, NOTE_DURATION_HALF);
        ng.relative(3);
        ng.endNoteGroup();

        ng.relative(3);

        ng.startNoteGroup();
        ng.drawNote(4, NOTE_DURATION_SIXTEENTH, 0, ACCIDENTAL_SHARP);
        ng.drawNote(5, NOTE_DURATION_SIXTEENTH);
        ng.drawNote(8, NOTE_DURATION_SIXTEENTH);
        ng.endNoteGroup();

        ng.relative(6);

        ng.startNoteGroup();
        ng.drawNote(-1, NOTE_DURATION_SIXTEENTH);
        ng.drawNote(0, NOTE_DURATION_SIXTEENTH, 0, ACCIDENTAL_NATURAL);
        ng.drawNote(4, NOTE_DURATION_SIXTEENTH);
        ng.endNoteGroup();

        ng.relative(6);

        ng.startNoteGroup();
        ng.drawNote(2, NOTE_DURATION_SIXTEENTH);
        ng.drawNote(3, NOTE_DURATION_SIXTEENTH, 0, ACCIDENTAL_FLAT);
        ng.drawNote(6, NOTE_DURATION_SIXTEENTH, 0, ACCIDENTAL_FLAT);
        ng.endNoteGroup();
    }

}
