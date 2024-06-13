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
package org.jjazz.rpcustomeditorfactoryimpl;

import javax.swing.JDialog;
import org.jjazz.phrasetransform.api.PtProperties;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.phrasetransform.spi.PtPropertyEditorFactory;

@ServiceProvider(service = PtPropertyEditorFactory.class)
public class PtPropertyEditorFactoryImpl implements PtPropertyEditorFactory
{

    @Override
    public JDialog getSinglePropertyEditor(PtProperties ptProperties, String dialogTitle, String property, String propertyDisplayName, int minPropertyValue, int maxPropertyValue, boolean usePanoramicKnob)
    {
        return new PtSinglePropertyEditor(ptProperties, dialogTitle, property, propertyDisplayName, minPropertyValue, maxPropertyValue, usePanoramicKnob);
    }

}
