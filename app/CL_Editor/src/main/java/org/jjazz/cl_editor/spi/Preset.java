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

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * How a CL_BarEditorDialog can be preset.
 *
 * @param type
 * @param item can be null only when type=BarEdit or AnnotationEdit
 * @param key  if != 0, indicates the edit action was triggered by this key press
 */
public record Preset(Preset.Type type, ChordLeadSheetItem<?> item, char key)
        {

    public enum Type
    {
        BarEdit, ChordSymbolEdit, TimeSignatureEdit, SectionNameEdit, AnnotationEdit
    }

    public Preset
    {
        Objects.requireNonNull(type);
        Preconditions.checkArgument(item != null || (type == Type.BarEdit || type == Type.AnnotationEdit), "%s", this);
    }
}
