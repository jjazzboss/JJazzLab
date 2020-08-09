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
package org.jjazz.ui.musiccontrolactions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.song.api.Song;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "File", id = "org.jjazz.ui.actions.DecreaseTempo")
@ActionRegistration(displayName = "#CTL_DecreaseTempo", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Shortcuts", name = "SUBTRACT"),
            @ActionReference(path = "Shortcuts", name = "MINUS")
        })
@Messages(
        {
            "CTL_DecreaseTempo=Decrease Tempo"
        })
public final class DecreaseTempo implements ActionListener
{

    final private Song song;

    public DecreaseTempo(Song sg)
    {
        song = sg;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (ActiveSongManager.getInstance().getActiveSong() == song)
        {
            int tempo = song.getTempo() - 5;
            if (TempoRange.checkTempo(tempo))
            {
                song.setTempo(tempo);
            }
        }
    }
}
