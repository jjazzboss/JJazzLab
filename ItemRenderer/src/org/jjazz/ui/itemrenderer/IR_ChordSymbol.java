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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.beans.PropertyChangeEvent;
import java.text.AttributedString;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.Timer;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.ChordTypeDatabase;
import org.jjazz.leadsheet.chordleadsheet.api.item.AltDataFilter;
import org.jjazz.leadsheet.chordleadsheet.api.item.AltExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.ui.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.ui.itemrenderer.api.IR_Copiable;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;
import org.jjazz.ui.itemrenderer.api.ItemRendererSettings;
import org.jjazz.ui.utilities.api.TextLayoutUtils;
import org.jjazz.util.api.ResUtil;

/**
 * An ItemRenderer for ChordSymbols.
 * <p>
 */
public class IR_ChordSymbol extends ItemRenderer implements IR_Copiable
{

    private final static int OPTION_LINE_V_PADDING = 1;   // Additional space for the option line
    private final static int OPTION_LINE_THICKNESS = 1;   // Additional space for the option line
    private AttributedString attChordString;
    private boolean copyMode;
    private final IR_ChordSymbolSettings settings;
    private int zoomFactor = 50;
    private String chordSymbolString;
    private String chordSymbolBase;
    private String chordSymbolExtension;
    private String chordSymbolBass;
    private ChordSymbol altChordSymbol;
    private int chordSymbolWidth;
    private int chordSymbolHeight;
    private ExtChordSymbol ecs;
    private ChordRenderingInfo cri;
    private Timer timer;
    private Color optionLineColor;
    private static final Logger LOGGER = Logger.getLogger(IR_ChordSymbol.class.getSimpleName());

    @SuppressWarnings("LeakingThisInConstructor")
    public IR_ChordSymbol(CLI_ChordSymbol item, ItemRendererSettings irSettings)
    {
        super(item, IR_Type.ChordSymbol);
        LOGGER.fine("IR_ChordSymbol() item=" + item);   //NOI18N


        // Apply settings and listen to their changes
        settings = irSettings.getIR_ChordSymbolSettings();
        settings.addPropertyChangeListener(this);
        setFont(settings.getFont());


        modelChanged();

    }

    @Override
    public void modelChanged()
    {
        // Save previous state
        ExtChordSymbol oldEcs = ecs;
        ChordRenderingInfo oldCri = cri;
        String oldChordSymbolString = chordSymbolString;
        ChordSymbol oldAltChordSymbol = altChordSymbol;


        // Update our state
        ecs = (ExtChordSymbol) getModel().getData();
        cri = ecs.getRenderingInfo();
        chordSymbolString = ecs.getOriginalName();
        altChordSymbol = ecs.getAlternateChordSymbol();


        // Get the strings making up the chord symbol : base extension [/bass]
        if (ecs.getName().equals(chordSymbolString))
        {
            // Easy
            chordSymbolBase = ecs.getRootNote().toRelativeNoteString() + ecs.getChordType().getBase();
            chordSymbolExtension = ecs.getChordType().getExtension();
        } else
        {
            // Chord symbol alias used, need to guess where the extension starts
            int rootNoteLength = ecs.getRootNote().toRelativeNoteString().length();
            String ctString = chordSymbolString.substring(rootNoteLength).replaceFirst("/.*", "");  // Remove root note possible bass note
            int extStart = ChordTypeDatabase.getInstance().guessExtension(ctString);
            if (extStart == -1)
            {
                // No extension found, use major chord symbol by default
                chordSymbolBase = ecs.getRootNote().toRelativeNoteString();
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


        // Update UI
        setForeground(cri.getAccentFeature() == null ? settings.getColor() : settings.getAccentColor(cri.getAccentFeature()));
        optionLineColor = getForeground();
        updateToolTipText();
        revalidate();
        repaint();


        // Request attention if option mark was ON and remains ON and only one of the following option has changed:
        // crash/no crash/extended holdshot/scale/pedalBass/altChord
        if (oldCri != null
                && oldChordSymbolString.equals(chordSymbolString)
                && needOptionMark(ecs, cri)
                && needOptionMark(oldEcs, oldCri)
                && (oldAltChordSymbol != altChordSymbol
                || !Objects.equals(cri.getScaleInstance(), oldCri.getScaleInstance())
                || cri.hasOneFeature(Feature.PEDAL_BASS) != oldCri.hasOneFeature(Feature.PEDAL_BASS)
                || cri.hasOneFeature(Feature.CRASH) != oldCri.hasOneFeature(Feature.CRASH)
                || cri.hasOneFeature(Feature.NO_CRASH) != oldCri.hasOneFeature(Feature.NO_CRASH)
                || cri.hasOneFeature(Feature.EXTENDED_HOLD_SHOT) != oldCri.hasOneFeature(Feature.EXTENDED_HOLD_SHOT)))
        {
            // UI won't be updated so request attention to assure user that something happened indeed
            flashOptionLine();
        }
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
        assert g2 != null;   //NOI18N
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


        // Create the AttributedString from the strings
        String strChord = chordSymbolBase + chordSymbolExtension + chordSymbolBass;
        String strChord2 = strChord.replace('#', settings.getSharpCharInMusicFont()).replace('b', settings.getFlatCharInMusicFont());
        attChordString = new AttributedString(strChord2, font.getAttributes());
        attChordString.addAttribute(TextAttribute.SIZE, zFontSize);                 // Override
//        if (needOptionDots())
//        {
//            attChordString.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);      // Default attribute
//        }

        // Use the music font for all the # and b symbols
        for (int i = 0; i < strChord.length(); i++)
        {
            if (strChord.charAt(i) == '#' || strChord.charAt(i) == 'b')
            {
                attChordString.addAttribute(TextAttribute.FAMILY, musicFont.getFontName(), i, i + 1);
            }
        }

        // Superscript for the extension
        if (!chordSymbolExtension.isEmpty())
        {
            attChordString.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, chordSymbolBase.length(), chordSymbolBase.length() + chordSymbolExtension.length());
        }


        // Create the TextLayout to get its dimension       
        TextLayout textLayout = new TextLayout(attChordString.getIterator(), frc);
        chordSymbolWidth = (int) TextLayoutUtils.getWidth(textLayout, strChord2, false);
        chordSymbolHeight = TextLayoutUtils.getHeight(textLayout, frc);
        Insets in = getInsets();
        final int PADDING = 1;
        int wFinal = chordSymbolWidth + 2 * PADDING + in.left + in.right; //  + (needOptionDots(ecs) ? CORNER_SIZE : 0);
        int hFinal = chordSymbolHeight + PADDING + OPTION_LINE_THICKNESS + 2 * OPTION_LINE_V_PADDING + in.top + in.bottom;
        Dimension d = new Dimension(wFinal, hFinal);


        LOGGER.fine("getPreferredSize()    result d=" + d + "   (insets=" + in + ")");   //NOI18N


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
        if (ecs != null)
        {
            // Chord Symbol
            ChordRenderingInfo cri = ecs.getRenderingInfo();
            StringBuilder sb = new StringBuilder(ecs.getChord().toRelativeNoteString(ecs.getRootNote().getAlterationDisplay()));


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
                assert altFilter != null;   //NOI18N
                sb.append(altFilter.isRandom() ? ResUtil.getString(getClass(), "IR_ChordSymbol.random") : altFilter.getValues());
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
        if (needOptionMark(ecs, cri))
        {
            int length = (int) Math.round(8 * (0.7f + 0.6 * zoomFactor / 100f));
            int x1 = getWidth() / 2 - length / 2;
            int x2 = getWidth() / 2 + length / 2;
            y = getHeight() - 1 - in.bottom - OPTION_LINE_V_PADDING;
            g2.setColor(optionLineColor);
            g2.drawLine(x1, (int) y, x2, (int) y);
        }
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
            } else if (e.getPropertyName().equals(IR_ChordSymbolSettings.PROP_FONT_COLOR)
                    || e.getPropertyName().equals(IR_ChordSymbolSettings.PROP_FONT_ACCENT_COLOR))
            {
                setForeground(cri.getAccentFeature() == null ? settings.getColor() : settings.getAccentColor(cri.getAccentFeature()));
                optionLineColor = getForeground();
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

    private boolean needOptionMark(ExtChordSymbol extCs, ChordRenderingInfo cri)
    {
        return ((cri.getAccentFeature() != null && cri.hasOneFeature(Feature.CRASH, Feature.EXTENDED_HOLD_SHOT, Feature.NO_CRASH))
                || cri.getScaleInstance() != null
                || extCs.getAlternateChordSymbol() != null
                || cri.hasOneFeature(Feature.PEDAL_BASS));
    }

}
