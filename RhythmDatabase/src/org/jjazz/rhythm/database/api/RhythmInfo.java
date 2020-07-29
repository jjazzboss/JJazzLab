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
package org.jjazz.rhythm.database.api;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;

/**
 * A description of a Rhythm for catalog purpose.
 * <p>
 * A RhythmInfo contains descriptive information of a Rhythm instance. It allows to defer creation of Rhythm instances when possible,
 * because Rhythm instance creation can be very time consuming when dealing with hundreds of file-based Rhythms. The RhythmDatabase provides
 * methods to get RhythmInfo/Rhythm instances.
 * <p>
 * This is an immutable class.
 * <p>
 */
public interface RhythmInfo extends Serializable
{

    /**
     * Check that this RhythmInfo object matches data from specified rhythm.
     * <p>
     *
     * @param r
     * @return False if inconsistency detected
     */
    boolean checkConsistency(Rhythm r);

    /**
     * The unique Id of the target rhythm.
     *
     * @return
     */
    String getUniqueId();

    /**
     * The unique id of the provider of the target rhythm.
     *
     * @return
     */
    String getRhythmProviderId();

    String getAuthor();

    List<RhythmParameterInfo> getRhythmParametersInfos();

    List<RhythmVoiceInfo> getRhythmVoiceInfos();

    String getDescription();

    RhythmFeatures getFeatures();

    File getFile();

    String getName();

    int getPreferredTempo();

    String[] getTags();

    TimeSignature getTimeSignature();

    String getVersion();
}
