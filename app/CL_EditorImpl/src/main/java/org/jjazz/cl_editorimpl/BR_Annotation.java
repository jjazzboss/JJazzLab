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
package org.jjazz.cl_editorimpl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import org.jjazz.cl_editor.itemrenderer.api.IR_AnnotationTextSettings;
import org.jjazz.cl_editor.spi.BarRendererSettings;
import org.jjazz.cl_editorimpl.itemrenderer.IR_AnnotationText;
import org.jjazz.cl_editor.itemrenderer.api.IR_Copiable;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererFactory;
import org.jjazz.uiutilities.api.CornerLayout;
import org.jjazz.uiutilities.api.StringMetrics;

/**
 * A BarRenderer to show an annotation.
 */
public class BR_Annotation extends BarRenderer
{

    private static final Dimension MIN_SIZE = new Dimension(10, 4);

    /**
     * The ItemRenderer to show the insertion point.
     */
    private ItemRenderer insertionPointRenderer;
    private int zoomVFactor;
    private int nbLines;

    private static final Logger LOGGER = Logger.getLogger(BR_Annotation.class.getSimpleName());


    @SuppressWarnings("LeakingThisInConstructor")
    public BR_Annotation(CL_Editor editor, int barIndex, BarRendererSettings settings, ItemRendererFactory irf)
    {
        super(editor, barIndex, settings, irf);
        this.zoomVFactor = 50;
        nbLines = CL_EditorClientProperties.getBarAnnotationNbLines(getEditor().getSongModel());

        // Our layout manager
        setLayout(new AnnotationLayoutManager());

        setMinimumSize(MIN_SIZE);

    }

    public void setNbLines(int n)
    {
        nbLines = n;
        for (ItemRenderer ir : getItemRenderers())
        {
            if (ir instanceof IR_AnnotationText irAt)
            {
                irAt.setNbLines(n);
            }
        }
        revalidate();
        repaint();
    }

    public int getNbLines()
    {
        return nbLines;
    }

    /**
     * Overridden to unregister the pref size panel shared instance.
     */
    @Override
    public void cleanup()
    {
        super.cleanup();
        getEditor().removePropertyChangeListener(this);
    }

    @Override
    public void moveItemRenderer(ChordLeadSheetItem<?> item)
    {
        throw new IllegalStateException("item=" + item);
    }

    /**
     *
     * @param cliSection Can be null
     */
    @Override
    public void setSection(CLI_Section cliSection)
    {
        // Nothing
    }

    @Override
    public void showInsertionPoint(boolean b, ChordLeadSheetItem<?> item, Position pos, boolean copyMode)
    {
        LOGGER.log(Level.FINE, "showInsertionPoint() b={0} item={1} pos={2}", new Object[]
        {
            b, item, pos
        });
        if (b)
        {
            if (insertionPointRenderer == null)
            {
                insertionPointRenderer = addItemRenderer(item);
                insertionPointRenderer.setSelected(true);
            }
            if (insertionPointRenderer instanceof IR_Copiable irc)
            {
                irc.showCopyMode(copyMode);
            }
        } else
        {
            removeItemRenderer(insertionPointRenderer);
            insertionPointRenderer = null;
        }

        if (insertionPointRenderer instanceof IR_Copiable irc)
        {
            irc.showCopyMode(copyMode);
        }
    }

    @Override
    public void showPlaybackPoint(boolean b, Position pos)
    {
        // Do nothing
    }

    /**
     * Vertical zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    @Override
    public void setZoomVFactor(int factor)
    {
        if (zoomVFactor == factor)
        {
            return;
        }

        zoomVFactor = factor;
        for (ItemRenderer ir : getItemRenderers())
        {
            ir.setZoomFactor(factor);
        }
        revalidate();
        repaint();
    }

    @Override
    public int getZoomVFactor()
    {
        return zoomVFactor;
    }

    @Override
    public String toString()
    {
        return "BR_Annotation[" + getBarIndex() + "]";
    }

    @Override
    public boolean isRegisteredItemClass(ChordLeadSheetItem<?> item)
    {
        return item instanceof CLI_BarAnnotation;
    }

    @Override
    protected ItemRenderer createItemRenderer(ChordLeadSheetItem<?> item)
    {
        if (!isRegisteredItemClass(item))
        {
            throw new IllegalArgumentException("item=" + item);
        }
        assert item instanceof CLI_BarAnnotation;
        IR_AnnotationText irAt = (IR_AnnotationText) getItemRendererFactory().createItemRenderer(IR_Type.BarAnnotationText,
                item,
                getSettings().getItemRendererSettings());
        irAt.setNbLines(nbLines);
        return irAt;
    }

    /**
     * Overridden to draw an almost transparent background (component is not opaque).
     *
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Color c = new Color(127, 127, 127, 30);
        g.setColor(c);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    // ---------------------------------------------------------------
    // Private methods
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // Inner classes
    // ---------------------------------------------------------------
    /**
     * Special layout for annotations.
     * <p>
     * Layout the first component in the top left corner, using all available width and preferred height. If there is 2nd component it's an insertion point,
     * then put each component on left/right sides.
     */
    public class AnnotationLayoutManager implements LayoutManager
    {

        private static final int PADDING = 0;

        @Override
        public void layoutContainer(Container container)
        {
            Insets in = container.getInsets();
            int xLeft = in.left + PADDING;
            int yTop = in.top + PADDING;
            int w = getWidth() - xLeft - in.right - PADDING;

            var irs = getItemRenderers();
            assert irs.size() < 3 : "irs=" + irs;

            if (!irs.isEmpty())
            {
                var ir = irs.getFirst();
                var pd = ir.getPreferredSize();
                ir.setSize(w, pd.height);
                ir.setLocation(xLeft, yTop);

                if (irs.size() == 2)
                {
                    ir.setSize(w / 2, pd.height);
                    ir = irs.getLast();
                    ir.setSize(w / 2, pd.height);
                    ir.setLocation(xLeft + w / 2, yTop);
                }
            }


        }

        @Override
        public void addLayoutComponent(String corner, Component comp)
        {
            // Nothing
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
            // Nothing
        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            Dimension d = parent.getSize();

            var irs = getItemRenderers();
            if (irs.isEmpty())
            {
                var font = IR_AnnotationTextSettings.getDefault().getFont();
                StringMetrics sm = StringMetrics.create(font);
                d.height = (int) Math.ceil(nbLines * (sm.getHeight("|") + 1));
            } else
            {
                var pd = irs.getFirst().getPreferredSize();
                d.height = pd.height;
            }
            return d;
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return new Dimension();
        }

    }
}
