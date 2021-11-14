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
package org.jjazz.notesviewer.guitar;

import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.notesviewer.spi.NotesViewer;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * A NotesViewer based on guitar chord diagrams.
 */
@ServiceProvider(service = NotesViewer.class)
public class GuitarNotesViewer implements NotesViewer
{

    @StaticResource(relative = true)
    final private static String ICON_PATH = "resources/DiagramIcon.png";
    final private static Icon ICON = new ImageIcon(GuitarNotesViewer.class.getResource(ICON_PATH));
    private Mode mode = Mode.ShowSelection;
    private Song song;
    private MidiMix midiMix;
    private RhythmVoice rhythmVoice;
    private GuitarNotesViewerComponent component;
    private static final Logger LOGGER = Logger.getLogger(GuitarNotesViewer.class.getSimpleName());

    @Override
    public JComponent getComponent()
    {
        if (component == null)
        {
            component = new GuitarNotesViewerComponent(this);
        }
        return component;
    }

    @Override
    public Icon getIcon()
    {
        return ICON;
    }

    @Override
    public String getDescription()
    {
        return ResUtil.getString(getClass(), "GuitarViewerDesc");
    }

    @Override
    public void setContext(Song song, MidiMix midiMix, RhythmVoice rv)
    {
        this.song = song;
        this.midiMix = midiMix;
        this.rhythmVoice = rv;        
    }

    @Override
    public void setMode(Mode mode)
    {
        if (mode == null)
        {
            throw new NullPointerException("mode");
        }
        if (!this.mode.equals(mode))
        {
            this.mode = mode;
            releaseAllNotes();
            component.setMode(mode);
        }
    }

    @Override
    public Mode getMode()
    {
        return mode;
    }

    @Override
    public void realTimeNoteOn(int pitch, int velocity)
    {
        // Nothing
    }

    @Override
    public void realTimeNoteOff(int pitch)
    {
        // Nothing
    }

    @Override
    public void releaseAllNotes()
    {
        component.clear();
    }

    @Override
    public void showChordSymbolNotes(CLI_ChordSymbol cliCs)
    {
        component.showDiagrams(cliCs);
    }

    @Override
    public void showScaleNotes(StandardScaleInstance ssi)
    {
        // Nothing
    }

    @Override
    public void cleanup()
    {
        // Nothing
    }

    @Override
    public void setEnabled(boolean b)
    {
        getComponent().setEnabled(b);
    }

    // =================================================================================
    // Private methods
    // =================================================================================     
}
