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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.Note;

/**
 * A piano keyboard key.
 * <p>
 * Can show if key is selected, or if pressed with an indication of the velocity.
 */
public class PianoKey extends JComponent
{

    /**
     * Property change fired when setSelected() is called.
     */
    private static final String PROP_SELECTED = "PropSelected";
    /**
     * Property change fired when setPressed() is called.
     */
    private static final String PROP_PRESSED = "PropPressed";

    // 
    // Client properties
    // call repaint() after updating these clientProperties
    private static final String COLOR_WKEY = "WKeyColor";
    private static final String COLOR_WKEY_PRESSED = "WKeyPressedColor";
    private static final String COLOR_BKEY_DARKEST = "WKeyDarkestColor";
    private static final String COLOR_BKEY_LIGHTEST = "WKeyLightestColor";
    private static final String COLOR_BKEY_PRESSED = "WKeyPressedColor";
    private static final String COLOR_KEY_CONTOUR = "WKeyContourColor";
    private static final String COLOR_KEY_CONTOUR_SELECTED = "WKeyContourSelectedColor";
    private static final String COLOR_DISABLED_KEY = "WKeyDisabledColor";

    /**
     * Standard White Key Width.
     */
    public static final int WW = 12;

    /**
     * Standard White Key Height.
     */
    public static final int WH = WW * 5;

    /**
     * Minimum White Key Width.
     */
    public static final int WW_MIN = 4;

    /**
     * Minimum White Key Height.
     */
    public static final int WH_MIN = WW_MIN * 5;

    // Key colors depending on state
    /**
     * The polygon to store the shape of the key
     */
    private final Polygon shape = new Polygon();

    /**
     * The x position of the next key (because black and white keys overlap).
     */
    private int xPosNextKey;

    /**
     * If velocity > 0 it means the key is pressed.
     */
    private int velocity;

    /**
     * Pitch of the note
     */
    private final int pitch;

    private boolean isSelected;

    /**
     * True if this key if the leftmost key of the pianodisplay (special shape).
     */
    private boolean leftMost;

    /**
     * True if this key if the righmost key of the pianodisplay (special shape).
     */
    private boolean rightMost;

    private static final Logger LOGGER = Logger.getLogger(PianoKey.class.getSimpleName());

    private transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    public PianoKey(int p)
    {
        this(p, false, false);
    }

    /**
     * Construct a piano key for a specified pitch.
     *
     * @param p The pitch of the key.
     * @param leftMost If true this the leftmost key of the keyboard (different shape)
     * @param rightMost If true this the rightmost key of the keyboard (different shape)
     */
    public PianoKey(int p, boolean leftMost, boolean rightMost)
    {
        if (!Note.checkPitch(p) || (leftMost == true && rightMost == true))
        {
            throw new IllegalArgumentException("p=" + p + " leftMost=" + leftMost + " rightMost=" + rightMost);
        }

        pitch = p;
        this.leftMost = leftMost;
        this.rightMost = rightMost;

        // Default color favlues
        putClientProperty(COLOR_WKEY, Color.WHITE);
        putClientProperty(COLOR_WKEY_PRESSED, Color.RED);
        putClientProperty(COLOR_BKEY_DARKEST, Color.BLACK);
        putClientProperty(COLOR_BKEY_LIGHTEST, new Color(55, 55, 55));
        putClientProperty(COLOR_BKEY_PRESSED, Color.RED);
        putClientProperty(COLOR_KEY_CONTOUR, new Color(117, 117, 117));
        putClientProperty(COLOR_KEY_CONTOUR_SELECTED, Color.BLUE.brighter());
        putClientProperty(COLOR_DISABLED_KEY, Color.LIGHT_GRAY);


        // Preferred size
        setPreferredSize(new Dimension(WW, WH));

        // Tooltip
        Note n = new Note(pitch);

        setToolTipText(n.toAbsoluteNoteString() + " (Midi pitch=" + pitch + ")");
    }

    public Color getColorProperty(String key)
    {
        return (Color) getClientProperty(key);
    }

    public void setColorProperty(String key, Color c)
    {
        putClientProperty(key, c);
        repaint();
    }

    public final boolean isWhiteKey()
    {
        return Note.isWhiteKey(pitch);
    }

    /**
     * Set the key in the "pressed" state or not with the specified velocity.
     * <p>
     * Key is pressed if velocity &gt; 0. Fire a changed event if velocity was changed.
     *
     *
     *
     * @param v
     */
    public void setPressed(int v)
    {
        if (!Note.checkVelocity(v))
        {
            throw new IllegalArgumentException("v=" + v);
        }
        if (!isEnabled())
        {
            velocity = v;           // But don't update UI
            return;
        }
        if (v != velocity)
        {
            int old = velocity;
            velocity = v;
            pcs.firePropertyChange(PROP_PRESSED, old, velocity);
            repaint();

        }
    }

    public void setSelected(boolean b)
    {
        if (b != isSelected)
        {
            isSelected = b;
            pcs.firePropertyChange(PROP_SELECTED, !isSelected, isSelected);
            repaint();
        }
    }

    public boolean isSelected()
    {
        return isSelected;
    }

    /**
     * If velocity is &gt; 0 then it means the key is being pressed.
     *
     * @return
     */
    public int getVelocity()
    {
        return velocity;
    }

    /**
     * Convenience method, same as setPressed(0).
     */
    public void release()
    {
        setPressed(0);
    }

    /**
     * Convenience method, same as getVelocity() &gt; 0.
     *
     * @return
     */
    public boolean isPressed()
    {
        return velocity > 0;
    }

    public int getPitch()
    {
        return pitch;
    }

    /**
     * Return the relative X position of the next key.
     */
    public int getNextKeyPos()
    {
        return xPosNextKey;
    }

    /**
     * Draw the key.
     *
     * @param g The Graphics context in which to draw.
     */
    @Override
    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;

        final int CONTOUR_WIDTH = 2;
        if (isEnabled())
        {
            if (velocity > 0)
            {
                // Show pressed color adjusted to velocity
                float f = ((Note.VELOCITY_MAX - (float) velocity) / (Note.VELOCITY_MAX - Note.VELOCITY_MIN));
                int alpha = 255 - Math.round(220 * f);
                Color keyPressedColor = isWhiteKey() ? getColorProperty(COLOR_WKEY_PRESSED) : getColorProperty(COLOR_BKEY_PRESSED);
                g2.setColor(new Color(keyPressedColor.getRed(), keyPressedColor.getGreen(), keyPressedColor.getBlue(), alpha));

//                int boundedLuminance = Math.min(luminance, getLuminanceNoEvent());
//                boundedLuminance = Math.max(boundedLuminance, getLuminanceMaxEvents());
//                g2.setColor(hslFillColor.adjustLuminance(boundedLuminance));

            } else
            {
                // Easy 
                if (!isWhiteKey())
                {
                    GradientPaint paint = new GradientPaint(0, CONTOUR_WIDTH, getColorProperty(COLOR_BKEY_LIGHTEST),
                            0, getHeight(), getColorProperty(COLOR_BKEY_DARKEST));
                    g2.setPaint(paint);
                } else
                {
                    g2.setColor(getColorProperty(COLOR_WKEY));
                }
            }
        } else
        {
            g2.setColor(getColorProperty(COLOR_DISABLED_KEY));
        }


        g2.fill(shape);
        g2.setColor(isSelected ? getColorProperty(COLOR_KEY_CONTOUR_SELECTED) : getColorProperty(COLOR_KEY_CONTOUR));
        g2.setStroke(isSelected ? new BasicStroke(CONTOUR_WIDTH) : new BasicStroke());
        g2.draw(shape);
    }

    /**
     * Change the size of the key from a reference rectangular white key size.
     *
     * @param wwRef The base width of a reference white key.
     * @param whRef The height of a reference white key.
     */
    public void setRelativeSize(int wwRef, int whRef)
    {
        // Sizes from my piano keyboard
        int bh = (int) (whRef * .66666f);
        int bw = (int) ((wwRef * 15f) / 28f);

        // Minus 1 to be used as coordinates (0 to (wh-1) => height=wh)
        int ww = wwRef;
        int wh = whRef;

        // Pre-calculate values to avoid integer rounding errors
        // bw_2_3=two third of a black key width
        int bw_2_3 = (int) ((bw * 2f) / 3f);
        int bw_1_3 = bw - bw_2_3;
        int bw_4_5 = (int) ((bw * 4f) / 5f);
        int bw_1_5 = bw - bw_4_5;
        int bw_1_2a = (int) (bw / 2f);
        int bw_1_2b = bw - bw_1_2a;

        // The shape of the key
        int[] keySize = null;

        // The little angle for white keys
        final int r = 1;
        int rp = pitch % 12;

        switch (rp)
        {
            // C key
            case 0:
                if (!rightMost)
                {
                    keySize = new int[]
                    {
                        0, 0, ww - bw_2_3, 0, ww - bw_2_3, bh, ww, bh, ww, wh - r,
                        ww - r, wh, r, wh, 0, wh - r
                    };
                    xPosNextKey = ww - bw_2_3;
                } else
                {
                    keySize = new int[]
                    {
                        0, 0, ww, 0, ww, wh - r, ww - r, wh, r, wh, 0, wh - r
                    };
                    xPosNextKey = ww;
                }

                break;
            // C# key
            case 1:
                keySize = new int[]
                {
                    0, 0, bw, 0, bw, bh, 0, bh
                };
                xPosNextKey = bw_2_3;

                break;
            // D key
            case 2:
                keySize = new int[]
                {
                    bw_1_3, 0, ww - bw_1_3, 0, ww - bw_1_3, bh, ww, bh, ww,
                    wh - r, ww - r, wh, r, wh, 0, wh - r, 0, bh, bw_1_3, bh
                };
                xPosNextKey = ww - bw_1_3;
                break;
            // D# key
            case 3:
                keySize = new int[]
                {
                    0, 0, bw, 0, bw, bh, 0, bh
                };
                xPosNextKey = bw_1_3;
                break;

            // E key
            case 4:
                if (!leftMost)
                {
                    keySize = new int[]
                    {
                        bw_2_3, 0, ww, 0, ww, wh - r, ww - r, wh, r, wh, 0, wh - r,
                        0, bh, bw_2_3, bh
                    };
                    xPosNextKey = ww;
                } else
                {
                    keySize = new int[]
                    {
                        0, 0, ww, 0, ww, wh - r, ww - r, wh, r, wh, 0, wh - r
                    };
                    xPosNextKey = ww;
                }
                break;

            // F key
            case 5:
                keySize = new int[]
                {
                    0, 0, ww - bw_4_5, 0, ww - bw_4_5, bh, ww, bh, ww, wh - r,
                    ww - r, wh, r, wh, 0, wh - r
                };
                xPosNextKey = ww - bw_4_5;
                break;

            // F# key
            case 6:
                keySize = new int[]
                {
                    0, 0, bw, 0, bw, bh, 0, bh
                };
                xPosNextKey = bw_4_5;
                break;

            // G key
            case 7:
                if (!rightMost)
                {
                    keySize = new int[]
                    {
                        bw_1_5, 0, ww - bw_1_2a, 0, ww - bw_1_2a, bh, ww, bh, ww,
                        wh - r, ww - r, wh, r, wh, 0, wh - r, 0, bh, bw_1_5, bh
                    };
                    xPosNextKey = ww - bw_1_2a;
                } else
                {
                    keySize = new int[]
                    {
                        bw_1_5, 0, ww, 0, ww, wh - r, ww - r, wh, r, wh, 0, wh - r, 0, bh, bw_1_5, bh
                    };
                    xPosNextKey = ww;
                }
                break;
            // G# key
            case 8:
                keySize = new int[]
                {
                    0, 0, bw, 0, bw, bh, 0, bh
                };
                xPosNextKey = bw_1_2a;
                break;
            // A key
            case 9:
                if (!leftMost)
                {
                    keySize = new int[]
                    {
                        bw_1_2b, 0, ww - bw_1_5, 0, ww - bw_1_5, bh, ww, bh, ww,
                        wh - r, ww - r, wh, r, wh, 0, wh - r, 0, bh, bw_1_2b, bh
                    };
                    xPosNextKey = ww - bw_1_5;
                } else
                {
                    keySize = new int[]
                    {
                        0, 0, ww - bw_1_5, 0, ww - bw_1_5, bh, ww, bh, ww,
                        wh - r, ww - r, wh, r, wh, 0, wh - r
                    };
                    xPosNextKey = ww - bw_1_5;
                }

                break;
            // A# key
            case 10:
                keySize = new int[]
                {
                    0, 0, bw, 0, bw, bh, 0, bh
                };
                xPosNextKey = bw_1_5;
                break;
            // B key
            case 11:
                keySize = new int[]
                {
                    bw_4_5, 0, ww, 0, ww, wh - r, ww - r, wh, r, wh, 0, wh - r,
                    0, bh, bw_4_5, bh
                };
                xPosNextKey = ww;
                break;
        }

        // Zero polygon data
        shape.reset();

        // Compute factors for piano keys
        for (int i = 0; i < keySize.length; i += 2)
        {
            int x = keySize[i];
            int y = keySize[i + 1];
            shape.addPoint(x, y);
        }

        // Set the component size bounding box size
        Rectangle rec = shape.getBounds();
        setSize(rec.width + 1, rec.height + 1);
    }

    /**
     * Determine whether the key polygon contains a specified point x,y.
     *
     * @param x
     * @param y
     */
    @Override
    public boolean contains(int x, int y)
    {
        return shape.contains(x, y);
    }

    public void addChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removeChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("PianoKey pitch=" + pitch + " [ ");
        int rp = pitch % 12;

        for (int i = 0; i < shape.npoints; i++)
        {
            sb.append("(" + shape.xpoints[i] + "," + shape.ypoints[i] + ") ");
        }
        sb.append("]");
        return sb.toString();
    }

    // =================================================================================
    // Private methods
    // =================================================================================
}
