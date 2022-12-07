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
package org.jjazz.ui.keyboardcomponent.api;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 * A JPanel representing a Piano keyboard with selectable keys.
 * <p>
 */
public class KeyboardComponent extends JPanel
{

    /**
     * Piano keyboard orientation.
     */
    public enum Orientation
    {
        DOWN, UP, LEFT, RIGHT;

        /**
         * Return the transform associated to the orientation, provided that DOWN is associated to the identity transform.
         *
         * @param bounds
         * @return
         */
        public AffineTransform getTransform(Rectangle bounds)
        {
            AffineTransform res;
            switch (this)
            {
                case DOWN:
                    res = new AffineTransform();
                    break;
                case UP:
                    res = AffineTransform.getQuadrantRotateInstance(2);
                    res.concatenate(AffineTransform.getTranslateInstance(0, bounds.height));
                    break;
                case LEFT:
                    res = AffineTransform.getQuadrantRotateInstance(-1);
                    res.concatenate(AffineTransform.getTranslateInstance(bounds.height, 0));
                    break;
                case RIGHT:
                    res = AffineTransform.getQuadrantRotateInstance(1);
                    res.concatenate(AffineTransform.getTranslateInstance(0, bounds.width));
                    break;
                default:
                    throw new IllegalStateException("this=" + this);
            }
            return res;
        }
    }

    private final static int OUT_OF_RANGE_INDICATOR_SPACE = 6;
    private boolean outOfRangeIndicatorLeft;
    private boolean outOfRangeIndicatorRight;
    private final Orientation orientation;
    private KeyboardRange keyboardRange;

    private final List<PianoKey> pianoKeys = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(KeyboardComponent.class.getSimpleName());

    /**
     * Create an horizontal component with 88 notes and Orientation.DOWN.
     * <p>
     */
    public KeyboardComponent()
    {
        this(KeyboardRange._88_KEYS);
    }

    /**
     * Create an horizontal component with the specified size and Orientation.DOWN.
     * <p>
     * @param kbdSize
     */
    public KeyboardComponent(KeyboardRange kbdSize)
    {
        this(kbdSize, Orientation.DOWN);
    }

    /**
     * Create a keyboard with the specified parameters.
     *
     * @param kbdSize
     * @param orientation
     */
    public KeyboardComponent(KeyboardRange kbdSize, Orientation orientation)
    {
        Preconditions.checkNotNull(kbdSize);
        Preconditions.checkNotNull(orientation);

        this.orientation = orientation;
        setKeyboardRange(kbdSize);
    }

    public Orientation getOrientation()
    {
        return orientation;
    }


    @Override
    public String toString()
    {
        return "Keyboard[" + keyboardRange + "]";
    }

    public KeyboardRange getRange()
    {
        return keyboardRange;
    }

    /**
     * Set the keyboard size.
     * <p>
     * New PianoKeys are created. Pressed/marked notes are maintained. This updates also the preferred and minimum size. Caller
     * must synchronize this method if other threads update this keyboard in parallel.
     *
     * @param kbdRange
     */
    public final void setKeyboardRange(KeyboardRange kbdRange)
    {
        if (kbdRange == null)
        {
            throw new NullPointerException("kbdRange");
        }

        if (kbdRange.equals(keyboardRange))
        {
            return;
        }

        // Save state
        HashMap<Integer, Color> pressedColors = new HashMap<>();
        HashMap<Integer, Integer> pressedVelocities = new HashMap<>();
        HashMap<Integer, Color> markedColors = new HashMap<>();
        pianoKeys.forEach(pk ->
        {
            int pitch = pk.getPitch();
            if (pk.isPressed())
            {
                pressedColors.put(pitch, pk.getPressedWhiteKeyColor());
                pressedVelocities.put(pitch, pk.getVelocity());
            }
            Color mColor = pk.getMarkedColor();
            if (mColor != null)
            {
                markedColors.put(pitch, mColor);
            }
        });

        // Update keys
        keyboardRange = kbdRange;

        pianoKeys.forEach(pk -> remove(pk));
        pianoKeys.clear();

        for (int i = keyboardRange.getLowestPitch(); i <= keyboardRange.getHighestPitch(); i++)
        {
            boolean leftmost = (i == keyboardRange.getLowestPitch());
            boolean rightmost = (i == keyboardRange.getHighestPitch());
            PianoKey key = new PianoKey(i, leftmost, rightmost, orientation);
            pianoKeys.add(key);
            add(key);

            // Restore state
            Color c = pressedColors.get(i);
            if (c != null)
            {
                key.setPressed(pressedVelocities.get(i), c);
            }
            key.setMarked(markedColors.get(i));
        }


        // Set preferred size
        Insets in = getInsets();
        int w = (PianoKey.WW * getRange().getNbWhiteKeys()) + in.left + in.right + 1 + 2 * OUT_OF_RANGE_INDICATOR_SPACE;
        int h = PianoKey.WH;
        if (isVertical())
        {
            int tmp = w;
            w = h;
            h = tmp;
        }
        setPreferredSize(new Dimension(w, h));


        // Set minimum size
        int wMin = PianoKey.WW_MIN * getRange().getNbWhiteKeys();
        int hMin = PianoKey.WH_MIN;
        if (isVertical())
        {
            int tmp = wMin;
            wMin = hMin;
            hMin = tmp;
        }
        setMinimumSize(new Dimension(wMin, hMin));


        revalidate();
        repaint();
    }

    /**
     * Get all the PianoKeys.
     *
     * @return
     */
    public List<PianoKey> getPianoKeys()
    {
        return new ArrayList<>(pianoKeys);
    }

    public List<PianoKey> getBlackKeys()
    {
        return pianoKeys.stream().filter(pk -> !pk.isWhiteKey()).toList();
    }

    public List<PianoKey> getWhiteKeys()
    {
        return pianoKeys.stream().filter(pk -> pk.isWhiteKey()).toList();
    }

    /**
     * Get the PianoKey for specified pitch.
     * <p>
     *
     * @param pitch
     * @return Can be null if pitch is out of range.
     */
    public PianoKey getPianoKey(int pitch)
    {
        return keyboardRange.isValid(pitch) ? pianoKeys.get(pitch - keyboardRange.lowPitch) : null;
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
     * Set all keys unpressed, remove all marks.
     */
    public void reset()
    {
        pianoKeys.forEach(pk ->
        {
            pk.setReleased();
            pk.setMarked(null);
        });
        showOutOfRangeNoteIndicator(false, false);
        showOutOfRangeNoteIndicator(true, false);
    }

    /**
     * Release the specified key.
     *
     * @param pitch
     */
    public void setReleased(int pitch)
    {
        if (keyboardRange.isValid(pitch))
        {
            getPianoKey(pitch).setReleased();
        } else
        {
            showOutOfRangeNoteIndicator(pitch < keyboardRange.lowPitch, false);
        }
    }

    /**
     * Set the pressed status of specified key.
     * <p>
     * Method delegates to setPressed() of the relevant PianoKey.
     * <p>
     * If pitch is outside the KeyboardRange, show an indicator on the leftmost/rightmost note.
     *
     * @param pitch
     * @param velocity        If 0 equivalent to calling setReleased()
     * @param pressedKeyColor The pressed key color to be used. If null use default color.
     */
    public void setPressed(int pitch, int velocity, Color pressedKeyColor)
    {
        if (keyboardRange.isValid(pitch))
        {
            getPianoKey(pitch).setPressed(velocity, pressedKeyColor);
        } else
        {
            showOutOfRangeNoteIndicator(pitch < keyboardRange.lowPitch, true);
        }
    }

    /**
     * Set the marked status of specified key.
     * <p>
     * Method delegates to setMarked() of the relevant PianoKey.
     * <p>
     *
     * @param pitch
     * @param markColor If null remove the mark.
     */
    public void setMarked(int pitch, Color markColor)
    {
        if (keyboardRange.isValid(pitch))
        {
            getPianoKey(pitch).setMarked(markColor);
        }
    }

    /**
     * Get the pressed velocity of a specific key.
     * <p>
     * Method delegates to getVelocity() of the relevant PianoKey.
     *
     * @param pitch The pitch of the key. Must be a valid pitch for the KeyboardRange.
     *
     * @return If 0 it means the key is released.
     */
    public int getPressedVelocity(int pitch)
    {
        if (!keyboardRange.isValid(pitch))
        {
            throw new IllegalArgumentException("pitch=" + pitch + " keyboardRange=" + keyboardRange);
        }
        return getPianoKey(pitch).getVelocity();
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
     * Overridden to paint the out of range indicators.
     *
     * @param g
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);        // Honor the opaque property
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.LIGHT_GRAY);
        g2.transform(orientation.getTransform(getBounds()));

        final int length = 5;
        final int semiHeight = 2;
        Insets in = getInsets();
        float y = in.top + 20;

        if (outOfRangeIndicatorLeft)
        {
            float x = in.left + OUT_OF_RANGE_INDICATOR_SPACE / 2 - length / 2;
            var s = new Path2D.Float();
            s.moveTo(x, y);
            s.lineTo(x + length, y - semiHeight);
            s.lineTo(x + length, y + semiHeight);
            s.lineTo(x, y);
            s.closePath();
            g2.fill(s);
        }
        if (outOfRangeIndicatorRight)
        {
            float x = getWidth() - in.right - OUT_OF_RANGE_INDICATOR_SPACE / 2 + length / 2;
            var s = new Path2D.Float();
            s.moveTo(x, y);
            s.lineTo(x - length, y - semiHeight);
            s.lineTo(x - length, y + semiHeight);
            s.lineTo(x, y);
            s.closePath();
            g2.fill(s);
        }

        g2.dispose();

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
        // super.doLayout();
        Insets in = getInsets();
        Rectangle r = new Rectangle(in.left + OUT_OF_RANGE_INDICATOR_SPACE, in.top, getWidth() - in.left - in.right - 2 * OUT_OF_RANGE_INDICATOR_SPACE, getHeight() - in.top - in.bottom);
        if (isVertical())
        {
            r = new Rectangle(in.left, in.top + OUT_OF_RANGE_INDICATOR_SPACE, getWidth() - in.left - in.right, getHeight() - in.top - in.bottom - 2 * OUT_OF_RANGE_INDICATOR_SPACE);
        }

        // Keyboard takes all the horizontal space (in DOWN orientation)
        int wKeyHeight = computeKeyboardHeightFromWidth(r.width);
        int minNoteHeight = isVertical() ? getMinimumSize().width : getMinimumSize().height;
        int availableNoteHeight = isVertical() ? r.width : r.height;
        wKeyHeight = Math.max(wKeyHeight, minNoteHeight);  // Can't be smaller than minimal height
        wKeyHeight = Math.min(wKeyHeight, availableNoteHeight);      // Can't be taller than available height

        // Size of a white key
        int wKeyWidth = (isVertical() ? r.height - 1 : r.width - 1) / keyboardRange.getNbWhiteKeys();


        // Calculate X so the keyboard will be centered (because of integer rounding we may have differences
        // between the object size and the real keyboard size)
        int realSize = wKeyWidth * keyboardRange.getNbWhiteKeys();
        int x_pos = isVertical() ? r.x : r.x + ((r.width - realSize) / 2);

        // Y centered
        // int y_pos = r.y + (r.height - wKeyHeight) / 2;
        int y_pos = isVertical() ? r.y + ((r.height - realSize) / 2) : r.y;

        for (PianoKey key : pianoKeys)
        {
            // adjust size
            key.setSizeRelativeToWhiteKey(wKeyWidth, wKeyHeight);

            // translate it to put it after the last key
            key.setLocation(x_pos, y_pos);

            // offset to next key
            if (isVertical())
            {
                y_pos += key.getNextKeyPosX();
            } else
            {
                x_pos += key.getNextKeyPosX();
            }

        }
    }

    //--------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------
    /**
     * Calculate the keyboard height (in DOWN orientation) from specified keyboard width in order to maintain the optimal aspect
     * ratio.
     *
     * @param w The target keyboard width.
     * @return The keyboard height.
     */
    private int computeKeyboardHeightFromWidth(int w)
    {
        // Adapt to minimize integer rounding errors
        int wKeyWidth = (w - 1) / keyboardRange.getNbWhiteKeys();
        int h = (int) (wKeyWidth * ((float) PianoKey.WH / PianoKey.WW));
        return h;
    }

    private boolean isVertical()
    {
        return orientation.equals(Orientation.LEFT) || orientation.equals(Orientation.RIGHT);
    }

    /**
     *
     * @param left     false means right
     * @param showHide show if true, hide if false
     */
    private void showOutOfRangeNoteIndicator(boolean left, boolean showHide)
    {
        if (left)
        {
            outOfRangeIndicatorLeft = showHide;
        } else
        {
            outOfRangeIndicatorRight = showHide;
        }
        repaint();
    }

}
