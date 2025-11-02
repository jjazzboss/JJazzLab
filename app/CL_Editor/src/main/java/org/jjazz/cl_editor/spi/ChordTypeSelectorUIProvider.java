/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.cl_editor.spi;

import java.util.function.Consumer;
import javax.swing.JComponent;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.ChordType;
import org.openide.util.Lookup;

/**
 * Provide a UI component to select a chord type for a CLI_ChordSymbol.
 */
public interface ChordTypeSelectorUIProvider
{
    /**
     * Search an implementation in the global context.
     *
     * @return Can be null
     */
    public static ChordTypeSelectorUIProvider getDefault()
    {
        ChordTypeSelectorUIProvider result = Lookup.getDefault().lookup(ChordTypeSelectorUIProvider.class);
        return result;
    }


    /**
     * Get the UI component to select a chord type.
     *
     * @param chordSymbol     The chord symbol to initialize the UI. Can be null.
     * @param chordTypeSetter The consumer to be called when user selected a chord type. A null parameter indicates that user cancelled the action.
     * @return
     */
    JComponent getUI(ChordSymbol chordSymbol, Consumer<ChordType> chordTypeSetter);
}
