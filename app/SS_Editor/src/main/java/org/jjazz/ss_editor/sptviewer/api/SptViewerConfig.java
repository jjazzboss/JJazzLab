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

import java.util.Objects;

/**
 * The UI config of a SptViewer.
 */
public record SptViewerConfig(boolean showName, boolean showRhythm, boolean showTimeSignature, MultiSelect multiSelect)
        {

    public enum MultiSelect
    {
        OFF, ON, ON_FIRST
    }

    public SptViewerConfig()
    {
        this(true, true, true, MultiSelect.OFF);
    }

    public SptViewerConfig
    {
        Objects.requireNonNull(multiSelect);
    }

    public SptViewerConfig setShowName(boolean b)
    {
        return new SptViewerConfig(b, showRhythm, showTimeSignature, multiSelect);
    }

    public SptViewerConfig setShowRhythm(boolean b)
    {
        return new SptViewerConfig(showName, b, showTimeSignature, multiSelect);
    }

    public SptViewerConfig setShowTimeSignature(boolean b)
    {
        return new SptViewerConfig(showName, showRhythm, b, multiSelect);
    }

    public SptViewerConfig setMultiSelect(MultiSelect ms)
    {
        Objects.requireNonNull(ms);
        return new SptViewerConfig(showName, showRhythm, showTimeSignature, ms);
    }
}
