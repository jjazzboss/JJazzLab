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
package org.jjazz.ss_editor.sptviewer.api;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jjazz.rhythm.api.RhythmParameter;

/**
 * The UI config of a SptViewer.
 */
public record SptViewerConfig(boolean showName, boolean showRhythm, boolean showParentSection, boolean showTimeSignature, MultiSelect multiSelect,
        List<RhythmParameter<?>> visibleRPs)
        {

    public enum MultiSelect
    {
        OFF, ON, ON_FIRST
    }

    public SptViewerConfig()
    {
        this(true, true, true, true, MultiSelect.OFF, Collections.emptyList());
    }

    public SptViewerConfig
    {
        Objects.requireNonNull(multiSelect);
    }

    public SptViewerConfig setShowName(boolean b)
    {
        return new SptViewerConfig(b, showRhythm, showParentSection, showTimeSignature, multiSelect, visibleRPs);
    }

    public SptViewerConfig setShowRhythm(boolean b)
    {
        return new SptViewerConfig(showName, b, showParentSection, showTimeSignature, multiSelect, visibleRPs);
    }

    public SptViewerConfig setShowParentSection(boolean b)
    {
        return new SptViewerConfig(showName, showRhythm, b, showTimeSignature, multiSelect, visibleRPs);
    }

    public SptViewerConfig setShowTimeSignature(boolean b)
    {
        return new SptViewerConfig(showName, showRhythm, showParentSection, b, multiSelect, visibleRPs);
    }

    public SptViewerConfig setMultiSelect(MultiSelect ms)
    {
        Objects.requireNonNull(ms);
        return new SptViewerConfig(showName, showRhythm, showParentSection, showTimeSignature, ms, visibleRPs);
    }

    public SptViewerConfig setVisibleRPs(List<RhythmParameter<?>> rps)
    {
        Objects.requireNonNull(rps);
        return new SptViewerConfig(showName, showRhythm, showParentSection, showTimeSignature, multiSelect, rps);
    }
}
