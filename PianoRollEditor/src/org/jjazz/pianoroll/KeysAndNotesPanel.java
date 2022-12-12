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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent.Orientation;
import org.jjazz.ui.keyboardcomponent.api.KeyboardRange;

/**
 * The panel representing the ruler, the keyboard on the left, and the notes on the right.
 */
public class KeysAndNotesPanel extends javax.swing.JPanel
{

    private NotesPanel notesPanel;
    private KeyboardComponent keyboard;
    private JPanel pnl_keynotes;
    private JPanel pnl_ruler;
    private JScrollPane scrollPane_keysAndNotes;

    private final PianoRollEditor editor;

    /**
     * Creates new form PianoRollPanel
     */
    public KeysAndNotesPanel(PianoRollEditor editor)
    {
        this.editor = editor;

        createUI();
    }

    /**
     * @param factor A value &gt; 0
     */
    public void setZoomY(float factor)
    {
        keyboard.setScaleFactor(factor, Math.min(1.5f, factor));
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================    
    private void addNotesText(KeyboardComponent kbd)
    {
        for (var key : kbd.getWhiteKeys())
        {
            int p = key.getPitch();
            if (p % 12 == 0)
            {
                int o = p / 12 - 1;
                key.setText("C" + o);
            }
        }
    }

    private void createUI()
    {
        keyboard = new KeyboardComponent(KeyboardRange._128_KEYS, Orientation.RIGHT, false);
        addNotesText(keyboard);        
        notesPanel = new NotesPanel(editor, keyboard);
        pnl_ruler = new RulerPanel(editor, notesPanel);        
        
        pnl_keynotes = new javax.swing.JPanel();
        pnl_keynotes.setLayout(new MyLayoutManager());                
        pnl_keynotes.add(keyboard);
        pnl_keynotes.add(notesPanel);        
        scrollPane_keysAndNotes = new javax.swing.JScrollPane();
        scrollPane_keysAndNotes.setViewportView(pnl_keynotes);
        
        setLayout(new java.awt.BorderLayout());
        add(pnl_ruler, java.awt.BorderLayout.NORTH);
        add(scrollPane_keysAndNotes, java.awt.BorderLayout.CENTER);
    }

    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================    

    /**
     * A simple LayoutManager for keyboard + notesPanel.
     * <p>
     * Layout the keyboard on the left at preferred size. NotesPanel reuses the same height than keyboard.
     */
    private class MyLayoutManager implements LayoutManager
    {

        @Override
        public void addLayoutComponent(String name, Component comp)
        {
            assert comp == keyboard || comp == notesPanel;
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
            // 
        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            return new Dimension(keyboard.getPreferredSize().width + notesPanel.getPreferredSize().width, keyboard.getPreferredSize().height);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return new Dimension(keyboard.getMinimumSize().width + notesPanel.getMinimumSize().width, keyboard.getMinimumSize().height);
        }

        @Override
        public void layoutContainer(Container parent)
        {
            assert parent == pnl_keynotes;
            keyboard.setLocation(0, 0);
            keyboard.setSize(keyboard.getPreferredSize());
            notesPanel.setLocation(keyboard.getWidth(), 0);
            notesPanel.setSize(notesPanel.getPreferredSize().width, keyboard.getHeight());
        }

    }


    // Variables declaration - do not modify                     
    // End of variables declaration                   
}
