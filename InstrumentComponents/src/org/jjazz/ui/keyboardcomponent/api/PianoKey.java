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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.jjazz.harmony.api.Note;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent.Orientation;

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
    private static final String COLOR_BKEY_DARKEST = "WKeyDarkestColor";
    private static final String COLOR_BKEY_LIGHTEST = "WKeyLightestColor";
    private static final String COLOR_KEY_CONTOUR = "WKeyContourColor";
    private static final String COLOR_KEY_CONTOUR_SELECTED = "WKeyContourSelectedColor";
    private static final String COLOR_DISABLED_KEY = "WKeyDisabledColor";

    /**
     * Standard White Key Width in DOWN orientation.
     */
    public static final int WW = 12;

    /**
     * Standard White Key Height in DOWN orientation.
     */
    public static final int WH = WW * 5;

    /**
     * Minimum White Key Width in DOWN orientation..
     */
    public static final int WW_MIN = 4;

    /**
     * Minimum White Key Height in DOWN orientation..
     */
    public static final int WH_MIN = WW_MIN * 5;

    // Key colors depending on state
    /**
     * The polygon to store the shape of the key
     */
    private Polygon polygon;

    /**
     * The x position of the next key (because black and white keys overlap) in DOWN orientation..
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
    private Color pressedWhiteKeyColor;
    private boolean showVelocityColor;
    private Color markColor;
    private final KeyboardComponent.Orientation orientation;
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

    // private transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    /**
     * Create a standard Pianokey with Orientation.DOWN.
     *
     * @param p
     */
    public PianoKey(int p)
    {
        this(p, false, false, Orientation.DOWN);
    }

    /**
     * Construct a piano key for a specified pitch.
     *
     * @param p           The pitch of the key.
     * @param leftMost    If true this the leftmost key of the keyboard (different shape)
     * @param rightMost   If true this the rightmost key of the keyboard (different shape)
     * @param orientation
     */
    public PianoKey(int p, boolean leftMost, boolean rightMost, Orientation orientation)
    {
        if (!Note.checkPitch(p) || (leftMost == true && rightMost == true) || orientation == null)
        {
            throw new IllegalArgumentException("p=" + p + " leftMost=" + leftMost + " rightMost=" + rightMost + " orientation=" + orientation);
        }

        pitch = p;
        this.leftMost = leftMost;
        this.rightMost = rightMost;
        this.orientation = orientation;
        this.polygon = new Polygon();

        // Default color favlues
        putClientProperty(COLOR_WKEY, Color.WHITE);
        putClientProperty(COLOR_BKEY_DARKEST, Color.BLACK);
        putClientProperty(COLOR_BKEY_LIGHTEST, new Color(55, 55, 55));
        putClientProperty(COLOR_KEY_CONTOUR, new Color(117, 117, 117));
        putClientProperty(COLOR_KEY_CONTOUR_SELECTED, Color.BLUE.brighter());
        putClientProperty(COLOR_DISABLED_KEY, new Color(70, 70, 70));

        showVelocityColor = false;
        pressedWhiteKeyColor = new Color(0, 128, 192);

        // Preferred size
        var pd = orientation.equals(Orientation.DOWN) || orientation.equals(Orientation.UP) ? new Dimension(WW, WH) : new Dimension(WH, WW);
        setPreferredSize(pd);

        // Tooltip
        Note n = new Note(pitch);

        setToolTipText(n.toPianoOctaveString() + " (Midi pitch=" + pitch + ")");
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
     * Add a mark on the note using the specified color.
     *
     * @param color If null remove the existing mark.
     */
    public void setMarked(Color color)
    {
        if (!Objects.equals(color, markColor))
        {
            markColor = color;
            repaint();
        }
    }

    /**
     * Set this key released (velocity is set to 0).
     * <p>
     * Fire a change event if velocity was changed. Do nothing if this key is not enabled.
     */
    public synchronized void setReleased()
    {
        if (!isEnabled())
        {
            return;
        }


        if (velocity > 0)
        {
            int old = velocity;
            velocity = 0;
            firePropertyChange(PROP_PRESSED, old, velocity);
            repaint();
        }
    }

    /**
     * Set the key in the "pressed" state.
     * <p>
     * Key is pressed if velocity &gt; 0. If velocity==0, setReleased() is called. Fire a changed event if velocity was changed.
     * Do nothing if this key is not enabled.
     *
     * @param v
     * @param pressedKeyColor Set the pressed color for white key (pressed color for a black key is calculated from this color as
     *                        well). If null use default color.
     */
    public synchronized void setPressed(int v, Color pressedKeyColor)
    {
        if (!Note.checkVelocity(v))
        {
            throw new IllegalArgumentException("v=" + v + " pressedColorWK=" + pressedKeyColor);
        }


        if (!isEnabled())
        {
            return;
        }


        if (v == 0)
        {
            setReleased();
            return;
        }

        // Special case, no velocity change, don't fire a change event
        if (v == velocity)
        {
            if (pressedKeyColor != null && !pressedWhiteKeyColor.equals(pressedKeyColor))
            {
                // Repaint only if color has changed
                pressedWhiteKeyColor = pressedKeyColor;
                repaint();
            }
            return;
        }


        // Special case: no need to repaint if key is already pressed and no color change
        if (velocity > 0 && !showVelocityColor && pressedKeyColor == null)
        {
            int old = velocity;
            velocity = v;
            firePropertyChange(PROP_PRESSED, old, velocity);
            return;
        }


        // Normal case
        if (pressedKeyColor != null && !pressedWhiteKeyColor.equals(pressedKeyColor))
        {
            pressedWhiteKeyColor = pressedKeyColor;
        }
        int old = velocity;
        velocity = v;
        firePropertyChange(PROP_PRESSED, old, velocity);
        repaint();


    }

    public void setSelected(boolean b)
    {
        if (b != isSelected)
        {
            isSelected = b;
            firePropertyChange(PROP_SELECTED, !isSelected, isSelected);
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
        setPressed(0, null);
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
     * Return the relative X position of the next key, in DOWN orientation.
     *
     * @return
     */
    public int getNextKeyPosX()
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

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final int CONTOUR_WIDTH = 2;
        if (isEnabled())
        {
            if (velocity > 0)
            {
                Color keyPressedColor = isWhiteKey() ? getPressedWhiteKeyColor() : getPressedBlackKeyColor();
                Color c = showVelocityColor ? getAdaptedColor(keyPressedColor, velocity) : keyPressedColor;
                g2.setColor(c);

//                int boundedLuminance = Math.min(luminance, getLuminanceNoEvent());
//                boundedLuminance = Math.max(boundedLuminance, getLuminanceMaxEvents());
//                g2.setColor(hslFillColor.adjustLuminance(boundedLuminance));

            } else
            {
                // Easy 
                if (!isWhiteKey())
                {
                    // Slight gradient on black keys
                    float x1, y1, x2, y2;
                    Color c1, c2;
                    switch (orientation)
                    {
                        case DOWN:
                            x1 = 0;
                            y1 = CONTOUR_WIDTH;
                            x2 = 0;
                            y2 = getHeight();
                            c1 = getColorProperty(COLOR_BKEY_LIGHTEST);
                            c2 = getColorProperty(COLOR_BKEY_DARKEST);
                            break;
                        case UP:
                            throw new UnsupportedOperationException("UP");
                        case LEFT:
                            throw new UnsupportedOperationException("LEFT");
                        case RIGHT:
                            x1 = CONTOUR_WIDTH;
                            y1 = 0;
                            x2 = getWidth();
                            y2 = 0;
                            c1 = getColorProperty(COLOR_BKEY_LIGHTEST);
                            c2 = getColorProperty(COLOR_BKEY_DARKEST);
                            break;
                        default:
                            throw new AssertionError(orientation.name());

                    }
                    GradientPaint paint = new GradientPaint(x1, y1, c1, x2, y2, c2);
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


        g2.fill(polygon);
        g2.setColor(isSelected ? getColorProperty(COLOR_KEY_CONTOUR_SELECTED) : getColorProperty(COLOR_KEY_CONTOUR));
        g2.setStroke(isSelected ? new BasicStroke(CONTOUR_WIDTH) : new BasicStroke());
        g2.draw(polygon);


        // For all other graphics painted directly over the polygon shape: rotate the graphics context as required
        Graphics2D g3 = (Graphics2D) g2.create();
        g3.transform(orientation.getTransform(getBounds()));


        // Add the optional mark
        if (isEnabled() && markColor != null)
        {
            double w = getWidth();
            double h = getHeight();
            double side = w * 0.4;
            double x = w / 2 - side / 2;
            double y = h - 5 - side;
            Rectangle2D.Double rect = new Rectangle2D.Double(x, y, side, side);
            g3.setColor(markColor);
            g3.fill(rect);
        }
        g3.dispose();
    }

    /**
     * Change the color c according to midiValue.
     *
     * @param c
     * @param midiValue A value 0-127.
     * @return
     */
    static public Color getAdaptedColor(Color c, int midiValue)
    {
        Preconditions.checkNotNull(c);
        Preconditions.checkArgument(midiValue >= 0 && midiValue < 128);
        float f = (127f - midiValue) / 127f;
        int alpha = 255 - Math.round(220 * f);
        Color res = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        return res;
    }

    /**
     * Change the size of the key from a reference rectangular white key size.
     *
     * @param wwRef The base width of a reference white key in DOWN orientation.
     * @param whRef The height of a reference white key in DOWN orientation.
     */
    public void setSizeRelativeToWhiteKey(int wwRef, int whRef)
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
        double[] points = null;

        // The little angle for white keys
        final int r = 1;
        int rp = pitch % 12;

        switch (rp)
        {
            // C key
            case 0:
                if (!rightMost)
                {
                    points = new double[]
                    {
                        0, 0, ww - bw_2_3, 0, ww - bw_2_3, bh, ww, bh, ww, wh - r,
                        ww - r, wh, r, wh, 0, wh - r
                    };
                    xPosNextKey = ww - bw_2_3;
                } else
                {
                    points = new double[]
                    {
                        0, 0, ww, 0, ww, wh - r, ww - r, wh, r, wh, 0, wh - r
                    };
                    xPosNextKey = ww;
                }

                break;
            // C# key
            case 1:
                points = new double[]
                {
                    0, 0, bw, 0, bw, bh, 0, bh
                };
                xPosNextKey = bw_2_3;

                break;
            // D key
            case 2:
                points = new double[]
                {
                    bw_1_3, 0, ww - bw_1_3, 0, ww - bw_1_3, bh, ww, bh, ww,
                    wh - r, ww - r, wh, r, wh, 0, wh - r, 0, bh, bw_1_3, bh
                };
                xPosNextKey = ww - bw_1_3;
                break;
            // D# key
            case 3:
                points = new double[]
                {
                    0, 0, bw, 0, bw, bh, 0, bh
                };
                xPosNextKey = bw_1_3;
                break;

            // E key
            case 4:
                if (!leftMost)
                {
                    points = new double[]
                    {
                        bw_2_3, 0, ww, 0, ww, wh - r, ww - r, wh, r, wh, 0, wh - r,
                        0, bh, bw_2_3, bh
                    };
                    xPosNextKey = ww;
                } else
                {
                    points = new double[]
                    {
                        0, 0, ww, 0, ww, wh - r, ww - r, wh, r, wh, 0, wh - r
                    };
                    xPosNextKey = ww;
                }
                break;

            // F key
            case 5:
                points = new double[]
                {
                    0, 0, ww - bw_4_5, 0, ww - bw_4_5, bh, ww, bh, ww, wh - r,
                    ww - r, wh, r, wh, 0, wh - r
                };
                xPosNextKey = ww - bw_4_5;
                break;

            // F# key
            case 6:
                points = new double[]
                {
                    0, 0, bw, 0, bw, bh, 0, bh
                };
                xPosNextKey = bw_4_5;
                break;

            // G key
            case 7:
                if (!rightMost)
                {
                    points = new double[]
                    {
                        bw_1_5, 0, ww - bw_1_2a, 0, ww - bw_1_2a, bh, ww, bh, ww,
                        wh - r, ww - r, wh, r, wh, 0, wh - r, 0, bh, bw_1_5, bh
                    };
                    xPosNextKey = ww - bw_1_2a;
                } else
                {
                    points = new double[]
                    {
                        bw_1_5, 0, ww, 0, ww, wh - r, ww - r, wh, r, wh, 0, wh - r, 0, bh, bw_1_5, bh
                    };
                    xPosNextKey = ww;
                }
                break;
            // G# key
            case 8:
                points = new double[]
                {
                    0, 0, bw, 0, bw, bh, 0, bh
                };
                xPosNextKey = bw_1_2a;
                break;
            // A key
            case 9:
                if (!leftMost)
                {
                    points = new double[]
                    {
                        bw_1_2b, 0, ww - bw_1_5, 0, ww - bw_1_5, bh, ww, bh, ww,
                        wh - r, ww - r, wh, r, wh, 0, wh - r, 0, bh, bw_1_2b, bh
                    };
                    xPosNextKey = ww - bw_1_5;
                } else
                {
                    points = new double[]
                    {
                        0, 0, ww - bw_1_5, 0, ww - bw_1_5, bh, ww, bh, ww,
                        wh - r, ww - r, wh, r, wh, 0, wh - r
                    };
                    xPosNextKey = ww - bw_1_5;
                }

                break;
            // A# key
            case 10:
                points = new double[]
                {
                    0, 0, bw, 0, bw, bh, 0, bh
                };
                xPosNextKey = bw_1_5;
                break;
            // B key
            case 11:
                points = new double[]
                {
                    bw_4_5, 0, ww, 0, ww, wh - r, ww - r, wh, r, wh, 0, wh - r,
                    0, bh, bw_4_5, bh
                };
                xPosNextKey = ww;
                break;
        }

        // By default DOWN orientation
        polygon = createPolygon(points);
        var bounds = polygon.getBounds();


        if (!orientation.equals(Orientation.DOWN))
        {
            // Other orientation: apply transforms
            double[] newPoints = new double[points.length];
            AffineTransform transform = orientation.getTransform(bounds);
            transform.transform(points, 0, newPoints, 0, points.length / 2);
            polygon = createPolygon(newPoints);
            bounds = polygon.getBounds();
        }


        // Set the component size bounding box size
        setSize(bounds.width, bounds.height);
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
        return polygon.contains(x, y);
    }

    /**
     * @return the showVelocityColor
     */
    public boolean isShowVelocityColor()
    {
        return showVelocityColor;
    }

    /**
     * Set if pressed color is adjusted depending on note velocity.
     *
     * @param showVelocityColor the showVelocityColor to set
     */
    public void setShowVelocityColor(boolean showVelocityColor)
    {
        this.showVelocityColor = showVelocityColor;
        repaint();
    }

    /**
     * @return Based on getPressedWhiteKeyColor()
     */
    public Color getPressedBlackKeyColor()
    {
        // return pressedWhiteKeyColor.darker();
        return pressedWhiteKeyColor;
    }

    /**
     * @return the pressedWhiteKeyColor
     */
    public Color getPressedWhiteKeyColor()
    {
        return pressedWhiteKeyColor;
    }

    /**
     *
     * @return
     */
    public Color getMarkedColor()
    {
        return markColor;
    }

    /**
     * Set the pressed color for a white key.
     * <p>
     * Used also to derive the pressed color for a black key.
     *
     * @param pressedKeyColor the pressedKeyColor to set
     */
    public void setPressedWhiteKeyColor(Color pressedKeyColor)
    {
        this.pressedWhiteKeyColor = pressedKeyColor;
        if (velocity > 0)
        {
            repaint();
        }

    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("PianoKey pitch=" + pitch + " [ ");
        int rp = pitch % 12;

        for (int i = 0; i < polygon.npoints; i++)
        {
            sb.append("(" + polygon.xpoints[i] + "," + polygon.ypoints[i] + ") ");
        }
        sb.append("]");
        return sb.toString();
    }

    // =================================================================================
    // Private methods
    // =================================================================================

    private Polygon createPolygon(double[] points)
    {
        Polygon res = new Polygon();
        for (int i = 0; i < points.length; i += 2)
        {
            int x = (int) Math.round(points[i]);
            int y = (int) Math.round(points[i + 1]);
            res.addPoint(x, y);
        }
        return res;
    }
}
