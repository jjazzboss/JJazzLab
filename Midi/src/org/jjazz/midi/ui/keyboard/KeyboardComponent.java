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
package org.jjazz.midi.ui.keyboard;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import org.jjazz.harmony.Chord;
import org.jjazz.harmony.Note;

/**
 * A JPanel representing a Piano keyboard with selectable keys.
 * <p>
 * Pressed notes can be represented with their velocity.
 * <p>
 */
public class KeyboardComponent extends JPanel implements MouseListener
{

    private Size kbdSize;

    private List<PianoKey> pianoKeys = new ArrayList<>();

    /**
     * Create a component with 88 notes.
     * <p>
     */
    public KeyboardComponent()
    {
        this(Size._88_KEYS);
    }

    /**
     *
     * @param sz KbdSize
     */
    public KeyboardComponent(Size sz)
    {

        // Add PianoKeys and register them
        kbdSize = sz;
        for (int i = kbdSize.getLowestPitch(); i <= kbdSize.getHighestPitch(); i++)
        {
            boolean leftmost = (i == kbdSize.getLowestPitch());
            boolean rightmost = (i == kbdSize.getHighestPitch());
            PianoKey key = new PianoKey(i, leftmost, rightmost);
            add(key);

            // Listen to the mouse clickes
            key.addMouseListener(this);
        }

        // Set preferred size
        Dimension newDimension = new Dimension(getWidthFromHeight(PianoKey.WH), PianoKey.WH);
        setPreferredSize(newDimension);
        setMinimumSize(new Dimension(kbdSize.getNbWhiteKeys() * PianoKey.WW_MIN, PianoKey.WH_MIN));
    }

    @Override
    public String toString()
    {
        return "Keyboard[" + kbdSize + "]";
    }

    public Size getKbdSize()
    {
        return kbdSize;
    }

    /**
     * Get the PianoKey for specified pitch.
     *
     * @param pitch
     * @return
     */
    public PianoKey getPianoKey(int pitch)
    {
        kbdSize.checkPitch(pitch);
        return pianoKeys.get(pitch - kbdSize.lowPitch);
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        for (PianoKey c : pianoKeys)
        {
            c.setEnabled(b);
        }
    }

    /**
     * Set all keys unpressed.
     */
    public void releaseAllNotes()
    {
        pianoKeys.forEach(pk -> pk.pressNote(0));
    }

    /**
     * Set colors used to draw all the keys of the keyboard.
     * <p>
     * If one of the color argument is null, we don't change this color.
     *
     * @param wKey Color of a white key not pressed.
     * @param wPressedKey Color of a white key pressed.
     * @param bKey Color of a black key not pressed.
     * @param bPressedKey Color of a black key pressed.
     * @param contour Color of a key contour when not selected.
     * @param selectedContour Color of a key contour when selected.
     */
    public void setColors(Color wKey, Color wPressedKey, Color bKey, Color bPressedKey, Color contour, Color selectedContour)
    {
        pianoKeys.forEach(pk -> pk.setColors(wKey, wPressedKey, bKey, bPressedKey, contour, selectedContour));
    }

    /**
     * Set the pressed status of specified key.
     * <p>
     * Method delegates to setVelocity() of the relevant PianoKey.
     *
     * @param pitch
     * @param velocity If 0 it means the key must be released.
     */
    public void pressNote(int pitch, int velocity)
    {
        kbdSize.checkPitch(pitch);
        if (!Note.checkVelocity(velocity))
        {
            throw new IllegalArgumentException("pitch=" + pitch + " velocity=" + velocity);
        }
        getPianoKey(pitch).pressNote(velocity);
    }

    /**
     * Get the pressed status of a specific key.
     * <p>
     * Method delegates to getVelocity() of the relevant PianoKey.
     *
     * @param pitch The pitch of the key.
     *
     * @return If 0 it means the key is released.
     */
    public int getVelocity(int pitch)
    {
        kbdSize.checkPitch(pitch);
        return getPianoKey(pitch).getVelocity();
    }

    /**
     * Return the chord shown by the pressed notes.
     *
     * @return A Chord object.
     */
    public Chord getChord()
    {
        Chord c = new Chord();
        pianoKeys.stream()
                .filter(pk -> pk.getVelocity() > 0)
                .forEach(pk -> c.add(new Note(pk.getPitch(), 1.0f, pk.getVelocity())));
        return c;
    }

    /**
     * Get the PianoKey that correspond to a specific point.
     *
     * @param p A Point object relative to this component.
     *
     * @return Can be null.
     */
    public PianoKey getPianokey(Point p)
    {
        Component c = this.getComponentAt(p.x, p.y);
        if (c instanceof PianoKey)
        {
            return (PianoKey) c;
        }
        return null;
    }

    /**
     * Layout the keys to fit the size.
     * <p>
     * Because of integer rounding errors, it may not fit exactly the required dimensions. The keyboard is centered inside the
     * box.
     */
    @Override
    public void doLayout()
    {
        Insets in = getInsets();
        Rectangle r = new Rectangle(in.left, in.top, getWidth() - in.left - in.right,
                getHeight() - in.top - in.bottom);

        // Size of a white key.
        int wKeyWidth = (r.width - 1) / kbdSize.getNbWhiteKeys();
        int wKeyHeight = (r.height - 1);

        // Calculate X so the keyboard will be centered (because of integer rounding we may have differences
        // between the object size and the real keyboard size).
        int realSize = wKeyWidth * kbdSize.getNbWhiteKeys();
        int x_pos = ((r.width - realSize) / 2) + r.x;
        // System.out.println("PianoDisplay.doLayout() wKeyWidth=" + wKeyWidth + " realSize=" + realSize + " r.width=" + r.width +
        //                   " r.x=" + r.x + " x_pos=" + x_pos);

        int y_pos = r.y;

        for (Component c : getComponents())
        {
            PianoKey key = (PianoKey) c;

            // adjust size
            key.setRelativeSize(wKeyWidth, wKeyHeight);

            // translate it to put it after the last key
            key.setLocation(x_pos, y_pos);

            // adjust with top x size of the key
            x_pos += key.getNextKeyPos();
        }
    }

    /**
     * Overriden so mouse clicks NOT on keys (e.g. on the border) does not trigger any event.
     *
     * @param e A MouseEvent.
     */
    @Override
    protected void processMouseEvent(MouseEvent e)
    {
        if (e.getID() == MouseEvent.MOUSE_PRESSED)
        {
            // Nothing.
            // System.out.println("PianoDisplay Trapped ! MouseEvent e=" + e);

            return;
        }

        super.processMouseEvent(e);
    }

    //--------------------------------------------------------------------
    // Implement the MouseListener interface
    //--------------------------------------------------------------------
    @Override
    public void mousePressed(MouseEvent e)
    {
        PianoKey key = (PianoKey) e.getSource();

        if (!key.isEnabled())
        {
            return;
        }

        key.setSelected(!key.isSelected());

//        // Translate coordinates and dispatch the event to the PianoDisplay MouseListeners...
//        MouseEvent newEvent = new MouseEvent(this, e.getID(), e.getWhen(), e.getModifiers(),
//                e.getX() + key.getX(), e.getY() + key.getY(),
//                e.getClickCount(), false, e.getButton());
//
//        // Fire the event
//        super.processMouseEvent(newEvent);
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    //--------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------
    /**
     * Calculate the best width (best width/height ratio) corresponding to a specific height.
     *
     * @param h The required height.
     * @return The width.
     */
    private int getWidthFromHeight(int h)
    {
        // Adapt to minimize integer rounding errors : MUST BE UPTODATE WITH DOLAYOUT() !!!
        Insets in = getInsets();
        int rh = h - in.top - in.bottom;
        int wKeyWidth = (int) (rh * ((float) PianoKey.WW / PianoKey.WH));
        int w = (wKeyWidth * kbdSize.getNbWhiteKeys()) + in.left + in.right + 1;
        // System.out.println("PianoDisplay.getWidthFromHeight() h=" + h + " wKeyWidth=" + wKeyWidth + " w=" + w); ;
        return w;
    }

    /**
     * Calculate the best height (best width/height ratio) corresponding to a specific width.
     *
     * @param w The required width.
     * @return The height.
     */
    private int getHeightFromWidth(int w)
    {
        // Adapt to minimize integer rounding errors
        Insets in = getInsets();
        int rw = w - in.left - in.right;
        int wKeyWidth = (rw - 1) / kbdSize.getNbWhiteKeys();
        int h = (int) (wKeyWidth * ((float) PianoKey.WH / PianoKey.WW));
        return (h < getMinimumSize().height) ? getMinimumSize().height : h;
    }

}
