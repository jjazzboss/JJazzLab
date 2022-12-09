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
package org.jjazz.pianoroll;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent;
import org.jjazz.ui.keyboardcomponent.api.PianoKey;
import org.jjazz.util.api.IntRange;

/**
 * Show the grid and hold the NoteViews.
 * <p>
 * Work in conjunction with a vertical KeyboardComponent on the left side which must be in the same enclosing panel.
 */
public class NotesPanel extends javax.swing.JPanel
{

    private final KeyboardComponent keyboard;
    private static final Logger LOGGER = Logger.getLogger(NotesPanel.class.getSimpleName());

    /**
     * Creates new form NotesPanel
     */
    public NotesPanel(KeyboardComponent keyboard)
    {
        this.keyboard = keyboard;
        this.keyboard.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                keyboardResized();
            }
        });

        initComponents();

        keyboardResized();
    }


    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);        // Honor the opaque property
        Graphics2D g2 = (Graphics2D) g;
        // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawHorizontalGrid(g2);
    }


    // ========================================================================================
    // Private methods
    // ========================================================================================
    private void drawHorizontalGrid(Graphics2D g2)
    {
        var settings = PianoRollEditorSettings.getDefault();
        Color cl1 = settings.getBarLineColor();
        var cl2 = Color.PINK;
        Color cb1 = settings.getBackgroundColor1();
        Color cb2 = settings.getBackgroundColor2();

        int w = getWidth();
        int h = getHeight();


        var wKey0 = keyboard.getKey(0); // first C
        var yRange0 = getYRange(wKey0);
        float adjustedLargeHeight = 24f * yRange0.size() / 32;
        var wKey1 = keyboard.getKey(12); // 2nd C
        var yRange1 = getYRange(wKey1);
        int octaveHeight = yRange0.to - yRange1.to + 1;
        float adjustedSmallHeight = (octaveHeight - 4 * adjustedLargeHeight) / 8;

        int y = yRange0.to;

        for (int i = 0; i < 127; i++)
        {
            Color c = (i % 2 == 0) ? cl1 : cl2;
            g2.setColor(c);

            int p = i % 12;
            float yUp = (p == 0 || p == 4 || p == 5 || p == 11) ? adjustedLargeHeight : adjustedSmallHeight;
            int y1 = y - Math.round(yUp);
            g2.fillRect(0, y1, w - 1, y - y1 + 1);
            y = y1;
        }

    }


    // =====================================================================================
    // Inner classes
    // =====================================================================================
    /**
     * Conversion methods between keyboard's Y coordinate, pitch and NoteView line's Y coordinate.
     */
    public class YMapper
    {

        /**
         * The keyboard height used for the last YMapper refresh.
         */
        private int lastKeyboardHeight = -1;
        private TreeMap<Integer, Integer> tmapPixelPitch = new TreeMap<>();


        /**
         * To be called when associated keyboard size has changed.
         * <p>
         * Recompute the internal tmapPixelPitch tree map.
         *
         * @param kbdHeight
         */
        private void refresh(int kbdHeight)
        {
            if (kbdHeight == lastKeyboardHeight)
            {
                return;
            }
            lastKeyboardHeight = kbdHeight;


            // Compute the large NoteView line height (for C, E, F, B) and the small NoteView line height (for the other notes)
            var wKeyC0 = keyboard.getKey(0); // first C
            var yRangeC0 = getKeyboardYRange(wKeyC0);
            float adjustedLargeHeight = 24f * yRangeC0.size() / 32;
            var wKeyC1 = keyboard.getKey(12); // 2nd C
            var yRangeC1 = getKeyboardYRange(wKeyC1);
            int octaveHeight = yRangeC0.to - yRangeC1.to + 1;
            float adjustedSmallHeight = (octaveHeight - 4 * adjustedLargeHeight) / 8;       // So we can accomodate 4 small + 4 large


            // Fill the tree map
            float y = yRangeC0.to;
            tmapPixelPitch.clear();
            for (int p = 0; p < 127; p++)
            {
                tmapPixelPitch.put(Math.round(y), p);
                int pp = p % 12;
                float yUp = (pp == 0 || pp == 4 || pp == 5 || pp == 11) ? adjustedLargeHeight : adjustedSmallHeight;
                y -= yUp;
            }
        }

        /**
         * Get the pitch corresponding to y coordinate.
         *
         * @param y
         * @return
         */
        public int getPitch(int y)
        {
            return tmapPixelPitch.ceilingEntry(y).getValue();
        }

        public IntRange getNoteViewChannelYRange(int pitch)
        {
            var entry = tmapPixelPitch.ceilingEntry(pitch)
        }

        /**
         * Get the Y range of the specified keyboard key.
         *
         * @param pitch
         * @return
         */
        public IntRange getKeyboardYRange(int pitch)
        {
            return getKeyboardYRange(keyboard.getKey(pitch));
        }

        /**
         * Get the Y range of the specified keyboard key.
         *
         * @param key
         * @return
         */
        public IntRange getKeyboardYRange(PianoKey key)
        {
            int yTop = key.getY();
            int yBottom = yTop + key.getHeight();
            return new IntRange(yTop, yBottom);
        }

    }

    /**
     * Conversion methods between NoteView X coordinate and position in beats.
     */
    public class XMapper
    {

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        setLayout(null);
    }// </editor-fold>//GEN-END:initComponents



    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
