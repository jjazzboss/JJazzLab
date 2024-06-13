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

        ng.drawNote(1, 2);
        ng.relative(3);
        ng.drawNote(6, 2);

        ng.relative(3);
        ng.drawNote(6, -2);
        ng.relative(3);
        ng.drawNote(4, -1);
        ng.relative(3);
        ng.drawNote(4, 0);
        ng.relative(3);
        ng.drawNote(4, 1);
        ng.relative(3);
        ng.drawNote(4, 2);
        ng.relative(3);
        ng.drawNote(4, 3);
        ng.relative(3);
        ng.drawNote(4, 4);

        ng.relative(3);
        ng.drawNote(4, 2);
        ng.relative(3);
        ng.drawNote(4, 3);
        ng.relative(3);
        ng.drawNote(4, 4);

        ng.relative(3);
        ng.drawNote(0, 2);
        ng.drawNote(2, 2);
        ng.drawNote(4, 3);

        ng.relative(3);
        ng.drawNote(-1, 3);
        ng.relative(3);
        ng.drawNote(-2, 3);
        ng.relative(3);
        ng.drawNote(-3, 3);
        ng.relative(3);
        ng.drawNote(-4, 3);
        ng.relative(3);


        ng.drawNote(12, 3, 0);
        ng.relative(3);
        ng.drawNote(10, 3, 1);
        ng.relative(3);
        ng.drawNote(9, 3, 2);


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

        ng.drawNote(1, 2, 0, -200);
        ng.relative(6);
        ng.drawNote(1, 2, 0, -150);
        ng.relative(6);
        ng.drawNote(1, 2, 0, -100);
        ng.relative(6);
        ng.drawNote(1, 2, 0, -50);
        ng.relative(6);
        ng.drawNote(1, 2, 0, NotationGraphics.ACCIDENTAL_NATURAL);
        ng.relative(6);
        ng.drawNote(1, 2, 0, 0);
        ng.relative(6);
        ng.drawNote(1, 2, 0, 50);
        ng.relative(6);
        ScoreNote n1 = ng.drawNote(1, 2, 0, 100);
        ng.relative(6);
        ScoreNote n2 = ng.drawNote(1, 2, 0, 150);
        ng.drawNoteTie(n1, n2);

        ng.relative(6);
        ng.drawNote(2, 2, 0, 200);
        ng.relative(6);

        ng.startNoteGroup();
        ng.drawNote(2, 3);
        ng.drawNote(4, 3);
        ng.drawNote(6, 3);
        ng.relative(3);
        ng.endNoteGroup();

        ng.startNoteGroup();
        ng.drawNote(2, 2);
        ng.drawNote(4, 2);
        ng.drawNote(6, 2);
        ng.relative(3);
        ng.endNoteGroup();

        ng.startNoteGroup();
        ng.drawNote(2, 1);
        ng.drawNote(4, 1);
        ng.drawNote(6, 1);
        ng.relative(3);
        ng.endNoteGroup();

        ng.startNoteGroup();
        ng.drawNote(2, 0);
        ng.drawNote(4, 0);
        ng.drawNote(6, 0);
        ng.relative(3);
        ng.endNoteGroup();


        ng.startNoteGroup();
        ng.drawNote(2, 3, 0, 0, 0, 1);
        ng.drawNote(4, 3, 0, 0, 0, 1);
        ng.drawNote(6, 3, 0, 0, 0, 1);
        ng.relative(3);
        ng.endNoteGroup();

        ng.startNoteGroup();
        ng.drawNote(8, 3);
        ng.relative(3);
        ng.drawNote(5, 4);
        ng.relative(3);
        ng.drawNote(4, 4);
        ng.relative(3);
        ng.endNoteGroup();


        ng.startNoteGroup();
        ng.drawNote(0, 2).mark = NotationGraphics.ARTICULATION_MARK_OPEN_NOTE;
        ng.relative(3);
        ng.drawNote(0, 3);
        ng.relative(3);
        ng.drawNote(2, 4).mark = NotationGraphics.ORNAMENT_MARK_TRILL;
        ng.relative(3);
        ng.drawNote(6, 5).mark = NotationGraphics.ORNAMENT_MARK_TURN;
        ng.relative(3);
        ng.drawNote(4, 1);
        ng.relative(3);
        ng.endNoteGroup();

        ng.relative(3);

        ng.startNoteGroup();
        ng.drawNote(8, 4);
        ng.drawNote(5, 4);
        ng.drawNote(4, 4, 0, 100);
        ng.endNoteGroup();

        ng.relative(6);

        ng.startNoteGroup();
        ng.drawNote(-1, 4);
        ng.drawNote(0, 4, 0, 100);
        ng.drawNote(4, 4);
        ng.endNoteGroup();


    }

}
