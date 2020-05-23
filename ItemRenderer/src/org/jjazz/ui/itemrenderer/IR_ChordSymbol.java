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
package org.jjazz.ui.itemrenderer;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.beans.PropertyChangeEvent;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.jjazz.harmony.StandardScaleInstance;
import org.jjazz.leadsheet.chordleadsheet.api.item.AltDataFilter;
import org.jjazz.leadsheet.chordleadsheet.api.item.AltExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.ui.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.ui.itemrenderer.api.IR_Copiable;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;
import org.jjazz.ui.utilities.TextLayoutUtils;

/**
 * An ItemRenderer for ChordSymbols.
 * <p>
 */
public class IR_ChordSymbol extends ItemRenderer implements IR_Copiable
{

    private AttributedString attChordString;
    private boolean copyMode;
    private IR_ChordSymbolSettings settings;
    private int zoomFactor = 50;
    private ChordRenderingInfo cri;
    private static final Logger LOGGER = Logger.getLogger(IR_ChordSymbol.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_ChordSymbol(CLI_ChordSymbol item)
    {
        super(item, IR_Type.ChordSymbol);
        LOGGER.fine("IR_ChordSymbol() item=" + item);
        cri = item.getData().getRenderingInfo();
        
        updateToolTipText();

        // Apply settings and listen to their changes
        settings = IR_ChordSymbolSettings.getDefault();
        settings.addPropertyChangeListener(this);
        setForeground(cri.getAccentFeature() == null ? settings.getColor() : settings.getAccentColor(cri.getAccentFeature()));
        // setForeground(settings.getColor());
        setFont(settings.getFont());
    }

    @Override
    public void modelChanged()
    {
        ExtChordSymbol ecs = (ExtChordSymbol) getModel().getData();
        cri = ((CLI_ChordSymbol) getModel()).getData().getRenderingInfo();
        setForeground(cri.getAccentFeature() == null ? settings.getColor() : settings.getAccentColor(cri.getAccentFeature()));
        updateToolTipText();
        revalidate();
        repaint();
    }

    /**
     * Calculate the preferredSize() depending on chord symbol, font and zoomFactor.
     * <p>
     */
    @Override
    public Dimension getPreferredSize()
    {
        // The chord symbol to show
        ExtChordSymbol ecs = (ExtChordSymbol) getModel().getData();

        // Prepare the graphics context
        Graphics2D g2 = (Graphics2D) getGraphics();
        assert g2 != null;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // The fonts to be used
        FontRenderContext frc = g2.getFontRenderContext();
        Font font = getFont();
        Font musicFont = settings.getMusicFont();

        // Make font size depend on the zoom factor
        float factor = 0.5f + (getZoomFactor() / 100f);
        float zFontSize = factor * font.getSize2D();
        zFontSize = Math.max(zFontSize, 12);

        // Get the strings making up the chord symbol : base [bass] extension
        String base = ecs.getRootNote().toRelativeNoteString() + ecs.getChordType().getBase();
        String bass = "";
        if (!ecs.getBassNote().equalsRelativePitch(ecs.getRootNote()))
        {
            bass = "/" + ecs.getBassNote().toRelativeNoteString();
        }
        String extension = ecs.getChordType().getExtension();

        // Create the AttributedString from the strings
        String strChord = base + extension + bass;
        String strChord2 = strChord.replace('#', settings.getSharpCharInMusicFont()).replace('b', settings.getFlatCharInMusicFont());
        attChordString = new AttributedString(strChord2);
        attChordString.addAttribute(TextAttribute.SIZE, zFontSize);                 // Default attribute
        attChordString.addAttribute(TextAttribute.FAMILY, font.getFontName());      // Default attribute
        // Use the music font for all the # and b symbols
        for (int i = 0; i < strChord.length(); i++)
        {
            if (strChord.charAt(i) == '#' || strChord.charAt(i) == 'b')
            {
                attChordString.addAttribute(TextAttribute.FAMILY, musicFont.getFontName(), i, i + 1);
            }
        }
        // Superscript for the extension
        if (!extension.isEmpty())
        {
            attChordString.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, base.length(), base.length() + extension.length());
        }

        // Create the TextLayout to get its dimension       
        TextLayout textLayout = new TextLayout(attChordString.getIterator(), frc);
        int w = (int) TextLayoutUtils.getWidth(textLayout, strChord2, false);
        int h = (int) TextLayoutUtils.getHeight(textLayout, frc);

        Insets in = getInsets();
        final int PADDING = 1;
        Dimension d = new Dimension((int) w + 2 * PADDING + in.left + in.right, h + 2 * PADDING + in.top + in.bottom);
        LOGGER.fine("getPreferredSize()    result d=" + d + "   (insets=" + in + ")");
        return d;
    }

    @Override
    public void modelMoved()
    {
        updateToolTipText();
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        settings.removePropertyChangeListener(this);
    }

    private void updateToolTipText()
    {
        ExtChordSymbol ecs = (ExtChordSymbol) getModel().getData();
        if (ecs != null)
        {
            // Chord Symbol
            ChordRenderingInfo cri = ecs.getRenderingInfo();
            StringBuilder sb = new StringBuilder(ecs.getChord().toRelativeNoteString(ecs.getRootNote().getAlterationDisplay()));
            if (!cri.getFeatures().isEmpty())
            {
                sb.append(" - ").append(cri.getFeatures().toString());
            }

            StandardScaleInstance ssi = cri.getScaleInstance();
            if (ssi != null)
            {
                sb.append(" - ").append(ssi);
            }

            // Alt chord symbol if any
            AltExtChordSymbol altSymbol = ecs.getAlternateChordSymbol();
            if (altSymbol != null)
            {
                sb.append("   /ALTERNATE: ");
                if (altSymbol == VoidAltExtChordSymbol.getInstance())
                {
                    sb.append("Void");
                } else
                {
                    sb.append(altSymbol);
                    cri = altSymbol.getRenderingInfo();
                    if (!cri.getFeatures().isEmpty())
                    {
                        sb.append(" - ").append(cri.getFeatures().toString());
                    }
                    ssi = cri.getScaleInstance();
                    if (ssi != null)
                    {
                        sb.append(" - ").append(ssi);
                    }
                }
                sb.append(" - Condition=");
                AltDataFilter altFilter = ecs.getAlternateFilter();
                assert altFilter != null;
                sb.append(altFilter.isRandom() ? "Random" : altFilter.getValues());
            }
            setToolTipText(sb.toString());
        } else
        {
            setToolTipText(null);
        }
    }

    /**
     * Zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    @Override
    public void setZoomFactor(int factor)
    {
        zoomFactor = factor;
        revalidate();
        repaint();
    }

    @Override
    public int getZoomFactor()
    {
        return zoomFactor;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        // Paint background
        super.paintComponent(g);

        Insets in = this.getInsets();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // LOGGER.severe("paintComponent() model=" + chordSymbol + " prefSize=" + getPreferredSize() + "  g2=" + g2);
        float x = in.left;
        float y = getHeight() - 1 - in.bottom;

        g2.drawString(attChordString.getIterator(), x, y);

        // Draw the scale presence indicator
        ExtChordSymbol ecs = (ExtChordSymbol) getModel().getData();
        if (ecs.getRenderingInfo().getScaleInstance() != null)
        {
            int length = (int) Math.round(8 * (0.7f + 0.6 * zoomFactor / 100f));
            int x1 = getWidth() / 2 - length / 2;
            int x2 = getWidth() / 2 + length / 2;
            y = getHeight() - 2;
            g2.drawLine(x1, (int) y, x2, (int) y);
        }

        if (copyMode)
        {
            // Draw the copy indicator in upper right corner
            int size = IR_Copiable.CopyIndicator.getSideLength();
            Graphics2D gg2 = (Graphics2D) g2.create(Math.max(getWidth() - size - 1, 0), 1, size, size);
            IR_Copiable.CopyIndicator.drawCopyIndicator(gg2);
            gg2.dispose();
        }

    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        super.propertyChange(e);
        if (e.getSource() == settings)
        {
            if (e.getPropertyName().equals(IR_ChordSymbolSettings.PROP_FONT))
            {
                setFont(settings.getFont());
            } else if (e.getPropertyName().equals(IR_ChordSymbolSettings.PROP_FONT_COLOR)
                    || e.getPropertyName().equals(IR_ChordSymbolSettings.PROP_FONT_ACCENT_COLOR))
            {
                ExtChordSymbol ecs = (ExtChordSymbol) getModel().getData();
                setForeground(cri.getAccentFeature() == null ? settings.getColor() : settings.getAccentColor(cri.getAccentFeature()));
            }
        }
    }

    //-------------------------------------------------------------------------------
    // IR_Copiable interface
    //-------------------------------------------------------------------------------
    @Override
    public void showCopyMode(boolean b)
    {
        if (copyMode != b)
        {
            copyMode = b;
            repaint();
        }
    }

    /**
     * Split the string around b or # chars.
     * <p>
     * Example : s=F7b9 will return list v[0] = F7, v1[1]=b v[2]=9
     */
    @SuppressWarnings("empty-statement")
    private java.util.List<String> splitStringSharpOrFlat(String str)
    {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> v = new ArrayList<>();

        for (int i = 0; i < str.length(); i++)
        {
            char c = str.charAt(i);

            if ((c == '#') || (c == 'b'))
            {
                char[] ca =
                {
                    c
                };
                v.add(sb.toString());
                sb.setLength(0);
                v.add(new String(ca));
            } else
            {
                sb.append(c);
            }
        }

        v.add(sb.toString());

        // Remove empty values
        while (v.remove(""))
        {
            ;
        }
        return v;
    }

}
