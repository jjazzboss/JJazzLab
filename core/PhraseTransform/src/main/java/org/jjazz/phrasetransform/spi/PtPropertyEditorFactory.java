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
package org.jjazz.phrasetransform.spi;

import javax.swing.JDialog;
import org.jjazz.phrasetransform.api.PtProperties;
import org.openide.util.Lookup;

/**
 * A factory to get dialogs to edit PhraseTransform properties.
 */
public interface PtPropertyEditorFactory
{

    static public PtPropertyEditorFactory getDefault()
    {
        var res = Lookup.getDefault().lookup(PtPropertyEditorFactory.class);
        if (res == null)
        {
            throw new IllegalStateException("No PropertyEditorFactory implementation found");
        }
        return res;
    }

    /**
     * Get a JDialog to edit a single property.
     *
     * @param ptProperties        The properties we modify
     * @param dialogTitle
     * @param property            The proprerty name we read/write
     * @param propertyDisplayName The display name of the property
     * @param minPropertyValue
     * @param maxPropertyValue
     * @param usePanoramicKnob
     * @return
     */
    JDialog getSinglePropertyEditor(PtProperties ptProperties, String dialogTitle, String property, String propertyDisplayName, int minPropertyValue, int maxPropertyValue, boolean usePanoramicKnob);
}
