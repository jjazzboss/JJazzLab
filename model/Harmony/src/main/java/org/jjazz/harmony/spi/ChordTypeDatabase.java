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
package org.jjazz.harmony.spi;

import java.util.List;
import org.jjazz.harmony.ChordTypeDatabaseImpl;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.api.Degree;
import org.openide.util.Lookup;

/**
 * Manage the list of recognized chordtypes and their aliases.
 * <p>
 * Used to retrieve instances of ChordTypes (which are unmutable). User can alter aliases which are saved as preferences.
 */
public interface ChordTypeDatabase
{

    public class InvalidAliasException extends Exception
    {

        public InvalidAliasException(String msg)
        {
            super(msg);
        }
    }

    /**
     * Return the first implementation available in the global lookup,otherwise use the default implementation.
     *
     * @return
     */
    static public ChordTypeDatabase getDefault()
    {
        var res = Lookup.getDefault().lookup(ChordTypeDatabase.class);
        if (res == null)
        {
            res = ChordTypeDatabaseImpl.getInstance();
        }
        return res;
    }

    /**
     * Add an alias for the specified chord type.
     * <p>
     * An alias can be used by only one chord type.
     *
     * @param ct
     * @param alias e.g. "-7" for the "m7" chord type
     * @throws IllegalArgumentException If ct is not part of this database
     * @throws InvalidAliasException    If alias is invalid, e.g. it's already used by a different chord type.
     */
    void addAlias(ChordType ct, String alias) throws InvalidAliasException;

    /**
     * Get the aliases of the specified chord type.
     * <p>
     * Use the aliases stored in the preferences if available, otherwise return the default aliases.
     *
     * @param ct
     * @return
     */
    List<String> getAliases(ChordType ct);

    /**
     * Get a chord type from the database.
     *
     * @param i The index of the chord type
     * @return
     */
    ChordType getChordType(int i);

    /**
     * Get a chord type from the database from a String description.
     * <p>
     * The String must match the chord type name or one of its aliases.
     *
     * @param s The String, e.g. "m7".
     * @return A ChordType, null if no ChordType correspond to s.
     */
    ChordType getChordType(String s);

    /**
     * Get the ChordType which match the specified degrees.
     *
     * @param degrees
     * @return Can be null
     */
    ChordType getChordType(List<Degree> degrees);

    /**
     * The index of the ct in the database.
     *
     * @param ct
     * @return -1 if ct is not present in the database.
     */
    int getChordTypeIndex(ChordType ct);

    /**
     * Get all the ChordTypes of the database.
     *
     * @return An unmodifiable list.
     */
    List<ChordType> getChordTypes();

    /**
     * Get the number of chord types in the database.
     *
     * @return
     */
    int getSize();

    /**
     * Try to guess where the extension part of a chord type string starts.
     * <p>
     * For example for "madd9", return 1 because base=m and extension="add9". Should be used only if a ChordType.getOriginalName() differs from
     * ChordType.getName().
     *
     * @param ctStr A chord type string like "", "sus7", "7dim7M", "Maj7aug", "madd9", etc.
     * @return The index of the first char of the extension. -1 if no extension found.
     */
    int guessExtension(String ctStr);

    void resetAliases(ChordType ct);

    /**
     * Clear all the user changes.
     */
    void resetAliasesToDefault();

}
