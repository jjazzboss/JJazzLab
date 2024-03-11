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

import java.awt.BorderLayout;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.spteditor.api.RpEditor;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.spteditor.spi.RpEditorComponent;

/**
 * A RpEditor stub.
 * <p>
 */
public class RpEditorStub extends RpEditorComponent
{
    JLabel label;
    Object value;
    private static final Logger LOGGER = Logger.getLogger(RpEditorStub.class.getSimpleName());

    public RpEditorStub(SongPart spt, RhythmParameter<?> rp)
    {
        super(spt, rp);

        // Prepare our editor component
        value = spt.getRPValue(rp);
        label = new JLabel(value.toString());

        // Add it
        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);
    }

    @Override
    public void showMultiValueMode(boolean b)
    {
        RpEditor.showMultiModeUsingFont(b, label);
    }

    @Override
    public Object getEditorValue()
    {
        return value;
    }

    @Override
    public void updateEditorValue(Object value)
    {
        this.value = value;
        label.setText(value.toString());
    }

}
