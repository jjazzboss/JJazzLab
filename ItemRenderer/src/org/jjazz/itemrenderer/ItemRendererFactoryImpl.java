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
package org.jjazz.itemrenderer;

import org.jjazz.itemrenderer.api.IR_AnnotationText;
import org.jjazz.itemrenderer.api.IR_Section;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.itemrenderer.api.IR_Type;
import org.jjazz.itemrenderer.api.ItemRenderer;
import org.jjazz.itemrenderer.api.ItemRendererFactory;
import org.jjazz.itemrenderer.api.ItemRendererSettings;

public class ItemRendererFactoryImpl implements ItemRendererFactory
{

    private static ItemRendererFactoryImpl INSTANCE;
    private static IR_ChordSymbol SAMPLE_CHORD_SYMBOL_RENDERER;
    private static IR_Section SAMPLE_SECTION_RENDERER;
    private static IR_ChordPosition SAMPLE_CHORD_POSITION_RENDERER;
    private static IR_TimeSignature SAMPLE_TIME_SIGNATURE_RENDERER;
    private static IR_PositionMark SAMPLE_POSITION_MARK_RENDERER;
    private static IR_PaperNote SAMPLE_PAPER_NOTE_RENDERER;
    private static IR_AnnotationText SAMPLE_ANNOTATION_TEXT_RENDERER;
    private static final Logger LOGGER = Logger.getLogger(ItemRendererFactoryImpl.class.getName());

    public static ItemRendererFactoryImpl getInstance()
    {
        synchronized (ItemRendererFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ItemRendererFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private ItemRendererFactoryImpl()
    {
    }

    @Override
    public ItemRenderer createItemRenderer(IR_Type type, ChordLeadSheetItem<?> item, ItemRendererSettings settings)
    {
        ItemRenderer ir = switch (type)
        {
            case ChordSymbol ->
                new IR_ChordSymbol((CLI_ChordSymbol) item, settings);
            case ChordPosition ->
                new IR_ChordPosition((CLI_ChordSymbol) item, settings);
            case Section ->
                new IR_Section((CLI_Section) item, settings);
            case TimeSignature ->
                new IR_TimeSignature((CLI_Section) item, settings);
            case PositionMark ->
                new IR_PositionMark(item);
            case BarAnnotationPaperNote ->
                new IR_PaperNote((CLI_BarAnnotation) item, settings);
            case BarAnnotationText ->
                new IR_AnnotationText((CLI_BarAnnotation) item, settings);
            default -> throw new IllegalArgumentException("type=" + type + " item=" + item);
        };
        return ir;
    }

    @Override
    public ItemRenderer getItemRendererSample(IR_Type type, ItemRendererSettings settings)
    {
        ItemRenderer ir;
        CLI_Factory cliFactory = CLI_Factory.getDefault();
        switch (type)
        {
            case ChordSymbol ->
            {
                if (SAMPLE_CHORD_SYMBOL_RENDERER == null)
                {
                    SAMPLE_CHORD_SYMBOL_RENDERER = new IR_ChordSymbol(cliFactory.getSampleChordSymbol(), settings);
                }
                ir = SAMPLE_CHORD_SYMBOL_RENDERER;
            }
            case ChordPosition ->
            {
                if (SAMPLE_CHORD_POSITION_RENDERER == null)
                {
                    SAMPLE_CHORD_POSITION_RENDERER = new IR_ChordPosition(cliFactory.getSampleChordSymbol(), settings);
                }
                ir = SAMPLE_CHORD_POSITION_RENDERER;
            }
            case Section ->
            {
                if (SAMPLE_SECTION_RENDERER == null)
                {
                    SAMPLE_SECTION_RENDERER = new IR_Section(cliFactory.getSampleSection(), settings);
                }
                ir = SAMPLE_SECTION_RENDERER;
            }
            case TimeSignature ->
            {
                if (SAMPLE_TIME_SIGNATURE_RENDERER == null)
                {
                    SAMPLE_TIME_SIGNATURE_RENDERER = new IR_TimeSignature(cliFactory.getSampleSection(), settings);
                }
                ir = SAMPLE_TIME_SIGNATURE_RENDERER;
            }
            case PositionMark ->
            {
                if (SAMPLE_POSITION_MARK_RENDERER == null)
                {
                    SAMPLE_POSITION_MARK_RENDERER = new IR_PositionMark(cliFactory.getSampleSection());
                }
                ir = SAMPLE_POSITION_MARK_RENDERER;
            }
            case BarAnnotationPaperNote ->
            {
                if (SAMPLE_PAPER_NOTE_RENDERER == null)
                {
                    SAMPLE_PAPER_NOTE_RENDERER = new IR_PaperNote(cliFactory.createBarAnnotation(null, "edit me", 0), settings);
                }
                ir = SAMPLE_PAPER_NOTE_RENDERER;
            }
            case BarAnnotationText ->
            {
                if (SAMPLE_ANNOTATION_TEXT_RENDERER == null)
                {
                    SAMPLE_ANNOTATION_TEXT_RENDERER = new IR_AnnotationText(cliFactory.createBarAnnotation(null, "Some LYRICS!", 0),
                            settings);
                }
                ir = SAMPLE_ANNOTATION_TEXT_RENDERER;
            }
            default ->
                throw new IllegalArgumentException("type=" + type);
        }
        return ir;
    }

    @Override
    public ItemRenderer createDraggedItemRenderer(IR_Type type, ChordLeadSheetItem<?> item, ItemRendererSettings settings)
    {
        // Use default implementations, but we could imagine special renderers for dragged items.
        ItemRenderer ir = switch (type)
        {
            case ChordSymbol ->
                new IR_ChordSymbol((CLI_ChordSymbol) item, settings);
            case Section ->
                new IR_Section((CLI_Section) item, settings);
            case TimeSignature ->
                new IR_TimeSignature((CLI_Section) item, settings);
            default ->
                null;
        };
        return ir;
    }
}
