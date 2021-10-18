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
package org.jjazz.phrasetransform.api;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Dimension;
import java.text.ParseException;
import javax.swing.Icon;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.songcontext.api.SongPartContext;

/**
 * Transform a phrase into another one.
 */
public interface PhraseTransform extends Comparable<PhraseTransform>
{

    public static final String SAVE_STRING_DELIMITER = "#";
    public static final Dimension ICON_SIZE = new Dimension(48, 24);

    /**
     * Transform inPhrase into another phrase.
     * <p>
     * The optional context must match inPhrase beat range. Context can be used to retrieve additional information, e.g.:<br>
     * - the chord symbols used in the phrase<br>
     * - the target RhythmVoice (or RhythmVoiceDelegate) for the phrase, obtained via
     * context.getMidiMix().getRhythmVoice(inPhrase.getChannel()). <br>
     * - the InstrumentMix used for the phrase, obtained via
     * context.getMidiMix().getInstrumentMixFromChannel(inPhrase.getChannel()).
     *
     * @param inPhrase
     * @param context Can be null. An optional context to provide additional information about inPhrase.
     * @return The new transformed phrase.
     * @throws IllegalArgumentException If context is non-null and its beat range differs from the beat range of inPhrase.
     */
    public SizedPhrase transform(SizedPhrase inPhrase, SongPartContext context);

    /**
     * Return a [0-100] value which indicates how much this transform is adapted for the specified parameters.
     * <p>
     * A PhraseTransform which can not work without a context should return 0 if context is null.
     *
     * @param inPhrase
     * @param context Can be null. An optional context to provide additional information about inPhrase.
     * @return 0 means this transform is not adapted at all, 100 means this transform is perfectly fit for this context.
     * @see #transform(org.jjazz.phrase.api.SizedPhrase, org.jjazz.songcontext.api.SongPartContext)
     */
    public int getFitScore(SizedPhrase inPhrase, SongPartContext context);

    /**
     * A unique id associated to this transform class.
     * <p>
     * IMPORTANT: character SAVE_STRING_DELIMITER is forbidden here! (used as a separator in saveAsString()/loadFromString())
     *
     * @return
     */
    public String getUniqueId();

    /**
     * The category of this transform.
     * <p>
     * Used to group PhraseTransforms in the user interface.
     *
     * @return Can't be null
     */
    public PhraseTransformCategory getCategory();

    /**
     * The name of the PhraseTransform.
     *
     * @return
     */
    public String getName();

     
    /**
     * Describes what this transform does.
     *
     * @return Can't be null
     */
    public String getDescription();

    /**
     * Get a copy of this PhraseTransform instance.
     *
     * @return
     */
    public PhraseTransform getCopy();

    /**
     * Compare using alphabetical order first on category, then on name.
     *
     * @param pt
     * @return
     */
    @Override
    default public int compareTo​(PhraseTransform pt)
    {
        int res = getCategory().getDisplayName().compareTo(pt.getCategory().getDisplayName());
        if (res == 0)
        {
            res = getName().compareTo(pt.getName());
        }
        return res;
    }

    /**
     * An optional icon representing this transform.
     * <p>
     * Icon size must be ICON_SIZE.
     *
     * @return Can be null
     * @see #ICON_SIZE
     */
    default public Icon getIcon()
    {
        return null;
    }

    /**
     * Get the PhraseTransform properties.
     *
     * @return Can't be null (but can be empty)
     */
    public PtProperties getProperties();


    /**
     * Show a modal dialog to modify the user settings of this PhraseTransform.
     * <p>
     * The PhraseTransform is responsible for the persistence of its settings. The method does nothing if hasUserSettings()
     * returns false.
     *
     * @see hasUserSettings()
     */
    default public void showUserSettingsDialog()
    {
        // Nothing
    }

    /**
     * Return true if PhraseTransform has settings which can be modified by end-user.
     * <p>
     *
     * @return @see showUserSettingsDialog()
     */
    default public boolean hasUserSettings()
    {
        return false;
    }

    /**
     * Helper method to implement PhraseTransform subclass equals(Object) method.
     * <p>
     * 2 PhraseTransforms are equal when they have same uniqueId and same property/value pairs.
     *
     * @param pt1
     * @param o2
     * @return
     */
    static public boolean equals(PhraseTransform pt1, Object o2)
    {
        if (!(o2 instanceof PhraseTransform))
        {
            return false;
        }

        PhraseTransform pt2 = (PhraseTransform) o2;

        if (!pt1.getUniqueId().equals(pt2.getUniqueId()))
        {
            return false;
        }

        return pt1.getProperties().equals(pt2.getProperties());
    }

    /**
     * Helper method to implement PhraseTransform subclass hashCode() method.
     * <p>
     * Rely on uniqueId and properties.
     *
     * @param pt
     * @return
     */
    static public int hashCode(PhraseTransform pt)
    {
        return Objects.hashCode(pt.getUniqueId(), pt.getProperties());
    }

    /**
     * A string like "uniqueId#prop1=value1,prop=value2".
     * <p>
     * If no property set, return "uniqueId#".
     *
     * @param pt
     * @return
     * @see PhraseTransform#loadFromString(java.lang.String)
     * @see #SAVE_STRING_DELIMITER
     */
    static public String saveAsString(PhraseTransform pt)
    {
        String res = pt.getUniqueId() + SAVE_STRING_DELIMITER + pt.getProperties().saveAsString(pt.getProperties().getNonDefaultValueProperties());
        return res;
    }


    /**
     * Try to get a PhraseTransform instance from the specified save string.
     *
     * @param s
     * @return Null if not found.
     * @throws java.text.ParseException
     * @see PhraseTransform#saveAsString(PhraseTransform)
     * @see #SAVE_STRING_DELIMITER
     */
    static public PhraseTransform loadFromString(String s) throws ParseException
    {
        checkNotNull(s);
        PhraseTransform res = null;
        var ptm = PhraseTransformManager.getDefault();

        String strs[] = s.split(SAVE_STRING_DELIMITER);

        switch (strs.length)
        {
            case 1:
                res = ptm.getPhraseTransform(strs[0].trim());
                break;
            case 2:
                res = ptm.getPhraseTransform(strs[0].trim());
                res.getProperties().setPropertiesFromString(strs[1].trim());
                break;
            default:
                throw new ParseException("Invalid PhraseTransform string s=" + s, 0);
        }

        return res;

    }

}
