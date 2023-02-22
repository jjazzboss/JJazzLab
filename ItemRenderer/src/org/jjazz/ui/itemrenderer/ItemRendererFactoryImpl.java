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

import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.ui.itemrenderer.api.IR_Type;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;
import org.jjazz.ui.itemrenderer.api.ItemRendererFactory;
import org.jjazz.ui.itemrenderer.api.ItemRendererSettings;

public class ItemRendererFactoryImpl implements ItemRendererFactory
{

    private static ItemRendererFactoryImpl INSTANCE;
    private static IR_ChordSymbol SAMPLE_CHORD_SYMBOL_RENDERER;
    private static IR_Section SAMPLE_SECTION_RENDERER;
    private static IR_ChordPosition SAMPLE_CHORD_POSITION_RENDERER;
    private static IR_TimeSignature SAMPLE_TIME_SIGNATURE_RENDERER;
    private static IR_PositionMark SAMPLE_POSITION_MARK_RENDERER;
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
    public ItemRenderer createItemRenderer(IR_Type type, ChordLeadSheetItem<?> item ,ItemRendererSettings settings)
    {
        ItemRenderer ir;
        switch (type)
        {
            case ChordSymbol:
                ir = new IR_ChordSymbol((CLI_ChordSymbol) item, settings);
                break;
            case ChordPosition:
                ir = new IR_ChordPosition((CLI_ChordSymbol) item, settings);
                break;
            case Section:
                ir = new IR_Section((CLI_Section) item, settings);
                break;
            case TimeSignature:
                ir = new IR_TimeSignature((CLI_Section) item, settings);
                break;
            case PositionMark:
                ir = new IR_PositionMark(item);
                break;
            default:
                throw new IllegalArgumentException("type=" + type + " item=" + item);   
        }
        return ir;
    }

    @Override
    public ItemRenderer getItemRendererSample(IR_Type type,ItemRendererSettings settings)
    {
        ItemRenderer ir;
        CLI_Factory cliFactory = CLI_Factory.getDefault();
        switch (type)
        {
            case ChordSymbol:
                if (SAMPLE_CHORD_SYMBOL_RENDERER==null)
                {
                    SAMPLE_CHORD_SYMBOL_RENDERER = new IR_ChordSymbol(cliFactory.getSampleChordSymbol(), settings);
                }
                ir = SAMPLE_CHORD_SYMBOL_RENDERER;
                
                break;
            case ChordPosition:                
                if (SAMPLE_CHORD_POSITION_RENDERER==null)
                {
                        SAMPLE_CHORD_POSITION_RENDERER = new IR_ChordPosition(cliFactory.getSampleChordSymbol(), settings);
                }
                ir = SAMPLE_CHORD_POSITION_RENDERER;
                break;
            case Section:
                if (SAMPLE_SECTION_RENDERER==null)
                {
                    SAMPLE_SECTION_RENDERER = new IR_Section(cliFactory.getSampleSection(), settings);                
                }
                ir = SAMPLE_SECTION_RENDERER;
                break;
            case TimeSignature:
                if (SAMPLE_TIME_SIGNATURE_RENDERER == null)
                {
                    SAMPLE_TIME_SIGNATURE_RENDERER = new IR_TimeSignature(cliFactory.getSampleSection(), settings);
                }
                ir = SAMPLE_TIME_SIGNATURE_RENDERER;
                break;
            case PositionMark:
                if (SAMPLE_POSITION_MARK_RENDERER == null)
                {
                    SAMPLE_POSITION_MARK_RENDERER = new IR_PositionMark(cliFactory.getSampleSection());
                }
                ir = SAMPLE_POSITION_MARK_RENDERER;
                break;
            default:
                throw new IllegalArgumentException("type=" + type);   
        }
        return ir;
    }

    @Override
    public ItemRenderer createDraggedItemRenderer(IR_Type type, ChordLeadSheetItem<?> item,ItemRendererSettings settings)
    {
        // Use default implementations, but we could imagine special renderers for dragged items.
        ItemRenderer ir;
        switch (type)
        {
            case ChordSymbol:
                ir = new IR_ChordSymbol((CLI_ChordSymbol) item, settings);
                break;
            case Section:
                ir = new IR_Section((CLI_Section) item, settings);
                break;
            case TimeSignature:
                ir = new IR_TimeSignature((CLI_Section) item, settings);
                break;
            default:
                ir = null;
        }
        return ir;
    }
}
