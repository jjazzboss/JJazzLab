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
package org.jjazz.cl_editorimpl.itemrenderer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.beans.PropertyChangeEvent;
import java.text.AttributedString;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.chordleadsheet.api.item.AltDataFilter;
import org.jjazz.chordleadsheet.api.item.AltExtChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.chordleadsheet.api.item.NCExtChordSymbol;
import org.jjazz.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_Copiable;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererSettings;
import org.jjazz.uiutilities.api.TextLayoutUtils;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.cl_editor.api.DisplayTransposableRenderer;

/**
 * An ItemRenderer for ChordSymbols.
 * <p>
 */
public class IR_ChordSymbol extends ItemRenderer implements IR_Copiable, DisplayTransposableRenderer
{

    private final static int OPTION_LINE_V_PADDING = 1;   // Additional space for the option line
    private final static int OPTION_LINE_THICKNESS = 1;   // Additional space for the option line
    private static final Logger LOGGER = Logger.getLogger(IR_ChordSymbol.class.getSimpleName());

    private boolean copyMode;
    private final IR_ChordSymbolSettings settings;
    private int zoomFactor = 50;
    private Timer timer;
    private Color optionLineColor;
    private int dislayTransposition;
    private ExtChordSymbol ecsModel;
    private String strChordReplacedAccidentals;
    private AttributedString attChordString;

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_ChordSymbol(CLI_ChordSymbol item, ItemRendererSettings irSettings)
    {
        super(item, IR_Type.ChordSymbol);
        LOGGER.log(Level.FINE, "IR_ChordSymbol() item={0}", item);

        dislayTransposition = 0;

        // Apply settings and listen to their changes
        settings = irSettings.getIR_ChordSymbolSettings();
        settings.addPropertyChangeListener(this);
        setFont(settings.getFont());

        // Listen to color change
        getModel().getClientProperties().addPropertyChangeListener(this);
        updateForegroundColor();


        modelChanged();
    }

    /**
     * Sets a transposition to be applied before rendering the chord.
     *
     * @param newTransposition
     */
    @Override
    public void setDisplayTransposition(int newTransposition)
    {
        dislayTransposition = newTransposition;
        modelChanged();
    }

    @Override
    public int getDisplayTransposition()
    {
        return dislayTransposition;
    }

    @Override
    public final void modelChanged()
    {
        ExtChordSymbol oldEcsModel = ecsModel;
        ecsModel = (ExtChordSymbol) getModel().getData();

        // The AttributedString used by getPreferredSize() and paintComponent() to represent the chord symbol
        attChordString = createAttrString(getPossiblyTransposedModel(), dislayTransposition > 0);

        updateToolTipText(getPossiblyTransposedModel());

        if (isFlashOptionLineRequired(oldEcsModel, ecsModel))
        {
            flashOptionLine();
        }

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
        // Prepare the graphics context
        Graphics2D g2 = (Graphics2D) getGraphics();
        assert g2 != null;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // The fonts to be used
        FontRenderContext frc = g2.getFontRenderContext();

        // Create the TextLayout to get its dimension       
        TextLayout textLayout = new TextLayout(attChordString.getIterator(), frc);
        int chordSymbolWidth = (int) TextLayoutUtils.getWidth(textLayout, strChordReplacedAccidentals, dislayTransposition == 0);
        int chordSymbolHeight = TextLayoutUtils.getHeight(textLayout, frc);

        Insets in = getInsets();
        final int PADDING = 1;
        int wFinal = chordSymbolWidth + 2 * PADDING + in.left + in.right; //  + (needOptionDots(ecs) ? CORNER_SIZE : 0);
        int hFinal = chordSymbolHeight + PADDING + OPTION_LINE_THICKNESS + 2 * OPTION_LINE_V_PADDING + in.top + in.bottom;
        Dimension dimension = new Dimension(wFinal, hFinal);

        LOGGER.log(Level.FINE, "evaluateTextRepresentation()    result d={0}   (insets={1})", new Object[]
        {
            dimension, in
        });

        return dimension;
    }

    @Override
    public void modelMoved()
    {
        updateToolTipText(getPossiblyTransposedModel());
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        getModel().getClientProperties().removePropertyChangeListener(this);
        settings.removePropertyChangeListener(this);
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
        float y = getHeight() - 1 - in.bottom - 2 * OPTION_LINE_V_PADDING - OPTION_LINE_THICKNESS + 1; // +1 needed ! (don't understand why)

        // Draw the chord symbol elements
        g2.drawString(attChordString.getIterator(), x, y);

        // Draw the copy indicator in upper right corner
        if (copyMode)
        {
            int size = IR_Copiable.CopyIndicator.getSideLength();
            Graphics2D gg2 = (Graphics2D) g2.create(Math.max(getWidth() - size - 1, 0), 1, size, size);
            IR_Copiable.CopyIndicator.drawCopyIndicator(gg2);
            gg2.dispose();
        }

        // Draw the option mark if needed
        if (needOptionMark(ecsModel))       // Does not depend on displayTransposition
        {
            int length = (int) Math.round(8 * (0.7f + 0.6 * zoomFactor / 100f));
            int x1 = getWidth() / 2 - length / 2;
            int x2 = getWidth() / 2 + length / 2;
            y = getHeight() - 1 - in.bottom - OPTION_LINE_V_PADDING;
            g2.setColor(optionLineColor);
            g2.drawLine(x1, (int) y, x2, (int) y);
        }
    }

    private boolean isFlashOptionLineRequired(ExtChordSymbol oldEcs, ExtChordSymbol ecs)
    {
        boolean b = false;

        if (oldEcs != null)
        {
            var oldCri = oldEcs.getRenderingInfo();
            var cri = ecs.getRenderingInfo();

            // Request attention if option mark was ON and remains ON and only one of the following option has changed:
            // crash/no crash/extended holdshot/scale/pedalBass/altChord
            b = oldCri != null
                    && oldEcs.getOriginalName().equals(ecs.getOriginalName())
                    && needOptionMark(ecs)
                    && needOptionMark(oldEcs)
                    && (oldEcs.getAlternateChordSymbol() != ecs.getAlternateChordSymbol()
                    || !Objects.equals(cri.getScaleInstance(), oldCri.getScaleInstance())
                    || cri.hasOneFeature(Feature.ACCENT_STRONGER) != oldCri.hasOneFeature(Feature.ACCENT_STRONGER)
                    || cri.hasOneFeature(Feature.PEDAL_BASS) != oldCri.hasOneFeature(Feature.PEDAL_BASS)
                    || cri.hasOneFeature(Feature.CRASH) != oldCri.hasOneFeature(Feature.CRASH)
                    || cri.hasOneFeature(Feature.NO_CRASH) != oldCri.hasOneFeature(Feature.NO_CRASH)
                    || cri.hasOneFeature(Feature.EXTENDED_HOLD_SHOT) != oldCri.hasOneFeature(Feature.EXTENDED_HOLD_SHOT));
        }

        return b;
    }

    public void flashOptionLine()
    {
        if (timer != null && timer.isRunning())
        {
            timer.stop();
            optionLineColor = getForeground();
        }

        final Color flashColor = Color.RED;

        if (timer == null)
        {
            // Create the timer
            ActionListener al = new ActionListener()
            {
                static final int NB_FLASH = 11;
                int count = NB_FLASH;

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (count % 2 == 1)     // 5 - 3 - 1
                    {
                        // setBackground(saveBackground);
                        optionLineColor = getForeground();
                        repaint();
                    } else
                    {
                        // setBackground(flashColor);
                        optionLineColor = flashColor;
                        repaint();
                    }
                    count--;
                    if (count == 0)
                    {
                        timer.stop();
                        count = NB_FLASH;
                    }
                }
            };
            timer = new Timer(60, al);
        }

        optionLineColor = flashColor;
        repaint();

        timer.restart();

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
            } else if (e.getPropertyName().equals(IR_ChordSymbolSettings.PROP_DEFAULT_FONT_COLOR))
            {
                updateForegroundColor();
            }
        } else if (e.getSource() == getModel().getClientProperties())
        {
            if (e.getPropertyName().equals(CL_EditorClientProperties.PROP_CHORD_USER_FONT_COLOR))
            {
                updateForegroundColor();
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

    //-------------------------------------------------------------------------------
    // Private methods
    //-------------------------------------------------------------------------------
    private boolean needOptionMark(ExtChordSymbol ecs)
    {
        var cri = ecs.getRenderingInfo();
        return (cri.getAccentFeature() != null && cri.hasOneFeature(Feature.ACCENT_STRONGER, Feature.CRASH, Feature.EXTENDED_HOLD_SHOT, Feature.NO_CRASH)
                || cri.getScaleInstance() != null
                || ecs.getAlternateChordSymbol() != null
                || cri.hasOneFeature(Feature.PEDAL_BASS));
    }

    /**
     * Update color depending on song property and settings.
     */
    private void updateForegroundColor()
    {
        Color c = getModel().getClientProperties().getColor(CL_EditorClientProperties.PROP_CHORD_USER_FONT_COLOR, settings.getColor());
        optionLineColor = c;
        setForeground(c);
    }

    private ExtChordSymbol getPossiblyTransposedModel()
    {
        ExtChordSymbol res = ecsModel;
        if (dislayTransposition != 0)
        {
            ExtChordSymbol ecs = (ExtChordSymbol) getModel().getData();
            res = ecs.getTransposedChordSymbol(dislayTransposition, null);
        }
        return res;
    }

    /**
     *
     * @param ecs
     */
    private void updateToolTipText(ExtChordSymbol ecs)
    {
        String tt = null;

        if (ecs instanceof NCExtChordSymbol)
        {
            tt = NCExtChordSymbol.DESCRIPTION;

        } else if (ecs != null)
        {
            // Chord Symbol
            ChordRenderingInfo cri = ecs.getRenderingInfo();
            StringBuilder sb = new StringBuilder(ecs.getChord().toRelativeNoteString(null));

            // Rendering info
            String criStr = cri.toUserString();
            if (!criStr.isBlank())
            {
                sb.append(" - ").append(criStr);
            }

            // Alt chord symbol if any
            AltExtChordSymbol altSymbol = ecs.getAlternateChordSymbol();
            if (altSymbol != null)
            {
                sb.append("   ").append(ResUtil.getString(getClass(), "IR_ChordSymbol.ALTERNATE")).append(": ");
                if (altSymbol == VoidAltExtChordSymbol.getInstance())
                {
                    sb.append(ResUtil.getString(getClass(), "IR_ChordSymbol.void"));
                } else
                {
                    sb.append(altSymbol);
                    cri = altSymbol.getRenderingInfo();
                    criStr = cri.toUserString();
                    if (!criStr.isBlank())
                    {
                        sb.append(" - ").append(criStr);
                    }
                }
                sb.append(" - ").append(ResUtil.getString(getClass(), "IR_ChordSymbol.condition")).append("=");
                AltDataFilter altFilter = ecs.getAlternateFilter();
                assert altFilter != null;
                sb.append(altFilter.isRandom() ? ResUtil.getString(getClass(), "IR_ChordSymbol.random") : altFilter.getValues());
            }
            tt = sb.toString();
        }

        setToolTipText(tt);
    }

    /**
     * Create the AttributedString used by paintComponent().
     * <p>
     * Also update strChordReplacedAccidentals used by getPreferredSize().
     *
     * @param ecs
     * @param isTransposed
     * @return
     */
    private AttributedString createAttrString(ExtChordSymbol ecs, boolean isTransposed)
    {
        Objects.requireNonNull(ecs);
        AttributedString res;


        // Prepare the strings making up the chord symbol : base extension [/bass]
        String chordSymbolBase;
        String chordSymbolExtension;
        String chordSymbolBass;
        if (ecs instanceof NCExtChordSymbol)
        {
            chordSymbolBase = ecs.getName();
            chordSymbolExtension = "";
        } else if (ecs.getName().equals(ecs.getOriginalName()))
        {
            // Easy
            chordSymbolBase = ecs.getRootNote().toRelativeNoteString() + ecs.getChordType().getBase();
            chordSymbolExtension = ecs.getChordType().getExtension();
        } else
        {
            // Chord symbol alias used, need to guess where the extension starts
            int rootNoteLength = ecs.getRootNote().toRelativeNoteString().length();
            String ctString = ecs.getOriginalName().substring(rootNoteLength).replaceFirst("/.*", "");  // Remove root note possible bass note
            int extStart = ChordTypeDatabase.getDefault().guessExtension(ctString);
            if (extStart == -1)
            {
                // No extension found
                chordSymbolBase = ecs.getRootNote().toRelativeNoteString() + ctString;
                chordSymbolExtension = "";
            } else
            {
                chordSymbolBase = ecs.getRootNote().toRelativeNoteString() + ctString.substring(0, extStart);
                chordSymbolExtension = ctString.substring(extStart);
            }
        }
        chordSymbolBass = "";
        if (!ecs.getBassNote().equalsRelativePitch(ecs.getRootNote()))
        {
            chordSymbolBass = "/" + ecs.getBassNote().toRelativeNoteString();
        }


        // The fonts to be used, depends on the zoom factor
        Font font = getFont();
        Font musicFont = settings.getMusicFont();
        float factor = 0.5f + (getZoomFactor() / 100f);
        float zFontSize = factor * font.getSize2D();
        zFontSize = Math.max(zFontSize, 12);


        // Create the AttributedString from the strings
        String strChord = chordSymbolBase + chordSymbolExtension + chordSymbolBass;
        strChordReplacedAccidentals = strChord
                .replace('#', settings.getSharpCharInMusicFont())
                .replace('b', settings.getFlatCharInMusicFont());
        res = new AttributedString(strChordReplacedAccidentals, font.getAttributes());
        res.addAttribute(TextAttribute.SIZE, zFontSize);                 // Override


        // Use the music font for all the # and b symbols
        for (int i = 0; i < strChord.length(); i++)
        {
            if (strChord.charAt(i) == '#' || strChord.charAt(i) == 'b')
            {
                res.addAttribute(TextAttribute.FAMILY, musicFont.getFontName(), i, i + 1);
            }
        }


        // Superscript for the extension
        if (!chordSymbolExtension.isEmpty())
        {
            res.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, chordSymbolBase.length(),
                    chordSymbolBase.length() + chordSymbolExtension.length());
        }


        // Italics if transposed
        Float fontPosture = !isTransposed ? TextAttribute.POSTURE_REGULAR : TextAttribute.POSTURE_OBLIQUE;
        res.addAttribute(TextAttribute.POSTURE, fontPosture);

        return res;
    }
}
