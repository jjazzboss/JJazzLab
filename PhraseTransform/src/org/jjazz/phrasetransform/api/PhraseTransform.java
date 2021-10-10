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

import java.util.List;
import javax.swing.Icon;
import org.jjazz.midi.api.Instrument;
import org.jjazz.phrase.api.SizedPhrase;

/**
 * Transform a phrase into another one.
 */
public interface PhraseTransform
{        
    /**
     * Transform the specified phrase to create another one.
     *
     * @param inPhrase
     * @param ins The instrument from which inPhrase comes from
     * @return A new transformed phrase.
     */
    public SizedPhrase transform(SizedPhrase inPhrase, Instrument ins);

    /**
     * Return a [0-100] value which indicates how much this transform is adapted for the specified parameters.
     *
     * @param inPhrase
     * @param ins The instrument from which inPhrase comes from
     * @return 0 means this transform is not adapted at all, 100 means this transform is perfectly fit for this context.
     */
    public int getFitScore(SizedPhrase inPhrase, Instrument ins);

    /**
     * A unique id associated to this transform class.
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
     * Describes what this transform does.
     *
     * @return Can't be null
     */
    public String getDescription();

    /**
     * An optional 16pix-height icon representing this transform.
     *
     * @return Can be null
     */
    default public Icon getIcon()
    {
        return null;
    }

    /**
     * Get the supported property keys.
     *
     * @return
     */
    public List<String> getPropertyKeys();

    /**
     * Set (or reset) the property for key.
     *
     * @param key
     * @param value Use null to reset the property to its default value.
     */
    public void setProperty(String key, String value);

    /**
     * Get a property value.
     *
     * @param key
     * @return Can be null.
     */
    public String getProperty(String key);

      /**
     * Show a modal dialog to modify the user settings of this PhraseTransform.
     * <p>
 The PhraseTransform is responsible for the persistence of its settings. The method does nothing if hasUserSettings() returns
 false.
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

}
