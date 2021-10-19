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
package org.jjazz.phrasetransform;

import java.util.ArrayList;
import java.util.List;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.phrasetransform.spi.PhraseTransformProvider;
import org.jjazz.phrasetransform.api.PhraseTransform;

/**
 *
 */
@ServiceProvider(service=PhraseTransformProvider.class)
public class DefaultPhraseTransformProvider implements PhraseTransformProvider
{

    List<PhraseTransform> transforms = new ArrayList<>();
    public DefaultPhraseTransformProvider()
    {
        transforms.add(new OpenHiHatTransform());
        transforms.add(new SwingTransform());
    }
            
    @Override
    public List<PhraseTransform> getTransforms()
    {
        return new ArrayList<>(transforms);
    }
    
}
