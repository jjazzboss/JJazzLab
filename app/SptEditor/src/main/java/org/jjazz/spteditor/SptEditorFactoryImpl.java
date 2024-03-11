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
package org.jjazz.spteditor;

import org.jjazz.spteditor.api.SptEditor;
import org.jjazz.spteditor.api.SptEditorSettings;
import org.jjazz.spteditor.spi.SptEditorFactory;
import org.jjazz.spteditor.spi.DefaultRpEditorComponentFactory;

public class SptEditorFactoryImpl implements SptEditorFactory
{

    static private SptEditorFactoryImpl INSTANCE;

    static public SptEditorFactoryImpl getInstance()
    {
        synchronized (SptEditorFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new SptEditorFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private SptEditorFactoryImpl()
    {
    }

    @Override
    public SptEditor createEditor(SptEditorSettings settings, DefaultRpEditorComponentFactory factory)
    {
        return new SptEditorImpl(settings, factory);
    }
}
