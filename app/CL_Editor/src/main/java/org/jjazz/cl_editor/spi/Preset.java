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
package org.jjazz.cl_editor.spi;

import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * A CL_BarEditorDialog can be preset depending on the way it has been triggered.
 * <p>
 * Possibilities:<br>
 * - bar edit action <br>
 * - ChordSymbol edit action <br>
 * - TimeSignature edit action <br>
 * - Section's name edit action<br>
 * - bar annotation edit action<br>
 */
public class Preset
{

    public enum Type
    {
        BarEdit, ChordSymbolEdit, TimeSignatureEdit, SectionNameEdit, AnnotationEdit
    }
    private final Type presetType;
    private final ChordLeadSheetItem<?> item;
    private final char key;

    /**
     * Create the preset.
     *
     * @param pt
     * @param item can be null if pt=BarEdit
     * @param key if != 0, indicates the edit action was triggered by this key press.
     */
    public Preset(Type pt, ChordLeadSheetItem<?> item, char key)
    {
        presetType = pt;
        this.item = item;
        this.key = key;
    }

    public Type getPresetType()
    {
        return presetType;
    }

    public ChordLeadSheetItem<?> getItem()
    {
        return item;
    }

    public char getKey()
    {
        return key;
    }

    @Override
    public String toString()
    {
        return "Preset[" + presetType + " item=" + item + " key=" + key + "]";
    }
}
