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

import java.util.List;
import javax.swing.Icon;
import org.jjazz.midi.api.Instrument;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransformCategory;
import org.jjazz.phrasetransform.api.PhraseTransform;

/**
 *
 * @author Jerome
 */
public class BassDrumsTransform implements PhraseTransform
{

    @Override
    public SizedPhrase transform(SizedPhrase inPhrase, Instrument ins)
    {
        return inPhrase;
    }

    @Override
    public int getFitScore(SizedPhrase inPhrase, Instrument ins)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getUniqueId()
    {
        return "BassDrumsTransformID";
    }

    @Override
    public PhraseTransformCategory getCategory()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getDescription()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Icon getIcon()
    {
        return PhraseTransform.super.getIcon(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<String> getPropertyKeys()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setProperty(String key, String value)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getProperty(String key)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
