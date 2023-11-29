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
package org.jjazz.utilities.api;

import java.util.ArrayList;
import java.util.List;

/**
 * An error report which can store multiple individual errors.
 * <p>
 */
public class MultipleErrorsReport
{

    /**
     * If non-null this one-line summary message will be used by the framework to notify end-user.
     * <p>
     * Example: "2 files could not be read: aa.sty, bb.sty"
     */
    public String primaryErrorMessage;

    /**
     * If non-null this one-line message will be used by the framework to provide additional information to end-user.
     * <p>
     * Example: "Rhythm Provider: YamJJazz Extended"
     */
    public String secondaryErrorMessage;

    /**
     * If not empty will be used by the framework to provide error details to end-user.
     * <p>
     * Example:<br>
     * [0] = "aa.sty: CASM data is corrupted"<br>
     * [1] = "bb.sty: invalid low key parameter value=182 at byte 0x1029. Authorized value range is 0-127."
     */
    public List<String> individualErrorMessages = new ArrayList<>();
}
