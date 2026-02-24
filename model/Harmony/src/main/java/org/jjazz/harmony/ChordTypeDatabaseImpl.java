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
package org.jjazz.harmony;

import java.util.*;
import java.util.logging.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.api.Degree;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.*;

/**
 * Default implementation.
 */
public class ChordTypeDatabaseImpl implements ChordTypeDatabase
{

    private static ChordTypeDatabaseImpl INSTANCE;
    private static final String PREFIX = "CT_";


    private final List<ChordType> chordTypes = new ArrayList<>();
    private final HashMap<ChordType, String> mapCtDefaultAliases = new HashMap<>();
    private HashMap<String, ChordType> mapAliasCt = new HashMap<>(450);     // Try to avoid rehash
    private final HashMap<String, Integer> mapExtensionIndex = new HashMap<>();
    private final HashMap<String, Integer> mapExtensionIndexIgnoreCase = new HashMap<>(40);

    protected static final Preferences prefs = NbPreferences.forModule(ChordTypeDatabase.class);
    private static final Logger LOGGER = Logger.getLogger(ChordTypeDatabaseImpl.class.getSimpleName());

    public static ChordTypeDatabaseImpl getInstance()
    {
        synchronized (ChordTypeDatabaseImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ChordTypeDatabaseImpl();
            }
        }
        return INSTANCE;
    }

    private ChordTypeDatabaseImpl()
    {
        // Just to make lines shorter !
        int NP = ChordType.NOT_PRESENT;
        ChordType.Family MAJ = ChordType.Family.MAJOR;
        ChordType.Family MIN = ChordType.Family.MINOR;
        ChordType.Family SEV = ChordType.Family.SEVENTH;
        ChordType.Family DIM = ChordType.Family.DIMINISHED;
        ChordType.Family SUS = ChordType.Family.SUS;


        // MAJOR
        addBuiltin("", "", MAJ, ":M:maj:MAJ:Maj:bass:Bass:BASS:1+8:1+5:5:", NP, 0, NP, 0, NP, NP);
        addBuiltin("+", "", MAJ, ":maj#5:maj+5:M#5:ma#5:ma+5:aug:", NP, 0, NP, 1, NP, NP);
        addBuiltin("6", "", MAJ, ":maj6:MAJ6:Maj6:M6:", NP, 0, NP, 0, 0, NP);
        addBuiltin("6", "9", MAJ, ":M69:ma69:maj69:MAJ69:Maj6(9):", 0, 0, NP, 0, 0, NP);
        addBuiltin("M7", "", MAJ, ":7M:maj7:ma7:MAJ7:Maj7:", NP, 0, NP, 0, NP, 0);
        addBuiltin("M7", "13", MAJ, ":maj713:ma713:MAJ713:M7add13:", NP, 0, NP, 0, 0, 0);
        addBuiltin("M9", "", MAJ, ":9M:maj79:maj9:ma79:MAJ79:Maj9:Maj(9):Maj7(9):Maj9(no3):", 0, 0, NP, 0, NP, 0);
        addBuiltin("M13", "", MAJ, ":ma13:maj13:MAJ13:13M:Maj13:", 0, 0, NP, 0, 0, 0);
        addBuiltin("M7", "b5", MAJ, ":maj7b5:maj-5:Mb5:7M-5:7Mb5:ma7b5:ma-5:b5:Maj7b5:", NP, 0, NP, -1, NP, 0);
        addBuiltin("M7", "#5", MAJ, ":maj7#5:7M+5:7M#5:ma7#5:Maj7aug:Maj7#5:", NP, 0, NP, +1, NP, 0);
        addBuiltin("M7", "#11", MAJ, ":maj7#11:7M#11:Maj7#11:ma7#11:", NP, 0, +1, 0, NP, 0);
        addBuiltin("M9", "#11", MAJ, ":maj9#11:9M#11:ma9#11:Maj9#11:Lyd:lyd:Maj7Lyd:7Mlyd:M7lyd:", 0, 0, +1, 0, NP, 0);
        addBuiltin("M13", "#11", MAJ, ":maj13#11:13M#11:ma13#11:Maj13#11:", 0, 0, +1, 0, 0, 0);

        // SEVENTH
        addBuiltin("7", "", SEV, ":7th:", NP, 0, NP, 0, NP, -1);
        addBuiltin("9", "", SEV, ":79:7(9):", 0, 0, NP, 0, NP, -1);
        addBuiltin("13", "", SEV, ":713:7(13):7add6:7add13:67:", NP, 0, NP, 0, 0, -1);
        addBuiltin("7", "b5", SEV, ":7-5:", NP, 0, NP, -1, NP, -1);
        addBuiltin("9", "b5", SEV, ":9-5:79b5:79-5:", 0, 0, NP, -1, NP, -1);
        addBuiltin("7", "#5", SEV, ":7+5:+7:7+:7(b13):7aug:aug7:7b13:", NP, 0, NP, +1, NP, -1);
        addBuiltin("9", "#5", SEV, ":9+5:79#5:9+:", 0, 0, NP, +1, NP, -1);
        addBuiltin("7", "b9", SEV, ":7-9:7(b9):", -1, 0, NP, 0, NP, -1);
        addBuiltin("7", "#9", SEV, ":7+9:7(#9):", +1, 0, NP, 0, NP, -1);
        addBuiltin("7", "#9#5", SEV, ":7+5+9:7#5#9:7alt:", +1, 0, NP, +1, NP, -1);
        addBuiltin("7", "b9#5", SEV, ":7+5-9:7#5b9:7b9b13:", -1, 0, NP, +1, NP, -1);
        addBuiltin("7", "b9b5", SEV, ":7-5-9:7b5b9:", -1, 0, NP, -1, NP, -1);
        addBuiltin("7", "#9b5", SEV, ":7-5+9:7b5#9:", +1, 0, NP, -1, NP, -1);
        addBuiltin("7", "#11", SEV, ":7+11:", NP, 0, +1, 0, NP, -1);
        addBuiltin("9", "#11", SEV, ":9+11:", 0, 0, +1, 0, NP, -1);
        addBuiltin("7", "b9#11", SEV, ":7-9+11:", -1, 0, +1, 0, NP, -1);
        addBuiltin("7", "#9#11", SEV, ":7+9+11:", +1, 0, +1, 0, NP, -1);
        addBuiltin("13", "b5", SEV, ":13-5:713b5:713-5:", NP, 0, NP, -1, 0, -1);
        addBuiltin("13", "b9", SEV, ":13-9:713b9:713-9:", -1, 0, NP, 0, 0, -1);
        addBuiltin("13", "b9b5", SEV, ":13-9-5:13b5b9:", -1, 0, NP, -1, 0, -1);
        addBuiltin("13", "#9", SEV, ":13+9:713#9:713+9:", +1, 0, NP, 0, 0, -1);
        addBuiltin("13", "#9b5", SEV, ":13+9-5:13b5#9:", +1, 0, NP, -1, 0, -1);
        addBuiltin("13", "#11", SEV, ":13+11:713#11:713+11:", 0, 0, +1, 0, 0, -1);
        addBuiltin("13", "b9#11", SEV, ":13-9+11:", -1, 0, +1, 0, 0, -1);

        // MINOR
        addBuiltin("m", "", MIN, ":min:mi:-:", NP, -1, NP, 0, NP, NP);
        addBuiltin("m2", "", MIN, ":min2:mi2:-2:madd2:madd9:", 0, -1, NP, 0, NP, NP);
        addBuiltin("m+", "", MIN, ":m#5:mi#5:m+5:mi+:-#5:maug:", NP, -1, NP, 1, NP, NP);
        addBuiltin("m6", "", MIN, ":min6:mi6:-6:", NP, -1, NP, 0, 0, NP);
        addBuiltin("m6", "9", MIN, ":min69:mi69:-69:", 0, -1, NP, 0, 0, NP);
        addBuiltin("m7", "", MIN, ":mi7:min7:-7:", NP, -1, NP, 0, NP, -1);
        addBuiltin("m7", "b9", MIN, ":mi7b9:min7b9:-7b9:", -1, -1, NP, 0, NP, -1);
        addBuiltin("m7", "13", MIN, ":mi713:min713:-713:m7add13:", NP, -1, NP, 0, 0, -1);
        addBuiltin("m7", "#5", MIN, ":mi7#5:min7#5:-7#5:", NP, -1, NP, 1, NP, -1);
        addBuiltin("m9", "", MIN, ":mi9:min9:min(9):min7(9):-9:", 0, -1, NP, 0, NP, -1);
        addBuiltin("m9", "11", MIN, ":m9(11):mi911:min911:-9(11):-911:", 0, -1, 0, 0, NP, -1);
        addBuiltin("m11", "", MIN, ":m711:mi711:min711:-11:-711:min7(11):m7add11:m7add4:madd4:", NP, -1, 0, 0, NP, -1);
        addBuiltin("m13", "", MIN, ":mi13:min13:-13:m913:m9add13:", 0, -1, NP, 0, 0, -1);
        addBuiltin("m", "7M", MIN, ":-maj7:min7M:minMaj7:-7M:mM7:mMaj7:", NP, -1, NP, 0, NP, 0);
        addBuiltin("m9", "7M", MIN, ":mi9M:min9M:minMaj7(9):-9M:mM9:m7M9", 0, -1, NP, 0, NP, 0);


        // DIMINISHED
        addBuiltin("", "dim", DIM, ":°:o:h:mb5:dim5:", NP, -1, NP, -1, NP, NP);
        addBuiltin("", "dim7", DIM, ":°7:o7:7dim:h7:", NP, -1, NP, -1, 0, NP);
        addBuiltin("", "dim7M", DIM, ":°7M:o7M:oM7:7dim7M:dimM7:", NP, -1, NP, -1, NP, 0);
        addBuiltin("m7", "b5", DIM, ":m7-5:mi7b5:mi7-5:min7b5:min7-5:-7b5:", NP, -1, NP, -1, NP, -1);
        addBuiltin("m9", "b5", DIM, ":m9-5:mi9b5:mi9-5:min9b5:min9-5:-9b5:", 0, -1, NP, -1, NP, -1);
        addBuiltin("m11", "b5", DIM, ":m11(b5):min11(b5):-11b5:-11(b5):", NP, -1, 0, -1, NP, -1);

        
        // SUS
        addBuiltin("2", "", SUS, ":add9:1+2+5:sus2:add2:", 0, NP, NP, 0, NP, NP);           
        addBuiltin("", "sus", SUS, ":sus4:4:", NP, NP, 0, 0, NP, NP);
        addBuiltin("7", "sus", SUS, ":sus7:7sus4:74:11:", NP, NP, 0, 0, NP, -1);
        addBuiltin("9", "sus", SUS, ":79sus:sus79:sus9:9sus4:94:", 0, NP, 0, 0, NP, -1);
        addBuiltin("13", "sus", SUS, ":713sus:sus713:sus13:13sus4:134:", 0, NP, 0, 0, 0, -1);
        addBuiltin("7", "susb9", SUS, ":7sus-9:7sus4b9:sus7b9:sus7-9:7b9sus:7b9sus4:", -1, NP, 0, 0, NP, -1);
        addBuiltin("13", "susb9", SUS, ":13sus-9:sus13b9:sus13-9:", -1, NP, 0, 0, 0, -1);


        buildAliasMap();
        // LOGGER.severe("DEBUG DUMP AliasMap=============");
        // mapAliasCt.keySet().forEach(s -> LOGGER.severe(s + " -> " + mapAliasCt.get(s)));

        buildExtensionMap();


    }


    /**
     * Try to guess where the extension part of a chord type string starts.
     * <p>
     * For example for "madd9", return 1 because base=m and extension="add9". Should be used only if a ChordType.getOriginalName() differs from
     * ChordType.getName().
     *
     * @param ctStr A chord type string like "", "sus7", "7dim7M", "Maj7aug", "madd9", "+", "+7" etc.
     * @return The index of the first char of the extension. -1 if no extension found.
     */
    @Override
    public int guessExtension(String ctStr)
    {
        if (ctStr == null)
        {
            throw new IllegalArgumentException("ctStr=" + ctStr);
        }
        if (ctStr.isBlank())
        {
            return -1;
        }

        Integer res;
        int start = Math.min(ctStr.length(), 5);

        // Need to start by longer strings first
        for (int length = start; length >= 1; length--)
        {
            String first = ctStr.substring(0, length);

            // Try first case aware strings
            res = mapExtensionIndex.get(first);
            if (res != null)
            {
                return res;
            }

            res = mapExtensionIndexIgnoreCase.get(first.toLowerCase());
            if (res != null)
            {
                return res;
            }
        }

        return -1;
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
    @Override
    public void addAlias(ChordType ct, String alias) throws InvalidAliasException
    {
        if (!chordTypes.contains(ct))
        {
            throw new IllegalArgumentException("ct=" + ct + " alias=" + alias);
        }
        ChordType oldCt = mapAliasCt.get(alias);
        if (oldCt == ct)
        {
            return;
        } else if (oldCt != null)
        {
            String msg = ResUtil.getString(getClass(), "ChordTypeDatabase.ERR_RedondantAlias", alias, ct, oldCt.getName());
            throw new InvalidAliasException(msg);
        }

        // Update the aliases string
        String newAliases = getAliasesString(ct);
        String pre = newAliases.endsWith(":") ? "" : ":";
        newAliases += pre + alias + ":";

        // Update model
        mapAliasCt.put(alias, ct);
        storeAliasesString(ct, newAliases);
    }

    @Override
    public void resetAliases(ChordType ct)
    {
        storeAliasesString(ct, null);
        buildAliasMap();
    }

    /**
     * Clear all the user changes.
     */
    @Override
    public final void resetAliasesToDefault()
    {
        try
        {
            prefs.clear();
            buildAliasMap();
        } catch (BackingStoreException ex)
        {
            LOGGER.log(Level.WARNING, "resetAliasesToDefault() problem resetting aliases : {0}", ex.getMessage());
        }
    }

    /**
     * Get the aliases of the specified chord type.
     * <p>
     * Use the aliases stored in the preferences if available, otherwise return the default aliases.
     *
     * @param ct
     * @return
     */
    @Override
    public List<String> getAliases(ChordType ct)
    {
        ArrayList<String> res = new ArrayList<>();
        String aliases = getAliasesString(ct);
        if (aliases != null)
        {
            for (String alias : aliases.split(":"))
            {
                if (!alias.isBlank())
                {
                    res.add(alias);
                }
            }
        }
        return res;
    }

    /**
     * Get all the ChordTypes of the database.
     *
     * @return
     */
    @Override
    public List<ChordType>  getChordTypes()
    {
        return Collections.unmodifiableList(chordTypes);
    }

    /**
     * Get the number of chord types in the database.
     *
     * @return
     */
    @Override
    public int getSize()
    {
        return chordTypes.size();
    }

    /**
     * Get a chord type from the database.
     *
     * @param i The index of the chord type
     * @return
     */
    @Override
    public ChordType getChordType(int i)
    {
        if ((i < 0) || (i >= chordTypes.size()))
        {
            throw new IllegalArgumentException("i=" + i);
        }

        return chordTypes.get(i);
    }

    /**
     * Get a chord type from the database from a String description.
     * <p>
     * The String must match the chord type name or one of its aliases.
     *
     * @param s The String, e.g. "m7".
     * @return A ChordType, null if no ChordType correspond to s.
     */
    @Override
    public ChordType getChordType(String s)
    {
        if (s == null)
        {
            throw new IllegalArgumentException("s=" + s);
        }
        return mapAliasCt.get(s);
    }

    /**
     * Get the ChordType which match the specified degrees.
     *
     * @param degrees
     * @return Can be null
     */
    @Override
    public ChordType getChordType(List<Degree> degrees)
    {
        if (degrees == null || degrees.isEmpty())
        {
            throw new IllegalArgumentException("degrees=" + degrees);
        }
        for (ChordType ct : chordTypes)
        {
            var ctDegrees = ct.getDegrees();
            if (ctDegrees.size() == degrees.size() && ctDegrees.containsAll(degrees))
            {
                return ct;
            }
        }
        return null;
    }

    /**
     * The index of the ct in the database.
     *
     * @param ct
     * @return -1 if ct is not present in the database.
     */
    @Override
    public int getChordTypeIndex(ChordType ct)
    {
        if (ct == null)
        {
            throw new IllegalArgumentException("ct=" + ct);
        }
        return chordTypes.indexOf(ct);
    }

    // ==================================================================================================================
    // Private methods
    // ==================================================================================================================
    /**
     * Build a chordtype and add it in the database with its default aliases.
     * <p>
     * Chordtype should be unique.
     *
     * @param b
     * @param e
     * @param f
     * @param aliases A string of aliases separated with ':'
     * @param i9
     * @param i3
     * @param i11
     * @param i5
     * @param i13
     * @param i7
     */
    private void addBuiltin(String b, String e, ChordType.Family f, String aliases, int i9, int i3, int i11, int i5, int i13, int i7)
    {
        // Build the ChordType 
        ChordType ct = new ChordType(b, e, f, i9, i3, i11, i5, i13, i7);
        int index = chordTypes.indexOf(ct);
        if (index != -1)
        {
            throw new IllegalStateException("ChordType already exists ! ct=" + ct + " a=" + aliases + " existing_ct=" + chordTypes.get(index));
        }

        // Save in the database
        chordTypes.add(ct);

        // Save the alias list
        mapCtDefaultAliases.put(ct, aliases);
    }

    /**
     * Build the mapAliasCt map.
     *
     * @throws IllegalStateException If error building the map (e.g. redundant aliases)
     */
    private void buildAliasMap()
    {
        boolean b = true;
        HashMap<String, ChordType> mapSave = new HashMap<>(mapAliasCt);
        mapAliasCt.clear();
        for (ChordType ct : chordTypes)
        {
            mapAliasCt.put(ct.getName(), ct);       // By default
            for (String alias : getAliases(ct))
            {
                ChordType curCt = mapAliasCt.get(alias);
                if (curCt != null)
                {
                    mapAliasCt = mapSave;
                    String msg = "Alias '" + alias + "' can not be used for chord type '" + ct + "', it's already used for chord type '" + curCt + "'";
                    LOGGER.severe("buildAliasMap() " + msg);
                    b = false;
                } else
                {
                    mapAliasCt.put(alias, ct);
                }
            }
        }
        if (!b)
        {
            throw new IllegalStateException("buildAliasMap() error(s) building the alias map, see log messages.");
        }
    }

    /**
     * Return the Preferences string if defined, otherwise use the builtin default aliases string.
     *
     * @param ct
     * @return
     */
    private String getAliasesString(ChordType ct)
    {
        String str = prefs.get(PREFIX + ct.getName(), mapCtDefaultAliases.get(ct));
        return str;
    }

    private void storeAliasesString(ChordType ct, String s)
    {
        String key = PREFIX + ct.getName();
        if (s == null)
        {
            prefs.remove(key);
        } else
        {
            prefs.put(key, s);
        }
    }

    /**
     * Build the data to guess base/extension of a chord string.
     */
    private void buildExtensionMap()
    {

        // mapExtensionIndexIgnoreCase data must be lowercase

        // 5 first letters
        mapExtensionIndexIgnoreCase.put("maj11", 5);
        mapExtensionIndexIgnoreCase.put("maj13", 5);
        mapExtensionIndexIgnoreCase.put("min11", 5);
        mapExtensionIndexIgnoreCase.put("min13", 5);
        mapExtensionIndex.put("min7m", 3);
        mapExtensionIndex.put("min9M", 3);

        // 4 letters
        mapExtensionIndexIgnoreCase.put("maj6", 4);
        mapExtensionIndexIgnoreCase.put("maj7", 4);
        mapExtensionIndexIgnoreCase.put("maj9", 4);
        mapExtensionIndexIgnoreCase.put("min6", 4);
        mapExtensionIndexIgnoreCase.put("min7", 4);
        mapExtensionIndexIgnoreCase.put("min9", 4);
        mapExtensionIndexIgnoreCase.put("ma11", 4);
        mapExtensionIndexIgnoreCase.put("ma13", 4);
        mapExtensionIndexIgnoreCase.put("mi11", 4);
        mapExtensionIndexIgnoreCase.put("mi13", 4);
        mapExtensionIndexIgnoreCase.put("bass", 4);
        mapExtensionIndex.put("mi7M", 2);
        mapExtensionIndex.put("mi9M", 2);

        // 3 letters
        mapExtensionIndexIgnoreCase.put("ma6", 3);
        mapExtensionIndexIgnoreCase.put("ma7", 3);
        mapExtensionIndexIgnoreCase.put("ma9", 3);
        mapExtensionIndexIgnoreCase.put("mi6", 3);
        mapExtensionIndexIgnoreCase.put("mi7", 3);
        mapExtensionIndexIgnoreCase.put("mi9", 3);
        mapExtensionIndex.put("11M", 3);
        mapExtensionIndex.put("13M", 3);
        mapExtensionIndex.put("M11", 3);
        mapExtensionIndex.put("M13", 3);
        mapExtensionIndexIgnoreCase.put("maj", 3);
        mapExtensionIndexIgnoreCase.put("min", 3);
        mapExtensionIndex.put("-7M", 1);
        mapExtensionIndex.put("-9M", 1);
        mapExtensionIndexIgnoreCase.put("dim", 0);
        mapExtensionIndexIgnoreCase.put("add", 0);
        mapExtensionIndexIgnoreCase.put("lyd", 0);
        mapExtensionIndexIgnoreCase.put("sus", 0);

        // 2 letters
        mapExtensionIndex.put("6M", 2);
        mapExtensionIndex.put("7M", 2);
        mapExtensionIndex.put("9M", 2);
        mapExtensionIndex.put("M6", 2);
        mapExtensionIndex.put("M7", 2);
        mapExtensionIndex.put("M9", 2);
        mapExtensionIndex.put("m6", 2);
        mapExtensionIndex.put("m7", 2);
        mapExtensionIndex.put("m9", 2);
        mapExtensionIndex.put("-6", 2);
        mapExtensionIndex.put("-7", 2);
        mapExtensionIndex.put("-9", 2);
        mapExtensionIndex.put("+7", 2);
        mapExtensionIndex.put("+9", 2);       
        mapExtensionIndexIgnoreCase.put("ma", 2);
        mapExtensionIndexIgnoreCase.put("mi", 2);
        mapExtensionIndex.put("11", 2);
        mapExtensionIndex.put("13", 2);

        // 1 letter
        mapExtensionIndex.put("m", 1);
        mapExtensionIndex.put("M", 1);
        mapExtensionIndex.put("-", 1);
        mapExtensionIndex.put("4", 1);
        mapExtensionIndex.put("6", 1);
        mapExtensionIndex.put("7", 1);
        mapExtensionIndex.put("9", 1);
        mapExtensionIndex.put("°", 1);
        mapExtensionIndex.put("o", 1);
        mapExtensionIndex.put("h", 1);
    }
}
