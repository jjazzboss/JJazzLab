/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jjazz.ui.utilities;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.Bidi;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities related to text layout and TextLayoutPart.
 * 
 * @author Miloslav Metelka
 */
final class TextLayoutUtils {
    
    // -J-Dorg.netbeans.modules.editor.lib2.view.TextLayoutUtils.level=FINE
    private static final Logger LOG = Logger.getLogger(TextLayoutUtils.class.getName());
    
    private TextLayoutUtils() {
        // NO instances
    }

    public static float getHeight(TextLayout textLayout) {
        float height = textLayout.getAscent() + textLayout.getDescent() + textLayout.getLeading();
        // Ceil to whole points since when doing a compound TextLayout and then
        // using TextLayoutUtils.getRealAlloc() with its TL.getVisualHighlightShape() and doing
        // Graphics2D.fill(Shape) on the returned shape then for certain fonts such as
        // Lucida Sans Typewriter size=10 on Ubuntu 10.04 the background is rendered one pixel down for certain lines
        // so there appear white lines inside a selection.
        return (float) Math.ceil(height);
    }
    
    /**
     * Compute a most appropriate width of the given text layout.
     */
    public static float getWidth(TextLayout textLayout, String textLayoutText, Font font) {
        // For italic fonts the textLayout.getAdvance() includes some extra horizontal space.
        // On the other hand index2X() for TL.getCharacterCount() is width along baseline
        // so when TL ends with e.g. 'd' char the end of 'd' char is cut off.
        float width;
        int tlLen = textLayoutText.length();
        if (!font.isItalic() ||
            tlLen == 0 ||
            Character.isWhitespace(textLayoutText.charAt(tlLen - 1)) ||
            Bidi.requiresBidi(textLayoutText.toCharArray(), 0, textLayoutText.length()))
        {
            width = textLayout.getAdvance();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("TLUtils.getWidth(\"" + debugText(textLayoutText) + // NOI18N
                        "\"): Using TL.getAdvance()=" + width + // NOI18N
//                        textLayoutDump(textLayout) + 
                        '\n');
            }
        } else {
            // Compute pixel bounds (with frc being null - means use textLayout's frc; and with default bounds)
            Rectangle pixelBounds = textLayout.getPixelBounds(null, 0, 0);
            width = (float) pixelBounds.getMaxX();
            // On Mac OS X with retina displays the TL.getPixelBounds() give incorrect results. Luckily
            // TL.getAdvance() gives a correct result in that case.
            // Therefore use a minimum of both values (on all platforms).
            float tlAdvance = textLayout.getAdvance();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("TLUtils.getWidth(\"" + debugText(textLayoutText) + // NOI18N
                        "\"): Using minimum of TL.getPixelBounds().getMaxX()=" + width + // NOI18N
                        " or TL.getAdvance()=" + tlAdvance +
                        textLayoutDump(textLayout) +
                        '\n');
            }
            width = Math.min(width, tlAdvance);
        }
        
        // For RTL text the hit-info of the first char is above the hit-info of ending char.
        // However textLayout.isLeftToRight() returns true in case of mixture of LTR and RTL text
        // in a single textLayout.
        
        // Ceil the width to avoid rendering artifacts.
        width = (float) Math.ceil(width);
        return width;
    }
    
    private static String textLayoutDump(TextLayout textLayout) {
        return "\n  TL.getAdvance()=" + textLayout.getAdvance() + // NOI18N
                "\n  TL.getBounds()=" + textLayout.getBounds() + // NOI18N
                "\n  TL: " + textLayout; // NOI18N
    }
    
  
    
    public static String toStringShort(TextLayout textLayout) {
        return "[" + textLayout.getCharacterCount() + "]W=" + textLayout.getAdvance(); // NOI18N
    }

    public static String toString(TextLayout textLayout) {
        return toStringShort(textLayout) + "; " + // NOI18N
                textLayout.toString();
    }
    
    
     /**
     * Get real allocation (possibly not rectangular) of a part of layout.
     * <br>
     * It's used when rendering the text layout for filling background highlights of the view.
     *
     * @param length Total number of characters for which the allocation is computed.
     * @param alloc Allocation given by a parent view.
     * @return
     */
    public static Shape getRealAlloc(TextLayout textLayout, Rectangle2D textLayoutRect,
            TextHitInfo startHit, TextHitInfo endHit)
    {
        // Quick-fix to eliminate missing line in rendering italic "d" - more elaborate fix is needed
        textLayoutRect = new Rectangle2D.Double(textLayoutRect.getX(), textLayoutRect.getY(),
                textLayoutRect.getWidth() + 2, textLayoutRect.getHeight());
        Rectangle2D.Double zeroBasedRect = shape2Bounds(textLayoutRect);
        zeroBasedRect.x = 0;
        zeroBasedRect.y = 0;
        Shape ret = textLayout.getVisualHighlightShape(startHit, endHit, zeroBasedRect);
        AffineTransform transform = AffineTransform.getTranslateInstance(
                textLayoutRect.getX(),
                textLayoutRect.getY()
        );
        ret = transform.createTransformedShape(ret);
        // The following gives bad result for some reason (works for layout but not for caret modelToView())
//        Shape ret2 = textLayout.getVisualHighlightShape(startHit.getCharIndex(), endHit.getCharIndex(), textLayoutRect);
        return ret;
    }
    
     public static Rectangle2D.Double shape2Bounds(Shape s) {
        Rectangle2D r;
        if (s instanceof Rectangle2D) {
            r = (Rectangle2D) s;
        } else {
            r = s.getBounds2D();
        }
        return new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }
    
    /**
     * Append the character description to the given string buffer
     * translating the special characters (and '\') into escape sequences.
     *
     * @param sb non-null string buffer to append to.
     * @param ch character to be debugged.
     */
    public static void debugChar(StringBuffer sb, char ch) {
        switch (ch) {
            case '\n':
                sb.append("\\n"); // NOI18N
                break;
            case '\r':
                sb.append("\\r"); // NOI18N
                break;
            case '\t':
                sb.append("\\t"); // NOI18N
                break;
            case '\b':
                sb.append("\\b"); // NOI18N
                break;
            case '\f':
                sb.append("\\f"); // NOI18N
                break;
            case '\\':
                sb.append("\\\\"); // NOI18N
                break;
            default:
                sb.append(ch);
                break;
        }
    }
    
    /**
     * Append the character description to the given string builder
     * translating the special characters (and '\') into escape sequences.
     *
     * @param sb non-null string buffer to append to.
     * @param ch character to be debugged.
     */
    public static void debugChar(StringBuilder sb, char ch) {
        switch (ch) {
            case '\n':
                sb.append("\\n"); // NOI18N
                break;
            case '\r':
                sb.append("\\r"); // NOI18N
                break;
            case '\t':
                sb.append("\\t"); // NOI18N
                break;
            case '\b':
                sb.append("\\b"); // NOI18N
                break;
            case '\f':
                sb.append("\\f"); // NOI18N
                break;
            case '\\':
                sb.append("\\\\"); // NOI18N
                break;
            default:
                sb.append(ch);
                break;
        }
    }
    
    /**
     * Return the text description of the given character
     * translating the special characters (and '\') into escape sequences.
     *
     * @param ch char to debug.
     * @return non-null debug text.
     */
    public static String debugChar(char ch) {
        StringBuilder sb = new StringBuilder();
        debugChar(sb, ch);
        return sb.toString();
    }
    
    /**
     * Append the text description to the given string buffer
     * translating the special characters (and '\') into escape sequences.
     *
     * @param sb non-null string buffer to append to.
     * @param text non-null text to be debugged.
     */
    public static void debugText(StringBuffer sb, CharSequence text) {
        for (int i = 0; i < text.length(); i++) {
            debugChar(sb, text.charAt(i));
        }
    }
    
    /**
     * Append the text description to the given string builder
     * translating the special characters (and '\') into escape sequences.
     *
     * @param sb non-null string builder to append to.
     * @param text non-null text to be debugged.
     */
    public static void debugText(StringBuilder sb, CharSequence text) {
        for (int i = 0; i < text.length(); i++) {
            debugChar(sb, text.charAt(i));
        }
    }
    
    /**
     * Create text description as String
     * translating the special characters (and '\') into escape sequences.
     *
     * @param text non-null text to be debugged.
     * @return non-null string containing the debug text.
     */
    public static String debugText(CharSequence text) {
        StringBuilder sb = new StringBuilder();
        debugText(sb, text);
        return sb.toString();
    }

}
