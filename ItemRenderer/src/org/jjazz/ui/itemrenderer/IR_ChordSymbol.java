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
import java.awt.font.GlyphVector;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.jjazz.harmony.StandardScaleInstance;
import org.jjazz.leadsheet.chordleadsheet.api.item.AltDataFilter;
import org.jjazz.leadsheet.chordleadsheet.api.item.AltExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo.PlayStyle;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.ui.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.ui.itemrenderer.api.IR_Copiable;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;

/**
 * An ItemRenderer for ChordSymbols.
 * <p>
 * The base and the extension of a ChordSymbol are rendered with different sizes and height using the extensionFactorSize and
 * extensionOffset. e.g. for "C7b9" C7=base b9=extension. The rendering is stored in vectors of GlyphVector so that it can be
 * rendered quickly.
 */
public class IR_ChordSymbol extends ItemRenderer implements IR_Copiable
{

    private static final float EXT_FACTOR_SIZE = 0.8f;
    private static final float EXT_FACTOR_OFFSET_HEIGHT = 0.3f;
    private static final float SHARP_BASE_FACTOR_SIZE = (14f / 16);
    private static final float SHARP_EXT_FACTOR_SIZE = (14f / 16);
    private static final float FLAT_BASE_FACTOR_SIZE = (12f / 16);
    private static final float FLAT_EXT_FACTOR_SIZE = (12f / 16);
    // The fonts for the Sharp & Flat
    private Font sharpBaseFont;
    private Font sharpExtensionFont;
    private Font flatBaseFont;
    private Font flatExtensionFont;
    /**
     * List of the glyphVectors to draw the base of the ChordEvent.
     */
    private final ArrayList<GlyphVector> baseGlyphVectors = new ArrayList<>();
    /**
     * Width of each base GlyphVector.
     */
    private final ArrayList<Float> baseGlyphVectorsWidths = new ArrayList<>();
    /**
     * List of the glyphVectors to draw the bass part of the base of the ChordEvent.
     */
    private final ArrayList<GlyphVector> bassGlyphVectors = new ArrayList<>();
    /**
     * Width of each base bassGlyphVector.
     */
    private final ArrayList<Float> bassGlyphVectorsWidths = new ArrayList<>();
    /**
     * List of the glyphVectors to draw the extension of the ChordEvent.
     */
    private final ArrayList<GlyphVector> extensionGlyphVectors = new ArrayList<>();
    /**
     * Width of each extension GlyphVector.
     */
    private final ArrayList<Float> extensionGlyphVectorsWidths = new ArrayList<>();
    /**
     * Font used for the extension part.
     */
    private Font extensionFont;
    /**
     * Offset of the height of the extension part compared to the base.
     */
    private float extensionOffset;
    /**
     * Copy mode.
     */
    private boolean copyMode;
    /**
     * The settings for this object.
     */
    private IR_ChordSymbolSettings settings;
    private int zoomFactor = 50;
    private boolean isMacOS = false;
    private static final Logger LOGGER = Logger.getLogger(IR_ChordSymbol.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_ChordSymbol(CLI_ChordSymbol item)
    {
        super(item, IR_Type.ChordSymbol);
        LOGGER.fine("IR_ChordSymbol() item=" + item);

        updateToolTipText();

        // Apply settings and listen to their changes
        settings = IR_ChordSymbolSettings.getDefault();
        settings.addPropertyChangeListener(this);
        setForeground(item.getData().getAlternateChordSymbol() == null ? settings.getColor() : settings.getAltColor());
        setFont(settings.getFont());
        isMacOS = System.getProperty("os.name").toLowerCase().contains("mac");       
    }

    @Override
    public void modelChanged()
    {
        ExtChordSymbol ecs = (ExtChordSymbol) getModel().getData();
        setForeground(ecs.getAlternateChordSymbol() == null ? settings.getColor() : settings.getAltColor());
        updateToolTipText();
        revalidate();
        repaint();
    }

    /**
     * Calculate the preferredSize() depending on chord symbol, font and zoomFactor.
     * <p>
     * Also precalculate data to speed it paintComponent().
     * <p>
     */
    @Override
    public Dimension getPreferredSize()
    {
        ExtChordSymbol ecs = (ExtChordSymbol) getModel().getData();
        Font f = getFont();
        int zFactor = getZoomFactor();
        Graphics2D g2 = (Graphics2D) getGraphics();
        assert g2 != null : "g2=" + g2 + " ecs=" + ecs + " f=" + f + " zFactor=" + zFactor;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float factor = 0.5f + (zFactor / 100f);
        float zBaseFontSize = factor * f.getSize2D();
        zBaseFontSize = Math.max(zBaseFontSize, 11);
        Font zBaseFont = f.deriveFont(zBaseFontSize);

        // Update fonts
        extensionFont = zBaseFont.deriveFont(zBaseFontSize * EXT_FACTOR_SIZE);
        sharpBaseFont = settings.getMusicFont().deriveFont(zBaseFontSize * SHARP_BASE_FACTOR_SIZE);
        sharpExtensionFont = settings.getMusicFont().deriveFont(zBaseFontSize * SHARP_EXT_FACTOR_SIZE);
        flatBaseFont = settings.getMusicFont().deriveFont(zBaseFontSize * FLAT_BASE_FACTOR_SIZE);
        flatExtensionFont = settings.getMusicFont().deriveFont(zBaseFontSize * FLAT_EXT_FACTOR_SIZE);

        // Update extensionOffset
        float baseHeight = g2.getFontMetrics(zBaseFont).getHeight();
        extensionOffset = baseHeight * EXT_FACTOR_OFFSET_HEIGHT;

        float width = 0;
        float height = 0;

        // Clear previous data
        baseGlyphVectors.clear();
        baseGlyphVectorsWidths.clear();
        bassGlyphVectors.clear();
        bassGlyphVectorsWidths.clear();
        extensionGlyphVectors.clear();
        extensionGlyphVectorsWidths.clear();

        // Ok we can precalculate
        String base = ecs.getRootNote().toRelativeNoteString() + ecs.getChordType().getBase();
        String bass = "";
        if (!ecs.getBassNote().equalsRelativePitch(ecs.getRootNote()))
        {
            bass = "/" + ecs.getBassNote().toRelativeNoteString();
        }
        String extension = ecs.getChordType().getExtension();

        // Split out the flats and sharps...
        java.util.List<String> baseStrings = splitStringAlt(base);
        java.util.List<String> bassStrings = splitStringAlt(bass);
        java.util.List<String> extensionStrings = splitStringAlt(extension);

        FontRenderContext frc = g2.getFontRenderContext();

        LOGGER.fine("getPreferredSize() -- baseStrings=" + baseStrings + " bassStrings=" + bassStrings + " extStrings=" + extensionStrings);

        // Base of the ChordSymbol
        GlyphVector gv;
        for (String s : baseStrings)
        {
            switch (s)
            {
                case "b":
                    gv = flatBaseFont.createGlyphVector(frc, settings.getFlatGlyphCode());
                    break;
                case "#":
                    gv = sharpBaseFont.createGlyphVector(frc, settings.getSharpGlyphCode());
                    break;
                default:
                    gv = zBaseFont.createGlyphVector(frc, s);
                    break;
            }
            baseGlyphVectors.add(gv);
            LOGGER.fine("getPreferredSize()    base glyph bounds=" + gv.getPixelBounds(frc, 1, 0));
        }

        // Bass part of the base of the ChordSymbol
        for (String s : bassStrings)
        {
            switch (s)
            {
                case "b":
                    gv = flatBaseFont.createGlyphVector(frc, settings.getFlatGlyphCode());
                    break;
                case "#":
                    gv = sharpBaseFont.createGlyphVector(frc, settings.getSharpGlyphCode());
                    break;
                default:
                    gv = zBaseFont.createGlyphVector(frc, s);
                    break;
            }
            bassGlyphVectors.add(gv);
            LOGGER.fine("getPreferredSize()    bass glyph bounds=" + gv.getPixelBounds(frc, 1, 0));
        }

        // Extension of the ChordSymbol
        for (String s : extensionStrings)
        {
            switch (s)
            {
                case "b":
                    gv = flatExtensionFont.createGlyphVector(frc, settings.getFlatGlyphCode());
                    break;
                case "#":
                    gv = sharpExtensionFont.createGlyphVector(frc, settings.getSharpGlyphCode());
                    break;
                default:
                    gv = extensionFont.createGlyphVector(frc, s);
                    break;
            }
            extensionGlyphVectors.add(gv);
            LOGGER.fine("getPreferredSize()    extension glyph bounds=" + gv.getPixelBounds(frc, 1, 0));
        }

        // Calculate width
        for (GlyphVector gvi : baseGlyphVectors)
        {
            Rectangle r = gvi.getPixelBounds(frc, 1, 0);
            int w = r.width + 1;
            int h = r.height + 1;
            height = Math.max(height, h);
            width += w;
            baseGlyphVectorsWidths.add(new Float(w));
        }

        for (GlyphVector gvi : bassGlyphVectors)
        {
            Rectangle r = gvi.getPixelBounds(frc, 1, 0);
            int w = r.width + 1;
            int h = r.height + 1;
            height = Math.max(height, h);
            width += w;
            bassGlyphVectorsWidths.add(new Float(w));
        }

        for (GlyphVector gvi : extensionGlyphVectors)
        {
            Rectangle r = gvi.getPixelBounds(frc, 1, 0);
            int w = gvi.getPixelBounds(frc, 1, 0).width;
            int h = (int) extensionOffset + r.height + 1;
            height = Math.max(height, h);
            width += w;
            extensionGlyphVectorsWidths.add(new Float(w));
        }
        g2.dispose();

        Insets in = getInsets();
        Dimension d = new Dimension((int) width + 2 + in.left + in.right, (int) height + 2 + in.top + in.bottom);
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
            if (!cri.getPlayStyle().equals(PlayStyle.NORMAL))
            {
                sb.append(" - ").append(cri.getPlayStyle());
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
                    if (!cri.getPlayStyle().equals(PlayStyle.NORMAL))
                    {
                        sb.append(" - ").append(cri.getPlayStyle());
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

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // LOGGER.severe("paintComponent() model=" + chordSymbol + " prefSize=" + getPreferredSize() + "  g2=" + g2);
        float x = 2;
        float y = getHeight() - 3;

        // Draw base
        for (int i = 0; i < baseGlyphVectors.size(); i++)
        {
            GlyphVector gv = baseGlyphVectors.get(i);
            g2.drawGlyphVector(gv, x, y);
            x += baseGlyphVectorsWidths.get(i);
        }

        // Draw extension
        y = y - extensionOffset;

        for (int i = 0; i < extensionGlyphVectors.size(); i++)
        {
            GlyphVector gv = extensionGlyphVectors.get(i);
            g2.drawGlyphVector(gv, x, y);
            x += extensionGlyphVectorsWidths.get(i);
        }

        // Draw bass part if any
        y = y + extensionOffset;
        for (int i = 0; i < bassGlyphVectors.size(); i++)
        {
            GlyphVector gv = bassGlyphVectors.get(i);
            g2.drawGlyphVector(gv, x, y);
            x += bassGlyphVectorsWidths.get(i);
        }

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
            if (e.getPropertyName() == IR_ChordSymbolSettings.PROP_FONT)
            {
                setFont(settings.getFont());
            } else if (e.getPropertyName() == IR_ChordSymbolSettings.PROP_FONT_COLOR
                    || e.getPropertyName() == IR_ChordSymbolSettings.PROP_FONT_ALT_COLOR)
            {
                ExtChordSymbol ecs = (ExtChordSymbol) getModel().getData();
                setForeground(ecs.getAlternateChordSymbol() == null ? settings.getColor() : settings.getAltColor());
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
     * Example : s=F7b9 will return list v[0] = F7, v1[1]=b v[2]=9
     */
    @SuppressWarnings("empty-statement")
    private java.util.List<String> splitStringAlt(String str)
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
