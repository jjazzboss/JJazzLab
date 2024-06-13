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
package org.jjazz.phrasetransform;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.openide.util.Lookup;
import org.jjazz.phrasetransform.spi.PhraseTransformProvider;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransformManager;

/**
 * Manage the available PhraseTransforms on the system.
 */
public class PhraseTransformManagerImpl implements PhraseTransformManager
{

    private static PhraseTransformManagerImpl INSTANCE;

    private List<PhraseTransform> transforms;
    private static final Logger LOGGER = Logger.getLogger(PhraseTransformManagerImpl.class.getSimpleName());

    public static PhraseTransformManagerImpl getInstance()
    {
        synchronized (PhraseTransformManagerImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new PhraseTransformManagerImpl();
            }
        }
        return INSTANCE;
    }

    private PhraseTransformManagerImpl()
    {
    }

    @Override
    public List<PhraseTransform> getPhraseTransforms()
    {
        if (transforms == null)
        {
            refresh();
        }
        var res = transforms.stream().map(t -> t.getCopy()).toList();
        return res;
    }


    @Override
    public void refresh()
    {
        if (transforms == null)
        {
            transforms = new ArrayList<>();
        } else
        {
            transforms.clear();
        }
        var ptps = Lookup.getDefault().lookupAll(PhraseTransformProvider.class);
        for (var ptp : ptps)
        {
            transforms.addAll(ptp.getTransforms());
        }
    }

    // ==============================================================================
    // Private methods
    // ==============================================================================

}
