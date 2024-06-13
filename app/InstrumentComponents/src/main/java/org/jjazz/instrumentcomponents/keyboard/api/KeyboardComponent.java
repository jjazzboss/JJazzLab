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
package org.jjazz.instrumentcomponents.keyboard.api;

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
 * The keyboard respects a fixed width/length ratio and is painted centered.
 * <p>
 * Features:<br>
 * - 4 possible orientations<br>
 * - optional "out of range" graphic indicators<br>
 * - optional text on notes<br>
 * - show pressed notes with color depending on velocity<br>
 * - different keyboard sizes<br>
 * - scalable<br>
 *
 * @see PianoKey
 */
public class KeyboardComponent extends JPanel
{

    /**
     * Piano keyboard orientation.
     * <p>
     * DOWN means the only-white-keys-side is on the down/bottom side, the black-keys-side is up/top.
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
                    res = AffineTransform.getQuadrantRotateInstance(-2);
                    res.preConcatenate(AffineTransform.getTranslateInstance(0, bounds.height));
                    break;
                case LEFT:
                    res = AffineTransform.getQuadrantRotateInstance(1);
                    res.preConcatenate(AffineTransform.getTranslateInstance(bounds.height, 0));
                    break;
                case RIGHT:
                    res = AffineTransform.getQuadrantRotateInstance(-1);
                    res.preConcatenate(AffineTransform.getTranslateInstance(0, bounds.width));
                    break;
                default:
                    throw new IllegalStateException("this=" + this);
            }
            return res;
        }
    }

    private final static int OUT_OF_RANGE_INDICATOR_DEFAULT_SPACE = 6;
    private final int OUT_OF_RANGE_INDICATOR_SPACE;
    private final boolean useOutOfRangeIndicator;
    private boolean outOfRangeIndicatorLeft;
    private boolean outOfRangeIndicatorRight;
    private final Orientation orientation;
    private KeyboardRange range;
    private float scaleFactorX;
    private float scaleFactorY;

    private final List<PianoKey> pianoKeys = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(KeyboardComponent.class.getSimpleName());

    /**
     * Create an horizontal component with 88 notes and Orientation.DOWN, and no out of range indicators.
     * <p>
     */
    public KeyboardComponent()
    {
        this(KeyboardRange._88_KEYS);
    }

    /**
     * Create an horizontal component with the specified size, Orientation.DOWN, and no out of range indicators.
     * <p>
     * @param kbdSize
     */
    public KeyboardComponent(KeyboardRange kbdSize)
    {
        this(kbdSize, Orientation.DOWN, false);
    }

    /**
     * Create a keyboard with the specified parameters.
     *
     * @param kbdSize
     * @param orientation
     * @param useOutOfRangeIndicator Show an indicator on the side if a note is out of the range of the keyboard.
     */
    public KeyboardComponent(KeyboardRange kbdSize, Orientation orientation, boolean useOutOfRangeIndicator)
    {
        Preconditions.checkNotNull(kbdSize);
        Preconditions.checkNotNull(orientation);

        this.orientation = orientation;
        this.useOutOfRangeIndicator = useOutOfRangeIndicator;
        this.OUT_OF_RANGE_INDICATOR_SPACE = useOutOfRangeIndicator ? OUT_OF_RANGE_INDICATOR_DEFAULT_SPACE : 0;
        if (useOutOfRangeIndicator && !orientation.equals(Orientation.DOWN))
        {
            throw new UnsupportedOperationException("");
        }
        this.scaleFactorX = 1;
        this.scaleFactorY = 1;

        setLayout(null);

        setRange(kbdSize);


    }

    public boolean useOutOfRangeIndicator()
    {
        return useOutOfRangeIndicator;
    }

    public Orientation getOrientation()
    {
        return orientation;
    }


    @Override
    public String toString()
    {
        return "Keyboard[" + range + "]";
    }

    public KeyboardRange getRange()
    {
        return range;
    }

    /**
     * Make the component smaller or larger.
     *
     * @param factorX A value &gt; 0. Impact the keyboard width in DOWN/UP orientation (height in LEFT/RIGHT orientation).
     * @param factorY A value &gt; 0. Impact the keyboard height in DOWN/UP orientation (width in LEFT/RIGHT orientation).
     */
    public void setScaleFactor(float factorX, float factorY)
    {
        Preconditions.checkArgument(factorX > 0);
        Preconditions.checkArgument(factorY > 0);

        if (scaleFactorX != factorX || scaleFactorY != factorY)
        {
            scaleFactorX = factorX;
            scaleFactorY = factorY;
            // LOGGER.severe("setScaleFactor() factorX=" + factorX + " factorY=" + factorY);
            // We need to update the PianoKeys size & position now, because other components (see PianoRollEditor) might use it
            // once revalidate() is called.
            layoutKeys();
            revalidate();
            repaint();
        }
    }

    public float getScaleFactorX()
    {
        return scaleFactorX;
    }

    public float getScaleFactorY()
    {
        return scaleFactorY;
    }

    /**
     * Set the keyboard size.
     * <p>
     * New PianoKeys are created. Pressed/marked notes are maintained. This updates also the preferred and minimum size. Caller
     * must synchronize this method if other threads update this keyboard in parallel.
     *
     * @param kbdRange
     */
    public final void setRange(KeyboardRange kbdRange)
    {
        if (kbdRange == null)
        {
            throw new NullPointerException("kbdRange");
        }

        if (kbdRange.equals(range))
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
        range = kbdRange;

        pianoKeys.forEach(pk -> remove(pk));
        pianoKeys.clear();

        for (int i = range.getLowestPitch(); i <= range.getHighestPitch(); i++)
        {
            boolean leftmost = (i == range.getLowestPitch());
            boolean rightmost = (i == range.getHighestPitch());
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

        layoutKeys();
        revalidate();
        repaint();
    }

    /**
     * Get the rectangle enclosing all the keys.
     *
     * @return The Rectangle position is relative to this KeyboardComponent.
     */
    public Rectangle getKeysBounds()
    {
        var firstKey = pianoKeys.get(0);
        var lastKey = pianoKeys.get(pianoKeys.size() - 1);
        var r = firstKey.getBounds().union(lastKey.getBounds());
        return r;
    }


    /**
     * Get all the PianoKeys.
     *
     * @return
     */
    public List<PianoKey> getAllKeys()
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
    public PianoKey getKey(int pitch)
    {
        return range.isValid(pitch) ? pianoKeys.get(pitch - range.lowPitch) : null;
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
        if (range.isValid(pitch))
        {
            getKey(pitch).setReleased();
        } else
        {
            showOutOfRangeNoteIndicator(pitch < range.lowPitch, false);
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
        if (range.isValid(pitch))
        {
            getKey(pitch).setPressed(velocity, pressedKeyColor);
        } else
        {
            showOutOfRangeNoteIndicator(pitch < range.lowPitch, true);
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
        if (range.isValid(pitch))
        {
            getKey(pitch).setMarked(markColor);
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
        if (!range.isValid(pitch))
        {
            throw new IllegalArgumentException("pitch=" + pitch + " keyboardRange=" + range);
        }
        return getKey(pitch).getVelocity();
    }

    /**
     * Get the PianoKey that correspond to a specific point.
     *
     * @param p A Point object relative to this component.
     *
     * @return Can be null.
     */
    public PianoKey getKey(Point p)
    {
        Component c = this.getComponentAt(p.x, p.y);
        if (c instanceof PianoKey pianoKey)
        {
            return pianoKey;
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

        // LOGGER.severe("paintComponent() -- width=" + getWidth() + " h=" + getHeight());

        // Prepare out of range indicators
        final int length = 5;
        final int semiHeight = 2;
        Insets in = getInsets();
        float yTop = in.top;
        if (!pianoKeys.isEmpty())
        {
            switch (orientation)
            {
                case DOWN ->
                    yTop = pianoKeys.get(0).getY();
                case UP ->
                    throw new UnsupportedOperationException("UP");
                case LEFT ->
                    throw new UnsupportedOperationException("LEFT");
                case RIGHT ->
                    yTop = pianoKeys.get(pianoKeys.size() - 1).getY();
                default ->
                    throw new AssertionError(orientation.name());
            }
        }
        float y = yTop + 20;

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

    @Override
    public Dimension getMinimumSize()
    {
        Insets in = getInsets();

        int wMin = in.left + in.right + 1 + (int) Math.ceil(scaleFactorX * getRange().getNbWhiteKeys() * PianoKey.WW_MIN);
        int hMin = in.top + (int) Math.ceil(scaleFactorY * PianoKey.WH_MIN) + in.bottom;
        if (isVertical())
        {
            int tmp = wMin;
            wMin = hMin;
            hMin = tmp;
        }
        return new Dimension(wMin, hMin);
    }

    @Override
    public Dimension getPreferredSize()
    {
        Insets in = getInsets();
        int w = (int) Math.ceil(scaleFactorX * PianoKey.WW * getRange().getNbWhiteKeys()) + in.left + in.right + 1 + 2 * OUT_OF_RANGE_INDICATOR_SPACE;
        int h = in.top + (int) Math.ceil(scaleFactorY * PianoKey.WH) + in.bottom;
        if (isVertical())
        {
            int tmp = w;
            w = h;
            h = tmp;
        }
        var res = new Dimension(w, h);
        // LOGGER.severe("getPreferredSize() res=" + res);
        return res;
    }


//    @Override
//    public void doLayout()
//    {
//        LOGGER.severe("doLayout() -- ");
//        doLayout(getSize());
//    }
    //--------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------
    /**
     * Layout the keys to fit the size.
     * <p>
     * Because of integer rounding errors, it may not fit exactly the required dimensions. The keyboard is centered inside the
     * box.
     */
    private void layoutKeys()
    {
        Dimension pd = getPreferredSize();
        // LOGGER.severe("layoutKeys() -- pd=" + pd);
        Insets in = getInsets();
        Rectangle r = new Rectangle(in.left + OUT_OF_RANGE_INDICATOR_SPACE, in.top, pd.width - in.left - in.right - 2 * OUT_OF_RANGE_INDICATOR_SPACE, pd.height - in.top - in.bottom);
        if (isVertical())
        {
            r = new Rectangle(in.left, in.top + OUT_OF_RANGE_INDICATOR_SPACE, pd.width - in.left - in.right, pd.height - in.top - in.bottom - 2 * OUT_OF_RANGE_INDICATOR_SPACE);
        }


        // Keyboard takes all the horizontal space (in DOWN orientation)
        int wKeyHeight = computeKeyboardHeightFromWidth(isVertical() ? r.height : r.width);
        int minNoteHeight = isVertical() ? getMinimumSize().width : getMinimumSize().height;
        int availableNoteHeight = isVertical() ? r.width : r.height;
        wKeyHeight = Math.max(wKeyHeight, minNoteHeight);  // Can't be smaller than minimal height
        wKeyHeight = Math.min(wKeyHeight, availableNoteHeight);      // Can't be taller than available height


        // Width of a white key
        int wKeyWidth = (isVertical() ? r.height - 1 : r.width - 1) / range.getNbWhiteKeys();


        // Calculate X/Y so the keyboard will be centered (because of integer rounding we may have differences
        // between the object size and the real keyboard size)
        int realSize = wKeyWidth * range.getNbWhiteKeys();
        int x_pos;
        switch (orientation)
        {
            case DOWN ->
                x_pos = r.x + ((r.width - realSize) / 2);
            case UP ->
                throw new UnsupportedOperationException("UP");
            case LEFT ->
                throw new UnsupportedOperationException("LEFT");
            case RIGHT ->
                x_pos = r.x + ((r.width - wKeyHeight) / 2);
            default ->
                throw new AssertionError(orientation.name());
        }


        int y_pos;
        switch (orientation)
        {
            case DOWN ->
                y_pos = r.y + (r.height - wKeyHeight) / 2;          // centered
            case UP ->
                throw new UnsupportedOperationException("UP");
            case LEFT ->
                throw new UnsupportedOperationException("LEFT");
            case RIGHT ->
                y_pos = r.y + r.height - ((r.height - realSize) / 2);          // centered
            default ->
                throw new AssertionError(orientation.name());
        }

        for (PianoKey key : pianoKeys)
        {
            // adjust size
            key.setSizeRelativeToWhiteKeyRef(wKeyWidth, wKeyHeight);

            switch (orientation)
            {
                case DOWN ->
                {
                    key.setLocation(x_pos, y_pos);
                    x_pos += key.getNextKeyPosX();
                }
                case UP ->
                    throw new UnsupportedOperationException("UP");
                case LEFT ->
                    throw new UnsupportedOperationException("LEFT");
                case RIGHT ->
                {
                    key.setLocation(x_pos, y_pos - key.getHeight());
                    y_pos -= key.getNextKeyPosX();
                }
                default ->
                    throw new AssertionError(orientation.name());
            }


        }
    }


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
        float scaleRatio = ((float) PianoKey.WH * scaleFactorX) / (PianoKey.WW * scaleFactorY);
        int wKeyWidth = (w - 1) / range.getNbWhiteKeys();
        int h = Math.round(wKeyWidth * scaleRatio);
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
        if (!useOutOfRangeIndicator)
        {
            return;
        }
        if (left)
        {
            outOfRangeIndicatorLeft = showHide;
        } else
        {
            outOfRangeIndicatorRight = showHide;
        }
        repaint();
    }


    // =================================================================================================
    // Inner classes
    // =================================================================================================
}
