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
package org.jjazz.pianoroll.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import org.jjazz.phrase.api.NoteEvent;

/**
 * A JComponent which represents a NoteEvent.
 */
public class NoteView extends JPanel implements PropertyChangeListener
{

    private NoteEvent noteEvent;
    
    public NoteView(NoteEvent ne)
    {
        setModel(ne);
    }

    public void setModel(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        if (noteEvent != null)
        {
            noteEvent.removeClientPropertyChangeListener(this);
        }
        noteEvent.addClientPropertyChangeListener(this);
    }
    
    public void cleanup()
    {
         noteEvent.removeClientPropertyChangeListener(this);
    }

    // ==============================================================================================================
    // PropertyChangeListener interface
    // ==============================================================================================================

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    // ==============================================================================================================
    // Private methods
    // ==============================================================================================================

}
