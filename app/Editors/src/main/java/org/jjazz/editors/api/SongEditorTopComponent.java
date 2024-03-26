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
package org.jjazz.editors.api;

import java.util.Set;
import org.jjazz.song.api.Song;
import org.openide.windows.TopComponent;

/**
 * A TopComponent which holds a SongEditor.
 */
public abstract class SongEditorTopComponent<T extends SongEditor> extends TopComponent
{

    /**
     * The type of SongEditorTopComponent.
     *
     * @return Can't be null
     */
    abstract String getTypeId();

    /**
     * The editor component of this TopComponent.
     *
     * @return Can't be null
     */
    abstract public T getEditor();

    /**
     * Convenience method to call getEditor().getSongModel().
     *
     * @return
     */
    public Song getSongModel()
    {
        return getEditor().getSongModel();
    }


    /**
     * Return the active SongEditorTopComponent (i.e. focused or ancestor of the focused component) with specified type.
     *
     * @return Can be null if no active SongEditorTopComponent, or if the active SongEditorTopComponent has the wrong type.
     */
    static public SongEditorTopComponent<?> getActive(String typeId)
    {
        TopComponent tc = TopComponent.getRegistry().getActivated();
        return (tc instanceof SongEditorTopComponent setc) && setc.getTypeId().equals(typeId) ? (SongEditorTopComponent) tc : null;
    }

    /**
     * Return the SongEditorTopComponent of the specified type for a song.
     *
     * @param song
     * @return Can be null
     */
    static public SongEditorTopComponent get(String typeId, Song song)
    {
        Set<TopComponent> tcs = TopComponent.getRegistry().getOpened();
        for (TopComponent tc : tcs)
        {
            if (tc instanceof SongEditorTopComponent seTc && seTc.getSongModel() == song)
            {
                return seTc;
            }
        }
        return null;
    }

}
