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
package org.jjazz.chordleadsheet.api.item;

import java.awt.datatransfer.DataFlavor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.harmony.api.Position;

/**
 * Represent a section with a name (e.g. "Verse1") and a time signature, at a specific bar.
 * <p>
 */
public interface CLI_Section extends ChordLeadSheetItem<Section>
{

    public final static int POSITION_ORDER = 0;

    public static final DataFlavor DATA_FLAVOR = new DataFlavor(CLI_Section.class, "Section");

    static Pattern PATTERN = Pattern.compile("-([0-9]+)$");


    /**
     * Same as getCopy() except that we possibly rename section if its name clashes with an existing section in cls.
     * 
     * ClientProperties are also copied.
     *
     * @param newPos
     * @param cls    If null, section name is directly reused.
     * @return
     * @see ChordLeadSheetItem#getCopy(java.lang.Object, org.jjazz.harmony.api.Position) 
     */
    CLI_Section getCopy(Position newPos, ChordLeadSheet cls);


    /**
     * Derive a new section name which is unique in the specified chord leadsheet.
     * <p>
     * If sectionName is not used, just return it. Otherwise append a number to sectionName (eg "Chorus-2") until we get a non-used section name.
     *
     * @param sectionName Create a name from this parameter.
     * @param cls         If null just returns sectionName
     * @return
     */
    static public String createSectionName(String sectionName, ChordLeadSheet cls)
    {
        if (cls == null || cls.getSection(sectionName) == null)
        {
            return sectionName;
        }
        // Find possible index at the end of the string
        Matcher m = PATTERN.matcher(sectionName);
        int index = 0;
        StringBuilder baseName = new StringBuilder(sectionName);
        if (m.find())
        {
            index = Integer.parseInt(m.group(1));
            baseName.delete(m.start(), m.end());
        }
        int robustness = 1000;
        String newName;
        do
        {
            robustness--;
            index++;
            newName = baseName.toString() + "-" + index;
        } while (cls.getSection(newName) != null && robustness > 0);

        if (robustness == 0)
        {
            throw new IllegalStateException("createSectionName() sectionName=" + sectionName + " robustness="
                    + robustness);
        }
        return newName;
    }
}
